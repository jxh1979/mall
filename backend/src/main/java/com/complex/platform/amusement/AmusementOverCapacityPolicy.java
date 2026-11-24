package com.complex.platform.amusement;

/**
 * P0 清单#6 超容处置策略（V104 amusement_over_capacity_policy，项目维一行）。
 *
 * <p>operating_capacity：运营容量（软上限）；核销后场次在场达到该值即「软超容」（告警放行，不阻断）。
 * disposal_mode：达到运营容量（软上限）时的处置方式（后台可配，BSM:1267、DEC-R7-02）——{@code block_hard} 即「记录异常事件」（出厂默认，达到软上限
 * 生成异常事件留痕）；{@code warn_only} 即「仅提示」（只在现场提示、不生成异常事件，第十八批 CF 段 409-R1）；两种方式都在核销结果里现场提示（409-R3）。
 * F-OPS-03（裁决#6）：安全容量（硬上限）=人身安全红线，<b>任何处置方式都不可旁路</b>，两者都不改变硬上限的阻断与自动停运。
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
