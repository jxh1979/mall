package com.complex.platform.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.inventory.dto.PurchaseRequestReceiveRequest;
import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * CG-10 采购入库确认形成对供应商的应付（BSM「采购周期」：「入库确认后按供应合同的价格口径与账期形成对供应商的应付，
 * 进入应收应付台账」；BSM「应收应付台账」：「来源（含采购入库形成的供应商应付）」）。
 *
 * <p>走真实 Flyway 迁移库，{@code @Transactional} 每用例回滚；入库确认走正路 {@code PurchaseRequestService.receive}
 * （真实幂等记录、入库流水、余额），追 {@code ar_ap_items} / {@code ar_ap_item_sources} / {@code inventory_purchase_requests.status} 列。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class PurchaseInboundPayableJdbcIntegrationTests {

    @Autowired private PurchaseRequestService purchaseRequestService;
    @Autowired private JdbcInventoryRepository inventoryRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String suffix;
    private String itemId;
    private String supplierId;
    private String purchaseId;

    @BeforeEach
    void seed() {
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        itemId = "item_cg10_" + suffix;
        supplierId = "sup_cg10_" + suffix;
        purchaseId = "pr_cg10_" + suffix;
        jdbcTemplate.update("INSERT INTO inventory_items (id, item_name, item_type, unit, min_quantity, status, created_at, updated_at) "
                + "VALUES (?, '采购应付测试物料', 'beverage', '瓶', 0, 'active', now(), now())", itemId);
        jdbcTemplate.update("INSERT INTO inventory_suppliers (id, supplier_name, supplier_type, status, created_at, updated_at) "
                + "VALUES (?, '采购应付测试供应商', 'general', 'active', now(), now())", supplierId);
        Instant now = Instant.now();
        // 采购单已到货（arrived），数量 10，待入库确认。
        inventoryRepository.savePurchaseRequest(new PurchaseRequestRecord(
                purchaseId, "PR-CG10-" + suffix, supplierId, itemId, 10, LocalDate.now(LocalZone.ID),
                "arrived", List.of(), now, now, null,
                null, "usr_appr", now, LocalDate.now(LocalZone.ID), null, null));
        authenticateWithPurchasePermission();
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    /** 入库确认后存在一条对该供应商的应付：金额 = 入库数量 × 录入单价，到期 = 入库日 + 供应商账期；重放不重复挂账。 */
    @Test
    void receiveCreatesSupplierPayableAndReplayDoesNotDuplicate() {
        settlementMethod("credit_term", 30);
        PurchaseRequestReceiveRequest request = new PurchaseRequestReceiveRequest(
                "recv_" + suffix, "loc_cg10_" + suffix, null, List.of(), null, 1_500L);

        purchaseRequestService.receive(purchaseId, request);

        assertThat(purchaseStatus()).isEqualTo("received");
        List<Map<String, Object>> payables = payables();
        assertThat(payables).hasSize(1);
        Map<String, Object> payable = payables.get(0);
        assertThat(payable).containsEntry("direction", "payable")
                .containsEntry("counterparty_id", supplierId)
                .containsEntry("source_type", "purchase_inbound")
                .containsEntry("source_id", purchaseId)
                .containsEntry("amount_cent", 15_000L)
                .containsEntry("paid_amount_cent", 0L)
                .containsEntry("unpaid_amount_cent", 15_000L)
                .containsEntry("status", "pending_payment");
        assertThat(((java.sql.Date) payable.get("due_date")).toLocalDate())
                .isEqualTo(LocalDate.now(LocalZone.ID).plusDays(30));
        // 来源明细指向本次入库流水（采购单 received 写入的 inbound 行）。
        String movementId = jdbcTemplate.queryForObject("""
                SELECT id FROM inventory_stock_movements
                 WHERE business_type = 'purchase_request' AND business_id = ? AND movement_type = 'inbound'
                """, String.class, purchaseId);
        assertThat(jdbcTemplate.queryForList("""
                SELECT source_type, source_id, amount_cent FROM ar_ap_item_sources WHERE ar_ap_item_id = ?
                """, payable.get("id")))
                .singleElement()
                .satisfies(src -> assertThat(src).containsEntry("source_type", "purchase_inbound")
                        .containsEntry("source_id", movementId).containsEntry("amount_cent", 15_000L));

        // 幂等重放（同幂等键）：不重复挂账、不重复入库。
        purchaseRequestService.receive(purchaseId, request);
        assertThat(payables()).hasSize(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM inventory_stock_movements
                 WHERE business_type = 'purchase_request' AND business_id = ?
                """, Integer.class, purchaseId)).isEqualTo(1);
    }

    /** 月结供应商：到期 = 入库日所在月最后一天。 */
    @Test
    void monthlySettlementDueAtEndOfReceivingMonth() {
        settlementMethod("monthly", null);
        purchaseRequestService.receive(purchaseId, new PurchaseRequestReceiveRequest(
                "recv_" + suffix, "loc_cg10_" + suffix, null, List.of(), null, 200L));
        assertThat(payables()).hasSize(1);
        Map<String, Object> payable = payables().get(0);
        assertThat(payable).containsEntry("amount_cent", 2_000L);
        LocalDate today = LocalDate.now(LocalZone.ID);
        assertThat(((java.sql.Date) payable.get("due_date")).toLocalDate())
                .isEqualTo(today.withDayOfMonth(today.lengthOfMonth()));
    }

    /** 供应商未配结算档、采购单未挂合同：按账期 0 天落账（到期 = 入库日）。 */
    @Test
    void noSettlementMethodFallsBackToZeroTermDays() {
        purchaseRequestService.receive(purchaseId, new PurchaseRequestReceiveRequest(
                "recv_" + suffix, "loc_cg10_" + suffix, null, List.of(), null, 100L));
        assertThat(payables()).hasSize(1);
        Map<String, Object> payable = payables().get(0);
        assertThat(((java.sql.Date) payable.get("due_date")).toLocalDate()).isEqualTo(LocalDate.now(LocalZone.ID));
    }

    /** 入库确认未录合同单价：无法形成应付，拒绝入库（400），采购单仍为 arrived，无入库流水、无应付。 */
    @Test
    void receiveWithoutUnitPriceIsRejected() {
        settlementMethod("credit_term", 30);
        assertThatThrownBy(() -> purchaseRequestService.receive(purchaseId, new PurchaseRequestReceiveRequest(
                "recv_" + suffix, "loc_cg10_" + suffix, null, List.of(), null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThat(purchaseStatus()).isEqualTo("arrived");
        assertThat(payables()).isEmpty();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM inventory_stock_movements
                 WHERE business_type = 'purchase_request' AND business_id = ?
                """, Integer.class, purchaseId)).isZero();
    }

    // ---- helpers ----

    private void settlementMethod(String method, Integer termDays) {
        jdbcTemplate.update("INSERT INTO supplier_settlement_methods (id, supplier_id, settlement_method, payment_term_days, "
                + "is_default, status, created_at, updated_at) VALUES (?, ?, ?, ?, TRUE, 'active', now(), now())",
                "ssm_cg10_" + suffix, supplierId, method, termDays);
    }

    private String purchaseStatus() {
        return jdbcTemplate.queryForObject("SELECT status FROM inventory_purchase_requests WHERE id = ?",
                String.class, purchaseId);
    }

    private List<Map<String, Object>> payables() {
        return jdbcTemplate.queryForList("""
                SELECT id, direction, counterparty_id, source_type, source_id, amount_cent, paid_amount_cent,
                       unpaid_amount_cent, due_date, status
                  FROM ar_ap_items
                 WHERE source_type = 'purchase_inbound' AND source_id = ?
                """, purchaseId);
    }

    /** 入库确认走 requirePermission(inventory.purchase.approve)：高危码，须给操作人显式角色来源。 */
    private void authenticateWithPurchasePermission() {
        String userId = "usr_cg10_" + suffix;
        String roleId = "role_cg10_" + suffix;
        jdbcTemplate.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', 'org_1', NULL, 'active', now())", userId, "u_cg10_" + suffix);
        jdbcTemplate.update("INSERT INTO roles (id, role_name, created_at) VALUES (?, ?, now())", roleId,
                "采购应付测试角色" + System.nanoTime());
        jdbcTemplate.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, 'inventory.purchase.approve')", roleId);
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        AccessActor actor = new AccessActor(userId, "u_cg10_" + suffix, "platform_admin", "org_1", null, "active");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }
}
