package com.complex.platform.settlement;

// 文件版本：V0.01

import com.complex.platform.finance.FinanceService;
import com.complex.platform.merchantbilling.MerchantAmountChangeNotifier;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 第十八批第 13 条：结算金额出账短信。BSM 第 1597 行「结算金额出账与调整……通知中写明金额、原因与异议截止日期……异议期自该账单在
 * 商户专区可查看之日的次日起算」——普通内部结算往来到出账日（visible_at，商户专区可查看）即逐笔通知对方商户，异议截止日取往来固化的
 * objection_deadline。停车抵免出账已由 ParkingCreditService 通知，不重发。去重靠发送日志 request_id，不加列。
 * 操作员裁决（第十八批第 2 条「不逐笔发：只在结算账单出账时发一条，里面已含这些赠饮应付」）：通票赠饮逐杯挂给出品酒吧的应付不逐笔发；
 * 该酒吧本期赠饮应付净额只写进本期一条门票分销出账短信的原因里（同一酒吧同期有几条门票分销往来时取最先出账、编号最小的那条；追收不带）。
 * 逻辑在可直调的 {@link #notifyVisibleStatements(Instant)}；@Scheduled 薄包装，默认关，prod 开（每日一轮）。
 */
@Component
public class MerchantSettlementStatementNoticeJob {

    static final String EVENT = "settlement_statement";

    private final JdbcTemplate jdbc;
    private final MerchantAmountChangeNotifier notifier;
    private final boolean scheduledEnabled;

    public MerchantSettlementStatementNoticeJob(JdbcTemplate jdbc, MerchantAmountChangeNotifier notifier,
            @Value("${settlement.merchant-statement-notice.scheduled.enabled:false}") boolean scheduledEnabled) {
        this.jdbc = jdbc;
        this.notifier = notifier;
        this.scheduledEnabled = scheduledEnabled;
    }

    @ConditionalOnProperty(name = "settlement.merchant-statement-notice.scheduled.enabled", havingValue = "true")
    @Scheduled(cron = "${settlement.merchant-statement-notice.scheduled.cron:0 0 9 * * *}", zone = "Asia/Shanghai")
    public void scheduled() {
        if (scheduledEnabled) {
            notifyVisibleStatements(Instant.now());
        }
    }

    /** 已到出账日、尚未通知的商户往来逐笔通知；返回本轮通知笔数。 */
    public int notifyVisibleStatements(Instant now) {
        record Row(String itemId, String projectId, String merchantId, String route, LocalDate deadline, LocalDate periodStart,
                   long amountCent, String direction, boolean carriesDrinks) {
        }
        var rows = jdbc.query("""
                SELECT s.ar_ap_item_id, s.project_id, s.counterparty_id, s.route, s.objection_deadline, s.billing_period_start,
                       i.amount_cent, i.direction,
                       s.ar_ap_item_id = (SELECT x.ar_ap_item_id FROM internal_payable_schedules x
                                           WHERE x.route = 'ticket_distribution' AND x.counterparty_id = s.counterparty_id
                                             AND x.billing_period_start = s.billing_period_start
                                           ORDER BY x.visible_at, x.ar_ap_item_id LIMIT 1) AS carries_drinks
                  FROM internal_payable_schedules s
                  JOIN ar_ap_items i ON i.id = s.ar_ap_item_id
                  JOIN merchants m ON m.id = s.counterparty_id
                 WHERE s.visible_at <= ? AND s.route <> ?
                   AND NOT EXISTS (SELECT 1 FROM notification_send_log l WHERE l.request_id = 'mamt:settlement_statement:' || s.ar_ap_item_id)
                 ORDER BY s.visible_at, s.ar_ap_item_id
                """, (rs, n) -> new Row(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getObject(5, LocalDate.class), rs.getObject(6, LocalDate.class), rs.getLong(7), rs.getString(8),
                rs.getBoolean(9)), Timestamp.from(now), InternalPayDayService.PARKING_CREDIT_COST);
        for (Row r : rows) {
            String reason = FinanceService.sourceTypeLabel(r.route()) + "出账：" + ("payable".equals(r.direction()) ? "应付商户" : "商户应付");
            if (r.carriesDrinks()) {
                long drinks = jdbc.queryForObject("""
                        SELECT COALESCE(SUM(CASE direction WHEN 'payable' THEN amount_cent ELSE -amount_cent END), 0) FROM ar_ap_items
                         WHERE source_type = 'pass_drink_settlement' AND counterparty_id = ? AND accounting_period = ?
                        """, Long.class, r.merchantId(), YearMonth.from(r.periodStart()).toString());
                if (drinks != 0) {
                    reason += "；另含本期通票赠饮应付 " + BigDecimal.valueOf(drinks, 2).toPlainString() + " 元";
                }
            }
            notifier.notifyAfterCommit(r.merchantId(), EVENT, r.amountCent(), reason, r.deadline(), null, r.projectId(), r.itemId());
        }
        return rows.size();
    }
}
