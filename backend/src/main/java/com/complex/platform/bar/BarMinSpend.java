package com.complex.platform.bar;

// 文件版本：V0.04

import java.util.List;
import java.util.Optional;

/**
 * 第十批 TK-24-R2 / TK-25：台组（当前有效并台链）最低消费的统一口径。
 *
 * <p>依据 BSM L1079「并台……最低消费额按两侧所包含全部原台相加」「已经发生的消费金额相加作为新合并台的最低消费完成进度」
 * 「合并台组整体转台到目标台后……原台及原并台关系不再参加之后的……数值计算」「原台已经发生的消费金额随台结转」；
 * BSM L1064「最低消费按打折前的订单消费金额累计判定」「结账补足的差额按普通消费计入」；
 * TK-25 留款：退品留在台位账上的金额仍算低消进度，被新账单抵用后不再重复计。
 *
 * <p>复核修复：入口先上溯到当前有效并台链根会话再取台组（money I1）；进度扣减账单级商品退款（退品路径明细已作废，不重复扣），
 * 账单级留款的商品明细本就仍在，不再另加（money B2）。
 */
final class BarMinSpend {

    private BarMinSpend() {
    }

    /** 阈值 = 当前有效并台链各会话低消快照之和（整体转台后的旧链 merged_transferred 不参与）。 */
    static long thresholdCent(BarRepository repository, String sessionId) {
        return repository.familySessionIds(rootSessionId(repository, sessionId), false).stream()
                .map(repository::findTableSessionById)
                .flatMap(Optional::stream)
                .mapToLong(BarTableSessionRecord::minConsumptionSnapshotCent)
                .sum();
    }

    /** 进度 = 链内（含整体转台前旧链）订单未撤明细金额 + 结账补足 + 退品留款 − 留款已抵用。 */
    static long consumedCent(BarRepository repository, String sessionId) {
        List<String> orderIds = repository.listOrderIdsBySessionIds(repository.familySessionIds(rootSessionId(repository, sessionId), true));
        long items = orderIds.stream()
                .flatMap(orderId -> repository.listOrderItems(orderId).stream())
                .filter(item -> !item.voided())
                .mapToLong(BarOrderItemRecord::amountCent)
                .sum();
        return items + adjustmentsCent(repository, orderIds, true);
    }

    /**
     * 留款未抵用余额 = Σ min_spend_retained − Σ min_spend_retained_applied（链内全部账单），不小于 0。
     * ponytail: 留款已被别的账单抵用后原单又作废（或退品驳回）时差额为负，这里只按 0 计、不向新账单加收；这一多条件子情形维持现行处理（第十七批控制席定：不改）。
     */
    static long retainedBalanceCent(BarRepository repository, String sessionId) {
        return Math.max(0L, adjustmentsCent(repository,
                repository.listOrderIdsBySessionIds(repository.familySessionIds(rootSessionId(repository, sessionId), true)), false));
    }

    /**
     * 第十八批 AU 段第 283 条 283-R1（BSM:1064「不足最低消费的要补足」；BSM:1079「各结果台显示差额并按既有规则继续消费或结账补足」）：账单的低消差额（分）
     * ＝ max(0, 阈值 − 进度)，结账闸与订单列表账单摘要共用（原结账闸算法原样搬入）。有开台会话归属的账单按台组阈值与累计进度；
     * 无会话归属的按台位低消对本账单 subtotal＋本账单补足；无台位为 0。
     */
    static long gapCent(BarRepository repository, BarBillRecord bill) {
        if (bill.tableId() == null) {
            return 0L;
        }
        Optional<String> sessionId = bill.orderId() == null ? Optional.empty() : repository.findSessionIdByOrderId(bill.orderId());
        long threshold;
        long consumed;
        if (sessionId.isPresent()) {
            threshold = thresholdCent(repository, sessionId.get());
            consumed = consumedCent(repository, sessionId.get());
        } else {
            threshold = repository.findTableById(bill.tableId()).map(BarTableRecord::minConsumptionCent).orElse(0L);
            consumed = bill.subtotalAmountCent() + repository.listBillAdjustments(bill.id()).stream()
                    .filter(a -> "min_spend_top_up".equals(a.adjustmentType()))
                    .mapToLong(BarBillAdjustmentRecord::amountCent)
                    .sum();
        }
        return threshold > 0 ? Math.max(0L, threshold - consumed) : 0L;
    }

    /**
     * 第十八批 AY 段第 342 条 342-R1（BSM:1064「因此台位上要么没有任何消费（空台，无账可结），要么最终累计消费达到最低消费」）：账单订单所在开台会话的
     * 当前有效链根仍开台时返回该会话；无会话归属或已关台为空。
     */
    static Optional<BarTableSessionRecord> openRootSession(BarRepository repository, BarBillRecord bill) {
        return Optional.ofNullable(bill.orderId())
                .flatMap(repository::findSessionIdByOrderId)
                .map(sessionId -> rootSessionId(repository, sessionId))
                .flatMap(repository::findTableSessionById)
                .filter(session -> "open".equals(session.status()));
    }

    /** 同上：已结账单所在开台会话仍开台且有低消差额时返回差额（分），否则 null（订单列表账单摘要用）。 */
    static Long openSessionGapCent(BarRepository repository, BarBillRecord bill) {
        return openRootSession(repository, bill).map(session -> gapCent(repository, bill)).filter(gap -> gap > 0).orElse(null);
    }

    /** 当前有效并台链根：被并（merged）/ 整体转台前旧链（merged_transferred）会话沿 source_session_id 上溯到链根。 */
    static String rootSessionId(BarRepository repository, String sessionId) {
        String current = sessionId;
        for (int depth = 0; depth < 64; depth++) {
            BarTableSessionRecord session = repository.findTableSessionById(current).orElse(null);
            if (session == null || session.sourceSessionId() == null
                    || !List.of("merged", "merged_transferred").contains(session.status())) {
                return current;
            }
            current = session.sourceSessionId();
        }
        return current;
    }

    /** 退品路径（BarOrderService 撤单）的调整记录 id 前缀：其明细已作废，进度不再按退款额重复扣减。 */
    private static final String ITEM_VOID_ADJUSTMENT_PREFIX = "bill_adjustment_void_";

    /** 退品可退额：退后进度仍达标则全退，否则只退超出阈值的部分（其余留款）。 */
    static long refundableCent(long consumedCent, long thresholdCent, long amountCent) {
        if (consumedCent - amountCent >= thresholdCent) {
            return amountCent;
        }
        return Math.max(0L, Math.min(amountCent, consumedCent - thresholdCent));
    }

    private static long adjustmentsCent(BarRepository repository, List<String> orderIds, boolean includeTopUp) {
        if (orderIds.isEmpty()) {
            return 0L;
        }
        return repository.listBillsByOrderIds(orderIds).stream()
                .flatMap(bill -> repository.listBillAdjustments(bill.id()).stream())
                .filter(adjustment -> "applied".equals(adjustment.status()))
                .mapToLong(adjustment -> {
                    boolean billLevel = !adjustment.id().startsWith(ITEM_VOID_ADJUSTMENT_PREFIX);
                    return switch (adjustment.adjustmentType()) {
                        case "min_spend_top_up" -> includeTopUp ? adjustment.amountCent() : 0L;
                        // 账单级留款：商品明细未作废、已在进度里，进度不另加；留款余额照常计。
                        case "min_spend_retained" -> includeTopUp && billLevel ? 0L : adjustment.amountCent();
                        case "min_spend_retained_applied" -> -adjustment.amountCent();
                        // 账单级退款：已实际退出的商品金额不再算低消进度。
                        case "refund" -> includeTopUp && billLevel
                                ? -repository.refundGoodsCent(adjustment.id(), adjustment.amountCent()) : 0L;
                        default -> 0L;
                    };
                })
                .sum();
    }
}
