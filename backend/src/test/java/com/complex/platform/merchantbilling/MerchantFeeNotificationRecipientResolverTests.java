package com.complex.platform.merchantbilling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.governance.MerchantRecord;
import com.complex.platform.governance.PlatformRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MerchantFeeNotificationRecipientResolverTests {

    @Test
    void exactFeeEventRecipientWinsOverMerchantDefault() {
        MerchantFeeNotificationRecipientRepository repository = mock(MerchantFeeNotificationRecipientRepository.class);
        PlatformRepository platformRepository = mock(PlatformRepository.class);
        when(repository.findActiveFeeNotificationRecipient("merchant_1", "rent"))
                .thenReturn(java.util.Optional.of(recipient("rent", "15577000001")));
        when(repository.findActiveFeeNotificationRecipient("merchant_1", null))
                .thenReturn(java.util.Optional.of(recipient(null, "15577000002")));

        MerchantFeeNotificationRecipientResolver resolver =
                new MerchantFeeNotificationRecipientResolver(repository, platformRepository);

        assertThat(resolver.resolvePhone("merchant_1", "rent")).isEqualTo("15577000001");
    }

    @Test
    void merchantDefaultRecipientCoversFeeEventsWithoutExactConfig() {
        MerchantFeeNotificationRecipientRepository repository = mock(MerchantFeeNotificationRecipientRepository.class);
        PlatformRepository platformRepository = mock(PlatformRepository.class);
        when(repository.findActiveFeeNotificationRecipient("merchant_1", "property"))
                .thenReturn(java.util.Optional.empty());
        when(repository.findActiveFeeNotificationRecipient("merchant_1", null))
                .thenReturn(java.util.Optional.of(recipient(null, "15577000002")));

        MerchantFeeNotificationRecipientResolver resolver =
                new MerchantFeeNotificationRecipientResolver(repository, platformRepository);

        assertThat(resolver.resolvePhone("merchant_1", "property")).isEqualTo("15577000002");
    }

    @Test
    void missingOrInactiveConfigFallsBackToMerchantPrimaryContact() {
        MerchantFeeNotificationRecipientRepository repository = mock(MerchantFeeNotificationRecipientRepository.class);
        PlatformRepository platformRepository = mock(PlatformRepository.class);
        when(repository.findActiveFeeNotificationRecipient("merchant_1", "utilities"))
                .thenReturn(java.util.Optional.empty());
        when(repository.findActiveFeeNotificationRecipient("merchant_1", null))
                .thenReturn(java.util.Optional.empty());
        when(platformRepository.listMerchants()).thenReturn(List.of(merchant("merchant_1", "15577009999")));

        MerchantFeeNotificationRecipientResolver resolver =
                new MerchantFeeNotificationRecipientResolver(repository, platformRepository);

        assertThat(resolver.resolvePhone("merchant_1", "utilities")).isEqualTo("15577009999");
    }

    @Test
    void repositoryFailureFallsBackToMerchantPrimaryContact() {
        MerchantFeeNotificationRecipientRepository repository = mock(MerchantFeeNotificationRecipientRepository.class);
        PlatformRepository platformRepository = mock(PlatformRepository.class);
        when(repository.findActiveFeeNotificationRecipient("merchant_1", "rent"))
                .thenThrow(new IllegalStateException("config table temporarily unavailable"));
        when(platformRepository.listMerchants()).thenReturn(List.of(merchant("merchant_1", "15577009999")));

        MerchantFeeNotificationRecipientResolver resolver =
                new MerchantFeeNotificationRecipientResolver(repository, platformRepository);

        assertThat(resolver.resolvePhone("merchant_1", "rent")).isEqualTo("15577009999");
    }

    private MerchantFeeNotificationRecipientRecord recipient(String feeEvent, String phone) {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new MerchantFeeNotificationRecipientRecord(
                "mfnr_" + (feeEvent == null ? "all" : feeEvent),
                "merchant_1",
                feeEvent,
                "Billing",
                phone,
                "active",
                "staff_finance",
                now,
                now,
                null
        );
    }

    private MerchantRecord merchant(String id, String phone) {
        return new MerchantRecord(
                id,
                "Merchant " + id,
                "retail",
                null,
                null,
                null,
                "Primary Contact",
                phone,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "active",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        );
    }
}
