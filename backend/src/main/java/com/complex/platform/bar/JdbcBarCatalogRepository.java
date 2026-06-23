package com.complex.platform.bar;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

final class JdbcBarCatalogRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcBarRowMapper rowMapper;
    private final JdbcBarParameterSource parameterSource;

    JdbcBarCatalogRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            JdbcBarRowMapper rowMapper,
            JdbcBarParameterSource parameterSource
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = rowMapper;
        this.parameterSource = parameterSource;
    }

    public BarTableRecord saveTable(BarTableRecord table) {
        jdbcTemplate.update("""
                INSERT INTO bar_tables (
                    id, bar_id, merchant_id, table_code, table_name, table_type, capacity,
                    allowed_pass_free_guest_count, min_consumption_cent, currency,
                    status, created_at, updated_at
                )
                VALUES (
                    :id, :bar_id, :merchant_id, :table_code, :table_name, :table_type, :capacity,
                    :allowed_pass_free_guest_count, :min_consumption_cent, :currency,
                    :status, :created_at, :updated_at
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
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.tableParameters(table));
        return table;
    }

    public List<BarTableRecord> listTables(Optional<String> merchantScope) {
        if (merchantScope.isEmpty()) {
            return jdbcTemplate.query("""
                    SELECT id, bar_id, merchant_id, table_code, table_name, table_type, capacity,
                           allowed_pass_free_guest_count, min_consumption_cent, currency,
                           status, created_at, updated_at
                      FROM bar_tables
                     ORDER BY created_at DESC, id
                    """, rowMapper::mapTable);
        }
        return jdbcTemplate.query("""
                SELECT id, bar_id, merchant_id, table_code, table_name, table_type, capacity,
                       allowed_pass_free_guest_count, min_consumption_cent, currency,
                       status, created_at, updated_at
                  FROM bar_tables
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantScope.get()), rowMapper::mapTable);
    }

    public Optional<BarTableRecord> findTableById(String id) {
        return jdbcTemplate.query("""
                SELECT id, bar_id, merchant_id, table_code, table_name, table_type, capacity,
                       allowed_pass_free_guest_count, min_consumption_cent, currency,
                       status, created_at, updated_at
                  FROM bar_tables
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapTable).stream().findFirst();
    }

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

    public int assignTableSpace(String tableId, String spaceId) {
        return jdbcTemplate.update("""
                UPDATE bar_tables
                   SET space_id = :space_id,
                       updated_at = NOW()
                 WHERE id = :id
                """, Map.of("id", tableId, "space_id", spaceId));
    }

    public Optional<String> findSpaceMerchant(String spaceId) {
        // spaces.merchant_id 可能为 NULL（平台/共享空间）；用 query 区分「空间不存在」与「存在但 merchant 为空」。
        List<String> rows = jdbcTemplate.query(
                "SELECT COALESCE(merchant_id, '') FROM spaces WHERE id = :id",
                Map.of("id", spaceId),
                (rs, n) -> rs.getString(1));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public List<BarTableSpaceBacklogRecord> listTablesWithoutSpace(Optional<String> merchantScope) {
        String base = """
                SELECT id, bar_id, merchant_id, table_code, table_name, status
                  FROM bar_tables
                 WHERE space_id IS NULL
                """;
        if (merchantScope.isEmpty()) {
            return jdbcTemplate.query(base + " ORDER BY created_at DESC, id", JdbcBarCatalogRepository::mapBacklog);
        }
        return jdbcTemplate.query(
                base + " AND merchant_id = :merchant_id ORDER BY created_at DESC, id",
                Map.of("merchant_id", merchantScope.get()),
                JdbcBarCatalogRepository::mapBacklog);
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

    public BarReservationRecord saveReservation(BarReservationRecord reservation) {
        jdbcTemplate.update("""
                INSERT INTO bar_reservations (
                    id, bar_id, merchant_id, table_id, customer_id, reserved_at, guest_count,
                    allowed_pass_free_guest_count, deposit_amount_cent, deposit_status,
                    order_source, sales_staff_id, status, created_terminal, device_id,
                    evidence_attachment_ids, created_at, updated_at, deposit_rule,
                    deposit_intent_id, deposit_paid_at, deposit_settlement,
                    invitation_token, invitation_signature, invitation_capacity,
                    invitation_status, invitation_issued_at
                )
                VALUES (
                    :id, :bar_id, :merchant_id, :table_id, :customer_id, :reserved_at, :guest_count,
                    :allowed_pass_free_guest_count, :deposit_amount_cent, :deposit_status,
                    :order_source, :sales_staff_id, :status, :created_terminal, :device_id,
                    CAST(:evidence_attachment_ids AS jsonb), :created_at, :updated_at, :deposit_rule,
                    :deposit_intent_id, :deposit_paid_at, :deposit_settlement,
                    :invitation_token, :invitation_signature, :invitation_capacity,
                    :invitation_status, :invitation_issued_at
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
                    invitation_issued_at = EXCLUDED.invitation_issued_at
                """, parameterSource.reservationParameters(reservation));
        return reservation;
    }

    public Optional<BarReservationRecord> findReservationByInvitationToken(String token) {
        return jdbcTemplate.query("""
                SELECT *
                  FROM bar_reservations
                 WHERE invitation_token = :token
                """, Map.of("token", token), rowMapper::mapReservation).stream().findFirst();
    }

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

    public List<BarReservationInvitationMemberRecord> listInvitationMembers(String reservationId) {
        return jdbcTemplate.query("""
                SELECT *
                  FROM bar_reservation_invitation_members
                 WHERE reservation_id = :reservation_id
                 ORDER BY is_holder DESC, confirmed_at, id
                """, Map.of("reservation_id", reservationId), rowMapper::mapInvitationMember);
    }

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

    public List<BarDepositRuleRecord> listDepositRules(String barId) {
        return jdbcTemplate.query("""
                SELECT *
                  FROM bar_deposit_rules
                 WHERE bar_id = :bar_id AND enabled = TRUE
                 ORDER BY scope_type DESC, created_at DESC, id
                """, Map.of("bar_id", barId), rowMapper::mapDepositRule);
    }

    public List<BarReservationRecord> listReservations(Optional<String> merchantScope) {
        if (merchantScope.isEmpty()) {
            return jdbcTemplate.query("""
                    SELECT *
                      FROM bar_reservations
                     ORDER BY created_at DESC, id
                    """, rowMapper::mapReservation);
        }
        return jdbcTemplate.query("""
                SELECT *
                  FROM bar_reservations
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantScope.get()), rowMapper::mapReservation);
    }

    public Optional<BarReservationRecord> findReservationById(String id) {
        return jdbcTemplate.query("""
                SELECT *
                  FROM bar_reservations
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapReservation).stream().findFirst();
    }

    public Optional<BarReservationRecord> findReservationByIdForUpdate(String id) {
        return jdbcTemplate.query("""
                SELECT *
                  FROM bar_reservations
                 WHERE id = :id
                 FOR UPDATE
                """, Map.of("id", id), rowMapper::mapReservation).stream().findFirst();
    }

    public int countGuestCheckins(String reservationId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)::int
                  FROM bar_table_guest_checkins
                 WHERE reservation_id = :reservation_id
                """, Map.of("reservation_id", reservationId), Integer.class);
        return count == null ? 0 : count;
    }

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

    public BarAgeVerificationRecord saveAgeVerification(BarAgeVerificationRecord verification) {
        jdbcTemplate.update("""
                INSERT INTO bar_age_verifications (
                    id, business_id, credential_payload, device_id, staff_id,
                    result, provider_code, review_status, evidence_attachment_ids, created_at
                )
                VALUES (
                    :id, :business_id, :credential_payload, :device_id, :staff_id,
                    :result, :provider_code, :review_status,
                    CAST(:evidence_attachment_ids AS jsonb), :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    result = EXCLUDED.result,
                    provider_code = EXCLUDED.provider_code,
                    review_status = EXCLUDED.review_status,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids
                """, parameterSource.ageVerificationParameters(verification));
        return verification;
    }

    public BarProductRecord saveProduct(BarProductRecord product) {
        jdbcTemplate.update("""
                INSERT INTO bar_products (
                    id, bar_id, merchant_id, product_name, product_type, price_cent, currency,
                    inventory_item_id, stock_location_id, stock_deduction_required,
                    status, created_at, updated_at,
                    standard_pour_volume, pour_volume_unit, loss_threshold,
                    sold_out, sold_out_at, sold_out_reason, internal_settlement_price_cent
                )
                VALUES (
                    :id, :bar_id, :merchant_id, :product_name, :product_type, :price_cent, :currency,
                    :inventory_item_id, :stock_location_id, :stock_deduction_required,
                    :status, :created_at, :updated_at,
                    :standard_pour_volume, :pour_volume_unit, :loss_threshold,
                    :sold_out, :sold_out_at, :sold_out_reason, :internal_settlement_price_cent
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
                    internal_settlement_price_cent = EXCLUDED.internal_settlement_price_cent
                """, parameterSource.productParameters(product));
        return product;
    }

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

    public Optional<BarProductRecord> findProductById(String id) {
        return jdbcTemplate.query("""
                SELECT *
                  FROM bar_products
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapProduct).stream().findFirst();
    }

    public Optional<BarProductRecord> findProductByInventoryItem(String barId, String inventoryItemId) {
        // F-TXN-03 piece③b：按 (bar_id, inventory_item_id) 取吧台商品，active 优先、最近创建，取一条。
        return jdbcTemplate.query("""
                SELECT *
                  FROM bar_products
                 WHERE bar_id = :bar_id
                   AND inventory_item_id = :inventory_item_id
                 ORDER BY (status = 'active') DESC, created_at DESC, id
                 LIMIT 1
                """, Map.of("bar_id", barId, "inventory_item_id", inventoryItemId),
                rowMapper::mapProduct).stream().findFirst();
    }
}
