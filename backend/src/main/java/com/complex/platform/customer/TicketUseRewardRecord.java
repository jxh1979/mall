package com.complex.platform.customer;

// 文件版本：V0.01

import java.time.Instant;

public record TicketUseRewardRecord(String id,String orderId,String parentTicketId,int slotIndex,int useIndex,
        String verificationId,String customerId,String projectId,String pointsRuleId,String growthRuleId,
        String waitRuleId,long points,long growth,int waitingHours,Instant verifiedAt,Instant availableAt,
        String status,Instant confirmedAt,long confirmedPoints,Instant reversedAt,String reversalSource) {}
