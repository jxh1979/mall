package com.complex.platform.ticketing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.terminal.MdmService;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 跨模块闭环②a：核销链路接入 MDM 高风险门控的单测。
 *
 * <p>核销主链携带操作设备标识（VerificationRequest.deviceId），故本链路【已强制门控】：
 * 真正消费权益前调用 {@link MdmService#requireManagedForHighRiskAction}(deviceId, "ticket.verify")。
 * 门控拒绝（开关开+未纳管 / blocked / lost）时核销被拒、不消费权益、不落 success 记录。
 */
class TicketVerificationMdmGateTests {

    private static final String ENTITLEMENT_CODE = "P0-GATE-1";
    private static final String DEVICE_ID = "dev_work_phone_1";

    private IssuedEntitlementRecord activeEntitlement() {
        Instant now = Instant.now();
        return new IssuedEntitlementRecord(
                "issued_1", ENTITLEMENT_CODE, "tt_1", "order_1", "tpl_1", "ticket",
                "merchant_1", "venue_1", "point_1", 2, 0, 9900L, "active",
                null, null, now, now, 1, null,
                null, null);
    }

    private VerificationRequest verifyRequest() {
        return new VerificationRequest(
                "verify-gate-1", "qr", ENTITLEMENT_CODE, DEVICE_ID, "gate_1",
                Map.of("scenario", "ticket_checkin"));
    }

    @Test
    void unmanagedDeviceVerificationIsBlockedByMdmGateAndDoesNotConsumeEntitlement() {
        TicketingRepository repository = mock(TicketingRepository.class);
        when(repository.terminalDeviceExists(any())).thenReturn(true);
        MdmService mdmService = mock(MdmService.class);
        when(repository.findIssuedEntitlementByCode(ENTITLEMENT_CODE))
                .thenReturn(Optional.of(activeEntitlement()));
        // 门控硬拒绝：未纳管设备执行高风险核销动作（开关开）。
        doThrow(ApiException.forbidden("DEVICE_NOT_MANAGED", "device is not managed by MDM: " + DEVICE_ID))
                .when(mdmService).requireManagedForHighRiskAction(eq(DEVICE_ID), eq("ticket.verify"));

        TicketVerificationService service = new TicketVerificationService(
                repository, mock(ApplicationEventPublisher.class), mdmService);

        assertThatThrownBy(() -> service.createVerification(verifyRequest()))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("DEVICE_NOT_MANAGED");

        // 被门控拦截：不消费权益（不调 atomicUseEntitlement），不落核销记录。
        verify(repository, never()).atomicUseEntitlement(any(), any());
        verify(repository, never()).saveVerification(any());
    }

    @Test
    void managedDevicePassesGateAndConsumesEntitlement() {
        TicketingRepository repository = mock(TicketingRepository.class);
        when(repository.terminalDeviceExists(any())).thenReturn(true);
        MdmService mdmService = mock(MdmService.class);
        IssuedEntitlementRecord entitlement = activeEntitlement();
        when(repository.findIssuedEntitlementByCode(ENTITLEMENT_CODE))
                .thenReturn(Optional.of(entitlement));
        // 门控放行（已纳管/开关关代偿）：requireManagedForHighRiskAction 不抛异常。
        when(repository.atomicUseEntitlement(eq("issued_1"), any()))
                .thenReturn(Optional.of(entitlement.useOnce(Instant.now())));
        when(repository.findEntitlementTemplateById(any())).thenReturn(Optional.empty());
        when(repository.saveVerification(any())).thenAnswer(inv -> inv.getArgument(0));

        TicketVerificationService service = new TicketVerificationService(
                repository, mock(ApplicationEventPublisher.class), mdmService);

        VerificationResponse response = service.createVerification(verifyRequest());

        assertThat(response.status()).isEqualTo("success");
        // 门控被调用一次（已强制接入），随后消费权益成功。
        verify(mdmService).requireManagedForHighRiskAction(eq(DEVICE_ID), eq("ticket.verify"));
        verify(repository).atomicUseEntitlement(eq("issued_1"), any());
        verify(repository).saveVerification(any());
    }

    @Test
    void legacyConstructorWithoutMdmServiceSkipsGate() {
        TicketingRepository repository = mock(TicketingRepository.class);
        when(repository.terminalDeviceExists(any())).thenReturn(true);
        IssuedEntitlementRecord entitlement = activeEntitlement();
        when(repository.findIssuedEntitlementByCode(ENTITLEMENT_CODE))
                .thenReturn(Optional.of(entitlement));
        when(repository.atomicUseEntitlement(eq("issued_1"), any()))
                .thenReturn(Optional.of(entitlement.useOnce(Instant.now())));
        when(repository.findEntitlementTemplateById(any())).thenReturn(Optional.empty());
        when(repository.saveVerification(any())).thenAnswer(inv -> inv.getArgument(0));

        // 兼容构造（mdmService=null）：门控未接，核销按原行为通过。
        TicketVerificationService service = new TicketVerificationService(
                repository, mock(ApplicationEventPublisher.class));

        VerificationResponse response = service.createVerification(verifyRequest());

        assertThat(response.status()).isEqualTo("success");
    }

    @Test
    void unregisteredDeviceRejectedCleanlyBeforeAnyInsert() {
        // 健壮性修复：device_id 不在 terminal_devices 时，核销前置校验抛干净 DEVICE_NOT_REGISTERED(400)，
        // 而非等到 saveVerification 违反 FK 使事务中止后返回脏 500。不消费权益、不落核销记录。
        TicketingRepository repository = mock(TicketingRepository.class);
        MdmService mdmService = mock(MdmService.class);
        when(repository.findIssuedEntitlementByCode(ENTITLEMENT_CODE))
                .thenReturn(Optional.of(activeEntitlement()));
        when(repository.terminalDeviceExists(any())).thenReturn(false);

        TicketVerificationService service = new TicketVerificationService(
                repository, mock(ApplicationEventPublisher.class), mdmService);

        assertThatThrownBy(() -> service.createVerification(verifyRequest()))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("DEVICE_NOT_REGISTERED");

        verify(repository, never()).atomicUseEntitlement(any(), any());
        verify(repository, never()).saveVerification(any());
    }
}
