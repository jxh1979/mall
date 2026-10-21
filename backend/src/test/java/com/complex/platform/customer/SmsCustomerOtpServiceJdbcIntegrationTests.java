package com.complex.platform.customer;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.crypto.BlindIndex;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.notification.SmsMessage;
import com.complex.platform.notification.SmsSendResult;
import com.complex.platform.notification.SmsSender;
import com.complex.platform.notification.UnconfiguredSmsSender;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ROOT-05-R3：平台自有短信验证码签发、留存与核验（BSM 第 1563 行「验证码下发与送达结果、失败原因（用于新人判定、注册登录和关键操作验证）」；
 * 第 1564 行「不自研短信通道。」）。被测对象直接构造（生产环境才装配），短信发送器用捕获型替身或未配置发送器；
 * 每次调用包在外层事务里，模拟调用方因 401/409 回滚，断言发码留存与错误计数仍独立提交。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SmsCustomerOtpServiceJdbcIntegrationTests {

    private static final String SCENE = "customer.login";

    @Autowired private NamedParameterJdbcTemplate namedJdbc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BlindIndex blindIndex;
    @Autowired private AuditEventService auditEventService;
    @Autowired private PlatformTransactionManager transactionManager;

    private final String phone = "139" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    private final Instant now = Instant.now();
    private final CapturingSmsSender sender = new CapturingSmsSender();

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM customer_otp_codes WHERE phone_index = ?", blindIndex.hash(phone));
    }

    private CustomerOtpVerifier subject(SmsSender smsSender, Clock clock) {
        return new SmsCustomerOtpService(namedJdbc, blindIndex, smsSender, auditEventService, transactionManager, clock);
    }

    private CustomerOtpVerifier subject() {
        return subject(sender, Clock.fixed(now, ZoneOffset.UTC));
    }

    /** 调用方事务：异常时整笔回滚（与登录、储值付款、存酒转让的外层事务同口径）。 */
    private void inCallerTransaction(Runnable action) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> action.run());
    }

    private List<Map<String, Object>> rows() {
        return jdbc.queryForList("SELECT * FROM customer_otp_codes WHERE phone_index = ? ORDER BY created_at, id",
                blindIndex.hash(phone));
    }

    private List<Map<String, Object>> audits(String eventName, Object businessId) {
        return jdbc.queryForList(
                "SELECT event_result, metadata::text AS metadata FROM audit_events WHERE event_name = ? AND business_id = ?",
                eventName, businessId);
    }

    private static void assertInvalid(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo("CUSTOMER_OTP_INVALID"));
    }

    @Test
    void requestedCodeVerifiesOnceThenRejectsReuse() {
        CustomerOtpVerifier otp = subject();
        assertThatCode(() -> inCallerTransaction(() -> otp.requestCode(phone, SCENE))).doesNotThrowAnyException();
        String code = sender.lastCode();
        assertThat(code).matches("\\d{6}");
        assertThat(sender.last.recipient()).isEqualTo(phone);

        assertThatCode(() -> inCallerTransaction(() -> otp.verify(phone, code, SCENE))).doesNotThrowAnyException();
        assertInvalid(() -> inCallerTransaction(() -> otp.verify(phone, code, SCENE)));
        assertThat(rows()).singleElement().satisfies(row -> {
            assertThat(row.get("send_status")).isEqualTo("sent");
            assertThat(row.get("consumed_at")).isNotNull();
            assertThat(audits("customer.otp_requested", row.get("id"))).singleElement().satisfies(audit -> {
                assertThat(audit.get("event_result")).isEqualTo("success");
                assertThat((String) audit.get("metadata")).doesNotContain(phone).doesNotContain(code);
            });
        });
    }

    @Test
    void wrongCodeFiveTimesInvalidatesCodeEvenWhenCallerRollsBack() {
        CustomerOtpVerifier otp = subject();
        assertThatCode(() -> inCallerTransaction(() -> otp.requestCode(phone, SCENE))).doesNotThrowAnyException();
        String code = sender.lastCode();
        String wrong = code.equals("000000") ? "111111" : "000000";
        for (int attempt = 1; attempt <= 5; attempt++) {
            assertInvalid(() -> inCallerTransaction(() -> otp.verify(phone, wrong, SCENE)));
            assertThat(rows()).singleElement().extracting(row -> row.get("failed_attempts")).isEqualTo(attempt);
        }
        assertThat(rows()).singleElement().extracting(row -> row.get("invalidated_at")).isNotNull();
        assertInvalid(() -> inCallerTransaction(() -> otp.verify(phone, code, SCENE)));
    }

    @Test
    void expiredCodeIsRejected() {
        assertThatCode(() -> inCallerTransaction(() -> subject().requestCode(phone, SCENE))).doesNotThrowAnyException();
        String code = sender.lastCode();
        CustomerOtpVerifier later = subject(sender, Clock.fixed(now.plus(Duration.ofMinutes(5)).plusSeconds(1), ZoneOffset.UTC));
        assertInvalid(() -> inCallerTransaction(() -> later.verify(phone, code, SCENE)));
        assertThat(rows()).singleElement().extracting(row -> row.get("consumed_at")).isNull();
    }

    @Test
    void newRequestInvalidatesPreviousCode() {
        CustomerOtpVerifier otp = subject();
        assertThatCode(() -> inCallerTransaction(() -> otp.requestCode(phone, SCENE))).doesNotThrowAnyException();
        String first = sender.lastCode();
        inCallerTransaction(() -> otp.requestCode(phone, SCENE));
        String second = sender.lastCode();

        List<Map<String, Object>> rows = rows();
        assertThat(rows).hasSize(2);
        assertThat(rows).filteredOn(row -> row.get("invalidated_at") != null).hasSize(1);
        if (!first.equals(second)) {
            assertInvalid(() -> inCallerTransaction(() -> otp.verify(phone, first, SCENE)));
        }
        assertThatCode(() -> inCallerTransaction(() -> otp.verify(phone, second, SCENE))).doesNotThrowAnyException();
    }

    @Test
    void unconfiguredProviderKeepsFailedSendRecordAndThrowsProviderUnavailable() {
        CustomerOtpVerifier otp = subject(new UnconfiguredSmsSender(), Clock.fixed(now, ZoneOffset.UTC));
        assertThatThrownBy(() -> inCallerTransaction(() -> otp.requestCode(phone, SCENE)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("CUSTOMER_OTP_PROVIDER_UNAVAILABLE");
                    assertThat(e.status().value()).isEqualTo(409);
                });
        assertThat(rows()).singleElement().satisfies(row -> {
            assertThat(row.get("send_status")).isEqualTo("failed");
            assertThat(row.get("send_reason")).isEqualTo("SMS_PROVIDER_NOT_CONFIGURED");
            assertThat(row.get("invalidated_at")).isNotNull();
            assertThat(audits("customer.otp_request_failed", row.get("id")))
                    .singleElement().extracting(audit -> audit.get("event_result")).isEqualTo("failure");
        });
        assertInvalid(() -> inCallerTransaction(() -> otp.verify(phone, "000000", SCENE)));
    }

    /** 重新取码被供应商频控时，已送达的旧码仍可用（只在新码成功下发后作废旧码）。 */
    @Test
    void throttledResendKeepsDeliveredCodeUsable() {
        CustomerOtpVerifier otp = subject();
        assertThatCode(() -> inCallerTransaction(() -> otp.requestCode(phone, SCENE))).doesNotThrowAnyException();
        String delivered = sender.lastCode();
        sender.status = "throttled";
        sender.reason = "SMS_THROTTLED";
        assertThatThrownBy(() -> inCallerTransaction(() -> otp.requestCode(phone, SCENE)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CUSTOMER_OTP_SEND_FAILED"));

        assertThat(rows()).extracting(row -> row.get("send_status")).containsExactlyInAnyOrder("sent", "throttled");
        assertThatCode(() -> inCallerTransaction(() -> otp.verify(phone, delivered, SCENE))).doesNotThrowAnyException();
    }

    /** 供应商返回其他失败原因：409 CUSTOMER_OTP_SEND_FAILED，留存 failed 记录与失败审计（外层回滚后仍在）。 */
    @Test
    void providerFailureKeepsFailedRecordAndThrowsSendFailed() {
        sender.status = "failed";
        sender.reason = "SMS_PROVIDER_REJECTED";
        CustomerOtpVerifier otp = subject();
        assertThatThrownBy(() -> inCallerTransaction(() -> otp.requestCode(phone, SCENE)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("CUSTOMER_OTP_SEND_FAILED");
                    assertThat(e.status().value()).isEqualTo(409);
                });
        assertThat(rows()).singleElement().satisfies(row -> {
            assertThat(row.get("send_status")).isEqualTo("failed");
            assertThat(row.get("send_reason")).isEqualTo("SMS_PROVIDER_REJECTED");
            assertThat(audits("customer.otp_request_failed", row.get("id")))
                    .singleElement().extracting(audit -> audit.get("event_result")).isEqualTo("failure");
        });
    }

    @Test
    void tableHoldsNoPlainPhoneOrCode() {
        assertThatCode(() -> inCallerTransaction(() -> subject().requestCode(phone, SCENE))).doesNotThrowAnyException();
        String code = sender.lastCode();
        String stored = jdbc.queryForObject(
                "SELECT string_agg(row_to_json(c)::text, '') FROM customer_otp_codes c WHERE phone_index = ?",
                String.class, blindIndex.hash(phone));
        assertThat(stored).isNotNull().doesNotContain(phone).doesNotContain("\"" + code + "\"");
        assertThat(rows()).singleElement().extracting(row -> row.get("code_hash")).isNotEqualTo(code);
    }

    /** 捕获型短信发送器：记录最后一条消息，按 status/reason 返回（缺省 sent）。 */
    private static final class CapturingSmsSender implements SmsSender {
        private SmsMessage last;
        private String status = "sent";
        private String reason;

        @Override
        public SmsSendResult send(SmsMessage message) {
            last = message;
            return new SmsSendResult(status, "sent".equals(status) ? "test-" + message.requestId() : null, reason);
        }

        String lastCode() {
            assertThat(last).as("短信发送器应收到验证码消息").isNotNull();
            return String.valueOf(last.params().get("code"));
        }
    }
}
