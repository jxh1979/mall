package com.complex.platform.amusement;

/**
 * P0 清单#6 超容处置策略（V104 amusement_over_capacity_policy，项目维一行）。
 *
 * <p>operating_capacity：运营容量（软上限）；核销后场次在场达到该值即「软超容」（告警放行，不阻断）。
 * disposal_mode：处置模式（后台可切换，仅作为 incident 留痕标注）——
 * F-OPS-03（裁决#6）：安全容量（硬上限）=人身安全红线，<b>任何处置模式都不可旁路</b>，达硬上限一律阻断并触发自动停运；
 * {@code block_hard}（默认）与 {@code warn_only} 对硬上限行为一致（均阻断），软上限均告警放行，disposal_mode 仅影响 incident 留痕语义。
 * （历史 warn_only「连硬上限也放行」语义已按 F-OPS-03 废止；原 isWarnOnly() 判定方法已移除。）
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

    /** 是否启用了运营容量（软上限）告警。 */
    public boolean hasOperatingCapacity() {
        return enabled && operatingCapacity > 0;
    }
}
