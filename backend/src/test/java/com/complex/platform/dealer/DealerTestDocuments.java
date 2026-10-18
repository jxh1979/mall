package com.complex.platform.dealer;

// 文件版本：V0.01

import com.complex.platform.dealer.dto.DealerAdmissionSubmitRequest;
import com.complex.platform.evidence.EvidenceEventService;
import java.util.List;
import org.springframework.mock.web.MockMultipartFile;

final class DealerTestDocuments {
    static DealerRecord submit(DealerService service, EvidenceEventService evidence, String dealerId, String key) {
        return service.submitDealer(dealerId, documents(evidence, dealerId, key));
    }

    static DealerAdmissionSubmitRequest documents(EvidenceEventService evidence, String dealerId, String key) {
        return new DealerAdmissionSubmitRequest(key, List.of(upload(evidence, dealerId, "company")), List.of(upload(evidence, dealerId, "contract")),
                List.of(upload(evidence, dealerId, "invoice")), List.of(upload(evidence, dealerId, "bank")));
    }

    private static String upload(EvidenceEventService evidence, String dealerId, String role) {
        byte[] png = java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl6JQAAAABJRU5ErkJggg==");
        return evidence.uploadAttachment(new MockMultipartFile("file", role + ".png", "image/png", png), "dealer_admission", dealerId, role).id();
    }
}
