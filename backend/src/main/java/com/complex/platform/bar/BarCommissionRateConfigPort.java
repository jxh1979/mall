package com.complex.platform.bar;

/**
 * 卡座销售提成费率配置端口(P0-D/CFG-01,V340)。
 *
 * <p>依赖方向 bar → 本端口 ← settlement(实现方 {@code JdbcCommissionPointsConfigRepository} 落在结算配置中心侧):
 * 酒吧域只声明「我要一个可配的提成率」,费率的作用域优先级/草案闸/复核转正一律由结算配置中心统一承接,
 * 酒吧域不感知配置表结构 —— 与 {@code AmusementVerificationPort} 同一套端口范式。
 *
 * <p>本端口消灭的硬编码:{@code BarBillingService.calculateCommission} 原
 * {@code int rateBp = "sales".equals(roleType) ? 1500 : 1000;}(bar PRD 早写「后台可配」而代码写死)。
 */
public interface BarCommissionRateConfigPort {

    /**
     * 取提成率(万分比,1500=15%)。
     *
     * <p>作用域按「越具体越优先」解析:项目公司 × 商户 × 角色档 →(缺则)项目公司 × 通用商户 × 角色档 →
     * (缺则)全局默认 × 通用商户 × 角色档。三层全空(连 V340 的 __DEFAULT__ 种子都没有 = 部署坏)时
     * <b>响亮抛错</b>而非静默套硬编码默认,与结算配置中心 {@code requireConfigured} 同口径。
     *
     * @param projectId  项目公司 id;null = 全局默认作用域
     * @param merchantId 商户(酒吧)id;null = 不按商户细分
     * @param roleType   提成角色档:{@code sales} / {@code non_sales}
     * @return 提成率万分比,∈[0,10000]
     * @throws com.complex.platform.common.api.ApiException 配置缺失(SETTLEMENT_CONFIG_MISSING)
     */
    int findCommissionRateBp(String projectId, String merchantId, String roleType);
}
