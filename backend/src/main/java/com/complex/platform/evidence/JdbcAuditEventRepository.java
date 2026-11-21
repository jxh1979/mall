package com.complex.platform.evidence;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcAuditEventRepository implements AuditEventRepository {

    /**
     * 第十八批 BT 段第 370 条 370-R1：启用中的综合体全部落在 :project_scope 内（启用口径同 BusinessDayResolver.soleProject：status = 'active'）。
     * 归不到任何项目的留痕与不带商户的证据事件只对这样的读者出现，JdbcEvidenceEventRepository.listPage 共用。
     * ponytail: 启用综合体不止一个时，这几类只对能看全部启用综合体的账号出现；多综合体启用前给两表加所属综合体列，客人操作可取 TerminalSwitchGuard.checkAtProject 已判出的综合体。
     */
    static final String ALL_ACTIVE_PROJECTS_IN_SCOPE =
            "NOT EXISTS (SELECT 1 FROM projects WHERE status = 'active' AND id NOT IN (:project_scope))";

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

    /** 内部取数（退卡重放核对、对象时间线、复核意见等）：不加商户级卡过滤。 */
    @Override
    public java.util.List<AuditEvent> searchOperationEvents(String businessType, String businessId,
            String actorId, String eventName, java.time.Instant from, java.time.Instant to, int limit, String merchantScope) {
        return search(businessType, businessId, actorId, eventName, from, to, limit, merchantScope, null, false);
    }

    /**
     * 操作留痕查询与导出（AuditController 经 AuditEventService 调用）。第十八批 CL 段 421-R4（第十八批裁决 79，BSM:833「平台各岗位（含最高权限）一律
     * 看不到商户级卡的卡号、持卡人、余额与充值、退款流水」）：不挂商户的读者（merchantScope 为空：平台管理员、最高权限、项目侧）不列以商户级卡为对象
     * 或以商户级卡充值意向为对象的事件；商户账号照旧按 merchant_id。
     */
    @Override
    public java.util.List<AuditEvent> searchOperationEvents(String businessType, String businessId, String actorId, String eventName,
            java.time.Instant from, java.time.Instant to, int limit, String merchantScope, java.util.Set<String> projectScope) {
        return search(businessType, businessId, actorId, eventName, from, to, limit, merchantScope, projectScope, merchantScope == null);
    }

    private java.util.List<AuditEvent> search(String businessType, String businessId, String actorId, String eventName,
            java.time.Instant from, java.time.Instant to, int limit, String merchantScope, java.util.Set<String> projectScope,
            boolean hideMerchantCardEvents) {
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
        // 第十八批 BT 段第 370 条 370-R1：不带商户、操作人也不是任何项目员工账号的留痕（客人、系统作业、平台管理员与最高权限等）归不到项目，
        // 启用中的综合体都可触及时一并可见；已停用项目的员工所做操作仍归该项目（JOIN projects 不看状态）。
        if (projectScope != null) {
            sql.append(" AND (merchant_id IN (SELECT id FROM merchants WHERE project_id IN (:project_scope)"
                    + " UNION SELECT merchant_id FROM merchant_projects WHERE project_id IN (:project_scope))"
                    + " OR actor_id IN (SELECT id FROM access_users WHERE organization_id IN (:project_scope))"
                    + " OR (merchant_id IS NULL AND (actor_id IS NULL OR actor_id NOT IN"
                    + " (SELECT u.id FROM access_users u JOIN projects p ON p.id = u.organization_id))"
                    + " AND " + ALL_ACTIVE_PROJECTS_IN_SCOPE + "))");
            params.addValue("project_scope", projectScope);
        }
        if (hideMerchantCardEvents) {
            sql.append(" AND NOT EXISTS (SELECT 1 FROM customer_stored_value_accounts a WHERE a.merchant_id IS NOT NULL AND a.id = audit_events.business_id)"
                    + " AND NOT EXISTS (SELECT 1 FROM stored_value_topup_intents i WHERE i.merchant_id IS NOT NULL"
                    + " AND i.payment_intent_id = audit_events.business_id)");
            // 18CL 收尾复核：商户账号在冻结、解冻、线上与柜台退卡写口失败的留痕以客人编号为对象（LoyaltyController 的 recordingFailure），
            // 421-R1 后商户账号只办本商户卡，一并不列；商户账号的成功留痕以账户为对象（如酒吧结账扣通用卡），按上两条判，通用卡的照列。
            sql.append(" AND (business_type IS DISTINCT FROM 'customer_stored_value' OR event_result IS DISTINCT FROM 'failure'"
                    + " OR merchant_id IS NULL)");
        }
        // 第十八批 CM 段第 372 条第 2 段 372-S2-R1（PGM:682；第十八批裁决 77）：外审数据时段按留痕发生时刻，与 from、to 同时生效。
        sql.append(com.complex.platform.access.AuditDataWindow.andInstant("occurred_at"));
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
