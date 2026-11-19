package com.complex.platform.evidence;

// 文件版本：V0.02

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
            "wecom", "device", "thirdparty", "analytics", "platform", "parking"
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
                    RiskAlertRecord existing = requireOpenForFlow(alertId, CLAIM_PERMISSION);
                    requireStatusIn(existing, "open", "escalated");
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
                    RiskAlertRecord existing = requireOpenForFlow(alertId, TRANSFER_PERMISSION);
                    requireStatusIn(existing, "handling");
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
                    RiskAlertRecord existing = requireOpenForFlow(alertId, ESCALATE_PERMISSION);
                    requireStatusIn(existing, "open", "handling");
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
                    RiskAlertRecord existing = requireOpenForFlow(alertId, RESOLVE_PERMISSION);
                    requireStatusIn(existing, "open", "handling", "escalated");
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
                    RiskAlertRecord existing = requireOpenForFlow(alertId, DISMISS_PERMISSION);
                    requireStatusIn(existing, "open", "handling", "escalated");
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
    /**
     * 第十八批 BC 段 369-R1（BSM:1657「集中汇集账户与出入场行为异常……供风控岗与管理层查看与处置」）：来源可多个（为空不按来源过滤）。
     * 375-R2（BSM:1657「每条预警的查看……均须留痕」）：成功留痕另记本次列出的预警编号 alert_ids。
     */
    public List<RiskAlertRecord> search(List<String> sourceDomains, String status, String projectId, int limit) {
        authorizationService.requirePermission(VIEW_PERMISSION);
        int safeLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        List<String> domains = requireDomains(sourceDomains);
        Scope scope = scope(VIEW_PERMISSION);
        return executor.readPage(
                AuditedOperation.of("exception.alerts_viewed", "exception.alerts_view_failed", VIEW_PERMISSION, "risk_alert"),
                () -> repository.search(domains, trimToNull(status), trimToNull(projectId), scope.merchant(), scope.projects(), safeLimit),
                items -> Map.of("alert_ids", items.stream().map(RiskAlertRecord::alertId).toList()))
                .items();
    }

    /**
     * 369-R1（BSM:1835「经营看板显示异常预警的入口与未处理条数计数卡片……看板上的计数只是入口提示」）：未处理（待处理、处理中）条数，
     * 来源与范围同 {@link #search}；计数不显示任何预警内容，不留痕。
     */
    public long countUnfinished(List<String> sourceDomains) {
        authorizationService.requirePermission(VIEW_PERMISSION);
        List<String> domains = requireDomains(sourceDomains);
        Scope scope = scope(VIEW_PERMISSION);
        return repository.countUnfinished(domains, scope.merchant(), scope.projects());
    }

    /** 375-R1（BSM:1657「每条预警的查看、转办与处置结论均须留痕。」）：每打开一次记一条，业务编号为该预警；不存在或不在范围内记失败。 */
    public RiskAlertDetail getDetail(String alertId) {
        authorizationService.requirePermission(VIEW_PERMISSION);
        String id = requireText(alertId, "alert_id");
        return executor.readSingle(
                AuditedOperation.of("exception.alert_viewed", "exception.alert_view_failed", VIEW_PERMISSION, "risk_alert"),
                () -> new RiskAlertDetail(requireScopedAlert(id, VIEW_PERMISSION), repository.listActions(id)),
                item -> ((RiskAlertDetail) item).alert().alertId());
    }

    /**
     * HP-P01：业务服务在自身已鉴权的动作里（或第三方签名回写里，无登录人）直接把异常放进统一队列。
     * reporterId 为操作人或系统标识；同一 alertId 已入队即返回原预警（回写重放不重复入队）。
     */
    @Transactional
    public RiskAlertRecord recordSystemAlert(String alertId, String sourceDomain, String sourceRef, String alertType,
            String riskLevel, String summary, String projectId, String reporterId) {
        Optional<RiskAlertRecord> existing = repository.findById(alertId);
        if (existing.isPresent()) {
            return existing.get();
        }
        Instant now = Instant.now();
        RiskAlertRecord alert = new RiskAlertRecord(alertId, requireDomain(sourceDomain), sourceRef, alertType,
                requireRiskLevel(riskLevel), "open", summary, reporterId, null, null, null, null, projectId, null,
                alertId, now, now);
        repository.saveAlert(alert);
        appendAction(alertId, "report", reporterId, null, "open", summary, List.of(), now);
        return alert;
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

    /** 取出可流转的预警，已做 scope 隔离与存在校验（范围按该动作的入口权限码）。 */
    private RiskAlertRecord requireOpenForFlow(String alertId, String permission) {
        return requireScopedAlert(requireText(alertId, "alert_id"), permission);
    }

    /**
     * 存在性 + 范围：商户用户只能看、办自己的预警；其余按入口权限码可触及的项目（369-R6，见 {@link Scope}），
     * 不带项目的只有平台位（平台管理员、最高权限）可看可办。
     */
    private RiskAlertRecord requireScopedAlert(String alertId, String permission) {
        RiskAlertRecord alert = repository.findById(alertId)
                .orElseThrow(() -> ApiException.notFound("RISK_ALERT_NOT_FOUND", "风险预警不存在或已被删除"));
        Scope scope = scope(permission);
        if (scope.merchant() != null) {
            if (!scope.merchant().equals(alert.merchantId())) {
                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "风险预警不属于当前商户");
            }
        } else if (!scope.reach().covers(alert.projectId())) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "风险预警不在本权限可触及的项目范围内");
        }
        return alert;
    }

    /**
     * 第十八批 BC 段 369-R6（PGM:659「所有业务数据默认按项目隔离，普通岗位没有跨项目查看权；只有项目之上的获授权岗位才可以跨项目汇总。」）：
     * 读与处置的范围。挂商户的账号照旧只按商户（merchant 非空）；其余按入口权限码可触及的项目（{@link AuthorizationService#projectReach}）——
     * 平台管理员与最高权限（363-R1）为平台位，看全部，含不带项目的同号注销注册超限预警；其余人只到可触及项目里带项目的预警。
     */
    private record Scope(String merchant, AuthorizationService.ProjectReach reach) {
        /** 列表与计数的项目集合：null＝不按项目过滤。 */
        Set<String> projects() {
            return merchant != null || reach.platformAdmin() ? null : reach.organizationIds();
        }
    }

    private Scope scope(String permission) {
        Optional<String> merchant = authorizationService.currentMerchantIdQuietly();
        return merchant.isPresent() ? new Scope(merchant.get(), null)
                : new Scope(null, authorizationService.projectReach(authorizationService.currentActor(), permission));
    }

    /**
     * 状态机校验：终态（resolved/dismissed）不可再流转；非法迁移返回 INVALID_STATE。第十八批 BC 段 369-R1（PGM:287「……提示、报错……
     * 必须全部为中文」）：报错一句固定中文，不带动作与状态码（两人同时处置同一条时第二人会看到）。
     */
    private void requireStatusIn(RiskAlertRecord alert, String... allowed) {
        if (!List.of(allowed).contains(alert.status())) {
            throw ApiException.conflict("INVALID_STATE", "风险预警状态已变化，不能执行该操作，请刷新后查看");
        }
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

    /** 369-R1（PGM:287）：转办记录的原因写「转办给 对象编号」，填了原因的为「原因（转办给 对象编号）」。 */
    private String transferReason(String reason, String target) {
        String base = trimToNull(reason);
        String suffix = "转办给 " + target;
        return base == null ? suffix : base + "（" + suffix + "）";
    }

    private String escalateReason(String reason, String target) {
        String base = trimToNull(reason);
        String suffix = "escalated to " + target;
        return base == null ? suffix : base + " (" + suffix + ")";
    }

    private List<String> requireDomains(List<String> values) {
        return values == null ? List.of() : values.stream().map(this::requireDomain).distinct().toList();
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
