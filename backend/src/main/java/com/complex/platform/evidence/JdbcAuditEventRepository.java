package com.complex.platform.evidence;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcAuditEventRepository implements AuditEventRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcAuditEventRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public AuditEvent save(AuditEvent event) {
        String sql = """
                INSERT INTO audit_events (
                    id, occurred_at, event_name, event_result, actor_type, actor_id, auth_context,
                    terminal, permission_code, business_type, business_id, request_id, idempotency_key,
                    metadata, merchant_id, event_category, subject_type
                )
                VALUES (
                    :id, :occurred_at, :event_name, :event_result, :actor_type, :actor_id, :auth_context,
                    :terminal, :permission_code, :business_type, :business_id, :request_id, :idempotency_key,
                    CAST(:metadata AS jsonb), :merchant_id, :event_category, :subject_type
                )
                """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("id", event.id())
                .addValue("occurred_at", timestampWithTimeZone(event.occurredAt()))
                .addValue("event_name", event.eventName())
                .addValue("event_result", event.eventResult())
                .addValue("actor_type", event.actorType())
                .addValue("actor_id", event.actorId())
                .addValue("auth_context", event.authContext())
                .addValue("terminal", event.terminal())
                .addValue("permission_code", event.permissionCode())
                .addValue("business_type", event.businessType())
                .addValue("business_id", event.businessId())
                .addValue("request_id", event.requestId())
                .addValue("idempotency_key", event.idempotencyKey())
                .addValue("metadata", writeMetadata(event))
                .addValue("merchant_id", event.merchantId())
                .addValue("event_category", event.eventCategory())
                .addValue("subject_type", event.subjectType()));
        return event;
    }


    private String writeMetadata(AuditEvent event) {
        try {
            return objectMapper.writeValueAsString(event.metadata());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("audit metadata is not serializable", exception);
        }
    }

    @Override
    public java.util.List<AuditEvent> searchOperationEvents(String businessType, String businessId,
            String actorId, String eventName, java.time.Instant from, java.time.Instant to, int limit, String merchantScope) {
        return searchOperationEvents(businessType, businessId, actorId, eventName, from, to, limit, merchantScope, null);
    }

    @Override
    public java.util.List<AuditEvent> searchOperationEvents(String businessType, String businessId, String actorId, String eventName,
            java.time.Instant from, java.time.Instant to, int limit, String merchantScope, java.util.Set<String> projectScope) {
        if (projectScope != null && projectScope.isEmpty()) {
            return java.util.List.of();
        }
        StringBuilder sql = new StringBuilder("""
                SELECT id, occurred_at, event_name, event_result, actor_type, actor_id, auth_context,
                       terminal, permission_code, business_type, business_id, request_id, idempotency_key,
                       metadata::text AS metadata, merchant_id, event_category, subject_type
                  FROM audit_events
                 WHERE 1 = 1
                """);
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (businessType != null) { sql.append(" AND business_type = :business_type"); params.addValue("business_type", businessType); }
        if (businessId != null) { sql.append(" AND business_id = :business_id"); params.addValue("business_id", businessId); }
        if (actorId != null) { sql.append(" AND actor_id = :actor_id"); params.addValue("actor_id", actorId); }
        if (eventName != null) { sql.append(" AND event_name = :event_name"); params.addValue("event_name", eventName); }
        if (from != null) { sql.append(" AND occurred_at >= :from_ts"); params.addValue("from_ts", timestampWithTimeZone(from)); }
        if (to != null) { sql.append(" AND occurred_at <= :to_ts"); params.addValue("to_ts", timestampWithTimeZone(to)); }
        // 3.1：非平台角色（有商户 scope）只看本商户审计；平台角色 merchantScope=null 不加谓词看全部。
        if (merchantScope != null) { sql.append(" AND merchant_id = :merchant_scope"); params.addValue("merchant_scope", merchantScope); }
        // 第十八批 BA 段第 324 条 324-R1：audit_events 没有项目列、merchant_id 取自操作人（V045）——按操作人或其商户所属项目归到综合体。
        // 18BA 收尾复核修正：商户所属项目＝主项目 UNION 已批准的经营项目（merchant_projects），与 listMerchantOperatingProjectIds 同一口径。
        if (projectScope != null) {
            sql.append(" AND (merchant_id IN (SELECT id FROM merchants WHERE project_id IN (:project_scope)"
                    + " UNION SELECT merchant_id FROM merchant_projects WHERE project_id IN (:project_scope))"
                    + " OR actor_id IN (SELECT id FROM access_users WHERE organization_id IN (:project_scope)))");
            params.addValue("project_scope", projectScope);
        }
        sql.append(" ORDER BY occurred_at DESC, id LIMIT :limit");
        params.addValue("limit", Math.max(1, Math.min(limit, 1000)));
        return jdbcTemplate.query(sql.toString(), params, this::mapAuditRow);
    }

    private AuditEvent mapAuditRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new AuditEvent(
                (java.util.UUID) rs.getObject("id"),
                rs.getTimestamp("occurred_at").toInstant(),
                rs.getString("event_name"), rs.getString("event_result"),
                rs.getString("actor_type"), rs.getString("actor_id"), rs.getString("auth_context"),
                rs.getString("terminal"), rs.getString("permission_code"),
                rs.getString("business_type"), rs.getString("business_id"),
                rs.getString("request_id"), rs.getString("idempotency_key"),
                readMetadataMap(rs.getString("metadata")),
                rs.getString("merchant_id"), rs.getString("event_category"), rs.getString("subject_type"));
    }

    private java.util.Map<String, Object> readMetadataMap(String json) {
        if (json == null || json.isBlank()) { return java.util.Map.of(); }
        try {
            return objectMapper.readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
        } catch (JsonProcessingException exception) {
            return java.util.Map.of();
        }
    }
}
