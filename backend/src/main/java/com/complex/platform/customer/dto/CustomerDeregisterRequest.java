package com.complex.platform.customer.dto;

// 文件版本：V0.02

/**
 * TK-21-R3 / ROOT-03-R21（BSM 第 863 行「客人可以选择不取存酒、放弃余额，径行注销」）：自助注销请求体。
 * waiveBalance=true 明确确认放弃全部储值余额（归发卡主体）；false/缺省 需退本金的按「储值」条生成待退事项。
 * abandonSavedBottles=true 明确确认放弃名下存酒（按存酒到期规则转为所在酒吧的自有库存资产）；名下有存酒而未确认放弃的不予注销。
 */
public record CustomerDeregisterRequest(Boolean waiveBalance, Boolean abandonSavedBottles) {
    public boolean waive() {
        return Boolean.TRUE.equals(waiveBalance);
    }

    public boolean abandonBottles() {
        return Boolean.TRUE.equals(abandonSavedBottles);
    }
}
