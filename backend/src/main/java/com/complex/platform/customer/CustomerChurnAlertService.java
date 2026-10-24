package com.complex.platform.customer;

// 文件版本：V0.01

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.crypto.BlindIndex;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.RiskAlertService;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ROOT-03-R22 同号注销与注册频次预警（ENUM 第 68 行「同一手机号在一年内注销与注册合计超过后台配置的次数上限的，触发异常预警交风控查看处置
 * （业务参数，全平台统一可配，出厂默认一年内合计 3 次，须同时配置上下限）；预警只作提示，不自动阻断注册。为完成上述计数……系统仅留存注销事件与
 * 该手机号的对应记录至一年统计窗口届满；该记录仅用于上述两项目的、不与新账户建立任何关联」）。
 *
 * <p>计数＝注销记录（只存手机号盲索引、注册时点、注销时点，时点只到日）里近一年的注册与注销时点数；注册时另加本次注册。超过上限即进统一风控预警队列
 * （只写次数，不写手机号），注册、注销照常成功。
 */
@Service
public class CustomerChurnAlertService {

    static final String PERMISSION = "customer.account.manage";
    private static final Duration WINDOW = Duration.ofDays(365);

    public record Setting(int threshold, Integer minThreshold, Integer maxThreshold, String updatedBy, Instant updatedAt) {
    }

    public record SettingRequest(String idempotencyKey, Integer threshold, Integer minThreshold, Integer maxThreshold) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final BlindIndex blindIndex;
    private final RiskAlertService riskAlerts;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    @Autowired
    public CustomerChurnAlertService(NamedParameterJdbcTemplate jdbc, BlindIndex blindIndex, RiskAlertService riskAlerts,
            AuthorizationService authorizationService, AuditEventService auditEventService, IdempotencyService idempotencyService) {
        this.jdbc = jdbc;
        this.blindIndex = blindIndex;
        this.riskAlerts = riskAlerts;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
        this.idempotencyService = idempotencyService;
        this.clock = Clock.systemUTC();
    }

    public Setting getSetting() {
        authorizationService.requirePermission(PERMISSION);
        authorizationService.requirePlatformScope();
        return load();
    }

    /** 全平台统一参数：次数上限与其下限、上限须同时填写，且 1 ≤ 下限 ≤ 次数上限 ≤ 上限。变更留痕前后值。 */
    @Transactional
    public Setting configure(SettingRequest request) {
        try {
            authorizationService.requirePermission(PERMISSION);
            authorizationService.requirePlatformScope();
            if (request == null || request.threshold() == null || request.minThreshold() == null || request.maxThreshold() == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "次数上限须与其下限、上限同时填写");
            }
            int value = request.threshold();
            int min = request.minThreshold();
            int max = request.maxThreshold();
            if (min < 1 || min > value || value > max) {
                throw ApiException.badRequest("VALIDATION_FAILED", "次数上限须满足：1 ≤ 下限 ≤ 次数上限 ≤ 上限");
            }
            return idempotencyService.execute("configureCustomerChurnAlertSetting", request.idempotencyKey(), request, () -> {
                Setting before = load();
                jdbc.update("""
                        UPDATE customer_churn_alert_settings
                           SET threshold = :value, min_threshold = :min, max_threshold = :max, updated_by = :by, updated_at = now()
                         WHERE id = 'global'
                        """, new MapSqlParameterSource().addValue("value", value).addValue("min", min).addValue("max", max)
                        .addValue("by", authorizationService.currentActor().id()));
                auditEventService.recordSuccess("customer.churn_alert_setting.configured", PERMISSION,
                        "customer_churn_alert_setting", "global", null, request.idempotencyKey(),
                        Map.of("threshold_before", before.threshold(), "threshold", value, "min_threshold", min, "max_threshold", max));
                return load();
            }, Setting.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("customer.churn_alert_setting.config_failed", PERMISSION,
                    "customer_churn_alert_setting", "global", null, request == null ? null : request.idempotencyKey(),
                    exception instanceof ApiException api ? api.code() : exception.getClass().getSimpleName());
            throw exception;
        }
    }

    /** 注册成功后（同一事务）：本次注册＋留存记录里近一年的注册与注销。 */
    public void afterRegistration(String phone, String customerId) {
        alertIfOver(blindIndex.hash(phone), customerId, 1, "alert_churn_reg_" + customerId);
    }

    /** 注销状态写入后（同一事务、删除手机号之前）：留存本次注销与该手机号的对应记录，再计数（第 N 个事件是注销时也要预警）。 */
    public void afterDeregistration(String customerId) {
        Map<String, Object> row = jdbc.queryForMap("SELECT phone_hash, created_at FROM customers WHERE id = :id",
                Map.of("id", customerId));
        String phoneHash = (String) row.get("phone_hash");
        if (phoneHash == null) {
            return;
        }
        // 时点只留到日（一年滚动计数够用）：不和 customers 行的建号、注销时刻精确对上，凭手机号查到的记录反查不出客人编号
        // （BSM 第 863 行「钱账留存，但脱去个人标识、改以不可反查的编号记录」；操作员裁决第十三批第 10 条「只脱去与客人的关联标识」）。
        Instant day = clock.instant().truncatedTo(ChronoUnit.DAYS);
        jdbc.update("""
                INSERT INTO customer_deregistration_marks (id, phone_hash, registered_at, deregistered_at, purge_after)
                VALUES (:id, :phone_hash, :registered_at, :now, :purge_after)
                """, new MapSqlParameterSource()
                .addValue("id", "cdm_" + UUID.randomUUID().toString().replace("-", ""))
                .addValue("phone_hash", phoneHash)
                .addValue("registered_at", Timestamp.from(((Timestamp) row.get("created_at")).toInstant().truncatedTo(ChronoUnit.DAYS)))
                .addValue("now", Timestamp.from(day))
                .addValue("purge_after", Timestamp.from(day.plus(WINDOW))));
        alertIfOver(phoneHash, customerId, 0, "alert_churn_dereg_" + customerId);
    }

    private void alertIfOver(String phoneHash, String customerId, int extra, String alertId) {
        Integer marked = jdbc.queryForObject("""
                SELECT (SELECT count(*) FROM customer_deregistration_marks WHERE phone_hash = :hash AND registered_at >= :since)
                     + (SELECT count(*) FROM customer_deregistration_marks WHERE phone_hash = :hash AND deregistered_at >= :since)
                """, new MapSqlParameterSource().addValue("hash", phoneHash)
                .addValue("since", Timestamp.from(clock.instant().minus(WINDOW))), Integer.class);
        int events = (marked == null ? 0 : marked) + extra;
        int threshold = load().threshold();
        if (events > threshold) {
            riskAlerts.recordSystemAlert(alertId, "platform", customerId, "customer.churn", "suspicious",
                    "同一手机号一年内注销与注册合计 " + events + " 次，超过上限 " + threshold + " 次（只作提示，不阻断注册）",
                    null, "system");
        }
    }

    private Setting load() {
        return jdbc.queryForObject("""
                SELECT threshold, min_threshold, max_threshold, updated_by, updated_at FROM customer_churn_alert_settings WHERE id = 'global'
                """, Map.of(), (rs, n) -> new Setting(rs.getInt("threshold"), (Integer) rs.getObject("min_threshold"),
                (Integer) rs.getObject("max_threshold"), rs.getString("updated_by"), rs.getTimestamp("updated_at").toInstant()));
    }
}
