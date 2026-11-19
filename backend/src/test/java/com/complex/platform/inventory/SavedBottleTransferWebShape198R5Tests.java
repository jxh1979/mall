package com.complex.platform.inventory;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AF 段第 198 条 198-R5（存酒）：契约删去 SavedBottleRequest.operator_note、SavedBottleTransferRequest.operator_note（代码不读；
 * 后台存酒页 StoredWinePage 转让停发 operator_note）后，门店员工按新形状走真实 HTTP、真实授权链：先按契约删后形状登记存酒，
 * 再按存酒页转让的真实请求形状（idempotency_key、evidence_attachment_ids、to_name_or_nickname、to_phone、verification_code）转让。
 * 入口未改，本类只锁现行为，不先红。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SavedBottleTransferWebShape198R5Tests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = String.valueOf(System.nanoTime());
    private final String merchant = "bar_b52sw_" + s;
    private final String item = "item_b52sw_" + s;
    private final String staffId = "usr_b52sw_" + s;
    private RequestPostProcessor staff;

    @BeforeEach
    void seed() {
        String project = "prj_b52sw_" + s;
        String entity = "ent_b52sw_" + s;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','存酒形状综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'存酒形状主体','project_company','active',now(),now())", entity);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", entity, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,project_id,status,created_at,updated_at)"
                + " VALUES(?,'存酒形状酒吧','bar',?,?,'active',now(),now())", merchant, entity, project);
        // 转让的准入闸 inventory.saved_bottle_claim 按合同经营范围判（V099 over_contract_scope）：给本酒吧一份含 inventory 的生效合同。
        jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,lifecycle_status,"
                + "allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,'effective','[\"inventory\"]'::jsonb,now(),now())",
                "ct_b52sw_" + s, merchant, entity, entity);
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at)"
                + " VALUES(?,'存酒形状威士忌','liquor','瓶',0,'active',?,now(),now())", item, merchant);
        staff = new StaffRequestFixture(jdbc).staff(staffId, "staff", project, merchant, "bar.saved_bottle.manage");
    }

    // 登记（不带 operator_note）与转让（存酒页形状，不带 operator_note）：都 200，持有人换成受让人，转让记录落操作人与整瓶数量。
    @Test
    void transferSavedBottle_webShapeWithoutOperatorNote_ok() throws Exception {
        String digits = s.substring(s.length() - 8);
        Map<String, Object> create = new LinkedHashMap<>();
        create.put("idempotency_key", "b52sw-create-" + s);
        create.put("bar_id", merchant);
        create.put("items", List.of(Map.of("item_id", item, "quantity", 2, "bottle_name", "存酒形状威士忌", "unit", "bottle")));
        create.put("bottle_label_value", "LBL-B52SW");
        create.put("customer_name_or_nickname", "出让人");
        create.put("customer_phone", "155" + digits);
        create.put("registration_photo_refs", List.of("reg_photo_b52sw"));
        // 第十八批 AP 段 254-R1、254-R4：登记人工备注必填、客人报隐私同意短信验证码（测试环境固定码）；254-R2：到期日服务端固化，不再发。
        create.put("manual_note", "吧台登记");
        create.put("verification_code", "000000");
        String bottleId = JsonPath.read(call("/api/v1/inventory/saved-bottles", create).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.data.id");

        Map<String, Object> transfer = new LinkedHashMap<>();
        transfer.put("idempotency_key", "admin-saved-bottle-transfer-" + bottleId + "-156" + digits);
        transfer.put("evidence_attachment_ids", List.of());
        transfer.put("to_name_or_nickname", "受让人");
        transfer.put("to_phone", "156" + digits);
        transfer.put("verification_code", "000000");
        call("/api/v1/inventory/saved-bottles/" + bottleId + "/transfer", transfer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.saved_bottle_id").value(bottleId))
                .andExpect(jsonPath("$.data.status").value("transferred"));

        assertThat(jdbc.queryForObject("SELECT customer_name_or_nickname FROM saved_bottles WHERE id = ?", String.class, bottleId))
                .isEqualTo("受让人");
        assertThat(jdbc.queryForMap("SELECT to_name_or_nickname, operator_id, transfer_quantity"
                        + " FROM saved_bottle_transfers WHERE saved_bottle_id = ?", bottleId))
                .containsEntry("to_name_or_nickname", "受让人")
                .containsEntry("operator_id", staffId)
                .containsEntry("transfer_quantity", 2);
    }

    // 第十八批 AY 段 268-R1：按存酒页领取的真实形状（claim_items 只带 saved_bottle_id 与 quantity，不带瓶底标签与姓名）领取，
    // 核验短信验证码通过即出酒（BSM:1073 只要求核验验证码或动态取酒码，标签只留痕）；同一瓶分次取用两次都出酒，不转待核身复核。
    @Test
    void claimSavedBottle_webShapeWithoutLabelAndName_dispensesTwice() throws Exception {
        String digits = s.substring(s.length() - 8);
        Map<String, Object> create = new LinkedHashMap<>();
        create.put("idempotency_key", "b71-268-create-" + s);
        create.put("bar_id", merchant);
        create.put("items", List.of(Map.of("item_id", item, "quantity", 2, "bottle_name", "存酒形状威士忌", "unit", "bottle")));
        create.put("bottle_label_value", "LBL-268");
        create.put("customer_name_or_nickname", "取酒人");
        create.put("customer_phone", "157" + digits);
        create.put("registration_photo_refs", List.of("reg_photo_b71268"));
        create.put("manual_note", "吧台登记");
        create.put("verification_code", "000000");
        String bottleId = JsonPath.read(call("/api/v1/inventory/saved-bottles", create).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.data.id");

        Map<String, Object> first = new LinkedHashMap<>();
        first.put("idempotency_key", "admin-saved-bottle-claim-" + bottleId + "-" + System.nanoTime());
        first.put("claim_items", List.of(Map.of("saved_bottle_id", bottleId, "quantity", 1)));
        first.put("verification_code", "000000");
        call("/api/v1/inventory/saved-bottles/" + bottleId + "/claim", first)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("picked_up"))
                .andExpect(jsonPath("$.data.claim_quantity").value(1))
                .andExpect(jsonPath("$.data.remaining_before").value(2))
                .andExpect(jsonPath("$.data.remaining_after").value(1))
                .andExpect(jsonPath("$.data.verification_method").value("sms"));
        assertThat(jdbc.queryForMap("SELECT status, remaining_quantity FROM saved_bottles WHERE id = ?", bottleId))
                .containsEntry("status", "stored")
                .containsEntry("remaining_quantity", 1);

        Map<String, Object> second = new LinkedHashMap<>();
        second.put("idempotency_key", "admin-saved-bottle-claim-" + bottleId + "-" + System.nanoTime());
        second.put("claim_items", List.of(Map.of("saved_bottle_id", bottleId, "quantity", 1)));
        second.put("verification_code", "000000");
        call("/api/v1/inventory/saved-bottles/" + bottleId + "/claim", second)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.remaining_after").value(0));
        assertThat(jdbc.queryForObject("SELECT status FROM saved_bottles WHERE id = ?", String.class, bottleId))
                .isEqualTo("claimed");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM saved_bottle_claims WHERE saved_bottle_id = ?", Integer.class, bottleId))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM saved_bottle_claims WHERE saved_bottle_id = ? AND status = 'pending_identity_review'",
                Integer.class, bottleId)).isZero();
    }

    private ResultActions call(String path, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post(path).with(staff).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }
}
