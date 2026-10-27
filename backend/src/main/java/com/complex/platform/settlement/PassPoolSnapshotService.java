package com.complex.platform.settlement;

// 文件版本：V0.01

import com.complex.platform.common.money.MoneyRounding;
import com.complex.platform.ticketing.InternalSettlementPriceResolver;
import com.complex.platform.ticketing.TicketRewardFacts;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 第十八批第 2 条：酒吧通票可分配池售票时逐张固化（与出票同一事务）。
 *
 * <p>依据：ENUM L52「第一步从整单扣一次通路费，经第三方渠道售出的再从整单扣一次销售渠道费……第二步按各业态价格占比拆出各域份额……
 * 第三步仅对酒吧份额继续扣减——先扣赠送权益预估成本……再扣门票综合服务费……第四步，酒吧份额扣完上述两项后的剩余，即为酒吧通票
 * 可分配池，池金额在售票时随订单固化」「变更只对通告生效后新售出的票适用，已售出的票一律按售票时固化的第一段名单与固定额结算」；
 * BSM L755「酒吧通票和跨域组合通票中的酒吧部分仍按原不封顶逐票规则处理」（综合服务费不套月度封顶）。
 *
 * <p>酒吧份额＝该票酒吧业态非专属权益的出票分摊额（{@code issued_entitlements.allocated_revenue_cent}，即按各业态价格占比拆出的份额）
 * 加项目承担的套票折扣；通路费按本单付款时固化额按份额分摊；销售渠道费仅第三方渠道（官方渠道不扣），按该票酒吧份额占比计；
 * 覆盖酒吧＝该票酒吧业态入场类权益（entry/admission）的提供方。单一酒吧门票（订单挂酒吧）与不覆盖任何酒吧的票不建快照。
 */
@Service
public class PassPoolSnapshotService {

    private static final Set<String> ENTRY_TYPES = Set.of("entry", "admission");

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcSettlementBillRepository bills;
    private final RouteFeeShares routeFeeShares;
    private final InternalSettlementPriceResolver priceResolver;
    private final DistributionSchemeService schemes;

    public PassPoolSnapshotService(NamedParameterJdbcTemplate jdbc, JdbcSettlementBillRepository bills,
                                   RouteFeeShares routeFeeShares, InternalSettlementPriceResolver priceResolver,
                                   DistributionSchemeService schemes) {
        this.jdbc = jdbc;
        this.bills = bills;
        this.routeFeeShares = routeFeeShares;
        this.priceResolver = priceResolver;
        this.schemes = schemes;
    }

    @EventListener
    public void onIssued(TicketRewardFacts.Issued fact) {
        Map<String, Object> order = jdbc.queryForList("""
                SELECT project_id, bar_id, ticket_type_id, paid_at FROM ticket_orders WHERE id = :id
                """, new MapSqlParameterSource("id", fact.orderId())).stream().findFirst().orElse(null);
        if (order == null || order.get("bar_id") != null) {
            return; // 单一酒吧门票按 FS-013 售出月逐票计费，不进通票池。
        }
        String project = (String) order.get("project_id");
        String ticketType = (String) order.get("ticket_type_id");
        Instant soldAt = order.get("paid_at") == null ? Instant.now() : ((Timestamp) order.get("paid_at")).toInstant();
        String version = null;
        for (Map<String, Object> ticket : jdbc.queryForList(
                "SELECT id, num_people, total_times FROM ticket_instances WHERE order_id = :id ORDER BY seq_in_order, id",
                new MapSqlParameterSource("id", fact.orderId()))) {
            String ticketId = (String) ticket.get("id");
            long ticketAlloc = 0L;
            long barAlloc = 0L;
            long discount = 0L;
            Set<String> covered = new LinkedHashSet<>();
            for (Map<String, Object> e : jdbc.queryForList("""
                    SELECT e.merchant_id, e.entitlement_type, e.allocated_revenue_cent, e.component_full_price_cent,
                           e.exclusive_merchant_id, COALESCE(m.merchant_type, 'bar') AS merchant_type
                      FROM issued_entitlements e LEFT JOIN merchants m ON m.id = e.merchant_id
                     WHERE e.parent_ticket_id = :t
                    """, new MapSqlParameterSource("t", ticketId))) {
                long alloc = e.get("allocated_revenue_cent") == null ? 0L : ((Number) e.get("allocated_revenue_cent")).longValue();
                ticketAlloc += alloc;
                if (!TicketDistributionSettlementService.FORMAT_BAR.equals(e.get("merchant_type"))
                        || e.get("exclusive_merchant_id") != null) {
                    continue;
                }
                barAlloc += alloc;
                if (e.get("component_full_price_cent") != null) {
                    discount += Math.max(0L, ((Number) e.get("component_full_price_cent")).longValue() - alloc);
                }
                if (ENTRY_TYPES.contains(e.get("entitlement_type"))) {
                    covered.add((String) e.get("merchant_id"));
                }
            }
            if (covered.isEmpty()) {
                continue; // 该票不覆盖任何酒吧：没有酒吧通票可分配池。
            }
            if (version == null) {
                version = schemes.effectiveVersionId(project, soldAt);
            }
            long borne = ComprehensiveServiceFee.platformBorneCent(bills.findComboDiscountConfig(project), discount);
            long barGross = barAlloc + borne;
            long routeFee = routeFeeShares.shareOf(fact.orderId(), barAlloc);
            // 销售渠道费按本单付款时固化的配置（官方渠道与无固化行＝不扣），与月结、到期同一版。
            JdbcSettlementBillRepository.ChannelFeeConfig salesFee = bills.findOrderSalesChannelFee(fact.orderId());
            long salesChannelFee = salesFee.id() == null ? 0L
                    : salesFee.fixedAmountCent() != null
                            ? (ticketAlloc == 0L ? 0L : MoneyRounding.prorate(salesFee.fixedAmountCent(), barAlloc, ticketAlloc))
                            : MoneyRounding.bpsOf(barAlloc, salesFee.feeBps());
            var giftCfg = bills.findPassGiftCostConfig(project, ticketType);
            long giftCost = "by_item".equals(giftCfg.predeductMode())
                    ? priceResolver.byItemGiftCostCent(ticketType)
                    : (giftCfg.ticketGiftAmountCent() == null ? 0L : giftCfg.ticketGiftAmountCent());
            var serviceFee = bills.findComprehensiveServiceFee(project, null, ticketType);
            long serviceFeeCent = ComprehensiveServiceFee.perTicketCent(serviceFee, barGross, 1);
            int inserted = jdbc.update("""
                    INSERT INTO pass_pool_snapshots (ticket_instance_id, order_id, project_id, ticket_type_id, slots, visits,
                        bar_gross_cent, platform_borne_cent, route_fee_cent, sales_channel_fee_cent, sales_channel_fee_config_id,
                        gift_cost_cent, gift_cost_config_id, service_fee_cent, service_fee_config_id, pool_cent,
                        scheme_version_id, covered_bar_ids)
                    VALUES (:t, :o, :p, :tt, :slots, :visits, :gross, :borne, :route, :sales, :sales_cfg, :gift, :gift_cfg,
                        :svc, :svc_cfg, :pool, :v, CAST(:covered AS text[]))
                    ON CONFLICT (ticket_instance_id) DO NOTHING
                    """, new MapSqlParameterSource().addValue("t", ticketId).addValue("o", fact.orderId())
                    .addValue("p", project).addValue("tt", ticketType)
                    .addValue("slots", Math.max(1, ((Number) ticket.get("num_people")).intValue()))
                    .addValue("visits", Math.max(1, ((Number) ticket.get("total_times")).intValue()))
                    .addValue("gross", barGross).addValue("borne", borne).addValue("route", routeFee)
                    .addValue("sales", salesChannelFee).addValue("sales_cfg", salesFee.id())
                    .addValue("gift", giftCost).addValue("gift_cfg", giftCfg.id())
                    .addValue("svc", serviceFeeCent).addValue("svc_cfg", serviceFee.id())
                    .addValue("pool", barGross - routeFee - salesChannelFee - giftCost - serviceFeeCent).addValue("v", version)
                    .addValue("covered", "{" + String.join(",", covered) + "}"));
            if (inserted == 0) {
                continue;
            }
            for (DistributionSchemeService.FirstSegmentRow row : schemes.firstSegment(version)) {
                if (covered.contains(row.merchantId())) {
                    jdbc.update("""
                            INSERT INTO pass_pool_snapshot_first_segment (ticket_instance_id, merchant_id, rule_type, amount_cent)
                            VALUES (:t, :m, :r, :a)
                            """, new MapSqlParameterSource().addValue("t", ticketId).addValue("m", row.merchantId())
                            .addValue("r", row.ruleType()).addValue("a", row.amountCent()));
                }
            }
        }
    }

    /** 整张退款成功（与确认拨付同一事务）：该父票池快照作废，暂挂的份不再续算。 */
    @EventListener
    public void onRefunded(TicketRewardFacts.Refunded fact) {
        jdbc.update("""
                UPDATE pass_pool_snapshots ps SET status = 'void'
                 WHERE ps.status = 'active'
                   AND ps.ticket_instance_id IN (SELECT e.parent_ticket_id FROM ticket_refund_lines l
                                                   JOIN issued_entitlements e ON e.id = l.entitlement_id
                                                  WHERE l.refund_id = :refund)
                   AND EXISTS (SELECT 1 FROM ticket_instances ti WHERE ti.id = ps.ticket_instance_id AND ti.status = 'refunded')
                """, new MapSqlParameterSource("refund", fact.refundId()));
    }

    /** 该父票的酒吧份额是否已在售票时固化进池（是则核销时不再逐笔计提综合服务费、赠送权益预估成本与酒吧核销的通路费）。 */
    boolean pooledAtSale(String parentTicketId) {
        return parentTicketId != null && Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM pass_pool_snapshots WHERE ticket_instance_id = :t)",
                new MapSqlParameterSource("t", parentTicketId), Boolean.class));
    }
}
