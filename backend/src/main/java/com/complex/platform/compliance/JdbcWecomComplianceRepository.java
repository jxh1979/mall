// 文件版本：V0.01
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

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcWecomComplianceRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public boolean projectExists(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            return false;
        }
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM projects WHERE id = :id)",
                Map.of("id", projectId), Boolean.class);
        return Boolean.TRUE.equals(exists);
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

    // P2 多综合体 project_id scoped 读通道：按真实 project_id 列做行级隔离（AC-ISO-1，§4）。既有查询不变。
    public List<WecomConversationRecord> findConversationsByProject(String projectId) {
        return jdbcTemplate.query("""
                SELECT id, external_conversation_id, project_id, owner_staff_id, customer_ref,
                       archive_status, retention_until, created_at, updated_at
                  FROM wecom_conversations
                 WHERE project_id = :project_id
                 ORDER BY created_at DESC, id
                """, Map.of("project_id", projectId), this::mapConversation);
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

    // ------------------------------------------------------------------- P0#8 企微违规规则码 / 命中

    @Override
    public List<WecomRiskRuleRecord> findEnabledWecomRiskRules() {
        // 企微违规规则码 = analytics_risk_rules 中 domain=compliance 且启用；关键词取自 rule_config_json.keywords。
        return jdbcTemplate.query("""
                SELECT rule_id, rule_code, risk_level, rule_config_json
                  FROM analytics_risk_rules
                 WHERE enabled = true
                   AND domain = 'compliance'
                 ORDER BY rule_code
                """, (rs, rowNum) -> new WecomRiskRuleRecord(
                rs.getString("rule_id"),
                rs.getString("rule_code"),
                rs.getString("risk_level"),
                readKeywords(rs.getString("rule_config_json"))));
    }

    @Override
    public WecomRiskHitRecord saveRiskHit(WecomRiskHitRecord record) {
        // 同一消息同一规则码幂等（uq_wecom_risk_hits_message_rule）：重复同步不产生重复命中。
        jdbcTemplate.update("""
                INSERT INTO wecom_risk_hits (
                    hit_id, conversation_id, message_id, rule_code, risk_level, matched_snippet,
                    status, reviewer_id, review_note, project_id, alert_id, created_at, updated_at
                )
                VALUES (
                    :hit_id, :conversation_id, :message_id, :rule_code, :risk_level, :matched_snippet,
                    :status, :reviewer_id, :review_note, :project_id, :alert_id, :created_at, :updated_at
                )
                ON CONFLICT (message_id, rule_code) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("hit_id", record.hitId())
                .addValue("conversation_id", record.conversationId())
                .addValue("message_id", record.messageId())
                .addValue("rule_code", record.ruleCode())
                .addValue("risk_level", record.riskLevel())
                .addValue("matched_snippet", record.matchedSnippet())
                .addValue("status", record.status())
                .addValue("reviewer_id", record.reviewerId())
                .addValue("review_note", record.reviewNote())
                .addValue("project_id", record.projectId())
                .addValue("alert_id", record.alertId())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt())));
        return record;
    }

    @Override
    public List<WecomRiskHitRecord> findRiskHits(String status, String conversationId) {
        return jdbcTemplate.query("""
                SELECT hit_id, conversation_id, message_id, rule_code, risk_level, matched_snippet,
                       status, reviewer_id, review_note, project_id, alert_id, created_at, updated_at
                  FROM wecom_risk_hits
                 WHERE (CAST(:status AS text) IS NULL OR status = :status)
                   AND (CAST(:conversation_id AS text) IS NULL OR conversation_id = :conversation_id)
                 ORDER BY created_at DESC, hit_id
                """, new MapSqlParameterSource()
                .addValue("status", status)
                .addValue("conversation_id", conversationId), this::mapRiskHit);
    }

    @Override
    public Optional<WecomRiskHitRecord> findRiskHitById(String hitId) {
        return jdbcTemplate.query("""
                SELECT hit_id, conversation_id, message_id, rule_code, risk_level, matched_snippet,
                       status, reviewer_id, review_note, project_id, alert_id, created_at, updated_at
                  FROM wecom_risk_hits
                 WHERE hit_id = :hit_id
                """, Map.of("hit_id", hitId), this::mapRiskHit).stream().findFirst();
    }

    // P2 多综合体 project_id scoped 读通道：按真实 project_id 列做行级隔离（AC-ISO-1，§4）。既有查询不变。
    public List<WecomRiskHitRecord> findRiskHitsByProject(String projectId) {
        return jdbcTemplate.query("""
                SELECT hit_id, conversation_id, message_id, rule_code, risk_level, matched_snippet,
                       status, reviewer_id, review_note, project_id, alert_id, created_at, updated_at
                  FROM wecom_risk_hits
                 WHERE project_id = :project_id
                 ORDER BY created_at DESC, hit_id
                """, Map.of("project_id", projectId), this::mapRiskHit);
    }

    @Override
    public WecomRiskHitRecord updateRiskHitReview(
            String hitId, String status, String reviewerId, String reviewNote) {
        jdbcTemplate.update("""
                UPDATE wecom_risk_hits
                   SET status = :status,
                       reviewer_id = :reviewer_id,
                       review_note = :review_note,
                       updated_at = now()
                 WHERE hit_id = :hit_id
                """, new MapSqlParameterSource()
                .addValue("hit_id", hitId)
                .addValue("status", status)
                .addValue("reviewer_id", reviewerId)
                .addValue("review_note", reviewNote));
        return findRiskHitById(hitId).orElse(null);
    }

    @Override
    public String enqueueRiskAlert(WecomRiskAlertEnqueue enqueue) {
        // 命中入复核队列：写孤岛 analytics_risk_alerts，标 source_domain='wecom'（V106 新增列）。
        // idempotency_key（=hit_id 派生）幂等：重复同步不产生重复告警，命中已落库即复用既有 alert_id。
        jdbcTemplate.update("""
                INSERT INTO analytics_risk_alerts (
                    id, rule_id, alert_type, risk_level, object_type, object_id, summary,
                    status, operator_note, evidence_attachment_ids_json, idempotency_key,
                    source_domain, created_at, resolved_at, updated_at
                )
                VALUES (
                    :id, :rule_id, 'wecom_risk_hit', :risk_level, :object_type, :object_id, :summary,
                    'open', NULL, '[]'::jsonb, :idempotency_key,
                    'wecom', :created_at, NULL, :created_at
                )
                ON CONFLICT (idempotency_key) WHERE idempotency_key IS NOT NULL DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", enqueue.alertId())
                .addValue("rule_id", enqueue.ruleId())
                .addValue("risk_level", enqueue.riskLevel())
                .addValue("object_type", enqueue.objectType())
                .addValue("object_id", enqueue.objectId())
                .addValue("summary", enqueue.summary())
                .addValue("idempotency_key", enqueue.idempotencyKey())
                .addValue("created_at", timestampWithTimeZone(enqueue.createdAt())));
        // 冲突时（已存在同 idempotency_key）返回既有 alert id，保证命中关联稳定。
        return jdbcTemplate.query("""
                SELECT id FROM analytics_risk_alerts WHERE idempotency_key = :idempotency_key
                """, Map.of("idempotency_key", enqueue.idempotencyKey()),
                (rs, rowNum) -> rs.getString("id")).stream().findFirst().orElse(enqueue.alertId());
    }

    private WecomRiskHitRecord mapRiskHit(ResultSet rs, int rowNum) throws SQLException {
        return new WecomRiskHitRecord(
                rs.getString("hit_id"),
                rs.getString("conversation_id"),
                rs.getString("message_id"),
                rs.getString("rule_code"),
                rs.getString("risk_level"),
                rs.getString("matched_snippet"),
                rs.getString("status"),
                rs.getString("reviewer_id"),
                rs.getString("review_note"),
                rs.getString("project_id"),
                rs.getString("alert_id"),
                nullableInstant(rs, "created_at"),
                nullableInstant(rs, "updated_at")
        );
    }

    private List<String> readKeywords(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return List.of();
        }
        try {
            Map<String, Object> config = objectMapper.readValue(configJson, MAP_TYPE);
            Object keywords = config.get("keywords");
            if (keywords instanceof List<?> list) {
                return list.stream().filter(java.util.Objects::nonNull).map(Object::toString).toList();
            }
            return List.of();
        } catch (JsonProcessingException exception) {
            // AUD-020：库内坏 JSON 是服务端数据问题，须走 500 并记堆栈（IllegalArgumentException 会被全局映射 400 掩盖）。
            throw new IllegalStateException("rule_config_json is not readable", exception);
        }
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


    // AUD-020：同 readKeywords——序列化失败/库内坏 JSON 走 500 并记堆栈。
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("json field is not serializable", exception);
        }
    }

    private List<String> readStringList(String value) {
        try {
            return value == null ? List.of() : objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("json string list is not readable", exception);
        }
    }
}
