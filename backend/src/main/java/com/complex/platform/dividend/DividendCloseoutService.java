package com.complex.platform.dividend;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.dividend.DividendRecords.DividendPeriodHoldRecord;
import com.complex.platform.dividend.DividendRecords.DividendPlanRecord;
import com.complex.platform.dividend.DividendRecords.DividendStatementRecord;
import com.complex.platform.dividend.dto.DividendActionRequest;
import com.complex.platform.dividend.dto.DividendCloseoutRequest;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AQ 段第 243 条 243-R1 酒吧关停结算（BSM:1787「酒吧关闭须完成关停结算，依法核清本次分红、已算未发分红与发放保护线待释放款项；门店关停结算与
 * 公司依法解散清算分别办理。」「已确认应付及待释款项的法律性质和处理由财务、法务核实，不因关店自动核销或一概改为剩余财产分配；相关款项须按核实结果处理并
 * 留痕后才能结案。」；PGM:528「需要平行线审批的（如法务、财务），另行指定该条线的最终审批人」；PGM:529「申请人与直接受益人不得审批与自己相关的申请」）。
 * <ul>
 *   <li>平台财务在持股分红页按酒吧登记（关店最后营业日、原因必填，一家酒吧一条），不改商户状态、不动任何应付与计算单。</li>
 *   <li>款项三类现查：本次分红＝按关店最后营业日已生效的方案周期求关店所在期，取该期正式单（该期登记了暂不分红的列一行金额 0）；已算未发分红＝其余草稿、
 *       已驳回、待审批的正式单与未付清的分红应付（主理人、其他股东、已释放留存）；保护线待释放＝未付的留存应付。</li>
 *   <li>本次分红出单（或该期登记了暂不分红）后才能提交核实（「依法核清本次分红」）；逐条填法律性质与处理方式，进统一审批：发起人沿直接上级到指定的
 *       主线最终审批人，必带财务、法务两线，受益人为方案主理人。驳回退回待提交核实（PGM:528「任一节点驳回则整单退回起草人」），可改后再提交。</li>
 *   <li>核实通过后逐条登记处理说明，全部登记后才能结案；须付或须释放的仍走既有付款确认、登记付款与留存释放。</li>
 * </ul>
 * 写路径走 {@link AuditedOperationExecutor#executeWrite}，范围闸放在幂等之前；直接用 JdbcTemplate（照 PaymentConfirmationService）。
 */
@Service
public class DividendCloseoutService {

    static final String ACTION = "finance.dividend.closeout.verify";
    static final String OBJECT = "dividend_closeout";
    private static final String CURRENT = "current_period";
    private static final String UNPAID = "unpaid_dividend";
    private static final String RESERVE = "reserve_pending";
    private static final Map<String, String> ITEM_LABELS = Map.of(CURRENT, "本次分红", UNPAID, "已算未发分红", RESERVE, "保护线待释放");
    private static final Map<String, String> STATUS_LABELS = Map.of(
            "open", "待提交核实", "pending_review", "财务法务核实中", "verified", "已核实待处理", "closed", "已结案");

    /**
     * 一条款项。id 为提交核实时的快照编号（待提交核实时现查，为空）；periodLabel、statementStatus 为所属计算单的期间与状态；holdType 为本次分红
     * 没有正式单时该期的暂不分红（skip）或暂缓分红（defer）登记；sourceType 为台账来源；法律性质、处理方式与处理留痕在提交核实后才有。
     */
    public record Item(String id, String itemType, String periodLabel, String statementId, String statementStatus, String holdType,
                       String arApItemId, String sourceType, long amountCent, String legalNature, String handling, String handledNote,
                       String handledBy, Instant handledAt) {
    }

    /** 一条关停结算登记。款项键不叫 items：后台客户端会把 data.items 当分页信封拍平。 */
    public record Closeout(String id, String merchantId, LocalDate closedBusinessDate, String reason, String status, String reviewId,
                           String createdBy, Instant createdAt, String submittedBy, Instant submittedAt, String closedBy, Instant closedAt,
                           List<Item> closeoutItems) {
        Closeout withItems(List<Item> items) {
            return new Closeout(id, merchantId, closedBusinessDate, reason, status, reviewId, createdBy, createdAt, submittedBy, submittedAt,
                    closedBy, closedAt, items);
        }
    }

    private final JdbcTemplate jdbc;
    private final DividendRepository repository;
    private final BusinessDayResolver businessDays;
    private final AuthorizationService auth;
    private final AuditedOperationExecutor operations;
    private final SupervisorReviewService reviews;

    public DividendCloseoutService(JdbcTemplate jdbc, DividendRepository repository, BusinessDayResolver businessDays,
                                   AuthorizationService auth, AuditedOperationExecutor operations, SupervisorReviewService reviews) {
        this.jdbc = jdbc;
        this.repository = repository;
        this.businessDays = businessDays;
        this.auth = auth;
        this.operations = operations;
        this.reviews = reviews;
    }

    /** 关停结算列表（平台口径），最近登记在前；merchantId 非空只看该商户。 */
    public List<Closeout> list(String merchantId) {
        auth.requirePermission(DividendService.VIEW_PERM);
        auth.requirePlatformScope();
        String merchant = trimToNull(merchantId);
        return operations.readSingle(
                AuditedOperation.of("dividend.closeouts_listed", "dividend.closeouts_list_failed", DividendService.VIEW_PERM, OBJECT),
                () -> (merchant == null
                        ? jdbc.query("SELECT * FROM dividend_closeouts ORDER BY created_at DESC, id", this::mapHead)
                        : jdbc.query("SELECT * FROM dividend_closeouts WHERE merchant_id = ? ORDER BY created_at DESC, id", this::mapHead, merchant))
                        .stream().map(this::withItems).toList(),
                result -> "all");
    }

    /** 登记关停结算：只适用于酒吧，须有关店最后营业日已生效的分红方案，一家酒吧一条。 */
    @Transactional
    public Closeout register(DividendCloseoutRequest request) {
        auth.requirePermission(DividendService.MANAGE_PERM);
        auth.requirePlatformScope();
        requireRequest(request);
        return operations.executeWrite("registerDividendCloseout", request.idempotencyKey(), request, Closeout.class,
                AuditedOperation.of("dividend.closeout_registered", "dividend.closeout_register_failed", DividendService.MANAGE_PERM, OBJECT),
                () -> {
                    String merchantId = requireText(request.merchantId(), "merchant_id");
                    LocalDate closed = parseDate(requireText(request.closedBusinessDate(), "closed_business_date"));
                    String reason = requireText(request.reason(), "reason");
                    if (businessDays.barOfMerchant(merchantId) == null) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "关停结算只适用于酒吧");
                    }
                    planAt(merchantId, closed);
                    String id = "dco_" + UUID.randomUUID().toString().replace("-", "");
                    if (jdbc.update("INSERT INTO dividend_closeouts (id, merchant_id, closed_business_date, reason, status, created_by)"
                            + " VALUES (?, ?, ?, ?, 'open', ?) ON CONFLICT (merchant_id) DO NOTHING", id, merchantId, closed, reason,
                            auth.currentActor().id()) == 0) {
                        throw ApiException.conflict("DIVIDEND_CLOSEOUT_EXISTS", "该酒吧已登记关停结算");
                    }
                    return view(id);
                },
                DividendCloseoutService::businessId, Map.of());
    }

    /** 提交财务、法务核实：只认待提交核实；本次分红未出单（且该期没有暂不分红登记）409；逐条法律性质、处理方式必填，写快照后进统一审批。 */
    @Transactional
    public Closeout submit(String closeoutId, DividendCloseoutRequest.Submit request) {
        auth.requirePermission(DividendService.MANAGE_PERM);
        auth.requirePlatformScope();
        requireRequest(request);
        String id = requireText(closeoutId, "closeout_id");
        return operations.executeWrite("submitDividendCloseout:" + id, request.idempotencyKey(), request, Closeout.class,
                AuditedOperation.of("dividend.closeout_submitted", "dividend.closeout_submit_failed", DividendService.MANAGE_PERM, OBJECT),
                () -> {
                    Closeout head = lock(id);
                    if (!"open".equals(head.status())) {
                        throw ApiException.conflict("DIVIDEND_STATE_INVALID",
                                "只有待提交核实的关停结算可以提交核实（当前状态：" + STATUS_LABELS.get(head.status()) + "）");
                    }
                    List<Item> live = liveItems(head.merchantId(), head.closedBusinessDate());
                    Item current = live.getFirst();
                    if (current.statementId() == null && !"skip".equals(current.holdType())) {
                        throw ApiException.conflict("DIVIDEND_CLOSEOUT_CURRENT_STATEMENT_MISSING", "关店所在期正式分红单尚未出单，出单后再提交核实");
                    }
                    String finalApprover = requireText(request.finalApproverId(), "final_approver_id");
                    List<DividendCloseoutRequest.Item> given = request.items() == null ? List.of() : request.items();
                    jdbc.update("DELETE FROM dividend_closeout_items WHERE closeout_id = ?", id);
                    StringBuilder summary = new StringBuilder("酒吧关停结算核实：商户 " + head.merchantId() + "，关店最后营业日 "
                            + head.closedBusinessDate() + "；关店原因：" + head.reason() + "。款项：");
                    long total = 0L;
                    for (int i = 0; i < live.size(); i++) {
                        Item item = live.get(i);
                        DividendCloseoutRequest.Item match = given.stream().filter(row -> row != null && item.itemType().equals(row.itemType())
                                && Objects.equals(item.statementId(), trimToNull(row.statementId()))
                                && Objects.equals(item.arApItemId(), trimToNull(row.arApItemId()))).findFirst().orElse(null);
                        String what = "第 " + (i + 1) + " 条款项（" + describe(item) + "）";
                        String nature = match == null ? null : trimToNull(match.legalNature());
                        String handling = match == null ? null : trimToNull(match.handling());
                        if (nature == null) {
                            throw ApiException.badRequest("VALIDATION_FAILED", what + "须填写法律性质");
                        }
                        if (handling == null) {
                            throw ApiException.badRequest("VALIDATION_FAILED", what + "须填写处理方式");
                        }
                        jdbc.update("INSERT INTO dividend_closeout_items (id, closeout_id, sort_order, item_type, statement_id, ar_ap_item_id,"
                                        + " amount_cent, legal_nature, handling) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                "dcoi_" + UUID.randomUUID().toString().replace("-", ""), id, i, item.itemType(), item.statementId(),
                                item.arApItemId(), item.amountCent(), nature, handling);
                        summary.append('\n').append(i + 1).append(". ").append(describe(item)).append("；法律性质：").append(nature)
                                .append("；处理方式：").append(handling);
                        total += item.amountCent();
                    }
                    String beneficiary = planAt(head.merchantId(), head.closedBusinessDate()).principalUserId();
                    String reviewId = reviews.evaluate(new SupervisorReviewEvaluateRequest(request.idempotencyKey() + ":" + OBJECT, head.reason(),
                            List.of(), ACTION, OBJECT, id, total, summary.toString(), finalApprover,
                            request.parallelLines() == null ? List.of() : request.parallelLines(), beneficiary)).reviewId();
                    jdbc.update("UPDATE dividend_closeouts SET status = 'pending_review', review_id = ?, submitted_by = ?, submitted_at = clock_timestamp()"
                            + " WHERE id = ? AND status = 'open'", reviewId, auth.currentActor().id(), id);
                    return view(id);
                },
                DividendCloseoutService::businessId, Map.of());
    }

    /** 核实结果回写（照 PaymentConfirmationService#decided）：只认本动作与对象、仍待核实且复核单号相符、复核表里确有该终态的行。驳回退回待提交核实。 */
    @EventListener
    @Transactional
    public void decided(SupervisorReviewDecisionRecord decision) {
        if (!ACTION.equals(decision.actionType()) || !OBJECT.equals(decision.objectType())
                || !Set.of("approved", "rejected").contains(decision.reviewStatus())) {
            return;
        }
        boolean approved = "approved".equals(decision.reviewStatus());
        jdbc.update("UPDATE dividend_closeouts SET status = ?, review_id = " + (approved ? "review_id" : "NULL")
                        + " WHERE id = ? AND review_id = ? AND status = 'pending_review' AND EXISTS (SELECT 1 FROM supervisor_review_decisions"
                        + " WHERE review_id = ? AND action_type = ? AND object_type = ? AND object_id = ? AND review_status = ?)",
                approved ? "verified" : "open", decision.objectId(), decision.reviewId(), decision.reviewId(), ACTION, OBJECT,
                decision.objectId(), decision.reviewStatus());
    }

    /** 按核实结果登记一条款项的处理（处理说明必填，只在已核实待处理时，每条只登记一次）。 */
    @Transactional
    public Closeout handle(String closeoutId, String itemId, DividendCloseoutRequest.Handle request) {
        auth.requirePermission(DividendService.MANAGE_PERM);
        auth.requirePlatformScope();
        requireRequest(request);
        String id = requireText(closeoutId, "closeout_id");
        String item = requireText(itemId, "item_id");
        return operations.executeWrite("handleDividendCloseoutItem:" + id + ":" + item, request.idempotencyKey(), request, Closeout.class,
                AuditedOperation.of("dividend.closeout_item_handled", "dividend.closeout_item_handle_failed", DividendService.MANAGE_PERM, OBJECT),
                () -> {
                    requireVerified(lock(id));
                    String note = requireText(request.handledNote(), "handled_note");
                    if (jdbc.update("UPDATE dividend_closeout_items SET handled_note = ?, handled_by = ?, handled_at = clock_timestamp()"
                            + " WHERE id = ? AND closeout_id = ? AND handled_at IS NULL", note, auth.currentActor().id(), item, id) == 0) {
                        if (jdbc.queryForObject("SELECT count(*) FROM dividend_closeout_items WHERE id = ? AND closeout_id = ?", Integer.class,
                                item, id) > 0) {
                            throw ApiException.conflict("DIVIDEND_CLOSEOUT_ITEM_HANDLED", "该款项已登记处理，不能重复登记");
                        }
                        throw ApiException.notFound("DIVIDEND_CLOSEOUT_NOT_FOUND", "关停结算款项不存在：" + item);
                    }
                    return view(id);
                },
                DividendCloseoutService::businessId, Map.of());
    }

    /** 结案：只在已核实待处理时，且每条款项都已登记处理。 */
    @Transactional
    public Closeout close(String closeoutId, DividendActionRequest request) {
        auth.requirePermission(DividendService.MANAGE_PERM);
        auth.requirePlatformScope();
        requireRequest(request);
        String id = requireText(closeoutId, "closeout_id");
        return operations.executeWrite("closeDividendCloseout:" + id, request.idempotencyKey(), request, Closeout.class,
                AuditedOperation.of("dividend.closeout_closed", "dividend.closeout_close_failed", DividendService.MANAGE_PERM, OBJECT),
                () -> {
                    requireVerified(lock(id));
                    int pending = jdbc.queryForObject("SELECT count(*) FROM dividend_closeout_items WHERE closeout_id = ? AND handled_at IS NULL",
                            Integer.class, id);
                    if (pending > 0) {
                        throw ApiException.conflict("DIVIDEND_CLOSEOUT_ITEMS_PENDING",
                                "还有 " + pending + " 条款项未按核实结果登记处理，逐条处理留痕后才能结案");
                    }
                    jdbc.update("UPDATE dividend_closeouts SET status = 'closed', closed_by = ?, closed_at = clock_timestamp()"
                            + " WHERE id = ? AND status = 'verified'", auth.currentActor().id(), id);
                    return view(id);
                },
                DividendCloseoutService::businessId, Map.of());
    }

    // ── 款项 ──────────────────────────────────────────────────

    /** 现查三类款项：本次分红一行在前，再列其余未发的正式单、未付清的分红应付，最后是保护线待释放。 */
    private List<Item> liveItems(String merchantId, LocalDate closed) {
        String label = DividendPeriod.labelOf(closed, planAt(merchantId, closed).periodType());
        List<Item> items = new ArrayList<>();
        Optional<DividendStatementRecord> current = repository.findActiveStatement(merchantId, "official", label);
        String currentId = current.map(DividendStatementRecord::id).orElse(null);
        items.add(current.map(statement -> item(CURRENT, statement.periodLabel(), statement.id(), statement.status(), null, null, null,
                        statement.payoutCent()))
                .orElseGet(() -> item(CURRENT, label, null, null,
                        repository.findOpenHold(merchantId, label).map(DividendPeriodHoldRecord::holdType).orElse(null), null, null, 0L)));
        items.addAll(jdbc.query("SELECT id, period_label, status, payout_cent FROM dividend_statements WHERE merchant_id = ? AND kind = 'official'"
                        + " AND status IN ('draft', 'rejected', 'pending_review') AND period_label <> ? ORDER BY period_start, id",
                (rs, n) -> item(UNPAID, rs.getString("period_label"), rs.getString("id"), rs.getString("status"), null, null, null,
                        rs.getLong("payout_cent")), merchantId, label));
        List<Item> payables = jdbc.query("""
                SELECT a.id, a.source_type, a.source_id, a.unpaid_amount_cent, COALESCE(s.id, ls.id) AS statement_id,
                       COALESCE(s.period_label, ls.period_label) AS period_label, COALESCE(s.status, ls.status) AS statement_status
                  FROM ar_ap_items a
                  LEFT JOIN dividend_statements s ON s.id = a.source_id
                  LEFT JOIN dividend_statement_lines l ON l.id = a.source_id
                  LEFT JOIN dividend_statements ls ON ls.id = l.statement_id
                 WHERE a.merchant_id = ? AND a.direction = 'payable' AND a.origin_item_id IS NULL AND a.unpaid_amount_cent > 0
                   AND a.source_type IN ('dividend_payout', 'dividend_reserve_released', 'dividend_reserve_withheld')
                   AND NOT (a.source_type = 'dividend_payout' AND a.source_id = ?)
                 ORDER BY a.created_at, a.id
                """, (rs, n) -> item("dividend_reserve_withheld".equals(rs.getString("source_type")) ? RESERVE : UNPAID,
                rs.getString("period_label"), rs.getString("statement_id"), rs.getString("statement_status"), null, rs.getString("id"),
                rs.getString("source_type"), rs.getLong("unpaid_amount_cent")), merchantId, currentId == null ? "" : currentId);
        payables.stream().filter(item -> UNPAID.equals(item.itemType())).forEach(items::add);
        payables.stream().filter(item -> RESERVE.equals(item.itemType())).forEach(items::add);
        return items;
    }

    private static Item item(String type, String periodLabel, String statementId, String statementStatus, String holdType, String arApItemId,
                             String sourceType, long amountCent) {
        return new Item(null, type, periodLabel, statementId, statementStatus, holdType, arApItemId, sourceType, amountCent, null, null, null,
                null, null);
    }

    /** 提交核实时的快照（期间、计算单状态与台账来源现读）；本次分红没有计算单的即该期登记了暂不分红。 */
    private List<Item> snapshot(String closeoutId) {
        return jdbc.query("""
                SELECT i.*, s.period_label, s.status AS statement_status, a.source_type
                  FROM dividend_closeout_items i
                  LEFT JOIN dividend_statements s ON s.id = i.statement_id
                  LEFT JOIN ar_ap_items a ON a.id = i.ar_ap_item_id
                 WHERE i.closeout_id = ? ORDER BY i.sort_order
                """, (rs, n) -> new Item(rs.getString("id"), rs.getString("item_type"), rs.getString("period_label"), rs.getString("statement_id"),
                rs.getString("statement_status"), CURRENT.equals(rs.getString("item_type")) && rs.getString("statement_id") == null ? "skip" : null,
                rs.getString("ar_ap_item_id"), rs.getString("source_type"), rs.getLong("amount_cent"), rs.getString("legal_nature"),
                rs.getString("handling"), rs.getString("handled_note"), rs.getString("handled_by"), nullableInstant(rs, "handled_at")), closeoutId);
    }

    private static String describe(Item item) {
        String what = ITEM_LABELS.get(item.itemType()) + (item.periodLabel() == null ? "" : " " + item.periodLabel());
        if (CURRENT.equals(item.itemType()) && item.statementId() == null) {
            what += "skip".equals(item.holdType()) ? "（本期暂不分红）" : "（正式单未出）";
        }
        String ref = item.arApItemId() != null ? "，台账 " + item.arApItemId() : item.statementId() != null ? "，计算单 " + item.statementId() : "";
        return what + ref + "，" + BigDecimal.valueOf(item.amountCent(), 2).toPlainString() + " 元";
    }

    // ── 辅助 ──────────────────────────────────────────────────

    /** 关店最后营业日当日已生效的方案版本（第十八批 AQ 段 234-R1 起方案按版本取）。 */
    private DividendPlanRecord planAt(String merchantId, LocalDate date) {
        return repository.findPlanInForce(merchantId, date)
                .orElseThrow(() -> ApiException.notFound("DIVIDEND_PLAN_NOT_FOUND", "该商户在 " + date + " 没有已生效的分红方案"));
    }

    private Closeout view(String id) {
        return withItems(jdbc.query("SELECT * FROM dividend_closeouts WHERE id = ?", this::mapHead, id).getFirst());
    }

    private Closeout lock(String id) {
        return jdbc.query("SELECT * FROM dividend_closeouts WHERE id = ? FOR UPDATE", this::mapHead, id).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("DIVIDEND_CLOSEOUT_NOT_FOUND", "关停结算登记不存在：" + id));
    }

    private Closeout withItems(Closeout head) {
        return head.withItems("open".equals(head.status()) ? liveItems(head.merchantId(), head.closedBusinessDate()) : snapshot(head.id()));
    }

    private static void requireVerified(Closeout head) {
        if (!"verified".equals(head.status())) {
            throw ApiException.conflict("DIVIDEND_CLOSEOUT_NOT_VERIFIED",
                    "关停结算须经财务、法务核实通过后才能登记处理与结案（当前状态：" + STATUS_LABELS.get(head.status()) + "）");
        }
    }

    private Closeout mapHead(ResultSet rs, int rowNum) throws SQLException {
        return new Closeout(rs.getString("id"), rs.getString("merchant_id"), rs.getObject("closed_business_date", LocalDate.class),
                rs.getString("reason"), rs.getString("status"), rs.getString("review_id"), rs.getString("created_by"),
                nullableInstant(rs, "created_at"), rs.getString("submitted_by"), nullableInstant(rs, "submitted_at"), rs.getString("closed_by"),
                nullableInstant(rs, "closed_at"), List.of());
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException error) {
            throw ApiException.badRequest("VALIDATION_FAILED", "关店最后营业日须为日期（如 2026-08-20）");
        }
    }

    private static void requireRequest(Object request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
    }

    private static String businessId(Object saved) {
        return saved instanceof Closeout closeout ? closeout.id() : "all";
    }
}
