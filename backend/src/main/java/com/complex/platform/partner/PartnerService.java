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

/**
 * 外部协作机构与自助下属门户（条目2 业务层）：
 * 平台管理员维护机构（含下属上限/账号时效）；机构管理员在上限内自助开通/停用下属账号、下属继承机构范围。
 */
@Service
public class PartnerService {

    private final PartnerRepository repository;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;

    public PartnerService(PartnerRepository repository, AuthorizationService authorizationService,
            AuditEventService auditEventService) {
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
    }

    // ---- 平台管理员：机构 CRUD ----

    public PageResponse<ExternalPartnerOrgRecord> listPartnerOrgs() {
        authorizationService.requirePlatformScope();
        List<ExternalPartnerOrgRecord> rows = repository.listOrgs();
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
                .orElseThrow(() -> ApiException.notFound("PARTNER_ORG_NOT_FOUND", "partner org not found"));
        if (!"active".equals(org.status())) {
            throw ApiException.conflict("PARTNER_ORG_INACTIVE", "partner org is not active");
        }
        if (org.validUntil() != null && org.validUntil().isBefore(Instant.now())) {
            throw ApiException.conflict("PARTNER_ORG_EXPIRED", "partner org account validity has expired");
        }
        if (org.maxSubordinates() <= 0 || repository.countActiveSubordinates(orgId) >= org.maxSubordinates()) {
            throw ApiException.conflict("PARTNER_SUBORDINATE_LIMIT_REACHED",
                    "subordinate account limit reached for this partner org");
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
                        "subordinate does not belong to your partner org"));
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
                        "current account is not bound to a partner org"));
    }

    private String subordinateRoleForCategory(String category) {
        return switch (category == null ? "" : category) {
            case "security_cleaning" -> "role_outsourced_security_cleaning";
            case "construction" -> "role_outsourced_construction";
            default -> "role_outsourced_facility";
        };
    }

    private String requireText(String value, String field) {
        String t = value == null ? "" : value.trim();
        if (t.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        return t;
    }

    private String trimToNull(String value) {
        String t = value == null ? "" : value.trim();
        return t.isEmpty() ? null : t;
    }

    private String trimToDefault(String value, String def) {
        String t = value == null ? "" : value.trim();
        return t.isEmpty() ? def : t;
    }
}
