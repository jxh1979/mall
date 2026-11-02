package com.complex.platform.facility.dto;

// 文件版本：V0.01

/** 第十八批 M 段第 117 条（BSM:1380）：工单登记用料、标明费用承担方、承担方争议三类请求（前两类设施工单与商户服务工单共用）。 */
public final class WorkOrderMaterialRequests {

    private WorkOrderMaterialRequests() {
    }

    /** 登记用料：从所选库位出库的物料与数量（正整数）。 */
    public record Issue(String locationId, String itemId, Integer quantity, String idempotencyKey) {}

    /** 标明费用承担方：property 物业承担、merchant 商户承担（须带商户）。 */
    public record Bearer(String costBearer, String merchantId, String idempotencyKey) {}

    /** 承担方争议：主张的承担方（商户承担须带商户）、原因必填；finalApproverId 可空（按直接上级）。 */
    public record Dispute(String claimedCostBearer, String claimedMerchantId, String reason, String finalApproverId,
            String idempotencyKey) {}
}
