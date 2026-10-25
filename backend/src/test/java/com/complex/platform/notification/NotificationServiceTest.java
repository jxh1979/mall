package com.complex.platform.notification;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ScaffoldSmsSender smsSender;

    @Autowired
    private JdbcNotificationRepository notificationRepository;

    @Autowired
    private NotificationSwitchResolver switchResolver;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void reset() {
        jdbcTemplate.execute("TRUNCATE notification_send_log");
        // M724：行键不含提交范围——IdempotencyService.storedOperationId(操作号)
        jdbcTemplate.update("DELETE FROM idempotency_records WHERE operation_id = ?",
                com.complex.platform.common.idempotency.IdempotencyService.storedOperationId("notification.sms.notify"));
        smsSender.reset();
        setSwitch("sms_notification", false);
        setSwitch("sms.notify.stored_wine", true);
        setSwitch("sms.notify.stored_wine.deposit", true);
    }

    @AfterEach
    void resetSharedSender() {
        // 替身是测试上下文共享的单例：复位，免得失败模式漏到复用同一上下文的其它测试类。
        smsSender.reset();
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
        assertThat(result.providerRef()).isEqualTo("scaffold-sms-sms-all-on");
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

    @Test
    void notificationServiceWithUnconfiguredSenderWritesFailedLogAndFailureAudit() {
        // ROOT-05 R1：生产发送器（未接短信供应商）下，总开关开也只记失败，不出现「已发送」。
        setSwitch("sms_notification", true);
        NotificationService unconfigured = new NotificationService(
                notificationRepository, switchResolver, new UnconfiguredSmsSender(), idempotencyService, auditEventService);

        NotificationSendLogRecord result = unconfigured.notify(
                "stored_wine", "deposit", "+15551234567", "stored_wine_deposit",
                Map.of("ticket_no", "SW-002"), "sms-unconfigured");

        assertThat(result.status()).isEqualTo("failed");
        assertThat(result.reason()).isEqualTo("SMS_PROVIDER_NOT_CONFIGURED");
        assertThat(result.providerRef()).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM notification_send_log WHERE request_id = ?", String.class, "sms-unconfigured"))
                .isEqualTo("failed");
        assertThat(auditCount("sms.notification.failed", result.id())).isEqualTo(1);
        assertThat(auditCount("sms.notification.sent", result.id())).isZero();
    }

    @Test
    void mandatoryEventIgnoresMainSwitchAndFailsHonestlyWithUnconfiguredSender() {
        // OBS-13-04（BSM 第 1598 行储值充值到账等客人金额变化通知不在可关闭范围内）：总开关关（出厂即关）也不记跳过，
        // 照常交给发送器；未接短信供应商时如实记失败。
        NotificationService unconfigured = new NotificationService(
                notificationRepository, switchResolver, new UnconfiguredSmsSender(), idempotencyService, auditEventService);

        NotificationSendLogRecord result = unconfigured.notify(
                "stored_value", "topup", "+15551234567", "sms.stored_value.topup",
                Map.of("amount_cent", 5000), "sms-mandatory-topup");

        assertThat(result.status()).isEqualTo("failed");
        assertThat(result.reason()).isEqualTo("SMS_PROVIDER_NOT_CONFIGURED");
        assertThat(auditCount("sms.notification.failed", result.id())).isEqualTo(1);
        assertThat(auditCount("sms.notification.skipped", result.id())).isZero();
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

    private int auditCount(String eventName, String businessId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events WHERE event_name = ? AND business_id = ?",
                Integer.class,
                eventName,
                businessId
        );
        return count == null ? 0 : count;
    }
}
