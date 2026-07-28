package com.complex.platform.terminal;

import java.time.Instant;

/**
 * 灰度试点白名单条目（V342）。{@code targetType} 取既有 scope 维度
 * （project / merchant / business_system / module / version / device），
 * {@code targetId} 与该维度在请求上下文中的取值逐字比较。
 *
 * <p>删除=软删除铁律：移出白名单写 {@code archivedAt/archiveReason/archivedBy}，行保留可恢复；
 * 求值与列表只认 {@code archivedAt IS NULL} 的条目。
 */
public record TerminalCanaryTargetRecord(
        String id,
        String rolloutId,
        String targetType,
        String targetId,
        String reason,
        Instant createdAt,
        Instant updatedAt,
        Instant archivedAt,
        String archiveReason,
        String archivedBy
) {
}
