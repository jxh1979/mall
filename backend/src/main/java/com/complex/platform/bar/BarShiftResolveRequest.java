package com.complex.platform.bar;

// 文件版本：V0.01

/**
 * 第十八批 R 段第 142 条 142-R1：财务复核平账请求。短款办理即确认已由责任人当场足额补足（BSM:1081「短款补足即为处理完毕」），
 * 不收金额；长款须填录像查明的原因（BSM:1081「长款同样必须登记，并由财务调取视频录像查明差额产生的原因」）。
 * 第十八批 R 段第 142 条 142-R4：有成员（同一钱箱多人当班）的班次短款可带责任人 responsibleUserId（须是本班次当班收银员），不带即当班组共同补足
 * （BSM:1081「单人当班或能够明确定位到具体收银员时，由该收银员补足；多人共同使用同一钱箱时，由整个当班组共同补足。」）。
 */
public record BarShiftResolveRequest(
        String idempotencyKey,
        String overageCause,
        String responsibleUserId
) {
}
