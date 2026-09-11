package com.complex.platform.governance;
// 文件版本：V0.03

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.governance.dto.BusinessEntityBindingRequest;
import com.complex.platform.governance.dto.BusinessEntityRequest;
import com.complex.platform.governance.dto.MerchantProfileRequest;
import com.complex.platform.governance.dto.MerchantDeleteApprovalRequest;
import com.complex.platform.governance.dto.MerchantDeleteRequest;
import com.complex.platform.governance.dto.MerchantArchiveRequest;
import com.complex.platform.governance.dto.ProjectRequest;
import com.complex.platform.governance.dto.SpaceRequest;
import com.complex.platform.governance.dto.StatusChangeRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

@Service
public class PlatformService {

    private static final int DEFAULT_PAGE = 1;
    // AUD-005: 列表端点默认页大小(保留既有一次拉取上限, 供下拉/客户端分页); total 已取真值。
    private static final int LIST_MAX_PAGE_SIZE = 500;

    /**
     * V348 + V384 商户业态六值（真源 HTML §商户管理 + §收入归属：酒吧/餐饮/零售/游乐/传媒/其他，
     * 每个商户必归其一、招商录入必填）。传媒＝演出提供方，其收入按「演出票 → 传媒公司」归传媒公司。
     * 业态＝通票分账按业态拆份额与分流结算的依据；业态主体「娱乐平台公司」即酒吧业态，不另设「娱乐」业态。
     * 与迁移 V348 的 chk_merchants_business_format 同词表：应用层给业务人员看得懂的报错，数据库兜底封口。
     */
    public static final Set<String> MERCHANT_BUSINESS_FORMATS =
            Set.of("bar", "catering", "retail", "amusement", "media", "other");

    /** M009-003：BEQR-148/BSM-028/PGM-026 合作方式六类（自营/租赁/联营/代运营/服务商接入/其他）的技术代码；空＝招商签约阶段尚未确定。 */
    public static final Set<String> MERCHANT_COOPERATION_FORMS =
            Set.of("self_operated", "lease", "joint_operation", "managed_operation", "service_provider", "other");

    /** M009-004：BEQR-155 商户/运营主体可设状态（待准入/正常/暂停/终止/黑名单）；「归档」只经存档端点（archived_at 轴），不作可设代码。 */
    public static final Set<String> SUBJECT_STATUSES =
            Set.of("pending_admission", "active", "suspended", "terminated", "blacklisted");

    /** M009-004：BEQR-154 综合体项目状态六态（草稿/待启用/已启用/暂停/终止/归档）。 */
    public static final Set<String> PROJECT_STATUSES =
            Set.of("draft", "pending_activation", "active", "suspended", "terminated", "archived");

    /** M009-005：PGM-018/019 主体角色——顶层平台 / 城市项目公司 / 域主管公司（业态主体）。 */
    public static final Set<String> BUSINESS_ENTITY_ROLES = Set.of("platform", "project_company", "management_company");

    /** M009-036：空间状态闭集来自已发布迁移 V203 的 chk_spaces_status（active/unavailable），服务层前置校验以中文 400 安全失败。 */
    public static final Set<String> SPACE_STATUSES = Set.of("active", "unavailable");

    private final PlatformRepository repository;
    private final AuditedOperationExecutor operations;
    private final AuditEventService auditEventService;
    // F-AUTHZ-02 治理写仅平台：治理写方法（建商户/法律实体绑定/合同/项目空间等）首行经
    // requirePlatformScope() fail-closed 拒 merchant-scoped 主体，防权限被下放给商户角色后越权治理。
    private final AuthorizationService authorizationService;

    public PlatformService(
            PlatformRepository repository,
            AuditedOperationExecutor operations,
            AuditEventService auditEventService,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.operations = operations;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
    }

    public PageResponse<BusinessEntityRecord> listBusinessEntities() {
        return listBusinessEntities(DEFAULT_PAGE, LIST_MAX_PAGE_SIZE);
    }

    // AUD-005 真分页: SQL COUNT + LIMIT/OFFSET, total 取真值; 默认 500 保留既有前端一次拉取(下拉源)契约。
    public PageResponse<BusinessEntityRecord> listBusinessEntities(int page, int size) {
        PageResponse<BusinessEntityRecord> result = repository.listBusinessEntitiesPaged(page, size);
        auditEventService.recordSuccess(
                "entity_binding.viewed",
                "entity.binding.view",
                "business_entity",
                "all",
                null,
                null,
                Map.of("count", result.items().size(), "total", result.total())
        );
        return result;
    }

    public BusinessEntityRecord createBusinessEntity(BusinessEntityRequest request) {
        authorizationService.requirePlatformScope();
        return executeWrite(
                "createBusinessEntity",
                idempotencyKey(request),
                request,
                BusinessEntityRecord.class,
                "business_entity.changed",
                "business_entity.change_failed",
                "entity.binding.manage",
                "business_entity",
                () -> {
                    String entityName = requireText(request == null ? null : request.entityName(), "entity_name");
                    String entityRole = requireBusinessEntityRole(request == null ? null : request.entityRole());
                    String status = requireSubjectStatus(request.status());
                    Instant now = Instant.now();
                    return repository.saveBusinessEntity(new BusinessEntityRecord(
                            "be_" + UUID.randomUUID(),
                            entityName,
                            entityRole,
                            trimToNull(request.unifiedSocialCreditCode()),
                            trimToNull(request.legalRepresentative()),
                            trimToNull(request.bankAccountRef()),
                            status,
                            now,
                            now
                    ));
                }
        );
    }

    public PageResponse<MerchantRecord> listMerchants(boolean includeArchived) {
        return listMerchants(includeArchived, null, DEFAULT_PAGE, LIST_MAX_PAGE_SIZE);
    }

    /** V348 businessFormat 非空＝只看该业态的商户（分账按业态拆份额、招商/财务按业态筛商户）。 */
    public PageResponse<MerchantRecord> listMerchants(boolean includeArchived, String businessFormat, int page, int size) {
        String format = trimToNull(businessFormat);
        if (format != null && !MERCHANT_BUSINESS_FORMATS.contains(format)) {
            throw ApiException.badRequest("MERCHANT_BUSINESS_FORMAT_INVALID",
                    "商户业态必须为 酒吧/餐饮/零售/游乐/传媒/其他 之一"
                            + "(bar/catering/retail/amusement/media/other)");
        }
        PageResponse<MerchantRecord> result = repository.listMerchantsPaged(includeArchived, format, page, size);
        auditEventService.recordSuccess(
                "merchant.viewed",
                "merchant.profile.view",
                "merchant",
                "all",
                null,
                null,
                Map.of("count", result.items().size(), "total", result.total())
        );
        return result;
    }

    public MerchantRecord createMerchant(MerchantProfileRequest request) {
        authorizationService.requirePlatformScope();
        return saveMerchant(
                "createMerchant",
                "mch_" + UUID.randomUUID(),
                request,
                false
        );
    }

    /** M009-001/002：更新只覆盖档案字段；商户须已存在（404），存档/删除申请状态与 createdAt 原样保留。 */
    public MerchantRecord updateMerchant(String merchantId, MerchantProfileRequest request) {
        authorizationService.requirePlatformScope();
        return saveMerchant(
                "updateMerchant:" + merchantId,
                requireText(merchantId, "merchant_id"),
                request,
                true
        );
    }

    // ================= V260 商户存档与审批后删除范式 =================

    /** 当前操作者 id（存档/删除申请留痕与自审校验）；无鉴权上下文（测试/内部）回退 null。 */
    private String actorId() {
        if (authorizationService == null) {
            return null;
        }
        try {
            return authorizationService.currentActor().id();
        } catch (RuntimeException unauthenticated) {
            return null;
        }
    }

    /** 存档/取消存档商户（软标记，可逆；列表默认过滤已存档）。取消存档时把 pending 删除申请回退 none。 */
    public MerchantRecord archiveMerchant(String merchantId, MerchantArchiveRequest request) {
        authorizationService.requirePlatformScope();
        String id = requireText(merchantId, "merchant_id");
        return executeWrite(
                "archiveMerchant:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                MerchantRecord.class,
                "merchant.archived",
                "merchant.archive_failed",
                "merchant.profile.manage",
                "merchant",
                () -> {
                    String r = requireText(request == null ? null : request.reason(), "reason");
                    MerchantRecord merchant = requireMerchant(id);
                    Instant now = Instant.now();
                    // M009-011：存档＝可逆软下线（BSM-004），不是商户退出；退出缓冲期与停票不在此触发（BEQR-155）。
                    MerchantRecord saved = repository.saveMerchant(merchant.withArchive(now, actorId(), now));
                    repository.setMerchantArchiveReason(id, r);
                    return saved;
                }
        );
    }

    public MerchantRecord unarchiveMerchant(String merchantId, MerchantArchiveRequest request) {
        authorizationService.requirePlatformScope();
        String id = requireText(merchantId, "merchant_id");
        return executeWrite(
                "unarchiveMerchant:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                MerchantRecord.class,
                "merchant.unarchived",
                "merchant.unarchive_failed",
                "merchant.profile.manage",
                "merchant",
                () -> {
                    MerchantRecord merchant = requireMerchant(id);
                    Instant now = Instant.now();
                    // 取消存档=保留该商户,顺带撤销待审批删除,避免"生效可见却挂着待删"的迷惑态。
                    MerchantRecord saved = repository.saveMerchant(
                            merchant.withArchive(null, null, now).withDelete("none", null, null, null, now));
                    repository.setMerchantArchiveReason(id, null);
                    return saved;
                }
        );
    }

    /** 发起删除申请：须先存档（防误删在用商户），记 requested_by/at/reason，状态置 pending。 */
    public MerchantRecord requestDeleteMerchant(String merchantId, MerchantDeleteRequest request) {
        authorizationService.requirePlatformScope();
        String id = requireText(merchantId, "merchant_id");
        return executeWrite(
                "requestDeleteMerchant:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                MerchantRecord.class,
                "merchant.delete_requested",
                "merchant.delete_request_failed",
                "merchant.profile.manage",
                "merchant",
                () -> {
                    MerchantRecord merchant = requireMerchant(id);
                    if (merchant.archivedAt() == null) {
                        throw ApiException.badRequest("MERCHANT_NOT_ARCHIVED",
                                "商户必须先存档才能申请删除");
                    }
                    if ("pending".equals(merchant.deleteStatus())) {
                        throw ApiException.conflict("MERCHANT_DELETE_ALREADY_PENDING",
                                "该商户已有待审批的删除申请");
                    }
                    Instant now = Instant.now();
                    String reason = requireText(request == null ? null : request.reason(), "reason");
                    String actor = actorId();
                    // M009-009：BSM-004「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批」——直接按软删执行，仍查占用并留痕。
                    if (isMasterAuthority()) {
                        long refs = repository.countMerchantReferences(id);
                        if (refs > 0) {
                            throw ApiException.conflict("MERCHANT_IN_USE",
                                    "商户仍被 " + refs + " 条业务记录引用（合同/账单/工单/员工/酒吧等），不可删除");
                        }
                        MerchantRecord deleted = merchant.withDelete("approved", actor, now, reason, now);
                        repository.saveMerchant(deleted);
                        repository.deleteMerchant(id);
                        return deleted;
                    }
                    return repository.saveMerchant(merchant.withDelete("pending", actor, now, reason, now));
                }
        );
    }

    /**
     * 审批删除申请。approved 且无活引用 → 软删除标记（delete_status=approved，行保留、默认列表不显示，BSM-004）；rejected → 回 none。
     * M009-009：审批人须为最高权限（持授权 U 盘密钥者，BSM-004/BSM-007）；禁自审：审批人不得为删除申请发起人。
     */
    public MerchantDeleteDecision approveDeleteMerchant(String merchantId, MerchantDeleteApprovalRequest request) {
        authorizationService.requirePlatformScope();
        String id = requireText(merchantId, "merchant_id");
        return executeWrite(
                "approveDeleteMerchant:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                MerchantDeleteDecision.class,
                "merchant.delete_approved",
                "merchant.delete_approve_failed",
                "merchant.profile.approve",
                "merchant",
                () -> {
                    MerchantRecord merchant = requireMerchant(id);
                    if (!"pending".equals(merchant.deleteStatus())) {
                        throw ApiException.badRequest("MERCHANT_DELETE_NOT_PENDING",
                                "该商户没有待审批的删除申请");
                    }
                    String decision = request == null || request.decision() == null
                            ? null : request.decision().trim().toLowerCase(Locale.ROOT);
                    if (!"approved".equals(decision) && !"rejected".equals(decision)) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "审批结论必须为通过（approved）或驳回（rejected）");
                    }
                    if (!isMasterAuthority()) {
                        throw ApiException.forbidden("MERCHANT_DELETE_APPROVER_NOT_MASTER", "商户删除只能由最高权限审批通过");
                    }
                    // 职责分离：删除申请发起人不能审批自己的申请。actorId 为 null（无鉴权/测试）时跳过该闸。
                    String actor = actorId();
                    if (actor != null && actor.equals(merchant.deleteRequestedBy())) {
                        throw ApiException.forbidden("MERCHANT_SELF_APPROVE_FORBIDDEN",
                                "删除申请发起人不能审批自己的申请");
                    }
                    if ("rejected".equals(decision)) {
                        Instant now = Instant.now();
                        repository.saveMerchant(merchant.withDelete("none", null, null, null, now));
                        return new MerchantDeleteDecision(id, "rejected", false);
                    }
                    // approved：先校验商户无核心运营表活引用，有则拒绝删除（防连锁孤儿）。
                    long refs = repository.countMerchantReferences(id);
                    if (refs > 0) {
                        throw ApiException.conflict("MERCHANT_IN_USE",
                                "商户仍被 " + refs + " 条业务记录引用（合同/账单/工单/员工/酒吧等），不可删除");
                    }
                    repository.deleteMerchant(id);
                    return new MerchantDeleteDecision(id, "approved", true);
                }
        );
    }

    private MerchantRecord requireMerchant(String id) {
        return repository.findMerchantById(id)
                .orElseThrow(() -> ApiException.notFound("MERCHANT_NOT_FOUND", "商户不存在或已被删除"));
    }

    /** M009-028：BSM-004「下线就不能再用」——新项目/空间/绑定只能引用未存档且未审批删除的商户；档案更新、存档/恢复与删除流程仍经 requireMerchant 命中已存档行。 */
    private MerchantRecord requireActiveMerchant(String id) {
        MerchantRecord merchant = requireMerchant(id);
        if (merchant.archivedAt() != null || "approved".equals(merchant.deleteStatus())) {
            throw ApiException.badRequest("MERCHANT_ARCHIVED", "商户已存档或已删除，不能再用于新的项目、空间或主体绑定");
        }
        return merchant;
    }

    public PageResponse<ProjectRecord> listProjects() {
        return listProjects(DEFAULT_PAGE, LIST_MAX_PAGE_SIZE);
    }

    public PageResponse<ProjectRecord> listProjects(int page, int size) {
        PageResponse<ProjectRecord> result = repository.listProjectsPaged(page, size);
        auditEventService.recordSuccess(
                "project.viewed",
                "governance.project.view",
                "project",
                "all",
                null,
                null,
                Map.of("count", result.items().size(), "total", result.total())
        );
        return result;
    }

    public ProjectRecord createProject(ProjectRequest request) {
        authorizationService.requirePlatformScope();
        return executeWrite(
                "createProject",
                idempotencyKey(request),
                request,
                ProjectRecord.class,
                "project.created",
                "project.create_failed",
                "governance.project.manage",
                "project",
                () -> {
                    String name = requireText(request == null ? null : request.name(), "name");
                    String venueType = requireText(request == null ? null : request.venueType(), "venue_type");
                    String merchantId = trimToNull(request.merchantId());
                    if (merchantId != null) {
                        requireActiveMerchant(merchantId);
                    }
                    String status = requireProjectStatus(request.status());
                    Instant now = Instant.now();
                    return repository.saveProject(new ProjectRecord(
                            "prj_" + UUID.randomUUID(),
                            merchantId,
                            venueType,
                            name,
                            trimToNull(request.code()),
                            trimToNull(request.floor()),
                            request.capacity(),
                            status,
                            now,
                            now
                    ));
                }
        );
    }

    /** 3.13：空间存在性（招商线索/意向意向空间校验用）。 */
    public boolean spaceExists(String spaceId) {
        return spaceId != null && repository.findSpaceById(spaceId).isPresent();
    }

    public PageResponse<SpaceRecord> listSpaces() {
        return listSpaces(DEFAULT_PAGE, LIST_MAX_PAGE_SIZE);
    }

    public PageResponse<SpaceRecord> listSpaces(int page, int size) {
        PageResponse<SpaceRecord> result = repository.listSpacesPaged(page, size);
        auditEventService.recordSuccess(
                "space.viewed",
                "governance.space.view",
                "space",
                "all",
                null,
                null,
                Map.of("count", result.items().size(), "total", result.total())
        );
        return result;
    }

    public PageResponse<SpaceTypeRecord> listSpaceTypes() {
        List<SpaceTypeRecord> records = repository.listSpaceTypes();
        auditEventService.recordSuccess(
                "space_type.viewed", "governance.space.view", "space_type", "all", null, null,
                Map.of("count", records.size()));
        return PageResponse.of(records, DEFAULT_PAGE, LIST_MAX_PAGE_SIZE);
    }

    public SpaceTypeRecord createSpaceType(com.complex.platform.governance.dto.SpaceTypeRequest request) {
        authorizationService.requirePlatformScope();
        return executeWrite(
                "createSpaceType",
                request == null ? null : request.idempotencyKey(),
                request,
                SpaceTypeRecord.class,
                "space_type.updated",
                "space_type.update_failed",
                "governance.space.manage",
                "space_type",
                () -> {
                    String typeCode = requireText(request == null ? null : request.typeCode(), "type_code");
                    String typeName = requireText(request == null ? null : request.typeName(), "type_name");
                    Instant now = Instant.now();
                    java.util.Optional<SpaceTypeRecord> existing = repository.listSpaceTypes().stream()
                            .filter(item -> typeCode.equals(item.typeCode()))
                            .findFirst();
                    if (existing.isPresent()) {
                        throw ApiException.conflict("SPACE_TYPE_CODE_EXISTS",
                                "该种类编码已存在，不能用新增改已有种类");
                    }
                    return repository.saveSpaceType(new SpaceTypeRecord(
                            "sptype_" + typeCode, typeCode, typeName,
                            request.sortOrder() == null ? 100 : request.sortOrder(),
                            "active", now, now));
                });
    }

    public void deleteSpaceType(String id) {
        authorizationService.requirePlatformScope();
        repository.findSpaceTypeById(id)
                .orElseThrow(() -> ApiException.notFound("SPACE_TYPE_NOT_FOUND", "空间种类不存在或已被删除"));
        throw ApiException.badRequest("SPACE_TYPE_DELETE_REQUIRES_APPROVAL", "删除须先存档，再申请并由最高权限审批");
    }

    public void archiveSpaceType(String id, String reason) {
        authorizationService.requirePlatformScope();
        requireText(reason, "reason");
        SpaceTypeRecord existing = repository.findSpaceTypeById(id)
                .orElseThrow(() -> ApiException.notFound("SPACE_TYPE_NOT_FOUND", "空间种类不存在或已被删除"));
        if ("pending_delete".equals(existing.status())) {
            throw ApiException.badRequest("SPACE_TYPE_ARCHIVE_NOT_ALLOWED", "待审批的删除申请不能用存档撤回，请驳回");
        }
        if ("disabled".equals(existing.status())) {
            throw ApiException.badRequest("SPACE_TYPE_ALREADY_DELETED", "该种类已删除");
        }
        Instant now = Instant.now();
        repository.saveSpaceType(new SpaceTypeRecord(
                existing.id(), existing.typeCode(), existing.typeName(), existing.sortOrder(),
                "archived", existing.createdAt(), now));
        auditEventService.recordSuccess(
                "space_type.archived", "governance.space.manage", "space_type", id, null, null,
                Map.of("type_code", existing.typeCode(), "reason", reason.trim()));
    }

    public void unarchiveSpaceType(String id) {
        authorizationService.requirePlatformScope();
        SpaceTypeRecord existing = repository.findSpaceTypeById(id)
                .orElseThrow(() -> ApiException.notFound("SPACE_TYPE_NOT_FOUND", "空间种类不存在或已被删除"));
        if (!"archived".equals(existing.status())) {
            throw ApiException.badRequest("SPACE_TYPE_NOT_ARCHIVED", "只有已存档的种类可以取消存档");
        }
        Instant now = Instant.now();
        repository.saveSpaceType(new SpaceTypeRecord(
                existing.id(), existing.typeCode(), existing.typeName(), existing.sortOrder(),
                "active", existing.createdAt(), now));
        auditEventService.recordSuccess(
                "space_type.unarchived", "governance.space.manage", "space_type", id, null, null,
                Map.of("type_code", existing.typeCode()));
    }

    public void requestDeleteSpaceType(String id, String reason) {
        authorizationService.requirePlatformScope();
        SpaceTypeRecord existing = repository.findSpaceTypeById(id)
                .orElseThrow(() -> ApiException.notFound("SPACE_TYPE_NOT_FOUND", "空间种类不存在或已被删除"));
        if (!"archived".equals(existing.status())) {
            throw ApiException.badRequest("SPACE_TYPE_NOT_ARCHIVED", "要删除须先存档");
        }
        String text = requireText(reason, "reason");
        long used = repository.countSpacesByTypeCode(existing.typeCode());
        if (used > 0) {
            throw ApiException.conflict("SPACE_TYPE_IN_USE", "还有空间在用这个种类，不能删除");
        }
        String actor = actorId() == null ? "unknown" : actorId();
        if (isMasterAuthority()) {
            repository.saveSpaceTypeDeleteRequest(existing.id(), text, actor);
            repository.saveSpaceType(new SpaceTypeRecord(
                    existing.id(), existing.typeCode(), existing.typeName(), existing.sortOrder(),
                    "disabled", existing.createdAt(), Instant.now()));
            auditEventService.recordSuccess(
                    "space_type.deleted", "governance.space.manage", "space_type", id, null, null,
                    Map.of("type_code", existing.typeCode()));
            return;
        }
        repository.saveSpaceType(new SpaceTypeRecord(
                existing.id(), existing.typeCode(), existing.typeName(), existing.sortOrder(),
                "pending_delete", existing.createdAt(), Instant.now()));
        repository.saveSpaceTypeDeleteRequest(existing.id(), text, actor);
        auditEventService.recordSuccess(
                "space_type.delete_requested", "governance.space.manage", "space_type", id, null, null,
                Map.of("type_code", existing.typeCode()));
    }

    public void approveDeleteSpaceType(String id, String decision) {
        authorizationService.requirePlatformScope();
        if (!isMasterAuthority()) {
            throw ApiException.forbidden("SPACE_TYPE_DELETE_APPROVER_NOT_MASTER", "空间种类删除只能由最高权限审批通过");
        }
        SpaceTypeRecord existing = repository.findSpaceTypeById(id)
                .orElseThrow(() -> ApiException.notFound("SPACE_TYPE_NOT_FOUND", "空间种类不存在或已被删除"));
        if (!"pending_delete".equals(existing.status())) {
            throw ApiException.badRequest("SPACE_TYPE_DELETE_NOT_PENDING", "该种类没有待审批的删除申请");
        }
        String actor = actorId();
        String requestedBy = repository.findSpaceTypeDeleteRequestedBy(id);
        if (actor != null && actor.equals(requestedBy)) {
            throw ApiException.forbidden("SPACE_TYPE_SELF_APPROVE_FORBIDDEN", "删除申请发起人不能审批自己的申请");
        }
        String verdict = decision == null ? "" : decision.trim().toLowerCase(Locale.ROOT);
        if (!"approved".equals(verdict) && !"rejected".equals(verdict)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "审批结论必须为通过或驳回");
        }
        Instant now = Instant.now();
        String next = "approved".equals(verdict) ? "disabled" : "archived";
        repository.saveSpaceType(new SpaceTypeRecord(
                existing.id(), existing.typeCode(), existing.typeName(), existing.sortOrder(),
                next, existing.createdAt(), now));
        auditEventService.recordSuccess(
                "space_type.delete_approved", "governance.space.manage", "space_type", id, null, null,
                Map.of("decision", next, "type_code", existing.typeCode()));
    }

    public SpaceRecord createSpace(SpaceRequest request) {
        authorizationService.requirePlatformScope();
        return executeWrite(
                "createSpace",
                idempotencyKey(request),
                request,
                SpaceRecord.class,
                "space.updated",
                "space.update_failed",
                "governance.space.manage",
                "space",
                () -> {
                    String spaceName = requireText(request == null ? null : request.spaceName(), "space_name");
                    String spaceType = requireText(request == null ? null : request.spaceType(), "space_type");
                    // 条目3 硬校验：space_type 必须是空间类型字典中处于 active 的取值（前端走下拉、后台可维护字典）。
                    if (!repository.listActiveSpaceTypeCodes().contains(spaceType)) {
                        throw com.complex.platform.common.api.ApiException.badRequest("VALIDATION_FAILED",
                                "空间种类必须是空间种类表中的启用值");
                    }
                    String projectId = trimToNull(request.projectId());
                    if (projectId != null && !repository.projectExists(projectId)) {
                        throw ApiException.notFound("PROJECT_NOT_FOUND", "项目不存在");
                    }
                    String merchantId = trimToNull(request.merchantId());
                    if (merchantId != null) {
                        requireActiveMerchant(merchantId);
                    }
                    String parentSpaceId = trimToNull(request.parentSpaceId());
                    if (parentSpaceId != null && repository.findSpaceById(parentSpaceId).isEmpty()) {
                        throw ApiException.notFound("SPACE_NOT_FOUND", "上级空间不存在");
                    }
                    Instant now = Instant.now();
                    return repository.saveSpace(new SpaceRecord(
                            "sp_" + UUID.randomUUID(),
                            projectId,
                            merchantId,
                            parentSpaceId,
                            spaceName,
                            spaceType,
                            trimToNull(request.floorNo()),
                            request.areaSquareMeter(),
                            request.polygonPoints() == null ? List.of() : request.polygonPoints(),
                            requireSpaceStatus(request.status()),
                            now,
                            now
                    ));
                }
        );
    }

    public PageResponse<BusinessEntityBindingRecord> listBusinessEntityBindings() {
        return listBusinessEntityBindings(DEFAULT_PAGE, LIST_MAX_PAGE_SIZE);
    }

    public PageResponse<BusinessEntityBindingRecord> listBusinessEntityBindings(int page, int size) {
        try {
            PageResponse<BusinessEntityBindingRecord> result =
                    repository.listBusinessEntityBindingsPaged(page, size);
            auditEventService.recordSuccess(
                    "entity_binding.viewed",
                    "entity.binding.view",
                    "business_entity_binding",
                    "all",
                    null,
                    null,
                    Map.of("count", result.items().size(), "total", result.total())
            );
            return result;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "entity_binding.view_failed",
                    "entity.binding.view",
                    "business_entity_binding",
                    "all",
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    public BusinessEntityBindingRecord createBusinessEntityBinding(BusinessEntityBindingRequest request) {
        authorizationService.requirePlatformScope();
        return executeWrite(
                "createBusinessEntityBinding",
                idempotencyKey(request),
                request,
                BusinessEntityBindingRecord.class,
                "entity_binding.changed",
                "entity_binding.change_failed",
                "entity.binding.manage",
                "business_entity_binding",
                () -> {
                    BusinessEntityBindingRequest actual = requireRequest(request);
                    String merchantId = requireText(actual.merchantId(), "merchant_id");
                    // M009-006：四类主体绑定的经营对象与三个主体都必须真实存在（BEQR-143/PGM-026）；M009-028：且商户未下线。
                    requireActiveMerchant(merchantId);
                    String operationEntityId = requireBusinessEntity(actual.operationEntityId(), "operation_entity_id");
                    String contractEntityId = requireBusinessEntity(actual.contractEntityId(), "contract_entity_id");
                    String settlementEntityId = requireBusinessEntity(actual.settlementEntityId(), "settlement_entity_id");
                    repository.findActiveBusinessEntityBindingByMerchantId(merchantId)
                            .ifPresent(existing -> {
                                throw ApiException.conflict(
                                        "ACTIVE_BINDING_EXISTS",
                                        "该商户已存在生效中的主体绑定，不能重复绑定");
                            });
                    Instant now = Instant.now();
                    BusinessEntityBindingRecord saved = repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(
                            UUID.randomUUID().toString(),
                            merchantId,
                            operationEntityId,
                            contractEntityId,
                            settlementEntityId,
                            "active",
                            actual.effectiveFrom() == null ? LocalDate.now(ZoneOffset.UTC) : actual.effectiveFrom(),
                            null,
                            trimToNull(actual.changeReason()),
                            now,
                            now
                    ));
                    syncMerchantEntities(saved);
                    return saved;
                }
        );
    }

    public BusinessEntityBindingRecord changeBusinessEntityBinding(String id, StatusChangeRequest request) {
        authorizationService.requirePlatformScope();
        String bindingId = requireText(id, "id");
        return executeWrite(
                "changeBusinessEntityBinding:" + bindingId,
                idempotencyKey(request),
                request,
                BusinessEntityBindingRecord.class,
                "entity_binding.changed",
                "entity_binding.change_failed",
                "entity.binding.manage",
                "business_entity_binding",
                () -> {
                    StatusChangeRequest actual = requireRequest(request);
                    String status = requireText(actual.action(), "action");
                    if (!Set.of("active", "inactive", "suspended").contains(status)) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "操作必须为启用（active）、停用（inactive）或暂停（suspended）");
                    }
                    String reason = requireText(actual.reason(), "reason");
                    BusinessEntityBindingRecord existing = repository.findBusinessEntityBindingById(bindingId)
                            .orElseThrow(() -> ApiException.notFound("BINDING_NOT_FOUND", "主体绑定不存在或已被删除"));
                    if ("active".equals(status)) {
                        repository.findActiveBusinessEntityBindingByMerchantId(existing.merchantId())
                                .filter(active -> !active.id().equals(existing.id()))
                                .ifPresent(active -> {
                                    throw ApiException.conflict(
                                            "ACTIVE_BINDING_EXISTS",
                                            "该商户已存在生效中的主体绑定，不能重复绑定");
                                });
                    }
                    Instant now = Instant.now();
                    BusinessEntityBindingRecord saved = repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(
                            existing.id(),
                            existing.merchantId(),
                            existing.operationEntityId(),
                            existing.contractEntityId(),
                            existing.settlementEntityId(),
                            status,
                            existing.effectiveFrom(),
                            "active".equals(status) ? null : LocalDate.now(ZoneOffset.UTC),
                            reason,
                            existing.createdAt(),
                            now
                    ));
                    if ("active".equals(status)) {
                        syncMerchantEntities(saved);
                    }
                    return saved;
                }
        );
    }




    /** V348+V384 业态必填且必须是六值之一；招商自动建档与后台手工建档同一入口、同一校验。 */
    private static String requireBusinessFormat(String raw) {
        String value = requireText(raw, "merchant_type");
        if (!MERCHANT_BUSINESS_FORMATS.contains(value)) {
            throw ApiException.badRequest("MERCHANT_BUSINESS_FORMAT_INVALID",
                    "商户业态必须为 酒吧/餐饮/零售/游乐/传媒/其他 之一"
                            + "(bar/catering/retail/amusement/media/other)");
        }
        return value;
    }

    /** M009-003：合作方式非空时须为六类之一；空＝尚未确定（招商签约阶段）。 */
    private static String requireCooperationForm(String raw) {
        String value = trimToNull(raw);
        if (value != null && !MERCHANT_COOPERATION_FORMS.contains(value)) {
            throw ApiException.badRequest("MERCHANT_COOPERATION_FORM_INVALID",
                    "合作方式必须为 自营/租赁/联营/代运营/服务商接入/其他 之一"
                            + "(self_operated/lease/joint_operation/managed_operation/service_provider/other)");
        }
        return value;
    }

    /** M009-004：商户/运营主体状态白名单（BEQR-155）；缺省正常（active）。 */
    private String requireSubjectStatus(String raw) {
        String value = trimToDefault(raw, "active");
        if (!SUBJECT_STATUSES.contains(value)) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "状态必须为 待准入/正常/暂停/终止/黑名单 之一"
                            + "(pending_admission/active/suspended/terminated/blacklisted)；归档请使用存档操作");
        }
        return value;
    }

    /** M009-004：综合体项目状态白名单（BEQR-154）；缺省已启用（active）。 */
    private String requireProjectStatus(String raw) {
        String value = trimToDefault(raw, "active");
        if (!PROJECT_STATUSES.contains(value)) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "项目状态必须为 草稿/待启用/已启用/暂停/终止/归档 之一"
                            + "(draft/pending_activation/active/suspended/terminated/archived)");
        }
        return value;
    }

    /** M009-036：空间状态白名单＝已发布 V203 chk_spaces_status 二值（active/unavailable），缺省 active；不引入其它状态。 */
    private String requireSpaceStatus(String raw) {
        String value = trimToDefault(raw, "active");
        if (!SPACE_STATUSES.contains(value)) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "空间状态必须为 可用/不可用 之一(active/unavailable)");
        }
        return value;
    }

    /** M009-005：主体角色白名单（PGM-018/019）；非法值在服务层以中文 400 拒绝，不落到数据库 CHECK。 */
    private static String requireBusinessEntityRole(String raw) {
        String value = requireText(raw, "entity_role");
        if (!BUSINESS_ENTITY_ROLES.contains(value)) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "主体角色必须为 顶层平台/城市项目公司/域主管公司 之一(platform/project_company/management_company)");
        }
        return value;
    }

    /** M009-006：绑定引用的主体必须存在。 */
    private String requireBusinessEntity(String raw, String fieldName) {
        String id = requireText(raw, fieldName);
        if (!repository.businessEntityExists(id)) {
            throw ApiException.notFound("BUSINESS_ENTITY_NOT_FOUND", "主体不存在：" + id);
        }
        return id;
    }

    /** M009-008：PGM-026「合同、结算、权限、看板、第三方接入都引用同一组绑定」——生效绑定是商户三个主体列的唯一来源，激活时同步投影。 */
    private void syncMerchantEntities(BusinessEntityBindingRecord binding) {
        repository.findMerchantById(binding.merchantId()).ifPresent(merchant -> repository.saveMerchant(
                merchant.withEntities(binding.operationEntityId(), binding.contractEntityId(),
                        binding.settlementEntityId(), Instant.now())));
    }

    /** M009-009：当前操作者是否为持授权 U 盘密钥的最高权限；无鉴权上下文回退 false。 */
    private boolean isMasterAuthority() {
        if (authorizationService == null) {
            return false;
        }
        try {
            return authorizationService.isMasterAuthority(authorizationService.currentActor());
        } catch (RuntimeException unauthenticated) {
            return false;
        }
    }

    private MerchantRecord saveMerchant(
            String operationId,
            String merchantId,
            MerchantProfileRequest request,
            boolean update
    ) {
        return executeWrite(
                operationId,
                idempotencyKey(request),
                request,
                MerchantRecord.class,
                "merchant.updated",
                "merchant.update_failed",
                "merchant.profile.manage",
                "merchant",
                () -> {
                    String merchantName = requireText(request == null ? null : request.merchantName(), "merchant_name");
                    String merchantType = requireBusinessFormat(request == null ? null : request.merchantType());
                    String cooperationForm = requireCooperationForm(request.cooperationForm());
                    String status = requireSubjectStatus(request.status());
                    // M009-001/002：更新须命中已有商户，并保留存档/删除申请状态与 createdAt；
                    // M009-008：三个主体列以生效绑定为准，档案更新不覆盖。
                    MerchantRecord existing = update ? requireMerchant(merchantId) : null;
                    Instant now = Instant.now();
                    return repository.saveMerchant(new MerchantRecord(
                            merchantId,
                            merchantName,
                            merchantType,
                            existing == null ? trimToNull(request.businessEntityId()) : existing.businessEntityId(),
                            existing == null ? trimToNull(request.contractEntityId()) : existing.contractEntityId(),
                            existing == null ? trimToNull(request.settlementEntityId()) : existing.settlementEntityId(),
                            trimToNull(request.contactName()),
                            trimToNull(request.contactPhone()),
                            trimToNull(request.ownerPerson()),
                            trimToNull(request.mallBlockCode()),
                            request.areaSquareMeter(),
                            request.leasedAreaSquareMeter(),
                            trimToNull(request.secondaryContactName()),
                            trimToNull(request.secondaryContactPhone()),
                            request.rentPerSquareMeter(),
                            cooperationForm,
                            status,
                            existing == null ? now : existing.createdAt(),
                            now,
                            existing == null ? null : existing.archivedAt(),
                            existing == null ? null : existing.archivedBy(),
                            existing == null ? "none" : existing.deleteStatus(),
                            existing == null ? null : existing.deleteRequestedBy(),
                            existing == null ? null : existing.deleteRequestedAt(),
                            existing == null ? null : existing.deleteReason()
                    ));
                }
        );
    }

    private <T> T executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<T> action
    ) {
        return operations.executeWrite(
                operationId,
                idempotencyKey,
                request,
                responseType,
                AuditedOperation.of(successEvent, failureEvent, permissionCode, businessType),
                action,
                this::businessId,
                Map.of()
        );
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private String idempotencyKey(BusinessEntityRequest request) {
        return request == null ? null : request.idempotencyKey();
    }

    private String idempotencyKey(MerchantProfileRequest request) {
        return request == null ? null : request.idempotencyKey();
    }

    private String idempotencyKey(ProjectRequest request) {
        return request == null ? null : request.idempotencyKey();
    }

    private String idempotencyKey(SpaceRequest request) {
        return request == null ? null : request.idempotencyKey();
    }

    private String idempotencyKey(BusinessEntityBindingRequest request) {
        return request == null ? null : request.idempotencyKey();
    }

    private String idempotencyKey(StatusChangeRequest request) {
        return request == null ? null : request.idempotencyKey();
    }



    private String businessId(Object value) {
        if (value instanceof BusinessEntityRecord record) {
            return record.id();
        }
        if (value instanceof MerchantRecord record) {
            return record.id();
        }
        if (value instanceof ProjectRecord record) {
            return record.id();
        }
        if (value instanceof SpaceRecord record) {
            return record.id();
        }
        if (value instanceof BusinessEntityBindingRecord record) {
            return record.id();
        }
        return null;
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }
}
