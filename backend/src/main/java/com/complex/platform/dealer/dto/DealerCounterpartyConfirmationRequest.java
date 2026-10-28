package com.complex.platform.dealer.dto;

// 文件版本：V0.01

import java.time.Instant;
import java.util.List;

/**
 * 第十八批第 31 条 经办人登记对方确认留证。matterType：delivery_receipt 签收 / return_request 退货申请 / freight_confirmation 运费确认 /
 * delivery_resolution 交付差异 / overpayment_use 超收用途 / result_delivery 结果交付（relatedObjectId 填退款单、发票号或到账记录号）。
 * 裁决补充 15：停用后原订单尾款的收款凭证即财务核验通过的到账记录（订单号、金额、到账时间、核验人），经办人交付对方后登记结果交付。
 * 确认人由服务端取经销商档案已核验联系人，不收前端填写。
 */
public record DealerCounterpartyConfirmationRequest(String idempotencyKey, String matterType, String relatedObjectId, String content,
        Instant confirmedAt, List<String> evidenceAttachmentIds) {
}
