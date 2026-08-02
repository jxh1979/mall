package com.complex.platform.bar;

import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.complex.platform.common.api.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** V256/V257 图形化点单 POS 目录仓储：分类、套餐、按分类列 SKU。均按 merchant scope 隔离。 */
@Repository
class JdbcBarPosCatalogRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcBarRowMapper productRowMapper;

    JdbcBarPosCatalogRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        // JdbcBarRowMapper 非 Spring bean（同 JdbcBarRepository 手工 new 范式）：复用其 mapProduct 保持商品映射单一事实源。
        this.productRowMapper = new JdbcBarRowMapper(objectMapper.copy().findAndRegisterModules());
    }

    // ---------- 分类 ----------

    List<BarProductCategoryRecord> listCategories(Optional<String> merchantScope, String barId, boolean includeArchived) {
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM bar_product_categories WHERE delete_status <> 'approved'");
        MapSqlParameterSource p = new MapSqlParameterSource();
        if (!includeArchived) {
            sql.append(" AND archived_at IS NULL");
        }
        if (barId != null && !barId.isBlank()) {
            sql.append(" AND bar_id = :bar_id");
            p.addValue("bar_id", barId);
        }
        if (merchantScope.isPresent()) {
            sql.append(" AND merchant_id = :merchant_id");
            p.addValue("merchant_id", merchantScope.get());
        }
        sql.append(" ORDER BY sort_order, created_at, id");
        return jdbcTemplate.query(sql.toString(), p, this::mapCategory);
    }

    Optional<BarProductCategoryRecord> findCategory(String id) {
        return jdbcTemplate.query(
                "SELECT * FROM bar_product_categories WHERE id = :id AND delete_status <> 'approved'",
                Map.of("id", id), this::mapCategory).stream().findFirst();
    }

    BarProductCategoryRecord saveCategory(BarProductCategoryRecord c) {
        jdbcTemplate.update("""
                INSERT INTO bar_product_categories (
                    id, bar_id, merchant_id, category_name, sort_order, status,
                    archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason,
                    created_at, updated_at
                ) VALUES (
                    :id, :bar_id, :merchant_id, :category_name, :sort_order, :status,
                    :archived_at, :archived_by, :delete_status, :delete_requested_by, :delete_requested_at, :delete_reason,
                    :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    category_name = EXCLUDED.category_name,
                    sort_order = EXCLUDED.sort_order,
                    status = EXCLUDED.status,
                    archived_at = EXCLUDED.archived_at,
                    archived_by = EXCLUDED.archived_by,
                    delete_status = EXCLUDED.delete_status,
                    delete_requested_by = EXCLUDED.delete_requested_by,
                    delete_requested_at = EXCLUDED.delete_requested_at,
                    delete_reason = EXCLUDED.delete_reason,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", c.id())
                .addValue("bar_id", c.barId())
                .addValue("merchant_id", c.merchantId())
                .addValue("category_name", c.categoryName())
                .addValue("sort_order", c.sortOrder())
                .addValue("status", c.status())
                .addValue("archived_at", timestampWithTimeZone(c.archivedAt()))
                .addValue("archived_by", c.archivedBy())
                .addValue("delete_status", c.deleteStatus() == null ? "none" : c.deleteStatus())
                .addValue("delete_requested_by", c.deleteRequestedBy())
                .addValue("delete_requested_at", timestampWithTimeZone(c.deleteRequestedAt()))
                .addValue("delete_reason", c.deleteReason())
                .addValue("created_at", timestampWithTimeZone(c.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(c.updatedAt())));
        return c;
    }

    void deleteCategory(String id) {
        jdbcTemplate.update("UPDATE bar_product_categories SET delete_status = 'approved', updated_at = now() WHERE id = :id", Map.of("id", id));
    }

    /** 分类引用：任何仍挂在该分类下的商品数——含已存档商品。fk_bar_products_category 无 ON DELETE
     *  动作，只要还有商品行（哪怕已存档）指着该分类，物理 DELETE 必然 FK 违约，故一律计为引用，
     *  须先给商品改挂/清空分类再删。 */
    long countCategoryReferences(String id) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM bar_products WHERE category_id = :id",
                Map.of("id", id), Long.class);
        return count == null ? 0L : count;
    }

    // ---------- 商品：设分类/图片、按分类列、存档/删除 ----------

    Optional<BarProductRecord> findProduct(String id) {
        return jdbcTemplate.query(
                "SELECT * FROM bar_products WHERE id = :id AND delete_status <> 'approved'",
                Map.of("id", id), productRowMapper::mapProduct).stream().findFirst();
    }

    void updateProductCategory(String productId, String categoryId, Instant now) {
        jdbcTemplate.update("""
                UPDATE bar_products SET category_id = :category_id, updated_at = :updated_at WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("category_id", categoryId)
                .addValue("updated_at", timestampWithTimeZone(now))
                .addValue("id", productId));
    }

    void updateProductImage(String productId, String imageUrl, Instant now) {
        jdbcTemplate.update("""
                UPDATE bar_products SET image_url = :image_url, updated_at = :updated_at WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("image_url", imageUrl)
                .addValue("updated_at", timestampWithTimeZone(now))
                .addValue("id", productId));
    }

    /** 存档/删除范式：整体覆盖 6 列（targeted UPDATE，不触碰商品其余字段，语义清晰）。 */
    void updateProductArchiveDelete(BarProductRecord product) {
        jdbcTemplate.update("""
                UPDATE bar_products SET
                    archived_at = :archived_at,
                    archived_by = :archived_by,
                    delete_status = :delete_status,
                    delete_requested_by = :delete_requested_by,
                    delete_requested_at = :delete_requested_at,
                    delete_reason = :delete_reason,
                    updated_at = :updated_at
                 WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("archived_at", timestampWithTimeZone(product.archivedAt()))
                .addValue("archived_by", product.archivedBy())
                .addValue("delete_status", product.deleteStatus() == null ? "none" : product.deleteStatus())
                .addValue("delete_requested_by", product.deleteRequestedBy())
                .addValue("delete_requested_at", timestampWithTimeZone(product.deleteRequestedAt()))
                .addValue("delete_reason", product.deleteReason())
                .addValue("updated_at", timestampWithTimeZone(product.updatedAt()))
                .addValue("id", product.id()));
    }

    void deleteProduct(String id) {
        jdbcTemplate.update("UPDATE bar_products SET delete_status = 'approved', updated_at = now() WHERE id = :id", Map.of("id", id));
    }

    /** 商品活引用：被订单明细(bar_order_items)或套餐明细(bar_combo_items)引用的行数之和。
     *  为何不照票种版(V260)走 information_schema 元数据驱动：ticket_type_id 是域限定列名，而 product_id
     *  是通用列名——2026-07-06 实查 public schema 带 product_id 列的基表仅 bar_order_items/bar_combo_items
     *  两张（均酒吧域），但未来别域（零售/库存等）完全可能新增同名列，元数据驱动会把别域引用误算进来。
     *  故取显式清单；新增酒吧侧商品引用表时须同步补此清单。 */
    long countProductReferences(String id) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT (SELECT COUNT(1) FROM bar_order_items WHERE product_id = :id)
                     + (SELECT COUNT(1) FROM bar_combo_items WHERE product_id = :id)
                """, Map.of("id", id), Long.class);
        return count == null ? 0L : count;
    }

    /** 按分类列某吧台 SKU（供 POS 拉菜单）。categoryId 空=列未分类商品。存档/待删商品不上菜单。 */
    List<BarProductRecord> listProductsByCategory(Optional<String> merchantScope, String barId, String categoryId) {
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM bar_products WHERE bar_id = :bar_id"
                + " AND archived_at IS NULL AND delete_status <> 'approved'");
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("bar_id", barId);
        if (categoryId == null || categoryId.isBlank()) {
            sql.append(" AND category_id IS NULL");
        } else {
            sql.append(" AND category_id = :category_id");
            p.addValue("category_id", categoryId);
        }
        if (merchantScope.isPresent()) {
            sql.append(" AND merchant_id = :merchant_id");
            p.addValue("merchant_id", merchantScope.get());
        }
        sql.append(" ORDER BY sold_out, product_name, id");
        return jdbcTemplate.query(sql.toString(), p, productRowMapper::mapProduct);
    }

    // ---------- 套餐 ----------

    List<BarComboRecord> listCombos(Optional<String> merchantScope, String barId, boolean includeArchived) {
        StringBuilder sql = new StringBuilder("SELECT * FROM bar_combos WHERE delete_status <> 'approved'");
        MapSqlParameterSource p = new MapSqlParameterSource();
        if (!includeArchived) {
            sql.append(" AND archived_at IS NULL");
        }
        if (barId != null && !barId.isBlank()) {
            sql.append(" AND bar_id = :bar_id");
            p.addValue("bar_id", barId);
        }
        if (merchantScope.isPresent()) {
            sql.append(" AND merchant_id = :merchant_id");
            p.addValue("merchant_id", merchantScope.get());
        }
        sql.append(" ORDER BY created_at DESC, id");
        List<BarComboRecord> combos = jdbcTemplate.query(sql.toString(), p, this::mapCombo);
        return combos.stream().map(this::withItems).toList();
    }

    Optional<BarComboRecord> findCombo(String id) {
        return jdbcTemplate.query(
                "SELECT * FROM bar_combos WHERE id = :id AND delete_status <> 'approved'",
                Map.of("id", id), this::mapCombo).stream().findFirst().map(this::withItems);
    }

    // ---------- 批10/E 内部结算 SKU 目录(V379/V380) ----------

    /** 目录条目(生效或草案)的绑定商户:业务对象设引用时校验「条目商家 = 提供方」。空 = 无此条目。 */
    Optional<String> findCatalogItemMerchant(String itemCode) {
        return Optional.ofNullable(jdbcTemplate.query("""
                SELECT merchant_id FROM internal_price_catalog
                 WHERE item_code = :item_code AND status = 'active'
                 ORDER BY is_draft LIMIT 1
                """, Map.of("item_code", itemCode), rs -> rs.next() ? rs.getString(1) : null));
    }

    /** 目录生效价(is_draft=FALSE):设引用当刻的快照来源。空 = 条目还只有草案(未复核转正)/已归档。 */
    Optional<Long> findActiveCatalogPriceCent(String itemCode) {
        return Optional.ofNullable(jdbcTemplate.query("""
                SELECT price_cent FROM internal_price_catalog
                 WHERE item_code = :item_code AND is_draft = FALSE AND status = 'active'
                """, Map.of("item_code", itemCode), rs -> rs.next() ? rs.getLong(1) : null));
    }

    /**
     * 商品挂目录引用 + 同步快照(定向 UPDATE,照 updateProductImage 范式,不走 saveProduct 全字段 upsert ——
     * 快照列只由系统写:设引用当刻取生效价,此后由目录转正传播维护;任何人不得手改)。
     */
    void setProductCatalogRef(String productId, String itemCode, Long snapshotPriceCent) {
        jdbcTemplate.update("""
                UPDATE bar_products
                   SET catalog_item_code = :item_code, internal_settlement_price_cent = :price, updated_at = now()
                 WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", productId)
                .addValue("item_code", itemCode).addValue("price", snapshotPriceCent));
    }

    /** 套餐挂目录引用 + 同步快照(同上;saveCombo 全字段 upsert 不含 catalog_item_code,防重建时误清引用)。 */
    void setComboCatalogRef(String comboId, String itemCode, Long snapshotPriceCent) {
        jdbcTemplate.update("""
                UPDATE bar_combos
                   SET catalog_item_code = :item_code, internal_settlement_price_cent = :price, updated_at = now()
                 WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", comboId)
                .addValue("item_code", itemCode).addValue("price", snapshotPriceCent));
    }

    BarComboRecord saveCombo(BarComboRecord combo) {
        jdbcTemplate.update("""
                INSERT INTO bar_combos (
                    id, bar_id, merchant_id, combo_name, price_cent, internal_settlement_price_cent,
                    image_url, status,
                    archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason,
                    created_at, updated_at
                ) VALUES (
                    :id, :bar_id, :merchant_id, :combo_name, :price_cent, :internal_settlement_price_cent,
                    :image_url, :status,
                    :archived_at, :archived_by, :delete_status, :delete_requested_by, :delete_requested_at, :delete_reason,
                    :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    combo_name = EXCLUDED.combo_name,
                    price_cent = EXCLUDED.price_cent,
                    internal_settlement_price_cent = EXCLUDED.internal_settlement_price_cent,
                    image_url = EXCLUDED.image_url,
                    status = EXCLUDED.status,
                    archived_at = EXCLUDED.archived_at,
                    archived_by = EXCLUDED.archived_by,
                    delete_status = EXCLUDED.delete_status,
                    delete_requested_by = EXCLUDED.delete_requested_by,
                    delete_requested_at = EXCLUDED.delete_requested_at,
                    delete_reason = EXCLUDED.delete_reason,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", combo.id())
                .addValue("bar_id", combo.barId())
                .addValue("merchant_id", combo.merchantId())
                .addValue("combo_name", combo.comboName())
                .addValue("price_cent", combo.priceCent())
                .addValue("internal_settlement_price_cent", combo.internalSettlementPriceCent())
                .addValue("image_url", combo.imageUrl())
                .addValue("status", combo.status())
                .addValue("archived_at", timestampWithTimeZone(combo.archivedAt()))
                .addValue("archived_by", combo.archivedBy())
                .addValue("delete_status", combo.deleteStatus() == null ? "none" : combo.deleteStatus())
                .addValue("delete_requested_by", combo.deleteRequestedBy())
                .addValue("delete_requested_at", timestampWithTimeZone(combo.deleteRequestedAt()))
                .addValue("delete_reason", combo.deleteReason())
                .addValue("created_at", timestampWithTimeZone(combo.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(combo.updatedAt())));
        // 明细整体替换：先删后插（套餐编辑=重设组合，语义清晰）。
        jdbcTemplate.update("DELETE FROM bar_combo_items WHERE combo_id = :combo_id",
                Map.of("combo_id", combo.id()));
        for (BarComboItemRecord item : combo.items()) {
            jdbcTemplate.update("""
                    INSERT INTO bar_combo_items (id, combo_id, product_id, quantity, created_at)
                    VALUES (:id, :combo_id, :product_id, :quantity, :created_at)
                    """, new MapSqlParameterSource()
                    .addValue("id", item.id())
                    .addValue("combo_id", combo.id())
                    .addValue("product_id", item.productId())
                    .addValue("quantity", item.quantity())
                    .addValue("created_at", timestampWithTimeZone(combo.updatedAt())));
        }
        return findCombo(combo.id()).orElseThrow(
                () -> ApiException.conflict("BAR_COMBO_SAVE_FAILED", "combo not readable after save"));
    }

    void deleteCombo(String id) {
        // 软删：保留套餐主行（delete_status=approved 被读取过滤隐藏）。
        // 改 UPDATE 后 ON DELETE CASCADE（V257）不再触发，bar_combo_items 明细自动保留（可恢复）。
        jdbcTemplate.update("UPDATE bar_combos SET delete_status = 'approved', updated_at = now() WHERE id = :id", Map.of("id", id));
    }

    private BarComboRecord withItems(BarComboRecord combo) {
        List<BarComboItemRecord> items = jdbcTemplate.query("""
                SELECT ci.id, ci.combo_id, ci.product_id, ci.quantity, p.product_name
                  FROM bar_combo_items ci
                  JOIN bar_products p ON p.id = ci.product_id
                 WHERE ci.combo_id = :combo_id
                 ORDER BY ci.created_at, ci.id
                """, Map.of("combo_id", combo.id()), (rs, i) -> new BarComboItemRecord(
                rs.getString("id"), rs.getString("combo_id"), rs.getString("product_id"),
                rs.getString("product_name"), rs.getInt("quantity")));
        return new BarComboRecord(combo.id(), combo.barId(), combo.merchantId(), combo.comboName(),
                combo.priceCent(), combo.internalSettlementPriceCent(), combo.imageUrl(), combo.status(),
                combo.createdAt(), combo.updatedAt(), items,
                combo.archivedAt(), combo.archivedBy(), combo.deleteStatus(),
                combo.deleteRequestedBy(), combo.deleteRequestedAt(), combo.deleteReason());
    }

    private BarProductCategoryRecord mapCategory(ResultSet rs, int rowNum) throws SQLException {
        return new BarProductCategoryRecord(
                rs.getString("id"), rs.getString("bar_id"), rs.getString("merchant_id"),
                rs.getString("category_name"), rs.getInt("sort_order"), rs.getString("status"),
                instantOf(rs, "created_at"), instantOf(rs, "updated_at"),
                instantOf(rs, "archived_at"), rs.getString("archived_by"), rs.getString("delete_status"),
                rs.getString("delete_requested_by"), instantOf(rs, "delete_requested_at"), rs.getString("delete_reason"));
    }

    private BarComboRecord mapCombo(ResultSet rs, int rowNum) throws SQLException {
        return new BarComboRecord(
                rs.getString("id"), rs.getString("bar_id"), rs.getString("merchant_id"),
                rs.getString("combo_name"), rs.getLong("price_cent"),
                rs.getObject("internal_settlement_price_cent", Long.class), rs.getString("image_url"),
                rs.getString("status"), instantOf(rs, "created_at"), instantOf(rs, "updated_at"),
                List.of(),
                instantOf(rs, "archived_at"), rs.getString("archived_by"), rs.getString("delete_status"),
                rs.getString("delete_requested_by"), instantOf(rs, "delete_requested_at"), rs.getString("delete_reason"));
    }

    private static Instant instantOf(ResultSet rs, String col) throws SQLException {
        OffsetDateTime v = rs.getObject(col, OffsetDateTime.class);
        return v == null ? null : v.toInstant();
    }

    public void setCategoryArchiveReason(String id, String reason) {
        jdbcTemplate.update("UPDATE bar_product_categories SET archive_reason = :reason WHERE id = :id",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("reason", reason).addValue("id", id));
    }

    public void setProductArchiveReason(String id, String reason) {
        jdbcTemplate.update("UPDATE bar_products SET archive_reason = :reason WHERE id = :id",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("reason", reason).addValue("id", id));
    }

    public void setComboArchiveReason(String id, String reason) {
        jdbcTemplate.update("UPDATE bar_combos SET archive_reason = :reason WHERE id = :id",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("reason", reason).addValue("id", id));
    }
}
