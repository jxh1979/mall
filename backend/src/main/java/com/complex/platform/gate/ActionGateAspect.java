package com.complex.platform.gate;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.idempotency.IdempotentReplayContext;
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
            admissionGuard.check(guarded.admissionAction(), guarded.objectType(), resolveObjectId(joinPoint, guarded));
        }
        if (!guarded.permission().isEmpty()) {
            authorizationService.requirePermission(guarded.permission());
        }
        return joinPoint.proceed();
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
