package com.complex.platform.entry;

/**
 * 人脸入场核验结果（端口出参，厂商无关）。永不抛出受检异常以阻塞业务——
 * 开关关/无厂商均以状态码表达，由业务侧决定处置（本期 RESERVED/NOT_CONFIGURED 一律非阻塞豁免）。
 *
 * @param status   结果状态
 * @param provider 人脸/实名厂商标识（占位实现为 null 或 "reserved"）
 * @param reason   机读原因码（如 underage / no_pass_record；占位期为 gate_disabled / not_configured）
 * @param message  说明（留痕用）
 */
public record FaceVerificationResult(
        Status status,
        String provider,
        String reason,
        String message
) {

    public enum Status {
        /** 开关关：人脸闸能力预留未启用，入场不阻塞，年龄由制度兜底（本期默认）。 */
        RESERVED,
        /** 开关开但无真实厂商接入：占位适配器未配置，留接缝。 */
        NOT_CONFIGURED,
        /** 实名人脸核验通过且 ≥18 周岁（真实适配器接入后使用）。 */
        PASSED,
        /** 实名人脸核验未通过 / 无通过记录 / 未成年（真实适配器接入后使用，fail-closed）。 */
        FAILED
    }

    public static FaceVerificationResult reserved(String message) {
        return new FaceVerificationResult(Status.RESERVED, null, "gate_disabled", message);
    }

    public static FaceVerificationResult notConfigured(String message) {
        return new FaceVerificationResult(Status.NOT_CONFIGURED, "reserved", "not_configured", message);
    }

    public static FaceVerificationResult passed(String provider) {
        return new FaceVerificationResult(Status.PASSED, provider, null, "face/age verification passed");
    }

    public static FaceVerificationResult failed(String provider, String reason, String message) {
        return new FaceVerificationResult(Status.FAILED, provider, reason, message);
    }

    /**
     * 是否“未真正经人脸闸系统裁决”——业务据此走豁免/制度兜底（本期 RESERVED/NOT_CONFIGURED 均为真）。
     * 与 {@code PaymentGatewayResult.requiresInternalFallback} 同范式。
     */
    public boolean requiresExemptionFallback() {
        return status == Status.RESERVED || status == Status.NOT_CONFIGURED;
    }

    /** 是否人脸闸明确拒绝（真实接入后 fail-closed：未通过/无通过记录/未成年）。 */
    public boolean isRejected() {
        return status == Status.FAILED;
    }
}
