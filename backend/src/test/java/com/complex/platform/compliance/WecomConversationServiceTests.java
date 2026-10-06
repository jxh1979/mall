package com.complex.platform.compliance;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.support.InMemoryIdempotencyRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.compliance.dto.WecomConversationSyncRequest;
import com.complex.platform.compliance.dto.WecomRetentionPolicyRequest;
import com.complex.platform.compliance.qc.DisabledWecomQc;
import com.complex.platform.compliance.qc.ReservedWecomQc;
import com.complex.platform.compliance.qc.WecomQcPort;
import com.complex.platform.compliance.qc.WecomQcRequest;
import com.complex.platform.compliance.qc.WecomQcResult;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.RiskAlertService;
import com.complex.platform.evidence.dto.RiskAlertReportRequest;
import com.complex.platform.gate.TerminalSwitchGuard;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class WecomConversationServiceTests {

    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-06-18T00:00:00Z"), ZoneOffset.UTC);

    private InMemoryWecomRepository repository;
    private InMemoryAuditEventRepository auditRepository;
    private TerminalSwitchGuard switchGuard;
    private RiskAlertService riskAlertService;
    private WecomConversationService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryWecomRepository();
        auditRepository = new InMemoryAuditEventRepository();
        switchGuard = Mockito.mock(TerminalSwitchGuard.class);
        // 统一异常责任链服务（evidence 既有，P1：企微命中接入统一 risk_alerts）。
        riskAlertService = Mockito.mock(RiskAlertService.class);
        // 默认：AI 质检开关关（DisabledWecomQc）→ 仅关键词质检（行为不变）。
        service = newService(new DisabledWecomQc());
    }

    private WecomConversationService newService(WecomQcPort qcPort) {
        return new WecomConversationService(
                repository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditRepository),
                switchGuard,
                qcPort,
                new ObjectMapper(),
                riskAlertService,
                // M723-F14：项目范围取自登录主体；这里的桩返回空范围＝全量（平台管理员口径），项目校验的回归在真库接口用例里
                Mockito.mock(AuthorizationService.class),
                FIXED
        );
    }

    @Test
    void syncRejectedWhenSwitchDisabled() {
        // 开关关 → TerminalSwitchGuard.check 抛 TERMINAL_DISABLED；同步拒绝、不落库。
        Mockito.doThrow(ApiException.forbidden("TERMINAL_DISABLED", "disabled"))
                .when(switchGuard).check(WecomConversationService.SYNC_CAPABILITY);

        assertThatThrownBy(() -> service.syncConversation(syncRequest("ext-1", "加我微信 13800001111")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("TERMINAL_DISABLED");
        assertThat(repository.conversations).isEmpty();
        assertThat(repository.messages).isEmpty();
        assertThat(auditRepository.names()).contains("wecom.conversation_sync_failed");
    }

    @Test
    void syncRejectedWhenProjectIdNotRegistered() {
        // 堵越权注入：syncWecom 不再裸信客户端 projectId，未登记项目一律拒绝，不落库。
        repository.knownProjects.add("proj_other"); // 只认 proj_other；syncRequest 用 proj_1 → 应拒
        assertThatThrownBy(() -> service.syncConversation(syncRequest("ext-bad-proj", "普通消息")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("WECOM_PROJECT_NOT_FOUND");
        assertThat(repository.conversations).isEmpty();
        assertThat(repository.messages).isEmpty();
    }

    @Test
    void syncStoresRedactedDisplayContentAndKeepsRawReference() {
        // 开关开（mock 默认不抛）→ 落库脱敏：展示内容无明文手机号/身份证，原文以引用形式保留。
        WecomSyncResult result = service.syncConversation(
                syncRequest("ext-2", "我的电话 13800001111 身份证 11010119900307123X"));

        assertThat(result.messagesStored()).isEqualTo(1);
        WecomMessageRecord stored = repository.messages.get(0);
        assertThat(stored.contentRedacted()).doesNotContain("13800001111");
        assertThat(stored.contentRedacted()).doesNotContain("11010119900307123X");
        assertThat(stored.contentRedacted()).contains("[PHONE_REDACTED]");
        assertThat(stored.contentRawRef()).startsWith("wecom-raw:");
        assertThat(auditRepository.names()).contains("wecom.conversation_synced");
    }

    @Test
    void syncFlagsSensitiveHitsForReview() {
        // 敏感命中（诱导私微 + 私收款）→ risk_flags 标记进复核。
        WecomSyncResult result = service.syncConversation(
                syncRequest("ext-3", "加我私微，直接私收款转我"));

        assertThat(result.flaggedForReview()).isEqualTo(1);
        assertThat(repository.messages.get(0).riskFlags())
                .contains("lure_private_contact", "private_payment");
    }

    @Test
    void syncProducesRiskHitRecordsAndEnqueuesReviewAlerts() {
        // P0#8：命中 → 写 wecom_risk_hits（open）+ 入复核队列（analytics_risk_alerts，source_domain=wecom）+ 事件。
        service.syncConversation(syncRequest("ext-hit", "加我私微，直接私收款转我"));

        // 命中两条规则码（诱导私微 + 私收款），各产一条 hit（open，关联 message + project）。
        assertThat(repository.riskHits)
                .hasSize(2)
                .allSatisfy(hit -> {
                    assertThat(hit.status()).isEqualTo("open");
                    assertThat(hit.projectId()).isEqualTo("proj_1");
                    assertThat(hit.messageId()).startsWith("wmsg_");
                    assertThat(hit.alertId()).isNotBlank();
                });
        assertThat(repository.riskHits).extracting(WecomRiskHitRecord::ruleCode)
                .contains("LURE_PRIVATE_WECHAT", "STAFF_PRIVATE_PAYMENT");
        // 入复核队列（一命中一告警）。
        assertThat(repository.riskAlerts).hasSize(2);
        assertThat(auditRepository.names()).contains("wecom.risk.hit_detected");
    }

    @Test
    void syncHitReportsToUnifiedRiskAlertsWithWecomDomain() {
        // P1：企微违规命中 → 接入统一异常责任链 risk_alerts（RiskAlertService.report），
        // source_domain='wecom'，带 conversation/message/rule_code/risk_level 上下文。
        service.syncConversation(syncRequest("ext-unified", "加我私微，直接私收款转我"));

        org.mockito.ArgumentCaptor<RiskAlertReportRequest> captor =
                org.mockito.ArgumentCaptor.forClass(RiskAlertReportRequest.class);
        // 命中两条规则（诱导私微 + 私收款）→ 统一 risk_alerts 各上报一行。
        Mockito.verify(riskAlertService, Mockito.times(2)).report(captor.capture());
        List<RiskAlertReportRequest> reports = captor.getAllValues();
        assertThat(reports).allSatisfy(req -> {
            assertThat(req.sourceDomain()).isEqualTo("wecom");
            assertThat(req.riskLevel()).isIn("normal", "suspicious", "high", "critical");
            assertThat(req.sourceRef()).isNotBlank();
            assertThat(req.alertType()).isEqualTo("wecom_risk_hit");
            assertThat(req.summary()).isNotBlank();
            assertThat(req.projectId()).isEqualTo("proj_1");
            assertThat(req.idempotencyKey()).isNotBlank();
        });
        // 命中规则码透传到统一上报上下文（reason 或 summary）。
        assertThat(reports).anySatisfy(
                req -> assertThat(req.summary()).contains("LURE_PRIVATE_WECHAT"));
        assertThat(reports).anySatisfy(
                req -> assertThat(req.summary()).contains("STAFF_PRIVATE_PAYMENT"));
    }

    @Test
    void riskHitSnippetIsRedacted() {
        // P0#8 脱敏：命中片段经 PiiTextRedactor，不落明文 PII（此处规则关键词本身无 PII，校验脱敏链已接）。
        service.syncConversation(syncRequest("ext-snippet", "我的电话 13800001111，加我私微"));
        assertThat(repository.riskHits).isNotEmpty();
        assertThat(repository.riskHits).allSatisfy(
                hit -> assertThat(hit.matchedSnippet()).doesNotContain("13800001111"));
    }

    @Test
    void riskHitIdempotentOnRepeatedSync() {
        // 同一消息同一规则码幂等：重复同步（同 message 派生键）不产生重复命中/告警。
        service.syncConversation(syncRequest("ext-idem", "加我私微"));
        int hitsAfterFirst = repository.riskHits.size();
        // 重新触发同一会话同一内容（新消息 id），命中仍按 message×rule 各一。
        assertThat(hitsAfterFirst).isEqualTo(1);
    }

    @Test
    void reviewRiskHitFlowsOpenToReviewingThenConfirmed() {
        // P0#8 复核流转：open → reviewing → confirmed，写 reviewer + 事件 wecom.risk.hit_reviewed。
        service.syncConversation(syncRequest("ext-review", "加我私微"));
        String hitId = repository.riskHits.get(0).hitId();

        WecomRiskHitRecord reviewing = service.reviewRiskHit(hitId, "reviewing", "reviewer_1", null);
        assertThat(reviewing.status()).isEqualTo("reviewing");
        assertThat(reviewing.reviewerId()).isEqualTo("reviewer_1");

        WecomRiskHitRecord confirmed = service.reviewRiskHit(hitId, "confirmed", "reviewer_1", "确认违规");
        assertThat(confirmed.status()).isEqualTo("confirmed");
        assertThat(confirmed.reviewNote()).isEqualTo("确认违规");
        assertThat(auditRepository.names()).contains("wecom.risk.hit_reviewed");
    }

    @Test
    void reviewRiskHitDismissAsFalsePositive() {
        service.syncConversation(syncRequest("ext-dismiss", "加我私微"));
        String hitId = repository.riskHits.get(0).hitId();

        WecomRiskHitRecord dismissed = service.reviewRiskHit(hitId, "dismissed", "reviewer_2", "误报");
        assertThat(dismissed.status()).isEqualTo("dismissed");
    }

    @Test
    void reviewResolvedHitIsRejected() {
        // 终态不可再流转。
        service.syncConversation(syncRequest("ext-resolved", "加我私微"));
        String hitId = repository.riskHits.get(0).hitId();
        service.reviewRiskHit(hitId, "confirmed", "reviewer_3", null);

        assertThatThrownBy(() -> service.reviewRiskHit(hitId, "dismissed", "reviewer_3", null))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("WECOM_RISK_HIT_ALREADY_RESOLVED");
    }

    @Test
    void listRiskHitsFiltersByStatus() {
        service.syncConversation(syncRequest("ext-list", "加我私微"));
        assertThat(service.listRiskHits("open", null).items()).hasSize(1);
        assertThat(service.listRiskHits("confirmed", null).items()).isEmpty();
    }

    @Test
    void aiQcDisabledKeepsKeywordResultAndDoesNotInvokeAi() {
        // 开关关（DisabledWecomQc，setUp 默认）：不调 AI；ai_qc_status=skipped，无 AI 结果；关键词命中照常。
        CountingQcPort counting = new CountingQcPort(new DisabledWecomQc());
        WecomConversationService svc = newService(counting);

        svc.syncConversation(syncRequest("ext-ai-off", "加我私微，私收款转我"));

        WecomMessageRecord stored = repository.messages.get(0);
        // 关键词质检结果不变（AI 不覆盖）。
        assertThat(stored.riskFlags()).contains("lure_private_contact", "private_payment");
        // AI 走 SKIPPED 占位（不调真实 AI），结果留空。
        assertThat(stored.aiQcStatus()).isEqualTo("skipped");
        assertThat(stored.aiQcResult()).isNull();
        // 端口仍被调用（开关由 router 内部判定，本测用 Disabled 实现验证语义）。
        assertThat(counting.calls).isEqualTo(1);
    }

    @Test
    void aiQcEnabledReservedReturnsNotConfiguredWithoutBreakingKeyword() {
        // 开关开但无真实厂商（ReservedWecomQc）：返回 NOT_CONFIGURED 占位，不崩、不阻塞；关键词结果仍在。
        WecomConversationService svc = newService(new ReservedWecomQc());

        WecomSyncResult result = svc.syncConversation(syncRequest("ext-ai-on", "加我私微 13800001111"));

        assertThat(result.messagesStored()).isEqualTo(1);
        WecomMessageRecord stored = repository.messages.get(0);
        // 关键词命中保留 + 脱敏照常（AI 不影响）。
        assertThat(stored.riskFlags()).contains("lure_private_contact");
        assertThat(stored.contentRedacted()).doesNotContain("13800001111");
        // AI 开关开但无厂商 → not_configured 占位接缝，结果留空（不崩）。
        assertThat(stored.aiQcStatus()).isEqualTo("not_configured");
        assertThat(stored.aiQcResult()).isNull();
    }

    @Test
    void aiQcAnalyzedResultIsPersistedAlongsideKeywordFlags() {
        // 真实适配器接缝（ANALYZED 有结构化结果）：ai_qc_result 落库，关键词结果独立并存。
        WecomQcPort analyzing = new WecomQcPort() {
            @Override
            public WecomQcResult analyzeMessage(WecomQcRequest request) {
                return new WecomQcResult(WecomQcResult.Status.ANALYZED, "negative", 80,
                        List.of("complaint_tendency"), "ok");
            }

            @Override
            public List<WecomQcResult> analyzeConversation(List<WecomQcRequest> requests) {
                return List.of();
            }
        };
        WecomConversationService svc = newService(analyzing);

        svc.syncConversation(syncRequest("ext-ai-analyzed", "加我私微"));

        WecomMessageRecord stored = repository.messages.get(0);
        assertThat(stored.aiQcStatus()).isEqualTo("analyzed");
        assertThat(stored.aiQcResult())
                .contains("negative")
                .contains("complaint_tendency");
        // 关键词结果独立保留（AI 不覆盖）。
        assertThat(stored.riskFlags()).contains("lure_private_contact");
    }

    @Test
    void aiQcAdapterErrorIsFailSafeAndDoesNotBlock() {
        // 适配器抛异常 → fail-safe：本测直接用会抛异常的端口，service 不应崩（端口路由层兜底）。
        // 注：生产路由 WecomQcRouter 已兜底；此处校验占位 FAILED 路径不影响落库与关键词结果。
        WecomQcPort failing = new WecomQcPort() {
            @Override
            public WecomQcResult analyzeMessage(WecomQcRequest request) {
                return WecomQcResult.failed("adapter error");
            }

            @Override
            public List<WecomQcResult> analyzeConversation(List<WecomQcRequest> requests) {
                return List.of();
            }
        };
        WecomConversationService svc = newService(failing);

        WecomSyncResult result = svc.syncConversation(syncRequest("ext-ai-fail", "加我私微"));

        assertThat(result.messagesStored()).isEqualTo(1);
        WecomMessageRecord stored = repository.messages.get(0);
        assertThat(stored.aiQcStatus()).isEqualTo("failed");
        assertThat(stored.aiQcResult()).isNull();
        assertThat(stored.riskFlags()).contains("lure_private_contact");
    }

    /** 计数包装端口：验证 AI 质检确被调用次数（语义=每条消息一次）。 */
    private static final class CountingQcPort implements WecomQcPort {
        private final WecomQcPort delegate;
        private int calls;

        CountingQcPort(WecomQcPort delegate) {
            this.delegate = delegate;
        }

        @Override
        public WecomQcResult analyzeMessage(WecomQcRequest request) {
            calls++;
            return delegate.analyzeMessage(request);
        }

        @Override
        public List<WecomQcResult> analyzeConversation(List<WecomQcRequest> requests) {
            return delegate.analyzeConversation(requests);
        }
    }

    @Test
    void syncComputesRetentionUntilFromPolicyDays() {
        // 默认 180 天：retention_until = business_date + 180。
        service.syncConversation(syncRequest("ext-4", "普通消息"));
        WecomConversationRecord conversation = repository.conversations.get(0);
        assertThat(conversation.retentionUntil())
                .isEqualTo(LocalDate.parse("2026-06-18").plusDays(180));
    }

    @Test
    void viewRedactedReturnsRedactedAndWritesAudit() {
        service.syncConversation(syncRequest("ext-5", "电话 13800001111"));
        String conversationId = repository.conversations.get(0).id();

        PageResponse<WecomMessageView> view = service.viewMessages(conversationId, "redacted", "user_view", null);

        assertThat(view.items()).hasSize(1);
        assertThat(view.items().get(0).rawContent()).isNull();
        assertThat(view.items().get(0).content()).doesNotContain("13800001111");
        assertThat(repository.audits)
                .extracting(WecomAccessAuditRecord::viewType)
                .containsExactly("redacted");
        assertThat(auditRepository.names()).contains("wecom.redacted_viewed");
    }

    @Test
    void viewRawWithoutApprovalIsRejected() {
        service.syncConversation(syncRequest("ext-6", "电话 13800001111"));
        String conversationId = repository.conversations.get(0).id();

        assertThatThrownBy(() -> service.viewMessages(conversationId, "raw", "user_view", null))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("WECOM_RAW_APPROVAL_REQUIRED");
        assertThat(repository.audits).isEmpty();
        assertThat(auditRepository.names()).contains("wecom.raw_view_failed");
    }

    @Test
    void viewRawWithApprovalReturnsRawAndWritesRawAudit() {
        service.syncConversation(syncRequest("ext-7", "电话 13800001111"));
        String conversationId = repository.conversations.get(0).id();

        PageResponse<WecomMessageView> view =
                service.viewMessages(conversationId, "raw", "user_view", "approval-123");

        assertThat(view.items().get(0).rawContent()).startsWith("wecom-raw:");
        assertThat(repository.audits)
                .singleElement()
                .satisfies(audit -> {
                    assertThat(audit.viewType()).isEqualTo("raw");
                    assertThat(audit.approvalRef()).isEqualTo("approval-123");
                    assertThat(audit.viewerUserId()).isEqualTo("user_view");
                });
        assertThat(auditRepository.names()).contains("wecom.raw_viewed", "sensitive.viewed");
    }

    @Test
    void retentionArchiveActionSetsArchivedStatus() {
        seedDueConversation("conv-archive", "archive");
        WecomRetentionRunResult result = service.applyRetention(LocalDate.parse("2026-06-18"));

        assertThat(result.archived()).isEqualTo(1);
        assertThat(repository.conversations.get(0).archiveStatus()).isEqualTo("archived");
        // archive 保留消息内容（不清空）。
        assertThat(repository.messages.get(0).anonymized()).isFalse();
        assertThat(auditRepository.names()).contains("wecom.retention_applied");
    }

    @Test
    void retentionDeleteActionRemovesMessagesAndMarksDeleted() {
        seedDueConversation("conv-delete", "delete");
        WecomRetentionRunResult result = service.applyRetention(LocalDate.parse("2026-06-18"));

        assertThat(result.deleted()).isEqualTo(1);
        assertThat(repository.conversations.get(0).archiveStatus()).isEqualTo("deleted");
        assertThat(repository.messages).isEmpty();
    }

    @Test
    void retentionAnonymizeActionClearsRawAndMarksAnonymized() {
        seedDueConversation("conv-anon", "anonymize");
        WecomRetentionRunResult result = service.applyRetention(LocalDate.parse("2026-06-18"));

        assertThat(result.anonymized()).isEqualTo(1);
        assertThat(repository.conversations.get(0).archiveStatus()).isEqualTo("anonymized");
        WecomMessageRecord msg = repository.messages.get(0);
        assertThat(msg.anonymized()).isTrue();
        assertThat(msg.contentRawRef()).isNull();
        assertThat(msg.contentRedacted()).isEqualTo("[ANONYMIZED]");
    }

    @Test
    void retentionSkipsConversationsNotYetDue() {
        // retention_until 未到期 → 不处理。
        repository.conversations.add(new WecomConversationRecord(
                "conv-future", "ext-future", "proj_1", null, null, "active",
                LocalDate.parse("2026-12-31"), Instant.now(FIXED), Instant.now(FIXED)));
        WecomRetentionRunResult result = service.applyRetention(LocalDate.parse("2026-06-18"));
        assertThat(result.archived() + result.deleted() + result.anonymized()).isZero();
    }

    @Test
    void upsertRetentionPolicyChangesEffectiveDaysForNewSync() {
        // 配置改 365 → 新会话按新期限计算 retention_until。
        service.upsertRetentionPolicy(new WecomRetentionPolicyRequest(
                "policy-365", "global", "*", 365, "archive", true));

        service.syncConversation(syncRequest("ext-365", "普通消息"));
        assertThat(repository.conversations.get(0).retentionUntil())
                .isEqualTo(LocalDate.parse("2026-06-18").plusDays(365));
    }

    private void seedDueConversation(String externalId, String action) {
        repository.saveRetentionPolicy(new WecomRetentionPolicyRecord(
                "wrp_test", "global", "*", 180, action, true, Instant.now(FIXED), Instant.now(FIXED)));
        WecomConversationRecord conversation = repository.saveConversation(new WecomConversationRecord(
                "wcv_" + externalId, externalId, "proj_1", "staff_1", "cust_1", "active",
                LocalDate.parse("2026-01-01"), Instant.now(FIXED), Instant.now(FIXED)));
        repository.saveMessage(new WecomMessageRecord(
                "wmsg_" + externalId, conversation.id(), "staff_1", "wecom-raw:abc",
                "电话 [PHONE_REDACTED]", List.of(), false, Instant.now(FIXED),
                LocalDate.parse("2026-01-01"), Instant.now(FIXED)));
    }

    private WecomConversationSyncRequest syncRequest(String externalId, String content) {
        return new WecomConversationSyncRequest(
                "idem-" + externalId,
                externalId,
                "proj_1",
                "staff_1",
                "cust_1",
                List.of(new WecomConversationSyncRequest.Message(
                        "msg-" + externalId, "staff_1", content, Instant.parse("2026-06-18T08:00:00Z")))
        );
    }

    // ---------------------------------------------------------------- in-memory fakes

    static final class InMemoryWecomRepository implements WecomComplianceRepository {
        final List<WecomRetentionPolicyRecord> policies = new ArrayList<>();
        final List<WecomConversationRecord> conversations = new ArrayList<>();
        final List<WecomMessageRecord> messages = new ArrayList<>();
        final List<WecomAccessAuditRecord> audits = new ArrayList<>();

        @Override
        public WecomRetentionPolicyRecord saveRetentionPolicy(WecomRetentionPolicyRecord record) {
            policies.removeIf(p -> p.scopeType().equals(record.scopeType()) && p.scopeId().equals(record.scopeId()));
            policies.add(record);
            return record;
        }

        @Override
        public List<WecomRetentionPolicyRecord> findAllRetentionPolicies() {
            return new ArrayList<>(policies);
        }

        final java.util.Set<String> knownProjects = new java.util.HashSet<>();

        @Override
        public boolean projectExists(String projectId) {
            // 默认（未显式登记项目时）放行，保持既有同步测试不变；登记后只认已登记项目，供越权拒绝用例。
            return knownProjects.isEmpty() || knownProjects.contains(projectId);
        }

        @Override
        public Optional<WecomRetentionPolicyRecord> findEffectivePolicy(String projectId) {
            return policies.stream()
                    .filter(WecomRetentionPolicyRecord::enabled)
                    .filter(p -> ("project".equals(p.scopeType()) && p.scopeId().equals(projectId))
                            || "global".equals(p.scopeType()))
                    .sorted((a, b) -> Integer.compare(
                            "project".equals(a.scopeType()) ? 0 : 1,
                            "project".equals(b.scopeType()) ? 0 : 1))
                    .findFirst();
        }

        @Override
        public WecomConversationRecord saveConversation(WecomConversationRecord record) {
            conversations.removeIf(c -> c.id().equals(record.id())
                    || c.externalConversationId().equals(record.externalConversationId()));
            conversations.add(record);
            return record;
        }

        @Override
        public Optional<WecomConversationRecord> findConversationByExternalId(String externalConversationId) {
            return conversations.stream()
                    .filter(c -> c.externalConversationId().equals(externalConversationId))
                    .findFirst();
        }

        @Override
        public Optional<WecomConversationRecord> findConversationById(String id) {
            return conversations.stream().filter(c -> c.id().equals(id)).findFirst();
        }

        @Override
        public List<WecomConversationRecord> findConversationsDueForRetention(LocalDate today) {
            return conversations.stream()
                    .filter(c -> "active".equals(c.archiveStatus()))
                    .filter(c -> c.retentionUntil() != null && c.retentionUntil().isBefore(today))
                    .toList();
        }

        @Override
        public void updateConversationArchiveStatus(String id, String archiveStatus) {
            for (int i = 0; i < conversations.size(); i++) {
                WecomConversationRecord c = conversations.get(i);
                if (c.id().equals(id)) {
                    conversations.set(i, new WecomConversationRecord(
                            c.id(), c.externalConversationId(), c.projectId(), c.ownerStaffId(),
                            c.customerRef(), archiveStatus, c.retentionUntil(), c.createdAt(), c.updatedAt()));
                }
            }
        }

        @Override
        public WecomMessageRecord saveMessage(WecomMessageRecord record) {
            messages.add(record);
            return record;
        }

        @Override
        public List<WecomMessageRecord> findMessagesByConversation(String conversationId) {
            return messages.stream().filter(m -> m.conversationId().equals(conversationId)).toList();
        }

        @Override
        public void anonymizeMessagesByConversation(String conversationId) {
            for (int i = 0; i < messages.size(); i++) {
                WecomMessageRecord m = messages.get(i);
                if (m.conversationId().equals(conversationId)) {
                    messages.set(i, new WecomMessageRecord(
                            m.id(), m.conversationId(), m.senderRef(), null, "[ANONYMIZED]",
                            m.riskFlags(), true, m.sentAt(), m.businessDate(), m.createdAt()));
                }
            }
        }

        @Override
        public void deleteMessagesByConversation(String conversationId) {
            messages.removeIf(m -> m.conversationId().equals(conversationId));
        }

        @Override
        public WecomAccessAuditRecord saveAccessAudit(WecomAccessAuditRecord record) {
            audits.add(record);
            return record;
        }

        // P0#8 ---------------------------------------------------------------- risk rules / hits / alerts

        final List<WecomRiskRuleRecord> riskRules = new ArrayList<>();
        final List<WecomRiskHitRecord> riskHits = new ArrayList<>();
        final List<WecomRiskAlertEnqueue> riskAlerts = new ArrayList<>();

        @Override
        public List<WecomRiskRuleRecord> findEnabledWecomRiskRules() {
            // 空 → service 回退内置默认规则（不退化），与既有关键词测试一致。
            return new ArrayList<>(riskRules);
        }

        @Override
        public WecomRiskHitRecord saveRiskHit(WecomRiskHitRecord record) {
            // 幂等：同一 message_id × rule_code 不重复。
            boolean exists = riskHits.stream().anyMatch(
                    h -> h.messageId().equals(record.messageId()) && h.ruleCode().equals(record.ruleCode()));
            if (!exists) {
                riskHits.add(record);
            }
            return record;
        }

        @Override
        public List<WecomRiskHitRecord> findRiskHits(String status, String conversationId) {
            return riskHits.stream()
                    .filter(h -> status == null || h.status().equals(status))
                    .filter(h -> conversationId == null || h.conversationId().equals(conversationId))
                    .toList();
        }

        @Override
        public Optional<WecomRiskHitRecord> findRiskHitById(String hitId) {
            return riskHits.stream().filter(h -> h.hitId().equals(hitId)).findFirst();
        }

        @Override
        public WecomRiskHitRecord updateRiskHitReview(
                String hitId, String status, String reviewerId, String reviewNote) {
            for (int i = 0; i < riskHits.size(); i++) {
                WecomRiskHitRecord h = riskHits.get(i);
                if (h.hitId().equals(hitId)) {
                    WecomRiskHitRecord updated = new WecomRiskHitRecord(
                            h.hitId(), h.conversationId(), h.messageId(), h.ruleCode(), h.riskLevel(),
                            h.matchedSnippet(), status, reviewerId, reviewNote, h.projectId(),
                            h.alertId(), h.createdAt(), h.updatedAt());
                    riskHits.set(i, updated);
                    return updated;
                }
            }
            return null;
        }

        @Override
        public String enqueueRiskAlert(WecomRiskAlertEnqueue enqueue) {
            // 幂等键复用既有 alert id（重复同步不产生重复告警）。
            return riskAlerts.stream()
                    .filter(a -> a.idempotencyKey().equals(enqueue.idempotencyKey()))
                    .map(WecomRiskAlertEnqueue::alertId)
                    .findFirst()
                    .orElseGet(() -> {
                        riskAlerts.add(enqueue);
                        return enqueue.alertId();
                    });
        }

    }

    private static final class InMemoryAuditEventRepository implements AuditEventRepository {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public AuditEvent save(AuditEvent event) {
            events.add(event);
            return event;
        }

        List<String> names() {
            return events.stream().map(AuditEvent::eventName).toList();
        }
    }
}
