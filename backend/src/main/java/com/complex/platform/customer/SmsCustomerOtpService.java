package com.complex.platform.customer;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.crypto.BlindIndex;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.notification.SmsMessage;
import com.complex.platform.notification.SmsSendResult;
import com.complex.platform.notification.SmsSender;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 平台自有的客人短信验证码签发、留存与核验（ROOT-05-R3）。BSM 第 1563 行「验证码下发与送达结果、失败原因（用于新人判定、注册登录和关键操作验证）」；
 * 第 1564 行「不自研短信通道。」——下发只经 {@link SmsSender}（未接供应商时如实失败），签发、有效期、一次性消费与错误次数由平台负责。
 * 库里只存手机号盲索引与「id:验证码」盲索引，不存明文。发码留存与错误计数在独立事务提交：调用方随后因 409/401 回滚时仍留住。
 */
@Component
@Profile("prod")
public class SmsCustomerOtpService implements CustomerOtpVerifier {

    private static final Duration TTL = Duration.ofMinutes(5);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final NamedParameterJdbcTemplate jdbc;
    private final BlindIndex blindIndex;
    private final SmsSender smsSender;
    private final AuditEventService auditEventService;
    private final TransactionTemplate independent;
    private final Clock clock;

    @Autowired
    public SmsCustomerOtpService(NamedParameterJdbcTemplate jdbc, BlindIndex blindIndex, SmsSender smsSender,
            AuditEventService auditEventService, PlatformTransactionManager transactionManager) {
        this(jdbc, blindIndex, smsSender, auditEventService, transactionManager, Clock.systemUTC());
    }

    SmsCustomerOtpService(NamedParameterJdbcTemplate jdbc, BlindIndex blindIndex, SmsSender smsSender,
            AuditEventService auditEventService, PlatformTransactionManager transactionManager, Clock clock) {
        this.jdbc = jdbc;
        this.blindIndex = blindIndex;
        this.smsSender = smsSender;
        this.auditEventService = auditEventService;
        this.independent = new TransactionTemplate(transactionManager);
        this.independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock = clock;
    }

    @Override
    public void requestCode(String phone, String scene) {
        String id = "otp_" + UUID.randomUUID().toString().replace("-", "");
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        SmsSendResult result = smsSender.send(new SmsMessage(
                id, "customer_otp", scene, phone, "sms.customer.otp", Map.of("code", code)));
        boolean sent = "sent".equals(result.status());
        String sendStatus = sent || "throttled".equals(result.status()) ? result.status() : "failed";
        Instant now = clock.instant();
        independent.executeWithoutResult(status -> {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("id", id)
                    .addValue("phone_index", blindIndex.hash(phone))
                    .addValue("scene", scene)
                    .addValue("code_hash", blindIndex.hash(id + ":" + code))
                    .addValue("now", Timestamp.from(now))
                    .addValue("expires_at", Timestamp.from(now.plus(TTL)))
                    .addValue("invalidated_at", sent ? null : Timestamp.from(now))
                    .addValue("send_status", sendStatus)
                    .addValue("provider_ref", result.providerRef())
                    .addValue("send_reason", result.reason());
            // 只在新码成功下发后作废旧码：频控/失败时客人手里已送达的旧码仍可用。
            if (sent) {
                jdbc.update("""
                        UPDATE customer_otp_codes SET invalidated_at = :now
                        WHERE phone_index = :phone_index AND scene = :scene AND consumed_at IS NULL AND invalidated_at IS NULL
                        """, params);
            }
            jdbc.update("""
                    INSERT INTO customer_otp_codes (id, phone_index, scene, code_hash, expires_at, invalidated_at,
                        send_status, provider_ref, send_reason, created_at)
                    VALUES (:id, :phone_index, :scene, :code_hash, :expires_at, :invalidated_at,
                        :send_status, :provider_ref, :send_reason, :now)
                    """, params);
            if (sent) {
                auditEventService.recordSuccess("customer.otp_requested", "public", "customer_otp", id, null, null,
                        Map.of("scene", scene, "send_status", sendStatus));
            }
        });
        if (sent) {
            return;
        }
        String errorCode = "SMS_PROVIDER_NOT_CONFIGURED".equals(result.reason())
                ? "CUSTOMER_OTP_PROVIDER_UNAVAILABLE" : "CUSTOMER_OTP_SEND_FAILED";
        auditEventService.recordFailure("customer.otp_request_failed", "public", "customer_otp", id, null, null, errorCode);
        throw "CUSTOMER_OTP_PROVIDER_UNAVAILABLE".equals(errorCode)
                ? ApiException.conflict(errorCode, "短信服务未配置，请联系管理员")
                : ApiException.conflict(errorCode, "验证码发送失败，请稍后重试");
    }

    @Override
    public void verify(String phone, String smsCode, String scene) {
        Instant now = clock.instant();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("phone_index", blindIndex.hash(phone))
                .addValue("scene", scene)
                .addValue("now", Timestamp.from(now));
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id, code_hash FROM customer_otp_codes
                WHERE phone_index = :phone_index AND scene = :scene AND send_status = 'sent'
                  AND consumed_at IS NULL AND invalidated_at IS NULL AND expires_at > :now
                ORDER BY created_at DESC LIMIT 1
                """, params);
        if (rows.isEmpty()) {
            throw invalid();
        }
        String id = (String) rows.get(0).get("id");
        params.addValue("id", id).addValue("max_failed", MAX_FAILED_ATTEMPTS);
        String expected = (String) rows.get(0).get("code_hash");
        String actual = blindIndex.hash(id + ":" + (smsCode == null ? "" : smsCode.trim()));
        if (actual == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8))) {
            // 调用方事务会因 401 回滚，错误计数必须独立提交；外层只做了普通读、未持该行锁。
            independent.executeWithoutResult(status -> jdbc.update("""
                    UPDATE customer_otp_codes SET failed_attempts = failed_attempts + 1,
                        invalidated_at = CASE WHEN failed_attempts + 1 >= :max_failed THEN :now ELSE invalidated_at END
                    WHERE id = :id
                    """, params));
            throw invalid();
        }
        // 一次性消费随调用方事务：调用方后续失败回滚则该码未被消费。
        int consumed = jdbc.update("""
                UPDATE customer_otp_codes SET consumed_at = :now
                WHERE id = :id AND consumed_at IS NULL AND invalidated_at IS NULL
                """, params);
        if (consumed == 0) {
            throw invalid();
        }
    }

    private static ApiException invalid() {
        return ApiException.unauthorized("CUSTOMER_OTP_INVALID", "短信验证码不正确或已过期");
    }
}
