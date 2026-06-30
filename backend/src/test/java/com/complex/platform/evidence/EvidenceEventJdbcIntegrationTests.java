package com.complex.platform.evidence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
@Transactional
class EvidenceEventJdbcIntegrationTests {

    @Autowired
    private JdbcEvidenceEventRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JdbcAuditEventRepository auditRepository;

    @BeforeEach
    void resetEvidenceEventTables() {
        jdbcTemplate.execute("TRUNCATE evidence_events");
    }

    @Test
    void searchOperationEventsFiltersAcrossObjects() {
        jdbcTemplate.execute("TRUNCATE audit_events");
        Instant t = Instant.parse("2026-06-14T00:00:00Z");
        auditRepository.save(auditEvent("it_ticket_order", "o_x", "alice", "ticket_refund.approved", t));
        auditRepository.save(auditEvent("it_ticket_order", "o_y", "bob", "ticket_refund.approved", t.plusSeconds(10)));
        auditRepository.save(auditEvent("it_contract", "c_z", "alice", "contract.approved", t.plusSeconds(20)));

        // 跨对象按业务类型查询：两条 ticket_order，按时间倒序。
        assertThat(auditRepository.searchOperationEvents("it_ticket_order", null, null, null, null, null, 100))
                .extracting(AuditEvent::businessId).containsExactly("o_y", "o_x");
        // 按操作人 + 时间范围跨对象。
        assertThat(auditRepository.searchOperationEvents(null, null, "alice", null, t, t.plusSeconds(25), 100))
                .extracting(AuditEvent::businessId).containsExactlyInAnyOrder("o_x", "c_z");
        // 按事件名。
        assertThat(auditRepository.searchOperationEvents(null, null, null, "contract.approved", null, null, 100))
                .extracting(AuditEvent::businessId).containsExactly("c_z");
        // 按具体业务对象。
        assertThat(auditRepository.searchOperationEvents(null, "o_x", null, null, null, null, 100))
                .extracting(AuditEvent::actorId).containsExactly("alice");
    }

    @Test
    void v185SeedsAuditOperationLogPermissions() {
        assertThat(permissionCodes()).contains("audit.operation_log.view", "audit.operation_log.export");
    }

    private static AuditEvent auditEvent(String businessType, String businessId, String actorId,
            String eventName, Instant at) {
        return new AuditEvent(java.util.UUID.randomUUID(), at, eventName, "success", "staff", actorId, actorId,
                "web", null, businessType, businessId, "req_" + businessId, "idem_" + businessId,
                Map.of(), null, null, null);
    }

    @Test
    void migrationCreatesEvidenceEventTable() {
        assertThat(regclass("evidence_events")).isEqualTo("evidence_events");
        assertThat(regclass("evidence_attachments")).isEqualTo("evidence_attachments");
        assertThat(regclass("evidence_reviews")).isEqualTo("evidence_reviews");
        assertThat(permissionCodes()).contains(
                "evidence.attachment.create",
                "evidence.review.create",
                "evidence.timeline.view"
        );
    }

    @Test
    void evidenceEventListReturnsLatestEvents() {
        Instant occurred = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveAll(List.of(
                new EvidenceEventRecord(
                        "evt_old",
                        "P0-ANDROID-JDBC",
                        "staff_jdbc",
                        "terminal.login",
                        "android_terminal",
                        "success",
                        "login",
                        Map.of("mode", "online"),
                        occurred,
                        occurred.plusSeconds(1),
                        null
                ),
                new EvidenceEventRecord(
                        "evt_new",
                        "P0-ANDROID-JDBC",
                        "staff_jdbc",
                        "terminal.verification.online",
                        "android_terminal",
                        "success",
                        "verification",
                        Map.of("verification_no", "V1"),
                        occurred.plusSeconds(2),
                        occurred.plusSeconds(3),
                        null
                )
        ));

        assertThat(repository.listLatest(2, null))
                .extracting(EvidenceEventRecord::id, EvidenceEventRecord::eventType)
                .containsExactly(
                        tuple("evt_new", "terminal.verification.online"),
                        tuple("evt_old", "terminal.login")
                );
        assertThat(repository.listLatest(1, null))
                .singleElement()
                .satisfies(event -> assertThat(event.payload()).containsEntry("verification_no", "V1"));
    }

    @Test
    void attachmentReviewAndTimelinePersistFormalFields() {
        Instant now = Instant.parse("2026-06-14T08:00:00Z");
        repository.saveAll(List.of(new EvidenceEventRecord(
                "evt_timeline_jdbc",
                "P0-ANDROID-JDBC",
                "staff_jdbc",
                "terminal.exception",
                "android_terminal",
                "success",
                "exception evidence",
                Map.of("business_type", "contract", "business_id", "ctr_jdbc"),
                now,
                now.plusSeconds(1),
                null
        )));
        repository.saveAttachment(new EvidenceAttachmentRecord(
                "att_jdbc",
                "contract.pdf",
                "application/pdf",
                1024L,
                "contract",
                "ctr_jdbc",
                "contract",
                "evidence/contracts/ctr_jdbc.pdf",
                "https://object-storage.local/upload/att_jdbc",
                "uploaded",
                "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                now,
                now.plusSeconds(2),
                null
        ));
        repository.saveReview(new EvidenceReviewRecord(
                "rev_jdbc",
                "contract",
                "ctr_jdbc",
                "medium",
                "pass",
                "complete",
                List.of("att_jdbc"),
                "created",
                now.plusSeconds(3),
                now.plusSeconds(3),
                null
        ));

        assertThat(repository.findAttachmentById("att_jdbc"))
                .get()
                .satisfies(attachment -> {
                    assertThat(attachment.status()).isEqualTo("uploaded");
                    assertThat(attachment.checksum()).isEqualTo("sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
                });
        assertThat(repository.listTimeline("contract", "ctr_jdbc", null))
                .extracting(EvidenceTimelineItem::itemType)
                .contains("evidence_event", "attachment", "review");
    }

    private String regclass(String tableName) {
        return jdbcTemplate.queryForObject("SELECT to_regclass('public." + tableName + "')::text", String.class);
    }

    private List<String> permissionCodes() {
        return jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM permissions
                 ORDER BY permission_code
                """, String.class);
    }
}
