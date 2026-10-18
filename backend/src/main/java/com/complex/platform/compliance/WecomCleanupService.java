// 文件版本：V0.01
package com.complex.platform.compliance;

import static com.complex.platform.common.Strings.requireText;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.auth.MasterAuthorityService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.compliance.WecomCleanupPreview.Item;
import com.complex.platform.evidence.AuditEventService;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WecomCleanupService {
    private final JdbcWecomCleanupRepository repository;
    private final AuthorizationService authorization;
    private final MasterAuthorityService master;
    private final AuditEventService audit;

    WecomCleanupService(JdbcWecomCleanupRepository repository, AuthorizationService authorization,
                        MasterAuthorityService master, AuditEventService audit) {
        this.repository = repository;
        this.authorization = authorization;
        this.master = master;
        this.audit = audit;
    }

    @Transactional
    public WecomCleanupPreview preview(WecomCleanupPreview.Request request) {
        try {
            String actor = requireMaster();
            if (request == null || request.items() == null || request.items().isEmpty()) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请选择要清理的消息并逐条填写理由");
            }
            var ids = new HashSet<String>();
            List<Item> items = request.items().stream().map(item -> {
                if (item == null) throw ApiException.badRequest("VALIDATION_FAILED", "清理条目不能为空");
                String id = requireText(item.messageId(), "message_id");
                if (!ids.add(id)) throw ApiException.badRequest("VALIDATION_FAILED", "同一消息不能重复选择");
                return new Item(id, requireText(item.reason(), "每条清理理由"));
            }).toList();
            WecomCleanupPreview result = repository.create("wcp_" + UUID.randomUUID(), actor, repository.capture(items));
            record("wecom.cleanup_previewed", result);
            return result;
        } catch (RuntimeException exception) {
            audit.recordFailure("wecom.cleanup_preview_failed", "wecom.retention.manage",
                    "wecom_cleanup", "new", null, null,
                    exception instanceof ApiException api ? api.code() : "INTERNAL_ERROR");
            throw exception;
        }
    }

    public List<WecomCleanupPreview> list() {
        try {
            requireMaster();
            List<WecomCleanupPreview> rows = repository.list();
            audit.recordSuccess("wecom.cleanup_listed", "wecom.retention.manage", "wecom_cleanup", "list",
                    null, null, Map.of("count", rows.size()));
            return rows;
        } catch (RuntimeException exception) {
            audit.recordFailure("wecom.cleanup_list_failed", "wecom.retention.manage",
                    "wecom_cleanup", "list", null, null,
                    exception instanceof ApiException api ? api.code() : "INTERNAL_ERROR");
            throw exception;
        }
    }

    public WecomCleanupPreview get(String id) {
        try {
            String actor = requireMaster();
            WecomCleanupPreview preview = requireOwner(repository.find(requireText(id, "preview_id"), false), actor);
            record("wecom.cleanup_viewed", preview);
            return preview;
        } catch (RuntimeException exception) {
            audit.recordFailure("wecom.cleanup_view_failed", "wecom.retention.manage",
                    "wecom_cleanup", id, null, null,
                    exception instanceof ApiException api ? api.code() : "INTERNAL_ERROR");
            throw exception;
        }
    }

    public MasterAuthorityService.ActionChallenge challenge(String id) {
        try {
            WecomCleanupPreview preview = get(id);
            requirePending(preview);
            var challenge = master.issueActionChallenge(preview.createdBy(), action(preview), preview.scopeDigest());
            record("wecom.cleanup_challenged", preview);
            return challenge;
        } catch (RuntimeException exception) {
            audit.recordFailure("wecom.cleanup_challenge_failed", "wecom.retention.manage",
                    "wecom_cleanup", id, null, null,
                    exception instanceof ApiException api ? api.code() : "INTERNAL_ERROR");
            throw exception;
        }
    }

    @Transactional
    public WecomCleanupPreview execute(String id, WecomCleanupPreview.ExecuteRequest request) {
        try {
            String actor = requireMaster();
            WecomCleanupPreview preview = requireOwner(repository.find(requireText(id, "preview_id"), true), actor);
            if (preview.executedAt() != null) {
                record("wecom.cleanup_result_viewed", preview);
                return preview;
            }
            if (request == null) throw ApiException.badRequest("VALIDATION_FAILED", "必须提交本次 U 盘授权签名");
            master.verifyAction(actor, action(preview), preview.scopeDigest(), request.challenge(), request.signature());
            List<Item> items = preview.candidates().stream().map(c -> new Item(c.messageId(), c.reason())).toList();
            List<WecomCleanupPreview.Candidate> current = repository.capture(items);
            if (!current.equals(preview.candidates()) || !repository.scopeDigest(current).equals(preview.scopeDigest())) {
                throw ApiException.conflict("WECOM_CLEANUP_SCOPE_CHANGED", "消息或关联命中已变化，请重新预览并授权");
            }
            repository.deleteExact(preview);
            WecomCleanupPreview result = repository.complete(id, actor, master.publicKeyFingerprint());
            record("wecom.cleanup_executed", result);
            return result;
        } catch (RuntimeException exception) {
            audit.recordFailure("wecom.cleanup_execute_failed", "wecom.retention.manage",
                    "wecom_cleanup", id, null, null,
                    exception instanceof ApiException api ? api.code() : "INTERNAL_ERROR");
            throw exception;
        }
    }

    private String requireMaster() {
        authorization.requirePermission("wecom.retention.manage");
        authorization.requirePlatformComplianceScope();
        var actor = authorization.currentActor();
        if (actor == null || !authorization.isMasterAuthority(actor)) {
            throw ApiException.forbidden("WECOM_CLEANUP_MASTER_REQUIRED", "只能由最高权限本人执行清理");
        }
        return actor.id();
    }

    private WecomCleanupPreview requireOwner(WecomCleanupPreview preview, String actor) {
        if (!preview.createdBy().equals(actor)) {
            throw ApiException.forbidden("WECOM_CLEANUP_OWNER_REQUIRED", "清理必须由预览的本人完成");
        }
        return preview;
    }

    private void requirePending(WecomCleanupPreview preview) {
        if (preview.executedAt() != null) {
            throw ApiException.conflict("WECOM_CLEANUP_ALREADY_EXECUTED", "该预览已执行，不得重复清理");
        }
    }

    private String action(WecomCleanupPreview preview) { return "wecom-cleanup:" + preview.id(); }

    private void record(String event, WecomCleanupPreview preview) {
        audit.recordSuccess(event, "wecom.retention.manage", "wecom_cleanup", preview.id(), null, null,
                Map.of("scope_digest", preview.scopeDigest(), "message_count", preview.messageCount(),
                        "risk_hit_count", preview.riskHitCount(), "actor_id", preview.createdBy()));
    }
}
