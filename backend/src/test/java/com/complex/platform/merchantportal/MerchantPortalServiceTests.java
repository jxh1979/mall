package com.complex.platform.merchantportal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.contract.ContractRecord;
import com.complex.platform.contract.ContractRepository;
import com.complex.platform.contract.ContractSettlementRecord;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.merchantportal.dto.MerchantSettlementDisputeRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 商户专区一期(3.18 方案A)商户自助层:我的合同/我的对账只读 + 结算异议留言。
 * 铁律:只见本商户(currentMerchantScope 硬闸),跨商户异议 CROSS_MERCHANT_FORBIDDEN,
 * 已结清/已有争议的结算单不可再提异议。
 */
class MerchantPortalServiceTests {

    private final ContractRepository contractRepository = mock(ContractRepository.class);
    private final AuthorizationService authz = mock(AuthorizationService.class);
    private MerchantPortalService service;

    @BeforeEach
    void setUp() {
        service = new MerchantPortalService(contractRepository, authz, mock(AuditEventService.class));
    }

    private static ContractRecord contract(String id, String merchantId) {
        return new ContractRecord(id, merchantId, "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of(),
                null, null, 500000L, Map.of(), List.of(), null, null, List.of(),
                "effective", "project_1", null, null, null, Instant.now(), Instant.now());
    }

    private static ContractSettlementRecord settlement(String id, String contractId, String status) {
        return new ContractSettlementRecord(id, contractId, "move_out", status, List.of(),
                null, null, null, "project_1", Instant.now(), Instant.now());
    }

    @Test
    void merchantSeesOnlyOwnContractsWithSpaceIds() {
        when(authz.currentMerchantScope()).thenReturn(Optional.of("mch_a"));
        when(contractRepository.listContractsByMerchant("mch_a")).thenReturn(List.of(contract("c1", "mch_a")));
        when(contractRepository.listContractSpaceIds("c1")).thenReturn(List.of("space_1"));

        PageResponse<MerchantContractView> page = service.listMyContracts();

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().get(0).merchantId()).isEqualTo("mch_a");
        assertThat(page.items().get(0).spaceIds()).containsExactly("space_1");
        verify(authz).requirePermission("contract.view");
    }

    @Test
    void platformAdminSeesAllContracts() {
        when(authz.currentMerchantScope()).thenReturn(Optional.empty());
        when(contractRepository.listContracts()).thenReturn(List.of(contract("c1", "mch_a"), contract("c2", "mch_b")));
        when(contractRepository.listContractSpaceIds(any())).thenReturn(List.of());

        assertThat(service.listMyContracts().items()).hasSize(2);
    }

    @Test
    void merchantSeesOnlyOwnSettlements() {
        when(authz.currentMerchantScope()).thenReturn(Optional.of("mch_a"));
        when(contractRepository.listSettlementsByMerchant("mch_a"))
                .thenReturn(List.of(settlement("s1", "c1", "pending_settlement")));

        PageResponse<ContractSettlementRecord> page = service.listMySettlements();

        assertThat(page.items()).extracting(ContractSettlementRecord::id).containsExactly("s1");
        verify(authz).requirePermission("merchant.portal.settlement.view");
    }

    @Test
    void merchantCanDisputeOwnOpenSettlement() {
        when(authz.currentMerchantScope()).thenReturn(Optional.of("mch_a"));
        when(contractRepository.findSettlementById("s1"))
                .thenReturn(Optional.of(settlement("s1", "c1", "pending_settlement")));
        when(contractRepository.findContractById("c1")).thenReturn(Optional.of(contract("c1", "mch_a")));
        when(contractRepository.saveSettlement(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ContractSettlementRecord disputed = service.submitSettlementDispute("s1",
                new MerchantSettlementDisputeRequest("k1", null, "金额与合同约定不符"));

        assertThat(disputed.status()).isEqualTo("disputed");
        assertThat(disputed.disputeReason()).contains("金额与合同约定不符");
        ArgumentCaptor<ContractSettlementRecord> captor = ArgumentCaptor.forClass(ContractSettlementRecord.class);
        verify(contractRepository).saveSettlement(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo("disputed");
    }

    @Test
    void disputeOnForeignSettlementIsForbidden() {
        when(authz.currentMerchantScope()).thenReturn(Optional.of("mch_b"));
        when(contractRepository.findSettlementById("s1"))
                .thenReturn(Optional.of(settlement("s1", "c1", "pending_settlement")));
        when(contractRepository.findContractById("c1")).thenReturn(Optional.of(contract("c1", "mch_a")));

        assertThatThrownBy(() -> service.submitSettlementDispute("s1",
                new MerchantSettlementDisputeRequest("k1", null, "越权尝试")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
    }

    @Test
    void settledOrAlreadyDisputedSettlementCannotBeDisputed() {
        when(authz.currentMerchantScope()).thenReturn(Optional.of("mch_a"));
        when(contractRepository.findContractById("c1")).thenReturn(Optional.of(contract("c1", "mch_a")));
        for (String status : List.of("settled", "disputed")) {
            when(contractRepository.findSettlementById("s1"))
                    .thenReturn(Optional.of(settlement("s1", "c1", status)));
            assertThatThrownBy(() -> service.submitSettlementDispute("s1",
                    new MerchantSettlementDisputeRequest("k1", null, "重复异议")))
                    .isInstanceOfSatisfying(ApiException.class,
                            e -> assertThat(e.code()).isEqualTo("SETTLEMENT_NOT_DISPUTABLE"));
        }
    }

    @Test
    void disputeReasonIsRequired() {
        when(authz.currentMerchantScope()).thenReturn(Optional.of("mch_a"));
        when(contractRepository.findSettlementById("s1"))
                .thenReturn(Optional.of(settlement("s1", "c1", "pending_settlement")));
        when(contractRepository.findContractById("c1")).thenReturn(Optional.of(contract("c1", "mch_a")));

        assertThatThrownBy(() -> service.submitSettlementDispute("s1",
                new MerchantSettlementDisputeRequest("k1", null, "  ")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
    }
}
