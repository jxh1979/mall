package com.complex.platform.settlement;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.analytics.OperatingParamsService;
import com.complex.platform.common.api.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 结算配置中心(新立项批0)统一总控入口 —— 依据 settlement-rebuild-prd-v1.0 §9(配置中心=配置表族+ADM-STL页+权限)。
 *
 * <p>批0 骨架能力:<b>草案复核转正</b> —— 把结算配置草案(is_draft=TRUE)经复核转为正式生效(FALSE)。
 * 读取侧已统一强制「仅 is_draft=FALSE 生效」(JdbcSettlementBillRepository / 归因规则应用路径),
 * 故任何配置改动先落草案、不即时生效,须经此端点转正。
 *
 * <p>复核闸(批0 轻量式):转正须 {@code settlement.config.manage}(高敏,仅平台管理员)+ 前态守卫 + 审计留痕;
 * 与编辑权限 {@code analytics.operating_params.manage} 天然分离(运营编辑→草案,平台管理员复核→生效)。
 * 资金级配置(如分红,批4)将改用 supervisor_review 重审批闸。
 */
@RestController
@RequestMapping("/api/v1/settlement/config")
public class SettlementConfigController {

    private final AuthorizationService authorizationService;
    private final OperatingParamsService operatingParamsService;
    private final SettlementConfigService settlementConfigService;

    public SettlementConfigController(AuthorizationService authorizationService,
                                      OperatingParamsService operatingParamsService,
                                      SettlementConfigService settlementConfigService) {
        this.authorizationService = authorizationService;
        this.operatingParamsService = operatingParamsService;
        this.settlementConfigService = settlementConfigService;
    }

    /** 结算配置中心「看全局」总览:当前平台默认生效配置 + 待复核草案队列(须 settlement.config.view + 平台域)。 */
    @org.springframework.web.bind.annotation.GetMapping("/overview")
    public ApiResponse<SettlementConfigService.SettlementConfigOverview> overview() {
        authorizationService.requirePermission("settlement.config.view");
        authorizationService.requirePlatformScope();
        return ApiResponse.ok(settlementConfigService.overview());
    }

    /** 复核转正:把一条收入归因规则草案转为正式生效口径(高敏,须 settlement.config.manage + 平台域)。 */
    @PostMapping("/attribution-rules/{id}/activate")
    public ApiResponse<Void> activateAttributionRuleDraft(@PathVariable("id") String id) {
        authorizationService.requirePermission("settlement.config.manage");
        authorizationService.requirePlatformScope();
        operatingParamsService.activateRevenueAttributionRuleDraft(
                id, authorizationService.currentActor().id());
        return ApiResponse.ok(null);
    }
}
