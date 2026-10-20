package com.complex.platform.inventory;

// 文件版本：V0.01

import java.util.Map;
import java.util.Set;

/**
 * FS-019 借用单的归还事实（按行）：已归还数、无法归还登记中（未驳回）数、实物处置已完成数，以及已归还 / 登记中的溯源码。
 * 尚未归还＝数量 − 已归还 − 登记中；全部数量都已归还或处置完成才结束归还事项（BSM L1201「结束对应数量的原物归还事项」）。
 */
public record InventoryLendingFacts(
        Map<String, Integer> returnedByLine,
        Map<String, Integer> claimedUnreturnableByLine,
        Map<String, Integer> closedUnreturnableByLine,
        Set<String> returnedCodes,
        Set<String> claimedCodes
) {
    public static final InventoryLendingFacts EMPTY = new InventoryLendingFacts(Map.of(), Map.of(), Map.of(), Set.of(), Set.of());

    public int returned(String lineId) {
        return returnedByLine.getOrDefault(lineId, 0);
    }

    public int claimed(String lineId) {
        return claimedUnreturnableByLine.getOrDefault(lineId, 0);
    }

    public int closed(String lineId) {
        return closedUnreturnableByLine.getOrDefault(lineId, 0);
    }

    /** 尚未归还也未登记无法归还的数量。 */
    public int outstanding(InventoryTransferLineRecord line) {
        return line.quantity() - returned(line.id()) - claimed(line.id());
    }

    /** 该行的归还事项是否已全部结束（归还＋处置完成＝数量）。 */
    public boolean settled(InventoryTransferLineRecord line) {
        return returned(line.id()) + closed(line.id()) >= line.quantity();
    }
}
