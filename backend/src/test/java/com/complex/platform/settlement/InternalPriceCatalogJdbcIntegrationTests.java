package com.complex.platform.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.settlement.SettlementConfigWriteRequests.ArchiveRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.InternalPriceItemRequest;
import com.complex.platform.ticketing.InternalSettlementPriceResolver;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 批10/E 内部结算 SKU 目录（V379/V380）验收：
 *
 * <ol>
 *   <li><b>收编等价</b>（V380 判据：迁移后各业务对象解析出的内部价与迁移前逐分相同）——
 *       按旧形态播四处存量价（价格列有值、无引用），重放 V380 迁移 SQL（其守卫 WHERE catalog_item_code IS NULL
 *       保证可重放），断言：四处都被收进目录、条目价 = 原列值逐分不差、解析器按目录解析回同一个数。</li>
 *   <li><b>目录草案链治理</b>——就地新建落草案不生效、他人复核转正、绑定商户不可改、0 价转正有券引用先验拒、
 *       下线归档后解析 fail-closed 且历史行留库。</li>
 * </ol>
 *
 * <p>⚠️ 走真实 Flyway 迁移库；按 suffix 播种/清理，勿与其它 Flyway 集成测试并发同 scope 数据。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class InternalPriceCatalogJdbcIntegrationTests {

    private static final String DRAFTER = "drafter_ipc_it";
    private static final String APPROVER = "approver_ipc_it";

    @Autowired
    private SettlementConfigWriteService writeService;

    @Autowired
    private InternalSettlementPriceResolver priceResolver;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String suffix;

    @AfterEach
    void cleanup() {
        if (suffix == null) {
            return;
        }
        jdbcTemplate.update("DELETE FROM voucher_definitions WHERE id = ?", "vd_ipc_" + suffix);
        jdbcTemplate.update("DELETE FROM entitlement_templates WHERE id = ?", "et_ipc_" + suffix);
        jdbcTemplate.update("DELETE FROM bar_combos WHERE id = ?", "bc_ipc_" + suffix);
        jdbcTemplate.update("DELETE FROM bar_products WHERE id = ?", "bp_ipc_" + suffix);
        jdbcTemplate.update("DELETE FROM internal_price_catalog WHERE merchant_id = ?", "merch_ipc_" + suffix);
        jdbcTemplate.update("DELETE FROM merchants WHERE id = ?", "merch_ipc_" + suffix);
        jdbcTemplate.update("DELETE FROM merchants WHERE id = ?", "merch_other_ipc_" + suffix);
    }

    /** 收编等价：四处旧价重放 V380 → 目录条目逐分同值 + 引用建立 + 解析器按目录解析回同一个数。 */
    @Test
    void v380BackfillCollectsFourLegacyStoresEquivalently() throws Exception {
        suffix = Long.toString(System.nanoTime());
        String merchant = seedMerchant();
        // 四处旧形态存量价（有价、无引用）——四个不同的数，防"碰巧相等"假绿。
        String productId = "bp_ipc_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO bar_products (id, bar_id, product_name, product_type, price_cent, currency,
                    stock_deduction_required, status, internal_settlement_price_cent, created_at, updated_at)
                VALUES (?, ?, 'IPC威士忌', 'spirit', 12000, 'CNY', FALSE, 'active', 5100, now(), now())
                """, productId, merchant);
        String comboId = "bc_ipc_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO bar_combos (id, bar_id, merchant_id, combo_name, price_cent,
                    internal_settlement_price_cent, status, created_at, updated_at)
                VALUES (?, ?, ?, 'IPC双人套餐', 28800, 5200, 'active', now(), now())
                """, comboId, merchant, merchant);
        String templateId = "et_ipc_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times,
                    default_value_cent, status, merchant_id, entitlement_kind, agreement_price_cent,
                    created_at, updated_at)
                VALUES (?, 'IPC餐饮优惠', 'dining', 1, 0, 'active', ?, 'virtual', 5300, now(), now())
                """, templateId, merchant);
        String voucherId = "vd_ipc_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO voucher_definitions (id, name, voucher_kind, redeem_method, settlement_mode,
                    merchant_id, inventory_item_id, stock_location_id, stock_quantity, internal_price_cent,
                    status, created_at, updated_at)
                VALUES (?, 'IPC实物券', 'in_store_goods', 'in_store_staff', 'internal_price',
                    ?, 'inv_ipc', 'loc_ipc', 1, 5400, 'active', now(), now())
                """, voucherId, merchant);

        replayV380();

        // 收编等价判据：目录条目价 = 原列值，逐分不差；引用建立；快照列保持原值。
        assertThat(catalogPrice("sku_bp_" + productId)).isEqualTo(5100L);
        assertThat(catalogPrice("sku_bc_" + comboId)).isEqualTo(5200L);
        assertThat(catalogPrice("sku_et_" + templateId)).isEqualTo(5300L);
        assertThat(catalogPrice("sku_vd_" + voucherId)).isEqualTo(5400L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT catalog_item_code FROM bar_products WHERE id = ?", String.class, productId))
                .isEqualTo("sku_bp_" + productId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT internal_settlement_price_cent FROM bar_products WHERE id = ?", Long.class, productId))
                .isEqualTo(5100L);
        // 解析器（两支都收口目录）解析回同一个数 = 调用方无感等价。
        assertThat(priceResolver.require(productId, merchant, null)).isEqualTo(5100L);
        assertThat(priceResolver.require(null, merchant, "sku_et_" + templateId)).isEqualTo(5300L);
        // 目录条目绑定商户 = 提供方（跨商户引用会在解析层断路，这里断言绑定本身正确）。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT merchant_id FROM internal_price_catalog WHERE item_code = ?",
                String.class, "sku_vd_" + voucherId)).isEqualTo(merchant);
    }

    /** 目录草案链治理：新建落草案不生效 → 他人转正生效；绑定商户不可改；0 价有券引用拒转正；归档 fail-closed。 */
    @Test
    void catalogDraftGovernanceGuards() {
        suffix = Long.toString(System.nanoTime());
        String merchant = seedMerchant();

        // ① 就地新建 = 落草案：返回 item_code 可立即被引用，但没有生效价（解析 fail-closed）。
        String itemCode = writeService.saveInternalPriceItem(new InternalPriceItemRequest(
                "idem-ipc-new-" + suffix, null, merchant, "spirit", "IPC新品", 6000L), DRAFTER);
        assertThat(itemCode).startsWith("sku_");
        assertThat(priceResolver.resolve(null, merchant, itemCode)).isEmpty();

        // ② 他人复核转正 → 生效价 6000。
        writeService.activateDraft("internal_price_item", draftId(itemCode), APPROVER);
        assertThat(priceResolver.require(null, merchant, itemCode)).isEqualTo(6000L);

        // ③ 绑定商户不可改（换商户 = 成本价基整体易主，引用它的对象全部跨商户）。
        String otherMerchant = "merch_other_ipc_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at)
                VALUES (?, 'IPC另一商户', 'bar', 'active', now(), now()) ON CONFLICT (id) DO NOTHING
                """, otherMerchant);
        assertThatThrownBy(() -> writeService.saveInternalPriceItem(new InternalPriceItemRequest(
                "idem-ipc-rebind-" + suffix, itemCode, otherMerchant, "spirit", "IPC新品", 6000L), DRAFTER))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CATALOG_ITEM_MERCHANT_IMMUTABLE"));

        // ④ 0 价转正 + 有券引用 → 先验拒绝（券口径要求内部价 >0，不给 DB 约束炸 500 的机会）。
        jdbcTemplate.update("""
                INSERT INTO voucher_definitions (id, name, voucher_kind, redeem_method, settlement_mode,
                    merchant_id, inventory_item_id, stock_location_id, stock_quantity, internal_price_cent,
                    catalog_item_code, status, created_at, updated_at)
                VALUES (?, 'IPC引用券', 'in_store_goods', 'in_store_staff', 'internal_price',
                    ?, 'inv_ipc', 'loc_ipc', 1, 6000, ?, 'active', now(), now())
                """, "vd_ipc_" + suffix, merchant, itemCode);
        writeService.saveInternalPriceItem(new InternalPriceItemRequest(
                "idem-ipc-zero-" + suffix, itemCode, merchant, "spirit", "IPC新品", 0L), DRAFTER);
        String zeroDraftId = draftId(itemCode);
        assertThatThrownBy(() -> writeService.activateDraft("internal_price_item", zeroDraftId, APPROVER))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CATALOG_PRICE_ZERO_VOUCHER_REF"));

        // ⑤ 下线归档：生效行软删留库、解析 fail-closed、历史（inactive）行不动。
        String activeId = jdbcTemplate.queryForObject(
                "SELECT id FROM internal_price_catalog WHERE item_code = ? AND is_draft = FALSE AND status = 'active'",
                String.class, itemCode);
        // 软删同样差人：生效行最后编辑人是转正的 APPROVER，故由 DRAFTER 执行下线。
        writeService.archive("internal_price_item", activeId, new ArchiveRequest(
                "idem-ipc-arch-" + suffix, "该 SKU 已停售，下线归档"), DRAFTER);
        assertThat(priceResolver.resolve(null, merchant, itemCode)).isEmpty();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM internal_price_catalog WHERE item_code = ? AND status = 'inactive'",
                Integer.class, itemCode)).isGreaterThanOrEqualTo(1);
    }

    private String seedMerchant() {
        String merchant = "merch_ipc_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at)
                VALUES (?, ?, 'bar', 'active', now(), now())
                ON CONFLICT (id) DO NOTHING
                """, merchant, "IPC测试商户-" + suffix);
        return merchant;
    }

    private String draftId(String itemCode) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM internal_price_catalog WHERE item_code = ? AND is_draft = TRUE AND status = 'active'",
                String.class, itemCode);
    }

    private Long catalogPrice(String itemCode) {
        List<Long> prices = jdbcTemplate.queryForList(
                "SELECT price_cent FROM internal_price_catalog WHERE item_code = ? AND is_draft = FALSE AND status = 'active'",
                Long.class, itemCode);
        return prices.isEmpty() ? null : prices.get(0);
    }

    /** 重放 V380 收编 SQL（真源文件、非手抄副本——迁移改了测试跟着变，防"测试测的不是上线跑的"）。 */
    private void replayV380() throws Exception {
        try (InputStream in = getClass().getResourceAsStream(
                "/db/migration/V380__internal_price_catalog_backfill.sql")) {
            assertThat(in).as("V380 migration must be on classpath").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            jdbcTemplate.execute(sql);
        }
    }
}
