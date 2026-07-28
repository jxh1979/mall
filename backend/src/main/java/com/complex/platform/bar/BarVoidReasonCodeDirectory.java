package com.complex.platform.bar;

import java.util.List;

/**
 * 退酒原因码字典（V351）。出厂八条由用户 2026-07-28 逐条批准，后台可增删改（删=软删留理由）。
 *
 * <p>替换 V345 的「正则形状校验」：形状校验下店员打 `asdf` 照样过闸，事后按原因码聚合退酒
 * （哪家店质量问题多、哪个班次上错单多）全是垃圾数据。
 */
public interface BarVoidReasonCodeDirectory {

    /** 该码是否为字典里的**生效**码（下线的码历史单据仍引用，但不能再选）。 */
    boolean isActiveReasonCode(String code);

    /** 列字典项；activeOnly=true 只列生效码（前端下拉用），false 含已下线（后台维护用）。 */
    List<BarVoidReasonCodeRecord> listReasonCodes(boolean activeOnly);

    /** 新增或修改一条（按 code 覆盖中文标签与排序；已下线的码重新保存即恢复生效）。 */
    BarVoidReasonCodeRecord saveReasonCode(String code, String labelZh, Integer sortOrder, String actorId);

    /** 下线一条（软删铁律：置 inactive + 留理由，不物理删——历史退酒单还引用着）。 */
    BarVoidReasonCodeRecord archiveReasonCode(String code, String archiveReason, String actorId);
}
