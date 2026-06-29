package com.complex.platform.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
@Transactional
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private MutableSmsSender smsSender;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void reset() {
        jdbcTemplate.execute("TRUNCATE notification_send_log");
        jdbcTemplate.update("DELETE FROM idempotency_records WHERE operation_id = 'notification.sms.notify'");
        smsSender.reset();
        setSwitch("sms_notification", false);
        setSwitch("sms.notify.stored_wine", true);
        setSwitch("sms.notify.stored_wine.deposit", true);
    }

    @Test
    void mainSwitchOffWritesSkippedLogWithoutSending() {
        NotificationSendLogRecord result = notifyStoredWineDeposit("sms-main-off");

        assertThat(result.status()).isEqualTo("skipped_switch_off");
        assertThat(result.reason()).isEqualTo("switch_off:sms_notification");
        assertThat(smsSender.sendCount()).isZero();
        assertThat(logCount("sms-main-off")).isEqualTo(1);
    }

    @Test
    void allSwitchesOnSendsSmsAndWritesSentLog() {
        setSwitch("sms_notification", true);

        NotificationSendLogRecord result = notifyStoredWineDeposit("sms-all-on");

        assertThat(result.status()).isEqualTo("sent");
        assertThat(result.providerRef()).isEqualTo("test-sms-sms-all-on");
        assertThat(result.reason()).isNull();
        assertThat(smsSender.sendCount()).isEqualTo(1);
        assertThat(logCount("sms-all-on")).isEqualTo(1);
    }

    @Test
    void duplicateRequestIdReturnsExistingLogAndDoesNotSendTwice() {
        setSwitch("sms_notification", true);

        NotificationSendLogRecord first = notifyStoredWineDeposit("sms-dup");
        NotificationSendLogRecord replay = notifyStoredWineDeposit("sms-dup");

        assertThat(replay).isEqualTo(first);
        assertThat(smsSender.sendCount()).isEqualTo(1);
        assertThat(logCount("sms-dup")).isEqualTo(1);
    }

    @Test
    void senderFailureWritesFailedLog() {
        setSwitch("sms_notification", true);
        smsSender.fail("provider rejected template");

        NotificationSendLogRecord result = notifyStoredWineDeposit("sms-failed");

        assertThat(result.status()).isEqualTo("failed");
        assertThat(result.reason()).isEqualTo("provider rejected template");
        assertThat(result.providerRef()).isNull();
        assertThat(smsSender.sendCount()).isEqualTo(1);
        assertThat(logCount("sms-failed")).isEqualTo(1);
    }

    private NotificationSendLogRecord notifyStoredWineDeposit(String requestId) {
        return notificationService.notify(
                "stored_wine",
                "deposit",
                "+15551234567",
                "stored_wine_deposit",
                Map.of("ticket_no", "SW-001"),
                requestId
        );
    }

    private void setSwitch(String capabilityCode, boolean enabled) {
        jdbcTemplate.update("""
                INSERT INTO terminal_capability_switches (
                    id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at
                )
                VALUES (?, '*', ?, 'global', 'global', ?, 'notification service test switch override', now(), now())
                ON CONFLICT (terminal_type, capability_code, scope_type, scope_id) DO UPDATE SET
                    enabled = EXCLUDED.enabled,
                    reason = EXCLUDED.reason,
                    updated_at = now()
                """, idFor(capabilityCode), capabilityCode, enabled);
    }

    private String idFor(String capabilityCode) {
        return "test_tcs_" + capabilityCode.replace(".", "_");
    }

    private int logCount(String requestId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM notification_send_log WHERE request_id = ?",
                Integer.class,
                requestId
        );
        return count == null ? 0 : count;
    }

    @TestConfiguration
    static class SmsSenderConfig {

        @Bean
        @Primary
        MutableSmsSender mutableSmsSender() {
            return new MutableSmsSender();
        }
    }

    static class MutableSmsSender implements SmsSender {

        private int sendCount;
        private String status = "sent";
        private String reason;

        @Override
        public SmsSendResult send(SmsMessage message) {
            sendCount++;
            if ("sent".equals(status)) {
                return new SmsSendResult("sent", "test-sms-" + message.requestId(), null);
            }
            return new SmsSendResult(status, null, reason);
        }

        void reset() {
            sendCount = 0;
            status = "sent";
            reason = null;
        }

        void fail(String reason) {
            this.status = "failed";
            this.reason = reason;
        }

        int sendCount() {
            return sendCount;
        }
    }
}
