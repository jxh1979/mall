package com.complex.platform.facility.dto;

// 文件版本：V0.01

import java.util.List;

/** HP-F03 楼宇设备报废：申请（审批）、实际处置登记、财务残值/损失登记三类请求（BSM L1408）。 */
public final class FacilityEquipmentScrapRequests {

    private FacilityEquipmentScrapRequests() {
    }

    /** 报废申请：原因与处置方式必填；finalApproverId 可空（按审批链沿直接上级生成）。 */
    public record Create(String reason, String disposalMethod, String finalApproverId, String idempotencyKey) {}

    /** 实际处置登记：须附已上传且绑定本报废单的凭证。 */
    public record Disposal(List<String> evidenceAttachmentIds, String disposalNote, String idempotencyKey) {}

    /** 财务登记：残值（应收回收方，分）与损失（内部核算，分）可同时有；残值大于 0 须写回收方。 */
    public record Finance(Long residualAmountCent, String residualCounterpartyId, Long lossAmountCent,
            String idempotencyKey) {}
}
