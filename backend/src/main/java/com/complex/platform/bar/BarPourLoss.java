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
}
