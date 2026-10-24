package com.complex.platform.ticketing;

// 文件版本：V0.01

import java.time.Instant;

/**
 * ROOT-03-R29「我的票」使用记录（BSM 第 862 行「查看我的订单、我的票、我的权益和使用记录」；第 880 行「核销后客人能看到使用记录」）：
 * 只给客人可见字段——结果（成功/已冲正）、时间、核销商户；不含员工、设备与凭证。
 */
public record CustomerTicketUsageResponse(String result, String resultLabel, Instant verifiedAt, String merchantName) {
}
