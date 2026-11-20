package com.complex.platform.settlement;

// 文件版本：V0.02

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.ArApItemRecord;
import com.complex.platform.finance.ArApItemSourceRecord;
import com.complex.platform.finance.FinanceRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AZ 段第 350 条 350-R1（BSM:1704「长沙项目公司就通用卡余额的实际消费向各业态的经营主体直接付款结算，按三个付账日结清。」；PGM:420 同句；
 * 决策台账 DEC-R3-2026-08-04-02；ENUM:86「购买通票按通票规则与娱乐平台公司结算，购买游乐与游乐公司结算，购买演出与传媒公司结算」「充值赠送金额与赠送权益的
 * 成本一律由长沙项目公司承担」「相应消费结算的通路费调整关联原消费批次及原费率依据」；ENUM:32；BSM:749「两者一律在拆分给各域之前从整单扣除且只扣一次」；
 * BSM:1673「凡属电子数据、且计算比例固定的，一律在月结定格时即出结果」；BSM:755「收支两条线，不得轧差」）：通用卡购票按营业月合并挂长沙项目公司对
 * 票款收款主体的应付，跨月退款另挂追收应收。由 {@link GeneralCardConsumptionSettlementJob} 每日一轮调用，写法照停车抵免成本按月入账。
 * <ul>
 *   <li>取数：通用卡（卡不挂商户）的消费流水，其储值付款意向是已成功的票务订单付款、订单已出票（出票前撤单的消费与其回冲都不取）；计入额＝消费额减该笔
 *       已固化的批次通路费（通票分账已按同一笔从整单扣过，只扣一次；赠送段不扣）。回冲流水（消费退款回原卡）计入额＝负的（回冲额减本次按原批次原费率
 *       冲减的通路费）。</li>
 *   <li>收款主体按本单有价权益的商户业态：全为游乐取 amusement_ticket、全为传媒取 performance_ticket，其余（单一酒吧门票、组合通票）取 all_pass_ticket，
 *       与通票分账 {@link JdbcSettlementBillRepository#OWN_COLLECTED} 同一口径；归属未配置的跳过、配好后下一轮入账。</li>
 *   <li>只入已结束的营业月；按（项目、收款主体、营业月、正常或追收）合并：正常组＝该月消费加同月回冲，挂应付；回冲所属营业月晚于原消费的另成追收组，
 *       挂应收（同一来源正挂应付、负挂应收，照 pass_domain_share），不与当月应付轧差，已付出的应付也不冲正。</li>
 *   <li>台账号与来源号按组内流水集合派生（晚到的流水自然另成一笔）；付款时点按钱路 general_card_consume 固化，缺规则即整组不入、下一轮重试。</li>
 * </ul>
 * 第十八批 BD 段第 367 条 367-R2（ENUM:86「在商户处消费商品或服务的，按该商户给客人的成交价格、扣除该笔消费所对应的通路费后与该商户结算，不另扣任何比例」；
 * BSM:704「独立酒吧商户负责自己的经营、人员、设备、单一酒吧门票和店内收入。」）：酒吧账单通用卡消费（储值结账 barsv_ 流水对应 settle_ 结算行）按酒吧商户成组，
 * 收款方＝该酒吧商户，计入额＝消费额减该笔已分摊的批次通路费，营业月按该酒吧的营业日；反结与退款回卡的回冲行照同一公式冲减。
 * ponytail: 通用卡已关闭后才办的消费退款，本金改登记退本人、不恢复批次也不写 fee_adjust，这笔回冲按全额冲减，会比原消费净额多冲一笔通路费；卡关闭期间仍有
 *   未退门票的情形罕见，真遇到再按 stored_value_consume_allocations 的原费率补算。
 */
@Service
public class GeneralCardConsumptionSettlement {

    public static final String ROUTE = "general_card_consume";

    private final NamedParameterJdbcTemplate jdbc;
    private final FinanceRepository finance;
    private final InternalPayDayService payDays;
    private final BusinessDayResolver businessDays;
    private final RevenueStreamAttributionRepository attribution;
    private final AuditEventService audit;

    public GeneralCardConsumptionSettlement(NamedParameterJdbcTemplate jdbc, FinanceRepository finance, InternalPayDayService payDays,
            BusinessDayResolver businessDays, RevenueStreamAttributionRepository attribution, AuditEventService audit) {
        this.jdbc = jdbc;
        this.finance = finance;
        this.payDays = payDays;
        this.businessDays = businessDays;
        this.attribution = attribution;
        this.audit = audit;
    }

    /** 一笔待计入的流水：at＝流水时刻，consumedAt＝原消费时刻（消费即本身），amountCent＝计入额；merchantId、barId 只酒吧账单行有（367-R2）。 */
    record ConsumeLine(String txnId, String projectId, String revenueStream, Instant at, Instant consumedAt, long amountCent,
            String merchantId, String barId) {
    }

    /** 一组（项目、收款主体、营业月、正常或追收）的待入账流水。 */
    public record Group(String projectId, String payeeEntityId, YearMonth month, boolean recovery, List<ConsumeLine> lines) {
    }

    private record GroupKey(String projectId, String payeeEntityId, YearMonth month, boolean recovery) {
    }

    /** 尚未计入、所属营业月已结束的流水，按（项目、收款主体、营业月、正常或追收）分组。 */
    public List<Group> pendingGroups(Instant now) {
        String own = JdbcSettlementBillRepository.OWN_COLLECTED;
        List<ConsumeLine> lines = jdbc.query("""
                WITH consumes AS (
                    SELECT t.id, o.id AS order_id, o.project_id, t.created_at, -t.amount_cent - COALESCE(f.fee_cent, 0) AS amount_cent,
                           NULL::varchar AS merchant_id, NULL::varchar AS bar_id
                      FROM customer_stored_value_transactions t
                      JOIN customer_stored_value_accounts a ON a.id = t.account_id AND a.merchant_id IS NULL
                      JOIN ticket_payment_intents p ON p.id = t.id AND p.business_order_type = 'ticket_order'
                           AND p.channel = 'stored_value' AND p.status = 'succeeded'
                      JOIN ticket_orders o ON o.id = p.business_order_id
                      LEFT JOIN ticket_payment_route_fees f ON f.payment_intent_id = t.id
                     WHERE t.txn_type = 'consume' AND EXISTS (SELECT 1 FROM issued_entitlements e WHERE e.order_id = o.id)
                    UNION ALL
                    -- 367-R2：酒吧账单通用卡消费（反结只把结算行置 reversed，不按它过滤，回冲行另行冲减）
                    SELECT t.id, NULL, a.project_id, t.created_at,
                           -t.amount_cent - COALESCE((SELECT SUM(x.fee_cent) FROM stored_value_consume_allocations x
                                                       WHERE x.consume_txn_id = t.id), 0),
                           bs.merchant_id, b.bar_id
                      FROM customer_stored_value_transactions t
                      JOIN customer_stored_value_accounts a ON a.id = t.account_id AND a.merchant_id IS NULL
                      JOIN bar_bill_settlements bs ON left(t.idempotency_key, 6) = 'barsv_'
                           AND bs.id = 'settle_' || substr(t.idempotency_key, 7) AND bs.settle_channel = 'stored_value'
                      JOIN bar_bills b ON b.id = bs.bill_id
                     WHERE t.txn_type = 'consume'
                ), streams AS (
                    SELECT c.*, CASE WHEN %s THEN 'amusement_ticket' WHEN %s THEN 'performance_ticket' ELSE 'all_pass_ticket' END AS stream
                      FROM consumes c
                )
                SELECT s.id, s.project_id, s.stream, s.created_at, s.created_at AS consumed_at, s.amount_cent, s.merchant_id, s.bar_id
                  FROM streams s
                 WHERE NOT EXISTS (SELECT 1 FROM general_card_consume_lines l WHERE l.txn_id = s.id)
                UNION ALL
                SELECT r.id, s.project_id, s.stream, r.created_at, s.created_at,
                       -(r.amount_cent + COALESCE((SELECT SUM(x.fee_adjust_cent) FROM stored_value_consume_restorations x
                                                    WHERE x.reversal_txn_id = r.id), 0)),
                       s.merchant_id, s.bar_id
                  FROM customer_stored_value_transactions r
                  JOIN streams s ON s.id = r.original_txn_id
                 WHERE r.txn_type = 'consume_reversal'
                   AND NOT EXISTS (SELECT 1 FROM general_card_consume_lines l WHERE l.txn_id = r.id)
                """.formatted(own.formatted("c", "'amusement'"), own.formatted("c", "'media'")), Map.of(),
                (rs, i) -> new ConsumeLine(rs.getString(1), rs.getString(2), rs.getString(3), rs.getTimestamp(4).toInstant(),
                        rs.getTimestamp(5).toInstant(), rs.getLong(6), rs.getString(7), rs.getString(8)));
        Map<GroupKey, List<ConsumeLine>> groups = new LinkedHashMap<>();
        for (ConsumeLine line : lines) {
            YearMonth month = month(line.projectId(), line.barId(), line.at());
            if (!month.isBefore(month(line.projectId(), line.barId(), now))) {
                continue;
            }
            // 367-R2：酒吧账单行与该酒吧商户结算（ENUM:86「与该商户结算」；BSM:704 店内收入归独立酒吧商户）
            String payee = line.merchantId() != null ? line.merchantId()
                    : attribution.resolveManagementEntity(line.projectId(), line.revenueStream());
            if (payee == null) {
                continue;
            }
            boolean recovery = month(line.projectId(), line.barId(), line.consumedAt()).isBefore(month);
            groups.computeIfAbsent(new GroupKey(line.projectId(), payee, month, recovery), k -> new ArrayList<>()).add(line);
        }
        return groups.entrySet().stream().map(e -> new Group(e.getKey().projectId(), e.getKey().payeeEntityId(), e.getKey().month(),
                e.getKey().recovery(), e.getValue())).toList();
    }

    /**
     * 一组合并为一笔长沙项目公司与收款主体之间的普通内部结算往来：正常组挂应付（金额为 0＝同月全额退回，不挂），追收组挂应收；组内每笔流水记进
     * general_card_consume_lines。缺钱路付账日规则 409 PAY_DAY_NOT_CONFIGURED，整组回滚、下一轮重试。
     */
    @Transactional
    public void postGroup(Group g, Instant now) {
        long net = g.lines().stream().mapToLong(ConsumeLine::amountCent).sum();
        long amount = g.recovery() ? -net : net;
        String itemId = null;
        if (amount > 0) {
            String digest = digest(g.lines().stream().map(ConsumeLine::txnId).sorted().toList());
            itemId = (g.recovery() ? "arap_gcr_" : "arap_gcc_") + digest;
            InternalPayDayService.Schedule schedule = payDays.schedule(ROUTE, g.projectId(), g.month().atDay(1), now);
            ArApItemRecord item = new ArApItemRecord(itemId, (g.recovery() ? "ARAP-GCR-" : "ARAP-GCC-") + digest,
                    g.recovery() ? "receivable" : "payable", g.payeeEntityId(), ROUTE, digest, amount, 0L, amount, "CNY",
                    schedule.dueDate(), g.month().toString(), g.recovery() ? "pending_collection" : "pending_payment", now, now,
                    null, g.projectId());
            finance.appendArApItem(item);
            finance.saveArApItemSource(new ArApItemSourceRecord("arap_src_" + itemId, itemId, ROUTE, digest, amount, now));
            payDays.record(item, schedule, g.projectId());
            // 18AZ 收尾复核（BSM:522「都必须可点击、可追溯到来源单据与操作记录」）：照停车抵免成本 parking.credit_cost.posted 记一条入账留痕。
            audit.recordSuccess("settlement.general_card_consume.posted", "not_applicable", "ar_ap_item", itemId, null, itemId,
                    Map.of("payee_entity_id", g.payeeEntityId(), "accounting_period", g.month().toString(), "recovery", g.recovery(),
                            "amount_cent", amount));
        }
        for (ConsumeLine line : g.lines()) {
            jdbc.update("INSERT INTO general_card_consume_lines (txn_id, ar_ap_item_id, amount_cent) VALUES (:txn, :item, :amount)",
                    new MapSqlParameterSource().addValue("txn", line.txnId()).addValue("item", itemId).addValue("amount", line.amountCent()));
        }
    }

    private YearMonth month(String projectId, String barId, Instant at) {
        return YearMonth.from(businessDays.businessDate(projectId, barId, at));
    }

    /** 组内流水号（已排序）拼接的 SHA-256 前 20 位：同一组流水同一编号，晚到的流水另成一笔。 */
    private static String digest(List<String> txnIds) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(String.join(",", txnIds).getBytes(StandardCharsets.UTF_8))).substring(0, 20);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
