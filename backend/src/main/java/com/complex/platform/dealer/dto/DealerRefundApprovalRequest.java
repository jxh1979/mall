package com.complex.platform.dealer.dto;

/** 为系统取消产生的待发起退款开启既有退款审批（finalApproverId 可空：按发起人直接上级链生成）。 */
public record DealerRefundApprovalRequest(String idempotencyKey, String finalApproverId) {
}
