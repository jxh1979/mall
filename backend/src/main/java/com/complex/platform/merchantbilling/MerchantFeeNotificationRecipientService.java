package com.complex.platform.merchantbilling;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.merchantbilling.dto.MerchantFeeNotificationRecipientRequest;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

@Service
public class MerchantFeeNotificationRecipientService {

    private static final Set<String> FEE_EVENTS = Set.of("rent", "utilities", "property", "renewal");

    private final MerchantFeeNotificationRecipientRepository repository;

    public MerchantFeeNotificationRecipientService(MerchantFeeNotificationRecipientRepository repository) {
        this.repository = repository;
    }

    public PageResponse<MerchantFeeNotificationRecipientRecord> list(String merchantId) {
        String normalizedMerchantId = requireText(merchantId, "merchant_id");
        List<MerchantFeeNotificationRecipientRecord> records =
                repository.listFeeNotificationRecipients(normalizedMerchantId);
        return new PageResponse<>(records, 1, 200, records.size());
    }

    public MerchantFeeNotificationRecipientRecord get(String id) {
        return repository.findFeeNotificationRecipientById(requireText(id, "id"))
                .orElseThrow(this::notFound);
    }

    @Transactional
    public MerchantFeeNotificationRecipientRecord upsert(MerchantFeeNotificationRecipientRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        Instant now = Instant.now();
        return repository.upsertFeeNotificationRecipient(new MerchantFeeNotificationRecipientRecord(
                "mfnr_" + UUID.randomUUID().toString().replace("-", ""),
                requireText(request.merchantId(), "merchant_id"),
                normalizeFeeEvent(request.feeEvent()),
                requireText(request.recipientName(), "recipient_name"),
                requireText(request.recipientPhone(), "recipient_phone"),
                "active",
                requireText(request.operatorUserId(), "operator_user_id"),
                now,
                now,
                null
        ));
    }

    @Transactional
    public MerchantFeeNotificationRecipientRecord deactivate(String id) {
        return repository.deactivateFeeNotificationRecipient(requireText(id, "id"))
                .orElseThrow(this::notFound);
    }

    private String normalizeFeeEvent(String feeEvent) {
        String normalized = trimToNull(feeEvent);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (!FEE_EVENTS.contains(normalized)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "fee_event is invalid");
        }
        return normalized;
    }

    private ApiException notFound() {
        return ApiException.notFound(
                "MERCHANT_FEE_NOTIFICATION_RECIPIENT_NOT_FOUND",
                "费用通知接收人不存在或已被删除");
    }
}
