package com.complex.platform.gate;

import com.complex.platform.access.AuthorizationService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * R5 T2/T3 动作前置闸口链切面。在带 {@link GuardedAction} 的方法执行前，依次过三道闸：
 * <ol>
 *   <li>终端开关闸（capability）</li>
 *   <li>合同准入闸（admissionAction + objectType + 解析的 objectId）</li>
 *   <li>权限/职级闸（permission）</li>
 * </ol>
 * 任一闸不过即拒绝并留痕；空注解项跳过。@Order(10) 使其早于业务事务。
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

    @Before("@annotation(guarded)")
    public void gate(JoinPoint joinPoint, GuardedAction guarded) {
        if (!guarded.capability().isEmpty()) {
            terminalSwitchGuard.check(guarded.capability());
        }
        if (!guarded.admissionAction().isEmpty()) {
            admissionGuard.check(guarded.admissionAction(), guarded.objectType(), resolveObjectId(joinPoint, guarded));
        }
        if (!guarded.permission().isEmpty()) {
            authorizationService.requirePermission(guarded.permission());
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
