package com.complex.platform.settlement;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.settlement.SettlementConfigWriteRequests.ArchiveRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.HighValueThresholdRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.ChannelFeeRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.ComboDiscountRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.CommissionRateRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.ComprehensiveServiceFeeRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.DistributionMethodRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.MerchantRateRequest;
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

    public SettlementConfigController(AuthorizationService authorizationService,
                                      SettlementConfigService settlementConfigService,
                                      SettlementConfigWriteService writeService) {
        this.authorizationService = authorizationService;
        this.settlementConfigService = settlementConfigService;
        this.writeService = writeService;
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

    /** V350：全平台高价值判定阈值（项目 × 商户，单位分；比的是物品进货价）落草案。 */
    @PostMapping("/high-value-threshold")
    public ApiResponse<Void> saveHighValueThreshold(@RequestBody HighValueThresholdRequest request) {
        writeService.saveHighValueThreshold(request, requireManageActor());
        return ApiResponse.ok(null);
    }

    @PostMapping("/rank-tiers")
    public ApiResponse<Void> saveRankTiers(@RequestBody RankTiersRequest request) {
        writeService.saveRankTiers(request, requireManageActor());
        return ApiResponse.ok(null);
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

    /** 编辑/转正/软删统一闸:settlement.config.manage(高敏)+ 平台 scope;返回操作者 id 供 updated_by/审计。 */
    private String requireManageActor() {
        authorizationService.requirePermission("settlement.config.manage");
        authorizationService.requirePlatformScope();
        return authorizationService.currentActor().id();
    }
}
