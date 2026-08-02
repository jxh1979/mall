package com.complex.platform.gate;

import com.complex.platform.support.InMemoryAuditEventRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AdmissionGuardTests {

    private final AdmissionPolicyRepository policyRepo = mock(AdmissionPolicyRepository.class);
    private final SupervisorReviewService reviewService = mock(SupervisorReviewService.class);
    private final AdmissionFactRepository factRepo = mock(AdmissionFactRepository.class);
    private final AuthorizationService authz = mock(AuthorizationService.class);
    private final InMemoryAuditEventRepository auditRepo = new InMemoryAuditEventRepository();
    private final AdmissionGuard guard = new AdmissionGuard(
            policyRepo, reviewService, new AuditEventService(auditRepo), factRepo, authz);

    private static AdmissionPolicyRecord policy(String triggerMode, String outcome, int requiredLevel) {
        return new AdmissionPolicyRecord(
                "p", "act", "obj", triggerMode, outcome, "one_level_up", requiredLevel, true);
    }

    // ── 无策略 / outcome 基线 ──────────────────────────────────────────────

    @Test
    void noPolicyPassesThrough() {
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.empty());
        assertThatCode(() -> guard.check("bar.bill_waiver", "bar_bill", "b1")).doesNotThrowAnyException();
        verify(reviewService, never()).blockForAdmission(any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void allowOutcomeWhenTriggeredPassesThrough() {
        // permission_insufficient + 低职级 → 触发，但 outcome=allow → 放行不阻断。
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(
                policy("permission_insufficient", "allow", 2)));
        when(authz.currentActorLevelRank()).thenReturn(1);
        assertThatCode(() -> guard.check("act", "obj", null)).doesNotThrowAnyException();
        verify(reviewService, never()).blockForAdmission(any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void warnOutcomeWhenAlwaysTriggeredRecordsWarnedEvent() {
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(
                policy("always", "warn", 2)));
        assertThatCode(() -> guard.check("act", "obj", "a1")).doesNotThrowAnyException();
        assertThat(auditRepo.events).extracting(AuditEvent::eventName).contains("admission.warned");
        verify(reviewService, never()).blockForAdmission(any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void alwaysBlockBuildsPendingReviewAndThrowsAdmissionBlocked() {
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(new AdmissionPolicyRecord(
                "p_block", "bar.bill_waiver", "bar_bill", "always",
                "block", "one_level_up", 2, true)));
        when(reviewService.blockForAdmission(eq("p_block"), eq("bar.bill_waiver"), eq("bar_bill"), eq("b1"),
                eq("one_level_up"), eq(2)))
                .thenReturn(decision("review_x"));

        assertThatThrownBy(() -> guard.check("bar.bill_waiver", "bar_bill", "b1"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("ADMISSION_BLOCKED");
                    assertThat(e.getMessage()).contains("review_x");
                });
        verify(reviewService).blockForAdmission("p_block", "bar.bill_waiver", "bar_bill", "b1", "one_level_up", 2);
    }

    // ── expired：真实合同数据源 ─────────────────────────────────────────────

    @Test
    void expiredTriggeredWhenMerchantHasNoActiveContractBlocks() {
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(policy("expired", "block", 2)));
        when(authz.currentMerchantIdQuietly()).thenReturn(Optional.of("m1"));
        when(factRepo.isContractExpiredForMerchant("m1")).thenReturn(true);
        when(reviewService.blockForAdmission(any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(decision("r_exp"));

        assertThatThrownBy(() -> guard.check("act", "obj", "o1"))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.code()).isEqualTo("ADMISSION_BLOCKED"));
        verify(reviewService).blockForAdmission("p", "act", "obj", "o1", "one_level_up", 2);
    }

    @Test
    void expiredNotTriggeredWhenMerchantHasActiveContractPassesThrough() {
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(policy("expired", "block", 2)));
        when(authz.currentMerchantIdQuietly()).thenReturn(Optional.of("m1"));
        when(factRepo.isContractExpiredForMerchant("m1")).thenReturn(false);

        assertThatCode(() -> guard.check("act", "obj", "o1")).doesNotThrowAnyException();
        verify(reviewService, never()).blockForAdmission(any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void expiredNotTriggeredWhenNoMerchantScopePassesThrough() {
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(policy("expired", "block", 2)));
        when(authz.currentMerchantIdQuietly()).thenReturn(Optional.empty());

        assertThatCode(() -> guard.check("act", "obj", "o1")).doesNotThrowAnyException();
        verify(reviewService, never()).blockForAdmission(any(), any(), any(), any(), any(), anyInt());
    }

    // ── permission_insufficient：真实职级 ───────────────────────────────────

    @Test
    void permissionInsufficientTriggeredWhenLevelBelowRequiredBlocks() {
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(
                policy("permission_insufficient", "block", 3)));
        when(authz.currentActorLevelRank()).thenReturn(2);
        when(reviewService.blockForAdmission(any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(decision("r_perm"));

        assertThatThrownBy(() -> guard.check("act", "obj", "o1"))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.code()).isEqualTo("ADMISSION_BLOCKED"));
    }

    @Test
    void permissionInsufficientNotTriggeredWhenLevelMeetsRequiredPassesThrough() {
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(
                policy("permission_insufficient", "block", 3)));
        when(authz.currentActorLevelRank()).thenReturn(3);

        assertThatCode(() -> guard.check("act", "obj", "o1")).doesNotThrowAnyException();
        verify(reviewService, never()).blockForAdmission(any(), any(), any(), any(), any(), anyInt());
    }

    // ── qualification_abnormal：真实合规到期项 ──────────────────────────────

    @Test
    void qualificationAbnormalTriggeredWhenExpiredLicenseBlocks() {
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(
                policy("qualification_abnormal", "block", 3)));
        when(authz.currentMerchantIdQuietly()).thenReturn(Optional.of("m1"));
        when(factRepo.isQualificationAbnormalForMerchant("m1")).thenReturn(true);
        when(reviewService.blockForAdmission(any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(decision("r_qual"));

        assertThatThrownBy(() -> guard.check("act", "obj", "o1"))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.code()).isEqualTo("ADMISSION_BLOCKED"));
    }

    @Test
    void qualificationAbnormalNotTriggeredWhenLicensesValidPassesThrough() {
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(
                policy("qualification_abnormal", "block", 3)));
        when(authz.currentMerchantIdQuietly()).thenReturn(Optional.of("m1"));
        when(factRepo.isQualificationAbnormalForMerchant("m1")).thenReturn(false);

        assertThatCode(() -> guard.check("act", "obj", "o1")).doesNotThrowAnyException();
        verify(reviewService, never()).blockForAdmission(any(), any(), any(), any(), any(), anyInt());
    }

    // ── over_contract_scope：真实合同经营范围（V080 allowed_business_scope） ──

    @Test
    void overContractScopeNotTriggeredWhenScopeContainsDomainPassesThrough() {
        // 合同经营范围含 bar → bar.bill_waiver 放行。businessDomain = actionType 第一段 = "bar"。
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(
                policy("over_contract_scope", "block", 2)));
        when(authz.currentMerchantIdQuietly()).thenReturn(Optional.of("m1"));
        when(factRepo.isOverContractScopeForMerchant("m1", "bar")).thenReturn(false);

        assertThatCode(() -> guard.check("bar.bill_waiver", "bar_bill", "b1")).doesNotThrowAnyException();
        verify(reviewService, never()).blockForAdmission(any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void overContractScopeTriggeredWhenScopeMissesDomainBlocks() {
        // 经营范围不含该业务域 → 越界命中 → block。
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(
                policy("over_contract_scope", "block", 2)));
        when(authz.currentMerchantIdQuietly()).thenReturn(Optional.of("m1"));
        when(factRepo.isOverContractScopeForMerchant("m1", "ticketing")).thenReturn(true);
        when(reviewService.blockForAdmission(any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(decision("r_scope"));

        assertThatThrownBy(() -> guard.check("ticketing.special_release", "verification", "v1"))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.code()).isEqualTo("ADMISSION_BLOCKED"));
    }

    @Test
    void overContractScopeNotTriggeredWhenNoMerchantScopePassesThrough() {
        // 无商户上下文（平台管理员）→ 放行，不查事实源。
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(
                policy("over_contract_scope", "block", 2)));
        when(authz.currentMerchantIdQuietly()).thenReturn(Optional.empty());

        assertThatCode(() -> guard.check("bar.bill_waiver", "bar_bill", "b1")).doesNotThrowAnyException();
        verify(reviewService, never()).blockForAdmission(any(), any(), any(), any(), any(), anyInt());
    }

    // ── over_space：对象→空间→合同租赁（V080 contract_spaces） ──────────────

    @Test
    void overSpaceNotTriggeredWhenSpaceLeasedByContractPassesThrough() {
        // 解析到 space 且在生效合同租赁内 → 放行。
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(
                policy("over_space", "block", 2)));
        when(authz.currentMerchantIdQuietly()).thenReturn(Optional.of("m1"));
        when(factRepo.resolveSpaceId("merchant_service_work_order", "wo1")).thenReturn(Optional.of("sp1"));
        when(factRepo.isOverSpaceForMerchant("m1", "sp1")).thenReturn(false);

        assertThatCode(() -> guard.check("merchant_service.work_order_close",
                "merchant_service_work_order", "wo1")).doesNotThrowAnyException();
        verify(reviewService, never()).blockForAdmission(any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void overSpaceTriggeredWhenSpaceNotLeasedBlocks() {
        // 解析到 space 但不在生效合同租赁内 → 越界命中 → block。
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(
                policy("over_space", "block", 2)));
        when(authz.currentMerchantIdQuietly()).thenReturn(Optional.of("m1"));
        when(factRepo.resolveSpaceId("merchant_service_work_order", "wo1")).thenReturn(Optional.of("sp1"));
        when(factRepo.isOverSpaceForMerchant("m1", "sp1")).thenReturn(true);
        when(reviewService.blockForAdmission(any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(decision("r_space"));

        assertThatThrownBy(() -> guard.check("merchant_service.work_order_close",
                "merchant_service_work_order", "wo1"))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.code()).isEqualTo("ADMISSION_BLOCKED"));
    }

    @Test
    void overSpaceFailsClosedToReviewWhenObjectTypeNotResolvable() {
        // P0 fail-closed：配置了 over_space 却解析不到 space（resolveSpaceId 返回 empty）→
        // 保守进复核（不再 silent pass），并写 admission.space_unresolved_closed 留痕。
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(
                policy("over_space", "block", 2)));
        when(authz.currentMerchantIdQuietly()).thenReturn(Optional.of("m1"));
        when(factRepo.resolveSpaceId("ar_ap_item", "a1")).thenReturn(Optional.empty());
        when(reviewService.blockForAdmission(any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(decision("r_unresolved"));

        assertThatThrownBy(() -> guard.check("finance.locked_backfill", "ar_ap_item", "a1"))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.code()).isEqualTo("ADMISSION_BLOCKED"));
        assertThat(auditRepo.events).extracting(AuditEvent::eventName)
                .contains("admission.space_unresolved_closed");
        verify(reviewService).blockForAdmission(
                "p", "finance.locked_backfill", "ar_ap_item", "a1", "one_level_up", 2);
    }

    @Test
    void overSpaceNoMerchantContextStillPassesThrough() {
        // 无商户上下文（平台管理员等）对 over_space → 放行，不误伤合法越权角色。
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(
                policy("over_space", "block", 2)));
        when(authz.currentMerchantIdQuietly()).thenReturn(Optional.empty());

        assertThatCode(() -> guard.check("bar.bill_reopen", "bar_bill", "b1")).doesNotThrowAnyException();
        verify(reviewService, never()).blockForAdmission(any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void overSpaceBarBillResolvedAndLeasedPassesThrough() {
        // V084：bar_bill 解析到 space（bar_bills.table_id→bar_tables.space_id），且在合同租赁内 → 放行。
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(
                policy("over_space", "block", 2)));
        when(authz.currentMerchantIdQuietly()).thenReturn(Optional.of("m1"));
        when(factRepo.resolveSpaceId("bar_bill", "b1")).thenReturn(Optional.of("sp_bar"));
        when(factRepo.isOverSpaceForMerchant("m1", "sp_bar")).thenReturn(false);

        assertThatCode(() -> guard.check("bar.bill_waiver", "bar_bill", "b1")).doesNotThrowAnyException();
        verify(reviewService, never()).blockForAdmission(any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void overSpaceBarBillResolvedButNotLeasedBlocks() {
        // V084：bar_bill 解析到 space 但不在合同租赁内 → 越界命中 → block。
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(
                policy("over_space", "block", 2)));
        when(authz.currentMerchantIdQuietly()).thenReturn(Optional.of("m1"));
        when(factRepo.resolveSpaceId("bar_bill", "b1")).thenReturn(Optional.of("sp_bar"));
        when(factRepo.isOverSpaceForMerchant("m1", "sp_bar")).thenReturn(true);
        when(reviewService.blockForAdmission(any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(decision("r_bar_space"));

        assertThatThrownBy(() -> guard.check("bar.bill_waiver", "bar_bill", "b1"))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.code()).isEqualTo("ADMISSION_BLOCKED"));
    }

    @Test
    void overSpaceAmusementInspectionResolvedButNotLeasedBlocks() {
        // V084：inspection 经 project_id 解析到 amusement_projects.space_id，不在合同租赁内 → block。
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(
                policy("over_space", "block", 2)));
        when(authz.currentMerchantIdQuietly()).thenReturn(Optional.of("m1"));
        when(factRepo.resolveSpaceId("inspection", "ins1")).thenReturn(Optional.of("sp_amuse"));
        when(factRepo.isOverSpaceForMerchant("m1", "sp_amuse")).thenReturn(true);
        when(reviewService.blockForAdmission(any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(decision("r_amuse_space"));

        assertThatThrownBy(() -> guard.check("amusement.reopen", "inspection", "ins1"))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.code()).isEqualTo("ADMISSION_BLOCKED"));
    }

    @Test
    void overSpaceAmusementGroupBookingResolvedAndLeasedPassesThrough() {
        // V084：amusement_group_booking 经 project_id 解析到 space，在合同租赁内 → 放行。
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(
                policy("over_space", "block", 2)));
        when(authz.currentMerchantIdQuietly()).thenReturn(Optional.of("m1"));
        when(factRepo.resolveSpaceId("amusement_group_booking", "gb1")).thenReturn(Optional.of("sp_amuse"));
        when(factRepo.isOverSpaceForMerchant("m1", "sp_amuse")).thenReturn(false);

        assertThatCode(() -> guard.check("amusement.group_booking_approve",
                "amusement_group_booking", "gb1")).doesNotThrowAnyException();
        verify(reviewService, never()).blockForAdmission(any(), any(), any(), any(), any(), anyInt());
    }

    // ── 异常口径：非越界类 fail-open 放行；over_* 越界类 fail-closed 进复核 ──────

    @Test
    void evalErrorFailsClosedToReviewForNonOverScopeTriggerAfterC3() {
        // C3（用户裁决统一 fail-closed）：expired（非越界类）求值异常 → 改为 fail-closed 进复核
        // + admission.eval_failed_closed 留痕 + 抛 ADMISSION_BLOCKED（此前为 fail-open 放行 eval_skipped）。
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(policy("expired", "block", 2)));
        when(authz.currentMerchantIdQuietly()).thenReturn(Optional.of("m1"));
        when(factRepo.isContractExpiredForMerchant("m1")).thenThrow(new RuntimeException("db down"));
        when(reviewService.blockForAdmission(any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(decision("r_expired_closed"));

        assertThatThrownBy(() -> guard.check("act", "obj", "o1"))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.code()).isEqualTo("ADMISSION_BLOCKED"));
        assertThat(auditRepo.events).extracting(AuditEvent::eventName).contains("admission.eval_failed_closed");
        verify(reviewService).blockForAdmission(any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void overContractScopeEvalErrorFailsClosedToReview() {
        // P0：over_contract_scope 求值异常 → fail-closed 进复核（不放行）+ admission.eval_failed_closed 留痕。
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(
                policy("over_contract_scope", "block", 2)));
        when(authz.currentMerchantIdQuietly()).thenReturn(Optional.of("m1"));
        when(factRepo.isOverContractScopeForMerchant(eq("m1"), any()))
                .thenThrow(new RuntimeException("db down"));
        when(reviewService.blockForAdmission(any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(decision("r_scope_closed"));

        assertThatThrownBy(() -> guard.check("ticketing.refund_approve", "ticket_refund", "tr1"))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.code()).isEqualTo("ADMISSION_BLOCKED"));
        assertThat(auditRepo.events).extracting(AuditEvent::eventName)
                .contains("admission.eval_failed_closed");
        verify(reviewService).blockForAdmission(
                "p", "ticketing.refund_approve", "ticket_refund", "tr1", "one_level_up", 2);
    }

    @Test
    void overSpaceEvalErrorFailsClosedToReview() {
        // P0：over_space 求值异常（解析阶段抛错）→ fail-closed 进复核（不放行）+ 留痕。
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(
                policy("over_space", "block", 3)));
        when(authz.currentMerchantIdQuietly()).thenReturn(Optional.of("m1"));
        when(factRepo.resolveSpaceId("bar_bill", "b1")).thenThrow(new RuntimeException("db down"));
        when(reviewService.blockForAdmission(any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(decision("r_space_closed"));

        assertThatThrownBy(() -> guard.check("bar.bill_reopen", "bar_bill", "b1"))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.code()).isEqualTo("ADMISSION_BLOCKED"));
        assertThat(auditRepo.events).extracting(AuditEvent::eventName)
                .contains("admission.eval_failed_closed");
    }

    private static SupervisorReviewDecisionRecord decision(String reviewId) {
        return new SupervisorReviewDecisionRecord(
                reviewId, "p", "act", "obj", "o1", true, "pending_review",
                2, null, "blocked", null, List.of(), Instant.now(), "u1", "one_level_up", null);
    }
}
