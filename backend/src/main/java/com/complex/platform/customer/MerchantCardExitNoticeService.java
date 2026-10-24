package com.complex.platform.customer;

// 文件版本：V0.01

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.trimToNull;

/**
 * ROOT-03-R31 商户级储值卡退出公示。ENUM 第 86 行「发卡商户退租、合同终止或被清退前，必须提前公告告知持卡消费者本商户即将结束租赁关系，并同时给出
 * 该卡在剩余期间的使用条件与退款条件（可用到哪天、能否退、退多少比例、怎么退、找谁退），公告须在客人手机应用、小程序和该商户的商场店面三处同时公示，
 * 公示期不少于后台配置的天数（业务参数，全平台统一可配，须同时配置上下限）」；第十三批裁决第 2 条「商户在商户门户自行发公告，平台只校验公示天数，
 * 不影响退租流程」。公示天数 HTML 未给出厂值：平台配置前商户发公告 409；客人端只读公示中的公告。
 */
@Service
public class MerchantCardExitNoticeService {

    static final String POLICY_PERMISSION = "customer.stored_value.refund";
    static final String PUBLISH_PERMISSION = "merchant.portal.card_exit_notice.publish";

    /** 公示天数（值与上下限同配）。 */
    public record Policy(int publishDays, int minDays, int maxDays) {
    }

    public record PolicyRequest(String idempotencyKey, Integer publishDays, Integer minDays, Integer maxDays, String reason) {
    }

    /** 五项条件：可用到哪天、能否退、退多少比例（基点）、怎么退、找谁退；publishUntil＝公示截止日。 */
    public record PublishRequest(String idempotencyKey, LocalDate useUntil, Boolean refundable, Integer refundRatioBps,
            String refundMethod, String refundContact, LocalDate publishUntil) {
    }

    /** holdsCard＝当前客人持有该商户未销户的商户卡（只在客人端列表有意义）。 */
    public record Notice(String id, String merchantId, String merchantName, String projectId, LocalDate useUntil, boolean refundable,
            int refundRatioBps, String refundMethod, String refundContact, Instant publishFrom, LocalDate publishUntil, boolean holdsCard) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final AuthorizationService authorizationService;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;

    public MerchantCardExitNoticeService(NamedParameterJdbcTemplate jdbc, AuthorizationService authorizationService,
            IdempotencyService idempotencyService, AuditEventService auditEventService) {
        this.jdbc = jdbc;
        this.authorizationService = authorizationService;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
    }

    public Policy getPolicy() {
        authorizationService.requirePlatformScope();
        authorizationService.requirePermission(POLICY_PERMISSION);
        return findPolicy();
    }

    @Transactional
    public Policy savePolicy(PolicyRequest request) {
        return audited("stored_value.exit_notice_policy_configured", "stored_value.exit_notice_policy_config_failed", POLICY_PERMISSION,
                "global", request == null ? null : request.idempotencyKey(), () -> {
                    authorizationService.requirePlatformScope();
                    authorizationService.requirePermission(POLICY_PERMISSION);
                    Integer days = request == null ? null : request.publishDays();
                    Integer min = request == null ? null : request.minDays();
                    Integer max = request == null ? null : request.maxDays();
                    if (days == null || min == null || max == null || min < 1 || days < min || days > max) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "公示天数须同时填写参数值及上下限，下限不小于 1 天，参数值须在上下限内");
                    }
                    return idempotencyService.execute("saveMerchantCardExitNoticePolicy", request.idempotencyKey(), request, () -> {
                        jdbc.update("""
                                INSERT INTO merchant_card_exit_notice_policy (scope_id, publish_days, min_days, max_days, updated_by, updated_at)
                                VALUES ('global', :days, :min, :max, :actor, now())
                                ON CONFLICT (scope_id) DO UPDATE SET publish_days = EXCLUDED.publish_days, min_days = EXCLUDED.min_days,
                                    max_days = EXCLUDED.max_days, updated_by = EXCLUDED.updated_by, updated_at = now()
                                """, new MapSqlParameterSource().addValue("days", days).addValue("min", min).addValue("max", max)
                                .addValue("actor", authorizationService.currentActor().id()));
                        return new Policy(days, min, max);
                    }, Policy.class);
                }, Map.of("publish_days", request == null || request.publishDays() == null ? "" : request.publishDays(),
                        "reason", request == null || request.reason() == null ? "" : request.reason()));
    }

    /** 商户门户发布本商户的退出公告：只校验五项条件齐全与公示天数，不与退租、合同终止流程联动（裁决第 2 条）。 */
    @Transactional
    public Notice publish(PublishRequest request) {
        return audited("stored_value.exit_notice_published", "stored_value.exit_notice_publish_failed", PUBLISH_PERMISSION,
                authorizationService.currentMerchantIdQuietly().orElse("none"), request == null ? null : request.idempotencyKey(), () -> {
                    authorizationService.requirePermission(PUBLISH_PERMISSION);
                    String merchantId = requireMerchant();
                    if (request == null || request.useUntil() == null || request.refundable() == null || request.refundRatioBps() == null
                            || request.refundRatioBps() < 0 || request.refundRatioBps() > 10_000 || trimToNull(request.refundMethod()) == null
                            || trimToNull(request.refundContact()) == null || request.publishUntil() == null) {
                        throw ApiException.badRequest("VALIDATION_FAILED",
                                "须写明可用到哪天、能否退、退款比例（0～10000 基点）、怎么退、找谁退，以及公示截止日");
                    }
                    Policy policy = findPolicy();
                    if (policy == null) {
                        throw ApiException.conflict("EXIT_NOTICE_POLICY_NOT_CONFIGURED", "平台尚未配置退出公示天数，暂不能发布");
                    }
                    long days = ChronoUnit.DAYS.between(LocalDate.now(LocalZone.ID), request.publishUntil());
                    if (days < policy.publishDays()) {
                        throw ApiException.badRequest("EXIT_NOTICE_PERIOD_TOO_SHORT", "公示期不得少于 " + policy.publishDays() + " 天");
                    }
                    return idempotencyService.execute("publishMerchantCardExitNotice:" + merchantId, request.idempotencyKey(), request, () -> {
                        String id = "mcen_" + UUID.randomUUID().toString().replace("-", "");
                        jdbc.update("""
                                INSERT INTO merchant_card_exit_notices (id, merchant_id, project_id, use_until, refundable, refund_ratio_bps,
                                    refund_method, refund_contact, publish_from, publish_until, created_by)
                                VALUES (:id, :merchant, (SELECT project_id FROM merchants WHERE id = :merchant), :use_until, :refundable, :ratio,
                                    :method, :contact, now(), :publish_until, :actor)
                                """, new MapSqlParameterSource().addValue("id", id).addValue("merchant", merchantId)
                                .addValue("use_until", Date.valueOf(request.useUntil())).addValue("refundable", request.refundable())
                                .addValue("ratio", request.refundRatioBps()).addValue("method", request.refundMethod().trim())
                                .addValue("contact", request.refundContact().trim()).addValue("publish_until", Date.valueOf(request.publishUntil()))
                                .addValue("actor", authorizationService.currentActor().id()));
                        return query("WHERE n.id = :id", new MapSqlParameterSource("id", id).addValue("customer", null)).getFirst();
                    }, Notice.class);
                }, Map.of());
    }

    public List<Notice> listMine() {
        authorizationService.requirePermission(PUBLISH_PERMISSION);
        return query("WHERE n.merchant_id = :merchant ORDER BY n.publish_from DESC",
                new MapSqlParameterSource("merchant", requireMerchant()).addValue("customer", null));
    }

    /** 客人端：公示中的全部公告，本人持该商户卡的排前（客人会话由控制器校验）。 */
    public List<Notice> listForCustomer(String customerId) {
        return query("WHERE n.publish_until >= :today ORDER BY holds_card DESC, n.publish_from DESC",
                new MapSqlParameterSource("today", Date.valueOf(LocalDate.now(LocalZone.ID))).addValue("customer", customerId));
    }

    private List<Notice> query(String tail, MapSqlParameterSource params) {
        return jdbc.query("""
                SELECT n.*, m.merchant_name,
                       EXISTS (SELECT 1 FROM customer_stored_value_accounts a WHERE a.customer_id = :customer
                                  AND a.merchant_id = n.merchant_id AND a.status <> 'closed') AS holds_card
                  FROM merchant_card_exit_notices n JOIN merchants m ON m.id = n.merchant_id
                """ + tail, params, (rs, i) -> new Notice(rs.getString("id"), rs.getString("merchant_id"), rs.getString("merchant_name"),
                rs.getString("project_id"), rs.getDate("use_until").toLocalDate(), rs.getBoolean("refundable"), rs.getInt("refund_ratio_bps"),
                rs.getString("refund_method"), rs.getString("refund_contact"), rs.getTimestamp("publish_from").toInstant(),
                rs.getDate("publish_until").toLocalDate(), rs.getBoolean("holds_card")));
    }

    private Policy findPolicy() {
        return jdbc.query("SELECT publish_days, min_days, max_days FROM merchant_card_exit_notice_policy WHERE scope_id = 'global'",
                (rs, i) -> new Policy(rs.getInt("publish_days"), rs.getInt("min_days"), rs.getInt("max_days"))).stream().findFirst().orElse(null);
    }

    private String requireMerchant() {
        return authorizationService.currentMerchantIdQuietly()
                .orElseThrow(() -> ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "退出公告只能由发卡商户在商户门户发布"));
    }

    private <T> T audited(String successEvent, String failureEvent, String permission, String businessId, String key, Supplier<T> action,
            Map<String, Object> detail) {
        try {
            T result = action.get();
            auditEventService.recordSuccess(successEvent, permission, "merchant_card_exit_notice", businessId, null, key, detail);
            return result;
        } catch (RuntimeException failure) {
            auditEventService.recordFailure(failureEvent, permission, "merchant_card_exit_notice", businessId, null, key,
                    failure instanceof ApiException api ? api.code() : "INTERNAL_ERROR");
            throw failure;
        }
    }
}
