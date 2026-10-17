package com.complex.platform.dealer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.dealer.dto.DealerCatalogDecisionRequest;
import com.complex.platform.dealer.dto.DealerCatalogDraftRequest;
import com.complex.platform.dealer.dto.DealerCatalogLineRequest;
import com.complex.platform.dealer.dto.DealerContractRequest;
import com.complex.platform.dealer.dto.DealerCreateRequest;
import com.complex.platform.dealer.dto.DealerDeactivateRequest;
import com.complex.platform.dealer.dto.DealerInquiryRequest;
import com.complex.platform.dealer.dto.DealerQuoteDecisionRequest;
import com.complex.platform.dealer.dto.DealerQuoteRequest;
import com.complex.platform.dealer.dto.DealerReviewRequest;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * CG-11a（BSM:1216-1222 外部经销商准入、价格与库存；ENUM:170 经销商状态、171 询价状态、172 价格目录状态、175 库存外显、178 职责分离）。
 * 真实 PostgreSQL（Flyway 全量迁移），@Transactional 回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=${COMPLEX_DATABASE_USERNAME:complex}",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class DealerAdmissionCatalogJdbcIntegrationTests {

    @Autowired private DealerService service;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AccessRepository access;
    @Autowired private PlatformTransactionManager transactionManager;

    private String suffix;
    private String editor;
    private String approver;
    private String salesEntity;
    private String warehouse;
    private String otherWarehouse;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        editor = "usr_dlr_edit_" + suffix;
        approver = "usr_dlr_appr_" + suffix;
        String role = "role_dlr_" + suffix;
        jdbc.update("INSERT INTO roles(id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '经销测试', 'platform', 'active', now())",
                role, "经销测试角色" + System.nanoTime());
        jdbc.update("INSERT INTO role_permissions(role_id, permission_code) SELECT ?, permission_code FROM permissions WHERE permission_code LIKE 'dealer.%' ON CONFLICT DO NOTHING", role);
        for (String user : List.of(editor, approver)) {
            access.createUser(new AccessUser(user, user, "platform_admin", "project_main", null, "active", Instant.now()));
            access.assignUserRole(user, role);
        }
        salesEntity = "be_dlr_" + suffix;
        jdbc.update("INSERT INTO business_entities(id, entity_name, entity_role, status, created_at, updated_at) VALUES (?, '经销销售主体', 'project_company', 'active', now(), now())", salesEntity);
        warehouse = "wh_dlr_main_" + suffix;
        otherWarehouse = "wh_dlr_other_" + suffix;
        jdbc.update("INSERT INTO warehouses(id, project_id, warehouse_type, name, status) VALUES (?, 'project_main', 'main', '经销中心仓', 'active')", warehouse);
        jdbc.update("INSERT INTO warehouses(id, project_id, warehouse_type, name, status) VALUES (?, 'project_main', 'main', '其他仓', 'active')", otherWarehouse);
        actAs(editor);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** ENUM:170 五态闭集 + BSM:1217「企业资料、合同、开票和银行资料审核通过后，才可启用独立经销商 Web 门户」+ 停用后禁止新询价。 */
    @Test
    void dealerStatusClosedSetAndPortalEnabledOnlyAfterFourReviews() {
        DealerRecord created = service.createDealer(new DealerCreateRequest(key(), "测试经销商" + suffix, "91310000MA1DLR" + suffix.substring(0, 4), "张三", "李四", "13800000000"));
        assertThat(created.status()).isEqualTo("pending_submission");
        assertThat(created.portalEnabled()).isFalse();
        DealerRecord submitted = service.submitDealer(created.id(), key());
        assertThat(submitted.status()).isEqualTo("under_review");

        actAs(approver);
        assertThatThrownBy(() -> service.reviewDealer(created.id(), new DealerReviewRequest(key(), "approve", true, true, true, false, "银行资料未核")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_REVIEW_INCOMPLETE"));
        assertThat(dealerRow(created.id(), "status")).isEqualTo("under_review");
        DealerRecord approved = service.reviewDealer(created.id(), new DealerReviewRequest(key(), "approve", true, true, true, true, "四项资料均通过"));
        assertThat(approved.status()).isEqualTo("approved");
        assertThat(approved.portalEnabled()).isTrue();
        assertThat(dealerRow(created.id(), "portal_enabled")).isEqualTo(true);

        DealerContractRecord contract = seedContract(created.id());
        String item = seedItem("停用后询价", 5);
        DealerRecord deactivated = service.deactivateDealer(created.id(), new DealerDeactivateRequest(key(), "合同终止"));
        assertThat(deactivated.status()).isEqualTo("deactivated");
        assertThat(deactivated.portalEnabled()).isFalse();
        assertThatThrownBy(() -> service.createInquiry(created.id(), new DealerInquiryRequest(key(), contract.id(), item, "箱", 10, "停用后询价")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_DEACTIVATED"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name = 'dealer.deactivated' AND business_id = ?", Integer.class, created.id())).isEqualTo(1);
    }

    /** ENUM:172 四态闭集、版本递增、「合同价格编辑与审批不得同人」（ENUM:178）、新版本生效后旧版本失效。 */
    @Test
    void priceCatalogVersionsAreApprovedByAnotherPersonAndSupersedePriorVersion() {
        DealerRecord dealer = approvedDealer();
        DealerContractRecord contract = seedContract(dealer.id());
        String item = seedItem("合同价商品", 2);
        DealerPriceCatalogRecord v1 = service.createCatalogDraft(contract.id(), new DealerCatalogDraftRequest(key(), "首版目录",
                List.of(new DealerCatalogLineRequest(item, "箱", 12000L, 1300, null))));
        assertThat(v1.version()).isEqualTo(1);
        assertThat(v1.status()).isEqualTo("draft");
        assertThat(v1.editedBy()).isEqualTo(editor);
        DealerPriceCatalogRecord pending = service.submitCatalog(v1.id(), key());
        assertThat(pending.status()).isEqualTo("pending_approval");
        assertThatThrownBy(() -> service.decideCatalog(v1.id(), new DealerCatalogDecisionRequest(key(), "approve", "自审")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_SOD_SAME_PERSON"));
        assertThat(catalogStatus(v1.id())).isEqualTo("pending_approval");

        actAs(approver);
        DealerPriceCatalogRecord effective = service.decideCatalog(v1.id(), new DealerCatalogDecisionRequest(key(), "approve", "通过"));
        assertThat(effective.status()).isEqualTo("effective");
        assertThat(effective.approvedBy()).isEqualTo(approver);

        actAs(editor);
        DealerPriceCatalogRecord v2 = service.createCatalogDraft(contract.id(), new DealerCatalogDraftRequest(key(), "调价",
                List.of(new DealerCatalogLineRequest(item, "箱", 11000L, 1300, null))));
        assertThat(v2.version()).isEqualTo(2);
        service.submitCatalog(v2.id(), key());
        actAs(approver);
        service.decideCatalog(v2.id(), new DealerCatalogDecisionRequest(key(), "approve", "通过"));
        assertThat(catalogStatus(v2.id())).isEqualTo("effective");
        assertThat(catalogStatus(v1.id())).isEqualTo("expired");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dealer_price_catalogs WHERE dealer_contract_id = ? AND status = 'effective'", Integer.class, contract.id())).isEqualTo(1);

        // 驳回回到草稿
        actAs(editor);
        DealerPriceCatalogRecord v3 = service.createCatalogDraft(contract.id(), new DealerCatalogDraftRequest(key(), "再调",
                List.of(new DealerCatalogLineRequest(item, "箱", 10000L, 1300, null))));
        service.submitCatalog(v3.id(), key());
        actAs(approver);
        assertThat(service.decideCatalog(v3.id(), new DealerCatalogDecisionRequest(key(), "reject", "价格偏低")).status()).isEqualTo("draft");
        assertThat(catalogStatus(v2.id())).isEqualTo("effective");
    }

    /** BSM:1220「已定合同价商品可下单；未定价商品只能询价……报价审批通过后才能订购」+ ENUM:171 四态 + 报价人与审批人不得同人。 */
    @Test
    void inquiryOnlyForUnpricedItemsAndQuoteApprovedByAnotherPerson() {
        DealerRecord dealer = approvedDealer();
        DealerContractRecord contract = seedContract(dealer.id());
        String priced = seedItem("已定价", 2);
        String unpriced = seedItem("未定价", 2);
        DealerPriceCatalogRecord v1 = service.createCatalogDraft(contract.id(), new DealerCatalogDraftRequest(key(), "目录",
                List.of(new DealerCatalogLineRequest(priced, "箱", 9000L, 1300, null))));
        service.submitCatalog(v1.id(), key());
        actAs(approver);
        service.decideCatalog(v1.id(), new DealerCatalogDecisionRequest(key(), "approve", "通过"));

        actAs(editor);
        assertThatThrownBy(() -> service.createInquiry(dealer.id(), new DealerInquiryRequest(key(), contract.id(), priced, "箱", 5, "已定价询价")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("ITEM_ALREADY_PRICED"));
        DealerInquiryRecord inquiry = service.createInquiry(dealer.id(), new DealerInquiryRequest(key(), contract.id(), unpriced, "箱", 5, "询价"));
        assertThat(inquiry.status()).isEqualTo("pending_quote");
        DealerInquiryRecord quoting = service.quoteInquiry(inquiry.id(), new DealerQuoteRequest(key(), 8800L, LocalDate.now().plusDays(7), "报价"));
        assertThat(quoting.status()).isEqualTo("quote_approval");
        assertThat(quoting.quotedBy()).isEqualTo(editor);
        assertThatThrownBy(() -> service.decideQuote(inquiry.id(), new DealerQuoteDecisionRequest(key(), "approve", "自审")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_SOD_SAME_PERSON"));
        actAs(approver);
        DealerInquiryRecord quoted = service.decideQuote(inquiry.id(), new DealerQuoteDecisionRequest(key(), "approve", "通过"));
        assertThat(quoted.status()).isEqualTo("quoted");
        assertThat(quoted.quotedPriceCent()).isEqualTo(8800L);
        DealerInquiryRecord expired = service.expireInquiry(inquiry.id(), key());
        assertThat(expired.status()).isEqualTo("expired");

        // 驳回报价回到待报价
        actAs(editor);
        DealerInquiryRecord second = service.createInquiry(dealer.id(), new DealerInquiryRequest(key(), contract.id(), unpriced, "瓶", 3, "再询"));
        service.quoteInquiry(second.id(), new DealerQuoteRequest(key(), 500L, LocalDate.now().plusDays(3), "报价"));
        actAs(approver);
        assertThat(service.decideQuote(second.id(), new DealerQuoteDecisionRequest(key(), "reject", "过低")).status()).isEqualTo("pending_quote");
    }

    /** BSM:1221 / ENUM:175：外部只见 有货、紧张、缺货、预计交期，不得见精确数量、仓库总库存或内部调拨。 */
    @Test
    void externalStockShowsOnlyFourStatesAndNeverQuantities() {
        DealerRecord dealer = approvedDealer();
        DealerContractRecord contract = seedContract(dealer.id());
        String inStock = seedItem("有货", 2);
        String low = seedItem("紧张", 5);
        String out = seedItem("缺货", 2);
        String restock = seedItem("预计交期", 2);
        String unlisted = seedItem("未上目录", 2);
        seedBalance(inStock, warehouse, 10);
        seedBalance(low, warehouse, 3);
        seedBalance(low, otherWarehouse, 100); // 其他仓库存不得混入中心仓外显
        seedBalance(out, otherWarehouse, 50);
        seedBalance(unlisted, warehouse, 10);
        DealerPriceCatalogRecord v1 = service.createCatalogDraft(contract.id(), new DealerCatalogDraftRequest(key(), "目录", List.of(
                new DealerCatalogLineRequest(inStock, "箱", 1000L, 1300, null),
                new DealerCatalogLineRequest(low, "箱", 1000L, 1300, null),
                new DealerCatalogLineRequest(out, "箱", 1000L, 1300, null),
                new DealerCatalogLineRequest(restock, "箱", 1000L, 1300, LocalDate.now().plusDays(10)))));
        service.submitCatalog(v1.id(), key());
        actAs(approver);
        service.decideCatalog(v1.id(), new DealerCatalogDecisionRequest(key(), "approve", "通过"));

        List<DealerExternalStockRecord> rows = service.externalStock(contract.id()).items();
        assertThat(rows).extracting(DealerExternalStockRecord::itemId).containsExactlyInAnyOrder(inStock, low, out, restock);
        assertThat(availability(rows, inStock)).isEqualTo("in_stock");
        assertThat(availability(rows, low)).isEqualTo("low");
        assertThat(availability(rows, out)).isEqualTo("out_of_stock");
        assertThat(availability(rows, restock)).isEqualTo("expected_restock");
        assertThat(rows.stream().filter(r -> r.itemId().equals(restock)).findFirst().orElseThrow().expectedRestockDate()).isEqualTo(LocalDate.now().plusDays(10));
        for (RecordComponent component : DealerExternalStockRecord.class.getRecordComponents()) {
            assertThat(component.getName()).as("外显不得带数量字段").doesNotContainIgnoringCase("quantity").doesNotContainIgnoringCase("qty");
        }
    }

    /** 库 CHECK 与 ENUM:170 / 171 / 172 闭集逐字对应：闭集之外的值一律拒绝（各用保存点；本例不触发任何服务层异常以免外层事务被标记回滚）。 */
    @Test
    void statusChecksRejectValuesOutsideClosedSets() {
        DealerRecord dealer = approvedDealer();
        DealerContractRecord contract = seedContract(dealer.id());
        String item = seedItem("闭集商品", 2);
        DealerPriceCatalogRecord catalog = service.createCatalogDraft(contract.id(), new DealerCatalogDraftRequest(key(), "目录",
                List.of(new DealerCatalogLineRequest(item, "箱", 1000L, 1300, null))));
        DealerInquiryRecord inquiry = service.createInquiry(dealer.id(), new DealerInquiryRequest(key(), contract.id(), item, "瓶", 1, "询价"));
        TransactionTemplate nested = new TransactionTemplate(transactionManager);
        nested.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        for (String illegal : List.of("suspended", "active", "pending")) {
            assertThatThrownBy(() -> nested.executeWithoutResult(s -> jdbc.update("UPDATE dealers SET status = ?, portal_enabled = FALSE WHERE id = ?", illegal, dealer.id())))
                    .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("chk_dealers_status");
        }
        for (String illegal : List.of("active", "approved", "archived")) {
            assertThatThrownBy(() -> nested.executeWithoutResult(s -> jdbc.update("UPDATE dealer_price_catalogs SET status = ? WHERE id = ?", illegal, catalog.id())))
                    .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("chk_dealer_price_catalogs_status");
        }
        for (String illegal : List.of("ordered", "approved", "cancelled")) {
            assertThatThrownBy(() -> nested.executeWithoutResult(s -> jdbc.update("UPDATE dealer_inquiries SET status = ? WHERE id = ?", illegal, inquiry.id())))
                    .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("chk_dealer_inquiries_status");
        }
        // 门户只能在「已通过」状态下启用（BSM:1217）
        assertThatThrownBy(() -> nested.executeWithoutResult(s -> jdbc.update("UPDATE dealers SET status = 'under_review', portal_enabled = TRUE WHERE id = ?", dealer.id())))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("chk_dealers_portal_requires_approved");
    }

    // ── helpers ──

    private DealerRecord approvedDealer() {
        DealerRecord created = service.createDealer(new DealerCreateRequest(key(), "经销商" + suffix + System.nanoTime(), "9131" + System.nanoTime(), "法人", "联系人", "13900000000"));
        service.submitDealer(created.id(), key());
        actAs(approver);
        service.reviewDealer(created.id(), new DealerReviewRequest(key(), "approve", true, true, true, true, "通过"));
        actAs(editor);
        return service.getDealer(created.id());
    }

    private DealerContractRecord seedContract(String dealerId) {
        return service.createContract(dealerId, new DealerContractRequest(key(), "DC-" + System.nanoTime(), salesEntity, warehouse,
                LocalDate.now().minusDays(1), LocalDate.now().plusYears(1)));
    }

    private String seedItem(String name, int minQuantity) {
        String id = "item_dlr_" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO inventory_items(id, item_name, item_type, unit, trace_required, min_quantity, status, created_at, updated_at) VALUES (?, ?, 'beverage', '箱', false, ?, 'active', now(), now())",
                id, name, minQuantity);
        return id;
    }

    private void seedBalance(String itemId, String warehouseId, int available) {
        jdbc.update("INSERT INTO inventory_stock_balances(id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, warehouse_id, project_id) VALUES (?, ?, ?, ?, 0, ?, now(), ?, 'project_main')",
                "bal_" + UUID.randomUUID().toString().substring(0, 8), itemId, warehouseId, available, available, warehouseId);
    }

    private static String availability(List<DealerExternalStockRecord> rows, String itemId) {
        return rows.stream().filter(r -> r.itemId().equals(itemId)).findFirst().orElseThrow().availability();
    }

    private Object dealerRow(String id, String column) {
        return jdbc.queryForMap("SELECT " + column + " FROM dealers WHERE id = ?", id).get(column);
    }

    private String catalogStatus(String id) {
        return jdbc.queryForObject("SELECT status FROM dealer_price_catalogs WHERE id = ?", String.class, id);
    }

    private void actAs(String userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(userId, userId, "platform_admin", "project_main", null, "active"), "test", List.of()));
    }

    private static String key() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
