package com.complex.platform.governance;
// 文件版本：V0.17

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
import com.complex.platform.governance.dto.ProjectAssociationRequest;
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
        platformRepository.saveProject(new ProjectRecord("org_1", null, "complex", "当前项目", null, null,
                null, "active", Instant.EPOCH, Instant.EPOCH));
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }


    @Test
    void merchantViewAuditRecordsOnlyReturnedPageAndQueryContext() {
        MerchantRecord first = seedMerchant("audit-first");
        MerchantRecord second = seedMerchant("audit-second");
        service.createMerchant(new MerchantProfileRequest(
                "audit-bar", "seed", List.of(), "筛选外商户", "bar", "be_1",
                null, null, "Jane", "18800000011",
                null, null, null, null, null, null, null, null, "active"));

        assertThat(service.listMerchants(false, " other ", 2, 1).items())
                .extracting(MerchantRecord::id).containsExactly(second.id());
        service.listMerchants(true, null, 0, 0);
        service.listMerchants(false, "bar", 99, 1);

        List<AuditEvent> views = auditEventRepository.events.stream()
                .filter(event -> event.eventName().equals("merchant.viewed")).toList();
        assertThat(views).hasSize(3);
        assertThat(views.get(0).metadata())
                .containsEntry("merchantIds", List.of(second.id()))
                .containsEntry("entry", "/api/v1/platform/merchants")
                .containsEntry("projectId", "org_1")
                .containsEntry("includeArchived", false)
                .containsEntry("businessFormat", "other")
                .containsEntry("page", 2).containsEntry("pageSize", 1)
                .containsEntry("count", 1).containsEntry("total", 2L);
        assertThat(views.get(0).metadata().toString())
                .doesNotContain(first.id(), "Jane", "18800000011");
        assertThat(views.get(1).metadata())
                .containsEntry("includeArchived", true)
                .containsEntry("businessFormat", "")
                .containsEntry("page", 1).containsEntry("pageSize", 500)
                .containsEntry("count", 3).containsEntry("total", 3L);
        assertThat(views.get(2).metadata())
                .containsEntry("merchantIds", List.of())
                .containsEntry("businessFormat", "bar")
                .containsEntry("page", 99).containsEntry("pageSize", 1)
                .containsEntry("count", 0).containsEntry("total", 1L);
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
        if (!platformRepository.businessEntityExists("be_1")) seedEntity("be_1");
        return service.createMerchant(new MerchantProfileRequest(
                key, "seed", List.of(), "存档删除测试商户", "other", "be_1",
                null, null, "Jane", "18800000011",
                null, null, null, null, null, null, null, null, "active"));
    }

    /** M009-006：绑定引用的主体须真实存在——直接落一条主体行（不经服务，避免干扰幂等计数断言）。 */
    private void seedEntity(String id) {
        platformRepository.saveBusinessEntity(new BusinessEntityRecord(
                id, id, "management_company", null, null, null, "active", Instant.EPOCH, Instant.EPOCH));
        platformRepository.associateBusinessEntityProject(id, "org_1");
    }

    /** M009-009：最高权限（持授权 U 盘密钥者）身份——与生产 SessionIssuer 相同的最高权限三件套（BSM-004/007）。 */
    private static void asMasterAuthority() {
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active");
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
                "sp-bad-mch", "create", List.of(), "org_1", "mch_missing", null, "A", "shop", null, null, null, "active")))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("MERCHANT_NOT_FOUND"));
        assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
                "sp-bad-parent", "create", List.of(), "org_1", null, "sp_missing", "A", "shop", null, null, null, "active")))
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
                "sp-archived-mch", "create", List.of(), "org_1", merchant.id(), null, "A", "shop", null, null, null, "active")))
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
                "sp-bad-status", "create", List.of(), "org_1", null, null, "A", "shop", null, null, null, "zzz")))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("VALIDATION_FAILED"));
        assertThat(platformRepository.listSpaces()).noneMatch(s -> "zzz".equals(s.status()));
        assertThat(service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
                "sp-unavailable", "create", List.of(), "org_1", null, null, "B", "shop", null, null, null, "unavailable")).status())
                .isEqualTo("unavailable");
    }

    // ---- M009-008：生效绑定同步商户三个主体列（PGM-026「引用同一组绑定」） ----
    @Test
    void createBusinessEntityBindingSyncsMerchantEntityColumns() {
        MerchantRecord merchant = unboundMerchant("seed-sync");
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

    @Test
    void requestDeleteMerchantWithoutArchiveIsRejected() {
        MerchantRecord merchant = seedMerchant("seed-no-archive");
        assertThatThrownBy(() -> service.requestDeleteMerchant(merchant.id(),
                new MerchantDeleteRequest("delete-no-archive", "跳过存档")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("MERCHANT_NOT_ARCHIVED"));
    }

    @Test
    void requestDeleteOnAlreadyDeletedMerchantIsRejected() {
        MerchantRecord merchant = seedMerchant("seed-already-del");
        service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("archive-ad", "测试存档理由"));
        platformRepository.merchantReferenceCount = 0;
        asMasterAuthority();
        service.requestDeleteMerchant(merchant.id(), new MerchantDeleteRequest("delete-ad-1", "第一次删除"));
        TestSecurityActors.asPlatformAdmin();
        assertThatThrownBy(() -> service.requestDeleteMerchant(merchant.id(),
                new MerchantDeleteRequest("delete-ad-2", "再申请一次")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("MERCHANT_ALREADY_DELETED"));
    }

    @Test
    void requesterCannotApproveOwnMerchantDelete() {
        MerchantRecord merchant = seedMerchant("seed-self-appr");
        service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("archive-sa", "测试存档理由"));
        TestSecurityActors.asPlatformAdmin(SessionIssuer.MASTER_USER_ID);
        service.requestDeleteMerchant(merchant.id(), new MerchantDeleteRequest("delete-sa", "自己申请"));
        asMasterAuthority();
        assertThatThrownBy(() -> service.approveDeleteMerchant(merchant.id(),
                new MerchantDeleteApprovalRequest("delete-sa-appr", "approved", null)))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("MERCHANT_SELF_APPROVE_FORBIDDEN"));
    }

    @Test
    void masterDirectDeleteStillRejectedWhenMerchantReferenced() {
        MerchantRecord merchant = seedMerchant("seed-master-inuse");
        service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("archive-mi", "测试存档理由"));
        platformRepository.merchantReferenceCount = 3;
        asMasterAuthority();
        assertThatThrownBy(() -> service.requestDeleteMerchant(merchant.id(),
                new MerchantDeleteRequest("delete-mi", "密钥直办仍占用")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("MERCHANT_IN_USE"));
    }

    // ---- M009-014：字典分页元数据须覆盖全部条目 ----
    @Test
    void listSpaceTypesPageSizeCoversAllItems() {
        var page = service.listSpaceTypes();
        assertThat(page.pageSize()).isGreaterThanOrEqualTo(page.items().size());
        assertThat(page.pageSize()).isEqualTo(500);
    }

    @Test
    void createSpaceTypeRejectsExistingCodeEvenWhenActive() {
        assertThatThrownBy(() -> service.createSpaceType(new com.complex.platform.governance.dto.SpaceTypeRequest(
                "st-dup-shop", "add", List.of(), "shop", "改名铺位", 1, "active")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("SPACE_TYPE_CODE_EXISTS"));
    }

    @Test
    void createSpaceTypeIgnoresRequestedDisabledAndStaysActive() {
        SpaceTypeRecord created = service.createSpaceType(new com.complex.platform.governance.dto.SpaceTypeRequest(
                "st-force-active", "add", List.of(), "tempkind", "临时种类", 10, "disabled"));
        assertThat(created.status()).isEqualTo("active");
    }

    @Test
    void archiveSpaceTypeRequiresReasonAndCanUnarchive() {
        service.archiveSpaceType("sptype_booth", "暂时不用");
        assertThat(platformRepository.findSpaceTypeById("sptype_booth").orElseThrow().status()).isEqualTo("archived");
        service.unarchiveSpaceType("sptype_booth");
        assertThat(platformRepository.findSpaceTypeById("sptype_booth").orElseThrow().status()).isEqualTo("active");
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
        service.archiveSpaceType(created.id(), "不再使用");
        service.requestDeleteSpaceType(created.id(), "不再使用");
        assertThat(platformRepository.findSpaceTypeById(created.id()).orElseThrow().status()).isEqualTo("pending_delete");
        assertThatThrownBy(() -> service.approveDeleteSpaceType(created.id(), "approved"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("SPACE_TYPE_DELETE_APPROVER_NOT_MASTER"));
        asMasterAuthority();
        service.approveDeleteSpaceType(created.id(), "approved");
        assertThat(platformRepository.findSpaceTypeById(created.id()).orElseThrow().status()).isEqualTo("disabled");
        assertThatThrownBy(() -> service.createSpaceType(new com.complex.platform.governance.dto.SpaceTypeRequest(
                "st-revive", "add", List.of(), "tempdel", "复活", 20, "active")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("SPACE_TYPE_CODE_EXISTS"));
    }

    @Test
    void occupiedSpaceTypeCannotBeDeleted() {
        service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
                "sp-shop-occ", "create", List.of(), "org_1", null, null, "Shop Occ", "shop", "1", null, null, "active"));
        service.archiveSpaceType("sptype_shop", "测试占用");
        assertThatThrownBy(() -> service.requestDeleteSpaceType("sptype_shop", "想删"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("SPACE_TYPE_IN_USE"));
    }

    @Test
    void masterRequestDeleteSpaceTypeExecutesWithoutApproval() {
        SpaceTypeRecord created = service.createSpaceType(new com.complex.platform.governance.dto.SpaceTypeRequest(
                "st-master-del", "add", List.of(), "masterdel", "密钥直删", 30, "active"));
        service.archiveSpaceType(created.id(), "密钥持有人清理");
        asMasterAuthority();
        service.requestDeleteSpaceType(created.id(), "密钥持有人清理");
        assertThat(platformRepository.findSpaceTypeById(created.id()).orElseThrow().status()).isEqualTo("disabled");
    }

    @Test
    void archiveCannotPullPendingDeleteBack() {
        SpaceTypeRecord created = service.createSpaceType(new com.complex.platform.governance.dto.SpaceTypeRequest(
                "st-pend", "add", List.of(), "pendkind", "待审种类", 40, "active"));
        service.archiveSpaceType(created.id(), "先存档");
        service.requestDeleteSpaceType(created.id(), "申请删");
        assertThatThrownBy(() -> service.archiveSpaceType(created.id(), "想撤回"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("SPACE_TYPE_ARCHIVE_NOT_ALLOWED"));
        asMasterAuthority();
        service.approveDeleteSpaceType(created.id(), "rejected");
        assertThat(platformRepository.findSpaceTypeById(created.id()).orElseThrow().status()).isEqualTo("archived");
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
        seedEntity("be_1");
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
        TestSecurityActors.asPlatformAdmin();
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
        TestSecurityActors.asPlatformAdmin();
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
        TestSecurityActors.asPlatformAdmin();
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
                "org_1",
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
        MerchantRecord merchant = unboundMerchant("seed-binding"); // M009-006：经营对象与主体须存在
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
    void activeBindingCannotBeDisabledToReplaceFinancialSubjects() {
        MerchantRecord merchant = unboundMerchant("active-merchant");
        seedEntity("active-company");
        var binding = service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
                "active-create", null, List.of(), merchant.id(), "active-company", "active-company",
                "active-company", LocalDate.now(), "初始生效"));
        for (String action : List.of("inactive", "suspended")) {
            assertThatThrownBy(() -> service.changeBusinessEntityBinding(binding.id(),
                    new StatusChangeRequest("stop-" + action, action, "停用后改主体", List.of())))
                    .isInstanceOf(ApiException.class);
        }
        assertThat(platformRepository.findBusinessEntityBindingById(binding.id())).contains(binding);
        assertThat(platformRepository.findMerchantById(merchant.id()).orElseThrow().settlementEntityId())
                .isEqualTo("active-company");
    }


    // P1-4 red→green: action 字段拒绝非法枚举值
    @Test
    void changeBusinessEntityBindingRejectsInvalidAction() {
        MerchantRecord merchant = unboundMerchant("seed-enum"); // M009-006
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
                "sp-kiosk", "create", List.of(), "org_1", null, null, "K1", "kiosk", null, null, null, "active")); // M009-039：归属为当前真实项目
        assertThat(space.spaceType()).isEqualTo("kiosk");
    }

    @Test
    void createSpaceRejectsUnknownSpaceType() {
        // 条目3 硬校验：不在字典内的 space_type → VALIDATION_FAILED。
        assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
                        "sp-bad", "create", List.of(), "org_1", null, null, "Bad", "no_such_type", null, null, null, "active")))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class, ex ->
                        assertThat(ex.code()).isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    void createSpaceAcceptsDictionarySpaceType() {
        SpaceRecord saved = service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
                "sp-ok", "create", List.of(), "org_1", null, null, "Shop A", "shop", "1", null, null, "active"));
        assertThat(saved.spaceType()).isEqualTo("shop");
    }

    private MerchantRecord unboundMerchant(String key) {
        return service.createMerchant(new MerchantProfileRequest(
                key, "seed", List.of(), "待绑定商户", "other", null, null, null,
                "Jane", "18800000011", null, null, null, null, null, null, null, null, "active"));
    }

    @Test
    void futureBindingDoesNotChangeCurrentMerchantSubjects() {
        MerchantRecord merchant = unboundMerchant("future-merchant");
        seedEntity("future-company");
        var binding = service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
                "future-binding", null, List.of(), merchant.id(), "future-company", "future-company",
                "future-company", LocalDate.now().plusDays(10), "未来生效"));
        assertThat(binding.status()).isEqualTo("inactive");
        assertThat(binding.effectiveTo()).isNull();
        MerchantRecord saved = platformRepository.findMerchantById(merchant.id()).orElseThrow();
        assertThat(saved.businessEntityId()).isNull();
        assertThat(saved.contractEntityId()).isNull();
        assertThat(saved.settlementEntityId()).isNull();
    }

    @Test
    void anotherFutureBindingCannotReserveTheSameMerchant() {
        MerchantRecord merchant = unboundMerchant("reserved-merchant");
        seedEntity("reserved-company");
        service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
                "reserve-first", null, List.of(), merchant.id(), "reserved-company", "reserved-company",
                "reserved-company", LocalDate.now().plusDays(10), "预约"));
        assertThatThrownBy(() -> service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
                "reserve-second", null, List.of(), merchant.id(), "reserved-company", "reserved-company",
                "reserved-company", LocalDate.now().plusDays(11), "第二条")))
                .isInstanceOf(ApiException.class);
        assertThat(platformRepository.listBusinessEntityBindings()).hasSize(1);
    }

    @Test
    void futureBindingCannotActivateOrEndBeforeItsStart() {
        MerchantRecord merchant = unboundMerchant("early-merchant");
        seedEntity("early-company");
        var binding = service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
                "early-create", null, List.of(), merchant.id(), "early-company", "early-company",
                "early-company", LocalDate.now().plusDays(10), "未来生效"));
        assertThatThrownBy(() -> service.changeBusinessEntityBinding(binding.id(),
                new StatusChangeRequest("early-activate", "active", "提前启用", List.of())))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.changeBusinessEntityBinding(binding.id(),
                new StatusChangeRequest("early-end", "inactive", "提前结束", List.of())))
                .isInstanceOf(ApiException.class);
        assertThat(platformRepository.findBusinessEntityBindingById(binding.id())).contains(binding);
    }

    @Test
    void dueBindingCanActivateButAnEndedHistoricalPeriodCannotReopen() {
        MerchantRecord merchant = unboundMerchant("due-merchant");
        seedEntity("due-company");
        var due = new BusinessEntityBindingRecord("due-binding", merchant.id(), "due-company", "due-company",
                "due-company", "inactive", LocalDate.now(), null, "到期", Instant.EPOCH, Instant.EPOCH);
        platformRepository.saveBusinessEntityBinding(due);
        assertThat(service.changeBusinessEntityBinding(due.id(),
                new StatusChangeRequest("due-activate", "active", "到期启用", List.of())).status()).isEqualTo("active");
        assertThat(platformRepository.findMerchantById(merchant.id()).orElseThrow().settlementEntityId())
                .isEqualTo("due-company");

        MerchantRecord historicalMerchant = unboundMerchant("ended-history-merchant");
        var ended = new BusinessEntityBindingRecord("ended-binding", historicalMerchant.id(), "due-company", "due-company",
                "due-company", "inactive", LocalDate.now().minusDays(5), LocalDate.now().minusDays(1),
                "历史", Instant.EPOCH, Instant.EPOCH);
        platformRepository.saveBusinessEntityBinding(ended);
        assertThatThrownBy(() -> service.changeBusinessEntityBinding(ended.id(),
                new StatusChangeRequest("reopen-history", "active", "重开历史", List.of())))
                .isInstanceOf(ApiException.class);
        assertThat(platformRepository.findBusinessEntityBindingById(ended.id())).contains(ended);
    }

    @Test
    void bindingCannotReplaceExistingFinancialSubjectsEvenForMasterAuthority() {
        MerchantRecord merchant = unboundMerchant("existing-subject-merchant");
        platformRepository.saveMerchant(merchant.withEntities("known-company", "known-company", "known-company", Instant.now()));
        seedEntity("different-company");
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "org_1", null, "active");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
        assertThatThrownBy(() -> service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
                "replace-subjects", null, List.of(), merchant.id(), "different-company", "different-company",
                "different-company", LocalDate.now(), "不经财税审核替换")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW"));
        assertThat(platformRepository.findMerchantById(merchant.id()).orElseThrow().settlementEntityId())
                .isEqualTo("known-company");
        assertThat(platformRepository.listBusinessEntityBindings()).isEmpty();
    }

    @Test
    void projectCreationReturnsApprovedOptionalProfileFields() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ProjectRequest request = mapper.convertValue(Map.of(
                "idempotencyKey", "project-profile", "name", "甲综合体", "venueType", "bar",
                "status", "draft", "ownerPerson", "王负责人", "address", "甲路一号",
                "businessScope", "餐饮与零售"), ProjectRequest.class);
        var result = mapper.valueToTree(service.createProject(request));
        assertThat(result.path("ownerPerson").asText()).isEqualTo("王负责人");
        assertThat(result.path("address").asText()).isEqualTo("甲路一号");
        assertThat(result.path("businessScope").asText()).isEqualTo("餐饮与零售");
    }

    @Test
    void deletedSpaceTypeCanBeRestoredButPendingRequestCannotBeBypassed() {
        service.archiveSpaceType("sptype_booth", "暂不用");
        service.requestDeleteSpaceType("sptype_booth", "删除错误配置");
        assertThatThrownBy(() -> service.unarchiveSpaceType("sptype_booth")).isInstanceOf(ApiException.class);
        asMasterAuthority();
        service.approveDeleteSpaceType("sptype_booth", "approved");
        org.assertj.core.api.Assertions.assertThatCode(() -> service.unarchiveSpaceType("sptype_booth"))
                .doesNotThrowAnyException();
        assertThat(platformRepository.findSpaceTypeById("sptype_booth").orElseThrow().status()).isEqualTo("active");
    }

    @Test
    void spaceTypeDeletionCompletesOnlyTheCurrentRequest() {
        service.archiveSpaceType("sptype_booth", "暂不用");
        service.requestDeleteSpaceType("sptype_booth", "申请");
        asMasterAuthority();
        service.approveDeleteSpaceType("sptype_booth", "rejected");
        assertThat(platformRepository.findSpaceTypeDeleteRequestedBy("sptype_booth")).isNull();
        service.requestDeleteSpaceType("sptype_booth", "最高权限直办");
        assertThat(platformRepository.findSpaceTypeDeleteRequestedBy("sptype_booth")).isNull();
        assertThat(platformRepository.findSpaceTypeById("sptype_booth").orElseThrow().status()).isEqualTo("disabled");
    }

    @Test
    void spaceTypeAuditHasObjectIdAndActualRejectedDecision() {
        var type = service.createSpaceType(new com.complex.platform.governance.dto.SpaceTypeRequest(
                "audit-type", null, List.of(), "auditable_type", "审计类型", 10, "active"));
        assertThat(auditEventRepository.events).filteredOn(e -> "space_type.updated".equals(e.eventName()))
                .singleElement().satisfies(e -> assertThat(e.businessId()).isEqualTo(type.id()));
        service.archiveSpaceType(type.id(), "存档原因");
        service.requestDeleteSpaceType(type.id(), "删除原因");
        asMasterAuthority();
        service.approveDeleteSpaceType(type.id(), "rejected");
        assertThat(auditEventRepository.events).filteredOn(e -> "space_type.delete_rejected".equals(e.eventName()))
                .singleElement().satisfies(e -> {
                    assertThat(e.businessId()).isEqualTo(type.id());
                    assertThat(e.metadata()).containsEntry("decision", "rejected");
                });
        assertThat(auditEventRepository.events).filteredOn(e -> "space_type.delete_requested".equals(e.eventName()))
                .singleElement().satisfies(e -> assertThat(e.metadata()).containsEntry("reason", "删除原因"));
    }

    @Test
    void merchantDeleteAuditKeepsReasonAndRecordsRejectionAgainstTheMerchant() {
        MerchantRecord merchant = seedMerchant("audit-merchant");
        service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("audit-archive", "存档原因"));
        service.requestDeleteMerchant(merchant.id(), new MerchantDeleteRequest("audit-delete", "删除原因"));
        asMasterAuthority();
        service.approveDeleteMerchant(merchant.id(), new MerchantDeleteApprovalRequest("audit-reject", "rejected", "核实保留"));
        assertThat(auditEventRepository.events).filteredOn(e -> "merchant.delete_requested".equals(e.eventName()))
                .singleElement().satisfies(e -> assertThat(e.metadata()).containsEntry("reason", "删除原因"));
        assertThat(auditEventRepository.events).filteredOn(e -> "merchant.delete_rejected".equals(e.eventName()))
                .singleElement().satisfies(e -> {
                    assertThat(e.businessId()).isEqualTo(merchant.id());
                    assertThat(e.metadata()).containsEntry("decision", "rejected").containsEntry("comment", "核实保留");
                });
    }

    private MerchantProfileRequest numericMerchant(String key, String status,
            BigDecimal area, BigDecimal leasedArea, BigDecimal rent) {
        return new MerchantProfileRequest(key, null, List.of(), "数值验证商户", "other",
                null, null, null, null, null, null, null, area, leasedArea, null, null, rent, null, status);
    }

    @Test
    void negativeCapacityAreasAndRentAreRejectedWhileZeroAndNullRemainValid() {
        assertThatThrownBy(() -> service.createProject(new ProjectRequest(
                "negative-capacity", null, List.of(), null, "bar", "项目", null, null, -1, "draft")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        for (MerchantProfileRequest request : List.of(
                numericMerchant("negative-area", "active", new BigDecimal("-1"), null, null),
                numericMerchant("negative-leased", "active", null, new BigDecimal("-1"), null),
                numericMerchant("negative-rent", "active", null, null, new BigDecimal("-1")))) {
            assertThatThrownBy(() -> service.createMerchant(request))
                    .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        }
        assertThat(service.createProject(new ProjectRequest(
                "zero-capacity", null, List.of(), null, "bar", "项目", null, null, 0, "draft")).capacity()).isZero();
        assertThat(service.createMerchant(numericMerchant("zero-values", "active",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)).rentPerSquareMeter()).isZero();
        assertThat(service.createMerchant(numericMerchant("null-values", "active", null, null, null)).areaSquareMeter()).isNull();
    }

    @Test
    void ordinaryMerchantProfileCannotCauseImmediateExit() {
        assertThatThrownBy(() -> service.createMerchant(numericMerchant("new-exit", "terminated", null, null, null)))
                .isInstanceOf(ApiException.class);
        MerchantRecord merchant = unboundMerchant("exit-merchant");
        assertThatThrownBy(() -> service.updateMerchant(merchant.id(),
                numericMerchant("update-exit", "terminated", null, null, null))).isInstanceOf(ApiException.class);
        assertThat(platformRepository.findMerchantById(merchant.id()).orElseThrow().status()).isEqualTo("active");
    }

    @Test
    void userFacingRoleValidationDoesNotIncludeEnglishEnumNames() {
        assertThatThrownBy(() -> service.createBusinessEntity(new BusinessEntityRequest(
                "invalid-role-message", null, List.of(), "公司", "unknown", null, null, null, "active")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
                    assertThat(e.getMessage()).doesNotContain("platform", "project_company", "management_company");
                });
    }

    @Test
    void savingAnAlreadyTerminatedMerchantDoesNotReactivateItWhenStatusIsOmitted() throws Exception {
        MerchantRecord merchant = unboundMerchant("already-terminated");
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        var data = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.valueToTree(merchant);
        data.put("status", "terminated");
        platformRepository.saveMerchant(mapper.treeToValue(data, MerchantRecord.class));
        assertThat(service.updateMerchant(merchant.id(),
                numericMerchant("same-terminated", "terminated", null, null, null)).status()).isEqualTo("terminated");
        assertThat(service.updateMerchant(merchant.id(),
                numericMerchant("omitted-status", null, null, null, null)).status()).isEqualTo("terminated");
    }

    private void twoProjectFixtures() {
        for (String id : List.of("org_1", "other-project")) {
            platformRepository.saveProject(new ProjectRecord(id, null, "bar", id, null, null, null,
                    "active", Instant.EPOCH, Instant.EPOCH));
        }
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        for (String suffix : List.of("a", "b")) {
            String project = "a".equals(suffix) ? "org_1" : "other-project";
            platformRepository.saveMerchant(mapper.convertValue(Map.of(
                    "id", "scoped-merchant-" + suffix, "merchantName", suffix, "merchantType", "other",
                    "status", "active", "projectId", project, "deleteStatus", "none",
                    "createdAt", Instant.EPOCH, "updatedAt", Instant.EPOCH), MerchantRecord.class));
            platformRepository.saveSpace(new SpaceRecord("scoped-space-" + suffix, project,
                    "scoped-merchant-" + suffix, null, suffix, "shop", null, null, List.of(),
                    "active", Instant.EPOCH, Instant.EPOCH));
            seedEntity("scoped-company-" + suffix);
            platformRepository.businessEntityProjects.put("scoped-company-" + suffix, java.util.Set.of(project));
            platformRepository.saveBusinessEntityBinding(new BusinessEntityBindingRecord("scoped-binding-" + suffix,
                    "scoped-merchant-" + suffix, "scoped-company-" + suffix, "scoped-company-" + suffix,
                    "scoped-company-" + suffix, "inactive", LocalDate.now().plusDays(1), null,
                    null, Instant.EPOCH, Instant.EPOCH));
        }
    }

    @Test
    void projectListsAndTotalsStayWithinCurrentProjectForOrdinaryAdmin() {
        twoProjectFixtures();
        org.junit.jupiter.api.Assertions.assertAll(
                () -> { var p = service.listProjects(1, 1); assertThat(p.total()).isEqualTo(1);
                    assertThat(p.items()).extracting(ProjectRecord::id).containsExactly("org_1"); },
                () -> { var p = service.listSpaces(1, 1); assertThat(p.total()).isEqualTo(1);
                    assertThat(p.items()).extracting(SpaceRecord::id).containsExactly("scoped-space-a"); },
                () -> { var p = service.listMerchants(true, null, 1, 1); assertThat(p.total()).isEqualTo(1);
                    assertThat(p.items()).extracting(MerchantRecord::id).containsExactly("scoped-merchant-a"); },
                () -> { var p = service.listBusinessEntities(1, 1); assertThat(p.total()).isEqualTo(1);
                    assertThat(p.items()).extracting(BusinessEntityRecord::id).containsExactly("scoped-company-a"); },
                () -> { var p = service.listBusinessEntityBindings(1, 1); assertThat(p.total()).isEqualTo(1);
                    assertThat(p.items()).extracting(BusinessEntityBindingRecord::id).containsExactly("scoped-binding-a"); });
    }

    @Test
    void employeeCannotReadOtherProjectThroughSpaceLookupOrWrite() {
        twoProjectFixtures();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor("employee-a", "employee-a", "employee", "org_1", null, "active"), "token", List.of()));
        org.junit.jupiter.api.Assertions.assertAll(
                () -> assertThat(service.spaceExists("scoped-space-b")).isFalse(),
                () -> assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
                        "cross-parent", null, List.of(), "org_1", null, "scoped-space-b", "空间", "shop",
                        null, null, null, "active"))).isInstanceOf(ApiException.class),
                () -> assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
                        "cross-merchant", null, List.of(), "org_1", "scoped-merchant-b", null, "空间", "shop",
                        null, null, null, "active"))).isInstanceOf(ApiException.class));
    }

    @Test
    void spaceRequiresCurrentProjectAndNonnegativeArea() {
        twoProjectFixtures();
        org.junit.jupiter.api.Assertions.assertAll(
                () -> assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
                        "missing-project", null, List.of(), null, null, null, "空间", "shop",
                        null, null, null, "active"))).isInstanceOf(ApiException.class),
                () -> assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
                        "cross-project", null, List.of(), "other-project", null, null, "空间", "shop",
                        null, null, null, "active"))).isInstanceOf(ApiException.class),
                () -> assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
                        "negative-space", null, List.of(), "org_1", null, null, "空间", "shop",
                        null, null, new BigDecimal("-1"), "active"))).isInstanceOf(ApiException.class));
    }

    @Test
    void initialMerchantRejectsUnknownOrForeignBusinessEntities() {
        twoProjectFixtures();
        ObjectMapper mapper = new ObjectMapper()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        for (String entity : List.of("missing-company", "scoped-company-b")) {
            MerchantProfileRequest request = mapper.convertValue(Map.of(
                    "idempotencyKey", "initial-" + entity, "merchantName", "商户", "merchantType", "other",
                    "status", "active", "businessEntityId", entity), MerchantProfileRequest.class);
            assertThatThrownBy(() -> service.createMerchant(request)).isInstanceOf(ApiException.class);
        }
    }

    @Test
    void ordinaryProfileUpdateCannotClaimOrMoveProject() {
        twoProjectFixtures();
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        platformRepository.saveMerchant(mapper.convertValue(Map.of(
                "id", "legacy-unassigned", "merchantName", "旧商户", "merchantType", "other", "status", "active",
                "deleteStatus", "none", "createdAt", Instant.EPOCH, "updatedAt", Instant.EPOCH), MerchantRecord.class));
        org.junit.jupiter.api.Assertions.assertAll(
                () -> assertThatThrownBy(() -> service.updateMerchant("scoped-merchant-a", mapper.convertValue(Map.of(
                        "idempotencyKey", "move-project", "merchantName", "商户", "merchantType", "other",
                        "projectId", "other-project", "status", "active"), MerchantProfileRequest.class)))
                        .isInstanceOf(ApiException.class),
                () -> assertThatThrownBy(() -> service.updateMerchant("legacy-unassigned", mapper.convertValue(Map.of(
                        "idempotencyKey", "claim-project", "merchantName", "商户", "merchantType", "other",
                        "projectId", "org_1", "status", "active"), MerchantProfileRequest.class)))
                        .isInstanceOf(ApiException.class));
    }


    @Test
    void masterProjectBackfillChangesOnlyTheProjectAndCannotMoveIt() {
        twoProjectFixtures();
        MerchantRecord original = new MerchantRecord("legacy-backfill", "旧商户", "other", "financial-original",
                "contract-original", "settlement-original", "联系人", "18800000011", "负责人", "A1",
                BigDecimal.TEN, BigDecimal.ONE, "第二联系人", "18800000012", BigDecimal.ZERO, "lease",
                "suspended", Instant.EPOCH, Instant.EPOCH).withArchive(Instant.EPOCH, "operator", Instant.EPOCH)
                .withDelete("pending", "operator", Instant.EPOCH, "保留理由", Instant.EPOCH);
        platformRepository.saveMerchant(original);
        asMasterAuthority();
        MerchantRecord saved = service.assignMerchantProject(original.id(),
                new ProjectAssociationRequest("project-backfill", "org_1", "核实原始归属"));
        assertThat(saved).isEqualTo(original.withProject("org_1"));
        assertThat(service.assignMerchantProject(original.id(),
                new ProjectAssociationRequest("project-backfill", "org_1", "核实原始归属"))).isEqualTo(saved);
        assertThat(service.assignMerchantProject(original.id(),
                new ProjectAssociationRequest("project-same", "org_1", "确认同值"))).isEqualTo(saved);
        assertThatThrownBy(() -> service.assignMerchantProject(original.id(),
                new ProjectAssociationRequest("project-move", "other-project", "迁移")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("MERCHANT_PROJECT_ALREADY_ASSIGNED"));
        assertThat(auditEventRepository.events).filteredOn(e -> "merchant.project_assigned".equals(e.eventName()))
                .hasSize(2).allSatisfy(e -> {
                    assertThat(e.businessId()).isEqualTo(original.id());
                    assertThat(e.metadata()).containsEntry("projectId", "org_1").containsKey("reason");
                    assertThat(e.actorId()).isEqualTo(SessionIssuer.MASTER_USER_ID);
                });
    }

    @Test
    void companyCanBeAssociatedWithTwoProjectsOnlyByRealMaster() {
        twoProjectFixtures();
        asMasterAuthority();
        assertThat(service.associateBusinessEntityProject("scoped-company-a",
                new ProjectAssociationRequest("company-link", "other-project", "确认共同运营"))).isNotNull();
        assertThat(platformRepository.businessEntityInProject("scoped-company-a", "org_1")).isTrue();
        assertThat(platformRepository.businessEntityInProject("scoped-company-a", "other-project")).isTrue();
        BusinessEntityRecord created = service.createBusinessEntity(new BusinessEntityRequest(
                "master-company-create", null, List.of(), "新关联公司", "management_company", null, null, null, "active", "org_1"));
        assertThat(platformRepository.businessEntityInProject(created.id(), "org_1")).isTrue();
        TestSecurityActors.asPlatformAdmin();
        assertThatThrownBy(() -> service.associateBusinessEntityProject("scoped-company-b",
                new ProjectAssociationRequest("ordinary-link", "org_1", "普通认领"))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.assignMerchantProject("scoped-merchant-b",
                new ProjectAssociationRequest("ordinary-assign", "org_1", "普通认领"))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.createBusinessEntity(new BusinessEntityRequest(
                "ordinary-company-foreign", null, List.of(), "其它项目", "management_company", null, null, null, "active", "other-project")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void associationRequiresExistingExplicitProjectReasonAndIdempotency() {
        twoProjectFixtures();
        asMasterAuthority();
        for (ProjectAssociationRequest request : List.of(
                new ProjectAssociationRequest(null, "org_1", "确认"),
                new ProjectAssociationRequest("missing-project", null, "确认"),
                new ProjectAssociationRequest("unknown-project", "missing", "确认"),
                new ProjectAssociationRequest("missing-reason", "org_1", " "))) {
            assertThatThrownBy(() -> service.assignMerchantProject("scoped-merchant-a", request)).isInstanceOf(ApiException.class);
            assertThatThrownBy(() -> service.associateBusinessEntityProject("scoped-company-a", request)).isInstanceOf(ApiException.class);
        }
        assertThatThrownBy(() -> service.assignMerchantProject("missing",
                new ProjectAssociationRequest("missing-merchant", "org_1", "确认"))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.associateBusinessEntityProject("missing",
                new ProjectAssociationRequest("missing-company", "org_1", "确认"))).isInstanceOf(ApiException.class);
    }

    @Test
    void masterHasNoGlobalListOrOrdinaryProfileBypass() {
        twoProjectFixtures();
        asMasterAuthority();
        org.junit.jupiter.api.Assertions.assertAll(
                () -> assertThat(service.listMerchants(true).total()).isZero(),
                () -> assertThat(service.listBusinessEntities().total()).isZero(),
                () -> assertThat(service.listProjects().total()).isZero(),
                () -> assertThat(service.listSpaces().total()).isZero(),
                () -> assertThat(service.listBusinessEntityBindings().total()).isZero());
        assertThatThrownBy(() -> service.updateMerchant("scoped-merchant-a",
                numericMerchant("master-ordinary-edit", "active", null, null, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PROJECT_SCOPE_MISMATCH"));
    }

    private static final class InMemoryPlatformRepository implements PlatformRepository {
        @Override public void setMerchantArchiveReason(String id, String reason) { }
        @Override public void lockMerchant(String merchantId) { }
        @Override public boolean businessEntityInProject(String entityId, String projectId) {
            return projectId != null && businessEntityProjects.getOrDefault(entityId, java.util.Set.of()).contains(projectId);
        }
        @Override public void associateBusinessEntityProject(String entityId, String projectId) {
            businessEntityProjects.compute(entityId, (ignored, current) -> {
                java.util.Set<String> projects = new java.util.HashSet<>(current == null ? java.util.Set.of() : current);
                projects.add(projectId);
                return projects;
            });
        }
        @Override public boolean assignMerchantProject(String merchantId, String projectId) {
            MerchantRecord merchant = merchants.get(merchantId);
            if (merchant == null || (merchant.projectId() != null && !merchant.projectId().equals(projectId))) return false;
            merchants.put(merchantId, merchant.withProject(projectId));
            return true;
        }
        @Override public void completeSpaceTypeDeleteRequest(String spaceTypeId, String status) {
            deleteRequestedBy.remove(spaceTypeId);
        }
        private final Map<String, BusinessEntityRecord> businessEntities = new LinkedHashMap<>();
        private final Map<String, java.util.Set<String>> businessEntityProjects = new LinkedHashMap<>();
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
