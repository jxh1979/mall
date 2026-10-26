package com.complex.platform.bar;

// 文件版本：V0.02

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
