package com.complex.platform.contract.dto;

import java.util.List;

/** 追加合同附件（电子签章扫描件归档）。 */
public record ContractAttachmentRequest(
        String idempotencyKey,
        List<String> attachmentIds
) {
}
