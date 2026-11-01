package com.complex.platform.inventory;

// 文件版本：V0.01

/**
 * 第十八批 K 段 97-R3 吧台消耗统计一行（BSM:1168「记录吧台售卖后的酒水消耗，支持按杯、按瓶、按套餐或其他形式统计」）：
 * 按酒吧、销售形式、套餐、酒吧商品汇总期间内出品的售卖明细。
 * saleForm：by_glass 按杯、by_bottle 按瓶、combo 套餐；quantity 为杯数、瓶数或套餐明细件数（套餐内件数×份数），回库退品不计；
 * pourVolumeMl 出酒量（毫升），回库退品不计，没记出酒量的为空；lossVolumeMl 损耗量（毫升），损耗不退。
 */
public record BarConsumptionStatRecord(
        String barId,
        String barName,
        String saleForm,
        String comboId,
        String comboName,
        String productId,
        String productName,
        String inventoryItemId,
        String itemName,
        long quantity,
        Long pourVolumeMl,
        long lossVolumeMl
) {
}
