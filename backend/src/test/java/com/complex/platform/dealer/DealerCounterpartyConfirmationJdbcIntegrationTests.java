package com.complex.platform.dealer;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.StaffLevelAssignment;
import com.complex.platform.dealer.dto.DealerCatalogDecisionRequest;
import com.complex.platform.dealer.dto.DealerCatalogDraftRequest;
import com.complex.platform.dealer.dto.DealerCatalogLineRequest;
import com.complex.platform.dealer.dto.DealerContractRequest;
import com.complex.platform.dealer.dto.DealerContractTermsRequest;
import com.complex.platform.dealer.dto.DealerCreateRequest;
import com.complex.platform.dealer.dto.DealerDeactivateRequest;
import com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest;
import com.complex.platform.dealer.dto.DealerOrderDecisionRequest;
import com.complex.platform.dealer.dto.DealerOrderDraftRequest;
import com.complex.platform.dealer.dto.DealerOrderLineRequest;
import com.complex.platform.dealer.dto.DealerPaymentRequest;
import com.complex.platform.dealer.dto.DealerPaymentVerifyRequest;
import com.complex.platform.dealer.dto.DealerReturnDecisionRequest;
import com.complex.platform.dealer.dto.DealerReturnInspectionLineRequest;
import com.complex.platform.dealer.dto.DealerReturnInspectionRequest;
import com.complex.platform.dealer.dto.DealerReturnLineRequest;
import com.complex.platform.dealer.dto.DealerReturnRequest;
import com.complex.platform.dealer.dto.DealerReviewRequest;
import com.complex.platform.dealer.dto.DealerShipRequest;
import com.complex.platform.evidence.EvidenceEventService;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
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
 * 第十八批第 31 条 经销商停用后的存量事项真库回归。BSM:1247「合同销售主体指定经办人，通过已核验的企业联系人取得签收、退货等确认并留证，
 * 经办人不得代替对方确认……原已成立订单的尾款仍可按原合同，通过已核验的销售主体对公账户回款，由指定经办人和财务核实入账、冲减原应收」；
 * CG 第 67 条「另开 CG 建经办人确认留证对象」。业务入口一律按前端真实请求形状（蛇形键）走真实入口与授权链；
 * 建单、出库等前置事实沿用既有服务调用。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=${COMPLEX_DATABASE_USERNAME:complex}",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class DealerCounterpartyConfirmationJdbcIntegrationTests {

    private static final String PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jz1sAAAAASUVORK5CYII=";

    @Autowired private MockMvc mvc;
    @Autowired private DealerService dealerService;
    @Autowired private DealerOrderService orders;
    @Autowired private DealerDeliveryService delivery;
    @Autowired private DealerReturnService returns;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EvidenceEventService files;
    @Autowired private AccessRepository access;

    private String suffix;
    private String sales;      // 建单 / 合同指定经办人
    private String other;      // 同样持经销全部权限、但不是指定经办人
    private String viewer;     // 只读
    private String reviewer;   // 订单审核 / 退货审批
    private String finance;    // 到账核验 / 超收用途确认
    private String warehouse;  // 出库 / 退货验收
    private String approver;   // 既有审批链上级
    private String salesEntity;
    private String centralWarehouse;
    private String dealerId;
    private String contractId;
    private String itemA;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        sales = "usr_dcc_sales_" + suffix;
        other = "usr_dcc_other_" + suffix;
        viewer = "usr_dcc_view_" + suffix;
        reviewer = "usr_dcc_rev_" + suffix;
        finance = "usr_dcc_fin_" + suffix;
        warehouse = "usr_dcc_wh_" + suffix;
        approver = "usr_dcc_apv_" + suffix;
        String role = "role_dcc_" + suffix;
        jdbc.update("INSERT INTO roles(id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '经销停用存量测试', 'platform', 'active', now())",
                role, "经销停用存量测试角色" + System.nanoTime());
        jdbc.update("INSERT INTO role_permissions(role_id, permission_code) SELECT ?, permission_code FROM permissions WHERE permission_code LIKE 'dealer.%' OR permission_code LIKE 'governance.supervisor_review.%' OR permission_code LIKE 'inventory.%' OR permission_code LIKE 'finance.%' OR permission_code LIKE 'evidence.%' ON CONFLICT DO NOTHING", role);
        for (String user : List.of(sales, other, reviewer, finance, warehouse, approver)) {
            access.createUser(new AccessUser(user, user, "platform_admin", "project_main", null, "active", Instant.now()));
            access.assignUserRole(user, role);
        }
        String viewRole = "role_dcc_view_" + suffix;
        jdbc.update("INSERT INTO roles(id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '经销只读测试', 'platform', 'active', now())",
                viewRole, "经销只读测试角色" + System.nanoTime());
        jdbc.update("INSERT INTO role_permissions(role_id, permission_code) VALUES (?, 'dealer.view'), (?, 'dealer.order.view')", viewRole, viewRole);
        access.createUser(new AccessUser(viewer, viewer, "platform_admin", "project_main", null, "active", Instant.now()));
        access.assignUserRole(viewer, viewRole);
        for (String user : List.of(sales, finance, warehouse, other)) {
            jdbc.update("INSERT INTO org_hierarchy (user_id, superior_id, line_code, updated_by) VALUES (?, ?, 'main', ?)", user, approver, sales);
        }
        access.saveStaffLevelAssignment(new StaffLevelAssignment(approver, "lvl_m3_lower", null, "CNY", null, "project_main", null, sales, Instant.now()));
        access.saveStaffLevelAssignment(new StaffLevelAssignment(finance, "lvl_m3_lower", null, "CNY", null, "project_main", null, sales, Instant.now()));
        salesEntity = "be_dcc_" + suffix;
        jdbc.update("INSERT INTO business_entities(id, entity_name, entity_role, status, created_at, updated_at) VALUES (?, '经销销售主体', 'project_company', 'active', now(), now())", salesEntity);
        centralWarehouse = "wh_dcc_" + suffix;
        jdbc.update("INSERT INTO warehouses(id, project_id, warehouse_type, name, status) VALUES (?, 'project_main', 'main', '经销中心仓', 'active')", centralWarehouse);
        itemA = "item_dcc_a_" + suffix;
        jdbc.update("INSERT INTO inventory_items(id, item_name, item_type, unit, trace_required, min_quantity, status, created_at, updated_at) VALUES (?, '经销威士忌', 'beverage', '箱', false, 2, 'active', now(), now())", itemA);
        jdbc.update("INSERT INTO inventory_stock_balances(id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, warehouse_id, project_id) VALUES (?, ?, ?, 100, 0, 100, now(), ?, 'project_main')",
                "bal_dcc_" + suffix, itemA, centralWarehouse, centralWarehouse);

        actAs(sales);
        DealerRecord dealer = dealerService.createDealer(new DealerCreateRequest(key(), "停用经销商" + suffix, "9131" + System.nanoTime(), "法人", "联系人", "13900000000"));
        DealerTestDocuments.submit(dealerService, files, dealer.id(), key());
        actAs(reviewer);
        dealerService.reviewDealer(dealer.id(), new DealerReviewRequest(key(), "approve", true, true, true, true, "通过"));
        dealerId = dealer.id();
        actAs(sales);
        contractId = dealerService.createContract(dealerId, new DealerContractRequest(key(), "DC-DCC-" + suffix, salesEntity, centralWarehouse,
                LocalDate.now().minusDays(1), LocalDate.now().plusYears(1))).id();
        // 账期合同：首付 30%；运费 10 元、包邮 500 元；非质量退回运费承担方未约定（须双方确认，BSM:1241），首程运费约定不退。
        orders.updateContractTerms(contractId, new DealerContractTermsRequest(key(), true, 3000, null, 3, 30, 1_000L, 50_000L, "dealer", null, false,
                Instant.now().truncatedTo(ChronoUnit.MICROS), "payment_notified", null, "signed", null));
        DealerPriceCatalogRecord v1 = dealerService.createCatalogDraft(contractId, new DealerCatalogDraftRequest(key(), "目录",
                List.of(new DealerCatalogLineRequest(itemA, "箱", 1_000L, 1300, null))));
        dealerService.submitCatalog(v1.id(), key());
        actAs(reviewer);
        dealerService.decideCatalog(v1.id(), new DealerCatalogDecisionRequest(key(), "approve", "通过"));
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** 31-R1：合同销售主体指定经办人走真实入口，合同列表回显；无 dealer.manage 拒绝。 */
    @Test
    void designatedHandlerIsSetThroughRealEntryAndShownInContractList() throws Exception {
        mvc.perform(post("/api/v1/dealers/contracts/" + contractId + "/designated-handler").with(as(viewer)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"handler_user_id\":\"" + sales + "\",\"idempotency_key\":\"" + key() + "\"}"))
                .andExpect(status().isForbidden());
        designate(sales).andExpect(jsonPath("$.data.designated_handler_user_id").value(sales));
        mvc.perform(get("/api/v1/dealers/" + dealerId + "/contracts").with(as(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == '" + contractId + "')].designated_handler_user_id").value(hasItem(sales)));
    }

    /** 31-R2：只有指定经办人能登记对方确认；确认人固定取档案已核验联系人；必须有真实凭证。 */
    @Test
    void onlyDesignatedHandlerRecordsConfirmationWithVerifiedContactSnapshotAndRealEvidence() throws Exception {
        DealerOrderRecord order = shippedOrder(2, 1_000L);
        deactivate();
        String proof = deliveryProof(order.id());
        record(sales, order.id(), "delivery_receipt", proof).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DEALER_HANDLER_NOT_DESIGNATED"));
        designate(sales);
        record(other, order.id(), "delivery_receipt", proof).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("DEALER_HANDLER_ONLY"));
        mvc.perform(post(confirmations(order.id())).with(as(sales)).contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key":"%s","matter_type":"delivery_receipt","content":"对方口头确认","confirmed_at":"%s","evidence_attachment_ids":[]}
                        """.formatted(key(), Instant.now().minusSeconds(60))))
                .andExpect(status().isBadRequest());
        mvc.perform(post(confirmations(order.id())).with(as(sales)).contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key":"%s","matter_type":"delivery_receipt","content":"对方盖章签收函确认实收 2 箱","confirmed_at":"%s",
                         "evidence_attachment_ids":["%s"],"contact_name":"经办人自填的确认人"}
                        """.formatted(key(), Instant.now().minusSeconds(60), proof)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contact_name").value("联系人"))
                .andExpect(jsonPath("$.data.handler_user_id").value(sales));
        mvc.perform(get(confirmations(order.id())).with(as(viewer))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1));
    }

    /** 31-R2：停用后签收须带同订单、同事项、未用过的确认；确认人取档案联系人；同一确认不得再用。执行人不限经办人。 */
    @Test
    void deactivatedReceiptRequiresUnusedMatchingConfirmationAndRecordsVerifiedContact() throws Exception {
        DealerOrderRecord order = shippedOrder(2, 1_000L);
        String line = lineOf(order.id());
        deactivate();
        String proof = deliveryProof(order.id());
        sign(order.id(), line, 1, proof, null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DEALER_COUNTERPARTY_CONFIRMATION_REQUIRED"));
        designate(sales);
        String wrong = recordedId(order.id(), "return_request", proof);
        sign(order.id(), line, 1, proof, wrong).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DEALER_COUNTERPARTY_CONFIRMATION_REQUIRED"));
        String right = recordedId(order.id(), "delivery_receipt", proof);
        sign(order.id(), line, 1, proof, right).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT used_by_id FROM dealer_counterparty_confirmations WHERE id = ?", String.class, right)).isEqualTo(other);
        assertThat(jdbc.queryForList("SELECT buyer_contact FROM dealer_delivery_receipts WHERE order_id = ?", String.class, order.id())).containsExactly("联系人");
        sign(order.id(), line, 1, proof, right).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DEALER_COUNTERPARTY_CONFIRMATION_REQUIRED"));
    }

    /** 31-R2：退货申请、合同未约定的运费确认、交付差异方案、超收用途（财务执行）同一守卫；合同已锁定运费规则时不要求确认。 */
    @Test
    void otherCounterpartyEntriesRequireMatchingConfirmationAfterDeactivation() throws Exception {
        DealerOrderRecord order = shippedOrder(6, 8_000L); // 应收 7000 分，超收 1000 分
        receive(order.id(), 6, 0);
        DealerOrderRecord damagedOrder = shippedOrder(2, 1_000L);
        receive(damagedOrder.id(), 1, 1);
        String mutual = inspectedReturn(order.id(), "unopened_resalable");
        String locked = inspectedReturn(order.id(), "quality_issue");
        actAs(finance);
        String difference = delivery.get(damagedOrder.id()).discrepancies().getFirst().id();
        String overpayment = orders.listOverpayments(dealerId).items().getFirst().id();
        String overpaymentProof = upload("dealer_overpayment", overpayment);
        String mutualProof = upload("dealer_return", mutual);
        String lockedProof = upload("dealer_return", locked);
        String proof = deliveryProof(order.id());
        String damagedProof = deliveryProof(damagedOrder.id());
        deactivate();

        // 合同已锁定运费规则（质量问题 + 首程不退）：不要求对方确认留证。
        freight(locked, "sales_entity", lockedProof, null).andExpect(status().isOk());
        String returnBody = "\"reason_type\":\"unopened_resalable\",\"lines\":[{\"item_id\":\"" + itemA + "\",\"spec\":\"箱\",\"quantity\":1}],\"note\":\"停用后存量退货\"";
        call(sales, "/api/v1/dealers/orders/" + order.id() + "/returns", returnBody).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DEALER_COUNTERPARTY_CONFIRMATION_REQUIRED"));
        freight(mutual, "dealer", mutualProof, null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DEALER_COUNTERPARTY_CONFIRMATION_REQUIRED"));
        String resolutionBody = "\"quantity\":1,\"resolution_type\":\"replacement\",\"physical_route\":\"return_quarantine\",\"adjustment_cent\":0,"
                + "\"responsibility_party\":\"合同销售主体\",\"agreement_basis\":\"双方确认补交 1 箱\",\"evidence_attachment_ids\":[\"" + damagedProof + "\"],"
                + "\"final_approver_id\":\"" + approver + "\",\"parallel_lines\":[{\"line_code\":\"finance_review\",\"final_approver_id\":\"" + finance + "\"}]";
        call(sales, "/api/v1/dealers/delivery/discrepancies/" + difference + "/resolutions", resolutionBody).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DEALER_COUNTERPARTY_CONFIRMATION_REQUIRED"));
        String useBody = "\"usage\":\"prepayment\",\"amount_cent\":1000,\"note\":\"双方确认转预付款\",\"confirmation_attachment_ids\":[\"" + overpaymentProof + "\"]";
        call(finance, "/api/v1/dealers/overpayments/" + overpayment + "/use", useBody).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DEALER_COUNTERPARTY_CONFIRMATION_REQUIRED"));

        designate(sales);
        call(sales, "/api/v1/dealers/orders/" + order.id() + "/returns",
                returnBody + ",\"counterparty_confirmation_id\":\"" + recordedId(order.id(), "return_request", proof) + "\"").andExpect(status().isOk());
        freight(mutual, "dealer", mutualProof, recordedId(order.id(), "freight_confirmation", proof)).andExpect(status().isOk());
        call(sales, "/api/v1/dealers/delivery/discrepancies/" + difference + "/resolutions",
                resolutionBody + ",\"counterparty_confirmation_id\":\"" + recordedId(damagedOrder.id(), "delivery_resolution", damagedProof) + "\"").andExpect(status().isOk());
        call(finance, "/api/v1/dealers/overpayments/" + overpayment + "/use",
                useBody + ",\"counterparty_confirmation_id\":\"" + recordedId(order.id(), "overpayment_use", proof) + "\"").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dealer_counterparty_confirmations WHERE order_id IN (?, ?) AND used_at IS NOT NULL", Integer.class,
                order.id(), damagedOrder.id())).isEqualTo(4);
    }

    /** 31-R3：停用后原订单尾款只由指定经办人登记、只走对公转账；财务核验后冲减原应收。 */
    @Test
    void deactivatedBalancePaymentOnlyByDesignatedHandlerThroughBankTransfer() throws Exception {
        DealerOrderRecord order = shippedOrder(2, 1_000L); // 应收 3000 分，已收 1000 分
        deactivate();
        payment(other, order.id(), "bank_transfer").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DEALER_HANDLER_NOT_DESIGNATED"));
        designate(sales);
        payment(other, order.id(), "bank_transfer").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("DEALER_HANDLER_ONLY"));
        payment(sales, order.id(), "platform_online").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DEALER_DEACTIVATED_BANK_TRANSFER_ONLY"));
        String paymentId = JsonPath.read(payment(sales, order.id(), "bank_transfer").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        actAs(finance);
        orders.verifyPayment(paymentId, new DealerPaymentVerifyRequest(key(), "approve", "对公账户实际到账"));
        assertThat(orders.getOrder(order.id()).paidAmountCent()).isEqualTo(3_000L);
        SecurityContextHolder.clearContext();
        // 裁决补充 15：核验通过的到账记录即收款凭证；经办人交付对方后登记一条结果交付，关联该到账记录。
        String proof = deliveryProof(order.id());
        mvc.perform(post(confirmations(order.id())).with(as(sales)).contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key":"%s","matter_type":"result_delivery","related_object_id":"%s","content":"收款凭证已按核验联系方式交付对方",
                         "confirmed_at":"%s","evidence_attachment_ids":["%s"]}
                        """.formatted(key(), paymentId, Instant.now().minusSeconds(60), proof)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.related_object_id").value(paymentId));
    }

    // ── helpers ──

    private ResultActions designate(String handler) throws Exception {
        return mvc.perform(post("/api/v1/dealers/contracts/" + contractId + "/designated-handler").with(as(sales)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"handler_user_id\":\"" + handler + "\",\"idempotency_key\":\"" + key() + "\"}"))
                .andExpect(status().isOk());
    }

    private ResultActions record(String who, String orderId, String matter, String proof) throws Exception {
        return mvc.perform(post(confirmations(orderId)).with(as(who)).contentType(MediaType.APPLICATION_JSON).content("""
                {"idempotency_key":"%s","matter_type":"%s","content":"已核验联系人书面确认","confirmed_at":"%s","evidence_attachment_ids":["%s"]}
                """.formatted(key(), matter, Instant.now().minusSeconds(60), proof)));
    }

    private String recordedId(String orderId, String matter, String proof) throws Exception {
        return JsonPath.read(record(sales, orderId, matter, proof).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    private ResultActions sign(String orderId, String line, int accepted, String proof, String confirmationId) throws Exception {
        String body = """
                "delivery_batch_id":"%s","confirmed_at":"%s","buyer_contact":"经办人自填","evidence_attachment_ids":["%s"],
                "lines":[{"order_line_id":"%s","accepted_quantity":%d,"rejected_quantity":0,"damaged_quantity":0,"short_quantity":0}],"note":"停用后存量签收"
                """.formatted(orderId, Instant.now().truncatedTo(ChronoUnit.MICROS), proof, line, accepted)
                + (confirmationId == null ? "" : ",\"counterparty_confirmation_id\":\"" + confirmationId + "\"");
        return call(other, "/api/v1/dealers/orders/" + orderId + "/sign", body);
    }

    private ResultActions freight(String returnId, String bearer, String proof, String confirmationId) throws Exception {
        return call(sales, "/api/v1/dealers/returns/" + returnId + "/freight-confirmation",
                "\"return_freight_bearer\":\"" + bearer + "\",\"first_leg_freight_refundable\":false,\"first_leg_adjustment_cent\":0,"
                        + "\"note\":\"按原合同与双方确认\",\"evidence_attachment_ids\":[\"" + proof + "\"]"
                        + (confirmationId == null ? "" : ",\"counterparty_confirmation_id\":\"" + confirmationId + "\""));
    }

    private ResultActions payment(String who, String orderId, String method) throws Exception {
        return call(who, "/api/v1/dealers/orders/" + orderId + "/payments",
                "\"method\":\"" + method + "\",\"amount_cent\":2000,\"reference_no\":\"TXN-" + key() + "\"");
    }

    /** 补上幂等键后按蛇形键原样提交。 */
    private ResultActions call(String who, String path, String fields) throws Exception {
        return mvc.perform(post(path).with(as(who)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + key() + "\"," + fields.strip() + "}"));
    }

    private static String confirmations(String orderId) {
        return "/api/v1/dealers/orders/" + orderId + "/counterparty-confirmations";
    }

    private String lineOf(String orderId) {
        actAs(sales);
        try {
            return orders.listLines(orderId).items().getFirst().id();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /** 停用前按既有签收入口登记实收与破损。 */
    private void receive(String orderId, int accepted, int damaged) {
        String line = lineOf(orderId);
        String proof = deliveryProof(orderId);
        actAs(sales);
        delivery.receive(orderId, new DealerDeliveryReceiptRequest(key(), orderId, Instant.now().truncatedTo(ChronoUnit.MICROS), "联系人",
                List.of(proof), List.of(new DealerDeliveryReceiptRequest.Line(line, accepted, 0, damaged, 0)), "停用前签收"));
        SecurityContextHolder.clearContext();
    }

    private String inspectedReturn(String orderId, String reason) {
        actAs(sales);
        DealerReturnRecord request = returns.requestReturn(orderId, new DealerReturnRequest(key(), reason,
                List.of(new DealerReturnLineRequest(itemA, "箱", 1)), "停用前退货"));
        actAs(reviewer);
        returns.decideReturn(request.id(), new DealerReturnDecisionRequest(key(), "approve", "同意"));
        String proof = upload("dealer_return", request.id());
        actAs(warehouse);
        returns.receiveReturn(request.id(), key());
        boolean quality = "quality_issue".equals(reason);
        returns.inspectReturn(request.id(), new DealerReturnInspectionRequest(key(), List.of(new DealerReturnInspectionLineRequest(itemA, "箱", 1,
                quality ? 0 : 1, quality ? 1 : 0, "实物验收")), "验收", List.of(proof)));
        SecurityContextHolder.clearContext();
        return request.id();
    }

    /** 建单 → 审核 → 到账核验 → 出库（配送中）。 */
    private DealerOrderRecord shippedOrder(int quantity, long paidCent) {
        actAs(sales);
        DealerOrderRecord draft = orders.createOrderDraft(dealerId, new DealerOrderDraftRequest(key(), contractId, List.of(new DealerOrderLineRequest(itemA, "箱", quantity, null)), "停用前已成立订单"));
        orders.submitOrder(draft.id(), key());
        actAs(reviewer);
        orders.reviewOrder(draft.id(), new DealerOrderDecisionRequest(key(), "approve", "通过"));
        actAs(sales);
        DealerOrderPaymentRecord payment = orders.registerPayment(draft.id(), new DealerPaymentRequest(key(), "bank_transfer", paidCent, Instant.now(), "TXN-" + key()));
        actAs(finance);
        orders.verifyPayment(payment.id(), new DealerPaymentVerifyRequest(key(), "approve", "核验"));
        actAs(warehouse);
        DealerOrderRecord shipped = orders.shipOrder(draft.id(), new DealerShipRequest(key(), "出库"));
        SecurityContextHolder.clearContext();
        return shipped;
    }

    private void deactivate() {
        actAs(sales);
        dealerService.deactivateDealer(dealerId, new DealerDeactivateRequest(key(), "合作终止"));
        SecurityContextHolder.clearContext();
    }

    private String deliveryProof(String orderId) {
        return upload("dealer_delivery", orderId);
    }

    private String upload(String type, String id) {
        actAs(sales);
        try {
            return files.uploadAttachment(new MockMultipartFile("file", "对方确认凭证.png", "image/png", Base64.getDecoder().decode(PNG)), type, id, "other").id();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private RequestPostProcessor as(String userId) {
        AccessActor actor = new AccessActor(userId, userId, "platform_admin", "project_main", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private void actAs(String userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(userId, userId, "platform_admin", "project_main", null, "active"), "test", List.of()));
    }

    private static String key() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
