package com.complex.platform.bar;

// 文件版本：V0.01

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.governance.MerchantTableTransferLimitRecord;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.util.List;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AB 段第 190 条 190-R2（BSM:1079「同一会话的转台次数不得超过后台配置的上限（业务参数，按酒吧可配，出厂默认 10 次，须同时配置上下限），
 * 达到上限后须主管审批才能继续转台」；决策台账 DEC-R6-19「达上限须主管审批」；PGM:528「系统按组织层级自动补全从发起人到该最终审批人之间的全部直接上级
 * 节点，逐级审批」）：超限转台申请。逐次审批、一次用一张——批准的申请只用于一次转到申请里那张目标台的转台；同一会话同时只有一张待审批。
 * 审批走统一审批一级上级（发起人直接上级），写法同工单承担方争议（WorkOrderMaterialService.openDispute/decided）。
 */
@Service
public class BarTransferLimitApprovalService {

    static final String ACTION = "bar.table_session.transfer_over_limit";
    static final String OBJECT = "bar_table_transfer_limit_approval";

    private final JdbcTemplate jdbc;
    private final SupervisorReviewService reviews;
    private final AuthorizationService auth;

    public BarTransferLimitApprovalService(JdbcTemplate jdbc, SupervisorReviewService reviews, AuthorizationService auth) {
        this.jdbc = jdbc;
        this.reviews = reviews;
        this.auth = auth;
    }

    /** 本酒吧（merchants.id＝会话 bar_id）转台次数上限；未配置按出厂 10 次（190-R1）。 */
    int limit(String barId) {
        return jdbc.queryForObject("SELECT COALESCE((SELECT table_transfer_limit FROM merchants WHERE id = ?), ?)", Integer.class,
                barId, MerchantTableTransferLimitRecord.FACTORY_DEFAULT_COUNT);
    }

    /** 同会话同目标台已批准未用的申请改为已用并返回其编号，没有返回 null。在转台事务里调用：转台失败随事务回滚、不算用掉。 */
    String consume(String sessionId, String targetTableId) {
        return jdbc.queryForList("""
                UPDATE bar_table_transfer_limit_approvals SET status = 'used', used_at = now()
                 WHERE id = (SELECT id FROM bar_table_transfer_limit_approvals
                              WHERE session_id = ? AND target_table_id = ? AND status = 'approved'
                              ORDER BY decided_at LIMIT 1 FOR UPDATE)
                RETURNING id""", String.class, sessionId, targetTableId).stream().findFirst().orElse(null);
    }

    /** 用掉的申请记上本次转台留痕编号。 */
    void linkAdjustment(String approvalId, String adjustmentId) {
        jdbc.update("UPDATE bar_table_transfer_limit_approvals SET adjustment_id = ? WHERE id = ?", adjustmentId, approvalId);
    }

    /** 本会话待审批的超限转台申请的审批单号，没有返回 null。 */
    String pendingReview(String sessionId) {
        return jdbc.queryForList("SELECT review_id FROM bar_table_transfer_limit_approvals WHERE session_id = ? AND status = 'pending'",
                String.class, sessionId).stream().findFirst().orElse(null);
    }

    /**
     * 提交超限转台申请（摘要写台位、已转次数、上限、目标台与原因），返回审批单号。独立事务：转台随后抛 409 回滚时，申请与复核单仍在，
     * 主管能看到并审批（同 AdmissionGuard.blockToReview，XM-M002-J）。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String request(BarTableSessionRecord session, BarTableRecord source, BarTableRecord target, int transferCount, int limit,
            String reason, String idempotencyKey) {
        String id = "btla_" + UUID.randomUUID();
        String reviewId = reviews.evaluate(new SupervisorReviewEvaluateRequest(idempotencyKey + ":transfer_over_limit", reason, List.of(),
                ACTION, OBJECT, id, null, "台位 " + source.tableName() + " 本会话已转台 " + transferCount + " 次，达到本酒吧上限 " + limit
                + " 次，申请转到 " + target.tableName() + "，原因：" + reason, null, List.of())).reviewId();
        jdbc.update("""
                INSERT INTO bar_table_transfer_limit_approvals (id, session_id, bar_id, merchant_id, target_table_id, transfer_count, limit_count,
                    reason, requested_by, review_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                id, session.id(), session.barId(), session.merchantId(), target.id(), transferCount, limit, reason, auth.currentActor().id(), reviewId);
        return reviewId;
    }

    /** 审批定案回写：待审批的申请改为已批准或已驳回，记定案时刻。 */
    @EventListener
    @Transactional
    public void decided(SupervisorReviewDecisionRecord d) {
        if (ACTION.equals(d.actionType()) && OBJECT.equals(d.objectType()) && List.of("approved", "rejected").contains(d.reviewStatus())) {
            jdbc.update("UPDATE bar_table_transfer_limit_approvals SET status = ?, decided_at = now() WHERE review_id = ? AND status = 'pending'",
                    d.reviewStatus(), d.reviewId());
        }
    }
}
