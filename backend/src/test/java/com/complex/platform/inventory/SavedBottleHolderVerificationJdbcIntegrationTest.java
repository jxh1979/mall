package com.complex.platform.inventory;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.inventory.dto.SavedBottleClaimRequest;
import com.complex.platform.inventory.dto.SavedBottleManualPickupPolicyRequest;
import com.complex.platform.inventory.dto.SavedBottleManualPickupRequest;
import com.complex.platform.inventory.dto.SavedBottleRequest;
import com.complex.platform.inventory.dto.SavedBottleTransferRequest;
import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * FS-016 真库回归（V512）：转让换持有人并落操作来源/转让前手机哈希；取酒流水落取用前后剩余量与核验方式；
 * 动态取酒码只存哈希、窗口=全场统一刷新间隔、取酒后 consumed；有效卡座订单计数与归属营销按真 SQL。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SavedBottleHolderVerificationJdbcIntegrationTest {

    private static final String HOLDER_PHONE = "15500020001";
    private static final String TRANSFEREE_PHONE = "15500020002";

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private com.complex.platform.common.crypto.PiiCipher piiCipher;

    private String prefix;

    @BeforeEach
    void setUp() {
        TestSecurityActors.asPlatformAdmin();
        prefix = "sw_hv_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        jdbcTemplate.update("""
                INSERT INTO inventory_items (id, sku_code, item_name, item_type, unit, barcode, trace_required,
                    min_quantity, status, created_at, updated_at)
                VALUES (?, ?, 'Holder Whisky', 'wine', 'bottle', NULL, false, 0, 'active', now(), now())
                """, prefix + "_item", prefix + "_sku");
    }

    @AfterEach
    void tearDown() {
        String bottles = "(SELECT id FROM saved_bottles WHERE item_id = '" + prefix + "_item')";
        jdbcTemplate.update("DELETE FROM saved_bottle_manual_pickups WHERE saved_bottle_id IN " + bottles);
        jdbcTemplate.update("DELETE FROM saved_bottle_manual_pickup_policy WHERE project_id LIKE ?", prefix + "%");
        jdbcTemplate.update("DELETE FROM saved_bottle_pickup_codes WHERE saved_bottle_id IN " + bottles);
        jdbcTemplate.update("DELETE FROM saved_bottle_claims WHERE saved_bottle_id IN " + bottles);
        jdbcTemplate.update("DELETE FROM saved_bottle_transfers WHERE saved_bottle_id IN " + bottles);
        jdbcTemplate.update("DELETE FROM saved_bottles WHERE item_id = ?", prefix + "_item");
        jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key LIKE ?", prefix + "%");
        // 卡座订单夹具（projects/merchants/bar_tables/bar_orders/bar_bills/bar_bill_adjustments）不清：账单减项为追加写表，
        // 删除被触发器拒绝，且上游表被其外键引用；夹具 id 带唯一前缀，残留不影响其他用例。
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id LIKE ?", prefix + "%");
        jdbcTemplate.update("DELETE FROM role_permissions WHERE role_id LIKE ?", prefix + "%");
        jdbcTemplate.update("DELETE FROM roles WHERE id LIKE ?", prefix + "%");
        jdbcTemplate.update("DELETE FROM access_users WHERE id LIKE ?", prefix + "%");
        jdbcTemplate.update("DELETE FROM staff_profiles WHERE id LIKE ?", prefix + "%");
        jdbcTemplate.update("DELETE FROM customers WHERE id LIKE ?", prefix + "%");
        jdbcTemplate.update("DELETE FROM inventory_items WHERE id LIKE ?", prefix + "%");
        TestSecurityActors.clear();
    }

    @Test
    void transferSwitchesHolderAndClaimLeavesFullTrace() {
        SavedBottleRecord bottle = inventoryService.createSavedBottle(new SavedBottleRequest(
                prefix + "_create", null, List.of(), null, "bar_hv",
                List.of(Map.of("item_id", prefix + "_item", "quantity", 2, "bottle_name", "Holder Whisky", "unit", "bottle")),
                Instant.now().plusSeconds(86400), "LBL-HV", "甲", HOLDER_PHONE, List.of("reg_photo"), null));

        inventoryService.transferSavedBottle(bottle.id(), new SavedBottleTransferRequest(
                prefix + "_transfer", null, List.of(), "乙", TRANSFEREE_PHONE, null, "转让", "000000"));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT customer_name_or_nickname, customer_phone_hash FROM saved_bottles WHERE id = ?", bottle.id());
        assertThat(row.get("customer_name_or_nickname")).isEqualTo("乙");
        assertThat(row.get("customer_phone_hash")).isEqualTo(sha256(TRANSFEREE_PHONE));
        Map<String, Object> transfer = jdbcTemplate.queryForMap(
                "SELECT operation_source, from_phone_hash, to_customer_id, operator_id, transfer_quantity FROM saved_bottle_transfers WHERE saved_bottle_id = ?",
                bottle.id());
        assertThat(transfer.get("operation_source")).isEqualTo("web");
        assertThat(transfer.get("from_phone_hash")).isEqualTo(sha256(HOLDER_PHONE));
        assertThat(transfer.get("operator_id")).isEqualTo("usr_admin");
        assertThat(transfer.get("transfer_quantity")).isEqualTo(2);

        // 受让人按本人短信取酒（test 固定码 000000）：流水落取用前后剩余量/核验方式/被核验手机哈希。
        SavedBottleClaimRecord claim = inventoryService.claimSavedBottle(bottle.id(), new SavedBottleClaimRequest(
                prefix + "_claim", null, List.of(), List.of(Map.of("quantity", 1)), null, "000000", "LBL-HV", "乙"));
        Map<String, Object> claimRow = jdbcTemplate.queryForMap(
                "SELECT remaining_before, remaining_after, verification_method, verified_phone_hash, claim_staff_id FROM saved_bottle_claims WHERE id = ?",
                claim.id());
        assertThat(claimRow.get("remaining_before")).isEqualTo(2);
        assertThat(claimRow.get("remaining_after")).isEqualTo(1);
        assertThat(claimRow.get("verification_method")).isEqualTo("sms");
        assertThat(claimRow.get("verified_phone_hash")).isEqualTo(sha256(TRANSFEREE_PHONE));
        assertThat(claimRow.get("claim_staff_id")).isEqualTo("usr_admin");
    }

    @Test
    void pickupCodeIsHashedUsesUnifiedWindowAndIsConsumedByClaim() {
        String customerId = prefix + "_customer";
        jdbcTemplate.update("""
                INSERT INTO customers (id, phone, phone_hash, masked_phone, status, created_at, updated_at)
                VALUES (?, ?, ?, '155****0001', 'active', now(), now())
                """, customerId, piiCipher.encrypt(HOLDER_PHONE), "bi_" + prefix);
        SavedBottleRecord bottle = inventoryService.createSavedBottle(new SavedBottleRequest(
                prefix + "_create2", null, List.of(), customerId, "bar_hv",
                List.of(Map.of("item_id", prefix + "_item", "quantity", 1, "bottle_name", "Holder Whisky", "unit", "bottle")),
                Instant.now().plusSeconds(86400), "LBL-HV", "甲", HOLDER_PHONE, List.of("reg_photo"), null));

        SavedBottlePickupCodeResponse code = inventoryService.issueSavedBottlePickupCode(bottle.id(), customerId, HOLDER_PHONE);
        Integer configured = jdbcTemplate.queryForObject(
                "SELECT refresh_interval_seconds FROM ticket_qr_refresh_config WHERE id = 'default'", Integer.class);
        assertThat(code.refreshIntervalSeconds()).isEqualTo(configured);
        Map<String, Object> issued = jdbcTemplate.queryForMap(
                "SELECT code_hash, status, refresh_interval_seconds FROM saved_bottle_pickup_codes WHERE saved_bottle_id = ?", bottle.id());
        assertThat(issued.get("code_hash")).isEqualTo(sha256(code.code()));
        assertThat(issued.get("status")).isEqualTo("active");
        assertThat(issued.get("refresh_interval_seconds")).isEqualTo(configured);

        SavedBottleClaimRecord claim = inventoryService.claimSavedBottle(bottle.id(), new SavedBottleClaimRequest(
                prefix + "_claim2", null, List.of(), List.of(Map.of("quantity", 1)), null, code.code(), "LBL-HV", "甲"));
        Map<String, Object> consumed = jdbcTemplate.queryForMap(
                "SELECT status, consumed_claim_id FROM saved_bottle_pickup_codes WHERE saved_bottle_id = ?", bottle.id());
        assertThat(consumed.get("status")).isEqualTo("consumed");
        assertThat(consumed.get("consumed_claim_id")).isEqualTo(claim.id());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT verification_method FROM saved_bottle_claims WHERE id = ?", String.class, claim.id()))
                .isEqualTo("pickup_code");
    }

    @Test
    void validTableOrderCountExcludesUnpaidAndFullyRefundedAndFindsActiveMarketer() {
        String customerId = prefix + "_c3";
        String barId = prefix + "_bar";
        String projectId = prefix + "_project";
        jdbcTemplate.update("INSERT INTO customers (id, phone, phone_hash, masked_phone, status, created_at, updated_at) VALUES (?, ?, ?, '155****0003', 'active', now(), now())",
                customerId, piiCipher.encrypt("15500020003"), "bi3_" + prefix);
        jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', 'HV Project', 'active', now(), now())", projectId);
        jdbcTemplate.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) VALUES (?, 'HV Bar', 'bar', ?, 'active', now(), now())", barId, projectId);
        jdbcTemplate.update("INSERT INTO bar_tables (id, bar_id, table_code, table_name, table_type, status, created_at, updated_at) VALUES (?, ?, 'T1', 'T1', 'card_seat', 'available', now(), now())",
                prefix + "_table", barId);
        jdbcTemplate.update("INSERT INTO staff_profiles (id, project_id, name, user_id, status, registration_status) VALUES (?, ?, '营销甲', ?, 'active', 'approved')",
                prefix + "_sp_active", projectId, prefix + "_mk_active");
        jdbcTemplate.update("INSERT INTO staff_profiles (id, project_id, name, user_id, status, registration_status) VALUES (?, ?, '营销乙', ?, 'inactive', 'approved')",
                prefix + "_sp_left", projectId, prefix + "_mk_left");
        insertPaidBill(prefix + "_b1", barId, customerId, prefix + "_mk_left", Instant.now().minusSeconds(10 * 86400), 10000, 0);
        insertPaidBill(prefix + "_b2", barId, customerId, prefix + "_mk_active", Instant.now().minusSeconds(5 * 86400), 20000, 0);
        insertPaidBill(prefix + "_b3", barId, customerId, null, Instant.now().minusSeconds(60 * 86400), 30000, 30000); // 全额退款不计
        insertPaidBill(prefix + "_b4", barId, customerId, null, Instant.now().minusSeconds(400 * 86400), 40000, 10000); // 部分退款计入

        assertThat(inventoryRepository.countValidTableOrders(customerId, projectId, Instant.now().minusSeconds(30L * 86400))).isEqualTo(2);
        assertThat(inventoryRepository.countValidTableOrders(customerId, projectId, Instant.now().minusSeconds(90L * 86400))).isEqualTo(2);
        assertThat(inventoryRepository.countValidTableOrders(customerId, projectId, null)).isEqualTo(3);
        assertThat(inventoryRepository.countValidTableOrders(customerId, "other_project", null)).isZero();
        assertThat(inventoryRepository.findActiveAttributedMarketer(customerId, projectId)).contains(prefix + "_mk_active");
        assertThat(inventoryRepository.findBarProjectId(barId)).contains(projectId);
    }

    /** BSM L1073 ②：消费历史条件由最高权限本人按项目配置并留痕（updated_by + 审计事件），配置后店长可发起。 */
    @Test
    void manualPickupPolicyConfiguredByMasterAuthorityLeavesTraceAndUnblocksInitiate() {
        String customerId = prefix + "_c4";
        String barId = prefix + "_bar4";
        String projectId = prefix + "_project4";
        jdbcTemplate.update("INSERT INTO customers (id, phone, phone_hash, masked_phone, status, created_at, updated_at) VALUES (?, ?, ?, '155****0004', 'active', now(), now())",
                customerId, piiCipher.encrypt("15500020004"), sha256("15500020004"));
        jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', 'MP Project', 'active', now(), now())", projectId);
        jdbcTemplate.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) VALUES (?, 'MP Bar', 'bar', ?, 'active', now(), now())", barId, projectId);
        jdbcTemplate.update("INSERT INTO bar_tables (id, bar_id, table_code, table_name, table_type, status, created_at, updated_at) VALUES (?, ?, 'T4', 'T4', 'card_seat', 'available', now(), now())",
                prefix + "_table", barId);
        insertPaidBill(prefix + "_mb1", barId, customerId, null, Instant.now().minusSeconds(5 * 86400), 10000, 0);
        insertPaidBill(prefix + "_mb2", barId, customerId, null, Instant.now().minusSeconds(10 * 86400), 10000, 0);
        SavedBottleRecord bottle = inventoryService.createSavedBottle(new SavedBottleRequest(
                prefix + "_mp_create", null, List.of(), customerId, barId,
                List.of(Map.of("item_id", prefix + "_item", "quantity", 1, "bottle_name", "Holder Whisky", "unit", "bottle")),
                Instant.now().plusSeconds(86400), "LBL-MP", "甲", "15500020004", List.of("reg_photo"), null));

        assertThatThrownBy(() -> inventoryService.configureManualPickupPolicy(
                new SavedBottleManualPickupPolicyRequest(prefix + "_cfg0", projectId, 2, 3, 5, 1, 10, "首次配置")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("MASTER_APPROVAL_REQUIRED");

        asMasterAuthority();
        inventoryService.configureManualPickupPolicy(
                new SavedBottleManualPickupPolicyRequest(prefix + "_cfg1", projectId, 2, 3, 5, 1, 10, "首次配置"));
        Map<String, Object> policy = jdbcTemplate.queryForMap(
                "SELECT orders_within_1m, orders_total, min_orders, max_orders, updated_by FROM saved_bottle_manual_pickup_policy WHERE project_id = ?", projectId);
        assertThat(policy.get("orders_within_1m")).isEqualTo(2);
        assertThat(policy.get("orders_total")).isEqualTo(5);
        assertThat(policy.get("min_orders")).isEqualTo(1);
        assertThat(policy.get("max_orders")).isEqualTo(10);
        assertThat(policy.get("updated_by")).isEqualTo(SessionIssuer.MASTER_USER_ID);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE event_name = 'bar.saved_bottle.manual_pickup_policy_configured' AND idempotency_key = ?",
                Integer.class, prefix + "_cfg1")).isEqualTo(1);

        // 发起店长须持 customer.account.manage（客人查询专门通道）：给平台管理员测试账号挂一个只含该码的临时角色（范式同 CG-06b 真库用例）。
        asPlatformAdminWithCustomerLookupPermission();
        SavedBottleManualPickupRecord initiated = inventoryService.initiateManualPickup(bottle.id(),
                new SavedBottleManualPickupRequest(prefix + "_mp1", null, List.of(), "客人手机遗失"));
        assertThat(initiated.status()).isEqualTo("initiated");
        assertThat(initiated.historyCondition()).isEqualTo("orders_within_1m");
        assertThat(initiated.projectId()).isEqualTo(projectId);
    }

    /**
     * BSM L1073 ②「出厂默认：近 1 个月内至少 2 笔、近 3 个月内至少 3 笔、或历史累计至少 5 笔有效卡座消费订单……须同时配置上下限」：
     * 出厂落 global 行（上下限留空，首次改参时与条件值同填，范式同 V497/V543）；未单独配置的项目按出厂条件核对，不再一律拒绝。
     */
    @Test
    void manualPickupUsesFactoryDefaultConditionsWhenProjectUnconfigured() {
        Map<String, Object> factory = jdbcTemplate.queryForList("""
                SELECT orders_within_1m, orders_within_3m, orders_total, min_orders, max_orders
                  FROM saved_bottle_manual_pickup_policy WHERE project_id = 'global'
                """).stream().findFirst().orElse(Map.of());
        assertThat(factory).containsEntry("orders_within_1m", 2).containsEntry("orders_within_3m", 3)
                .containsEntry("orders_total", 5);
        assertThat(factory.get("min_orders")).isNull();
        assertThat(factory.get("max_orders")).isNull();

        String barId = prefix + "_bar5";
        String projectId = prefix + "_project5";
        jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', 'MP Factory', 'active', now(), now())", projectId);
        jdbcTemplate.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) VALUES (?, 'MP Factory Bar', 'bar', ?, 'active', now(), now())", barId, projectId);
        jdbcTemplate.update("INSERT INTO bar_tables (id, bar_id, table_code, table_name, table_type, status, created_at, updated_at) VALUES (?, ?, 'T5', 'T5', 'card_seat', 'available', now(), now())",
                prefix + "_table", barId);
        SavedBottleRecord met = factoryBottle("_c5", "15500020005", barId);
        insertPaidBill(prefix + "_fb1", barId, prefix + "_c5", null, Instant.now().minusSeconds(3 * 86400), 10000, 0);
        insertPaidBill(prefix + "_fb2", barId, prefix + "_c5", null, Instant.now().minusSeconds(12 * 86400), 10000, 0);
        SavedBottleRecord unmet = factoryBottle("_c6", "15500020006", barId);
        insertPaidBill(prefix + "_fb3", barId, prefix + "_c6", null, Instant.now().minusSeconds(3 * 86400), 10000, 0);

        asPlatformAdminWithCustomerLookupPermission();
        assertThat(inventoryService.initiateManualPickup(met.id(),
                new SavedBottleManualPickupRequest(prefix + "_fmp1", null, List.of(), "客人手机遗失")).historyCondition())
                .isEqualTo("orders_within_1m");
        assertThatThrownBy(() -> inventoryService.initiateManualPickup(unmet.id(),
                new SavedBottleManualPickupRequest(prefix + "_fmp2", null, List.of(), "客人手机遗失")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_MANUAL_PICKUP_HISTORY_NOT_MET");
    }

    private SavedBottleRecord factoryBottle(String customerSuffix, String phone, String barId) {
        String customerId = prefix + customerSuffix;
        jdbcTemplate.update("INSERT INTO customers (id, phone, phone_hash, masked_phone, status, created_at, updated_at) VALUES (?, ?, ?, '155****0005', 'active', now(), now())",
                customerId, piiCipher.encrypt(phone), sha256(phone));
        return inventoryService.createSavedBottle(new SavedBottleRequest(
                prefix + customerSuffix + "_create", null, List.of(), customerId, barId,
                List.of(Map.of("item_id", prefix + "_item", "quantity", 1, "bottle_name", "Holder Whisky", "unit", "bottle")),
                Instant.now().plusSeconds(86400), "LBL-MP", "甲", phone, List.of("reg_photo"), null));
    }

    private void asPlatformAdminWithCustomerLookupPermission() {
        String userId = prefix + "_usr";
        String roleId = prefix + "_role";
        jdbcTemplate.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', 'org_1', NULL, 'active', now())", userId, prefix + "_u");
        jdbcTemplate.update("INSERT INTO roles (id, role_name, created_at) VALUES (?, ?, now())", roleId, "存酒人工例外测试角色" + prefix);
        jdbcTemplate.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", roleId, "customer.account.manage");
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        AccessActor actor = new AccessActor(userId, prefix + "_u", "platform_admin", "org_1", null, "active");
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }

    private static void asMasterAuthority() {
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "org_1", null, "active");
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }

    private void insertPaidBill(String id, String barId, String customerId, String salesStaffId, Instant paidAt, long paidCent, long refundCent) {
        jdbcTemplate.update("""
                INSERT INTO bar_orders (id, bar_id, table_id, customer_id, order_source, status, total_amount_cent, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'pos', 'settled', ?, ?, ?)
                """, id, barId, prefix + "_table", customerId, paidCent,
                java.sql.Timestamp.from(paidAt), java.sql.Timestamp.from(paidAt));
        jdbcTemplate.update("""
                INSERT INTO bar_bills (id, bill_no, order_id, bar_id, table_id, subtotal_amount_cent, payable_amount_cent, paid_amount_cent,
                    payment_status, settlement_status, sales_staff_id, paid_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'paid', 'settled', ?, ?, ?, ?)
                """, id, id, id, barId, prefix + "_table", paidCent, paidCent, paidCent, salesStaffId,
                java.sql.Timestamp.from(paidAt), java.sql.Timestamp.from(paidAt), java.sql.Timestamp.from(paidAt));
        if (refundCent > 0) {
            jdbcTemplate.update("""
                    INSERT INTO bar_bill_adjustments (id, bill_id, adjustment_type, amount_cent, before_payable_amount_cent,
                        after_payable_amount_cent, reason, requested_by_user_id, approval_required, status, idempotency_key,
                        refunded_at, created_at)
                    VALUES (?, ?, 'refund', ?, ?, ?, 'test refund', 'usr_admin', false, 'approved', ?, now(), now())
                    """, id + "_rf", id, refundCent, paidCent, paidCent - refundCent, id + "_rf");
        }
    }

    private static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
