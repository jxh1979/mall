package com.complex.platform.common.idempotency;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRepository {

    Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey);

    boolean tryReserve(IdempotencyRecord record);

    default boolean tryUpdateWhenResponseCode(IdempotencyRecord record, String expectedResponseCode) {
        return false;
    }

    IdempotencyRecord complete(IdempotencyRecord record);

    /** XM-M002-J：把消耗了单次授权的那次请求（消耗人、请求摘要）记到它成功完成的幂等记录上。内存仓不记，重放照旧拒绝。 */
    default void bindSingleUseRequest(UUID recordId, String actorId, String requestDigest) {
    }

    /** XM-M002-J：该操作人以该幂等键发起的这次请求是否已成功完成、且消耗过单次授权。 */
    default boolean hasCompletedSingleUseRequest(String actorId, String idempotencyKey, String requestDigest) {
        return false;
    }
}
