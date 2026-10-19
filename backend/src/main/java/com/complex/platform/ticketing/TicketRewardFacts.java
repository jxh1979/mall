package com.complex.platform.ticketing;

// 文件版本：V0.02

/** 仅携带已落库的业务事实编号；奖励域重新读取事实，不接受终端自报会员或金额。 */
public final class TicketRewardFacts {
    private TicketRewardFacts() {}
    public record OrderCreated(String orderId) {}
    /** FS-013：订单出票完成（父票已建、paid_at 已定），单一酒吧门票据此逐票锁定综合服务费计费口径。 */
    public record Issued(String orderId) {}
    public record CredentialPresented(String credentialId, String customerId) {}
    public record Used(String verificationId, String credentialId) {}
    public record Reversed(String verificationId) {}
    public record Refunded(String refundId) {}
    public record PointsConfirmed(String rewardId) {}
}
