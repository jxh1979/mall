package com.complex.platform.amusement;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcAmusementSafetyRepository implements AmusementSafetyRepository {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcAmusementSafetyRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public List<AmusementSafetyNoticeRecord> findAllSafetyNotices() {
        return jdbcTemplate.query("""
                SELECT id, project_id, amusement_project_id, notice_type, title, content_version,
                       content_text, status, effective_from, effective_to, created_by_user_id,
                       evidence_attachment_ids, created_at, updated_at, merchant_id
                  FROM amusement_safety_notices
                 ORDER BY project_id, title, id
                """, this::mapNotice);
    }

    @Override
    public List<AmusementSafetyNoticeRecord> findSafetyNoticesByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, amusement_project_id, notice_type, title, content_version,
                       content_text, status, effective_from, effective_to, created_by_user_id,
                       evidence_attachment_ids, created_at, updated_at, merchant_id
                  FROM amusement_safety_notices
                 WHERE merchant_id = :merchant_id
                 ORDER BY project_id, title, id
                """, Map.of("merchant_id", merchantId), this::mapNotice);
    }

    // P2 多综合体 project_id scoped 读通道：按真实 project_id 列做行级隔离（AC-ISO-1，§4）。既有查询不变。
    public List<AmusementSafetyNoticeRecord> findSafetyNoticesByProject(String projectId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, amusement_project_id, notice_type, title, content_version,
                       content_text, status, effective_from, effective_to, created_by_user_id,
                       evidence_attachment_ids, created_at, updated_at, merchant_id
                  FROM amusement_safety_notices
                 WHERE project_id = :project_id
                 ORDER BY title, id
                """, Map.of("project_id", projectId), this::mapNotice);
    }

    @Override
    public Optional<AmusementSafetyNoticeRecord> findSafetyNoticeById(String id) {
        return jdbcTemplate.query("""
                SELECT id, project_id, amusement_project_id, notice_type, title, content_version,
                       content_text, status, effective_from, effective_to, created_by_user_id,
                       evidence_attachment_ids, created_at, updated_at, merchant_id
                  FROM amusement_safety_notices
                 WHERE id = :id
                """, Map.of("id", id), this::mapNotice).stream().findFirst();
    }

    @Override
    public AmusementSafetyNoticeRecord saveSafetyNotice(AmusementSafetyNoticeRecord record) {
        jdbcTemplate.update("""
                INSERT INTO amusement_safety_notices (
                    id, project_id, amusement_project_id, notice_type, title, content_version,
                    content_text, status, effective_from, effective_to, created_by_user_id,
                    evidence_attachment_ids, created_at, updated_at, merchant_id
                )
                VALUES (
                    :id, :project_id, :amusement_project_id, :notice_type, :title, :content_version,
                    :content_text, :status, :effective_from, :effective_to, :created_by_user_id,
                    CAST(:evidence_attachment_ids AS jsonb), :created_at, :updated_at, :merchant_id
                )
                ON CONFLICT (id) DO NOTHING
                """, noticeParameters(record));
        return record;
    }

    @Override
    public AmusementWaiverAcknowledgementRecord saveWaiverAcknowledgement(AmusementWaiverAcknowledgementRecord record) {
        jdbcTemplate.update("""
                INSERT INTO amusement_waiver_acknowledgements (
                    id, notice_id, project_id, amusement_project_id, session_id, customer_id,
                    visitor_name, signer_name, acknowledgement_method, content_version, content_hash,
                    acknowledged_at, operator_user_id, evidence_attachment_ids, created_at
                )
                VALUES (
                    :id, :notice_id, :project_id, :amusement_project_id, :session_id, :customer_id,
                    :visitor_name, :signer_name, :acknowledgement_method, :content_version, :content_hash,
                    :acknowledged_at, :operator_user_id, CAST(:evidence_attachment_ids AS jsonb), :created_at
                )
                ON CONFLICT (id) DO NOTHING
                """, acknowledgementParameters(record));
        return record;
    }

    @Override
    public List<AmusementIncidentRecord> findAllIncidents() {
        return jdbcTemplate.query("""
                SELECT id, project_id, amusement_project_id, session_id, device_id, customer_id,
                       severity, incident_type, status, title, description, reported_by_user_id, reported_channel,
                       occurred_at, resolution, closed_at, evidence_attachment_ids, created_at, updated_at, merchant_id
                  FROM amusement_incidents
                 ORDER BY occurred_at DESC, id
                """, this::mapIncident);
    }

    @Override
    public List<AmusementIncidentRecord> findIncidentsByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, amusement_project_id, session_id, device_id, customer_id,
                       severity, incident_type, status, title, description, reported_by_user_id, reported_channel,
                       occurred_at, resolution, closed_at, evidence_attachment_ids, created_at, updated_at, merchant_id
                  FROM amusement_incidents
                 WHERE merchant_id = :merchant_id
                 ORDER BY occurred_at DESC, id
                """, Map.of("merchant_id", merchantId), this::mapIncident);
    }

    // P2 多综合体 project_id scoped 读通道：按真实 project_id 列做行级隔离（AC-ISO-1，§4）。既有查询不变。
    public List<AmusementIncidentRecord> findIncidentsByProject(String projectId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, amusement_project_id, session_id, device_id, customer_id,
                       severity, incident_type, status, title, description, reported_by_user_id, reported_channel,
                       occurred_at, resolution, closed_at, evidence_attachment_ids, created_at, updated_at, merchant_id
                  FROM amusement_incidents
                 WHERE project_id = :project_id
                 ORDER BY occurred_at DESC, id
                """, Map.of("project_id", projectId), this::mapIncident);
    }

    @Override
    public Optional<AmusementIncidentRecord> findIncidentById(String id) {
        return jdbcTemplate.query("""
                SELECT id, project_id, amusement_project_id, session_id, device_id, customer_id,
                       severity, incident_type, status, title, description, reported_by_user_id, reported_channel,
                       occurred_at, resolution, closed_at, evidence_attachment_ids, created_at, updated_at, merchant_id
                  FROM amusement_incidents
                 WHERE id = :id
                """, Map.of("id", id), this::mapIncident).stream().findFirst();
    }

    @Override
    public AmusementIncidentRecord saveIncident(AmusementIncidentRecord record) {
        jdbcTemplate.update("""
                INSERT INTO amusement_incidents (
                    id, project_id, amusement_project_id, session_id, device_id, customer_id,
                    severity, incident_type, status, title, description, reported_by_user_id, reported_channel,
                    occurred_at, resolution, closed_at, evidence_attachment_ids, created_at, updated_at, merchant_id
                )
                VALUES (
                    :id, :project_id, :amusement_project_id, :session_id, :device_id, :customer_id,
                    :severity, :incident_type, :status, :title, :description, :reported_by_user_id, :reported_channel,
                    :occurred_at, :resolution, :closed_at, CAST(:evidence_attachment_ids AS jsonb), :created_at, :updated_at, :merchant_id
                )
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    resolution = EXCLUDED.resolution,
                    closed_at = EXCLUDED.closed_at,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    updated_at = EXCLUDED.updated_at
                """, incidentParameters(record));
        return record;
    }

    @Override
    public AmusementIncidentActionRecord saveIncidentAction(AmusementIncidentActionRecord record) {
        jdbcTemplate.update("""
                INSERT INTO amusement_incident_actions (
                    id, incident_id, action_type, from_status, to_status, operator_user_id,
                    operator_note, evidence_attachment_ids, created_at
                )
                VALUES (
                    :id, :incident_id, :action_type, :from_status, :to_status, :operator_user_id,
                    :operator_note, CAST(:evidence_attachment_ids AS jsonb), :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    incident_id = EXCLUDED.incident_id,
                    action_type = EXCLUDED.action_type,
                    from_status = EXCLUDED.from_status,
                    to_status = EXCLUDED.to_status,
                    operator_user_id = EXCLUDED.operator_user_id,
                    operator_note = EXCLUDED.operator_note,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids
                """, actionParameters(record));
        return record;
    }

    @Override
    public List<AmusementIncidentActionRecord> findIncidentActionsByIncidentId(String incidentId) {
        return jdbcTemplate.query("""
                SELECT id, incident_id, action_type, from_status, to_status, operator_user_id,
                       operator_note, evidence_attachment_ids, created_at
                  FROM amusement_incident_actions
                 WHERE incident_id = :incident_id
                 ORDER BY created_at ASC, id
                """, Map.of("incident_id", incidentId), this::mapAction);
    }

    private MapSqlParameterSource noticeParameters(AmusementSafetyNoticeRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("project_id", record.projectId())
                .addValue("amusement_project_id", record.amusementProjectId())
                .addValue("notice_type", record.noticeType())
                .addValue("title", record.title())
                .addValue("content_version", record.contentVersion())
                .addValue("content_text", record.contentText())
                .addValue("status", record.status())
                .addValue("effective_from", timestampWithTimeZone(record.effectiveFrom()))
                .addValue("effective_to", timestampWithTimeZone(record.effectiveTo()))
                .addValue("created_by_user_id", record.createdByUserId())
                .addValue("evidence_attachment_ids", writeJson(record.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt()))
                .addValue("merchant_id", record.merchantId());
    }

    private MapSqlParameterSource acknowledgementParameters(AmusementWaiverAcknowledgementRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("notice_id", record.noticeId())
                .addValue("project_id", record.projectId())
                .addValue("amusement_project_id", record.amusementProjectId())
                .addValue("session_id", record.sessionId())
                .addValue("customer_id", record.customerId())
                .addValue("visitor_name", record.visitorName())
                .addValue("signer_name", record.signerName())
                .addValue("acknowledgement_method", record.acknowledgementMethod())
                .addValue("content_version", record.contentVersion())
                .addValue("content_hash", record.contentHash())
                .addValue("acknowledged_at", timestampWithTimeZone(record.acknowledgedAt()))
                .addValue("operator_user_id", record.operatorUserId())
                .addValue("evidence_attachment_ids", writeJson(record.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(record.createdAt()));
    }

    private MapSqlParameterSource incidentParameters(AmusementIncidentRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("project_id", record.projectId())
                .addValue("amusement_project_id", record.amusementProjectId())
                .addValue("session_id", record.sessionId())
                .addValue("device_id", record.deviceId())
                .addValue("customer_id", record.customerId())
                .addValue("severity", record.severity())
                .addValue("incident_type", record.incidentType())
                .addValue("status", record.status())
                .addValue("title", record.title())
                .addValue("description", record.description())
                .addValue("reported_by_user_id", record.reportedByUserId())
                .addValue("reported_channel", record.reportedChannel())
                .addValue("occurred_at", timestampWithTimeZone(record.occurredAt()))
                .addValue("resolution", record.resolution())
                .addValue("closed_at", timestampWithTimeZone(record.closedAt()))
                .addValue("evidence_attachment_ids", writeJson(record.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt()))
                .addValue("merchant_id", record.merchantId());
    }

    private MapSqlParameterSource actionParameters(AmusementIncidentActionRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("incident_id", record.incidentId())
                .addValue("action_type", record.actionType())
                .addValue("from_status", record.fromStatus())
                .addValue("to_status", record.toStatus())
                .addValue("operator_user_id", record.operatorUserId())
                .addValue("operator_note", record.operatorNote())
                .addValue("evidence_attachment_ids", writeJson(record.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(record.createdAt()));
    }

    private AmusementSafetyNoticeRecord mapNotice(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AmusementSafetyNoticeRecord(
                resultSet.getString("id"),
                resultSet.getString("project_id"),
                resultSet.getString("amusement_project_id"),
                resultSet.getString("notice_type"),
                resultSet.getString("title"),
                resultSet.getString("content_version"),
                resultSet.getString("content_text"),
                resultSet.getString("status"),
                instant(resultSet, "effective_from"),
                instant(resultSet, "effective_to"),
                resultSet.getString("created_by_user_id"),
                readStringList(resultSet.getString("evidence_attachment_ids")),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant(),
                resultSet.getString("merchant_id")
        );
    }

    private AmusementWaiverAcknowledgementRecord mapAcknowledgement(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AmusementWaiverAcknowledgementRecord(
                resultSet.getString("id"),
                resultSet.getString("notice_id"),
                resultSet.getString("project_id"),
                resultSet.getString("amusement_project_id"),
                resultSet.getString("session_id"),
                resultSet.getString("customer_id"),
                resultSet.getString("visitor_name"),
                resultSet.getString("signer_name"),
                resultSet.getString("acknowledgement_method"),
                resultSet.getString("content_version"),
                resultSet.getString("content_hash"),
                resultSet.getTimestamp("acknowledged_at").toInstant(),
                resultSet.getString("operator_user_id"),
                readStringList(resultSet.getString("evidence_attachment_ids")),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }

    private AmusementIncidentRecord mapIncident(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AmusementIncidentRecord(
                resultSet.getString("id"),
                resultSet.getString("project_id"),
                resultSet.getString("amusement_project_id"),
                resultSet.getString("session_id"),
                resultSet.getString("device_id"),
                resultSet.getString("customer_id"),
                resultSet.getString("severity"),
                resultSet.getString("incident_type"),
                resultSet.getString("status"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getString("reported_by_user_id"),
                resultSet.getString("reported_channel"),
                resultSet.getTimestamp("occurred_at").toInstant(),
                resultSet.getString("resolution"),
                instant(resultSet, "closed_at"),
                readStringList(resultSet.getString("evidence_attachment_ids")),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant(),
                resultSet.getString("merchant_id")
        );
    }

    private AmusementIncidentActionRecord mapAction(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AmusementIncidentActionRecord(
                resultSet.getString("id"),
                resultSet.getString("incident_id"),
                resultSet.getString("action_type"),
                resultSet.getString("from_status"),
                resultSet.getString("to_status"),
                resultSet.getString("operator_user_id"),
                resultSet.getString("operator_note"),
                readStringList(resultSet.getString("evidence_attachment_ids")),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }

    private Instant instant(ResultSet resultSet, String columnName) throws SQLException {
        return resultSet.getTimestamp(columnName) == null ? null : resultSet.getTimestamp(columnName).toInstant();
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
            throw new IllegalArgumentException("json field is not readable", exception);
        }
    }
}
