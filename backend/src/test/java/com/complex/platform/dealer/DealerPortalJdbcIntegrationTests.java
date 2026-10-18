package com.complex.platform.dealer;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.auth.AuthService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.dealer.dto.DealerCatalogDecisionRequest;
import com.complex.platform.dealer.dto.DealerCatalogDraftRequest;
import com.complex.platform.dealer.dto.DealerCatalogLineRequest;
import com.complex.platform.dealer.dto.DealerContractRequest;
import com.complex.platform.dealer.dto.DealerContractTermsRequest;
import com.complex.platform.dealer.dto.DealerCreateRequest;
import com.complex.platform.dealer.dto.DealerDeactivateRequest;
import com.complex.platform.dealer.dto.DealerInquiryRequest;
import com.complex.platform.dealer.dto.DealerOrderDecisionRequest;
import com.complex.platform.dealer.dto.DealerOrderDraftRequest;
import com.complex.platform.dealer.dto.DealerOrderLineRequest;
import com.complex.platform.dealer.dto.DealerPaymentRequest;
import com.complex.platform.dealer.dto.DealerPaymentVerifyRequest;
import com.complex.platform.dealer.dto.DealerPortalAccountRequest;
import com.complex.platform.dealer.dto.DealerPortalProfileRequest;
import com.complex.platform.dealer.dto.DealerReturnLineRequest;
import com.complex.platform.dealer.dto.DealerReturnRequest;
import com.complex.platform.dealer.dto.DealerReviewRequest;
import com.complex.platform.dealer.dto.DealerShipRequest;
import com.complex.platform.gate.TerminalSwitchGuard;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * CG-11d 独立经销商 Web 门户（BSM:1217-1218、1246；ENUM:181、233）：受控第十一端 dealer_portal，与商户门户及内部员工端隔离账号、权限和数据；
 * 适用统一终端开关、访问拒绝留痕和兜底关闭；停用生效时立即终止全部门户会话。真实 PostgreSQL（Flyway 全量迁移），@Transactional 回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=${COMPLEX_DATABASE_USERNAME:complex}",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@org.springframework.transaction.annotation.Transactional
class DealerPortalJdbcIntegrationTests {

    @Autowired private DealerService dealerService;
    @Autowired private DealerOrderService orders;
    @Autowired private DealerPortalAccountService portalAccounts;
    @Autowired private DealerPortalService portal;
    @Autowired private TerminalSwitchGuard terminalSwitchGuard;
    @Autowired private AuthService authService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private com.complex.platform.evidence.EvidenceEventService admissionFiles;
    @Autowired private AccessRepository access;

    private String suffix;
    private String staff;
    private String reviewer;
    private String warehouse;
    private String salesEntity;
    private String centralWarehouse;
    private String itemA;
    private String itemB;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        staff = "usr_dp_staff_" + suffix;
        reviewer = "usr_dp_rev_" + suffix;
        warehouse = "usr_dp_wh_" + suffix;
        String role = "role_dp_" + suffix;
        jdbc.update("INSERT INTO roles(id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '经销门户测试', 'platform', 'active', now())",
                role, "经销门户测试角色" + System.nanoTime());
        jdbc.update("INSERT INTO role_permissions(role_id, permission_code) SELECT ?, permission_code FROM permissions WHERE permission_code LIKE 'dealer.%' ON CONFLICT DO NOTHING", role);
        for (String user : List.of(staff, reviewer, warehouse)) {
            access.createUser(new AccessUser(user, user, "platform_admin", "project_main", null, "active", Instant.now()));
            access.assignUserRole(user, role);
        }
        salesEntity = "be_dp_" + suffix;
        jdbc.update("INSERT INTO business_entities(id, entity_name, entity_role, status, created_at, updated_at) VALUES (?, '经销销售主体', 'project_company', 'active', now(), now())", salesEntity);
        centralWarehouse = "wh_dp_" + suffix;
        jdbc.update("INSERT INTO warehouses(id, project_id, warehouse_type, name, status) VALUES (?, 'project_main', 'main', '经销中心仓', 'active')", centralWarehouse);
        itemA = "item_dp_a_" + suffix;
        itemB = "item_dp_b_" + suffix;
        for (String item : List.of(itemA, itemB)) {
            jdbc.update("INSERT INTO inventory_items(id, item_name, item_type, unit, trace_required, min_quantity, status, created_at, updated_at) VALUES (?, '经销威士忌', 'beverage', '箱', false, 2, 'active', now(), now())", item);
        }
        jdbc.update("INSERT INTO inventory_stock_balances(id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, warehouse_id, project_id) VALUES (?, ?, ?, 100, 0, 100, now(), ?, 'project_main')",
                "bal_dp_" + suffix, itemA, centralWarehouse, centralWarehouse);
        terminal("dealer_portal");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void precreatedPortalCredentialsRemainDisabledBeforeAdmission() {
        actAs(staff);
        DealerRecord dealer = dealerService.createDealer(new DealerCreateRequest(key(), "未审账号", "USCC-" + suffix, "法人", "联系人", "13900000000"));
        DealerPortalAccountRecord account = portalAccounts.createPortalAccount(dealer.id(), new DealerPortalAccountRequest(key(), "dealer_disabled_" + suffix, "Portal#2026", "联系人"));
        assertThat(account.status()).isEqualTo("disabled");
        assertThat(jdbc.queryForObject("SELECT status FROM access_users WHERE id = ?", String.class, account.userId())).isEqualTo("disabled");
        var loginRequest = new AuthService.LoginRequest(account.username(), "Portal#2026", null, null, "dealer_portal");
        assertThatThrownBy(() -> authService.login(loginRequest)).isInstanceOf(ApiException.class);
        actAsDealer(account.userId(), dealer.projectId());
        assertThatThrownBy(() -> portal.me()).isInstanceOf(ApiException.class);
        actAs(staff);
        DealerTestDocuments.submit(dealerService, admissionFiles, dealer.id(), key());
        actAs(reviewer);
        dealerService.reviewDealer(dealer.id(), new DealerReviewRequest(key(), "approve", true, true, true, true, "核验四项资料"));
        assertThat(authService.login(loginRequest).accessToken()).isNotBlank();
        assertThat(jdbc.queryForObject("SELECT status FROM access_users WHERE id = ?", String.class, account.userId())).isEqualTo("active");
        actAsDealer(account.userId(), dealer.projectId());
        assertThat(portal.me().id()).isEqualTo(dealer.id());
    }

    @Test
    void admissionApprovalDoesNotReactivateRevokedPortalAccounts() {
        actAs(staff);
        DealerRecord dealer = dealerService.createDealer(new DealerCreateRequest(key(), "旧账号", "USCC-" + suffix, "法人", "联系人", "13900000000"));
        DealerPortalAccountRecord account = portalAccounts.createPortalAccount(dealer.id(), new DealerPortalAccountRequest(key(), "dealer_revoked_" + suffix, "Portal#2026", "联系人"));
        jdbc.update("UPDATE dealer_portal_accounts SET status = 'revoked', revoked_at = now() WHERE user_id = ?", account.userId());
        DealerTestDocuments.submit(dealerService, admissionFiles, dealer.id(), key());
        actAs(reviewer);
        dealerService.reviewDealer(dealer.id(), new DealerReviewRequest(key(), "approve", true, true, true, true, "四项通过"));
        assertThat(jdbc.queryForObject("SELECT status FROM dealer_portal_accounts WHERE user_id = ?", String.class, account.userId())).isEqualTo("revoked");
        assertThat(jdbc.queryForObject("SELECT status FROM access_users WHERE id = ?", String.class, account.userId())).isEqualTo("disabled");
        assertThatThrownBy(() -> authService.login(new AuthService.LoginRequest(account.username(), "Portal#2026", null, null, "dealer_portal")))
                .isInstanceOf(ApiException.class);
    }

    /** 隔离账号 / 权限 / 终端：内部账号进不了门户、门户账号进不了内部后台、非 dealer_portal 终端拒绝并留痕、总开关关闭兜底拒绝并留痕。 */
    @Test
    void portalIsIsolatedByAccountPermissionAndTerminalWithRejectionTrail() {
        actAs(staff);
        DealerRecord dealer = dealerService.createDealer(new DealerCreateRequest(key(), "门户经销商" + suffix, "9131" + System.nanoTime(), "法人", "联系人", "13900000000"));
        DealerPortalAccountRecord account = portalAccounts.createPortalAccount(dealer.id(), new DealerPortalAccountRequest(key(), "dealer_" + suffix, "Portal#2026", "企业联系人"));
        assertThat(account.dealerId()).isEqualTo(dealer.id());
        assertThat(jdbc.queryForObject("SELECT account_type FROM access_users WHERE id = ?", String.class, account.userId())).as("隔离账号类型").isEqualTo("dealer");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role_id = 'role_dealer_portal'", Integer.class, account.userId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM role_permissions WHERE role_id = 'role_dealer_portal' AND permission_code LIKE 'dealer.%'", Integer.class))
                .as("门户角色不含任何内部 dealer.* 权限").isZero();

        DealerTestDocuments.submit(dealerService, admissionFiles, dealer.id(), key());
        actAs(reviewer);
        dealerService.reviewDealer(dealer.id(), new DealerReviewRequest(key(), "approve", true, true, true, true, "通过"));
        actAs(staff);

        // 内部员工账号（即使持全部 dealer.* 权限）不得进入门户
        assertThatThrownBy(() -> portal.me()).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_PORTAL_ACCOUNT_REQUIRED"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name = 'dealer.portal.access_rejected' AND business_id = ?", Integer.class, staff)).isEqualTo(1);

        // 门户账号不得使用内部后台接口
        actAsDealer(account.userId(), dealer.projectId());
        assertThatThrownBy(() -> dealerService.listDealers()).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
        assertThatThrownBy(() -> dealerService.createDealer(new DealerCreateRequest(key(), "越权", "9131" + System.nanoTime(), "法人", "联系人", "13900000001")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
        assertThat(portal.me().id()).isEqualTo(dealer.id());

        // 非 dealer_portal 终端（员工 Web 端自报）访问门户：拒绝并留痕
        terminal("web");
        assertThatThrownBy(() -> portal.me()).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("TERMINAL_NOT_ALLOWED"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name = 'terminal.access_rejected' AND metadata::text LIKE '%terminal_mismatch%' AND metadata::text LIKE ?",
                Integer.class, "%" + account.userId() + "%")).isEqualTo(1);
        terminal("dealer_portal");

        // 第十一端总开关行存在且 CHECK 接受 dealer_portal；关闭后统一终端开关拒绝并留痕 terminal.access_rejected
        assertThat(jdbc.queryForObject("SELECT enabled FROM terminal_capability_switches WHERE id = 'tcs_term_dealer_portal' AND terminal_type = 'dealer_portal' AND capability_code = 'terminal.enabled'", Boolean.class)).isTrue();
        String guardTrail = "SELECT COUNT(*) FROM audit_events WHERE event_name = 'terminal.access_rejected' AND metadata::text LIKE '%dealer_portal%' AND metadata::text LIKE '%terminal.enabled%'";
        int trailBefore = jdbc.queryForObject(guardTrail, Integer.class); // 闸口留痕走独立事务提交，跨测试运行累计，只比增量
        jdbc.update("UPDATE terminal_capability_switches SET enabled = FALSE WHERE id = 'tcs_term_dealer_portal'");
        assertThatThrownBy(() -> terminalSwitchGuard.checkTerminalEnabled()).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("TERMINAL_DISABLED"));
        assertThat(jdbc.queryForObject(guardTrail, Integer.class)).isEqualTo(trailBefore + 1);
        jdbc.update("UPDATE terminal_capability_switches SET enabled = TRUE WHERE id = 'tcs_term_dealer_portal'");
        terminalSwitchGuard.checkTerminalEnabled();

        // 已停用经销商不得再开门户账号
        actAs(reviewer);
        dealerService.deactivateDealer(dealer.id(), new DealerDeactivateRequest(key(), "合作终止"));
        assertThatThrownBy(() -> portalAccounts.createPortalAccount(dealer.id(), new DealerPortalAccountRequest(key(), "dealer2_" + suffix, "Portal#2026", "联系人")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_DEACTIVATED"));
    }

    /** 门户全链：注册资料提交 → （内部审核启用门户）→ 库存外显只显示四态 → 询价 → 下单 → 付款登记 → 签收 → 退货申请；只见本企业数据。 */
    @Test
    void portalFlowFromProfileSubmissionToReturnRequestShowsOnlyOwnDataAndFourStockStates() throws Exception {
        actAs(staff);
        DealerRecord dealer = dealerService.createDealer(new DealerCreateRequest(key(), "门户经销商" + suffix, "9131" + System.nanoTime(), "法人", "联系人", "13900000000"));
        DealerPortalAccountRecord account = portalAccounts.createPortalAccount(dealer.id(), new DealerPortalAccountRequest(key(), "dealer_" + suffix, "Portal#2026", "企业联系人"));
        DealerRecord other = dealerService.createDealer(new DealerCreateRequest(key(), "别家经销商" + suffix, "9132" + System.nanoTime(), "法人", "联系人", "13900000002"));
        DealerPortalAccountRecord otherAccount = portalAccounts.createPortalAccount(other.id(), new DealerPortalAccountRequest(key(), "dealer_o_" + suffix, "Portal#2026", "联系人"));

        // 未四审账号不能登录，资料由内部销售提交，运营审核后激活。
        actAsDealer(account.userId(), dealer.projectId());
        assertThatThrownBy(() -> portal.submitProfile(new DealerPortalProfileRequest(key(), "新法人", "新联系人", "13911112222")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_PORTAL_NOT_ENABLED"));
        actAs(staff);
        DealerRecord submitted = DealerTestDocuments.submit(dealerService, admissionFiles, dealer.id(), key());
        assertThat(submitted.status()).isEqualTo("under_review");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name = 'dealer.submitted' AND business_id = ?", Integer.class, dealer.id())).isEqualTo(1);

        actAs(reviewer);
        dealerService.reviewDealer(dealer.id(), new DealerReviewRequest(key(), "approve", true, true, true, true, "通过"));
        actAs(staff);
        DealerContractRecord contract = dealerService.createContract(dealer.id(), new DealerContractRequest(key(), "DC-DP-" + suffix, salesEntity, centralWarehouse,
                LocalDate.now().minusDays(1), LocalDate.now().plusYears(1)));
        orders.updateContractTerms(contract.id(), new DealerContractTermsRequest(key(), false, null, null, 3, 30, 1_000L, 50_000L, "dealer", null, null, null, "payment_notified", null, "shipped", null));
        DealerPriceCatalogRecord v1 = dealerService.createCatalogDraft(contract.id(), new DealerCatalogDraftRequest(key(), "目录",
                List.of(new DealerCatalogLineRequest(itemA, "箱", 1_000L, 1300, null))));
        dealerService.submitCatalog(v1.id(), key());
        actAs(reviewer);
        dealerService.decideCatalog(v1.id(), new DealerCatalogDecisionRequest(key(), "approve", "通过"));

        // 库存外显：只显示有货 / 紧张 / 缺货 / 预计交期，不含数量
        actAsDealer(account.userId(), dealer.projectId());
        assertThat(portal.me().portalEnabled()).isTrue();
        assertThat(portal.listContracts().items()).extracting(DealerContractRecord::id).containsExactly(contract.id());
        List<DealerExternalStockRecord> stock = portal.externalStock(contract.id()).items();
        assertThat(stock).hasSize(1);
        assertThat(stock.get(0).availability()).isEqualTo("in_stock");
        assertThat(DealerExternalStockRecord.class.getRecordComponents()).extracting(c -> c.getName().toLowerCase())
                .noneMatch(n -> n.contains("quantity") || n.contains("qty"));
        // 别家经销商的合同不可见、不可外显
        actAsDealer(otherAccount.userId(), other.projectId());
        assertThatThrownBy(() -> portal.externalStock(contract.id())).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_PORTAL_NOT_ENABLED"));
        actAsDealer(account.userId(), dealer.projectId());

        // 询价（未定价商品）与下单（已定价商品）
        DealerInquiryRecord inquiry = portal.createInquiry(new DealerInquiryRequest(key(), contract.id(), itemB, "箱", 5, "询价"));
        assertThat(inquiry.status()).isEqualTo("pending_quote");
        assertThat(portal.listInquiries().items()).extracting(DealerInquiryRecord::id).containsExactly(inquiry.id());
        DealerOrderRecord draft = portal.createOrder(new DealerOrderDraftRequest(key(), contract.id(), List.of(new DealerOrderLineRequest(itemA, "箱", 4, null)), "门户下单"));
        assertThat(draft.status()).isEqualTo("draft");
        assertThat(portal.submitOrder(draft.id(), key()).status()).isEqualTo("pending_review");
        assertThat(portal.listOrders().items()).extracting(DealerOrderRecord::id).containsExactly(draft.id());

        actAs(reviewer);
        orders.reviewOrder(draft.id(), new DealerOrderDecisionRequest(key(), "approve", "通过"));

        // 付款登记（只能对公转账 / 平台线上支付）→ 内部核验 → 出库 → 门户签收
        actAsDealer(account.userId(), dealer.projectId());
        assertThatThrownBy(() -> portal.registerPayment(draft.id(), new DealerPaymentRequest(key(), "cash", 5_000L, null, "现金")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PAYMENT_METHOD_FORBIDDEN"));
        DealerOrderPaymentRecord payment = portal.registerPayment(draft.id(), new DealerPaymentRequest(key(), "bank_transfer", 5_000L, null, "TX-DP-1"));
        assertThat(payment.status()).isEqualTo("pending_verification");
        assertThat(portal.listPayments(draft.id()).items()).hasSize(1);
        actAs(staff);
        orders.verifyPayment(payment.id(), new DealerPaymentVerifyRequest(key(), "approve", "核验"));
        actAs(warehouse);
        orders.shipOrder(draft.id(), new DealerShipRequest(key(), "出库"));
        actAsDealer(account.userId(), dealer.projectId());
        assertThat(portal.getOrder(draft.id()).status()).isEqualTo("shipping");
        String deliveryProof = portal.uploadDeliveryEvidence(draft.id(), DealerTestDeliveries.file()).id();
        assertThat(portal.readDeliveryEvidence(draft.id(),deliveryProof).getBody().getContentAsByteArray()).isEqualTo(DealerTestDeliveries.file().getBytes());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name='dealer.portal.delivery_evidence_viewed' AND business_id=? AND event_result='success'",Long.class,deliveryProof)).isEqualTo(1L);
        var anotherOwnOrder=portal.createOrder(new DealerOrderDraftRequest(key(),contract.id(),List.of(new DealerOrderLineRequest(itemA,"箱",1,null)),"另一订单凭证隔离"));
        assertThatThrownBy(()->portal.readDeliveryEvidence(anotherOwnOrder.id(),deliveryProof)).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("ATTACHMENT_NOT_FOUND"));
        var signedReceipt=DealerTestDeliveries.full(draft.id(),portal.listLines(draft.id()).items(),deliveryProof);
        assertThat(portal.signOrder(draft.id(), signedReceipt).status()).as("4000 货款 + 1000 运费 = 5000 全额到账，签收即完成").isEqualTo("completed");
        assertThat(portal.delivery(draft.id()).receipts()).hasSize(1);

        // 退货申请（门户自助）→ 待审批
        DealerReturnRecord ret = portal.requestReturn(draft.id(), new DealerReturnRequest(key(), "unopened_resalable", List.of(new DealerReturnLineRequest(itemA, "箱", 1)), "未拆封"));
        assertThat(ret.status()).isEqualTo("pending_approval");
        assertThat(portal.listReturns(draft.id()).items()).extracting(DealerReturnRecord::id).containsExactly(ret.id());

        // 数据隔离：别家门户账号看不到本企业订单（404 不泄露存在性）
        actAsDealer(otherAccount.userId(), other.projectId());
        assertThatThrownBy(() -> portal.getOrder(draft.id())).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_PORTAL_NOT_ENABLED"));
        actAs(staff);
        DealerTestDocuments.submit(dealerService, admissionFiles, other.id(), key());
        actAs(reviewer);
        dealerService.reviewDealer(other.id(), new DealerReviewRequest(key(), "approve", true, true, true, true, "通过"));
        actAsDealer(otherAccount.userId(), other.projectId());
        assertThatThrownBy(() -> portal.getOrder(draft.id())).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_ORDER_NOT_FOUND"));
        assertThat(portal.listOrders().items()).isEmpty();
        assertThatThrownBy(() -> portal.readDeliveryEvidence(draft.id(),deliveryProof)).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("DEALER_ORDER_NOT_FOUND"));
        assertThatThrownBy(()->portal.signOrder(draft.id(),signedReceipt)).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("DEALER_ORDER_NOT_FOUND"));
        assertThatThrownBy(()->portal.delivery(draft.id())).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("DEALER_ORDER_NOT_FOUND"));
        assertThatThrownBy(()->portal.uploadDeliveryEvidence(draft.id(),DealerTestDeliveries.file())).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("DEALER_ORDER_NOT_FOUND"));
        SecurityContextHolder.clearContext();
        assertThatThrownBy(()->portal.signOrder(draft.id(),signedReceipt)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->portal.delivery(draft.id())).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->portal.uploadDeliveryEvidence(draft.id(),DealerTestDeliveries.file())).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->portal.readDeliveryEvidence(draft.id(),deliveryProof)).isInstanceOf(ApiException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name='dealer.delivery.receive_failed' AND business_id=? AND event_result='failure'",Long.class,draft.id())).isEqualTo(2L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name='dealer.delivery.view_failed' AND business_id=? AND event_result='failure'",Long.class,draft.id())).isEqualTo(2L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name='dealer.delivery.viewed' AND business_id=? AND event_result='success'",Long.class,draft.id())).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name='evidence.attachment_upload_failed' AND business_id=? AND event_result='failure'",Long.class,draft.id())).isEqualTo(2L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name='dealer.delivery.received' AND business_id=? AND event_result='success'",Long.class,draft.id())).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name='evidence.attachment_uploaded' AND business_id=? AND event_result='success'",Long.class,deliveryProof)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dealer_delivery_receipts WHERE order_id=?",Long.class,draft.id())).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM evidence_attachments WHERE business_type='dealer_delivery' AND business_id=?",Long.class,draft.id())).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name='dealer.portal.delivery_evidence_view_failed' AND business_id=? AND event_result='failure'",Long.class,deliveryProof)).isEqualTo(3L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name='dealer.portal.delivery_evidence_view_failed' AND business_id=? AND actor_id IS NULL",Long.class,deliveryProof)).isEqualTo(1L);
    }

    /** ENUM:181 / BSM:1246：停用生效时立即终止全部门户登录会话，门户账号停用，已完成订单与审计记录保留；停用后门户访问被拒并留痕。 */
    @Test
    void deactivationTerminatesPortalSessionsImmediately() {
        actAs(staff);
        DealerRecord dealer = dealerService.createDealer(new DealerCreateRequest(key(), "门户经销商" + suffix, "9131" + System.nanoTime(), "法人", "联系人", "13900000000"));
        DealerPortalAccountRecord account = portalAccounts.createPortalAccount(dealer.id(), new DealerPortalAccountRequest(key(), "dealer_" + suffix, "Portal#2026", "企业联系人"));
        DealerTestDocuments.submit(dealerService, admissionFiles, dealer.id(), key());
        actAs(reviewer);
        dealerService.reviewDealer(dealer.id(), new DealerReviewRequest(key(), "approve", true, true, true, true, "通过"));

        // 门户账号登录（test profile 为内存会话，与生产 auth_sessions 同一撤销接口）
        AuthService.LoginResponse login = authService.login(new AuthService.LoginRequest("dealer_" + suffix, "Portal#2026", null, null, "dealer_portal"));
        assertThat(authService.authenticateToken(login.accessToken())).isPresent();

        actAs(reviewer);
        dealerService.deactivateDealer(dealer.id(), new DealerDeactivateRequest(key(), "合作终止"));
        assertThat(authService.authenticateToken(login.accessToken())).as("停用生效时立即终止全部门户会话").isEmpty();
        assertThatThrownBy(() -> authService.login(new AuthService.LoginRequest(account.username(), "Portal#2026", null, null, "dealer_portal")))
                .isInstanceOf(ApiException.class);
        assertThat(jdbc.queryForObject("SELECT status FROM access_users WHERE id = ?", String.class, account.userId())).isEqualTo("disabled");
        assertThat(jdbc.queryForObject("SELECT status FROM dealer_portal_accounts WHERE user_id = ?", String.class, account.userId())).isEqualTo("revoked");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name = 'dealer.portal.revoked' AND business_id = ?", Integer.class, dealer.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name = 'dealer.deactivated' AND business_id = ?", Integer.class, dealer.id())).as("审计记录保留").isEqualTo(1);

        actAsDealer(account.userId(), dealer.projectId());
        assertThatThrownBy(() -> portal.me()).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_DEACTIVATED"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name = 'dealer.portal.access_rejected' AND business_id = ?", Integer.class, account.userId())).isEqualTo(1);
    }

    // ── helpers ──

    private void terminal(String type) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/dealer-portal/me");
        request.addHeader("X-Terminal-Type", type);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private void actAs(String userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(userId, userId, "platform_admin", "project_main", null, "active"), "test", List.of()));
    }

    private void actAsDealer(String userId, String projectId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(userId, userId, "dealer", projectId, null, "active"), "test", List.of()));
    }

    private static String key() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
