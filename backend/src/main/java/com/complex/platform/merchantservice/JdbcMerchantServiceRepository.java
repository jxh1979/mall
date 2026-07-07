package com.complex.platform.merchantservice;
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
class JdbcMerchantServiceRepository implements MerchantServiceRepository {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcMerchantServiceRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public List<MerchantWorkOrderRecord> findWorkOrdersByScope(Optional<String> merchantScope, int limit) {
        if (merchantScope.isPresent()) {
            return jdbcTemplate.query("""
                    SELECT id, merchant_id, space_id, contract_id, work_order_type, priority, status,
                           title, description, reported_by_user_id, reported_channel, owner_department,
                           owner_user_id, due_at, closed_at, resolution, satisfaction_score,
                           evidence_attachment_ids, sla_breached, escalated_at, created_at, updated_at,
                           archived_at, archived_by, delete_status, delete_requested_by,
                           delete_requested_at, delete_reason
                      FROM merchant_service_work_orders
                     WHERE delete_status <> 'approved'
                       AND merchant_id = :merchant_id
                     ORDER BY created_at DESC, id
                     LIMIT :limit
                    """, Map.of("merchant_id", merchantScope.get(), "limit", limit), this::mapWorkOrder);
        }
        return jdbcTemplate.query("""
                SELECT id, merchant_id, space_id, contract_id, work_order_type, priority, status,
                       title, description, reported_by_user_id, reported_channel, owner_department,
                       owner_user_id, due_at, closed_at, resolution, satisfaction_score,
                       evidence_attachment_ids, sla_breached, escalated_at, created_at, updated_at,
                       archived_at, archived_by, delete_status, delete_requested_by,
                       delete_requested_at, delete_reason
                  FROM merchant_service_work_orders
                 WHERE delete_status <> 'approved'
                 ORDER BY created_at DESC, id
                 LIMIT :limit
                """, Map.of("limit", limit), this::mapWorkOrder);
    }

    @Override
    public List<MerchantWorkOrderRecord> findWorkOrdersByScope(Optional<String> merchantScope, boolean includeArchived, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, merchant_id, space_id, contract_id, work_order_type, priority, status,
                       title, description, reported_by_user_id, reported_channel, owner_department,
                       owner_user_id, due_at, closed_at, resolution, satisfaction_score,
                       evidence_attachment_ids, sla_breached, escalated_at, created_at, updated_at,
                       archived_at, archived_by, delete_status, delete_requested_by,
                       delete_requested_at, delete_reason
                  FROM merchant_service_work_orders
                 WHERE delete_status <> 'approved'
                """);
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("limit", limit);
        if (merchantScope.isPresent()) {
            sql.append(" AND merchant_id = :merchant_id");
            params.addValue("merchant_id", merchantScope.get());
        }
        if (!includeArchived) {
            sql.append(" AND archived_at IS NULL");
        }
        sql.append(" ORDER BY created_at DESC, id LIMIT :limit");
        return jdbcTemplate.query(sql.toString(), params, this::mapWorkOrder);
    }

    @Override
    public Optional<MerchantWorkOrderRecord> findWorkOrderById(String id) {
        return jdbcTemplate.query("""
                SELECT id, merchant_id, space_id, contract_id, work_order_type, priority, status,
                       title, description, reported_by_user_id, reported_channel, owner_department,
                       owner_user_id, due_at, closed_at, resolution, satisfaction_score,
                       evidence_attachment_ids, sla_breached, escalated_at, created_at, updated_at,
                       archived_at, archived_by, delete_status, delete_requested_by,
                       delete_requested_at, delete_reason
                  FROM merchant_service_work_orders
                 WHERE id = :id
                   AND delete_status <> 'approved'
                """, Map.of("id", id), this::mapWorkOrder).stream().findFirst();
    }

    @Override
    public List<MerchantWorkOrderRecord> findOverdueOpenWorkOrders(Instant now, int limit) {
        return jdbcTemplate.query("""
                SELECT id, merchant_id, space_id, contract_id, work_order_type, priority, status,
                       title, description, reported_by_user_id, reported_channel, owner_department,
                       owner_user_id, due_at, closed_at, resolution, satisfaction_score,
                       evidence_attachment_ids, sla_breached, escalated_at, created_at, updated_at,
                       archived_at, archived_by, delete_status, delete_requested_by,
                       delete_requested_at, delete_reason
                  FROM merchant_service_work_orders
                 WHERE delete_status <> 'approved'
                   AND due_at IS NOT NULL
                   AND due_at <= :now
                   AND escalated_at IS NULL
                   AND status NOT IN ('resolved', 'closed', 'cancelled')
                 ORDER BY due_at ASC, id
                 LIMIT :limit
                 FOR UPDATE SKIP LOCKED
                """, Map.of("now", timestampWithTimeZone(now), "limit", limit), this::mapWorkOrder);
    }

    @Override
    public int markSlaBreachedIfNotEscalated(String id, Instant now) {
        // 条件 UPDATE 幂等护栏：仅当 escalated_at IS NULL 才打标，返回受影响行数（0 表示已被并发抢先）。
        return jdbcTemplate.update("""
                UPDATE merchant_service_work_orders
                   SET sla_breached = true,
                       escalated_at = :now,
                       updated_at = :now
                 WHERE id = :id
                   AND escalated_at IS NULL
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("now", timestampWithTimeZone(now)));
    }

    @Override
    public MerchantWorkOrderRecord saveWorkOrder(MerchantWorkOrderRecord record) {
        jdbcTemplate.update("""
                INSERT INTO merchant_service_work_orders (
                    id, merchant_id, space_id, contract_id, work_order_type, priority, status,
                    title, description, reported_by_user_id, reported_channel, owner_department,
                    owner_user_id, due_at, closed_at, resolution, satisfaction_score,
                    evidence_attachment_ids, sla_breached, escalated_at, created_at, updated_at,
                    archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason
                )
                VALUES (
                    :id, :merchant_id, :space_id, :contract_id, :work_order_type, :priority, :status,
                    :title, :description, :reported_by_user_id, :reported_channel, :owner_department,
                    :owner_user_id, :due_at, :closed_at, :resolution, :satisfaction_score,
                    CAST(:evidence_attachment_ids AS jsonb), :sla_breached, :escalated_at, :created_at, :updated_at,
                    :archived_at, :archived_by, :delete_status, :delete_requested_by, :delete_requested_at, :delete_reason
                )
                ON CONFLICT (id) DO UPDATE SET
                    space_id = EXCLUDED.space_id,
                    contract_id = EXCLUDED.contract_id,
                    priority = EXCLUDED.priority,
                    status = EXCLUDED.status,
                    title = EXCLUDED.title,
                    description = EXCLUDED.description,
                    owner_department = EXCLUDED.owner_department,
                    owner_user_id = EXCLUDED.owner_user_id,
                    due_at = EXCLUDED.due_at,
                    closed_at = EXCLUDED.closed_at,
                    resolution = EXCLUDED.resolution,
                    satisfaction_score = EXCLUDED.satisfaction_score,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    sla_breached = EXCLUDED.sla_breached,
                    escalated_at = EXCLUDED.escalated_at,
                    updated_at = EXCLUDED.updated_at,
                    archived_at = EXCLUDED.archived_at,
                    archived_by = EXCLUDED.archived_by,
                    delete_status = EXCLUDED.delete_status,
                    delete_requested_by = EXCLUDED.delete_requested_by,
                    delete_requested_at = EXCLUDED.delete_requested_at,
                    delete_reason = EXCLUDED.delete_reason
                """, workOrderParameters(record));
        return record;
    }

    @Override
    public MerchantWorkOrderActionRecord saveAction(MerchantWorkOrderActionRecord record) {
        jdbcTemplate.update("""
                INSERT INTO merchant_service_work_order_actions (
                    id, work_order_id, action_type, from_status, to_status, operator_user_id,
                    operator_note, evidence_attachment_ids, created_at
                )
                VALUES (
                    :id, :work_order_id, :action_type, :from_status, :to_status, :operator_user_id,
                    :operator_note, CAST(:evidence_attachment_ids AS jsonb), :created_at
                )
                """, actionParameters(record));
        return record;
    }

    @Override
    public List<MerchantWorkOrderActionRecord> findActionsByWorkOrderId(String workOrderId) {
        return jdbcTemplate.query("""
                SELECT id, work_order_id, action_type, from_status, to_status, operator_user_id,
                       operator_note, evidence_attachment_ids, created_at
                  FROM merchant_service_work_order_actions
                 WHERE work_order_id = :work_order_id
                 ORDER BY created_at ASC, id
                """, Map.of("work_order_id", workOrderId), this::mapAction);
    }

    @Override
    public long countWorkOrderReferences(String id) {
        // 子表 actions 走 ON DELETE CASCADE，不算活引用；活引用=工单非终态（未 closed/cancelled）。
        // 非终态返回 1（拒删：须先流转关闭再删），终态返回 0（可删）。
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM merchant_service_work_orders
                 WHERE id = :id AND status NOT IN ('closed','cancelled')
                """, Map.of("id", id), Long.class);
        return count == null ? 0L : count;
    }

    @Override
    public void deleteWorkOrder(String id) {
        jdbcTemplate.update("UPDATE merchant_service_work_orders SET delete_status = 'approved', updated_at = now() WHERE id = :id", Map.of("id", id));
    }

    private MapSqlParameterSource workOrderParameters(MerchantWorkOrderRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("merchant_id", record.merchantId())
                .addValue("space_id", record.spaceId())
                .addValue("contract_id", record.contractId())
                .addValue("work_order_type", record.workOrderType())
                .addValue("priority", record.priority())
                .addValue("status", record.status())
                .addValue("title", record.title())
                .addValue("description", record.description())
                .addValue("reported_by_user_id", record.reportedByUserId())
                .addValue("reported_channel", record.reportedChannel())
                .addValue("owner_department", record.ownerDepartment())
                .addValue("owner_user_id", record.ownerUserId())
                .addValue("due_at", timestampWithTimeZone(record.dueAt()))
                .addValue("closed_at", timestampWithTimeZone(record.closedAt()))
                .addValue("resolution", record.resolution())
                .addValue("satisfaction_score", record.satisfactionScore())
                .addValue("evidence_attachment_ids", writeJson(record.evidenceAttachmentIds()))
                .addValue("sla_breached", record.slaBreached())
                .addValue("escalated_at", timestampWithTimeZone(record.escalatedAt()))
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt()))
                .addValue("archived_at", timestampWithTimeZone(record.archivedAt()))
                .addValue("archived_by", record.archivedBy())
                .addValue("delete_status", record.deleteStatus() == null ? "none" : record.deleteStatus())
                .addValue("delete_requested_by", record.deleteRequestedBy())
                .addValue("delete_requested_at", timestampWithTimeZone(record.deleteRequestedAt()))
                .addValue("delete_reason", record.deleteReason());
    }

    private MapSqlParameterSource actionParameters(MerchantWorkOrderActionRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("work_order_id", record.workOrderId())
                .addValue("action_type", record.actionType())
                .addValue("from_status", record.fromStatus())
                .addValue("to_status", record.toStatus())
                .addValue("operator_user_id", record.operatorUserId())
                .addValue("operator_note", record.operatorNote())
                .addValue("evidence_attachment_ids", writeJson(record.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(record.createdAt()));
    }

    private MerchantWorkOrderRecord mapWorkOrder(ResultSet resultSet, int rowNumber) throws SQLException {
        return new MerchantWorkOrderRecord(
                resultSet.getString("id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("space_id"),
                resultSet.getString("contract_id"),
                resultSet.getString("work_order_type"),
                resultSet.getString("priority"),
                resultSet.getString("status"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getString("reported_by_user_id"),
                resultSet.getString("reported_channel"),
                resultSet.getString("owner_department"),
                resultSet.getString("owner_user_id"),
                nullableInstant(resultSet, "due_at"),
                nullableInstant(resultSet, "closed_at"),
                resultSet.getString("resolution"),
                nullableInteger(resultSet, "satisfaction_score"),
                readStringList(resultSet.getString("evidence_attachment_ids")),
                resultSet.getBoolean("sla_breached"),
                nullableInstant(resultSet, "escalated_at"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant(),
                nullableInstant(resultSet, "archived_at"),
                resultSet.getString("archived_by"),
                resultSet.getString("delete_status"),
                resultSet.getString("delete_requested_by"),
                nullableInstant(resultSet, "delete_requested_at"),
                resultSet.getString("delete_reason")
        );
    }

    private MerchantWorkOrderActionRecord mapAction(ResultSet resultSet, int rowNumber) throws SQLException {
        return new MerchantWorkOrderActionRecord(
                resultSet.getString("id"),
                resultSet.getString("work_order_id"),
                resultSet.getString("action_type"),
                resultSet.getString("from_status"),
                resultSet.getString("to_status"),
                resultSet.getString("operator_user_id"),
                resultSet.getString("operator_note"),
                readStringList(resultSet.getString("evidence_attachment_ids")),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }


    private Instant nullableInstant(ResultSet resultSet, String columnName) throws SQLException {
        var timestamp = resultSet.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private Integer nullableInteger(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
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

    @Override
    public void setWorkOrderArchiveReason(String id, String reason) {
        jdbcTemplate.update("UPDATE merchant_service_work_orders SET archive_reason = :reason WHERE id = :id",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("reason", reason).addValue("id", id));
    }
}
