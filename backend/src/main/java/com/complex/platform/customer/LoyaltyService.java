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

    private static final int EXPIRE_BATCH_SIZE = 200;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.context.ApplicationContext applicationContext;

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
            throw ApiException.badRequest("VALIDATION_FAILED", "基础积分必须大于 0");
        }
        Instant now = now();
        CustomerRecord customer = requireCustomer(customerId);
        CustomerPointsAccountRecord account = loadOrCreateForUpdate(customerId, now);
        if ("frozen".equals(account.accountStatus())) {
            throw ApiException.conflict("POINTS_ACCOUNT_FROZEN", "积分账户已被冻结");
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
        // 审计留痕（P2 缺口修复）：积分入账可追溯，与 confirmPoints/earnPointsPending 留痕口径一致。
        auditEventService.recordSuccess("points.earned", "not_applicable", "customer_points",
                customerId, null, sourceId,
                Map.of("source_type", sourceType == null ? "earn" : sourceType,
                        "source_id", sourceId == null ? "" : sourceId,
                        "points", awarded, "status", "earned"),
                "loyalty", "customer_points");
        return awarded;
    }

    // ============================================================
    // P0 清单#2 积分入账引擎：支付入 pending / 履约转 confirmed / 退款按比例 deduct
    //   R6-D06：支付成功入 pending、履约（核销/完成）转 confirmed、退款按已入账比例 deduct。
    //   幂等：按 (source_doc_type, source_doc_id, change_type) 唯一；重放不重复入账。
    //   口径：pending 不计入可用余额（不可花），confirm 时才加余额 + 保级进度；
    //         deduct 只冲减 confirmed（已入账）部分，复用账户行锁（findPointsByCustomerIdForUpdate）防并发超兑。
    // ============================================================

    /**
     * 支付成功：按业务单据写一条 pending 入账（change_type=earn,status=pending）。pending 不计入可用余额。
     * 按 (sourceDocType, sourceDocId, "earn") 幂等——重放命中已存在流水即原样返回，不重复入账。
     * 发事件 points.earned（category=loyalty, subject=customer_points）同事务留痕。
     *
     * @return 实际计算的待入账积分（命中幂等时返回既有流水的正向 delta）。
     */
    @Transactional
    public long earnPointsPending(String customerId, long basePoints, String sourceDocType, String sourceDocId,
                                  String description, Integer validityDays) {
        if (basePoints <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "基础积分必须大于 0");
        }
        requireSourceDoc(sourceDocType, sourceDocId);
        CustomerPointsLedgerRecord existing = repository
                .findPointsLedgerBySourceDoc(sourceDocType, sourceDocId, CustomerPointsLedgerRecord.CHANGE_EARN)
                .orElse(null);
        if (existing != null) {
            return Math.max(0, existing.pointsDelta());
        }
        Instant now = now();
        CustomerRecord customer = requireCustomer(customerId);
        CustomerPointsAccountRecord account = loadOrCreateForUpdate(customerId, now);
        if ("frozen".equals(account.accountStatus())) {
            throw ApiException.conflict("POINTS_ACCOUNT_FROZEN", "积分账户已被冻结");
        }
        long levelMultiplier = levelMultiplierBasisPoints(account.level());
        long birthdayMultiplier = birthdayMultiplierBasisPoints(customer, now);
        long awarded = applyMultiplier(applyMultiplier(basePoints, levelMultiplier), birthdayMultiplier);
        Instant expireAt = validityDays != null && validityDays > 0
                ? now.plusSeconds((long) validityDays * 86_400L)
                : null;
        // pending：余额不变（不可花），仅落账等待履约确认。balance_after 记录当前余额（未变）。
        repository.savePointsLedger(new CustomerPointsLedgerRecord(
                "ledger_earn_" + sourceDocId,
                customerId,
                awarded,
                account.balance(),
                "earn",
                sourceDocId,
                description,
                expireAt,
                expireAt == null ? null : awarded,
                false,
                now,
                CustomerPointsLedgerRecord.CHANGE_EARN,
                CustomerPointsLedgerRecord.STATUS_PENDING,
                sourceDocType,
                sourceDocId));
        auditEventService.recordSuccess("points.earned", "not_applicable", "customer_points",
                customerId, null, sourceDocId,
                Map.of("source_doc_type", sourceDocType, "source_doc_id", sourceDocId,
                        "points", awarded, "status", CustomerPointsLedgerRecord.STATUS_PENDING),
                "loyalty", "customer_points");
        return awarded;
    }

    /**
     * 履约（核销/完成）：把该订单 earn 的「已确认份额」对齐 target = floor(总额 × 已核销张数 / 订单总张数)，
     * 按差额双向增减可用余额 + 保级进度（核销→升、冲正→降，统一对齐 target，自然幂等）。
     * 多次/多人票部分核销按张数比例确认；单次票(1/1)即整单全额，与原口径一致。
     *
     * @param verifiedUnits 该订单已核销张数（Σ used_times）
     * @param totalUnits    该订单总张数（Σ total_times）；&lt;=0 时退化为全额（兼容无张数信息）
     * @return 本次确认份额变动（正=新增确认，负=冲正回滚，0=无变化幂等 noop）。
     */
    @Transactional
    public long confirmPoints(String sourceDocType, String sourceDocId, long verifiedUnits, long totalUnits) {
        requireSourceDoc(sourceDocType, sourceDocId);
        // AUD-014：确认/冲正前先 FOR UPDATE 锁定 earn 行（锁序 earn→account），串行化同一订单的并发确认，
        // 使后到事务读到已提交的 confirmed_amount → delta=0 幂等，杜绝基于同一旧值重复加余额。
        CustomerPointsLedgerRecord earn = repository
                .findPointsLedgerBySourceDocForUpdate(sourceDocType, sourceDocId, CustomerPointsLedgerRecord.CHANGE_EARN)
                .orElse(null);
        if (earn == null) {
            return 0; // 无会员订单从未入账：幂等 noop。
        }
        long pendingTotal = Math.max(0, earn.pointsDelta());
        if (pendingTotal == 0) {
            return 0;
        }
        // 目标已确认份额 = floor(总额 × 已核销张数 / 订单总张数)，clamp [0, 总额]。
        long target;
        if (totalUnits <= 0) {
            target = pendingTotal; // 退化：无张数信息按全额（单次票 / 兼容旧事件）。
        } else {
            long v = Math.max(0, Math.min(verifiedUnits, totalUnits));
            target = Math.floorDiv(pendingTotal * v, totalUnits);
        }
        // P0-B（V344）积分台账改追加：已确认份额不再读 earn 行被就地改写的 confirmed_amount，
        // 改读该单 confirm 分录的净额和（存量单由 V344 逐单补建等额分录，派生值与改造前严格相等）。
        long confirmed = repository.sumConfirmedPointsBySourceDoc(sourceDocType, sourceDocId);
        long delta = target - confirmed;
        if (delta == 0) {
            return 0; // 已对齐 target：幂等 noop（重复核销事件 / 无变化）。
        }
        Instant now = now();
        CustomerPointsAccountRecord account = loadOrCreateForUpdate(earn.customerId(), now);
        long balanceAfter = Math.max(0, account.balance() + delta);
        long appliedDelta = balanceAfter - account.balance(); // 余额下限 0：实际变动（已花光时不为负）。
        long keepProgress = Math.max(0, account.keepThresholdProgress() + appliedDelta);
        repository.savePoints(withBalance(account, balanceAfter, keepProgress, now));
        // ① 追加一条带符号的 confirm 分录（正=本次新确认、负=本次冲正回滚）——账本只增不改，每次变动都留痕。
        repository.savePointsLedger(new CustomerPointsLedgerRecord(
                "ledger_confirm_" + UUID.randomUUID().toString().replace("-", ""),
                earn.customerId(),
                delta,
                balanceAfter,
                CustomerPointsLedgerRecord.CHANGE_CONFIRM,
                earn.id(),
                "履约确认份额变动（原积分获取流水 " + earn.id() + "）",
                null,
                null,
                false,
                now,
                CustomerPointsLedgerRecord.CHANGE_CONFIRM,
                delta > 0
                        ? CustomerPointsLedgerRecord.STATUS_CONFIRMED
                        : CustomerPointsLedgerRecord.STATUS_REVERSED,
                sourceDocType,
                sourceDocId));
        // ② 同步 earn 行生命周期列（非金额事实列）：status：target>0→confirmed（计入余额）、
        //    target==0→pending（全部回滚未确认）；有有效期批次令 remaining_points 同步为 target
        //    （过期口径与实际入余额一致）。confirmed_amount 已被 DB 触发器冻结，不再回写。
        String status = target > 0
                ? CustomerPointsLedgerRecord.STATUS_CONFIRMED
                : CustomerPointsLedgerRecord.STATUS_PENDING;
        Long remaining = earn.expireAt() == null ? null : target;
        repository.updatePointsLedgerLifecycle(earn.id(), status, remaining);
        auditEventService.recordSuccess("points.earned", "not_applicable", "customer_points",
                earn.customerId(), null, sourceDocId,
                Map.of("source_doc_type", sourceDocType, "source_doc_id", sourceDocId,
                        "confirmed_amount", target, "verified_units", Math.max(0, verifiedUnits),
                        "total_units", Math.max(0, totalUnits), "delta", delta, "status", status),
                "loyalty", "customer_points");
        return delta;
    }

    /**
     * 兼容入口（无张数信息）：按全额确认（单次票 / 直接发放履约）。委托四参 set-to-target（target=全额）。
     *
     * @return 本次确认份额变动（首次=全额，已确认=0 幂等 noop）。
     */
    @Transactional
    public long confirmPoints(String sourceDocType, String sourceDocId) {
        return confirmPoints(sourceDocType, sourceDocId, 1, 1);
    }

    /**
     * 退款：按 refundRatio（退款额/原单额，basis points，10000=100%）冲减原单已 confirmed 的积分。
     * 只冲减「已入账（confirmed）」部分，pending（未履约）不冲减。复用账户行锁防并发超兑，余额不低于 0。
     * 幂等：按 (refundDocType, refundDocId, "deduct") 唯一；重放命中即返回既有冲减额。
     *
     * @param sourceDocType   原单据类型（earn 落账时的 source_doc_type，如 ticket_order）
     * @param sourceDocId     原单据 id
     * @param refundDocType   退款单据类型（幂等键，如 ticket_refund）
     * @param refundDocId     退款单据 id
     * @param refundRatioBp   退款比例（basis points，按已 confirmed 积分比例冲减；&gt;10000 截断为全额）
     * @return 本次实际冲减积分（命中幂等返回既有冲减额；无可冲减返回 0）。
     */
    @Transactional
    public long deductPoints(String sourceDocType, String sourceDocId,
                             String refundDocType, String refundDocId, long refundRatioBp) {
        requireSourceDoc(sourceDocType, sourceDocId);
        requireSourceDoc(refundDocType, refundDocId);
        if (refundRatioBp <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "退还比例必须大于 0");
        }
        CustomerPointsLedgerRecord existing = repository
                .findPointsLedgerBySourceDoc(refundDocType, refundDocId, CustomerPointsLedgerRecord.CHANGE_DEDUCT)
                .orElse(null);
        if (existing != null) {
            return Math.abs(existing.pointsDelta());
        }
        // 仅取原单已确认份额冲减；未履约/已冲正回零的份额不冲减。
        // F-TXN-03：按核销张数确认后，已入余额的是已确认份额（≤points_delta），故退款按已确认份额比例冲减。
        // P0-B（V344）：已确认份额改由 confirm 分录净额和派生（earn 行 confirmed_amount 已冻结，不再是权威口径）。
        long confirmedAwarded = Math.max(0, repository.sumConfirmedPointsBySourceDoc(sourceDocType, sourceDocId));
        if (confirmedAwarded <= 0) {
            return 0; // 未履约/未入账：无可冲减。
        }
        long ratio = Math.min(refundRatioBp, BASIS_POINTS_DENOMINATOR);
        long deduct = applyMultiplier(confirmedAwarded, ratio);
        if (deduct <= 0) {
            return 0;
        }
        Instant now = now();
        CustomerPointsAccountRecord account = loadOrCreateForUpdate(sourceDocCustomerId(sourceDocType, sourceDocId), now);
        long balanceAfter = Math.max(0, account.balance() - deduct);
        long actualDeduct = account.balance() - balanceAfter; // 余额不足时按可冲减额冲减（不为负）。
        repository.savePoints(withBalance(account, balanceAfter, account.keepThresholdProgress(), now));
        repository.savePointsLedger(new CustomerPointsLedgerRecord(
                "ledger_deduct_" + refundDocId,
                account.customerId(),
                -actualDeduct,
                balanceAfter,
                "deduct",
                refundDocId,
                "points deducted for refund " + refundDocId,
                null, null, false, now,
                CustomerPointsLedgerRecord.CHANGE_DEDUCT,
                CustomerPointsLedgerRecord.STATUS_REVERSED,
                refundDocType,
                refundDocId));
        auditEventService.recordSuccess("points.deducted", "not_applicable", "customer_points",
                account.customerId(), null, refundDocId,
                Map.of("source_doc_type", refundDocType, "source_doc_id", refundDocId,
                        "origin_doc_type", sourceDocType, "origin_doc_id", sourceDocId,
                        "points", actualDeduct, "refund_ratio_bp", ratio),
                "loyalty", "customer_points");
        return actualDeduct;
    }

    /** 取原单 earn 流水关联的会员 id（deduct 需对该会员账户加锁冲减）。 */
    private String sourceDocCustomerId(String sourceDocType, String sourceDocId) {
        return repository.listPointsLedgerBySourceDoc(sourceDocType, sourceDocId, CustomerPointsLedgerRecord.CHANGE_EARN)
                .stream().findFirst()
                .map(CustomerPointsLedgerRecord::customerId)
                .orElseThrow(() -> ApiException.notFound("POINTS_EARN_NOT_FOUND",
                        "该来源单据没有积分获取记录"));
    }

    private void requireSourceDoc(String sourceDocType, String sourceDocId) {
        if (sourceDocType == null || sourceDocType.isBlank() || sourceDocId == null || sourceDocId.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "必须填写来源单据类型与编号");
        }
    }

    // ============================================================
    // 等级评估（升级/保级/宽限/降级）—— 周期末按保级进度判定
    // ============================================================

    /** 单账户等级评估，供单测/手动后台触发（过权限闸）。 */
    @Transactional
    @GuardedAction(capability = "customer.loyalty.evaluate", permission = "customer.loyalty.evaluate")
    public CustomerPointsAccountRecord evaluateLevel(String customerId, Instant now) {
        CustomerPointsAccountRecord account = repository.findPointsByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> ApiException.notFound("POINTS_ACCOUNT_NOT_FOUND", "积分账户不存在或已被删除"));
        return evaluateAccount(account, now);
    }

    /** 全量等级评估（定时作业逻辑本体，可单测直调；系统作业不过权限闸）。
     *  F2（审计三补三）：改 keyset 分页逐账户小事务（对齐 3.14 expirePoints 范式，去全表大事务），
     *  等级配置表在循环外取一次（原实现每账户重查一次 = 1+N）。 */
    public int evaluateLevels(Instant now) {
        LoyaltyService self = applicationContext != null
                ? applicationContext.getBean(LoyaltyService.class) : this;
        List<CustomerMemberLevelRecord> levels = repository.listMemberLevels(null);
        if (levels.isEmpty()) {
            return 0;
        }
        int changed = 0;
        String cursor = null;
        List<String> page;
        do {
            page = repository.listPointsAccountCustomerIdsAfter(cursor, EXPIRE_BATCH_SIZE);
            for (String customerId : page) {
                if (self.evaluateLevelForCustomer(customerId, levels, now)) {
                    changed++;
                }
                cursor = customerId;
            }
        } while (page.size() == EXPIRE_BATCH_SIZE);
        return changed;
    }

    /** 单账户等级评估小事务（F2 批处理步进；行锁只覆盖单账户）。 */
    @Transactional
    public boolean evaluateLevelForCustomer(String customerId, List<CustomerMemberLevelRecord> levels, Instant now) {
        CustomerPointsAccountRecord account = repository.findPointsByCustomerIdForUpdate(customerId).orElse(null);
        if (account == null) {
            return false;
        }
        CustomerPointsAccountRecord after = evaluateAccount(account, levels, now);
        return !account.level().equals(after.level());
    }

    private CustomerPointsAccountRecord evaluateAccount(CustomerPointsAccountRecord account, Instant now) {
        return evaluateAccount(account, repository.listMemberLevels(null), now);
    }

    private CustomerPointsAccountRecord evaluateAccount(CustomerPointsAccountRecord account,
            List<CustomerMemberLevelRecord> levels, Instant now) {
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
                .orElseThrow(() -> ApiException.notFound("POINTS_ACCOUNT_NOT_FOUND", "积分账户不存在或已被删除"));
        return expireAccount(account, now);
    }

    /** 3.14 全量过期清零：keyset 分页逐账户小事务（每账户经代理走独立 @Transactional，
     *  避免一次性大事务长时间持锁全表）。系统作业不过权限闸。可单测直调（无 Spring 上下文时退化为直调）。 */
    public long expirePoints(Instant now) {
        LoyaltyService self = applicationContext != null
                ? applicationContext.getBean(LoyaltyService.class) : this;
        long total = 0;
        String cursor = null;
        List<String> page;
        do {
            page = repository.listPointsAccountCustomerIdsAfter(cursor, EXPIRE_BATCH_SIZE);
            for (String customerId : page) {
                total += self.expirePointsForCustomer(customerId, now);
                cursor = customerId;
            }
        } while (page.size() == EXPIRE_BATCH_SIZE);
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
    @GuardedAction(capability = "customer.points.freeze", permission = "customer.points.freeze")
    public CustomerPointsAccountRecord freeze(String customerId, String reason) {
        Instant now = now();
        CustomerPointsAccountRecord account = loadOrCreateForUpdate(customerId, now);
        if ("frozen".equals(account.accountStatus())) {
            throw ApiException.conflict("POINTS_ACCOUNT_ALREADY_FROZEN", "积分账户已是冻结状态");
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
    @GuardedAction(capability = "customer.points.freeze", permission = "customer.points.freeze")
    public CustomerPointsAccountRecord unfreeze(String customerId, String reason) {
        Instant now = now();
        CustomerPointsAccountRecord account = repository.findPointsByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> ApiException.notFound("POINTS_ACCOUNT_NOT_FOUND", "积分账户不存在或已被删除"));
        if (!"frozen".equals(account.accountStatus())) {
            throw ApiException.conflict("POINTS_ACCOUNT_NOT_FROZEN", "积分账户不在冻结状态");
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
            throw ApiException.conflict("ALREADY_CHECKED_IN", "今天已签到，请勿重复签到");
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

    // 积分转赠（会员间转移）已下线：HTML 明确「积分不支持会员间转赠」；服务/repo方法、记录类、V090转赠表已随 V172 清理移除。

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
                .orElseThrow(() -> ApiException.notFound("CUSTOMER_NOT_FOUND", "客人不存在或已被删除"));
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
