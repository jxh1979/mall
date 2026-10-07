package com.complex.platform.common.idempotency;

// 文件版本：V0.03

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.common.api.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class IdempotencyService {

    private static final long DEFAULT_TTL_SECONDS = 86_400;
    private static final String RESPONSE_CODE_OK = "OK";
    private static final String RESPONSE_CODE_IN_PROGRESS = "IN_PROGRESS";
    private static final String RESPONSE_CODE_FAILED_RETRYABLE = "FAILED_RETRYABLE";
    // M724：占位那一条 INSERT 等别的事务超过锁等待上限（JdbcIdempotencyRepository#tryReserve）时数据库报的 SQLSTATE（lock_not_available）
    private static final String SQL_STATE_LOCK_NOT_AVAILABLE = "55P03";
    // request_hash 前半段（提交范围散列）的长度：SHA-256 十六进制
    private static final int SCOPE_HASH_LENGTH = 64;

    private final IdempotencyRepository repository;
    private final ObjectMapper objectMapper;
    private CallerScopeResolver callerScopeResolver = () -> "";
    // B5 事务边界：可选事务模板。注入后把 action(多写) + complete(标记OK) 包成一个原子单元。
    // M724：调用方没有外层事务时，再把「占位＋动作＋完成」整段放进同一个事务——占位不再先提交，失败、被拒、回滚、进程中断时占位行
    // 随事务消失（失败不占编号），同一键上后到的提交在占位处等先到的事务结束再判定（先处理成功的为准）。调用方已有事务时不再套一层，
    // 占位并入调用方的事务。
    // 经 setter 可选注入(同 setCallerScopeResolver 范式)，单测 new 构造不传则保持原直跑行为，零破坏。
    private TransactionTemplate transactionTemplate;

    public IdempotencyService(IdempotencyRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper.copy()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Autowired(required = false)
    public void setCallerScopeResolver(CallerScopeResolver callerScopeResolver) {
        this.callerScopeResolver = callerScopeResolver != null ? callerScopeResolver : () -> "";
    }

    @Autowired(required = false)
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = transactionManager == null ? null : new TransactionTemplate(transactionManager);
    }

    public <T> T execute(
            String operationId,
            String idempotencyKey,
            Object request,
            Supplier<T> action,
            Class<T> responseType
    ) {
        return execute(operationId, idempotencyKey, request, action, responseType, Function.identity());
    }

    public <T> T execute(
            String operationId,
            String idempotencyKey,
            Object request,
            Supplier<T> action,
            Class<T> responseType,
            Function<T, T> storedResponseTransformer
    ) {
        requireIdempotencyKey(idempotencyKey);
        String scope = callerScopeResolver.resolve();
        String legacyScope = callerScopeResolver.legacyScope();
        String legacyId = scopedOperationId(operationId, legacyScope);
        boolean changedScope = !java.util.Objects.equals(scope, legacyScope);
        // 旧共享记录没有可靠主体归属：保留结果，阻止串用或再次执行。
        if (changedScope && repository.findByOperationIdAndIdempotencyKey(legacyId, idempotencyKey).isPresent()) {
            throw ApiException.conflict("IDEMPOTENCY_CONFLICT", "该请求编号存在旧处理记录，请先核实原操作结果");
        }
        // M724：行键不含提交范围，同一「操作号＋请求编号」全平台只有一行；提交范围散列并在请求散列的前 64 位（占位与完成用同一个串）。
        String storedId = storedOperationId(operationId);
        String requestHash = sha256Hex(scope == null ? "" : scope) + hashJson(request);
        IdempotentReplayContext replayContext = IdempotentReplayContext.current();
        if (replayContext != null && replayContext.replayOnly()) {
            // XM-M002-J：权限核验只凭「用掉单次授权的那次成功请求」的重放通过——只能返回第一次的结果，不占位、不执行；
            // 记录不在或未成功完成（例如第一次失败）即拒绝，正文不同照常 409。
            IdempotencyRecord completed = repository.findByOperationIdAndIdempotencyKey(storedId, idempotencyKey)
                    .filter(record -> RESPONSE_CODE_OK.equals(record.responseCode()))
                    .orElseThrow(IdempotentReplayContext::replayWithoutResult);
            return replay(completed, requestHash, responseType);
        }
        Supplier<T> flow = () -> reserveRunOrReplay(
                storedId, idempotencyKey, requestHash, action, responseType, storedResponseTransformer);
        // M724：调用方没有外层事务时，占位、动作、完成放进同一个事务。调用方已有事务时不再套模板——否则下面抛出的 409 会把
        // 调用方的事务标成只回滚；没有事务模板的实例（单测 new 构造）保持原直跑。
        if (transactionTemplate != null && !TransactionSynchronizationManager.isActualTransactionActive()) {
            return transactionTemplate.execute(status -> flow.get());
        }
        return flow.get();
    }

    /**
     * M724：行键——{@code "once:"}＋操作号（UTF-8 字节）的 SHA-256 十六进制，定长 69，不含提交范围；与既有唯一约束
     * {@code (operation_id, idempotency_key)} 一起保证同一「操作号＋请求编号」全平台只有一行。取散列是因为原始操作号可能超过列宽 128。
     * 生产代码与测试都用它算行键。
     */
    public static String storedOperationId(String operationId) {
        return "once:" + sha256Hex(operationId);
    }

    private <T> T reserveRunOrReplay(
            String storedId,
            String idempotencyKey,
            String requestHash,
            Supplier<T> action,
            Class<T> responseType,
            Function<T, T> storedResponseTransformer
    ) {
        Instant now = Instant.now();
        IdempotencyRecord reservation = new IdempotencyRecord(
                UUID.randomUUID(),
                storedId,
                idempotencyKey,
                requestHash,
                RESPONSE_CODE_IN_PROGRESS,
                null,
                202,
                now,
                now.plusSeconds(DEFAULT_TTL_SECONDS)
        );

        if (tryReserve(reservation)) {
            return runAndComplete(reservation, action, responseType, storedResponseTransformer);
        }

        Optional<IdempotencyRecord> existing = repository.findByOperationIdAndIdempotencyKey(storedId, idempotencyKey);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            // M724：失败的提交不占编号——失败可重试的行任何来者（不论提交范围、请求体）都可接管，行上的散列换成来者的。
            if (RESPONSE_CODE_FAILED_RETRYABLE.equals(record.responseCode())) {
                IdempotencyRecord retryReservation = inProgressReservation(record, requestHash);
                if (repository.tryUpdateWhenResponseCode(retryReservation, RESPONSE_CODE_FAILED_RETRYABLE)) {
                    return runAndComplete(retryReservation, action, responseType, storedResponseTransformer);
                }
                return repository.findByOperationIdAndIdempotencyKey(storedId, idempotencyKey)
                        .map(latest -> replay(latest, requestHash, responseType))
                        .orElseThrow(() -> ApiException.conflict(
                                "IDEMPOTENCY_IN_PROGRESS",
                                "请求正在处理中，请稍后刷新查看结果"));
            }
            return replay(record, requestHash, responseType);
        }

        throw ApiException.conflict(
                "IDEMPOTENCY_IN_PROGRESS",
                "请求正在处理中，请稍后刷新查看结果");
    }

    // M724：同一键上有别的事务未提交的占位时，数据库让占位语句等对方事务结束；等待超过上限（SQLSTATE 55P03）按「处理中」拒绝、不执行。
    // 按根因 SQLException 的 SQLSTATE 判断，不依赖 Spring 的异常子类；只转这一种，其余（含死锁牺牲者）原样抛出。
    private boolean tryReserve(IdempotencyRecord reservation) {
        try {
            return repository.tryReserve(reservation);
        } catch (RuntimeException exception) {
            Throwable root = exception;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            if (root instanceof java.sql.SQLException sqlException
                    && SQL_STATE_LOCK_NOT_AVAILABLE.equals(sqlException.getSQLState())) {
                throw ApiException.conflict("IDEMPOTENCY_IN_PROGRESS", "请求正在处理中，请稍后刷新查看结果");
            }
            throw exception;
        }
    }

    private String scopedOperationId(String operationId, String scope) {
        if (scope == null || scope.isEmpty()) {
            return operationId;
        }
        return scope + ":" + operationId;
    }

    private <T> T replay(IdempotencyRecord existing, String requestHash, Class<T> responseType) {
        // M724（操作员 2026-10-04 裁决「同一提交编号只能提交一次不允许多次提交」）：先比提交范围（散列前 64 位）、后比请求体。
        // 其他范围遇到成功行——该编号已被用掉：拒绝，不执行、不读也不返回第一次的结果，请求体相同与否回答一样；
        // 遇到未成功的行与同范围同一句「处理中」（第一次还没成功，不说「已被使用」）。
        if (!existing.requestHash().regionMatches(0, requestHash, 0, SCOPE_HASH_LENGTH)) {
            if (RESPONSE_CODE_OK.equals(existing.responseCode()) && existing.responseBody() != null) {
                throw ApiException.conflict("IDEMPOTENCY_KEY_USED", "该提交编号已被使用，本次操作未执行");
            }
            throw ApiException.conflict("IDEMPOTENCY_IN_PROGRESS", "请求正在处理中，请稍后刷新查看结果");
        }
        if (!existing.requestHash().equals(requestHash)) {
            throw ApiException.conflict("IDEMPOTENCY_CONFLICT", "该幂等键已被不同的请求使用，请刷新后重试");
        }
        if (RESPONSE_CODE_IN_PROGRESS.equals(existing.responseCode()) || existing.responseBody() == null) {
            throw ApiException.conflict("IDEMPOTENCY_IN_PROGRESS", "请求正在处理中，请稍后刷新查看结果");
        }
        T first;
        try {
            first = objectMapper.readValue(existing.responseBody(), responseType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("stored idempotency response is unreadable", exception);
        }
        // M723-F04（操作员 2026-09-25 确认句「同一次操作重复提交时，直接返回第一次的结果并提示已完成」）：
        // 确实读出第一次的结果、即将返回时才记为已重放，响应据此提示已完成。
        IdempotentReplayContext.markReplayed(existing.idempotencyKey());
        return first;
    }

    // M724：接管失败可重试的行——沿用原行 id、创建与过期时间，请求散列（提交范围＋请求体）换成来者的。
    private IdempotencyRecord inProgressReservation(IdempotencyRecord record, String requestHash) {
        return new IdempotencyRecord(
                record.id(),
                record.operationId(),
                record.idempotencyKey(),
                requestHash,
                RESPONSE_CODE_IN_PROGRESS,
                null,
                202,
                record.createdAt(),
                record.expiresAt()
        );
    }

    private <T> T runAndComplete(
            IdempotencyRecord reservation,
            Supplier<T> action,
            Class<T> responseType,
            Function<T, T> storedResponseTransformer
    ) {
        try {
            // action(多写) + complete(标记OK) 须原子：注入了事务模板则在一个事务内执行（REQUIRED，并入 execute 开的事务或
            // 调用方的事务），否则保持原直跑（单测路径）。M724：占位与本段同在一个事务里，动作失败时经本模板把该事务标成只回滚，
            // 占位行与下面的失败留痕（markRetryableFailure）随之回滚，不留行；没有事务的实例才留下失败可重试的行。
            T response = transactionTemplate == null
                    ? actionAndComplete(reservation, action, storedResponseTransformer)
                    : transactionTemplate.execute(status ->
                            actionAndComplete(reservation, action, storedResponseTransformer));
            return responseType.cast(response);
        } catch (RuntimeException | Error exception) {
            markRetryableFailure(reservation, exception);
            throw exception;
        }
    }

    private <T> T actionAndComplete(
            IdempotencyRecord reservation,
            Supplier<T> action,
            Function<T, T> storedResponseTransformer
    ) {
        T response = action.get();
        T storedResponse = storedResponseTransformer == null
                ? response
                : storedResponseTransformer.apply(response);
        repository.complete(new IdempotencyRecord(
                reservation.id(),
                reservation.operationId(),
                reservation.idempotencyKey(),
                reservation.requestHash(),
                RESPONSE_CODE_OK,
                writeJson(storedResponse),
                200,
                reservation.createdAt(),
                reservation.expiresAt()
        ));
        // XM-M002-J：本请求消耗过单次授权时，把消耗人与请求摘要记到这条成功记录上（与消耗同一事务），
        // 权限核验据此确认重放的那次请求确已成功完成、有结果可返回。
        IdempotentReplayContext replayContext = IdempotentReplayContext.current();
        if (replayContext != null && replayContext.identified() && replayContext.consumedBy() != null) {
            repository.bindSingleUseRequest(reservation.id(), replayContext.consumedBy(), replayContext.requestDigest());
        }
        // M723-F04：本请求真的执行了动作，不算重复提交（见 IdempotentReplayContext#replayed）
        IdempotentReplayContext.markExecuted();
        return response;
    }

    private void markRetryableFailure(IdempotencyRecord reservation, Throwable failure) {
        try {
            repository.complete(new IdempotencyRecord(
                    reservation.id(),
                    reservation.operationId(),
                    reservation.idempotencyKey(),
                    reservation.requestHash(),
                    RESPONSE_CODE_FAILED_RETRYABLE,
                    null,
                    500,
                    reservation.createdAt(),
                    reservation.expiresAt()
            ));
        } catch (RuntimeException | Error completionException) {
            failure.addSuppressed(completionException);
        }
    }

    private String hashJson(Object value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(writeJson(value).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("request hashing unavailable", exception);
        }
    }

    private static String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("request hashing unavailable", exception);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("idempotency payload is not serializable", exception);
        }
    }
}
