package com.complex.platform.contract;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 退租结算算法（纯函数，无副作用、无 IO）：未清应收清算 + 押金处置 + 提前退租违约金，
 * 产出结算清单快照（逐项留痕）+ 落台账净额。抵扣规则：应退押金先抵未清应收，
 * 净额&gt;0 挂应付给商户，净额≤0=商户仍欠差额（不挂应付、只列示，未清应收本已在应收台账不重复挂）。
 * 违约金仅 early_termination 计入；move_out 不计违约金。
 */
final class TerminationSettlementMath {

    private TerminationSettlementMath() {
    }

    record Result(
            long depositCent,
            long unpaidCent,
            long offsetCent,              // 押金抵未清的实际抵扣额 = min(deposit, unpaid)
            long depositRefundCent,       // 应退押金净额 = max(0, deposit - unpaid)，>0 才挂应付
            long merchantStillOwesCent,   // = max(0, unpaid - deposit)，>0 只列示不挂
            long penaltyCent,             // 生效违约金（move_out 恒 0）
            List<Map<String, Object>> items) {
    }

    /** trigger ∈ {move_out, early_termination}；penaltyCent 仅 early_termination 计入。 */
    static Result compute(String trigger, long depositCent, long unpaidCent, long penaltyCent, String penaltyReason) {
        long deposit = Math.max(0L, depositCent);
        long unpaid = Math.max(0L, unpaidCent);
        boolean early = "early_termination".equals(trigger);
        long penalty = early ? Math.max(0L, penaltyCent) : 0L;
        long offset = Math.min(deposit, unpaid);
        long depositRefund = Math.max(0L, deposit - unpaid);
        long stillOwes = Math.max(0L, unpaid - deposit);

        List<Map<String, Object>> items = new ArrayList<>();
        // ① 未清应收清算（只读汇总，不重复挂账——这些应收本就在应收台账）
        Map<String, Object> unpaidItem = new LinkedHashMap<>();
        unpaidItem.put("item", "unpaid_receivable_summary");
        unpaidItem.put("amountCent", unpaid);
        unpaidItem.put("status", "listed");
        unpaidItem.put("note", "未清应收合计（已在应收台账，不重复挂账）");
        items.add(unpaidItem);
        // ② 押金处置（有保证金才有此项）
        if (deposit > 0) {
            Map<String, Object> dep = new LinkedHashMap<>();
            dep.put("item", "deposit_disposal");
            dep.put("depositCent", deposit);
            dep.put("offsetCent", offset);
            dep.put("netRefundCent", depositRefund);
            dep.put("merchantStillOwesCent", stillOwes);
            dep.put("amountCent", depositRefund);
            dep.put("direction", depositRefund > 0 ? "payable" : "none");
            dep.put("status", depositRefund > 0 ? "payable_posted" : "listed_only");
            dep.put("note", depositRefund > 0
                    ? "应退押金净额（押金抵未清后）挂应付给商户"
                    : "押金不足抵未清应收，商户仍欠差额（不挂应付、列示）");
            items.add(dep);
        }
        // ③ 提前退租违约金
        if (early && penalty > 0) {
            Map<String, Object> pen = new LinkedHashMap<>();
            pen.put("item", "termination_penalty");
            pen.put("amountCent", penalty);
            pen.put("direction", "receivable");
            pen.put("status", "receivable_posted");
            pen.put("note", "提前退租违约金挂应收");
            if (penaltyReason != null && !penaltyReason.isBlank()) {
                pen.put("reason", penaltyReason);
            }
            items.add(pen);
        }
        return new Result(deposit, unpaid, offset, depositRefund, stillOwes, penalty, items);
    }
}
