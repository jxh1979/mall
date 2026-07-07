package com.complex.platform.ops;

/** 提交清理申请：dry_run 作业带上事由转为 pending_approval。 */
public record DataCleanupSubmitRequest(String reason) {
}
