package com.complex.platform.gate;

import com.complex.platform.support.InMemoryAuditEventRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.customer.LoyaltyService;
import com.complex.platform.customer.StoredValueService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.terminal.TerminalCapabilitySwitchRecord;
import com.complex.platform.terminal.TerminalDeviceRepository;
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

/**
 * P1 #17 验证：客户域资金/权益写动作（会员/储值/积分）真实挂上终端开关 capability 闸（修正此前 capability=""
 * 留空导致资金/权益动作未门控），且 {@link TerminalSwitchGuard} 拒绝留痕含 scope 维度并采用失败语义。
 */
class CustomerGuardedActionAndRejectionAuditTests {

    /** 客户域资金/权益动作：Service 类 + 方法名 + 期望补全的 capability 码。 */
    static Stream<Arguments> customerFundAndEntitlementActions() {
        return Stream.of(
                // 储值（资金：充值/消费/退卡/冻结）
                Arguments.of(StoredValueService.class, "topup", "customer.stored_value.topup"),
                Arguments.of(StoredValueService.class, "settleTopup", "customer.stored_value.topup"),
                Arguments.of(StoredValueService.class, "consume", "customer.stored_value.consume"),
                Arguments.of(StoredValueService.class, "refund", "customer.stored_value.refund"),
                Arguments.of(StoredValueService.class, "freeze", "customer.stored_value.freeze"),
                Arguments.of(StoredValueService.class, "unfreeze", "customer.stored_value.freeze"),
                // 积分/权益
                Arguments.of(LoyaltyService.class, "evaluateLevel", "customer.loyalty.evaluate"),
                Arguments.of(LoyaltyService.class, "freeze", "customer.points.freeze"),
                Arguments.of(LoyaltyService.class, "unfreeze", "customer.points.freeze")
                // savePointsTransferLimit 已随「积分转赠下线」移除（HTML：积分不支持会员间转赠）。
        );
    }

    /** 1) capability 已补全：每个资金/权益动作带 @GuardedAction 且 capability == 期望码（非空）。 */
    @ParameterizedTest(name = "{0}#{1} -> capability={2}")
    @MethodSource("customerFundAndEntitlementActions")
    void customerActionsCarryNonEmptyCapability(
            Class<?> serviceType, String methodName, String expectedCapability) {
        GuardedAction guarded = findGuardedAnnotation(serviceType, methodName);
        assertThat(guarded)
                .as("%s#%s 应带 @GuardedAction", serviceType.getSimpleName(), methodName)
                .isNotNull();
        assertThat(guarded.capability())
                .as("%s#%s capability 不得留空、须为期望码", serviceType.getSimpleName(), methodName)
                .isNotEmpty()
                .isEqualTo(expectedCapability);
    }

    /** 2) 闸口生效：客户域 capability 显式 disabled → 拒绝并写 terminal.access_rejected 留痕。 */
    @ParameterizedTest(name = "disabled[{2}] -> TERMINAL_DISABLED + audit")
    @MethodSource("customerFundAndEntitlementActions")
    void disabledSwitchRejectsCustomerCapability(
            Class<?> serviceType, String methodName, String capability) {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(any(), eq(capability), any()))
                .thenReturn(Optional.of(new TerminalCapabilitySwitchRecord(
                        "sw_" + capability, "*", capability, "global", "global",
                        false, "off", Instant.now(), Instant.now())));
        InMemoryAuditEventRepository auditRepo = new InMemoryAuditEventRepository();
        TerminalSwitchGuard guard = new TerminalSwitchGuard(repo, new AuditEventService(auditRepo));

        assertThatThrownBy(() -> guard.check(capability))
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
     * 3) 拒绝留痕含 scope 维度 + 失败语义：disabled 命中显式开关行时，payload 含
     * scope_type/scope_id/terminal_type/business_system/module_code/device + result=failure。
     */
    @Test
    void rejectionAuditCarriesScopeFieldsAndFailureResult() {
        TerminalDeviceRepository repo = mock(TerminalDeviceRepository.class);
        when(repo.resolveEffectiveSwitch(any(), eq("customer.stored_value.topup"), any()))
                .thenReturn(Optional.of(new TerminalCapabilitySwitchRecord(
                        "sw_sv", "*", "customer.stored_value.topup", "merchant", "m_001",
                        false, "off", Instant.now(), Instant.now())));
        InMemoryAuditEventRepository auditRepo = new InMemoryAuditEventRepository();
        TerminalSwitchGuard guard = new TerminalSwitchGuard(repo, new AuditEventService(auditRepo));

        assertThatThrownBy(() -> guard.check("customer.stored_value.topup"))
                .isInstanceOf(ApiException.class);

        AuditEvent rejected = auditRepo.events.stream()
                .filter(e -> "terminal.access_rejected".equals(e.eventName()))
                .findFirst()
                .orElseThrow();
        // 留痕维度齐全（PRD §6.3 拒绝铁律）：有值维度必在。
        // business_system/module_code/device 源自当前请求头，单测无请求上下文时为 null，
        // 按「跳过 null 值」口径不写入 payload，故此处不断言其存在（有命中行时 scope_type/scope_id 必在）。
        assertThat(rejected.metadata())
                .containsKeys("scope_type", "scope_id", "terminal_type", "capability", "reason");
        assertThat(rejected.metadata()).containsEntry("scope_type", "merchant");
        assertThat(rejected.metadata()).containsEntry("scope_id", "m_001");
        // 失败语义：拒绝事件显式标 result=failure（审计按失败过滤不再漏）
        assertThat(rejected.metadata()).containsEntry("result", "failure");
    }

    private GuardedAction findGuardedAnnotation(Class<?> serviceType, String methodName) {
        for (Method method : serviceType.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.isAnnotationPresent(GuardedAction.class)) {
                return method.getAnnotation(GuardedAction.class);
            }
        }
        return null;
    }
}
