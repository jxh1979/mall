package com.complex.platform.contract;

// 文件版本：V0.24

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.contract.dto.ContractActivateRequest;
import com.complex.platform.contract.dto.ContractApprovalFlowRequest;
import com.complex.platform.contract.dto.ContractAttachmentRequest;
import com.complex.platform.contract.dto.ContractChangeRequest;
import com.complex.platform.contract.dto.ContractDraftRequest;
import com.complex.platform.contract.dto.ContractDraftRentTermsRequest;
import com.complex.platform.contract.dto.ContractRentBillRequest;
import com.complex.platform.contract.dto.ContractResolveDisputeRequest;
import com.complex.platform.contract.dto.ContractScopeRequestRequest;
import com.complex.platform.contract.dto.ContractSettleRequest;
import com.complex.platform.contract.dto.ContractSubmitRequest;
import com.complex.platform.contract.dto.DepositMonthsSettingRequest;
import com.complex.platform.contract.dto.DepositRefundWaitSettingRequest;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.PlatformRepository;
import com.complex.platform.governance.SpaceRecord;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 合同域 unit 测试（V162 重建；HP-C01 起审批走统一审批模型）：提交/变更按三类模板开复核单（指定主线最终审批人＋平行部门线），
 * 复核单终态经 onReviewDecided 回写（全部通过→待生效/应用变更，驳回→退回起草/变更驳回）、生效固化快照、退租争议挂起+裁决。无 DB。
 */
class ContractServiceTests {

    private final InMemoryContractRepository repo = new InMemoryContractRepository();
    private final AuthorizationService authz = mock(AuthorizationService.class);
    private final AuditEventService audit = mock(AuditEventService.class);
    // HP-C01：统一审批引擎以 mock 接入——evaluate 入参逐条记录，返回待审复核单；终态由用例经 onReviewDecided 回灌。
    private final SupervisorReviewService reviews = mock(SupervisorReviewService.class);
    private final List<SupervisorReviewEvaluateRequest> evaluated = new ArrayList<>();
    private ContractService service;
    private RecordingTerminationPort terminationPort;

    /** 三条平行线（业务/财务/法务）各自的最终审批人，与平台默认模板的 required_line_codes 对应。 */
    private static final List<ParallelLine> THREE_LINES = List.of(
            new ParallelLine("business_review", "usr_biz"),
            new ParallelLine("finance_review", "usr_fin"),
            new ParallelLine("legal_review", "usr_legal"));

    @BeforeEach
    void setUp() {
        when(authz.currentActor()).thenReturn(
                new AccessActor("usr_admin", "platform_admin_user", "platform_admin", "org_1", null, "active"));
        // 82b-R3：模拟授权服务把上面的平台管理员当平台管理员（合同项目闸放行）；等待期项目判定按真实口径（不带 400、带了原样返回）。
        when(authz.isPlatformAdmin(any())).thenReturn(true);
        when(authz.requireReachableProject(any(), eq("contract.deposit_refund_wait.manage"), any())).thenAnswer(invocation -> {
            String project = invocation.getArgument(0);
            if (project == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "必须指定综合体项目");
            }
            return project;
        });
        when(reviews.evaluate(any())).thenAnswer(invocation -> {
            SupervisorReviewEvaluateRequest request = invocation.getArgument(0);
            evaluated.add(request);
            return decision("review_" + evaluated.size(), "pending_review", request.actionType(), request.objectType(),
                    request.objectId());
        });
        terminationPort = new RecordingTerminationPort();
        terminationPort.ledger = repo;
        service = new ContractService(repo, audit, authz, null, null, null, null, terminationPort, null, reviews,
                new com.complex.platform.common.idempotency.IdempotencyService(
                        new com.complex.platform.support.InMemoryIdempotencyRepository(),
                        new com.fasterxml.jackson.databind.ObjectMapper()));
    }

    /**
     * 变更请求：主线最终审批人 usr_gm + 平台默认「变更」模板的三线最终审批人。
     * HP-C02-R6：退租/提前终止载荷未给退租日时补默认 2026-08-31（裁决第 2 条「发起时必填退租日」；缺退租日的拒绝另有专测）。
     */
    private static ContractChangeRequest change(String key, String type, Map<String, Object> payload) {
        Map<String, Object> actual = payload;
        if (("move_out".equals(type) || "early_termination".equals(type)) && !payload.containsKey("moveOutDate")
                && !payload.containsKey("move_out_date")) {
            actual = new LinkedHashMap<>(payload);
            actual.put("moveOutDate", "2026-08-31");
        }
        return new ContractChangeRequest(key, type, actual, "usr_gm", THREE_LINES, null);
    }

    private static ContractScopeRequestRequest scopeRequest(String key, String category, String reason,
            String grantType, LocalDate validUntil) {
        return new ContractScopeRequestRequest(key, category, reason, grantType, validUntil, "usr_gm", THREE_LINES);
    }

    private static SupervisorReviewDecisionRecord decision(String reviewId, String status, String actionType,
            String objectType, String objectId) {
        boolean pending = "pending_review".equals(status);
        return new SupervisorReviewDecisionRecord(reviewId, "contract_" + actionType.substring("contract.".length()),
                actionType, objectType, objectId, pending, status, 1, null, pending ? "blocked" : status, null, List.of(),
                Instant.now(), "usr_admin", "one_level_up", null);
    }

    // ============ HP-C01：合同三类审批接入统一审批模型（BSM L1349 / PGM L528、L536） ============
    /**
     * BSM L1349「发起时指定这条主线的最终审批人……业务审核、财务审核、法务审核作为平行的相关部门线，各指定该部门的最终审批人」：
     * 提交即按「新建」类模板开复核单——actionType=contract.new、主线最终审批人=请求值、平行线=模板三线各自的最终审批人；
     * 合同置待确认并挂上复核单号。
     */
    @Test
    void submitOpensReviewWithTemplateLinesAndDesignatedFinalApprover() {
        ContractRecord c = newDraft();
        ContractRecord submitted = service.submit(c.id(),
                new ContractSubmitRequest("k_sub_open", "usr_gm", THREE_LINES, "新签提交"));
        assertThat(submitted.lifecycleStatus()).isEqualTo("pending_confirmation");
        assertThat(submitted.reviewId()).isEqualTo("review_1");
        assertThat(evaluated).hasSize(1);
        SupervisorReviewEvaluateRequest request = evaluated.get(0);
        assertThat(request.actionType()).isEqualTo("contract.new");
        assertThat(request.objectType()).isEqualTo("contract");
        assertThat(request.objectId()).isEqualTo(c.id());
        assertThat(request.finalApproverId()).isEqualTo("usr_gm");
        assertThat(request.idempotencyKey()).isEqualTo("k_sub_open:review");
        assertThat(request.operatorNote()).isEqualTo("新签提交");
        assertThat(request.parallelLines()).extracting(ParallelLine::lineCode, ParallelLine::finalApproverId)
                .containsExactly(tuple("business_review", "usr_biz"), tuple("finance_review", "usr_fin"),
                        tuple("legal_review", "usr_legal"));
    }

    /** BSM L1349「主线的最终审批人（默认总经理）」：未指定时取「新建」类模板配置的默认最终审批人。 */
    @Test
    void submitFallsBackToTemplateDefaultFinalApproverWhenNotDesignated() {
        service.saveApprovalFlow(new ContractApprovalFlowRequest("k_tpl_default", null, "new", "平台默认·新建",
                List.of("business_review", "finance_review", "legal_review"), "usr_admin", null, null));
        ContractRecord c = newDraft();
        service.submit(c.id(), new ContractSubmitRequest("k_sub_default", null, THREE_LINES, null));
        assertThat(evaluated).hasSize(1);
        assertThat(evaluated.get(0).finalApproverId()).isEqualTo("usr_admin");
    }

    /** 主线最终审批人请求与模板皆空 → 400 CONTRACT_FINAL_APPROVER_REQUIRED（fail-closed，不开复核单、合同仍草拟中）。 */
    @Test
    void submitWithoutFinalApproverAndNoTemplateDefaultIsRejected() {
        ContractRecord c = newDraft();
        assertThatThrownBy(() -> service.submit(c.id(), new ContractSubmitRequest("k_sub_nofa", null, THREE_LINES, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTRACT_FINAL_APPROVER_REQUIRED"));
        assertThat(evaluated).isEmpty();
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("drafting");
    }

    /** 模板要求的每条平行线都须指定最终审批人：缺法务线 → CONTRACT_APPROVAL_LINE_FINAL_APPROVER_REQUIRED；模板外的线 → 拒绝。 */
    @Test
    void submitMissingRequiredLineFinalApproverIsRejected() {
        ContractRecord c = newDraft();
        assertThatThrownBy(() -> service.submit(c.id(), new ContractSubmitRequest("k_sub_noline", "usr_gm",
                List.of(new ParallelLine("business_review", "usr_biz"), new ParallelLine("finance_review", "usr_fin")), null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTRACT_APPROVAL_LINE_FINAL_APPROVER_REQUIRED"));
        List<ParallelLine> extra = new ArrayList<>(THREE_LINES);
        extra.add(new ParallelLine("tax_review", "usr_tax"));
        assertThatThrownBy(() -> service.submit(c.id(), new ContractSubmitRequest("k_sub_extra", "usr_gm", extra, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTRACT_APPROVAL_LINE_NOT_IN_TEMPLATE"));
        assertThat(evaluated).isEmpty();
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("drafting");
    }

    /**
     * BSM L1349「合同的新建、到期终止、变更分别使用各自配置的审批流程模板」：同一综合体 new=三线、change=仅业务审核线时，
     * 变更与超范围申请都按「变更」类模板开复核单（actionType=contract.change、线集=业务审核线），不沿用「新建」模板的三线。
     */
    @Test
    void changeAndScopeGrantUseChangeTemplateNotNewTemplate() {
        ContractRecord c = effectiveContract();   // 新建走三线模板
        service.saveApprovalFlow(new ContractApprovalFlowRequest("k_tpl_change", null, "change", "平台默认·变更",
                List.of("business_review"), null, null, null));
        evaluated.clear();
        List<ParallelLine> businessOnly = List.of(new ParallelLine("business_review", "usr_biz"));
        ContractChangeRecord raised = service.raiseChange(c.id(), new ContractChangeRequest("k_ch_tpl", "rent_change",
                Map.of("baseRentCent", 120000), "usr_gm", businessOnly, "租金调整"));
        assertThat(raised.reviewId()).isEqualTo("review_1");
        ContractChangeRecord grant = service.requestScopeGrant(c.id(), new ContractScopeRequestRequest("k_sg_tpl",
                "spirit", "客人要烈酒", "permanent", null, "usr_gm", businessOnly));
        assertThat(grant.reviewId()).isEqualTo("review_2");
        assertThat(evaluated).hasSize(2);
        assertThat(evaluated).extracting(SupervisorReviewEvaluateRequest::actionType,
                SupervisorReviewEvaluateRequest::objectType, SupervisorReviewEvaluateRequest::objectId)
                .containsExactly(tuple("contract.change", "contract_change", raised.id()),
                        tuple("contract.change", "contract_change", grant.id()));
        assertThat(evaluated).allSatisfy(request -> assertThat(request.parallelLines())
                .extracting(ParallelLine::lineCode).containsExactly("business_review"));
        // 沿用「新建」模板三线发起变更 → 财务/法务线不在变更模板内，拒绝。
        assertThatThrownBy(() -> service.raiseChange(c.id(), new ContractChangeRequest("k_ch_three", "rent_change",
                Map.of("baseRentCent", 130000), "usr_gm", THREE_LINES, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTRACT_APPROVAL_LINE_NOT_IN_TEMPLATE"));
    }

    /** 统一审批服务未注入（旧构造）→ 409 CONTRACT_APPROVAL_UNAVAILABLE，不得静默跳过审批把合同推进待确认。 */
    @Test
    void submitWithoutReviewServiceFailsClosed() {
        ContractService noReviews = new ContractService(repo, mock(AuditEventService.class), authz, null, null,
                terminationPort);
        ContractRecord c = newDraft();
        assertThatThrownBy(() -> noReviews.submit(c.id(), new ContractSubmitRequest("k_sub_nosvc", "usr_gm", THREE_LINES, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTRACT_APPROVAL_UNAVAILABLE"));
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("drafting");
    }

    @Test
    void renewalJobScheduledUsesBeijingCalendarDayNotUtc() {
        // 所在地时间口径(2026-08-02 用户拍板):续约提醒扫描基准日按北京日历日,不按 UTC。
        // 固定时刻 = 北京 2026-08-03 02:00(= UTC 2026-08-02 18:00):北京口径 asOf=08-03,旧 UTC 口径为 08-02。
        ContractService mockService = mock(ContractService.class);
        new ContractRenewalNotificationJob(mockService,
                Clock.fixed(Instant.parse("2026-08-02T18:00:00Z"), ZoneOffset.UTC), true)
                .scheduledNotifyRenewals();
        verify(mockService).processMerchantFeeRenewalNotifications(
                eq(LocalDate.parse("2026-08-03")), anyInt());
    }

    // 3.13：草稿带意向空间——被生效合同占用即起草拒(409)；未占用则预绑定；提交审批时被占同样拒。
    @Test
    void draftWithOccupiedSpaceIsRejectedEarly() {
        repo.occupiedSpaces.add("space_a");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createDraft(new ContractDraftRequest(
                "k_sp1", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of(),
                null, null, null, null, null, List.of(), null, "餐饮", List.of("space_a"))))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.code()).isEqualTo("CONTRACT_SPACE_CONFLICT"));
    }

    @Test
    void draftWithFreeSpacePreBindsAndSubmitReChecksConflict() {
        ContractRecord draft = service.createDraft(new ContractDraftRequest(
                "k_sp2", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of(),
                null, null, null, null, null, List.of(), null, "餐饮", List.of("space_b")));
        org.assertj.core.api.Assertions.assertThat(repo.listContractSpaceIds(draft.id())).containsExactly("space_b");

        // 审批期间空间被别的生效合同占用 → 提交审批被拒（审批人不该审注定冲突的单）。
        repo.occupiedSpaces.add("space_b");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> submit(service, draft, "k_sp2_submit"))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.code()).isEqualTo("CONTRACT_SPACE_CONFLICT"));
    }

    // AUD-002③：幂等命中(重放)也必须走业务校验——同键但请求非法(commission_mode 越界)仍被拒，不被短路绕过。
    @Test
    void idempotentReplayStillEnforcesValidation() {
        service.createDraft(new ContractDraftRequest("k_idem", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of(),
                "fixed_ratio", null, null, null, null, List.of(), null, null));
        // 同一 idempotencyKey 但携带非法 commission_mode → 落到既有草稿短路前先过校验 → VALIDATION_FAILED（未被幂等绕过）。
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createDraft(new ContractDraftRequest(
                "k_idem", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of(),
                "NOT_A_REAL_MODE", null, null, null, null, List.of(), null, null)))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
    }

    private ContractRecord newDraft() {
        return service.createDraft(new ContractDraftRequest("k_draft", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("baseRentCent", 100000),
                "fixed_ratio", "none", null, Map.of("breach", "2x"),
                List.of(Map.of("item", "deposit_offset"), Map.of("item", "unpaid_fee")), List.of(), null, "餐饮"));
    }

    /** 提交审批：主线最终审批人 usr_gm + 三线最终审批人（平台默认「新建」模板）。 */
    private static ContractRecord submit(ContractService svc, ContractRecord c, String key) {
        return svc.submit(c.id(), new ContractSubmitRequest(key, "usr_gm", THREE_LINES, "提交审批"));
    }

    /** 回灌合同复核单终态（approved / rejected），返回回写后的合同。 */
    private ContractRecord decideContract(ContractService svc, String contractId, String status) {
        String reviewId = repo.findContractById(contractId).orElseThrow().reviewId();
        svc.onReviewDecided(decision(reviewId, status, "contract.new", "contract", contractId));
        return repo.findContractById(contractId).orElseThrow();
    }

    private ContractRecord approveContract(String contractId) {
        return decideContract(service, contractId, "approved");
    }

    /** 回灌变更复核单终态（approved / rejected），返回回写后的变更。 */
    private ContractChangeRecord decideChange(String changeId, String status) {
        String reviewId = repo.findChangeById(changeId).orElseThrow().reviewId();
        service.onReviewDecided(decision(reviewId, status, "contract.change", "contract_change", changeId));
        return repo.findChangeById(changeId).orElseThrow();
    }

    /** 结算开账请求：到期结算按「到期终止」类模板开复核单（主线最终审批人 usr_gm + 三线）；退租/提前终止不用审批入参。 */
    private static ContractSettleRequest settleReq(String key, String trigger, String disputeReason) {
        return new ContractSettleRequest(key, trigger, disputeReason, "usr_gm", THREE_LINES, null);
    }

    /** 回灌到期结算单「到期终止」复核单终态（approved / rejected），返回回写后的结算单。 */
    private ContractSettlementRecord decideExpiry(String settlementId, String status) {
        String reviewId = repo.findSettlementById(settlementId).orElseThrow().reviewId();
        service.onReviewDecided(decision(reviewId, status, "contract.expiry_termination", "contract_settlement", settlementId));
        return repo.findSettlementById(settlementId).orElseThrow();
    }

    private ContractSettlementRecord approveExpiry(String settlementId) {
        return decideExpiry(settlementId, "approved");
    }

    @Test
    void createDraftIsIdempotentBySameKey() {
        // 1.5 幂等：同 idempotencyKey 重复提交返回同一草稿、不重复建。
        ContractRecord first = newDraft();
        ContractRecord second = newDraft();
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(repo.listContracts()).extracting(ContractRecord::id).containsExactly(first.id());
    }

    @Test
    void createDraftDistinctKeysCreateDistinctContracts() {
        ContractRecord a = newDraft();
        ContractRecord b = service.createDraft(new ContractDraftRequest("k_draft_2", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("baseRentCent", 100000),
                "fixed_ratio", "none", null, Map.of("breach", "2x"),
                List.of(Map.of("item", "deposit_offset")), List.of(), null, "餐饮"));
        assertThat(b.id()).isNotEqualTo(a.id());
        assertThat(repo.listContracts()).hasSize(2);
    }

    /**
     * HP-C01-R3（BSM L1349「主线与各条平行线都通过才生效」；ENUM L255「新建未审批不生效」）：复核单仍待审时合同保持待确认；
     * 只有与合同挂着的复核单号一致的 approved 终态才把合同推到待生效（生效仍走 activate），并留痕 contract.review_approved。
     */
    @Test
    void reviewApprovedMovesContractToPendingEffectiveOnlyWhenEventMatchesStoredReview() {
        ContractRecord c = newDraft();
        assertThat(c.lifecycleStatus()).isEqualTo("drafting");
        ContractRecord submitted = submit(service, c, "k_submit");
        assertThat(submitted.lifecycleStatus()).isEqualTo("pending_confirmation");
        assertThat(submitted.reviewId()).isEqualTo("review_1");
        // 别的复核单号 → 忽略，合同仍待确认、不留痕。
        service.onReviewDecided(decision("review_other", "approved", "contract.new", "contract", c.id()));
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("pending_confirmation");
        verify(audit, never()).recordSuccess(eq("contract.review_approved"), any(), any(), any(), any(), any(), any());
        // 挂着的那张复核单整单通过 → 待生效。
        assertThat(approveContract(c.id()).lifecycleStatus()).isEqualTo("pending_effective");
        verify(audit).recordSuccess(eq("contract.review_approved"), eq("governance.supervisor_review.approve"),
                eq("contract"), eq(c.id()), isNull(), eq("review_1"), any());
    }

    /** BSM L1349「任一节点驳回则整单退回起草」：整单驳回 → 退回起草（留痕 contract.review_rejected），可重新提交并开新复核单。 */
    @Test
    void reviewRejectedReturnsContractToDrafting() {
        ContractRecord c = newDraft();
        submit(service, c, "k_submit");
        assertThat(decideContract(service, c.id(), "rejected").lifecycleStatus()).isEqualTo("drafting");
        verify(audit).recordSuccess(eq("contract.review_rejected"), eq("governance.supervisor_review.approve"),
                eq("contract"), eq(c.id()), isNull(), eq("review_1"), any());
        ContractRecord resubmitted = submit(service, c, "k_submit_2");
        assertThat(resubmitted.lifecycleStatus()).isEqualTo("pending_confirmation");
        assertThat(resubmitted.reviewId()).isEqualTo("review_2");
    }

    /** 陈旧/错配事件一律忽略：复核单号不同、对象已非待审（合同已待生效 / 变更已终态）、别的域的事件。 */
    @Test
    void staleOrMismatchedReviewEventIsIgnored() {
        ContractRecord c = newDraft();
        submit(service, c, "k_submit");
        assertThat(approveContract(c.id()).lifecycleStatus()).isEqualTo("pending_effective");
        // 合同已出待确认：同号再来一个驳回终态 → 忽略。
        service.onReviewDecided(decision("review_1", "rejected", "contract.new", "contract", c.id()));
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("pending_effective");
        // 变更：复核单号不同 → 忽略；已终态的变更再收到事件 → 忽略，条款不重复应用。
        ContractRecord e = effectiveContractRecord("ctr_stale", 500000L, Map.of());
        ContractChangeRecord raised = service.raiseChange(e.id(), change("k_ch", "rent_change", Map.of("baseRentCent", 120000)));
        service.onReviewDecided(decision("review_other", "approved", "contract.change", "contract_change", raised.id()));
        assertThat(repo.findChangeById(raised.id()).orElseThrow().approvalStatus()).isEqualTo("pending");
        assertThat(repo.findContractById(e.id()).orElseThrow().rentTerms()).containsEntry("baseRentCent", 100000);
        assertThat(decideChange(raised.id(), "rejected").approvalStatus()).isEqualTo("rejected");
        service.onReviewDecided(decision(raised.reviewId(), "approved", "contract.change", "contract_change", raised.id()));
        assertThat(repo.findChangeById(raised.id()).orElseThrow().approvalStatus()).isEqualTo("rejected");
        assertThat(repo.findContractById(e.id()).orElseThrow().rentTerms()).containsEntry("baseRentCent", 100000);
        // 别的域 / 仍待审的事件：不碰合同域对象。
        service.onReviewDecided(decision("review_x", "approved", "bar.payroll_rule", "bar_payroll_rule", c.id()));
        service.onReviewDecided(decision("review_1", "pending_review", "contract.new", "contract", c.id()));
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("pending_effective");
    }

    /**
     * CG-01（PGM「招商运营细则」：「合同审批全部通过、生效后转为已签约」）：只有 activate 成功才经端口通知招商域置线索已签约；
     * 起草、提交、审批驳回都不通知。
     */
    @Test
    void activateNotifiesEffectiveLeadPortWhileDraftSubmitAndRejectDoNot() {
        List<String> notified = new ArrayList<>();
        ContractService wired = new ContractService(repo, mock(AuditEventService.class), authz,
                (contractId, idempotencyKey) -> notified.add(contractId + "|" + idempotencyKey), reviews);
        ContractRecord c = wired.createDraft(new ContractDraftRequest("k_cg01", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("baseRentCent", 100000),
                "fixed_ratio", "none", null, Map.of("breach", "2x"),
                List.of(Map.of("item", "deposit_offset"), Map.of("item", "unpaid_fee")), List.of(), null, "餐饮"));
        submit(wired, c, "k_cg01_submit");
        decideContract(wired, c.id(), "rejected");
        assertThat(notified).isEmpty();   // 驳回退回起草：线索不得置已签约

        submit(wired, c, "k_cg01_submit2");
        decideContract(wired, c.id(), "approved");
        assertThat(notified).isEmpty();   // 审批全部通过但尚未生效：仍不通知

        wired.activate(c.id(), new ContractActivateRequest("k_cg01_act", List.of("sp_1")));
        assertThat(notified).containsExactly(c.id() + "|k_cg01_act");
    }

    @Test
    void activateFreezesSnapshotAndRequiresPendingEffective() {
        ContractRecord c = newDraft();
        // 未走审批不能生效。
        assertThatThrownBy(() -> service.activate(c.id(),
                new ContractActivateRequest("k_a", List.of("sp_1"))))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTRACT_NOT_PENDING_EFFECTIVE"));

        submit(service, c, "k_submit");
        approveContract(c.id());

        ContractRecord eff = service.activate(c.id(), new ContractActivateRequest("k_a2", List.of("sp_1")));
        assertThat(eff.lifecycleStatus()).isEqualTo("effective");
        assertThat(eff.snapshot()).isNotNull();
        assertThat(eff.snapshot()).containsKeys("rentTerms", "commissionMode", "energyFeeBasis", "penaltyTerms");
        assertThat(eff.snapshot().get("commissionMode")).isEqualTo("fixed_ratio");
    }

    @Test
    void settleDisputeResolveToPendingThenCompleteExpires() {
        ContractRecord c = effectiveContract();
        // 带争议 → 挂起（有争议），合同仍生效，到期终止最后审批 409；裁决＋齐单核对后终审通过才落账。
        ContractSettlementRecord disputed = service.settle(c.id(),
                settleReq("k_s", "expiry", "押金抵扣有异议"));
        assertThat(disputed.status()).isEqualTo("disputed");
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("effective");
        assertThatThrownBy(() -> approveExpiry(disputed.id()))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTRACT_SETTLEMENT_DISPUTED"));

        // 主管裁决 → 回到待结算（逐例项备齐），合同仍生效。
        ContractSettlementRecord resumed = service.resolveDispute(disputed.id(),
                new ContractResolveDisputeRequest("k_rd", "押金按约抵扣"));
        assertThat(resumed.status()).isEqualTo("pending_settlement");
        assertThat(resumed.settlementItems()).hasSize(2);
        // 裁决后重算走 TerminationSettlementMath：未清汇总 + 押金处置（deposit 500000、unpaid 0 → 净退 500000）。
        Map<String, Object> depResumed = resumed.settlementItems().stream()
                .filter(i -> "deposit_disposal".equals(i.get("item"))).findFirst().orElseThrow();
        assertThat(depResumed).containsEntry("netRefundCent", 300000L);
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("effective");

        // 齐单核对 + 终审通过 → 已结清 + 合同到期。
        checkDocuments(c.id(), "k_cs_dc");
        ContractSettlementRecord settled = approveExpiry(resumed.id());
        assertThat(settled.status()).isEqualTo("deposit_offsetting");
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("expired");
    }

    @Test
    void settleNoDisputeOpensPendingThenCompleteSettles() {
        ContractRecord c = effectiveContract();
        ContractSettlementRecord opened = service.settle(c.id(), settleReq("k_s2", "expiry", null));
        assertThat(opened.status()).isEqualTo("pending_settlement");
        assertThat(opened.settlementItems()).hasSize(2);
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("effective");
        checkDocuments(c.id(), "k_cs2_dc");
        ContractSettlementRecord settled = approveExpiry(opened.id());
        assertThat(settled.status()).isEqualTo("deposit_offsetting");
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("expired");
    }

    /** 变更按「变更」类模板开复核单（与新签复核单不混）；整单通过→应用变更+新版本快照，驳回→变更置 rejected、条款不动。 */
    @Test
    void contractChangeAppliesOnReviewApprovalAndRejectsOnRejection() {
        ContractRecord c = effectiveContract();
        ContractChangeRecord raised = service.raiseChange(c.id(),
                change("k_ch", "rent_change", Map.of("baseRentCent", 120000)));
        assertThat(raised.approvalStatus()).isEqualTo("pending");
        assertThat(raised.reviewId()).isNotNull().isNotEqualTo(repo.findContractById(c.id()).orElseThrow().reviewId());
        // 复核单仍待审：变更 pending，条款不动。
        assertThat(repo.findContractById(c.id()).orElseThrow().rentTerms()).containsEntry("baseRentCent", 100000);
        ContractChangeRecord applied = decideChange(raised.id(), "approved");
        assertThat(applied.approvalStatus()).isEqualTo("approved");
        assertThat(applied.newSnapshot()).isNotNull();
        assertThat(repo.findContractById(c.id()).orElseThrow().rentTerms()).containsEntry("baseRentCent", 120000);
        // 驳回：变更置 rejected，条款不动。
        ContractChangeRecord second = service.raiseChange(c.id(),
                change("k_ch2", "rent_change", Map.of("baseRentCent", 150000)));
        assertThat(decideChange(second.id(), "rejected").approvalStatus()).isEqualTo("rejected");
        assertThat(repo.findContractById(c.id()).orElseThrow().rentTerms()).containsEntry("baseRentCent", 120000);
    }

    @Test
    void activateSchedulesWholeTermRentBillsIdempotently() {
        // P1-A：生效即按整租期(2026-07..2027-06=12期)幂等排租金AR（ContractRentMath base=100000，无免租/递增）。
        ContractRecord c = effectiveContract();   // rentTerms baseRentCent=100000, 生效 2026-07-01 到期 2027-06-30
        List<ContractRentBillRecord> rent = repo.rentBills.stream()
                .filter(b -> b.contractId().equals(c.id()) && b.feeType().equals("rent")).toList();
        assertThat(rent).hasSize(12);
        assertThat(rent).allMatch(b -> b.amountCent() == 100000L);
        assertThat(rent).extracting(ContractRentBillRecord::period)
                .contains("2026-07", "2026-12", "2027-06");
        // 整租期已预排 → 手工再排任一期幂等冲突。
        assertThatThrownBy(() -> service.generateRentBill(c.id(), new ContractRentBillRequest("k_rb2", "2026-08")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTRACT_RENT_ALREADY_BILLED"));
    }

    @Test
    void draftStoresBusinessScopeAndAttachmentsMergeDedup() {
        ContractRecord c = newDraft();
        assertThat(c.rentTerms()).containsEntry("businessScope", "餐饮");   // 缺口3 经营范围入 rent_terms
        ContractRecord a1 = service.addAttachments(c.id(),
                new ContractAttachmentRequest("k_a1", List.of("scan_1", "scan_2")));
        assertThat(a1.attachmentIds()).containsExactly("scan_1", "scan_2");
        ContractRecord a2 = service.addAttachments(c.id(),
                new ContractAttachmentRequest("k_a2", List.of("scan_2", "scan_3")));   // 去重合并
        assertThat(a2.attachmentIds()).containsExactly("scan_1", "scan_2", "scan_3");
    }

    @Test
    void activateBooksRentPropertyForEachPeriodAndLeavesEnergyToEnergyDomain() {
        // 独表按实际费用代收，合同域只排租金及物业费。
        // 能源费统一由能耗抄表域 settleEnergyFee 出账,消除合同域×能耗域对同一户同期双挂账。
        ContractRecord d = service.createDraft(new ContractDraftRequest("k_pf", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"),
                Map.of("baseRentCent", 100000, "propertyFeeCent", 20000),
                "fixed_ratio", "by_contract", null, Map.of(), List.of(), List.of(), null, null));
        submit(service, d, "k_sub");
        approveContract(d.id());
        service.activate(d.id(), new ContractActivateRequest("k_act", List.of("sp_pf")));
        List<ContractRentBillRecord> all = repo.rentBills.stream()
                .filter(b -> b.contractId().equals(d.id())).toList();
        assertThat(all).hasSize(24);
        List<ContractRentBillRecord> aug = all.stream().filter(b -> b.period().equals("2026-08")).toList();
        assertThat(aug).extracting(ContractRentBillRecord::feeType)
                .containsExactlyInAnyOrder("rent", "property_fee");
        assertThat(aug).extracting(ContractRentBillRecord::feeType).doesNotContain("energy_fee");
        assertThat(service.generatePeriodBills(d.id(), new ContractRentBillRequest("k_pb", "2026-08"))).isEmpty();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"none","by_contract","by_area","by_usage"})
    void contractRejectsFixedEnergyFeeForEveryBasis(String basis) {
        assertThatThrownBy(() -> service.createDraft(new ContractDraftRequest("k_fixed_"+basis,"mch_1","subj_1",null,"subj_1",
                LocalDate.parse("2026-07-01"),LocalDate.parse("2027-06-30"),Map.of("energyFeeCent",5000),
                "fixed_ratio",basis,500000L,Map.of(),List.of(),List.of(),null,null))).isInstanceOf(ApiException.class);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"by_area","by_usage"})
    void contractRejectsPublicEnergyAllocationEvenWithoutFixedFee(String basis) {
        assertThatThrownBy(() -> service.createDraft(new ContractDraftRequest("k_allocation_"+basis,"mch_1","subj_1",null,"subj_1",
                LocalDate.parse("2026-07-01"),LocalDate.parse("2027-06-30"),Map.of(),
                "fixed_ratio",basis,500000L,Map.of(),List.of(),List.of(),null,null))).isInstanceOf(ApiException.class);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"energyFeeCent","energy_fee_cent"})
    void fixedEnergyChargeCannotBypassDraftOrChangeValidation(String key) {
        ContractRecord c = effectiveContract();
        for (Object value : List.of("5000", 0.5, "invalid")) {
            assertThatThrownBy(() -> service.createDraft(new ContractDraftRequest("k_fixed_type", "mch_1", "subj_1", null, "subj_1",
                    LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of(key, value),
                    "fixed_ratio", "by_contract", 500000L, Map.of(), List.of(), List.of(), null, null)))
                    .hasMessageContaining("不可预设固定能源费");
            assertThatThrownBy(() -> service.raiseChange(c.id(), change(
                    "k_fixed_change", "business_scope_change", Map.of(key, value))))
                    .hasMessageContaining("不可预设固定能源费");
        }
    }

    @Test
    void contractRejectsFixedEnergyFeeWhenBasisIsMetered() {
        // 清单 P1-3 固定额校验:按面积/按用量的合同不许再填固定能源费金额(下单时拦住,防脏数据)。
        assertThatThrownBy(() -> service.createDraft(new ContractDraftRequest("k_bad", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"),
                Map.of("baseRentCent", 100000, "energyFeeCent", 5000),
                "fixed_ratio", "by_usage", 500000L, Map.of(), List.of(), List.of(), null, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    void settlementItemsCarryRealAmountsOnOpenWithoutPosting() {
        ContractRecord c = effectiveContract();   // HP-C03：押金＝3 个月×月租 100000＝300000
        ContractSettlementRecord opened = service.settle(c.id(), settleReq("k_s3", "move_out", null));
        assertThat(opened.status()).isEqualTo("pending_settlement");
        assertThat(opened.reviewId()).isNull();
        // 开账即算真实金额（TerminationSettlementMath）：押金处置净退=500000（unpaid 0）；开账绝不落账。
        Map<String, Object> depOpen = opened.settlementItems().stream()
                .filter(i -> "deposit_disposal".equals(i.get("item"))).findFirst().orElseThrow();
        assertThat(depOpen).containsEntry("netRefundCent", 300000L);
        assertThat(terminationPort.depositCalls).isZero();
        // HP-C02-R3：「逐相推进」入口已删除（第二条手动落账旁路）；退租单的落账只由变更终审通过触发。
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("effective");
    }

    // ============ HP-C02 齐单核对（BSM L1352／ENUM L252「退租对应期间的全部相关单据收齐并核对完成后，才允许退租结算最后审批通过；缺单时保持待处理」、裁决 Q16A） ============
    /** 缺单：核对入口 409 CONTRACT_SETTLEMENT_DOCUMENTS_INCOMPLETE 列缺项明细；结算单保持待结算、不记核对人（「缺单时保持待处理」）。 */
    @Test
    void documentsCheckRejectsWhenPeriodBillMissing() {
        ContractRecord c = effectiveContract();
        ContractSettlementRecord opened = service.settle(c.id(), settleReq("k_dc1", "move_out", null));
        repo.missingDocuments = List.of("2026-08:rent", "2026-08:energy_fee");
        // HP-C02-R6：手动开的退租单尚无退租变更（退租日未定）→ 无从确定截止月，保持待处理。
        assertThatThrownBy(() -> service.checkSettlementDocuments(opened.id(),
                new ContractResolveDisputeRequest("k_dc1n", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("CONTRACT_SETTLEMENT_DOCUMENTS_INCOMPLETE");
                    assertThat(e.getMessage()).contains("退租日");
                });
        service.raiseChange(c.id(), change("k_dc1ch", "move_out", Map.of()));   // 沿用这张在办单
        assertThatThrownBy(() -> service.checkSettlementDocuments(opened.id(),
                new ContractResolveDisputeRequest("k_dc1c", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("CONTRACT_SETTLEMENT_DOCUMENTS_INCOMPLETE");
                    assertThat(e.getMessage()).contains("2026-08:rent", "2026-08:energy_fee");
                });
        ContractSettlementRecord still = repo.findSettlementById(opened.id()).orElseThrow();
        assertThat(still.status()).isEqualTo("pending_settlement");
        assertThat(still.documentsCheckedBy()).isNull();
        assertThat(still.documentsCheckedAt()).isNull();
    }

    /** 齐单：结算单记核对人（当前操作人）/时间/备注，状态不变；核对事实随结算单保留到结清。 */
    @Test
    void documentsCheckStampsCheckerWhenComplete() {
        ContractRecord c = effectiveContract();
        ContractSettlementRecord opened = service.settle(c.id(), settleReq("k_dc2", "expiry", null));
        ContractSettlementRecord checked = service.checkSettlementDocuments(opened.id(),
                new ContractResolveDisputeRequest("k_dc2c", "租金/物业/能源费账单已逐月对齐"));
        assertThat(checked.documentsCheckedBy()).isEqualTo("usr_admin");
        assertThat(checked.documentsCheckedAt()).isNotNull();
        assertThat(checked.documentsCheckNote()).isEqualTo("租金/物业/能源费账单已逐月对齐");
        assertThat(checked.status()).isEqualTo("pending_settlement");
        verify(audit).recordSuccess(eq("contract.settlement_documents_checked"), eq("contract.settlement.manage"),
                eq("contract_settlement"), eq(opened.id()), isNull(), eq("k_dc2c"), any());
        ContractSettlementRecord settled = approveExpiry(opened.id());
        assertThat(settled.status()).isEqualTo("deposit_offsetting");
        assertThat(settled.documentsCheckedBy()).isEqualTo("usr_admin");
    }

    /**
     * 退租/提前终止变更发起即开一张待结算单承载齐单核对事实与 ENUM L252 诸态（Q16A「不是齐单前连审批也不得发起」）：
     * 不落账（depositCalls=0）、合同仍生效；同合同已有开放结算单则不重复开。
     */
    @Test
    void raiseMoveOutOpensPendingSettlementWithoutPosting() {
        ContractRecord c = effectiveContractRecord("ctr_dc3", 500000L, Map.of());
        service.raiseChange(c.id(), change("k_dc3", "move_out", Map.of()));
        assertThat(terminationPort.depositCalls).isZero();
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("effective");
        List<ContractSettlementRecord> sts = repo.listSettlementsByContract(c.id());
        assertThat(sts).hasSize(1);
        assertThat(sts.get(0).status()).isEqualTo("pending_settlement");
        assertThat(sts.get(0).triggerType()).isEqualTo("move_out");
        assertThat(sts.get(0).documentsCheckedBy()).isNull();
        service.raiseChange(c.id(), change("k_dc3b", "early_termination", Map.of()));
        assertThat(repo.listSettlementsByContract(c.id())).hasSize(1);
    }

    // ============ HP-C02-R6 退租日（2026-10-18 裁决第 2 条「发起时必填退租日」：期间＝生效月→退租日所在月（合同到期走到期日所在月）；
    // 齐单核对、缺单判定、欠费口径都按这个截止月） ============
    /** 退租/提前终止变更发起时必填载荷退租日（ISO 日期）：缺失或不可解析 → 400 VALIDATION_FAILED，不开复核单、不留变更与结算单。 */
    @Test
    void moveOutAndEarlyTerminationRequireMoveOutDate() {
        ContractRecord c = effectiveContractRecord("ctr_r6a", 500000L, Map.of());
        int n = 0;
        for (String type : List.of("move_out", "early_termination")) {
            for (Map<String, Object> payload : List.<Map<String, Object>>of(Map.of(), Map.of("moveOutDate", "2026-13-01"),
                    Map.of("move_out_date", "8月15日"))) {
                String key = "k_r6a_" + (n++);
                assertThatThrownBy(() -> service.raiseChange(c.id(),
                        new ContractChangeRequest(key, type, payload, "usr_gm", THREE_LINES, null)))
                        .as("%s %s", type, payload)
                        .isInstanceOfSatisfying(ApiException.class, e -> {
                            assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
                            assertThat(e.getMessage()).contains("退租日");
                        });
            }
        }
        assertThat(evaluated).isEmpty();
        assertThat(repo.listChangesByContract(c.id())).isEmpty();
        assertThat(repo.listSettlementsByContract(c.id())).isEmpty();
    }

    /**
     * 截止月：退租＝退租日所在月——发起时预算欠费、齐单核对、最后审批闸与落账欠费都按 2026-08（web 蛇形键 move_out_date 归一为 moveOutDate）；
     * 合同到期结算＝到期日所在月（2027-06）。
     */
    @Test
    void terminationThroughMonthIsMoveOutMonthAndExpiryUsesExpiryMonth() {
        ContractRecord c = effectiveContractRecord("ctr_r6b", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(), change("k_r6b", "move_out", Map.of("move_out_date", "2026-08-15")));
        assertThat(ch.payload()).containsEntry("moveOutDate", "2026-08-15").doesNotContainKey("move_out_date");
        assertThat(repo.unpaidThrough).isNotEmpty().containsOnly(YearMonth.of(2026, 8));
        repo.unpaidThrough.clear();
        checkDocuments(c.id(), "k_r6b_dc");
        assertThat(repo.lastThrough).isEqualTo(YearMonth.of(2026, 8));
        repo.lastThrough = null;
        approveTerminationChain(ch.id());
        assertThat(repo.lastThrough).isEqualTo(YearMonth.of(2026, 8));
        assertThat(repo.unpaidThrough).isNotEmpty().containsOnly(YearMonth.of(2026, 8));
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("terminated");

        ContractRecord e = effectiveContractRecord("ctr_r6c", 500000L, Map.of());   // 到期日 2027-06-30
        ContractSettlementRecord st = service.settle(e.id(), settleReq("k_r6c", "expiry", null));
        service.checkSettlementDocuments(st.id(), new ContractResolveDisputeRequest("k_r6c_dc", null));
        assertThat(repo.lastThrough).isEqualTo(YearMonth.of(2027, 6));
    }

    private ContractRecord effectiveContract() {
        ContractRecord c = newDraft();
        submit(service, c, "k_submit");
        approveContract(c.id());
        return service.activate(c.id(), new ContractActivateRequest("k_a", List.of("sp_1")));
    }

    // ============ 合同经营范围（rent_terms.allowedCategories，2026-07-18） ============
    @Test
    void draftValidatesAndStoresAllowedCategories() {
        ContractRecord c = service.createDraft(new ContractDraftRequest("k_scope1", "mch_1", "subj_1", null, "subj_1",
                LocalDate.now().minusDays(1), LocalDate.now().plusYears(1), Map.of(),
                null, null, null, null, null, List.of(), null, "酒吧经营", null, List.of("beer", "snack")));
        assertThat(c.rentTerms()).containsEntry("allowedCategories", List.of("beer", "snack"));
        // 词表外取值（发明新枚举）→ 拒。
        assertThatThrownBy(() -> service.createDraft(new ContractDraftRequest("k_scope2", "mch_1", "subj_1", null,
                "subj_1", LocalDate.now().minusDays(1), LocalDate.now().plusYears(1), Map.of(),
                null, null, null, null, null, List.of(), null, null, null, List.of("metaverse"))))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    void draftRejectsVocabularyBypassEmbeddedInRentTerms() {
        // I-1: 直接把词表外 allowedCategories 塞进 rent_terms 整 Map（绕过专用字段）→ 同样拒；
        // 合法内嵌值（含客户端蛇形键）则归一化入库。
        assertThatThrownBy(() -> service.createDraft(new ContractDraftRequest("k_embed_bad", "mch_1", "subj_1",
                null, "subj_1", LocalDate.now().minusDays(1), LocalDate.now().plusYears(1),
                Map.of("allowedCategories", List.of("任意串")),
                null, null, null, null, null, List.of(), null, null, null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        ContractRecord ok = service.createDraft(new ContractDraftRequest("k_embed_ok", "mch_1", "subj_1",
                null, "subj_1", LocalDate.now().minusDays(1), LocalDate.now().plusYears(1),
                Map.of("allowed_categories", List.of("beer")),
                null, null, null, null, null, List.of(), null, null, null, null));
        assertThat(ok.rentTerms()).containsEntry("allowedCategories", List.of("beer"));
        assertThat(ok.rentTerms()).doesNotContainKey("allowed_categories");
    }

    @Test
    void businessScopeChangeUpdatesAllowedCategoriesAndGuardEnforcesAfterApproval() {
        // 变更后生效：走既有 business_scope_change 变更链，终审通过 payload 并入 rent_terms，拦截即读新范围。
        ContractRecord c = service.createDraft(new ContractDraftRequest("k_scope3", "mch_scope", "subj_1", null,
                "subj_1", LocalDate.now().minusDays(1), LocalDate.now().plusYears(1),
                Map.of("baseRentCent", 100000), "fixed_ratio", "none", null, null, null, List.of(), null,
                "酒吧经营", null, List.of("beer")));
        submit(service, c, "k_scope3_submit");
        approveContract(c.id());
        service.activate(c.id(), new ContractActivateRequest("k_scope3_act", List.of("sp_scope")));

        ContractScopeGuard guard = new ContractScopeGuard(repo);
        // 变更前：只许 beer，卖 spirit 拦。
        assertThatThrownBy(() -> guard.requireBarSaleAllowed("mch_scope", List.of("spirit")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTRACT_SCOPE_VIOLATION"));
        // 发起经营范围变更（web 客户端把嵌套键蛇形化为 allowed_categories → 后端归一化）：改为 spirit+beer。
        ContractChangeRecord change = service.raiseChange(c.id(), change(
                "k_scope3_ch", "business_scope_change", Map.of("allowed_categories", List.of("spirit", "beer"))));
        // 复核单未终态 → 旧范围仍生效。
        assertThatThrownBy(() -> guard.requireBarSaleAllowed("mch_scope", List.of("spirit")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTRACT_SCOPE_VIOLATION"));
        decideChange(change.id(), "approved");
        // 整单通过 → 新范围立即生效。
        assertThatCode(() -> guard.requireBarSaleAllowed("mch_scope", List.of("spirit")))
                .doesNotThrowAnyException();
        // 变更链里发明新枚举同样拒。
        assertThatThrownBy(() -> service.raiseChange(c.id(), change(
                "k_scope3_bad", "business_scope_change", Map.of("allowed_categories", List.of("metaverse")))))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
    }

    // ============ 退租结算落财务台账（move_out/early_termination 终审通过自动结算） ============
    private static final class RecordingTerminationPort
            implements com.complex.platform.finance.TerminationSettlementPort {
        Long depositRefundCent;
        Long penaltyCent;
        int depositCalls;
        int penaltyCalls;
        Long depositBalance;   // null=不封顶(返回固化额)；非 null=按此台账余额封顶
        LocalDate depositDueDate;   // HP-C06：最近一次押金退还应付到期日

        @Override
        public long createDepositRefundPayable(String merchantId, long netRefundCent, String settlementId, LocalDate dueDate) {
            depositDueDate = dueDate;
            long eff = depositBalance == null ? netRefundCent : Math.min(netRefundCent, Math.max(0L, depositBalance));
            if (depositBalance != null) {
                depositBalance -= eff;   // 模拟 deposit_ledger refund 行扣减余额
            }
            depositRefundCent = eff;
            depositCalls++;
            return eff;
        }

        @Override
        public String createTerminationPenaltyReceivable(String merchantId, long penaltyCent, String settlementId) {
            this.penaltyCent = penaltyCent;
            penaltyCalls++;
            ledger.unpaidPenaltyBySettlement.put(settlementId, penaltyCent);
            return "arap_pen_" + settlementId;
        }

        /** 第四轮裁决第 3 条：押金抵扣销账逐次记录 {结算单 id, 候选台账项 id 顺序, 抵扣上限}；按夹具台账未收额逐笔销账并扣押金余额（模拟 deduct 行）。 */
        final List<Object[]> offsetCalls = new ArrayList<>();
        InMemoryContractRepository ledger;

        @Override
        public List<DepositOffset> offsetReceivablesWithDeposit(String merchantId, String settlementId, List<String> arApItemIds,
                long offsetCent) {
            offsetCalls.add(new Object[]{settlementId, List.copyOf(arApItemIds), offsetCent});
            long remaining = depositBalance == null ? offsetCent : Math.min(offsetCent, depositBalance);
            List<DepositOffset> offsets = new ArrayList<>();
            for (String id : arApItemIds) {
                long apply = Math.min(remaining, ledger.unpaidOf(id));
                if (apply > 0) {
                    ledger.setUnpaid(id, ledger.unpaidOf(id) - apply);
                    offsets.add(new DepositOffset(id, apply));
                    remaining -= apply;
                }
            }
            if (depositBalance != null) {
                depositBalance -= offsets.stream().mapToLong(DepositOffset::amountCent).sum();
            }
            return offsets;
        }

        @Override
        public long effectiveRefundableDeposit(String merchantId, long contractDepositCent) {
            return depositBalance == null ? contractDepositCent : Math.min(contractDepositCent, depositBalance);
        }

        /** HP-C03：押金补退差额登记逐笔记录 {merchantId, childContractId, changeId, deltaCent, deadline}，回台账项 id/单号。 */
        final List<Object[]> adjustments = new ArrayList<>();

        @Override
        public com.complex.platform.finance.ArApItemRecord registerDepositAdjustment(String merchantId, String childContractId,
                String changeId, long deltaCent, LocalDate deadline) {
            adjustments.add(new Object[]{merchantId, childContractId, changeId, deltaCent, deadline});
            long amount = Math.abs(deltaCent);
            return new com.complex.platform.finance.ArApItemRecord("arap_adj_" + changeId, "ARAP-ADJ-" + adjustments.size(),
                    deltaCent > 0 ? "receivable" : "payable", merchantId, "deposit_adjustment", childContractId, amount, 0L,
                    amount, "CNY", deadline, null, deltaCent > 0 ? "pending_collection" : "pending_payment",
                    Instant.now(), Instant.now(), merchantId, null);
        }
    }

    private ContractRecord effectiveContractRecord(String id, long depositCent, Map<String, Object> penaltyTerms) {
        Instant now = Instant.now();
        return repo.saveContract(new ContractRecord(id, "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("baseRentCent", 100000),
                "fixed_ratio", "none", depositCent, penaltyTerms, List.of(), Map.of(), null, List.of(),
                "effective", "proj_1", null, null, null, now, now));
    }

    private void approveTerminationChain(String changeId) {
        decideChange(changeId, "approved");
    }

    /** HP-C02：对合同当前待结算的那张退租结算单做齐单核对（夹具缺单清单默认为空＝齐全），返回记了核对人的单。 */
    private ContractSettlementRecord checkDocuments(String contractId, String key) {
        ContractSettlementRecord live = repo.listSettlementsByContract(contractId).stream()
                .filter(s -> "pending_settlement".equals(s.status())).findFirst().orElseThrow();
        return service.checkSettlementDocuments(live.id(), new ContractResolveDisputeRequest(key, null));
    }

    /** 镜像 ContractService.deterministicSuffix：SHA-256 前 48 hex。 */
    private static String sha48(String key) {
        try {
            byte[] d = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(d).substring(0, 48);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void moveOutTerminationAutoSettlesPostsDepositRefundAndTerminates() {
        ContractRecord c = effectiveContractRecord("ctr_t1", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(),
                change("k_t1", "move_out", Map.of()));
        checkDocuments(c.id(), "k_t1_dc");
        approveTerminationChain(ch.id());
        assertThat(terminationPort.depositCalls).isEqualTo(1);
        assertThat(terminationPort.depositRefundCent).isEqualTo(500000L);
        assertThat(terminationPort.penaltyCalls).isZero();
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("terminated");
        List<ContractSettlementRecord> sts = repo.listSettlementsByContract(c.id());
        assertThat(sts).hasSize(1);
        assertThat(sts.get(0).status()).isEqualTo("deposit_offsetting");
        assertThat(sts.get(0).triggerType()).isEqualTo("move_out");
    }

    @Test
    void earlyTerminationUsesContractPenaltyTermsWhenPresent() {
        ContractRecord c = effectiveContractRecord("ctr_t2", 0L, Map.of("amountCent", 200000));
        ContractChangeRecord ch = service.raiseChange(c.id(),
                change("k_t2", "early_termination", Map.of("penaltyAmountCent", 999999)));
        checkDocuments(c.id(), "k_t2_dc");
        approveTerminationChain(ch.id());
        assertThat(terminationPort.penaltyCalls).isEqualTo(1);
        assertThat(terminationPort.penaltyCent).isEqualTo(200000L);
    }

    @Test
    void earlyTerminationFallsBackToManualPenaltyFromPayload() {
        ContractRecord c = effectiveContractRecord("ctr_t3", 0L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(),
                change("k_t3", "early_termination",
                        Map.of("penaltyAmountCent", 150000, "penaltyReason", "提前退租违约")));
        checkDocuments(c.id(), "k_t3_dc");
        approveTerminationChain(ch.id());
        assertThat(terminationPort.penaltyCalls).isEqualTo(1);
        assertThat(terminationPort.penaltyCent).isEqualTo(150000L);
        ContractSettlementRecord st = repo.listSettlementsByContract(c.id()).get(0);
        Map<String, Object> pen = st.settlementItems().stream()
                .filter(i -> "termination_penalty".equals(i.get("item"))).findFirst().orElseThrow();
        assertThat(pen).containsEntry("reason", "提前退租违约").containsEntry("arApItemId", "arap_pen_" + st.id());
    }

    @Test
    void earlyTerminationParsesStringPenaltyAmount() {
        // S3：人工录入金额被序列化成字符串时不静默变 0。
        ContractRecord c = effectiveContractRecord("ctr_t3s", 0L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(),
                change("k_t3s", "early_termination", Map.of("penaltyAmountCent", "120000")));
        checkDocuments(c.id(), "k_t3s_dc");
        approveTerminationChain(ch.id());
        assertThat(terminationPort.penaltyCalls).isEqualTo(1);
        assertThat(terminationPort.penaltyCent).isEqualTo(120000L);
    }

    @Test
    void unpaidReceivableOffsetsDepositNetPositivePayable() {
        repo.unpaidAr = 300000L;
        ContractRecord c = effectiveContractRecord("ctr_t4", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(),
                change("k_t4", "move_out", Map.of()));
        checkDocuments(c.id(), "k_t4_dc");
        approveTerminationChain(ch.id());
        assertThat(terminationPort.depositRefundCent).isEqualTo(200000L);
    }

    @Test
    void unpaidExceedsDepositPostsNoPayableMerchantStillOwes() {
        repo.unpaidAr = 800000L;
        ContractRecord c = effectiveContractRecord("ctr_t5", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(),
                change("k_t5", "move_out", Map.of()));
        checkDocuments(c.id(), "k_t5_dc");
        approveTerminationChain(ch.id());
        assertThat(terminationPort.depositCalls).isZero();
        ContractSettlementRecord st = repo.listSettlementsByContract(c.id()).get(0);
        Map<String, Object> dep = st.settlementItems().stream()
                .filter(i -> "deposit_disposal".equals(i.get("item"))).findFirst().orElseThrow();
        assertThat(dep).containsEntry("merchantStillOwesCent", 300000L).containsEntry("status", "listed_only");
    }

    @Test
    void depositRefundCappedByLedgerBalance() {
        // I3：台账实收余额 300000 < 固化 500000 → 只退 300000（封顶）。
        terminationPort.depositBalance = 300000L;
        ContractRecord c = effectiveContractRecord("ctr_t9", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(),
                change("k_t9", "move_out", Map.of()));
        checkDocuments(c.id(), "k_t9_dc");
        approveTerminationChain(ch.id());
        assertThat(terminationPort.depositRefundCent).isEqualTo(300000L);
        ContractSettlementRecord st = repo.listSettlementsByContract(c.id()).get(0);
        Map<String, Object> dep = st.settlementItems().stream()
                .filter(i -> "deposit_disposal".equals(i.get("item"))).findFirst().orElseThrow();
        assertThat(dep).containsEntry("depositCent", 300000L);
    }

    @Test
    void twoContractsSameMerchantCapByLedgerBalanceNoOverRefund() {
        // #3② 多合同同商户押金超退根治：退押金扣减台账余额，第二个合同按已减余额封顶。
        terminationPort.depositBalance = 4000L;   // 商户押金台账实收余额
        ContractRecord c1 = effectiveContractRecord("ctr_mc1", 2000L, Map.of());
        ContractRecord c2 = effectiveContractRecord("ctr_mc2", 3000L, Map.of());
        ContractChangeRecord ch1 = service.raiseChange(c1.id(),
                change("k_mc1", "move_out", Map.of()));
        ContractChangeRecord ch2 = service.raiseChange(c2.id(),
                change("k_mc2", "move_out", Map.of()));
        checkDocuments(c1.id(), "k_mc1_dc");
        approveTerminationChain(ch1.id());
        long refund1 = terminationPort.depositRefundCent;
        checkDocuments(c2.id(), "k_mc2_dc");
        approveTerminationChain(ch2.id());
        long refund2 = terminationPort.depositRefundCent;
        assertThat(refund1).isEqualTo(2000L);              // 固化 2000 ≤ 余额 4000
        assertThat(refund2).isEqualTo(2000L);              // 固化 3000 但余额只剩 2000 → 封顶 2000
        assertThat(refund1 + refund2).isEqualTo(4000L);    // 共退=实交，不超退
        assertThat(terminationPort.depositBalance).isZero();
    }

    /**
     * HP-C01-R3：变更复核单终态由 onReviewDecided 驱动——整单通过即 applyChange + settleTerminationIfNeeded；
     * 同一终态事件重放（变更已 approved）被忽略，退租结算只落一次、押金只退一次。
     */
    @Test
    void changeReviewApprovedAppliesChangeAndTerminationSettlesOnce() {
        ContractRecord c = effectiveContractRecord("ctr_t8r", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(), change("k_t8r", "move_out", Map.of()));
        assertThat(terminationPort.depositCalls).isZero();
        checkDocuments(c.id(), "k_t8r_dc");
        assertThat(decideChange(ch.id(), "approved").approvalStatus()).isEqualTo("approved");
        assertThat(terminationPort.depositCalls).isEqualTo(1);
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("terminated");
        // 同一 approved 事件重放：变更已终态 → 忽略，不再结算。
        service.onReviewDecided(decision(ch.reviewId(), "approved", "contract.change", "contract_change", ch.id()));
        assertThat(terminationPort.depositCalls).isEqualTo(1);
        assertThat(repo.listSettlementsByContract(c.id())).hasSize(1);
        verify(audit).recordSuccess(eq("contract.change_approved"), eq("governance.supervisor_review.approve"),
                eq("contract_change"), eq(ch.id()), isNull(), eq(ch.reviewId()), any());
    }

    @Test
    void twoTerminationChangesSettleOnlyOnceNoDoubleDepositRefund() {
        // I2：同合同两个退租变更并存，A 先审终止合同，B 审批完成但结算被守卫跳过 → 押金只退一次。
        ContractRecord c = effectiveContractRecord("ctr_t8", 500000L, Map.of());
        ContractChangeRecord a = service.raiseChange(c.id(),
                change("k_t8a", "move_out", Map.of()));
        ContractChangeRecord b = service.raiseChange(c.id(),
                change("k_t8b", "early_termination", Map.of("penaltyAmountCent", 100000)));
        checkDocuments(c.id(), "k_t8_dc");   // B 未另开单，沿用 A 发起时开的那张
        approveTerminationChain(a.id());
        approveTerminationChain(b.id());
        assertThat(terminationPort.depositCalls).isEqualTo(1);
        assertThat(terminationPort.penaltyCalls).isZero();
        assertThat(repo.listSettlementsByContract(c.id())).hasSize(1);
    }

    @Test
    void terminationSettlementIsIdempotentAgainstReplay() {
        ContractRecord c = effectiveContractRecord("ctr_t6", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(),
                change("k_t6", "move_out", Map.of()));
        String settlementId = "cst_" + sha48(ch.id());
        repo.saveSettlement(new ContractSettlementRecord(settlementId, c.id(), "move_out", "settled",
                List.of(), null, null, null, "proj_1", Instant.now(), Instant.now()));
        approveTerminationChain(ch.id());
        assertThat(terminationPort.depositCalls).isZero();
        assertThat(terminationPort.penaltyCalls).isZero();
    }

    @Test
    void rejectedTerminationChangeDoesNotSettle() {
        ContractRecord c = effectiveContractRecord("ctr_t7", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(),
                change("k_t7", "early_termination", Map.of("penaltyAmountCent", 100000)));
        decideChange(ch.id(), "rejected");
        assertThat(terminationPort.depositCalls).isZero();
        assertThat(terminationPort.penaltyCalls).isZero();
        // HP-C02：发起时开的待结算单随驳回作废，不留开放/已结清单。
        assertThat(repo.listSettlementsByContract(c.id())).extracting(ContractSettlementRecord::status)
                .containsExactly("voided");
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("effective");
    }

    /**
     * 退租/提前终止驳回作废发起时开的那张单——商户异议挂起中（有争议／有争议，诉讼/仲裁）同样作废（合同继续履行，不留悬挂单；
     * BSM L1352「同一份合同的退租结算只算一次、不重复出账」），下次发起退租另开新单。
     */
    @Test
    void rejectedTerminationVoidsMerchantDisputedSettlement() {
        for (String held : List.of("disputed", "disputed_litigation")) {
            ContractRecord c = effectiveContractRecord("ctr_f3_" + held, 500000L, Map.of());
            ContractChangeRecord ch = service.raiseChange(c.id(), change("k_f3_" + held, "move_out", Map.of()));
            String sid = "cst_" + sha48(ch.id());
            ContractSettlementRecord st = repo.findSettlementById(sid).orElseThrow();
            repo.saveSettlement(st.withDispute(held, st.settlementItems(), "商户：违约金多算", null, null, null, Instant.now()));
            decideChange(ch.id(), "rejected");
            assertThat(repo.findSettlementById(sid).orElseThrow().status()).as(held).isEqualTo("voided");
            assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("effective");
            ContractChangeRecord again = service.raiseChange(c.id(), change("k_f3b_" + held, "move_out", Map.of()));
            assertThat(repo.findSettlementById("cst_" + sha48(again.id()))).as(held).isPresent();
        }
    }

    // ============ HP-C02-R2 缺单阻断退租变更最后审批（BSM L1352「退租对应期间的全部相关单据收齐并核对完成后，才允许退租结算最后审批通过；缺单时保持待处理」「有争议进「有争议」状态挂起、由主管裁决后继续」、Q16A） ============
    /**
     * 未齐单核对：变更复核单「全部通过」终态回写被 409 CONTRACT_SETTLEMENT_DOCUMENTS_INCOMPLETE 挡住——
     * 变更仍 pending、不落账、合同仍生效、结算单保持待结算；核对后又出现缺项同样挡住并列明细。
     */
    @Test
    void terminationFinalApprovalBlockedWhenDocumentsUnchecked() {
        ContractRecord c = effectiveContractRecord("ctr_r2a", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(), change("k_r2a", "move_out", Map.of()));
        assertThatThrownBy(() -> approveTerminationChain(ch.id()))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTRACT_SETTLEMENT_DOCUMENTS_INCOMPLETE"));
        assertThat(repo.findChangeById(ch.id()).orElseThrow().approvalStatus()).isEqualTo("pending");
        assertThat(terminationPort.depositCalls).isZero();
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("effective");
        assertThat(repo.listSettlementsByContract(c.id())).extracting(ContractSettlementRecord::status)
                .containsExactly("pending_settlement");
        checkDocuments(c.id(), "k_r2a_dc");
        repo.missingDocuments = List.of("2026-09:rent");
        assertThatThrownBy(() -> approveTerminationChain(ch.id()))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("CONTRACT_SETTLEMENT_DOCUMENTS_INCOMPLETE");
                    assertThat(e.getMessage()).contains("2026-09:rent");
                });
        assertThat(repo.findChangeById(ch.id()).orElseThrow().approvalStatus()).isEqualTo("pending");
        assertThat(terminationPort.depositCalls).isZero();
    }

    /** 非末阶段（主线／平行线中间节点通过，复核单仍待审）不校验齐单：事件被忽略，变更仍 pending、不落账。 */
    @Test
    void terminationEarlierStagesApproveWithoutDocumentsCheck() {
        ContractRecord c = effectiveContractRecord("ctr_r2b", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(), change("k_r2b", "move_out", Map.of()));
        service.onReviewDecided(decision(ch.reviewId(), "pending_review", "contract.change", "contract_change", ch.id()));
        assertThat(repo.findChangeById(ch.id()).orElseThrow().approvalStatus()).isEqualTo("pending");
        assertThat(terminationPort.depositCalls).isZero();
        assertThat(repo.listSettlementsByContract(c.id()).get(0).documentsCheckedAt()).isNull();
    }

    /**
     * 核对后最后审批通过 → 落账一次、沿用合同在办的那张结算单（更早手动开账的单同样被沿用：settled、带核对人），
     * 不新建第二张；终审事件重放不二次落账。
     */
    @Test
    void terminationFinalApprovalPostsOnceAfterDocumentsChecked() {
        ContractRecord c = effectiveContractRecord("ctr_r2c", 500000L, Map.of());
        ContractSettlementRecord manual = service.settle(c.id(), settleReq("k_r2c_m", "move_out", null));
        ContractChangeRecord ch = service.raiseChange(c.id(), change("k_r2c", "move_out", Map.of()));
        assertThat(repo.listSettlementsByContract(c.id())).hasSize(1);
        assertThat(checkDocuments(c.id(), "k_r2c_dc").id()).isEqualTo(manual.id());
        approveTerminationChain(ch.id());
        assertThat(terminationPort.depositCalls).isEqualTo(1);
        assertThat(terminationPort.depositRefundCent).isEqualTo(500000L);
        assertThat(repo.findChangeById(ch.id()).orElseThrow().approvalStatus()).isEqualTo("approved");
        List<ContractSettlementRecord> sts = repo.listSettlementsByContract(c.id());
        assertThat(sts).hasSize(1);
        assertThat(sts.get(0).id()).isEqualTo(manual.id());
        assertThat(sts.get(0).status()).isEqualTo("deposit_offsetting");
        assertThat(sts.get(0).documentsCheckedBy()).isEqualTo("usr_admin");
        assertThat(sts.get(0).settlementItems()).anySatisfy(i -> assertThat(i).containsEntry("item", "deposit_disposal"));
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("terminated");
        service.onReviewDecided(decision(ch.reviewId(), "approved", "contract.change", "contract_change", ch.id()));   // 重放
        assertThat(terminationPort.depositCalls).isEqualTo(1);
    }

    /** 有争议（settle 带 disputeReason）→ 最后审批 409 CONTRACT_SETTLEMENT_DISPUTED 挂起；主管裁决＋齐单核对后才落账，且只算一次。 */
    @Test
    void disputedSettlementKeepsFinalApprovalPending() {
        ContractRecord c = effectiveContractRecord("ctr_r2d", 500000L, Map.of());
        ContractSettlementRecord disputed = service.settle(c.id(),
                settleReq("k_r2d_s", "move_out", "商户对结算金额有异议"));
        assertThat(disputed.status()).isEqualTo("disputed");
        ContractChangeRecord ch = service.raiseChange(c.id(), change("k_r2d", "move_out", Map.of()));
        assertThat(repo.listSettlementsByContract(c.id())).hasSize(1);   // 争议挂起单仍是在办单，不另开
        assertThatThrownBy(() -> approveTerminationChain(ch.id()))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTRACT_SETTLEMENT_DISPUTED"));
        assertThat(repo.findChangeById(ch.id()).orElseThrow().approvalStatus()).isEqualTo("pending");
        assertThat(terminationPort.depositCalls).isZero();
        service.resolveDispute(disputed.id(), new ContractResolveDisputeRequest("k_r2d_r", "主管裁决：按清单结算"));
        assertThatThrownBy(() -> approveTerminationChain(ch.id()))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTRACT_SETTLEMENT_DOCUMENTS_INCOMPLETE"));
        checkDocuments(c.id(), "k_r2d_dc");
        approveTerminationChain(ch.id());
        assertThat(terminationPort.depositCalls).isEqualTo(1);
        assertThat(repo.findChangeById(ch.id()).orElseThrow().approvalStatus()).isEqualTo("approved");
        List<ContractSettlementRecord> sts = repo.listSettlementsByContract(c.id());
        assertThat(sts).hasSize(1);
        assertThat(sts.get(0).id()).isEqualTo(disputed.id());
        assertThat(sts.get(0).status()).isEqualTo("deposit_offsetting");
        assertThat(sts.get(0).resolvedBy()).isEqualTo("usr_admin");
    }

    // ============ HP-C04 商户退租异议（BSM L1352「有争议进「有争议」状态挂起、由主管裁决后继续 ，如主管裁决不接受，则进「有争议，诉讼/仲裁」状态无限期挂起，
    // 直至诉讼或仲裁有结果后再行处理」；ENUM L252「商户对退租结算金额的异议在退租审批流程中提出并解决」） ============
    /**
     * 2026-10-18 裁决第 5 条「商户异议→结算单进「有争议」挂起、最后审批不得通过；主管裁决（写结论留痕）后回待结算继续审批；商户不接受裁决→
     * 「有争议，诉讼/仲裁」无限期挂起，有结果后由主管录结果继续……押金退还因算账未完成自然顺延」、第 6 条「商户在商户专区点」：
     * 发起退租即开的结算单在商户「我的对账」可见 → 商户异议 → disputed（清单保留）→ 终审 409 → 主管裁决（结论必填、落库）→ 待结算 →
     * 商户不接受裁决 → disputed_litigation → 终审 409 → 主管录诉讼/仲裁结果 → 待结算 → 终审通过，押金退还应付只挂一次。
     */
    @Test
    void merchantDisputeAndLitigationHoldFinalApprovalUntilSupervisorRecordsOutcome() {
        when(authz.currentMerchantScope()).thenReturn(Optional.of("mch_1"));
        com.complex.platform.merchantportal.MerchantPortalService portal = new com.complex.platform.merchantportal.MerchantPortalService(
                repo, authz, audit, new com.complex.platform.common.idempotency.IdempotencyService(
                        new com.complex.platform.support.InMemoryIdempotencyRepository(),
                        new com.fasterxml.jackson.databind.ObjectMapper()));
        ContractRecord c = effectiveContractRecord("ctr_c04", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(), change("k_c04", "move_out", Map.of()));
        String sid = "cst_" + sha48(ch.id());
        assertThat(portal.listMySettlements(1, 500).items()).extracting(ContractSettlementRecord::id).containsExactly(sid);
        checkDocuments(c.id(), "k_c04_dc");
        int itemCount = repo.findSettlementById(sid).orElseThrow().settlementItems().size();

        ContractSettlementRecord disputed = portal.submitSettlementDispute(sid,
                new com.complex.platform.merchantportal.dto.MerchantSettlementDisputeRequest("k_c04_d", null, "违约金多算一期"));
        assertThat(disputed.status()).isEqualTo("disputed");
        assertThat(disputed.settlementItems()).hasSize(itemCount);
        assertFinalApprovalBlocked(ch);

        assertThatThrownBy(() -> service.resolveDispute(sid, new ContractResolveDisputeRequest("k_c04_r0", "  ")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        ContractSettlementRecord ruled = service.resolveDispute(sid,
                new ContractResolveDisputeRequest("k_c04_r", "按清单结算，违约金不调整"));
        assertThat(ruled.status()).isEqualTo("pending_settlement");
        assertThat(ruled.disputeResolution()).isEqualTo("按清单结算，违约金不调整");
        assertThat(ruled.resolvedBy()).isEqualTo("usr_admin");
        assertThat(ruled.resolvedAt()).isNotNull();

        ContractSettlementRecord litigation = portal.rejectSettlementResolution(sid,
                new com.complex.platform.merchantportal.dto.MerchantSettlementDisputeRequest("k_c04_x", null, "不接受，已申请仲裁"));
        assertThat(litigation.status()).isEqualTo("disputed_litigation");
        assertThat(litigation.disputeReason()).contains("违约金多算一期").contains("不接受，已申请仲裁");
        assertFinalApprovalBlocked(ch);

        ContractSettlementRecord outcome = service.resolveDispute(sid,
                new ContractResolveDisputeRequest("k_c04_o", "仲裁裁决维持原清单"));
        assertThat(outcome.status()).isEqualTo("pending_settlement");
        assertThat(outcome.disputeResolution()).contains("按清单结算，违约金不调整").contains("仲裁裁决维持原清单");

        approveTerminationChain(ch.id());
        assertThat(terminationPort.depositCalls).isEqualTo(1);
        assertThat(repo.findSettlementById(sid).orElseThrow().status()).isEqualTo("deposit_offsetting");
        service.onReviewDecided(decision(ch.reviewId(), "approved", "contract.change", "contract_change", ch.id()));
        assertThat(terminationPort.depositCalls).isEqualTo(1);
    }

    /** 有争议／诉讼仲裁挂起期间：变更终审 409 CONTRACT_SETTLEMENT_DISPUTED，变更保持待审、押金不落账。 */
    private void assertFinalApprovalBlocked(ContractChangeRecord ch) {
        assertThatThrownBy(() -> approveTerminationChain(ch.id()))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("CONTRACT_SETTLEMENT_DISPUTED");
                    assertThat(e.getMessage()).contains("有争议");
                });
        assertThat(repo.findChangeById(ch.id()).orElseThrow().approvalStatus()).isEqualTo("pending");
        assertThat(terminationPort.depositCalls).isZero();
    }

    // ============ 建设U：手动结算入口也落账 + 退押金确认闸 + 跨路径去重 + 悬挂单作废 ============

    @Test
    void manualSettleDoesNotPostUntilConfirmed() {
        ContractRecord c = effectiveContractRecord("ctr_u1", 500000L, Map.of());
        ContractSettlementRecord opened = service.settle(c.id(), settleReq("k_u1", "expiry", null));
        assertThat(opened.status()).isEqualTo("pending_settlement");
        assertThat(terminationPort.depositCalls).isZero();
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("effective");
        checkDocuments(c.id(), "k_u1_dc");
        assertThat(terminationPort.depositCalls).isZero();
        ContractSettlementRecord settled = approveExpiry(opened.id());
        assertThat(settled.status()).isEqualTo("deposit_offsetting");
        assertThat(terminationPort.depositCalls).isEqualTo(1);
        assertThat(terminationPort.depositRefundCent).isEqualTo(500000L);
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("expired");
    }

    @Test
    void manualExpirySettleConfirmPostsDepositRefundAndExpires() {
        ContractRecord c = effectiveContractRecord("ctr_u2", 500000L, Map.of());
        ContractSettlementRecord opened = service.settle(c.id(), settleReq("k_u2", "expiry", null));
        assertThat(opened.triggerType()).isEqualTo("expiry");
        assertThat(terminationPort.depositCalls).isZero();
        checkDocuments(c.id(), "k_u2_dc");
        ContractSettlementRecord settled = approveExpiry(opened.id());
        assertThat(settled.status()).isEqualTo("deposit_offsetting");
        assertThat(terminationPort.depositCalls).isEqualTo(1);
        assertThat(terminationPort.depositRefundCent).isEqualTo(500000L);
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("expired");
    }

    /** 提前终止手动开账：清单即带合同条款违约金；落账只能由提前终止变更终审通过触发（沿用这张在办单）。 */
    @Test
    void manualEarlyTerminationPostsContractPenaltyOnlyViaTerminationApproval() {
        ContractRecord c = effectiveContractRecord("ctr_u3", 500000L, Map.of("amountCent", 200000));
        ContractSettlementRecord opened = service.settle(c.id(),
                settleReq("k_u3", "early_termination", null));
        Map<String, Object> pen = opened.settlementItems().stream()
                .filter(i -> "termination_penalty".equals(i.get("item"))).findFirst().orElseThrow();
        assertThat(pen).containsEntry("amountCent", 200000L).containsEntry("reason", "合同条款违约金");
        assertThat(terminationPort.penaltyCalls).isZero();
        ContractChangeRecord ch = service.raiseChange(c.id(), change("k_u3_ch", "early_termination", Map.of()));
        assertThat(repo.listSettlementsByContract(c.id())).hasSize(1);   // 沿用手动开的那张在办单，不另开
        checkDocuments(c.id(), "k_u3_dc");
        approveTerminationChain(ch.id());
        assertThat(terminationPort.penaltyCalls).isEqualTo(1);
        assertThat(terminationPort.penaltyCent).isEqualTo(200000L);
        assertThat(terminationPort.depositCalls).isEqualTo(1);
        assertThat(repo.findSettlementById(opened.id()).orElseThrow().status()).isEqualTo("deposit_offsetting");
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("terminated");
    }

    @Test
    void manualUnpaidExceedsDepositConfirmPostsNoPayable() {
        repo.unpaidAr = 800000L;
        ContractRecord c = effectiveContractRecord("ctr_u4", 500000L, Map.of());
        ContractSettlementRecord opened = service.settle(c.id(), settleReq("k_u4", "expiry", null));
        checkDocuments(c.id(), "k_u4_dc");
        approveExpiry(opened.id());
        assertThat(terminationPort.depositCalls).isZero();
        ContractSettlementRecord st = repo.findSettlementById(opened.id()).orElseThrow();
        Map<String, Object> dep = st.settlementItems().stream()
                .filter(i -> "deposit_disposal".equals(i.get("item"))).findFirst().orElseThrow();
        assertThat(dep).containsEntry("merchantStillOwesCent", 300000L).containsEntry("status", "listed_only");
    }

    @Test
    void expiryApprovalReplayDoesNotPostTwice() {
        ContractRecord c = effectiveContractRecord("ctr_u5", 500000L, Map.of());
        ContractSettlementRecord opened = service.settle(c.id(), settleReq("k_u5", "expiry", null));
        checkDocuments(c.id(), "k_u5_dc");
        approveExpiry(opened.id());
        assertThat(terminationPort.depositCalls).isEqualTo(1);
        assertThat(approveExpiry(opened.id()).status()).isEqualTo("deposit_offsetting");   // 终审事件重放：已落账单不再落账
        assertThat(terminationPort.depositCalls).isEqualTo(1);
    }

    @Test
    void secondManualSettlementVoidedAfterFirstAlreadySettled() {
        ContractRecord c = effectiveContractRecord("ctr_u7", 500000L, Map.of());
        ContractSettlementRecord s1 = service.settle(c.id(), settleReq("k_u7a", "expiry", null));
        ContractSettlementRecord s2 = service.settle(c.id(), settleReq("k_u7b", "expiry", null));
        service.checkSettlementDocuments(s1.id(), new ContractResolveDisputeRequest("k_u7a_dc", null));
        approveExpiry(s1.id());
        assertThat(terminationPort.depositCalls).isEqualTo(1);
        // HP-C02-R3：作废不是落账——s2 未核对也直接作废、不落账。
        ContractSettlementRecord v = approveExpiry(s2.id());
        assertThat(v.status()).isEqualTo("voided");
        assertThat(terminationPort.depositCalls).isEqualTo(1);
    }

    @Test
    void autoTerminationStillPostsUnchangedAfterSharedLedgerExtraction() {
        ContractRecord c = effectiveContractRecord("ctr_u8", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(),
                change("k_u8", "move_out", Map.of()));
        checkDocuments(c.id(), "k_u8_dc");
        approveTerminationChain(ch.id());
        assertThat(terminationPort.depositCalls).isEqualTo(1);
        assertThat(terminationPort.depositRefundCent).isEqualTo(500000L);
        ContractSettlementRecord st = repo.listSettlementsByContract(c.id()).get(0);
        assertThat(st.status()).isEqualTo("deposit_offsetting");
        Map<String, Object> dep = st.settlementItems().stream()
                .filter(i -> "deposit_disposal".equals(i.get("item"))).findFirst().orElseThrow();
        assertThat(dep).containsEntry("actualRefundCent", 500000L);
    }

    @Test
    void expiredContractRejectsMoveOutSettleButAllowsExpiry() {
        // I-3：到期合同点结算若误选 move_out 应被拒（否则确认时误作废、押金静默不退）；到期结算放行。
        Instant now = Instant.now();
        ContractRecord c = repo.saveContract(new ContractRecord("ctr_u9", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("baseRentCent", 100000),
                "fixed_ratio", "none", 500000L, Map.of(), List.of(), Map.of(), null, List.of(),
                "expired", "proj_1", null, null, null, now, now));
        assertThatThrownBy(() -> service.settle(c.id(), settleReq("k_u9", "move_out", null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTRACT_NOT_SETTLEABLE"));
        ContractSettlementRecord ok = service.settle(c.id(), settleReq("k_u9e", "expiry", null));
        assertThat(ok.status()).isEqualTo("pending_settlement");
    }

    @Test
    void confirmRecomputesUnpaidSoNewArrearsAfterOpenAreOffset() {
        // I-4：开账后新增欠费，确认时按最新 unpaid 现算（押金优先抵欠费），不用开账快照——与自动路径同口径。
        ContractRecord c = effectiveContractRecord("ctr_u10", 500000L, Map.of());
        ContractSettlementRecord opened = service.settle(c.id(), settleReq("k_u10", "expiry", null));
        checkDocuments(c.id(), "k_u10_dc");
        Map<String, Object> depOpen = opened.settlementItems().stream()
                .filter(i -> "deposit_disposal".equals(i.get("item"))).findFirst().orElseThrow();
        assertThat(depOpen).containsEntry("netRefundCent", 500000L);   // 开账 unpaid=0
        repo.unpaidAr = 300000L;   // 开账→确认间新增欠费
        approveExpiry(opened.id());
        assertThat(terminationPort.depositRefundCent).isEqualTo(200000L);   // 500000 抵 300000 → 净退 200000（非快照 500000）
    }

    @Test
    void confirmLocksContractRowForUpdate() {
        // S-2：确认落账走 findContractByIdForUpdate（合同行加锁），串行化并发确认防违约金双挂。
        ContractRecord c = effectiveContractRecord("ctr_u11", 500000L, Map.of());
        ContractSettlementRecord opened = service.settle(c.id(), settleReq("k_u11", "expiry", null));
        checkDocuments(c.id(), "k_u11_dc");
        int before = repo.forUpdateCalls;
        approveExpiry(opened.id());
        assertThat(repo.forUpdateCalls).isGreaterThan(before);
    }

    // ============ HP-C02 到期结算走「到期终止」类复核单（BSM L1352「退租、提前终止、合同到期等各种终止结算，都要经过多级审批」「审批全部通过后，系统按算好的结算清单自动落账，不再单设手动确认入口」、ENUM L255「到期不得无审批自动终止」、台账 §21） ============
    /**
     * ENUM L255「合同审批链按新建、到期终止、变更三类分别配置流程模板」：到期结算开账即按「到期终止」类模板开复核单（actionType=contract.expiry_termination、
     * objectType=contract_settlement、主线最终审批人＋三线）并挂在结算单上；开账与非本单复核单的事件都不落账；终审通过 → 落账一次、已结清、合同到期。
     * 退租开账不开复核单（退租的审批挂在退租变更上）。
     */
    @Test
    void expirySettleOpensExpiryTerminationReviewAndFinalApprovalPosts() {
        ContractRecord c = effectiveContractRecord("ctr_r3x", 500000L, Map.of());
        ContractSettlementRecord opened = service.settle(c.id(), settleReq("k_r3x", "expiry", null));
        assertThat(opened.reviewId()).isNotNull();
        SupervisorReviewEvaluateRequest request = evaluated.get(evaluated.size() - 1);
        assertThat(request.actionType()).isEqualTo("contract.expiry_termination");
        assertThat(request.objectType()).isEqualTo("contract_settlement");
        assertThat(request.objectId()).isEqualTo(opened.id());
        assertThat(request.finalApproverId()).isEqualTo("usr_gm");
        assertThat(request.parallelLines()).isEqualTo(THREE_LINES);
        assertThat(opened.reviewId()).isEqualTo("review_" + evaluated.size());
        checkDocuments(c.id(), "k_r3x_dc");
        service.onReviewDecided(decision("review_other", "approved", "contract.expiry_termination", "contract_settlement",
                opened.id()));
        assertThat(terminationPort.depositCalls).isZero();
        assertThat(approveExpiry(opened.id()).status()).isEqualTo("deposit_offsetting");
        assertThat(terminationPort.depositCalls).isEqualTo(1);
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("expired");
        int reviewsBefore = evaluated.size();
        ContractRecord m = effectiveContractRecord("ctr_r3x_m", 500000L, Map.of());
        assertThat(service.settle(m.id(), settleReq("k_r3x_m", "move_out", null)).reviewId()).isNull();
        assertThat(evaluated).hasSize(reviewsBefore);
    }

    /** BSM L1349「任一节点驳回则整单退回起草」：到期终止复核单驳回 → 结算单作废、不落账、合同状态不动（重新开账即重新发起）。 */
    @Test
    void expiryReviewRejectedVoidsSettlementWithoutPosting() {
        ContractRecord c = effectiveContractRecord("ctr_r3y", 500000L, Map.of());
        ContractSettlementRecord opened = service.settle(c.id(), settleReq("k_r3y", "expiry", null));
        checkDocuments(c.id(), "k_r3y_dc");
        assertThat(decideExpiry(opened.id(), "rejected").status()).isEqualTo("voided");
        assertThat(terminationPort.depositCalls).isZero();
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("effective");
    }

    /** 到期终止最后审批：未齐单核对 → 409 CONTRACT_SETTLEMENT_DOCUMENTS_INCOMPLETE 不落账；核对后又出现缺项同样挡住并列明细。 */
    @Test
    void expiryFinalApprovalBlockedWithoutDocumentsCheck() {
        ContractRecord c = effectiveContractRecord("ctr_r3b", 500000L, Map.of());
        ContractSettlementRecord opened = service.settle(c.id(), settleReq("k_r3b", "expiry", null));
        assertThatThrownBy(() -> approveExpiry(opened.id()))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTRACT_SETTLEMENT_DOCUMENTS_INCOMPLETE"));
        assertThat(terminationPort.depositCalls).isZero();
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("effective");
        checkDocuments(c.id(), "k_r3b_dc");
        repo.missingDocuments = List.of("2026-09:rent");
        assertThatThrownBy(() -> approveExpiry(opened.id()))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("CONTRACT_SETTLEMENT_DOCUMENTS_INCOMPLETE");
                    assertThat(e.getMessage()).contains("2026-09:rent");
                });
        assertThat(terminationPort.depositCalls).isZero();
        assertThat(repo.findSettlementById(opened.id()).orElseThrow().status()).isEqualTo("pending_settlement");
    }

    /** 到期结算齐单核对后终审通过 → 落账一次、单已结清（核对事实随单保留）、合同到期。 */
    @Test
    void expiryFinalApprovalPostsAfterDocumentsCheck() {
        ContractRecord c = effectiveContractRecord("ctr_r3c", 500000L, Map.of());
        ContractSettlementRecord opened = service.settle(c.id(), settleReq("k_r3c", "expiry", null));
        checkDocuments(c.id(), "k_r3c_dc");
        ContractSettlementRecord settled = approveExpiry(opened.id());
        assertThat(settled.status()).isEqualTo("deposit_offsetting");
        assertThat(settled.documentsCheckedBy()).isEqualTo("usr_admin");
        assertThat(terminationPort.depositCalls).isEqualTo(1);
        assertThat(terminationPort.depositRefundCent).isEqualTo(500000L);
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("expired");
    }

    // ============ #30 超范围下单经营申请（scope_grant，按「变更」类模板走统一审批） ============
    private ContractRecord effectiveScopedContract(String key, String merchant, List<String> allowed, String space) {
        ContractRecord c = service.createDraft(new ContractDraftRequest(key, merchant, "subj_1", null, "subj_1",
                LocalDate.now().minusDays(1), LocalDate.now().plusYears(1),
                Map.of("baseRentCent", 100000), "fixed_ratio", "none", null, null, null, List.of(), null,
                "酒吧经营", null, allowed));
        submit(service, c, key + "_submit");
        approveContract(c.id());
        return service.activate(c.id(), new ContractActivateRequest(key + "_act", List.of(space)));
    }

    @Test
    void scopeRequestPermanentGrantPassesGuardAfterApproval() {
        effectiveScopedContract("k_sr1", "mch_sr1", List.of("beer"), "sp_sr1");
        ContractScopeGuard guard = new ContractScopeGuard(repo);
        assertThatThrownBy(() -> guard.requireBarSaleAllowed("mch_sr1", List.of("spirit")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTRACT_SCOPE_VIOLATION"));
        String cid = repo.listContractsByMerchant("mch_sr1").get(0).id();
        assertThatThrownBy(() -> service.requestScopeGrant(cid,
                scopeRequest("k_sr1_bad", "metaverse", "扩营", "permanent", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        ContractChangeRecord req = service.requestScopeGrant(cid,
                scopeRequest("k_sr1", "spirit", "客人要烈酒，申请扩营", "permanent", null));
        assertThat(req.changeType()).isEqualTo("scope_grant");
        assertThatThrownBy(() -> guard.requireBarSaleAllowed("mch_sr1", List.of("spirit")))
                .isInstanceOf(ApiException.class);   // 复核单未终态：仍拦
        decideChange(req.id(), "approved");
        assertThatCode(() -> guard.requireBarSaleAllowed("mch_sr1", List.of("spirit"))).doesNotThrowAnyException();
        assertThat(castList(repo.findContractById(cid).orElseThrow().rentTerms().get("allowedCategories")))
                .contains("spirit");
    }

    @Test
    void scopeRequestTemporaryGrantHonoredAndRejectedStaysBlocked() {
        effectiveScopedContract("k_sr2", "mch_sr2", List.of("beer"), "sp_sr2");
        ContractScopeGuard guard = new ContractScopeGuard(repo);
        String cid = repo.listContractsByMerchant("mch_sr2").get(0).id();
        assertThatThrownBy(() -> service.requestScopeGrant(cid,
                scopeRequest("k_sr2_novu", "spirit", "临时活动", "temporary", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        ContractChangeRecord req = service.requestScopeGrant(cid, scopeRequest(
                "k_sr2", "spirit", "周末临时活动", "temporary", LocalDate.now().plusDays(30)));
        approveFullChain(req.id());
        assertThatCode(() -> guard.requireBarSaleAllowed("mch_sr2", List.of("spirit"))).doesNotThrowAnyException();
        ContractChangeRecord req2 = service.requestScopeGrant(cid, scopeRequest(
                "k_sr2b", "cocktail", "再申请鸡尾酒", "permanent", null));
        decideChange(req2.id(), "rejected");
        assertThatThrownBy(() -> guard.requireBarSaleAllowed("mch_sr2", List.of("cocktail")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTRACT_SCOPE_VIOLATION"));
    }

    // ============ #31 变更子合同（经营位置/面积变更：继承 + 押金同步 + 原合同 amended） ============
    @Test
    void areaChangeOpensChildSubContractInheritsAndCollectsDepositDiff() {
        ContractRecord parent = effectiveContract();   // HP-C03：押金 3×100000＝300000
        ContractChangeRecord ch = service.raiseChange(parent.id(), change(
                "k_am1", "area_change", Map.of("areaSqm", 120, "baseRentCent", 150000, "depositDeadline", "2026-09-30")));
        approveFullChain(ch.id());
        assertThat(repo.findContractById(parent.id()).orElseThrow().lifecycleStatus()).isEqualTo("amended");
        List<ContractRecord> children = service.listChildContracts(parent.id());
        assertThat(children).hasSize(1);
        ContractRecord child = children.get(0);
        assertThat(child.lifecycleStatus()).isEqualTo("effective");
        assertThat(child.merchantId()).isEqualTo(parent.merchantId());
        assertThat(child.contractSubjectId()).isEqualTo(parent.contractSubjectId());
        assertThat(child.depositCent()).isEqualTo(450000L);   // 3 × 150000
        assertThat(child.rentTerms()).containsEntry("parentContractId", parent.id())
                .containsEntry("amendmentType", "area_change").containsEntry("areaSqm", 120);
        Map<String, Object> adj = castMap(child.rentTerms().get("depositAdjustment"));
        assertThat(adj).containsEntry("direction", "collect").containsEntry("targetDepositCent", 450000L)
                .containsEntry("deltaCent", 150000L).containsEntry("deadline", "2026-09-30");
        assertThat(repo.listContractSpaceIds(child.id())).containsExactly("sp_1");
    }

    @Test
    void locationChangeShiftsSpacesAndRecordsDepositRefundDiff() {
        ContractRecord parent = effectiveContract();
        ContractChangeRecord ch = service.raiseChange(parent.id(), change(
                "k_am2", "location_change", Map.of("spaceIds", List.of("sp_2"), "baseRentCent", 80000, "depositDeadline", "2026-09-30")));
        approveFullChain(ch.id());
        ContractRecord child = service.listChildContracts(parent.id()).get(0);
        assertThat(repo.listContractSpaceIds(child.id())).containsExactly("sp_2");
        Map<String, Object> adj = castMap(child.rentTerms().get("depositAdjustment"));
        assertThat(adj).containsEntry("direction", "refund").containsEntry("targetDepositCent", 240000L)
                .containsEntry("deltaCent", -60000L);
    }

    /** 变更复核单整单通过（主线 + 各平行线全部通过），任意变更/申请通用。 */
    private void approveFullChain(String changeId) {
        decideChange(changeId, "approved");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object o) {
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    private static List<String> castList(Object o) {
        return (List<String>) o;
    }

    @Test
    void childTerminationOffsetsParentUnpaidArAgainstDeposit() {
        // P2-1: 子合同退租结算把变更前挂在父合同的欠租纳入押金抵扣安全网。
        ContractRecord parent = effectiveContract();   // HP-C03：押金 3×100000＝300000
        ContractChangeRecord am = service.raiseChange(parent.id(), change(
                "k_p21", "area_change", Map.of("baseRentCent", 200000, "depositDeadline", "2026-09-30")));
        approveFullChain(am.id());
        ContractRecord child = service.listChildContracts(parent.id()).get(0);
        repo.unpaidArById.put(parent.id(), 300000L);   // 变更前父合同欠租
        ContractChangeRecord mo = service.raiseChange(child.id(), change(
                "k_p21_mo", "move_out", Map.of()));
        checkDocuments(child.id(), "k_p21_dc");
        approveFullChain(mo.id());
        // 子合同押金 3×200000＝600000 抵父欠租 300000 → 净退 300000（未修则错退 600000）。
        assertThat(terminationPort.depositRefundCent).isEqualTo(300000L);
    }

    @Test
    void childRentBillDedupCrossesParentChain() {
        // P2-1: 父合同已出账月份，子合同 period-bills 被链去重，不双开。
        ContractRecord parent = effectiveContract();   // 生效即排整租期租金账单(含 2026-08)
        ContractChangeRecord am = service.raiseChange(parent.id(), change(
                "k_p21b", "area_change", Map.of("baseRentCent", 100000, "depositDeadline", "2026-09-30")));
        approveFullChain(am.id());
        ContractRecord child = service.listChildContracts(parent.id()).get(0);
        List<ContractRentBillRecord> bills = service.generatePeriodBills(child.id(),
                new ContractRentBillRequest("k_p21b_bill", "2026-08"));
        assertThat(bills).isEmpty();
    }

    /**
     * HP-C01-R1（V494）：BSM L1349「各模板的阶段与岗位可以不同」「业务审核、财务审核、法务审核作为平行的相关部门线」——
     * 模板配置校验：合同类型只认新建／到期终止／变更三类；平行线码须在 approval_lines 且 active；默认最终审批人须存在且 active。
     * 合法配置落为该综合体该类型的生效模板，缺 contract_type 的查询返回三类（项目覆盖优先、否则平台默认）。
     */
    @Test
    void saveTemplateRejectsUnknownTypeInactiveLineOrUnknownDefaultApprover() {
        assertThatThrownBy(() -> service.saveApprovalFlow(new ContractApprovalFlowRequest(
                "k_tpl_type", null, "renewal", "续租模板", List.of("business_review"), null, null, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThatThrownBy(() -> service.saveApprovalFlow(new ContractApprovalFlowRequest(
                "k_tpl_line", null, "change", "变更模板", List.of("business_review", "tax_review"), null, null, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThatThrownBy(() -> service.saveApprovalFlow(new ContractApprovalFlowRequest(
                "k_tpl_user", null, "change", "变更模板", List.of("business_review"), "usr_ghost", null, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThat(repo.findActiveTemplate(null, "change").orElseThrow().requiredLineCodes())
                .containsExactly("business_review", "finance_review", "legal_review");

        ContractApprovalTemplateRecord saved = service.saveApprovalFlow(new ContractApprovalFlowRequest(
                "k_tpl_ok", "proj_1", "change", "proj_1 变更模板", List.of("business_review", "legal_review"), "usr_admin", null, null));
        assertThat(saved).extracting(ContractApprovalTemplateRecord::projectId, ContractApprovalTemplateRecord::contractType,
                ContractApprovalTemplateRecord::requiredLineCodes, ContractApprovalTemplateRecord::defaultFinalApproverId)
                .containsExactly("proj_1", "change", List.of("business_review", "legal_review"), "usr_admin");
        assertThat(service.listApprovalFlow("proj_1", "change")).extracting(ContractApprovalTemplateRecord::name)
                .containsExactly("proj_1 变更模板");
        assertThat(service.listApprovalFlow("proj_2", "change")).extracting(ContractApprovalTemplateRecord::projectId)
                .containsExactly((String) null);
        assertThat(service.listApprovalFlow("proj_1", null)).extracting(ContractApprovalTemplateRecord::contractType)
                .containsExactlyInAnyOrder("new", "expiry_termination", "change");
    }

    /**
     * HP-C01-R5（BSM L1349「阶段数与审批岗位按综合体后台可配（业务参数，按综合体可配，须同时配置上下限）」；2026-10-18 裁决第 1 条
     * 「主线层级数配上下限」）：最少/最多主线层级只填一项、最少&lt;1、最少&gt;最多 → 400 VALIDATION_FAILED、模板不变；1～3 落库；都不填＝未配置。
     */
    @Test
    void saveTemplateMainLevelBoundsMustBePairedAndOrdered() {
        Integer[][] invalid = {{2, null}, {null, 3}, {0, 2}, {3, 2}};
        for (Integer[] b : invalid) {
            assertThatThrownBy(() -> service.saveApprovalFlow(new ContractApprovalFlowRequest("k_lv_bad", null, "new", "平台默认·新建",
                    List.of("business_review", "finance_review", "legal_review"), null, b[0], b[1])))
                    .as("min=%s max=%s", b[0], b[1])
                    .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        }
        assertThat(repo.findActiveTemplate(null, "new").orElseThrow())
                .extracting(ContractApprovalTemplateRecord::minMainLevels, ContractApprovalTemplateRecord::maxMainLevels)
                .containsExactly(null, null);
        ContractApprovalTemplateRecord saved = service.saveApprovalFlow(new ContractApprovalFlowRequest("k_lv_ok", "proj_1", "new",
                "proj_1 新建", List.of("business_review", "finance_review", "legal_review"), null, 1, 3));
        assertThat(saved).extracting(ContractApprovalTemplateRecord::minMainLevels, ContractApprovalTemplateRecord::maxMainLevels)
                .containsExactly(1, 3);
    }

    /**
     * 第三轮控制席约定「审批模板保存审计补 project_id、上下限、默认最终审批人」：contract.approval_flow_saved 元数据带 project_id
     * （平台默认模板记 default）、min_main_levels、max_main_levels、default_final_approver_id（未填记空串）。
     */
    @Test
    void approvalFlowSavedAuditCarriesProjectBoundsAndDefaultApprover() {
        service.saveApprovalFlow(new ContractApprovalFlowRequest("k_tpl_au1", "proj_1", "new", "proj_1 新建",
                List.of("business_review"), "usr_admin", 1, 3));
        service.saveApprovalFlow(new ContractApprovalFlowRequest("k_tpl_au2", null, "change", "平台默认·变更",
                List.of("business_review"), null, null, null));
        verify(audit).recordSuccess(eq("contract.approval_flow_saved"), eq("contract.approval.flow.manage"),
                eq("contract_approval_flow"), any(), isNull(), eq("k_tpl_au1"), eq(Map.of("project_id", "proj_1",
                        "contract_type", "new", "line_count", 1, "min_main_levels", 1, "max_main_levels", 3,
                        "default_final_approver_id", "usr_admin")));
        verify(audit).recordSuccess(eq("contract.approval_flow_saved"), eq("contract.approval.flow.manage"),
                eq("contract_approval_flow"), any(), isNull(), eq("k_tpl_au2"), eq(Map.of("project_id", "default",
                        "contract_type", "change", "line_count", 1, "min_main_levels", "", "max_main_levels", "",
                        "default_final_approver_id", "")));
    }

    // ============ HP-C03 押金（BSM L1347 押金收取月数与签约押金；L1353 变更子合同押金补退；2026-10-18 裁决第 3 条） ============
    private static SpaceRecord space(String id, String areaSqm) {
        return new SpaceRecord(id, "proj_1", null, null, id, "shop", "1", new java.math.BigDecimal(areaSqm), List.of(), "active",
                Instant.now(), Instant.now());
    }

    private ContractService perSqmService(PlatformRepository platform) {
        return new ContractService(repo, audit, authz, null, platform, null, null, terminationPort, null, reviews);
    }

    /**
     * BSM L1347「商户押金的收取月数全平台统一（业务参数，全平台统一可配，出厂默认三个月，须同时配置上下限；下限不得低于三个月，
     * 配置校验拒绝更低的值，不允许零押金或负押金）」：出厂 3 个月、上下限未配置；缺上下限/缺值、下限&lt;3、零/负、值越界、下限&gt;上限
     * → 400 VALIDATION_FAILED 且参数不变；合法配置落库；读写均须平台范围＋contract.deposit_months.manage。
     */
    @Test
    void depositMonthsSettingRejectsMissingBoundsFloorBelowThreeAndOutOfRange() {
        assertThat(service.getDepositMonthsSetting()).isEqualTo(new DepositMonthsSettingRecord(3, null, null));
        Integer[][] invalid = {{4, null, 6}, {4, 3, null}, {null, 3, 6}, {3, 2, 6}, {0, 0, 6}, {-1, -1, 6}, {7, 3, 6}, {2, 3, 6}, {4, 5, 4}};
        for (Integer[] v : invalid) {
            assertThatThrownBy(() -> service.saveDepositMonthsSetting(new DepositMonthsSettingRequest("k_dm_bad", v[0], v[1], v[2], "调整")))
                    .as("value=%s min=%s max=%s", v[0], v[1], v[2])
                    .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        }
        assertThat(repo.depositMonths).isEqualTo(new DepositMonthsSettingRecord(3, null, null));
        DepositMonthsSettingRecord saved = service.saveDepositMonthsSetting(new DepositMonthsSettingRequest("k_dm_ok", 4, 3, 6, "调整"));
        assertThat(saved).isEqualTo(new DepositMonthsSettingRecord(4, 3, 6));
        assertThat(repo.depositMonths).isEqualTo(saved);
        verify(authz, org.mockito.Mockito.atLeastOnce()).requirePlatformScope();
        verify(authz, org.mockito.Mockito.atLeast(2)).requirePermission("contract.deposit_months.manage");
    }

    /**
     * BSM L1347「押金金额 ＝ 收取月数 × 该商户签约时的月租金（只乘月租，不含物业费、水电能源费等其他费用）」：生效固化时押金＝当前配置月数×
     * 固化月租，月数写入生效快照；起草不接受与公式不一致的手工押金（含物业费算进去的 360000）；per_sqm 用绑定铺位面积固化后的月租。
     */
    @Test
    void activationDepositIsMonthsTimesSignedRentAndMonthsFrozenInSnapshot() {
        Map<String, Object> terms = Map.of("baseRentCent", 100000, "propertyFeeCent", 20000);
        assertThatThrownBy(() -> service.createDraft(new ContractDraftRequest("k_dep_manual", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), terms, "fixed_ratio", "none", 360000L,
                Map.of(), List.of(), List.of(), null, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CONTRACT_DEPOSIT_FORMULA_MISMATCH"));
        ContractRecord ok = service.createDraft(new ContractDraftRequest("k_dep_ok", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), terms, "fixed_ratio", "none", 300000L,
                Map.of(), List.of(), List.of(), null, null));
        submit(service, ok, "k_dep_ok_sub");
        approveContract(ok.id());
        ContractRecord eff = service.activate(ok.id(), new ContractActivateRequest("k_dep_ok_act", List.of("sp_dep")));
        assertThat(eff.depositCent()).isEqualTo(300000L);
        assertThat(eff.snapshot()).containsEntry("depositMonths", 3).containsEntry("depositCent", 300000L);

        PlatformRepository platform = mock(PlatformRepository.class);
        when(platform.findSpaceById("sp_sqm")).thenReturn(Optional.of(space("sp_sqm", "120.00")));
        ContractService sqm = perSqmService(platform);
        ContractRecord d = sqm.createDraft(new ContractDraftRequest("k_dep_sqm", "mch_sqm", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("rentMode", "per_sqm", "perSqmRentCent", 1000),
                "fixed_ratio", "none", null, Map.of(), List.of(), List.of(), null, null));
        submit(sqm, d, "k_dep_sqm_sub");
        decideContract(sqm, d.id(), "approved");
        ContractRecord sqmEff = sqm.activate(d.id(), new ContractActivateRequest("k_dep_sqm_act", List.of("sp_sqm")));
        assertThat(sqmEff.depositCent()).isEqualTo(360000L);   // 120㎡ × 10 元 = 1200 元/月 × 3
        assertThat(sqmEff.snapshot()).containsEntry("depositMonths", 3);
    }

    /**
     * BSM L1353「面积或位置变更……的目标押金＝原合同押金月数×变更……后的月租；子合同写明目标金额、补退差额及办理期限；批准生效后登记应收或应退……
     * 原合同押金月数按原合同快照沿用，不改用当前配置月数」：原合同生效时 5 个月（快照）、其后配置改回 3 个月——面积变更月租 100000→150000，
     * 目标＝5×150000＝750000、差额＝750000−500000＝+250000 → 登记应收（补缴）一笔、到期日＝办理期限；子合同押金＝目标、快照沿用 5 个月。
     */
    @Test
    void areaChangeTargetDepositUsesSnapshotMonthsAndRegistersReceivableDueOnDeadline() {
        repo.depositMonths = new DepositMonthsSettingRecord(5, 3, 6);
        ContractRecord parent = effectiveContract();
        assertThat(parent.depositCent()).isEqualTo(500000L);
        repo.depositMonths = new DepositMonthsSettingRecord(3, 3, 6);
        ContractChangeRecord ch = service.raiseChange(parent.id(), change("k_c3a", "area_change",
                Map.of("areaSqm", 150, "baseRentCent", 150000, "depositDeadline", "2026-09-30")));
        approveFullChain(ch.id());
        ContractRecord child = service.listChildContracts(parent.id()).get(0);
        assertThat(child.depositCent()).isEqualTo(750000L);
        assertThat(child.snapshot()).containsEntry("depositMonths", 5);
        assertThat(terminationPort.adjustments).hasSize(1);
        assertThat(terminationPort.adjustments.get(0))
                .containsExactly("mch_1", child.id(), ch.id(), 250000L, LocalDate.parse("2026-09-30"));
        Map<String, Object> adj = castMap(child.rentTerms().get("depositAdjustment"));
        assertThat(adj).containsEntry("targetDepositCent", 750000L).containsEntry("deltaCent", 250000L)
                .containsEntry("direction", "collect").containsEntry("deadline", "2026-09-30")
                .containsEntry("arApItemId", "arap_adj_" + ch.id()).containsEntry("itemNo", "ARAP-ADJ-1");
        assertThat(child.rentTerms()).containsEntry("baseRentCent", 150000).doesNotContainKey("depositDeadline");
    }

    /**
     * 位置变更降租（per_sqm：子合同铺位 80㎡ 经 resolvePerSqmBaseRent 重算月租 80000）→ 目标＝3×80000＝240000、差额 −60000 → 登记应付（退还）；
     * web 蛇形键 space_ids / deposit_deadline 归一。
     */
    @Test
    void locationChangeLowerPerSqmRentRegistersRefundPayable() {
        PlatformRepository platform = mock(PlatformRepository.class);
        when(platform.findSpaceById("sp_big")).thenReturn(Optional.of(space("sp_big", "100")));
        when(platform.findSpaceById("sp_small")).thenReturn(Optional.of(space("sp_small", "80")));
        ContractService sqm = perSqmService(platform);
        ContractRecord d = sqm.createDraft(new ContractDraftRequest("k_c3b", "mch_c3b", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("rentMode", "per_sqm", "perSqmRentCent", 1000),
                "fixed_ratio", "none", null, Map.of(), List.of(), List.of(), null, null));
        submit(sqm, d, "k_c3b_sub");
        decideContract(sqm, d.id(), "approved");
        ContractRecord parent = sqm.activate(d.id(), new ContractActivateRequest("k_c3b_act", List.of("sp_big")));
        assertThat(parent.depositCent()).isEqualTo(300000L);
        ContractChangeRecord ch = sqm.raiseChange(parent.id(), change("k_c3b_ch", "location_change",
                Map.of("space_ids", List.of("sp_small"), "deposit_deadline", "2026-10-15")));
        String reviewId = repo.findChangeById(ch.id()).orElseThrow().reviewId();
        sqm.onReviewDecided(decision(reviewId, "approved", "contract.change", "contract_change", ch.id()));
        ContractRecord child = repo.listChildContracts(parent.id()).get(0);
        assertThat(repo.listContractSpaceIds(child.id())).containsExactly("sp_small");
        assertThat(child.depositCent()).isEqualTo(240000L);
        assertThat(terminationPort.adjustments.get(0))
                .containsExactly("mch_c3b", child.id(), ch.id(), -60000L, LocalDate.parse("2026-10-15"));
        Map<String, Object> adj = castMap(child.rentTerms().get("depositAdjustment"));
        assertThat(adj).containsEntry("direction", "refund").containsEntry("targetDepositCent", 240000L)
                .containsEntry("deltaCent", -60000L).containsEntry("deadline", "2026-10-15");
    }

    /**
     * 手工押金（newDepositCent／web 蛇形键 new_deposit_cent）拒收；缺或不可解析的办理期限、非每平米缺/零变更后月租 → 400 VALIDATION_FAILED；
     * 原合同快照缺押金月数（本次改动前的旧数据）→ 409 CONTRACT_DEPOSIT_MONTHS_MISSING 拒绝并提示，不推算。均不开复核单、不留变更。
     */
    @Test
    void amendmentRejectsManualDepositMissingDeadlineOrRentAndLegacySnapshotWithoutMonths() {
        ContractRecord parent = effectiveContract();
        List<Map<String, Object>> bad = List.of(
                Map.of("baseRentCent", 150000, "depositDeadline", "2026-09-30", "newDepositCent", 700000),
                Map.of("baseRentCent", 150000, "depositDeadline", "2026-09-30", "new_deposit_cent", 700000),
                Map.of("baseRentCent", 150000),
                Map.of("baseRentCent", 150000, "depositDeadline", "9月30日"),
                Map.of("depositDeadline", "2026-09-30"),
                Map.of("baseRentCent", 0, "depositDeadline", "2026-09-30"));
        int n = 0;
        for (Map<String, Object> payload : bad) {
            String key = "k_c3c_" + (n++);
            assertThatThrownBy(() -> service.raiseChange(parent.id(), change(key, "area_change", payload)))
                    .as("%s", payload)
                    .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        }
        assertThat(repo.listChangesByContract(parent.id())).isEmpty();
        ContractRecord legacy = effectiveContractRecord("ctr_c3c_legacy", 500000L, Map.of());   // 快照无 depositMonths
        assertThatThrownBy(() -> service.raiseChange(legacy.id(), change("k_c3c_legacy", "location_change",
                Map.of("baseRentCent", 150000, "depositDeadline", "2026-09-30"))))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("CONTRACT_DEPOSIT_MONTHS_MISSING");
                    assertThat(e.getMessage()).contains("押金月数");
                });
        assertThat(repo.listChangesByContract(legacy.id())).isEmpty();
    }

    /** 同租金面积变更：目标＝原押金、差额 0 → 不登记、不写 depositAdjustment；子合同再变更的终审事件重放不再开子合同、不双挂。 */
    @Test
    void amendmentWithoutDeltaRegistersNothingAndReplayDoesNotDoubleRegister() {
        ContractRecord parent = effectiveContract();
        ContractChangeRecord same = service.raiseChange(parent.id(), change("k_c3d", "area_change",
                Map.of("areaSqm", 100, "baseRentCent", 100000, "depositDeadline", "2026-09-30")));
        approveFullChain(same.id());
        ContractRecord child = service.listChildContracts(parent.id()).get(0);
        assertThat(child.depositCent()).isEqualTo(300000L);
        assertThat(child.rentTerms()).doesNotContainKey("depositAdjustment");
        assertThat(terminationPort.adjustments).isEmpty();

        ContractChangeRecord up = service.raiseChange(child.id(), change("k_c3d2", "area_change",
                Map.of("baseRentCent", 120000, "depositDeadline", "2026-10-31")));
        String reviewId = repo.findChangeById(up.id()).orElseThrow().reviewId();
        SupervisorReviewDecisionRecord approved = decision(reviewId, "approved", "contract.change", "contract_change", up.id());
        service.onReviewDecided(approved);
        service.onReviewDecided(approved);
        assertThat(terminationPort.adjustments).hasSize(1);
        assertThat(terminationPort.adjustments.get(0)[3]).isEqualTo(60000L);   // 3×120000 − 300000（子合同快照沿用 3 个月）
        assertThat(service.listChildContracts(child.id())).hasSize(1);
    }

    // ============ HP-C03-4 续租＝同一合同延期（2026-10-18 裁决第 4 条；BSM L1353；ENUM L255） ============
    /**
     * 裁决第 4 条「续租变更载荷带续租后月租、新到期日、押金办理期限」：缺续租后月租或为 0、缺新到期日或不晚于现到期日、缺办理期限、手工押金 →
     * 400 VALIDATION_FAILED；原合同快照缺押金月数 → 409 CONTRACT_DEPOSIT_MONTHS_MISSING。均不留变更、合同状态不动。
     */
    @Test
    void renewalRaiseRequiresRentLaterExpiryAndDepositDeadline() {
        ContractRecord c = effectiveContract();   // 到期 2027-06-30
        List<Map<String, Object>> bad = List.of(
                Map.of("newExpiryDate", "2028-06-30", "depositDeadline", "2027-07-15"),
                Map.of("baseRentCent", 0, "newExpiryDate", "2028-06-30", "depositDeadline", "2027-07-15"),
                Map.of("baseRentCent", 120000, "depositDeadline", "2027-07-15"),
                Map.of("baseRentCent", 120000, "newExpiryDate", "2027-06-30", "depositDeadline", "2027-07-15"),
                Map.of("baseRentCent", 120000, "newExpiryDate", "2027-05-31", "depositDeadline", "2027-07-15"),
                Map.of("baseRentCent", 120000, "newExpiryDate", "2028-06-30"),
                Map.of("baseRentCent", 120000, "newExpiryDate", "2028-06-30", "depositDeadline", "2027-07-15",
                        "newDepositCent", 360000));
        int n = 0;
        for (Map<String, Object> payload : bad) {
            String key = "k_rn_bad_" + (n++);
            assertThatThrownBy(() -> service.raiseChange(c.id(), change(key, "renewal", payload)))
                    .as("%s", payload)
                    .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        }
        assertThat(repo.listChangesByContract(c.id())).isEmpty();
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("effective");
        ContractRecord legacy = effectiveContractRecord("ctr_rn_legacy", 300000L, Map.of());   // 快照无 depositMonths
        assertThatThrownBy(() -> service.raiseChange(legacy.id(), change("k_rn_legacy", "renewal",
                Map.of("baseRentCent", 120000, "newExpiryDate", "2028-06-30", "depositDeadline", "2027-07-15"))))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTRACT_DEPOSIT_MONTHS_MISSING"));
        assertThat(repo.listChangesByContract(legacy.id())).isEmpty();
    }

    /**
     * 裁决第 4 条「终审通过时延长到期日、换月租、重固化快照、状态回「生效」、补排延长期账单，并按公式登记押金差额」＋ BSM L1353「续租的目标押金＝原合同押金
     * 月数×续租后的月租……原合同押金月数按原合同快照沿用，不改用当前配置月数」：生效时 5 个月（押金 500000）、其后配置改 3 个月；续租月租 100000→120000、
     * 到期 2027-06-30→2028-06-30 → 同一合同（不开子合同）到期日/月租/押金 600000/快照（月数 5）/生效态；延长期 2027-07～2028-06 按新月租补排 12 期、
     * 原租期 12 期不动；差额 +100000 登记应收、到期日＝办理期限；终审事件重放不再登记、不再排账。
     */
    @Test
    void renewalApprovalExtendsSameContractRefreezesSnapshotBillsExtensionAndRegistersDepositDelta() {
        repo.depositMonths = new DepositMonthsSettingRecord(5, 3, 6);
        ContractRecord c = effectiveContract();
        assertThat(c.depositCent()).isEqualTo(500000L);
        repo.depositMonths = new DepositMonthsSettingRecord(3, 3, 6);
        ContractChangeRecord ch = service.raiseChange(c.id(), change("k_rn1", "renewal",
                Map.of("baseRentCent", 120000, "newExpiryDate", "2028-06-30", "depositDeadline", "2027-07-15")));
        assertThat(repo.findContractById(c.id()).orElseThrow().expiryDate()).isEqualTo(LocalDate.parse("2027-06-30"));
        String reviewId = repo.findChangeById(ch.id()).orElseThrow().reviewId();
        SupervisorReviewDecisionRecord approved = decision(reviewId, "approved", "contract.change", "contract_change", ch.id());
        service.onReviewDecided(approved);

        ContractRecord renewed = repo.findContractById(c.id()).orElseThrow();
        assertThat(renewed.lifecycleStatus()).isEqualTo("effective");
        assertThat(renewed.expiryDate()).isEqualTo(LocalDate.parse("2028-06-30"));
        assertThat(renewed.rentTerms()).containsEntry("baseRentCent", 120000).doesNotContainKeys("newExpiryDate", "depositDeadline");
        assertThat(renewed.depositCent()).isEqualTo(600000L);
        assertThat(renewed.snapshot()).containsEntry("depositMonths", 5).containsEntry("depositCent", 600000L);
        assertThat(castMap(renewed.snapshot().get("rentTerms"))).containsEntry("baseRentCent", 120000);
        assertThat(service.listChildContracts(c.id())).isEmpty();
        List<ContractRentBillRecord> rent = rentBills(c.id());
        assertThat(rent).hasSize(24);
        assertThat(rent).filteredOn(b -> b.period().compareTo("2027-06") <= 0).hasSize(12)
                .allMatch(b -> b.amountCent() == 100000L);
        assertThat(rent).filteredOn(b -> b.period().compareTo("2027-07") >= 0).hasSize(12)
                .allMatch(b -> b.amountCent() == 120000L)
                .extracting(ContractRentBillRecord::period).contains("2027-07", "2028-06");
        assertThat(terminationPort.adjustments).hasSize(1);
        assertThat(terminationPort.adjustments.get(0))
                .containsExactly("mch_1", c.id(), ch.id(), 100000L, LocalDate.parse("2027-07-15"));
        Map<String, Object> adj = castMap(renewed.rentTerms().get("depositAdjustment"));
        assertThat(adj).containsEntry("targetDepositCent", 600000L).containsEntry("deltaCent", 100000L)
                .containsEntry("direction", "collect").containsEntry("deadline", "2027-07-15")
                .containsEntry("arApItemId", "arap_adj_" + ch.id()).containsEntry("itemNo", "ARAP-ADJ-1");
        ContractChangeRecord done = repo.findChangeById(ch.id()).orElseThrow();
        assertThat(done.approvalStatus()).isEqualTo("approved");
        assertThat(done.newSnapshot()).containsEntry("depositCent", 600000L);

        service.onReviewDecided(approved);   // 重放
        assertThat(terminationPort.adjustments).hasSize(1);
        assertThat(rentBills(c.id())).hasSize(24);
        assertThat(repo.findContractById(c.id()).orElseThrow().expiryDate()).isEqualTo(LocalDate.parse("2028-06-30"));
    }

    /**
     * 每平米计租续租（裁决第 4 条＋任务口径「per_sqm 合同按其 rent_terms 口径给单价并经 resolvePerSqmBaseRent 算月租」）：须给续租后单价（只给月租拒绝）；
     * 100㎡ × 9 元 → 续租后月租 90000、目标押金 3×90000＝270000、差额 −30000 → 登记应付（退还）；web 蛇形键归一。
     */
    @Test
    void perSqmRenewalUsesUnitPriceTimesSpaceAreaAndRegistersRefund() {
        PlatformRepository platform = mock(PlatformRepository.class);
        when(platform.findSpaceById("sp_rn")).thenReturn(Optional.of(space("sp_rn", "100")));
        ContractService sqm = perSqmService(platform);
        ContractRecord d = sqm.createDraft(new ContractDraftRequest("k_rn_sqm", "mch_rn_sqm", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("rentMode", "per_sqm", "perSqmRentCent", 1000),
                "fixed_ratio", "none", null, Map.of(), List.of(), List.of(), null, null));
        submit(sqm, d, "k_rn_sqm_sub");
        decideContract(sqm, d.id(), "approved");
        ContractRecord c = sqm.activate(d.id(), new ContractActivateRequest("k_rn_sqm_act", List.of("sp_rn")));
        assertThat(c.depositCent()).isEqualTo(300000L);
        assertThatThrownBy(() -> sqm.raiseChange(c.id(), change("k_rn_sqm_bad", "renewal",
                Map.of("baseRentCent", 90000, "newExpiryDate", "2028-06-30", "depositDeadline", "2027-07-15"))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        ContractChangeRecord ch = sqm.raiseChange(c.id(), change("k_rn_sqm_ch", "renewal",
                Map.of("per_sqm_rent_cent", 900, "new_expiry_date", "2028-06-30", "deposit_deadline", "2027-07-15")));
        String reviewId = repo.findChangeById(ch.id()).orElseThrow().reviewId();
        sqm.onReviewDecided(decision(reviewId, "approved", "contract.change", "contract_change", ch.id()));
        ContractRecord renewed = repo.findContractById(c.id()).orElseThrow();
        assertThat(renewed.expiryDate()).isEqualTo(LocalDate.parse("2028-06-30"));
        assertThat(renewed.rentTerms()).containsEntry("perSqmRentCent", 900).containsEntry("baseRentCent", 90000L);
        assertThat(renewed.depositCent()).isEqualTo(270000L);
        assertThat(terminationPort.adjustments.get(0))
                .containsExactly("mch_rn_sqm", c.id(), ch.id(), -30000L, LocalDate.parse("2027-07-15"));
        assertThat(castMap(renewed.rentTerms().get("depositAdjustment")))
                .containsEntry("direction", "refund").containsEntry("deltaCent", -30000L).containsEntry("targetDepositCent", 270000L);
    }

    /**
     * 续租延期铺位防重复出租（BSM L1350「合同生效时固化收益口径并绑定铺位（防止同一铺位重复出租）」）：延长期内铺位已被别的生效合同占用 →
     * 终审 409 CONTRACT_SPACE_CONFLICT，到期日/月租不动、不登记押金差额、变更仍待审。
     */
    @Test
    void renewalApprovalRejectedWhenExtensionSpaceOccupied() {
        ContractRecord c = effectiveContract();   // 绑 sp_1
        ContractChangeRecord ch = service.raiseChange(c.id(), change("k_rn_occ", "renewal",
                Map.of("baseRentCent", 120000, "newExpiryDate", "2028-06-30", "depositDeadline", "2027-07-15")));
        repo.occupiedSpaces.add("sp_1");   // 同一铺位的下一份合同已签约生效
        assertThatThrownBy(() -> decideChange(ch.id(), "approved"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CONTRACT_SPACE_CONFLICT"));
        ContractRecord after = repo.findContractById(c.id()).orElseThrow();
        assertThat(after.expiryDate()).isEqualTo(c.expiryDate());
        assertThat(after.rentTerms()).containsEntry("baseRentCent", 100000);
        assertThat(terminationPort.adjustments).isEmpty();
        assertThat(repo.findChangeById(ch.id()).orElseThrow().approvalStatus()).isEqualTo("pending");
    }

    /** 驳回不改条款（原租期内发起的续租，第三轮裁决第 5 条）：到期日/月租/押金/状态（生效中）不动、不登记不排账。 */
    @Test
    void renewalRejectedKeepsTermsAndStatus() {
        ContractRecord c = effectiveContract();
        int bills = rentBills(c.id()).size();
        ContractChangeRecord ch = service.raiseChange(c.id(), change("k_rn_rej", "renewal",
                Map.of("baseRentCent", 120000, "newExpiryDate", "2028-06-30", "depositDeadline", "2027-07-15")));
        decideChange(ch.id(), "rejected");
        ContractRecord after = repo.findContractById(c.id()).orElseThrow();
        assertThat(after.lifecycleStatus()).isEqualTo("effective");
        assertThat(after.expiryDate()).isEqualTo(c.expiryDate());
        assertThat(after.rentTerms()).isEqualTo(c.rentTerms());
        assertThat(after.depositCent()).isEqualTo(c.depositCent());
        assertThat(terminationPort.adjustments).isEmpty();
        assertThat(rentBills(c.id())).hasSize(bills);
    }

    // ============ 第三轮裁决第 3 条：续租／面积位置变更后租金递增以新月租为基数、自生效日重新计期 ============
    private static final Map<String, Object> RATIO_10_PER_YEAR = Map.of("mode", "ratio", "value", 0.10, "everyMonths", 12);

    /**
     * 第三轮裁决第 3 条「续租/变更录入的月租就是新的基数；原递增条款的周期与比例沿用，但从续租/变更生效日起重新计期」：原合同 2025-07-01 起、月租 100000、
     * 每 12 个月递增 10%、到期 2027-06-30；续租月租 120000、到期 2029-06-30 → 续租生效日 2027-07-01 起第一个周期（2027-07～2028-06）按 120000，满一个周期
     * （2028-07 起）才为 120000×1.1＝132000，不按原生效日累计期数（否则 2027-07 即为 120000×1.1²）；押金仍按新基数×月数＝3×120000。
     */
    @Test
    void renewalRestartsRentIncreasePeriodsFromRenewalEffectiveDateOnNewBase() {
        Instant t = Instant.now();
        ContractRecord c = repo.saveContract(new ContractRecord("ctr_rn_inc", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2025-07-01"), LocalDate.parse("2027-06-30"),
                Map.of("baseRentCent", 100000, "increase", RATIO_10_PER_YEAR), "fixed_ratio", "none", 300000L, Map.of(), List.of(),
                Map.of("depositMonths", 3), null, List.of(), "effective", "proj_1", null, null, null, t, t));
        ContractChangeRecord ch = service.raiseChange(c.id(), change("k_rn_inc", "renewal",
                Map.of("baseRentCent", 120000, "newExpiryDate", "2029-06-30", "depositDeadline", "2027-07-15")));
        decideChange(ch.id(), "approved");
        Map<String, Long> rent = new LinkedHashMap<>();
        rentBills(c.id()).forEach(b -> rent.put(b.period(), b.amountCent()));
        assertThat(rent).containsEntry("2027-07", 120000L).containsEntry("2028-06", 120000L)
                .containsEntry("2028-07", 132000L).containsEntry("2029-06", 132000L);
        assertThat(repo.findContractById(c.id()).orElseThrow().depositCent()).isEqualTo(360000L);
    }

    /**
     * 同上，面积/位置变更子合同（第四轮：自 activate 走起的真实流程）：原合同 6 个月前生效、月租 100000、每 12 个月递增 10%，生效即预排整租期；今日终审面积变更、
     * 变更后月租 150000 → 子合同自变更生效日（终审当日）起计期并重排剩余租期：变更月与其后第 11 个月为 150000，满 12 个月才为 165000（不按原合同生效日累计，
     * 否则变更后第 6 个月即已递增）；押金按新基数×月数＝3×150000。
     */
    @Test
    void areaChangeRestartsRentIncreasePeriodsFromChangeEffectiveDateOnNewBase() {
        YearMonth now = YearMonth.now(LocalZone.ID);
        ContractRecord parent = activatedSixMonthsAgo(service, "k_am_inc", Map.of("baseRentCent", 100000, "increase", RATIO_10_PER_YEAR),
                null, "sp_1");
        ContractChangeRecord ch = service.raiseChange(parent.id(), change("k_am_inc_ch", "area_change",
                Map.of("areaSqm", 150, "baseRentCent", 150000, "depositDeadline", now.atEndOfMonth().toString())));
        approveFullChain(ch.id());
        ContractRecord child = service.listChildContracts(parent.id()).get(0);
        assertThat(bills(child.id(), "rent", false)).containsEntry(now.toString(), 150000L)
                .containsEntry(now.plusMonths(6).toString(), 150000L).containsEntry(now.plusMonths(11).toString(), 150000L)
                .containsEntry(now.plusMonths(12).toString(), 165000L).containsEntry(now.plusMonths(24).toString(), 181500L);
        assertThat(child.depositCent()).isEqualTo(450000L);
    }

    // ============ 第四轮裁决第 2 条：面积/位置变更生效时重排剩余租期 ============
    /**
     * 第四轮裁决第 2 条「变更生效时作废父合同自变更生效月起尚未收款的预排租金/物业费应收（留痕），按子合同新月租和重新计期的递增重排剩余租期」：
     * 生效即预排整租期（6 个月前生效、30 个月后到期；月租 100000、物业费 20000）；变更月后第 2 个月的租金已部分收款。今日终审面积变更（月租 150000）→
     * 变更月前的父账单不动；变更月起父合同整笔未收的租金/物业费账单作废、作废原因含变更 id；已收款那期不作废、子合同也不为它重排（链去重仍认它）；
     * 子合同自变更月起重排：租金 150000、物业费沿用合同物业费 20000；重放终审事件不重复作废/重排。
     */
    @Test
    void areaChangeVoidsUnpaidParentBillsFromChangeMonthAndRebillsRemainingTermOnChild() {
        YearMonth now = YearMonth.now(LocalZone.ID);
        ContractRecord parent = activatedSixMonthsAgo(service, "k_rb", Map.of("baseRentCent", 100000, "propertyFeeCent", 20000),
                null, "sp_1");
        String paidPeriod = now.plusMonths(2).toString();
        repo.rentBills.stream().filter(b -> b.contractId().equals(parent.id()) && b.period().equals(paidPeriod) && b.feeType().equals("rent"))
                .forEach(b -> repo.paidArItems.add(b.arApItemId()));
        ContractChangeRecord ch = service.raiseChange(parent.id(), change("k_rb_ch", "area_change",
                Map.of("areaSqm", 150, "baseRentCent", 150000, "depositDeadline", now.atEndOfMonth().toString())));
        approveFullChain(ch.id());
        ContractRecord child = service.listChildContracts(parent.id()).get(0);

        assertThat(bills(parent.id(), "rent", false)).hasSize(7).containsEntry(now.minusMonths(6).toString(), 100000L)
                .containsEntry(now.minusMonths(1).toString(), 100000L).containsEntry(paidPeriod, 100000L);
        assertThat(bills(parent.id(), "rent", true)).hasSize(30).containsKey(now.toString())
                .containsKey(now.plusMonths(30).toString()).doesNotContainKey(paidPeriod);
        assertThat(bills(parent.id(), "property_fee", false)).hasSize(6).doesNotContainKey(now.toString());
        assertThat(bills(parent.id(), "property_fee", true)).hasSize(31);
        assertThat(repo.voidedBills.values()).hasSize(61).allSatisfy(reason -> assertThat(reason).contains(ch.id()));

        Map<String, Long> childRent = bills(child.id(), "rent", false);
        assertThat(childRent).hasSize(30).doesNotContainKey(paidPeriod).doesNotContainKey(now.minusMonths(1).toString());
        assertThat(childRent.values()).containsOnly(150000L);
        assertThat(bills(child.id(), "property_fee", false)).hasSize(31).containsEntry(now.toString(), 20000L)
                .containsEntry(now.plusMonths(30).toString(), 20000L);

        int billCount = repo.rentBills.size();
        service.onReviewDecided(decision(repo.findChangeById(ch.id()).orElseThrow().reviewId(), "approved", "contract.change",
                "contract_change", ch.id()));
        assertThat(repo.rentBills).hasSize(billCount);
        assertThat(repo.voidedBills).hasSize(61);
    }

    /**
     * 位置变更同理（web 端 adminClient.mapKeys 把载荷键蛇形化：space_ids／base_rent_cent／deposit_deadline）：变更月起父合同租金账单全部作废，
     * 子合同按新月租 80000 重排至到期月；变更月前的父账单不动。
     */
    @Test
    void locationChangeWithSnakeCasePayloadVoidsAndRebillsFromChangeMonth() {
        YearMonth now = YearMonth.now(LocalZone.ID);
        ContractRecord parent = activatedSixMonthsAgo(service, "k_rbl", Map.of("baseRentCent", 100000), null, "sp_1");
        ContractChangeRecord ch = service.raiseChange(parent.id(), change("k_rbl_ch", "location_change",
                Map.of("space_ids", List.of("sp_2"), "base_rent_cent", 80000, "deposit_deadline", now.atEndOfMonth().toString())));
        approveFullChain(ch.id());
        ContractRecord child = service.listChildContracts(parent.id()).get(0);
        assertThat(repo.listContractSpaceIds(child.id())).containsExactly("sp_2");
        assertThat(bills(parent.id(), "rent", false)).hasSize(6).doesNotContainKey(now.toString());
        assertThat(bills(parent.id(), "rent", true)).hasSize(31);
        Map<String, Long> childRent = bills(child.id(), "rent", false);
        assertThat(childRent).hasSize(31).containsKey(now.toString()).containsKey(now.plusMonths(30).toString());
        assertThat(childRent.values()).containsOnly(80000L);
    }

    /**
     * 纯提成合同只重排物业费（无月租）：生效预排物业费 20000/月（提成租金生效时为 0 不出账），变更月已按营业额出过一期提成租金；面积变更生效 →
     * 变更月起父合同物业费作废、子合同重排；提成租金账单不作废，重排也不调提成租金计算、子合同不出租金账单。
     */
    @Test
    void turnoverOnlyAmendmentRebillsOnlyPropertyFee() {
        com.complex.platform.finance.FinanceService turnover = mock(com.complex.platform.finance.FinanceService.class);
        when(turnover.computePeriodTurnoverRentSnapshot(any(), any(), any()))
                .thenReturn(new com.complex.platform.finance.FinanceService.PeriodTurnoverRent(null, 0L));
        ContractService svc = new ContractService(repo, audit, authz, null, null, null, turnover, terminationPort, null, reviews);
        YearMonth now = YearMonth.now(LocalZone.ID);
        ContractRecord parent = activatedSixMonthsAgo(svc, "k_rbt", Map.of("rentBillingMode", "turnover_only", "propertyFeeCent", 20000),
                500000L, "sp_tov");
        when(turnover.computePeriodTurnoverRentSnapshot(any(), any(), any()))
                .thenReturn(new com.complex.platform.finance.FinanceService.PeriodTurnoverRent("trr_rbt", 50000L));
        svc.generatePeriodBills(parent.id(), new ContractRentBillRequest("k_rbt_bill", now.toString()));
        assertThat(bills(parent.id(), "rent", false)).containsOnlyKeys(now.toString());
        org.mockito.Mockito.clearInvocations(turnover);

        ContractChangeRecord area = svc.raiseChange(parent.id(), change("k_rbt_ch", "area_change", Map.of("areaSqm", 150)));
        svc.onReviewDecided(decision(repo.findChangeById(area.id()).orElseThrow().reviewId(), "approved", "contract.change",
                "contract_change", area.id()));
        ContractRecord child = repo.listChildContracts(parent.id()).get(0);
        assertThat(bills(parent.id(), "rent", false)).containsOnlyKeys(now.toString());
        assertThat(bills(parent.id(), "property_fee", true)).hasSize(31);
        assertThat(bills(child.id(), "property_fee", false)).hasSize(31).containsEntry(now.toString(), 20000L);
        assertThat(bills(child.id(), "rent", false)).isEmpty();
        verify(turnover, never()).computePeriodTurnoverRentSnapshot(any(), any(), any());
    }

    /**
     * 「保底与抽成取高」合同（BSM L1347「模式含：保底与抽成取高、固定比例、阶梯比例、保底加抽成叠加」）面积变更终审重排（第四轮裁决第 2 条「按子合同新月租和
     * 重新计期的递增重排剩余租期」）：抽成规则按合同 id 精确匹配、只挂在父合同（子合同 id 终审时才派生，没有它的规则）→ 子合同出账沿变更链取父合同该期规则，
     * 终审不因 TURNOVER_RENT_RULE_PERIOD_UNRESOLVED 整笔回滚；子合同自变更月起重排租金＝max(新月租 150000, 当期抽成 0)，账单固化父合同规则 id。
     */
    @Test
    void higherOfAmendmentRebillsChildRentWithParentTurnoverRule() {
        com.complex.platform.finance.FinanceService turnover = mock(com.complex.platform.finance.FinanceService.class);
        when(turnover.computePeriodTurnoverRentSnapshot(any(), any(), any())).thenAnswer(inv -> {
            boolean child = repo.findContractById(inv.getArgument(1))
                    .map(c -> c.rentTerms() != null && c.rentTerms().containsKey("parentContractId")).orElse(false);
            if (child) {
                throw ApiException.conflict("TURNOVER_RENT_RULE_PERIOD_UNRESOLVED", "账期须由唯一已复核且未人工下线的抽成规则完整覆盖");
            }
            return new com.complex.platform.finance.FinanceService.PeriodTurnoverRent("trr_parent", 0L);
        });
        ContractService svc = new ContractService(repo, audit, authz, null, null, null, turnover, terminationPort, null, reviews);
        YearMonth now = YearMonth.now(LocalZone.ID);
        ContractRecord parent = activatedSixMonthsAgo(svc, "k_rbh", Map.of("rentBillingMode", "higher_of", "baseRentCent", 100000),
                null, "sp_hof");
        org.mockito.Mockito.clearInvocations(turnover);
        ContractChangeRecord area = svc.raiseChange(parent.id(), change("k_rbh_ch", "area_change",
                Map.of("areaSqm", 150, "baseRentCent", 150000, "depositDeadline", now.atEndOfMonth().toString())));
        assertThatCode(() -> svc.onReviewDecided(decision(repo.findChangeById(area.id()).orElseThrow().reviewId(), "approved",
                "contract.change", "contract_change", area.id()))).doesNotThrowAnyException();

        ContractRecord child = repo.listChildContracts(parent.id()).get(0);
        Map<String, Long> childRent = bills(child.id(), "rent", false);
        assertThat(childRent).hasSize(31).containsKey(now.toString()).containsKey(now.plusMonths(30).toString());
        assertThat(childRent.values()).containsOnly(150000L);
        assertThat(repo.rentBills.stream().filter(b -> b.contractId().equals(child.id())).map(ContractRentBillRecord::turnoverRuleId))
                .containsOnly("trr_parent");
        assertThat(bills(parent.id(), "rent", true)).hasSize(31);
        verify(turnover).computePeriodTurnoverRentSnapshot(eq("mch_1"), eq(parent.id()), eq(now.toString()));
    }

    // ============ 第五轮裁决第 2 条：租金调整生效时重排剩余租期 ============
    /**
     * 第五轮裁决第 2 条「租金调整生效时，作废自生效月起尚未收款的预排租金应收（留痕），按调整后月租重排剩余租期；递增同样以新月租为基数从生效日起重新计期」：
     * 生效即预排整租期（6 个月前生效、30 个月后到期；月租 100000、每 12 个月递增 10%、物业费 20000）；终审月后第 2 个月租金已部分收款。今日终审租金调整
     * （月租 150000；载荷无生效日 → 生效日＝终审当日）→ 同一合同、不开子合同：终审月前的租金账单不动；终审月起整笔未收的租金账单作废、原因含变更 id；
     * 已收款那期不作废、不重排；物业费不作废（裁决只写租金）；自终审月起按 150000 重排，满 12 个月才 165000、满 24 个月 181500；rent_terms.increaseFrom＝终审当日；
     * 重放终审事件不重复作废/重排。
     */
    @Test
    void rentChangeVoidsUnpaidRentFromApprovalMonthAndRebillsSameContractOnNewBase() {
        YearMonth now = YearMonth.now(LocalZone.ID);
        ContractRecord c = activatedSixMonthsAgo(service, "k_rc",
                Map.of("baseRentCent", 100000, "increase", RATIO_10_PER_YEAR, "propertyFeeCent", 20000), null, "sp_1");
        String paidPeriod = now.plusMonths(2).toString();
        repo.rentBills.stream().filter(b -> b.contractId().equals(c.id()) && b.period().equals(paidPeriod) && b.feeType().equals("rent"))
                .forEach(b -> repo.paidArItems.add(b.arApItemId()));
        ContractChangeRecord ch = service.raiseChange(c.id(), change("k_rc_ch", "rent_change", Map.of("baseRentCent", 150000)));
        approveFullChain(ch.id());

        assertThat(service.listChildContracts(c.id())).isEmpty();
        assertThat(repo.findContractById(c.id()).orElseThrow().rentTerms()).containsEntry("baseRentCent", 150000)
                .containsEntry("increaseFrom", LocalDate.now(LocalZone.ID).toString());
        assertThat(bills(c.id(), "rent", true)).hasSize(30).containsKey(now.toString()).containsKey(now.plusMonths(30).toString())
                .doesNotContainKey(paidPeriod).doesNotContainKey(now.minusMonths(1).toString());
        assertThat(repo.voidedBills.values()).hasSize(30).allSatisfy(reason -> assertThat(reason).contains(ch.id()));
        assertThat(bills(c.id(), "property_fee", true)).isEmpty();
        assertThat(bills(c.id(), "rent", false)).hasSize(37).containsEntry(now.minusMonths(6).toString(), 100000L)
                .containsEntry(now.minusMonths(1).toString(), 100000L).containsEntry(paidPeriod, 100000L)
                .containsEntry(now.toString(), 150000L).containsEntry(now.plusMonths(11).toString(), 150000L)
                .containsEntry(now.plusMonths(12).toString(), 165000L).containsEntry(now.plusMonths(30).toString(), 181500L);

        int billCount = repo.rentBills.size();
        service.onReviewDecided(decision(repo.findChangeById(ch.id()).orElseThrow().reviewId(), "approved", "contract.change",
                "contract_change", ch.id()));
        assertThat(repo.rentBills).hasSize(billCount);
        assertThat(repo.voidedBills).hasSize(30);
    }

    /**
     * 同上，每平米计租合同、web 端蛇形载荷（adminClient.mapKeys：per_sqm_rent_cent）：给调整后单价 → 终审按合同铺位面积算月租（100㎡×12 元＝120000）重排、
     * 蛇形键归一不留进 rent_terms；只给月租、单价为 0、缺调整后月租 → 400 VALIDATION_FAILED、不留变更。
     */
    @Test
    void perSqmRentChangeWithSnakeCasePayloadRebillsOnUnitPriceTimesArea() {
        PlatformRepository platform = mock(PlatformRepository.class);
        when(platform.findSpaceById("sp_rc")).thenReturn(Optional.of(space("sp_rc", "100")));
        ContractService sqm = perSqmService(platform);
        YearMonth now = YearMonth.now(LocalZone.ID);
        ContractRecord c = activatedSixMonthsAgo(sqm, "k_rcs", Map.of("rentMode", "per_sqm", "perSqmRentCent", 1000), null, "sp_rc");
        int n = 0;
        for (Map<String, Object> bad : List.<Map<String, Object>>of(Map.of("note", "调租"), Map.of("base_rent_cent", 120000),
                Map.of("per_sqm_rent_cent", 0))) {
            String key = "k_rcs_bad_" + (n++);
            assertThatThrownBy(() -> sqm.raiseChange(c.id(), change(key, "rent_change", bad))).as("%s", bad)
                    .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        }
        assertThat(repo.listChangesByContract(c.id())).isEmpty();
        ContractChangeRecord ch = sqm.raiseChange(c.id(), change("k_rcs_ch", "rent_change", Map.of("per_sqm_rent_cent", 1200)));
        sqm.onReviewDecided(decision(repo.findChangeById(ch.id()).orElseThrow().reviewId(), "approved", "contract.change",
                "contract_change", ch.id()));
        assertThat(repo.findContractById(c.id()).orElseThrow().rentTerms()).containsEntry("perSqmRentCent", 1200)
                .containsEntry("baseRentCent", 120000L).doesNotContainKey("per_sqm_rent_cent");
        assertThat(bills(c.id(), "rent", false)).hasSize(37).containsEntry(now.minusMonths(1).toString(), 100000L)
                .containsEntry(now.toString(), 120000L).containsEntry(now.plusMonths(30).toString(), 120000L);
    }

    /** 纯提成合同（没有月租）的租金调整不重排租金：终审后不作废已出的提成租金账单、不调提成计算重排。 */
    @Test
    void turnoverOnlyRentChangeDoesNotVoidOrRebillRent() {
        com.complex.platform.finance.FinanceService turnover = mock(com.complex.platform.finance.FinanceService.class);
        when(turnover.computePeriodTurnoverRentSnapshot(any(), any(), any()))
                .thenReturn(new com.complex.platform.finance.FinanceService.PeriodTurnoverRent(null, 0L));
        ContractService svc = new ContractService(repo, audit, authz, null, null, null, turnover, terminationPort, null, reviews);
        YearMonth now = YearMonth.now(LocalZone.ID);
        ContractRecord c = activatedSixMonthsAgo(svc, "k_rct", Map.of("rentBillingMode", "turnover_only", "propertyFeeCent", 20000),
                500000L, "sp_rct");
        when(turnover.computePeriodTurnoverRentSnapshot(any(), any(), any()))
                .thenReturn(new com.complex.platform.finance.FinanceService.PeriodTurnoverRent("trr_rct", 50000L));
        svc.generatePeriodBills(c.id(), new ContractRentBillRequest("k_rct_bill", now.toString()));
        org.mockito.Mockito.clearInvocations(turnover);
        ContractChangeRecord ch = svc.raiseChange(c.id(), change("k_rct_ch", "rent_change", Map.of("note", "提成条款说明")));
        svc.onReviewDecided(decision(repo.findChangeById(ch.id()).orElseThrow().reviewId(), "approved", "contract.change",
                "contract_change", ch.id()));
        assertThat(repo.findChangeById(ch.id()).orElseThrow().approvalStatus()).isEqualTo("approved");
        assertThat(repo.voidedBills).isEmpty();
        assertThat(bills(c.id(), "rent", false)).containsOnlyKeys(now.toString());
        verify(turnover, never()).computePeriodTurnoverRentSnapshot(any(), any(), any());
    }

    /** 起草→提交→审批通过→生效（绑 spaceId，生效即预排整租期）：生效于 6 个月前月初、到期于 30 个月后月末。 */
    private ContractRecord activatedSixMonthsAgo(ContractService svc, String key, Map<String, Object> rentTerms, Long depositCent,
            String spaceId) {
        YearMonth now = YearMonth.now(LocalZone.ID);
        ContractRecord d = svc.createDraft(new ContractDraftRequest(key, "mch_1", "subj_1", null, "subj_1",
                now.minusMonths(6).atDay(1), now.plusMonths(30).atEndOfMonth(), rentTerms, "fixed_ratio", "none", depositCent,
                Map.of(), List.of(), List.of(), null, null));
        submit(svc, d, key + "_sub");
        decideContract(svc, d.id(), "approved");
        return svc.activate(d.id(), new ContractActivateRequest(key + "_act", List.of(spaceId)));
    }

    /** 某合同某费目的账单（账期→金额）：voided=true 只列已作废的，false 只列未作废的。 */
    private Map<String, Long> bills(String contractId, String feeType, boolean voided) {
        Map<String, Long> out = new LinkedHashMap<>();
        repo.rentBills.stream().filter(b -> b.contractId().equals(contractId) && b.feeType().equals(feeType)
                && repo.voidedBills.containsKey(b.id()) == voided).forEach(b -> out.put(b.period(), b.amountCent()));
        return out;
    }

    // ============ 第三轮裁决第 1 条：纯提成合同押金按约定金额录入 ============
    /**
     * 第三轮裁决第 1 条「纯提成合同的押金由招商按合同约定金额直接录入，不走月数公式，但不得为零；这类合同变更/续租时押金不按公式重算」（BSM L1347「不允许零押金
     * 或负押金」）：收租方式纯提成（rent_terms.rentBillingMode=turnover_only，无月租）起草录约定押金——0、负、缺 → 400 VALIDATION_FAILED；录 500000 →
     * 草稿与生效押金均为 500000、快照不写押金月数；面积变更（不给月租与办理期限）、续租（只给新到期日）不报 CONTRACT_DEPOSIT_MONTHS_MISSING、押金沿用
     * 500000、不登记押金差额。有月租的合同（保底取高 higher_of）仍按公式：手工押金与公式不一致 → CONTRACT_DEPOSIT_FORMULA_MISMATCH。
     */
    @Test
    void turnoverOnlyDepositIsAgreedAmountAndNotRecomputedOnAmendmentOrRenewal() {
        com.complex.platform.finance.FinanceService turnover = mock(com.complex.platform.finance.FinanceService.class);
        when(turnover.computePeriodTurnoverRentSnapshot(any(), any(), any()))
                .thenReturn(new com.complex.platform.finance.FinanceService.PeriodTurnoverRent(null, 0L));
        ContractService svc = new ContractService(repo, audit, authz, null, null, null, turnover, terminationPort, null, reviews);
        Map<String, Object> terms = Map.of("rentBillingMode", "turnover_only");
        for (Long bad : new Long[]{0L, -100L, null}) {
            assertThatThrownBy(() -> svc.createDraft(new ContractDraftRequest("k_tov_bad_" + bad, "mch_tov", "subj_1", null, "subj_1",
                    LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), terms, "fixed_ratio", "none", bad,
                    Map.of(), List.of(), List.of(), null, null)))
                    .as("deposit=%s", bad)
                    .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        }
        ContractRecord d = svc.createDraft(new ContractDraftRequest("k_tov", "mch_tov", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), terms, "fixed_ratio", "none", 500000L,
                Map.of(), List.of(), List.of(), null, null));
        assertThat(d.depositCent()).isEqualTo(500000L);
        submit(svc, d, "k_tov_sub");
        decideContract(svc, d.id(), "approved");
        ContractRecord eff = svc.activate(d.id(), new ContractActivateRequest("k_tov_act", List.of("sp_tov")));
        assertThat(eff.depositCent()).isEqualTo(500000L);
        assertThat(eff.snapshot()).containsEntry("depositCent", 500000L);
        assertThat(eff.snapshot().get("depositMonths")).isNull();

        ContractChangeRecord[] area = new ContractChangeRecord[1];
        assertThatCode(() -> area[0] = svc.raiseChange(eff.id(), change("k_tov_area", "area_change", Map.of("areaSqm", 150))))
                .doesNotThrowAnyException();
        svc.onReviewDecided(decision(repo.findChangeById(area[0].id()).orElseThrow().reviewId(), "approved", "contract.change",
                "contract_change", area[0].id()));
        ContractRecord child = repo.listChildContracts(eff.id()).get(0);
        assertThat(child.depositCent()).isEqualTo(500000L);
        assertThat(child.rentTerms()).doesNotContainKey("depositAdjustment");

        ContractChangeRecord[] renewal = new ContractChangeRecord[1];
        assertThatCode(() -> renewal[0] = svc.raiseChange(child.id(), change("k_tov_rn", "renewal",
                Map.of("newExpiryDate", "2028-06-30")))).doesNotThrowAnyException();
        svc.onReviewDecided(decision(repo.findChangeById(renewal[0].id()).orElseThrow().reviewId(), "approved", "contract.change",
                "contract_change", renewal[0].id()));
        ContractRecord renewed = repo.findContractById(child.id()).orElseThrow();
        assertThat(renewed.expiryDate()).isEqualTo(LocalDate.parse("2028-06-30"));
        assertThat(renewed.depositCent()).isEqualTo(500000L);
        assertThat(terminationPort.adjustments).isEmpty();

        assertThatThrownBy(() -> svc.createDraft(new ContractDraftRequest("k_tov_hi", "mch_hi", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"),
                Map.of("baseRentCent", 100000, "rentBillingMode", "higher_of"), "guarantee_or_commission_higher", "none", 500000L,
                Map.of(), List.of(), List.of(), null, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CONTRACT_DEPOSIT_FORMULA_MISMATCH"));

        // web 客户端把 rent_terms 嵌套键蛇形化（rent_billing_mode）：同样识别为纯提成、按约定押金起草，收租方式键归一回 rentBillingMode（出账读此键）。
        ContractRecord[] web = new ContractRecord[1];
        assertThatCode(() -> web[0] = svc.createDraft(new ContractDraftRequest("k_tov_web", "mch_tov_web", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("rent_billing_mode", "turnover_only"), "fixed_ratio",
                "none", 500000L, Map.of(), List.of(), List.of(), null, null))).doesNotThrowAnyException();
        assertThat(web[0].depositCent()).isEqualTo(500000L);
        assertThat(web[0].rentTerms()).containsEntry("rentBillingMode", "turnover_only").doesNotContainKey("rent_billing_mode");
    }

    private List<ContractRentBillRecord> rentBills(String contractId) {
        return repo.rentBills.stream().filter(b -> b.contractId().equals(contractId) && b.feeType().equals("rent")).toList();
    }

    // ============ HP-C06 押金退还等待期（BSM L1352／ENUM L252「押金的退还不在审批通过时立即执行，而是以退租结算算账完成为准执行支付，且不早于
    // 等待期届满（等待期自最后一个审批完成之日起算，业务参数，按项目可配，出厂默认 30 日，须同时配置上下限）……算账晚于等待期完成的顺延至算账完成之日
    // 支付，顺延原因与预计时间在商户专区对商户可见」、2026-10-18 裁决第 7 条） ============

    private Map<String, Object> depositDisposalOf(String merchantId, String contractId) {
        // 商户专区「我的对账」读 listSettlementsByMerchant：取该合同已落账那张单的押金处置项。
        return repo.listSettlementsByMerchant(merchantId).stream()
                .filter(s -> contractId.equals(s.contractId()) && !"pending_settlement".equals(s.status()))
                .flatMap(s -> s.settlementItems().stream())
                .filter(i -> "deposit_disposal".equals(i.get("item"))).findFirst().orElseThrow();
    }

    /**
     * 出厂 30 日、上下限未配置；缺值/缺上下限、下限&lt;0、值越界、下限&gt;上限、缺项目 → 400 VALIDATION_FAILED 且不落库；合法配置只作用于该项目；
     * 项目范围账号不得改别的项目（CROSS_PROJECT_FORBIDDEN）；读写均须 contract.deposit_refund_wait.manage。
     */
    @Test
    void depositRefundWaitDefaultsTo30PerProjectAndRejectsMissingOrOutOfBounds() {
        assertThat(service.getDepositRefundWaitSetting("proj_1"))
                .isEqualTo(new DepositRefundWaitSettingRecord("__global__", 30, null, null));
        Integer[][] invalid = {{45, null, 60}, {45, 30, null}, {null, 30, 60}, {45, -1, 60}, {20, 30, 60}, {61, 30, 60}, {45, 50, 40}};
        for (Integer[] v : invalid) {
            assertThatThrownBy(() -> service.saveDepositRefundWaitSetting(
                    new DepositRefundWaitSettingRequest("k_rw_bad", "proj_1", v[0], v[1], v[2], "调整")))
                    .as("value=%s min=%s max=%s", v[0], v[1], v[2])
                    .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        }
        assertThatThrownBy(() -> service.saveDepositRefundWaitSetting(
                new DepositRefundWaitSettingRequest("k_rw_nop", null, 45, 30, 60, "调整")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThat(repo.refundWait).isEmpty();
        DepositRefundWaitSettingRecord saved = service.saveDepositRefundWaitSetting(
                new DepositRefundWaitSettingRequest("k_rw_ok", "proj_1", 45, 30, 60, "调整"));
        assertThat(saved).isEqualTo(new DepositRefundWaitSettingRecord("proj_1", 45, 30, 60));
        assertThat(service.getDepositRefundWaitSetting("proj_1")).isEqualTo(saved);
        assertThat(service.getDepositRefundWaitSetting("proj_2").value()).isEqualTo(30);
        when(authz.requireReachableProject(eq("proj_1"), eq("contract.deposit_refund_wait.manage"), any()))
                .thenThrow(ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "所选项目不在本权限可触及的项目范围内"));
        assertThatThrownBy(() -> service.saveDepositRefundWaitSetting(
                new DepositRefundWaitSettingRequest("k_rw_x", "proj_1", 50, 30, 60, "调整")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
        assertThat(repo.refundWait.get("proj_1").value()).isEqualTo(45);
        verify(authz, org.mockito.Mockito.atLeast(3)).requirePermission("contract.deposit_refund_wait.manage");
    }

    /**
     * 第三轮控制席约定「两个新参数配置写口按 OpenAPI 声明记失败审计」（照 CG-08b configurePolicy）：押金收取月数下限低于 3 →
     * contract.deposit_months_config_failed（VALIDATION_FAILED）；押金退还等待期改别的项目 → contract.deposit_refund_wait_config_failed
     * （CROSS_PROJECT_FORBIDDEN）；均不记成功审计、配置不变。
     */
    @Test
    void depositSettingWriteRejectionsRecordConfigFailedAudit() {
        assertThatThrownBy(() -> service.saveDepositMonthsSetting(new DepositMonthsSettingRequest("k_dm_f", 2, 2, 6, "调整")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        verify(audit).recordFailure("contract.deposit_months_config_failed", "contract.deposit_months.manage",
                "contract_deposit_policy", "global", null, "k_dm_f", "VALIDATION_FAILED");
        when(authz.requireReachableProject(eq("proj_1"), eq("contract.deposit_refund_wait.manage"), any()))
                .thenThrow(ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "所选项目不在本权限可触及的项目范围内"));
        assertThatThrownBy(() -> service.saveDepositRefundWaitSetting(
                new DepositRefundWaitSettingRequest("k_rw_f", "proj_1", 45, 30, 60, "调整")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
        verify(audit).recordFailure("contract.deposit_refund_wait_config_failed", "contract.deposit_refund_wait.manage",
                "contract_deposit_refund_wait_policy", "proj_1", null, "k_rw_f", "CROSS_PROJECT_FORBIDDEN");
        verify(audit, never()).recordSuccess(eq("contract.deposit_months_configured"), any(), any(), any(), any(), any(), any());
        verify(audit, never()).recordSuccess(eq("contract.deposit_refund_wait_configured"), any(), any(), any(), any(), any(), any());
        assertThat(repo.depositMonths).isEqualTo(new DepositMonthsSettingRecord(3, null, null));
        assertThat(repo.refundWait).isEmpty();
    }

    /**
     * 两个配置写口的写库与成功审计经 idempotencyService.execute（照 CG-08b configurePolicy）：同键同体重放返回首次结果、
     * 不重复写成功审计；同键不同体 409 IDEMPOTENCY_CONFLICT 且配置不变。
     */
    @Test
    void depositSettingWritesReplaySameKeyAndRejectDifferentBody() {
        DepositMonthsSettingRecord months = service.saveDepositMonthsSetting(new DepositMonthsSettingRequest("k_dm_i", 4, 3, 6, "调整"));
        assertThat(service.saveDepositMonthsSetting(new DepositMonthsSettingRequest("k_dm_i", 4, 3, 6, "调整"))).isEqualTo(months);
        assertThatThrownBy(() -> service.saveDepositMonthsSetting(new DepositMonthsSettingRequest("k_dm_i", 5, 3, 6, "调整")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
        assertThat(repo.depositMonths).isEqualTo(new DepositMonthsSettingRecord(4, 3, 6));
        verify(audit).recordSuccess(eq("contract.deposit_months_configured"), any(), any(), any(), any(), any(), any());

        DepositRefundWaitSettingRecord wait = service.saveDepositRefundWaitSetting(
                new DepositRefundWaitSettingRequest("k_rw_i", "proj_1", 45, 30, 60, "调整"));
        assertThat(service.saveDepositRefundWaitSetting(
                new DepositRefundWaitSettingRequest("k_rw_i", "proj_1", 45, 30, 60, "调整"))).isEqualTo(wait);
        assertThatThrownBy(() -> service.saveDepositRefundWaitSetting(
                new DepositRefundWaitSettingRequest("k_rw_i", "proj_1", 50, 30, 60, "调整")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
        assertThat(repo.refundWait.get("proj_1").value()).isEqualTo(45);
        verify(audit).recordSuccess(eq("contract.deposit_refund_wait_configured"), any(), any(), any(), any(), any(), any());
    }

    /** 退租终审自动落账：押金退还应付到期日＝最后审批完成日（所在地日期）＋等待期——未配置取出厂 30 日、项目配 45 日取 45；清单写预计退押金日期、无顺延原因。 */
    @Test
    void autoTerminationDepositRefundDueIsLastApprovalDatePlusWait() {
        LocalDate today = LocalDate.now(LocalZone.ID);
        ContractRecord c = effectiveContractRecord("ctr_c6a", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(), change("k_c6a", "move_out", Map.of()));
        checkDocuments(c.id(), "k_c6a_dc");
        approveTerminationChain(ch.id());
        assertThat(terminationPort.depositDueDate).isEqualTo(today.plusDays(30));
        assertThat(depositDisposalOf("mch_1", c.id())).containsEntry("expectedRefundDate", today.plusDays(30).toString())
                .doesNotContainKey("refundDeferralReason");

        repo.refundWait.put("proj_1", new DepositRefundWaitSettingRecord("proj_1", 45, 30, 60));
        ContractRecord c2 = effectiveContractRecord("ctr_c6b", 500000L, Map.of());
        ContractChangeRecord ch2 = service.raiseChange(c2.id(), change("k_c6b", "early_termination", Map.of()));
        checkDocuments(c2.id(), "k_c6b_dc");
        approveTerminationChain(ch2.id());
        assertThat(terminationPort.depositDueDate).isEqualTo(today.plusDays(45));
        assertThat(depositDisposalOf("mch_1", c2.id())).containsEntry("expectedRefundDate", today.plusDays(45).toString())
                .doesNotContainKey("refundDeferralReason");
    }

    /** 到期结算终审落账：押金退还应付到期日＝最后审批完成日＋等待期，无顺延原因。 */
    @Test
    void expiryFinalApprovalDepositRefundDueIsApprovalDayPlusWait() {
        ContractRecord c = effectiveContractRecord("ctr_c6d", 500000L, Map.of());
        ContractSettlementRecord opened = service.settle(c.id(), settleReq("k_c6d", "expiry", null));
        checkDocuments(c.id(), "k_c6d_dc");
        approveExpiry(opened.id());
        LocalDate expected = LocalDate.now(LocalZone.ID).plusDays(30);
        assertThat(terminationPort.depositDueDate).isEqualTo(expected);
        assertThat(depositDisposalOf("mch_1", c.id())).containsEntry("expectedRefundDate", expected.toString())
                .doesNotContainKey("refundDeferralReason");
    }

    // ============ 第三轮裁决第 4 条「按押金够不够分流」：终审落账后 押金抵扣中 / 欠费追缴中 / 已结清（ENUM L252 退租结算状态） ============
    /** 台账项夹具（回调入参）：counterparty/merchant＝mch_1。 */
    private static com.complex.platform.finance.ArApItemRecord arApItem(String direction, String sourceType, String sourceId,
            long amountCent, long paidCent, String status) {
        return new com.complex.platform.finance.ArApItemRecord("arap_" + sourceId + "_" + paidCent, "ARAP-T", direction, "mch_1",
                sourceType, sourceId, amountCent, paidCent, amountCent - paidCent, "CNY", LocalDate.parse("2026-09-01"), null,
                status, Instant.now(), Instant.now(), "mch_1", "proj_1");
    }

    /** 押金足以抵扣全部欠费、有余额要退 → 押金抵扣中（押金退还应付已挂）；合同终止仍在终审落账时。 */
    @Test
    void finalApprovalWithRefundDueEntersDepositOffsetting() {
        repo.unpaidAr = 300000L;
        ContractRecord c = effectiveContractRecord("ctr_s4a", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(), change("k_s4a", "move_out", Map.of()));
        checkDocuments(c.id(), "k_s4a_dc");
        approveTerminationChain(ch.id());
        assertThat(terminationPort.depositRefundCent).isEqualTo(200000L);
        assertThat(repo.findSettlementById("cst_" + sha48(ch.id())).orElseThrow().status()).isEqualTo("deposit_offsetting");
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("terminated");
    }

    /** 押金不足抵扣（到期终止终审路径）→ 欠费追缴中，不挂押金退还应付；合同到期仍在终审落账时。 */
    @Test
    void expiryFinalApprovalWithDepositShortEntersArrearsCollecting() {
        repo.unpaidAr = 800000L;
        ContractRecord c = effectiveContractRecord("ctr_s4b", 500000L, Map.of());
        ContractSettlementRecord opened = service.settle(c.id(), settleReq("k_s4b", "expiry", null));
        checkDocuments(c.id(), "k_s4b_dc");
        assertThat(approveExpiry(opened.id()).status()).isEqualTo("arrears_collecting");
        assertThat(terminationPort.depositCalls).isZero();
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("expired");
    }

    /** 押金正好抵完、无退无欠 → 直接已结清。 */
    @Test
    void finalApprovalDepositExactlyCoversArrearsSettlesDirectly() {
        repo.unpaidAr = 500000L;
        ContractRecord c = effectiveContractRecord("ctr_s4c", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(), change("k_s4c", "move_out", Map.of()));
        checkDocuments(c.id(), "k_s4c_dc");
        approveTerminationChain(ch.id());
        assertThat(terminationPort.depositCalls).isZero();
        assertThat(repo.findSettlementById("cst_" + sha48(ch.id())).orElseThrow().status()).isEqualTo("settled");
        assertThat(repo.findContractById(c.id()).orElseThrow().lifecycleStatus()).isEqualTo("terminated");
    }

    /** 押金退还应付付清（settleArApItem 回调，source_type=deposit_refund、来源＝结算单）→ 押金抵扣中转已结清；部分付款、别的结算单的退款不推进。 */
    @Test
    void depositRefundPaidInFullMovesOffsettingToSettled() {
        ContractRecord c = effectiveContractRecord("ctr_s4d", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(), change("k_s4d", "move_out", Map.of()));
        checkDocuments(c.id(), "k_s4d_dc");
        approveTerminationChain(ch.id());
        String sid = "cst_" + sha48(ch.id());
        assertThat(repo.findSettlementById(sid).orElseThrow().status()).isEqualTo("deposit_offsetting");
        service.onArApItemPaid(arApItem("payable", "deposit_refund", sid, 500000L, 200000L, "partial_paid"));
        service.onArApItemPaid(arApItem("payable", "deposit_refund", "cst_other", 500000L, 500000L, "settled"));
        assertThat(repo.findSettlementById(sid).orElseThrow().status()).isEqualTo("deposit_offsetting");
        service.onArApItemPaid(arApItem("payable", "deposit_refund", sid, 500000L, 500000L, "settled"));
        assertThat(repo.findSettlementById(sid).orElseThrow().status()).isEqualTo("settled");
    }

    /**
     * 押金不足（第四轮裁决第 3 条「终审落账时把被押金覆盖的应收逐笔以「押金抵扣」销账……「欠费追缴中」的收齐判定随之改为看台账未收额」）：
     * 未清应收 800000、押金 500000 → 押金抵扣销账 500000、台账剩 300000 未收 → 欠费追缴中；按退租日所在月同口径看台账未收额，
     * 部分收不转，全部收齐（未收额为零）才转已结清（move_out 无违约金；违约金并入见 earlyTerminationPenaltyIsOffsetByDepositBeforeRefund）。
     */
    @Test
    void arrearsCollectingSettlesOnlyWhenLedgerUnpaidFullyCollected() {
        repo.unpaidAr = 800000L;
        ContractRecord c = effectiveContractRecord("ctr_s4e", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(), change("k_s4e", "move_out", Map.of("moveOutDate", "2026-08-15")));
        checkDocuments(c.id(), "k_s4e_dc");
        approveTerminationChain(ch.id());
        String sid = "cst_" + sha48(ch.id());
        assertThat(repo.findSettlementById(sid).orElseThrow().status()).isEqualTo("arrears_collecting");
        assertThat(repo.unpaidOf("arap_unpaid_ctr_s4e")).isEqualTo(300000L);   // 押金覆盖的 500000 已在台账销账
        repo.unpaidArById.put(c.id(), 100000L);   // 再收回 200000，仍欠 100000
        repo.unpaidThrough.clear();
        service.onArApItemPaid(arApItem("receivable", "contract", c.id() + "|2026-08|rent", 800000L, 700000L, "partial_paid"));
        assertThat(repo.findSettlementById(sid).orElseThrow().status()).isEqualTo("arrears_collecting");
        assertThat(repo.unpaidThrough).isNotEmpty().containsOnly(YearMonth.of(2026, 8));
        repo.unpaidArById.put(c.id(), 0L);   // 全部收齐
        service.onArApItemPaid(arApItem("receivable", "contract", c.id() + "|2026-08|rent", 800000L, 800000L, "settled"));
        assertThat(repo.findSettlementById(sid).orElseThrow().status()).isEqualTo("settled");
    }

    /**
     * 第四轮裁决第 3 条（财务结算 PRD L957「终止结算既有多级审批全部通过后，系统按清单自动落账，使相应应收未付额与押金余额等额减少并可对账」）＋第 1 条
     * 「押金先抵欠费加违约金，剩余才退」：押金 500000、截止月前未清应收 300000、合同违约金 100000 → 先挂违约金应收，再按「未清应收 → 违约金」顺序逐笔
     * 押金抵扣销账（300000、100000），押金退还＝扣减后余额 100000、押金余额归零不二次扣减；押金处置项回写每笔销账的台账项 id 与金额；
     * 押金抵扣中，终审事件重放不重复销账，退还付清 → 已结清。
     */
    @Test
    void depositOffsetWritesOffArrearsAndPenaltyThenRefundsRemainder() {
        repo.unpaidAr = 300000L;
        terminationPort.depositBalance = 500000L;
        ContractRecord c = effectiveContractRecord("ctr_off1", 500000L, Map.of("amountCent", 100000));
        ContractChangeRecord ch = service.raiseChange(c.id(), change("k_off1", "early_termination", Map.of()));
        checkDocuments(c.id(), "k_off1_dc");
        approveTerminationChain(ch.id());
        String sid = "cst_" + sha48(ch.id());
        assertThat(terminationPort.offsetCalls).hasSize(1);
        assertThat(terminationPort.offsetCalls.get(0)).containsExactly(sid, List.of("arap_unpaid_ctr_off1", "arap_pen_" + sid), 400000L);
        assertThat(repo.unpaidOf("arap_unpaid_ctr_off1")).isZero();
        assertThat(repo.unpaidOf("arap_pen_" + sid)).isZero();
        assertThat(terminationPort.depositRefundCent).isEqualTo(100000L);
        assertThat(terminationPort.depositBalance).isZero();
        assertThat(repo.findSettlementById(sid).orElseThrow().status()).isEqualTo("deposit_offsetting");
        assertThat(depositDisposalOf("mch_1", c.id()).get("offsets")).isEqualTo(List.of(
                Map.of("arApItemId", "arap_unpaid_ctr_off1", "amountCent", 300000L),
                Map.of("arApItemId", "arap_pen_" + sid, "amountCent", 100000L)));
        approveTerminationChain(ch.id());   // 终审事件重放
        assertThat(terminationPort.offsetCalls).hasSize(1);
        assertThat(terminationPort.depositCalls).isEqualTo(1);
        service.onArApItemPaid(arApItem("payable", "deposit_refund", sid, 100000L, 100000L, "settled"));
        assertThat(repo.findSettlementById(sid).orElseThrow().status()).isEqualTo("settled");
    }

    /** 到期终止复核单终审落账同一口径：押金 500000 抵截止月前未清应收 200000（逐笔销账），退还 300000，押金抵扣中。 */
    @Test
    void expiryFinalApprovalOffsetsArrearsWithDeposit() {
        repo.unpaidAr = 200000L;
        ContractRecord c = effectiveContractRecord("ctr_off2", 500000L, Map.of());
        ContractSettlementRecord opened = service.settle(c.id(), settleReq("k_off2", "expiry", null));
        service.checkSettlementDocuments(opened.id(), new ContractResolveDisputeRequest("k_off2_dc", null));
        assertThat(approveExpiry(opened.id()).status()).isEqualTo("deposit_offsetting");
        assertThat(terminationPort.offsetCalls).hasSize(1);
        assertThat(terminationPort.offsetCalls.get(0)).containsExactly(opened.id(), List.of("arap_unpaid_ctr_off2"), 200000L);
        assertThat(repo.unpaidOf("arap_unpaid_ctr_off2")).isZero();
        assertThat(terminationPort.depositRefundCent).isEqualTo(300000L);
    }

    /**
     * 落账后（押金抵扣中/欠费追缴中）＝已落账（控制席第三轮约定，由 BSM L1352「商户对退租结算金额的异议在退租审批流程中提出并解决」
     * 「同一份合同的退租结算只算一次、不重复出账」推出）：商户不可再提异议、齐单核对 409、到期终止复核单驳回/重放不作废不重复落账，
     * 同合同另一张到期结算单终审即作废。
     */
    @Test
    void postedSettlementIsNotDisputableNotVoidedAndNotReposted() {
        when(authz.currentMerchantScope()).thenReturn(Optional.of("mch_1"));
        com.complex.platform.merchantportal.MerchantPortalService portal = new com.complex.platform.merchantportal.MerchantPortalService(
                repo, authz, audit, new com.complex.platform.common.idempotency.IdempotencyService(
                        new com.complex.platform.support.InMemoryIdempotencyRepository(),
                        new com.fasterxml.jackson.databind.ObjectMapper()));
        ContractRecord c = effectiveContractRecord("ctr_s4f", 500000L, Map.of());
        ContractSettlementRecord first = service.settle(c.id(), settleReq("k_s4f1", "expiry", null));
        ContractSettlementRecord second = service.settle(c.id(), settleReq("k_s4f2", "expiry", null));
        service.checkSettlementDocuments(first.id(), new ContractResolveDisputeRequest("k_s4f1_dc", null));
        assertThat(approveExpiry(first.id()).status()).isEqualTo("deposit_offsetting");
        assertThatThrownBy(() -> portal.submitSettlementDispute(first.id(),
                new com.complex.platform.merchantportal.dto.MerchantSettlementDisputeRequest("k_s4f_d", null, "押金退少了")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SETTLEMENT_NOT_DISPUTABLE"));
        assertThatThrownBy(() -> service.checkSettlementDocuments(first.id(), new ContractResolveDisputeRequest("k_s4f1_dc2", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CONTRACT_SETTLEMENT_NOT_OPEN"));
        assertThat(decideExpiry(first.id(), "rejected").status()).isEqualTo("deposit_offsetting");
        assertThat(approveExpiry(first.id()).status()).isEqualTo("deposit_offsetting");
        assertThat(approveExpiry(second.id()).status()).isEqualTo("voided");
        assertThat(terminationPort.depositCalls).isEqualTo(1);
    }

    /**
     * 并发（控制席第三轮约定「商户异议/不接受裁决/主管裁决与终审落账之间加行锁」）：主管裁决与两条终审落账路径都先锁合同行再读结算单状态——
     * 等锁期间结算单已被先提交的事务改掉（夹具在加锁读时改状态），按锁后状态判定，不互相覆盖。
     */
    @Test
    void resolveAndFinalApprovalReadSettlementAfterContractRowLock() {
        ContractRecord c = effectiveContractRecord("ctr_s4g", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(), change("k_s4g", "move_out", Map.of()));
        String sid = "cst_" + sha48(ch.id());
        ContractSettlementRecord open = repo.findSettlementById(sid).orElseThrow();
        repo.saveSettlement(open.withDispute("disputed", open.settlementItems(), "商户：违约金多算", null, null, null, Instant.now()));
        repo.onLock = () -> repo.saveSettlement(open.withDispute("pending_settlement", open.settlementItems(), "商户：违约金多算",
                "另一主管：按清单结算", "usr_other", Instant.now(), Instant.now()));
        assertThatThrownBy(() -> service.resolveDispute(sid, new ContractResolveDisputeRequest("k_s4g_r", "按清单结算")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CONTRACT_SETTLEMENT_NOT_DISPUTED"));
        assertThat(repo.findSettlementById(sid).orElseThrow().disputeResolution()).isEqualTo("另一主管：按清单结算");

        ContractSettlementRecord checked = checkDocuments(c.id(), "k_s4g_dc");
        repo.onLock = () -> repo.saveSettlement(checked.withDispute("disputed", checked.settlementItems(), "商户：押金退少了",
                checked.disputeResolution(), checked.resolvedBy(), checked.resolvedAt(), Instant.now()));
        assertThatThrownBy(() -> approveTerminationChain(ch.id()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CONTRACT_SETTLEMENT_DISPUTED"));
        assertThat(repo.findSettlementById(sid).orElseThrow().status()).isEqualTo("disputed");
        assertThat(terminationPort.depositCalls).isZero();

        ContractRecord exp = effectiveContractRecord("ctr_s4h", 500000L, Map.of());
        ContractSettlementRecord opened = service.settle(exp.id(), settleReq("k_s4h", "expiry", null));
        ContractSettlementRecord expChecked = checkDocuments(exp.id(), "k_s4h_dc");
        repo.onLock = () -> repo.saveSettlement(expChecked.withDispute("disputed", expChecked.settlementItems(), "商户：押金退少了",
                null, null, null, Instant.now()));
        assertThatThrownBy(() -> approveExpiry(opened.id()))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("CONTRACT_SETTLEMENT_DISPUTED"));
        assertThat(terminationPort.depositCalls).isZero();
    }

    /**
     * BSM L1347「只有清退时押金才能抵扣欠费与违约金」「清退按提前终止办理」、第四轮裁决第 1 条「押金先抵欠费加违约金，剩余才退；不够的进「欠费追缴中」，
     * 违约金和欠费都收齐才「已结清」」：押金 500000、未清应收 300000、合同违约金 300000 → 未清应收全额、违约金 200000 以押金抵扣销账，不挂押金退还应付、
     * 违约金应收台账剩 100000 → 欠费追缴中；违约金部分收不转，台账未收额为零才转已结清。押金 0、无欠费、违约金 &gt; 0 同样进欠费追缴中。
     */
    @Test
    void earlyTerminationPenaltyIsOffsetByDepositBeforeRefund() {
        repo.unpaidAr = 300000L;
        ContractRecord c = effectiveContractRecord("ctr_pen1", 500000L, Map.of("amountCent", 300000));
        ContractChangeRecord ch = service.raiseChange(c.id(), change("k_pen1", "early_termination", Map.of()));
        checkDocuments(c.id(), "k_pen1_dc");
        approveTerminationChain(ch.id());
        String sid = "cst_" + sha48(ch.id());
        assertThat(terminationPort.depositCalls).isZero();
        assertThat(terminationPort.penaltyCent).isEqualTo(300000L);
        ContractSettlementRecord st = repo.findSettlementById(sid).orElseThrow();
        assertThat(st.status()).isEqualTo("arrears_collecting");
        assertThat(st.settlementItems()).anySatisfy(i -> assertThat(i).containsEntry("item", "deposit_disposal")
                .containsEntry("offsetCent", 500000L).containsEntry("merchantStillOwesCent", 100000L));
        assertThat(repo.unpaidOf("arap_unpaid_ctr_pen1")).isZero();
        assertThat(repo.unpaidOf("arap_pen_" + sid)).isEqualTo(100000L);
        service.onArApItemPaid(arApItem("receivable", "contract", c.id() + "|2026-08|rent", 300000L, 300000L, "settled"));
        assertThat(repo.findSettlementById(sid).orElseThrow().status()).isEqualTo("arrears_collecting");
        repo.unpaidPenaltyBySettlement.put(sid, 50000L);   // 违约金再收回 50000：部分收不转
        service.onArApItemPaid(arApItem("receivable", "termination_penalty", sid, 300000L, 250000L, "partial_paid"));
        assertThat(repo.findSettlementById(sid).orElseThrow().status()).isEqualTo("arrears_collecting");
        repo.unpaidPenaltyBySettlement.put(sid, 0L);
        service.onArApItemPaid(arApItem("receivable", "termination_penalty", sid, 300000L, 300000L, "settled"));
        assertThat(repo.findSettlementById(sid).orElseThrow().status()).isEqualTo("settled");

        repo.unpaidAr = 0L;
        ContractRecord z = effectiveContractRecord("ctr_pen2", 0L, Map.of("amountCent", 200000));
        ContractChangeRecord zc = service.raiseChange(z.id(), change("k_pen2", "early_termination", Map.of()));
        checkDocuments(z.id(), "k_pen2_dc");
        approveTerminationChain(zc.id());
        assertThat(repo.findSettlementById("cst_" + sha48(zc.id())).orElseThrow().status()).isEqualTo("arrears_collecting");
    }

    /**
     * 收款回调与终审落账并发（第三轮控制席约定「并发不互相覆盖」）：回调先锁合同行再重读结算单——等锁期间落账事务把单提交成「欠费追缴中」
     * 且差额已收齐（夹具在加锁读时改状态）→ 本次回调按锁后状态推进已结清，不会永久停在欠费追缴中。
     */
    @Test
    void arrearsCallbackLocksContractRowAndRereadsSettlement() {
        ContractRecord c = effectiveContractRecord("ctr_lk1", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(), change("k_lk1", "move_out", Map.of()));
        String sid = "cst_" + sha48(ch.id());
        ContractSettlementRecord open = repo.findSettlementById(sid).orElseThrow();
        List<Map<String, Object>> posted = List.of(
                Map.of("item", "unpaid_receivable_summary", "amountCent", 800000L, "throughMonth", "2026-08"),
                Map.of("item", "deposit_disposal", "offsetCent", 500000L, "merchantStillOwesCent", 300000L));
        repo.unpaidAr = 0L;   // 已全部收齐：台账未收额为零
        repo.onLock = () -> repo.saveSettlement(open.withStatus("arrears_collecting", posted, Instant.now()));
        service.onArApItemPaid(arApItem("receivable", "contract", c.id() + "|2026-08|rent", 300000L, 300000L, "settled"));
        assertThat(repo.findSettlementById(sid).orElseThrow().status()).isEqualTo("settled");
    }

    /** 齐单核对与商户异议并发：先锁合同行再重读——等锁期间商户异议已提交 → 409 CONTRACT_SETTLEMENT_NOT_OPEN，异议不被冲回待结算。 */
    @Test
    void documentsCheckReadsSettlementAfterContractRowLock() {
        ContractRecord c = effectiveContractRecord("ctr_dck1", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(), change("k_dck1", "move_out", Map.of()));
        String sid = "cst_" + sha48(ch.id());
        ContractSettlementRecord open = repo.findSettlementById(sid).orElseThrow();
        repo.onLock = () -> repo.saveSettlement(open.withDispute("disputed", open.settlementItems(), "商户：押金退少了",
                null, null, null, Instant.now()));
        assertThatThrownBy(() -> service.checkSettlementDocuments(sid, new ContractResolveDisputeRequest("k_dck1_dc", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CONTRACT_SETTLEMENT_NOT_OPEN"));
        ContractSettlementRecord after = repo.findSettlementById(sid).orElseThrow();
        assertThat(after.status()).isEqualTo("disputed");
        assertThat(after.disputeReason()).isEqualTo("商户：押金退少了");
        assertThat(after.documentsCheckedAt()).isNull();
    }

    /** OpenAPI 声明 x-audit-event-failure: contract.settlement_documents_check_failed：缺单 409 也留失败审计。 */
    @Test
    void documentsCheckFailureRecordsFailedAudit() {
        ContractRecord c = effectiveContractRecord("ctr_dck2", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(), change("k_dck2", "move_out", Map.of()));
        String sid = "cst_" + sha48(ch.id());
        repo.missingDocuments = List.of("2026-08:rent");
        assertThatThrownBy(() -> service.checkSettlementDocuments(sid, new ContractResolveDisputeRequest("k_dck2_dc", null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTRACT_SETTLEMENT_DOCUMENTS_INCOMPLETE"));
        verify(audit).recordFailure(eq("contract.settlement_documents_check_failed"), eq("contract.settlement.manage"),
                eq("contract_settlement"), eq(sid), any(), eq("k_dck2_dc"), eq("CONTRACT_SETTLEMENT_DOCUMENTS_INCOMPLETE"));
    }

    /** OpenAPI 声明 x-idempotency-required：同一幂等键重放返回第一次的处理结果、不再执行（核对人/时间不被覆盖）。 */
    @Test
    void documentsCheckSameKeyReplaysFirstResult() {
        ContractRecord c = effectiveContractRecord("ctr_dck3", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(), change("k_dck3", "move_out", Map.of()));
        String sid = "cst_" + sha48(ch.id());
        service.checkSettlementDocuments(sid, new ContractResolveDisputeRequest("k_dck3_dc", "已核对"));
        when(authz.currentActor()).thenReturn(
                new AccessActor("usr_other", "other_user", "platform_admin", "org_1", null, "active"));
        ContractSettlementRecord replay = service.checkSettlementDocuments(sid, new ContractResolveDisputeRequest("k_dck3_dc", "已核对"));
        assertThat(replay.documentsCheckedBy()).isEqualTo("usr_admin");
        assertThat(repo.findSettlementById(sid).orElseThrow().documentsCheckedBy()).isEqualTo("usr_admin");
    }

    /**
     * 驳回作废与商户异议并发：退租变更驳回、到期终止复核单驳回都先锁合同行再重读——等锁期间商户异议已提交，作废的是锁后那张（异议理由保留），
     * 异议事务不会再把已作废的单写回「有争议」。
     */
    @Test
    void rejectionVoidsSettlementReadAfterContractRowLock() {
        ContractRecord c = effectiveContractRecord("ctr_rjl1", 500000L, Map.of());
        ContractChangeRecord ch = service.raiseChange(c.id(), change("k_rjl1", "move_out", Map.of()));
        String sid = "cst_" + sha48(ch.id());
        ContractSettlementRecord open = repo.findSettlementById(sid).orElseThrow();
        repo.onLock = () -> repo.saveSettlement(open.withDispute("disputed", open.settlementItems(), "商户：违约金多算",
                null, null, null, Instant.now()));
        decideChange(ch.id(), "rejected");
        ContractSettlementRecord voided = repo.findSettlementById(sid).orElseThrow();
        assertThat(voided.status()).isEqualTo("voided");
        assertThat(voided.disputeReason()).isEqualTo("商户：违约金多算");

        ContractRecord exp = effectiveContractRecord("ctr_rjl2", 500000L, Map.of());
        ContractSettlementRecord opened = service.settle(exp.id(), settleReq("k_rjl2", "expiry", null));
        repo.onLock = () -> repo.saveSettlement(opened.withDispute("disputed", opened.settlementItems(), "商户：押金退少了",
                null, null, null, Instant.now()));
        ContractSettlementRecord expVoided = decideExpiry(opened.id(), "rejected");
        assertThat(expVoided.status()).isEqualTo("voided");
        assertThat(expVoided.disputeReason()).isEqualTo("商户：押金退少了");
    }

    /**
     * web 客户端把 rent_terms 嵌套键蛇形化（adminClient.mapKeys：base_rent_cent／rent_mode／rent_billing_mode）：起草把顶层蛇形键归一回驼峰——
     * BSM L1347「押金金额 ＝ 收取月数 × 该商户签约时的月租金」：押金＝3×月租，生效后首期租金按月租出账。
     */
    @Test
    void draftWithSnakeCaseRentTermsComputesDepositAndBillsRent() {
        ContractRecord d = service.createDraft(new ContractDraftRequest("k_snake", "mch_snake", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"),
                Map.of("rent_mode", "monthly_fixed", "rent_billing_mode", "fixed_only", "base_rent_cent", 100000),
                "fixed_ratio", "none", null, Map.of(), List.of(), List.of(), null, null));
        assertThat(d.depositCent()).isEqualTo(300000L);
        assertThat(d.rentTerms()).containsEntry("baseRentCent", 100000).containsEntry("rentMode", "monthly_fixed")
                .doesNotContainKeys("base_rent_cent", "rent_mode");
        submit(service, d, "k_snake_sub");
        approveContract(d.id());
        ContractRecord eff = service.activate(d.id(), new ContractActivateRequest("k_snake_act", List.of("sp_snake")));
        assertThat(eff.depositCent()).isEqualTo(300000L);
        assertThat(repo.rentBills).anySatisfy(b -> {
            assertThat(b.contractId()).isEqualTo(d.id());
            assertThat(b.period()).isEqualTo("2026-07");
            assertThat(b.feeType()).isEqualTo("rent");
            assertThat(b.amountCent()).isEqualTo(100000L);
        });
    }

    // ============ 第四轮裁决第 4 条「合同草稿可编辑补录」 ============
    private static ContractDraftRentTermsRequest rentTermsEdit(String key, Map<String, Object> rentTerms, Long depositCent) {
        return new ContractDraftRentTermsRequest(key, rentTerms, depositCent);
    }

    /**
     * 第四轮裁决第 4 条「签约起草后在草稿上补录租金条款再提交；非纯提成合同月租为 0 的不得提交审批」：没有月租的草稿提交 → 400「请先在草稿上补录月租」、
     * 不开复核单；按 web 真实形状（adminClient.mapKeys 把 rent_terms 嵌套键蛇形化）补录月租 → 归一为驼峰、经营范围原样保留、押金＝收取月数×月租
     * （BSM L1347「押金金额 ＝ 收取月数 × 该商户签约时的月租金」，复用起草公式）、留痕 contract.draft_rent_terms_updated；再提交 → 待确认。
     */
    @Test
    void draftWithoutMonthlyRentCannotSubmitUntilRentFilledOnDraft() {
        ContractRecord d = service.createDraft(new ContractDraftRequest("k_r4_norent", "mch_r4", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of(), "fixed_ratio", "none", null,
                Map.of(), List.of(), List.of(), null, "餐饮"));
        assertThatThrownBy(() -> submit(service, d, "k_r4_sub0"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("CONTRACT_RENT_REQUIRED");
                    assertThat(e.getMessage()).isEqualTo("请先在草稿上补录月租");
                });
        assertThat(evaluated).isEmpty();
        assertThat(repo.findContractById(d.id()).orElseThrow().lifecycleStatus()).isEqualTo("drafting");

        repo.depositMonths = new DepositMonthsSettingRecord(4, 3, 6);
        ContractRecord edited = service.editDraftRentTerms(d.id(), rentTermsEdit("k_r4_edit",
                Map.of("rent_mode", "monthly_fixed", "rent_billing_mode", "fixed_only", "base_rent_cent", 120000), null));
        assertThat(edited.rentTerms()).containsEntry("baseRentCent", 120000).containsEntry("rentMode", "monthly_fixed")
                .containsEntry("rentBillingMode", "fixed_only").containsEntry("businessScope", "餐饮")
                .doesNotContainKeys("base_rent_cent", "rent_mode", "rent_billing_mode");
        assertThat(edited.depositCent()).isEqualTo(480000L);
        assertThat(repo.findContractById(d.id()).orElseThrow().depositCent()).isEqualTo(480000L);
        verify(audit).recordSuccess(eq("contract.draft_rent_terms_updated"), eq("contract.draft.manage"), eq("contract"),
                eq(d.id()), isNull(), eq("k_r4_edit"), any());
        assertThat(submit(service, d, "k_r4_sub1").lifecycleStatus()).isEqualTo("pending_confirmation");
    }

    /**
     * 据第四轮裁决第 4 条「签约起草后在草稿上补录租金条款再提交」：仅草拟中（含驳回退回起草）可编辑——待确认 → 409 CONTRACT_NOT_DRAFTING、条款不变、
     * 留失败痕；整单驳回退回起草后可再补录，押金随月租按公式重算。
     */
    @Test
    void draftRentTermsEditableOnlyWhileDraftingIncludingAfterRejection() {
        ContractRecord c = newDraft();
        submit(service, c, "k_r4_nd_sub");
        assertThatThrownBy(() -> service.editDraftRentTerms(c.id(),
                rentTermsEdit("k_r4_nd_edit", Map.of("baseRentCent", 150000), null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CONTRACT_NOT_DRAFTING"));
        assertThat(repo.findContractById(c.id()).orElseThrow().rentTerms()).containsEntry("baseRentCent", 100000);
        verify(audit).recordFailure(eq("contract.draft_rent_terms_update_failed"), eq("contract.draft.manage"), eq("contract"),
                eq(c.id()), isNull(), eq("k_r4_nd_edit"), eq("CONTRACT_NOT_DRAFTING"));

        decideContract(service, c.id(), "rejected");
        ContractRecord edited = service.editDraftRentTerms(c.id(),
                rentTermsEdit("k_r4_nd_edit2", Map.of("baseRentCent", 150000), null));
        assertThat(edited.lifecycleStatus()).isEqualTo("drafting");
        assertThat(edited.rentTerms()).containsEntry("baseRentCent", 150000);
        assertThat(edited.depositCent()).isEqualTo(450000L);
    }

    /**
     * 权限沿用起草合同（contract.draft.manage + 平台口径 + 项目范围）：缺权限码 → 403；商户账号 → 403 PLATFORM_SCOPE_REQUIRED；
     * 草稿所属项目不在本人可触及范围 → 403 CROSS_PROJECT_FORBIDDEN；被拒均不改条款。可触及后正常补录。
     */
    @Test
    void draftRentTermsEditRequiresDraftPermissionPlatformScopeAndProjectReach() {
        ContractRecord d = service.createDraft(new ContractDraftRequest("k_r4_scope", "mch_r4s", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of(), "fixed_ratio", "none", null,
                Map.of(), List.of(), List.of(), "proj_a", null));
        Map<String, Object> rent = Map.of("baseRentCent", 120000);
        org.mockito.Mockito.doThrow(ApiException.forbidden("PERMISSION_DENIED", "无权限"))
                .when(authz).requirePermission("contract.draft.manage");
        assertThatThrownBy(() -> service.editDraftRentTerms(d.id(), rentTermsEdit("k_r4_s1", rent, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
        org.mockito.Mockito.doNothing().when(authz).requirePermission("contract.draft.manage");
        org.mockito.Mockito.doThrow(ApiException.forbidden("PLATFORM_SCOPE_REQUIRED", "商户账号无权"))
                .when(authz).requirePlatformScope();
        assertThatThrownBy(() -> service.editDraftRentTerms(d.id(), rentTermsEdit("k_r4_s2", rent, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PLATFORM_SCOPE_REQUIRED"));
        org.mockito.Mockito.doNothing().when(authz).requirePlatformScope();
        when(authz.isPlatformAdmin(any())).thenReturn(false);
        when(authz.canReachProject(any(), eq("proj_a"), eq("contract.draft.manage"))).thenReturn(false);
        assertThatThrownBy(() -> service.editDraftRentTerms(d.id(), rentTermsEdit("k_r4_s3", rent, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
        assertThat(repo.findContractById(d.id()).orElseThrow().rentTerms()).doesNotContainKey("baseRentCent");

        when(authz.canReachProject(any(), eq("proj_a"), eq("contract.draft.manage"))).thenReturn(true);
        assertThat(service.editDraftRentTerms(d.id(), rentTermsEdit("k_r4_s4", rent, null)).rentTerms())
                .containsEntry("baseRentCent", 120000);
    }

    /**
     * 第三轮裁决第 1 条「纯提成合同的押金由招商按合同约定金额直接录入，不走月数公式，但不得为零」：草稿编辑改约定押金 → 原样落押金；0 → 400、押金不变；
     * 押金为 0 的纯提成草稿提交 → 400、不开复核单。只开放租金条款与纯提成约定押金：有月租合同带与公式不符的押金 → 400 CONTRACT_DEPOSIT_FORMULA_MISMATCH；
     * 带经营范围键 → 400。
     */
    @Test
    void turnoverOnlyDraftAgreedDepositEditableAndZeroRejected() {
        ContractRecord d = service.createDraft(new ContractDraftRequest("k_r4_tov", "mch_r4t", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("rentBillingMode", "turnover_only"),
                "fixed_ratio", "none", 500000L, Map.of(), List.of(), List.of(), null, null));
        ContractRecord edited = service.editDraftRentTerms(d.id(),
                rentTermsEdit("k_r4_tov_e1", Map.of("rent_billing_mode", "turnover_only"), 800000L));
        assertThat(edited.depositCent()).isEqualTo(800000L);
        assertThatThrownBy(() -> service.editDraftRentTerms(d.id(),
                rentTermsEdit("k_r4_tov_e0", Map.of("rent_billing_mode", "turnover_only"), 0L)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThat(repo.findContractById(d.id()).orElseThrow().depositCent()).isEqualTo(800000L);

        Instant now = Instant.now();
        ContractRecord zero = repo.saveContract(new ContractRecord("ctr_r4_tov_zero", "mch_r4t", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("rentBillingMode", "turnover_only"),
                "fixed_ratio", "none", 0L, Map.of(), List.of(), null, null, List.of(), "drafting", null, null, null, null, now, now));
        assertThatThrownBy(() -> submit(service, zero, "k_r4_tov_zero_sub"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThat(evaluated).isEmpty();
        assertThat(submit(service, d, "k_r4_tov_sub").lifecycleStatus()).isEqualTo("pending_confirmation");

        ContractRecord f = newDraft();
        assertThatThrownBy(() -> service.editDraftRentTerms(f.id(), rentTermsEdit("k_r4_mis", Map.of("baseRentCent", 120000), 1L)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CONTRACT_DEPOSIT_FORMULA_MISMATCH"));
        assertThatThrownBy(() -> service.editDraftRentTerms(f.id(),
                rentTermsEdit("k_r4_scope_key", Map.of("allowed_categories", List.of("bar")), null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThat(repo.findContractById(f.id()).orElseThrow().rentTerms()).containsEntry("baseRentCent", 100000)
                .doesNotContainKey("allowedCategories");
    }

    /**
     * 草稿编辑只开放租金条款：系统维护的变更链键（parentContractId 等，由变更终审派生子合同时写入，页面从不发送）按 web 真实形状（mapKeys 蛇形化）
     * 带入 → 400、条款不变——否则变更作废重排与退租押金抵扣沿链会触及链上别的合同台账。
     */
    @Test
    void draftRentTermsEditRejectsSystemMaintainedChainKeys() {
        ContractRecord f = newDraft();
        assertThatThrownBy(() -> service.editDraftRentTerms(f.id(), rentTermsEdit("k_r4_parent",
                Map.of("base_rent_cent", 120000, "parent_contract_id", "ctr_other_merchant"), null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThat(repo.findContractById(f.id()).orElseThrow().rentTerms()).containsEntry("baseRentCent", 100000)
                .doesNotContainKey("parentContractId");
    }

    /**
     * 变更链系统键（parentContractId／amendmentType／increaseFrom／depositAdjustment／scopeGrants）只由变更/续租终审写入：直接调 API 在租金调整、
     * 经营范围变更载荷里带入（驼峰或蛇形）→ 400、不留变更；起草条款按 web 真实形状（mapKeys 蛇形化）带 parent_contract_id／amendment_type／
     * deposit_adjustment → 400、不建草稿——否则租金调整作废重排与退租押金抵扣沿伪造的链触及同商户别的合同台账。
     */
    @Test
    void rentChangeScopeChangeAndDraftRejectChainKeys() {
        ContractRecord c = effectiveContract();
        int n = 0;
        for (String type : List.of("rent_change", "business_scope_change")) {
            for (Map<String, Object> bad : List.<Map<String, Object>>of(Map.of("baseRentCent", 150000, "parentContractId", "ctr_other"),
                    Map.of("baseRentCent", 150000, "amendment_type", "area_change"), Map.of("baseRentCent", 150000, "scopeGrants", List.of()))) {
                String key = "k_chainkey_" + (n++);
                assertThatThrownBy(() -> service.raiseChange(c.id(), change(key, type, bad))).as("%s %s", type, bad)
                        .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
            }
        }
        assertThat(repo.listChangesByContract(c.id())).isEmpty();
        int contractsBefore = repo.contracts.size();
        for (String chainKey : List.of("parent_contract_id", "amendment_type", "deposit_adjustment")) {
            assertThatThrownBy(() -> service.createDraft(new ContractDraftRequest("k_chainkey_draft_" + chainKey, "mch_1", "subj_1", null,
                    "subj_1", LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("base_rent_cent", 100000, chainKey, "x"),
                    "fixed_ratio", "none", null, Map.of(), List.of(), List.of(), null, "餐饮"))).as(chainKey)
                    .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        }
        assertThat(repo.contracts).hasSize(contractsBefore);
    }

    /** 变更链只沿同商户父合同上溯（合法子合同由变更终审复制父合同商户）：rent_terms.parentContractId 指向别的商户的合同 → 链只有自身。 */
    @Test
    void contractChainDoesNotClimbIntoAnotherMerchantsContract() {
        Instant now = Instant.now();
        ContractRecord other = repo.saveContract(new ContractRecord("ctr_chain_other", "mch_other", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("baseRentCent", 100000),
                "fixed_ratio", "none", 300000L, Map.of(), List.of(), null, null, List.of(), "effective", null, null, null, null, now, now));
        ContractRecord forged = repo.saveContract(new ContractRecord("ctr_chain_forged", "mch_1", "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("baseRentCent", 100000, "parentContractId", other.id()),
                "fixed_ratio", "none", 300000L, Map.of(), List.of(), null, null, List.of(), "effective", null, null, null, null, now, now));
        assertThat(repo.contractChainIds(forged)).containsExactly(forged.id());
    }

    private static final class InMemoryContractRepository implements ContractRepository {
        // 提前终止违约金应收的未收额（按结算单 id；source_type=termination_penalty）。
        final Map<String, Long> unpaidPenaltyBySettlement = new java.util.HashMap<>();
        // HP-C03：押金收取月数（全平台 'global' 行），出厂 3 个月、上下限未配置。
        DepositMonthsSettingRecord depositMonths = new DepositMonthsSettingRecord(3, null, null);

        @Override
        public DepositMonthsSettingRecord depositMonthsSetting() {
            return depositMonths;
        }

        @Override
        public void saveDepositMonthsSetting(DepositMonthsSettingRecord setting, String actorId) {
            depositMonths = setting;
        }

        // HP-C06：押金退还等待期（项目行；无行回落 '__global__' 出厂 30 日、上下限未配置）。
        final Map<String, DepositRefundWaitSettingRecord> refundWait = new java.util.HashMap<>();

        @Override
        public DepositRefundWaitSettingRecord depositRefundWaitSetting(String projectId) {
            return refundWait.getOrDefault(projectId, new DepositRefundWaitSettingRecord("__global__", 30, null, null));
        }

        @Override
        public void saveDepositRefundWaitSetting(DepositRefundWaitSettingRecord setting, String actorId) {
            refundWait.put(setting.projectId(), setting);
        }

        // 3.13 测试基建：草稿预绑定存储 + 可控"空间已被生效合同占用"集合。
        final java.util.Map<String, java.util.Set<String>> boundSpaces = new java.util.HashMap<>();
        final java.util.Set<String> occupiedSpaces = new java.util.HashSet<>();
        // 退租结算测试：可控未清应收合计（unpaidArById 有条目则按合同覆盖，否则回退 unpaidAr）。
        long unpaidAr = 0L;
        final java.util.Map<String, Long> unpaidArById = new java.util.HashMap<>();
        // HP-C02 齐单核对测试：可控缺单清单（「yyyy-MM:fee_type」）。
        List<String> missingDocuments = List.of();
        // HP-C02-R6：记录服务传入的截止月（齐单/缺单判定 lastThrough；欠费口径逐次 unpaidThrough）。
        YearMonth lastThrough;
        final List<YearMonth> unpaidThrough = new ArrayList<>();

        private final Map<String, ContractRecord> contracts = new LinkedHashMap<>();
        private final Map<String, ContractChangeRecord> changes = new LinkedHashMap<>();
        private final Map<String, ContractSettlementRecord> settlements = new LinkedHashMap<>();
        private final List<SpaceExpiryColorRuleRecord> colorRules = new ArrayList<>();
        private final List<ContractRentBillRecord> rentBills = new ArrayList<>();
        // V494：按 (projectId, contractType) 的生效模板；出厂三类平台默认各一条（三线齐全、默认最终审批人空）。
        private final Map<String, ContractApprovalTemplateRecord> templates = new LinkedHashMap<>();
        final java.util.Set<String> activeLines = new java.util.HashSet<>(
                List.of("business_review", "finance_review", "legal_review"));
        final java.util.Set<String> activeUsers = new java.util.HashSet<>(List.of("usr_admin"));

        InMemoryContractRepository() {
            for (String type : ContractApprovalTemplateRecord.TYPES) {
                templates.put(templateKey(null, type), new ContractApprovalTemplateRecord("caf_default_" + type, null, type,
                        "平台默认·" + type, List.of("business_review", "finance_review", "legal_review"), null, true, null, null));
            }
        }

        private static String templateKey(String projectId, String type) {
            return projectId + "|" + type;
        }

        @Override
        public ContractRecord saveContract(ContractRecord c) {
            contracts.put(c.id(), c);
            return c;
        }

        @Override
        public List<ContractRecord> listContracts() {
            return List.copyOf(contracts.values());
        }

        @Override
        public List<ContractRecord> listChildContracts(String parentContractId) {
            return contracts.values().stream()
                    .filter(c -> c.rentTerms() != null
                            && parentContractId.equals(c.rentTerms().get("parentContractId")))
                    .toList();
        }

        @Override
        public Optional<ContractRecord> findContractById(String id) {
            return Optional.ofNullable(contracts.get(id));
        }

        int forUpdateCalls = 0;
        // 第三轮并发约定：加锁读时执行一次（模拟等锁期间先提交的另一事务改了结算单），执行后清空。
        Runnable onLock;

        @Override
        public Optional<ContractRecord> findContractByIdForUpdate(String id) {
            forUpdateCalls++;   // S-2：无真锁，仅计数证明确认路径走加锁读
            Runnable hook = onLock;
            onLock = null;
            if (hook != null) {
                hook.run();
            }
            return findContractById(id);
        }

        @Override
        public ContractChangeRecord saveChange(ContractChangeRecord ch) {
            changes.put(ch.id(), ch);
            return ch;
        }

        @Override
        public Optional<ContractChangeRecord> findChangeById(String id) {
            return Optional.ofNullable(changes.get(id));
        }

        @Override
        public List<ContractChangeRecord> listChangesByContract(String contractId) {
            return changes.values().stream().filter(ch -> ch.contractId().equals(contractId)).toList();
        }

        @Override
        public ContractSettlementRecord saveSettlement(ContractSettlementRecord s) {
            settlements.put(s.id(), s);
            return s;
        }

        @Override
        public Optional<ContractSettlementRecord> findSettlementById(String id) {
            return Optional.ofNullable(settlements.get(id));
        }

        @Override
        public List<ContractSettlementRecord> listSettlementsByContract(String contractId) {
            return settlements.values().stream().filter(s -> s.contractId().equals(contractId)).toList();
        }

        @Override
        public Optional<ContractApprovalTemplateRecord> findActiveTemplate(String projectId, String contractType) {
            ContractApprovalTemplateRecord t = templates.get(templateKey(projectId, contractType));
            return Optional.ofNullable(t != null ? t : templates.get(templateKey(null, contractType)));
        }

        @Override
        public List<ContractApprovalTemplateRecord> listTemplates(String projectId) {
            return ContractApprovalTemplateRecord.TYPES.stream()
                    .flatMap(type -> findActiveTemplate(projectId, type).stream()).toList();
        }

        @Override
        public ContractApprovalTemplateRecord saveTemplate(ContractApprovalTemplateRecord t) {
            String key = templateKey(t.projectId(), t.contractType());
            ContractApprovalTemplateRecord saved = new ContractApprovalTemplateRecord(
                    t.id() == null ? "caf_" + key : t.id(), t.projectId(), t.contractType(), t.name(),
                    t.requiredLineCodes(), t.defaultFinalApproverId(), true, t.minMainLevels(), t.maxMainLevels());
            templates.put(key, saved);
            return saved;
        }

        @Override
        public boolean approvalLineActive(String lineCode) {
            return activeLines.contains(lineCode);
        }

        @Override
        public boolean userActive(String userId) {
            return activeUsers.contains(userId);
        }

        @Override
        public SpaceExpiryColorRuleRecord saveColorRule(SpaceExpiryColorRuleRecord rule) {
            colorRules.add(rule);
            return rule;
        }

        @Override
        public List<SpaceExpiryColorRuleRecord> listColorRules(String projectId) {
            return List.copyOf(colorRules);
        }

        @Override
        public void bindContractSpaces(String contractId, List<String> spaceIds, String projectId) {
            boundSpaces.computeIfAbsent(contractId, k -> new java.util.LinkedHashSet<>()).addAll(spaceIds);
        }

        @Override
        public List<String> listContractSpaceIds(String contractId) {
            return List.copyOf(boundSpaces.getOrDefault(contractId, java.util.Set.of()));
        }

        @Override
        public boolean hasOverlappingEffectiveContractForSpace(
                String spaceId, LocalDate effectiveDate, LocalDate expiryDate, String excludeContractId) {
            return occupiedSpaces.contains(spaceId);
        }

        @Override
        public int markContractsExpired(LocalDate asOf) {
            return 0;
        }

        @Override
        public List<ContractRecord> listEffectiveContractsExpiringBetween(
                LocalDate fromExclusive,
                LocalDate toInclusive
        ) {
            return contracts.values().stream()
                    .filter(c -> "effective".equals(c.lifecycleStatus()))
                    .filter(c -> c.expiryDate().isAfter(fromExclusive) && !c.expiryDate().isAfter(toInclusive))
                    .toList();
        }

        @Override
        public List<SpaceRentalStatusRecord> listSpaceRentalRaw(String projectId, LocalDate asOf) {
            return List.of();
        }

        // 第四轮裁决第 2 条：已（部分）收款账单的台账项 id（Jdbc 口径：ar_ap_items.paid_amount_cent&gt;0）；作废账单 id → 作废原因。
        final java.util.Set<String> paidArItems = new java.util.HashSet<>();
        final Map<String, String> voidedBills = new LinkedHashMap<>();

        @Override
        public boolean rentBillExists(String contractId, String period, String feeType) {
            return rentBills.stream().anyMatch(b -> b.contractId().equals(contractId) && b.period().equals(period)
                    && b.feeType().equals(feeType) && !voidedBills.containsKey(b.id()));
        }

        @Override
        public List<String> voidUnpaidRentBills(List<String> contractIds, String fromPeriod, List<String> feeTypes, String reason) {
            List<String> out = new ArrayList<>();
            for (ContractRentBillRecord b : rentBills) {
                if (contractIds.contains(b.contractId()) && b.period().compareTo(fromPeriod) >= 0 && feeTypes.contains(b.feeType())
                        && !voidedBills.containsKey(b.id()) && !paidArItems.contains(b.arApItemId())) {
                    voidedBills.put(b.id(), reason);
                    out.add(b.id());
                }
            }
            return out;
        }

        @Override
        public ContractRentBillRecord insertRentBill(ContractRentBillRecord bill, LocalDate dueDate) {
            rentBills.add(bill);
            return bill;
        }

        @Override
        public long sumUnpaidArForContract(String contractId, YearMonth through) {
            unpaidThrough.add(through);
            return unpaidArById.getOrDefault(contractId, unpaidAr);
        }

        @Override
        public long sumUnpaidTerminationPenalty(String settlementId) {
            return unpaidPenaltyBySettlement.getOrDefault(settlementId, 0L);
        }

        // 第四轮裁决第 3 条夹具台账项 id：未清应收「arap_unpaid_<合同 id>」（未收额取 unpaidArById/unpaidAr）、违约金「arap_pen_<结算单 id>」。
        @Override
        public List<String> listUnpaidArIdsForContract(String contractId, YearMonth through) {
            return unpaidArById.getOrDefault(contractId, unpaidAr) > 0 ? List.of("arap_unpaid_" + contractId) : List.of();
        }

        long unpaidOf(String arApItemId) {
            return arApItemId.startsWith("arap_pen_")
                    ? unpaidPenaltyBySettlement.getOrDefault(arApItemId.substring("arap_pen_".length()), 0L)
                    : unpaidArById.getOrDefault(arApItemId.substring("arap_unpaid_".length()), unpaidAr);
        }

        void setUnpaid(String arApItemId, long unpaid) {
            if (arApItemId.startsWith("arap_pen_")) {
                unpaidPenaltyBySettlement.put(arApItemId.substring("arap_pen_".length()), unpaid);
            } else {
                unpaidArById.put(arApItemId.substring("arap_unpaid_".length()), unpaid);
            }
        }

        @Override
        public List<String> listMissingTerminationDocuments(ContractRecord contract, YearMonth through) {
            lastThrough = through;
            return missingDocuments;
        }
    }
}
