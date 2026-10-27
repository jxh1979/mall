package com.complex.platform.settlement;

// 文件版本：V0.01

import com.complex.platform.common.money.MoneyRounding;
import com.complex.platform.ticketing.TicketOrderService;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 第十八批第 3 条：票务订单的两层渠道费——付款成功时按实际支付通路固化通路费、按订单销售渠道固化销售渠道费所用配置；
 * 核销与到期时通路费按金额占比分摊。
 *
 * <p>依据：BSM L749「通路费（银行、微信、支付宝等支付通路收取）所有支付都会产生、官方渠道同样产生……两者一律在拆分给各域之前
 * 从整单扣除且只扣一次，由各域按分配份额自然共担……每笔分摊须记录所用合同版本」；BSM L1757「多方分配产生的非涉税尾差单独留痕」；
 * ENUM L86「储值卡充值时即产生通路费，按该笔充值实际使用的支付通路记录金额与费率」。
 */
@Component
public class RouteFeeShares {

    static final String STORED_VALUE_CHANNEL = "stored_value";

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcSettlementBillRepository billRepository;

    public RouteFeeShares(NamedParameterJdbcTemplate jdbc, JdbcSettlementBillRepository billRepository) {
        this.jdbc = jdbc;
        this.billRepository = billRepository;
    }

    /**
     * 一笔付款成功（同一事务内）固化其通路费：储值笔＝该笔消费在各充值批次分摊的通路费之和（充值时已产生，不再按费率另算）；
     * 其它笔＝付款额 × 该支付通路在订单项目下的生效通路费率（未配置＝0），记所用配置行。同一笔重放不改已固化行。
     * 本单销售渠道费所用配置同时固化（见 {@link #fixSalesChannelFee}）。
     */
    public void fix(String paymentIntentId, String orderId, String projectId, String paymentChannel, long amountCent) {
        Integer feeBps = null;
        String configId = null;
        long feeCent;
        if (STORED_VALUE_CHANNEL.equals(paymentChannel)) {
            feeCent = jdbc.queryForObject(
                    "SELECT COALESCE(SUM(fee_cent), 0) FROM stored_value_consume_allocations WHERE consume_txn_id = :id",
                    new MapSqlParameterSource("id", paymentIntentId), Long.class);
        } else {
            JdbcSettlementBillRepository.RouteFee fee = billRepository.findRouteFee(projectId, paymentChannel);
            feeBps = fee.feeBps();
            configId = fee.configId();
            feeCent = MoneyRounding.bpsOf(amountCent, fee.feeBps());
        }
        jdbc.update("""
                INSERT INTO ticket_payment_route_fees
                    (payment_intent_id, order_id, project_id, payment_channel, amount_cent, fee_bps, fee_cent, config_id)
                VALUES (:payment_intent_id, :order_id, :project_id, :payment_channel, :amount_cent, :fee_bps, :fee_cent, :config_id)
                ON CONFLICT (payment_intent_id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("payment_intent_id", paymentIntentId)
                .addValue("order_id", orderId)
                .addValue("project_id", projectId)
                .addValue("payment_channel", paymentChannel)
                .addValue("amount_cent", amountCent)
                .addValue("fee_bps", feeBps, java.sql.Types.INTEGER)
                .addValue("fee_cent", feeCent)
                .addValue("config_id", configId));
        fixSalesChannelFee(orderId);
    }

    /**
     * 本单首笔受理付款时固化销售渠道费所用配置（之后改配置不回改，核销、到期与售票池快照都按这一版算）：官方渠道与未标渠道不扣、不记；
     * 第三方渠道按订单项目与销售渠道解析当时生效的配置（比例或每张票固定金额），记配置行 id。
     */
    public void fixSalesChannelFee(String orderId) {
        Map<String, Object> order = jdbc.queryForList("SELECT project_id, sales_channel FROM ticket_orders WHERE id = :id",
                new MapSqlParameterSource("id", orderId)).stream().findFirst().orElse(null);
        String channel = order == null ? null : com.complex.platform.common.Strings.trimToNull((String) order.get("sales_channel"));
        if (channel == null || TicketOrderService.OFFICIAL_SALES_CHANNELS.contains(channel)) {
            return;
        }
        String projectId = (String) order.get("project_id");
        JdbcSettlementBillRepository.ChannelFeeConfig cfg = billRepository.findChannelFee(projectId, channel);
        jdbc.update("""
                INSERT INTO ticket_order_channel_fees (order_id, project_id, sales_channel, sales_fee_config_id, sales_fee_bps,
                    sales_fixed_amount_cent, sales_fixed_at)
                VALUES (:order_id, :project_id, :sales_channel, :config_id, :fee_bps, :fixed_amount_cent, now())
                ON CONFLICT (order_id) DO UPDATE SET sales_channel = EXCLUDED.sales_channel,
                    sales_fee_config_id = EXCLUDED.sales_fee_config_id, sales_fee_bps = EXCLUDED.sales_fee_bps,
                    sales_fixed_amount_cent = EXCLUDED.sales_fixed_amount_cent, sales_fixed_at = EXCLUDED.sales_fixed_at
                 WHERE ticket_order_channel_fees.sales_fixed_at IS NULL
                """, new MapSqlParameterSource()
                .addValue("order_id", orderId)
                .addValue("project_id", projectId)
                .addValue("sales_channel", channel)
                .addValue("config_id", cfg.id())
                .addValue("fee_bps", cfg.feeBps())
                .addValue("fixed_amount_cent", cfg.fixedAmountCent(), java.sql.Types.BIGINT));
    }

    /**
     * 本单某一部分（一次核销额、一份未核销额、一张票的酒吧毛额）应分摊的通路费，按本单累计额差分：
     * 本部分＝round(合计 ×（已分摊部分＋本部分）÷ 实付) − round(合计 × 已分摊部分 ÷ 实付)。本单各部分分摊完后合计恰为固化的通路费，
     * 不留尾差。每一部分只调用一次（调用即计入已分摊部分）。本单无固化行（无付款、存量订单）＝0。
     */
    public long shareOf(String orderId, long partCent) {
        if (orderId == null || partCent == 0L) {
            return 0L;
        }
        Map<String, Object> totals = jdbc.queryForMap("""
                SELECT COALESCE(SUM(fee_cent), 0) AS fee, COALESCE(SUM(amount_cent), 0) AS paid, MAX(project_id) AS project_id
                  FROM ticket_payment_route_fees WHERE order_id = :order_id
                """, new MapSqlParameterSource("order_id", orderId));
        long fee = ((Number) totals.get("fee")).longValue();
        long paid = ((Number) totals.get("paid")).longValue();
        if (paid == 0L) {
            return 0L;
        }
        long after = jdbc.queryForObject("""
                INSERT INTO ticket_order_channel_fees (order_id, project_id, route_allocated_part_cent)
                VALUES (:order_id, :project_id, :part)
                ON CONFLICT (order_id) DO UPDATE
                   SET route_allocated_part_cent = ticket_order_channel_fees.route_allocated_part_cent + EXCLUDED.route_allocated_part_cent
                RETURNING route_allocated_part_cent
                """, new MapSqlParameterSource().addValue("order_id", orderId)
                .addValue("project_id", totals.get("project_id")).addValue("part", partCent), Long.class);
        return MoneyRounding.prorate(fee, after, paid) - MoneyRounding.prorate(fee, after - partCent, paid);
    }
}
