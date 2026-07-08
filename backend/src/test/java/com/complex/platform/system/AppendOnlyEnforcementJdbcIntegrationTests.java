package com.complex.platform.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * V140 append-only 防篡改硬强制验证：审计/不可变表的 UPDATE/DELETE 必须被 DB 触发器拦截，
 * 永久留痕表的 DELETE 必须被拦截。覆盖"对抗审计 P0 #1（审计可被篡改/删除）"。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
@Transactional
class AppendOnlyEnforcementJdbcIntegrationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID insertAuditEvent() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO audit_events (id, occurred_at, event_name, event_result) VALUES (?, ?, ?, ?)",
                id, Timestamp.from(Instant.now()), "test.append_only", "success");
        return id;
    }

    @Test
    void auditEventUpdateIsBlockedByTrigger() {
        UUID id = insertAuditEvent();
        assertThatThrownBy(() ->
                jdbcTemplate.update("UPDATE audit_events SET event_result = ? WHERE id = ?", "tampered", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("COMPLEX_APPEND_ONLY_VIOLATION");
    }

    @Test
    void auditEventDeleteIsBlockedByTrigger() {
        UUID id = insertAuditEvent();
        assertThatThrownBy(() ->
                jdbcTemplate.update("DELETE FROM audit_events WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("COMPLEX_APPEND_ONLY_VIOLATION");
    }

    @Test
    void immutableTablesCarryUpdateDeleteTrigger() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_trigger t JOIN pg_class c ON c.oid = t.tgrelid "
                        + "WHERE t.tgname = 'trg_append_only_ud' AND c.relname IN "
                        + "('audit_events','evidence_events','customer_stored_value_transactions','deposit_ledger',"
                        + "'risk_alert_actions','ar_ap_payments')",
                Integer.class);
        assertThat(count).isEqualTo(8);
    }

    @Test
    void appendOnlyIntentTablesCarryDeleteTrigger() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_trigger t JOIN pg_class c ON c.oid = t.tgrelid "
                        + "WHERE t.tgname = 'trg_append_only_d' AND c.relname IN "
                        + "('ticket_payment_intents','finance_payment_intents','ticket_refund_lines',"
                        + "'inventory_stock_movements','customer_points_ledger','finance_daily_closes')",
                Integer.class);
        // P1-9（V149）：日结锁表 finance_daily_closes 纳入 delete_only，故由 5 → 6。
        assertThat(count).isEqualTo(6);
    }
}
