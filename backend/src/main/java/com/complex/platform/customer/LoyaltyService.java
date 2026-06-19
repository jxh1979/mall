package com.complex.platform.customer;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.gate.GuardedAction;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 会员忠诚度 第1部分（gap-analysis §7A，D12 定稿）：
 * <ul>
 *   <li>等级降级/保级宽限（P1）：周期末按保级进度判定 升级/保级/宽限/降级，事件 member.level_changed。</li>
 *   <li>积分过期清零 / 冻结 / 解冻（P1）：按批次 expire_at 到期清零 change_type=expire；账户级冻结/解冻。</li>
 *   <li>连续签到阶梯（P2）：每日签到 streak+1（断签重置），命中阶梯天数发奖励积分，事件 member.checked_in。</li>
 *   <li>生日加倍（P2）：获得积分时若命中生日窗口按倍率，叠加等级倍率。</li>
 * </ul>
 *
 * <p>等级评估/过期清零的批处理逻辑落在可直接调用的 {@link #evaluateLevels(Instant)} /
 * {@link #expirePoints(Instant)}（供单测直调），定时仅由 {@link LoyaltyScheduledJob} 薄包装。
 */
@Service
public class LoyaltyService {

    private static final long BASIS_POINTS_DENOMINATOR = 10_000L;

    private final CustomerRepository repository;
    private final AuditEventService auditEventService;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public LoyaltyService(CustomerRepository repository, AuditEventService auditEventService) {
        this(repository, auditEventService, Clock.systemUTC());
    }

    LoyaltyService(CustomerRepository repository, AuditEventService auditEventService, Clock clock) {
        this.repository = repository;
        this.auditEventService = auditEventService;
        this.clock = clock;
    }

    // ============================================================
    // 积分获得（消费/活动）—— 叠加等级倍率 + 生日加倍，写获取批次（带 expire_at）
    // ============================================================

    /**
     * 为会员发放积分。基础分按 等级倍率 × 生日倍率 计算最终分；validityDays>0 时写批次过期时间。
     * @return 实际发放积分。
     */
    @Transactional
    public long earnPoints(String customerId, long basePoints, String sourceType, String sourceId,
                           String description, Integer validityDays) {
        if (basePoints <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "base_points must be positive");
        }
        Instant now = now();
        CustomerRecord customer = requireCustomer(customerId);
        CustomerPointsAccountRecord account = loadOrCreateForUpdate(customerId, now);
        if ("frozen".equals(account.accountStatus())) {
            throw ApiException.conflict("POINTS_ACCOUNT_FROZEN", "points account is frozen");
        }
        long levelMultiplier = levelMultiplierBasisPoints(account.level());
        long birthdayMultiplier = birthdayMultiplierBasisPoints(customer, now);
        long awarded = applyMultiplier(applyMultiplier(basePoints, levelMultiplier), birthdayMultiplier);
        long balanceAfter = account.balance() + awarded;
        long keepProgress = account.keepThresholdProgress() + awarded;
        Instant expireAt = validityDays != null && validityDays > 0
                ? now.plusSeconds((long) validityDays * 86_400L)
                : null;
        repository.savePoints(withBalance(account, balanceAfter, keepProgress, now));
        repository.savePointsLedger(new CustomerPointsLedgerRecord(
                "ledger_earn_" + UUID.randomUUID().toString().replace("-", ""),
                customerId,
                awarded,
                balanceAfter,
                sourceType == null ? "earn" : sourceType,
                sourceId,
                description,
                expireAt,
                expireAt == null ? null : awarded,
                false,
                now));
        return awarded;
    }

    // ============================================================
    // 等级评估（升级/保级/宽限/降级）—— 周期末按保级进度判定
    // ============================================================

    /** 单账户等级评估，供单测/手动后台触发（过权限闸）。 */
    @Transactional
    @GuardedAction(capability = "", permission = "customer.loyalty.evaluate")
    public CustomerPointsAccountRecord evaluateLevel(String customerId, Instant now) {
        CustomerPointsAccountRecord account = repository.findPointsByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> ApiException.notFound("POINTS_ACCOUNT_NOT_FOUND", "points account not found"));
        return evaluateAccount(account, now);
    }

    /** 全量等级评估（定时作业逻辑本体，可单测直调；系统作业不过权限闸）。 */
    @Transactional
    public int evaluateLevels(Instant now) {
        int changed = 0;
        for (CustomerPointsAccountRecord account : repository.listAllPointsAccounts()) {
            CustomerPointsAccountRecord before = account;
            CustomerPointsAccountRecord after = evaluateAccount(account, now);
            if (!before.level().equals(after.level())) {
                changed++;
            }
        }
        return changed;
    }

    private CustomerPointsAccountRecord evaluateAccount(CustomerPointsAccountRecord account, Instant now) {
        List<CustomerMemberLevelRecord> levels = repository.listMemberLevels(null);
        if (levels.isEmpty()) {
            return account;
        }
        CustomerMemberLevelRecord current = levelFor(levels, account.level());
        // 1) 升级：保级进度达更高等级 upgrade_threshold 立即升级（不必等周期末）。
        CustomerMemberLevelRecord upgradeTarget = highestQualified(levels, account.keepThresholdProgress());
        if (upgradeTarget != null && upgradeTarget.levelRank() > rankOf(current)) {
            return applyLevelChange(account, upgradeTarget, "upgrade", now, true);
        }
        // 2) 周期未到：维持现状。
        Instant periodStart = account.levelPeriodStart() == null ? account.createdAt() : account.levelPeriodStart();
        if (current == null) {
            return account;
        }
        Instant periodEnd = periodStart.plusSeconds((long) current.keepPeriodDays() * 86_400L);
        if (now.isBefore(periodEnd)) {
            return account;
        }
        // 3) 周期末：达标→保级（重置周期/进度）。
        if (account.keepThresholdProgress() >= current.keepThreshold()) {
            return applyLevelChange(account, current, "keep", now, true);
        }
        // 4) 未达标：宽限期内→进入/维持宽限；超宽限→降级。
        Instant graceEnd = periodEnd.plusSeconds((long) current.graceDays() * 86_400L);
        if (now.isBefore(graceEnd)) {
            if (account.graceUntil() == null) {
                CustomerPointsAccountRecord graced = new CustomerPointsAccountRecord(
                        account.id(), account.customerId(), account.balance(), account.level(),
                        account.recentSummary(), account.frozenPoints(), account.accountStatus(),
                        account.levelPeriodStart(), account.keepThresholdProgress(), graceEnd,
                        account.lastCheckInDate(), account.checkInStreak(), account.createdAt(), now);
                repository.savePoints(graced);
                recordLevelChanged(account.customerId(), account.level(), account.level(), "grace");
                return graced;
            }
            return account;
        }
        CustomerMemberLevelRecord downgrade = nextLower(levels, current);
        return applyLevelChange(account, downgrade == null ? current : downgrade,
                downgrade == null ? "keep" : "downgrade", now, true);
    }

    private CustomerPointsAccountRecord applyLevelChange(CustomerPointsAccountRecord account,
            CustomerMemberLevelRecord target, String reason, Instant now, boolean resetPeriod) {
        String oldLevel = account.level();
        String newLevel = target.levelCode();
        CustomerPointsAccountRecord updated = new CustomerPointsAccountRecord(
                account.id(), account.customerId(), account.balance(), newLevel, account.recentSummary(),
                account.frozenPoints(), account.accountStatus(),
                resetPeriod ? now : account.levelPeriodStart(),
                resetPeriod ? 0L : account.keepThresholdProgress(),
                null,
                account.lastCheckInDate(), account.checkInStreak(), account.createdAt(), now);
        repository.savePoints(updated);
        recordLevelChanged(account.customerId(), oldLevel, newLevel, reason);
        return updated;
    }

    // ============================================================
    // 积分过期清零（FIFO 批次到期）—— change_type=expire
    // ============================================================

    /** 单账户过期清零。 */
    @Transactional
    public long expirePointsForCustomer(String customerId, Instant now) {
        CustomerPointsAccountRecord account = repository.findPointsByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> ApiException.notFound("POINTS_ACCOUNT_NOT_FOUND", "points account not found"));
        return expireAccount(account, now);
    }

    /** 全量过期清零（定时作业逻辑本体，可单测直调；系统作业不过权限闸）。 */
    @Transactional
    public long expirePoints(Instant now) {
        long total = 0;
        for (CustomerPointsAccountRecord account : repository.listAllPointsAccounts()) {
            CustomerPointsAccountRecord locked = repository.findPointsByCustomerIdForUpdate(account.customerId())
                    .orElse(account);
            total += expireAccount(locked, now);
        }
        return total;
    }

    private long expireAccount(CustomerPointsAccountRecord account, Instant now) {
        List<CustomerPointsLedgerRecord> batches = repository.listExpirableBatches(account.customerId(), now);
        long expiredTotal = 0;
        long balance = account.balance();
        for (CustomerPointsLedgerRecord batch : batches) {
            long remaining = batch.remainingPoints() == null ? 0 : batch.remainingPoints();
            if (remaining <= 0) {
                repository.markBatchExpired(batch.id());
                continue;
            }
            balance = Math.max(0, balance - remaining);
            repository.markBatchExpired(batch.id());
            repository.savePointsLedger(new CustomerPointsLedgerRecord(
                    "ledger_expire_" + UUID.randomUUID().toString().replace("-", ""),
                    account.customerId(),
                    -remaining,
                    balance,
                    "expire",
                    batch.id(),
                    "points expired",
                    null, null, false, now));
            expiredTotal += remaining;
        }
        if (expiredTotal > 0) {
            repository.savePoints(withBalance(account, balance, account.keepThresholdProgress(), now));
        }
        return expiredTotal;
    }

    // ============================================================
    // 冻结 / 解冻（账户级风控）
    // ============================================================

    @Transactional
    @GuardedAction(capability = "", permission = "customer.points.freeze")
    public CustomerPointsAccountRecord freeze(String customerId, String reason) {
        Instant now = now();
        CustomerPointsAccountRecord account = loadOrCreateForUpdate(customerId, now);
        if ("frozen".equals(account.accountStatus())) {
            throw ApiException.conflict("POINTS_ACCOUNT_ALREADY_FROZEN", "points account already frozen");
        }
        CustomerPointsAccountRecord frozen = new CustomerPointsAccountRecord(
                account.id(), account.customerId(), account.balance(), account.level(), account.recentSummary(),
                account.balance(), "frozen", account.levelPeriodStart(), account.keepThresholdProgress(),
                account.graceUntil(), account.lastCheckInDate(), account.checkInStreak(), account.createdAt(), now);
        repository.savePoints(frozen);
        auditEventService.recordSuccess("customer.points.frozen", "customer.points.freeze",
                "customer_points", customerId, null, null,
                Map.of("reason", reason == null ? "" : reason));
        return frozen;
    }

    @Transactional
    @GuardedAction(capability = "", permission = "customer.points.freeze")
    public CustomerPointsAccountRecord unfreeze(String customerId, String reason) {
        Instant now = now();
        CustomerPointsAccountRecord account = repository.findPointsByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> ApiException.notFound("POINTS_ACCOUNT_NOT_FOUND", "points account not found"));
        if (!"frozen".equals(account.accountStatus())) {
            throw ApiException.conflict("POINTS_ACCOUNT_NOT_FROZEN", "points account is not frozen");
        }
        CustomerPointsAccountRecord active = new CustomerPointsAccountRecord(
                account.id(), account.customerId(), account.balance(), account.level(), account.recentSummary(),
                0L, "active", account.levelPeriodStart(), account.keepThresholdProgress(),
                account.graceUntil(), account.lastCheckInDate(), account.checkInStreak(), account.createdAt(), now);
        repository.savePoints(active);
        auditEventService.recordSuccess("customer.points.unfrozen", "customer.points.freeze",
                "customer_points", customerId, null, null,
                Map.of("reason", reason == null ? "" : reason));
        return active;
    }

    // ============================================================
    // 连续签到阶梯
    // ============================================================

    /** 会员签到：streak+1（断签重置为 1），命中阶梯天数发奖励积分。重复签到当日抛冲突。 */
    @Transactional
    public CustomerCheckInRecord checkIn(String customerId, String sourceTerminal) {
        Instant now = now();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        requireCustomer(customerId);
        if (repository.findCheckIn(customerId, today).isPresent()) {
            throw ApiException.conflict("ALREADY_CHECKED_IN", "already checked in today");
        }
        CustomerPointsAccountRecord account = loadOrCreateForUpdate(customerId, now);
        int streak;
        LocalDate last = account.lastCheckInDate();
        if (last != null && last.plusDays(1).equals(today)) {
            streak = account.checkInStreak() + 1;
        } else {
            streak = 1;
        }
        long award = tierAwardForStreak(streak);
        CustomerCheckInRecord checkIn = repository.saveCheckIn(new CustomerCheckInRecord(
                "checkin_" + UUID.randomUUID().toString().replace("-", ""),
                customerId, today, streak, award,
                sourceTerminal == null ? "miniapp" : sourceTerminal, now));
        long balanceAfter = account.balance() + award;
        long keepProgress = account.keepThresholdProgress() + award;
        CustomerPointsAccountRecord updated = new CustomerPointsAccountRecord(
                account.id(), account.customerId(), balanceAfter, account.level(), account.recentSummary(),
                account.frozenPoints(), account.accountStatus(), account.levelPeriodStart(), keepProgress,
                account.graceUntil(), today, streak, account.createdAt(), now);
        repository.savePoints(updated);
        if (award > 0) {
            repository.savePointsLedger(new CustomerPointsLedgerRecord(
                    "ledger_checkin_" + checkIn.id(),
                    customerId, award, balanceAfter, "check_in", checkIn.id(),
                    "check-in streak " + streak, null, null, false, now));
        }
        auditEventService.recordSuccess("member.checked_in", "not_applicable", "customer_check_in",
                customerId, null, null, Map.of("streak", streak, "awarded_points", award));
        return checkIn;
    }

    // ============================================================
    // 辅助
    // ============================================================

    private long tierAwardForStreak(int streak) {
        long award = 0;
        for (CustomerCheckInTierRecord tier : repository.listCheckInTiers(null)) {
            if (tier.streakDays() <= streak && tier.streakDays() > 0) {
                // 取命中的最高阶梯（列表按 streak_days 升序）。
                award = tier.awardPoints();
            }
        }
        return award;
    }

    private long levelMultiplierBasisPoints(String levelCode) {
        for (CustomerMemberLevelRecord level : repository.listMemberLevels(null)) {
            if (level.levelCode().equals(levelCode)) {
                return level.pointMultiplierBasisPoints();
            }
        }
        return BASIS_POINTS_DENOMINATOR;
    }

    private long birthdayMultiplierBasisPoints(CustomerRecord customer, Instant now) {
        if (customer.birthDate() == null) {
            return BASIS_POINTS_DENOMINATOR;
        }
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        LocalDate birth = customer.birthDate();
        long best = BASIS_POINTS_DENOMINATOR;
        for (CustomerBirthdayRuleRecord rule : repository.listActiveBirthdayRules(null)) {
            boolean hit = switch (rule.windowType()) {
                case "birthday_day" -> birth.getMonthValue() == today.getMonthValue()
                        && birth.getDayOfMonth() == today.getDayOfMonth();
                case "birthday_month" -> birth.getMonthValue() == today.getMonthValue();
                default -> false;
            };
            if (hit) {
                best = Math.max(best, rule.multiplierBasisPoints());
            }
        }
        return best;
    }

    private long applyMultiplier(long points, long basisPoints) {
        return points * basisPoints / BASIS_POINTS_DENOMINATOR;
    }

    private CustomerMemberLevelRecord levelFor(List<CustomerMemberLevelRecord> levels, String levelCode) {
        return levels.stream().filter(l -> l.levelCode().equals(levelCode)).findFirst().orElse(null);
    }

    private CustomerMemberLevelRecord highestQualified(List<CustomerMemberLevelRecord> levels, long progress) {
        CustomerMemberLevelRecord best = null;
        for (CustomerMemberLevelRecord level : levels) {
            if (progress >= level.upgradeThreshold() && (best == null || level.levelRank() > best.levelRank())) {
                best = level;
            }
        }
        return best;
    }

    private CustomerMemberLevelRecord nextLower(List<CustomerMemberLevelRecord> levels, CustomerMemberLevelRecord current) {
        CustomerMemberLevelRecord best = null;
        for (CustomerMemberLevelRecord level : levels) {
            if (level.levelRank() < current.levelRank() && (best == null || level.levelRank() > best.levelRank())) {
                best = level;
            }
        }
        return best;
    }

    private int rankOf(CustomerMemberLevelRecord level) {
        return level == null ? -1 : level.levelRank();
    }

    private void recordLevelChanged(String customerId, String oldLevel, String newLevel, String reason) {
        auditEventService.recordSuccess("member.level_changed", "not_applicable", "customer_points",
                customerId, null, null,
                Map.of("old_level", oldLevel == null ? "" : oldLevel,
                        "new_level", newLevel == null ? "" : newLevel,
                        "reason", reason));
    }

    private CustomerPointsAccountRecord withBalance(CustomerPointsAccountRecord account, long balance,
            long keepProgress, Instant now) {
        return new CustomerPointsAccountRecord(
                account.id(), account.customerId(), balance, account.level(), account.recentSummary(),
                account.frozenPoints(), account.accountStatus(),
                account.levelPeriodStart() == null ? now : account.levelPeriodStart(),
                keepProgress, account.graceUntil(), account.lastCheckInDate(), account.checkInStreak(),
                account.createdAt(), now);
    }

    private CustomerPointsAccountRecord loadOrCreateForUpdate(String customerId, Instant now) {
        return repository.findPointsByCustomerIdForUpdate(customerId)
                .orElseGet(() -> repository.savePoints(CustomerService.newPointsAccount(customerId, now)));
    }

    private CustomerRecord requireCustomer(String customerId) {
        return repository.findCustomerById(customerId)
                .orElseThrow(() -> ApiException.notFound("CUSTOMER_NOT_FOUND", "customer not found"));
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
