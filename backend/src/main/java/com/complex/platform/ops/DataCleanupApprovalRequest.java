package com.complex.platform.ops;

/** 审批决定：approved（批准并执行真删）或 rejected（驳回）。 */
public record DataCleanupApprovalRequest(String decision, String idempotencyKey) {

    /** 服务内调用不带请求编号。 */
    public DataCleanupApprovalRequest(String decision) {
        this(decision, null);
    }
}
