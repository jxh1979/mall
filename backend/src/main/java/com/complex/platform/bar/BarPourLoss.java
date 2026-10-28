package com.complex.platform.bar;

/**
 * 按杯出酒量损耗计算（REQ-22）。实际出酒量超出"标准出酒量 × 数量"的部分记为损耗；
 * 损耗超过产品配置的阈值视为严重损耗，触发核查留痕。单位与产品配置一致（如 ml，整数）。
 */
public final class BarPourLoss {

    private BarPourLoss() {
    }

    /** 损耗量 = max(0, 实际出酒总量 - 标准出酒量 × 数量)。 */
    public static long lossVolume(int standardPourVolumePerUnit, int quantity, int actualPourVolumeTotal) {
        long standardTotal = (long) standardPourVolumePerUnit * quantity;
        long loss = (long) actualPourVolumeTotal - standardTotal;
        return loss > 0 ? loss : 0L;
    }

    /** 损耗是否超过阈值（阈值为空视为未配置，不触发）。 */
    public static boolean exceedsThreshold(long lossVolume, Integer lossThreshold) {
        return lossThreshold != null && lossThreshold >= 0 && lossVolume > lossThreshold;
    }

    /**
     * 第十八批 C 段第 17 条（BSM 第 1065 行 / ENUM 第 106 行「未配备采集设备的吧台，必须按标准出酒量配酒，也按该标准计算扣减与标准损耗，
     * 不得按零损耗记账」；第十八批裁决 4）：标准损耗＝标准出酒总量×本酒吧损耗率阈值（%），四舍五入到整数且至少 1
     * （损耗量列为整数，防小标准量被取整成零损耗）。点单出品与通票赠饮出品共用。
     */
    public static long standardLoss(int standardPourVolumeTotal, int lossRatePct) {
        return Math.max(1L, ((long) standardPourVolumeTotal * lossRatePct + 50) / 100);
    }
}
