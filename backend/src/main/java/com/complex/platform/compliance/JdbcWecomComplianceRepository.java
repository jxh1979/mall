package com.complex.platform.compliance;
import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcWecomComplianceRepository implements WecomComplianceRepository {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcWecomComplianceRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public WecomRetentionPolicyRecord saveRetentionPolicy(WecomRetentionPolicyRecord record) {
        jdbcTemplate.update("""
                INSERT INTO wecom_retention_policies (
                    id, scope_type, scope_id, retention_days, expiry_action, enabled, created_at, updated_at
                )
                VALUES (
                    :id, :scope_type, :scope_id, :retention_days, :expiry_action, :enabled, :created_at, :updated_at
                )
                ON CONFLICT (scope_type, scope_id) DO UPDATE SET
                    retention_days = EXCLUDED.retention_days,
                    expiry_action = EXCLUDED.expiry_action,
                    enabled = EXCLUDED.enabled,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("scope_type", record.scopeType())
                .addValue("scope_id", record.scopeId())
                .addValue("retention_days", record.retentionDays())
                .addValue("expiry_action", record.expiryAction())
                .addValue("enabled", record.enabled())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt())));
        return record;
    }

    @Override
    public List<WecomRetentionPolicyRecord> findAllRetentionPolicies() {
        return jdbcTemplate.query("""
                SELECT id, scope_type, scope_id, retention_days, expiry_action, enabled, created_at, updated_at
                  FROM wecom_retention_policies
                 ORDER BY scope_type, scope_id
                """, this::mapPolicy);
    }

    @Override
    public Optional<WecomRetentionPolicyRecord> findEffectivePolicy(String projectId) {
        // 优先 project 精确命中（enabled），否则回退全局默认（enabled）。最具体生效。
        List<WecomRetentionPolicyRecord> rows = jdbcTemplate.query("""
                SELECT id, scope_type, scope_id, retention_days, expiry_action, enabled, created_at, updated_at
                  FROM wecom_retention_policies
                 WHERE enabled = true
                   AND (
                        (scope_type = 'project' AND scope_id = :project_id)
                     OR (scope_type = 'global')
                   )
                 ORDER BY CASE WHEN scope_type = 'project' THEN 0 ELSE 1 END
                """, Map.of("project_id", projectId == null ? "" : projectId), this::mapPolicy);
        return rows.stream().findFirst();
    }

    @Override
    public WecomConversationRecord saveConversation(WecomConversationRecord record) {
        jdbcTemplate.update("""
                INSERT INTO wecom_conversations (
                    id, external_conversation_id, project_id, owner_staff_id, customer_ref,
                    archive_status, retention_until, created_at, updated_at
                )
                VALUES (
                    :id, :external_conversation_id, :project_id, :owner_staff_id, :customer_ref,
                    :archive_status, :retention_until, :created_at, :updated_at
                )
                ON CONFLICT (external_conversation_id) DO UPDATE SET
                    owner_staff_id = EXCLUDED.owner_staff_id,
                    customer_ref = EXCLUDED.customer_ref,
                    retention_until = EXCLUDED.retention_until,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("external_conversation_id", record.externalConversationId())
                .addValue("project_id", record.projectId())
                .addValue("owner_staff_id", record.ownerStaffId())
                .addValue("customer_ref", record.customerRef())
                .addValue("archive_status", record.archiveStatus())
                .addValue("retention_until", record.retentionUntil())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt())));
        return record;
    }

    @Override
    public Optional<WecomConversationRecord> findConversationByExternalId(String externalConversationId) {
        return jdbcTemplate.query("""
                SELECT id, external_conversation_id, project_id, owner_staff_id, customer_ref,
                       archive_status, retention_until, created_at, updated_at
                  FROM wecom_conversations
                 WHERE external_conversation_id = :external_conversation_id
                """, Map.of("external_conversation_id", externalConversationId), this::mapConversation)
                .stream().findFirst();
    }

    @Override
    public Optional<WecomConversationRecord> findConversationById(String id) {
        return jdbcTemplate.query("""
                SELECT id, external_conversation_id, project_id, owner_staff_id, customer_ref,
                       archive_status, retention_until, created_at, updated_at
                  FROM wecom_conversations
                 WHERE id = :id
                """, Map.of("id", id), this::mapConversation).stream().findFirst();
    }

    @Override
    public List<WecomConversationRecord> findConversationsDueForRetention(LocalDate today) {
        return jdbcTemplate.query("""
                SELECT id, external_conversation_id, project_id, owner_staff_id, customer_ref,
                       archive_status, retention_until, created_at, updated_at
                  FROM wecom_conversations
                 WHERE archive_status = 'active'
                   AND retention_until IS NOT NULL
                   AND retention_until < :today
                 ORDER BY retention_until, id
                """, Map.of("today", today), this::mapConversation);
    }

    @Override
    public void updateConversationArchiveStatus(String id, String archiveStatus) {
        jdbcTemplate.update("""
                UPDATE wecom_conversations
                   SET archive_status = :archive_status, updated_at = now()
                 WHERE id = :id
                """, Map.of("id", id, "archive_status", archiveStatus));
    }

    @Override
    public WecomMessageRecord saveMessage(WecomMessageRecord record) {
        jdbcTemplate.update("""
                INSERT INTO wecom_messages (
                    id, conversation_id, sender_ref, content_raw_ref, content_redacted,
                    risk_flags, anonymized, sent_at, business_date, created_at,
                    ai_qc_status, ai_qc_result
                )
                VALUES (
                    :id, :conversation_id, :sender_ref, :content_raw_ref, :content_redacted,
                    CAST(:risk_flags AS jsonb), :anonymized, :sent_at, :business_date, :created_at,
                    :ai_qc_status, CAST(:ai_qc_result AS jsonb)
                )
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("conversation_id", record.conversationId())
                .addValue("sender_ref", record.senderRef())
                .addValue("content_raw_ref", record.contentRawRef())
                .addValue("content_redacted", record.contentRedacted())
                .addValue("risk_flags", writeJson(record.riskFlags()))
                .addValue("anonymized", record.anonymized())
                .addValue("sent_at", timestampWithTimeZone(record.sentAt()))
                .addValue("business_date", record.businessDate())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("ai_qc_status", record.aiQcStatus() == null ? "skipped" : record.aiQcStatus())
                .addValue("ai_qc_result", record.aiQcResult()));
        return record;
    }

    @Override
    public List<WecomMessageRecord> findMessagesByConversation(String conversationId) {
        return jdbcTemplate.query("""
                SELECT id, conversation_id, sender_ref, content_raw_ref, content_redacted,
                       risk_flags, anonymized, sent_at, business_date, created_at,
                       ai_qc_status, ai_qc_result
                  FROM wecom_messages
                 WHERE conversation_id = :conversation_id
                 ORDER BY sent_at, id
                """, Map.of("conversation_id", conversationId), this::mapMessage);
    }

    @Override
    public void anonymizeMessagesByConversation(String conversationId) {
        // 匿名化：清原文引用，展示内容不可逆化（占位），置 anonymized=true。
        jdbcTemplate.update("""
                UPDATE wecom_messages
                   SET content_raw_ref = NULL,
                       content_redacted = '[ANONYMIZED]',
                       anonymized = true,
                       ai_qc_result = NULL
                 WHERE conversation_id = :conversation_id
                """, Map.of("conversation_id", conversationId));
    }

    @Override
    public void deleteMessagesByConversation(String conversationId) {
        jdbcTemplate.update("""
                DELETE FROM wecom_messages WHERE conversation_id = :conversation_id
                """, Map.of("conversation_id", conversationId));
    }

    @Override
    public WecomAccessAuditRecord saveAccessAudit(WecomAccessAuditRecord record) {
        jdbcTemplate.update("""
                INSERT INTO wecom_access_audits (
                    id, conversation_id, viewer_user_id, view_type, approval_ref, viewed_at
                )
                VALUES (
                    :id, :conversation_id, :viewer_user_id, :view_type, :approval_ref, :viewed_at
                )
                """, new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("conversation_id", record.conversationId())
                .addValue("viewer_user_id", record.viewerUserId())
                .addValue("view_type", record.viewType())
                .addValue("approval_ref", record.approvalRef())
                .addValue("viewed_at", timestampWithTimeZone(record.viewedAt())));
        return record;
    }

    @Override
    public List<WecomAccessAuditRecord> findAccessAuditsByConversation(String conversationId) {
        return jdbcTemplate.query("""
                SELECT id, conversation_id, viewer_user_id, view_type, approval_ref, viewed_at
                  FROM wecom_access_audits
                 WHERE conversation_id = :conversation_id
                 ORDER BY viewed_at DESC, id
                """, Map.of("conversation_id", conversationId), this::mapAudit);
    }

    private WecomRetentionPolicyRecord mapPolicy(ResultSet rs, int rowNum) throws SQLException {
        return new WecomRetentionPolicyRecord(
                rs.getString("id"),
                rs.getString("scope_type"),
                rs.getString("scope_id"),
                rs.getInt("retention_days"),
                rs.getString("expiry_action"),
                rs.getBoolean("enabled"),
                nullableInstant(rs, "created_at"),
                nullableInstant(rs, "updated_at")
        );
    }

    private WecomConversationRecord mapConversation(ResultSet rs, int rowNum) throws SQLException {
        return new WecomConversationRecord(
                rs.getString("id"),
                rs.getString("external_conversation_id"),
                rs.getString("project_id"),
                rs.getString("owner_staff_id"),
                rs.getString("customer_ref"),
                rs.getString("archive_status"),
                rs.getObject("retention_until", LocalDate.class),
                nullableInstant(rs, "created_at"),
                nullableInstant(rs, "updated_at")
        );
    }

    private WecomMessageRecord mapMessage(ResultSet rs, int rowNum) throws SQLException {
        return new WecomMessageRecord(
                rs.getString("id"),
                rs.getString("conversation_id"),
                rs.getString("sender_ref"),
                rs.getString("content_raw_ref"),
                rs.getString("content_redacted"),
                readStringList(rs.getString("risk_flags")),
                rs.getBoolean("anonymized"),
                nullableInstant(rs, "sent_at"),
                rs.getObject("business_date", LocalDate.class),
                nullableInstant(rs, "created_at"),
                rs.getString("ai_qc_status"),
                rs.getString("ai_qc_result")
        );
    }

    private WecomAccessAuditRecord mapAudit(ResultSet rs, int rowNum) throws SQLException {
        return new WecomAccessAuditRecord(
                rs.getString("id"),
                rs.getString("conversation_id"),
                rs.getString("viewer_user_id"),
                rs.getString("view_type"),
                rs.getString("approval_ref"),
                nullableInstant(rs, "viewed_at")
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("json field is not serializable", exception);
        }
    }

    private List<String> readStringList(String value) {
        try {
            return value == null ? List.of() : objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("json string list is not readable", exception);
        }
    }
}
