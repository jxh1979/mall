package com.complex.platform.dividend.dto;

// 文件版本：V0.01

import java.time.LocalDate;
import java.util.List;

/**
 * 第十八批 AX 段第 273 条 273-R1 登记公司向酒吧的借款（PGM:436「任何公司向酒吧提供的借款，出借时须写明金额、利率、还款期限与还款顺序」）。merchantId 酒吧商户；
 * 出借方为平台内主体填 lenderEntityId（lenderName 不填取主体名），外部公司只填 lenderName；principalCent 本金、annualRateBps 年利率（万分比）、
 * interestCent 借款协议约定的到期应付利息、lentOn 出借日、dueDate 还款期限、repayOrder 还款顺序必填；note 备注与 evidenceAttachmentIds 凭证可选。
 */
public record DividendLoanRequest(
        String idempotencyKey,
        String merchantId,
        String lenderEntityId,
        String lenderName,
        Long principalCent,
        Integer annualRateBps,
        Long interestCent,
        LocalDate lentOn,
        LocalDate dueDate,
        Integer repayOrder,
        String note,
        List<String> evidenceAttachmentIds) {
}
