package com.complex.platform.access;

import java.time.Instant;
import java.util.List;

/**
 * 当前 actor（或指定用户）的有效权限集。
 *
 * <p>语义对齐 {@code p0-operation-permission-matrix} §2.2 与 {@link AuthorizationService#requirePermission}：
 * DENY 优先级最高。{@code grants} 已剔除被 DENY 覆盖的权限码（实现 A），同时仍单独返回 {@code denies}
 * 供前端展示冲突来源；前端 {@code hasPermissions} 对是否预扣两种形态判定结果一致。
 *
 * <p>{@code wildcard=true} 仅当 actor 命中平台管理员桥（status=active 且 accountType=platform_admin），
 * 但前端/后端仍须让 {@code denies} 优先于 wildcard。
 */
public record EffectivePermissions(
        String userId,
        List<String> grants,
        List<String> denies,
        boolean wildcard,
        Instant computedAt
) {
}
