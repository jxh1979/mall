package com.complex.platform.inventory.dto;

// 文件版本：V0.01

import java.util.List;

/**
 * FS-017 补存/赔付后续动作共用请求体：审批（role=supervisor|finance）、结案（evidenceIds=交付/支付凭证）、
 * 平台垫付、认定员工责任（staffUserId；金额由服务端照内部结算价计，不收前端金额）、追收收回。
 */
public record SavedBottleRemedyActionRequest(
        String idempotencyKey,
        String operatorNote,
        String role,
        String reason,
        List<String> evidenceIds,
        String staffUserId
) {
}
