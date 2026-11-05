package com.complex.platform.ticketing;

// 文件版本：V0.08

// 文件版本：V0.03

import com.complex.platform.support.InMemoryAuditEventRepository;
import com.complex.platform.support.InMemoryIdempotencyRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.complex.platform.common.api.ApiException;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.support.TestSecurityActors;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.finance.RefundAuthorityLimitRecord;
import com.complex.platform.finance.RefundAuthorizationRepository;
import com.complex.platform.finance.RefundAuthorizationService;
import com.complex.platform.finance.RefundAutoRuleRecord;
import com.complex.platform.ticketing.dto.ConfirmTicketOrderPaymentRequest;
import com.complex.platform.ticketing.dto.EntitlementTemplateRequest;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.TicketIssueRequest;
import com.complex.platform.ticketing.dto.TicketOrderRequest;
import com.complex.platform.ticketing.dto.TicketPriceRuleRequest;
import com.complex.platform.ticketing.dto.TicketRefundRequest;
import com.complex.platform.ticketing.dto.TicketTypeEntitlementRequest;
import com.complex.platform.ticketing.dto.QrRefreshConfigRequest;
import com.complex.platform.ticketing.dto.TicketTypeRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class TicketingServiceTests {

    private InMemoryTicketingRepository ticketingRepository;
    private InMemoryAuditEventRepository auditEventRepository;
    private TicketingService service;
    private TicketOrderService orderService;
    // AUD-076 酒吧目录（业态 bar 商户）测试双子：注入 catalog/order 服务供单店票校验/解析。
    private java.util.Map<String, com.complex.platform.governance.MerchantRecord> bars;
    // P1 清单#19：捕获 ticketing 域发布的领域事件（如退款/结算冲正联动 VerificationReversedEvent），供断言。
    private final List<Object> publishedEvents = new ArrayList<>();
    // P0-D/CFG-01（V340）：单独收集 TicketOrderService 发布的事件（积分入账事件断言用），
    // 与 publishedEvents 分开以免影响既有核销/退款事件用例的计数。
    private final List<Object> orderEvents = new ArrayList<>();
    // 积分累计率配置端口测试双子：默认 100 分/分 = 出厂种子 = 改造前写死的「每元 1 分」。
    private StubPointsAccrualRateConfigPort pointsRateStub;

    // V369：票种目录服务（凑整误差闸测试要往它上挂允许误差端口测试双子）。
    private TicketCatalogService catalogService;
    // V369 凑整允许误差配置端口测试双子。默认不挂到服务上——既有用例不受误差闸影响；
    //   要测误差闸的用例自己 catalogService.comboPriceToleranceConfigPort = toleranceStub。
    private StubComboPriceToleranceConfigPort toleranceStub;

    /** V369 凑整允许误差配置端口测试双子（出厂种子 500 分 = 5 元）。 */
    private static final class StubComboPriceToleranceConfigPort implements ComboPriceToleranceConfigPort {
        private long toleranceCent = 500L;

        @Override
        public long findRoundingToleranceCent(String projectId) {
            return toleranceCent;
        }
    }

    /** P0-D/CFG-01（V340）积分累计率配置端口测试双子。 */
    private static final class StubPointsAccrualRateConfigPort implements PointsAccrualRateConfigPort {
        private long amountCentPerPoint = 100L;
        private String lastMerchantId;

        @Override
        public long findAmountCentPerPoint(String projectId, String merchantId) {
            lastMerchantId = merchantId;
            return amountCentPerPoint;
        }
    }

    @BeforeEach
    void setUp() {
        publishedEvents.clear();
        orderEvents.clear();
        ticketingRepository = new InMemoryTicketingRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        AuditEventService auditEventService = new AuditEventService(auditEventRepository);
        IdempotencyService idempotencyService = new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper());
        AuditedOperationExecutor operations = new AuditedOperationExecutor(idempotencyService, auditEventService);
        TicketPricingService pricingService = new TicketPricingService(ticketingRepository);
        TicketVerificationService verificationService = new TicketVerificationService(ticketingRepository, publishedEvents::add, null, null, null, null);
        AuthorizationService authorizationService = new AuthorizationService(null);
        bars = new java.util.HashMap<>();
        BarMerchantDirectory barDirectory = id -> java.util.Optional.ofNullable(bars.get(id));
        service = new TicketingService(
                ticketingRepository,
                catalogService = new TicketCatalogService(ticketingRepository, barDirectory),
                pricingService,
                orderService = new TicketOrderService(ticketingRepository, pricingService, authorizationService, orderEvents::add, barDirectory),
                new TicketRefundService(ticketingRepository, event -> { }, null, null),
                verificationService,
                new TicketCredentialService(verificationService, auditEventService),
                new TicketingOperationExecutor(operations),
                authorizationService,
                null
        );
        // P0-D/CFG-01（V340）：积分累计率改后台可配。注入「出厂种子等值」测试双子（100 分 = 每 1 元 1 分），
        // 令既有出票/支付用例行为一字不变 = 本次改造【零行为改变】的单测侧证据。
        pointsRateStub = new StubPointsAccrualRateConfigPort();
        orderService.pointsAccrualRateConfigPort = pointsRateStub;
        toleranceStub = new StubComboPriceToleranceConfigPort();
        // 🔴 用例隔离(2026-07-28 修):本类此前【没有】统一的登录主体准备,靠「先跑的用例里 TestSecurityActors.asMerchantUser(...)
        // 未清理、后跑的用例捡漏」跑通——JUnit 方法顺序一变(例如本类新增/删除任一 @Test)就整片 "Api authentication is required"。
        // 现改为每例前置平台管理员、每例后清理:各用例要特定主体的自己覆盖(既有用例本就显式调 asMerchantUser),
        // 不再依赖跨用例泄漏。此举只补齐前置、不改任何断言。
        TestSecurityActors.asPlatformAdmin();
    }

    @org.junit.jupiter.api.AfterEach
    void clearSecurityActor() {
        TestSecurityActors.clear();
    }

    @Test
    void issuedUnusedTicketDoesNotCreatePurchaserPoints() {
        TicketTypeRecord ticketType = createTicketType("unused-no-reward");
        EntitlementTemplateRecord template = createTemplate("unused-no-reward", 1, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "unused-bind", "bind", List.of(), template.id(), 1, 2, 9900L, "default", 1, 9900L));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "unused-order", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-20"), "admin", "13800138000", "customer_unused_buyer"));
        service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "unused-issue", "issue", "paid offline", List.of()));

        assertThat(orderEvents).noneMatch(PointsEarnRequestedEvent.class::isInstance);
        assertThat(orderEvents).noneMatch(PointsConfirmRequestedEvent.class::isInstance);
    }

    // FS-013（BSM L755；裁决 2026-10-19 第 3 条「售出月，只退款剔除」）：出票完成发布出票事实，单一酒吧门票据此逐票锁定综合服务费。
    @Test
    void issuingOrderPublishesIssuedFactForServiceFeeLock() {
        TicketTypeRecord ticketType = createTicketType("issued-fact");
        EntitlementTemplateRecord template = createTemplate("issued-fact", 1, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "issued-fact-bind", "bind", List.of(), template.id(), 1, 2, 9900L, "default", 1, 9900L));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "issued-fact-order", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-20"), "admin", "13800138000", "customer_issued_fact"));
        service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "issued-fact-issue", "issue", "paid offline", List.of()));

        assertThat(orderEvents).contains(new TicketRewardFacts.Issued(order.id()));
    }

    @Test
    void issuedTicketsDoNotReadMerchantConsumptionAccrualRate() {
        // P0-D/CFG-01（V340）：基础分 = 实付 ÷ 后台可配的「每积 1 分所需消费金额」，不再写死每元 1 分。
        TicketTypeRecord ticketType = createTicketType("points-rate");
        EntitlementTemplateRecord template = createTemplate("points-rate", 1, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "points-rate-bind", "bind", List.of(), template.id(), 1, 2, 9900L, "default", 1, 9900L));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "points-rate-order", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-20"), "admin", "13800138000", "customer_points_rate"));
        service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "points-rate-issue", "issue", "paid offline", List.of()));

        // 出厂默认 100（每元 1 分）：99.00 元 → 99 基础分（与改造前 amountCent/100 逐字等价）。
        assertThat(orderEvents).noneMatch(PointsEarnRequestedEvent.class::isInstance);

        // 改配置为 50 分/分（每元 2 分）→ 同样金额基础分翻倍，证明累计率不再写死。
        orderEvents.clear();
        pointsRateStub.amountCentPerPoint = 50L;
        TicketOrderRecord order2 = service.createTicketOrder(new TicketOrderRequest(
                "points-rate-order-2", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-20"), "admin", "13800138000", "customer_points_rate"));
        service.issueTicketOrder(order2.id(), new StatusChangeRequest(
                "points-rate-issue-2", "issue", "paid offline", List.of()));
        assertThat(orderEvents).noneMatch(PointsEarnRequestedEvent.class::isInstance);
    }

    @Test
    void ticketIssueDoesNotRequireMerchantConsumptionAccrualConfig() {
        // fail-closed（对齐结算配置中心 requireConfigured）：配置端口不可用时响亮报错，
        // 不静默回落硬编码「每元 1 分」、也不静默把会员该得的积分吞掉。
        TicketTypeRecord ticketType = createTicketType("points-rate-missing");
        EntitlementTemplateRecord template = createTemplate("points-rate-missing", 1, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "points-missing-bind", "bind", List.of(), template.id(), 1, 2, 9900L, "default", 1, 9900L));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "points-missing-order", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-20"), "admin", "13800138000", "customer_points_missing"));
        orderService.pointsAccrualRateConfigPort = null;

        service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "points-missing-issue", "issue", "paid offline", List.of()));
        assertThat(orderEvents).noneMatch(PointsEarnRequestedEvent.class::isInstance);
    }

    /** 取本轮唯一一条积分入账事件（orderEvents 仅收 TicketOrderService 发布的事件）。 */
    private PointsEarnRequestedEvent onlyPointsEvent() {
        List<PointsEarnRequestedEvent> found = orderEvents.stream()
                .filter(PointsEarnRequestedEvent.class::isInstance)
                .map(PointsEarnRequestedEvent.class::cast)
                .toList();
        assertThat(found).hasSize(1);
        return found.getFirst();
    }

    @Test
    void createTicketTypeWritesOpenApiAuditEvent() {
        TicketTypeRecord created = service.createTicketType(new TicketTypeRequest(
                "ticket-type-create",
                "create ticket type",
                List.of(),
                " P0 Pass ",
                "weekday_weekend_holiday",
                9900L,
                11900L,
                12900L,
                false,
                "active"
        , null, null, null, null, null, null, null));

        assertThat(created.id()).startsWith("ticket_type_");
        assertThat(created.ticketName()).isEqualTo("P0 Pass");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("ticket_product.changed");
    }

    @Test
    void publishBlocksAnyTicketWithAnEntitlementMissingItsFaceValuePrice() {
        // S11③:票卖出去以后,客人付的钱要按各权益的【票面原价】占比分给游乐公司/餐厅/零售商户/酒吧。
        //   少填一条价,这条就分不到钱、别人凭空多分,而且票已经卖了只能人工调账 → 票没配全价不许开卖。
        //   免费的填 0(0=免费=不参与分钱),不是留空;留空=还没定价。
        TicketTypeRecord pass = createTicketType("multi-pass");
        EntitlementTemplateRecord barTpl = service.createEntitlementTemplate(new EntitlementTemplateRequest(
                "s6-tpl-bar", "bar", List.of(), "S6 Bar Drink", "bar_drink", 1, 9900L, "active",
                null, "mch_s6_bar", null, null));
        EntitlementTemplateRecord rideTpl = service.createEntitlementTemplate(new EntitlementTemplateRequest(
                "s6-tpl-ride", "ride", List.of(), "S6 Ride", "amusement_ride", 1, 3000L, "active",
                null, "mch_s6_ride", null, null));
        service.bindTicketTypeEntitlement(pass.id(), new TicketTypeEntitlementRequest(
                "s6-bind-bar", "bind", List.of(), barTpl.id(), 1, 1, 9900L, "default", 1, 9900L));
        service.bindTicketTypeEntitlement(pass.id(), new TicketTypeEntitlementRequest(
                "s6-bind-ride", "bind", List.of(), rideTpl.id(), 1, 1, 3000L, "default", 1, null));

        assertThatThrownBy(() -> service.publishTicketType(pass.id(),
                        new StatusChangeRequest("s6-pub-1", "publish", "ready", List.of())))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("TICKET_COMPONENT_PRICE_REQUIRED"))
                .hasMessageContaining("S6 Ride");   // 报出是哪条权益没填,运营能直接去补

        // 再绑一条带价的同模板行不算补齐——逐行必须填全,不按模板聚合放水。
        service.bindTicketTypeEntitlement(pass.id(), new TicketTypeEntitlementRequest(
                "s6-bind-ride-2", "bind", List.of(), rideTpl.id(), 1, 1, 3000L, "default", 1, 3000L));
        assertThatThrownBy(() -> service.publishTicketType(pass.id(),
                        new StatusChangeRequest("s6-pub-2", "publish", "ready", List.of())))
                .isInstanceOf(ApiException.class);

        // S11③:单业态票同样要填(旧口径只卡跨业态票,于是纯酒吧票整票没价、出票时只能均摊兜底)。
        TicketTypeRecord single = createTicketType("single-pass");
        service.bindTicketTypeEntitlement(single.id(), new TicketTypeEntitlementRequest(
                "s6-bind-single", "bind", List.of(), barTpl.id(), 1, 1, 9900L, "default", 1, null));
        assertThatThrownBy(() -> service.publishTicketType(single.id(), new StatusChangeRequest(
                "s6-pub-3", "publish", "ready", List.of())))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("TICKET_COMPONENT_PRICE_REQUIRED"));

        // 填上票面原价(免费的填 0)即可发布。
        TicketTypeRecord ok = createTicketType("single-pass-priced");
        service.bindTicketTypeEntitlement(ok.id(), new TicketTypeEntitlementRequest(
                "s6-bind-priced", "bind", List.of(), barTpl.id(), 1, 1, 9900L, "default", 1, 9900L));
        service.bindTicketTypeEntitlement(ok.id(), new TicketTypeEntitlementRequest(
                "s6-bind-free", "bind", List.of(), rideTpl.id(), 1, 1, 0L, "default", 1, 0L));
        assertThat(service.publishTicketType(ok.id(), new StatusChangeRequest(
                "s6-pub-4", "publish", "ready", List.of())).status()).isEqualTo("published");
    }

    /** K17 用例夹具：某家酒吧的入场 + 可选赠酒，绑到同一张票上。 */
    private void bindBarEntitlements(String ticketTypeId, String barMerchantId, String tag, int giftCups) {
        ticketingRepository.merchantTypes.put(barMerchantId, "bar");
        EntitlementTemplateRecord entry = service.createEntitlementTemplate(new EntitlementTemplateRequest(
                "k17-tpl-entry-" + tag, "entry", List.of(), tag + " 入场", "admission", 1, 5000L, "active",
                null, barMerchantId, null, null));
        service.bindTicketTypeEntitlement(ticketTypeId, new TicketTypeEntitlementRequest(
                "k17-bind-entry-" + tag, "bind", List.of(), entry.id(), 1, 1, 5000L, "default", 1, 5000L));
        if (giftCups > 0) {
            ticketingRepository.barProducts.put("k17-prod-" + tag,
                    new BarProductRefRecord("k17-prod-" + tag, barMerchantId, 4200L, null, false, "active"));
            EntitlementTemplateRecord gift = service.createEntitlementTemplate(new EntitlementTemplateRequest(
                    "k17-tpl-gift-" + tag, "gift", List.of(), tag + " 赠酒", "gift_drink", 1, 0L, "active",
                    "k17-prod-" + tag, barMerchantId, null, null));
            // V376：赠送类绑定必须显式标成本承担方（漏标拒保存），夹具按改造前唯一行为标平台出资型。
            service.bindTicketTypeEntitlement(ticketTypeId, new TicketTypeEntitlementRequest(
                    "k17-bind-gift-" + tag, "bind", List.of(), gift.id(), giftCups, 1, 0L, "default", 1, 0L,
                    "platform_funded"));
        }
    }

    @Test
    void publishBlocksPassWhereSomeCoveredBarHasNoGiftDrink() {
        // K17（业务 HTML §通票赠酒按酒吧分配）：票已经给 A 吧配了赠酒，却让客人也能进 B 吧、B 吧一杯没有 →
        // 客人到了 B 吧要么白跑一趟、要么 B 吧店长自掏腰包，事后没法补（票面已印）。发布前点名报出来。
        TicketTypeRecord pass = createTicketType("k17-pass");
        bindBarEntitlements(pass.id(), "mch_bar_a", "A", 1);
        bindBarEntitlements(pass.id(), "mch_bar_b", "B", 0);

        assertThatThrownBy(() -> service.publishTicketType(pass.id(),
                new StatusChangeRequest("k17-pub-1", "publish", "ready", List.of())))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("PASS_BAR_GIFT_COVERAGE_INSUFFICIENT"));
    }

    @Test
    void publishAllowsPassWhenEveryCoveredBarHasAtLeastOneGiftDrink() {
        TicketTypeRecord pass = createTicketType("k17-pass-ok");
        bindBarEntitlements(pass.id(), "mch_bar_c", "C", 1);
        bindBarEntitlements(pass.id(), "mch_bar_d", "D", 2);

        assertThat(service.publishTicketType(pass.id(),
                new StatusChangeRequest("k17-pub-2", "publish", "ready", List.of())).status())
                .isEqualTo("published");

        // 票面按吧分组：各吧分别赠几杯，与发布校验取的是同一份数据。
        List<TicketTypeGiftCoverageRow> coverage = service.listGiftCoverage(pass.id()).items();
        assertThat(coverage).extracting(TicketTypeGiftCoverageRow::barId, TicketTypeGiftCoverageRow::barTotalCups)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("mch_bar_c", 1),
                        org.assertj.core.groups.Tuple.tuple("mch_bar_d", 2));
    }

    @Test
    void publishDoesNotRequireGiftDrinksOnAnOrdinaryTicketWithoutAnyGift() {
        // 普通门票本来就不含赠酒，卡它只会让运营连普通票都发不出去 → 本条不适用。
        TicketTypeRecord ticket = createTicketType("k17-plain");
        bindBarEntitlements(ticket.id(), "mch_bar_e", "E", 0);

        assertThat(service.publishTicketType(ticket.id(),
                new StatusChangeRequest("k17-pub-3", "publish", "ready", List.of())).status())
                .isEqualTo("published");
    }

    // ===== V376（2026-08-01 拍板）：赠送权益成本承担方必填，漏标拒保存/拒发布 =====

    @Test
    void bindRejectsGiftEntitlementMissingCostBearerAndAcceptsTaggedOne() {
        // 漏标不再默认平台出资型——否则运营漏标一个商户让利型赠品，平台就替商户多付一笔预估成本。
        TicketTypeRecord tt = createTicketType("cb-bind");
        ticketingRepository.barProducts.put("cb-prod",
                new BarProductRefRecord("cb-prod", "mch_cb_bar", 4200L, null, false, "active"));
        EntitlementTemplateRecord gift = service.createEntitlementTemplate(new EntitlementTemplateRequest(
                "cb-tpl-gift", "gift", List.of(), "赠酒一杯", "gift_drink", 1, 0L, "active",
                "cb-prod", "mch_cb_bar", null, null));

        // 赠送类（票面原价 0 + 提供方兑付类）漏标 → 中文 400 点名是哪条权益没标。
        assertThatThrownBy(() -> service.bindTicketTypeEntitlement(tt.id(), new TicketTypeEntitlementRequest(
                "cb-bind-miss", "bind", List.of(), gift.id(), 1, 1, 0L, "default", 1, 0L)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("TICKET_COST_BEARER_REQUIRED"))
                .hasMessageContaining("赠酒一杯");

        // 标了（商户让利型）即可保存，存的就是标的值。
        assertThat(service.bindTicketTypeEntitlement(tt.id(), new TicketTypeEntitlementRequest(
                "cb-bind-tagged", "bind", List.of(), gift.id(), 1, 1, 0L, "default", 1, 0L,
                "merchant_concession")).costBearer()).isEqualTo("merchant_concession");

        // 非赠送类（入场，票面价 >0）没有成本承担概念：不带承担方照常绑，存 NULL 不再悄悄落平台出资型。
        EntitlementTemplateRecord entry = service.createEntitlementTemplate(new EntitlementTemplateRequest(
                "cb-tpl-entry", "entry", List.of(), "普通入场", "admission", 1, 5000L, "active",
                null, "mch_cb_bar", null, null));
        assertThat(service.bindTicketTypeEntitlement(tt.id(), new TicketTypeEntitlementRequest(
                "cb-bind-entry", "bind", List.of(), entry.id(), 1, 1, 5000L, "default", 1, 5000L))
                .costBearer()).isNull();
    }

    @Test
    void publishBlocksGiftEntitlementWhoseCostBearerWasNeverTagged() {
        // 发布闸兜底：绑定后模板被补上专属商户等而「变成」赠送类的旧绑定，绑定面拦不到 → 发布前逐行再查、点名拒发布。
        Instant now = Instant.now();
        TicketTypeRecord tt = createTicketType("cb-pub");
        // 专属商户餐饮权益（providerRedeemed 第三判据）+ 票面原价 0 = 赠送类；cost_bearer 未标（NULL）。
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_cb_free", "免费小食", "dining", 1, 0L, "active", "mch_cb_rest", null, null, null, now, now,
                "virtual", 2000L, null, "mch_cb_rest"));
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_cb_free", tt.id(), "tpl_cb_free", 1, 1, 0L, "default", 1, 0L, null, now, now));

        assertThatThrownBy(() -> service.publishTicketType(tt.id(),
                new StatusChangeRequest("cb-pub-1", "publish", "ready", List.of())))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("TICKET_COST_BEARER_REQUIRED"))
                .hasMessageContaining("免费小食");

        // 非赠送类（入场）cost_bearer 为 NULL 不受影响：照常发布，别误伤普通票。
        TicketTypeRecord plain = createTicketType("cb-pub-plain");
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_cb_entry", "普通入场", "admission", 1, 5000L, "active", "mch_cb_rest", null, null, null, now, now,
                "virtual", null, null, null));
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_cb_entry", plain.id(), "tpl_cb_entry", 1, 1, 5000L, "default", 1, 5000L, null, now, now));
        assertThat(service.publishTicketType(plain.id(),
                new StatusChangeRequest("cb-pub-2", "publish", "ready", List.of())).status())
                .isEqualTo("published");
    }

    @Test
    void createEntitlementTemplateIsIdempotent() {
        EntitlementTemplateRequest request = new EntitlementTemplateRequest(
                "template-once",
                "create template",
                List.of(),
                "P0 Entry",
                "entry",
                2,
                9900L,
                "active",
                null,
                "mch_idem",
                null,
                null
        );

        EntitlementTemplateRecord first = service.createEntitlementTemplate(request);
        EntitlementTemplateRecord second = service.createEntitlementTemplate(request);

        assertThat(second).isEqualTo(first);
        assertThat(ticketingRepository.entitlementTemplateSaveCount).isEqualTo(1);
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "ticket_entitlement_template.changed".equals(event.eventName()))
                .hasSize(1);
    }

    @Test
    void createEntitlementTemplateRejectsNullMerchant() {
        // 审计断层①：模板不绑 merchant_id → 核销结算款因月度成账 SQL(merchant_id IS NOT NULL)成孤儿。
        // 从源头拒绝:建模板必须绑商户,否则 PARAM_INVALID。
        EntitlementTemplateRequest noMerchant = new EntitlementTemplateRequest(
                "tpl-no-merchant", "create", List.of(), "Orphan Entry", "entry", 1, 9900L, "active",
                null, null, null, null);
        assertThatThrownBy(() -> service.createEntitlementTemplate(noMerchant))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("merchant_id");
    }

    @Test
    void directIssueCreatesEntitlementCodeFromBoundTemplate() {
        TicketTypeRecord ticketType = createTicketType("issue-ticket");
        EntitlementTemplateRecord template = createTemplate("issue-template", 2, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "bind-template",
                "bind",
                List.of(),
                template.id(),
                1,
                2,
                9900L,
                "default",
                1,
                9900L
        ));

        IssuedTicketResponse issued = issueDirectlyForTest(ticketType.id(), new TicketIssueRequest(
                "issue-direct",
                "issue",
                List.of(),
                "order_1",
                "manual",
                List.of("qr")
        ));

        assertThat(issued.issuedEntitlements()).hasSize(1);
        assertThat(issued.issuedEntitlements().getFirst().entitlementCode()).startsWith("P0-");
        assertThat(ticketingRepository.issuedEntitlements).hasSize(1);
        // AUD-019：direct 端点退役后本用例走出票内核（无 executeWrite 审计包装），审计事件由订单出票链专项覆盖。
    }

    @Test
    void previewDoesNotConsumeRemainingTimes() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("preview");

        VerificationPreviewResponse preview = service.previewVerification(new VerificationRequest(
                "preview-once",
                "qr",
                entitlement.entitlementCode(),
                "dev_1",
                "gate_1",
                Map.of("scenario", "ticket_checkin")
        ));

        assertThat(preview.allowed()).isTrue();
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode()))
                .get()
                .extracting(IssuedEntitlementRecord::usedTimes)
                .isEqualTo(0);
    }

    // R5 T5：多人票核销，verified_people 随权益 num_people 写入（当前默认 1）。
    @Test
    void verificationWritesVerifiedPeopleFromEntitlementNumPeople() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("verified-people");
        // 模拟多人票：把权益 num_people 改为 4（来源待票种配置，本期发放默认 1）。
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                entitlement.id(), entitlement.entitlementCode(), entitlement.ticketTypeId(), entitlement.orderId(),
                entitlement.entitlementTemplateId(), entitlement.entitlementType(), entitlement.merchantId(),
                entitlement.venueId(), entitlement.pointId(), entitlement.totalTimes(), entitlement.usedTimes(),
                entitlement.valueAmountCent(), entitlement.status(), entitlement.validFrom(), entitlement.validTo(),
                entitlement.createdAt(), entitlement.updatedAt(), 4, entitlement.allocatedRevenueCent()
        ,
        null, null, null, null, null, 0, null, null));

        service.createVerification(new VerificationRequest(
                "verified-people-1", "qr", entitlement.entitlementCode(), "dev_1", "gate_1", Map.of()));

        assertThat(ticketingRepository.verifications.values())
                .filteredOn(v -> "success".equals(v.status()))
                .singleElement()
                .extracting(VerificationRecord::verifiedPeople)
                .isEqualTo(4);
        // used_times 一次性 +1（沿用 atomicUseEntitlement），不超 total_times。
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode()))
                .get().extracting(IssuedEntitlementRecord::usedTimes).isEqualTo(1);
    }

    // 2a-4（用户裁定）：binding.num_people=4 → 购买即 fan-out 为 4 张独立单人票（各 num_people=1），各自独立核销。
    @Test
    void multiPersonBindingFansOutIntoIndependentSinglePersonTickets() {
        Instant now = Instant.now();
        ticketingRepository.saveTicketType(new TicketTypeRecord(
                "tt_np4", "Group Pass", "weekday_weekend_holiday", 9900L, 11900L, 12900L,
                false, "published", null, null, now, now, null, 0, null, false, null, TicketTypeRecord.NO_COMBO_DISCOUNT));
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_np4", "Group Entry", "entry", 1, 9900L, "active", null, null, null, null, now, now,
                null, null, null, null));
        // binding 配 num_people=4（多人票/联合票运营配置；本期通过仓储直配，创建 DTO 透传为后续工作）。
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_np4", "tt_np4", "tpl_np4", 1, 1, 9900L, "default", 4, null, now, now));

        IssuedTicketResponse issued = issueDirectlyForTest("tt_np4", new TicketIssueRequest(
                "np4-issue", "issue", List.of(), "order_np4", "manual", List.of("qr")));
        // 4 人票 fan-out 为 4 张独立票（quantity 1 × num_people 4）。
        assertThat(issued.issuedEntitlements()).hasSize(4);
        // 每张均为独立单人票（num_people=1），共享有效期、各自独立核销。
        assertThat(ticketingRepository.issuedEntitlements.values())
                .filteredOn(e -> "order_np4".equals(e.orderId()))
                .hasSize(4)
                .allSatisfy(e -> assertThat(e.numPeople()).isEqualTo(1));

        // 核销其中一张 → verified_people=1（独立单人核销，占 1 容量/扣 1 次）。
        String code = issued.issuedEntitlements().getFirst().entitlementCode();
        service.createVerification(new VerificationRequest(
                "np4-verify", "qr", code, "dev_1", "gate_1", Map.of()));
        assertThat(ticketingRepository.verifications.values())
                .filteredOn(v -> "success".equals(v.status()))
                .singleElement()
                .extracting(VerificationRecord::verifiedPeople)
                .isEqualTo(1);
    }

    // AUD-066 按张出票（用户口径）：一单买 N 张同种票 = N 个父票实例各一套权益；多人票按人分 slot 1..N（无 slot 0）。
    @Test
    void perOrderQuantityFansOutIntoParentTicketsEachWithPersonSlots() {
        TicketTypeRecord ticketType = createTicketType("qty-fanout");
        EntitlementTemplateRecord template = createTemplate("qty-fanout", 1, 9900L);
        // 四人票：binding num_people=4。
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "qty-fanout-bind", "bind", List.of(), template.id(), 1, 1, 9900L, "default", 4, 9900L));
        // 一单买 2 张。
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "qty-fanout-order", "create", List.of(), ticketType.id(), null, 2,
                LocalDate.parse("2026-06-15"), "admin", "13800138000", "cust_qty_fanout"));
        IssuedTicketResponse issued = service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "qty-fanout-issue", "issue", "issue", List.of()));

        // 2 张 × 4 人 = 8 权益（修此前"收 2 张钱只发 1 套 4 权益"的 bug）。
        assertThat(issued.issuedEntitlements()).hasSize(8);
        List<IssuedEntitlementRecord> ents = ticketingRepository.listIssuedEntitlementsByOrderId(order.id());
        assertThat(ents).hasSize(8);
        // 2 个父票实例，各 num_people=4。
        assertThat(ticketingRepository.ticketInstances.values())
                .filteredOn(ti -> order.id().equals(ti.orderId()))
                .hasSize(2)
                .allSatisfy(ti -> assertThat(ti.numPeople()).isEqualTo(4));
        // 每个父票下 4 条权益，slot_index 恰为 1..4（无 slot 0），每条 num_people=1（一行=一人）。
        Map<String, List<IssuedEntitlementRecord>> byParent = new LinkedHashMap<>();
        for (IssuedEntitlementRecord e : ents) {
            assertThat(e.parentTicketId()).isNotNull();
            assertThat(e.numPeople()).isEqualTo(1);
            byParent.computeIfAbsent(e.parentTicketId(), k -> new ArrayList<>()).add(e);
        }
        assertThat(byParent).hasSize(2);
        byParent.values().forEach(group -> assertThat(group)
                .hasSize(4)
                .extracting(IssuedEntitlementRecord::slotIndex)
                .containsExactlyInAnyOrder(1, 2, 3, 4));
    }

    // AUD-066 多人票同场次核销守卫：同场次内不同人可不同时间分别入场（都放行，不要求同一时刻）。
    @Test
    void multiPersonSiblingsAllowSameSessionDifferentTimes() {
        List<String> codes = issueTwoPersonTicketCodes("mp-same");
        ticketingRepository.openSessionId = "S1";
        VerificationResponse first = service.createVerification(new VerificationRequest(
                "mp-same-1", "qr", codes.get(0), "dev_1", "gate_1", Map.of()));
        assertThat(first.status()).isEqualTo("success");
        // 同一场次 S1，第二人稍后入场：放行成功。
        VerificationResponse second = service.createVerification(new VerificationRequest(
                "mp-same-2", "qr", codes.get(1), "dev_1", "gate_1", Map.of()));
        assertThat(second.status()).isEqualTo("success");
    }

    // AUD-066 多人票同场次核销守卫：跨场次核销拒（一人在 S1 入场后，另一人不能在 S2 入场）。
    @Test
    void multiPersonSiblingsRejectCrossSessionVerification() {
        List<String> codes = issueTwoPersonTicketCodes("mp-cross");
        ticketingRepository.openSessionId = "S1";
        service.createVerification(new VerificationRequest(
                "mp-cross-1", "qr", codes.get(0), "dev_1", "gate_1", Map.of()));
        // 切到别的场次 S2：另一人份额被拒（不能跨场次/日期）。
        ticketingRepository.openSessionId = "S2";
        assertThatThrownBy(() -> service.createVerification(new VerificationRequest(
                "mp-cross-2", "qr", codes.get(1), "dev_1", "gate_1", Map.of())))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).code()).isEqualTo("MULTI_PERSON_CROSS_SESSION_FORBIDDEN"));
    }

    /** AUD-066 测试辅助：直发一张 2 人票（1 父票 + slot 1/2 两条权益），返回两人的权益码（顺序即 slot 1、slot 2）。 */
    private List<String> issueTwoPersonTicketCodes(String key) {
        TicketTypeRecord ticketType = createTicketType(key);
        EntitlementTemplateRecord template = createTemplate(key, 1, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                key + "-bind", "bind", List.of(), template.id(), 1, 1, 9900L, "default", 2, 9900L));
        IssuedTicketResponse issued = issueDirectlyForTest(ticketType.id(), new TicketIssueRequest(
                key + "-issue", "issue", List.of(), "order_" + key, "manual", List.of("qr")));
        assertThat(issued.issuedEntitlements()).hasSize(2);
        return issued.issuedEntitlements().stream().map(IssuedEntitlementResponse::entitlementCode).toList();
    }

    // ===== V253 数据存档与审批后删除范式（试点）=====

    /**
     * 批10/E(V380)：内部结算价（签约价）唯一可编辑处 = 内部结算 SKU 目录。
     * 模板侧的两条旧路都必须封死且喊出来（静默忽略会骗运营"以为改好了"）：
     * ①建模板直填 agreement_price_cent → 400；②引用不存在的目录条目 → 400。
     * （原 updateAgreementPrice 端点已随 PR#274 下线，改价走目录草案→他人复核转正→快照传播。）
     */
    @Test
    void agreementPriceIsCatalogOnlySinceV380() {
        TicketCatalogService catalog = new TicketCatalogService(ticketingRepository, null);

        // ① 直填签约价：拒绝并指路目录。
        assertThatThrownBy(() -> catalog.createEntitlementTemplate(new EntitlementTemplateRequest(
                "cat-only-1", null, List.of(), "餐饮优惠", "dining", 1, 5000L, "active",
                null, "m1", null, null, 3000L, null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("AGREEMENT_PRICE_CATALOG_ONLY"));

        // ② 引用不存在的目录条目：当场拒绝，不留一个悬空引用等核销时炸。
        assertThatThrownBy(() -> catalog.createEntitlementTemplate(new EntitlementTemplateRequest(
                "cat-only-2", null, List.of(), "餐饮优惠", "dining", 1, 5000L, "active",
                null, "m1", null, null, null, null, "sku_not_exist")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CATALOG_ITEM_NOT_FOUND"));

        // ③ 不带价、不带引用：允许建（未配价权益,提供方兑付类核销时 fail-closed,其余回退 value_amount）。
        EntitlementTemplateRecord created = catalog.createEntitlementTemplate(new EntitlementTemplateRequest(
                "cat-only-3", null, List.of(), "餐饮优惠", "dining", 1, 5000L, "active",
                null, "m1", null, null, null, null, null));
        assertThat(created.agreementPriceCent()).isNull();
        assertThat(created.catalogItemCode()).isNull();
    }

    @Test
    void archiveHidesTemplateFromDefaultListAndUnarchiveRestores() {
        Instant now = Instant.now();
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_arch", "过期入园", "entry", 1, 100L, "active", "m1", null, null, null, now, now, null, null, null, null));
        TicketCatalogService catalog = new TicketCatalogService(ticketingRepository, null);

        catalog.archiveEntitlementTemplate("tpl_arch", true, "测试存档理由", "op_a");
        assertThat(ticketingRepository.listEntitlementTemplates(false)).isEmpty();
        assertThat(ticketingRepository.listEntitlementTemplates(true)).hasSize(1);
        assertThat(ticketingRepository.findEntitlementTemplateById("tpl_arch").orElseThrow().archivedAt()).isNotNull();

        catalog.archiveEntitlementTemplate("tpl_arch", false, null, "op_a");
        assertThat(ticketingRepository.listEntitlementTemplates(false)).hasSize(1);
        assertThat(ticketingRepository.findEntitlementTemplateById("tpl_arch").orElseThrow().archivedAt()).isNull();
    }

    @Test
    void archiveAndDeleteRejectBlankReason() {
        Instant now = Instant.now();
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_noreason", "缺理由", "entry", 1, 100L, "active", "m1", null, null, null, now, now, null, null, null, null));
        TicketCatalogService catalog = new TicketCatalogService(ticketingRepository, null);

        // 存档缺理由被拒(业务HTML 2026-07-07)：空白理由不得存档，且未发生副作用。
        assertThatThrownBy(() -> catalog.archiveEntitlementTemplate("tpl_noreason", true, "  ", "op_a"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("reason is required");
        assertThat(ticketingRepository.findEntitlementTemplateById("tpl_noreason").orElseThrow().archivedAt()).isNull();

        // 删除申请缺理由被拒。
        catalog.archiveEntitlementTemplate("tpl_noreason", true, "误配存档", "op_a");
        assertThatThrownBy(() -> catalog.requestDeleteEntitlementTemplate("tpl_noreason",
                new com.complex.platform.ticketing.dto.EntitlementTemplateDeleteRequest("k1", null), "op_a"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("reason is required");
    }

    @Test
    void deleteRequiresArchiveFirstThenApprovalAndForbidsSelfApprove() {
        Instant now = Instant.now();
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_del", "配错模板", "entry", 1, 100L, "active", "m1", null, null, null, now, now, null, null, null, null));
        TicketCatalogService catalog = new TicketCatalogService(ticketingRepository, null);

        // 未存档不得申请删除。
        assertThatThrownBy(() -> catalog.requestDeleteEntitlementTemplate("tpl_del",
                new com.complex.platform.ticketing.dto.EntitlementTemplateDeleteRequest("k1", "误配"), "op_a"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("存档");

        catalog.archiveEntitlementTemplate("tpl_del", true, "测试存档理由", "op_a");
        catalog.requestDeleteEntitlementTemplate("tpl_del",
                new com.complex.platform.ticketing.dto.EntitlementTemplateDeleteRequest("k1", "误配"), "op_a");
        assertThat(ticketingRepository.findEntitlementTemplateById("tpl_del").orElseThrow().deleteStatus()).isEqualTo("pending");

        // 自审禁止：申请人 op_a 不能审批自己的申请。
        assertThatThrownBy(() -> catalog.approveDeleteEntitlementTemplate("tpl_del",
                new com.complex.platform.ticketing.dto.EntitlementTemplateDeleteApprovalRequest("k2", "approved", null), "op_a"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("不能审批");

        // 他人审批通过 → 物理删除。
        EntitlementTemplateDeleteDecision decision = catalog.approveDeleteEntitlementTemplate("tpl_del",
                new com.complex.platform.ticketing.dto.EntitlementTemplateDeleteApprovalRequest("k3", "approved", null), "op_b");
        assertThat(decision.deleted()).isTrue();
        assertThat(ticketingRepository.findEntitlementTemplateById("tpl_del")).isEmpty();
    }

    @Test
    void bindRejectsArchivedTemplate() {
        // 复核§3:已存档模板不可再被绑定到新票种。
        TicketTypeRecord tt = createTicketType("arch-bind-tt");
        Instant now = Instant.now();
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_ab", "已存档模板", "entry", 1, 100L, "active", "m1", null, null, null, now, now, null, null, null, null)
                .withArchive(now, "op_a", now));
        assertThatThrownBy(() -> service.bindTicketTypeEntitlement(tt.id(), new TicketTypeEntitlementRequest(
                "bind-ab", "bind", java.util.List.of(), "tpl_ab", 1, 1, 100L, "default", 1, 9900L)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("不可绑定");
    }

    @Test
    void unarchiveClearsPendingDeleteRequest() {
        // 复核§5:取消存档时把 pending 删除申请回退 none,避免"生效可见却挂着待删"迷惑态。
        Instant now = Instant.now();
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_uc", "配错待删", "entry", 1, 100L, "active", "m1", null, null, null, now, now, null, null, null, null));
        TicketCatalogService catalog = new TicketCatalogService(ticketingRepository, null);
        catalog.archiveEntitlementTemplate("tpl_uc", true, "测试存档理由", "op_a");
        catalog.requestDeleteEntitlementTemplate("tpl_uc",
                new com.complex.platform.ticketing.dto.EntitlementTemplateDeleteRequest("k1", "误配"), "op_a");
        assertThat(ticketingRepository.findEntitlementTemplateById("tpl_uc").orElseThrow().deleteStatus()).isEqualTo("pending");
        catalog.archiveEntitlementTemplate("tpl_uc", false, null, "op_a");
        EntitlementTemplateRecord t = ticketingRepository.findEntitlementTemplateById("tpl_uc").orElseThrow();
        assertThat(t.archivedAt()).isNull();
        assertThat(t.deleteStatus()).isEqualTo("none");
    }

    // ===== V260 存档/删除范式铺到票种/价格规则 =====

    @Test
    void archivedTicketTypeRejectsNewPriceRuleBindingAndOrder() {
        // 复核§3(V260)：存档=软下线，三个引用入口（配价规/绑权益/下单）都必须拒，否则删除永远被新引用卡死。
        TicketTypeRecord tt = createTicketType("archived-ref-guard");
        TicketCatalogService catalog = new TicketCatalogService(ticketingRepository, null);
        catalog.archiveTicketType(tt.id(), true, "测试存档理由", "op_a");

        assertThatThrownBy(() -> service.createTicketPriceRule(new TicketPriceRuleRequest(
                "arch-rule", "fixed", List.of(), tt.id(), "student", "fixed_price",
                1000L, null, null, null, "ch_default", null)))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("TICKET_TYPE_NOT_CONFIGURABLE"));

        assertThatThrownBy(() -> service.bindTicketTypeEntitlement(tt.id(), new TicketTypeEntitlementRequest(
                "arch-bind", "bind", java.util.List.of(), "tpl_any", 1, 1, 100L, "default", 1, 9900L)))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("TICKET_TYPE_NOT_BINDABLE"));

        assertThatThrownBy(() -> service.createTicketOrder(new TicketOrderRequest(
                "arch-order", "order", List.of(), tt.id(), null, 1,
                LocalDate.parse("2026-07-01"), "app", "13800138000", "customer_x")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("TICKET_TYPE_NOT_SELLABLE"));
    }



    @Test
    void ticketTypeArchiveHidesFromDefaultListAndUnarchiveRestores() {
        Instant now = Instant.now();
        ticketingRepository.saveTicketType(new TicketTypeRecord(
                "tt_arch", "配错票种", "calendar", 100L, 200L, 300L, false, "active", null, null, now, now, null, 0, null, false, null, TicketTypeRecord.NO_COMBO_DISCOUNT));
        TicketCatalogService catalog = new TicketCatalogService(ticketingRepository, null);

        catalog.archiveTicketType("tt_arch", true, "测试存档理由", "op_a");
        assertThat(ticketingRepository.listTicketTypes(false)).isEmpty();
        assertThat(ticketingRepository.listTicketTypes(true)).hasSize(1);
        assertThat(ticketingRepository.findTicketTypeById("tt_arch").orElseThrow().archivedAt()).isNotNull();

        catalog.archiveTicketType("tt_arch", false, null, "op_a");
        assertThat(ticketingRepository.listTicketTypes(false)).hasSize(1);
        assertThat(ticketingRepository.findTicketTypeById("tt_arch").orElseThrow().archivedAt()).isNull();
    }

    @Test
    void ticketTypeDeleteRequiresArchiveForbidsSelfApproveThenDeletes() {
        Instant now = Instant.now();
        ticketingRepository.saveTicketType(new TicketTypeRecord(
                "tt_del", "误录票种", "calendar", 100L, 200L, 300L, false, "active", null, null, now, now, null, 0, null, false, null, TicketTypeRecord.NO_COMBO_DISCOUNT));
        TicketCatalogService catalog = new TicketCatalogService(ticketingRepository, null);

        // 未存档不得申请删除。
        assertThatThrownBy(() -> catalog.requestDeleteTicketType("tt_del",
                new TicketCatalogDeleteRequest("k1", "误录"), "op_a"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("先存档");

        catalog.archiveTicketType("tt_del", true, "测试存档理由", "op_a");
        catalog.requestDeleteTicketType("tt_del", new TicketCatalogDeleteRequest("k1", "误录"), "op_a");
        assertThat(ticketingRepository.findTicketTypeById("tt_del").orElseThrow().deleteStatus()).isEqualTo("pending");

        // 禁自审：申请人 op_a 不能审批自己的申请。
        assertThatThrownBy(() -> catalog.approveDeleteTicketType("tt_del",
                new TicketCatalogDeleteApprovalRequest("k2", "approved", null), "op_a"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("不能审批");

        // 他人审批通过（无引用）→ 物理删除。
        ticketingRepository.ticketTypeReferenceCount = 0;
        TicketTypeDeleteDecision decision = catalog.approveDeleteTicketType("tt_del",
                new TicketCatalogDeleteApprovalRequest("k3", "approved", null), "op_b");
        assertThat(decision.deleted()).isTrue();
        assertThat(ticketingRepository.findTicketTypeById("tt_del")).isEmpty();
    }

    @Test
    void ticketTypeDeleteBlockedWhenStillReferenced() {
        Instant now = Instant.now();
        ticketingRepository.saveTicketType(new TicketTypeRecord(
                "tt_used", "在用票种", "calendar", 100L, 200L, 300L, false, "active", null, null, now, now, null, 0, null, false, null, TicketTypeRecord.NO_COMBO_DISCOUNT));
        TicketCatalogService catalog = new TicketCatalogService(ticketingRepository, null);
        catalog.archiveTicketType("tt_used", true, "测试存档理由", "op_a");
        catalog.requestDeleteTicketType("tt_used", new TicketCatalogDeleteRequest("k1", "尝试删在用"), "op_a");

        ticketingRepository.ticketTypeReferenceCount = 3; // 模拟仍被订单/价规/权益绑定引用
        assertThatThrownBy(() -> catalog.approveDeleteTicketType("tt_used",
                new TicketCatalogDeleteApprovalRequest("k2", "approved", null), "op_b"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("TICKET_TYPE_IN_USE"));
        // 拒删后票种仍在（含已存档列表可见），pending 态保留。
        assertThat(ticketingRepository.listTicketTypes(true)).hasSize(1);
    }

    @Test
    void priceRuleArchiveDeleteFlowWithReferenceGuardAndUnarchiveRollback() {
        Instant now = Instant.now();
        ticketingRepository.savePriceRule(new TicketPriceRuleRecord(
                "pr_del", "tt_x", "student", "calendar", 100L, 200L, 300L, null,
                "ch_default", "CNY", "active", Map.of(), now, now));
        TicketCatalogService catalog = new TicketCatalogService(ticketingRepository, null);

        catalog.archivePriceRule("pr_del", true, "测试存档理由", "op_a");
        assertThat(ticketingRepository.listPriceRules(false)).isEmpty();
        assertThat(ticketingRepository.listPriceRules(true)).hasSize(1);

        catalog.requestDeletePriceRule("pr_del", new TicketCatalogDeleteRequest("k1", "配错"), "op_a");
        // 取消存档 → pending 回退 none（复核§5）。
        catalog.archivePriceRule("pr_del", false, null, "op_a");
        TicketPriceRuleRecord back = ticketingRepository.findPriceRuleById("pr_del").orElseThrow();
        assertThat(back.archivedAt()).isNull();
        assertThat(back.deleteStatus()).isEqualTo("none");

        // 重新走删除：有引用拒删，无引用他人审批通过删除。
        catalog.archivePriceRule("pr_del", true, "测试存档理由", "op_a");
        catalog.requestDeletePriceRule("pr_del", new TicketCatalogDeleteRequest("k2", "配错"), "op_a");
        ticketingRepository.priceRuleReferenceCount = 2;
        assertThatThrownBy(() -> catalog.approveDeletePriceRule("pr_del",
                new TicketCatalogDeleteApprovalRequest("k3", "approved", null), "op_b"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("TICKET_PRICE_RULE_IN_USE"));
        ticketingRepository.priceRuleReferenceCount = 0;
        TicketPriceRuleDeleteDecision decision = catalog.approveDeletePriceRule("pr_del",
                new TicketCatalogDeleteApprovalRequest("k4", "approved", null), "op_b");
        assertThat(decision.deleted()).isTrue();
        assertThat(ticketingRepository.findPriceRuleById("pr_del")).isEmpty();
    }

    @Test
    void deleteApprovalBlockedWhenTemplateStillReferenced() {
        Instant now = Instant.now();
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_used", "在用模板", "entry", 1, 100L, "active", "m1", null, null, null, now, now, null, null, null, null));
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_used", "tt_x", "tpl_used", 1, 1, 100L, "default", 1, null, now, now));
        TicketCatalogService catalog = new TicketCatalogService(ticketingRepository, null);

        catalog.archiveEntitlementTemplate("tpl_used", true, "测试存档理由", "op_a");
        catalog.requestDeleteEntitlementTemplate("tpl_used",
                new com.complex.platform.ticketing.dto.EntitlementTemplateDeleteRequest("k1", "在用模板误配需删"), "op_a");

        assertThatThrownBy(() -> catalog.approveDeleteEntitlementTemplate("tpl_used",
                new com.complex.platform.ticketing.dto.EntitlementTemplateDeleteApprovalRequest("k2", "approved", null), "op_b"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("引用");
        // 仍在库（未删）。
        assertThat(ticketingRepository.findEntitlementTemplateById("tpl_used")).isPresent();
    }

    // R5 T5：核销冲正回滚 used_times，原记录置 reversed，新建 reversed 记录指向原记录。
    @Test
    void reverseVerificationRollsBackUsedTimesAndMarksOriginalReversed() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("reverse-rollback");
        VerificationResponse success = service.createVerification(new VerificationRequest(
                "reverse-rollback-1", "qr", entitlement.entitlementCode(), "dev_1", "gate_1", Map.of()));
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode()))
                .get().extracting(IssuedEntitlementRecord::usedTimes).isEqualTo(1);

        VerificationResponse reversal = service.reverseVerification(
                success.id(),
                new com.complex.platform.ticketing.dto.VerificationReverseRequest("reverse-rollback-rev", "误核销"),
                "supervisor_1");

        assertThat(reversal.status()).isEqualTo("reversed");
        // used_times 回滚到 0，权益恢复 active。
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode()))
                .get().extracting(IssuedEntitlementRecord::usedTimes, IssuedEntitlementRecord::status)
                .containsExactly(0, "active");
        // 原记录置 reversed + reversed_by/reversed_at。
        assertThat(ticketingRepository.findVerificationById(success.id()))
                .get()
                .satisfies(v -> {
                    assertThat(v.status()).isEqualTo("reversed");
                    assertThat(v.reversedBy()).isEqualTo("supervisor_1");
                    assertThat(v.reversedAt()).isNotNull();
                });
        // 新建一条 reversed 记录 reverses_verification_id 指向原 id。
        assertThat(ticketingRepository.findVerificationById(reversal.id()))
                .get()
                .satisfies(v -> {
                    assertThat(v.status()).isEqualTo("reversed");
                    assertThat(v.reversesVerificationId()).isEqualTo(success.id());
                });
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket.verification_reversed");
    }

    // R5 T5：重复冲正被拒（幂等/重复冲正拒绝）。
    @Test
    void reverseVerificationRejectsRepeatedReverse() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("reverse-dup");
        VerificationResponse success = service.createVerification(new VerificationRequest(
                "reverse-dup-1", "qr", entitlement.entitlementCode(), "dev_1", "gate_1", Map.of()));
        service.reverseVerification(success.id(),
                new com.complex.platform.ticketing.dto.VerificationReverseRequest("reverse-dup-rev-1", "误核销"),
                "supervisor_1");

        // 第二次冲正（不同幂等键，非幂等回放）原记录已 reversed（非 success 且 reversed_at 非空）→ 拒绝，不二次回滚。
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.reverseVerification(success.id(),
                        new com.complex.platform.ticketing.dto.VerificationReverseRequest("reverse-dup-rev-2", "再次冲正"),
                        "supervisor_1"))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageMatching(".*(已冲正|冲正).*");
        // used_times 不被二次回滚（仍为 0，不会变负）。
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode()))
                .get().extracting(IssuedEntitlementRecord::usedTimes).isEqualTo(0);
    }

    // F-TXN-04 并发双花：markVerificationReversed 是状态 CAS——首次得 1（success+未冲正），二次得 0（已 reversed）。
    // 锁定并发唯一闸：两路冲正只一路 CAS 胜出，另一路得 0 → service 据此拒绝，不二次回滚 used_times。
    @Test
    void markVerificationReversedIsCasAndSecondCallReturnsZero() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("reverse-cas");
        VerificationResponse success = service.createVerification(new VerificationRequest(
                "reverse-cas-1", "qr", entitlement.entitlementCode(), "dev_1", "gate_1", Map.of()));

        java.time.Instant now = java.time.Instant.now();
        // 首次 CAS：success 且 reversed_at IS NULL → 翻转，返回 1。
        assertThat(ticketingRepository.markVerificationReversed(success.id(), "supervisor_1", now)).isEqualTo(1);
        // 二次 CAS（模拟并发败方/重复）：已 reversed（reversed_at 非空）→ 不翻转，返回 0。
        assertThat(ticketingRepository.markVerificationReversed(success.id(), "supervisor_2", now)).isZero();
        // reversed_by 不被二次覆盖（败方不得篡改胜方留痕）。
        assertThat(ticketingRepository.findVerificationById(success.id()))
                .get().extracting(VerificationRecord::reversedBy).isEqualTo("supervisor_1");
    }

    // F-TXN-04 并发双花（多次票）：核销冲正先以状态 CAS 抢闸、再回滚 used_times。
    // 模拟并发：手工对同一核销先抢得冲正闸（markVerificationReversed=1）后，service.reverseVerification 再次冲正该核销
    // 必须被拒（CAS 败方），且【不】二次回滚 used_times（多次票 used_times 不被多减、不虚增权益）。
    @Test
    void reverseRejectedWhenVerificationAlreadyReversedConcurrently() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("reverse-concurrent");
        // 多次票：核销两次，used_times=2。
        service.createVerification(new VerificationRequest(
                "reverse-concurrent-a", "qr", entitlement.entitlementCode(), "dev_1", "gate_1", Map.of()));
        VerificationResponse second = service.createVerification(new VerificationRequest(
                "reverse-concurrent-b", "qr", entitlement.entitlementCode(), "dev_1", "gate_1", Map.of()));
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode()))
                .get().extracting(IssuedEntitlementRecord::usedTimes).isEqualTo(2);

        // 模拟并发胜方：抢先把第二条核销置 reversed（CAS 得手），但此处【不】回滚 used_times（保持 used_times=2 以暴露双减风险）。
        assertThat(ticketingRepository.markVerificationReversed(second.id(), "winner", java.time.Instant.now()))
                .isEqualTo(1);

        // 并发败方：service 冲正同一核销被拒、回滚前即止。
        // （本顺序化模拟里 original 已读到 reversed，先被 status!=success 读检拦下；真正并发中两路都读到 success 时，
        //  则由 markVerificationReversed 的状态 CAS 兜底——败方得 0 → 抛 ALREADY_REVERSED。两道都在回滚 used_times 之前。）
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.reverseVerification(second.id(),
                        new com.complex.platform.ticketing.dto.VerificationReverseRequest("reverse-concurrent-rev", "并发重复冲正"),
                        "loser"))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageMatching(".*(已冲正|冲正).*");

        // used_times 未被败方二次回滚（仍为 2；真实并发中胜方那一路才回滚到 1）。修复前败方会把它再减一次=1（双减虚增）。
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode()))
                .get().extracting(IssuedEntitlementRecord::usedTimes).isEqualTo(2);
    }

    @Test
    void verificationConsumesExactlyOneRemainingTimeAndIsIdempotent() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("verify");
        VerificationRequest request = new VerificationRequest(
                "verify-once",
                "qr",
                entitlement.entitlementCode(),
                "dev_1",
                "gate_1",
                Map.of("scenario", "ticket_checkin")
        );

        VerificationResponse first = service.createVerification(request);
        VerificationResponse second = service.createVerification(request);

        assertThat(second).isEqualTo(first);
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode()))
                .get()
                .extracting(IssuedEntitlementRecord::usedTimes)
                .isEqualTo(1);
        assertThat(ticketingRepository.verifications).hasSize(1);
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "verification.succeeded".equals(event.eventName()))
                .hasSize(1);
    }

    @Test
    void createPriceRuleIsIdempotentAndListWritesViewAuditEvent() {
        TicketTypeRecord ticketType = createTicketType("price-rule-ticket");
        TicketPriceRuleRequest request = new TicketPriceRuleRequest(
                "price-rule-once",
                "student fixed",
                List.of(),
                ticketType.id(),
                "student",
                "fixed_price",
                6900L,
                7900L,
                8900L,
                null,
                "miniapp",
                Map.of("student", "verified")
        );

        TicketPriceRuleRecord first = service.createTicketPriceRule(request);
        TicketPriceRuleRecord second = service.createTicketPriceRule(request);

        assertThat(second).isEqualTo(first);
        assertThat(first.currency()).isEqualTo("CNY");
        assertThat(ticketingRepository.priceRules).hasSize(1);

        PageResponse<TicketPriceRuleRecord> page = service.listTicketPriceRules();

        assertThat(page.items()).extracting(TicketPriceRuleRecord::id).contains(first.id());
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_price_variant.changed", "ticket_price_variant.viewed");
    }

    @Test
    void createPriceRuleAcceptsMemberVariantDerivedFromCalendarPrice() {
        TicketTypeRecord ticketType = createTicketType("member-price-rule-ticket");
        TicketPriceRuleRecord rule = service.createTicketPriceRule(new TicketPriceRuleRequest(
                "member-price-rule-once",
                "member discount",
                List.of(),
                ticketType.id(),
                "member",
                "derive_from_calendar_price",
                null,
                null,
                null,
                new BigDecimal("90.00"),
                null,
                Map.of("member_level", "standard")
        ));

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "member-price-order",
                "create order",
                List.of(),
                ticketType.id(),
                rule.id(),
                2,
                LocalDate.parse("2026-06-20"),
                "app",
                "13800138000",
                "customer_member"
        ));

        assertThat(rule)
                .extracting(TicketPriceRuleRecord::variantType, TicketPriceRuleRecord::priceMode)
                .containsExactly("member", "derive_from_calendar_price");
        assertThat(order.amountCent()).isEqualTo(21420L);
    }

    @Test
    void createPriceRuleRejectsUnknownTicketType() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createTicketPriceRule(new TicketPriceRuleRequest(
                        "price-rule-missing-ticket",
                        "missing",
                        List.of(),
                        "ticket_type_missing",
                        "group",
                        "derive_from_calendar_price",
                        null,
                        null,
                        null,
                        new BigDecimal("80.00"),
                        null,
                        Map.of()
                )))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("票种不存在或已被删除");

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_price_variant.change_failed");
    }

    @Test
    void createTicketOrderComputesAmountAndRequiresPositiveQuantity() {
        TicketTypeRecord ticketType = createTicketType("order-ticket");
        TicketPriceRuleRecord rule = service.createTicketPriceRule(new TicketPriceRuleRequest(
                "order-fixed-rule",
                "fixed",
                List.of(),
                ticketType.id(),
                "channel",
                "fixed_price",
                8000L,
                9000L,
                10000L,
                null,
                "web",
                Map.of()
        ));

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "order-create",
                "create order",
                List.of(),
                ticketType.id(),
                rule.id(),
                2,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_1"
        ));

        assertThat(order.amountCent()).isEqualTo(18000L);
        assertThat(order.currency()).isEqualTo("CNY");
        assertThat(order.payStatus()).isEqualTo("unpaid");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_order.created");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createTicketOrder(new TicketOrderRequest(
                        "order-zero",
                        "bad quantity",
                        List.of(),
                        ticketType.id(),
                        rule.id(),
                        0,
                        LocalDate.parse("2026-06-20"),
                        "web",
                        null,
                        "customer_1"
                )))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("quantity 必须大于 0");
    }

    @Test
    void derivedPriceRuleUsesDecimalDiscountRateForCentRounding() {
        assertDerivedPriceRuleAmount("decimal-discount-3333", 9999L, new BigDecimal("33.33"), 3333L);
        assertDerivedPriceRuleAmount("decimal-discount-half-cent", 250L, new BigDecimal("64.60"), 162L);
    }

    @Test
    void payIntentAndIssueOrderAreIdempotent() {
        TicketTypeRecord ticketType = createTicketType("issue-order-ticket");
        EntitlementTemplateRecord template = createTemplate("issue-order-template", 2, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "issue-order-bind",
                "bind",
                List.of(),
                template.id(),
                1,
                2,
                9900L,
                "default",
                1,
                9900L
        ));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "issue-order-create",
                "create",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "admin",
                "13800138000",
                "customer_issue"
        ));

        TicketPaymentIntentRecord intent = service.createTicketOrderPayIntent(order.id(), new PaymentIntentRequest(
                "issue-order-pay",
                "pay",
                List.of(),
                "ticket_order",
                order.id(),
                order.amountCent(),
                "CNY",
                "aggregate_pay"
        ));
        IssuedTicketResponse firstIssue = service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "issue-order-once",
                "issue",
                "paid offline",
                List.of()
        ));
        IssuedTicketResponse secondIssue = service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "issue-order-once",
                "issue",
                "paid offline",
                List.of()
        ));

        assertThat(intent.status()).isEqualTo("created");
        assertThat(firstIssue).isEqualTo(secondIssue);
        assertThat(ticketingRepository.issuedEntitlements).hasSize(1);
        assertThat(ticketingRepository.findTicketOrderById(order.id()))
                .get()
                .extracting(TicketOrderRecord::issueStatus)
                .isEqualTo("issued");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("payment.intent_created", "ticket.issued");
    }

    @Test
    void confirmPaymentWritesBackPaidAndIsIdempotent() {
        TicketTypeRecord ticketType = createTicketType("confirm-pay-ticket");
        EntitlementTemplateRecord template = createTemplate("confirm-pay-template", 1, 5000L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "confirm-pay-bind", "bind", List.of(), template.id(), 1, 1, 5000L, "default", 1, 9900L));
        // 非 admin 渠道下单 → 初始 unpaid。
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "confirm-pay-create", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-20"), "web", "13800138000", "customer_confirm"));
        assertThat(order.payStatus()).isEqualTo("unpaid");
        // V231 资金/时间铁律：未支付订单 paid_at 必须为 null（不得拿下单时间冒充）。
        assertThat(order.paidAt()).isNull();

        // 闭环前置：非 admin 渠道未支付不可出票。
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.issueTicketOrder(order.id(),
                        new StatusChangeRequest("confirm-pay-early", "issue", "early", List.of())))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class);

        TicketPaymentIntentRecord intent = service.createTicketOrderPayIntent(order.id(), new PaymentIntentRequest(
                "confirm-pay-intent", "pay", List.of(), "ticket_order", order.id(), order.amountCent(), "CNY", "aggregate_pay"));

        // 确认支付 → 回写 paid，意图置 succeeded。
        TicketOrderRecord paid = service.confirmTicketOrderPayment(order.id(), new ConfirmTicketOrderPaymentRequest(
                "confirm-pay-do", intent.id(), "gw-ref-1"));
        assertThat(paid.payStatus()).isEqualTo("paid");
        // V231 资金/时间铁律：支付确认写入 paid_at，且 ≈支付时刻、≠下单时间(createdAt)。
        assertThat(paid.paidAt()).isNotNull();
        assertThat(paid.paidAt()).isAfterOrEqualTo(paid.createdAt());
        assertThat(paid.paidAt()).isNotEqualTo(paid.createdAt());
        assertThat(paid.paidAt()).isCloseTo(java.time.Instant.now(),
                org.assertj.core.api.Assertions.within(1, java.time.temporal.ChronoUnit.MINUTES));
        assertThat(ticketingRepository.findTicketPaymentIntentById(intent.id()))
                .get().extracting(TicketPaymentIntentRecord::status).isEqualTo("succeeded");

        // 幂等：再次确认仍 paid、不报错。
        TicketOrderRecord paidAgain = service.confirmTicketOrderPayment(order.id(), new ConfirmTicketOrderPaymentRequest(
                "confirm-pay-do-2", intent.id(), null));
        assertThat(paidAgain.payStatus()).isEqualTo("paid");

        // 已 paid → 非 admin 渠道现在可出票。
        service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "confirm-pay-issue", "issue", "issue after pay", List.of()));
        assertThat(ticketingRepository.issuedEntitlements).isNotEmpty();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_order.payment_confirmed");
    }

    @Test
    void ticketOrderWriteEndpointsRejectCrossMerchantAccess() {
        // 审计 A3：订单写端（创建支付意图/确认支付/出票/取消）补跨商户归属校验。
        // 商户A 受理下单 → 商户B 持自身令牌操作该单四个写端点，均应 CROSS_MERCHANT_FORBIDDEN；商户A 自身不受影响。
        TestSecurityActors.clear();
        try {
            TestSecurityActors.asMerchantUser("mch_a");
            TicketTypeRecord ticketType = createTicketType("xm-ticket");
            EntitlementTemplateRecord template = createTemplate("xm-template", 1, 5000L);
            service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                    "xm-bind", "bind", List.of(), template.id(), 1, 1, 5000L, "default", 1, 9900L));
            TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                    "xm-order", "create", List.of(), ticketType.id(), null, 1,
                    LocalDate.parse("2026-06-20"), "web", "13800138000", "customer_xm"));
            TicketPaymentIntentRecord intentA = service.createTicketOrderPayIntent(order.id(), new PaymentIntentRequest(
                    "xm-intent-a", "pay", List.of(), "ticket_order", order.id(), order.amountCent(), "CNY", "aggregate_pay"));

            // 切到商户B —— 四个写端点都应拒绝。
            TestSecurityActors.asMerchantUser("mch_b");
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createTicketOrderPayIntent(order.id(),
                            new PaymentIntentRequest("xm-intent-b", "pay", List.of(), "ticket_order", order.id(),
                                    order.amountCent(), "CNY", "aggregate_pay")))
                    .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                            ex -> assertThat(ex.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.confirmTicketOrderPayment(order.id(),
                            new ConfirmTicketOrderPaymentRequest("xm-confirm-b", intentA.id(), null)))
                    .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                            ex -> assertThat(ex.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.issueTicketOrder(order.id(),
                            new StatusChangeRequest("xm-issue-b", "issue", "x", List.of())))
                    .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                            ex -> assertThat(ex.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.cancelTicketOrder(order.id(),
                            new StatusChangeRequest("xm-cancel-b", "cancel", "x", List.of())))
                    .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                            ex -> assertThat(ex.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));

            // 商户A 自身确认支付不受影响。
            TestSecurityActors.asMerchantUser("mch_a");
            TicketOrderRecord paid = service.confirmTicketOrderPayment(order.id(),
                    new ConfirmTicketOrderPaymentRequest("xm-confirm-a", intentA.id(), null));
            assertThat(paid.payStatus()).isEqualTo("paid");
        } finally {
            TestSecurityActors.clear();
        }
    }

    @Test
    void cancelUnpaidOrderAndExpiryJobReclaimWhilePaidOrderCannotBeCancelled() {
        TicketTypeRecord ticketType = createTicketType("cancel-ticket");
        EntitlementTemplateRecord template = createTemplate("cancel-template", 1, 5000L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "cancel-bind", "bind", List.of(), template.id(), 1, 1, 5000L, "default", 1, 9900L));

        // (1) 手动取消未付订单 → cancelled；幂等；取消后不可确认支付（防超卖）。
        TicketOrderRecord order1 = service.createTicketOrder(new TicketOrderRequest(
                "cancel-o1", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-20"), "web", "13800138000", "c1"));
        TicketPaymentIntentRecord intent1 = service.createTicketOrderPayIntent(order1.id(), new PaymentIntentRequest(
                "cancel-o1-pay", "pay", List.of(), "ticket_order", order1.id(), order1.amountCent(), "CNY", "aggregate_pay"));
        TicketOrderRecord cancelled = service.cancelTicketOrder(order1.id(), new StatusChangeRequest(
                "cancel-o1-do", "cancel", "user cancel", List.of()));
        assertThat(cancelled.status()).isEqualTo("cancelled");
        assertThat(service.cancelTicketOrder(order1.id(), new StatusChangeRequest(
                "cancel-o1-do2", "cancel", "again", List.of())).status()).isEqualTo("cancelled");
        assertThatThrownBy(() -> service.createTicketOrderPayIntent(order1.id(), new PaymentIntentRequest(
                "cancel-o1-new-pay", "pay", List.of(), "ticket_order", order1.id(), order1.amountCent(), "CNY", "aggregate_pay")))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("ORDER_CANCELLED"));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.confirmTicketOrderPayment(order1.id(),
                        new ConfirmTicketOrderPaymentRequest("cancel-o1-confirm", intent1.id(), null)))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class);

        // (2) 已付订单不可取消。
        TicketOrderRecord order2 = service.createTicketOrder(new TicketOrderRequest(
                "cancel-o2", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-20"), "web", "13800138000", "c2"));
        TicketPaymentIntentRecord intent2 = service.createTicketOrderPayIntent(order2.id(), new PaymentIntentRequest(
                "cancel-o2-pay", "pay", List.of(), "ticket_order", order2.id(), order2.amountCent(), "CNY", "aggregate_pay"));
        service.confirmTicketOrderPayment(order2.id(), new ConfirmTicketOrderPaymentRequest("cancel-o2-confirm", intent2.id(), null));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.cancelTicketOrder(order2.id(),
                        new StatusChangeRequest("cancel-o2-do", "cancel", "x", List.of())))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class);

        // (3) 超时任务取消未付订单（cutoff 取未来 → 命中刚下的未付单）。
        TicketOrderRecord order3 = service.createTicketOrder(new TicketOrderRequest(
                "cancel-o3", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-20"), "web", "13800138000", "c3"));
        int released = service.cancelExpiredUnpaidTicketOrders(java.time.Instant.now().plusSeconds(60));
        assertThat(released).isGreaterThanOrEqualTo(1);
        assertThat(ticketingRepository.findTicketOrderById(order3.id()))
                .get().extracting(TicketOrderRecord::status).isEqualTo("cancelled");
    }

    @Test
    void publishListIssuedAndCredentialRefreshUseShortLivedCredential() {
        TicketTypeRecord ticketType = createTicketType("credential-ticket");
        // AUD-015 发布闸要求≥1个权益绑定。
        EntitlementTemplateRecord credentialTpl = createTemplate("credential-gate-tpl", 2, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "credential-gate-bind", "bind", List.of(), credentialTpl.id(), 1, 2, 9900L, "default", 1, 9900L));
        TicketTypeRecord published = service.publishTicketType(ticketType.id(), new StatusChangeRequest(
                "publish-ticket",
                "publish",
                "ready",
                List.of()
        ));
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("credential");

        TicketCredentialResponse credential = service.getTicketCredential(entitlement.id());

        assertThat(published.status()).isEqualTo("published");
        assertThat(service.listIssuedEntitlements().items())
                .extracting(IssuedEntitlementRecord::id)
                .contains(entitlement.id());
        assertThat(credential.credentialToken()).startsWith("cred_");
        assertThat(credential.expiresAt()).isAfter(credential.issuedAt());
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_price.published", "ticket_issued_entitlement.viewed", "ticket_credential.refreshed");
    }

    @Test
    void credentialPayloadCanBeVerifiedAndUnknownCredentialFails() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("credential-verify");
        TicketCredentialResponse credential = service.getTicketCredential(entitlement.id());

        VerificationPreviewResponse preview = service.previewVerification(new VerificationRequest(
                "credential-preview",
                "dynamic_qr",
                credential.credentialToken(),
                "dev_1",
                "gate_1",
                Map.of()
        ));

        assertThat(preview.allowed()).isTrue();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.previewVerification(new VerificationRequest(
                        "credential-preview-missing",
                        "dynamic_qr",
                        "cred_missing",
                        "dev_1",
                        "gate_1",
                        Map.of()
                )))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("核销凭证不存在或已失效");
    }

    @Test
    void dynamicCredentialCannotBeReplayedAfterSuccessfulVerification() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("credential-replay");
        TicketCredentialResponse credential = service.getTicketCredential(entitlement.id());

        service.createVerification(new VerificationRequest(
                "credential-replay-first",
                "dynamic_qr",
                credential.credentialToken(),
                "dev_1",
                "gate_1",
                Map.of()
        ));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createVerification(new VerificationRequest(
                        "credential-replay-second",
                        "dynamic_qr",
                        credential.credentialToken(),
                        "dev_1",
                        "gate_1",
                        Map.of()
                )))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("核销凭证已过期，请刷新后重试");
    }

    // T4：刷新窗口与票种级 refresh_interval_seconds 联动（替换硬编码 300s）。
    @Test
    void credentialRefreshExpiryUsesRefreshIntervalNotHardcoded300s() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("credential-window");
        TicketCredentialResponse credential = service.getTicketCredential(entitlement.id());

        long windowSeconds = java.time.Duration.between(credential.issuedAt(), credential.expiresAt()).getSeconds();
        TicketCredentialRecord stored = ticketingRepository.findTicketCredentialByToken(credential.credentialToken()).orElseThrow();

        assertThat(windowSeconds).isEqualTo(stored.refreshIntervalSeconds());
        assertThat(windowSeconds).isNotEqualTo(300L);
        assertThat(stored.windowStartedAt()).isEqualTo(credential.issuedAt());
        assertThat(stored.signatureValue()).isNull();
        assertThat(stored.signatureVersion()).isNull();
    }

    // #172(2026-07-19 拍板)：刷新窗口读全场统一配置，不再按票种——票种配 30 也被忽略，凭证读全场配置(此处 20)。
    @Test
    void credentialRefreshUsesUnifiedConfigNotTicketType() {
        Instant now = Instant.now();
        ticketingRepository.qrRefreshIntervalSeconds = 20; // 全场统一配置=20（票种仍带 30，应被忽略）
        ticketingRepository.saveTicketType(new TicketTypeRecord(
                "tt_ri30", "RI30 Pass", "weekday_weekend_holiday", 9900L, 11900L, 12900L,
                false, "published", null, 30, now, now, null, 0, null, false, null, TicketTypeRecord.NO_COMBO_DISCOUNT));
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_ri30", "Entry RI30", "entry", 1, 9900L, "active", null, null, null, null, now, now,
                null, null, null, null));
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_ri30", "tt_ri30", "tpl_ri30", 1, 1, 9900L, "default", 1, null, now, now));
        issueDirectlyForTest("tt_ri30", new TicketIssueRequest(
                "ri30-issue", "issue", List.of(), "order_ri30", "manual", List.of("qr")));
        IssuedEntitlementRecord issued = ticketingRepository.issuedEntitlements.values().stream()
                .filter(e -> "order_ri30".equals(e.orderId())).findFirst().orElseThrow();

        TicketCredentialResponse credential = service.getTicketCredential(issued.id());
        long windowSeconds = java.time.Duration.between(credential.issuedAt(), credential.expiresAt()).getSeconds();
        assertThat(windowSeconds).isEqualTo(20L);
        assertThat(ticketingRepository.findTicketCredentialByToken(credential.credentialToken()).orElseThrow()
                .refreshIntervalSeconds()).isEqualTo(20);
    }

    // P1 清单#19：动态二维码过刷新窗口（截图重放旧 token）→ window_valid=false、result_reason=window_expired、不消费权益。
    @Test
    void verificationPastRefreshWindowMarksWindowExpiredAndDoesNotConsume() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("window-expired");
        TicketCredentialResponse credential = service.getTicketCredential(entitlement.id());
        TicketCredentialRecord stored = ticketingRepository.findTicketCredentialByToken(credential.credentialToken()).orElseThrow();
        // 把窗口起点推到过去，使 window_started_at + refresh_interval 落在当前之前（模拟离线截图重放过窗口旧 token）。
        Instant past = Instant.now().minusSeconds(stored.refreshIntervalSeconds() + 60L);
        ticketingRepository.saveTicketCredential(new TicketCredentialRecord(
                stored.id(), stored.entitlementId(), stored.orderId(), stored.credentialToken(), stored.nonce(),
                stored.credentialHash(), stored.credentialType(), past, past.plusSeconds(stored.refreshIntervalSeconds()),
                "active", stored.refreshedCount(), past, past, stored.refreshIntervalSeconds(), past,
                stored.signatureValue(), stored.signatureVersion()));

        VerificationResponse response = service.createVerification(new VerificationRequest(
                "window-expired-once", "dynamic_qr", credential.credentialToken(), "dev_1", "gate_1", Map.of()));

        assertThat(response.status()).isEqualTo("failed");
        assertThat(response.resultReason()).isEqualTo("window_expired");
        // 过窗口不消费权益：used_times 保持 0。
        assertThat(ticketingRepository.findIssuedEntitlementById(entitlement.id()).orElseThrow().usedTimes()).isZero();
        // 核销记录 window_valid=false 留痕。
        assertThat(ticketingRepository.verifications.values())
                .filteredOn(v -> "failed".equals(v.status()))
                .singleElement()
                .satisfies(v -> {
                    assertThat(v.windowValid()).isFalse();
                    assertThat(v.resultReason()).isEqualTo("window_expired");
                });
    }

    // P1 清单#19：核销冲正 → 发 VerificationReversedEvent（退款/结算冲正联动对接点）。
    @Test
    void reverseVerificationPublishesVerificationReversedEvent() {
        IssuedEntitlementRecord entitlement = issueOrderEntitlement("reverse-event");
        VerificationResponse success = service.createVerification(new VerificationRequest(
                "reverse-event-1", "qr", entitlement.entitlementCode(), "dev_1", "gate_1", Map.of()));
        publishedEvents.clear();

        service.reverseVerification(
                success.id(),
                new com.complex.platform.ticketing.dto.VerificationReverseRequest("reverse-event-rev", "误核销"),
                "supervisor_1");

        assertThat(publishedEvents)
                .filteredOn(e -> e instanceof VerificationReversedEvent)
                .singleElement()
                .satisfies(e -> {
                    VerificationReversedEvent event = (VerificationReversedEvent) e;
                    assertThat(event.verificationId()).isEqualTo(success.id());
                    assertThat(event.entitlementId()).isEqualTo(entitlement.id());
                    assertThat(event.orderId()).isEqualTo(entitlement.orderId());
                });
    }

    // T4：成功核销留 nonce/merchant_id/window_valid=true 事后核对维度，signature_valid 本期恒 null。
    @Test
    void successfulVerificationRecordsNonceMerchantAndWindowValid() {
        TestSecurityActors.asMerchantUser("mch_window_test");
        try {
            IssuedEntitlementRecord entitlement = issueSingleEntitlement("credential-window-dims");
            TicketCredentialResponse credential = service.getTicketCredential(entitlement.id());

            service.createVerification(new VerificationRequest(
                    "credential-window-dims-once",
                    "dynamic_qr",
                    credential.credentialToken(),
                    "dev_1",
                    "gate_1",
                    Map.of()
            ));

            assertThat(ticketingRepository.verifications.values())
                    .filteredOn(v -> "success".equals(v.status()))
                    .singleElement()
                    .satisfies(v -> {
                        assertThat(v.nonce()).isNotBlank();
                        assertThat(v.merchantId()).isEqualTo("mch_window_test");
                        assertThat(v.windowValid()).isTrue();
                        assertThat(v.signatureValid()).isNull();
                    });
        } finally {
            TestSecurityActors.clear();
        }
    }

    @Test
    void dynamicCredentialVerificationStoresHashedPayloadOnly() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("credential-payload-hash");
        TicketCredentialResponse credential = service.getTicketCredential(entitlement.id());

        service.createVerification(new VerificationRequest(
                "credential-payload-hash-once",
                "dynamic_qr",
                credential.credentialToken(),
                "dev_1",
                "gate_1",
                Map.of()
        ));

        assertThat(ticketingRepository.listVerifications())
                .extracting(VerificationRecord::credentialPayload)
                .allSatisfy(payload -> assertThat(payload)
                        .startsWith("sha256:")
                        .doesNotContain(credential.credentialToken()));
    }

    @Test
    void expiredFullyUnusedPaidTicketIsForfeitedNotRefunded() {
        java.time.Instant past = java.time.Instant.parse("2020-01-01T00:00:00Z");
        java.time.Instant now = java.time.Instant.parse("2026-06-22T00:00:00Z");
        // 可退票种 + 已付订单（9900 分，1 张）+ 1 张全未核销的父票实例（到期）。
        seedRefundableType("tt_auto");
        ticketingRepository.ticketOrders.put("o_auto", new TicketOrderRecord(
                "o_auto", "ON-AUTO", "c1", "tt_auto", null, 1, LocalDate.parse("2026-06-20"),
                "web", "13800138000", 9900L, null, 9900L, 0L, "CNY", "paid", "issued", "paid", past, past, null, null, null));
        seedInstance("inst_auto", "o_auto", "tt_auto", 1, 1, 1, "active", null);
        ticketingRepository.issuedEntitlements.put("e_auto",
                seedEntitlement("e_auto", "o_auto", "tt_auto", 1, 0, "active", past, "inst_auto", 1));

        int processed = service.processExpiredPaidEntitlements(now);

        assertThat(processed).isEqualTo(1);
        // business-enum-quick-reference-v0.1.html「售票与退款原则」：门票售出后原则上不退款，
        // 未到场、忘记使用或主动放弃都不是退款理由；「到期未核销款」：到期票余额按票种经营域归属。
        // 故即便票种 refundable=true（=客人可申请整票退款），到期也只作废、不产生退款单、不回补名额。
        assertThat(ticketingRepository.refunds.values()).noneMatch(r -> "o_auto".equals(r.targetId()));
        assertThat(ticketingRepository.issuedEntitlements.get("e_auto").status()).isEqualTo("forfeited");
        assertThat(ticketingRepository.findTicketInstanceById("inst_auto")).get()
                .extracting(TicketInstanceRecord::status).isEqualTo("active");
        assertThat(ticketingRepository.releasedStock).doesNotContain("tt_auto:1");
    }

    @Test
    void refundBecomesSuccessOnlyAfterPayoutConfirmed() {
        // 依据 business-enum-quick-reference-v0.1.html「退款单状态」：退款以“成功”为最终完成态，
        // 只有“成功”的退款才计入“已退金额”；“已通过”只表示已批准并已发起不可逆兑付。
        IssuedEntitlementRecord entitlement = issueOrderEntitlement("payout");
        TicketOrderRecord order = ticketingRepository.findTicketOrderById(entitlement.orderId()).orElseThrow();

        TicketRefundRecord refund = service.requestTicketRefund(refundRequest(
                "payout-request", "refund", List.of(), order.id(), order.amountCent(), "customer request", "original"));
        TicketRefundRecord approved = service.approveTicketRefund(refund.id(), new StatusChangeRequest(
                "payout-approve", "approve", "approved", List.of()));

        // 审批通过只到“已通过”：钱还没打出去，经营净收入不得先被扣。
        assertThat(approved.status()).isEqualTo("approved");
        assertThat(ticketingRepository.findTicketRefundById(refund.id())).get()
                .extracting(TicketRefundRecord::status).isEqualTo("approved");

        // 财务实际打款后确认拨付 → success 终态（reason 即“原路退回结果”留痕）。
        TicketRefundRecord confirmed = service.confirmTicketRefundPayout(refund.id(), new StatusChangeRequest(
                "payout-confirm", "confirm_payout", "原路退回成功，支付流水 TESTPAY-1", List.of()));
        assertThat(confirmed.status()).isEqualTo("success");
        assertThat(ticketingRepository.findTicketRefundById(refund.id())).get()
                .extracting(TicketRefundRecord::status).isEqualTo("success");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_refund.payout_confirmed");

        // 防重复拨付：已 success 再确认 → CAS 命中 0 行 → 拒。
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.confirmTicketRefundPayout(refund.id(),
                        new StatusChangeRequest("payout-confirm-again", "confirm_payout", "重复确认", List.of())))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class, ex ->
                        assertThat(ex.code()).isEqualTo("TICKET_REFUND_NOT_PAYOUT_CONFIRMABLE"));
    }

    @Test
    void expiredNonRefundableTicketIsForfeitedNotRefunded() {
        // 规则2：不可退票（refundable=false 默认）到期未使用 → 直接作废不退，不产生退款单。
        java.time.Instant past = java.time.Instant.parse("2020-01-01T00:00:00Z");
        java.time.Instant now = java.time.Instant.parse("2026-06-22T00:00:00Z");
        seedTicketType("tt_nr", false, null);
        ticketingRepository.ticketOrders.put("o_nr", new TicketOrderRecord(
                "o_nr", "ON-NR", "c1", "tt_nr", null, 1, LocalDate.parse("2026-06-20"),
                "web", "13800138000", 9900L, null, 9900L, 0L, "CNY", "paid", "issued", "paid", past, past, null, null, null));
        seedInstance("inst_nr", "o_nr", "tt_nr", 1, 1, 1, "active", null);
        ticketingRepository.issuedEntitlements.put("e_nr",
                seedEntitlement("e_nr", "o_nr", "tt_nr", 1, 0, "active", past, "inst_nr", 1));

        int processed = service.processExpiredPaidEntitlements(now);

        assertThat(processed).isEqualTo(1);
        assertThat(ticketingRepository.refunds.values()).noneMatch(r -> "o_nr".equals(r.targetId()));
        assertThat(ticketingRepository.issuedEntitlements.get("e_nr").status()).isEqualTo("forfeited");
        assertThat(ticketingRepository.releasedStock).isEmpty();
    }

    @Test
    void expiredPartiallyUsedMultiPersonTicketIsForfeitedNotRefunded() {
        // 到期整票口径（business-enum「售票与退款原则」）：到期一律作废、不产生退款单——
        // 无论整票已首用（多人票核销过 1 人）还是完全未核销，都只作废，已核销权益不动。
        java.time.Instant past = java.time.Instant.parse("2020-01-01T00:00:00Z");
        java.time.Instant now = java.time.Instant.parse("2026-06-22T00:00:00Z");
        seedRefundableType("tt_mp");
        // 两张票（各 9900）：inst_part=四人票已核销 1 人（首用），inst_free=完全未核销。
        ticketingRepository.ticketOrders.put("o_mp", new TicketOrderRecord(
                "o_mp", "ON-MP", "c1", "tt_mp", null, 2, LocalDate.parse("2026-06-20"),
                "web", "13800138000", 19800L, null, 19800L, 0L, "CNY", "paid", "issued", "paid", past, past, null, null, null));
        seedInstance("inst_part", "o_mp", "tt_mp", 1, 2, 1, "active", past); // firstUsedAt 非空=已首用
        ticketingRepository.issuedEntitlements.put("e_part_used",
                seedEntitlement("e_part_used", "o_mp", "tt_mp", 1, 1, "active", past, "inst_part", 1));
        ticketingRepository.issuedEntitlements.put("e_part_free",
                seedEntitlement("e_part_free", "o_mp", "tt_mp", 1, 0, "active", past, "inst_part", 2));
        seedInstance("inst_free", "o_mp", "tt_mp", 2, 1, 1, "active", null);
        ticketingRepository.issuedEntitlements.put("e_free",
                seedEntitlement("e_free", "o_mp", "tt_mp", 1, 0, "active", past, "inst_free", 1));

        service.processExpiredPaidEntitlements(now);

        // 完全未核销票 inst_free → 权益作废；父票实例不动（作废只落 issued_entitlements）。
        assertThat(ticketingRepository.issuedEntitlements.get("e_free").status()).isEqualTo("forfeited");
        assertThat(ticketingRepository.findTicketInstanceById("inst_free")).get()
                .extracting(TicketInstanceRecord::status).isEqualTo("active");
        // 部分使用票 inst_part 的到期未核销权益 → 作废不退；已核销权益不动。
        assertThat(ticketingRepository.issuedEntitlements.get("e_part_free").status()).isEqualTo("forfeited");
        assertThat(ticketingRepository.issuedEntitlements.get("e_part_used").status()).isEqualTo("active");
        // 全程不产生任何退款单。
        assertThat(ticketingRepository.refunds.values()).noneMatch(r -> "o_mp".equals(r.targetId()));
    }

    @Test
    void wholeTicketRefundOnlyRefundsSelectedInstanceOthersStayUsable() {
        // 同单两张票：只退其一（整张），另一张仍 active 可用；退成功放回 1 个名额。
        java.time.Instant now = java.time.Instant.parse("2026-06-22T00:00:00Z");
        seedRefundableType("tt_sel");
        ticketingRepository.ticketOrders.put("o_sel", new TicketOrderRecord(
                "o_sel", "ON-SEL", "c1", "tt_sel", null, 2, LocalDate.parse("2026-06-20"),
                "web", "13800138000", 19800L, null, 19800L, 0L, "CNY", "paid", "issued", "paid", now, now, null, null, null));
        seedInstance("inst_x", "o_sel", "tt_sel", 1, 1, 1, "active", null);
        ticketingRepository.issuedEntitlements.put("e_x",
                seedEntitlement("e_x", "o_sel", "tt_sel", 1, 0, "active", now, "inst_x", 1));
        seedInstance("inst_y", "o_sel", "tt_sel", 2, 1, 1, "active", null);
        ticketingRepository.issuedEntitlements.put("e_y",
                seedEntitlement("e_y", "o_sel", "tt_sel", 1, 0, "active", now, "inst_y", 1));

        TicketRefundRecord refund = service.requestTicketRefund(refundOfInstances(
                "sel-refund", "refund", List.of(), "o_sel", null, "customer request", "original", List.of("inst_x")));
        service.approveTicketRefund(refund.id(), new StatusChangeRequest("sel-approve", "approve", "approved", List.of()));

        assertThat(refund.refundAmountCent()).isEqualTo(9900L);
        // 只退的那张：权益冻结、父票 refunded；另一张 active、权益 active 可继续核销。
        assertThat(ticketingRepository.issuedEntitlements.get("e_x").status()).isEqualTo("refunded");
        assertThat(ticketingRepository.findTicketInstanceById("inst_x")).get()
                .extracting(TicketInstanceRecord::status).isEqualTo("refunded");
        assertThat(ticketingRepository.issuedEntitlements.get("e_y").status()).isEqualTo("active");
        assertThat(ticketingRepository.findTicketInstanceById("inst_y")).get()
                .extracting(TicketInstanceRecord::status).isEqualTo("active");
        // 退一张放一个名额。
        assertThat(ticketingRepository.releasedStock).containsExactly("tt_sel:1");
    }

    @Test
    void wholeTicketRefundRejectsVerifiedInstance() {
        // 整票连坐：父票任一权益已核销 → 整张不可退（选中它 → TICKET_INSTANCE_VERIFIED_NO_REFUND）。
        java.time.Instant now = java.time.Instant.parse("2026-06-22T00:00:00Z");
        seedRefundableType("tt_v");
        ticketingRepository.ticketOrders.put("o_v", new TicketOrderRecord(
                "o_v", "ON-V", "c1", "tt_v", null, 1, LocalDate.parse("2026-06-20"),
                "web", "13800138000", 9900L, null, 9900L, 0L, "CNY", "paid", "issued", "paid", now, now, null, null, null));
        seedInstance("inst_v", "o_v", "tt_v", 1, 2, 1, "active", null);
        ticketingRepository.issuedEntitlements.put("e_v_used",
                seedEntitlement("e_v_used", "o_v", "tt_v", 1, 1, "active", now, "inst_v", 1)); // 一个人头已核销
        ticketingRepository.issuedEntitlements.put("e_v_free",
                seedEntitlement("e_v_free", "o_v", "tt_v", 1, 0, "active", now, "inst_v", 2));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.requestTicketRefund(
                refundOfInstances(
                        "v-refund", "refund", List.of(), "o_v", null, "customer request", "original", List.of("inst_v"))))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class, ex ->
                        assertThat(ex.code()).isEqualTo("TICKET_INSTANCE_VERIFIED_NO_REFUND"));
    }

    @Test
    void wholeTicketRefundRejectsPartialAmountAndEntitlementIds() {
        // 金额服务端算定：传 refund_amount_cent≠整票额 → REFUND_AMOUNT_MISMATCH；传 entitlement_ids → REFUND_UNIT_IS_TICKET。
        java.time.Instant now = java.time.Instant.parse("2026-06-22T00:00:00Z");
        seedRefundableType("tt_m");
        ticketingRepository.ticketOrders.put("o_m", new TicketOrderRecord(
                "o_m", "ON-M", "c1", "tt_m", null, 1, LocalDate.parse("2026-06-20"),
                "web", "13800138000", 9900L, null, 9900L, 0L, "CNY", "paid", "issued", "paid", now, now, null, null, null));
        seedInstance("inst_m", "o_m", "tt_m", 1, 1, 1, "active", null);
        ticketingRepository.issuedEntitlements.put("e_m",
                seedEntitlement("e_m", "o_m", "tt_m", 1, 0, "active", now, "inst_m", 1));

        // 自由/部分金额（5000≠9900）→ 拒。
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.requestTicketRefund(
                refundOfInstances(
                        "m-refund", "refund", List.of(), "o_m", 5000L, "partial", "original", List.of("inst_m"))))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class, ex ->
                        assertThat(ex.code()).isEqualTo("REFUND_AMOUNT_MISMATCH"));
        // 旧入参 entitlement_ids → 拒。
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.requestTicketRefund(new TicketRefundRequest(
                        "m-refund2", "refund", List.of(), "o_m", null, "old", "original", List.of("e_m"), null,
                        "ticket_type_rule", REFUND_ENTITY)))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class, ex ->
                        assertThat(ex.code()).isEqualTo("REFUND_UNIT_IS_TICKET"));
    }

    @Test
    void wholeTicketRefundRejectsDuplicateInstanceId() {
        java.time.Instant now = java.time.Instant.parse("2026-06-22T00:00:00Z");
        seedRefundableType("tt_d");
        ticketingRepository.ticketOrders.put("o_d", new TicketOrderRecord(
                "o_d", "ON-D", "c1", "tt_d", null, 1, LocalDate.parse("2026-06-20"),
                "web", "13800138000", 9900L, null, 9900L, 0L, "CNY", "paid", "issued", "paid", now, now, null, null, null));
        seedInstance("inst_d", "o_d", "tt_d", 1, 1, 1, "active", null);
        ticketingRepository.issuedEntitlements.put("e_d",
                seedEntitlement("e_d", "o_d", "tt_d", 1, 0, "active", now, "inst_d", 1));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.requestTicketRefund(
                refundOfInstances(
                        "d-refund", "refund", List.of(), "o_d", null, "dup", "original", List.of("inst_d", "inst_d"))))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class, ex ->
                        assertThat(ex.getMessage().toLowerCase()).contains("重复"));
    }

    @Test
    void refundRequestRejectedForNonRefundableTicketType() {
        // 规则2：不可退票种（refundable=false）→ 客人不能申请退款。
        java.time.Instant now = java.time.Instant.parse("2026-06-22T00:00:00Z");
        seedTicketType("tt_nrq", false, null);
        ticketingRepository.ticketOrders.put("o_nrq", new TicketOrderRecord(
                "o_nrq", "ON-NRQ", "c1", "tt_nrq", null, 1, LocalDate.parse("2026-06-20"),
                "web", "13800138000", 9900L, null, 9900L, 0L, "CNY", "paid", "issued", "paid", now, now, null, null, null));
        seedInstance("inst_nrq", "o_nrq", "tt_nrq", 1, 1, 1, "active", null);
        ticketingRepository.issuedEntitlements.put("e_nrq",
                seedEntitlement("e_nrq", "o_nrq", "tt_nrq", 1, 0, "active", now, "inst_nrq", 1));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.requestTicketRefund(
                refundOfInstances(
                        "nrq-refund", "refund", List.of(), "o_nrq", null, "customer request", "original", List.of("inst_nrq"))))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class, ex ->
                        assertThat(ex.code()).isEqualTo("TICKET_TYPE_NOT_REFUNDABLE"));
    }

    @Test
    void approvalRolledBackWhenTicketVerifiedInRequestApproveWindow() {
        // 方案2 强 CAS 竞态闸：申请→审批空窗内票被核销（used_times 0→1）→ 审批时冻结命中 0 行 → 整笔拒（回滚），杜绝钱退票也用。
        java.time.Instant now = java.time.Instant.parse("2026-06-22T00:00:00Z");
        seedRefundableType("tt_race");
        ticketingRepository.ticketOrders.put("o_race", new TicketOrderRecord(
                "o_race", "ON-RACE", "c1", "tt_race", null, 1, LocalDate.parse("2026-06-20"),
                "web", "13800138000", 9900L, null, 9900L, 0L, "CNY", "paid", "issued", "paid", now, now, null, null, null));
        seedInstance("inst_race", "o_race", "tt_race", 1, 1, 1, "active", null);
        ticketingRepository.issuedEntitlements.put("e_race",
                seedEntitlement("e_race", "o_race", "tt_race", 1, 0, "active", now, "inst_race", 1));

        TicketRefundRecord refund = service.requestTicketRefund(refundRequest(
                "race-refund", "refund", List.of(), "o_race", null, "customer request", "original"));
        // 空窗内票被核销（模拟游乐 gateway 直连绕过父票守卫）。
        ticketingRepository.issuedEntitlements.put("e_race",
                ticketingRepository.issuedEntitlements.get("e_race").useOnce(now));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.approveTicketRefund(refund.id(),
                        new StatusChangeRequest("race-approve", "approve", "approved", List.of())))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class, ex ->
                        assertThat(ex.code()).isEqualTo("TICKET_INSTANCE_VERIFIED_NO_REFUND"));
    }

    @Test
    void refundRequestAndApprovalDoNotDeleteIssuedHistory() {
        IssuedEntitlementRecord entitlement = issueOrderEntitlement("refund");
        TicketOrderRecord order = ticketingRepository.findTicketOrderById(entitlement.orderId()).orElseThrow();

        TicketRefundRecord refund = service.requestTicketRefund(refundRequest(
                "refund-request",
                "refund",
                List.of(),
                order.id(),
                order.amountCent(),
                "customer request",
                "original"
        ));
        TicketRefundRecord approved = service.approveTicketRefund(refund.id(), new StatusChangeRequest(
                "refund-approve",
                "approve",
                "approved",
                List.of()
        ));

        // business-enum「退款单状态」：审批通过=“已通过”（已批准并已发起兑付），钱未打出去前不得记“成功”。
        assertThat(approved.status()).isEqualTo("approved");
        assertThat(ticketingRepository.findIssuedEntitlementByCode(entitlement.entitlementCode())).isPresent();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_refund.requested", "ticket_refund.approved");

        // 唯一的票已退（父票 refunded）→ 再申请退款无可退票，整笔拒。
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.requestTicketRefund(refundRequest(
                        "refund-again",
                        "refund",
                        List.of(),
                        order.id(),
                        null,
                        "again",
                        "original"
                )))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class, ex ->
                        assertThat(ex.code()).isEqualTo("TICKET_INSTANCE_VERIFIED_NO_REFUND"));
    }

    @Test
    void refundRejectedAfterAnyEntitlementVerified() {
        // 方案2：核销一张则整票不可退（仅全未核销可退）。
        IssuedEntitlementRecord entitlement = issueOrderEntitlement("refund-verified");
        TicketOrderRecord order = ticketingRepository.findTicketOrderById(entitlement.orderId()).orElseThrow();
        // 标记该订单的权益已核销一张（usedTimes 0→1）。
        ticketingRepository.saveIssuedEntitlement(entitlement.useOnce(Instant.now()));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.requestTicketRefund(refundRequest(
                        "refund-after-verify",
                        "refund",
                        List.of(),
                        order.id(),
                        null,
                        "customer request",
                        "original"
                )))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class, exception ->
                        // 该单仅一张票且已核销 → 无可退，整票拒退。
                        assertThat(exception.code()).isEqualTo("TICKET_INSTANCE_VERIFIED_NO_REFUND"));
    }

    @Test
    void approveTicketRefundRejectsAlreadyApprovedRefund() {
        IssuedEntitlementRecord entitlement = issueOrderEntitlement("refund-reapprove");
        TicketOrderRecord order = ticketingRepository.findTicketOrderById(entitlement.orderId()).orElseThrow();

        TicketRefundRecord refund = service.requestTicketRefund(refundRequest(
                "refund-reapprove-request",
                "refund",
                List.of(),
                order.id(),
                order.amountCent(),
                "customer request",
                "original"
        ));
        service.approveTicketRefund(refund.id(), new StatusChangeRequest(
                "refund-reapprove-1", "approve", "approved", List.of()));

        // A second approval (fresh idempotency key, so not an idempotent replay) must be rejected
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.approveTicketRefund(
                        refund.id(),
                        new StatusChangeRequest("refund-reapprove-2", "approve", "approved", List.of())))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .extracting("code")
                .isEqualTo("TICKET_REFUND_NOT_APPROVABLE");
    }

    @Test
    void approvedRefundFreezesEntitlementSoItCannotBeVerified() {
        TicketTypeRecord ticketType = createTicketType("refund-freeze-ticket");
        EntitlementTemplateRecord template = createTemplate("refund-freeze-template", 1, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "refund-freeze-bind", "bind", List.of(), template.id(), 1, 1, 9900L, "default", 1, 9900L));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "refund-freeze-order", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-15"), "admin", "13800138000", "customer_refund_freeze"));
        service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "refund-freeze-issue", "issue", "issue", List.of()));
        IssuedEntitlementRecord ent = ticketingRepository.listIssuedEntitlementsByOrderId(order.id()).get(0);
        assertThat(ent.status()).isEqualTo("active");

        TicketRefundRecord refund = service.requestTicketRefund(refundRequest(
                "refund-freeze-request", "refund", List.of(), order.id(), order.amountCent(), "customer request", "original"));
        service.approveTicketRefund(refund.id(), new StatusChangeRequest(
                "refund-freeze-approve", "approve", "approved", List.of()));

        // 退款审批生效 → 权益冻结为 refunded。
        assertThat(ticketingRepository.findIssuedEntitlementById(ent.id()))
                .get().extracting(IssuedEntitlementRecord::status).isEqualTo("refunded");
        // 冻结后核销被拒（直连 atomicUseEntitlement，覆盖游乐 gateway 无 service 状态检查的路径）。
        assertThat(ticketingRepository.atomicUseEntitlement(ent.id(), java.time.Instant.now())).isEmpty();
    }

    @Test
    void fullRefundApprovalReleasesTicketStock() {
        // 票务退款打款：全额退款审批 → 回补该票种库存（限量名额返还，与下单即扣对称）。
        TicketTypeRecord ticketType = createTicketType("refund-stock-ticket");
        EntitlementTemplateRecord template = createTemplate("refund-stock-template", 1, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "refund-stock-bind", "bind", List.of(), template.id(), 1, 1, 9900L, "default", 1, 9900L));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "refund-stock-order", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-15"), "admin", "13800138000", "customer_refund_stock"));
        service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "refund-stock-issue", "issue", "issue", List.of()));
        TicketRefundRecord refund = service.requestTicketRefund(refundRequest(
                "refund-stock-request", "refund", List.of(), order.id(), order.amountCent(), "customer request", "original"));

        service.approveTicketRefund(refund.id(), new StatusChangeRequest(
                "refund-stock-approve", "approve", "approved", List.of()));

        assertThat(ticketingRepository.releasedStock).contains(ticketType.id() + ":" + order.quantity());
    }

    @Test
    void partialAmountRefundIsRejected() {
        // 方案2：金额服务端算定，客户端传部分金额（≠整票额）→ REFUND_AMOUNT_MISMATCH，不产生退款、不回补库存。
        TicketTypeRecord ticketType = createTicketType("refund-partial-ticket");
        EntitlementTemplateRecord template = createTemplate("refund-partial-template", 1, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "refund-partial-bind", "bind", List.of(), template.id(), 1, 1, 9900L, "default", 1, 9900L));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "refund-partial-order", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-15"), "admin", "13800138000", "customer_refund_partial"));
        service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "refund-partial-issue", "issue", "issue", List.of()));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.requestTicketRefund(refundRequest(
                        "refund-partial-request", "refund", List.of(), order.id(), order.amountCent() / 2, "partial", "original")))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class, ex ->
                        assertThat(ex.code()).isEqualTo("REFUND_AMOUNT_MISMATCH"));
        assertThat(ticketingRepository.releasedStock).isEmpty();
    }

    @Test
    void tieredAuthorizationSelfCompletesWithinLimitAndEnforcesSoDOverLimit() {
        // Stage B（审计 A2）：开启分级授权后——额度内退款发起人当场自助自动退（审批人=发起人本人、权益冻结）；
        //   超额退款留待复核且 SoD：发起人不得自审自批，须由不同审批人放行，全程真实发起人/审批人留痕。
        Instant ts = Instant.parse("2026-06-23T00:00:00Z");
        // 分级授权引擎：op_big 额度极大（一律额度内自助）、op_tiny 额度仅 1.00（任何票单都超额→复核）。
        RefundAuthorizationRepository refundRepo = new RefundAuthorizationRepository() {
            @Override
            public Optional<RefundAuthorityLimitRecord> findActiveLimit(String st, String sid, String dom, String pid) {
                if ("staff".equals(st) && "ticket".equals(dom) && "op_big".equals(sid)) {
                    return Optional.of(new RefundAuthorityLimitRecord(
                            "l_big", "staff", "op_big", "ticket", 100_000_000L, null, "active", ts, ts));
                }
                if ("staff".equals(st) && "ticket".equals(dom) && "op_tiny".equals(sid)) {
                    return Optional.of(new RefundAuthorityLimitRecord(
                            "l_tiny", "staff", "op_tiny", "ticket", 100L, null, "active", ts, ts));
                }
                return Optional.empty();
            }
            @Override
            public boolean isAutoRuleEnabled(String dom, String type, String pid) {
                return false;
            }
            @Override
            public RefundAuthorityLimitRecord saveLimit(RefundAuthorityLimitRecord l) {
                return l;
            }
            @Override
            public List<RefundAuthorityLimitRecord> listLimits(String projectId) {
                return List.of();
            }
            @Override
            public RefundAutoRuleRecord saveRule(RefundAutoRuleRecord r) {
                return r;
            }
            @Override
            public List<RefundAutoRuleRecord> listRules(String projectId) {
                return List.of();
            }
        };
        AuthorizationService authz = new AuthorizationService(null);
        TicketRefundService tiered = new TicketRefundService(
                ticketingRepository, publishedEvents::add, authz, new RefundAuthorizationService(refundRepo));
        tiered.tieredRefundEnabled = true;

        // ── 额度内：op_big 发起 → 当场自助自动退（status approved、审批人=发起人 op_big、权益冻结）。 ──
        TicketTypeRecord tt1 = createTicketType("tier-within-ticket");
        EntitlementTemplateRecord tpl1 = createTemplate("tier-within-template", 1, 9900L);
        service.bindTicketTypeEntitlement(tt1.id(), new TicketTypeEntitlementRequest(
                "tier-within-bind", "bind", List.of(), tpl1.id(), 1, 1, 9900L, "default", 1, 9900L));
        TestSecurityActors.asMerchantUser("mch_tier");
        TicketOrderRecord order1 = service.createTicketOrder(new TicketOrderRequest(
                "tier-within-order", "create", List.of(), tt1.id(), null, 1,
                LocalDate.parse("2026-06-15"), "admin", "13800138000", "cust_tier_within"));
        service.issueTicketOrder(order1.id(), new StatusChangeRequest("tier-within-issue", "issue", "issue", List.of()));
        IssuedEntitlementRecord ent1 = ticketingRepository.listIssuedEntitlementsByOrderId(order1.id()).get(0);

        actAsAdmin("op_big");
        TicketRefundRecord within = tiered.requestRefund(refundRequest(
                "tier-within-refund", "refund", List.of(), order1.id(), order1.amountCent(), "within limit", "original"));
        // 额度内自助同样只到“已通过”：兑付确认前不计入已退金额。
        assertThat(within.status()).isEqualTo("approved");
        assertThat(within.requestedBy()).isEqualTo("op_big");
        assertThat(within.approvedBy()).isEqualTo("op_big");
        assertThat(ticketingRepository.findIssuedEntitlementById(ent1.id()))
                .get().extracting(IssuedEntitlementRecord::status).isEqualTo("refunded");

        // ── 超额度：op_tiny 发起 → 留待复核（status requested、未冻结）；SoD：本人不可批、他人可批。 ──
        TicketTypeRecord tt2 = createTicketType("tier-review-ticket");
        EntitlementTemplateRecord tpl2 = createTemplate("tier-review-template", 1, 9900L);
        service.bindTicketTypeEntitlement(tt2.id(), new TicketTypeEntitlementRequest(
                "tier-review-bind", "bind", List.of(), tpl2.id(), 1, 1, 9900L, "default", 1, 9900L));
        TestSecurityActors.asMerchantUser("mch_tier");
        TicketOrderRecord order2 = service.createTicketOrder(new TicketOrderRequest(
                "tier-review-order", "create", List.of(), tt2.id(), null, 1,
                LocalDate.parse("2026-06-15"), "admin", "13800138000", "cust_tier_review"));
        service.issueTicketOrder(order2.id(), new StatusChangeRequest("tier-review-issue", "issue", "issue", List.of()));
        IssuedEntitlementRecord ent2 = ticketingRepository.listIssuedEntitlementsByOrderId(order2.id()).get(0);

        actAsAdmin("op_tiny");
        TicketRefundRecord review = tiered.requestRefund(refundRequest(
                "tier-review-refund", "refund", List.of(), order2.id(), order2.amountCent(), "over limit", "original"));
        assertThat(review.status()).isEqualTo("requested");
        assertThat(review.requestedBy()).isEqualTo("op_tiny");
        assertThat(review.approvedBy()).isNull();
        assertThat(ticketingRepository.findIssuedEntitlementById(ent2.id()))
                .get().extracting(IssuedEntitlementRecord::status).isEqualTo("active");

        // SoD：发起人 op_tiny 自审自批 → 拒。
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> tiered.approveRefund(review.id(), new StatusChangeRequest(
                        "tier-review-selfapprove", "approve", "approved", List.of())))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("退款申请人不能自行审批");

        // 不同审批人 op_other 放行 → 生效（审批人留痕 op_other、发起人仍 op_tiny、权益冻结）。
        actAsAdmin("op_other");
        TicketRefundRecord approved = tiered.approveRefund(review.id(), new StatusChangeRequest(
                "tier-review-approve", "approve", "approved", List.of()));
        assertThat(approved.approvedBy()).isEqualTo("op_other");
        assertThat(approved.requestedBy()).isEqualTo("op_tiny");
        assertThat(ticketingRepository.findIssuedEntitlementById(ent2.id()))
                .get().extracting(IssuedEntitlementRecord::status).isEqualTo("refunded");

        TestSecurityActors.clear();
    }

    /** 设平台管理员上下文但 id 可指定——分级授权需「不同发起人/审批人」验 SoD，而平台管理员 scope 为空不触发商户闸。 */
    private static void actAsAdmin(String userId) {
        AccessActor actor = new AccessActor(userId, userId, "platform_admin", "org_1", null, "active");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }

    @Test
    void limitedStockRefundReleasesOneSlotPerTicket() {
        // 方案2 限量票名额回补：一单 2 张，分别退 → 每退一张放回一个名额（退一张放一个）。
        TicketTypeRecord ticketType = createTicketType("refund-stock2-ticket");
        EntitlementTemplateRecord template = createTemplate("refund-stock2-template", 1, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "refund-stock2-bind", "bind", List.of(), template.id(), 1, 1, 9900L, "default", 1, 9900L));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "refund-stock2-order", "create", List.of(), ticketType.id(), null, 2,
                LocalDate.parse("2026-06-15"), "admin", "13800138000", "customer_refund_stock2"));
        service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "refund-stock2-issue", "issue", "issue", List.of()));
        List<TicketInstanceRecord> instances = ticketingRepository.listTicketInstancesByOrderId(order.id());
        assertThat(instances).hasSize(2);

        TicketRefundRecord r1 = service.requestTicketRefund(refundOfInstances(
                "refund-stock2-1", "refund", List.of(), order.id(), null, "one", "original", List.of(instances.get(0).id())));
        service.approveTicketRefund(r1.id(), new StatusChangeRequest("refund-stock2-app1", "approve", "approved", List.of()));
        assertThat(ticketingRepository.releasedStock).containsExactly(ticketType.id() + ":1");

        TicketRefundRecord r2 = service.requestTicketRefund(refundOfInstances(
                "refund-stock2-2", "refund", List.of(), order.id(), null, "two", "original", List.of(instances.get(1).id())));
        service.approveTicketRefund(r2.id(), new StatusChangeRequest("refund-stock2-app2", "approve", "approved", List.of()));
        // 再退一张再放一个（共两次各放一个）。
        assertThat(ticketingRepository.releasedStock).containsExactly(ticketType.id() + ":1", ticketType.id() + ":1");
    }

    @Test
    void refundRequestFreezesTicketAndRejectRestoresIt() {
        // 方案2：申请即冻结（父票 refund_pending、核销被拒）；复核驳回 → 父票恢复 active、可再核销/再申请退。
        TicketTypeRecord ticketType = createTicketType("refund-reject-ticket");
        EntitlementTemplateRecord template = createTemplate("refund-reject-template", 1, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "refund-reject-bind", "bind", List.of(), template.id(), 1, 1, 9900L, "default", 1, 9900L));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "refund-reject-order", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-15"), "admin", "13800138000", "customer_refund_reject"));
        service.issueTicketOrder(order.id(), new StatusChangeRequest("refund-reject-issue", "issue", "issue", List.of()));
        IssuedEntitlementRecord ent = ticketingRepository.listIssuedEntitlementsByOrderId(order.id()).get(0);
        String instanceId = ent.parentTicketId();

        TicketRefundRecord refund = service.requestTicketRefund(refundRequest(
                "refund-reject-request", "refund", List.of(), order.id(), null, "customer request", "original"));
        // 申请即冻结：父票 refund_pending，核销被拒（父票守卫）。
        assertThat(ticketingRepository.findTicketInstanceById(instanceId)).get()
                .extracting(TicketInstanceRecord::status).isEqualTo("refund_pending");
        TicketCredentialResponse frozenCred = service.getTicketCredential(ent.id());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createVerification(new VerificationRequest(
                        "verify-frozen", "dynamic_qr", frozenCred.credentialToken(), "dev_1", "gate_1", Map.of())))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class, ex ->
                        assertThat(ex.code()).isEqualTo("TICKET_INSTANCE_REFUND_IN_PROGRESS"));

        // 复核驳回 → 父票恢复 active、退款单 rejected、权益仍 active。
        TicketRefundRecord rejected = service.rejectTicketRefund(refund.id(), new StatusChangeRequest(
                "refund-reject-decide", "reject", "rejected", List.of()));
        assertThat(rejected.status()).isEqualTo("rejected");
        assertThat(ticketingRepository.findTicketInstanceById(instanceId)).get()
                .extracting(TicketInstanceRecord::status).isEqualTo("active");
        assertThat(ticketingRepository.findIssuedEntitlementById(ent.id())).get()
                .extracting(IssuedEntitlementRecord::status).isEqualTo("active");
    }

    @Test
    void listTicketRefundLinesFiltersByMerchantScopeSoMerchantCannotReadAnotherMerchants() {
        // B1d.3 跨租户读隔离：退款行无 merchant_id 列，按其 order 的受理商户过滤。
        Instant now = Instant.parse("2026-06-22T00:00:00Z");
        ticketingRepository.ticketOrders.put("o_b1d3", new TicketOrderRecord(
                "o_b1d3", "ON-B1D3", "c1", "tt1", null, 1, LocalDate.parse("2026-06-20"),
                "web", "13800138000", 9900L, null, 9900L, 0L, "CNY", "paid", "issued", "paid", now, now, "mch_a", null, null));
        ticketingRepository.saveTicketRefundLine(new TicketRefundLineRecord(
                "rl_b1d3", "refund_b1d3", "o_b1d3", "e1", "entitlement", "entitlement",
                9900L, 1, 0, "requested", now, now));

        // 商户B 读不到商户A 订单的退款行。
        TestSecurityActors.asMerchantUser("mch_b");
        assertThat(service.listTicketRefundLines("refund_b1d3").items()).isEmpty();
        // 商户A 看得到自己的。
        TestSecurityActors.asMerchantUser("mch_a");
        assertThat(service.listTicketRefundLines("refund_b1d3").items()).hasSize(1);
        // 平台（无商户范围）见全部。
        TestSecurityActors.asPlatformAdmin();
        assertThat(service.listTicketRefundLines("refund_b1d3").items()).hasSize(1);
    }

    @Test
    void refundRequestCreatesEntitlementLevelLinesAndApprovalMarksLinesApproved() {
        TicketTypeRecord ticketType = createTicketType("refund-lines-ticket");
        EntitlementTemplateRecord template = createTemplate("refund-lines-template", 1, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "refund-lines-bind",
                "bind",
                List.of(),
                template.id(),
                1,
                1,
                9900L,
                "default",
                1,
                9900L
        ));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "refund-lines-order",
                "create",
                List.of(),
                ticketType.id(),
                null,
                2,
                LocalDate.parse("2026-06-15"),
                "admin",
                "13800138000",
                "customer_refund_lines"
        ));
        IssuedTicketResponse issued = service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "refund-lines-issue-order",
                "issue",
                "issue",
                List.of()
        ));

        TicketRefundRecord refund = service.requestTicketRefund(refundRequest(
                "refund-lines-request",
                "refund",
                List.of(),
                order.id(),
                order.amountCent(),
                "customer request",
                "original"
        ));

        PageResponse<TicketRefundLineRecord> requestedLines = service.listTicketRefundLines(refund.id());
        assertThat(requestedLines.items())
                .hasSize(2)
                .allSatisfy(line -> {
                    assertThat(line.refundId()).isEqualTo(refund.id());
                    assertThat(line.orderId()).isEqualTo(order.id());
                    assertThat(line.lineType()).isEqualTo("entitlement");
                    assertThat(line.refundAmountCent()).isEqualTo(9900L);
                    assertThat(line.remainingTimesBefore()).isEqualTo(1);
                    assertThat(line.usedTimesBefore()).isZero();
                    assertThat(line.status()).isEqualTo("requested");
                });
        assertThat(requestedLines.items())
                .extracting(TicketRefundLineRecord::entitlementId)
                .containsExactlyInAnyOrderElementsOf(ticketingRepository.listIssuedEntitlementsByOrderId(order.id()).stream()
                        .map(IssuedEntitlementRecord::id)
                        .toList());

        service.approveTicketRefund(refund.id(), new StatusChangeRequest(
                "refund-lines-approve",
                "approve",
                "approved",
                List.of()
        ));

        assertThat(service.listTicketRefundLines(refund.id()).items())
                .extracting(TicketRefundLineRecord::status)
                .containsOnly("approved");
        assertThat(ticketingRepository.listIssuedEntitlementsByOrderId(order.id()))
                .extracting(IssuedEntitlementRecord::entitlementCode)
                .containsExactlyInAnyOrderElementsOf(issued.issuedEntitlements().stream()
                        .map(IssuedEntitlementResponse::entitlementCode)
                        .toList());
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_refund.requested", "ticket.refund_line.list", "ticket_refund.approved");
    }

    @Test
    void dynamicPricingRuleSimulationDoesNotChangeOrderPricingUntilFutureIntegration() {
        TicketTypeRecord ticketType = createTicketType("dynamic-pricing");
        TicketDynamicPricingRuleRecord draft = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-rule-create",
                "summer weekend uplift",
                List.of("att_dynamic"),
                ticketType.id(),
                "Summer weekend uplift",
                100,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "web",
                "percent_delta",
                new BigDecimal("20.00"),
                null,
                9900L,
                15000L,
                Map.of("inventory_level", "low")
        ));

        TicketDynamicPricingSimulationResponse simulation = service.simulateDynamicPricing(new TicketDynamicPricingSimulationRequest(
                "dynamic-simulate",
                ticketType.id(),
                LocalDate.parse("2026-06-20"),
                "web",
                Map.of("inventory_level", "low")
        ));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-order-unaffected",
                "create order",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic"
        ));

        assertThat(draft.status()).isEqualTo("draft");
        assertThat(simulation)
                .extracting(
                        TicketDynamicPricingSimulationResponse::basePriceCent,
                        TicketDynamicPricingSimulationResponse::adjustmentCent,
                        TicketDynamicPricingSimulationResponse::finalPriceCent,
                        TicketDynamicPricingSimulationResponse::matchedRuleId
                )
                .containsExactly(11900L, 2380L, 14280L, draft.id());
        assertThat(order.amountCent()).isEqualTo(11900L);
        assertThat(order.dynamicPricingRuleId()).isNull();
        assertThat(order.baseAmountCent()).isEqualTo(11900L);
        assertThat(order.dynamicAdjustmentCent()).isZero();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_dynamic_pricing.created.success", "ticket_dynamic_pricing.simulated.success", "ticket_order.created");
    }

    @Test
    void publishedDynamicPricingRuleChangesFormalOrderAmount() {
        TicketTypeRecord ticketType = createTicketType("dynamic-order-applied");
        TicketDynamicPricingRuleRecord draft = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-order-applied-create",
                "publish uplift",
                List.of("att_dynamic_order"),
                ticketType.id(),
                "Published weekend uplift",
                100,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "web",
                "fixed_delta",
                null,
                2000L,
                9900L,
                16000L,
                Map.of()
        ));
        TicketDynamicPricingRuleRecord published = service.publishDynamicPricingRule(
                draft.id(),
                new StatusChangeRequest("dynamic-order-applied-publish", "publish", "approved for order pricing", List.of())
        );

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-order-applied-order",
                "create order with published dynamic pricing",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic_order"
        ));

        assertThat(published.status()).isEqualTo("published");
        assertThat(order.amountCent()).isEqualTo(13900L);
        assertThat(order.dynamicPricingRuleId()).isEqualTo(published.id());
        assertThat(order.baseAmountCent()).isEqualTo(11900L);
        assertThat(order.dynamicAdjustmentCent()).isEqualTo(2000L);
    }

    @Test
    void publishedDynamicPricingRuleRequiresMatchingOrderContext() {
        TicketTypeRecord ticketType = createTicketType("dynamic-order-context");
        TicketDynamicPricingRuleRecord draft = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-order-context-create",
                "conditional uplift",
                List.of(),
                ticketType.id(),
                "Conditional weekend uplift",
                100,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "web",
                "percent_delta",
                new BigDecimal("20.00"),
                null,
                9900L,
                15000L,
                Map.of("inventory_level", "low")
        ));
        TicketDynamicPricingRuleRecord published = service.publishDynamicPricingRule(
                draft.id(),
                new StatusChangeRequest("dynamic-order-context-publish", "publish", "approved for conditional order pricing", List.of())
        );

        TicketOrderRecord unmatched = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-order-context-unmatched",
                "create without context",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic_context"
        ));
        TicketOrderRecord matched = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-order-context-matched",
                "create with context",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic_context",
                Map.of("inventory_level", "low")
        ));

        assertThat(unmatched.amountCent()).isEqualTo(11900L);
        assertThat(unmatched.dynamicPricingRuleId()).isNull();
        assertThat(matched.amountCent()).isEqualTo(14280L);
        assertThat(matched.dynamicPricingRuleId()).isEqualTo(published.id());
        assertThat(matched.baseAmountCent()).isEqualTo(11900L);
        assertThat(matched.dynamicAdjustmentCent()).isEqualTo(2380L);
    }

    @Test
    void createTicketOrderRecordsDynamicPricingTraceWhenRuleApplies() {
        TicketTypeRecord ticketType = createTicketType("dynamic-trace-applied");
        TicketDynamicPricingRuleRecord draft = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-trace-applied-create",
                "publish discount",
                List.of("att_dynamic_trace"),
                ticketType.id(),
                "Published discount",
                100,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "web",
                "fixed_delta",
                null,
                -2000L,
                null,
                null,
                Map.of()
        ));
        TicketDynamicPricingRuleRecord published = service.publishDynamicPricingRule(
                draft.id(),
                new StatusChangeRequest("dynamic-trace-applied-publish", "publish", "approved discount", List.of())
        );

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-trace-applied-order",
                "create order with published discount",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic_trace"
        ));

        long unitPrice = 11900L;
        assertThat(order.baseAmountCent()).isEqualTo(unitPrice * 1);
        assertThat(order.dynamicAdjustmentCent()).isLessThan(0L);
        assertThat(order.baseAmountCent() + order.dynamicAdjustmentCent()).isEqualTo(order.amountCent());
        assertThat(order.dynamicPricingRuleId()).isEqualTo(published.id());
    }

    @Test
    void createTicketOrderRecordsZeroTraceWhenNoDynamicRule() {
        TicketTypeRecord ticketType = createTicketType("dynamic-trace-none");

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-trace-none-order",
                "create order without dynamic rule",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic_trace_none"
        ));

        assertThat(order.dynamicAdjustmentCent()).isEqualTo(0L);
        assertThat(order.baseAmountCent()).isEqualTo(order.amountCent());
        assertThat(order.dynamicPricingRuleId()).isNull();
    }

    @Test
    void dynamicPricingTraceScalesWithQuantity() {
        TicketTypeRecord ticketType = createTicketType("dynamic-trace-scale");
        TicketDynamicPricingRuleRecord draft = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-trace-scale-create",
                "publish discount",
                List.of("att_dynamic_scale"),
                ticketType.id(),
                "Published discount",
                100,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "web",
                "fixed_delta",
                null,
                -2000L,
                null,
                null,
                Map.of()
        ));
        TicketDynamicPricingRuleRecord published = service.publishDynamicPricingRule(
                draft.id(),
                new StatusChangeRequest("dynamic-trace-scale-publish", "publish", "approved discount", List.of())
        );

        int quantity = 3;
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-trace-scale-order",
                "create order with quantity",
                List.of(),
                ticketType.id(),
                null,
                quantity,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic_trace_scale"
        ));

        long unitPrice = 11900L;
        long unitAdjustment = -2000L;
        assertThat(order.dynamicPricingRuleId()).isEqualTo(published.id());
        assertThat(order.baseAmountCent()).isEqualTo(unitPrice * quantity);
        assertThat(order.dynamicAdjustmentCent()).isEqualTo(unitAdjustment * quantity);
        assertThat(order.baseAmountCent() + order.dynamicAdjustmentCent()).isEqualTo(order.amountCent());
    }

    @Test
    void dynamicPricingNeverProducesNegativeFinalPrice() {
        TicketTypeRecord ticketType = createTicketType("dynamic-non-negative");
        TicketDynamicPricingRuleRecord draft = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-non-negative-create",
                "large discount",
                List.of(),
                ticketType.id(),
                "Large discount",
                100,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "web",
                "fixed_delta",
                null,
                -20000L,
                null,
                null,
                Map.of()
        ));
        service.publishDynamicPricingRule(
                draft.id(),
                new StatusChangeRequest("dynamic-non-negative-publish", "publish", "approved discount", List.of())
        );

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-non-negative-order",
                "create non negative order",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic_non_negative"
        ));

        assertThat(order.amountCent()).isZero();
        assertThat(order.baseAmountCent()).isEqualTo(11900L);
        assertThat(order.dynamicAdjustmentCent()).isEqualTo(-11900L);
    }

    @Test
    void publishedDynamicPricingUsesHighestPriorityMatchingRuleForFormalOrder() {
        TicketTypeRecord ticketType = createTicketType("dynamic-priority");
        TicketDynamicPricingRuleRecord lowPriority = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-priority-low",
                "low priority large uplift",
                List.of(),
                ticketType.id(),
                "Low priority large uplift",
                10,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "web",
                "fixed_delta",
                null,
                5000L,
                null,
                null,
                Map.of()
        ));
        TicketDynamicPricingRuleRecord highPriority = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-priority-high",
                "high priority small uplift",
                List.of(),
                ticketType.id(),
                "High priority small uplift",
                100,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "web",
                "fixed_delta",
                null,
                1000L,
                null,
                null,
                Map.of()
        ));
        service.publishDynamicPricingRule(
                lowPriority.id(),
                new StatusChangeRequest("dynamic-priority-low-publish", "publish", "approved low priority", List.of())
        );
        TicketDynamicPricingRuleRecord publishedHigh = service.publishDynamicPricingRule(
                highPriority.id(),
                new StatusChangeRequest("dynamic-priority-high-publish", "publish", "approved high priority", List.of())
        );

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "dynamic-priority-order",
                "create order with competing dynamic rules",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "web",
                "13800138000",
                "customer_dynamic_priority"
        ));

        assertThat(order.amountCent()).isEqualTo(12900L);
        assertThat(order.dynamicPricingRuleId()).isEqualTo(publishedHigh.id());
        assertThat(order.baseAmountCent()).isEqualTo(11900L);
        assertThat(order.dynamicAdjustmentCent()).isEqualTo(1000L);
    }

    @Test
    void dynamicPricingPublishAndNoMatchSimulationAreAudited() {
        TicketTypeRecord ticketType = createTicketType("dynamic-publish");
        TicketDynamicPricingRuleRecord draft = service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-publish-create",
                "weekday discount",
                List.of(),
                ticketType.id(),
                "Weekday discount",
                10,
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                "app",
                "fixed_delta",
                null,
                -1000L,
                6900L,
                15000L,
                Map.of("member_level", "gold")
        ));

        TicketDynamicPricingRuleRecord published = service.publishDynamicPricingRule(
                draft.id(),
                new StatusChangeRequest("dynamic-publish-once", "publish", "approved by ops", List.of("att_publish"))
        );
        TicketDynamicPricingSimulationResponse noMatch = service.simulateDynamicPricing(new TicketDynamicPricingSimulationRequest(
                "dynamic-no-match",
                ticketType.id(),
                LocalDate.parse("2026-07-01"),
                "app",
                Map.of("member_level", "gold")
        ));

        assertThat(published.status()).isEqualTo("published");
        assertThat(noMatch.matchedRuleId()).isNull();
        assertThat(noMatch.finalPriceCent()).isEqualTo(noMatch.basePriceCent());
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_dynamic_pricing.published.success", "ticket_dynamic_pricing.simulated.success");
    }

    @Test
    void dynamicPricingFailuresAreAuditedWithDeclaredEventNames() {
        // 第十八批 N 段 124-R2a：动态定价建、发布、模拟失败落 OpenAPI 声明名
        assertThatThrownBy(() -> service.createDynamicPricingRule(new TicketDynamicPricingRuleRequest(
                "dynamic-fail-create", "missing ticket type", List.of(), "ticket_type_missing", "Missing", 10,
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-30"), "web", "fixed_delta", null, 1000L,
                6900L, 15000L, Map.of())))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("TICKET_TYPE_NOT_FOUND");
        assertThatThrownBy(() -> service.publishDynamicPricingRule("dynamic_rule_missing",
                new StatusChangeRequest("dynamic-fail-publish", "publish", "missing rule", List.of())))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("DYNAMIC_PRICING_RULE_NOT_FOUND");
        assertThatThrownBy(() -> service.simulateDynamicPricing(new TicketDynamicPricingSimulationRequest(
                "dynamic-fail-simulate", "ticket_type_missing", LocalDate.parse("2026-06-20"), "web", Map.of())))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("TICKET_TYPE_NOT_FOUND");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("ticket_dynamic_pricing.created.failure", "ticket_dynamic_pricing.published.failure",
                        "ticket_dynamic_pricing.simulated.failure");
    }

    @Test
    void verificationFailsWhenEntitlementIsExpired() {
        TicketTypeRecord ticketType = createTicketType("expired-ticket");
        EntitlementTemplateRecord template = createTemplate("expired-template", 2, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "expired-bind", "bind", List.of(), template.id(), 1, 2, 9900L, "default", null, null
        ));
        IssuedTicketResponse issued = issueDirectlyForTest(ticketType.id(), new TicketIssueRequest(
                "expired-issue", "issue", List.of(), "order_expired", "manual", List.of("qr")
        ));
        String code = issued.issuedEntitlements().getFirst().entitlementCode();
        IssuedEntitlementRecord entitlement = ticketingRepository.findIssuedEntitlementByCode(code).orElseThrow();
        // Manually expire the entitlement by overwriting with past validTo
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                entitlement.id(), entitlement.entitlementCode(), entitlement.ticketTypeId(),
                entitlement.orderId(), entitlement.entitlementTemplateId(), entitlement.entitlementType(),
                entitlement.merchantId(), entitlement.venueId(), entitlement.pointId(),
                entitlement.totalTimes(), entitlement.usedTimes(), entitlement.valueAmountCent(),
                entitlement.status(), entitlement.validFrom(),
                Instant.now().minusSeconds(3600), // expired 1 hour ago
                entitlement.createdAt(), entitlement.updatedAt(), entitlement.numPeople(), entitlement.allocatedRevenueCent()
        ,
        null, null, null, null, null, 0, null, null));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createVerification(new VerificationRequest(
                        "expired-verify", "qr", code, "dev_1", "gate_1", Map.of()
                )))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("该权益已过期");

        VerificationPreviewResponse preview = service.previewVerification(new VerificationRequest(
                "expired-preview", "qr", code, "dev_1", "gate_1", Map.of()
        ));
        org.assertj.core.api.Assertions.assertThat(preview.allowed()).isFalse();
    }

    @Test
    void atomicVerificationPreventsDoubleUseWhenRemainingTimesExhausted() {
        TicketTypeRecord ticketType = createTicketType("atomic-ticket");
        EntitlementTemplateRecord template = createTemplate("atomic-template", 1, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "atomic-bind", "bind", List.of(), template.id(), 1, 1, 9900L, "default", null, null
        ));
        IssuedTicketResponse issued = issueDirectlyForTest(ticketType.id(), new TicketIssueRequest(
                "atomic-issue", "issue", List.of(), "order_atomic", "manual", List.of("qr")
        ));
        String code = issued.issuedEntitlements().getFirst().entitlementCode();

        service.createVerification(new VerificationRequest(
                "atomic-first", "qr", code, "dev_1", "gate_1", Map.of()
        ));

        // Second call with different idempotency key should fail – entitlement is now exhausted
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createVerification(new VerificationRequest(
                        "atomic-second-key", "qr", code, "dev_1", "gate_1", Map.of()
                )))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("该权益剩余次数已用完");

        org.assertj.core.api.Assertions.assertThat(ticketingRepository.findIssuedEntitlementByCode(code))
                .get().extracting(IssuedEntitlementRecord::usedTimes).isEqualTo(1);
    }

    @Test
    void duplicateRefundOnSameTicketIsRejectedWhileInProgress() {
        // 方案2：同一张票在途退款（refund_pending）不能再被申请退款 → TICKET_INSTANCE_REFUND_IN_PROGRESS。
        IssuedEntitlementRecord entitlement = issueOrderEntitlement("dup-refund");
        TicketOrderRecord order = ticketingRepository.findTicketOrderById(entitlement.orderId()).orElseThrow();
        String instanceId = entitlement.parentTicketId();

        // 第一笔申请（不自助，留 refund_pending）。
        service.requestTicketRefund(refundOfInstances(
                "dup-refund-1", "refund", List.of(), order.id(), null, "first", "original", List.of(instanceId)));

        // 同票再申请 → 拒（在途冻结中）。
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.requestTicketRefund(
                refundOfInstances(
                        "dup-refund-2", "refund", List.of(), order.id(), null, "second", "original", List.of(instanceId))))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class, ex ->
                        assertThat(ex.code()).isEqualTo("TICKET_INSTANCE_REFUND_IN_PROGRESS"));
    }

    @Test
    void issueOrderRequiresPaymentForNonAdminChannel() {
        TicketTypeRecord ticketType = createTicketType("payment-guard");
        EntitlementTemplateRecord template = createTemplate("payment-guard-template", 1, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "payment-guard-bind", "bind", List.of(), template.id(), 1, 1, 9900L, "default", null, null
        ));

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "payment-guard-order", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-20"), "web", "13800138000", "customer_guard"
        ));

        org.assertj.core.api.Assertions.assertThat(order.payStatus()).isEqualTo("unpaid");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.issueTicketOrder(order.id(),
                        new StatusChangeRequest("payment-guard-issue", "issue", "force issue", List.of())
                ))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                .hasMessageContaining("订单须支付完成后才能出票");
    }

    @Test
    void issueOrderToctouLockPreventsDoubleIssuance() {
        TicketTypeRecord ticketType = createTicketType("toctou-ticket");
        EntitlementTemplateRecord template = createTemplate("toctou-template", 1, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "toctou-bind", "bind", List.of(), template.id(), 1, 1, 9900L, "default", null, 9900L
        ));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "toctou-order", "create", List.of(), ticketType.id(), null, 1,
                LocalDate.parse("2026-06-20"), "admin", "13800138000", "customer_toctou"
        ));

        service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "toctou-issue-once", "issue", "first issue", List.of()
        ));

        // tryLockOrderForIssuance must now return false (order is in 'issued' state)
        org.assertj.core.api.Assertions.assertThat(
                ticketingRepository.tryLockOrderForIssuance(order.id())
        ).isFalse();
        org.assertj.core.api.Assertions.assertThat(ticketingRepository.issuedEntitlements).hasSize(1);
    }

    @Test
    void issuedEntitlementCarriesMerchantIdFromSecurityContext() {
        TestSecurityActors.asMerchantUser("mch_filter_test");
        try {
            TicketTypeRecord ticketType = createTicketType("merchant-filter");
            EntitlementTemplateRecord template = createTemplate("merchant-filter-template", 1, 9900L);
            service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                    "merchant-filter-bind", "bind", List.of(), template.id(), 1, 1, 9900L, "default", null, null
            ));
            issueDirectlyForTest(ticketType.id(), new TicketIssueRequest(
                    "merchant-filter-issue", "issue", List.of(), "order_mch_filter", "manual", List.of("qr")
            ));

            // Entitlement should have merchantId from the security context
            List<IssuedEntitlementRecord> entitlements = ticketingRepository.listIssuedEntitlements().stream()
                    .filter(e -> "order_mch_filter".equals(e.orderId()))
                    .toList();
            org.assertj.core.api.Assertions.assertThat(entitlements).hasSize(1);
            org.assertj.core.api.Assertions.assertThat(entitlements.getFirst().merchantId()).isEqualTo("mch_filter_test");
        } finally {
            TestSecurityActors.clear();
        }
    }

    @Test
    void unionPassEntitlementsInheritPerTemplateMerchantVenuePointNotIssuer() {
        // R5 审计点②：联合通票两个模板分属商户 A/B，出票后两权益各归属各自模板（非出票方）。
        TestSecurityActors.asMerchantUser("mch_issuer");
        try {
            Instant now = Instant.now();
            ticketingRepository.saveTicketType(new TicketTypeRecord(
                    "tt_union", "Union Pass", "weekday_weekend_holiday", 9900L, 11900L, 12900L,
                    false, "published", null, null, now, now, null, 0, null, false, null, TicketTypeRecord.NO_COMBO_DISCOUNT));
            ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                    "tpl_a", "Entry A", "entry", 1, 9900L, "active",
                    "mch_a", "venue_a", "gate_a", null, now, now,
                    null, null, null, null));
            ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                    "tpl_b", "Entry B", "entry", 1, 9900L, "active",
                    "mch_b", "venue_b", "gate_b", null, now, now,
                    null, null, null, null));
            ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                    "link_a", "tt_union", "tpl_a", 1, 1, 9900L, "default", 1, null, now, now));
            ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                    "link_b", "tt_union", "tpl_b", 1, 1, 9900L, "default", 1, null, now, now));

            issueDirectlyForTest("tt_union", new TicketIssueRequest(
                    "union-issue", "issue", List.of(), "order_union", "manual", List.of("qr")));

            List<IssuedEntitlementRecord> issued = ticketingRepository.issuedEntitlements.values().stream()
                    .filter(e -> "order_union".equals(e.orderId()))
                    .toList();
            assertThat(issued).hasSize(2);
            assertThat(issued).filteredOn(e -> "tpl_a".equals(e.entitlementTemplateId()))
                    .singleElement()
                    .satisfies(e -> {
                        assertThat(e.merchantId()).isEqualTo("mch_a");
                        assertThat(e.venueId()).isEqualTo("venue_a");
                        assertThat(e.pointId()).isEqualTo("gate_a");
                    });
            assertThat(issued).filteredOn(e -> "tpl_b".equals(e.entitlementTemplateId()))
                    .singleElement()
                    .satisfies(e -> {
                        assertThat(e.merchantId()).isEqualTo("mch_b");
                        assertThat(e.venueId()).isEqualTo("venue_b");
                        assertThat(e.pointId()).isEqualTo("gate_b");
                    });
            // 不得统一为出票方 mch_issuer
            assertThat(issued).extracting(IssuedEntitlementRecord::merchantId)
                    .doesNotContain("mch_issuer");
        } finally {
            TestSecurityActors.clear();
        }
    }

    // 待确认项#1：组合票酒吧分项 600 / 游乐分项 400、组合实付 800（打折）→ 出票后酒吧权益 allocated=480、游乐 allocated=320。
    @Test
    void issueAllocatesOrderRevenueByComponentPriceRatio() {
        Instant now = Instant.now();
        ticketingRepository.saveTicketType(new TicketTypeRecord(
                "tt_combo", "Combo Pass", "weekday_weekend_holiday", 99900L, 99900L, 99900L,
                false, "published", null, null, now, now, null, 0, null, false, null, TicketTypeRecord.NO_COMBO_DISCOUNT));
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_bar", "Bar Entry", "gift_drink", 1, 0L, "active", "mch_bar", null, null, "bar_sku_1", now, now,
                null, null, null, null));
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_amu", "Amusement Ride", "amusement", 1, 0L, "active", "mch_amu", null, null, null, now, now,
                null, null, null, null));
        // binding 分项单价：酒吧 600 / 游乐 400。
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_bar", "tt_combo", "tpl_bar", 1, 1, 0L, "default", 1, 600L, now, now));
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_amu", "tt_combo", "tpl_amu", 1, 1, 0L, "default", 1, 400L, now, now));
        // 组合票打折实付 800（admin 渠道可直接出票）。
        ticketingRepository.saveTicketOrder(new TicketOrderRecord(
                "order_combo", "TO-COMBO", "customer_combo", "tt_combo", null, 1,
                LocalDate.parse("2026-06-20"), "admin", "13800138000", 800L, null, 1000L, -200L,
                "CNY", "paid", "not_issued", "created", now, now, null, null, null));

        service.issueTicketOrder("order_combo", new StatusChangeRequest(
                "combo-issue", "issue", "issue", List.of()));

        List<IssuedEntitlementRecord> issued = ticketingRepository.listIssuedEntitlementsByOrderId("order_combo");
        assertThat(issued).hasSize(2);
        assertThat(issued).filteredOn(e -> "tpl_bar".equals(e.entitlementTemplateId()))
                .singleElement().extracting(IssuedEntitlementRecord::allocatedRevenueCent).isEqualTo(480L);
        assertThat(issued).filteredOn(e -> "tpl_amu".equals(e.entitlementTemplateId()))
                .singleElement().extracting(IssuedEntitlementRecord::allocatedRevenueCent).isEqualTo(320L);
        // 净收入不重复硬约束：各权益分配收入之和恰等于订单实付（不丢分、不超额）。
        assertThat(issued.stream().mapToLong(IssuedEntitlementRecord::allocatedRevenueCent).sum()).isEqualTo(800L);
    }

    // S11③：整票一条票面原价都没配却收了钱 → 当场拒绝出票，绝不再「大家平均分」。
    //   平均分会把免费权益和赠品也算成一份收入：提供方既拿一份营收份额、又按内部结算价被兑付一次，
    //   同一份东西付两遍钱；而且账面上每一分都分掉了，事后查不出来分错了谁。
    @Test
    void issueRefusesWhenTicketTypeHasNoFaceValuePriceAtAll() {
        Instant now = Instant.now();
        ticketingRepository.saveTicketType(new TicketTypeRecord(
                "tt_even", "Even Pass", "weekday_weekend_holiday", 99900L, 99900L, 99900L,
                false, "published", null, null, now, now, null, 0, null, false, null, TicketTypeRecord.NO_COMBO_DISCOUNT));
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_e1", "E1", "entry", 1, 0L, "active", null, null, null, null, now, now,
                null, null, null, null));
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_e2", "E2", "amusement", 1, 0L, "active", null, null, null, null, now, now,
                null, null, null, null));
        // 两个 binding 票面原价均为空（旧口径会回退均摊各 400）。
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_e1", "tt_even", "tpl_e1", 1, 1, 0L, "default", 1, null, now, now));
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_e2", "tt_even", "tpl_e2", 1, 1, 0L, "default", 1, null, now, now));
        ticketingRepository.saveTicketOrder(new TicketOrderRecord(
                "order_even", "TO-EVEN", "customer_even", "tt_even", null, 1,
                LocalDate.parse("2026-06-20"), "admin", "13800138000", 800L, null, 800L, 0L,
                "CNY", "paid", "not_issued", "created", now, now, null, null, null));

        assertThatThrownBy(() -> service.issueTicketOrder("order_even", new StatusChangeRequest(
                "even-issue", "issue", "issue", List.of())))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("TICKET_COMPONENT_PRICE_MISSING"));
    }

    // S11③：票面原价 0 = 免费 = 权重 0 不参与分钱（赠酒、0 元餐饮券），钱全归收费的那几项。
    @Test
    void freeEntitlementsGetZeroShareAndPaidOnesTakeEverything() {
        Instant now = Instant.now();
        ticketingRepository.saveTicketType(new TicketTypeRecord(
                "tt_free", "Free Mix Pass", "weekday_weekend_holiday", 99900L, 99900L, 99900L,
                false, "published", null, null, now, now, null, 0, null, false, null, TicketTypeRecord.NO_COMBO_DISCOUNT));
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_paid", "Paid Entry", "entry", 1, 0L, "active", "mch_bar", null, null, null, now, now,
                null, null, null, null));
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_gift", "Gift Drink", "gift_drink", 1, 0L, "active", "mch_bar", null, null, "bar_sku_x", now, now,
                null, null, null, null));
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_paid", "tt_free", "tpl_paid", 1, 1, 0L, "default", 1, 1000L, now, now));
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_gift", "tt_free", "tpl_gift", 1, 1, 0L, "default", 1, 0L, now, now));
        ticketingRepository.saveTicketOrder(new TicketOrderRecord(
                "order_free", "TO-FREE", "customer_free", "tt_free", null, 1,
                LocalDate.parse("2026-06-20"), "admin", "13800138000", 800L, null, 1000L, -200L,
                "CNY", "paid", "not_issued", "created", now, now, null, null, null));

        service.issueTicketOrder("order_free", new StatusChangeRequest(
                "free-issue", "issue", "issue", List.of()));

        List<IssuedEntitlementRecord> issued = ticketingRepository.listIssuedEntitlementsByOrderId("order_free");
        assertThat(issued).filteredOn(e -> "tpl_paid".equals(e.entitlementTemplateId()))
                .singleElement().extracting(IssuedEntitlementRecord::allocatedRevenueCent).isEqualTo(800L);
        assertThat(issued).filteredOn(e -> "tpl_gift".equals(e.entitlementTemplateId()))
                .singleElement().extracting(IssuedEntitlementRecord::allocatedRevenueCent).isEqualTo(0L);
    }

    // K21 两条价线不串：拆份额只看【票面原价】，与【内部结算价】（签约单价）毫无关系。
    //   把内部结算价改到天上去，各业态分到的钱一分不动——两个价管两件事，谁也不能顶替谁。
    @Test
    void revenueSplitIgnoresInternalSettlementPrice() {
        Instant now = Instant.now();
        ticketingRepository.saveTicketType(new TicketTypeRecord(
                "tt_k21", "K21 Pass", "weekday_weekend_holiday", 99900L, 99900L, 99900L,
                false, "published", null, null, now, now, null, 0, null, false, null, TicketTypeRecord.NO_COMBO_DISCOUNT));
        // 酒吧权益签约单价 1 分、游乐权益签约单价 999999 分——若拆份额误用内部结算价，占比会完全颠倒。
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_k21_bar", "K21 Bar", "entry", 1, 0L, "active", "mch_bar", null, null, null, now, now,
                "virtual", 1L, null, null));
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_k21_amu", "K21 Ride", "amusement", 1, 0L, "active", "mch_amu", null, null, null, now, now,
                "virtual", 999999L, null, null));
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_k21_bar", "tt_k21", "tpl_k21_bar", 1, 1, 0L, "default", 1, 600L, now, now));
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_k21_amu", "tt_k21", "tpl_k21_amu", 1, 1, 0L, "default", 1, 400L, now, now));
        ticketingRepository.saveTicketOrder(new TicketOrderRecord(
                "order_k21", "TO-K21", "customer_k21", "tt_k21", null, 1,
                LocalDate.parse("2026-06-20"), "admin", "13800138000", 800L, null, 1000L, -200L,
                "CNY", "paid", "not_issued", "created", now, now, null, null, null));

        service.issueTicketOrder("order_k21", new StatusChangeRequest(
                "k21-issue", "issue", "issue", List.of()));

        List<IssuedEntitlementRecord> issued = ticketingRepository.listIssuedEntitlementsByOrderId("order_k21");
        assertThat(issued).filteredOn(e -> "tpl_k21_bar".equals(e.entitlementTemplateId()))
                .singleElement().extracting(IssuedEntitlementRecord::allocatedRevenueCent).isEqualTo(480L);
        assertThat(issued).filteredOn(e -> "tpl_k21_amu".equals(e.entitlementTemplateId()))
                .singleElement().extracting(IssuedEntitlementRecord::allocatedRevenueCent).isEqualTo(320L);
    }

    // 待确认项#1：direct 出票（无真实订单）→ 不拆分，allocated_revenue_cent 为 null（向后兼容）。
    @Test
    void directIssueWithoutOrderLeavesAllocatedRevenueNull() {
        // 出票内核在无真实订单时不拆分（allocated null）——退役端点前的向后兼容语义仍由内核保证。
        TicketTypeRecord ticketType = createTicketType("no-order-alloc-ticket");
        EntitlementTemplateRecord tpl = createTemplate("no-order-alloc-tpl", 2, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "no-order-alloc-bind", "bind", List.of(), tpl.id(), 1, 2, 9900L, "default", 1, 9900L));
        IssuedTicketResponse response = issueDirectlyForTest(ticketType.id(), new TicketIssueRequest(
                "no-order-alloc-issue", "issue", List.of(), "order_no_order_alloc", "manual", List.of("qr")));
        IssuedEntitlementRecord entitlement = ticketingRepository
                .findIssuedEntitlementByCode(response.issuedEntitlements().getFirst().entitlementCode()).orElseThrow();
        assertThat(entitlement.allocatedRevenueCent()).isNull();
    }

    // #3 配置入口：创建 binding 带 num_people / component_price_cent 透传落库。
    @Test
    void bindTicketTypeEntitlementPassesThroughNumPeopleAndComponentPrice() {
        TicketTypeRecord ticketType = createTicketType("cfg-binding");
        EntitlementTemplateRecord template = createTemplate("cfg-binding-template", 1, 9900L);
        TicketTypeEntitlementRecord bound = service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                "cfg-binding-bind", "bind", List.of(), template.id(), 1, 1, 9900L, "default", 4, 600L));
        assertThat(bound.numPeople()).isEqualTo(4);
        assertThat(bound.componentPriceCent()).isEqualTo(600L);
    }

    // #3 配置入口：创建 template 带 bar_item_ref / merchant/venue/point 透传落库。
    @Test
    void createEntitlementTemplatePassesThroughBarItemRefAndOwnership() {
        // S5：bar_item_ref 必须指向真实存在、且属于本提供方商户的商品。
        ticketingRepository.barProducts.put("bar_sku_42",
                new BarProductRefRecord("bar_sku_42", "mch_x", 4200L, null, false, "active"));
        EntitlementTemplateRecord template = service.createEntitlementTemplate(new EntitlementTemplateRequest(
                "cfg-template", "create", List.of(), "Gift Drink", "gift_drink", 1, 0L, "active",
                "bar_sku_42", "mch_x", "venue_x", "point_x"));
        assertThat(template.barItemRef()).isEqualTo("bar_sku_42");
        assertThat(template.merchantId()).isEqualTo("mch_x");
        assertThat(template.venueId()).isEqualTo("venue_x");
        assertThat(template.pointId()).isEqualTo("point_x");
    }

    @Test
    void createEntitlementTemplateRejectsUnknownProviderProduct() {
        // S5：选了个不存在的商品 → 当场拒，别留到客人站在吧台前才炸。
        assertThatThrownBy(() -> service.createEntitlementTemplate(new EntitlementTemplateRequest(
                "cfg-tpl-missing", "create", List.of(), "Gift Drink", "gift_drink", 1, 0L, "active",
                "no_such_product", "mch_x", null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("GIFT_TEMPLATE_ITEM_NOT_FOUND"));
    }

    @Test
    void createEntitlementTemplateRejectsProductOfAnotherMerchant() {
        // S5：A 吧的权益绑到 B 吧的商品 → 成本记给谁全凭巧合，拒。
        ticketingRepository.barProducts.put("prod_of_b",
                new BarProductRefRecord("prod_of_b", "mch_b", 4200L, null, false, "active"));
        assertThatThrownBy(() -> service.createEntitlementTemplate(new EntitlementTemplateRequest(
                "cfg-tpl-cross", "create", List.of(), "Gift Drink", "gift_drink", 1, 0L, "active",
                "prod_of_b", "mch_x", null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("GIFT_TEMPLATE_ITEM_MERCHANT_MISMATCH"));
    }

    @Test
    void createEntitlementTemplateRejectsStockManagedProductWithoutInventoryItem() {
        // S5：要扣库存却没绑库存物料 → 核销时扣不动库，建模板阶段就拒。
        ticketingRepository.barProducts.put("prod_nostock",
                new BarProductRefRecord("prod_nostock", "mch_x", 4200L, null, true, "active"));
        assertThatThrownBy(() -> service.createEntitlementTemplate(new EntitlementTemplateRequest(
                "cfg-tpl-nostock", "create", List.of(), "Gift Drink", "gift_drink", 1, 0L, "active",
                "prod_nostock", "mch_x", null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("GIFT_TEMPLATE_ITEM_STOCK_UNMAPPED"));
    }

    @Test
    void entitlementFallsBackToIssuerMerchantWhenTemplateMerchantNull() {
        // R5 审计点②：模板 merchant_id 为空时回退出票方（向后兼容既有单模板票）。
        TestSecurityActors.asMerchantUser("mch_issuer_fb");
        try {
            Instant now = Instant.now();
            ticketingRepository.saveTicketType(new TicketTypeRecord(
                    "tt_fb", "Single Pass", "weekday_weekend_holiday", 9900L, 11900L, 12900L,
                    false, "published", null, null, now, now, null, 0, null, false, null, TicketTypeRecord.NO_COMBO_DISCOUNT));
            ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                    "tpl_fb", "Entry FB", "entry", 1, 9900L, "active",
                    null, null, null, null, now, now,
                    null, null, null, null));
            ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                    "link_fb", "tt_fb", "tpl_fb", 1, 1, 9900L, "default", 1, null, now, now));

            issueDirectlyForTest("tt_fb", new TicketIssueRequest(
                    "fb-issue", "issue", List.of(), "order_fb", "manual", List.of("qr")));

            IssuedEntitlementRecord issued = ticketingRepository.issuedEntitlements.values().stream()
                    .filter(e -> "order_fb".equals(e.orderId()))
                    .findFirst().orElseThrow();
            assertThat(issued.merchantId()).isEqualTo("mch_issuer_fb");
            assertThat(issued.venueId()).isNull();
            assertThat(issued.pointId()).isNull();
        } finally {
            TestSecurityActors.clear();
        }
    }

    @Test
    void validToUsesTicketTypeValidityDaysWithFallbackToOneDay() {
        // R5 审计点②：票种 validity_days=30 → valid_to=now+30天；为空回退 now+1天。
        Instant now = Instant.now();
        ticketingRepository.saveTicketType(new TicketTypeRecord(
                "tt_v30", "30d Pass", "weekday_weekend_holiday", 9900L, 11900L, 12900L,
                false, "published", 30, null, now, now, null, 0, null, false, null, TicketTypeRecord.NO_COMBO_DISCOUNT));
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_v30", "Entry V30", "entry", 1, 9900L, "active", null, null, null, null, now, now,
                null, null, null, null));
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_v30", "tt_v30", "tpl_v30", 1, 1, 9900L, "default", 1, null, now, now));

        Instant before30 = Instant.now();
        issueDirectlyForTest("tt_v30", new TicketIssueRequest(
                "v30-issue", "issue", List.of(), "order_v30", "manual", List.of("qr")));
        IssuedEntitlementRecord ent30 = ticketingRepository.issuedEntitlements.values().stream()
                .filter(e -> "order_v30".equals(e.orderId())).findFirst().orElseThrow();
        long days30 = ChronoUnit.DAYS.between(ent30.validFrom(), ent30.validTo());
        assertThat(days30).isEqualTo(30);
        assertThat(ent30.validTo()).isAfter(before30.plus(29, ChronoUnit.DAYS));

        // validity_days 为空 → 回退 1 天
        ticketingRepository.saveTicketType(new TicketTypeRecord(
                "tt_vnull", "Default Pass", "weekday_weekend_holiday", 9900L, 11900L, 12900L,
                false, "published", null, null, now, now, null, 0, null, false, null, TicketTypeRecord.NO_COMBO_DISCOUNT));
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_vnull", "Entry Vnull", "entry", 1, 9900L, "active", null, null, null, null, now, now,
                null, null, null, null));
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(
                "link_vnull", "tt_vnull", "tpl_vnull", 1, 1, 9900L, "default", 1, null, now, now));
        issueDirectlyForTest("tt_vnull", new TicketIssueRequest(
                "vnull-issue", "issue", List.of(), "order_vnull", "manual", List.of("qr")));
        IssuedEntitlementRecord entNull = ticketingRepository.issuedEntitlements.values().stream()
                .filter(e -> "order_vnull".equals(e.orderId())).findFirst().orElseThrow();
        assertThat(ChronoUnit.SECONDS.between(entNull.validFrom(), entNull.validTo())).isEqualTo(86_400);
    }

    // ===================== AUD-076 单店票挂酒吧 =====================
    @Test
    void singleStoreTicketSnapshotsBarAndRejectsMismatch() {
        registerBar("mch_bar_a", "话梅酒馆");
        TicketTypeRecord aTicket = createBarTicketType("aud076-a", "mch_bar_a", false);
        // 匹配 barId 下单+出票：订单/父票/权益都快照同一 bar_id。
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "aud076-a-order", "c", List.of(), aTicket.id(), null, 1, LocalDate.parse("2026-06-20"),
                "admin", "13800138000", "cust_a", java.util.Map.of(), "mch_bar_a"));
        assertThat(order.barId()).isEqualTo("mch_bar_a");
        service.issueTicketOrder(order.id(), new StatusChangeRequest("aud076-a-issue", "i", "paid offline", List.of()));
        assertThat(ticketingRepository.ticketInstances.values()).isNotEmpty().allMatch(i -> "mch_bar_a".equals(i.barId()));
        assertThat(ticketingRepository.issuedEntitlements.values()).isNotEmpty().allMatch(e -> "mch_bar_a".equals(e.barId()));

        // 篡改：barId 与票种归属不符 → 拒。
        assertThatThrownBy(() -> service.createTicketOrder(new TicketOrderRequest(
                "aud076-a-x", "c", List.of(), aTicket.id(), null, 1, LocalDate.parse("2026-06-20"),
                "admin", "13800138000", "cust_x", java.util.Map.of(), "mch_bar_wrong")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("BAR_TICKET_MISMATCH"));
        // 单店票缺 barId → 拒。
        assertThatThrownBy(() -> service.createTicketOrder(new TicketOrderRequest(
                "aud076-a-miss", "c", List.of(), aTicket.id(), null, 1, LocalDate.parse("2026-06-20"),
                "admin", "13800138000", "cust_m", java.util.Map.of(), null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("BAR_ID_REQUIRED"));
        // 通票带 barId → 拒。
        TicketTypeRecord pass = createTicketType("aud076-pass");
        service.bindTicketTypeEntitlement(pass.id(), new TicketTypeEntitlementRequest(
                "aud076-pass-bind", "b", List.of(), createTemplate("aud076-pass-tpl", 1, 9900L).id(),
                1, 1, 9900L, "default", 1, 9900L));
        assertThatThrownBy(() -> service.createTicketOrder(new TicketOrderRequest(
                "aud076-pass-x", "c", List.of(), pass.id(), null, 1, LocalDate.parse("2026-06-20"),
                "admin", "13800138000", "cust_p", java.util.Map.of(), "mch_bar_a")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("BAR_ID_NOT_ALLOWED"));
    }

    @Test
    void singleStoreTicketOrderBlockedWhenContractScopeExcludesTicketing() {
        // 合同经营范围拦截接线（票务域级）：单店票归属酒吧商户，其生效合同范围不含 ticketing → 出票拒；补上则放行。
        registerBar("mch_bar_a", "话梅酒馆");
        TicketTypeRecord aTicket = createBarTicketType("scope-enf", "mch_bar_a", false);
        com.complex.platform.contract.ContractRepository contractRepository =
                org.mockito.Mockito.mock(com.complex.platform.contract.ContractRepository.class);
        Instant now = Instant.now();
        org.mockito.Mockito.when(contractRepository.listContractsByMerchant("mch_bar_a")).thenReturn(List.of(
                new com.complex.platform.contract.ContractRecord("ctr_scope", "mch_bar_a", "subj", null, "subj",
                        LocalDate.now().minusDays(1), LocalDate.now().plusYears(1),
                        Map.of("allowedCategories", List.of("bar")), null, "none", null, Map.of(),
                        List.of(), null, null, List.of(), "effective", null, null, null, null, now, now)));
        orderService.contractScopeGuard = new com.complex.platform.contract.ContractScopeGuard(contractRepository);
        try {
            assertThatThrownBy(() -> service.createTicketOrder(new TicketOrderRequest(
                    "scope-enf-x", "c", List.of(), aTicket.id(), null, 1, LocalDate.parse("2026-06-20"),
                    "admin", "13800138000", "cust_a", java.util.Map.of(), "mch_bar_a")))
                    .isInstanceOfSatisfying(ApiException.class,
                            e -> assertThat(e.code()).isEqualTo("CONTRACT_SCOPE_VIOLATION"));
            org.mockito.Mockito.when(contractRepository.listContractsByMerchant("mch_bar_a")).thenReturn(List.of(
                    new com.complex.platform.contract.ContractRecord("ctr_scope2", "mch_bar_a", "subj", null, "subj",
                            LocalDate.now().minusDays(1), LocalDate.now().plusYears(1),
                            Map.of("allowedCategories", List.of("bar", "ticketing")), null, "none", null,
                            Map.of(), List.of(), null, null, List.of(), "effective", null, null, null, null,
                            now, now)));
            TicketOrderRecord ok = service.createTicketOrder(new TicketOrderRequest(
                    "scope-enf-ok", "c", List.of(), aTicket.id(), null, 1, LocalDate.parse("2026-06-20"),
                    "admin", "13800138000", "cust_a", java.util.Map.of(), "mch_bar_a"));
            assertThat(ok.barId()).isEqualTo("mch_bar_a");
        } finally {
            orderService.contractScopeGuard = null;
        }
    }

    @Test
    void twoBarsSameNameTicketsOrderSeparately() {
        registerBar("mch_bar_a", "话梅酒馆");
        registerBar("mch_bar_b", "夜航船");
        // 两家酒吧各发同名同价票——只靠 bar_id 区分。
        TicketTypeRecord aTicket = createBarTicketType("same-a", "mch_bar_a", false);
        TicketTypeRecord bTicket = createBarTicketType("same-b", "mch_bar_b", false);
        assertThat(aTicket.ticketName()).isEqualTo(bTicket.ticketName());
        TicketOrderRecord aOrder = service.createTicketOrder(new TicketOrderRequest(
                "same-a-order", "c", List.of(), aTicket.id(), null, 1, LocalDate.parse("2026-06-20"),
                "admin", "13800138000", "cust_a", java.util.Map.of(), "mch_bar_a"));
        TicketOrderRecord bOrder = service.createTicketOrder(new TicketOrderRequest(
                "same-b-order", "c", List.of(), bTicket.id(), null, 1, LocalDate.parse("2026-06-20"),
                "admin", "13800138000", "cust_b", java.util.Map.of(), "mch_bar_b"));
        assertThat(aOrder.barId()).isEqualTo("mch_bar_a");
        assertThat(bOrder.barId()).isEqualTo("mch_bar_b");
    }

    @Test
    void buildingSingleStoreTicketRejectsNonBarMerchant() {
        bars.put("mch_shop", new com.complex.platform.governance.MerchantRecord(
                "mch_shop", "百货商户", "retail", null, null, null, null, null, null, null,
                null, null, null, null, null, null, "active", Instant.now(), Instant.now()));
        assertThatThrownBy(() -> createBarTicketType("bad", "mch_shop", false))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("BAR_MERCHANT_INVALID"));
    }

    @Test
    void nonKioskMerchantScopeCannotSellAnotherBarSingleTicket() {
        // AUD 复核 P1-4：越权闸不再只在 kiosk 触发——商户A员工经 web（非 kiosk）渠道给别家酒吧B单店票下单也须 403。
        registerBar("mch_bar_a", "话梅酒馆");
        registerBar("mch_bar_b", "夜航船");
        TicketTypeRecord aTicket = createBarTicketType("p14-a", "mch_bar_a", false);
        TicketTypeRecord bTicket = createBarTicketType("p14-b", "mch_bar_b", false);
        TestSecurityActors.clear();
        try {
            TestSecurityActors.asMerchantUser("mch_bar_a");
            // 本店单店票经 web 渠道 OK。
            TicketOrderRecord ok = service.createTicketOrder(new TicketOrderRequest(
                    "p14-ok", "c", List.of(), aTicket.id(), null, 1, LocalDate.parse("2026-06-20"),
                    "web", "13800138000", null, java.util.Map.of(), "mch_bar_a"));
            assertThat(ok.barId()).isEqualTo("mch_bar_a");
            // 别家单店票经 web（非 kiosk）→ 越权拒（此前 kiosk-only 判断会放行=漏洞）。
            assertThatThrownBy(() -> service.createTicketOrder(new TicketOrderRequest(
                    "p14-x", "c", List.of(), bTicket.id(), null, 1, LocalDate.parse("2026-06-20"),
                    "web", "13800138000", null, java.util.Map.of(), "mch_bar_b")))
                    .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("BAR_SCOPE_FORBIDDEN"));
        } finally {
            TestSecurityActors.clear();
        }
    }

    @Test
    void singleBarTicketRejectsBindingOtherMerchantTemplate() {
        // AUD 复核 P2-5：单店票（bar_id=A）只能绑归属本店的权益模板；绑他商户 B 的模板 → 拒（出票权益 merchant/父票 bar 归属分裂）。
        registerBar("mch_bar_a", "话梅酒馆");
        TicketTypeRecord aTicket = createBarTicketType("p25-a", "mch_bar_a", false);
        Instant now = Instant.now();
        // 他商户 B 归属的模板 → 绑到 bar=A 单店票被拒。
        EntitlementTemplateRecord otherMerchantTpl = ticketingRepository.saveEntitlementTemplate(
                new EntitlementTemplateRecord("tpl_p25_b", "他店权益", "entry", 1, 100L, "active",
                        "mch_bar_b", null, null, null, now, now, null, null, null, null));
        assertThatThrownBy(() -> service.bindTicketTypeEntitlement(aTicket.id(), new TicketTypeEntitlementRequest(
                "p25-badbind", "bind", List.of(), otherMerchantTpl.id(), 1, 1, 100L, "default", 1, 9900L)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("BAR_TICKET_TEMPLATE_MERCHANT_MISMATCH"));
        // 本店 A 归属的模板可绑（归属一致）。
        EntitlementTemplateRecord ownTpl = ticketingRepository.saveEntitlementTemplate(
                new EntitlementTemplateRecord("tpl_p25_a", "本店权益", "entry", 1, 100L, "active",
                        "mch_bar_a", null, null, null, now, now, null, null, null, null));
        assertThat(service.bindTicketTypeEntitlement(aTicket.id(), new TicketTypeEntitlementRequest(
                "p25-okbind", "bind", List.of(), ownTpl.id(), 1, 1, 100L, "default", 1, 9900L))).isNotNull();
    }

    @Test
    void barTeardownStopsSaleAndExpiresSoldTickets() {
        registerBar("mch_bar_c", "撤场酒馆");
        TicketTypeRecord tt = createBarTicketType("teardown", "mch_bar_c", false); // 不可退
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "teardown-order", "c", List.of(), tt.id(), null, 1, LocalDate.parse("2026-06-20"),
                "admin", "13800138000", "cust_teardown", java.util.Map.of(), "mch_bar_c"));
        service.issueTicketOrder(order.id(), new StatusChangeRequest("teardown-issue", "i", "paid offline", List.of()));
        assertThat(ticketingRepository.issuedEntitlements.values()).isNotEmpty().allMatch(e -> "active".equals(e.status()));

        // 撤场=商户存档：停售新票。
        bars.put("mch_bar_c", bars.get("mch_bar_c").withArchive(Instant.now(), "admin", Instant.now()));
        assertThatThrownBy(() -> service.createTicketOrder(new TicketOrderRequest(
                "teardown-order2", "c", List.of(), tt.id(), null, 1, LocalDate.parse("2026-06-20"),
                "admin", "13800138000", "cust2", java.util.Map.of(), "mch_bar_c")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("TICKET_BAR_OFFLINE"));

        // 已售提前到期 → 方案2 到期规则：不可退直接作废。
        Instant cut = Instant.now();
        int moved = service.expireBarSingleStoreTickets("mch_bar_c", cut);
        assertThat(moved).isGreaterThanOrEqualTo(1);
        service.processExpiredPaidEntitlements(cut.plusSeconds(60));
        assertThat(ticketingRepository.issuedEntitlements.values()).isNotEmpty().allMatch(e -> "forfeited".equals(e.status()));
    }

    private com.complex.platform.governance.MerchantRecord registerBar(String id, String name) {
        com.complex.platform.governance.MerchantRecord bar = new com.complex.platform.governance.MerchantRecord(
                id, name, "bar", null, null, null, null, null, null, null,
                null, null, null, null, null, null, "active", Instant.now(), Instant.now());
        bars.put(id, bar);
        return bar;
    }

    private TicketTypeRecord createBarTicketType(String key, String barId, boolean refundable) {
        TicketTypeRecord tt = service.createTicketType(new TicketTypeRequest(
                key, "create", List.of(), "周末单人票",
                "weekday_weekend_holiday", 9900L, 9900L, 9900L, false, "active", null, null, refundable, barId, null, null, null));
        EntitlementTemplateRecord tpl = createTemplate(key + "-tpl", 1, 9900L);
        service.bindTicketTypeEntitlement(tt.id(), new TicketTypeEntitlementRequest(
                key + "-bind", "bind", List.of(), tpl.id(), 1, 1, 9900L, "default", 1, 9900L));
        // AUD-015：走真实发布闸上架（已有绑定）。
        return service.publishTicketType(tt.id(), new StatusChangeRequest(
                key + "-pub", "publish", "publish for test", List.of()));
    }

    private TicketTypeRecord createTicketType(String key) {
        // 方案2：测试票种默认设 refundable=true，退款链测试才能走通（生产默认 false，另有 refundable=false 专项用例）。
        TicketTypeRecord created = service.createTicketType(new TicketTypeRequest(
                key,
                "create",
                List.of(),
                "P0 Pass " + key,
                "weekday_weekend_holiday",
                9900L,
                11900L,
                12900L,
                false,
                "active",
                null,
                null,
                true
        , null, null, null, null));
        return publishedForTest(created);
    }

    /** AUD-015：建票固定 draft；下单/退款/核销用例直接仓储级置 published 跳过发布闸（发布闸另有专项用例）。 */
    private TicketTypeRecord publishedForTest(TicketTypeRecord created) {
        return ticketingRepository.saveTicketType(new TicketTypeRecord(
                created.id(), created.ticketName(), created.baseCalendarPriceMode(),
                created.weekdayPriceCent(), created.weekendPriceCent(), created.holidayPriceCent(),
                created.stockRequired(), "published", created.validityDays(), created.refreshIntervalSeconds(),
                created.createdAt(), created.updatedAt(), created.stockQuantity(), created.soldQuantity(),
                created.ticketCategory(), created.refundable(), created.barId(), TicketTypeRecord.NO_COMBO_DISCOUNT));
    }

    private void assertDerivedPriceRuleAmount(String key, long basePriceCent, BigDecimal discountRate, long amountCent) {
        TicketTypeRecord ticketType = service.createTicketType(new TicketTypeRequest(
                key + "-ticket",
                "create",
                List.of(),
                "P0 Decimal Pass " + key,
                "weekday_weekend_holiday",
                basePriceCent,
                basePriceCent,
                basePriceCent,
                false,
                "active"
        , null, null, null, null, null, null, null));
        ticketType = publishedForTest(ticketType);
        TicketPriceRuleRecord rule = service.createTicketPriceRule(new TicketPriceRuleRequest(
                key + "-rule",
                "derive",
                List.of(),
                ticketType.id(),
                "channel",
                "derive_from_calendar_price",
                null,
                null,
                null,
                discountRate,
                "web",
                Map.of()
        ));

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                key + "-order",
                "create order",
                List.of(),
                ticketType.id(),
                rule.id(),
                1,
                LocalDate.parse("2026-06-15"),
                "web",
                "13800138000",
                "customer_" + key
        ));

        assertThat(rule.discountRate()).isEqualByComparingTo(discountRate);
        assertThat(order.amountCent()).isEqualTo(amountCent);
    }

    private EntitlementTemplateRecord createTemplate(String key, int totalTimes, long valueCent) {
        // 审计断层①后:建模板经 service 已强制绑商户。本 helper 专为「出票方回退」契约测试直存
        // 一条 merchant_id=null 的模板(绕过 service 校验),使发放时权益回退到出票方 merchant scope。
        Instant now = Instant.now();
        return ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(
                "tpl_" + key, "P0 Entry " + key, "entry", totalTimes, valueCent, "active",
                null, null, null, null, now, now, null, null, null, null));
    }

    // 1.8 客人端我的票：本人订单列票实例（安全投影含票名/剩余次数）；他人 403；不存在订单 404。
    @Test
    void listCustomerOrderTicketsReturnsProjectionForOwnerOnly() {
        Instant now = Instant.now();
        TicketTypeRecord ticketType = createTicketType("mytickets");
        ticketingRepository.saveTicketOrder(new TicketOrderRecord(
                "order_mytickets", "TO-MYT", "customer_mine", ticketType.id(), null, 2,
                LocalDate.parse("2026-06-20"), "miniapp", "13800138000", 19800L, null, 19800L, 0L,
                "CNY", "paid", "issued", "created", now, now, null, null, null));
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_myt_1", "P0-MYT1", ticketType.id(), "order_mytickets", null, "ticket",
                null, null, null, 2, 1, null, "active", now, now.plusSeconds(86400), now, now, 1, null,
                null, null, null, null, null, 0, null, null));
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_myt_2", "P0-MYT2", ticketType.id(), "order_mytickets", null, "ticket",
                null, null, null, 1, 0, null, "active", now, now.plusSeconds(86400), now, now, 1, null,
                null, null, null, null, null, 0, null, null));

        List<CustomerTicketResponse> tickets = service.listCustomerOrderTickets("order_mytickets", "customer_mine");
        assertThat(tickets).hasSize(2);
        assertThat(tickets.getFirst().ticketName()).isEqualTo(ticketType.ticketName());
        assertThat(tickets.getFirst().remainingTimes()).isEqualTo(1); // 2次已用1
        assertThat(tickets.get(1).remainingTimes()).isEqualTo(1);

        assertThatThrownBy(() -> service.listCustomerOrderTickets("order_mytickets", "customer_other"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CUSTOMER_ORDER_FORBIDDEN"));
        assertThatThrownBy(() -> service.listCustomerOrderTickets("order_ghost", "customer_mine"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("TICKET_ORDER_NOT_FOUND"));
    }

    // ── 批2 新增:AUD-004/015/011 ──

    @Test
    void createTicketTypePersistsValidityAndRefreshAndStartsAsDraft() {
        TicketTypeRecord tt = service.createTicketType(new TicketTypeRequest(
                "b2-validity", "create", List.of(), "周票", "weekday_weekend_holiday",
                9900L, 11900L, 12900L, false, "active", null, null, true, null, 7, 30, null));
        assertThat(tt.status()).isEqualTo("draft");
        assertThat(tt.validityDays()).isEqualTo(7);
        assertThat(tt.refreshIntervalSeconds()).isEqualTo(30);
    }

    @Test
    void createTicketTypeRejectsInvalidValidityAndRefresh() {
        assertThatThrownBy(() -> service.createTicketType(new TicketTypeRequest(
                "b2-badvalidity", "create", List.of(), "坏有效期", "weekday_weekend_holiday",
                9900L, 11900L, 12900L, false, null, null, null, true, null, 0, null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getMessage()).contains("有效天数"));
        assertThatThrownBy(() -> service.createTicketType(new TicketTypeRequest(
                "b2-badrefresh", "create", List.of(), "坏刷新", "weekday_weekend_holiday",
                9900L, 11900L, 12900L, false, null, null, null, true, null, 7, 12, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getMessage()).contains("刷新间隔秒数"));
    }

    // 2026-07-28：基础日历价模式收白名单——业务口径只有「平日/周末/节假日三类基础价」一种，
    // 前端曾误传 "calendar"（openapi 枚举里根本没有），后端只 requireText 全盘放行 → 脏值入库。
    @Test
    void createTicketTypeRejectsUnknownBaseCalendarPriceMode() {
        assertThatThrownBy(() -> service.createTicketType(new TicketTypeRequest(
                "bcpm-bad", "create", List.of(), "坏日历价模式", "calendar",
                9900L, 11900L, 12900L, false, null, null, null, true, null, 1, null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getMessage()).contains("base_calendar_price_mode"));
    }

    // 2026-07-18 拍板：验证二维码默认 15 秒动态刷新，白名单放宽为 {10,15,30,60}（V300）。
    @Test
    void createTicketTypeAcceptsFifteenSecondRefresh() {
        TicketTypeRecord tt = service.createTicketType(new TicketTypeRequest(
                "qr15-ok", "create", List.of(), "十五秒票", "weekday_weekend_holiday",
                9900L, 11900L, 12900L, false, null, null, null, true, null, 1, 15, null));
        assertThat(tt.refreshIntervalSeconds()).isEqualTo(15);
    }

    // #172(2026-07-19 拍板)：白名单改「5 秒为单位、最长 60」——5 秒现为合法（票种休眠列亦按同式校验）。
    @Test
    void createTicketTypeAcceptsFiveSecondRefresh() {
        TicketTypeRecord tt = service.createTicketType(new TicketTypeRequest(
                "qr5-ok", "create", List.of(), "五秒票", "weekday_weekend_holiday",
                9900L, 11900L, 12900L, false, null, null, null, true, null, 1, 5, null));
        assertThat(tt.refreshIntervalSeconds()).isEqualTo(5);
    }

    // #172：非 5 倍数（如 12）仍被拒。
    @Test
    void createTicketTypeRejectsNonMultipleOfFiveRefresh() {
        assertThatThrownBy(() -> service.createTicketType(new TicketTypeRequest(
                "qr12-bad", "create", List.of(), "十二秒票", "weekday_weekend_holiday",
                9900L, 11900L, 12900L, false, null, null, null, true, null, 1, 12, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getMessage()).contains("刷新间隔秒数"));
    }

    // #172(2026-07-19 拍板)：全场统一动态码刷新间隔端点——边界 5 通过 / 60 通过 / 65 拒 / 12 拒（非 5 倍数）；统一读一套。
    @Test
    void updateQrRefreshConfigAcceptsFiveAndSixtyRejectsOutOfRangeAndNonMultiple() {
        assertThat(service.updateQrRefreshConfig(new QrRefreshConfigRequest("qrc-5", 5)).refreshIntervalSeconds()).isEqualTo(5);
        assertThat(service.updateQrRefreshConfig(new QrRefreshConfigRequest("qrc-60", 60)).refreshIntervalSeconds()).isEqualTo(60);
        assertThat(service.getQrRefreshConfig().refreshIntervalSeconds()).isEqualTo(60);
        assertThatThrownBy(() -> service.updateQrRefreshConfig(new QrRefreshConfigRequest("qrc-65", 65)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getMessage()).contains("刷新间隔秒数"));
        assertThatThrownBy(() -> service.updateQrRefreshConfig(new QrRefreshConfigRequest("qrc-12", 12)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getMessage()).contains("刷新间隔秒数"));
    }

    // 票种未配置刷新秒数 → 凭证窗口回退新默认 15 秒（不再是 60）。
    @Test
    void credentialWindowDefaultsToFifteenSecondsWhenTicketTypeUnset() {
        IssuedEntitlementRecord entitlement = issueSingleEntitlement("qr15-default");
        TicketCredentialResponse credential = service.getTicketCredential(entitlement.id());
        long windowSeconds = java.time.Duration.between(credential.issuedAt(), credential.expiresAt()).getSeconds();
        assertThat(windowSeconds).isEqualTo(15L);
    }

    @Test
    void weeklyMonthlyYearlyTicketsIssueWithConfiguredValidity() {
        for (int days : new int[]{7, 30, 365}) {
            String key = "b2-days-" + days;
            TicketTypeRecord tt = service.createTicketType(new TicketTypeRequest(
                    key, "create", List.of(), "Pass " + days, "weekday_weekend_holiday",
                    9900L, 11900L, 12900L, false, null, null, null, true, null, days, null, null));
            EntitlementTemplateRecord tpl = createTemplate(key + "-tpl", 1, 9900L);
            service.bindTicketTypeEntitlement(tt.id(), new TicketTypeEntitlementRequest(
                    key + "-bind", "bind", List.of(), tpl.id(), 1, 1, 9900L, "default", 1, 9900L));
            service.publishTicketType(tt.id(), new StatusChangeRequest(key + "-pub", "p", "publish", List.of()));
            TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                    key + "-order", "c", List.of(), tt.id(), null, 1, LocalDate.parse("2026-06-20"),
                    "admin", "13800138000", "cust_" + key));
            IssuedTicketResponse issued = service.issueTicketOrder(order.id(), new StatusChangeRequest(
                    key + "-issue", "i", "issue", List.of()));
            IssuedEntitlementRecord ent = ticketingRepository
                    .findIssuedEntitlementByCode(issued.issuedEntitlements().getFirst().entitlementCode())
                    .orElseThrow();
            java.time.Instant expected = java.time.Instant.now().plus(days, java.time.temporal.ChronoUnit.DAYS);
            assertThat(ent.validTo()).isBetween(expected.minusSeconds(300), expected.plusSeconds(300));
        }
    }

    @Test
    void publishGateRequiresEntitlementBindingAndDraftIsNotSellable() {
        TicketTypeRecord tt = service.createTicketType(new TicketTypeRequest(
                "b2-gate", "create", List.of(), "闸口票", "weekday_weekend_holiday",
                9900L, 11900L, 12900L, false, "active", null, null, true, null, null, null, null));
        assertThat(tt.status()).isEqualTo("draft");
        // 无权益绑定不可发布(AUD-015:公开可售的票必须能出票)。
        assertThatThrownBy(() -> service.publishTicketType(tt.id(), new StatusChangeRequest(
                "b2-gate-pub", "p", "publish", List.of())))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("TICKET_TYPE_ENTITLEMENT_REQUIRED"));
        // draft 不可下单(直传 id 也拒)。
        assertThatThrownBy(() -> service.createTicketOrder(new TicketOrderRequest(
                "b2-gate-order", "c", List.of(), tt.id(), null, 1, LocalDate.parse("2026-06-20"),
                "admin", "13800138000", "cust_gate")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("TICKET_TYPE_NOT_SELLABLE"));
        // 绑定后经发布闸上架,即可下单。
        EntitlementTemplateRecord tpl = createTemplate("b2-gate-tpl", 1, 9900L);
        service.bindTicketTypeEntitlement(tt.id(), new TicketTypeEntitlementRequest(
                "b2-gate-bind", "bind", List.of(), tpl.id(), 1, 1, 9900L, "default", 1, 9900L));
        assertThat(service.publishTicketType(tt.id(), new StatusChangeRequest(
                "b2-gate-pub2", "p", "publish", List.of())).status()).isEqualTo("published");
        assertThat(service.createTicketOrder(new TicketOrderRequest(
                "b2-gate-order2", "c", List.of(), tt.id(), null, 1, LocalDate.parse("2026-06-20"),
                "admin", "13800138000", "cust_gate"))).isNotNull();
    }

    @Test
    void expiryStillProcessesRemainingTicketsAfterPartialManualRefund() {
        java.time.Instant past = java.time.Instant.parse("2020-01-01T00:00:00Z");
        java.time.Instant now = java.time.Instant.parse("2026-06-22T00:00:00Z");
        seedRefundableType("tt_part");
        ticketingRepository.ticketOrders.put("o_part", new TicketOrderRecord(
                "o_part", "ON-PART", "c1", "tt_part", null, 2, LocalDate.parse("2026-06-20"),
                "web", "13800138000", 19800L, null, 19800L, 0L, "CNY", "paid", "issued", "paid", past, past, null, null, null));
        // 实例1已被人工整票退款(refunded);实例2完全未核销、已到期。
        seedInstance("inst_p1", "o_part", "tt_part", 1, 1, 1, "refunded", null);
        seedInstance("inst_p2", "o_part", "tt_part", 2, 1, 1, "active", null);
        ticketingRepository.issuedEntitlements.put("e_p1",
                seedEntitlement("e_p1", "o_part", "tt_part", 1, 0, "refunded", past, "inst_p1", 1));
        ticketingRepository.issuedEntitlements.put("e_p2",
                seedEntitlement("e_p2", "o_part", "tt_part", 1, 0, "active", past, "inst_p2", 1));
        ticketingRepository.refunds.put("ticket_refund_manual_part", new TicketRefundRecord(
                "ticket_refund_manual_part", "o_part", 9900L, "manual partial refund", "original",
                "approved", "admin", "admin", past, past, past, null));

        int processed = service.processExpiredPaidEntitlements(now);

        // AUD-011:此前「订单已有退款」整单跳过——实例2永远既不退也不作废;现按实例处置,剩余整张未核销票照旧被处理。
        // 处置方式=作废(business-enum「售票与退款原则」不认「到期未用」为退款理由),不再产生到期自动退款单。
        assertThat(processed).isEqualTo(1);
        assertThat(ticketingRepository.refunds.values())
                .noneMatch(r -> r.id().startsWith("ticket_refund_auto_expiry_"));
        assertThat(ticketingRepository.issuedEntitlements.get("e_p2").status()).isEqualTo("forfeited");
        assertThat(ticketingRepository.findTicketInstanceById("inst_p2")).get()
                .extracting(TicketInstanceRecord::status).isEqualTo("active");
    }

    @Test
    void expiryLeavesEntitlementsOfManualRefundPendingInstancesToRefundChain() {
        // 复核修正(P1-B):人工退款「申请即冻结」只冻父实例,子权益仍 active——到期作业作废它会让
        // 审批冻结命中0行、退款单永远批不了。该类权益必须留给退款链。
        java.time.Instant past = java.time.Instant.parse("2020-01-01T00:00:00Z");
        java.time.Instant now = java.time.Instant.parse("2026-06-22T00:00:00Z");
        seedRefundableType("tt_rp");
        ticketingRepository.ticketOrders.put("o_rp", new TicketOrderRecord(
                "o_rp", "ON-RP", "c1", "tt_rp", null, 1, LocalDate.parse("2026-06-20"),
                "web", "13800138000", 9900L, null, 9900L, 0L, "CNY", "paid", "issued", "paid", past, past, null, null, null));
        seedInstance("inst_rp", "o_rp", "tt_rp", 1, 1, 1, "refund_pending", null);
        ticketingRepository.issuedEntitlements.put("e_rp",
                seedEntitlement("e_rp", "o_rp", "tt_rp", 1, 0, "active", past, "inst_rp", 1));

        int processed = service.processExpiredPaidEntitlements(now);

        assertThat(processed).isEqualTo(0);
        assertThat(ticketingRepository.issuedEntitlements.get("e_rp").status()).isEqualTo("active");
        assertThat(ticketingRepository.refunds.values()).noneMatch(r -> "o_rp".equals(r.targetId()));
        assertThat(ticketingRepository.findTicketInstanceById("inst_rp")).get()
                .extracting(TicketInstanceRecord::status).isEqualTo("refund_pending");
    }

    private IssuedEntitlementRecord issueSingleEntitlement(String key) {
        TicketTypeRecord ticketType = createTicketType(key + "-ticket");
        EntitlementTemplateRecord template = createTemplate(key + "-template", 2, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                key + "-bind",
                "bind",
                List.of(),
                template.id(),
                1,
                2,
                9900L,
                "default",
                1,
                9900L
        ));
        // AUD-019：direct 出票已退役，统一走真实订单链（admin 渠道免支付出票）。
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                key + "-order",
                "create",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "admin",
                "13800138000",
                "customer_" + key
        ));
        IssuedTicketResponse response = service.issueTicketOrder(order.id(), new StatusChangeRequest(
                key + "-issue",
                "issue",
                "issue",
                List.of()
        ));
        String entitlementCode = response.issuedEntitlements().getFirst().entitlementCode();
        return ticketingRepository.findIssuedEntitlementByCode(entitlementCode).orElseThrow();
    }

    /** F-TXN-03 piece② 测试构造：直接构造一条已发权益（用于到期/作废场景 seed）。 */
    private static IssuedEntitlementRecord seedEntitlement(String id, String orderId, String ticketTypeId,
            int totalTimes, int usedTimes, String status, java.time.Instant validTo) {
        java.time.Instant t0 = validTo.minusSeconds(86400);
        return new IssuedEntitlementRecord(
                id, "CODE_" + id, ticketTypeId, orderId, "tmpl_" + id, "general", "mch", "venue", "point",
                totalTimes, usedTimes, 9900L, status, t0, validTo, t0, t0, 1, 9900L,
                null, null, null, null, null, 0, null, null);
    }

    // ── 方案2 整票退款测试脚手架 ──
    /** 播种一个 refundable=true 的票种（直接入库，供直接 seed 的退款/到期用例）。 */
    private void seedRefundableType(String id) {
        seedTicketType(id, true, null);
    }

    private void seedTicketType(String id, boolean refundable, Integer stockQuantity) {
        java.time.Instant t0 = java.time.Instant.parse("2026-06-19T00:00:00Z");
        ticketingRepository.saveTicketType(new TicketTypeRecord(
                id, "T " + id, "weekday_weekend_holiday", 9900L, 11900L, 12900L, false, "active",
                null, null, t0, t0, stockQuantity, stockQuantity == null ? 0 : stockQuantity, null, refundable, null, TicketTypeRecord.NO_COMBO_DISCOUNT));
    }

    /** 播种一张父票实例。 */
    private void seedInstance(String id, String orderId, String ticketTypeId, int seq, int numPeople,
            int totalTimes, String status, java.time.Instant firstUsedAt) {
        java.time.Instant t0 = java.time.Instant.parse("2026-06-19T00:00:00Z");
        ticketingRepository.saveTicketInstance(new TicketInstanceRecord(
                id, "TN_" + id, orderId, ticketTypeId, seq, numPeople, totalTimes,
                "c1", "c1", status, firstUsedAt, null, t0, t0));
    }

    /** 播种一条挂父票的权益（parentTicketId+slotIndex）。 */
    private static IssuedEntitlementRecord seedEntitlement(String id, String orderId, String ticketTypeId,
            int totalTimes, int usedTimes, String status, java.time.Instant validTo,
            String parentTicketId, int slotIndex) {
        java.time.Instant t0 = validTo.minusSeconds(86400);
        return new IssuedEntitlementRecord(
                id, "CODE_" + id, ticketTypeId, orderId, "tmpl_" + id, "general", "mch", "venue", "point",
                totalTimes, usedTimes, 9900L, status, t0, validTo, t0, t0, 1, 9900L,
                null, null, null, null, parentTicketId, slotIndex, null, null);
    }

    private IssuedEntitlementRecord issueOrderEntitlement(String key) {
        TicketTypeRecord ticketType = createTicketType(key + "-ticket");
        EntitlementTemplateRecord template = createTemplate(key + "-template", 2, 9900L);
        service.bindTicketTypeEntitlement(ticketType.id(), new TicketTypeEntitlementRequest(
                key + "-bind",
                "bind",
                List.of(),
                template.id(),
                1,
                2,
                9900L,
                "default",
                1,
                9900L
        ));
        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                key + "-order",
                "create",
                List.of(),
                ticketType.id(),
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "admin",
                "13800138000",
                "customer_" + key
        ));
        IssuedTicketResponse response = service.issueTicketOrder(order.id(), new StatusChangeRequest(
                key + "-issue-order",
                "issue",
                "issue",
                List.of()
        ));
        return ticketingRepository.findIssuedEntitlementByCode(response.issuedEntitlements().getFirst().entitlementCode()).orElseThrow();
    }

    /** AUD-019：生产 direct 出票端点已退役；测试保留等价内核入口（无订单 fan-out=1 张）供核销/归属/有效期用例复用。 */
    private IssuedTicketResponse issueDirectlyForTest(String ticketTypeId, TicketIssueRequest request) {
        return orderService.issueEntitlements(ticketTypeId, request);
    }

    // BSM:806 每笔退款须写明责任主体：退款用例统一记到内存仓里在用的测试主体（理由按「票种规则明确允许」）。
    private static final String REFUND_ENTITY = "be_refund_test";

    private static TicketRefundRequest refundRequest(String key, String note, List<String> evidence, String targetId,
            Long amountCent, String reason, String channel) {
        return new TicketRefundRequest(key, note, evidence, targetId, amountCent, reason, channel, null, null,
                "ticket_type_rule", REFUND_ENTITY);
    }

    private static TicketRefundRequest refundOfInstances(String key, String note, List<String> evidence, String targetId,
            Long amountCent, String reason, String channel, List<String> ticketInstanceIds) {
        return new TicketRefundRequest(key, note, evidence, targetId, amountCent, reason, channel, null, ticketInstanceIds,
                "ticket_type_rule", REFUND_ENTITY);
    }

    private static final class InMemoryTicketingRepository implements TicketingRepository {
        @Override public boolean isActiveBusinessEntity(String id) {
            return REFUND_ENTITY.equals(id);
        }
        final Map<String, List<String>> ticketProjects = new LinkedHashMap<>();
        @Override public List<String> listTicketTypeProjectIds(String ticketTypeId) {
            return ticketProjects.getOrDefault(ticketTypeId, List.of());
        }
        @Override public void setEntitlementTemplateArchiveReason(String id, String reason) { }
        @Override public void setTicketTypeArchiveReason(String id, String reason) { }
        @Override public void setPriceRuleArchiveReason(String id, String reason) { }
        // #172：全场统一动态码刷新间隔（可设，验票发凭证读它，替代票种级取值）。
        int qrRefreshIntervalSeconds = 15;
        @Override public int findQrRefreshIntervalSeconds() { return qrRefreshIntervalSeconds; }
        @Override public void upsertQrRefreshIntervalSeconds(int seconds, String updatedBy, java.time.Instant now) { this.qrRefreshIntervalSeconds = seconds; }
        // S5：提供方商品目录（bar_item_ref = bar_products.id）与设备归属（K16 判定核销门店）。
        final Map<String, BarProductRefRecord> barProducts = new LinkedHashMap<>();
        final Map<String, String> deviceMerchants = new LinkedHashMap<>();
        @Override public java.util.Optional<BarProductRefRecord> findBarProductRef(String id) {
            return java.util.Optional.ofNullable(barProducts.get(id));
        }
        @Override public java.util.Optional<String> findTerminalDeviceMerchantId(String deviceId) {
            return java.util.Optional.ofNullable(deviceMerchants.get(deviceId));
        }
        /** 商户业态（V348 五值）。K17「每吧至少一杯」按业态挑出酒吧。 */
        final Map<String, String> merchantTypes = new LinkedHashMap<>();
        @Override public List<TicketTypeProviderEntitlementRow> listTicketTypeProviderEntitlements(String ticketTypeId) {
            return ticketTypeEntitlements.values().stream()
                    .filter(binding -> ticketTypeId.equals(binding.ticketTypeId()))
                    .map(binding -> {
                        EntitlementTemplateRecord template = entitlementTemplates.get(binding.entitlementTemplateId());
                        return template == null ? null : new TicketTypeProviderEntitlementRow(
                                template.id(), template.name(), template.entitlementType(), template.merchantId(),
                                template.merchantId(), merchantTypes.get(template.merchantId()),
                                binding.quantity(), binding.numPeople());
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }
        private final Map<String, TicketTypeRecord> ticketTypes = new LinkedHashMap<>();
        private final Map<String, EntitlementTemplateRecord> entitlementTemplates = new LinkedHashMap<>();
        private final Map<String, TicketTypeEntitlementRecord> ticketTypeEntitlements = new LinkedHashMap<>();
        private final Map<String, IssuedEntitlementRecord> issuedEntitlements = new LinkedHashMap<>();
        private final Map<String, TicketInstanceRecord> ticketInstances = new LinkedHashMap<>();
        // AUD-066 可控场次（测试用）：非 null 时 resolveOpenSessionForVenue 返回它（模拟当前 open 场次），否则回退自然日。
        String openSessionId = null;
        private final Map<String, VerificationRecord> verifications = new LinkedHashMap<>();
        private final Map<String, TicketPriceRuleRecord> priceRules = new LinkedHashMap<>();
        private final Map<String, TicketDynamicPricingRuleRecord> dynamicPricingRules = new LinkedHashMap<>();
        private final Map<String, TicketOrderRecord> ticketOrders = new LinkedHashMap<>();
        private final Map<String, TicketPaymentIntentRecord> paymentIntents = new LinkedHashMap<>();
        private final Map<String, TicketCredentialRecord> credentials = new LinkedHashMap<>();
        private final Map<String, TicketRefundRecord> refunds = new LinkedHashMap<>();
        private final Map<String, TicketRefundLineRecord> refundLines = new LinkedHashMap<>();
        private final List<String> releasedStock = new ArrayList<>();
        private int entitlementTemplateSaveCount;

        @Override
        public void releaseTicketStock(String ticketTypeId, int quantity) {
            releasedStock.add(ticketTypeId + ":" + quantity);
        }

        @Override
        public TicketTypeRecord saveTicketType(TicketTypeRecord ticketType) {
            ticketTypes.put(ticketType.id(), ticketType);
            return ticketType;
        }

        @Override
        public List<TicketTypeRecord> listTicketTypes(boolean includeArchived) {
            List<TicketTypeRecord> out = new ArrayList<>();
            for (TicketTypeRecord t : ticketTypes.values()) {
                if ("approved".equals(t.deleteStatus())) {
                    continue;
                }
                if (!includeArchived && t.archivedAt() != null) {
                    continue;
                }
                out.add(t);
            }
            return out;
        }

        @Override
        public void deleteTicketType(String id) {
            ticketTypes.remove(id);
        }

        long ticketTypeReferenceCount;

        @Override
        public long countTicketTypeReferences(String id) {
            return ticketTypeReferenceCount;
        }

        @Override
        public Optional<TicketTypeRecord> findTicketTypeById(String id) {
            return Optional.ofNullable(ticketTypes.get(id));
        }

        @Override
        public EntitlementTemplateRecord saveEntitlementTemplate(EntitlementTemplateRecord template) {
            entitlementTemplateSaveCount += 1;
            entitlementTemplates.put(template.id(), template);
            return template;
        }

        @Override
        public List<EntitlementTemplateRecord> listEntitlementTemplates(boolean includeArchived) {
            List<EntitlementTemplateRecord> out = new ArrayList<>();
            for (EntitlementTemplateRecord t : entitlementTemplates.values()) {
                if ("approved".equals(t.deleteStatus())) {
                    continue;
                }
                if (!includeArchived && t.archivedAt() != null) {
                    continue;
                }
                out.add(t);
            }
            return out;
        }

        @Override
        public void deleteEntitlementTemplate(String id) {
            entitlementTemplates.remove(id);
        }

        @Override
        public long countTemplateReferences(String id) {
            long count = 0;
            for (TicketTypeEntitlementRecord b : ticketTypeEntitlements.values()) {
                if (id.equals(b.entitlementTemplateId())) {
                    count++;
                }
            }
            for (IssuedEntitlementRecord ie : issuedEntitlements.values()) {
                if (id.equals(ie.entitlementTemplateId())) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public Optional<EntitlementTemplateRecord> findEntitlementTemplateById(String id) {
            return Optional.ofNullable(entitlementTemplates.get(id));
        }

        @Override
        public TicketTypeEntitlementRecord saveTicketTypeEntitlement(TicketTypeEntitlementRecord entitlement) {
            ticketTypeEntitlements.put(entitlement.id(), entitlement);
            return entitlement;
        }

        @Override
        public List<TicketTypeEntitlementRecord> listTicketTypeEntitlements(String ticketTypeId) {
            return ticketTypeEntitlements.values().stream()
                    .filter(entitlement -> ticketTypeId.equals(entitlement.ticketTypeId()))
                    .toList();
        }

        @Override
        public IssuedEntitlementRecord saveIssuedEntitlement(IssuedEntitlementRecord entitlement) {
            issuedEntitlements.put(entitlement.id(), entitlement);
            return entitlement;
        }

        @Override
        public TicketInstanceRecord saveTicketInstance(TicketInstanceRecord instance) {
            ticketInstances.put(instance.id(), instance);
            return instance;
        }

        @Override
        public Optional<TicketInstanceRecord> findTicketInstanceById(String id) {
            return Optional.ofNullable(ticketInstances.get(id));
        }

        @Override
        public List<TicketInstanceRecord> listTicketInstances() {
            return new ArrayList<>(ticketInstances.values());
        }

        @Override
        public List<TicketInstanceRecord> listTicketInstancesByOrderId(String orderId) {
            List<TicketInstanceRecord> out = new ArrayList<>();
            for (TicketInstanceRecord ti : ticketInstances.values()) {
                if (orderId != null && orderId.equals(ti.orderId())) {
                    out.add(ti);
                }
            }
            return out;
        }

        @Override
        public int markTicketInstanceFirstUse(String id, String sessionKey, java.time.Instant now) {
            TicketInstanceRecord ti = ticketInstances.get(id);
            if (ti == null) {
                return 1; // 无父票记录=无可抢占冲突，放行（生产父票必存在）
            }
            if (ti.firstSessionKey() != null && !ti.firstSessionKey().equals(sessionKey)) {
                return 0; // AUD 复核 P2-6：已被别的场次抢占
            }
            ticketInstances.put(id, new TicketInstanceRecord(
                    ti.id(), ti.ticketNo(), ti.orderId(), ti.ticketTypeId(), ti.seqInOrder(), ti.numPeople(),
                    ti.totalTimes(), ti.buyerCustomerId(), ti.currentHolderCustomerId(), ti.status(),
                    ti.firstUsedAt() == null ? now : ti.firstUsedAt(),
                    sessionKey,
                    ti.createdAt(), now));
            return 1;
        }

        @Override
        public Optional<String> resolveOpenSessionForVenue(String venueId, java.time.Instant now) {
            return Optional.ofNullable(openSessionId);
        }

        @Override
        public Optional<IssuedEntitlementRecord> findIssuedEntitlementByCode(String entitlementCode) {
            return issuedEntitlements.values().stream()
                    .filter(entitlement -> entitlementCode.equals(entitlement.entitlementCode()))
                    .findFirst();
        }

        @Override
        public IssuedEntitlementRecord updateIssuedEntitlement(IssuedEntitlementRecord entitlement) {
            issuedEntitlements.put(entitlement.id(), entitlement);
            return entitlement;
        }

        @Override
        public VerificationRecord saveVerification(VerificationRecord verification) {
            verifications.put(verification.id(), verification);
            return verification;
        }

        @Override
        public List<VerificationRecord> listVerifications() {
            return new ArrayList<>(verifications.values());
        }

        @Override
        public boolean hasVerifiedBarEntrySince(String parentTicketId, int slotIndex, String providerMerchantId, java.time.Instant since) {
            // 148-R1：与 JdbcTicketingRepository 同口径（同票同人份、这家酒吧的入场类权益、核销成功、自 since 起）。
            return parentTicketId != null && verifications.values().stream().anyMatch(v -> {
                IssuedEntitlementRecord e = issuedEntitlements.get(v.entitlementId());
                return e != null && parentTicketId.equals(e.parentTicketId()) && e.slotIndex() == slotIndex
                        && java.util.Set.of("entry", "admission").contains(e.entitlementType())
                        && providerMerchantId.equals(e.barId() != null ? e.barId() : e.merchantId())
                        && "success".equals(v.status()) && !v.createdAt().isBefore(since);
            });
        }

        @Override
        public Optional<VerificationRecord> findVerificationById(String id) {
            return Optional.ofNullable(verifications.get(id));
        }

        @Override
        public int markVerificationReversed(String id, String reversedBy, java.time.Instant reversedAt) {
            VerificationRecord v = verifications.get(id);
            // F-TXN-04：模拟状态 CAS——仅 status='success' 且未冲正（reversed_at IS NULL）才翻转，返回受影响行数。
            if (v == null || !"success".equals(v.status()) || v.reversedAt() != null) {
                return 0;
            }
            verifications.put(id, new VerificationRecord(
                    v.id(), v.verificationNo(), v.entitlementId(), v.entitlementCode(), v.credentialType(),
                    v.credentialPayload(), v.deviceId(), v.pointId(), "reversed", v.resultReason(), v.sourceType(),
                    v.clientTaskId(), v.createdAt(), v.nonce(), v.staffId(), v.merchantId(), v.windowValid(),
                    v.signatureValid(), v.verifiedPeople(), v.reversesVerificationId(), reversedBy, reversedAt
            ,
            null));
            return 1;
        }

        @Override
        public TicketPriceRuleRecord savePriceRule(TicketPriceRuleRecord rule) {
            priceRules.put(rule.id(), rule);
            return rule;
        }

        @Override
        public List<TicketPriceRuleRecord> listPriceRules(boolean includeArchived) {
            List<TicketPriceRuleRecord> out = new ArrayList<>();
            for (TicketPriceRuleRecord r : priceRules.values()) {
                if ("approved".equals(r.deleteStatus())) {
                    continue;
                }
                if (!includeArchived && r.archivedAt() != null) {
                    continue;
                }
                out.add(r);
            }
            return out;
        }

        @Override
        public void deletePriceRule(String id) {
            priceRules.remove(id);
        }

        long priceRuleReferenceCount;

        @Override
        public long countPriceRuleReferences(String id) {
            return priceRuleReferenceCount;
        }

        @Override
        public Optional<TicketPriceRuleRecord> findPriceRuleById(String id) {
            return Optional.ofNullable(priceRules.get(id));
        }

        @Override
        public TicketDynamicPricingRuleRecord saveDynamicPricingRule(TicketDynamicPricingRuleRecord rule) {
            dynamicPricingRules.put(rule.id(), rule);
            return rule;
        }

        @Override
        public List<TicketDynamicPricingRuleRecord> listDynamicPricingRules() {
            return new ArrayList<>(dynamicPricingRules.values());
        }

        @Override
        public Optional<TicketDynamicPricingRuleRecord> findDynamicPricingRuleById(String id) {
            return Optional.ofNullable(dynamicPricingRules.get(id));
        }

        @Override
        public TicketOrderRecord saveTicketOrder(TicketOrderRecord order) {
            ticketOrders.put(order.id(), order);
            return order;
        }

        @Override
        public Optional<TicketOrderRecord> findTicketOrderById(String id) {
            return Optional.ofNullable(ticketOrders.get(id));
        }

        @Override
        public TicketOrderRecord updateTicketOrder(TicketOrderRecord order) {
            ticketOrders.put(order.id(), order);
            return order;
        }

        @Override
        public List<TicketOrderRecord> listTicketOrders() {
            return new ArrayList<>(ticketOrders.values());
        }

        @Override
        public List<TicketOrderRecord> listTicketOrdersByCustomerId(String customerId) {
            return ticketOrders.values().stream()
                    .filter(order -> customerId.equals(order.customerId()))
                    .toList();
        }

        @Override
        public List<TicketOrderRecord> listTicketOrdersByMerchant(String merchantId) {
            return ticketOrders.values().stream()
                    .filter(order -> merchantId.equals(order.merchantId()))
                    .toList();
        }

        @Override
        public int atomicCancelUnpaidOrder(String orderId, java.time.Instant now) {
            TicketOrderRecord order = ticketOrders.get(orderId);
            if (order == null || !"unpaid".equals(order.payStatus()) || !"created".equals(order.status())) {
                return 0;
            }
            ticketOrders.put(orderId, new TicketOrderRecord(
                    order.id(), order.orderNo(), order.customerId(), order.ticketTypeId(), order.priceRuleId(),
                    order.quantity(), order.visitDate(), order.salesChannel(), order.buyerPhone(), order.amountCent(),
                    order.dynamicPricingRuleId(), order.baseAmountCent(), order.dynamicAdjustmentCent(), order.currency(),
                    order.payStatus(), order.issueStatus(), "cancelled", order.createdAt(), now, order.merchantId(),
                    order.paidAt(), order.barId()));
            return 1;
        }

        @Override
        public List<TicketOrderRecord> listExpiredUnpaidOrders(java.time.Instant cutoff) {
            return ticketOrders.values().stream()
                    .filter(o -> "created".equals(o.status()) && "unpaid".equals(o.payStatus())
                            && o.createdAt() != null && o.createdAt().isBefore(cutoff))
                    .toList();
        }

        @Override
        public TicketPaymentIntentRecord saveTicketPaymentIntent(TicketPaymentIntentRecord intent) {
            paymentIntents.put(intent.id(), intent);
            return intent;
        }

        @Override
        public Optional<TicketPaymentIntentRecord> findTicketPaymentIntentById(String id) {
            return Optional.ofNullable(paymentIntents.get(id));
        }

        @Override
        public List<IssuedEntitlementRecord> listIssuedEntitlements() {
            return new ArrayList<>(issuedEntitlements.values());
        }

        @Override
        public List<IssuedEntitlementRecord> listIssuedEntitlementsByOrderId(String orderId) {
            return issuedEntitlements.values().stream()
                    .filter(entitlement -> orderId.equals(entitlement.orderId()))
                    .toList();
        }

        @Override
        public Optional<IssuedEntitlementRecord> findIssuedEntitlementById(String id) {
            return Optional.ofNullable(issuedEntitlements.get(id));
        }

        @Override
        public TicketCredentialRecord saveTicketCredential(TicketCredentialRecord credential) {
            credentials.put(credential.credentialToken(), credential);
            return credential;
        }

        @Override
        public Optional<TicketCredentialRecord> findTicketCredentialByToken(String credentialToken) {
            return Optional.ofNullable(credentials.get(credentialToken));
        }

        @Override
        public int markTicketCredentialConsumed(String credentialId, java.time.Instant now) {
            // AUD-012 与 Jdbc 同语义的 CAS:仅 active→consumed,否则 0 行。
            for (Map.Entry<String, TicketCredentialRecord> entry : credentials.entrySet()) {
                TicketCredentialRecord c = entry.getValue();
                if (c.id().equals(credentialId)) {
                    if (!"active".equals(c.status())) {
                        return 0;
                    }
                    entry.setValue(new TicketCredentialRecord(
                            c.id(), c.entitlementId(), c.orderId(), c.credentialToken(), c.nonce(),
                            c.credentialHash(), c.credentialType(), c.issuedAt(), c.expiresAt(),
                            "consumed", c.refreshedCount(), c.createdAt(), now, c.refreshIntervalSeconds(),
                            c.windowStartedAt(), c.signatureValue(), c.signatureVersion()));
                    return 1;
                }
            }
            return 0;
        }

        @Override
        public TicketRefundRecord saveTicketRefund(TicketRefundRecord refund) {
            refunds.put(refund.id(), refund);
            return refund;
        }

        @Override
        public int markTicketRefundApproved(String refundId, String approvedBy, java.time.Instant approvedAt) {
            TicketRefundRecord r = refunds.get(refundId);
            // F-TXN-04：模拟状态 CAS——仅 status='requested' 才翻转 approved，返回受影响行数。
            if (r == null || !"requested".equals(r.status())) {
                return 0;
            }
            refunds.put(refundId, new TicketRefundRecord(
                    r.id(), r.targetId(), r.refundAmountCent(), r.reason(), r.refundChannel(),
                    "approved", r.requestedBy(), approvedBy, approvedAt, r.createdAt(), approvedAt, r.merchantId()));
            return 1;
        }

        @Override
        public int markTicketRefundSucceeded(String refundId, java.time.Instant succeededAt) {
            TicketRefundRecord r = refunds.get(refundId);
            if (r == null || !"approved".equals(r.status())) {
                return 0;
            }
            refunds.put(refundId, new TicketRefundRecord(
                    r.id(), r.targetId(), r.refundAmountCent(), r.reason(), r.refundChannel(),
                    "success", r.requestedBy(), r.approvedBy(), r.approvedAt(), r.createdAt(), succeededAt, r.merchantId()));
            return 1;
        }

        @Override
        public List<TicketRefundRecord> listTicketRefunds() {
            return new java.util.ArrayList<>(refunds.values());
        }

        @Override
        public Optional<TicketRefundRecord> findTicketRefundById(String id) {
            return Optional.ofNullable(refunds.get(id));
        }

        @Override
        public TicketRefundLineRecord saveTicketRefundLine(TicketRefundLineRecord line) {
            refundLines.put(line.id(), line);
            return line;
        }

        @Override
        public List<TicketRefundLineRecord> listTicketRefundLines(String refundId) {
            return refundLines.values().stream()
                    .filter(line -> refundId.equals(line.refundId()))
                    .toList();
        }

        @Override
        public long sumPendingRefundAmountByOrderId(String orderId) {
            return refunds.values().stream()
                    .filter(r -> orderId.equals(r.targetId()))
                    .filter(r -> "requested".equals(r.status()) || "approved".equals(r.status()))
                    .mapToLong(TicketRefundRecord::refundAmountCent)
                    .sum();
        }

        @Override
        public java.util.Optional<IssuedEntitlementRecord> atomicUseEntitlement(String id, java.time.Instant updatedAt) {
            IssuedEntitlementRecord entitlement = issuedEntitlements.get(id);
            if (entitlement == null || entitlement.remainingTimes() <= 0 || !"active".equals(entitlement.status())) {
                return java.util.Optional.empty();
            }
            IssuedEntitlementRecord updated = entitlement.useOnce(updatedAt);
            issuedEntitlements.put(id, updated);
            return java.util.Optional.of(updated);
        }

        @Override
        public java.util.Optional<IssuedEntitlementRecord> atomicReverseEntitlement(String id, java.time.Instant updatedAt) {
            IssuedEntitlementRecord entitlement = issuedEntitlements.get(id);
            if (entitlement == null || entitlement.usedTimes() <= 0 || "refunded".equals(entitlement.status())) {
                return java.util.Optional.empty();
            }
            IssuedEntitlementRecord updated = new IssuedEntitlementRecord(
                    entitlement.id(), entitlement.entitlementCode(), entitlement.ticketTypeId(), entitlement.orderId(),
                    entitlement.entitlementTemplateId(), entitlement.entitlementType(), entitlement.merchantId(),
                    entitlement.venueId(), entitlement.pointId(), entitlement.totalTimes(),
                    Math.max(entitlement.usedTimes() - 1, 0), entitlement.valueAmountCent(),
                    "active", entitlement.validFrom(), entitlement.validTo(), entitlement.createdAt(), updatedAt,
                    entitlement.numPeople(), entitlement.allocatedRevenueCent(),
                    entitlement.entitlementKind(), entitlement.agreementPriceCent(), null, null, null, 0, null, null);
            issuedEntitlements.put(id, updated);
            return java.util.Optional.of(updated);
        }

        @Override
        public int freezeEntitlementForRefund(String id, java.time.Instant updatedAt) {
            IssuedEntitlementRecord entitlement = issuedEntitlements.get(id);
            // 方案2 强 CAS：仅冻结 active 且 used_times=0（申请→审批空窗被核销则命中 0 行，调用方回滚拒退）。
            if (entitlement == null || !"active".equals(entitlement.status()) || entitlement.usedTimes() != 0) {
                return 0;
            }
            issuedEntitlements.put(id, new IssuedEntitlementRecord(
                    entitlement.id(), entitlement.entitlementCode(), entitlement.ticketTypeId(), entitlement.orderId(),
                    entitlement.entitlementTemplateId(), entitlement.entitlementType(), entitlement.merchantId(),
                    entitlement.venueId(), entitlement.pointId(), entitlement.totalTimes(),
                    entitlement.usedTimes(), entitlement.valueAmountCent(),
                    "refunded", entitlement.validFrom(), entitlement.validTo(), entitlement.createdAt(), updatedAt,
                    entitlement.numPeople(), entitlement.allocatedRevenueCent(),
                    entitlement.entitlementKind(), entitlement.agreementPriceCent(),
                    entitlement.consumptionWindow(), entitlement.exclusiveMerchantId(),
                    entitlement.parentTicketId(), entitlement.slotIndex(), null, null));
            return 1;
        }

        @Override
        public int markTicketInstanceRefundPending(String id, java.time.Instant now) {
            return casTicketInstanceStatus(id, "active", "refund_pending", now);
        }

        @Override
        public int markTicketInstanceRefunded(String id, java.time.Instant now) {
            return casTicketInstanceStatus(id, "refund_pending", "refunded", now);
        }

        @Override
        public int restoreTicketInstanceActive(String id, java.time.Instant now) {
            return casTicketInstanceStatus(id, "refund_pending", "active", now);
        }

        private int casTicketInstanceStatus(String id, String from, String to, java.time.Instant now) {
            TicketInstanceRecord ti = ticketInstances.get(id);
            if (ti == null || !from.equals(ti.status())) {
                return 0;
            }
            ticketInstances.put(id, new TicketInstanceRecord(
                    ti.id(), ti.ticketNo(), ti.orderId(), ti.ticketTypeId(), ti.seqInOrder(), ti.numPeople(),
                    ti.totalTimes(), ti.buyerCustomerId(), ti.currentHolderCustomerId(), to,
                    ti.firstUsedAt(), ti.firstSessionKey(), ti.createdAt(), now));
            return 1;
        }

        @Override
        public int markTicketRefundRejected(String refundId, String rejectedBy, java.time.Instant rejectedAt) {
            TicketRefundRecord r = refunds.get(refundId);
            if (r == null || !"requested".equals(r.status())) {
                return 0;
            }
            refunds.put(refundId, new TicketRefundRecord(
                    r.id(), r.targetId(), r.refundAmountCent(), r.reason(), r.refundChannel(), "rejected",
                    r.requestedBy(), rejectedBy, rejectedAt, r.createdAt(), rejectedAt, r.merchantId()));
            return 1;
        }

        @Override
        public List<IssuedEntitlementRecord> listExpiredUnusedActiveEntitlements(java.time.Instant now) {
            return issuedEntitlements.values().stream()
                    .filter(e -> "active".equals(e.status()) && e.usedTimes() == 0
                            && e.validTo() != null && e.validTo().isBefore(now))
                    .toList();
        }

        @Override
        public int forfeitEntitlement(String id, java.time.Instant now) {
            IssuedEntitlementRecord e = issuedEntitlements.get(id);
            if (e == null || !"active".equals(e.status()) || e.usedTimes() != 0) {
                return 0;
            }
            issuedEntitlements.put(id, new IssuedEntitlementRecord(
                    e.id(), e.entitlementCode(), e.ticketTypeId(), e.orderId(), e.entitlementTemplateId(),
                    e.entitlementType(), e.merchantId(), e.venueId(), e.pointId(), e.totalTimes(),
                    e.usedTimes(), e.valueAmountCent(), "forfeited", e.validFrom(), e.validTo(),
                    e.createdAt(), now, e.numPeople(), e.allocatedRevenueCent(),
                    e.entitlementKind(), e.agreementPriceCent(), null, null, null, 0, null, null));
            return 1;
        }

        @Override
        public int expireBarEntitlements(String barId, java.time.Instant validTo, java.time.Instant now) {
            int n = 0;
            for (IssuedEntitlementRecord e : new ArrayList<>(issuedEntitlements.values())) {
                if (barId != null && barId.equals(e.barId()) && "active".equals(e.status())) {
                    java.time.Instant target = e.validTo() != null && e.validTo().isBefore(validTo) ? e.validTo() : validTo;
                    issuedEntitlements.put(e.id(), e.withValidTo(target, now));
                    n++;
                }
            }
            return n;
        }

        @Override
        public boolean tryLockOrderForIssuance(String orderId) {
            TicketOrderRecord order = ticketOrders.get(orderId);
            if (order == null || !"not_issued".equals(order.issueStatus())) {
                return false;
            }
            ticketOrders.put(orderId, new TicketOrderRecord(
                    order.id(), order.orderNo(), order.customerId(), order.ticketTypeId(),
                    order.priceRuleId(), order.quantity(), order.visitDate(), order.salesChannel(),
                    order.buyerPhone(), order.amountCent(), order.dynamicPricingRuleId(),
                    order.baseAmountCent(), order.dynamicAdjustmentCent(), order.currency(),
                    order.payStatus(), "issuing", order.status(), order.createdAt(), order.updatedAt(),
                    order.merchantId(), order.paidAt(), order.barId()
            ));
            return true;
        }

        @Override
        public String findLatestFlywayVersion() {
            return "9";
        }
    }


    // ================= V369 综合／联合通票售价自动算 + 凑整误差闸（业务 HTML:741） =================

    /** 建一张「游乐 + 酒吧 + 免费赠品」的联合通票，三档价一律填 priceYuanCent，票种折扣 discountRate。 */
    private TicketTypeRecord seedComboPass(String key, long priceCent, java.math.BigDecimal discountRate) {
        TicketTypeRecord tt = service.createTicketType(new TicketTypeRequest(
                key, "create", List.of(), "联合通票 " + key, "weekday_weekend_holiday",
                priceCent, priceCent, priceCent, Boolean.FALSE, null, null, null, Boolean.TRUE,
                null, null, null, discountRate));
        EntitlementTemplateRecord ride = service.createEntitlementTemplate(new EntitlementTemplateRequest(
                key + "-tpl-ride", "ride", List.of(), "游乐 " + key, "amusement_ride", 1, 8000L, "active",
                null, "mch_" + key + "_ride", null, null));
        EntitlementTemplateRecord bar = service.createEntitlementTemplate(new EntitlementTemplateRequest(
                key + "-tpl-bar", "bar", List.of(), "酒吧 " + key, "bar_drink", 1, 12500L, "active",
                null, "mch_" + key + "_bar", null, null));
        EntitlementTemplateRecord free = service.createEntitlementTemplate(new EntitlementTemplateRequest(
                key + "-tpl-free", "free", List.of(), "免费小食 " + key, "dining", 1, 0L, "active",
                null, "mch_" + key + "_dining", null, null));
        // 游乐 80 元 + 酒吧 125 元 = 205 元；免费小食票面原价 0（HTML:741「免费的（价格为 0）不计入价值」）。
        service.bindTicketTypeEntitlement(tt.id(), new TicketTypeEntitlementRequest(
                key + "-bind-ride", "bind", List.of(), ride.id(), 1, 1, 8000L, "default", 1, 8000L));
        service.bindTicketTypeEntitlement(tt.id(), new TicketTypeEntitlementRequest(
                key + "-bind-bar", "bind", List.of(), bar.id(), 1, 1, 12500L, "default", 1, 12500L));
        service.bindTicketTypeEntitlement(tt.id(), new TicketTypeEntitlementRequest(
                key + "-bind-free", "bind", List.of(), free.id(), 1, 1, 0L, "default", 1, 0L));
        return ticketingRepository.findTicketTypeById(tt.id()).orElseThrow();
    }

    private void publish(String ticketTypeId, String key) {
        service.publishTicketType(ticketTypeId, new StatusChangeRequest(key, "p", "上架", List.of()));
    }

    @Test
    void comboPassSellingPriceIsComputedFromFormatFacePricesTimesDiscount() {
        // HTML:741「售价由各业态价格相加后统一打折得出……由系统自动算出售价」。
        // 游乐 80 + 酒吧 125 = 205 元（免费小食 0 不计入）；八五折 → 174.25 元。
        catalogService.comboPriceToleranceConfigPort = toleranceStub;
        TicketTypeRecord pass = seedComboPass("v369-calc", 17800L, new java.math.BigDecimal("85"));

        TicketTypePriceCheck check = service.ticketTypePriceCheck(pass.id());

        assertThat(check.componentTotalCent()).isEqualTo(20500L);   // 免费权益不计入价值
        assertThat(check.computedPriceCent()).isEqualTo(17425L);    // 205 × 85% = 174.25 元
        assertThat(check.toleranceCent()).isEqualTo(500L);
        assertThat(check.filledPriceCent()).isEqualTo(17800L);
        assertThat(check.diffCent()).isEqualTo(375L);               // 上调 3.75 元凑成 178
        assertThat(check.withinTolerance()).isTrue();
        assertThat(check.roundingSuggested()).isEqualTo(17800L);    // 整元且尾数 8
    }

    @Test
    void roundingWithinToleranceIsPublishableEitherWayUpOrDown() {
        // HTML:741「上调下调均可」：只要没超出允许误差（5 元），凑整成好卖的整数就放行。
        catalogService.comboPriceToleranceConfigPort = toleranceStub;
        TicketTypeRecord up = seedComboPass("v369-up", 17800L, new java.math.BigDecimal("85"));   // 算出 174.25 → 上调到 178
        TicketTypeRecord down = seedComboPass("v369-down", 17000L, new java.math.BigDecimal("85")); // 下调到 170

        assertThat(service.publishTicketType(up.id(), new StatusChangeRequest(
                "v369-up-pub", "p", "上架", List.of())).status()).isEqualTo("published");
        assertThat(service.publishTicketType(down.id(), new StatusChangeRequest(
                "v369-down-pub", "p", "上架", List.of())).status()).isEqualTo("published");
    }

    @Test
    void publishRejectsPriceBeyondToleranceBothDirectionsWithChineseNumbers() {
        // HTML:741「与系统算出的售价之间的差额不得超过后台配置的允许误差，超出即拒绝发布」。
        // 这就是本刀要堵的洞：运营想填 178 手滑填成 160，以前票照样上架、少收的钱被当成套票折扣由项目公司吃掉。
        catalogService.comboPriceToleranceConfigPort = toleranceStub;
        TicketTypeRecord tooHigh = seedComboPass("v369-high", 18000L, new java.math.BigDecimal("85")); // 算出 174.25，上调 5.75
        TicketTypeRecord tooLow = seedComboPass("v369-low", 16000L, new java.math.BigDecimal("85"));   // 下调 14.25

        assertThatThrownBy(() -> publish(tooHigh.id(), "v369-high-pub"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("TICKET_PRICE_OUT_OF_TOLERANCE"))
                // 报文要让运营当场看懂：算出多少、填了多少、差多少、超了多少、凑整该填哪个数。
                .hasMessageContaining("174.25")
                .hasMessageContaining("180.00")
                .hasMessageContaining("上调")
                .hasMessageContaining("178.00");

        assertThatThrownBy(() -> publish(tooLow.id(), "v369-low-pub"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("TICKET_PRICE_OUT_OF_TOLERANCE"))
                .hasMessageContaining("下调")
                .hasMessageContaining("14.25");
    }

    @Test
    void toleranceIsBackofficeConfigurableAndBehaviourFollowsIt() {
        // HTML:741「允许误差后台可配」：同一张票，误差调宽就能发、调窄就发不了——不是写死在代码里的数。
        catalogService.comboPriceToleranceConfigPort = toleranceStub;
        TicketTypeRecord pass = seedComboPass("v369-cfg", 18000L, new java.math.BigDecimal("85")); // 差 5.75 元

        toleranceStub.toleranceCent = 500L;   // 5 元：拦
        assertThatThrownBy(() -> publish(pass.id(), "v369-cfg-pub1"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("TICKET_PRICE_OUT_OF_TOLERANCE"));

        toleranceStub.toleranceCent = 1000L;  // 10 元：放行
        assertThat(service.publishTicketType(pass.id(), new StatusChangeRequest(
                "v369-cfg-pub2", "p", "上架", List.of())).status()).isEqualTo("published");
        assertThat(service.ticketTypePriceCheck(pass.id()).toleranceCent()).isEqualTo(1000L);
    }

    @Test
    void freeEntitlementsDoNotCountIntoTheComputedPrice() {
        // HTML:741「免费的（价格为 0）不计入价值」：再加两条免费权益，算出售价一分不变。
        catalogService.comboPriceToleranceConfigPort = toleranceStub;
        TicketTypeRecord pass = seedComboPass("v369-free", 17800L, new java.math.BigDecimal("85"));
        long before = service.ticketTypePriceCheck(pass.id()).computedPriceCent();

        EntitlementTemplateRecord gift = service.createEntitlementTemplate(new EntitlementTemplateRequest(
                "v369-free-tpl2", "gift", List.of(), "加赠小食", "dining", 1, 0L, "active",
                null, "mch_v369_free_2", null, null));
        service.bindTicketTypeEntitlement(pass.id(), new TicketTypeEntitlementRequest(
                "v369-free-bind2", "bind", List.of(), gift.id(), 3, 1, 0L, "default", 1, 0L));

        TicketTypePriceCheck after = service.ticketTypePriceCheck(pass.id());
        assertThat(after.componentTotalCent()).isEqualTo(20500L);
        assertThat(after.computedPriceCent()).isEqualTo(before);
        assertThat(service.publishTicketType(pass.id(), new StatusChangeRequest(
                "v369-free-pub", "p", "上架", List.of())).status()).isEqualTo("published");
    }

    @Test
    void changingEntitlementsOnPublishedPassSendsItBackToDraftForRePublish() {
        // 已上架的票被改了组成，它原来的售价就不再是「按现在这些业态算出来的」——退回草稿，逼它重过一次发布闸。
        catalogService.comboPriceToleranceConfigPort = toleranceStub;
        TicketTypeRecord pass = seedComboPass("v369-redraft", 17800L, new java.math.BigDecimal("85"));
        publish(pass.id(), "v369-redraft-pub");
        assertThat(ticketingRepository.findTicketTypeById(pass.id()).orElseThrow().status()).isEqualTo("published");

        EntitlementTemplateRecord extra = service.createEntitlementTemplate(new EntitlementTemplateRequest(
                "v369-redraft-tpl", "extra", List.of(), "加的游乐", "amusement_ride", 1, 6000L, "active",
                null, "mch_v369_redraft_extra", null, null));
        service.bindTicketTypeEntitlement(pass.id(), new TicketTypeEntitlementRequest(
                "v369-redraft-bind", "bind", List.of(), extra.id(), 1, 1, 6000L, "default", 1, 6000L));

        assertThat(ticketingRepository.findTicketTypeById(pass.id()).orElseThrow().status()).isEqualTo("draft");
        // 加了 60 元的游乐，算出售价变成 (205+60)×85% = 225.25 元，原来的 178 就发不出去了。
        assertThat(service.ticketTypePriceCheck(pass.id()).computedPriceCent()).isEqualTo(22525L);
        assertThatThrownBy(() -> publish(pass.id(), "v369-redraft-pub2"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("TICKET_PRICE_OUT_OF_TOLERANCE"));
    }

    @Test
    void updatePricingRejectsOutOfToleranceAtSaveTimeAndAcceptsRoundedPrice() {
        // 改价这一步就比对：让运营先存下一个明知超限的价、等到发布才报错，等于让他白填一遍。
        catalogService.comboPriceToleranceConfigPort = toleranceStub;
        TicketTypeRecord pass = seedComboPass("v369-save", 17800L, new java.math.BigDecimal("85"));

        assertThatThrownBy(() -> service.updateTicketTypePricing(pass.id(),
                new com.complex.platform.ticketing.dto.TicketTypePricingRequest(
                        "v369-save-bad", "改价", new java.math.BigDecimal("85"), 16000L, 16000L, 16000L)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("TICKET_PRICE_OUT_OF_TOLERANCE"));

        TicketTypeRecord saved = service.updateTicketTypePricing(pass.id(),
                new com.complex.platform.ticketing.dto.TicketTypePricingRequest(
                        "v369-save-ok", "改价", new java.math.BigDecimal("85"), 17800L, 19800L, 19800L));
        assertThat(saved.weekdayPriceCent()).isEqualTo(17800L);
        // 周末/节假日加价按日历规则走，不在误差闸内（否则运营只能把误差调到几十元，那道闸就等于没有了）。
        assertThat(saved.weekendPriceCent()).isEqualTo(19800L);
    }

    @Test
    void roundingDifferenceFlowsIntoTheExistingComboDiscountAccounting() {
        // HTML:741「凑整产生的差额按既有折扣承担口径处理」：不新建口径。
        //   出票时每张权益照旧记「满额份额=票面原价」与「实付份额」，两者之差就是折扣，
        //   由结算配置中心那条 combo_discount_bearer 决定谁承担——凑整多出/少收的那点钱自然走同一条路。
        catalogService.comboPriceToleranceConfigPort = toleranceStub;
        TicketTypeRecord pass = seedComboPass("v369-discount", 17800L, new java.math.BigDecimal("85"));
        ticketingRepository.ticketProjects.put(pass.id(), List.of("project_rounding"));
        publish(pass.id(), "v369-discount-pub");

        TicketOrderRecord order = service.createTicketOrder(new TicketOrderRequest(
                "v369-discount-order", "c", List.of(), pass.id(), null, 1,
                LocalDate.parse("2026-06-22"), "admin", "13800138000", "cust_v369"));
        IssuedTicketResponse issued = service.issueTicketOrder(order.id(), new StatusChangeRequest(
                "v369-discount-issue", "i", "已收款", List.of()));

        long fullSum = 0L;
        long allocatedSum = 0L;
        for (var one : issued.issuedEntitlements()) {
            IssuedEntitlementRecord ent = ticketingRepository
                    .findIssuedEntitlementByCode(one.entitlementCode()).orElseThrow();
            fullSum += ent.componentFullPriceCent() == null ? 0L : ent.componentFullPriceCent();
            allocatedSum += ent.allocatedRevenueCent() == null ? 0L : ent.allocatedRevenueCent();
        }
        assertThat(order.amountCent()).isEqualTo(17800L);   // 客人实付=凑整后的售价
        assertThat(allocatedSum).isEqualTo(17800L);         // 实付逐分拆到各权益，不丢分
        assertThat(fullSum).isEqualTo(20500L);              // 满额=各业态票面原价合计（不受凑整影响）
        assertThat(fullSum - allocatedSum).isEqualTo(2700L); // 折扣=满额−净额（含 8.5 折 + 凑整那 3.75 元）
    }

    @Test
    void niceEndingSuggestionPicksTheNearestEightNineOrZero() {
        // 凑整建议只是给运营的一个数（尾数 8/9/0），不强制——真正拒绝发布的只有「超出允许误差」这一条。
        assertThat(TicketCatalogService.roundToNiceEnding(17425L)).isEqualTo(17800L);
        assertThat(TicketCatalogService.roundToNiceEnding(19100L)).isEqualTo(19000L);
        assertThat(TicketCatalogService.roundToNiceEnding(19300L)).isEqualTo(19000L);
        assertThat(TicketCatalogService.roundToNiceEnding(19600L)).isEqualTo(19800L);
        assertThat(TicketCatalogService.roundToNiceEnding(0L)).isEqualTo(0L);
    }
}
