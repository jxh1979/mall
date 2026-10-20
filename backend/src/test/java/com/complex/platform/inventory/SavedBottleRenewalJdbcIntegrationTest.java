package com.complex.platform.inventory;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.customer.dto.CustomerSavedBottleRenewRequest;
import com.complex.platform.inventory.dto.SavedBottleRenewalRequest;
import com.complex.platform.inventory.dto.SavedBottleRequest;
import com.complex.platform.support.TestSecurityActors;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * FS-025 真库回归（V513）：转存业务参数出厂行（上限 2 次 / 顺延 30 天，上下限留空）；客人本人转存按真库顺延到期日并固化
 * 配置版本、顺延天数、新到期日、累计次数、经办人与确认方式；员工代办落 pending_review；状态 CHECK 拒非法值。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SavedBottleRenewalJdbcIntegrationTest {

    private static final String HOLDER_PHONE = "15500030001";

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private com.complex.platform.common.crypto.PiiCipher piiCipher;

    private String prefix;

    @BeforeEach
    void setUp() {
        TestSecurityActors.asPlatformAdmin();
        prefix = "sw_rn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        jdbcTemplate.update("""
                INSERT INTO inventory_items (id, sku_code, item_name, item_type, unit, barcode, trace_required,
                    min_quantity, status, created_at, updated_at)
                VALUES (?, ?, 'Renewal Whisky', 'wine', 'bottle', NULL, false, 0, 'active', now(), now())
                """, prefix + "_item", prefix + "_sku");
        jdbcTemplate.update("""
                INSERT INTO customers (id, phone, phone_hash, masked_phone, status, created_at, updated_at)
                VALUES (?, ?, ?, '155****0001', 'active', now(), now())
                """, prefix + "_customer", piiCipher.encrypt(HOLDER_PHONE), "bi_" + prefix);
    }

    @AfterEach
    void tearDown() {
        String bottles = "(SELECT id FROM saved_bottles WHERE item_id = '" + prefix + "_item')";
        jdbcTemplate.update("DELETE FROM saved_bottle_renewals WHERE saved_bottle_id IN " + bottles);
        jdbcTemplate.update("DELETE FROM saved_bottle_pickup_codes WHERE saved_bottle_id IN " + bottles);
        jdbcTemplate.update("DELETE FROM saved_bottles WHERE item_id = ?", prefix + "_item");
        jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key LIKE ?", prefix + "%");
        jdbcTemplate.update("DELETE FROM customers WHERE id LIKE ?", prefix + "%");
        jdbcTemplate.update("DELETE FROM inventory_items WHERE id LIKE ?", prefix + "%");
        TestSecurityActors.clear();
    }

    @Test
    void factoryPolicyRowsExistWithBoundsLeftEmpty() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name IN ('saved_bottle_renewals', 'saved_bottle_renewal_policy')",
                Integer.class)).isEqualTo(2);
        Map<String, Object> limit = jdbcTemplate.queryForMap(
                "SELECT numeric_value, min_value, max_value, version FROM saved_bottle_renewal_policy WHERE policy_key = 'max_renewals' AND scope_id = 'global'");
        assertThat(limit.get("numeric_value")).isEqualTo(2);
        assertThat(limit.get("min_value")).isNull();
        assertThat(limit.get("max_value")).isNull();
        Map<String, Object> days = jdbcTemplate.queryForMap(
                "SELECT numeric_value, min_value, max_value FROM saved_bottle_renewal_policy WHERE policy_key = 'extension_days' AND scope_id = 'global'");
        assertThat(days.get("numeric_value")).isEqualTo(30);
        assertThat(days.get("min_value")).isNull();
    }

    @Test
    void customerRenewalExtendsExpiryAndPersistsSnapshotAndStaffRenewalWaitsForReview() {
        Instant expireAt = Instant.now().plus(Duration.ofDays(5)).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        SavedBottleRecord bottle = inventoryService.createSavedBottle(new SavedBottleRequest(
                prefix + "_create", null, List.of(), prefix + "_customer", "bar_rn",
                List.of(Map.of("item_id", prefix + "_item", "quantity", 1, "bottle_name", "Renewal Whisky", "unit", "bottle")),
                expireAt, "LBL-RN", "甲", HOLDER_PHONE, List.of("reg_photo"), null));

        SavedBottleRenewalRecord renewal = inventoryService.renewSavedBottleByCustomer(
                bottle.id(), prefix + "_customer", HOLDER_PHONE, new CustomerSavedBottleRenewRequest(prefix + "_renew1", "000000"));

        OffsetDateTime stored = jdbcTemplate.queryForObject("SELECT expire_at FROM saved_bottles WHERE id = ?", OffsetDateTime.class, bottle.id());
        assertThat(stored.toInstant()).isEqualTo(expireAt.plus(Duration.ofDays(30)));
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT extension_days, renewal_count, max_renewals, limit_config_version, days_config_version, operator_id,
                       operation_source, confirmation_method, status, phone_hash, reviewed_by
                  FROM saved_bottle_renewals WHERE id = ?
                """, renewal.id());
        assertThat(row.get("extension_days")).isEqualTo(30);
        assertThat(row.get("renewal_count")).isEqualTo(1);
        assertThat(row.get("max_renewals")).isEqualTo(2);
        assertThat(row.get("limit_config_version")).isEqualTo("global:v1");
        assertThat(row.get("days_config_version")).isEqualTo("global:v1");
        assertThat(row.get("operator_id")).isEqualTo(prefix + "_customer");
        assertThat(row.get("operation_source")).isEqualTo("customer_self");
        assertThat(row.get("confirmation_method")).isEqualTo("sms");
        assertThat(row.get("status")).isEqualTo("effective");
        assertThat(row.get("phone_hash")).isEqualTo(sha256(HOLDER_PHONE));
        assertThat(row.get("reviewed_by")).isNull();

        SavedBottleRenewalRecord pending = inventoryService.renewSavedBottle(
                bottle.id(), new SavedBottleRenewalRequest(prefix + "_renew2", "员工代办", "000000"));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM saved_bottle_renewals WHERE id = ?", String.class, pending.id()))
                .isEqualTo("pending_review");
        assertThat(jdbcTemplate.queryForObject("SELECT renewal_count FROM saved_bottle_renewals WHERE id = ?", Integer.class, pending.id()))
                .isEqualTo(2);
        OffsetDateTime unchanged = jdbcTemplate.queryForObject("SELECT expire_at FROM saved_bottles WHERE id = ?", OffsetDateTime.class, bottle.id());
        assertThat(unchanged.toInstant()).isEqualTo(expireAt.plus(Duration.ofDays(30)));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE saved_bottle_renewals SET status = 'bogus' WHERE id = ?", pending.id()))
                .isInstanceOf(DataIntegrityViolationException.class);

        // 营销主管驳回：rejected + 复核人 + 原因落库、到期日不动；客人本人随后可再转存，被驳回的代办不计次数（累计仍为 2）。
        TestSecurityActors.asPlatformAdmin("usr_mk_sup");
        SavedBottleRenewalRecord rejected = inventoryService.reviewSavedBottleRenewal(pending.id(),
                new SavedBottleRenewalRequest(prefix + "_rv", null, null, "reject", "营销主管不认可"));
        assertThat(rejected.status()).isEqualTo("rejected");
        Map<String, Object> rejectedRow = jdbcTemplate.queryForMap(
                "SELECT status, reviewed_by, review_reason FROM saved_bottle_renewals WHERE id = ?", pending.id());
        assertThat(rejectedRow.get("status")).isEqualTo("rejected");
        assertThat(rejectedRow.get("reviewed_by")).isEqualTo("usr_mk_sup");
        assertThat(rejectedRow.get("review_reason")).isEqualTo("营销主管不认可");
        assertThat(jdbcTemplate.queryForObject("SELECT expire_at FROM saved_bottles WHERE id = ?", OffsetDateTime.class, bottle.id()).toInstant())
                .isEqualTo(expireAt.plus(Duration.ofDays(30)));
        TestSecurityActors.asPlatformAdmin();
        SavedBottleRenewalRecord second = inventoryService.renewSavedBottleByCustomer(
                bottle.id(), prefix + "_customer", HOLDER_PHONE, new CustomerSavedBottleRenewRequest(prefix + "_renew3", "000000"));
        assertThat(second.renewalCount()).isEqualTo(2);
        // 驳回必须带原因（V513 chk_saved_bottle_renewals_reject_reason）。
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE saved_bottle_renewals SET status = 'rejected', review_reason = NULL WHERE id = ?", second.id()))
                .isInstanceOf(DataIntegrityViolationException.class);
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
