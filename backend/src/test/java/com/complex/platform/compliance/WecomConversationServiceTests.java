package com.complex.platform.compliance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.compliance.dto.WecomConversationSyncRequest;
import com.complex.platform.compliance.dto.WecomRetentionPolicyRequest;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
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
    private WecomConversationService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryWecomRepository();
        auditRepository = new InMemoryAuditEventRepository();
        switchGuard = Mockito.mock(TerminalSwitchGuard.class);
        service = new WecomConversationService(
                repository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditRepository),
                switchGuard,
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

        @Override
        public List<WecomAccessAuditRecord> findAccessAuditsByConversation(String conversationId) {
            return audits.stream().filter(a -> a.conversationId().equals(conversationId)).toList();
        }
    }

    private static final class InMemoryIdempotencyRepository implements IdempotencyRepository {
        private final Map<String, IdempotencyRecord> records = new LinkedHashMap<>();

        @Override
        public Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey) {
            return Optional.ofNullable(records.get(operationId + ":" + idempotencyKey));
        }

        @Override
        public boolean tryReserve(IdempotencyRecord record) {
            String key = record.operationId() + ":" + record.idempotencyKey();
            if (records.containsKey(key)) {
                return false;
            }
            records.put(key, record);
            return true;
        }

        @Override
        public IdempotencyRecord complete(IdempotencyRecord record) {
            records.put(record.operationId() + ":" + record.idempotencyKey(), record);
            return record;
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
