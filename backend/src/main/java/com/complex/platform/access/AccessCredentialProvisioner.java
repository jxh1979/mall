package com.complex.platform.access;

import java.time.Instant;

@FunctionalInterface
public interface AccessCredentialProvisioner {
    void provisionCredential(String userId, String password, String status, Instant now);

    static AccessCredentialProvisioner noop() {
        return (userId, password, status, now) -> { };
    }
}
