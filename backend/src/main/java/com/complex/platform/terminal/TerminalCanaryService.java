package com.complex.platform.terminal;

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.terminal.dto.CanaryRolloutRequest;
import com.complex.platform.terminal.dto.CanaryTargetRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 终端开关灰度层后台维护（V342，R-10）。
 *
 * <p>灰度是求值链最细的一档，只能收窄不能放宽——运营在后台开一个 端×能力 的灰度试点，
 * 再把试点对象（某商户/某设备/某模块…）加进白名单；白名单外的对象在试点期一律关闭。
 *
 * <p>幂等：试点按 (端, 能力) 唯一、白名单条目按 (试点, 维度, 对象) 唯一，重复提交即覆盖，
 * 天然幂等，不需要幂等键。留痕：开/关试点、加白名单、移出白名单全部写 audit_events。
 */
@Service
public class TerminalCanaryService {

    /** 试点对象可用维度（与 V342 chk_tcct_target_type 逐字一致；不含 global）。 */
    static final Set<String> TARGET_TYPES = Set.of(
            "project", "merchant", "business_system", "module", "version", "device");

    /** 端取值集（与 V342 chk_tccr_terminal_type 逐字一致）。 */
    static final Set<String> TERMINAL_TYPES = Set.of(
            "customer_app", "customer_miniapp", "web_admin", "mobile_admin",
            "staff_tablet", "staff_work_phone", "staff_personal_app",
            "merchant_web", "merchant_app", "kiosk", "tablet", "*",
            "app", "miniapp", "web", "staff_private_phone", "merchant_portal", "dealer_portal");

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;

    private final TerminalCanaryRepository repository;
    private final AuditEventService auditEventService;

    public TerminalCanaryService(TerminalCanaryRepository repository, AuditEventService auditEventService) {
        this.repository = repository;
        this.auditEventService = auditEventService;
    }

    /** 灰度试点全量（含各自未存档的白名单条目）。 */
    public PageResponse<TerminalCanaryRolloutView> listRollouts() {
        try {
            List<TerminalCanaryRolloutView> views = repository.listRollouts().stream()
                    .map(rollout -> new TerminalCanaryRolloutView(rollout, repository.listActiveTargets(rollout.id())))
                    .toList();
            auditEventService.recordSuccess(
                    "terminal_canary.viewed",
                    "terminal.switch.view",
                    "terminal_capability_canary_rollout",
                    "all",
                    null,
                    null,
                    Map.of("count", views.size())
            );
            return new PageResponse<>(views, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, views.size());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "terminal_canary.view_failed",
                    "terminal.switch.view",
                    "terminal_capability_canary_rollout",
                    "all",
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    /** 开/关某 端×能力 的灰度试点。 */
    @Transactional
    public TerminalCanaryRolloutRecord upsertRollout(CanaryRolloutRequest request) {
        try {
            return doUpsertRollout(request);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "terminal_canary.rollout_change_failed",
                    "terminal.switch.manage",
                    "terminal_capability_canary_rollout",
                    request == null ? "unknown" : String.valueOf(request.capabilityCode()),
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private TerminalCanaryRolloutRecord doUpsertRollout(CanaryRolloutRequest request) {
        CanaryRolloutRequest actual = requireRequest(request);
        String terminalType = requireEnum(
                requireText(actual.terminalType(), "terminal_type"), TERMINAL_TYPES, "terminal_type");
        String capabilityCode = requireText(actual.capabilityCode(), "capability_code");
        if (actual.active() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "active is required");
        }
        // 开启灰度=收窄放行面，必须写清为什么（运营事后要知道这是试点期还是被误开）。
        String reason = requireText(actual.reason(), "reason");
        Instant now = Instant.now();
        TerminalCanaryRolloutRecord existing = repository.findRollout(terminalType, capabilityCode).orElse(null);
        TerminalCanaryRolloutRecord saved = repository.saveRollout(new TerminalCanaryRolloutRecord(
                existing != null ? existing.id() : newId("tccr"),
                terminalType,
                capabilityCode,
                actual.active(),
                reason,
                existing != null ? existing.createdAt() : now,
                now
        ));
        // 单一事件名 + metadata 带 active/action，便于按"灰度开放/停用"过滤（对齐 OpenAPI x-audit-event-success）。
        auditEventService.recordSuccess(
                "terminal_canary.rollout_changed",
                "terminal.switch.manage",
                "terminal_capability_canary_rollout",
                saved.id(),
                null,
                null,
                Map.of(
                        "terminal_type", terminalType,
                        "capability", capabilityCode,
                        "active", saved.active(),
                        "action", saved.active() ? "canary_opened" : "canary_closed",
                        "reason", reason
                )
        );
        return saved;
    }

    /** 把一个试点对象加进白名单（已存在则刷新理由并"复活"已存档条目）。 */
    @Transactional
    public TerminalCanaryTargetRecord addTarget(String rolloutId, CanaryTargetRequest request) {
        try {
            return doAddTarget(rolloutId, request);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "terminal_canary.target_add_failed",
                    "terminal.switch.manage",
                    "terminal_capability_canary_target",
                    rolloutId == null ? "unknown" : rolloutId,
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private TerminalCanaryTargetRecord doAddTarget(String rolloutId, CanaryTargetRequest request) {
        String id = requireText(rolloutId, "rollout_id");
        CanaryTargetRequest actual = requireRequest(request);
        TerminalCanaryRolloutRecord rollout = repository.findRolloutById(id)
                .orElseThrow(() -> ApiException.notFound("CANARY_ROLLOUT_NOT_FOUND", "灰度发布不存在或已被删除"));
        String targetType = requireEnum(
                requireText(actual.targetType(), "target_type"), TARGET_TYPES, "target_type");
        String targetId = requireText(actual.targetId(), "target_id");
        String reason = requireText(actual.reason(), "reason");
        Instant now = Instant.now();
        TerminalCanaryTargetRecord saved = repository.saveTarget(new TerminalCanaryTargetRecord(
                newId("tcct"),
                rollout.id(),
                targetType,
                targetId,
                reason,
                now,
                now,
                null,
                null,
                null
        ));
        auditEventService.recordSuccess(
                "terminal_canary.target_added",
                "terminal.switch.manage",
                "terminal_capability_canary_target",
                saved.id(),
                null,
                null,
                Map.of(
                        "rollout_id", rollout.id(),
                        "terminal_type", rollout.terminalType(),
                        "capability", rollout.capabilityCode(),
                        "target_type", targetType,
                        "target_id", targetId,
                        "reason", reason
                )
        );
        return saved;
    }

    /**
     * 把一个试点对象移出白名单。删除=软删除铁律：行保留、填存档理由、可恢复（重新加回即复活）。
     * 注意方向：移出白名单是<b>收紧</b>（该对象随即被灰度关闭），不是放开。
     */
    @Transactional
    public TerminalCanaryTargetRecord archiveTarget(String targetId, CanaryTargetRequest request) {
        try {
            return doArchiveTarget(targetId, request);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "terminal_canary.target_archive_failed",
                    "terminal.switch.manage",
                    "terminal_capability_canary_target",
                    targetId == null ? "unknown" : targetId,
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private TerminalCanaryTargetRecord doArchiveTarget(String targetId, CanaryTargetRequest request) {
        String id = requireText(targetId, "target_id");
        CanaryTargetRequest actual = requireRequest(request);
        String reason = requireText(actual.reason(), "reason");
        TerminalCanaryTargetRecord existing = repository.findTargetById(id)
                .orElseThrow(() -> ApiException.notFound("CANARY_TARGET_NOT_FOUND", "灰度目标不存在或已被删除"));
        if (existing.archivedAt() != null) {
            return existing;
        }
        TerminalCanaryTargetRecord archived = repository.archiveTarget(id, Instant.now(), reason, currentActorId());
        auditEventService.recordSuccess(
                "terminal_canary.target_archived",
                "terminal.switch.manage",
                "terminal_capability_canary_target",
                id,
                null,
                null,
                Map.of(
                        "rollout_id", existing.rolloutId(),
                        "target_type", existing.targetType(),
                        "target_id", existing.targetId(),
                        "archive_reason", reason
                )
        );
        return archived;
    }

    /** 灰度试点 + 其白名单（后台列表用）。 */
    public record TerminalCanaryRolloutView(
            TerminalCanaryRolloutRecord rollout,
            List<TerminalCanaryTargetRecord> targets
    ) {
    }

    private static String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    /** 失败留痕的 error_code：ApiException 取其业务码，其余取异常类名。 */
    private static String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException && apiException.code() != null) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }

    private static <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private static String requireEnum(String value, Set<String> allowed, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null || !allowed.contains(trimmed)) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " 不支持该取值：" + value);
        }
        return trimmed;
    }

    /** 当前操作人 id（无安全上下文时留空，不阻断存档）。 */
    private static String currentActorId() {
        try {
            var authentication = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                if (authentication.getPrincipal() instanceof com.complex.platform.access.AccessActor actor) {
                    return actor.id();
                }
                return authentication.getName();
            }
        } catch (RuntimeException ignored) {
            // best-effort：无安全上下文（异步/测试）→ null。
        }
        return null;
    }
}
