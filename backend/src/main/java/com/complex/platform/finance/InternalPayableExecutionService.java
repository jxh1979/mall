package com.complex.platform.finance;

// 文件版本：V0.03

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FS-021-R5 普通内部结算应付的执行状态（BSM L1683；ENUM L253「内部应付执行状态：待付（含已发起、尚未确认到账且未收到失败结果的
 * 银行在途，在途不重付）、付款失败待处理、已结清」；DEC-P1B-07、DEC-P1C-02、DEC-R4-53）。
 * <ul>
 *   <li>付款发起记一次尝试（在途）：在途不标失败、不得再发起；异议期未届满的不进入付款（顺延二）。</li>
 *   <li>收到明确失败结果：标「付款失败待处理」，注明原因与预计重付时间，重付不迟于下一个付账日；原路重付或换账户后重付。</li>
 *   <li>更换收款账户须经收款方所属公司财务负责人审批：由收款方持该权限的人在商户专区登记即为审批。</li>
 *   <li>资金不足（顺延三）：顺延至下一个付账日，逐次留痕，不另行通知收款方，待付状态对收款方可见。</li>
 *   <li>只有确认到账（既有登记收付款 {@link FinanceService#registerArApPayment}）才结清，同时回写在途那次为到账。</li>
 *   <li>已有冲正/重述行回指的原行（V344 红冲范式）不再是有效款项：执行状态 reversed，不得再发起付款、登记失败或顺延
 *       （409 AR_AP_ITEM_REVERSED，与登记收付款 {@link FinanceService#settleArApItem} 同码）。</li>
 * </ul>
 * 只作用于有付款时点固化（internal_payable_schedules）的普通内部结算往来；工资与暂缓的拨付（CG 第 18 条）不在此列。
 */
@Service
public class InternalPayableExecutionService {

    static final String VIEW = "finance.ar_ap.view";
    static final String PAY = "finance.payment.register";
    static final String PORTAL_VIEW = "merchant.portal.settlement.view";
    static final String PAYEE_APPROVE = "merchant.portal.payee_account.approve";

    public record InitiateRequest(String idempotencyKey, String bankRef, String accountChangeId) {
    }

    public record FailureRequest(String idempotencyKey, String failureReason, LocalDate expectedRepayDate) {
    }

    public record FundsInsufficientRequest(String idempotencyKey, String note) {
    }

    public record AccountChangeRequest(String idempotencyKey, String newAccountRef, String reason) {
    }

    public record Attempt(String id, String arApItemId, int attemptNo, long amountCent, String bankRef, String accountChangeId,
                          String status, String failureReason, LocalDate expectedRepayDate, Instant initiatedAt) {
    }

    /** 收款方与财务可见的一笔普通内部结算往来：金额、可查看时刻、异议截止、指定付账日、当前到期日与执行状态。 */
    public record Execution(String arApItemId, String direction, String sourceType, String counterpartyId, String projectId,
                            long amountCent, long unpaidAmountCent, Instant visibleAt, LocalDate objectionDeadline,
                            LocalDate designatedPayDay, LocalDate dueDate, String executionStatus, boolean inTransit,
                            String failureReason, LocalDate expectedRepayDate, List<Attempt> attempts,
                            List<Map<String, Object>> postponements) {
    }

    private record Schedule(LocalDate objectionDeadline, LocalDate designatedPayDay, Instant visibleAt, int[] payDays) {
    }

    private final JdbcTemplate jdbc;
    private final AuthorizationService auth;
    private final FinanceRepository repository;
    private final FinanceService finance;
    private final WorkingDayCalendar calendar;
    private final IdempotencyService idem;
    private final AuditEventService audit;

    public InternalPayableExecutionService(JdbcTemplate jdbc, AuthorizationService auth, FinanceRepository repository,
                                           FinanceService finance, WorkingDayCalendar calendar, IdempotencyService idem,
                                           AuditEventService audit) {
        this.jdbc = jdbc;
        this.auth = auth;
        this.repository = repository;
        this.finance = finance;
        this.calendar = calendar;
        this.idem = idem;
        this.audit = audit;
    }

    public Execution execution(String arApItemId) {
        ArApItemRecord item = item(arApItemId, false);
        finance.requireArApScope(item, VIEW);
        return view(item, schedule(item.id()));
    }

    @Transactional
    public Attempt initiate(String arApItemId, InitiateRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        try {
            InitiateRequest r = requireRequest(request);
            auth.requirePermission(PAY);
            return idem.execute("initiateInternalPayment:" + arApItemId, key, r, () -> {
                ArApItemRecord item = payable(arApItemId);
                Schedule s = schedule(item.id());
                if (LocalDate.now(LocalZone.ID).isBefore(s.objectionDeadline())) {
                    throw ApiException.conflict("OBJECTION_PERIOD_NOT_EXPIRED",
                            "异议期 " + s.objectionDeadline() + " 届满前不进入付款（顺延二）");
                }
                if (latest(item.id()).filter(a -> "initiated".equals(a.status())).isPresent()) {
                    throw ApiException.conflict("PAYMENT_IN_TRANSIT", "该笔付款已发起、尚未确认到账且未收到失败结果，属银行在途，不得重付");
                }
                String change = trimToNull(r.accountChangeId());
                if (change != null && jdbc.queryForObject(
                        "SELECT count(*) FROM internal_payable_account_changes WHERE id=? AND ar_ap_item_id=?",
                        Integer.class, change, item.id()) == 0) {
                    throw ApiException.conflict("ACCOUNT_CHANGE_NOT_APPROVED", "更换收款账户须经收款方所属公司财务负责人审批");
                }
                String id = "ippa_" + UUID.randomUUID().toString().replace("-", "");
                int no = jdbc.queryForObject("SELECT count(*)+1 FROM internal_payable_payment_attempts WHERE ar_ap_item_id=?",
                        Integer.class, item.id());
                jdbc.update("""
                        INSERT INTO internal_payable_payment_attempts(id,ar_ap_item_id,attempt_no,amount_cent,bank_ref,account_change_id,
                            status,initiated_by,initiated_at)
                        VALUES(?,?,?,?,?,?,'initiated',?,?)
                        """, id, item.id(), no, item.unpaidAmountCent(), trimToNull(r.bankRef()), change, auth.currentActor().id(),
                        Timestamp.from(Instant.now()));
                audit.recordSuccess("finance.internal_payment.initiated", PAY, "ar_ap_item", item.id(), null, key,
                        Map.of("attempt_id", id, "amount_cent", item.unpaidAmountCent()));
                return attempt(id);
            }, Attempt.class);
        } catch (RuntimeException exception) {
            audit.recordFailure("finance.internal_payment.initiate_failed", PAY, "ar_ap_item", arApItemId, null, key, code(exception));
            throw exception;
        }
    }

    @Transactional
    public Attempt fail(String arApItemId, String attemptId, FailureRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        try {
            FailureRequest r = requireRequest(request);
            auth.requirePermission(PAY);
            requireText(r.failureReason(), "failure_reason");
            return idem.execute("failInternalPayment:" + attemptId, key, r, () -> {
                ArApItemRecord item = payable(arApItemId);
                Attempt attempt = latest(item.id()).filter(a -> a.id().equals(attemptId) && "initiated".equals(a.status()))
                        .orElseThrow(() -> ApiException.conflict("ATTEMPT_NOT_IN_TRANSIT", "只有在途的那次付款可登记失败结果"));
                LocalDate today = LocalDate.now(LocalZone.ID);
                LocalDate nextPayDay = calendar.bankDayOnOrAfter(
                        com.complex.platform.settlement.InternalPayDayService.firstPayDayOnOrAfter(schedule(item.id()).payDays(),
                                today.plusDays(1)));
                LocalDate repay = r.expectedRepayDate();
                if (repay == null || repay.isBefore(today) || repay.isAfter(nextPayDay)) {
                    throw ApiException.badRequest("REPAY_AFTER_NEXT_PAY_DAY", "预计重付时间须在今天至下一个付账日（" + nextPayDay + "）之间");
                }
                jdbc.update("""
                        UPDATE internal_payable_payment_attempts SET status='failed',failure_reason=?,expected_repay_date=?,
                            resolved_by=?,resolved_at=now() WHERE id=?
                        """, r.failureReason().trim(), repay, auth.currentActor().id(), attempt.id());
                audit.recordSuccess("finance.internal_payment.failed", PAY, "ar_ap_item", item.id(), null, key,
                        Map.of("attempt_id", attempt.id(), "expected_repay_date", repay.toString()));
                return attempt(attempt.id());
            }, Attempt.class);
        } catch (RuntimeException exception) {
            audit.recordFailure("finance.internal_payment.fail_record_failed", PAY, "ar_ap_item", arApItemId, null, key, code(exception));
            throw exception;
        }
    }

    /** 顺延三：账上资金不足的，该笔付款顺延至下一个付账日；不另行通知收款方。 */
    @Transactional
    public Execution fundsInsufficient(String arApItemId, FundsInsufficientRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        try {
            FundsInsufficientRequest r = requireRequest(request);
            auth.requirePermission(PAY);
            requireText(r.note(), "note");
            return idem.execute("postponeInternalPayment:" + arApItemId, key, r, () -> {
                ArApItemRecord item = payable(arApItemId);
                if (latest(item.id()).filter(a -> "initiated".equals(a.status())).isPresent()) {
                    throw ApiException.conflict("PAYMENT_IN_TRANSIT", "该笔付款已发起在途，不属资金不足顺延");
                }
                Schedule s = schedule(item.id());
                LocalDate next = calendar.bankDayOnOrAfter(
                        com.complex.platform.settlement.InternalPayDayService.firstPayDayOnOrAfter(s.payDays(), item.dueDate().plusDays(1)));
                jdbc.update("""
                        INSERT INTO internal_payable_postponements(id,ar_ap_item_id,reason_code,previous_due_date,due_date,note,recorded_by)
                        VALUES(?,?,'funds_insufficient',?,?,?,?)
                        """, "ipp_" + UUID.randomUUID().toString().replace("-", ""), item.id(), item.dueDate(), next, r.note().trim(),
                        auth.currentActor().id());
                jdbc.update("UPDATE ar_ap_items SET due_date=?,updated_at=now() WHERE id=?", next, item.id());
                audit.recordSuccess("finance.internal_payment.postponed", PAY, "ar_ap_item", item.id(), null, key,
                        Map.of("previous_due_date", item.dueDate().toString(), "due_date", next.toString()));
                return view(repository.findArApItemById(item.id()).orElseThrow(), s);
            }, Execution.class);
        } catch (RuntimeException exception) {
            audit.recordFailure("finance.internal_payment.postpone_failed", PAY, "ar_ap_item", arApItemId, null, key, code(exception));
            throw exception;
        }
    }

    /** 收款方所属公司财务负责人在商户专区登记新收款账户即为审批；只有付款失败待处理的款项可换账户。 */
    @Transactional
    public Map<String, Object> approveAccountChange(String arApItemId, AccountChangeRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        try {
            AccountChangeRequest r = requireRequest(request);
            auth.requirePermission(PAYEE_APPROVE);
            requireText(r.newAccountRef(), "new_account_ref");
            requireText(r.reason(), "reason");
            return idem.execute("approveInternalPayeeAccount:" + arApItemId, key, r, () -> {
                ArApItemRecord item = item(arApItemId, true);
                schedule(item.id());
                requirePayee(item, PAYEE_APPROVE);
                Attempt failed = latest(item.id()).filter(a -> "failed".equals(a.status()))
                        .orElseThrow(() -> ApiException.conflict("ACCOUNT_CHANGE_NOT_ALLOWED", "只有付款失败待处理的款项可更换收款账户"));
                String id = "ipac_" + UUID.randomUUID().toString().replace("-", "");
                jdbc.update("""
                        INSERT INTO internal_payable_account_changes(id,ar_ap_item_id,failed_attempt_id,new_account_ref,reason,approved_by,approved_at)
                        VALUES(?,?,?,?,?,?,now())
                        """, id, item.id(), failed.id(), r.newAccountRef().trim(), r.reason().trim(), auth.currentActor().id());
                audit.recordSuccess("merchant_portal.payee_account.approved", PAYEE_APPROVE, "ar_ap_item", item.id(), null, key,
                        Map.of("account_change_id", id, "failed_attempt_id", failed.id()));
                return Map.<String, Object>of("id", id, "ar_ap_item_id", item.id(), "failed_attempt_id", failed.id());
            }, Map.class);
        } catch (RuntimeException exception) {
            audit.recordFailure("merchant_portal.payee_account.approve_failed", PAYEE_APPROVE, "ar_ap_item", arApItemId, null, key,
                    code(exception));
            throw exception;
        }
    }

    /** 商户专区「门票分账」：本商户作为对方的、已可查看的普通内部结算往来及其执行状态。
     *  第十八批 E 段 20-R3：商户账号看各经营项目里本商户的往来，只按商户（不再按本人项目过滤）。 */
    public List<Execution> merchantTicketSettlements() {
        auth.requirePermission(PORTAL_VIEW);
        String merchant = auth.currentMerchantIdQuietly()
                .orElseThrow(() -> ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "此操作必须在商户范围内进行"));
        return jdbc.queryForList("""
                        SELECT ar_ap_item_id, project_id FROM internal_payable_schedules WHERE counterparty_id=? AND visible_at<=now()
                         ORDER BY visible_at DESC, ar_ap_item_id
                        """, merchant).stream()
                .map(row -> repository.findArApItemById((String) row.get("ar_ap_item_id")).orElseThrow())
                .map(item -> view(item, schedule(item.id())))
                .toList();
    }

    // ─── 内部 ───────────────────────────────────────────

    private ArApItemRecord payable(String arApItemId) {
        ArApItemRecord item = item(arApItemId, true);
        finance.requireArApScope(item, PAY);
        if (!"payable".equals(item.direction())) {
            throw ApiException.conflict("INTERNAL_PAYABLE_REQUIRED", "只有平台向对方的应付适用付款执行状态");
        }
        if (repository.hasCorrectionItems(item.id())) {
            throw ApiException.conflict("AR_AP_ITEM_REVERSED", "该应收应付已被冲正作废，请使用当前有效款项");
        }
        if (item.unpaidAmountCent() == 0) {
            throw ApiException.conflict("AR_AP_ITEM_SETTLED", "该笔应付已结清");
        }
        return item;
    }

    private ArApItemRecord item(String arApItemId, boolean lock) {
        String id = requireText(arApItemId, "ar_ap_item_id");
        return (lock ? repository.findArApItemByIdForUpdate(id) : repository.findArApItemById(id))
                .orElseThrow(() -> ApiException.notFound("AR_AP_ITEM_NOT_FOUND", "应收应付款项不存在或已被删除"));
    }

    private void requirePayee(ArApItemRecord item, String permission) {
        String merchant = auth.currentMerchantIdQuietly().orElse(null);
        if (merchant == null || !merchant.equals(item.counterpartyId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "只有收款方所属公司的财务负责人可审批更换收款账户");
        }
        // 按固化付款时点的项目（往来对方所属项目）判定，写入口只到本人项目（列表对商户账号只按商户，第十八批 E 段 20-R3）；平台单台账行本身无项目归属。
        String project = jdbc.queryForObject("SELECT project_id FROM internal_payable_schedules WHERE ar_ap_item_id=?", String.class,
                item.id());
        if (!auth.projectReach(auth.currentActor(), permission).covers(project)) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "无权处理其他项目的款项");
        }
    }

    private Schedule schedule(String arApItemId) {
        return jdbc.query("""
                SELECT s.objection_deadline, s.designated_pay_day, s.visible_at, r.day_1, r.day_2, r.day_3
                  FROM internal_payable_schedules s JOIN internal_pay_day_rules r ON r.id = s.pay_days_rule_id
                 WHERE s.ar_ap_item_id=?
                """, (rs, n) -> new Schedule(rs.getObject(1, LocalDate.class), rs.getObject(2, LocalDate.class),
                rs.getTimestamp(3).toInstant(), new int[]{rs.getInt(4), rs.getInt(5), rs.getInt(6)}), arApItemId)
                .stream().findFirst()
                .orElseThrow(() -> ApiException.conflict("INTERNAL_PAYABLE_NOT_SCHEDULED", "该款项不是按付账日执行的普通内部结算往来"));
    }

    private Execution view(ArApItemRecord item, Schedule s) {
        List<Attempt> attempts = attempts(item.id());
        Attempt last = attempts.isEmpty() ? null : attempts.getLast();
        boolean failed = last != null && "failed".equals(last.status());
        String status = repository.hasCorrectionItems(item.id()) ? "reversed"
                : item.unpaidAmountCent() == 0 ? "settled" : failed ? "failed_pending" : "pending";
        return new Execution(item.id(), item.direction(), item.sourceType(), item.counterpartyId(), item.projectId(),
                item.amountCent(), item.unpaidAmountCent(), s.visibleAt(), s.objectionDeadline(), s.designatedPayDay(),
                item.dueDate(), status, last != null && "initiated".equals(last.status()),
                failed ? last.failureReason() : null, failed ? last.expectedRepayDate() : null, attempts,
                jdbc.queryForList("""
                        SELECT reason_code, previous_due_date, due_date, recorded_at FROM internal_payable_postponements
                         WHERE ar_ap_item_id=? ORDER BY recorded_at
                        """, item.id()));
    }

    private java.util.Optional<Attempt> latest(String arApItemId) {
        List<Attempt> all = attempts(arApItemId);
        return all.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(all.getLast());
    }

    private Attempt attempt(String id) {
        return jdbc.query("SELECT * FROM internal_payable_payment_attempts WHERE id=?", this::mapAttempt, id).getFirst();
    }

    private List<Attempt> attempts(String arApItemId) {
        return jdbc.query("SELECT * FROM internal_payable_payment_attempts WHERE ar_ap_item_id=? ORDER BY attempt_no",
                this::mapAttempt, arApItemId);
    }

    private Attempt mapAttempt(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new Attempt(rs.getString("id"), rs.getString("ar_ap_item_id"), rs.getInt("attempt_no"), rs.getLong("amount_cent"),
                rs.getString("bank_ref"), rs.getString("account_change_id"), rs.getString("status"), rs.getString("failure_reason"),
                rs.getObject("expected_repay_date", LocalDate.class), rs.getTimestamp("initiated_at").toInstant());
    }

    private static <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private static String code(RuntimeException exception) {
        return exception instanceof ApiException api ? api.code() : "INTERNAL_ERROR";
    }
}
