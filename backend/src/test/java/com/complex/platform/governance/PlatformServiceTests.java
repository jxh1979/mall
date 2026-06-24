package com.complex.platform.governance;

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
import com.complex.platform.governance.dto.ApprovalRequest;
import com.complex.platform.governance.dto.ContractActivateRequest;
import com.complex.platform.governance.dto.ContractRenewRequest;
import com.complex.platform.governance.dto.ContractTerminateRequest;
import com.complex.platform.governance.dto.BusinessEntityBindingRequest;
import com.complex.platform.governance.dto.BusinessEntityRequest;
import com.complex.platform.governance.dto.ContractDraftRequest;
import com.complex.platform.governance.dto.MerchantProfileRequest;
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

    @Test
    void createBusinessEntityUsesIdempotencyAndWritesAudit() {
        BusinessEntityRequest request = new BusinessEntityRequest(
                "entity-once",
                "create entity",
                List.of("att_1"),
                " P0 Entity ",
                "operation",
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

    // P1-3 red→green: 已 approved 合同不得再次审批
    @Test
    void approveContractRejectsNonDraftStatus() {
        ContractRecord draft = service.createContractDraft(new ContractDraftRequest(
                "contract-state-draft",
                "draft",
                List.of(),
                "mch_state",
                "be_contract",
                "be_operation",
                "be_settlement",
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2027-06-30"),
                Map.of(),
                null,
                List.of()
        ));
        service.approveContract(draft.id(), new ApprovalRequest("contract-state-approve1", "approve", List.of(), "approve", "legal ok", "ops"));

        assertThatThrownBy(() -> service.approveContract(draft.id(),
                new ApprovalRequest("contract-state-approve2", "reject", List.of(), "reject", "changed mind", "ops")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("INVALID_STATUS_TRANSITION"));
    }

    // ② 履约闭环 S1：合同生效 + 空间绑定（功能标志门控 + 状态守卫 + 空间冲突）
    @Test
    void activateContractSetsEffectiveAndBindsSpacesGatedByFlag() {
        service.contractLifecycleEffectiveEnabled = true;
        platformRepository.spaces.put("sp_act", new SpaceRecord(
                "sp_act", "prj_1", "mch_act", null, "Shop A", "shop", "1F", null, List.of(), "active",
                java.time.Instant.parse("2026-06-14T00:00:00Z"), java.time.Instant.parse("2026-06-14T00:00:00Z")));
        ContractRecord draft = service.createContractDraft(new ContractDraftRequest(
                "ctr-act-draft", "draft", List.of(), "mch_act", "be_contract", "be_operation", "be_settlement",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of(), null, List.of()));
        service.approveContract(draft.id(), new ApprovalRequest("ctr-act-approve", "approve", List.of(), "approve", "ok", "ops"));

        // 关标志：activate 被拒（合同止于 approved，现状语义不变）。
        service.contractLifecycleEffectiveEnabled = false;
        assertThatThrownBy(() -> service.activateContract(draft.id(),
                new ContractActivateRequest(List.of("sp_act"), "ctr-act-1")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CONTRACT_LIFECYCLE_EFFECTIVE_DISABLED"));

        // 开标志：approved→effective + 写 contract_spaces。
        service.contractLifecycleEffectiveEnabled = true;
        ContractRecord effective = service.activateContract(draft.id(),
                new ContractActivateRequest(List.of("sp_act"), "ctr-act-2"));
        assertThat(effective.lifecycleStatus()).isEqualTo("effective");
        assertThat(service.listContractSpaces(draft.id()).items())
                .extracting(ContractSpaceRecord::spaceId).contains("sp_act");
    }

    @Test
    void activateContractRejectsNonApprovedAndSpaceConflict() {
        service.contractLifecycleEffectiveEnabled = true;
        platformRepository.spaces.put("sp_x", new SpaceRecord(
                "sp_x", "prj_1", "m", null, "X", "shop", "1F", null, List.of(), "active",
                java.time.Instant.parse("2026-06-14T00:00:00Z"), java.time.Instant.parse("2026-06-14T00:00:00Z")));

        // 未 approve（draft）→ activate 拒 CONTRACT_NOT_APPROVED。
        ContractRecord draftA = service.createContractDraft(new ContractDraftRequest(
                "ctr-x-draft", "draft", List.of(), "m", "bc", "bo", "bs",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of(), null, List.of()));
        assertThatThrownBy(() -> service.activateContract(draftA.id(), new ContractActivateRequest(List.of("sp_x"), "k1")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CONTRACT_NOT_APPROVED"));

        // A 生效占用 sp_x；B（租期重叠）activate 同空间 → CONTRACT_SPACE_CONFLICT。
        service.approveContract(draftA.id(), new ApprovalRequest("ctr-x-approve", "approve", List.of(), "approve", "ok", "ops"));
        service.activateContract(draftA.id(), new ContractActivateRequest(List.of("sp_x"), "k2"));
        ContractRecord draftB = service.createContractDraft(new ContractDraftRequest(
                "ctr-y-draft", "draft", List.of(), "m", "bc", "bo", "bs",
                LocalDate.parse("2026-08-01"), LocalDate.parse("2027-01-31"), Map.of(), null, List.of()));
        service.approveContract(draftB.id(), new ApprovalRequest("ctr-y-approve", "approve", List.of(), "approve", "ok", "ops"));
        assertThatThrownBy(() -> service.activateContract(draftB.id(), new ContractActivateRequest(List.of("sp_x"), "k3")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CONTRACT_SPACE_CONFLICT"));
    }

    // ② 履约闭环 S3：到期/退租/续约 + 空间释放
    private ContractRecord effectiveContractBoundToSpace(String key, String spaceId, String merchantId, LocalDate eff, LocalDate exp) {
        service.contractLifecycleEffectiveEnabled = true;
        platformRepository.spaces.put(spaceId, new SpaceRecord(spaceId, "prj_1", merchantId, null, "S", "shop", "1F", null,
                List.of(), "active", java.time.Instant.parse("2026-06-14T00:00:00Z"), java.time.Instant.parse("2026-06-14T00:00:00Z")));
        ContractRecord draft = service.createContractDraft(new ContractDraftRequest(
                key + "-draft", "draft", List.of(), merchantId, "bc", "bo", "bs", eff, exp, Map.of(), null, List.of()));
        service.approveContract(draft.id(), new ApprovalRequest(key + "-appr", "approve", List.of(), "approve", "ok", "ops"));
        service.activateContract(draft.id(), new ContractActivateRequest(List.of(spaceId), key + "-act"));
        return draft;
    }

    @Test
    void terminateContractReleasesSpacesAndSetsTerminatedGatedByFlag() {
        ContractRecord c = effectiveContractBoundToSpace("ctr-t", "sp_t", "m",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"));
        assertThat(service.listContractSpaces(c.id()).items()).hasSize(1);

        // 关标志 → 拒。
        service.contractRenewalTerminationEnabled = false;
        assertThatThrownBy(() -> service.terminateContract(c.id(), new ContractTerminateRequest("退租", "ctr-t-1")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CONTRACT_RENEWAL_TERMINATION_DISABLED"));

        // 开标志 → terminated + 释放空间。
        service.contractRenewalTerminationEnabled = true;
        ContractRecord terminated = service.terminateContract(c.id(), new ContractTerminateRequest("退租", "ctr-t-2"));
        assertThat(terminated.lifecycleStatus()).isEqualTo("terminated");
        assertThat(service.listContractSpaces(c.id()).items()).isEmpty();
    }

    @Test
    void renewContractCreatesNewDraftFromPrior() {
        service.contractRenewalTerminationEnabled = true;
        ContractRecord prior = effectiveContractBoundToSpace("ctr-r", "sp_r", "mch_r",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"));

        ContractRecord renewed = service.renewContract(prior.id(), new ContractRenewRequest(
                LocalDate.parse("2027-07-01"), LocalDate.parse("2028-06-30"), "ctr-r-1"));
        assertThat(renewed.id()).isNotEqualTo(prior.id());
        assertThat(renewed.lifecycleStatus()).isEqualTo("draft");
        assertThat(renewed.merchantId()).isEqualTo("mch_r");                          // 复用原商户/条款
        assertThat(renewed.effectiveDate()).isEqualTo(LocalDate.parse("2027-07-01")); // 新租期
    }

    @Test
    void expiryTransitionMarksExpiredAndReleasesSpaces() {
        service.contractRenewalTerminationEnabled = true;
        ContractRecord c = effectiveContractBoundToSpace("ctr-e", "sp_e", "m",
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-03-31")); // 已过期租期
        assertThat(service.listContractSpaces(c.id()).items()).hasSize(1);

        int n = service.generateExpiredContractTransitions(LocalDate.parse("2026-06-24"));
        assertThat(n).isEqualTo(1);
        assertThat(service.listContracts().items().stream()
                .filter(r -> r.id().equals(c.id())).findFirst().orElseThrow().lifecycleStatus()).isEqualTo("expired");
        assertThat(service.listContractSpaces(c.id()).items()).isEmpty(); // 到期释放空间
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
                "active"
        ));

        assertThat(e1.id()).isNotEqualTo(e2.id());
        assertThat(e1.entityName()).isEqualTo("Corp A"); // 第一条未被覆盖
        assertThat(e2.entityName()).isEqualTo("Corp B");
    }

    @Test
    void createContractDraftPersistsAttachmentsAndApprovalKeepsDraftFields() {
        ContractRecord draft = service.createContractDraft(new ContractDraftRequest(
                "contract-draft",
                "draft contract",
                List.of("att_evidence"),
                "mch_contract",
                "be_contract",
                "be_operation",
                "be_settlement",
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2027-06-30"),
                Map.of("monthly_rent_cent", 100_000),
                java.math.BigDecimal.valueOf(12.50),
                List.of("att_contract_pdf", "att_appendix")
        ));

        ContractRecord approved = service.approveContract(
                draft.id(),
                new ApprovalRequest("contract-approve", "approve", List.of("att_approval"), "approve", "legal ok", "staff_finance")
        );

        assertThat(approved.id()).isEqualTo(draft.id());
        assertThat(approved.lifecycleStatus()).isEqualTo("approved");
        assertThat(approved.attachmentIds()).containsExactly("att_contract_pdf", "att_appendix");
        assertThat(approved.effectiveDate()).isEqualTo(LocalDate.parse("2026-07-01"));
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("contract.draft_saved", "contract.lifecycle_changed");
    }

    private static final class InMemoryPlatformRepository implements PlatformRepository {
        private final Map<String, BusinessEntityRecord> businessEntities = new LinkedHashMap<>();
        private final Map<String, MerchantRecord> merchants = new LinkedHashMap<>();
        private final Map<String, ProjectRecord> projects = new LinkedHashMap<>();
        private final Map<String, SpaceRecord> spaces = new LinkedHashMap<>();
        private final Map<String, BusinessEntityBindingRecord> businessEntityBindings = new LinkedHashMap<>();
        private final List<String> businessEntityBindingStatusHistory = new ArrayList<>();
        private final Map<String, ContractRecord> contracts = new LinkedHashMap<>();
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

        @Override
        public ContractRecord saveContract(ContractRecord contract) {
            contracts.put(contract.id(), contract);
            return contract;
        }

        @Override
        public List<ContractRecord> listContracts() {
            return new ArrayList<>(contracts.values());
        }

        @Override
        public Optional<ContractRecord> findContractById(String id) {
            return Optional.ofNullable(contracts.get(id));
        }

        private final List<ContractSpaceRecord> contractSpaces = new ArrayList<>();

        @Override
        public Optional<SpaceRecord> findSpaceById(String id) {
            return Optional.ofNullable(spaces.get(id));
        }

        @Override
        public List<ContractSpaceRecord> listContractSpaces(String contractId) {
            return contractSpaces.stream().filter(cs -> contractId.equals(cs.contractId())).toList();
        }

        @Override
        public void bindContractSpace(String contractId, String spaceId, String projectId, java.time.Instant createdAt) {
            boolean exists = contractSpaces.stream()
                    .anyMatch(cs -> contractId.equals(cs.contractId()) && spaceId.equals(cs.spaceId()));
            if (!exists) {
                contractSpaces.add(new ContractSpaceRecord(contractId, spaceId, projectId, createdAt));
            }
        }

        @Override
        public boolean hasOverlappingEffectiveContractForSpace(
                String spaceId, java.time.LocalDate effectiveDate, java.time.LocalDate expiryDate, String excludeContractId) {
            return contractSpaces.stream()
                    .filter(cs -> spaceId.equals(cs.spaceId()) && !excludeContractId.equals(cs.contractId()))
                    .map(cs -> contracts.get(cs.contractId()))
                    .filter(java.util.Objects::nonNull)
                    .filter(c -> "effective".equals(c.lifecycleStatus()))
                    .anyMatch(c -> !c.effectiveDate().isAfter(expiryDate) && !c.expiryDate().isBefore(effectiveDate));
        }

        @Override
        public void updateContractLifecycle(String contractId, String status, java.time.Instant terminatedAt, String terminationReason) {
            ContractRecord c = contracts.get(contractId);
            if (c != null) {
                contracts.put(contractId, new ContractRecord(c.id(), c.merchantId(), c.contractSubjectId(),
                        c.operationSubjectId(), c.settlementSubjectId(), c.effectiveDate(), c.expiryDate(),
                        c.rentTerms(), c.shareholderRatioForReport(), c.attachmentIds(), status, c.createdAt(),
                        java.time.Instant.now()));
            }
        }

        @Override
        public int releaseContractSpaces(String contractId) {
            int before = contractSpaces.size();
            contractSpaces.removeIf(cs -> contractId.equals(cs.contractId()));
            return before - contractSpaces.size();
        }

        @Override
        public List<String> listEffectiveContractIdsExpiredBy(java.time.LocalDate asOf) {
            return contracts.values().stream()
                    .filter(c -> "effective".equals(c.lifecycleStatus())
                            && c.expiryDate() != null && c.expiryDate().isBefore(asOf))
                    .map(ContractRecord::id).toList();
        }

        @Override
        public void setContractRenewedFrom(String newContractId, String fromContractId) {
            // renewed_from 不在 ContractRecord 字段；内存桩 no-op（续约测试断言新 draft 存在即可）。
        }
    }

    private static final class InMemoryIdempotencyRepository implements IdempotencyRepository {
        private final Map<String, IdempotencyRecord> records = new LinkedHashMap<>();

        @Override
        public Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey) {
            return Optional.ofNullable(records.get(operationId + ":" + idempotencyKey));
        }

        @Override
        public boolean tryReserve(IdempotencyRecord record) {
            String key = record.operationId() + ":" + record.idempotencyKey();
            if (records.containsKey(key)) {
                return false;
            }
            records.put(key, record);
            return true;
        }

        @Override
        public IdempotencyRecord complete(IdempotencyRecord record) {
            records.put(record.operationId() + ":" + record.idempotencyKey(), record);
            return record;
        }
    }

    private static final class InMemoryAuditEventRepository implements AuditEventRepository {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public AuditEvent save(AuditEvent event) {
            events.add(event);
            return event;
        }
    }
}
