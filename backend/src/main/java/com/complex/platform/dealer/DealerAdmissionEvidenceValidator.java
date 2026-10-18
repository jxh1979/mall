package com.complex.platform.dealer;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.EvidenceAttachmentRecord;
import com.complex.platform.evidence.EvidenceEventRepository;
import com.complex.platform.evidence.EvidenceAttachmentValidator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DealerAdmissionEvidenceValidator {
    private final EvidenceEventRepository evidence;
    private final EvidenceAttachmentValidator storage;

    public DealerAdmissionEvidenceValidator(EvidenceEventRepository evidence, EvidenceAttachmentValidator storage) {
        this.evidence = evidence;
        this.storage = storage;
    }

    public void requireDocuments(DealerAdmissionRecord submission) {
        if (submission == null || submission.companyAttachmentIds() == null || submission.contractAttachmentIds() == null
                || submission.invoiceAttachmentIds() == null || submission.bankAttachmentIds() == null) {
            throw ApiException.conflict("DEALER_ADMISSION_EVIDENCE_REQUIRED", "准入须提交企业、合同、开票及银行四类资料附件");
        }
        Map<String, List<String>> groups = Map.of("company", submission.companyAttachmentIds(), "contract", submission.contractAttachmentIds(),
                "invoice", submission.invoiceAttachmentIds(), "bank", submission.bankAttachmentIds());
        Set<String> seen = new HashSet<>();
        for (var group : groups.entrySet()) {
            if (group.getValue().isEmpty()) {
                throw ApiException.conflict("DEALER_ADMISSION_EVIDENCE_REQUIRED", "四类准入资料均须关联附件");
            }
            for (String id : group.getValue()) {
                if (id == null || id.isBlank() || !seen.add(id)) {
                    throw ApiException.badRequest("DEALER_ADMISSION_EVIDENCE_INVALID", "各类准入附件不可为空或重复引用");
                }
                EvidenceAttachmentRecord attachment = evidence.findAttachmentById(id)
                        .orElseThrow(() -> ApiException.badRequest("EVIDENCE_ATTACHMENT_NOT_FOUND", "准入资料附件不存在"));
                if (!"dealer_admission".equals(attachment.businessType()) || !submission.dealerId().equals(attachment.businessId())
                        || !group.getKey().equals(attachment.attachmentRole())) {
                    throw ApiException.badRequest("DEALER_ADMISSION_EVIDENCE_BINDING_MISMATCH", "附件须绑定本经销商及对应资料类别");
                }
                storage.requireUploaded(attachment);
            }
        }
    }
}
