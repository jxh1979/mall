package com.complex.platform.inventory;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.evidence.EvidenceEventService;
import com.complex.platform.governance.review.SupervisorReviewApprovalRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * FS-019 不可消费物品借用真库、真接口回归，请求体一律按前端真实形状（蛇形键）。
 * BSM L1201「「借」只转移保管责任、不转移所有权（仅适用于不可消费物品……）——借出方始终是这批货的所有权人」
 * 「不可消费物品（设备、器具等）的借用必须约定归还时间，借用期间保管人变更为借入方并在溯源码上登记」
 * 「实际归还时间取借出方收货扫码的时刻」「结束对应数量的原物归还事项」「不得伪造归还扫码或实际归还时间」
 * 「不可消费借用物无法归还时……由借出所有权方与借入保管方确认原因、数量和实物处置，并经主管及财务复核；赔偿……形成借入方对借出方的应付。
 * 双方确认的实物处置实际完成后，结束对应数量的原物归还事项；未付赔偿款单独跟进直到实际清偿」；ENUM L166；台账 §25；DEC-R3-38；批准 13｜Q18。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class InventoryTransferBorrowJdbcIntegrationTests {

    private static final String BASE = "/api/v1/inventory/transfer-orders";
    private static final String DUE = "2026-12-31T10:00:00Z";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SupervisorReviewService reviews;

    @Autowired
    private EvidenceEventService evidence;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "fs019_prj_" + suffix;
    private final String lender = "fs019_lender_" + suffix;
    private final String borrower = "fs019_borrower_" + suffix;
    private final String third = "fs019_third_" + suffix;
    private final String device = "fs019_device_" + suffix;
    private final String liquor = "fs019_liquor_" + suffix;
    private final String codeA = "FS019A" + suffix;
    private final String codeB = "FS019B" + suffix;
    private RequestPostProcessor admin;
    private RequestPostProcessor lenderStaff;
    private RequestPostProcessor borrowerStaff;
    private RequestPostProcessor borrowerColleague;
    private RequestPostProcessor thirdStaff;

    @BeforeEach
    void seed() {
        String entity = "fs019_ent_" + suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','借用回归项目','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'项目主体','project_company','active',now(),now())", entity);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", entity, project);
        for (String m : List.of(lender, borrower, third)) {
            jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,project_id,status,created_at,updated_at) VALUES(?,?,'retail',?,?,'active',now(),now())", m, m, entity, project);
            jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,lifecycle_status,allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,'effective','[\"inventory\",\"finance\"]'::jsonb,now(),now())", "fs019_ct_" + m, m, entity, entity);
        }
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at,non_consumable) VALUES(?,'舞台音响','equipment','台',0,'active',?,now(),now(),TRUE)", device, lender);
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at) VALUES(?,'威士忌','liquor','瓶',0,'active',?,now(),now())", liquor, lender);
        for (String code : List.of(codeA, codeB)) {
            jdbc.update("INSERT INTO inventory_trace_codes(id,trace_code,item_id,merchant_id,owner_type,owner_id) VALUES(?,?,?,?,'merchant',?)", "tc_" + code, code, device, lender, lender);
        }
        String role = "fs019_role_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'借用回归','project','active',now())", role, role);
        jdbc.update("""
                INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN(
                'inventory.transfer_order.manage','inventory.transfer_order.view','governance.supervisor_review.view','governance.supervisor_review.approve',
                'finance.ar_ap.manage','evidence.attachment.create')
                """, role);
        admin = account("admin", "platform_admin", "org", null, role);
        lenderStaff = account("lender", "staff", project, lender, role);
        account("lender_boss", "staff", project, lender, role);
        borrowerStaff = account("borrower", "staff", project, borrower, role);
        borrowerColleague = account("borrower2", "staff", project, borrower, role);
        account("borrower_boss", "staff", project, borrower, role);
        thirdStaff = account("third", "staff", project, third, role);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", user("lender"), user("lender_boss"));
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", user("borrower"), user("borrower_boss"));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // R1：借用必须约定归还时间；可消费物品不存在保留所有权的借用（只能买断）。
    @Test
    void borrowOrderRequiresAgreedReturnTimeAndNonConsumableItem() throws Exception {
        mockMvc.perform(post(BASE).with(admin).contentType(MediaType.APPLICATION_JSON).content(borrowBody(device, 2, List.of(codeA, codeB), null)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        mockMvc.perform(post(BASE).with(admin).contentType(MediaType.APPLICATION_JSON).content(borrowBody(liquor, 2, List.of(), DUE)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(count("SELECT count(*) FROM inventory_transfer_orders WHERE from_owner_id=?", lender)).isZero();

        String id = createBorrow(2, List.of(codeA, codeB));
        assertThat(jdbc.queryForObject("SELECT return_due_at IS NOT NULL FROM inventory_transfer_orders WHERE id=?", Boolean.class, id)).isTrue();
    }

    // R1：借出方交货扫码（借出时间）、借入方收货扫码（保管人变更时间）各记一行保管台账；收货后所有权仍是借出方、保管主体为借入方、码挂在本借用单上。
    @Test
    void borrowReceiptChangesCustodianOnlyAndKeepsLenderAsOwner() throws Exception {
        String id = createBorrow(2, List.of(codeA, codeB));
        call(admin, id, "ship", "{\"scanned_trace_codes\":[\"" + codeA + "\"]}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("TRACE_CODE_INCOMPLETE"));
        step(admin, id, "ship", "{\"scanned_trace_codes\":[\"" + codeA + "\",\"" + codeB + "\"]}", "shipped");
        step(borrowerStaff, id, "receive", "{\"scanned_trace_codes\":[\"" + codeA + "\",\"" + codeB + "\"]}", "received");

        Map<String, Object> code = jdbc.queryForMap("SELECT owner_id,custodian_id,lending_order_id FROM inventory_trace_codes WHERE trace_code=?", codeA);
        assertThat(code).containsEntry("owner_id", lender).containsEntry("custodian_id", borrower).containsEntry("lending_order_id", id);
        List<Map<String, Object>> history = jdbc.queryForList(
                "SELECT change_kind,from_owner_id,to_owner_id FROM inventory_trace_ownership_history WHERE trace_code=? ORDER BY changed_at,id", codeA);
        assertThat(history).extracting(h -> h.get("change_kind")).containsExactly("custody", "custody");
        assertThat(history).extracting(h -> h.get("to_owner_id")).containsExactly(lender, borrower);
        assertThat(count("SELECT count(*) FROM ar_ap_items WHERE source_type='inventory_transfer' AND source_id=?", id)).isZero();
    }

    // R2：借出方扫 1 码归还只结束 1 件（单据仍 received、另一码仍在借入方保管）；再归还第 2 码才 returned；归还登记记借出方账号与扫码时刻。
    @Test
    void partialReturnClosesOnlyScannedQuantityAndStampsLenderScanTime() throws Exception {
        String id = lentOut();
        call(lenderStaff, id, "return", "{\"lines\":[{\"line_id\":\"" + id + "_l0\",\"returned_quantity\":1,\"trace_codes\":[\"" + codeA + "\"]}]}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("received"));

        assertThat(custodian(codeA)).isEqualTo(lender);
        assertThat(custodian(codeB)).isEqualTo(borrower);
        assertThat(jdbc.queryForObject("SELECT lending_order_id FROM inventory_trace_codes WHERE trace_code=?", String.class, codeA)).isNull();
        Map<String, Object> returned = jdbc.queryForMap("SELECT returned_quantity,received_by,received_at FROM inventory_transfer_returns WHERE transfer_order_id=?", id);
        assertThat(returned).containsEntry("returned_quantity", 1).containsEntry("received_by", user("lender"));
        assertThat(returned.get("received_at")).isNotNull();
        assertThat(jdbc.queryForMap("SELECT change_kind,to_owner_id FROM inventory_trace_ownership_history WHERE trace_code=? ORDER BY changed_at DESC,id DESC LIMIT 1", codeA))
                .containsEntry("change_kind", "custody").containsEntry("to_owner_id", lender);
        assertThat(owner(codeA)).isEqualTo(lender);
        assertThat(owner(codeB)).isEqualTo(lender);

        call(lenderStaff, id, "return", "{\"lines\":[{\"line_id\":\"" + id + "_l0\",\"returned_quantity\":1,\"trace_codes\":[\"" + codeB + "\"]}]}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("returned"));
        assertThat(custodian(codeB)).isEqualTo(lender);
        assertThat(jdbc.queryForMap("SELECT from_status,operator_id FROM inventory_transfer_order_events WHERE transfer_order_id=? AND to_status='returned'", id))
                .containsEntry("from_status", "received").containsEntry("operator_id", user("lender"));
    }

    // R2：归还只认借出方收货扫码——借入方账号不能登记归还；不带扫码也不能归还。
    @Test
    void borrowerAccountCannotRecordTheReturn() throws Exception {
        String id = lentOut();
        call(borrowerStaff, id, "return", "{\"lines\":[{\"line_id\":\"" + id + "_l0\",\"returned_quantity\":2,\"trace_codes\":[\"" + codeA + "\",\"" + codeB + "\"]}]}")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("LENDER_RETURN_ONLY"));
        call(lenderStaff, id, "return", "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("TRANSFER_RETURN_REQUIRES_SCAN"));
        assertThat(orderStatus(id)).isEqualTo("received");
        assertThat(custodian(codeA)).isEqualTo(borrower);
    }

    // R2：扫入非本单码、或码数与数量不符都拒绝。
    @Test
    void returnRejectsCodesNotOnTheOrder() throws Exception {
        String id = lentOut();
        call(lenderStaff, id, "return", "{\"lines\":[{\"line_id\":\"" + id + "_l0\",\"returned_quantity\":1,\"trace_codes\":[\"OTHER" + suffix + "\"]}]}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("TRACE_CODE_MISMATCH"));
        call(lenderStaff, id, "return", "{\"lines\":[{\"line_id\":\"" + id + "_l0\",\"returned_quantity\":2,\"trace_codes\":[\"" + codeA + "\"]}]}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("TRACE_CODE_MISMATCH"));
        assertThat(count("SELECT count(*) FROM inventory_transfer_returns WHERE transfer_order_id=?", id)).isZero();
    }

    // R3：借入方登记无法归还（附证据）→ 借出方确认并挂主管复核 → 主管通过进财务复核 → 财务通过形成借入方对借出方应付（独立跟进）→
    //     借出方登记实物处置完成才结束该件的归还事项；另一件已归还，于是借用单 returned；应付仍待付。所有权始终是借出方。
    @Test
    void unreturnableNeedsBothPartiesSupervisorAndFinanceBeforePayableAndClosure() throws Exception {
        String id = lentOut();
        call(lenderStaff, id, "return", "{\"lines\":[{\"line_id\":\"" + id + "_l0\",\"returned_quantity\":1,\"trace_codes\":[\"" + codeA + "\"]}]}")
                .andExpect(status().isOk());

        String disposition = registerUnreturnable(id, borrowerStaff, "borrower", 1, List.of(codeB), null);
        assertThat(orderStatus(id)).isEqualTo("received");

        call(lenderStaff, id, "dispositions/" + disposition + "/confirm", "{\"operator_note\":\"音响摔坏，同意报废处置\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("pending_supervisor"));
        Map<String, Object> confirmed = jdbc.queryForMap("SELECT confirmed_by,supervisor_review_id FROM inventory_transfer_dispositions WHERE id=?", disposition);
        assertThat(confirmed).containsEntry("confirmed_by", user("lender"));
        assertThat(confirmed.get("supervisor_review_id")).isNotNull();
        call(admin, id, "dispositions/" + disposition + "/review", "{\"decision\":\"approved\",\"reason\":\"提前复核\"}").andExpect(status().isConflict());

        decide((String) confirmed.get("supervisor_review_id"), "lender_boss", "approved");
        assertThat(dispositionStatus(disposition)).isEqualTo("pending_review");

        call(admin, id, "dispositions/" + disposition + "/review", "{\"decision\":\"approved\",\"reason\":\"按内部结算价赔偿\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("approved"));
        String payableId = jdbc.queryForObject("SELECT payable_item_id FROM inventory_transfer_dispositions WHERE id=?", String.class, disposition);
        assertThat(jdbc.queryForMap("SELECT * FROM ar_ap_items WHERE id=?", payableId)).containsEntry("direction", "payable")
                .containsEntry("merchant_id", borrower).containsEntry("counterparty_id", lender).containsEntry("amount_cent", 80000L)
                .containsEntry("unpaid_amount_cent", 80000L).containsEntry("status", "pending_payment").containsEntry("project_id", project);
        assertThat(orderStatus(id)).isEqualTo("received");
        assertThat(custodian(codeB)).isEqualTo(borrower);

        call(borrowerStaff, id, "dispositions/" + disposition + "/disposal-completed", "{\"operator_note\":\"已送废品站\"}")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("LENDER_DISPOSAL_ONLY"));
        call(lenderStaff, id, "dispositions/" + disposition + "/disposal-completed", "{\"operator_note\":\"已拆解报废，留照片\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("disposal_completed"));

        assertThat(custodian(codeB)).isEqualTo(lender);
        assertThat(owner(codeB)).isEqualTo(lender);
        assertThat(orderStatus(id)).isEqualTo("returned");
        assertThat(jdbc.queryForObject("SELECT status FROM ar_ap_items WHERE id=?", String.class, payableId)).isEqualTo("pending_payment");
        assertThat(count("SELECT count(*) FROM inventory_transfer_returns WHERE transfer_order_id=?", id)).isEqualTo(1);
    }

    // R3：双方确认＝另一方商户确认，登记方同商户同事与第三方商户都不能确认。
    @Test
    void counterpartyConfirmMustComeFromTheOtherMerchant() throws Exception {
        String id = lentOut();
        String disposition = registerUnreturnable(id, borrowerStaff, "borrower", 1, List.of(codeB), null);
        call(borrowerColleague, id, "dispositions/" + disposition + "/confirm", "{}").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("COUNTERPARTY_CONFIRM_ONLY"));
        call(thirdStaff, id, "dispositions/" + disposition + "/confirm", "{}").andExpect(status().isForbidden());
        assertThat(dispositionStatus(disposition)).isEqualTo("pending_counterparty");
    }

    // R3：无法归还数量不超过尚未归还数；登记中的件不能再被归还。
    @Test
    void unreturnableQuantityCannotExceedOutstanding() throws Exception {
        String id = lentOut();
        call(lenderStaff, id, "return", "{\"lines\":[{\"line_id\":\"" + id + "_l0\",\"returned_quantity\":1,\"trace_codes\":[\"" + codeA + "\"]}]}")
                .andExpect(status().isOk());
        call(borrowerStaff, id, "unreturnable", unreturnableBody(id, "borrower", 2, List.of(codeA, codeB), null))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("TRANSFER_DISPOSITION_EXCEEDS_DISCREPANCY"));
        registerUnreturnable(id, borrowerStaff, "borrower", 1, List.of(codeB), null);
        call(lenderStaff, id, "return", "{\"lines\":[{\"line_id\":\"" + id + "_l0\",\"returned_quantity\":1,\"trace_codes\":[\"" + codeB + "\"]}]}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("TRANSFER_RETURN_EXCEEDS_OUTSTANDING"));
    }

    // 2026-10-20 操作员裁决「不得低于内部结算价」：赔偿金额≥数量×建单锁定内部结算价（缺省即该值），低于（含 0）400；缺省登记→复核通过一律挂应付。
    @Test
    void unreturnableAmountMustNotBeBelowLockedSettlementPriceAndDefaultPostsPayable() throws Exception {
        String id = lentOut();
        call(borrowerStaff, id, "unreturnable", unreturnableBody(id, "borrower", 2, List.of(codeA, codeB), 159999L))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("赔偿不得低于内部结算价")));
        call(borrowerStaff, id, "unreturnable", unreturnableBody(id, "borrower", 1, List.of(codeB), 0L))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(count("SELECT count(*) FROM inventory_transfer_dispositions WHERE transfer_order_id=?", id)).isZero();

        // 「已有约定」只能往高约定：高于锁定价可登记。
        String higher = registerUnreturnable(id, borrowerStaff, "borrower", 1, List.of(codeA), 90000L);
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM inventory_transfer_dispositions WHERE id=?", Long.class, higher)).isEqualTo(90000L);
        // 缺省＝数量×锁定价，复核通过挂借入方对借出方应付。
        String disposition = registerUnreturnable(id, borrowerStaff, "borrower", 1, List.of(codeB), null);
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM inventory_transfer_dispositions WHERE id=?", Long.class, disposition)).isEqualTo(80000L);
        call(lenderStaff, id, "dispositions/" + disposition + "/confirm", "{}").andExpect(status().isOk());
        decide(jdbc.queryForObject("SELECT supervisor_review_id FROM inventory_transfer_dispositions WHERE id=?", String.class, disposition), "lender_boss", "approved");
        call(admin, id, "dispositions/" + disposition + "/review", "{\"decision\":\"approved\",\"reason\":\"按内部结算价赔偿\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payable_item_id").isNotEmpty());
        String payableId = jdbc.queryForObject("SELECT payable_item_id FROM inventory_transfer_dispositions WHERE id=?", String.class, disposition);
        assertThat(jdbc.queryForMap("SELECT * FROM ar_ap_items WHERE id=?", payableId)).containsEntry("merchant_id", borrower)
                .containsEntry("counterparty_id", lender).containsEntry("amount_cent", 80000L).containsEntry("status", "pending_payment");
    }

    // R3：财务驳回不动账、单据仍开着，该件回到待归还（约定金额高于锁定价 2×80000）。
    @Test
    void financeReviewRejectedKeepsOrderOpenAndPostsNoMoney() throws Exception {
        String id = lentOut();
        String disposition = registerUnreturnable(id, lenderStaff, "lender", 2, List.of(codeA, codeB), 200000L);
        call(borrowerStaff, id, "dispositions/" + disposition + "/confirm", "{}").andExpect(status().isOk());
        decide(jdbc.queryForObject("SELECT supervisor_review_id FROM inventory_transfer_dispositions WHERE id=?", String.class, disposition), "borrower_boss", "approved");
        call(admin, id, "dispositions/" + disposition + "/review", "{\"decision\":\"rejected\",\"reason\":\"赔偿金额与约定不符\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("rejected"));

        assertThat(count("SELECT count(*) FROM ar_ap_items WHERE merchant_id=? AND counterparty_id=?", borrower, lender)).isZero();
        assertThat(orderStatus(id)).isEqualTo("received");
        assertThat(custodian(codeA)).isEqualTo(borrower);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(BASE + "/" + id + "/dispositions").with(lenderStaff))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.lines[0].outstanding_quantity").value(2));
    }

    // ── 夹具 ──

    private String borrowBody(String itemId, int quantity, List<String> codes, String due) {
        return """
                {"idempotency_key":"%s","transfer_type":"lending","lending_mode":"borrow","from_owner_type":"merchant","from_owner_id":"%s",
                 "to_owner_type":"merchant","to_owner_id":"%s",%s
                 "lines":[{"item_id":"%s","quantity":%d,"unit_price_cent":80000,"trace_codes":%s}]}
                """.formatted(key(), lender, borrower, due == null ? "" : "\"return_due_at\":\"" + due + "\",", itemId, quantity, json(codes));
    }

    private String createBorrow(int quantity, List<String> codes) throws Exception {
        return JsonPath.read(mockMvc.perform(post(BASE).with(admin).contentType(MediaType.APPLICATION_JSON).content(borrowBody(device, quantity, codes, DUE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("created"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    /** 两码借用单：建单→借出方交货扫码→借入方收货扫码。 */
    private String lentOut() throws Exception {
        String id = createBorrow(2, List.of(codeA, codeB));
        step(lenderStaff, id, "ship", "{\"scanned_trace_codes\":[\"" + codeA + "\",\"" + codeB + "\"]}", "shipped");
        step(borrowerStaff, id, "receive", "{\"scanned_trace_codes\":[\"" + codeA + "\",\"" + codeB + "\"]}", "received");
        return id;
    }

    private String registerUnreturnable(String id, RequestPostProcessor who, String tag, int quantity, List<String> codes, Long amountCent) throws Exception {
        return JsonPath.read(call(who, id, "unreturnable", unreturnableBody(id, tag, quantity, codes, amountCent))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("pending_counterparty"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    private String unreturnableBody(String id, String tag, int quantity, List<String> codes, Long amountCent) {
        return "{\"line_id\":\"" + id + "_l0\",\"quantity\":" + quantity + ",\"trace_codes\":" + json(codes)
                + (amountCent == null ? "" : ",\"amount_cent\":" + amountCent)
                + ",\"reason\":\"演出中摔坏\",\"disposal_note\":\"借出方收回残件报废\",\"evidence_attachment_ids\":[\"" + upload(id, tag) + "\"]}";
    }

    private void step(RequestPostProcessor who, String id, String action, String body, String expectedStatus) throws Exception {
        call(who, id, action, body).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(expectedStatus));
    }

    /** 补上幂等键后按蛇形键原样提交。 */
    private ResultActions call(RequestPostProcessor who, String id, String action, String body) throws Exception {
        String withKey = "{\"idempotency_key\":\"" + key() + "\"" + (body.equals("{}") ? "}" : "," + body.substring(1));
        return mockMvc.perform(post(BASE + "/" + id + "/" + action).with(who).contentType(MediaType.APPLICATION_JSON).content(withKey));
    }

    private String upload(String orderId, String tag) {
        String userId = user(tag);
        String merchant = "lender".equals(tag) ? lender : borrower;
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(userId, userId, "staff", project, merchant, "active"), "test", List.of()));
        try {
            return evidence.uploadAttachment(new MockMultipartFile("file", "damage.png", "image/png",
                    Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jCYQAAAAASUVORK5CYII=")),
                    "inventory_transfer_order", orderId, "other").id();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void decide(String review, String approverTag, String decision) {
        String userId = user(approverTag);
        String merchant = approverTag.startsWith("lender") ? lender : borrower;
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(userId, userId, "staff", project, merchant, "active"), "test", List.of()));
        reviews.approve(review, new SupervisorReviewApprovalRequest(key(), "核对双方确认的原因与处置", List.of(), null, decision, "主管裁决"));
        SecurityContextHolder.clearContext();
    }

    private RequestPostProcessor account(String tag, String accountType, String organizationId, String merchantId, String role) {
        String userId = user(tag);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,?,?,?,'active',now())",
                userId, userId, accountType, organizationId, merchantId);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId, organizationId);
        AccessActor actor = new AccessActor(userId, userId, accountType, organizationId, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private String user(String tag) {
        return "fs019_" + tag + "_" + suffix;
    }

    private String orderStatus(String id) {
        return jdbc.queryForObject("SELECT status FROM inventory_transfer_orders WHERE id=?", String.class, id);
    }

    private String dispositionStatus(String id) {
        return jdbc.queryForObject("SELECT status FROM inventory_transfer_dispositions WHERE id=?", String.class, id);
    }

    private String owner(String code) {
        return jdbc.queryForObject("SELECT owner_id FROM inventory_trace_codes WHERE trace_code=?", String.class, code);
    }

    private String custodian(String code) {
        return jdbc.queryForObject("SELECT custodian_id FROM inventory_trace_codes WHERE trace_code=?", String.class, code);
    }

    private long count(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    private static String json(List<String> values) {
        return values.isEmpty() ? "[]" : "[\"" + String.join("\",\"", values) + "\"]";
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }
}
