package com.complex.platform.governance.chain;

// 文件版本：V0.01

import java.time.Instant;

/**
 * M087（PGM-1.7.1「逐级审批」）：复核单上的一个审批节点。
 * lineCode = {@link #MAIN_LINE} 为主线，其余为平行部门线；approverKind：{@link #KIND_USER} 组织层级上的直接上级、
 * {@link #KIND_MASTER} USB 最高权限主会话、{@link #KIND_ROLE} 按角色解析（specific_role 策略）。
 */
public record ApprovalChainNode(
        String nodeId,
        String reviewId,
        String lineCode,
        int seq,
        String approverKind,
        String approverId,
        String status,
        String decidedBy,
        Instant decidedAt,
        String forwardedTo,
        String forwardedBy,
        int escalationCount
) {
    public static final String MAIN_LINE = "main";
    public static final String KIND_USER = "user";
    public static final String KIND_MASTER = "master";
    public static final String KIND_ROLE = "role";
    public static final String PENDING = "pending";
    public static final String APPROVED = "approved";
    public static final String REJECTED = "rejected";
    public static final String CANCELLED = "cancelled";

    public boolean pending() {
        return PENDING.equals(status);
    }

    public ApprovalChainNode withStatus(String newStatus, String by, Instant at) {
        return new ApprovalChainNode(nodeId, reviewId, lineCode, seq, approverKind, approverId, newStatus, by, at,
                forwardedTo, forwardedBy, escalationCount);
    }

    public ApprovalChainNode withForward(String to, String by) {
        return new ApprovalChainNode(nodeId, reviewId, lineCode, seq, approverKind, approverId, status, decidedBy, decidedAt,
                to, by, escalationCount);
    }

    public ApprovalChainNode withApprover(String newApproverId, int newEscalationCount) {
        return new ApprovalChainNode(nodeId, reviewId, lineCode, seq, approverKind, newApproverId, status, decidedBy, decidedAt,
                forwardedTo, forwardedBy, newEscalationCount);
    }
}
