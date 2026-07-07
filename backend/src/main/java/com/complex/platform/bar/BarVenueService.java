package com.complex.platform.bar;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 酒吧门店主数据服务（V263）：门店 CRUD + 存档 + 审批后删除（照 V262 BarPosCatalogService 范式）。
 * 租户隔离：按 project scope 过滤/守卫（bars.project_id）。删除生命周期走 auditedWrite 留痕。
 */
@Service
public class BarVenueService {

    private final JdbcBarVenueRepository repository;
    private final AuthorizationService authorizationService;
    private final BarOperationExecutor operations;

    public BarVenueService(JdbcBarVenueRepository repository,
            AuthorizationService authorizationService,
            BarOperationExecutor operations) {
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.operations = operations;
    }

    public List<BarVenueRecord> list(boolean includeArchived) {
        return repository.list(authorizationService.currentProjectScopeQuietly(), includeArchived);
    }

    @Transactional
    public BarVenueRecord create(BarVenueRequest request) {
        BarVenueRequest r = require(request);
        Instant now = Instant.now();
        String projectId = requireText(r.projectId(), "project_id");
        // 项目员工新建只能落在自己项目下；平台管理员（scope empty）可指定任意项目。
        Optional<String> scope = authorizationService.currentProjectScopeQuietly();
        if (scope.isPresent() && !scope.get().equals(projectId)) {
            throw ApiException.forbidden("BAR_VENUE_PROJECT_SCOPE", "cannot create a bar in another project");
        }
        return repository.save(new BarVenueRecord(
                "bar_" + slug(),
                projectId,
                trimToNull(r.merchantId()),
                trimToNull(r.spaceId()),
                requireText(r.barName(), "bar_name"),
                trimToNull(r.barCode()),
                nonNegativeOrNull(r.minimumSpendCent(), "minimum_spend_cent"),
                statusOrDefault(r.status()),
                now, now, null, null, "none", null, null, null));
    }

    @Transactional
    public BarVenueRecord update(String id, BarVenueRequest request) {
        BarVenueRequest r = require(request);
        BarVenueRecord existing = requireVenue(id);
        checkOwnership(existing.projectId());
        if (existing.archivedAt() != null) {
            throw ApiException.conflict("BAR_VENUE_ARCHIVED", "bar is archived; unarchive before editing");
        }
        Instant now = Instant.now();
        // project_id 不允许在编辑里跨项目搬迁（越权面）；其余字段可改。
        return repository.save(new BarVenueRecord(
                existing.id(),
                existing.projectId(),
                r.merchantId() == null ? existing.merchantId() : trimToNull(r.merchantId()),
                r.spaceId() == null ? existing.spaceId() : trimToNull(r.spaceId()),
                r.barName() == null ? existing.barName() : requireText(r.barName(), "bar_name"),
                r.barCode() == null ? existing.barCode() : trimToNull(r.barCode()),
                r.minimumSpendCent() == null ? existing.minimumSpendCent()
                        : nonNegativeOrNull(r.minimumSpendCent(), "minimum_spend_cent"),
                r.status() == null ? existing.status() : statusOrDefault(r.status()),
                existing.createdAt(), now,
                existing.archivedAt(), existing.archivedBy(), existing.deleteStatus(),
                existing.deleteRequestedBy(), existing.deleteRequestedAt(), existing.deleteReason()));
    }

    @Transactional
    public BarVenueRecord archive(String id, boolean archive) {
        BarVenueRecord bar = requireVenue(id);
        checkOwnership(bar.projectId());
        Instant now = Instant.now();
        if (archive) {
            return repository.save(bar.withArchive(now, actorId(), now));
        }
        // 取消存档：清存档标记，并把 pending 删除申请回退 none。
        return repository.save(bar.withArchive(null, null, now).withDelete("none", null, null, null, now));
    }

    @Transactional
    public BarVenueRecord requestDelete(String id, BarCatalogDeleteRequest request) {
        return operations.auditedWrite(BarWriteOperation.REQUEST_DELETE_BAR_VENUE, () -> doRequestDelete(id, request));
    }

    private BarVenueRecord doRequestDelete(String id, BarCatalogDeleteRequest request) {
        BarVenueRecord bar = requireVenue(id);
        checkOwnership(bar.projectId());
        if (bar.archivedAt() == null) {
            throw ApiException.badRequest("BAR_VENUE_NOT_ARCHIVED", "bar must be archived before requesting deletion");
        }
        if ("pending".equals(bar.deleteStatus())) {
            throw ApiException.conflict("BAR_VENUE_DELETE_ALREADY_PENDING", "a delete request is already pending");
        }
        Instant now = Instant.now();
        String reason = request == null ? null : trimToNull(request.reason());
        return repository.save(bar.withDelete("pending", actorId(), now, reason, now));
    }

    @Transactional
    public BarCatalogDeleteDecision approveDelete(String id, BarCatalogDeleteApprovalRequest request) {
        return operations.auditedWrite(BarWriteOperation.APPROVE_DELETE_BAR_VENUE, () -> doApproveDelete(id, request));
    }

    private BarCatalogDeleteDecision doApproveDelete(String id, BarCatalogDeleteApprovalRequest request) {
        BarVenueRecord bar = requireVenue(id);
        checkOwnership(bar.projectId());
        if (!"pending".equals(bar.deleteStatus())) {
            throw ApiException.badRequest("BAR_VENUE_DELETE_NOT_PENDING", "no pending delete request");
        }
        String decision = decisionOf(request);
        forbidSelfApprove(bar.deleteRequestedBy());
        if ("rejected".equals(decision)) {
            repository.save(bar.withDelete("none", null, null, null, Instant.now()));
            return new BarCatalogDeleteDecision(id, "rejected", false);
        }
        long refs = repository.countReferences(id);
        if (refs > 0) {
            throw ApiException.conflict("BAR_VENUE_IN_USE",
                    "bar is still referenced by " + refs + " operating records");
        }
        repository.delete(id);
        return new BarCatalogDeleteDecision(id, "approved", true);
    }

    // ---------- helpers ----------

    private BarVenueRecord requireVenue(String id) {
        return repository.find(requireText(id, "id"))
                .orElseThrow(() -> ApiException.notFound("BAR_VENUE_NOT_FOUND", "bar not found"));
    }

    private static String decisionOf(BarCatalogDeleteApprovalRequest request) {
        String decision = request == null || request.decision() == null
                ? null : request.decision().trim().toLowerCase(Locale.ROOT);
        if (!"approved".equals(decision) && !"rejected".equals(decision)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "decision must be approved or rejected");
        }
        return decision;
    }

    /** 职责分离：删除申请发起人不能审批自己的申请。actorId 为 null（测试/无鉴权）时跳过。 */
    private void forbidSelfApprove(String requestedBy) {
        String actor = actorId();
        if (actor != null && actor.equals(requestedBy)) {
            throw ApiException.forbidden("BAR_VENUE_SELF_APPROVE_FORBIDDEN",
                    "the initiator of a delete request cannot approve it");
        }
    }

    /** 项目隔离守卫：项目员工只能操作本项目酒吧；平台管理员（scope empty）放行。 */
    private void checkOwnership(String resourceProjectId) {
        Optional<String> scope = authorizationService.currentProjectScopeQuietly();
        if (scope.isPresent() && !scope.get().equals(resourceProjectId)) {
            throw ApiException.forbidden("BAR_VENUE_PROJECT_SCOPE", "bar belongs to another project");
        }
    }

    private String actorId() {
        try {
            return authorizationService.currentActor().id();
        } catch (RuntimeException unauthenticated) {
            return null;
        }
    }

    private static <T> T require(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private static String requireText(String value, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        return trimmed;
    }

    private static Long nonNegativeOrNull(Long value, String field) {
        if (value == null) {
            return null;
        }
        if (value < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " must be >= 0");
        }
        return value;
    }

    private static String statusOrDefault(String status) {
        String trimmed = trimToNull(status);
        return trimmed == null ? "active" : trimmed;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String slug() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
