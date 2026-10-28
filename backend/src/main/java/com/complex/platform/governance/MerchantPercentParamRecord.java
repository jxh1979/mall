package com.complex.platform.governance;

// 文件版本：V0.01

/**
 * 第十八批 C 段：酒吧（商户）按酒吧可配的百分比参数，值与上下限同配（范式同 TK-27 {@link MerchantPendingClearAlertRecord}）。
 * 未配置三者皆空；生效值取所配值，未配置取出厂值（无出厂值的为空）。
 */
public record MerchantPercentParamRecord(
        String merchantId,
        Integer valuePct,
        Integer minPct,
        Integer maxPct,
        Integer effectivePct
) {

    public enum Param {
        /** 第 15 条 BSM:1468「经手员工的爽约率超过后台配置的上限时（业务参数，按酒吧可配，须同时配置上下限），自动停用其代订权限」：HTML 未给出厂值，未配置不自动停用。 */
        STAFF_BOOKING_NO_SHOW_LIMIT("staff_booking_no_show_limit", "staff-booking-no-show-limit", "员工代订爽约率上限", null, 0),
        /** 第 17 条 BSM:2015 损耗率阈值「按酒吧可配，出厂默认 5%，须同时配置上下限」；第十八批裁决 4 标准损耗＝标准出酒量×该阈值，故下限不得为 0。 */
        POUR_LOSS_RATE("pour_loss_rate", "pour-loss-rate", "损耗率阈值", 5, 1);

        private final String column;
        private final String path;
        private final String label;
        private final Integer factoryDefault;
        private final int minFloor;

        Param(String column, String path, String label, Integer factoryDefault, int minFloor) {
            this.column = column;
            this.path = path;
            this.label = label;
            this.factoryDefault = factoryDefault;
            this.minFloor = minFloor;
        }

        /** merchants 列前缀：{column}_pct / {column}_min_pct / {column}_max_pct。 */
        public String column() {
            return column;
        }

        public String path() {
            return path;
        }

        public String label() {
            return label;
        }

        public Integer factoryDefault() {
            return factoryDefault;
        }

        public int minFloor() {
            return minFloor;
        }
    }

    public static MerchantPercentParamRecord of(String merchantId, Param param, Integer value, Integer min, Integer max) {
        return new MerchantPercentParamRecord(merchantId, value, min, max, value == null ? param.factoryDefault() : value);
    }
}
