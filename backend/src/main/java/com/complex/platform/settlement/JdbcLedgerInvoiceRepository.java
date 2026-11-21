package com.complex.platform.settlement;

// 文件版本：V0.02

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 第十八批 BU 段第 374 条 374-R1（BSM:1803「平台内部各主体之间的往来需要开具发票」；BSM:1750「开票金额≤已收−已退−已开」「退款后先红冲再重开」）：
 * 台账来源月度组取数。每组＝一类往来 × 主体 × 月，应开额取台账现值（出账即进清单，同 290-R2）；同月冲回、货款调整落在原月组里抵减。
 */
@Repository
public class JdbcLedgerInvoiceRepository {

    /**
     * 各来源月度组（不含绑定参数，好让 {@link JdbcSettlementInvoiceRepository#listOverIssued} 直接 JOIN）。统一列：group_key、ledger_source、
     * project_id、period_start、seller_entity_id、seller_name（只通用卡酒吧商户行有，兼作「收款方是酒吧商户」标记）、buyer_entity_id、
     * buyer_merchant_id、due_cent。
     * 第十八批 CD 段第 402 条另加三支（BSM:1803；BSM:1750「开票金额≤已收−已退−已开」）：lnd 商户之间借调买断货款与借用物赔偿（往来对方不是供货主体，
     * 与 trf 互补；项目缺时取借用单所属项目，第十八批裁决 18）；pds 通票免费酒水结算（出品酒吧 × 会计期间，应付减对冲应收，跨月冲正落冲正所在月）；
     * cpf 补偿新增履约结算（提供方 × 批次承担主体 × 原应付会计期间，冲正、重述与已付追收都并入原履约月）。
     * ponytail: 每轮全量聚合这几类来源的台账行（ar_ap_items 无 source_type 单列索引），行数大了再加 (source_type, accounting_period) 索引或只扫近几个月；
     * 通票酒水逐杯一行，是这些来源里行数最多的一类，聚合慢了先加这个索引。
     */
    static final String GROUPS_SQL = """
            SELECT 'sbf:' || m.id AS group_key, 'single_bar_service_fee'::varchar AS ledger_source, m.project_id,
                   m.billing_month AS period_start, NULL::varchar AS seller_entity_id, NULL::varchar AS seller_name,
                   (SELECT COALESCE(b.settlement_entity_id, b.business_entity_id) FROM merchants b WHERE b.id = m.bar_merchant_id) AS buyer_entity_id,
                   m.bar_merchant_id AS buyer_merchant_id,
            """ + SingleBarServiceFeeService.MONTH_TOTAL + " AS due_cent FROM " + SingleBarServiceFeeService.MONTH_TOTAL_FROM + """
             WHERE m.ar_ap_item_id IS NOT NULL
            UNION ALL
            SELECT 'trf:' || md5(concat(a.project_id, '|', a.merchant_id, '|', date_trunc('month', a.due_date::timestamp))), 'inventory_transfer',
                   a.project_id, date_trunc('month', a.due_date::timestamp)::date, NULL, NULL,
                   (SELECT COALESCE(b.settlement_entity_id, b.business_entity_id) FROM merchants b WHERE b.id = a.merchant_id), a.merchant_id,
                   SUM(a.amount_cent)
              FROM ar_ap_items a
             WHERE a.source_type = 'inventory_transfer' AND a.direction = 'payable'
               AND a.counterparty_id IN (SELECT id FROM supply_entities)
             GROUP BY a.project_id, a.merchant_id, date_trunc('month', a.due_date::timestamp)
            UNION ALL
            SELECT 'lnd:' || md5(concat(COALESCE(a.project_id, o.project_id), '|', a.merchant_id, '|', a.counterparty_id, '|',
                   date_trunc('month', a.due_date::timestamp))), 'inventory_lending',
                   COALESCE(a.project_id, o.project_id), date_trunc('month', a.due_date::timestamp)::date,
                   (SELECT COALESCE(b.settlement_entity_id, b.business_entity_id) FROM merchants b WHERE b.id = a.counterparty_id), NULL,
                   (SELECT COALESCE(b.settlement_entity_id, b.business_entity_id) FROM merchants b WHERE b.id = a.merchant_id), a.merchant_id,
                   SUM(a.amount_cent)
              FROM ar_ap_items a
              LEFT JOIN inventory_transfer_orders o ON o.id = a.source_id
             WHERE a.source_type = 'inventory_transfer' AND a.direction = 'payable'
               AND a.counterparty_id NOT IN (SELECT id FROM supply_entities)
             GROUP BY COALESCE(a.project_id, o.project_id), a.merchant_id, a.counterparty_id, date_trunc('month', a.due_date::timestamp)
            UNION ALL
            SELECT 'stf:' || md5(concat(a.source_type, '|', a.merchant_id, '|', a.counterparty_id, '|', a.accounting_period)), a.source_type,
                   MAX(a.project_id), to_date(a.accounting_period || '-01', 'YYYY-MM-DD'), a.counterparty_id, NULL,
                   (SELECT COALESCE(b.settlement_entity_id, b.business_entity_id) FROM merchants b WHERE b.id = a.merchant_id), a.merchant_id,
                   SUM(CASE WHEN a.direction = 'payable' THEN a.amount_cent ELSE -a.amount_cent END)
              FROM ar_ap_items a
             WHERE a.source_type IN ('dispatch_commission_transfer', 'bar_payroll_transfer')
             GROUP BY a.source_type, a.merchant_id, a.counterparty_id, a.accounting_period
            UNION ALL
            SELECT 'pcc:' || pm.id, 'parking_credit_cost', pm.project_id, pm.cost_month, pm.receiver_entity_id, NULL,
                   pm.bearer_entity_id, pm.merchant_id, pm.amount_cent
              FROM parking_credit_cost_months pm
            UNION ALL
            SELECT 'gcc:' || md5(concat(a.project_id, '|', a.counterparty_id, '|', a.accounting_period)), 'general_card_consume', a.project_id,
                   to_date(a.accounting_period || '-01', 'YYYY-MM-DD'),
                   CASE WHEN m.id IS NULL THEN a.counterparty_id ELSE COALESCE(m.settlement_entity_id, m.business_entity_id) END, m.merchant_name,
                   NULL, NULL, SUM(CASE WHEN a.direction = 'payable' THEN a.amount_cent ELSE -a.amount_cent END)
              FROM ar_ap_items a
              LEFT JOIN merchants m ON m.id = a.counterparty_id
             WHERE a.source_type = 'general_card_consume'
             GROUP BY a.project_id, a.counterparty_id, a.accounting_period, m.id, m.settlement_entity_id, m.business_entity_id, m.merchant_name
            UNION ALL
            SELECT 'pds:' || md5(concat(a.counterparty_id, '|', a.accounting_period)), 'pass_drink_settlement', MAX(a.project_id),
                   to_date(a.accounting_period || '-01', 'YYYY-MM-DD'),
                   (SELECT COALESCE(b.settlement_entity_id, b.business_entity_id) FROM merchants b WHERE b.id = a.counterparty_id), NULL,
                   NULL, NULL, SUM(CASE WHEN a.direction = 'payable' THEN a.amount_cent ELSE -a.amount_cent END)
              FROM ar_ap_items a
             WHERE a.source_type = 'pass_drink_settlement'
             GROUP BY a.counterparty_id, a.accounting_period
            UNION ALL
            SELECT 'cpf:' || md5(concat(a.project_id, '|', a.counterparty_id, '|', f.bearer_company_id, '|', p.accounting_period)),
                   'compensation_fulfillment', a.project_id, to_date(p.accounting_period || '-01', 'YYYY-MM-DD'),
                   (SELECT COALESCE(b.settlement_entity_id, b.business_entity_id) FROM merchants b WHERE b.id = a.counterparty_id), NULL,
                   f.bearer_company_id, NULL, SUM(CASE WHEN a.direction = 'payable' THEN a.amount_cent ELSE -a.amount_cent END)
              FROM ar_ap_items a
              JOIN amusement_compensation_fulfillments f
                ON (a.source_type = 'compensation_fulfillment' AND f.id = a.source_id)
                OR (a.source_type = 'compensation_fulfillment_recovery' AND f.reversal_item_id = a.source_id)
              JOIN ar_ap_items p ON p.id = f.payable_item_id
             WHERE a.source_type IN ('compensation_fulfillment', 'compensation_fulfillment_recovery')
             GROUP BY a.project_id, a.counterparty_id, f.bearer_company_id, p.accounting_period
            """;

    private static final String OPEN_STATUSES = "('pending','issuing','issued')";

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcLedgerInvoiceRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 已结束月份（period_start 早于本月首日）里应开额高于已开额（占额三态合计）的组。 */
    public List<LedgerGroup> groupsToInvoice(LocalDate monthStart) {
        return jdbc.query("SELECT g.* FROM (" + GROUPS_SQL + """
                ) g
                  LEFT JOIN (SELECT ledger_group_key, SUM(amount_cent) AS open_cent FROM settlement_invoice_items
                              WHERE ledger_group_key IS NOT NULL AND status IN """ + OPEN_STATUSES + """
                              GROUP BY ledger_group_key) o ON o.ledger_group_key = g.group_key
                 WHERE g.period_start < :month_start AND g.due_cent > COALESCE(o.open_cent, 0)
                 ORDER BY g.group_key
                """, new MapSqlParameterSource("month_start", monthStart), (rs, i) -> new LedgerGroup(rs.getString("group_key"),
                rs.getString("ledger_source"), rs.getString("project_id"), rs.getObject("period_start", LocalDate.class),
                rs.getString("seller_entity_id"), rs.getString("seller_name"), rs.getString("buyer_entity_id"),
                rs.getString("buyer_merchant_id"), rs.getLong("due_cent")));
    }

    /** 该组已开额（占额三态合计）。 */
    public long sumOpen(String groupKey) {
        Long v = jdbc.queryForObject("SELECT COALESCE(SUM(amount_cent), 0) FROM settlement_invoice_items WHERE ledger_group_key = :key AND status IN "
                + OPEN_STATUSES, new MapSqlParameterSource("key", groupKey), Long.class);
        return v == null ? 0L : v;
    }

    /** 同组并发生成串行（事务级咨询锁，同 JdbcFinanceRepository.lockRefundSource）。 */
    public void lock(String groupKey) {
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(:key, 0))", new MapSqlParameterSource("key", groupKey), Object.class);
    }

    /** 一个台账来源月度组。 */
    public record LedgerGroup(String groupKey, String ledgerSource, String projectId, LocalDate periodStart, String sellerEntityId,
            String sellerName, String buyerEntityId, String buyerMerchantId, long dueCent) {
    }
}
