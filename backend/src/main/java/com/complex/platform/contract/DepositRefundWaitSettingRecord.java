package com.complex.platform.contract;

// 文件版本：V0.01

/**
 * HP-C06 押金退还等待期（BSM L1352／ENUM L252「等待期自最后一个审批完成之日起算，业务参数，按项目可配，出厂默认 30 日，须同时配置上下限」）。
 * projectId＝生效行所属：项目自有行，或 '__global__' 出厂回落行（30 日、上下限未配置为 null）；首次配置必须同时填写上下限。
 */
public record DepositRefundWaitSettingRecord(String projectId, int value, Integer minValue, Integer maxValue) {
    public static final String GLOBAL_SCOPE = "__global__";
}
