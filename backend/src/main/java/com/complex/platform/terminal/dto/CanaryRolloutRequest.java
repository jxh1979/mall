package com.complex.platform.terminal.dto;

/**
 * 灰度试点开/关（V342）。按 端×能力 唯一，重复提交即覆盖（天然幂等，不需幂等键）。
 * {@code active=true} 起该 端×能力 进入试点期：白名单内照常，白名单外一律关闭。
 */
public record CanaryRolloutRequest(
        String terminalType,
        String capabilityCode,
        Boolean active,
        String reason
) {
}
