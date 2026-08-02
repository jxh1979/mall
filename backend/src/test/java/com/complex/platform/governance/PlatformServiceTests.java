package com.complex.platform.governance;

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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    // V348 商户业态标准字段：五值之外的业态一律拒收（业态错＝通票分账份额拆给错的业态）。
    @Test
    void createMerchantRejectsBusinessFormatOutsideFiveValues() {
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
    void approveDeleteRemovesArchivedMerchantWhenNoReferences() {
        MerchantRecord merchant = seedMerchant("seed-delete");
        service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("archive-2", "测试存档理由"));
        service.requestDeleteMerchant(merchant.id(),
                new MerchantDeleteRequest("delete-req", "录入错误，退场清理"));

        platformRepository.merchantReferenceCount = 0;
        TestSecurityActors.asPlatformAdmin("usr_admin2"); // 禁自审:审批人≠申请发起人
        MerchantDeleteDecision decision = service.approveDeleteMerchant(merchant.id(),
                new MerchantDeleteApprovalRequest("delete-approve", "approved", null));

        assertThat(decision.deleted()).isTrue();
        assertThat(service.listMerchants(true).items())
                .noneMatch(m -> m.id().equals(merchant.id()));
    }

    @Test
    void approveDeleteRejectsWhenMerchantStillReferenced() {
        MerchantRecord merchant = seedMerchant("seed-inuse");
        service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("archive-3", "测试存档理由"));
        service.requestDeleteMerchant(merchant.id(),
                new MerchantDeleteRequest("delete-req-2", "尝试删除仍在用商户"));

        platformRepository.merchantReferenceCount = 3; // 模拟仍被酒吧商品/合同等引用
        TestSecurityActors.asPlatformAdmin("usr_admin2"); // 禁自审:审批人≠申请发起人

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
                "operation",
                null,
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
        ProjectRecord created = service.createProject(new ProjectRequest(
                "project-create",
                "create project",
                List.of(),
                "mch_1",
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
        MerchantRecord updated = service.updateMerchant("mch_existing", new MerchantProfileRequest(
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
                "fixed_rent_plus_commission",
                "active"
        ));

        assertThat(updated.id()).isEqualTo("mch_existing");
        assertThat(updated.merchantName()).isEqualTo("P0 Merchant");
        assertThat(updated.mallBlockCode()).isEqualTo("1F-A");
        assertThat(updated.ownerPerson()).isEqualTo("Owner Li");
        assertThat(updated.areaSquareMeter()).isEqualByComparingTo("128.50");
        assertThat(updated.leasedAreaSquareMeter()).isEqualByComparingTo("120.00");
        assertThat(updated.secondaryContactName()).isEqualTo("Jack Contact");
        assertThat(updated.secondaryContactPhone()).isEqualTo("18800000013");
        assertThat(updated.rentPerSquareMeter()).isEqualByComparingTo("12.30");
        assertThat(updated.cooperationForm()).isEqualTo("fixed_rent_plus_commission");
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
        BusinessEntityBindingRequest first = new BusinessEntityBindingRequest(
                "binding-first",
                "first binding",
                List.of("att_1"),
                "mch_binding",
                "be_operation_1",
                "be_contract_1",
                "be_settlement_1",
                LocalDate.parse("2026-06-14"),
                "initial setup"
        );

        BusinessEntityBindingRecord created = service.createBusinessEntityBinding(first);

        assertThat(created.status()).isEqualTo("active");
        assertThat(created.merchantId()).isEqualTo("mch_binding");
        assertThatThrownBy(() -> service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
                "binding-second",
                "second binding",
                List.of(),
                "mch_binding",
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
        BusinessEntityBindingRecord created = service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
                "binding-change-create",
                "create binding",
                List.of(),
                "mch_change",
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
                "mch_change",
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
        BusinessEntityBindingRecord created = service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
                "binding-enum-create",
                "create",
                List.of(),
                "mch_enum",
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
                "operation",
                null,
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
                "operation",
                null,
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
                "sp-kiosk", "create", List.of(), "prj1", "mch1", null, "K1", "kiosk", null, null, null, "active"));
        assertThat(space.spaceType()).isEqualTo("kiosk");
    }

    @Test
    void createSpaceRejectsUnknownSpaceType() {
        // 条目3 硬校验：不在字典内的 space_type → VALIDATION_FAILED。
        assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
                        "sp-bad", "create", List.of(), "prj1", "mch1", null, "Bad", "no_such_type", null, null, null, "active")))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class, ex ->
                        assertThat(ex.code()).isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    void createSpaceAcceptsDictionarySpaceType() {
        SpaceRecord saved = service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
                "sp-ok", "create", List.of(), "prj1", "mch1", null, "Shop A", "shop", "1", null, null, "active"));
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

        // V260 测试用：模拟商户被核心运营表引用的活条数（>0 拒硬删）。
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
            merchants.remove(merchantId);
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
