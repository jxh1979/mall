package com.complex.platform.merchantportal;
// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.FieldLabels;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.contract.ContractRecord;
import com.complex.platform.contract.ContractRepository;
import com.complex.platform.contract.ContractSettlementRecord;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.merchantportal.dto.MerchantSettlementDisputeRequest;
import com.complex.platform.support.InMemoryIdempotencyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 铁律:只见本商户(currentMerchantScope 硬闸;无商户归属一律 MERCHANT_SCOPE_REQUIRED,不存在平台全量口径),
 * 跨商户异议 CROSS_MERCHANT_FORBIDDEN,已结清/已挂起争议的结算单不可再提异议;留言不覆盖平台裁决痕迹、
 * 受列宽约束、同幂等键只认一次;成功/失败均落审计(M020-001…006)。
 */
class MerchantPortalServiceTests {

    private static final ApiException SCOPE_REQUIRED =
            ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "此操作必须在商户范围内进行");

    private final ContractRepository contractRepository = mock(ContractRepository.class);
    private final AuthorizationService authz = mock(AuthorizationService.class);
    private final AuditEventService audit = mock(AuditEventService.class);
    private MerchantPortalService service;

    @BeforeEach
    void setUp() {
        service = new MerchantPortalService(contractRepository, authz, audit,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()));
    }

    private static ContractRecord contract(String id, String merchantId) {
        return new ContractRecord(id, merchantId, "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of(),
                null, null, 500000L, Map.of(), List.of(), null, null, List.of(),
                "effective", "project_1", null, null, null, Instant.now(), Instant.now());
    }

    private static ContractSettlementRecord settlement(String id, String contractId, String status) {
        return settlement(id, contractId, status, null, null, null);
    }

    private static ContractSettlementRecord settlement(String id, String contractId, String status,
            String disputeReason, String resolvedBy, Instant resolvedAt) {
        return new ContractSettlementRecord(id, contractId, "move_out", status, List.of(),
                disputeReason, resolvedBy, resolvedAt, "project_1", Instant.now(), Instant.now());
    }

    private void stubOwnOpenSettlement() {
        when(authz.currentMerchantScope()).thenReturn(Optional.of("mch_a"));
        when(contractRepository.findSettlementById("s1"))
                .thenReturn(Optional.of(settlement("s1", "c1", "pending_settlement")));
        when(contractRepository.findContractById("c1")).thenReturn(Optional.of(contract("c1", "mch_a")));
        when(contractRepository.saveSettlement(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void merchantSeesOnlyOwnContractsWithSpaceIds() {
        when(authz.currentMerchantScope()).thenReturn(Optional.of("mch_a"));
        when(contractRepository.listContractsByMerchant("mch_a")).thenReturn(List.of(contract("c1", "mch_a")));
        when(contractRepository.listContractSpaceIds("c1")).thenReturn(List.of("space_1"));

        PageResponse<MerchantContractView> page = service.listMyContracts(1, 500);

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().get(0).merchantId()).isEqualTo("mch_a");
        assertThat(page.items().get(0).spaceIds()).containsExactly("space_1");
        assertThat(page.total()).isEqualTo(1L);
        verify(authz).requirePermission("contract.view");
        verify(contractRepository, never()).listContractsPaged(anyInt(), anyInt());
        verify(audit).recordSuccess(eq("merchant_portal.contract.viewed"), eq("contract.view"),
                eq("merchant_portal_contract"), eq("mch_a"), isNull(), isNull(), eq(Map.of("count", 1)));
    }

    // M020-001/005:无商户归属的账号(含平台管理员)按真实现 fail-closed,不存在平台全量口径,并落失败审计。
    @Test
    void callerWithoutMerchantScopeIsRejectedAndAudited() {
        when(authz.currentMerchantScope()).thenThrow(SCOPE_REQUIRED);

        assertThatThrownBy(() -> service.listMyContracts(1, 500))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("MERCHANT_SCOPE_REQUIRED"));
        assertThatThrownBy(() -> service.listMySettlements(1, 500))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("MERCHANT_SCOPE_REQUIRED"));

        verify(contractRepository, never()).listContractsPaged(anyInt(), anyInt());
        verify(contractRepository, never()).listAllSettlementsPaged(anyInt(), anyInt());
        verify(contractRepository, never()).listContractsByMerchant(any());
        verify(contractRepository, never()).listSettlementsByMerchant(any());
        verify(audit, never()).recordSuccess(any(), any(), any(), any(), any(), any(), any());
        verify(audit).recordFailure("merchant_portal.contract.view_failed", "contract.view",
                "merchant_portal_contract", null, null, null, "MERCHANT_SCOPE_REQUIRED");
        verify(audit).recordFailure("merchant_portal.settlement.view_failed", "merchant.portal.settlement.view",
                "merchant_portal_settlement", null, null, null, "MERCHANT_SCOPE_REQUIRED");

        // M022-004:异议入口同样 fail-closed(disputeOnce 内的商户 scope 闸),不写库并落 dispute_failed。
        when(contractRepository.findSettlementById("s1"))
                .thenReturn(Optional.of(settlement("s1", "c1", "pending_settlement")));
        when(contractRepository.findContractById("c1")).thenReturn(Optional.of(contract("c1", "mch_a")));
        assertThatThrownBy(() -> service.submitSettlementDispute("s1",
                new MerchantSettlementDisputeRequest("k0", null, "无归属账号留言")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("MERCHANT_SCOPE_REQUIRED"));
        verify(contractRepository, never()).saveSettlement(any());
        verify(audit).recordFailure("merchant_portal.settlement.dispute_failed", "merchant.portal.settlement.dispute",
                "merchant_portal_settlement", "s1", null, "k0", "MERCHANT_SCOPE_REQUIRED");
    }

    @Test
    void merchantSeesOnlyOwnSettlements() {
        when(authz.currentMerchantScope()).thenReturn(Optional.of("mch_a"));
        when(contractRepository.listSettlementsByMerchant("mch_a"))
                .thenReturn(List.of(settlement("s1", "c1", "pending_settlement")));

        PageResponse<ContractSettlementRecord> page = service.listMySettlements(1, 500);

        assertThat(page.items()).extracting(ContractSettlementRecord::id).containsExactly("s1");
        assertThat(page.total()).isEqualTo(1L);
        verify(authz).requirePermission("merchant.portal.settlement.view");
        // M022-005:成功路径落 merchant_portal.settlement.viewed 审计。
        verify(audit).recordSuccess(eq("merchant_portal.settlement.viewed"), eq("merchant.portal.settlement.view"),
                eq("merchant_portal_settlement"), eq("mch_a"), isNull(), isNull(), eq(Map.of("count", 1)));
        verify(contractRepository, never()).listAllSettlementsPaged(anyInt(), anyInt());
    }

    @Test
    void merchantCanDisputeOwnOpenSettlementWithoutChangingStatus() {
        stubOwnOpenSettlement();

        ContractSettlementRecord disputed = service.submitSettlementDispute("s1",
                new MerchantSettlementDisputeRequest("k1", null, "金额与合同约定不符"));

        // M022-006:异议入口先过 merchant.portal.settlement.dispute 权限码。
        verify(authz).requirePermission("merchant.portal.settlement.dispute");
        assertThat(disputed.status()).isEqualTo("pending_settlement");
        assertThat(disputed.disputeReason()).isEqualTo("金额与合同约定不符");
        ArgumentCaptor<ContractSettlementRecord> captor = ArgumentCaptor.forClass(ContractSettlementRecord.class);
        verify(contractRepository).saveSettlement(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo("pending_settlement");
        verify(audit).recordSuccess(eq("merchant_portal.settlement.disputed"), eq("merchant.portal.settlement.dispute"),
                eq("merchant_portal_settlement"), eq("s1"), isNull(), eq("k1"),
                eq(Map.of("merchant_id", "mch_a", "reason", "金额与合同约定不符")));
    }

    // M020-002:平台开账(有争议)→主管裁决→商户留言,不清空裁决人/裁决时间,不覆盖平台理由。
    @Test
    void disputeKeepsResolutionTrailAndAppendsToExistingReason() {
        Instant resolvedAt = Instant.parse("2026-09-01T08:00:00Z");
        when(authz.currentMerchantScope()).thenReturn(Optional.of("mch_a"));
        when(contractRepository.findSettlementById("s1")).thenReturn(Optional.of(
                settlement("s1", "c1", "pending_settlement", "平台：押金核算存疑", "usr_manager", resolvedAt)));
        when(contractRepository.findContractById("c1")).thenReturn(Optional.of(contract("c1", "mch_a")));
        when(contractRepository.saveSettlement(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ContractSettlementRecord disputed = service.submitSettlementDispute("s1",
                new MerchantSettlementDisputeRequest("k2", null, "商户：违约金多算"));

        assertThat(disputed.resolvedBy()).isEqualTo("usr_manager");
        assertThat(disputed.resolvedAt()).isEqualTo(resolvedAt);
        assertThat(disputed.disputeReason()).isEqualTo("平台：押金核算存疑\n商户：违约金多算");
        assertThat(disputed.status()).isEqualTo("pending_settlement");
    }

    // M020-003 / M022-003:新留言单独超列宽、或「既有理由 + 新留言」合并后超列宽(500)→ 400 且不写库;恰为 500 允许。
    @Test
    void disputeReasonBeyondColumnWidthIsRejected() {
        stubOwnOpenSettlement();

        assertThatThrownBy(() -> service.submitSettlementDispute("s1",
                new MerchantSettlementDisputeRequest("k3", null, "漏算".repeat(251))))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
                    assertThat(e.getMessage()).contains("500");
                });
        verify(contractRepository, never()).saveSettlement(any());

        // M022-003:既有理由 300 字 + 换行 + 新留言 250 字 = 551 > 500;新留言单独并不超限,量的是合并后长度。
        when(contractRepository.findSettlementById("s1")).thenReturn(Optional.of(
                settlement("s1", "c1", "pending_settlement", "平".repeat(300), "usr_manager",
                        Instant.parse("2026-09-01T08:00:00Z"))));
        assertThatThrownBy(() -> service.submitSettlementDispute("s1",
                new MerchantSettlementDisputeRequest("k3b", null, "漏".repeat(250))))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
                    assertThat(e.getMessage()).contains("500");
                });
        verify(contractRepository, never()).saveSettlement(any());

        when(contractRepository.findSettlementById("s1"))
                .thenReturn(Optional.of(settlement("s1", "c1", "pending_settlement")));
        assertThat(service.submitSettlementDispute("s1",
                new MerchantSettlementDisputeRequest("k4", null, "漏算".repeat(250))).disputeReason())
                .hasSize(500);
    }

    // M020-004:同一幂等键重复提交只认一次(真 IdempotencyService + 内存仓储)。
    @Test
    void disputeWithSameIdempotencyKeyIsProcessedOnce() {
        stubOwnOpenSettlement();
        MerchantSettlementDisputeRequest request = new MerchantSettlementDisputeRequest("k5", null, "少算一期租金");

        ContractSettlementRecord first = service.submitSettlementDispute("s1", request);
        ContractSettlementRecord replay = service.submitSettlementDispute("s1", request);

        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(replay.disputeReason()).isEqualTo("少算一期租金");
        verify(contractRepository, times(1)).saveSettlement(any());
        verify(audit, times(1)).recordSuccess(eq("merchant_portal.settlement.disputed"), any(), any(), any(), any(), any(), any());
    }

    // M020-005:跨商户异议被拒、不写库,并落 dispute_failed 失败审计。
    @Test
    void disputeOnForeignSettlementIsForbiddenAndAudited() {
        when(authz.currentMerchantScope()).thenReturn(Optional.of("mch_b"));
        when(contractRepository.findSettlementById("s1"))
                .thenReturn(Optional.of(settlement("s1", "c1", "pending_settlement")));
        when(contractRepository.findContractById("c1")).thenReturn(Optional.of(contract("c1", "mch_a")));

        assertThatThrownBy(() -> service.submitSettlementDispute("s1",
                new MerchantSettlementDisputeRequest("k6", null, "越权尝试")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
        verify(contractRepository, never()).saveSettlement(any());
        verify(audit).recordFailure("merchant_portal.settlement.dispute_failed", "merchant.portal.settlement.dispute",
                "merchant_portal_settlement", "s1", null, "k6", "CROSS_MERCHANT_FORBIDDEN");
    }

    // M020-006:不可异议的报错固定中文,不回显英文内部状态码。
    @Test
    void settledOrAlreadyDisputedSettlementCannotBeDisputed() {
        when(authz.currentMerchantScope()).thenReturn(Optional.of("mch_a"));
        when(contractRepository.findContractById("c1")).thenReturn(Optional.of(contract("c1", "mch_a")));
        int n = 0;
        for (String status : List.of("settled", "disputed", "voided")) {
            n++;
            String key = "k7-" + n;
            when(contractRepository.findSettlementById("s1"))
                    .thenReturn(Optional.of(settlement("s1", "c1", status)));
            assertThatThrownBy(() -> service.submitSettlementDispute("s1",
                    new MerchantSettlementDisputeRequest(key, null, "重复异议")))
                    .isInstanceOfSatisfying(ApiException.class, e -> {
                        assertThat(e.code()).isEqualTo("SETTLEMENT_NOT_DISPUTABLE");
                        assertThat(e.getMessage()).isEqualTo("结算单当前状态不可发起异议").doesNotContain(status);
                    });
        }
        verify(contractRepository, never()).saveSettlement(any());
    }

    @Test
    void disputeReasonIsRequired() {
        stubOwnOpenSettlement();

        assertThatThrownBy(() -> service.submitSettlementDispute("s1",
                new MerchantSettlementDisputeRequest("k8", null, "  ")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
                    // M022-007:服务层机读英文经 GlobalExceptionHandler → FieldLabels.translate 出网,用户看到的是中文。
                    assertThat(FieldLabels.translate(e.getMessage())).isEqualTo("原因未填写");
                });
        verify(contractRepository, never()).saveSettlement(any());
        verify(audit).recordFailure(eq("merchant_portal.settlement.dispute_failed"), any(), any(), eq("s1"),
                isNull(), eq("k8"), eq("VALIDATION_FAILED"));
    }
}
