package com.complex.platform.common.idempotency;

import java.util.Optional;

public interface IdempotencyRepository {

    Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey);

    boolean tryReserve(IdempotencyRecord record);

    IdempotencyRecord complete(IdempotencyRecord record);
}
