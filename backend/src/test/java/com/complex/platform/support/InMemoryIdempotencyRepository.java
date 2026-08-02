package com.complex.platform.support;

import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** 测试共享的内存版幂等仓储：收敛自各测试类中逐字相同的本地副本；records 公开供测试直接断言。 */
public final class InMemoryIdempotencyRepository implements IdempotencyRepository {

    public final Map<String, IdempotencyRecord> records = new LinkedHashMap<>();

    @Override
    public Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey) {
        return Optional.ofNullable(records.get(operationId + ":" + idempotencyKey));
    }

    @Override
    public boolean tryReserve(IdempotencyRecord record) {
        String key = record.operationId() + ":" + record.idempotencyKey();
        if (records.containsKey(key)) {
            return false;
        }
        records.put(key, record);
        return true;
    }

    @Override
    public IdempotencyRecord complete(IdempotencyRecord record) {
        records.put(record.operationId() + ":" + record.idempotencyKey(), record);
        return record;
    }
}
