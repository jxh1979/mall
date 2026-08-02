package com.complex.platform.ticketing;

import java.time.Instant;

/**
 * V253/V260 数据存档与审批后删除范式的公共契约（docs/data-archive-delete-pattern-design-v0.1.md）：
 * 权益模板（V253 试点）/票种/价格规则（V260 镜像铺开）三张目录记录的软删字段与写回方法逐字同构，
 * 由 {@link TicketCatalogService} 的三个泛型骨架统一承接 archive → requestDelete → approveDelete 生命周期；
 * 各实体的错误码/中文文案/引用计数口径仍留在服务层各自接线处，逐字不动。
 */
interface ArchivableRecord<T extends ArchivableRecord<T>> {

    Instant archivedAt();

    String deleteStatus();

    String deleteRequestedBy();

    T withArchive(Instant archivedAt, String archivedBy, Instant updatedAt);

    T withDelete(String deleteStatus, String deleteRequestedBy, Instant deleteRequestedAt, String deleteReason,
            Instant updatedAt);
}
