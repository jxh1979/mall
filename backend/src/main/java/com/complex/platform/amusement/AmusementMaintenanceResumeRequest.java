package com.complex.platform.amusement;

/**
 * 主管确认恢复营业入参（设计 §8.7.1）。result=pass 方可调用，触发 maintenance → open。
 */
public record AmusementMaintenanceResumeRequest(
        String idempotencyKey,
        String supervisorConfirmedBy,
        String remark
) {
}
