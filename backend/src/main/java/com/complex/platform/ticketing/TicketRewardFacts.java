package com.complex.platform.ticketing;

// 文件版本：V0.01

/** 仅携带已落库的业务事实编号；奖励域重新读取事实，不接受终端自报会员或金额。 */
public final class TicketRewardFacts {
    private TicketRewardFacts() {}
    public record OrderCreated(String orderId) {}
    public record CredentialPresented(String credentialId, String customerId) {}
    public record Used(String verificationId, String credentialId) {}
    public record Reversed(String verificationId) {}
    public record Refunded(String refundId) {}
    public record PointsConfirmed(String rewardId) {}
}
