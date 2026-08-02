package com.complex.platform.evidence;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.dto.RiskAlertActionRequest;
import com.complex.platform.evidence.dto.RiskAlertReportRequest;
import com.complex.platform.evidence.dto.RiskAlertResolveRequest;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

/**
 * 统一异常责任链服务（evidence-risk-layer-spec §8，P0 清单#5）。
 *
 * <p>跨域汇聚层：任意域（ticketing/bar/amusement/inventory/finance/wecom/device/thirdparty…）
 * 的异常以统一 schema 上报，并经一致的责任链流转。
 *
 * <p>状态机：
 * <pre>
 *   open ──claim──▶ handling ──transfer──▶ handling
 *    │                 │
 *    │                 ├──escalate──▶ escalated ──claim──▶ handling
 *    │                 │                   │
 *    │                 ├──resolve──▶ resolved (需结论)
 *    │                 └──dismiss──▶ dismissed
 *    ├──escalate──▶ escalated
 *    ├──resolve──▶ resolved (需结论)
 *    └──dismiss──▶ dismissed
 * </pre>
 * resolved / dismissed 为终态，不可再流转。resolve 必须带 resolutionConclusion（服务层 + DB 双校验）。
 * 每个动作写一条 risk_alert_actions 流水并发 {@code exception.*} 审计事件。
 */
@Service
public class RiskAlertService {

    static final String REPORT_PERMISSION = "risk_alert.report";
    static final String CLAIM_PERMISSION = "risk_alert.claim";
    static final String TRANSFER_PERMISSION = "risk_alert.transfer";
    static final String ESCALATE_PERMISSION = "risk_alert.escalate";
    static final String RESOLVE_PERMISSION = "risk_alert.resolve";
    static final String DISMISS_PERMISSION = "risk_alert.dismiss";
    static final String VIEW_PERMISSION = "risk_alert.view";

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 200;

    private static final Set<String> SOURCE_DOMAINS = Set.of(
            "ticketing", "bar", "amusement", "inventory", "finance",
            "wecom", "device", "thirdparty", "analytics", "platform"
    );
    private static final Set<String> RISK_LEVELS = Set.of("normal", "suspicious", "high", "critical");

    private final RiskAlertRepository repository;
    private final AuthorizationService authorizationService;
    private final AuditedOperationExecutor executor;

    public RiskAlertService(
            RiskAlertRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.executor = new AuditedOperationExecutor(idempotencyService, auditEventService);
    }

    // ── report：创建 open 状态预警 ──────────────────────────────────────────────
    @Transactional
    public RiskAlertRecord report(RiskAlertReportRequest request) {
        RiskAlertReportRequest actual = requireRequest(request);
        return execute(
                "reportRiskAlert",
                actual.idempotencyKey(),
                request,
                RiskAlertRecord.class,
                "exception.reported",
                "exception.report_failed",
                REPORT_PERMISSION,
                () -> {
                    String sourceDomain = requireDomain(actual.sourceDomain());
                    String riskLevel = requireRiskLevel(actual.riskLevel());
                    String alertType = requireText(actual.alertType(), "alert_type");
                    String summary = requireText(actual.summary(), "summary");
                    AccessActor actor = authorizationService.currentActor();
                    String merchantId = authorizationService.currentMerchantScope().orElse(null);
                    Instant now = Instant.now();
                    String alertId = "alert_" + slug(actual.idempotencyKey());

                    RiskAlertRecord alert = new RiskAlertRecord(
                            alertId,
                            sourceDomain,
                            trimToNull(actual.sourceRef()),
                            alertType,
                            riskLevel,
                            "open",
                            summary,
                            actor.id(),
                            null,
                            null,
                            null,
                            null,
                            trimToNull(actual.projectId()),
                            merchantId,
                            actual.idempotencyKey(),
                            now,
                            now
                    );
                    repository.saveAlert(alert);
                    appendAction(alertId, "report", actor.id(), null, "open",
                            actual.reason(), actual.evidenceRefs(), now);
                    return alert;
                }
        );
    }

    // ── claim：认领（open/escalated → handling）────────────────────────────────
    @Transactional
    public RiskAlertRecord claim(String alertId, RiskAlertActionRequest request) {
        RiskAlertActionRequest actual = requireRequest(request);
        return execute(
                "claimRiskAlert:" + requireText(alertId, "alert_id"),
                actual.idempotencyKey(),
                request,
                RiskAlertRecord.class,
                "exception.claimed",
                "exception.claim_failed",
                CLAIM_PERMISSION,
                () -> {
                    RiskAlertRecord existing = requireOpenForFlow(alertId);
                    requireStatusIn(existing, "claim", "open", "escalated");
                    AccessActor actor = authorizationService.currentActor();
                    Instant now = Instant.now();
                    RiskAlertRecord updated = existing.withStatus("handling")
                            .withFirstHandlerId(existing.firstHandlerId() == null ? actor.id() : existing.firstHandlerId())
                            .touched(now);
                    repository.updateAlert(updated);
                    appendAction(alertId, "claim", actor.id(), existing.status(), "handling",
                            actual.reason(), actual.evidenceRefs(), now);
                    return updated;
                }
        );
    }

    // ── transfer：转交（handling → handling，换处理人）─────────────────────────
    @Transactional
    public RiskAlertRecord transfer(String alertId, RiskAlertActionRequest request) {
        RiskAlertActionRequest actual = requireRequest(request);
        return execute(
                "transferRiskAlert:" + requireText(alertId, "alert_id"),
                actual.idempotencyKey(),
                request,
                RiskAlertRecord.class,
                "exception.transferred",
                "exception.transfer_failed",
                TRANSFER_PERMISSION,
                () -> {
                    RiskAlertRecord existing = requireOpenForFlow(alertId);
                    requireStatusIn(existing, "transfer", "handling");
                    String target = requireText(actual.targetHandlerId(), "target_handler_id");
                    AccessActor actor = authorizationService.currentActor();
                    Instant now = Instant.now();
                    RiskAlertRecord updated = existing.withStatus("handling")
                            .withFirstHandlerId(target)
                            .touched(now);
                    repository.updateAlert(updated);
                    appendAction(alertId, "transfer", actor.id(), existing.status(), "handling",
                            transferReason(actual.reason(), target), actual.evidenceRefs(), now);
                    return updated;
                }
        );
    }

    // ── escalate：升级（open/handling → escalated）─────────────────────────────
    @Transactional
    public RiskAlertRecord escalate(String alertId, RiskAlertActionRequest request) {
        RiskAlertActionRequest actual = requireRequest(request);
        return execute(
                "escalateRiskAlert:" + requireText(alertId, "alert_id"),
                actual.idempotencyKey(),
                request,
                RiskAlertRecord.class,
                "exception.escalated",
                "exception.escalate_failed",
                ESCALATE_PERMISSION,
                () -> {
                    RiskAlertRecord existing = requireOpenForFlow(alertId);
                    requireStatusIn(existing, "escalate", "open", "handling");
                    String target = requireText(actual.targetHandlerId(), "target_handler_id");
                    AccessActor actor = authorizationService.currentActor();
                    Instant now = Instant.now();
                    RiskAlertRecord updated = existing.withStatus("escalated")
                            .withEscalatedTo(target)
                            .touched(now);
                    repository.updateAlert(updated);
                    appendAction(alertId, "escalate", actor.id(), existing.status(), "escalated",
                            escalateReason(actual.reason(), target), actual.evidenceRefs(), now);
                    return updated;
                }
        );
    }

    // ── resolve：关闭（→ resolved），必须带结论 ────────────────────────────────
    @Transactional
    public RiskAlertRecord resolve(String alertId, RiskAlertResolveRequest request) {
        RiskAlertResolveRequest actual = requireRequest(request);
        return execute(
                "resolveRiskAlert:" + requireText(alertId, "alert_id"),
                actual.idempotencyKey(),
                request,
                RiskAlertRecord.class,
                "exception.resolved",
                "exception.resolve_failed",
                RESOLVE_PERMISSION,
                () -> {
                    RiskAlertRecord existing = requireOpenForFlow(alertId);
                    requireStatusIn(existing, "resolve", "open", "handling", "escalated");
                    // 服务层校验："异常关闭必须有结论"（DB CHECK 为第二道防线）。
                    String conclusion = requireText(actual.resolutionConclusion(), "resolution_conclusion");
                    AccessActor actor = authorizationService.currentActor();
                    Instant now = Instant.now();
                    RiskAlertRecord updated = existing.withStatus("resolved")
                            .withResolvedBy(actor.id())
                            .withResolutionConclusion(conclusion)
                            .touched(now);
                    repository.updateAlert(updated);
                    appendAction(alertId, "resolve", actor.id(), existing.status(), "resolved",
                            conclusion, actual.evidenceRefs(), now);
                    return updated;
                }
        );
    }

    // ── dismiss：忽略关闭（→ dismissed）────────────────────────────────────────
    @Transactional
    public RiskAlertRecord dismiss(String alertId, RiskAlertActionRequest request) {
        RiskAlertActionRequest actual = requireRequest(request);
        return execute(
                "dismissRiskAlert:" + requireText(alertId, "alert_id"),
                actual.idempotencyKey(),
                request,
                RiskAlertRecord.class,
                "exception.dismissed",
                "exception.dismiss_failed",
                DISMISS_PERMISSION,
                () -> {
                    RiskAlertRecord existing = requireOpenForFlow(alertId);
                    requireStatusIn(existing, "dismiss", "open", "handling", "escalated");
                    AccessActor actor = authorizationService.currentActor();
                    Instant now = Instant.now();
                    RiskAlertRecord updated = existing.withStatus("dismissed").touched(now);
                    repository.updateAlert(updated);
                    appendAction(alertId, "dismiss", actor.id(), existing.status(), "dismissed",
                            actual.reason(), actual.evidenceRefs(), now);
                    return updated;
                }
        );
    }

    // ── 查询（scope 隔离）──────────────────────────────────────────────────────
    public List<RiskAlertRecord> search(String sourceDomain, String status, String projectId, int limit) {
        authorizationService.requirePermission(VIEW_PERMISSION);
        int safeLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        String domain = sourceDomain == null ? null : requireDomain(sourceDomain);
        String merchantScope = authorizationService.currentMerchantScope().orElse(null);
        return executor.readPage(
                AuditedOperation.of("exception.alerts_viewed", "exception.alerts_view_failed", VIEW_PERMISSION, "risk_alert"),
                () -> repository.search(domain, trimToNull(status), trimToNull(projectId), merchantScope, safeLimit))
                .items();
    }

    public RiskAlertDetail getDetail(String alertId) {
        authorizationService.requirePermission(VIEW_PERMISSION);
        String id = requireText(alertId, "alert_id");
        RiskAlertRecord alert = requireScopedAlert(id);
        return new RiskAlertDetail(alert, repository.listActions(id));
    }

    // ── 内部 ──────────────────────────────────────────────────────────────────

    private void appendAction(
            String alertId,
            String actionType,
            String actorId,
            String fromStatus,
            String toStatus,
            String reason,
            List<String> evidenceRefs,
            Instant now
    ) {
        repository.appendAction(new RiskAlertActionRecord(
                "ract_" + UUID.randomUUID().toString().replace("-", ""),
                alertId,
                actionType,
                actorId,
                fromStatus,
                toStatus,
                trimToNull(reason),
                evidenceRefs == null ? List.of() : List.copyOf(evidenceRefs),
                now
        ));
    }

    /** 取出可流转的预警，已做 scope 隔离与存在校验。 */
    private RiskAlertRecord requireOpenForFlow(String alertId) {
        return requireScopedAlert(requireText(alertId, "alert_id"));
    }

    /** 存在性 + 商户隔离：商户用户只能操作自己的预警。 */
    private RiskAlertRecord requireScopedAlert(String alertId) {
        RiskAlertRecord alert = repository.findById(alertId)
                .orElseThrow(() -> ApiException.notFound("RISK_ALERT_NOT_FOUND", "风险预警不存在或已被删除"));
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(alert.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                    "风险预警不属于当前商户");
        }
        return alert;
    }

    /** 状态机校验：终态（resolved/dismissed）不可再流转；非法迁移返回 INVALID_STATE。 */
    private void requireStatusIn(RiskAlertRecord alert, String action, String... allowed) {
        for (String status : allowed) {
            if (status.equals(alert.status())) {
                return;
            }
        }
        throw ApiException.conflict("INVALID_STATE",
                "风险预警当前状态不可执行 " + action + " 操作：" + alert.status());
    }

    private <T> T execute(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            String successEvent,
            String failureEvent,
            String permissionCode,
            Supplier<T> action
    ) {
        // P0: 幂等键按 actor 维度隔离，避免跨用户缓存串号。
        String actorId = authorizationService.currentActor().id();
        return executor.executeWrite(
                actorId + ":" + operationId,
                idempotencyKey,
                request,
                responseType,
                AuditedOperation.of(successEvent, failureEvent, permissionCode, "risk_alert"),
                action,
                this::businessId,
                Map.of());
    }

    private String businessId(Object response) {
        if (response instanceof RiskAlertRecord alert) {
            return alert.alertId();
        }
        return null;
    }

    private String transferReason(String reason, String target) {
        String base = trimToNull(reason);
        String suffix = "transferred to " + target;
        return base == null ? suffix : base + " (" + suffix + ")";
    }

    private String escalateReason(String reason, String target) {
        String base = trimToNull(reason);
        String suffix = "escalated to " + target;
        return base == null ? suffix : base + " (" + suffix + ")";
    }

    private String requireDomain(String value) {
        String domain = requireText(value, "source_domain").toLowerCase(Locale.ROOT);
        if (!SOURCE_DOMAINS.contains(domain)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "不支持的来源域：" + domain);
        }
        return domain;
    }

    private String requireRiskLevel(String value) {
        String level = requireText(value, "risk_level").toLowerCase(Locale.ROOT);
        if (!RISK_LEVELS.contains(level)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "不支持的风险等级：" + level);
        }
        return level;
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private String slug(String value) {
        String slug = (value == null ? UUID.randomUUID().toString() : value)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        String resolved = slug.isEmpty() ? UUID.randomUUID().toString().replace("-", "") : slug;
        return resolved.length() > 56 ? resolved.substring(0, 56) : resolved;
    }
}
