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

    List<BarProductCategoryRecord> listCategories(Optional<String> merchantScope, String barId) {
        StringBuilder sql = new StringBuilder("SELECT * FROM bar_product_categories WHERE 1=1");
        MapSqlParameterSource p = new MapSqlParameterSource();
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
        return jdbcTemplate.query("SELECT * FROM bar_product_categories WHERE id = :id",
                Map.of("id", id), this::mapCategory).stream().findFirst();
    }

    BarProductCategoryRecord saveCategory(BarProductCategoryRecord c) {
        jdbcTemplate.update("""
                INSERT INTO bar_product_categories (
                    id, bar_id, merchant_id, category_name, sort_order, status, created_at, updated_at
                ) VALUES (
                    :id, :bar_id, :merchant_id, :category_name, :sort_order, :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    category_name = EXCLUDED.category_name,
                    sort_order = EXCLUDED.sort_order,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", c.id())
                .addValue("bar_id", c.barId())
                .addValue("merchant_id", c.merchantId())
                .addValue("category_name", c.categoryName())
                .addValue("sort_order", c.sortOrder())
                .addValue("status", c.status())
                .addValue("created_at", timestampWithTimeZone(c.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(c.updatedAt())));
        return c;
    }

    // ---------- 商品：设分类/图片、按分类列 ----------

    Optional<BarProductRecord> findProduct(String id) {
        return jdbcTemplate.query("SELECT * FROM bar_products WHERE id = :id",
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

    /** 按分类列某吧台 SKU（供 POS 拉菜单）。categoryId 空=列未分类商品。 */
    List<BarProductRecord> listProductsByCategory(Optional<String> merchantScope, String barId, String categoryId) {
        StringBuilder sql = new StringBuilder("SELECT * FROM bar_products WHERE bar_id = :bar_id");
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

    List<BarComboRecord> listCombos(Optional<String> merchantScope, String barId) {
        StringBuilder sql = new StringBuilder("SELECT * FROM bar_combos WHERE 1=1");
        MapSqlParameterSource p = new MapSqlParameterSource();
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
        return jdbcTemplate.query("SELECT * FROM bar_combos WHERE id = :id",
                Map.of("id", id), this::mapCombo).stream().findFirst().map(this::withItems);
    }

    BarComboRecord saveCombo(BarComboRecord combo) {
        jdbcTemplate.update("""
                INSERT INTO bar_combos (
                    id, bar_id, merchant_id, combo_name, price_cent, internal_settlement_price_cent,
                    image_url, status, created_at, updated_at
                ) VALUES (
                    :id, :bar_id, :merchant_id, :combo_name, :price_cent, :internal_settlement_price_cent,
                    :image_url, :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    combo_name = EXCLUDED.combo_name,
                    price_cent = EXCLUDED.price_cent,
                    internal_settlement_price_cent = EXCLUDED.internal_settlement_price_cent,
                    image_url = EXCLUDED.image_url,
                    status = EXCLUDED.status,
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
                combo.createdAt(), combo.updatedAt(), items);
    }

    private BarProductCategoryRecord mapCategory(ResultSet rs, int rowNum) throws SQLException {
        return new BarProductCategoryRecord(
                rs.getString("id"), rs.getString("bar_id"), rs.getString("merchant_id"),
                rs.getString("category_name"), rs.getInt("sort_order"), rs.getString("status"),
                instantOf(rs, "created_at"), instantOf(rs, "updated_at"));
    }

    private BarComboRecord mapCombo(ResultSet rs, int rowNum) throws SQLException {
        return new BarComboRecord(
                rs.getString("id"), rs.getString("bar_id"), rs.getString("merchant_id"),
                rs.getString("combo_name"), rs.getLong("price_cent"),
                rs.getObject("internal_settlement_price_cent", Long.class), rs.getString("image_url"),
                rs.getString("status"), instantOf(rs, "created_at"), instantOf(rs, "updated_at"),
                List.of());
    }

    private static Instant instantOf(ResultSet rs, String col) throws SQLException {
        OffsetDateTime v = rs.getObject(col, OffsetDateTime.class);
        return v == null ? null : v.toInstant();
    }
}
