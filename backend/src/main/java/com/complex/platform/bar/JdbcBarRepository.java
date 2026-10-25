package com.complex.platform.bar;

// 文件版本：V0.04
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.complex.platform.common.crypto.PiiCipher;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.inventory.InventoryRepository;
import com.complex.platform.inventory.StockMovementRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
class JdbcBarRepository implements BarRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcBarRowMapper rowMapper;
    private final JdbcBarParameterSource parameterSource;
    private final InventoryRepository inventoryRepository;
    private final TransactionTemplate transactionTemplate;
    private final PiiCipher piiCipher;

    // C-1(V296)：注入库存仓储 + 事务管理器——酒吧域流水写入改道库存域成本管道（见本类
    // saveStockMovement），不再裸 INSERT inventory_stock_movements。
    JdbcBarRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            InventoryRepository inventoryRepository,
            PlatformTransactionManager transactionManager,
            PiiCipher piiCipher
    ) {
        this.jdbcTemplate = jdbcTemplate;
        var jdbcObjectMapper = objectMapper.copy().findAndRegisterModules();
        this.rowMapper = new JdbcBarRowMapper(jdbcObjectMapper);
        this.parameterSource = new JdbcBarParameterSource(jdbcObjectMapper);
        this.inventoryRepository = inventoryRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.piiCipher = piiCipher;
    }

    // ── 目录：桌位 / 预订 / 押金规则 / 商品 ──────────────────────────────────────
    @Override
    public BarTableRecord saveTable(BarTableRecord table) {
        jdbcTemplate.update("""
                INSERT INTO bar_tables (
                    id, bar_id, merchant_id, table_code, table_name, table_type, capacity,
                    allowed_pass_free_guest_count, min_consumption_cent, currency,
                    status, created_at, updated_at, pending_clear_since
                )
                VALUES (
                    :id, :bar_id, :merchant_id, :table_code, :table_name, :table_type, :capacity,
                    :allowed_pass_free_guest_count, :min_consumption_cent, :currency,
                    :status, :created_at, :updated_at,
                    CASE WHEN CAST(:status AS VARCHAR) = 'pending_clear' THEN CAST(:updated_at AS TIMESTAMPTZ) END
                )
                ON CONFLICT (id) DO UPDATE SET
                    bar_id = EXCLUDED.bar_id,
                    merchant_id = EXCLUDED.merchant_id,
                    table_code = EXCLUDED.table_code,
                    table_name = EXCLUDED.table_name,
                    table_type = EXCLUDED.table_type,
                    capacity = EXCLUDED.capacity,
                    allowed_pass_free_guest_count = EXCLUDED.allowed_pass_free_guest_count,
                    min_consumption_cent = EXCLUDED.min_consumption_cent,
                    currency = EXCLUDED.currency,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at,
                    -- 复核 money m4：待清台起点只在进入待清台时写一次，编辑资料不重置；离开待清台清空。
                    pending_clear_since = CASE
                        WHEN EXCLUDED.status <> 'pending_clear' THEN NULL
                        WHEN bar_tables.status = 'pending_clear' THEN COALESCE(bar_tables.pending_clear_since, EXCLUDED.pending_clear_since)
                        ELSE EXCLUDED.pending_clear_since END
                """, parameterSource.tableParameters(table));
        return table;
    }

    /** BYOD 一期空位视图：ROOT-03-R19 起按排他预留口径——正被有效预留占着的台位（ENUM 第 100 行「排他锁，不是展示标记」）。 */
    @Override
    public java.util.Set<String> listActiveReservedTableIds() {
        return new java.util.LinkedHashSet<>(jdbcTemplate.queryForList(
                "SELECT table_id FROM bar_reservations WHERE hold_status = 'held'", Map.of(), String.class));
    }

    // 第十批 TK-27-R2（BSM L1058 待清台超时提醒）：列表面带出待清台起始时刻与是否超过酒吧（merchant_type=bar 的商户）
    // 配置的 merchants.pending_clear_alert_minutes（出厂 15）；起点为 bar_tables.pending_clear_since（复核 money m4，编辑台位不重置）。
    private static final String TABLE_LISTING_SQL = """
            SELECT t.id, t.bar_id, t.merchant_id, t.table_code, t.table_name, t.table_type, t.capacity,
                   t.allowed_pass_free_guest_count, t.min_consumption_cent, t.currency,
                   t.status, t.created_at, t.updated_at,
                   CASE WHEN t.status = 'pending_clear' THEN t.pending_clear_since END AS pending_clear_since,
                   (t.status = 'pending_clear'
                        AND t.pending_clear_since < now() - make_interval(mins => COALESCE(m.pending_clear_alert_minutes, 15)))
                       AS pending_clear_overdue,
                   (SELECT r.id FROM bar_reservations r WHERE r.table_id = t.id AND r.hold_status = 'held') AS held_reservation_id
              FROM bar_tables t
              LEFT JOIN merchants m ON m.id = t.bar_id
            """;

    @Override
    public List<BarTableRecord> listTables(Optional<String> merchantScope) {
        if (merchantScope.isEmpty()) {
            return jdbcTemplate.query(TABLE_LISTING_SQL + " ORDER BY t.created_at DESC, t.id", rowMapper::mapTableListing);
        }
        return jdbcTemplate.query(TABLE_LISTING_SQL + " WHERE t.merchant_id = :merchant_id ORDER BY t.created_at DESC, t.id",
                Map.of("merchant_id", merchantScope.get()), rowMapper::mapTableListing);
    }

    @Override
    public Optional<BarTableRecord> findTableById(String id) {
        return jdbcTemplate.query("""
                SELECT id, bar_id, merchant_id, table_code, table_name, table_type, capacity,
                       allowed_pass_free_guest_count, min_consumption_cent, currency,
                       status, created_at, updated_at
                  FROM bar_tables
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapTable).stream().findFirst();
    }

    @Override
    public Optional<BarTableRecord> findFirstTableByBar(String barId) {
        return jdbcTemplate.query("""
                SELECT id, bar_id, merchant_id, table_code, table_name, table_type, capacity,
                       allowed_pass_free_guest_count, min_consumption_cent, currency,
                       status, created_at, updated_at
                  FROM bar_tables
                 WHERE bar_id = :bar_id
                 ORDER BY created_at, id
                 LIMIT 1
                """, Map.of("bar_id", barId), rowMapper::mapTable).stream().findFirst();
    }

    @Override
    public int assignTableSpace(String tableId, String spaceId) {
        return jdbcTemplate.update("""
                UPDATE bar_tables
                   SET space_id = :space_id,
                       updated_at = NOW()
                 WHERE id = :id
                """, Map.of("id", tableId, "space_id", spaceId));
    }

    @Override
    public Optional<String> findSpaceMerchant(String spaceId) {
        // spaces.merchant_id 可能为 NULL（平台/共享空间）；用 query 区分「空间不存在」与「存在但 merchant 为空」。
        List<String> rows = jdbcTemplate.query(
                "SELECT COALESCE(merchant_id, '') FROM spaces WHERE id = :id",
                Map.of("id", spaceId),
                (rs, n) -> rs.getString(1));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public List<BarTableSpaceBacklogRecord> listTablesWithoutSpace(Optional<String> merchantScope) {
        String base = """
                SELECT id, bar_id, merchant_id, table_code, table_name, status
                  FROM bar_tables
                 WHERE space_id IS NULL
                """;
        if (merchantScope.isEmpty()) {
            return jdbcTemplate.query(base + " ORDER BY created_at DESC, id", JdbcBarRepository::mapBacklog);
        }
        return jdbcTemplate.query(
                base + " AND merchant_id = :merchant_id ORDER BY created_at DESC, id",
                Map.of("merchant_id", merchantScope.get()),
                JdbcBarRepository::mapBacklog);
    }

    private static BarTableSpaceBacklogRecord mapBacklog(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new BarTableSpaceBacklogRecord(
                rs.getString("id"),
                rs.getString("bar_id"),
                rs.getString("merchant_id"),
                rs.getString("table_code"),
                rs.getString("table_name"),
                rs.getString("status"));
    }

    @Override
    public BarReservationRecord saveReservation(BarReservationRecord reservation) {
        jdbcTemplate.update("""
                INSERT INTO bar_reservations (
                    id, bar_id, merchant_id, table_id, customer_id, reserved_at, guest_count,
                    allowed_pass_free_guest_count, deposit_amount_cent, deposit_status,
                    order_source, sales_staff_id, status, created_terminal, device_id,
                    evidence_attachment_ids, created_at, updated_at, deposit_rule,
                    deposit_intent_id, deposit_paid_at, deposit_settlement,
                    invitation_token, invitation_signature, invitation_capacity,
                    invitation_status, invitation_issued_at,
                    archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason,
                    deposit_channel, deposit_settled_at, latest_arrival_at
                )
                VALUES (
                    :id, :bar_id, :merchant_id, :table_id, :customer_id, :reserved_at, :guest_count,
                    :allowed_pass_free_guest_count, :deposit_amount_cent, :deposit_status,
                    :order_source, :sales_staff_id, :status, :created_terminal, :device_id,
                    CAST(:evidence_attachment_ids AS jsonb), :created_at, :updated_at, :deposit_rule,
                    :deposit_intent_id, :deposit_paid_at, :deposit_settlement,
                    :invitation_token, :invitation_signature, :invitation_capacity,
                    :invitation_status, :invitation_issued_at,
                    :archived_at, :archived_by, :delete_status, :delete_requested_by, :delete_requested_at, :delete_reason,
                    :deposit_channel, :deposit_settled_at, :latest_arrival_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    bar_id = EXCLUDED.bar_id,
                    merchant_id = EXCLUDED.merchant_id,
                    table_id = EXCLUDED.table_id,
                    customer_id = EXCLUDED.customer_id,
                    reserved_at = EXCLUDED.reserved_at,
                    guest_count = EXCLUDED.guest_count,
                    allowed_pass_free_guest_count = EXCLUDED.allowed_pass_free_guest_count,
                    deposit_amount_cent = EXCLUDED.deposit_amount_cent,
                    deposit_status = EXCLUDED.deposit_status,
                    order_source = EXCLUDED.order_source,
                    sales_staff_id = EXCLUDED.sales_staff_id,
                    status = EXCLUDED.status,
                    created_terminal = EXCLUDED.created_terminal,
                    device_id = EXCLUDED.device_id,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    updated_at = EXCLUDED.updated_at,
                    deposit_rule = EXCLUDED.deposit_rule,
                    deposit_intent_id = EXCLUDED.deposit_intent_id,
                    deposit_paid_at = EXCLUDED.deposit_paid_at,
                    deposit_settlement = EXCLUDED.deposit_settlement,
                    invitation_token = EXCLUDED.invitation_token,
                    invitation_signature = EXCLUDED.invitation_signature,
                    invitation_capacity = EXCLUDED.invitation_capacity,
                    invitation_status = EXCLUDED.invitation_status,
                    invitation_issued_at = EXCLUDED.invitation_issued_at,
                    archived_at = EXCLUDED.archived_at,
                    archived_by = EXCLUDED.archived_by,
                    delete_status = EXCLUDED.delete_status,
                    delete_requested_by = EXCLUDED.delete_requested_by,
                    delete_requested_at = EXCLUDED.delete_requested_at,
                    delete_reason = EXCLUDED.delete_reason,
                    deposit_channel = EXCLUDED.deposit_channel,
                    deposit_settled_at = EXCLUDED.deposit_settled_at,
                    latest_arrival_at = EXCLUDED.latest_arrival_at
                """, parameterSource.reservationParameters(reservation));
        return reservation;
    }

    // ROOT-03-R19（ENUM 第 100 行「一个台位同一时刻只能存在一笔有效预留（排他锁，不是展示标记）；台位未被占用——既无有效预留也未开台——
    // 时即可再次接受预订」）：hold_status 只由下面三条语句写，saveReservation 不碰它（避免拿旧记录回写把占台状态改掉）。
    @Override
    public boolean holdTable(String reservationId, String tableId) {
        // 台位行锁序列化同一台位的并发占台：后到者在锁上等，拿到锁后再读已是先到者提交后的结果（先付者得、先提交者得）。
        String tableStatus = jdbcTemplate.query("SELECT status FROM bar_tables WHERE id = :table_id FOR UPDATE",
                Map.of("table_id", tableId), (rs, n) -> rs.getString("status")).stream().findFirst().orElse(null);
        if (!"active".equals(tableStatus) || heldByOther(tableId, reservationId)) {
            return false;
        }
        jdbcTemplate.update("UPDATE bar_reservations SET hold_status = 'held', updated_at = now() WHERE id = :id",
                Map.of("id", reservationId));
        return true;
    }

    @Override
    public boolean isTableReservable(String tableId) {
        String tableStatus = jdbcTemplate.query("SELECT status FROM bar_tables WHERE id = :table_id",
                Map.of("table_id", tableId), (rs, n) -> rs.getString("status")).stream().findFirst().orElse(null);
        return "active".equals(tableStatus) && !heldByOther(tableId, "");
    }

    private boolean heldByOther(String tableId, String reservationId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM bar_reservations
                                WHERE table_id = :table_id AND hold_status = 'held' AND id <> :id)
                """, Map.of("table_id", tableId, "id", reservationId), Boolean.class));
    }

    @Override
    public void releaseHold(String reservationId) {
        // 尚未占台（定金未付，NULL）的到店开台同样记 released：超时作业只扫 NULL/held，开台后不会再被当成未到店爽约。
        jdbcTemplate.update("UPDATE bar_reservations SET hold_status = 'released', updated_at = now()"
                + " WHERE id = :id AND (hold_status IS NULL OR hold_status = 'held')", Map.of("id", reservationId));
    }

    @Override
    public Optional<String> findHeldReservationId(String tableId) {
        return jdbcTemplate.queryForList("SELECT id FROM bar_reservations WHERE table_id = :table_id AND hold_status = 'held'",
                Map.of("table_id", tableId), String.class).stream().findFirst();
    }

    /** BSM 第 1062 行：超过最晚到店时间仍未到店（未占台的待付订台，或仍占着台位）的有效订台。到店开台已释放的不在内。 */
    @Override
    public List<String> listOverdueReservationIds(java.time.Instant now) {
        return jdbcTemplate.queryForList("""
                SELECT id FROM bar_reservations
                 WHERE status = 'reserved' AND delete_status <> 'approved'
                   AND latest_arrival_at < :now AND (hold_status IS NULL OR hold_status = 'held')
                 ORDER BY latest_arrival_at, id
                """, Map.of("now", java.sql.Timestamp.from(now)), String.class);
    }

    // OBS-13-03-R1：员工代订录入的客人手机号只经 piiCipher 加密落库（V545 enc:v1: 信封 CHECK；verify-pii-encryption 登记）。
    @Override
    public void saveReservationCustomerPhone(String reservationId, String phone) {
        jdbcTemplate.update("UPDATE bar_reservations SET customer_phone = :customer_phone WHERE id = :id",
                new MapSqlParameterSource().addValue("id", reservationId).addValue("customer_phone", piiCipher.encrypt(phone)));
    }

    @Override
    public Optional<String> findReservationCustomerPhone(String reservationId) {
        return jdbcTemplate.queryForList("SELECT customer_phone FROM bar_reservations WHERE id = :id AND customer_phone IS NOT NULL",
                Map.of("id", reservationId), String.class).stream().findFirst().map(piiCipher::decrypt);
    }

    @Override
    public Optional<BarReservationRecord> findReservationByInvitationToken(String token) {
        return jdbcTemplate.query("""
                SELECT *
                  FROM bar_reservations
                 WHERE invitation_token = :token
                   AND delete_status <> 'approved'
                """, Map.of("token", token), rowMapper::mapReservation).stream().findFirst();
    }

    @Override
    public BarReservationInvitationMemberRecord saveInvitationMember(BarReservationInvitationMemberRecord member) {
        jdbcTemplate.update("""
                INSERT INTO bar_reservation_invitation_members (
                    id, reservation_id, bar_id, member_identifier, member_customer_id,
                    is_holder, status, confirmed_at, entered_at, device_id, created_at, updated_at
                )
                VALUES (
                    :id, :reservation_id, :bar_id, :member_identifier, :member_customer_id,
                    :is_holder, :status, :confirmed_at, :entered_at, :device_id, :created_at, :updated_at
                )
                ON CONFLICT (reservation_id, member_identifier) DO UPDATE SET
                    status = EXCLUDED.status,
                    entered_at = EXCLUDED.entered_at,
                    device_id = EXCLUDED.device_id,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.invitationMemberParameters(member));
        return member;
    }

    @Override
    public List<BarReservationInvitationMemberRecord> listInvitationMembers(String reservationId) {
        return jdbcTemplate.query("""
                SELECT *
                  FROM bar_reservation_invitation_members
                 WHERE reservation_id = :reservation_id
                 ORDER BY is_holder DESC, confirmed_at, id
                """, Map.of("reservation_id", reservationId), rowMapper::mapInvitationMember);
    }

    @Override
    public BarDepositRuleRecord saveDepositRule(BarDepositRuleRecord rule) {
        jdbcTemplate.update("""
                INSERT INTO bar_deposit_rules (
                    id, bar_id, merchant_id, scope_type, scope_id, mode,
                    fixed_amount_cent, ratio_bps, cancel_free_before_minutes, enabled,
                    created_at, updated_at
                )
                VALUES (
                    :id, :bar_id, :merchant_id, :scope_type, :scope_id, :mode,
                    :fixed_amount_cent, :ratio_bps, :cancel_free_before_minutes, :enabled,
                    :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    bar_id = EXCLUDED.bar_id,
                    merchant_id = EXCLUDED.merchant_id,
                    scope_type = EXCLUDED.scope_type,
                    scope_id = EXCLUDED.scope_id,
                    mode = EXCLUDED.mode,
                    fixed_amount_cent = EXCLUDED.fixed_amount_cent,
                    ratio_bps = EXCLUDED.ratio_bps,
                    cancel_free_before_minutes = EXCLUDED.cancel_free_before_minutes,
                    enabled = EXCLUDED.enabled,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.depositRuleParameters(rule));
        return rule;
    }

    @Override
    public List<BarDepositRuleRecord> listDepositRules(String barId) {
        return jdbcTemplate.query("""
                SELECT *
                  FROM bar_deposit_rules
                 WHERE bar_id = :bar_id AND enabled = TRUE
                 ORDER BY scope_type DESC, created_at DESC, id
                """, Map.of("bar_id", barId), rowMapper::mapDepositRule);
    }

    @Override
    public List<BarReservationRecord> listReservations(Optional<String> merchantScope) {
        return listReservations(merchantScope, false);
    }

    // V264：includeArchived=false（默认）排除已存档/已审批删除的预订，与列表偏索引口径一致。
    @Override
    public List<BarReservationRecord> listReservations(Optional<String> merchantScope, boolean includeArchived) {
        StringBuilder sql = new StringBuilder("SELECT * FROM bar_reservations WHERE delete_status <> 'approved'");
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (merchantScope.isPresent()) {
            sql.append(" AND merchant_id = :merchant_id");
            params.addValue("merchant_id", merchantScope.get());
        }
        if (!includeArchived) {
            sql.append(" AND archived_at IS NULL");
        }
        sql.append(" ORDER BY created_at DESC, id");
        return jdbcTemplate.query(sql.toString(), params, rowMapper::mapReservation);
    }

    @Override
    public long countReservationReferences(String id) {
        // 活引用（拒删）：有订单/账单/桌位会话/邀请函成员挂在该预订上（均为 ON DELETE NO ACTION 外键）。
        // bar_table_guest_checkins 走 ON DELETE CASCADE，不算活引用（照范式）。
        Long count = jdbcTemplate.queryForObject("""
                SELECT (SELECT COUNT(1) FROM bar_orders WHERE reservation_id = :id)
                     + (SELECT COUNT(1) FROM bar_bills WHERE reservation_id = :id)
                     + (SELECT COUNT(1) FROM bar_table_sessions WHERE reservation_id = :id)
                     + (SELECT COUNT(1) FROM bar_reservation_invitation_members WHERE reservation_id = :id)
                """, Map.of("id", id), Long.class);
        return count == null ? 0L : count;
    }

    @Override
    public void deleteReservation(String id) {
        jdbcTemplate.update("UPDATE bar_reservations SET delete_status = 'approved', updated_at = now() WHERE id = :id", Map.of("id", id));
    }

    @Override
    public Optional<BarReservationRecord> findReservationById(String id) {
        return jdbcTemplate.query("""
                SELECT *
                  FROM bar_reservations
                 WHERE id = :id
                   AND delete_status <> 'approved'
                """, Map.of("id", id), rowMapper::mapReservation).stream().findFirst();
    }

    @Override
    public Optional<BarReservationRecord> findReservationByIdForUpdate(String id) {
        return jdbcTemplate.query("""
                SELECT *
                  FROM bar_reservations
                 WHERE id = :id
                 FOR UPDATE
                """, Map.of("id", id), rowMapper::mapReservation).stream().findFirst();
    }

    @Override
    public int countGuestCheckins(String reservationId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)::int
                  FROM bar_table_guest_checkins
                 WHERE reservation_id = :reservation_id
                """, Map.of("reservation_id", reservationId), Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    public BarTableGuestCheckinRecord saveGuestCheckin(BarTableGuestCheckinRecord checkin) {
        jdbcTemplate.update("""
                INSERT INTO bar_table_guest_checkins (
                    id, reservation_id, bar_id, table_id, guest_identifier,
                    verification_result, checked_in_count_after, device_id, staff_id,
                    evidence_attachment_ids, created_at
                )
                VALUES (
                    :id, :reservation_id, :bar_id, :table_id, :guest_identifier,
                    :verification_result, :checked_in_count_after, :device_id, :staff_id,
                    CAST(:evidence_attachment_ids AS jsonb), :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    verification_result = EXCLUDED.verification_result,
                    checked_in_count_after = EXCLUDED.checked_in_count_after,
                    device_id = EXCLUDED.device_id,
                    staff_id = EXCLUDED.staff_id,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids
                """, parameterSource.checkinParameters(checkin));
        return checkin;
    }

    @Override
    public BarProductRecord saveProduct(BarProductRecord product) {
        jdbcTemplate.update("""
                INSERT INTO bar_products (
                    id, bar_id, merchant_id, product_name, product_type, price_cent, currency,
                    inventory_item_id, stock_location_id, stock_deduction_required,
                    status, created_at, updated_at,
                    standard_pour_volume, pour_volume_unit, loss_threshold,
                    sold_out, sold_out_at, sold_out_reason, internal_settlement_price_cent,
                    category_id, image_url
                )
                VALUES (
                    :id, :bar_id, :merchant_id, :product_name, :product_type, :price_cent, :currency,
                    :inventory_item_id, :stock_location_id, :stock_deduction_required,
                    :status, :created_at, :updated_at,
                    :standard_pour_volume, :pour_volume_unit, :loss_threshold,
                    :sold_out, :sold_out_at, :sold_out_reason, :internal_settlement_price_cent,
                    :category_id, :image_url
                )
                ON CONFLICT (id) DO UPDATE SET
                    bar_id = EXCLUDED.bar_id,
                    merchant_id = EXCLUDED.merchant_id,
                    product_name = EXCLUDED.product_name,
                    product_type = EXCLUDED.product_type,
                    price_cent = EXCLUDED.price_cent,
                    currency = EXCLUDED.currency,
                    inventory_item_id = EXCLUDED.inventory_item_id,
                    stock_location_id = EXCLUDED.stock_location_id,
                    stock_deduction_required = EXCLUDED.stock_deduction_required,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at,
                    standard_pour_volume = EXCLUDED.standard_pour_volume,
                    pour_volume_unit = EXCLUDED.pour_volume_unit,
                    loss_threshold = EXCLUDED.loss_threshold,
                    sold_out = EXCLUDED.sold_out,
                    sold_out_at = EXCLUDED.sold_out_at,
                    sold_out_reason = EXCLUDED.sold_out_reason,
                    internal_settlement_price_cent = EXCLUDED.internal_settlement_price_cent,
                    category_id = EXCLUDED.category_id,
                    image_url = EXCLUDED.image_url
                """, parameterSource.productParameters(product));
        return product;
    }

    @Override
    public List<BarProductRecord> listProducts(Optional<String> merchantScope) {
        if (merchantScope.isEmpty()) {
            return jdbcTemplate.query("""
                    SELECT *
                      FROM bar_products
                     ORDER BY created_at DESC, id
                    """, rowMapper::mapProduct);
        }
        return jdbcTemplate.query("""
                SELECT *
                  FROM bar_products
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantScope.get()), rowMapper::mapProduct);
    }

    @Override
    public Optional<BarProductRecord> findProductById(String id) {
        return jdbcTemplate.query("""
                SELECT *
                  FROM bar_products
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapProduct).stream().findFirst();
    }

    @Override
    public void setReservationArchiveReason(String id, String reason) {
        jdbcTemplate.update("UPDATE bar_reservations SET archive_reason = :reason WHERE id = :id",
                new MapSqlParameterSource("reason", reason).addValue("id", id));
    }

    // ── 订单 / 明细 / 退酒审批 / 出品 / 赠饮核销 ─────────────────────────────────
    @Override
    public BarOrderRecord saveOrder(BarOrderRecord order) {
        jdbcTemplate.update("""
                INSERT INTO bar_orders (
                    id, bar_id, merchant_id, table_id, reservation_id, customer_id, order_source,
                    status, total_amount_cent, currency, created_terminal, device_id,
                    sales_staff_id, created_at, updated_at, order_type, paid_at
                )
                VALUES (
                    :id, :bar_id, :merchant_id, :table_id, :reservation_id, :customer_id, :order_source,
                    :status, :total_amount_cent, :currency, :created_terminal, :device_id,
                    :sales_staff_id, :created_at, :updated_at, :order_type, :paid_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    bar_id = EXCLUDED.bar_id,
                    merchant_id = EXCLUDED.merchant_id,
                    table_id = EXCLUDED.table_id,
                    reservation_id = EXCLUDED.reservation_id,
                    customer_id = EXCLUDED.customer_id,
                    order_source = EXCLUDED.order_source,
                    status = EXCLUDED.status,
                    total_amount_cent = EXCLUDED.total_amount_cent,
                    currency = EXCLUDED.currency,
                    created_terminal = EXCLUDED.created_terminal,
                    device_id = EXCLUDED.device_id,
                    sales_staff_id = EXCLUDED.sales_staff_id,
                    updated_at = EXCLUDED.updated_at,
                    order_type = EXCLUDED.order_type,
                    -- V231：结账回填的订单 paid_at 随 upsert 落库；stampOrderPaidAt 只在为空时首次写。
                    -- 第十一批 TK-04-R3：已付时点一经写入不被后续状态写（如结账后出品送达用旧构造器存单）抹空（BSM L831 支付成功时点锁定）。
                    paid_at = COALESCE(EXCLUDED.paid_at, bar_orders.paid_at)
                """, parameterSource.orderParameters(order));
        return order;
    }

    @Override
    public List<BarOrderRecord> listOrders(Optional<String> merchantScope) {
        if (merchantScope.isEmpty()) {
            return jdbcTemplate.query("""
                    SELECT id, bar_id, merchant_id, table_id, reservation_id, customer_id, order_source,
                           status, total_amount_cent, currency, created_terminal, device_id,
                           sales_staff_id, created_at, updated_at, order_type, paid_at
                      FROM bar_orders
                     ORDER BY created_at DESC, id
                    """, rowMapper::mapOrder);
        }
        return jdbcTemplate.query("""
                SELECT id, bar_id, merchant_id, table_id, reservation_id, customer_id, order_source,
                       status, total_amount_cent, currency, created_terminal, device_id,
                       sales_staff_id, created_at, updated_at, order_type, paid_at
                  FROM bar_orders
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantScope.get()), rowMapper::mapOrder);
    }

    @Override
    public Optional<BarOrderRecord> findOrderById(String id) {
        return jdbcTemplate.query("""
                SELECT id, bar_id, merchant_id, table_id, reservation_id, customer_id, order_source,
                       status, total_amount_cent, currency, created_terminal, device_id,
                       sales_staff_id, created_at, updated_at, order_type, paid_at
                  FROM bar_orders
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapOrder).stream().findFirst();
    }

    @Override
    public BarOrderItemRecord saveOrderItem(BarOrderItemRecord item) {
        jdbcTemplate.update("""
                INSERT INTO bar_order_items (
                    id, order_id, product_id, product_name, quantity, unit_price_cent,
                    amount_cent, delivery_status, created_at, updated_at,
                    actual_pour_volume, loss_volume,
                    voided, void_reason, voided_at, voided_by_user_id, return_to_stock,
                    is_gift, delivered_at
                )
                VALUES (
                    :id, :order_id, :product_id, :product_name, :quantity, :unit_price_cent,
                    :amount_cent, :delivery_status, :created_at, :updated_at,
                    :actual_pour_volume, :loss_volume,
                    :voided, :void_reason, :voided_at, :voided_by_user_id, :return_to_stock,
                    :is_gift, :delivered_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    product_name = EXCLUDED.product_name,
                    quantity = EXCLUDED.quantity,
                    unit_price_cent = EXCLUDED.unit_price_cent,
                    amount_cent = EXCLUDED.amount_cent,
                    delivery_status = EXCLUDED.delivery_status,
                    updated_at = EXCLUDED.updated_at,
                    actual_pour_volume = EXCLUDED.actual_pour_volume,
                    loss_volume = EXCLUDED.loss_volume,
                    voided = EXCLUDED.voided,
                    void_reason = EXCLUDED.void_reason,
                    voided_at = EXCLUDED.voided_at,
                    voided_by_user_id = EXCLUDED.voided_by_user_id,
                    return_to_stock = EXCLUDED.return_to_stock,
                    is_gift = EXCLUDED.is_gift,
                    delivered_at = EXCLUDED.delivered_at
                """, parameterSource.orderItemParameters(item));
        return item;
    }

    @Override
    public List<BarOrderItemRecord> listOrderItems(String orderId) {
        return jdbcTemplate.query("""
                SELECT *
                  FROM bar_order_items
                 WHERE order_id = :order_id
                 ORDER BY created_at, id
                """, Map.of("order_id", orderId), rowMapper::mapOrderItem);
    }

    @Override
    public Optional<BarOrderItemRecord> findOrderItemById(String id) {
        return jdbcTemplate.query("""
                SELECT *
                  FROM bar_order_items
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapOrderItem).stream().findFirst();
    }

    @Override
    public BarOrderItemVoidRecord saveOrderItemVoid(BarOrderItemVoidRecord voidRecord) {
        jdbcTemplate.update("""
                INSERT INTO bar_order_item_voids (
                    id, order_item_id, order_id, bar_id, merchant_id, void_type, quantity,
                    amount_cent, was_delivered, return_to_stock, inventory_movement_id,
                    bill_id, bill_adjustment_id, reason, requested_by_user_id, approval_required,
                    status, idempotency_key, evidence_attachment_ids, created_at,
                    reviewed_by_user_id, reviewed_at, review_reason,
                    approval_mode, reason_code, high_value_threshold_cent, effects_applied_at
                )
                VALUES (
                    :id, :order_item_id, :order_id, :bar_id, :merchant_id, :void_type, :quantity,
                    :amount_cent, :was_delivered, :return_to_stock, :inventory_movement_id,
                    :bill_id, :bill_adjustment_id, :reason, :requested_by_user_id, :approval_required,
                    :status, :idempotency_key, CAST(:evidence_attachment_ids AS jsonb), :created_at,
                    :reviewed_by_user_id, :reviewed_at, :review_reason,
                    :approval_mode, :reason_code, :high_value_threshold_cent, :effects_applied_at
                )
                ON CONFLICT (order_item_id, idempotency_key) DO UPDATE SET
                    void_type = EXCLUDED.void_type,
                    quantity = EXCLUDED.quantity,
                    amount_cent = EXCLUDED.amount_cent,
                    was_delivered = EXCLUDED.was_delivered,
                    return_to_stock = EXCLUDED.return_to_stock,
                    inventory_movement_id = EXCLUDED.inventory_movement_id,
                    bill_id = EXCLUDED.bill_id,
                    bill_adjustment_id = EXCLUDED.bill_adjustment_id,
                    reason = EXCLUDED.reason,
                    status = EXCLUDED.status,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    reviewed_by_user_id = EXCLUDED.reviewed_by_user_id,
                    reviewed_at = EXCLUDED.reviewed_at,
                    review_reason = EXCLUDED.review_reason,
                    approval_mode = EXCLUDED.approval_mode,
                    reason_code = EXCLUDED.reason_code,
                    high_value_threshold_cent = EXCLUDED.high_value_threshold_cent,
                    effects_applied_at = EXCLUDED.effects_applied_at
                """, parameterSource.orderItemVoidParameters(voidRecord));
        return voidRecord;
    }

    @Override
    public Optional<BarOrderItemVoidRecord> findOrderItemVoidById(String id) {
        return jdbcTemplate.query("""
                SELECT * FROM bar_order_item_voids WHERE id = :id
                """, Map.of("id", id), rowMapper::mapOrderItemVoid).stream().findFirst();
    }

    @Override
    public Optional<BarOrderItemVoidRecord> findOrderItemVoidByIdForUpdate(String id) {
        // V125 P1-1：行锁读，使审批决策的 status==pending 守卫与翻转在同一事务串行化，
        // 并发第二次 reject 阻塞至首事务提交后读到 rejected/approved 被 BAR_ORDER_ITEM_VOID_NOT_PENDING 拦截。
        return jdbcTemplate.query("""
                SELECT * FROM bar_order_item_voids WHERE id = :id FOR UPDATE
                """, Map.of("id", id), rowMapper::mapOrderItemVoid).stream().findFirst();
    }

    @Override
    public List<BarOrderItemVoidRecord> listOrderItemVoids(String orderId) {
        return jdbcTemplate.query("""
                SELECT * FROM bar_order_item_voids WHERE order_id = :order_id ORDER BY created_at, id
                """, Map.of("order_id", orderId), rowMapper::mapOrderItemVoid);
    }

    /**
     * V345 退酒审批待审队列：按状态取申请，商户 scope 非空时下沉为 SQL 谓词（AC-ISO-1 租户隔离），
     * 平台域调用方（无商户 scope）看全域——与 {@link #listProductionTickets} 同形。
     */
    @Override
    public List<BarOrderItemVoidRecord> listOrderItemVoidsByStatus(Optional<String> merchantScope, String status) {
        if (merchantScope.isEmpty()) {
            return jdbcTemplate.query("""
                    SELECT * FROM bar_order_item_voids
                     WHERE status = :status
                     ORDER BY created_at DESC, id
                    """, Map.of("status", status), rowMapper::mapOrderItemVoid);
        }
        return jdbcTemplate.query("""
                SELECT * FROM bar_order_item_voids
                 WHERE status = :status AND merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("status", status, "merchant_id", merchantScope.get()), rowMapper::mapOrderItemVoid);
    }

    @Override
    public BarProductionTicketRecord saveProductionTicket(BarProductionTicketRecord ticket) {
        jdbcTemplate.update("""
                INSERT INTO bar_production_tickets (
                    id, order_id, bar_id, merchant_id, ticket_status, created_at, updated_at
                )
                VALUES (
                    :id, :order_id, :bar_id, :merchant_id, :ticket_status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    merchant_id = EXCLUDED.merchant_id,
                    ticket_status = EXCLUDED.ticket_status,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.productionTicketParameters(ticket));
        return ticket;
    }

    @Override
    public List<BarProductionTicketRecord> listProductionTickets(Optional<String> merchantScope) {
        if (merchantScope.isEmpty()) {
            return jdbcTemplate.query("""
                    SELECT id, order_id, bar_id, merchant_id, ticket_status, created_at, updated_at
                      FROM bar_production_tickets
                     ORDER BY created_at DESC, id
                    """, rowMapper::mapProductionTicket);
        }
        return jdbcTemplate.query("""
                SELECT id, order_id, bar_id, merchant_id, ticket_status, created_at, updated_at
                  FROM bar_production_tickets
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantScope.get()), rowMapper::mapProductionTicket);
    }

    @Override
    public Optional<BarProductionTicketRecord> findProductionTicketById(String id) {
        return jdbcTemplate.query("""
                SELECT id, order_id, bar_id, merchant_id, ticket_status, created_at, updated_at
                  FROM bar_production_tickets
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapProductionTicket).stream().findFirst();
    }

    @Override
    public Optional<BarProductionTicketRecord> findProductionTicketByOrderId(String orderId) {
        return jdbcTemplate.query("""
                SELECT id, order_id, bar_id, merchant_id, ticket_status, created_at, updated_at
                  FROM bar_production_tickets
                 WHERE order_id = :order_id
                 ORDER BY created_at DESC, id
                 LIMIT 1
                """, Map.of("order_id", orderId), rowMapper::mapProductionTicket).stream().findFirst();
    }

    @Override
    public BarProductionTicketEventRecord saveProductionTicketEvent(BarProductionTicketEventRecord event) {
        jdbcTemplate.update("""
                INSERT INTO bar_production_ticket_events (
                    id, ticket_id, order_id, bar_id, action_type, from_status, to_status,
                    station_code, operator_user_id, operator_note, idempotency_key,
                    evidence_attachment_ids, created_at, inventory_movement_id
                )
                VALUES (
                    :id, :ticket_id, :order_id, :bar_id, :action_type, :from_status, :to_status,
                    :station_code, :operator_user_id, :operator_note, :idempotency_key,
                    CAST(:evidence_attachment_ids AS jsonb), :created_at, :inventory_movement_id
                )
                ON CONFLICT (ticket_id, idempotency_key) DO UPDATE SET
                    action_type = EXCLUDED.action_type,
                    from_status = EXCLUDED.from_status,
                    to_status = EXCLUDED.to_status,
                    station_code = EXCLUDED.station_code,
                    operator_user_id = EXCLUDED.operator_user_id,
                    operator_note = EXCLUDED.operator_note,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    inventory_movement_id = EXCLUDED.inventory_movement_id
                """, parameterSource.productionTicketEventParameters(event));
        return event;
    }

    @Override
    public List<BarProductionTicketEventRecord> listProductionTicketEvents(String ticketId) {
        return jdbcTemplate.query("""
                SELECT id, ticket_id, order_id, bar_id, action_type, from_status, to_status,
                       station_code, operator_user_id, operator_note, idempotency_key,
                       evidence_attachment_ids, created_at, inventory_movement_id
                  FROM bar_production_ticket_events
                 WHERE ticket_id = :ticket_id
                 ORDER BY created_at DESC, id
                """, Map.of("ticket_id", ticketId), rowMapper::mapProductionTicketEvent);
    }

    @Override
    public boolean insertGiftRedemptionIfAbsent(BarGiftRedemptionRecord redemption) {
        // 幂等：uq_bar_gift_redemption_verification 唯一键防一次核销重复生成单；撞唯一键即 DO NOTHING（返回 0 行）。
        int affected = jdbcTemplate.update("""
                INSERT INTO bar_gift_redemptions (
                    id, entitlement_id, verification_id, bar_order_id, bar_id, merchant_id,
                    item_ref, inventory_movement_id, status, note, created_at, updated_at
                )
                VALUES (
                    :id, :entitlement_id, :verification_id, :bar_order_id, :bar_id, :merchant_id,
                    :item_ref, :inventory_movement_id, :status, :note, :created_at, :updated_at
                )
                ON CONFLICT (verification_id) DO NOTHING
                """, parameterSource.giftRedemptionParameters(redemption));
        return affected > 0;
    }

    @Override
    public BarGiftRedemptionRecord saveGiftRedemption(BarGiftRedemptionRecord redemption) {
        jdbcTemplate.update("""
                INSERT INTO bar_gift_redemptions (
                    id, entitlement_id, verification_id, bar_order_id, bar_id, merchant_id,
                    item_ref, inventory_movement_id, status, note, created_at, updated_at
                )
                VALUES (
                    :id, :entitlement_id, :verification_id, :bar_order_id, :bar_id, :merchant_id,
                    :item_ref, :inventory_movement_id, :status, :note, :created_at, :updated_at
                )
                ON CONFLICT (verification_id) DO UPDATE SET
                    bar_order_id = EXCLUDED.bar_order_id,
                    item_ref = EXCLUDED.item_ref,
                    inventory_movement_id = EXCLUDED.inventory_movement_id,
                    status = EXCLUDED.status,
                    note = EXCLUDED.note,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.giftRedemptionParameters(redemption));
        return redemption;
    }

    @Override
    public Optional<BarGiftRedemptionRecord> findGiftRedemptionByVerification(String verificationId) {
        return jdbcTemplate.query("""
                SELECT id, entitlement_id, verification_id, bar_order_id, bar_id, merchant_id,
                       item_ref, inventory_movement_id, status, note, created_at, updated_at
                  FROM bar_gift_redemptions
                 WHERE verification_id = :verification_id
                """, Map.of("verification_id", verificationId), rowMapper::mapGiftRedemption).stream().findFirst();
    }

    @Override
    public List<BarGiftRedemptionRecord> listGiftRedemptions(Optional<String> merchantScope) {
        if (merchantScope.isEmpty()) {
            return jdbcTemplate.query("""
                    SELECT id, entitlement_id, verification_id, bar_order_id, bar_id, merchant_id,
                           item_ref, inventory_movement_id, status, note, created_at, updated_at
                      FROM bar_gift_redemptions
                     ORDER BY created_at DESC, id
                    """, rowMapper::mapGiftRedemption);
        }
        return jdbcTemplate.query("""
                SELECT id, entitlement_id, verification_id, bar_order_id, bar_id, merchant_id,
                       item_ref, inventory_movement_id, status, note, created_at, updated_at
                  FROM bar_gift_redemptions
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantScope.get()), rowMapper::mapGiftRedemption);
    }

    // ── 账单 / 结算 / 调整 / 交班 / 提成 ─────────────────────────────────────────
    @Override
    public BarBillRecord saveBill(BarBillRecord bill) {
        jdbcTemplate.update("""
                INSERT INTO bar_bills (
                    id, bill_no, order_id, bar_id, merchant_id, table_id, reservation_id,
                    subtotal_amount_cent, discount_amount_cent, deposit_deduct_amount_cent,
                    payable_amount_cent, paid_amount_cent, currency, payment_status,
                    settlement_status, sales_staff_id, sales_role_type, created_at, updated_at,
                    reopened, reopened_by_user_id, reopened_at, reopen_reason, reopen_count, tip_amount_cent,
                    paid_at
                )
                VALUES (
                    :id, :bill_no, :order_id, :bar_id, :merchant_id, :table_id, :reservation_id,
                    :subtotal_amount_cent, :discount_amount_cent, :deposit_deduct_amount_cent,
                    :payable_amount_cent, :paid_amount_cent, :currency, :payment_status,
                    :settlement_status, :sales_staff_id, :sales_role_type, :created_at, :updated_at,
                    :reopened, :reopened_by_user_id, :reopened_at, :reopen_reason, :reopen_count, :tip_amount_cent,
                    :paid_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    merchant_id = EXCLUDED.merchant_id,
                    subtotal_amount_cent = EXCLUDED.subtotal_amount_cent,
                    discount_amount_cent = EXCLUDED.discount_amount_cent,
                    deposit_deduct_amount_cent = EXCLUDED.deposit_deduct_amount_cent,
                    payable_amount_cent = EXCLUDED.payable_amount_cent,
                    paid_amount_cent = EXCLUDED.paid_amount_cent,
                    payment_status = EXCLUDED.payment_status,
                    settlement_status = EXCLUDED.settlement_status,
                    sales_staff_id = EXCLUDED.sales_staff_id,
                    sales_role_type = EXCLUDED.sales_role_type,
                    updated_at = EXCLUDED.updated_at,
                    reopened = EXCLUDED.reopened,
                    reopened_by_user_id = EXCLUDED.reopened_by_user_id,
                    reopened_at = EXCLUDED.reopened_at,
                    reopen_reason = EXCLUDED.reopen_reason,
                    reopen_count = EXCLUDED.reopen_count,
                    tip_amount_cent = EXCLUDED.tip_amount_cent,
                    -- V231：结账成功写入的 paid_at 随 upsert 落库（billWithAmounts 已保证已 paid 不覆盖、反结清空）。
                    paid_at = EXCLUDED.paid_at
                """, parameterSource.billParameters(bill));
        return bill;
    }

    @Override
    public BarBillSettlementRecord saveBillSettlement(BarBillSettlementRecord settlement) {
        jdbcTemplate.update("""
                INSERT INTO bar_bill_settlements (
                    id, bill_id, settle_channel, cashier_staff_id, pos_transaction_no,
                    settlement_amount_cent, merchant_id, settled_at
                )
                VALUES (
                    :id, :bill_id, :settle_channel, :cashier_staff_id, :pos_transaction_no,
                    :settlement_amount_cent, :merchant_id, :settled_at
                )
                ON CONFLICT (id) DO NOTHING
                """, parameterSource.billSettlementParameters(settlement));
        return settlement;
    }

    @Override
    public List<BarBillSettlementRecord> listBillSettlements(String billId) {
        return jdbcTemplate.query("""
                SELECT * FROM bar_bill_settlements WHERE bill_id = :bill_id ORDER BY settled_at, id
                """, Map.of("bill_id", billId), rowMapper::mapBillSettlement);
    }

    @Override
    public Optional<BarBillRecord> findBillById(String id) {
        return jdbcTemplate.query("""
                SELECT id, bill_no, order_id, bar_id, merchant_id, table_id, reservation_id,
                       subtotal_amount_cent, discount_amount_cent, deposit_deduct_amount_cent,
                       payable_amount_cent, paid_amount_cent, currency, payment_status,
                       settlement_status, sales_staff_id, sales_role_type, created_at, updated_at,
                       reopened, reopened_by_user_id, reopened_at, reopen_reason, reopen_count, tip_amount_cent,
                       paid_at
                  FROM bar_bills
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapBill).stream().findFirst();
    }

    // 2c-2 结算/计提行锁：SELECT ... FOR UPDATE 序列化并发 settle/commission，防 TOCTOU 重复结算/重复计提。
    @Override
    public Optional<BarBillRecord> findBillByIdForUpdate(String id) {
        return jdbcTemplate.query("""
                SELECT id, bill_no, order_id, bar_id, merchant_id, table_id, reservation_id,
                       subtotal_amount_cent, discount_amount_cent, deposit_deduct_amount_cent,
                       payable_amount_cent, paid_amount_cent, currency, payment_status,
                       settlement_status, sales_staff_id, sales_role_type, created_at, updated_at,
                       reopened, reopened_by_user_id, reopened_at, reopen_reason, reopen_count, tip_amount_cent,
                       paid_at
                  FROM bar_bills
                 WHERE id = :id
                 FOR UPDATE
                """, Map.of("id", id), rowMapper::mapBill).stream().findFirst();
    }

    @Override
    public List<BarBillRecord> listBillsByOrderIds(List<String> orderIds) {
        if (orderIds.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT id, bill_no, order_id, bar_id, merchant_id, table_id, reservation_id,
                       subtotal_amount_cent, discount_amount_cent, deposit_deduct_amount_cent,
                       payable_amount_cent, paid_amount_cent, currency, payment_status,
                       settlement_status, sales_staff_id, sales_role_type, created_at, updated_at,
                       reopened, reopened_by_user_id, reopened_at, reopen_reason, reopen_count, tip_amount_cent,
                       paid_at
                  FROM bar_bills
                 WHERE order_id IN (:order_ids)
                 ORDER BY created_at DESC, id
                """, Map.of("order_ids", orderIds), rowMapper::mapBill);
    }

    // 2c-2 提成防重：按 bill_id 查已有提成计算（calculateCommission 查重守卫用，避免同一账单多幂等键重复计提）。
    @Override
    public Optional<BarCommissionCalculationRecord> findCommissionByBillId(String billId) {
        return jdbcTemplate.query("""
                SELECT id, bill_id, sales_staff_id, sales_role_type, commission_base_cent,
                       commission_rate_bp, commission_amount_cent, status, created_at
                  FROM bar_commission_calculations
                 WHERE bill_id = :bill_id
                 ORDER BY created_at ASC, id
                 LIMIT 1
                """, Map.of("bill_id", billId), rowMapper::mapCommission).stream().findFirst();
    }

    // G/V349：订单列表批量带出提成（避免逐单 N+1）。同一账单至多一条（计提防重），故不做去重。
    @Override
    public List<BarCommissionCalculationRecord> listCommissionsByBillIds(List<String> billIds) {
        if (billIds.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT id, bill_id, sales_staff_id, sales_role_type, commission_base_cent,
                       commission_rate_bp, commission_amount_cent, status, created_at
                  FROM bar_commission_calculations
                 WHERE bill_id IN (:bill_ids)
                 ORDER BY created_at ASC, id
                """, Map.of("bill_ids", billIds), rowMapper::mapCommission);
    }

    // 2c-3 reopen 冲正：把账单现有结算流水标 reversed（聚合侧排除），避免反结→重结被双计。
    @Override
    public int markBillSettlementsReversed(String billId) {
        return jdbcTemplate.update("""
                UPDATE bar_bill_settlements
                   SET reversed = TRUE
                 WHERE bill_id = :bill_id
                   AND reversed = FALSE
                """, Map.of("bill_id", billId));
    }

    @Override
    public BarBillAdjustmentRecord saveBillAdjustment(BarBillAdjustmentRecord adjustment) {
        jdbcTemplate.update("""
                INSERT INTO bar_bill_adjustments (
                    id, bill_id, adjustment_type, amount_cent,
                    before_payable_amount_cent, after_payable_amount_cent,
                    reason, requested_by_user_id, approval_required, status,
                    idempotency_key, evidence_attachment_ids, ar_ap_item_id, snapshot_json,
                    created_at, refunded_at
                )
                VALUES (
                    :id, :bill_id, :adjustment_type, :amount_cent,
                    :before_payable_amount_cent, :after_payable_amount_cent,
                    :reason, :requested_by_user_id, :approval_required, :status,
                    :idempotency_key, CAST(:evidence_attachment_ids AS jsonb),
                    :ar_ap_item_id, CAST(:snapshot_json AS jsonb), :created_at, :refunded_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    amount_cent = EXCLUDED.amount_cent,
                    before_payable_amount_cent = EXCLUDED.before_payable_amount_cent,
                    after_payable_amount_cent = EXCLUDED.after_payable_amount_cent,
                    reason = EXCLUDED.reason,
                    requested_by_user_id = EXCLUDED.requested_by_user_id,
                    approval_required = EXCLUDED.approval_required,
                    status = EXCLUDED.status,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    ar_ap_item_id = EXCLUDED.ar_ap_item_id,
                    snapshot_json = EXCLUDED.snapshot_json,
                    refunded_at = EXCLUDED.refunded_at
                """, parameterSource.billAdjustmentParameters(adjustment));
        return adjustment;
    }

    @Override
    public List<BarBillAdjustmentRecord> listBillAdjustments(String billId) {
        return jdbcTemplate.query("""
                SELECT id, bill_id, adjustment_type, amount_cent,
                       before_payable_amount_cent, after_payable_amount_cent,
                       reason, requested_by_user_id, approval_required, status,
                       idempotency_key, evidence_attachment_ids, ar_ap_item_id, snapshot_json,
                       created_at, refunded_at
                  FROM bar_bill_adjustments
                 WHERE bill_id = :bill_id
                 ORDER BY created_at, id
                """, Map.of("bill_id", billId), rowMapper::mapBillAdjustment);
    }

    @Override
    public BarBillOperationRecord saveBillOperation(BarBillOperationRecord operation) {
        jdbcTemplate.update("""
                INSERT INTO bar_bill_operations (
                    id, operation_type, primary_bill_id, secondary_bill_id, generated_bill_id,
                    amount_cent, reason, requested_by_user_id, status, idempotency_key,
                    evidence_attachment_ids, snapshot_json, created_at
                )
                VALUES (
                    :id, :operation_type, :primary_bill_id, :secondary_bill_id, :generated_bill_id,
                    :amount_cent, :reason, :requested_by_user_id, :status, :idempotency_key,
                    CAST(:evidence_attachment_ids AS jsonb), CAST(:snapshot_json AS jsonb), :created_at
                )
                ON CONFLICT (primary_bill_id, idempotency_key) DO UPDATE SET
                    secondary_bill_id = EXCLUDED.secondary_bill_id,
                    generated_bill_id = EXCLUDED.generated_bill_id,
                    amount_cent = EXCLUDED.amount_cent,
                    reason = EXCLUDED.reason,
                    requested_by_user_id = EXCLUDED.requested_by_user_id,
                    status = EXCLUDED.status,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    snapshot_json = EXCLUDED.snapshot_json
                """, parameterSource.billOperationParameters(operation));
        return operation;
    }

    @Override
    public List<BarBillOperationRecord> listBillOperations(String billId) {
        return jdbcTemplate.query("""
                SELECT id, operation_type, primary_bill_id, secondary_bill_id, generated_bill_id,
                       amount_cent, reason, requested_by_user_id, status, idempotency_key,
                       evidence_attachment_ids, snapshot_json, created_at
                  FROM bar_bill_operations
                 WHERE primary_bill_id = :bill_id OR secondary_bill_id = :bill_id OR generated_bill_id = :bill_id
                 ORDER BY created_at, id
                """, Map.of("bill_id", billId), rowMapper::mapBillOperation);
    }

    // ── V095 交班 / 班结对账 ──────────────────────────────────────────────────
    @Override
    public BarShiftRecord saveShift(BarShiftRecord shift) {
        jdbcTemplate.update("""
                INSERT INTO bar_shifts (
                    id, project_id, merchant_id, bar_id, cashier_user_id, status,
                    opened_at, closed_at, opening_cash_cent, declared_cash_cent,
                    system_cash_cent, system_pos_cent, system_online_cent, system_refund_cent, system_tip_cent,
                    cash_diff_cent, cash_diff_threshold_cent, needs_review, handover_to_user_id, note,
                    opened_by_user_id, closed_by_user_id, created_at, updated_at
                )
                VALUES (
                    :id, :project_id, :merchant_id, :bar_id, :cashier_user_id, :status,
                    :opened_at, :closed_at, :opening_cash_cent, :declared_cash_cent,
                    :system_cash_cent, :system_pos_cent, :system_online_cent, :system_refund_cent, :system_tip_cent,
                    :cash_diff_cent, :cash_diff_threshold_cent, :needs_review, :handover_to_user_id, :note,
                    :opened_by_user_id, :closed_by_user_id, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    closed_at = EXCLUDED.closed_at,
                    declared_cash_cent = EXCLUDED.declared_cash_cent,
                    system_cash_cent = EXCLUDED.system_cash_cent,
                    system_pos_cent = EXCLUDED.system_pos_cent,
                    system_online_cent = EXCLUDED.system_online_cent,
                    system_refund_cent = EXCLUDED.system_refund_cent,
                    system_tip_cent = EXCLUDED.system_tip_cent,
                    cash_diff_cent = EXCLUDED.cash_diff_cent,
                    cash_diff_threshold_cent = EXCLUDED.cash_diff_threshold_cent,
                    needs_review = EXCLUDED.needs_review,
                    handover_to_user_id = EXCLUDED.handover_to_user_id,
                    note = EXCLUDED.note,
                    closed_by_user_id = EXCLUDED.closed_by_user_id,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.shiftParameters(shift));
        return shift;
    }

    @Override
    public Optional<BarShiftRecord> findShiftById(String id) {
        return jdbcTemplate.query("SELECT * FROM bar_shifts WHERE id = :id",
                Map.of("id", id), rowMapper::mapShift).stream().findFirst();
    }

    @Override
    public Optional<BarShiftRecord> findOpenShift(String barId, String cashierUserId) {
        return jdbcTemplate.query("""
                SELECT * FROM bar_shifts
                 WHERE bar_id = :bar_id AND cashier_user_id = :cashier_user_id AND status = 'open'
                 ORDER BY opened_at DESC
                 LIMIT 1
                """, Map.of("bar_id", barId, "cashier_user_id", cashierUserId), rowMapper::mapShift)
                .stream().findFirst();
    }

    @Override
    public List<BarShiftRecord> listShifts(Optional<String> merchantScope) {
        if (merchantScope.isPresent()) {
            return jdbcTemplate.query("""
                    SELECT * FROM bar_shifts WHERE merchant_id = :merchant_id ORDER BY opened_at DESC, id
                    """, Map.of("merchant_id", merchantScope.get()), rowMapper::mapShift);
        }
        return jdbcTemplate.query("SELECT * FROM bar_shifts ORDER BY opened_at DESC, id", Map.of(), rowMapper::mapShift);
    }

    // P2 多综合体 project_id scoped 读通道：按真实 project_id 列做行级隔离（AC-ISO-1，§4）。既有查询不变。
    public List<BarShiftRecord> listShiftsByProject(String projectId) {
        return jdbcTemplate.query("""
                SELECT * FROM bar_shifts WHERE project_id = :project_id ORDER BY opened_at DESC, id
                """, Map.of("project_id", projectId), rowMapper::mapShift);
    }

    @Override
    public BarShiftReconciliationSnapshot aggregateShiftReconciliation(
            String barId,
            String cashierUserId,
            Instant from,
            Instant to,
            Optional<String> merchantScope
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("bar_id", barId)
                .addValue("cashier_user_id", cashierUserId)
                .addValue("from_ts", timestampWithTimeZone(from))
                .addValue("to_ts", timestampWithTimeZone(to))
                .addValue("merchant_id", merchantScope.orElse(null));
        String merchantFilter = merchantScope.isPresent() ? " AND b.merchant_id = :merchant_id" : "";
        // 各支付方式应收：本吧台 + 本收银员 + settled_at 落窗口的结算流水，按 settle_channel 归并。
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT
                  COALESCE(SUM(CASE WHEN s.settle_channel = 'cash' THEN s.settlement_amount_cent ELSE 0 END), 0) AS cash_cent,
                  COALESCE(SUM(CASE WHEN s.settle_channel = 'pos' THEN s.settlement_amount_cent ELSE 0 END), 0) AS pos_cent,
                  COALESCE(SUM(CASE WHEN s.settle_channel IN ('aggregate_pay', 'bank_transfer', 'other')
                                    THEN s.settlement_amount_cent ELSE 0 END), 0) AS online_cent
                  FROM bar_bill_settlements s
                  JOIN bar_bills b ON b.id = s.bill_id
                 WHERE b.bar_id = :bar_id
                   AND s.cashier_staff_id = :cashier_user_id
                   AND s.reversed = FALSE
                   AND s.settled_at >= :from_ts AND s.settled_at < :to_ts""" + merchantFilter,
                new HashMap<>(params.getValues()));
        long cash = ((Number) row.get("cash_cent")).longValue();
        long pos = ((Number) row.get("pos_cent")).longValue();
        long online = ((Number) row.get("online_cent")).longValue();
        // 退款：本吧台 + refunded_at 落窗口的 refund 调整额，净额扣减 refund_reversal（延期项④：
        // 退菜退酒审批驳回的退款冲销，加回应付并冲正退款台账，故经营日退款净额须相应抵减，不重复计入退款额）。
        Long refund = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(
                           CASE WHEN a.adjustment_type = 'refund' THEN a.amount_cent
                                WHEN a.adjustment_type = 'refund_reversal' THEN -a.amount_cent
                                ELSE 0 END), 0)
                  FROM bar_bill_adjustments a
                  JOIN bar_bills b ON b.id = a.bill_id
                 WHERE b.bar_id = :bar_id
                   AND a.adjustment_type IN ('refund', 'refund_reversal')
                   AND a.refunded_at >= :from_ts AND a.refunded_at < :to_ts""" + merchantFilter,
                new HashMap<>(params.getValues()), Long.class);
        // 小费：本吧台 + 本收银员窗口内结算账单的 tip（代收，不计低消/提成基数）。
        Long tip = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(b.tip_amount_cent), 0)
                  FROM bar_bills b
                 WHERE b.bar_id = :bar_id
                   AND b.tip_amount_cent > 0
                   AND EXISTS (
                       SELECT 1 FROM bar_bill_settlements s
                        WHERE s.bill_id = b.id
                          AND s.cashier_staff_id = :cashier_user_id
                          AND s.reversed = FALSE
                          AND s.settled_at >= :from_ts AND s.settled_at < :to_ts
                   )""" + merchantFilter,
                new HashMap<>(params.getValues()), Long.class);
        return new BarShiftReconciliationSnapshot(cash, pos, online,
                refund == null ? 0L : refund, tip == null ? 0L : tip);
    }

    @Override
    public BarCommissionCalculationRecord saveCommissionCalculation(BarCommissionCalculationRecord calculation) {
        jdbcTemplate.update("""
                INSERT INTO bar_commission_calculations (
                    id, bill_id, sales_staff_id, sales_role_type, commission_base_cent,
                    commission_rate_bp, commission_amount_cent, status, created_at
                )
                VALUES (
                    :id, :bill_id, :sales_staff_id, :sales_role_type, :commission_base_cent,
                    :commission_rate_bp, :commission_amount_cent, :status, :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    sales_staff_id = EXCLUDED.sales_staff_id,
                    sales_role_type = EXCLUDED.sales_role_type,
                    commission_base_cent = EXCLUDED.commission_base_cent,
                    commission_rate_bp = EXCLUDED.commission_rate_bp,
                    commission_amount_cent = EXCLUDED.commission_amount_cent,
                    status = EXCLUDED.status
                """, parameterSource.commissionParameters(calculation));
        return calculation;
    }

    // ── 桌位会话 ─────────────────────────────────────────────────────────────
    @Override
    public BarTableSessionRecord saveTableSession(BarTableSessionRecord session) {
        jdbcTemplate.update("""
                INSERT INTO bar_table_sessions (
                    id, bar_id, merchant_id, business_date, table_id, reservation_id,
                    customer_id, guest_count, min_consumption_snapshot_cent,
                    allowed_pass_free_guest_count_snapshot, sales_staff_id, service_staff_id,
                    status, opened_at, closed_at, source_session_id, session_version,
                    created_at, updated_at
                )
                VALUES (
                    :id, :bar_id, :merchant_id, :business_date, :table_id, :reservation_id,
                    :customer_id, :guest_count, :min_consumption_snapshot_cent,
                    :allowed_pass_free_guest_count_snapshot, :sales_staff_id, :service_staff_id,
                    :status, :opened_at, :closed_at, :source_session_id, :session_version,
                    :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    bar_id = EXCLUDED.bar_id,
                    merchant_id = EXCLUDED.merchant_id,
                    business_date = EXCLUDED.business_date,
                    table_id = EXCLUDED.table_id,
                    reservation_id = EXCLUDED.reservation_id,
                    customer_id = EXCLUDED.customer_id,
                    guest_count = EXCLUDED.guest_count,
                    min_consumption_snapshot_cent = EXCLUDED.min_consumption_snapshot_cent,
                    allowed_pass_free_guest_count_snapshot = EXCLUDED.allowed_pass_free_guest_count_snapshot,
                    sales_staff_id = EXCLUDED.sales_staff_id,
                    service_staff_id = EXCLUDED.service_staff_id,
                    status = EXCLUDED.status,
                    opened_at = EXCLUDED.opened_at,
                    closed_at = EXCLUDED.closed_at,
                    source_session_id = EXCLUDED.source_session_id,
                    session_version = EXCLUDED.session_version,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.tableSessionParameters(session));
        return session;
    }

    // 2c-5 卡座并发：DB 级乐观锁 CAS。WHERE session_version = expected 序列化并发 transfer/merge/renew，
    // 返回 false 表示版本已被并发改动（saveTableSession 的 ON CONFLICT 无版本条件，内存比对在并发下被覆盖绕过）。
    @Override
    public boolean updateTableSessionIfVersionMatches(BarTableSessionRecord session, long expectedVersion) {
        int rows = jdbcTemplate.update("""
                UPDATE bar_table_sessions SET
                    bar_id = :bar_id, merchant_id = :merchant_id, business_date = :business_date,
                    table_id = :table_id, reservation_id = :reservation_id, customer_id = :customer_id,
                    guest_count = :guest_count, min_consumption_snapshot_cent = :min_consumption_snapshot_cent,
                    allowed_pass_free_guest_count_snapshot = :allowed_pass_free_guest_count_snapshot,
                    sales_staff_id = :sales_staff_id, service_staff_id = :service_staff_id,
                    status = :status, opened_at = :opened_at, closed_at = :closed_at,
                    source_session_id = :source_session_id, session_version = :session_version,
                    updated_at = :updated_at
                 WHERE id = :id
                   AND session_version = :expected_version
                """, parameterSource.tableSessionParameters(session).addValue("expected_version", expectedVersion));
        return rows == 1;
    }

    @Override
    public List<BarTableSessionRecord> listTableSessions(Optional<String> merchantScope) {
        if (merchantScope.isEmpty()) {
            return jdbcTemplate.query("""
                    SELECT id, bar_id, merchant_id, business_date, table_id, reservation_id,
                           customer_id, guest_count, min_consumption_snapshot_cent,
                           allowed_pass_free_guest_count_snapshot, sales_staff_id, service_staff_id,
                           status, opened_at, closed_at, source_session_id, session_version,
                           created_at, updated_at
                      FROM bar_table_sessions
                     ORDER BY created_at DESC, id
                    """, rowMapper::mapTableSession);
        }
        return jdbcTemplate.query("""
                SELECT id, bar_id, merchant_id, business_date, table_id, reservation_id,
                       customer_id, guest_count, min_consumption_snapshot_cent,
                       allowed_pass_free_guest_count_snapshot, sales_staff_id, service_staff_id,
                       status, opened_at, closed_at, source_session_id, session_version,
                       created_at, updated_at
                  FROM bar_table_sessions
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantScope.get()), rowMapper::mapTableSession);
    }

    @Override
    public Optional<BarTableSessionRecord> findTableSessionById(String id) {
        return jdbcTemplate.query("""
                SELECT id, bar_id, merchant_id, business_date, table_id, reservation_id,
                       customer_id, guest_count, min_consumption_snapshot_cent,
                       allowed_pass_free_guest_count_snapshot, sales_staff_id, service_staff_id,
                       status, opened_at, closed_at, source_session_id, session_version,
                       created_at, updated_at
                  FROM bar_table_sessions
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapTableSession).stream().findFirst();
    }

    @Override
    public BarTableSessionAdjustmentRecord saveTableSessionAdjustment(BarTableSessionAdjustmentRecord adjustment) {
        jdbcTemplate.update("""
                INSERT INTO bar_table_session_adjustments (
                    id, session_id, adjustment_type, source_session_id, target_session_id,
                    from_table_id, to_table_id, bar_id, merchant_id, business_date,
                    min_consumption_before_cent, min_consumption_after_cent, renewed_until,
                    additional_min_consumption_cent, additional_deposit_amount_cent,
                    additional_service_fee_cent, reason, requested_by_user_id,
                    approval_required, status, idempotency_key, evidence_attachment_ids,
                    snapshot_json, created_at
                )
                VALUES (
                    :id, :session_id, :adjustment_type, :source_session_id, :target_session_id,
                    :from_table_id, :to_table_id, :bar_id, :merchant_id, :business_date,
                    :min_consumption_before_cent, :min_consumption_after_cent, :renewed_until,
                    :additional_min_consumption_cent, :additional_deposit_amount_cent,
                    :additional_service_fee_cent, :reason, :requested_by_user_id,
                    :approval_required, :status, :idempotency_key,
                    CAST(:evidence_attachment_ids AS jsonb), CAST(:snapshot_json AS jsonb),
                    :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    snapshot_json = EXCLUDED.snapshot_json
                """, parameterSource.tableSessionAdjustmentParameters(adjustment));
        return adjustment;
    }

    @Override
    public List<BarTableSessionAdjustmentRecord> listTableSessionAdjustments(String sessionId) {
        return jdbcTemplate.query("""
                SELECT id, session_id, adjustment_type, source_session_id, target_session_id,
                       from_table_id, to_table_id, bar_id, merchant_id, business_date,
                       min_consumption_before_cent, min_consumption_after_cent, renewed_until,
                       additional_min_consumption_cent, additional_deposit_amount_cent,
                       additional_service_fee_cent, reason, requested_by_user_id,
                       approval_required, status, idempotency_key, evidence_attachment_ids,
                       snapshot_json, created_at
                  FROM bar_table_session_adjustments
                 WHERE session_id = :session_id
                 ORDER BY created_at DESC, id
                """, Map.of("session_id", sessionId), rowMapper::mapTableSessionAdjustment);
    }

    // ── 第十批 TK-24/TK-25：台组（并台链）口径原语 ────────────────────────────────
    @Override
    public List<String> familySessionIds(String sessionId, boolean includeTransferred) {
        return jdbcTemplate.query("""
                WITH RECURSIVE family AS (
                    SELECT id, 0 AS depth FROM bar_table_sessions WHERE id = :id
                    UNION ALL
                    SELECT s.id, f.depth + 1 FROM bar_table_sessions s JOIN family f ON s.source_session_id = f.id
                     WHERE s.status = 'merged' OR (:include_transferred AND s.status = 'merged_transferred')
                )
                SELECT id FROM family ORDER BY depth, id
                """, Map.of("id", sessionId, "include_transferred", includeTransferred),
                (rs, rowNum) -> rs.getString("id"));
    }

    @Override
    public List<BarOrderRecord> listOrdersWithoutSession(List<String> tableIds, Instant since) {
        if (tableIds.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT o.id, o.bar_id, o.merchant_id, o.table_id, o.reservation_id, o.customer_id, o.order_source,
                       o.status, o.total_amount_cent, o.currency, o.created_terminal, o.device_id,
                       o.sales_staff_id, o.created_at, o.updated_at, o.order_type, o.paid_at
                  FROM bar_orders o
                 WHERE o.table_id IN (:table_ids)
                   AND o.created_at >= :since
                   AND o.status <> 'cancelled'
                   AND NOT EXISTS (SELECT 1 FROM bar_order_service_sources s WHERE s.order_id = o.id)
                 ORDER BY o.created_at, o.id
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("table_ids", tableIds)
                .addValue("since", timestampWithTimeZone(since)), rowMapper::mapOrder);
    }

    @Override
    public void saveOrderServiceSource(String orderId, String sessionId) {
        jdbcTemplate.update("""
                INSERT INTO bar_order_service_sources (order_id, session_id, table_id, project_id, merchant_id, business_date)
                SELECT :order_id, s.id, s.table_id, sp.project_id, t.merchant_id, s.business_date
                  FROM bar_table_sessions s
                  JOIN bar_tables t ON t.id = s.table_id
                  JOIN spaces sp ON sp.id = t.space_id
                 WHERE s.id = :session_id
                ON CONFLICT (order_id) DO NOTHING
                """, Map.of("order_id", orderId, "session_id", sessionId));
    }

    @Override
    public List<String> listOrderIdsBySessionIds(List<String> sessionIds) {
        if (sessionIds.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT order_id FROM bar_order_service_sources
                 WHERE session_id IN (:session_ids)
                 ORDER BY recorded_at, order_id
                """, Map.of("session_ids", sessionIds), (rs, rowNum) -> rs.getString("order_id"));
    }

    @Override
    public Optional<String> findSessionIdByOrderId(String orderId) {
        return jdbcTemplate.query("""
                SELECT session_id FROM bar_order_service_sources WHERE order_id = :order_id
                """, Map.of("order_id", orderId), (rs, rowNum) -> rs.getString("session_id")).stream().findFirst();
    }

    @Override
    public boolean depositRefundPayableExists(String reservationId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM ar_ap_items
                                WHERE direction = 'payable' AND source_type = 'bar_deposit_refund' AND source_id = :id)
                """, Map.of("id", reservationId), Boolean.class));
    }

    @Override
    public long refundGoodsCent(String adjustmentId, long amountCent) {
        List<Long> goods = jdbcTemplate.query("""
                SELECT CASE WHEN count(*) = 0 THEN NULL
                            ELSE COALESCE(SUM(amount_cent) FILTER (WHERE component_type = 'goods'), 0) END AS goods
                  FROM bar_payroll_refund_lines WHERE adjustment_id = :id
                """, Map.of("id", adjustmentId), (rs, rowNum) -> (Long) rs.getObject("goods", Long.class));
        return goods.isEmpty() || goods.get(0) == null ? amountCent : goods.get(0);
    }

    // ── 库存原子扣补 / 流水改道成本管道 / 支付意向 ────────────────────────────────
    @Override
    public boolean atomicDecreaseStock(String itemId, String locationId, int quantity) {
        int updated = jdbcTemplate.update("""
                UPDATE inventory_stock_balances
                   SET available_quantity = available_quantity - :qty,
                       quantity           = quantity           - :qty,
                       updated_at         = NOW()
                 WHERE item_id   = :item_id
                   AND location_id = :location_id
                   AND available_quantity >= :qty
                """, Map.of("item_id", itemId, "location_id", locationId, "qty", quantity));
        return updated > 0;
    }

    @Override
    public boolean atomicIncreaseStock(String itemId, String locationId, int quantity) {
        // V091 退酒回补库存：无可用量下限，原子加回（仅当余额行已存在；不存在则由调用方决定是否兜底创建）。
        int updated = jdbcTemplate.update("""
                UPDATE inventory_stock_balances
                   SET available_quantity = available_quantity + :qty,
                       quantity           = quantity           + :qty,
                       updated_at         = NOW()
                 WHERE item_id   = :item_id
                   AND location_id = :location_id
                """, Map.of("item_id", itemId, "location_id", locationId, "qty", quantity));
        return updated > 0;
    }

    @Override
    public Optional<StockBalanceSnapshot> findStockBalance(String itemId, String locationId) {
        return jdbcTemplate.query("""
                SELECT id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at
                  FROM inventory_stock_balances
                 WHERE item_id = :item_id
                   AND location_id = :location_id
                """, Map.of("item_id", itemId, "location_id", locationId), rowMapper::mapStockBalance).stream().findFirst();
    }

    @Override
    public StockBalanceSnapshot saveStockBalance(StockBalanceSnapshot balance) {
        jdbcTemplate.update("""
                INSERT INTO inventory_stock_balances (
                    id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at
                )
                VALUES (
                    :id, :item_id, :location_id, :quantity, :reserved_quantity, :available_quantity, :updated_at
                )
                ON CONFLICT (item_id, location_id) DO UPDATE SET
                    quantity = EXCLUDED.quantity,
                    reserved_quantity = EXCLUDED.reserved_quantity,
                    available_quantity = EXCLUDED.available_quantity,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.stockBalanceParameters(balance));
        return balance;
    }

    @Override
    public StockMovementSnapshot saveStockMovement(StockMovementSnapshot movement) {
        // C-1(V296)：酒吧域流水（售出扣减/退酒回补/撤单冲销/赠饮扣减）改道库存域成本管道单一汇聚点——
        // 此前裸 INSERT 不写成本列/不滚动移动加权状态行/缺 merchant_id、project_id，导致主销售路径成本恒 NULL、
        // 状态行持有量虚高、按商户的期间成本汇总漏酒吧域。归属补齐：merchant_id 取物料归属（inventory_items），
        // project_id 取对应库位余额行。调用方（BarOrderService/BarGiftConsumer）无事务（自动提交），
        // 此处 REQUIRED 事务包住 锁状态行→计成本→插流水→滚状态 保证原子；已有事务则直接加入。
        return transactionTemplate.execute(status -> {
            // merchant_id/project_id 列可为 NULL（平台自营物料/未标项目库位），
            // findFirst() 遇 null 元素抛 NPE，须列表判空取值而非流。
            java.util.List<String> merchantRows = jdbcTemplate.query(
                    "SELECT merchant_id FROM inventory_items WHERE id = :item_id",
                    new MapSqlParameterSource("item_id", movement.itemId()),
                    (rs, i) -> rs.getString(1));
            String merchantId = merchantRows.isEmpty() ? null : merchantRows.get(0);
            String locationId = movement.sourceLocationId() != null
                    ? movement.sourceLocationId() : movement.targetLocationId();
            java.util.List<String> projectRows = locationId == null ? java.util.List.of() : jdbcTemplate.query(
                    "SELECT project_id FROM inventory_stock_balances WHERE item_id = :item_id AND location_id = :location_id",
                    new MapSqlParameterSource("item_id", movement.itemId()).addValue("location_id", locationId),
                    (rs, i) -> rs.getString(1));
            String projectId = projectRows.isEmpty() ? null : projectRows.get(0);
            inventoryRepository.saveStockMovement(new StockMovementRecord(
                    movement.id(),
                    movement.movementType(),
                    movement.itemId(),
                    movement.sourceLocationId(),
                    movement.targetLocationId(),
                    movement.quantity(),
                    null,
                    null,
                    movement.businessType(),
                    movement.businessId(),
                    movement.traceCode(),
                    movement.operatorId(),
                    movement.evidenceAttachmentIds(),
                    movement.status(),
                    movement.createdAt(),
                    merchantId), projectId);
            return movement;
        });
    }

    @Override
    public PaymentIntentRecord savePaymentIntent(PaymentIntentRecord paymentIntent) {
        jdbcTemplate.update("""
                INSERT INTO finance_payment_intents (
                    id, business_order_type, business_order_id, amount_cent, currency,
                    channel, status, created_at, updated_at
                )
                VALUES (
                    :id, :business_order_type, :business_order_id, :amount_cent, :currency,
                    :channel, :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    business_order_type = EXCLUDED.business_order_type,
                    business_order_id = EXCLUDED.business_order_id,
                    amount_cent = EXCLUDED.amount_cent,
                    currency = EXCLUDED.currency,
                    channel = EXCLUDED.channel,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.paymentIntentParameters(paymentIntent));
        return paymentIntent;
    }
}
