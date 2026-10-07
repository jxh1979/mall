package com.complex.platform.gate;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.idempotency.IdempotentReplayContext;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * R5 T2/T3 动作前置闸口链切面。在带 {@link GuardedAction} 的方法执行前，依次过三道闸：
 * <ol>
 *   <li>终端开关闸（capability）</li>
 *   <li>合同准入闸（admissionAction + objectType + 解析的 objectId）</li>
 *   <li>权限/职级闸（permission）</li>
 * </ol>
 * 任一闸不过即拒绝并留痕；空注解项跳过。@Order(10) 使其早于业务事务。
 *
 * <p>XM-ONESHOT-RETURN B01（治理地图第 303 行「全局原则 · 同时操作以先成功为准」及操作员 2026-09-25 确认的追加句「凡只能用一次的动作……
 * 只有这次操作成功完成、返回结果确认后才算完成、才算用掉；没有返回确认的不算完成，不扣减」）：带准入闸的方法在无外层事务时，
 * 准入闸、权限闸与业务方法包进同一个事务（REQUIRED，业务 {@code @Transactional} 并入）。逐笔放行的消费（放行单标记已放行与
 * admission.release_executed 留痕）随业务一起提交或回滚：权限闸拒绝、业务校验失败或回滚时放行单不算用掉，可再放行一次。
 * 拦截复核单与拒绝留痕走独立事务（AdmissionGuard、AuditEventService.recordSuccessIndependently），不随回滚丢失。
 * 已在事务里（持单次授权的请求事务、调用方事务、测试事务）则直接并入，消费同样随外层事务提交或回滚。终端开关闸仍在事务外先判。
 *
 * <p>M724-F16（治理地图第 528 行「每次只能对当前这一笔放行、不得整类关闭校验」、第 303 行「每次只对当前这一笔生效并留痕」；操作员
 * 2026-10-07 裁决「批准只能被『被拦下的那次提交的同样内容』用掉，内容不同的另一笔照常拦截送审」）：过准入闸前算出本次受闸调用的
 * 请求内容摘要（全部实参去掉提交编号），只在过准入闸期间绑在本线程上；准入闸建复核单时记下它，消费放行单时按它匹配。
 */
@Aspect
@Component
@Order(10)
@ConditionalOnBean(AuthorizationService.class)
public class ActionGateAspect {

    private final TerminalSwitchGuard terminalSwitchGuard;
    private final AdmissionGuard admissionGuard;
    private final AuthorizationService authorizationService;

    public ActionGateAspect(
            TerminalSwitchGuard terminalSwitchGuard,
            AdmissionGuard admissionGuard,
            AuthorizationService authorizationService
    ) {
        this.terminalSwitchGuard = terminalSwitchGuard;
        this.admissionGuard = admissionGuard;
        this.authorizationService = authorizationService;
    }

    // XM-ONESHOT-RETURN B01：准入闸与业务共用的事务模板（REQUIRED）。经 setter 可选注入（同 AdmissionGuard），单测 new 构造不传则不加事务。
    private TransactionTemplate transactionTemplate;

    @Autowired(required = false)
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = transactionManager == null ? null : new TransactionTemplate(transactionManager);
    }

    @Around("@annotation(guarded)")
    public Object gate(ProceedingJoinPoint joinPoint, GuardedAction guarded) throws Throwable {
        if (!guarded.capability().isEmpty()) {
            terminalSwitchGuard.check(guarded.capability());
        }
        if (guarded.admissionAction().isEmpty() || transactionTemplate == null
                || TransactionSynchronizationManager.isActualTransactionActive()) {
            return admitAndProceed(joinPoint, guarded);
        }
        Throwable[] failure = new Throwable[1];
        Object result = transactionTemplate.execute(status -> {
            try {
                Object value = admitAndProceed(joinPoint, guarded);
                // M723-F04 的同一口径：本请求只是返回第一次的结果，却在闸口新用掉了放行单 → 闸口自己开的事务整笔回滚，放行单不算用掉
                // （已并入请求事务时由请求事务切面统一回滚，走不到这里）
                if (IdempotentReplayContext.replayDiscardsConsumption()) {
                    status.setRollbackOnly();
                }
                return value;
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
    }

    private Object admitAndProceed(ProceedingJoinPoint joinPoint, GuardedAction guarded) throws Throwable {
        if (!guarded.admissionAction().isEmpty()) {
            // M724-F16：摘要只包住过准入闸这一次调用，不包权限闸与业务方法；finally 还原成进入前的值
            String outer = SupervisorReviewService.bindGatedRequestContent(contentDigestOf(joinPoint.getArgs()));
            try {
                admissionGuard.check(guarded.admissionAction(), guarded.objectType(), resolveObjectId(joinPoint, guarded));
            } finally {
                SupervisorReviewService.bindGatedRequestContent(outer);
            }
        }
        if (!guarded.permission().isEmpty()) {
            authorizationService.requirePermission(guarded.permission());
        }
        return joinPoint.proceed();
    }

    // M724-F16：请求内容摘要专用。record 属性按名字排序、Map（含嵌套）按键排序、时间写 ISO 文本；不用全局 mapper，不受全局命名策略影响
    private static final ObjectMapper CONTENT_MAPPER = JsonMapper.builder()
            .findAndAddModules()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    /**
     * M724-F16（操作员 2026-10-07 裁决「批准只能被『被拦下的那次提交的同样内容』用掉」）：受闸方法全部实参（按声明顺序，路径里的字符串
     * 参数也算）去掉提交编号后的规范 JSON 的 SHA-256（十六进制 64 位）。提交编号只删各实参顶层的 idempotencyKey / idempotency_key
     * （请求 record 的组件名是前者，Map 请求体的键是后者）。不做任何归一；序列化失败直接抛，不降级成「无摘要」。
     */
    private static String contentDigestOf(Object[] args) {
        try {
            JsonNode content = CONTENT_MAPPER.valueToTree(args);
            for (JsonNode arg : content) {
                if (arg instanceof ObjectNode body) {
                    body.remove(List.of("idempotencyKey", "idempotency_key"));
                }
            }
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(CONTENT_MAPPER.writeValueAsBytes(content)));
        } catch (JsonProcessingException | IllegalArgumentException | NoSuchAlgorithmException unserializable) {
            throw new IllegalStateException("gated request content is not serializable", unserializable);
        }
    }

    /**
     * objectId 解析：4.10 起优先用 @GuardedAction.objectIdParam 显式指名的参数下标；
     * 未指定(-1)则沿用旧启发式（取第一个非空 String 参）；无 String 参 → null。
     */
    private String resolveObjectId(JoinPoint joinPoint, GuardedAction guarded) {
        Object[] args = joinPoint.getArgs();
        int idx = guarded.objectIdParam();
        if (idx >= 0) {
            return idx < args.length && args[idx] instanceof String str && !str.isBlank() ? str : null;
        }
        for (Object arg : args) {
            if (arg instanceof String str && !str.isBlank()) {
                return str;
            }
        }
        return null;
    }
}
