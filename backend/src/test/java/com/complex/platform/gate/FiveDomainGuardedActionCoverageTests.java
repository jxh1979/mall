package com.complex.platform.gate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.amusement.AmusementSafetyService;
import com.complex.platform.amusement.AmusementService;
import com.complex.platform.bar.BarService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.inventory.InventoryService;
import com.complex.platform.terminal.TerminalCapabilitySwitchRecord;
import com.complex.platform.terminal.TerminalDeviceRepository;
import com.complex.platform.ticketing.TicketingService;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

/**
 * R5 T10/T11 全景埋点覆盖测试：证明五域（finance/bar/ticketing/amusement/inventory）的写动作
 * 真实带上了 {@link GuardedAction} 能力闸注解（capability 非空、为期望码），且当对应 capability 的
 * 终端开关被显式置为 disabled 时，闸口（{@link TerminalSwitchGuard}，即 {@link ActionGateAspect}
 * 在能力闸阶段调用的同一实现）会拒绝并写 terminal.access_rejected 留痕——证明埋点是真闸口、非死注解。
 *
 * <p>默认开放（无开关行）由全量回归保证不破坏；此处只对每域代表写动作做"关→拒+留痕"的硬验证。
 */
class FiveDomainGuardedActionCoverageTests {

    /** 每域取代表写动作：Service 类 + 方法名 + 期望 capability 码。 */
    static Stream<Arguments> representativeWriteActions() {
        return Stream.of(
                // finance
                Arguments.of(FinanceService.class, "createDailyClose", "finance.daily_close.lock"),
                Arguments.of(FinanceService.class, "createRefund", "finance.refund.create"),
                // bar
                Arguments.of(BarService.class, "createOrder", "bar.order.create"),
                Arguments.of(BarService.class, "applyBillAdjustment", "bar.bill.adjust"),
                // ticketing
                Arguments.of(TicketingService.class, "createVerification", "verification.execute"),
                Arguments.of(TicketingService.class, "reverseVerification", "ticket.verify.reverse"),
                // amusement
                Arguments.of(AmusementService.class, "createVerification", "amusement.verification.execute"),
                Arguments.of(AmusementSafetyService.class, "closeIncident", "amusement.incident.close"),
                // inventory
                Arguments.of(InventoryService.class, "createStockMovement", "inventory.stock.move"),
                Arguments.of(InventoryService.class, "createStockAdjustment", "inventory.stock.adjust")
        );
    }

    /** 1) 注解真实存在：每域代表写方法带 @GuardedAction 且 capability == 期望码。 */
    @ParameterizedTest(name = "{0}#{1} -> capability={2}")
    @MethodSource("representativeWriteActions")
    void representativeWriteActionsCarryExpectedCapability(
            Class<?> serviceType, String methodName, String expectedCapability) {
        GuardedAction guarded = findGuardedAnnotation(serviceType, methodName);
        assertThat(guarded)
                .as("%s#%s 应带 @GuardedAction", serviceType.getSimpleName(), methodName)
                .isNotNull();
        assertThat(guarded.capability())
                .as("%s#%s capability 码", serviceType.getSimpleName(), methodName)
                .isEqualTo(expectedCapability);
    }

    /**
     * 2) 闸口真实生效：对每域代表 capability，seed 一条 disabled 开关，调用能力闸（切面在 T2 阶段
     * 调用的同一 {@link TerminalSwitchGuard#check}）→ 抛 TERMINAL_DISABLED + 写
     * terminal.access_rejected(reason=disabled, capability=该码) 留痕。
     */
    @ParameterizedTest(name = "disabled[{2}] -> TERMINAL_DISABLED + audit")
    @MethodSource("representativeWriteActions")
    void disabledSwitchRejectsAndAuditsForEachCapability(
            Class<?> serviceType, String methodName, String capability) {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(any(), eq(capability), any()))
                .thenReturn(Optional.of(new TerminalCapabilitySwitchRecord(
                        "sw_" + capability, "*", capability, "global", "global",
                        false, "off", Instant.now(), Instant.now())));
        InMemoryAuditEventRepository auditRepo = new InMemoryAuditEventRepository();
        TerminalSwitchGuard switchGuard = new TerminalSwitchGuard(repo, new AuditEventService(auditRepo));

        assertThatThrownBy(() -> switchGuard.check(capability))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.code()).isEqualTo("TERMINAL_DISABLED"));
        assertThat(auditRepo.events)
                .filteredOn(e -> "terminal.access_rejected".equals(e.eventName()))
                .anySatisfy(e -> {
                    assertThat(e.metadata()).containsEntry("reason", "disabled");
                    assertThat(e.metadata()).containsEntry("capability", capability);
                });
    }

    /**
     * 3) 切面真实把 @GuardedAction.capability 传给能力闸：用带注解的目标 bean 经 AspectJ 织入，
     * disabled 开关命中注解上的 capability → 业务方法体不执行、抛 TERMINAL_DISABLED。
     * （证明 capability 埋点经 ActionGateAspect 真正驱动闸口，而非仅静态存在。）
     */
    @Test
    void aspectDrivesCapabilityGateFromAnnotation() {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(any(), eq("inventory.stock.move"), any()))
                .thenReturn(Optional.of(new TerminalCapabilitySwitchRecord(
                        "sw_inv", "*", "inventory.stock.move", "global", "global",
                        false, "off", Instant.now(), Instant.now())));
        InMemoryAuditEventRepository auditRepo = new InMemoryAuditEventRepository();
        TerminalSwitchGuard switchGuard = new TerminalSwitchGuard(repo, new AuditEventService(auditRepo));
        ActionGateAspect aspect = new ActionGateAspect(
                switchGuard, mock(AdmissionGuard.class), mock(AuthorizationService.class));

        StockMoveTarget target = new StockMoveTarget();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        StockMoveTarget guarded = factory.getProxy();

        assertThatThrownBy(guarded::move)
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.code()).isEqualTo("TERMINAL_DISABLED"));
        assertThat(target.executed).isFalse();
        assertThat(auditRepo.events)
                .extracting(AuditEvent::eventName)
                .contains("terminal.access_rejected");
    }

    static class StockMoveTarget {
        boolean executed = false;

        @GuardedAction(capability = "inventory.stock.move")
        public void move() {
            executed = true;
        }
    }

    private GuardedAction findGuardedAnnotation(Class<?> serviceType, String methodName) {
        for (Method method : serviceType.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.isAnnotationPresent(GuardedAction.class)) {
                return method.getAnnotation(GuardedAction.class);
            }
        }
        return null;
    }

    private static final class InMemoryAuditEventRepository implements AuditEventRepository {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public AuditEvent save(AuditEvent event) {
            events.add(event);
            return event;
        }
    }
}
