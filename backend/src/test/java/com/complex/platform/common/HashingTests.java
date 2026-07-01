package com.complex.platform.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HashingTests {

    @Test
    void idempotencyCodeIsDeterministic16HexAndReproducesLegacySlug() {
        // 4.5：与此前 inventory slug（SHA-256 前16位hex）等价——确定性、16 位、仅 hex。
        String a = Hashing.idempotencyCode("seed-abc");
        String b = Hashing.idempotencyCode("seed-abc");
        assertThat(a).isEqualTo(b).hasSize(16).matches("[0-9a-f]{16}");
        assertThat(Hashing.idempotencyCode("seed-xyz")).isNotEqualTo(a);
    }
}
