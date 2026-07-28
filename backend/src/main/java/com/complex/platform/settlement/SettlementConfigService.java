package com.complex.platform.settlement;

import com.complex.platform.settlement.JdbcSettlementConfigWriteRepository.PendingConfigDraft;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 结算配置中心只读概览聚合 —— 把散落的结算配置「当前平台默认生效值」+「待复核草案队列」
 * 汇成一处「看全局」总览,供总控页展示(PRD settlement-rebuild §9)。
 *
 * <p>读侧统一经 {@link JdbcSettlementBillRepository} 各 finder(强制 {@code is_draft=FALSE}),
 * 故展示的一律是「正式生效口径」;待复核草案单列(批1 起聚合全部配置表,非仅归因规则),不混入生效值。project=null 取全局平台默认。
 */
@Service
public class SettlementConfigService {

    private final JdbcSettlementBillRepository configRepository;
    private final JdbcSettlementConfigWriteRepository writeRepository;
    private final JdbcCommissionPointsConfigRepository commissionPointsConfigRepository;

    public SettlementConfigService(JdbcSettlementBillRepository configRepository,
                                   JdbcSettlementConfigWriteRepository writeRepository,
                                   JdbcCommissionPointsConfigRepository commissionPointsConfigRepository) {
        this.configRepository = configRepository;
        this.writeRepository = writeRepository;
        this.commissionPointsConfigRepository = commissionPointsConfigRepository;
    }

    /** 平台默认口径概览 + 待复核草案队列(批1 起草案源=全部结算配置表 UNION,见 {@link JdbcSettlementConfigWriteRepository#listPendingConfigDrafts})。 */
    public SettlementConfigOverview overview() {
        List<ConfigItem> defaults = new ArrayList<>();
        defaults.add(new ConfigItem("通票分账", "platform_retention_bps", "项目公司留存比例(万分比)",
                String.valueOf(configRepository.findRetentionBps(null))));
        defaults.add(new ConfigItem("通票分账", "distribution_method", "各酒吧分配方式",
                configRepository.findDistributionMethod(null)));
        var svcFee = configRepository.findComprehensiveServiceFee(null, null, null);
        defaults.add(new ConfigItem("门票综合服务费", "comprehensive_service_fee", "综合服务费(比例或固定额)",
                svcFee.fixedAmountCent() != null ? "固定额 " + svcFee.fixedAmountCent() + " 分" : svcFee.feeBps() + " 万分比"));
        JdbcSettlementBillRepository.ComboDiscountConfig combo = configRepository.findComboDiscountConfig(null);
        defaults.add(new ConfigItem("通票分账", "combo_discount_bearer", "套票折扣归属",
                combo.discountBearer()));
        // V340(P0-D/CFG-01):提成/积分两条原写死口径纳入总览「当前生效值」,与其它配置同源(只读生效行)。
        defaults.add(new ConfigItem("提成与积分", "commission_rate_sales", "卡座提成率·销售人员(万分比)",
                String.valueOf(commissionPointsConfigRepository.findCommissionRateBp(null, null, "sales"))));
        defaults.add(new ConfigItem("提成与积分", "commission_rate_non_sales", "卡座提成率·非销售人员(万分比)",
                String.valueOf(commissionPointsConfigRepository.findCommissionRateBp(null, null, "non_sales"))));
        defaults.add(new ConfigItem("提成与积分", "points_amount_cent_per_point", "积分累计率(每积1分所需消费·分)",
                String.valueOf(commissionPointsConfigRepository.findAmountCentPerPoint(null, null))));

        List<DraftItem> drafts = new ArrayList<>();
        for (PendingConfigDraft d : writeRepository.listPendingConfigDrafts()) {
            drafts.add(new DraftItem(d.id(), d.configGroup(), d.summary()));
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
