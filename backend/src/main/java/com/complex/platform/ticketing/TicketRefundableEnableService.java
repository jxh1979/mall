package com.complex.platform.ticketing;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AW 段 295-R1 票种开启可退（BSM:785「「票种规则明确允许退款」这一项在后台出厂默认关闭；要让某一个票种可以退款，必须逐个票种单独开启，
 * 每次开启都须经高敏权限审批并留痕，不得整批打开，也不得把原则与例外倒置」；DEC-R3B-2026-08-04-31；BSM:576「持授权 U 盘密钥的最高权限本人发起的操作，
 * 不需要经过任何审批。」）：建票一律不可退；建票后逐个票种申请，最终审批人须经角色持有票种开启可退审批权限（复核策略 specific_role），获批才置可退。
 */
@Service
public class TicketRefundableEnableService {

    static final String ACTION_TYPE = "ticket.type.refundable.enable";
    static final String OBJECT_TYPE = "ticket_type";
    static final String APPROVE_PERMISSION = "ticket.product.refundable.approve";

    /** 申请：理由必填；最终审批人除最高权限本人外必填。 */
    public record Request(String idempotencyKey, String reason, String finalApproverId) {
    }

    /** 申请结果：待审时带复核单号；最高权限本人发起即生效（复核单号为空、状态 approved）。 */
    public record TicketRefundableEnableResult(String ticketTypeId, String reviewId, String reviewStatus, boolean refundable) {
    }

    private final JdbcTemplate jdbc;
    private final TicketingRepository repository;
    private final SupervisorReviewService reviews;
    private final AuditEventService audit;
    private final AuthorizationService auth;

    public TicketRefundableEnableService(JdbcTemplate jdbc, TicketingRepository repository, SupervisorReviewService reviews,
            AuditEventService audit, AuthorizationService auth) {
        this.jdbc = jdbc;
        this.repository = repository;
        this.reviews = reviews;
        this.audit = audit;
        this.auth = auth;
    }

    /** 调用方（TicketingService 的 @Transactional 写方法）事务内执行；先锁票种行，同一票种的申请与审批回写串行。 */
    TicketRefundableEnableResult request(String ticketTypeId, Request request) {
        String reason = request == null ? null : trimToNull(request.reason());
        if (reason == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请填写开启可退的理由");
        }
        jdbc.query("SELECT 1 FROM ticket_types WHERE id = ? FOR UPDATE", rs -> { }, ticketTypeId);
        TicketTypeRecord ticketType = repository.findTicketTypeById(ticketTypeId)
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "票种不存在或已被删除"));
        if (ticketType.archivedAt() != null || !"none".equals(ticketType.deleteStatus())) {
            throw ApiException.conflict("TICKET_TYPE_NOT_CONFIGURABLE", "已存档或待删除的票种不可开启可退");
        }
        if (ticketType.refundable()) {
            throw ApiException.conflict("TICKET_TYPE_ALREADY_REFUNDABLE", "该票种已可退");
        }
        if (Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM supervisor_review_decisions
                 WHERE action_type = ? AND object_type = ? AND object_id = ? AND review_status = 'pending_review')
                """, Boolean.class, ACTION_TYPE, OBJECT_TYPE, ticketTypeId))) {
            throw ApiException.conflict("TICKET_REFUNDABLE_REQUEST_PENDING", "该票种已有待审批的开启可退申请，请等审批结果");
        }
        if (auth.isMasterAuthority(auth.currentActor())) {
            enable(ticketTypeId);
            audit.recordSuccess("ticket_type.refundable_enabled", APPROVE_PERMISSION, OBJECT_TYPE, ticketTypeId, null,
                    request.idempotencyKey(), Map.of("master_self_initiated", true, "reason", reason));
            return new TicketRefundableEnableResult(ticketTypeId, null, "approved", true);
        }
        String approver = trimToNull(request.finalApproverId());
        if (approver == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "开启可退须指定最终审批人（持票种开启可退审批权限）");
        }
        SupervisorReviewDecisionRecord review = reviews.evaluate(new SupervisorReviewEvaluateRequest(request.idempotencyKey() + ":review",
                reason, List.of(), ACTION_TYPE, OBJECT_TYPE, ticketTypeId, 0L,
                "开启可退理由：" + reason + "\n票种：" + ticketType.ticketName(), approver, List.of()));
        if (!"pending_review".equals(review.reviewStatus())) {
            throw ApiException.conflict("TICKET_REFUNDABLE_APPROVAL_REQUIRED", "票种开启可退须进入审批，不得直接生效");
        }
        return new TicketRefundableEnableResult(ticketTypeId, review.reviewId(), review.reviewStatus(), false);
    }

    /** 审批回写（与 approve 同一事务）：通过且仍不可退才置可退；驳回只留痕，票种不动。 */
    @EventListener
    @Transactional
    public void decided(SupervisorReviewDecisionRecord decision) {
        if (!OBJECT_TYPE.equals(decision.objectType()) || !ACTION_TYPE.equals(decision.actionType())) {
            return;
        }
        Map<String, Object> trail = Map.of("review_id", decision.reviewId());
        if ("approved".equals(decision.reviewStatus()) && enable(decision.objectId())) {
            audit.recordSuccess("ticket_type.refundable_enabled", APPROVE_PERMISSION, OBJECT_TYPE, decision.objectId(), null, null, trail);
        } else if ("rejected".equals(decision.reviewStatus())) {
            audit.recordSuccess("ticket_type.refundable_enable.rejected", APPROVE_PERMISSION, OBJECT_TYPE, decision.objectId(), null, null, trail);
        }
    }

    private boolean enable(String ticketTypeId) {
        return jdbc.update("UPDATE ticket_types SET refundable = TRUE, updated_at = ? WHERE id = ? AND refundable = FALSE",
                Timestamp.from(Instant.now()), ticketTypeId) > 0;
    }
}
