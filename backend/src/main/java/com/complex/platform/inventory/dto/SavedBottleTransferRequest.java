package com.complex.platform.inventory.dto;

import java.util.List;

/**
 * 存酒转让请求(A 客人把存酒转给 B 客人;或转赠好友)。
 * toNameOrNickname 必填(受让人姓名/昵称,供后续取酒核身);toPhone 可空(加密+盲索引留存);
 * transferQuantity 可空(缺省转让全部剩余);reason 转让事由。
 */
public record SavedBottleTransferRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String toNameOrNickname,
        String toPhone,
        Integer transferQuantity,
        String reason,
        // 出让人手机短信验证码(店员收集原持有人手机收到的码录入,授权本次转让)。
        String verificationCode
) {
}
