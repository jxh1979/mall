package com.complex.platform.access;

// 文件版本：V0.06

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.idempotency.IdempotentReplayContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * XM-M002-J（M002-F18 / CODEX-M002-6-02；治理地图「权限规则」「临时授权可设有效期或单次使用；权限的授予、收回、到期、单次消耗、失效全部留痕」）：
 * 控制器先核权限、再校验入参和调业务，且控制器外没有事务。持有未用掉单次授权的主体发起请求时，本切面把整个控制器方法包进一个事务，
 * {@link AuthorizationService#requirePermissions} 的单次消耗并入其中：入参校验失败、业务抛错、业务事务回滚时消耗与消耗日志一起回滚，
 * 成功才提交且只消耗一次；同一单次来源的并发请求在消耗行上串行，最多一笔成功。失败留痕（AuditEventService.recordFailure）是独立事务，不受影响。
 * 闸口链「先写入、再拒绝」的产物（准入拦截复核单及 admission.blocked、admission.warned / eval_failed_closed / space_unresolved_closed、
 * terminal.access_rejected、device.policy_violation）同样走独立事务（AdmissionGuard 的 REQUIRES_NEW 模板、AuditEventService.recordSuccessIndependently），不随本事务回滚。
 *
 * <p>不持有单次授权的主体（绝大多数请求）没有可消耗的来源，照旧运行、不加事务；已在事务里（例如测试事务）则直接并入。
 * 控制器方法抛出任何异常（含受检异常）都整笔回滚后原样抛出。
 *
 * <p>V0.02（操作员 2026-09-25 答复：单次授权只在业务成功并返回结果时才算用掉，同一请求编号重放应返回第一次的结果）：
 * 每次控制器调用都放入 {@link IdempotentReplayContext}——本次请求的幂等键与请求摘要（方法、路径与查询串的 SHA-256），
 * 单次消耗据此记到这次请求名下，重放据此认出（判定见 {@link AuthorizationService} 的 SingleUseReplay）。重放不消耗、不执行，
 * 不需要请求事务。
 *
 * <p>M723（操作员 2026-10-02：「只有真正执行的写操作（分配、审批等）才扣单次授权，打开页面、看列表不扣。」）：
 * GET / HEAD 请求是查看，上下文记为只读，权限核验照常但不消耗单次授权；没有要随业务回滚的消耗，所以不查持有、不包请求事务。
 * 处理方法标了 {@link ConsumesSingleUseGrant} 的查看请求（导出、敏感明文查看；操作员 2026-10-03：「算用掉，导一次扣一次」
 * 「算用掉，看一次扣一次」）对所列权限码照旧消耗，与写请求一样查持有、包请求事务。
 * 处理方法标了 {@link DoesNotConsumeSingleUseGrant} 的非 GET 请求（以提交方式实现的预览、查询；操作员 2026-10-03：「预览、查询不扣
 * 一次性授权，只有真正核销、真正执行的那一次才扣。」；2026-10-04：价格仿真「不算用掉」）与查看请求同样处理：只核验、不消耗、不包请求事务。
 *
 * <p>M723-F04（操作员 2026-09-25 确认句「同一次操作重复提交时，直接返回第一次的结果并提示已完成，不再重复执行、也不再扣减」；
 * 2026-10-03「同一项目、同一商户的员工用同一个提交编号，只算一次、返回第一次的结果」）：请求事务里处理方法正常返回后，
 * 若幂等层只是返回了第一次的结果、动作没有执行（{@link IdempotentReplayContext#replayed}），而本请求在权限核验处新用掉了单次来源
 * （{@link IdempotentReplayContext#consumedBy} 非空）——同项目同商户的同事用自己的单次来源重复提交、本人凭另一份单次来源重复提交而
 * 权限核验没有认出重放——就把请求事务标成只回滚：新用掉的来源与消耗日志一起回滚（不算用掉），结果照常返回。
 * 本人重放在权限核验处认出的（SingleUseReplay）本来就不消耗，不走这一支。
 * 走这一支的请求，响应里凭这次消耗才给的明文联系电话由各控制器按同一判断（{@link IdempotentReplayContext#replayDiscardsConsumption}）
 * 改回掩码：消耗不算，明文也不给（操作员 2026-10-03：敏感明文「算用掉，看一次扣一次」）。
 *
 * <p>M723-F08（第 656 行「操作被拒绝或失败的不算用掉」；操作员 2026-10-03「只有真正核销、真正执行的那一次才扣」）：在线核销以正常返回
 * 报告失败（过刷新窗口、签名失配，响应形状不变；终端离线同步不标记）时服务层把本请求记为业务失败（{@link IdempotentReplayContext#businessFailed}）；
 * 处理方法返回后，若不是上面的重放回滚、而本请求在权限核验处新用掉了单次来源，就在同一请求事务里把这次消耗撤回
 * （{@link AuthorizationService#releaseRequestConsumption}：来源改回未消耗、追加撤回留痕），结果照常返回、事务正常提交——
 * 失败核销记录与幂等记录保留，同一请求编号重复提交由幂等层返回第一次的失败结果。
 *
 * <p>顺序：排在闸口切面（ActionGateAspect，@Order(10)）之外，闸口里的权限核验才落在本切面的请求上下文与请求事务之内。
 *
 * <p>M723-F09（第 656 行「操作被拒绝或失败的不算用掉」）：不包请求事务放行时（进入时不持有未消耗单次来源，且当时没有外层事务）
 * 在请求上下文里记下「无请求事务」（{@link IdempotentReplayContext#markWithoutRequestTransaction}）；权限核验据此不消耗判定期间才被
 * 授出的单次来源——不论核验时有没有业务层自己的事务（那个事务的提交不受上面的重放回滚与失败撤回约束）。
 *
 * <p>ponytail: 每个已登录写请求多一次存在性查询；判定与消耗之间新授出的单次授权由 AuthorizationService 按不具备拒绝
 * （M723-F09），用户重试即可，需要时再把判定改成请求内缓存。
 */
@Aspect
@Component
@Order(5)
@ConditionalOnBean(AuthorizationService.class)
public class SingleUseGrantRequestTransactionAspect {

    private final AuthorizationService authorizationService;
    private final TransactionTemplate transactionTemplate;

    public SingleUseGrantRequestTransactionAspect(
            AuthorizationService authorizationService,
            PlatformTransactionManager transactionManager
    ) {
        this.authorizationService = authorizationService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // 只套本项目的控制器：第三方库（如 springdoc 文档接口）的控制器不需要、也不宜被代理。
    @Around("within(com.complex.platform..*) && @within(org.springframework.web.bind.annotation.RestController)")
    public Object inRequestTransaction(ProceedingJoinPoint handler) throws Throwable {
        HttpServletRequest request = currentRequest();
        Method handlerMethod = ((MethodSignature) handler.getSignature()).getMethod();
        // 查看（GET / HEAD），或以提交方式实现的预览、查询（DoesNotConsumeSingleUseGrant）：不消耗单次授权
        boolean readOnly = request != null && ("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod())
                || handlerMethod.isAnnotationPresent(DoesNotConsumeSingleUseGrant.class));
        ConsumesSingleUseGrant stillConsumed = !readOnly ? null : handlerMethod.getAnnotation(ConsumesSingleUseGrant.class);
        Set<String> requestKeys = idempotencyKeysOf(handler.getArgs(), request);
        IdempotentReplayContext previous = IdempotentReplayContext.enter(
                requestKeys != null && requestKeys.size() == 1 ? requestKeys.iterator().next() : null,
                requestDigestOf(request), readOnly,
                stillConsumed == null ? Set.of() : Set.copyOf(java.util.Arrays.asList(stillConsumed.value())));
        try {
            if ((readOnly && stillConsumed == null) || TransactionSynchronizationManager.isActualTransactionActive()
                    || !authorizationService.currentActorHoldsUnconsumedSingleUseGrant()) {
                if (!TransactionSynchronizationManager.isActualTransactionActive()) {
                    // M723-F09：本请求没有请求事务（进入时不持有未消耗单次来源，或本请求不消耗）——记下，权限核验据此不消耗
                    // 判定期间才被授出的单次来源，即使核验发生在业务层自己的事务里
                    IdempotentReplayContext.markWithoutRequestTransaction();
                }
                return handler.proceed();
            }
            // M723-F15（治理地图第 656 行「只有这次操作成功完成、返回结果后才算用掉」）：响应转换发生在本切面返回之后，那时请求事务
            // 已经提交。统一信封只能以 JSON 返回——请求头 Accept 不接受 JSON（或写错）时响应必然发不出去，所以在开请求事务之前
            // 就拒绝：置 406、不带响应体直接返回，不执行业务、不消耗。（本切面抛不了受检的 HttpMediaTypeNotAcceptableException——
            // 处理方法没有声明它，代理会包成 UndeclaredThrowableException；带统一信封的错误响应也同样发不出去。）
            if (!envelopeAcceptable(request, handlerMethod)) {
                currentResponse().setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
                return null;
            }
            // M723-F16（治理地图第 656 行「同一次操作重复提交时，直接返回第一次的结果并提示已完成，不再重复执行、也不再扣减」）：
            // 提交编号取到多个不同的值或读不出来时，本切面定不出这次请求的编号，业务层却可能照其中一个认出重复提交、返回第一次的
            // 结果——那样本请求新用掉的单次来源不会回滚。所以在开请求事务之前就拒绝（400）：不执行业务、不消耗。
            if (requestKeys == null || requestKeys.size() > 1) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请求里带了多个不同或无法识别的提交编号，请只带一个提交编号后重试");
            }
            Throwable[] failure = new Throwable[1];
            Object result = transactionTemplate.execute(status -> {
                try {
                    Object returned = handler.proceed();
                    IdempotentReplayContext context = IdempotentReplayContext.current();
                    if (IdempotentReplayContext.replayDiscardsConsumption()) {
                        // M723-F04：只是返回了第一次的结果、动作没有执行，本请求新用掉的单次来源不算用掉
                        status.setRollbackOnly();
                    } else if (context != null && context.businessFailed() && context.consumedBy() != null) {
                        // M723-F08：业务以正常返回报告失败，本请求新用掉的单次来源不算用掉——同一请求事务里撤回消耗，
                        // 失败留痕与幂等记录照常提交
                        authorizationService.releaseRequestConsumption();
                    }
                    return returned;
                } catch (Throwable throwable) {
                    failure[0] = throwable;
                    status.setRollbackOnly();
                    return null;
                }
            });
            if (failure[0] != null) {
                throw failure[0];
            }
            return result;
        } finally {
            IdempotentReplayContext.restore(previous);
        }
    }

    /**
     * 本次请求的幂等键：请求记录的 {@code idempotencyKey} 组件（请求体记录统一这样写）、Map 请求体的 {@code idempotency_key} 键
     * （M723-F17：终端设备签名公钥登记、开放平台回调这样取）、{@code Idempotency-Key} /
     * {@code X-Idempotency-Key} 请求头、查询串里的 {@code idempotency_key}。返回取到的全部不同取值（各处写同一个值只算一个）；
     * 读取失败返回 null。恰好一个值才是本次请求的幂等键；一个都没有时消耗不记请求信息、重放照旧拒绝；
     * 多个不同的值或读取失败，要开请求事务的请求直接拒绝（M723-F16）。
     */
    static Set<String> idempotencyKeysOf(Object[] args, HttpServletRequest request) {
        Set<String> keys = new HashSet<>();
        for (Object arg : args) {
            if (arg instanceof java.util.Map<?, ?> body && body.get("idempotency_key") instanceof String key) {
                addKey(keys, key);
            }
            if (arg == null || !arg.getClass().isRecord()) {
                continue;
            }
            for (RecordComponent component : arg.getClass().getRecordComponents()) {
                if ("idempotencyKey".equals(component.getName()) && component.getType() == String.class) {
                    try {
                        Method accessor = component.getAccessor();
                        accessor.setAccessible(true);
                        addKey(keys, (String) accessor.invoke(arg));
                    } catch (ReflectiveOperationException | RuntimeException unreadable) {
                        return null;
                    }
                }
            }
        }
        if (request != null) {
            addKey(keys, request.getHeader("Idempotency-Key"));
            addKey(keys, request.getHeader("X-Idempotency-Key"));
            // 只读查询串，不调 getParameter：表单请求体可能要留给控制器自己读原文（如回调验签）
            String query = request.getQueryString();
            for (String pair : query == null ? new String[0] : query.split("&")) {
                if (pair.startsWith("idempotency_key=")) {
                    try {
                        addKey(keys, URLDecoder.decode(pair.substring("idempotency_key=".length()), StandardCharsets.UTF_8));
                    } catch (IllegalArgumentException malformed) {
                        return null;
                    }
                }
            }
        }
        return keys;
    }

    private static void addKey(Set<String> keys, String key) {
        if (key != null && !key.isBlank()) {
            keys.add(key);
        }
    }

    /** 请求摘要：方法、路径与查询串的 SHA-256（十六进制 64 位），同一幂等键换接口、换对象、换查询条件都对不上。 */
    static String requestDigestOf(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String query = request.getQueryString();
        String target = request.getMethod() + " " + request.getRequestURI() + (query == null ? "" : "?" + query);
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(target.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 unavailable", unavailable);
        }
    }

    // 统一信封 ApiResponse 由 JSON 转换器写出；判断与响应转换同口径：解析请求头 Accept，没有该头即都接受。
    private static final List<MediaType> ENVELOPE_MEDIA_TYPES =
            List.of(MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));

    private static boolean envelopeAcceptable(HttpServletRequest request, Method handlerMethod) {
        if (request == null || !ApiResponse.class.isAssignableFrom(handlerMethod.getReturnType())) {
            return true;
        }
        List<MediaType> acceptable;
        try {
            acceptable = MediaType.parseMediaTypes(java.util.Collections.list(request.getHeaders(HttpHeaders.ACCEPT)));
        } catch (InvalidMediaTypeException exception) {
            return false;
        }
        return acceptable.isEmpty() || acceptable.stream().anyMatch(
                type -> ENVELOPE_MEDIA_TYPES.stream().anyMatch(type::isCompatibleWith));
    }

    private static HttpServletResponse currentResponse() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
    }

    private static HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest() : null;
    }
}
