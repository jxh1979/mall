package com.complex.platform.ticketing;

/**
 * S5 赠送权益逐项模型：提供方商品（{@code bar_products}）在票务侧的只读投影。
 *
 * <p>只取「解析内部结算价」与「扣库前置校验」需要的几列，不引 bar 域类型（避免 bar↔ticketing 包环）。
 *
 * <p><b>{@code bar_item_ref} 语义定死为 {@code bar_products.id}</b>（S5）：它才是「提供方的商品」，
 * 天然带 {@code bar_id}（=提供方商户）与 {@code internal_settlement_price_cent}（内部结算价）；
 * 库存物料 {@code inventory_item_id} 从商品二跳取即可，且只在「该商品需扣库存」时才必需。
 *
 * @param id                           商品 id（即权益模板的 bar_item_ref）
 * @param barId                        提供方商户 id（须与权益模板的提供方商户一致）
 * @param internalSettlementPriceCent  内部结算价快照（分）；V380 起唯一真源在目录，本列由系统同步、只读展示用
 * @param inventoryItemId              库存物料 id（可空；仅 stockDeductionRequired 时必需）
 * @param stockDeductionRequired       售出/兑付是否需扣库存
 * @param status                       商品状态
 * @param catalogItemCode              批10/E：内部结算 SKU 目录引用；空=该商品未挂目录（无内部价）→ 解析 fail-closed
 */
public record BarProductRefRecord(
        String id,
        String barId,
        Long internalSettlementPriceCent,
        String inventoryItemId,
        boolean stockDeductionRequired,
        String status,
        String catalogItemCode
) {
    /** V380 前的 6 参构造（既有测试/构造点）：默认未挂目录。 */
    public BarProductRefRecord(String id, String barId, Long internalSettlementPriceCent, String inventoryItemId,
                               boolean stockDeductionRequired, String status) {
        this(id, barId, internalSettlementPriceCent, inventoryItemId, stockDeductionRequired, status, null);
    }
}
