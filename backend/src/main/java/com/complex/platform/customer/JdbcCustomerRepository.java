package com.complex.platform.customer;

import com.complex.platform.bar.BarReservationRecord;
import com.complex.platform.bar.BarTableRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcCustomerRepository implements CustomerRepository {

    private static final TypeReference<Map<String, Object>> OBJECT_MAP_TYPE = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcCustomerRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public Optional<CustomerRecord> findCustomerByPhone(String phone) {
        return jdbcTemplate.query("""
                SELECT id, phone, masked_phone, status, last_login_at, created_at, updated_at
                  FROM customers
                 WHERE phone = :phone
                """, Map.of("phone", phone), this::mapCustomer).stream().findFirst();
    }

    @Override
    public Optional<CustomerRecord> findCustomerById(String customerId) {
        return jdbcTemplate.query("""
                SELECT id, phone, masked_phone, status, last_login_at, created_at, updated_at
                  FROM customers
                 WHERE id = :id
                """, Map.of("id", customerId), this::mapCustomer).stream().findFirst();
    }

    @Override
    public CustomerRecord saveCustomer(CustomerRecord customer) {
        jdbcTemplate.update("""
                INSERT INTO customers (id, phone, masked_phone, status, last_login_at, created_at, updated_at)
                VALUES (:id, :phone, :masked_phone, :status, :last_login_at, :created_at, :updated_at)
                ON CONFLICT (id) DO UPDATE SET
                    phone = EXCLUDED.phone,
                    masked_phone = EXCLUDED.masked_phone,
                    status = EXCLUDED.status,
                    last_login_at = EXCLUDED.last_login_at,
                    updated_at = EXCLUDED.updated_at
                """, customerParameters(customer));
        return customer;
    }

    @Override
    public CustomerSessionRecord saveSession(CustomerSessionRecord session) {
        jdbcTemplate.update("""
                INSERT INTO customer_sessions (id, customer_id, customer_token, scene, issued_at, expires_at, status)
                VALUES (:id, :customer_id, :customer_token, :scene, :issued_at, :expires_at, :status)
                ON CONFLICT (id) DO UPDATE SET
                    customer_token = EXCLUDED.customer_token,
                    expires_at = EXCLUDED.expires_at,
                    status = EXCLUDED.status
                """, sessionParameters(session));
        return session;
    }

    @Override
    public Optional<CustomerSessionRecord> findSessionByToken(String token) {
        return jdbcTemplate.query("""
                SELECT id, customer_id, customer_token, scene, issued_at, expires_at, status
                  FROM customer_sessions
                 WHERE customer_token = :customer_token
                """, Map.of("customer_token", token), this::mapSession).stream().findFirst();
    }

    @Override
    public CustomerConsentRecord saveConsent(CustomerConsentRecord consent) {
        jdbcTemplate.update("""
                INSERT INTO customer_consents (
                    id, customer_id, consent_type, accepted, version, source_terminal,
                    accepted_at, withdrawn_at, created_at, updated_at
                )
                VALUES (
                    :id, :customer_id, :consent_type, :accepted, :version, :source_terminal,
                    :accepted_at, :withdrawn_at, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    accepted = EXCLUDED.accepted,
                    version = EXCLUDED.version,
                    source_terminal = EXCLUDED.source_terminal,
                    accepted_at = EXCLUDED.accepted_at,
                    withdrawn_at = EXCLUDED.withdrawn_at,
                    updated_at = EXCLUDED.updated_at
                """, consentParameters(consent));
        return consent;
    }

    @Override
    public Optional<CustomerPointsAccountRecord> findPointsByCustomerId(String customerId) {
        return jdbcTemplate.query("""
                SELECT id, customer_id, balance, level, recent_summary::text AS recent_summary, created_at, updated_at
                  FROM customer_points_accounts
                 WHERE customer_id = :customer_id
                """, Map.of("customer_id", customerId), this::mapPoints).stream().findFirst();
    }

    @Override
    public CustomerPointsAccountRecord savePoints(CustomerPointsAccountRecord account) {
        jdbcTemplate.update("""
                INSERT INTO customer_points_accounts (
                    id, customer_id, balance, level, recent_summary, created_at, updated_at
                )
                VALUES (
                    :id, :customer_id, :balance, :level, CAST(:recent_summary AS jsonb), :created_at, :updated_at
                )
                ON CONFLICT (customer_id) DO UPDATE SET
                    balance = EXCLUDED.balance,
                    level = EXCLUDED.level,
                    recent_summary = EXCLUDED.recent_summary,
                    updated_at = EXCLUDED.updated_at
                """, pointsParameters(account));
        return account;
    }

    @Override
    public CustomerPointsLedgerRecord savePointsLedger(CustomerPointsLedgerRecord ledger) {
        jdbcTemplate.update("""
                INSERT INTO customer_points_ledger (
                    id, customer_id, points_delta, balance_after, source_type,
                    source_id, description, created_at
                )
                VALUES (
                    :id, :customer_id, :points_delta, :balance_after, :source_type,
                    :source_id, :description, :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    points_delta = EXCLUDED.points_delta,
                    balance_after = EXCLUDED.balance_after,
                    source_type = EXCLUDED.source_type,
                    source_id = EXCLUDED.source_id,
                    description = EXCLUDED.description
                """, pointsLedgerParameters(ledger));
        return ledger;
    }

    @Override
    public List<CustomerPointsLedgerRecord> listPointsLedgerByCustomerId(String customerId) {
        return jdbcTemplate.query("""
                SELECT id, customer_id, points_delta, balance_after, source_type,
                       source_id, description, created_at
                  FROM customer_points_ledger
                 WHERE customer_id = :customer_id
                 ORDER BY created_at DESC, id DESC
                """, Map.of("customer_id", customerId), this::mapPointsLedger);
    }

    @Override
    public CustomerRedemptionCatalogItemRecord saveRedemptionCatalogItem(CustomerRedemptionCatalogItemRecord item) {
        jdbcTemplate.update("""
                INSERT INTO customer_redemption_catalog_items (
                    id, item_name, item_type, points_cost, inventory_item_id,
                    stock_location_id, stock_quantity, status, available_from,
                    available_until, created_at, updated_at
                )
                VALUES (
                    :id, :item_name, :item_type, :points_cost, :inventory_item_id,
                    :stock_location_id, :stock_quantity, :status, :available_from,
                    :available_until, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    item_name = EXCLUDED.item_name,
                    item_type = EXCLUDED.item_type,
                    points_cost = EXCLUDED.points_cost,
                    inventory_item_id = EXCLUDED.inventory_item_id,
                    stock_location_id = EXCLUDED.stock_location_id,
                    stock_quantity = EXCLUDED.stock_quantity,
                    status = EXCLUDED.status,
                    available_from = EXCLUDED.available_from,
                    available_until = EXCLUDED.available_until,
                    updated_at = EXCLUDED.updated_at
                """, redemptionCatalogParameters(item));
        return item;
    }

    @Override
    public Optional<CustomerRedemptionCatalogItemRecord> findRedemptionCatalogItemById(String itemId) {
        return jdbcTemplate.query("""
                SELECT id, item_name, item_type, points_cost, inventory_item_id,
                       stock_location_id, stock_quantity, status, available_from,
                       available_until, created_at, updated_at
                  FROM customer_redemption_catalog_items
                 WHERE id = :id
                """, Map.of("id", itemId), this::mapRedemptionCatalogItem).stream().findFirst();
    }

    @Override
    public List<CustomerRedemptionCatalogItemRecord> listActiveRedemptionCatalogItems(Instant now) {
        return jdbcTemplate.query("""
                SELECT id, item_name, item_type, points_cost, inventory_item_id,
                       stock_location_id, stock_quantity, status, available_from,
                       available_until, created_at, updated_at
                  FROM customer_redemption_catalog_items
                 WHERE status = 'active'
                   AND (available_from IS NULL OR available_from <= :now)
                   AND (available_until IS NULL OR available_until > :now)
                 ORDER BY points_cost ASC, item_name ASC, id ASC
                """, Map.of("now", timestampWithTimeZone(now)), this::mapRedemptionCatalogItem);
    }

    @Override
    public CustomerPointsRedemptionRecord savePointsRedemption(CustomerPointsRedemptionRecord redemption) {
        jdbcTemplate.update("""
                INSERT INTO customer_points_redemptions (
                    id, customer_id, catalog_item_id, item_name, quantity,
                    points_cost, total_points, inventory_item_id, stock_location_id,
                    stock_quantity, source_terminal, status, created_at, updated_at
                )
                VALUES (
                    :id, :customer_id, :catalog_item_id, :item_name, :quantity,
                    :points_cost, :total_points, :inventory_item_id, :stock_location_id,
                    :stock_quantity, :source_terminal, :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, pointsRedemptionParameters(redemption));
        return redemption;
    }

    @Override
    public List<CustomerPointsRedemptionRecord> listPointsRedemptionsByCustomerId(String customerId) {
        return jdbcTemplate.query("""
                SELECT id, customer_id, catalog_item_id, item_name, quantity,
                       points_cost, total_points, inventory_item_id, stock_location_id,
                       stock_quantity, source_terminal, status, created_at, updated_at
                  FROM customer_points_redemptions
                 WHERE customer_id = :customer_id
                 ORDER BY created_at DESC, id DESC
                """, Map.of("customer_id", customerId), this::mapPointsRedemption);
    }

    @Override
    public List<CustomerBarReservationOptionResponse> listBarReservationOptions() {
        return jdbcTemplate.query("""
                SELECT bar_id, id AS table_id, table_type, table_name, capacity,
                       allowed_pass_free_guest_count, min_consumption_cent, currency, status
                  FROM bar_tables
                 WHERE status = 'active'
                 ORDER BY bar_id, table_name, id
                """, this::mapBarReservationOption);
    }

    @Override
    public Optional<BarTableRecord> findActiveBarTableForReservation(String barId, String tableId, String tableType) {
        return jdbcTemplate.query("""
                SELECT id, bar_id, table_code, table_name, table_type, capacity,
                       allowed_pass_free_guest_count, min_consumption_cent, currency,
                       status, created_at, updated_at
                  FROM bar_tables
                 WHERE bar_id = :bar_id
                   AND id = :table_id
                   AND table_type = :table_type
                   AND status = 'active'
                """, Map.of(
                "bar_id", barId,
                "table_id", tableId,
                "table_type", tableType
        ), this::mapBarTable).stream().findFirst();
    }

    @Override
    public BarReservationRecord saveCustomerBarReservation(BarReservationRecord reservation) {
        jdbcTemplate.update("""
                INSERT INTO bar_reservations (
                    id, bar_id, table_id, customer_id, reserved_at, guest_count,
                    allowed_pass_free_guest_count, deposit_amount_cent, deposit_status,
                    order_source, sales_staff_id, status, created_terminal, device_id,
                    evidence_attachment_ids, created_at, updated_at
                )
                VALUES (
                    :id, :bar_id, :table_id, :customer_id, :reserved_at, :guest_count,
                    :allowed_pass_free_guest_count, :deposit_amount_cent, :deposit_status,
                    :order_source, :sales_staff_id, :status, :created_terminal, :device_id,
                    CAST(:evidence_attachment_ids AS jsonb), :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    bar_id = EXCLUDED.bar_id,
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
                    updated_at = EXCLUDED.updated_at
                """, barReservationParameters(reservation));
        return reservation;
    }

    private MapSqlParameterSource customerParameters(CustomerRecord customer) {
        return new MapSqlParameterSource()
                .addValue("id", customer.id())
                .addValue("phone", customer.phone())
                .addValue("masked_phone", customer.maskedPhone())
                .addValue("status", customer.status())
                .addValue("last_login_at", timestampWithTimeZone(customer.lastLoginAt()))
                .addValue("created_at", timestampWithTimeZone(customer.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(customer.updatedAt()));
    }

    private MapSqlParameterSource sessionParameters(CustomerSessionRecord session) {
        return new MapSqlParameterSource()
                .addValue("id", session.id())
                .addValue("customer_id", session.customerId())
                .addValue("customer_token", session.customerToken())
                .addValue("scene", session.scene())
                .addValue("issued_at", timestampWithTimeZone(session.issuedAt()))
                .addValue("expires_at", timestampWithTimeZone(session.expiresAt()))
                .addValue("status", session.status());
    }

    private MapSqlParameterSource consentParameters(CustomerConsentRecord consent) {
        return new MapSqlParameterSource()
                .addValue("id", consent.id())
                .addValue("customer_id", consent.customerId())
                .addValue("consent_type", consent.consentType())
                .addValue("accepted", consent.accepted())
                .addValue("version", consent.version())
                .addValue("source_terminal", consent.sourceTerminal())
                .addValue("accepted_at", timestampWithTimeZone(consent.acceptedAt()))
                .addValue("withdrawn_at", timestampWithTimeZone(consent.withdrawnAt()))
                .addValue("created_at", timestampWithTimeZone(consent.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(consent.updatedAt()));
    }

    private MapSqlParameterSource pointsParameters(CustomerPointsAccountRecord account) {
        return new MapSqlParameterSource()
                .addValue("id", account.id())
                .addValue("customer_id", account.customerId())
                .addValue("balance", account.balance())
                .addValue("level", account.level())
                .addValue("recent_summary", writeJson(account.recentSummary()))
                .addValue("created_at", timestampWithTimeZone(account.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(account.updatedAt()));
    }

    private MapSqlParameterSource pointsLedgerParameters(CustomerPointsLedgerRecord ledger) {
        return new MapSqlParameterSource()
                .addValue("id", ledger.id())
                .addValue("customer_id", ledger.customerId())
                .addValue("points_delta", ledger.pointsDelta())
                .addValue("balance_after", ledger.balanceAfter())
                .addValue("source_type", ledger.sourceType())
                .addValue("source_id", ledger.sourceId())
                .addValue("description", ledger.description())
                .addValue("created_at", timestampWithTimeZone(ledger.createdAt()));
    }

    private MapSqlParameterSource redemptionCatalogParameters(CustomerRedemptionCatalogItemRecord item) {
        return new MapSqlParameterSource()
                .addValue("id", item.id())
                .addValue("item_name", item.itemName())
                .addValue("item_type", item.itemType())
                .addValue("points_cost", item.pointsCost())
                .addValue("inventory_item_id", item.inventoryItemId())
                .addValue("stock_location_id", item.stockLocationId())
                .addValue("stock_quantity", item.stockQuantity())
                .addValue("status", item.status())
                .addValue("available_from", timestampWithTimeZone(item.availableFrom()))
                .addValue("available_until", timestampWithTimeZone(item.availableUntil()))
                .addValue("created_at", timestampWithTimeZone(item.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(item.updatedAt()));
    }

    private MapSqlParameterSource pointsRedemptionParameters(CustomerPointsRedemptionRecord redemption) {
        return new MapSqlParameterSource()
                .addValue("id", redemption.id())
                .addValue("customer_id", redemption.customerId())
                .addValue("catalog_item_id", redemption.catalogItemId())
                .addValue("item_name", redemption.itemName())
                .addValue("quantity", redemption.quantity())
                .addValue("points_cost", redemption.pointsCost())
                .addValue("total_points", redemption.totalPoints())
                .addValue("inventory_item_id", redemption.inventoryItemId())
                .addValue("stock_location_id", redemption.stockLocationId())
                .addValue("stock_quantity", redemption.stockQuantity())
                .addValue("source_terminal", redemption.sourceTerminal())
                .addValue("status", redemption.status())
                .addValue("created_at", timestampWithTimeZone(redemption.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(redemption.updatedAt()));
    }

    private MapSqlParameterSource barReservationParameters(BarReservationRecord reservation) {
        return new MapSqlParameterSource()
                .addValue("id", reservation.id())
                .addValue("bar_id", reservation.barId())
                .addValue("table_id", reservation.tableId())
                .addValue("customer_id", reservation.customerId())
                .addValue("reserved_at", timestampWithTimeZone(reservation.reservedAt()))
                .addValue("guest_count", reservation.guestCount())
                .addValue("allowed_pass_free_guest_count", reservation.allowedPassFreeGuestCount())
                .addValue("deposit_amount_cent", reservation.depositAmountCent())
                .addValue("deposit_status", reservation.depositStatus())
                .addValue("order_source", reservation.orderSource())
                .addValue("sales_staff_id", reservation.salesStaffId())
                .addValue("status", reservation.status())
                .addValue("created_terminal", reservation.createdTerminal())
                .addValue("device_id", reservation.deviceId())
                .addValue("evidence_attachment_ids", writeJson(reservation.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(reservation.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(reservation.updatedAt()));
    }

    private CustomerRecord mapCustomer(ResultSet resultSet, int rowNumber) throws SQLException {
        return new CustomerRecord(
                resultSet.getString("id"),
                resultSet.getString("phone"),
                resultSet.getString("masked_phone"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "last_login_at"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private CustomerSessionRecord mapSession(ResultSet resultSet, int rowNumber) throws SQLException {
        return new CustomerSessionRecord(
                resultSet.getString("id"),
                resultSet.getString("customer_id"),
                resultSet.getString("customer_token"),
                resultSet.getString("scene"),
                nullableInstant(resultSet, "issued_at"),
                nullableInstant(resultSet, "expires_at"),
                resultSet.getString("status")
        );
    }

    private CustomerPointsAccountRecord mapPoints(ResultSet resultSet, int rowNumber) throws SQLException {
        return new CustomerPointsAccountRecord(
                resultSet.getString("id"),
                resultSet.getString("customer_id"),
                resultSet.getLong("balance"),
                resultSet.getString("level"),
                readObjectMap(resultSet.getString("recent_summary")),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private CustomerPointsLedgerRecord mapPointsLedger(ResultSet resultSet, int rowNumber) throws SQLException {
        return new CustomerPointsLedgerRecord(
                resultSet.getString("id"),
                resultSet.getString("customer_id"),
                resultSet.getLong("points_delta"),
                resultSet.getLong("balance_after"),
                resultSet.getString("source_type"),
                resultSet.getString("source_id"),
                resultSet.getString("description"),
                nullableInstant(resultSet, "created_at")
        );
    }

    private CustomerRedemptionCatalogItemRecord mapRedemptionCatalogItem(ResultSet resultSet, int rowNumber) throws SQLException {
        return new CustomerRedemptionCatalogItemRecord(
                resultSet.getString("id"),
                resultSet.getString("item_name"),
                resultSet.getString("item_type"),
                resultSet.getLong("points_cost"),
                resultSet.getString("inventory_item_id"),
                resultSet.getString("stock_location_id"),
                resultSet.getInt("stock_quantity"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "available_from"),
                nullableInstant(resultSet, "available_until"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private CustomerPointsRedemptionRecord mapPointsRedemption(ResultSet resultSet, int rowNumber) throws SQLException {
        return new CustomerPointsRedemptionRecord(
                resultSet.getString("id"),
                resultSet.getString("customer_id"),
                resultSet.getString("catalog_item_id"),
                resultSet.getString("item_name"),
                resultSet.getInt("quantity"),
                resultSet.getLong("points_cost"),
                resultSet.getLong("total_points"),
                resultSet.getString("inventory_item_id"),
                resultSet.getString("stock_location_id"),
                resultSet.getInt("stock_quantity"),
                resultSet.getString("source_terminal"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private CustomerBarReservationOptionResponse mapBarReservationOption(ResultSet resultSet, int rowNumber) throws SQLException {
        return new CustomerBarReservationOptionResponse(
                resultSet.getString("bar_id"),
                resultSet.getString("table_id"),
                resultSet.getString("table_type"),
                resultSet.getString("table_name"),
                resultSet.getInt("capacity"),
                resultSet.getInt("allowed_pass_free_guest_count"),
                resultSet.getLong("min_consumption_cent"),
                resultSet.getString("currency"),
                resultSet.getString("status")
        );
    }

    private BarTableRecord mapBarTable(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BarTableRecord(
                resultSet.getString("id"),
                resultSet.getString("bar_id"),
                resultSet.getString("table_code"),
                resultSet.getString("table_name"),
                resultSet.getString("table_type"),
                resultSet.getInt("capacity"),
                resultSet.getInt("allowed_pass_free_guest_count"),
                resultSet.getLong("min_consumption_cent"),
                resultSet.getString("currency"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private OffsetDateTime timestampWithTimeZone(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    private Instant nullableInstant(ResultSet resultSet, String columnName) throws SQLException {
        OffsetDateTime value = resultSet.getObject(columnName, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("customer JSON value is not serializable", exception);
        }
    }

    private Map<String, Object> readObjectMap(String value) {
        try {
            return value == null ? Map.of() : objectMapper.readValue(value, OBJECT_MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("customer JSON payload is not readable", exception);
        }
    }
}
