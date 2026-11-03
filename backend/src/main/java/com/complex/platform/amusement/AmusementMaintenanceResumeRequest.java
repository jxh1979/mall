package com.complex.platform.amusement;

/**
 * 主管确认恢复营业入参（设计 §8.7.1）。result=pass 方可调用，项目处于 maintenance 时触发 maintenance → open。
 * 第十八批 N 段第 120 条 120-R6（BSM:993「检修完成后必须由主管确认，设施才能恢复营业。」）：确认人取当前登录主管，请求不再收确认人。
 */
public record AmusementMaintenanceResumeRequest(
        String idempotencyKey,
        String remark
) {
}
