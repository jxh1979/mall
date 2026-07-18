package com.complex.platform.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.ticketing.PointsConfirmRequestedEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 会员忠诚度 第1部分逻辑单测：降级/宽限/保级、积分过期、冻结/解冻、连续签到阶梯、生日加倍。 */
class LoyaltyServiceTests {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private LoyaltyService serviceAt(InMemoryLoyaltyRepository repo, AuditEventService audit, Instant now) {
        return new LoyaltyService(repo, audit, Clock.fixed(now, ZoneOffset.UTC));
    }

    private InMemoryLoyaltyRepository seededRepo() {
        InMemoryLoyaltyRepository repo = new InMemoryLoyaltyRepository();
        repo.saveCustomer(new CustomerRecord("c1", "13800000000", "138****0000", "active", null, T0, T0, T0));
        // 等级：base(rank0,保级0) / silver(rank1,升级1000,保级500,周期30天,宽限7天,倍率1.5x)
        repo.saveMemberLevel(new CustomerMemberLevelRecord(
                "lv_base", null, "base", "基础", 0, 0, 0, 365, 0, 10000, "active", T0, T0));
        repo.saveMemberLevel(new CustomerMemberLevelRecord(
                "lv_silver", null, "silver", "白银", 1, 1000, 500, 30, 7, 15000, "active", T0, T0));
        return repo;
    }

    private long days(int d) {
        return (long) d * 86_400L;
    }

    // ---- 连续签到阶梯 + 断签重置 ----
    @Test
    void checkInIncrementsStreakAndAwardsTierThenResetsOnBreak() {
        InMemoryLoyaltyRepository repo = seededRepo();
        AuditEventService audit = new AuditEventService(new RecordingAudit());
        repo.saveCheckInTier(new CustomerCheckInTierRecord("t3", null, 3, 50, "active", T0, T0));

        // day1
        CustomerCheckInRecord d1 = serviceAt(repo, audit, T0).checkIn("c1", "miniapp");
        assertThat(d1.streak()).isEqualTo(1);
        assertThat(d1.awardedPoints()).isZero();
        // day2
        CustomerCheckInRecord d2 = serviceAt(repo, audit, T0.plusSeconds(days(1))).checkIn("c1", "miniapp");
        assertThat(d2.streak()).isEqualTo(2);
        // day3 -> hits tier 3 => +50
        CustomerCheckInRecord d3 = serviceAt(repo, audit, T0.plusSeconds(days(2))).checkIn("c1", "miniapp");
        assertThat(d3.streak()).isEqualTo(3);
        assertThat(d3.awardedPoints()).isEqualTo(50);
        assertThat(repo.points.get("c1").balance()).isEqualTo(50);

        // skip day4, check-in day5 -> streak resets to 1
        CustomerCheckInRecord d5 = serviceAt(repo, audit, T0.plusSeconds(days(4))).checkIn("c1", "miniapp");
        assertThat(d5.streak()).isEqualTo(1);
    }

    @Test
    void checkInTwiceSameDayIsRejected() {
        InMemoryLoyaltyRepository repo = seededRepo();
        AuditEventService audit = new AuditEventService(new RecordingAudit());
        LoyaltyService service = serviceAt(repo, audit, T0);
        service.checkIn("c1", "miniapp");
        assertThatThrownBy(() -> service.checkIn("c1", "miniapp"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("ALREADY_CHECKED_IN"));
    }

    // ---- 生日加倍倍率（叠加等级倍率）----
    @Test
    void earnPointsAppliesBirthdayMultiplierOnBirthday() {
        InMemoryLoyaltyRepository repo = new InMemoryLoyaltyRepository();
        repo.saveCustomer(new CustomerRecord("c1", "138", "138", "active",
                LocalDate.of(1990, 1, 1), T0, T0, T0));
        repo.saveBirthdayRule(new CustomerBirthdayRuleRecord(
                "br", null, "生日双倍", "birthday_day", 20000, "active", T0, T0));
        AuditEventService audit = new AuditEventService(new RecordingAudit());
        // T0 = 2026-01-01 == birthday day -> 100 * 2.0 = 200 (no level multiplier configured -> 1.0x)
        long awarded = serviceAt(repo, audit, T0).earnPoints("c1", 100, "consume_amount", "order1", "buy", 365);
        assertThat(awarded).isEqualTo(200);
        assertThat(repo.points.get("c1").balance()).isEqualTo(200);
    }

    @Test
    void earnPointsNoBirthdayMultiplierOffWindow() {
        InMemoryLoyaltyRepository repo = new InMemoryLoyaltyRepository();
        repo.saveCustomer(new CustomerRecord("c1", "138", "138", "active",
                LocalDate.of(1990, 7, 1), T0, T0, T0));
        repo.saveBirthdayRule(new CustomerBirthdayRuleRecord(
                "br", null, "生日双倍", "birthday_day", 20000, "active", T0, T0));
        AuditEventService audit = new AuditEventService(new RecordingAudit());
        long awarded = serviceAt(repo, audit, T0).earnPoints("c1", 100, "consume_amount", "order1", "buy", null);
        assertThat(awarded).isEqualTo(100);
    }

    // ---- 积分过期清零 ----
    @Test
    void expirePointsZeroesOutExpiredBatch() {
        InMemoryLoyaltyRepository repo = seededRepo();
        AuditEventService audit = new AuditEventService(new RecordingAudit());
        // earn 100 valid 10 days at T0
        serviceAt(repo, audit, T0).earnPoints("c1", 100, "consume_amount", "o1", "buy", 10);
        assertThat(repo.points.get("c1").balance()).isEqualTo(100);
        // run expiry at T0+11d -> expired
        long expired = serviceAt(repo, audit, T0.plusSeconds(days(11))).expirePoints(T0.plusSeconds(days(11)));
        assertThat(expired).isEqualTo(100);
        assertThat(repo.points.get("c1").balance()).isZero();
        assertThat(repo.pointsLedger).anyMatch(l -> "expire".equals(l.sourceType()) && l.pointsDelta() == -100);
    }

    @Test
    void expirePointsKeepsUnexpiredBatch() {
        InMemoryLoyaltyRepository repo = seededRepo();
        AuditEventService audit = new AuditEventService(new RecordingAudit());
        serviceAt(repo, audit, T0).earnPoints("c1", 100, "consume_amount", "o1", "buy", 30);
        long expired = serviceAt(repo, audit, T0.plusSeconds(days(11))).expirePoints(T0.plusSeconds(days(11)));
        assertThat(expired).isZero();
        assertThat(repo.points.get("c1").balance()).isEqualTo(100);
    }

    // ---- 冻结 / 解冻 ----
    @Test
    void freezeBlocksEarnAndUnfreezeRestores() {
        InMemoryLoyaltyRepository repo = seededRepo();
        AuditEventService audit = new AuditEventService(new RecordingAudit());
        serviceAt(repo, audit, T0).earnPoints("c1", 100, "consume_amount", "o1", "buy", null);
        serviceAt(repo, audit, T0).freeze("c1", "risk");
        assertThat(repo.points.get("c1").accountStatus()).isEqualTo("frozen");
        assertThatThrownBy(() -> serviceAt(repo, audit, T0).earnPoints("c1", 50, "consume_amount", "o2", "buy", null))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("POINTS_ACCOUNT_FROZEN"));
        serviceAt(repo, audit, T0).unfreeze("c1", "cleared");
        assertThat(repo.points.get("c1").accountStatus()).isEqualTo("active");
        long awarded = serviceAt(repo, audit, T0).earnPoints("c1", 50, "consume_amount", "o3", "buy", null);
        assertThat(awarded).isEqualTo(50);
    }

    // ---- 等级升级 ----
    @Test
    void evaluateUpgradesWhenProgressMeetsThreshold() {
        InMemoryLoyaltyRepository repo = seededRepo();
        AuditEventService audit = new AuditEventService(new RecordingAudit());
        serviceAt(repo, audit, T0).earnPoints("c1", 1200, "consume_amount", "o1", "buy", null);
        // base account default level "base", period start T0
        int changed = serviceAt(repo, audit, T0).evaluateLevels(T0);
        assertThat(changed).isEqualTo(1);
        assertThat(repo.points.get("c1").level()).isEqualTo("silver");
    }

    // ---- 周期末未达标：宽限内保留，超宽限降级 ----
    @Test
    void periodEndShortfallEntersGraceThenDowngrades() {
        InMemoryLoyaltyRepository repo = seededRepo();
        AuditEventService audit = new AuditEventService(new RecordingAudit());
        // put account at silver, period start T0, low progress (below keep 500)
        repo.points.put("c1", new CustomerPointsAccountRecord(
                "p1", "c1", 100, "silver", Map.of(), 0L, "active", T0, 100L, null, null, 0, T0, T0));

        // at T0+31d (period 30d ended), shortfall -> grace (within 7d grace), level kept
        Instant afterPeriod = T0.plusSeconds(days(31));
        CustomerPointsAccountRecord graced = serviceAt(repo, audit, afterPeriod).evaluateLevel("c1", afterPeriod);
        assertThat(graced.level()).isEqualTo("silver");
        assertThat(graced.graceUntil()).isNotNull();

        // at T0+40d (> period 30 + grace 7 = 37) -> downgrade to base
        Instant afterGrace = T0.plusSeconds(days(40));
        CustomerPointsAccountRecord downgraded = serviceAt(repo, audit, afterGrace).evaluateLevel("c1", afterGrace);
        assertThat(downgraded.level()).isEqualTo("base");
    }

    // ---- 周期末达标：保级（重置周期/进度）----
    @Test
    void periodEndMeetingKeepThresholdKeepsLevel() {
        InMemoryLoyaltyRepository repo = seededRepo();
        AuditEventService audit = new AuditEventService(new RecordingAudit());
        repo.points.put("c1", new CustomerPointsAccountRecord(
                "p1", "c1", 600, "silver", Map.of(), 0L, "active", T0, 600L, null, null, 0, T0, T0));
        Instant afterPeriod = T0.plusSeconds(days(31));
        CustomerPointsAccountRecord kept = serviceAt(repo, audit, afterPeriod).evaluateLevel("c1", afterPeriod);
        assertThat(kept.level()).isEqualTo("silver");
        assertThat(kept.keepThresholdProgress()).isZero(); // reset
        assertThat(kept.levelPeriodStart()).isEqualTo(afterPeriod); // new period
    }

    // ===================== P0 清单#2 积分入账引擎：earn→confirm→deduct =====================

    /** E2E：支付入 pending（不计余额）→ 履约 confirm（计余额）→ 退款 deduct（按比例冲减已入账）。 */
    @Test
    void earnPendingThenConfirmThenDeductEndToEnd() {
        InMemoryLoyaltyRepository repo = seededRepo();
        AuditEventService audit = new AuditEventService(new RecordingAudit());
        LoyaltyService service = serviceAt(repo, audit, T0);

        // 支付成功：写 pending，余额不变（pending 不可花）。
        long pending = service.earnPointsPending("c1", 100, "ticket_order", "o1", "buy", null);
        assertThat(pending).isEqualTo(100);
        assertThat(repo.points.get("c1").balance()).isZero();
        assertThat(repo.pointsLedger).anyMatch(l -> "o1".equals(l.sourceDocId())
                && "earn".equals(l.changeType()) && "pending".equals(l.status()));

        // 履约（核销/完成）：转 confirmed，计入余额 + 保级进度。
        long confirmed = service.confirmPoints("ticket_order", "o1");
        assertThat(confirmed).isEqualTo(100);
        assertThat(repo.points.get("c1").balance()).isEqualTo(100);
        assertThat(repo.pointsLedger).anyMatch(l -> "o1".equals(l.sourceDocId())
                && "earn".equals(l.changeType()) && "confirmed".equals(l.status()));

        // 退款 60%：按已 confirmed 比例冲减 60 分，余额 40。
        long deducted = service.deductPoints("ticket_order", "o1", "ticket_refund", "r1", 6000);
        assertThat(deducted).isEqualTo(60);
        assertThat(repo.points.get("c1").balance()).isEqualTo(40);
        assertThat(repo.pointsLedger).anyMatch(l -> "r1".equals(l.sourceDocId())
                && "deduct".equals(l.changeType()) && l.pointsDelta() == -60);
    }

    /** 幂等：earn / confirm / deduct 重放都不重复入账。 */
    @Test
    void earnConfirmDeductAreIdempotentOnReplay() {
        InMemoryLoyaltyRepository repo = seededRepo();
        AuditEventService audit = new AuditEventService(new RecordingAudit());
        LoyaltyService service = serviceAt(repo, audit, T0);

        // earn 重放：第二次命中既有流水，不新增 earn 行。
        service.earnPointsPending("c1", 100, "ticket_order", "o1", "buy", null);
        service.earnPointsPending("c1", 100, "ticket_order", "o1", "buy", null);
        assertThat(repo.pointsLedger.stream().filter(l -> "earn".equals(l.changeType())).count()).isEqualTo(1);

        // confirm 重放：第二次为 noop，余额仍为 100（不二次加账）。
        assertThat(service.confirmPoints("ticket_order", "o1")).isEqualTo(100);
        assertThat(service.confirmPoints("ticket_order", "o1")).isZero();
        assertThat(repo.points.get("c1").balance()).isEqualTo(100);

        // deduct 重放：第二次返回既有冲减额，不二次冲减，余额仍为 40。
        assertThat(service.deductPoints("ticket_order", "o1", "ticket_refund", "r1", 6000)).isEqualTo(60);
        assertThat(service.deductPoints("ticket_order", "o1", "ticket_refund", "r1", 6000)).isEqualTo(60);
        assertThat(repo.points.get("c1").balance()).isEqualTo(40);
        assertThat(repo.pointsLedger.stream().filter(l -> "deduct".equals(l.changeType())).count()).isEqualTo(1);
    }

    /** F-TXN-03 按核销张数：多次票部分核销按 Σused/Σtotal 比例确认，冲正按份回滚（confirm-to-target）。 */
    @Test
    void confirmByVerifiedUnitsIsProportionalAndReversible() {
        InMemoryLoyaltyRepository repo = seededRepo();
        AuditEventService audit = new AuditEventService(new RecordingAudit());
        LoyaltyService service = serviceAt(repo, audit, T0);

        // 3 张多次票，整单 99 分 pending（余额不变）。
        service.earnPointsPending("c1", 99, "ticket_order", "o1", "buy", null);
        assertThat(repo.points.get("c1").balance()).isZero();

        // 核销 1/3 → 确认 floor(99×1/3)=33，余额 33。
        assertThat(service.confirmPoints("ticket_order", "o1", 1, 3)).isEqualTo(33);
        assertThat(repo.points.get("c1").balance()).isEqualTo(33);
        // 核销 2/3 → 对齐 floor(99×2/3)=66，本次 +33，余额 66。
        assertThat(service.confirmPoints("ticket_order", "o1", 2, 3)).isEqualTo(33);
        assertThat(repo.points.get("c1").balance()).isEqualTo(66);
        // 核销 3/3 → 对齐 99，余额 99。
        assertThat(service.confirmPoints("ticket_order", "o1", 3, 3)).isEqualTo(33);
        assertThat(repo.points.get("c1").balance()).isEqualTo(99);

        // 冲正一张（回 2/3）→ 对齐 66，本次 -33，余额 66。
        assertThat(service.confirmPoints("ticket_order", "o1", 2, 3)).isEqualTo(-33);
        assertThat(repo.points.get("c1").balance()).isEqualTo(66);
        // 全部冲正（0/3）→ 对齐 0，余额 0，earn 行回 pending、confirmed_amount=0。
        assertThat(service.confirmPoints("ticket_order", "o1", 0, 3)).isEqualTo(-66);
        assertThat(repo.points.get("c1").balance()).isZero();
        assertThat(repo.pointsLedger).anyMatch(l -> "o1".equals(l.sourceDocId())
                && "earn".equals(l.changeType()) && "pending".equals(l.status()) && l.confirmedAmount() == 0);

        // 重复同张数事件幂等：再发 (0,3) delta=0、不二次变动。
        assertThat(service.confirmPoints("ticket_order", "o1", 0, 3)).isZero();
        assertThat(repo.points.get("c1").balance()).isZero();
    }

    /** 退款只冲减已 confirmed 部分：未履约（仍 pending）时 deduct 无可冲减。 */
    @Test
    void deductOnlyReversesConfirmedNotPending() {
        InMemoryLoyaltyRepository repo = seededRepo();
        AuditEventService audit = new AuditEventService(new RecordingAudit());
        LoyaltyService service = serviceAt(repo, audit, T0);

        service.earnPointsPending("c1", 100, "ticket_order", "o1", "buy", null);
        // 未 confirm 即退款：confirmed=0，无可冲减。
        long deducted = service.deductPoints("ticket_order", "o1", "ticket_refund", "r1", 10000);
        assertThat(deducted).isZero();
        assertThat(repo.points.get("c1").balance()).isZero();
    }

    /** 不超兑：deduct 受账户余额行锁口径约束，余额不足只冲减可用部分（不为负）。 */
    @Test
    void deductDoesNotOverReverseBelowZeroBalance() {
        InMemoryLoyaltyRepository repo = seededRepo();
        AuditEventService audit = new AuditEventService(new RecordingAudit());
        LoyaltyService service = serviceAt(repo, audit, T0);

        service.earnPointsPending("c1", 100, "ticket_order", "o1", "buy", null);
        service.confirmPoints("ticket_order", "o1");
        // 模拟期间已被花掉 80（余额 20），再全额退款应只冲减 20，不为负。
        CustomerPointsAccountRecord acct = repo.points.get("c1");
        repo.points.put("c1", new CustomerPointsAccountRecord(acct.id(), acct.customerId(), 20,
                acct.level(), acct.recentSummary(), acct.frozenPoints(), acct.accountStatus(),
                acct.levelPeriodStart(), acct.keepThresholdProgress(), acct.graceUntil(),
                acct.lastCheckInDate(), acct.checkInStreak(), acct.createdAt(), acct.updatedAt()));
        long deducted = service.deductPoints("ticket_order", "o1", "ticket_refund", "r1", 10000);
        assertThat(deducted).isEqualTo(20);
        assertThat(repo.points.get("c1").balance()).isZero();
    }

    // ============ F-TXN-03 核销冲正回滚积分（confirm-to-target，按核销张数）============

    /**
     * 核销冲正按份回滚（与"核销→confirm"对称）：confirm 把订单积分入账后，冲正路径发带更低 Σused/Σtotal 的
     * 确认事件，消费者据此把已确认份额对齐更低 target——单次票全部冲正即回滚至 0，earn 行回 pending、
     * 不产生独立 deduct 流水（取代原"冲正全额 deduct"）。
     */
    @Test
    void verificationReversalRollsBackConfirmedPointsByShare() {
        InMemoryLoyaltyRepository repo = seededRepo();
        AuditEventService audit = new AuditEventService(new RecordingAudit());
        LoyaltyService service = serviceAt(repo, audit, T0);
        LoyaltyPointsConsumer consumer = new LoyaltyPointsConsumer(service);

        // 支付入 pending → 履约 confirm（单次票 1/1，计余额 100）。
        service.earnPointsPending("c1", 100, "ticket_order", "o1", "buy", null);
        consumer.onPointsConfirmRequested(new PointsConfirmRequestedEvent("o1", 1, 1));
        assertThat(repo.points.get("c1").balance()).isEqualTo(100);

        // 核销冲正（回到 0/1）：对齐 target=0 → 余额 0，earn 行回 pending、confirmed_amount=0，无 deduct 流水。
        consumer.onPointsConfirmRequested(new PointsConfirmRequestedEvent("o1", 0, 1));
        assertThat(repo.points.get("c1").balance()).isZero();
        assertThat(repo.pointsLedger).anyMatch(l -> "o1".equals(l.sourceDocId())
                && "earn".equals(l.changeType()) && "pending".equals(l.status()) && l.confirmedAmount() == 0);
        assertThat(repo.pointsLedger).noneMatch(l -> "deduct".equals(l.changeType()));
    }

    /** 冲正幂等：同一最新张数的确认事件重复到达，对齐 target 后 delta=0、不二次变动。 */
    @Test
    void verificationReversalIsIdempotent() {
        InMemoryLoyaltyRepository repo = seededRepo();
        AuditEventService audit = new AuditEventService(new RecordingAudit());
        LoyaltyService service = serviceAt(repo, audit, T0);
        LoyaltyPointsConsumer consumer = new LoyaltyPointsConsumer(service);

        service.earnPointsPending("c1", 100, "ticket_order", "o1", "buy", null);
        consumer.onPointsConfirmRequested(new PointsConfirmRequestedEvent("o1", 1, 1));

        // 冲正事件（0/1）重复三次：首次回滚至 0，其余 delta=0 noop。
        consumer.onPointsConfirmRequested(new PointsConfirmRequestedEvent("o1", 0, 1));
        consumer.onPointsConfirmRequested(new PointsConfirmRequestedEvent("o1", 0, 1));
        consumer.onPointsConfirmRequested(new PointsConfirmRequestedEvent("o1", 0, 1));

        assertThat(repo.points.get("c1").balance()).isZero();
        assertThat(repo.pointsLedger.stream().filter(l -> "earn".equals(l.changeType())).count()).isEqualTo(1);
        assertThat(repo.pointsLedger).noneMatch(l -> "deduct".equals(l.changeType()));
    }

    /**
     * 退款按已确认份额冲减（取代原"按 status=confirmed 全额"）：多次票部分核销只确认部分份额，
     * 退款全额比例只冲减已确认份额。现行"核销后不可退"使退款实际只发生在全未核销→份额 0→自然 noop；
     * 本用例直接验证 deductPoints 的份额基准口径。
     */
    @Test
    void refundDeductsByConfirmedShareNotFullOrder() {
        InMemoryLoyaltyRepository repo = seededRepo();
        AuditEventService audit = new AuditEventService(new RecordingAudit());
        LoyaltyService service = serviceAt(repo, audit, T0);
        LoyaltyPointsConsumer consumer = new LoyaltyPointsConsumer(service);

        // 3 张多次票 99 分，部分核销 2/3 → 已确认份额 66、余额 66。
        service.earnPointsPending("c1", 99, "ticket_order", "o1", "buy", null);
        consumer.onPointsConfirmRequested(new PointsConfirmRequestedEvent("o1", 2, 3));
        assertThat(repo.points.get("c1").balance()).isEqualTo(66);

        // 退款 100%：按已确认份额 66 冲减（非整单 99），余额 0，落 ticket_refund/r1 deduct -66。
        long deducted = service.deductPoints("ticket_order", "o1", "ticket_refund", "r1", 10000);
        assertThat(deducted).isEqualTo(66);
        assertThat(repo.points.get("c1").balance()).isZero();
        assertThat(repo.pointsLedger).anyMatch(l -> "ticket_refund".equals(l.sourceDocType())
                && "r1".equals(l.sourceDocId()) && "deduct".equals(l.changeType()) && l.pointsDelta() == -66);
    }

    /** 无订单（直接发放无 orderId）的确认/冲正事件：消费者按 orderId 空判定跳过，不动任何流水。 */
    @Test
    void confirmEventWithoutOrderIsNoop() {
        InMemoryLoyaltyRepository repo = seededRepo();
        AuditEventService audit = new AuditEventService(new RecordingAudit());
        LoyaltyService service = serviceAt(repo, audit, T0);
        LoyaltyPointsConsumer consumer = new LoyaltyPointsConsumer(service);

        consumer.onPointsConfirmRequested(new PointsConfirmRequestedEvent(null, 0, 1));
        consumer.onPointsConfirmRequested(new PointsConfirmRequestedEvent("  ", 0, 1));

        assertThat(repo.pointsLedger).isEmpty();
    }

    /** 未履约即冲正：积分仍 pending（confirmed_amount=0），对齐 target=0 → delta=0 noop，余额不变、无 deduct。 */
    @Test
    void verificationReversalOnUnconfirmedIsNoop() {
        InMemoryLoyaltyRepository repo = seededRepo();
        AuditEventService audit = new AuditEventService(new RecordingAudit());
        LoyaltyService service = serviceAt(repo, audit, T0);
        LoyaltyPointsConsumer consumer = new LoyaltyPointsConsumer(service);

        service.earnPointsPending("c1", 100, "ticket_order", "o1", "buy", null);
        // 未 confirm 即冲正：confirmed_amount=0，对齐 target=0 → delta=0 noop。
        consumer.onPointsConfirmRequested(new PointsConfirmRequestedEvent("o1", 0, 1));

        assertThat(repo.points.get("c1").balance()).isZero();
        assertThat(repo.pointsLedger).noneMatch(l -> "deduct".equals(l.changeType()));
    }

    // ===================== P2 缺口：积分入账审计留痕 + 转赠客人端自助口径 =====================

    /**
     * P2-1（红→绿）：earnPoints 入账后必须有 points.earned 审计留痕（与 confirmPoints/earnPointsPending 口径一致）。
     * 修复前：earnPoints 全程不调 auditEventService —— audit 无任何事件，断言失败（红）。
     * 修复后：earnPoints 发 points.earned（category=loyalty, subject=customer_points）—— 断言通过（绿）。
     */
    @Test
    void earnPointsRecordsPointsEarnedAudit() {
        InMemoryLoyaltyRepository repo = seededRepo();
        RecordingAudit recording = new RecordingAudit();
        AuditEventService audit = new AuditEventService(recording);

        long awarded = serviceAt(repo, audit, T0).earnPoints("c1", 100, "consume_amount", "o1", "buy", 365);
        assertThat(awarded).isEqualTo(100);

        // 必须留痕 points.earned；与 earnPointsPending/confirmPoints 口径一致：business_id=customerId(c1)，
        // 来源单据通过 idempotencyKey/metadata.source_id(o1) 关联。
        assertThat(recording.events)
                .anyMatch(e -> "points.earned".equals(e.eventName())
                        && "success".equals(e.eventResult())
                        && "c1".equals(e.businessId())
                        && e.metadata() != null
                        && "o1".equals(e.metadata().get("source_id"))
                        && ((Number) e.metadata().get("points")).longValue() == 100L);
    }

    // P2-2 积分转赠客人端自助用例已随「积分转赠下线」移除（HTML：积分不支持会员间转赠）。

    // ===================== In-memory repo =====================

    private static final class RecordingAudit implements AuditEventRepository {
        final List<AuditEvent> events = new ArrayList<>();

        @Override
        public AuditEvent save(AuditEvent event) {
            events.add(event);
            return event;
        }
    }

    private static final class InMemoryLoyaltyRepository implements CustomerRepository {
        final Map<String, CustomerRecord> customers = new java.util.LinkedHashMap<>();
        final Map<String, CustomerPointsAccountRecord> points = new java.util.LinkedHashMap<>();
        final List<CustomerPointsLedgerRecord> pointsLedger = new ArrayList<>();
        final Map<String, CustomerMemberLevelRecord> levels = new java.util.LinkedHashMap<>();
        final Map<String, CustomerCheckInTierRecord> tiers = new java.util.LinkedHashMap<>();
        final Map<String, CustomerBirthdayRuleRecord> birthdayRules = new java.util.LinkedHashMap<>();
        final Map<String, CustomerCheckInRecord> checkIns = new java.util.LinkedHashMap<>();

        @Override
        public java.util.Optional<CustomerRecord> findCustomerByPhone(String phone) {
            return customers.values().stream().filter(c -> phone.equals(c.phone())).findFirst();
        }

        @Override
        public java.util.Optional<CustomerRecord> findCustomerById(String customerId) {
            return java.util.Optional.ofNullable(customers.get(customerId));
        }

        @Override
        public CustomerRecord saveCustomer(CustomerRecord customer) {
            customers.put(customer.id(), customer);
            return customer;
        }

        @Override
        public CustomerSessionRecord saveSession(CustomerSessionRecord session) {
            return session;
        }

        @Override
        public java.util.Optional<CustomerSessionRecord> findSessionByToken(String token) {
            return java.util.Optional.empty();
        }

        @Override
        public int revokeSessionByToken(String token) {
            return 0;
        }

        @Override
        public CustomerConsentRecord saveConsent(CustomerConsentRecord consent) {
            return consent;
        }

        @Override
        public java.util.Optional<CustomerPointsAccountRecord> findPointsByCustomerId(String customerId) {
            return java.util.Optional.ofNullable(points.get(customerId));
        }

        @Override
        public java.util.Optional<CustomerPointsAccountRecord> findPointsByCustomerIdForUpdate(String customerId) {
            return java.util.Optional.ofNullable(points.get(customerId));
        }

        @Override
        public CustomerPointsAccountRecord savePoints(CustomerPointsAccountRecord account) {
            points.put(account.customerId(), account);
            return account;
        }

        @Override
        public CustomerPointsLedgerRecord savePointsLedger(CustomerPointsLedgerRecord ledger) {
            // 模拟 V100 幂等唯一索引 (source_doc_type, source_doc_id, change_type)：重复落账抛冲突。
            if (ledger.sourceDocType() != null && ledger.sourceDocId() != null
                    && findPointsLedgerBySourceDoc(ledger.sourceDocType(), ledger.sourceDocId(), ledger.changeType())
                            .isPresent()) {
                throw new IllegalStateException("duplicate ledger for source doc (unique index)");
            }
            pointsLedger.add(ledger);
            return ledger;
        }

        @Override
        public List<CustomerPointsLedgerRecord> listPointsLedgerByCustomerId(String customerId) {
            return pointsLedger.stream().filter(l -> customerId.equals(l.customerId())).toList();
        }

        @Override
        public java.util.Optional<CustomerPointsLedgerRecord> findPointsLedgerBySourceDoc(
                String sourceDocType, String sourceDocId, String changeType) {
            return pointsLedger.stream()
                    .filter(l -> sourceDocType.equals(l.sourceDocType())
                            && sourceDocId.equals(l.sourceDocId())
                            && changeType.equals(l.changeType()))
                    .findFirst();
        }

        @Override
        public List<CustomerPointsLedgerRecord> listPointsLedgerBySourceDoc(
                String sourceDocType, String sourceDocId, String changeType) {
            return pointsLedger.stream()
                    .filter(l -> sourceDocType.equals(l.sourceDocType())
                            && sourceDocId.equals(l.sourceDocId())
                            && changeType.equals(l.changeType()))
                    .toList();
        }

        @Override
        public void updatePointsLedgerStatus(String ledgerId, String status) {
            replace(ledgerId, l -> new CustomerPointsLedgerRecord(l.id(), l.customerId(), l.pointsDelta(),
                    l.balanceAfter(), l.sourceType(), l.sourceId(), l.description(), l.expireAt(),
                    l.remainingPoints(), l.expired(), l.createdAt(), l.changeType(), status,
                    l.sourceDocType(), l.sourceDocId()));
        }

        @Override
        public void updatePointsLedgerConfirmation(String ledgerId, long confirmedAmount, String status, Long remainingPoints) {
            replace(ledgerId, l -> new CustomerPointsLedgerRecord(l.id(), l.customerId(), l.pointsDelta(),
                    l.balanceAfter(), l.sourceType(), l.sourceId(), l.description(), l.expireAt(),
                    remainingPoints == null ? l.remainingPoints() : remainingPoints, l.expired(), l.createdAt(),
                    l.changeType(), status, l.sourceDocType(), l.sourceDocId(), confirmedAmount));
        }

        @Override
        public CustomerRedemptionCatalogItemRecord saveRedemptionCatalogItem(CustomerRedemptionCatalogItemRecord item) {
            return item;
        }

        @Override
        public java.util.Optional<CustomerRedemptionCatalogItemRecord> findRedemptionCatalogItemById(String itemId) {
            return java.util.Optional.empty();
        }

        @Override
        public List<CustomerRedemptionCatalogItemRecord> listActiveRedemptionCatalogItems(Instant now) {
            return List.of();
        }

        @Override
        public CustomerPointsRedemptionRecord savePointsRedemption(CustomerPointsRedemptionRecord redemption) {
            return redemption;
        }

        @Override
        public List<CustomerPointsRedemptionRecord> listPointsRedemptionsByCustomerId(String customerId) {
            return List.of();
        }

        @Override
        public List<CustomerBarReservationOptionResponse> listBarReservationOptions() {
            return List.of();
        }

        @Override
        public java.util.Optional<com.complex.platform.bar.BarTableRecord> findActiveBarTableForReservation(
                String barId, String tableId, String tableType) {
            return java.util.Optional.empty();
        }

        @Override
        public com.complex.platform.bar.BarReservationRecord saveCustomerBarReservation(
                com.complex.platform.bar.BarReservationRecord reservation) {
            return reservation;
        }

        @Override
        public java.util.List<com.complex.platform.bar.BarDepositRuleRecord> listDepositRules(String barId) {
            return java.util.List.of();
        }

        @Override
        public List<CustomerPointsAccountRecord> listAllPointsAccounts() {
            return new ArrayList<>(points.values());
        }

        @Override
        public CustomerMemberLevelRecord saveMemberLevel(CustomerMemberLevelRecord level) {
            levels.put(level.id(), level);
            return level;
        }

        @Override
        public List<CustomerMemberLevelRecord> listMemberLevels(String projectId) {
            return levels.values().stream()
                    .filter(l -> "active".equals(l.status()))
                    .sorted((a, b) -> Integer.compare(a.levelRank(), b.levelRank()))
                    .toList();
        }

        @Override
        public CustomerCheckInTierRecord saveCheckInTier(CustomerCheckInTierRecord tier) {
            tiers.put(tier.id(), tier);
            return tier;
        }

        @Override
        public List<CustomerCheckInTierRecord> listCheckInTiers(String projectId) {
            return tiers.values().stream()
                    .filter(t -> "active".equals(t.status()))
                    .sorted((a, b) -> Integer.compare(a.streakDays(), b.streakDays()))
                    .toList();
        }

        @Override
        public CustomerBirthdayRuleRecord saveBirthdayRule(CustomerBirthdayRuleRecord rule) {
            birthdayRules.put(rule.id(), rule);
            return rule;
        }

        @Override
        public List<CustomerBirthdayRuleRecord> listActiveBirthdayRules(String projectId) {
            return birthdayRules.values().stream().filter(r -> "active".equals(r.status())).toList();
        }

        @Override
        public CustomerCheckInRecord saveCheckIn(CustomerCheckInRecord checkIn) {
            checkIns.put(checkIn.customerId() + ":" + checkIn.checkInDate(), checkIn);
            return checkIn;
        }

        @Override
        public java.util.Optional<CustomerCheckInRecord> findCheckIn(String customerId, LocalDate checkInDate) {
            return java.util.Optional.ofNullable(checkIns.get(customerId + ":" + checkInDate));
        }

        @Override
        public List<CustomerCheckInRecord> listCheckIns(String customerId) {
            return checkIns.values().stream().filter(c -> customerId.equals(c.customerId())).toList();
        }

        @Override
        public List<CustomerPointsLedgerRecord> listExpirableBatches(String customerId, Instant now) {
            return pointsLedger.stream()
                    .filter(l -> customerId.equals(l.customerId()))
                    .filter(l -> l.pointsDelta() > 0 && !l.expired() && l.expireAt() != null)
                    .filter(l -> !l.expireAt().isAfter(now))
                    .filter(l -> l.remainingPoints() != null && l.remainingPoints() > 0)
                    .sorted((a, b) -> a.expireAt().compareTo(b.expireAt()))
                    .toList();
        }

        @Override
        public List<CustomerPointsLedgerRecord> listOpenBatches(String customerId) {
            return pointsLedger.stream()
                    .filter(l -> customerId.equals(l.customerId()))
                    .filter(l -> l.pointsDelta() > 0 && !l.expired() && l.expireAt() != null)
                    .filter(l -> l.remainingPoints() != null && l.remainingPoints() > 0)
                    .sorted((a, b) -> a.expireAt().compareTo(b.expireAt()))
                    .toList();
        }

        @Override
        public void markBatchExpired(String ledgerId) {
            replace(ledgerId, l -> new CustomerPointsLedgerRecord(l.id(), l.customerId(), l.pointsDelta(),
                    l.balanceAfter(), l.sourceType(), l.sourceId(), l.description(), l.expireAt(), 0L, true, l.createdAt()));
        }

        @Override
        public void consumeBatchRemaining(String ledgerId, long remainingPoints) {
            replace(ledgerId, l -> new CustomerPointsLedgerRecord(l.id(), l.customerId(), l.pointsDelta(),
                    l.balanceAfter(), l.sourceType(), l.sourceId(), l.description(), l.expireAt(), remainingPoints,
                    l.expired(), l.createdAt()));
        }

        private void replace(String id, java.util.function.UnaryOperator<CustomerPointsLedgerRecord> op) {
            for (int i = 0; i < pointsLedger.size(); i++) {
                if (pointsLedger.get(i).id().equals(id)) {
                    pointsLedger.set(i, op.apply(pointsLedger.get(i)));
                    return;
                }
            }
        }
    }
}
