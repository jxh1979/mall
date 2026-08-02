package com.complex.platform.evidence;

import com.complex.platform.support.InMemoryAuditEventRepository;
import com.complex.platform.support.InMemoryIdempotencyRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.dto.RiskAlertActionRequest;
import com.complex.platform.evidence.dto.RiskAlertReportRequest;
import com.complex.platform.evidence.dto.RiskAlertResolveRequest;
import com.complex.platform.support.TestSecurityActors;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RiskAlertServiceTests {

    private InMemoryRiskAlertRepository repository;
    private InMemoryAuditEventRepository auditEventRepository;
    private AuthorizationService authorizationService;
    private RiskAlertService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRiskAlertRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        authorizationService = mock(AuthorizationService.class);
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
        when(authorizationService.currentActor()).thenReturn(
                new AccessActor("usr_admin", "platform_admin_user", "platform_admin", "org_1", null, "active"));
        when(authorizationService.isPlatformAdmin(any())).thenReturn(true);
        TestSecurityActors.asPlatformAdmin();

        service = new RiskAlertService(
                repository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                authorizationService
        );
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    // ── report → claim → transfer → escalate → resolve（带结论）全链 ────────────

    @Test
    void fullResponsibilityChainReportToResolve() {
        RiskAlertRecord reported = service.report(report("chain-report"));
        assertThat(reported.status()).isEqualTo("open");
        assertThat(reported.firstReportedBy()).isEqualTo("usr_admin");

        RiskAlertRecord claimed = service.claim(reported.alertId(),
                new RiskAlertActionRequest("chain-claim", null, "认领", List.of()));
        assertThat(claimed.status()).isEqualTo("handling");
        assertThat(claimed.firstHandlerId()).isEqualTo("usr_admin");

        RiskAlertRecord transferred = service.transfer(reported.alertId(),
                new RiskAlertActionRequest("chain-transfer", "usr_handler_2", "转交", List.of()));
        assertThat(transferred.status()).isEqualTo("handling");
        assertThat(transferred.firstHandlerId()).isEqualTo("usr_handler_2");

        RiskAlertRecord escalated = service.escalate(reported.alertId(),
                new RiskAlertActionRequest("chain-escalate", "usr_manager", "升级", List.of()));
        assertThat(escalated.status()).isEqualTo("escalated");
        assertThat(escalated.escalatedTo()).isEqualTo("usr_manager");

        RiskAlertRecord resolved = service.resolve(reported.alertId(),
                new RiskAlertResolveRequest("chain-resolve", "已处理完毕并补救", "结案", List.of()));
        assertThat(resolved.status()).isEqualTo("resolved");
        assertThat(resolved.resolutionConclusion()).isEqualTo("已处理完毕并补救");
        assertThat(resolved.resolvedBy()).isEqualTo("usr_admin");

        // 责任链流水：report/claim/transfer/escalate/resolve 各一条。
        List<RiskAlertActionRecord> actions = repository.listActions(reported.alertId());
        assertThat(actions)
                .extracting(RiskAlertActionRecord::actionType)
                .containsExactly("report", "claim", "transfer", "escalate", "resolve");

        // 每个动作发 exception.* 审计事件。
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("exception.reported", "exception.claimed", "exception.transferred",
                        "exception.escalated", "exception.resolved");
    }

    // ── 无结论 resolve 被拒（服务层校验）───────────────────────────────────────

    @Test
    void resolveWithoutConclusionIsRejected() {
        RiskAlertRecord reported = service.report(report("no-conclusion-report"));

        assertThatThrownBy(() -> service.resolve(reported.alertId(),
                new RiskAlertResolveRequest("no-conclusion-resolve", "  ", null, List.of())))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");

        // 预警保持 open，未被错误关闭。
        assertThat(repository.findById(reported.alertId()))
                .get().extracting(RiskAlertRecord::status).isEqualTo("open");
    }

    // ── dismiss：直接忽略关闭 ──────────────────────────────────────────────────

    @Test
    void dismissClosesAlertWithoutConclusion() {
        RiskAlertRecord reported = service.report(report("dismiss-report"));

        RiskAlertRecord dismissed = service.dismiss(reported.alertId(),
                new RiskAlertActionRequest("dismiss-1", null, "误报", List.of()));
        assertThat(dismissed.status()).isEqualTo("dismissed");

        // 终态不可再流转。
        assertThatThrownBy(() -> service.claim(reported.alertId(),
                new RiskAlertActionRequest("dismiss-then-claim", null, null, List.of())))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("INVALID_STATE");
    }

    // ── 幂等：同 actor 同键重复 report 不重复建单 ──────────────────────────────

    @Test
    void reportIsIdempotentForSameActorAndKey() {
        RiskAlertRecord first = service.report(report("idem-report"));
        RiskAlertRecord second = service.report(report("idem-report"));

        assertThat(second.alertId()).isEqualTo(first.alertId());
        assertThat(repository.alerts).hasSize(1);
        assertThat(repository.listActions(first.alertId()))
                .extracting(RiskAlertActionRecord::actionType)
                .containsExactly("report");
    }

    // ── 幂等：动作级重复 claim 不重复推进流水 ──────────────────────────────────

    @Test
    void claimIsIdempotentForSameActorAndKey() {
        RiskAlertRecord reported = service.report(report("idem-claim-report"));
        service.claim(reported.alertId(), new RiskAlertActionRequest("idem-claim", null, null, List.of()));
        service.claim(reported.alertId(), new RiskAlertActionRequest("idem-claim", null, null, List.of()));

        assertThat(repository.listActions(reported.alertId()))
                .extracting(RiskAlertActionRecord::actionType)
                .containsExactly("report", "claim");
    }

    private RiskAlertReportRequest report(String idempotencyKey) {
        return new RiskAlertReportRequest(
                idempotencyKey, "bar", "bill_99", "settlement_mismatch", "high",
                "结算金额不符", "project_1", "巡检发现", List.of("att_1"));
    }

    // ── in-memory 仓储 / 依赖 ──────────────────────────────────────────────────

    private static final class InMemoryRiskAlertRepository implements RiskAlertRepository {
        private final Map<String, RiskAlertRecord> alerts = new LinkedHashMap<>();
        private final List<RiskAlertActionRecord> actions = new ArrayList<>();

        @Override
        public RiskAlertRecord saveAlert(RiskAlertRecord alert) {
            alerts.put(alert.alertId(), alert);
            return alert;
        }

        @Override
        public RiskAlertRecord updateAlert(RiskAlertRecord alert) {
            alerts.put(alert.alertId(), alert);
            return alert;
        }

        @Override
        public Optional<RiskAlertRecord> findById(String alertId) {
            return Optional.ofNullable(alerts.get(alertId));
        }

        @Override
        public RiskAlertActionRecord appendAction(RiskAlertActionRecord action) {
            actions.add(action);
            return action;
        }

        @Override
        public List<RiskAlertActionRecord> listActions(String alertId) {
            return actions.stream().filter(a -> alertId.equals(a.alertId())).toList();
        }

        @Override
        public List<RiskAlertRecord> search(String sourceDomain, String status, String projectId,
                                            String merchantScope, int limit) {
            List<RiskAlertRecord> newestFirst = new ArrayList<>(alerts.values());
            java.util.Collections.reverse(newestFirst);
            return newestFirst.stream()
                    .filter(a -> sourceDomain == null || sourceDomain.equals(a.sourceDomain()))
                    .filter(a -> status == null || status.equals(a.status()))
                    .filter(a -> projectId == null || projectId.equals(a.projectId()))
                    .filter(a -> merchantScope == null || merchantScope.equals(a.merchantId()))
                    .limit(limit)
                    .toList();
        }
    }
}
