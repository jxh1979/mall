package com.complex.platform.serviceorder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcServiceOrderRepository implements ServiceOrderRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcServiceOrderRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ServiceCatalogItemRecord upsertCatalogItem(ServiceCatalogItemRecord item) {
        jdbcTemplate.update("""
                INSERT INTO service_catalog_items (id, name, price_cent, duration_minutes, active, updated_by, updated_at)
                VALUES (:id, :name, :price, :duration, :active, :updated_by, :now)
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name,
                    price_cent = EXCLUDED.price_cent,
                    duration_minutes = EXCLUDED.duration_minutes,
                    active = EXCLUDED.active,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", item.id()).addValue("name", item.name()).addValue("price", item.priceCent())
                .addValue("duration", item.durationMinutes()).addValue("active", item.active())
                .addValue("updated_by", item.updatedBy())
                .addValue("now", java.sql.Timestamp.from(item.updatedAt())));
        return item;
    }

    @Override
    public List<ServiceCatalogItemRecord> listCatalog(boolean activeOnly) {
        return jdbcTemplate.query("""
                SELECT id, name, price_cent, duration_minutes, active, updated_by, updated_at
                  FROM service_catalog_items
                 WHERE (:active_only = FALSE OR active = TRUE)
                 ORDER BY name
                """, new MapSqlParameterSource().addValue("active_only", activeOnly), this::mapItem);
    }

    @Override
    public Optional<ServiceCatalogItemRecord> findCatalogItem(String id) {
        return jdbcTemplate.query("""
                SELECT id, name, price_cent, duration_minutes, active, updated_by, updated_at
                  FROM service_catalog_items WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", id), this::mapItem).stream().findFirst();
    }

    private ServiceCatalogItemRecord mapItem(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new ServiceCatalogItemRecord(rs.getString("id"), rs.getString("name"), rs.getLong("price_cent"),
                (Integer) rs.getObject("duration_minutes"), rs.getBoolean("active"), rs.getString("updated_by"),
                rs.getTimestamp("updated_at").toInstant());
    }

    @Override
    public void upsertPaymentCode(String userId, String qrContent, Instant now) {
        jdbcTemplate.update("""
                INSERT INTO staff_payment_codes (user_id, qr_content, updated_at)
                VALUES (:user_id, :qr, :now)
                ON CONFLICT (user_id) DO UPDATE SET qr_content = EXCLUDED.qr_content, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource().addValue("user_id", userId).addValue("qr", qrContent)
                .addValue("now", java.sql.Timestamp.from(now)));
    }

    @Override
    public Optional<String> findPaymentCode(String userId) {
        return jdbcTemplate.query("SELECT qr_content FROM staff_payment_codes WHERE user_id = :user_id",
                new MapSqlParameterSource().addValue("user_id", userId),
                (rs, n) -> rs.getString("qr_content")).stream().findFirst();
    }

    @Override
    public ServiceOrderRecord saveOrder(ServiceOrderRecord order) {
        jdbcTemplate.update("""
                INSERT INTO service_orders (id, catalog_item_id, item_name, price_cent, bar_id, table_id,
                        performer_user_id, ordered_by, customer_note, status, cancel_reason,
                        created_at, updated_at, completed_at)
                VALUES (:id, :catalog_item_id, :item_name, :price, :bar_id, :table_id,
                        :performer, :ordered_by, :note, :status, :cancel_reason, :created_at, :updated_at, :completed_at)
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    cancel_reason = EXCLUDED.cancel_reason,
                    updated_at = EXCLUDED.updated_at,
                    completed_at = EXCLUDED.completed_at
                """, new MapSqlParameterSource()
                .addValue("id", order.id()).addValue("catalog_item_id", order.catalogItemId())
                .addValue("item_name", order.itemName()).addValue("price", order.priceCent())
                .addValue("bar_id", order.barId()).addValue("table_id", order.tableId())
                .addValue("performer", order.performerUserId()).addValue("ordered_by", order.orderedBy())
                .addValue("note", order.customerNote()).addValue("status", order.status())
                .addValue("cancel_reason", order.cancelReason())
                .addValue("created_at", java.sql.Timestamp.from(order.createdAt()))
                .addValue("updated_at", java.sql.Timestamp.from(order.updatedAt()))
                .addValue("completed_at", order.completedAt() == null ? null : java.sql.Timestamp.from(order.completedAt())));
        return order;
    }

    @Override
    public Optional<ServiceOrderRecord> findOrder(String id) {
        return jdbcTemplate.query(SELECT_ORDER + " WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id), this::mapOrder).stream().findFirst();
    }

    private static final String SELECT_ORDER = """
            SELECT id, catalog_item_id, item_name, price_cent, bar_id, table_id, performer_user_id,
                   ordered_by, customer_note, status, cancel_reason, created_at, updated_at, completed_at
              FROM service_orders""";

    @Override
    public List<ServiceOrderRecord> listOrdersForPerformer(String performerUserId) {
        return jdbcTemplate.query(SELECT_ORDER + """
                 WHERE performer_user_id = :performer
                 ORDER BY created_at DESC
                 LIMIT 200
                """, new MapSqlParameterSource().addValue("performer", performerUserId), this::mapOrder);
    }

    @Override
    public List<ServiceOrderRecord> listOrders(LocalDate date, String status) {
        return jdbcTemplate.query(SELECT_ORDER + """
                 WHERE (:date::date IS NULL OR created_at::date = :date)
                   AND (:status::text IS NULL OR status = :status)
                 ORDER BY created_at DESC
                 LIMIT 500
                """, new MapSqlParameterSource().addValue("date", date).addValue("status", status), this::mapOrder);
    }

    @Override
    public long[] performerMonthlyStats(String performerUserId, String month) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) AS cnt, COALESCE(SUM(price_cent), 0) AS total
                  FROM service_orders
                 WHERE performer_user_id = :performer AND status = 'completed'
                   AND to_char(completed_at, 'YYYY-MM') = :month
                """, new MapSqlParameterSource().addValue("performer", performerUserId).addValue("month", month),
                (rs, n) -> new long[] {rs.getLong("cnt"), rs.getLong("total")});
    }

    private ServiceOrderRecord mapOrder(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        java.sql.Timestamp completed = rs.getTimestamp("completed_at");
        return new ServiceOrderRecord(rs.getString("id"), rs.getString("catalog_item_id"),
                rs.getString("item_name"), rs.getLong("price_cent"), rs.getString("bar_id"),
                rs.getString("table_id"), rs.getString("performer_user_id"), rs.getString("ordered_by"),
                rs.getString("customer_note"), rs.getString("status"), rs.getString("cancel_reason"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant(),
                completed == null ? null : completed.toInstant());
    }
}
