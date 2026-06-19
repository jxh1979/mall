package com.complex.platform.evidence;
import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcRiskAlertRepository implements RiskAlertRepository {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcRiskAlertRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public RiskAlertRecord saveAlert(RiskAlertRecord alert) {
        jdbcTemplate.update("""
                INSERT INTO risk_alerts (
                    alert_id, source_domain, source_ref, alert_type, risk_level, status, summary,
                    first_reported_by, first_handler_id, escalated_to, resolved_by, resolution_conclusion,
                    project_id, merchant_id, idempotency_key, created_at, updated_at
                )
                VALUES (
                    :alert_id, :source_domain, :source_ref, :alert_type, :risk_level, :status, :summary,
                    :first_reported_by, :first_handler_id, :escalated_to, :resolved_by, :resolution_conclusion,
                    :project_id, :merchant_id, :idempotency_key, :created_at, :updated_at
                )
                """, alertParameters(alert));
        return alert;
    }

    @Override
    public RiskAlertRecord updateAlert(RiskAlertRecord alert) {
        jdbcTemplate.update("""
                UPDATE risk_alerts SET
                    status = :status,
                    first_handler_id = :first_handler_id,
                    escalated_to = :escalated_to,
                    resolved_by = :resolved_by,
                    resolution_conclusion = :resolution_conclusion,
                    updated_at = :updated_at
                 WHERE alert_id = :alert_id
                """, alertParameters(alert));
        return alert;
    }

    @Override
    public Optional<RiskAlertRecord> findById(String alertId) {
        return jdbcTemplate.query("""
                SELECT alert_id, source_domain, source_ref, alert_type, risk_level, status, summary,
                       first_reported_by, first_handler_id, escalated_to, resolved_by, resolution_conclusion,
                       project_id, merchant_id, idempotency_key, created_at, updated_at
                  FROM risk_alerts
                 WHERE alert_id = :alert_id
                """, java.util.Map.of("alert_id", alertId), this::mapAlert).stream().findFirst();
    }

    @Override
    public RiskAlertActionRecord appendAction(RiskAlertActionRecord action) {
        jdbcTemplate.update("""
                INSERT INTO risk_alert_actions (
                    action_id, alert_id, action_type, actor_id, from_status, to_status,
                    reason, evidence_refs, created_at
                )
                VALUES (
                    :action_id, :alert_id, :action_type, :actor_id, :from_status, :to_status,
                    :reason, CAST(:evidence_refs AS jsonb), :created_at
                )
                """, actionParameters(action));
        return action;
    }

    @Override
    public List<RiskAlertActionRecord> listActions(String alertId) {
        return jdbcTemplate.query("""
                SELECT action_id, alert_id, action_type, actor_id, from_status, to_status,
                       reason, evidence_refs::text AS evidence_refs, created_at
                  FROM risk_alert_actions
                 WHERE alert_id = :alert_id
                 ORDER BY created_at ASC, action_id ASC
                """, java.util.Map.of("alert_id", alertId), this::mapAction);
    }

    @Override
    public List<RiskAlertRecord> search(
            String sourceDomain,
            String status,
            String projectId,
            String merchantScope,
            int limit
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT alert_id, source_domain, source_ref, alert_type, risk_level, status, summary,
                       first_reported_by, first_handler_id, escalated_to, resolved_by, resolution_conclusion,
                       project_id, merchant_id, idempotency_key, created_at, updated_at
                  FROM risk_alerts
                 WHERE 1 = 1
                """);
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (sourceDomain != null) {
            sql.append(" AND source_domain = :source_domain");
            params.addValue("source_domain", sourceDomain);
        }
        if (status != null) {
            sql.append(" AND status = :status");
            params.addValue("status", status);
        }
        if (projectId != null) {
            sql.append(" AND project_id = :project_id");
            params.addValue("project_id", projectId);
        }
        if (merchantScope != null) {
            sql.append(" AND merchant_id = :merchant_scope");
            params.addValue("merchant_scope", merchantScope);
        }
        sql.append(" ORDER BY created_at DESC, alert_id DESC LIMIT :limit");
        params.addValue("limit", limit);
        return jdbcTemplate.query(sql.toString(), params, this::mapAlert);
    }

    private MapSqlParameterSource alertParameters(RiskAlertRecord alert) {
        return new MapSqlParameterSource()
                .addValue("alert_id", alert.alertId())
                .addValue("source_domain", alert.sourceDomain())
                .addValue("source_ref", alert.sourceRef())
                .addValue("alert_type", alert.alertType())
                .addValue("risk_level", alert.riskLevel())
                .addValue("status", alert.status())
                .addValue("summary", alert.summary())
                .addValue("first_reported_by", alert.firstReportedBy())
                .addValue("first_handler_id", alert.firstHandlerId())
                .addValue("escalated_to", alert.escalatedTo())
                .addValue("resolved_by", alert.resolvedBy())
                .addValue("resolution_conclusion", alert.resolutionConclusion())
                .addValue("project_id", alert.projectId())
                .addValue("merchant_id", alert.merchantId())
                .addValue("idempotency_key", alert.idempotencyKey())
                .addValue("created_at", timestampWithTimeZone(alert.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(alert.updatedAt()));
    }

    private MapSqlParameterSource actionParameters(RiskAlertActionRecord action) {
        return new MapSqlParameterSource()
                .addValue("action_id", action.actionId())
                .addValue("alert_id", action.alertId())
                .addValue("action_type", action.actionType())
                .addValue("actor_id", action.actorId())
                .addValue("from_status", action.fromStatus())
                .addValue("to_status", action.toStatus())
                .addValue("reason", action.reason())
                .addValue("evidence_refs", writeJson(action.evidenceRefs()))
                .addValue("created_at", timestampWithTimeZone(action.createdAt()));
    }

    private RiskAlertRecord mapAlert(ResultSet resultSet, int rowNumber) throws SQLException {
        return new RiskAlertRecord(
                resultSet.getString("alert_id"),
                resultSet.getString("source_domain"),
                resultSet.getString("source_ref"),
                resultSet.getString("alert_type"),
                resultSet.getString("risk_level"),
                resultSet.getString("status"),
                resultSet.getString("summary"),
                resultSet.getString("first_reported_by"),
                resultSet.getString("first_handler_id"),
                resultSet.getString("escalated_to"),
                resultSet.getString("resolved_by"),
                resultSet.getString("resolution_conclusion"),
                resultSet.getString("project_id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("idempotency_key"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private RiskAlertActionRecord mapAction(ResultSet resultSet, int rowNumber) throws SQLException {
        return new RiskAlertActionRecord(
                resultSet.getString("action_id"),
                resultSet.getString("alert_id"),
                resultSet.getString("action_type"),
                resultSet.getString("actor_id"),
                resultSet.getString("from_status"),
                resultSet.getString("to_status"),
                resultSet.getString("reason"),
                readStringList(resultSet.getString("evidence_refs")),
                nullableInstant(resultSet, "created_at")
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("risk alert evidence_refs is not serializable", exception);
        }
    }

    private List<String> readStringList(String value) {
        try {
            return value == null ? List.of() : objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("risk alert evidence_refs is not readable", exception);
        }
    }
}
