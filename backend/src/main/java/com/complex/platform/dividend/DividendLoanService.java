package com.complex.platform.dividend;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.dividend.dto.DividendLoanRequest;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.evidence.EvidenceAttachmentValidator;
import com.complex.platform.finance.ArApItemRecord;
import com.complex.platform.finance.ArApItemSourceRecord;
import com.complex.platform.finance.FinanceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AX 段第 273 条 273-R1、273-R2 公司向酒吧借款登记与分红前置借款清偿（PGM:436「必须按增资、股东借款、公司间借款等真实关系登记，借款不得当作股权或
 * 分红基数。任何公司向酒吧提供的借款，出借时须写明金额、利率、还款期限与还款顺序；酒吧当期可分配利润必须先偿还全部到期借款本息，剩余部分才可用于分红，存在逾期
 * 未还借款的一律不得分红，分红方案提交时须附借款清偿情况」；BSM:1785；ENUM:301；决策台账 DEC-R7-21、DEC-R7-41；第十八批裁决 55、65、66）。
 * <ul>
 *   <li>平台财务在持股分红页按酒吧登记（只插不改）：本金、年利率、借款协议约定的到期应付利息、出借日、还款期限、还款顺序必填；同事务挂一笔酒吧应付
 *       （source_type bar_loan，往来方为出借方主体、外部公司取借款号，金额＝本金＋利息，到期日＝还款期限），还款走台账付款确认与登记付款，已还多少以该应付为准。</li>
 *   <li>到期本息＝该应付未付额；到期未还＝还款期限不晚于某营业日且未付清（出单时先偿，{@link #dueUnpaid}）；逾期未还＝还款期限早于当前营业日且未付清（提交与
 *       审批即拦，{@link #requireNoOverdue}）。</li>
 * </ul>
 * 写路径走 {@link AuditedOperationExecutor#executeWrite}，范围闸放在幂等之前；直接用 JdbcTemplate（照 DividendCloseoutService）。
 */
@Service
public class DividendLoanService {

    static final String OBJECT = "dividend_bar_loan";
    static final String SOURCE_TYPE = "bar_loan";
    private static final String SELECT = "SELECT l.*, a.item_no, a.unpaid_amount_cent FROM dividend_bar_loans l JOIN ar_ap_items a ON a.id = l.ar_ap_item_id";
    private static final String ORDER = " ORDER BY l.repay_order, l.due_date, l.created_at, l.id";

    /**
     * 一笔借款。itemNo 为所挂应付的台账单号；outstandingCent 为该应付未付额（未还本息）；status 按该酒吧当前营业日判：not_due 未到期、due 已到期未还、
     * overdue 逾期未还、repaid 已还清（出单内部取数时为空）。
     */
    public record Loan(String id, String merchantId, String lenderEntityId, String lenderName, long principalCent, int annualRateBps,
                       long interestCent, LocalDate lentOn, LocalDate dueDate, int repayOrder, String note, List<String> evidenceAttachmentIds,
                       String arApItemId, String itemNo, long outstandingCent, String status, String createdBy, Instant createdAt) {
        Loan withStatus(LocalDate today) {
            String state = outstandingCent <= 0 ? "repaid" : dueDate.isAfter(today) ? "not_due" : dueDate.isBefore(today) ? "overdue" : "due";
            return new Loan(id, merchantId, lenderEntityId, lenderName, principalCent, annualRateBps, interestCent, lentOn, dueDate, repayOrder,
                    note, evidenceAttachmentIds, arApItemId, itemNo, outstandingCent, state, createdBy, createdAt);
        }
    }

    private final JdbcTemplate jdbc;
    private final FinanceRepository ledger;
    private final BusinessDayResolver businessDays;
    private final AuthorizationService auth;
    private final AuditedOperationExecutor operations;
    private final EvidenceAttachmentValidator attachments;
    private final ObjectMapper json;

    public DividendLoanService(JdbcTemplate jdbc, FinanceRepository ledger, BusinessDayResolver businessDays, AuthorizationService auth,
                               AuditedOperationExecutor operations, EvidenceAttachmentValidator attachments, ObjectMapper json) {
        this.jdbc = jdbc;
        this.ledger = ledger;
        this.businessDays = businessDays;
        this.auth = auth;
        this.operations = operations;
        this.attachments = attachments;
        this.json = json;
    }

    /** 借款列表（平台口径），按还款顺序；merchantId 非空只看该酒吧。状态按各酒吧当前营业日判。 */
    public List<Loan> list(String merchantId) {
        auth.requirePermission(DividendService.VIEW_PERM);
        auth.requirePlatformScope();
        String merchant = trimToNull(merchantId);
        return operations.readSingle(
                AuditedOperation.of("dividend.loans_listed", "dividend.loans_list_failed", DividendService.VIEW_PERM, OBJECT),
                () -> {
                    List<Loan> rows = merchant == null
                            ? jdbc.query(SELECT + " ORDER BY l.merchant_id, l.repay_order, l.due_date, l.created_at, l.id", this::row)
                            : jdbc.query(SELECT + " WHERE l.merchant_id = ?" + ORDER, this::row, merchant);
                    Map<String, LocalDate> days = new HashMap<>();
                    Instant now = Instant.now();
                    return rows.stream().map(loan -> loan.withStatus(days.computeIfAbsent(loan.merchantId(),
                            id -> businessDays.merchantBusinessDate(null, id, now)))).toList();
                },
                rows -> merchant == null ? "all" : merchant);
    }

    /** 登记借款并同事务挂酒吧应付（只适用于酒吧）。 */
    @Transactional
    public Loan register(DividendLoanRequest request) {
        auth.requirePermission(DividendService.MANAGE_PERM);
        auth.requirePlatformScope();
        if (request == null) {
            throw invalid("请求内容不能为空");
        }
        return operations.executeWrite("registerDividendBarLoan", request.idempotencyKey(), request, Loan.class,
                AuditedOperation.of("dividend.loan_registered", "dividend.loan_register_failed", DividendService.MANAGE_PERM, OBJECT),
                () -> {
                    String merchantId = requireText(request.merchantId(), "merchant_id");
                    String type = jdbc.query("SELECT merchant_type FROM merchants WHERE id = ?", (rs, n) -> rs.getString(1), merchantId)
                            .stream().findFirst().orElseThrow(() -> ApiException.notFound("MERCHANT_NOT_FOUND", "商户不存在或已被删除"));
                    if (!"bar".equals(type)) {
                        throw invalid("借款登记只适用于酒吧（公司向酒吧提供的借款）");
                    }
                    String entityId = trimToNull(request.lenderEntityId());
                    String lenderName = trimToNull(request.lenderName());
                    if (entityId != null) {
                        String entityName = jdbc.query("SELECT entity_name FROM business_entities WHERE id = ?", (rs, n) -> rs.getString(1), entityId)
                                .stream().findFirst().orElseThrow(() -> ApiException.notFound("BUSINESS_ENTITY_NOT_FOUND", "出借方主体不存在：" + entityId));
                        lenderName = lenderName == null ? entityName : lenderName;
                    }
                    if (lenderName == null) {
                        throw invalid("请选择出借方主体或填写出借方名称");
                    }
                    Long principal = request.principalCent();
                    Integer rate = request.annualRateBps();
                    Long interest = request.interestCent();
                    Integer order = request.repayOrder();
                    LocalDate lentOn = request.lentOn();
                    LocalDate due = request.dueDate();
                    if (principal == null || principal <= 0) {
                        throw invalid("借款本金须大于 0");
                    }
                    if (rate == null || rate < 0 || rate > 10000) {
                        throw invalid("年利率须在 0%～100% 之间");
                    }
                    if (interest == null || interest < 0) {
                        throw invalid("须填写借款协议约定的到期应付利息（无息填 0）");
                    }
                    if (lentOn == null || due == null) {
                        throw invalid("须填写出借日与还款期限");
                    }
                    if (due.isBefore(lentOn)) {
                        throw invalid("还款期限不能早于出借日");
                    }
                    if (order == null || order < 1) {
                        throw invalid("还款顺序须为不小于 1 的整数");
                    }
                    List<String> evidence = request.evidenceAttachmentIds() == null ? List.of() : request.evidenceAttachmentIds();
                    attachments.requireExisting(evidence, "evidence_attachment_ids");
                    String key = UUID.randomUUID().toString().replace("-", "");
                    String id = "dbl_" + key;
                    String item = "arap_dbl_" + key;
                    long amount = principal + interest;
                    Instant now = Instant.now();
                    // 照 BarBillAnomalyService 派生应付写法：往来方为出借方主体（外部公司无主体时取借款号），归该酒吧与所属综合体，待付。
                    ledger.appendArApItem(new ArApItemRecord(item, "ARAP-LOAN-" + key, "payable", entityId != null ? entityId : id, SOURCE_TYPE, id,
                            amount, 0L, amount, "CNY", due, YearMonth.from(due).toString(), "pending_payment", now, now, merchantId,
                            businessDays.projectOfMerchant(merchantId)));
                    ledger.saveArApItemSource(new ArApItemSourceRecord("arap_src_" + item, item, SOURCE_TYPE, id, amount, now));
                    jdbc.update("INSERT INTO dividend_bar_loans (id, merchant_id, lender_entity_id, lender_name, principal_cent, annual_rate_bps,"
                                    + " interest_cent, lent_on, due_date, repay_order, note, evidence_attachment_ids, ar_ap_item_id, created_by)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)",
                            id, merchantId, entityId, lenderName, principal, rate, interest, lentOn, due, order, trimToNull(request.note()),
                            encode(evidence), item, auth.currentActor().id());
                    return jdbc.query(SELECT + " WHERE l.id = ?", this::row, id).getFirst()
                            .withStatus(businessDays.merchantBusinessDate(null, merchantId, now));
                },
                saved -> ((Loan) saved).id(), Map.of());
    }

    /** 某营业日已到期未还的借款（按还款顺序、到期日），出单时先偿。不判登录人（月末自动预览无请求上下文）。 */
    public List<Loan> dueUnpaid(String merchantId, LocalDate asOf) {
        return jdbc.query(SELECT + " WHERE l.merchant_id = ? AND l.due_date <= ? AND a.unpaid_amount_cent > 0" + ORDER, this::row, merchantId, asOf);
    }

    /** 有逾期未还借款（还款期限早于当前营业日仍未付清）即 409：分红提交与审批前判（PGM:436「存在逾期未还借款的一律不得分红」）。 */
    public void requireNoOverdue(String merchantId, LocalDate today) {
        List<Loan> overdue = jdbc.query(SELECT + " WHERE l.merchant_id = ? AND l.due_date < ? AND a.unpaid_amount_cent > 0" + ORDER, this::row,
                merchantId, today);
        if (!overdue.isEmpty()) {
            throw ApiException.conflict("DIVIDEND_LOAN_OVERDUE", "该酒吧有逾期未还借款（" + overdue.stream()
                    .map(loan -> "出借方 " + loan.lenderName() + "，到期 " + loan.dueDate() + "，未还本息 " + yuan(loan.outstandingCent()))
                    .collect(Collectors.joining("；")) + "），须先还清才能分红");
        }
    }

    private Loan row(ResultSet rs, int n) throws SQLException {
        return new Loan(rs.getString("id"), rs.getString("merchant_id"), rs.getString("lender_entity_id"), rs.getString("lender_name"),
                rs.getLong("principal_cent"), rs.getInt("annual_rate_bps"), rs.getLong("interest_cent"), rs.getObject("lent_on", LocalDate.class),
                rs.getObject("due_date", LocalDate.class), rs.getInt("repay_order"), rs.getString("note"), decode(rs.getString("evidence_attachment_ids")),
                rs.getString("ar_ap_item_id"), rs.getString("item_no"), rs.getLong("unpaid_amount_cent"), null, rs.getString("created_by"),
                nullableInstant(rs, "created_at"));
    }

    private String encode(List<String> ids) {
        try {
            return json.writeValueAsString(ids);
        } catch (Exception e) {
            throw new IllegalStateException("借款凭证清单无法保存", e);
        }
    }

    private List<String> decode(String text) {
        try {
            return text == null ? List.of() : List.of(json.readValue(text, String[].class));
        } catch (Exception e) {
            throw new IllegalStateException("借款凭证清单无法解析", e);
        }
    }

    static String yuan(long cent) {
        return BigDecimal.valueOf(cent, 2).toPlainString() + " 元";
    }

    private static ApiException invalid(String message) {
        return ApiException.badRequest("VALIDATION_FAILED", message);
    }
}
