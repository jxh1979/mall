package com.complex.platform.ticketing;

// 文件版本：V0.01

/**
 * S5 票面赠酒清单行（按酒吧分组的只读投影）：售票时票面要写明「各酒吧分别赠几杯」
 * （业务 HTML §通票赠酒按酒吧分配）。一行 = 某家酒吧的一项赠酒权益。
 *
 * @param barId          提供该赠酒的酒吧商户 id
 * @param barName        酒吧名称（票面展示）
 * @param templateId     权益模板 id
 * @param giftName       赠品名称（票面展示，如「精酿一杯」）
 * @param quantity       份数
 * @param numPeople      每份核销人数（多人票 &gt; 1）
 * @param cups           本项杯数 = 份数 × 次数 × 每份人数
 * @param barTotalCups   该酒吧在本票面上的合计杯数（票面写明的那个数）
 */
public record TicketTypeGiftCoverageRow(
        String barId,
        String barName,
        String templateId,
        String giftName,
        int quantity,
        int numPeople,
        int cups,
        int barTotalCups
) {
}
