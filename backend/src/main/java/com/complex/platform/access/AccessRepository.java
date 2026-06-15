package com.complex.platform.access;

import java.time.Instant;
import java.util.List;

public interface AccessRepository {

    List<PermissionDefinition> listPermissions();

    List<PermissionTemplate> listTemplates();

    PermissionTemplate saveTemplate(PermissionTemplate template, List<PermissionTemplateItem> items);

    PermissionTemplate copyTemplate(String sourceTemplateId, PermissionTemplate copy);

    List<PermissionTemplateItem> findTemplateItems(String templateId);

    AccessUser createUser(AccessUser user);

    List<AccessUser> listUsers();

    List<RoleRecord> listRoles();

    RoleRecord createRole(RoleRecord role);

    List<PermissionTemplateApplication> applyTemplate(
            String templateId,
            List<String> targetUserIds,
            Instant validFrom,
            Instant validUntil,
            boolean singleUse,
            String appliedBy
    );

    TemporaryGrant createTemporaryGrant(TemporaryGrant temporaryGrant);

    TemporaryGrant revokeTemporaryGrant(String id, String revokedBy, Instant revokedAt);

    boolean permissionExists(String permissionCode);

    List<PermissionEffect> findPermissionEffects(String userId, Instant checkedAt);

    void markSingleUsePermissionConsumed(String sourceId, String permissionCode, String userId, Instant consumedAt);

    enum Effect {
        GRANT,
        DENY
    }

    enum Source {
        ROLE,
        TEMPLATE_APPLICATION,
        TEMPORARY_GRANT,
        USER_OVERRIDE
    }

    record PermissionEffect(
            String userId,
            String permissionCode,
            Effect effect,
            Source source,
            String sourceId,
            Instant validFrom,
            Instant validUntil,
            boolean singleUse,
            boolean consumed
    ) {
    }
}
