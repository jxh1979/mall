package com.complex.platform.voucher;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** 纯状态机/过期算法单测：零依赖，不触数据库。 */
class VoucherStateMachineTests {

    private static final Instant T0 = Instant.parse("2026-07-18T00:00:00Z");

    @Test
    void issuedCanTransitionToEachTerminalState() {
        assertThat(VoucherStateMachine.canTransition("issued", "redeemed")).isTrue();
        assertThat(VoucherStateMachine.canTransition("issued", "expired")).isTrue();
        assertThat(VoucherStateMachine.canTransition("issued", "revoked")).isTrue();
    }

    @Test
    void terminalStatesCannotTransitionAnywhere() {
        for (String terminal : new String[] {"redeemed", "expired", "revoked"}) {
            for (String to : new String[] {"issued", "redeemed", "expired", "revoked"}) {
                assertThat(VoucherStateMachine.canTransition(terminal, to))
                        .as("%s -> %s must be rejected", terminal, to)
                        .isFalse();
            }
        }
    }

    @Test
    void unknownOrNullStatesAreRejected() {
        assertThat(VoucherStateMachine.canTransition(null, "redeemed")).isFalse();
        assertThat(VoucherStateMachine.canTransition("issued", null)).isFalse();
        assertThat(VoucherStateMachine.canTransition("issued", "issued")).isFalse();
        assertThat(VoucherStateMachine.canTransition("bogus", "redeemed")).isFalse();
    }

    @Test
    void computeExpiryWithNeitherRuleIsNull() {
        assertThat(VoucherStateMachine.computeExpiry(T0, null, null)).isNull();
    }

    @Test
    void computeExpiryByDaysOnly() {
        assertThat(VoucherStateMachine.computeExpiry(T0, 30, null))
                .isEqualTo(Instant.parse("2026-08-17T00:00:00Z"));
    }

    @Test
    void computeExpiryByFixedDateOnly() {
        Instant fixed = Instant.parse("2026-12-31T00:00:00Z");
        assertThat(VoucherStateMachine.computeExpiry(T0, null, fixed)).isEqualTo(fixed);
    }

    @Test
    void computeExpiryTakesEarlierOfDaysAndFixedDate() {
        Instant nearFixed = Instant.parse("2026-07-20T00:00:00Z");
        Instant farFixed = Instant.parse("2027-01-01T00:00:00Z");
        // 固定截止更早 → 取固定截止。
        assertThat(VoucherStateMachine.computeExpiry(T0, 30, nearFixed)).isEqualTo(nearFixed);
        // 按天数更早 → 取天数。
        assertThat(VoucherStateMachine.computeExpiry(T0, 30, farFixed))
                .isEqualTo(Instant.parse("2026-08-17T00:00:00Z"));
    }

    @Test
    void effectiveStatusLazilyExpiresIssuedAtOrAfterExpiry() {
        Instant expiresAt = Instant.parse("2026-07-19T00:00:00Z");
        // 未到期：原样 issued。
        assertThat(VoucherStateMachine.effectiveStatus("issued", expiresAt, T0)).isEqualTo("issued");
        // 恰好到期时刻（含边界）与已过期：expired。
        assertThat(VoucherStateMachine.effectiveStatus("issued", expiresAt, expiresAt)).isEqualTo("expired");
        assertThat(VoucherStateMachine.effectiveStatus("issued", expiresAt, expiresAt.plusSeconds(1))).isEqualTo("expired");
        // 不过期券（expires_at 空）永远 issued。
        assertThat(VoucherStateMachine.effectiveStatus("issued", null, T0)).isEqualTo("issued");
        // 终态不受过期折算影响。
        assertThat(VoucherStateMachine.effectiveStatus("redeemed", expiresAt, expiresAt.plusSeconds(1))).isEqualTo("redeemed");
        assertThat(VoucherStateMachine.effectiveStatus("revoked", expiresAt, expiresAt.plusSeconds(1))).isEqualTo("revoked");
    }

    @Test
    void redeemBlockReasonCoversEveryState() {
        Instant future = T0.plusSeconds(3600);
        Instant past = T0.minusSeconds(3600);
        assertThat(VoucherStateMachine.redeemBlockReason("issued", future, T0)).isNull();
        assertThat(VoucherStateMachine.redeemBlockReason("issued", null, T0)).isNull();
        assertThat(VoucherStateMachine.redeemBlockReason("issued", past, T0)).isEqualTo("VOUCHER_EXPIRED");
        assertThat(VoucherStateMachine.redeemBlockReason("redeemed", future, T0)).isEqualTo("VOUCHER_ALREADY_REDEEMED");
        assertThat(VoucherStateMachine.redeemBlockReason("revoked", future, T0)).isEqualTo("VOUCHER_REVOKED");
        assertThat(VoucherStateMachine.redeemBlockReason("expired", future, T0)).isEqualTo("VOUCHER_EXPIRED");
        assertThat(VoucherStateMachine.redeemBlockReason("bogus", future, T0)).isEqualTo("VOUCHER_NOT_REDEEMABLE");
    }
}
