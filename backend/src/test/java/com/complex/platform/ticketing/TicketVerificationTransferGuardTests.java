package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 批3 转赠安全:核销侧两道竞态闸的纯内存单测(无 DB)。
 * <ul>
 *   <li>AUD-018:atomicUseEntitlement 得手后复检 pending 转赠(新快照)——命中即抛错回滚,不落 success 记录。</li>
 *   <li>AUD-012:动态凭证消费为 CAS(仅 active→consumed)——0 行=已被转赠领取并发撤销 → CREDENTIAL_EXPIRED。</li>
 * </ul>
 */
class TicketVerificationTransferGuardTests {

    private static final String ENTITLEMENT_CODE = "TRF-GUARD-1";
    private static final String PARENT_ID = "tkt_parent_1";

    private IssuedEntitlementRecord slotEntitlement() {
        Instant now = Instant.now();
        return new IssuedEntitlementRecord(
                "issued_1", ENTITLEMENT_CODE, "tt_1", "order_1", "tpl_1", "ticket",
                "merchant_1", null, null, 1, 0, 100L, "active",
                null, null, now, now, 1, null, null, null, null, null, PARENT_ID, 1, null, null);
    }

    private TicketInstanceRecord activeParent() {
        Instant now = Instant.now();
        return new TicketInstanceRecord(
                PARENT_ID, "TNO_1", "order_1", "tt_1", 1, 1, 1,
                "cust_a", "cust_a", "active", null, null, now, now);
    }

    @Test
    void pendingTransferNoLongerConsultedByVerification() {
        // clients-R40（BSM 第 809 行「发起转赠后不冻结该权益」）：核销主链不再查在途转赠，转赠由消费成功同事务作废（atomicUseEntitlement）。
        TicketingRepository repository = mock(TicketingRepository.class);
        when(repository.terminalDeviceExists(any())).thenReturn(true);
        IssuedEntitlementRecord entitlement = slotEntitlement();
        when(repository.findIssuedEntitlementByCode(ENTITLEMENT_CODE)).thenReturn(Optional.of(entitlement));
        when(repository.findTicketInstanceById(PARENT_ID)).thenReturn(Optional.of(activeParent()));
        when(repository.existsPendingTransferForSlot(PARENT_ID, 1)).thenReturn(true);
        when(repository.atomicUseEntitlement(eq("issued_1"), any()))
                .thenReturn(Optional.of(entitlement.useOnce(Instant.now())));

        TicketVerificationService service = new TicketVerificationService(
                repository, mock(ApplicationEventPublisher.class), null, null, null, null);

        Throwable outcome = org.assertj.core.api.Assertions.catchThrowable(() -> service.createVerification(new VerificationRequest(
                "guard-race-1", "qr", ENTITLEMENT_CODE, "dev_1", "gate_1", Map.of())));
        if (outcome instanceof ApiException api) {
            org.assertj.core.api.Assertions.assertThat(api.code()).isNotEqualTo("TICKET_TRANSFER_PENDING");
        }
        verify(repository).atomicUseEntitlement(eq("issued_1"), any());
        verify(repository, never()).existsPendingTransferForSlot(any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void staleCredentialConsumeCasFailureRejectsVerification() {
        Instant now = Instant.now();
        TicketingRepository repository = mock(TicketingRepository.class);
        when(repository.terminalDeviceExists(any())).thenReturn(true);
        // 无父票权益:隔离转赠冻结守卫,只测凭证 CAS。
        IssuedEntitlementRecord entitlement = new IssuedEntitlementRecord(
                "issued_1", ENTITLEMENT_CODE, "tt_1", "order_1", "tpl_1", "ticket",
                "merchant_1", null, null, 1, 0, 100L, "active",
                null, null, now, now, 1, null, null, null, null, null, null, 0, null, null);
        TicketCredentialRecord credential = new TicketCredentialRecord(
                "credential_1", "issued_1", "order_1", "cred_tok1", "nonce1", "hash1", "dynamic_qr",
                now, now.plusSeconds(60), "active", 1, now, now, 60, now, null, null);
        when(repository.findTicketCredentialByToken("cred_tok1")).thenReturn(Optional.of(credential));
        when(repository.findIssuedEntitlementById("issued_1")).thenReturn(Optional.of(entitlement));
        when(repository.atomicUseEntitlement(eq("issued_1"), any()))
                .thenReturn(Optional.of(entitlement.useOnce(now)));
        when(repository.saveVerification(any())).thenAnswer(inv -> inv.getArgument(0));
        // CAS 0 行=凭证已被转赠领取并发撤销(active→revoked) → 本次核销必须被拒(事务回滚扣减与 success 记录)。
        when(repository.markTicketCredentialConsumed(eq("credential_1"), any())).thenReturn(0);

        TicketVerificationService service = new TicketVerificationService(
                repository, mock(ApplicationEventPublisher.class), null, null, null, null);

        assertThatThrownBy(() -> service.createVerification(new VerificationRequest(
                "guard-cas-1", "dynamic_qr", "cred_tok1", "dev_1", "gate_1", Map.of())))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo("CREDENTIAL_EXPIRED");
    }
}
