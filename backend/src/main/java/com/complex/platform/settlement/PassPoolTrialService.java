package com.complex.platform.settlement;

// 文件版本：V0.01

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.money.MoneyRounding;
import com.complex.platform.ticketing.EntitlementTemplateRecord;
import com.complex.platform.ticketing.InternalSettlementPriceResolver;
import com.complex.platform.ticketing.TicketOrderService;
import com.complex.platform.ticketing.TicketPricingService;
import com.complex.platform.ticketing.TicketTypeEntitlementRecord;
import com.complex.platform.ticketing.TicketingRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 第 75 条：第一段固定金额或固定单价生效前，按各受影响票种在当前各项配置下的最低可能可分配池金额试算。
 *
 * <p>依据：ENUM L52「第一段的固定金额或固定单价在配置生效之前，必须先按该票种在当前各项配置下的最低可能可分配池金额试算（即最不利的一张票：
 * 按扣点最高的销售渠道、按费率最高的支付通路计取通路费、按门票综合服务费足额计取、按当前赠送权益预估成本与当前最低实付价组合计算），
 * 确认第一段应分合计不超过该最低池金额的百分之百，试算不通过的配置无效、不得生效」；BSM L752 同义；DEC-P1A-09「足额计取」＝通票服务费不封顶。
 *
 * <p>公式逐项照售票池快照（{@link PassPoolSnapshotService#onIssued}），只把实付价与费率换成最不利值：实付价取当前最低实付价，
 * 按出票同一算法拆到各权益（{@link TicketOrderService#allocateRevenue}）；通路费按费率最高的支付通路；销售渠道费取各第三方渠道里最高的一档
 * （现有票种对下单渠道不限售，官方渠道不扣）；赠送权益预估成本与门票综合服务费按该票种当前配置。第一段按每份计（每人每次入场一份），
 * 与逐份结算同口径，故比的是按份均分后最小的那一份池。
 */
@Service
public class PassPoolTrialService {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcSettlementBillRepository bills;
    private final TicketingRepository ticketing;
    private final TicketPricingService pricing;
    private final InternalSettlementPriceResolver priceResolver;
    private final BusinessDayResolver businessDays;

    public PassPoolTrialService(NamedParameterJdbcTemplate jdbc, JdbcSettlementBillRepository bills, TicketingRepository ticketing,
                                TicketPricingService pricing, InternalSettlementPriceResolver priceResolver,
                                BusinessDayResolver businessDays) {
        this.jdbc = jdbc;
        this.bills = bills;
        this.ticketing = ticketing;
        this.pricing = pricing;
        this.priceResolver = priceResolver;
        this.businessDays = businessDays;
    }

    /** 一个受试票种的试算结果（分）：最低实付价、最不利一份可分配池、本版第一段对该票种的应分合计。 */
    public record TicketTypeTrial(String ticketTypeId, String ticketName, String projectId, long lowestPriceCent,
                                  long lowestUnitPoolCent, long firstSegmentCent, boolean passed) {

        public long excessCent() {
            return Math.max(0L, firstSegmentCent - lowestUnitPoolCent);
        }
    }

    /**
     * 受试票种：可售（已发布、未存档、未待删）的通票（非单店票）中，入场类权益（模板未存档、非专属、提供方为酒吧）覆盖本版第一段名单任一酒吧的；
     * 范围同方案通告的受影响酒吧——项目版限本项目酒吧；全平台版不含所在项目已有生效营业日不晚于本版生效营业日、且未凭激励转正的本项目版的酒吧
     * （本版此刻尚未写生效日，按传入的 effectiveDate 比；凭激励转正的本项目版不算——激励一到期新售票就落回此前方案，没有本项目上一版即本版，
     * 裁决第 12 条）。
     * 不覆盖第一段任何酒吧的票种第一段合计为 0，不列。
     */
    public List<TicketTypeTrial> trial(String versionProjectId, LocalDate effectiveDate,
                                       List<DistributionSchemeService.FirstSegmentRow> firstSegment) {
        if (firstSegment.isEmpty()) {
            return List.of();
        }
        Map<String, Long> amounts = new LinkedHashMap<>();
        firstSegment.forEach(f -> amounts.put(f.merchantId(), f.amountCent()));
        Map<String, String> names = new LinkedHashMap<>();
        Map<String, Long> firstSegmentCent = new LinkedHashMap<>();
        jdbc.query("""
                SELECT DISTINCT tt.id, tt.ticket_name, et.merchant_id
                  FROM ticket_types tt
                  JOIN ticket_type_entitlements tte ON tte.ticket_type_id = tt.id
                  JOIN entitlement_templates et ON et.id = tte.entitlement_template_id
                       AND et.archived_at IS NULL AND et.exclusive_merchant_id IS NULL
                  JOIN merchants m ON m.id = et.merchant_id AND m.merchant_type = 'bar'
                 WHERE tt.status = 'published' AND tt.archived_at IS NULL AND tt.delete_status = 'none' AND tt.bar_id IS NULL
                   AND et.entitlement_type IN ('entry', 'admission')
                   AND et.merchant_id IN (:bars)
                   AND (:project_id::text IS NULL OR m.project_id = :project_id)
                   AND (:project_id::text IS NOT NULL OR NOT EXISTS (
                        SELECT 1 FROM ticket_distribution_scheme_versions pv
                         WHERE pv.project_id = m.project_id AND pv.status = 'notified'
                           AND (pv.effective_at AT TIME ZONE :zone)::date <= :effective_date
                           AND %s = 'infinity'::date))
                 ORDER BY 1, 3
                """.formatted(DistributionSchemeService.incentiveExpiryDate("pv")),
                new MapSqlParameterSource().addValue("bars", amounts.keySet()).addValue("project_id", versionProjectId)
                        .addValue("zone", LocalZone.ID.getId()).addValue("effective_date", java.sql.Date.valueOf(effectiveDate)),
                rs -> {
                    names.put(rs.getString("id"), rs.getString("ticket_name"));
                    firstSegmentCent.merge(rs.getString("id"), amounts.get(rs.getString("merchant_id")), Long::sum);
                });
        List<TicketTypeTrial> out = new ArrayList<>();
        firstSegmentCent.forEach((type, q) -> out.add(trialOf(type, names.get(type), versionProjectId, q)));
        return out;
    }

    private TicketTypeTrial trialOf(String ticketTypeId, String ticketName, String versionProjectId, long q) {
        // 项目：与售票同口径取票种唯一所属综合体；取不到唯一值时用本版项目（全平台版为空＝按全平台默认解析配置）。
        List<String> projects = ticketing.listTicketTypeProjectIds(ticketTypeId);
        String p = projects.size() == 1 ? projects.getFirst() : versionProjectId;
        long price = pricing.lowestUnitPriceCent(ticketTypeId, businessDays.businessDate(p, null, Instant.now()));
        List<TicketTypeEntitlementRecord> bindings = ticketing.listTicketTypeEntitlements(ticketTypeId);
        long[] alloc = TicketOrderService.allocateRevenue(bindings, price);
        long[] full = TicketOrderService.componentFullPrices(bindings);
        long barAlloc = 0L;
        long discount = 0L;
        int people = 1;
        int times = 1;
        int idx = 0;
        for (TicketTypeEntitlementRecord binding : bindings) {
            int n = Math.max(1, binding.numPeople());
            people = Math.max(people, n);
            times = Math.max(times, binding.totalTimes());
            EntitlementTemplateRecord template = ticketing.findEntitlementTemplateById(binding.entitlementTemplateId()).orElseThrow();
            // 酒吧份额＝酒吧业态非专属权益的出票分摊额（快照同 COALESCE(merchant_type,'bar')）。
            boolean pooledBar = template.exclusiveMerchantId() == null && TicketDistributionSettlementService.FORMAT_BAR.equals(
                    jdbc.queryForObject("SELECT COALESCE((SELECT merchant_type FROM merchants WHERE id = :id), 'bar')",
                            new MapSqlParameterSource("id", template.merchantId()), String.class));
            for (int i = 0; i < binding.quantity(); i++, idx++) {
                if (!pooledBar) {
                    continue;
                }
                long[] revenue = TicketOrderService.allocateEvenly(alloc[idx], n);
                long[] fullPrice = full == null ? null : TicketOrderService.allocateEvenly(full[idx], n);
                for (int k = 0; k < n; k++) {
                    barAlloc += revenue[k];
                    if (fullPrice != null) {
                        discount += Math.max(0L, fullPrice[k] - revenue[k]);
                    }
                }
            }
        }
        long borne = ComprehensiveServiceFee.platformBorneCent(bills.findComboDiscountConfig(p), discount);
        long gross = barAlloc + borne;
        long route = price == 0L ? 0L : MoneyRounding.prorate(MoneyRounding.bpsOf(price, bills.findMaxRouteFeeBps(p)), barAlloc, price);
        long sales = 0L;
        for (String channel : TicketOrderService.THIRD_PARTY_SALES_CHANNELS) {
            JdbcSettlementBillRepository.ChannelFeeConfig cfg = bills.findChannelFee(p, channel);
            sales = Math.max(sales, cfg.fixedAmountCent() != null
                    ? (price == 0L ? 0L : MoneyRounding.prorate(cfg.fixedAmountCent(), barAlloc, price))
                    : MoneyRounding.bpsOf(barAlloc, cfg.feeBps()));
        }
        var giftCfg = bills.findPassGiftCostConfig(p, ticketTypeId);
        long gift = "by_item".equals(giftCfg.predeductMode())
                ? priceResolver.byItemGiftCostCent(ticketTypeId)
                : (giftCfg.ticketGiftAmountCent() == null ? 0L : giftCfg.ticketGiftAmountCent());
        long service = ComprehensiveServiceFee.perTicketCent(bills.findComprehensiveServiceFee(p, null, ticketTypeId), gross, 1);
        int units = people * times;
        long lowest = Long.MAX_VALUE;
        for (int i = 0; i < units; i++) {
            lowest = Math.min(lowest, TicketDistributionSettlementService.share(gross, units, i)
                    - TicketDistributionSettlementService.share(route, units, i)
                    - TicketDistributionSettlementService.share(sales, units, i)
                    - TicketDistributionSettlementService.share(gift, units, i)
                    - TicketDistributionSettlementService.share(service, units, i));
        }
        return new TicketTypeTrial(ticketTypeId, ticketName, p, price, lowest, q, q <= lowest);
    }
}
