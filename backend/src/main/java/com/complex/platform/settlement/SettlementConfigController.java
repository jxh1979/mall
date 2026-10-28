package com.complex.platform.settlement;

// 文件版本：V0.03

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.settlement.SettlementConfigWriteRequests.ArchiveRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.HighValueThresholdRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.ChannelFeeRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.ComboDiscountRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.CommissionRateRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.ComprehensiveServiceFeeRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.DistributionMethodRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.DistributionSchemeRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.IncentiveApplicationRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.IncentiveDecisionRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.MerchantRateRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.NoticePeriodRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.PassGiftCostRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.PointsAccrualRateRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.RankTiersActivateRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.RankTiersRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.RevenueStreamAttributionRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 结算配置中心统一总控入口 —— settlement-rebuild-prd-v1.0 §9(配置中心 = 配置表族 + ADM-STL 页 + 权限点)。
 *
 * <p>批0:草案复核转正骨架 + overview。批1(本批):8 类配置写入面(落草案) + 转正泛化 + 软删。
 * <p>复核闸(照 batch0 轻量式):编辑(落草案)与转正均 {@code settlement.config.manage}(高敏·requires_approval)+ 平台 scope,
 * 与运营编辑权限 {@code analytics.operating_params.manage} 天然分离;写入侧强制 is_draft=TRUE,读侧只认 is_draft=FALSE。
 * (batch0 activate 未加显式 blockForAdmission；本批一致——设计钉死的资金级审批闸留待后续以 supervisor_review 强化。)
 */
@RestController
@RequestMapping("/api/v1/settlement/config")
public class SettlementConfigController {

    private final AuthorizationService authorizationService;
    private final SettlementConfigService settlementConfigService;
    private final SettlementConfigWriteService writeService;
    private final DistributionSchemeService schemeService;
    private final com.complex.platform.evidence.AuditEventService audit;
    private static final String INCENTIVE_APPROVE = "settlement.bill.special_approve";

    public SettlementConfigController(AuthorizationService authorizationService,
                                      SettlementConfigService settlementConfigService,
                                      SettlementConfigWriteService writeService,
                                      DistributionSchemeService schemeService,
                                      com.complex.platform.evidence.AuditEventService audit) {
        this.authorizationService = authorizationService;
        this.settlementConfigService = settlementConfigService;
        this.writeService = writeService;
        this.schemeService = schemeService;
        this.audit = audit;
    }

    /** 「看全局」总览:平台默认生效配置 + 待复核草案队列(须 settlement.config.view + 平台域)。 */
    @GetMapping("/overview")
    public ApiResponse<SettlementConfigService.SettlementConfigOverview> overview() {
        authorizationService.requirePermission("settlement.config.view");
        authorizationService.requirePlatformScope();
        return ApiResponse.ok(settlementConfigService.overview());
    }

    // ================= §2 写入面(落草案;settlement.config.manage + 平台域)=================

    @PostMapping("/distribution-method")
    public ApiResponse<Void> saveDistributionMethod(@RequestBody DistributionMethodRequest request) {
        writeService.saveDistributionMethod(request, requireManageActor());
        return ApiResponse.ok(null);
    }

    @PostMapping("/merchant-rate")
    public ApiResponse<Void> saveMerchantRate(@RequestBody MerchantRateRequest request) {
        writeService.saveMerchantRate(request, requireManageActor());
        return ApiResponse.ok(null);
    }

    @PostMapping("/comprehensive-service-fee")
    public ApiResponse<Void> saveComprehensiveServiceFee(@RequestBody ComprehensiveServiceFeeRequest request) {
        writeService.saveComprehensiveServiceFee(request, requireManageActor());
        return ApiResponse.ok(null);
    }

    @PostMapping("/combo-discount")
    public ApiResponse<Void> saveComboDiscount(@RequestBody ComboDiscountRequest request) {
        writeService.saveComboDiscount(request, requireManageActor());
        return ApiResponse.ok(null);
    }

    @PostMapping("/channel-fee")
    public ApiResponse<Void> saveChannelFee(@RequestBody ChannelFeeRequest request) {
        writeService.saveChannelFee(request, requireManageActor());
        return ApiResponse.ok(null);
    }

    /** V340(P0-D/CFG-01):卡座销售提成费率(项目 × 商户 × 角色档)落草案。 */
    @PostMapping("/commission-rate")
    public ApiResponse<Void> saveCommissionRate(@RequestBody CommissionRateRequest request) {
        writeService.saveCommissionRate(request, requireManageActor());
        return ApiResponse.ok(null);
    }

    /** V340(P0-D/CFG-01):积分累计率(项目 × 商户)落草案。 */
    @PostMapping("/points-accrual-rate")
    public ApiResponse<Void> savePointsAccrualRate(@RequestBody PointsAccrualRateRequest request) {
        writeService.savePointsAccrualRate(request, requireManageActor());
        return ApiResponse.ok(null);
    }

    /**
     * 第十八批第 24 条：全平台统一高价值判定阈值（单位分；最近一次实际到货价大于或等于本值即高价值）连同上下限与理由提交最高权限审批；
     * 权限、范围与校验失败按 OpenAPI 声明的失败事件留痕。
     */
    @PostMapping("/high-value-threshold")
    public ApiResponse<Void> saveHighValueThreshold(@RequestBody HighValueThresholdRequest request) {
        auditFailure("settlement.config.high_value_thresholds.update_failed", "settlement.config.manage", "high_value_thresholds", null,
                request == null ? null : request.idempotencyKey(), () -> {
                    writeService.saveHighValueThreshold(request, requireManageActor());
                    return null;
                });
        return ApiResponse.ok(null);
    }

    /** S6(V373)：赠送权益预估成本预扣规则（项目 × 票种，两模式二选一）落草案。 */
    @PostMapping("/pass-gift-cost")
    public ApiResponse<Void> savePassGiftCost(@RequestBody PassGiftCostRequest request) {
        writeService.savePassGiftCost(request, requireManageActor());
        return ApiResponse.ok(null);
    }

    /**
     * 批10/E:内部结算 SKU 目录条目落草案(新建与改价同一入口)。返回落地 item_code ——
     * 业务页「就地新建」拿它立刻建立引用;条目须他人复核转正后才有生效价(此前引用方解析 fail-closed)。
     */
    @PostMapping("/internal-price-item")
    public ApiResponse<String> saveInternalPriceItem(
            @RequestBody SettlementConfigWriteRequests.InternalPriceItemRequest request) {
        return ApiResponse.ok(writeService.saveInternalPriceItem(request, requireManageActor()));
    }

    /**
     * 批10/E:目录三维筛选列表(商家/业态/品类;业态随商家带出)。内部结算价属敏感信息,仅授权后台可见:
     * 结算配置读 或 V279/V301 两个内部价读脱敏点任一。商户 scope 操作者强制只看本商户条目(租户隔离)。
     */
    @GetMapping("/internal-price-items")
    public ApiResponse<java.util.List<JdbcSettlementConfigWriteRepository.CatalogEntryRow>> listInternalPriceItems(
            @org.springframework.web.bind.annotation.RequestParam(value = "merchant_id", required = false) String merchantId,
            @org.springframework.web.bind.annotation.RequestParam(value = "business_format", required = false) String businessFormat,
            @org.springframework.web.bind.annotation.RequestParam(value = "category", required = false) String category,
            @org.springframework.web.bind.annotation.RequestParam(value = "include_drafts", required = false,
                    defaultValue = "false") boolean includeDrafts,
            @org.springframework.web.bind.annotation.RequestParam(value = "include_archived", required = false,
                    defaultValue = "false") boolean includeArchived) {
        if (!authorizationService.currentActorHasPermission("settlement.config.view")
                && !authorizationService.currentActorHasPermission("bar.internal_settlement_price.view")
                && !authorizationService.currentActorHasPermission("voucher.internal_price.view")) {
            throw com.complex.platform.common.api.ApiException.forbidden("INTERNAL_PRICE_FORBIDDEN",
                    "内部结算价属敏感信息,仅授权后台人员可见");
        }
        String scopedMerchant = authorizationService.currentMerchantScope().orElse(merchantId);
        return ApiResponse.ok(settlementConfigService.listInternalPriceItems(
                scopedMerchant, businessFormat, category, includeDrafts, includeArchived));
    }

    @PostMapping("/rank-tiers")
    public ApiResponse<Void> saveRankTiers(@RequestBody RankTiersRequest request) {
        writeService.saveRankTiers(request, requireManageActor());
        return ApiResponse.ok(null);
    }

    /**
     * 第十八批第 2 条（ENUM L52「档位表变更须提前向受影响的酒吧商户发出通告……经过通告期后方可生效」）：通票分账方案各版本
     * （状态、通告日、生效日、参与家数档位表与第一段名单）、通告期，以及「补配该家数档位行」待办。
     */
    @GetMapping("/distribution-scheme")
    public ApiResponse<DistributionSchemeService.SchemeOverview> distributionScheme() {
        authorizationService.requirePermission("settlement.config.view");
        authorizationService.requirePlatformScope();
        return ApiResponse.ok(schemeService.overview());
    }

    /** 通票分账方案整套落草案（参与家数档位表＋第一段名单）；每个家数行合计须 100%、每格须同时配置上下限。返回草案 id。 */
    @PostMapping("/distribution-scheme")
    public ApiResponse<String> saveDistributionScheme(@RequestBody DistributionSchemeRequest request) {
        return ApiResponse.ok(auditFailure("settlement.config.distribution_scheme.update_failed", null,
                request == null ? null : request.idempotencyKey(), () -> schemeService.saveDraft(request, requireManageActor())));
    }

    /**
     * 差人复核转正：当日逐家向受影响酒吧发通告即视为送达，生效日＝转正日＋通告期（操作员裁决第十八批第 1 条）。
     * 第 75 条：转正前按最低可能可分配池金额逐票种试算第一段，不通过且没有已批准的首次短期经营激励申请 409 POOL_TRIAL_FAILED。
     */
    @PostMapping("/distribution-scheme/{id}/activate")
    public ApiResponse<DistributionSchemeService.SchemeVersion> activateDistributionScheme(@PathVariable("id") String id) {
        return ApiResponse.ok(auditFailure("settlement.config.draft.activate_failed", id, null,
                () -> schemeService.activate(id, requireManageActor())));
    }

    /** 档位表与第一段变更共用的通告期：数值、单位与上下限同时必填。 */
    @PostMapping("/distribution-scheme/notice-period")
    public ApiResponse<DistributionSchemeService.NoticePeriod> saveDistributionNoticePeriod(@RequestBody NoticePeriodRequest request) {
        return ApiResponse.ok(auditFailure("settlement.config.distribution_notice_period.update_failed", "default",
                request == null ? null : request.idempotencyKey(), () -> schemeService.saveNoticePeriod(request, requireManageActor())));
    }

    /**
     * 第 75 条短期经营激励申请（ENUM L52「须在配置生效前提交短期经营激励申请并经项目之上的平台权限审批」）：草案版本提交首次申请，
     * 已凭激励放行转正的版本提交到期重审；由结算配置管理人员代娱乐平台公司提交（settlement.config.manage＋平台域）。
     */
    @PostMapping("/distribution-scheme/{id}/incentive-applications")
    public ApiResponse<DistributionSchemeService.IncentiveApplication> submitIncentiveApplication(
            @PathVariable("id") String id, @RequestBody IncentiveApplicationRequest request) {
        return ApiResponse.ok(auditFailure("settlement.config.pass_incentive.submit_failed", "settlement.config.manage",
                "pass_incentive_applications", id, request == null ? null : request.idempotencyKey(),
                () -> schemeService.submitIncentive(id, request, requireManageActor())));
    }

    /** 第 75 条审批：批准（项目之上的平台权限，DEC-R3-2026-08-04-13；审批人不得是提交人）。 */
    @PostMapping("/incentive-applications/{id}/approve")
    public ApiResponse<DistributionSchemeService.IncentiveApplication> approveIncentiveApplication(
            @PathVariable("id") String id, @RequestBody IncentiveDecisionRequest request) {
        return ApiResponse.ok(auditFailure("settlement.config.pass_incentive.approve_failed", INCENTIVE_APPROVE,
                "pass_incentive_applications", id, request == null ? null : request.idempotencyKey(),
                () -> schemeService.decideIncentive(id, true, request, requireIncentiveApprover())));
    }

    /** 第 75 条审批：驳回（同批准的权限与差人规则）。 */
    @PostMapping("/incentive-applications/{id}/reject")
    public ApiResponse<DistributionSchemeService.IncentiveApplication> rejectIncentiveApplication(
            @PathVariable("id") String id, @RequestBody IncentiveDecisionRequest request) {
        return ApiResponse.ok(auditFailure("settlement.config.pass_incentive.reject_failed", INCENTIVE_APPROVE,
                "pass_incentive_applications", id, request == null ? null : request.idempotencyKey(),
                () -> schemeService.decideIncentive(id, false, request, requireIncentiveApprover())));
    }

    /** 分账方案三个写口按 OpenAPI 声明的失败事件留痕（权限、范围与校验失败一律记，再原样抛出）。 */
    private <T> T auditFailure(String failureEvent, String businessId, String idempotencyKey, java.util.function.Supplier<T> action) {
        return auditFailure(failureEvent, "settlement.config.manage", "ticket_distribution_scheme_versions", businessId,
                idempotencyKey, action);
    }

    private <T> T auditFailure(String failureEvent, String permission, String table, String businessId, String idempotencyKey,
                               java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (RuntimeException exception) {
            audit.recordFailure(failureEvent, permission, table, businessId, null,
                    idempotencyKey, exception instanceof com.complex.platform.common.api.ApiException api ? api.code() : "INTERNAL_ERROR");
            throw exception;
        }
    }

    @PostMapping("/revenue-stream-attribution")
    public ApiResponse<Void> saveRevenueStreamAttribution(@RequestBody RevenueStreamAttributionRequest request) {
        writeService.saveRevenueStreamAttribution(request, requireManageActor());
        return ApiResponse.ok(null);
    }

    // ================= §3 复核转正 =================

    /** 排名档位梯子整体转正(与 {configType}/{id}/activate 分开,梯子按 project 换梯)。放在通配前声明。 */
    @PostMapping("/rank-tiers/activate")
    public ApiResponse<Void> activateRankTiers(@RequestBody(required = false) RankTiersActivateRequest request) {
        String projectId = request == null ? null : request.projectId();
        writeService.activateRankTiers(projectId, requireManageActor());
        return ApiResponse.ok(null);
    }

    /** 转正泛化:单行/归属/归因规则草案转正(configType 下划线白名单,不含 giveaway)。 */
    @PostMapping("/{configType}/{id}/activate")
    public ApiResponse<Void> activateDraft(@PathVariable("configType") String configType,
                                           @PathVariable("id") String id) {
        writeService.activateDraft(configType, id, requireManageActor());
        return ApiResponse.ok(null);
    }

    /** 软删(仅 merchant_rate / revenue_stream_attribution):status→inactive + archive_reason。 */
    @PostMapping("/{configType}/{id}/archive")
    public ApiResponse<Void> archiveConfig(@PathVariable("configType") String configType,
                                           @PathVariable("id") String id,
                                           @RequestBody ArchiveRequest request) {
        writeService.archive(configType, id, request, requireManageActor());
        return ApiResponse.ok(null);
    }

    /**
     * 弃用薄别名:保留 batch0 前端在用的归因规则转正入口,内部转调转正泛化(attribution_rule)。
     * @deprecated 用 {@code POST /{configType}/{id}/activate}(configType=attribution_rule)。
     */
    @Deprecated
    @PostMapping("/attribution-rules/{id}/activate")
    public ApiResponse<Void> activateAttributionRuleDraft(@PathVariable("id") String id) {
        writeService.activateDraft("attribution_rule", id, requireManageActor());
        return ApiResponse.ok(null);
    }

    /**
     * 第 75 条激励审批闸（DEC-R3-2026-08-04-13「项目之上的平台权限」）：settlement.bill.special_approve（高敏，只授平台负责人）
     * ＋平台域，且须能看全部项目（平台管理员或勾了「可以查看全部项目」的角色）；只管单个项目的人 403。
     */
    private String requireIncentiveApprover() {
        authorizationService.requirePermission(INCENTIVE_APPROVE);
        authorizationService.requirePlatformScope();
        if (!authorizationService.allowsPlatformScope(authorizationService.currentActor(), INCENTIVE_APPROVE)) {
            throw com.complex.platform.common.api.ApiException.forbidden("PLATFORM_SCOPE_REQUIRED",
                    "短期经营激励须由项目之上的平台权限审批");
        }
        return authorizationService.currentActor().id();
    }

    /** 编辑/转正/软删统一闸:settlement.config.manage(高敏)+ 平台 scope;返回操作者 id 供 updated_by/审计。 */
    private String requireManageActor() {
        authorizationService.requirePermission("settlement.config.manage");
        authorizationService.requirePlatformScope();
        return authorizationService.currentActor().id();
    }
}
