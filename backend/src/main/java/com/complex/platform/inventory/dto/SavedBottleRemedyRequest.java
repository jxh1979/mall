package com.complex.platform.inventory.dto;

// 文件版本：V0.01

import java.util.List;

/**
 * FS-017 作废后对客补存/赔付登记。remedyType=replacement 时须带补存瓶登记照片、新瓶底标签值与出库库位（同品同量从该酒吧库存出库）；
 * compensation 时 amountCent（分）必填。
 */
public record SavedBottleRemedyRequest(
        String idempotencyKey,
        String operatorNote,
        String remedyType,
        Long amountCent,
        String reason,
        List<String> registrationPhotoRefs,
        String bottleLabelValue,
        String locationId
) {
}
