package com.complex.platform.leasing;
// 文件版本：V0.08

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
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
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
    private final AuditEventService audit = mock(AuditEventService.class);
    private final SupervisorReviewService reviews = mock(SupervisorReviewService.class);
    private final LeasingBlacklistExceptionService blacklistExceptions = mock(LeasingBlacklistExceptionService.class);
    private LeasingService service;

    @BeforeEach
    void setUp() {
        when(authz.currentActor()).thenReturn(
                new AccessActor("usr_admin", "platform_admin_user", "platform_admin", "org_1", null, "active"));
        // 3.2 默认：平台管理员无 project scope（全量）；跨项目用例再覆写。
        when(authz.currentProjectScopeQuietly()).thenReturn(Optional.empty());
        when(authz.isPlatformAdmin(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        // 3.13 默认：空间存在性放行（专项用例再覆写为 false）。
        org.mockito.Mockito.when(platformService.spaceExists(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(true);
        // 第十八批第 30 条：关键阶段送统一审批，模拟返回待审复核单（恒阻断策略）。
        when(reviews.evaluate(any())).thenAnswer(inv -> pendingReview(inv.getArgument(0)));
        service = new LeasingService(repo, audit, authz, contractService, platformService, reviews, blacklistExceptions);
    }

    // 3.13：意向空间传了必须真实存在（挂空编号=400）；不传放行。
    @Test
    void createLeadRejectsNonexistentIntendedSpace() {
        org.mockito.Mockito.when(platformService.spaceExists(org.mockito.ArgumentMatchers.eq("space_ghost"), org.mockito.ArgumentMatchers.any())).thenReturn(false);
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
        // 18E（第 19 条 19-R2）：列表改按入口权限码可触及的项目取；本例不挂可跨项目角色，只到本人项目
        when(authz.projectReach(any(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(AuthorizationService.ProjectReach.ownOnly("prj_a"));
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

    /**
     * 第十八批第 30 条 R4（PGM L502「意向五阶段中进入方案报价、商务谈判须主管确认才能推进」）：送审不改阶段、记待确认；
     * 有待确认不能再推进；驳回阶段不变、清空待确认；批准才进入并记确认人。
     */
    @Test
    void intentKeyStageWaitsForSupervisorReview() {
        LeasingLeadRecord lead = service.createLead(new LeasingLeadRequest(
                "赵六", null, "exhibition", "retail", "sp_2", null, null, "k1"));
        LeasingIntentRecord intent = service.createIntent(new LeasingIntentRequest(lead.id(), "sp_2", null, Map.of(), "k2"));
        assertThat(intent.stage()).isEqualTo("initial_contact");
        assertThat(repo.findLeadById(lead.id()).orElseThrow().status()).isEqualTo("converted_to_intent");

        // 非关键阶段直接推进。
        service.advanceIntent(intent.id(), new LeasingIntentAdvanceRequest("requirement_confirmed", null, "k3"));
        LeasingIntentRecord sent = service.advanceIntent(intent.id(),
                new LeasingIntentAdvanceRequest("proposal_quoted", "usr_gm", "k4"));
        assertThat(sent.stage()).isEqualTo("requirement_confirmed");
        assertThat(sent.pendingStage()).isEqualTo("proposal_quoted");
        org.mockito.ArgumentCaptor<SupervisorReviewEvaluateRequest> sentReview =
                org.mockito.ArgumentCaptor.forClass(SupervisorReviewEvaluateRequest.class);
        verify(reviews).evaluate(sentReview.capture());
        assertThat(sentReview.getValue().actionType()).isEqualTo("leasing.intent.key_stage");
        assertThat(sentReview.getValue().objectId()).isEqualTo(intent.id());
        assertThat(sentReview.getValue().finalApproverId()).isEqualTo("usr_gm");
        assertThatThrownBy(() -> service.advanceIntent(intent.id(),
                new LeasingIntentAdvanceRequest("proposal_quoted", null, "k5")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("LEASING_INTENT_STAGE_PENDING_REVIEW"));

        service.keyStageDecided(decided(sent.pendingReviewId(), intent.id(), "rejected"));
        LeasingIntentRecord rejected = repo.findIntentById(intent.id()).orElseThrow();
        assertThat(rejected.stage()).isEqualTo("requirement_confirmed");
        assertThat(rejected.pendingStage()).isNull();
        assertThat(rejected.pendingReviewId()).isNull();

        LeasingIntentRecord again = service.advanceIntent(intent.id(),
                new LeasingIntentAdvanceRequest("proposal_quoted", null, "k6"));
        service.keyStageDecided(decided(again.pendingReviewId(), intent.id(), "approved"));
        LeasingIntentRecord advanced = repo.findIntentById(intent.id()).orElseThrow();
        assertThat(advanced.stage()).isEqualTo("proposal_quoted");
        assertThat(advanced.lastStageChangeBy()).isEqualTo("usr_admin");
        assertThat(advanced.pendingStage()).isNull();
        assertThat(advanced.pendingReviewId()).isNull();
    }

    /** 第十八批第 30 条 R1／R2：拉黑后凭例外放行单建的同品牌线索，转意向不再拦（线索建于拉黑之后）；只在被拦时用单。 */
    @Test
    void leadCreatedByBlacklistExceptionConvertsWithoutAnotherException() {
        LeasingBrandRecord brand = service.createBrand(new LeasingBrandRequest("回归品牌", "prospective", "餐饮", "r2b"));
        service.blacklistBrand(brand.id(), new LeasingBrandBlacklistRequest(true, "多次违约", "r2bl"));

        LeasingLeadRecord lead = service.createLead(new LeasingLeadRequest(
                "王五", null, "outbound", null, null, null, brand.id(), "r2l", "lbe_1"));
        verify(blacklistExceptions).consume(org.mockito.ArgumentMatchers.eq("lbe_1"), org.mockito.ArgumentMatchers.eq("lead_create"),
                org.mockito.ArgumentMatchers.eq(brand.id()), org.mockito.ArgumentMatchers.eq(lead.id()),
                org.mockito.ArgumentMatchers.isNull());
        LeasingIntentRecord intent = service.createIntent(new LeasingIntentRequest(lead.id(), null, null, Map.of(), "r2i", "lbe_1"));
        assertThat(intent.stage()).isEqualTo("initial_contact");
        verify(blacklistExceptions, org.mockito.Mockito.times(1)).consume(any(), any(), any(), any(), any());
    }

    /**
     * CG-01（PGM「招商运营细则」：「合同审批全部通过、生效后转为已签约」）：签约只起草合同草稿，线索状态不动；
     * 已签约要等合同生效（{@link #contractEffectiveMarksSourceLeadSignedOnceWithAudit}）。
     */
    @Test
    void signPendingSigningIntentCreatesContractDraftWithoutMarkingLeadSigned() {
        when(contractService.createDraft(any(), any())).thenReturn(new ContractRecord(
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

        advanceTo(intent.id(), "pending_signing");
        String statusBeforeSign = repo.findLeadById(lead.id()).orElseThrow().status();
        ContractRecord contract = service.signIntent(intent.id(), new LeasingSignRequest(
                "mch_1", "cs", null, null, LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), "k5"));
        assertThat(contract.id()).isEqualTo("ctr_new");
        // 线索状态保持签约前原值：合同只是起草，未经审批、未生效。
        assertThat(repo.findLeadById(lead.id()).orElseThrow().status()).isEqualTo(statusBeforeSign);
        assertThat(statusBeforeSign).isNotEqualTo("signed");
        // 合同绑到来源意向，供生效后反查。
        assertThat(repo.contractOfIntent(intent.id())).isEqualTo("ctr_new");
        // AUD-003 边界：带既有 merchant_id 签约不新建绑定（其绑定由治理域管理），只有自动建商户路径才建。
        org.mockito.Mockito.verify(platformService, org.mockito.Mockito.never())
                .createBusinessEntityBinding(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    /**
     * 第三轮裁决第 1 条「纯提成合同的押金由招商按合同约定金额直接录入」：纯提成意向（rent_terms.rentBillingMode=turnover_only）签约时
     * 招商签约表单录入的约定押金随签约喂入合同草稿（不得为零等校验在合同域 createDraft，另测）。
     */
    @Test
    void signPassesEnteredAgreedDepositIntoContractDraft() {
        when(contractService.createDraft(any(), any())).thenReturn(new ContractRecord(
                "ctr_tov", "mch_1", "cs", null, "cs", LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"),
                Map.of("rentBillingMode", "turnover_only"), null, "none", 500000L, Map.of(), List.of(), null, null, List.of(),
                "drafting", null, null, null, null, Instant.now(), Instant.now()));
        LeasingLeadRecord lead = service.createLead(new LeasingLeadRequest(
                "周九", null, "online_inquiry", null, null, null, null, "tov1"));
        LeasingIntentRecord intent = service.createIntent(new LeasingIntentRequest(lead.id(), null, null,
                Map.of("rentBillingMode", "turnover_only"), "tov2"));
        advanceTo(intent.id(), "pending_signing");
        service.signIntent(intent.id(), new LeasingSignRequest(
                "mch_1", "cs", null, null, LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), "tov4", 500000L));
        org.mockito.ArgumentCaptor<com.complex.platform.contract.dto.ContractDraftRequest> draft =
                org.mockito.ArgumentCaptor.forClass(com.complex.platform.contract.dto.ContractDraftRequest.class);
        verify(contractService).createDraft(draft.capture(), org.mockito.ArgumentMatchers.eq("leasing.convert"));
        assertThat(draft.getValue().depositCent()).isEqualTo(500000L);
        assertThat(draft.getValue().rentTerms()).containsEntry("rentBillingMode", "turnover_only");
    }

    /** CG-01：合同生效 → 来源线索置已签约，留痕一次；重复生效回调（重放）不二次改写、不二次留痕。 */
    @Test
    void contractEffectiveMarksSourceLeadSignedOnceWithAudit() {
        when(contractService.createDraft(any(), any())).thenReturn(new ContractRecord(
                "ctr_eff", "mch_1", "cs", null, "cs", LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"),
                Map.of(), null, "none", null, Map.of(), List.of(), null, null, List.of(), "drafting", null,
                null, null, null, Instant.now(), Instant.now()));
        LeasingLeadRecord lead = service.createLead(new LeasingLeadRequest(
                "孙八", null, "referral", null, null, null, null, "e1"));
        LeasingIntentRecord intent = service.createIntent(new LeasingIntentRequest(lead.id(), null, null, Map.of(), "e2"));
        advanceTo(intent.id(), "pending_signing");
        service.signIntent(intent.id(), new LeasingSignRequest(
                "mch_1", "cs", null, null, LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), "e4"));
        assertThat(repo.findLeadById(lead.id()).orElseThrow().status()).isNotEqualTo("signed");

        service.markSourceLeadSigned("ctr_eff", "act_1");
        assertThat(repo.findLeadById(lead.id()).orElseThrow().status()).isEqualTo("signed");
        service.markSourceLeadSigned("ctr_eff", "act_1");
        assertThat(repo.findLeadById(lead.id()).orElseThrow().status()).isEqualTo("signed");
        org.mockito.Mockito.verify(audit, org.mockito.Mockito.times(1)).recordSuccess(
                org.mockito.ArgumentMatchers.eq("leasing.lead.signed"), any(), org.mockito.ArgumentMatchers.eq("leasing_lead"),
                org.mockito.ArgumentMatchers.eq(lead.id()), any(), any(), any());
    }

    /** CG-01：不是招商签约起草的合同（无绑定意向）生效 → 无事发生、不留痕。 */
    @Test
    void contractEffectiveWithoutLeasingSourceIsNoOp() {
        service.markSourceLeadSigned("ctr_not_from_leasing", "act_x");
        org.mockito.Mockito.verify(audit, org.mockito.Mockito.never()).recordSuccess(
                org.mockito.ArgumentMatchers.eq("leasing.lead.signed"), any(), any(), any(), any(), any(), any());
    }

    /** CG-01（PGM「已签约后不得回退至未签约状态」）：已签约线索改任何其他状态一律 409，状态不变。 */
    @Test
    void signedLeadCannotBeRolledBackByStatusUpdate() {
        Instant now = Instant.now();
        repo.saveLead(new LeasingLeadRecord("lead_done", "已签约客户", null, "outbound", null, null, null, null, "u",
                "signed", null, now, null, now, now));
        for (String target : List.of("pending_follow_up", "following_up", "converted_to_intent", "negotiating",
                "lost", "on_hold")) {
            assertThatThrownBy(() -> service.updateLeadStatus("lead_done",
                    new LeasingLeadStatusRequest(target, "试图回退", "rb_" + target)))
                    .isInstanceOfSatisfying(ApiException.class,
                            e -> assertThat(e.code()).isEqualTo("LEASING_LEAD_ALREADY_SIGNED"));
            assertThat(repo.findLeadById("lead_done").orElseThrow().status()).isEqualTo("signed");
        }
    }

    @Test
    void signIntentWithoutMerchantAutoCreatesFormalMerchant() {
        when(authz.currentProjectScopeQuietly()).thenReturn(Optional.of("lease-current-project"));
        // 1.4 决策A：merchant_id 空 → 签约自动建正式商户，带入线索联系人/业态、四类主体默认同一公司。
        when(contractService.createDraft(any(), any())).thenReturn(new ContractRecord(
                "ctr_auto", "mch_auto", "cs", null, "cs", LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"),
                Map.of(), null, "none", null, Map.of(), List.of(), null, null, List.of(), "drafting", null,
                null, null, null, Instant.now(), Instant.now()));
        when(platformService.createMerchantOnLeasingSign(any())).thenReturn(new MerchantRecord(
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
        advanceTo(intent.id(), "pending_signing");

        ContractRecord contract = service.signIntent(intent.id(), new LeasingSignRequest(
                null, "cs", null, null, LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), "k5"));
        assertThat(contract.id()).isEqualTo("ctr_auto");

        org.mockito.ArgumentCaptor<MerchantProfileRequest> captor =
                org.mockito.ArgumentCaptor.forClass(MerchantProfileRequest.class);
        var order = org.mockito.Mockito.inOrder(platformService);
        order.verify(platformService).createMerchantOnLeasingSign(captor.capture());
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
        order.verify(platformService).createBusinessEntityBinding(bindingCaptor.capture(), org.mockito.ArgumentMatchers.eq("leasing.convert"));
        com.complex.platform.governance.dto.BusinessEntityBindingRequest binding = bindingCaptor.getValue();
        assertThat(binding.merchantId()).isEqualTo("mch_auto");
        assertThat(binding.operationEntityId()).isEqualTo("cs");
        assertThat(binding.contractEntityId()).isEqualTo("cs");
        assertThat(binding.settlementEntityId()).isEqualTo("cs");
    }

    /**
     * FS-024 复核：线索未定业态（null）或意向业态就是「其他」的签约起草走不触发主管公司必填的签约建档入口
     * （PlatformService.createMerchantOnLeasingSign），商户业态落 other、主管公司与理由为空，签约不再 400。
     */
    @Test
    void signIntentWithUndecidedOrOtherBusinessTypeAutoCreatesOtherMerchantWithoutSupervisingCompany() {
        when(contractService.createDraft(any(), any())).thenReturn(new ContractRecord(
                "ctr_other", "mch_other", "cs", null, "cs", LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"),
                Map.of(), null, "none", null, Map.of(), List.of(), null, null, List.of(), "drafting", null,
                null, null, null, Instant.now(), Instant.now()));
        MerchantRecord other = new MerchantRecord(
                "mch_other", "孙八", "other", null, null, null, "孙八", null, "孙八", null, null, null,
                null, null, null, null, "active", Instant.now(), Instant.now());
        when(platformService.createMerchantOnLeasingSign(any())).thenReturn(other);
        // 后台档案写口（createMerchant）对「其他」业态必填主管公司；签约建档不得走它（下面 never() 断言）。
        when(platformService.createMerchant(any())).thenReturn(other);
        int n = 0;
        for (String businessType : java.util.Arrays.asList(null, "other")) {
            n++;
            LeasingLeadRecord lead = service.createLead(new LeasingLeadRequest(
                    "孙八", "13800002222", "online_inquiry", businessType, "sp_1", null, null, "k_other_lead_" + n));
            LeasingIntentRecord intent = service.createIntent(new LeasingIntentRequest(lead.id(), "sp_1", null, Map.of(), "k_other_intent_" + n));
            advanceTo(intent.id(), "pending_signing");
            ContractRecord contract = service.signIntent(intent.id(), new LeasingSignRequest(
                    null, "cs", null, null, LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), "k_other_sign_" + n));
            assertThat(contract.id()).isEqualTo("ctr_other");
        }
        org.mockito.ArgumentCaptor<MerchantProfileRequest> captor = org.mockito.ArgumentCaptor.forClass(MerchantProfileRequest.class);
        verify(platformService, org.mockito.Mockito.times(2)).createMerchantOnLeasingSign(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(req -> {
            assertThat(req.merchantType()).isEqualTo("other");
            assertThat(req.supervisingCompanyId()).isNull();
            assertThat(req.supervisingCompanyReason()).isNull();
        });
        verify(platformService, org.mockito.Mockito.never()).createMerchant(any());
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
    @Test
    void manualStatusCannotDeclareUnsignedLeadSigned() {
        LeasingLeadRecord lead = service.createLead(new LeasingLeadRequest(
                "客户", null, "outbound", null, null, null, null, "manual-signed"));
        assertThatThrownBy(() -> service.updateLeadStatus(lead.id(),
                new LeasingLeadStatusRequest("signed", null, "manual-status")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("LEASING_SIGNED_REQUIRES_EFFECTIVE_CONTRACT"));
        assertThat(repo.findLeadById(lead.id()).orElseThrow().status()).isEqualTo("pending_follow_up");
    }

    @Test
    void signedLeadCannotCreateAnotherIntent() {
        Instant now = Instant.now();
        repo.saveLead(new LeasingLeadRecord("lead_signed_intent", "已签客户", null, "outbound",
                null, null, null, null, "u", "signed", null, now, null, now, now));
        assertThatThrownBy(() -> service.createIntent(new LeasingIntentRequest(
                "lead_signed_intent", null, null, Map.of(), "signed-intent")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("LEASING_LEAD_ALREADY_SIGNED"));
        assertThat(repo.listIntents(null)).isEmpty();
        assertThat(repo.findLeadById("lead_signed_intent").orElseThrow().status()).isEqualTo("signed");
    }

    /** 第十八批第 30 条 R1（PGM L502「黑名单品牌拦截……由拉黑前既有线索新建意向」）：拉黑前建的线索转意向 409，意向不建、线索状态不动。 */
    @Test
    void leadCreatedBeforeBrandBlacklistedCannotConvertToIntent() {
        LeasingBrandRecord brand = service.createBrand(new LeasingBrandRequest("旧品牌", "prospective", "餐饮", "r1b"));
        Instant earlier = Instant.now().minus(1, ChronoUnit.HOURS);
        repo.saveLead(new LeasingLeadRecord("lead_pre_black", "老客户", null, "outbound", null, null, null, brand.id(), "u",
                "following_up", null, null, null, earlier, earlier));
        service.blacklistBrand(brand.id(), new LeasingBrandBlacklistRequest(true, "多次违约", "r1bl"));

        assertThatThrownBy(() -> service.createIntent(new LeasingIntentRequest("lead_pre_black", null, null, Map.of(), "r1i")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("LEASING_BRAND_BLACKLISTED"));
        assertThat(repo.listIntents(null)).isEmpty();
        assertThat(repo.findLeadById("lead_pre_black").orElseThrow().status()).isEqualTo("following_up");
    }

    /** 第十八批第 30 条 R3（PGM L500 五阶段顺序推进；ENUM L249）：不得向前跳级，被拒阶段不变；逐级推进与回退照常。 */
    @Test
    void intentStageCannotSkipForward() {
        LeasingLeadRecord lead = service.createLead(new LeasingLeadRequest(
                "跳级客户", null, "outbound", null, null, null, null, "r3l"));
        LeasingIntentRecord intent = service.createIntent(new LeasingIntentRequest(lead.id(), null, null, Map.of(), "r3i"));

        assertThatThrownBy(() -> service.advanceIntent(intent.id(), new LeasingIntentAdvanceRequest("pending_signing", null, "r3a")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("LEASING_INTENT_STAGE_SKIP"));
        assertThat(repo.findIntentById(intent.id()).orElseThrow().stage()).isEqualTo("initial_contact");
        assertThat(service.advanceIntent(intent.id(), new LeasingIntentAdvanceRequest("requirement_confirmed", null, "r3b")).stage())
                .isEqualTo("requirement_confirmed");
        assertThatThrownBy(() -> service.advanceIntent(intent.id(), new LeasingIntentAdvanceRequest("negotiation", null, "r3c")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("LEASING_INTENT_STAGE_SKIP"));

        LeasingIntentRecord at = repo.findIntentById(intent.id()).orElseThrow();
        repo.saveIntent(new LeasingIntentRecord(at.id(), at.leadId(), "negotiation", at.intendedSpaceId(), at.intendedAreaSqm(),
                at.rentTerms(), at.lastStageChangeAt(), at.lastStageChangeBy(), at.projectId(), at.createdAt(), at.updatedAt()));
        assertThat(service.advanceIntent(intent.id(), new LeasingIntentAdvanceRequest("requirement_confirmed", null, "r3d")).stage())
                .isEqualTo("requirement_confirmed");
    }

    /** 逐级推进到目标阶段；方案报价、商务谈判模拟主管批准（第十八批第 30 条：不得跳级、关键阶段须主管确认）。 */
    private void advanceTo(String intentId, String target) {
        List<String> order = List.of("initial_contact", "requirement_confirmed", "proposal_quoted", "negotiation", "pending_signing");
        String stage = repo.findIntentById(intentId).orElseThrow().stage();
        for (int i = order.indexOf(stage) + 1; i <= order.indexOf(target); i++) {
            String next = order.get(i);
            LeasingIntentRecord after = service.advanceIntent(intentId,
                    new LeasingIntentAdvanceRequest(next, null, "adv_" + intentId + "_" + next));
            if (after.pendingReviewId() != null) {
                service.keyStageDecided(decided(after.pendingReviewId(), intentId, "approved"));
            }
        }
    }

    private static SupervisorReviewDecisionRecord pendingReview(SupervisorReviewEvaluateRequest r) {
        return new SupervisorReviewDecisionRecord("review_" + r.idempotencyKey(), "leasing_intent_key_stage", r.actionType(),
                r.objectType(), r.objectId(), true, "pending_review", 1, null, "blocked", r.violationSummary(), List.of(),
                Instant.now(), "usr_admin", "one_level_up", null);
    }

    private static SupervisorReviewDecisionRecord decided(String reviewId, String intentId, String status) {
        return new SupervisorReviewDecisionRecord(reviewId, "leasing_intent_key_stage", "leasing.intent.key_stage",
                "leasing_intent", intentId, false, status, 1, 1, status, null, List.of(), Instant.now(), "usr_admin",
                "one_level_up", null);
    }

    private static final class InMemoryLeasingRepository implements LeasingRepository {
        @Override
        public Optional<LeasingLeadRecord> findLeadByIdForUpdate(String id) {
            return findLeadById(id);
        }

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
        public Optional<LeasingIntentRecord> findIntentByPendingReviewId(String reviewId) {
            return intents.values().stream().filter(i -> reviewId.equals(i.pendingReviewId())).findFirst();
        }

        // CG-01：意向 ↔ 合同绑定 + 线索 CAS 置已签约（与 JdbcLeasingRepository 同语义）。
        private final Map<String, String> intentContracts = new LinkedHashMap<>();

        String contractOfIntent(String intentId) {
            return intentContracts.get(intentId);
        }

        @Override
        public void bindIntentContract(String intentId, String contractId) {
            intentContracts.put(intentId, contractId);
        }

        @Override
        public Optional<LeasingIntentRecord> findIntentByContractId(String contractId) {
            return intentContracts.entrySet().stream()
                    .filter(e -> contractId.equals(e.getValue()))
                    .map(e -> intents.get(e.getKey()))
                    .filter(java.util.Objects::nonNull)
                    .findFirst();
        }

        @Override
        public boolean markLeadSignedIfNotSigned(String leadId, Instant now) {
            LeasingLeadRecord lead = leads.get(leadId);
            if (lead == null || "signed".equals(lead.status())) {
                return false;
            }
            leads.put(leadId, new LeasingLeadRecord(lead.id(), lead.contactName(), lead.contactPhone(), lead.source(),
                    lead.intendedBusinessType(), lead.intendedSpaceId(), lead.intendedAreaSqm(), lead.brandId(),
                    lead.ownerUserId(), "signed", lead.lostOrHoldReason(), lead.lastFollowUpAt(), lead.projectId(),
                    lead.createdAt(), now));
            return true;
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
