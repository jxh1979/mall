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
    private final JdbcHighValueThresholdRepository highValueThresholdRepository;

    public SettlementConfigService(JdbcSettlementBillRepository configRepository,
                                   JdbcSettlementConfigWriteRepository writeRepository,
                                   JdbcCommissionPointsConfigRepository commissionPointsConfigRepository,
                                   JdbcHighValueThresholdRepository highValueThresholdRepository) {
        this.configRepository = configRepository;
        this.writeRepository = writeRepository;
        this.commissionPointsConfigRepository = commissionPointsConfigRepository;
        this.highValueThresholdRepository = highValueThresholdRepository;
    }

    /** 平台默认口径概览 + 待复核草案队列(批1 起草案源=全部结算配置表 UNION,见 {@link JdbcSettlementConfigWriteRepository#listPendingConfigDrafts})。 */
    public SettlementConfigOverview overview() {
        List<ConfigItem> defaults = new ArrayList<>();
        // S4:原「项目公司留存比例」卡片已下线——与门票综合服务费为同一项、只扣一次(业务 HTML),
        //   费率统一看下面的「门票综合服务费」。
        defaults.add(new ConfigItem("通票分账", "distribution_method", "各酒吧分配方式",
                configRepository.findDistributionMethod(null)));
        // S9:超池特批(平台负责人短期激励)有效期,到期自动失效不再允许超池过账。
        //   V368:期限带单位(3 天 / 3 个月),总览直接显示带单位的写法,免得看的人还要猜单位。
        defaults.add(new ConfigItem("通票分账", "special_approval_term", "超池特批有效期",
                configRepository.findSpecialApprovalTerm(null).label()));
        var svcFee = configRepository.findComprehensiveServiceFee(null, null, null);
        defaults.add(new ConfigItem("门票综合服务费", "comprehensive_service_fee", "综合服务费(比例或固定额)",
                svcFee.fixedAmountCent() != null ? "固定额 " + svcFee.fixedAmountCent() + " 分" : svcFee.feeBps() + " 万分比"));
        // S6:赠送权益预估成本预扣(池前扣、归娱乐平台公司)。平台默认生效值:两模式二选一。
        var giftCost = configRepository.findPassGiftCostConfig(null, null);
        defaults.add(new ConfigItem("赠送权益预估成本", "pass_gift_cost", "预扣模式(按件计/按票计)",
                "by_item".equals(giftCost.predeductMode())
                        ? "按件计(票面各权益数量 × 内部结算价之和)"
                        : "按票计 整额 " + giftCost.ticketGiftAmountCent() + " 分"
                                + (giftCost.monthlyCapAmountCent() == null ? ""
                                        : ",每月封顶 " + giftCost.monthlyCapAmountCent() + " 分")));
        JdbcSettlementBillRepository.ComboDiscountConfig combo = configRepository.findComboDiscountConfig(null);
        defaults.add(new ConfigItem("通票分账", "combo_discount_bearer", "套票折扣归属",
                combo.discountBearer()));
        // V369:综合/联合通票「凑整允许误差」。运营给通票填的近似售价与系统算出售价差多少还算凑整、
        // 差多少就是填错了——超出这个数的票一律不许开卖。与折扣归属同一行配置(凑整差额走同一条承担口径)。
        defaults.add(new ConfigItem("通票分账", "rounding_tolerance_cent", "通票凑整允许误差(分)",
                String.valueOf(combo.roundingToleranceCent())));
        // V340(P0-D/CFG-01):提成/积分两条原写死口径纳入总览「当前生效值」,与其它配置同源(只读生效行)。
        defaults.add(new ConfigItem("提成与积分", "commission_rate_sales", "卡座提成率·销售人员(万分比)",
                String.valueOf(commissionPointsConfigRepository.findCommissionRateBp(null, null, "sales"))));
        defaults.add(new ConfigItem("提成与积分", "commission_rate_non_sales", "卡座提成率·非销售人员(万分比)",
                String.valueOf(commissionPointsConfigRepository.findCommissionRateBp(null, null, "non_sales"))));
        defaults.add(new ConfigItem("提成与积分", "points_amount_cent_per_point", "积分累计率(每积1分所需消费·分)",
                String.valueOf(commissionPointsConfigRepository.findAmountCentPerPoint(null, null))));
        // V350:全平台「高价值物品」判定阈值(退酒/报损/盘点差异/存酒作废共用一个值,比的是物品进货价)。
        // 未配置时不报错、如实显示「未配置」——判定侧此时一律按高价值拦下走审批(业务HTML 口径),
        // 概览页照常打开,运营看得见「现在是未配置状态、所有单子都在被拦」才知道要去补。
        Long highValueThresholdCent = highValueThresholdRepository.findHighValueThresholdCent(null, null);
        defaults.add(new ConfigItem("高价值判定", "high_value_threshold_cent",
                "高价值判定阈值(物品进货价高于此值即高价值·分)",
                highValueThresholdCent == null
                        ? "未配置(一律按高价值拦下走审批)"
                        : String.valueOf(highValueThresholdCent)));

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
    /**
     * 批10/E:内部结算 SKU 目录三维筛选列表(商家/业态/品类)。权限闸在 controller
     * (settlement.config.view 或 V279/V301 内部价读脱敏点任一;商户 scope 已被 controller 收窄为本商户)。
     */
    public List<JdbcSettlementConfigWriteRepository.CatalogEntryRow> listInternalPriceItems(
            String merchantId, String businessFormat, String category,
            boolean includeDrafts, boolean includeArchived) {
        return writeRepository.listCatalogEntries(merchantId, businessFormat, category,
                includeDrafts, includeArchived);
    }

    public record ConfigItem(String group, String key, String label, String value) {
    }

    /** 待复核草案条目(展示用)。 */
    public record DraftItem(String id, String configGroup, String summary) {
    }
}
