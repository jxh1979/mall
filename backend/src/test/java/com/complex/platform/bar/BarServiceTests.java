package com.complex.platform.bar;

// 文件版本：V0.11

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.entry.FaceVerificationRouter;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.TraceabilityRepository;
import com.complex.platform.evidence.TraceabilityService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.support.InMemoryAuditEventRepository;
import com.complex.platform.support.InMemoryIdempotencyRepository;
import com.complex.platform.support.TestSecurityActors;
import com.complex.platform.ticketing.dto.VerificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BarServiceTests {

    @org.junit.jupiter.api.io.TempDir java.nio.file.Path evidenceDirectory;
    private InMemoryBarRepository barRepository;
    private BarDepositSettlementService depositSettlements;
    private InMemoryAuditEventRepository auditEventRepository;
    private CapturingBarReceivableLedgerService receivableLedgerService;
    private AuthorizationService authorizationService;
    private static final String VOID_REVIEWER = "usr_void_reviewer";
    private BarService service;
    private final BarInvitationTokenSigner invitationSigner = new BarInvitationTokenSigner("test-bar-invitation-secret");
    private BarBillingService billingService;
    private BarOrderService barOrderService;
    private StubCommissionRateConfigPort commissionRateStub;
    private com.complex.platform.access.staff.JdbcStaffRepository staffRepositoryStub;
    private com.complex.platform.finance.FinanceService financeServiceStub;
    private StubHighValueDecisionService highValueStub;
    private com.complex.platform.evidence.EvidenceEventRepository evidenceRepositoryStub;

    /** V351 退酒原因码字典测试双子：只有集合内的码算生效码。 */
    private record StubVoidReasonCodeDirectory(java.util.Set<String> activeCodes)
            implements BarVoidReasonCodeDirectory {
        @Override
        public boolean isActiveReasonCode(String code) {
            return activeCodes.contains(code);
        }

        @Override
        public List<BarVoidReasonCodeRecord> listReasonCodes(boolean activeOnly) {
            return activeCodes.stream()
                    .map(c -> new BarVoidReasonCodeRecord(c, c, 0, "active", null, null, Instant.now()))
                    .toList();
        }

        @Override
        public BarVoidReasonCodeRecord saveReasonCode(String code, String labelZh, Integer sortOrder, String actorId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public BarVoidReasonCodeRecord archiveReasonCode(String code, String archiveReason, String actorId) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * V350 高价值判定测试双子：退酒是否高价值由<b>物品进货价 vs 后台单一阈值</b>决定（不再看账单金额）。
     * 判定逻辑本身在 {@code HighValueDecisionServiceTests} 里逐分支验；这里只驱动退酒路径的分流。
     */
    private static final class StubHighValueDecisionService
            extends com.complex.platform.inventory.HighValueDecisionService {
        private boolean highValue;
        private Long thresholdCent = 500_000L;
        private Long purchaseUnitCostCent = 100_000L;
        private String lastInventoryItemId;
        private String lastMerchantId;

        StubHighValueDecisionService() {
            super(null);
        }

        @Override
        public HighValueDecision decide(String inventoryItemId, String merchantId) {
            lastInventoryItemId = inventoryItemId;
            lastMerchantId = merchantId;
            return new HighValueDecision(highValue, thresholdCent, purchaseUnitCostCent,
                    highValue ? BASIS_ABOVE_THRESHOLD : BASIS_BELOW_THRESHOLD);
        }
    }

    /** P0-D/CFG-01（V340）提成费率配置端口测试双子：记录被问到的作用域，按角色档返回可设定的费率。 */
    private static final class StubCommissionRateConfigPort implements BarCommissionRateConfigPort {
        private int salesBp;
        private int nonSalesBp;
        private String lastMerchantId;
        private String lastRoleType;

        StubCommissionRateConfigPort(int salesBp, int nonSalesBp) {
            this.salesBp = salesBp;
            this.nonSalesBp = nonSalesBp;
        }

        @Override
        public int findCommissionRateBp(String projectId, String merchantId, String roleType) {
            lastMerchantId = merchantId;
            lastRoleType = roleType;
            return "sales".equals(roleType) ? salesBp : nonSalesBp;
        }
    }

    @BeforeEach
    void setUp() {
        TestSecurityActors.asPlatformAdmin();
        barRepository = new InMemoryBarRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        receivableLedgerService = new CapturingBarReceivableLedgerService();
        AccessRepository accessRepository = mock(AccessRepository.class);
        when(accessRepository.permissionExists(anyString())).thenReturn(true);
        when(accessRepository.listPermissions()).thenReturn(List.of(
                new com.complex.platform.access.PermissionDefinition("bar.bill.refund", "退款", "bar", "high", false, "active"),
                new com.complex.platform.access.PermissionDefinition("bar.order_item.void_decide", "退酒审批", "bar", "high", false, "active")));
        // 第十八批第 24 条 24-R3（PGM:529）：退品审批人（申请人以外的人）同持退款与退酒审批码。
        when(accessRepository.findPermissionEffects(org.mockito.ArgumentMatchers.eq(VOID_REVIEWER), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(
                        new AccessRepository.PermissionEffect(VOID_REVIEWER, "bar.bill.refund", AccessRepository.Effect.GRANT,
                                AccessRepository.Source.ROLE, "explicit_refund", null, null, false, false),
                        new AccessRepository.PermissionEffect(VOID_REVIEWER, "bar.order_item.void_decide", AccessRepository.Effect.GRANT,
                                AccessRepository.Source.ROLE, "explicit_void_review", null, null, false, false)));
        when(accessRepository.findPermissionEffects(org.mockito.ArgumentMatchers.eq("usr_admin"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(
                        new AccessRepository.PermissionEffect("usr_admin", "bar.bill.refund", AccessRepository.Effect.GRANT,
                                AccessRepository.Source.ROLE, "explicit_refund", null, null, false, false),
                        new AccessRepository.PermissionEffect("usr_admin", "bar.order_item.void_decide", AccessRepository.Effect.GRANT,
                                AccessRepository.Source.ROLE, "explicit_void_review", null, null, false, false)));
        authorizationService = new AuthorizationService(accessRepository);
        AuditEventService auditEventService = new AuditEventService(auditEventRepository);
        IdempotencyService idempotencyService = new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper());
        AuditedOperationExecutor operations = new AuditedOperationExecutor(idempotencyService, auditEventService);
        BarEntryService entryService = new BarEntryService(
                barRepository, authorizationService, auditEventService, FaceVerificationRouter.alwaysDisabled(),
                invitationSigner);
        // 裁决第十三批第 7 条员工代订核对受限必经客人域：手工构造时注入桩（查不到受限会员＝照常代订）。
        entryService.customerService = mock(com.complex.platform.customer.CustomerService.class);
        // 第十四批 OBS-13-03-R6：免费时限内取消登记原路待退经财务域，手工构造时注入桩。
        financeServiceStub = mock(com.complex.platform.finance.FinanceService.class);
        entryService.financeService = financeServiceStub;
        barOrderService = new BarOrderService(barRepository, authorizationService, auditEventService,
                new TraceabilityService(mock(TraceabilityRepository.class), authorizationService),
                receivableLedgerService);
        // V350：退酒高价值判定改走全平台唯一入口（物品进货价 vs 后台单一阈值）。默认低价值，
        // 需要高价值分支的用例各自把 highValueStub.highValue 置 true。
        highValueStub = new StubHighValueDecisionService();
        barOrderService.highValueDecisionService = highValueStub;
        // V351 原因码字典：默认只认出厂八条里的两条，用于验「不在字典的码被拒」。
        barOrderService.voidReasonCodeDirectory = new StubVoidReasonCodeDirectory(
                java.util.Set.of("quality_defect", "customer_change"));
        // 双证据闭环：附件必须真实存在。默认只有 att-void-photo 这一个「已上传」的附件。
        evidenceRepositoryStub = mock(com.complex.platform.evidence.EvidenceEventRepository.class);
        when(evidenceRepositoryStub.findAttachmentById(anyString())).thenReturn(java.util.Optional.empty());
        when(evidenceRepositoryStub.findAttachmentById("att-void-photo")).thenReturn(java.util.Optional.of(
                new com.complex.platform.evidence.EvidenceAttachmentRecord(
                        "att-void-photo", "p.jpg", "image/jpeg", 1L, "bar_order_item_void", null,
                        "other", "evidence-files/p.jpg", "/api/v1/evidence/attachments/att-void-photo/file",
                        "uploaded", null, Instant.now(), Instant.now(), null)));
        try {
            java.nio.file.Files.write(evidenceDirectory.resolve("p.jpg"), new byte[]{1});
            java.nio.file.Files.write(evidenceDirectory.resolve("v.mp4"), new byte[]{2});
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
        when(evidenceRepositoryStub.findAttachmentById("att-void-video")).thenReturn(Optional.of(
                new com.complex.platform.evidence.EvidenceAttachmentRecord(
                        "att-void-video", "v.mp4", "video/mp4", 1L, "bar_order_item_void", null,
                        "other", "evidence-files/v.mp4", null, "uploaded", null, Instant.now(), Instant.now(), null)));
        barOrderService.evidenceAttachmentValidator =
                new com.complex.platform.evidence.EvidenceAttachmentValidator(evidenceRepositoryStub,
                        new com.complex.platform.evidence.EvidenceFileStorage(evidenceDirectory.toString(), 1024));
        billingService = new BarBillingService(
                barRepository, receivableLedgerService, authorizationService, auditEventService);
        // P0-D/CFG-01（V340）：提成率改后台可配。单测注入「出厂种子等值」测试双子（销售 1500bp / 非销售 1000bp）——
        // 既有提成用例的期望数字一字未改仍全绿 = 本次改造【零行为改变】的单测侧证据。
        commissionRateStub = new StubCommissionRateConfigPort(1500, 1000);
        billingService.commissionRateConfigPort = commissionRateStub;
        // G/V349：提成角色档改按【员工岗位】判。员工档案桩：
        //   staff_sales     = 员工档案里标了「销售岗」    → 按销售人员费率；
        //   staff_non_sales = 员工档案里没标销售岗        → 按非销售人员费率（改前这人挂上归属就拿 15%，方向是反的）；
        //   其余 id         = 档案里查无此人             → 「其余」→ 非销售人员费率。
        staffRepositoryStub = mock(com.complex.platform.access.staff.JdbcStaffRepository.class);
        when(staffRepositoryStub.findProfileByUserId(anyString())).thenReturn(Optional.empty());
        when(staffRepositoryStub.findProfileByUserId("staff_sales"))
                .thenReturn(Optional.of(staffProfile("staff_sales", true)));
        when(staffRepositoryStub.findProfileByUserId("staff_non_sales"))
                .thenReturn(Optional.of(staffProfile("staff_non_sales", false)));
        billingService.staffRepository = staffRepositoryStub;
        // AUD-007 退款额度闸 fail-closed 常在：默认注入宽松引擎，令既有退款调账用例（平台管理员操作）仍额度内放行；
        // 超额拒绝 / 引擎缺失场景各用例自行覆盖。
        billingService.refundAuthorizationService =
                new com.complex.platform.finance.RefundAuthorizationService(barRefundRepoWithLimit(Long.MAX_VALUE));
        // 第十批 TK-24-R3：定金处置引擎接到下单 / 补足 / 并台转台三处触发点。
        depositSettlements = new BarDepositSettlementService(barRepository, receivableLedgerService, auditEventService);
        barOrderService.depositSettlements = depositSettlements;
        billingService.depositSettlements = depositSettlements;
        BarTableSessionService tableSessionService = new BarTableSessionService(barRepository, authorizationService);
        tableSessionService.depositSettlements = depositSettlements;
        tableSessionService.auditEvents = auditEventService;
        service = new BarService(
                barRepository,
                new BarOperationExecutor(operations),
                billingService,
                new BarShiftService(barRepository, authorizationService, auditEventService),
                tableSessionService,
                barOrderService,
                entryService,
                authorizationService
        );
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    private static com.complex.platform.finance.RefundAuthorizationRepository barRefundRepoWithLimit(long cap) {
        Instant ts = Instant.parse("2026-06-23T00:00:00Z");
        return new com.complex.platform.finance.RefundAuthorizationRepository() {
            @Override public Optional<com.complex.platform.finance.RefundAuthorityLimitRecord> findActiveLimit(
                    String st, String sid, String dom, String pid) {
                return Optional.of(new com.complex.platform.finance.RefundAuthorityLimitRecord(
                        "perm", st, sid, dom, cap, pid, "active", ts, ts));
            }
            @Override public boolean isAutoRuleEnabled(String dom, String type, String pid) { return false; }
            @Override public com.complex.platform.finance.RefundAuthorityLimitRecord saveLimit(
                    com.complex.platform.finance.RefundAuthorityLimitRecord l) { return l; }
            @Override public List<com.complex.platform.finance.RefundAuthorityLimitRecord> listLimits(String projectId) {
                return List.of();
            }
            @Override public com.complex.platform.finance.RefundAutoRuleRecord saveRule(
                    com.complex.platform.finance.RefundAutoRuleRecord r) { return r; }
            @Override public List<com.complex.platform.finance.RefundAutoRuleRecord> listRules(String projectId) {
                return List.of();
            }
        };
    }

    private BarBillRecord settledBillForRefund(String tag) {
        BarBillRecord bill = billForOrder(tag, "staff_sales");
        service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-" + tag, "settle before refund", List.of(),
                "pay-" + tag, null, "aggregate_pay", null, null));
        return bill;
    }

    @Test
    void refundOverSelfServiceLimitIsBlockedByTieredAuthorization() {
        // AUD-007 bar 侧额度闸生效（无平台管理员旁路）：额度=100 分的引擎下，30000 分退款超额 → REFUND_REVIEW_REQUIRED。
        billingService.refundAuthorizationService =
                new com.complex.platform.finance.RefundAuthorizationService(barRefundRepoWithLimit(100L));
        BarBillRecord bill = settledBillForRefund("refund-over-limit");
        assertThatThrownBy(() -> service.applyBillAdjustment(bill.id(), new BarBillAdjustmentRequest(
                "bill-adjust-refund-over", "refund over limit", List.of(), "refund", 30000L, "over limit", "finance_1")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("REFUND_REVIEW_REQUIRED");
    }

    @Test
    void refundWithoutTieredEngineFailsClosed() {
        // AUD-007 依赖缺失 fail-closed：分级授权引擎未注入 → 拒绝而非放行（生产恒注入，此处模拟异常）。
        billingService.refundAuthorizationService = null;
        BarBillRecord bill = settledBillForRefund("refund-failclosed");
        assertThatThrownBy(() -> service.applyBillAdjustment(bill.id(), new BarBillAdjustmentRequest(
                "bill-adjust-refund-fc", "refund no engine", List.of(), "refund", 30000L, "no engine", "finance_1")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("REFUND_AUTHORIZATION_UNAVAILABLE");
    }

    @Test
    void createTableLandsIndependentCapacitySeparateFromFreeEntryCount() {
        // 可进人数（容量）与免票入场人数解耦：显式传 capacity=8、免票=2，两者各自落库、互不相等。
        BarTableRecord table = service.createTable(new BarTableRequest(
                "cap-independent", "create", List.of(), "bar_1", "容量测试桌", "card_seat",
                50000L, 2, 8, "active"));
        assertThat(table.capacity()).isEqualTo(8);
        assertThat(table.allowedPassFreeGuestCount()).isEqualTo(2);
    }

    @Test
    void createTableWithoutCapacityFallsBackToFreeEntryCount() {
        // 未传 capacity（旧签名/前端不填）时沿用免票人数口径，保持向后兼容。
        BarTableRecord table = service.createTable(new BarTableRequest(
                "cap-fallback", "create", List.of(), "bar_1", "回退桌", "card_seat", 50000L, 5, "active"));
        assertThat(table.capacity()).isEqualTo(5);
        assertThat(table.allowedPassFreeGuestCount()).isEqualTo(5);
    }

    @Test
    void createTableSessionForPlatformDirectFillsNonNullMerchantPlaceholder() {
        // 平台直营开台（平台管理员 + 桌台 merchant 为空）：会话 merchant_id 落 __platform__ 占位，
        // 满足 bar_table_sessions.merchant_id NOT NULL 约束（此前直插 null 触发 500）。
        BarTableRecord table = createTable("platform-open", "bar_1", "active", 50000L);
        assertThat(table.merchantId()).isNull();
        BarTableSessionRecord session = createSession("platform-open", table.id(), "bar_1", null);
        assertThat(session.merchantId()).isEqualTo("__platform__");
    }

    @Test
    void scanOrderBindsOpenSessionInheritsEntryGateAndSourcesApp() {
        // 扫码点单（绑桌台会话 + 继承入场闸）：开启功能标志后，扫开台会话二维码下单 → 单据绑会话桌台、来源 app/miniapp、
        // 不重复核龄（继承场馆入场闸结论）；bar/桌台取自会话（扫码定位），客户端只传 items。
        BarTableRecord table = service.createTable(new BarTableRequest(
                "scan-table", "create", List.of(), "bar_1", "VIP 扫码", "card_seat", 50000L, 4, "active"));
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "scan-product", "create product", List.of(), "bar_1", "精酿", "beer",
                3800L, null, null, "active", null, null, false, null, null, null, null, null, null, null));
        Instant now = Instant.now();
        barRepository.saveTableSession(new BarTableSessionRecord(
                "session_scan", "bar_1", null, LocalDate.parse("2026-06-24"), table.id(), null, null,
                4, 0L, 0, null, null, "open", now, null, null, 1L, now, now));
        barOrderService.scanOrderEnabled = true;

        BarOrderRecord order = service.placeScanOrder("session_scan", new BarOrderRequest(
                "scan-order-1", "scan order", List.of(), null, null, null,
                List.of(Map.of("product_id", product.id(), "quantity", 2)), List.of(), "app"));

        assertThat(order.orderSource()).isEqualTo("app");
        assertThat(order.createdTerminal()).isEqualTo("miniapp");
        assertThat(order.tableId()).isEqualTo(table.id());
        assertThat(order.totalAmountCent()).isEqualTo(7600L);
    }

    @Test
    void scanOrderRejectedWhenFeatureFlagDisabled() {
        // 功能标志默认关 → 扫码点单被拒（上线前灰度/急停开关），不创建订单。
        Instant now = Instant.now();
        barRepository.saveTableSession(new BarTableSessionRecord(
                "session_scan_off", "bar_1", null, LocalDate.parse("2026-06-24"), "table_x", null, null,
                2, 0L, 0, null, null, "open", now, null, null, 1L, now, now));
        assertThatThrownBy(() -> service.placeScanOrder("session_scan_off", new BarOrderRequest(
                "scan-off", "x", List.of(), null, null, null,
                List.of(Map.of("product_id", "p", "quantity", 1)), List.of(), "app")))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .extracting("code").isEqualTo("BAR_SCAN_ORDER_DISABLED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // P0-2: settleBill state guard and amount cap
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void settleBillRejectsAlreadySettledBillToPreventDoubleSettlement() {
        BarBillRecord bill = billForOrder("already-settled", "staff_sales");
        service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-first", "first settlement", List.of(), "pay_1",
                bill.payableAmountCent(), "aggregate_pay"
        ));

        assertThatThrownBy(() -> service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-second", "duplicate settlement", List.of(), "pay_2",
                bill.payableAmountCent(), "aggregate_pay"
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_BILL_ALREADY_SETTLED");
    }

    @Test
    void settleBillRejectsAmountExceedingPayableToPreventOverpaymentManipulation() {
        BarBillRecord bill = billForOrder("overpay-settle", "staff_sales");

        assertThatThrownBy(() -> service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-overpay", "overpay", List.of(), "pay_overpay",
                bill.payableAmountCent() + 100_00L, "aggregate_pay"
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void settleBillRejectsWhenBelowTableMinimumConsumption() {
        // 2c-1 低消硬阻断：桌低消 50000（createReservation helper 内建），消费仅 10000 → 结算被拒。
        BarReservationRecord reservation = createReservation("mincon", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-mincon", "create product", List.of(), "bar_1", "Soda", "spirit",
                10000L, null, null, "active", null, null, false, null, null, null, null, null, null, null));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-mincon", "create order", List.of(), "bar_1", reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)), List.of(), "offline"));
        BarBillRecord bill = service.createOrderBill(order.id(), new BarOrderBillCreateRequest(
                "bill-mincon", "create bill", List.of(), "offline", false, 0));

        assertThatThrownBy(() -> service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-mincon", "settle", List.of(), "pay_m", bill.payableAmountCent(), "aggregate_pay")))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("BAR_MIN_CONSUMPTION_NOT_MET");
    }

    @Test
    void createProductRejectsDirectInternalPriceSinceV380() {
        // 批10/E：内部结算价唯一可编辑处 = 内部结算 SKU 目录。商品侧直填一律 400 fail-loud
        // （静默忽略会骗运营"以为配好了"）；无价商品照常可建（不参与结算/提成扣减）。
        assertThatThrownBy(() -> service.createProduct(new BarProductRequest(
                "product-internal", "create", List.of(), "bar_1", "House Beer", "beer",
                3800L, null, null, "active", null, null, false, null, null, null, 1500L, null, null, null)))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("BAR_INTERNAL_PRICE_CATALOG_ONLY");

        BarProductRecord noPrice = service.createProduct(new BarProductRequest(
                "product-no-internal", "create", List.of(), "bar_1", "House Beer", "beer",
                3800L, null, null, "active", null, null, false, null, null, null, null, null, null, null));
        assertThat(noPrice.internalSettlementPriceCent()).isNull();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // P1-1: list interface tenant isolation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void tableAvailabilityMarksActivelyReservedTables() {
        // BYOD 一期(2026-07-02):员工自有手机帮客人订位——空位视图=桌位+活跃预订占用标记;
        // 权限走 bar.reservation.create(能订位自然能看空位),不放大到桌位管理。
        TestSecurityActors.asMerchantUser("merchant_a");
        BarTableRecord freeTable = service.createTable(new BarTableRequest(
                "t1-avail-free", "create", List.of(), "bar_1", "卡座A1", "card_seat", 50000L, 4, "active"));
        BarTableRecord takenTable = service.createTable(new BarTableRequest(
                "t1-avail-taken", "create", List.of(), "bar_1", "散台A2", "loose_seat", 0L, 2, "active"));
        barRepository.activeReservedTableIds = java.util.Set.of(takenTable.id());

        java.util.List<BarTableAvailabilityRecord> rows = service.listTableAvailability().items();
        java.util.Map<String, Boolean> byId = new java.util.LinkedHashMap<>();
        for (BarTableAvailabilityRecord row : rows) {
            byId.put(row.id(), row.reserved());
        }
        assertThat(byId).containsEntry(freeTable.id(), false);
        assertThat(byId).containsEntry(takenTable.id(), true);
    }

    @Test
    void createReservationRejectsAnotherMerchantsTable() {
        // 审计 C-P1:卡座预订须校验桌位属调用方商户,否则恶意商户可把预订写到他人物理桌位。
        TestSecurityActors.asMerchantUser("merchant_b");
        BarTableRecord othersTable = service.createTable(new BarTableRequest(
                "table-cp1-b", "create", List.of(), "bar_1", "VIP B", "card_seat", 50000L, 2, "active"));

        TestSecurityActors.asMerchantUser("merchant_a");
        assertThatThrownBy(() -> service.createReservation(new BarReservationRequest(
                "resv-cp1", "create reservation", List.of(), "bar_1",
                othersTable.id(), Instant.parse("2030-06-20T21:00:00Z"), 2, "staff_sales", null, null, Instant.parse("2030-06-20T23:00:00Z"), "13800000000")))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    @Test
    void listTablesReturnsOnlyCurrentMerchantRecordsWhenScopedToMerchant() {
        TestSecurityActors.asMerchantUser("merchant_a");
        BarTableRecord merchantATable = service.createTable(new BarTableRequest(
                "table-isolation-a", "create", List.of(), "bar_1", "VIP A", "card_seat", 50000L, 2, "active"
        ));

        TestSecurityActors.asMerchantUser("merchant_b");
        service.createTable(new BarTableRequest(
                "table-isolation-b", "create", List.of(), "bar_1", "VIP B", "card_seat", 60000L, 2, "active"
        ));

        TestSecurityActors.asMerchantUser("merchant_a");
        PageResponse<BarTableRecord> result = service.listTables();

        assertThat(result.items())
                .extracting(BarTableRecord::id)
                .containsExactly(merchantATable.id());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 处理清单 #8：bar_tables 空间指派/回填（over_space 依赖 space_id）
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void assignTableSpaceSetsSpaceIdAndIsReadableForBacklog() {
        TestSecurityActors.asMerchantUser("merchant_a");
        BarTableRecord table = service.createTable(new BarTableRequest(
                "table-space-assign", "create", List.of(), "bar_1", "VIP S", "card_seat", 50000L, 2, "active"));
        barRepository.registerSpace("space_a", "merchant_a");

        // 回填前：出现在「未回填空间」清单。
        assertThat(service.listTablesWithoutSpace().items())
                .extracting(BarTableSpaceBacklogRecord::id)
                .contains(table.id());

        BarTableSpaceAssignmentRecord assigned = service.assignTableSpace(table.id(),
                new BarTableSpaceAssignmentRequest("assign-key", "space_a", "运营回填", List.of()));
        assertThat(assigned.spaceId()).isEqualTo("space_a");
        // 回读 bar_tables.space_id 命中目标空间。
        assertThat(barRepository.tableSpaceId(table.id())).isEqualTo("space_a");
        // 回填后：不再出现在「未回填空间」清单。
        assertThat(service.listTablesWithoutSpace().items())
                .extracting(BarTableSpaceBacklogRecord::id)
                .doesNotContain(table.id());
    }

    @Test
    void assignTableSpaceRejectsCrossMerchantSpaceWithForbiddenError() {
        TestSecurityActors.asMerchantUser("merchant_a");
        BarTableRecord table = service.createTable(new BarTableRequest(
                "table-space-cross", "create", List.of(), "bar_1", "VIP X", "card_seat", 50000L, 2, "active"));
        barRepository.registerSpace("space_b", "merchant_b");

        assertThatThrownBy(() -> service.assignTableSpace(table.id(),
                new BarTableSpaceAssignmentRequest("assign-cross", "space_b", null, List.of())))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    @Test
    void assignTableSpaceRejectsMissingSpaceWithNotFoundError() {
        TestSecurityActors.asMerchantUser("merchant_a");
        BarTableRecord table = service.createTable(new BarTableRequest(
                "table-space-missing", "create", List.of(), "bar_1", "VIP M", "card_seat", 50000L, 2, "active"));

        assertThatThrownBy(() -> service.assignTableSpace(table.id(),
                new BarTableSpaceAssignmentRequest("assign-missing", "space_ghost", null, List.of())))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SPACE_NOT_FOUND");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // P1-2: by-ID writes verify resource ownership
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void settleBillRejectsCrossMerchantAccessWithForbiddenError() {
        TestSecurityActors.asMerchantUser("merchant_a");
        BarBillRecord bill = billForOrder("cross-merchant-settle", "staff_sales");

        TestSecurityActors.asMerchantUser("merchant_b");
        assertThatThrownBy(() -> service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-cross", "cross settle", List.of(), "pay_x", bill.payableAmountCent(), "aggregate_pay"
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    @Test
    void deliverOrderRejectsCrossMerchantAccessWithForbiddenError() {
        TestSecurityActors.asMerchantUser("merchant_a");
        BarReservationRecord reservation = createReservation("cross-merchant-deliver", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-cross-deliver", "create", List.of(), "bar_1", "Gin", "spirit", 8800L,
                null, null, "active", null, null, false
        , null, null, null, null, null, null, null));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-cross-deliver", "create", List.of(), "bar_1", reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)), List.of(), "offline"
        ));
        String itemId = barRepository.listOrderItems(order.id()).getFirst().id();

        TestSecurityActors.asMerchantUser("merchant_b");
        assertThatThrownBy(() -> service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-cross", "cross deliver", List.of(),
                List.of(Map.of("order_item_id", itemId, "quantity", 1)), "staff_1", List.of()
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    // B1d④：下单引用他商户资源（桌位/商品/预定）必须拒（裁决：所有资源都属某商户，无 null-merchant 合法下单）。
    @Test
    void createOrderRejectsTableOwnedByAnotherMerchant() {
        TestSecurityActors.asMerchantUser("merchant_b");
        BarTableRecord bTable = service.createTable(new BarTableRequest(
                "table-cross-order", "create", List.of(), "bar_1", "VIP B2", "card_seat", 50000L, 2, "active"));

        TestSecurityActors.asMerchantUser("merchant_a");
        BarProductRecord aProduct = service.createProduct(new BarProductRequest(
                "product-own-a", "create", List.of(), "bar_1", "Gin A", "spirit", 8800L,
                null, null, "active", null, null, false, null, null, null, null, null, null, null));
        assertThatThrownBy(() -> service.createOrder(new BarOrderRequest(
                "order-cross-table", "create", List.of(), "bar_1", bTable.id(), null,
                List.of(Map.of("product_id", aProduct.id(), "quantity", 1)), List.of(), "offline")))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    @Test
    void createOrderRejectsProductOwnedByAnotherMerchant() {
        TestSecurityActors.asMerchantUser("merchant_b");
        BarProductRecord bProduct = service.createProduct(new BarProductRequest(
                "product-cross-order-b", "create", List.of(), "bar_1", "Rum B", "spirit", 7700L,
                null, null, "active", null, null, false, null, null, null, null, null, null, null));

        TestSecurityActors.asMerchantUser("merchant_a");
        BarTableRecord aTable = service.createTable(new BarTableRequest(
                "table-own-a", "create", List.of(), "bar_1", "VIP A2", "card_seat", 50000L, 2, "active"));
        assertThatThrownBy(() -> service.createOrder(new BarOrderRequest(
                "order-cross-product", "create", List.of(), "bar_1", aTable.id(), null,
                List.of(Map.of("product_id", bProduct.id(), "quantity", 1)), List.of(), "offline")))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    @Test
    void createOrderBlocksCategoryOutsideContractScope() {
        // 合同经营范围拦截接线：merchant_a 生效合同只许 beer → 卖 spirit 整单拒；范围内品类照常下单。
        com.complex.platform.contract.ContractRepository contractRepository =
                mock(com.complex.platform.contract.ContractRepository.class);
        Instant now = Instant.now();
        when(contractRepository.listContractsByMerchant("merchant_a")).thenReturn(List.of(
                new com.complex.platform.contract.ContractRecord("ctr_scope", "merchant_a", "subj", null, "subj",
                        LocalDate.now().minusDays(1), LocalDate.now().plusYears(1),
                        Map.of("allowedCategories", List.of("beer")), null, "none", null, Map.of(), List.of(),
                        null, null, List.of(), "effective", null, null, null, null, now, now)));
        barOrderService.contractScopeGuard = new com.complex.platform.contract.ContractScopeGuard(contractRepository);
        try {
            TestSecurityActors.asMerchantUser("merchant_a");
            BarTableRecord table = service.createTable(new BarTableRequest(
                    "table-scope", "create", List.of(), "bar_1", "范围桌", "card_seat", 50000L, 2, "active"));
            BarProductRecord spirit = service.createProduct(new BarProductRequest(
                    "product-scope-spirit", "create", List.of(), "bar_1", "金酒", "spirit", 8800L,
                    null, null, "active", null, null, false, null, null, null, null, null, null, null));
            assertThatThrownBy(() -> service.createOrder(new BarOrderRequest(
                    "order-scope-x", "create", List.of(), "bar_1", table.id(), null,
                    List.of(Map.of("product_id", spirit.id(), "quantity", 1)), List.of(), "offline")))
                    .isInstanceOfSatisfying(ApiException.class,
                            e -> assertThat(e.code()).isEqualTo("CONTRACT_SCOPE_VIOLATION"));
            BarProductRecord beer = service.createProduct(new BarProductRequest(
                    "product-scope-beer", "create", List.of(), "bar_1", "精酿", "beer", 3800L,
                    null, null, "active", null, null, false, null, null, null, null, null, null, null));
            BarOrderRecord ok = service.createOrder(new BarOrderRequest(
                    "order-scope-ok", "create", List.of(), "bar_1", table.id(), null,
                    List.of(Map.of("product_id", beer.id(), "quantity", 1)), List.of(), "offline"));
            assertThat(ok.totalAmountCent()).isEqualTo(3800L);
        } finally {
            barOrderService.contractScopeGuard = null;
        }
    }

    @Test
    void createProductRejectsCategoryOutsideVocabulary() {
        // I-2: 品类是合同范围拦截比对基准——新建商品词表外品类（自报绕过）→ 拒。
        assertThatThrownBy(() -> service.createProduct(new BarProductRequest(
                "product-bad-cat", "create", List.of(), "bar_1", "私酿", "洋酒", 8800L,
                null, null, "active", null, null, false, null, null, null, null, null, null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    void addOrderItemFallsBackToTableMerchantWhenOrderMerchantMissing() {
        // S-2: 平台直营下单（订单商户为空）→ 加菜与创建同口径回退桌台归属商户查合同，越界仍拦。
        com.complex.platform.contract.ContractRepository contractRepository =
                mock(com.complex.platform.contract.ContractRepository.class);
        Instant now = Instant.now();
        when(contractRepository.listContractsByMerchant("merchant_a")).thenReturn(List.of(
                new com.complex.platform.contract.ContractRecord("ctr_scope_fb", "merchant_a", "subj", null, "subj",
                        LocalDate.now().minusDays(1), LocalDate.now().plusYears(1),
                        Map.of("allowedCategories", List.of("beer")), null, "none", null, Map.of(), List.of(),
                        null, null, List.of(), "effective", null, null, null, null, now, now)));
        try {
            TestSecurityActors.asMerchantUser("merchant_a");
            BarTableRecord table = service.createTable(new BarTableRequest(
                    "table-scope-fb", "create", List.of(), "bar_1", "回退桌", "card_seat", 50000L, 2, "active"));
            BarProductRecord beer = service.createProduct(new BarProductRequest(
                    "product-scope-fb-beer", "create", List.of(), "bar_1", "精酿", "beer", 3800L,
                    null, null, "active", null, null, false, null, null, null, null, null, null, null));
            BarProductRecord spirit = service.createProduct(new BarProductRequest(
                    "product-scope-fb-spirit", "create", List.of(), "bar_1", "金酒", "spirit", 8800L,
                    null, null, "active", null, null, false, null, null, null, null, null, null, null));
            barOrderService.contractScopeGuard =
                    new com.complex.platform.contract.ContractScopeGuard(contractRepository);
            // 平台账号下单：订单商户为空，创建时按桌台归属商户比对（beer 在范围内 → 成单）。
            TestSecurityActors.asPlatformAdmin();
            BarOrderRecord order = service.createOrder(new BarOrderRequest(
                    "order-scope-fb", "create", List.of(), "bar_1", table.id(), null,
                    List.of(Map.of("product_id", beer.id(), "quantity", 1)), List.of(), "offline"));
            assertThat(order.merchantId()).isNull();
            // 加菜越界品类：修复前 order.merchantId()=null 会跳过拦截 → 现回退桌台商户仍拦。
            assertThatThrownBy(() -> service.addOrderItem(order.id(), new BarOrderRequest(
                    "order-scope-fb-add", "create", List.of(), "bar_1", table.id(), null,
                    List.of(Map.of("product_id", spirit.id(), "quantity", 1)), List.of(), "offline")))
                    .isInstanceOfSatisfying(ApiException.class,
                            e -> assertThat(e.code()).isEqualTo("CONTRACT_SCOPE_VIOLATION"));
        } finally {
            barOrderService.contractScopeGuard = null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // P1-3: audit attribution comes from auth token, not request body
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void waiverRejectsNegativeAmountWithValidationFailed() {
        // P1-13：金额下界负向测试——免单(waiver)负数金额必须被 positive() 拒绝（防负额免单=变相加钱）。
        BarBillRecord bill = billForOrder("neg-waiver", "staff_sales");
        assertThatThrownBy(() -> service.applyBillAdjustment(
                bill.id(),
                new BarBillAdjustmentRequest("bill-neg-waiver", "neg", List.of(), "waiver", -5000L, "neg waiver", null)))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void billAdjustmentRecordsAuthenticatedActorAsRequestedByUserIdIgnoringRequestBody() {
        BarBillRecord bill = billForOrder("audit-actor", "staff_sales");

        BarBillAdjustmentRecord adjustment = service.applyBillAdjustment(
                bill.id(),
                new BarBillAdjustmentRequest(
                        "bill-audit-actor",
                        "test audit actor",
                        List.of(),
                        "service_fee",
                        5000L,
                        "audit test",
                        "injected_attacker_id"
                )
        );

        assertThat(adjustment.requestedByUserId())
                .as("requestedByUserId must come from the auth token, not the request body")
                .isEqualTo("usr_admin");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Existing tests — preserved intact with updated assertions
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void verifyReservationEntryAllowanceDoesNotConsumeQuotaAndReportsRemaining() {
        BarReservationRecord reservation = createReservation("reservation-verify", "bar_1", 1);

        BarReservationEntryAllowanceVerificationRecord verified = service.verifyReservationEntryAllowance(
                reservation.id(),
                verification("verify-1", "guest_1", "bar_1")
        );

        assertThat(verified.allowed()).isTrue();
        assertThat(verified.reasonCode()).isEqualTo("allowed");
        assertThat(verified.checkedInCount()).isZero();
        assertThat(verified.remainingGuestCount()).isEqualTo(1);
        assertThat(barRepository.guestCheckins).isEmpty();
        assertThat(barRepository.lockedReservations).isEmpty();
    }

    @Test
    void verifyReservationEntryAllowanceRejectsWrongBarCancelledAndExhaustedWithoutConsuming() {
        BarReservationRecord reservation = createReservation("reservation-denied", "bar_1", 1);

        BarReservationEntryAllowanceVerificationRecord wrongBar = service.verifyReservationEntryAllowance(
                reservation.id(),
                verification("verify-wrong-bar", "guest_1", "bar_2")
        );
        assertThat(wrongBar.allowed()).isFalse();
        assertThat(wrongBar.reasonCode()).isEqualTo("BAR_SCOPE_MISMATCH");

        service.checkInReservationGuest(reservation.id(), verification("checkin-quota", "guest_1", "bar_1"));
        BarReservationEntryAllowanceVerificationRecord exhausted = service.verifyReservationEntryAllowance(
                reservation.id(),
                verification("verify-exhausted", "guest_2", "bar_1")
        );
        assertThat(exhausted.allowed()).isFalse();
        assertThat(exhausted.reasonCode()).isEqualTo("FREE_GUEST_QUOTA_EXHAUSTED");
        assertThat(exhausted.remainingGuestCount()).isZero();

        BarReservationRecord cancelled = barRepository.saveReservation(new BarReservationRecord(
                "reservation_cancelled", "bar_1", null, "table_1", "customer_1", Instant.now(),
                1, 1, 0L, "not_required", "staff", null, "cancelled", "web", null,
                List.of(), Instant.now(), Instant.now(), null));
        BarReservationEntryAllowanceVerificationRecord cancelledResult = service.verifyReservationEntryAllowance(
                cancelled.id(),
                verification("verify-cancelled", "guest_3", "bar_1")
        );
        assertThat(cancelledResult.allowed()).isFalse();
        assertThat(cancelledResult.reasonCode()).isEqualTo("BAR_RESERVATION_NOT_CHECKINABLE");
        assertThat(barRepository.guestCheckins).hasSize(1);
    }

    @Test
    void checkInLocksReservationBeforeConsumingFreeGuestQuota() {
        BarReservationRecord reservation = createReservation("reservation-lock", "bar_1", 1);

        service.checkInReservationGuest(reservation.id(), verification("checkin-lock", "guest_1", "bar_1"));

        assertThat(barRepository.lockedReservations).contains(reservation.id());
    }

    @Test
    void checkInConsumesOnlyReservationFreeGuestQuotaAndRejectsExhaustion() {
        BarReservationRecord reservation = createReservation("reservation-quota", "bar_1", 1);

        BarTableGuestCheckinRecord first = service.checkInReservationGuest(
                reservation.id(),
                verification("checkin-1", "guest_1", "bar_1")
        );

        assertThat(first.checkedInCountAfter()).isEqualTo(1);
        assertThatThrownBy(() -> service.checkInReservationGuest(
                reservation.id(),
                verification("checkin-2", "guest_2", "bar_1")
        ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("FREE_GUEST_QUOTA_EXHAUSTED");
        assertThat(barRepository.guestCheckins).hasSize(1);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("bar.table_guest.checked_in", "bar.table_guest.rejected");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // R5 T16 — 订台定金闭环
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void staffReservationWaivesDepositEvenWithRatioRule() {
        // 第十八批 C 段第 15 条（BSM 第 1062 行「员工代客订台不收定金」；ENUM 第 103 行「免收（员工代订）」）：
        // 本酒吧配了低消比例定金规则（30%），员工代订仍免收，金额 0、无付款意向、不出现定金待付审计。
        service.createDepositRule(new BarDepositRuleRequest(
                "deposit-rule-ratio", "rule", List.of(), "bar_t16",
                "bar", "bar_t16", "min_consumption_ratio", null, 3000, 60));
        BarTableRecord table = service.createTable(new BarTableRequest(
                "table-t16-ratio", "create table", List.of(), "bar_t16",
                "VIP RATIO", "card_seat", 100000L, 2, "active"));
        BarReservationRecord reservation = service.createReservation(new BarReservationRequest(
                "resv-t16-ratio", "create reservation", List.of(), "bar_t16",
                table.id(), Instant.parse("2026-06-20T21:00:00Z"), 2, "staff_sales", null, null, Instant.parse("2030-06-20T23:00:00Z"), "13800000000"));

        assertThat(reservation.depositAmountCent()).isZero();
        assertThat(reservation.depositStatus()).isEqualTo("staff_waived");
        assertThat(reservation.depositIntentId()).isNull();
        assertThat(reservation.depositPaidAt()).isNull();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .doesNotContain("bar.reservation_deposit_required");
    }

    @Test
    void checkInKeepsDepositPaidAndDoesNotDeduct() {
        // 第十批 TK-24-R1（BSM L889「定金不抵扣消费」）：到店只留痕，定金保持已支付待判，不再标 settled/deducted。
        BarReservationRecord reservation = reservationWithFixedDeposit("settle", 20000L, 30);

        BarReservationRecord paid = service.payReservationDeposit(reservation.id(),
                new BarReservationDepositActionRequest("pay-settle", "pay", List.of(), "aggregate_pay"));
        assertThat(paid.depositStatus()).isEqualTo("paid");
        assertThat(paid.depositPaidAt()).isNotNull();
        assertThat(paid.depositChannel()).isEqualTo("aggregate_pay");

        BarReservationRecord checkedIn = service.checkInReservationDeposit(reservation.id(),
                new BarReservationDepositActionRequest("checkin-settle", "checkin", List.of(), null));
        assertThat(checkedIn.depositStatus()).isEqualTo("paid");
        assertThat(checkedIn.depositSettlement()).isNull();
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName)
                .contains("bar.reservation_deposit_paid", "bar.reservation_checked_in")
                .doesNotContain("bar.reservation_deposit_deducted");
    }

    @Test
    void cancelBeforeFreeWindowRegistersOriginalRouteRefund() {
        // 免没收时限 30 分钟，预约在远未来 → 取消属早于时限 → 按原付款渠道登记原路待退；第十四批 OBS-13-03-R6（BSM L1062
        // 「退款处理中的记录继续关联原付款，只有实际退款成功才记为已退」）：定金保持已支付（退款处理中），不当场记已退。
        BarReservationRecord reservation = reservationWithFixedDeposit("refund", 20000L, 30);
        service.payReservationDeposit(reservation.id(),
                new BarReservationDepositActionRequest("pay-refund", "pay", List.of(), "aggregate_pay"));
        when(financeServiceStub.recordRejectedTicketPaymentRefund(org.mockito.ArgumentMatchers.eq("bar_deposit_rejection"),
                org.mockito.ArgumentMatchers.eq(reservation.id()), org.mockito.ArgumentMatchers.eq(20000L),
                org.mockito.ArgumentMatchers.eq("aggregate_pay"), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), anyString()))
                .thenReturn("refund_rejected_stub");

        BarReservationRecord cancelled = service.cancelReservation(reservation.id(),
                new BarReservationCancelRequest("cancel-refund", "cancel", List.of(), false, "customer"));
        assertThat(cancelled.status()).isEqualTo("cancelled");
        assertThat(cancelled.depositStatus()).isEqualTo("paid");
        assertThat(auditEventRepository.events).filteredOn(e -> "bar.reservation_deposit_refunded".equals(e.eventName()))
                .singleElement().satisfies(e -> assertThat(e.metadata()).containsEntry("refund_status", "requested")
                        .containsEntry("refund_id", "refund_rejected_stub"));
    }

    @Test
    void noShowForfeitsDeposit() {
        BarReservationRecord reservation = reservationWithFixedDeposit("forfeit", 20000L, 30);
        service.payReservationDeposit(reservation.id(),
                new BarReservationDepositActionRequest("pay-forfeit", "pay", List.of(), "aggregate_pay"));

        BarReservationRecord cancelled = service.cancelReservation(reservation.id(),
                new BarReservationCancelRequest("cancel-forfeit", "no-show", List.of(), true, "no_show"));
        assertThat(cancelled.depositSettlement()).isEqualTo("forfeited");
        assertThat(cancelled.depositStatus()).isEqualTo("settled");
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName)
                .contains("bar.reservation_deposit_forfeited");
    }

    @Test
    void payDepositRejectsWhenNotPending() {
        BarReservationRecord reservation = createReservation("resv-t16-nopay", "bar_norule", 2);
        assertThatThrownBy(() -> service.payReservationDeposit(reservation.id(),
                new BarReservationDepositActionRequest("pay-bad", "pay", List.of(), "aggregate_pay")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_DEPOSIT_NOT_PAYABLE");
    }

    private BarReservationRecord reservationWithFixedDeposit(String key, long fixedAmountCent, int freeMinutes) {
        service.createDepositRule(new BarDepositRuleRequest(
                "deposit-rule-" + key, "rule", List.of(), "bar_" + key,
                "bar", "bar_" + key, "fixed", fixedAmountCent, null, freeMinutes));
        BarTableRecord table = service.createTable(new BarTableRequest(
                "table-" + key, "create table", List.of(), "bar_" + key,
                "VIP " + key, "card_seat", 100000L, 2, "active"));
        return pendingDeposit(service.createReservation(new BarReservationRequest(
                "resv-" + key, "create reservation", List.of(), "bar_" + key,
                table.id(), Instant.parse("2030-06-20T21:00:00Z"), 2, "staff_sales", null, null, Instant.parse("2030-06-20T23:00:00Z"), "13800000000")),
                fixedAmountCent);
    }

    /** 第十八批 C 段第 15 条：员工代订一律免收定金；定金链用例直接把订台置为待支付造数（客人自助订台的定金形态）。 */
    private BarReservationRecord pendingDeposit(BarReservationRecord r, long amountCent) {
        return barRepository.saveReservation(new BarReservationRecord(r.id(), r.barId(), r.merchantId(), r.tableId(), r.customerId(),
                r.reservedAt(), r.guestCount(), r.allowedPassFreeGuestCount(), amountCent, "pending", r.orderSource(), r.salesStaffId(),
                r.status(), r.createdTerminal(), r.deviceId(), r.evidenceAttachmentIds(), r.createdAt(), r.updatedAt(), r.depositRule(),
                r.depositIntentId(), r.depositPaidAt(), r.depositSettlement(), r.invitationToken(), r.invitationSignature(),
                r.invitationCapacity(), r.invitationStatus(), r.invitationIssuedAt(), r.archivedAt(), r.archivedBy(), r.deleteStatus(),
                r.deleteRequestedBy(), r.deleteRequestedAt(), r.deleteReason(), r.depositChannel(), r.depositSettledAt(),
                r.latestArrivalAt(), r.holdStatus()));
    }

    @Test
    void checkInRejectsGuestWhenVerificationBarDiffersFromReservationBar() {
        BarReservationRecord reservation = createReservation("reservation-scope", "bar_1", 2);

        assertThatThrownBy(() -> service.checkInReservationGuest(
                reservation.id(),
                verification("checkin-cross-bar", "guest_1", "bar_2")
        ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_SCOPE_MISMATCH");

        assertThat(barRepository.guestCheckins).isEmpty();
    }

    @Test
    void billUsesOrderCentsAndReservationDepositWithoutTicketOrMinimumSpendOffset() {
        BarReservationRecord reservation = createReservation("reservation-bill", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-bill",
                "create product",
                List.of(),
                "bar_1",
                "Whisky",
                "spirit",
                128800L,
                null,
                null,
                "active",
                null,
                null,
                false
        , null, null, null, null, null, null, null));

        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-bill",
                "create order",
                List.of(),
                "bar_1",
                reservation.tableId(),
                reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 2)),
                List.of("staff_service"),
                "offline"
        ));
        BarBillRecord bill = service.createOrderBill(order.id(), new BarOrderBillCreateRequest(
                "bill-create",
                "create bill",
                List.of(),
                "offline",
                true,
                0
        ));

        assertThat(order.totalAmountCent()).isEqualTo(257600L);
        assertThat(bill.subtotalAmountCent()).isEqualTo(257600L);
        assertThat(bill.depositDeductAmountCent()).isZero();
        assertThat(bill.payableAmountCent()).isEqualTo(257600L);
        assertThat(bill.paymentStatus()).isEqualTo("unpaid");
    }

    @Test
    void salesCommissionNoLongerPostsPerOrderEmployeePayable() {
        // CG-07（BSM「营销人员的归属与报酬」「派驻人员的成本与报酬」）：提成按月由酒吧拨付人员所属主体（内部结算应付），
        // 计提时不再按单对员工本人挂 finance 应付；提成金额仍留在 bar_commission_calculations 供工资单与月结汇总。
        BarBillRecord bill = billForOrder("commission-payable", "staff_sales");
        BarCommissionCalculationRecord saved = service.calculateCommission(bill.id(),
                new CommissionCalculateRequest("commission-payable-1", bill.id(), true, false));
        assertThat(saved.commissionAmountCent()).isPositive();
        assertThat(receivableLedgerService.ledgerPostings).isEmpty();
    }

    // 第十批 TK-28（ENUM L108「券抵扣的金额不计入…销售提成基数」；BSM L1064「不倒扣达标进度」）：
    // 结账带折扣券抵扣 5000 后，提成基数 = subtotal − discount；桌低消 50000 与消费 128800 的判定不受抵扣影响。
    @Test
    void voucherDeductionExcludedFromCommissionBaseAndMinSpend() {
        BarBillRecord bill = billForOrder("voucher-commission", "staff_sales");
        com.complex.platform.voucher.VoucherService voucherService = mock(com.complex.platform.voucher.VoucherService.class);
        when(voucherService.redeemForBill(org.mockito.ArgumentMatchers.eq("VCDISC"), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("bar_bill"), org.mockito.ArgumentMatchers.eq(bill.id()),
                org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(new com.complex.platform.voucher.VoucherRedemptionEntryRecord(
                        "vrr_vi_disc", "vi_disc", "vdef_disc", "merchant_discount", "customer-1", null, "usr_admin",
                        "in_store_staff", "none", null, null, null, null, null, Instant.now(), Instant.now(),
                        "bar_bill", bill.id(), 5000L));
        billingService.voucherService = voucherService;

        BarBillRecord settled = service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-voucher-commission", "settle", List.of(), "pay_voucher", bill.payableAmountCent() - 5000L,
                "aggregate_pay", null, null, null, null, "VCDISC", null));
        assertThat(settled.discountAmountCent()).isEqualTo(5000L);
        assertThat(settled.payableAmountCent()).isEqualTo(bill.payableAmountCent() - 5000L);
        assertThat(settled.settlementStatus()).isEqualTo("settled");

        BarCommissionCalculationRecord commission = service.calculateCommission(bill.id(),
                new CommissionCalculateRequest("commission-voucher-1", bill.id(), true, false));
        assertThat(commission.commissionBaseCent()).isEqualTo(bill.subtotalAmountCent() - 5000L);
    }

    @Test
    void calculateCommissionIsIdempotentPerBill() {
        // 2c-2 提成防重：同一账单不同幂等键重复计提 → 返回首次结果，不重复建提成；计提全程不向 finance 台账过账（CG-07）。
        BarBillRecord bill = billForOrder("commission-dup", "staff_sales");
        BarCommissionCalculationRecord first = service.calculateCommission(bill.id(),
                new CommissionCalculateRequest("commission-dup-1", bill.id(), true, false));
        BarCommissionCalculationRecord second = service.calculateCommission(bill.id(),
                new CommissionCalculateRequest("commission-dup-2", bill.id(), true, false));
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(receivableLedgerService.ledgerPostings).isEmpty();
    }

    @Test
    void commissionUsesSalesRateForSalesPositionStaffAndNonSalesRateForEveryoneElse() {
        // G/V349：档位按【员工岗位】判——员工档案里标了销售岗 → 销售人员费率(15%)。
        BarBillRecord salesBill = billForOrder("sales-order", "staff_sales");
        BarCommissionCalculationRecord salesCommission = service.calculateCommission(
                salesBill.id(),
                new CommissionCalculateRequest("commission-sales", salesBill.id(), true, false)
        );

        // 归属人在员工档案里没标销售岗 → 非销售人员费率(10%)（无归属的账单不计提成，86-R4）。
        BarBillRecord nonSalesBill = billForOrder("service-order", "staff_non_sales");
        BarCommissionCalculationRecord nonSalesCommission = service.calculateCommission(
                nonSalesBill.id(),
                new CommissionCalculateRequest("commission-non-sales", nonSalesBill.id(), true, false)
        );

        assertThat(salesCommission)
                .extracting(
                        BarCommissionCalculationRecord::salesRoleType,
                        BarCommissionCalculationRecord::commissionRateBp,
                        BarCommissionCalculationRecord::commissionAmountCent
                )
                .containsExactly("sales", 1500, 19320L);
        assertThat(nonSalesCommission)
                .extracting(
                        BarCommissionCalculationRecord::salesRoleType,
                        BarCommissionCalculationRecord::commissionRateBp,
                        BarCommissionCalculationRecord::commissionAmountCent
                )
                .containsExactly("non_sales", 1000, 12880L);
    }

    @Test
    void nonSalesPositionStaffAssignedAsTableSellerGetsNonSalesRate() {
        // G/V349 本批的病根用例（改前方向是反的）：非销售岗员工【被归属为】卡座销售。
        // 改前按「挂没挂 salesStaffId」判 → 挂了人就 sales 拿 15%，多发 5 个点；
        // 改后按员工档案的销售岗标识判 → 这人没标销售岗 → 非销售人员费率 10%。
        BarBillRecord bill = billForOrder("assigned-non-sales-order", "staff_non_sales");
        BarCommissionCalculationRecord commission = service.calculateCommission(
                bill.id(), new CommissionCalculateRequest("commission-assigned-non-sales", bill.id(), true, false));

        assertThat(bill.salesStaffId()).isEqualTo("staff_non_sales");   // 归属人确实挂上了
        assertThat(bill.salesRoleType()).isEqualTo("non_sales");        // 开单即按岗位落档
        assertThat(commission)
                .extracting(
                        BarCommissionCalculationRecord::salesRoleType,
                        BarCommissionCalculationRecord::commissionRateBp,
                        BarCommissionCalculationRecord::commissionAmountCent
                )
                .containsExactly("non_sales", 1000, 12880L);
    }

    @Test
    void unknownSalesStaffFallsBackToNonSalesRate() {
        // 「其余按非销售人员费率」：归属人在员工档案里查无此人 → 非销售档，不因判不出岗就按高档发钱。
        BarBillRecord bill = billForOrder("unknown-staff-order", "staff_not_in_hr");
        BarCommissionCalculationRecord commission = service.calculateCommission(
                bill.id(), new CommissionCalculateRequest("commission-unknown-staff", bill.id(), true, false));
        assertThat(commission.salesRoleType()).isEqualTo("non_sales");
        assertThat(commission.commissionRateBp()).isEqualTo(1000);
    }

    @Test
    void salesRoleIsSnapshotAtBillCreationSoLaterPositionChangeDoesNotRewriteHistory() {
        // 开单时快照角色档；此后人事把这人改成销售岗，已开账单的计提仍按开单时的档——不悄悄改写历史提成。
        BarBillRecord bill = billForOrder("snapshot-order", "staff_non_sales");
        when(staffRepositoryStub.findProfileByUserId("staff_non_sales"))
                .thenReturn(Optional.of(staffProfile("staff_non_sales", true)));

        BarCommissionCalculationRecord commission = service.calculateCommission(
                bill.id(), new CommissionCalculateRequest("commission-snapshot", bill.id(), true, false));
        assertThat(commission.salesRoleType()).isEqualTo("non_sales");
        assertThat(commission.commissionRateBp()).isEqualTo(1000);

        // 改岗之后【新开】的单才走销售档。
        BarBillRecord afterChange = billForOrder("snapshot-order-after", "staff_non_sales");
        assertThat(afterChange.salesRoleType()).isEqualTo("sales");
    }

    @Test
    void commissionFailsLoudWhenStaffProfileLookupMissing() {
        // 资金级 fail-closed：判不出岗位时【拒绝开单】，绝不默认一个档照发提成。
        billingService.staffRepository = null;
        assertThatThrownBy(() -> billForOrder("staff-lookup-missing", "staff_sales"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("员工档案不可用");
    }

    @Test
    void orderListCarriesRealPersistedCommissionRateForReconciliation() {
        // 对账铁律：后台页面显示的计提比例必须 == 提成台账里真实落库的 commission_rate_bp。
        // 页面此前把 15%/10% 写死在文案里，配置中心改成 20% 后页面照旧显示 15%，页面与台账对不上。
        commissionRateStub.salesBp = 2000;
        BarBillRecord bill = billForOrder("reconcile-order", "staff_sales");
        BarCommissionCalculationRecord commission = service.calculateCommission(
                bill.id(), new CommissionCalculateRequest("commission-reconcile", bill.id(), true, false));

        BarBillSummary summary = service.listOrders().items().stream()
                .filter(view -> view.id().equals(bill.orderId()))
                .findFirst().orElseThrow()
                .bills().stream().filter(b -> b.id().equals(bill.id())).findFirst().orElseThrow();

        assertThat(summary.salesRoleType()).isEqualTo("sales");
        assertThat(summary.commissionRateBp()).isEqualTo(commission.commissionRateBp()).isEqualTo(2000);
        assertThat(summary.commissionAmountCent()).isEqualTo(commission.commissionAmountCent());
    }

    @Test
    void orderListShowsNoCommissionRateBeforeCalculation() {
        // 未计提的账单不编数字：费率/金额为空，页面显示「未计提」。
        BarBillRecord bill = billForOrder("uncalculated-order", "staff_sales");
        BarBillSummary summary = service.listOrders().items().stream()
                .filter(view -> view.id().equals(bill.orderId()))
                .findFirst().orElseThrow()
                .bills().stream().filter(b -> b.id().equals(bill.id())).findFirst().orElseThrow();
        assertThat(summary.commissionRateBp()).isNull();
        assertThat(summary.commissionAmountCent()).isNull();
    }

    @Test
    void commissionRateComesFromConfigCenterNotHardcode() {
        // P0-D/CFG-01（V340）：改后台配置（销售 20% / 非销售 5%）→ 计提立即按新费率，证明费率不再写死在代码里。
        commissionRateStub.salesBp = 2000;
        commissionRateStub.nonSalesBp = 500;

        BarBillRecord salesBill = billForOrder("cfg-sales-order", "staff_sales");
        BarCommissionCalculationRecord salesCommission = service.calculateCommission(
                salesBill.id(), new CommissionCalculateRequest("cfg-commission-sales", salesBill.id(), true, false));
        BarBillRecord nonSalesBill = billForOrder("cfg-service-order", "staff_non_sales");
        BarCommissionCalculationRecord nonSalesCommission = service.calculateCommission(
                nonSalesBill.id(), new CommissionCalculateRequest("cfg-commission-non-sales", nonSalesBill.id(), true, false));

        // 基数与 commissionUsesFifteenPercent… 用例同为 128800 分：20% → 25760；5% → 6440。
        assertThat(salesCommission.commissionBaseCent()).isEqualTo(128800L);
        assertThat(salesCommission.commissionRateBp()).isEqualTo(2000);
        assertThat(salesCommission.commissionAmountCent()).isEqualTo(25760L);
        assertThat(nonSalesCommission.commissionRateBp()).isEqualTo(500);
        assertThat(nonSalesCommission.commissionAmountCent()).isEqualTo(6440L);
        // 费率按「账单归属商户 × 角色档」解析（作用域取自账单，不来自请求体）。
        assertThat(commissionRateStub.lastRoleType).isEqualTo("non_sales");
        assertThat(commissionRateStub.lastMerchantId).isEqualTo(nonSalesBill.merchantId());
    }

    @Test
    void commissionFailsLoudWhenRateConfigPortMissing() {
        // 资金级 fail-closed：配置端口不可用时【拒绝计提】而非静默回落硬编码 15%/10%（零硬编码母铁律）。
        BarBillRecord bill = billForOrder("cfg-missing", "staff_sales");
        billingService.commissionRateConfigPort = null;
        assertThatThrownBy(() -> service.calculateCommission(bill.id(),
                new CommissionCalculateRequest("cfg-commission-missing", bill.id(), true, false)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("提成费率配置不可用");
    }

    @Test
    void commissionDeductsGiftCostAtInternalPriceFromBase() {
        // F-TXN-05 赠菜扣提成：随单赠送项对客 amount_cent=0 不计 subtotal，其成本再按商品内部结算价 × 数量
        // 从提成基数扣除（赠送项成本不计提成）。
        BarReservationRecord reservation = createReservation("gift-commission", "bar_1", 2);
        BarProductRecord sold = service.createProduct(new BarProductRequest(
                "product-gift-sold", "create product", List.of(), "bar_1", "Whisky", "spirit",
                100000L, null, null, "active", null, null, false, null, null, null, null, null, null, null));
        // 赠送商品：售价 20000/份、内部结算价 8000/份。
        // 批10/E：内部价快照由系统写（目录转正传播）——测试直接落快照模拟收编后的状态（商品侧无直填入口）。
        BarProductRecord giftCreated = service.createProduct(new BarProductRequest(
                "product-gift-comp", "create product", List.of(), "bar_1", "Fruit Plate", "beer",
                20000L, null, null, "active", null, null, false, null, null, null, null, null, null, null));
        BarProductRecord gift = new BarProductRecord(giftCreated.id(), giftCreated.barId(), giftCreated.merchantId(),
                giftCreated.productName(), giftCreated.productType(), giftCreated.priceCent(), giftCreated.currency(),
                giftCreated.inventoryItemId(), giftCreated.stockLocationId(), giftCreated.stockDeductionRequired(),
                giftCreated.status(), giftCreated.createdAt(), giftCreated.updatedAt(),
                giftCreated.standardPourVolume(), giftCreated.pourVolumeUnit(), giftCreated.lossThreshold(),
                giftCreated.soldOut(), giftCreated.soldOutAt(), giftCreated.soldOutReason(), 8000L,
                giftCreated.categoryId(), giftCreated.imageUrl());
        barRepository.saveProduct(gift);
        BarOrderRecord order = withSales(service.createOrder(new BarOrderRequest(
                "order-gift-commission", "create order", List.of(), "bar_1",
                reservation.tableId(), reservation.id(),
                List.of(
                        Map.of("product_id", sold.id(), "quantity", 1),
                        Map.of("product_id", gift.id(), "quantity", 2, "is_gift", true)
                ),
                List.of(), "offline")), "staff_sales");

        // 赠送项落库：金额 0、单价保留售价、isGift=true、数量保留。
        BarOrderItemRecord giftItem = barRepository.listOrderItems(order.id()).stream()
                .filter(BarOrderItemRecord::isGift).findFirst().orElseThrow();
        assertThat(giftItem.amountCent()).isZero();
        assertThat(giftItem.unitPriceCent()).isEqualTo(20000L);
        assertThat(giftItem.quantity()).isEqualTo(2);
        // 赠送项 0 元 → 不进订单总额/账单 subtotal（仅售卖项 100000）。
        assertThat(order.totalAmountCent()).isEqualTo(100000L);

        BarBillRecord bill = service.createOrderBill(order.id(), new BarOrderBillCreateRequest(
                "bill-gift-commission", "create bill", List.of(), "offline", false, 0));
        assertThat(bill.subtotalAmountCent()).isEqualTo(100000L);

        BarCommissionCalculationRecord commission = service.calculateCommission(bill.id(),
                new CommissionCalculateRequest("commission-gift", bill.id(), true, false));
        // base = 100000 − 赠菜成本(8000 × 2 = 16000) = 84000；销售 15% → 12600。
        assertThat(commission.commissionBaseCent()).isEqualTo(84000L);
        assertThat(commission.commissionAmountCent()).isEqualTo(12600L);
    }

    @Test
    void cardSeatInvitationIssueEntryEnforcesCapacityAndScope() {
        // V155 订卡邀请函：签发→持函人占一名额→门口入场核验（已入场≤capacity）；未确认/超容量/跨吧均拒。
        BarReservationRecord reservation = createReservation("inv-cap", "bar_1", 2); // 卡座 capacity=2
        BarReservationInvitationRecord invitation = service.issueReservationInvitation(
                reservation.id(), new BarInvitationActionRequest("inv-issue", null));
        assertThat(invitation.status()).isEqualTo("active");
        assertThat(invitation.capacity()).isEqualTo(2);
        assertThat(invitation.invitationToken()).isNotBlank();
        assertThat(invitation.invitationSignature()).isNotBlank();
        assertThat(invitation.confirmedCount()).isEqualTo(1); // 持函人自动占一个名额
        String holderMemberId = barRepository.listInvitationMembers(reservation.id()).stream()
                .filter(BarReservationInvitationMemberRecord::isHolder).findFirst().orElseThrow().id();

        // 模拟朋友确认（客人端 confirm 落成员，此处直接落库以测门口入场闸）。
        Instant now = Instant.parse("2026-06-20T21:30:00Z");
        barRepository.saveInvitationMember(new BarReservationInvitationMemberRecord(
                "invm-friend-1", reservation.id(), "bar_1", "friend-1", "cust-friend-1",
                false, "confirmed", now, null, null, now, now));
        barRepository.saveInvitationMember(new BarReservationInvitationMemberRecord(
                "invm-friend-2", reservation.id(), "bar_1", "friend-2", "cust-friend-2",
                false, "confirmed", now, null, null, now, now));

        // 入场：持函人 + friend-1（2 = capacity），各凭本人动态入场码。
        assertThat(service.verifyReservationInvitationEntry(reservation.id(),
                verification("entry-holder", entryCode(holderMemberId, reservation.id()), "bar_1")).status()).isEqualTo("entered");
        assertThat(service.verifyReservationInvitationEntry(reservation.id(),
                verification("entry-friend-1", entryCode("invm-friend-1", reservation.id()), "bar_1")).status()).isEqualTo("entered");

        // 第三人入场超 capacity → 拒。
        assertThatThrownBy(() -> service.verifyReservationInvitationEntry(reservation.id(),
                verification("entry-friend-2", entryCode("invm-friend-2", reservation.id()), "bar_1")))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("BAR_INVITATION_CAPACITY_EXHAUSTED");

        // 未确认者入场 → 拒。
        assertThatThrownBy(() -> service.verifyReservationInvitationEntry(reservation.id(),
                verification("entry-stranger", entryCode("invm-stranger", reservation.id()), "bar_1")))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("BAR_INVITATION_MEMBER_NOT_CONFIRMED");

        // 跨吧入场 → 拒（持函免通票仅限定卡吧）。
        assertThatThrownBy(() -> service.verifyReservationInvitationEntry(reservation.id(),
                verification("entry-crossbar", entryCode(holderMemberId, reservation.id()), "bar_other")))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("BAR_SCOPE_MISMATCH");
    }

    @Test
    void cardSeatInvitationEntryRejectsTamperedSignature() {
        // B2⑥ 邀请函 HMAC 入场闸强制核验：绕过签发直改库篡改邀请函绑定（如签名被替换）→ 门口扫码必须拒。
        BarReservationRecord reservation = createReservation("inv-sig", "bar_1", 2);
        service.issueReservationInvitation(reservation.id(), new BarInvitationActionRequest("inv-issue-sig", null));
        Instant now = Instant.parse("2026-06-20T21:30:00Z");
        barRepository.saveInvitationMember(new BarReservationInvitationMemberRecord(
                "invm-sig-friend", reservation.id(), "bar_1", "friend-sig", "cust-friend-sig",
                false, "confirmed", now, null, null, now, now));
        // 篡改：保留所有权威字段，仅把防伪签名替换为伪造串（模拟绕过签发改库）。
        BarReservationRecord issued = barRepository.findReservationById(reservation.id()).orElseThrow();
        barRepository.saveReservation(new BarReservationRecord(
                issued.id(), issued.barId(), issued.merchantId(), issued.tableId(), issued.customerId(),
                issued.reservedAt(), issued.guestCount(), issued.allowedPassFreeGuestCount(),
                issued.depositAmountCent(), issued.depositStatus(), issued.orderSource(), issued.salesStaffId(),
                issued.status(), issued.createdTerminal(), issued.deviceId(), issued.evidenceAttachmentIds(),
                issued.createdAt(), issued.updatedAt(), issued.depositRule(), issued.depositIntentId(),
                issued.depositPaidAt(), issued.depositSettlement(), issued.invitationToken(),
                "forged-signature", issued.invitationCapacity(), issued.invitationStatus(),
                issued.invitationIssuedAt()));

        assertThatThrownBy(() -> service.verifyReservationInvitationEntry(reservation.id(),
                verification("entry-tampered", entryCode("invm-sig-friend", reservation.id()), "bar_1")))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("BAR_INVITATION_SIGNATURE_INVALID");
    }

    @Test
    void deliveryRejectsStockManagedProductWhenInventoryIsInsufficientAndKeepsItemUndelivered() {
        BarReservationRecord reservation = createReservation("reservation-stock", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-stock",
                "create product",
                List.of(),
                "bar_1",
                "Champagne",
                "spirit",
                188800L,
                null,
                null,
                "active",
                "inventory_champagne",
                "bar_1",
                true
        , null, null, null, null, null, null, null));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-stock",
                "create order",
                List.of(),
                "bar_1",
                reservation.tableId(),
                reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 2)),
                List.of(),
                "offline"
        ));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();

        assertThatThrownBy(() -> service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-stock",
                "deliver",
                List.of(),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 2)),
                "staff_1",
                List.of()
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("STOCK_INSUFFICIENT");

        assertThat(barRepository.orderItems.values())
                .extracting(BarOrderItemRecord::deliveryStatus)
                .containsExactly("pending");
        assertThat(barRepository.stockMovements).isEmpty();
    }

    @Test
    void deliveryRejectsAlreadyDeliveredOrderItemAndDoesNotDeductStockAgain() {
        BarReservationRecord reservation = createReservation("reservation-stock-repeat", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-stock-repeat",
                "create product",
                List.of(),
                "bar_1",
                "Champagne",
                "spirit",
                188800L,
                null,
                null,
                "active",
                "inventory_champagne_repeat",
                "bar_1",
                true
        , null, null, null, null, null, null, null));
        barRepository.saveStockBalance(new StockBalanceSnapshot(
                "balance-repeat",
                "inventory_champagne_repeat",
                "bar_1",
                5,
                0,
                5,
                Instant.now()
        ));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-stock-repeat",
                "create order",
                List.of(),
                "bar_1",
                reservation.tableId(),
                reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 2)),
                List.of(),
                "offline"
        ));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();

        service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-stock-repeat-1",
                "deliver once",
                List.of(),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 2)),
                "staff_1",
                List.of("att_delivery_1")
        ));

        assertThatThrownBy(() -> service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-stock-repeat-2",
                "deliver twice",
                List.of(),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 2)),
                "staff_1",
                List.of("att_delivery_2")
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_ORDER_ITEM_ALREADY_DELIVERED");

        assertThat(barRepository.findStockBalance("inventory_champagne_repeat", "bar_1"))
                .get()
                .extracting(StockBalanceSnapshot::quantity, StockBalanceSnapshot::availableQuantity)
                .containsExactly(3, 3);
        assertThat(barRepository.stockMovements).hasSize(1);
        assertThat(barRepository.orderItems.get(orderItemId).deliveryStatus()).isEqualTo("delivered");
    }

    @Test
    void deliveryRejectsOrderItemFromAnotherOrderAndKeepsBothOrdersPending() {
        BarReservationRecord firstReservation = createReservation("reservation-scope-1", "bar_1", 2);
        BarReservationRecord secondReservation = createReservation("reservation-scope-2", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-scope",
                "create product",
                List.of(),
                "bar_1",
                "Soda",
                "soft_drink",
                1800L,
                null,
                null,
                "active",
                null,
                null,
                false
        , null, null, null, null, null, null, null));
        BarOrderRecord firstOrder = service.createOrder(new BarOrderRequest(
                "order-scope-1",
                "create first order",
                List.of(),
                "bar_1",
                firstReservation.tableId(),
                firstReservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)),
                List.of(),
                "offline"
        ));
        BarOrderRecord secondOrder = service.createOrder(new BarOrderRequest(
                "order-scope-2",
                "create second order",
                List.of(),
                "bar_1",
                secondReservation.tableId(),
                secondReservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)),
                List.of(),
                "offline"
        ));
        String secondOrderItemId = barRepository.listOrderItems(secondOrder.id()).getFirst().id();

        assertThatThrownBy(() -> service.deliverOrder(firstOrder.id(), new BarOrderDeliveryRequest(
                "deliver-scope-mismatch",
                "deliver wrong item",
                List.of(),
                List.of(Map.of("order_item_id", secondOrderItemId, "quantity", 1)),
                "staff_1",
                List.of("att_delivery_scope")
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_ORDER_ITEM_SCOPE_MISMATCH");

        assertThat(barRepository.findOrderById(firstOrder.id()))
                .get()
                .extracting(BarOrderRecord::status)
                .isEqualTo("created");
        assertThat(barRepository.findOrderById(secondOrder.id()))
                .get()
                .extracting(BarOrderRecord::status)
                .isEqualTo("created");
        assertThat(barRepository.orderItems.values())
                .extracting(BarOrderItemRecord::deliveryStatus)
                .containsOnly("pending");
    }

    @Test
    void deliveryPersistsEvidenceAndPhotoAttachmentsOnTicketEventAndStockMovement() {
        BarReservationRecord reservation = createReservation("reservation-delivery-evidence", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-delivery-evidence",
                "create product",
                List.of(),
                "bar_1",
                "Champagne",
                "spirit",
                188800L,
                null,
                null,
                "active",
                "inventory_champagne_evidence",
                "bar_1",
                true
        , null, null, null, null, null, null, null));
        barRepository.saveStockBalance(new StockBalanceSnapshot(
                "balance-evidence",
                "inventory_champagne_evidence",
                "bar_1",
                3,
                0,
                3,
                Instant.now()
        ));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-delivery-evidence",
                "create order",
                List.of(),
                "bar_1",
                reservation.tableId(),
                reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)),
                List.of(),
                "offline"
        ));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();

        service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-with-photo-evidence",
                "deliver with photo evidence",
                List.of("att_delivery_doc", "att_shared"),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 1)),
                "staff_1",
                List.of("att_delivery_photo", "att_shared")
        ));

        assertThat(barRepository.stockMovements).hasSize(1);
        assertThat(barRepository.stockMovements.getFirst().evidenceAttachmentIds())
                .containsExactly("att_delivery_doc", "att_shared", "att_delivery_photo");
        assertThat(barRepository.productionTicketEvents)
                .extracting(BarProductionTicketEventRecord::evidenceAttachmentIds)
                .containsExactly(List.of("att_delivery_doc", "att_shared", "att_delivery_photo"));
    }

    @Test
    void deliveryWithExcessPourRecordsLossAndEmitsLossExceptionWhenOverThreshold() {
        BarReservationRecord reservation = createReservation("reservation-pour-loss", "bar_1", 2);
        // 标准 30ml/杯，损耗阈值 10ml；按杯销售配置（REQ-22）。第十八批 C 段第 17 条：出酒吧台配有采集设备，按设备实际量计损耗。
        equippedCounterStock("inventory_whisky_loss");
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-pour-loss", "create", List.of(), "bar_1", "Whisky", "spirit", 128800L, null, null,
                "active", "inventory_whisky_loss", "bar_1", true, 30, "ml", 10, null, null, null, null));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-pour-loss", "create order", List.of(), "bar_1", reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 2)), List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();

        // 2 杯标准 60ml，实际 75ml → 损耗 15ml > 阈值 10ml
        service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-pour-loss", "deliver", List.of(),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 2, "actual_pour_volume", 75)),
                "staff_1", List.of()));

        assertThat(barRepository.listOrderItems(order.id()).getFirst().lossVolume()).isEqualTo(15L);
        assertThat(barRepository.listOrderItems(order.id()).getFirst().actualPourVolume()).isEqualTo(75);
        assertThat(auditEventRepository.events.stream()
                .anyMatch(event -> "inventory.loss_exception".equals(event.eventName()))).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // P0#4 按杯 pour cost 强制采集（文档 bar §17.4 / R6）
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void deliveryRejectsByGlassAtCounterWithPourDeviceWhenActualPourVolumeMissing() {
        // 第十八批 C 段第 17 条（BSM 第 1065 行「配有出酒量采集设备的吧台，实际出酒量必须由设备采集」）：缺实际出酒量 → 拒单。
        BarReservationRecord reservation = createReservation("reservation-pour-required", "bar_1", 2);
        equippedCounterStock("inventory_whisky_required");
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-pour-required", "create", List.of(), "bar_1", "Whisky", "spirit", 128800L, null, null,
                "active", "inventory_whisky_required", "bar_1", true, 30, "ml", 10, null, null, null, null));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-pour-required", "create order", List.of(), "bar_1", reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 2)), List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();

        assertThatThrownBy(() -> service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-pour-required", "deliver", List.of(),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 2)),
                "staff_1", List.of())))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("POUR_VOLUME_REQUIRED");

        assertThat(barRepository.orderItems.get(orderItemId).deliveryStatus()).isEqualTo("pending");
    }

    /** 第十八批 C 段第 17 条：该物品在 bar_1 库位有库存，且所挂吧台配有出酒量采集设备。 */
    private void equippedCounterStock(String itemId) {
        barRepository.saveStockBalance(new StockBalanceSnapshot("balance-" + itemId, itemId, "bar_1", 5, 0, 5, Instant.now()));
        barRepository.pourDeviceStock.add(itemId + ":bar_1");
        // 第十八批 I 段第 76 条：按杯按容量扣库，物料须登记规格容量（700 毫升瓶装）。
        barRepository.specVolumes.put(itemId, 700);
    }

    @Test
    void deliveryByGlassAtCounterWithoutPourDeviceUsesStandardVolumeAndStandardLoss() {
        // 第十八批 C 段第 17 条（BSM 第 1065 行「未配备采集设备的吧台，必须按标准出酒量配酒，也按该标准计算扣减与标准损耗，不得按零损耗记账」；
        // 第十八批裁决 4「标准损耗＝标准出酒量×本酒吧损耗率阈值」，未配置按出厂 5%）：吧台未配设备、不传实际出酒量照常出品。
        BarReservationRecord reservation = createReservation("reservation-pour-standard", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-pour-standard", "create", List.of(), "bar_1", "Whisky", "spirit", 128800L, null, null,
                "active", null, null, false, 30, "ml", 10, null, null, null, null));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-pour-standard", "create order", List.of(), "bar_1", reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 2)), List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();

        assertThatCode(() -> service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-pour-standard", "deliver", List.of(),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 2)),
                "staff_1", List.of()))).doesNotThrowAnyException();

        // 2 杯标准 30ml → 实际量按标准 60ml，标准损耗 60×5%＝3ml，属正常记账、不落兜底待核查事件。
        assertThat(barRepository.listOrderItems(order.id()).getFirst().actualPourVolume()).isEqualTo(60);
        assertThat(barRepository.listOrderItems(order.id()).getFirst().lossVolume()).isEqualTo(3L);
        assertThat(auditEventRepository.events.stream()
                .anyMatch(event -> "pour_volume_fallback".equals(event.metadata().get("reason")))).isFalse();
    }

    @Test
    void deliveryNonByGlassProductWithoutPourVolumeIsAllowed() {
        // 非按杯产品（未配置 standard_pour_volume）：缺 actual_pour_volume 不拦截，保持既有行为。
        BarReservationRecord reservation = createReservation("reservation-non-by-glass", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-non-by-glass", "create", List.of(), "bar_1", "Bottle Beer", "beer", 2800L, null, null,
                "active", null, null, false, null, null, null, null, null, null, null));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-non-by-glass", "create order", List.of(), "bar_1", reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)), List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();

        service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-non-by-glass", "deliver", List.of(),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 1)),
                "staff_1", List.of()));

        assertThat(barRepository.orderItems.get(orderItemId).deliveryStatus()).isEqualTo("delivered");
        assertThat(barRepository.listOrderItems(order.id()).getFirst().actualPourVolume()).isNull();
        assertThat(barRepository.listOrderItems(order.id()).getFirst().lossVolume()).isEqualTo(0L);
    }

    @Test
    void deliveryRejectsPartialQuantityAndKeepsOrderInventoryAndTicketPending() {
        BarReservationRecord reservation = createReservation("reservation-partial-delivery", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-partial-delivery",
                "create product",
                List.of(),
                "bar_1",
                "Champagne",
                "spirit",
                188800L,
                null,
                null,
                "active",
                "inventory_champagne_partial",
                "bar_1",
                true
        , null, null, null, null, null, null, null));
        barRepository.saveStockBalance(new StockBalanceSnapshot(
                "balance-partial",
                "inventory_champagne_partial",
                "bar_1",
                5,
                0,
                5,
                Instant.now()
        ));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-partial-delivery",
                "create order",
                List.of(),
                "bar_1",
                reservation.tableId(),
                reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 2)),
                List.of(),
                "offline"
        ));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();

        assertThatThrownBy(() -> service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-partial-quantity",
                "deliver partial quantity",
                List.of(),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 1)),
                "staff_1",
                List.of("att_partial_delivery")
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_ORDER_PARTIAL_DELIVERY_UNSUPPORTED");

        assertThat(barRepository.findOrderById(order.id()))
                .get()
                .extracting(BarOrderRecord::status)
                .isEqualTo("created");
        assertThat(barRepository.orderItems.get(orderItemId).deliveryStatus()).isEqualTo("pending");
        assertThat(barRepository.findStockBalance("inventory_champagne_partial", "bar_1"))
                .get()
                .extracting(StockBalanceSnapshot::quantity, StockBalanceSnapshot::availableQuantity)
                .containsExactly(5, 5);
        assertThat(barRepository.stockMovements).isEmpty();
        assertThat(barRepository.findProductionTicketByOrderId(order.id()))
                .get()
                .extracting(BarProductionTicketRecord::ticketStatus)
                .isEqualTo("created");
    }

    @Test
    void barBillPayIntentReusesFinancePaymentIntentSemantics() {
        BarBillRecord bill = billForOrder("pay-intent-order", "staff_sales");

        PaymentIntentRecord paymentIntent = service.createBillPayIntent(bill.id(), new PaymentIntentRequest(
                "pay-intent",
                "create intent",
                List.of(),
                "bar_bill",
                bill.id(),
                bill.payableAmountCent(),
                "CNY",
                "aggregate_pay"
        ));

        assertThat(paymentIntent)
                .extracting(
                        PaymentIntentRecord::businessOrderType,
                        PaymentIntentRecord::businessOrderId,
                        PaymentIntentRecord::amountCent,
                        PaymentIntentRecord::channel,
                        PaymentIntentRecord::status
                )
                .containsExactly("bar_bill", bill.id(), bill.payableAmountCent(), "aggregate_pay", "created");
    }

    @Test
    void barBillPayIntentRejectsUnknownPaymentChannel() {
        BarBillRecord bill = billForOrder("pay-intent-bad-channel", "staff_sales");

        assertThatThrownBy(() -> service.createBillPayIntent(bill.id(), new PaymentIntentRequest(
                "pay-intent-bad-channel",
                "create intent",
                List.of(),
                "bar_bill",
                bill.id(),
                bill.payableAmountCent(),
                "CNY",
                "unsupported_gateway"
        )))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "VALIDATION_FAILED");
    }

    @Test
    void productionTicketTransitionsThroughAcceptedPreparingAndReadyWithEventTrace() {
        BarReservationRecord reservation = createReservation("reservation-production-flow", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-production-flow",
                "create product",
                List.of(),
                "bar_1",
                "Negroni",
                "cocktail",
                8800L,
                null,
                null,
                "active",
                null,
                null,
                false
        , null, null, null, null, null, null, null));
        service.createOrder(new BarOrderRequest(
                "production-flow",
                "create order",
                List.of(),
                "bar_1",
                reservation.tableId(),
                reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)),
                List.of(),
                "offline"
        ));

        BarProductionTicketRecord accepted = service.transitionProductionTicket(
                "ticket_production_flow",
                new BarProductionTicketTransitionRequest(
                        "production-accept",
                        "accept order",
                        List.of("att_accept"),
                        "accepted",
                        "bar-counter-1",
                        "staff_bar_1",
                        null
                )
        );
        BarProductionTicketRecord preparing = service.transitionProductionTicket(
                "ticket_production_flow",
                new BarProductionTicketTransitionRequest(
                        "production-prepare",
                        "start mixing",
                        List.of(),
                        "preparing",
                        "bar-counter-1",
                        "staff_bar_1",
                        null
                )
        );
        BarProductionTicketRecord ready = service.transitionProductionTicket(
                "ticket_production_flow",
                new BarProductionTicketTransitionRequest(
                        "production-ready",
                        "ready for waiter",
                        List.of("att_ready"),
                        "ready",
                        "bar-counter-1",
                        "staff_bar_1",
                        null
                )
        );

        assertThat(accepted.ticketStatus()).isEqualTo("accepted");
        assertThat(preparing.ticketStatus()).isEqualTo("preparing");
        assertThat(ready.ticketStatus()).isEqualTo("ready");
        // P1-3 fix: operatorUserId is now from auth token ("usr_admin"), not request body ("staff_bar_1")
        assertThat(service.listProductionTicketEvents("ticket_production_flow").items())
                .extracting(
                        BarProductionTicketEventRecord::actionType,
                        BarProductionTicketEventRecord::fromStatus,
                        BarProductionTicketEventRecord::toStatus,
                        BarProductionTicketEventRecord::stationCode,
                        BarProductionTicketEventRecord::operatorUserId
                )
                .containsExactly(
                        tuple("mark_ready", "preparing", "ready", "bar-counter-1", "usr_admin"),
                        tuple("start_prepare", "accepted", "preparing", "bar-counter-1", "usr_admin"),
                        tuple("accept", "created", "accepted", "bar-counter-1", "usr_admin")
                );
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("bar.production_ticket.transitioned");
    }

    @Test
    void productionTicketExceptionTransitionUsesDbAllowedActionType() {
        BarReservationRecord reservation = createReservation("reservation-production-exception", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-production-exception",
                "create product",
                List.of(),
                "bar_1",
                "Old Fashioned",
                "cocktail",
                9800L,
                null,
                null,
                "active",
                null,
                null,
                false
        , null, null, null, null, null, null, null));
        service.createOrder(new BarOrderRequest(
                "production-exception",
                "create order",
                List.of(),
                "bar_1",
                reservation.tableId(),
                reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)),
                List.of(),
                "offline"
        ));

        BarProductionTicketRecord exception = service.transitionProductionTicket(
                "ticket_production_exception",
                new BarProductionTicketTransitionRequest(
                        "production-exception-mark",
                        "spill incident",
                        List.of(),
                        "exception",
                        "bar-counter-1",
                        "staff_bar_1",
                        null
                )
        );

        assertThat(exception.ticketStatus()).isEqualTo("exception");
        // action_type must be one of the values allowed by V029 chk_bar_production_ticket_event_action
        // ('accept','start_prepare','mark_ready','cancel','exception','deliver') — NOT "mark_exception".
        assertThat(service.listProductionTicketEvents("ticket_production_exception").items())
                .extracting(BarProductionTicketEventRecord::actionType, BarProductionTicketEventRecord::toStatus)
                .containsExactly(tuple("exception", "exception"));
    }

    @Test
    void productionTicketRejectsInvalidTransitionAndKeepsStatusUnchanged() {
        BarReservationRecord reservation = createReservation("reservation-production-invalid", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-production-invalid",
                "create product",
                List.of(),
                "bar_1",
                "Mojito",
                "cocktail",
                6800L,
                null,
                null,
                "active",
                null,
                null,
                false
        , null, null, null, null, null, null, null));
        service.createOrder(new BarOrderRequest(
                "production-invalid",
                "create order",
                List.of(),
                "bar_1",
                reservation.tableId(),
                reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)),
                List.of(),
                "offline"
        ));

        assertThatThrownBy(() -> service.transitionProductionTicket(
                "ticket_production_invalid",
                new BarProductionTicketTransitionRequest(
                        "production-ready-from-created",
                        "skip states",
                        List.of(),
                        "ready",
                        "bar-counter-1",
                        "staff_bar_1",
                        null
                )
        ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_PRODUCTION_INVALID_TRANSITION");

        assertThat(barRepository.findProductionTicketById("ticket_production_invalid"))
                .get()
                .extracting(BarProductionTicketRecord::ticketStatus)
                .isEqualTo("created");
        assertThat(barRepository.productionTicketEvents).isEmpty();
    }

    @Test
    void deliveryMarksProductionTicketDeliveredWithoutOverwritingCancelledTicket() {
        BarReservationRecord reservation = createReservation("reservation-production-delivery", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-production-delivery",
                "create product",
                List.of(),
                "bar_1",
                "Champagne",
                "spirit",
                188800L,
                null,
                null,
                "active",
                null,
                null,
                false
        , null, null, null, null, null, null, null));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "production-delivery",
                "create order",
                List.of(),
                "bar_1",
                reservation.tableId(),
                reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)),
                List.of(),
                "offline"
        ));
        String itemId = barRepository.listOrderItems(order.id()).getFirst().id();

        service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "production-deliver",
                "deliver",
                List.of(),
                List.of(Map.of("order_item_id", itemId, "quantity", 1)),
                "staff_1",
                List.of("att_delivery")
        ));

        assertThat(barRepository.findProductionTicketById("ticket_production_delivery"))
                .get()
                .extracting(BarProductionTicketRecord::ticketStatus)
                .isEqualTo("delivered");
        assertThat(barRepository.productionTicketEvents)
                .extracting(BarProductionTicketEventRecord::actionType, BarProductionTicketEventRecord::toStatus)
                .contains(tuple("deliver", "delivered"));
    }

    // ========================================================================
    // 第十批 TK-26-R1：整单与出品票据只在全部有效明细送达后才置已送达（BSM L1025「分别标注已上菜与未上菜」；
    // ENUM L112 出品票据状态流转；BSM L823「整笔……全部实际履约完成的时点」；裁决 W01）
    // ========================================================================

    @Test
    void partialDeliveryKeepsOrderAndTicketUndelivered() {
        BarReservationRecord reservation = createReservation("reservation-partial", "bar_1", 2);
        BarProductRecord beer = product("partial-beer", "bar_1", 3000L);
        BarProductRecord wine = product("partial-wine", "bar_1", 8000L);
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "partial-deliver", "create order", List.of(), "bar_1", reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", beer.id(), "quantity", 1), Map.of("product_id", wine.id(), "quantity", 1)),
                List.of(), "offline"));

        BarOrderRecord afterFirst = service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "partial-deliver-1", "deliver beer", List.of(),
                List.of(Map.of("order_item_id", itemOf(order, beer), "quantity", 1)), "staff_1", List.of()));

        assertThat(barRepository.findOrderById(order.id())).get()
                .extracting(BarOrderRecord::status).isNotEqualTo("delivered");
        assertThat(afterFirst.status()).isNotEqualTo("delivered");
        assertThat(barRepository.findProductionTicketById("ticket_partial_deliver")).get()
                .extracting(BarProductionTicketRecord::ticketStatus).isEqualTo("created");
        assertThat(barRepository.productionTicketEvents)
                .extracting(BarProductionTicketEventRecord::actionType).contains("partial_deliver");
        assertThat(barRepository.listOrderItems(order.id()))
                .extracting(BarOrderItemRecord::deliveryStatus).containsExactlyInAnyOrder("delivered", "pending");
    }

    @Test
    void deliveryCompletesOnlyWhenAllNonVoidedItemsDelivered() {
        BarReservationRecord reservation = createReservation("reservation-allvoid", "bar_1", 2);
        BarProductRecord beer = product("allvoid-beer", "bar_1", 3000L);
        BarProductRecord wine = product("allvoid-wine", "bar_1", 8000L);
        BarProductRecord gin = product("allvoid-gin", "bar_1", 9000L);
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "allvoid-deliver", "create order", List.of(), "bar_1", reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", beer.id(), "quantity", 1), Map.of("product_id", wine.id(), "quantity", 1),
                        Map.of("product_id", gin.id(), "quantity", 1)),
                List.of(), "offline"));
        BarOrderItemRecord ginItem = barRepository.orderItems.get(itemOf(order, gin));
        barRepository.saveOrderItem(new BarOrderItemRecord(ginItem.id(), ginItem.orderId(), ginItem.productId(),
                ginItem.productName(), ginItem.quantity(), ginItem.unitPriceCent(), ginItem.amountCent(), "voided",
                ginItem.createdAt(), Instant.now(), null, 0L, true, "customer_change", Instant.now(), "staff_1",
                false, false, null));

        service.deliverOrder(order.id(), new BarOrderDeliveryRequest("allvoid-1", "deliver beer", List.of(),
                List.of(Map.of("order_item_id", itemOf(order, beer), "quantity", 1)), "staff_1", List.of()));
        assertThat(barRepository.findOrderById(order.id())).get()
                .extracting(BarOrderRecord::status).isNotEqualTo("delivered");

        BarOrderRecord delivered = service.deliverOrder(order.id(), new BarOrderDeliveryRequest("allvoid-2", "deliver wine",
                List.of(), List.of(Map.of("order_item_id", itemOf(order, wine), "quantity", 1)), "staff_1", List.of()));
        assertThat(delivered.status()).isEqualTo("delivered");
        assertThat(barRepository.findProductionTicketById("ticket_allvoid_deliver")).get()
                .extracting(BarProductionTicketRecord::ticketStatus).isEqualTo("delivered");

        assertThatThrownBy(() -> service.deliverOrder(order.id(), new BarOrderDeliveryRequest("allvoid-3", "deliver gin",
                List.of(), List.of(Map.of("order_item_id", ginItem.id(), "quantity", 1)), "staff_1", List.of())))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("BAR_ORDER_ITEM_VOIDED");
    }

    @Test
    void closedSessionStillAllowsDeliveringRemainingItems() {
        // BSM L1058「关台、清台及新客开台不自动完成原接待会话的未送达商品……原责任人仍须按实际送达……处理并留痕」。
        BarReservationRecord reservation = createReservation("reservation-closed-deliver", "bar_1", 2);
        BarTableSessionRecord session = openSession("closed-deliver", reservation.tableId(), "bar_1", reservation.id());
        BarProductRecord beer = product("closed-beer", "bar_1", 3000L);
        BarProductRecord wine = product("closed-wine", "bar_1", 8000L);
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "closed-deliver", "create order", List.of(), "bar_1", reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", beer.id(), "quantity", 1), Map.of("product_id", wine.id(), "quantity", 1)),
                List.of(), "offline"));
        service.deliverOrder(order.id(), new BarOrderDeliveryRequest("closed-1", "deliver beer", List.of(),
                List.of(Map.of("order_item_id", itemOf(order, beer), "quantity", 1)), "staff_1", List.of()));

        Instant closedAt = Instant.now();
        barRepository.saveTableSession(new BarTableSessionRecord(session.id(), session.barId(), session.merchantId(),
                session.businessDate(), session.tableId(), session.reservationId(), session.customerId(),
                session.guestCount(), session.minConsumptionSnapshotCent(), session.allowedPassFreeGuestCountSnapshot(),
                session.salesStaffId(), session.serviceStaffId(), "closed", session.openedAt(), closedAt,
                session.sourceSessionId(), session.sessionVersion() + 1, session.createdAt(), closedAt));
        assertThat(barRepository.findOrderById(order.id())).get()
                .extracting(BarOrderRecord::status).isNotEqualTo("delivered");

        BarOrderRecord delivered = service.deliverOrder(order.id(), new BarOrderDeliveryRequest("closed-2", "deliver wine",
                List.of(), List.of(Map.of("order_item_id", itemOf(order, wine), "quantity", 1)), "staff_1", List.of()));
        assertThat(delivered.status()).isEqualTo("delivered");
        assertThat(barRepository.listOrderItems(order.id()))
                .extracting(BarOrderItemRecord::deliveryStatus).containsOnly("delivered");
    }
    // A2/A3(REQ-21): 现金结算发 payment.cash_received 事件；channel 用统一词表
    @Test
    void settleWithCashChannelEmitsCashReceivedEvent() {
        BarBillRecord bill = billForOrder("settle-cash", "staff_sales");
        service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-cash-1", "cash settle", List.of(), "pay_cash_1", null, "cash", "cashier_1", null));
        assertThat(auditEventRepository.events.stream()
                .anyMatch(event -> "payment.cash_received".equals(event.eventName()))).isTrue();
    }

    @Test
    void zeroRemainingBillSettlesAsCloseWithoutNewSettlementRow() {
        // 复核修正(P2):全额减免后应收=0 的账单必须能关——结账=关单,不产生收款流水。
        BarBillRecord bill = billForOrder("waived-close", "staff_sales");
        service.applyBillAdjustment(bill.id(), new BarBillAdjustmentRequest(
                "bill-waived-close", "full waiver", List.of(), "waiver", 128800L, "全额减免", null));
        BarBillRecord closed = service.settleBill(bill.id(), new BarBillSettleRequest(
                "wc-settle", null, List.of(), "pay_wc", null, "aggregate_pay"));
        assertThat(closed.settlementStatus()).isEqualTo("settled");
        assertThat(closed.paymentStatus()).isEqualTo("paid");
        assertThat(closed.paidAmountCent()).isEqualTo(0L);
    }

    @Test
    void continuationSettlementAccumulatesPaidAmountAndSettlesAtTotal() {
        // AUD-009:续付按剩余校验并累计,不再覆盖首付;累计到总额即 settled。
        BarBillRecord bill = billForOrder("cont-settle", "staff_sales");
        service.settleBill(bill.id(), new BarBillSettleRequest(
                "cont-1", null, List.of(), "pay_c1", 40000L, "aggregate_pay"));
        BarBillRecord second = service.settleBill(bill.id(), new BarBillSettleRequest(
                "cont-2", null, List.of(), "pay_c2", 88800L, "aggregate_pay"));
        assertThat(second.paidAmountCent()).isEqualTo(128800L);
        assertThat(second.paymentStatus()).isEqualTo("paid");
        assertThat(second.settlementStatus()).isEqualTo("settled");
    }

    @Test
    void continuationSettlementRejectsOverRemainingAndZeroAmounts() {
        BarBillRecord bill = billForOrder("cont-guard", "staff_sales");
        service.settleBill(bill.id(), new BarBillSettleRequest(
                "cg-1", null, List.of(), "pay_g1", 40000L, "aggregate_pay"));
        // 二次付全额=超剩余(此前会超收 4 万并把账单标 settled)。
        assertThatThrownBy(() -> service.settleBill(bill.id(), new BarBillSettleRequest(
                "cg-2", null, List.of(), "pay_g2", 128800L, "aggregate_pay")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getMessage()).contains("剩余未付"));
        // 0 元结算拒绝(此前会被接受并写 partial_paid/paid_at)。
        assertThatThrownBy(() -> service.settleBill(bill.id(), new BarBillSettleRequest(
                "cg-3", null, List.of(), "pay_g3", 0L, "aggregate_pay")))
                .isInstanceOf(ApiException.class);
        assertThat(barRepository.findBillById(bill.id())).get()
                .extracting(BarBillRecord::paidAmountCent).isEqualTo(40000L);
    }

    @Test
        void partialSettlementKeepsOriginalPayableAndMarksPartialStatuses() {
        BarBillRecord bill = billForOrder("partial-settle", "staff_sales");

        BarBillRecord settled = service.settleBill(
                bill.id(),
                new BarBillSettleRequest(
                        "partial-settle-once",
                        "customer pays deposit first",
                        List.of("att_partial_settle"),
                        "pay_partial_1",
                        40000L,
                        "aggregate_pay"
                )
        );

        assertThat(settled)
                .extracting(
                        BarBillRecord::payableAmountCent,
                        BarBillRecord::paidAmountCent,
                        BarBillRecord::paymentStatus,
                        BarBillRecord::settlementStatus
                )
                .containsExactly(128800L, 40000L, "partial_paid", "partial_settled");
        // V231 资金/时间铁律：账单转 partial_paid（结账成功）即记支付时刻 paid_at，非 null 且 ≥开单时间 createdAt。
        assertThat(settled.paidAt()).isNotNull();
        assertThat(settled.paidAt()).isAfterOrEqualTo(settled.createdAt());
        assertThat(bill.paidAt()).as("结账前账单 paid_at 为 null").isNull();
        assertThat(barRepository.findBillById(bill.id()))
                .get()
                .extracting(BarBillRecord::payableAmountCent, BarBillRecord::paidAmountCent)
                .containsExactly(128800L, 40000L);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("bar.bill.settled");
    }

    @Test
    void serviceFeeAdjustmentIncreasesPayableAndWritesLedger() {
        BarBillRecord bill = billForOrder("service-fee-adjustment", "staff_sales");

        BarBillAdjustmentRecord adjustment = service.applyBillAdjustment(
                bill.id(),
                new BarBillAdjustmentRequest(
                        "bill-adjust-service-fee",
                        "add service fee",
                        List.of("att_service_fee"),
                        "service_fee",
                        12880L,
                        "10 percent service fee",
                        "supervisor_1"
                )
        );

        assertThat(adjustment)
                .extracting(
                        BarBillAdjustmentRecord::adjustmentType,
                        BarBillAdjustmentRecord::amountCent,
                        BarBillAdjustmentRecord::beforePayableAmountCent,
                        BarBillAdjustmentRecord::afterPayableAmountCent,
                        BarBillAdjustmentRecord::status
                )
                .containsExactly("service_fee", 12880L, 128800L, 141680L, "applied");
        assertThat(barRepository.findBillById(bill.id()))
                .get()
                .extracting(BarBillRecord::payableAmountCent, BarBillRecord::paymentStatus, BarBillRecord::settlementStatus)
                .containsExactly(141680L, "unpaid", "unsettled");
        assertThat(barRepository.billAdjustments).hasSize(1);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("bar.bill.adjusted");
    }

    @Test
    void billAdjustmentIdIsScopedByBillToAvoidCrossBillIdempotencyCollision() {
        BarBillRecord firstBill = billForOrder("adjustment-id-scope-a", "staff_sales");
        BarBillRecord secondBill = billForOrder("adjustment-id-scope-b", "staff_sales");

        BarBillAdjustmentRecord first = service.applyBillAdjustment(
                firstBill.id(),
                new BarBillAdjustmentRequest(
                        "shared-adjustment-key",
                        "add service fee",
                        List.of(),
                        "service_fee",
                        1000L,
                        "same idempotency key on another bill must not collide",
                        "supervisor_1"
                )
        );
        BarBillAdjustmentRecord second = service.applyBillAdjustment(
                secondBill.id(),
                new BarBillAdjustmentRequest(
                        "shared-adjustment-key",
                        "add service fee",
                        List.of(),
                        "service_fee",
                        1000L,
                        "same idempotency key on another bill must not collide",
                        "supervisor_1"
                )
        );

        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(barRepository.billAdjustments)
                .extracting(BarBillAdjustmentRecord::billId)
                .containsExactly(firstBill.id(), secondBill.id());
    }

    @Test
    void minimumSpendTopUpIncreasesPayableWithoutUsingTicketOrEntryRights() {
        BarBillRecord bill = billForOrder("min-spend-top-up", "staff_sales");

        BarBillAdjustmentRecord adjustment = service.applyBillAdjustment(
                bill.id(),
                new BarBillAdjustmentRequest(
                        "bill-adjust-min-spend",
                        "top up minimum spend",
                        List.of(),
                        "min_spend_top_up",
                        20000L,
                        "minimum spend gap cannot be offset by ticket or card-seat entry rights",
                        "supervisor_1"
                )
        );

        assertThat(adjustment)
                .extracting(
                        BarBillAdjustmentRecord::adjustmentType,
                        BarBillAdjustmentRecord::amountCent,
                        BarBillAdjustmentRecord::beforePayableAmountCent,
                        BarBillAdjustmentRecord::afterPayableAmountCent
                )
                .containsExactly("min_spend_top_up", 20000L, 128800L, 148800L);
        assertThat(barRepository.findBillById(bill.id()))
                .get()
                .extracting(BarBillRecord::payableAmountCent)
                .isEqualTo(148800L);
    }

    @Test
    void onAccountMarksBillAsReceivableWithoutChangingPayable() {
        BarBillRecord bill = billForOrder("on-account", "staff_sales");

        BarBillAdjustmentRecord adjustment = service.applyBillAdjustment(
                bill.id(),
                new BarBillAdjustmentRequest(
                        "bill-adjust-on-account",
                        "register on account",
                        List.of("att_on_account"),
                        "on_account",
                        0L,
                        "approved customer credit",
                        "finance_1"
                )
        );

        assertThat(adjustment)
                .extracting(
                        BarBillAdjustmentRecord::adjustmentType,
                        BarBillAdjustmentRecord::amountCent,
                        BarBillAdjustmentRecord::beforePayableAmountCent,
                        BarBillAdjustmentRecord::afterPayableAmountCent,
                        BarBillAdjustmentRecord::arApItemId
                )
                .containsExactly("on_account", 0L, 128800L, 128800L, "arap_for_" + bill.id());
        assertThat(barRepository.findBillById(bill.id()))
                .get()
                .extracting(BarBillRecord::payableAmountCent, BarBillRecord::paymentStatus, BarBillRecord::settlementStatus)
                .containsExactly(128800L, "on_account", "receivable");
        assertThat(receivableLedgerService.requests)
                .extracting(
                        CapturedReceivableRequest::billId,
                        CapturedReceivableRequest::orderId,
                        CapturedReceivableRequest::amountCent,
                        CapturedReceivableRequest::idempotencyKey,
                        CapturedReceivableRequest::operatorNote,
                        CapturedReceivableRequest::evidenceAttachmentIds
                )
                .containsExactly(tuple(
                        bill.id(),
                        bill.orderId(),
                        bill.payableAmountCent(),
                        "bill-adjust-on-account",
                        "approved customer credit",
                        List.of("att_on_account")
                ));
    }

    @Test
    void onAccountRejectsAlreadyReceivableBillToAvoidDuplicateArApItem() {
        BarBillRecord bill = billForOrder("on-account-duplicate", "staff_sales");
        service.applyBillAdjustment(
                bill.id(),
                new BarBillAdjustmentRequest(
                        "bill-adjust-on-account-first",
                        "register on account",
                        List.of(),
                        "on_account",
                        0L,
                        "approved customer credit",
                        "finance_1"
                )
        );

        assertThatThrownBy(() -> service.applyBillAdjustment(
                bill.id(),
                new BarBillAdjustmentRequest(
                        "bill-adjust-on-account-second",
                        "register on account again",
                        List.of(),
                        "on_account",
                        0L,
                        "approved customer credit duplicate",
                        "finance_1"
                )
        ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_BILL_ALREADY_ON_ACCOUNT");

        assertThat(receivableLedgerService.requests).hasSize(1);
        assertThat(barRepository.billAdjustments).hasSize(1);
    }

    @Test
    void billAdjustmentRejectsUnsupportedType() {
        BarBillRecord bill = billForOrder("unsupported-adjustment", "staff_sales");

        assertThatThrownBy(() -> service.applyBillAdjustment(
                bill.id(),
                new BarBillAdjustmentRequest(
                        "bill-adjust-unsupported",
                        "unsupported",
                        List.of(),
                        "free_ticket_offset",
                        1000L,
                        "ticket rights must not offset minimum spend",
                        "supervisor_1"
                )
        ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void waiverReducesPayableBeforeSettlementAndDoesNotRecordRefundTimestamp() {
        BarBillRecord bill = billForOrder("waiver-pre-settle", "staff_sales");

        BarBillAdjustmentRecord adjustment = service.applyBillAdjustment(
                bill.id(),
                new BarBillAdjustmentRequest(
                        "bill-adjust-waiver",
                        "waive part of the bill",
                        List.of("att_waiver"),
                        "waiver",
                        28800L,
                        "goodwill waiver before checkout",
                        "supervisor_1"
                )
        );

        // 免单是结账前减项：after = before - amount；refundedAt 为空（不计入 net 退款冲减）。
        assertThat(adjustment)
                .extracting(
                        BarBillAdjustmentRecord::adjustmentType,
                        BarBillAdjustmentRecord::amountCent,
                        BarBillAdjustmentRecord::beforePayableAmountCent,
                        BarBillAdjustmentRecord::afterPayableAmountCent,
                        BarBillAdjustmentRecord::status,
                        BarBillAdjustmentRecord::refundedAt
                )
                .containsExactly("waiver", 28800L, 128800L, 100000L, "applied", null);
        assertThat(barRepository.findBillById(bill.id()))
                .get()
                .extracting(BarBillRecord::payableAmountCent, BarBillRecord::paymentStatus, BarBillRecord::settlementStatus)
                .containsExactly(100000L, "unpaid", "unsettled");
    }

    @Test
    void waiverRejectsAmountExceedingPayable() {
        BarBillRecord bill = billForOrder("waiver-too-big", "staff_sales");

        assertThatThrownBy(() -> service.applyBillAdjustment(
                bill.id(),
                new BarBillAdjustmentRequest(
                        "bill-adjust-waiver-too-big",
                        "waive more than payable",
                        List.of(),
                        "waiver",
                        128801L,
                        "cannot waive beyond payable",
                        "supervisor_1"
                )
        ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void refundReducesPayableAfterSettlementAndRecordsRefundTimestamp() {
        BarBillRecord bill = billForOrder("refund-post-settle", "staff_sales");
        service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-for-refund", "settle before refund", List.of(),
                "pay_for_refund", null, "aggregate_pay", null, null));

        BarBillAdjustmentRecord adjustment = service.applyBillAdjustment(
                bill.id(),
                new BarBillAdjustmentRequest(
                        "bill-adjust-refund",
                        "refund part of settled bill",
                        List.of("att_refund"),
                        "refund",
                        30000L,
                        "customer complaint refund after checkout",
                        "finance_1"
                )
        );

        // 退款是结账后减项：记录 refundedAt（用于经营日窗口聚合冲减 net）。
        assertThat(adjustment)
                .extracting(
                        BarBillAdjustmentRecord::adjustmentType,
                        BarBillAdjustmentRecord::amountCent,
                        BarBillAdjustmentRecord::beforePayableAmountCent,
                        BarBillAdjustmentRecord::afterPayableAmountCent,
                        BarBillAdjustmentRecord::status
                )
                .containsExactly("refund", 30000L, 128800L, 98800L, "applied");
        assertThat(adjustment.refundedAt()).isNotNull();

        // RC1①：退款过账到 finance ar_ap 台账（direction=payable, source_type=refund），
        // source_id=退款调整记录 id，金额=退款额；调整记录上回填 arApItemId（强一致联动）。
        assertThat(receivableLedgerService.refundPayables).hasSize(1);
        CapturedRefundPayable posted = receivableLedgerService.refundPayables.get(0);
        assertThat(posted.billId()).isEqualTo(bill.id());
        assertThat(posted.refundAmountCent()).isEqualTo(30000L);
        assertThat(posted.adjustmentId()).isEqualTo(adjustment.id());
        assertThat(adjustment.arApItemId()).isEqualTo("arap_refund_" + adjustment.id());
    }

    @Test
    void refundRejectsUnsettledBill() {
        BarBillRecord bill = billForOrder("refund-unsettled", "staff_sales");

        assertThatThrownBy(() -> service.applyBillAdjustment(
                bill.id(),
                new BarBillAdjustmentRequest(
                        "bill-adjust-refund-unsettled",
                        "refund before settle",
                        List.of(),
                        "refund",
                        1000L,
                        "cannot refund an unsettled bill",
                        "finance_1"
                )
        ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_BILL_NOT_REFUNDABLE");
    }

    @Test
    void splitBillCreatesChildBillReducesSourceAndWritesOperation() {
        BarBillRecord bill = billForOrder("split-bill", "staff_sales");

        BarBillOperationRecord operation = service.splitBill(
                bill.id(),
                new BarBillSplitRequest(
                        "split-bill-once",
                        "split bill",
                        List.of("att_split"),
                        40000L,
                        "guest wants separate payment",
                        "cashier_1"
                )
        );

        assertThat(operation)
                .extracting(
                        BarBillOperationRecord::operationType,
                        BarBillOperationRecord::primaryBillId,
                        BarBillOperationRecord::secondaryBillId,
                        BarBillOperationRecord::amountCent,
                        BarBillOperationRecord::status
                )
                .containsExactly("split", bill.id(), operation.generatedBillId(), 40000L, "applied");
        assertThat(barRepository.findBillById(bill.id()))
                .get()
                .extracting(BarBillRecord::subtotalAmountCent, BarBillRecord::payableAmountCent)
                .containsExactly(88800L, 88800L);
        assertThat(barRepository.findBillById(operation.generatedBillId()))
                .get()
                .extracting(BarBillRecord::subtotalAmountCent, BarBillRecord::payableAmountCent, BarBillRecord::paymentStatus)
                .containsExactly(40000L, 40000L, "unpaid");
        assertThat(barRepository.listBillOperations(bill.id()))
                .extracting(BarBillOperationRecord::operationType)
                .containsExactly("split");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("bar.bill.split");
    }

    @Test
    void mergeBillsMovesSecondaryAmountIntoPrimaryAndWritesOperation() {
        BarBillRecord primary = billForOrder("merge-primary-bill", "staff_sales");
        BarBillRecord secondary = billForOrder("merge-secondary-bill", "staff_sales");

        BarBillOperationRecord operation = service.mergeBills(
                primary.id(),
                new BarBillMergeRequest(
                        "merge-bill-once",
                        "merge bill",
                        List.of("att_merge"),
                        secondary.id(),
                        "same party pays together",
                        "cashier_1"
                )
        );

        assertThat(operation)
                .extracting(
                        BarBillOperationRecord::operationType,
                        BarBillOperationRecord::primaryBillId,
                        BarBillOperationRecord::secondaryBillId,
                        BarBillOperationRecord::amountCent,
                        BarBillOperationRecord::status
                )
                .containsExactly("merge", primary.id(), secondary.id(), 128800L, "applied");
        assertThat(barRepository.findBillById(primary.id()))
                .get()
                .extracting(BarBillRecord::subtotalAmountCent, BarBillRecord::payableAmountCent)
                .containsExactly(257600L, 257600L);
        assertThat(barRepository.findBillById(secondary.id()))
                .get()
                .extracting(BarBillRecord::subtotalAmountCent, BarBillRecord::payableAmountCent, BarBillRecord::paymentStatus, BarBillRecord::settlementStatus)
                .containsExactly(0L, 0L, "merged", "merged");
        assertThat(barRepository.listBillOperations(primary.id()))
                .extracting(BarBillOperationRecord::operationType)
                .containsExactly("merge");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("bar.bill.merged");
    }

    @Test
    void splitAndMergeRejectInvalidPaidSettledAndSameBillOperations() {
        BarBillRecord bill = billForOrder("split-merge-reject", "staff_sales");

        assertThatThrownBy(() -> service.splitBill(
                        bill.id(),
                        new BarBillSplitRequest("split-too-much", "split", List.of(), 128800L, "too much", "cashier_1")
                ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_BILL_SPLIT_AMOUNT_INVALID");

        BarBillRecord paid = barRepository.saveBill(new BarBillRecord(
                bill.id(),
                bill.billNo(),
                bill.orderId(),
                bill.barId(),
                bill.merchantId(),
                bill.tableId(),
                bill.reservationId(),
                bill.subtotalAmountCent(),
                bill.discountAmountCent(),
                bill.depositDeductAmountCent(),
                bill.payableAmountCent(),
                1000L,
                bill.currency(),
                "partial_paid",
                "partial_settled",
                bill.salesStaffId(),
                bill.salesRoleType(),
                bill.createdAt(),
                bill.updatedAt()
        ));

        assertThatThrownBy(() -> service.splitBill(
                        paid.id(),
                        new BarBillSplitRequest("split-paid", "split", List.of(), 1000L, "paid", "cashier_1")
                ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_BILL_NOT_SPLITTABLE");

        assertThatThrownBy(() -> service.mergeBills(
                        paid.id(),
                        new BarBillMergeRequest("merge-same", "merge", List.of(), paid.id(), "same", "cashier_1")
                ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_BILL_MERGE_SAME_BILL");

        BarBillRecord primary = billForOrder("merge-cross-bar-primary", "staff_sales");
        BarBillRecord secondary = billForOrder("merge-cross-bar-secondary", "staff_sales");
        BarBillRecord otherBarSecondary = barRepository.saveBill(new BarBillRecord(
                secondary.id(),
                secondary.billNo(),
                secondary.orderId(),
                "bar_2",
                secondary.merchantId(),
                secondary.tableId(),
                secondary.reservationId(),
                secondary.subtotalAmountCent(),
                secondary.discountAmountCent(),
                secondary.depositDeductAmountCent(),
                secondary.payableAmountCent(),
                secondary.paidAmountCent(),
                secondary.currency(),
                secondary.paymentStatus(),
                secondary.settlementStatus(),
                secondary.salesStaffId(),
                secondary.salesRoleType(),
                secondary.createdAt(),
                secondary.updatedAt()
        ));

        assertThatThrownBy(() -> service.mergeBills(
                        primary.id(),
                        new BarBillMergeRequest("merge-cross-bar", "merge", List.of(), otherBarSecondary.id(), "cross bar", "cashier_1")
                ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_BILL_MERGE_BAR_MISMATCH");
    }


    @Test
    void transferTableSessionKeepsSameBarMerchantAndBusinessDateTrace() {
        BarTableRecord sourceTable = createTable("source-transfer", "bar_1", "active", 50000L);
        BarTableRecord targetTable = createTable("target-transfer", "bar_1", "active", 80000L);
        BarTableSessionRecord session = createSession("session-transfer", sourceTable.id(), "bar_1", "merchant_1");

        BarTableSessionAdjustmentRecord adjustment = service.transferTableSession(
                session.id(),
                new BarTableTransferRequest(
                        "transfer-once",
                        "guest wants larger card seat",
                        List.of("att_transfer"),
                        targetTable.id(),
                        "guest wants larger card seat",
                        "supervisor_1",
                        session.sessionVersion(),
                        "target_service_staff"
                )
        );

        assertThat(adjustment)
                .extracting(
                        BarTableSessionAdjustmentRecord::adjustmentType,
                        BarTableSessionAdjustmentRecord::fromTableId,
                        BarTableSessionAdjustmentRecord::toTableId,
                        BarTableSessionAdjustmentRecord::status
                )
                .containsExactly("transfer", sourceTable.id(), targetTable.id(), "applied");
        assertThat(barRepository.findTableSessionById(session.id()))
                .get()
                .extracting(BarTableSessionRecord::tableId, BarTableSessionRecord::sessionVersion, BarTableSessionRecord::status)
                .containsExactly(targetTable.id(), 2L, "open");
        assertThat(barRepository.findTableSessionById(session.id())).get()
                .extracting(BarTableSessionRecord::serviceStaffId).isEqualTo("target_service_staff");
        // 第十批 TK-27-R2（BSM L1079「原台随转台成功立即释放，可供其他客人预订或开台」）：源台直接回空闲。
        assertThat(barRepository.findTableById(sourceTable.id())).get().extracting(BarTableRecord::status).isEqualTo("active");
        assertThat(barRepository.findTableById(targetTable.id())).get().extracting(BarTableRecord::status).isEqualTo("opened");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("bar.table.transferred");
    }

    @Test
    void transferTableSessionRejectsCrossBarTargetAndLeavesSessionUnchanged() {
        BarTableRecord sourceTable = createTable("source-cross-bar", "bar_1", "active", 50000L);
        BarTableRecord targetTable = createTable("target-cross-bar", "bar_2", "active", 80000L);
        BarTableSessionRecord session = createSession("session-cross-bar", sourceTable.id(), "bar_1", "merchant_1");

        assertThatThrownBy(() -> service.transferTableSession(
                session.id(),
                new BarTableTransferRequest(
                        "transfer-cross-bar",
                        "cross bar should fail",
                        List.of(),
                        targetTable.id(),
                        "cross bar should fail",
                        "supervisor_1",
                        session.sessionVersion()
                )
        ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_SCOPE_MISMATCH");

        assertThat(barRepository.findTableSessionById(session.id()))
                .get()
                .extracting(BarTableSessionRecord::tableId, BarTableSessionRecord::sessionVersion)
                .containsExactly(sourceTable.id(), 1L);
        assertThat(barRepository.findTableById(targetTable.id())).get().extracting(BarTableRecord::status).isEqualTo("active");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("bar.table.transfer_failed");
    }

    @Test
    void mergeTableSessionsMarksSecondaryMergedAndKeepsPrimaryTraceable() {
        BarTableRecord primaryTable = createTable("primary-merge", "bar_1", "active", 50000L);
        BarTableRecord secondaryTable = createTable("secondary-merge", "bar_1", "active", 60000L);
        BarTableSessionRecord primary = createSession("primary-merge", primaryTable.id(), "bar_1", "merchant_1");
        BarTableSessionRecord secondary = createSession("secondary-merge", secondaryTable.id(), "bar_1", "merchant_1");

        BarTableSessionAdjustmentRecord adjustment = service.mergeTableSessions(
                primary.id(),
                new BarTableMergeRequest(
                        "merge-once",
                        "two groups joined",
                        List.of("att_merge"),
                        secondary.id(),
                        "two groups joined",
                        "supervisor_1",
                        primary.sessionVersion(),
                        secondary.sessionVersion()
                )
        );

        assertThat(adjustment)
                .extracting(
                        BarTableSessionAdjustmentRecord::adjustmentType,
                        BarTableSessionAdjustmentRecord::sourceSessionId,
                        BarTableSessionAdjustmentRecord::targetSessionId,
                        BarTableSessionAdjustmentRecord::status
                )
                .containsExactly("merge", secondary.id(), primary.id(), "applied");
        assertThat(barRepository.findTableSessionById(primary.id()))
                .get()
                .extracting(BarTableSessionRecord::sessionVersion, BarTableSessionRecord::status)
                .containsExactly(2L, "open");
        assertThat(barRepository.findTableSessionById(secondary.id()))
                .get()
                .extracting(BarTableSessionRecord::status, BarTableSessionRecord::sourceSessionId)
                .containsExactly("merged", primary.id());
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("bar.table.merged");
    }

    @Test
    void renewTableSessionAppendsRenewalAdjustmentAndIncrementsVersion() {
        BarTableRecord table = createTable("renew", "bar_1", "active", 50000L);
        BarTableSessionRecord session = createSession("renew", table.id(), "bar_1", "merchant_1");
        Instant renewedUntil = Instant.now().plus(Duration.ofHours(6));

        BarTableSessionAdjustmentRecord adjustment = service.renewTableSession(
                session.id(),
                new BarTableRenewRequest(
                        "renew-once",
                        "guest extends time",
                        List.of("att_renew"),
                        renewedUntil,
                        20000L,
                        0L,
                        1000L,
                        "guest extends time",
                        "supervisor_1",
                        session.sessionVersion()
                )
        );

        assertThat(adjustment)
                .extracting(
                        BarTableSessionAdjustmentRecord::adjustmentType,
                        BarTableSessionAdjustmentRecord::renewedUntil,
                        BarTableSessionAdjustmentRecord::additionalMinConsumptionCent,
                        BarTableSessionAdjustmentRecord::additionalServiceFeeCent
                )
                .containsExactly("renew", renewedUntil, 20000L, 1000L);
        assertThat(barRepository.findTableSessionById(session.id()))
                .get()
                .extracting(BarTableSessionRecord::sessionVersion, BarTableSessionRecord::status)
                .containsExactly(2L, "open");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("bar.table.renewed");
    }

    @Test
    void tableSessionAdjustmentRejectsStaleVersion() {
        BarTableRecord sourceTable = createTable("source-stale", "bar_1", "active", 50000L);
        BarTableRecord targetTable = createTable("target-stale", "bar_1", "active", 80000L);
        BarTableSessionRecord session = createSession("session-stale", sourceTable.id(), "bar_1", "merchant_1");

        assertThatThrownBy(() -> service.transferTableSession(
                session.id(),
                new BarTableTransferRequest(
                        "transfer-stale-version",
                        "stale version",
                        List.of(),
                        targetTable.id(),
                        "stale version",
                        "supervisor_1",
                        0L
                )
        ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_TABLE_SESSION_VERSION_CONFLICT");
    }

    // ========================================================================
    // 第十批 TK-24：定金不抵扣、台组低消合计与进度、达标各笔原路退回 / 关台没收
    // ========================================================================

    @Test
    void billNeverDeductsReservationDeposit() {
        // BSM L889「定金不抵扣消费、不计入最低消费累计」：已付定金 20000 的订台开单，账单不减定金、应付=消费小计。
        BarReservationRecord reservation = reservationWithFixedDeposit("nodeduct", 20000L, 30);
        service.payReservationDeposit(reservation.id(),
                new BarReservationDepositActionRequest("pay-nodeduct", "pay", List.of(), "aggregate_pay"));
        BarProductRecord product = product("nodeduct", "bar_nodeduct", 30000L);
        BarOrderRecord order = order("nodeduct", "bar_nodeduct", reservation.tableId(), reservation.id(), product, 1);
        BarBillRecord bill = bill("nodeduct", order.id());

        assertThat(bill.depositDeductAmountCent()).isZero();
        assertThat(bill.payableAmountCent()).isEqualTo(30000L);
    }

    @Test
    void mergeTableSessionsSumsMinConsumptionOfBothSides() {
        // BSM L1079「合并后的……最低消费额，分别按两侧所包含全部原台的对应数值相加计算」：A 500 + B 600 → 1100。
        BarTableRecord a = createTable("sum-a", "bar_1", "active", 50000L);
        BarTableRecord b = createTable("sum-b", "bar_1", "active", 60000L);
        BarTableSessionRecord primary = createSession("sum-a", a.id(), "bar_1", "merchant_1");
        BarTableSessionRecord secondary = createSession("sum-b", b.id(), "bar_1", "merchant_1");

        BarTableSessionAdjustmentRecord adjustment = service.mergeTableSessions(primary.id(), new BarTableMergeRequest(
                "merge-sum", "sum", List.of(), secondary.id(), "sum", "supervisor_1",
                primary.sessionVersion(), secondary.sessionVersion()));

        assertThat(adjustment.minConsumptionBeforeCent()).isEqualTo(50000L);
        assertThat(adjustment.minConsumptionAfterCent()).isEqualTo(110000L);
        assertThat(BarMinSpend.thresholdCent(barRepository, primary.id())).isEqualTo(110000L);
    }

    @Test
    void groupTransferDropsOldChainFromThresholdButKeepsConsumption() {
        // BSM L1079「合并台组整体转台到目标台后……原台及原并台关系不再参加之后的……数值计算」「原台已经发生的消费金额随台结转」。
        BarTableRecord a = createTable("gt-a", "bar_1", "active", 50000L);
        BarTableRecord b = createTable("gt-b", "bar_1", "active", 60000L);
        BarTableRecord t = createTable("gt-t", "bar_1", "active", 80000L);
        BarTableSessionRecord primary = createSession("gt-a", a.id(), "bar_1", "merchant_1");
        BarTableSessionRecord secondary = createSession("gt-b", b.id(), "bar_1", "merchant_1");
        BarProductRecord product = product("gt", "bar_1", 30000L);
        order("gt-a", "bar_1", a.id(), null, product, 1);
        order("gt-b", "bar_1", b.id(), null, product, 2);
        service.mergeTableSessions(primary.id(), new BarTableMergeRequest(
                "merge-gt", "join", List.of(), secondary.id(), "join", "supervisor_1",
                primary.sessionVersion(), secondary.sessionVersion()));
        long mergedVersion = barRepository.findTableSessionById(primary.id()).orElseThrow().sessionVersion();

        service.transferTableSession(primary.id(), new BarTableTransferRequest(
                "transfer-gt", "move group", List.of(), t.id(), "move group", "supervisor_1",
                mergedVersion, "target_service_staff"));

        assertThat(BarMinSpend.thresholdCent(barRepository, primary.id())).isEqualTo(80000L);
        assertThat(BarMinSpend.consumedCent(barRepository, primary.id())).isEqualTo(90000L);
        assertThat(barRepository.findTableSessionById(secondary.id())).get()
                .extracting(BarTableSessionRecord::status).isEqualTo("merged_transferred");
    }

    @Test
    void depositRefundsOnThresholdOnceAndKeepsResultAcrossMerge() {
        // BSM L1062「达标时，各笔定金分别原路退回」「已成功退还……的定金保留原结果，不因转台、并台……重复退款」。
        BarReservationRecord reservation = reservationWithFixedDeposit("dep1", 20000L, 30); // 台位低消 100000
        service.payReservationDeposit(reservation.id(),
                new BarReservationDepositActionRequest("pay-dep1", "pay", List.of(), "aggregate_pay"));
        BarTableSessionRecord session = openSession("dep1", reservation.tableId(), "bar_dep1", reservation.id());
        BarProductRecord product = product("dep1", "bar_dep1", 60000L);

        order("dep1-first", "bar_dep1", reservation.tableId(), reservation.id(), product, 1);
        assertThat(barRepository.findReservationById(reservation.id())).get()
                .extracting(BarReservationRecord::depositStatus).isEqualTo("paid");
        assertThat(receivableLedgerService.depositRefunds).isEmpty();

        order("dep1-second", "bar_dep1", reservation.tableId(), reservation.id(), product, 1);
        // 复核 money I2（BSM L1062「退款处理中的记录继续关联原付款，只有实际退款成功才记为已退」）：达标只挂原路退款应付，
        // 定金仍为已支付（退款处理中），实付成功后由财务统一回写点置「达标已原路退回」。
        BarReservationRecord settled = barRepository.findReservationById(reservation.id()).orElseThrow();
        assertThat(settled.depositStatus()).isEqualTo("paid");
        assertThat(settled.depositSettlement()).isNull();
        assertThat(receivableLedgerService.depositRefunds).containsExactly(reservation.id());
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName)
                .contains("bar.reservation_deposit_refunded_on_threshold");

        // 并台 + 再下单：已退定金保留原结果，不重复退。
        BarTableRecord other = createTable("dep1-other", "bar_dep1", "active", 30000L);
        BarTableSessionRecord otherSession = createSession("dep1-other", other.id(), "bar_dep1", null);
        service.mergeTableSessions(session.id(), new BarTableMergeRequest(
                "merge-dep1", "join", List.of(), otherSession.id(), "join", "supervisor_1",
                session.sessionVersion(), otherSession.sessionVersion()));
        order("dep1-third", "bar_dep1", reservation.tableId(), reservation.id(), product, 1);
        assertThat(receivableLedgerService.depositRefunds).containsExactly(reservation.id());
        // 退款处理中的定金关台不没收。
        depositSettlements.forfeitAtClose(session.id());
        assertThat(barRepository.findReservationById(reservation.id())).get()
                .extracting(BarReservationRecord::depositStatus, BarReservationRecord::depositSettlement)
                .containsExactly("paid", null);
    }

    @Test
    void mergedGroupRefundsEachUnsettledDepositWhenCombinedThresholdReached() {
        // BSM L1062「并台按合并台的合计最低消费进度判断；达标时，各笔定金分别原路退回」：A 已退不重复，B 并台达标后新退。
        BarReservationRecord a = reservationWithFixedDeposit("depa", 20000L, 30); // bar_depa，台位低消 100000
        BarTableRecord tableB = createTable("depb", "bar_depa", "active", 60000L);
        BarReservationRecord b = pendingDeposit(service.createReservation(new BarReservationRequest(
                "resv-depb", "create reservation", List.of(), "bar_depa", tableB.id(),
                Instant.parse("2030-06-20T21:00:00Z"), 2, "staff_sales", null, null, Instant.parse("2030-06-20T23:00:00Z"), "13800000000")), 20000L);
        service.payReservationDeposit(a.id(), new BarReservationDepositActionRequest("pay-depa", "pay", List.of(), "cash"));
        service.payReservationDeposit(b.id(), new BarReservationDepositActionRequest("pay-depb", "pay", List.of(), "cash"));
        BarTableSessionRecord sessionA = openSession("depa", a.tableId(), "bar_depa", a.id());
        BarTableSessionRecord sessionB = openSession("depb", tableB.id(), "bar_depa", b.id());
        BarProductRecord product = product("depab", "bar_depa", 50000L);

        order("depa-1", "bar_depa", a.tableId(), a.id(), product, 2);   // A 100000 ≥ 100000 → A 退
        order("depb-1", "bar_depa", tableB.id(), b.id(), product, 1);   // B 50000 < 60000 → 不退
        assertThat(receivableLedgerService.depositRefunds).containsExactly(a.id());

        service.mergeTableSessions(sessionA.id(), new BarTableMergeRequest(
                "merge-depab", "join", List.of(), sessionB.id(), "join", "supervisor_1",
                sessionA.sessionVersion(), sessionB.sessionVersion()));
        // 合计阈值 160000、进度 150000 → 并台本身不触发。
        assertThat(receivableLedgerService.depositRefunds).containsExactly(a.id());

        order("depa-2", "bar_depa", a.tableId(), a.id(), product, 1);   // 进度 200000 ≥ 160000 → 只有 B 新退
        assertThat(receivableLedgerService.depositRefunds).containsExactly(a.id(), b.id());
    }

    @Test
    void closeSessionForfeitsDepositsBelowThreshold() {
        // BSM L1062「客人已到店但未满足该台位最低消费的，定金不退、予以没收」。
        BarReservationRecord reservation = reservationWithFixedDeposit("depc", 20000L, 30);
        service.payReservationDeposit(reservation.id(),
                new BarReservationDepositActionRequest("pay-depc", "pay", List.of(), "aggregate_pay"));
        BarTableSessionRecord session = openSession("depc", reservation.tableId(), "bar_depc", reservation.id());
        order("depc-1", "bar_depc", reservation.tableId(), reservation.id(), product("depc", "bar_depc", 30000L), 1);

        depositSettlements.forfeitAtClose(session.id());

        BarReservationRecord forfeited = barRepository.findReservationById(reservation.id()).orElseThrow();
        assertThat(forfeited.depositStatus()).isEqualTo("settled");
        assertThat(forfeited.depositSettlement()).isEqualTo("forfeited");
        assertThat(forfeited.depositSettledAt()).isNotNull();
        assertThat(receivableLedgerService.depositRefunds).isEmpty();
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName)
                .contains("bar.reservation_deposit_forfeited");
    }

    // ========================================================================
    // 第十批 TK-25：退品按低消拆可退与留款，留款抵用后续点单
    // ========================================================================

    @Test
    void voidWithinMinSpendRetainsInsteadOfRefunding() {
        // BSM L1064「退款后该台位累计消费将低于……最低消费的，不退款——该笔金额留在本台位账上」：低消 1000、已结 1000，退 400。
        BarTableRecord table = createTable("ret1", "bar_1", "active", 100000L);
        createSession("ret1", table.id(), "bar_1", "merchant_1");
        BarProductRecord big = product("ret1-big", "bar_1", 60000L);
        BarProductRecord small = product("ret1-small", "bar_1", 40000L);
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-ret1", "create order", List.of(), "bar_1", table.id(), null,
                List.of(Map.of("product_id", big.id(), "quantity", 1), Map.of("product_id", small.id(), "quantity", 1)),
                List.of(), "offline"));
        BarBillRecord bill = bill("ret1", order.id());
        settle(bill, "ret1");

        service.voidOrderItem(order.id(), new BarOrderItemVoidRequest(
                "void-ret1", "return", List.of(), itemOf(order, small), "customer returned", false, bill.id(), null));

        assertThat(barRepository.listBillAdjustments(bill.id()))
                .extracting(BarBillAdjustmentRecord::adjustmentType, BarBillAdjustmentRecord::amountCent)
                .containsExactly(tuple("min_spend_retained", 40000L));
        assertThat(receivableLedgerService.refundPayables).isEmpty();
        assertThat(barRepository.findBillById(bill.id())).get()
                .extracting(BarBillRecord::payableAmountCent).isEqualTo(100000L);
    }

    @Test
    void voidAboveMinSpendRefundsOnlyExcess() {
        // 低消 1000、已结 1300，退 400：可退 300、留款 100。
        BarTableRecord table = createTable("ret2", "bar_1", "active", 100000L);
        createSession("ret2", table.id(), "bar_1", "merchant_1");
        BarProductRecord big = product("ret2-big", "bar_1", 90000L);
        BarProductRecord small = product("ret2-small", "bar_1", 40000L);
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-ret2", "create order", List.of(), "bar_1", table.id(), null,
                List.of(Map.of("product_id", big.id(), "quantity", 1), Map.of("product_id", small.id(), "quantity", 1)),
                List.of(), "offline"));
        BarBillRecord bill = bill("ret2", order.id());
        settle(bill, "ret2");

        service.voidOrderItem(order.id(), new BarOrderItemVoidRequest(
                "void-ret2", "return", List.of(), itemOf(order, small), "customer returned", false, bill.id(), null));

        assertThat(barRepository.listBillAdjustments(bill.id()))
                .extracting(BarBillAdjustmentRecord::adjustmentType, BarBillAdjustmentRecord::amountCent)
                .containsExactlyInAnyOrder(tuple("refund", 30000L), tuple("min_spend_retained", 10000L));
        assertThat(receivableLedgerService.refundPayables)
                .extracting(CapturedRefundPayable::refundAmountCent).containsExactly(30000L);
        assertThat(barRepository.findBillById(bill.id())).get()
                .extracting(BarBillRecord::payableAmountCent).isEqualTo(100000L);
    }

    @Test
    void voidUnsettledBillWaivesOnlyExcessAndRetainsRest() {
        // 未结 1000、低消 1000，退 400：应付仍 1000（等结账照常收取）、留款 400。
        BarTableRecord table = createTable("ret3", "bar_1", "active", 100000L);
        createSession("ret3", table.id(), "bar_1", "merchant_1");
        BarProductRecord big = product("ret3-big", "bar_1", 60000L);
        BarProductRecord small = product("ret3-small", "bar_1", 40000L);
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-ret3", "create order", List.of(), "bar_1", table.id(), null,
                List.of(Map.of("product_id", big.id(), "quantity", 1), Map.of("product_id", small.id(), "quantity", 1)),
                List.of(), "offline"));
        BarBillRecord bill = bill("ret3", order.id());

        service.voidOrderItem(order.id(), new BarOrderItemVoidRequest(
                "void-ret3", "cancel", List.of(), itemOf(order, small), "customer changed mind", false, bill.id(), null));

        assertThat(barRepository.findBillById(bill.id())).get()
                .extracting(BarBillRecord::payableAmountCent).isEqualTo(100000L);
        assertThat(barRepository.listBillAdjustments(bill.id()))
                .extracting(BarBillAdjustmentRecord::adjustmentType, BarBillAdjustmentRecord::amountCent)
                .containsExactly(tuple("min_spend_retained", 40000L));
    }

    @Test
    void newBillAppliesRetainedBalanceWithoutRecollecting() {
        // BSM L1064「继续点其他产品时，以该笔留款抵付的部分不重复收款」：留款 400 后再点 300 → 应付 0、余额 100；再点 200 → 抵 100、应付 100。
        BarTableRecord table = createTable("ret4", "bar_1", "active", 100000L);
        BarTableSessionRecord session = createSession("ret4", table.id(), "bar_1", "merchant_1");
        BarProductRecord big = product("ret4-big", "bar_1", 60000L);
        BarProductRecord small = product("ret4-small", "bar_1", 40000L);
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-ret4", "create order", List.of(), "bar_1", table.id(), null,
                List.of(Map.of("product_id", big.id(), "quantity", 1), Map.of("product_id", small.id(), "quantity", 1)),
                List.of(), "offline"));
        BarBillRecord bill = bill("ret4", order.id());
        settle(bill, "ret4");
        service.voidOrderItem(order.id(), new BarOrderItemVoidRequest(
                "void-ret4", "return", List.of(), itemOf(order, small), "customer returned", false, bill.id(), null));
        assertThat(BarMinSpend.retainedBalanceCent(barRepository, session.id())).isEqualTo(40000L);

        BarOrderRecord second = order("ret4-second", "bar_1", table.id(), null, product("ret4-30", "bar_1", 30000L), 1);
        BarBillRecord secondBill = bill("ret4-second", second.id());
        assertThat(secondBill.payableAmountCent()).isZero();
        assertThat(barRepository.listBillAdjustments(secondBill.id()))
                .extracting(BarBillAdjustmentRecord::adjustmentType, BarBillAdjustmentRecord::amountCent)
                .containsExactly(tuple("min_spend_retained_applied", 30000L));
        assertThat(BarMinSpend.retainedBalanceCent(barRepository, session.id())).isEqualTo(10000L);

        BarOrderRecord third = order("ret4-third", "bar_1", table.id(), null, product("ret4-20", "bar_1", 20000L), 1);
        BarBillRecord thirdBill = bill("ret4-third", third.id());
        assertThat(thirdBill.payableAmountCent()).isEqualTo(10000L);
        assertThat(barRepository.listBillAdjustments(thirdBill.id()))
                .extracting(BarBillAdjustmentRecord::amountCent).containsExactly(10000L);
        assertThat(BarMinSpend.retainedBalanceCent(barRepository, session.id())).isZero();
    }

    @Test
    void commissionBaseExcludesRetainedApplied() {
        // Q28「不重复收款、收入或奖励」：抵用账单提成基数 = subtotal − 已抵用留款。
        BarTableRecord table = createTable("ret5", "bar_1", "active", 100000L);
        createSession("ret5", table.id(), "bar_1", "merchant_1");
        BarProductRecord big = product("ret5-big", "bar_1", 60000L);
        BarProductRecord small = product("ret5-small", "bar_1", 40000L);
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-ret5", "create order", List.of(), "bar_1", table.id(), null,
                List.of(Map.of("product_id", big.id(), "quantity", 1), Map.of("product_id", small.id(), "quantity", 1)),
                List.of(), "offline"));
        BarBillRecord bill = bill("ret5", order.id());
        settle(bill, "ret5");
        service.voidOrderItem(order.id(), new BarOrderItemVoidRequest(
                "void-ret5", "return", List.of(), itemOf(order, small), "customer returned", false, bill.id(), null));

        BarOrderRecord second = order("ret5-second", "bar_1", table.id(), null, product("ret5-50", "bar_1", 50000L), 1);
        BarBillRecord secondBill = bill("ret5-second", second.id());
        assertThat(secondBill.payableAmountCent()).isEqualTo(10000L);

        BarCommissionCalculationRecord commission = service.calculateCommission(secondBill.id(),
                new CommissionCalculateRequest("commission-ret5", secondBill.id(), true, false));
        assertThat(commission.commissionBaseCent()).isEqualTo(10000L);
    }

    @Test
    void voidRejectionReversesRetainedRecordAndRestoresProgress() {
        // 退品驳回：留款作废（status=reversed，不再计入进度/余额）；全额留款无应付可加回，应付不变；明细恢复后进度回到退品前。
        BarTableRecord table = createTable("ret6", "bar_1", "active", 100000L);
        BarTableSessionRecord session = createSession("ret6", table.id(), "bar_1", "merchant_1");
        BarProductRecord big = product("ret6-big", "bar_1", 60000L);
        BarProductRecord small = product("ret6-small", "bar_1", 40000L);
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-ret6", "create order", List.of(), "bar_1", table.id(), null,
                List.of(Map.of("product_id", big.id(), "quantity", 1), Map.of("product_id", small.id(), "quantity", 1)),
                List.of(), "offline"));
        BarBillRecord bill = bill("ret6", order.id());
        settle(bill, "ret6");
        BarOrderItemVoidRecord voided = service.voidOrderItem(order.id(), new BarOrderItemVoidRequest(
                "void-ret6", "return", List.of(), itemOf(order, small), "customer returned", false, bill.id(), null));
        assertThat(BarMinSpend.retainedBalanceCent(barRepository, session.id())).isEqualTo(40000L);
        assertThat(BarMinSpend.consumedCent(barRepository, session.id())).isEqualTo(100000L);

        decideAsReviewer(voided.id(),
                new BarOrderItemVoidDecisionRequest("void-ret6-decide", "rejected", "not justified"));

        assertThat(barRepository.listBillAdjustments(bill.id()))
                .extracting(BarBillAdjustmentRecord::adjustmentType, BarBillAdjustmentRecord::status)
                .containsExactly(tuple("min_spend_retained", "reversed"));
        assertThat(BarMinSpend.retainedBalanceCent(barRepository, session.id())).isZero();
        assertThat(BarMinSpend.consumedCent(barRepository, session.id())).isEqualTo(100000L);
        assertThat(barRepository.findBillById(bill.id())).get()
                .extracting(BarBillRecord::payableAmountCent).isEqualTo(100000L);
    }

    private BarProductRecord product(String key, String barId, long priceCent) {
        return service.createProduct(new BarProductRequest(
                "product-" + key, "create product", List.of(), barId, "Drink " + key, "beer",
                priceCent, null, null, "active", null, null, false, null, null, null, null, null, null, null));
    }

    private BarOrderRecord order(String key, String barId, String tableId, String reservationId,
            BarProductRecord product, int quantity) {
        return withSales(service.createOrder(new BarOrderRequest(
                "order-" + key, "create order", List.of(), barId, tableId, reservationId,
                List.of(Map.of("product_id", product.id(), "quantity", quantity)), List.of(), "offline")), "staff_sales");
    }

    private BarBillRecord bill(String key, String orderId) {
        return service.createOrderBill(orderId, new BarOrderBillCreateRequest(
                "bill-" + key, "create bill", List.of(), "offline", false, 0));
    }

    private void settle(BarBillRecord bill, String key) {
        service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-" + key, "settle", List.of(), "pay-" + key, null, "aggregate_pay", null, null));
    }

    private String itemOf(BarOrderRecord order, BarProductRecord product) {
        return barRepository.listOrderItems(order.id()).stream()
                .filter(item -> item.productId().equals(product.id())).findFirst().orElseThrow().id();
    }

    private BarTableSessionRecord openSession(String key, String tableId, String barId, String reservationId) {
        return service.createTableSession(new BarTableSessionRequest(
                "session-" + key, "open session", List.of(), barId, null,
                tableId, reservationId, null, 2, "staff_sales", "staff_service", "supervisor_1"));
    }

    // ========================================================================
    // 第十批 TK-27-R2：清台登记恢复空闲、并台期间原台位保持已开台、转台源台立即释放
    // （BSM L1058「关台成功后台位自动进入待清台；由负责该台位的服务员完成清理后在系统登记恢复空闲」
    //  「并台期间全部原台位保持已开台并归属该合并台组」；BSM L1079「原台随转台成功立即释放」；DEC-R8-97-16）
    // ========================================================================

    @Test
    void clearTableRestoresIdleOnlyFromPendingClear() {
        BarTableRecord table = createTable("clear-idle", "bar_1", "active", 50000L);
        barRepository.saveTable(new BarTableRecord(table.id(), table.barId(), table.merchantId(), table.tableCode(),
                table.tableName(), table.tableType(), table.capacity(), table.allowedPassFreeGuestCount(),
                table.minConsumptionCent(), table.currency(), "pending_clear", table.createdAt(), Instant.now()));

        service.clearTable(table.id(), new BarTableClearRequest("clear-idle-1", "清理完成", List.of()));

        assertThat(barRepository.findTableById(table.id())).get()
                .extracting(BarTableRecord::status).isEqualTo("active");
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).contains("bar.table.cleared");

        BarTableRecord opened = createTable("clear-opened", "bar_1", "active", 50000L);
        createSession("clear-opened", opened.id(), "bar_1", "merchant_1");
        assertThatThrownBy(() -> service.clearTable(opened.id(), new BarTableClearRequest("clear-opened-1", "清理完成", List.of())))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("BAR_TABLE_NOT_PENDING_CLEAR");
        assertThat(barRepository.findTableById(opened.id())).get()
                .extracting(BarTableRecord::status).isEqualTo("opened");
    }

    @Test
    void mergeKeepsMergedTableOpenedUntilClose() {
        BarTableRecord primaryTable = createTable("keep-primary", "bar_1", "active", 50000L);
        BarTableRecord secondaryTable = createTable("keep-secondary", "bar_1", "active", 60000L);
        BarTableSessionRecord primary = createSession("keep-primary", primaryTable.id(), "bar_1", "merchant_1");
        BarTableSessionRecord secondary = createSession("keep-secondary", secondaryTable.id(), "bar_1", "merchant_1");

        service.mergeTableSessions(primary.id(), new BarTableMergeRequest("merge-keep", "join", List.of(),
                secondary.id(), "join", "supervisor_1", primary.sessionVersion(), secondary.sessionVersion()));

        assertThat(barRepository.findTableById(secondaryTable.id())).get()
                .extracting(BarTableRecord::status).isEqualTo("opened");
        assertThat(barRepository.findTableById(primaryTable.id())).get()
                .extracting(BarTableRecord::status).isEqualTo("opened");
    }

    // 复核 money B1（BSM L1079「原台随转台成功立即释放……单台转台与合并台组转台同样适用」）：合并台组整体转台后，组内每张原台都释放。
    @Test
    void groupTransferReleasesEveryOriginalTable() {
        BarTableRecord a = createTable("gtr-a", "bar_1", "active", 50000L);
        BarTableRecord b = createTable("gtr-b", "bar_1", "active", 60000L);
        BarTableRecord t = createTable("gtr-t", "bar_1", "active", 80000L);
        BarTableSessionRecord primary = createSession("gtr-a", a.id(), "bar_1", "merchant_1");
        BarTableSessionRecord secondary = createSession("gtr-b", b.id(), "bar_1", "merchant_1");
        merge("gtr", primary, secondary);
        service.transferTableSession(primary.id(), new BarTableTransferRequest("transfer-gtr", "move group", List.of(),
                t.id(), "move group", "supervisor_1", barRepository.findTableSessionById(primary.id()).orElseThrow().sessionVersion(),
                "target_service_staff"));
        assertThat(barRepository.findTableById(a.id())).get().extracting(BarTableRecord::status).isEqualTo("active");
        assertThat(barRepository.findTableById(b.id())).get().extracting(BarTableRecord::status).isEqualTo("active");
        assertThat(barRepository.findTableById(t.id())).get().extracting(BarTableRecord::status).isEqualTo("opened");
    }

    // 复核 money B2（BSM L1064「退款后该台位累计消费将低于……最低消费的，不退款——该笔金额留在本台位账上」）：账单级商品退款减低消进度，
    // 连续两次退款时第二次按扣减后的进度判留款，留款不重复计入进度。
    @Test
    void billLevelRefundsReduceProgressSoSecondRefundIsRetained() {
        BarTableRecord table = createTable("brf", "bar_1", "active", 100000L);
        BarTableSessionRecord session = createSession("brf", table.id(), "bar_1", "merchant_1");
        BarOrderRecord order = order("brf", "bar_1", table.id(), null, product("brf", "bar_1", 130000L), 1);
        BarBillRecord bill = bill("brf", order.id());
        settle(bill, "brf");
        service.applyBillAdjustment(bill.id(), new BarBillAdjustmentRequest(
                "brf-r1", "refund", List.of("att_brf"), "refund", 30000L, "客人退款", "finance_1"));
        assertThat(BarMinSpend.consumedCent(barRepository, session.id())).isEqualTo(100000L);

        BarBillAdjustmentRecord second = service.applyBillAdjustment(bill.id(), new BarBillAdjustmentRequest(
                "brf-r2", "refund", List.of("att_brf"), "refund", 20000L, "客人再退", "finance_1"));
        assertThat(second.adjustmentType()).isEqualTo("min_spend_retained");
        assertThat(second.amountCent()).isEqualTo(20000L);
        assertThat(receivableLedgerService.refundPayables).hasSize(1);
        assertThat(BarMinSpend.consumedCent(barRepository, session.id())).isEqualTo(100000L);
        assertThat(BarMinSpend.retainedBalanceCent(barRepository, session.id())).isEqualTo(20000L);
    }

    @Test
    void transferReleasesSourceTableToIdle() {
        BarTableRecord sourceTable = createTable("release-source", "bar_1", "active", 50000L);
        BarTableRecord targetTable = createTable("release-target", "bar_1", "active", 80000L);
        BarTableSessionRecord session = createSession("release-source", sourceTable.id(), "bar_1", "merchant_1");

        service.transferTableSession(session.id(), new BarTableTransferRequest("transfer-release", "move", List.of(),
                targetTable.id(), "move", "supervisor_1", session.sessionVersion(), "target_service_staff"));

        assertThat(barRepository.findTableById(sourceTable.id())).get()
                .extracting(BarTableRecord::status).isEqualTo("active");
        assertThat(barRepository.findTableById(targetTable.id())).get()
                .extracting(BarTableRecord::status).isEqualTo("opened");
    }
    // ========================================================================
    // 第十批 TK-27-R3：拆台（BSM L1079 拆台整段：拆回两侧 / 任选一台 / 一次全拆、订单归属校验、结果台保持已开台；
    // BSM L1062 定金随结果台组重判；「后续拆台只处理该次转台之后新形成的并台关系」；DEC-R6-19、DEC-R8-95）
    // ========================================================================

    private BarTableSessionAdjustmentRecord merge(String key, BarTableSessionRecord primary, BarTableSessionRecord merged) {
        return service.mergeTableSessions(primary.id(), new BarTableMergeRequest("merge-" + key, "join", List.of(),
                merged.id(), "join", "supervisor_1",
                barRepository.findTableSessionById(primary.id()).orElseThrow().sessionVersion(),
                barRepository.findTableSessionById(merged.id()).orElseThrow().sessionVersion()));
    }

    private BarTableSplitRequest split(String key, BarTableSessionRecord primary, String mode, String splitSessionId,
            List<Map<String, Object>> orderAssignments) {
        return new BarTableSplitRequest("split-" + key, "split", List.of(), mode, splitSessionId, orderAssignments,
                "客人分开结账", "supervisor_1",
                barRepository.findTableSessionById(primary.id()).orElseThrow().sessionVersion());
    }

    @Test
    void splitLastMergeRestoresMergedSessionAndKeepsHistory() {
        BarTableRecord tableA = createTable("split-a", "bar_1", "active", 50000L);
        BarTableRecord tableB = createTable("split-b", "bar_1", "active", 60000L);
        BarTableSessionRecord a = createSession("split-a", tableA.id(), "bar_1", "merchant_1");
        BarTableSessionRecord b = createSession("split-b", tableB.id(), "bar_1", "merchant_1");
        merge("split-ab", a, b);

        BarTableSessionAdjustmentRecord adjustment = service.splitTableSessions(a.id(), split("ab", a, "last_merge", null, List.of()));

        assertThat(barRepository.findTableSessionById(b.id())).get()
                .extracting(BarTableSessionRecord::status, BarTableSessionRecord::sourceSessionId)
                .containsExactly("open", null);
        assertThat(barRepository.findTableSessionById(a.id())).get()
                .extracting(BarTableSessionRecord::status, BarTableSessionRecord::sessionVersion)
                .containsExactly("open", 3L);
        assertThat(barRepository.findTableById(tableB.id())).get().extracting(BarTableRecord::status).isEqualTo("opened");
        assertThat(barRepository.familySessionIds(a.id(), true)).containsExactly(a.id());
        assertThat(adjustment)
                .extracting(BarTableSessionAdjustmentRecord::adjustmentType, BarTableSessionAdjustmentRecord::sourceSessionId,
                        BarTableSessionAdjustmentRecord::minConsumptionBeforeCent, BarTableSessionAdjustmentRecord::minConsumptionAfterCent)
                .containsExactly("split", b.id(), 110000L, 50000L);
        assertThat(barRepository.listTableSessionAdjustments(a.id()))
                .extracting(BarTableSessionAdjustmentRecord::adjustmentType).containsExactly("merge", "split");
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).contains("bar.table.split");
    }

    @Test
    void splitSingleFromThreeCompressesTree() {
        BarTableRecord tableQ = createTable("split-q", "bar_1", "active", 50000L);
        BarTableRecord tableP = createTable("split-p", "bar_1", "active", 50000L);
        BarTableRecord tableM = createTable("split-m", "bar_1", "active", 50000L);
        BarTableSessionRecord q = createSession("split-q", tableQ.id(), "bar_1", "merchant_1");
        BarTableSessionRecord p = createSession("split-p", tableP.id(), "bar_1", "merchant_1");
        BarTableSessionRecord m = createSession("split-m", tableM.id(), "bar_1", "merchant_1");
        merge("split-pm", p, m);   // M1 → P
        merge("split-qp", q, p);   // P（含 M1）→ Q

        service.splitTableSessions(q.id(), split("qp", q, "single", p.id(), List.of()));

        assertThat(barRepository.findTableSessionById(m.id())).get()
                .extracting(BarTableSessionRecord::status, BarTableSessionRecord::sourceSessionId)
                .containsExactly("merged", q.id());
        assertThat(barRepository.findTableSessionById(p.id())).get()
                .extracting(BarTableSessionRecord::status, BarTableSessionRecord::sourceSessionId)
                .containsExactly("open", null);
        assertThat(barRepository.familySessionIds(q.id(), false)).containsExactlyInAnyOrder(q.id(), m.id());
    }

    @Test
    void splitAllRestoresEveryOriginalTable() {
        BarTableRecord tableQ = createTable("split-all-q", "bar_1", "active", 50000L);
        BarTableRecord tableP = createTable("split-all-p", "bar_1", "active", 50000L);
        BarTableRecord tableM = createTable("split-all-m", "bar_1", "active", 50000L);
        BarTableSessionRecord q = createSession("split-all-q", tableQ.id(), "bar_1", "merchant_1");
        BarTableSessionRecord p = createSession("split-all-p", tableP.id(), "bar_1", "merchant_1");
        BarTableSessionRecord m = createSession("split-all-m", tableM.id(), "bar_1", "merchant_1");
        merge("split-all-pm", p, m);
        merge("split-all-qp", q, p);

        service.splitTableSessions(q.id(), split("all", q, "all", null, List.of()));

        for (BarTableSessionRecord session : List.of(q, p, m)) {
            assertThat(barRepository.findTableSessionById(session.id())).get()
                    .extracting(BarTableSessionRecord::status, BarTableSessionRecord::sourceSessionId)
                    .containsExactly("open", null);
        }
        assertThat(barRepository.familySessionIds(q.id(), false)).containsExactly(q.id());
    }

    @Test
    void splitRejectsWhenOrdersLackAttribution() {
        BarTableRecord tableA = createTable("split-attr-a", "bar_1", "active", 50000L);
        BarTableRecord tableB = createTable("split-attr-b", "bar_1", "active", 60000L);
        BarTableSessionRecord a = createSession("split-attr-a", tableA.id(), "bar_1", "merchant_1");
        BarTableSessionRecord b = createSession("split-attr-b", tableB.id(), "bar_1", "merchant_1");
        merge("split-attr", a, b);
        BarReservationRecord reservationB = service.createReservation(new BarReservationRequest(
                "resv-split-attr-b", "create reservation", List.of(), "bar_1", tableB.id(),
                Instant.parse("2030-06-20T21:00:00Z"), 2, "staff_sales", null, null, Instant.parse("2030-06-20T23:00:00Z"), "13800000000"));
        // 复核 money B3 后并台期间在被并台位落单会归属其并台会话；这里模拟一笔无法确认归属的订单（无 bar_order_service_sources）。
        BarOrderRecord orphan = order("split-attr", "bar_1", tableB.id(), reservationB.id(), product("split-attr", "bar_1", 30000L), 1);
        barRepository.orderSessions.remove(orphan.id());
        assertThat(barRepository.findSessionIdByOrderId(orphan.id())).isEmpty();

        assertThatThrownBy(() -> service.splitTableSessions(a.id(), split("attr-1", a, "last_merge", null, List.of())))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("BAR_SPLIT_ORDER_ATTRIBUTION_REQUIRED");
                    assertThat(e.getMessage()).contains(orphan.id());
                });
        assertThat(barRepository.findTableSessionById(b.id())).get()
                .extracting(BarTableSessionRecord::status).isEqualTo("merged");

        service.splitTableSessions(a.id(), split("attr-2", a, "last_merge", null,
                List.of(Map.of("order_id", orphan.id(), "session_id", b.id()))));

        assertThat(barRepository.findSessionIdByOrderId(orphan.id())).contains(b.id());
        assertThat(barRepository.findTableSessionById(b.id())).get()
                .extracting(BarTableSessionRecord::status).isEqualTo("open");
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).contains("bar.table_session.order_attributed");
    }

    @Test
    void splitRejectsHistoricalTransferredChain() {
        BarTableRecord tableA = createTable("split-hist-a", "bar_1", "active", 50000L);
        BarTableRecord tableB = createTable("split-hist-b", "bar_1", "active", 60000L);
        BarTableRecord tableT = createTable("split-hist-t", "bar_1", "active", 80000L);
        BarTableSessionRecord a = createSession("split-hist-a", tableA.id(), "bar_1", "merchant_1");
        BarTableSessionRecord b = createSession("split-hist-b", tableB.id(), "bar_1", "merchant_1");
        merge("split-hist", a, b);
        service.transferTableSession(a.id(), new BarTableTransferRequest("transfer-split-hist", "move", List.of(),
                tableT.id(), "move", "supervisor_1",
                barRepository.findTableSessionById(a.id()).orElseThrow().sessionVersion(), "target_service_staff"));
        assertThat(barRepository.findTableSessionById(b.id())).get()
                .extracting(BarTableSessionRecord::status).isEqualTo("merged_transferred");

        assertThatThrownBy(() -> service.splitTableSessions(a.id(), split("hist-1", a, "last_merge", null, List.of())))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("BAR_SPLIT_HISTORICAL_CHAIN");
        assertThatThrownBy(() -> service.splitTableSessions(a.id(), split("hist-2", a, "single", b.id(), List.of())))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("BAR_SPLIT_HISTORICAL_CHAIN");
        assertThat(barRepository.findTableSessionById(b.id())).get()
                .extracting(BarTableSessionRecord::status).isEqualTo("merged_transferred");
    }

    @Test
    void splitReevaluatesDepositsPerResultGroup() {
        // BSM L1062「拆台时，各笔尚未处置定金按照其在当前有效并台链中的台位归属关联到对应拆分结果，再按该结果台位或台组的
        // 最低消费进度判断」：合并达标前拆台，达标的一侧定金退、未达标一侧仍 paid。
        BarReservationRecord a = reservationWithFixedDeposit("spld", 20000L, 30); // bar_spld，台位低消 100000
        BarTableRecord tableB = createTable("spld-b", "bar_spld", "active", 60000L);
        BarReservationRecord b = pendingDeposit(service.createReservation(new BarReservationRequest(
                "resv-spld-b", "create reservation", List.of(), "bar_spld", tableB.id(),
                Instant.parse("2030-06-20T21:00:00Z"), 2, "staff_sales", null, null, Instant.parse("2030-06-20T23:00:00Z"), "13800000000")), 20000L);
        service.payReservationDeposit(a.id(), new BarReservationDepositActionRequest("pay-spld-a", "pay", List.of(), "cash"));
        service.payReservationDeposit(b.id(), new BarReservationDepositActionRequest("pay-spld-b", "pay", List.of(), "cash"));
        BarTableSessionRecord sessionA = openSession("spld-a", a.tableId(), "bar_spld", a.id());
        BarTableSessionRecord sessionB = openSession("spld-b", tableB.id(), "bar_spld", b.id());
        merge("spld", sessionA, sessionB);
        order("spld-1", "bar_spld", a.tableId(), a.id(), product("spld", "bar_spld", 60000L), 2); // 120000 < 合计 160000
        assertThat(receivableLedgerService.depositRefunds).isEmpty();

        service.splitTableSessions(sessionA.id(), split("spld", sessionA, "last_merge", null, List.of()));

        assertThat(receivableLedgerService.depositRefunds).containsExactly(a.id());
        assertThat(barRepository.findReservationById(b.id())).get()
                .extracting(BarReservationRecord::depositStatus, BarReservationRecord::depositSettlement)
                .containsExactly("paid", null);
    }
    private BarTableRecord createTable(String key, String barId, String status, long minConsumptionCent) {
        return service.createTable(new BarTableRequest(
                "table-" + key,
                "create table",
                List.of(),
                barId,
                "VIP " + key,
                "card_seat",
                minConsumptionCent,
                2,
                status
        ));
    }

    private BarTableSessionRecord createSession(String key, String tableId, String barId, String merchantId) {
        return service.createTableSession(new BarTableSessionRequest(
                "session-" + key,
                "open session",
                List.of("att_session"),
                barId,
                merchantId,
                tableId,
                null,
                "customer_" + key,
                2,
                "staff_sales",
                "staff_service",
                "supervisor_1"
        ));
    }

    // ========================================================================
    // V091 成熟 POS 深度：退菜退酒 / 反结 / 沽清 / 小费
    // ========================================================================

    @Test
    void voidPendingOrderItemCancelsLineWithoutStockMovement() {
        BarReservationRecord reservation = createReservation("void-pending", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-void-pending", "create product", List.of(), "bar_1", "Beer", "beer",
                3000L, null, null, "active", null, null, false, null, null, null, null, null, null, null));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-void-pending", "create order", List.of(), "bar_1",
                reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)), List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();

        BarOrderItemVoidRecord voided = service.voidOrderItem(order.id(), new BarOrderItemVoidRequest(
                "void-pending-1", "wrong item", List.of(), orderItemId, "customer changed mind", false, null, null));

        assertThat(voided.voidType()).isEqualTo("cancel");
        assertThat(voided.wasDelivered()).isFalse();
        assertThat(voided.returnToStock()).isFalse();
        assertThat(barRepository.orderItems.get(orderItemId).voided()).isTrue();
        assertThat(barRepository.orderItems.get(orderItemId).deliveryStatus()).isEqualTo("voided");
        assertThat(barRepository.stockMovements).isEmpty();
    }

    @Test
    void voidDeliveredOrderItemReturnsStockAndDeductsBill() {
        BarReservationRecord reservation = createReservation("void-return", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-void-return", "create product", List.of(), "bar_1", "Champagne", "spirit",
                100000L, null, null, "active", "inventory_void_return", "bar_1", true, null, null, null, null, null, null, null));
        barRepository.saveStockBalance(new StockBalanceSnapshot(
                "balance-void-return", "inventory_void_return", "bar_1", 5, 0, 5, Instant.now()));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-void-return", "create order", List.of(), "bar_1",
                reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 2)), List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();
        service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-void-return", "deliver", List.of(),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 2)), "staff_1", List.of("att1")));
        // stock decreased to 3 after delivery
        assertThat(barRepository.findStockBalance("inventory_void_return", "bar_1"))
                .get().extracting(StockBalanceSnapshot::quantity).isEqualTo(3);
        BarBillRecord bill = service.createOrderBill(order.id(), new BarOrderBillCreateRequest(
                "bill-void-return", "create bill", List.of(), "offline", false, 0));
        long payableBefore = bill.payableAmountCent();

        BarOrderItemVoidRecord voided = service.voidOrderItem(order.id(), new BarOrderItemVoidRequest(
                "void-return-1", "quality issue", List.of(), orderItemId, "spoiled", true, bill.id(), null));

        assertThat(voided.voidType()).isEqualTo("return");
        assertThat(voided.wasDelivered()).isTrue();
        assertThat(voided.returnToStock()).isTrue();
        // stock restored to 5, inbound movement recorded
        assertThat(barRepository.findStockBalance("inventory_void_return", "bar_1"))
                .get().extracting(StockBalanceSnapshot::quantity).isEqualTo(5);
        assertThat(barRepository.stockMovements.stream().anyMatch(m -> "inbound".equals(m.movementType()))).isTrue();
        // bill payable deducted by the voided line amount
        assertThat(barRepository.bills.get(bill.id()).payableAmountCent())
                .isEqualTo(payableBefore - 200000L);
    }

    @Test
    void voidAlreadyVoidedOrderItemIsRejected() {
        BarReservationRecord reservation = createReservation("void-twice", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-void-twice", "create product", List.of(), "bar_1", "Beer", "beer",
                3000L, null, null, "active", null, null, false, null, null, null, null, null, null, null));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-void-twice", "create order", List.of(), "bar_1",
                reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)), List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();
        service.voidOrderItem(order.id(), new BarOrderItemVoidRequest(
                "void-twice-1", "x", List.of(), orderItemId, "first", false, null, null));

        assertThatThrownBy(() -> service.voidOrderItem(order.id(), new BarOrderItemVoidRequest(
                "void-twice-2", "y", List.of(), orderItemId, "second", false, null, null)))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_ORDER_ITEM_ALREADY_VOIDED");
    }

    @Test
    void voidApprovalDecisionWritesBackApprovedStatusToVoidLedger() {
        BarReservationRecord reservation = createReservation("void-approve", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-void-approve", "create product", List.of(), "bar_1", "Beer", "beer",
                3000L, null, null, "active", null, null, false, null, null, null, null, null, null, null));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-void-approve", "create order", List.of(), "bar_1",
                reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)), List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();

        BarOrderItemVoidRecord voided = service.voidOrderItem(order.id(), new BarOrderItemVoidRequest(
                "void-approve-1", "x", List.of(), orderItemId, "guest left", false, null, null));
        // 高危需审批 → 流水落待审批态。
        assertThat(voided.approvalRequired()).isTrue();
        assertThat(voided.status()).isEqualTo("pending");

        BarOrderItemVoidRecord decided = decideAsReviewer(voided.id(),
                new BarOrderItemVoidDecisionRequest("void-approve-decide-1", "approved", "supervisor ok"));

        // 审批通过 → status 回写 approved，落审批人/时点/理由。
        assertThat(decided.status()).isEqualTo("approved");
        assertThat(decided.reviewedByUserId()).isNotNull();
        assertThat(decided.reviewedAt()).isNotNull();
        assertThat(decided.reviewReason()).isEqualTo("supervisor ok");
        assertThat(barRepository.findOrderItemVoidById(voided.id()))
                .get().extracting(BarOrderItemVoidRecord::status).isEqualTo("approved");
    }

    @Test
    void voidRejectionDecisionWritesBackRejectedStatusToVoidLedger() {
        BarReservationRecord reservation = createReservation("void-reject", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-void-reject", "create product", List.of(), "bar_1", "Beer", "beer",
                3000L, null, null, "active", null, null, false, null, null, null, null, null, null, null));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-void-reject", "create order", List.of(), "bar_1",
                reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)), List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();

        BarOrderItemVoidRecord voided = service.voidOrderItem(order.id(), new BarOrderItemVoidRequest(
                "void-reject-1", "x", List.of(), orderItemId, "mistake", false, null, null));
        assertThat(voided.status()).isEqualTo("pending");

        BarOrderItemVoidRecord decided = decideAsReviewer(voided.id(),
                new BarOrderItemVoidDecisionRequest("void-reject-decide-1", "rejected", "not justified"));

        // 审批驳回 → status 回写 rejected。
        assertThat(decided.status()).isEqualTo("rejected");
        assertThat(decided.reviewedByUserId()).isNotNull();
        assertThat(barRepository.findOrderItemVoidById(voided.id()))
                .get().extracting(BarOrderItemVoidRecord::status).isEqualTo("rejected");

        // 已决策（非 pending）再次决策被拒。
        assertThatThrownBy(() -> decideAsReviewer(voided.id(),
                new BarOrderItemVoidDecisionRequest("void-reject-decide-2", "approved", "late")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_ORDER_ITEM_VOID_NOT_PENDING");
    }

    // ========================================================================
    // 延期项④：退菜退酒审批驳回的资金/库存反向冲销（恢复 void 前状态）。
    // ========================================================================

    @Test
    void voidRejectionReversesStockAndBillBackToPreVoidState() {
        BarReservationRecord reservation = createReservation("void-rej-reverse", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-void-rej-reverse", "create product", List.of(), "bar_1", "Champagne", "spirit",
                100000L, null, null, "active", "inventory_void_rej_reverse", "bar_1", true, null, null, null, null, null, null, null));
        barRepository.saveStockBalance(new StockBalanceSnapshot(
                "balance-void-rej-reverse", "inventory_void_rej_reverse", "bar_1", 5, 0, 5, Instant.now()));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-void-rej-reverse", "create order", List.of(), "bar_1",
                reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 2)), List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();
        service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-void-rej-reverse", "deliver", List.of(),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 2)), "staff_1", List.of("att1")));
        // 出品后库存 5→3。
        assertThat(barRepository.findStockBalance("inventory_void_rej_reverse", "bar_1"))
                .get().extracting(StockBalanceSnapshot::quantity).isEqualTo(3);
        BarBillRecord bill = service.createOrderBill(order.id(), new BarOrderBillCreateRequest(
                "bill-void-rej-reverse", "create bill", List.of(), "offline", false, 0));
        long payableBeforeVoid = bill.payableAmountCent();

        // void：回补库存 3→5、账单减 200000。
        BarOrderItemVoidRecord voided = service.voidOrderItem(order.id(), new BarOrderItemVoidRequest(
                "void-rej-reverse-1", "quality issue", List.of(), orderItemId, "spoiled", true, bill.id(), null));
        assertThat(barRepository.findStockBalance("inventory_void_rej_reverse", "bar_1"))
                .get().extracting(StockBalanceSnapshot::quantity).isEqualTo(5);
        assertThat(barRepository.bills.get(bill.id()).payableAmountCent()).isEqualTo(payableBeforeVoid - 200000L);

        // 驳回：对称冲销 → 库存回到 void 前 3、账单回到 void 前 payable、明细恢复 delivered/未撤单。
        BarOrderItemVoidRecord decided = decideAsReviewer(voided.id(),
                new BarOrderItemVoidDecisionRequest("void-rej-reverse-decide", "rejected", "not justified"));
        assertThat(decided.status()).isEqualTo("rejected");
        assertThat(barRepository.findStockBalance("inventory_void_rej_reverse", "bar_1"))
                .get().extracting(StockBalanceSnapshot::quantity).isEqualTo(3);
        assertThat(barRepository.bills.get(bill.id()).payableAmountCent()).isEqualTo(payableBeforeVoid);
        // outbound 冲销流水落库。
        assertThat(barRepository.stockMovements.stream()
                .anyMatch(m -> "outbound".equals(m.movementType())
                        && "bar_order_item_void_reject".equals(m.businessType()))).isTrue();
        // 反向账单调整落 waiver_reversal（未结账单）。
        assertThat(barRepository.billAdjustments.stream()
                .anyMatch(a -> "waiver_reversal".equals(a.adjustmentType()))).isTrue();
        // 明细恢复未撤单 + delivered。
        BarOrderItemRecord restored = barRepository.orderItems.get(orderItemId);
        assertThat(restored.voided()).isFalse();
        assertThat(restored.deliveryStatus()).isEqualTo("delivered");
    }

    @Test
    void voidApprovalKeepsAppliedSideEffectsWithoutReversal() {
        BarReservationRecord reservation = createReservation("void-appr-keep", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-void-appr-keep", "create product", List.of(), "bar_1", "Champagne", "spirit",
                100000L, null, null, "active", "inventory_void_appr_keep", "bar_1", true, null, null, null, null, null, null, null));
        barRepository.saveStockBalance(new StockBalanceSnapshot(
                "balance-void-appr-keep", "inventory_void_appr_keep", "bar_1", 5, 0, 5, Instant.now()));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-void-appr-keep", "create order", List.of(), "bar_1",
                reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 2)), List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();
        service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-void-appr-keep", "deliver", List.of(),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 2)), "staff_1", List.of("att1")));
        BarBillRecord bill = service.createOrderBill(order.id(), new BarOrderBillCreateRequest(
                "bill-void-appr-keep", "create bill", List.of(), "offline", false, 0));
        long payableBeforeVoid = bill.payableAmountCent();

        BarOrderItemVoidRecord voided = service.voidOrderItem(order.id(), new BarOrderItemVoidRequest(
                "void-appr-keep-1", "quality issue", List.of(), orderItemId, "spoiled", true, bill.id(), null));
        // void 副作用：库存回补 5、账单减项。
        long payableAfterVoid = barRepository.bills.get(bill.id()).payableAmountCent();
        assertThat(payableAfterVoid).isEqualTo(payableBeforeVoid - 200000L);

        // 通过：保持已施加副作用，不冲销。
        decideAsReviewer(voided.id(),
                new BarOrderItemVoidDecisionRequest("void-appr-keep-decide", "approved", "supervisor ok"));
        assertThat(barRepository.findStockBalance("inventory_void_appr_keep", "bar_1"))
                .get().extracting(StockBalanceSnapshot::quantity).isEqualTo(5);
        assertThat(barRepository.bills.get(bill.id()).payableAmountCent()).isEqualTo(payableAfterVoid);
        // 无冲销流水/反向调整。
        assertThat(barRepository.stockMovements.stream()
                .noneMatch(m -> "bar_order_item_void_reject".equals(m.businessType()))).isTrue();
        assertThat(barRepository.billAdjustments.stream()
                .noneMatch(a -> a.adjustmentType().endsWith("_reversal"))).isTrue();
        assertThat(barRepository.orderItems.get(orderItemId).voided()).isTrue();
    }

    @Test
    void voidRejectionReversalIsIdempotentBecauseSecondDecisionIsBlocked() {
        BarReservationRecord reservation = createReservation("void-rej-idem", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-void-rej-idem", "create product", List.of(), "bar_1", "Champagne", "spirit",
                100000L, null, null, "active", "inventory_void_rej_idem", "bar_1", true, null, null, null, null, null, null, null));
        barRepository.saveStockBalance(new StockBalanceSnapshot(
                "balance-void-rej-idem", "inventory_void_rej_idem", "bar_1", 5, 0, 5, Instant.now()));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-void-rej-idem", "create order", List.of(), "bar_1",
                reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 2)), List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();
        service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-void-rej-idem", "deliver", List.of(),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 2)), "staff_1", List.of("att1")));
        BarBillRecord bill = service.createOrderBill(order.id(), new BarOrderBillCreateRequest(
                "bill-void-rej-idem", "create bill", List.of(), "offline", false, 0));
        BarOrderItemVoidRecord voided = service.voidOrderItem(order.id(), new BarOrderItemVoidRequest(
                "void-rej-idem-1", "quality issue", List.of(), orderItemId, "spoiled", true, bill.id(), null));

        decideAsReviewer(voided.id(),
                new BarOrderItemVoidDecisionRequest("void-rej-idem-decide-1", "rejected", "not justified"));
        int stockAfterFirst = barRepository.findStockBalance("inventory_void_rej_idem", "bar_1")
                .map(StockBalanceSnapshot::quantity).orElseThrow();
        long reversalAdjustmentsAfterFirst = barRepository.billAdjustments.stream()
                .filter(a -> a.adjustmentType().endsWith("_reversal")).count();

        // 二次决策被 pending 守卫拦截 → 不会重复冲销（幂等）。
        assertThatThrownBy(() -> decideAsReviewer(voided.id(),
                new BarOrderItemVoidDecisionRequest("void-rej-idem-decide-2", "rejected", "again")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_ORDER_ITEM_VOID_NOT_PENDING");
        assertThat(barRepository.findStockBalance("inventory_void_rej_idem", "bar_1")
                .map(StockBalanceSnapshot::quantity).orElseThrow()).isEqualTo(stockAfterFirst);
        assertThat(barRepository.billAdjustments.stream()
                .filter(a -> a.adjustmentType().endsWith("_reversal")).count())
                .isEqualTo(reversalAdjustmentsAfterFirst);
    }

    // ========================================================================
    // V125 P1-1：void 驳回库存双扣防护——并发/重复 reject 库存仅扣一次（未结 waiver_reversal 分支）。
    // V125 P1-2：void 审批分离——decideOrderItemVoid 需 bar.order_item.void_decide，
    //            仅持申请权限 bar.order_item.void 的 actor 不得自审批。
    // ========================================================================

    @Test
    void voidRejectionDeductsStockOnlyOnceForUnsettledWaiverReversalBranch() {
        // 未结账单 void 驳回：库存回补→驳回重新扣减（waiver_reversal 加回应付）。
        // 红线：行锁 + status==pending 守卫使第二次 reject 被拒，库存只扣一次，waiver_reversal 仅一条。
        BarReservationRecord reservation = createReservation("void-rej-once", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-void-rej-once", "create product", List.of(), "bar_1", "Champagne", "spirit",
                100000L, null, null, "active", "inventory_void_rej_once", "bar_1", true, null, null, null, null, null, null, null));
        barRepository.saveStockBalance(new StockBalanceSnapshot(
                "balance-void-rej-once", "inventory_void_rej_once", "bar_1", 5, 0, 5, Instant.now()));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-void-rej-once", "create order", List.of(), "bar_1",
                reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 2)), List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();
        service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-void-rej-once", "deliver", List.of(),
                List.of(Map.of("order_item_id", orderItemId, "quantity", 2)), "staff_1", List.of("att1")));
        // 出品后库存 5→3。
        BarBillRecord bill = service.createOrderBill(order.id(), new BarOrderBillCreateRequest(
                "bill-void-rej-once", "create bill", List.of(), "offline", false, 0));
        long payableBeforeVoid = bill.payableAmountCent();

        // void：回补库存 3→5、账单减 200000（未结 → 后续驳回走 waiver_reversal）。
        BarOrderItemVoidRecord voided = service.voidOrderItem(order.id(), new BarOrderItemVoidRequest(
                "void-rej-once-1", "quality issue", List.of(), orderItemId, "spoiled", true, bill.id(), null));
        assertThat(barRepository.findStockBalance("inventory_void_rej_once", "bar_1"))
                .get().extracting(StockBalanceSnapshot::quantity).isEqualTo(5);

        // 第一次驳回：对称冲销，库存 5→3，落一条 waiver_reversal、应付加回。
        decideAsReviewer(voided.id(),
                new BarOrderItemVoidDecisionRequest("void-rej-once-decide-1", "rejected", "not justified"));
        assertThat(barRepository.findStockBalance("inventory_void_rej_once", "bar_1"))
                .get().extracting(StockBalanceSnapshot::quantity).isEqualTo(3);
        assertThat(barRepository.bills.get(bill.id()).payableAmountCent()).isEqualTo(payableBeforeVoid);
        assertThat(barRepository.billAdjustments.stream()
                .filter(a -> "waiver_reversal".equals(a.adjustmentType())).count()).isEqualTo(1L);

        // 第二次驳回（不同 idempotencyKey）：行锁内 status 已非 pending → 被拒，库存不再扣减、不再加 waiver_reversal。
        assertThatThrownBy(() -> decideAsReviewer(voided.id(),
                new BarOrderItemVoidDecisionRequest("void-rej-once-decide-2", "rejected", "again")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_ORDER_ITEM_VOID_NOT_PENDING");
        // 库存仍为 3（仅扣一次），waiver_reversal 仍仅一条。
        assertThat(barRepository.findStockBalance("inventory_void_rej_once", "bar_1"))
                .get().extracting(StockBalanceSnapshot::quantity).isEqualTo(3);
        assertThat(barRepository.billAdjustments.stream()
                .filter(a -> "waiver_reversal".equals(a.adjustmentType())).count()).isEqualTo(1L);
    }

    // ========================================================================
    // P0-C / FLOW-07（V345·BAR-VOID-A1）高价值已出品退酒「先审后行」
    // 口径：仅已出品进入判定；阈值后台可配（默认 5000 元）；超阈值先审后行 + 双证据；
    //       低于阈值即时生效不受影响。审批通过前不得扣库存、不得改账单金额、不得进分账。
    // ========================================================================

    /** 造一笔「已出品 + 已出账单」的高价值明细（单价 3000 元 × 2 = 6000 元 > 默认阈值 5000 元）。 */
    private HighValueVoidFixture highValueDeliveredItem(String key, long unitPriceCent, int quantity) {
        BarReservationRecord reservation = createReservation(key, "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-" + key, "create product", List.of(), "bar_1", "Vintage Champagne", "wine",
                unitPriceCent, null, null, "active", "inventory_" + key, "bar_1", true, null, null, null, null, null, null, null));
        barRepository.saveStockBalance(new StockBalanceSnapshot(
                "balance-" + key, "inventory_" + key, "bar_1", 5, 0, 5, Instant.now()));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-" + key, "create order", List.of(), "bar_1",
                reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", quantity)), List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();
        service.deliverOrder(order.id(), new BarOrderDeliveryRequest(
                "deliver-" + key, "deliver", List.of(),
                List.of(Map.of("order_item_id", orderItemId, "quantity", quantity)), "staff_1", List.of("att1")));
        BarBillRecord bill = service.createOrderBill(order.id(), new BarOrderBillCreateRequest(
                "bill-" + key, "create bill", List.of(), "offline", false, 0));
        return new HighValueVoidFixture(order, orderItemId, bill,
                barRepository.findStockBalance("inventory_" + key, "bar_1").orElseThrow().quantity(),
                "inventory_" + key);
    }

    private record HighValueVoidFixture(BarOrderRecord order, String orderItemId, BarBillRecord bill,
                                        int stockAfterDelivery, String inventoryItemId) {
    }

    @Test
    void highValueDeliveredVoidStaysPendingWithoutTouchingStockBillOrItem() {
        highValueStub.highValue = true;
        HighValueVoidFixture f = highValueDeliveredItem("void-highvalue-hold", 300_000L, 2);
        long payableBefore = f.bill().payableAmountCent();
        int adjustmentsBefore = barRepository.billAdjustments.size();

        BarOrderItemVoidRecord voided = service.voidOrderItem(f.order().id(), new BarOrderItemVoidRequest(
                "void-highvalue-hold-1", "quality issue", List.of("att-void-photo"), f.orderItemId(),
                "bottle spoiled", true, f.bill().id(), "quality_defect"));

        assertThat(voided.voidType()).isEqualTo("return");
        assertThat(voided.approvalMode()).isEqualTo("approval_first");
        assertThat(voided.status()).isEqualTo("pending");
        assertThat(voided.highValueThresholdCent()).isEqualTo(500_000L);
        assertThat(voided.effectsAppliedAt()).isNull();
        // 🔴 先审后行三不动：库存不动、账单金额不动、明细不标 voided（因而也没有任何减项进分账）。
        assertThat(barRepository.findStockBalance(f.inventoryItemId(), "bar_1"))
                .get().extracting(StockBalanceSnapshot::quantity).isEqualTo(f.stockAfterDelivery());
        assertThat(barRepository.bills.get(f.bill().id()).payableAmountCent()).isEqualTo(payableBefore);
        assertThat(barRepository.billAdjustments).hasSize(adjustmentsBefore);
        assertThat(barRepository.orderItems.get(f.orderItemId()).voided()).isFalse();
        assertThat(barRepository.orderItems.get(f.orderItemId()).deliveryStatus()).isEqualTo("delivered");
        assertThat(voided.inventoryMovementId()).isNull();
        assertThat(voided.billAdjustmentId()).isNull();
        assertThat(barRepository.stockMovements.stream().noneMatch(m -> "inbound".equals(m.movementType()))).isTrue();
    }

    @Test
    void highValueDeliveredVoidAppliesStockAndBillOnlyAfterApproval() {
        highValueStub.highValue = true;
        HighValueVoidFixture f = highValueDeliveredItem("void-highvalue-approve", 300_000L, 2);
        long payableBefore = f.bill().payableAmountCent();
        BarOrderItemVoidRecord voided = service.voidOrderItem(f.order().id(), new BarOrderItemVoidRequest(
                "void-highvalue-approve-1", "quality issue", List.of("att-void-photo", "att-void-video"), f.orderItemId(),
                "bottle spoiled", true, f.bill().id(), "quality_defect"));

        BarOrderItemVoidRecord decided = decideAsReviewer(voided.id(),
                new BarOrderItemVoidDecisionRequest("decide-highvalue-approve", "approved", "supervisor ok"));

        assertThat(decided.status()).isEqualTo("approved");
        assertThat(decided.effectsAppliedAt()).isNotNull();
        // 审批通过后才动：库存回补、账单减项、明细标 voided。
        assertThat(barRepository.findStockBalance(f.inventoryItemId(), "bar_1"))
                .get().extracting(StockBalanceSnapshot::quantity).isEqualTo(f.stockAfterDelivery() + 2);
        assertThat(barRepository.bills.get(f.bill().id()).payableAmountCent())
                .isEqualTo(payableBefore - 600_000L);
        assertThat(barRepository.orderItems.get(f.orderItemId()).voided()).isTrue();
        assertThat(decided.billAdjustmentId()).isNotNull();
        assertThat(decided.inventoryMovementId()).isNotNull();
        assertThat(barRepository.billAdjustments.stream()
                .filter(a -> "waiver".equals(a.adjustmentType())).count()).isEqualTo(1L);
    }

    @Test
    void highValueApprovalRequiresVideoAndRechecksTheStoredFile() throws Exception {
        highValueStub.highValue = true;
        HighValueVoidFixture f = highValueDeliveredItem("void-evidence-review", 300_000L, 2);
        BarOrderItemVoidRecord pending = service.voidOrderItem(f.order().id(), new BarOrderItemVoidRequest(
                "void-evidence-review-1", "quality issue", List.of("att-void-photo"), f.orderItemId(),
                "bottle spoiled", true, f.bill().id(), "quality_defect"));
        assertThatThrownBy(() -> decideAsReviewer(pending.id(),
                new BarOrderItemVoidDecisionRequest("decide-missing-video", "approved", "confirmed")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("EVIDENCE_PHOTO_VIDEO_REQUIRED"));
        assertThat(barRepository.orderItems.get(f.orderItemId()).voided()).isFalse();
        assertThat(barRepository.bills.get(f.bill().id()).payableAmountCent()).isEqualTo(f.bill().payableAmountCent());

        HighValueVoidFixture g = highValueDeliveredItem("void-lost-evidence", 300_000L, 2);
        BarOrderItemVoidRecord withBoth = service.voidOrderItem(g.order().id(), new BarOrderItemVoidRequest(
                "void-lost-evidence-1", "quality issue", List.of("att-void-photo", "att-void-video"), g.orderItemId(),
                "bottle spoiled", true, g.bill().id(), "quality_defect"));
        java.nio.file.Files.delete(evidenceDirectory.resolve("v.mp4"));
        assertThatThrownBy(() -> decideAsReviewer(withBoth.id(),
                new BarOrderItemVoidDecisionRequest("decide-lost-file", "approved", "confirmed")))
                .isInstanceOf(ApiException.class);
        assertThat(barRepository.orderItems.get(g.orderItemId()).voided()).isFalse();
        assertThat(barRepository.bills.get(g.bill().id()).payableAmountCent()).isEqualTo(g.bill().payableAmountCent());
    }

    @Test
    void highValueDeliveredVoidRejectionLeavesMoneyAndStockUntouchedWithNoReversal() {
        highValueStub.highValue = true;
        HighValueVoidFixture f = highValueDeliveredItem("void-highvalue-reject", 300_000L, 2);
        long payableBefore = f.bill().payableAmountCent();
        BarOrderItemVoidRecord voided = service.voidOrderItem(f.order().id(), new BarOrderItemVoidRequest(
                "void-highvalue-reject-1", "quality issue", List.of("att-void-photo"), f.orderItemId(),
                "bottle spoiled", true, f.bill().id(), "quality_defect"));

        BarOrderItemVoidRecord decided = decideAsReviewer(voided.id(),
                new BarOrderItemVoidDecisionRequest("decide-highvalue-reject", "rejected", "证据不足"));

        assertThat(decided.status()).isEqualTo("rejected");
        assertThat(decided.effectsAppliedAt()).isNull();
        // 从未生效 → 没有正向减项、也没有任何冲销（这正是 A1 相对旧「先生效后审」的价值）。
        assertThat(barRepository.bills.get(f.bill().id()).payableAmountCent()).isEqualTo(payableBefore);
        assertThat(barRepository.findStockBalance(f.inventoryItemId(), "bar_1"))
                .get().extracting(StockBalanceSnapshot::quantity).isEqualTo(f.stockAfterDelivery());
        assertThat(barRepository.orderItems.get(f.orderItemId()).voided()).isFalse();
        assertThat(barRepository.billAdjustments.stream()
                .noneMatch(a -> a.adjustmentType().contains("waiver") || a.adjustmentType().contains("refund")))
                .isTrue();
    }

    @Test
    void lowValueDeliveredVoidTakesEffectImmediatelyWithoutWaitingForApproval() {
        // A1 明确允许低价值即时生效：不能一刀切全拦。单价 1000 元 × 2 = 2000 元 ≤ 5000 元阈值。
        HighValueVoidFixture f = highValueDeliveredItem("void-lowvalue-now", 100_000L, 2);
        long payableBefore = f.bill().payableAmountCent();

        BarOrderItemVoidRecord voided = service.voidOrderItem(f.order().id(), new BarOrderItemVoidRequest(
                "void-lowvalue-now-1", "ops", List.of(), f.orderItemId(), "guest changed mind", true,
                f.bill().id(), null));

        assertThat(voided.approvalMode()).isEqualTo("immediate");
        assertThat(voided.effectsAppliedAt()).isNotNull();
        assertThat(barRepository.bills.get(f.bill().id()).payableAmountCent())
                .isEqualTo(payableBefore - 200_000L);
        assertThat(barRepository.orderItems.get(f.orderItemId()).voided()).isTrue();
        assertThat(barRepository.findStockBalance(f.inventoryItemId(), "bar_1"))
                .get().extracting(StockBalanceSnapshot::quantity).isEqualTo(f.stockAfterDelivery() + 2);
    }

    @Test
    void voidHighValueFollowsItemPurchasePriceNotBillAmount() {
        // V350 归一：判定问的是「这件物品进货价多少」，问的对象是该明细商品关联的库存物料，
        // 不再是「本条明细账单金额 > 5000 元」——同一笔金额，判定结果只随物品口径走。
        highValueStub.highValue = true;
        HighValueVoidFixture f = highValueDeliveredItem("void-by-cost", 100_000L, 2);
        long payableBefore = f.bill().payableAmountCent();

        BarOrderItemVoidRecord voided = service.voidOrderItem(f.order().id(), new BarOrderItemVoidRequest(
                "void-by-cost-1", "ops", List.of("att-void-photo"), f.orderItemId(),
                "guest changed mind", true, f.bill().id(), "customer_change"));

        assertThat(voided.approvalMode()).isEqualTo("approval_first");
        assertThat(voided.highValueThresholdCent()).isEqualTo(500_000L);
        assertThat(barRepository.bills.get(f.bill().id()).payableAmountCent()).isEqualTo(payableBefore);
        // 判定入口被问到的是该商品关联的库存物料 id（进货价的载体），而不是订单/明细金额。
        assertThat(highValueStub.lastInventoryItemId).isEqualTo(f.inventoryItemId());

        // 同样金额、同样明细：物品被判低价值 → 回到即时生效路径。
        highValueStub.highValue = false;
        HighValueVoidFixture g = highValueDeliveredItem("void-by-cost-2", 100_000L, 2);
        BarOrderItemVoidRecord immediate = service.voidOrderItem(g.order().id(), new BarOrderItemVoidRequest(
                "void-by-cost-2-1", "ops", List.of(), g.orderItemId(),
                "guest changed mind", true, g.bill().id(), null));
        assertThat(immediate.approvalMode()).isEqualTo("immediate");
    }

    @Test
    void highValueVoidWithoutStructuredReasonCodeIsRejected() {
        highValueStub.highValue = true;
        HighValueVoidFixture f = highValueDeliveredItem("void-noreasoncode", 300_000L, 2);

        assertThatThrownBy(() -> service.voidOrderItem(f.order().id(), new BarOrderItemVoidRequest(
                "void-noreasoncode-1", "quality issue", List.of("att-void-photo"), f.orderItemId(),
                "bottle spoiled", true, f.bill().id(), null)))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_VOID_EVIDENCE_REQUIRED");
    }

    @Test
    void highValueVoidWithoutAttachmentEvidenceIsRejected() {
        highValueStub.highValue = true;
        HighValueVoidFixture f = highValueDeliveredItem("void-noattachment", 300_000L, 2);

        assertThatThrownBy(() -> service.voidOrderItem(f.order().id(), new BarOrderItemVoidRequest(
                "void-noattachment-1", "quality issue", List.of(), f.orderItemId(),
                "bottle spoiled", true, f.bill().id(), "quality_defect")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_VOID_EVIDENCE_REQUIRED");
        // 双证据缺一即拒 → 库存/账单一律未动。
        assertThat(barRepository.orderItems.get(f.orderItemId()).voided()).isFalse();
    }

    @Test
    void duplicatePendingHighValueVoidRequestOnSameItemIsRejected() {
        highValueStub.highValue = true;
        HighValueVoidFixture f = highValueDeliveredItem("void-duplicate-pending", 300_000L, 2);
        service.voidOrderItem(f.order().id(), new BarOrderItemVoidRequest(
                "void-duplicate-pending-1", "quality issue", List.of("att-void-photo"), f.orderItemId(),
                "bottle spoiled", true, f.bill().id(), "quality_defect"));

        // 先审后行模式下明细未被标 voided，若无守卫则可重复申请 → 两笔各自被批 = 双倍退款 + 双倍回库。
        assertThatThrownBy(() -> service.voidOrderItem(f.order().id(), new BarOrderItemVoidRequest(
                "void-duplicate-pending-2", "quality issue", List.of("att-void-photo"), f.orderItemId(),
                "bottle spoiled", true, f.bill().id(), "quality_defect")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_ORDER_ITEM_VOID_PENDING_EXISTS");
    }

    @Test
    void undeliveredCancelIsNeverApprovalFirstEvenAboveThreshold() {
        // A1「只审已出品」：出品前 cancel 即便金额高于阈值也不强制先审后行。
        BarReservationRecord reservation = createReservation("void-cancel-highamount", "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-void-cancel-highamount", "create product", List.of(), "bar_1", "Vintage", "wine",
                800_000L, null, null, "active", null, null, false, null, null, null, null, null, null, null));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-void-cancel-highamount", "create order", List.of(), "bar_1",
                reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)), List.of(), "offline"));
        String orderItemId = barRepository.listOrderItems(order.id()).getFirst().id();

        BarOrderItemVoidRecord voided = service.voidOrderItem(order.id(), new BarOrderItemVoidRequest(
                "void-cancel-highamount-1", "ops", List.of(), orderItemId, "wrong order", false, null, null));

        assertThat(voided.voidType()).isEqualTo("cancel");
        assertThat(voided.approvalMode()).isEqualTo("immediate");
        assertThat(voided.effectsAppliedAt()).isNotNull();
        assertThat(barRepository.orderItems.get(orderItemId).voided()).isTrue();
    }

    @Test
    void voidIsHeldForApprovalWhenHighValueDecisionIsUnavailable() {
        // V350 fail-closed：判定器/阈值不可用时**按高价值拦下走审批**（业务HTML「阈值未配置一律按高价值处理、
        // 不得静默放行」），而不是让退酒整个入口不可用——主管仍能逐单放行，补配置后自动恢复分流。
        barOrderService.highValueDecisionService = null;
        HighValueVoidFixture f = highValueDeliveredItem("void-config-missing", 300_000L, 2);
        long payableBefore = f.bill().payableAmountCent();

        BarOrderItemVoidRecord voided = service.voidOrderItem(f.order().id(), new BarOrderItemVoidRequest(
                "void-config-missing-1", "quality issue", List.of("att-void-photo"), f.orderItemId(),
                "bottle spoiled", true, f.bill().id(), "quality_defect"));

        assertThat(voided.approvalMode()).isEqualTo("approval_first");
        assertThat(voided.highValueThresholdCent()).isNull(); // 阈值未配置的留痕快照
        assertThat(voided.effectsAppliedAt()).isNull();
        assertThat(barRepository.bills.get(f.bill().id()).payableAmountCent()).isEqualTo(payableBefore);
        assertThat(barRepository.orderItems.get(f.orderItemId()).voided()).isFalse();
    }

    @Test
    void voidReasonCodeOutsideDictionaryIsRejected() {
        // V351：原因码只认字典里的生效码。改造前只校形状，随手编的短码照样过闸，
        // 事后按原因码聚合退酒（哪家店质量问题多）全是垃圾数据。
        highValueStub.highValue = true;
        HighValueVoidFixture f = highValueDeliveredItem("void-badreason", 300_000L, 2);

        assertThatThrownBy(() -> service.voidOrderItem(f.order().id(), new BarOrderItemVoidRequest(
                "void-badreason-1", "quality issue", List.of("att-void-photo"), f.orderItemId(),
                "bottle spoiled", true, f.bill().id(), "asdf")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_VOID_REASON_CODE_UNKNOWN");
        assertThat(barRepository.orderItems.get(f.orderItemId()).voided()).isFalse();
    }

    @Test
    void voidWithNonExistentAttachmentIdIsRejected() {
        // 双证据闭环：附件 id 必须真实存在于 evidence_attachments，不能随手编字符串。
        highValueStub.highValue = true;
        HighValueVoidFixture f = highValueDeliveredItem("void-fakeatt", 300_000L, 2);

        assertThatThrownBy(() -> service.voidOrderItem(f.order().id(), new BarOrderItemVoidRequest(
                "void-fakeatt-1", "quality issue", List.of("att-does-not-exist"), f.orderItemId(),
                "bottle spoiled", true, f.bill().id(), "quality_defect")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("EVIDENCE_ATTACHMENT_NOT_FOUND");
        assertThat(barRepository.orderItems.get(f.orderItemId()).voided()).isFalse();
    }

    @Test
    void voidWithUploadedAttachmentAndDictionaryReasonCodePasses() {
        // 正例：真上传过的附件 + 字典里的中文原因码 → 受理为先审后行申请。
        highValueStub.highValue = true;
        HighValueVoidFixture f = highValueDeliveredItem("void-goodevidence", 300_000L, 2);

        BarOrderItemVoidRecord voided = service.voidOrderItem(f.order().id(), new BarOrderItemVoidRequest(
                "void-goodevidence-1", "quality issue", List.of("att-void-photo"), f.orderItemId(),
                "bottle spoiled", true, f.bill().id(), "quality_defect"));

        assertThat(voided.approvalMode()).isEqualTo("approval_first");
        assertThat(voided.reasonCode()).isEqualTo("quality_defect");
        assertThat(voided.evidenceAttachmentIds()).containsExactly("att-void-photo");
    }

    @Test
    void decideOrderItemVoidRequiresVoidDecidePermissionDistinctFromVoidApply() {
        // 审批分离：持申请权限 bar.order_item.void 但无 bar.order_item.void_decide 的 actor → PERMISSION_DENIED。
        // 用真实权限闸（非 platform_admin bridge）：自建 AuthorizationService + service，actor 为 merchant 用户。
        AccessRepository accessRepository = mock(AccessRepository.class);
        when(accessRepository.permissionExists(anyString())).thenReturn(true);
        // 只授予申请权限，不授予审批权限。
        when(accessRepository.findPermissionEffects(anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(new AccessRepository.PermissionEffect(
                        "usr_merchant_voider", "bar.order_item.void",
                        AccessRepository.Effect.GRANT, AccessRepository.Source.ROLE,
                        "role_bar_staff", null, null, false, false)));
        AuthorizationService scopedAuth = new AuthorizationService(accessRepository);
        BarOrderService scopedOrderService = new BarOrderService(
                barRepository, scopedAuth,
                new AuditEventService(auditEventRepository),
                new TraceabilityService(mock(TraceabilityRepository.class), scopedAuth),
                receivableLedgerService);

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new com.complex.platform.access.AccessActor(
                                "usr_merchant_voider", "merchant_voider", "staff",
                                "org_1", "merchant_a", "active"),
                        "token", List.of()));

        assertThatThrownBy(() -> scopedOrderService.decideOrderItemVoid("void_any",
                new BarOrderItemVoidDecisionRequest("decide-perm", "approved", "self approve")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("PERMISSION_DENIED");
    }

    @Test
    void reopenSettledBillTransitionsToReopenedAndAllowsResettle() {
        BarBillRecord bill = billForOrder("reopen-flow", null);
        service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-reopen-flow", "settle", List.of(), "pay_reopen_flow", null, "cash"));
        assertThat(barRepository.bills.get(bill.id()).settlementStatus()).isEqualTo("settled");

        BarBillOperationRecord op = service.reopenBill(bill.id(), new BarBillReopenRequest(
                "reopen-flow-1", "reopen", List.of(), "wrong payment method"));

        assertThat(op.operationType()).isEqualTo("reopen");
        BarBillRecord reopened = barRepository.bills.get(bill.id());
        assertThat(reopened.reopened()).isTrue();
        assertThat(reopened.settlementStatus()).isEqualTo("unsettled");
        assertThat(reopened.reopenCount()).isEqualTo(1);

        // re-settle after reopen succeeds
        BarBillRecord resettled = service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-reopen-flow-2", "resettle", List.of(), "pay_reopen_flow_2", null, "pos"));
        assertThat(resettled.settlementStatus()).isEqualTo("settled");
    }

    @Test
    void reopenUnsettledBillIsRejected() {
        BarBillRecord bill = billForOrder("reopen-unsettled", null);
        assertThatThrownBy(() -> service.reopenBill(bill.id(), new BarBillReopenRequest(
                "reopen-unsettled-1", "reopen", List.of(), "no")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_BILL_NOT_REOPENABLE");
    }

    @Test
    void reopenSettledBillOnLockedBusinessDayRequiresBackfill() {
        BarBillRecord bill = billForOrder("reopen-locked", null);
        service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-reopen-locked", "settle", List.of(), "pay_reopen_locked", null, "cash"));
        receivableLedgerService.businessDayLocked = true;

        assertThatThrownBy(() -> service.reopenBill(bill.id(), new BarBillReopenRequest(
                "reopen-locked-1", "reopen", List.of(), "late fix")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("DAY_CLOSE_LOCKED");
    }

    @Test
    void soldOutProductIsRejectedOnOrderThenRestored() {
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-soldout", "create product", List.of(), "bar_1", "Limited Whisky", "spirit",
                500000L, null, null, "active", null, null, false, null, null, null, null, null, null, null));
        BarReservationRecord reservation = createReservation("soldout", "bar_1", 2);

        BarProductRecord soldOut = service.markProductSoldOut(product.id(), new BarProductSoldOutRequest(
                "soldout-1", "out of stock", List.of(), true, "kegs empty"));
        assertThat(soldOut.soldOut()).isTrue();

        assertThatThrownBy(() -> service.createOrder(new BarOrderRequest(
                "order-soldout", "create order", List.of(), "bar_1",
                reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)), List.of(), "offline")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("PRODUCT_SOLD_OUT");

        BarProductRecord restored = service.markProductSoldOut(product.id(), new BarProductSoldOutRequest(
                "soldout-2", "back in stock", List.of(), false, "refilled"));
        assertThat(restored.soldOut()).isFalse();

        // now ordering succeeds
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-soldout-ok", "create order", List.of(), "bar_1",
                reservation.tableId(), reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)), List.of(), "offline"));
        assertThat(order.totalAmountCent()).isEqualTo(500000L);
    }

    @Test
    void addTipIncreasesBillAndSettlementIncludesTip() {
        BarBillRecord bill = billForOrder("tip-flow", null);
        long payable = bill.payableAmountCent();

        BarBillRecord tipped = service.addBillTip(bill.id(), new BarBillTipRequest(
                "tip-flow-1", "tip", List.of(), 5000L, null));
        assertThat(tipped.tipAmountCent()).isEqualTo(5000L);

        // 同单组合支付未足额不完成，渠道只记录本次实收，无须手工分商品和小费。
        BarBillRecord first = service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-tip-first", "settle", List.of(), "pay_tip_first", payable, "pos"));
        assertThat(first.settlementStatus()).isEqualTo("partial_settled");
        assertThat(first.paymentStatus()).isEqualTo("partial_paid");
        BarBillRecord settled = service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-tip-flow", "settle", List.of(), "pay_tip_flow", 5000L, "aggregate_pay"));
        assertThat(settled.settlementStatus()).isEqualTo("settled");
        assertThat(settled.paidAmountCent()).isEqualTo(payable + 5000L);
    }

    @Test
    void settlementExceedingPayablePlusTipIsRejected() {
        BarBillRecord bill = billForOrder("tip-exceed", null);
        long payable = bill.payableAmountCent();
        service.addBillTip(bill.id(), new BarBillTipRequest("tip-exceed-1", "tip", List.of(), 1000L, null));

        assertThatThrownBy(() -> service.settleBill(bill.id(), new BarBillSettleRequest(
                "settle-tip-exceed", "settle", List.of(), "pay_tip_exceed", payable + 1000L + 1L, "cash")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    private BarReservationRecord createReservation(String key, String barId, int allowedFreeGuests) {
        BarTableRecord table = service.createTable(new BarTableRequest(
                "table-" + key,
                "create table",
                List.of(),
                barId,
                "VIP " + key,
                "card_seat",
                50000L,
                allowedFreeGuests,
                "active"
        ));
        return service.createReservation(new BarReservationRequest(
                key,
                "create reservation",
                List.of(),
                barId,
                table.id(),
                Instant.parse("2026-06-20T21:00:00Z"),
                allowedFreeGuests,
                "staff_sales",
                null,
                null,
                Instant.parse("2030-06-20T23:00:00Z"), "13800000000"
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // V095: 交班 / 班结对账（PRD §10.1）
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void closeShiftAggregatesSettlementsByChannelRefundAndTipIntoSystemReceivable() {
        BarShiftRecord shift = service.openShift(new BarShiftOpenRequest(
                "open-1", "bar_1", "cashier_1", null, 50_000L, 0L, null));

        // 三笔不同支付方式结算 + 一笔小费账单，均归属 cashier_1。
        BarBillRecord cashBill = billForOrder("agg-cash", "staff_sales");
        service.settleBill(cashBill.id(), new BarBillSettleRequest(
                "s-cash", null, List.of(), "pay_cash", cashBill.payableAmountCent(),
                "cash", "cashier_1", null));
        BarBillRecord posBill = billForOrder("agg-pos", "staff_sales");
        service.settleBill(posBill.id(), new BarBillSettleRequest(
                "s-pos", null, List.of(), "pay_pos", posBill.payableAmountCent(),
                "pos", "cashier_1", "POS123"));
        BarBillRecord onlineBill = billForOrder("agg-online", "staff_sales");
        service.settleBill(onlineBill.id(), new BarBillSettleRequest(
                "s-online", null, List.of(), "pay_online", onlineBill.payableAmountCent(),
                "aggregate_pay", "cashier_1", null));

        // 非现金小费由公司代收并单列；现金小费直接交给员工，不进钱箱。
        BarBillRecord tipBill = billForOrder("agg-tip", "staff_sales");
        service.addBillTip(tipBill.id(), new BarBillTipRequest("tip-1", null, List.of(), 1_000L, null));
        BarBillRecord tippedReloaded = barRepository.findBillById(tipBill.id()).orElseThrow();
        service.settleBill(tipBill.id(), new BarBillSettleRequest(
                "s-tip", null, List.of(), "pay_tip",
                tippedReloaded.payableAmountCent() + tippedReloaded.tipAmountCent(),
                "aggregate_pay", "cashier_1", null));

        BarShiftRecord closed = service.closeShift(shift.id(), new BarShiftCloseRequest(
                "close-1", 50_000L + cashBill.payableAmountCent(),
                "cashier_2", "end of night"));

        assertThat(closed.status()).isEqualTo("closed");
        assertThat(closed.systemCashCent())
                .isEqualTo(cashBill.payableAmountCent());
        assertThat(closed.systemPosCent()).isEqualTo(posBill.payableAmountCent());
        assertThat(closed.systemOnlineCent()).isEqualTo(onlineBill.payableAmountCent()
                + tippedReloaded.payableAmountCent() + tippedReloaded.tipAmountCent());
        assertThat(closed.systemTipCent()).isEqualTo(1_000L);
        assertThat(closed.systemRefundCent()).isZero();
        assertThat(closed.handoverToUserId()).isEqualTo("cashier_2");
    }

    @Test
    void closeShiftComputesCashDiffAgainstDeclaredCashAndOpeningFloat() {
        BarShiftRecord shift = service.openShift(new BarShiftOpenRequest(
                "open-diff", "bar_1", "cashier_diff", null, 20_000L, 0L, null));
        BarBillRecord cashBill = billForOrder("diff-cash", "staff_sales");
        service.settleBill(cashBill.id(), new BarBillSettleRequest(
                "s-diff", null, List.of(), "pay_diff", cashBill.payableAmountCent(),
                "cash", "cashier_diff", null));

        // 实点现金 = 备用金 + 系统现金 + 300 长款。
        long declared = 20_000L + cashBill.payableAmountCent() + 300L;
        BarShiftRecord closed = service.closeShift(shift.id(), new BarShiftCloseRequest(
                "close-diff", declared, null, null));

        assertThat(closed.systemCashCent()).isEqualTo(cashBill.payableAmountCent());
        assertThat(closed.cashDiffCent()).isEqualTo(300L);
        assertThat(closed.needsReview()).isTrue();
    }

    @Test
    void closeShiftBalancesWhenDeclaredCashMatchesSystemAndDoesNotNeedReview() {
        BarShiftRecord shift = service.openShift(new BarShiftOpenRequest(
                "open-bal", "bar_1", "cashier_bal", null, 10_000L, 500L, null));
        BarBillRecord cashBill = billForOrder("bal-cash", "staff_sales");
        service.settleBill(cashBill.id(), new BarBillSettleRequest(
                "s-bal", null, List.of(), "pay_bal", cashBill.payableAmountCent(),
                "cash", "cashier_bal", null));

        BarShiftRecord closed = service.closeShift(shift.id(), new BarShiftCloseRequest(
                "close-bal", 10_000L + cashBill.payableAmountCent(), null, null));

        assertThat(closed.cashDiffCent()).isZero();
        assertThat(closed.needsReview()).isFalse();
    }

    @Test
    void closeShiftRejectsHandoverToOutgoingCashierSelf() {
        // F-OPS-06：现金钱箱交接必须移交他人；交接给本班收银员自己 = 伪交接规避对账责任，拒绝。
        BarShiftRecord shift = service.openShift(new BarShiftOpenRequest(
                "open-self", "bar_1", "cashier_self", null, 0L, 0L, null));
        assertThatThrownBy(() -> service.closeShift(shift.id(), new BarShiftCloseRequest(
                "close-self", 0L, "cashier_self", null)))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("BAR_SHIFT_HANDOVER_TO_SELF"));
    }

    @Test
    void closeShiftAggregatesRefundWithinWindow() {
        BarShiftRecord shift = service.openShift(new BarShiftOpenRequest(
                "open-refund", "bar_1", "cashier_refund", null, 0L, 0L, null));
        BarBillRecord bill = billForOrder("refund-bill", "staff_sales");
        service.settleBill(bill.id(), new BarBillSettleRequest(
                "s-refund", null, List.of(), "pay_refund", bill.payableAmountCent(),
                "cash", "cashier_refund", null));
        service.applyBillAdjustment(bill.id(), new BarBillAdjustmentRequest(
                "refund-1", null, List.of(), "refund", 5_000L, "guest complaint", null));

        BarShiftRecord closed = service.closeShift(shift.id(), new BarShiftCloseRequest(
                "close-refund", bill.payableAmountCent(), null, null));

        assertThat(closed.systemRefundCent()).isEqualTo(5_000L);
    }

    @Test
    void openShiftRejectsDuplicateOpenShiftForSameCashierAndBar() {
        service.openShift(new BarShiftOpenRequest("open-dup-1", "bar_1", "cashier_dup", null, 0L, 0L, null));
        assertThatThrownBy(() -> service.openShift(
                new BarShiftOpenRequest("open-dup-2", "bar_1", "cashier_dup", null, 0L, 0L, null)))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_SHIFT_ALREADY_OPEN");
    }

    @Test
    void closeShiftRejectsAlreadyClosedShift() {
        BarShiftRecord shift = service.openShift(new BarShiftOpenRequest(
                "open-closed", "bar_1", "cashier_closed", null, 0L, 0L, null));
        service.closeShift(shift.id(), new BarShiftCloseRequest("close-once", 0L, null, null));
        assertThatThrownBy(() -> service.closeShift(shift.id(),
                new BarShiftCloseRequest("close-twice", 0L, null, null)))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_SHIFT_NOT_OPEN");
    }

    @Test
    void closeShiftRejectsCrossMerchantAccessWithForbiddenError() {
        TestSecurityActors.asMerchantUser("merchant_a");
        BarShiftRecord shift = barRepository.saveShift(new BarShiftRecord(
                "bar_shift_other", null, "merchant_b", "bar_1", "cashier_x", "open",
                Instant.now(), null, 0L, null, 0L, 0L, 0L, 0L, 0L, null, 0L, false,
                null, null, "opener", null, Instant.now(), Instant.now()));
        assertThatThrownBy(() -> service.closeShift(shift.id(),
                new BarShiftCloseRequest("close-x", 0L, null, null)))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    @Test
    void listShiftsReturnsOnlyCurrentMerchantRecordsWhenScopedToMerchant() {
        barRepository.saveShift(new BarShiftRecord(
                "bar_shift_a", null, "merchant_a", "bar_1", "c1", "open",
                Instant.now(), null, 0L, null, 0L, 0L, 0L, 0L, 0L, null, 0L, false,
                null, null, "opener", null, Instant.now(), Instant.now()));
        barRepository.saveShift(new BarShiftRecord(
                "bar_shift_b", null, "merchant_b", "bar_1", "c2", "open",
                Instant.now(), null, 0L, null, 0L, 0L, 0L, 0L, 0L, null, 0L, false,
                null, null, "opener", null, Instant.now(), Instant.now()));

        TestSecurityActors.asMerchantUser("merchant_a");
        assertThat(service.listShifts().items())
                .extracting(BarShiftRecord::id)
                .containsExactly("bar_shift_a");
    }

    // ── 出品票据双驱动（业务HTML 983 行口径）──

    @Test
    void barCounterTableOrderSkipsProductionTicketWhileCardSeatKeepsIt() {
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-dualdrive", "create product", List.of(), "bar_1", "Whisky dualdrive", "spirit",
                12800L, null, null, "active", null, null, false, null, null, null, null, null, null, null));
        // 吧台位（bar_counter）=吧台直点：即点即取，不建出品票据；送达/退菜路径对无票已容错。
        BarTableRecord counter = service.createTable(new BarTableRequest(
                "table-dualdrive-counter", "create table", List.of(), "bar_1", "吧台1号位", "bar_counter",
                0L, 2, "active"));
        BarOrderRecord counterOrder = service.createOrder(new BarOrderRequest(
                "order-dualdrive-counter", "create order", List.of(), "bar_1", counter.id(), null,
                List.of(Map.of("product_id", product.id(), "quantity", 1)), List.of(), "offline"));
        assertThat(barRepository.findProductionTicketByOrderId(counterOrder.id())).isEmpty();
        // 卡座=完整流转：照旧建票（既有行为不回退）。
        BarTableRecord seat = createTable("dualdrive-seat", "bar_1", "active", 0L);
        BarOrderRecord seatOrder = service.createOrder(new BarOrderRequest(
                "order-dualdrive-seat", "create order", List.of(), "bar_1", seat.id(), null,
                List.of(Map.of("product_id", product.id(), "quantity", 1)), List.of(), "offline"));
        assertThat(barRepository.findProductionTicketByOrderId(seatOrder.id())).isPresent();
    }

    @Test
    void cateringMerchantOrderSkipsProductionTicket() {
        // 独立餐饮（merchant_type=catering，后台商户词表/集成域 CHECK 的规范值）：
        // 出品走第三方餐饮系统，平台只拿回流，不建出品票据。用真 record 防「mock 词汇与实现互证」假绿。
        java.time.Instant mNow = java.time.Instant.now();
        com.complex.platform.governance.MerchantRecord cateringMerchant =
                new com.complex.platform.governance.MerchantRecord(
                        "merchant_food", "餐饮甲", "catering", null, null, null, null, null, null, null,
                        null, null, null, null, null, null, "active", mNow, mNow);
        barOrderService.merchantDirectory = merchantId -> java.util.Optional.of(cateringMerchant);
        try {
            TestSecurityActors.asMerchantUser("merchant_food");
            BarProductRecord product = service.createProduct(new BarProductRequest(
                    // I-2 词表校验后品类须用规范值：独立餐饮判定看商户业态(merchantDirectory)非商品品类，snack 等价。
                    "product-fooddrive", "create product", List.of(), "bar_1", "小吃 fooddrive", "snack",
                    3800L, null, null, "active", null, null, false, null, null, null, null, null, null, null));
            BarTableRecord table = service.createTable(new BarTableRequest(
                    "table-fooddrive", "create table", List.of(), "bar_1", "餐位1", "card_seat",
                    0L, 2, "active"));
            BarOrderRecord order = service.createOrder(new BarOrderRequest(
                    "order-fooddrive", "create order", List.of(), "bar_1", table.id(), null,
                    List.of(Map.of("product_id", product.id(), "quantity", 1)), List.of(), "offline"));
            assertThat(barRepository.findProductionTicketByOrderId(order.id())).isEmpty();
        } finally {
            barOrderService.merchantDirectory = null;
            TestSecurityActors.asPlatformAdmin();
        }
    }

    @Test
    void listOrdersAttachesBillSummaryPerOrderSoFrontendLocatesBillAndSplitsTruthfully() {
        BarBillRecord bill = billForOrder("list-view", "staff_sales");
        PageResponse<BarOrderView> page = service.listOrders();
        BarOrderView view = page.items().stream()
                .filter(candidate -> candidate.id().equals(bill.orderId()))
                .findFirst()
                .orElseThrow();
        assertThat(view.bills()).extracting(BarBillSummary::id).contains(bill.id());
        assertThat(view.bills().get(0).status()).isEqualTo(bill.paymentStatus());
        assertThat(view.salesStaffId()).isEqualTo("staff_sales");
    }

    /** G/V349 员工档案桩：只有「销售岗」标识对提成有意义，其余字段取默认。 */
    private static com.complex.platform.access.staff.StaffProfileRecord staffProfile(String userId, boolean salesPosition) {
        java.time.Instant now = java.time.Instant.now();
        return new com.complex.platform.access.staff.StaffProfileRecord(
                "sp_" + userId, "project_1", null, userId, userId, null, null, null,
                "approved", "usr_admin", now, null, "active", now, now,
                null, null, "none", null, null, null, salesPosition);
    }

    private BarBillRecord billForOrder(String key, String salesStaffId) {
        BarReservationRecord reservation = createReservation("reservation-" + key, "bar_1", 2);
        BarProductRecord product = service.createProduct(new BarProductRequest(
                "product-" + key,
                "create product",
                List.of(),
                "bar_1",
                "Whisky " + key,
                "spirit",
                128800L,
                null,
                null,
                "active",
                null,
                null,
                false
        , null, null, null, null, null, null, null));
        BarOrderRecord order = service.createOrder(new BarOrderRequest(
                "order-" + key,
                "create order",
                List.of(),
                "bar_1",
                reservation.tableId(),
                reservation.id(),
                List.of(Map.of("product_id", product.id(), "quantity", 1)),
                List.of(),
                "offline"
        ));
        if (salesStaffId != null) {
            order = withSales(order, salesStaffId);
        }
        return service.createOrderBill(order.id(), new BarOrderBillCreateRequest(
                "bill-" + key,
                "create bill",
                List.of(),
                "offline",
                false,
                0
        ));
    }

    /**
     * 第十八批 I 段 86-R1：订单销售取开台会话的销售归属（本类未注入服务责任，下单后订单销售恒空）；要带销售归属的用例按落库形状直接写入订单记录，
     * 不为单测在生产代码加回落。
     */
    private BarOrderRecord withSales(BarOrderRecord order, String salesStaffId) {
        return barRepository.saveOrder(new BarOrderRecord(order.id(), order.barId(), order.merchantId(), order.tableId(),
                order.reservationId(), order.customerId(), order.orderSource(), order.status(), order.totalAmountCent(), order.currency(),
                order.createdTerminal(), order.deviceId(), salesStaffId, order.createdAt(), order.updatedAt(), order.orderType(), order.paidAt()));
    }

    /** ROOT-03 clients-R6：门口只认成员本人动态入场码（客人端签发，窗口内有效）。 */
    private String entryCode(String memberId, String reservationId) {
        return invitationSigner.entryCode(memberId, reservationId, Instant.now().plusSeconds(60));
    }

    private VerificationRequest verification(String idempotencyKey, String guestIdentifier, String barId) {
        return new VerificationRequest(
                idempotencyKey,
                "manual",
                guestIdentifier,
                "device_1",
                barId,
                Map.of("bar_id", barId)
        );
    }

    private static final class InMemoryBarRepository implements BarRepository {
        @Override public void setReservationArchiveReason(String id, String reason) { }
        java.util.Set<String> activeReservedTableIds = java.util.Set.of();

        @Override
        public java.util.Set<String> listActiveReservedTableIds() {
            return activeReservedTableIds;
        }

        private final Map<String, BarTableRecord> tables = new LinkedHashMap<>();
        private final Map<String, BarReservationRecord> reservations = new LinkedHashMap<>();
        private final Map<String, BarDepositRuleRecord> depositRules = new LinkedHashMap<>();
        private final Map<String, BarProductRecord> products = new LinkedHashMap<>();
        private final Map<String, BarOrderRecord> orders = new LinkedHashMap<>();
        private final Map<String, BarOrderItemRecord> orderItems = new LinkedHashMap<>();
        private final List<BarOrderItemVoidRecord> orderItemVoids = new ArrayList<>();
        private final Map<String, BarBillRecord> bills = new LinkedHashMap<>();
        private final List<BarBillSettlementRecord> billSettlements = new ArrayList<>();
        private final Map<String, BarShiftRecord> shifts = new LinkedHashMap<>();
        private final List<BarBillAdjustmentRecord> billAdjustments = new ArrayList<>();
        private final List<BarBillOperationRecord> billOperations = new ArrayList<>();
        private final Map<String, BarTableSessionRecord> tableSessions = new LinkedHashMap<>();
        private final List<BarTableSessionAdjustmentRecord> tableSessionAdjustments = new ArrayList<>();
        private final List<BarTableGuestCheckinRecord> guestCheckins = new ArrayList<>();
        private final List<String> lockedReservations = new ArrayList<>();
        private final List<BarProductionTicketRecord> productionTickets = new ArrayList<>();
        private final List<BarProductionTicketEventRecord> productionTicketEvents = new ArrayList<>();
        private final List<BarCommissionCalculationRecord> commissions = new ArrayList<>();
        private final Map<String, StockBalanceSnapshot> stockBalances = new LinkedHashMap<>();
        private final List<StockMovementSnapshot> stockMovements = new ArrayList<>();
        /** 第十八批 C 段第 17 条：所挂吧台配有出酒量采集设备的库存行（物品:库位）。 */
        private final java.util.Set<String> pourDeviceStock = new java.util.HashSet<>();

        @Override
        public boolean pourDeviceEquipped(String itemId, String locationId) {
            return pourDeviceStock.contains(itemId + ":" + locationId);
        }

        /** 第十八批 I 段第 76 条：物料规格容量（毫升）与库存行已开瓶余量（物品:库位），按 Jdbc 语义扣加。 */
        final Map<String, Integer> specVolumes = new LinkedHashMap<>();
        final Map<String, Integer> openedVolumes = new LinkedHashMap<>();

        @Override
        public Optional<Integer> findInventoryItemSpecVolumeMl(String itemId) {
            return Optional.ofNullable(specVolumes.get(itemId));
        }

        @Override
        public Optional<Integer> decreaseStockByVolume(String itemId, String locationId, int volumeMl) {
            String key = itemId + ":" + locationId;
            int opened = openedVolumes.getOrDefault(key, 0);
            Integer spec = specVolumes.get(itemId);
            if (spec == null) {
                return Optional.empty();
            }
            int bottles = BarPourLoss.bottlesToOpen(volumeMl, opened, spec);
            if (!atomicDecreaseStock(itemId, locationId, bottles)) {
                return Optional.empty();
            }
            openedVolumes.put(key, opened + bottles * spec - volumeMl);
            return Optional.of(bottles);
        }

        @Override
        public boolean increaseOpenedVolume(String itemId, String locationId, int volumeMl) {
            String key = itemId + ":" + locationId;
            if (!stockBalances.containsKey(key)) {
                return false;
            }
            openedVolumes.merge(key, volumeMl, Integer::sum);
            return true;
        }

        @Override
        public BarTableRecord saveTable(BarTableRecord table) {
            tables.put(table.id(), table);
            return table;
        }

        @Override
        public List<BarTableRecord> listTables(Optional<String> merchantScope) {
            List<BarTableRecord> all = new ArrayList<>(tables.values());
            if (merchantScope.isEmpty()) {
                return all;
            }
            return all.stream().filter(r -> merchantScope.get().equals(r.merchantId())).toList();
        }

        @Override
        public Optional<BarTableRecord> findTableById(String id) {
            return Optional.ofNullable(tables.get(id));
        }

        @Override
        public BarReservationRecord saveReservation(BarReservationRecord reservation) {
            reservations.put(reservation.id(), reservation);
            return reservation;
        }

        @Override
        public List<BarReservationRecord> listReservations(Optional<String> merchantScope) {
            List<BarReservationRecord> all = new ArrayList<>(reservations.values());
            if (merchantScope.isEmpty()) {
                return all;
            }
            return all.stream().filter(r -> merchantScope.get().equals(r.merchantId())).toList();
        }

        @Override
        public Optional<BarReservationRecord> findReservationById(String id) {
            return Optional.ofNullable(reservations.get(id));
        }

        @Override
        public Optional<BarReservationRecord> findReservationByIdForUpdate(String id) {
            lockedReservations.add(id);
            return findReservationById(id);
        }

        @Override
        public BarDepositRuleRecord saveDepositRule(BarDepositRuleRecord rule) {
            depositRules.put(rule.id(), rule);
            return rule;
        }

        @Override
        public List<BarDepositRuleRecord> listDepositRules(String barId) {
            return depositRules.values().stream()
                    .filter(r -> r.enabled() && barId.equals(r.barId()))
                    .sorted((a, b) -> b.scopeType().compareTo(a.scopeType()))
                    .toList();
        }

        @Override
        public int countGuestCheckins(String reservationId) {
            return (int) guestCheckins.stream()
                    .filter(checkin -> reservationId.equals(checkin.reservationId()))
                    .count();
        }

        @Override
        public BarTableGuestCheckinRecord saveGuestCheckin(BarTableGuestCheckinRecord checkin) {
            guestCheckins.add(checkin);
            return checkin;
        }

        private final Map<String, BarReservationInvitationMemberRecord> invitationMembers = new LinkedHashMap<>();

        @Override
        public Optional<BarReservationRecord> findReservationByInvitationToken(String token) {
            return reservations.values().stream()
                    .filter(reservation -> token != null && token.equals(reservation.invitationToken()))
                    .findFirst();
        }

        @Override
        public BarReservationInvitationMemberRecord saveInvitationMember(BarReservationInvitationMemberRecord member) {
            invitationMembers.put(member.reservationId() + "|" + member.memberIdentifier(), member);
            return member;
        }

        @Override
        public List<BarReservationInvitationMemberRecord> listInvitationMembers(String reservationId) {
            return invitationMembers.values().stream()
                    .filter(member -> reservationId.equals(member.reservationId()))
                    .sorted((a, b) -> Boolean.compare(b.isHolder(), a.isHolder()))
                    .toList();
        }

        @Override
        public BarProductRecord saveProduct(BarProductRecord product) {
            products.put(product.id(), product);
            return product;
        }

        @Override
        public List<BarProductRecord> listProducts(Optional<String> merchantScope) {
            List<BarProductRecord> all = new ArrayList<>(products.values());
            if (merchantScope.isEmpty()) {
                return all;
            }
            return all.stream().filter(r -> merchantScope.get().equals(r.merchantId())).toList();
        }

        @Override
        public Optional<BarProductRecord> findProductById(String id) {
            return Optional.ofNullable(products.get(id));
        }

        @Override
        public BarOrderRecord saveOrder(BarOrderRecord order) {
            // 第十批 TK-24：订单首次保存时按台位上的 open 会话锁定归属（JDBC 由 recordOrder 写 bar_order_service_sources）。
            if (!orders.containsKey(order.id())) {
                tableSessions.values().stream()
                        .filter(s -> ("open".equals(s.status()) || "merged".equals(s.status())) && s.tableId().equals(order.tableId()))
                        .findFirst().ifPresent(s -> orderSessions.put(order.id(), s.id()));
            }
            orders.put(order.id(), order);
            return order;
        }

        private final Map<String, String> orderSessions = new LinkedHashMap<>();

        @Override
        public List<String> familySessionIds(String sessionId, boolean includeTransferred) {
            List<String> family = new ArrayList<>(List.of(sessionId));
            for (int i = 0; i < family.size(); i++) {
                String parent = family.get(i);
                tableSessions.values().stream()
                        .filter(s -> parent.equals(s.sourceSessionId()) && ("merged".equals(s.status())
                                || (includeTransferred && "merged_transferred".equals(s.status()))))
                        .map(BarTableSessionRecord::id).filter(id -> !family.contains(id)).forEach(family::add);
            }
            return family;
        }

        @Override
        public List<BarOrderRecord> listOrdersWithoutSession(List<String> tableIds, Instant since) {
            return orders.values().stream()
                    .filter(o -> tableIds.contains(o.tableId()) && !o.createdAt().isBefore(since)
                            && !"cancelled".equals(o.status()) && !orderSessions.containsKey(o.id()))
                    .toList();
        }

        @Override
        public void saveOrderServiceSource(String orderId, String sessionId) {
            orderSessions.put(orderId, sessionId);
        }

        @Override
        public List<String> listOrderIdsBySessionIds(List<String> sessionIds) {
            return orderSessions.entrySet().stream()
                    .filter(e -> sessionIds.contains(e.getValue())).map(Map.Entry::getKey).toList();
        }

        @Override
        public Optional<String> findSessionIdByOrderId(String orderId) {
            return Optional.ofNullable(orderSessions.get(orderId));
        }

        @Override
        public List<BarOrderRecord> listOrders(Optional<String> merchantScope) {
            List<BarOrderRecord> all = new ArrayList<>(orders.values());
            if (merchantScope.isEmpty()) {
                return all;
            }
            return all.stream().filter(r -> merchantScope.get().equals(r.merchantId())).toList();
        }

        @Override
        public Optional<BarOrderRecord> findOrderById(String id) {
            return Optional.ofNullable(orders.get(id));
        }

        @Override
        public BarOrderItemRecord saveOrderItem(BarOrderItemRecord item) {
            orderItems.put(item.id(), item);
            return item;
        }

        @Override
        public List<BarOrderItemRecord> listOrderItems(String orderId) {
            return orderItems.values().stream()
                    .filter(item -> orderId.equals(item.orderId()))
                    .toList();
        }

        @Override
        public Optional<BarOrderItemRecord> findOrderItemById(String id) {
            return Optional.ofNullable(orderItems.get(id));
        }

        @Override
        public BarOrderItemVoidRecord saveOrderItemVoid(BarOrderItemVoidRecord voidRecord) {
            orderItemVoids.removeIf(existing -> existing.id().equals(voidRecord.id()));
            orderItemVoids.add(voidRecord);
            return voidRecord;
        }

        @Override
        public List<BarOrderItemVoidRecord> listOrderItemVoids(String orderId) {
            return orderItemVoids.stream().filter(v -> orderId.equals(v.orderId())).toList();
        }

        @Override
        public java.util.Optional<BarOrderItemVoidRecord> findOrderItemVoidById(String id) {
            return orderItemVoids.stream().filter(v -> v.id().equals(id)).findFirst();
        }

        @Override
        public BarProductionTicketRecord saveProductionTicket(BarProductionTicketRecord ticket) {
            productionTickets.removeIf(existing -> existing.id().equals(ticket.id()));
            productionTickets.add(ticket);
            return ticket;
        }

        @Override
        public List<BarProductionTicketRecord> listProductionTickets(Optional<String> merchantScope) {
            List<BarProductionTicketRecord> all = List.copyOf(productionTickets);
            if (merchantScope.isEmpty()) {
                return all;
            }
            return all.stream().filter(r -> merchantScope.get().equals(r.merchantId())).toList();
        }

        @Override
        public Optional<BarProductionTicketRecord> findProductionTicketById(String id) {
            return productionTickets.stream()
                    .filter(ticket -> id.equals(ticket.id()))
                    .findFirst();
        }

        @Override
        public Optional<BarProductionTicketRecord> findProductionTicketByOrderId(String orderId) {
            return productionTickets.stream()
                    .filter(ticket -> orderId.equals(ticket.orderId()))
                    .findFirst();
        }

        @Override
        public BarProductionTicketEventRecord saveProductionTicketEvent(BarProductionTicketEventRecord event) {
            productionTicketEvents.add(event);
            return event;
        }

        @Override
        public List<BarProductionTicketEventRecord> listProductionTicketEvents(String ticketId) {
            return productionTicketEvents.stream()
                    .filter(event -> ticketId.equals(event.ticketId()))
                    .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
                    .toList();
        }

        @Override
        public BarBillRecord saveBill(BarBillRecord bill) {
            bills.put(bill.id(), bill);
            return bill;
        }

        @Override
        public Optional<BarBillRecord> findBillById(String id) {
            return Optional.ofNullable(bills.get(id));
        }

        @Override
        public List<BarBillRecord> listBillsByOrderIds(List<String> orderIds) {
            return bills.values().stream()
                    .filter(candidate -> orderIds.contains(candidate.orderId()))
                    .toList();
        }

        @Override
        public Optional<BarBillRecord> findBillByIdForUpdate(String id) {
            return Optional.ofNullable(bills.get(id));
        }

        @Override
        public Optional<BarCommissionCalculationRecord> findCommissionByBillId(String billId) {
            return commissions.stream().filter(c -> billId.equals(c.billId())).findFirst();
        }

        @Override
        public List<BarCommissionCalculationRecord> listCommissionsByBillIds(List<String> billIds) {
            return commissions.stream().filter(c -> billIds.contains(c.billId())).toList();
        }

        @Override
        public BarBillAdjustmentRecord saveBillAdjustment(BarBillAdjustmentRecord adjustment) {
            billAdjustments.removeIf(existing -> existing.id().equals(adjustment.id()));
            billAdjustments.add(adjustment);
            return adjustment;
        }

        @Override
        public List<BarBillAdjustmentRecord> listBillAdjustments(String billId) {
            return billAdjustments.stream()
                    .filter(adjustment -> billId.equals(adjustment.billId()))
                    .toList();
        }

        @Override
        public BarBillOperationRecord saveBillOperation(BarBillOperationRecord operation) {
            billOperations.removeIf(existing ->
                    existing.primaryBillId().equals(operation.primaryBillId())
                            && existing.idempotencyKey().equals(operation.idempotencyKey()));
            billOperations.add(operation);
            return operation;
        }

        @Override
        public List<BarBillOperationRecord> listBillOperations(String billId) {
            return billOperations.stream()
                    .filter(operation -> billId.equals(operation.primaryBillId())
                            || billId.equals(operation.secondaryBillId())
                            || billId.equals(operation.generatedBillId()))
                    .toList();
        }

        @Override
        public BarBillSettlementRecord saveBillSettlement(BarBillSettlementRecord settlement) {
            billSettlements.removeIf(existing -> existing.id().equals(settlement.id()));
            billSettlements.add(settlement);
            return settlement;
        }

        @Override
        public List<BarBillSettlementRecord> listBillSettlements(String billId) {
            return billSettlements.stream().filter(s -> billId.equals(s.billId())).toList();
        }

        @Override
        public int markBillSettlementsReversed(String billId) {
            return (int) billSettlements.stream().filter(s -> billId.equals(s.billId())).count();
        }

        @Override
        public BarShiftRecord saveShift(BarShiftRecord shift) {
            shifts.put(shift.id(), shift);
            return shift;
        }

        @Override
        public Optional<BarShiftRecord> findShiftById(String id) {
            return Optional.ofNullable(shifts.get(id));
        }

        @Override
        public Optional<BarShiftRecord> findOpenShift(String barId, String cashierUserId) {
            return shifts.values().stream()
                    .filter(s -> "open".equals(s.status())
                            && barId.equals(s.barId())
                            && cashierUserId.equals(s.cashierUserId()))
                    .findFirst();
        }

        @Override
        public List<BarShiftRecord> listShifts(Optional<String> merchantScope) {
            List<BarShiftRecord> all = new ArrayList<>(shifts.values());
            if (merchantScope.isEmpty()) {
                return all;
            }
            return all.stream().filter(r -> merchantScope.get().equals(r.merchantId())).toList();
        }

        @Override
        public BarShiftReconciliationSnapshot aggregateShiftReconciliation(
                String barId,
                String cashierUserId,
                Instant from,
                Instant to,
                Optional<String> merchantScope
        ) {
            long cash = 0L;
            long pos = 0L;
            long online = 0L;
            long tip = 0L;
            java.util.Set<String> tippedBills = new java.util.LinkedHashSet<>();
            for (BarBillSettlementRecord s : billSettlements) {
                BarBillRecord bill = bills.get(s.billId());
                if (bill == null || !barId.equals(bill.barId())) {
                    continue;
                }
                if (!cashierUserId.equals(s.cashierStaffId())) {
                    continue;
                }
                if (merchantScope.isPresent() && !merchantScope.get().equals(bill.merchantId())) {
                    continue;
                }
                Instant at = s.settledAt();
                if (at == null || at.isBefore(from) || !at.isBefore(to)) {
                    continue;
                }
                switch (s.settleChannel()) {
                    case "cash" -> cash += s.settlementAmountCent();
                    case "pos" -> pos += s.settlementAmountCent();
                    default -> online += s.settlementAmountCent();
                }
                if (bill.tipAmountCent() > 0) {
                    tippedBills.add(bill.id());
                }
            }
            for (String billId : tippedBills) {
                tip += bills.get(billId).tipAmountCent();
            }
            long refund = 0L;
            for (BarBillAdjustmentRecord a : billAdjustments) {
                boolean isRefund = "refund".equals(a.adjustmentType());
                boolean isRefundReversal = "refund_reversal".equals(a.adjustmentType());
                if ((!isRefund && !isRefundReversal) || a.refundedAt() == null) {
                    continue;
                }
                BarBillRecord bill = bills.get(a.billId());
                if (bill == null || !barId.equals(bill.barId())) {
                    continue;
                }
                if (merchantScope.isPresent() && !merchantScope.get().equals(bill.merchantId())) {
                    continue;
                }
                Instant at = a.refundedAt();
                if (at.isBefore(from) || !at.isBefore(to)) {
                    continue;
                }
                // 延期项④：refund 计入退款净额，refund_reversal（驳回冲销）抵减，net 归零。
                refund += isRefundReversal ? -a.amountCent() : a.amountCent();
            }
            return new BarShiftReconciliationSnapshot(cash, pos, online, refund, tip);
        }

        @Override
        public BarCommissionCalculationRecord saveCommissionCalculation(BarCommissionCalculationRecord calculation) {
            commissions.add(calculation);
            return calculation;
        }

        @Override
        public BarTableSessionRecord saveTableSession(BarTableSessionRecord session) {
            tableSessions.put(session.id(), session);
            return session;
        }

        @Override
        public boolean updateTableSessionIfVersionMatches(BarTableSessionRecord session, long expectedVersion) {
            BarTableSessionRecord current = tableSessions.get(session.id());
            if (current == null || current.sessionVersion() != expectedVersion) {
                return false;
            }
            tableSessions.put(session.id(), session);
            return true;
        }

        @Override
        public List<BarTableSessionRecord> listTableSessions(Optional<String> merchantScope) {
            List<BarTableSessionRecord> all = new ArrayList<>(tableSessions.values());
            if (merchantScope.isEmpty()) {
                return all;
            }
            return all.stream().filter(r -> merchantScope.get().equals(r.merchantId())).toList();
        }

        @Override
        public Optional<BarTableSessionRecord> findTableSessionById(String id) {
            return Optional.ofNullable(tableSessions.get(id));
        }

        @Override
        public BarTableSessionAdjustmentRecord saveTableSessionAdjustment(BarTableSessionAdjustmentRecord adjustment) {
            tableSessionAdjustments.add(adjustment);
            return adjustment;
        }

        @Override
        public List<BarTableSessionAdjustmentRecord> listTableSessionAdjustments(String sessionId) {
            return tableSessionAdjustments.stream()
                    .filter(adjustment -> sessionId.equals(adjustment.sessionId()))
                    .toList();
        }

        @Override
        public Optional<StockBalanceSnapshot> findStockBalance(String itemId, String locationId) {
            return Optional.ofNullable(stockBalances.get(itemId + ":" + locationId));
        }

        @Override
        public boolean atomicDecreaseStock(String itemId, String locationId, int quantity) {
            String key = itemId + ":" + locationId;
            StockBalanceSnapshot current = stockBalances.get(key);
            if (current == null || current.availableQuantity() < quantity) {
                return false;
            }
            stockBalances.put(key, new StockBalanceSnapshot(
                    current.id(),
                    current.itemId(),
                    current.locationId(),
                    current.quantity() - quantity,
                    current.reservedQuantity(),
                    current.availableQuantity() - quantity,
                    Instant.now()
            ));
            return true;
        }

        @Override
        public boolean atomicIncreaseStock(String itemId, String locationId, int quantity) {
            String key = itemId + ":" + locationId;
            StockBalanceSnapshot current = stockBalances.get(key);
            if (current == null) {
                return false;
            }
            stockBalances.put(key, new StockBalanceSnapshot(
                    current.id(),
                    current.itemId(),
                    current.locationId(),
                    current.quantity() + quantity,
                    current.reservedQuantity(),
                    current.availableQuantity() + quantity,
                    Instant.now()
            ));
            return true;
        }

        @Override
        public StockBalanceSnapshot saveStockBalance(StockBalanceSnapshot balance) {
            stockBalances.put(balance.itemId() + ":" + balance.locationId(), balance);
            return balance;
        }

        @Override
        public StockMovementSnapshot saveStockMovement(StockMovementSnapshot movement) {
            stockMovements.add(movement);
            return movement;
        }

        private final Map<String, BarGiftRedemptionRecord> giftRedemptions = new LinkedHashMap<>();

        @Override
        public Optional<BarTableRecord> findFirstTableByBar(String barId) {
            return tables.values().stream().filter(t -> barId.equals(t.barId())).findFirst();
        }

        // 处理清单 #8：内存版空间映射（spaceId -> merchantId；空串表示无商户/共享空间）。
        private final Map<String, String> spaceMerchants = new LinkedHashMap<>();
        private final Map<String, String> tableSpaceIds = new LinkedHashMap<>();

        void registerSpace(String spaceId, String merchantId) {
            spaceMerchants.put(spaceId, merchantId == null ? "" : merchantId);
        }

        String tableSpaceId(String tableId) {
            return tableSpaceIds.get(tableId);
        }

        @Override
        public int assignTableSpace(String tableId, String spaceId) {
            if (!tables.containsKey(tableId)) {
                return 0;
            }
            tableSpaceIds.put(tableId, spaceId);
            return 1;
        }

        @Override
        public Optional<String> findSpaceMerchant(String spaceId) {
            return Optional.ofNullable(spaceMerchants.get(spaceId));
        }

        @Override
        public List<BarTableSpaceBacklogRecord> listTablesWithoutSpace(Optional<String> merchantScope) {
            return tables.values().stream()
                    .filter(t -> tableSpaceIds.get(t.id()) == null)
                    .filter(t -> merchantScope.isEmpty() || merchantScope.get().equals(t.merchantId()))
                    .map(t -> new BarTableSpaceBacklogRecord(
                            t.id(), t.barId(), t.merchantId(), t.tableCode(), t.tableName(), t.status()))
                    .toList();
        }

        @Override
        public boolean insertGiftRedemptionIfAbsent(BarGiftRedemptionRecord redemption) {
            if (giftRedemptions.containsKey(redemption.verificationId())) {
                return false;
            }
            giftRedemptions.put(redemption.verificationId(), redemption);
            return true;
        }

        @Override
        public BarGiftRedemptionRecord saveGiftRedemption(BarGiftRedemptionRecord redemption) {
            giftRedemptions.put(redemption.verificationId(), redemption);
            return redemption;
        }

        @Override
        public Optional<BarGiftRedemptionRecord> findGiftRedemptionByVerification(String verificationId) {
            return Optional.ofNullable(giftRedemptions.get(verificationId));
        }

        @Override
        public List<BarGiftRedemptionRecord> listGiftRedemptions(Optional<String> merchantScope) {
            List<BarGiftRedemptionRecord> all = new ArrayList<>(giftRedemptions.values());
            if (merchantScope.isEmpty()) {
                return all;
            }
            return all.stream().filter(r -> merchantScope.get().equals(r.merchantId())).toList();
        }
    }

    private static final class CapturingBarReceivableLedgerService implements BarReceivableLedgerService {
        private final List<CapturedReceivableRequest> requests = new ArrayList<>();
        private boolean businessDayLocked = false;

        @Override
        public boolean isBusinessDayLocked(java.time.LocalDate businessDate) {
            return businessDayLocked;
        }

        @Override
        public String createReceivableForOnAccountBill(
                BarBillRecord bill,
                BarOrderRecord order,
                String idempotencyKey,
                String operatorNote,
                List<String> evidenceAttachmentIds
        ) {
            ledgerPostings.add("createReceivableForOnAccountBill");
            requests.add(new CapturedReceivableRequest(
                    bill.id(),
                    order.id(),
                    bill.payableAmountCent(),
                    idempotencyKey,
                    operatorNote,
                    List.copyOf(evidenceAttachmentIds)
            ));
            return "arap_for_" + bill.id();
        }

        /** 每一次向 finance 台账过账的调用名（CG-07：提成计提须一条都不过账）。 */
        private final List<String> ledgerPostings = new ArrayList<>();

        private final List<CapturedRefundPayable> refundPayables = new ArrayList<>();
        /** 第十批 TK-24-R3：达标退回定金的订台 id（按过账顺序）。 */
        private final List<String> depositRefunds = new ArrayList<>();

        @Override
        public String createDepositRefundPayable(BarReservationRecord reservation) {
            ledgerPostings.add("createDepositRefundPayable");
            depositRefunds.add(reservation.id());
            return "arap_deposit_refund_" + reservation.id();
        }

        @Override
        public boolean hasDepositRefundPayable(String reservationId) {
            return depositRefunds.contains(reservationId);
        }

        @Override
        public String createRefundPayable(
                BarBillRecord bill,
                BarOrderRecord order,
                String adjustmentId,
                long refundAmountCent,
                String idempotencyKey,
                String reason,
                List<String> evidenceAttachmentIds
        ) {
            ledgerPostings.add("createRefundPayable");
            refundPayables.add(new CapturedRefundPayable(
                    bill.id(),
                    order.id(),
                    adjustmentId,
                    refundAmountCent,
                    idempotencyKey,
                    reason,
                    List.copyOf(evidenceAttachmentIds)
            ));
            return "arap_refund_" + adjustmentId;
        }

        private final List<CapturedRefundPayable> refundReversals = new ArrayList<>();

        @Override
        public String reverseRefundPayable(
                BarBillRecord bill,
                BarOrderRecord order,
                String reversalAdjustmentId,
                long refundAmountCent,
                String idempotencyKey,
                String reason,
                List<String> evidenceAttachmentIds
        ) {
            ledgerPostings.add("reverseRefundPayable");
            refundReversals.add(new CapturedRefundPayable(
                    bill.id(),
                    order.id(),
                    reversalAdjustmentId,
                    refundAmountCent,
                    idempotencyKey,
                    reason,
                    List.copyOf(evidenceAttachmentIds)
            ));
            return "arap_refund_reversal_" + reversalAdjustmentId;
        }

        @Override
        public String createPayableForGiftSettlement(String verificationId, String merchantId, long internalPriceCent) {
            ledgerPostings.add("createPayableForGiftSettlement");
            return "arap_gift_settle_" + verificationId;
        }

        @Override
        public String reverseGiftSettlementPayable(String verificationId, String merchantId, long internalPriceCent) {
            ledgerPostings.add("reverseGiftSettlementPayable");
            return "arap_gift_settle_reverse_" + verificationId;
        }
    }

    private record CapturedRefundPayable(
            String billId,
            String orderId,
            String adjustmentId,
            long refundAmountCent,
            String idempotencyKey,
            String reason,
            List<String> evidenceAttachmentIds
    ) {
    }

    private record CapturedReceivableRequest(
            String billId,
            String orderId,
            long amountCent,
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds
    ) {
    }

    /** 第十八批第 24 条 24-R3（PGM:529「申请人与直接受益人不得审批与自己相关的申请」）：退品审批由申请人以外的平台账号办理，办完恢复原身份。 */
    private BarOrderItemVoidRecord decideAsReviewer(String voidId, BarOrderItemVoidDecisionRequest request) {
        var previous = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        TestSecurityActors.asPlatformAdmin(VOID_REVIEWER);
        try {
            return service.decideOrderItemVoid(voidId, request);
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }
}
