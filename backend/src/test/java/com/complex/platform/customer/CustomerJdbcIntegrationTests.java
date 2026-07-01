package com.complex.platform.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import com.complex.platform.bar.BarReservationRecord;
import com.complex.platform.customer.dto.CustomerPhoneLoginRequest;
import com.complex.platform.bar.BarTableRecord;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
@Transactional
class CustomerJdbcIntegrationTests {

    @Autowired
    private JdbcCustomerRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CustomerService customerService;

    @Test
    void phoneLoginPersistsNonLeakingCustomerIdToRealPostgres() {
        // 真实后端活证据：新客人经真实 CustomerService.phoneLogin 落真实 Postgres，主键不得内嵌明文手机号。
        String phone = "13655667788";
        CustomerLoginResponse response = customerService.phoneLogin(
                new CustomerPhoneLoginRequest(phone, "000000", "miniapp"));

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM customers WHERE id = ?", Integer.class, response.customerId());
        assertThat(rows).isEqualTo(1);                                      // 确已落真实库
        assertThat(response.customerId()).doesNotContain(phone);           // 主键不含明文手机号
        assertThat(response.customerId()).isNotEqualTo("customer_" + phone); // 非旧的可反推方案
    }

    @Test
    void v191GuardRekeysLeakingCustomerIdAndCascadesReferences() throws Exception {
        // 兜底自检活证据：历史遗留 customer_<手机号> 主键，V191 重键为随机不可反推 id 并级联子行。
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        String leakingId = "customer_13900001111";
        repository.saveCustomer(new CustomerRecord(
                leakingId, "13900001111", "139****1111", "active", null, now, now, now));
        repository.saveSession(new CustomerSessionRecord(
                "session_leakguard", leakingId, "cust_leakguard", "miniapp", now, now.plusSeconds(86400), "active"));

        // 执行 V191 迁移 SQL（守卫式，可安全重跑）
        String sql = new String(new ClassPathResource(
                "db/migration/V191__customer_id_phone_leak_guard.sql").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        jdbcTemplate.execute(sql);

        // 泄露主键已消失
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM customers WHERE id = ?", Integer.class, leakingId)).isZero();
        // 同一手机号（盲索引）仍可定位，新主键与手机号无关且不可反推
        String newId = repository.findCustomerByPhone("13900001111").orElseThrow().id();
        assertThat(newId).startsWith("customer_");
        assertThat(newId).isNotEqualTo(leakingId);
        assertThat(newId).doesNotContain("13900001111");
        // 外键子行（会话）已级联改指到新主键
        assertThat(jdbcTemplate.queryForObject(
                "SELECT customer_id FROM customer_sessions WHERE id = 'session_leakguard'", String.class))
                .isEqualTo(newId);
    }

    @Test
    void migrationCreatesCustomerTables() {
        assertThat(regclass("customers")).isEqualTo("customers");
        assertThat(regclass("customer_sessions")).isEqualTo("customer_sessions");
        assertThat(regclass("customer_consents")).isEqualTo("customer_consents");
        assertThat(regclass("customer_points_accounts")).isEqualTo("customer_points_accounts");
        assertThat(regclass("customer_points_ledger")).isEqualTo("customer_points_ledger");
        assertThat(regclass("customer_redemption_catalog_items")).isEqualTo("customer_redemption_catalog_items");
        assertThat(regclass("customer_points_redemptions")).isEqualTo("customer_points_redemptions");
        assertThat(regclass("customer_points_rules")).isEqualTo("customer_points_rules");
        assertThat(regclass("bar_tables")).isEqualTo("bar_tables");
        assertThat(regclass("bar_reservations")).isEqualTo("bar_reservations");
        assertThat(currentFlywayVersion()).isEqualTo(com.complex.platform.support.MigrationVersions.latest());
    }

    @Test
    void customerPhonePersistedAsCiphertextWithBlindIndexAndPlaintextLookupWorks() {
        // 1.2 真实后端活证据：手机号密文落库（enc:v1:）、盲索引 64 hex、明文仍可定位、同号唯一冲突落 hash 列。
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveCustomer(new CustomerRecord(
                "customer_pii_live", "13712340001", "137****0001", "active", null, now, now, now));

        // DB 层：phone 是密文信封、不含明文；phone_hash 是 64 位十六进制。
        String storedPhone = jdbcTemplate.queryForObject(
                "SELECT phone FROM customers WHERE id = 'customer_pii_live'", String.class);
        assertThat(storedPhone).startsWith("enc:v1:");
        assertThat(storedPhone).doesNotContain("13712340001");
        String storedHash = jdbcTemplate.queryForObject(
                "SELECT phone_hash FROM customers WHERE id = 'customer_pii_live'", String.class);
        assertThat(storedHash).matches("[0-9a-f]{64}");

        // 应用层：按明文手机号仍能定位，读回解密为明文。
        assertThat(repository.findCustomerByPhone("13712340001"))
                .get().extracting(CustomerRecord::id, CustomerRecord::phone)
                .containsExactly("customer_pii_live", "13712340001");

        // 唯一性：同号不同 id → 盲索引唯一约束拒（钱/账不会因手机号明文改造而重复建号）。
        assertThatThrownBy(() -> repository.saveCustomer(new CustomerRecord(
                "customer_pii_dup", "13712340001", "137****0001", "active", null, now, now, now)))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void listPointsAccountCustomerIdsAfterPaginatesByKeyset() {
        // 3.14 真实库活证据：keyset 分页按 customer_id 游标有序返回下一批。
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        String[] cids = {"cust_k_a", "cust_k_b", "cust_k_c"};
        String[] phones = {"13600000021", "13600000022", "13600000023"};
        for (int i = 0; i < cids.length; i++) {
            repository.saveCustomer(new CustomerRecord(cids[i], phones[i], "mask", "active", null, now, now, now));
            repository.savePoints(new CustomerPointsAccountRecord("pa_" + cids[i], cids[i], 0L, "base", Map.of(),
                    0L, "active", now, 0L, null, null, 0, now, now));
        }
        assertThat(repository.listPointsAccountCustomerIdsAfter(null, 2)).hasSize(2);
        assertThat(repository.listPointsAccountCustomerIdsAfter(null, 100))
                .contains("cust_k_a", "cust_k_b", "cust_k_c");
        assertThat(repository.listPointsAccountCustomerIdsAfter("cust_k_a", 100))
                .contains("cust_k_b", "cust_k_c").doesNotContain("cust_k_a");
    }

    @Test
    void customersStatusCheckAllowsFourLifecycleStatesAndRejectsOthers() {
        // 3.5（用户最终拍板四态）：active/suspended/blacklisted/deregistered 放行、未知状态被库层拒。
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveCustomer(new CustomerRecord(
                "customer_status_active", "13611110000", "136****0000", "active", null, now, now, now));
        repository.saveCustomer(new CustomerRecord(
                "customer_status_suspended", "13611110002", "136****0002", "suspended", null, now, now, now));
        repository.saveCustomer(new CustomerRecord(
                "customer_status_blacklisted", "13611110003", "136****0003", "blacklisted", null, now, now, now));
        repository.saveCustomer(new CustomerRecord(
                "customer_status_deregistered", "13611110004", "136****0004", "deregistered", null, now, now, now));
        // 未知/已废弃状态仍被 CHECK 拒（放最后：约束违反会中止本 @Transactional 事务，其后不可再查库）。
        assertThatThrownBy(() -> repository.saveCustomer(new CustomerRecord(
                "customer_status_bad", "13611110001", "136****0001", "banned", null, now, now, now)))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void pointsAccountBalanceCannotGoNegativeAtDatabaseLevel() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveCustomer(new CustomerRecord(
                "customer_neg", "13900139000", "139****9000", "active", null, now, now, now));

        // The V054 CHECK backstop must reject a negative balance even if a writer bypasses the service guard.
        assertThatThrownBy(() -> repository.savePoints(new CustomerPointsAccountRecord(
                "points_neg", "customer_neg", -100L, "base", Map.of(), 0L, "active", now, 0L, null, null, 0, now, now)))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void merchantScopedAndProjectStoredValueCardsCoexistForSameCustomer() {
        // B1d⑤ 储值卡分级（V175）：同一会员可同时持有「综合体通用卡」(merchant_id NULL) 与「商户级卡」(merchant_id 非空)，
        // 卡范围唯一键=(customer, project, merchant)；通用卡查找不得命中商户级卡，商户级卡按 merchant 精确命中。
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveCustomer(new CustomerRecord(
                "cust_sv_scope", "13900130001", "139****0001", "active", null, now, now, now));

        repository.saveStoredValueAccount(new CustomerStoredValueAccountRecord(
                "svacc_project", "cust_sv_scope", "prj_1", null, 5000L, "active", now, now));
        repository.saveStoredValueAccount(new CustomerStoredValueAccountRecord(
                "svacc_merchant", "cust_sv_scope", "prj_1", "merchant_x", 3000L, "active", now, now));

        // 通用卡查找（无 merchant）只命中 merchant_id IS NULL 的卡。
        assertThat(repository.findStoredValueAccount("cust_sv_scope", "prj_1")).get()
                .extracting(CustomerStoredValueAccountRecord::id, CustomerStoredValueAccountRecord::merchantId,
                        CustomerStoredValueAccountRecord::cardScope)
                .containsExactly("svacc_project", null, "project");

        // 商户级卡按 merchant 精确命中，与通用卡互不串。
        assertThat(repository.findStoredValueAccount("cust_sv_scope", "prj_1", "merchant_x")).get()
                .extracting(CustomerStoredValueAccountRecord::id, CustomerStoredValueAccountRecord::cardScope)
                .containsExactly("svacc_merchant", "merchant");
        assertThat(repository.findStoredValueAccount("cust_sv_scope", "prj_1", "merchant_other")).isEmpty();
    }

    @Test
    void customerPointsRulePersistsAndReadsBack() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.savePointsRule(new CustomerPointsRuleRecord(
                "rule_1", "prj_1", "消费积分", "consume_amount", 1L, "{\"silver\":1000}",
                "fixed_days", 365, "fifo_batch", null, "active", now, now));

        assertThat(repository.findPointsRuleById("rule_1")).get()
                .extracting(CustomerPointsRuleRecord::earnMethod, CustomerPointsRuleRecord::validityType,
                        CustomerPointsRuleRecord::status)
                .containsExactly("consume_amount", "fixed_days", "active");
        assertThat(repository.listPointsRules())
                .extracting(CustomerPointsRuleRecord::id)
                .contains("rule_1");
        // P2 多综合体 project_id scoped 读通道：按 project_id 行级隔离。
        assertThat(repository.listPointsRulesByProject("prj_1"))
                .extracting(CustomerPointsRuleRecord::id)
                .contains("rule_1");
        assertThat(repository.listPointsRulesByProject("prj_nonexistent")).isEmpty();
    }

    @Test
    void customerPointsRuleRejectsInvalidValidityTypeAtDatabaseLevel() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        assertThatThrownBy(() -> repository.savePointsRule(new CustomerPointsRuleRecord(
                "rule_bad", null, "bad", "consume_amount", 1L, "{}", "calendar_year", null,
                "fifo_batch", null, "draft", now, now)))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void persistCustomerSessionConsentAndPoints() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveCustomer(new CustomerRecord(
                "customer_jdbc",
                "13800138000",
                "138****8000",
                "active",
                java.time.LocalDate.of(1990, 6, 14),
                now,
                now,
                now
        ));
        repository.saveSession(new CustomerSessionRecord(
                "session_jdbc",
                "customer_jdbc",
                "cust_jdbc",
                "miniapp",
                now,
                now.plusSeconds(86400),
                "active"
        ));
        repository.saveConsent(new CustomerConsentRecord(
                "consent_jdbc",
                "customer_jdbc",
                "privacy",
                true,
                "v1",
                "miniapp",
                now,
                null,
                now,
                now
        ));
        repository.savePoints(new CustomerPointsAccountRecord(
                "points_jdbc",
                "customer_jdbc",
                120L,
                "silver",
                Map.of("last", "ticket_order"),
                0L,
                "active",
                now,
                0L,
                null,
                null,
                0,
                now,
                now
        ));

        assertThat(repository.findCustomerByPhone("13800138000"))
                .get()
                .extracting(CustomerRecord::maskedPhone)
                .isEqualTo("138****8000");
        assertThat(repository.findSessionByToken("cust_jdbc"))
                .get()
                .extracting(CustomerSessionRecord::customerId)
                .isEqualTo("customer_jdbc");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT customer_token FROM customer_sessions WHERE id = 'session_jdbc'",
                String.class
        )).isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT customer_token_hash FROM customer_sessions WHERE id = 'session_jdbc'", String.class))
                .isEqualTo(sha256("cust_jdbc"));
        assertThat(repository.findPointsByCustomerId("customer_jdbc"))
                .get()
                .extracting(CustomerPointsAccountRecord::balance)
                .isEqualTo(120L);
    }

    @Test
    void customerRepositoryListsActiveBarReservationOptionsAndCreatesReservation() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        jdbcTemplate.update("""
                INSERT INTO bar_tables (
                    id, bar_id, table_code, table_name, table_type, capacity,
                    allowed_pass_free_guest_count, min_consumption_cent, currency,
                    status, created_at, updated_at
                )
                VALUES (
                    'table_customer_jdbc', 'bar_customer_jdbc', 'T-CUSTOMER-JDBC', 'VIP Customer JDBC',
                    'card_seat', 6, 2, 50000, 'CNY', 'active', now(), now()
                )
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """);

        BarReservationRecord reservation = repository.saveCustomerBarReservation(new BarReservationRecord(
                "reservation_customer_jdbc",
                "bar_customer_jdbc",
                null,
                "table_customer_jdbc",
                "customer_jdbc",
                now.plusSeconds(3600),
                2,
                2,
                0L,
                "not_required",
                "miniapp",
                null,
                "reserved",
                "miniapp",
                null,
                java.util.List.of(),
                now,
                now,
                null
        ));

        assertThat(repository.listBarReservationOptions())
                .extracting(CustomerBarReservationOptionResponse::barId, CustomerBarReservationOptionResponse::tableId, CustomerBarReservationOptionResponse::allowedPassFreeGuestCount)
                .contains(tuple("bar_customer_jdbc", "table_customer_jdbc", 2));
        assertThat(repository.findActiveBarTableForReservation("bar_customer_jdbc", "table_customer_jdbc", "card_seat"))
                .get()
                .extracting(BarTableRecord::tableName)
                .isEqualTo("VIP Customer JDBC");
        assertThat(reservation.customerId()).isEqualTo("customer_jdbc");
    }

    @Test
    void persistPointsLedgerRedemptionCatalogAndRedemptions() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveCustomer(new CustomerRecord(
                "customer_points_jdbc",
                "13900139000",
                "139****9000",
                "active",
                null,
                now,
                now,
                now
        ));
        repository.savePoints(new CustomerPointsAccountRecord(
                "points_points_jdbc",
                "customer_points_jdbc",
                380L,
                "silver",
                Map.of("last_redemption_id", "redemption_jdbc"),
                0L,
                "active",
                now,
                0L,
                null,
                null,
                0,
                now,
                now
        ));
        repository.savePointsLedger(new CustomerPointsLedgerRecord(
                "ledger_points_jdbc",
                "customer_points_jdbc",
                -120L,
                380L,
                "points_redemption",
                "redemption_jdbc",
                "Redeemed Plush Gift",
                null,
                null,
                false,
                now
        ));
        repository.saveRedemptionCatalogItem(new CustomerRedemptionCatalogItemRecord(
                "gift_points_jdbc",
                "Plush Gift JDBC",
                "gift",
                120L,
                "item_gift_jdbc",
                "gift_shelf_jdbc",
                1,
                "active",
                now.minusSeconds(60),
                now.plusSeconds(3600),
                now,
                now
        ));
        repository.saveRedemptionCatalogItem(new CustomerRedemptionCatalogItemRecord(
                "gift_inactive_jdbc",
                "Inactive Gift JDBC",
                "gift",
                100L,
                "item_inactive_jdbc",
                "gift_shelf_jdbc",
                1,
                "inactive",
                null,
                null,
                now,
                now
        ));
        repository.savePointsRedemption(new CustomerPointsRedemptionRecord(
                "redemption_jdbc",
                "customer_points_jdbc",
                "gift_points_jdbc",
                "Plush Gift JDBC",
                1,
                120L,
                120L,
                "item_gift_jdbc",
                "gift_shelf_jdbc",
                1,
                "miniapp",
                "pending_pickup",
                now,
                now
        ));

        assertThat(repository.listPointsLedgerByCustomerId("customer_points_jdbc"))
                .extracting(CustomerPointsLedgerRecord::id, CustomerPointsLedgerRecord::pointsDelta, CustomerPointsLedgerRecord::balanceAfter)
                .containsExactly(tuple("ledger_points_jdbc", -120L, 380L));
        assertThat(repository.listActiveRedemptionCatalogItems(now))
                .extracting(CustomerRedemptionCatalogItemRecord::id)
                .contains("gift_points_jdbc")
                .doesNotContain("gift_inactive_jdbc");
        assertThat(repository.findRedemptionCatalogItemById("gift_points_jdbc"))
                .get()
                .extracting(CustomerRedemptionCatalogItemRecord::inventoryItemId)
                .isEqualTo("item_gift_jdbc");
        assertThat(repository.listPointsRedemptionsByCustomerId("customer_points_jdbc"))
                .extracting(CustomerPointsRedemptionRecord::id, CustomerPointsRedemptionRecord::status)
                .containsExactly(tuple("redemption_jdbc", "pending_pickup"));
    }

    private String regclass(String tableName) {
        return jdbcTemplate.queryForObject("SELECT to_regclass('public." + tableName + "')::text", String.class);
    }

    private String currentFlywayVersion() {
        return jdbcTemplate.queryForObject("""
                SELECT version
                  FROM flyway_schema_history
                 WHERE success = true
                 ORDER BY installed_rank DESC
                 LIMIT 1
                """, String.class);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
