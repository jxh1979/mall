// 文件版本：V0.01
package com.complex.platform.compliance;

/**
 * 第十八批 E 段第 22 条 按证照类型的宽限天数与预警提前月数（PGM:531「按证照类型可配……须同时配置上下限」）。
 * 三项一组同空或同填；读出时预警未配置按出厂 3 个月、上下限留空。
 */
public record ComplianceItemTypeConfigRecord(
        String itemType,
        Integer graceDays,
        Integer graceMinDays,
        Integer graceMaxDays,
        Integer warningMonths,
        Integer warningMinMonths,
        Integer warningMaxMonths
) {
}
