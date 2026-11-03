package com.complex.platform.common.idempotency;

// 文件版本：V0.03

import com.complex.platform.common.api.ApiException;

/**
 * XM-M002-J（操作员 2026-09-25 答复：「单次权限使用过后。不应该是返回结果吗然后权限才被用掉嘛，没有返回结果，不会被用掉，
 * 返回结果会提示这个完成啊。」）：本次控制器调用的幂等键与请求摘要（方法、路径与查询串的 SHA-256），
 * 由请求事务切面在控制器方法外层放入、返回后还原。
 *
 * <p>权限核验据此把单次消耗记到这次请求名下（{@link #markConsumed}），并认出「用掉该单次来源的那次成功请求」的重放
 * （{@link #markReplayOnly}）；幂等层据此把消耗过单次来源的成功记录记到这次请求名下，并让重放只能返回第一次的结果、不得执行。
 * 不在控制器调用里（定时作业等）时没有本上下文，一律按旧行为。
 *
 * <p>M723：另记两件事。① {@link #readOnly}——本次请求是查看（GET / HEAD）或以提交方式实现的预览、查询：权限核验照常，但不消耗
 * 单次授权（操作员 2026-10-02：「只有真正执行的写操作（分配、审批等）才扣单次授权，打开页面、看列表不扣。」2026-10-03：
 * 「预览、查询不扣一次性授权，只有真正核销、真正执行的那一次才扣。」）；
 * 例外是接口声明「仍然扣」的权限码（{@link #consumesSingleUse}：导出、敏感明文查看，操作员 2026-10-03：导一次扣一次、看一次扣一次）。
 * ② 本请求已消耗的单次来源（{@link #markConsumed} / {@link #consumedInThisRequest}）：同一请求里分层再核验同一来源时
 * 仍视为有效、不再消耗（一次操作只算一次使用；账本 XM-M001-W、XM-M002-AU）。
 *
 * <p>M723-F04（操作员 2026-09-25 确认句「同一次操作重复提交时，直接返回第一次的结果并提示已完成，不再重复执行、也不再扣减」）：
 * 幂等层返回的是本请求自己请求编号第一次的结果、本请求没有经幂等层执行过动作时记为已重放（{@link #markReplayed} / {@link #replayed}）：
 * {@code ApiResponse.ok} 据此提示已完成；请求事务切面据此把本请求新用掉的单次来源随请求事务回滚（不算用掉）。
 *
 * <p>M723-F08（第 656 行「操作被拒绝或失败的不算用掉」）：在线核销以正常返回报告失败（过刷新窗口、签名失配）时由服务层记为
 * {@link #markBusinessFailed 业务失败}；请求事务切面据此把本请求新用掉的单次来源撤回（失败留痕与幂等记录照常提交）。
 * 撤回要知道来源号与权限码，{@link #markConsumed(String, String, String, String)} 另记一份（{@link #consumedSourceCodes}）。
 *
 * <p>M723-F09（同行）：请求事务切面没有给本请求开请求事务、进入时也没有外层事务时记下（{@link #markWithoutRequestTransaction}）：
 * 这样的请求里权限核验不消耗单次来源——即使核验发生在业务层自己的事务里，那个事务的提交也不受切面的重放回滚与失败撤回约束。
 *
 * <p>M724（XM-ONESHOT-RETURN B01）：准入闸逐笔放行的放行单同样是只能用一次的来源——消费时记到这次请求名下（{@link #markConsumed}），
 * 重放时由主管复核服务认出并记为「只能重放」（{@link #markReplayOnly}），准入闸据此放行、不再消费、不建复核单。
 *
 * <p>M724（XM-ONESHOT-RETURN B02）：本请求请求编号的派生键经 {@link #registerDerivedKey} 登记后，其回放同样算本请求的重复提交；
 * 按业务对象认出重复提交的模块自带重放用 {@link #markCurrentReplayed}。
 */
public final class IdempotentReplayContext {

    private static final ThreadLocal<IdempotentReplayContext> CURRENT = new ThreadLocal<>();

    private final String idempotencyKey;
    private final String requestDigest;
    private final boolean readOnly;
    private final java.util.Set<String> consumedOnRead;
    private final java.util.Set<String> consumedSources = new java.util.HashSet<>();
    // M723-F08：本请求消耗的来源号 → 权限码（撤回时用）
    private final java.util.Map<String, String> consumedSourceCodes = new java.util.LinkedHashMap<>();
    private String consumedBy;
    private boolean replayOnly;
    private boolean replayed;
    private boolean executed;
    private boolean businessFailed;
    private boolean withoutRequestTransaction;
    private final java.util.Set<String> derivedKeys = new java.util.HashSet<>();

    private IdempotentReplayContext(String idempotencyKey, String requestDigest, boolean readOnly,
                                    java.util.Set<String> consumedOnRead) {
        this.idempotencyKey = idempotencyKey;
        this.requestDigest = requestDigest;
        this.readOnly = readOnly;
        this.consumedOnRead = java.util.Set.copyOf(consumedOnRead);
    }

    /** 放入本次控制器调用的上下文，返回外层原有的（控制器方法互相调用时由 {@link #restore} 还原）。 */
    public static IdempotentReplayContext enter(String idempotencyKey, String requestDigest) {
        return enter(idempotencyKey, requestDigest, false, java.util.Set.of());
    }

    /**
     * 同上；{@code readOnly} 为真表示本次请求是查看（GET / HEAD）或预览、查询，不消耗单次授权——{@code consumedOnRead} 里的权限码除外
     * （该接口声明仍然扣的码）。
     */
    public static IdempotentReplayContext enter(String idempotencyKey, String requestDigest, boolean readOnly,
                                                java.util.Set<String> consumedOnRead) {
        IdempotentReplayContext previous = CURRENT.get();
        CURRENT.set(new IdempotentReplayContext(idempotencyKey, requestDigest, readOnly, consumedOnRead));
        return previous;
    }

    public static void restore(IdempotentReplayContext previous) {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }

    public static IdempotentReplayContext current() {
        return CURRENT.get();
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public String requestDigest() {
        return requestDigest;
    }

    /** 幂等键与请求摘要都取到了，才把消耗记到这次请求名下、才认重放；取不到（或取到多个不同的幂等键）按旧行为。 */
    public boolean identified() {
        return idempotencyKey != null && requestDigest != null;
    }

    /** 本请求消耗了单次来源（单次授权或准入放行单）的操作人；未消耗为 null。 */
    public String consumedBy() {
        return consumedBy;
    }

    /** M723：本次请求是查看（GET / HEAD）或预览、查询，权限核验通过后不消耗单次授权。 */
    public boolean readOnly() {
        return readOnly;
    }

    /** M723：本请求里核验这个权限码要不要消耗单次授权——写请求都要；查看、预览、查询请求只有接口声明「仍然扣」的码要。 */
    public boolean consumesSingleUse(String permissionCode) {
        return !readOnly || consumedOnRead.contains(permissionCode);
    }

    /** M723：记下本请求里该操作人已消耗的单次来源（来源类型:来源号）。 */
    public void markConsumed(String actorId, String source) {
        this.consumedBy = actorId;
        this.consumedSources.add(source);
    }

    /** M723-F08：同上，并记下来源号与权限码，业务失败时据此撤回这次消耗。 */
    public void markConsumed(String actorId, String source, String sourceId, String permissionCode) {
        markConsumed(actorId, source);
        this.consumedSourceCodes.put(sourceId, permissionCode);
    }

    /** M723-F08：本请求消耗的单次来源（来源号 → 权限码），按消耗顺序。 */
    public java.util.Map<String, String> consumedSourceCodes() {
        return java.util.Collections.unmodifiableMap(consumedSourceCodes);
    }

    /**
     * M723-F08：业务以正常返回报告失败时由服务层调用（现只有在线核销过刷新窗口、签名失配一处调用）；请求事务切面据此把本请求新用掉的单次来源撤回。
     * 不在控制器调用里（没有请求上下文）时无操作。
     */
    public static void markBusinessFailed() {
        IdempotentReplayContext request = CURRENT.get();
        if (request != null) {
            request.businessFailed = true;
        }
    }

    /** M723-F08：本请求的业务以正常返回报告了失败。 */
    public boolean businessFailed() {
        return businessFailed;
    }

    /**
     * M723-F09：请求事务切面放行本请求时没有开请求事务、当时也没有外层事务（进入时不持有未消耗单次来源，或本请求不消耗）。
     * 不在控制器调用里（没有请求上下文）时无操作。
     */
    public static void markWithoutRequestTransaction() {
        IdempotentReplayContext request = CURRENT.get();
        if (request != null) {
            request.withoutRequestTransaction = true;
        }
    }

    /** M723-F09：本请求没有请求事务——判定期间才被授出的单次来源不在本请求里消耗。 */
    public boolean withoutRequestTransaction() {
        return withoutRequestTransaction;
    }

    /** M723：该单次来源是否正是本请求里这名操作人刚消耗的——是则本请求内再核验时仍视为有效、不再消耗。 */
    public boolean consumedInThisRequest(String actorId, String source) {
        return actorId != null && actorId.equals(consumedBy) && consumedSources.contains(source);
    }

    /** 本请求的权限核验是凭「用掉单次来源的那次成功请求」的重放通过的：只能返回第一次的结果，不得再执行、不得再消耗。 */
    public boolean replayOnly() {
        return replayOnly;
    }

    public void markReplayOnly() {
        this.replayOnly = true;
    }

    /**
     * M723-F04：本请求是重复提交——幂等层返回的是自己请求编号第一次的结果，且本请求没有经幂等层执行过动作。
     * 响应须提示已完成；本请求新用掉的单次来源不算用掉。
     */
    public boolean replayed() {
        return replayed && !executed;
    }

    /**
     * M723-F04 / F06：本请求是重复提交、且在权限核验处新用掉了单次来源——请求事务切面会把这次消耗回滚（不算用掉）。
     * 既然不扣，响应里凭这次消耗才给的敏感明文（联系电话）也不得给出，一律按掩码返回（操作员 2026-10-03：敏感明文「算用掉，看一次扣一次」）。
     * 不在控制器调用里时为假。
     * ponytail: 只看「本请求有没有新消耗」，不分是哪个权限码的来源——长期持有明文码、但凭单次业务码重复提交的人这次也看掩码；
     * 要分清需按权限码记录本请求的消耗。
     */
    public static boolean replayDiscardsConsumption() {
        IdempotentReplayContext request = CURRENT.get();
        return request != null && request.replayed() && request.consumedBy != null;
    }

    /**
     * M723-F04：幂等层返回第一次的结果时调用。回放的正是本请求自己的请求编号（M724 B02：或经 {@link #registerDerivedKey} 登记的它的派生键）
     * 才记为已重放；业务内部按别的键去重的回放、取不到本请求的请求编号、不在控制器调用里（没有请求上下文）时都不记（响应照旧，不提示已完成）。
     */
    public static void markReplayed(String replayedKey) {
        IdempotentReplayContext request = CURRENT.get();
        if (request != null && replayedKey != null
                && (replayedKey.equals(request.idempotencyKey) || request.derivedKeys.contains(replayedKey))) {
            request.replayed = true;
        }
    }

    /**
     * M724（XM-ONESHOT-RETURN B02）：业务把本请求的请求编号派生成内部键再交给幂等层时（如储值充值的 svtop_ 键）登记；
     * 派生前的键正是本请求自己的请求编号才登记，其回放才算本请求的重复提交。
     */
    public static void registerDerivedKey(String requestKey, String derivedKey) {
        IdempotentReplayContext request = CURRENT.get();
        if (request != null && requestKey != null && derivedKey != null && requestKey.equals(request.idempotencyKey)) {
            request.derivedKeys.add(derivedKey);
        }
    }

    /**
     * M723-F04：幂等层在本请求里真的执行了动作并保存了结果时调用。此后本请求不算重复提交
     * （同一请求里一步返回第一次的结果、另一步新执行时，新执行的写入不得被当成重放回滚）。
     */
    public static void markExecuted() {
        IdempotentReplayContext request = CURRENT.get();
        if (request != null) {
            request.executed = true;
        }
    }

    /**
     * M724（XM-ONESHOT-RETURN B02）：模块按业务对象（不按请求编号）认出本次控制器调用是同一次操作的重复提交、
     * 返回第一次的结果时调用（如储值充值入账按支付意向认出「已入账」）。只对本次控制器调用生效，调用方须排在各项拒绝之后。
     */
    public static void markCurrentReplayed() {
        IdempotentReplayContext request = CURRENT.get();
        if (request != null) {
            request.replayed = true;
        }
    }

    /** 凭重放过了权限核验、却找不到第一次的成功结果时的拒绝，与权限核验拒绝同码。 */
    static ApiException replayWithoutResult() {
        return ApiException.forbidden("PERMISSION_DENIED", "没有操作权限：单次授权或放行单已用掉，且没有第一次的成功结果可返回");
    }
}
