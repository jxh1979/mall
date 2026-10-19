package com.complex.platform.settlement;

// 文件版本：V0.02

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 批 4b 开票清单数据访问(V362:invoice_subject_rules + settlement_invoice_items)。
 *
 * <p>只做取数与落库,规则/税率解析复用既有两张配置表(见 {@link SettlementInvoiceService}):
 * 主体走 {@code revenue_stream_attribution}、费率走 {@code settlement_tax_configs}。
 */
@Repository
public class JdbcSettlementInvoiceRepository {

    /** 单头级条目(无分账行来源)在唯一键里的占位,与迁移 uq_sii_source_open 一致。 */
    static final String BILL_LEVEL_KEY = "__BILL__";

    /** 占用「已开票额度」的状态:待开票/开票中/已开票。红冲/作废/失败不占额。 */
    private static final String OPEN_STATUSES = "('pending','issuing','issued')";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcSettlementInvoiceRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ------------------------------------------------------------------
    // 规则(invoice_subject_rules):项目特例 > 平台默认 > 未配置(null)
    // ------------------------------------------------------------------

    public InvoiceSubjectRule findRule(String projectId, String incomeType) {
        String sql = """
                SELECT income_type, revenue_stream, invoice_item_name, tax_category,
                       evidence_required, invoiceable
                  FROM invoice_subject_rules
                 WHERE income_type = :income_type AND status = 'active'
                   AND (project_id = :project_id OR project_id IS NULL)
                 ORDER BY (project_id IS NOT NULL) DESC
                 LIMIT 1
                """;
        var params = new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("income_type", incomeType);
        return jdbcTemplate.query(sql, params, rs -> rs.next()
                ? new InvoiceSubjectRule(rs.getString("income_type"), rs.getString("revenue_stream"),
                        rs.getString("invoice_item_name"), rs.getString("tax_category"),
                        rs.getString("evidence_required"), rs.getBoolean("invoiceable"))
                : null);
    }

    public List<InvoiceSubjectRule> listRules(String projectId) {
        String sql = """
                SELECT DISTINCT ON (income_type)
                       income_type, revenue_stream, invoice_item_name, tax_category,
                       evidence_required, invoiceable
                  FROM invoice_subject_rules
                 WHERE status = 'active' AND (project_id = :project_id OR project_id IS NULL)
                 ORDER BY income_type, (project_id IS NOT NULL) DESC
                """;
        var params = new MapSqlParameterSource().addValue("project_id", projectId);
        return jdbcTemplate.query(sql, params, (rs, i) -> new InvoiceSubjectRule(
                rs.getString("income_type"), rs.getString("revenue_stream"),
                rs.getString("invoice_item_name"), rs.getString("tax_category"),
                rs.getString("evidence_required"), rs.getBoolean("invoiceable")));
    }

    // ------------------------------------------------------------------
    // 税率(settlement_tax_configs):同 项目特例 > 平台默认 解析口径
    // ------------------------------------------------------------------

    /** 某收入类型某税种的生效费率;{@code applicable=false} 或未配置返回 null(= 不计这笔费用)。 */
    public Integer findTaxRateBps(String projectId, String incomeType, String taxType) {
        String sql = """
                SELECT rate_bps FROM settlement_tax_configs
                 WHERE income_type = :income_type AND tax_type = :tax_type
                   AND applicable = TRUE AND status = 'active' AND is_draft = FALSE
                   AND (project_id = :project_id OR project_id IS NULL)
                 ORDER BY (project_id IS NOT NULL) DESC
                 LIMIT 1
                """;
        var params = new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("income_type", incomeType)
                .addValue("tax_type", taxType);
        return jdbcTemplate.query(sql, params, rs -> rs.next() ? rs.getInt("rate_bps") : null);
    }

    // ------------------------------------------------------------------
    // 取数:结算单 / 分账行 / 商户结算主体
    // ------------------------------------------------------------------

    public InvoiceSource findBillSource(String billId) {
        String sql = """
                SELECT id, project_id, period_start, period_end, status,
                       -- S4:综合服务费拆两支(池链/专属,同一道费都归娱乐平台公司),开票应开额=两支合计。
                       comp_service_fee_pooled_cent + comp_service_fee_exclusive_cent AS comp_service_fee_cent
                  FROM settlement_bills WHERE id = :id
                """;
        var params = new MapSqlParameterSource().addValue("id", billId);
        return jdbcTemplate.query(sql, params, rs -> rs.next()
                ? new InvoiceSource(rs.getString("id"), rs.getString("project_id"),
                        rs.getObject("period_start", LocalDate.class), rs.getObject("period_end", LocalDate.class),
                        rs.getString("status"), rs.getLong("comp_service_fee_cent"))
                : null);
    }

    /** 该结算单的各酒吧分账行(仅未冲正行进清单)。 */
    public List<BillMerchantShare> listMerchantShares(String billId) {
        String sql = """
                SELECT i.id, i.merchant_id, i.final_amount_cent,
                       COALESCE(m.settlement_entity_id, m.business_entity_id) AS entity_id,
                       m.merchant_name
                  FROM settlement_items i
                  LEFT JOIN merchants m ON m.id = i.merchant_id
                 WHERE i.bill_id = :bill_id AND i.status <> 'reversed'
                 ORDER BY i.id
                """;
        var params = new MapSqlParameterSource().addValue("bill_id", billId);
        return jdbcTemplate.query(sql, params, (rs, i) -> new BillMerchantShare(
                rs.getString("id"), rs.getString("merchant_id"), rs.getLong("final_amount_cent"),
                rs.getString("entity_id"), rs.getString("merchant_name")));
    }

    public String findEntityName(String entityId) {
        if (entityId == null) {
            return null;
        }
        var params = new MapSqlParameterSource().addValue("id", entityId);
        return jdbcTemplate.query("SELECT entity_name FROM business_entities WHERE id = :id", params,
                rs -> rs.next() ? rs.getString("entity_name") : null);
    }

    // ------------------------------------------------------------------
    // 额度:已开票额(占额状态合计)——服务层用它算「应开 − 已开」
    // ------------------------------------------------------------------

    public long sumOpenAmountCent(String billId, String incomeType, String settlementItemId) {
        String sql = """
                SELECT COALESCE(SUM(amount_cent), 0) FROM settlement_invoice_items
                 WHERE bill_id = :bill_id AND income_type = :income_type
                   AND COALESCE(settlement_item_id, :bill_level) = COALESCE(:item_id, :bill_level)
                   AND status IN """ + OPEN_STATUSES;
        var params = new MapSqlParameterSource()
                .addValue("bill_id", billId)
                .addValue("income_type", incomeType)
                .addValue("item_id", settlementItemId)
                .addValue("bill_level", BILL_LEVEL_KEY);
        Long v = jdbcTemplate.queryForObject(sql, params, Long.class);
        return v == null ? 0L : v;
    }

    // ------------------------------------------------------------------
    // FS-022-R1：积分发行费月账单来源（已形成正式应收的账单；已收额取应收应付台账）
    // ------------------------------------------------------------------

    /** 锁住积分月账单行并取开票所需事实；尚未形成正式应收返回 null。同账单并发生成在此串行。 */
    public PointsBillSource lockPointsBillSource(String pointsBillId) {
        String sql = """
                SELECT b.id, b.project_id, b.billing_month, b.seller_entity_id, b.seller_merchant_id,
                       b.receiver_entity_id, a.paid_amount_cent
                  FROM ticket_points_monthly_bills b
                  JOIN ar_ap_items a ON a.id = b.ar_ap_item_id
                 WHERE b.id = :id
                   FOR UPDATE OF b
                """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource().addValue("id", pointsBillId), rs -> rs.next()
                ? new PointsBillSource(rs.getString("id"), rs.getString("project_id"),
                        rs.getObject("billing_month", LocalDate.class), rs.getString("seller_entity_id"),
                        rs.getString("seller_merchant_id"), rs.getString("receiver_entity_id"),
                        rs.getLong("paid_amount_cent"))
                : null);
    }

    /** 已收额高于已开额（占额三态合计）的积分月账单，供定时作业逐张生成清单。 */
    public List<String> listPointsBillsToInvoice() {
        return jdbcTemplate.queryForList("""
                SELECT b.id FROM ticket_points_monthly_bills b
                  JOIN ar_ap_items a ON a.id = b.ar_ap_item_id
                 WHERE a.paid_amount_cent > (SELECT COALESCE(SUM(s.amount_cent), 0) FROM settlement_invoice_items s
                                              WHERE s.points_bill_id = b.id AND s.status IN """ + OPEN_STATUSES + """
                                            )
                 ORDER BY b.id
                """, new MapSqlParameterSource(), String.class);
    }

    public long sumOpenPointsAmountCent(String pointsBillId) {
        Long v = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(amount_cent), 0) FROM settlement_invoice_items
                 WHERE points_bill_id = :id AND status IN """ + OPEN_STATUSES,
                new MapSqlParameterSource().addValue("id", pointsBillId), Long.class);
        return v == null ? 0L : v;
    }

    // ------------------------------------------------------------------
    // 落库
    // ------------------------------------------------------------------

    public void insert(SettlementInvoiceItemRecord item) {
        String sql = """
                INSERT INTO settlement_invoice_items (
                    id, bill_id, settlement_item_id, project_id, period_start, period_end, income_type,
                    subject_entity_id, subject_name, invoice_item_name, tax_category, evidence_required,
                    amount_cent, vat_rate_bps, vat_amount_cent,
                    cultural_fee_applicable, cultural_fee_rate_bps, cultural_fee_cent,
                    status, remark, created_by, updated_by, points_bill_id, buyer_entity_id, buyer_merchant_id
                ) VALUES (
                    :id, :bill_id, :settlement_item_id, :project_id, :period_start, :period_end, :income_type,
                    :subject_entity_id, :subject_name, :invoice_item_name, :tax_category, :evidence_required,
                    :amount_cent, :vat_rate_bps, :vat_amount_cent,
                    :cultural_fee_applicable, :cultural_fee_rate_bps, :cultural_fee_cent,
                    :status, :remark, :actor, :actor, :points_bill_id, :buyer_entity_id, :buyer_merchant_id
                )
                """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("id", item.id())
                .addValue("bill_id", item.billId())
                .addValue("settlement_item_id", item.settlementItemId())
                .addValue("project_id", item.projectId())
                .addValue("period_start", item.periodStart())
                .addValue("period_end", item.periodEnd())
                .addValue("income_type", item.incomeType())
                .addValue("subject_entity_id", item.subjectEntityId())
                .addValue("subject_name", item.subjectName())
                .addValue("invoice_item_name", item.invoiceItemName())
                .addValue("tax_category", item.taxCategory())
                .addValue("evidence_required", item.evidenceRequired())
                .addValue("amount_cent", item.amountCent())
                .addValue("vat_rate_bps", item.vatRateBps())
                .addValue("vat_amount_cent", item.vatAmountCent())
                .addValue("cultural_fee_applicable", item.culturalFeeApplicable())
                .addValue("cultural_fee_rate_bps", item.culturalFeeRateBps())
                .addValue("cultural_fee_cent", item.culturalFeeCent())
                .addValue("status", item.status())
                .addValue("remark", item.remark())
                .addValue("actor", item.updatedBy())
                .addValue("points_bill_id", item.pointsBillId())
                .addValue("buyer_entity_id", item.buyerEntityId())
                .addValue("buyer_merchant_id", item.buyerMerchantId()));
    }

    public String findStatus(String itemId) {
        var params = new MapSqlParameterSource().addValue("id", itemId);
        return jdbcTemplate.query("SELECT status FROM settlement_invoice_items WHERE id = :id", params,
                rs -> rs.next() ? rs.getString("status") : null);
    }

    public int updateStatus(String itemId, String status, String invoiceNo, String remark, String actorId) {
        String sql = """
                UPDATE settlement_invoice_items
                   SET status = :status,
                       invoice_no = COALESCE(:invoice_no, invoice_no),
                       remark = COALESCE(:remark, remark),
                       updated_by = :actor, updated_at = now()
                 WHERE id = :id
                """;
        return jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("id", itemId).addValue("status", status)
                .addValue("invoice_no", invoiceNo).addValue("remark", remark)
                .addValue("actor", actorId));
    }

    // ------------------------------------------------------------------
    // 查询:分页清单 + 按开票主体分组汇总
    // ------------------------------------------------------------------

    private static final String LIST_FILTER = """
             WHERE (CAST(:project_id AS VARCHAR) IS NULL OR project_id = CAST(:project_id AS VARCHAR))
               AND (CAST(:period_start AS DATE) IS NULL OR period_start = CAST(:period_start AS DATE))
               AND (CAST(:subject_entity_id AS VARCHAR) IS NULL OR subject_entity_id = CAST(:subject_entity_id AS VARCHAR))
               AND (CAST(:status AS VARCHAR) IS NULL OR status = CAST(:status AS VARCHAR))
            """;

    public List<SettlementInvoiceItemView> list(String projectId, LocalDate periodStart, String subjectEntityId,
            String status, int limit, int offset) {
        String sql = """
                SELECT id, bill_id, settlement_item_id, project_id, period_start, period_end, income_type,
                       subject_entity_id, subject_name, invoice_item_name, tax_category, evidence_required,
                       amount_cent, vat_rate_bps, vat_amount_cent,
                       cultural_fee_applicable, cultural_fee_rate_bps, cultural_fee_cent,
                       status, invoice_no, remark, points_bill_id, buyer_entity_id,
                       (SELECT be.entity_name FROM business_entities be WHERE be.id = buyer_entity_id) AS buyer_name
                  FROM settlement_invoice_items
                """ + LIST_FILTER + """
                 ORDER BY period_start DESC, subject_name NULLS LAST, income_type, id
                 LIMIT :limit OFFSET :offset
                """;
        return jdbcTemplate.query(sql, filterParams(projectId, periodStart, subjectEntityId, status)
                .addValue("limit", limit).addValue("offset", offset),
                (rs, i) -> new SettlementInvoiceItemView(
                        rs.getString("id"), rs.getString("bill_id"), rs.getString("settlement_item_id"),
                        rs.getString("project_id"), rs.getObject("period_start", LocalDate.class),
                        rs.getObject("period_end", LocalDate.class), rs.getString("income_type"),
                        rs.getString("subject_entity_id"), rs.getString("subject_name"),
                        rs.getString("invoice_item_name"), rs.getString("tax_category"),
                        rs.getString("evidence_required"), rs.getLong("amount_cent"),
                        rs.getInt("vat_rate_bps"), rs.getLong("vat_amount_cent"),
                        rs.getBoolean("cultural_fee_applicable"), rs.getInt("cultural_fee_rate_bps"),
                        rs.getLong("cultural_fee_cent"), rs.getString("status"),
                        rs.getString("invoice_no"), rs.getString("remark"), rs.getString("points_bill_id"),
                        rs.getString("buyer_entity_id"), rs.getString("buyer_name")));
    }

    public long count(String projectId, LocalDate periodStart, String subjectEntityId, String status) {
        String sql = "SELECT count(*) FROM settlement_invoice_items" + LIST_FILTER;
        Long v = jdbcTemplate.queryForObject(sql, filterParams(projectId, periodStart, subjectEntityId, status),
                Long.class);
        return v == null ? 0L : v;
    }

    /** 按开票主体分组汇总(财务拿到即按主体开票);已红冲/已作废不计入。 */
    public List<InvoiceSubjectSummary> summarizeBySubject(String projectId, LocalDate periodStart) {
        String sql = """
                SELECT subject_entity_id, subject_name,
                       COUNT(*)                        AS item_count,
                       COALESCE(SUM(amount_cent), 0)   AS amount_cent,
                       COALESCE(SUM(vat_amount_cent), 0) AS vat_amount_cent,
                       COALESCE(SUM(cultural_fee_cent), 0) AS cultural_fee_cent
                  FROM settlement_invoice_items
                """ + LIST_FILTER + """
                   AND status NOT IN ('reversed','voided')
                 GROUP BY subject_entity_id, subject_name
                 ORDER BY subject_name NULLS LAST
                """;
        return jdbcTemplate.query(sql, filterParams(projectId, periodStart, null, null),
                (rs, i) -> new InvoiceSubjectSummary(
                        rs.getString("subject_entity_id"), rs.getString("subject_name"),
                        rs.getInt("item_count"), rs.getLong("amount_cent"),
                        rs.getLong("vat_amount_cent"), rs.getLong("cultural_fee_cent")));
    }

    /**
     * 已超开明细:同来源【已开额】(占额三态合计)> 结算单当期【应开额】。
     * 只会在结算单重跑(退款/冲正致金额下调)后出现;业务 HTML 要求「退款后先红冲再重开」。
     */
    public List<OverIssuedRow> listOverIssued(String projectId, LocalDate periodStart) {
        String sql = """
                SELECT s.bill_id, s.income_type, s.settlement_item_id,
                       MIN(s.subject_name) AS subject_name,
                       COALESCE(i.final_amount_cent, b.comp_service_fee_pooled_cent + b.comp_service_fee_exclusive_cent) AS due_amount_cent,
                       SUM(s.amount_cent) AS open_amount_cent
                  FROM settlement_invoice_items s
                  JOIN settlement_bills b ON b.id = s.bill_id
                  LEFT JOIN settlement_items i ON i.id = s.settlement_item_id
                 WHERE s.status IN ('pending','issuing','issued')
                   AND (CAST(:project_id AS VARCHAR) IS NULL OR s.project_id = CAST(:project_id AS VARCHAR))
                   AND (CAST(:period_start AS DATE) IS NULL OR s.period_start = CAST(:period_start AS DATE))
                 GROUP BY s.bill_id, s.income_type, s.settlement_item_id,
                          i.final_amount_cent, b.comp_service_fee_pooled_cent, b.comp_service_fee_exclusive_cent
                HAVING SUM(s.amount_cent) > COALESCE(i.final_amount_cent, b.comp_service_fee_pooled_cent + b.comp_service_fee_exclusive_cent)
                 ORDER BY s.bill_id, s.income_type
                """;
        var params = new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("period_start", periodStart);
        return jdbcTemplate.query(sql, params, (rs, i) -> new OverIssuedRow(
                rs.getString("bill_id"), rs.getString("income_type"), rs.getString("settlement_item_id"),
                rs.getString("subject_name"), rs.getLong("due_amount_cent"), rs.getLong("open_amount_cent")));
    }

    private static MapSqlParameterSource filterParams(String projectId, LocalDate periodStart,
            String subjectEntityId, String status) {
        return new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("period_start", periodStart)
                .addValue("subject_entity_id", subjectEntityId)
                .addValue("status", status);
    }

    // ------------------------------------------------------------------
    // 投影
    // ------------------------------------------------------------------

    /** 开票映射规则(invoice_subject_rules 一行)。 */
    public record InvoiceSubjectRule(String incomeType, String revenueStream, String invoiceItemName,
            String taxCategory, String evidenceRequired, boolean invoiceable) {
    }

    /** 生成清单的来源结算单头。 */
    public record InvoiceSource(String billId, String projectId, LocalDate periodStart, LocalDate periodEnd,
            String status, long compServiceFeeCent) {
    }

    /** 结算单下的一条酒吧分账(含该商户的收款主体)。 */
    public record BillMerchantShare(String settlementItemId, String merchantId, long finalAmountCent,
            String entityId, String merchantName) {
    }

    /** 清单条目只读视图。 */
    public record SettlementInvoiceItemView(String id, String billId, String settlementItemId, String projectId,
            LocalDate periodStart, LocalDate periodEnd, String incomeType, String subjectEntityId, String subjectName,
            String invoiceItemName, String taxCategory, String evidenceRequired, long amountCent, int vatRateBps,
            long vatAmountCent, boolean culturalFeeApplicable, int culturalFeeRateBps, long culturalFeeCent,
            String status, String invoiceNo, String remark,
            // FS-022-R1：积分发行费行的来源账单与购方（合同销售方）；结算单行为 null。
            String pointsBillId, String buyerEntityId, String buyerName) {
    }

    /** FS-022-R1：开票所需的积分月账单事实（收款方＝开票主体，合同销售方＝购方，已收额取应收应付台账）。 */
    public record PointsBillSource(String pointsBillId, String projectId, LocalDate billingMonth, String sellerEntityId,
            String sellerMerchantId, String receiverEntityId, long paidAmountCent) {
    }

    /** 按开票主体的分组汇总。 */
    public record InvoiceSubjectSummary(String subjectEntityId, String subjectName, int itemCount,
            long amountCent, long vatAmountCent, long culturalFeeCent) {
    }

    /** 已超开一行(已开额 > 应开额),须先红冲再重开。 */
    public record OverIssuedRow(String billId, String incomeType, String settlementItemId, String subjectName,
            long dueAmountCent, long openAmountCent) {
    }
}
