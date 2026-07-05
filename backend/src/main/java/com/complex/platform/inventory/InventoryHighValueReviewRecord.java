package com.complex.platform.inventory;

import java.time.Instant;
import java.util.List;

/**
 * R5 T6（库存域内自建）高价值复核（inventory_high_value_reviews）。
 *
 * <p>裁决②：高价值复核 = 库存域内自建，不走 governance/SupervisorReview。报损（damage_report）/
 * 盘点差异（stock_variance）/存酒作废（saved_bottle_void）命中 {@code is_high_value} 时，<b>不直接生效</b>，
 * 改为建本复核（{@code pending}）并要求<b>照片 + 视频双证据</b>（{@code photoAttachmentIds} 与
 * {@code videoAttachmentIds} 各非空）。主管 approve 后动作才生效，reject 则回滚（保持原态/不扣减）。
 *
 * <p>{@code subjectType} 取值：{@code damage_report}/{@code stock_variance}/{@code saved_bottle_void}；
 * {@code subjectId} 关联 movement/adjustment/saved_bottle。{@code reviewStatus}：pending/approved/rejected。
 */
public record InventoryHighValueReviewRecord(
        String id,
        String subjectType,
        String subjectId,
        List<String> photoAttachmentIds,
        List<String> videoAttachmentIds,
        String reviewStatus,
        String reviewerUserId,
        String reason,
        Instant createdAt,
        Instant reviewedAt,
        // C3：复核单归属商户（建单时由 createPending 从 currentMerchantScope 落库），
        // reviewInternal 据此拒绝跨商户翻转他人复核单（含 reject 分支）。历史行由 V237 从 subject 回填。
        String merchantId
) {
}
