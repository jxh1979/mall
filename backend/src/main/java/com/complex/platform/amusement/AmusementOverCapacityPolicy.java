package com.complex.platform.amusement;

/**
 * P0 清单#6 超容处置策略（V104 amusement_over_capacity_policy，项目维一行）。
 *
 * <p>operating_capacity：运营容量（软上限）；核销后场次在场达到该值即「软超容」（告警放行）。
 * disposal_mode：处置模式（后台可切换）——
 * {@code block_hard}（默认）硬阻断+软告警：达安全容量（硬上限）阻断、达运营容量（软上限）告警放行，均落 incident+事件；
 * {@code warn_only} 仅告警留痕不阻断：达任一上限均不阻断，但必落 incident+事件。
 *
 * <p>未配置该项目时由仓储返回禁用占位（operating_capacity=0、block_hard、enabled=false），
 * 退化为「仅硬上限阻断、无软上限告警」，与既有行为一致。
 */
public record AmusementOverCapacityPolicy(
        String projectId,
        int operatingCapacity,
        String disposalMode,
        boolean enabled
) {

    public static final String MODE_BLOCK_HARD = "block_hard";
    public static final String MODE_WARN_ONLY = "warn_only";

    /** 未配置项目的禁用占位：仅硬阻断、无软上限告警（向后兼容）。 */
    public static AmusementOverCapacityPolicy disabled(String projectId) {
        return new AmusementOverCapacityPolicy(projectId, 0, MODE_BLOCK_HARD, false);
    }

    /** 是否仅告警留痕、不阻断（硬上限亦放行）。 */
    public boolean isWarnOnly() {
        return enabled && MODE_WARN_ONLY.equals(disposalMode);
    }

    /** 是否启用了运营容量（软上限）告警。 */
    public boolean hasOperatingCapacity() {
        return enabled && operatingCapacity > 0;
    }
}
