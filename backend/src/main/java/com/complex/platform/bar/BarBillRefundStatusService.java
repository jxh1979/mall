package com.complex.platform.bar;

// 文件版本：V0.02

import com.complex.platform.finance.RefundPayableSettled;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十六批第 16 条（ENUM L101 / BSM L1059「账单状态 待结、部分支付、已结、部分退款、已退款、异常」）：已结账单按已成功的退款额置
 * 部分退款（partial_refunded）或已退款（refunded，退满实收）。退款成功口径沿用 BSM L1062「只有实际退款成功才记为已退」与
 * TK-04-b：退回储值卡的部分即时成功；其余渠道挂出的退款应付须经财务实付结清（{@link RefundPayableSettled}）才算；
 * 退品待审核或被驳回的退款不算。结算状态仍为 settled，退款、反结等既有判断不变。
 * 第十八批裁决 45：反结时留存的收款（被反结前已登记退款占用、不冲正）不在重结后的实收里，退满按重结实收加留存计。
 */
@Service
public class BarBillRefundStatusService {

    private final JdbcTemplate jdbc;

    public BarBillRefundStatusService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void refresh(String billId) {
        jdbc.update("""
                UPDATE bar_bills b
                   SET payment_status = CASE WHEN r.done <= 0 THEN 'paid'
                                             WHEN r.done >= b.paid_amount_cent + (SELECT COALESCE(SUM(k.settlement_amount_cent), 0)
                                                                FROM bar_bill_settlements k
                                                               WHERE k.bill_id = b.id AND left(k.id, 12) = 'settle_kept_') THEN 'refunded'
                                             ELSE 'partial_refunded' END,
                       updated_at = now()
                  FROM (SELECT COALESCE(SUM(a.amount_cent - CASE WHEN x.status <> 'settled' THEN x.amount_cent ELSE 0 END), 0) AS done
                          FROM bar_bill_adjustments a
                          LEFT JOIN ar_ap_items x ON x.id = a.ar_ap_item_id
                         WHERE a.bill_id = ? AND a.adjustment_type = 'refund' AND a.status = 'applied'
                           AND NOT EXISTS (SELECT 1 FROM bar_order_item_voids v
                                            WHERE v.bill_adjustment_id = a.id AND v.status IN ('pending', 'rejected'))) r
                 WHERE b.id = ? AND b.settlement_status = 'settled'
                   AND b.payment_status IN ('paid', 'partial_refunded', 'refunded')
                """, billId, billId);
    }

    /** 酒吧账单退款挂出的应付经财务实付结清＝该笔退款已实际退出：同一事务回写账单状态。 */
    @EventListener
    @Transactional
    public void onRefundPayableSettled(RefundPayableSettled event) {
        jdbc.queryForList("SELECT bill_id FROM bar_bill_adjustments WHERE id = ?", String.class, event.sourceId())
                .forEach(this::refresh);
    }
}
