package com.complex.platform.settlement;

import java.util.List;

/**
 * 结算配置中心批1 写入面请求 DTO 族(§2)。JSON 全局 SNAKE_CASE:字段 idempotencyKey ↔ idempotency_key 等。
 * 所有写入落草案(服务层强制 is_draft=TRUE),需 idempotency_key,平台 scope + settlement.config.manage。
 *
 * <p>TODO(生效日期调度):5 张单行表有 effective_from/effective_to 列,但所有 finder 均不按结算月过滤=形同虚设
 * (排期下月转正后当月即按新值结算=按错时点)。批1 不接收该二字段(写面不赋值、留 null);生效日期调度=独立功能,
 * 需 finder 按结算月过滤 effective_from/to 才能开放,届时随写面一并加。
 */
public final class SettlementConfigWriteRequests {

    private SettlementConfigWriteRequests() {
    }

    /** platform_retention_rules:项目公司留存万分比 bps∈[0,10000]。 */
    public record RetentionRequest(String idempotencyKey, String projectId, Integer retentionBps) {
    }

    /** ticket_distribution_configs:分配方式 ∈{by_venue_per_guest,by_rank_tier,by_fixed_amount}。 */
    public record DistributionMethodRequest(String idempotencyKey, String projectId, String methodType) {
    }

    /** ticket_distribution_merchant_rates:某商户每客单价/固定额(分),两额至少配一、各≥0。 */
    public record MerchantRateRequest(String idempotencyKey, String projectId, String merchantId,
                                      Long perGuestCent, Long fixedAmountCent) {
    }

    /** comprehensive_service_fee_configs:综合服务费。二选一:配固定额则 feeBps 必 0(对齐 V328 chk_csfc_fee_mode)。 */
    public record ComprehensiveServiceFeeRequest(String idempotencyKey, String projectId, String merchantId,
                                                 String entitlementType, Integer feeBps, Long fixedAmountCent,
                                                 Long monthlyCapAmountCent) {
    }

    /** ticket_combo_discount_configs:套票折扣归属 ∈{platform_absorb,merchant_absorb,split_by_ratio};splitPlatformBps∈[0,10000]。 */
    public record ComboDiscountRequest(String idempotencyKey, String projectId, String discountBearer,
                                       Integer splitPlatformBps) {
    }

    /** channel_fee_configs:渠道抽成万分比。批1 UI 只填 channel=null 通用率;feeBps∈[0,10000]。 */
    public record ChannelFeeRequest(String idempotencyKey, String projectId, String channel, Integer feeBps) {
    }

    /** ticket_distribution_rank_tiers:by_rank_tier 排名档位梯子(整套替换草案)。 */
    public record RankTiersRequest(String idempotencyKey, String projectId, List<RankTierInput> tiers) {
    }

    /** 单档:1≤rankFrom≤rankTo、shareBps∈[0,10000];money 守卫 Σ((rankTo-rankFrom+1)×shareBps)=10000 在服务层校验。 */
    public record RankTierInput(Integer rankFrom, Integer rankTo, Integer shareBps) {
    }

    /** revenue_stream_attribution:六业态收入流 → L3 管理公司/业态主体。 */
    public record RevenueStreamAttributionRequest(String idempotencyKey, String projectId, String revenueStream,
                                                  String managementEntityId) {
    }

    /**
     * V340(P0-D/CFG-01)bar_commission_rate_configs:卡座销售提成率万分比。
     * roleType ∈{sales,non_sales};merchantId 留空=该项目通用;rateBp∈[0,10000](出厂默认 sales=1500/non_sales=1000)。
     */
    public record CommissionRateRequest(String idempotencyKey, String projectId, String merchantId,
                                        String roleType, Integer rateBp) {
    }

    /**
     * V340(P0-D/CFG-01)points_accrual_rate_configs:积分累计率。
     * amountCentPerPoint=每积 1 基础分所需消费金额(分),须&gt;0(出厂默认 100=每 1 元积 1 分);merchantId 留空=通用。
     */
    public record PointsAccrualRateRequest(String idempotencyKey, String projectId, String merchantId,
                                           Long amountCentPerPoint) {
    }

    /**
     * V350 high_value_thresholds：全平台「高价值物品」判定阈值（退酒/报损/盘点差异/存酒作废共用）。
     * highValueThresholdCent=单笔已出品退品金额「超过」本值即高价值（须先审后行 + 双证据），单位分，须≥0；
     * 出厂默认 500000（5000 元）；merchantId 留空=该项目通用。
     */
    public record HighValueThresholdRequest(String idempotencyKey, String projectId, String merchantId,
                                          Long highValueThresholdCent) {
    }

    /** 梯子整体转正 body(§3):projectId 可空=全局默认梯子。 */
    public record RankTiersActivateRequest(String projectId) {
    }

    /** 软删 body(§2 软删铁律):archive_reason 必填。 */
    public record ArchiveRequest(String idempotencyKey, String archiveReason) {
    }
}
