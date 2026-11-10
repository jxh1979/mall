package com.complex.platform.access;

// 文件版本：V0.28

import com.complex.platform.auth.AuthService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.CallerScopeResolver;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotentReplayContext;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.complex.platform.auth.SessionIssuer;

@Service
@ConditionalOnBean(AccessRepository.class)
public class AuthorizationService implements CallerScopeResolver {

    // #14 钱包双路径：客人自助主体可自行执行的权限白名单（仅作用于本人账户，由控制器按客人 token 绑定）。
    // 仅限客人端 App/小程序自助场景：储值充值/确认、储值消费。退款/冻结等仍为员工专属。
    private static final java.util.Set<String> CUSTOMER_SELF_SERVICE_PERMISSIONS = java.util.Set.of(
            "customer.stored_value.topup",
            "customer.stored_value.consume");

    // M736 第二步：待批条目（role_viewable_project_holds.viewable_id）里代表「可以查看全部项目」这一勾的取值。
    public static final String ALL_PROJECTS = "*";
    // M736 第二步（技术推导）：待批条目里的保留标记，不是可查看项目——角色原来一条勾选都没有、第一次勾上项目时已持有它的人各记一条：
    // 重新审批通过之前，这个角色对他仍按没勾选的角色算（只到本人项目，见 projectReach）。随重新审批落地、失去角色整人整角色一起删。
    // 取值不会与勾选条目相撞：勾选条目只有 "*" 与过了项目集合校验的编号（编号由系统生成）。
    public static final String HELD_SINCE_UNTICKED = "-";

    private final AccessRepository accessRepository;
    // XM-M002-J：单次消耗走的事务模板（REQUIRED：有业务/请求事务就并入，没有就自建）。经 setter 可选注入（同 IdempotencyService），
    // 单测 new 构造不传则直接消耗。
    private TransactionTemplate transactionTemplate;
    // XM-M002-J：重放核验要确认「用掉单次授权的那次请求」已成功完成（幂等记录）。可选注入，单测 new 构造不传则不认重放（照旧拒绝）。
    private IdempotencyRepository idempotencyRepository;

    public AuthorizationService(AccessRepository accessRepository) {
        this.accessRepository = accessRepository;
    }

    @Autowired(required = false)
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = transactionManager == null ? null : new TransactionTemplate(transactionManager);
    }

    @Autowired(required = false)
    public void setIdempotencyRepository(IdempotencyRepository idempotencyRepository) {
        this.idempotencyRepository = idempotencyRepository;
    }

    // 第十八批裁决 46：派驻期按各派驻酒吧营业日判（191-R1 商户范围），可选注入；单测 new 构造不传则按无派驻处理。
    private com.complex.platform.access.staff.JdbcStaffRepository staffRepository;

    @Autowired(required = false)
    public void setStaffRepository(com.complex.platform.access.staff.JdbcStaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    public void requirePermission(String permissionCode) {
        requirePermissions(permissionCode);
    }

    /**
     * 同一动作先核验所有权限，再按来源消费一次。
     * XM-M002-J：核验不过直接拒绝、不经事务边界，调用方捕获拒绝后改走别的权限时不会把外层事务标成只能回滚；
     * 单次消耗并入外层事务（请求事务切面 {@link SingleUseGrantRequestTransactionAspect} 或业务事务），随业务一起提交或回滚，
     * 同一次调用内多个来源要么全消耗、要么全不消耗；抢不到（已被并发请求用掉）即拒绝并让整笔回滚。
     * XM-M002-J（操作员 2026-09-25 答复）：同一操作人以同一幂等键重放「用掉该单次来源的那次成功请求」时按重放放行、不再消耗，
     * 由幂等层返回第一次的结果（见下方 SingleUseReplay）；同一请求不能既按重放放行、又消耗新的单次来源。
     * M723（操作员 2026-10-02「只有真正执行的写操作（分配、审批等）才扣单次授权，打开页面、看列表不扣」；2026-10-03「预览、查询不扣
     * 一次性授权，只有真正核销、真正执行的那一次才扣」）：查看请求（GET / HEAD）与标了 {@link DoesNotConsumeSingleUseGrant} 的预览、
     * 查询接口核验照常、不消耗；查看请求里接口声明「仍然扣」的码（{@link ConsumesSingleUseGrant}：导出、敏感明文查看，
     * 操作员 2026-10-03「导一次扣一次」「看一次扣一次」）照旧消耗。
     * M723（技术推导，一次操作只算一次使用）：本请求已消耗的单次来源在本请求内再核验时仍视为有效、不再消耗。
     * 不在控制器请求里（没有请求上下文）时一律照旧消耗。
     * M723-F09（第 656 行「操作被拒绝或失败的不算用掉」）：控制器请求里本次要消耗单次来源（写请求，或带 {@link ConsumesSingleUseGrant}
     * 的查看请求）而本请求没有请求事务（请求进入时不持有、判定期间才被授出；切面记了「无请求事务」，或当前没有实际事务）时
     * 不消耗、按不具备处理。M723-F10、F11（同行「同一次操作重复提交时，直接返回第一次的结果并提示已完成」）：抢不到来源时若本次调用还没有
     * 新消耗任何来源、本次要消耗的每一份来源都正是本人以本请求编号、本请求摘要刚用掉的、且那次请求已经幂等层成功完成（与 SingleUseReplay
     * 第③项同口径），转为只能重放（不分一份还是多份来源）；不经幂等层的接口没有
     * 可返回的第一次结果，照旧拒绝。M723-F08：消耗记到请求名下时连来源号与权限码一起记，供 {@link #releaseRequestConsumption} 撤回。
     */
    public void requirePermissions(String... permissionCodes) {
        AccessActor actor = currentActor();
        Optional<AccessRepository.UserIdentity> identity = identityAtStart(actor); // M722-F05：读能力之前
        Instant checkedAt = Instant.now();
        IdempotentReplayContext request = IdempotentReplayContext.current();
        List<AccessRepository.PermissionEffect> all = accessRepository.findPermissionEffects(actor.id(), checkedAt);
        List<AccessRepository.PermissionEffect> effects = all.stream()
                .filter(effect -> usable(effect, checkedAt, actor, request)).toList();
        SingleUseReplay replay = new SingleUseReplay(actor, all, checkedAt);
        java.util.Map<String, AccessRepository.PermissionEffect> consume = new java.util.LinkedHashMap<>();
        java.util.function.Supplier<java.util.Set<String>> daily = dailyPermissions();
        java.util.function.Supplier<java.util.function.Predicate<AccessRepository.PermissionEffect>> positionDerived =
                positionDerivedSources(actor.id());
        for (String code : new java.util.LinkedHashSet<>(java.util.Arrays.asList(permissionCodes))) {
            AccessRepository.PermissionEffect grant = permissionGrant(actor, code, effects, daily, positionDerived, replay);
            // M723：查看、预览、查询不算一次使用——这类请求里只有接口声明「仍然扣」的码（ConsumesSingleUseGrant）才消耗
            if (grant != null && grant.singleUse() && (request == null || request.consumesSingleUse(code))) {
                consume.putIfAbsent(sourceKey(grant), grant);
            }
        }
        // 岗位派生事实在上面的循环里才按操作人 id 读取：核对放在最后一次读取之后、重放判断与单次来源消费之前
        // （查看、预览、查询请求不消耗，同样先核身份）。
        requireIdentityUnchanged(actor, identity);
        if (replay.matched()) {
            if (!consume.isEmpty()) {
                deny(consume.values().iterator().next().permissionCode());
            }
            replay.request().markReplayOnly();
        }
        if (request != null) {
            // M723：本请求已消耗的来源不再消耗
            consume.keySet().removeIf(source -> request.consumedInThisRequest(actor.id(), source));
        }
        if (consume.isEmpty()) {
            return;
        }
        if (request != null && request.replayOnly()) {
            // 已按重放放行的请求只能返回第一次的结果，不再消耗新的单次来源
            deny(consume.values().iterator().next().permissionCode());
        }
        // M723-F09：控制器请求里本次要消耗单次来源（consume 非空：写请求，或带 ConsumesSingleUseGrant 的查看请求如导出、商户列表）
        // 而本请求没有请求事务——请求进入时不持有单次来源、判定期间才被授出：切面放行时记了「无请求事务」（这时即使核验发生在业务层
        // 自己的事务里，那个事务的提交也不受切面的重放回滚与失败撤回约束），或当前没有实际事务——消耗无法随请求结果处理，不消耗、
        // 按不具备处理；用户重试即可（下一次进入时已持有，切面会开请求事务）。不消耗的查看请求（consume 为空）与不在控制器请求里的
        // 调用不受影响。
        if (request != null && (request.withoutRequestTransaction()
                || !TransactionSynchronizationManager.isActualTransactionActive())) {
            deny(consume.values().iterator().next().permissionCode());
        }
        boolean recordRequest = request != null && request.identified();
        boolean[] replayedAtConsumption = new boolean[1];
        int[] newlyConsumed = new int[1];
        Runnable consumeAll = () -> {
            for (AccessRepository.PermissionEffect grant : consume.values()) {
                // 取不到本次请求的幂等键时沿用四参写法（不记请求信息，其重放照旧拒绝）；内存仓与桩只实现了它
                boolean consumed = recordRequest
                        ? accessRepository.markSingleUsePermissionConsumed(grant.sourceId(), grant.permissionCode(),
                                actor.id(), checkedAt, request.idempotencyKey(), request.requestDigest())
                        : accessRepository.markSingleUsePermissionConsumed(
                                grant.sourceId(), grant.permissionCode(), actor.id(), checkedAt);
                if (consumed) {
                    newlyConsumed[0]++;
                    continue;
                }
                // M723-F10：抢不到时重读消耗记录——正是本人以本请求编号、本请求摘要用掉的（同号并发提交的另一笔刚提交），
                // 且那次请求的幂等记录已成功完成（SingleUseReplay 第③项同口径：消耗与幂等记录同一事务提交，读到消耗行时记录必已 OK；
                // 不经幂等层的接口没有这条记录、也没有可返回的第一次结果，照旧拒绝——replayOnly 只有幂等层尊重），
                // 转为只能重放，由幂等层返回第一次的结果（走到这里记录必已 OK；首笔尚未完成时的 409 出在幂等占位处的既有逻辑，不在本分支）；
                // M723-F11（同行，不分来源个数）：一次要消耗多份单次来源时同样转——条件是本次调用还没有新消耗任何来源（落败在第一份上），
                // 且本次要消耗的每一份来源都正是本请求编号、本请求摘要用掉的（同号的另一笔把它们一起用掉并提交了）；
                // 已经新消耗过一份、或有一份是被别的请求用掉的，照旧拒绝并让整笔回滚（同一次调用不能既按重放放行、又消耗别的单次来源）。
                if (recordRequest && newlyConsumed[0] == 0 && idempotencyRepository != null
                        && consume.values().stream().allMatch(each -> accessRepository.singleUseSourceConsumedByRequest(
                                each.sourceId(), each.permissionCode(), actor.id(), request.idempotencyKey(), request.requestDigest()))
                        && idempotencyRepository.hasCompletedSingleUseRequest(
                                actor.id(), request.idempotencyKey(), request.requestDigest())) {
                    replayedAtConsumption[0] = true;
                    return;
                }
                deny(grant.permissionCode());
            }
        };
        if (transactionTemplate == null) {
            consumeAll.run();
        } else {
            transactionTemplate.executeWithoutResult(status -> consumeAll.run());
        }
        if (replayedAtConsumption[0]) {
            request.markReplayOnly();
            return;
        }
        if (request != null) {
            // M723-F08：连来源号与权限码一起记，业务失败时据此撤回
            consume.forEach((source, grant) -> request.markConsumed(actor.id(), source, grant.sourceId(), grant.permissionCode()));
        }
    }

    /**
     * M723-F08（第 656 行「操作被拒绝或失败的不算用掉」）：业务以正常返回报告失败时，由请求事务切面在同一请求事务里调用——
     * 把本请求记在名下的每个单次消耗撤回（来源改回未消耗、追加撤回留痕），失败留痕与幂等记录照常提交。
     * 没有请求上下文、本请求没消耗过时无操作。
     */
    public void releaseRequestConsumption() {
        IdempotentReplayContext request = IdempotentReplayContext.current();
        if (request == null || request.consumedBy() == null) {
            return;
        }
        request.consumedSourceCodes().forEach((sourceId, permissionCode) ->
                accessRepository.releaseSingleUsePermissionConsumption(sourceId, permissionCode, request.consumedBy(),
                        request.idempotencyKey(), request.requestDigest()));
    }

    private static String sourceKey(AccessRepository.PermissionEffect effect) {
        return effect.source() + ":" + effect.sourceId();
    }

    /**
     * XM-M002-J：当前主体是否持有尚未用掉的单次授权（只读）。未登录或无法识别的主体没有可消耗的来源，返回 false。
     * 供 {@link SingleUseGrantRequestTransactionAspect} 判断是否要把整个请求包进一个事务。
     */
    public boolean currentActorHoldsUnconsumedSingleUseGrant() {
        AccessActor actor;
        try {
            actor = currentActor();
        } catch (ApiException unauthenticated) {
            return false;
        }
        return actor.id() != null && accessRepository.hasUnconsumedSingleUseGrant(actor.id());
    }

    /**
     * 返回 null：身份放行（最高权限等）或按重放放行，均不消耗；否则返回授出来源（单次来源须消耗）。拒绝时抛 403。
     * 主体停用、未知权限码、显式拒绝一律先于重放判定。
     */
    private AccessRepository.PermissionEffect permissionGrant(
            AccessActor actor, String code, List<AccessRepository.PermissionEffect> effects,
            java.util.function.Supplier<java.util.Set<String>> daily,
            java.util.function.Supplier<java.util.function.Predicate<AccessRepository.PermissionEffect>> positionDerived,
            SingleUseReplay replay) {
        if (!"active".equals(actor.status()) || !knownPermission(code)) {
            deny(code);
        }
        List<AccessRepository.PermissionEffect> matching = effects.stream()
                .filter(effect -> code.equals(effect.permissionCode())).toList();
        // M002-F33：员工显式拒绝压过全部来源；岗位高危挂起只压住岗位派生的那一份授予（见 positionDerivedSources）
        if (matching.stream().anyMatch(AuthorizationService::isExplicitDeny)) {
            deny(code);
        }
        boolean held = matching.stream().anyMatch(AuthorizationService::isPositionHold);
        if (isMasterAuthority(actor)
                || (isPlatformAdminBridge(actor) && daily.get().contains(code))
                || (isCustomerSelfServiceBridge(actor) && CUSTOMER_SELF_SERVICE_PERMISSIONS.contains(code))) {
            return null;
        }
        AccessRepository.PermissionEffect grant = matching.stream()
                .filter(effect -> effect.effect() == AccessRepository.Effect.GRANT)
                .filter(effect -> isExplicitOrDailyGrant(effect, daily))
                .filter(effect -> !held || !positionDerived.get().test(effect))
                .min(Comparator.comparing(AccessRepository.PermissionEffect::sourceId,
                        Comparator.nullsLast(String::compareTo))).orElse(null);
        // XM-M002-J：取到的是单次来源（或已无来源），而本请求正是用掉它的那次成功请求的重放 → 按重放放行、不再消耗
        if ((grant == null || grant.singleUse()) && replay.test(code)) {
            return null;
        }
        if (grant == null) {
            deny(code);
        }
        return grant;
    }

    /**
     * XM-M002-J（操作员 2026-09-25 答复：单次授权只在业务成功并返回结果时才算用掉；同一请求编号重放应返回第一次的成功结果、
     * 不重复扣减、不重复执行）：本请求是否正是「用掉某单次来源的那次成功请求」的重放。四项同时成立才认：
     * ① 同一操作人、同一幂等键、同一请求（方法 + 路径 + 查询串摘要，见 {@link SingleUseGrantRequestTransactionAspect}）；
     * ② 该来源仍有效（未收回、未到期、在生效时段内），只是已消耗，且消耗日志记着正是这次请求用掉的；
     * ③ 那次请求的幂等记录已成功完成且记着这次消耗（{@link IdempotencyRepository#hasCompletedSingleUseRequest}）——
     *    第一次失败时消耗与幂等记录一起回滚，没有可认的记录；不经幂等层的接口没有这条记录，重放照旧拒绝；
     * ④ 当前在控制器请求里，本请求尚未消耗过别的单次来源。
     * 认出后本请求记为「只能重放」，幂等层只返回第一次的结果、不再执行（{@link IdempotentReplayContext#replayOnly}）。
     */
    private final class SingleUseReplay implements java.util.function.Predicate<String> {

        private final IdempotentReplayContext request;
        private final String actorId;
        private final List<AccessRepository.PermissionEffect> consumedInForce;
        private java.util.Set<String> consumedSources;
        private Boolean completed;
        private boolean matched;

        private SingleUseReplay(AccessActor actor, List<AccessRepository.PermissionEffect> all, Instant checkedAt) {
            IdempotentReplayContext current = IdempotentReplayContext.current();
            this.request = current != null && current.identified() && current.consumedBy() == null ? current : null;
            this.actorId = actor.id();
            this.consumedInForce = all.stream()
                    .filter(effect -> effect.effect() == AccessRepository.Effect.GRANT
                            && effect.singleUse() && effect.consumed() && inForce(effect, checkedAt))
                    .toList();
        }

        @Override
        public boolean test(String code) {
            if (request == null || actorId == null || idempotencyRepository == null) {
                return false;
            }
            List<String> sources = consumedInForce.stream()
                    .filter(effect -> code.equals(effect.permissionCode()))
                    .map(AccessRepository.PermissionEffect::sourceId)
                    .toList();
            if (sources.isEmpty()) {
                return false;
            }
            if (consumedSources == null) {
                consumedSources = new java.util.HashSet<>(accessRepository.singleUseSourcesConsumedBy(
                        actorId, request.idempotencyKey(), request.requestDigest()));
            }
            if (sources.stream().noneMatch(consumedSources::contains)) {
                return false;
            }
            if (completed == null) {
                completed = idempotencyRepository.hasCompletedSingleUseRequest(
                        actorId, request.idempotencyKey(), request.requestDigest());
            }
            matched |= completed;
            return completed;
        }

        private boolean matched() {
            return matched;
        }

        private IdempotentReplayContext request() {
            return request;
        }
    }

    /**
     * 日常权限码集合（active、无需审批、low/medium）。M006-035：一次授权判定或有效集计算内只读一次权限字典，
     * 由调用方持有本 Supplier 复用；首次 get() 才查库，普通员工路径不触发。
     */
    private java.util.function.Supplier<java.util.Set<String>> dailyPermissions() {
        return new java.util.function.Supplier<>() {
            private java.util.Set<String> cached;

            @Override
            public java.util.Set<String> get() {
                if (cached == null) {
                    cached = accessRepository.listPermissions().stream()
                            .filter(p -> "active".equals(p.status()) && !p.requiresApproval()
                                    && java.util.Set.of("low", "medium").contains(normalize(p.riskLevel())))
                            .map(PermissionDefinition::permissionCode)
                            .collect(java.util.stream.Collectors.toSet());
                }
                return cached;
            }
        };
    }

    /**
     * M006-I9-01：HTML 指派给平台管理员的权限治理本职（PGM-1.10.1 角色装配「平台管理员配置各岗位权限模板，并定职位/层级对应哪个模板」；
     * BSM-1.3.4「拥有系统里全部日常功能权限…不会出现"管理员反而没权限"」）。这些码本身是「提权」类（授给他人须审批），
     * 但管理员自己做本职不受授出审批限制：随默认管理员角色（role_platform_admin）生效。资金 / 敏感数据 / 删除 / 最终业务审批
     * （如 access.grant_request.review）不在此列，仍须逐项授予并留痕。
     */
    static final java.util.Set<String> PLATFORM_ADMIN_GOVERNANCE_PERMISSIONS = java.util.Set.of(
            "access.role.assign", "access.role.manage",
            "access.permission_template.manage", "access.permission_template.apply", "access.permission_template.revoke",
            "access.staff_level.manage", "access.user.manage",
            "access.temporary_grant.create", "access.temporary_grant.revoke");

    private static boolean isExplicitDeny(AccessRepository.PermissionEffect effect) {
        return effect.effect() == AccessRepository.Effect.DENY && effect.source() != AccessRepository.Source.POSITION_HOLD;
    }

    private static boolean isPositionHold(AccessRepository.PermissionEffect effect) {
        return effect.effect() == AccessRepository.Effect.DENY && effect.source() == AccessRepository.Source.POSITION_HOLD;
    }

    /**
     * M002-F33（依据：治理地图第 655 行「最终权限 = 角色权限 + 基础模板 + 有效临时模板 + 员工单独授予 − 显式拒绝；显式拒绝优先级最高」；
     * 以下为技术推导）：岗位高危挂起不是显式拒绝，只压住岗位派生的那一份授予——员工岗位行记下的岗位派生角色（未另行直接绑定）与岗位派生
     * 模板套用。员工单独授予、直接角色绑定、非岗位模板套用与临时授权照常生效。首次用到才查库（只在该码有挂起时），一次判定内复用。
     */
    private java.util.function.Supplier<java.util.function.Predicate<AccessRepository.PermissionEffect>> positionDerivedSources(String userId) {
        java.util.concurrent.atomic.AtomicReference<java.util.function.Predicate<AccessRepository.PermissionEffect>> memo =
                new java.util.concurrent.atomic.AtomicReference<>();
        return () -> {
            if (memo.get() == null) {
                String positionRole = accessRepository.findAssignedPositionRoleId(userId).orElse(null);
                java.util.Set<String> positionApplications = accessRepository.findPositionApplications(userId).stream()
                        .map(PermissionTemplateApplication::id).collect(java.util.stream.Collectors.toSet());
                java.util.Set<String> directRoles = positionRole == null ? java.util.Set.of() : directRoleIds(userId);
                memo.set(effect -> switch (effect.source()) {
                    case ROLE -> effect.sourceId() != null && effect.sourceId().equals(positionRole)
                            && !directRoles.contains(effect.sourceId());
                    case TEMPLATE_APPLICATION -> positionApplications.contains(effect.sourceId());
                    default -> false;
                });
            }
            return memo.get();
        };
    }

    // 不支持直接角色查询的仓储按「没有另行直接绑定」处理（挂起照常压住岗位派生角色）。
    private java.util.Set<String> directRoleIds(String userId) {
        try {
            return new java.util.HashSet<>(accessRepository.findUserRoleIds(userId));
        } catch (UnsupportedOperationException unsupported) {
            return java.util.Set.of();
        }
    }

    private boolean isExplicitOrDailyGrant(AccessRepository.PermissionEffect effect,
                                           java.util.function.Supplier<java.util.Set<String>> daily) {
        // 默认管理员角色随权限字典播种，不能把其高危条目当成逐项授权；治理本职码除外（M006-I9-01）。
        return effect.source() != AccessRepository.Source.ROLE
                || !"role_platform_admin".equals(effect.sourceId())
                || daily.get().contains(effect.permissionCode())
                || PLATFORM_ADMIN_GOVERNANCE_PERMISSIONS.contains(effect.permissionCode());
    }

    public boolean isMasterAuthority(AccessActor actor) {
        return "active".equals(actor.status())
                && SessionIssuer.MASTER_ACCOUNT_TYPE.equals(normalize(actor.accountType()))
                && SessionIssuer.MASTER_USER_ID.equals(actor.id())
                && SessionIssuer.MASTER_USERNAME.equals(actor.username());
    }

    /** 供定时任务核实指定收件人的实际查看能力，不切换会话或消耗单次授权。 */
    public boolean hasEffectivePermission(AccessActor actor, String permissionCode) {
        if (actor == null || !"active".equals(actor.status()) || !knownPermission(permissionCode)) return false;
        try {
            Optional<AccessRepository.UserIdentity> identity = identityAtStart(actor);
            EffectivePermissions result = computeEffectivePermissions(
                    actor.id(), isPlatformAdminBridge(actor), isMasterAuthority(actor), effect -> true);
            requireIdentityUnchanged(actor, identity);
            return result.grants().contains(permissionCode);
        } catch (ApiException denied) {
            return false;
        }
    }

    /**
     * 当前 actor 自身的有效权限集（self 端，只读）。不消费单次授权——
     * 单次授权仅在真正执行动作的 {@link #requirePermission} 中消费，避免"看一眼菜单就烧掉"。
     */
    public EffectivePermissions effectivePermissions() {
        AccessActor actor = currentActor();
        Optional<AccessRepository.UserIdentity> identity = identityAtStart(actor); // M722-F05：读能力之前
        EffectivePermissions result = computeEffectivePermissions(
                actor.id(), isPlatformAdminBridge(actor), isMasterAuthority(actor), effect -> true);
        requireIdentityUnchanged(actor, identity);
        return result;
    }

    /**
     * M723-F13：当前操作人在本请求里持有的权限集（只读）——来源口径与动作核验相同（{@link #usable}）：有效来源，加上本请求里这名操作人
     * 刚消耗、仍在生效时段内的单次来源。供授出入口判断「只凭临时或单次来源持有」用：入口权限与待授码出自同一份单次来源时，入口核权已把它
     * 消耗掉，按 {@link #effectivePermissions} 算会把待授码当成从未持有。菜单与前端展示仍用 {@link #effectivePermissions}。
     */
    public EffectivePermissions effectivePermissionsWithinRequest() {
        AccessActor actor = currentActor();
        Optional<AccessRepository.UserIdentity> identity = identityAtStart(actor);
        IdempotentReplayContext request = IdempotentReplayContext.current();
        EffectivePermissions result = computeEffectivePermissions(actor.id(), isPlatformAdminBridge(actor), isMasterAuthority(actor),
                effect -> true, (effect, checkedAt) -> usable(effect, checkedAt, actor, request));
        requireIdentityUnchanged(actor, identity);
        return result;
    }

    /**
     * XM-M002-I（操作员 2026-09-25 裁定口径 B「一律只认长期来源」）：当前操作人可作为授出依据的权限集（只读）。一切授出只认长期来源：
     * 角色（含岗位派生角色）、无到期且非单次的模板套用、员工单独授予，以及随账号身份的平台管理员日常码与最高权限全字典；
     * 临时授权、有到期或单次的模板套用（含临时模板、单次模板）不作任何授出的依据。显式拒绝照常扣减。
     */
    public EffectivePermissions grantablePermissions() {
        AccessActor actor = currentActor();
        Optional<AccessRepository.UserIdentity> identity = identityAtStart(actor); // M722-F05：读能力之前
        EffectivePermissions result = computeEffectivePermissions(actor.id(), isPlatformAdminBridge(actor), isMasterAuthority(actor),
                effect -> effect.source() != AccessRepository.Source.TEMPORARY_GRANT && effect.validUntil() == null && !effect.singleUse());
        requireIdentityUnchanged(actor, identity); // 同 effectivePermissions：按操作人读完权限后核身份
        return result;
    }

    /** 指定用户的有效权限集（平台管理用；调用方须先校验 {@code access.user.view}）。M736：可触及项目按该码算。 */
    public EffectivePermissions effectivePermissionsForUser(String userId) {
        return effectivePermissionsForUser(userId, "access.user.view");
    }

    /**
     * M736：同上，可触及项目按给定的入口权限码现算。现算要按操作人读能力事实（其中有到期处理的写入），
     * 已持有员工行锁的调用方改用传入 reach 的重载。
     */
    public EffectivePermissions effectivePermissionsForUser(String userId, String permissionCode) {
        return effectivePermissionsForUser(userId, () -> projectReach(currentActor(), permissionCode));
    }

    /** M736：同上，可触及项目用调用方在取锁前算好的 reach（{@link #projectReach}）。 */
    public EffectivePermissions effectivePermissionsForUser(String userId, ProjectReach reach) {
        return effectivePermissionsForUser(userId, () -> reach);
    }

    // 判定体。项目半边按可触及项目判；平台管理员与最高权限照旧放行，不取 reach。商户半边不变。
    private EffectivePermissions effectivePermissionsForUser(String userId, java.util.function.Supplier<ProjectReach> reach) {
        AccessActor actor = currentActor();
        AccessUser user = accessRepository.findUserById(userId)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "账号不存在"));
        if (!isPlatformAdmin(actor) && !isMasterAuthority(actor)) {
            if (!canReachUser(reach.get(), user)) {
                throw ApiException.forbidden("CROSS_ORG_FORBIDDEN", "无权查询其他组织的用户");
            }
            if (actor.merchantId() != null && !actor.merchantId().isBlank()
                    && !actor.merchantId().equals(user.merchantId())) {
                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "无权查询其他商户的用户");
            }
        }
        AccessActor subject = new AccessActor(user.id(), user.username(), user.accountType(), user.organizationId(),
                user.merchantId(), user.status());
        return computeEffectivePermissions(userId, isPlatformAdminBridge(subject), isMasterAuthority(subject), effect -> true);
    }

    private EffectivePermissions computeEffectivePermissions(String userId, boolean platformAdmin, boolean master,
            java.util.function.Predicate<AccessRepository.PermissionEffect> grantBasis) {
        return computeEffectivePermissions(userId, platformAdmin, master, grantBasis, this::isActive);
    }

    // inEffect：哪些来源算数——默认「未消耗且在生效时段内」；M723-F13 的请求内口径另算上本请求刚消耗的单次来源
    private EffectivePermissions computeEffectivePermissions(String userId, boolean platformAdmin, boolean master,
            java.util.function.Predicate<AccessRepository.PermissionEffect> grantBasis,
            java.util.function.BiPredicate<AccessRepository.PermissionEffect, Instant> inEffect) {
        Instant checkedAt = Instant.now();
        if (accessRepository.isEmploymentBusinessBlocked(userId, checkedAt)) {
            return new EffectivePermissions(userId, List.of(), List.of(), false, checkedAt);
        }
        java.util.function.Supplier<java.util.Set<String>> daily = dailyPermissions();
        List<AccessRepository.PermissionEffect> active = accessRepository.findPermissionEffects(userId, checkedAt).stream()
                .filter(effect -> inEffect.test(effect, checkedAt))
                .toList();
        // M002-F33：员工显式拒绝压过全部来源；岗位高危挂起只压住岗位派生的那一份授予，与 permissionGrant 同口径。
        java.util.Set<String> explicitDenies = active.stream().filter(AuthorizationService::isExplicitDeny)
                .map(AccessRepository.PermissionEffect::permissionCode).collect(java.util.stream.Collectors.toSet());
        java.util.Set<String> held = active.stream().filter(AuthorizationService::isPositionHold)
                .map(AccessRepository.PermissionEffect::permissionCode).collect(java.util.stream.Collectors.toSet());
        java.util.function.Supplier<java.util.function.Predicate<AccessRepository.PermissionEffect>> positionDerived =
                positionDerivedSources(userId);
        // 实现 A：grants 预扣显式拒绝（DENY 优先）与被挂起压住的岗位派生授予。
        List<String> grants = active.stream()
                .filter(effect -> effect.effect() == AccessRepository.Effect.GRANT)
                .filter(effect -> isExplicitOrDailyGrant(effect, daily))
                .filter(grantBasis)
                .filter(effect -> !held.contains(effect.permissionCode()) || !positionDerived.get().test(effect))
                .map(AccessRepository.PermissionEffect::permissionCode)
                .filter(code -> !explicitDenies.contains(code))
                .distinct()
                .sorted()
                .toList();
        java.util.Set<String> effective = new java.util.TreeSet<>(grants);
        // M002-F03（R-001「有效集并入全部权限字典，显式拒绝仍优先」）：最高权限并入全部 active 码，与 permissionGrant 对最高权限的放行同口径；
        // 平台管理员只并入日常码。
        if (platformAdmin || master) {
            accessRepository.listPermissions().stream()
                    .filter(p -> master || daily.get().contains(p.permissionCode()))
                    .map(PermissionDefinition::permissionCode).forEach(effective::add);
        }
        effective.removeAll(explicitDenies);
        // denies 仍单独返回供前端展示（前端以 denies 压过 grants）：员工显式拒绝，加上被挂起压住、又没有其他来源的码。
        List<String> denies = java.util.stream.Stream.concat(explicitDenies.stream(),
                        held.stream().filter(code -> !effective.contains(code)))
                .distinct().sorted().toList();
        return new EffectivePermissions(userId, List.copyOf(effective), denies, false, checkedAt);
    }

    public AccessActor currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw ApiException.unauthorized("UNAUTHENTICATED", "需要登录后操作");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AccessActor actor) {
            return actor;
        }
        if (principal instanceof AuthService.UserRecord user) {
            return new AccessActor(
                    user.id(),
                    user.username(),
                    user.accountType(),
                    user.organizationId(),
                    user.merchantId(),
                    user.status()
            );
        }
        throw ApiException.unauthorized("UNAUTHENTICATED", "无法识别的认证主体");
    }

    // XM-M001-AF、M722-F05（治理地图第 653 行项目与商户隔离、第 658 行跨项目或换商户后旧范围派生权限立即失效；技术推导）：
    // 一次判定在按操作人读取能力事实之前、最后一次读取之后各读一次账号行——项目、商户、账号类型、状态两次都须与会话一致，
    // 行版本须前后相同；否则（在途调岗、换商户、停用，含改走又改回）按不具备处理。不加锁、不需同一快照；
    // 仓储不给行版本时只比四项。

    // 读账号行四项＋行版本。无仓储（与 canResolveRoles() 同口径）、仓储不支持按号读员工（与 directRoleIds 同口径）→ 空，不核对。
    private Optional<AccessRepository.UserIdentity> readIdentity(String userId) {
        if (accessRepository == null) {
            return Optional.empty();
        }
        try {
            return accessRepository.findUserIdentity(userId);
        } catch (UnsupportedOperationException unsupported) {
            return Optional.empty();
        }
    }

    /** 判定开始：任何按操作人读取能力事实之前调用。账号行与会话不一致即拒绝；返回值原样交给 requireIdentityUnchanged。 */
    private Optional<AccessRepository.UserIdentity> identityAtStart(AccessActor actor) {
        Optional<AccessRepository.UserIdentity> row = readIdentity(actor.id());
        // 空 = 主体不来自员工表（客人、调度主体）：没有可被换项目/停用改写的员工事实
        row.ifPresent(identity -> requireMatchesSession(actor, identity));
        return row;
    }

    /** 判定末尾：最后一次按操作人读取之后、单次来源消费之前调用。四项仍须与会话一致，行版本须与开始时相同。 */
    private void requireIdentityUnchanged(AccessActor actor, Optional<AccessRepository.UserIdentity> start) {
        Optional<AccessRepository.UserIdentity> end = readIdentity(actor.id());
        end.ifPresent(identity -> requireMatchesSession(actor, identity));
        if (!java.util.Objects.equals(start.map(AccessRepository.UserIdentity::rowVersion),
                end.map(AccessRepository.UserIdentity::rowVersion))) {
            throw identityChanged();
        }
    }

    private void requireMatchesSession(AccessActor actor, AccessRepository.UserIdentity identity) {
        if (accessRepository.isEmploymentBusinessBlocked(actor.id(), Instant.now())) {
            throw ApiException.forbidden("STAFF_BUSINESS_ACCESS_STOPPED", "停岗或离职已生效，员工业务权限已停止");
        }
        if (!java.util.Objects.equals(actor.organizationId(), identity.organizationId())
                || !java.util.Objects.equals(actor.merchantId(), identity.merchantId())
                || !java.util.Objects.equals(actor.accountType(), identity.accountType())
                || !java.util.Objects.equals(actor.status(), identity.status())) {
            throw identityChanged();
        }
    }

    private ApiException identityChanged() {
        return ApiException.forbidden("PERMISSION_DENIED", "账号归属或状态已变更，本次操作未执行");
    }

    // 当前主体且编号等于 userId 时返回它；无认证、非员工主体（如 MockMvc user()）、查他人 → null（不核对，保持原行为）。
    private AccessActor selfOrNull(String userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        AccessActor self;
        try {
            self = currentActor();
        } catch (ApiException unrecognized) {
            return null;
        }
        return userId.equals(self.id()) ? self : null;
    }

    /** 已登录请求：不带商户归属的按操作人与组织隔离，带商户归属的按所属项目与所属商户隔离；安全上下文无认证时沿用原范围（空）。 */
    @Override
    public String resolve() {
        String legacy = legacyScope();
        if (legacy.isEmpty()) {
            return legacy; // 无认证：原样 ""
        }
        AccessActor actor = currentActor();
        if (actor.id() == null || actor.id().isBlank()) {
            throw ApiException.unauthorized("UNAUTHENTICATED", "无法识别的认证主体");
        }
        String id = actor.id();
        String org = java.util.Objects.toString(actor.organizationId(), "");
        if ("p".equals(legacy)) {
            return "u:" + id.length() + ":" + id + ":" + org; // 非商户主体：与 #415 形态逐字节相同
        }
        // XM-M002-BH、M722-F07（治理地图第 303、528、653 行；操作员 2026-10-03 裁决「同项目同商户内认一次」）：
        // 带商户归属的主体按所属项目＋所属商户算提交范围——同项目同商户的员工用同一请求编号只认一次，其他范围再用同一编号即拒绝
        // （M724；操作员 2026-10-04 裁决「同一提交编号只能提交一次不允许多次提交」）。
        return "mo:" + org.length() + ":" + org + ":" + actor.merchantId();
    }

    @Override
    public String legacyScope() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // ROOT-03-R7（ENUM 第 129 行「非会员可以浏览、点单和付款」）：未登录访客（框架匿名令牌）同无认证，按原样 "" 归口。
        if (auth == null || !auth.isAuthenticated()
                || auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return "";
        }
        AccessActor actor = currentActor();
        String merchantId = actor.merchantId();
        return merchantId == null || merchantId.isBlank() ? "p" : "m:" + merchantId;
    }

    private boolean knownPermission(String permissionCode) {
        try {
            return accessRepository.permissionExists(permissionCode);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * 当前主体是否持有某权限(布尔·不抛错):用于按权限脱敏字段(如内部结算价)。
     * XM-M002-J：对重放的判定与 {@link #requirePermissions} 一致——按重放认出时同样把本请求记为「只能重放」。
     * M723：本请求已消耗的单次来源在本请求内仍视为有效（同 {@link #requirePermissions}）。
     */
    public boolean currentActorHasPermission(String permissionCode) {
        try {
            Instant checkedAt = Instant.now();
            AccessActor actor = currentActor();
            Optional<AccessRepository.UserIdentity> identity = identityAtStart(actor); // M722-F05：读能力之前
            IdempotentReplayContext request = IdempotentReplayContext.current();
            List<AccessRepository.PermissionEffect> all = accessRepository.findPermissionEffects(actor.id(), checkedAt);
            List<AccessRepository.PermissionEffect> effects = all.stream()
                    .filter(effect -> usable(effect, checkedAt, actor, request)).toList();
            SingleUseReplay replay = new SingleUseReplay(actor, all, checkedAt);
            permissionGrant(actor, permissionCode, effects, dailyPermissions(), positionDerivedSources(actor.id()), replay);
            requireIdentityUnchanged(actor, identity); // 在最后一次按操作人 id 读取（岗位派生事实）之后；核对不过不留重放标记
            if (replay.matched()) {
                replay.request().markReplayOnly();
            }
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean isPlatformAdmin(AccessActor actor) {
        return isPlatformAdminBridge(actor);
    }

    public boolean isOutsourcedVendor(AccessActor actor) {
        return actor != null
                && "active".equals(actor.status())
                && "outsourced_vendor".equals(normalize(actor.accountType()));
    }

    public String requireProjectScope(AccessActor actor) {
        String projectId = actor == null ? null : actor.organizationId();
        if (projectId == null || projectId.isBlank()) {
            throw ApiException.forbidden("PROJECT_SCOPE_REQUIRED", "此操作必须在项目范围内进行");
        }
        return projectId;
    }

    /**
     * M736（XM-M001-D 可跨项目角色；操作员 2026-10-01 确认的治理地图第 653 行追加句：「被分配了可跨项目角色的员工（含经岗位得到），
     * 在所分配的权限内可以查看、处理该角色可查看项目的数据。」）：一次判定算出的可触及项目范围。
     * platformAdmin＝平台管理员（一切，D-032）；allProjects＝经勾了「可以查看全部项目」的角色持有入口权限码；ownProject＝本人项目；
     * ownBySource＝入口权限码另有不是可跨项目角色的来源（普通角色、员工单独授予、模板、临时授权；M736 第二步：带保留标记的老持有人
     * 所持的、对他有效的勾选一条不剩的角色也算，见 {@link #projectReach}）；
     * projects＝可跨项目角色勾中、且在当时项目集合内的归属。
     * 本人项目只在 ownBySource 或被勾中时才算——权限码只来自「只勾了别的项目」的角色时，本人项目也碰不到
     * （同一追加句：「逐个勾选的只能查看被选中的项目」）。
     */
    public record ProjectReach(boolean platformAdmin, boolean allProjects, String ownProject,
                               boolean ownBySource, Set<String> projects) {
        public ProjectReach {
            projects = projects == null ? Set.of() : Set.copyOf(projects);
        }

        /** 只到本人项目：没有经可跨项目角色持有入口权限码的人，以及停用、带商户归属、外包账号。 */
        public static ProjectReach ownOnly(String ownProject) {
            return new ProjectReach(false, false, ownProject, true, Set.of());
        }

        public boolean ownIncluded() {
            return ownBySource || (ownProject != null && projects.contains(ownProject));
        }

        public boolean covers(String organizationId) {
            return platformAdmin || (organizationId != null
                    && ((ownIncluded() && organizationId.equals(ownProject)) || projects.contains(organizationId)));
        }

        /** 恰好只能触及本人项目时返回它（各列表据此走原来的单项目查询），否则空。 */
        public Optional<String> onlyOwnProject() {
            return !platformAdmin && ownProject != null && organizationIds().equals(Set.of(ownProject))
                    ? Optional.of(ownProject) : Optional.empty();
        }

        /** 非平台管理员的全部可触及归属（可能为空）。平台管理员不看这个集合。 */
        public Set<String> organizationIds() {
            if (platformAdmin || !ownBySource || ownProject == null) {
                return projects;
            }
            Set<String> ids = new java.util.HashSet<>(projects);
            ids.add(ownProject);
            return ids;
        }

        /** 入口要求多个权限码时，各码的范围取交集。 */
        public ProjectReach intersect(ProjectReach other) {
            if (platformAdmin) {
                return other;
            }
            if (other.platformAdmin) {
                return this;
            }
            Set<String> both = new java.util.HashSet<>(projects);
            both.retainAll(other.projects);
            // 本人项目：一边凭其他来源、另一边凭勾选时，仍在交集里
            if (ownProject != null && ((ownBySource && other.projects.contains(ownProject))
                    || (other.ownBySource && projects.contains(ownProject)))) {
                both.add(ownProject);
            }
            return new ProjectReach(false, allProjects && other.allProjects, ownProject,
                    ownBySource && other.ownBySource, both);
        }
    }

    /**
     * M736：操作人按入口权限码可触及的项目范围。按顺序判：平台管理员 → 一切；本人没有项目归属 → 照旧 403；
     * 账号停用、带商户归属、外包账号 → 只到本人项目（操作员 2026-09-25「商户账号。是不能跨项目的。」；治理地图第 675 行「也不能跨出本综合体」）；
     * 其余逐码取有效授予（{@link #validGrants}，来源是否在效与入口核权同一口径 {@link #usable}）——来源是可跨项目角色的
     * （直接分配与岗位派生都算，操作员 2026-09-25「也算」）累计「勾了全部」与勾选清单，其余来源记本人项目；
     * 勾了全部的取当时的项目集合，否则取勾选清单与项目集合的交集（操作员 2026-10-01 所选选项原文：「「全部项目」始终指所有项目，
     * 含以后新建的；只有展开后逐个勾选的，才只管勾中的那几个。」）。同一个码有多个可跨项目角色取并集，多个码取交集；
     * 一个码都不给按只到本人项目。
     * M736 第二步：每个可跨项目角色累计的是「对这个持有人有效的勾选」——减掉他对该角色的待批条目；减完一条不剩的，
     * 这个角色的权限对他哪个项目都不给，只有带保留标记的人仍只到本人项目（见循环内注释）。
     * 按操作人读能力事实，包在 M722-F05 的判定窗口里。读取会触发到期处理的写入（findPermissionEffects），
     * 调用方须在取员工行锁之前算好，把结果传给后面的闸。
     */
    public ProjectReach projectReach(AccessActor actor, String... permissionCodes) {
        if (isPlatformAdminBridge(actor)) {
            return new ProjectReach(true, true, actor.organizationId(), true, Set.of());
        }
        String ownProject = requireProjectScope(actor);
        ProjectReach ownOnly = ProjectReach.ownOnly(ownProject);
        String merchantId = actor.merchantId();
        if (!"active".equals(actor.status()) || (merchantId != null && !merchantId.isBlank()) || isOutsourcedVendor(actor)) {
            return ownOnly;
        }
        Optional<AccessRepository.UserIdentity> identity = identityAtStart(actor); // M722-F05：读能力之前
        Instant checkedAt = Instant.now();
        IdempotentReplayContext request = IdempotentReplayContext.current();
        List<AccessRepository.PermissionEffect> effects = accessRepository.findPermissionEffects(actor.id(), checkedAt);
        java.util.function.Supplier<java.util.Set<String>> daily = dailyPermissions();
        java.util.function.Supplier<java.util.function.Predicate<AccessRepository.PermissionEffect>> positionDerived =
                positionDerivedSources(actor.id());
        // 本次判定内按编号读过的角色。ponytail: 含入口权限码的角色逐个按编号读；一人挂的角色多到成为负担时改成一次批量取勾选。
        java.util.Map<String, Optional<RoleRecord>> roles = new java.util.HashMap<>();
        java.util.Map<String, Set<String>> holds = null;
        ProjectReach reach = null;
        for (String code : new java.util.LinkedHashSet<>(java.util.Arrays.asList(permissionCodes))) {
            boolean ownBySource = false;
            boolean allProjects = false;
            boolean nothingBySource = false; // 有可跨项目角色含该码，但对他有效的勾选一条不剩、他又不带保留标记
            Set<String> ticked = new java.util.HashSet<>();
            for (AccessRepository.PermissionEffect grant : validGrants(effects, code,
                    effect -> usable(effect, checkedAt, actor, request), daily, positionDerived).toList()) {
                RoleRecord role = grant.source() == AccessRepository.Source.ROLE && grant.sourceId() != null
                        ? roles.computeIfAbsent(grant.sourceId(), accessRepository::findRoleById).orElse(null) : null;
                if (isCrossProjectRole(role)) {
                    // M736 第二步（操作员 2026-10-03 所选选项原文「直接在原角色上勾 C；但张三等所有持有人要各自重新审批通过，才能看 C。」）：
                    // 改之前已持有该角色的人，对后来加上的条目在重新审批通过之前不算；有「全部」那一条待批的，按角色上留着的清单算。
                    // 以下为技术推导：对他有效的勾选一条不剩时，这个角色的权限对他哪个项目都不给——原来生效的勾选被减掉（同日所选选项原文
                    // 「减少也要审批通过后才生效；审批期间持有人仍能看 C。」，通过后即生效）、新加的还没重新审批的人，不能反而到了
                    // 从没被勾过的本人项目。只有带保留标记的人（角色原来一条勾选都没有、第一次勾上项目时已持有它，HELD_SINCE_UNTICKED）
                    // 仍按没勾选的角色算：否则第一次勾上项目的角色会让老持有人丢掉本人项目。
                    if (holds == null) {
                        holds = accessRepository.findRoleViewableProjectHolds(actor.id()); // 本次判定最多读一次；没有可跨项目角色的人不读
                    }
                    Set<String> held = holds.getOrDefault(role.id(), Set.of());
                    boolean all = role.viewAllProjects() && !held.contains(ALL_PROJECTS);
                    List<String> list = role.viewableProjectIds().stream().filter(id -> !held.contains(id)).toList();
                    if (all || !list.isEmpty()) {
                        allProjects |= all;
                        ticked.addAll(list);
                    } else if (held.contains(HELD_SINCE_UNTICKED)) {
                        ownBySource = true;
                    } else {
                        nothingBySource = true;
                    }
                } else {
                    ownBySource = true;
                }
            }
            // 没有有效授予，或没有任何可跨项目角色含该码：只到本人项目，与原先一致（不查项目集合）
            ProjectReach each = ownOnly;
            if (allProjects || !ticked.isEmpty()) {
                Set<String> projectSet = projectOrganizationIds();
                ticked.retainAll(projectSet);
                each = new ProjectReach(false, allProjects, ownProject, ownBySource, allProjects ? projectSet : ticked);
            } else if (nothingBySource && !ownBySource) {
                // M736 第二步：该码只来自对他有效的勾选一条不剩的可跨项目角色——哪个项目都不到，本人项目也不到
                each = new ProjectReach(false, false, ownProject, false, Set.of());
            }
            reach = reach == null ? each : reach.intersect(each);
        }
        // 岗位派生事实在上面的循环里才按操作人 id 读取：核对放在最后一次读取之后
        requireIdentityUnchanged(actor, identity);
        return reach == null ? ownOnly : reach;
    }

    /** M736：某归属（项目，或城市项目公司、域主管公司）是否在操作人按入口权限码可触及的范围内。 */
    public boolean canReachProject(AccessActor actor, String organizationId, String... permissionCodes) {
        return projectReach(actor, permissionCodes).covers(organizationId);
    }

    /** 第十八批 G 段 82b-R4：{@link #requireReachableProject} 拒绝时各按项目配置入口共用的提示。 */
    public static final String PROJECT_OUT_OF_REACH = "所选项目不在本权限可触及的项目范围内";

    /**
     * 第十八批 G 段 82b-R4（PGM 第 659 行「在所分配的权限内可以查看、处理该角色可查看项目的数据」「逐个勾选的只能查看被选中的项目」；
     * 第 528 行「不受第④类「别的项目」限制」）：按入口权限码取一个项目。先核该码；不带项目取本人项目（平台管理员没有本人项目，
     * 400 VALIDATION_FAILED「必须指定综合体项目」；没有项目归属的其余账号 403 PROJECT_SCOPE_REQUIRED）；项目不在该码可触及范围内
     * （{@link #canReachProject}：含可跨项目角色勾选的项目；只经「只勾了别的项目」的角色持码时本人项目也不在内）403 CROSS_PROJECT_FORBIDDEN。
     * 返回去掉首尾空白的项目编号。
     */
    public String requireReachableProject(String requested, String permission, String deniedMessage) {
        requirePermission(permission);
        String project = requested == null || requested.isBlank() ? null : requested.trim();
        if (project == null) {
            project = currentProjectScopeQuietly()
                    .orElseThrow(() -> ApiException.badRequest("VALIDATION_FAILED", "必须指定综合体项目"));
        }
        if (!canReachProject(currentActor(), project, permission)) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", deniedMessage);
        }
        return project;
    }

    /**
     * M736：目标账号是否在可触及范围内（reach 由调用方在取锁前算好）。目标就在本人项目的，与原先一致（含本项目的外包账号），
     * 只看本人项目算不算。跨出本人项目只到项目为止（操作员 2026-10-01 确认的治理地图第 653 行追加句：「只能触及归属在项目或项目公司下
     * （含在用的域主管公司）的账号与数据，平台公司员工、平台管理员、最高权限、外部机构账号一律碰不到。」）：平台管理员账号、
     * 外部机构账号、最高权限账号一律不可触及，其余看归属是否被勾中——平台公司员工的归属不在项目集合里，自然不可触及。
     */
    public boolean canReachUser(ProjectReach reach, AccessUser target) {
        if (reach.platformAdmin()) {
            return true;
        }
        String organizationId = target.organizationId();
        if (organizationId != null && organizationId.equals(reach.ownProject())) {
            return reach.ownIncluded();
        }
        String accountType = normalize(target.accountType());
        // 最高权限账号按账号身份字段识别，不看当前状态
        if ("platform_admin".equals(accountType) || "outsourced_vendor".equals(accountType)
                || isMasterAuthority(new AccessActor(target.id(), target.username(), target.accountType(),
                        target.organizationId(), target.merchantId(), "active"))) {
            return false;
        }
        return organizationId != null && reach.projects().contains(organizationId);
    }

    /** M736：同上，reach 按入口权限码现算（不得在持有员工行锁的区段里调用，见 {@link #projectReach}）。 */
    public boolean canReachUser(AccessActor actor, AccessUser target, String... permissionCodes) {
        return canReachUser(projectReach(actor, permissionCodes), target);
    }

    /**
     * M736：能不能看全平台合计（操作员 2026-10-03 所选选项原文：「全平台合计只给平台管理员和勾了「全部项目」的人看。
     * 李四只能分别看 A、B 各自的数据。」）——平台管理员，或经勾了「可以查看全部项目」的角色持有该权限码；只逐个勾选了项目的为否。
     */
    public boolean allowsPlatformScope(AccessActor actor, String permissionCode) {
        return isPlatformAdminBridge(actor) || projectReach(actor, permissionCode).allProjects();
    }

    /**
     * M736：可跨项目角色＝启用中，且勾了「可以查看全部项目」或展开后逐个勾选了项目（操作员 2026-10-01 确认的治理地图第 653 行追加句：
     * 「可跨项目角色是在权限配置中勾选了「可以查看全部项目」、或展开后逐个勾选了项目的角色」）。不看 data_scope。
     */
    public static boolean isCrossProjectRole(RoleRecord role) {
        return role != null && "active".equals(role.status())
                && (role.viewAllProjects() || !role.viewableProjectIds().isEmpty());
    }

    /** 当时的项目集合（{@link AccessRepository#findProjectOrganizationIds}）；查询异常按空集处理——谁也跨不出本人项目。 */
    private Set<String> projectOrganizationIds() {
        try {
            return accessRepository.findProjectOrganizationIds();
        } catch (RuntimeException exception) {
            return Set.of();
        }
    }

    public void enforceOutsourcedAssigneeScope(AccessActor actor, String assigneeId, String projectId) {
        if (!isOutsourcedVendor(actor)) {
            return;
        }
        String scopeProjectId = requireProjectScope(actor);
        if (!actor.id().equals(assigneeId) || !scopeProjectId.equals(projectId)) {
            throw ApiException.forbidden("FORBIDDEN", "所分配的资源不在当前权限范围内");
        }
    }

    /** 审批身份须由明确角色权限及实际管理职级共同证明；USB 主授权独立识别。 */
    public int currentSupervisorLevel() {
        AccessActor actor = currentActor();
        if (isMasterAuthority(actor)) {
            // M722-F06：最高权限分支同样走判定窗口——开始时账号行须与会话一致，返回之前行版本须未变；没有账号行或无仓储时不核对。
            try {
                requireIdentityUnchanged(actor, identityAtStart(actor));
            } catch (RuntimeException exception) {
                return 0;
            }
            return 3;
        }
        if (!"active".equals(actor.status()) || accessRepository == null) {
            return 0;
        }
        int rank;
        try {
            // M722-F05：角色事实与职级在同一个判定窗口内读取
            Optional<AccessRepository.UserIdentity> identity = identityAtStart(actor);
            if (!holdsViaRoleFacts(actor.id(), "governance.supervisor_review.approve")) {
                return 0;
            }
            rank = levelRankOf(actor.id());
            requireIdentityUnchanged(actor, identity);
        } catch (RuntimeException exception) {
            return 0;
        }
        int derived = rank > 0 ? (rank + 2) / 3 : 0;
        // 最高权限（第 3 级）只给持密钥主体；职级只映射到上一级/上两级。
        return Math.min(derived, 2);
    }

    /**
     * Returns the current caller's management level_rank (1..30, see staff_levels), or 0 if unknown.
     * Degrades to 0 on null repository or any lookup error so callers can fall back to level-based checks.
     */
    public int currentActorLevelRank() {
        try {
            AccessActor actor = currentActor();
            Optional<AccessRepository.UserIdentity> identity = identityAtStart(actor); // M722-F05：读职级之前
            int rank = isMasterAuthority(actor) ? 30 : levelRankOf(actor.id());
            requireIdentityUnchanged(actor, identity);
            return rank;
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    /** Returns the management level_rank (1..30) of the given user, or 0 if no staff level is assigned/unknown. */
    public int levelRankOf(String userId) {
        if (userId == null || accessRepository == null) {
            return 0;
        }
        try {
            return accessRepository.findStaffLevelAssignment(userId)
                    .flatMap(assignment -> accessRepository.findStaffLevelById(assignment.staffLevelId()))
                    .map(StaffLevelRecord::levelRank)
                    .orElse(0);
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    /**
     * 是否具备角色/隔离键解析能力（仓储可用）。双轨收口的 specific_role 角色闸与同商户范围闸仅在可解析时强制，
     * 在无仓储的轻量上下文（如部分单测/未接入环境）下回退既有 level 闸，避免误伤合法流程。
     */
    public boolean canResolveRoles() {
        return accessRepository != null;
    }

    /**
     * 角色解析（双轨收口 specific_role，platform-governance §24.3）：判断指定用户是否通过其角色
     * （ROLE 来源，非临时授权/模板单发）持有某权限码，从而解析"指定角色"审批闸而非旁路。
     *
     * <p>语义对齐 {@link #computeEffectivePermissions}：DENY 优先（命中 DENY 即不持有）；GRANT 仅当来源为
     * {@link AccessRepository.Source#ROLE} 且在有效期内才算"角色赋予"。不因平台管理员身份补齐审批角色。
     * 仓储为空或查询异常时安全保守地返回 {@code false}（不放行）。
     */
    public boolean holdsPermissionViaRole(String userId, String permissionCode) {
        if (userId == null || permissionCode == null || accessRepository == null) {
            return false;
        }
        try {
            if (accessRepository.isEmploymentBusinessBlocked(userId, Instant.now())) return false;
            AccessActor self = selfOrNull(userId);
            Optional<AccessRepository.UserIdentity> identity = self == null ? Optional.empty() : identityAtStart(self);
            boolean holds = holdsViaRoleFacts(userId, permissionCode);
            if (self != null) {
                requireIdentityUnchanged(self, identity); // 在最后一次按该员工 id 读取（岗位派生事实）之后
            }
            return holds;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    // holdsPermissionViaRole 的判定体（不含身份核对）；currentSupervisorLevel 与它共用，各自包一个判定窗口。
    private boolean holdsViaRoleFacts(String userId, String permissionCode) {
        Instant checkedAt = Instant.now();
        return validGrants(accessRepository.findPermissionEffects(userId, checkedAt), permissionCode,
                effect -> isActive(effect, checkedAt), dailyPermissions(), positionDerivedSources(userId))
                .anyMatch(effect -> effect.source() == AccessRepository.Source.ROLE);
    }

    // 某权限码的有效授予（各种来源），holdsViaRoleFacts 与 projectReach（M736）共用：显式拒绝优先——有即空；岗位高危挂起只压岗位派生的
    // 那一份（M002-F33）；默认管理员角色的高危条目不算逐项授权。inEffect＝哪些来源算在效。
    private java.util.stream.Stream<AccessRepository.PermissionEffect> validGrants(
            List<AccessRepository.PermissionEffect> effects, String permissionCode,
            java.util.function.Predicate<AccessRepository.PermissionEffect> inEffect,
            java.util.function.Supplier<java.util.Set<String>> daily,
            java.util.function.Supplier<java.util.function.Predicate<AccessRepository.PermissionEffect>> positionDerived) {
        List<AccessRepository.PermissionEffect> active = effects.stream()
                .filter(effect -> permissionCode.equals(effect.permissionCode()))
                .filter(inEffect)
                .toList();
        if (active.stream().anyMatch(AuthorizationService::isExplicitDeny)) {
            return java.util.stream.Stream.empty();
        }
        boolean held = active.stream().anyMatch(AuthorizationService::isPositionHold);
        return active.stream()
                .filter(effect -> effect.effect() == AccessRepository.Effect.GRANT)
                .filter(effect -> isExplicitOrDailyGrant(effect, daily))
                .filter(effect -> !held || !positionDerived.get().test(effect));
    }

    /**
     * 返回指定用户的隔离键 merchant_id（用于同商户范围闸），无则空。绝不抛错（查询异常→空）。
     * 与 {@link #currentMerchantIdQuietly()} 对称，但面向"任意用户"（如审批记录的发起人）。
     */
    /** M007-R6-02：指定用户所属项目（organization_id），用于待复核清单的项目隔离；无则空，绝不抛错。 */
    public Optional<String> organizationIdOf(String userId) {
        if (userId == null || accessRepository == null) {
            return Optional.empty();
        }
        try {
            return accessRepository.findUserById(userId).map(AccessUser::organizationId).filter(id -> id != null && !id.isBlank());
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public Optional<String> merchantIdOf(String userId) {
        if (userId == null || accessRepository == null) {
            return Optional.empty();
        }
        try {
            return accessRepository.findUserById(userId)
                    .map(AccessUser::merchantId)
                    .filter(merchantId -> merchantId != null && !merchantId.isBlank());
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    /**
     * Returns the merchant scope of the current caller.
     * Platform admins get Optional.empty() (no filter, sees everything; D-032：红线④不约束平台管理员).
     * Merchant users get Optional.of(merchantId).
     * Other callers without a merchantId throw MERCHANT_SCOPE_REQUIRED.
     */
    public Optional<String> currentMerchantScope() {
        AccessActor actor = currentActor();
        if (isPlatformAdminBridge(actor)) {
            return Optional.empty();
        }
        String merchantId = scopeMerchantIdOf(actor);
        if (merchantId != null && !merchantId.isBlank()) {
            return Optional.of(merchantId);
        }
        throw ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "此操作必须在商户范围内进行");
    }

    /**
     * 第十八批 AB 段第 191 条 191-R1（BSM:1143「派驻仅由平台侧获授权人员登记，保留员工、目标商户及项目组织范围」「其他公司的人员派驻到某家酒吧工作期间……」；
     * BSM:931、BSM:1468 本人帮客人订位）：本人此刻恰好在一家酒吧的派驻期内时取该派驻酒吧，否则取账号商户号。
     * 只看起止日期、不看状态：结束派驻即置 ended，结束日期可选当天或以后（与提成归集同一判定）。
     * 第十八批裁决 46：派驻期按各派驻记录所属酒吧此刻的营业日判（结束日当天的夜班过零点后、到营业日结束前仍算派驻期内）。
     */
    private String scopeMerchantIdOf(AccessActor actor) {
        if (staffRepository != null) {
            List<String> dispatched = staffRepository.dispatchMerchantIdsAt(actor.id(), Instant.now());
            if (dispatched.size() == 1) {
                return dispatched.get(0);
            }
        }
        return actor.merchantId();
    }

    /**
     * Returns the current caller's merchant id if present, otherwise empty.
     * Unlike {@link #currentMerchantScope()}, this never throws: platform admins and callers without a merchant
     * yield {@code Optional.empty()}. Degrades to empty on any error.
     * 平台管理员即便被种子误绑 merchant_id，也按平台视角看全量（D-032）。
     */
    public Optional<String> currentMerchantIdQuietly() {
        try {
            AccessActor actor = currentActor();
            if (isPlatformAdminBridge(actor)) {
                return Optional.empty();
            }
            String merchantId = scopeMerchantIdOf(actor);
            if (merchantId != null && !merchantId.isBlank()) {
                return Optional.of(merchantId);
            }
            return Optional.empty();
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    /**
     * 平台合规口径守卫：企微会话/留痕/风控等平台级合规数据按 projectId 归属，不按 merchant 切片。
     * 此方法 fail-closed 拒绝任何 merchant-scoped 调用方（持 merchantId 的商户用户），
     * 把"治理层确保 wecom.* 不授商户角色"落到代码层防御——即使权限被错误下放给商户角色，
     * 商户用户也无法越权调阅他项目的合规会话。平台管理员与无 merchantId 的平台合规员放行。
     */
    public void requirePlatformComplianceScope() {
        AccessActor actor = currentActor();
        if (isPlatformAdminBridge(actor)) {
            return;
        }
        if (isOutsourcedVendor(actor)) {
            throw ApiException.forbidden(
                    "PLATFORM_COMPLIANCE_SCOPE_REQUIRED",
                    "外包供应商无权查看平台合规数据");
        }
        String merchantId = actor.merchantId();
        if (merchantId != null && !merchantId.isBlank()) {
            throw ApiException.forbidden(
                    "PLATFORM_COMPLIANCE_SCOPE_REQUIRED",
                    "商户账号无权查看平台合规数据");
        }
    }

    /**
     * 平台口径守卫：平台级聚合分析（无 merchant 维度的指标定义/风控规则/风险告警/员工评分/机器人/
     * AI 质检/客户意图等）不向商户切片，fail-closed 拒绝 merchant-scoped 调用方，
     * 防"权限被下放给商户角色即可拉取全平台分析数据"的越权读。平台管理员/无 merchantId 平台员放行。
     */
    public void requirePlatformScope() {
        AccessActor actor = currentActor();
        if (isPlatformAdminBridge(actor)) {
            return;
        }
        if (isOutsourcedVendor(actor)) {
            throw ApiException.forbidden(
                    "PLATFORM_SCOPE_REQUIRED",
                    "外包供应商无权查看平台级数据");
        }
        String merchantId = actor.merchantId();
        if (merchantId != null && !merchantId.isBlank()) {
            throw ApiException.forbidden(
                    "PLATFORM_SCOPE_REQUIRED",
                    "商户账号无权查看平台级经营分析");
        }
    }

    /**
     * 项目维度只读 scope：平台管理员返回空＝全量（D-032）；其余主体返回其项目 ID，没有项目归属则拒绝。
     */
    public Optional<String> currentProjectScopeQuietly() {
        AccessActor actor = currentActor();
        if (isPlatformAdminBridge(actor)) {
            return Optional.empty();
        }
        return Optional.of(requireProjectScope(actor));
    }

    private boolean isPlatformAdminBridge(AccessActor actor) {
        return "active".equals(actor.status())
                && "platform_admin".equals(normalize(actor.accountType()));
    }

    /** #14：客人自助主体（由客人 token 过滤器写入）。仅对 {@link #CUSTOMER_SELF_SERVICE_PERMISSIONS} 放行。 */
    private boolean isCustomerSelfServiceBridge(AccessActor actor) {
        return "active".equals(actor.status())
                && "customer".equals(normalize(actor.accountType()));
    }

    private boolean isActive(AccessRepository.PermissionEffect effect, Instant checkedAt) {
        return !effect.consumed() && inForce(effect, checkedAt);
    }

    /**
     * M723（技术推导，一次操作只算一次使用）：动作核验可用的来源＝有效来源，加上「已消耗、仍在生效时段内、且正是本请求里
     * 这名操作人刚消耗的」单次来源。没有请求上下文时与 {@link #isActive} 相同。
     */
    private boolean usable(AccessRepository.PermissionEffect effect, Instant checkedAt, AccessActor actor,
                           IdempotentReplayContext request) {
        return isActive(effect, checkedAt)
                || (request != null && effect.consumed() && effect.singleUse()
                        && effect.effect() == AccessRepository.Effect.GRANT && inForce(effect, checkedAt)
                        && request.consumedInThisRequest(actor.id(), sourceKey(effect)));
    }

    /** 在生效时段内（不看是否已消耗）。 */
    private boolean inForce(AccessRepository.PermissionEffect effect, Instant checkedAt) {
        if (effect.validFrom() != null && checkedAt.isBefore(effect.validFrom())) {
            return false;
        }
        return effect.validUntil() == null || checkedAt.isBefore(effect.validUntil());
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private void deny(String permissionCode) {
        throw ApiException.forbidden("PERMISSION_DENIED", "没有操作权限：" + permissionCode);
    }
}
