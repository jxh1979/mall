package com.complex.platform.merchantbilling;

import com.complex.platform.governance.MerchantRecord;
import com.complex.platform.governance.PlatformRepository;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MerchantFeeNotificationRecipientResolver {

    private static final Logger log = LoggerFactory.getLogger(MerchantFeeNotificationRecipientResolver.class);
    private static final Set<String> FEE_EVENTS = Set.of("rent", "utilities", "property", "renewal");

    private final MerchantFeeNotificationRecipientRepository repository;
    private final PlatformRepository platformRepository;

    public MerchantFeeNotificationRecipientResolver(
            MerchantFeeNotificationRecipientRepository repository,
            PlatformRepository platformRepository
    ) {
        this.repository = repository;
        this.platformRepository = platformRepository;
    }

    public String resolvePhone(String merchantId, String feeEvent) {
        String normalizedMerchantId = trimToNull(merchantId);
        if (normalizedMerchantId == null) {
            return null;
        }
        String normalizedEvent = normalizeFeeEventOrNull(feeEvent);
        try {
            if (normalizedEvent != null) {
                Optional<MerchantFeeNotificationRecipientRecord> exact =
                        repository.findActiveFeeNotificationRecipient(normalizedMerchantId, normalizedEvent);
                if (exact.isPresent()) {
                    return trimToNull(exact.get().recipientPhone());
                }
            }
            Optional<MerchantFeeNotificationRecipientRecord> generic =
                    repository.findActiveFeeNotificationRecipient(normalizedMerchantId, null);
            if (generic.isPresent()) {
                return trimToNull(generic.get().recipientPhone());
            }
        } catch (RuntimeException exception) {
            log.warn(
                    "failed to resolve merchant fee notification recipient config: merchant_id={}, fee_event={}",
                    normalizedMerchantId,
                    normalizedEvent,
                    exception);
        }
        return resolveMerchantPrimaryContactPhone(normalizedMerchantId);
    }

    private String resolveMerchantPrimaryContactPhone(String merchantId) {
        if (platformRepository == null) {
            return null;
        }
        try {
            return platformRepository.listMerchants().stream()
                    .filter(merchant -> merchantId.equals(merchant.id()))
                    .findFirst()
                    .map(MerchantRecord::contactPhone)
                    .map(this::trimToNull)
                    .orElse(null);
        } catch (RuntimeException exception) {
            log.warn("failed to resolve merchant primary contact phone: merchant_id={}", merchantId, exception);
            return null;
        }
    }

    private String normalizeFeeEventOrNull(String feeEvent) {
        String normalized = trimToNull(feeEvent);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toLowerCase();
        return FEE_EVENTS.contains(normalized) ? normalized : null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
