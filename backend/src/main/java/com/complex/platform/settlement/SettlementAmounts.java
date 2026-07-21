package com.complex.platform.settlement;

/**
 * 结算状态机重构(AUD-012/017)：核销结算金额的<b>不可变增量</b>公式（单一真相，消费者与重建任务共用）。
 *
 * <p>累计已核销金额函数 f(x)：非专属 = clamp(round(allocated × x / total), 0, allocated)；
 * 专属定价 = price × x。第 k 次核销的<b>增量</b> = f(k) − f(k−1)，逐笔相加 telescope 回 f(usedTimes)，
 * 无舍入漂移（Σ增量 ≡ 累计值）。冲正则取原核销行金额的严格取反，配对天然净 0。
 */
final class SettlementAmounts {

    private SettlementAmounts() {
    }

    /** 累计到第 x 次核销的已核销金额（分）。x<=0 → 0。 */
    static long cumulativeVerified(Long allocatedRevenueCent, int totalTimes, int x,
                                   boolean exclusiveByPrice, Long settlementPriceCent) {
        if (x <= 0) {
            return 0L;
        }
        if (exclusiveByPrice) {
            long price = settlementPriceCent == null ? 0L : settlementPriceCent;
            return Math.max(0L, price * x);
        }
        long allocated = allocatedRevenueCent == null ? 0L : allocatedRevenueCent;
        if (totalTimes <= 0) {
            return Math.max(0L, allocated); // 防御:发票时至少 1 次,理论不达。
        }
        long v = Math.round((double) allocated * x / totalTimes);
        return Math.max(0L, Math.min(allocated, v));
    }

    /**
     * 第 usedTimes 次核销（本次核销后的最新累计次数）的不可变增量已核销金额（分）。
     * = f(usedTimes) − f(usedTimes−1)。恒 &gt;= 0。
     */
    static long incrementalVerified(Long allocatedRevenueCent, int totalTimes, int usedTimes,
                                    boolean exclusiveByPrice, Long settlementPriceCent) {
        long cur = cumulativeVerified(allocatedRevenueCent, totalTimes, usedTimes, exclusiveByPrice, settlementPriceCent);
        long prev = cumulativeVerified(allocatedRevenueCent, totalTimes, usedTimes - 1, exclusiveByPrice, settlementPriceCent);
        return cur - prev;
    }

    /** 专属直付判定：专属商户 + 有内部结算价 → 按结算价×核销数直付（不走 allocated 比例分摊）。 */
    static boolean exclusiveByPrice(String exclusiveMerchantId, Long settlementPriceCent) {
        return exclusiveMerchantId != null && settlementPriceCent != null;
    }
}
