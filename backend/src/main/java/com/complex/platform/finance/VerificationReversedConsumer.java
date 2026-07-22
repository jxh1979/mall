package com.complex.platform.finance;

import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.ticketing.VerificationReversedEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import static com.complex.platform.common.Strings.trimToNull;

/**
 * P1 清单#19 跨模块闭环②b：finance 侧消费 ticketing 的 {@link VerificationReversedEvent}（核销已冲正权威信号），
 * 同步 {@link EventListener} 与 ticketing 核销冲正主链同事务（与 BarGiftConsumer / LoyaltyPointsConsumer 风格一致）。
 *
 * <p><b>本消费者做什么</b>：把"某核销已冲正"这一事实在 finance 侧落一条审计留痕
 * （{@code finance.verification_reversed_notified}，复用 {@link AuditEventService#recordSuccess}），
 * 记录 verificationId / reversalId / entitlementId / orderId / merchantId / verifiedPeople，
 * 供审计追溯（可检索"核销已冲正"在 finance 侧的可见性）。
 *
 * <p><b>正式闭环决策（已评估收口，非待办）</b>：核销冲正在 finance 侧<b>无独立财务冲销对象</b>，
 * 本消费者落审计留痕即为完整闭环，不发起任何退款、不写任何冲正金额。依据如下：
 * <ul>
 *   <li><b>收入源自订单支付，核销不直接生成可冲正财务台账</b>：平台净收入
 *       revenue.net = 自营售票（ticket_orders.paid）+ 酒吧（bar_bill_settlements）− 退款，
 *       由订单/结算聚合派生（见 JdbcFinanceRepository 六源日结聚合）。
 *       游乐核销 amusement_verifications 在日结里只计核销/客流量（{@code 0 AS amount_cent}），不计任何收入。
 *       ar_ap_items 的来源是营业额抽成（turnover_rent）/ CAM / 保证金（deposit）/ 库存差异（inventory_difference）/
 *       订单（order）等聚合或独立口径，<b>没有"一次核销 → 一条可冲正台账行"的映射</b>，
 *       据 verificationId/orderId 也无法定位唯一可冲销的财务行。故核销冲正不改 revenue、无对象可冲销。</li>
 *   <li><b>权益回补由 ticketing 处理，退款另有独立审批链</b>：核销冲正本质是权益履约的回退（再核销权），
 *       由 ticketing 域在核销冲正主链内完成；money 侧若需退款须走 finance 独立审批链
 *       （{@code finance.refund.create} → 审批），自动发起退款会绕过审批并可能与既有退款重复打款。
 *       因此本消费者不触发退款，也不写冲正金额（避免伪造财务冲销）。</li>
 * </ul>
 * 结论：<b>finance 仅留痕供审计追溯</b>，此为该事件在 finance 侧的正式且完整的处理方式。
 *
 * <p><b>幂等</b>：以 reversalId 为幂等键，经 {@link IdempotencyService} 去重——同一 reversalId 仅落一次留痕，
 * 重复事件第二次命中已完成记录直接返回缓存结果（不重复写审计）。
 */
@Component
public class VerificationReversedConsumer {

    /** 审计事件名：核销冲正已通知 finance。 */
    static final String EVENT_VERIFICATION_REVERSED_NOTIFIED = "finance.verification_reversed_notified";
    /** 留痕沿用结算冲正联动权限码（与 ticketing T7 结算冲正对接点同口径）。 */
    static final String PERMISSION_CODE = "finance.settlement.reverse";
    static final String BUSINESS_TYPE = "finance_verification_reversal";
    /** 幂等 operationId（与 reversalId 一起构成去重键）。 */
    static final String OPERATION_ID = "finance.verificationReversed";

    private final AuditEventService auditEventService;
    private final IdempotencyService idempotencyService;

    public VerificationReversedConsumer(
            AuditEventService auditEventService,
            IdempotencyService idempotencyService) {
        this.auditEventService = auditEventService;
        this.idempotencyService = idempotencyService;
    }

    @EventListener
    public void onVerificationReversed(VerificationReversedEvent event) {
        if (event == null) {
            return;
        }
        String reversalId = trimToNull(event.reversalId());
        if (reversalId == null) {
            // 无冲正记录 id：无法构成幂等锚点，跳过（不阻断核销冲正主链）。
            return;
        }

        // 幂等：同一 reversalId 仅处理一次。重复事件第二次命中已完成记录 → 返回缓存，不重复落审计。
        idempotencyService.execute(
                OPERATION_ID,
                reversalId,
                event,
                () -> recordNotice(event, reversalId),
                Boolean.class);
    }

    /**
     * 落一条冲正通知留痕。明确<b>不</b>发起退款、<b>不</b>写任何冲正金额——这是正式闭环决策，
     * 非待办：核销冲正在 finance 侧无独立财务冲销对象（收入源自订单支付，权益回补由 ticketing 处理），
     * finance 仅留痕供审计追溯。见类注释"正式闭环决策"。
     * 返回布尔占位仅为满足幂等存储的响应序列化（无业务含义）。
     */
    private Boolean recordNotice(VerificationReversedEvent event, String reversalId) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("verification_id", nullToEmpty(event.verificationId()));
        metadata.put("reversal_id", reversalId);
        metadata.put("entitlement_id", nullToEmpty(event.entitlementId()));
        metadata.put("order_id", nullToEmpty(event.orderId()));
        metadata.put("merchant_id", nullToEmpty(event.merchantId()));
        metadata.put("verified_people", event.verifiedPeople());
        // 正式闭环决策：本事件在 finance 侧无独立财务冲销对象，仅留痕。
        // action 标识审计可据此区分"已通知/无需冲销"与（其他场景的）"已冲销"。
        metadata.put("action", "notified_no_financial_reversal_object");
        metadata.put("note", "verification reversal notified to finance; settled as a deliberate closed loop: "
                + "no settlement reversal posted and no refund initiated. Verification has no dedicated reversible "
                + "financial ledger item in finance (revenue derives from order payments; amusement verifications "
                + "carry zero revenue in daily close). Entitlement is restored by ticketing; refunds, if any, go "
                + "through finance's independent approval chain. finance keeps this trace for audit only.");

        auditEventService.recordSuccess(
                EVENT_VERIFICATION_REVERSED_NOTIFIED,
                PERMISSION_CODE,
                BUSINESS_TYPE,
                reversalId,
                null,
                reversalId,
                metadata);
        return Boolean.TRUE;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
