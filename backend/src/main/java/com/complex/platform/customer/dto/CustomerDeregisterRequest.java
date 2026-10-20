package com.complex.platform.customer.dto;

// 文件版本：V0.01

/**
 * TK-21-R3（BSM「账户注销」：「客人可以选择……放弃余额，径行注销」）：自助注销请求体。
 * waiveBalance=true 明确确认放弃全部储值余额（归发卡主体）；false/缺省 需退本金的按「储值」条生成待退事项。
 */
public record CustomerDeregisterRequest(Boolean waiveBalance) {
    public boolean waive() {
        return Boolean.TRUE.equals(waiveBalance);
    }
}
