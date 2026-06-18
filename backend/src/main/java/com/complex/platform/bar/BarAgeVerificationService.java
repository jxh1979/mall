package com.complex.platform.bar;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class BarAgeVerificationService {

    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final BarEntryService entryService;

    public BarAgeVerificationService(IdempotencyService idempotencyService, AuditEventService auditEventService, BarEntryService entryService) {
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.entryService = entryService;
    }

    BarAgeVerificationRecord createAgeVerification(VerificationRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        try {
            return idempotencyService.execute(
                    "createBarAgeVerification",
                    key,
                    request,
                    () -> {
                        BarAgeVerificationRecord record = entryService.createAgeVerification(request);
                        if ("passed".equals(record.result()) || "accepted".equals(record.result())) {
                            auditEventService.recordSuccess("bar.age_verification.passed", "bar.entry.age_verify", "bar_age_verification", record.id(), null, key, Map.of("result", record.result()));
                        } else {
                            auditEventService.recordFailure("bar.age_verification.rejected", "bar.entry.age_verify", "bar_age_verification", record.id(), null, key, "AGE_VERIFICATION_REJECTED");
                        }
                        return record;
                    },
                    BarAgeVerificationRecord.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("bar.age_verification.rejected", "bar.entry.age_verify", "bar_age_verification", null, null, key, codeOf(exception));
            throw exception;
        }
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }
}
