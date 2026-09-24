package com.complex.platform.leasing;
// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.contract.ContractRecord;
import com.complex.platform.contract.ContractService;
import com.complex.platform.governance.MerchantRecord;
import com.complex.platform.governance.PlatformService;
import com.complex.platform.governance.dto.MerchantProfileRequest;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.leasing.dto.LeasingBrandBlacklistRequest;
import com.complex.platform.leasing.dto.LeasingBrandRequest;
import com.complex.platform.leasing.dto.LeasingFollowUpRequest;
import com.complex.platform.leasing.dto.LeasingIntentAdvanceRequest;
import com.complex.platform.leasing.dto.LeasingIntentRequest;
import com.complex.platform.leasing.dto.LeasingLeadAssignRequest;
import com.complex.platform.leasing.dto.LeasingLeadRequest;
import com.complex.platform.leasing.dto.LeasingLeadStatusRequest;
import com.complex.platform.leasing.dto.LeasingSignRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 招商获客 unit 测试（V161 重建）：验 7 态/来源/黑名单拦截/意向卡控/签约/跟进提醒 M1 逻辑。无 DB。 */
class LeasingServiceTests {

    private final InMemoryLeasingRepository repo = new InMemoryLeasingRepository();
    private final AuthorizationService authz = mock(AuthorizationService.class);
    private final ContractService contractService = mock(ContractService.class);
    private final PlatformService platformService = mock(PlatformService.class);
    private LeasingService service;

    @BeforeEach
    void setUp() {
        when(authz.currentActor()).thenReturn(
                new AccessActor("usr_admin", "platform_admin_user", "platform_admin", "org_1", null, "active"));
        // 3.2 默认：平台管理员无 project scope（全量）；跨项目用例再覆写。
        when(authz.currentProjectScopeQuietly()).thenReturn(Optional.empty());
        // 3.13 默认：空间存在性放行（专项用例再覆写为 false）。
        org.mockito.Mockito.when(platformService.spaceExists(org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
        service = new LeasingService(repo, mock(AuditEventService.class), authz, contractService, platformService);
    }

    // 3.13：意向空间传了必须真实存在（挂空编号=400）；不传放行。
    @Test
    void createLeadRejectsNonexistentIntendedSpace() {
        org.mockito.Mockito.when(platformService.spaceExists("space_ghost")).thenReturn(false);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createLead(new com.complex.platform.leasing.dto.LeasingLeadRequest(
                "张三", "13800000000", "outbound", "retail", "space_ghost", null, null, "k_sp_lead")))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.code()).isEqualTo("LEASING_SPACE_NOT_FOUND"));
    }

    @Test
    void listReadsRejectNonPlatformScopedCaller() {
        // 5 表跨租户读越权防御（棘轮 F1 leasing_leads/brands/follow_ups）：招商读接口必须过 platform-scope 闸，
        // 拒绝商户/外包越权读；平台管理员不受影响（见 leadFunnel 等既有测试）。
        org.mockito.Mockito.doThrow(ApiException.forbidden("PLATFORM_SCOPE_REQUIRED", "denied"))
                .when(authz).requirePlatformScope();
        assertThatThrownBy(() -> service.listLeads()).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.listLeadsNeedingFollowUp()).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.listIntents()).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.listBrands()).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.listFollowUps("lead", "lead_x")).isInstanceOf(ApiException.class);
    }

    @Test
    void listLeadsPagedReturnsHonestTotalAndReachesNextPage() {
        // AUD-005 真分页: service 透传 page/size; total 取全量真实值(非当前页条数), 第二页可达首页之后的数据。
        Instant now = Instant.now();
        repo.saveLead(new LeasingLeadRecord("lead_1", "一", null, "outbound", null, null, null, null, "u",
                "following_up", null, null, null, now, now));
        repo.saveLead(new LeasingLeadRecord("lead_2", "二", null, "outbound", null, null, null, null, "u",
                "following_up", null, null, null, now, now));
        repo.saveLead(new LeasingLeadRecord("lead_3", "三", null, "outbound", null, null, null, null, "u",
                "following_up", null, null, null, now, now));
        var page1 = service.listLeads(1, 2);
        assertThat(page1.total()).isEqualTo(3L);
        assertThat(page1.items()).hasSize(2);
        var page2 = service.listLeads(2, 2);
        assertThat(page2.total()).isEqualTo(3L);
        assertThat(page2.items()).hasSize(1);
        assertThat(page2.items()).extracting(LeasingLeadRecord::id)
                .doesNotContainAnyElementsOf(page1.items().stream().map(LeasingLeadRecord::id).toList());
    }

    @Test
    void projectScopedStaffOnlySeesOwnProjectRows() {
        // 3.2 跨项目读隔离：项目员工（organizationId=prj_a）只见本项目行，他项目（prj_b）不可见；
        // 平台管理员（scope empty，见 setUp）全量。
        Instant now = Instant.now();
        repo.saveLead(new LeasingLeadRecord("lead_a", "甲", null, "outbound", null, null, null, null, "u",
                "following_up", null, null, "prj_a", now, now));
        repo.saveLead(new LeasingLeadRecord("lead_b", "乙", null, "outbound", null, null, null, null, "u",
                "following_up", null, null, "prj_b", now, now));
        repo.saveBrand(new LeasingBrandRecord("brand_a", "品牌甲", "prospective", null, null, null, null, "prj_a", now, now));
        repo.saveBrand(new LeasingBrandRecord("brand_b", "品牌乙", "prospective", null, null, null, null, "prj_b", now, now));
        repo.saveFollowUp(new LeasingFollowUpRecord("fu_a", "lead", "lead_a", "phone", "跟进甲", null, "u", "prj_a", now));
        repo.saveFollowUp(new LeasingFollowUpRecord("fu_b", "lead", "lead_b", "phone", "跟进乙", null, "u", "prj_b", now));

        // 平台管理员：全量可见。
        assertThat(service.listLeads().items()).extracting(LeasingLeadRecord::id).contains("lead_a", "lead_b");

        // prj_a 项目员工：只见 prj_a。
        when(authz.currentProjectScopeQuietly()).thenReturn(Optional.of("prj_a"));
        assertThat(service.listLeads().items()).extracting(LeasingLeadRecord::id)
                .contains("lead_a").doesNotContain("lead_b");
        assertThat(service.listBrands().items()).extracting(LeasingBrandRecord::id)
                .contains("brand_a").doesNotContain("brand_b");
        // 2026-07-18 收尾：跟进列表（全量+按对象）同样按项目 scope 过滤。
        assertThat(service.listFollowUps(null, null).items()).extracting(LeasingFollowUpRecord::id)
                .contains("fu_a").doesNotContain("fu_b");
        assertThat(service.listFollowUps("lead", "lead_b").items()).isEmpty();
    }

    @Test
    void leadFunnel_defaultStatusSourceEnumAssignAndReason() {
        LeasingLeadRecord lead = service.createLead(new LeasingLeadRequest(
                "张三", "13800000000", "referral", "bar", "sp_1", null, null, "k1"));
        assertThat(lead.status()).isEqualTo("pending_follow_up");

        // 来源枚举校验。
        assertThatThrownBy(() -> service.createLead(new LeasingLeadRequest(
                "李四", null, "bogus_source", null, null, null, null, "k2")))
                .isInstanceOf(ApiException.class);

        // 主管手动派单。
        LeasingLeadRecord assigned = service.assignLead(lead.id(), new LeasingLeadAssignRequest("usr_sales", "k3"));
        assertThat(assigned.ownerUserId()).isEqualTo("usr_sales");

        // lost 必填原因。
        assertThatThrownBy(() -> service.updateLeadStatus(lead.id(), new LeasingLeadStatusRequest("lost", null, "k4")))
                .isInstanceOf(ApiException.class);
        LeasingLeadRecord lost = service.updateLeadStatus(lead.id(), new LeasingLeadStatusRequest("lost", "预算不足", "k5"));
        assertThat(lost.status()).isEqualTo("lost");
        assertThat(lost.lostOrHoldReason()).isEqualTo("预算不足");
    }

    @Test
    void blacklistedBrandBlocksNewLead() {
        LeasingBrandRecord brand = service.createBrand(new LeasingBrandRequest("脏脏堡", "prospective", "餐饮", "b1"));
        service.blacklistBrand(brand.id(), new LeasingBrandBlacklistRequest(true, "多次违约", "b2"));
        assertThat(repo.findBrandById(brand.id()).orElseThrow().brandType()).isEqualTo("blacklist");

        assertThatThrownBy(() -> service.createLead(new LeasingLeadRequest(
                "王五", null, "outbound", null, null, null, brand.id(), "k1")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("LEASING_BRAND_BLACKLISTED"));
    }

    @Test
    void intentKeyStageRequiresSupervisor() {
        LeasingLeadRecord lead = service.createLead(new LeasingLeadRequest(
                "赵六", null, "exhibition", "retail", "sp_2", null, null, "k1"));
        LeasingIntentRecord intent = service.createIntent(new LeasingIntentRequest(lead.id(), "sp_2", null, Map.of(), "k2"));
        assertThat(intent.stage()).isEqualTo("initial_contact");
        assertThat(repo.findLeadById(lead.id()).orElseThrow().status()).isEqualTo("converted_to_intent");

        // 非关键阶段无需主管。
        service.advanceIntent(intent.id(), new LeasingIntentAdvanceRequest("requirement_confirmed", null, "k3"));
        // 关键阶段缺主管 → 阻断。
        assertThatThrownBy(() -> service.advanceIntent(intent.id(),
                new LeasingIntentAdvanceRequest("proposal_quoted", null, "k4")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("LEASING_INTENT_SUPERVISOR_REQUIRED"));
        // 关键阶段带主管 → 通过。
        LeasingIntentRecord advanced = service.advanceIntent(intent.id(),
                new LeasingIntentAdvanceRequest("proposal_quoted", "usr_supervisor", "k5"));
        assertThat(advanced.stage()).isEqualTo("proposal_quoted");
        assertThat(advanced.lastStageChangeBy()).isEqualTo("usr_supervisor");
    }

    @Test
    void signPendingSigningIntentCreatesContractAndMarksLeadSigned() {
        when(contractService.createDraft(any())).thenReturn(new ContractRecord(
                "ctr_new", "mch_1", "cs", null, "cs", LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"),
                Map.of(), null, "none", null, Map.of(), List.of(), null, null, List.of(), "drafting", null,
                null, null, null, Instant.now(), Instant.now()));
        LeasingLeadRecord lead = service.createLead(new LeasingLeadRequest(
                "钱七", null, "online_inquiry", null, null, null, null, "k1"));
        LeasingIntentRecord intent = service.createIntent(new LeasingIntentRequest(lead.id(), null, null, Map.of(), "k2"));

        // 非 pending_signing → 拒签。
        assertThatThrownBy(() -> service.signIntent(intent.id(), new LeasingSignRequest(
                "mch_1", "cs", null, null, LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), "k3")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("LEASING_INTENT_NOT_PENDING_SIGNING"));

        service.advanceIntent(intent.id(), new LeasingIntentAdvanceRequest("pending_signing", null, "k4"));
        ContractRecord contract = service.signIntent(intent.id(), new LeasingSignRequest(
                "mch_1", "cs", null, null, LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), "k5"));
        assertThat(contract.id()).isEqualTo("ctr_new");
        assertThat(repo.findLeadById(lead.id()).orElseThrow().status()).isEqualTo("signed");
        // AUD-003 边界：带既有 merchant_id 签约不新建绑定（其绑定由治理域管理），只有自动建商户路径才建。
        org.mockito.Mockito.verify(platformService, org.mockito.Mockito.never())
                .createBusinessEntityBinding(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void signIntentWithoutMerchantAutoCreatesFormalMerchant() {
        when(authz.currentProjectScopeQuietly()).thenReturn(Optional.of("lease-current-project"));
        // 1.4 决策A：merchant_id 空 → 签约自动建正式商户，带入线索联系人/业态、四类主体默认同一公司。
        when(contractService.createDraft(any())).thenReturn(new ContractRecord(
                "ctr_auto", "mch_auto", "cs", null, "cs", LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"),
                Map.of(), null, "none", null, Map.of(), List.of(), null, null, List.of(), "drafting", null,
                null, null, null, Instant.now(), Instant.now()));
        when(platformService.createMerchant(any())).thenReturn(new MerchantRecord(
                "mch_auto", "钱七", "retail", "cs", "cs", "cs", "钱七", null, "钱七", null, null, null,
                null, null, null, "leasing_auto", "active", Instant.now(), Instant.now()));
        LeasingLeadRecord lead = service.createLead(new LeasingLeadRequest(
                "钱七", "13800001111", "online_inquiry", "retail", "sp_1", null, null, "k1"));
        // 本例验证已归属意向的签约调用点；线索创建归属由其所属模块另审。
        repo.saveLead(new LeasingLeadRecord(lead.id(), lead.contactName(), lead.contactPhone(), lead.source(),
                lead.intendedBusinessType(), lead.intendedSpaceId(), lead.intendedAreaSqm(), lead.brandId(),
                lead.ownerUserId(), lead.status(), lead.lostOrHoldReason(), lead.lastFollowUpAt(),
                "lease-current-project", lead.createdAt(), lead.updatedAt()));
        LeasingIntentRecord intent = service.createIntent(new LeasingIntentRequest(lead.id(), "sp_1", null, Map.of(), "k2"));
        service.advanceIntent(intent.id(), new LeasingIntentAdvanceRequest("pending_signing", null, "k4"));

        ContractRecord contract = service.signIntent(intent.id(), new LeasingSignRequest(
                null, "cs", null, null, LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), "k5"));
        assertThat(contract.id()).isEqualTo("ctr_auto");

        org.mockito.ArgumentCaptor<MerchantProfileRequest> captor =
                org.mockito.ArgumentCaptor.forClass(MerchantProfileRequest.class);
        var order = org.mockito.Mockito.inOrder(platformService);
        order.verify(platformService).createMerchant(captor.capture());
        MerchantProfileRequest req = captor.getValue();
        assertThat(req.contactName()).isEqualTo("钱七");
        assertThat(req.contactPhone()).isEqualTo("13800001111");
        assertThat(req.merchantType()).isEqualTo("retail");
        org.junit.jupiter.api.Assertions.assertAll(
                () -> assertThat(req.projectId()).isEqualTo("lease-current-project"),
                () -> assertThat(req.contractEntityId()).isNull(),
                () -> assertThat(req.businessEntityId()).isNull(),
                () -> assertThat(req.settlementEntityId()).isNull());

        // AUD-003：自动建商户后必建"四主体"权威绑定（经营/合同/结算主体默认同一公司=cs）。
        org.mockito.ArgumentCaptor<com.complex.platform.governance.dto.BusinessEntityBindingRequest> bindingCaptor =
                org.mockito.ArgumentCaptor.forClass(com.complex.platform.governance.dto.BusinessEntityBindingRequest.class);
        order.verify(platformService).createBusinessEntityBinding(bindingCaptor.capture());
        com.complex.platform.governance.dto.BusinessEntityBindingRequest binding = bindingCaptor.getValue();
        assertThat(binding.merchantId()).isEqualTo("mch_auto");
        assertThat(binding.operationEntityId()).isEqualTo("cs");
        assertThat(binding.contractEntityId()).isEqualTo("cs");
        assertThat(binding.settlementEntityId()).isEqualTo("cs");
    }

    @Test
    void followUpWritesBackLastFollowUpAndReminderFiltersDueLeads() {
        LeasingLeadRecord lead = service.createLead(new LeasingLeadRequest(
                "孙八", null, "outbound", null, null, null, null, "k1"));
        service.createFollowUp(new LeasingFollowUpRequest("lead", lead.id(), "phone", "已电话沟通", null, "k2"));
        LeasingLeadRecord after = repo.findLeadById(lead.id()).orElseThrow();
        assertThat(after.lastFollowUpAt()).isNotNull();
        assertThat(after.status()).isEqualTo("following_up");

        // 刚跟进的不进提醒；构造一个 4 天前未跟进的线索 → 进提醒；signed 的排除。
        assertThat(service.listLeadsNeedingFollowUp().items()).extracting(LeasingLeadRecord::id).doesNotContain(lead.id());
        Instant old = Instant.now().minus(4, ChronoUnit.DAYS);
        repo.saveLead(new LeasingLeadRecord("lead_old", "老线索", null, "outbound", null, null, null, null, "u",
                "following_up", null, old, null, old, old));
        repo.saveLead(new LeasingLeadRecord("lead_signed", "已签", null, "outbound", null, null, null, null, "u",
                "signed", null, old, null, old, old));
        List<String> due = service.listLeadsNeedingFollowUp().items().stream().map(LeasingLeadRecord::id).toList();
        assertThat(due).contains("lead_old").doesNotContain("lead_signed");
    }

    // 招商工作台首屏无参加载：跟进列表全量（不传 ref_type/ref_id）返回所有跟进，不再 400。
    @Test
    void listFollowUpsWithoutRefReturnsAllScoped() {
        LeasingLeadRecord lead = service.createLead(new LeasingLeadRequest(
                "赵九", null, "outbound", null, null, null, null, "kf1"));
        service.createFollowUp(new LeasingFollowUpRequest("lead", lead.id(), "phone", "首访", null, "kf2"));
        service.createFollowUp(new LeasingFollowUpRequest("lead", lead.id(), "visit", "复访", null, "kf3"));
        assertThat(service.listFollowUps(null, null).items()).hasSize(2);
        // 带 ref 时仍走精确下钻。
        assertThat(service.listFollowUps("lead", lead.id()).items()).hasSize(2);
    }

    /** 内存仓储（实现 V161 重建后的 11 个方法）。 */
    private static final class InMemoryLeasingRepository implements LeasingRepository {
        private final Map<String, LeasingLeadRecord> leads = new LinkedHashMap<>();
        private final Map<String, LeasingIntentRecord> intents = new LinkedHashMap<>();
        private final Map<String, LeasingBrandRecord> brands = new LinkedHashMap<>();
        private final List<LeasingFollowUpRecord> followUps = new java.util.ArrayList<>();

        @Override
        public LeasingLeadRecord saveLead(LeasingLeadRecord lead) {
            leads.put(lead.id(), lead);
            return lead;
        }

        @Override
        public List<LeasingLeadRecord> listLeads(String projectScope) {
            return leads.values().stream()
                    .filter(l -> projectScope == null || projectScope.equals(l.projectId()))
                    .toList();
        }

        @Override
        public Optional<LeasingLeadRecord> findLeadById(String id) {
            return Optional.ofNullable(leads.get(id));
        }

        @Override
        public LeasingIntentRecord saveIntent(LeasingIntentRecord intent) {
            intents.put(intent.id(), intent);
            return intent;
        }

        @Override
        public List<LeasingIntentRecord> listIntents(String projectScope) {
            return intents.values().stream()
                    .filter(i -> projectScope == null || projectScope.equals(i.projectId()))
                    .toList();
        }

        @Override
        public Optional<LeasingIntentRecord> findIntentById(String id) {
            return Optional.ofNullable(intents.get(id));
        }

        @Override
        public LeasingBrandRecord saveBrand(LeasingBrandRecord brand) {
            brands.put(brand.id(), brand);
            return brand;
        }

        @Override
        public List<LeasingBrandRecord> listBrands(String projectScope) {
            return brands.values().stream()
                    .filter(b -> projectScope == null || projectScope.equals(b.projectId()))
                    .toList();
        }

        @Override
        public Optional<LeasingBrandRecord> findBrandById(String id) {
            return Optional.ofNullable(brands.get(id));
        }

        @Override
        public LeasingFollowUpRecord saveFollowUp(LeasingFollowUpRecord followUp) {
            followUps.add(followUp);
            return followUp;
        }

        @Override
        public List<LeasingFollowUpRecord> listFollowUpsByRef(String refType, String refId, String projectScope) {
            return followUps.stream().filter(f -> f.refType().equals(refType) && f.refId().equals(refId))
                    .filter(f -> projectScope == null || projectScope.equals(f.projectId()))
                    .toList();
        }

        @Override
        public List<LeasingFollowUpRecord> listAllFollowUps(String projectScope) {
            return followUps.stream()
                    .filter(f -> projectScope == null || projectScope.equals(f.projectId()))
                    .toList();
        }
    }
}
