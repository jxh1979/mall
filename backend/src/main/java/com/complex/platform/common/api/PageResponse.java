package com.complex.platform.common.api;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        int page,
        int pageSize,
        long total
) {

    /**
     * 内存分页(接口默认/测试双子用): 对已取全量 all 做 page/size 切片, total 取全量真实大小。
     * 生产 Jdbc 各自走 SQL COUNT + LIMIT/OFFSET, 不经此方法。
     */
    public static <T> PageResponse<T> of(java.util.List<T> all, int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 500 : size;
        int from = (int) Math.min((long) (safePage - 1) * safeSize, all.size());
        int to = (int) Math.min((long) from + safeSize, all.size());
        return new PageResponse<>(all.subList(from, to), safePage, safeSize, all.size());
    }
}
