package com.complex.platform.gate;

import com.complex.platform.support.InMemoryAuditEventRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.terminal.TerminalCapabilitySwitchRecord;
import com.complex.platform.terminal.TerminalDeviceRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

/**
 * 证明 ActionGateAspect 通过 @GuardedAction 注解切面真实拦截被注解方法（AOP 生效）。
 * 用 Spring 的 AspectJProxyFactory 直接织入，无需起完整容器即可验证切面被调用。
 */
class ActionGateAspectTests {

    /** 被守护的目标 bean：方法带 @GuardedAction(capability=...)。 */
    static class GuardedTarget {
        boolean executed = false;

        @GuardedAction(capability = "finance.daily_close.lock")
        public String lock(String objectId) {
            executed = true;
            return "ok:" + objectId;
        }
    }

    private GuardedTarget proxy(ActionGateAspect aspect, GuardedTarget target) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        return factory.getProxy();
    }

    @Test
    void disabledSwitchBlocksGuardedMethodWithTerminalDisabled() {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(any(), eq("finance.daily_close.lock"), any()))
                .thenReturn(Optional.of(new TerminalCapabilitySwitchRecord(
                        "sw1", "*", "finance.daily_close.lock", "global", "global",
                        false, "off", Instant.now(), Instant.now())));
        InMemoryAuditEventRepository auditRepo = new InMemoryAuditEventRepository();
        AuditEventService auditService = new AuditEventService(auditRepo);
        TerminalSwitchGuard switchGuard = new TerminalSwitchGuard(repo, auditService);
        AdmissionGuard admissionGuard = mock(AdmissionGuard.class);
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        ActionGateAspect aspect = new ActionGateAspect(switchGuard, admissionGuard, authorizationService);

        GuardedTarget target = new GuardedTarget();
        GuardedTarget guarded = proxy(aspect, target);

        assertThatThrownBy(() -> guarded.lock("obj1"))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.code()).isEqualTo("TERMINAL_DISABLED"));
        // 被拦截：业务方法体未执行
        assertThat(target.executed).isFalse();
        // 拒绝留痕 terminal.access_rejected(reason=disabled)
        assertThat(auditRepo.events)
                .extracting(AuditEvent::eventName)
                .contains("terminal.access_rejected");
    }

    @Test
    void enabledOrAbsentSwitchAllowsGuardedMethodToRun() {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(any(), any(), any())).thenReturn(Optional.empty()); // 无行=默认开启
        AuditEventService auditService = new AuditEventService(new InMemoryAuditEventRepository());
        TerminalSwitchGuard switchGuard = new TerminalSwitchGuard(repo, auditService);
        ActionGateAspect aspect = new ActionGateAspect(switchGuard, mock(AdmissionGuard.class), mock(AuthorizationService.class));

        GuardedTarget target = new GuardedTarget();
        GuardedTarget guarded = proxy(aspect, target);

        assertThat(guarded.lock("obj2")).isEqualTo("ok:obj2");
        assertThat(target.executed).isTrue();
    }

    @Test
    void failClosedWhenSwitchLookupThrows() {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(any(), any(), any())).thenThrow(new RuntimeException("db down"));
        InMemoryAuditEventRepository auditRepo = new InMemoryAuditEventRepository();
        TerminalSwitchGuard switchGuard = new TerminalSwitchGuard(repo, new AuditEventService(auditRepo));
        ActionGateAspect aspect = new ActionGateAspect(switchGuard, mock(AdmissionGuard.class), mock(AuthorizationService.class));

        GuardedTarget guarded = proxy(aspect, new GuardedTarget());

        assertThatThrownBy(() -> guarded.lock("obj3"))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.code()).isEqualTo("TERMINAL_DISABLED"));
        assertThat(auditRepo.events)
                .filteredOn(e -> "terminal.access_rejected".equals(e.eventName()))
                .first()
                .satisfies(e -> assertThat(e.metadata()).containsEntry("reason", "switch_fetch_failed"));
    }

    /** 准入 block 闸：命中 block 策略时拦截并抛 ADMISSION_BLOCKED，第三道权限闸不再执行。 */
    @Test
    void admissionBlockInterceptsBeforePermissionGate() {
        AdmissionTarget target = new AdmissionTarget();
        AdmissionGuard admissionGuard = mock(AdmissionGuard.class);
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        org.mockito.Mockito.doThrow(new ApiException(
                        org.springframework.http.HttpStatus.CONFLICT, "ADMISSION_BLOCKED", "blocked"))
                .when(admissionGuard).check(eq("bar.bill_waiver"), eq("bar_bill"), any());
        ActionGateAspect aspect = new ActionGateAspect(
                mock(TerminalSwitchGuard.class), admissionGuard, authorizationService);

        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        AdmissionTarget guarded = factory.getProxy();

        assertThatThrownBy(() -> guarded.waive("bill1"))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.code()).isEqualTo("ADMISSION_BLOCKED"));
        verify(admissionGuard, times(1)).check(eq("bar.bill_waiver"), eq("bar_bill"), eq("bill1"));
        // 准入闸在权限闸之前拦截，权限闸不应被调用
        verify(authorizationService, never()).requirePermission(any());
        assertThat(target.executed).isFalse();
    }

    @Test
    void objectIdParamPicksDesignatedArgNotFirstString() {
        // 4.10：显式 objectIdParam=1 → 用第 2 个 String(billId) 作 objectId，而非首个 String(idempotencyKey)。
        ExplicitObjectIdTarget target = new ExplicitObjectIdTarget();
        AdmissionGuard admissionGuard = mock(AdmissionGuard.class);
        ActionGateAspect aspect = new ActionGateAspect(
                mock(TerminalSwitchGuard.class), admissionGuard, mock(AuthorizationService.class));
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        ExplicitObjectIdTarget guarded = factory.getProxy();

        guarded.act("idem-key-123", "bill-999");
        verify(admissionGuard, times(1)).check(eq("bar.bill_waiver"), eq("bar_bill"), eq("bill-999"));
    }

    static class ExplicitObjectIdTarget {
        @GuardedAction(admissionAction = "bar.bill_waiver", objectType = "bar_bill", objectIdParam = 1)
        public String act(String idempotencyKey, String billId) {
            return billId;
        }
    }

    static class AdmissionTarget {
        boolean executed = false;

        @GuardedAction(admissionAction = "bar.bill_waiver", objectType = "bar_bill", permission = "bar.bill.adjust")
        public String waive(String billId) {
            executed = true;
            return billId;
        }
    }
}
