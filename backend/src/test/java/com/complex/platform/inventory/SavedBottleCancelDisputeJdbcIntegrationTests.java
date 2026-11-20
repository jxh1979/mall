package com.complex.platform.inventory;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.crypto.PiiCipher;
import com.complex.platform.evidence.EvidenceEventService;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AP 段第 252 条 252-R1 与第 254 条 254-R6（真库、真实 HTTP、蛇形键、真实授权链；存酒行 JDBC 直接插入，不依赖登记形状）。
 * 252-R1：BSM:1940「……存酒作废、异常调拨都可能产生责任争议，必须记录原因、员工、时间、点位、照片、视频和主管复核结果。」——
 * 作废原因必填、落 saved_bottles.cancel_reason、进成功审计元数据，高价值作废的主管复核页摘要显示该原因。
 * 254-R6：第十八批裁决 58「门店主管核实后二选一：争议不成立，恢复为存酒中、照常可取；争议成立，走现有存酒作废（高价值的要照片＋视频＋主管复核，
 * 裁决 14），作废后按现有补存或赔付处理。」——争议原因进审计；门店主管（inventory.high_value.review）经 dispute-dismissal 恢复存酒中、
 * 或把争议中的存酒作废；店员与别家商户主管都不能办。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SavedBottleCancelDisputeJdbcIntegrationTests {

    private static final String BASE = "/api/v1/inventory/saved-bottles/";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private PiiCipher cipher;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private EvidenceEventService evidence;

    private final String s = String.valueOf(System.nanoTime());
    private final String project = "prj_b62sd_" + s;
    private final String merchant = "bar_b62sd_" + s;
    private final String lowItem = "item_b62sd_low_" + s;
    private final String highItem = "item_b62sd_high_" + s;
    private final String clerkId = "usr_b62sd_clerk_" + s;
    private final String phone = "155" + s.substring(s.length() - 8);
    private RequestPostProcessor clerk;
    private RequestPostProcessor supervisor;
    private RequestPostProcessor otherSupervisor;
    private RequestPostProcessor supervisor2;

    @BeforeEach
    void seed() {
        String entity = "ent_b62sd_" + s;
        String otherMerchant = "bar_b62sd_other_" + s;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','存酒争议综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'存酒争议主体','project_company','active',now(),now())", entity);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", entity, project);
        for (String m : List.of(merchant, otherMerchant)) {
            jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,project_id,status,created_at,updated_at)"
                    + " VALUES(?,'存酒争议酒吧','bar',?,?,'active',now(),now())", m, entity, project);
        }
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at)"
                + " VALUES(?,'存酒争议啤酒','liquor','瓶',0,'active',?,now(),now())", lowItem, merchant);
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at)"
                + " VALUES(?,'存酒争议威士忌','liquor','瓶',0,'active',?,now(),now())", highItem, merchant);
        // 高价值判定只看最近一次实际到货价（出厂阈值 5000 元）：低价值 10 元，高价值 6000 元。
        arrival(lowItem, 1_000L);
        arrival(highItem, 600_000L);
        StaffRequestFixture fixture = new StaffRequestFixture(jdbc);
        clerk = fixture.staff(clerkId, "staff", project, merchant, "bar.saved_bottle.manage", "evidence.attachment.create");
        supervisor = fixture.staff("usr_b62sd_sup_" + s, "staff", project, merchant,
                "bar.saved_bottle.manage", "inventory.high_value.review");
        otherSupervisor = fixture.staff("usr_b62sd_osup_" + s, "staff", project, otherMerchant,
                "bar.saved_bottle.manage", "inventory.high_value.review");
        supervisor2 = fixture.staff("usr_b62sd_sup2_" + s, "staff", project, merchant,
                "bar.saved_bottle.manage", "inventory.high_value.review");
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // 252-R1：低价值作废不带原因 → 400、状态不变、无复核单；带原因 → cancelled，原因落存酒行并进成功审计元数据。
    @Test
    void lowValueCancelRequiresReasonPersistsItAndAuditsIt() throws Exception {
        String bottle = insertBottle(lowItem, "stored");

        call(clerk, bottle + "/cancel", body("b62sd-cancel-noreason-" + s, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(statusOf(bottle)).isEqualTo("stored");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM inventory_high_value_reviews WHERE subject_id=?", Long.class, bottle)).isZero();

        call(clerk, bottle + "/cancel", body("b62sd-cancel-" + s, "酒瓶破损"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("cancelled"));
        assertThat(jdbc.queryForObject("SELECT cancel_reason FROM saved_bottles WHERE id=?", String.class, bottle)).isEqualTo("酒瓶破损");
        assertThat(auditReason("bar.saved_bottle.cancelled", bottle)).isEqualTo("酒瓶破损");
    }

    // 252-R1：高价值作废（照片＋视频）→ disputed_frozen，原因落库；门店主管在高价值复核页看到该单摘要原因。
    @Test
    void highValueCancelReasonShowsOnSupervisorReviewList() throws Exception {
        String bottle = insertBottle(highItem, "stored");
        Map<String, Object> cancel = body("b62sd-cancel-hv-" + s, "客人退回时瓶身碎裂");
        cancel.put("photo_attachment_ids", List.of(upload("broken.jpg", "image/jpeg", "photo")));
        cancel.put("video_attachment_ids", List.of(upload("broken.mp4", "video/mp4", "video")));

        call(clerk, bottle + "/cancel", cancel)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("disputed_frozen"));
        assertThat(jdbc.queryForObject("SELECT cancel_reason FROM saved_bottles WHERE id=?", String.class, bottle))
                .isEqualTo("客人退回时瓶身碎裂");

        mockMvc.perform(get("/api/v1/inventory/high-value-reviews").with(supervisor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].subject_id").value(bottle))
                .andExpect(jsonPath("$.data.items[0].subject_summary.reason").value("客人退回时瓶身碎裂"));
    }

    // 第十八批 AY 段 269-R2（第十八批裁决 14「高价值的……存酒作废，必须照片＋视频＋主管复核才生效」；PGM:287 报错全中文）：前端不再预检照片视频，
    // 店员只带原因作废高价值存酒 → 400 HIGH_VALUE_EVIDENCE_REQUIRED，报错是全中文（不夹带 photo_attachment_ids），存酒不动、不建复核单。
    @Test
    void highValueCancelWithoutEvidenceRejectsWithChineseMessage() throws Exception {
        String bottle = insertBottle(highItem, "stored");

        call(clerk, bottle + "/cancel", body("b71-269-cancel-noevidence-" + s, "交还时瓶身碎裂"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("HIGH_VALUE_EVIDENCE_REQUIRED"))
                .andExpect(jsonPath("$.message").value("照片附件不能为空：高价值动作需照片+视频双证据"));
        assertThat(statusOf(bottle)).isEqualTo("stored");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM inventory_high_value_reviews WHERE subject_id=?", Long.class, bottle)).isZero();
    }

    // 第十八批 AY 段 269-R1（BSM:1072「存酒作废需照片+视频双证据+主管复核」）：存酒页改为真实上传后，店员按页面形状经上传接口
    // （business_type=saved_bottle_cancel，attachment_role 照片 photo、视频 video）拿附件编号，再用这两个编号作废高价值存酒 → 冻结待复核，
    // 复核单记的照片、视频编号与上传一致。后端未改，本条锁现行为。
    @Test
    void highValueCancelWithUploadedPhotoAndVideoFreezesForReview() throws Exception {
        String bottle = insertBottle(highItem, "stored");
        String photo = uploadViaHttp("broken.jpg", "image/jpeg", "photo");
        String video = uploadViaHttp("broken.mp4", "video/mp4", "video");
        Map<String, Object> cancel = body("b71-269-cancel-hv-" + s, "交还时瓶身碎裂");
        cancel.put("photo_attachment_ids", List.of(photo));
        cancel.put("video_attachment_ids", List.of(video));

        call(clerk, bottle + "/cancel", cancel)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("disputed_frozen"));
        assertThat(jdbc.queryForMap("SELECT photo_attachment_ids::text AS photos, video_attachment_ids::text AS videos"
                        + " FROM inventory_high_value_reviews WHERE subject_id=?", bottle))
                .containsEntry("photos", "[\"" + photo + "\"]")
                .containsEntry("videos", "[\"" + video + "\"]");
    }

    // 254-R6：争议原因必填并进审计；店员、别家商户主管不能了结；门店主管核实争议不成立 → 恢复存酒中，同号重放返回首次结果，随后照常取酒。
    @Test
    void disputeReasonAuditedAndSupervisorDismissalRestoresClaimableBottle() throws Exception {
        String bottle = insertBottle(lowItem, "stored");

        call(clerk, bottle + "/dispute", body("b62sd-dispute-noreason-" + s, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(statusOf(bottle)).isEqualTo("stored");

        call(clerk, bottle + "/dispute", body("b62sd-dispute-" + s, "客人质疑剩余量"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("disputed"));
        assertThat(auditReason("bar.saved_bottle.dispute", bottle)).isEqualTo("客人质疑剩余量");

        call(clerk, bottle + "/dispute-dismissal", body("b62sd-dismiss-clerk-" + s, null))
                .andExpect(status().isForbidden());
        call(otherSupervisor, bottle + "/dispute-dismissal", body("b62sd-dismiss-other-" + s, null))
                .andExpect(status().isForbidden());
        assertThat(statusOf(bottle)).isEqualTo("disputed");

        String key = "b62sd-dismiss-" + s;
        call(supervisor, bottle + "/dispute-dismissal", body(key, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("stored"));
        call(supervisor, bottle + "/dispute-dismissal", body(key, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(bottle))
                .andExpect(jsonPath("$.data.status").value("stored"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='bar.saved_bottle.dispute_dismissed'"
                + " AND event_result='success' AND business_id=?", Long.class, bottle)).isEqualTo(1L);

        Map<String, Object> claim = new LinkedHashMap<>();
        claim.put("idempotency_key", "b62sd-claim-" + s);
        claim.put("verification_code", "000000");
        claim.put("bottle_label_value", "LBL-B62SD");
        claim.put("customer_name_or_nickname", "争议客人");
        claim.put("claim_items", List.of(Map.of("quantity", 1)));
        call(clerk, bottle + "/claim", claim)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("picked_up"));
        assertThat(jdbc.queryForObject("SELECT remaining_quantity FROM saved_bottles WHERE id=?", Integer.class, bottle)).isEqualTo(1);
    }

    // 254-R6：争议成立转作废须门店主管码——店员 403、状态不变；门店主管低价值作废 → cancelled，原因落库。
    @Test
    void disputedBottleCancelNeedsStoreSupervisor() throws Exception {
        String bottle = insertBottle(lowItem, "stored");
        call(clerk, bottle + "/dispute", body("b62sd-dispute2-" + s, "客人称瓶底标签被改"))
                .andExpect(status().isOk());

        call(clerk, bottle + "/cancel", body("b62sd-cancel-clerk-" + s, "争议成立"))
                .andExpect(status().isForbidden());
        assertThat(statusOf(bottle)).isEqualTo("disputed");

        call(supervisor, bottle + "/cancel", body("b62sd-cancel-sup-" + s, "核实标签确被改动，争议成立"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("cancelled"));
        assertThat(jdbc.queryForObject("SELECT cancel_reason FROM saved_bottles WHERE id=?", String.class, bottle))
                .isEqualTo("核实标签确被改动，争议成立");
    }

    // 第十八批 BD 段第 361 条 361-R1（BSM:1072「存酒作废需照片+视频双证据+主管复核」；第十八批裁决 14「必须照片＋视频＋主管复核才生效」）：
    // 店员作废高价值存酒冻结待复核，门店主管驳回 → 作废不生效，存酒回到作废前的存酒中，照常可取。
    @Test
    void highValueVoidRejectedRestoresStoredBottle() throws Exception {
        String bottle = insertBottle(highItem, "stored");
        Map<String, Object> cancel = body("b76-361-cancel-" + s, "交还时瓶身碎裂");
        cancel.put("photo_attachment_ids", List.of(uploadViaHttp("broken.jpg", "image/jpeg", "photo")));
        cancel.put("video_attachment_ids", List.of(uploadViaHttp("broken.mp4", "video/mp4", "video")));
        call(clerk, bottle + "/cancel", cancel)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("disputed_frozen"));

        reject(supervisor, bottle, "b76-361-reject-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_status").value("rejected"));
        assertThat(statusOf(bottle)).isEqualTo("stored");
        assertThat(voidPriorStatusOf(bottle)).isEqualTo("stored");

        Map<String, Object> claim = new LinkedHashMap<>();
        claim.put("idempotency_key", "b76-361-claim-" + s);
        claim.put("verification_code", "000000");
        claim.put("bottle_label_value", "LBL-B62SD");
        claim.put("customer_name_or_nickname", "争议客人");
        claim.put("claim_items", List.of(Map.of("quantity", 1)));
        call(clerk, bottle + "/claim", claim)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("picked_up"));
        assertThat(jdbc.queryForObject("SELECT remaining_quantity FROM saved_bottles WHERE id=?", Integer.class, bottle)).isEqualTo(1);
        assertThat(statusOf(bottle)).isEqualTo("stored");
    }

    // 361-R1（第十八批裁决 58「争议成立，走现有存酒作废（高价值的要照片＋视频＋主管复核，裁决 14）」）：争议中的高价值存酒由门店主管转作废，
    // 另一位门店主管驳回 → 回到争议中，仍由门店主管二选一了结。
    @Test
    void disputedHighValueVoidRejectedReturnsToDispute() throws Exception {
        String bottle = insertBottle(highItem, "stored");
        call(clerk, bottle + "/dispute", body("b76-361-dispute-" + s, "客人称瓶底标签被改"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("disputed"));
        Map<String, Object> cancel = body("b76-361-cancel2-" + s, "核实标签确被改动，争议成立");
        cancel.put("photo_attachment_ids", List.of(uploadViaHttp("label.jpg", "image/jpeg", "photo")));
        cancel.put("video_attachment_ids", List.of(uploadViaHttp("label.mp4", "video/mp4", "video")));
        call(supervisor, bottle + "/cancel", cancel)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("disputed_frozen"));

        reject(supervisor2, bottle, "b76-361-reject2-" + s)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_status").value("rejected"));
        assertThat(statusOf(bottle)).isEqualTo("disputed");
        assertThat(voidPriorStatusOf(bottle)).isEqualTo("disputed");

        call(supervisor, bottle + "/dispute-dismissal", body("b76-361-dismiss-" + s, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("stored"));
    }

    private ResultActions reject(RequestPostProcessor who, String bottle, String key) throws Exception {
        String review = jdbc.queryForObject("SELECT id FROM inventory_high_value_reviews WHERE subject_id=?", String.class, bottle);
        Map<String, Object> decision = new LinkedHashMap<>();
        decision.put("idempotency_key", key);
        decision.put("decision", "rejected");
        decision.put("reason", "视频看不清破损");
        return mockMvc.perform(post("/api/v1/inventory/high-value-reviews/" + review + "/review").with(who)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(decision)));
    }

    private String voidPriorStatusOf(String bottle) {
        return jdbc.queryForObject("SELECT void_prior_status FROM saved_bottles WHERE id=?", String.class, bottle);
    }

    private Map<String, Object> body(String key, String reason) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key);
        if (reason != null) {
            body.put("reason", reason);
        }
        return body;
    }

    private ResultActions call(RequestPostProcessor who, String path, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post(BASE + path).with(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    /** 散客存酒（无会员编号），手机号按库内形态落密文与哈希；剩余 2 瓶、30 天后到期。 */
    private String insertBottle(String item, String status) throws Exception {
        String id = "sb_b62sd_" + item.substring(item.lastIndexOf('_') + 1) + "_" + System.nanoTime();
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(phone.getBytes(StandardCharsets.UTF_8)));
        jdbc.update("""
                INSERT INTO saved_bottles (
                    id, customer_id, bar_project_id, item_id, bottle_name, initial_quantity, remaining_quantity, unit, expire_at,
                    status, evidence_attachment_ids, created_at, updated_at, is_high_value, bottle_label_value,
                    customer_name_or_nickname, customer_phone, customer_phone_hash, registration_photo_refs, manual_note, merchant_id
                )
                VALUES (?, NULL, ?, ?, '存酒争议酒', 2, 2, '瓶', ?, ?, '[]'::jsonb, now(), now(), false, 'LBL-B62SD',
                        '争议客人', ?, ?, '["reg_photo_b62sd"]'::jsonb, '吧台登记', ?)
                """, id, merchant, item, Timestamp.from(Instant.now().plusSeconds(30L * 86400)), status,
                cipher.encrypt(phone), hash, merchant);
        return id;
    }

    private String statusOf(String bottle) {
        return jdbc.queryForObject("SELECT status FROM saved_bottles WHERE id=?", String.class, bottle);
    }

    private String auditReason(String event, String bottle) {
        return jdbc.queryForObject("SELECT metadata->>'reason' FROM audit_events WHERE event_name=? AND event_result='success'"
                + " AND business_id=?", String.class, event, bottle);
    }

    private void arrival(String item, long unitCostCent) {
        inventoryRepository.saveStockMovement(new StockMovementRecord("mv_b62sd_" + item, "inbound", item, null, "loc_b62sd_" + s, 1,
                unitCostCent, null, "purchase_request", "pr_" + item, null, null, List.of(), "completed",
                Instant.now().minusSeconds(60), merchant), project);
    }

    private String uploadViaHttp(String name, String contentType, String role) throws Exception {
        String response = mockMvc.perform(multipart("/api/v1/evidence/attachments/upload")
                        .file(new MockMultipartFile("file", name, contentType, "fake-bytes".getBytes()))
                        .param("business_type", "saved_bottle_cancel").param("attachment_role", role).with(clerk))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.data.id");
    }

    private String upload(String name, String contentType, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(clerkId, clerkId, "staff", project, merchant, "active"), "test", List.of()));
        try {
            return evidence.uploadAttachment(new MockMultipartFile("file", name, contentType, "fake-bytes".getBytes()),
                    "inventory_stock_adjustment", null, role).id();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
