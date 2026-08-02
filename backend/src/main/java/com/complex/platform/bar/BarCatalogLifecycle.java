package com.complex.platform.bar;

import java.time.Instant;

/**
 * V262「数据存档与审批后删除」范式的公共形状：分类 / 商品 / 套餐三类目录记录的
 * 存档与删除生命周期字段、不可变更新方法完全同构，抽成自引用泛型接口，
 * 供 {@link BarPosCatalogService} 的泛型生命周期助手统一接线（消除三套逐字重复实现）。
 */
interface BarCatalogLifecycle<T extends BarCatalogLifecycle<T>> {

    String merchantId();

    Instant archivedAt();

    String deleteStatus();

    String deleteRequestedBy();

    T withArchive(Instant archivedAt, String archivedBy, Instant updatedAt);

    T withDelete(String deleteStatus, String deleteRequestedBy, Instant deleteRequestedAt,
            String deleteReason, Instant updatedAt);
}
