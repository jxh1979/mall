package com.complex.platform.partner;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.partner.dto.ExternalPartnerOrgRequest;
import com.complex.platform.partner.dto.PartnerSubordinateRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

/**
 * 外部协作机构与自助下属门户（条目2 业务层）：
 * 平台管理员维护机构（含下属上限/账号时效）；机构管理员在上限内自助开通/停用下属账号、下属继承机构范围。
 */
@Service
public class PartnerService {

    private final JdbcPartnerRepository repository;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;

    public PartnerService(JdbcPartnerRepository repository, AuthorizationService authorizationService,
            AuditEventService auditEventService) {
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
    }

    // ---- 平台管理员：机构 CRUD ----

    public PageResponse<ExternalPartnerOrgRecord> listPartnerOrgs() {
        authorizationService.requirePlatformScope();
        // 3.2 跨项目读隔离：项目员工只见本项目机构，平台管理员/全局平台员工全量。
        List<ExternalPartnerOrgRecord> rows = repository.listOrgs(
                authorizationService.currentProjectScopeQuietly().orElse(null));
        auditEventService.recordSuccess("partner_org.viewed", "partner.org.view", "partner_org", "all",
                null, null, Map.of("count", rows.size()));
        return new PageResponse<>(rows, 1, rows.size(), rows.size());
    }

    public ExternalPartnerOrgRecord createPartnerOrg(ExternalPartnerOrgRequest request) {
        authorizationService.requirePlatformScope();
        String name = requireText(request == null ? null : request.orgName(), "org_name");
        String category = requireText(request == null ? null : request.orgCategory(), "org_category");
        Instant now = Instant.now();
        ExternalPartnerOrgRecord org = new ExternalPartnerOrgRecord(
                "epo_" + UUID.randomUUID(), trimToNull(request.projectId()), name, category,
                trimToNull(request.linkedSupplierId()),
                request.maxSubordinates() == null ? 0 : Math.max(0, request.maxSubordinates()),
                request.validUntil(), trimToDefault(request.status(), "active"), now, now);
        ExternalPartnerOrgRecord saved = repository.saveOrg(org);
        auditEventService.recordSuccess("partner_org.updated", "partner.org.manage", "partner_org", saved.id(),
                null, request.idempotencyKey(), Map.of("org_name", name, "max_subordinates", saved.maxSubordinates()));
        return saved;
    }

    /**
     * AUD-033 账号绑机构：顶层外包账号（平台代建）此前无任何入口写 partner_org_id，导致外包模块按机构
     * 过滤时整段不可用。仅平台侧（partner.org.manage 权限，控制器闸）可绑；机构须存在且 active；
     * 覆盖旧绑定（换机构=重绑）。审计留痕成功/失败事件。
     */
    public void bindUserToOrg(String orgId, String userId) {
        try {
            if (userId == null || userId.isBlank()) {
                throw ApiException.badRequest("VALIDATION_FAILED", "user_id is required");
            }
            ExternalPartnerOrgRecord org = repository.findOrgById(orgId)
                    .orElseThrow(() -> ApiException.notFound("PARTNER_ORG_NOT_FOUND", "合作方组织不存在或已被删除"));
            if (!"active".equals(org.status())) {
                throw ApiException.conflict("PARTNER_ORG_NOT_ACTIVE", "合作方组织未启用");
            }
            String accountType = repository.findAccessUserAccountType(userId.trim())
                    .orElseThrow(() -> ApiException.notFound("ACCESS_USER_NOT_FOUND", "账号不存在或已被删除"));
            // 复核 Important:只允许外包类账号入机构——机构管理员可停用其"下属"、机构到期批量禁用挂靠账号,
            // 平台/员工/商户账号被误绑=外部方可停用平台账号(权限倒挂)+配额被吃+名单外泄。
            if (!"outsourced_vendor".equals(accountType)) {
                throw ApiException.badRequest("ACCESS_USER_NOT_OUTSOURCED",
                        "只有外包供应商账号才能绑定合作方组织");
            }
            repository.updateUserPartnerOrg(userId.trim(), orgId);
            auditEventService.recordSuccess("partner.org.user_bound", "partner.org.manage",
                    "external_partner_org", orgId, null, null,
                    java.util.Map.of("user_id", userId.trim()));
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("partner.org.user_bind_failed", "partner.org.manage",
                    "external_partner_org", orgId, null, null,
                    exception instanceof ApiException apiException ? apiException.code() : "INTERNAL_ERROR");
            throw exception;
        }
    }

    /**
     * 2026-07-18 收尾：解绑机构入口（bind 的逆操作，此前只能换绑不能清空）。
     * 校验该账号当前绑的就是这个机构（防误解他人绑定）；机构状态不限（停用/过期机构也要能解绑）。
     */
    public void unbindUserFromOrg(String orgId, String userId) {
        try {
            if (userId == null || userId.isBlank()) {
                throw ApiException.badRequest("VALIDATION_FAILED", "user_id is required");
            }
            repository.findOrgById(orgId)
                    .orElseThrow(() -> ApiException.notFound("PARTNER_ORG_NOT_FOUND", "合作方组织不存在或已被删除"));
            String trimmedUserId = userId.trim();
            repository.findAccessUserAccountType(trimmedUserId)
                    .orElseThrow(() -> ApiException.notFound("ACCESS_USER_NOT_FOUND", "账号不存在或已被删除"));
            // CAS 解绑（复核建议）：条件更新原子判定「当前绑的就是该机构」，0 行=未绑/已被并发换绑,409。
            if (repository.unbindUserPartnerOrg(trimmedUserId, orgId) == 0) {
                throw ApiException.conflict("PARTNER_ORG_USER_NOT_BOUND", "该账号未绑定到此合作方组织");
            }
            auditEventService.recordSuccess("partner.org.user_unbound", "partner.org.manage",
                    "external_partner_org", orgId, null, null,
                    Map.of("user_id", trimmedUserId));
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("partner.org.user_unbind_failed", "partner.org.manage",
                    "external_partner_org", orgId, null, null,
                    exception instanceof ApiException apiException ? apiException.code() : "INTERNAL_ERROR");
            throw exception;
        }
    }

    // ---- 机构管理员：自助管下属 ----

    public PageResponse<PartnerSubordinateRecord> listMySubordinates() {
        String orgId = currentPartnerOrgId();
        List<PartnerSubordinateRecord> rows = repository.listSubordinates(orgId);
        auditEventService.recordSuccess("partner_subordinate.viewed", "partner.subordinate.view",
                "partner_org", orgId, null, null, Map.of("count", rows.size()));
        return new PageResponse<>(rows, 1, rows.size(), rows.size());
    }

    public PartnerSubordinateRecord createSubordinate(PartnerSubordinateRequest request) {
        String orgId = currentPartnerOrgId();
        ExternalPartnerOrgRecord org = repository.findOrgById(orgId)
                .orElseThrow(() -> ApiException.notFound("PARTNER_ORG_NOT_FOUND", "合作方组织不存在或已被删除"));
        if (!"active".equals(org.status())) {
            throw ApiException.conflict("PARTNER_ORG_INACTIVE", "合作方组织未启用");
        }
        if (org.validUntil() != null && org.validUntil().isBefore(Instant.now())) {
            throw ApiException.conflict("PARTNER_ORG_EXPIRED", "合作方组织账号有效期已过");
        }
        if (org.maxSubordinates() <= 0 || repository.countActiveSubordinates(orgId) >= org.maxSubordinates()) {
            throw ApiException.conflict("PARTNER_SUBORDINATE_LIMIT_REACHED",
                    "该合作方组织的下属账号数量已达上限");
        }
        String username = requireText(request == null ? null : request.username(), "username");
        Instant now = Instant.now();
        PartnerSubordinateRecord sub = new PartnerSubordinateRecord(
                "epu_" + UUID.randomUUID(), username, orgId, "outsourced_vendor", "active", now);
        PartnerSubordinateRecord saved = repository.createSubordinate(sub, subordinateRoleForCategory(org.orgCategory()));
        auditEventService.recordSuccess("partner_subordinate.created", "partner.subordinate.manage",
                "access_user", saved.id(), null, request.idempotencyKey(), Map.of("partner_org_id", orgId));
        return saved;
    }

    public PartnerSubordinateRecord disableSubordinate(String userId) {
        String orgId = currentPartnerOrgId();
        PartnerSubordinateRecord sub = repository.findSubordinate(requireText(userId, "user_id"))
                .filter(s -> orgId.equals(s.partnerOrgId()))
                .orElseThrow(() -> ApiException.forbidden("PARTNER_SUBORDINATE_FORBIDDEN",
                        "该下属账号不属于当前合作方组织"));
        repository.disableSubordinate(userId);
        auditEventService.recordSuccess("partner_subordinate.disabled", "partner.subordinate.manage",
                "access_user", userId, null, null, Map.of("partner_org_id", orgId));
        return new PartnerSubordinateRecord(sub.id(), sub.username(), sub.partnerOrgId(), sub.accountType(),
                "disabled", sub.createdAt());
    }

    /** 时效到期停用任务（系统发起，无 auth 上下文）：机构过期→机构 expired + 其下属 disabled。 */
    public int expireOverdueAccounts(Instant now) {
        return repository.disableExpiredOrgsAndSubordinates(now);
    }

    private String currentPartnerOrgId() {
        String actorId = authorizationService.currentActor().id();
        return repository.findPartnerOrgIdForUser(actorId)
                .orElseThrow(() -> ApiException.forbidden("PARTNER_ORG_NOT_BOUND",
                        "当前账号未绑定合作方组织"));
    }

    private String subordinateRoleForCategory(String category) {
        return switch (category == null ? "" : category) {
            case "security_cleaning" -> "role_outsourced_security_cleaning";
            case "construction" -> "role_outsourced_construction";
            default -> "role_outsourced_facility";
        };
    }

    private String trimToDefault(String value, String def) {
        String t = value == null ? "" : value.trim();
        return t.isEmpty() ? def : t;
    }
}
