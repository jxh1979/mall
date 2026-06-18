package com.complex.platform.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import com.complex.platform.bar.BarReservationRecord;
import com.complex.platform.bar.BarTableRecord;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
    void pointsAccountBalanceCannotGoNegativeAtDatabaseLevel() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveCustomer(new CustomerRecord(
                "customer_neg", "13900139000", "139****9000", "active", now, now, now));

        // The V054 CHECK backstop must reject a negative balance even if a writer bypasses the service guard.
        assertThatThrownBy(() -> repository.savePoints(new CustomerPointsAccountRecord(
                "points_neg", "customer_neg", -100L, "base", Map.of(), now, now)))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
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
