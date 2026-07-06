package com.complex.platform.bar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * V256/V257 图形化点单 POS 目录仓储集成测试：分类 CRUD、给 SKU 设分类、按分类列 SKU、套餐 CRUD（选 SKU 组合）。
 * 仓储层测试（不经 AuthorizationService），验证建模与查询正确；merchant scope 传 empty（平台视角，列全部）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
@Transactional
class BarPosCatalogJdbcIntegrationTests {

    @Autowired
    private JdbcBarPosCatalogRepository catalog;

    @Autowired
    private JdbcBarRepository barRepository;

    @Autowired
    private BarPosCatalogService service;

    private BarProductRecord newProduct(String id, String barId, String name) {
        Instant now = Instant.now();
        return new BarProductRecord(id, barId, "merchant_pos_test", name, "spirit", 5000L, "CNY",
                null, null, false, "active", now, now, null, null, null);
    }

    @Test
    void categoryCrudAndSortOrder() {
        Instant now = Instant.now();
        catalog.saveCategory(new BarProductCategoryRecord(
                "barcat_pos_a", "bar_pos_test", "merchant_pos_test", "烈酒", 2, "active", now, now));
        catalog.saveCategory(new BarProductCategoryRecord(
                "barcat_pos_b", "bar_pos_test", "merchant_pos_test", "啤酒", 1, "active", now, now));

        List<BarProductCategoryRecord> categories = catalog.listCategories(Optional.empty(), "bar_pos_test");
        assertThat(categories).extracting(BarProductCategoryRecord::id)
                .containsExactly("barcat_pos_b", "barcat_pos_a"); // 按 sort_order

        // 更新：改名 + 停用
        catalog.saveCategory(new BarProductCategoryRecord(
                "barcat_pos_a", "bar_pos_test", "merchant_pos_test", "烈酒-改", 2, "archived", now, now));
        assertThat(catalog.findCategory("barcat_pos_a")).get()
                .extracting(BarProductCategoryRecord::categoryName, BarProductCategoryRecord::status)
                .containsExactly("烈酒-改", "archived");
    }

    @Test
    void assignCategoryAndListProductsByCategory() {
        Instant now = Instant.now();
        catalog.saveCategory(new BarProductCategoryRecord(
                "barcat_pos_c", "bar_pos_test2", "merchant_pos_test", "鸡尾酒", 1, "active", now, now));
        barRepository.saveProduct(newProduct("product_pos_1", "bar_pos_test2", "长岛冰茶"));
        barRepository.saveProduct(newProduct("product_pos_2", "bar_pos_test2", "无类商品"));

        // 未分类时列到 categoryId=null 桶
        assertThat(catalog.listProductsByCategory(Optional.empty(), "bar_pos_test2", null))
                .extracting(BarProductRecord::id).contains("product_pos_1", "product_pos_2");

        catalog.updateProductCategory("product_pos_1", "barcat_pos_c", now);

        assertThat(catalog.listProductsByCategory(Optional.empty(), "bar_pos_test2", "barcat_pos_c"))
                .extracting(BarProductRecord::id).containsExactly("product_pos_1");
        // product_pos_1 已归类，未分类桶只剩 product_pos_2
        assertThat(catalog.listProductsByCategory(Optional.empty(), "bar_pos_test2", null))
                .extracting(BarProductRecord::id).containsExactly("product_pos_2");

        // 设图片
        catalog.updateProductImage("product_pos_1", "bar-images/abc.jpg", now);
        assertThat(catalog.findProduct("product_pos_1")).get()
                .extracting(BarProductRecord::imageUrl, BarProductRecord::categoryId)
                .containsExactly("bar-images/abc.jpg", "barcat_pos_c");
    }

    @Test
    void comboCrudWithItems() {
        Instant now = Instant.now();
        barRepository.saveProduct(newProduct("product_pos_c1", "bar_pos_test3", "威士忌"));
        barRepository.saveProduct(newProduct("product_pos_c2", "bar_pos_test3", "苏打水"));

        BarComboRecord saved = catalog.saveCombo(new BarComboRecord(
                "barcombo_pos_1", "bar_pos_test3", "merchant_pos_test", "威士忌套餐",
                9900L, 6000L, "bar-images/combo.jpg", "active", now, now,
                List.of(
                        new BarComboItemRecord("barcomboitem_1", "barcombo_pos_1", "product_pos_c1", null, 1),
                        new BarComboItemRecord("barcomboitem_2", "barcombo_pos_1", "product_pos_c2", null, 2))));
        assertThat(saved.items()).hasSize(2);
        assertThat(saved.items()).extracting(BarComboItemRecord::productName)
                .containsExactlyInAnyOrder("威士忌", "苏打水"); // JOIN 回填名称

        // 更新：整体替换明细为单项 + 改售价
        catalog.saveCombo(new BarComboRecord(
                "barcombo_pos_1", "bar_pos_test3", "merchant_pos_test", "威士忌套餐",
                8800L, 6000L, "bar-images/combo.jpg", "active", now, now,
                List.of(new BarComboItemRecord("barcomboitem_3", "barcombo_pos_1", "product_pos_c1", null, 1))));
        BarComboRecord updated = catalog.findCombo("barcombo_pos_1").orElseThrow();
        assertThat(updated.priceCent()).isEqualTo(8800L);
        assertThat(updated.items()).extracting(BarComboItemRecord::productId).containsExactly("product_pos_c1");

        assertThat(catalog.listCombos(Optional.empty(), "bar_pos_test3"))
                .extracting(BarComboRecord::id).contains("barcombo_pos_1");
    }
    @Test
    void crossMerchantCategoryEditIsForbidden() {
        // 复核 Important:服务层租户隔离守卫 checkOwnership 需有测试——B 商户不得改 A 商户的分类。
        Instant now = Instant.now();
        catalog.saveCategory(new BarProductCategoryRecord(
                "barcat_iso_a", "bar_iso_a", "merchant_iso_A", "A家分类", 1, "active", now, now));
        com.complex.platform.support.TestSecurityActors.asMerchantUser("merchant_iso_B");
        try {
            assertThatThrownBy(() -> service.saveCategory(new BarProductCategoryRequest(
                    "barcat_iso_a", "bar_iso_a", "B想改名", 1, "active")))
                    .isInstanceOf(com.complex.platform.common.api.ApiException.class);
        } finally {
            com.complex.platform.support.TestSecurityActors.clear();
        }
    }
}
