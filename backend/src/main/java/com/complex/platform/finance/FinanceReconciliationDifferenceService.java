package com.complex.platform.finance;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.staff.EmployeeNoticeRecord;
import com.complex.platform.access.staff.JdbcStaffRepository;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.evidence.EvidenceAttachmentValidator;
import com.complex.platform.finance.FinanceRepository.ReconciliationEscalationCandidate;
import com.complex.platform.finance.FinanceRepository.ReconciliationStageLimit;
import com.complex.platform.finance.dto.CloseReconciliationDifferenceRequest;
import com.complex.platform.finance.dto.IdentifyReconciliationDifferenceRequest;
import com.complex.platform.finance.dto.ReconciliationStageLimitRequest;
import com.complex.platform.gate.GuardedAction;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewRepository;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 Y 段第 181 条 181-R1（ENUM:191「差异处理状态 待识别、待处理、待复核、已关闭。……复核环节属业务审批，沿用平台治理地图「全平台审批通用时限」条。
 * 各阶段由财务岗全程负责。」；BSM:1729「对不上的进差异处理，按原因定位、复核后处理」；ENUM:202「复核人须比对原件与录入值后签字；无原件的录入不得进入
 * 对账结论，只能挂为待补凭证。」；PGM:528「任一节点驳回则整单退回起草人」）：财务对账差异四态——登记进待识别（{@link FinanceService} 登记），
 * 识别写原因转待处理，提交复核进统一审批转待复核，审批通过才关闭、驳回退回待处理。负责人按办理人记（登记人、识别人）。
 * 第十八批 Y 段第 181 条 181-R2（ENUM:191「待识别与待处理两个阶段各自的停留时限为业务参数，按项目可配，出厂默认 1 个工作日，须同时配置上下限；
 * 超时未推进的，系统自动提醒并升级到上一级负责人。」）：两阶段停留时限按项目配置，超时扫描见 {@link #escalateOverdue}。
 */
@Service
public class FinanceReconciliationDifferenceService {

    static final String REVIEW_ACTION = "finance.reconciliation_difference.close";
    static final String REVIEW_OBJECT = "finance_reconciliation_difference";
    private static final String PERM = "finance.reconciliation.resolve";
    // 181-R2：停留时限配置权限、两阶段与中文名、ENUM:191「出厂默认 1 个工作日」（项目未配置该阶段时）。
    private static final String POLICY_PERM = "finance.reconciliation.policy.manage";
    private static final List<String> STAGES = List.of("pending_identification", "pending_handling");
    private static final Map<String, String> STAGE_LABELS = Map.of("pending_identification", "待识别", "pending_handling", "待处理");
    private static final int DEFAULT_WORKING_DAYS = 1;
    private static final AuditedOperation SAVE_LIMIT = AuditedOperation.of("finance.reconciliation_stage_limit.saved",
            "finance.reconciliation_stage_limit.save_failed", POLICY_PERM, "finance_reconciliation_stage_limit");
    // 第十八批第 13 条（BSM 第 1597 行「对账差异认定结果」）：认定结果短信里的差异类型中文名（自 FinanceService 原一步关闭处移来）。
    private static final Map<String, String> DIFFERENCE_TYPE_LABELS = Map.of(
            "amount", "金额差异", "count", "数量差异", "status", "状态差异", "missing", "缺失差异");

    private final FinanceRepository repository;
    private final FinanceService finance;
    private final AuthorizationService authorizationService;
    private final AuditedOperationExecutor operations;
    private final AuditEventService auditEventService;
    private final SupervisorReviewService reviews;
    private final EvidenceAttachmentValidator attachments;
    private final SupervisorReviewRepository supervisorReviews;
    private final JdbcStaffRepository staffRepository;

    public FinanceReconciliationDifferenceService(FinanceRepository repository, FinanceService finance,
            AuthorizationService authorizationService, AuditedOperationExecutor operations, AuditEventService auditEventService,
            SupervisorReviewService reviews, EvidenceAttachmentValidator attachments, SupervisorReviewRepository supervisorReviews,
            JdbcStaffRepository staffRepository) {
        this.repository = repository;
        this.finance = finance;
        this.authorizationService = authorizationService;
        this.operations = operations;
        this.auditEventService = auditEventService;
        this.reviews = reviews;
        this.attachments = attachments;
        this.supervisorReviews = supervisorReviews;
        this.staffRepository = staffRepository;
    }

    /** 识别：只收待识别，写识别原因转待处理，负责人改为办理人；范围闸在幂等之前。 */
    @GuardedAction(capability = PERM)
    @Transactional
    public ReconciliationDifferenceRecord identify(String differenceId, IdentifyReconciliationDifferenceRequest request) {
        finance.requireReconciliationDifferenceInScope(differenceId, PERM);
        return write("identifyFinanceReconciliationDifference:" + differenceId, request == null ? null : request.idempotencyKey(), request,
                "finance.reconciliation.difference_identified", "finance.reconciliation.difference_identify_failed", () -> {
                    finance.requirePlatformReconciliationAccess();
                    String cause = requireText(require(request).causeNote(), "cause_note");
                    if (!repository.identifyReconciliationDifference(differenceId, cause, authorizationService.currentActor().id(), Instant.now())) {
                        throw statusConflict();
                    }
                    return find(differenceId);
                });
    }

    /**
     * 提交关闭复核：只收待处理；所附凭证须真实存在并追加进原件附件（补原件即在此补）；人工录入合并后仍无原件拒 409（ENUM:202「只能挂为待补凭证」）；
     * 否则进统一审批（每笔都进待审，动作名以 finance. 开头按涉钱 1 个工作日），记复核单号转待复核。范围闸在幂等之前。
     */
    @GuardedAction(capability = PERM)
    @Transactional
    public ReconciliationDifferenceRecord submitReview(String differenceId, CloseReconciliationDifferenceRequest request) {
        finance.requireReconciliationDifferenceInScope(differenceId, PERM);
        return write("closeFinanceReconciliationDifference:" + differenceId, request == null ? null : request.idempotencyKey(), request,
                "finance.reconciliation.difference_review_submitted", "finance.reconciliation.difference_review_submit_failed", () -> {
                    finance.requirePlatformReconciliationAccess();
                    CloseReconciliationDifferenceRequest actual = require(request);
                    String closeReason = requireText(actual.closeReason(), "close_reason");
                    ReconciliationDifferenceRecord difference = find(differenceId);
                    if (!"pending_handling".equals(difference.status())) {
                        throw statusConflict();
                    }
                    List<String> added = actual.evidenceAttachmentIds() == null ? List.of() : actual.evidenceAttachmentIds();
                    if (!added.isEmpty()) {
                        attachments.requireExisting(added, "evidence_attachment_ids");
                    }
                    LinkedHashSet<String> merged = new LinkedHashSet<>(difference.evidenceAttachmentIds());
                    merged.addAll(added);
                    List<String> evidence = List.copyOf(merged);
                    if ("manual".equals(difference.source()) && evidence.isEmpty()) {
                        throw ApiException.conflict("RECONCILIATION_EVIDENCE_PENDING", "人工录入未附渠道对账单原件，只能挂为待补凭证，补上原件后再提交复核");
                    }
                    String summary = "对账差异 " + difference.id() + "（" + typeLabel(difference) + "，差额 "
                            + BigDecimal.valueOf(difference.amountCent(), 2).toPlainString() + " 元）处理结论：" + closeReason;
                    String reviewId = reviews.evaluate(new SupervisorReviewEvaluateRequest(actual.idempotencyKey() + ":review", closeReason, evidence,
                            REVIEW_ACTION, REVIEW_OBJECT, difference.id(), difference.amountCent(), summary,
                            trimToNull(actual.finalApproverId()), List.of())).reviewId();
                    if (!repository.submitReconciliationDifferenceReview(difference.id(), closeReason, evidence, reviewId, Instant.now())) {
                        throw statusConflict();
                    }
                    return find(differenceId);
                });
    }

    /**
     * 审批结论回写：通过才关闭（记终审人），照原一步关闭向往来商户发认定结果短信（第十八批第 13 条）；驳回退回待处理，可再提交复核，
     * 审计带驳回意见。申请人与受益人不得审批由统一审批引擎保证。
     */
    @EventListener
    @Transactional
    public void decided(SupervisorReviewDecisionRecord decision) {
        if (!REVIEW_ACTION.equals(decision.actionType()) || !REVIEW_OBJECT.equals(decision.objectType())) {
            return;
        }
        String differenceId = decision.objectId();
        Instant now = Instant.now();
        if ("approved".equals(decision.reviewStatus())) {
            repository.closeReconciliationDifference(differenceId, decision.reviewId(), authorizationService.currentActor().id(), now)
                    .ifPresent(closeReason -> {
                        ReconciliationDifferenceRecord difference = find(differenceId);
                        if (difference.businessRef() != null) {
                            repository.findArApPaymentById(difference.businessRef())
                                    .flatMap(payment -> repository.findArApItemById(payment.arApItemId()))
                                    .ifPresent(item -> finance.notifyMerchantAmount(item.counterpartyId(), "reconciliation_difference",
                                            difference.amountCent(), typeLabel(difference) + "；认定结果：" + closeReason, null,
                                            item.projectId(), difference.id()));
                        }
                        auditEventService.recordSuccess("finance.reconciliation.difference_closed", PERM, REVIEW_OBJECT, differenceId, null, null,
                                Map.of("review_id", decision.reviewId(), "close_reason", closeReason));
                    });
        } else if ("rejected".equals(decision.reviewStatus())
                && repository.returnReconciliationDifferenceToHandling(differenceId, decision.reviewId(), now)) {
            String comment = auditEventService.searchOperationEvents("supervisor_review", decision.reviewId(), null,
                            "governance.supervisor_review.decided", null, null, 1, null).stream()
                    .findFirst().map(event -> event.metadata().get("comment")).map(String::valueOf).orElse("");
            auditEventService.recordSuccess("finance.reconciliation.difference_review_rejected", PERM, REVIEW_OBJECT, differenceId, null, null,
                    Map.of("review_id", decision.reviewId(), "comment", comment));
        }
    }

    /**
     * 18Y 复核修复（ENUM:202「渠道接入前的人工录入必须同时上传渠道提供的对账单原件作为凭证，复核人须比对原件与录入值后签字」）：不带商户编号的账号回读
     * 渠道对账单原件时，引用该附件的对账差异里有一笔——本人是其当前关闭复核单的审批人、被转发人，或持 finance.reconciliation.view 且可触及其所属批次
     * （口径同 {@link FinanceService#requireReconciliationDifferenceInScope}）——即可看（证据附件回读调）。
     */
    public boolean canViewStatement(String attachmentId) {
        String actorId = authorizationService.currentActor().id();
        for (String differenceId : repository.listReconciliationDifferenceIdsByEvidenceAttachment(attachmentId)) {
            String reviewId = find(differenceId).reviewId();
            if (reviewId != null && supervisorReviews.listChainNodes(reviewId).stream()
                    .anyMatch(node -> actorId.equals(node.approverId()) || actorId.equals(node.forwardedTo()))) {
                return true;
            }
            try {
                finance.requireReconciliationDifferenceInScope(differenceId, "finance.reconciliation.view");
                return true;
            } catch (ApiException outOfScope) {
                // 不可触及这笔差异的批次：看下一笔。
            }
        }
        return false;
    }

    /**
     * 第十八批 Y 段第 181 条 181-R2：项目两阶段停留时限（待识别、待处理顺序）；未配置的阶段按出厂默认 1 个工作日返回、上下限为空。
     * 项目须在 finance.reconciliation.policy.manage 可触及范围内（不填取本人项目）。
     */
    public List<ReconciliationStageLimit> stageLimits(String projectId) {
        String project = authorizationService.requireReachableProject(projectId, POLICY_PERM, AuthorizationService.PROJECT_OUT_OF_REACH);
        List<ReconciliationStageLimit> saved = repository.listReconciliationStageLimits(project);
        return STAGES.stream().map(stage -> saved.stream().filter(limit -> limit.stage().equals(stage)).findFirst()
                .orElse(new ReconciliationStageLimit(project, stage, DEFAULT_WORKING_DAYS, null, null))).toList();
    }

    /** 181-R2：配置项目某一阶段的停留时限——时限与上下限须同时填写，1 ≤ 下限 ≤ 时限 ≤ 上限（工作日），否则 400；范围闸在幂等之前。 */
    public ReconciliationStageLimit saveStageLimit(String projectId, ReconciliationStageLimitRequest request) {
        String project = authorizationService.requireReachableProject(projectId, POLICY_PERM, AuthorizationService.PROJECT_OUT_OF_REACH);
        return operations.executeWrite("saveFinanceReconciliationStageLimit:" + project, request == null ? null : request.idempotencyKey(),
                request, ReconciliationStageLimit.class, SAVE_LIMIT, () -> {
                    ReconciliationStageLimit limit = validated(project, require(request));
                    repository.saveReconciliationStageLimit(limit, authorizationService.currentActor().id());
                    return limit;
                }, saved -> project, Map.of());
    }

    /**
     * 181-R2（ENUM:191「待识别与待处理两个阶段各自的停留时限为业务参数，按项目可配，出厂默认 1 个工作日，须同时配置上下限；超时未推进的，系统自动提醒并
     * 升级到上一级负责人。」）：{@link FinanceReconciliationEscalationJob} 的业务规则，系统作业没有登录人，不走员工范围闸。待识别、待处理的差异自进入本阶段起
     * 按所属项目该阶段的时限（工作日，口径同统一审批）到期；到期的本阶段只升级一次（条件写入），给当前负责人发员工系统通知，负责人有直接上级的改为上一级
     * 并同样通知，没有上级的只提醒。待复核属统一审批，由 {@link SupervisorReviewService#escalateOverdue} 按 PGM:529 处理，不在此扫。返回本轮处理的差异数。
     * ponytail: 每条待扫差异各查一次假期表；待扫差异多到成为负担时按项目与阶段缓存到期时刻。
     */
    @Transactional
    public int escalateOverdue(Instant now) {
        int handled = 0;
        for (ReconciliationEscalationCandidate due : repository.listReconciliationEscalationCandidates()) {
            int days = due.workingDays() == null ? DEFAULT_WORKING_DAYS : due.workingDays();
            if (reviews.addWorkingDays(due.stageEnteredAt(), days).isAfter(now)) {
                continue;
            }
            String from = due.responsibleUserId();
            String to = supervisorReviews.findDirectSuperior(from).orElse(null);
            if (!repository.markReconciliationDifferenceEscalated(due.id(), due.status(), to, now)) {
                continue;
            }
            String title = "对账差异超时：" + due.id();
            String body = "对账差异 " + due.id() + " 处于" + STAGE_LABELS.get(due.status()) + "已超过停留时限（" + days + " 个工作日）仍未推进，"
                    + (to == null ? "负责人没有上一级，仅提醒。" : "已升级给上一级负责人 " + to + "。");
            int notified = notify(from, title, body, now) + (to == null ? 0 : notify(to, title, body, now));
            auditEventService.recordSuccess("finance.reconciliation_difference.escalated", PERM, REVIEW_OBJECT, due.id(), null, null,
                    Map.of("stage", due.status(), "from", from, "to", to == null ? from : to, "notified", notified));
            handled++;
        }
        return handled;
    }

    /** 181-R2：给该账号的员工档案发一条员工系统通知；没有员工档案的不发，返回发出条数。 */
    private int notify(String userId, String title, String body, Instant now) {
        return staffRepository.findProfileByUserId(userId).map(profile -> {
            staffRepository.saveNotice(new EmployeeNoticeRecord("notice_" + UUID.randomUUID().toString().replace("-", ""), profile.id(),
                    "system_notice", title, body, false, "pending", null, "system", now, now));
            return 1;
        }).orElse(0);
    }

    private static ReconciliationStageLimit validated(String project, ReconciliationStageLimitRequest request) {
        if (!STAGES.contains(request.stage())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "阶段只能是待识别（pending_identification）或待处理（pending_handling）");
        }
        Integer days = request.workingDays();
        Integer min = request.minWorkingDays();
        Integer max = request.maxWorkingDays();
        if (days == null || min == null || max == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "停留时限与时限上下限须同时填写");
        }
        if (min < 1 || min > days || days > max) {
            throw ApiException.badRequest("VALIDATION_FAILED", "须满足 1 ≤ 时限下限 ≤ 停留时限 ≤ 时限上限（工作日）");
        }
        return new ReconciliationStageLimit(project, request.stage(), days, min, max);
    }

    private ReconciliationDifferenceRecord write(String operationId, String idempotencyKey, Object request, String successEvent,
            String failureEvent, Supplier<ReconciliationDifferenceRecord> action) {
        return operations.executeWrite(operationId, idempotencyKey, request, ReconciliationDifferenceRecord.class,
                AuditedOperation.of(successEvent, failureEvent, PERM, REVIEW_OBJECT), action,
                value -> value instanceof ReconciliationDifferenceRecord record ? record.id() : null, Map.of());
    }

    private ReconciliationDifferenceRecord find(String differenceId) {
        return repository.findReconciliationDifferenceById(differenceId)
                .orElseThrow(() -> ApiException.notFound("RECONCILIATION_DIFFERENCE_NOT_FOUND", "对账差异不存在或已被删除"));
    }

    private static <T> T require(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private static ApiException statusConflict() {
        return ApiException.conflict("RECONCILIATION_DIFFERENCE_STATUS_CONFLICT", "对账差异当前状态不能办理该步骤，请刷新后按待识别、待处理、待复核的顺序办理");
    }

    private static String typeLabel(ReconciliationDifferenceRecord difference) {
        return DIFFERENCE_TYPE_LABELS.getOrDefault(difference.differenceType(), difference.differenceType());
    }
}
