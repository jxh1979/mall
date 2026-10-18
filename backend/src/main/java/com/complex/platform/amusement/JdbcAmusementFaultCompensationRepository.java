package com.complex.platform.amusement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** CG-08b 补偿有效期参数 / 补偿批次 / 补偿记录读写，以及受影响人份快照查询。 */
@Repository
public class JdbcAmusementFaultCompensationRepository {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private static final String BATCH_SELECT = """
            SELECT id, fault_id, project_id, status, reason, bearer_company_id, validity_days, window_from, window_to,
                   valid_until, initiated_by, initiated_role, initiated_at, confirmed_by, confirmed_role, confirmed_at,
                   item_count, extend_count, reissue_count, created_at, updated_at
              FROM amusement_fault_compensation_batches
            """;
    private static final String ITEM_SELECT = """
            SELECT id, batch_id, fault_id, project_id, entitlement_id, order_id, ticket_type_id, parent_ticket_id, slot_index,
                   customer_id, kind, share_count, original_session_id, original_entry_verification_id, original_valid_to,
                   status, new_valid_to, compensation_entitlement_ids, fulfilled_verification_id, fulfilled_at,
                   fulfilled_settlement_price_cent, created_at, updated_at
              FROM amusement_fault_compensation_items
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcAmusementFaultCompensationRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    // ---------- 补偿有效期参数 ----------

    public Optional<AmusementCompensationPolicyRecord> findPolicy(String projectId) {
        return jdbcTemplate.query("""
                SELECT project_id, validity_days, min_days, max_days, updated_by, updated_at
                  FROM amusement_compensation_policies WHERE project_id = :project_id
                """, new MapSqlParameterSource("project_id", projectId), (rs, n) -> new AmusementCompensationPolicyRecord(
                rs.getString("project_id"), rs.getInt("validity_days"), rs.getInt("min_days"), rs.getInt("max_days"),
                rs.getString("updated_by"), instant(rs, "updated_at"))).stream().findFirst();
    }

    public AmusementCompensationPolicyRecord upsertPolicy(AmusementCompensationPolicyRecord r) {
        jdbcTemplate.update("""
                INSERT INTO amusement_compensation_policies (project_id, validity_days, min_days, max_days, updated_by, updated_at)
                VALUES (:project_id, :validity_days, :min_days, :max_days, :updated_by, :updated_at)
                ON CONFLICT (project_id) DO UPDATE SET validity_days = EXCLUDED.validity_days, min_days = EXCLUDED.min_days,
                    max_days = EXCLUDED.max_days, updated_by = EXCLUDED.updated_by, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource().addValue("project_id", r.projectId()).addValue("validity_days", r.validityDays())
                .addValue("min_days", r.minDays()).addValue("max_days", r.maxDays()).addValue("updated_by", r.updatedBy())
                .addValue("updated_at", ts(r.updatedAt())));
        return r;
    }

    // ---------- 批次 ----------

    public AmusementFaultCompensationBatchRecord insertBatch(AmusementFaultCompensationBatchRecord b) {
        jdbcTemplate.update("""
                INSERT INTO amusement_fault_compensation_batches (id, fault_id, project_id, status, reason, bearer_company_id,
                        validity_days, window_from, window_to, valid_until, initiated_by, initiated_role, initiated_at,
                        item_count, extend_count, reissue_count, created_at, updated_at)
                VALUES (:id, :fault_id, :project_id, :status, :reason, :bearer_company_id, :validity_days, :window_from, :window_to,
                        :valid_until, :initiated_by, :initiated_role, :initiated_at, :item_count, :extend_count, :reissue_count,
                        :created_at, :updated_at)
                """, batchParams(b));
        return b;
    }

    public AmusementFaultCompensationBatchRecord updateBatch(AmusementFaultCompensationBatchRecord b) {
        jdbcTemplate.update("""
                UPDATE amusement_fault_compensation_batches
                   SET status = :status, valid_until = :valid_until, confirmed_by = :confirmed_by, confirmed_role = :confirmed_role,
                       confirmed_at = :confirmed_at, item_count = :item_count, extend_count = :extend_count,
                       reissue_count = :reissue_count, updated_at = :updated_at
                 WHERE id = :id
                """, batchParams(b));
        return b;
    }

    public Optional<AmusementFaultCompensationBatchRecord> findBatchById(String id) {
        return jdbcTemplate.query(BATCH_SELECT + " WHERE id = :id", new MapSqlParameterSource("id", id), this::mapBatch)
                .stream().findFirst();
    }

    public Optional<AmusementFaultCompensationBatchRecord> findPendingBatch(String faultId) {
        return jdbcTemplate.query(BATCH_SELECT + " WHERE fault_id = :fault_id AND status = 'pending_confirmation'",
                new MapSqlParameterSource("fault_id", faultId), this::mapBatch).stream().findFirst();
    }

    public List<AmusementFaultCompensationBatchRecord> listBatches(String faultId) {
        return jdbcTemplate.query(BATCH_SELECT + " WHERE fault_id = :fault_id ORDER BY created_at DESC",
                new MapSqlParameterSource("fault_id", faultId), this::mapBatch);
    }

    // ---------- 补偿记录 ----------

    public void insertItem(AmusementFaultCompensationItemRecord i) {
        jdbcTemplate.update("""
                INSERT INTO amusement_fault_compensation_items (id, batch_id, fault_id, project_id, entitlement_id, order_id,
                        ticket_type_id, parent_ticket_id, slot_index, customer_id, kind, share_count, original_session_id,
                        original_entry_verification_id, original_valid_to, status, new_valid_to, compensation_entitlement_ids,
                        created_at, updated_at)
                VALUES (:id, :batch_id, :fault_id, :project_id, :entitlement_id, :order_id, :ticket_type_id, :parent_ticket_id,
                        :slot_index, :customer_id, :kind, :share_count, :original_session_id, :original_entry_verification_id,
                        :original_valid_to, :status, :new_valid_to, CAST(:compensation_entitlement_ids AS jsonb), :created_at, :updated_at)
                """, itemParams(i));
    }

    public void markItemIssued(String id, Instant newValidTo, List<String> compensationEntitlementIds, Instant now) {
        jdbcTemplate.update("""
                UPDATE amusement_fault_compensation_items
                   SET status = 'issued', new_valid_to = :new_valid_to,
                       compensation_entitlement_ids = CAST(:ids AS jsonb), updated_at = :updated_at
                 WHERE id = :id AND status = 'pending'
                """, new MapSqlParameterSource().addValue("id", id).addValue("new_valid_to", ts(newValidTo))
                .addValue("ids", json(compensationEntitlementIds)).addValue("updated_at", ts(now)));
    }

    /** 补偿机会用一次即用尽：仅 issued → used 一次；重复核销同一补偿不改写首次履约。返回是否本次写入。 */
    public boolean markItemFulfilled(String id, String verificationId, Instant at, Long settlementPriceCent) {
        return jdbcTemplate.update("""
                UPDATE amusement_fault_compensation_items
                   SET status = 'used', fulfilled_verification_id = :verification_id, fulfilled_at = :at,
                       fulfilled_settlement_price_cent = :price, updated_at = :at
                 WHERE id = :id AND status = 'issued'
                """, new MapSqlParameterSource().addValue("id", id).addValue("verification_id", verificationId)
                .addValue("at", ts(at)).addValue("price", settlementPriceCent)) > 0;
    }

    public List<AmusementFaultCompensationItemRecord> listItems(String batchId) {
        return jdbcTemplate.query(ITEM_SELECT + " WHERE batch_id = :batch_id ORDER BY created_at, id",
                new MapSqlParameterSource("batch_id", batchId), this::mapItem);
    }

    public Optional<AmusementFaultCompensationItemRecord> findItemById(String id) {
        return jdbcTemplate.query(ITEM_SELECT + " WHERE id = :id", new MapSqlParameterSource("id", id), this::mapItem)
                .stream().findFirst();
    }

    // ---------- 受影响人份快照 ----------

    /**
     * 故障发生时已完成入场核销、且本场次履约受影响的人份：入场核销（success）所在场次与故障时间窗 [from, to] 相交，
     * 且故障发生前未出场；只取游乐类权益；排除补偿补发行与同故障已补偿的人份。行：entitlement_id、order_id、ticket_type_id、
     * parent_ticket_id、slot_index、valid_to、customer_id、session_id、entry_verification_id。
     */
    public List<Map<String, Object>> listVerifiedAffectedShares(String faultId, String projectId, Instant from, Instant to) {
        return jdbcTemplate.queryForList("""
                SELECT DISTINCT ON (e.id) e.id AS entitlement_id, e.order_id, e.ticket_type_id, e.parent_ticket_id, e.slot_index,
                       e.valid_to, COALESCE(h.holder_customer_id, o.customer_id) AS customer_id,
                       av.session_id, av.id AS entry_verification_id
                  FROM amusement_verifications av
                  JOIN amusement_sessions s ON s.id = av.session_id
                  JOIN amusement_projects ap ON ap.id = s.project_id
                  LEFT JOIN merchants m ON m.id = ap.merchant_id
                  JOIN issued_entitlements e ON e.id = av.entitlement_id
                  LEFT JOIN ticket_orders o ON o.id = e.order_id
                  LEFT JOIN ticket_slot_holders h ON h.parent_ticket_id = e.parent_ticket_id AND h.slot_index = e.slot_index
                 WHERE av.direction = 'entry' AND av.verification_result = 'success'
                   AND s.start_time < :window_to AND s.end_time > :window_from
                   AND av.created_at <= :window_to
                   AND (av.released_at IS NULL OR av.released_at >= :window_from)
                   AND LOWER(e.entitlement_type) LIKE 'amusement%'
                   AND e.compensation_item_id IS NULL
                   AND (m.project_id IS NULL OR m.project_id = :project_id)
                   AND NOT EXISTS (SELECT 1 FROM amusement_fault_compensation_items i
                                    WHERE i.fault_id = :fault_id AND i.entitlement_id = e.id)
                 ORDER BY e.id, av.created_at DESC
                """, new MapSqlParameterSource().addValue("fault_id", faultId).addValue("project_id", projectId)
                .addValue("window_from", ts(from)).addValue("window_to", ts(to)));
    }

    /**
     * 故障发生时尚未核销、但对应游乐权益仍在有效期内的人份（含多次票剩余次份）：active、剩余次数>0、故障发生时点在有效期内；
     * 排除补偿补发行与同故障已补偿的人份。行：entitlement_id、order_id、ticket_type_id、parent_ticket_id、slot_index、valid_to、customer_id。
     */
    public List<Map<String, Object>> listUnverifiedAffectedShares(String faultId, String projectId, Instant at) {
        return jdbcTemplate.queryForList("""
                SELECT e.id AS entitlement_id, e.order_id, e.ticket_type_id, e.parent_ticket_id, e.slot_index, e.valid_to,
                       COALESCE(h.holder_customer_id, o.customer_id) AS customer_id
                  FROM issued_entitlements e
                  LEFT JOIN merchants m ON m.id = COALESCE(e.bar_id, e.merchant_id)
                  LEFT JOIN ticket_orders o ON o.id = e.order_id
                  LEFT JOIN ticket_slot_holders h ON h.parent_ticket_id = e.parent_ticket_id AND h.slot_index = e.slot_index
                 WHERE LOWER(e.entitlement_type) LIKE 'amusement%'
                   AND e.status = 'active' AND e.used_times < e.total_times
                   AND e.valid_from <= :at AND e.valid_to >= :at
                   AND e.compensation_item_id IS NULL
                   AND (m.project_id IS NULL OR m.project_id = :project_id)
                   AND NOT EXISTS (SELECT 1 FROM amusement_fault_compensation_items i
                                    WHERE i.fault_id = :fault_id AND i.entitlement_id = e.id)
                 ORDER BY e.created_at, e.id
                """, new MapSqlParameterSource().addValue("fault_id", faultId).addValue("project_id", projectId)
                .addValue("at", ts(at)));
    }

    // ---------- mapping ----------

    private MapSqlParameterSource batchParams(AmusementFaultCompensationBatchRecord b) {
        return new MapSqlParameterSource().addValue("id", b.id()).addValue("fault_id", b.faultId())
                .addValue("project_id", b.projectId()).addValue("status", b.status()).addValue("reason", b.reason())
                .addValue("bearer_company_id", b.bearerCompanyId()).addValue("validity_days", b.validityDays())
                .addValue("window_from", ts(b.windowFrom())).addValue("window_to", ts(b.windowTo()))
                .addValue("valid_until", ts(b.validUntil())).addValue("initiated_by", b.initiatedBy())
                .addValue("initiated_role", b.initiatedRole()).addValue("initiated_at", ts(b.initiatedAt()))
                .addValue("confirmed_by", b.confirmedBy()).addValue("confirmed_role", b.confirmedRole())
                .addValue("confirmed_at", ts(b.confirmedAt())).addValue("item_count", b.itemCount())
                .addValue("extend_count", b.extendCount()).addValue("reissue_count", b.reissueCount())
                .addValue("created_at", ts(b.createdAt())).addValue("updated_at", ts(b.updatedAt()));
    }

    private MapSqlParameterSource itemParams(AmusementFaultCompensationItemRecord i) {
        return new MapSqlParameterSource().addValue("id", i.id()).addValue("batch_id", i.batchId())
                .addValue("fault_id", i.faultId()).addValue("project_id", i.projectId())
                .addValue("entitlement_id", i.entitlementId()).addValue("order_id", i.orderId())
                .addValue("ticket_type_id", i.ticketTypeId()).addValue("parent_ticket_id", i.parentTicketId())
                .addValue("slot_index", i.slotIndex()).addValue("customer_id", i.customerId()).addValue("kind", i.kind())
                .addValue("share_count", i.shareCount()).addValue("original_session_id", i.originalSessionId())
                .addValue("original_entry_verification_id", i.originalEntryVerificationId())
                .addValue("original_valid_to", ts(i.originalValidTo())).addValue("status", i.status())
                .addValue("new_valid_to", ts(i.newValidTo()))
                .addValue("compensation_entitlement_ids", json(i.compensationEntitlementIds()))
                .addValue("created_at", ts(i.createdAt())).addValue("updated_at", ts(i.updatedAt()));
    }

    private AmusementFaultCompensationBatchRecord mapBatch(ResultSet rs, int n) throws SQLException {
        return new AmusementFaultCompensationBatchRecord(rs.getString("id"), rs.getString("fault_id"), rs.getString("project_id"),
                rs.getString("status"), rs.getString("reason"), rs.getString("bearer_company_id"), rs.getInt("validity_days"),
                instant(rs, "window_from"), instant(rs, "window_to"), instant(rs, "valid_until"), rs.getString("initiated_by"),
                rs.getString("initiated_role"), instant(rs, "initiated_at"), rs.getString("confirmed_by"),
                rs.getString("confirmed_role"), instant(rs, "confirmed_at"), rs.getInt("item_count"), rs.getInt("extend_count"),
                rs.getInt("reissue_count"), instant(rs, "created_at"), instant(rs, "updated_at"));
    }

    private AmusementFaultCompensationItemRecord mapItem(ResultSet rs, int n) throws SQLException {
        Integer slot = rs.getObject("slot_index") == null ? null : rs.getInt("slot_index");
        Long price = rs.getObject("fulfilled_settlement_price_cent") == null ? null : rs.getLong("fulfilled_settlement_price_cent");
        return new AmusementFaultCompensationItemRecord(rs.getString("id"), rs.getString("batch_id"), rs.getString("fault_id"),
                rs.getString("project_id"), rs.getString("entitlement_id"), rs.getString("order_id"), rs.getString("ticket_type_id"),
                rs.getString("parent_ticket_id"), slot, rs.getString("customer_id"), rs.getString("kind"), rs.getInt("share_count"),
                rs.getString("original_session_id"), rs.getString("original_entry_verification_id"),
                instant(rs, "original_valid_to"), rs.getString("status"), instant(rs, "new_valid_to"),
                readList(rs.getString("compensation_entitlement_ids")), rs.getString("fulfilled_verification_id"),
                instant(rs, "fulfilled_at"), price, instant(rs, "created_at"), instant(rs, "updated_at"));
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("compensation_entitlement_ids 解析失败", e);
        }
    }

    private String json(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("compensation_entitlement_ids 序列化失败", e);
        }
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp t = rs.getTimestamp(column);
        return t == null ? null : t.toInstant();
    }

    private static Timestamp ts(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
