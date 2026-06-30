package com.complex.platform.merchantbilling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.merchantbilling.dto.MerchantFeeNotificationRecipientRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MerchantFeeNotificationRecipientServiceTests {

    private InMemoryRecipientRepository repository;
    private MerchantFeeNotificationRecipientService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRecipientRepository();
        service = new MerchantFeeNotificationRecipientService(repository);
    }

    @Test
    void upsertCreatesActiveExactRecipientAndListReturnsIt() {
        MerchantFeeNotificationRecipientRecord saved = service.upsert(new MerchantFeeNotificationRecipientRequest(
                "merchant_1",
                " Rent ",
                " Finance ",
                " 15577000001 ",
                "staff_finance"
        ));

        assertThat(saved)
                .extracting(
                        MerchantFeeNotificationRecipientRecord::merchantId,
                        MerchantFeeNotificationRecipientRecord::feeEvent,
                        MerchantFeeNotificationRecipientRecord::recipientName,
                        MerchantFeeNotificationRecipientRecord::recipientPhone,
                        MerchantFeeNotificationRecipientRecord::status,
                        MerchantFeeNotificationRecipientRecord::createdBy
                )
                .containsExactly("merchant_1", "rent", "Finance", "15577000001", "active", "staff_finance");
        assertThat(service.list("merchant_1").items()).containsExactly(saved);
    }

    @Test
    void upsertAllowsNullFeeEventAsMerchantDefault() {
        MerchantFeeNotificationRecipientRecord saved = service.upsert(new MerchantFeeNotificationRecipientRequest(
                "merchant_1",
                " ",
                " Finance ",
                " 15577000002 ",
                "staff_finance"
        ));

        assertThat(saved.feeEvent()).isNull();
        assertThat(repository.findActiveFeeNotificationRecipient("merchant_1", null))
                .get()
                .extracting(MerchantFeeNotificationRecipientRecord::recipientPhone)
                .isEqualTo("15577000002");
    }

    @Test
    void upsertRejectsInvalidFeeEvent() {
        assertThatThrownBy(() -> service.upsert(new MerchantFeeNotificationRecipientRequest(
                "merchant_1",
                "bar",
                "Finance",
                "15577000001",
                "staff_finance"
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void upsertRejectsMissingPhone() {
        assertThatThrownBy(() -> service.upsert(new MerchantFeeNotificationRecipientRequest(
                "merchant_1",
                "rent",
                "Finance",
                " ",
                "staff_finance"
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void getAndDeactivateReturnNotFoundForUnknownId() {
        assertThatThrownBy(() -> service.get("missing"))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("MERCHANT_FEE_NOTIFICATION_RECIPIENT_NOT_FOUND");
        assertThatThrownBy(() -> service.deactivate("missing"))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("MERCHANT_FEE_NOTIFICATION_RECIPIENT_NOT_FOUND");
    }

    @Test
    void deactivateMakesRecipientInactiveForResolverQueries() {
        MerchantFeeNotificationRecipientRecord saved = service.upsert(new MerchantFeeNotificationRecipientRequest(
                "merchant_1",
                "rent",
                "Finance",
                "15577000001",
                "staff_finance"
        ));

        MerchantFeeNotificationRecipientRecord inactive = service.deactivate(saved.id());

        assertThat(inactive.status()).isEqualTo("inactive");
        assertThat(inactive.deletedAt()).isNotNull();
        assertThat(repository.findActiveFeeNotificationRecipient("merchant_1", "rent")).isEmpty();
    }

    private static final class InMemoryRecipientRepository implements MerchantFeeNotificationRecipientRepository {
        private final Map<String, MerchantFeeNotificationRecipientRecord> rows = new LinkedHashMap<>();

        @Override
        public List<MerchantFeeNotificationRecipientRecord> listFeeNotificationRecipients(String merchantId) {
            return rows.values().stream()
                    .filter(row -> merchantId.equals(row.merchantId()))
                    .toList();
        }

        @Override
        public Optional<MerchantFeeNotificationRecipientRecord> findFeeNotificationRecipientById(String id) {
            return Optional.ofNullable(rows.get(id));
        }

        @Override
        public Optional<MerchantFeeNotificationRecipientRecord> findActiveFeeNotificationRecipient(String merchantId, String feeEvent) {
            return rows.values().stream()
                    .filter(row -> merchantId.equals(row.merchantId()))
                    .filter(row -> feeEvent == null ? row.feeEvent() == null : feeEvent.equals(row.feeEvent()))
                    .filter(row -> "active".equals(row.status()) && row.deletedAt() == null)
                    .findFirst();
        }

        @Override
        public MerchantFeeNotificationRecipientRecord upsertFeeNotificationRecipient(MerchantFeeNotificationRecipientRecord record) {
            rows.values().stream()
                    .filter(row -> record.merchantId().equals(row.merchantId()))
                    .filter(row -> record.feeEvent() == null ? row.feeEvent() == null : record.feeEvent().equals(row.feeEvent()))
                    .findFirst()
                    .ifPresent(existing -> rows.remove(existing.id()));
            rows.put(record.id(), record);
            return record;
        }

        @Override
        public Optional<MerchantFeeNotificationRecipientRecord> deactivateFeeNotificationRecipient(String id) {
            MerchantFeeNotificationRecipientRecord current = rows.get(id);
            if (current == null) {
                return Optional.empty();
            }
            Instant now = Instant.now();
            MerchantFeeNotificationRecipientRecord inactive = new MerchantFeeNotificationRecipientRecord(
                    current.id(),
                    current.merchantId(),
                    current.feeEvent(),
                    current.recipientName(),
                    current.recipientPhone(),
                    "inactive",
                    current.createdBy(),
                    current.createdAt(),
                    now,
                    now
            );
            rows.put(id, inactive);
            return Optional.of(inactive);
        }
    }
}
