package com.complex.platform.bar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BarEntitlementRedemptionServiceTests {

    private BarRepository repository;
    private BarOperationExecutor operations;
    private BarEntitlementRedemptionService service;

    @BeforeEach
    void setUp() {
        TestSecurityActors.asPlatformAdmin();
        repository = mock(BarRepository.class);
        operations = mock(BarOperationExecutor.class);
        when(operations.requireText(anyString(), eq("verification_id"))).thenAnswer(invocation -> invocation.getArgument(0));
        when(operations.executeWrite(
                anyString(),
                any(),
                any(),
                eq(BarGiftRedemptionRecord.class),
                eq(BarWriteOperation.REDEEM_DRINK_ENTITLEMENT),
                any()
        )).thenAnswer(invocation -> ((Supplier<BarGiftRedemptionRecord>) invocation.getArgument(5)).get());
        AccessRepository accessRepository = mock(AccessRepository.class);
        AuthorizationService authorizationService = new AuthorizationService(accessRepository);
        service = new BarEntitlementRedemptionService(repository, operations, authorizationService);
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    @Test
    void redeemDrinkReturnsExistingDeliveredGiftRedemption() {
        BarGiftRedemptionRecord record = redemption("merchant_1", "delivered");
        when(repository.findGiftRedemptionByVerification("ver_1")).thenReturn(Optional.of(record));

        BarGiftRedemptionRecord result = service.redeemDrink(request("ver_1"));

        assertThat(result.id()).isEqualTo("gift_1");
        assertThat(result.status()).isEqualTo("delivered");
    }

    @Test
    void redeemDrinkRejectsMissingTicketingVerificationBridge() {
        when(repository.findGiftRedemptionByVerification("ver_missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.redeemDrink(request("ver_missing")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_GIFT_REDEMPTION_NOT_FOUND");
    }

    @Test
    void redeemDrinkRejectsCrossMerchantRedemption() {
        TestSecurityActors.asMerchantUser("merchant_b");
        when(repository.findGiftRedemptionByVerification("ver_1")).thenReturn(Optional.of(redemption("merchant_a", "delivered")));

        assertThatThrownBy(() -> service.redeemDrink(request("ver_1")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    @Test
    void redeemDrinkRejectsReversedGiftRedemption() {
        when(repository.findGiftRedemptionByVerification("ver_1")).thenReturn(Optional.of(redemption("merchant_1", "reversed")));

        assertThatThrownBy(() -> service.redeemDrink(request("ver_1")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("BAR_GIFT_REDEMPTION_REVERSED");
    }

    private BarEntitlementDrinkRedemptionRequest request(String verificationId) {
        return new BarEntitlementDrinkRedemptionRequest("gift-once", verificationId, "claim drink", java.util.List.of("att_1"));
    }

    private BarGiftRedemptionRecord redemption(String merchantId, String status) {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new BarGiftRedemptionRecord(
                "gift_1",
                "ent_1",
                "ver_1",
                "order_gift_1",
                "bar_1",
                merchantId,
                "sku_beer",
                "movement_1",
                status,
                "gift redeemed",
                now,
                now
        );
    }
}
