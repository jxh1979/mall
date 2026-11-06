package com.complex.platform.settlement;

// 文件版本：V0.03

import com.complex.platform.settlement.MediaPerformanceRecords.FulfillmentRecord;
import com.complex.platform.settlement.MediaPerformanceRecords.SettlementRecord;
import com.complex.platform.settlement.MediaPerformanceRecords.ShowListItem;
import com.complex.platform.settlement.MediaPerformanceRecords.ShowRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** TK-29（V523）逐场演出合同 / 履约 / 结算三表的 JDBC 仓储，附结算基数取数（已核销入场张数、已出品酒水）。 */
@Repository
public class JdbcMediaPerformanceRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcMediaPerformanceRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------------------------------------------------------------- shows

    public ShowRecord insertShow(ShowRecord show) {
        jdbc.update("""
                INSERT INTO media_performance_shows (id, show_group_id, contract_version, project_id, bar_merchant_id, media_merchant_id,
                    show_kind, show_date, planned_start_at, planned_end_at, contract_ref, ticket_share_mode, ticket_share_bps,
                    ticket_share_amount_cent, ticket_surcharge_per_ticket_cent, ticket_type_id, fixed_fee_cent, drink_commission_bps,
                    drink_base_template, status, locked_at, locked_by, created_by, created_at, updated_at)
                VALUES (:id, :show_group_id, :contract_version, :project_id, :bar_merchant_id, :media_merchant_id,
                    :show_kind, :show_date, :planned_start_at, :planned_end_at, :contract_ref, :ticket_share_mode, :ticket_share_bps,
                    :ticket_share_amount_cent, :ticket_surcharge_per_ticket_cent, :ticket_type_id, :fixed_fee_cent, :drink_commission_bps,
                    :drink_base_template, :status, :locked_at, :locked_by, :created_by, :created_at, :updated_at)
                """, showParams(show));
        return show;
    }

    /** 只改草案的合同内容列（三种情形、时间、合同号）；状态列不在此处改。 */
    public ShowRecord updateShowValues(ShowRecord show) {
        jdbc.update("""
                UPDATE media_performance_shows
                   SET bar_merchant_id = :bar_merchant_id, media_merchant_id = :media_merchant_id, show_date = :show_date,
                       planned_start_at = :planned_start_at, planned_end_at = :planned_end_at, contract_ref = :contract_ref,
                       ticket_share_mode = :ticket_share_mode, ticket_share_bps = :ticket_share_bps,
                       ticket_share_amount_cent = :ticket_share_amount_cent,
                       ticket_surcharge_per_ticket_cent = :ticket_surcharge_per_ticket_cent, ticket_type_id = :ticket_type_id,
                       fixed_fee_cent = :fixed_fee_cent, drink_commission_bps = :drink_commission_bps,
                       drink_base_template = :drink_base_template, updated_at = :updated_at
                 WHERE id = :id AND status = 'draft'
                """, showParams(show));
        return show;
    }

    public void updateShowStatus(String id, String status, Instant lockedAt, String lockedBy) {
        jdbc.update("""
                UPDATE media_performance_shows
                   SET status = :status, locked_at = COALESCE(:locked_at, locked_at), locked_by = COALESCE(:locked_by, locked_by),
                       updated_at = now()
                 WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", id).addValue("status", status)
                .addValue("locked_at", lockedAt == null ? null : Timestamp.from(lockedAt)).addValue("locked_by", lockedBy));
    }

    public Optional<ShowRecord> findShow(String id) {
        return jdbc.query("SELECT * FROM media_performance_shows WHERE id = :id", new MapSqlParameterSource("id", id), SHOW)
                .stream().findFirst();
    }

    public Optional<ShowRecord> findShowForUpdate(String id) {
        return jdbc.query("SELECT * FROM media_performance_shows WHERE id = :id FOR UPDATE", new MapSqlParameterSource("id", id), SHOW)
                .stream().findFirst();
    }

    public List<ShowRecord> listVersions(String showGroupId) {
        return jdbc.query("SELECT * FROM media_performance_shows WHERE show_group_id = :g ORDER BY contract_version",
                new MapSqlParameterSource("g", showGroupId), SHOW);
    }

    /**
     * 第十八批 Y 段 183-R2 复核修复：挂起待确认的演出结算＝现行（已锁定）版本上 status=pending 的结算行。结算只写在已锁定版本上，
     * 被新版本取代（superseded）的版本上残留的 pending 不再计入。总控卡计数与场次列表逐行计数共用这一条件。
     */
    private static final String PENDING_SETTLEMENT = "st.status = 'pending' AND s.status = 'locked'";
    private static final String SHOW_LIST_SELECT = "SELECT s.*, (SELECT count(*) FROM media_performance_settlements st WHERE st.show_id = s.id AND "
            + PENDING_SETTLEMENT + ") AS pending_settlement_count FROM media_performance_shows s\n";

    /** 列表（按项目 / 商户范围过滤；商户既可是酒吧也可是传媒公司）；逐行带本场挂起待确认的演出结算条数。 */
    public List<ShowListItem> listShows(String projectId, String merchantId) {
        return jdbc.query(SHOW_LIST_SELECT + """
                 WHERE (:project_id::text IS NULL OR project_id = :project_id)
                   AND (:merchant_id::text IS NULL OR bar_merchant_id = :merchant_id OR media_merchant_id = :merchant_id)
                 ORDER BY show_date DESC, show_group_id, contract_version DESC
                 LIMIT 500
                """, new MapSqlParameterSource().addValue("project_id", projectId).addValue("merchant_id", merchantId), SHOW_LIST_ITEM);
    }

    /** 第十八批 G 段 82b-R5：给定项目集合内的场次（集合为空不出）。 */
    public List<ShowListItem> listShowsIn(java.util.Collection<String> projectIds, String merchantId) {
        if (projectIds.isEmpty()) {
            return List.of();
        }
        return jdbc.query(SHOW_LIST_SELECT + """
                 WHERE project_id IN (:project_ids)
                   AND (:merchant_id::text IS NULL OR bar_merchant_id = :merchant_id OR media_merchant_id = :merchant_id)
                 ORDER BY show_date DESC, show_group_id, contract_version DESC
                 LIMIT 500
                """, new MapSqlParameterSource().addValue("project_ids", projectIds).addValue("merchant_id", merchantId), SHOW_LIST_ITEM);
    }

    /** 第十八批 Y 段 183-R2：挂起待确认的演出结算条数（口径 {@link #PENDING_SETTLEMENT}）；projectIds 为 null 不限项目、为空集不出，merchantId 同 {@link #listShows}。 */
    public long countPendingSettlements(java.util.Collection<String> projectIds, String merchantId) {
        if (projectIds != null && projectIds.isEmpty()) {
            return 0L;
        }
        return jdbc.queryForObject("SELECT count(*) FROM media_performance_settlements st JOIN media_performance_shows s ON s.id = st.show_id"
                + " WHERE " + PENDING_SETTLEMENT
                + " AND (:merchant_id::text IS NULL OR s.bar_merchant_id = :merchant_id OR s.media_merchant_id = :merchant_id)"
                + (projectIds == null ? "" : " AND s.project_id IN (:project_ids)"),
                new MapSqlParameterSource().addValue("project_ids", projectIds).addValue("merchant_id", merchantId), Long.class);
    }

    /** 月结补跑：该月演出日、已锁定、履约已确认或已裁决的场次。 */
    public List<ShowRecord> listLockedShowsWithSettleableFacts(LocalDate from, LocalDate toExclusive) {
        return jdbc.query("""
                SELECT s.* FROM media_performance_shows s
                  JOIN media_performance_fulfillments f ON f.show_id = s.id AND f.status IN ('confirmed', 'resolved')
                 WHERE s.status = 'locked' AND s.show_date >= :from AND s.show_date < :to
                 ORDER BY s.show_date, s.id
                """, new MapSqlParameterSource().addValue("from", from).addValue("to", toExclusive), SHOW);
    }

    public Optional<String> merchantType(String merchantId) {
        return jdbc.query("SELECT merchant_type FROM merchants WHERE id = :id AND status <> 'deleted'",
                new MapSqlParameterSource("id", merchantId), (rs, i) -> rs.getString(1)).stream().findFirst();
    }

    /** 第十八批 G 段 82b-R8：商户以该项目为主项目或已批准的经营项目（merchant_projects 只在审批通过后写入）。 */
    public boolean merchantOperatesIn(String merchantId, String projectId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM merchants m WHERE m.id = :id AND m.status <> 'deleted'
                   AND (m.project_id = :project OR EXISTS (SELECT 1 FROM merchant_projects mp WHERE mp.merchant_id = m.id AND mp.project_id = :project)))
                """, new MapSqlParameterSource().addValue("id", merchantId).addValue("project", projectId), Boolean.class));
    }

    public Optional<String> ticketTypeBar(String ticketTypeId) {
        return jdbc.query("SELECT bar_id FROM ticket_types WHERE id = :id",
                new MapSqlParameterSource("id", ticketTypeId), (rs, i) -> rs.getString(1)).stream().findFirst();
    }

    // ---------------------------------------------------------------- fulfillments

    public FulfillmentRecord insertFulfillment(FulfillmentRecord f) {
        jdbc.update("""
                INSERT INTO media_performance_fulfillments (id, show_id, actual_start_at, actual_end_at, completed_content,
                    uncompleted_content, basis, recorded_by, recorded_at, status, updated_at)
                VALUES (:id, :show_id, :actual_start_at, :actual_end_at, :completed_content, :uncompleted_content, :basis,
                    :recorded_by, :recorded_at, :status, :recorded_at)
                """, new MapSqlParameterSource()
                .addValue("id", f.id()).addValue("show_id", f.showId())
                .addValue("actual_start_at", Timestamp.from(f.actualStartAt())).addValue("actual_end_at", Timestamp.from(f.actualEndAt()))
                .addValue("completed_content", f.completedContent()).addValue("uncompleted_content", f.uncompletedContent())
                .addValue("basis", f.basis()).addValue("recorded_by", f.recordedBy())
                .addValue("recorded_at", Timestamp.from(f.recordedAt())).addValue("status", f.status()));
        return f;
    }

    public FulfillmentRecord updateFulfillment(FulfillmentRecord f) {
        jdbc.update("""
                UPDATE media_performance_fulfillments
                   SET status = :status, confirmed_by = :confirmed_by, confirmed_at = :confirmed_at,
                       dispute_reason = :dispute_reason, disputed_by = :disputed_by, disputed_at = :disputed_at,
                       resolution_note = :resolution_note, resolved_by = :resolved_by, resolved_at = :resolved_at, updated_at = now()
                 WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", f.id()).addValue("status", f.status())
                .addValue("confirmed_by", f.confirmedBy()).addValue("confirmed_at", ts(f.confirmedAt()))
                .addValue("dispute_reason", f.disputeReason()).addValue("disputed_by", f.disputedBy()).addValue("disputed_at", ts(f.disputedAt()))
                .addValue("resolution_note", f.resolutionNote()).addValue("resolved_by", f.resolvedBy()).addValue("resolved_at", ts(f.resolvedAt())));
        return f;
    }

    public Optional<FulfillmentRecord> findFulfillmentByShow(String showId) {
        return jdbc.query("SELECT * FROM media_performance_fulfillments WHERE show_id = :show_id",
                new MapSqlParameterSource("show_id", showId), FULFILLMENT).stream().findFirst();
    }

    // ---------------------------------------------------------------- settlements

    public SettlementRecord insertSettlement(SettlementRecord s) {
        jdbc.update("""
                INSERT INTO media_performance_settlements (id, show_id, fulfillment_id, fee_kind, base_amount_cent, rate_bps,
                    fixed_amount_cent, amount_cent, payer_merchant_id, payer_entity_id, payee_entity_id, status, pending_reason,
                    ar_ap_item_id, adjusts_settlement_id, adjust_reason, created_by, created_at, updated_at)
                VALUES (:id, :show_id, :fulfillment_id, :fee_kind, :base_amount_cent, :rate_bps, :fixed_amount_cent, :amount_cent,
                    :payer_merchant_id, :payer_entity_id, :payee_entity_id, :status, :pending_reason, :ar_ap_item_id,
                    :adjusts_settlement_id, :adjust_reason, :created_by, :created_at, :updated_at)
                """, settlementParams(s));
        return s;
    }

    /** 同一行从 pending 走到 posted（或 pending 原因变化）；调整时原行只改 status。 */
    public SettlementRecord updateSettlement(SettlementRecord s) {
        jdbc.update("""
                UPDATE media_performance_settlements
                   SET fulfillment_id = :fulfillment_id, base_amount_cent = :base_amount_cent, rate_bps = :rate_bps,
                       fixed_amount_cent = :fixed_amount_cent, amount_cent = :amount_cent, payer_merchant_id = :payer_merchant_id,
                       payer_entity_id = :payer_entity_id, payee_entity_id = :payee_entity_id, status = :status,
                       pending_reason = :pending_reason, ar_ap_item_id = :ar_ap_item_id, updated_at = :updated_at
                 WHERE id = :id
                """, settlementParams(s));
        return s;
    }

    public void markSettlementAdjusted(String id) {
        jdbc.update("UPDATE media_performance_settlements SET status = 'adjusted', updated_at = now() WHERE id = :id",
                new MapSqlParameterSource("id", id));
    }

    public Optional<SettlementRecord> findSettlement(String id) {
        return jdbc.query("SELECT * FROM media_performance_settlements WHERE id = :id FOR UPDATE",
                new MapSqlParameterSource("id", id), SETTLEMENT).stream().findFirst();
    }

    public List<SettlementRecord> listSettlements(String showId) {
        return jdbc.query("SELECT * FROM media_performance_settlements WHERE show_id = :show_id ORDER BY created_at, id",
                new MapSqlParameterSource("show_id", showId), SETTLEMENT);
    }

    // ---------------------------------------------------------------- 结算基数取数

    /** ①基数：该票种在时间窗内成功核销（未被冲正）的入场张数（按父票去重，一张票多项权益只算一张）。 */
    public long countVerifiedTickets(String ticketTypeId, Instant from, Instant to) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(DISTINCT COALESCE(e.parent_ticket_id, e.id))
                  FROM ticket_verifications v
                  JOIN issued_entitlements e ON e.id = v.entitlement_id
                 WHERE e.ticket_type_id = :ticket_type_id AND v.status = 'success' AND v.reversed_at IS NULL
                   AND v.created_at >= :from AND v.created_at < :to
                """, new MapSqlParameterSource().addValue("ticket_type_id", ticketTypeId)
                .addValue("from", Timestamp.from(from)).addValue("to", Timestamp.from(to)), Long.class);
        return count == null ? 0L : count;
    }

    /**
     * ③基数：该酒吧时间窗内普通单（order_type='standard'，剔除通票赠饮 0 元单与酒吧门票赠饮桥表关联单）已出品、未作废的酒水金额。
     * bar_orders.bar_id 即该酒吧商户 id（BarOrderService 下单时按商户绑定校验）。
     */
    public long sumDeliveredDrinkBase(String barMerchantId, Instant from, Instant to) {
        Long sum = jdbc.queryForObject("""
                SELECT COALESCE(SUM(i.amount_cent), 0)
                  FROM bar_order_items i
                  JOIN bar_orders o ON o.id = i.order_id
                 WHERE o.bar_id = :bar AND o.order_type = 'standard' AND i.delivery_status = 'delivered'
                   AND o.created_at >= :from AND o.created_at < :to
                   AND NOT EXISTS (SELECT 1 FROM bar_gift_redemptions g WHERE g.bar_order_id = o.id)
                """, new MapSqlParameterSource().addValue("bar", barMerchantId)
                .addValue("from", Timestamp.from(from)).addValue("to", Timestamp.from(to)), Long.class);
        return sum == null ? 0L : sum;
    }

    // ---------------------------------------------------------------- mappers

    private static MapSqlParameterSource showParams(ShowRecord s) {
        return new MapSqlParameterSource()
                .addValue("id", s.id()).addValue("show_group_id", s.showGroupId()).addValue("contract_version", s.contractVersion())
                .addValue("project_id", s.projectId()).addValue("bar_merchant_id", s.barMerchantId())
                .addValue("media_merchant_id", s.mediaMerchantId()).addValue("show_kind", s.showKind())
                .addValue("show_date", s.showDate()).addValue("planned_start_at", ts(s.plannedStartAt()))
                .addValue("planned_end_at", ts(s.plannedEndAt())).addValue("contract_ref", s.contractRef())
                .addValue("ticket_share_mode", s.ticketShareMode()).addValue("ticket_share_bps", s.ticketShareBps())
                .addValue("ticket_share_amount_cent", s.ticketShareAmountCent())
                .addValue("ticket_surcharge_per_ticket_cent", s.ticketSurchargePerTicketCent())
                .addValue("ticket_type_id", s.ticketTypeId()).addValue("fixed_fee_cent", s.fixedFeeCent())
                .addValue("drink_commission_bps", s.drinkCommissionBps()).addValue("drink_base_template", s.drinkBaseTemplate())
                .addValue("status", s.status()).addValue("locked_at", ts(s.lockedAt())).addValue("locked_by", s.lockedBy())
                .addValue("created_by", s.createdBy()).addValue("created_at", ts(s.createdAt())).addValue("updated_at", ts(s.updatedAt()));
    }

    private static MapSqlParameterSource settlementParams(SettlementRecord s) {
        return new MapSqlParameterSource()
                .addValue("id", s.id()).addValue("show_id", s.showId()).addValue("fulfillment_id", s.fulfillmentId())
                .addValue("fee_kind", s.feeKind()).addValue("base_amount_cent", s.baseAmountCent()).addValue("rate_bps", s.rateBps())
                .addValue("fixed_amount_cent", s.fixedAmountCent()).addValue("amount_cent", s.amountCent())
                .addValue("payer_merchant_id", s.payerMerchantId()).addValue("payer_entity_id", s.payerEntityId())
                .addValue("payee_entity_id", s.payeeEntityId()).addValue("status", s.status()).addValue("pending_reason", s.pendingReason())
                .addValue("ar_ap_item_id", s.arApItemId()).addValue("adjusts_settlement_id", s.adjustsSettlementId())
                .addValue("adjust_reason", s.adjustReason()).addValue("created_by", s.createdBy())
                .addValue("created_at", ts(s.createdAt())).addValue("updated_at", ts(s.updatedAt()));
    }

    private static final RowMapper<ShowRecord> SHOW = (rs, i) -> new ShowRecord(
            rs.getString("id"), rs.getString("show_group_id"), rs.getInt("contract_version"), rs.getString("project_id"),
            rs.getString("bar_merchant_id"), rs.getString("media_merchant_id"), rs.getString("show_kind"),
            rs.getObject("show_date", LocalDate.class), instant(rs, "planned_start_at"), instant(rs, "planned_end_at"),
            rs.getString("contract_ref"), rs.getString("ticket_share_mode"), rs.getObject("ticket_share_bps", Integer.class),
            rs.getObject("ticket_share_amount_cent", Long.class), rs.getObject("ticket_surcharge_per_ticket_cent", Long.class),
            rs.getString("ticket_type_id"), rs.getObject("fixed_fee_cent", Long.class),
            rs.getObject("drink_commission_bps", Integer.class), rs.getString("drink_base_template"), rs.getString("status"),
            instant(rs, "locked_at"), rs.getString("locked_by"), rs.getString("created_by"),
            instant(rs, "created_at"), instant(rs, "updated_at"));

    private static final RowMapper<ShowListItem> SHOW_LIST_ITEM = (rs, i) ->
            new ShowListItem(SHOW.mapRow(rs, i), rs.getLong("pending_settlement_count"));

    private static final RowMapper<FulfillmentRecord> FULFILLMENT = (rs, i) -> new FulfillmentRecord(
            rs.getString("id"), rs.getString("show_id"), instant(rs, "actual_start_at"), instant(rs, "actual_end_at"),
            rs.getString("completed_content"), rs.getString("uncompleted_content"), rs.getString("basis"),
            rs.getString("recorded_by"), instant(rs, "recorded_at"), rs.getString("status"),
            rs.getString("confirmed_by"), instant(rs, "confirmed_at"), rs.getString("dispute_reason"),
            rs.getString("disputed_by"), instant(rs, "disputed_at"), rs.getString("resolution_note"),
            rs.getString("resolved_by"), instant(rs, "resolved_at"));

    private static final RowMapper<SettlementRecord> SETTLEMENT = (rs, i) -> new SettlementRecord(
            rs.getString("id"), rs.getString("show_id"), rs.getString("fulfillment_id"), rs.getString("fee_kind"),
            rs.getLong("base_amount_cent"), rs.getObject("rate_bps", Integer.class), rs.getObject("fixed_amount_cent", Long.class),
            rs.getLong("amount_cent"), rs.getString("payer_merchant_id"), rs.getString("payer_entity_id"),
            rs.getString("payee_entity_id"), rs.getString("status"), rs.getString("pending_reason"), rs.getString("ar_ap_item_id"),
            rs.getString("adjusts_settlement_id"), rs.getString("adjust_reason"), rs.getString("created_by"),
            instant(rs, "created_at"), instant(rs, "updated_at"));

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static Timestamp ts(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
