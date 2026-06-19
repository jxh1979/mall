package com.complex.platform.gate;

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
    private final InMemoryAuditEventRepository auditRepo = new InMemoryAuditEventRepository();
    private final AdmissionGuard guard =
            new AdmissionGuard(policyRepo, reviewService, new AuditEventService(auditRepo));

    @Test
    void noPolicyPassesThrough() {
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.empty());
        assertThatCode(() -> guard.check("bar.bill_waiver", "bar_bill", "b1")).doesNotThrowAnyException();
        verify(reviewService, never()).blockForAdmission(any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void allowOutcomePassesThrough() {
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(new AdmissionPolicyRecord(
                "p_allow", "ticketing.special_release", "verification", "permission_insufficient",
                "allow", "one_level_up", 1, true)));
        assertThatCode(() -> guard.check("ticketing.special_release", "verification", null))
                .doesNotThrowAnyException();
        verify(reviewService, never()).blockForAdmission(any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void warnOutcomePassesThroughAndRecordsWarnedEvent() {
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(new AdmissionPolicyRecord(
                "p_warn", "finance.locked_backfill", "ar_ap_item", "always",
                "warn", "one_level_up", 2, true)));
        assertThatCode(() -> guard.check("finance.locked_backfill", "ar_ap_item", "a1"))
                .doesNotThrowAnyException();
        assertThat(auditRepo.events).extracting(AuditEvent::eventName).contains("admission.warned");
        verify(reviewService, never()).blockForAdmission(any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void blockOutcomeBuildsPendingReviewAndThrowsAdmissionBlocked() {
        when(policyRepo.findEnabledPolicy(any(), any())).thenReturn(Optional.of(new AdmissionPolicyRecord(
                "p_block", "bar.bill_waiver", "bar_bill", "always",
                "block", "one_level_up", 2, true)));
        when(reviewService.blockForAdmission(eq("p_block"), eq("bar.bill_waiver"), eq("bar_bill"), eq("b1"),
                eq("one_level_up"), eq(2)))
                .thenReturn(new SupervisorReviewDecisionRecord(
                        "review_x", "p_block", "bar.bill_waiver", "bar_bill", "b1", true, "pending_review",
                        2, null, "blocked", null, List.of(), Instant.now(), "u1", "one_level_up", null));

        assertThatThrownBy(() -> guard.check("bar.bill_waiver", "bar_bill", "b1"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("ADMISSION_BLOCKED");
                    assertThat(e.getMessage()).contains("review_x");
                });
        verify(reviewService).blockForAdmission("p_block", "bar.bill_waiver", "bar_bill", "b1", "one_level_up", 2);
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
