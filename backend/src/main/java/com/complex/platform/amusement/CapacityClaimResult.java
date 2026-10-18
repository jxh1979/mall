package com.complex.platform.amusement;

// 文件版本：V0.01

import java.util.List;

/**
 * 项目硬容量原子占用结果，人数不足则不增加在场数。
 * exhaustedDimension 只可能为 project_safety 或成功时 null。
 */
public record CapacityClaimResult(
        boolean claimed,
        String exhaustedDimension,
        List<String> controlledDimensions
) {
}
