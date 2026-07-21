package com.complex.platform.settlement;

import com.complex.platform.analytics.AnalyticsRecords.MerchantRevenueAttributionRule;
import com.complex.platform.analytics.OperatingParamsService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 结算配置中心(新立项批0)只读概览聚合 —— 把散落的结算配置「当前平台默认生效值」+「待复核草案队列」
 * 汇成一处「看全局」总览,供总控页展示(PRD settlement-rebuild §9)。
 *
 * <p>读侧统一经 {@link JdbcSettlementBillRepository} 各 finder(批0 已强制 {@code is_draft=FALSE}),
 * 故展示的一律是「正式生效口径」;待复核草案单列,不混入生效值。project=null 取全局平台默认。
 */
@Service
public class SettlementConfigService {

    private final JdbcSettlementBillRepository configRepository;
    private final OperatingParamsService operatingParamsService;

    public SettlementConfigService(JdbcSettlementBillRepository configRepository,
                                   OperatingParamsService operatingParamsService) {
        this.configRepository = configRepository;
        this.operatingParamsService = operatingParamsService;
    }

    /** 平台默认口径概览 + 待复核草案队列(批0 草案源=收入归因规则;后续批次各配置页接入后一并汇入)。 */
    public SettlementConfigOverview overview() {
        List<ConfigItem> defaults = new ArrayList<>();
        defaults.add(new ConfigItem("通票分账", "platform_retention_bps", "项目公司留存比例(万分比)",
                String.valueOf(configRepository.findRetentionBps(null))));
        defaults.add(new ConfigItem("赠酒", "giveaway_cost_per_cup_cent", "每杯赠酒成本补偿(分)",
                String.valueOf(configRepository.findGiveawayCostPerCupCent(null))));
        defaults.add(new ConfigItem("通票分账", "distribution_method", "各酒吧分配方式",
                configRepository.findDistributionMethod(null)));
        defaults.add(new ConfigItem("门票综合服务费", "comprehensive_service_fee_bps", "综合服务费(万分比)",
                String.valueOf(configRepository.findComprehensiveServiceFeeBps(null, null, null))));
        JdbcSettlementBillRepository.ComboDiscountConfig combo = configRepository.findComboDiscountConfig(null);
        defaults.add(new ConfigItem("通票分账", "combo_discount_bearer", "套票折扣归属",
                combo.discountBearer()));

        List<DraftItem> drafts = new ArrayList<>();
        for (MerchantRevenueAttributionRule r : operatingParamsService.listRevenueAttributionRules()) {
            if (r.isDraft()) {
                drafts.add(new DraftItem(r.id(), "revenue_attribution",
                        r.revenueSource() + " / " + r.ruleScope() + " · " + r.attributionMode()
                                + " · " + r.shareBps() + "bps"));
            }
        }
        return new SettlementConfigOverview(defaults, drafts.size(), drafts);
    }

    /** 结算配置总览投影(批0)。 */
    public record SettlementConfigOverview(List<ConfigItem> platformDefaults, int pendingDraftCount,
                                           List<DraftItem> pendingDrafts) {
    }

    /** 单个配置项当前生效值(展示用)。 */
    public record ConfigItem(String group, String key, String label, String value) {
    }

    /** 待复核草案条目(展示用)。 */
    public record DraftItem(String id, String configGroup, String summary) {
    }
}
