package com.complex.platform.finance.gateway;

/**
 * 支付渠道结果（端口出参，厂商无关）。永不抛出受检异常以阻塞业务——
 * 开关关/无厂商均以状态码表达，由业务侧决定回退路径。
 *
 * @param status        结果状态
 * @param gatewayProvider 渠道厂商标识（占位实现为 null 或 "reserved"）
 * @param gatewayRef    渠道侧引用号（占位实现为 null）
 * @param message       说明（留痕用）
 */
public record PaymentGatewayResult(
        Status status,
        String gatewayProvider,
        String gatewayRef,
        String message
) {

    public enum Status {
        /** 开关关：能力预留未启用，业务回退现有内部/占位结算（不阻塞）。 */
        RESERVED,
        /** 开关开但无真实厂商接入：占位适配器未配置，留接缝。 */
        NOT_CONFIGURED,
        /** 渠道受理成功（真实适配器接入后使用）。 */
        SUCCEEDED,
        /** 渠道处理中（真实适配器接入后使用）。 */
        PENDING,
        /** 渠道拒绝/失败（真实适配器接入后使用）。 */
        FAILED
    }

    public static PaymentGatewayResult reserved(String message) {
        return new PaymentGatewayResult(Status.RESERVED, null, null, message);
    }

    public static PaymentGatewayResult notConfigured(String message) {
        return new PaymentGatewayResult(Status.NOT_CONFIGURED, "reserved", null, message);
    }

    /** 是否表示"未真正经由渠道结算"——业务据此走内部/占位回退。 */
    public boolean requiresInternalFallback() {
        return status == Status.RESERVED || status == Status.NOT_CONFIGURED;
    }
}
