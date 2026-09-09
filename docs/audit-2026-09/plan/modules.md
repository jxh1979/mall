# 审计模块切分

版本：V0.04

- 当前 main 基线：`242f487233d028173cb9198eecd079f5d877405c`。
- 模块总数：471；受审文件：2986。
- 按业务域、技术层重新聚合；域内按关联文件与当前物理行数分段，禁止跨业务域按全仓字母序硬切。
- 历史 87 模块仅提供业务名称和文件归属线索；审核要求唯一取操作员的 V0.17 提示词。
- 本计划新编号不继承任一旧同号状态；后附两套旧编号交叉索引。历史提交、修复和报告均保留。
- 未发布稿按后附材料表只读复审，不混入 main 的 scope.txt。

## M001 — 业务需求正本／需求HTML／第1段

- 聚合维度：`业务需求正本／需求HTML`
- 文件数：2
- 有效内容行数：2340
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/business-enum-quick-reference-v0.1.html
docs/business-system-map-for-operations-v0.1.html
<!-- FILES-END -->

## M002 — 业务需求正本／需求HTML／第2段

- 聚合维度：`业务需求正本／需求HTML`
- 文件数：1
- 有效内容行数：979
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/platform-governance-map-for-operations-v0.1.html
<!-- FILES-END -->

## M003 — 治理门禁与 CI 基线／配置与工程入口／第1段

- 聚合维度：`治理门禁与 CI 基线／配置与工程入口`
- 文件数：2
- 有效内容行数：202
- 标记：NORMAL

<!-- FILES-BEGIN -->
.gitignore
package.json
<!-- FILES-END -->

## M004 — 治理门禁与 CI 基线／配置与工程入口／第2段

- 聚合维度：`治理门禁与 CI 基线／配置与工程入口`
- 文件数：1
- 有效内容行数：3321
- 标记：OVERSIZE-SINGLE-FILE

<!-- FILES-BEGIN -->
package-lock.json
<!-- FILES-END -->

## M005 — 治理门禁与 CI 基线／配置与工程入口／第3段

- 聚合维度：`治理门禁与 CI 基线／配置与工程入口`
- 文件数：3
- 有效内容行数：52
- 标记：NORMAL

<!-- FILES-BEGIN -->
tsconfig.base.json
.github/CODEOWNERS
.github/workflows/predeploy-gates.yml
<!-- FILES-END -->

## M006 — 治理门禁与 CI 基线／文档与接口定义／第1段

- 聚合维度：`治理门禁与 CI 基线／文档与接口定义`
- 文件数：2
- 有效内容行数：207
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/delivery-readiness-gates-v0.1.md
docs/deploy-and-change-flow-v0.1.md
<!-- FILES-END -->

## M007 — 治理门禁与 CI 基线／运行和发布脚本／第1段

- 聚合维度：`治理门禁与 CI 基线／运行和发布脚本`
- 文件数：5
- 有效内容行数：156
- 标记：NORMAL

<!-- FILES-BEGIN -->
ops/160/install-git-hooks.sh
ops/160/merge-pr.sh
ops/160/propose-change.sh
ops/160/hooks/pre-push
ops/ci/predeploy-gates.yml
<!-- FILES-END -->

## M008 — 治理门禁与 CI 基线／检查及生成工具／第1段

- 聚合维度：`治理门禁与 CI 基线／检查及生成工具`
- 文件数：17
- 有效内容行数：1530
- 标记：NORMAL

<!-- FILES-BEGIN -->
tools/html-param-tier-baseline.json
tools/refresh-entry-doc-baseline.mjs
tools/verify-delivery-gates.mjs
tools/verify-entry-anchors.mjs
tools/verify-html-ledger-sync.mjs
tools/verify-html-param-tier.mjs
tools/gate-hardening-tests/99-predeploy-final.log
tools/gate-hardening-tests/A-worktree-uncommitted.log
tools/gate-hardening-tests/B-committed.log
tools/gate-hardening-tests/C-no-baseline.log
tools/gate-hardening-tests/D-baseline-legacy-pass.log
tools/gate-hardening-tests/E-ignore-marker.log
tools/gate-hardening-tests/F-ci-baseline-evidence.log
tools/gate-hardening-tests/G-merge-pr-guards.log
tools/gate-hardening-tests/I1-baseline-ratchet.log
tools/gate-hardening-tests/I2-ignore-granularity.log
tools/gate-hardening-tests/m001-regression.mjs
<!-- FILES-END -->

## M009 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台实现／第1段

- 聚合维度：`综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台实现`
- 文件数：20
- 有效内容行数：2476
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java
backend/src/main/java/com/complex/platform/governance/BusinessEntityBindingRecord.java
backend/src/main/java/com/complex/platform/governance/MerchantRecord.java
backend/src/main/java/com/complex/platform/governance/MerchantDeleteDecision.java
backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java
backend/src/main/java/com/complex/platform/governance/PlatformController.java
backend/src/main/java/com/complex/platform/governance/PlatformRepository.java
backend/src/main/java/com/complex/platform/governance/PlatformService.java
backend/src/main/java/com/complex/platform/governance/package-info.java
backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityBindingRequest.java
backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java
backend/src/main/java/com/complex/platform/governance/dto/MerchantArchiveRequest.java
backend/src/main/java/com/complex/platform/governance/dto/MerchantDeleteApprovalRequest.java
backend/src/main/java/com/complex/platform/governance/dto/MerchantDeleteRequest.java
backend/src/main/java/com/complex/platform/governance/dto/MerchantProfileRequest.java
backend/src/main/java/com/complex/platform/governance/dto/ProjectRequest.java
backend/src/main/java/com/complex/platform/governance/dto/SpaceRequest.java
backend/src/main/java/com/complex/platform/governance/dto/SpaceTypeRequest.java
backend/src/main/java/com/complex/platform/governance/dto/StatusChangeRequest.java
backend/src/main/java/com/complex/platform/governance/dto/VenueBusinessHoursRequest.java
<!-- FILES-END -->

## M010 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／数据库迁移／第1段

- 聚合维度：`综合体/集团/项目公司/业态主体拓扑与四类主体绑定／数据库迁移`
- 文件数：4
- 有效内容行数：203
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V005__p0_platform_master_data.sql
backend/src/main/resources/db/migration/V048__access_role_organization_isolation.sql
backend/src/main/resources/db/migration/V318__business_entity_parent_layer.sql
backend/src/main/resources/db/migration/V354__business_entity_master_data.sql
<!-- FILES-END -->

## M011 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台验证／第1段

- 聚合维度：`综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台验证`
- 文件数：3
- 有效内容行数：1342
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java
backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
<!-- FILES-END -->

## M012 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／网页与验证／第1段

- 聚合维度：`综合体/集团/项目公司/业态主体拓扑与四类主体绑定／网页与验证`
- 文件数：6
- 有效内容行数：1485
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/GovernanceTopologyPage.tsx
apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx
apps/web/src/admin/pages/OrganizationMerchantPage.tsx
apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
apps/web/src/admin/pages/SubjectBindingsPage.tsx
apps/web/src/admin/pages/SubjectBindingsPage.test.tsx
<!-- FILES-END -->

## M013 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第1段

- 聚合维度：`综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义`
- 文件数：4
- 有效内容行数：1517
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/business-entity-binding-development-design-v0.1.md
docs/group-governance-rebuild-prd-v1.0.md
docs/platform-governance-acceptance-pack-v0.2.md
docs/platform-governance-prd-v0.1.md
<!-- FILES-END -->

## M014 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第2段

- 聚合维度：`综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义`
- 文件数：1
- 有效内容行数：3494
- 标记：OVERSIZE-SINGLE-FILE

<!-- FILES-BEGIN -->
docs/platform-governance-product-spec-v0.2.md
<!-- FILES-END -->

## M015 — 空间点位、楼层平面图与营业时段／后台实现／第1段

- 聚合维度：`空间点位、楼层平面图与营业时段／后台实现`
- 文件数：23
- 有效内容行数：1999
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/floorplan/FloorPlanController.java
backend/src/main/java/com/complex/platform/floorplan/FloorPlanRecord.java
backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java
backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java
backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportCandidateRecord.java
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportDetailsResponse.java
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportErrorRecord.java
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParseResult.java
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java
backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportTaskRecord.java
backend/src/main/java/com/complex/platform/floorplan/FloorPlanPoint.java
backend/src/main/java/com/complex/platform/floorplan/FloorPlanShapeRecord.java
backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanCreateRequest.java
backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanImportCreateRequest.java
backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanImportPublishRequest.java
backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanShapeSaveRequest.java
backend/src/main/java/com/complex/platform/governance/ProjectRecord.java
backend/src/main/java/com/complex/platform/governance/SpaceRecord.java
backend/src/main/java/com/complex/platform/governance/SpaceTypeRecord.java
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRecord.java
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java
backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java
<!-- FILES-END -->

## M016 — 空间点位、楼层平面图与营业时段／数据库迁移／第1段

- 聚合维度：`空间点位、楼层平面图与营业时段／数据库迁移`
- 文件数：4
- 有效内容行数：164
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V023__p1_floor_plan_polygons.sql
backend/src/main/resources/db/migration/V034__p2_floor_plan_import_tasks.sql
backend/src/main/resources/db/migration/V184__space_type_dictionary.sql
backend/src/main/resources/db/migration/V203__spaces_status_check.sql
<!-- FILES-END -->

## M017 — 空间点位、楼层平面图与营业时段／后台验证／第1段

- 聚合维度：`空间点位、楼层平面图与营业时段／后台验证`
- 文件数：5
- 有效内容行数：1352
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java
backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java
backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java
backend/src/test/java/com/complex/platform/governance/VenueBusinessHoursServiceTests.java
<!-- FILES-END -->

## M018 — 空间点位、楼层平面图与营业时段／网页与验证／第1段

- 聚合维度：`空间点位、楼层平面图与营业时段／网页与验证`
- 文件数：7
- 有效内容行数：1080
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/FloorPlanImportPage.tsx
apps/web/src/admin/pages/FloorPlanImportPage.guidedcreate.test.tsx
apps/web/src/admin/pages/FloorPlanImportPage.test.tsx
apps/web/src/admin/pages/SpaceTypesPage.tsx
apps/web/src/admin/pages/SpaceTypesPage.guidedcreate.test.tsx
apps/web/src/admin/pages/VenueBusinessHoursPage.tsx
apps/web/src/admin/pages/VenueBusinessHoursPage.test.tsx
<!-- FILES-END -->

## M019 — 空间点位、楼层平面图与营业时段／文档与接口定义／第1段

- 聚合维度：`空间点位、楼层平面图与营业时段／文档与接口定义`
- 文件数：4
- 有效内容行数：680
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/p1-floor-plan-polygon-api-contract-v0.1.md
docs/p2-floor-plan-import-api-contract-v0.1.md
docs/p2-floor-plan-import-completion-audit-v0.1.md
docs/superpowers/plans/2026-06-15-p2-floor-plan-svg-json-import.md
<!-- FILES-END -->

## M020 — 商户档案、商户类型与商户侧门户／后台实现／第1段

- 聚合维度：`商户档案、商户类型与商户侧门户／后台实现`
- 文件数：4
- 有效内容行数：242
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/merchantportal/MerchantContractView.java
backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalController.java
backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalService.java
backend/src/main/java/com/complex/platform/merchantportal/dto/MerchantSettlementDisputeRequest.java
<!-- FILES-END -->

## M021 — 商户档案、商户类型与商户侧门户／数据库迁移／第1段

- 聚合维度：`商户档案、商户类型与商户侧门户／数据库迁移`
- 文件数：5
- 有效内容行数：103
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V147__merchants_owner_person.sql
backend/src/main/resources/db/migration/V199__merchant_portal_zone_module_switch.sql
backend/src/main/resources/db/migration/V200__merchant_portal_zone_permissions.sql
backend/src/main/resources/db/migration/V348__merchant_business_format.sql
backend/src/main/resources/db/migration/V384__merchant_business_format_media.sql
<!-- FILES-END -->

## M022 — 商户档案、商户类型与商户侧门户／后台验证／第1段

- 聚合维度：`商户档案、商户类型与商户侧门户／后台验证`
- 文件数：2
- 有效内容行数：227
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/merchantportal/MerchantPortalServiceTests.java
<!-- FILES-END -->

## M023 — 商户档案、商户类型与商户侧门户／网页与验证／第1段

- 聚合维度：`商户档案、商户类型与商户侧门户／网页与验证`
- 文件数：6
- 有效内容行数：991
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/MerchantScopePage.tsx
apps/web/src/admin/pages/MerchantScopePage.test.tsx
apps/web/src/admin/pages/MerchantZoneContractsPage.tsx
apps/web/src/admin/pages/MerchantZoneContractsPage.test.tsx
apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx
apps/web/src/admin/pages/MerchantZoneSettlementsPage.test.tsx
<!-- FILES-END -->

## M024 — 商户档案、商户类型与商户侧门户／文档与接口定义／第1段

- 聚合维度：`商户档案、商户类型与商户侧门户／文档与接口定义`
- 文件数：1
- 有效内容行数：164
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/merchant-portal-zone-prd-v0.1.md
<!-- FILES-END -->

## M025 — 商户档案、商户类型与商户侧门户／运行和发布脚本／第1段

- 聚合维度：`商户档案、商户类型与商户侧门户／运行和发布脚本`
- 文件数：1
- 有效内容行数：55
- 标记：NORMAL

<!-- FILES-BEGIN -->
ops/160/seed-dev-bars.sh
<!-- FILES-END -->

## M026 — 租户范围与数据隔离（技术不变量）／后台实现／第1段

- 聚合维度：`租户范围与数据隔离（技术不变量）／后台实现`
- 文件数：1
- 有效内容行数：29
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/common/jdbc/JdbcTimestamps.java
<!-- FILES-END -->

## M027 — 租户范围与数据隔离（技术不变量）／数据库迁移／第1段

- 聚合维度：`租户范围与数据隔离（技术不变量）／数据库迁移`
- 文件数：13
- 有效内容行数：375
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V044__finance_security_isolation.sql
backend/src/main/resources/db/migration/V045__evidence_security_isolation.sql
backend/src/main/resources/db/migration/V046__inventory_security_fixes.sql
backend/src/main/resources/db/migration/V047__bar_security_merchant_isolation.sql
backend/src/main/resources/db/migration/V049__amusement_security_fixes.sql
backend/src/main/resources/db/migration/V051__compliance_security_fixes.sql
backend/src/main/resources/db/migration/V052__floorplan_merchant_isolation.sql
backend/src/main/resources/db/migration/V065__amusement_merchant_isolation_indexes.sql
backend/src/main/resources/db/migration/V174__refund_authority_project_isolation.sql
backend/src/main/resources/db/migration/V214__tenant_scope_staff_service_tables.sql
backend/src/main/resources/db/migration/V237__inventory_isolation_fixes.sql
backend/src/main/resources/db/migration/V243__ar_ap_project_isolation.sql
backend/src/main/resources/db/migration/V244__inventory_movement_batch_project_isolation.sql
<!-- FILES-END -->

## M028 — 租户范围与数据隔离（技术不变量）／文档与接口定义／第1段

- 聚合维度：`租户范围与数据隔离（技术不变量）／文档与接口定义`
- 文件数：2
- 有效内容行数：700
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
docs/domain-ownership-scope-audit-v0.1.csv
<!-- FILES-END -->

## M029 — 租户范围与数据隔离（技术不变量）／检查及生成工具／第1段

- 聚合维度：`租户范围与数据隔离（技术不变量）／检查及生成工具`
- 文件数：4
- 有效内容行数：729
- 标记：NORMAL

<!-- FILES-BEGIN -->
tools/generate-data-isolation-spec.mjs
tools/generate-domain-ownership-scope-audit.mjs
tools/verify-tenant-isolation.mjs
tools/verify-test-db-isolation.mjs
<!-- FILES-END -->

## M030 — 租户范围与数据隔离（技术不变量）／质量检查记录／第1段

- 聚合维度：`租户范围与数据隔离（技术不变量）／质量检查记录`
- 文件数：1
- 有效内容行数：22
- 标记：NORMAL

<!-- FILES-BEGIN -->
security-scans/tenant-isolation-baseline.json
<!-- FILES-END -->

## M031 — 功能权限点、职级三十档、权限模板与最终权限计算／后台实现／第1段

- 聚合维度：`功能权限点、职级三十档、权限模板与最终权限计算／后台实现`
- 文件数：3
- 有效内容行数：2930
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/access/AccessController.java
backend/src/main/java/com/complex/platform/access/AccessRepository.java
backend/src/main/java/com/complex/platform/access/AccessService.java
<!-- FILES-END -->

## M032 — 功能权限点、职级三十档、权限模板与最终权限计算／后台实现／第2段

- 聚合维度：`功能权限点、职级三十档、权限模板与最终权限计算／后台实现`
- 文件数：25
- 有效内容行数：2494
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java
backend/src/main/java/com/complex/platform/access/AccessActor.java
backend/src/main/java/com/complex/platform/access/AccessCredentialProvisioner.java
backend/src/main/java/com/complex/platform/access/JdbcAccessCredentialProvisioner.java
backend/src/main/java/com/complex/platform/access/AccessGrantPolicy.java
backend/src/main/java/com/complex/platform/access/AccessGrantPositionSummary.java
backend/src/main/java/com/complex/platform/access/AccessGrantRequestRecord.java
backend/src/main/java/com/complex/platform/access/AccessGrantSummary.java
backend/src/main/java/com/complex/platform/access/AccessGrantUserSummary.java
backend/src/main/java/com/complex/platform/access/AccessUser.java
backend/src/main/java/com/complex/platform/access/AuthorizationService.java
backend/src/main/java/com/complex/platform/access/EffectivePermissions.java
backend/src/main/java/com/complex/platform/access/PermissionDefinition.java
backend/src/main/java/com/complex/platform/access/PermissionTemplate.java
backend/src/main/java/com/complex/platform/access/PermissionTemplateApplication.java
backend/src/main/java/com/complex/platform/access/PermissionTemplateApplyResult.java
backend/src/main/java/com/complex/platform/access/PermissionTemplateItem.java
backend/src/main/java/com/complex/platform/access/PositionRoleTemplate.java
backend/src/main/java/com/complex/platform/access/RoleRecord.java
backend/src/main/java/com/complex/platform/access/RoleAssignmentRecord.java
backend/src/main/java/com/complex/platform/access/StaffLevelRecord.java
backend/src/main/java/com/complex/platform/access/StaffLevelAssignment.java
backend/src/main/java/com/complex/platform/access/UserPositionAssignment.java
backend/src/main/java/com/complex/platform/access/UserPositionAssignmentResult.java
backend/src/main/java/com/complex/platform/access/package-info.java
<!-- FILES-END -->

## M033 — 功能权限点、职级三十档、权限模板与最终权限计算／后台实现／第3段

- 聚合维度：`功能权限点、职级三十档、权限模板与最终权限计算／后台实现`
- 文件数：13
- 有效内容行数：229
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/access/dto/AccessGrantApplyRequest.java
backend/src/main/java/com/complex/platform/access/dto/AccessGrantReviewRequest.java
backend/src/main/java/com/complex/platform/access/dto/AccessUserRequest.java
backend/src/main/java/com/complex/platform/access/dto/PermissionTemplateApplyRequest.java
backend/src/main/java/com/complex/platform/access/dto/PermissionTemplateRequest.java
backend/src/main/java/com/complex/platform/access/dto/PositionRoleTemplateRequest.java
backend/src/main/java/com/complex/platform/access/dto/RoleAssignmentRequest.java
backend/src/main/java/com/complex/platform/access/dto/RoleRequest.java
backend/src/main/java/com/complex/platform/access/dto/StaffLevelAssignmentRequest.java
backend/src/main/java/com/complex/platform/access/dto/StatusChangeRequest.java
backend/src/main/java/com/complex/platform/access/dto/TemporaryGrantRequest.java
backend/src/main/java/com/complex/platform/access/dto/UserPermissionOverrideRequest.java
backend/src/main/java/com/complex/platform/access/dto/UserPositionAssignmentRequest.java
<!-- FILES-END -->

## M034 — 功能权限点、职级三十档、权限模板与最终权限计算／数据库迁移／第1段

- 聚合维度：`功能权限点、职级三十档、权限模板与最终权限计算／数据库迁移`
- 文件数：13
- 有效内容行数：690
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V003__p0_access_permission_authz_slice.sql
backend/src/main/resources/db/migration/V092__amusement_operational_config_permissions.sql
backend/src/main/resources/db/migration/V158__refund_authority_admin_permissions.sql
backend/src/main/resources/db/migration/V169__access_role_assignment_closure.sql
backend/src/main/resources/db/migration/V180__auditor_roles_readonly_seed.sql
backend/src/main/resources/db/migration/V204__seed_runtime_checked_permissions.sql
backend/src/main/resources/db/migration/V261__platform_admin_full_permissions.sql
backend/src/main/resources/db/migration/V270__ticket_manual_override_permission.sql
backend/src/main/resources/db/migration/V321__staff_self_profile_permission.sql
backend/src/main/resources/db/migration/V322__staff_self_service_role.sql
backend/src/main/resources/db/migration/V387__access_display_names_chinese.sql
backend/src/main/resources/db/migration/V393__sensitive_business_data_permissions_require_approval.sql
backend/src/main/resources/db/migration/V394__permission_risk_tiers_and_sensitive_codes_require_approval.sql
<!-- FILES-END -->

## M035 — 功能权限点、职级三十档、权限模板与最终权限计算／后台验证／第1段

- 聚合维度：`功能权限点、职级三十档、权限模板与最终权限计算／后台验证`
- 文件数：2
- 有效内容行数：1166
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/access/AccessControllerTests.java
backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java
<!-- FILES-END -->

## M036 — 功能权限点、职级三十档、权限模板与最终权限计算／后台验证／第2段

- 聚合维度：`功能权限点、职级三十档、权限模板与最终权限计算／后台验证`
- 文件数：3
- 有效内容行数：2849
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java
backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java
<!-- FILES-END -->

## M037 — 功能权限点、职级三十档、权限模板与最终权限计算／后台验证／第3段

- 聚合维度：`功能权限点、职级三十档、权限模板与最终权限计算／后台验证`
- 文件数：1
- 有效内容行数：785
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
<!-- FILES-END -->

## M038 — 功能权限点、职级三十档、权限模板与最终权限计算／网页与验证／第1段

- 聚合维度：`功能权限点、职级三十档、权限模板与最终权限计算／网页与验证`
- 文件数：16
- 有效内容行数：2787
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/AccessGovernancePage.tsx
apps/web/src/admin/pages/AccessGovernancePage.degraded.test.tsx
apps/web/src/admin/pages/AccessGovernancePage.guidedcreate.test.tsx
apps/web/src/admin/pages/AccessGovernancePage.scopechange.test.tsx
apps/web/src/admin/pages/AccessGovernancePage.test.tsx
apps/web/src/admin/pages/AccessUsersPage.tsx
apps/web/src/admin/pages/AccessUsersPage.metricdetail.test.tsx
apps/web/src/admin/pages/AccessUsersPage.partnerbind.test.tsx
apps/web/src/admin/pages/AccessUsersPage.robustness.test.tsx
apps/web/src/admin/pages/AccessUsersPage.test.tsx
apps/web/src/admin/pages/PermissionTemplatesPage.tsx
apps/web/src/admin/pages/PermissionTemplatesPage.guidedcreate.test.tsx
apps/web/src/admin/pages/PermissionTemplatesPage.test.tsx
apps/web/src/admin/security/Can.tsx
apps/web/src/admin/security/PermissionGate.tsx
apps/web/src/admin/security/PermissionGate.test.ts
<!-- FILES-END -->

## M039 — 功能权限点、职级三十档、权限模板与最终权限计算／文档与接口定义／第1段

- 聚合维度：`功能权限点、职级三十档、权限模板与最终权限计算／文档与接口定义`
- 文件数：4
- 有效内容行数：860
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/p0-operation-permission-matrix-v0.1.md
docs/r5-spec-T13-frontend-effective-permissions-dev-design-20260618.md
docs/r6-spec-C3a-frontend-effective-permissions-impl-20260618.md
docs/superpowers/plans/2026-06-14-p0-access-permission-authz-slice.md
<!-- FILES-END -->

## M040 — 高危授权审批、临时授权、角色装配重算与主管复核链／后台实现／第1段

- 聚合维度：`高危授权审批、临时授权、角色装配重算与主管复核链／后台实现`
- 文件数：11
- 有效内容行数：1375
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/access/TemporaryGrant.java
backend/src/main/java/com/complex/platform/governance/review/ApprovalLevel.java
backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewController.java
backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewRepository.java
backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java
backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewApprovalRequest.java
backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewDecisionRecord.java
backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewEscalationJob.java
backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewEvaluateRequest.java
backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewForwardRequest.java
backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewPolicyRecord.java
<!-- FILES-END -->

## M041 — 高危授权审批、临时授权、角色装配重算与主管复核链／数据库迁移／第1段

- 聚合维度：`高危授权审批、临时授权、角色装配重算与主管复核链／数据库迁移`
- 文件数：16
- 有效内容行数：485
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V004__p0_temporary_grant_group_revoke.sql
backend/src/main/resources/db/migration/V043__supervisor_review_permissions.sql
backend/src/main/resources/db/migration/V050__governance_supervisor_review_persistence.sql
backend/src/main/resources/db/migration/V053__governance_supervisor_review_initiator.sql
backend/src/main/resources/db/migration/V057__governance_supervisor_review_approval_level.sql
backend/src/main/resources/db/migration/V148__supervisor_review_decisions_append_only.sql
backend/src/main/resources/db/migration/V170__access_grant_requests.sql
backend/src/main/resources/db/migration/V187__fix_auditor_audit_log_grants.sql
backend/src/main/resources/db/migration/V273__widen_arap_ids_and_grant_payments.sql
backend/src/main/resources/db/migration/V275__grant_deposit_ledger_update.sql
backend/src/main/resources/db/migration/V298__runtime_grants_cost_expense.sql
backend/src/main/resources/db/migration/V314__contract_scope_grant_and_amendment.sql
backend/src/main/resources/db/migration/V338__grant_channel_fee_and_integration_map_to_runtime.sql
backend/src/main/resources/db/migration/V386__access_grant_authorization_fingerprint.sql
backend/src/main/resources/db/migration/V388__supervisor_review_release_and_approval_classification.sql
backend/src/main/resources/db/migration/V389__supervisor_review_deadline_escalation_forward.sql
<!-- FILES-END -->

## M042 — 高危授权审批、临时授权、角色装配重算与主管复核链／后台验证／第1段

- 聚合维度：`高危授权审批、临时授权、角色装配重算与主管复核链／后台验证`
- 文件数：3
- 有效内容行数：1518
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/governance/review/ApprovalLevelTests.java
backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewControllerTests.java
backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java
<!-- FILES-END -->

## M043 — 高危授权审批、临时授权、角色装配重算与主管复核链／网页与验证／第1段

- 聚合维度：`高危授权审批、临时授权、角色装配重算与主管复核链／网页与验证`
- 文件数：3
- 有效内容行数：660
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/SupervisorReviewPage.tsx
apps/web/src/admin/pages/SupervisorReviewPage.chain.test.tsx
apps/web/src/admin/pages/SupervisorReviewPage.test.tsx
<!-- FILES-END -->

## M044 — 登录会话、生产凭证守卫与破窗紧急恢复（U 盘密钥）／后台实现／第1段

- 聚合维度：`登录会话、生产凭证守卫与破窗紧急恢复（U 盘密钥）／后台实现`
- 文件数：13
- 有效内容行数：2078
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/auth/AuthController.java
backend/src/main/java/com/complex/platform/auth/AuthService.java
backend/src/main/java/com/complex/platform/auth/AuthCookies.java
backend/src/main/java/com/complex/platform/auth/BearerAuthenticationFilter.java
backend/src/main/java/com/complex/platform/auth/CustomerSessionCookieFilter.java
backend/src/main/java/com/complex/platform/auth/CustomerTokenAuthenticationFilter.java
backend/src/main/java/com/complex/platform/auth/DoubleSubmitCsrfFilter.java
backend/src/main/java/com/complex/platform/auth/MasterAuthController.java
backend/src/main/java/com/complex/platform/auth/MasterAuthorityService.java
backend/src/main/java/com/complex/platform/auth/ProductionAuthController.java
backend/src/main/java/com/complex/platform/auth/ProductionAuthService.java
backend/src/main/java/com/complex/platform/auth/ProductionBearerAuthenticationFilter.java
backend/src/main/java/com/complex/platform/auth/SessionIssuer.java
<!-- FILES-END -->

## M045 — 登录会话、生产凭证守卫与破窗紧急恢复（U 盘密钥）／数据库迁移／第1段

- 聚合维度：`登录会话、生产凭证守卫与破窗紧急恢复（U 盘密钥）／数据库迁移`
- 文件数：1
- 有效内容行数：44
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V018__p0_production_auth_provider.sql
<!-- FILES-END -->

## M046 — 登录会话、生产凭证守卫与破窗紧急恢复（U 盘密钥）／后台验证／第1段

- 聚合维度：`登录会话、生产凭证守卫与破窗紧急恢复（U 盘密钥）／后台验证`
- 文件数：8
- 有效内容行数：1525
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/auth/AuthControllerTests.java
backend/src/test/java/com/complex/platform/auth/AuthCookieCsrfTests.java
backend/src/test/java/com/complex/platform/auth/AuthServiceTests.java
backend/src/test/java/com/complex/platform/auth/BearerAuthenticationFilterTests.java
backend/src/test/java/com/complex/platform/auth/CustomerSessionCookieFilterTests.java
backend/src/test/java/com/complex/platform/auth/CustomerTokenAuthenticationFilterTests.java
backend/src/test/java/com/complex/platform/auth/MasterAuthorityServiceTests.java
backend/src/test/java/com/complex/platform/auth/ProductionAuthJdbcIntegrationTests.java
<!-- FILES-END -->

## M047 — 登录会话、生产凭证守卫与破窗紧急恢复（U 盘密钥）／文档与接口定义／第1段

- 聚合维度：`登录会话、生产凭证守卫与破窗紧急恢复（U 盘密钥）／文档与接口定义`
- 文件数：3
- 有效内容行数：255
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/ops/keyvault-usb-runbook.md
docs/superpowers/plans/2026-06-14-p0-auth-token-credential-hardening-slice.md
docs/superpowers/plans/2026-06-14-p0-production-auth-provider-slice.md
<!-- FILES-END -->

## M048 — 登录会话、生产凭证守卫与破窗紧急恢复（U 盘密钥）／运行和发布脚本／第1段

- 聚合维度：`登录会话、生产凭证守卫与破窗紧急恢复（U 盘密钥）／运行和发布脚本`
- 文件数：7
- 有效内容行数：245
- 标记：NORMAL

<!-- FILES-BEGIN -->
ops/keyvault/generate-keystore.sh
ops/keyvault/keys.list
ops/keyvault/lib.sh
ops/keyvault/provision-from-usb.sh
ops/keyvault/selftest.sh
ops/keyvault/sign-challenge.sh
ops/keyvault/verify-provisioned.sh
<!-- FILES-END -->

## M049 — 个人信息保护：PII 加密、敏感查看审批、人脸/生物信息与同意／后台实现／第1段

- 聚合维度：`个人信息保护：PII 加密、敏感查看审批、人脸/生物信息与同意／后台实现`
- 文件数：15
- 有效内容行数：1170
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/amusement/AmusementGroupContactPhonePiiBackfill.java
backend/src/main/java/com/complex/platform/common/PiiTextRedactor.java
backend/src/main/java/com/complex/platform/common/crypto/BlindIndex.java
backend/src/main/java/com/complex/platform/common/crypto/KeyVault.java
backend/src/main/java/com/complex/platform/common/crypto/LocalKeyVault.java
backend/src/main/java/com/complex/platform/common/crypto/MultiPartyContactPhonePiiBackfill.java
backend/src/main/java/com/complex/platform/common/crypto/PiiCipher.java
backend/src/main/java/com/complex/platform/common/crypto/SigningSecretGuard.java
backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyResponseBodyPiiBackfill.java
backend/src/main/java/com/complex/platform/customer/CustomerPhonePiiBackfill.java
backend/src/main/java/com/complex/platform/ticketing/TicketBuyerPhonePiiBackfill.java
backend/src/main/java/com/complex/platform/ticketing/TicketFacePhotoRepository.java
backend/src/main/java/com/complex/platform/ticketing/TicketFacePhotoService.java
backend/src/main/java/com/complex/platform/ticketing/TicketFacePhotoPurgeJob.java
backend/src/main/java/com/complex/platform/ticketing/dto/FacePhotoConsentRequest.java
<!-- FILES-END -->

## M050 — 个人信息保护：PII 加密、敏感查看审批、人脸/生物信息与同意／数据库迁移／第1段

- 聚合维度：`个人信息保护：PII 加密、敏感查看审批、人脸/生物信息与同意／数据库迁移`
- 文件数：16
- 有效内容行数：434
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V188__customer_phone_pii_encryption.sql
backend/src/main/resources/db/migration/V191__customer_id_phone_leak_guard.sql
backend/src/main/resources/db/migration/V229__ticket_face_photo_batch3.sql
backend/src/main/resources/db/migration/V247__ticket_buyer_phone_pii_widen.sql
backend/src/main/resources/db/migration/V248__amusement_contact_phone_pii_widen.sql
backend/src/main/resources/db/migration/V249__pii_phone_envelope_check.sql
backend/src/main/resources/db/migration/V252__customers_phone_pii_check.sql
backend/src/main/resources/db/migration/V339__multi_party_contact_phone_pii.sql
backend/src/main/resources/db/migration/V353__ticket_face_photo_retention_window.sql
backend/src/main/resources/db/migration/V359__ticket_face_photo_order_staging.sql
backend/src/main/resources/db/migration/V368__face_photo_consent_and_special_approval_term_unit.sql
backend/src/main/resources/db/migration/V395__pii_face_photo_consent_required.sql
backend/src/main/resources/db/migration/V396__pii_phone_envelope_check_widen.sql
backend/src/main/resources/db/migration/V397__pii_face_photo_consent_fk.sql
backend/src/main/resources/db/migration/V398__pii_walkin_face_consent.sql
backend/src/main/resources/db/migration/V399__pii_idempotency_response_body_envelope.sql
<!-- FILES-END -->

## M051 — 个人信息保护：PII 加密、敏感查看审批、人脸/生物信息与同意／后台验证／第1段

- 聚合维度：`个人信息保护：PII 加密、敏感查看审批、人脸/生物信息与同意／后台验证`
- 文件数：6
- 有效内容行数：1226
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/common/crypto/MultiPartyContactPhonePiiBackfillTests.java
backend/src/test/java/com/complex/platform/common/crypto/PiiCipherTests.java
backend/src/test/java/com/complex/platform/common/idempotency/IdempotencyJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/ticketing/TicketFacePhotoJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/ticketing/TicketFacePhotoOrderStagingTests.java
backend/src/test/java/com/complex/platform/ticketing/TicketFacePhotoRetentionTests.java
<!-- FILES-END -->

## M052 — 个人信息保护：PII 加密、敏感查看审批、人脸/生物信息与同意／文档与接口定义／第1段

- 聚合维度：`个人信息保护：PII 加密、敏感查看审批、人脸/生物信息与同意／文档与接口定义`
- 文件数：1
- 有效内容行数：343
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/compliance-and-personal-information-protection-requirements-v0.1.md
<!-- FILES-END -->

## M053 — 个人信息保护：PII 加密、敏感查看审批、人脸/生物信息与同意／检查及生成工具／第1段

- 聚合维度：`个人信息保护：PII 加密、敏感查看审批、人脸/生物信息与同意／检查及生成工具`
- 文件数：1
- 有效内容行数：65
- 标记：NORMAL

<!-- FILES-BEGIN -->
tools/verify-pii-encryption.mjs
<!-- FILES-END -->

## M054 — 员工档案、注册审批与工作手机 BYOD／后台实现／第1段

- 聚合维度：`员工档案、注册审批与工作手机 BYOD／后台实现`
- 文件数：17
- 有效内容行数：2917
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java
backend/src/main/java/com/complex/platform/access/staff/StaffController.java
backend/src/main/java/com/complex/platform/access/staff/StaffService.java
backend/src/main/java/com/complex/platform/access/staff/StaffDeviceCredentialRecord.java
backend/src/main/java/com/complex/platform/access/staff/StaffProfileRecord.java
backend/src/main/java/com/complex/platform/access/staff/dto/EmployeeNoticeAcknowledgeRequest.java
backend/src/main/java/com/complex/platform/access/staff/dto/StaffDeviceBindRequest.java
backend/src/main/java/com/complex/platform/access/staff/dto/StaffDeviceIntegrityReportRequest.java
backend/src/main/java/com/complex/platform/access/staff/dto/StaffDeviceRebindDecisionRequest.java
backend/src/main/java/com/complex/platform/access/staff/dto/StaffDeviceRevokeRequest.java
backend/src/main/java/com/complex/platform/access/staff/dto/StaffProfileArchiveRequest.java
backend/src/main/java/com/complex/platform/access/staff/dto/StaffProfileDeleteApprovalRequest.java
backend/src/main/java/com/complex/platform/access/staff/dto/StaffProfileDeleteDecision.java
backend/src/main/java/com/complex/platform/access/staff/dto/StaffProfileDeleteRequest.java
backend/src/main/java/com/complex/platform/access/staff/dto/StaffProfileSelfUpdateRequest.java
backend/src/main/java/com/complex/platform/access/staff/dto/StaffRegistrationRequest.java
backend/src/main/java/com/complex/platform/access/staff/dto/StaffRegistrationReviewRequest.java
<!-- FILES-END -->

## M055 — 员工档案、注册审批与工作手机 BYOD／数据库迁移／第1段

- 聚合维度：`员工档案、注册审批与工作手机 BYOD／数据库迁移`
- 文件数：3
- 有效内容行数：174
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V055__access_staff_levels_and_salary.sql
backend/src/main/resources/db/migration/V056__access_staff_level_permissions.sql
backend/src/main/resources/db/migration/V086__staff_tables.sql
<!-- FILES-END -->

## M056 — 员工档案、注册审批与工作手机 BYOD／后台验证／第1段

- 聚合维度：`员工档案、注册审批与工作手机 BYOD／后台验证`
- 文件数：5
- 有效内容行数：1459
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/access/staff/PayrollSafetyJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/access/staff/StaffAttendanceLocalZoneTests.java
backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/access/staff/StaffProfileArchiveDeleteJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/access/staff/StaffScopeTests.java
<!-- FILES-END -->

## M057 — 员工档案、注册审批与工作手机 BYOD／网页与验证／第1段

- 聚合维度：`员工档案、注册审批与工作手机 BYOD／网页与验证`
- 文件数：2
- 有效内容行数：229
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/StaffProfilesPage.tsx
apps/web/src/admin/pages/StaffProfilesPage.test.tsx
<!-- FILES-END -->

## M058 — 员工档案、注册审批与工作手机 BYOD／文档与接口定义／第1段

- 聚合维度：`员工档案、注册审批与工作手机 BYOD／文档与接口定义`
- 文件数：2
- 有效内容行数：232
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/r5-spec-T1-staff-tables-dev-design-20260618.md
docs/r5-spec-T9-staff-device-android-dev-design-20260618.md
<!-- FILES-END -->

## M059 — 外包/服务商人员与审计人员边界／后台实现／第1段

- 聚合维度：`外包/服务商人员与审计人员边界／后台实现`
- 文件数：11
- 有效内容行数：608
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/partner/ExternalPartnerOrgRecord.java
backend/src/main/java/com/complex/platform/partner/JdbcPartnerRepository.java
backend/src/main/java/com/complex/platform/partner/PartnerController.java
backend/src/main/java/com/complex/platform/partner/PartnerService.java
backend/src/main/java/com/complex/platform/partner/PartnerAccountExpiryJob.java
backend/src/main/java/com/complex/platform/partner/PartnerSubordinateRecord.java
backend/src/main/java/com/complex/platform/partner/PartnerSubordinateCompletionRecord.java
backend/src/main/java/com/complex/platform/partner/PartnerSubordinateCompletionService.java
backend/src/main/java/com/complex/platform/partner/dto/ExternalPartnerOrgRequest.java
backend/src/main/java/com/complex/platform/partner/dto/PartnerOrgUserBindRequest.java
backend/src/main/java/com/complex/platform/partner/dto/PartnerSubordinateRequest.java
<!-- FILES-END -->

## M060 — 外包/服务商人员与审计人员边界／数据库迁移／第1段

- 聚合维度：`外包/服务商人员与审计人员边界／数据库迁移`
- 文件数：4
- 有效内容行数：263
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V178__outsourced_vendor_facility_assignee_permissions.sql
backend/src/main/resources/db/migration/V181__outsourced_adjustments_audit_lock.sql
backend/src/main/resources/db/migration/V183__external_partner_org_self_service.sql
backend/src/main/resources/db/migration/V186__partner_org_management_permissions.sql
<!-- FILES-END -->

## M061 — 外包/服务商人员与审计人员边界／后台验证／第1段

- 聚合维度：`外包/服务商人员与审计人员边界／后台验证`
- 文件数：3
- 有效内容行数：307
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/partner/PartnerJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/partner/PartnerServiceTests.java
backend/src/test/java/com/complex/platform/partner/PartnerSubordinateCompletionServiceTests.java
<!-- FILES-END -->

## M062 — 外包/服务商人员与审计人员边界／网页与验证／第1段

- 聚合维度：`外包/服务商人员与审计人员边界／网页与验证`
- 文件数：3
- 有效内容行数：358
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/VendorSubordinatesPage.tsx
apps/web/src/admin/pages/VendorSubordinatesPage.guidedcreate.test.tsx
apps/web/src/admin/pages/VendorSubordinatesPage.test.tsx
<!-- FILES-END -->

## M063 — 设备台账、领用状态与 MDM 管控／后台实现／第1段

- 聚合维度：`设备台账、领用状态与 MDM 管控／后台实现`
- 文件数：23
- 有效内容行数：2812
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/gate/CachedBodyHttpServletRequest.java
backend/src/main/java/com/complex/platform/gate/DeviceCredentialBodyCachingFilter.java
backend/src/main/java/com/complex/platform/gate/DeviceCredentialInterceptor.java
backend/src/main/java/com/complex/platform/gate/DeviceCredentialVerifier.java
backend/src/main/java/com/complex/platform/gate/GateRequestContext.java
backend/src/main/java/com/complex/platform/gate/NonceStore.java
backend/src/main/java/com/complex/platform/gate/TerminalIdentityHolder.java
backend/src/main/java/com/complex/platform/terminal/DeviceBindingResponse.java
backend/src/main/java/com/complex/platform/terminal/DeviceCredentialRecord.java
backend/src/main/java/com/complex/platform/terminal/DeviceSigningKeyStore.java
backend/src/main/java/com/complex/platform/terminal/JdbcMdmRepository.java
backend/src/main/java/com/complex/platform/terminal/MdmController.java
backend/src/main/java/com/complex/platform/terminal/MdmRepository.java
backend/src/main/java/com/complex/platform/terminal/MdmService.java
backend/src/main/java/com/complex/platform/terminal/MdmAppWhitelistRecord.java
backend/src/main/java/com/complex/platform/terminal/MdmDeviceManagementRecord.java
backend/src/main/java/com/complex/platform/terminal/MdmPolicyViolationRecord.java
backend/src/main/java/com/complex/platform/terminal/MdmSiteWhitelistRecord.java
backend/src/main/java/com/complex/platform/terminal/MdmViolationDetectionResult.java
backend/src/main/java/com/complex/platform/terminal/JdbcTerminalDeviceRepository.java
backend/src/main/java/com/complex/platform/terminal/TerminalDeviceController.java
backend/src/main/java/com/complex/platform/terminal/TerminalDeviceRecord.java
backend/src/main/java/com/complex/platform/terminal/TerminalDeviceRepository.java
<!-- FILES-END -->

## M064 — 设备台账、领用状态与 MDM 管控／后台实现／第2段

- 聚合维度：`设备台账、领用状态与 MDM 管控／后台实现`
- 文件数：13
- 有效内容行数：1249
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/terminal/TerminalDeviceService.java
backend/src/main/java/com/complex/platform/terminal/TerminalDeviceStatusEventRecord.java
backend/src/main/java/com/complex/platform/terminal/dto/DeviceBindRequest.java
backend/src/main/java/com/complex/platform/terminal/dto/DeviceLifecycleRequest.java
backend/src/main/java/com/complex/platform/terminal/dto/DeviceMerchantRequest.java
backend/src/main/java/com/complex/platform/terminal/dto/DeviceReturnRequest.java
backend/src/main/java/com/complex/platform/terminal/dto/DeviceUnbindRequest.java
backend/src/main/java/com/complex/platform/terminal/dto/MdmAppWhitelistRequest.java
backend/src/main/java/com/complex/platform/terminal/dto/MdmDeviceEnrollRequest.java
backend/src/main/java/com/complex/platform/terminal/dto/MdmPolicyViolationRequest.java
backend/src/main/java/com/complex/platform/terminal/dto/MdmSiteWhitelistRequest.java
backend/src/main/java/com/complex/platform/terminal/dto/MdmViolationDetectRequest.java
backend/src/main/java/com/complex/platform/terminal/dto/MdmViolationHandleRequest.java
<!-- FILES-END -->

## M065 — 设备台账、领用状态与 MDM 管控／数据库迁移／第1段

- 聚合维度：`设备台账、领用状态与 MDM 管控／数据库迁移`
- 文件数：3
- 有效内容行数：242
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V011__p0_terminal_device_credentials.sql
backend/src/main/resources/db/migration/V088__mdm_self_managed.sql
backend/src/main/resources/db/migration/V107__device_lifecycle_and_mdm_disposition.sql
<!-- FILES-END -->

## M066 — 设备台账、领用状态与 MDM 管控／后台验证／第1段

- 聚合维度：`设备台账、领用状态与 MDM 管控／后台验证`
- 文件数：7
- 有效内容行数：2033
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/gate/DeviceCredentialEnforcementJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/gate/DeviceCredentialVerifierTests.java
backend/src/test/java/com/complex/platform/gate/NonceAndSigningKeyJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/terminal/MdmJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/terminal/MdmServiceTests.java
backend/src/test/java/com/complex/platform/terminal/TerminalDeviceControllerTests.java
backend/src/test/java/com/complex/platform/terminal/TerminalDeviceJdbcIntegrationTests.java
<!-- FILES-END -->

## M067 — 设备台账、领用状态与 MDM 管控／后台验证／第2段

- 聚合维度：`设备台账、领用状态与 MDM 管控／后台验证`
- 文件数：1
- 有效内容行数：1150
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/terminal/TerminalDeviceServiceTests.java
<!-- FILES-END -->

## M068 — 设备台账、领用状态与 MDM 管控／网页与验证／第1段

- 聚合维度：`设备台账、领用状态与 MDM 管控／网页与验证`
- 文件数：1
- 有效内容行数：257
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/TerminalDevicesPage.tsx
<!-- FILES-END -->

## M069 — 设备台账、领用状态与 MDM 管控／文档与接口定义／第1段

- 聚合维度：`设备台账、领用状态与 MDM 管控／文档与接口定义`
- 文件数：1
- 有效内容行数：77
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/mdm-android-enterprise-implementation-plan-v0.1.md
<!-- FILES-END -->

## M070 — 十一类终端开关、优先级、缺行默认关闭与 canary／后台实现／第1段

- 聚合维度：`十一类终端开关、优先级、缺行默认关闭与 canary／后台实现`
- 文件数：13
- 有效内容行数：1106
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/gate/TerminalMasterSwitchInterceptor.java
backend/src/main/java/com/complex/platform/gate/TerminalSwitchGuard.java
backend/src/main/java/com/complex/platform/terminal/JdbcTerminalCanaryRepository.java
backend/src/main/java/com/complex/platform/terminal/TerminalCanaryController.java
backend/src/main/java/com/complex/platform/terminal/TerminalCanaryRepository.java
backend/src/main/java/com/complex/platform/terminal/TerminalCanaryService.java
backend/src/main/java/com/complex/platform/terminal/TerminalCanaryDecision.java
backend/src/main/java/com/complex/platform/terminal/TerminalCanaryRolloutRecord.java
backend/src/main/java/com/complex/platform/terminal/TerminalCanaryTargetRecord.java
backend/src/main/java/com/complex/platform/terminal/TerminalCapabilitySwitchRecord.java
backend/src/main/java/com/complex/platform/terminal/dto/CanaryRolloutRequest.java
backend/src/main/java/com/complex/platform/terminal/dto/CanaryTargetRequest.java
backend/src/main/java/com/complex/platform/terminal/dto/CapabilitySwitchRequest.java
<!-- FILES-END -->

## M071 — 十一类终端开关、优先级、缺行默认关闭与 canary／数据库迁移／第1段

- 聚合维度：`十一类终端开关、优先级、缺行默认关闭与 canary／数据库迁移`
- 文件数：7
- 有效内容行数：291
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V068__action_gate_switches_admission_policies.sql
backend/src/main/resources/db/migration/V097__camera_scan_and_native_client_switches.sql
backend/src/main/resources/db/migration/V098__device_keystore_and_tablet_switches.sql
backend/src/main/resources/db/migration/V146__face_verification_capability_switch.sql
backend/src/main/resources/db/migration/V197__terminal_master_switches.sql
backend/src/main/resources/db/migration/V198__terminal_web_module_switches.sql
backend/src/main/resources/db/migration/V342__terminal_capability_canary_layer.sql
<!-- FILES-END -->

## M072 — 十一类终端开关、优先级、缺行默认关闭与 canary／后台验证／第1段

- 聚合维度：`十一类终端开关、优先级、缺行默认关闭与 canary／后台验证`
- 文件数：5
- 有效内容行数：799
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/gate/CameraScanAndNativeClientSwitchTests.java
backend/src/test/java/com/complex/platform/gate/TerminalEnabledMasterPrecedenceTests.java
backend/src/test/java/com/complex/platform/gate/TerminalMasterSwitchInterceptorTests.java
backend/src/test/java/com/complex/platform/gate/TerminalSwitchStrictestAndCanaryMatrixTests.java
backend/src/test/java/com/complex/platform/terminal/TerminalCanaryEvaluationTests.java
<!-- FILES-END -->

## M073 — 十一类终端开关、优先级、缺行默认关闭与 canary／网页与验证／第1段

- 聚合维度：`十一类终端开关、优先级、缺行默认关闭与 canary／网页与验证`
- 文件数：3
- 有效内容行数：510
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/TerminalSwitchMatrixPage.tsx
apps/web/src/admin/security/terminalGate.ts
apps/web/src/admin/security/terminalGate.test.ts
<!-- FILES-END -->

## M074 — 十一类终端开关、优先级、缺行默认关闭与 canary／文档与接口定义／第1段

- 聚合维度：`十一类终端开关、优先级、缺行默认关闭与 canary／文档与接口定义`
- 文件数：1
- 有效内容行数：898
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/operation-traceability-and-terminal-switch-prd-v0.1.md
<!-- FILES-END -->

## M075 — 存档、软删除审批、删前占用与集中清理／后台实现／第1段

- 聚合维度：`存档、软删除审批、删前占用与集中清理／后台实现`
- 文件数：9
- 有效内容行数：595
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/ops/DataCleanupController.java
backend/src/main/java/com/complex/platform/ops/DataCleanupService.java
backend/src/main/java/com/complex/platform/ops/JdbcDataCleanupRepository.java
backend/src/main/java/com/complex/platform/ops/DataCleanupApprovalRequest.java
backend/src/main/java/com/complex/platform/ops/DataCleanupJobRecord.java
backend/src/main/java/com/complex/platform/ops/DataCleanupPreviewRequest.java
backend/src/main/java/com/complex/platform/ops/DataCleanupScope.java
backend/src/main/java/com/complex/platform/ops/DataCleanupSubmitRequest.java
backend/src/main/java/com/complex/platform/ops/package-info.java
<!-- FILES-END -->

## M076 — 存档、软删除审批、删前占用与集中清理／数据库迁移／第1段

- 聚合维度：`存档、软删除审批、删前占用与集中清理／数据库迁移`
- 文件数：9
- 有效内容行数：552
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V255__entitlement_template_archive_delete.sql
backend/src/main/resources/db/migration/V259__merchant_archive_delete.sql
backend/src/main/resources/db/migration/V260__ticket_catalog_archive_delete.sql
backend/src/main/resources/db/migration/V262__bar_catalog_archive_delete.sql
backend/src/main/resources/db/migration/V265__data_cleanup_jobs.sql
backend/src/main/resources/db/migration/V266__business_records_archive_delete.sql
backend/src/main/resources/db/migration/V271__archive_reason.sql
backend/src/main/resources/db/migration/V272__service_catalog_soft_delete.sql
backend/src/main/resources/db/migration/V341__amusement_project_entitlement_soft_delete.sql
<!-- FILES-END -->

## M077 — 存档、软删除审批、删前占用与集中清理／后台验证／第1段

- 聚合维度：`存档、软删除审批、删前占用与集中清理／后台验证`
- 文件数：4
- 有效内容行数：483
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/bar/BarReservationArchiveDeleteJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/facility/FacilityEquipmentArchiveDeleteJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/merchantservice/MerchantWorkOrderArchiveDeleteJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/ops/DataCleanupServiceTests.java
<!-- FILES-END -->

## M078 — 存档、软删除审批、删前占用与集中清理／网页与验证／第1段

- 聚合维度：`存档、软删除审批、删前占用与集中清理／网页与验证`
- 文件数：2
- 有效内容行数：455
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/DataCleanupPage.tsx
apps/web/src/admin/pages/DataCleanupPage.test.tsx
<!-- FILES-END -->

## M079 — 存档、软删除审批、删前占用与集中清理／文档与接口定义／第1段

- 聚合维度：`存档、软删除审批、删前占用与集中清理／文档与接口定义`
- 文件数：1
- 有效内容行数：303
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/data-archive-delete-pattern-design-v0.1.md
<!-- FILES-END -->

## M080 — 后台分步引导新建与全链可点击溯源／网页与验证／第1段

- 聚合维度：`后台分步引导新建与全链可点击溯源／网页与验证`
- 文件数：25
- 有效内容行数：2226
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/components/GuidedFlowModal.tsx
apps/web/src/admin/components/GuidedFlowModal.test.tsx
apps/web/src/admin/components/RowDetailDrawer.tsx
apps/web/src/admin/components/TraceLink.tsx
apps/web/src/admin/components/TraceLink.test.tsx
apps/web/src/admin/pages/AccessGovernanceGrantRequest.guidedcreate.test.tsx
apps/web/src/admin/pages/AdmissionAuditsPage.guidedcreate.test.tsx
apps/web/src/admin/pages/AmusementProjects.guidedcreate.test.tsx
apps/web/src/admin/pages/AmusementSafetyPage.guidedcreate.test.tsx
apps/web/src/admin/pages/AmusementSessionsPage.guidedcreate.test.tsx
apps/web/src/admin/pages/BarCommissionsPage.rowdetail.test.tsx
apps/web/src/admin/pages/BarOrdersPage.rowdetail.test.tsx
apps/web/src/admin/pages/BarProductsPage.guidedcreate.test.tsx
apps/web/src/admin/pages/BarProductsPage.rowdetail.test.tsx
apps/web/src/admin/pages/BarTableSessionsPage.guidedcreate.test.tsx
apps/web/src/admin/pages/BarTableSessionsPage.rowdetail.test.tsx
apps/web/src/admin/pages/BusinessAnalyticsPage.guidedcreate.test.tsx
apps/web/src/admin/pages/ComplianceRemindersPage.guidedcreate.test.tsx
apps/web/src/admin/pages/ContractWorkbenchPage.guidedcreate.test.tsx
apps/web/src/admin/pages/DynamicPricingPage.guidedcreate.test.tsx
apps/web/src/admin/pages/EnergyAnalyticsPage.rowdetail.test.tsx
apps/web/src/admin/pages/EnergyWorkbenchPage.guidedcreate.test.tsx
apps/web/src/admin/pages/FacilityWorkbenchPage.guidedcreate.test.tsx
apps/web/src/admin/pages/FacilityWorkbenchPage.rowdetail.test.tsx
apps/web/src/admin/pages/FinanceDividendsPage.guidedcreate.test.tsx
<!-- FILES-END -->

## M081 — 后台分步引导新建与全链可点击溯源／网页与验证／第2段

- 聚合维度：`后台分步引导新建与全链可点击溯源／网页与验证`
- 文件数：25
- 有效内容行数：2260
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/FinanceMerchantExpensePage.guidedcreate.test.tsx
apps/web/src/admin/pages/FinancePropertyFeePage.guidedcreate.test.tsx
apps/web/src/admin/pages/GroupOverviewPage.rowdetail.test.tsx
apps/web/src/admin/pages/HandlerTrace.test.tsx
apps/web/src/admin/pages/IntegrationAdaptersPage.guidedcreate.test.tsx
apps/web/src/admin/pages/IntegrationEventsPage.rowdetail.test.tsx
apps/web/src/admin/pages/InventoryLedgerPage.guidedcreate.test.tsx
apps/web/src/admin/pages/LeasingWorkbenchPage.guidedcreate.test.tsx
apps/web/src/admin/pages/MerchantWorkOrdersPage.guidedcreate.test.tsx
apps/web/src/admin/pages/MerchantZoneFeeRecipientsPage.guidedcreate.test.tsx
apps/web/src/admin/pages/MerchantZoneWorkOrders.guidedcreate.test.tsx
apps/web/src/admin/pages/OperatingParamsPage.guidedcreate.test.tsx
apps/web/src/admin/pages/PayrollTaxBracketsPage.guidedcreate.test.tsx
apps/web/src/admin/pages/ReceivablesAnalyticsPage.rowdetail.test.tsx
apps/web/src/admin/pages/RefundAuthorityPage.guidedcreate.test.tsx
apps/web/src/admin/pages/RiskAlertsPage.guidedcreate.test.tsx
apps/web/src/admin/pages/RowDetailDrawer.test.tsx
apps/web/src/admin/pages/ServiceOrdersPage.guidedcreate.test.tsx
apps/web/src/admin/pages/StaffPayrollLeavePage.guidedcreate.test.tsx
apps/web/src/admin/pages/StaffShiftAttendancePage.guidedcreate.test.tsx
apps/web/src/admin/pages/StoredWinePage.rowdetail.test.tsx
apps/web/src/admin/pages/T3AReadonlyAnalyticsPage.rowdetail.test.tsx
apps/web/src/admin/pages/TicketOrderTrace.test.tsx
apps/web/src/admin/pages/TicketingPoliciesPage.guidedcreate.test.tsx
apps/web/src/admin/pages/TraceLinkDrilldown.test.tsx
<!-- FILES-END -->

## M082 — 后台分步引导新建与全链可点击溯源／网页与验证／第3段

- 聚合维度：`后台分步引导新建与全链可点击溯源／网页与验证`
- 文件数：4
- 有效内容行数：487
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/TraceLinkP2A.test.tsx
apps/web/src/admin/pages/TraceLinkP2c.test.tsx
apps/web/src/admin/pages/VendorSiteEntriesPage.guidedcreate.test.tsx
apps/web/src/admin/pages/VoucherLedgerPage.guidedcreate.test.tsx
<!-- FILES-END -->

## M083 — 后台分步引导新建与全链可点击溯源／文档与接口定义／第1段

- 聚合维度：`后台分步引导新建与全链可点击溯源／文档与接口定义`
- 文件数：1
- 有效内容行数：37
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/admin-page-traceability-clickthrough-audit-v0.1.md
<!-- FILES-END -->

## M084 — 幂等、并发先成功与限流／后台实现／第1段

- 聚合维度：`幂等、并发先成功与限流／后台实现`
- 文件数：9
- 有效内容行数：736
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/common/idempotency/CallerScopeResolver.java
backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyRecord.java
backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyRepository.java
backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyService.java
backend/src/main/java/com/complex/platform/common/idempotency/JdbcIdempotencyRepository.java
backend/src/main/java/com/complex/platform/common/ratelimit/RateLimitInterceptor.java
backend/src/main/java/com/complex/platform/common/ratelimit/RateLimitProperties.java
backend/src/main/java/com/complex/platform/common/ratelimit/RateLimitWebMvcConfig.java
backend/src/main/java/com/complex/platform/common/ratelimit/RateLimiter.java
<!-- FILES-END -->

## M085 — 幂等、并发先成功与限流／后台验证／第1段

- 聚合维度：`幂等、并发先成功与限流／后台验证`
- 文件数：8
- 有效内容行数：1365
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/common/idempotency/IdempotencyServiceTests.java
backend/src/test/java/com/complex/platform/common/ratelimit/RateLimitInterceptorTest.java
backend/src/test/java/com/complex/platform/common/ratelimit/RateLimiterTest.java
backend/src/test/java/com/complex/platform/customer/LoyaltyConfirmConcurrencyIntegrationTests.java
backend/src/test/java/com/complex/platform/inventory/InventoryCostLedgerConcurrencyJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/support/InMemoryIdempotencyRepository.java
backend/src/test/java/com/complex/platform/ticketing/TicketStockConcurrencyJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/ticketing/TicketTransferConcurrencyJdbcIntegrationTests.java
<!-- FILES-END -->

## M086 — 金额精确到分、尾差归属与经营日切/本地时区／后台实现／第1段

- 聚合维度：`金额精确到分、尾差归属与经营日切/本地时区／后台实现`
- 文件数：3
- 有效内容行数：289
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/analytics/BusinessDayCutoffRequest.java
backend/src/main/java/com/complex/platform/common/BusinessDayResolver.java
backend/src/main/java/com/complex/platform/common/money/MoneyRounding.java
<!-- FILES-END -->

## M087 — 金额精确到分、尾差归属与经营日切/本地时区／数据库迁移／第1段

- 聚合维度：`金额精确到分、尾差归属与经营日切/本地时区／数据库迁移`
- 文件数：2
- 有效内容行数：76
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V201__business_day_cutoff_config.sql
backend/src/main/resources/db/migration/V231__order_paid_at.sql
<!-- FILES-END -->

## M088 — 金额精确到分、尾差归属与经营日切/本地时区／后台验证／第1段

- 聚合维度：`金额精确到分、尾差归属与经营日切/本地时区／后台验证`
- 文件数：4
- 有效内容行数：575
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/common/BusinessDayResolverTests.java
backend/src/test/java/com/complex/platform/common/money/MoneyRoundingTests.java
backend/src/test/java/com/complex/platform/settlement/SettlementJobLocalZoneTests.java
backend/src/test/java/com/complex/platform/settlement/SettlementRoundingResidualTests.java
<!-- FILES-END -->

## M089 — 界面全中文与枚举展示词典／数据库迁移／第1段

- 聚合维度：`界面全中文与枚举展示词典／数据库迁移`
- 文件数：3
- 有效内容行数：437
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V251__permission_names_i18n_zh.sql
backend/src/main/resources/db/migration/V254__permission_names_p3_polish.sql
backend/src/main/resources/db/migration/V385__permission_names_chinese.sql
<!-- FILES-END -->

## M090 — 界面全中文与枚举展示词典／网页与验证／第1段

- 聚合维度：`界面全中文与枚举展示词典／网页与验证`
- 文件数：7
- 有效内容行数：812
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/i18n/locationLabels.ts
apps/web/src/admin/i18n/metricLabels.ts
apps/web/src/admin/i18n/statusLabels.ts
apps/web/src/customer/i18n/statusLabels.ts
apps/web/src/customer/i18n/statusLabels.test.ts
apps/web/src/shared/apiError.ts
apps/web/src/shared/apiError.test.ts
<!-- FILES-END -->

## M091 — 界面全中文与枚举展示词典／安卓终端与验证／第1段

- 聚合维度：`界面全中文与枚举展示词典／安卓终端与验证`
- 文件数：1
- 有效内容行数：3
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/android-terminal/android-app/app/src/main/res/values/strings.xml
<!-- FILES-END -->

## M092 — 操作留痕、证据事件、附件双证据与责任链／后台实现／第1段

- 聚合维度：`操作留痕、证据事件、附件双证据与责任链／后台实现`
- 文件数：25
- 有效内容行数：2473
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/evidence/AuditController.java
backend/src/main/java/com/complex/platform/evidence/AuditEvent.java
backend/src/main/java/com/complex/platform/evidence/AuditEventRepository.java
backend/src/main/java/com/complex/platform/evidence/AuditEventService.java
backend/src/main/java/com/complex/platform/evidence/JdbcAuditEventRepository.java
backend/src/main/java/com/complex/platform/evidence/AuditedOperation.java
backend/src/main/java/com/complex/platform/evidence/AuditedOperationExecutor.java
backend/src/main/java/com/complex/platform/evidence/EventBatchResponse.java
backend/src/main/java/com/complex/platform/evidence/EvidenceAttachmentRecord.java
backend/src/main/java/com/complex/platform/evidence/EvidenceAttachmentValidator.java
backend/src/main/java/com/complex/platform/evidence/EvidenceEventController.java
backend/src/main/java/com/complex/platform/evidence/EvidenceEventRecord.java
backend/src/main/java/com/complex/platform/evidence/EvidenceEventRepository.java
backend/src/main/java/com/complex/platform/evidence/EvidenceEventService.java
backend/src/main/java/com/complex/platform/evidence/JdbcEvidenceEventRepository.java
backend/src/main/java/com/complex/platform/evidence/EvidenceFileStorage.java
backend/src/main/java/com/complex/platform/evidence/EvidenceReviewRecord.java
backend/src/main/java/com/complex/platform/evidence/EvidenceTimelineItem.java
backend/src/main/java/com/complex/platform/evidence/JdbcTraceabilityRepository.java
backend/src/main/java/com/complex/platform/evidence/TraceabilityRepository.java
backend/src/main/java/com/complex/platform/evidence/TraceabilityService.java
backend/src/main/java/com/complex/platform/evidence/TraceabilityAutoCaptureRate.java
backend/src/main/java/com/complex/platform/evidence/TraceabilityProcessInstanceRecord.java
backend/src/main/java/com/complex/platform/evidence/TraceabilityResponsibilityRecord.java
backend/src/main/java/com/complex/platform/evidence/TraceabilityStepRecord.java
<!-- FILES-END -->

## M093 — 操作留痕、证据事件、附件双证据与责任链／后台实现／第2段

- 聚合维度：`操作留痕、证据事件、附件双证据与责任链／后台实现`
- 文件数：6
- 有效内容行数：70
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/evidence/dto/EventBatchRequest.java
backend/src/main/java/com/complex/platform/evidence/dto/EvidenceAttachmentBindRequest.java
backend/src/main/java/com/complex/platform/evidence/dto/EvidenceAttachmentCompleteRequest.java
backend/src/main/java/com/complex/platform/evidence/dto/EvidenceAttachmentUploadIntentRequest.java
backend/src/main/java/com/complex/platform/evidence/dto/EvidenceReviewRequest.java
backend/src/main/java/com/complex/platform/evidence/dto/TerminalEventItemRequest.java
<!-- FILES-END -->

## M094 — 操作留痕、证据事件、附件双证据与责任链／数据库迁移／第1段

- 聚合维度：`操作留痕、证据事件、附件双证据与责任链／数据库迁移`
- 文件数：6
- 有效内容行数：402
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V007__p0_terminal_evidence.sql
backend/src/main/resources/db/migration/V010__p0_foundation_residual_platform_terminal_evidence.sql
backend/src/main/resources/db/migration/V059__operation_traceability_three_layer.sql
backend/src/main/resources/db/migration/V101__audit_events_category_subject.sql
backend/src/main/resources/db/migration/V131__access_manual_user_evidence_and_credentials.sql
backend/src/main/resources/db/migration/V185__audit_operation_log_query_permissions.sql
<!-- FILES-END -->

## M095 — 操作留痕、证据事件、附件双证据与责任链／后台验证／第1段

- 聚合维度：`操作留痕、证据事件、附件双证据与责任链／后台验证`
- 文件数：6
- 有效内容行数：1328
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/evidence/AuditControllerTests.java
backend/src/test/java/com/complex/platform/evidence/AuditEventServiceTests.java
backend/src/test/java/com/complex/platform/evidence/EvidenceEventControllerTests.java
backend/src/test/java/com/complex/platform/evidence/EvidenceEventJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/evidence/EvidenceEventServiceTests.java
backend/src/test/java/com/complex/platform/evidence/TraceabilityJdbcIntegrationTests.java
<!-- FILES-END -->

## M096 — 操作留痕、证据事件、附件双证据与责任链／网页与验证／第1段

- 聚合维度：`操作留痕、证据事件、附件双证据与责任链／网页与验证`
- 文件数：2
- 有效内容行数：639
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/AuditEventsPage.tsx
apps/web/src/admin/pages/AuditEventsPage.test.tsx
<!-- FILES-END -->

## M097 — 操作留痕、证据事件、附件双证据与责任链／文档与接口定义／第1段

- 聚合维度：`操作留痕、证据事件、附件双证据与责任链／文档与接口定义`
- 文件数：4
- 有效内容行数：1482
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/evidence-risk-control-product-prd-v0.1.md
docs/evidence-risk-layer-spec-v0.2.md
docs/p0-action-event-evidence-matrix-v0.1.md
docs/superpowers/plans/2026-06-14-p0-terminal-evidence-slice.md
<!-- FILES-END -->

## M098 — 风险分级、风险告警闭环与复核队列／后台实现／第1段

- 聚合维度：`风险分级、风险告警闭环与复核队列／后台实现`
- 文件数：12
- 有效内容行数：959
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/analytics/RiskAlertRequest.java
backend/src/main/java/com/complex/platform/analytics/StatusChangeRequest.java
backend/src/main/java/com/complex/platform/evidence/JdbcRiskAlertRepository.java
backend/src/main/java/com/complex/platform/evidence/RiskAlertController.java
backend/src/main/java/com/complex/platform/evidence/RiskAlertRecord.java
backend/src/main/java/com/complex/platform/evidence/RiskAlertRepository.java
backend/src/main/java/com/complex/platform/evidence/RiskAlertService.java
backend/src/main/java/com/complex/platform/evidence/RiskAlertActionRecord.java
backend/src/main/java/com/complex/platform/evidence/RiskAlertDetail.java
backend/src/main/java/com/complex/platform/evidence/dto/RiskAlertActionRequest.java
backend/src/main/java/com/complex/platform/evidence/dto/RiskAlertReportRequest.java
backend/src/main/java/com/complex/platform/evidence/dto/RiskAlertResolveRequest.java
<!-- FILES-END -->

## M099 — 风险分级、风险告警闭环与复核队列／数据库迁移／第1段

- 聚合维度：`风险分级、风险告警闭环与复核队列／数据库迁移`
- 文件数：1
- 有效内容行数：103
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V103__unified_risk_alerts.sql
<!-- FILES-END -->

## M100 — 风险分级、风险告警闭环与复核队列／后台验证／第1段

- 聚合维度：`风险分级、风险告警闭环与复核队列／后台验证`
- 文件数：1
- 有效内容行数：220
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/evidence/RiskAlertServiceTests.java
<!-- FILES-END -->

## M101 — 风险分级、风险告警闭环与复核队列／网页与验证／第1段

- 聚合维度：`风险分级、风险告警闭环与复核队列／网页与验证`
- 文件数：2
- 有效内容行数：426
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/RiskAlertsPage.tsx
apps/web/src/admin/pages/RiskAlertsPage.test.tsx
<!-- FILES-END -->

## M102 — 票种、通票、权益模板与资格判定／后台实现／第1段

- 聚合维度：`票种、通票、权益模板与资格判定／后台实现`
- 文件数：8
- 有效内容行数：1107
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/ticketing/TicketCatalogService.java
backend/src/main/java/com/complex/platform/ticketing/dto/EntitlementTemplateArchiveRequest.java
backend/src/main/java/com/complex/platform/ticketing/dto/EntitlementTemplateDeleteApprovalRequest.java
backend/src/main/java/com/complex/platform/ticketing/dto/EntitlementTemplateDeleteRequest.java
backend/src/main/java/com/complex/platform/ticketing/dto/EntitlementTemplateRequest.java
backend/src/main/java/com/complex/platform/ticketing/dto/TicketTypeEntitlementRequest.java
backend/src/main/java/com/complex/platform/ticketing/dto/TicketTypePricingRequest.java
backend/src/main/java/com/complex/platform/ticketing/dto/TicketTypeRequest.java
<!-- FILES-END -->

## M103 — 票种、通票、权益模板与资格判定／网页与验证／第1段

- 聚合维度：`票种、通票、权益模板与资格判定／网页与验证`
- 文件数：4
- 有效内容行数：1778
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/TicketingPoliciesPage.tsx
apps/web/src/admin/pages/TicketingPoliciesPage.combo.test.tsx
apps/web/src/admin/pages/TicketingPoliciesPage.kpi.test.tsx
apps/web/src/admin/pages/TicketingPoliciesPage.pricing.test.tsx
<!-- FILES-END -->

## M104 — 票种、通票、权益模板与资格判定／文档与接口定义／第1段

- 聚合维度：`票种、通票、权益模板与资格判定／文档与接口定义`
- 文件数：4
- 有效内容行数：2809
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/ticketing-database-implementation-notes.md
docs/ticketing-entitlement-checkin-product-prd-v0.1.md
docs/ticketing-entitlement-settlement-breakdown.md
docs/ticketing-pass-entitlement-development-design.md
<!-- FILES-END -->

## M105 — 票价规则、动态定价、会员价与套票／后台实现／第1段

- 聚合维度：`票价规则、动态定价、会员价与套票／后台实现`
- 文件数：2
- 有效内容行数：326
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/ticketing/TicketPricingService.java
backend/src/main/java/com/complex/platform/ticketing/dto/TicketPriceRuleRequest.java
<!-- FILES-END -->

## M106 — 票价规则、动态定价、会员价与套票／数据库迁移／第1段

- 聚合维度：`票价规则、动态定价、会员价与套票／数据库迁移`
- 文件数：3
- 有效内容行数：113
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V036__p2_ticket_dynamic_pricing_rules.sql
backend/src/main/resources/db/migration/V041__ticket_order_dynamic_pricing_trace.sql
backend/src/main/resources/db/migration/V369__combo_pass_price_autocalc_rounding.sql
<!-- FILES-END -->

## M107 — 票价规则、动态定价、会员价与套票／网页与验证／第1段

- 聚合维度：`票价规则、动态定价、会员价与套票／网页与验证`
- 文件数：2
- 有效内容行数：457
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/DynamicPricingPage.tsx
apps/web/src/admin/pages/DynamicPricingPage.kpi.test.tsx
<!-- FILES-END -->

## M108 — 票价规则、动态定价、会员价与套票／文档与接口定义／第1段

- 聚合维度：`票价规则、动态定价、会员价与套票／文档与接口定义`
- 文件数：4
- 有效内容行数：332
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/p1-ticket-member-price-variant-api-contract-v0.1.md
docs/p2-ticket-dynamic-pricing-api-contract-v0.1.md
docs/p2-ticket-dynamic-pricing-completion-audit-v0.1.md
docs/superpowers/plans/2026-06-15-p2-ticket-dynamic-pricing.md
<!-- FILES-END -->

## M109 — 购票订单、支付意向、出票与订单过期／后台实现／第1段

- 聚合维度：`购票订单、支付意向、出票与订单过期／后台实现`
- 文件数：7
- 有效内容行数：1275
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/finance/PaymentChannelPolicy.java
backend/src/main/java/com/complex/platform/finance/PaymentIntentRecord.java
backend/src/main/java/com/complex/platform/finance/dto/PaymentIntentRequest.java
backend/src/main/java/com/complex/platform/ticketing/TicketOrderService.java
backend/src/main/java/com/complex/platform/ticketing/TicketOrderExpiryJob.java
backend/src/main/java/com/complex/platform/ticketing/dto/ConfirmTicketOrderPaymentRequest.java
backend/src/main/java/com/complex/platform/ticketing/dto/TicketOrderRequest.java
<!-- FILES-END -->

## M110 — 购票订单、支付意向、出票与订单过期／数据库迁移／第1段

- 聚合维度：`购票订单、支付意向、出票与订单过期／数据库迁移`
- 文件数：3
- 有效内容行数：34
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V130__ticket_order_read_permission.sql
backend/src/main/resources/db/migration/V135__ticket_orders_merchant_id.sql
backend/src/main/resources/db/migration/V283__drop_ticket_order_items.sql
<!-- FILES-END -->

## M111 — 购票订单、支付意向、出票与订单过期／后台验证／第1段

- 聚合维度：`购票订单、支付意向、出票与订单过期／后台验证`
- 文件数：1
- 有效内容行数：35
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/finance/PaymentChannelPolicyTests.java
<!-- FILES-END -->

## M112 — 购票订单、支付意向、出票与订单过期／网页与验证／第1段

- 聚合维度：`购票订单、支付意向、出票与订单过期／网页与验证`
- 文件数：8
- 有效内容行数：1586
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/TicketOrdersPage.tsx
apps/web/src/admin/pages/TicketOrdersPage.degraded.test.tsx
apps/web/src/admin/pages/TicketOrdersPage.facephoto.test.tsx
apps/web/src/admin/pages/TicketOrdersPage.kpi.test.tsx
apps/web/src/admin/pages/TicketOrdersPage.metricdetail.test.tsx
apps/web/src/customer/pages/BuyTicketPage.tsx
apps/web/src/customer/pages/BuyTicketPage.facephoto.test.tsx
apps/web/src/customer/pages/MyOrdersPage.tsx
<!-- FILES-END -->

## M113 — 购票订单、支付意向、出票与订单过期／文档与接口定义／第1段

- 聚合维度：`购票订单、支付意向、出票与订单过期／文档与接口定义`
- 文件数：1
- 有效内容行数：201
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/superpowers/plans/2026-06-14-p0-ticketing-customer-order-credential-slice.md
<!-- FILES-END -->

## M114 — 动态二维码凭证、在线核销、多人同场次与冲正／后台实现／第1段

- 聚合维度：`动态二维码凭证、在线核销、多人同场次与冲正／后台实现`
- 文件数：10
- 有效内容行数：1579
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/ticketing/TicketCredentialRecord.java
backend/src/main/java/com/complex/platform/ticketing/TicketCredentialService.java
backend/src/main/java/com/complex/platform/ticketing/TicketCredentialResponse.java
backend/src/main/java/com/complex/platform/ticketing/TicketCredentialSigner.java
backend/src/main/java/com/complex/platform/ticketing/TicketOfflinePackageService.java
backend/src/main/java/com/complex/platform/ticketing/TicketVerificationService.java
backend/src/main/java/com/complex/platform/ticketing/dto/QrRefreshConfigRequest.java
backend/src/main/java/com/complex/platform/ticketing/dto/VerificationManualOverrideRequest.java
backend/src/main/java/com/complex/platform/ticketing/dto/VerificationRequest.java
backend/src/main/java/com/complex/platform/ticketing/dto/VerificationReverseRequest.java
<!-- FILES-END -->

## M115 — 动态二维码凭证、在线核销、多人同场次与冲正／数据库迁移／第1段

- 聚合维度：`动态二维码凭证、在线核销、多人同场次与冲正／数据库迁移`
- 文件数：5
- 有效内容行数：219
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V006__p0_ticketing_verification.sql
backend/src/main/resources/db/migration/V064__ticket_credential_refresh_interval.sql
backend/src/main/resources/db/migration/V233__ticket_verification_amusement_ref.sql
backend/src/main/resources/db/migration/V300__qr_refresh_interval_options.sql
backend/src/main/resources/db/migration/V320__qr_refresh_interval_5s_unified.sql
<!-- FILES-END -->

## M116 — 动态二维码凭证、在线核销、多人同场次与冲正／后台验证／第1段

- 聚合维度：`动态二维码凭证、在线核销、多人同场次与冲正／后台验证`
- 文件数：4
- 有效内容行数：515
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/ticketing/TicketCredentialSignatureTests.java
backend/src/test/java/com/complex/platform/ticketing/TicketOfflinePackageServiceTests.java
backend/src/test/java/com/complex/platform/ticketing/TicketVerificationMdmGateTests.java
backend/src/test/java/com/complex/platform/ticketing/TicketVerificationTransferGuardTests.java
<!-- FILES-END -->

## M117 — 动态二维码凭证、在线核销、多人同场次与冲正／网页与验证／第1段

- 聚合维度：`动态二维码凭证、在线核销、多人同场次与冲正／网页与验证`
- 文件数：2
- 有效内容行数：163
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/customer/components/TicketQrView.tsx
apps/web/src/customer/components/TicketQrView.test.tsx
<!-- FILES-END -->

## M118 — 动态二维码凭证、在线核销、多人同场次与冲正／文档与接口定义／第1段

- 聚合维度：`动态二维码凭证、在线核销、多人同场次与冲正／文档与接口定义`
- 文件数：2
- 有效内容行数：220
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/r5-spec-T4-ticket-anticounterfeit-dev-design-20260618.md
docs/superpowers/plans/2026-06-14-p0-ticketing-verification-slice.md
<!-- FILES-END -->

## M119 — 退款规则、有效期到期与退款分级授权／后台实现／第1段

- 聚合维度：`退款规则、有效期到期与退款分级授权／后台实现`
- 文件数：17
- 有效内容行数：1289
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/finance/FinanceRefundRecord.java
backend/src/main/java/com/complex/platform/finance/RefundAuthorityLimitRecord.java
backend/src/main/java/com/complex/platform/finance/JdbcRefundAuthorizationRepository.java
backend/src/main/java/com/complex/platform/finance/RefundAuthorizationController.java
backend/src/main/java/com/complex/platform/finance/RefundAuthorizationRepository.java
backend/src/main/java/com/complex/platform/finance/RefundAuthorizationService.java
backend/src/main/java/com/complex/platform/finance/RefundAuthorizationConfigService.java
backend/src/main/java/com/complex/platform/finance/RefundAutoRuleRecord.java
backend/src/main/java/com/complex/platform/finance/TicketRefundPayoutConsumer.java
backend/src/main/java/com/complex/platform/finance/dto/RefundAuthorityLimitRequest.java
backend/src/main/java/com/complex/platform/finance/dto/RefundAutoRuleRequest.java
backend/src/main/java/com/complex/platform/finance/dto/RefundRequest.java
backend/src/main/java/com/complex/platform/ticketing/TicketRefundRecord.java
backend/src/main/java/com/complex/platform/ticketing/TicketRefundService.java
backend/src/main/java/com/complex/platform/ticketing/TicketRefundApprovedEvent.java
backend/src/main/java/com/complex/platform/ticketing/TicketRefundLineRecord.java
backend/src/main/java/com/complex/platform/ticketing/dto/TicketRefundRequest.java
<!-- FILES-END -->

## M120 — 退款规则、有效期到期与退款分级授权／后台验证／第1段

- 聚合维度：`退款规则、有效期到期与退款分级授权／后台验证`
- 文件数：4
- 有效内容行数：621
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/finance/RefundAuthorizationControllerTests.java
backend/src/test/java/com/complex/platform/finance/RefundAuthorizationJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/finance/RefundAuthorizationServiceTests.java
backend/src/test/java/com/complex/platform/ticketing/TicketExpiryForfeitJdbcIntegrationTests.java
<!-- FILES-END -->

## M121 — 退款规则、有效期到期与退款分级授权／网页与验证／第1段

- 聚合维度：`退款规则、有效期到期与退款分级授权／网页与验证`
- 文件数：2
- 有效内容行数：411
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/RefundAuthorityPage.tsx
apps/web/src/admin/pages/RefundAuthorityPage.test.tsx
<!-- FILES-END -->

## M122 — 退款规则、有效期到期与退款分级授权／文档与接口定义／第1段

- 聚合维度：`退款规则、有效期到期与退款分级授权／文档与接口定义`
- 文件数：1
- 有效内容行数：57
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/p1-ticket-refund-line-ledger-api-contract-v0.1.md
<!-- FILES-END -->

## M123 — 门票转赠、多人票/一人多次票与反向流程／后台实现／第1段

- 聚合维度：`门票转赠、多人票/一人多次票与反向流程／后台实现`
- 文件数：4
- 有效内容行数：433
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/ticketing/TicketTransferRecord.java
backend/src/main/java/com/complex/platform/ticketing/TicketTransferService.java
backend/src/main/java/com/complex/platform/ticketing/TicketTransferPreviewResponse.java
backend/src/main/java/com/complex/platform/ticketing/TicketTransferResponse.java
<!-- FILES-END -->

## M124 — 门票转赠、多人票/一人多次票与反向流程／数据库迁移／第1段

- 聚合维度：`门票转赠、多人票/一人多次票与反向流程／数据库迁移`
- 文件数：1
- 有效内容行数：56
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V287__ticket_transfer.sql
<!-- FILES-END -->

## M125 — 门票转赠、多人票/一人多次票与反向流程／后台验证／第1段

- 聚合维度：`门票转赠、多人票/一人多次票与反向流程／后台验证`
- 文件数：1
- 有效内容行数：486
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/ticketing/TicketTransferJdbcIntegrationTests.java
<!-- FILES-END -->

## M126 — 门票转赠、多人票/一人多次票与反向流程／网页与验证／第1段

- 聚合维度：`门票转赠、多人票/一人多次票与反向流程／网页与验证`
- 文件数：2
- 有效内容行数：325
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/customer/pages/TicketTransferPage.tsx
apps/web/src/customer/pages/TicketTransferPage.degraded.test.tsx
<!-- FILES-END -->

## M127 — 门票转赠、多人票/一人多次票与反向流程／文档与接口定义／第1段

- 聚合维度：`门票转赠、多人票/一人多次票与反向流程／文档与接口定义`
- 文件数：1
- 有效内容行数：54
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/r5-spec-T5-multiperson-reverse-dev-design-20260618.md
<!-- FILES-END -->

## M128 — 入场三道环节：安检、人脸验龄与检票／后台实现／第1段

- 聚合维度：`入场三道环节：安检、人脸验龄与检票／后台实现`
- 文件数：6
- 有效内容行数：213
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/entry/DisabledFaceVerificationGate.java
backend/src/main/java/com/complex/platform/entry/FaceVerificationPort.java
backend/src/main/java/com/complex/platform/entry/FaceVerificationRequest.java
backend/src/main/java/com/complex/platform/entry/FaceVerificationResult.java
backend/src/main/java/com/complex/platform/entry/FaceVerificationRouter.java
backend/src/main/java/com/complex/platform/entry/ReservedFaceVerificationGate.java
<!-- FILES-END -->

## M129 — 入场三道环节：安检、人脸验龄与检票／后台验证／第1段

- 聚合维度：`入场三道环节：安检、人脸验龄与检票／后台验证`
- 文件数：1
- 有效内容行数：68
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/entry/FaceVerificationGateTests.java
<!-- FILES-END -->

## M130 — 票务核心服务、控制器与其余／后台实现／第1段

- 聚合维度：`票务核心服务、控制器与其余／后台实现`
- 文件数：25
- 有效内容行数：807
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/ticketing/ArchivableRecord.java
backend/src/main/java/com/complex/platform/ticketing/BarMerchantDirectory.java
backend/src/main/java/com/complex/platform/ticketing/BarProductRefRecord.java
backend/src/main/java/com/complex/platform/ticketing/ComboPriceToleranceConfigPort.java
backend/src/main/java/com/complex/platform/ticketing/CustomerTicketResponse.java
backend/src/main/java/com/complex/platform/ticketing/DynamicPriceResult.java
backend/src/main/java/com/complex/platform/ticketing/EntitlementTemplateRecord.java
backend/src/main/java/com/complex/platform/ticketing/EntitlementTemplateDeleteDecision.java
backend/src/main/java/com/complex/platform/ticketing/GiftReversedEvent.java
backend/src/main/java/com/complex/platform/ticketing/GiftVerifiedEvent.java
backend/src/main/java/com/complex/platform/ticketing/InternalSettlementPriceResolver.java
backend/src/main/java/com/complex/platform/ticketing/IssuedEntitlementRecord.java
backend/src/main/java/com/complex/platform/ticketing/IssuedEntitlementResponse.java
backend/src/main/java/com/complex/platform/ticketing/IssuedTicketResponse.java
backend/src/main/java/com/complex/platform/ticketing/OfflineEntitlementResponse.java
backend/src/main/java/com/complex/platform/ticketing/OfflinePackageResponse.java
backend/src/main/java/com/complex/platform/ticketing/PartnerVoucherConfigRecord.java
backend/src/main/java/com/complex/platform/ticketing/PartnerVoucherRedemptionRecord.java
backend/src/main/java/com/complex/platform/ticketing/PlatformBarMerchantDirectory.java
backend/src/main/java/com/complex/platform/ticketing/PointsAccrualRateConfigPort.java
backend/src/main/java/com/complex/platform/ticketing/PointsConfirmRequestedEvent.java
backend/src/main/java/com/complex/platform/ticketing/PointsDeductRequestedEvent.java
backend/src/main/java/com/complex/platform/ticketing/PointsEarnRequestedEvent.java
backend/src/main/java/com/complex/platform/ticketing/QrRefreshConfigResponse.java
backend/src/main/java/com/complex/platform/ticketing/SettlementSettledEvent.java
<!-- FILES-END -->

## M131 — 票务核心服务、控制器与其余／后台实现／第2段

- 聚合维度：`票务核心服务、控制器与其余／后台实现`
- 文件数：25
- 有效内容行数：636
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/ticketing/SyncTaskResult.java
backend/src/main/java/com/complex/platform/ticketing/SyncTasksResponse.java
backend/src/main/java/com/complex/platform/ticketing/TerminalSyncPayloadSanitizer.java
backend/src/main/java/com/complex/platform/ticketing/TerminalSyncTaskRecord.java
backend/src/main/java/com/complex/platform/ticketing/TicketCatalogArchiveRequest.java
backend/src/main/java/com/complex/platform/ticketing/TicketCatalogDeleteApprovalRequest.java
backend/src/main/java/com/complex/platform/ticketing/TicketCatalogDeleteRequest.java
backend/src/main/java/com/complex/platform/ticketing/TicketDistributionMerchantRateRecord.java
backend/src/main/java/com/complex/platform/ticketing/TicketDynamicPricingRuleRecord.java
backend/src/main/java/com/complex/platform/ticketing/TicketDynamicPricingRuleRequest.java
backend/src/main/java/com/complex/platform/ticketing/TicketDynamicPricingSimulationRequest.java
backend/src/main/java/com/complex/platform/ticketing/TicketDynamicPricingSimulationResponse.java
backend/src/main/java/com/complex/platform/ticketing/TicketEntitlementMapper.java
backend/src/main/java/com/complex/platform/ticketing/TicketInstanceRecord.java
backend/src/main/java/com/complex/platform/ticketing/TicketOrderRecord.java
backend/src/main/java/com/complex/platform/ticketing/TicketPaymentIntentRecord.java
backend/src/main/java/com/complex/platform/ticketing/TicketPriceRuleRecord.java
backend/src/main/java/com/complex/platform/ticketing/TicketPriceRuleDeleteDecision.java
backend/src/main/java/com/complex/platform/ticketing/TicketSlotHolderRecord.java
backend/src/main/java/com/complex/platform/ticketing/TicketTypeRecord.java
backend/src/main/java/com/complex/platform/ticketing/TicketTypeDeleteDecision.java
backend/src/main/java/com/complex/platform/ticketing/TicketTypeEntitlementRecord.java
backend/src/main/java/com/complex/platform/ticketing/TicketTypeGiftCoverageRow.java
backend/src/main/java/com/complex/platform/ticketing/TicketTypePriceCheck.java
backend/src/main/java/com/complex/platform/ticketing/TicketTypeProviderEntitlementRow.java
<!-- FILES-END -->

## M132 — 票务核心服务、控制器与其余／后台实现／第3段

- 聚合维度：`票务核心服务、控制器与其余／后台实现`
- 文件数：3
- 有效内容行数：2723
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/ticketing/JdbcTicketingRepository.java
backend/src/main/java/com/complex/platform/ticketing/TicketingController.java
backend/src/main/java/com/complex/platform/ticketing/TicketingRepository.java
<!-- FILES-END -->

## M133 — 票务核心服务、控制器与其余／后台实现／第4段

- 聚合维度：`票务核心服务、控制器与其余／后台实现`
- 文件数：17
- 有效内容行数：2255
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/ticketing/TicketingService.java
backend/src/main/java/com/complex/platform/ticketing/TicketingAmusementEntitlementGateway.java
backend/src/main/java/com/complex/platform/ticketing/TicketingOperationExecutor.java
backend/src/main/java/com/complex/platform/ticketing/JdbcTicketingParameterSource.java
backend/src/main/java/com/complex/platform/ticketing/TicketingReadOperation.java
backend/src/main/java/com/complex/platform/ticketing/JdbcTicketingRowMapper.java
backend/src/main/java/com/complex/platform/ticketing/JdbcTicketingSql.java
backend/src/main/java/com/complex/platform/ticketing/TicketingWriteOperation.java
backend/src/main/java/com/complex/platform/ticketing/VerificationRecord.java
backend/src/main/java/com/complex/platform/ticketing/VerificationPreviewResponse.java
backend/src/main/java/com/complex/platform/ticketing/VerificationResponse.java
backend/src/main/java/com/complex/platform/ticketing/VerificationReversedEvent.java
backend/src/main/java/com/complex/platform/ticketing/dto/StatusChangeRequest.java
backend/src/main/java/com/complex/platform/ticketing/dto/TerminalSyncTaskItemRequest.java
backend/src/main/java/com/complex/platform/ticketing/dto/TerminalSyncTasksRequest.java
backend/src/main/java/com/complex/platform/ticketing/dto/TicketDistributionMerchantRateRequest.java
backend/src/main/java/com/complex/platform/ticketing/dto/TicketIssueRequest.java
<!-- FILES-END -->

## M134 — 票务核心服务、控制器与其余／后台验证／第1段

- 聚合维度：`票务核心服务、控制器与其余／后台验证`
- 文件数：5
- 有效内容行数：2180
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/ticketing/SessionEntryPassJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/ticketing/SettlementDoubleWriteEndToEndTests.java
backend/src/test/java/com/complex/platform/ticketing/TerminalSyncPayloadSanitizerTests.java
backend/src/test/java/com/complex/platform/ticketing/TicketingControllerTests.java
backend/src/test/java/com/complex/platform/ticketing/TicketingJdbcIntegrationTests.java
<!-- FILES-END -->

## M135 — 票务核心服务、控制器与其余／后台验证／第2段

- 聚合维度：`票务核心服务、控制器与其余／后台验证`
- 文件数：1
- 有效内容行数：4842
- 标记：OVERSIZE-SINGLE-FILE

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/ticketing/TicketingServiceTests.java
<!-- FILES-END -->

## M136 — 票务核心服务、控制器与其余／后台验证／第3段

- 聚合维度：`票务核心服务、控制器与其余／后台验证`
- 文件数：1
- 有效内容行数：283
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/ticketing/UnlimitedInDayJdbcIntegrationTests.java
<!-- FILES-END -->

## M137 — 票务核心服务、控制器与其余／文档与接口定义／第1段

- 聚合维度：`票务核心服务、控制器与其余／文档与接口定义`
- 文件数：2
- 有效内容行数：1019
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/business-module-mature-system-gap-analysis.md
docs/p0-end-to-end-acceptance-and-edge-cases-v0.1.md
<!-- FILES-END -->

## M138 — 会员身份、注册认证、成长值等级、签到生日与账号状态／后台实现／第1段

- 聚合维度：`会员身份、注册认证、成长值等级、签到生日与账号状态／后台实现`
- 文件数：20
- 有效内容行数：1596
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/customer/CustomerAccountAdminController.java
backend/src/main/java/com/complex/platform/customer/CustomerAccountLookupRequest.java
backend/src/main/java/com/complex/platform/customer/CustomerAccountReasonRequest.java
backend/src/main/java/com/complex/platform/customer/CustomerAccountView.java
backend/src/main/java/com/complex/platform/customer/CustomerBirthdayRuleRecord.java
backend/src/main/java/com/complex/platform/customer/CustomerCheckInRecord.java
backend/src/main/java/com/complex/platform/customer/CustomerCheckInTierRecord.java
backend/src/main/java/com/complex/platform/customer/CustomerMemberLevelRecord.java
backend/src/main/java/com/complex/platform/customer/CustomerSuspendRequest.java
backend/src/main/java/com/complex/platform/customer/LoyaltyController.java
backend/src/main/java/com/complex/platform/customer/LoyaltyService.java
backend/src/main/java/com/complex/platform/customer/LoyaltyConfigAdminController.java
backend/src/main/java/com/complex/platform/customer/LoyaltyConfigAdminService.java
backend/src/main/java/com/complex/platform/customer/LoyaltyPointsConsumer.java
backend/src/main/java/com/complex/platform/customer/LoyaltyScheduledJob.java
backend/src/main/java/com/complex/platform/customer/dto/BirthdayRuleConfigRequest.java
backend/src/main/java/com/complex/platform/customer/dto/CheckInTierConfigRequest.java
backend/src/main/java/com/complex/platform/customer/dto/CustomerCheckInRequest.java
backend/src/main/java/com/complex/platform/customer/dto/LoyaltyFreezeRequest.java
backend/src/main/java/com/complex/platform/customer/dto/MemberLevelConfigRequest.java
<!-- FILES-END -->

## M139 — 会员身份、注册认证、成长值等级、签到生日与账号状态／数据库迁移／第1段

- 聚合维度：`会员身份、注册认证、成长值等级、签到生日与账号状态／数据库迁移`
- 文件数：5
- 有效内容行数：189
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V089__member_loyalty_level_points.sql
backend/src/main/resources/db/migration/V192__customers_status_check.sql
backend/src/main/resources/db/migration/V193__customers_status_allow_frozen_closed.sql
backend/src/main/resources/db/migration/V194__customers_status_lifecycle_states.sql
backend/src/main/resources/db/migration/V195__customer_account_lifecycle.sql
<!-- FILES-END -->

## M140 — 会员身份、注册认证、成长值等级、签到生日与账号状态／后台验证／第1段

- 聚合维度：`会员身份、注册认证、成长值等级、签到生日与账号状态／后台验证`
- 文件数：3
- 有效内容行数：1020
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/customer/LoyaltyConfigAdminServiceTests.java
backend/src/test/java/com/complex/platform/customer/LoyaltyConfigConflictAndReversibleTests.java
backend/src/test/java/com/complex/platform/customer/LoyaltyServiceTests.java
<!-- FILES-END -->

## M141 — 会员身份、注册认证、成长值等级、签到生日与账号状态／网页与验证／第1段

- 聚合维度：`会员身份、注册认证、成长值等级、签到生日与账号状态／网页与验证`
- 文件数：4
- 有效内容行数：623
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/CustomerAccountsPage.tsx
apps/web/src/admin/pages/CustomerAccountsPage.test.tsx
apps/web/src/admin/pages/MemberLoyaltyConfigPage.tsx
apps/web/src/admin/pages/MemberLoyaltyConfigPage.test.tsx
<!-- FILES-END -->

## M142 — 会员身份、注册认证、成长值等级、签到生日与账号状态／文档与接口定义／第1段

- 聚合维度：`会员身份、注册认证、成长值等级、签到生日与账号状态／文档与接口定义`
- 文件数：6
- 有效内容行数：1553
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/member-and-tier-product-prd-v1.0.md
docs/member-loyalty-and-stored-value-development-design-v0.1.md
docs/member-points-tier-and-clearing-prd-v1.0.md
docs/superpowers/plans/2026-07-28-member-points-settlement-redesign.md
docs/superpowers/plans/2026-07-28-member-points-tier-clearing-implementation-slices.md
docs/superpowers/specs/2026-07-28-member-points-settlement-redesign-design.md
<!-- FILES-END -->

## M143 — 积分账户、待追回积分、积分商城与虚拟券／后台实现／第1段

- 聚合维度：`积分账户、待追回积分、积分商城与虚拟券／后台实现`
- 文件数：25
- 有效内容行数：2311
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/customer/CustomerPointsAccountRecord.java
backend/src/main/java/com/complex/platform/customer/CustomerPointsLedgerRecord.java
backend/src/main/java/com/complex/platform/customer/CustomerPointsRedemptionRecord.java
backend/src/main/java/com/complex/platform/customer/CustomerPointsResponse.java
backend/src/main/java/com/complex/platform/customer/CustomerRedemptionCatalogItemRecord.java
backend/src/main/java/com/complex/platform/customer/dto/CustomerPointsRedemptionRequest.java
backend/src/main/java/com/complex/platform/voucher/JdbcVoucherRepository.java
backend/src/main/java/com/complex/platform/voucher/VoucherController.java
backend/src/main/java/com/complex/platform/voucher/VoucherRepository.java
backend/src/main/java/com/complex/platform/voucher/VoucherService.java
backend/src/main/java/com/complex/platform/voucher/VoucherDefinitionRecord.java
backend/src/main/java/com/complex/platform/voucher/VoucherInstanceRecord.java
backend/src/main/java/com/complex/platform/voucher/VoucherIssuePort.java
backend/src/main/java/com/complex/platform/voucher/VoucherIssueResultRecord.java
backend/src/main/java/com/complex/platform/voucher/VoucherOverviewRecord.java
backend/src/main/java/com/complex/platform/voucher/VoucherRedemptionEntryRecord.java
backend/src/main/java/com/complex/platform/voucher/VoucherReservationExpiryJob.java
backend/src/main/java/com/complex/platform/voucher/VoucherStateMachine.java
backend/src/main/java/com/complex/platform/voucher/JdbcVoucherTransferRepository.java
backend/src/main/java/com/complex/platform/voucher/VoucherTransferRecord.java
backend/src/main/java/com/complex/platform/voucher/VoucherTransferService.java
backend/src/main/java/com/complex/platform/voucher/VoucherTransferResponse.java
backend/src/main/java/com/complex/platform/voucher/dto/CustomerVoucherTransferClaimRequest.java
backend/src/main/java/com/complex/platform/voucher/dto/CustomerVoucherTransferRequest.java
backend/src/main/java/com/complex/platform/voucher/dto/VoucherDefinitionRequest.java
<!-- FILES-END -->

## M144 — 积分账户、待追回积分、积分商城与虚拟券／后台实现／第2段

- 聚合维度：`积分账户、待追回积分、积分商城与虚拟券／后台实现`
- 文件数：3
- 有效内容行数：28
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/voucher/dto/VoucherIssueRequest.java
backend/src/main/java/com/complex/platform/voucher/dto/VoucherRedeemRequest.java
backend/src/main/java/com/complex/platform/voucher/dto/VoucherRevokeRequest.java
<!-- FILES-END -->

## M145 — 积分账户、待追回积分、积分商城与虚拟券／数据库迁移／第1段

- 聚合维度：`积分账户、待追回积分、积分商城与虚拟券／数据库迁移`
- 文件数：13
- 有效内容行数：576
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V025__p1_customer_points_redemption.sql
backend/src/main/resources/db/migration/V054__customer_points_balance_non_negative.sql
backend/src/main/resources/db/migration/V060__customer_points_rules_and_gift_convergence.sql
backend/src/main/resources/db/migration/V100__points_ledger_earn_engine.sql
backend/src/main/resources/db/migration/V151__points_ledger_confirmed_amount.sql
backend/src/main/resources/db/migration/V172__points_transfer_offline_cleanup.sql
backend/src/main/resources/db/migration/V269__customer_points_rule_admin_permissions.sql
backend/src/main/resources/db/migration/V276__points_redemption_add_refunded_status.sql
backend/src/main/resources/db/migration/V301__unified_voucher_ledger.sql
backend/src/main/resources/db/migration/V312__voucher_stock_reservation.sql
backend/src/main/resources/db/migration/V313__voucher_transfer.sql
backend/src/main/resources/db/migration/V340__commission_and_points_rate_configs.sql
backend/src/main/resources/db/migration/V352__customer_points_rules_retire.sql
<!-- FILES-END -->

## M146 — 积分账户、待追回积分、积分商城与虚拟券／后台验证／第1段

- 聚合维度：`积分账户、待追回积分、积分商城与虚拟券／后台验证`
- 文件数：7
- 有效内容行数：1791
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/inventory/VoucherReservationJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/settlement/CommissionPointsConfigJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/voucher/VoucherJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/voucher/VoucherServiceTests.java
backend/src/test/java/com/complex/platform/voucher/VoucherStateMachineTests.java
backend/src/test/java/com/complex/platform/voucher/VoucherTransferJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/voucher/VoucherTransferServiceTests.java
<!-- FILES-END -->

## M147 — 积分账户、待追回积分、积分商城与虚拟券／网页与验证／第1段

- 聚合维度：`积分账户、待追回积分、积分商城与虚拟券／网页与验证`
- 文件数：6
- 有效内容行数：1552
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/VoucherLedgerPage.tsx
apps/web/src/customer/pages/PointsPage.tsx
apps/web/src/customer/pages/PointsPage.degraded.test.tsx
apps/web/src/customer/pages/PointsPage.test.tsx
apps/web/src/customer/pages/VoucherWalletPage.tsx
apps/web/src/customer/pages/VoucherWalletPage.test.tsx
<!-- FILES-END -->

## M148 — 积分账户、待追回积分、积分商城与虚拟券／文档与接口定义／第1段

- 聚合维度：`积分账户、待追回积分、积分商城与虚拟券／文档与接口定义`
- 文件数：5
- 有效内容行数：820
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/member-points-p1-ops-parameter-tables-v0.1.md
docs/p1-customer-points-redemption-api-contract-v0.1.md
docs/points-and-voucher-product-prd-v1.0.md
docs/proposals/积分虚拟券-券核销领取与结算-设计草案.md
docs/superpowers/plans/2026-06-14-p1-customer-points-redemption.md
<!-- FILES-END -->

## M149 — 商户积分结算、内部结算价目录与零售参考价／数据库迁移／第1段

- 聚合维度：`商户积分结算、内部结算价目录与零售参考价／数据库迁移`
- 文件数：3
- 有效内容行数：164
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V279__bar_internal_price_view_permission.sql
backend/src/main/resources/db/migration/V379__internal_price_catalog.sql
backend/src/main/resources/db/migration/V380__internal_price_catalog_backfill.sql
<!-- FILES-END -->

## M150 — 商户积分结算、内部结算价目录与零售参考价／后台验证／第1段

- 聚合维度：`商户积分结算、内部结算价目录与零售参考价／后台验证`
- 文件数：2
- 有效内容行数：527
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/settlement/InternalPriceCatalogJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/ticketing/SettlementPriceResolutionJdbcIntegrationTests.java
<!-- FILES-END -->

## M151 — 商户积分结算、内部结算价目录与零售参考价／网页与验证／第1段

- 聚合维度：`商户积分结算、内部结算价目录与零售参考价／网页与验证`
- 文件数：2
- 有效内容行数：361
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/InternalPriceCatalogPage.tsx
apps/web/src/admin/pages/InternalPriceCatalogPage.test.tsx
<!-- FILES-END -->

## M152 — 商户积分结算、内部结算价目录与零售参考价／文档与接口定义／第1段

- 聚合维度：`商户积分结算、内部结算价目录与零售参考价／文档与接口定义`
- 文件数：1
- 有效内容行数：474
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/merchant-points-settlement-rules-v1.0.md
<!-- FILES-END -->

## M153 — 储值卡与余额（含商户卡资金归属）／后台实现／第1段

- 聚合维度：`储值卡与余额（含商户卡资金归属）／后台实现`
- 文件数：14
- 有效内容行数：1359
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/customer/CustomerStoredValueAccountRecord.java
backend/src/main/java/com/complex/platform/customer/CustomerStoredValueTransactionRecord.java
backend/src/main/java/com/complex/platform/customer/MemberWalletController.java
backend/src/main/java/com/complex/platform/customer/StoredValueService.java
backend/src/main/java/com/complex/platform/customer/StoredValueMerchantRefundRuleRecord.java
backend/src/main/java/com/complex/platform/customer/StoredValueTopupIntentScope.java
backend/src/main/java/com/complex/platform/customer/dto/StoredValueConsumeRequest.java
backend/src/main/java/com/complex/platform/customer/dto/StoredValueCounterRefundRequest.java
backend/src/main/java/com/complex/platform/customer/dto/StoredValueFreezeRequest.java
backend/src/main/java/com/complex/platform/customer/dto/StoredValueMerchantCardConfigRequest.java
backend/src/main/java/com/complex/platform/customer/dto/StoredValueMerchantRefundRuleRequest.java
backend/src/main/java/com/complex/platform/customer/dto/StoredValueRefundRequest.java
backend/src/main/java/com/complex/platform/customer/dto/StoredValueSettleRequest.java
backend/src/main/java/com/complex/platform/customer/dto/StoredValueTopupRequest.java
<!-- FILES-END -->

## M154 — 储值卡与余额（含商户卡资金归属）／数据库迁移／第1段

- 聚合维度：`储值卡与余额（含商户卡资金归属）／数据库迁移`
- 文件数：6
- 有效内容行数：246
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V090__member_stored_value_paid_membership_transfer.sql
backend/src/main/resources/db/migration/V175__stored_value_merchant_scoped_cards.sql
backend/src/main/resources/db/migration/V176__stored_value_merchant_refund_rules.sql
backend/src/main/resources/db/migration/V284__stored_value_topup_intents.sql
backend/src/main/resources/db/migration/V289__stored_value_merchant_card_fund_mode.sql
backend/src/main/resources/db/migration/V292__bar_bill_settlement_stored_value_channel.sql
<!-- FILES-END -->

## M155 — 储值卡与余额（含商户卡资金归属）／后台验证／第1段

- 聚合维度：`储值卡与余额（含商户卡资金归属）／后台验证`
- 文件数：5
- 有效内容行数：1579
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/customer/M006StoredValueJdbcTests.java
backend/src/test/java/com/complex/platform/customer/M006StoredValuePaymentTests.java
backend/src/test/java/com/complex/platform/customer/MemberWalletServiceTests.java
backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java
<!-- FILES-END -->

## M156 — 储值卡与余额（含商户卡资金归属）／网页与验证／第1段

- 聚合维度：`储值卡与余额（含商户卡资金归属）／网页与验证`
- 文件数：4
- 有效内容行数：552
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/StoredValueRulesPage.tsx
apps/web/src/admin/pages/StoredValueRulesPage.test.tsx
apps/web/src/customer/pages/WalletPage.tsx
apps/web/src/customer/pages/WalletPage.test.tsx
<!-- FILES-END -->

## M157 — 储值卡与余额（含商户卡资金归属）／文档与接口定义／第1段

- 聚合维度：`储值卡与余额（含商户卡资金归属）／文档与接口定义`
- 文件数：2
- 有效内容行数：298
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/member-loyalty-and-stored-value-product-prd-v0.1.md
docs/stored-value-membership-checkin-birthday-prd-v0.1.md
<!-- FILES-END -->

## M158 — 客人端核心与其余／后台实现／第1段

- 聚合维度：`客人端核心与其余／后台实现`
- 文件数：4
- 有效内容行数：2004
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/customer/CustomerController.java
backend/src/main/java/com/complex/platform/customer/CustomerRecord.java
backend/src/main/java/com/complex/platform/customer/CustomerRepository.java
backend/src/main/java/com/complex/platform/customer/CustomerService.java
<!-- FILES-END -->

## M159 — 客人端核心与其余／后台实现／第2段

- 聚合维度：`客人端核心与其余／后台实现`
- 文件数：19
- 有效内容行数：1650
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/customer/JdbcCustomerRepository.java
backend/src/main/java/com/complex/platform/customer/CustomerBarReservationOptionResponse.java
backend/src/main/java/com/complex/platform/customer/CustomerConsentRecord.java
backend/src/main/java/com/complex/platform/customer/CustomerLoginResponse.java
backend/src/main/java/com/complex/platform/customer/CustomerOtpVerifier.java
backend/src/main/java/com/complex/platform/customer/CustomerSessionRecord.java
backend/src/main/java/com/complex/platform/customer/CustomerSessionInfoResponse.java
backend/src/main/java/com/complex/platform/customer/CustomerTicketProductResponse.java
backend/src/main/java/com/complex/platform/customer/FailClosedCustomerOtpVerifier.java
backend/src/main/java/com/complex/platform/customer/FixedCustomerOtpVerifier.java
backend/src/main/java/com/complex/platform/customer/dto/CustomerBarInvitationConfirmRequest.java
backend/src/main/java/com/complex/platform/customer/dto/CustomerBarMenuItemResponse.java
backend/src/main/java/com/complex/platform/customer/dto/CustomerBarReservationRequest.java
backend/src/main/java/com/complex/platform/customer/dto/CustomerBarScanOrderRequest.java
backend/src/main/java/com/complex/platform/customer/dto/CustomerConsentRequest.java
backend/src/main/java/com/complex/platform/customer/dto/CustomerPhoneLoginRequest.java
backend/src/main/java/com/complex/platform/customer/dto/CustomerTicketOrderRequest.java
backend/src/main/java/com/complex/platform/customer/dto/CustomerTicketTransferClaimRequest.java
backend/src/main/java/com/complex/platform/customer/dto/CustomerTicketTransferRequest.java
<!-- FILES-END -->

## M160 — 客人端核心与其余／数据库迁移／第1段

- 聚合维度：`客人端核心与其余／数据库迁移`
- 文件数：1
- 有效内容行数：202
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V009__p0_ticketing_customer_orders_credentials.sql
<!-- FILES-END -->

## M161 — 客人端核心与其余／后台验证／第1段

- 聚合维度：`客人端核心与其余／后台验证`
- 文件数：3
- 有效内容行数：2176
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/customer/CustomerControllerTests.java
backend/src/test/java/com/complex/platform/customer/CustomerJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/customer/CustomerServiceTests.java
<!-- FILES-END -->

## M162 — 客人端核心与其余／网页与验证／第1段

- 聚合维度：`客人端核心与其余／网页与验证`
- 文件数：12
- 有效内容行数：1771
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/customer/CustomerApp.tsx
apps/web/src/customer/types.ts
apps/web/src/customer/client/customerService.ts
apps/web/src/customer/client/customerClient.ts
apps/web/src/customer/client/customerClient.test.ts
apps/web/src/customer/components/CustomerAuthShell.tsx
apps/web/src/customer/pages/BarInvitationPage.tsx
apps/web/src/customer/pages/BarReservationPage.tsx
apps/web/src/customer/pages/BarScanOrderPage.tsx
apps/web/src/customer/pages/HomePage.tsx
apps/web/src/customer/pages/HomePage.test.tsx
apps/web/src/customer/pages/M006OrderStoredValue.test.tsx
<!-- FILES-END -->

## M163 — 客人端核心与其余／端到端验证／第1段

- 聚合维度：`客人端核心与其余／端到端验证`
- 文件数：4
- 有效内容行数：559
- 标记：NORMAL

<!-- FILES-BEGIN -->
tests/e2e/customer-account-lifecycle-live-evidence.spec.ts
tests/e2e/customer-journey.spec.ts
tests/e2e/order-tickets-live-evidence.spec.ts
tests/e2e/support/customer-mock-api.ts
<!-- FILES-END -->

## M164 — 客人端核心与其余／文档与接口定义／第1段

- 聚合维度：`客人端核心与其余／文档与接口定义`
- 文件数：2
- 有效内容行数：450
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/customer-app-miniapp-development-design-v0.1.md
docs/customer-app-miniapp-product-prd-v0.1.md
<!-- FILES-END -->

## M165 — 台位状态、订台定金、邀请函与转台/并台／后台实现／第1段

- 聚合维度：`台位状态、订台定金、邀请函与转台/并台／后台实现`
- 文件数：9
- 有效内容行数：1299
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/bar/BarEntryService.java
backend/src/main/java/com/complex/platform/bar/BarInvitationActionRequest.java
backend/src/main/java/com/complex/platform/bar/BarInvitationTokenSigner.java
backend/src/main/java/com/complex/platform/bar/BarReservationInvitationRecord.java
backend/src/main/java/com/complex/platform/bar/BarReservationInvitationMemberRecord.java
backend/src/main/java/com/complex/platform/bar/BarTableSessionRecord.java
backend/src/main/java/com/complex/platform/bar/BarTableSessionService.java
backend/src/main/java/com/complex/platform/bar/BarTableSessionAdjustmentRecord.java
backend/src/main/java/com/complex/platform/bar/BarTableSessionRequest.java
<!-- FILES-END -->

## M166 — 台位状态、订台定金、邀请函与转台/并台／数据库迁移／第1段

- 聚合维度：`台位状态、订台定金、邀请函与转台/并台／数据库迁移`
- 文件数：5
- 有效内容行数：487
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V013__p0_bar_reservation_order_billing.sql
backend/src/main/resources/db/migration/V019__p1_bar_table_session_adjustments.sql
backend/src/main/resources/db/migration/V063__bar_reservation_deposit_rule.sql
backend/src/main/resources/db/migration/V073__bar_reservation_deposit.sql
backend/src/main/resources/db/migration/V155__bar_card_seat_invitation.sql
<!-- FILES-END -->

## M167 — 台位状态、订台定金、邀请函与转台/并台／后台验证／第1段

- 聚合维度：`台位状态、订台定金、邀请函与转台/并台／后台验证`
- 文件数：1
- 有效内容行数：28
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/bar/BarInvitationTokenSignerTests.java
<!-- FILES-END -->

## M168 — 台位状态、订台定金、邀请函与转台/并台／网页与验证／第1段

- 聚合维度：`台位状态、订台定金、邀请函与转台/并台／网页与验证`
- 文件数：2
- 有效内容行数：601
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/BarTableSessionsPage.tsx
apps/web/src/admin/pages/BarTableSessionsPage.robustness.test.tsx
<!-- FILES-END -->

## M169 — 台位状态、订台定金、邀请函与转台/并台／文档与接口定义／第1段

- 聚合维度：`台位状态、订台定金、邀请函与转台/并台／文档与接口定义`
- 文件数：4
- 有效内容行数：626
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/p1-bar-table-session-api-contract-v0.1.md
docs/r5-spec-T16-reservation-deposit-dev-design-20260618.md
docs/superpowers/plans/2026-06-14-p0-bar-reservation-order-billing-slice.md
docs/superpowers/plans/2026-06-14-p1-bar-table-session-adjustments.md
<!-- FILES-END -->

## M170 — 平板图形点单 POS、商品目录、套餐与优惠券／后台实现／第1段

- 聚合维度：`平板图形点单 POS、商品目录、套餐与优惠券／后台实现`
- 文件数：7
- 有效内容行数：708
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/bar/BarCatalogArchiveRequest.java
backend/src/main/java/com/complex/platform/bar/BarCatalogDeleteApprovalRequest.java
backend/src/main/java/com/complex/platform/bar/BarCatalogDeleteDecision.java
backend/src/main/java/com/complex/platform/bar/BarCatalogDeleteRequest.java
backend/src/main/java/com/complex/platform/bar/BarCatalogLifecycle.java
backend/src/main/java/com/complex/platform/bar/BarPosCatalogController.java
backend/src/main/java/com/complex/platform/bar/BarPosCatalogService.java
<!-- FILES-END -->

## M171 — 平板图形点单 POS、商品目录、套餐与优惠券／数据库迁移／第1段

- 聚合维度：`平板图形点单 POS、商品目录、套餐与优惠券／数据库迁移`
- 文件数：3
- 有效内容行数：84
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V152__bar_product_internal_settlement_price.sql
backend/src/main/resources/db/migration/V154__bar_product_management_permissions.sql
backend/src/main/resources/db/migration/V257__bar_combo.sql
<!-- FILES-END -->

## M172 — 平板图形点单 POS、商品目录、套餐与优惠券／后台验证／第1段

- 聚合维度：`平板图形点单 POS、商品目录、套餐与优惠券／后台验证`
- 文件数：1
- 有效内容行数：588
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/bar/BarPosCatalogJdbcIntegrationTests.java
<!-- FILES-END -->

## M173 — 平板图形点单 POS、商品目录、套餐与优惠券／网页与验证／第1段

- 聚合维度：`平板图形点单 POS、商品目录、套餐与优惠券／网页与验证`
- 文件数：3
- 有效内容行数：948
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/BarCombosPage.tsx
apps/web/src/admin/pages/BarCombosPage.test.tsx
apps/web/src/admin/pages/BarProductsPage.tsx
<!-- FILES-END -->

## M174 — 点酒订单、出品票据、沽清、退酒与按杯出酒损耗／后台实现／第1段

- 聚合维度：`点酒订单、出品票据、沽清、退酒与按杯出酒损耗／后台实现`
- 文件数：9
- 有效内容行数：1838
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/bar/BarOrderService.java
backend/src/main/java/com/complex/platform/bar/BarOrderItemVoidRecord.java
backend/src/main/java/com/complex/platform/bar/BarOrderItemVoidDecisionRequest.java
backend/src/main/java/com/complex/platform/bar/BarOrderItemVoidRequest.java
backend/src/main/java/com/complex/platform/bar/BarPourLoss.java
backend/src/main/java/com/complex/platform/bar/BarVoidReasonCodeRecord.java
backend/src/main/java/com/complex/platform/bar/JdbcBarVoidReasonCodeRepository.java
backend/src/main/java/com/complex/platform/bar/BarVoidReasonCodeDirectory.java
backend/src/main/java/com/complex/platform/bar/BarVoidReasonCodeRequest.java
<!-- FILES-END -->

## M175 — 点酒订单、出品票据、沽清、退酒与按杯出酒损耗／数据库迁移／第1段

- 聚合维度：`点酒订单、出品票据、沽清、退酒与按杯出酒损耗／数据库迁移`
- 文件数：11
- 有效内容行数：653
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V029__p1_bar_production_ticket_workflow.sql
backend/src/main/resources/db/migration/V058__bar_pour_volume_loss.sql
backend/src/main/resources/db/migration/V091__bar_pos_depth.sql
backend/src/main/resources/db/migration/V117__bar_void_approval_writeback.sql
backend/src/main/resources/db/migration/V124__bar_void_reject_reversal_adjustment.sql
backend/src/main/resources/db/migration/V125__bar_void_decision_permission.sql
backend/src/main/resources/db/migration/V153__bar_order_item_gift_flag.sql
backend/src/main/resources/db/migration/V256__bar_pos_catalog_category_image.sql
backend/src/main/resources/db/migration/V258__bar_pos_catalog_permissions.sql
backend/src/main/resources/db/migration/V345__bar_void_approval_first_high_value.sql
backend/src/main/resources/db/migration/V351__bar_void_reason_code_dictionary.sql
<!-- FILES-END -->

## M176 — 点酒订单、出品票据、沽清、退酒与按杯出酒损耗／后台验证／第1段

- 聚合维度：`点酒订单、出品票据、沽清、退酒与按杯出酒损耗／后台验证`
- 文件数：1
- 有效内容行数：27
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/bar/BarPourLossTests.java
<!-- FILES-END -->

## M177 — 点酒订单、出品票据、沽清、退酒与按杯出酒损耗／网页与验证／第1段

- 聚合维度：`点酒订单、出品票据、沽清、退酒与按杯出酒损耗／网页与验证`
- 文件数：3
- 有效内容行数：840
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/BarOrdersPage.tsx
apps/web/src/admin/pages/BarOrdersPage.kpi.test.tsx
apps/web/src/admin/pages/BarOrdersPage.metricdetail.test.tsx
<!-- FILES-END -->

## M178 — 点酒订单、出品票据、沽清、退酒与按杯出酒损耗／文档与接口定义／第1段

- 聚合维度：`点酒订单、出品票据、沽清、退酒与按杯出酒损耗／文档与接口定义`
- 文件数：2
- 有效内容行数：177
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/p1-bar-production-ticket-workflow-api-contract-v0.1.md
docs/superpowers/plans/2026-06-14-p1-bar-production-ticket-workflow.md
<!-- FILES-END -->

## M179 — 账单结算、拆合单、小费、免单/调账/反结审批／后台实现／第1段

- 聚合维度：`账单结算、拆合单、小费、免单/调账/反结审批／后台实现`
- 文件数：1
- 有效内容行数：767
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/bar/BarBillingService.java
<!-- FILES-END -->

## M180 — 账单结算、拆合单、小费、免单/调账/反结审批／数据库迁移／第1段

- 聚合维度：`账单结算、拆合单、小费、免单/调账/反结审批／数据库迁移`
- 文件数：5
- 有效内容行数：182
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V020__p1_bar_bill_adjustments.sql
backend/src/main/resources/db/migration/V033__p1_bar_bill_split_merge.sql
backend/src/main/resources/db/migration/V062__bar_bill_settlement_channel.sql
backend/src/main/resources/db/migration/V079__bar_bill_waiver_refund.sql
backend/src/main/resources/db/migration/V136__bar_bill_settlement_reversed.sql
<!-- FILES-END -->

## M181 — 账单结算、拆合单、小费、免单/调账/反结审批／后台验证／第1段

- 聚合维度：`账单结算、拆合单、小费、免单/调账/反结审批／后台验证`
- 文件数：1
- 有效内容行数：122
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/bar/BarBillContinuationJdbcIntegrationTests.java
<!-- FILES-END -->

## M182 — 账单结算、拆合单、小费、免单/调账/反结审批／文档与接口定义／第1段

- 聚合维度：`账单结算、拆合单、小费、免单/调账/反结审批／文档与接口定义`
- 文件数：3
- 有效内容行数：487
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/p1-bar-bill-adjustment-api-contract-v0.1.md
docs/p1-bar-bill-split-merge-api-contract-v0.1.md
docs/superpowers/plans/2026-06-14-p1-bar-bill-adjustments.md
<!-- FILES-END -->

## M183 — 正式未付款订单挂起、应收挂账与审计结案／后台实现／第1段

- 聚合维度：`正式未付款订单挂起、应收挂账与审计结案／后台实现`
- 文件数：2
- 有效内容行数：244
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/bar/BarReceivableLedgerService.java
backend/src/main/java/com/complex/platform/bar/FinanceBarReceivableLedgerService.java
<!-- FILES-END -->

## M184 — 正式未付款订单挂起、应收挂账与审计结案／数据库迁移／第1段

- 聚合维度：`正式未付款订单挂起、应收挂账与审计结案／数据库迁移`
- 文件数：1
- 有效内容行数：6
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V021__p1_bar_on_account_receivable_link.sql
<!-- FILES-END -->

## M185 — 正式未付款订单挂起、应收挂账与审计结案／后台验证／第1段

- 聚合维度：`正式未付款订单挂起、应收挂账与审计结案／后台验证`
- 文件数：1
- 有效内容行数：381
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/bar/FinanceBarReceivableLedgerServiceTests.java
<!-- FILES-END -->

## M186 — 正式未付款订单挂起、应收挂账与审计结案／文档与接口定义／第1段

- 聚合维度：`正式未付款订单挂起、应收挂账与审计结案／文档与接口定义`
- 文件数：3
- 有效内容行数：358
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/superpowers/plans/2026-06-14-p1-bar-on-account-ar-ledger-link.md
docs/superpowers/reviews/2026-06-14-p1-bar-bill-adjustments-review.md
docs/superpowers/reviews/2026-06-14-p1-bar-on-account-ar-ledger-link-review.md
<!-- FILES-END -->

## M187 — 权益兑付、赠送成本与礼品核销／后台实现／第1段

- 聚合维度：`权益兑付、赠送成本与礼品核销／后台实现`
- 文件数：2
- 有效内容行数：380
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/bar/BarEntitlementRedemptionService.java
backend/src/main/java/com/complex/platform/bar/BarGiftConsumer.java
<!-- FILES-END -->

## M188 — 权益兑付、赠送成本与礼品核销／数据库迁移／第1段

- 聚合维度：`权益兑付、赠送成本与礼品核销／数据库迁移`
- 文件数：2
- 有效内容行数：64
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V072__bar_gift_redemption.sql
backend/src/main/resources/db/migration/V076__entitlement_num_people_and_gift_item.sql
<!-- FILES-END -->

## M189 — 权益兑付、赠送成本与礼品核销／后台验证／第1段

- 聚合维度：`权益兑付、赠送成本与礼品核销／后台验证`
- 文件数：3
- 有效内容行数：1101
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/bar/BarEntitlementRedemptionServiceTests.java
backend/src/test/java/com/complex/platform/bar/BarGiftConsumerTests.java
backend/src/test/java/com/complex/platform/ticketing/GiftRedemptionEndToEndTests.java
<!-- FILES-END -->

## M190 — 权益兑付、赠送成本与礼品核销／文档与接口定义／第1段

- 聚合维度：`权益兑付、赠送成本与礼品核销／文档与接口定义`
- 文件数：1
- 有效内容行数：65
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/r5-spec-T15-gift-redeem-stock-dev-design-20260618.md
<!-- FILES-END -->

## M191 — 班次交班与收银对账／后台实现／第1段

- 聚合维度：`班次交班与收银对账／后台实现`
- 文件数：5
- 有效内容行数：256
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/bar/BarShiftRecord.java
backend/src/main/java/com/complex/platform/bar/BarShiftService.java
backend/src/main/java/com/complex/platform/bar/BarShiftCloseRequest.java
backend/src/main/java/com/complex/platform/bar/BarShiftOpenRequest.java
backend/src/main/java/com/complex/platform/bar/BarShiftReconciliationSnapshot.java
<!-- FILES-END -->

## M192 — 班次交班与收银对账／数据库迁移／第1段

- 聚合维度：`班次交班与收银对账／数据库迁移`
- 文件数：1
- 有效内容行数：82
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V095__bar_shift_handover.sql
<!-- FILES-END -->

## M193 — 酒吧核心门面、控制器与其余／后台实现／第1段

- 聚合维度：`酒吧核心门面、控制器与其余／后台实现`
- 文件数：3
- 有效内容行数：1685
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/bar/BarController.java
backend/src/main/java/com/complex/platform/bar/BarRepository.java
backend/src/main/java/com/complex/platform/bar/BarService.java
<!-- FILES-END -->

## M194 — 酒吧核心门面、控制器与其余／后台实现／第2段

- 聚合维度：`酒吧核心门面、控制器与其余／后台实现`
- 文件数：25
- 有效内容行数：2426
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/bar/JdbcBarRepository.java
backend/src/main/java/com/complex/platform/bar/BarBillRecord.java
backend/src/main/java/com/complex/platform/bar/BarBillAdjustmentRecord.java
backend/src/main/java/com/complex/platform/bar/BarBillAdjustmentRequest.java
backend/src/main/java/com/complex/platform/bar/BarBillMergeRequest.java
backend/src/main/java/com/complex/platform/bar/BarBillOperationRecord.java
backend/src/main/java/com/complex/platform/bar/BarBillRecords.java
backend/src/main/java/com/complex/platform/bar/BarBillReopenRequest.java
backend/src/main/java/com/complex/platform/bar/BarBillSettleRequest.java
backend/src/main/java/com/complex/platform/bar/BarBillSettlementRecord.java
backend/src/main/java/com/complex/platform/bar/BarBillSplitRequest.java
backend/src/main/java/com/complex/platform/bar/BarBillSummary.java
backend/src/main/java/com/complex/platform/bar/BarBillTipRequest.java
backend/src/main/java/com/complex/platform/bar/BarComboRecord.java
backend/src/main/java/com/complex/platform/bar/BarComboItemRecord.java
backend/src/main/java/com/complex/platform/bar/BarComboRequest.java
backend/src/main/java/com/complex/platform/bar/BarDepositRuleRecord.java
backend/src/main/java/com/complex/platform/bar/BarDepositRuleRequest.java
backend/src/main/java/com/complex/platform/bar/BarEntitlementDrinkRedemptionRequest.java
backend/src/main/java/com/complex/platform/bar/BarGiftRedemptionRecord.java
backend/src/main/java/com/complex/platform/bar/BarOperationExecutor.java
backend/src/main/java/com/complex/platform/bar/BarOrderRecord.java
backend/src/main/java/com/complex/platform/bar/BarOrderBillCreateRequest.java
backend/src/main/java/com/complex/platform/bar/BarOrderDeliveryRequest.java
backend/src/main/java/com/complex/platform/bar/BarOrderItemRecord.java
<!-- FILES-END -->

## M195 — 酒吧核心门面、控制器与其余／后台实现／第3段

- 聚合维度：`酒吧核心门面、控制器与其余／后台实现`
- 文件数：25
- 有效内容行数：2148
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/bar/BarOrderRequest.java
backend/src/main/java/com/complex/platform/bar/BarOrderView.java
backend/src/main/java/com/complex/platform/bar/JdbcBarParameterSource.java
backend/src/main/java/com/complex/platform/bar/JdbcBarPosCatalogRepository.java
backend/src/main/java/com/complex/platform/bar/BarPosImageStorage.java
backend/src/main/java/com/complex/platform/bar/BarProductRecord.java
backend/src/main/java/com/complex/platform/bar/BarProductCategoryRecord.java
backend/src/main/java/com/complex/platform/bar/BarProductCategoryAssignRequest.java
backend/src/main/java/com/complex/platform/bar/BarProductCategoryRequest.java
backend/src/main/java/com/complex/platform/bar/BarProductRequest.java
backend/src/main/java/com/complex/platform/bar/BarProductSoldOutRequest.java
backend/src/main/java/com/complex/platform/bar/BarProductionTicketRecord.java
backend/src/main/java/com/complex/platform/bar/BarProductionTicketEventRecord.java
backend/src/main/java/com/complex/platform/bar/BarProductionTicketTransitionRequest.java
backend/src/main/java/com/complex/platform/bar/BarReadOperation.java
backend/src/main/java/com/complex/platform/bar/BarReservationRecord.java
backend/src/main/java/com/complex/platform/bar/BarReservationCancelRequest.java
backend/src/main/java/com/complex/platform/bar/BarReservationDepositActionRequest.java
backend/src/main/java/com/complex/platform/bar/BarReservationEntryAllowanceVerificationRecord.java
backend/src/main/java/com/complex/platform/bar/BarReservationRequest.java
backend/src/main/java/com/complex/platform/bar/JdbcBarRowMapper.java
backend/src/main/java/com/complex/platform/bar/BarTableRecord.java
backend/src/main/java/com/complex/platform/bar/BarTableAvailabilityRecord.java
backend/src/main/java/com/complex/platform/bar/BarTableGuestCheckinRecord.java
backend/src/main/java/com/complex/platform/bar/BarTableMergeRequest.java
<!-- FILES-END -->

## M196 — 酒吧核心门面、控制器与其余／后台实现／第4段

- 聚合维度：`酒吧核心门面、控制器与其余／后台实现`
- 文件数：11
- 有效内容行数：385
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/bar/BarTableRenewRequest.java
backend/src/main/java/com/complex/platform/bar/BarTableRequest.java
backend/src/main/java/com/complex/platform/bar/BarTableSpaceAssignmentRecord.java
backend/src/main/java/com/complex/platform/bar/BarTableSpaceAssignmentRequest.java
backend/src/main/java/com/complex/platform/bar/BarTableSpaceBacklogRecord.java
backend/src/main/java/com/complex/platform/bar/BarTableTransferRequest.java
backend/src/main/java/com/complex/platform/bar/BarValidation.java
backend/src/main/java/com/complex/platform/bar/BarWriteOperation.java
backend/src/main/java/com/complex/platform/bar/OpenBarTableRequest.java
backend/src/main/java/com/complex/platform/bar/StockBalanceSnapshot.java
backend/src/main/java/com/complex/platform/bar/StockMovementSnapshot.java
<!-- FILES-END -->

## M197 — 酒吧核心门面、控制器与其余／后台验证／第1段

- 聚合维度：`酒吧核心门面、控制器与其余／后台验证`
- 文件数：2
- 有效内容行数：1294
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/bar/BarControllerTests.java
backend/src/test/java/com/complex/platform/bar/BarJdbcIntegrationTests.java
<!-- FILES-END -->

## M198 — 酒吧核心门面、控制器与其余／后台验证／第2段

- 聚合维度：`酒吧核心门面、控制器与其余／后台验证`
- 文件数：1
- 有效内容行数：4576
- 标记：OVERSIZE-SINGLE-FILE

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/bar/BarServiceTests.java
<!-- FILES-END -->

## M199 — 酒吧核心门面、控制器与其余／文档与接口定义／第1段

- 聚合维度：`酒吧核心门面、控制器与其余／文档与接口定义`
- 文件数：2
- 有效内容行数：2686
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/bar-business-operations-product-prd-v0.1.md
docs/bar-operations-development-design.md
<!-- FILES-END -->

## M200 — 酒吧核心门面、控制器与其余／文档与接口定义／第2段

- 聚合维度：`酒吧核心门面、控制器与其余／文档与接口定义`
- 文件数：1
- 有效内容行数：876
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/business-systems-breakdown.md
<!-- FILES-END -->

## M201 — 服务点单：点单、公司统一收款、名录分账与退款／后台实现／第1段

- 聚合维度：`服务点单：点单、公司统一收款、名录分账与退款／后台实现`
- 文件数：13
- 有效内容行数：1042
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/serviceorder/ServiceCatalogItemRecord.java
backend/src/main/java/com/complex/platform/serviceorder/JdbcServiceOrderRepository.java
backend/src/main/java/com/complex/platform/serviceorder/ServiceOrderController.java
backend/src/main/java/com/complex/platform/serviceorder/ServiceOrderRecord.java
backend/src/main/java/com/complex/platform/serviceorder/ServiceOrderService.java
backend/src/main/java/com/complex/platform/serviceorder/dto/ServiceCatalogItemRequest.java
backend/src/main/java/com/complex/platform/serviceorder/dto/ServiceCatalogMutationRequest.java
backend/src/main/java/com/complex/platform/serviceorder/dto/ServiceOrderActionRequest.java
backend/src/main/java/com/complex/platform/serviceorder/dto/ServiceOrderCancelRequest.java
backend/src/main/java/com/complex/platform/serviceorder/dto/ServiceOrderCreateRequest.java
backend/src/main/java/com/complex/platform/serviceorder/dto/ServiceOrderMarkPaidRequest.java
backend/src/main/java/com/complex/platform/serviceorder/dto/ServicePayConfigRequest.java
backend/src/main/java/com/complex/platform/serviceorder/dto/StaffPaymentCodeRequest.java
<!-- FILES-END -->

## M202 — 服务点单：点单、公司统一收款、名录分账与退款／数据库迁移／第1段

- 聚合维度：`服务点单：点单、公司统一收款、名录分账与退款／数据库迁移`
- 文件数：3
- 有效内容行数：84
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V212__service_orders.sql
backend/src/main/resources/db/migration/V213__service_company_collect_tax.sql
backend/src/main/resources/db/migration/V242__service_order_fee_snapshot.sql
<!-- FILES-END -->

## M203 — 服务点单：点单、公司统一收款、名录分账与退款／后台验证／第1段

- 聚合维度：`服务点单：点单、公司统一收款、名录分账与退款／后台验证`
- 文件数：4
- 有效内容行数：613
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/serviceorder/ServiceOrderControllerTerminalPolicyTests.java
backend/src/test/java/com/complex/platform/serviceorder/ServiceOrderJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/serviceorder/ServiceOrderSafetyTests.java
backend/src/test/java/com/complex/platform/serviceorder/ServiceOrderScopeTests.java
<!-- FILES-END -->

## M204 — 服务点单：点单、公司统一收款、名录分账与退款／网页与验证／第1段

- 聚合维度：`服务点单：点单、公司统一收款、名录分账与退款／网页与验证`
- 文件数：3
- 有效内容行数：501
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/ServiceOrdersPage.tsx
apps/web/src/admin/pages/ServiceOrdersPage.kpi.test.tsx
apps/web/src/admin/pages/ServiceOrdersPage.robustness.test.tsx
<!-- FILES-END -->

## M205 — 服务点单：点单、公司统一收款、名录分账与退款／文档与接口定义／第1段

- 聚合维度：`服务点单：点单、公司统一收款、名录分账与退款／文档与接口定义`
- 文件数：1
- 有效内容行数：316
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/service-order-product-prd-v0.1.md
<!-- FILES-END -->

## M206 — 供应商资质、供应合同与采购要货／后台实现／第1段

- 聚合维度：`供应商资质、供应合同与采购要货／后台实现`
- 文件数：19
- 有效内容行数：1351
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/inventory/PurchaseRequestRecord.java
backend/src/main/java/com/complex/platform/inventory/PurchaseRequestService.java
backend/src/main/java/com/complex/platform/inventory/PurchaseRequestEventRecord.java
backend/src/main/java/com/complex/platform/inventory/SupplierQualificationRecord.java
backend/src/main/java/com/complex/platform/inventory/SupplierQualificationService.java
backend/src/main/java/com/complex/platform/inventory/SupplierReconciliationRecord.java
backend/src/main/java/com/complex/platform/inventory/SupplierSettlementMethodRecord.java
backend/src/main/java/com/complex/platform/inventory/SupplyContractRecord.java
backend/src/main/java/com/complex/platform/inventory/SupplyContractService.java
backend/src/main/java/com/complex/platform/inventory/dto/InventorySupplierRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/PurchaseRequestApproveRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/PurchaseRequestCreate.java
backend/src/main/java/com/complex/platform/inventory/dto/PurchaseRequestReceiveRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/PurchaseRequestTransitionRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/SupplierQualificationRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/SupplierReconciliationRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/SupplierSettlementMethodRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/SupplyContractApproveRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/SupplyContractCreateRequest.java
<!-- FILES-END -->

## M207 — 供应商资质、供应合同与采购要货／数据库迁移／第1段

- 聚合维度：`供应商资质、供应合同与采购要货／数据库迁移`
- 文件数：2
- 有效内容行数：99
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V112__supplier_qualification_settlement.sql
backend/src/main/resources/db/migration/V138__inventory_supply_contract_approve_permission.sql
<!-- FILES-END -->

## M208 — 库房、库存类型、流水、盘点报损与高价值复核／后台实现／第1段

- 聚合维度：`库房、库存类型、流水、盘点报损与高价值复核／后台实现`
- 文件数：17
- 有效内容行数：1744
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/inventory/HighValueDecisionService.java
backend/src/main/java/com/complex/platform/inventory/HighValueThresholdPort.java
backend/src/main/java/com/complex/platform/inventory/InventoryHighValueReviewRecord.java
backend/src/main/java/com/complex/platform/inventory/InventoryHighValueReviewService.java
backend/src/main/java/com/complex/platform/inventory/InventoryStockMovementService.java
backend/src/main/java/com/complex/platform/inventory/WarehouseRecord.java
backend/src/main/java/com/complex/platform/inventory/WarehouseService.java
backend/src/main/java/com/complex/platform/inventory/dto/CreateStocktakeRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/HighValueReviewDecisionRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/InventoryStockBatchRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/RecordVarianceRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/ReviewVarianceRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/StockAdjustmentRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/StockInRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/StockMovementRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/WarehouseCreateRequest.java
backend/src/main/java/com/complex/platform/settlement/JdbcHighValueThresholdRepository.java
<!-- FILES-END -->

## M209 — 库房、库存类型、流水、盘点报损与高价值复核／数据库迁移／第1段

- 聚合维度：`库房、库存类型、流水、盘点报损与高价值复核／数据库迁移`
- 文件数：10
- 有效内容行数：689
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V027__p1_inventory_alerts_reconciliation.sql
backend/src/main/resources/db/migration/V061__saved_bottle_high_value_identity.sql
backend/src/main/resources/db/migration/V085__supply_chain_t6.sql
backend/src/main/resources/db/migration/V094__inventory_item_high_value_threshold.sql
backend/src/main/resources/db/migration/V108__inventory_variance_to_ap.sql
backend/src/main/resources/db/migration/V126__inventory_stocktake_permissions.sql
backend/src/main/resources/db/migration/V137__inventory_stock_batch_quantity_nonneg.sql
backend/src/main/resources/db/migration/V296__inventory_cost_ledger.sql
backend/src/main/resources/db/migration/V350__high_value_threshold_unify.sql
backend/src/main/resources/db/migration/V377__pass_gift_variance_ledger.sql
<!-- FILES-END -->

## M210 — 库房、库存类型、流水、盘点报损与高价值复核／后台验证／第1段

- 聚合维度：`库房、库存类型、流水、盘点报损与高价值复核／后台验证`
- 文件数：4
- 有效内容行数：981
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/inventory/HighValueDecisionServiceTests.java
backend/src/test/java/com/complex/platform/inventory/HighValueUnificationJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/inventory/InventoryCostMathTests.java
backend/src/test/java/com/complex/platform/settlement/SettlementGiftVarianceS7Tests.java
<!-- FILES-END -->

## M211 — 库房、库存类型、流水、盘点报损与高价值复核／网页与验证／第1段

- 聚合维度：`库房、库存类型、流水、盘点报损与高价值复核／网页与验证`
- 文件数：2
- 有效内容行数：593
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/InventoryLedgerPage.tsx
apps/web/src/admin/pages/InventoryLedgerPage.test.tsx
<!-- FILES-END -->

## M212 — 库房、库存类型、流水、盘点报损与高价值复核／文档与接口定义／第1段

- 聚合维度：`库房、库存类型、流水、盘点报损与高价值复核／文档与接口定义`
- 文件数：4
- 有效内容行数：1630
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/p1-inventory-alerts-reconciliation-api-contract-v0.1.md
docs/r5-spec-T6-supply-chain-dev-design-20260618.md
docs/supply-chain-inventory-development-design-v0.1.md
docs/supply-chain-inventory-product-prd-v0.1.md
<!-- FILES-END -->

## M213 — 客人存酒、取酒、作废与到期提醒／后台实现／第1段

- 聚合维度：`客人存酒、取酒、作废与到期提醒／后台实现`
- 文件数：8
- 有效内容行数：282
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/inventory/SavedBottleRecord.java
backend/src/main/java/com/complex/platform/inventory/SavedBottleClaimRecord.java
backend/src/main/java/com/complex/platform/inventory/SavedBottleNotificationJob.java
backend/src/main/java/com/complex/platform/inventory/SavedBottleTransferRecord.java
backend/src/main/java/com/complex/platform/inventory/dto/SavedBottleCancelRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/SavedBottleClaimRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/SavedBottleRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/SavedBottleTransferRequest.java
<!-- FILES-END -->

## M214 — 客人存酒、取酒、作废与到期提醒／数据库迁移／第1段

- 聚合维度：`客人存酒、取酒、作废与到期提醒／数据库迁移`
- 文件数：2
- 有效内容行数：251
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V012__p0_inventory_saved_bottle_trace.sql
backend/src/main/resources/db/migration/V102__saved_bottle_walkin_transfer.sql
<!-- FILES-END -->

## M215 — 客人存酒、取酒、作废与到期提醒／后台验证／第1段

- 聚合维度：`客人存酒、取酒、作废与到期提醒／后台验证`
- 文件数：1
- 有效内容行数：357
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/inventory/SavedBottleNotificationIntegrationTest.java
<!-- FILES-END -->

## M216 — 客人存酒、取酒、作废与到期提醒／网页与验证／第1段

- 聚合维度：`客人存酒、取酒、作废与到期提醒／网页与验证`
- 文件数：3
- 有效内容行数：460
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/StoredWinePage.tsx
apps/web/src/admin/pages/StoredWinePage.kpi.test.tsx
apps/web/src/admin/pages/StoredWinePage.metricdetail.test.tsx
<!-- FILES-END -->

## M217 — 客人存酒、取酒、作废与到期提醒／文档与接口定义／第1段

- 聚合维度：`客人存酒、取酒、作废与到期提醒／文档与接口定义`
- 文件数：1
- 有效内容行数：182
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/superpowers/plans/2026-06-14-p0-inventory-saved-bottle-trace-slice.md
<!-- FILES-END -->

## M218 — 跨主体流转、外部经销商准入、经销订单/退货红冲与扫码交接链／后台实现／第1段

- 聚合维度：`跨主体流转、外部经销商准入、经销订单/退货红冲与扫码交接链／后台实现`
- 文件数：19
- 有效内容行数：329
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/inventory/InventoryTraceCodeRecord.java
backend/src/main/java/com/complex/platform/inventory/InventoryTraceOwnershipHistoryRecord.java
backend/src/main/java/com/complex/platform/inventory/InventoryTransferLineRecord.java
backend/src/main/java/com/complex/platform/inventory/InventoryTransferOrderRecord.java
backend/src/main/java/com/complex/platform/inventory/SupplyEntityRecord.java
backend/src/main/java/com/complex/platform/inventory/TraceHandoverRecord.java
backend/src/main/java/com/complex/platform/inventory/TraceHandoverChainView.java
backend/src/main/java/com/complex/platform/inventory/TraceHandoverExceptionRecord.java
backend/src/main/java/com/complex/platform/inventory/TraceHandoverStepRecord.java
backend/src/main/java/com/complex/platform/inventory/dto/CreateTransferOrderRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/PrintTraceCodeRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/ReceiveTransferRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/RegisterTraceCodeRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/SupplyEntityRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/TraceExceptionReviewRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/TraceHandoverCreateRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/TraceRobotReportRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/TraceScanSegmentRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/TransferLineRequest.java
<!-- FILES-END -->

## M219 — 跨主体流转、外部经销商准入、经销订单/退货红冲与扫码交接链／数据库迁移／第1段

- 聚合维度：`跨主体流转、外部经销商准入、经销订单/退货红冲与扫码交接链／数据库迁移`
- 文件数：5
- 有效内容行数：257
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V074__trace_handover_chain.sql
backend/src/main/resources/db/migration/V128__inventory_trace_codes.sql
backend/src/main/resources/db/migration/V139__inventory_trace_handover_merchant_id.sql
backend/src/main/resources/db/migration/V277__inventory_cross_entity_transfer.sql
backend/src/main/resources/db/migration/V278__trace_ownership_history_append_only.sql
<!-- FILES-END -->

## M220 — 跨主体流转、外部经销商准入、经销订单/退货红冲与扫码交接链／后台验证／第1段

- 聚合维度：`跨主体流转、外部经销商准入、经销订单/退货红冲与扫码交接链／后台验证`
- 文件数：1
- 有效内容行数：237
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/inventory/InventoryTransferLifecycleTests.java
<!-- FILES-END -->

## M221 — 跨主体流转、外部经销商准入、经销订单/退货红冲与扫码交接链／网页与验证／第1段

- 聚合维度：`跨主体流转、外部经销商准入、经销订单/退货红冲与扫码交接链／网页与验证`
- 文件数：2
- 有效内容行数：868
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/InventoryTransferWorkbenchPage.tsx
apps/web/src/admin/pages/InventoryTransferWorkbenchPage.test.tsx
<!-- FILES-END -->

## M222 — 跨主体流转、外部经销商准入、经销订单/退货红冲与扫码交接链／文档与接口定义／第1段

- 聚合维度：`跨主体流转、外部经销商准入、经销订单/退货红冲与扫码交接链／文档与接口定义`
- 文件数：2
- 有效内容行数：177
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/r5-spec-T17-trace-handover-chain-dev-design-20260618.md
docs/proposals/库存跨主体流转-平台供货借调-结算与溯源-设计草案.md
<!-- FILES-END -->

## M223 — 库存核心控制器、服务与其余／后台实现／第1段

- 聚合维度：`库存核心控制器、服务与其余／后台实现`
- 文件数：2
- 有效内容行数：900
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/inventory/InventoryController.java
backend/src/main/java/com/complex/platform/inventory/InventoryRepository.java
<!-- FILES-END -->

## M224 — 库存核心控制器、服务与其余／后台实现／第2段

- 聚合维度：`库存核心控制器、服务与其余／后台实现`
- 文件数：1
- 有效内容行数：2673
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/inventory/InventoryService.java
<!-- FILES-END -->

## M225 — 库存核心控制器、服务与其余／后台实现／第3段

- 聚合维度：`库存核心控制器、服务与其余／后台实现`
- 文件数：13
- 有效内容行数：2962
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/inventory/JdbcInventoryRepository.java
backend/src/main/java/com/complex/platform/inventory/InventoryCostMath.java
backend/src/main/java/com/complex/platform/inventory/InventoryCostSummaryRecord.java
backend/src/main/java/com/complex/platform/inventory/InventoryItemRecord.java
backend/src/main/java/com/complex/platform/inventory/InventoryItemCostStateRecord.java
backend/src/main/java/com/complex/platform/inventory/InventoryRiskAlertRecord.java
backend/src/main/java/com/complex/platform/inventory/InventoryRiskScanResult.java
backend/src/main/java/com/complex/platform/inventory/JdbcInventoryRowMapper.java
backend/src/main/java/com/complex/platform/inventory/InventoryStockBatchRecord.java
backend/src/main/java/com/complex/platform/inventory/InventorySupplierRecord.java
backend/src/main/java/com/complex/platform/inventory/InventoryVarianceRecord.java
backend/src/main/java/com/complex/platform/inventory/StockAdjustmentRecord.java
backend/src/main/java/com/complex/platform/inventory/StockBalanceRecord.java
<!-- FILES-END -->

## M226 — 库存核心控制器、服务与其余／后台实现／第4段

- 聚合维度：`库存核心控制器、服务与其余／后台实现`
- 文件数：5
- 有效内容行数：106
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/inventory/StockMovementRecord.java
backend/src/main/java/com/complex/platform/inventory/StocktakeRecord.java
backend/src/main/java/com/complex/platform/inventory/StocktakeDetailRecord.java
backend/src/main/java/com/complex/platform/inventory/dto/InventoryItemRequest.java
backend/src/main/java/com/complex/platform/inventory/dto/InventoryRiskScanRequest.java
<!-- FILES-END -->

## M227 — 库存核心控制器、服务与其余／后台验证／第1段

- 聚合维度：`库存核心控制器、服务与其余／后台验证`
- 文件数：2
- 有效内容行数：1124
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/inventory/InventoryControllerTests.java
backend/src/test/java/com/complex/platform/inventory/InventoryJdbcIntegrationTests.java
<!-- FILES-END -->

## M228 — 库存核心控制器、服务与其余／后台验证／第2段

- 聚合维度：`库存核心控制器、服务与其余／后台验证`
- 文件数：1
- 有效内容行数：2600
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/inventory/InventoryServiceTests.java
<!-- FILES-END -->

## M229 — 库存核心控制器、服务与其余／后台验证／第3段

- 聚合维度：`库存核心控制器、服务与其余／后台验证`
- 文件数：1
- 有效内容行数：761
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/inventory/SupplyChainT6ServiceTests.java
<!-- FILES-END -->

## M230 — 游乐项目、场次、实时在玩人数容量与入场核销／后台实现／第1段

- 聚合维度：`游乐项目、场次、实时在玩人数容量与入场核销／后台实现`
- 文件数：11
- 有效内容行数：2666
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/amusement/AmusementController.java
backend/src/main/java/com/complex/platform/amusement/AmusementService.java
backend/src/main/java/com/complex/platform/amusement/AmusementCapacityConfigRecord.java
backend/src/main/java/com/complex/platform/amusement/AmusementCapacityConfigRequest.java
backend/src/main/java/com/complex/platform/amusement/AmusementCapacitySnapshotRecord.java
backend/src/main/java/com/complex/platform/amusement/AmusementManualOverrideRecord.java
backend/src/main/java/com/complex/platform/amusement/AmusementManualOverrideRequest.java
backend/src/main/java/com/complex/platform/amusement/AmusementOverCapacityPolicy.java
backend/src/main/java/com/complex/platform/amusement/CapacityClaimContext.java
backend/src/main/java/com/complex/platform/amusement/CapacityClaimResult.java
backend/src/main/java/com/complex/platform/amusement/ConfigureOverCapacityPolicyRequest.java
<!-- FILES-END -->

## M231 — 游乐项目、场次、实时在玩人数容量与入场核销／数据库迁移／第1段

- 聚合维度：`游乐项目、场次、实时在玩人数容量与入场核销／数据库迁移`
- 文件数：6
- 有效内容行数：180
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V032__p1_amusement_reservation_queue_write_permissions.sql
backend/src/main/resources/db/migration/V082__amusement_project_entitlement_binding.sql
backend/src/main/resources/db/migration/V083__amusement_three_dim_capacity.sql
backend/src/main/resources/db/migration/V104__amusement_incident_type_over_capacity.sql
backend/src/main/resources/db/migration/V127__amusement_over_capacity_policy_permissions.sql
backend/src/main/resources/db/migration/V132__amusement_verification_verified_people.sql
<!-- FILES-END -->

## M232 — 游乐项目、场次、实时在玩人数容量与入场核销／后台验证／第1段

- 聚合维度：`游乐项目、场次、实时在玩人数容量与入场核销／后台验证`
- 文件数：1
- 有效内容行数：3391
- 标记：OVERSIZE-SINGLE-FILE

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/amusement/AmusementServiceTests.java
<!-- FILES-END -->

## M233 — 游乐项目、场次、实时在玩人数容量与入场核销／后台验证／第2段

- 聚合维度：`游乐项目、场次、实时在玩人数容量与入场核销／后台验证`
- 文件数：2
- 有效内容行数：689
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/ticketing/AmusementExitReleaseJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/ticketing/AmusementVerifyRoutingJdbcIntegrationTests.java
<!-- FILES-END -->

## M234 — 游乐项目、场次、实时在玩人数容量与入场核销／网页与验证／第1段

- 聚合维度：`游乐项目、场次、实时在玩人数容量与入场核销／网页与验证`
- 文件数：8
- 有效内容行数：1433
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/AmusementSessionsPage.tsx
apps/web/src/admin/pages/AmusementSessionsPage.entitlementsoftdelete.test.tsx
apps/web/src/admin/pages/AmusementSessionsPage.metricdetail.test.tsx
apps/web/src/admin/pages/AmusementSessionsPage.test.tsx
apps/web/src/admin/pages/AmusementVerificationRecordsPage.tsx
apps/web/src/admin/pages/AmusementVerificationRecordsPage.capacityleft.test.tsx
apps/web/src/admin/pages/AmusementVerificationRecordsPage.metricdetail.test.tsx
apps/web/src/admin/pages/AmusementVerificationRecordsPage.robustness.test.tsx
<!-- FILES-END -->

## M235 — 游乐项目、场次、实时在玩人数容量与入场核销／文档与接口定义／第1段

- 聚合维度：`游乐项目、场次、实时在玩人数容量与入场核销／文档与接口定义`
- 文件数：3
- 有效内容行数：1784
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/amusement-business-operations-product-prd-v0.1.md
docs/amusement-operations-development-design.md
docs/p1-amusement-reservation-queue-light-api-contract-v0.1.md
<!-- FILES-END -->

## M236 — 开放前安全巡检、严重度四档、整改复检主管确认与检修记录／后台实现／第1段

- 聚合维度：`开放前安全巡检、严重度四档、整改复检主管确认与检修记录／后台实现`
- 文件数：7
- 有效内容行数：1080
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/amusement/AmusementInspectionService.java
backend/src/main/java/com/complex/platform/amusement/AmusementInspectionTaskRecord.java
backend/src/main/java/com/complex/platform/amusement/AmusementInspectionTaskJob.java
backend/src/main/java/com/complex/platform/amusement/AmusementMaintenanceRecord.java
backend/src/main/java/com/complex/platform/amusement/AmusementMaintenanceService.java
backend/src/main/java/com/complex/platform/amusement/AmusementMaintenanceRecordRequest.java
backend/src/main/java/com/complex/platform/amusement/AmusementMaintenanceResumeRequest.java
<!-- FILES-END -->

## M237 — 开放前安全巡检、严重度四档、整改复检主管确认与检修记录／数据库迁移／第1段

- 聚合维度：`开放前安全巡检、严重度四档、整改复检主管确认与检修记录／数据库迁移`
- 文件数：8
- 有效内容行数：631
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V014__p0_amusement_safety_session_verification.sql
backend/src/main/resources/db/migration/V024__p1_amusement_safety_waiver_incident.sql
backend/src/main/resources/db/migration/V081__amusement_maintenance_record.sql
backend/src/main/resources/db/migration/V114__amusement_inspection_structured.sql
backend/src/main/resources/db/migration/V116__amusement_maintenance_vendor_fk.sql
backend/src/main/resources/db/migration/V123__amusement_maintenance_vendor_fk_validate.sql
backend/src/main/resources/db/migration/V143__amusement_device_safety_cycle.sql
backend/src/main/resources/db/migration/V144__amusement_device_permissions.sql
<!-- FILES-END -->

## M238 — 开放前安全巡检、严重度四档、整改复检主管确认与检修记录／网页与验证／第1段

- 聚合维度：`开放前安全巡检、严重度四档、整改复检主管确认与检修记录／网页与验证`
- 文件数：2
- 有效内容行数：183
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/AmusementInspectionTasksPage.tsx
apps/web/src/admin/pages/AmusementInspectionTasksPage.test.tsx
<!-- FILES-END -->

## M239 — 开放前安全巡检、严重度四档、整改复检主管确认与检修记录／文档与接口定义／第1段

- 聚合维度：`开放前安全巡检、严重度四档、整改复检主管确认与检修记录／文档与接口定义`
- 文件数：2
- 有效内容行数：516
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/amusement-p0-safety-inspection-spec-v0.1.md
docs/superpowers/plans/2026-06-14-p0-amusement-safety-session-verification-slice.md
<!-- FILES-END -->

## M240 — 使用限制、团体/包场预订、事故与免责／后台实现／第1段

- 聚合维度：`使用限制、团体/包场预订、事故与免责／后台实现`
- 文件数：22
- 有效内容行数：1735
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/amusement/AmusementBookingService.java
backend/src/main/java/com/complex/platform/amusement/AmusementGroupBookingRecord.java
backend/src/main/java/com/complex/platform/amusement/AmusementGroupBookingApprovalRequest.java
backend/src/main/java/com/complex/platform/amusement/AmusementGroupBookingReleaseRequest.java
backend/src/main/java/com/complex/platform/amusement/AmusementGroupBookingRequest.java
backend/src/main/java/com/complex/platform/amusement/AmusementIncidentRecord.java
backend/src/main/java/com/complex/platform/amusement/AmusementIncidentActionRecord.java
backend/src/main/java/com/complex/platform/amusement/AmusementSafetyController.java
backend/src/main/java/com/complex/platform/amusement/AmusementSafetyRepository.java
backend/src/main/java/com/complex/platform/amusement/AmusementSafetyService.java
backend/src/main/java/com/complex/platform/amusement/AmusementSafetyActionRecord.java
backend/src/main/java/com/complex/platform/amusement/AmusementSafetyActionRequest.java
backend/src/main/java/com/complex/platform/amusement/AmusementSafetyInspectionRecord.java
backend/src/main/java/com/complex/platform/amusement/AmusementSafetyInspectionRequest.java
backend/src/main/java/com/complex/platform/amusement/AmusementSafetyNoticeRecord.java
backend/src/main/java/com/complex/platform/amusement/AmusementSafetyTemplateRecord.java
backend/src/main/java/com/complex/platform/amusement/AmusementSafetyTemplateRequest.java
backend/src/main/java/com/complex/platform/amusement/dto/AmusementIncidentActionRequest.java
backend/src/main/java/com/complex/platform/amusement/dto/AmusementIncidentCloseRequest.java
backend/src/main/java/com/complex/platform/amusement/dto/AmusementIncidentCreateRequest.java
backend/src/main/java/com/complex/platform/amusement/dto/AmusementSafetyNoticeCreateRequest.java
backend/src/main/java/com/complex/platform/amusement/dto/AmusementWaiverAcknowledgementRequest.java
<!-- FILES-END -->

## M241 — 使用限制、团体/包场预订、事故与免责／数据库迁移／第1段

- 聚合维度：`使用限制、团体/包场预订、事故与免责／数据库迁移`
- 文件数：1
- 有效内容行数：64
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V035__p2_amusement_group_private_bookings.sql
<!-- FILES-END -->

## M242 — 使用限制、团体/包场预订、事故与免责／后台验证／第1段

- 聚合维度：`使用限制、团体/包场预订、事故与免责／后台验证`
- 文件数：3
- 有效内容行数：1005
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/amusement/AmusementSafetyControllerTests.java
backend/src/test/java/com/complex/platform/amusement/AmusementSafetyJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/amusement/AmusementSafetyServiceTests.java
<!-- FILES-END -->

## M243 — 使用限制、团体/包场预订、事故与免责／网页与验证／第1段

- 聚合维度：`使用限制、团体/包场预订、事故与免责／网页与验证`
- 文件数：6
- 有效内容行数：1120
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/AmusementGroupBookingsPage.tsx
apps/web/src/admin/pages/AmusementGroupBookingsPage.metricdetail.test.tsx
apps/web/src/admin/pages/AmusementGroupBookingsPage.test.tsx
apps/web/src/admin/pages/AmusementSafetyPage.tsx
apps/web/src/admin/pages/AmusementSafetyPage.metricdetail.test.tsx
apps/web/src/admin/pages/AmusementSafetyPage.test.tsx
<!-- FILES-END -->

## M244 — 使用限制、团体/包场预订、事故与免责／文档与接口定义／第1段

- 聚合维度：`使用限制、团体/包场预订、事故与免责／文档与接口定义`
- 文件数：5
- 有效内容行数：472
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/p1-amusement-safety-waiver-incident-api-contract-v0.1.md
docs/p2-amusement-group-private-booking-api-contract-v0.1.md
docs/p2-amusement-group-private-booking-completion-audit-v0.1.md
docs/superpowers/plans/2026-06-14-p1-amusement-safety-waiver-incident.md
docs/superpowers/plans/2026-06-15-p2-amusement-group-private-booking.md
<!-- FILES-END -->

## M245 — 游乐核心与其余／后台实现／第1段

- 聚合维度：`游乐核心与其余／后台实现`
- 文件数：24
- 有效内容行数：2823
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/amusement/AmusementRepository.java
backend/src/main/java/com/complex/platform/amusement/JdbcAmusementRepository.java
backend/src/main/java/com/complex/platform/amusement/AmusementDeviceRecord.java
backend/src/main/java/com/complex/platform/amusement/AmusementDeviceRequest.java
backend/src/main/java/com/complex/platform/amusement/AmusementDeviceStatusRecord.java
backend/src/main/java/com/complex/platform/amusement/AmusementEntitlementGateway.java
backend/src/main/java/com/complex/platform/amusement/AmusementEntitlementView.java
backend/src/main/java/com/complex/platform/amusement/AmusementEntryRequest.java
backend/src/main/java/com/complex/platform/amusement/AmusementExitRequest.java
backend/src/main/java/com/complex/platform/amusement/AmusementOpenEligibilityResponse.java
backend/src/main/java/com/complex/platform/amusement/AmusementProjectRecord.java
backend/src/main/java/com/complex/platform/amusement/AmusementProjectDeviceRegistryBacklogRecord.java
backend/src/main/java/com/complex/platform/amusement/AmusementProjectEntitlementRecord.java
backend/src/main/java/com/complex/platform/amusement/AmusementProjectEntitlementRequest.java
backend/src/main/java/com/complex/platform/amusement/AmusementProjectRequest.java
backend/src/main/java/com/complex/platform/amusement/AmusementProjectRuleRecord.java
backend/src/main/java/com/complex/platform/amusement/AmusementProjectRuleRequest.java
backend/src/main/java/com/complex/platform/amusement/AmusementProjectSpaceAssignmentRecord.java
backend/src/main/java/com/complex/platform/amusement/AmusementProjectSpaceAssignmentRequest.java
backend/src/main/java/com/complex/platform/amusement/AmusementProjectSpaceBacklogRecord.java
backend/src/main/java/com/complex/platform/amusement/AmusementQueueEntryRecord.java
backend/src/main/java/com/complex/platform/amusement/AmusementQueueEntryRequest.java
backend/src/main/java/com/complex/platform/amusement/AmusementReservationRecord.java
backend/src/main/java/com/complex/platform/amusement/AmusementReservationRequest.java
<!-- FILES-END -->

## M246 — 游乐核心与其余／后台实现／第2段

- 聚合维度：`游乐核心与其余／后台实现`
- 文件数：11
- 有效内容行数：967
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/amusement/JdbcAmusementRowMapper.java
backend/src/main/java/com/complex/platform/amusement/JdbcAmusementSafetyRepository.java
backend/src/main/java/com/complex/platform/amusement/AmusementSessionRecord.java
backend/src/main/java/com/complex/platform/amusement/AmusementSessionRequest.java
backend/src/main/java/com/complex/platform/amusement/AmusementVerificationRecord.java
backend/src/main/java/com/complex/platform/amusement/AmusementVerificationListItem.java
backend/src/main/java/com/complex/platform/amusement/AmusementVerificationQuery.java
backend/src/main/java/com/complex/platform/amusement/AmusementWaiverAcknowledgementRecord.java
backend/src/main/java/com/complex/platform/amusement/ClaimEntitlementSessionRequest.java
backend/src/main/java/com/complex/platform/amusement/EntitlementSessionRecord.java
backend/src/main/java/com/complex/platform/amusement/InitiateEntitlementSessionRequest.java
<!-- FILES-END -->

## M247 — 游乐核心与其余／数据库迁移／第1段

- 聚合维度：`游乐核心与其余／数据库迁移`
- 文件数：3
- 有效内容行数：142
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V129__entitlement_sessions.sql
backend/src/main/resources/db/migration/V134__entitlement_sessions_merchant_id_nullable.sql
backend/src/main/resources/db/migration/V226__entitlement_session_kind_batch3.sql
<!-- FILES-END -->

## M248 — 游乐核心与其余／后台验证／第1段

- 聚合维度：`游乐核心与其余／后台验证`
- 文件数：2
- 有效内容行数：999
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/amusement/AmusementControllerTests.java
backend/src/test/java/com/complex/platform/amusement/AmusementJdbcIntegrationTests.java
<!-- FILES-END -->

## M249 — 招商线索、意向、品牌库与签约转商户／后台实现／第1段

- 聚合维度：`招商线索、意向、品牌库与签约转商户／后台实现`
- 文件数：17
- 有效内容行数：1307
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/leasing/JdbcLeasingRepository.java
backend/src/main/java/com/complex/platform/leasing/LeasingController.java
backend/src/main/java/com/complex/platform/leasing/LeasingRepository.java
backend/src/main/java/com/complex/platform/leasing/LeasingService.java
backend/src/main/java/com/complex/platform/leasing/LeasingBrandRecord.java
backend/src/main/java/com/complex/platform/leasing/LeasingFollowUpRecord.java
backend/src/main/java/com/complex/platform/leasing/LeasingIntentRecord.java
backend/src/main/java/com/complex/platform/leasing/LeasingLeadRecord.java
backend/src/main/java/com/complex/platform/leasing/dto/LeasingBrandBlacklistRequest.java
backend/src/main/java/com/complex/platform/leasing/dto/LeasingBrandRequest.java
backend/src/main/java/com/complex/platform/leasing/dto/LeasingFollowUpRequest.java
backend/src/main/java/com/complex/platform/leasing/dto/LeasingIntentAdvanceRequest.java
backend/src/main/java/com/complex/platform/leasing/dto/LeasingIntentRequest.java
backend/src/main/java/com/complex/platform/leasing/dto/LeasingLeadAssignRequest.java
backend/src/main/java/com/complex/platform/leasing/dto/LeasingLeadRequest.java
backend/src/main/java/com/complex/platform/leasing/dto/LeasingLeadStatusRequest.java
backend/src/main/java/com/complex/platform/leasing/dto/LeasingSignRequest.java
<!-- FILES-END -->

## M250 — 招商线索、意向、品牌库与签约转商户／数据库迁移／第1段

- 聚合维度：`招商线索、意向、品牌库与签约转商户／数据库迁移`
- 文件数：1
- 有效内容行数：98
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V161__leasing_acquisition.sql
<!-- FILES-END -->

## M251 — 招商线索、意向、品牌库与签约转商户／后台验证／第1段

- 聚合维度：`招商线索、意向、品牌库与签约转商户／后台验证`
- 文件数：3
- 有效内容行数：657
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/leasing/LeasingJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/leasing/LeasingServiceTests.java
backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java
<!-- FILES-END -->

## M252 — 招商线索、意向、品牌库与签约转商户／网页与验证／第1段

- 聚合维度：`招商线索、意向、品牌库与签约转商户／网页与验证`
- 文件数：4
- 有效内容行数：1015
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/LeasingRentalStatusPage.tsx
apps/web/src/admin/pages/LeasingRentalStatusPage.test.tsx
apps/web/src/admin/pages/LeasingWorkbenchPage.tsx
apps/web/src/admin/pages/LeasingWorkbenchPage.test.tsx
<!-- FILES-END -->

## M253 — 招商线索、意向、品牌库与签约转商户／文档与接口定义／第1段

- 聚合维度：`招商线索、意向、品牌库与签约转商户／文档与接口定义`
- 文件数：1
- 有效内容行数：124
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/leasing-acquisition-prd-v1.0.md
<!-- FILES-END -->

## M254 — 合同履约、审批链、变更、退租结算、租金与营业额抽成／后台实现／第1段

- 聚合维度：`合同履约、审批链、变更、退租结算、租金与营业额抽成／后台实现`
- 文件数：12
- 有效内容行数：2841
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/contract/ContractController.java
backend/src/main/java/com/complex/platform/contract/ContractRecord.java
backend/src/main/java/com/complex/platform/contract/ContractRepository.java
backend/src/main/java/com/complex/platform/contract/ContractService.java
backend/src/main/java/com/complex/platform/contract/JdbcContractRepository.java
backend/src/main/java/com/complex/platform/contract/ContractApprovalRecord.java
backend/src/main/java/com/complex/platform/contract/ContractApprovalFlowStepRecord.java
backend/src/main/java/com/complex/platform/contract/ContractChangeRecord.java
backend/src/main/java/com/complex/platform/contract/ContractExpiryJob.java
backend/src/main/java/com/complex/platform/contract/ContractRenewalNotificationJob.java
backend/src/main/java/com/complex/platform/contract/ContractRentBillRecord.java
backend/src/main/java/com/complex/platform/contract/ContractRentMath.java
<!-- FILES-END -->

## M255 — 合同履约、审批链、变更、退租结算、租金与营业额抽成／后台实现／第2段

- 聚合维度：`合同履约、审批链、变更、退租结算、租金与营业额抽成／后台实现`
- 文件数：25
- 有效内容行数：711
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/contract/ContractScopeGuard.java
backend/src/main/java/com/complex/platform/contract/ContractSettlementRecord.java
backend/src/main/java/com/complex/platform/contract/SpaceExpiryColorRuleRecord.java
backend/src/main/java/com/complex/platform/contract/SpaceRentalStatusRecord.java
backend/src/main/java/com/complex/platform/contract/TerminationSettlementMath.java
backend/src/main/java/com/complex/platform/contract/dto/ContractActivateRequest.java
backend/src/main/java/com/complex/platform/contract/dto/ContractApprovalFlowRequest.java
backend/src/main/java/com/complex/platform/contract/dto/ContractApproveRequest.java
backend/src/main/java/com/complex/platform/contract/dto/ContractAttachmentRequest.java
backend/src/main/java/com/complex/platform/contract/dto/ContractChangeApproveRequest.java
backend/src/main/java/com/complex/platform/contract/dto/ContractChangeRequest.java
backend/src/main/java/com/complex/platform/contract/dto/ContractDraftRequest.java
backend/src/main/java/com/complex/platform/contract/dto/ContractRentBillRequest.java
backend/src/main/java/com/complex/platform/contract/dto/ContractResolveDisputeRequest.java
backend/src/main/java/com/complex/platform/contract/dto/ContractScopeRequestRequest.java
backend/src/main/java/com/complex/platform/contract/dto/ContractSettleRequest.java
backend/src/main/java/com/complex/platform/contract/dto/ContractSubmitRequest.java
backend/src/main/java/com/complex/platform/contract/dto/SpaceExpiryColorRuleRequest.java
backend/src/main/java/com/complex/platform/finance/DepositLedgerRecord.java
backend/src/main/java/com/complex/platform/finance/TerminationSettlementPort.java
backend/src/main/java/com/complex/platform/finance/TurnoverRentMath.java
backend/src/main/java/com/complex/platform/finance/TurnoverRentRuleRecord.java
backend/src/main/java/com/complex/platform/finance/TurnoverRentRuleTierRecord.java
backend/src/main/java/com/complex/platform/finance/dto/DepositActionRequest.java
backend/src/main/java/com/complex/platform/finance/dto/TurnoverRentBillRequest.java
<!-- FILES-END -->

## M256 — 合同履约、审批链、变更、退租结算、租金与营业额抽成／后台实现／第3段

- 聚合维度：`合同履约、审批链、变更、退租结算、租金与营业额抽成／后台实现`
- 文件数：1
- 有效内容行数：24
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/finance/dto/TurnoverRentRuleRequest.java
<!-- FILES-END -->

## M257 — 合同履约、审批链、变更、退租结算、租金与营业额抽成／数据库迁移／第1段

- 聚合维度：`合同履约、审批链、变更、退租结算、租金与营业额抽成／数据库迁移`
- 文件数：10
- 有效内容行数：564
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V028__p1_contract_compliance_expiry.sql
backend/src/main/resources/db/migration/V080__contract_scope_and_spaces.sql
backend/src/main/resources/db/migration/V110__complex_billing.sql
backend/src/main/resources/db/migration/V162__contract_lifecycle.sql
backend/src/main/resources/db/migration/V163__contract_settlement_status_5states.sql
backend/src/main/resources/db/migration/V164__contract_rent_bill_fee_type.sql
backend/src/main/resources/db/migration/V171__contract_settlement_status_widen.sql
backend/src/main/resources/db/migration/V281__turnover_rent_contract_and_tiers.sql
backend/src/main/resources/db/migration/V307__contract_settlement_voided_state.sql
backend/src/main/resources/db/migration/V346__p0e_reconciliation_no_manual_and_turnover_rent_draft_gate.sql
<!-- FILES-END -->

## M258 — 合同履约、审批链、变更、退租结算、租金与营业额抽成／后台验证／第1段

- 聚合维度：`合同履约、审批链、变更、退租结算、租金与营业额抽成／后台验证`
- 文件数：7
- 有效内容行数：2061
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/contract/ContractJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/contract/ContractScopeEnforcementJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/contract/ContractScopeGuardTests.java
backend/src/test/java/com/complex/platform/contract/ContractServiceTests.java
backend/src/test/java/com/complex/platform/contract/TerminationSettlementMathTests.java
backend/src/test/java/com/complex/platform/finance/TurnoverRentMathTests.java
backend/src/test/java/com/complex/platform/settlement/TurnoverRentRuleDraftGateJdbcIntegrationTests.java
<!-- FILES-END -->

## M259 — 合同履约、审批链、变更、退租结算、租金与营业额抽成／网页与验证／第1段

- 聚合维度：`合同履约、审批链、变更、退租结算、租金与营业额抽成／网页与验证`
- 文件数：3
- 有效内容行数：1257
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/ContractWorkbenchPage.tsx
apps/web/src/admin/pages/ContractWorkbenchPage.robustness.test.tsx
apps/web/src/admin/pages/ContractWorkbenchPage.test.tsx
<!-- FILES-END -->

## M260 — 合同履约、审批链、变更、退租结算、租金与营业额抽成／端到端验证／第1段

- 聚合维度：`合同履约、审批链、变更、退租结算、租金与营业额抽成／端到端验证`
- 文件数：1
- 有效内容行数：90
- 标记：NORMAL

<!-- FILES-BEGIN -->
tests/e2e/leasing-contract-live-evidence.spec.ts
<!-- FILES-END -->

## M261 — 合同履约、审批链、变更、退租结算、租金与营业额抽成／文档与接口定义／第1段

- 聚合维度：`合同履约、审批链、变更、退租结算、租金与营业额抽成／文档与接口定义`
- 文件数：3
- 有效内容行数：479
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/contract-date-compliance-field-granularity-options-20260610.md
docs/merchant-space-contract-model-v0.1.md
docs/real-estate-full-chain-domain-prd-v0.1.md
<!-- FILES-END -->

## M262 — 证照资质保险到期预警、阻断与宽限期／后台实现／第1段

- 聚合维度：`证照资质保险到期预警、阻断与宽限期／后台实现`
- 文件数：11
- 有效内容行数：1140
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/compliance/ComplianceController.java
backend/src/main/java/com/complex/platform/compliance/ComplianceRepository.java
backend/src/main/java/com/complex/platform/compliance/ComplianceService.java
backend/src/main/java/com/complex/platform/compliance/JdbcComplianceRepository.java
backend/src/main/java/com/complex/platform/compliance/ComplianceExpiryItemRecord.java
backend/src/main/java/com/complex/platform/compliance/ComplianceReminderRecord.java
backend/src/main/java/com/complex/platform/compliance/ComplianceReminderScanResult.java
backend/src/main/java/com/complex/platform/compliance/ContractKeyDateRecord.java
backend/src/main/java/com/complex/platform/compliance/dto/ComplianceExpiryItemRequest.java
backend/src/main/java/com/complex/platform/compliance/dto/ComplianceReminderScanRequest.java
backend/src/main/java/com/complex/platform/compliance/dto/ContractKeyDateRequest.java
<!-- FILES-END -->

## M263 — 证照资质保险到期预警、阻断与宽限期／后台验证／第1段

- 聚合维度：`证照资质保险到期预警、阻断与宽限期／后台验证`
- 文件数：3
- 有效内容行数：769
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/compliance/ComplianceControllerTests.java
backend/src/test/java/com/complex/platform/compliance/ComplianceJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/compliance/ComplianceServiceTests.java
<!-- FILES-END -->

## M264 — 证照资质保险到期预警、阻断与宽限期／网页与验证／第1段

- 聚合维度：`证照资质保险到期预警、阻断与宽限期／网页与验证`
- 文件数：1
- 有效内容行数：321
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/ComplianceRemindersPage.tsx
<!-- FILES-END -->

## M265 — 证照资质保险到期预警、阻断与宽限期／文档与接口定义／第1段

- 聚合维度：`证照资质保险到期预警、阻断与宽限期／文档与接口定义`
- 文件数：2
- 有效内容行数：229
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/p1-contract-compliance-expiry-api-contract-v0.1.md
docs/superpowers/plans/2026-06-14-p1-contract-compliance-expiry.md
<!-- FILES-END -->

## M266 — 物业工单、SLA 矩阵、外包进场报备与商户服务工单／后台实现／第1段

- 聚合维度：`物业工单、SLA 矩阵、外包进场报备与商户服务工单／后台实现`
- 文件数：25
- 有效内容行数：2856
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/facility/FacilityWorkOrderController.java
backend/src/main/java/com/complex/platform/facility/FacilityWorkOrderRecord.java
backend/src/main/java/com/complex/platform/facility/FacilityWorkOrderRepository.java
backend/src/main/java/com/complex/platform/facility/FacilityWorkOrderService.java
backend/src/main/java/com/complex/platform/facility/JdbcFacilityWorkOrderRepository.java
backend/src/main/java/com/complex/platform/facility/FacilityWorkOrderPolicyRecord.java
backend/src/main/java/com/complex/platform/facility/JdbcSiteEntryReportRepository.java
backend/src/main/java/com/complex/platform/facility/SiteEntryReportController.java
backend/src/main/java/com/complex/platform/facility/SiteEntryReportRecord.java
backend/src/main/java/com/complex/platform/facility/SiteEntryReportRepository.java
backend/src/main/java/com/complex/platform/facility/SiteEntryReportService.java
backend/src/main/java/com/complex/platform/facility/dto/FacilityWorkOrderActionRequest.java
backend/src/main/java/com/complex/platform/facility/dto/FacilityWorkOrderAssignRequest.java
backend/src/main/java/com/complex/platform/facility/dto/FacilityWorkOrderPolicyRequest.java
backend/src/main/java/com/complex/platform/facility/dto/FacilityWorkOrderRequest.java
backend/src/main/java/com/complex/platform/facility/dto/SiteEntryCompletionUploadRequest.java
backend/src/main/java/com/complex/platform/facility/dto/SiteEntryReportAcceptanceRequest.java
backend/src/main/java/com/complex/platform/facility/dto/SiteEntryReportRequest.java
backend/src/main/java/com/complex/platform/facility/dto/SiteEntryReportReviewRequest.java
backend/src/main/java/com/complex/platform/facility/dto/SiteEntryReportSubmitRequest.java
backend/src/main/java/com/complex/platform/merchantservice/JdbcMerchantServiceRepository.java
backend/src/main/java/com/complex/platform/merchantservice/MerchantServiceController.java
backend/src/main/java/com/complex/platform/merchantservice/MerchantServiceRepository.java
backend/src/main/java/com/complex/platform/merchantservice/MerchantServiceService.java
backend/src/main/java/com/complex/platform/merchantservice/MerchantWorkOrderRecord.java
<!-- FILES-END -->

## M267 — 物业工单、SLA 矩阵、外包进场报备与商户服务工单／后台实现／第2段

- 聚合维度：`物业工单、SLA 矩阵、外包进场报备与商户服务工单／后台实现`
- 文件数：10
- 有效内容行数：150
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/merchantservice/MerchantWorkOrderActionRecord.java
backend/src/main/java/com/complex/platform/merchantservice/MerchantWorkOrderSlaEscalationJob.java
backend/src/main/java/com/complex/platform/merchantservice/dto/MerchantWorkOrderActionRequest.java
backend/src/main/java/com/complex/platform/merchantservice/dto/MerchantWorkOrderArchiveRequest.java
backend/src/main/java/com/complex/platform/merchantservice/dto/MerchantWorkOrderAssignRequest.java
backend/src/main/java/com/complex/platform/merchantservice/dto/MerchantWorkOrderCloseRequest.java
backend/src/main/java/com/complex/platform/merchantservice/dto/MerchantWorkOrderCreateRequest.java
backend/src/main/java/com/complex/platform/merchantservice/dto/MerchantWorkOrderDeleteApprovalRequest.java
backend/src/main/java/com/complex/platform/merchantservice/dto/MerchantWorkOrderDeleteDecision.java
backend/src/main/java/com/complex/platform/merchantservice/dto/MerchantWorkOrderDeleteRequest.java
<!-- FILES-END -->

## M268 — 物业工单、SLA 矩阵、外包进场报备与商户服务工单／数据库迁移／第1段

- 聚合维度：`物业工单、SLA 矩阵、外包进场报备与商户服务工单／数据库迁移`
- 文件数：4
- 有效内容行数：361
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V022__p1_merchant_service_work_orders.sql
backend/src/main/resources/db/migration/V120__merchant_service_work_order_sla_escalation.sql
backend/src/main/resources/db/migration/V166__facility_work_orders.sql
backend/src/main/resources/db/migration/V179__site_entry_reports.sql
<!-- FILES-END -->

## M269 — 物业工单、SLA 矩阵、外包进场报备与商户服务工单／后台验证／第1段

- 聚合维度：`物业工单、SLA 矩阵、外包进场报备与商户服务工单／后台验证`
- 文件数：7
- 有效内容行数：2115
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/facility/FacilityWorkOrderControllerTests.java
backend/src/test/java/com/complex/platform/facility/FacilityWorkOrderServiceTests.java
backend/src/test/java/com/complex/platform/facility/SiteEntryReportControllerTests.java
backend/src/test/java/com/complex/platform/facility/SiteEntryReportServiceTests.java
backend/src/test/java/com/complex/platform/merchantservice/MerchantServiceControllerTests.java
backend/src/test/java/com/complex/platform/merchantservice/MerchantServiceJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/merchantservice/MerchantServiceServiceTests.java
<!-- FILES-END -->

## M270 — 物业工单、SLA 矩阵、外包进场报备与商户服务工单／网页与验证／第1段

- 聚合维度：`物业工单、SLA 矩阵、外包进场报备与商户服务工单／网页与验证`
- 文件数：11
- 有效内容行数：1991
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/MerchantWorkOrdersPage.tsx
apps/web/src/admin/pages/MerchantWorkOrdersPage.test.tsx
apps/web/src/admin/pages/MerchantZoneWorkOrdersPage.tsx
apps/web/src/admin/pages/MerchantZoneWorkOrdersPage.test.tsx
apps/web/src/admin/pages/VendorSiteEntriesPage.tsx
apps/web/src/admin/pages/VendorSiteEntriesPage.test.tsx
apps/web/src/admin/pages/VendorWorkOrdersPage.tsx
apps/web/src/admin/pages/VendorWorkOrdersPage.test.tsx
apps/web/src/admin/pages/WorkOrderSlaPage.tsx
apps/web/src/admin/pages/WorkOrderSlaPage.kpi.test.tsx
apps/web/src/admin/pages/WorkOrderSlaPage.robustness.test.tsx
<!-- FILES-END -->

## M271 — 物业工单、SLA 矩阵、外包进场报备与商户服务工单／文档与接口定义／第1段

- 聚合维度：`物业工单、SLA 矩阵、外包进场报备与商户服务工单／文档与接口定义`
- 文件数：1
- 有效内容行数：237
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/p1-merchant-service-work-order-api-contract-v0.1.md
<!-- FILES-END -->

## M272 — 楼宇机电设备台账、维保、告警与楼宇巡检／后台实现／第1段

- 聚合维度：`楼宇机电设备台账、维保、告警与楼宇巡检／后台实现`
- 文件数：25
- 有效内容行数：1875
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/facility/FacilityController.java
backend/src/main/java/com/complex/platform/facility/FacilityRepository.java
backend/src/main/java/com/complex/platform/facility/JdbcFacilityRepository.java
backend/src/main/java/com/complex/platform/facility/FacilityEquipmentRecord.java
backend/src/main/java/com/complex/platform/facility/FacilityEquipmentService.java
backend/src/main/java/com/complex/platform/facility/FacilityEquipmentAlarmedEvent.java
backend/src/main/java/com/complex/platform/facility/FacilityInspectionController.java
backend/src/main/java/com/complex/platform/facility/FacilityInspectionRepository.java
backend/src/main/java/com/complex/platform/facility/FacilityInspectionService.java
backend/src/main/java/com/complex/platform/facility/JdbcFacilityInspectionRepository.java
backend/src/main/java/com/complex/platform/facility/FacilityInspectionAbnormalEvent.java
backend/src/main/java/com/complex/platform/facility/FacilityInspectionRecordRecord.java
backend/src/main/java/com/complex/platform/facility/FacilityInspectionScheduleJob.java
backend/src/main/java/com/complex/platform/facility/FacilityInspectionTaskRecord.java
backend/src/main/java/com/complex/platform/facility/FacilityInspectionTemplateRecord.java
backend/src/main/java/com/complex/platform/facility/dto/FacilityEquipmentArchiveRequest.java
backend/src/main/java/com/complex/platform/facility/dto/FacilityEquipmentDeleteApprovalRequest.java
backend/src/main/java/com/complex/platform/facility/dto/FacilityEquipmentDeleteDecision.java
backend/src/main/java/com/complex/platform/facility/dto/FacilityEquipmentDeleteRequest.java
backend/src/main/java/com/complex/platform/facility/dto/FacilityEquipmentRequest.java
backend/src/main/java/com/complex/platform/facility/dto/FacilityEquipmentStatusRequest.java
backend/src/main/java/com/complex/platform/facility/dto/FacilityInspectionExceptionReportRequest.java
backend/src/main/java/com/complex/platform/facility/dto/FacilityInspectionRecheckConfirmRequest.java
backend/src/main/java/com/complex/platform/facility/dto/FacilityInspectionRecordSubmitRequest.java
backend/src/main/java/com/complex/platform/facility/dto/FacilityInspectionTaskRequest.java
<!-- FILES-END -->

## M273 — 楼宇机电设备台账、维保、告警与楼宇巡检／后台实现／第2段

- 聚合维度：`楼宇机电设备台账、维保、告警与楼宇巡检／后台实现`
- 文件数：1
- 有效内容行数：16
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/facility/dto/FacilityInspectionTemplateRequest.java
<!-- FILES-END -->

## M274 — 楼宇机电设备台账、维保、告警与楼宇巡检／数据库迁移／第1段

- 聚合维度：`楼宇机电设备台账、维保、告警与楼宇巡检／数据库迁移`
- 文件数：2
- 有效内容行数：126
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V165__facility_equipment.sql
backend/src/main/resources/db/migration/V167__facility_inspection.sql
<!-- FILES-END -->

## M275 — 楼宇机电设备台账、维保、告警与楼宇巡检／后台验证／第1段

- 聚合维度：`楼宇机电设备台账、维保、告警与楼宇巡检／后台验证`
- 文件数：5
- 有效内容行数：726
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/facility/FacilityControllerTests.java
backend/src/test/java/com/complex/platform/facility/FacilityEquipmentServiceTests.java
backend/src/test/java/com/complex/platform/facility/FacilityInspectionControllerTests.java
backend/src/test/java/com/complex/platform/facility/FacilityInspectionScheduleJobTests.java
backend/src/test/java/com/complex/platform/facility/FacilityInspectionServiceTests.java
<!-- FILES-END -->

## M276 — 楼宇机电设备台账、维保、告警与楼宇巡检／网页与验证／第1段

- 聚合维度：`楼宇机电设备台账、维保、告警与楼宇巡检／网页与验证`
- 文件数：4
- 有效内容行数：1221
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/FacilityWorkbenchPage.tsx
apps/web/src/admin/pages/FacilityWorkbenchPage.degraded.test.tsx
apps/web/src/admin/pages/VendorInspectionsPage.tsx
apps/web/src/admin/pages/VendorInspectionsPage.test.tsx
<!-- FILES-END -->

## M277 — 楼宇机电设备台账、维保、告警与楼宇巡检／文档与接口定义／第1段

- 聚合维度：`楼宇机电设备台账、维保、告警与楼宇巡检／文档与接口定义`
- 文件数：1
- 有效内容行数：229
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/building-operations-prd-v1.0.md
<!-- FILES-END -->

## M278 — 能耗计量、抄表复核与能源费归集／后台实现／第1段

- 聚合维度：`能耗计量、抄表复核与能源费归集／后台实现`
- 文件数：13
- 有效内容行数：960
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/energy/EnergyController.java
backend/src/main/java/com/complex/platform/energy/EnergyRepository.java
backend/src/main/java/com/complex/platform/energy/EnergyService.java
backend/src/main/java/com/complex/platform/energy/JdbcEnergyRepository.java
backend/src/main/java/com/complex/platform/energy/EnergyAnomalyThresholdRecord.java
backend/src/main/java/com/complex/platform/energy/EnergyFeeSettlementRecord.java
backend/src/main/java/com/complex/platform/energy/EnergyMeterRecord.java
backend/src/main/java/com/complex/platform/energy/EnergyMeterDifferenceRecord.java
backend/src/main/java/com/complex/platform/energy/EnergyMeterReadingRecord.java
backend/src/main/java/com/complex/platform/energy/dto/EnergyMeterRequest.java
backend/src/main/java/com/complex/platform/energy/dto/EnergyReadingRecordRequest.java
backend/src/main/java/com/complex/platform/energy/dto/EnergyReadingReviewRequest.java
backend/src/main/java/com/complex/platform/energy/dto/EnergySettleRequest.java
<!-- FILES-END -->

## M279 — 能耗计量、抄表复核与能源费归集／数据库迁移／第1段

- 聚合维度：`能耗计量、抄表复核与能源费归集／数据库迁移`
- 文件数：1
- 有效内容行数：99
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V168__energy_metering.sql
<!-- FILES-END -->

## M280 — 能耗计量、抄表复核与能源费归集／后台验证／第1段

- 聚合维度：`能耗计量、抄表复核与能源费归集／后台验证`
- 文件数：1
- 有效内容行数：343
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/energy/EnergyServiceTests.java
<!-- FILES-END -->

## M281 — 能耗计量、抄表复核与能源费归集／网页与验证／第1段

- 聚合维度：`能耗计量、抄表复核与能源费归集／网页与验证`
- 文件数：6
- 有效内容行数：859
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/EnergyAnalyticsPage.tsx
apps/web/src/admin/pages/EnergyAnalyticsPage.kpi.test.tsx
apps/web/src/admin/pages/EnergyAnalyticsPage.metricdetail.test.tsx
apps/web/src/admin/pages/EnergyAnalyticsPage.robustness.test.tsx
apps/web/src/admin/pages/EnergyWorkbenchPage.tsx
apps/web/src/admin/pages/EnergyWorkbenchPage.test.tsx
<!-- FILES-END -->

## M282 — 物业费按面积计费、商户综合账单、催收与争议／后台实现／第1段

- 聚合维度：`物业费按面积计费、商户综合账单、催收与争议／后台实现`
- 文件数：24
- 有效内容行数：1957
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/finance/PropertyFeeBillRecord.java
backend/src/main/java/com/complex/platform/finance/PropertyFeeRateRecord.java
backend/src/main/java/com/complex/platform/finance/dto/PropertyFeeBillRequest.java
backend/src/main/java/com/complex/platform/finance/dto/PropertyFeeRateRequest.java
backend/src/main/java/com/complex/platform/merchantbilling/MerchantBillRecord.java
backend/src/main/java/com/complex/platform/merchantbilling/MerchantBillDisputeRecord.java
backend/src/main/java/com/complex/platform/merchantbilling/MerchantBillDunningRecord.java
backend/src/main/java/com/complex/platform/merchantbilling/MerchantBillLineRecord.java
backend/src/main/java/com/complex/platform/merchantbilling/JdbcMerchantBillingRepository.java
backend/src/main/java/com/complex/platform/merchantbilling/MerchantBillingController.java
backend/src/main/java/com/complex/platform/merchantbilling/MerchantBillingRepository.java
backend/src/main/java/com/complex/platform/merchantbilling/MerchantBillingService.java
backend/src/main/java/com/complex/platform/merchantbilling/JdbcMerchantFeeNotificationRecipientRepository.java
backend/src/main/java/com/complex/platform/merchantbilling/MerchantFeeNotificationRecipientController.java
backend/src/main/java/com/complex/platform/merchantbilling/MerchantFeeNotificationRecipientRecord.java
backend/src/main/java/com/complex/platform/merchantbilling/MerchantFeeNotificationRecipientRepository.java
backend/src/main/java/com/complex/platform/merchantbilling/MerchantFeeNotificationRecipientService.java
backend/src/main/java/com/complex/platform/merchantbilling/MerchantFeeNotificationRecipientResolver.java
backend/src/main/java/com/complex/platform/merchantbilling/dto/MerchantBillConfirmRequest.java
backend/src/main/java/com/complex/platform/merchantbilling/dto/MerchantBillCreateRequest.java
backend/src/main/java/com/complex/platform/merchantbilling/dto/MerchantBillDisputeRequest.java
backend/src/main/java/com/complex/platform/merchantbilling/dto/MerchantBillDunningRequest.java
backend/src/main/java/com/complex/platform/merchantbilling/dto/MerchantBillLineRequest.java
backend/src/main/java/com/complex/platform/merchantbilling/dto/MerchantFeeNotificationRecipientRequest.java
<!-- FILES-END -->

## M283 — 物业费按面积计费、商户综合账单、催收与争议／数据库迁移／第1段

- 聚合维度：`物业费按面积计费、商户综合账单、催收与争议／数据库迁移`
- 文件数：3
- 有效内容行数：213
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V026__p1_merchant_billing_portal.sql
backend/src/main/resources/db/migration/V150__property_fee_fixed_billing.sql
backend/src/main/resources/db/migration/V182__merchant_fee_notification_recipient.sql
<!-- FILES-END -->

## M284 — 物业费按面积计费、商户综合账单、催收与争议／后台验证／第1段

- 聚合维度：`物业费按面积计费、商户综合账单、催收与争议／后台验证`
- 文件数：7
- 有效内容行数：2317
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/merchantbilling/MerchantBillingControllerTests.java
backend/src/test/java/com/complex/platform/merchantbilling/MerchantBillingJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/merchantbilling/MerchantBillingServiceTests.java
backend/src/test/java/com/complex/platform/merchantbilling/MerchantFeeNotificationIntegrationTest.java
backend/src/test/java/com/complex/platform/merchantbilling/MerchantFeeNotificationRecipientControllerTests.java
backend/src/test/java/com/complex/platform/merchantbilling/MerchantFeeNotificationRecipientResolverTests.java
backend/src/test/java/com/complex/platform/merchantbilling/MerchantFeeNotificationRecipientServiceTests.java
<!-- FILES-END -->

## M285 — 物业费按面积计费、商户综合账单、催收与争议／网页与验证／第1段

- 聚合维度：`物业费按面积计费、商户综合账单、催收与争议／网页与验证`
- 文件数：6
- 有效内容行数：1082
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/FinancePropertyFeePage.tsx
apps/web/src/admin/pages/FinancePropertyFeePage.robustness.test.tsx
apps/web/src/admin/pages/MerchantZoneBillsPage.tsx
apps/web/src/admin/pages/MerchantZoneBillsPage.test.tsx
apps/web/src/admin/pages/MerchantZoneFeeRecipientsPage.tsx
apps/web/src/admin/pages/MerchantZoneFeeRecipientsPage.test.tsx
<!-- FILES-END -->

## M286 — 物业费按面积计费、商户综合账单、催收与争议／端到端验证／第1段

- 聚合维度：`物业费按面积计费、商户综合账单、催收与争议／端到端验证`
- 文件数：1
- 有效内容行数：47
- 标记：NORMAL

<!-- FILES-BEGIN -->
tests/e2e/property-fee-evidence.spec.ts
<!-- FILES-END -->

## M287 — 物业费按面积计费、商户综合账单、催收与争议／文档与接口定义／第1段

- 聚合维度：`物业费按面积计费、商户综合账单、催收与争议／文档与接口定义`
- 文件数：1
- 有效内容行数：214
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/p1-merchant-billing-portal-api-contract-v0.1.md
<!-- FILES-END -->

## M288 — 排班、考勤打卡与请假／后台实现／第1段

- 聚合维度：`排班、考勤打卡与请假／后台实现`
- 文件数：10
- 有效内容行数：165
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/access/staff/StaffAttendanceRecord.java
backend/src/main/java/com/complex/platform/access/staff/StaffAttendanceDayRecord.java
backend/src/main/java/com/complex/platform/access/staff/StaffLeaveRequestRecord.java
backend/src/main/java/com/complex/platform/access/staff/StaffShiftRecord.java
backend/src/main/java/com/complex/platform/access/staff/dto/StaffAttendancePunchRequest.java
backend/src/main/java/com/complex/platform/access/staff/dto/StaffLeaveRequest.java
backend/src/main/java/com/complex/platform/access/staff/dto/StaffLeaveReviewRequest.java
backend/src/main/java/com/complex/platform/access/staff/dto/StaffShiftBatchRequest.java
backend/src/main/java/com/complex/platform/access/staff/dto/StaffShiftBatchResult.java
backend/src/main/java/com/complex/platform/access/staff/dto/StaffShiftRequest.java
<!-- FILES-END -->

## M289 — 排班、考勤打卡与请假／数据库迁移／第1段

- 聚合维度：`排班、考勤打卡与请假／数据库迁移`
- 文件数：3
- 有效内容行数：134
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V210__staff_shift_attendance.sql
backend/src/main/resources/db/migration/V211__staff_payroll_leave.sql
backend/src/main/resources/db/migration/V246__payroll_leave_deduction.sql
<!-- FILES-END -->

## M290 — 排班、考勤打卡与请假／网页与验证／第1段

- 聚合维度：`排班、考勤打卡与请假／网页与验证`
- 文件数：4
- 有效内容行数：661
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/StaffShiftAttendancePage.tsx
apps/web/src/admin/pages/StaffShiftAttendancePage.batchshift.test.tsx
apps/web/src/admin/pages/StaffShiftAttendancePage.metricdetail.test.tsx
apps/web/src/admin/pages/StaffShiftAttendancePage.test.tsx
<!-- FILES-END -->

## M291 — 薪资月结、税档与提成发放治理／后台实现／第1段

- 聚合维度：`薪资月结、税档与提成发放治理／后台实现`
- 文件数：11
- 有效内容行数：163
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/access/staff/EmployeeNoticeRecord.java
backend/src/main/java/com/complex/platform/access/staff/PayrollTaxBracketRecord.java
backend/src/main/java/com/complex/platform/access/staff/StaffCommissionLineRecord.java
backend/src/main/java/com/complex/platform/access/staff/StaffPayrollStatementRecord.java
backend/src/main/java/com/complex/platform/access/staff/dto/PayrollTaxBracketRequest.java
backend/src/main/java/com/complex/platform/access/staff/dto/StaffCommissionRequest.java
backend/src/main/java/com/complex/platform/access/staff/dto/StaffPayrollAdjustmentRequest.java
backend/src/main/java/com/complex/platform/access/staff/dto/StaffPayrollBaseRequest.java
backend/src/main/java/com/complex/platform/access/staff/dto/StaffPayrollCloseRequest.java
backend/src/main/java/com/complex/platform/access/staff/dto/StaffPayrollPayRequest.java
backend/src/main/java/com/complex/platform/access/staff/dto/StaffSalesPositionRequest.java
<!-- FILES-END -->

## M292 — 薪资月结、税档与提成发放治理／数据库迁移／第1段

- 聚合维度：`薪资月结、税档与提成发放治理／数据库迁移`
- 文件数：5
- 有效内容行数：131
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V217__payroll_tax_brackets.sql
backend/src/main/resources/db/migration/V238__payroll_statement_paid_state.sql
backend/src/main/resources/db/migration/V239__payroll_manual_dedup.sql
backend/src/main/resources/db/migration/V240__payroll_manage_requires_approval.sql
backend/src/main/resources/db/migration/V241__tax_bracket_no_overlap.sql
<!-- FILES-END -->

## M293 — 薪资月结、税档与提成发放治理／网页与验证／第1段

- 聚合维度：`薪资月结、税档与提成发放治理／网页与验证`
- 文件数：5
- 有效内容行数：629
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/PayrollTaxBracketsPage.tsx
apps/web/src/admin/pages/PayrollTaxBracketsPage.test.tsx
apps/web/src/admin/pages/StaffPayrollLeavePage.tsx
apps/web/src/admin/pages/StaffPayrollLeavePage.metricdetail.test.tsx
apps/web/src/admin/pages/StaffPayrollLeavePage.robustness.test.tsx
<!-- FILES-END -->

## M294 — 开放接口：鉴权、签名、防重放、动态端点与开放应用状态机／后台实现／第1段

- 聚合维度：`开放接口：鉴权、签名、防重放、动态端点与开放应用状态机／后台实现`
- 文件数：9
- 有效内容行数：299
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/integration/OpenApiEventRecord.java
backend/src/main/java/com/complex/platform/integration/OpenAppCredentialRecord.java
backend/src/main/java/com/complex/platform/integration/OpenEndpointConfigRecord.java
backend/src/main/java/com/complex/platform/integration/OpenEventResultResponse.java
backend/src/main/java/com/complex/platform/integration/OpenPlatformController.java
backend/src/main/java/com/complex/platform/integration/OpenPlatformCrypto.java
backend/src/main/java/com/complex/platform/integration/OpenPlatformSecretResolver.java
backend/src/main/java/com/complex/platform/integration/OpenSignatureRequest.java
backend/src/main/java/com/complex/platform/integration/SecretResolver.java
<!-- FILES-END -->

## M295 — 开放接口：鉴权、签名、防重放、动态端点与开放应用状态机／数据库迁移／第1段

- 聚合维度：`开放接口：鉴权、签名、防重放、动态端点与开放应用状态机／数据库迁移`
- 文件数：2
- 有效内容行数：309
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V015__p0_open_platform_integration_adapter_boundary.sql
backend/src/main/resources/db/migration/V016__p0_open_platform_secret_refs_hardened.sql
<!-- FILES-END -->

## M296 — 开放接口：鉴权、签名、防重放、动态端点与开放应用状态机／后台验证／第1段

- 聚合维度：`开放接口：鉴权、签名、防重放、动态端点与开放应用状态机／后台验证`
- 文件数：1
- 有效内容行数：128
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/integration/OpenPlatformControllerTests.java
<!-- FILES-END -->

## M297 — 开放接口：鉴权、签名、防重放、动态端点与开放应用状态机／文档与接口定义／第1段

- 聚合维度：`开放接口：鉴权、签名、防重放、动态端点与开放应用状态机／文档与接口定义`
- 文件数：2
- 有效内容行数：1402
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/open-platform-security-auth-development-design.md
docs/superpowers/plans/2026-06-14-p0-open-platform-integration-adapter-boundary-slice.md
<!-- FILES-END -->

## M298 — 第三方能力开关、供应商适配器边界、脚手架与 mock 策略／后台实现／第1段

- 聚合维度：`第三方能力开关、供应商适配器边界、脚手架与 mock 策略／后台实现`
- 文件数：11
- 有效内容行数：857
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/integration/AdapterTestRecord.java
backend/src/main/java/com/complex/platform/integration/ProviderAdapterRecord.java
backend/src/main/java/com/complex/platform/integration/ProviderAdapterService.java
backend/src/main/java/com/complex/platform/integration/ProviderAdapterRequest.java
backend/src/main/java/com/complex/platform/integration/ProviderAdapterTestRequest.java
backend/src/main/java/com/complex/platform/integration/ProviderAdapterUpdateRequest.java
backend/src/main/java/com/complex/platform/integration/RobotTaskEventRecord.java
backend/src/main/java/com/complex/platform/integration/RobotTaskEventRequest.java
backend/src/main/java/com/complex/platform/integration/ThirdPartyScaffoldingService.java
backend/src/main/java/com/complex/platform/integration/VideoAccessApprovalRecord.java
backend/src/main/java/com/complex/platform/integration/VideoAnchorRecord.java
<!-- FILES-END -->

## M299 — 第三方能力开关、供应商适配器边界、脚手架与 mock 策略／数据库迁移／第1段

- 聚合维度：`第三方能力开关、供应商适配器边界、脚手架与 mock 策略／数据库迁移`
- 文件数：3
- 有效内容行数：160
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V071__thirdparty_scaffolding.sql
backend/src/main/resources/db/migration/V177__sms_notification_scaffold.sql
backend/src/main/resources/db/migration/V190__integration_video_access_approval.sql
<!-- FILES-END -->

## M300 — 第三方能力开关、供应商适配器边界、脚手架与 mock 策略／网页与验证／第1段

- 聚合维度：`第三方能力开关、供应商适配器边界、脚手架与 mock 策略／网页与验证`
- 文件数：3
- 有效内容行数：376
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/IntegrationAdaptersPage.tsx
apps/web/src/admin/pages/IntegrationAdaptersPage.test.tsx
apps/web/src/admin/pages/IntegrationSwitchesPage.tsx
<!-- FILES-END -->

## M301 — 第三方能力开关、供应商适配器边界、脚手架与 mock 策略／文档与接口定义／第1段

- 聚合维度：`第三方能力开关、供应商适配器边界、脚手架与 mock 策略／文档与接口定义`
- 文件数：15
- 有效内容行数：2271
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/external-dependency-gates-v0.1.md
docs/integration-capability-switches-latest.json
docs/integration-capability-switches-v0.1.md
docs/p2-third-party-deep-integration-boundary-completion-audit-v0.1.md
docs/p2-third-party-deep-integration-boundary-contract-v0.1.md
docs/r1-pending-supplier-information-backfill-checklist-20260618.md
docs/r1-third-party-capability-switch-and-mock-strategy-20260618.md
docs/r5-spec-T14-thirdparty-scaffolding-dev-design-20260618.md
docs/t3-coding-entry-pack-v0.1.md
docs/t3-entry-assessment-and-coding-package-audit-v0.1.md
docs/t3-security-and-supplier-readiness-gate-v0.1.md
docs/t3-user-story-map-and-sprint-slices-v0.1.md
docs/t3b-supplier-sdk-mock-kit-v0.1.md
docs/third-party-access-layer-spec-v0.2.md
docs/third-party-business-access-product-prd-v0.1.md
<!-- FILES-END -->

## M302 — 第三方能力开关、供应商适配器边界、脚手架与 mock 策略／文档与接口定义／第2段

- 聚合维度：`第三方能力开关、供应商适配器边界、脚手架与 mock 策略／文档与接口定义`
- 文件数：5
- 有效内容行数：1740
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/third-party-integration-development-design.md
docs/third-party-supplier-integration-kit-v0.1.md
docs/supplier-mock/t3b-open-auth-request.example.json
docs/supplier-mock/t3b-provider-adapter-test.example.json
docs/supplier-mock/t3b-webhook-envelope.example.json
<!-- FILES-END -->

## M303 — 第三方能力开关、供应商适配器边界、脚手架与 mock 策略／检查及生成工具／第1段

- 聚合维度：`第三方能力开关、供应商适配器边界、脚手架与 mock 策略／检查及生成工具`
- 文件数：6
- 有效内容行数：872
- 标记：NORMAL

<!-- FILES-BEGIN -->
tools/generate-integration-capability-switches.mjs
tools/integration-capability-switches.mjs
tools/t3b-open-platform-signature-helper.mjs
tools/verify-external-dependency-gates.mjs
tools/verify-t3-coding-package.mjs
tools/verify-t3b-supplier-mock-kit.mjs
<!-- FILES-END -->

## M304 — 渠道订单回流、Webhook 幂等重试补偿与渠道对账差异／后台实现／第1段

- 聚合维度：`渠道订单回流、Webhook 幂等重试补偿与渠道对账差异／后台实现`
- 文件数：25
- 有效内容行数：1884
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/integration/ChannelReconciliationRequest.java
backend/src/main/java/com/complex/platform/integration/ChannelReconciliationResult.java
backend/src/main/java/com/complex/platform/integration/IntegrationChannelStoreMapRecord.java
backend/src/main/java/com/complex/platform/integration/IntegrationChannelStoreMapRequest.java
backend/src/main/java/com/complex/platform/integration/IntegrationChannelTicketTypeMapRecord.java
backend/src/main/java/com/complex/platform/integration/IntegrationChannelTicketTypeMapRequest.java
backend/src/main/java/com/complex/platform/integration/IntegrationOrderSyncRecord.java
backend/src/main/java/com/complex/platform/integration/IntegrationOrderSyncService.java
backend/src/main/java/com/complex/platform/integration/IntegrationOrderSyncRequest.java
backend/src/main/java/com/complex/platform/integration/IntegrationPollingCheckRecord.java
backend/src/main/java/com/complex/platform/integration/IntegrationPollingCheckRequest.java
backend/src/main/java/com/complex/platform/integration/IntegrationReconciliationService.java
backend/src/main/java/com/complex/platform/integration/IntegrationTaskRecord.java
backend/src/main/java/com/complex/platform/integration/IntegrationTaskRequest.java
backend/src/main/java/com/complex/platform/integration/IntegrationTaskResolveRequest.java
backend/src/main/java/com/complex/platform/integration/ReconciliationBatchRecord.java
backend/src/main/java/com/complex/platform/integration/ReconciliationBatchRequest.java
backend/src/main/java/com/complex/platform/integration/ReconciliationDifferenceRecord.java
backend/src/main/java/com/complex/platform/integration/ReconciliationDifferenceRequest.java
backend/src/main/java/com/complex/platform/integration/ReconciliationDifferenceResolveRequest.java
backend/src/main/java/com/complex/platform/integration/TicketChannelCallbackEnqueuer.java
backend/src/main/java/com/complex/platform/integration/TicketingChannelTicketIssueAdapter.java
backend/src/main/java/com/complex/platform/integration/WebhookAcceptedResponse.java
backend/src/main/java/com/complex/platform/ticketing/PartnerVoucherController.java
backend/src/main/java/com/complex/platform/ticketing/PartnerVoucherService.java
<!-- FILES-END -->

## M305 — 渠道订单回流、Webhook 幂等重试补偿与渠道对账差异／后台实现／第2段

- 聚合维度：`渠道订单回流、Webhook 幂等重试补偿与渠道对账差异／后台实现`
- 文件数：2
- 有效内容行数：25
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/ticketing/dto/PartnerVoucherConfigRequest.java
backend/src/main/java/com/complex/platform/ticketing/dto/PartnerVoucherRedemptionImportRequest.java
<!-- FILES-END -->

## M306 — 渠道订单回流、Webhook 幂等重试补偿与渠道对账差异／数据库迁移／第1段

- 聚合维度：`渠道订单回流、Webhook 幂等重试补偿与渠道对账差异／数据库迁移`
- 文件数：6
- 有效内容行数：454
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V030__p1_integration_retry_compensation_reconciliation.sql
backend/src/main/resources/db/migration/V038__security_credential_hashing_and_webhook_payload_hash.sql
backend/src/main/resources/db/migration/V109__integration_channel_semantics.sql
backend/src/main/resources/db/migration/V282__partner_voucher.sql
backend/src/main/resources/db/migration/V334__integration_order_sync_merchant_attribution.sql
backend/src/main/resources/db/migration/V382__thirdparty_pass_ticket_channel.sql
<!-- FILES-END -->

## M307 — 渠道订单回流、Webhook 幂等重试补偿与渠道对账差异／后台验证／第1段

- 聚合维度：`渠道订单回流、Webhook 幂等重试补偿与渠道对账差异／后台验证`
- 文件数：1
- 有效内容行数：313
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/settlement/SettlementChannelFeeFixedAmountTests.java
<!-- FILES-END -->

## M308 — 渠道订单回流、Webhook 幂等重试补偿与渠道对账差异／网页与验证／第1段

- 聚合维度：`渠道订单回流、Webhook 幂等重试补偿与渠道对账差异／网页与验证`
- 文件数：4
- 有效内容行数：826
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/IntegrationChannelMapsPage.tsx
apps/web/src/admin/pages/IntegrationChannelMapsPage.test.tsx
apps/web/src/admin/pages/IntegrationEventsPage.tsx
apps/web/src/admin/pages/IntegrationEventsPage.kpi.test.tsx
<!-- FILES-END -->

## M309 — 渠道订单回流、Webhook 幂等重试补偿与渠道对账差异／文档与接口定义／第1段

- 聚合维度：`渠道订单回流、Webhook 幂等重试补偿与渠道对账差异／文档与接口定义`
- 文件数：1
- 有效内容行数：220
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/p1-integration-retry-compensation-reconciliation-api-contract-v0.1.md
<!-- FILES-END -->

## M310 — 短信/通知开关、提醒窗口与重要事项通知／后台实现／第1段

- 聚合维度：`短信/通知开关、提醒窗口与重要事项通知／后台实现`
- 文件数：10
- 有效内容行数：535
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/analytics/NotificationWindowRequest.java
backend/src/main/java/com/complex/platform/notification/JdbcNotificationRepository.java
backend/src/main/java/com/complex/platform/notification/NotificationService.java
backend/src/main/java/com/complex/platform/notification/NotificationSendLogRecord.java
backend/src/main/java/com/complex/platform/notification/NotificationSwitchDecision.java
backend/src/main/java/com/complex/platform/notification/NotificationSwitchResolver.java
backend/src/main/java/com/complex/platform/notification/ScaffoldSmsSender.java
backend/src/main/java/com/complex/platform/notification/SmsMessage.java
backend/src/main/java/com/complex/platform/notification/SmsSendResult.java
backend/src/main/java/com/complex/platform/notification/SmsSender.java
<!-- FILES-END -->

## M311 — 短信/通知开关、提醒窗口与重要事项通知／数据库迁移／第1段

- 聚合维度：`短信/通知开关、提醒窗口与重要事项通知／数据库迁移`
- 文件数：1
- 有效内容行数：37
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V219__notification_windows.sql
<!-- FILES-END -->

## M312 — 短信/通知开关、提醒窗口与重要事项通知／后台验证／第1段

- 聚合维度：`短信/通知开关、提醒窗口与重要事项通知／后台验证`
- 文件数：1
- 有效内容行数：169
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/notification/NotificationServiceTest.java
<!-- FILES-END -->

## M313 — 短信/通知开关、提醒窗口与重要事项通知／网页与验证／第1段

- 聚合维度：`短信/通知开关、提醒窗口与重要事项通知／网页与验证`
- 文件数：3
- 有效内容行数：193
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/NotificationWindowsPage.tsx
apps/web/src/admin/pages/NotificationWindowsPage.test.tsx
apps/web/src/admin/pages/SmsNotificationSwitchesPage.tsx
<!-- FILES-END -->

## M314 — 短信/通知开关、提醒窗口与重要事项通知／文档与接口定义／第1段

- 聚合维度：`短信/通知开关、提醒窗口与重要事项通知／文档与接口定义`
- 文件数：1
- 有效内容行数：173
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/third-party-sms-notification-and-supplier-field-mapping-placeholders-v0.1.md
<!-- FILES-END -->

## M315 — 第三方接入核心与其余／后台实现／第1段

- 聚合维度：`第三方接入核心与其余／后台实现`
- 文件数：9
- 有效内容行数：2623
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/integration/ChannelTicketIssuePort.java
backend/src/main/java/com/complex/platform/integration/IntegrationController.java
backend/src/main/java/com/complex/platform/integration/IntegrationRepository.java
backend/src/main/java/com/complex/platform/integration/IntegrationService.java
backend/src/main/java/com/complex/platform/integration/JdbcIntegrationRepository.java
backend/src/main/java/com/complex/platform/integration/IntegrationLedgerRecord.java
backend/src/main/java/com/complex/platform/integration/JdbcIntegrationRowMapper.java
backend/src/main/java/com/complex/platform/integration/IntegrationValidation.java
backend/src/main/java/com/complex/platform/integration/StatusChangeRequest.java
<!-- FILES-END -->

## M316 — 第三方接入核心与其余／后台验证／第1段

- 聚合维度：`第三方接入核心与其余／后台验证`
- 文件数：3
- 有效内容行数：2849
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/integration/IntegrationControllerTests.java
backend/src/test/java/com/complex/platform/integration/IntegrationJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/integration/IntegrationServiceTests.java
<!-- FILES-END -->

## M317 — 第三方接入核心与其余／后台验证／第2段

- 聚合维度：`第三方接入核心与其余／后台验证`
- 文件数：1
- 有效内容行数：250
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/integration/ThirdPartyPassTicketEndToEndJdbcTests.java
<!-- FILES-END -->

## M318 — 收入归属、业态主体分账与通票分账瀑布／后台实现／第1段

- 聚合维度：`收入归属、业态主体分账与通票分账瀑布／后台实现`
- 文件数：6
- 有效内容行数：1204
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/analytics/RevenueAttributionRuleRequest.java
backend/src/main/java/com/complex/platform/settlement/ComprehensiveServiceFee.java
backend/src/main/java/com/complex/platform/settlement/RevenueStreamAttributionRecord.java
backend/src/main/java/com/complex/platform/settlement/RevenueStreamAttributionRepository.java
backend/src/main/java/com/complex/platform/settlement/SettlementBillFormatShareRecord.java
backend/src/main/java/com/complex/platform/settlement/TicketDistributionSettlementService.java
<!-- FILES-END -->

## M319 — 收入归属、业态主体分账与通票分账瀑布／数据库迁移／第1段

- 聚合维度：`收入归属、业态主体分账与通票分账瀑布／数据库迁移`
- 文件数：17
- 有效内容行数：1007
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V221__merchant_revenue_attribution_rules.sql
backend/src/main/resources/db/migration/V223__settlement_distribution_configs_batch2.sql
backend/src/main/resources/db/migration/V228__entitlement_exclusive_merchant_batch3.sql
backend/src/main/resources/db/migration/V268__settlement_comprehensive_service_fee_and_combo_discount.sql
backend/src/main/resources/db/migration/V319__revenue_stream_attribution.sql
backend/src/main/resources/db/migration/V328__settlement_comprehensive_service_fee_fixed_amount.sql
backend/src/main/resources/db/migration/V336__settlement_comprehensive_service_fee_monthly_cap.sql
backend/src/main/resources/db/migration/V337__settlement_transaction_service_fee.sql
backend/src/main/resources/db/migration/V347__revenue_stream_attribution_term_rename.sql
backend/src/main/resources/db/migration/V355__revenue_stream_attribution_and_ledger_entity.sql
backend/src/main/resources/db/migration/V365__settlement_comprehensive_service_fee_ticket_type_dimension.sql
backend/src/main/resources/db/migration/V366__settlement_bill_format_shares.sql
backend/src/main/resources/db/migration/V370__settlement_merge_retention_into_service_fee.sql
backend/src/main/resources/db/migration/V372__settlement_gift_cost_layer.sql
backend/src/main/resources/db/migration/V373__pass_gift_cost_configs.sql
backend/src/main/resources/db/migration/V374__ticket_distribution_merchant_rate_rule_type.sql
backend/src/main/resources/db/migration/V375__settlement_pool_undistributed_and_unverified_attribution.sql
<!-- FILES-END -->

## M320 — 收入归属、业态主体分账与通票分账瀑布／后台验证／第1段

- 聚合维度：`收入归属、业态主体分账与通票分账瀑布／后台验证`
- 文件数：7
- 有效内容行数：2369
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/settlement/RevenueStreamAttributionJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/settlement/SettlementEntityAttributionS1Tests.java
backend/src/test/java/com/complex/platform/settlement/SettlementFormatShareS11Tests.java
backend/src/test/java/com/complex/platform/settlement/SettlementGiftCostS6Tests.java
backend/src/test/java/com/complex/platform/settlement/SettlementMergeRetentionS4Tests.java
backend/src/test/java/com/complex/platform/settlement/SettlementServiceFeeConservationTests.java
backend/src/test/java/com/complex/platform/settlement/SettlementServiceFeeDimensionS2Tests.java
<!-- FILES-END -->

## M321 — 收入归属、业态主体分账与通票分账瀑布／文档与接口定义／第1段

- 聚合维度：`收入归属、业态主体分账与通票分账瀑布／文档与接口定义`
- 文件数：1
- 有效内容行数：147
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/prd-audit/HTML-PASS-SETTLEMENT-WATERFALL-2026-07-28.md
<!-- FILES-END -->

## M322 — 结算配置中心、结算类型与配置版本化／后台实现／第1段

- 聚合维度：`结算配置中心、结算类型与配置版本化／后台实现`
- 文件数：6
- 有效内容行数：1962
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/settlement/JdbcCommissionPointsConfigRepository.java
backend/src/main/java/com/complex/platform/settlement/SettlementConfigController.java
backend/src/main/java/com/complex/platform/settlement/SettlementConfigService.java
backend/src/main/java/com/complex/platform/settlement/JdbcSettlementConfigWriteRepository.java
backend/src/main/java/com/complex/platform/settlement/SettlementConfigWriteService.java
backend/src/main/java/com/complex/platform/settlement/SettlementConfigWriteRequests.java
<!-- FILES-END -->

## M323 — 结算配置中心、结算类型与配置版本化／数据库迁移／第1段

- 聚合维度：`结算配置中心、结算类型与配置版本化／数据库迁移`
- 文件数：6
- 有效内容行数：198
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V326__settlement_config_center_batch0.sql
backend/src/main/resources/db/migration/V327__settlement_channel_fee_configs.sql
backend/src/main/resources/db/migration/V329__settlement_config_draft_coexistence.sql
backend/src/main/resources/db/migration/V331__settlement_combo_discount_batch1.sql
backend/src/main/resources/db/migration/V335__settlement_bill_combo_discount_signed.sql
backend/src/main/resources/db/migration/V381__channel_fee_fixed_amount_mode.sql
<!-- FILES-END -->

## M324 — 结算配置中心、结算类型与配置版本化／后台验证／第1段

- 聚合维度：`结算配置中心、结算类型与配置版本化／后台验证`
- 文件数：3
- 有效内容行数：764
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/settlement/SettlementConfigBatch0JdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/settlement/SettlementConfigControllerTests.java
backend/src/test/java/com/complex/platform/settlement/SettlementConfigWriteJdbcIntegrationTests.java
<!-- FILES-END -->

## M325 — 结算配置中心、结算类型与配置版本化／网页与验证／第1段

- 聚合维度：`结算配置中心、结算类型与配置版本化／网页与验证`
- 文件数：2
- 有效内容行数：1061
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/SettlementConfigPage.tsx
apps/web/src/admin/pages/SettlementConfigPage.test.tsx
<!-- FILES-END -->

## M326 — 结算配置中心、结算类型与配置版本化／文档与接口定义／第1段

- 聚合维度：`结算配置中心、结算类型与配置版本化／文档与接口定义`
- 文件数：1
- 有效内容行数：1163
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/settlement-rebuild-prd-v1.0.md
<!-- FILES-END -->

## M327 — 结算单生成、月结作业、过期、冲正、复核与发票／后台实现／第1段

- 聚合维度：`结算单生成、月结作业、过期、冲正、复核与发票／后台实现`
- 文件数：14
- 有效内容行数：2927
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/settlement/SettlementController.java
backend/src/main/java/com/complex/platform/settlement/SettlementAmounts.java
backend/src/main/java/com/complex/platform/settlement/JdbcSettlementBillRepository.java
backend/src/main/java/com/complex/platform/settlement/SettlementBillRecord.java
backend/src/main/java/com/complex/platform/settlement/SettlementExpiryJob.java
backend/src/main/java/com/complex/platform/settlement/JdbcSettlementInvoiceRepository.java
backend/src/main/java/com/complex/platform/settlement/SettlementInvoiceController.java
backend/src/main/java/com/complex/platform/settlement/SettlementInvoiceService.java
backend/src/main/java/com/complex/platform/settlement/SettlementInvoiceItemRecord.java
backend/src/main/java/com/complex/platform/settlement/SettlementItemRecord.java
backend/src/main/java/com/complex/platform/settlement/SettlementMonthlyJob.java
backend/src/main/java/com/complex/platform/settlement/SettlementPostingService.java
backend/src/main/java/com/complex/platform/settlement/SettlementQueryService.java
backend/src/main/java/com/complex/platform/settlement/SettlementReconciliationJob.java
<!-- FILES-END -->

## M328 — 结算单生成、月结作业、过期、冲正、复核与发票／后台实现／第2段

- 聚合维度：`结算单生成、月结作业、过期、冲正、复核与发票／后台实现`
- 文件数：4
- 有效内容行数：810
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/settlement/SettlementReversedConsumer.java
backend/src/main/java/com/complex/platform/settlement/SettlementSettledConsumer.java
backend/src/main/java/com/complex/platform/settlement/JdbcSettlementTransactionRepository.java
backend/src/main/java/com/complex/platform/settlement/SettlementTransactionRecord.java
<!-- FILES-END -->

## M329 — 结算单生成、月结作业、过期、冲正、复核与发票／数据库迁移／第1段

- 聚合维度：`结算单生成、月结作业、过期、冲正、复核与发票／数据库迁移`
- 文件数：10
- 有效内容行数：474
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V222__settlement_transactions_batch1.sql
backend/src/main/resources/db/migration/V224__settlement_bills_batch2.sql
backend/src/main/resources/db/migration/V245__settlement_view_permission.sql
backend/src/main/resources/db/migration/V323__settlement_immutable_ledger.sql
backend/src/main/resources/db/migration/V330__settlement_bill_post_permission.sql
backend/src/main/resources/db/migration/V343__settlement_bill_rounding_residual.sql
backend/src/main/resources/db/migration/V357__settlement_special_approval_term.sql
backend/src/main/resources/db/migration/V362__settlement_invoice_list.sql
backend/src/main/resources/db/migration/V367__settlement_transaction_parent_ticket.sql
backend/src/main/resources/db/migration/V383__settlement_rounding_residual_entity.sql
<!-- FILES-END -->

## M330 — 结算单生成、月结作业、过期、冲正、复核与发票／网页与验证／第1段

- 聚合维度：`结算单生成、月结作业、过期、冲正、复核与发票／网页与验证`
- 文件数：5
- 有效内容行数：1015
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/SettlementInvoicePage.tsx
apps/web/src/admin/pages/SettlementInvoicePage.test.tsx
apps/web/src/admin/pages/SettlementReviewPage.tsx
apps/web/src/admin/pages/SettlementReviewPage.metricdetail.test.tsx
apps/web/src/admin/pages/SettlementReviewPage.specialapproval.test.tsx
<!-- FILES-END -->

## M331 — 财务核心（FinanceService/控制器/仓储）与应收应付台账、日结、三方对账／后台实现／第1段

- 聚合维度：`财务核心（FinanceService/控制器/仓储）与应收应付台账、日结、三方对账／后台实现`
- 文件数：10
- 有效内容行数：785
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/finance/ArApAdjustmentRecord.java
backend/src/main/java/com/complex/platform/finance/ArApItemRecord.java
backend/src/main/java/com/complex/platform/finance/ArApItemSourceRecord.java
backend/src/main/java/com/complex/platform/finance/ArApLedgerExportRecord.java
backend/src/main/java/com/complex/platform/finance/ArApPaymentRecord.java
backend/src/main/java/com/complex/platform/finance/ArApPaymentResponse.java
backend/src/main/java/com/complex/platform/finance/DailyCloseRecord.java
backend/src/main/java/com/complex/platform/finance/DailyCloseSummaryRecord.java
backend/src/main/java/com/complex/platform/finance/FinanceController.java
backend/src/main/java/com/complex/platform/finance/FinanceRepository.java
<!-- FILES-END -->

## M332 — 财务核心（FinanceService/控制器/仓储）与应收应付台账、日结、三方对账／后台实现／第2段

- 聚合维度：`财务核心（FinanceService/控制器/仓储）与应收应付台账、日结、三方对账／后台实现`
- 文件数：1
- 有效内容行数：2240
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/finance/FinanceService.java
<!-- FILES-END -->

## M333 — 财务核心（FinanceService/控制器/仓储）与应收应付台账、日结、三方对账／后台实现／第3段

- 聚合维度：`财务核心（FinanceService/控制器/仓储）与应收应付台账、日结、三方对账／后台实现`
- 文件数：22
- 有效内容行数：2214
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/finance/JdbcFinanceRepository.java
backend/src/main/java/com/complex/platform/finance/InventoryVarianceRecord.java
backend/src/main/java/com/complex/platform/finance/OfflinePaymentConfirmationRecord.java
backend/src/main/java/com/complex/platform/finance/ReconciliationBatchRecord.java
backend/src/main/java/com/complex/platform/finance/ReconciliationDifferenceRecord.java
backend/src/main/java/com/complex/platform/finance/VerificationReversedConsumer.java
backend/src/main/java/com/complex/platform/finance/package-info.java
backend/src/main/java/com/complex/platform/finance/dto/ArApAdjustmentDecisionRequest.java
backend/src/main/java/com/complex/platform/finance/dto/ArApAdjustmentRequest.java
backend/src/main/java/com/complex/platform/finance/dto/ArApItemRequest.java
backend/src/main/java/com/complex/platform/finance/dto/ArApPaymentRequest.java
backend/src/main/java/com/complex/platform/finance/dto/CloseReconciliationDifferenceRequest.java
backend/src/main/java/com/complex/platform/finance/dto/DailyCloseRequest.java
backend/src/main/java/com/complex/platform/finance/dto/InventoryVariancePostRequest.java
backend/src/main/java/com/complex/platform/finance/dto/OfflinePaymentConfirmRequest.java
backend/src/main/java/com/complex/platform/finance/dto/ReconciliationDifferenceRequest.java
backend/src/main/java/com/complex/platform/finance/gateway/DisabledPaymentGateway.java
backend/src/main/java/com/complex/platform/finance/gateway/PaymentGatewayPort.java
backend/src/main/java/com/complex/platform/finance/gateway/PaymentGatewayRequest.java
backend/src/main/java/com/complex/platform/finance/gateway/PaymentGatewayResult.java
backend/src/main/java/com/complex/platform/finance/gateway/PaymentGatewayRouter.java
backend/src/main/java/com/complex/platform/finance/gateway/ReservedPaymentGateway.java
<!-- FILES-END -->

## M334 — 财务核心（FinanceService/控制器/仓储）与应收应付台账、日结、三方对账／数据库迁移／第1段

- 聚合维度：`财务核心（FinanceService/控制器/仓储）与应收应付台账、日结、三方对账／数据库迁移`
- 文件数：14
- 有效内容行数：544
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V008__p0_finance_ar_ap_ledger.sql
backend/src/main/resources/db/migration/V031__p1_ticket_refund_line_ledger.sql
backend/src/main/resources/db/migration/V069__daily_close_summary_lock_backfill.sql
backend/src/main/resources/db/migration/V077__drop_orphan_finance_ar_ap_legacy_tables.sql
backend/src/main/resources/db/migration/V093__payment_gateway_reserved.sql
backend/src/main/resources/db/migration/V105__reconciliation_difference_hardening.sql
backend/src/main/resources/db/migration/V141__ar_ap_internal_accounting_direction.sql
backend/src/main/resources/db/migration/V142__finance_bar_cam_unique_constraints.sql
backend/src/main/resources/db/migration/V145__ar_ap_items_append_only.sql
backend/src/main/resources/db/migration/V149__finance_daily_closes_append_only.sql
backend/src/main/resources/db/migration/V173__finance_reconciliation_resolve_permission.sql
backend/src/main/resources/db/migration/V189__finance_reconciliation_difference_close.sql
backend/src/main/resources/db/migration/V196__finance_arap_export_permission.sql
backend/src/main/resources/db/migration/V205__widen_ar_ap_item_no.sql
<!-- FILES-END -->

## M335 — 财务核心（FinanceService/控制器/仓储）与应收应付台账、日结、三方对账／后台验证／第1段

- 聚合维度：`财务核心（FinanceService/控制器/仓储）与应收应付台账、日结、三方对账／后台验证`
- 文件数：2
- 有效内容行数：854
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/finance/FinanceControllerTests.java
backend/src/test/java/com/complex/platform/finance/FinanceJdbcIntegrationTests.java
<!-- FILES-END -->

## M336 — 财务核心（FinanceService/控制器/仓储）与应收应付台账、日结、三方对账／后台验证／第2段

- 聚合维度：`财务核心（FinanceService/控制器/仓储）与应收应付台账、日结、三方对账／后台验证`
- 文件数：5
- 有效内容行数：2776
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/finance/FinanceServiceTests.java
backend/src/test/java/com/complex/platform/finance/FundLedgerAppendOnlyJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/finance/VerificationReversedConsumerTests.java
backend/src/test/java/com/complex/platform/finance/gateway/PaymentGatewayRouterTests.java
backend/src/test/java/com/complex/platform/settlement/SettlementAmountsTests.java
<!-- FILES-END -->

## M337 — 财务核心（FinanceService/控制器/仓储）与应收应付台账、日结、三方对账／后台验证／第3段

- 聚合维度：`财务核心（FinanceService/控制器/仓储）与应收应付台账、日结、三方对账／后台验证`
- 文件数：10
- 有效内容行数：2927
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/settlement/SettlementBatch1P345Tests.java
backend/src/test/java/com/complex/platform/settlement/SettlementBatch2E2ETests.java
backend/src/test/java/com/complex/platform/settlement/SettlementBatch2EngineTests.java
backend/src/test/java/com/complex/platform/settlement/SettlementBatch2PostingTests.java
backend/src/test/java/com/complex/platform/settlement/SettlementBatch34ExclusiveE2ETests.java
backend/src/test/java/com/complex/platform/settlement/SettlementComboDiscountTests.java
backend/src/test/java/com/complex/platform/settlement/SettlementControllerPostBillTests.java
backend/src/test/java/com/complex/platform/settlement/SettlementFeePerTicketS3Tests.java
backend/src/test/java/com/complex/platform/settlement/SettlementInvoiceListJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/settlement/SettlementMonthlyJobPostingTests.java
<!-- FILES-END -->

## M338 — 财务核心（FinanceService/控制器/仓储）与应收应付台账、日结、三方对账／后台验证／第4段

- 聚合维度：`财务核心（FinanceService/控制器/仓储）与应收应付台账、日结、三方对账／后台验证`
- 文件数：4
- 有效内容行数：1503
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/settlement/SettlementOverPoolSpecialApprovalTests.java
backend/src/test/java/com/complex/platform/settlement/SettlementQueryServiceTests.java
backend/src/test/java/com/complex/platform/settlement/SettlementStateMachineJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/settlement/SettlementV268Tests.java
<!-- FILES-END -->

## M339 — 财务核心（FinanceService/控制器/仓储）与应收应付台账、日结、三方对账／网页与验证／第1段

- 聚合维度：`财务核心（FinanceService/控制器/仓储）与应收应付台账、日结、三方对账／网页与验证`
- 文件数：6
- 有效内容行数：894
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/FinanceArApLedgerPage.tsx
apps/web/src/admin/pages/FinanceArApLedgerPage.metricdetail.test.tsx
apps/web/src/admin/pages/FinanceArApLedgerPage.test.tsx
apps/web/src/admin/pages/ReconciliationPage.tsx
apps/web/src/admin/pages/ReconciliationPage.metricdetail.test.tsx
apps/web/src/admin/pages/ReconciliationPage.test.tsx
<!-- FILES-END -->

## M340 — 财务核心（FinanceService/控制器/仓储）与应收应付台账、日结、三方对账／文档与接口定义／第1段

- 聚合维度：`财务核心（FinanceService/控制器/仓储）与应收应付台账、日结、三方对账／文档与接口定义`
- 文件数：6
- 有效内容行数：2071
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/finance-ar-ap-ledger-development-design-v0.1.md
docs/finance-settlement-product-prd-v0.1.md
docs/operations-reconciliation-and-exception-handling-runbook-v0.1.md
docs/r5-aggregation-metrics-draft-20260618.md
docs/r5-spec-T7-daily-close-lock-dev-design-20260618.md
docs/superpowers/plans/2026-06-14-p0-finance-ar-ap-ledger-slice.md
<!-- FILES-END -->

## M341 — 商户支出台账、期间关账与持股分红引擎／后台实现／第1段

- 聚合维度：`商户支出台账、期间关账与持股分红引擎／后台实现`
- 文件数：13
- 有效内容行数：2722
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/dividend/DividendController.java
backend/src/main/java/com/complex/platform/dividend/DividendRepository.java
backend/src/main/java/com/complex/platform/dividend/DividendService.java
backend/src/main/java/com/complex/platform/dividend/JdbcDividendRepository.java
backend/src/main/java/com/complex/platform/dividend/DividendMath.java
backend/src/main/java/com/complex/platform/dividend/DividendMonthlyPreviewJob.java
backend/src/main/java/com/complex/platform/dividend/DividendPeriod.java
backend/src/main/java/com/complex/platform/dividend/DividendRecords.java
backend/src/main/java/com/complex/platform/dividend/DividendReserveReleaseJob.java
backend/src/main/java/com/complex/platform/dividend/DividendReserveReleaseMath.java
backend/src/main/java/com/complex/platform/dividend/dto/DividendActionRequest.java
backend/src/main/java/com/complex/platform/dividend/dto/DividendComputeRequest.java
backend/src/main/java/com/complex/platform/dividend/dto/DividendPlanRequest.java
<!-- FILES-END -->

## M342 — 商户支出台账、期间关账与持股分红引擎／后台实现／第2段

- 聚合维度：`商户支出台账、期间关账与持股分红引擎／后台实现`
- 文件数：11
- 有效内容行数：838
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/merchantexpense/JdbcMerchantExpenseRepository.java
backend/src/main/java/com/complex/platform/merchantexpense/MerchantExpenseController.java
backend/src/main/java/com/complex/platform/merchantexpense/MerchantExpenseRepository.java
backend/src/main/java/com/complex/platform/merchantexpense/MerchantExpenseService.java
backend/src/main/java/com/complex/platform/merchantexpense/MerchantExpenseDriftRecord.java
backend/src/main/java/com/complex/platform/merchantexpense/MerchantExpenseEntryRecord.java
backend/src/main/java/com/complex/platform/merchantexpense/MerchantExpenseSummaryRecord.java
backend/src/main/java/com/complex/platform/merchantexpense/MerchantExpenseSyncResultRecord.java
backend/src/main/java/com/complex/platform/merchantexpense/dto/MerchantExpenseEntryRequest.java
backend/src/main/java/com/complex/platform/merchantexpense/dto/MerchantExpenseReversalRequest.java
backend/src/main/java/com/complex/platform/merchantexpense/dto/MerchantExpenseSyncRequest.java
<!-- FILES-END -->

## M343 — 商户支出台账、期间关账与持股分红引擎／数据库迁移／第1段

- 聚合维度：`商户支出台账、期间关账与持股分红引擎／数据库迁移`
- 文件数：7
- 有效内容行数：509
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V297__merchant_expense_ledger.sql
backend/src/main/resources/db/migration/V299__dividend_engine.sql
backend/src/main/resources/db/migration/V302__dividend_period_lock_index.sql
backend/src/main/resources/db/migration/V303__dividend_deduction_options.sql
backend/src/main/resources/db/migration/V304__dividend_reserve_release.sql
backend/src/main/resources/db/migration/V308__dividend_principal_type.sql
backend/src/main/resources/db/migration/V309__dividend_custom_deductions.sql
<!-- FILES-END -->

## M344 — 商户支出台账、期间关账与持股分红引擎／后台验证／第1段

- 聚合维度：`商户支出台账、期间关账与持股分红引擎／后台验证`
- 文件数：6
- 有效内容行数：2793
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/dividend/DividendJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/dividend/DividendMathTests.java
backend/src/test/java/com/complex/platform/dividend/DividendMonthlyPreviewJobTests.java
backend/src/test/java/com/complex/platform/dividend/DividendReserveReleaseJobTests.java
backend/src/test/java/com/complex/platform/dividend/DividendReserveReleaseMathTests.java
backend/src/test/java/com/complex/platform/dividend/DividendServiceTests.java
<!-- FILES-END -->

## M345 — 商户支出台账、期间关账与持股分红引擎／后台验证／第2段

- 聚合维度：`商户支出台账、期间关账与持股分红引擎／后台验证`
- 文件数：2
- 有效内容行数：646
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/merchantexpense/MerchantExpenseJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/merchantexpense/MerchantExpenseServiceTests.java
<!-- FILES-END -->

## M346 — 商户支出台账、期间关账与持股分红引擎／网页与验证／第1段

- 聚合维度：`商户支出台账、期间关账与持股分红引擎／网页与验证`
- 文件数：6
- 有效内容行数：1851
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/FinanceDividendsPage.tsx
apps/web/src/admin/pages/FinanceDividendsPage.reserve.test.tsx
apps/web/src/admin/pages/FinanceMerchantExpensePage.tsx
apps/web/src/admin/pages/FinanceMerchantExpensePage.lock.test.tsx
apps/web/src/admin/pages/MerchantZoneDividendsPage.tsx
apps/web/src/admin/pages/MerchantZoneDividendsPage.test.tsx
<!-- FILES-END -->

## M347 — 商户支出台账、期间关账与持股分红引擎／文档与接口定义／第1段

- 聚合维度：`商户支出台账、期间关账与持股分红引擎／文档与接口定义`
- 文件数：1
- 有效内容行数：352
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/dividend-engine-prd-v0.1.md
<!-- FILES-END -->

## M348 — 经营看板指标、来源真实性、指标异常处置与 AI 质检边界／后台实现／第1段

- 聚合维度：`经营看板指标、来源真实性、指标异常处置与 AI 质检边界／后台实现`
- 文件数：6
- 有效内容行数：2921
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/analytics/AiQualitySuggestionRequest.java
backend/src/main/java/com/complex/platform/analytics/AiQualitySuggestionResponse.java
backend/src/main/java/com/complex/platform/analytics/AiThresholdsRequest.java
backend/src/main/java/com/complex/platform/analytics/AnalyticsController.java
backend/src/main/java/com/complex/platform/analytics/AnalyticsRepository.java
backend/src/main/java/com/complex/platform/analytics/AnalyticsService.java
<!-- FILES-END -->

## M349 — 经营看板指标、来源真实性、指标异常处置与 AI 质检边界／后台实现／第2段

- 聚合维度：`经营看板指标、来源真实性、指标异常处置与 AI 质检边界／后台实现`
- 文件数：1
- 有效内容行数：3072
- 标记：OVERSIZE-SINGLE-FILE

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/analytics/JdbcAnalyticsRepository.java
<!-- FILES-END -->

## M350 — 经营看板指标、来源真实性、指标异常处置与 AI 质检边界／后台实现／第3段

- 聚合维度：`经营看板指标、来源真实性、指标异常处置与 AI 质检边界／后台实现`
- 文件数：11
- 有效内容行数：2623
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/analytics/AnalyticsRecords.java
backend/src/main/java/com/complex/platform/analytics/AnomalyConfigRequest.java
backend/src/main/java/com/complex/platform/analytics/AnomalyDispositionRequest.java
backend/src/main/java/com/complex/platform/analytics/BusinessReviewLockRequest.java
backend/src/main/java/com/complex/platform/analytics/DashboardAggregationJob.java
backend/src/main/java/com/complex/platform/analytics/HolidayFetchRequest.java
backend/src/main/java/com/complex/platform/analytics/HolidayPeriodRequest.java
backend/src/main/java/com/complex/platform/analytics/LocalLlmClient.java
backend/src/main/java/com/complex/platform/analytics/MetricRegistry.java
backend/src/main/java/com/complex/platform/analytics/OpenAiCompatibleLocalLlmClient.java
backend/src/main/java/com/complex/platform/analytics/OperatingParamsRecord.java
<!-- FILES-END -->

## M351 — 经营看板指标、来源真实性、指标异常处置与 AI 质检边界／后台实现／第4段

- 聚合维度：`经营看板指标、来源真实性、指标异常处置与 AI 质检边界／后台实现`
- 文件数：2
- 有效内容行数：418
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/analytics/OperatingParamsService.java
backend/src/main/java/com/complex/platform/analytics/OperatingParamsRequest.java
<!-- FILES-END -->

## M352 — 经营看板指标、来源真实性、指标异常处置与 AI 质检边界／数据库迁移／第1段

- 聚合维度：`经营看板指标、来源真实性、指标异常处置与 AI 质检边界／数据库迁移`
- 文件数：12
- 有效内容行数：853
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V017__p0_analytics_ai_quality_risk_dashboard.sql
backend/src/main/resources/db/migration/V037__p2_business_metric_snapshots.sql
backend/src/main/resources/db/migration/V040__t3d_local_llm_ai_quality_mode.sql
backend/src/main/resources/db/migration/V070__t12_dashboard_etl_upsert_key.sql
backend/src/main/resources/db/migration/V115__analytics_group_overview_permission.sql
backend/src/main/resources/db/migration/V160__complex_operating_params.sql
backend/src/main/resources/db/migration/V206__analytics_report_export_permission.sql
backend/src/main/resources/db/migration/V207__metric_anomaly_pipeline.sql
backend/src/main/resources/db/migration/V216__business_review_locks.sql
backend/src/main/resources/db/migration/V218__analytics_ai_thresholds.sql
backend/src/main/resources/db/migration/V235__analytics_intent_seed_zh.sql
backend/src/main/resources/db/migration/V306__metric_anomaly_disposition.sql
<!-- FILES-END -->

## M353 — 经营看板指标、来源真实性、指标异常处置与 AI 质检边界／后台验证／第1段

- 聚合维度：`经营看板指标、来源真实性、指标异常处置与 AI 质检边界／后台验证`
- 文件数：3
- 有效内容行数：1964
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/analytics/AbandonedEntitlementRevenueJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/analytics/AnalyticsControllerTests.java
backend/src/test/java/com/complex/platform/analytics/AnalyticsJdbcIntegrationTests.java
<!-- FILES-END -->

## M354 — 经营看板指标、来源真实性、指标异常处置与 AI 质检边界／后台验证／第2段

- 聚合维度：`经营看板指标、来源真实性、指标异常处置与 AI 质检边界／后台验证`
- 文件数：2
- 有效内容行数：2601
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/analytics/AnalyticsServiceTests.java
backend/src/test/java/com/complex/platform/analytics/DashboardAggregationJobNetRevenueTests.java
<!-- FILES-END -->

## M355 — 经营看板指标、来源真实性、指标异常处置与 AI 质检边界／后台验证／第3段

- 聚合维度：`经营看板指标、来源真实性、指标异常处置与 AI 质检边界／后台验证`
- 文件数：4
- 有效内容行数：1766
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/analytics/MetricDetailConsistencyJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/analytics/MetricDetailProjectScopeJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/analytics/MetricDetailServiceTests.java
backend/src/test/java/com/complex/platform/analytics/OpenAiCompatibleLocalLlmClientTests.java
<!-- FILES-END -->

## M356 — 经营看板指标、来源真实性、指标异常处置与 AI 质检边界／网页与验证／第1段

- 聚合维度：`经营看板指标、来源真实性、指标异常处置与 AI 质检边界／网页与验证`
- 文件数：18
- 有效内容行数：2935
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/AbandonedEntitlementRevenuePage.tsx
apps/web/src/admin/pages/AiThresholdsPage.tsx
apps/web/src/admin/pages/AiThresholdsPage.test.tsx
apps/web/src/admin/pages/AnalyticsKpiFocus.test.tsx
apps/web/src/admin/pages/BusinessAnalyticsPage.tsx
apps/web/src/admin/pages/BusinessAnalyticsPage.degraded.test.tsx
apps/web/src/admin/pages/BusinessAnalyticsPage.disposition.test.tsx
apps/web/src/admin/pages/BusinessAnalyticsPage.metricdetail.test.tsx
apps/web/src/admin/pages/BusinessAnalyticsPage.test.tsx
apps/web/src/admin/pages/GroupOverviewPage.tsx
apps/web/src/admin/pages/MetricDetailPage.tsx
apps/web/src/admin/pages/MetricDetailPage.test.tsx
apps/web/src/admin/pages/MonthlyComparisonPage.tsx
apps/web/src/admin/pages/MonthlyComparisonPage.kpi.test.tsx
apps/web/src/admin/pages/OccupancyAnalyticsPage.tsx
apps/web/src/admin/pages/OccupancyAnalyticsPage.rentToSales.test.tsx
apps/web/src/admin/pages/OccupancyAnalyticsPage.robustness.test.tsx
apps/web/src/admin/pages/OccupancyAnalyticsPage.test.tsx
<!-- FILES-END -->

## M357 — 经营看板指标、来源真实性、指标异常处置与 AI 质检边界／网页与验证／第2段

- 聚合维度：`经营看板指标、来源真实性、指标异常处置与 AI 质检边界／网页与验证`
- 文件数：11
- 有效内容行数：1363
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/OperatingParamsPage.tsx
apps/web/src/admin/pages/OverviewPage.tsx
apps/web/src/admin/pages/OverviewPage.datepicker.test.tsx
apps/web/src/admin/pages/OverviewPage.kpi.test.tsx
apps/web/src/admin/pages/OverviewPage.metricdetail.test.tsx
apps/web/src/admin/pages/ReceivablesAnalyticsPage.tsx
apps/web/src/admin/pages/ReceivablesAnalyticsPage.kpi.test.tsx
apps/web/src/admin/pages/ReceivablesAnalyticsPage.metricdetail.test.tsx
apps/web/src/admin/pages/ReceivablesAnalyticsPage.robustness.test.tsx
apps/web/src/admin/pages/T3AReadonlyAnalyticsPage.tsx
apps/web/src/admin/pages/T3AReadonlyAnalyticsPage.test.tsx
<!-- FILES-END -->

## M358 — 经营看板指标、来源真实性、指标异常处置与 AI 质检边界／文档与接口定义／第1段

- 聚合维度：`经营看板指标、来源真实性、指标异常处置与 AI 质检边界／文档与接口定义`
- 文件数：10
- 有效内容行数：2945
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/business-dashboard-metrics-product-prd-v0.1.md
docs/data-analysis-layer-spec-v0.2.md
docs/data-risk-ai-development-design.md
docs/p2-business-metric-snapshots-api-contract-v0.1.md
docs/p2-business-metric-snapshots-completion-audit-v0.1.md
docs/p2-python-ai-service-boundary-completion-audit-v0.1.md
docs/p2-python-ai-service-boundary-contract-v0.1.md
docs/r5-spec-T12-dashboard-aggregation-dev-design-20260618.md
docs/t3d-local-llm-ai-quality-v0.1.md
docs/superpowers/plans/2026-06-14-p0-analytics-ai-quality-risk-dashboard-slice.md
<!-- FILES-END -->

## M359 — 经营看板指标、来源真实性、指标异常处置与 AI 质检边界／文档与接口定义／第2段

- 聚合维度：`经营看板指标、来源真实性、指标异常处置与 AI 质检边界／文档与接口定义`
- 文件数：2
- 有效内容行数：159
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/superpowers/plans/2026-06-15-p2-business-metric-snapshots.md
docs/superpowers/specs/2026-06-15-t3d-local-llm-ai-quality-design.md
<!-- FILES-END -->

## M360 — 经营看板指标、来源真实性、指标异常处置与 AI 质检边界／检查及生成工具／第1段

- 聚合维度：`经营看板指标、来源真实性、指标异常处置与 AI 质检边界／检查及生成工具`
- 文件数：1
- 有效内容行数：142
- 标记：NORMAL

<!-- FILES-BEGIN -->
tools/verify-t3d-local-llm-ai-quality.mjs
<!-- FILES-END -->

## M361 — 管理后台壳、路由/权限矩阵、页面验收与 UI 自动化／需求HTML／第1段

- 聚合维度：`管理后台壳、路由/权限矩阵、页面验收与 UI 自动化／需求HTML`
- 文件数：4
- 有效内容行数：73
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/index.html
apps/web/amusement/index.html
apps/web/bar/index.html
apps/web/ticketing/index.html
<!-- FILES-END -->

## M362 — 管理后台壳、路由/权限矩阵、页面验收与 UI 自动化／配置与工程入口／第1段

- 聚合维度：`管理后台壳、路由/权限矩阵、页面验收与 UI 自动化／配置与工程入口`
- 文件数：2
- 有效内容行数：72
- 标记：NORMAL

<!-- FILES-BEGIN -->
playwright.config.ts
playwright.real-backend.config.ts
<!-- FILES-END -->

## M363 — 管理后台壳、路由/权限矩阵、页面验收与 UI 自动化／网页与验证／第1段

- 聚合维度：`管理后台壳、路由/权限矩阵、页面验收与 UI 自动化／网页与验证`
- 文件数：11
- 有效内容行数：574
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/package.json
apps/web/tsconfig.json
apps/web/vite.config.ts
apps/web/vitest.config.ts
apps/web/public/fonts/jetbrains-mono-latin.woff2
apps/web/public/fonts/jetbrains-mono-latin-ext.woff2
apps/web/public/fonts/plus-jakarta-sans-latin.woff2
apps/web/public/fonts/plus-jakarta-sans-latin-ext.woff2
apps/web/src/basePath.ts
apps/web/src/basePath.test.ts
apps/web/src/main.tsx
<!-- FILES-END -->

## M364 — 管理后台壳、路由/权限矩阵、页面验收与 UI 自动化／网页与验证／第2段

- 聚合维度：`管理后台壳、路由/权限矩阵、页面验收与 UI 自动化／网页与验证`
- 文件数：1
- 有效内容行数：2577
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/styles.css
<!-- FILES-END -->

## M365 — 管理后台壳、路由/权限矩阵、页面验收与 UI 自动化／网页与验证／第3段

- 聚合维度：`管理后台壳、路由/权限矩阵、页面验收与 UI 自动化／网页与验证`
- 文件数：11
- 有效内容行数：2942
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/AdminApp.tsx
apps/web/src/admin/adminRoutes.ts
apps/web/src/admin/adminRoutes.test.ts
apps/web/src/admin/types.ts
apps/web/src/admin/client/adminClient.ts
apps/web/src/admin/client/adminClient.test.ts
apps/web/src/admin/components/AdminDataState.tsx
apps/web/src/admin/components/AdminDataState.test.tsx
apps/web/src/admin/components/AdminPageHeader.tsx
apps/web/src/admin/components/AuditTrailHint.tsx
apps/web/src/admin/components/BarVenueSelect.tsx
<!-- FILES-END -->

## M366 — 管理后台壳、路由/权限矩阵、页面验收与 UI 自动化／网页与验证／第4段

- 聚合维度：`管理后台壳、路由/权限矩阵、页面验收与 UI 自动化／网页与验证`
- 文件数：25
- 有效内容行数：1611
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/components/CnDatePicker.tsx
apps/web/src/admin/components/CnDatePicker.test.tsx
apps/web/src/admin/components/FormModal.tsx
apps/web/src/admin/components/FormModal.test.tsx
apps/web/src/admin/components/HighRiskConfirm.tsx
apps/web/src/admin/components/IconButton.tsx
apps/web/src/admin/components/MetricDetailEntry.tsx
apps/web/src/admin/components/MetricDetailEntry.projectscope.test.tsx
apps/web/src/admin/components/MiniChart.tsx
apps/web/src/admin/components/PermissionSwitch.tsx
apps/web/src/admin/components/RowActions.tsx
apps/web/src/admin/components/RowActions.test.tsx
apps/web/src/admin/components/TablePager.tsx
apps/web/src/admin/components/TechDetails.tsx
apps/web/src/admin/components/rowSearch.ts
apps/web/src/admin/components/useSortedRows.ts
apps/web/src/admin/format/ids.ts
apps/web/src/admin/hooks/useAdminAction.ts
apps/web/src/admin/hooks/useAdminResource.ts
apps/web/src/admin/hooks/useAdminResource.test.ts
apps/web/src/admin/hooks/useSelectedRow.ts
apps/web/src/admin/hooks/useServerPagedRows.ts
apps/web/src/admin/pages/M006AccessGovernance.test.tsx
apps/web/src/admin/pages/M006AccessRegistration.test.tsx
apps/web/src/admin/pages/M006BarStoredValue.test.tsx
<!-- FILES-END -->

## M367 — 管理后台壳、路由/权限矩阵、页面验收与 UI 自动化／网页与验证／第5段

- 聚合维度：`管理后台壳、路由/权限矩阵、页面验收与 UI 自动化／网页与验证`
- 文件数：1
- 有效内容行数：22
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/test/setup.ts
<!-- FILES-END -->

## M368 — 管理后台壳、路由/权限矩阵、页面验收与 UI 自动化／端到端验证／第1段

- 聚合维度：`管理后台壳、路由/权限矩阵、页面验收与 UI 自动化／端到端验证`
- 文件数：11
- 有效内容行数：2682
- 标记：NORMAL

<!-- FILES-BEGIN -->
tests/e2e/full-admin-mvp.spec.ts
tests/e2e/full-admin-remaining.spec.ts
tests/e2e/full-admin-ui-hardening.spec.ts
tests/e2e/full-admin-visual.spec.ts
tests/e2e/web-ui-real-backend.spec.ts
tests/e2e/full-admin-visual.spec.ts-snapshots/full-admin-bar-orders-mobile-chromium-linux.png
tests/e2e/full-admin-visual.spec.ts-snapshots/full-admin-dynamic-pricing-mobile-chromium-linux.png
tests/e2e/full-admin-visual.spec.ts-snapshots/full-admin-ops-readiness-desktop-chromium-linux.png
tests/e2e/full-admin-visual.spec.ts-snapshots/full-admin-overview-desktop-chromium-linux.png
tests/e2e/support/cn-date.ts
tests/e2e/support/global-setup.ts
<!-- FILES-END -->

## M369 — 管理后台壳、路由/权限矩阵、页面验收与 UI 自动化／端到端验证／第2段

- 聚合维度：`管理后台壳、路由/权限矩阵、页面验收与 UI 自动化／端到端验证`
- 文件数：1
- 有效内容行数：1046
- 标记：NORMAL

<!-- FILES-BEGIN -->
tests/e2e/support/mock-api.ts
<!-- FILES-END -->

## M370 — 管理后台壳、路由/权限矩阵、页面验收与 UI 自动化／文档与接口定义／第1段

- 聚合维度：`管理后台壳、路由/权限矩阵、页面验收与 UI 自动化／文档与接口定义`
- 文件数：13
- 有效内容行数：2226
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/closed-loop-implementation-ui-acceptance-report-v0.1.md
docs/frontend-admin-development-design.md
docs/full-admin-mvp-code-implementation-audit-v0.1.md
docs/full-admin-mvp-coding-package-v0.1.md
docs/full-admin-mvp-coding-task-breakdown-v0.1.csv
docs/full-admin-mvp-requirements-code-matrix-v0.1.csv
docs/full-admin-page-permission-field-matrix-v0.1.csv
docs/full-admin-remaining-code-implementation-audit-v0.1.md
docs/full-admin-remaining-requirements-code-matrix-v0.1.csv
docs/full-admin-srs-v0.1.md
docs/full-admin-ui-acceptance-matrix-v0.1.md
docs/full-admin-ui-automation-acceptance-report-20260618.md
docs/ui-automation-acceptance-report-v0.1.md
<!-- FILES-END -->

## M371 — 管理后台壳、路由/权限矩阵、页面验收与 UI 自动化／检查及生成工具／第1段

- 聚合维度：`管理后台壳、路由/权限矩阵、页面验收与 UI 自动化／检查及生成工具`
- 文件数：3
- 有效内容行数：662
- 标记：NORMAL

<!-- FILES-BEGIN -->
tools/generate-ui-experience-acceptance-report.mjs
tools/run-real-backend-ui-e2e.mjs
tools/verify-full-admin-page-matrix.mjs
<!-- FILES-END -->

## M372 — 一体机、安卓终端（工作手机核销、平板）与售票能力开关／后台实现／第1段

- 聚合维度：`一体机、安卓终端（工作手机核销、平板）与售票能力开关／后台实现`
- 文件数：6
- 有效内容行数：103
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/terminal/KioskConfigResponse.java
backend/src/main/java/com/complex/platform/terminal/KioskHeartbeatRecord.java
backend/src/main/java/com/complex/platform/terminal/PlatformTerminalMerchantDirectory.java
backend/src/main/java/com/complex/platform/terminal/TerminalKioskConfigRecord.java
backend/src/main/java/com/complex/platform/terminal/TerminalMerchantDirectory.java
backend/src/main/java/com/complex/platform/terminal/dto/KioskHeartbeatRequest.java
<!-- FILES-END -->

## M373 — 一体机、安卓终端（工作手机核销、平板）与售票能力开关／数据库迁移／第1段

- 聚合维度：`一体机、安卓终端（工作手机核销、平板）与售票能力开关／数据库迁移`
- 文件数：3
- 有效内容行数：88
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V039__security_terminal_sync_payload_redaction.sql
backend/src/main/resources/db/migration/V111__terminal_type_authority_code_alignment.sql
backend/src/main/resources/db/migration/V324__ticket_refund_terminal_state.sql
<!-- FILES-END -->

## M374 — 一体机、安卓终端（工作手机核销、平板）与售票能力开关／后台验证／第1段

- 聚合维度：`一体机、安卓终端（工作手机核销、平板）与售票能力开关／后台验证`
- 文件数：1
- 有效内容行数：81
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/gate/DeviceKeystoreAndTabletSwitchTests.java
<!-- FILES-END -->

## M375 — 一体机、安卓终端（工作手机核销、平板）与售票能力开关／安卓终端与验证／第1段

- 聚合维度：`一体机、安卓终端（工作手机核销、平板）与售票能力开关／安卓终端与验证`
- 文件数：18
- 有效内容行数：2783
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/android-terminal/README.md
apps/android-terminal/build.gradle.kts
apps/android-terminal/gradle.properties
apps/android-terminal/settings.gradle.kts
apps/android-terminal/android-app/build.gradle.kts
apps/android-terminal/android-app/gradle.properties
apps/android-terminal/android-app/settings.gradle.kts
apps/android-terminal/android-app/app/build.gradle.kts
apps/android-terminal/android-app/app/src/main/AndroidManifest.xml
apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/DeviceKeystore.kt
apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt
apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt
apps/android-terminal/android-app/app/src/main/res/raw/complex160.pem
apps/android-terminal/android-app/app/src/main/res/values/styles.xml
apps/android-terminal/android-app/app/src/main/res/xml/network_security_config.xml
apps/android-terminal/src/main/kotlin/com/complex/terminal/InMemoryLocalStore.kt
apps/android-terminal/src/main/kotlin/com/complex/terminal/P0Fixture.kt
apps/android-terminal/src/main/kotlin/com/complex/terminal/P0TerminalSmoke.kt
<!-- FILES-END -->

## M376 — 一体机、安卓终端（工作手机核销、平板）与售票能力开关／安卓终端与验证／第2段

- 聚合维度：`一体机、安卓终端（工作手机核销、平板）与售票能力开关／安卓终端与验证`
- 文件数：8
- 有效内容行数：2604
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt
apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt
apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalWorkflow.kt
apps/android-terminal/src/test/kotlin/com/complex/terminal/InMemoryLocalStoreTest.kt
apps/android-terminal/src/test/kotlin/com/complex/terminal/NdefTextTest.kt
apps/android-terminal/src/test/kotlin/com/complex/terminal/P0FixtureTest.kt
apps/android-terminal/src/test/kotlin/com/complex/terminal/TerminalFieldWorkflowTest.kt
apps/android-terminal/src/test/kotlin/com/complex/terminal/TerminalRequestSerializationTest.kt
<!-- FILES-END -->

## M377 — 一体机、安卓终端（工作手机核销、平板）与售票能力开关／文档与接口定义／第1段

- 聚合维度：`一体机、安卓终端（工作手机核销、平板）与售票能力开关／文档与接口定义`
- 文件数：2
- 有效内容行数：2115
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/android-device-terminal-development-design.md
docs/android-terminal-breakdown.md
<!-- FILES-END -->

## M378 — 一体机、安卓终端（工作手机核销、平板）与售票能力开关／文档与接口定义／第2段

- 聚合维度：`一体机、安卓终端（工作手机核销、平板）与售票能力开关／文档与接口定义`
- 文件数：3
- 有效内容行数：1676
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/terminal-display-layer-spec-v0.2.md
docs/terminal-page-product-prd-v0.1.md
docs/web-mobile-app-terminal-extension-product-prd-v0.1.md
<!-- FILES-END -->

## M379 — 一体机、安卓终端（工作手机核销、平板）与售票能力开关／运行和发布脚本／第1段

- 聚合维度：`一体机、安卓终端（工作手机核销、平板）与售票能力开关／运行和发布脚本`
- 文件数：1
- 有效内容行数：10
- 标记：NORMAL

<!-- FILES-BEGIN -->
ops/160/android-sdk-env.sh
<!-- FILES-END -->

## M380 — 一体机、安卓终端（工作手机核销、平板）与售票能力开关／检查及生成工具／第1段

- 聚合维度：`一体机、安卓终端（工作手机核销、平板）与售票能力开关／检查及生成工具`
- 文件数：3
- 有效内容行数：445
- 标记：NORMAL

<!-- FILES-BEGIN -->
tools/run-android-gradle.mjs
tools/run-android-p0-smoke.mjs
tools/verify-android-p0-smoke-wrapper.mjs
<!-- FILES-END -->

## M381 — 数据库迁移体系与其余迁移：版本序、缺号、就地改写、权限种子与运行角色授权／数据库迁移／第1段

- 聚合维度：`数据库迁移体系与其余迁移：版本序、缺号、就地改写、权限种子与运行角色授权／数据库迁移`
- 文件数：25
- 有效内容行数：890
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V001__baseline.sql
backend/src/main/resources/db/migration/V002__p0_foundation_first_slice.sql
backend/src/main/resources/db/migration/V042__merchant_profile_extended_fields.sql
backend/src/main/resources/db/migration/V066__ticket_anticounterfeit_window_nonce.sql
backend/src/main/resources/db/migration/V067__ticket_multiperson_reverse.sql
backend/src/main/resources/db/migration/V075__entitlement_merchant_and_ticket_validity.sql
backend/src/main/resources/db/migration/V078__entitlement_revenue_allocation.sql
backend/src/main/resources/db/migration/V084__object_space_mapping.sql
backend/src/main/resources/db/migration/V113__ticket_type_credential_refresh_interval.sql
backend/src/main/resources/db/migration/V121__cam_trueup_and_refund_direction.sql
backend/src/main/resources/db/migration/V133__ticket_type_stock_quantity.sql
backend/src/main/resources/db/migration/V140__append_only_enforcement_and_runtime_role.sql
backend/src/main/resources/db/migration/V156__refund_tiered_authorization.sql
backend/src/main/resources/db/migration/V157__ticket_refund_requested_by.sql
backend/src/main/resources/db/migration/V159__a6_data_integrity_constraints.sql
backend/src/main/resources/db/migration/V202__ar_receivable_aging_index.sql
backend/src/main/resources/db/migration/V208__merchant_summaries_real_pipeline.sql
backend/src/main/resources/db/migration/V209__holiday_calendar.sql
backend/src/main/resources/db/migration/V220__ticket_refunds_merchant_id.sql
backend/src/main/resources/db/migration/V225__entitlement_kind_price_batch3.sql
backend/src/main/resources/db/migration/V227__entitlement_day_activation_batch3.sql
backend/src/main/resources/db/migration/V230__ticket_type_category_batch4.sql
backend/src/main/resources/db/migration/V232__logistics_amusement_time.sql
backend/src/main/resources/db/migration/V234__amusement_exit_release.sql
backend/src/main/resources/db/migration/V253__status_check_constraints.sql
<!-- FILES-END -->

## M382 — 数据库迁移体系与其余迁移：版本序、缺号、就地改写、权限种子与运行角色授权／数据库迁移／第2段

- 聚合维度：`数据库迁移体系与其余迁移：版本序、缺号、就地改写、权限种子与运行角色授权／数据库迁移`
- 文件数：14
- 有效内容行数：520
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V263__bar_venue.sql
backend/src/main/resources/db/migration/V267__bar_is_merchant_drop_bar_venue.sql
backend/src/main/resources/db/migration/V274__drop_abolished_cam_tables.sql
backend/src/main/resources/db/migration/V280__drop_bar_age_verifications.sql
backend/src/main/resources/db/migration/V285__ticket_instance_parent_model.sql
backend/src/main/resources/db/migration/V286__whole_ticket_refund_config.sql
backend/src/main/resources/db/migration/V288__ticket_bar_binding.sql
backend/src/main/resources/db/migration/V290__drop_paid_membership.sql
backend/src/main/resources/db/migration/V293__ticket_type_active_status_normalize.sql
backend/src/main/resources/db/migration/V294__ticket_type_published_without_binding_normalize.sql
backend/src/main/resources/db/migration/V325__aud010_device_credential_nonce_and_device_signing_key.sql
backend/src/main/resources/db/migration/V344__fund_ledger_append_only_hardening.sql
backend/src/main/resources/db/migration/V349__staff_sales_position.sql
backend/src/main/resources/db/migration/V376__cost_bearer_required.sql
<!-- FILES-END -->

## M383 — 数据库迁移体系与其余迁移：版本序、缺号、就地改写、权限种子与运行角色授权／后台验证／第1段

- 聚合维度：`数据库迁移体系与其余迁移：版本序、缺号、就地改写、权限种子与运行角色授权／后台验证`
- 文件数：5
- 有效内容行数：258
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/support/AppendOnlyTestBypass.java
backend/src/test/java/com/complex/platform/support/InMemoryAuditEventRepository.java
backend/src/test/java/com/complex/platform/support/MigrationVersions.java
backend/src/test/java/com/complex/platform/support/TestSecurityActors.java
backend/src/test/java/com/complex/platform/system/AppendOnlyEnforcementJdbcIntegrationTests.java
<!-- FILES-END -->

## M384 — 数据库迁移体系与其余迁移：版本序、缺号、就地改写、权限种子与运行角色授权／文档与接口定义／第1段

- 聚合维度：`数据库迁移体系与其余迁移：版本序、缺号、就地改写、权限种子与运行角色授权／文档与接口定义`
- 文件数：1
- 有效内容行数：2297
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/foundation-database-design.md
<!-- FILES-END -->

## M385 — 数据库迁移体系与其余迁移：版本序、缺号、就地改写、权限种子与运行角色授权／文档与接口定义／第2段

- 聚合维度：`数据库迁移体系与其余迁移：版本序、缺号、就地改写、权限种子与运行角色授权／文档与接口定义`
- 文件数：2
- 有效内容行数：1121
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/matrix-migration-claims.json
docs/r3-migration-level-drift-audit-20260618.md
<!-- FILES-END -->

## M386 — 数据库迁移体系与其余迁移：版本序、缺号、就地改写、权限种子与运行角色授权／检查及生成工具／第1段

- 聚合维度：`数据库迁移体系与其余迁移：版本序、缺号、就地改写、权限种子与运行角色授权／检查及生成工具`
- 文件数：2
- 有效内容行数：149
- 标记：NORMAL

<!-- FILES-BEGIN -->
tools/verify-matrix-vs-migration.mjs
tools/verify-migration-version-order.mjs
<!-- FILES-END -->

## M387 — OpenAPI 契约闭合：operation 矩阵、x-permission/x-terminal、样例与客户端路由一致性／后台实现／第1段

- 聚合维度：`OpenAPI 契约闭合：operation 矩阵、x-permission/x-terminal、样例与客户端路由一致性／后台实现`
- 文件数：1
- 有效内容行数：23
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/config/OpenApiConfig.java
<!-- FILES-END -->

## M388 — OpenAPI 契约闭合：operation 矩阵、x-permission/x-terminal、样例与客户端路由一致性／文档与接口定义／第1段

- 聚合维度：`OpenAPI 契约闭合：operation 矩阵、x-permission/x-terminal、样例与客户端路由一致性／文档与接口定义`
- 文件数：2
- 有效内容行数：686
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/code-contract-difference-and-migration-audit-v0.1.md
docs/p0-api-contract-pack-v0.1.md
<!-- FILES-END -->

## M389 — OpenAPI 契约闭合：operation 矩阵、x-permission/x-terminal、样例与客户端路由一致性／文档与接口定义／第2段

- 聚合维度：`OpenAPI 契约闭合：operation 矩阵、x-permission/x-terminal、样例与客户端路由一致性／文档与接口定义`
- 文件数：1
- 有效内容行数：2584
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/p0-openapi-contract-closure-appendix-v0.1.md
<!-- FILES-END -->

## M390 — OpenAPI 契约闭合：operation 矩阵、x-permission/x-terminal、样例与客户端路由一致性／文档与接口定义／第3段

- 聚合维度：`OpenAPI 契约闭合：operation 矩阵、x-permission/x-terminal、样例与客户端路由一致性／文档与接口定义`
- 文件数：2
- 有效内容行数：936
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/p0-openapi-operation-acceptance-matrix-v0.1.md
docs/p0-task-api-permission-event-acceptance-map-v0.1.md
<!-- FILES-END -->

## M391 — OpenAPI 契约闭合：operation 矩阵、x-permission/x-terminal、样例与客户端路由一致性／文档与接口定义／第4段

- 聚合维度：`OpenAPI 契约闭合：operation 矩阵、x-permission/x-terminal、样例与客户端路由一致性／文档与接口定义`
- 文件数：1
- 有效内容行数：50519
- 标记：OVERSIZE-SINGLE-FILE

<!-- FILES-BEGIN -->
docs/openapi/openapi-p0.json
<!-- FILES-END -->

## M392 — OpenAPI 契约闭合：operation 矩阵、x-permission/x-terminal、样例与客户端路由一致性／文档与接口定义／第5段

- 聚合维度：`OpenAPI 契约闭合：operation 矩阵、x-permission/x-terminal、样例与客户端路由一致性／文档与接口定义`
- 文件数：1
- 有效内容行数：36485
- 标记：OVERSIZE-SINGLE-FILE

<!-- FILES-BEGIN -->
docs/openapi/openapi-p0.yaml
<!-- FILES-END -->

## M393 — OpenAPI 契约闭合：operation 矩阵、x-permission/x-terminal、样例与客户端路由一致性／文档与接口定义／第6段

- 聚合维度：`OpenAPI 契约闭合：operation 矩阵、x-permission/x-terminal、样例与客户端路由一致性／文档与接口定义`
- 文件数：1
- 有效内容行数：86
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/superpowers/plans/2026-06-15-legacy-api-physical-removal.md
<!-- FILES-END -->

## M394 — OpenAPI 契约闭合：operation 矩阵、x-permission/x-terminal、样例与客户端路由一致性／检查及生成工具／第1段

- 聚合维度：`OpenAPI 契约闭合：operation 矩阵、x-permission/x-terminal、样例与客户端路由一致性／检查及生成工具`
- 文件数：9
- 有效内容行数：1588
- 标记：NORMAL

<!-- FILES-BEGIN -->
tools/apply-openapi-p0-examples.mjs
tools/audit-client-api-routes.mjs
tools/audit-client-openapi-coverage.mjs
tools/generate-openapi-contract-closure-appendix.mjs
tools/generate-openapi-operation-matrix.mjs
tools/verify-client-payload-policy.mjs
tools/verify-no-legacy-api-prototype.mjs
tools/verify-openapi-contract.mjs
tools/verify-openapi-p0-examples.mjs
<!-- FILES-END -->

## M395 — 后端平台基础：配置、启动守卫、健康/版本、异常映射与安全过滤链／配置与工程入口／第1段

- 聚合维度：`后端平台基础：配置、启动守卫、健康/版本、异常映射与安全过滤链／配置与工程入口`
- 文件数：6
- 有效内容行数：397
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/README.md
backend/pom.xml
backend/src/main/resources/application.yml
backend/src/main/resources/application-local.yml
backend/src/main/resources/application-prod.yml
backend/src/test/resources/application.properties
<!-- FILES-END -->

## M396 — 后端平台基础：配置、启动守卫、健康/版本、异常映射与安全过滤链／后台实现／第1段

- 聚合维度：`后端平台基础：配置、启动守卫、健康/版本、异常映射与安全过滤链／后台实现`
- 文件数：22
- 有效内容行数：1585
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/ComplexApiApplication.java
backend/src/main/java/com/complex/platform/access/DevAccessCredentialProvisioner.java
backend/src/main/java/com/complex/platform/common/Hashing.java
backend/src/main/java/com/complex/platform/common/LocalZone.java
backend/src/main/java/com/complex/platform/common/NotificationWindowConfig.java
backend/src/main/java/com/complex/platform/common/PhoneMask.java
backend/src/main/java/com/complex/platform/common/Strings.java
backend/src/main/java/com/complex/platform/common/api/ApiException.java
backend/src/main/java/com/complex/platform/common/api/ApiRequests.java
backend/src/main/java/com/complex/platform/common/api/ApiResponse.java
backend/src/main/java/com/complex/platform/common/api/FieldLabels.java
backend/src/main/java/com/complex/platform/common/api/GlobalExceptionHandler.java
backend/src/main/java/com/complex/platform/common/api/PageResponse.java
backend/src/main/java/com/complex/platform/config/InternalJsonConfig.java
backend/src/main/java/com/complex/platform/config/ProductionDatabaseCredentialGuard.java
backend/src/main/java/com/complex/platform/config/RefundTieredAuthorizationProductionGuard.java
backend/src/main/java/com/complex/platform/config/SecurityConfig.java
backend/src/main/java/com/complex/platform/gate/ActionGateAspect.java
backend/src/main/java/com/complex/platform/gate/GateWebMvcConfig.java
backend/src/main/java/com/complex/platform/gate/GuardedAction.java
backend/src/main/java/com/complex/platform/system/SystemHealthController.java
backend/src/main/java/com/complex/platform/system/SystemVersionController.java
<!-- FILES-END -->

## M397 — 后端平台基础：配置、启动守卫、健康/版本、异常映射与安全过滤链／后台验证／第1段

- 聚合维度：`后端平台基础：配置、启动守卫、健康/版本、异常映射与安全过滤链／后台验证`
- 文件数：11
- 有效内容行数：934
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/common/HashingTests.java
backend/src/test/java/com/complex/platform/common/StringsTest.java
backend/src/test/java/com/complex/platform/common/api/ApiRequestsTests.java
backend/src/test/java/com/complex/platform/common/api/FieldLabelsTests.java
backend/src/test/java/com/complex/platform/common/api/GlobalExceptionHandlerTests.java
backend/src/test/java/com/complex/platform/config/ProductionDatabaseCredentialGuardTests.java
backend/src/test/java/com/complex/platform/gate/ActionGateAspectTests.java
backend/src/test/java/com/complex/platform/gate/CustomerGuardedActionAndRejectionAuditTests.java
backend/src/test/java/com/complex/platform/gate/FiveDomainGuardedActionCoverageTests.java
backend/src/test/java/com/complex/platform/gate/GuardedActionProxyBypassArchTest.java
backend/src/test/java/com/complex/platform/system/SystemControllerTests.java
<!-- FILES-END -->

## M398 — 后端平台基础：配置、启动守卫、健康/版本、异常映射与安全过滤链／文档与接口定义／第1段

- 聚合维度：`后端平台基础：配置、启动守卫、健康/版本、异常映射与安全过滤链／文档与接口定义`
- 文件数：7
- 有效内容行数：2825
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/architecture-overview-v0.1.md
docs/java-backend-platform-foundation-development-design.md
docs/r6-spec-C4-staged-quality-fixes-20260618.md
docs/system-development-spec.md
docs/superpowers/plans/2026-06-13-p0-first-coding-package-slice.md
docs/superpowers/plans/2026-06-14-p0-foundation-residual-platform-terminal-evidence-slice.md
docs/superpowers/plans/2026-06-14-p0-platform-master-data-slice.md
<!-- FILES-END -->

## M399 — 后端平台基础：配置、启动守卫、健康/版本、异常映射与安全过滤链／检查及生成工具／第1段

- 聚合维度：`后端平台基础：配置、启动守卫、健康/版本、异常映射与安全过滤链／检查及生成工具`
- 文件数：1
- 有效内容行数：168
- 标记：NORMAL

<!-- FILES-BEGIN -->
tools/run-backend-maven.mjs
<!-- FILES-END -->

## M400 — 160 部署、备份恢复、NAS、凭证轮换与运维 runbook／配置与工程入口／第1段

- 聚合维度：`160 部署、备份恢复、NAS、凭证轮换与运维 runbook／配置与工程入口`
- 文件数：1
- 有效内容行数：22
- 标记：NORMAL

<!-- FILES-BEGIN -->
.env.example
<!-- FILES-END -->

## M401 — 160 部署、备份恢复、NAS、凭证轮换与运维 runbook／文档与接口定义／第1段

- 聚合维度：`160 部署、备份恢复、NAS、凭证轮换与运维 runbook／文档与接口定义`
- 文件数：14
- 有效内容行数：1417
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/coding-host-protection-baseline-v0.1.md
docs/nas-backup-and-restore-runbook-v0.1.md
docs/nas-current-state-drift-report-v0.1.md
docs/nas-diagnostic-report-v0.1.md
docs/nas-it-controlled-mount-handoff-v0.1.md
docs/production-credential-rotation-runbook-v0.1.md
docs/server-160-runbook-v0.2.md
docs/production-readiness-evidence/nas-current-state-drift-latest.json
docs/production-readiness-evidence/nas-diagnostic-latest.json
docs/production-readiness-evidence/nas-restore-drill-latest.json
docs/production-readiness-evidence/nas-smb-mount-latest.json
docs/superpowers/plans/2026-06-14-p0-nas-backup-restore-drill.md
docs/superpowers/plans/2026-06-14-p0-runtime-service-scripts.md
docs/superpowers/plans/2026-06-14-p0a-ops-production-hardening-verifiable.md
<!-- FILES-END -->

## M402 — 160 部署、备份恢复、NAS、凭证轮换与运维 runbook／运行和发布脚本／第1段

- 聚合维度：`160 部署、备份恢复、NAS、凭证轮换与运维 runbook／运行和发布脚本`
- 文件数：25
- 有效内容行数：2272
- 标记：NORMAL

<!-- FILES-BEGIN -->
ops/160/README-backup-schedule.md
ops/160/complex-alert-dispatch.sh
ops/160/complex-backup.service
ops/160/complex-backup.sh
ops/160/complex-backup.timer
ops/160/complex-backup-alert.example
ops/160/complex-backup-guard.sh
ops/160/complex-backup-schedule.example
ops/160/complex-deploy-api.sh
ops/160/complex-deploy-web.sh
ops/160/complex-health-check.sh
ops/160/complex-lan-prod.sh
ops/160/complex-nas-preflight.sh
ops/160/complex-nas-system-mount.example.sh
ops/160/complex-restore-drill.service
ops/160/complex-restore-drill.sh
ops/160/complex-restore-drill.timer
ops/160/complex-restore-drill-guard.sh
ops/160/complex-rollback-api.sh
ops/160/complex-rollback-web.sh
ops/160/complex-runtime.sh
ops/160/complex-runtime-config-check.sh
ops/160/complex-verify-be000.sh
ops/160/complex-verify-p0.sh
ops/160/complex.env.example
<!-- FILES-END -->

## M403 — 160 部署、备份恢复、NAS、凭证轮换与运维 runbook／运行和发布脚本／第2段

- 聚合维度：`160 部署、备份恢复、NAS、凭证轮换与运维 runbook／运行和发布脚本`
- 文件数：1
- 有效内容行数：64
- 标记：NORMAL

<!-- FILES-BEGIN -->
ops/160/install-backup-schedule.sh
<!-- FILES-END -->

## M404 — 160 部署、备份恢复、NAS、凭证轮换与运维 runbook／检查及生成工具／第1段

- 聚合维度：`160 部署、备份恢复、NAS、凭证轮换与运维 runbook／检查及生成工具`
- 文件数：10
- 有效内容行数：1538
- 标记：NORMAL

<!-- FILES-BEGIN -->
tools/generate-nas-current-state-drift-report.mjs
tools/generate-nas-diagnostic-report.mjs
tools/verify-160-backup-restore-scripts.mjs
tools/verify-coding-host-protection.mjs
tools/verify-nas-it-handoff.mjs
tools/verify-nas-smb-mount-evidence.mjs
tools/verify-ops160-execution-policy.mjs
tools/verify-p0a-ops-hardening.mjs
tools/verify-prod-docs-access-policy.mjs
tools/verify-production-credential-rotation-ledger.mjs
<!-- FILES-END -->

## M405 — 生产就绪证据体系：阻断清单、责任包、manifest 模板与看板生成器／后台实现／第1段

- 聚合维度：`生产就绪证据体系：阻断清单、责任包、manifest 模板与看板生成器／后台实现`
- 文件数：11
- 有效内容行数：801
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/ops/productionreadiness/JdbcProductionReadinessRepository.java
backend/src/main/java/com/complex/platform/ops/productionreadiness/ProductionReadinessController.java
backend/src/main/java/com/complex/platform/ops/productionreadiness/ProductionReadinessRepository.java
backend/src/main/java/com/complex/platform/ops/productionreadiness/ProductionReadinessService.java
backend/src/main/java/com/complex/platform/ops/productionreadiness/ProductionReadinessCheck.java
backend/src/main/java/com/complex/platform/ops/productionreadiness/ProductionReadinessItem.java
backend/src/main/java/com/complex/platform/ops/productionreadiness/ProductionReadinessStatus.java
backend/src/main/java/com/complex/platform/ops/productionreadiness/dto/ProductionReadinessCheckRequest.java
backend/src/main/java/com/complex/platform/ops/productionreadiness/dto/ProductionReadinessCheckResponse.java
backend/src/main/java/com/complex/platform/ops/productionreadiness/dto/ProductionReadinessItemResponse.java
backend/src/main/java/com/complex/platform/ops/productionreadiness/dto/ProductionReadinessItemUpdateRequest.java
<!-- FILES-END -->

## M406 — 生产就绪证据体系：阻断清单、责任包、manifest 模板与看板生成器／后台验证／第1段

- 聚合维度：`生产就绪证据体系：阻断清单、责任包、manifest 模板与看板生成器／后台验证`
- 文件数：3
- 有效内容行数：646
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/ops/productionreadiness/ProductionReadinessControllerTests.java
backend/src/test/java/com/complex/platform/ops/productionreadiness/ProductionReadinessJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/ops/productionreadiness/ProductionReadinessServiceTests.java
<!-- FILES-END -->

## M407 — 生产就绪证据体系：阻断清单、责任包、manifest 模板与看板生成器／网页与验证／第1段

- 聚合维度：`生产就绪证据体系：阻断清单、责任包、manifest 模板与看板生成器／网页与验证`
- 文件数：2
- 有效内容行数：235
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/OpsReadinessPage.tsx
apps/web/src/admin/pages/OpsReadinessPage.test.tsx
<!-- FILES-END -->

## M408 — 生产就绪证据体系：阻断清单、责任包、manifest 模板与看板生成器／文档与接口定义／第1段

- 聚合维度：`生产就绪证据体系：阻断清单、责任包、manifest 模板与看板生成器／文档与接口定义`
- 文件数：24
- 有效内容行数：2957
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/closed-loop-current-state-snapshot-latest.json
docs/closed-loop-current-state-snapshot-v0.1.md
docs/closed-loop-live-acceptance-audit-20260618.md
docs/code-implementation-acceptance-audit-v0.1.md
docs/current-staged-handoff-manifest-latest.json
docs/current-staged-handoff-manifest-v0.1.md
docs/external-evidence-requirement-traceability-v0.1.md
docs/final-closed-loop-acceptance-audit-v0.1.md
docs/partial-requirement-mock-coverage-latest.json
docs/partial-requirement-mock-coverage-v0.1.md
docs/production-blocker-register-v0.1.md
docs/production-evidence-backfill-execution-ledger-v0.1.md
docs/production-evidence-backfill-priority-v0.1.md
docs/production-evidence-closure-gap-report-v0.1.md
docs/production-evidence-manifest-templates-v0.1.md
docs/production-evidence-owner-execution-packets-v0.1.md
docs/production-evidence-owner-guides-v0.1.md
docs/production-evidence-p0-quickstart-v0.1.md
docs/production-evidence-submission-kit-v0.1.md
docs/production-evidence-submission-status-v0.1.md
docs/production-existing-evidence-to-manifest-gap-audit-v0.1.md
docs/production-owner-action-packs-v0.1.md
docs/production-readiness-evidence-intake-template-v0.1.md
docs/production-readiness-implementation-ledger-template-v0.1.md
<!-- FILES-END -->

## M409 — 生产就绪证据体系：阻断清单、责任包、manifest 模板与看板生成器／文档与接口定义／第2段

- 聚合维度：`生产就绪证据体系：阻断清单、责任包、manifest 模板与看板生成器／文档与接口定义`
- 文件数：11
- 有效内容行数：2497
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/runtime-acceptance-evidence-latest.json
docs/runtime-acceptance-evidence-v0.1.md
docs/production-readiness-evidence/coding-host-protection-latest.json
docs/production-readiness-evidence/external-evidence-requirement-traceability-latest.csv
docs/production-readiness-evidence/external-evidence-requirement-traceability-latest.json
docs/production-readiness-evidence/p0a-ops-hardening-latest.json
docs/production-readiness-evidence/production-blocker-register-latest.json
docs/production-readiness-evidence/production-evidence-backfill-execution-ledger-latest.csv
docs/production-readiness-evidence/production-evidence-backfill-execution-ledger-latest.json
docs/production-readiness-evidence/production-evidence-backfill-priority-latest.json
docs/production-readiness-evidence/production-evidence-manifest-template-index-latest.json
<!-- FILES-END -->

## M410 — 生产就绪证据体系：阻断清单、责任包、manifest 模板与看板生成器／文档与接口定义／第3段

- 聚合维度：`生产就绪证据体系：阻断清单、责任包、manifest 模板与看板生成器／文档与接口定义`
- 文件数：8
- 有效内容行数：2934
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/production-readiness-evidence/production-evidence-owner-execution-packets-latest.json
docs/production-readiness-evidence/production-evidence-owner-guides-latest.json
docs/production-readiness-evidence/production-evidence-p0-quickstart-latest.csv
docs/production-readiness-evidence/production-evidence-p0-quickstart-latest.json
docs/production-readiness-evidence/production-evidence-submission-kit-latest.json
docs/production-readiness-evidence/production-evidence-submission-status-latest.json
docs/production-readiness-evidence/production-no-go-resolution-command-center-latest.csv
docs/production-readiness-evidence/production-no-go-resolution-command-center-latest.json
<!-- FILES-END -->

## M411 — 生产就绪证据体系：阻断清单、责任包、manifest 模板与看板生成器／文档与接口定义／第4段

- 聚合维度：`生产就绪证据体系：阻断清单、责任包、manifest 模板与看板生成器／文档与接口定义`
- 文件数：25
- 有效内容行数：2629
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/production-readiness-evidence/production-no-go-resolution-command-center-v0.1.md
docs/production-readiness-evidence/production-owner-action-packs-latest.json
docs/production-readiness-evidence/production-readiness-evidence-backfill-20260615.md
docs/production-readiness-evidence/security-baseline-latest.json
docs/production-readiness-evidence/owner-execution-packets/finance.md
docs/production-readiness-evidence/owner-execution-packets/it-ops-security.md
docs/production-readiness-evidence/owner-execution-packets/operations-legal.md
docs/production-readiness-evidence/owner-execution-packets/procurement-supplier.md
docs/production-readiness-evidence/owner-execution-packets/technical-owner.md
docs/production-readiness-evidence/owner-guides/finance.md
docs/production-readiness-evidence/owner-guides/it-ops-security.md
docs/production-readiness-evidence/owner-guides/operations-legal.md
docs/production-readiness-evidence/owner-guides/procurement-supplier.md
docs/production-readiness-evidence/owner-guides/technical-owner.md
docs/production-readiness-evidence/submission-manifest-templates/finance/template-PRD-CRED-001.json
docs/production-readiness-evidence/submission-manifest-templates/finance/template-PRD-CRED-3P-001.json
docs/production-readiness-evidence/submission-manifest-templates/finance/template-PRD-PAY-001.json
docs/production-readiness-evidence/submission-manifest-templates/finance/template-PRD-PAY-002.json
docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-160-001.json
docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-3P-001.json
docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-001.json
docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-3P-001.json
docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-DB-001.json
docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-JWT-001.json
docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-NAS-001.json
<!-- FILES-END -->

## M412 — 生产就绪证据体系：阻断清单、责任包、manifest 模板与看板生成器／文档与接口定义／第5段

- 聚合维度：`生产就绪证据体系：阻断清单、责任包、manifest 模板与看板生成器／文档与接口定义`
- 文件数：25
- 有效内容行数：764
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-OBJ-001.json
docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-OPEN-001.json
docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-CRED-OPS-001.json
docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-FACE-001.json
docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-MDM-001.json
docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-ALERT-001.json
docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-AUTOMOUNT-001.json
docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-DRILL-CADENCE-001.json
docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-SCHEDULE-001.json
docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-PAY-001.json
docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-PAY-002.json
docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-WECOM-001.json
docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-SEC-P0-MDM-DEVICE-001.json
docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-SEC-P0-OPS-RECOVERY-001.json
docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-SEC-P1-WEBHOOK-REPLAY-001.json
docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-SEC-P2-CSRF-API-001.json
docs/production-readiness-evidence/submission-manifest-templates/operations-legal/template-PRD-CRED-3P-001.json
docs/production-readiness-evidence/submission-manifest-templates/operations-legal/template-PRD-FACE-001.json
docs/production-readiness-evidence/submission-manifest-templates/operations-legal/template-PRD-WECOM-001.json
docs/production-readiness-evidence/submission-manifest-templates/procurement-supplier/template-PRD-3P-001.json
docs/production-readiness-evidence/submission-manifest-templates/procurement-supplier/template-PRD-CRED-3P-001.json
docs/production-readiness-evidence/submission-manifest-templates/procurement-supplier/template-PRD-FACE-001.json
docs/production-readiness-evidence/submission-manifest-templates/procurement-supplier/template-PRD-MDM-001.json
docs/production-readiness-evidence/submission-manifest-templates/procurement-supplier/template-SEC-P0-MDM-DEVICE-001.json
docs/production-readiness-evidence/submission-manifest-templates/procurement-supplier/template-SEC-P1-WEBHOOK-REPLAY-001.json
<!-- FILES-END -->

## M413 — 生产就绪证据体系：阻断清单、责任包、manifest 模板与看板生成器／文档与接口定义／第6段

- 聚合维度：`生产就绪证据体系：阻断清单、责任包、manifest 模板与看板生成器／文档与接口定义`
- 文件数：6
- 有效内容行数：159
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/production-readiness-evidence/submission-manifest-templates/technical-owner/template-PRD-CRED-JWT-001.json
docs/production-readiness-evidence/submission-manifest-templates/technical-owner/template-PRD-CRED-OPEN-001.json
docs/production-readiness-evidence/submission-manifest-templates/technical-owner/template-SEC-P1-WEBHOOK-REPLAY-001.json
docs/production-readiness-evidence/submission-manifest-templates/technical-owner/template-SEC-P2-CSRF-API-001.json
docs/production-readiness-evidence/submissions/.gitignore
docs/production-readiness-evidence/submissions/README.md
<!-- FILES-END -->

## M414 — 生产就绪证据体系：阻断清单、责任包、manifest 模板与看板生成器／检查及生成工具／第1段

- 聚合维度：`生产就绪证据体系：阻断清单、责任包、manifest 模板与看板生成器／检查及生成工具`
- 文件数：9
- 有效内容行数：2958
- 标记：NORMAL

<!-- FILES-BEGIN -->
tools/generate-closed-loop-current-state.mjs
tools/generate-external-evidence-requirement-traceability.mjs
tools/generate-partial-requirement-mock-coverage.mjs
tools/generate-production-blocker-register.mjs
tools/generate-production-closure-gap-report.mjs
tools/generate-production-evidence-backfill-execution-ledger.mjs
tools/generate-production-evidence-backfill-priority.mjs
tools/generate-production-evidence-manifest-templates.mjs
tools/generate-production-evidence-owner-execution-packets.mjs
<!-- FILES-END -->

## M415 — 生产就绪证据体系：阻断清单、责任包、manifest 模板与看板生成器／检查及生成工具／第2段

- 聚合维度：`生产就绪证据体系：阻断清单、责任包、manifest 模板与看板生成器／检查及生成工具`
- 文件数：10
- 有效内容行数：2964
- 标记：NORMAL

<!-- FILES-BEGIN -->
tools/generate-production-evidence-owner-guides.mjs
tools/generate-production-evidence-p0-quickstart.mjs
tools/generate-production-evidence-submission-kit.mjs
tools/generate-production-evidence-submission-status.mjs
tools/generate-production-no-go-resolution-command-center.mjs
tools/generate-production-owner-action-packs.mjs
tools/generate-runtime-acceptance-evidence.mjs
tools/prepare-production-evidence-manifest-drafts.mjs
tools/verify-entry-anchors.test.mjs
tools/verify-final-closed-loop-audit.mjs
<!-- FILES-END -->

## M416 — 生产就绪证据体系：阻断清单、责任包、manifest 模板与看板生成器／检查及生成工具／第3段

- 聚合维度：`生产就绪证据体系：阻断清单、责任包、manifest 模板与看板生成器／检查及生成工具`
- 文件数：12
- 有效内容行数：1879
- 标记：NORMAL

<!-- FILES-BEGIN -->
tools/verify-production-blocker-register-no-go-fixtures.mjs
tools/verify-production-closure-gap-reports.mjs
tools/verify-production-evidence-crosslinks.mjs
tools/verify-production-evidence-draft-prep-fixtures.mjs
tools/verify-production-evidence-intake-templates.mjs
tools/verify-production-evidence-negative-fixtures.mjs
tools/verify-production-evidence-positive-fixtures.mjs
tools/verify-production-evidence-positive-propagation-fixtures.mjs
tools/verify-production-evidence-submissions-guard.mjs
tools/verify-production-readiness-evidence.mjs
tools/verify-production-ready-no-go-fixtures.mjs
tools/verify-production-ready-positive-fixtures.mjs
<!-- FILES-END -->

## M417 — 安全基线、Semgrep 扫描证据与安全 findings 台账／配置与工程入口／第1段

- 聚合维度：`安全基线、Semgrep 扫描证据与安全 findings 台账／配置与工程入口`
- 文件数：1
- 有效内容行数：6
- 标记：NORMAL

<!-- FILES-BEGIN -->
.semgrepignore
<!-- FILES-END -->

## M418 — 安全基线、Semgrep 扫描证据与安全 findings 台账／文档与接口定义／第1段

- 聚合维度：`安全基线、Semgrep 扫描证据与安全 findings 台账／文档与接口定义`
- 文件数：3
- 有效内容行数：270
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/adversarial-audit-round3-findings-register-v0.1.md
docs/formal-security-baseline-generation-task-v0.1.md
docs/superpowers/reviews/2026-06-14-semgrep-scan-report.md
<!-- FILES-END -->

## M419 — 安全基线、Semgrep 扫描证据与安全 findings 台账／检查及生成工具／第1段

- 聚合维度：`安全基线、Semgrep 扫描证据与安全 findings 台账／检查及生成工具`
- 文件数：2
- 有效内容行数：786
- 标记：NORMAL

<!-- FILES-BEGIN -->
tools/generate-formal-security-baseline.mjs
tools/verify-formal-security-baseline.mjs
<!-- FILES-END -->

## M420 — 安全基线、Semgrep 扫描证据与安全 findings 台账／质量检查记录／第1段

- 聚合维度：`安全基线、Semgrep 扫描证据与安全 findings 台账／质量检查记录`
- 文件数：12
- 有效内容行数：2301
- 标记：NORMAL

<!-- FILES-BEGIN -->
security-scans/LEGACY-SECURITY-SCANS.md
security-scans/delivery-readiness-semgrep-20260615.json
security-scans/findings-register-v1.md
security-scans/formal-security-baseline-v1.md
security-scans/semgrep-latest-summary.json
security-scans/threat_model.md
security-scans/semgrep-20260623-180915-manual-request/json.stderr
security-scans/semgrep-20260623-180915-manual-request/sarif.stderr
security-scans/semgrep-20260623-180915-manual-request/semgrep.json
security-scans/semgrep-20260623-180915-manual-request/semgrep.sarif
security-scans/semgrep-20260703-delivery-gates-clean/semgrep.json
security-scans/semgrep-20260703-delivery-gates-clean/semgrep.sarif
<!-- FILES-END -->

## M421 — HTML→PRD 承接台账、规则账本、决策账本与审计轮次记录一致性／文档与接口定义／第1段

- 聚合维度：`HTML→PRD 承接台账、规则账本、决策账本与审计轮次记录一致性／文档与接口定义`
- 文件数：6
- 有效内容行数：866
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/business-html-source-of-truth-acceptance-spec-v0.1.md
docs/dev-plan-v7-audit-20260722.md
docs/development-documentation-adversarial-audit-v0.1.md
docs/development-documentation-audit-map-v0.1.csv
docs/development-documentation-audit-v0.2.md
docs/development-ready-spec-draft-v0.1.md
<!-- FILES-END -->

## M422 — HTML→PRD 承接台账、规则账本、决策账本与审计轮次记录一致性／文档与接口定义／第2段

- 聚合维度：`HTML→PRD 承接台账、规则账本、决策账本与审计轮次记录一致性／文档与接口定义`
- 文件数：1
- 有效内容行数：2281
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/prd-audit-fix-change-log-20260804.md
<!-- FILES-END -->

## M423 — HTML→PRD 承接台账、规则账本、决策账本与审计轮次记录一致性／文档与接口定义／第3段

- 聚合维度：`HTML→PRD 承接台账、规则账本、决策账本与审计轮次记录一致性／文档与接口定义`
- 文件数：1
- 有效内容行数：1692
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/prd-resync-change-log-20260805.md
<!-- FILES-END -->

## M424 — HTML→PRD 承接台账、规则账本、决策账本与审计轮次记录一致性／文档与接口定义／第4段

- 聚合维度：`HTML→PRD 承接台账、规则账本、决策账本与审计轮次记录一致性／文档与接口定义`
- 文件数：12
- 有效内容行数：2794
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/prd-sync-change-log-20260804.md
docs/r1-documentation-map-20260618.md
docs/r1-requirement-api-page-table-test-mapping-matrix-20260618.md
docs/r1-requirements-documentation-audit-report-20260618.md
docs/r2-code-audit-req26b-req21-req30-20260618.md
docs/r3-confirmed-gap-priorities-and-fix-backlog-20260618.md
docs/r4-remaining-work-backlog-20260618.md
docs/requirement-traceability-matrix-v0.1.md
docs/requirements-code-implementation-matrix-v0.1.csv
docs/prd-audit/CANONICAL_LEDGER_INDEX.md
docs/prd-audit/DEC-DATA-BACKEND-ACCURATE-001.md
docs/prd-audit/README.md
<!-- FILES-END -->

## M425 — HTML→PRD 承接台账、规则账本、决策账本与审计轮次记录一致性／文档与接口定义／第5段

- 聚合维度：`HTML→PRD 承接台账、规则账本、决策账本与审计轮次记录一致性／文档与接口定义`
- 文件数：1
- 有效内容行数：1236
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/prd-audit/authority-rule-ledger.csv
<!-- FILES-END -->

## M426 — HTML→PRD 承接台账、规则账本、决策账本与审计轮次记录一致性／文档与接口定义／第6段

- 聚合维度：`HTML→PRD 承接台账、规则账本、决策账本与审计轮次记录一致性／文档与接口定义`
- 文件数：1
- 有效内容行数：2582
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/prd-audit/business-decision-ledger.md
<!-- FILES-END -->

## M427 — HTML→PRD 承接台账、规则账本、决策账本与审计轮次记录一致性／文档与接口定义／第7段

- 聚合维度：`HTML→PRD 承接台账、规则账本、决策账本与审计轮次记录一致性／文档与接口定义`
- 文件数：2
- 有效内容行数：2236
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/prd-audit/document-scope-manifest.csv
docs/prd-audit/entry-registry.csv
<!-- FILES-END -->

## M428 — HTML→PRD 承接台账、规则账本、决策账本与审计轮次记录一致性／文档与接口定义／第8段

- 聚合维度：`HTML→PRD 承接台账、规则账本、决策账本与审计轮次记录一致性／文档与接口定义`
- 文件数：7
- 有效内容行数：1936
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/prd-audit/html-prd-coverage.csv
docs/prd-audit/rounds/r11/implementation-gaps.csv
docs/prd-audit/rounds/r11/invariant-registry.csv
docs/prd-audit/rounds/r11/consolidated/b-class-questions.md
docs/prd-audit/rounds/r14/findings.csv
docs/prd-audit/rounds/r14/rulings-b01.md
docs/prd-audit/rounds/r14/rulings-b02.md
<!-- FILES-END -->

## M429 — HTML→PRD 承接台账、规则账本、决策账本与审计轮次记录一致性／文档与接口定义／第9段

- 聚合维度：`HTML→PRD 承接台账、规则账本、决策账本与审计轮次记录一致性／文档与接口定义`
- 文件数：1
- 有效内容行数：2271
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/prd-audit/rounds/r7/html-map-bsm-1.md
<!-- FILES-END -->

## M430 — HTML→PRD 承接台账、规则账本、决策账本与审计轮次记录一致性／文档与接口定义／第10段

- 聚合维度：`HTML→PRD 承接台账、规则账本、决策账本与审计轮次记录一致性／文档与接口定义`
- 文件数：1
- 有效内容行数：3222
- 标记：OVERSIZE-SINGLE-FILE

<!-- FILES-BEGIN -->
docs/prd-audit/rounds/r7/html-map-bsm-2.md
<!-- FILES-END -->

## M431 — HTML→PRD 承接台账、规则账本、决策账本与审计轮次记录一致性／文档与接口定义／第11段

- 聚合维度：`HTML→PRD 承接台账、规则账本、决策账本与审计轮次记录一致性／文档与接口定义`
- 文件数：2
- 有效内容行数：2695
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/prd-audit/rounds/r7/html-map-pgm-enum.md
docs/prd-audit/rounds/r8/findings.csv
<!-- FILES-END -->

## M432 — HTML→PRD 承接台账、规则账本、决策账本与审计轮次记录一致性／文档与接口定义／第12段

- 聚合维度：`HTML→PRD 承接台账、规则账本、决策账本与审计轮次记录一致性／文档与接口定义`
- 文件数：2
- 有效内容行数：1046
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/superpowers/plans/2026-07-27-prd-audit.md
docs/superpowers/specs/2026-07-27-prd-audit-design.md
<!-- FILES-END -->

## M433 — 历史归档、废止文档与超期计划的引用完整性／文档与接口定义／第1段

- 聚合维度：`历史归档、废止文档与超期计划的引用完整性／文档与接口定义`
- 文件数：25
- 有效内容行数：2256
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/architecture-simplification-backlog-20260618.md
docs/code-implementation-goal-audit-fix-20260618.md
docs/codebase-architecture-zoom-out-audit-v0.1.md
docs/document-retention-merge-audit-20260610.md
docs/gstack-acceptance-ship-audit-v0.1.md
docs/p1-coding-completion-audit-v0.1.md
docs/p2-coding-candidate-audit-v0.1.md
docs/p2-current-diff-adversarial-review-v0.1.md
docs/p2-floor-plan-web-entry-completion-audit-v0.1.md
docs/p2-full-completion-audit-v0.1.md
docs/superpowers/plans/2026-06-14-p1-floor-plan-polygons.md
docs/superpowers/plans/2026-06-14-p1-integration-retry-compensation-reconciliation.md
docs/superpowers/plans/2026-06-14-p1-inventory-alerts-reconciliation.md
docs/superpowers/plans/2026-06-14-p1-merchant-billing-portal.md
docs/superpowers/plans/2026-06-14-p1-merchant-service-work-orders.md
docs/superpowers/plans/2026-06-14-p1-ticket-refund-line-ledger.md
docs/superpowers/plans/2026-06-15-coding-host-protection-baseline.md
docs/superpowers/plans/2026-06-15-p1-amusement-reservation-queue-light-workflow.md
docs/superpowers/plans/2026-06-15-p1-bar-bill-split-merge.md
docs/superpowers/plans/2026-06-15-p1-risk-alert-permission-hardening.md
docs/superpowers/plans/2026-06-15-p2-baseline-protection-and-web-entry.md
docs/superpowers/plans/2026-06-15-p2-floor-plan-web-entry.md
docs/superpowers/plans/2026-06-15-security-audit-fixes.md
docs/superpowers/plans/2026-06-15-t3-coding-package.md
docs/superpowers/plans/2026-06-15-t3-entry-assessment.md
<!-- FILES-END -->

## M434 — 历史归档、废止文档与超期计划的引用完整性／文档与接口定义／第2段

- 聚合维度：`历史归档、废止文档与超期计划的引用完整性／文档与接口定义`
- 文件数：3
- 有效内容行数：218
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/superpowers/plans/2026-06-15-t3a-readonly-analytics.md
docs/superpowers/plans/2026-06-15-t3b-supplier-mock-kit.md
docs/superpowers/plans/2026-06-15-t3d-local-llm-ai-quality.md
<!-- FILES-END -->

## M435 — 文档入口、统一口径基准、词表、索引与其余文档／配置与工程入口／第1段

- 聚合维度：`文档入口、统一口径基准、词表、索引与其余文档／配置与工程入口`
- 文件数：5
- 有效内容行数：538
- 标记：NORMAL

<!-- FILES-BEGIN -->
AGENTS.md
CONTEXT.md
PROGRESS.md
README.md
README.en.md
<!-- FILES-END -->

## M436 — 文档入口、统一口径基准、词表、索引与其余文档／文档与接口定义／第1段

- 聚合维度：`文档入口、统一口径基准、词表、索引与其余文档／文档与接口定义`
- 文件数：3
- 有效内容行数：1945
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/00-current-effective-document-entry-v0.1.md
docs/business-decision-log-v0.1.md
docs/business-functional-topology-v0.1.md
<!-- FILES-END -->

## M437 — 文档入口、统一口径基准、词表、索引与其余文档／文档与接口定义／第2段

- 聚合维度：`文档入口、统一口径基准、词表、索引与其余文档／文档与接口定义`
- 文件数：4
- 有效内容行数：2854
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/business-operations-layer-spec-v0.2.md
docs/delivery-package-manifest-v0.1.md
docs/documentation-map-v0.1.md
docs/final-open-decisions-and-owner-checklist-20260610.md
<!-- FILES-END -->

## M438 — 文档入口、统一口径基准、词表、索引与其余文档／文档与接口定义／第3段

- 聚合维度：`文档入口、统一口径基准、词表、索引与其余文档／文档与接口定义`
- 文件数：7
- 有效内容行数：2546
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/foundation-systems-breakdown.md
docs/master-roadmap.md
docs/original-requirements-capture-v0.1.md
docs/p0-coding-completion-and-handoff-report-v0.1.md
docs/p0-coding-entry-pack-v0.1.md
docs/p0-first-coding-package-v0.1.md
docs/p0-implementation-task-list.md
<!-- FILES-END -->

## M439 — 文档入口、统一口径基准、词表、索引与其余文档／文档与接口定义／第4段

- 聚合维度：`文档入口、统一口径基准、词表、索引与其余文档／文档与接口定义`
- 文件数：8
- 有效内容行数：2701
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/p0-user-story-map-and-sprint-slices-v0.1.md
docs/pending-development-backlog-v0.1.md
docs/pm-critic-and-acceptance-criteria-20260610.md
docs/product-documentation-index-v0.1.md
docs/product-requirements-v0.1.md
docs/project-progress-dashboard.md
docs/prototype-deprecation-and-technology-baseline-v0.1.md
docs/r5-development-implementation-backlog-20260618.md
<!-- FILES-END -->

## M440 — 文档入口、统一口径基准、词表、索引与其余文档／文档与接口定义／第5段

- 聚合维度：`文档入口、统一口径基准、词表、索引与其余文档／文档与接口定义`
- 文件数：4
- 有效内容行数：2948
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/scope-wide-coding-rule-canonical-V0.01.md
docs/system-breakdown.md
docs/system-capability-layers-product-index-v0.1.md
docs/terminology-canonical-v1.0.md
<!-- FILES-END -->

## M441 — 文档入口、统一口径基准、词表、索引与其余文档／文档与接口定义／第6段

- 聚合维度：`文档入口、统一口径基准、词表、索引与其余文档／文档与接口定义`
- 文件数：8
- 有效内容行数：1125
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/thread-registry.md
docs/unified-standards-baseline-v0.1.md
docs/v099-v124-feature-implementation-and-acceptance-spec-v0.1.md
docs/workstream-index.md
docs/agents/domain.md
docs/agents/issue-tracker.md
docs/agents/triage-labels.md
docs/pr/code-implementation-goal-pr-body.md
<!-- FILES-END -->

## M442 — 审计轮次记录、分片与非 canonical 快照／文档与接口定义／第1段

- 聚合维度：`审计轮次记录、分片与非 canonical 快照／文档与接口定义`
- 文件数：22
- 有效内容行数：2844
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/prd-audit/OTHER_15_FILES_FULL_SENTENCE_DIFF-2026-07-28.md
docs/prd-audit/PHASE-APPENDIX-A2-CLOSEOUT.md
docs/prd-audit/PHASE-B-DEFERRED-BATCH1.md
docs/prd-audit/PHASE-B-DEFERRED-BATCH2-SV.md
docs/prd-audit/PHASE-B-DEFERRED-BATCH3-SEAT.md
docs/prd-audit/PHASE-B-DEFERRED-BATCH4-ADM.md
docs/prd-audit/PHASE-B-DEFERRED-BATCH5-DATA.md
docs/prd-audit/PHASE-B-DEFERRED-BATCH6-BIZDAY.md
docs/prd-audit/PHASE-B-DEFERRED-COMPLETE.md
docs/prd-audit/PHASE-P2-PARTIAL-CLOSEOUT.md
docs/prd-audit/PHASE4-5-CLOSEOUT.md
docs/prd-audit/PHASE6-7-CLOSEOUT.md
docs/prd-audit/R12-LESSONS.md
docs/prd-audit/appendix-crosscheck-findings.csv
docs/prd-audit/appendix-deep-action-queue.csv
docs/prd-audit/appendix-deep-findings.csv
docs/prd-audit/appendix-deep-summary.json
docs/prd-audit/backup-current-diff.md
docs/prd-audit/backup-vs-main-FULL-SENTENCE-DIFF-2026-07-28.md
docs/prd-audit/bar-prd-backup-vs-main-FULL-DIFF.md
docs/prd-audit/document-scope-manifest.pre-b3.csv
docs/prd-audit/member-points-html-replacement-list-2026-07-28.md
<!-- FILES-END -->

## M443 — 审计轮次记录、分片与非 canonical 快照／文档与接口定义／第2段

- 聚合维度：`审计轮次记录、分片与非 canonical 快照／文档与接口定义`
- 文件数：25
- 有效内容行数：2228
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/prd-audit/member-points-rebuild-scope-2026-07-28.md
docs/prd-audit/member-points-related-doc-audit-2026-07-28.md
docs/prd-audit/method-b3-scope-and-tracks.md
docs/prd-audit/phase4-adversarial-findings.csv
docs/prd-audit/phase4-issue-rows.csv
docs/prd-audit/phase4-summary.json
docs/prd-audit/phase5-adversarial-findings.csv
docs/prd-audit/phase5-issue-rows.csv
docs/prd-audit/phase5-summary.json
docs/prd-audit/phase6-7-summary.json
docs/prd-audit/phase6-adversarial-findings.csv
docs/prd-audit/phase6-issue-rows.csv
docs/prd-audit/phase7-cross-domain-findings.csv
docs/prd-audit/prd-audit-report.md
docs/prd-audit/backup-diff-by-file-2026-07-28/FULL.md
docs/prd-audit/backup-diff-by-file-2026-07-28/README.md
docs/prd-audit/backup-diff-by-file-2026-07-28/amusement-business-operations-product-prd-v0.1.md.md
docs/prd-audit/backup-diff-by-file-2026-07-28/bar-business-operations-product-prd-v0.1.md.md
docs/prd-audit/backup-diff-by-file-2026-07-28/business-enum-quick-reference-v0.1.html.md
docs/prd-audit/backup-diff-by-file-2026-07-28/dividend-engine-prd-v0.1.md.md
docs/prd-audit/backup-diff-by-file-2026-07-28/finance-settlement-product-prd-v0.1.md.md
docs/prd-audit/backup-diff-by-file-2026-07-28/full-admin-mvp-requirements-code-matrix-v0.1.csv.md
docs/prd-audit/backup-diff-by-file-2026-07-28/full-admin-page-permission-field-matrix-v0.1.csv.md
docs/prd-audit/backup-diff-by-file-2026-07-28/full-admin-ui-acceptance-matrix-v0.1.md.md
docs/prd-audit/backup-diff-by-file-2026-07-28/group-governance-rebuild-prd-v1.0.md.md
<!-- FILES-END -->

## M444 — 审计轮次记录、分片与非 canonical 快照／文档与接口定义／第3段

- 聚合维度：`审计轮次记录、分片与非 canonical 快照／文档与接口定义`
- 文件数：25
- 有效内容行数：2729
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/prd-audit/backup-diff-by-file-2026-07-28/leasing-acquisition-prd-v1.0.md.md
docs/prd-audit/backup-diff-by-file-2026-07-28/operation-traceability-and-terminal-switch-prd-v0.1.md.md
docs/prd-audit/backup-diff-by-file-2026-07-28/platform-governance-prd-v0.1.md.md
docs/prd-audit/backup-diff-by-file-2026-07-28/product-requirements-v0.1.md.md
docs/prd-audit/backup-diff-by-file-2026-07-28/requirement-traceability-matrix-v0.1.md.md
docs/prd-audit/backup-diff-by-file-2026-07-28/terminal-page-product-prd-v0.1.md.md
docs/prd-audit/backup-diff-by-file-2026-07-28/ticketing-entitlement-checkin-product-prd-v0.1.md.md
docs/prd-audit/phases/01-baseline-and-backup.md
docs/prd-audit/phases/01b-scope-rework-b3.md
docs/prd-audit/phases/02-authority-rule-ledger.md
docs/prd-audit/phases/03-governance-and-common.md
docs/prd-audit/phases/03b-appendix-crosscheck.md
docs/prd-audit/phases/04-transaction-member-ticket-bar.md
docs/prd-audit/phases/05-realestate-leasing-building-supply.md
docs/prd-audit/phases/06-amusement-terminal-thirdparty-data.md
docs/prd-audit/phases/07-cross-domain-closure.md
docs/prd-audit/phases/08-appendix-deep-audit.md
docs/prd-audit/rounds/2026-07-27-b3-scope-crosscheck-report.md
docs/prd-audit/rounds/b6/worklog.md
docs/prd-audit/rounds/docfix/worklog.md
docs/prd-audit/rounds/final/code-round-input-money.md
docs/prd-audit/rounds/final/code-round-input-nonmoney.md
docs/prd-audit/rounds/final/worklog.md
docs/prd-audit/rounds/r10/b4-assert.log
docs/prd-audit/rounds/r10/b4-entry-anchors.log
<!-- FILES-END -->

## M445 — 审计轮次记录、分片与非 canonical 快照／文档与接口定义／第4段

- 聚合维度：`审计轮次记录、分片与非 canonical 快照／文档与接口定义`
- 文件数：13
- 有效内容行数：2749
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/prd-audit/rounds/r10/gate6-negative-test-A.log
docs/prd-audit/rounds/r10/gate6-negative-test-B.log
docs/prd-audit/rounds/r10/gate6-restored.log
docs/prd-audit/rounds/r10/p1-residual-assert.log
docs/prd-audit/rounds/r10/worklog.md
docs/prd-audit/rounds/r11/implementation-gaps-README.md
docs/prd-audit/rounds/r11/prd-scope-manifest.txt
docs/prd-audit/rounds/r11/value-flow-copy-baseline.txt
docs/prd-audit/rounds/r11/verdict.md
docs/prd-audit/rounds/r11/worklog.md
docs/prd-audit/rounds/r11/audit-shards/shard-01.md
docs/prd-audit/rounds/r11/audit-shards/shard-02.md
docs/prd-audit/rounds/r11/audit-shards/shard-03.md
<!-- FILES-END -->

## M446 — 审计轮次记录、分片与非 canonical 快照／文档与接口定义／第5段

- 聚合维度：`审计轮次记录、分片与非 canonical 快照／文档与接口定义`
- 文件数：6
- 有效内容行数：2812
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/prd-audit/rounds/r11/audit-shards/shard-04.md
docs/prd-audit/rounds/r11/audit-shards/shard-05.md
docs/prd-audit/rounds/r11/audit-shards/shard-06.md
docs/prd-audit/rounds/r11/audit-shards/shard-07a.md
docs/prd-audit/rounds/r11/audit-shards/shard-07b.md
docs/prd-audit/rounds/r11/audit-shards/shard-07c.md
<!-- FILES-END -->

## M447 — 审计轮次记录、分片与非 canonical 快照／文档与接口定义／第6段

- 聚合维度：`审计轮次记录、分片与非 canonical 快照／文档与接口定义`
- 文件数：10
- 有效内容行数：2799
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/prd-audit/rounds/r11/audit-shards/shard-08.md
docs/prd-audit/rounds/r11/consolidated/collisions.md
docs/prd-audit/rounds/r11/consolidated/d-class-open.md
docs/prd-audit/rounds/r11/consolidated/d01-resolution.md
docs/prd-audit/rounds/r11/consolidated/evidence-package.md
docs/prd-audit/rounds/r11/consolidated/findings-candidates.csv
docs/prd-audit/rounds/r11/consolidated/manifest.sha256
docs/prd-audit/rounds/r12-batch3/verify-shards/A.md
docs/prd-audit/rounds/r12-batch3/verify-shards/B.md
docs/prd-audit/rounds/r12-docbatch/verdict.md
<!-- FILES-END -->

## M448 — 审计轮次记录、分片与非 canonical 快照／文档与接口定义／第7段

- 聚合维度：`审计轮次记录、分片与非 canonical 快照／文档与接口定义`
- 文件数：9
- 有效内容行数：2887
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/prd-audit/rounds/r12-docbatch/verify-shards/C1.md
docs/prd-audit/rounds/r12-docbatch/verify-shards/C2.md
docs/prd-audit/rounds/r12-docbatch/verify-shards/C3.md
docs/prd-audit/rounds/r12-docbatch/verify-shards/C4.md
docs/prd-audit/rounds/r12-docbatch/verify-shards/C5.md
docs/prd-audit/rounds/r12-docbatch/verify-shards/C6.md
docs/prd-audit/rounds/r12-docbatch/verify-shards/C7-PGM0193.md
docs/prd-audit/rounds/r12-final/retrieval-audit.md
docs/prd-audit/rounds/r12-g1/CONSOLIDATED.md
<!-- FILES-END -->

## M449 — 审计轮次记录、分片与非 canonical 快照／文档与接口定义／第8段

- 聚合维度：`审计轮次记录、分片与非 canonical 快照／文档与接口定义`
- 文件数：14
- 有效内容行数：2923
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/prd-audit/rounds/r12-g1/PLAN.md
docs/prd-audit/rounds/r12-g1/verdict.md
docs/prd-audit/rounds/r12-g1/shards/S1.md
docs/prd-audit/rounds/r12-g1/shards/S2.md
docs/prd-audit/rounds/r12-g1/shards/S3.md
docs/prd-audit/rounds/r12-g1/shards/S4.md
docs/prd-audit/rounds/r12-g1/shards/S5.md
docs/prd-audit/rounds/r12-g1/shards/S6.md
docs/prd-audit/rounds/r12-g1/shards/S7.md
docs/prd-audit/rounds/r14/code-followup-baseline.txt
docs/prd-audit/rounds/r14/coverage.md
docs/prd-audit/rounds/r14/prd-scope-manifest.txt
docs/prd-audit/rounds/r14/consolidated/b01.md
docs/prd-audit/rounds/r14/consolidated/b02.md
<!-- FILES-END -->

## M450 — 审计轮次记录、分片与非 canonical 快照／文档与接口定义／第9段

- 聚合维度：`审计轮次记录、分片与非 canonical 快照／文档与接口定义`
- 文件数：9
- 有效内容行数：2949
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/prd-audit/rounds/r14/shards/b01-DE.md
docs/prd-audit/rounds/r14/shards/b01-FS.md
docs/prd-audit/rounds/r14/shards/b01-SR.md
docs/prd-audit/rounds/r14/shards/b02-SO.md
docs/prd-audit/rounds/r14/shards/b02-TK.md
docs/prd-audit/rounds/r14/shards/b02-TP.md
docs/prd-audit/rounds/r7/anchor-rebuild-a1.report.md
docs/prd-audit/rounds/r7/anchor-rebuild-a2.report.md
docs/prd-audit/rounds/r7/changelog-t6a.fragment.md
<!-- FILES-END -->

## M451 — 审计轮次记录、分片与非 canonical 快照／文档与接口定义／第10段

- 聚合维度：`审计轮次记录、分片与非 canonical 快照／文档与接口定义`
- 文件数：8
- 有效内容行数：2933
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/prd-audit/rounds/r7/changelog-t6b.fragment.md
docs/prd-audit/rounds/r7/changelog-t6c.fragment.md
docs/prd-audit/rounds/r7/diff-matrix-a.md
docs/prd-audit/rounds/r7/diff-matrix-b.md
docs/prd-audit/rounds/r7/gap25-cleanup.report.md
docs/prd-audit/rounds/r7/history-index.md
docs/prd-audit/rounds/r7/html-mirror-scan-bsm-enum.md
docs/prd-audit/rounds/r7/html-mirror-scan-pgm.md
<!-- FILES-END -->

## M452 — 审计轮次记录、分片与非 canonical 快照／文档与接口定义／第11段

- 聚合维度：`审计轮次记录、分片与非 canonical 快照／文档与接口定义`
- 文件数：25
- 有效内容行数：2908
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/prd-audit/rounds/r8/changelog-html-revision.md
docs/prd-audit/rounds/r8/changelog-html-revision-b2.md
docs/prd-audit/rounds/r8/changelog-imp1.md
docs/prd-audit/rounds/r8/changelog-imp2.md
docs/prd-audit/rounds/r8/changelog-rework.md
docs/prd-audit/rounds/r8/ledger-rebuild-report.md
docs/prd-audit/rounds/r8/reanchor-manual-review.csv
docs/prd-audit/rounds/r8/reanchor-text-drift.csv
docs/prd-audit/rounds/r9/ledger-source-refresh.csv
docs/prd-audit/rounds/r9/worklog.md
docs/prd-audit/rounds/regext/draft-01.jsonl
docs/prd-audit/rounds/regext/draft-02.jsonl
docs/prd-audit/rounds/regext/draft-03.jsonl
docs/prd-audit/rounds/regext/draft-04.jsonl
docs/prd-audit/rounds/regext/draft-05.jsonl
docs/prd-audit/rounds/regext/draft-06.jsonl
docs/prd-audit/rounds/regext/draft-07.jsonl
docs/prd-audit/rounds/regext/draft-08.jsonl
docs/prd-audit/rounds/regext/draft-09.jsonl
docs/prd-audit/rounds/regext/draft-10.jsonl
docs/prd-audit/rounds/regext/non-rule-dispositions.csv
docs/prd-audit/rounds/regext/shard-01.jsonl
docs/prd-audit/rounds/regext/shard-02.jsonl
docs/prd-audit/rounds/regext/shard-03.jsonl
docs/prd-audit/rounds/regext/shard-04.jsonl
<!-- FILES-END -->

## M453 — 审计轮次记录、分片与非 canonical 快照／文档与接口定义／第12段

- 聚合维度：`审计轮次记录、分片与非 canonical 快照／文档与接口定义`
- 文件数：8
- 有效内容行数：1408
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/prd-audit/rounds/regext/shard-05.jsonl
docs/prd-audit/rounds/regext/shard-06.jsonl
docs/prd-audit/rounds/regext/shard-07.jsonl
docs/prd-audit/rounds/regext/shard-08.jsonl
docs/prd-audit/rounds/regext/shard-09.jsonl
docs/prd-audit/rounds/regext/shard-10.jsonl
docs/prd-audit/rounds/regext/worklog.md
docs/prd-audit/rounds/sync1/worklog.md
<!-- FILES-END -->

## M454 — 企业微信会话合规链：永久保存、Webhook 签名防重放、AI 质检预留／后台实现／第1段

- 聚合维度：`企业微信会话合规链：永久保存、Webhook 签名防重放、AI 质检预留／后台实现`
- 文件数：25
- 有效内容行数：2079
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/compliance/WecomAccessAuditRecord.java
backend/src/main/java/com/complex/platform/compliance/JdbcWecomComplianceRepository.java
backend/src/main/java/com/complex/platform/compliance/WecomComplianceRepository.java
backend/src/main/java/com/complex/platform/compliance/WecomConversationRecord.java
backend/src/main/java/com/complex/platform/compliance/WecomConversationService.java
backend/src/main/java/com/complex/platform/compliance/WecomMessageRecord.java
backend/src/main/java/com/complex/platform/compliance/WecomMessageView.java
backend/src/main/java/com/complex/platform/compliance/WecomRetentionJob.java
backend/src/main/java/com/complex/platform/compliance/WecomRetentionPolicyRecord.java
backend/src/main/java/com/complex/platform/compliance/WecomRetentionRunResult.java
backend/src/main/java/com/complex/platform/compliance/WecomRiskAlertEnqueue.java
backend/src/main/java/com/complex/platform/compliance/WecomRiskHitRecord.java
backend/src/main/java/com/complex/platform/compliance/WecomRiskRuleRecord.java
backend/src/main/java/com/complex/platform/compliance/WecomSyncResult.java
backend/src/main/java/com/complex/platform/compliance/JdbcWecomWebhookNonceStore.java
backend/src/main/java/com/complex/platform/compliance/WecomWebhookNonceStore.java
backend/src/main/java/com/complex/platform/compliance/WecomWebhookSignatureVerifier.java
backend/src/main/java/com/complex/platform/compliance/dto/WecomConversationSyncRequest.java
backend/src/main/java/com/complex/platform/compliance/dto/WecomRetentionPolicyRequest.java
backend/src/main/java/com/complex/platform/compliance/dto/WecomRiskHitReviewRequest.java
backend/src/main/java/com/complex/platform/compliance/qc/DisabledWecomQc.java
backend/src/main/java/com/complex/platform/compliance/qc/ReservedWecomQc.java
backend/src/main/java/com/complex/platform/compliance/qc/WecomQcPort.java
backend/src/main/java/com/complex/platform/compliance/qc/WecomQcRequest.java
backend/src/main/java/com/complex/platform/compliance/qc/WecomQcResult.java
<!-- FILES-END -->

## M455 — 企业微信会话合规链：永久保存、Webhook 签名防重放、AI 质检预留／后台实现／第2段

- 聚合维度：`企业微信会话合规链：永久保存、Webhook 签名防重放、AI 质检预留／后台实现`
- 文件数：1
- 有效内容行数：88
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/compliance/qc/WecomQcRouter.java
<!-- FILES-END -->

## M456 — 企业微信会话合规链：永久保存、Webhook 签名防重放、AI 质检预留／数据库迁移／第1段

- 聚合维度：`企业微信会话合规链：永久保存、Webhook 签名防重放、AI 质检预留／数据库迁移`
- 文件数：5
- 有效内容行数：294
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V087__wecom_compliance.sql
backend/src/main/resources/db/migration/V096__wecom_ai_qc_reserved.sql
backend/src/main/resources/db/migration/V106__wecom_risk_hits_and_rules.sql
backend/src/main/resources/db/migration/V291__wecom_webhook_nonce.sql
backend/src/main/resources/db/migration/V295__wecom_webhook_nonce_fingerprint.sql
<!-- FILES-END -->

## M457 — 企业微信会话合规链：永久保存、Webhook 签名防重放、AI 质检预留／后台验证／第1段

- 聚合维度：`企业微信会话合规链：永久保存、Webhook 签名防重放、AI 质检预留／后台验证`
- 文件数：6
- 有效内容行数：1285
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/compliance/WecomComplianceJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/compliance/WecomConversationServiceTests.java
backend/src/test/java/com/complex/platform/compliance/WecomRiskHitsEndpointReproTests.java
backend/src/test/java/com/complex/platform/compliance/WecomWebhookNonceStoreJdbcIntegrationTests.java
backend/src/test/java/com/complex/platform/compliance/WecomWebhookSignatureVerifierTests.java
backend/src/test/java/com/complex/platform/compliance/qc/WecomQcRouterTests.java
<!-- FILES-END -->

## M458 — 企业微信会话合规链：永久保存、Webhook 签名防重放、AI 质检预留／网页与验证／第1段

- 聚合维度：`企业微信会话合规链：永久保存、Webhook 签名防重放、AI 质检预留／网页与验证`
- 文件数：2
- 有效内容行数：340
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/WecomComplianceReviewPage.tsx
apps/web/src/admin/pages/WecomComplianceReviewPage.guidedcreate.test.tsx
<!-- FILES-END -->

## M459 — 企业微信会话合规链：永久保存、Webhook 签名防重放、AI 质检预留／文档与接口定义／第1段

- 聚合维度：`企业微信会话合规链：永久保存、Webhook 签名防重放、AI 质检预留／文档与接口定义`
- 文件数：1
- 有效内容行数：95
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/r5-spec-T8-wecom-compliance-dev-design-20260618.md
<!-- FILES-END -->

## M460 — 合同准入闸链：AdmissionGuard、ContractScopeGuard 与 @GuardedAction 前置校验／后台实现／第1段

- 聚合维度：`合同准入闸链：AdmissionGuard、ContractScopeGuard 与 @GuardedAction 前置校验／后台实现`
- 文件数：4
- 有效内容行数：472
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/gate/JdbcAdmissionFactRepository.java
backend/src/main/java/com/complex/platform/gate/AdmissionGuard.java
backend/src/main/java/com/complex/platform/gate/AdmissionPolicyRecord.java
backend/src/main/java/com/complex/platform/gate/JdbcAdmissionPolicyRepository.java
<!-- FILES-END -->

## M461 — 合同准入闸链：AdmissionGuard、ContractScopeGuard 与 @GuardedAction 前置校验／数据库迁移／第1段

- 聚合维度：`合同准入闸链：AdmissionGuard、ContractScopeGuard 与 @GuardedAction 前置校验／数据库迁移`
- 文件数：1
- 有效内容行数：60
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V099__admission_over_scope_policies.sql
<!-- FILES-END -->

## M462 — 合同准入闸链：AdmissionGuard、ContractScopeGuard 与 @GuardedAction 前置校验／后台验证／第1段

- 聚合维度：`合同准入闸链：AdmissionGuard、ContractScopeGuard 与 @GuardedAction 前置校验／后台验证`
- 文件数：1
- 有效内容行数：419
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/gate/AdmissionGuardTests.java
<!-- FILES-END -->

## M463 — 合同准入闸链：AdmissionGuard、ContractScopeGuard 与 @GuardedAction 前置校验／网页与验证／第1段

- 聚合维度：`合同准入闸链：AdmissionGuard、ContractScopeGuard 与 @GuardedAction 前置校验／网页与验证`
- 文件数：2
- 有效内容行数：459
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/AdmissionAuditsPage.tsx
apps/web/src/admin/pages/AdmissionAuditsPage.test.tsx
<!-- FILES-END -->

## M464 — 合同准入闸链：AdmissionGuard、ContractScopeGuard 与 @GuardedAction 前置校验／文档与接口定义／第1段

- 聚合维度：`合同准入闸链：AdmissionGuard、ContractScopeGuard 与 @GuardedAction 前置校验／文档与接口定义`
- 文件数：1
- 有效内容行数：168
- 标记：NORMAL

<!-- FILES-BEGIN -->
docs/r5-spec-T2-T3-pre-action-gates-dev-design-20260618.md
<!-- FILES-END -->

## M465 — 卡座销售提成归属与费率治理／后台实现／第1段

- 聚合维度：`卡座销售提成归属与费率治理／后台实现`
- 文件数：3
- 有效内容行数：54
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/bar/BarCommissionCalculationRecord.java
backend/src/main/java/com/complex/platform/bar/BarCommissionRateConfigPort.java
backend/src/main/java/com/complex/platform/bar/CommissionCalculateRequest.java
<!-- FILES-END -->

## M466 — 卡座销售提成归属与费率治理／数据库迁移／第1段

- 聚合维度：`卡座销售提成归属与费率治理／数据库迁移`
- 文件数：1
- 有效内容行数：13
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V215__service_order_commission_idempotent.sql
<!-- FILES-END -->

## M467 — 卡座销售提成归属与费率治理／网页与验证／第1段

- 聚合维度：`卡座销售提成归属与费率治理／网页与验证`
- 文件数：5
- 有效内容行数：480
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/BarCommissionsPage.tsx
apps/web/src/admin/pages/BarCommissionsPage.kpi.test.tsx
apps/web/src/admin/pages/BarCommissionsPage.metricdetail.test.tsx
apps/web/src/admin/pages/barSalesRole.ts
apps/web/src/admin/pages/barSalesRole.test.ts
<!-- FILES-END -->

## M468 — 审批链引擎（组织层级逐级节点链）与人员范围变更／后台实现／第1段

- 聚合维度：`审批链引擎（组织层级逐级节点链）与人员范围变更／后台实现`
- 文件数：7
- 有效内容行数：503
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/java/com/complex/platform/access/dto/UserScopeChangeRequest.java
backend/src/main/java/com/complex/platform/access/org/ApprovalLineRequest.java
backend/src/main/java/com/complex/platform/access/org/OrgHierarchyController.java
backend/src/main/java/com/complex/platform/access/org/OrgHierarchyService.java
backend/src/main/java/com/complex/platform/access/org/OrgSuperiorRequest.java
backend/src/main/java/com/complex/platform/governance/chain/ApprovalChainService.java
backend/src/main/java/com/complex/platform/governance/chain/ApprovalChainNode.java
<!-- FILES-END -->

## M469 — 审批链引擎（组织层级逐级节点链）与人员范围变更／数据库迁移／第1段

- 聚合维度：`审批链引擎（组织层级逐级节点链）与人员范围变更／数据库迁移`
- 文件数：3
- 有效内容行数：73
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/main/resources/db/migration/V390__approval_chain_org_hierarchy.sql
backend/src/main/resources/db/migration/V391__approval_lines_seed.sql
backend/src/main/resources/db/migration/V392__access_grant_request_types_m087.sql
<!-- FILES-END -->

## M470 — 审批链引擎（组织层级逐级节点链）与人员范围变更／后台验证／第1段

- 聚合维度：`审批链引擎（组织层级逐级节点链）与人员范围变更／后台验证`
- 文件数：3
- 有效内容行数：385
- 标记：NORMAL

<!-- FILES-BEGIN -->
backend/src/test/java/com/complex/platform/access/org/OrgHierarchyServiceTests.java
backend/src/test/java/com/complex/platform/governance/chain/ApprovalChainNodeConcurrencyJdbcTests.java
backend/src/test/java/com/complex/platform/governance/chain/ApprovalChainServiceTests.java
<!-- FILES-END -->

## M471 — 审批链引擎（组织层级逐级节点链）与人员范围变更／网页与验证／第1段

- 聚合维度：`审批链引擎（组织层级逐级节点链）与人员范围变更／网页与验证`
- 文件数：4
- 有效内容行数：680
- 标记：NORMAL

<!-- FILES-BEGIN -->
apps/web/src/admin/pages/ApprovalChainPage.tsx
apps/web/src/admin/pages/ApprovalChainPage.test.tsx
apps/web/src/admin/pages/OrgHierarchyPage.tsx
apps/web/src/admin/pages/OrgHierarchyPage.test.tsx
<!-- FILES-END -->

## 本轮覆盖检查

- 当前受控范围减模块并集：空。
- 模块并集减当前受控范围：空。
- 重复主归属：空。
- 全部真实 UTF-8 路径可读取；未跟踪范围另见 untracked.txt。

## 原 322 模块交叉索引

| 原编号 | 原模块名称 | 本计划归属及文件数 |
|---|---|---|
| M001 | 00-authority-html-01 | M001（2） |
| M002 | 00-authority-html-02 | M002（1） |
| M003 | 01-root-config-and-guidance-01 | M003（1）、M400（1）、M417（1）、M435（5） |
| M004 | 01-root-config-and-guidance-02 | M004（1） |
| M005 | 01-root-config-and-guidance-03 | M003（1）、M005（1）、M362（2） |
| M006 | 10-backend-main-access-01 | M031（3）、M032（10）、M396（1） |
| M007 | 10-backend-main-access-02 | M032（13）、M033（11）、M040（1） |
| M008 | 10-backend-main-access-03 | M032（1）、M033（1）、M054（6）、M288（5）、M291（5） |
| M009 | 10-backend-main-access-04 | M054（11）、M288（5）、M291（6） |
| M010 | 10-backend-main-amusement-01 | M049（1）、M230（4）、M236（6）、M240（7）、M245（7） |
| M011 | 10-backend-main-amusement-02 | M230（3）、M236（1）、M240（5）、M245（16） |
| M012 | 10-backend-main-amusement-03 | M230（4）、M240（5）、M246（9） |
| M013 | 10-backend-main-amusement-04 | M240（5）、M245（1）、M246（2） |
| M014 | 10-backend-main-analytics-01 | M348（5）、M350（1） |
| M015 | 10-backend-main-analytics-02 | M086（1）、M348（1）、M350（6） |
| M016 | 10-backend-main-analytics-03 | M349（1） |
| M017 | 10-backend-main-analytics-04 | M098（2）、M310（1）、M318（1）、M350（4）、M351（2） |
| M018 | 10-backend-main-auth | M044（13） |
| M019 | 10-backend-main-bar-01 | M170（5）、M179（1）、M193（1）、M194（16）、M465（2） |
| M020 | 10-backend-main-bar-02 | M165（3）、M174（3）、M187（2）、M194（8）、M195（1） |
| M021 | 10-backend-main-bar-03 | M170（2）、M174（2）、M183（1）、M195（12） |
| M022 | 10-backend-main-bar-04 | M165（6）、M191（5）、M193（2）、M195（9）、M196（3） |
| M023 | 10-backend-main-bar-05 | M174（3）、M183（1）、M194（1）、M195（2）、M196（5）、M465（1） |
| M024 | 10-backend-main-bar-06 | M174（1）、M195（1）、M196（3） |
| M025 | 10-backend-main-common-01 | M026（1）、M049（7）、M084（5）、M086（1）、M396（11） |
| M026 | 10-backend-main-common-02 | M084（4）、M086（1） |
| M027 | 10-backend-main-compliance-01 | M262（8）、M454（17） |
| M028 | 10-backend-main-compliance-02 | M262（3）、M454（8）、M455（1） |
| M029 | 10-backend-main-config | M387（1）、M396（4） |
| M030 | 10-backend-main-contract-01 | M254（11）、M255（2） |
| M031 | 10-backend-main-contract-02 | M254（1）、M255（16） |
| M032 | 10-backend-main-customer-01 | M049（1）、M138（8）、M143（5）、M153（1）、M158（4）、M159（6） |
| M033 | 10-backend-main-customer-02 | M138（7）、M153（1）、M159（4） |
| M034 | 10-backend-main-customer-03 | M138（5）、M143（1）、M153（10）、M159（9） |
| M035 | 10-backend-main-customer-04 | M153（2） |
| M036 | 10-backend-main-dividend | M341（13） |
| M037 | 10-backend-main-energy | M278（13） |
| M038 | 10-backend-main-entry | M128（6） |
| M039 | 10-backend-main-evidence-01 | M092（19）、M098（6） |
| M040 | 10-backend-main-evidence-02 | M092（6）、M093（6）、M098（4） |
| M041 | 10-backend-main-facility-01 | M266（10）、M272（15） |
| M042 | 10-backend-main-facility-02 | M266（10）、M272（10）、M273（1） |
| M043 | 10-backend-main-finance-01 | M119（1）、M255（1）、M331（10） |
| M044 | 10-backend-main-finance-02 | M332（1）、M333（1） |
| M045 | 10-backend-main-finance-03 | M109（2）、M119（8）、M255（4）、M282（2）、M333（9） |
| M046 | 10-backend-main-finance-04 | M109（1）、M119（3）、M255（2）、M256（1）、M282（2）、M333（12） |
| M047 | 10-backend-main-floorplan | M015（17） |
| M048 | 10-backend-main-gate | M063（7）、M070（2）、M396（3）、M460（4） |
| M049 | 10-backend-main-governance-01 | M009（19）、M015（6） |
| M050 | 10-backend-main-governance-02 | M009（1）、M040（8） |
| M051 | 10-backend-main-integration-01 | M298（1）、M304（15）、M315（6） |
| M052 | 10-backend-main-integration-02 | M294（9）、M298（8）、M304（5）、M315（3） |
| M053 | 10-backend-main-integration-03 | M298（2）、M304（3） |
| M054 | 10-backend-main-inventory-01 | M208（4）、M223（2）、M225（6） |
| M055 | 10-backend-main-inventory-02 | M224（1）、M225（1） |
| M056 | 10-backend-main-inventory-03 | M208（1）、M218（4）、M225（2） |
| M057 | 10-backend-main-inventory-04 | M206（3）、M225（2） |
| M058 | 10-backend-main-inventory-05 | M206（6）、M208（4）、M213（4）、M218（6）、M225（2）、M226（3） |
| M059 | 10-backend-main-inventory-06 | M206（10）、M208（6）、M213（4）、M218（3）、M226（2） |
| M060 | 10-backend-main-inventory-07 | M208（1）、M218（6） |
| M061 | 10-backend-main-leasing | M249（17） |
| M062 | 10-backend-main-merchantbilling | M282（20） |
| M063 | 10-backend-main-merchantexpense | M342（11） |
| M064 | 10-backend-main-merchantportal | M020（4） |
| M065 | 10-backend-main-merchantservice | M266（5）、M267（10） |
| M066 | 10-backend-main-notification | M310（9） |
| M067 | 10-backend-main-ops | M075（9）、M405（11） |
| M068 | 10-backend-main-partner | M059（11） |
| M069 | 10-backend-main-platform-root | M396（1） |
| M070 | 10-backend-main-serviceorder | M201（13） |
| M071 | 10-backend-main-settlement-01 | M208（1）、M318（2）、M322（2）、M327（2）、M328（1） |
| M072 | 10-backend-main-settlement-02 | M318（2）、M322（4）、M327（12）、M328（1） |
| M073 | 10-backend-main-settlement-03 | M318（1）、M328（2） |
| M074 | 10-backend-main-system | M396（2） |
| M075 | 10-backend-main-terminal-01 | M063（14）、M070（8）、M372（3） |
| M076 | 10-backend-main-terminal-02 | M063（2）、M064（13）、M070（3）、M372（3） |
| M077 | 10-backend-main-ticketing-01 | M130（14）、M132（1）、M133（1） |
| M078 | 10-backend-main-ticketing-02 | M049（1）、M102（1）、M114（1）、M130（11）、M131（7）、M133（2）、M304（2） |
| M079 | 10-backend-main-ticketing-03 | M049（3）、M105（1）、M109（2）、M114（4）、M119（4）、M131（11） |
| M080 | 10-backend-main-ticketing-04 | M114（1）、M123（4）、M131（7）、M132（1）、M133（3） |
| M081 | 10-backend-main-ticketing-05 | M102（6）、M105（1）、M109（2）、M114（1）、M119（1）、M132（1）、M133（11）、M305（2） |
| M082 | 10-backend-main-ticketing-06 | M102（1）、M114（3） |
| M083 | 10-backend-main-voucher | M143（19）、M144（3） |
| M084 | 11-backend-test-access-01 | M035（2）、M036（2） |
| M085 | 11-backend-test-access-02 | M056（5） |
| M086 | 11-backend-test-amusement-01 | M242（3）、M248（2） |
| M087 | 11-backend-test-amusement-02 | M232（1） |
| M088 | 11-backend-test-analytics-01 | M353（3） |
| M089 | 11-backend-test-analytics-02 | M354（2） |
| M090 | 11-backend-test-analytics-03 | M355（4） |
| M091 | 11-backend-test-auth | M046（8） |
| M092 | 11-backend-test-bar-01 | M077（1）、M167（1）、M172（1）、M176（1）、M181（1）、M189（2）、M197（2） |
| M093 | 11-backend-test-bar-02 | M198（1） |
| M094 | 11-backend-test-bar-03 | M155（1）、M185（1） |
| M095 | 11-backend-test-common | M051（2）、M085（3）、M088（2）、M397（5） |
| M096 | 11-backend-test-compliance | M263（3）、M457（6） |
| M097 | 11-backend-test-config | M397（1） |
| M098 | 11-backend-test-contract | M258（5） |
| M099 | 11-backend-test-customer-01 | M085（1）、M140（2）、M161（3） |
| M100 | 11-backend-test-customer-02 | M140（1）、M155（2） |
| M101 | 11-backend-test-dividend | M344（6） |
| M102 | 11-backend-test-energy | M280（1） |
| M103 | 11-backend-test-entry | M129（1） |
| M104 | 11-backend-test-evidence | M095（6）、M100（1） |
| M105 | 11-backend-test-facility | M077（1）、M269（4）、M275（5） |
| M106 | 11-backend-test-finance-01 | M335（2） |
| M107 | 11-backend-test-finance-02 | M111（1）、M120（2）、M336（2） |
| M108 | 11-backend-test-finance-03 | M120（1）、M258（1）、M336（2） |
| M109 | 11-backend-test-floorplan | M017（4） |
| M110 | 11-backend-test-gate | M066（3）、M072（4）、M374（1）、M397（4）、M462（1） |
| M111 | 11-backend-test-governance | M011（3）、M017（1）、M042（3） |
| M112 | 11-backend-test-integration-01 | M296（1）、M316（3） |
| M113 | 11-backend-test-integration-02 | M317（1） |
| M114 | 11-backend-test-inventory-01 | M085（1）、M210（3）、M227（2） |
| M115 | 11-backend-test-inventory-02 | M220（1）、M228（1） |
| M116 | 11-backend-test-inventory-03 | M146（1）、M215（1）、M229（1） |
| M117 | 11-backend-test-leasing | M251（3） |
| M118 | 11-backend-test-merchantbilling | M284（7） |
| M119 | 11-backend-test-merchantexpense | M345（2） |
| M120 | 11-backend-test-merchantportal | M022（2） |
| M121 | 11-backend-test-merchantservice | M077（1）、M269（3） |
| M122 | 11-backend-test-notification | M312（1） |
| M123 | 11-backend-test-ops | M077（1）、M406（3） |
| M124 | 11-backend-test-partner | M061（3） |
| M125 | 11-backend-test-serviceorder | M203（4） |
| M126 | 11-backend-test-settlement-01 | M146（1）、M150（1）、M307（1）、M320（1）、M336（1）、M337（6） |
| M127 | 11-backend-test-settlement-02 | M320（3）、M324（3）、M337（2） |
| M128 | 11-backend-test-settlement-03 | M088（2）、M210（1）、M320（3）、M337（2）、M338（2） |
| M129 | 11-backend-test-settlement-04 | M258（1）、M338（2） |
| M130 | 11-backend-test-support | M085（1）、M383（4） |
| M131 | 11-backend-test-system | M383（1）、M397（1） |
| M132 | 11-backend-test-terminal | M066（4）、M067（1）、M072（1） |
| M133 | 11-backend-test-ticketing-01 | M051（3）、M116（2）、M120（1）、M134（3）、M150（1）、M189（1）、M233（2） |
| M134 | 11-backend-test-ticketing-02 | M085（2）、M116（2）、M125（1）、M134（2） |
| M135 | 11-backend-test-ticketing-03 | M135（1） |
| M136 | 11-backend-test-ticketing-04 | M136（1） |
| M137 | 11-backend-test-voucher | M146（5） |
| M138 | 12-backend-db-migrations-01 | M010（1）、M034（1）、M041（1）、M045（1）、M065（1）、M094（2）、M115（1）、M160（1）、M166（2）、M214（1）、M237（1）、M295（2）、M334（1）、M352（1）、M381（2） |
| M139 | 12-backend-db-migrations-02 | M016（2）、M027（1）、M041（1）、M106（2）、M145（1）、M175（1）、M180（2）、M184（1）、M209（1）、M231（1）、M237（1）、M241（1）、M257（1）、M268（1）、M283（1）、M306（2）、M334（1）、M352（2）、M373（1）、M381（1） |
| M140 | 12-backend-db-migrations-03 | M010（1）、M027（7）、M041（3）、M055（2）、M071（1）、M094（1）、M115（1）、M145（2）、M166（1）、M175（1）、M180（1）、M209（1）、M334（1）、M381（2） |
| M141 | 12-backend-db-migrations-04 | M034（1）、M055（1）、M065（1）、M139（1）、M154（1）、M166（1）、M175（1）、M180（1）、M188（2）、M209（2）、M219（1）、M231（2）、M237（1）、M257（1）、M299（1）、M334（2）、M352（1）、M381（3）、M456（1） |
| M142 | 12-backend-db-migrations-05 | M065（1）、M071（2）、M094（1）、M099（1）、M145（1）、M175（1）、M192（1）、M207（1）、M209（1）、M214（1）、M231（1）、M237（2）、M257（1）、M268（1）、M306（1）、M334（1）、M352（1）、M373（1）、M381（2）、M456（2）、M461（1） |
| M143 | 12-backend-db-migrations-06 | M021（1）、M071（1）、M094（1）、M110（2）、M175（2）、M180（1）、M207（1）、M209（2）、M219（2）、M231（2）、M237（3）、M247（2）、M334（3）、M381（2） |
| M144 | 12-backend-db-migrations-07 | M034（2）、M041（2）、M145（2）、M166（1）、M171（2）、M175（1）、M250（1）、M257（4）、M268（1）、M274（2）、M279（1）、M283（1）、M334（1）、M352（1）、M381（3） |
| M145 | 12-backend-db-migrations-08 | M016（1）、M027（1）、M034（1）、M041（1）、M050（2）、M060（4）、M071（1）、M094（1）、M139（4）、M154（2）、M268（1）、M283（1）、M299（2）、M334（3） |
| M146 | 12-backend-db-migrations-09 | M016（1）、M021（2）、M027（1）、M034（1）、M071（1）、M087（1）、M202（2）、M289（2）、M292（1）、M311（1）、M319（1）、M329（1）、M334（1）、M352（4）、M381（4）、M466（1） |
| M147 | 12-backend-db-migrations-10 | M027（3）、M050（3）、M087（1）、M115（1）、M202（1）、M247（1）、M289（1）、M292（4）、M319（2）、M329（2）、M352（1）、M381（5） |
| M148 | 12-backend-db-migrations-11 | M034（2）、M041（2）、M050（2）、M076（8）、M089（2）、M145（1）、M171（1）、M175（2）、M319（1）、M381（1）、M382（3） |
| M149 | 12-backend-db-migrations-12 | M041（1）、M110（1）、M115（1）、M124（1）、M145（1）、M149（1）、M154（3）、M209（1）、M219（2）、M257（1）、M306（1）、M343（2）、M382（7）、M456（2） |
| M150 | 12-backend-db-migrations-13 | M010（1）、M034（2）、M041（1）、M115（1）、M145（3）、M257（1）、M319（2）、M323（4）、M329（2）、M343（5）、M352（1）、M373（1）、M382（1） |
| M151 | 12-backend-db-migrations-14 | M010（1）、M021（1）、M041（1）、M050（3）、M071（1）、M076（1）、M145（2）、M175（2）、M209（1）、M257（1）、M306（1）、M319（4）、M323（1）、M329（3）、M382（2） |
| M152 | 12-backend-db-migrations-15 | M021（1）、M050（1）、M089（1）、M106（1）、M149（2）、M209（1）、M306（1）、M319（7）、M323（1）、M329（2）、M382（1） |
| M153 | 13-backend-resource-root | M395（3） |
| M154 | 14-backend-build-config | M395（3） |
| M155 | 20-web-admin-page-cross-domain-01 | M038（9）、M080（2）、M238（1）、M243（3）、M356（3）、M463（2） |
| M156 | 20-web-admin-page-cross-domain-02 | M080（3）、M096（1）、M234（8）、M238（1）、M243（3）、M356（1） |
| M157 | 20-web-admin-page-cross-domain-03 | M080（5）、M096（1）、M168（1）、M173（3）、M177（3）、M467（3） |
| M158 | 20-web-admin-page-cross-domain-04 | M080（4）、M168（1）、M259（2）、M264（1）、M356（5） |
| M159 | 20-web-admin-page-cross-domain-05 | M078（2）、M080（3）、M107（2）、M141（2）、M259（1）、M281（5） |
| M160 | 20-web-admin-page-cross-domain-06 | M080（3）、M276（2）、M281（1）、M339（3）、M346（2） |
| M161 | 20-web-admin-page-cross-domain-07 | M012（2）、M018（3）、M081（5）、M285（2）、M300（2）、M308（1）、M346（2）、M356（1） |
| M162 | 20-web-admin-page-cross-domain-08 | M081（2）、M151（2）、M211（2）、M221（2）、M252（1）、M300（1）、M308（3） |
| M163 | 20-web-admin-page-cross-domain-09 | M023（4）、M081（2）、M141（2）、M252（3）、M270（2）、M285（2）、M346（1） |
| M164 | 20-web-admin-page-cross-domain-10 | M023（2）、M081（2）、M270（2）、M285（2）、M313（2）、M346（1）、M356（8） |
| M165 | 20-web-admin-page-cross-domain-11 | M012（2）、M038（3）、M081（2）、M293（2）、M357（8）、M407（2） |
| M166 | 20-web-admin-page-cross-domain-12 | M081（5）、M101（2）、M121（2）、M204（3）、M325（1）、M339（3）、M357（1） |
| M167 | 20-web-admin-page-cross-domain-13 | M018（2）、M057（2）、M081（1）、M290（1）、M293（3）、M313（1）、M325（1）、M330（5） |
| M168 | 20-web-admin-page-cross-domain-14 | M012（2）、M043（2）、M068（1）、M081（3）、M156（2）、M216（3）、M290（3）、M357（2） |
| M169 | 20-web-admin-page-cross-domain-15 | M073（1）、M081（2）、M103（3）、M112（5） |
| M170 | 20-web-admin-page-cross-domain-16 | M062（2）、M081（1）、M082（3）、M103（1）、M270（2）、M276（2） |
| M171 | 20-web-admin-page-cross-domain-17 | M018（2）、M062（1）、M082（1）、M147（1）、M270（5）、M458（2）、M467（2） |
| M172 | 21-web-customer-page-cross-domain | M112（3）、M126（2）、M147（5）、M156（2）、M162（5） |
| M173 | 22-web-admin-client | M365（2） |
| M174 | 22-web-admin-components-01 | M080（5）、M365（5）、M366（15） |
| M175 | 22-web-admin-components-02 | M366（1） |
| M176 | 22-web-admin-format | M366（1） |
| M177 | 22-web-admin-hooks | M366（5） |
| M178 | 22-web-admin-i18n | M090（3） |
| M179 | 22-web-admin-root | M365（4） |
| M180 | 22-web-admin-security | M038（3）、M073（2） |
| M181 | 23-web-customer-client | M162（3） |
| M182 | 23-web-customer-components | M117（2）、M162（1） |
| M183 | 23-web-customer-i18n | M090（2） |
| M184 | 23-web-customer-root | M162（2） |
| M185 | 24-web-shared-root | M363（3）、M364（1） |
| M186 | 24-web-shared-shared | M090（2） |
| M187 | 24-web-shared-test | M367（1） |
| M188 | 25-web-build-config | M361（4）、M363（8） |
| M189 | 26-android-app-main | M091（1）、M375（7） |
| M190 | 27-android-terminal-android-app | M375（4） |
| M191 | 27-android-terminal-root | M375（4） |
| M192 | 27-android-terminal-src-01 | M375（3）、M376（7） |
| M193 | 27-android-terminal-src-02 | M376（1） |
| M194 | 30-docs-openapi-01 | M391（1） |
| M195 | 30-docs-openapi-02 | M392（1） |
| M196 | 31-docs-prd-audit-round-2026-07-27-b3-scope-crosscheck-report-md | M444（1） |
| M197 | 31-docs-prd-audit-round-b6 | M444（1） |
| M198 | 31-docs-prd-audit-round-docfix | M444（1） |
| M199 | 31-docs-prd-audit-round-final | M444（3） |
| M200 | 31-docs-prd-audit-round-r10 | M444（2）、M445（5） |
| M201 | 31-docs-prd-audit-round-r11-01 | M445（3）、M446（2） |
| M202 | 31-docs-prd-audit-round-r11-02 | M446（4）、M447（1） |
| M203 | 31-docs-prd-audit-round-r11-03 | M428（3）、M445（5）、M447（6） |
| M204 | 31-docs-prd-audit-round-r12-batch3 | M447（2） |
| M205 | 31-docs-prd-audit-round-r12-docbatch | M447（1）、M448（7） |
| M206 | 31-docs-prd-audit-round-r12-final | M448（1） |
| M207 | 31-docs-prd-audit-round-r12-g1 | M448（1）、M449（9） |
| M208 | 31-docs-prd-audit-round-r14-01 | M428（3）、M449（5）、M450（4） |
| M209 | 31-docs-prd-audit-round-r14-02 | M450（2） |
| M210 | 31-docs-prd-audit-round-r7-01 | M450（3）、M451（6） |
| M211 | 31-docs-prd-audit-round-r7-02 | M429（1） |
| M212 | 31-docs-prd-audit-round-r7-03 | M430（1） |
| M213 | 31-docs-prd-audit-round-r7-04 | M431（1）、M451（1） |
| M214 | 31-docs-prd-audit-round-r7-05 | M451（1） |
| M215 | 31-docs-prd-audit-round-r8 | M431（1）、M452（8） |
| M216 | 31-docs-prd-audit-round-r9 | M452（2） |
| M217 | 31-docs-prd-audit-round-regext | M452（15）、M453（7） |
| M218 | 31-docs-prd-audit-round-sync1 | M453（1） |
| M219 | 32-docs-prd-audit-backup-diff-by-file-2026-07-28 | M443（11）、M444（7） |
| M220 | 32-docs-prd-audit-bar | M442（1） |
| M221 | 32-docs-prd-audit-compliance | M443（2） |
| M222 | 32-docs-prd-audit-customer | M442（1）、M443（1） |
| M223 | 32-docs-prd-audit-entry | M427（1） |
| M224 | 32-docs-prd-audit-phases | M444（10） |
| M225 | 32-docs-prd-audit-root-01 | M424（3）、M425（1）、M442（18） |
| M226 | 32-docs-prd-audit-root-02 | M442（1） |
| M227 | 32-docs-prd-audit-root-03 | M426（1） |
| M228 | 32-docs-prd-audit-root-04 | M427（1）、M428（1）、M442（1）、M443（11） |
| M229 | 32-docs-prd-audit-settlement | M321（1） |
| M230 | 33-docs-production-readiness-owner-execution-packets | M411（5） |
| M231 | 33-docs-production-readiness-owner-guides | M411（5） |
| M232 | 33-docs-production-readiness-root-01 | M401（4）、M409（9） |
| M233 | 33-docs-production-readiness-root-02 | M410（8） |
| M234 | 33-docs-production-readiness-root-03 | M411（4） |
| M235 | 33-docs-production-readiness-submission-manifest-templates-01 | M411（11）、M412（14） |
| M236 | 33-docs-production-readiness-submission-manifest-templates-02 | M412（11）、M413（4） |
| M237 | 33-docs-production-readiness-submissions | M413（2） |
| M238 | 34-docs-plans-access | M039（1） |
| M239 | 34-docs-plans-amusement | M239（1）、M244（2）、M433（1） |
| M240 | 34-docs-plans-analytics | M358（1）、M359（1）、M434（1） |
| M241 | 34-docs-plans-auth | M047（2）、M169（1） |
| M242 | 34-docs-plans-bar | M169（1）、M178（1）、M182（1）、M186（1）、M433（1） |
| M243 | 34-docs-plans-compliance | M265（1）、M432（1）、M433（1） |
| M244 | 34-docs-plans-contract | M393（1） |
| M245 | 34-docs-plans-cross-domain | M398（2）、M401（2）、M433（3）、M434（1） |
| M246 | 34-docs-plans-customer | M113（1）、M142（2）、M148（1） |
| M247 | 34-docs-plans-entry | M433（3） |
| M248 | 34-docs-plans-evidence | M097（1）、M398（1） |
| M249 | 34-docs-plans-finance | M340（1）、M433（1） |
| M250 | 34-docs-plans-floorplan | M019（1）、M433（1） |
| M251 | 34-docs-plans-governance | M433（1） |
| M252 | 34-docs-plans-integration | M297（1）、M433（1） |
| M253 | 34-docs-plans-inventory | M217（1）、M433（1）、M434（1） |
| M254 | 34-docs-plans-merchantbilling | M433（1） |
| M255 | 34-docs-plans-ops | M401（1） |
| M256 | 34-docs-plans-ticketing | M108（1）、M118（1） |
| M257 | 35-docs-superpowers-reviews | M186（2）、M418（1） |
| M258 | 35-docs-superpowers-specs | M142（1）、M359（1）、M432（1） |
| M259 | 36-docs-agents | M441（3） |
| M260 | 36-docs-ops | M047（1） |
| M261 | 36-docs-pr | M441（1） |
| M262 | 36-docs-proposals | M148（1）、M222（1） |
| M263 | 36-docs-supplier-mock | M302（3） |
| M264 | 37-docs-domain-access | M058（2）、M301（2） |
| M265 | 37-docs-domain-amusement | M235（3）、M239（1）、M244（3） |
| M266 | 37-docs-domain-analytics | M358（4）、M439（1） |
| M267 | 37-docs-domain-auth | M169（1）、M297（1） |
| M268 | 37-docs-domain-bar | M178（1）、M182（2）、M199（2） |
| M269 | 37-docs-domain-compliance-01 | M028（2）、M052（1）、M083（1）、M261（1）、M370（2）、M388（1）、M408（3）、M418（2）、M421（4）、M433（4） |
| M270 | 37-docs-domain-compliance-02 | M019（1）、M108（1）、M265（1）、M301（1）、M358（1）、M422（1）、M433（4） |
| M271 | 37-docs-domain-compliance-03 | M301（2）、M385（1）、M408（1）、M424（2）、M459（1） |
| M272 | 37-docs-domain-contract-01 | M261（1）、M388（1） |
| M273 | 37-docs-domain-contract-02 | M389（1） |
| M274 | 37-docs-domain-contract-03 | M019（2）、M108（2）、M122（1）、M148（1）、M212（1）、M271（1）、M287（1）、M301（1）、M309（1）、M358（1）、M390（2）、M424（1） |
| M275 | 37-docs-domain-customer | M142（3）、M148（1）、M157（2）、M164（2） |
| M276 | 37-docs-domain-dividend | M347（1） |
| M277 | 37-docs-domain-entry | M301（1）、M436（1）、M438（1） |
| M278 | 37-docs-domain-evidence | M097（3）、M408（11）、M409（2） |
| M279 | 37-docs-domain-finance | M340（2） |
| M280 | 37-docs-domain-floorplan | M301（1）、M424（1）、M437（1）、M439（1） |
| M281 | 37-docs-domain-governance-01 | M013（3）、M039（1）、M370（1） |
| M282 | 37-docs-domain-governance-02 | M014（1） |
| M283 | 37-docs-domain-integration | M301（2）、M302（2） |
| M284 | 37-docs-domain-inventory | M190（1）、M212（3）、M301（2）、M314（1） |
| M285 | 37-docs-domain-leasing | M253（1） |
| M286 | 37-docs-domain-merchantportal | M024（1） |
| M287 | 37-docs-domain-ops | M006（2）、M074（1）、M340（1）、M401（3）、M408（1） |
| M288 | 37-docs-domain-settlement | M104（1）、M152（1）、M326（1） |
| M289 | 37-docs-domain-terminal-01 | M069（1）、M377（2） |
| M290 | 37-docs-domain-terminal-02 | M378（3） |
| M291 | 37-docs-domain-ticketing | M104（3）、M118（1） |
| M292 | 37-docs-domain-voucher | M148（1） |
| M293 | 38-docs-design-01 | M013（1）、M398（1）、M421（1）、M433（1）、M437（1） |
| M294 | 38-docs-design-02 | M079（1）、M358（2）、M421（1） |
| M295 | 38-docs-design-03 | M384（1） |
| M296 | 38-docs-design-04 | M039（1）、M169（1）、M222（1）、M301（1）、M370（1）、M398（1）、M464（1） |
| M297 | 38-docs-design-05 | M039（1）、M127（1）、M340（1）、M398（2）、M441（1） |
| M298 | 38-docs-general-01 | M137（1）、M200（1）、M436（2） |
| M299 | 38-docs-general-02 | M301（1）、M370（1）、M401（1）、M408（4）、M437（2）、M438（1） |
| M300 | 38-docs-general-03 | M370（7）、M385（1）、M401（3）、M438（3） |
| M301 | 38-docs-general-04 | M137（1）、M408（2）、M433（1）、M438（2）、M439（3） |
| M302 | 38-docs-general-05 | M301（1）、M340（1）、M408（2）、M424（4）、M439（3） |
| M303 | 38-docs-general-06 | M358（1）、M440（3） |
| M304 | 38-docs-general-07 | M370（1）、M440（1）、M441（3） |
| M305 | 38-docs-prd-01 | M277（1）、M423（1） |
| M306 | 38-docs-prd-02 | M205（1）、M261（1）、M424（1） |
| M307 | 40-ops-160-01 | M379（1）、M402（24） |
| M308 | 40-ops-160-02 | M007（4）、M025（1）、M402（1）、M403（1） |
| M309 | 40-ops-ci | M007（1） |
| M310 | 40-ops-keyvault | M048（7） |
| M311 | 41-tools-gate-hardening-tests | M008（10） |
| M312 | 41-tools-root-01 | M029（2）、M303（1）、M394（3）、M404（1）、M414（2）、M419（1） |
| M313 | 41-tools-root-02 | M394（2）、M404（1）、M414（7） |
| M314 | 41-tools-root-03 | M008（1）、M303（1）、M371（1）、M415（7） |
| M315 | 41-tools-root-04 | M008（4）、M303（2）、M371（2）、M380（3）、M394（1）、M399（1）、M404（2）、M415（2）、M419（1） |
| M316 | 41-tools-root-05 | M008（1）、M053（1）、M386（2）、M394（3）、M404（6）、M416（9） |
| M317 | 41-tools-root-06 | M029（2）、M303（2）、M360（1）、M416（3） |
| M318 | 42-tests-e2e-01 | M163（3）、M260（1）、M368（8） |
| M319 | 42-tests-e2e-02 | M163（1）、M286（1）、M368（3）、M369（1） |
| M320 | 43-security-scans | M030（1）、M420（12） |
| M321 | 44-github-root | M005（1） |
| M322 | 44-github-workflows | M005（1） |

## 历史编号交叉索引

旧 87 模块来自另一套历史计划。以下只按真实文件建立索引；旧编号、状态和审计要求不替代本计划。通配符按当前受控范围展开。

| 旧编号 | 旧模块名称 | 对应当前模块及文件数 | 当前范围外的旧条目 |
|---|---|---|---|
| M001 | 治理门禁与 CI 基线 | M003（2）、M004（1）、M005（3）、M006（2）、M007（5）、M008（17） | 无 |
| M002 | 综合体/集团/项目公司/业态主体拓扑与四类主体绑定 | M009（20）、M010（4）、M011（3）、M012（6）、M013（4）、M014（1） | 无 |
| M003 | 空间点位、楼层平面图与营业时段 | M015（23）、M016（4）、M017（5）、M018（7）、M019（4） | 无 |
| M004 | 商户档案、商户类型与商户侧门户 | M020（4）、M021（5）、M022（2）、M023（6）、M024（1）、M025（1） | 无 |
| M005 | 租户范围与数据隔离（技术不变量） | M026（1）、M027（13）、M028（2）、M029（4）、M030（1） | 无 |
| M006 | 功能权限点、职级三十档、权限模板与最终权限计算 | M031（3）、M032（25）、M033（12）、M034（10）、M035（2）、M036（3）、M037（1）、M038（15）、M039（4） | 无 |
| M007 | 高危授权审批、临时授权、角色装配重算与主管复核链 | M040（9）、M041（14）、M042（3）、M043（2） | 无 |
| M008 | 登录会话、生产凭证守卫与破窗紧急恢复（U 盘密钥） | M044（13）、M045（1）、M046（8）、M047（3）、M048（7） | 无 |
| M009 | 个人信息保护：PII 加密、敏感查看审批、人脸/生物信息与同意 | M049（13）、M050（12）、M051（5）、M052（1）、M053（1） | 无 |
| M010 | 员工档案、注册审批与工作手机 BYOD | M054（17）、M055（3）、M056（5）、M057（2）、M058（2） | 无 |
| M011 | 外包/服务商人员与审计人员边界 | M059（11）、M060（4）、M061（3）、M062（3） | 无 |
| M012 | 设备台账、领用状态与 MDM 管控 | M063（23）、M064（13）、M065（3）、M066（7）、M067（1）、M068（1）、M069（1） | 无 |
| M013 | 十一类终端开关、优先级、缺行默认关闭与 canary | M070（13）、M071（7）、M072（5）、M073（3）、M074（1） | 无 |
| M014 | 存档、软删除审批、删前占用与集中清理 | M075（9）、M076（9）、M077（4）、M078（2）、M079（1） | 无 |
| M015 | 后台分步引导新建与全链可点击溯源 | M080（25）、M081（25）、M082（4）、M083（1） | 无 |
| M016 | 幂等、并发先成功与限流 | M084（9）、M085（8） | 无 |
| M017 | 金额精确到分、尾差归属与经营日切/本地时区 | M086（3）、M087（2）、M088（4） | 无 |
| M018 | 界面全中文与枚举展示词典 | M089（3）、M090（7）、M091（1） | 无 |
| M019 | 操作留痕、证据事件、附件双证据与责任链 | M092（25）、M093（6）、M094（6）、M095（6）、M096（2）、M097（4） | 无 |
| M020 | 风险分级、风险告警闭环与复核队列 | M098（12）、M099（1）、M100（1）、M101（2） | 无 |
| M021 | 票种、通票、权益模板与资格判定 | M102（8）、M103（4）、M104（4） | 无 |
| M022 | 票价规则、动态定价、会员价与套票 | M105（2）、M106（3）、M107（2）、M108（4） | 无 |
| M023 | 购票订单、支付意向、出票与订单过期 | M109（7）、M110（3）、M111（1）、M112（8）、M113（1） | 无 |
| M024 | 动态二维码凭证、在线核销、多人同场次与冲正 | M114（10）、M115（5）、M116（4）、M117（2）、M118（2） | 无 |
| M025 | 退款规则、有效期到期与退款分级授权 | M119（17）、M120（4）、M121（2）、M122（1） | 无 |
| M026 | 门票转赠、多人票/一人多次票与反向流程 | M123（4）、M124（1）、M125（1）、M126（2）、M127（1） | 无 |
| M027 | 入场三道环节：安检、人脸验龄与检票 | M128（6）、M129（1） | 无 |
| M028 | 票务核心服务、控制器与其余 | M130（25）、M131（25）、M132（3）、M133（17）、M134（5）、M135（1）、M136（1）、M137（2） | 无 |
| M029 | 会员身份、注册认证、成长值等级、签到生日与账号状态 | M138（20）、M139（5）、M140（3）、M141（4）、M142（6） | 无 |
| M030 | 积分账户、待追回积分、积分商城与虚拟券 | M143（25）、M144（3）、M145（13）、M146（7）、M147（6）、M148（5） | 无 |
| M031 | 商户积分结算、内部结算价目录与零售参考价 | M149（3）、M150（2）、M151（2）、M152（1） | 无 |
| M032 | 储值卡与余额（含商户卡资金归属） | M153（14）、M154（6）、M155（5）、M156（4）、M157（2） | 无 |
| M033 | 客人端核心与其余 | M158（4）、M159（19）、M160（1）、M161（3）、M162（12）、M163（4）、M164（2） | 无 |
| M034 | 台位状态、订台定金、邀请函与转台/并台 | M165（9）、M166（5）、M167（1）、M168（2）、M169（4） | 无 |
| M035 | 平板图形点单 POS、商品目录、套餐与优惠券 | M170（7）、M171（3）、M172（1）、M173（3） | 无 |
| M036 | 点酒订单、出品票据、沽清、退酒与按杯出酒损耗 | M174（9）、M175（11）、M176（1）、M177（3）、M178（2） | 无 |
| M037 | 账单结算、拆合单、小费、免单/调账/反结审批 | M179（1）、M180（5）、M181（1）、M182（3） | 无 |
| M038 | 正式未付款订单挂起、应收挂账与审计结案 | M183（2）、M184（1）、M185（1）、M186（3） | 无 |
| M039 | 权益兑付、赠送成本与礼品核销 | M187（2）、M188（2）、M189（3）、M190（1） | 无 |
| M040 | 班次交班与收银对账 | M191（5）、M192（1） | 无 |
| M041 | 酒吧核心门面、控制器与其余 | M193（3）、M194（25）、M195（25）、M196（11）、M197（2）、M198（1）、M199（2）、M200（1） | 无 |
| M042 | 服务点单：点单、公司统一收款、名录分账与退款 | M201（13）、M202（3）、M203（4）、M204（3）、M205（1） | 无 |
| M043 | 供应商资质、供应合同与采购要货 | M206（19）、M207（2） | 无 |
| M044 | 库房、库存类型、流水、盘点报损与高价值复核 | M208（17）、M209（10）、M210（4）、M211（2）、M212（4） | 无 |
| M045 | 客人存酒、取酒、作废与到期提醒 | M213（8）、M214（2）、M215（1）、M216（3）、M217（1） | 无 |
| M046 | 跨主体流转、外部经销商准入、经销订单/退货红冲与扫码交接链 | M218（19）、M219（5）、M220（1）、M221（2）、M222（2） | 无 |
| M047 | 库存核心控制器、服务与其余 | M223（2）、M224（1）、M225（13）、M226（5）、M227（2）、M228（1）、M229（1） | 无 |
| M048 | 游乐项目、场次、实时在玩人数容量与入场核销 | M230（11）、M231（6）、M232（1）、M233（2）、M234（8）、M235（3） | 无 |
| M049 | 开放前安全巡检、严重度四档、整改复检主管确认与检修记录 | M236（7）、M237（8）、M238（2）、M239（2） | 无 |
| M050 | 使用限制、团体/包场预订、事故与免责 | M240（22）、M241（1）、M242（3）、M243（6）、M244（5） | 无 |
| M051 | 游乐核心与其余 | M245（24）、M246（11）、M247（3）、M248（2） | 无 |
| M052 | 招商线索、意向、品牌库与签约转商户 | M249（17）、M250（1）、M251（3）、M252（4）、M253（1） | 无 |
| M053 | 合同履约、审批链、变更、退租结算、租金与营业额抽成 | M254（12）、M255（25）、M256（1）、M257（10）、M258（7）、M259（3）、M260（1）、M261（3） | 无 |
| M054 | 证照资质保险到期预警、阻断与宽限期 | M262（11）、M263（3）、M264（1）、M265（2） | 无 |
| M055 | 物业工单、SLA 矩阵、外包进场报备与商户服务工单 | M266（25）、M267（10）、M268（4）、M269（7）、M270（11）、M271（1） | 无 |
| M056 | 楼宇机电设备台账、维保、告警与楼宇巡检 | M272（25）、M273（1）、M274（2）、M275（5）、M276（4）、M277（1） | 无 |
| M057 | 能耗计量、抄表复核与能源费归集 | M278（13）、M279（1）、M280（1）、M281（6） | 无 |
| M058 | 物业费按面积计费、商户综合账单、催收与争议 | M282（24）、M283（3）、M284（7）、M285（6）、M286（1）、M287（1） | 无 |
| M059 | 排班、考勤打卡与请假 | M288（10）、M289（3）、M290（4） | 无 |
| M060 | 薪资月结、税档与提成发放治理 | M291（11）、M292（5）、M293（5） | 无 |
| M061 | 开放接口：鉴权、签名、防重放、动态端点与开放应用状态机 | M294（9）、M295（2）、M296（1）、M297（2） | 无 |
| M062 | 第三方能力开关、供应商适配器边界、脚手架与 mock 策略 | M298（11）、M299（3）、M300（3）、M301（15）、M302（5）、M303（6） | 无 |
| M063 | 渠道订单回流、Webhook 幂等重试补偿与渠道对账差异 | M304（25）、M305（2）、M306（6）、M307（1）、M308（4）、M309（1） | 无 |
| M064 | 短信/通知开关、提醒窗口与重要事项通知 | M310（10）、M311（1）、M312（1）、M313（3）、M314（1） | 无 |
| M065 | 第三方接入核心与其余 | M315（9）、M316（3）、M317（1） | 无 |
| M066 | 收入归属、业态主体分账与通票分账瀑布 | M318（6）、M319（17）、M320（7）、M321（1） | 无 |
| M067 | 结算配置中心、结算类型与配置版本化 | M322（6）、M323（6）、M324（3）、M325（2）、M326（1） | 无 |
| M068 | 结算单生成、月结作业、过期、冲正、复核与发票 | M327（14）、M328（4）、M329（10）、M330（5） | 无 |
| M069 | 财务核心（FinanceService/控制器/仓储）与应收应付台账、日结、三方对账 | M331（10）、M332（1）、M333（22）、M334（14）、M335（2）、M336（5）、M337（10）、M338（4）、M339（6）、M340（6） | 无 |
| M070 | 商户支出台账、期间关账与持股分红引擎 | M341（13）、M342（11）、M343（7）、M344（6）、M345（2）、M346（6）、M347（1） | 无 |
| M071 | 经营看板指标、来源真实性、指标异常处置与 AI 质检边界 | M348（6）、M349（1）、M350（11）、M351（2）、M352（12）、M353（3）、M354（2）、M355（4）、M356（18）、M357（11）、M358（10）、M359（2）、M360（1） | 无 |
| M072 | 管理后台壳、路由/权限矩阵、页面验收与 UI 自动化 | M361（4）、M362（2）、M363（7）、M364（1）、M365（11）、M366（25）、M367（1）、M368（7）、M369（1）、M370（13）、M371（3） | 无 |
| M073 | 一体机、安卓终端（工作手机核销、平板）与售票能力开关 | M372（6）、M373（3）、M374（1）、M375（18）、M376（8）、M377（2）、M378（3）、M379（1）、M380（3） | 无 |
| M074 | 数据库迁移体系与其余迁移：版本序、缺号、就地改写、权限种子与运行角色授权 | M381（25）、M382（14）、M383（5）、M384（1）、M385（2）、M386（2） | 无 |
| M075 | OpenAPI 契约闭合：operation 矩阵、x-permission/x-terminal、样例与客户端路由一致性 | M387（1）、M388（2）、M389（1）、M390（2）、M391（1）、M392（1）、M393（1）、M394（9） | 无 |
| M076 | 后端平台基础：配置、启动守卫、健康/版本、异常映射与安全过滤链 | M395（6）、M396（22）、M397（11）、M398（7）、M399（1） | 无 |
| M077 | 160 部署、备份恢复、NAS、凭证轮换与运维 runbook | M400（1）、M401（14）、M402（25）、M403（1）、M404（10） | 无 |
| M078 | 生产就绪证据体系：阻断清单、责任包、manifest 模板与看板生成器 | M405（11）、M406（3）、M407（2）、M408（24）、M409（11）、M410（8）、M411（25）、M412（25）、M413（6）、M414（9）、M415（10）、M416（12） | 无 |
| M079 | 安全基线、Semgrep 扫描证据与安全 findings 台账 | M417（1）、M418（3）、M419（2）、M420（6） | 无 |
| M080 | HTML→PRD 承接台账、规则账本、决策账本与审计轮次记录一致性 | M421（6）、M422（1）、M423（1）、M424（12）、M425（1）、M426（1）、M427（2）、M428（7）、M429（1）、M430（1）、M431（2）、M432（2） | 无 |
| M081 | 历史归档、废止文档与超期计划的引用完整性 | M433（25）、M434（3） | `docs/_archive/20260610-retention-audit/historical-audits/adversarial-decision-options-20260610.md`；`docs/_archive/20260610-retention-audit/historical-audits/adversarial-full-document-audit-and-decision-list-20260610.md`；`docs/_archive/20260610-retention-audit/historical-audits/adversarial-full-document-audit-followup-20260610.md`；`docs/_archive/20260610-retention-audit/historical-audits/documentation-product-flow-audit-and-fix-report-20260608.md`；`docs/_archive/20260610-retention-audit/historical-audits/market-benchmark-adversarial-audit-20260609.md`；`docs/_archive/20260610-retention-audit/historical-audits/platform-governance-completion-audit-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/business-operations-acceptance-checklist-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/business-operations-field-dictionary-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/business-operations-flow-diagrams-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/business-operations-low-fi-prototype-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/business-operations-page-list-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/business-operations-product-breakdown-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/data-analysis-layer-acceptance-checklist-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/data-analysis-layer-field-dictionary-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/data-analysis-layer-flow-diagrams-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/data-analysis-layer-low-fi-prototype-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/data-analysis-layer-page-list-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/data-analysis-layer-product-prd-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/evidence-risk-layer-acceptance-checklist-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/evidence-risk-layer-field-dictionary-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/evidence-risk-layer-flow-diagrams-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/evidence-risk-layer-low-fi-prototype-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/evidence-risk-layer-page-list-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/platform-governance-acceptance-checklist-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/platform-governance-field-dictionary-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/platform-governance-flow-diagrams-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/platform-governance-low-fi-prototype-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/platform-governance-page-list-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/terminal-display-layer-acceptance-checklist-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/terminal-display-layer-field-dictionary-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/terminal-display-layer-flow-diagrams-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/terminal-display-layer-low-fi-prototype-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/terminal-display-layer-page-list-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/third-party-access-layer-acceptance-checklist-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/third-party-access-layer-field-dictionary-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/third-party-access-layer-flow-diagrams-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/third-party-access-layer-low-fi-prototype-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-layer-spec-sources/third-party-access-layer-page-list-v0.1.md`；`docs/_archive/20260610-retention-audit/merged-source-docs/data-risk-ai-delivery-note.md`；`docs/_archive/20260610-retention-audit/merged-source-docs/frontend-page-structure-optimization.md`；`docs/_archive/20260610-retention-audit/merged-source-docs/server-160-environment.md`；`docs/_archive/20260610-retention-audit/merged-source-docs/server-160-ops-implementation-design.md`；`docs/_archive/20260610-retention-audit/merged-source-docs/server-160-ops-plan.md`；`docs/_archive/20260610-retention-audit/obsolete-thread-indexes/thread-index.md`；`docs/_archive/20260610-retention-audit/obsolete-thread-indexes/thread-maintenance-map.md`；`docs/_archive/20260622-workspace-tidy/README.md`；`docs/_archive/20260622-workspace-tidy/codex-p0-notes/.codex-auth-compat-hardening.md`；`docs/_archive/20260622-workspace-tidy/codex-p0-notes/.codex-auth-refresh-logout.md`；`docs/_archive/20260622-workspace-tidy/codex-p0-notes/.codex-auth-username-compat.md`；`docs/_archive/20260622-workspace-tidy/codex-p0-notes/.codex-fix-ticketing-flyway-version-test.md`；`docs/_archive/20260622-workspace-tidy/codex-p0-notes/.codex-p0-access-authz-final.md`；`docs/_archive/20260622-workspace-tidy/codex-p0-notes/.codex-p0-access-authz-revoke-fix.md`；`docs/_archive/20260622-workspace-tidy/codex-p0-notes/.codex-p0-amusement-safety-session-verification-slice.md`；`docs/_archive/20260622-workspace-tidy/codex-p0-notes/.codex-p0-analytics-action-validation-fix.md`；`docs/_archive/20260622-workspace-tidy/codex-p0-notes/.codex-p0-analytics-ai-quality-risk-dashboard-slice.md`；`docs/_archive/20260622-workspace-tidy/codex-p0-notes/.codex-p0-android-smoke-idempotency-fix.md`；`docs/_archive/20260622-workspace-tidy/codex-p0-notes/.codex-p0-bar-reservation-order-billing-slice.md`；`docs/_archive/20260622-workspace-tidy/codex-p0-notes/.codex-p0-finance-ar-ap-ledger-slice.md`；`docs/_archive/20260622-workspace-tidy/codex-p0-notes/.codex-p0-first-slice-final.md`；`docs/_archive/20260622-workspace-tidy/codex-p0-notes/.codex-p0-idempotency-hardening-final.md`；`docs/_archive/20260622-workspace-tidy/codex-p0-notes/.codex-p0-inventory-saved-bottle-trace-slice.md`；`docs/_archive/20260622-workspace-tidy/codex-p0-notes/.codex-p0-open-platform-integration-adapter-boundary-slice.md`；`docs/_archive/20260622-workspace-tidy/codex-p0-notes/.codex-p0-open-platform-integration-secret-hardening.md`；`docs/_archive/20260622-workspace-tidy/codex-p0-notes/.codex-p0-platform-master-data-slice.md`；`docs/_archive/20260622-workspace-tidy/codex-p0-notes/.codex-p0-prod-readiness-runtime-fix-final.md`；`docs/_archive/20260622-workspace-tidy/codex-p0-notes/.codex-p0-terminal-device-credential-fix-2.md`；`docs/_archive/20260622-workspace-tidy/codex-p0-notes/.codex-p0-terminal-device-credential-fix.md`；`docs/_archive/20260622-workspace-tidy/codex-p0-notes/.codex-p0-terminal-evidence-slice.md`；`docs/_archive/20260622-workspace-tidy/codex-p0-notes/.codex-p0-ticketing-customer-order-credential-slice.md`；`docs/_archive/20260622-workspace-tidy/codex-p0-notes/.codex-p0-ticketing-discount-precision-fix.md`；`docs/_archive/20260622-workspace-tidy/codex-p0-notes/.codex-p0-ticketing-verification-slice.md`；`docs/_archive/20260622-workspace-tidy/security-scans/2026-06-15-full-repo-adversarial-audit/attack-path/attack_paths.md`；`docs/_archive/20260622-workspace-tidy/security-scans/2026-06-15-full-repo-adversarial-audit/context/threat_model.md`；`docs/_archive/20260622-workspace-tidy/security-scans/2026-06-15-full-repo-adversarial-audit/fix-report.md`；`docs/_archive/20260622-workspace-tidy/security-scans/2026-06-15-full-repo-adversarial-audit/report.html`；`docs/_archive/20260622-workspace-tidy/security-scans/2026-06-15-full-repo-adversarial-audit/report.md`；`docs/_archive/20260622-workspace-tidy/security-scans/2026-06-15-full-repo-adversarial-audit/validation/candidate_validation.md`；`docs/_archive/20260622-workspace-tidy/security-scans/nogit_20260605-000000/artifacts/01_context/threat_model.md`；`docs/_archive/20260622-workspace-tidy/security-scans/semgrep-triage-20260616-v1.md`；`docs/_archive/20260622-workspace-tidy/security-scans/t3d-local-llm-ai-quality-diff-review-20260615.md` |
| M082 | 文档入口、统一口径基准、词表、索引与其余文档 | M435（5）、M436（3）、M437（4）、M438（7）、M439（8）、M440（4）、M441（8） | 无 |
| M083 | 审计轮次记录、分片与非 canonical 快照 | M442（22）、M443（25）、M444（25）、M445（13）、M446（6）、M447（10）、M448（9）、M449（14）、M450（9）、M451（8）、M452（25）、M453（8） | 无 |
| M084 | 企业微信会话合规链：永久保存、Webhook 签名防重放、AI 质检预留 | M454（25）、M455（1）、M456（5）、M457（6）、M458（2）、M459（1） | 无 |
| M085 | 合同准入闸链：AdmissionGuard、ContractScopeGuard 与 @GuardedAction 前置校验 | M460（4）、M461（1）、M462（1）、M463（2）、M464（1） | 无 |
| M086 | 卡座销售提成归属与费率治理 | M465（3）、M466（1）、M467（5） | 无 |
| M087 | 审批链引擎（组织层级逐级节点链）与人员范围变更 | M468（7）、M469（1）、M470（3）、M471（4） | `backend/src/test/java/com/complex/platform/access/*ScopeChange*`；`backend/src/main/resources/db/migration/*scope_change*` |

## 已授权的未发布材料分段

操作员要求“尚未发布的这些也都审”。本附录列入完整 76 个改动文件；只读审查材料，不冒称已合并到 main，不混入 main 的 scope.txt。文件内容来自固定提交，输入副本只供读取与隔离验证。正式模块仍按上方 main 文件归属；每段记录原归属以便接续。

- 发布基线：`242f487233d028173cb9198eecd079f5d877405c`
- 未发布提交：`03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb`
- 未发布树：`e28dad059b2c7e08bcc95a8db18552d3f76edf4b`
- 160 输入根目录：`/home/james/complex-management/.scratch/module-recheck-20260908/unpublished-03acdb98`
- 三份业务 HTML 仍取 main 的当前正本；未发布材料不改变需求来源。
- 每段都检查完整文件及必要调用链；OpenAPI 两个超大文件分别完整结构验证和逐段覆盖，不把只看差异称为全量。

### 未发布材料第 1 段：安卓应用与设备密钥

- 文件数：3；物理行数：2217；标记：NORMAL

| 输入相对路径 | 行数 | 当前 main 主归属 |
|---|---:|---|
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/DeviceKeystore.kt` | 56 | M375 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | 1842 | M375 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt` | 319 | M375 |

### 未发布材料第 2 段：安卓共享请求与模型

- 文件数：4；物理行数：1180；标记：NORMAL

| 输入相对路径 | 行数 | 当前 main 主归属 |
|---|---:|---|
| `apps/android-terminal/src/main/kotlin/com/complex/terminal/DeviceRequestHeaders.kt` | 29 | 未发布新增文件 |
| `apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt` | 338 | M376 |
| `apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt` | 777 | M376 |
| `apps/android-terminal/src/test/kotlin/com/complex/terminal/DeviceRequestHeadersTest.kt` | 36 | 未发布新增文件 |

### 未发布材料第 3 段：员工网页和组织网页

- 文件数：8；物理行数：1844；标记：NORMAL

| 输入相对路径 | 行数 | 当前 main 主归属 |
|---|---:|---|
| `apps/web/src/admin/pages/OrganizationMerchantPage.tsx` | 480 | M012 |
| `apps/web/src/admin/pages/StaffPayrollLeavePage.tsx` | 331 | M293 |
| `apps/web/src/admin/pages/StaffProfilesPage.test.tsx` | 112 | M057 |
| `apps/web/src/admin/pages/StaffProfilesPage.tsx` | 212 | M057 |
| `apps/web/src/admin/pages/StaffShiftAttendancePage.batchshift.test.tsx` | 134 | M290 |
| `apps/web/src/admin/pages/StaffShiftAttendancePage.guidedcreate.test.tsx` | 107 | M081 |
| `apps/web/src/admin/pages/StaffShiftAttendancePage.settings.test.tsx` | 61 | 未发布新增文件 |
| `apps/web/src/admin/pages/StaffShiftAttendancePage.tsx` | 407 | M290 |

### 未发布材料第 4 段：员工网页和组织网页

- 文件数：1；物理行数：1301；标记：NORMAL

| 输入相对路径 | 行数 | 当前 main 主归属 |
|---|---:|---|
| `apps/web/src/admin/types.ts` | 1301 | M365 |

### 未发布材料第 5 段：员工服务与数据访问

- 文件数：7；物理行数：2098；标记：NORMAL

| 输入相对路径 | 行数 | 当前 main 主归属 |
|---|---:|---|
| `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | 1257 | M054 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffApprovalService.java` | 104 | 未发布新增文件 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffController.java` | 370 | M054 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffLockerService.java` | 85 | 未发布新增文件 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffNumericSetting.java` | 5 | 未发布新增文件 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffProfileRecord.java` | 139 | M054 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffRegistrationService.java` | 138 | 未发布新增文件 |

### 未发布材料第 6 段：员工服务与数据访问

- 文件数：9；物理行数：1664；标记：NORMAL

| 输入相对路径 | 行数 | 当前 main 主归属 |
|---|---:|---|
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | 1553 | M054 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffShiftRecord.java` | 33 | M288 |
| `backend/src/main/java/com/complex/platform/access/staff/dto/StaffEmploymentStatusRequest.java` | 4 | 未发布新增文件 |
| `backend/src/main/java/com/complex/platform/access/staff/dto/StaffNumericSettingRequest.java` | 4 | 未发布新增文件 |
| `backend/src/main/java/com/complex/platform/access/staff/dto/StaffPenaltyCapRequest.java` | 9 | 未发布新增文件 |
| `backend/src/main/java/com/complex/platform/access/staff/dto/StaffRegistrationRequest.java` | 19 | M054 |
| `backend/src/main/java/com/complex/platform/access/staff/dto/StaffSelfRegistrationRequest.java` | 5 | 未发布新增文件 |
| `backend/src/main/java/com/complex/platform/access/staff/dto/StaffShiftBatchResult.java` | 13 | M288 |
| `backend/src/main/java/com/complex/platform/access/staff/dto/StaffShiftRequest.java` | 24 | M288 |

### 未发布材料第 7 段：设备与接口访问联动

- 文件数：3；物理行数：1585；标记：NORMAL

| 输入相对路径 | 行数 | 当前 main 主归属 |
|---|---:|---|
| `backend/src/main/java/com/complex/platform/config/SecurityConfig.java` | 146 | M396 |
| `backend/src/main/java/com/complex/platform/gate/DeviceCredentialVerifier.java` | 371 | M063 |
| `backend/src/main/java/com/complex/platform/terminal/TerminalDeviceService.java` | 1068 | M064 |

### 未发布材料第 8 段：组织与商户联动

- 文件数：5；物理行数：2191；标记：NORMAL

| 输入相对路径 | 行数 | 当前 main 主归属 |
|---|---:|---|
| `backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java` | 1103 | M009 |
| `backend/src/main/java/com/complex/platform/governance/MerchantRecord.java` | 90 | M009 |
| `backend/src/main/java/com/complex/platform/governance/PlatformController.java` | 237 | M009 |
| `backend/src/main/java/com/complex/platform/governance/PlatformService.java` | 757 | M009 |
| `backend/src/main/java/com/complex/platform/governance/dto/MerchantSelfOperatedRequest.java` | 4 | 未发布新增文件 |

### 未发布材料第 9 段：主管审批联动

- 文件数：1；物理行数：890；标记：NORMAL

| 输入相对路径 | 行数 | 当前 main 主归属 |
|---|---:|---|
| `backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java` | 890 | M040 |

### 未发布材料第 10 段：员工数据库迁移

- 文件数：10；物理行数：90；标记：NORMAL

| 输入相对路径 | 行数 | 当前 main 主归属 |
|---|---:|---|
| `backend/src/main/resources/db/migration/V400__staff_profile_pii_and_self_device_permission.sql` | 6 | 未发布新增文件 |
| `backend/src/main/resources/db/migration/V401__staff_registration_initiator.sql` | 3 | 未发布新增文件 |
| `backend/src/main/resources/db/migration/V402__staff_policy_settings.sql` | 17 | 未发布新增文件 |
| `backend/src/main/resources/db/migration/V403__staff_profile_encrypted_personal_fields.sql` | 4 | 未发布新增文件 |
| `backend/src/main/resources/db/migration/V404__staff_employment_status.sql` | 4 | 未发布新增文件 |
| `backend/src/main/resources/db/migration/V405__staff_work_phone_receipts.sql` | 6 | 未发布新增文件 |
| `backend/src/main/resources/db/migration/V406__staff_shift_revision_approvals.sql` | 14 | 未发布新增文件 |
| `backend/src/main/resources/db/migration/V407__staff_penalty_cap_approval_history.sql` | 17 | 未发布新增文件 |
| `backend/src/main/resources/db/migration/V408__staff_registration_company_routes.sql` | 6 | 未发布新增文件 |
| `backend/src/main/resources/db/migration/V409__staff_locker_scan_records.sql` | 13 | 未发布新增文件 |

### 未发布材料第 11 段：员工回归测试

- 文件数：8；物理行数：2247；标记：NORMAL

| 输入相对路径 | 行数 | 当前 main 主归属 |
|---|---:|---|
| `backend/src/test/java/com/complex/platform/access/staff/M010RegistrationHttpTests.java` | 38 | 未发布新增文件 |
| `backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java` | 676 | 未发布新增文件 |
| `backend/src/test/java/com/complex/platform/access/staff/PayrollSafetyJdbcIntegrationTests.java` | 207 | M056 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffAttendanceLocalZoneTests.java` | 165 | M056 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java` | 942 | M056 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffProfileArchiveDeleteJdbcIntegrationTests.java` | 110 | M056 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffScopeTests.java` | 80 | M056 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffTestActors.java` | 29 | 未发布新增文件 |

### 未发布材料第 12 段：设备与主管审批测试

- 文件数：2；物理行数：1519；标记：NORMAL

| 输入相对路径 | 行数 | 当前 main 主归属 |
|---|---:|---|
| `backend/src/test/java/com/complex/platform/gate/DeviceCredentialVerifierTests.java` | 392 | M066 |
| `backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java` | 1127 | M042 |

### 未发布材料第 13 段：员工设计及交付说明

- 文件数：7；物理行数：1184；标记：NORMAL

| 输入相对路径 | 行数 | 当前 main 主归属 |
|---|---:|---|
| `docs/data-isolation-and-tenant-scope-security-spec-v0.1.md` | 371 | M028 |
| `docs/domain-ownership-scope-audit-v0.1.csv` | 333 | M028 |
| `docs/production-readiness-evidence/security-baseline-latest.json` | 14 | M411 |
| `docs/r5-spec-T1-staff-tables-dev-design-20260618.md` | 197 | M058 |
| `docs/r5-spec-T9-staff-device-android-dev-design-20260618.md` | 54 | M058 |
| `security-scans/findings-register-v1.md` | 53 | M420 |
| `security-scans/formal-security-baseline-v1.md` | 162 | M420 |

### 未发布材料第 14 段：完整接口定义

- 文件数：1；物理行数：51250；标记：OVERSIZE-SINGLE-FILE

| 输入相对路径 | 行数 | 当前 main 主归属 |
|---|---:|---|
| `docs/openapi/openapi-p0.json` | 51250 | M391 |

### 未发布材料第 15 段：完整接口定义

- 文件数：1；物理行数：37017；标记：OVERSIZE-SINGLE-FILE

| 输入相对路径 | 行数 | 当前 main 主归属 |
|---|---:|---|
| `docs/openapi/openapi-p0.yaml` | 37017 | M392 |

### 未发布材料第 16 段：接口覆盖说明

- 文件数：1；物理行数：2615；标记：NORMAL

| 输入相对路径 | 行数 | 当前 main 主归属 |
|---|---:|---|
| `docs/p0-openapi-contract-closure-appendix-v0.1.md` | 2615 | M389 |

### 未发布材料第 17 段：接口覆盖说明

- 文件数：2；物理行数：1112；标记：NORMAL

| 输入相对路径 | 行数 | 当前 main 主归属 |
|---|---:|---|
| `docs/p0-openapi-operation-acceptance-matrix-v0.1.md` | 822 | M390 |
| `tools/generate-openapi-contract-closure-appendix.mjs` | 290 | M394 |

### 未发布材料第 18 段：文档生成与检查工具

- 文件数：3；物理行数：492；标记：NORMAL

| 输入相对路径 | 行数 | 当前 main 主归属 |
|---|---:|---|
| `tools/generate-domain-ownership-scope-audit.mjs` | 230 | M029 |
| `tools/generate-openapi-operation-matrix.mjs` | 193 | M394 |
| `tools/verify-pii-encryption.mjs` | 69 | M053 |
