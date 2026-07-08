package com.complex.platform.finance;

/**
 * 提成租金阶梯档位（超额累进）：某提成规则下按月销售额分档，每档只对「落在本档区间的那部分销售额」用本档费率。
 * seq 从 1 起、按 upperBoundCent 升序；upperBoundCent=null 表示最高档（无上限）。rateBps=基点(0-10000)。
 */
public record TurnoverRentRuleTierRecord(
        String id,
        String ruleId,
        int seq,
        Long upperBoundCent,
        int rateBps
) {
}
