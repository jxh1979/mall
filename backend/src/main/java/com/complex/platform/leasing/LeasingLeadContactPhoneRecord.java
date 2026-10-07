package com.complex.platform.leasing;

// 文件版本：V0.01

/**
 * M724（治理地图第 871 行，操作员 2026-10-07 确认：「招商线索联系人的手机号另按本句处理：线索列表和操作结果一律只显示脱敏号码；
 * 经单独授权且查看权限有效的人员，可以在某一条线索上点“查看完整号码”，只显示这一条的完整号码，每点一次算查看一次并留痕，
 * 单次授权看一次即用掉。」）：「查看完整号码」的返回，只含这一条线索的编号与联系人手机号。
 */
public record LeasingLeadContactPhoneRecord(String leadId, String contactPhone) {
}
