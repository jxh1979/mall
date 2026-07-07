package com.complex.platform.ops;

/** 审批决定：approved（批准并执行真删）或 rejected（驳回）。 */
public record DataCleanupApprovalRequest(String decision) {
}
