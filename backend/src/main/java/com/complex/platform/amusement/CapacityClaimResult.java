package com.complex.platform.amusement;

import java.util.List;

/**
 * §6.4 三维度容量扣减结果。
 *
 * @param claimed             是否全部受控维度放行并已 +1（失败时已回滚已增维度，无副作用）
 * @param exhaustedDimension  失败时第一个余量耗尽的维度（session / point_device / project_safety），成功为 null
 * @param controlledDimensions 本次实际参与校验（已配置）的受控维度，便于诊断与测试
 */
public record CapacityClaimResult(
        boolean claimed,
        String exhaustedDimension,
        List<String> controlledDimensions
) {
}
