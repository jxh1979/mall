package com.complex.platform.voucher;

// 文件版本：V0.01

import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.complex.platform.common.api.PageResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 统一券台账 JDBC 仓储。核销记录 append-only：只 INSERT ON CONFLICT DO NOTHING。
 * merchantScope 谓词：商户 scope 账号见（自家商户∪平台通用 merchant_id IS NULL）券。
 */
@Repository
class JdbcVoucherRepository implements VoucherRepository {

    private static final String DEFINITION_COLS =
            "id, name, voucher_kind, redeem_method, settlement_mode, merchant_id, face_value_cent, "
                    + "inventory_item_id, stock_location_id, stock_quantity, internal_price_cent, "
                    + "valid_days, valid_until, status, transferable, created_at, updated_at, "
                    // 操作员裁决 2026-10-20 第三轮第 9 条：带出券定义商户的业态（只读派生），「必须结账抵扣」只对酒吧商户生效。
                    + "(SELECT m.merchant_type FROM merchants m WHERE m.id = voucher_definitions.merchant_id) AS merchant_type";
    private static final String INSTANCE_COLS =
            "id, definition_id, customer_id, code, source_type, source_ref, status, issued_at, "
                    + "expires_at, redeemed_at, revoked_at, revoke_reason, stock_reserved, created_at, updated_at";
    private static final String REDEMPTION_COLS =
            "id, voucher_instance_id, definition_id, voucher_kind, customer_id, merchant_id, operator_user_id, "
                    + "redeem_method, settlement_mode, settlement_amount_cent, ar_ap_item_id, stock_movement_ref, "
                    + "external_redemption_ref, note, redeemed_at, created_at, bill_type, bill_id, deduction_amount_cent";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcVoucherRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ── 券定义 ─────────────────────────────────────────────────────────────

    @Override
    public Optional<VoucherDefinitionRecord> findDefinitionById(String id) {
        return jdbcTemplate.query(
                "SELECT " + DEFINITION_COLS + " FROM voucher_definitions WHERE id = :id",
                new MapSqlParameterSource("id", id), this::mapDefinition).stream().findFirst();
    }

    @Override
    public Optional<VoucherDefinitionRecord> findDefinitionForCatalogItem(String catalogItemId) {
        // 目录显式挂接（voucher_definition_id）优先，否则回落派生定义 vdef_cat_<目录id>。
        return jdbcTemplate.query("""
                SELECT %s FROM voucher_definitions
                 WHERE id = (SELECT COALESCE(c.voucher_definition_id, 'vdef_cat_' || c.id)
                               FROM customer_redemption_catalog_items c
                              WHERE c.id = :catalog_item_id)
                """.formatted(DEFINITION_COLS),
                new MapSqlParameterSource("catalog_item_id", catalogItemId), this::mapDefinition)
                .stream().findFirst();
    }

    @Override
    public Optional<String> findCatalogItemMerchant(String itemCode) {
        return Optional.ofNullable(jdbcTemplate.query("""
                SELECT merchant_id FROM internal_price_catalog
                 WHERE item_code = :item_code AND status = 'active'
                 ORDER BY is_draft LIMIT 1
                """, new MapSqlParameterSource("item_code", itemCode),
                rs -> rs.next() ? rs.getString(1) : null));
    }

    @Override
    public Optional<Long> findActiveCatalogPriceCent(String itemCode) {
        return Optional.ofNullable(jdbcTemplate.query("""
                SELECT price_cent FROM internal_price_catalog
                 WHERE item_code = :item_code AND is_draft = FALSE AND status = 'active'
                """, new MapSqlParameterSource("item_code", itemCode),
                rs -> rs.next() ? rs.getLong(1) : null));
    }

    @Override
    public void setDefinitionCatalogRef(String definitionId, String itemCode) {
        jdbcTemplate.update(
                "UPDATE voucher_definitions SET catalog_item_code = :item_code, updated_at = now() WHERE id = :id",
                new MapSqlParameterSource("id", definitionId).addValue("item_code", itemCode));
    }

    @Override
    public boolean insertDefinition(VoucherDefinitionRecord definition) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO voucher_definitions (
                    id, name, voucher_kind, redeem_method, settlement_mode, merchant_id, face_value_cent,
                    inventory_item_id, stock_location_id, stock_quantity, internal_price_cent,
                    valid_days, valid_until, status, transferable, created_at, updated_at
                )
                VALUES (
                    :id, :name, :voucher_kind, :redeem_method, :settlement_mode, :merchant_id, :face_value_cent,
                    :inventory_item_id, :stock_location_id, :stock_quantity, :internal_price_cent,
                    :valid_days, :valid_until, :status, :transferable, :created_at, :updated_at
                )
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", definition.id())
                .addValue("name", definition.name())
                .addValue("voucher_kind", definition.voucherKind())
                .addValue("redeem_method", definition.redeemMethod())
                .addValue("settlement_mode", definition.settlementMode())
                .addValue("merchant_id", definition.merchantId(), Types.VARCHAR)
                .addValue("face_value_cent", definition.faceValueCent(), Types.BIGINT)
                .addValue("inventory_item_id", definition.inventoryItemId(), Types.VARCHAR)
                .addValue("stock_location_id", definition.stockLocationId(), Types.VARCHAR)
                .addValue("stock_quantity", definition.stockQuantity(), Types.INTEGER)
                .addValue("internal_price_cent", definition.internalPriceCent(), Types.BIGINT)
                .addValue("valid_days", definition.validDays(), Types.INTEGER)
                .addValue("valid_until", timestampWithTimeZone(definition.validUntil()))
                .addValue("status", definition.status())
                .addValue("transferable", definition.transferable())
                .addValue("created_at", timestampWithTimeZone(definition.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(definition.updatedAt())));
        return inserted > 0;
    }

    private static final String DEFINITION_SCOPE_WHERE = """
             WHERE (:merchant_scope IS NULL OR merchant_id = :merchant_scope OR merchant_id IS NULL)
               AND (:status IS NULL OR status = :status)
               AND (:voucher_kind IS NULL OR voucher_kind = :voucher_kind)
            """;

    @Override
    public PageResponse<VoucherDefinitionRecord> listDefinitions(
            String merchantScope, String status, String voucherKind, int page, int pageSize) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("merchant_scope", merchantScope, Types.VARCHAR)
                .addValue("status", status, Types.VARCHAR)
                .addValue("voucher_kind", voucherKind, Types.VARCHAR)
                .addValue("limit", pageSize)
                .addValue("offset", (long) (page - 1) * pageSize);
        List<VoucherDefinitionRecord> items = jdbcTemplate.query(
                "SELECT " + DEFINITION_COLS + " FROM voucher_definitions" + DEFINITION_SCOPE_WHERE
                        + " ORDER BY created_at DESC, id DESC LIMIT :limit OFFSET :offset",
                params, this::mapDefinition);
        Long total = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM voucher_definitions" + DEFINITION_SCOPE_WHERE, params, Long.class);
        return new PageResponse<>(items, page, pageSize, total == null ? 0L : total);
    }

    // ── 券实例 ─────────────────────────────────────────────────────────────

    @Override
    public Optional<VoucherInstanceRecord> findInstanceById(String id) {
        return jdbcTemplate.query(
                "SELECT " + INSTANCE_COLS + " FROM voucher_instances WHERE id = :id",
                new MapSqlParameterSource("id", id), this::mapInstance).stream().findFirst();
    }

    @Override
    public Optional<VoucherInstanceRecord> findInstanceByCode(String code) {
        return jdbcTemplate.query(
                "SELECT " + INSTANCE_COLS + " FROM voucher_instances WHERE code = :code",
                new MapSqlParameterSource("code", code), this::mapInstance).stream().findFirst();
    }

    @Override
    public boolean insertInstance(VoucherInstanceRecord instance) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO voucher_instances (
                    id, definition_id, customer_id, code, source_type, source_ref, status, issued_at,
                    expires_at, redeemed_at, revoked_at, revoke_reason, stock_reserved, created_at, updated_at
                )
                VALUES (
                    :id, :definition_id, :customer_id, :code, :source_type, :source_ref, :status, :issued_at,
                    :expires_at, :redeemed_at, :revoked_at, :revoke_reason, :stock_reserved, :created_at, :updated_at
                )
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", instance.id())
                .addValue("definition_id", instance.definitionId())
                .addValue("customer_id", instance.customerId())
                .addValue("code", instance.code())
                .addValue("source_type", instance.sourceType())
                .addValue("source_ref", instance.sourceRef(), Types.VARCHAR)
                .addValue("status", instance.status())
                .addValue("issued_at", timestampWithTimeZone(instance.issuedAt()))
                .addValue("expires_at", timestampWithTimeZone(instance.expiresAt()))
                .addValue("redeemed_at", timestampWithTimeZone(instance.redeemedAt()))
                .addValue("revoked_at", timestampWithTimeZone(instance.revokedAt()))
                .addValue("revoke_reason", instance.revokeReason(), Types.VARCHAR)
                .addValue("stock_reserved", instance.stockReserved())
                .addValue("created_at", timestampWithTimeZone(instance.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(instance.updatedAt())));
        return inserted > 0;
    }

    // V313 转赠领取：券实例归属改受赠人。守卫 status='issued' 防对已核销/失效券误改归属；at 同刷 updated_at。
    @Override
    public int updateInstanceCustomer(String id, String newCustomerId, Instant at) {
        return jdbcTemplate.update("""
                UPDATE voucher_instances
                   SET customer_id = :customer_id, updated_at = :at
                 WHERE id = :id AND status = 'issued'
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("customer_id", newCustomerId)
                .addValue("at", timestampWithTimeZone(at)));
    }

    // V312 过期释放扫描：issued + 已预占 + 已过期的实例（定时作业逐条 CAS 过期并释放预占）。
    @Override
    public List<VoucherInstanceRecord> listExpiredReservedIssuedInstances(Instant now, int limit) {
        return jdbcTemplate.query(
                "SELECT " + INSTANCE_COLS + " FROM voucher_instances"
                        + " WHERE status = 'issued' AND stock_reserved = true"
                        + "   AND expires_at IS NOT NULL AND expires_at <= :now"
                        + " ORDER BY expires_at ASC LIMIT :limit",
                new MapSqlParameterSource().addValue("now", timestampWithTimeZone(now)).addValue("limit", limit),
                this::mapInstance);
    }

    @Override
    public int updateInstanceStatus(String id, String expectFrom, String toStatus, Instant at, String revokeReason) {
        return jdbcTemplate.update("""
                UPDATE voucher_instances
                   SET status = :to_status,
                       redeemed_at = CASE WHEN :to_status = 'redeemed' THEN :at ELSE redeemed_at END,
                       revoked_at = CASE WHEN :to_status = 'revoked' THEN :at ELSE revoked_at END,
                       revoke_reason = CASE WHEN :to_status = 'revoked' THEN :revoke_reason ELSE revoke_reason END,
                       updated_at = :at
                 WHERE id = :id AND status = :expect_from
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("expect_from", expectFrom)
                .addValue("to_status", toStatus)
                .addValue("at", timestampWithTimeZone(at))
                .addValue("revoke_reason", revokeReason, Types.VARCHAR));
    }

    private static final String INSTANCE_SCOPE_WHERE = """
             WHERE (:merchant_scope IS NULL
                    OR EXISTS (SELECT 1 FROM voucher_definitions d
                                WHERE d.id = voucher_instances.definition_id
                                  AND (d.merchant_id = :merchant_scope OR d.merchant_id IS NULL)))
               AND (:customer_id IS NULL OR customer_id = :customer_id)
               AND (:definition_id IS NULL OR definition_id = :definition_id)
               AND (:status IS NULL OR status = :status)
               AND (:source_type IS NULL OR source_type = :source_type)
            """;

    @Override
    public PageResponse<VoucherInstanceRecord> listInstances(
            String merchantScope, String customerId, String definitionId, String status, String sourceType,
            int page, int pageSize) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("merchant_scope", merchantScope, Types.VARCHAR)
                .addValue("customer_id", customerId, Types.VARCHAR)
                .addValue("definition_id", definitionId, Types.VARCHAR)
                .addValue("status", status, Types.VARCHAR)
                .addValue("source_type", sourceType, Types.VARCHAR)
                .addValue("limit", pageSize)
                .addValue("offset", (long) (page - 1) * pageSize);
        List<VoucherInstanceRecord> items = jdbcTemplate.query(
                "SELECT " + INSTANCE_COLS + " FROM voucher_instances" + INSTANCE_SCOPE_WHERE
                        + " ORDER BY issued_at DESC, id DESC LIMIT :limit OFFSET :offset",
                params, this::mapInstance);
        Long total = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM voucher_instances" + INSTANCE_SCOPE_WHERE, params, Long.class);
        return new PageResponse<>(items, page, pageSize, total == null ? 0L : total);
    }

    // ── 核销记录（append-only）─────────────────────────────────────────────

    @Override
    public boolean insertRedemption(VoucherRedemptionEntryRecord entry) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO voucher_redemption_records (
                    id, voucher_instance_id, definition_id, voucher_kind, customer_id, merchant_id, operator_user_id,
                    redeem_method, settlement_mode, settlement_amount_cent, ar_ap_item_id, stock_movement_ref,
                    external_redemption_ref, note, redeemed_at, created_at, bill_type, bill_id, deduction_amount_cent
                )
                VALUES (
                    :id, :voucher_instance_id, :definition_id, :voucher_kind, :customer_id, :merchant_id, :operator_user_id,
                    :redeem_method, :settlement_mode, :settlement_amount_cent, :ar_ap_item_id, :stock_movement_ref,
                    :external_redemption_ref, :note, :redeemed_at, :created_at, :bill_type, :bill_id, :deduction_amount_cent
                )
                ON CONFLICT (voucher_instance_id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("bill_type", entry.billType(), Types.VARCHAR)
                .addValue("bill_id", entry.billId(), Types.VARCHAR)
                .addValue("deduction_amount_cent", entry.deductionAmountCent(), Types.BIGINT)
                .addValue("id", entry.id())
                .addValue("voucher_instance_id", entry.voucherInstanceId())
                .addValue("definition_id", entry.definitionId())
                .addValue("voucher_kind", entry.voucherKind())
                .addValue("customer_id", entry.customerId())
                .addValue("merchant_id", entry.merchantId(), Types.VARCHAR)
                .addValue("operator_user_id", entry.operatorUserId(), Types.VARCHAR)
                .addValue("redeem_method", entry.redeemMethod())
                .addValue("settlement_mode", entry.settlementMode())
                .addValue("settlement_amount_cent", entry.settlementAmountCent(), Types.BIGINT)
                .addValue("ar_ap_item_id", entry.arApItemId(), Types.VARCHAR)
                .addValue("stock_movement_ref", entry.stockMovementRef(), Types.VARCHAR)
                .addValue("external_redemption_ref", entry.externalRedemptionRef(), Types.VARCHAR)
                .addValue("note", entry.note(), Types.VARCHAR)
                .addValue("redeemed_at", timestampWithTimeZone(entry.redeemedAt()))
                .addValue("created_at", timestampWithTimeZone(entry.createdAt())));
        return inserted > 0;
    }

    private static final String REDEMPTION_WHERE = """
             WHERE (:merchant_id IS NULL OR merchant_id = :merchant_id)
               AND (:voucher_instance_id IS NULL OR voucher_instance_id = :voucher_instance_id)
            """;

    @Override
    public PageResponse<VoucherRedemptionEntryRecord> listRedemptions(
            String merchantId, String voucherInstanceId, int page, int pageSize) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("merchant_id", merchantId, Types.VARCHAR)
                .addValue("voucher_instance_id", voucherInstanceId, Types.VARCHAR)
                .addValue("limit", pageSize)
                .addValue("offset", (long) (page - 1) * pageSize);
        List<VoucherRedemptionEntryRecord> items = jdbcTemplate.query(
                "SELECT " + REDEMPTION_COLS + " FROM voucher_redemption_records" + REDEMPTION_WHERE
                        + " ORDER BY redeemed_at DESC, id DESC LIMIT :limit OFFSET :offset",
                params, this::mapRedemption);
        Long total = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM voucher_redemption_records" + REDEMPTION_WHERE, params, Long.class);
        return new PageResponse<>(items, page, pageSize, total == null ? 0L : total);
    }

    // ── "全部券"聚合（读层并入不搬表）──────────────────────────────────────

    private static final String OVERVIEW_UNION = """
            SELECT vi.id AS id, 'unified' AS source, vd.name AS name, vd.voucher_kind AS voucher_kind,
                   vi.customer_id AS customer_id, vi.status AS status,
                   CASE WHEN vi.status = 'issued' THEN 1 ELSE 0 END AS remaining_times,
                   vi.expires_at AS expires_at, vd.face_value_cent AS face_value_cent,
                   vd.redeem_method AS redeem_method, vi.code AS code, vi.issued_at AS sort_time
              FROM voucher_instances vi
              JOIN voucher_definitions vd ON vd.id = vi.definition_id
             WHERE (:merchant_scope IS NULL OR vd.merchant_id = :merchant_scope OR vd.merchant_id IS NULL)
               AND (:customer_id IS NULL OR vi.customer_id = :customer_id)
            UNION ALL
            SELECT ie.id AS id, 'pass_partner' AS source, et.name AS name, 'external_partner' AS voucher_kind,
                   o.customer_id AS customer_id, ie.status AS status,
                   GREATEST(ie.total_times - ie.used_times, 0) AS remaining_times,
                   ie.valid_to AS expires_at, CAST(NULL AS BIGINT) AS face_value_cent,
                   'external_reflow' AS redeem_method, CAST(NULL AS VARCHAR) AS code, ie.created_at AS sort_time
              FROM issued_entitlements ie
              JOIN partner_voucher_configs pvc ON pvc.entitlement_template_id = ie.entitlement_template_id
              JOIN entitlement_templates et ON et.id = ie.entitlement_template_id
              LEFT JOIN ticket_orders o ON o.id = ie.order_id
             WHERE (:merchant_scope IS NULL OR pvc.partner_id = :merchant_scope)
               AND (:customer_id IS NULL OR o.customer_id = :customer_id)
            """;

    @Override
    public PageResponse<VoucherOverviewRecord> overview(
            String merchantScope, String customerId, int page, int pageSize) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("merchant_scope", merchantScope, Types.VARCHAR)
                .addValue("customer_id", customerId, Types.VARCHAR)
                .addValue("limit", pageSize)
                .addValue("offset", (long) (page - 1) * pageSize);
        List<VoucherOverviewRecord> items = jdbcTemplate.query(
                "SELECT id, source, name, voucher_kind, customer_id, status, remaining_times, expires_at, face_value_cent, redeem_method, code FROM ("
                        + OVERVIEW_UNION
                        + ") all_vouchers ORDER BY sort_time DESC, id DESC LIMIT :limit OFFSET :offset",
                params,
                (rs, rowNum) -> new VoucherOverviewRecord(
                        rs.getString("id"),
                        rs.getString("source"),
                        rs.getString("name"),
                        rs.getString("voucher_kind"),
                        rs.getString("customer_id"),
                        rs.getString("status"),
                        rs.getInt("remaining_times"),
                        nullableInstant(rs, "expires_at"),
                        rs.getObject("face_value_cent", Long.class),
                        rs.getString("redeem_method"),
                        rs.getString("code")));
        Long total = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM (" + OVERVIEW_UNION + ") all_vouchers", params, Long.class);
        return new PageResponse<>(items, page, pageSize, total == null ? 0L : total);
    }

    // ── 行映射 ─────────────────────────────────────────────────────────────

    private VoucherDefinitionRecord mapDefinition(ResultSet rs, int rowNum) throws SQLException {
        return new VoucherDefinitionRecord(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("voucher_kind"),
                rs.getString("redeem_method"),
                rs.getString("settlement_mode"),
                rs.getString("merchant_id"),
                rs.getObject("face_value_cent", Long.class),
                rs.getString("inventory_item_id"),
                rs.getString("stock_location_id"),
                rs.getObject("stock_quantity", Integer.class),
                rs.getObject("internal_price_cent", Long.class),
                rs.getObject("valid_days", Integer.class),
                nullableInstant(rs, "valid_until"),
                rs.getString("status"),
                rs.getBoolean("transferable"),
                nullableInstant(rs, "created_at"),
                nullableInstant(rs, "updated_at"),
                rs.getString("merchant_type"));
    }

    private VoucherInstanceRecord mapInstance(ResultSet rs, int rowNum) throws SQLException {
        return new VoucherInstanceRecord(
                rs.getString("id"),
                rs.getString("definition_id"),
                rs.getString("customer_id"),
                rs.getString("code"),
                rs.getString("source_type"),
                rs.getString("source_ref"),
                rs.getString("status"),
                nullableInstant(rs, "issued_at"),
                nullableInstant(rs, "expires_at"),
                nullableInstant(rs, "redeemed_at"),
                nullableInstant(rs, "revoked_at"),
                rs.getString("revoke_reason"),
                rs.getBoolean("stock_reserved"),
                nullableInstant(rs, "created_at"),
                nullableInstant(rs, "updated_at"));
    }

    private VoucherRedemptionEntryRecord mapRedemption(ResultSet rs, int rowNum) throws SQLException {
        return new VoucherRedemptionEntryRecord(
                rs.getString("id"),
                rs.getString("voucher_instance_id"),
                rs.getString("definition_id"),
                rs.getString("voucher_kind"),
                rs.getString("customer_id"),
                rs.getString("merchant_id"),
                rs.getString("operator_user_id"),
                rs.getString("redeem_method"),
                rs.getString("settlement_mode"),
                rs.getObject("settlement_amount_cent", Long.class),
                rs.getString("ar_ap_item_id"),
                rs.getString("stock_movement_ref"),
                rs.getString("external_redemption_ref"),
                rs.getString("note"),
                nullableInstant(rs, "redeemed_at"),
                nullableInstant(rs, "created_at"),
                rs.getString("bill_type"),
                rs.getString("bill_id"),
                rs.getObject("deduction_amount_cent", Long.class));
    }
}
