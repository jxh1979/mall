package com.complex.platform.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 会员忠诚度 第2部分（V090）单测：储值充值（绑支付意图：无支付不加余额）/消费不足拒/冻结阻断；
 * 付费会员开通→生效期、到期→expired、续费；积分转赠原子 + 日限额拒 + 不足拒。
 */
class MemberWalletServiceTests {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private AuditEventService audit() {
        return new AuditEventService(event -> event);
    }

    private long days(int d) {
        return (long) d * 86_400L;
    }

    // ============================================================
    // 储值：充值绑支付意图 —— 无支付不加余额，支付成功后入账
    // ============================================================

    @Test
    void topupAddsBalanceOnlyAfterPaymentSucceeds() {
        InMemoryRepo repo = seeded();
        FakeFinance finance = new FakeFinance();
        StoredValueService service = new StoredValueService(repo, finance, audit(),
                Clock.fixed(T0, ZoneOffset.UTC));

        // 发起充值：仅建支付意图（created），余额不变。
        PaymentIntentRecord intent = service.topup("c1", null, null, 10_000L, "wechat", "topup-1");
        assertThat(intent.status()).isEqualTo("created");
        assertThat(repo.storedValue.get("c1|null|null").balanceCent()).isZero();

        // 支付未成功时入账被拒（不凭空加余额）。
        assertThatThrownBy(() -> service.settleTopup("c1", null, null, intent.id()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PAYMENT_NOT_SUCCEEDED"));
        assertThat(repo.storedValue.get("c1|null|null").balanceCent()).isZero();

        // 支付成功后入账：余额 +10000。
        finance.markPaymentIntentSucceeded(intent.id());
        CustomerStoredValueTransactionRecord txn = service.settleTopup("c1", null, null, intent.id());
        assertThat(txn.txnType()).isEqualTo("topup");
        assertThat(repo.storedValue.get("c1|null|null").balanceCent()).isEqualTo(10_000L);
        // V231 资金/时间铁律：充值即支付，topup 流水 paid_at=交易完成时刻（本用例固定时钟 T0），非 null。
        assertThat(txn.paidAt()).isEqualTo(T0);

        // 重复入账同一支付意图：不重复加余额。
        CustomerStoredValueTransactionRecord again = service.settleTopup("c1", null, null, intent.id());
        assertThat(again.id()).isEqualTo(txn.id());
        assertThat(repo.storedValue.get("c1|null|null").balanceCent()).isEqualTo(10_000L);
    }

    @Test
    void consumeRejectedWhenInsufficient() {
        InMemoryRepo repo = seeded();
        FakeFinance finance = new FakeFinance();
        StoredValueService service = new StoredValueService(repo, finance, audit(),
                Clock.fixed(T0, ZoneOffset.UTC));
        // 充值 5000 入账
        PaymentIntentRecord intent = service.topup("c1", null, null, 5_000L, "wechat", "topup-2");
        finance.markPaymentIntentSucceeded(intent.id());
        service.settleTopup("c1", null, null, intent.id());
        // 消费 3000 OK
        service.consume("c1", null, null, 3_000L, "buy", "consume-1");
        assertThat(repo.storedValue.get("c1|null|null").balanceCent()).isEqualTo(2_000L);
        // 消费 5000 不足拒
        assertThatThrownBy(() -> service.consume("c1", null, null, 5_000L, "buy", "consume-2"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("STORED_VALUE_INSUFFICIENT"));
        assertThat(repo.storedValue.get("c1|null|null").balanceCent()).isEqualTo(2_000L);
    }

    @Test
    void freezeBlocksConsumeAndUnfreezeRestores() {
        InMemoryRepo repo = seeded();
        FakeFinance finance = new FakeFinance();
        StoredValueService service = new StoredValueService(repo, finance, audit(),
                Clock.fixed(T0, ZoneOffset.UTC));
        PaymentIntentRecord intent = service.topup("c1", null, null, 5_000L, "wechat", "topup-3");
        finance.markPaymentIntentSucceeded(intent.id());
        service.settleTopup("c1", null, null, intent.id());

        service.freeze("c1", null, null, "risk");
        assertThat(repo.storedValue.get("c1|null|null").status()).isEqualTo("frozen");
        assertThatThrownBy(() -> service.consume("c1", null, null, 1_000L, "buy", "c-frozen"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("STORED_VALUE_ACCOUNT_FROZEN"));

        service.unfreeze("c1", null, null, "cleared");
        assertThat(repo.storedValue.get("c1|null|null").status()).isEqualTo("active");
        CustomerStoredValueTransactionRecord txn = service.consume("c1", null, null, 1_000L, "buy", "c-ok");
        assertThat(txn.balanceAfterCent()).isEqualTo(4_000L);
        // V231：消费非充值支付，paid_at 必须为 null（仅 topup 记支付时刻）。
        assertThat(txn.paidAt()).isNull();
    }

    @Test
    void refundReturnsBalanceAndClosesAccount() {
        InMemoryRepo repo = seeded();
        FakeFinance finance = new FakeFinance();
        StoredValueService service = new StoredValueService(repo, finance, audit(),
                Clock.fixed(T0, ZoneOffset.UTC));
        PaymentIntentRecord intent = service.topup("c1", null, null, 5_000L, "wechat", "topup-4");
        finance.markPaymentIntentSucceeded(intent.id());
        service.settleTopup("c1", null, null, intent.id());
        service.refund("c1", null, null, 5_000L, "refund all", true, "refund-1");
        assertThat(repo.storedValue.get("c1|null|null").balanceCent()).isZero();
        assertThat(repo.storedValue.get("c1|null|null").status()).isEqualTo("closed");
        // AUD-060：渠道未兑付（开关关=RESERVED）→ 登记一笔 requested 退款待办，不得静默记成功。
        assertThat(finance.payoutRefunds).hasSize(1);
        assertThat(finance.payoutRefunds.get(0)).contains("5000");
    }

    @Test
    void settleTopupRejectsWhenScopeDiffersFromTopupBinding() {
        // AUD-058：入账只认充值时绑定的卡范围——指名商户卡/跨项目/他人 customerId 均拒，资金不入错卡。
        InMemoryRepo repo = seeded();
        FakeFinance finance = new FakeFinance();
        StoredValueService service = new StoredValueService(repo, finance, audit(),
                Clock.fixed(T0, ZoneOffset.UTC));
        PaymentIntentRecord intent = service.topup("c1", null, null, 5_000L, "wechat", "topup-scope");
        finance.markPaymentIntentSucceeded(intent.id());

        // 通用卡充值被指名入商户卡 → 拒（结算口径错位）。
        assertThatThrownBy(() -> service.settleTopup("c1", null, "m1", intent.id()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("STORED_VALUE_SCOPE_MISMATCH"));
        // 跨项目 → 拒。
        assertThatThrownBy(() -> service.settleTopup("c1", "p9", null, intent.id()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("STORED_VALUE_SCOPE_MISMATCH"));
        // 他人 customerId → 意图不属于该充值。
        repo.saveCustomer(new CustomerRecord("c2", "139", "139", "active", null, T0, T0, T0));
        assertThatThrownBy(() -> service.settleTopup("c2", null, null, intent.id()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        // 与绑定一致 → 照常入账。
        service.settleTopup("c1", null, null, intent.id());
        assertThat(repo.storedValue.get("c1|null|null").balanceCent()).isEqualTo(5_000L);
    }

    @Test
    void refundFailedAtGatewayThrowsSoTransactionRollsBack() {
        // AUD-060：渠道明确 FAILED → 抛异常（生产 @Transactional 整单回滚：余额不扣、不写流水），不得记成功。
        InMemoryRepo repo = seeded();
        FakeFinance finance = new FakeFinance();
        com.complex.platform.finance.gateway.PaymentGatewayRouter failing =
                org.mockito.Mockito.mock(com.complex.platform.finance.gateway.PaymentGatewayRouter.class);
        org.mockito.Mockito.when(failing.refund(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new com.complex.platform.finance.gateway.PaymentGatewayResult(
                        com.complex.platform.finance.gateway.PaymentGatewayResult.Status.FAILED,
                        "prov", null, "declined"));
        StoredValueService service = new StoredValueService(repo, finance, audit(), failing,
                Clock.fixed(T0, ZoneOffset.UTC));
        PaymentIntentRecord intent = service.topup("c1", null, null, 5_000L, "wechat", "topup-gwfail");
        finance.markPaymentIntentSucceeded(intent.id());
        service.settleTopup("c1", null, null, intent.id());

        assertThatThrownBy(() -> service.refund("c1", null, null, 5_000L, "r", true, "rf-fail"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("STORED_VALUE_REFUND_FAILED"));
        // FAILED 不登记 requested 待办（余额回滚由生产事务保证，内存 repo 不模拟回滚故不断言余额）。
        assertThat(finance.payoutRefunds).isEmpty();
    }

    @Test
    void merchantCardIsolatedFromProjectCardAndOnlyConsumableByItsMerchant() {
        // B1d⑤ phase2：同一会员的「综合体通用卡」与「商户 m1 的商户级卡」互相独立；
        // 指定 m1 消费只扣 m1 卡（只扣指定的那一张）；商户卡只能在该发卡商户消费（拿 m2 范围取不到 m1 卡，不误扣）。
        InMemoryRepo repo = seeded();
        FakeFinance finance = new FakeFinance();
        StoredValueService service = new StoredValueService(repo, finance, audit(),
                Clock.fixed(T0, ZoneOffset.UTC));
        PaymentIntentRecord p = service.topup("c1", null, null, 8_000L, "wechat", "tp-prj");
        finance.markPaymentIntentSucceeded(p.id());
        service.settleTopup("c1", null, null, p.id());
        PaymentIntentRecord m = service.topup("c1", null, "m1", 3_000L, "wechat", "tp-m1");
        finance.markPaymentIntentSucceeded(m.id());
        service.settleTopup("c1", null, "m1", m.id());

        assertThat(repo.storedValue.get("c1|null|null").balanceCent()).isEqualTo(8_000L);
        assertThat(repo.storedValue.get("c1|null|m1").balanceCent()).isEqualTo(3_000L);

        // 指定 m1 消费只扣 m1 卡，通用卡不动。
        service.consume("c1", null, "m1", 1_000L, "buy at m1", "cs-m1");
        assertThat(repo.storedValue.get("c1|null|m1").balanceCent()).isEqualTo(2_000L);
        assertThat(repo.storedValue.get("c1|null|null").balanceCent()).isEqualTo(8_000L);

        // 商户卡只能在该商户消费：m2 范围取不到 m1 卡 → 账户不存在，不误扣 m1/通用卡。
        assertThatThrownBy(() -> service.consume("c1", null, "m2", 500L, "buy at m2", "cs-m2"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("STORED_VALUE_ACCOUNT_NOT_FOUND"));
    }

    @Test
    void merchantCardRefundRuleRejectsWhenNotAllowedAndConfigRoundTrips() {
        // B1d⑤ phase3：商户配置「不可退」→ 该商户卡退款被拒；配置「可退+比例」→ 退款放行（按比例打款，余额全额扣减）。
        InMemoryRepo repo = seeded();
        FakeFinance finance = new FakeFinance();
        StoredValueService service = new StoredValueService(repo, finance, audit(),
                Clock.fixed(T0, ZoneOffset.UTC));
        PaymentIntentRecord m = service.topup("c1", null, "m1", 5_000L, "wechat", "tp-m1r");
        finance.markPaymentIntentSucceeded(m.id());
        service.settleTopup("c1", null, "m1", m.id());

        // 商户 m1 配「不可退」→ 退款被拒，余额不动。
        service.configureMerchantRefundRule("m1", false, 10000);
        assertThat(service.getMerchantRefundRule("m1").refundAllowed()).isFalse();
        assertThatThrownBy(() -> service.refund("c1", null, "m1", 1_000L, "r", false, "rf-deny"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("STORED_VALUE_REFUND_NOT_ALLOWED"));
        assertThat(repo.storedValue.get("c1|null|m1").balanceCent()).isEqualTo(5_000L);

        // 改配「可退、80%」→ 退款放行；余额按请求额全额扣减（差额为商户留存，打款按比例由渠道处理）。
        service.configureMerchantRefundRule("m1", true, 8000);
        service.refund("c1", null, "m1", 5_000L, "refund", true, "rf-ok");
        assertThat(repo.storedValue.get("c1|null|m1").balanceCent()).isZero();
        assertThat(repo.storedValue.get("c1|null|m1").status()).isEqualTo("closed");

        // 通用卡不受商户退款规则约束（默认全额可退）。
        PaymentIntentRecord p = service.topup("c1", null, null, 2_000L, "wechat", "tp-prjr");
        finance.markPaymentIntentSucceeded(p.id());
        service.settleTopup("c1", null, null, p.id());
        service.refund("c1", null, null, 2_000L, "refund", true, "rf-prj");
        assertThat(repo.storedValue.get("c1|null|null").balanceCent()).isZero();
    }

    @Test
    void merchantStaffCanOnlyOperateOwnMerchantCards() {
        // B1d⑤ phase4 员工端卡范围隔离：商户 m1 范围的员工只能冻结本商户(m1)的商户卡；
        // 操作他商户(m2)卡或综合体通用卡(merchant 空)均 CROSS_MERCHANT_FORBIDDEN；平台管理员(scope 空)放行。
        InMemoryRepo repo = seeded();
        repo.saveStoredValueAccount(new CustomerStoredValueAccountRecord("a_m1", "c1", null, "m1", 1_000L, "active", T0, T0));
        repo.saveStoredValueAccount(new CustomerStoredValueAccountRecord("a_m2", "c1", null, "m2", 1_000L, "active", T0, T0));
        repo.saveStoredValueAccount(new CustomerStoredValueAccountRecord("a_prj", "c1", null, null, 1_000L, "active", T0, T0));

        com.complex.platform.access.AuthorizationService merchantAuth =
                org.mockito.Mockito.mock(com.complex.platform.access.AuthorizationService.class);
        org.mockito.Mockito.when(merchantAuth.currentMerchantScope()).thenReturn(Optional.of("m1"));
        StoredValueService m1Staff = new StoredValueService(repo, new FakeFinance(), audit(),
                com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled(), merchantAuth,
                Clock.fixed(T0, ZoneOffset.UTC));

        // 本商户卡 → 放行。
        assertThat(m1Staff.freeze("c1", null, "m1", "risk").status()).isEqualTo("frozen");
        // 他商户卡 → 拒。
        assertThatThrownBy(() -> m1Staff.freeze("c1", null, "m2", "x"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
        // 清单 P1-1:consume 也须受卡范围约束——商户 m1 员工不得扣他商户(m2)卡或通用卡(越权扣款);
        // scope 校验在状态检查之前,越权即拒不泄露卡状态。
        assertThatThrownBy(() -> m1Staff.consume("c1", null, "m2", 100L, "x", "sv-c-m2"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
        assertThatThrownBy(() -> m1Staff.consume("c1", null, null, 100L, "x", "sv-c-prj"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
        // 综合体通用卡 → 拒（商户员工不得操作平台级卡）。
        assertThatThrownBy(() -> m1Staff.freeze("c1", null, null, "x"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
        // 配置他商户退款规则 → 拒。
        assertThatThrownBy(() -> m1Staff.configureMerchantRefundRule("m2", true, 5000))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));

        // 平台管理员（scope 空）→ 任意卡放行。
        com.complex.platform.access.AuthorizationService platformAuth =
                org.mockito.Mockito.mock(com.complex.platform.access.AuthorizationService.class);
        org.mockito.Mockito.when(platformAuth.currentMerchantScope()).thenReturn(Optional.empty());
        StoredValueService platform = new StoredValueService(repo, new FakeFinance(), audit(),
                com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled(), platformAuth,
                Clock.fixed(T0, ZoneOffset.UTC));
        assertThat(platform.freeze("c1", null, "m2", "ok").status()).isEqualTo("frozen");
    }

    // ============================================================
    // 付费会员：开通→生效期、到期→expired、续费
    // ============================================================

    @Test
    void subscribeActivatesWithinValidityAndExpiresAfter() {
        InMemoryRepo repo = seeded();
        repo.plans.put("plan1", new CustomerMembershipPlanRecord(
                "plan1", null, "gold", "金卡", 9_900L, 30,
                Map.of("discount_bp", 9000), "active", T0, T0));
        FakeFinance finance = new FakeFinance();
        MembershipService service = new MembershipService(repo, finance, audit(),
                Clock.fixed(T0, ZoneOffset.UTC));

        PaymentIntentRecord intent = service.subscribe("c1", "plan1", "wechat", "sub-1");
        assertThat(intent.amountCent()).isEqualTo(9_900L);
        // 未支付不生效
        assertThatThrownBy(() -> service.activate("c1", "plan1", null, intent.id(), false))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PAYMENT_NOT_SUCCEEDED"));

        finance.markPaymentIntentSucceeded(intent.id());
        CustomerMembershipRecord m = service.activate("c1", "plan1", null, intent.id(), false);
        assertThat(m.status()).isEqualTo("active");
        assertThat(m.expireAt()).isEqualTo(T0.plusSeconds(days(30)));

        // 生效期内权益可用
        assertThat(service.activeBenefits("c1")).containsEntry("discount_bp", 9000);

        // 到期作业（在 T0+31d）→ expired
        MembershipService later = new MembershipService(repo, finance, audit(),
                Clock.fixed(T0.plusSeconds(days(31)), ZoneOffset.UTC));
        int expired = later.expireMemberships(T0.plusSeconds(days(31)));
        assertThat(expired).isEqualTo(1);
        assertThat(repo.memberships.get(m.id()).status()).isEqualTo("expired");
        // 过期后权益不可用
        assertThat(later.activeBenefits("c1")).isEmpty();
    }

    @Test
    void renewExtendsExpiry() {
        InMemoryRepo repo = seeded();
        repo.plans.put("plan1", new CustomerMembershipPlanRecord(
                "plan1", null, "gold", "金卡", 9_900L, 30, Map.of(), "active", T0, T0));
        FakeFinance finance = new FakeFinance();
        MembershipService service = new MembershipService(repo, finance, audit(),
                Clock.fixed(T0, ZoneOffset.UTC));
        PaymentIntentRecord intent = service.subscribe("c1", "plan1", "wechat", "sub-2");
        finance.markPaymentIntentSucceeded(intent.id());
        CustomerMembershipRecord m = service.activate("c1", "plan1", null, intent.id(), true);

        PaymentIntentRecord renewIntent = finance.create("membership_subscribe", "c1", 9_900L, "renew-1");
        finance.markPaymentIntentSucceeded(renewIntent.id());
        CustomerMembershipRecord renewed = service.renew(m.id(), renewIntent.id());
        // 生效期内续费：在原 expire 基础上 +30d
        assertThat(renewed.expireAt()).isEqualTo(T0.plusSeconds(days(60)));
    }

    // ============================================================
    // 支付渠道开关（V093）：会员自动续费扣款 / 储值退款打款
    // ============================================================

    private com.complex.platform.finance.gateway.PaymentGatewayRouter gatewayRouter(boolean enabled) {
        com.complex.platform.terminal.TerminalDeviceRepository terminalRepo =
                org.mockito.Mockito.mock(com.complex.platform.terminal.TerminalDeviceRepository.class);
        org.mockito.Mockito.when(terminalRepo.resolveEffectiveSwitch(
                org.mockito.ArgumentMatchers.eq("*"),
                org.mockito.ArgumentMatchers.eq(com.complex.platform.finance.gateway.PaymentGatewayRouter.CAPABILITY),
                org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(Optional.of(new com.complex.platform.terminal.TerminalCapabilitySwitchRecord(
                        "tcs_payment_gateway_off", "*",
                        com.complex.platform.finance.gateway.PaymentGatewayRouter.CAPABILITY,
                        "global", "global", enabled, "test", T0, T0)));
        return new com.complex.platform.finance.gateway.PaymentGatewayRouter(terminalRepo);
    }

    @Test
    void autoRenewChargeWithGatewayOffIsReservedDoesNotChargeOrExtend() {
        InMemoryRepo repo = seeded();
        repo.plans.put("plan1", new CustomerMembershipPlanRecord(
                "plan1", null, "gold", "金卡", 9_900L, 30, Map.of(), "active", T0, T0));
        FakeFinance finance = new FakeFinance();
        MembershipService service = new MembershipService(repo, finance, audit(),
                gatewayRouter(false), Clock.fixed(T0, ZoneOffset.UTC));
        PaymentIntentRecord intent = service.subscribe("c1", "plan1", "wechat", "sub-ar1");
        finance.markPaymentIntentSucceeded(intent.id());
        CustomerMembershipRecord m = service.activate("c1", "plan1", null, intent.id(), true);

        MembershipService.AutoRenewOutcome outcome = service.autoRenewCharge(m.id());
        assertThat(outcome.charged()).isFalse();
        assertThat(outcome.gatewayStatus()).isEqualTo("RESERVED");
        // 未实际扣款 → 未续期（expire 不变）。
        assertThat(repo.memberships.get(m.id()).expireAt()).isEqualTo(T0.plusSeconds(days(30)));
    }

    @Test
    void autoRenewChargeWithGatewayOnRoutesToPortNotConfiguredNoCrash() {
        InMemoryRepo repo = seeded();
        repo.plans.put("plan1", new CustomerMembershipPlanRecord(
                "plan1", null, "gold", "金卡", 9_900L, 30, Map.of(), "active", T0, T0));
        FakeFinance finance = new FakeFinance();
        MembershipService service = new MembershipService(repo, finance, audit(),
                gatewayRouter(true), Clock.fixed(T0, ZoneOffset.UTC));
        PaymentIntentRecord intent = service.subscribe("c1", "plan1", "wechat", "sub-ar2");
        finance.markPaymentIntentSucceeded(intent.id());
        CustomerMembershipRecord m = service.activate("c1", "plan1", null, intent.id(), true);

        MembershipService.AutoRenewOutcome outcome = service.autoRenewCharge(m.id());
        // 占位 NOT_CONFIGURED：不实际扣款、不续期、不崩。
        assertThat(outcome.charged()).isFalse();
        assertThat(outcome.gatewayStatus()).isEqualTo("NOT_CONFIGURED");
        assertThat(repo.memberships.get(m.id()).expireAt()).isEqualTo(T0.plusSeconds(days(30)));
    }

    @Test
    void storedValueRefundWithGatewayOnDoesNotCrashAndReturnsBalance() {
        InMemoryRepo repo = seeded();
        FakeFinance finance = new FakeFinance();
        StoredValueService service = new StoredValueService(repo, finance, audit(),
                gatewayRouter(true), Clock.fixed(T0, ZoneOffset.UTC));
        PaymentIntentRecord intent = service.topup("c1", null, null, 5_000L, "wechat", "topup-gw");
        finance.markPaymentIntentSucceeded(intent.id());
        service.settleTopup("c1", null, null, intent.id());
        // 退款：系统内余额退回；渠道打款占位不崩。
        service.refund("c1", null, null, 5_000L, "refund all", true, "refund-gw");
        assertThat(repo.storedValue.get("c1|null|null").balanceCent()).isZero();
        assertThat(repo.storedValue.get("c1|null|null").status()).isEqualTo("closed");
    }

    // ============================================================
    // 资金双花防护（B1b）：空幂等键必须拒，否则无幂等保护→可重复扣减/退款
    // ============================================================

    @Test
    void consumeRejectsBlankIdempotencyKey() {
        InMemoryRepo repo = seeded();
        FakeFinance finance = new FakeFinance();
        StoredValueService service = new StoredValueService(repo, finance, audit(),
                Clock.fixed(T0, ZoneOffset.UTC));
        PaymentIntentRecord intent = service.topup("c1", null, null, 5_000L, "wechat", "topup-b1b-c");
        finance.markPaymentIntentSucceeded(intent.id());
        service.settleTopup("c1", null, null, intent.id());

        assertThatThrownBy(() -> service.consume("c1", null, null, 1_000L, "buy", null))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThatThrownBy(() -> service.consume("c1", null, null, 1_000L, "buy", "   "))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        // 余额未被扣减（空键被拒，未发生扣减）。
        assertThat(repo.storedValue.get("c1|null|null").balanceCent()).isEqualTo(5_000L);
    }

    @Test
    void refundRejectsBlankIdempotencyKey() {
        InMemoryRepo repo = seeded();
        FakeFinance finance = new FakeFinance();
        StoredValueService service = new StoredValueService(repo, finance, audit(),
                Clock.fixed(T0, ZoneOffset.UTC));
        PaymentIntentRecord intent = service.topup("c1", null, null, 5_000L, "wechat", "topup-b1b-r");
        finance.markPaymentIntentSucceeded(intent.id());
        service.settleTopup("c1", null, null, intent.id());

        assertThatThrownBy(() -> service.refund("c1", null, null, 1_000L, "r", false, null))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThatThrownBy(() -> service.refund("c1", null, null, 1_000L, "r", false, ""))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        // 余额未被退减。
        assertThat(repo.storedValue.get("c1|null|null").balanceCent()).isEqualTo(5_000L);
    }

    // 积分转赠用例已随「积分转赠下线」移除（HTML：积分不支持会员间转赠）。
    // InMemoryRepo 仍保留转赠 repo 桩方法以实现 CustomerRepository 接口（转赠 repo 方法留孤儿）。

    // ============================================================
    // helpers
    // ============================================================

    private CustomerPointsAccountRecord account(String customerId, long balance) {
        return new CustomerPointsAccountRecord("points_" + customerId, customerId, balance, "base",
                Map.of(), 0L, "active", T0, 0L, null, null, 0, T0, T0);
    }

    private InMemoryRepo seeded() {
        InMemoryRepo repo = new InMemoryRepo();
        repo.saveCustomer(new CustomerRecord("c1", "138", "138", "active", null, T0, T0, T0));
        return repo;
    }

    /** 测试用 finance：仅实现支付意图三方法，其余继承（不调用）。 */
    private static final class FakeFinance extends FinanceService {
        private final Map<String, PaymentIntentRecord> intents = new java.util.LinkedHashMap<>();
        private int seq = 0;

        FakeFinance() {
            super(null, null, null, null, null);
        }

        PaymentIntentRecord create(String type, String orderId, long amount, String key) {
            PaymentIntentRecord intent = new PaymentIntentRecord(
                    "pi_" + (++seq), type, orderId, amount, "CNY", "wechat", "created", T0, T0);
            intents.put(intent.id(), intent);
            return intent;
        }

        @Override
        public PaymentIntentRecord createPaymentIntent(PaymentIntentRequest request) {
            return create(request.businessOrderType(), request.businessOrderId(),
                    request.amountCent(), request.idempotencyKey());
        }

        @Override
        public Optional<PaymentIntentRecord> findPaymentIntent(String id) {
            return Optional.ofNullable(intents.get(id));
        }

        @Override
        public PaymentIntentRecord markPaymentIntentSucceeded(String id) {
            PaymentIntentRecord i = intents.get(id);
            PaymentIntentRecord s = new PaymentIntentRecord(i.id(), i.businessOrderType(),
                    i.businessOrderId(), i.amountCent(), i.currency(), i.channel(), "succeeded", i.createdAt(), T0);
            intents.put(id, s);
            return s;
        }

        // AUD-060 储值退款未兑付登记退款待办：记录调用供断言（真实实现落 finance_refunds requested）。
        final List<String> payoutRefunds = new ArrayList<>();

        @Override
        public String recordStoredValuePayoutRefund(String accountId, long payoutCent, String reason,
                                                    String channel, String idempotencyKey) {
            String id = "refund_sv_" + idempotencyKey;
            payoutRefunds.add(id + "|" + accountId + "|" + payoutCent + "|" + channel);
            return id;
        }
    }

    /** 复用 V090 相关方法的内存 repo（只实现本测试用到的方法）。 */
    private static final class InMemoryRepo implements CustomerRepository {
        final Map<String, CustomerRecord> customers = new java.util.LinkedHashMap<>();
        final Map<String, CustomerPointsAccountRecord> points = new java.util.LinkedHashMap<>();
        final List<CustomerPointsLedgerRecord> pointsLedger = new ArrayList<>();
        final Map<String, CustomerStoredValueAccountRecord> storedValue = new java.util.LinkedHashMap<>();
        final List<CustomerStoredValueTransactionRecord> storedValueTxns = new ArrayList<>();
        final Map<String, CustomerMembershipPlanRecord> plans = new java.util.LinkedHashMap<>();
        final Map<String, CustomerMembershipRecord> memberships = new java.util.LinkedHashMap<>();

        @Override public Optional<CustomerRecord> findCustomerByPhone(String phone) {
            return customers.values().stream().filter(c -> phone.equals(c.phone())).findFirst();
        }
        @Override public Optional<CustomerRecord> findCustomerById(String id) {
            return Optional.ofNullable(customers.get(id));
        }
        @Override public CustomerRecord saveCustomer(CustomerRecord c) { customers.put(c.id(), c); return c; }
        @Override public CustomerSessionRecord saveSession(CustomerSessionRecord s) { return s; }
        @Override public Optional<CustomerSessionRecord> findSessionByToken(String t) { return Optional.empty(); }
        @Override public CustomerConsentRecord saveConsent(CustomerConsentRecord c) { return c; }
        @Override public Optional<CustomerPointsAccountRecord> findPointsByCustomerId(String id) {
            return Optional.ofNullable(points.get(id));
        }
        @Override public Optional<CustomerPointsAccountRecord> findPointsByCustomerIdForUpdate(String id) {
            return Optional.ofNullable(points.get(id));
        }
        @Override public CustomerPointsAccountRecord savePoints(CustomerPointsAccountRecord a) {
            points.put(a.customerId(), a); return a;
        }
        @Override public CustomerPointsLedgerRecord savePointsLedger(CustomerPointsLedgerRecord l) {
            pointsLedger.add(l); return l;
        }
        @Override public List<CustomerPointsLedgerRecord> listPointsLedgerByCustomerId(String id) {
            return pointsLedger.stream().filter(l -> id.equals(l.customerId())).toList();
        }
        @Override public CustomerRedemptionCatalogItemRecord saveRedemptionCatalogItem(CustomerRedemptionCatalogItemRecord i) { return i; }
        @Override public Optional<CustomerRedemptionCatalogItemRecord> findRedemptionCatalogItemById(String id) { return Optional.empty(); }
        @Override public List<CustomerRedemptionCatalogItemRecord> listActiveRedemptionCatalogItems(Instant n) { return List.of(); }
        @Override public CustomerPointsRedemptionRecord savePointsRedemption(CustomerPointsRedemptionRecord r) { return r; }
        @Override public List<CustomerPointsRedemptionRecord> listPointsRedemptionsByCustomerId(String id) { return List.of(); }
        @Override public List<CustomerBarReservationOptionResponse> listBarReservationOptions() { return List.of(); }
        @Override public Optional<com.complex.platform.bar.BarTableRecord> findActiveBarTableForReservation(String b, String t, String tt) { return Optional.empty(); }
        @Override public com.complex.platform.bar.BarReservationRecord saveCustomerBarReservation(com.complex.platform.bar.BarReservationRecord r) { return r; }
        @Override public java.util.List<com.complex.platform.bar.BarDepositRuleRecord> listDepositRules(String barId) { return java.util.List.of(); }

        // V090 storedValue（B1d⑤ 卡范围 (customer, project, merchant)）
        @Override public Optional<CustomerStoredValueAccountRecord> findStoredValueAccount(String cid, String pid) {
            return findStoredValueAccount(cid, pid, null);
        }
        @Override public Optional<CustomerStoredValueAccountRecord> findStoredValueAccountForUpdate(String cid, String pid) {
            return findStoredValueAccount(cid, pid, null);
        }
        @Override public Optional<CustomerStoredValueAccountRecord> findStoredValueAccount(String cid, String pid, String mid) {
            return storedValue.values().stream()
                    .filter(a -> cid.equals(a.customerId()) && java.util.Objects.equals(pid, a.projectId())
                            && java.util.Objects.equals(mid, a.merchantId())).findFirst();
        }
        @Override public Optional<CustomerStoredValueAccountRecord> findStoredValueAccountForUpdate(String cid, String pid, String mid) {
            return findStoredValueAccount(cid, pid, mid);
        }
        @Override public CustomerStoredValueAccountRecord saveStoredValueAccount(CustomerStoredValueAccountRecord a) {
            storedValue.put(a.customerId() + "|" + a.projectId() + "|" + a.merchantId(), a); return a;
        }
        @Override public CustomerStoredValueTransactionRecord saveStoredValueTransaction(CustomerStoredValueTransactionRecord t) {
            storedValueTxns.add(t); return t;
        }
        final Map<String, StoredValueMerchantRefundRuleRecord> refundRules = new java.util.LinkedHashMap<>();
        @Override public Optional<StoredValueMerchantRefundRuleRecord> findStoredValueMerchantRefundRule(String mid) {
            return Optional.ofNullable(refundRules.get(mid));
        }
        @Override public StoredValueMerchantRefundRuleRecord saveStoredValueMerchantRefundRule(StoredValueMerchantRefundRuleRecord r) {
            refundRules.put(r.merchantId(), r); return r;
        }
        @Override public Optional<CustomerStoredValueTransactionRecord> findStoredValueTransactionByIdempotencyKey(String k) {
            return storedValueTxns.stream().filter(t -> k != null && k.equals(t.idempotencyKey())).findFirst();
        }
        @Override public boolean storedValueTransactionExistsForPaymentIntent(String pi) {
            return storedValueTxns.stream().anyMatch(t -> "topup".equals(t.txnType()) && pi.equals(t.paymentIntentId()));
        }
        // AUD-058 充值支付意图 ↔ 卡范围绑定
        final Map<String, StoredValueTopupIntentScope> topupIntents = new java.util.LinkedHashMap<>();
        @Override public void bindStoredValueTopupIntent(String pi, String cid, String pjid, String mid, Instant at) {
            topupIntents.put(pi, new StoredValueTopupIntentScope(pi, cid, pjid, mid));
        }
        @Override public Optional<StoredValueTopupIntentScope> findStoredValueTopupIntent(String pi) {
            return Optional.ofNullable(topupIntents.get(pi));
        }
        @Override public List<CustomerStoredValueTransactionRecord> listStoredValueTransactions(String accId) {
            return storedValueTxns.stream().filter(t -> accId.equals(t.accountId())).toList();
        }

        // V090 membership
        @Override public CustomerMembershipPlanRecord saveMembershipPlan(CustomerMembershipPlanRecord p) {
            plans.put(p.id(), p); return p;
        }
        @Override public Optional<CustomerMembershipPlanRecord> findMembershipPlanById(String id) {
            return Optional.ofNullable(plans.get(id));
        }
        @Override public List<CustomerMembershipPlanRecord> listMembershipPlans(String pid) {
            return plans.values().stream().filter(p -> "active".equals(p.status())).toList();
        }
        @Override public CustomerMembershipRecord saveMembership(CustomerMembershipRecord m) {
            memberships.put(m.id(), m); return m;
        }
        @Override public Optional<CustomerMembershipRecord> findMembershipByIdForUpdate(String id) {
            return Optional.ofNullable(memberships.get(id));
        }
        @Override public Optional<CustomerMembershipRecord> findMembershipByIdempotencyKey(String k) {
            return memberships.values().stream().filter(m -> k != null && k.equals(m.idempotencyKey())).findFirst();
        }
        @Override public List<CustomerMembershipRecord> listMembershipsByCustomerId(String cid) {
            return memberships.values().stream().filter(m -> cid.equals(m.customerId())).toList();
        }
        @Override public List<CustomerMembershipRecord> listActiveMembershipsExpiringBefore(Instant now) {
            return memberships.values().stream()
                    .filter(m -> "active".equals(m.status()) && !m.expireAt().isAfter(now)).toList();
        }

    }
}
