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
                INSERT INTO service_catalog_items (id, name, price_cent, duration_minutes, active,
                        mgmt_fee_mode, mgmt_fee_value, updated_by, updated_at)
                VALUES (:id, :name, :price, :duration, :active, :fee_mode, :fee_value, :updated_by, :now)
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name,
                    price_cent = EXCLUDED.price_cent,
                    duration_minutes = EXCLUDED.duration_minutes,
                    active = EXCLUDED.active,
                    mgmt_fee_mode = EXCLUDED.mgmt_fee_mode,
                    mgmt_fee_value = EXCLUDED.mgmt_fee_value,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", item.id()).addValue("name", item.name()).addValue("price", item.priceCent())
                .addValue("duration", item.durationMinutes()).addValue("active", item.active())
                .addValue("fee_mode", item.mgmtFeeMode()).addValue("fee_value", item.mgmtFeeValue())
                .addValue("updated_by", item.updatedBy())
                .addValue("now", java.sql.Timestamp.from(item.updatedAt())));
        return item;
    }

    @Override
    public List<ServiceCatalogItemRecord> listCatalog(boolean activeOnly) {
        return jdbcTemplate.query("""
                SELECT id, name, price_cent, duration_minutes, active, mgmt_fee_mode, mgmt_fee_value, updated_by, updated_at
                  FROM service_catalog_items
                 WHERE (:active_only = FALSE OR active = TRUE)
                 ORDER BY name
                """, new MapSqlParameterSource().addValue("active_only", activeOnly), this::mapItem);
    }

    @Override
    public Optional<ServiceCatalogItemRecord> findCatalogItem(String id) {
        return jdbcTemplate.query("""
                SELECT id, name, price_cent, duration_minutes, active, mgmt_fee_mode, mgmt_fee_value, updated_by, updated_at
                  FROM service_catalog_items WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", id), this::mapItem).stream().findFirst();
    }

    private ServiceCatalogItemRecord mapItem(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new ServiceCatalogItemRecord(rs.getString("id"), rs.getString("name"), rs.getLong("price_cent"),
                (Integer) rs.getObject("duration_minutes"), rs.getBoolean("active"),
                rs.getString("mgmt_fee_mode"), rs.getLong("mgmt_fee_value"), rs.getString("updated_by"),
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
                        paid_at, paid_by, pay_method, refund_note, mgmt_fee_cent, dancer_share_cent,
                        created_at, updated_at, completed_at, snap_mgmt_fee_mode, snap_mgmt_fee_value, project_id)
                VALUES (:id, :catalog_item_id, :item_name, :price, :bar_id, :table_id,
                        :performer, :ordered_by, :note, :status, :cancel_reason,
                        :paid_at, :paid_by, :pay_method, :refund_note, :mgmt_fee, :dancer_share,
                        :created_at, :updated_at, :completed_at, :snap_fee_mode, :snap_fee_value,
                        (SELECT p.project_id FROM staff_profiles p WHERE p.user_id = :performer
                          ORDER BY p.created_at DESC LIMIT 1))
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    cancel_reason = EXCLUDED.cancel_reason,
                    paid_at = EXCLUDED.paid_at,
                    paid_by = EXCLUDED.paid_by,
                    pay_method = EXCLUDED.pay_method,
                    refund_note = EXCLUDED.refund_note,
                    mgmt_fee_cent = EXCLUDED.mgmt_fee_cent,
                    dancer_share_cent = EXCLUDED.dancer_share_cent,
                    updated_at = EXCLUDED.updated_at,
                    completed_at = EXCLUDED.completed_at
                """, new MapSqlParameterSource()
                .addValue("id", order.id()).addValue("catalog_item_id", order.catalogItemId())
                .addValue("item_name", order.itemName()).addValue("price", order.priceCent())
                .addValue("bar_id", order.barId()).addValue("table_id", order.tableId())
                .addValue("performer", order.performerUserId()).addValue("ordered_by", order.orderedBy())
                .addValue("note", order.customerNote()).addValue("status", order.status())
                .addValue("cancel_reason", order.cancelReason())
                .addValue("paid_at", order.paidAt() == null ? null : java.sql.Timestamp.from(order.paidAt()))
                .addValue("paid_by", order.paidBy()).addValue("pay_method", order.payMethod())
                .addValue("refund_note", order.refundNote())
                .addValue("mgmt_fee", order.mgmtFeeCent()).addValue("dancer_share", order.dancerShareCent())
                .addValue("created_at", java.sql.Timestamp.from(order.createdAt()))
                .addValue("updated_at", java.sql.Timestamp.from(order.updatedAt()))
                .addValue("completed_at", order.completedAt() == null ? null : java.sql.Timestamp.from(order.completedAt()))
                .addValue("snap_fee_mode", order.snapMgmtFeeMode())
                .addValue("snap_fee_value", order.snapMgmtFeeValue()));
        return order;
    }

    @Override
    public Optional<ServiceOrderRecord> findOrder(String id) {
        return jdbcTemplate.query(SELECT_ORDER + " WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id), this::mapOrder).stream().findFirst();
    }

    @Override
    public int markCompletedIfPaid(String id, long mgmtFeeCent, long dancerShareCent, Instant completedAt, Instant now) {
        // P1 并发守卫:paid->completed 原子转移。WHERE status='paid' 使受影响行数=1 成为唯一进入凭据。
        return jdbcTemplate.update("""
                UPDATE service_orders
                   SET status = 'completed', mgmt_fee_cent = :mgmt_fee, dancer_share_cent = :dancer_share,
                       completed_at = :completed_at, updated_at = :now
                 WHERE id = :id AND status = 'paid'
                """, new MapSqlParameterSource().addValue("id", id)
                .addValue("mgmt_fee", mgmtFeeCent).addValue("dancer_share", dancerShareCent)
                .addValue("completed_at", java.sql.Timestamp.from(completedAt))
                .addValue("now", java.sql.Timestamp.from(now)));
    }

    @Override
    public boolean insertReceivableForOrder(String orderId, long amountCent, Instant now) {
        // P1 收款对账:复用 ar_ap_items 应收台账。source 唯一索引保证一单一条(幂等)。
        // 公司代收即时到账 -> paid=amount、unpaid=0(满足账平 CHECK);counterparty='company'。
        int rows = jdbcTemplate.update("""
                INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id,
                        amount_cent, paid_amount_cent, unpaid_amount_cent, currency, due_date,
                        accounting_period, status, created_at, updated_at)
                VALUES (:id, :item_no, 'receivable', 'company', 'service_order', :order_id,
                        :amount, :amount, 0, 'CNY', :due_date,
                        :period, 'paid', :now, :now)
                ON CONFLICT (direction, source_type, source_id) WHERE source_id IS NOT NULL DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", "arap_svo_" + orderId)
                .addValue("item_no", "SVO-" + orderId)
                .addValue("order_id", orderId).addValue("amount", amountCent)
                .addValue("due_date", java.sql.Date.valueOf(
                        java.time.LocalDate.ofInstant(now, java.time.ZoneOffset.UTC)))
                .addValue("period", java.time.YearMonth.from(
                        java.time.LocalDate.ofInstant(now, java.time.ZoneOffset.UTC)).toString())
                .addValue("now", java.sql.Timestamp.from(now)));
        return rows == 1;
    }

    private static final String SELECT_ORDER = """
            SELECT id, catalog_item_id, item_name, price_cent, bar_id, table_id, performer_user_id,
                   ordered_by, customer_note, status, cancel_reason, paid_at, paid_by, pay_method,
                   refund_note, mgmt_fee_cent, dancer_share_cent, created_at, updated_at, completed_at,
                   snap_mgmt_fee_mode, snap_mgmt_fee_value
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
    public List<ServiceOrderRecord> listOrders(LocalDate date, String status, String projectScope) {
        // P0 租户隔离(V214):后台服务单列表按调用方项目 scope 过滤;null=平台全量。
        return jdbcTemplate.query(SELECT_ORDER + """
                 WHERE (:date::date IS NULL OR created_at::date = :date)
                   AND (:status::text IS NULL OR status = :status)
                   AND (:project_scope::text IS NULL OR project_id = :project_scope)
                 ORDER BY created_at DESC
                 LIMIT 500
                """, new MapSqlParameterSource().addValue("date", date).addValue("status", status)
                .addValue("project_scope", projectScope), this::mapOrder);
    }

    @Override
    public long[] performerMonthlyStats(String performerUserId, String month) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) AS cnt, COALESCE(SUM(dancer_share_cent), 0) AS total
                  FROM service_orders
                 WHERE performer_user_id = :performer AND status = 'completed'
                   AND to_char(completed_at, 'YYYY-MM') = :month
                """, new MapSqlParameterSource().addValue("performer", performerUserId).addValue("month", month),
                (rs, n) -> new long[] {rs.getLong("cnt"), rs.getLong("total")});
    }

    public void upsertPayConfig(String qrContent, String updatedBy, Instant now) {
        jdbcTemplate.update("""
                INSERT INTO service_pay_config (id, qr_content, updated_by, updated_at)
                VALUES ('default', :qr, :updated_by, :now)
                ON CONFLICT (id) DO UPDATE SET qr_content = EXCLUDED.qr_content,
                    updated_by = EXCLUDED.updated_by, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource().addValue("qr", qrContent)
                .addValue("updated_by", updatedBy).addValue("now", java.sql.Timestamp.from(now)));
    }

    public String companyPayQr() {
        return jdbcTemplate.query("SELECT qr_content FROM service_pay_config WHERE id = 'default'",
                new MapSqlParameterSource(), (rs, n) -> rs.getString("qr_content")).stream().findFirst().orElse("");
    }

    private ServiceOrderRecord mapOrder(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        java.sql.Timestamp completed = rs.getTimestamp("completed_at");
        java.sql.Timestamp paid = rs.getTimestamp("paid_at");
        return new ServiceOrderRecord(rs.getString("id"), rs.getString("catalog_item_id"),
                rs.getString("item_name"), rs.getLong("price_cent"), rs.getString("bar_id"),
                rs.getString("table_id"), rs.getString("performer_user_id"), rs.getString("ordered_by"),
                rs.getString("customer_note"), rs.getString("status"), rs.getString("cancel_reason"),
                paid == null ? null : paid.toInstant(), rs.getString("paid_by"), rs.getString("pay_method"),
                rs.getString("refund_note"), rs.getLong("mgmt_fee_cent"), rs.getLong("dancer_share_cent"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant(),
                completed == null ? null : completed.toInstant(),
                rs.getString("snap_mgmt_fee_mode"), (Long) rs.getObject("snap_mgmt_fee_value"));
    }
}
