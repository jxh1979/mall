package com.complex.platform.partner;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PartnerRepository {

    ExternalPartnerOrgRecord saveOrg(ExternalPartnerOrgRecord org);

    /** 3.2 跨项目读隔离：projectScope 非空时仅返回该项目的机构，null（平台管理员/全局平台员工）全量。 */
    List<ExternalPartnerOrgRecord> listOrgs(String projectScope);

    Optional<ExternalPartnerOrgRecord> findOrgById(String id);

    /** 某账号所属的机构 id（access_users.partner_org_id）。 */
    Optional<String> findPartnerOrgIdForUser(String userId);

    List<PartnerSubordinateRecord> listSubordinates(String partnerOrgId);

    Optional<PartnerSubordinateRecord> findSubordinate(String userId);

    long countActiveSubordinates(String partnerOrgId);

    /** 建下属账号（access_users 带 partner_org_id）+ 装配机构对应外包角色。 */
    PartnerSubordinateRecord createSubordinate(PartnerSubordinateRecord subordinate, String roleId);

    void disableSubordinate(String userId);

    /** 时效：机构到期→机构 expired + 其下属账号 disabled，返回处理的下属数。 */
    int disableExpiredOrgsAndSubordinates(Instant now);
}
