package com.complex.platform.serviceorder;

// 文件版本：V0.04

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

// ponytail: 仓储直用实现类,无接口(one impl)。
@Repository
class JdbcServiceOrderRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcServiceOrderRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String SELECT_ITEM = """
            SELECT id, name, price_cent, duration_minutes, active, mgmt_fee_mode, mgmt_fee_value, updated_by, updated_at,
                   archived_at, archived_by, archive_reason, delete_status, delete_requested_by, delete_requested_at, delete_reason,
                   fee_version, next_mgmt_fee_mode, next_mgmt_fee_value, next_effective_from, next_requested_by, next_approved_by
              FROM service_catalog_items""";

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
        return findCatalogItem(item.id()).orElse(item);
    }

    public List<ServiceCatalogItemRecord> listCatalog(boolean activeOnly, boolean includeArchived) {
        return jdbcTemplate.query(SELECT_ITEM + """

                 WHERE delete_status <> 'approved'
                   AND (:active_only = FALSE OR active = TRUE)
                   AND (:include_archived = TRUE OR archived_at IS NULL)
                 ORDER BY name
                """, new MapSqlParameterSource().addValue("active_only", activeOnly).addValue("include_archived", includeArchived), this::mapItem);
    }

    public void archiveCatalogItem(String id, boolean archive, String reason, String actorId) {
        jdbcTemplate.update("""
                UPDATE service_catalog_items
                   SET archived_at = :archived_at, archived_by = :archived_by, archive_reason = :reason, updated_at = now()
                 WHERE id = :id AND delete_status <> 'approved'
                """, new MapSqlParameterSource().addValue("id", id)
                .addValue("archived_at", archive ? java.sql.Timestamp.from(java.time.Instant.now()) : null)
                .addValue("archived_by", archive ? actorId : null)
                .addValue("reason", archive ? reason : null));
    }

    public void requestDeleteCatalogItem(String id, String reason, String actorId) {
        jdbcTemplate.update("""
                UPDATE service_catalog_items
                   SET delete_status = 'pending', delete_requested_by = :by, delete_requested_at = now(), delete_reason = :reason, updated_at = now()
                 WHERE id = :id AND delete_status <> 'approved'
                """, new MapSqlParameterSource().addValue("id", id).addValue("by", actorId).addValue("reason", reason));
    }

    public void approveDeleteCatalogItem(String id, boolean approved) {
        jdbcTemplate.update("""
                UPDATE service_catalog_items SET delete_status = :status, updated_at = now()
                 WHERE id = :id AND delete_status = 'pending'
                """, new MapSqlParameterSource().addValue("id", id).addValue("status", approved ? "approved" : "none"));
    }

    public Optional<ServiceCatalogItemRecord> findCatalogItem(String id) {
        return jdbcTemplate.query(SELECT_ITEM + " WHERE id = :id AND delete_status <> 'approved'",
                new MapSqlParameterSource().addValue("id", id), this::mapItem).stream().findFirst();
    }

    /** TK-23-R3:落管理费草案(覆盖未转正的旧草案;复核人清空,须重新差人复核)。 */
    public void saveFeeDraft(String id, String feeMode, long feeValue, LocalDate effectiveFrom, String actorId, Instant now) {
        jdbcTemplate.update("""
                UPDATE service_catalog_items
                   SET next_mgmt_fee_mode = :mode, next_mgmt_fee_value = :value, next_effective_from = :from,
                       next_requested_by = :by, next_requested_at = :now, next_approved_by = NULL, updated_at = :now
                 WHERE id = :id AND delete_status <> 'approved'
                """, new MapSqlParameterSource().addValue("id", id).addValue("mode", feeMode).addValue("value", feeValue)
                .addValue("from", java.sql.Date.valueOf(effectiveFrom)).addValue("by", actorId)
                .addValue("now", java.sql.Timestamp.from(now)));
    }

    /** TK-23-R3:差人复核通过,只标记复核人;生效日到了由下单路径翻为现行版本。 */
    public void approveFeeDraft(String id, String approverId) {
        jdbcTemplate.update("""
                UPDATE service_catalog_items SET next_approved_by = :by, updated_at = now()
                 WHERE id = :id AND next_requested_by IS NOT NULL
                """, new MapSqlParameterSource().addValue("id", id).addValue("by", approverId));
    }

    /** TK-23-R3:已复核草案在生效营业日(含)起翻为现行:fee_version+1、清空草案。WHERE 条件使并发下单只翻一次。 */
    public int promoteApprovedFeeDraft(String id, LocalDate businessDate) {
        return jdbcTemplate.update("""
                UPDATE service_catalog_items
                   SET mgmt_fee_mode = next_mgmt_fee_mode, mgmt_fee_value = next_mgmt_fee_value,
                       fee_version = fee_version + 1, updated_by = next_approved_by, updated_at = now(),
                       next_mgmt_fee_mode = NULL, next_mgmt_fee_value = NULL, next_effective_from = NULL,
                       next_requested_by = NULL, next_requested_at = NULL, next_approved_by = NULL
                 WHERE id = :id AND next_approved_by IS NOT NULL AND next_effective_from <= :today
                """, new MapSqlParameterSource().addValue("id", id).addValue("today", java.sql.Date.valueOf(businessDate)));
    }

    /** 管理费通告候选:在职员工(账号 active 且员工档案 active);project 非空时只取该项目员工档案。权限码由服务层逐个核。 */
    public List<com.complex.platform.access.AccessActor> listActiveStaffActors(String project) {
        return jdbcTemplate.query("""
                SELECT DISTINCT u.id, u.username, u.account_type, u.organization_id, u.merchant_id, u.status
                  FROM access_users u
                  JOIN staff_profiles sp ON sp.user_id = u.id
                 WHERE u.status = 'active' AND sp.status = 'active'
                   AND (CAST(:project AS VARCHAR) IS NULL OR sp.project_id = :project)
                 ORDER BY u.id
                """, new MapSqlParameterSource().addValue("project", project),
                (rs, n) -> new com.complex.platform.access.AccessActor(rs.getString("id"), rs.getString("username"),
                        rs.getString("account_type"), rs.getString("organization_id"), rs.getString("merchant_id"), rs.getString("status")));
    }

    /** 该目录项服务单发生过的酒吧的主理人:各酒吧当前有效分红方案(status='active',每商户至多一条)的 principal_user_id。 */
    public List<String> listBarPrincipalUserIds(String catalogItemId) {
        return jdbcTemplate.query("""
                SELECT DISTINCT dp.principal_user_id
                  FROM service_orders o
                  JOIN dividend_plans dp ON dp.merchant_id = o.bar_id AND dp.status = 'active'
                 WHERE o.catalog_item_id = :item
                """, new MapSqlParameterSource().addValue("item", catalogItemId),
                (rs, n) -> rs.getString("principal_user_id"));
    }

    private ServiceCatalogItemRecord mapItem(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        java.sql.Timestamp arch = rs.getTimestamp("archived_at");
        java.sql.Timestamp delReq = rs.getTimestamp("delete_requested_at");
        java.sql.Date nextFrom = rs.getDate("next_effective_from");
        return new ServiceCatalogItemRecord(rs.getString("id"), rs.getString("name"), rs.getLong("price_cent"),
                (Integer) rs.getObject("duration_minutes"), rs.getBoolean("active"),
                rs.getString("mgmt_fee_mode"), rs.getLong("mgmt_fee_value"), rs.getString("updated_by"),
                rs.getTimestamp("updated_at").toInstant(),
                arch == null ? null : arch.toInstant(), rs.getString("archived_by"), rs.getString("archive_reason"),
                rs.getString("delete_status"), rs.getString("delete_requested_by"),
                delReq == null ? null : delReq.toInstant(), rs.getString("delete_reason"),
                rs.getInt("fee_version"), rs.getString("next_mgmt_fee_mode"), (Long) rs.getObject("next_mgmt_fee_value"),
                nextFrom == null ? null : nextFrom.toLocalDate(), rs.getString("next_requested_by"), rs.getString("next_approved_by"));
    }

    public void upsertPaymentCode(String userId, String qrContent, Instant now) {
        jdbcTemplate.update("""
                INSERT INTO staff_payment_codes (user_id, qr_content, updated_at)
                VALUES (:user_id, :qr, :now)
                ON CONFLICT (user_id) DO UPDATE SET qr_content = EXCLUDED.qr_content, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource().addValue("user_id", userId).addValue("qr", qrContent)
                .addValue("now", java.sql.Timestamp.from(now)));
    }

    public Optional<String> findPaymentCode(String userId) {
        return jdbcTemplate.query("SELECT qr_content FROM staff_payment_codes WHERE user_id = :user_id",
                new MapSqlParameterSource().addValue("user_id", userId),
                (rs, n) -> rs.getString("qr_content")).stream().findFirst();
    }

    public ServiceOrderRecord saveOrder(ServiceOrderRecord order) {
        return saveOrder(order, null);
    }

    /** 第十八批 AB 段复核意见（第 191 条；BSM:1143「由该酒吧负责排班、现场考勤和履约调度」；PGM:659「所有业务数据默认按项目隔离」）：经派驻放行新建的服务单
     *  project_id 落派驻目标项目（dispatchProjectId），其余照旧按履约人档案；只影响插入，ON CONFLICT 更新段不动。 */
    public ServiceOrderRecord saveOrder(ServiceOrderRecord order, String dispatchProjectId) {
        jdbcTemplate.update("""
                INSERT INTO service_orders (id, catalog_item_id, item_name, price_cent, bar_id, table_id,
                        performer_user_id, ordered_by, customer_note, status, cancel_reason,
                        paid_at, paid_by, pay_method, refund_note, mgmt_fee_cent, dancer_share_cent,
                        created_at, updated_at, completed_at, snap_mgmt_fee_mode, snap_mgmt_fee_value, snap_fee_version, project_id)
                VALUES (:id, :catalog_item_id, :item_name, :price, :bar_id, :table_id,
                        :performer, :ordered_by, :note, :status, :cancel_reason,
                        :paid_at, :paid_by, :pay_method, :refund_note, :mgmt_fee, :dancer_share,
                        :created_at, :updated_at, :completed_at, :snap_fee_mode, :snap_fee_value, :snap_fee_version,
                        COALESCE(CAST(:dispatch_project_id AS VARCHAR), (SELECT p.project_id FROM staff_profiles p WHERE p.user_id = :performer
                          ORDER BY p.created_at DESC LIMIT 1)))
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
                .addValue("snap_fee_value", order.snapMgmtFeeValue())
                .addValue("snap_fee_version", order.snapFeeVersion())
                .addValue("dispatch_project_id", dispatchProjectId));
        return order;
    }

    public Optional<ServiceOrderRecord> findOrder(String id) {
        return jdbcTemplate.query(SELECT_ORDER + " WHERE id = :id",
                new MapSqlParameterSource().addValue("id", id), this::mapOrder).stream().findFirst();
    }

    /** 服务单所属项目（下单时按履约人档案落的 project_id），查不到为 null。 */
    public String findOrderProjectId(String id) {
        return jdbcTemplate.queryForList("SELECT project_id FROM service_orders WHERE id = :id AND project_id IS NOT NULL",
                new MapSqlParameterSource().addValue("id", id), String.class).stream().findFirst().orElse(null);
    }

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

    /** TK-23-R2:应收行 due_date/accounting_period 按收款时点所属营业日(调用方用 BusinessDayResolver 按酒吧或项目算),不按 UTC。 */
    public boolean insertReceivableForOrder(String orderId, long amountCent, LocalDate businessDate, Instant now) {
        // P1 收款对账:复用 ar_ap_items 应收台账。source 唯一索引保证一单一条(幂等)。
        // V344(P0-B 红冲):该部分唯一索引的谓词加了 origin_item_id IS NULL(排除补录派生的冲正/重述行),
        //   ON CONFLICT 推断须与索引谓词逐字一致,否则 PG 报"no unique or exclusion constraint matching"。
        // 公司代收即时到账 -> paid=amount、unpaid=0(满足账平 CHECK);counterparty='company'。
        int rows = jdbcTemplate.update("""
                INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id,
                        amount_cent, paid_amount_cent, unpaid_amount_cent, currency, due_date,
                        accounting_period, status, created_at, updated_at)
                VALUES (:id, :item_no, 'receivable', 'company', 'service_order', :order_id,
                        :amount, :amount, 0, 'CNY', :due_date,
                        :period, 'paid', :now, :now)
                ON CONFLICT (direction, source_type, source_id)
                        WHERE source_id IS NOT NULL AND origin_item_id IS NULL DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", "arap_svo_" + orderId)
                .addValue("item_no", "SVO-" + orderId)
                .addValue("order_id", orderId).addValue("amount", amountCent)
                .addValue("due_date", java.sql.Date.valueOf(businessDate))
                .addValue("period", java.time.YearMonth.from(businessDate).toString())
                .addValue("now", java.sql.Timestamp.from(now)));
        return rows == 1;
    }

    private static final String SELECT_ORDER = """
            SELECT id, catalog_item_id, item_name, price_cent, bar_id, table_id, performer_user_id,
                   ordered_by, customer_note, status, cancel_reason, paid_at, paid_by, pay_method,
                   refund_note, mgmt_fee_cent, dancer_share_cent, created_at, updated_at, completed_at,
                   snap_mgmt_fee_mode, snap_mgmt_fee_value, snap_fee_version
              FROM service_orders""";

    public List<ServiceOrderRecord> listOrdersForPerformer(String performerUserId) {
        return jdbcTemplate.query(SELECT_ORDER + """
                 WHERE performer_user_id = :performer
                 ORDER BY created_at DESC
                 LIMIT 200
                """, new MapSqlParameterSource().addValue("performer", performerUserId), this::mapOrder);
    }

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

    /** 第十八批 E 段 19-R1（移植 M738）：按项目集合取后台服务单；null＝全量（平台管理员），空集合＝一行不出（空 IN () 是语法错误，null 时用占位）。 */
    public List<ServiceOrderRecord> listOrdersInProjects(LocalDate date, String status, java.util.Collection<String> projectIds) {
        if (projectIds != null && projectIds.isEmpty()) return List.of();
        return jdbcTemplate.query(SELECT_ORDER + """
                 WHERE (:date::date IS NULL OR created_at::date = :date)
                   AND (:status::text IS NULL OR status = :status)
                   AND (:all_projects OR project_id IN (:project_ids))
                 ORDER BY created_at DESC
                 LIMIT 500
                """, new MapSqlParameterSource().addValue("date", date).addValue("status", status)
                .addValue("all_projects", projectIds == null)
                .addValue("project_ids", projectIds == null ? List.of("") : projectIds), this::mapOrder);
    }

    /** TK-23-R2:履约人月度统计与薪资月结同源——按 staff_commission_ledger(user_id, service_order, month) 计数/求和,
     * month 为完成时点所在 bar 营业日的年月(不再 to_char(completed_at) 按库会话时区)。 */
    public long[] performerMonthlyStats(String performerUserId, String month) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) AS cnt, COALESCE(SUM(amount_cent), 0) AS total
                  FROM staff_commission_ledger
                 WHERE user_id = :performer AND source_type = 'service_order' AND month = :month
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
                rs.getString("snap_mgmt_fee_mode"), (Long) rs.getObject("snap_mgmt_fee_value"),
                (Integer) rs.getObject("snap_fee_version"));
    }
}
