package com.complex.platform.customer;

// 文件版本：V0.02

import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十一批 TK-04-a：商户消费（酒吧账单、专项服务单）的会员识别与整笔履约完成后的待确认积分、成长值入账。
 * <ul>
 *   <li>会员识别（BSM L822「由收银在收款时现场录入会员号累计……走客人查询专门通道并留痕」；PGM L878）：会员号＝注册手机号，
 *       查了谁、谁查的独立留痕；查无会员即拒绝，不代建会员。</li>
 *   <li>完成快照（BSM L823「按整笔有效消费全部实际履约完成的时点统一起算等待期」「仅完成支付都不视为普通商品服务履约完成」；
 *       台账 §31 Q38A）：酒吧账单须已结清且所覆盖订单的非撤单明细全部送达，完成时点取最后一项送达时点；专项服务单取服务完成时点。
 *       有效消费＝账单应付（券抵扣、减免已扣出，小费不在其内）－本单退品留款＋本单抵用的留款（BSM L1064、DEC-MPS-2026-07-28-07）。</li>
 *   <li>规则（BSM L822「积分规则与成长值规则分别配置，任一结果不得从另一结果推导」；DEC-R8-93；2026-10-21 操作员裁决补充第 4 条）：
 *       积分与成长值各按商户业态已生效规则计算，缺积分规则或全国商户等待期不发积分，缺成长值规则不记成长值，互不回落；
 *       规则版本与生日倍率按支付成功时点锁定（BSM L831、DEC-MPS-2026-07-28-09）。</li>
 *   <li>积分记待确认，成长值完成即记。</li>
 *   <li>TK-04-b（BSM L823；ENUM L72、L75、L77；台账 §31 Q38B）：等待期届满且无待处理的对应退款才转为可用并形成商户积分发行费；
 *       退款成功部分按原锁定规则从有效消费中排除（剩余仍按原完成时点），已可用后退款从原获分会员扣回、不足挂待追回；
 *       客人端与账单端查看本单奖励结果。服务点单无退款（第十批 TK-22 裁决），只有酒吧账单对账。</li>
 * </ul>
 */
@Service
public class MerchantConsumptionRewardService {
    public static final String BAR_BILL = "bar_bill";
    public static final String SERVICE_ORDER = "service_order";
    private final JdbcTemplate jdbc;
    private final CustomerRepository customers;
    private final TicketRewardConfigService configs;
    private final LoyaltyService loyalty;
    private final AuditEventService audit;
    private final PointsRecoveryService recoveries;

    public MerchantConsumptionRewardService(JdbcTemplate jdbc, CustomerRepository customers, TicketRewardConfigService configs,
            LoyaltyService loyalty, AuditEventService audit, PointsRecoveryService recoveries) {
        this.jdbc = jdbc; this.customers = customers; this.configs = configs; this.loyalty = loyalty; this.audit = audit; this.recoveries = recoveries;
    }

    /** 收银录入会员手机号识别本单会员；查询留痕独立落库（本次收款失败回滚也留住）。 */
    public String identifyByPhone(String sourceType, String sourceId, String phone, String permissionCode, String idempotencyKey) {
        String number = phone.trim();
        Optional<CustomerRecord> found = customers.findCustomerByPhone(number);
        Map<String, Object> lookup = new LinkedHashMap<>();
        lookup.put("purpose", sourceType + "_member");
        lookup.put("source_id", sourceId);
        lookup.put("phone_hash", sha256(number));
        lookup.put("matched", found.isPresent());
        audit.recordSuccessIndependently("customer.account.viewed", permissionCode, "customer",
                found.map(CustomerRecord::id).orElse("not_found"), null, idempotencyKey, lookup);
        String customerId = found.orElseThrow(() -> ApiException.badRequest("MEMBER_NOT_FOUND",
                "该手机号尚未注册会员，请核对；客人不累计积分时会员手机号可不填")).id();
        bind(sourceType, sourceId, customerId, true);
        return customerId;
    }

    /** 储值支付短信验证通过的付款客人：本单尚未录入会员手机号时作为本单会员。 */
    public void bindIfAbsent(String sourceType, String sourceId, String customerId) {
        bind(sourceType, sourceId, customerId, false);
    }

    /** 出品全部送达：该订单下已结清账单逐张尝试生成。 */
    public void tryCreateForBarOrder(String orderId) {
        for (String billId : jdbc.queryForList("SELECT id FROM bar_bills WHERE order_id=? AND settlement_status='settled' ORDER BY id FOR UPDATE",
                String.class, orderId)) {
            tryCreateForBarBill(billId);
        }
    }

    /** 酒吧账单结清或最后一项送达时调用；锁账单行，结账与送达并发时由后提交的一方生成。 */
    public void tryCreateForBarBill(String billId) {
        jdbc.query("""
            SELECT b.merchant_id, m.merchant_type, m.project_id, o.paid_at, COALESCE(mm.customer_id, o.customer_id) AS member,
              """ + BAR_EFFECTIVE + """
               AS effective,
              (SELECT CASE WHEN bool_and(i.delivery_status = 'delivered' AND i.delivered_at IS NOT NULL) THEN max(i.delivered_at) END
                 FROM bar_order_items i WHERE i.order_id = b.order_id AND NOT i.voided AND i.delivery_status <> 'voided') AS completed_at
            FROM bar_bills b JOIN bar_orders o ON o.id = b.order_id JOIN merchants m ON m.id = b.merchant_id
            LEFT JOIN merchant_consumption_members mm ON mm.source_type = 'bar_bill' AND mm.source_id = b.id
            WHERE b.id = ? AND b.settlement_status = 'settled' FOR UPDATE OF b
            """, (ResultSet rs) -> { create(BAR_BILL, billId, fact(rs)); }, billId);
    }

    /** 专项服务单完成时调用：须收款时已识别会员（服务单本身不带客人）。 */
    public void tryCreateForServiceOrder(String serviceOrderId) {
        jdbc.query("""
            SELECT s.bar_id AS merchant_id, m.merchant_type, COALESCE(s.project_id, m.project_id) AS project_id, s.paid_at,
                   mm.customer_id AS member, s.price_cent AS effective, s.completed_at
            FROM service_orders s JOIN merchants m ON m.id = s.bar_id
            JOIN merchant_consumption_members mm ON mm.source_type = 'service_order' AND mm.source_id = s.id
            WHERE s.id = ? AND s.status = 'completed'
            """, (ResultSet rs) -> { create(SERVICE_ORDER, serviceOrderId, fact(rs)); }, serviceOrderId);
    }

    // ── 第十一批 TK-04-b：等待期届满释放、对应退款门槛与冲回、商户积分发行费；客人端与账单端查看 ─────────────

    /** 客人端与账单端看到的奖励结果（BSM L877 客人端只展示结果；ENUM L77 从消费单据可反查）：积分、成长值为扣除已退部分后的现值，不含客人身份。 */
    public record RewardView(String id, String sourceType, String sourceId, String sourceNo, String merchantName, long points, long growth,
            String status, Instant completedAt, Instant availableAt, Instant confirmedAt) {}

    public List<RewardView> listForCustomer(String customerId) {
        return views("r.customer_id = ?", customerId);
    }

    public List<RewardView> listForSource(String sourceType, String sourceId) {
        return views("r.source_type = ? AND r.source_id = ?", sourceType, sourceId);
    }

    /** 定时释放入口：等待期已到的待确认奖励。 */
    public List<String> due(Instant at) {
        return jdbc.queryForList("SELECT id FROM merchant_consumption_rewards WHERE status = 'pending' AND available_at <= ? ORDER BY available_at, id",
                String.class, ts(at));
    }

    /**
     * 等待期已满且无待处理的对应退款时转为可用（BSM L823）：先按已成功退款取消对应待确认积分（剩余仍按原完成时点，不重新起算），
     * 再抵扣待追回积分、入余额，并按转为可用的积分数×释放时全国发行单价形成商户积分发行费（ENUM L75；成本由消费商户承担，ENUM L72）。
     */
    @Transactional
    public boolean release(String id) {
        Instant at = Instant.now();
        LocalDate feeMonth = at.atZone(LocalZone.ID).toLocalDate().withDayOfMonth(1);
        // 与积分月结同一锁：跨月并发时旧月已关闭就保留待确认，由下一轮按真实确认时间重试（同门票积分）。
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))", rs -> {}, "points-month:" + feeMonth);
        if (jdbc.queryForObject("SELECT count(*) FROM ticket_points_closed_months WHERE billing_month = ?", Integer.class, feeMonth) > 0) return false;
        Reward r = locked(id);
        if (!"pending".equals(r.status()) || at.isBefore(r.availableAt())) return false;
        if (BAR_BILL.equals(r.sourceType())) {
            if (barRefundPending(r.sourceId())) return false;
            r = reconcile(r, at);
            if (!"pending".equals(r.status())) return false;
        }
        CustomerPointsAccountRecord a = loyalty.loadOrCreateForUpdate(r.customerId(), at);
        if ("frozen".equals(a.accountStatus())) return false;
        if (!customers.findCustomerById(r.customerId()).map(c -> "active".equals(c.status())).orElse(false)) return false;
        long gross = r.points() - r.reversedPoints();
        long offset = recoveries.consume(r.customerId(), gross, a.balance(), id, at);
        long net = gross - offset;
        long balance = Math.addExact(a.balance(), net);
        customers.savePoints(account(a, balance, a.keepThresholdProgress(), at));
        customers.savePointsLedger(new CustomerPointsLedgerRecord("ledger_confirm_" + id, r.customerId(), gross, balance, "confirm", id,
                "商户消费积分等待期届满，待追回抵扣：" + offset, null, null, false, at, "confirm", "confirmed", r.sourceType(), r.sourceId()));
        customers.updatePointsLedgerLifecycle("ledger_earn_" + id, "confirmed", null);
        jdbc.update("UPDATE merchant_consumption_rewards SET status = 'confirmed', confirmed_at = ?, confirmed_points = ? WHERE id = ?", ts(at), net, id);
        String priceVersion = null;
        if (net > 0) {
            TicketRewardConfig price = configs.effective("national", null, null, at);
            priceVersion = price.id();
            long fee = price.values().issuancePriceCent().multiply(BigDecimal.valueOf(net)).setScale(0, RoundingMode.HALF_UP).longValueExact();
            int count = jdbc.update("""
                INSERT INTO merchant_points_issuance_fees(reward_id, price_version_id, seller_entity_id, seller_merchant_id, receiver_entity_id,
                  project_id, points, unit_price_cent, amount_cent, confirmed_at)
                SELECT r.id, ?, e.entity_id, r.merchant_id, ?, r.project_id, ?, ?, ?, ?
                FROM merchant_consumption_rewards r JOIN merchants m ON m.id = r.merchant_id
                CROSS JOIN LATERAL (SELECT COALESCE((SELECT b.contract_entity_id FROM business_entity_bindings b WHERE b.merchant_id = m.id
                    AND b.status = 'active' AND b.effective_from <= CURRENT_DATE AND (b.effective_to IS NULL OR b.effective_to >= CURRENT_DATE)
                    ORDER BY b.created_at DESC LIMIT 1), m.contract_entity_id) AS entity_id) e
                WHERE r.id = ? AND e.entity_id IS NOT NULL AND r.project_id IS NOT NULL
                """, price.id(), price.values().receiverEntityId(), net, price.values().issuancePriceCent(), fee, ts(at), id);
            if (count != 1) throw ApiException.conflict("MERCHANT_FEE_PAYER_REQUIRED", "消费商户缺少有效合同主体或所属项目，不可确认商户积分发行费");
        }
        audit.recordSuccess("merchant.reward.confirmed", "not_applicable", "merchant_consumption_reward", id, null, id,
                Map.of("points", net, "recovery_offset", offset, "price_version_id", priceVersion == null ? "" : priceVersion));
        return true;
    }

    /**
     * 酒吧账单退款状态变化后对账（退款调整生效、退品审批、退款应付实付结清时调用）：仍有待审或未退出的退款时不动；
     * 已成功退出的部分从有效消费中排除，按原锁定规则与倍率重算——待确认的取消对应积分，已可用的从原获分会员扣回、不足挂待追回，
     * 成长值同口径冲回（BSM L823「已取消或已退款部分按原规则从有效消费中排除」「已可用积分后来发生特殊退款的，仍按原追回及待追回积分规则处理」）。
     */
    @Transactional
    public void reconcileBarBill(String billId) {
        for (String id : jdbc.queryForList("SELECT id FROM merchant_consumption_rewards WHERE source_type = 'bar_bill' AND source_id = ? "
                + "AND status IN ('pending', 'confirmed') FOR UPDATE", String.class, billId)) {
            if (barRefundPending(billId)) return;
            reconcile(locked(id), Instant.now());
        }
    }

    /** 退款应付实付结清（财务统一回写点）：来源为酒吧账单退款调整的，对该账单对账。 */
    @EventListener
    @Transactional
    public void refundPayableSettled(com.complex.platform.finance.RefundPayableSettled event) {
        jdbc.queryForList("SELECT bill_id FROM bar_bill_adjustments WHERE id = ?", String.class, event.sourceId()).forEach(this::reconcileBarBill);
    }

    /** 酒吧账单有效消费（停车消费抵免 HP-P01 同口径复用）。 */
    public static final String BAR_EFFECTIVE = """
            b.payable_amount_cent + COALESCE((SELECT sum(CASE a.adjustment_type WHEN 'min_spend_retained' THEN -a.amount_cent ELSE a.amount_cent END)
                FROM bar_bill_adjustments a WHERE a.bill_id = b.id AND a.status = 'applied'
                 AND a.adjustment_type IN ('min_spend_retained', 'min_spend_retained_applied')), 0)""";

    private record Reward(String id, String sourceType, String sourceId, String customerId, long effective, String pointsRuleId,
            String growthRuleId, long levelBp, long birthdayBp, long points, long growth, Instant completedAt, Instant availableAt,
            String status, long refunded, long reversedPoints, long reversedGrowth) {}

    private Reward locked(String id) {
        return jdbc.query("SELECT * FROM merchant_consumption_rewards WHERE id = ? FOR UPDATE", (rs, n) -> new Reward(rs.getString("id"),
                rs.getString("source_type"), rs.getString("source_id"), rs.getString("customer_id"), rs.getLong("effective_amount_cent"),
                rs.getString("points_rule_id"), rs.getString("growth_rule_id"), rs.getLong("level_multiplier_bp"), rs.getLong("birthday_multiplier_bp"),
                rs.getLong("points"), rs.getLong("growth"), TicketRewardConfigService.instant(rs, "completed_at"),
                TicketRewardConfigService.instant(rs, "available_at"), rs.getString("status"), rs.getLong("refunded_amount_cent"),
                rs.getLong("reversed_points"), rs.getLong("reversed_growth")), id).getFirst();
    }

    /** 退品待审核，或退款已生效但应付尚未实际退出（含失败仍需办理）；驳回的退品不算。 */
    private boolean barRefundPending(String billId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS (SELECT 1 FROM bar_order_item_voids v WHERE v.bill_id = ? AND v.status = 'pending')
                OR EXISTS (SELECT 1 FROM bar_bill_adjustments a JOIN ar_ap_items x ON x.id = a.ar_ap_item_id
                            WHERE a.bill_id = ? AND a.adjustment_type = 'refund' AND a.status = 'applied' AND x.status <> 'settled'
                              AND NOT EXISTS (SELECT 1 FROM bar_order_item_voids v WHERE v.bill_adjustment_id = a.id AND v.status = 'rejected'))
            """, Boolean.class, billId, billId));
    }

    private Reward reconcile(Reward r, Instant at) {
        long current = jdbc.queryForObject("SELECT " + BAR_EFFECTIVE + " FROM bar_bills b WHERE b.id = ?", Long.class, r.sourceId());
        long refunded = Math.max(0, r.effective() - current);
        if (refunded <= r.refunded()) return r;
        long remaining = r.effective() - refunded;
        // 按生成时锁定的规则版本与倍率重算（不按退款时的新规则），与生成时同一取整口径。
        long points = r.pointsRuleId() == null ? 0
                : remaining / configs.find(r.pointsRuleId()).values().fixedValue() * r.levelBp() / 10_000 * r.birthdayBp() / 10_000;
        long growth = r.growthRuleId() == null ? 0 : remaining / configs.find(r.growthRuleId()).values().fixedValue();
        long pointsBack = r.points() - r.reversedPoints() - points;
        long growthBack = r.growth() - r.reversedGrowth() - growth;
        CustomerPointsAccountRecord a = loyalty.loadOrCreateForUpdate(r.customerId(), at);
        boolean confirmed = "confirmed".equals(r.status());
        long debit = confirmed ? Math.min(a.balance(), pointsBack) : 0;
        long debt = confirmed ? pointsBack - debit : 0;
        boolean deregistered = debt > 0 && customers.findCustomerById(r.customerId()).map(c -> "deregistered".equals(c.status())).orElse(false);
        long periodGrowth = a.levelPeriodStart() != null && r.completedAt().isBefore(a.levelPeriodStart()) ? 0 : growthBack;
        customers.savePoints(account(a, a.balance() - debit, Math.max(0, a.keepThresholdProgress() - periodGrowth), at));
        String step = r.id() + "_" + refunded;
        if (confirmed && pointsBack > 0) {
            customers.savePointsLedger(new CustomerPointsLedgerRecord("ledger_deduct_" + step, r.customerId(), -debit, a.balance() - debit, "deduct",
                    r.id(), deregistered ? "商户消费退款冲回；会员已注销，取消追偿：" + debt : "商户消费退款冲回；待追回：" + debt, null, null, false, at,
                    "deduct", "reversed", "merchant_reward_refund", step));
        }
        if (debt > 0) {
            jdbc.update("""
                INSERT INTO customer_points_recoveries(merchant_reward_id, customer_id, original_points, remaining_points, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (merchant_reward_id) DO UPDATE SET original_points = customer_points_recoveries.original_points + EXCLUDED.original_points,
                  remaining_points = customer_points_recoveries.remaining_points + EXCLUDED.remaining_points,
                  status = CASE WHEN EXCLUDED.remaining_points > 0 THEN 'pending' ELSE customer_points_recoveries.status END, updated_at = EXCLUDED.updated_at
                """, r.id(), r.customerId(), debt, deregistered ? 0 : debt, deregistered ? "cancelled" : "pending", ts(at), ts(at));
        }
        if (growthBack > 0) {
            jdbc.update("INSERT INTO customer_growth_ledger(id, customer_id, merchant_reward_id, change_type, growth_delta, created_at) VALUES (?, ?, ?, 'reverse', ?, ?)",
                    "growth_reverse_" + step, r.customerId(), r.id(), -growthBack, ts(at));
        }
        boolean pointsGone = r.points() > 0 && points == 0;
        String status = !pointsGone ? r.status() : confirmed ? "reversed" : "cancelled";
        if (pointsGone) customers.updatePointsLedgerLifecycle("ledger_earn_" + r.id(), "reversed", null);
        jdbc.update("UPDATE merchant_consumption_rewards SET refunded_amount_cent = ?, reversed_points = reversed_points + ?, "
                + "reversed_growth = reversed_growth + ?, status = ? WHERE id = ?", refunded, pointsBack, growthBack, status, r.id());
        audit.recordSuccess("merchant.reward.refund_reversed", "not_applicable", "merchant_consumption_reward", r.id(), null, step,
                Map.of("refunded_amount_cent", refunded, "points_reversed", pointsBack, "recovered_points", debit,
                        "pending_recovery", deregistered ? 0 : debt, "growth_reversed", growthBack));
        return locked(r.id());
    }

    private List<RewardView> views(String where, Object... args) {
        return jdbc.query("""
            SELECT r.*, m.merchant_name, COALESCE(b.bill_no, r.source_id) AS source_no
            FROM merchant_consumption_rewards r JOIN merchants m ON m.id = r.merchant_id
            LEFT JOIN bar_bills b ON r.source_type = 'bar_bill' AND b.id = r.source_id
            WHERE""" + " " + where + " ORDER BY r.completed_at DESC, r.id", (rs, n) -> new RewardView(rs.getString("id"), rs.getString("source_type"),
                rs.getString("source_id"), rs.getString("source_no"), rs.getString("merchant_name"),
                rs.getLong("points") - rs.getLong("reversed_points"), rs.getLong("growth") - rs.getLong("reversed_growth"), rs.getString("status"),
                TicketRewardConfigService.instant(rs, "completed_at"), TicketRewardConfigService.instant(rs, "available_at"),
                TicketRewardConfigService.instant(rs, "confirmed_at")), args);
    }

    private static CustomerPointsAccountRecord account(CustomerPointsAccountRecord a, long balance, long growth, Instant at) {
        return new CustomerPointsAccountRecord(a.id(), a.customerId(), balance, a.level(), a.recentSummary(), a.frozenPoints(), a.accountStatus(),
                a.levelPeriodStart(), growth, a.graceUntil(), a.lastCheckInDate(), a.checkInStreak(), a.createdAt(), at);
    }

    private record Fact(String merchantId, String format, String projectId, Instant paidAt, String member,
            long effective, Instant completedAt) {}

    private static Fact fact(ResultSet rs) throws SQLException {
        return new Fact(rs.getString("merchant_id"), rs.getString("merchant_type"), rs.getString("project_id"),
                TicketRewardConfigService.instant(rs, "paid_at"), rs.getString("member"), rs.getLong("effective"),
                TicketRewardConfigService.instant(rs, "completed_at"));
    }

    private void create(String sourceType, String sourceId, Fact f) {
        if (f.member() == null || f.paidAt() == null || f.completedAt() == null || f.effective() <= 0) return;
        Optional<TicketRewardConfig> pointsRule = configs.effectiveOptional("merchant_points", null, null, f.format(), f.paidAt());
        Optional<TicketRewardConfig> wait = configs.effectiveOptional("national", null, null, null, f.paidAt())
                .filter(n -> n.values().merchantWaitingHours() != null);
        Optional<TicketRewardConfig> growthRule = configs.effectiveOptional("merchant_growth", null, null, f.format(), f.paidAt());
        boolean pointsOn = pointsRule.isPresent() && wait.isPresent();
        if (!pointsOn) {
            // 积分 PRD §6.1：缺已生效业态规则时阻断积分计算并告警补齐，不得猜算。
            audit.recordFailure("merchant.reward.points_blocked", "not_applicable", sourceType, sourceId, null, null,
                    pointsRule.isEmpty() ? "MERCHANT_POINTS_RULE_REQUIRED" : "MERCHANT_WAITING_HOURS_REQUIRED");
        }
        CustomerRecord customer = customers.findCustomerById(f.member()).orElse(null);
        if (customer == null) return;
        CustomerPointsAccountRecord account = loyalty.loadOrCreateForUpdate(f.member(), f.completedAt());
        long levelBp = loyalty.levelMultiplierBasisPoints(account.level());
        long birthdayBp = loyalty.birthdayMultiplierBasisPoints(customer, f.paidAt());
        long base = pointsOn ? f.effective() / pointsRule.get().values().fixedValue() : 0;
        long points = base * levelBp / 10_000 * birthdayBp / 10_000;
        long growth = growthRule.map(g -> f.effective() / g.values().fixedValue()).orElse(0L);
        if (points + growth == 0) return;
        String id = "mcr_" + UUID.randomUUID().toString().replace("-", "");
        Integer hours = points > 0 ? wait.get().values().merchantWaitingHours() : null;
        Instant available = hours == null ? null : f.completedAt().plusSeconds(hours * 3600L);
        int inserted = jdbc.update("""
            INSERT INTO merchant_consumption_rewards(id, source_type, source_id, customer_id, merchant_id, project_id, business_format,
              effective_amount_cent, points_rule_id, growth_rule_id, wait_rule_id, level_code, level_multiplier_bp, birthday_multiplier_bp,
              paid_at, base_points, points, growth, waiting_hours, completed_at, available_at, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
            ON CONFLICT (source_type, source_id) DO NOTHING
            """, id, sourceType, sourceId, f.member(), f.merchantId(), f.projectId(), f.format(), f.effective(),
                pointsOn ? pointsRule.get().id() : null, growthRule.map(TicketRewardConfig::id).orElse(null),
                hours == null ? null : wait.get().id(), account.level(), levelBp, birthdayBp, ts(f.paidAt()), base, points, growth,
                hours, ts(f.completedAt()), available == null ? null : ts(available), points > 0 ? "pending" : "confirmed");
        if (inserted == 0) return;
        if (growth > 0) {
            customers.savePoints(new CustomerPointsAccountRecord(account.id(), account.customerId(), account.balance(), account.level(),
                    account.recentSummary(), account.frozenPoints(), account.accountStatus(), account.levelPeriodStart(),
                    Math.addExact(account.keepThresholdProgress(), growth), account.graceUntil(), account.lastCheckInDate(),
                    account.checkInStreak(), account.createdAt(), f.completedAt()));
            jdbc.update("INSERT INTO customer_growth_ledger(id, customer_id, merchant_reward_id, change_type, growth_delta, created_at) VALUES (?, ?, ?, 'earn', ?, ?)",
                    "growth_" + id, f.member(), id, growth, ts(f.completedAt()));
        }
        if (points > 0) {
            customers.savePointsLedger(new CustomerPointsLedgerRecord("ledger_earn_" + id, f.member(), points, account.balance(),
                    "earn", id, "商户消费待确认积分", null, null, false, f.completedAt(), "earn", "pending", sourceType, sourceId));
        }
        audit.recordSuccess("merchant.reward.pending", "not_applicable", "merchant_consumption_reward", id, null, sourceId,
                Map.of("source_type", sourceType, "customer_id", f.member(), "points", points, "growth", growth,
                        "effective_amount_cent", f.effective()));
    }

    private void bind(String sourceType, String sourceId, String customerId, boolean overwrite) {
        jdbc.update("INSERT INTO merchant_consumption_members(source_type, source_id, customer_id, identified_at) VALUES (?, ?, ?, now()) "
                + "ON CONFLICT (source_type, source_id) DO " + (overwrite
                        ? "UPDATE SET customer_id = EXCLUDED.customer_id, identified_at = EXCLUDED.identified_at" : "NOTHING"),
                sourceType, sourceId, customerId);
    }

    private static Timestamp ts(Instant at) {
        return Timestamp.from(at);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
