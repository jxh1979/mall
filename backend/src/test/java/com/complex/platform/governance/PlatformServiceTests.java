package com.complex.platform.governance;
// 文件版本：V0.04

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.support.InMemoryAuditEventRepository;
import com.complex.platform.support.InMemoryIdempotencyRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.governance.dto.BusinessEntityBindingRequest;
import com.complex.platform.governance.dto.BusinessEntityRequest;
import com.complex.platform.governance.dto.MerchantProfileRequest;
import com.complex.platform.governance.dto.MerchantDeleteRequest;
import com.complex.platform.governance.dto.MerchantDeleteApprovalRequest;
import com.complex.platform.governance.dto.MerchantArchiveRequest;
import com.complex.platform.governance.dto.ProjectRequest;
import com.complex.platform.governance.dto.StatusChangeRequest;
import com.complex.platform.support.TestSecurityActors;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class PlatformServiceTests {

    private InMemoryPlatformRepository platformRepository;
    private InMemoryAuditEventRepository auditEventRepository;
    private PlatformService service;

    @BeforeEach
    void setUp() {
        platformRepository = new InMemoryPlatformRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        AuditEventService auditEventService = new AuditEventService(auditEventRepository);
        AuditedOperationExecutor operations = new AuditedOperationExecutor(
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                auditEventService
        );
        service = new PlatformService(
                platformRepository,
                operations,
                auditEventService,
                new AuthorizationService(null)
        );
        // F-AUTHZ-02：治理写方法现首行 requirePlatformScope()，以平台管理员身份运行既有写用例。
        TestSecurityActors.asPlatformAdmin();
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    @Test
    void createMerchantRequiresFormalMerchantName() {
        assertThatThrownBy(() -> service.createMerchant(new MerchantProfileRequest(
                "merchant-missing-name",
                "missing name",
                List.of(),
                " ",
                "other",
                "be_1",
                null,
                null,
                "Jane",
                "18800000011",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "active"
        ))).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("merchant.update_failed");
    }

    // ---- V260 商户存档与审批后删除范式 ----

    private MerchantRecord seedMerchant(String key) {
        return service.createMerchant(new MerchantProfileRequest(
                key, "seed", List.of(), "存档删除测试商户", "other", "be_1",
                null, null, "Jane", "18800000011",
                null, null, null, null, null, null, null, null, "active"));
    }

    /** M009-006：绑定引用的主体须真实存在——直接落一条主体行（不经服务，避免干扰幂等计数断言）。 */
    private void seedEntity(String id) {
        platformRepository.saveBusinessEntity(new BusinessEntityRecord(
                id, id, "management_company", null, null, null, "active", Instant.EPOCH, Instant.EPOCH));
    }

    /** M009-009：最高权限（持授权 U 盘密钥者）身份——isMasterAuthority 要求 platform_admin + 固定 id/用户名（BSM-004/007）。 */
    private static void asMasterAuthority() {
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                "platform_admin", "org_1", null, "active");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }

    // ---- M009-001/002：更新商户不得擦除存档/删除申请状态；更新不存在的商户须 404 ----
    @Test
    void updateMerchantKeepsArchiveAndPendingDeleteState() {
        MerchantRecord merchant = seedMerchant("seed-keep-state");
        service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("keep-archive", "测试存档理由"));
        service.requestDeleteMerchant(merchant.id(), new MerchantDeleteRequest("keep-delete-req", "清理"));

        MerchantRecord updated = service.updateMerchant(merchant.id(), new MerchantProfileRequest(
                "keep-update", "rename", List.of(), "改名后的商户", "other", "be_other",
                "be_contract_x", "be_settlement_x", "Jane", "18800000011",
                null, null, null, null, null, null, null, null, "active"));

        assertThat(updated.merchantName()).isEqualTo("改名后的商户");
        assertThat(updated.archivedAt()).isNotNull();
        assertThat(updated.deleteStatus()).isEqualTo("pending");
        assertThat(updated.deleteRequestedBy()).isEqualTo("usr_admin");
        assertThat(updated.createdAt()).isEqualTo(merchant.createdAt());
        // M009-008：三个主体列以生效绑定为准，档案更新不覆盖。
        assertThat(updated.businessEntityId()).isEqualTo("be_1");
        assertThat(updated.contractEntityId()).isNull();
        assertThat(service.listMerchants(false).items()).noneMatch(m -> m.id().equals(merchant.id()));
    }

    @Test
    void updateMerchantRejectsUnknownMerchant() {
        assertThatThrownBy(() -> service.updateMerchant("mch_missing", new MerchantProfileRequest(
                "update-missing", "update", List.of(), "幽灵商户", "other", null,
                null, null, null, null, null, null, null, null, null, null, null, null, "active")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("MERCHANT_NOT_FOUND"));
        assertThat(platformRepository.listMerchants()).noneMatch(m -> m.id().equals("mch_missing"));
    }

    // ---- M009-003：合作方式六类（BEQR-148）；空＝未定 ----
    @Test
    void createMerchantRejectsCooperationFormOutsideSixModes() {
        assertThatThrownBy(() -> service.createMerchant(new MerchantProfileRequest(
                "coop-bad", "seed", List.of(), "合作方式错的商户", "other", null,
                null, null, null, null, null, null, null, null, null, null, null, "fixed_rent_plus_commission", "active")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("MERCHANT_COOPERATION_FORM_INVALID"));
        MerchantRecord lease = service.createMerchant(new MerchantProfileRequest(
                "coop-lease", "seed", List.of(), "租赁商户", "other", null,
                null, null, null, null, null, null, null, null, null, null, null, "lease", "active"));
        assertThat(lease.cooperationForm()).isEqualTo("lease");
        assertThat(seedMerchant("coop-null").cooperationForm()).isNull();
    }

    // ---- M009-004：商户/主体/项目状态只认速查表六态（BEQR-154/155） ----
    @Test
    void createMerchantRejectsStatusOutsideEnumeratedStates() {
        assertThatThrownBy(() -> service.createMerchant(new MerchantProfileRequest(
                "status-bad", "seed", List.of(), "状态错的商户", "other", null,
                null, null, null, null, null, null, null, null, null, null, null, null, "draft")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));
        assertThat(service.createMerchant(new MerchantProfileRequest(
                "status-pending", "seed", List.of(), "待准入商户", "other", null,
                null, null, null, null, null, null, null, null, null, null, null, null, "pending_admission")).status())
                .isEqualTo("pending_admission");
    }

    @Test
    void createProjectRejectsUnknownStatusOrMerchant() {
        assertThatThrownBy(() -> service.createProject(new ProjectRequest(
                "project-bad-status", "create", List.of(), null, "bar", "P0 Bar", null, null, null, "zzz")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));
        // M009-007：merchantId 非空时必须存在。
        assertThatThrownBy(() -> service.createProject(new ProjectRequest(
                "project-bad-merchant", "create", List.of(), "mch_missing", "bar", "P0 Bar", null, null, null, "draft")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("MERCHANT_NOT_FOUND"));
    }

    // ---- M009-005：主体角色白名单（PGM-018/019） ----
    @Test
    void createBusinessEntityRejectsRoleOutsideWhitelist() {
        assertThatThrownBy(() -> service.createBusinessEntity(new BusinessEntityRequest(
                "entity-spv", "create", List.of(), "SPV 主体", "spv", null, null, null, "active")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));
    }

    // ---- M009-006：绑定的经营对象与三个主体须存在 ----
    @Test
    void createBusinessEntityBindingRejectsMissingMerchantOrEntity() {
        assertThatThrownBy(() -> service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
                "binding-ghost-merchant", "ghost", List.of(), "mch_missing", "be_x", "be_x", "be_x", null, null)))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("MERCHANT_NOT_FOUND"));
        MerchantRecord merchant = seedMerchant("seed-ghost-entity");
        assertThatThrownBy(() -> service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
                "binding-ghost-entity", "ghost", List.of(), merchant.id(), "be_missing", "be_missing", "be_missing", null, null)))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("BUSINESS_ENTITY_NOT_FOUND"));
        assertThat(platformRepository.listBusinessEntityBindings()).isEmpty();
    }

    // ---- M009-007：空间归属链须存在 ----
    @Test
    void createSpaceRejectsUnknownProjectMerchantOrParent() {
        assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
                "sp-bad-prj", "create", List.of(), "prj_missing", null, null, "A", "shop", null, null, null, "active")))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("PROJECT_NOT_FOUND"));
        assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
                "sp-bad-mch", "create", List.of(), null, "mch_missing", null, "A", "shop", null, null, null, "active")))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("MERCHANT_NOT_FOUND"));
        assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
                "sp-bad-parent", "create", List.of(), null, null, "sp_missing", "A", "shop", null, null, null, "active")))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("SPACE_NOT_FOUND"));
    }

    // ---- M009-028：已存档/已批删商户不能再被新项目、空间、绑定引用（BSM-004「下线就不能再用」） ----
    @Test
    void archivedMerchantCannotBeReferencedByNewProjectSpaceOrBinding() {
        MerchantRecord merchant = seedMerchant("seed-archived-ref");
        service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("archive-ref", "测试存档理由"));
        seedEntity("be_ref_1");
        assertThatThrownBy(() -> service.createProject(new ProjectRequest(
                "project-archived-mch", "create", List.of(), merchant.id(), "bar", "P0 Bar", null, null, null, "draft")))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("MERCHANT_ARCHIVED"));
        assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
                "sp-archived-mch", "create", List.of(), null, merchant.id(), null, "A", "shop", null, null, null, "active")))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("MERCHANT_ARCHIVED"));
        assertThatThrownBy(() -> service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
                "binding-archived-mch", "ref", List.of(), merchant.id(), "be_ref_1", "be_ref_1", "be_ref_1", null, null)))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("MERCHANT_ARCHIVED"));
        assertThat(platformRepository.listProjects()).noneMatch(p -> merchant.id().equals(p.merchantId()));
        assertThat(platformRepository.listBusinessEntityBindings()).isEmpty();
    }

    // ---- M009-036：空间状态须在已发布 CHECK（V203：active/unavailable）内，非法值 400 而非落库 500 ----
    @Test
    void createSpaceRejectsStatusOutsideCheckSet() {
        assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
                "sp-bad-status", "create", List.of(), null, null, null, "A", "shop", null, null, null, "zzz")))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("VALIDATION_FAILED"));
        assertThat(platformRepository.listSpaces()).noneMatch(s -> "zzz".equals(s.status()));
        assertThat(service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
                "sp-unavailable", "create", List.of(), null, null, null, "B", "shop", null, null, null, "unavailable")).status())
                .isEqualTo("unavailable");
    }

    // ---- M009-008：生效绑定同步商户三个主体列（PGM-026「引用同一组绑定」） ----
    @Test
    void createBusinessEntityBindingSyncsMerchantEntityColumns() {
        MerchantRecord merchant = seedMerchant("seed-sync");
        seedEntity("be_op_sync");
        seedEntity("be_ct_sync");
        seedEntity("be_st_sync");
        service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
                "binding-sync", "sync", List.of(), merchant.id(), "be_op_sync", "be_ct_sync", "be_st_sync", null, null));
        MerchantRecord synced = platformRepository.findMerchantById(merchant.id()).orElseThrow();
        assertThat(synced.businessEntityId()).isEqualTo("be_op_sync");
        assertThat(synced.contractEntityId()).isEqualTo("be_ct_sync");
        assertThat(synced.settlementEntityId()).isEqualTo("be_st_sync");
    }

    // ---- M009-009：删除审批只认最高权限；最高权限本人发起免审批直接执行（BSM-004/007） ----
    @Test
    void approveDeleteByNonMasterIsForbidden() {
        MerchantRecord merchant = seedMerchant("seed-nonmaster");
        service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("archive-nm", "测试存档理由"));
        service.requestDeleteMerchant(merchant.id(), new MerchantDeleteRequest("delete-req-nm", "清理"));
        TestSecurityActors.asPlatformAdmin("usr_admin2");
        assertThatThrownBy(() -> service.approveDeleteMerchant(merchant.id(),
                new MerchantDeleteApprovalRequest("delete-approve-nm", "approved", null)))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("MERCHANT_DELETE_APPROVER_NOT_MASTER"));
        assertThat(platformRepository.findMerchantById(merchant.id()).orElseThrow().deleteStatus()).isEqualTo("pending");
    }

    @Test
    void requestDeleteByMasterAuthorityExecutesWithoutApproval() {
        MerchantRecord merchant = seedMerchant("seed-master");
        service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("archive-master", "测试存档理由"));
        platformRepository.merchantReferenceCount = 0;
        asMasterAuthority();
        MerchantRecord deleted = service.requestDeleteMerchant(merchant.id(),
                new MerchantDeleteRequest("delete-req-master", "最高权限直接清理"));
        assertThat(deleted.deleteStatus()).isEqualTo("approved");
        assertThat(deleted.deleteRequestedBy()).isEqualTo(SessionIssuer.MASTER_USER_ID);
        assertThat(service.listMerchants(false).items()).noneMatch(m -> m.id().equals(merchant.id()));
    }

    // ---- M009-014：字典分页元数据须覆盖全部条目 ----
    @Test
    void listSpaceTypesPageSizeCoversAllItems() {
        var page = service.listSpaceTypes();
        assertThat(page.pageSize()).isGreaterThanOrEqualTo(page.items().size());
        assertThat(page.pageSize()).isEqualTo(500);
    }

    @Test
    void createSpaceTypeIgnoresRequestedDisabledAndStaysActive() {
        SpaceTypeRecord created = service.createSpaceType(new com.complex.platform.governance.dto.SpaceTypeRequest(
                "st-force-active", "add", List.of(), "tempkind", "临时种类", 10, "disabled"));
        assertThat(created.status()).isEqualTo("active");
    }

    @Test
    void disableSpaceTypeArchivesInsteadOfDeleting() {
        service.disableSpaceType("sptype_booth");
        assertThat(platformRepository.findSpaceTypeById("sptype_booth").orElseThrow().status()).isEqualTo("archived");
        assertThat(platformRepository.listSpaceTypes()).anyMatch(item -> "booth".equals(item.typeCode()));
    }

    @Test
    void directDeleteSpaceTypeRequiresArchiveAndMasterApproval() {
        assertThatThrownBy(() -> service.deleteSpaceType("sptype_other"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("SPACE_TYPE_DELETE_REQUIRES_APPROVAL"));
        assertThat(platformRepository.findSpaceTypeById("sptype_other")).isPresent();
    }

    @Test
    void spaceTypeDeleteGoesArchiveRequestThenMasterApprove() {
        SpaceTypeRecord created = service.createSpaceType(new com.complex.platform.governance.dto.SpaceTypeRequest(
                "st-del-flow", "add", List.of(), "tempdel", "待删种类", 20, "disabled"));
        assertThatThrownBy(() -> service.requestDeleteSpaceType(created.id(), "未存档"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("SPACE_TYPE_NOT_ARCHIVED"));
        service.archiveSpaceType(created.id());
        service.requestDeleteSpaceType(created.id(), "不再使用");
        assertThat(platformRepository.findSpaceTypeById(created.id()).orElseThrow().status()).isEqualTo("pending_delete");
        assertThatThrownBy(() -> service.approveDeleteSpaceType(created.id(), "approved"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("SPACE_TYPE_DELETE_APPROVER_NOT_MASTER"));
        asMasterAuthority();
        service.approveDeleteSpaceType(created.id(), "approved");
        assertThat(platformRepository.findSpaceTypeById(created.id()).orElseThrow().status()).isEqualTo("disabled");
    }

    // V348/V384 商户业态标准字段：六值（酒吧/餐饮/零售/游乐/传媒/其他）之外的业态一律拒收（业态错＝通票分账份额拆给错的业态）。M011-005
    @Test
    void createMerchantRejectsBusinessFormatOutsideSixValues() {
        assertThatThrownBy(() -> service.createMerchant(new MerchantProfileRequest(
                "bad-format", "seed", List.of(), "业态错的商户", "tenant", "be_1",
                null, null, "Jane", "18800000011",
                null, null, null, null, null, null, null, null, "active")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("MERCHANT_BUSINESS_FORMAT_INVALID"));
    }

    // V348 按业态查询（分账按业态拆份额要用）：只返回该业态的商户。
    @Test
    void listMerchantsFiltersByBusinessFormat() {
        service.createMerchant(new MerchantProfileRequest(
                "seed-bar", "seed", List.of(), "话梅酒馆", "bar", "be_1",
                null, null, "Jane", "18800000011",
                null, null, null, null, null, null, null, null, "active"));
        MerchantRecord other = seedMerchant("seed-other");

        assertThat(service.listMerchants(true, "bar", 1, 50).items())
                .extracting(MerchantRecord::merchantName).containsExactly("话梅酒馆");
        assertThat(service.listMerchants(true, "bar", 1, 50).items())
                .noneMatch(m -> m.id().equals(other.id()));
        assertThatThrownBy(() -> service.listMerchants(true, "tenant", 1, 50))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("MERCHANT_BUSINESS_FORMAT_INVALID"));
    }

    @Test
    void archiveMerchantHidesItFromDefaultListingAndKeepsWithIncludeArchived() {
        MerchantRecord merchant = seedMerchant("seed-archive");
        service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("archive-once", "测试存档理由"));

        assertThat(service.listMerchants(false).items())
                .noneMatch(m -> m.id().equals(merchant.id()));
        assertThat(service.listMerchants(true).items())
                .anyMatch(m -> m.id().equals(merchant.id()) && m.archivedAt() != null);
    }

    @Test
    void approveDeleteSoftDeletesArchivedMerchantWhenNoReferences() {
        MerchantRecord merchant = seedMerchant("seed-delete");
        service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("archive-2", "测试存档理由"));
        service.requestDeleteMerchant(merchant.id(),
                new MerchantDeleteRequest("delete-req", "录入错误，退场清理"));

        platformRepository.merchantReferenceCount = 0;
        asMasterAuthority(); // M009-009：删除只能由最高权限审批（BSM-004），且≠申请发起人
        MerchantDeleteDecision decision = service.approveDeleteMerchant(merchant.id(),
                new MerchantDeleteApprovalRequest("delete-approve", "approved", null));

        assertThat(decision.deleted()).isTrue();
        // M011-001：审批删除＝软删除标记（BSM-004「不是真从数据库里抹掉…可查、可审计、可恢复」）：
        // 默认列表不见；已存档列表仍可查且 delete_status=approved——与 JdbcPlatformRepository 同口径。
        assertThat(service.listMerchants(false).items())
                .noneMatch(m -> m.id().equals(merchant.id()));
        assertThat(service.listMerchants(true).items())
                .anyMatch(m -> m.id().equals(merchant.id()) && "approved".equals(m.deleteStatus()));
    }

    // M011-003：BSM-004「想恢复点“取消存档”即可」——存档后默认列表不可见，取消存档后默认列表重新可见、待审批删除回退 none。
    @Test
    void unarchiveMerchantRestoresDefaultListVisibility() {
        MerchantRecord merchant = seedMerchant("seed-unarchive");
        service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("archive-u1", "测试存档理由"));
        assertThat(service.listMerchants(false).items()).noneMatch(m -> m.id().equals(merchant.id()));

        service.unarchiveMerchant(merchant.id(), new MerchantArchiveRequest("unarchive-u1", null));

        assertThat(service.listMerchants(false).items())
                .anyMatch(m -> m.id().equals(merchant.id()) && m.archivedAt() == null && "none".equals(m.deleteStatus()));
    }

    // M011-003：驳回删除申请——商户回到 none 态、仍在已存档列表可查（BSM-004「可恢复」）；删除理由随申请落到记录（BSM-004「存档及删除都需要填写理由」）。
    @Test
    void rejectingDeleteRequestKeepsMerchantAndResetsDeleteStatus() {
        MerchantRecord merchant = seedMerchant("seed-reject");
        service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("archive-r1", "测试存档理由"));
        service.requestDeleteMerchant(merchant.id(), new MerchantDeleteRequest("delete-req-r1", "录入错误，申请删除"));
        assertThat(service.listMerchants(true).items())
                .anyMatch(m -> m.id().equals(merchant.id()) && "pending".equals(m.deleteStatus())
                        && "录入错误，申请删除".equals(m.deleteReason()));

        asMasterAuthority();
        MerchantDeleteDecision decision = service.approveDeleteMerchant(merchant.id(),
                new MerchantDeleteApprovalRequest("delete-reject-r1", "rejected", null));

        assertThat(decision.deleted()).isFalse();
        assertThat(decision.decision()).isEqualTo("rejected");
        assertThat(service.listMerchants(true).items())
                .anyMatch(m -> m.id().equals(merchant.id()) && "none".equals(m.deleteStatus()) && m.archivedAt() != null);
    }

    // M011-004：BSM-072/074「商户不能看平台全局和其他商户数据」——商户账号打治理写路径在服务层首行即被
    // requirePlatformScope 拒绝（F-AUTHZ-02），不依赖控制器。
    @Test
    void merchantActorIsRejectedByPlatformScopeOnGovernanceWrite() {
        TestSecurityActors.asMerchantUser("mch_scope_1");
        assertThatThrownBy(() -> seedMerchant("scope-write"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("PLATFORM_SCOPE_REQUIRED"));
        assertThatThrownBy(() -> service.archiveMerchant("mch_any", new MerchantArchiveRequest("archive-scope", "理由")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("PLATFORM_SCOPE_REQUIRED"));
    }

    @Test
    void approveDeleteRejectsWhenMerchantStillReferenced() {
        MerchantRecord merchant = seedMerchant("seed-inuse");
        service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("archive-3", "测试存档理由"));
        service.requestDeleteMerchant(merchant.id(),
                new MerchantDeleteRequest("delete-req-2", "尝试删除仍在用商户"));

        platformRepository.merchantReferenceCount = 3; // 模拟仍被酒吧商品/合同等引用
        asMasterAuthority(); // M009-009：删除只能由最高权限审批（BSM-004），且≠申请发起人

        assertThatThrownBy(() -> service.approveDeleteMerchant(merchant.id(),
                new MerchantDeleteApprovalRequest("delete-approve-2", "approved", null)))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("MERCHANT_IN_USE"));
        // 拒删后商户仍在（含已存档列表可见）。
        assertThat(service.listMerchants(true).items())
                .anyMatch(m -> m.id().equals(merchant.id()));
    }

    @Test
    void createBusinessEntityUsesIdempotencyAndWritesAudit() {
        BusinessEntityRequest request = new BusinessEntityRequest(
                "entity-once",
                "create entity",
                List.of("att_1"),
                " P0 Entity ",
                "management_company",
                "91310000MA1P0",
                "Jane Ops",
                "vault://entity/bank",
                "active"
        );

        BusinessEntityRecord first = service.createBusinessEntity(request);
        BusinessEntityRecord second = service.createBusinessEntity(request);

        assertThat(second).isEqualTo(first);
        assertThat(platformRepository.businessEntitySaveCount).isEqualTo(1);
        assertThat(first.id()).startsWith("be_");
        assertThat(first.entityName()).isEqualTo("P0 Entity");
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "business_entity.changed".equals(event.eventName()))
                .hasSize(1);
    }

    @Test
    void createProjectUsesOpenApiAuditEventNames() {
        MerchantRecord owner = seedMerchant("seed-project-owner"); // M009-007：merchantId 须存在
        ProjectRecord created = service.createProject(new ProjectRequest(
                "project-create",
                "create project",
                List.of(),
                owner.id(),
                "bar",
                "P0 Bar",
                "BAR-01",
                "1F",
                120,
                "active"
        ));

        assertThat(created.id()).startsWith("prj_");
        assertThat(created.name()).isEqualTo("P0 Bar");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("project.created");
    }

    @Test
    void updateMerchantWritesAuditAndPreservesId() {
        MerchantRecord existing = seedMerchant("seed-existing"); // M009-002：更新须命中已有商户
        MerchantRecord updated = service.updateMerchant(existing.id(), new MerchantProfileRequest(
                "merchant-update",
                "update merchant",
                List.of(),
                "P0 Merchant",
                "retail",
                "be_1",
                "be_contract",
                "be_settlement",
                "Jane Contact",
                "18800000012",
                "Owner Li",
                "1F-A",
                BigDecimal.valueOf(128.50),
                BigDecimal.valueOf(120.00),
                "Jack Contact",
                "18800000013",
                BigDecimal.valueOf(12.30),
                "lease",
                "active"
        ));

        assertThat(updated.id()).isEqualTo(existing.id());
        assertThat(updated.merchantName()).isEqualTo("P0 Merchant");
        assertThat(updated.mallBlockCode()).isEqualTo("1F-A");
        assertThat(updated.ownerPerson()).isEqualTo("Owner Li");
        assertThat(updated.areaSquareMeter()).isEqualByComparingTo("128.50");
        assertThat(updated.leasedAreaSquareMeter()).isEqualByComparingTo("120.00");
        assertThat(updated.secondaryContactName()).isEqualTo("Jack Contact");
        assertThat(updated.secondaryContactPhone()).isEqualTo("18800000013");
        assertThat(updated.rentPerSquareMeter()).isEqualByComparingTo("12.30");
        assertThat(updated.cooperationForm()).isEqualTo("lease");
        // M009-008：三个主体列以生效绑定为准，档案更新不覆盖（seed 时 businessEntityId=be_1、其余空）。
        assertThat(updated.businessEntityId()).isEqualTo("be_1");
        assertThat(updated.contractEntityId()).isNull();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("merchant.updated");
    }

    @Test
    void listSpacesWritesViewedAudit() {
        platformRepository.spaces.put("sp_1", new SpaceRecord(
                "sp_1",
                "prj_1",
                "mch_1",
                null,
                "P0 Space",
                "shop",
                "1F",
                null,
                List.of(),
                "active",
                java.time.Instant.parse("2026-06-14T00:00:00Z"),
                java.time.Instant.parse("2026-06-14T00:00:00Z")
        ));

        var page = service.listSpaces();

        assertThat(page.items()).hasSize(1);
        assertThat(page.total()).isEqualTo(1);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("space.viewed");
    }

    @Test
    void createBusinessEntityBindingAllowsOnlyOneActiveBindingPerMerchant() {
        MerchantRecord merchant = seedMerchant("seed-binding"); // M009-006：经营对象与主体须存在
        for (String id : List.of("be_operation_1", "be_contract_1", "be_settlement_1",
                "be_operation_2", "be_contract_2", "be_settlement_2")) {
            seedEntity(id);
        }
        BusinessEntityBindingRequest first = new BusinessEntityBindingRequest(
                "binding-first",
                "first binding",
                List.of("att_1"),
                merchant.id(),
                "be_operation_1",
                "be_contract_1",
                "be_settlement_1",
                LocalDate.parse("2026-06-14"),
                "initial setup"
        );

        BusinessEntityBindingRecord created = service.createBusinessEntityBinding(first);

        assertThat(created.status()).isEqualTo("active");
        assertThat(created.merchantId()).isEqualTo(merchant.id());
        assertThatThrownBy(() -> service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
                "binding-second",
                "second binding",
                List.of(),
                merchant.id(),
                "be_operation_2",
                "be_contract_2",
                "be_settlement_2",
                LocalDate.parse("2026-06-15"),
                "replacement without changing old binding"
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("ACTIVE_BINDING_EXISTS");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("entity_binding.changed", "entity_binding.change_failed");
    }

    @Test
    void changeBusinessEntityBindingRecordsStatusHistoryAndFreesActiveUniqueness() {
        MerchantRecord merchant = seedMerchant("seed-change"); // M009-006
        for (String id : List.of("be_operation", "be_contract", "be_settlement",
                "be_operation_new", "be_contract_new", "be_settlement_new")) {
            seedEntity(id);
        }
        BusinessEntityBindingRecord created = service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
                "binding-change-create",
                "create binding",
                List.of(),
                merchant.id(),
                "be_operation",
                "be_contract",
                "be_settlement",
                LocalDate.parse("2026-06-14"),
                "initial setup"
        ));

        BusinessEntityBindingRecord changed = service.changeBusinessEntityBinding(
                created.id(),
                new StatusChangeRequest("binding-change", "inactive", "contract entity changed", List.of("att_change"))
        );
        BusinessEntityBindingRecord replacement = service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
                "binding-change-replacement",
                "replacement binding",
                List.of(),
                merchant.id(),
                "be_operation_new",
                "be_contract_new",
                "be_settlement_new",
                LocalDate.parse("2026-06-15"),
                "replacement"
        ));

        assertThat(changed.status()).isEqualTo("inactive");
        assertThat(replacement.status()).isEqualTo("active");
        assertThat(platformRepository.businessEntityBindingStatusHistory)
                .containsExactly("active", "inactive", "active");
    }


    // P1-4 red→green: action 字段拒绝非法枚举值
    @Test
    void changeBusinessEntityBindingRejectsInvalidAction() {
        MerchantRecord merchant = seedMerchant("seed-enum"); // M009-006
        for (String id : List.of("be_op", "be_ctr", "be_stl")) {
            seedEntity(id);
        }
        BusinessEntityBindingRecord created = service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
                "binding-enum-create",
                "create",
                List.of(),
                merchant.id(),
                "be_op",
                "be_ctr",
                "be_stl",
                LocalDate.parse("2026-07-01"),
                "initial"
        ));

        assertThatThrownBy(() -> service.changeBusinessEntityBinding(created.id(),
                new StatusChangeRequest("binding-enum-bad", "active_bypass", "reason", List.of())))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));
    }

    // P1-5 red→green: slug 碰撞幂等键不产生相同 ID
    @Test
    void createBusinessEntityWithSlashCollidingKeysGetDistinctIds() {
        BusinessEntityRecord e1 = service.createBusinessEntity(new BusinessEntityRequest(
                "corp-A",
                "entity corp-A",
                List.of(),
                "Corp A",
                "management_company",
                null,
                null,
                null,
                "active"
        ));
        BusinessEntityRecord e2 = service.createBusinessEntity(new BusinessEntityRequest(
                "corp_A",
                "entity corp_A",
                List.of(),
                "Corp B",
                "management_company",
                null,
                null,
                null,
                "active"
        ));

        assertThat(e1.id()).isNotEqualTo(e2.id());
        assertThat(e1.entityName()).isEqualTo("Corp A"); // 第一条未被覆盖
        assertThat(e2.entityName()).isEqualTo("Corp B");
    }


    @Test
    void createSpaceTypeUpsertsDictionaryAndEnablesItForSpaceValidation() {
        SpaceTypeRecord created = service.createSpaceType(new com.complex.platform.governance.dto.SpaceTypeRequest(
                "st-1", "add", List.of(), "kiosk", "一体机位", 80, "active"));
        assertThat(created.typeCode()).isEqualTo("kiosk");
        assertThat(service.listSpaceTypes().items()).extracting(SpaceTypeRecord::typeCode).contains("kiosk", "shop");
        // 新录入类型立即可用于 createSpace 硬校验。
        SpaceRecord space = service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
                "sp-kiosk", "create", List.of(), null, null, null, "K1", "kiosk", null, null, null, "active")); // M009-007：归属为空或须存在
        assertThat(space.spaceType()).isEqualTo("kiosk");
    }

    @Test
    void createSpaceRejectsUnknownSpaceType() {
        // 条目3 硬校验：不在字典内的 space_type → VALIDATION_FAILED。
        assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
                        "sp-bad", "create", List.of(), null, null, null, "Bad", "no_such_type", null, null, null, "active")))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class, ex ->
                        assertThat(ex.code()).isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    void createSpaceAcceptsDictionarySpaceType() {
        SpaceRecord saved = service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
                "sp-ok", "create", List.of(), null, null, null, "Shop A", "shop", "1", null, null, "active"));
        assertThat(saved.spaceType()).isEqualTo("shop");
    }

    private static final class InMemoryPlatformRepository implements PlatformRepository {
        @Override public void setMerchantArchiveReason(String id, String reason) { }
        private final Map<String, BusinessEntityRecord> businessEntities = new LinkedHashMap<>();
        private final Map<String, MerchantRecord> merchants = new LinkedHashMap<>();
        private final Map<String, ProjectRecord> projects = new LinkedHashMap<>();
        private final Map<String, SpaceRecord> spaces = new LinkedHashMap<>();
        private final Map<String, BusinessEntityBindingRecord> businessEntityBindings = new LinkedHashMap<>();
        private final List<String> businessEntityBindingStatusHistory = new ArrayList<>();
        private int businessEntitySaveCount;

        @Override
        public BusinessEntityRecord saveBusinessEntity(BusinessEntityRecord businessEntity) {
            businessEntitySaveCount += 1;
            businessEntities.put(businessEntity.id(), businessEntity);
            return businessEntity;
        }

        @Override
        public List<BusinessEntityRecord> listBusinessEntities() {
            return new ArrayList<>(businessEntities.values());
        }

        // V260 测试用：模拟商户被核心运营表引用的活条数（>0 拒绝审批删除；删除本身为软删除标记，M011-001）。
        private long merchantReferenceCount;

        @Override
        public MerchantRecord saveMerchant(MerchantRecord merchant) {
            merchants.put(merchant.id(), merchant);
            return merchant;
        }

        @Override
        public List<MerchantRecord> listMerchants() {
            return new ArrayList<>(merchants.values());
        }

        @Override
        public void deleteMerchant(String merchantId) {
            // M011-001：与 JdbcPlatformRepository.deleteMerchant 同语义——软删除标记，行保留可查可恢复（BSM-004）。
            MerchantRecord merchant = merchants.get(merchantId);
            if (merchant != null) {
                merchants.put(merchantId, merchant.withDelete("approved", merchant.deleteRequestedBy(),
                        merchant.deleteRequestedAt(), merchant.deleteReason(), Instant.now()));
            }
        }

        @Override
        public long countMerchantReferences(String merchantId) {
            return merchantReferenceCount;
        }

        @Override
        public ProjectRecord saveProject(ProjectRecord project) {
            projects.put(project.id(), project);
            return project;
        }

        @Override
        public List<ProjectRecord> listProjects() {
            return new ArrayList<>(projects.values());
        }

        @Override
        public SpaceRecord saveSpace(SpaceRecord space) {
            spaces.put(space.id(), space);
            return space;
        }

        @Override
        public List<SpaceRecord> listSpaces() {
            return new ArrayList<>(spaces.values());
        }

        private final Map<String, SpaceTypeRecord> spaceTypes = seedSpaceTypes();

        private static Map<String, SpaceTypeRecord> seedSpaceTypes() {
            Map<String, SpaceTypeRecord> m = new LinkedHashMap<>();
            int sort = 10;
            for (String code : List.of("building", "floor", "zone", "shop", "public_area", "parking", "booth", "other")) {
                m.put(code, new SpaceTypeRecord("sptype_" + code, code, code, sort, "active",
                        java.time.Instant.EPOCH, java.time.Instant.EPOCH));
                sort += 10;
            }
            return m;
        }

        @Override
        public List<String> listActiveSpaceTypeCodes() {
            return spaceTypes.values().stream().filter(t -> "active".equals(t.status()))
                    .map(SpaceTypeRecord::typeCode).toList();
        }

        @Override
        public List<SpaceTypeRecord> listSpaceTypes() {
            return new ArrayList<>(spaceTypes.values());
        }

        @Override
        public SpaceTypeRecord saveSpaceType(SpaceTypeRecord t) {
            spaceTypes.put(t.typeCode(), t);
            return t;
        }

        @Override
        public long countSpacesByTypeCode(String typeCode) {
            return spaces.values().stream().filter(space -> typeCode.equals(space.spaceType())).count();
        }

        @Override
        public java.util.Optional<SpaceTypeRecord> findSpaceTypeById(String id) {
            return spaceTypes.values().stream().filter(item -> id.equals(item.id())).findFirst();
        }

        @Override
        public void deleteSpaceType(String id) {
            spaceTypes.replaceAll((code, item) -> id.equals(item.id())
                    ? new SpaceTypeRecord(item.id(), item.typeCode(), item.typeName(), item.sortOrder(),
                    "disabled", item.createdAt(), java.time.Instant.now())
                    : item);
        }

        private final java.util.Map<String, String> deleteRequestedBy = new java.util.HashMap<>();

        @Override
        public void saveSpaceTypeDeleteRequest(String spaceTypeId, String reason, String requestedBy) {
            deleteRequestedBy.put(spaceTypeId, requestedBy);
        }

        @Override
        public String findSpaceTypeDeleteRequestedBy(String spaceTypeId) {
            return deleteRequestedBy.get(spaceTypeId);
        }

        @Override
        public BusinessEntityBindingRecord saveBusinessEntityBinding(BusinessEntityBindingRecord binding) {
            businessEntityBindings.put(binding.id(), binding);
            businessEntityBindingStatusHistory.add(binding.status());
            return binding;
        }

        @Override
        public List<BusinessEntityBindingRecord> listBusinessEntityBindings() {
            return new ArrayList<>(businessEntityBindings.values());
        }

        @Override
        public Optional<BusinessEntityBindingRecord> findBusinessEntityBindingById(String id) {
            return Optional.ofNullable(businessEntityBindings.get(id));
        }

        @Override
        public Optional<BusinessEntityBindingRecord> findActiveBusinessEntityBindingByMerchantId(String merchantId) {
            return businessEntityBindings.values().stream()
                    .filter(binding -> merchantId.equals(binding.merchantId()))
                    .filter(binding -> "active".equals(binding.status()))
                    .findFirst();
        }



    }
}
