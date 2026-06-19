package com.complex.platform.access.staff;

import java.time.Instant;

/**
 * 员工通知 / 合规同意凭据（R5 T1）。审批结果、状态变更、合规同意、系统通知统一下发，
 * 员工私人手机 App 查看。必须有写入方 created_by（系统/审批人/合规模块），避免"建而无写入方"。
 * notice_type: approval_result/status_change/compliance_consent/system_notice；
 * consent_status: pending/acknowledged/consented/declined。
 */
public record EmployeeNoticeRecord(
        String id,
        String staffProfileId,
        String noticeType,
        String title,
        String body,
        boolean consentRequired,
        String consentStatus,
        Instant consentedAt,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
