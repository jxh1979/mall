package com.complex.platform.finance;

// 文件版本：V0.03

import static com.complex.platform.common.Strings.requireText;

import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.money.MoneyRounding;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.evidence.EvidenceAttachmentValidator;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.complex.platform.settlement.JdbcSettlementInvoiceRepository;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 经营主体税费实际申报金额登记（第十八批 AC 段第 210 条 210-R2；第十八批裁决 42「财务按公司、税种、所属期手工登记「实际申报金额」，经财务、税务、
 * 法务审核并由顶层平台公司老板最终确认后生效」）。依据 BSM:1760「平台只接收专业财税软件回传的数据，或保存经授权人员手工填写并通过审批的数据」
 * 「所有税费规则和税费调整均须经过财务审核、税务审核、法务审核，再由顶层平台公司的老板最终确认后生效，提交人不得审批自己提交的调整」。
 *
 * <p>登记即提交审批（不设草稿）；审批照 PayrollTaxService.submitReview：财务、税务、法务三条真实部门起点＋复核动作 finance.tax_rule.adjust
 * （tax_rule_adjustment_l3，终点最高权限确认；提交人自审由复核链拦），结果由 {@link #decided} 回写，审批通过才生效。分红扣税（210-R3）只取已生效行。</p>
 *
 * <p>第十八批 AR 段第 233 条（V645）：233-R1 已生效申报登记实际缴纳金额（BSM:1760「缴税差异等于实际缴纳金额减实际申报金额」「实缴与申报不一致的部分先按
 * 未缴、预缴、多缴、抵扣或待退分别处理，不得直接混入税费成本」），实缴不进分红、不生成应付；233-R3 原期间已申报的更正只能在当前开放期间生成关联原期间的
 * 差异调整单（BSM:1760、ENUM:201），原申报不改，生效当月计入分红应扣税费。两者同走本复核链，{@link #decided} 按对象类型回写。</p>
 *
 * <p>233-R2（V646）：税率按主体 × 收入类型 × 税种在后台版本化维护（ENUM:201「每个主体 × 每类收入 × 每种税……后台各自配“有没有这笔费用 + 收多少”」；
 * BSM:521「必须填写生效起始日期，并先存为草案、经高敏权限“转正”审批通过后按指定日期生效」；BSM:1760「企业所得税等税种：系统内配置税率比例」），
 * 同走本复核链；其他税费不配比例（第十八批裁决 59）。开票清单按开票主体与业务日期取版本（JdbcSettlementInvoiceRepository.findTaxRate）。</p>
 *
 * <p>233-R5（V647）：增值税、文化事业建设费、附加税登记时按 233-R4 收入归集（{@link EntityIncomeService}）与当时生效的税率版本现算系统计算金额，
 * 依据逐月、逐收入类型落在申报行上（BSM:1760「税费金额分三个阶段分别保存、互不覆盖」；BSM:521「每一笔单据在生成时就把所用配置版本固化在单上」），
 * 批准时按申报差异生成补提或冲回调整单、差异保留不拉平；企业所得税与其他税费按季与财税软件核对、以财税软件为准拉平（BSM:1760「两套差异机制按税种
 * 互斥适用、不得叠加」；第十八批裁决 59「附加税跟流转税走补提或冲回调整单；其他税费和企业所得税一样，按季与财税软件核对、以财税软件为准拉平」）。</p>
 *
 * <p>233-R6：按主体汇总收入、成本与利润（{@link EntityProfitService}，第十八批裁决 60）。233-R7：企业所得税登记时系统计算＝max(0, 所属期利润)×企业所得税
 * 比例（裁决 60「企业所得税系统计算＝利润×配置比例」），复核摘要写按季核对；企业所得税与其他税费批准后不出补提或冲回调整单，本平台该税该期以申报额
 * （财税软件结果）为准拉平（BSM:1760「两套数据须核对，以财税软件的结果为准……最终保证两套一致。税务按季度处理」）。</p>
 */
@Service
public class EntityTaxService {

    static final String VIEW = "finance.entity_tax.view";
    static final String MANAGE = "finance.entity_tax.manage";
    static final String ACTION = "finance.tax_rule.adjust";
    static final String TYPE = "entity_tax_declaration";
    static final String PAYMENT = "entity_tax_payment";
    static final String ADJUSTMENT = "entity_tax_adjustment";
    static final String RATE = "entity_tax_rate";
    /** 233-R2 ENUM:201「每类收入」（同前端 KEY_VALUE_LABELS.income_type，开票清单页同码同名）：V223 种子各类＋goods 供货货款、parking 停车费、retail 零售
     * （BSM:1722、1723 分行列出；组合通票业态份额餐饮、零售分列）；all 只用于企业所得税。 */
    static final Map<String, String> INCOME_LABELS = Map.ofEntries(Map.entry("ticket", "平台通票 / 门票综合服务费"),
            Map.entry("spv_service", "通票可分配池分账"), Map.entry("nonstd_service", "非散台服务费"),
            Map.entry("catering", "订台低消 / 二次消费 / 额外酒水"), Map.entry("retail", "零售"), Map.entry("game", "游乐收入"),
            Map.entry("show", "演出票"), Map.entry("rent", "商业租赁 / 市集摊位"), Map.entry("parking", "停车费"),
            Map.entry("points_issuance", "积分发行费"), Map.entry("goods", "供货货款"), Map.entry("all", "全部收入"));
    private static final String RATE_SELECT = "SELECT c.id, c.business_entity_id, e.entity_name, c.income_type, c.tax_type, c.applicable, c.rate_bps,"
            + " c.effective_from, c.reason, CASE WHEN c.status = 'inactive' THEN 'rejected' WHEN c.is_draft THEN 'pending_review' ELSE 'approved' END"
            + " AS review_status, c.review_id FROM settlement_tax_configs c LEFT JOIN business_entities e ON e.id = c.business_entity_id";
    private static final Map<String, String> TABLES = Map.of(TYPE, "entity_tax_declarations", PAYMENT, "entity_tax_payments",
            ADJUSTMENT, "entity_tax_adjustments");
    /** BSM:1760「实缴与申报不一致的部分先按未缴、预缴、多缴、抵扣或待退分别处理」。 */
    static final Map<String, String> CATEGORY_LABELS = Map.of("unpaid", "未缴", "prepaid", "预缴", "overpaid", "多缴", "offset", "抵扣",
            "refund_pending", "待退");
    public static final Map<String, String> TAX_LABELS = Map.of("vat", "增值税", "cultural_fee", "文化事业建设费",
            "surcharge", "附加税", "corporate_income", "企业所得税", "other", "其他税费");
    private static final Set<String> STATUSES = Set.of("pending_review", "approved", "rejected");
    /** 233-R5 按申报差异出补提或冲回调整单的税种（第十八批裁决 59）；其余（企业所得税、其他税费）按季与财税软件核对拉平。 */
    static final Set<String> ADJUSTMENT_TAXES = Set.of("vat", "cultural_fee", "surcharge");
    private static final Pattern MONTH = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");
    private static final String ADJUSTMENT_JSON = "json_build_object('id', a.id, 'declaration_id', a.declaration_id, 'kind', a.kind,"
            + " 'amount_cent', a.amount_cent, 'origin_period_start_month', a.origin_period_start_month, 'origin_period_end_month',"
            + " a.origin_period_end_month, 'accounting_month', a.accounting_month, 'reason', a.reason, 'status', a.status, 'review_id', a.review_id)";
    // 233-R1 当前实际缴纳金额取最近一条已生效登记；233-R3 下挂全部差异调整单。
    private static final String SELECT = "SELECT d.*, e.entity_name, p.paid_amount_cent, p.difference_cent AS payment_difference_cent,"
            + " p.difference_breakdown AS payment_breakdown,"
            + " EXISTS (SELECT 1 FROM entity_tax_payments q WHERE q.declaration_id = d.id AND q.status = 'pending_review') AS payment_pending,"
            + " (SELECT COALESCE(json_agg(" + ADJUSTMENT_JSON + " ORDER BY a.created_at, a.id), '[]'::json) FROM entity_tax_adjustments a"
            + " WHERE a.declaration_id = d.id) AS adjustments"
            + " FROM entity_tax_declarations d LEFT JOIN business_entities e ON e.id = d.business_entity_id"
            + " LEFT JOIN LATERAL (SELECT x.paid_amount_cent, x.difference_cent, x.difference_breakdown FROM entity_tax_payments x"
            + " WHERE x.declaration_id = d.id AND x.status = 'approved' ORDER BY x.approved_at DESC, x.id DESC LIMIT 1) p ON TRUE";

    private final JdbcTemplate jdbc;
    private final AccessRepository access;
    private final AuthorizationService auth;
    private final SupervisorReviewService reviews;
    private final AuditedOperationExecutor operations;
    private final EvidenceAttachmentValidator attachments;
    private final ObjectMapper json;
    private final JdbcSettlementInvoiceRepository rates;
    private final EntityIncomeService incomes;
    private final EntityProfitService profits;

    public EntityTaxService(JdbcTemplate jdbc, AccessRepository access, AuthorizationService auth, SupervisorReviewService reviews,
            AuditedOperationExecutor operations, EvidenceAttachmentValidator attachments, ObjectMapper json, JdbcSettlementInvoiceRepository rates,
            EntityIncomeService incomes, EntityProfitService profits) {
        this.jdbc = jdbc;
        this.access = access;
        this.auth = auth;
        this.reviews = reviews;
        this.operations = operations;
        this.attachments = attachments;
        this.json = json.copy().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        this.rates = rates;
        this.incomes = incomes;
        this.profits = profits;
    }

    /** 列表：按经营主体、所属月（所属期覆盖该月）、税种、状态筛选，分页。平台口径（商户账号拒）。 */
    public PageResponse<EntityTaxDeclaration> list(String entityId, String month, String taxType, String status, int page, int pageSize) {
        return operations.readPageResult(AuditedOperation.of("entity_tax.listed", "entity_tax.list_failed", VIEW, TYPE), () -> {
            auth.requirePermission(VIEW);
            auth.requirePlatformScope();
            String entity = blank(entityId);
            String m = blank(month) == null ? null : requireMonth(month, "month");
            String tax = blank(taxType) == null ? null : requireTaxType(taxType);
            String st = blank(status);
            if (st != null && !STATUSES.contains(st)) throw invalid("无法识别的登记状态：" + st);
            String where = " WHERE (?::text IS NULL OR d.business_entity_id = ?)"
                    + " AND (?::text IS NULL OR (d.period_start_month <= ? AND d.period_end_month >= ?))"
                    + " AND (?::text IS NULL OR d.tax_type = ?) AND (?::text IS NULL OR d.status = ?)";
            Object[] args = {entity, entity, m, m, m, tax, tax, st, st};
            int size = Math.min(Math.max(pageSize, 1), 200);
            int safePage = Math.max(page, 1);
            long total = jdbc.queryForObject("SELECT count(*) FROM entity_tax_declarations d" + where, Long.class, args);
            List<Object> pageArgs = new ArrayList<>(Arrays.asList(args));
            pageArgs.add(size);
            pageArgs.add((safePage - 1) * size);
            var items = jdbc.query(SELECT + where + " ORDER BY d.period_end_month DESC, d.created_at DESC, d.id LIMIT ? OFFSET ?",
                    this::row, pageArgs.toArray());
            return new PageResponse<>(items, safePage, size, total);
        });
    }

    /** 登记并提交审批（一步）。同主体、同税种、同所属期已有待审批或已生效登记的 409，驳回后可重登。 */
    @Transactional
    public EntityTaxDeclaration declare(EntityTaxDeclaration.Request r) {
        auth.requirePermission(MANAGE);
        auth.requirePlatformScope();
        if (r == null) throw invalid("请求内容不能为空");
        return operations.executeWrite("declareEntityTax", r.idempotencyKey(), r, EntityTaxDeclaration.class,
                AuditedOperation.of("entity_tax.declared", "entity_tax.declare_failed", MANAGE, TYPE), () -> {
                    String entity = requireText(r.businessEntityId(), "business_entity_id");
                    String taxType = requireTaxType(requireText(r.taxType(), "tax_type"));
                    String start = requireMonth(r.periodStartMonth(), "period_start_month");
                    String end = requireMonth(r.periodEndMonth(), "period_end_month");
                    if (end.compareTo(start) < 0) throw invalid("所属期末月不能早于首月");
                    if (r.declaredAmountCent() == null || r.declaredAmountCent() < 0) throw invalid("实际申报金额须填写，且不能为负数");
                    String reason = requireText(r.reason(), "reason");
                    List<String> evidence = r.evidenceAttachmentIds() == null ? List.of() : List.copyOf(r.evidenceAttachmentIds());
                    attachments.requireExisting(evidence, "evidence_attachment_ids");
                    String entityName = jdbc.query("SELECT entity_name FROM business_entities WHERE id = ?", (rs, n) -> rs.getString(1), entity)
                            .stream().findFirst().orElseThrow(() -> ApiException.notFound("BUSINESS_ENTITY_NOT_FOUND", "经营主体不存在：" + entity));
                    jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", rs -> { },
                            "entity-tax:" + entity + ":" + taxType + ":" + start + ":" + end);
                    if (jdbc.queryForObject("SELECT count(*) FROM entity_tax_declarations WHERE business_entity_id = ? AND tax_type = ?"
                            + " AND period_start_month = ? AND period_end_month = ? AND status IN ('pending_review', 'approved')",
                            Integer.class, entity, taxType, start, end) > 0) {
                        throw ApiException.conflict("ENTITY_TAX_DECLARATION_DUPLICATE",
                                "该经营主体同一税种、同一所属期已有待审批或已生效的申报登记，驳回后才能重新登记");
                    }
                    String id = "etd_" + UUID.randomUUID().toString().replace("-", "");
                    long cent = r.declaredAmountCent();
                    // 233-R5：提交审批前现算系统计算金额（收入或分账未定 409、不落行），随登记落库、之后不改。
                    Calculation calc = calculate(entity, taxType, start, end);
                    String summary = entityName + " " + TAX_LABELS.get(taxType) + " 所属期 " + start + "～" + end + " 实际申报金额 "
                            + yuan(cent) + " 元（新登记，生效后计入该期分红应扣税费）" + (calc == null ? "" : calc.summary(cent, taxType));
                    var review = submitReview(r.idempotencyKey(), r.evidenceAttachmentIds(), r.parallelLines(), TYPE, id, reason, summary);
                    jdbc.update("INSERT INTO entity_tax_declarations(id, business_entity_id, tax_type, period_start_month, period_end_month,"
                                    + " declared_amount_cent, system_calculated_amount_cent, calculation_basis, reason, evidence_attachment_ids, status,"
                                    + " review_id, created_by, created_at)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?::jsonb, 'pending_review', ?, ?, clock_timestamp())",
                            id, entity, taxType, start, end, cent, calc == null ? null : calc.amountCent(), calc == null ? null : encode(calc.basis()),
                            reason, encode(evidence), review.reviewId(), auth.currentActor().id());
                    return find(id);
                }, saved -> ((EntityTaxDeclaration) saved).id(), Map.of());
    }

    /**
     * 233-R1 已生效申报登记实际缴纳金额并提交审批。缴税差异＝实际缴纳−更正后的实际申报金额（申报额＋已生效差异调整单），差额不为 0 时按五类分配、
     * 合计等于差额绝对值；同一申报至多一条待审批，批准后可再登记（如未缴部分后来补缴）。实缴不进分红、不生成应付（BSM:1760「不得直接混入税费成本」）。
     */
    @Transactional
    public EntityTaxDeclaration.Payment registerPayment(String declarationId, EntityTaxDeclaration.PaymentRequest r) {
        auth.requirePermission(MANAGE);
        auth.requirePlatformScope();
        if (r == null) throw invalid("请求内容不能为空");
        return operations.executeWrite("registerEntityTaxPayment", r.idempotencyKey(), r, EntityTaxDeclaration.Payment.class,
                AuditedOperation.of("entity_tax.payment_registered", "entity_tax.payment_register_failed", MANAGE, PAYMENT), () -> {
                    EntityTaxDeclaration d = lockApproved(declarationId);
                    if (jdbc.queryForObject("SELECT count(*) FROM entity_tax_payments WHERE declaration_id = ? AND status = 'pending_review'",
                            Integer.class, d.id()) > 0) {
                        throw ApiException.conflict("ENTITY_TAX_PAYMENT_PENDING", "该申报已有待审批的实际缴纳登记，审批完成后才能再登记");
                    }
                    if (r.paidAmountCent() == null || r.paidAmountCent() < 0) throw invalid("实际缴纳金额须填写，且不能为负数");
                    long declared = correctedDeclared(d);
                    long paid = r.paidAmountCent();
                    long difference = paid - declared;
                    List<EntityTaxDeclaration.Breakdown> breakdown = requireBreakdown(r.differenceBreakdown(), difference);
                    String reason = requireText(r.reason(), "reason");
                    List<String> evidence = r.evidenceAttachmentIds() == null ? List.of() : List.copyOf(r.evidenceAttachmentIds());
                    attachments.requireExisting(evidence, "evidence_attachment_ids");
                    String id = "etp_" + UUID.randomUUID().toString().replace("-", "");
                    String summary = d.entityName() + " " + TAX_LABELS.get(d.taxType()) + " 所属期 " + d.periodStartMonth() + "～" + d.periodEndMonth()
                            + " 实际申报 " + yuan(declared) + " 元，本次登记实际缴纳 " + yuan(paid) + " 元，缴税差异 " + yuan(difference) + " 元"
                            + (breakdown.isEmpty() ? "" : breakdown.stream().map(b -> CATEGORY_LABELS.get(b.category()) + " " + yuan(b.amountCent()) + " 元")
                                    .collect(Collectors.joining("、", "（", "）")))
                            + "，不计入税费成本";
                    var review = submitReview(r.idempotencyKey(), evidence, r.parallelLines(), PAYMENT, id, reason, summary);
                    jdbc.update("INSERT INTO entity_tax_payments(id, declaration_id, business_entity_id, tax_type, paid_amount_cent, difference_cent,"
                                    + " difference_breakdown, reason, evidence_attachment_ids, status, review_id, created_by, created_at)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?::jsonb, 'pending_review', ?, ?, clock_timestamp())",
                            id, d.id(), d.businessEntityId(), d.taxType(), paid, difference, encode(breakdown), reason, encode(evidence),
                            review.reviewId(), auth.currentActor().id());
                    return jdbc.query("SELECT * FROM entity_tax_payments WHERE id = ?", (rs, n) -> new EntityTaxDeclaration.Payment(rs.getString("id"),
                            rs.getString("declaration_id"), rs.getLong("paid_amount_cent"), rs.getLong("difference_cent"),
                            list(rs.getString("difference_breakdown"), EntityTaxDeclaration.Breakdown.class), rs.getString("reason"),
                            rs.getString("status"), rs.getString("review_id")), id).getFirst();
                }, saved -> ((EntityTaxDeclaration.Payment) saved).id(), Map.of());
    }

    /**
     * 233-R3 原期间已生效申报的更正：在当前开放期间生成关联原期间的差异调整单并提交审批，原申报行一律不改（BSM:1760、ENUM:201）。生效时记入批准当日
     * 北京时间所在月（批准当月不可能已封账、该月正式分红单也还没出），计入该月所在期分红应扣税费。
     */
    @Transactional
    public EntityTaxDeclaration.Adjustment registerPeriodDifference(String declarationId, EntityTaxDeclaration.AdjustmentRequest r) {
        auth.requirePermission(MANAGE);
        auth.requirePlatformScope();
        if (r == null) throw invalid("请求内容不能为空");
        return operations.executeWrite("registerEntityTaxAdjustment", r.idempotencyKey(), r, EntityTaxDeclaration.Adjustment.class,
                AuditedOperation.of("entity_tax.adjustment_registered", "entity_tax.adjustment_register_failed", MANAGE, ADJUSTMENT), () -> {
                    EntityTaxDeclaration d = lockApproved(declarationId);
                    if (r.amountCent() == null || r.amountCent() == 0) throw invalid("更正差额须填写且不能为 0（多申报的填负数）");
                    long amount = r.amountCent();
                    String reason = requireText(r.reason(), "reason");
                    List<String> evidence = r.evidenceAttachmentIds() == null ? List.of() : List.copyOf(r.evidenceAttachmentIds());
                    attachments.requireExisting(evidence, "evidence_attachment_ids");
                    String id = "eta_" + UUID.randomUUID().toString().replace("-", "");
                    String summary = d.entityName() + " " + TAX_LABELS.get(d.taxType()) + " 原所属期 " + d.periodStartMonth() + "～" + d.periodEndMonth()
                            + " 已生效申报 " + yuan(d.declaredAmountCent()) + " 元，更正差额 " + (amount > 0 ? "+" : "") + yuan(amount)
                            + " 元，生效当月计入（差异调整单，原申报不改），生效后计入该月所在期分红应扣税费";
                    var review = submitReview(r.idempotencyKey(), evidence, r.parallelLines(), ADJUSTMENT, id, reason, summary);
                    jdbc.update("INSERT INTO entity_tax_adjustments(id, declaration_id, business_entity_id, tax_type, origin_period_start_month,"
                                    + " origin_period_end_month, kind, amount_cent, reason, evidence_attachment_ids, status, review_id, created_by, created_at)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, 'period_difference', ?, ?, ?::jsonb, 'pending_review', ?, ?, clock_timestamp())",
                            id, d.id(), d.businessEntityId(), d.taxType(), d.periodStartMonth(), d.periodEndMonth(), amount, reason, encode(evidence),
                            review.reviewId(), auth.currentActor().id());
                    return read(jdbc.queryForObject("SELECT " + ADJUSTMENT_JSON + " FROM entity_tax_adjustments a WHERE a.id = ?", String.class, id),
                            json.constructType(EntityTaxDeclaration.Adjustment.class));
                }, saved -> ((EntityTaxDeclaration.Adjustment) saved).id(), Map.of());
    }

    /** 233-R6 主体所属期收入、成本与利润（三个参数必填）。平台口径（商户账号拒）；收入或分账未定 409。 */
    public EntityProfitService.Profit profit(String entityId, String startMonth, String endMonth) {
        return operations.readSingle(AuditedOperation.of("entity_tax.profit_viewed", "entity_tax.profit_view_failed", VIEW, TYPE), () -> {
            auth.requirePermission(VIEW);
            auth.requirePlatformScope();
            String entity = requireText(entityId, "business_entity_id");
            String start = requireMonth(startMonth, "start_month");
            String end = requireMonth(endMonth, "end_month");
            if (end.compareTo(start) < 0) throw invalid("截止月份不能早于起始月份");
            return profits.profit(entity, YearMonth.parse(start), YearMonth.parse(end));
        }, result -> entityId);
    }

    /** 233-R2 税率版本列表：全部版本（含待审与驳回），平台默认在前，带主体名。平台口径（商户账号拒）。 */
    public List<EntityTaxDeclaration.Rate> listRates() {
        return operations.readSingle(AuditedOperation.of("entity_tax.rates_listed", "entity_tax.rates_list_failed", VIEW, RATE), () -> {
            auth.requirePermission(VIEW);
            auth.requirePlatformScope();
            return jdbc.query(RATE_SELECT + " ORDER BY c.business_entity_id IS NOT NULL, e.entity_name, c.business_entity_id, c.income_type, c.tax_type,"
                    + " c.effective_from DESC NULLS LAST, c.id", this::rateRow);
        }, rows -> "all");
    }

    /**
     * 233-R2 提交税率版本：存为草案（is_draft=TRUE）并提交财税法审核与最高确认，批准后按生效日起用、驳回置 inactive 留痕（{@link #decided}）。
     * 增值税、文化事业建设费、附加税按收入类型分别配；企业所得税只配全部收入（all）；其他税费没有系统计算基数，不配比例（第十八批裁决 59）。
     * 同主体、同收入类型、同税种、同生效日已有待审或已生效版本 409。不设「生效日须晚于今日」（照 PayrollTaxService.draft）。
     */
    @Transactional
    public EntityTaxDeclaration.Rate submitRate(EntityTaxDeclaration.RateRequest r) {
        auth.requirePermission(MANAGE);
        auth.requirePlatformScope();
        if (r == null) throw invalid("请求内容不能为空");
        return operations.executeWrite("submitEntityTaxRate", r.idempotencyKey(), r, EntityTaxDeclaration.Rate.class,
                AuditedOperation.of("entity_tax.rate_submitted", "entity_tax.rate_submit_failed", MANAGE, RATE), () -> {
                    String taxType = requireTaxType(requireText(r.taxType(), "tax_type"));
                    if ("other".equals(taxType)) {
                        throw invalid("其他税费没有系统计算基数，不配比例：按季与财税软件核对、以财税软件为准拉平");
                    }
                    String incomeType = requireText(r.incomeType(), "income_type").trim();
                    if ("corporate_income".equals(taxType) ? !"all".equals(incomeType) : ("all".equals(incomeType) || !INCOME_LABELS.containsKey(incomeType))) {
                        throw invalid("企业所得税按全部收入配一个比例；增值税、文化事业建设费、附加税按收入类型分别配：" + incomeType);
                    }
                    if (r.applicable() == null) throw invalid("请选择有没有这笔费用");
                    if (r.rateBps() == null || r.rateBps() < 0 || r.rateBps() > 10000) throw invalid("比例须在 0%～100% 之间");
                    if (r.effectiveFrom() == null) throw invalid("须填写生效起始日期");
                    String reason = requireText(r.reason(), "reason");
                    List<String> evidence = r.evidenceAttachmentIds() == null ? List.of() : List.copyOf(r.evidenceAttachmentIds());
                    attachments.requireExisting(evidence, "evidence_attachment_ids");
                    String entity = blank(r.businessEntityId());
                    String entityName = entity == null ? "平台默认" : jdbc.query("SELECT entity_name FROM business_entities WHERE id = ?",
                            (rs, n) -> rs.getString(1), entity).stream().findFirst()
                            .orElseThrow(() -> ApiException.notFound("BUSINESS_ENTITY_NOT_FOUND", "经营主体不存在：" + entity));
                    LocalDate from = r.effectiveFrom();
                    jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", rs -> { },
                            "entity-tax-rate:" + (entity == null ? "__DEFAULT__" : entity) + ":" + incomeType + ":" + taxType);
                    if (jdbc.queryForObject("SELECT count(*) FROM settlement_tax_configs WHERE COALESCE(business_entity_id, '__DEFAULT__')"
                            + " = COALESCE(?::text, '__DEFAULT__') AND income_type = ? AND tax_type = ? AND effective_from = ? AND status = 'active'",
                            Integer.class, entity, incomeType, taxType, from) > 0) {
                        throw ApiException.conflict("TAX_RATE_VERSION_CONFLICT",
                                "该主体同一收入类型、同一税种在同一生效日已有待审批或已生效的税率版本，驳回后才能重新提交");
                    }
                    var before = rates.findTaxRateVersion(entity, incomeType, taxType, from);
                    String id = "stc_" + UUID.randomUUID().toString().replace("-", "");
                    String summary = entityName + " " + INCOME_LABELS.get(incomeType) + " " + TAX_LABELS.get(taxType) + "：原生效版本 "
                            + (before == null ? "未配置" : rateText(before.applicable(), before.rateBps())
                                    + (before.effectiveFrom() == null ? "（自始有效）" : "（" + before.effectiveFrom() + " 起）"))
                            + " → 新版本 " + rateText(r.applicable(), r.rateBps()) + "，" + from + " 起生效（此前发生的业务仍按原版本）";
                    var review = submitReview(r.idempotencyKey(), evidence, r.parallelLines(), RATE, id, reason, summary);
                    jdbc.update("INSERT INTO settlement_tax_configs(id, business_entity_id, income_type, tax_type, applicable, rate_bps, effective_from,"
                                    + " status, is_draft, reason, evidence_attachment_ids, review_id, created_by, updated_by, updated_at)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, 'active', TRUE, ?, ?::jsonb, ?, ?, ?, clock_timestamp())",
                            id, entity, incomeType, taxType, r.applicable(), r.rateBps(), from, reason, encode(evidence), review.reviewId(),
                            auth.currentActor().id(), auth.currentActor().id());
                    return jdbc.query(RATE_SELECT + " WHERE c.id = ?", this::rateRow, id).getFirst();
                }, saved -> ((EntityTaxDeclaration.Rate) saved).id(), Map.of());
    }

    /**
     * 复核终态回写：只认对应表待审批行、复核单号一致且复核表里确有该终态（伪造或重放的回调不改行）。差异调整单批准时同一条 UPDATE 写入计入月份
     * （批准当日北京时间所在月，233-R3）。233-R5：增值税、文化事业建设费、附加税申报批准且确实改到 1 行时，同事务按申报差异（申报−系统计算）生成
     * 补提（高于）或冲回（低于）调整单，计入批准当月、直接生效（随申报同一复核单），差额为 0 或驳回不出单；补提、冲回调整单不进分红。
     */
    @EventListener
    @Transactional
    public void decided(SupervisorReviewDecisionRecord d) {
        if (RATE.equals(d.objectType()) && ACTION.equals(d.actionType()) && Set.of("approved", "rejected").contains(d.reviewStatus())) {
            // 233-R2 税率版本：批准即转正（is_draft=FALSE，按生效日起用）；驳回置 inactive，草案留痕。
            jdbc.update("UPDATE settlement_tax_configs SET is_draft = (?::text <> 'approved'),"
                            + " status = CASE WHEN ?::text = 'approved' THEN 'active' ELSE 'inactive' END, updated_at = clock_timestamp()"
                            + " WHERE id = ? AND is_draft AND status = 'active' AND review_id = ? AND EXISTS (SELECT 1 FROM supervisor_review_decisions"
                            + " WHERE review_id = ? AND action_type = ? AND object_type = ? AND object_id = ? AND review_status = ?)",
                    d.reviewStatus(), d.reviewStatus(), d.objectId(), d.reviewId(), d.reviewId(), ACTION, d.objectType(), d.objectId(), d.reviewStatus());
            return;
        }
        String table = TABLES.get(d.objectType());
        if (!ACTION.equals(d.actionType()) || table == null || !Set.of("approved", "rejected").contains(d.reviewStatus())) {
            return;
        }
        List<Object> args = new ArrayList<>(List.of(d.reviewStatus(), d.reviewStatus()));
        String month = "";
        if (ADJUSTMENT.equals(d.objectType())) {
            month = ", accounting_month = CASE WHEN ?::text = 'approved' THEN ? END";
            args.addAll(List.of(d.reviewStatus(), YearMonth.now(LocalZone.ID).toString()));
        }
        args.addAll(List.of(d.objectId(), d.reviewId(), d.reviewId(), ACTION, d.objectType(), d.objectId(), d.reviewStatus()));
        int changed = jdbc.update("UPDATE " + table + " SET status = ?, approved_at = CASE WHEN ?::text = 'approved' THEN clock_timestamp() END" + month
                        + " WHERE id = ? AND status = 'pending_review' AND review_id = ? AND EXISTS (SELECT 1 FROM supervisor_review_decisions"
                        + " WHERE review_id = ? AND action_type = ? AND object_type = ? AND object_id = ? AND review_status = ?)",
                args.toArray());
        if (changed == 1 && TYPE.equals(d.objectType()) && "approved".equals(d.reviewStatus())) {
            jdbc.update("INSERT INTO entity_tax_adjustments(id, declaration_id, business_entity_id, tax_type, origin_period_start_month,"
                            + " origin_period_end_month, kind, amount_cent, accounting_month, reason, status, review_id, created_by, created_at, approved_at)"
                            + " SELECT ?, x.id, x.business_entity_id, x.tax_type, x.period_start_month, x.period_end_month,"
                            + " CASE WHEN x.declared_amount_cent > x.system_calculated_amount_cent THEN 'supplement' ELSE 'reversal' END,"
                            + " abs(x.declared_amount_cent - x.system_calculated_amount_cent), ?,"
                            + " CASE WHEN x.declared_amount_cent > x.system_calculated_amount_cent THEN '实际申报金额高于系统计算金额，生成补提调整单'"
                            + " ELSE '实际申报金额低于系统计算金额，生成冲回调整单' END, 'approved', x.review_id, x.created_by, clock_timestamp(),"
                            + " clock_timestamp() FROM entity_tax_declarations x WHERE x.id = ? AND x.tax_type IN ('vat', 'cultural_fee', 'surcharge')"
                            + " AND x.declared_amount_cent <> x.system_calculated_amount_cent",
                    "eta_" + UUID.randomUUID().toString().replace("-", ""), YearMonth.now(LocalZone.ID).toString(), d.objectId());
        }
    }

    /**
     * 233-R5 系统计算金额：所属期每月、233-R4 每类收入 a，按主体在该月末日当时生效的税率版本（233-R2 findTaxRate）算——增值税 prorate(a, r, 10000+r)、
     * 文化事业建设费 bpsOf(a, r)（同开票清单 SettlementInvoiceService.tax），附加税以同期系统计算增值税为基数 bpsOf(增值税, r)（BSM:1760「计税基数……
     * 按照业务发生时有效的国家财税法律法规」）；没配或「没有这笔费用」记 0。233-R7 企业所得税＝max(0, 所属期利润)×主体在所属期末月月末日生效的企业所得税
     * 比例（收入类型 all），没配记 0。其他税费没有系统计算基数（第十八批裁决 59）返回 null。
     */
    private Calculation calculate(String entity, String taxType, String start, String end) {
        if ("corporate_income".equals(taxType)) {
            var profit = profits.profit(entity, YearMonth.parse(start), YearMonth.parse(end));
            var rate = rates.findTaxRate(entity, "all", taxType, YearMonth.parse(end).atEndOfMonth());
            long tax = rate == null ? 0L : MoneyRounding.bpsOf(Math.max(0L, profit.profitCent()), rate.rateBps());
            return new Calculation(tax, List.of(new EntityTaxDeclaration.CalculationBasis(end, "all", profit.revenueCent(),
                    rate == null ? 0 : rate.rateBps(), rate == null ? null : rate.id(), tax, null, null, null, profit)));
        }
        if (!ADJUSTMENT_TAXES.contains(taxType)) return null;
        boolean surcharge = "surcharge".equals(taxType);
        List<EntityTaxDeclaration.CalculationBasis> basis = new ArrayList<>();
        long total = 0L;
        for (YearMonth m = YearMonth.parse(start); !m.isAfter(YearMonth.parse(end)); m = m.plusMonths(1)) {
            LocalDate date = m.atEndOfMonth();
            Map<String, Long> byType = new TreeMap<>();
            for (var item : incomes.income(entity, m)) byType.merge(item.incomeType(), item.amountCent(), Math::addExact);
            for (var e : byType.entrySet()) {
                long income = e.getValue();
                if (income == 0) continue;
                var vat = rates.findTaxRate(entity, e.getKey(), "vat", date);
                long vatCent = vat == null ? 0L : MoneyRounding.prorate(income, vat.rateBps(), MoneyRounding.BPS_DENOMINATOR + vat.rateBps());
                var rate = "vat".equals(taxType) ? vat : rates.findTaxRate(entity, e.getKey(), taxType, date);
                long tax = "vat".equals(taxType) ? vatCent : rate == null ? 0L : MoneyRounding.bpsOf(surcharge ? vatCent : income, rate.rateBps());
                basis.add(new EntityTaxDeclaration.CalculationBasis(m.toString(), e.getKey(), income, rate == null ? 0 : rate.rateBps(),
                        rate == null ? null : rate.id(), tax, surcharge ? vatCent : null, surcharge ? (vat == null ? 0 : vat.rateBps()) : null,
                        surcharge && vat != null ? vat.id() : null, null));
                total = Math.addExact(total, tax);
            }
        }
        return new Calculation(total, basis);
    }

    /**
     * 233-R5 系统计算结果；summary 为复核摘要追加句（BSM:1760「申报差异等于实际申报金额减系统计算金额」）。按季核对拉平的税种另写核对句（233-R7，BSM:1760
     * 「以财税软件的结果为准，出现误差的须查明原因」，差异原因即必填的登记理由）。
     */
    private record Calculation(long amountCent, List<EntityTaxDeclaration.CalculationBasis> basis) {
        String summary(long declaredCent, String taxType) {
            long difference = declaredCent - amountCent;
            if (!ADJUSTMENT_TAXES.contains(taxType)) {
                return "；按季核对：系统计算 " + yuan(amountCent) + " 元，财税软件结果（本次申报）" + yuan(declaredCent) + " 元，差异 " + yuan(difference)
                        + " 元，以财税软件结果为准拉平；差异原因见登记理由";
            }
            return "；系统计算金额 " + yuan(amountCent) + " 元，申报差异 " + yuan(difference) + " 元（申报−系统计算）"
                    + (difference > 0 ? "，生效后生成补提调整单 " + yuan(difference) + " 元"
                            : difference < 0 ? "，生效后生成冲回调整单 " + yuan(-difference) + " 元" : "，与系统计算一致，不生成调整单");
        }
    }

    /** 申报行加锁后取出；只有已生效的申报才能登记实缴或生成差异调整单。 */
    private EntityTaxDeclaration lockApproved(String id) {
        jdbc.query("SELECT 1 FROM entity_tax_declarations WHERE id = ? FOR UPDATE", rs -> { }, id);
        EntityTaxDeclaration d = find(id);
        if (!"approved".equals(d.status())) {
            throw ApiException.conflict("ENTITY_TAX_DECLARATION_NOT_APPROVED", "只有已生效的申报登记才能登记实际缴纳或生成差异调整单");
        }
        return d;
    }

    /** 更正后的实际申报金额＝申报额＋已生效 period_difference 差异调整单合计（233-R1 缴税差异的基数）。 */
    private static long correctedDeclared(EntityTaxDeclaration d) {
        return correctedDeclared(d.declaredAmountCent(), d.adjustments());
    }

    private static long correctedDeclared(long declared, List<EntityTaxDeclaration.Adjustment> adjustments) {
        return declared + adjustments.stream()
                .filter(a -> "period_difference".equals(a.kind()) && "approved".equals(a.status()))
                .mapToLong(EntityTaxDeclaration.Adjustment::amountCent).sum();
    }

    /** 差额不为 0 时按五类分配：每类至多一行、金额大于 0、合计等于差额绝对值；差额为 0 时不填。不设类别与差额方向的对应（HTML 没写）。 */
    private static List<EntityTaxDeclaration.Breakdown> requireBreakdown(List<EntityTaxDeclaration.Breakdown> items, long difference) {
        List<EntityTaxDeclaration.Breakdown> list = items == null ? List.of() : List.copyOf(items);
        if (difference == 0) {
            if (!list.isEmpty()) throw invalid("实际缴纳金额与实际申报金额一致，没有缴税差异，不用填写差异分类");
            return list;
        }
        long total = 0;
        Set<String> seen = new HashSet<>();
        for (EntityTaxDeclaration.Breakdown b : list) {
            if (b == null || b.category() == null || !CATEGORY_LABELS.containsKey(b.category())) {
                throw invalid("缴税差异分类只能是未缴、预缴、多缴、抵扣或待退");
            }
            if (!seen.add(b.category())) throw invalid("同一缴税差异分类只能填写一行：" + CATEGORY_LABELS.get(b.category()));
            if (b.amountCent() == null || b.amountCent() <= 0) throw invalid("缴税差异分类金额须大于 0：" + CATEGORY_LABELS.get(b.category()));
            total += b.amountCent();
        }
        if (total != Math.abs(difference)) {
            throw invalid("缴税差异 " + yuan(difference) + " 元（实际缴纳减实际申报）须按未缴、预缴、多缴、抵扣或待退分配，各类合计须等于 "
                    + yuan(Math.abs(difference)) + " 元");
        }
        return list;
    }

    /**
     * 照 PayrollTaxService.submitReview：三条线须配真实部门起点（在用、具正式审批角色），再进 finance.tax_rule.adjust 复核链。
     * 第十八批 AX 段第 271 条 271-R1 起分红代扣税率版本（DividendTaxRateService）同用本链。
     */
    public SupervisorReviewDecisionRecord submitReview(String idempotencyKey, List<String> evidenceIds,
            List<SupervisorReviewEvaluateRequest.ParallelLine> parallelLines, String objectType,
            String id, String reason, String summary) {
        for (String code : List.of("finance_review", "tax_review", "legal_review")) {
            var entry = jdbc.query("SELECT entry_user_id FROM approval_lines WHERE line_code = ? AND status = 'active'", (rs, n) -> rs.getString(1), code);
            if (entry.isEmpty() || entry.getFirst() == null || entry.getFirst().isBlank()) {
                throw ApiException.conflict("TAX_DEPARTMENT_ROUTING_REQUIRED", "须先配置财务、税务、法务真实部门审批起点：" + code);
            }
            var account = access.findUserById(entry.getFirst()).orElse(null);
            if (account == null || !"active".equals(account.status()) || !auth.holdsPermissionViaRole(account.id(), "governance.supervisor_review.approve")) {
                throw ApiException.conflict("TAX_DEPARTMENT_ROUTING_REQUIRED", "部门起点须为在用且具有正式审批角色的人员：" + code);
            }
        }
        var review = reviews.evaluate(new SupervisorReviewEvaluateRequest(idempotencyKey + ":review", reason, evidenceIds,
                ACTION, objectType, id, 0L, summary, null, parallelLines, null));
        if (!"pending_review".equals(review.reviewStatus())) {
            throw ApiException.conflict("TAX_APPROVAL_REQUIRED", "税费事项必须进入财税法和最高确认审批");
        }
        return review;
    }

    private EntityTaxDeclaration find(String id) {
        return jdbc.query(SELECT + " WHERE d.id = ?", this::row, id).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("ENTITY_TAX_DECLARATION_NOT_FOUND", "税费申报登记不存在"));
    }

    private EntityTaxDeclaration row(ResultSet rs, int n) throws SQLException {
        Timestamp approved = rs.getTimestamp("approved_at");
        Long system = rs.getObject("system_calculated_amount_cent", Long.class);
        List<EntityTaxDeclaration.Adjustment> adjustments = list(rs.getString("adjustments"), EntityTaxDeclaration.Adjustment.class);
        boolean alignment = !ADJUSTMENT_TAXES.contains(rs.getString("tax_type"));
        return new EntityTaxDeclaration(rs.getString("id"), rs.getString("business_entity_id"), rs.getString("entity_name"),
                rs.getString("tax_type"), rs.getString("period_start_month"), rs.getString("period_end_month"),
                rs.getLong("declared_amount_cent"), rs.getString("reason"), list(rs.getString("evidence_attachment_ids"), String.class),
                rs.getString("status"), rs.getString("review_id"), rs.getString("created_by"),
                rs.getTimestamp("created_at").toInstant(), approved == null ? null : approved.toInstant(),
                rs.getObject("paid_amount_cent", Long.class), rs.getObject("payment_difference_cent", Long.class),
                list(rs.getString("payment_breakdown"), EntityTaxDeclaration.Breakdown.class), rs.getBoolean("payment_pending"),
                adjustments, system, system == null ? null : rs.getLong("declared_amount_cent") - system, alignment ? "alignment" : "adjustment",
                list(rs.getString("calculation_basis"), EntityTaxDeclaration.CalculationBasis.class),
                alignment && "approved".equals(rs.getString("status")) ? correctedDeclared(rs.getLong("declared_amount_cent"), adjustments) : null);
    }

    private EntityTaxDeclaration.Rate rateRow(ResultSet rs, int n) throws SQLException {
        return new EntityTaxDeclaration.Rate(rs.getString("id"), rs.getString("business_entity_id"), rs.getString("entity_name"),
                rs.getString("income_type"), rs.getString("tax_type"), rs.getBoolean("applicable"), rs.getInt("rate_bps"),
                rs.getObject("effective_from", LocalDate.class), rs.getString("reason"), rs.getString("review_status"), rs.getString("review_id"));
    }

    private static String rateText(boolean applicable, int bps) {
        return applicable ? "有，" + BigDecimal.valueOf(bps, 2).toPlainString() + "%" : "没有这笔费用";
    }

    private <T> List<T> list(String text, Class<T> type) {
        return text == null ? null : read(text, json.getTypeFactory().constructCollectionType(List.class, type));
    }

    private <T> T read(String text, JavaType type) {
        try {
            return json.readValue(text, type);
        } catch (Exception e) {
            throw new IllegalStateException("税费登记数据无法解析", e);
        }
    }

    private String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("申报登记附件清单无法保存", e);
        }
    }

    private static String yuan(long cent) {
        return BigDecimal.valueOf(cent, 2).toPlainString();
    }

    private static String requireTaxType(String value) {
        String taxType = value.trim();
        if (!TAX_LABELS.containsKey(taxType)) throw invalid("无法识别的税种：" + taxType);
        return taxType;
    }

    private static String requireMonth(String value, String field) {
        String month = requireText(value, field);
        if (!MONTH.matcher(month).matches()) throw invalid("月份须为「年-月」格式（如 2026-01）：" + month);
        return month;
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ApiException invalid(String message) {
        return ApiException.badRequest("VALIDATION_FAILED", message);
    }
}
