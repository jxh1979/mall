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

    @Autowired
    private BarOrderService barOrderService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

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

        List<BarProductCategoryRecord> categories = catalog.listCategories(Optional.empty(), "bar_pos_test", false);
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

        assertThat(catalog.listCombos(Optional.empty(), "bar_pos_test3", false))
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

    @Test
    void comboOrderExpandsToSkuItemsAndSplitsPriceExactly() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        Instant now = Instant.now();
        String bar = "bar_pos_combo";
        BarProductRecord skuA = new BarProductRecord("product_combo_a", bar, null, "金酒", "spirit",
                3000L, "CNY", null, null, false, "active", now, now, null, null, null);
        BarProductRecord skuB = new BarProductRecord("product_combo_b", bar, null, "汤力水", "soft",
                7000L, "CNY", null, null, false, "active", now, now, null, null, null);
        barRepository.saveProduct(skuA);
        barRepository.saveProduct(skuB);
        catalog.saveCombo(new BarComboRecord("barcombo_gt", bar, null, "金汤力套餐",
                10000L, 6000L, null, "active", now, now,
                List.of(new BarComboItemRecord("bci_a", "barcombo_gt", "product_combo_a", "金酒", 1),
                        new BarComboItemRecord("bci_b", "barcombo_gt", "product_combo_b", "汤力水", 1))));
        barRepository.saveTable(new BarTableRecord("bartable_combo", bar, null, "T-COMBO", "组合桌",
                "card_seat", 4, 0, 0L, "CNY", "active", now, now));

        BarOrderRecord order = barOrderService.createOrder(new BarOrderRequest(
                "combo-order-1", null, List.of(), bar, "bartable_combo", null,
                List.of(java.util.Map.of("combo_id", "barcombo_gt", "quantity", "2")),
                null, List.of(), null));

        assertThat(order.totalAmountCent()).isEqualTo(20000L);
        List<BarOrderItemRecord> items = barRepository.listOrderItems(order.id());
        assertThat(items).extracting(BarOrderItemRecord::productId)
                .containsExactlyInAnyOrder("product_combo_a", "product_combo_b");
        assertThat(items).allSatisfy(it -> assertThat(it.quantity()).isEqualTo(2));
        long sum = items.stream().mapToLong(BarOrderItemRecord::amountCent).sum();
        assertThat(sum).isEqualTo(20000L);
        assertThat(items).filteredOn(it -> it.productId().equals("product_combo_a"))
                .first().extracting(BarOrderItemRecord::amountCent).isEqualTo(6000L);
        assertThat(items).filteredOn(it -> it.productId().equals("product_combo_b"))
                .first().extracting(BarOrderItemRecord::amountCent).isEqualTo(14000L);
    }
    @Test
    void comboOrderSplitsIndivisiblePriceByLargestRemainder() {
        // 锁死最大余数法 leftover>0 分支:两等权 SKU、套餐价 10001 分不整除,
        // 那一分落在第一明细 → 5001/5000,拆分合计严格 = 10001,不多不少。
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        Instant now = Instant.now();
        String bar = "bar_pos_combo_rem";
        BarProductRecord skuA = new BarProductRecord("product_rem_a", bar, null, "SKU甲", "spirit",
                5000L, "CNY", null, null, false, "active", now, now, null, null, null);
        BarProductRecord skuB = new BarProductRecord("product_rem_b", bar, null, "SKU乙", "soft",
                5000L, "CNY", null, null, false, "active", now, now, null, null, null);
        barRepository.saveProduct(skuA);
        barRepository.saveProduct(skuB);
        catalog.saveCombo(new BarComboRecord("barcombo_rem", bar, null, "等权不整除套餐",
                10001L, null, null, "active", now, now,
                List.of(new BarComboItemRecord("bci_rem_a", "barcombo_rem", "product_rem_a", "SKU甲", 1),
                        new BarComboItemRecord("bci_rem_b", "barcombo_rem", "product_rem_b", "SKU乙", 1))));
        barRepository.saveTable(new BarTableRecord("bartable_rem", bar, null, "T-REM", "余数桌",
                "card_seat", 4, 0, 0L, "CNY", "active", now, now));

        BarOrderRecord order = barOrderService.createOrder(new BarOrderRequest(
                "combo-order-rem", null, List.of(), bar, "bartable_rem", null,
                List.of(java.util.Map.of("combo_id", "barcombo_rem", "quantity", "1")),
                null, List.of(), null));

        assertThat(order.totalAmountCent()).isEqualTo(10001L);
        List<BarOrderItemRecord> items = barRepository.listOrderItems(order.id());
        long sum = items.stream().mapToLong(BarOrderItemRecord::amountCent).sum();
        assertThat(sum).isEqualTo(10001L); // 守恒:每一分都落到某一明细
        assertThat(items).filteredOn(it -> it.productId().equals("product_rem_a"))
                .first().extracting(BarOrderItemRecord::amountCent).isEqualTo(5001L); // 余数落第一行
        assertThat(items).filteredOn(it -> it.productId().equals("product_rem_b"))
                .first().extracting(BarOrderItemRecord::amountCent).isEqualTo(5000L);
    }

    // ================= V262 存档 / 审批后删除范式（经服务层状态机） =================

    // ---- 分类 ----
    @Test
    void archiveCategorySucceeds() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        try {
            Instant now = Instant.now();
            catalog.saveCategory(new BarProductCategoryRecord(
                    "barcat_arc", "bar_arc", "merchant_pos_test", "待存档分类", 1, "active", now, now));
            BarProductCategoryRecord archived = service.archiveCategory("barcat_arc", true, "测试存档理由");
            assertThat(archived.archivedAt()).isNotNull();
            // 默认列表(不含存档)不返回；含存档时可见。
            assertThat(catalog.listCategories(Optional.empty(), "bar_arc", false)).isEmpty();
            assertThat(catalog.listCategories(Optional.empty(), "bar_arc", true))
                    .extracting(BarProductCategoryRecord::id).containsExactly("barcat_arc");
        } finally {
            com.complex.platform.support.TestSecurityActors.clear();
        }
    }

    @Test
    void approveDeleteCategorySucceedsWhenNoReferences() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        try {
            Instant now = Instant.now();
            catalog.saveCategory(new BarProductCategoryRecord(
                    "barcat_del", "bar_del", "merchant_pos_test", "待删分类", 1, "active", now, now));
            service.archiveCategory("barcat_del", true, "测试存档理由");
            service.requestDeleteCategory("barcat_del", new BarCatalogDeleteRequest("配错"));
            // 禁自审：换一个审批人（申请人不能审批自己的申请）。
            com.complex.platform.support.TestSecurityActors.asPlatformAdmin("usr_approver");
            BarCatalogDeleteDecision decision = service.approveDeleteCategory(
                    "barcat_del", new BarCatalogDeleteApprovalRequest("approved", null));
            assertThat(decision.deleted()).isTrue();
            assertThat(catalog.findCategory("barcat_del")).isEmpty();
        } finally {
            com.complex.platform.support.TestSecurityActors.clear();
        }
    }

    @Test
    void approveDeleteCategoryRejectedWhenInUse() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        try {
            Instant now = Instant.now();
            catalog.saveCategory(new BarProductCategoryRecord(
                    "barcat_use", "bar_use", "merchant_pos_test", "在用分类", 1, "active", now, now));
            barRepository.saveProduct(newProduct("product_cat_use", "bar_use", "挂此分类商品"));
            catalog.updateProductCategory("product_cat_use", "barcat_use", now);
            service.archiveCategory("barcat_use", true, "测试存档理由");
            service.requestDeleteCategory("barcat_use", new BarCatalogDeleteRequest("试删"));
            // 禁自审：换一个审批人（申请人不能审批自己的申请）。
            com.complex.platform.support.TestSecurityActors.asPlatformAdmin("usr_approver");
            assertThatThrownBy(() -> service.approveDeleteCategory(
                    "barcat_use", new BarCatalogDeleteApprovalRequest("approved", null)))
                    .isInstanceOf(com.complex.platform.common.api.ApiException.class);
            assertThat(catalog.findCategory("barcat_use")).isPresent();
        } finally {
            com.complex.platform.support.TestSecurityActors.clear();
        }
    }

    // ---- 商品 ----
    @Test
    void archiveProductSucceeds() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        try {
            barRepository.saveProduct(newProduct("product_arc", "bar_parc", "待存档商品"));
            BarProductRecord archived = service.archiveProduct("product_arc", true, "测试存档理由");
            assertThat(archived.archivedAt()).isNotNull();
            // 已存档商品不上 POS 菜单。
            assertThat(catalog.listProductsByCategory(Optional.empty(), "bar_parc", null))
                    .extracting(BarProductRecord::id).doesNotContain("product_arc");
        } finally {
            com.complex.platform.support.TestSecurityActors.clear();
        }
    }

    @Test
    void approveDeleteProductSucceedsWhenNoReferences() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        try {
            barRepository.saveProduct(newProduct("product_del", "bar_pdel", "待删商品"));
            service.archiveProduct("product_del", true, "测试存档理由");
            service.requestDeleteProduct("product_del", new BarCatalogDeleteRequest("下架"));
            // 禁自审：换一个审批人（申请人不能审批自己的申请）。
            com.complex.platform.support.TestSecurityActors.asPlatformAdmin("usr_approver");
            BarCatalogDeleteDecision decision = service.approveDeleteProduct(
                    "product_del", new BarCatalogDeleteApprovalRequest("approved", null));
            assertThat(decision.deleted()).isTrue();
            assertThat(catalog.findProduct("product_del")).isEmpty();
        } finally {
            com.complex.platform.support.TestSecurityActors.clear();
        }
    }

    @Test
    void approveDeleteProductRejectedWhenInUseByCombo() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        try {
            Instant now = Instant.now();
            barRepository.saveProduct(newProduct("product_puse", "bar_puse", "被套餐引用商品"));
            // 套餐明细引用该商品 → 活引用 >0。（先建套餐再存档商品，绕开 saveCombo 拒已存档商品的守卫。）
            catalog.saveCombo(new BarComboRecord("barcombo_puse", "bar_puse", "merchant_pos_test", "含此商品套餐",
                    5000L, null, null, "active", now, now,
                    List.of(new BarComboItemRecord("bci_puse", "barcombo_puse", "product_puse", "被套餐引用商品", 1))));
            service.archiveProduct("product_puse", true, "测试存档理由");
            service.requestDeleteProduct("product_puse", new BarCatalogDeleteRequest("试删"));
            // 禁自审：换一个审批人（申请人不能审批自己的申请）。
            com.complex.platform.support.TestSecurityActors.asPlatformAdmin("usr_approver");
            assertThatThrownBy(() -> service.approveDeleteProduct(
                    "product_puse", new BarCatalogDeleteApprovalRequest("approved", null)))
                    .isInstanceOf(com.complex.platform.common.api.ApiException.class);
            assertThat(catalog.findProduct("product_puse")).isPresent();
        } finally {
            com.complex.platform.support.TestSecurityActors.clear();
        }
    }

    // ---- 套餐（无外部活引用，可直接删；明细随 CASCADE 清除）----
    @Test
    void archiveComboSucceeds() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        try {
            Instant now = Instant.now();
            barRepository.saveProduct(newProduct("product_carc", "bar_carc", "套餐用商品"));
            catalog.saveCombo(new BarComboRecord("barcombo_arc", "bar_carc", "merchant_pos_test", "待存档套餐",
                    9900L, null, null, "active", now, now,
                    List.of(new BarComboItemRecord("bci_carc", "barcombo_arc", "product_carc", "套餐用商品", 1))));
            BarComboRecord archived = service.archiveCombo("barcombo_arc", true, "测试存档理由");
            assertThat(archived.archivedAt()).isNotNull();
            assertThat(catalog.listCombos(Optional.empty(), "bar_carc", false)).isEmpty();
            assertThat(catalog.listCombos(Optional.empty(), "bar_carc", true))
                    .extracting(BarComboRecord::id).containsExactly("barcombo_arc");
        } finally {
            com.complex.platform.support.TestSecurityActors.clear();
        }
    }

    @Test
    void approveDeleteComboSucceeds() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        try {
            Instant now = Instant.now();
            barRepository.saveProduct(newProduct("product_cdel", "bar_cdel", "套餐用商品2"));
            catalog.saveCombo(new BarComboRecord("barcombo_del", "bar_cdel", "merchant_pos_test", "待删套餐",
                    9900L, null, null, "active", now, now,
                    List.of(new BarComboItemRecord("bci_cdel", "barcombo_del", "product_cdel", "套餐用商品2", 1))));
            service.archiveCombo("barcombo_del", true, "测试存档理由");
            service.requestDeleteCombo("barcombo_del", new BarCatalogDeleteRequest("撤套餐"));
            // 禁自审：换一个审批人（申请人不能审批自己的申请）。
            com.complex.platform.support.TestSecurityActors.asPlatformAdmin("usr_approver");
            BarCatalogDeleteDecision decision = service.approveDeleteCombo(
                    "barcombo_del", new BarCatalogDeleteApprovalRequest("approved", null));
            assertThat(decision.deleted()).isTrue();
            assertThat(catalog.findCombo("barcombo_del")).isEmpty();
        } finally {
            com.complex.platform.support.TestSecurityActors.clear();
        }
    }

    @Test
    void requestDeleteComboRejectedWhenNotArchived() {
        // 须先存档才可申请删除（防误删在用套餐）。
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        try {
            Instant now = Instant.now();
            barRepository.saveProduct(newProduct("product_cna", "bar_cna", "套餐用商品3"));
            catalog.saveCombo(new BarComboRecord("barcombo_na", "bar_cna", "merchant_pos_test", "未存档套餐",
                    9900L, null, null, "active", now, now,
                    List.of(new BarComboItemRecord("bci_cna", "barcombo_na", "product_cna", "套餐用商品3", 1))));
            assertThatThrownBy(() -> service.requestDeleteCombo("barcombo_na", new BarCatalogDeleteRequest("x")))
                    .isInstanceOf(com.complex.platform.common.api.ApiException.class);
        } finally {
            com.complex.platform.support.TestSecurityActors.clear();
        }
    }

    // ================= V262 复核追加：删除状态机细化 + 引用侧守卫 =================

    @Test
    void deleteRequestInitiatorCannotApprove() {
        // 职责分离：删除申请发起人不能审批自己的申请。
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin("usr_initiator");
        try {
            barRepository.saveProduct(newProduct("product_self", "bar_self", "自审商品"));
            service.archiveProduct("product_self", true, "测试存档理由");
            service.requestDeleteProduct("product_self", new BarCatalogDeleteRequest("自审试探"));
            assertThatThrownBy(() -> service.approveDeleteProduct(
                    "product_self", new BarCatalogDeleteApprovalRequest("approved", null)))
                    .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                    .hasFieldOrPropertyWithValue("code", "BAR_CATALOG_SELF_APPROVE_FORBIDDEN");
        } finally {
            com.complex.platform.support.TestSecurityActors.clear();
        }
    }

    @Test
    void rejectedDeleteResetsToNoneAndKeepsArchive() {
        // 驳回：delete_status 回 none，存档标记保留。
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin("usr_initiator");
        try {
            barRepository.saveProduct(newProduct("product_rej", "bar_rej", "被驳回商品"));
            service.archiveProduct("product_rej", true, "测试存档理由");
            service.requestDeleteProduct("product_rej", new BarCatalogDeleteRequest("误删申请"));
            com.complex.platform.support.TestSecurityActors.asPlatformAdmin("usr_approver");
            BarCatalogDeleteDecision decision = service.approveDeleteProduct(
                    "product_rej", new BarCatalogDeleteApprovalRequest("rejected", null));
            assertThat(decision.deleted()).isFalse();
            BarProductRecord after = catalog.findProduct("product_rej").orElseThrow();
            assertThat(after.deleteStatus()).isEqualTo("none");
            assertThat(after.archivedAt()).isNotNull();
        } finally {
            com.complex.platform.support.TestSecurityActors.clear();
        }
    }

    @Test
    void unarchiveResetsPendingDeleteToNone() {
        // 取消存档：pending 删除申请一并回退 none（避免「生效可见却挂着待删」的迷惑态）。
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        try {
            Instant now = Instant.now();
            catalog.saveCategory(new BarProductCategoryRecord(
                    "barcat_unarc", "bar_unarc", "merchant_pos_test", "回退分类", 1, "active", now, now));
            service.archiveCategory("barcat_unarc", true, "测试存档理由");
            service.requestDeleteCategory("barcat_unarc", new BarCatalogDeleteRequest("先申请后反悔"));
            BarProductCategoryRecord restored = service.archiveCategory("barcat_unarc", false, null);
            assertThat(restored.archivedAt()).isNull();
            assertThat(restored.deleteStatus()).isEqualTo("none");
        } finally {
            com.complex.platform.support.TestSecurityActors.clear();
        }
    }

    @Test
    void archivedCategoryCannotBeAssignedToProduct() {
        // 引用侧守卫①：已存档/待删除分类不可再被商品指派（assignCategory 路径）。
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        try {
            Instant now = Instant.now();
            catalog.saveCategory(new BarProductCategoryRecord(
                    "barcat_na", "bar_na", "merchant_pos_test", "已存档分类", 1, "active", now, now));
            barRepository.saveProduct(newProduct("product_na", "bar_na", "想挂档分类的商品"));
            service.archiveCategory("barcat_na", true, "测试存档理由");
            assertThatThrownBy(() -> service.assignCategory(
                    "product_na", new BarProductCategoryAssignRequest("barcat_na")))
                    .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                    .hasFieldOrPropertyWithValue("code", "BAR_CATEGORY_NOT_ASSIGNABLE");
        } finally {
            com.complex.platform.support.TestSecurityActors.clear();
        }
    }

    @Test
    void createProductIntoArchivedCategoryRejected() {
        // 引用侧守卫①（saveProduct 路径）：新录入商品不可挂到已存档/待删除分类。
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        try {
            Instant now = Instant.now();
            catalog.saveCategory(new BarProductCategoryRecord(
                    "barcat_cp", "bar_cp", null, "已存档分类2", 1, "active", now, now));
            service.archiveCategory("barcat_cp", true, "测试存档理由");
            assertThatThrownBy(() -> barOrderService.createProduct(new BarProductRequest(
                    "cp-key-1", null, List.of(), "bar_cp", "新品", "spirit", 1000L, null, null,
                    "active", null, null, false, null, null, null, null, "barcat_cp", null)))
                    .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                    .hasFieldOrPropertyWithValue("code", "BAR_CATEGORY_NOT_ASSIGNABLE");
        } finally {
            com.complex.platform.support.TestSecurityActors.clear();
        }
    }

    @Test
    void archivedProductCannotJoinCombo() {
        // 引用侧守卫②：已存档/待删除商品不可再被加进套餐（saveCombo 路径）。
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        try {
            barRepository.saveProduct(newProduct("product_aj", "bar_aj", "已存档商品"));
            service.archiveProduct("product_aj", true, "测试存档理由");
            assertThatThrownBy(() -> service.saveCombo(new BarComboRequest(
                    null, "bar_aj", "守卫套餐", 5000L, null, null, "active",
                    List.of(new BarComboRequest.Item("product_aj", 1)))))
                    .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                    .hasFieldOrPropertyWithValue("code", "BAR_PRODUCT_NOT_ASSIGNABLE");
        } finally {
            com.complex.platform.support.TestSecurityActors.clear();
        }
    }

    @Test
    void archivedProductCannotBeOrderedDirectly() {
        // 引用侧守卫③：已存档商品不可下单——菜单隐藏之外，直传 product_id 也拒。
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        try {
            Instant now = Instant.now();
            barRepository.saveProduct(new BarProductRecord("product_ns", "bar_ns", null, "已存档酒", "spirit",
                    3000L, "CNY", null, null, false, "active", now, now, null, null, null));
            barRepository.saveTable(new BarTableRecord("bartable_ns", "bar_ns", null, "T-NS", "守卫桌",
                    "card_seat", 4, 0, 0L, "CNY", "active", now, now));
            service.archiveProduct("product_ns", true, "测试存档理由");
            assertThatThrownBy(() -> barOrderService.createOrder(new BarOrderRequest(
                    "ns-order-1", null, List.of(), "bar_ns", "bartable_ns", null,
                    List.of(java.util.Map.of("product_id", "product_ns", "quantity", "1")),
                    null, List.of(), null)))
                    .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                    .hasFieldOrPropertyWithValue("code", "BAR_PRODUCT_NOT_SELLABLE");
        } finally {
            com.complex.platform.support.TestSecurityActors.clear();
        }
    }

    @Test
    void archivedComboCannotBeOrdered() {
        // 引用侧守卫③：已存档套餐不可下单（直传 combo_id 也拒）。
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        try {
            Instant now = Instant.now();
            barRepository.saveProduct(new BarProductRecord("product_cns", "bar_cns", null, "套餐酒", "spirit",
                    3000L, "CNY", null, null, false, "active", now, now, null, null, null));
            catalog.saveCombo(new BarComboRecord("barcombo_ns", "bar_cns", null, "已存档套餐",
                    5000L, null, null, "active", now, now,
                    List.of(new BarComboItemRecord("bci_ns", "barcombo_ns", "product_cns", "套餐酒", 1))));
            barRepository.saveTable(new BarTableRecord("bartable_cns", "bar_cns", null, "T-CNS", "守卫桌2",
                    "card_seat", 4, 0, 0L, "CNY", "active", now, now));
            service.archiveCombo("barcombo_ns", true, "测试存档理由");
            assertThatThrownBy(() -> barOrderService.createOrder(new BarOrderRequest(
                    "cns-order-1", null, List.of(), "bar_cns", "bartable_cns", null,
                    List.of(java.util.Map.of("combo_id", "barcombo_ns", "quantity", "1")),
                    null, List.of(), null)))
                    .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                    .hasFieldOrPropertyWithValue("code", "BAR_COMBO_NOT_SELLABLE");
        } finally {
            com.complex.platform.support.TestSecurityActors.clear();
        }
    }

    @Test
    void productReferenceGuardListStaysInSyncWithSchemaForeignKeys() {
        // 守护:countProductReferences 用显式清单 {bar_order_items, bar_combo_items}(因 product_id 是通用列名,
        // 元数据驱动会误算别域同名列)。若将来有新表加外键引用 bar_products(id),此测试红→强制同步清单,
        // 否则漏表会导致该商品删除后订单/明细成孤儿。
        java.util.List<String> referencing = jdbcTemplate.queryForList("""
                SELECT DISTINCT tc.table_name
                  FROM information_schema.table_constraints tc
                  JOIN information_schema.constraint_column_usage ccu
                    ON tc.constraint_name = ccu.constraint_name
                   AND tc.table_schema = ccu.table_schema
                 WHERE tc.constraint_type = 'FOREIGN KEY'
                   AND tc.table_schema = 'public'
                   AND ccu.table_name = 'bar_products'
                   AND ccu.column_name = 'id'
                """, String.class);
        org.assertj.core.api.Assertions.assertThat(referencing)
                .as("引用 bar_products(id) 的外键表集合须与 countProductReferences 显式清单一致;新增引用表请同步 JdbcBarPosCatalogRepository.countProductReferences")
                .containsExactlyInAnyOrder("bar_order_items", "bar_combo_items");
    }
}
