package com.complex.platform.amusement;

/**
 * 待回填空间的游乐项目清单项（space_id 为空）。
 *
 * <p>供运营核对哪些项目尚未录入 space_id（over_space 对这些项目相关受控对象不生效，保守放行）。</p>
 */
public record AmusementProjectSpaceBacklogRecord(
        String id,
        String projectName,
        String openStatus,
        String merchantId
) {
}
