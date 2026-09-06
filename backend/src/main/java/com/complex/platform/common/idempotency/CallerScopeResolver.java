package com.complex.platform.common.idempotency;

// 文件版本：V0.01

/** Resolves the trusted caller scope used to isolate idempotent operations. */
@FunctionalInterface
public interface CallerScopeResolver {
    String resolve();

    /** Existing scopes keep their keys; changed scopes must also check their old records. */
    default String legacyScope() {
        return resolve();
    }
}
