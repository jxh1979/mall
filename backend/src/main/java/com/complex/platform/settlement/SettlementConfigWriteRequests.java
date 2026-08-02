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

    /**
     * ticket_distribution_configs:分配方式 ∈{by_venue_per_guest,by_rank_tier,by_fixed_amount} + S9 超池特批有效期。
     *
     * <p>V368(用户 2026-07-28 纠正「不是指校验大于等于一个月,也可以三天」):有效期 = 数值 + 单位,
     * specialApprovalTermUnit ∈{day,month};两项都留空 = 出厂默认 3 个月(业务口径「期限可配,默认不超过 3 个月」)。
     * 下限只到「1 个单位」(1 天/1 个月),没有上限。
     */
    public record DistributionMethodRequest(String idempotencyKey, String projectId, String methodType,
                                            Integer specialApprovalTermValue, String specialApprovalTermUnit) {
    }

    /** ticket_distribution_merchant_rates:某商户每客单价/固定额(分),两额至少配一、各≥0。 */
    public record MerchantRateRequest(String idempotencyKey, String projectId, String merchantId,
                                      Long perGuestCent, Long fixedAmountCent) {
    }

    /**
     * comprehensive_service_fee_configs:门票综合服务费。范围=项目 × 商户 × 票种(留空=该层通用)。
     * 二选一:配固定额则 feeBps 必 0(对齐 V328 chk_csfc_fee_mode);monthlyCapAmountCent 留空=不封顶。
     *
     * <p>S2(V365):第三维由 entitlementType(权益类型)换成 ticketTypeId(票种),对齐 HTML
     * 「可按（项目 × 商户 × 票种）范围另设每月封顶金额」——业务人员按票种谈费率、按票种对账。
     */
    public record ComprehensiveServiceFeeRequest(String idempotencyKey, String projectId, String merchantId,
                                                 String ticketTypeId, Integer feeBps, Long fixedAmountCent,
                                                 Long monthlyCapAmountCent) {
    }

    /**
     * ticket_combo_discount_configs:套票折扣归属 ∈{platform_absorb,merchant_absorb,split_by_ratio};splitPlatformBps∈[0,10000]。
     *
     * <p>V369 roundingToleranceCent:综合/联合通票「凑整允许误差」(分,≥0,留空=不改沿用 500 出厂值)。
     * 运营给通票填的近似售价与系统按各业态票面原价×折扣算出的售价,差额超过这个数就不许开卖。
     * 和折扣归属同填一处,因为凑整多出/少收的那点钱走的正是这条折扣承担口径。
     */
    public record ComboDiscountRequest(String idempotencyKey, String projectId, String discountBearer,
                                       Integer splitPlatformBps, Long roundingToleranceCent) {

        /** V369 前的 4 参签名(无 roundingToleranceCent)→ null=沿用出厂 500 分。 */
        public ComboDiscountRequest(String idempotencyKey, String projectId, String discountBearer,
                                    Integer splitPlatformBps) {
            this(idempotencyKey, projectId, discountBearer, splitPlatformBps, null);
        }
    }

    /**
     * channel_fee_configs:渠道抽成。比例(feeBps∈[0,10000])或每张票固定金额(fixedAmountCent,分,≥0)二选一
     * (V381 chk_cfc_fee_mode:配固定金额则 feeBps 必 0);批1 UI 只填 channel=null 通用配置。
     */
    public record ChannelFeeRequest(String idempotencyKey, String projectId, String channel, Integer feeBps,
                                    Long fixedAmountCent) {

        /** V381 前的 4 参签名(无 fixedAmountCent)→ null=比例模式。 */
        public ChannelFeeRequest(String idempotencyKey, String projectId, String channel, Integer feeBps) {
            this(idempotencyKey, projectId, channel, feeBps, null);
        }
    }

    /** ticket_distribution_rank_tiers:by_rank_tier 排名档位梯子(整套替换草案)。 */
    public record RankTiersRequest(String idempotencyKey, String projectId, List<RankTierInput> tiers) {
    }

    /** 单档:1≤rankFrom≤rankTo、shareBps∈[0,10000];money 守卫 Σ((rankTo-rankFrom+1)×shareBps)=10000 在服务层校验。 */
    public record RankTierInput(Integer rankFrom, Integer rankTo, Integer shareBps) {
    }

    /**
     * revenue_stream_attribution:业态收入流 → 收款方。
     * beneficiaryKind 留空视同 {@code entity}(归某个经营主体,managementEntityId 必填);
     * {@code per_merchant}=归产生该笔收入的各商户自己(订台低消/二次消费/额外酒水、非散台服务费),此时不得填主体。
     */
    public record RevenueStreamAttributionRequest(String idempotencyKey, String projectId, String revenueStream,
                                                  String managementEntityId, String beneficiaryKind) {
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

    /**
     * S6(V373)pass_gift_cost_configs:赠送权益预估成本预扣规则。范围=项目 × 票种(留空=该层通用)。
     * predeductMode 二选一:by_item 按件计(票面各权益数量 × 内部结算价之和,ticketGiftAmountCent 必须留空)/
     * by_ticket_amount 按票计(该票种赠送部分售价整额,ticketGiftAmountCent 必填 ≥0,0=预扣关闭态)。
     * monthlyCapAmountCent 留空=不封顶;varianceHandling 留空=platform_absorb(差额留娱乐平台公司,S7 消费)。
     */
    public record PassGiftCostRequest(String idempotencyKey, String projectId, String ticketTypeId,
                                      String predeductMode, Long ticketGiftAmountCent,
                                      Long monthlyCapAmountCent, String varianceHandling) {
    }

    /** 梯子整体转正 body(§3):projectId 可空=全局默认梯子。 */
    public record RankTiersActivateRequest(String projectId) {
    }

    /**
     * 批10/E internal_price_catalog:内部结算 SKU 目录条目(草案)。
     * itemCode 留空=新建条目(服务层生成稳定码,业务表引用它);非空=对既有条目改价/改名/改类。
     * merchantId 必填(HTML:每条必须绑定提供它的商家,业态随商家带出);category/itemName 必填;
     * priceCent 必填 ≥0(0=商户让利型合法价,S5 同一套 0/NULL 语义)。
     */
    public record InternalPriceItemRequest(String idempotencyKey, String itemCode, String merchantId,
                                           String category, String itemName, Long priceCent) {
    }

    /** 软删 body(§2 软删铁律):archive_reason 必填。 */
    public record ArchiveRequest(String idempotencyKey, String archiveReason) {
    }
}
