package com.complex.platform.parking;

// 文件版本：V0.04

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.money.MoneyRounding;
import com.complex.platform.customer.MerchantConsumptionRewardService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.ArApItemRecord;
import com.complex.platform.finance.ArApItemSourceRecord;
import com.complex.platform.finance.FinanceRepository;
import com.complex.platform.finance.RefundPayableSettled;
import com.complex.platform.settlement.InternalPayDayService;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HP-P01 停车消费抵免（BSM「停车场」章「消费抵免」条；决策台账 N02、N03；第十二批操作员裁决补充第 3、4、11 条）。
 * <ul>
 *   <li>资格：同一客人、本项目、同一营业日（ENUM「运营营业日」，按下单时点归属）的酒吧账单与商户服务点单有效消费累计达门槛即生成当日唯一一条，
 *       门票不计入、也不承担；酒吧账单有效消费与商户消费奖励同口径（收银识别的会员优先，否则下单客人；已退部分排除），服务点单须收款时已识别会员；
 *       客人查看、查车与下单时按需评估，未使用前按当前有效消费刷新组成，跌破门槛撤销、重新达标恢复；达到门槛的多倍不增加次数。</li>
 *   <li>使用：只有缴费时实际抵掉计费小时才置已使用（免费时长内离场、没抵掉计费时长的不算用掉、不产生成本；实际抵掉小时按实际少收的钱折回，
 *       一分没少收的——如封顶下抵免前后同价——不算用掉）；同一次抵免以缴费预占防重复使用。
 *       续缴按整次停车累计重算实际抵掉小时与成本（可升可降）；续缴后累计一分没少收的（如带抵免缴费后停到每日封顶），抵免退回可用、不计成本。
 *       成本＝实际抵掉的计费小时数×首次使用时固化的每小时内部结算价（内部结算价目录中城市项目公司「停车抵免（每小时）」生效条目）。</li>
 *   <li>分担：同一商户的组成先合并，按组成有效消费金额比例四舍五入到分，尾差单列一行由停车收款主体城市项目公司承担；
 *       按月合并为城市项目公司对各承担商户的普通内部结算应收（钱路 parking_credit_cost，付款时点按付账日规则固化），到出账日由 MerchantSettlementStatementNoticeJob 随结算金额出账短信通知商户（金额、原因、异议截止日；第 110 条）。</li>
 *   <li>退款：对应酒吧账单退款成功后，未使用的按剩余有效消费重核或撤销；已使用的保留真实停车服务与原费用承担，不另扣、不冲已发生的内部应收应付。</li>
 * </ul>
 */
@Service
public class ParkingCreditService {

    static final String PRICE_CATEGORY = "parking_credit";
    static final String ROUTE = "parking_credit_cost";

    /** 消费商户的合同主体（与商户积分发行费承担方同口径）。 */
    private static final String BEARER = """
            (SELECT COALESCE((SELECT x.contract_entity_id FROM business_entity_bindings x WHERE x.merchant_id = m.id
                AND x.status = 'active' AND x.effective_from <= CURRENT_DATE AND (x.effective_to IS NULL OR x.effective_to >= CURRENT_DATE)
                ORDER BY x.created_at DESC LIMIT 1), m.contract_entity_id) AS entity_id) e""";
    private static final String COMPONENTS = """
            SELECT * FROM (
              SELECT 'bar_bill' AS source_type, b.id AS source_id, b.merchant_id, e.entity_id AS bearer_entity_id,
                     __BAR_EFFECTIVE__ AS amount_cent, o.created_at AS consumed_at
                FROM bar_bills b JOIN bar_orders o ON o.id = b.order_id JOIN merchants m ON m.id = b.merchant_id
                LEFT JOIN merchant_consumption_members mm ON mm.source_type = 'bar_bill' AND mm.source_id = b.id
                CROSS JOIN LATERAL __BEARER__
               WHERE b.settlement_status = 'settled' AND COALESCE(mm.customer_id, o.customer_id) = ? AND m.project_id = ?
                 AND o.created_at >= ? AND o.created_at < ?
              UNION ALL
              SELECT 'service_order', s.id, s.bar_id, e.entity_id, s.price_cent, s.created_at
                FROM service_orders s JOIN merchants m ON m.id = s.bar_id
                JOIN merchant_consumption_members mm ON mm.source_type = 'service_order' AND mm.source_id = s.id
                CROSS JOIN LATERAL __BEARER__
               WHERE s.status = 'completed' AND mm.customer_id = ? AND COALESCE(s.project_id, m.project_id) = ?
                 AND s.created_at >= ? AND s.created_at < ?
            ) c WHERE c.amount_cent > 0 ORDER BY c.consumed_at, c.source_id
            """.replace("__BAR_EFFECTIVE__", MerchantConsumptionRewardService.BAR_EFFECTIVE).replace("__BEARER__", BEARER);

    public record Credit(String id, String projectId, String customerId, LocalDate businessDate, long thresholdCent, long creditMinutes,
            long eligibleAmountCent, String status, String pendingPaymentId, String sessionId) {}

    /** 当日评估结果：credit 为空表示从未达标；accumulated 为当前有效消费累计。 */
    public record Evaluation(Credit credit, long accumulatedCent, long thresholdCent, long creditMinutes) {}

    public record HourPrice(String entryId, long priceCent) {}

    private record Component(String sourceType, String sourceId, String merchantId, String bearerEntityId, long amountCent,
            Timestamp consumedAt) {}

    private final JdbcTemplate jdbc;
    private final ParkingService parking;
    private final BusinessDayResolver businessDays;
    private final AuditEventService audit;
    private final FinanceRepository finance;
    private final InternalPayDayService payDays;

    public ParkingCreditService(JdbcTemplate jdbc, ParkingService parking, BusinessDayResolver businessDays, AuditEventService audit,
            FinanceRepository finance, InternalPayDayService payDays) {
        this.jdbc = jdbc;
        this.parking = parking;
        this.businessDays = businessDays;
        this.audit = audit;
        this.finance = finance;
        this.payDays = payDays;
    }

    // ============ 资格 ============

    @Transactional
    public Evaluation evaluate(String customerId, String projectId, LocalDate day) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))", rs -> {}, "parking-credit:" + projectId + ":" + customerId + ":" + day);
        Credit existing = find("project_id=? AND customer_id=? AND business_date=? FOR UPDATE", projectId, customerId, day);
        if (existing != null && "used".equals(existing.status())) {
            return new Evaluation(existing, existing.eligibleAmountCent(), existing.thresholdCent(), existing.creditMinutes());
        }
        Timestamp from = Timestamp.from(businessDays.start(projectId, null, day));
        Timestamp to = Timestamp.from(businessDays.end(projectId, null, day));
        List<Component> components = jdbc.query(COMPONENTS, (rs, n) -> new Component(rs.getString("source_type"), rs.getString("source_id"),
                rs.getString("merchant_id"), rs.getString("bearer_entity_id"), rs.getLong("amount_cent"), rs.getTimestamp("consumed_at")),
                customerId, projectId, from, to, customerId, projectId, from, to);
        long total = components.stream().mapToLong(Component::amountCent).sum();
        if (existing == null) {
            ParkingService.Policy policy = parking.requirePolicy(projectId);
            if (total <= 0 || total < policy.creditThresholdCent()) {
                return new Evaluation(null, total, policy.creditThresholdCent(), policy.creditMinutes());
            }
            String id = "pcr_" + UUID.randomUUID().toString().replace("-", "");
            jdbc.update("""
                    INSERT INTO parking_credits (id, project_id, customer_id, business_date, threshold_cent, credit_minutes, eligible_amount_cent, status)
                    VALUES (?,?,?,?,?,?,?,'available')""", id, projectId, customerId, day, policy.creditThresholdCent(), policy.creditMinutes(), total);
            saveComponents(id, components);
            audit.recordSuccess("parking.credit.granted", "not_applicable", "parking_credit", id, null, id,
                    Map.of("customer_id", customerId, "business_date", day.toString(), "eligible_amount_cent", total));
            Credit created = find("id=?", id);
            return new Evaluation(created, total, created.thresholdCent(), created.creditMinutes());
        }
        boolean eligible = total > 0 && total >= existing.thresholdCent();
        String status = eligible ? "available" : "revoked";
        jdbc.update("""
                UPDATE parking_credits SET eligible_amount_cent=?, status=?, pending_payment_id=CASE WHEN ?='revoked' THEN NULL ELSE pending_payment_id END,
                       revoked_at=CASE WHEN ?='revoked' THEN COALESCE(revoked_at, now()) ELSE NULL END, updated_at=now() WHERE id=?""",
                total, status, status, status, existing.id());
        jdbc.update("DELETE FROM parking_credit_components WHERE credit_id=?", existing.id());
        saveComponents(existing.id(), components);
        if (!status.equals(existing.status())) {
            audit.recordSuccess(eligible ? "parking.credit.restored" : "parking.credit.revoked", "not_applicable", "parking_credit",
                    existing.id(), null, existing.id() + ":" + status + ":" + total,
                    Map.of("eligible_amount_cent", total, "threshold_cent", existing.thresholdCent()));
        }
        Credit refreshed = find("id=?", existing.id());
        return new Evaluation(refreshed, total, refreshed.thresholdCent(), refreshed.creditMinutes());
    }

    /** 缴费试算可用的抵免：按入场营业日评估；未使用的，或已用在本次停车上的（续缴时继续抵同一段时长）。 */
    @Transactional
    public Credit applicable(String customerId, String projectId, Instant enteredAt, String sessionId) {
        Credit credit = evaluate(customerId, projectId, businessDays.businessDate(projectId, null, enteredAt)).credit();
        if (credit == null) {
            return null;
        }
        boolean usable = "available".equals(credit.status()) || ("used".equals(credit.status()) && sessionId.equals(credit.sessionId()));
        return usable ? credit : null;
    }

    /** 下单预占：只有一笔缴费能占到同一次抵免（本次停车另一笔待付的缴费、或已出场车辆遗留的待付缴费可被接替）。 */
    boolean reserve(String creditId, String paymentId, String sessionId) {
        return jdbc.update("""
                UPDATE parking_credits SET pending_payment_id=?, updated_at=now()
                 WHERE id=? AND status='available' AND (pending_payment_id IS NULL OR pending_payment_id IN (
                     SELECT p.id FROM parking_payments p JOIN parking_sessions s ON s.id = p.session_id
                      WHERE p.status='pending' AND (p.session_id=? OR s.status='exited')))""", paymentId, creditId, sessionId) == 1;
    }

    /** 城市项目公司「停车抵免（每小时）」的生效内部结算价；没有或不唯一时 409，不按 0 计。 */
    HourPrice hourPrice(String receiverEntityId) {
        List<HourPrice> rows = jdbc.query("""
                SELECT id, price_cent FROM internal_price_catalog
                 WHERE provider_entity_id=? AND category=? AND is_draft=FALSE AND status='active'""",
                (rs, n) -> new HourPrice(rs.getString("id"), rs.getLong("price_cent")), receiverEntityId, PRICE_CATEGORY);
        if (rows.size() != 1) {
            throw ApiException.conflict("PARKING_CREDIT_PRICE_MISSING", "城市项目公司须有且仅有一条已生效的停车抵免每小时内部结算价");
        }
        return rows.getFirst();
    }

    // ============ 使用与分担 ============

    /**
     * 缴费成功：带抵免的缴费把抵免置已使用，实际抵掉小时数取本笔缴费按整次停车累计算出的值（续缴时可升可降），固化首次使用时的
     * 每小时内部结算价，按组成比例重算分担；续缴后累计一分没少收的（本笔实际抵掉 0 小时），抵免退回可用、不计成本
     * （第十二批裁决补充第 11 条「一分没少收＝没用掉抵免、不计成本」）。抵免已被撤销或已被别的缴费占走的，本笔缴费照常成立、不产生抵免成本。
     */
    void use(String paymentId, Instant at) {
        Map<String, Object> p = jdbc.queryForMap("""
                SELECT credit_id, session_id, credit_offset_hours, credit_hour_price_cent, credit_price_entry_id, receiver_entity_id
                  FROM parking_payments WHERE id=?""", paymentId);
        String creditId = (String) p.get("credit_id");
        if (creditId == null) {
            return;
        }
        if (((Number) p.get("credit_offset_hours")).longValue() == 0) {
            release(creditId, (String) p.get("session_id"), paymentId);
            return;
        }
        int updated = jdbc.update("""
                UPDATE parking_credits SET status='used', session_id=?, pending_payment_id=NULL, used_at=COALESCE(used_at, ?),
                       offset_hours=?, hour_price_cent=COALESCE(hour_price_cent, ?),
                       price_entry_id=COALESCE(price_entry_id, ?), receiver_entity_id=COALESCE(receiver_entity_id, ?),
                       cost_cent=? * COALESCE(hour_price_cent, ?), updated_at=now()
                 WHERE id=? AND ((status='available' AND pending_payment_id=?) OR (status='used' AND session_id=?))""",
                p.get("session_id"), Timestamp.from(at), p.get("credit_offset_hours"), p.get("credit_hour_price_cent"),
                p.get("credit_price_entry_id"), p.get("receiver_entity_id"), p.get("credit_offset_hours"), p.get("credit_hour_price_cent"),
                creditId, paymentId, p.get("session_id"));
        if (updated == 0) {
            return;
        }
        allocate(creditId);
        audit.recordSuccess("parking.credit.used", "not_applicable", "parking_credit", creditId, null, paymentId,
                Map.of("payment_id", paymentId, "offset_hours", p.get("credit_offset_hours")));
    }

    /** 续缴后整次停车累计一分没少收：已用在本次停车上的抵免退回可用，清本次停车、使用时点与成本，删未入账的分担。 */
    private void release(String creditId, String sessionId, String paymentId) {
        int updated = jdbc.update("""
                UPDATE parking_credits SET status='available', session_id=NULL, used_at=NULL, offset_hours=NULL, hour_price_cent=NULL,
                       price_entry_id=NULL, cost_cent=NULL, receiver_entity_id=NULL, updated_at=now()
                 WHERE id=? AND status='used' AND session_id=? AND NOT EXISTS (SELECT 1 FROM parking_credit_cost_allocations a
                     WHERE a.credit_id=parking_credits.id AND a.monthly_id IS NOT NULL)""", creditId, sessionId);
        if (updated == 0) {
            return;
        }
        jdbc.update("DELETE FROM parking_credit_cost_allocations WHERE credit_id=?", creditId);
        audit.recordSuccess("parking.credit.restored", "not_applicable", "parking_credit", creditId, null, paymentId,
                Map.of("payment_id", paymentId, "offset_hours", 0));
    }

    /** 成本分担：已并入月度内部结算的不再改动（ponytail：跨月续缴后抵掉小时的增减不再追加或退回，属极少情形）。 */
    private void allocate(String creditId) {
        if (jdbc.queryForObject("SELECT count(*) FROM parking_credit_cost_allocations WHERE credit_id=? AND monthly_id IS NOT NULL",
                Integer.class, creditId) > 0) {
            return;
        }
        Map<String, Object> c = jdbc.queryForMap("SELECT project_id, cost_cent, receiver_entity_id, used_at FROM parking_credits WHERE id=?",
                creditId);
        long cost = ((Number) c.get("cost_cent")).longValue();
        LocalDate month = ((Timestamp) c.get("used_at")).toInstant().atZone(LocalZone.ID).toLocalDate().withDayOfMonth(1);
        List<Map<String, Object>> groups = jdbc.queryForList("""
                SELECT merchant_id, max(bearer_entity_id) AS bearer_entity_id, sum(amount_cent) AS amount_cent
                  FROM parking_credit_components WHERE credit_id=? GROUP BY merchant_id ORDER BY merchant_id""", creditId);
        long total = groups.stream().mapToLong(g -> ((Number) g.get("amount_cent")).longValue()).sum();
        jdbc.update("DELETE FROM parking_credit_cost_allocations WHERE credit_id=?", creditId);
        List<Long> shares = new ArrayList<>();
        for (Map<String, Object> g : groups) {
            long amount = ((Number) g.get("amount_cent")).longValue();
            long share = MoneyRounding.prorate(cost, amount, total);
            shares.add(share);
            insertAllocation(creditId + ":" + g.get("merchant_id"), creditId, c.get("project_id"), g.get("merchant_id"),
                    g.get("bearer_entity_id"), amount, share, month);
        }
        long residual = MoneyRounding.residual(BigDecimal.valueOf(cost), shares);
        if (residual != 0) {
            insertAllocation(creditId + ":residual", creditId, c.get("project_id"), null, c.get("receiver_entity_id"), 0, residual, month);
        }
    }

    private void insertAllocation(String id, String creditId, Object projectId, Object merchantId, Object bearer, long amount, long share,
            LocalDate month) {
        jdbc.update("""
                INSERT INTO parking_credit_cost_allocations (id, credit_id, project_id, merchant_id, bearer_entity_id, component_amount_cent,
                    share_cent, cost_month) VALUES (?,?,?,?,?,?,?,?)""", id, creditId, projectId, merchantId, bearer, amount, share, month);
    }

    // ============ 按月入账 ============

    public record MonthGroup(String projectId, String merchantId, LocalDate costMonth) {}

    /** 已结束月份里尚未入账的承担商户分担（尾差行由城市项目公司自担，不对自己挂账）。 */
    public List<MonthGroup> dueMonths(Instant now) {
        LocalDate current = now.atZone(LocalZone.ID).toLocalDate().withDayOfMonth(1);
        return jdbc.query("""
                SELECT DISTINCT project_id, merchant_id, cost_month FROM parking_credit_cost_allocations
                 WHERE monthly_id IS NULL AND merchant_id IS NOT NULL AND cost_month < ? ORDER BY cost_month, project_id, merchant_id""",
                (rs, n) -> new MonthGroup(rs.getString(1), rs.getString(2), rs.getObject(3, LocalDate.class)), current);
    }

    /** 一个项目、一个承担商户、一个月合并为一笔城市项目公司对该商户的普通内部结算应收；重跑命中已入账即跳过。 */
    @Transactional
    public void postMonth(MonthGroup g, Instant now) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT a.id, a.share_cent, a.bearer_entity_id, c.receiver_entity_id FROM parking_credit_cost_allocations a
                  JOIN parking_credits c ON c.id = a.credit_id
                 WHERE a.project_id=? AND a.merchant_id=? AND a.cost_month=? AND a.monthly_id IS NULL
                 ORDER BY a.created_at DESC, a.id FOR UPDATE OF a""", g.projectId(), g.merchantId(), g.costMonth());
        if (rows.isEmpty()) {
            return;
        }
        long amount = rows.stream().mapToLong(r -> ((Number) r.get("share_cent")).longValue()).sum();
        String monthId = "pccm_" + UUID.randomUUID().toString().replace("-", "");
        String itemId = "arap_pcc_" + monthId.substring(5);
        YearMonth month = YearMonth.from(g.costMonth());
        InternalPayDayService.Schedule schedule = payDays.schedule(ROUTE, g.projectId(), g.costMonth(), now);
        ArApItemRecord item = new ArApItemRecord(itemId, "ARAP-PCC-" + monthId.substring(5, 17), "receivable", g.merchantId(), ROUTE, monthId,
                amount, 0L, amount, "CNY", schedule.dueDate(), month.toString(), "pending_collection", now, now, g.merchantId(), g.projectId());
        finance.appendArApItem(item);
        finance.saveArApItemSource(new ArApItemSourceRecord("arap_src_" + itemId, itemId, ROUTE, monthId, amount, now));
        payDays.record(item, schedule, g.projectId());
        jdbc.update("""
                INSERT INTO parking_credit_cost_months (id, project_id, merchant_id, bearer_entity_id, receiver_entity_id, cost_month, amount_cent, ar_ap_item_id)
                VALUES (?,?,?,?,?,?,?,?)""", monthId, g.projectId(), g.merchantId(), rows.getFirst().get("bearer_entity_id"),
                rows.getFirst().get("receiver_entity_id"), g.costMonth(), amount, itemId);
        for (Map<String, Object> r : rows) {
            jdbc.update("UPDATE parking_credit_cost_allocations SET monthly_id=? WHERE id=?", monthId, r.get("id"));
        }
        audit.recordSuccess("parking.credit_cost.posted", "not_applicable", "parking_credit_cost_month", monthId, null, monthId,
                Map.of("merchant_id", g.merchantId(), "cost_month", month.toString(), "amount_cent", amount, "ar_ap_item_id", itemId));
    }

    // ============ 退款重核 ============

    /**
     * 酒吧账单退款应付实付结清＝退款成功：未使用的按剩余有效消费重核或撤销；已使用的不改动，只留对照审计。
     * 第十七批：账单「作废并冲正」的原路退款应付以账单 ID 为来源（BarBillingService#refundCollectedOnVoid），同样纳入。
     */
    @EventListener
    @Transactional
    public void refundPayableSettled(RefundPayableSettled event) {
        for (String bill : jdbc.queryForList("""
                SELECT bill_id FROM bar_bill_adjustments WHERE id=?
                UNION SELECT id FROM bar_bills WHERE id=? AND payment_status='void'""", String.class, event.sourceId(), event.sourceId())) {
            List<Map<String, Object>> hits = jdbc.queryForList("""
                    SELECT c.id, c.project_id, c.customer_id, c.business_date, c.status FROM parking_credit_components x
                      JOIN parking_credits c ON c.id = x.credit_id WHERE x.source_type='bar_bill' AND x.source_id=?""", bill);
            for (Map<String, Object> c : hits) {
                if ("used".equals(c.get("status"))) {
                    audit.recordSuccess("parking.credit.refund_after_use", "not_applicable", "parking_credit", (String) c.get("id"), null,
                            event.sourceId(), Map.of("bill_id", bill, "refund_source_id", event.sourceId()));
                } else {
                    evaluate((String) c.get("customer_id"), (String) c.get("project_id"), ((java.sql.Date) c.get("business_date")).toLocalDate());
                }
            }
        }
    }

    // ============ 查看 ============

    /** 后台消费抵免台账：抵免、构成资格的消费与成本分担。 */
    public PageResponse<Map<String, Object>> list(String projectId, LocalDate businessDate) {
        String project = parking.reachableProject(projectId, ParkingService.VIEW);
        List<Map<String, Object>> credits = jdbc.queryForList("""
                SELECT id, customer_id, business_date::text AS business_date, threshold_cent, credit_minutes, eligible_amount_cent, status,
                       session_id, offset_hours, hour_price_cent, cost_cent, used_at, revoked_at
                  FROM parking_credits WHERE project_id=? AND (?::date IS NULL OR business_date=?)
                 ORDER BY business_date DESC, created_at DESC LIMIT 200""", project, businessDate, businessDate);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> c : credits) {
            Map<String, Object> row = ParkingVendorService.iso(c);
            row.put("components", jdbc.queryForList("""
                    SELECT x.source_type, x.source_id, x.merchant_id, m.merchant_name, x.bearer_entity_id, x.amount_cent, x.consumed_at::text AS consumed_at
                      FROM parking_credit_components x JOIN merchants m ON m.id = x.merchant_id WHERE x.credit_id=? ORDER BY x.consumed_at""",
                    c.get("id")));
            row.put("allocations", jdbc.queryForList("""
                    SELECT merchant_id, bearer_entity_id, component_amount_cent, share_cent, cost_month::text AS cost_month, monthly_id
                      FROM parking_credit_cost_allocations WHERE credit_id=? ORDER BY merchant_id NULLS LAST""", c.get("id")));
            items.add(row);
        }
        return new PageResponse<>(items, 1, items.size(), items.size());
    }

    /** 客人端：本项目当前营业日的抵免状态（可用／已用／已撤销／未达标）、已累计、门槛与抵免时长。 */
    @Transactional
    public Map<String, Object> customerView(String customerId, String projectId) {
        LocalDate day = businessDays.businessDate(projectId, null, Instant.now());
        Evaluation ev = evaluate(customerId, projectId, day);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("project_id", projectId);
        out.put("business_date", day.toString());
        out.put("status", ev.credit() == null ? "not_reached" : ev.credit().status());
        out.put("eligible_amount_cent", ev.accumulatedCent());
        out.put("threshold_cent", ev.thresholdCent());
        out.put("credit_minutes", ev.creditMinutes());
        return out;
    }

    private void saveComponents(String creditId, List<Component> components) {
        for (Component c : components) {
            jdbc.update("""
                    INSERT INTO parking_credit_components (credit_id, source_type, source_id, merchant_id, bearer_entity_id, amount_cent, consumed_at)
                    VALUES (?,?,?,?,?,?,?)""", creditId, c.sourceType(), c.sourceId(), c.merchantId(), c.bearerEntityId(), c.amountCent(),
                    c.consumedAt());
        }
    }

    private Credit find(String where, Object... args) {
        return jdbc.query("SELECT * FROM parking_credits WHERE " + where, (rs, n) -> new Credit(rs.getString("id"), rs.getString("project_id"),
                rs.getString("customer_id"), rs.getObject("business_date", LocalDate.class), rs.getLong("threshold_cent"),
                rs.getLong("credit_minutes"), rs.getLong("eligible_amount_cent"), rs.getString("status"), rs.getString("pending_payment_id"),
                rs.getString("session_id")), args).stream().findFirst().orElse(null);
    }
}
