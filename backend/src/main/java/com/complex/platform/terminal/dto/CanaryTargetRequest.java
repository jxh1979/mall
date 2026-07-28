package com.complex.platform.terminal.dto;

/**
 * 灰度白名单条目新增/移出（V342）。
 * 新增：{@code targetType}（project/merchant/business_system/module/version/device）+ {@code targetId} + {@code reason}。
 * 移出：软删除，只需 {@code reason}（存档理由必填，删除=软删除铁律）。
 */
public record CanaryTargetRequest(
        String targetType,
        String targetId,
        String reason
) {
}
