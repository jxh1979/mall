package com.complex.platform.customer;

// 文件版本：V0.05

import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.gate.GuardedAction;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 会员忠诚度 第1部分（gap-analysis §7A，D12 定稿）：
 * <ul>
 *   <li>会员等级（OBS-13-01，BSM L820）：按滚动统计周期内的成长值定级、达到门槛立即升级并起保护期，保护期结束前降级预警、
 *       「已保级」与降级补发通知（客人端站内弹出），成长值冲回即时重定，事件 member.level_changed。</li>
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

    @org.springframework.beans.factory.annotation.Autowired
    private PointsRecoveryService pointsRecoveries;

    /** 等级评估读成长值流水、等级参数版本与等级通知（OBS-13-01）。 */
    @org.springframework.beans.factory.annotation.Autowired
    private JdbcTemplate jdbc;

    @org.springframework.beans.factory.annotation.Autowired
    private TicketRewardConfigService configs;

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
        long recovery = pointsRecoveries == null ? 0 : pointsRecoveries.consume(customerId, awarded, account.balance(), sourceId, now);
        long balanceAfter = account.balance() + awarded - recovery;
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
                expireAt == null ? null : awarded - recovery,
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
        long recovery = delta > 0 && pointsRecoveries != null ? pointsRecoveries.consume(earn.customerId(), delta, account.balance(), sourceDocId, now) : 0;
        long balanceAfter = Math.max(0, account.balance() + delta - recovery);
        long appliedDelta = balanceAfter - account.balance(); // 余额下限 0：实际变动（已花光时不为负）。
        long keepProgress = Math.max(0, account.keepThresholdProgress() + delta);
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
        //    target==0→pending（全部回滚未确认）；有有效期批次仅累加本次净入余额，
        //    保留该批之前未花的剩余量，不让已抵欠或已消费积分再次到期扣减。首次确认前无已入账剩余量。
        //    confirmed_amount 已被 DB 触发器冻结，不再回写。
        String status = target > 0
                ? CustomerPointsLedgerRecord.STATUS_CONFIRMED
                : CustomerPointsLedgerRecord.STATUS_PENDING;
        long previousRemaining = confirmed > 0 && earn.remainingPoints() != null ? Math.max(0, earn.remainingPoints()) : 0;
        Long remaining = earn.expireAt() == null ? null : Math.max(0, previousRemaining + appliedDelta);
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
    // 等级评估（OBS-13-01；BSM L820「等级与成长值」；操作员裁决补充（第十四批）第 1、2 条）
    //   按滚动统计周期内的成长值（customer_growth_ledger，冲回跟原获取那笔走）定级，签到与消费积分不推动等级；
    //   达到门槛立即升级并起保护期；只有升级才起保护期，期末定级（保级或降级）后不再起新一轮，此后按滚动成长值定级直到下次升级；
    //   保护期结束前按提前通知天数发降级预警，预警后至期末重新达到原门槛发「已保级」、到期不降（之后再跌回也不降）；
    //   期末应降而未预警或实际降得比预警更低的照降并在生效后立即补发通知；成长值冲回后立即按当前滚动成长值重定，低于现级就降并通知。
    //   三项参数取最近一次定级时点（level_period_start：升级或降级导致等级变化时更新（含期末降级、保护期外按滚动成长值降级、冲回重定），期末保级不更新）生效的版本——只对后续周期生效，
    //   不在保护期的会员也不因改参当天整批降级，下次等级变化后才换用当时生效的新版本（第十四批第 5 条）。
    // ============================================================

    static final String NOTICE_WARNING = "downgrade_warning";
    static final String NOTICE_KEPT = "level_kept";
    static final String NOTICE_DOWNGRADED = "downgraded";

    /** 单账户等级评估，供单测/手动后台触发（过权限闸）。 */
    @Transactional
    @GuardedAction(capability = "customer.loyalty.evaluate", permission = "customer.loyalty.evaluate")
    public CustomerPointsAccountRecord evaluateLevel(String customerId, Instant now) {
        CustomerPointsAccountRecord account = repository.findPointsByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> ApiException.notFound("POINTS_ACCOUNT_NOT_FOUND", "积分账户不存在或已被删除"));
        return evaluateAccount(account, repository.listMemberLevels(), now, false);
    }

    /** 全量等级评估（每日作业逻辑本体，可单测直调；系统作业不过权限闸）。
     *  F2（审计三补三）：改 keyset 分页逐账户小事务（对齐 3.14 expirePoints 范式，去全表大事务），
     *  等级配置表在循环外取一次（原实现每账户重查一次 = 1+N）。 */
    public int evaluateLevels(Instant now) {
        LoyaltyService self = applicationContext != null
                ? applicationContext.getBean(LoyaltyService.class) : this;
        List<CustomerMemberLevelRecord> levels = repository.listMemberLevels();
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
        CustomerPointsAccountRecord after = evaluateAccount(account, levels, now, false);
        return !account.level().equals(after.level());
    }

    /** 成长值入账或冲回后在同一事务内即时重算（门票核销、商户消费完成即记成长值；退款、核销冲正冲回成长值）。 */
    void regrade(String customerId, Instant at, boolean reversal) {
        repository.findPointsByCustomerIdForUpdate(customerId)
                .ifPresent(account -> evaluateAccount(account, repository.listMemberLevels(), at, reversal));
    }

    private CustomerPointsAccountRecord evaluateAccount(CustomerPointsAccountRecord account,
            List<CustomerMemberLevelRecord> levels, Instant now, boolean reversal) {
        if (levels.isEmpty()) {
            return account;
        }
        String customerId = account.customerId();
        CustomerMemberLevelRecord current = levelFor(levels, account.level());
        Instant until = account.protectionUntil();
        Instant cycleStart = account.levelPeriodStart() == null ? now : account.levelPeriodStart();
        int rollingMonths = (int) configs.levelParam(TicketRewardConfigService.LEVEL_ROLLING_MONTHS, cycleStart);
        long rolling = rollingGrowth(customerId, now, rollingMonths);
        CustomerMemberLevelRecord qualified = highestQualified(levels, rolling);
        // 达到门槛立即升级（可跨级），升级时点起享有当时生效的保护期。
        if (rankOf(qualified) > rankOf(current)) {
            int months = (int) configs.levelParam(TicketRewardConfigService.LEVEL_PROTECTION_MONTHS, now);
            return applyLevelChange(account, qualified, "upgrade", now, months(now, months));
        }
        if (current == null) {
            return account;
        }
        boolean lower = rankOf(qualified) < rankOf(current);
        // 保护期外按滚动成长值定级；成长值冲回不受保护期限制。降级生效后立即通知。
        if (until == null || reversal) {
            if (!lower) {
                return account;
            }
            CustomerPointsAccountRecord changed = applyLevelChange(account, qualified, reversal ? "reversal" : "downgrade", now, null);
            notice(customerId, NOTICE_DOWNGRADED, null, account.level(), qualified.levelCode(), now);
            return changed;
        }
        boolean ended = !now.isBefore(until);
        // 期末一律按保护期结束当日、届时滚动周期内的成长值定级（作业晚跑也按当日算）。
        long growth = ended ? rollingGrowth(customerId, until, rollingMonths) : rolling;
        String warned = cycleNotice(customerId, until, NOTICE_WARNING);
        boolean kept = cycleNotice(customerId, until, NOTICE_KEPT) != null;
        if (warned != null && !kept && growth >= current.upgradeThreshold()) {
            notice(customerId, NOTICE_KEPT, until, account.level(), account.level(), now);
            kept = true;
        }
        if (!ended) {
            int noticeDays = (int) configs.levelParam(TicketRewardConfigService.LEVEL_NOTICE_DAYS, cycleStart);
            if (warned == null && lower && !now.isBefore(until.minus(noticeDays, ChronoUnit.DAYS))) {
                notice(customerId, NOTICE_WARNING, until, account.level(), qualified.levelCode(), now);
            }
            return account;
        }
        CustomerMemberLevelRecord target = kept ? current : highestQualified(levels, growth);
        if (rankOf(target) >= rankOf(current)) {
            return applyLevelChange(account, current, "keep", now, null);
        }
        CustomerPointsAccountRecord changed = applyLevelChange(account, target, "downgrade", now, null);
        // 期末晚处理（作业晚跑或期末后首笔成长值触发）时当前滚动成长值已高于期末定出的等级：达到门槛立即升级、起新一轮保护期，不发这次降级通知。
        if (rankOf(qualified) > rankOf(target)) {
            int months = (int) configs.levelParam(TicketRewardConfigService.LEVEL_PROTECTION_MONTHS, now);
            return applyLevelChange(changed, qualified, "upgrade", now, months(now, months));
        }
        if (warned == null || rankOf(target) < rankOf(levelFor(levels, warned))) {
            notice(customerId, NOTICE_DOWNGRADED, null, account.level(), target.levelCode(), now);
        }
        return changed;
    }

    private CustomerPointsAccountRecord applyLevelChange(CustomerPointsAccountRecord account,
            CustomerMemberLevelRecord target, String reason, Instant now, Instant protectionUntil) {
        // 期末保级不是等级变化，参数版本仍按原定级时点（第十四批第 5 条）。
        Instant periodStart = "keep".equals(reason) ? account.levelPeriodStart() : now;
        CustomerPointsAccountRecord updated = new CustomerPointsAccountRecord(
                account.id(), account.customerId(), account.balance(), target.levelCode(), account.recentSummary(),
                account.frozenPoints(), account.accountStatus(), periodStart, account.keepThresholdProgress(), protectionUntil,
                account.lastCheckInDate(), account.checkInStreak(), account.createdAt(), now);
        repository.savePoints(updated);
        recordLevelChanged(account.customerId(), account.level(), target.levelCode(), reason);
        return updated;
    }

    /** 滚动统计周期（at 往前 months 个月，北京时间口径）内获取的成长值净额：冲回跟原获取那笔走，按获取时点判是否在窗口内。 */
    private long rollingGrowth(String customerId, Instant at, int months) {
        Long sum = jdbc.queryForObject("""
                SELECT COALESCE(sum(g.growth_delta), 0) FROM customer_growth_ledger g
                  JOIN customer_growth_ledger e ON e.customer_id = g.customer_id AND e.change_type = 'earn'
                   AND COALESCE(e.reward_id, e.merchant_reward_id) = COALESCE(g.reward_id, g.merchant_reward_id)
                 WHERE g.customer_id = ? AND g.created_at <= ? AND e.created_at > ? AND e.created_at <= ?
                """, Long.class, customerId, Timestamp.from(at), Timestamp.from(months(at, -months)), Timestamp.from(at));
        return sum == null ? 0 : sum;
    }

    private static Instant months(Instant at, int months) {
        return at.atZone(LocalZone.ID).plusMonths(months).toInstant();
    }

    /** 本轮保护期内某类通知（预警、已保级各至多一次）的目标等级；没有返回 null。 */
    private String cycleNotice(String customerId, Instant cycleEnd, String type) {
        return jdbc.query("SELECT target_level FROM customer_level_notices WHERE customer_id = ? AND cycle_end = ? AND notice_type = ?",
                (rs, n) -> rs.getString(1), customerId, Timestamp.from(cycleEnd), type).stream().findFirst().orElse(null);
    }

    private void notice(String customerId, String type, Instant cycleEnd, String currentLevel, String targetLevel, Instant at) {
        jdbc.update("INSERT INTO customer_level_notices(id, customer_id, notice_type, cycle_end, current_level, target_level, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                "lvn_" + UUID.randomUUID().toString().replace("-", ""), customerId, type, cycleEnd == null ? null : Timestamp.from(cycleEnd),
                currentLevel, targetLevel, Timestamp.from(at));
    }

    /** 客人端弹出的本人等级通知（BSM L820 降级预警、「已保级」、降级补发；等级显示配置的中文名）。 */
    public record LevelNotice(String id, String noticeType, String currentLevel, String currentLevelName, String targetLevel,
            String targetLevelName, Instant cycleEnd, Instant createdAt) {}

    /** 本人未读等级通知，新到旧。 */
    public List<LevelNotice> listLevelNotices(String customerId) {
        Map<String, String> names = new java.util.HashMap<>();
        for (CustomerMemberLevelRecord level : repository.listMemberLevels()) {
            names.putIfAbsent(level.levelCode(), level.levelName());
        }
        return jdbc.query("SELECT * FROM customer_level_notices WHERE customer_id = ? AND read_at IS NULL ORDER BY created_at DESC, id",
                (rs, n) -> new LevelNotice(rs.getString("id"), rs.getString("notice_type"),
                        rs.getString("current_level"), names.getOrDefault(rs.getString("current_level"), rs.getString("current_level")),
                        rs.getString("target_level"), names.getOrDefault(rs.getString("target_level"), rs.getString("target_level")),
                        TicketRewardConfigService.instant(rs, "cycle_end"), rs.getTimestamp("created_at").toInstant()), customerId);
    }

    /** 客人点「知道了」：只改本人的通知，他人或不存在 404；重复调用不改首次已读时间。 */
    @Transactional
    public void markLevelNoticeRead(String customerId, String noticeId) {
        if (jdbc.update("UPDATE customer_level_notices SET read_at = COALESCE(read_at, now()) WHERE id = ? AND customer_id = ?", noticeId, customerId) == 0) {
            throw ApiException.notFound("LEVEL_NOTICE_NOT_FOUND", "等级通知不存在");
        }
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

    /**
     * ROOT-03-R21（BSM 第 863 行「注销后……积分、券一律清零」）：注销时可用积分与冻结积分清零，追加一条 adjust 明细（账本只增不改），
     * 各有效期批次剩余量置零（之后的到期作业不再扣）。不退任何东西。返回清掉的可用积分。
     */
    @Transactional
    public long clearOnDeregistration(String customerId, Instant now) {
        CustomerPointsAccountRecord account = repository.findPointsByCustomerIdForUpdate(customerId).orElse(null);
        if (account == null) {
            return 0;
        }
        long cleared = account.balance();
        for (CustomerPointsLedgerRecord batch : repository.listOpenBatches(customerId)) {
            repository.markBatchExpired(batch.id());
        }
        if (cleared > 0) {
            repository.savePointsLedger(new CustomerPointsLedgerRecord(
                    "ledger_dereg_" + UUID.randomUUID().toString().replace("-", ""),
                    customerId, -cleared, 0, "adjust", null, "账户注销清零", null, null, false, now));
        }
        if (cleared > 0 || account.frozenPoints() > 0) {
            repository.savePoints(new CustomerPointsAccountRecord(account.id(), account.customerId(), 0, account.level(),
                    account.recentSummary(), 0, account.accountStatus(), account.levelPeriodStart(),
                    account.keepThresholdProgress(), account.protectionUntil(), account.lastCheckInDate(), account.checkInStreak(),
                    account.createdAt(), now));
        }
        return cleared;
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
                account.protectionUntil(), account.lastCheckInDate(), account.checkInStreak(), account.createdAt(), now);
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
                account.protectionUntil(), account.lastCheckInDate(), account.checkInStreak(), account.createdAt(), now);
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
        // 签到「今天」按所在地(北京)日历日:会员北京 0 点后签到,UTC 口径算作昨天 → 判"今天已签到"直接拒绝。
        LocalDate today = LocalDate.ofInstant(now, LocalZone.ID);
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
        long recovery = pointsRecoveries == null ? 0 : pointsRecoveries.consume(customerId, award, account.balance(), "check_in", now);
        long balanceAfter = account.balance() + award - recovery;
        long keepProgress = account.keepThresholdProgress() + award;
        CustomerPointsAccountRecord updated = new CustomerPointsAccountRecord(
                account.id(), account.customerId(), balanceAfter, account.level(), account.recentSummary(),
                account.frozenPoints(), account.accountStatus(), account.levelPeriodStart(), keepProgress,
                account.protectionUntil(), today, streak, account.createdAt(), now);
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

    /** 包内复用：商户消费奖励（MerchantConsumptionRewardService）按同一等级倍率计算。 */
    long levelMultiplierBasisPoints(String levelCode) {
        for (CustomerMemberLevelRecord level : repository.listMemberLevels()) {
            if (level.levelCode().equals(levelCode)) {
                return level.pointMultiplierBasisPoints();
            }
        }
        return BASIS_POINTS_DENOMINATOR;
    }

    long birthdayMultiplierBasisPoints(CustomerRecord customer, Instant now) {
        if (customer.birthDate() == null) {
            return BASIS_POINTS_DENOMINATOR;
        }
        // 生日窗口「今天」同按所在地(北京)日历日:会员北京生日当天 0~8 点消费,UTC 口径还算前一天、拿不到生日倍率。
        LocalDate today = LocalDate.ofInstant(now, LocalZone.ID);
        LocalDate birth = customer.birthDate();
        long best = BASIS_POINTS_DENOMINATOR;
        for (CustomerBirthdayRuleRecord rule : repository.listActiveBirthdayRules()) {
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
                keepProgress, account.protectionUntil(), account.lastCheckInDate(), account.checkInStreak(),
                account.createdAt(), now);
    }

    CustomerPointsAccountRecord loadOrCreateForUpdate(String customerId, Instant now) {
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
