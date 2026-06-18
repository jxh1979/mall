package com.complex.platform.common.idempotency;

/**
 * Resolves the current caller's scope token for idempotency key isolation.
 * Returns "m:{merchantId}" for merchant callers, "p" for platform admins,
 * or "" (empty) when no scope is available (e.g., in tests).
 * An empty scope leaves the operationId unmodified (backward-compatible).
 */
@FunctionalInterface
public interface CallerScopeResolver {
    String resolve();
}
