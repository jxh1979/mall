package com.complex.platform.entry;

/**
 * 人脸入场闸端口（端口/适配器模式，用户口径：第三方/人脸一律“预留接口 + 能力开关默认关”）。
 *
 * <p>业务规则（业务 HTML：全场 18+，统一人脸入场核验闸保证场内皆 ≥18 周岁）要求入场处经实名人脸闸判龄、
 * fail-closed（无通过记录不得入场）。所有真实第三方人脸/活体/实名核验交互经此端口抽象，业务侧
 * （入场核销、bar 售酒年龄继承）只依赖本接口，不直接耦合任何厂商 SDK。
 *
 * <p>路由由 {@link FaceVerificationRouter} 按能力开关 {@code face.verification.enabled} 决定：
 * <ul>
 *   <li>开关关（默认，本期）→ {@link DisabledFaceVerificationGate}：返回 {@code RESERVED}，不调外部、不阻塞，
 *       入场年龄由制度兜底（残留风险：人脸关闭期年龄非系统强制，已向用户知会）。</li>
 *   <li>开关开 → {@link ReservedFaceVerificationGate}：当前无真实厂商=占位实现，返回 {@code NOT_CONFIGURED}
 *       （留接缝，接入真实人脸/实名 SDK 后替换此实现，届时按实名身份证出生日期判龄、fail-closed）。</li>
 * </ul>
 *
 * <p>与 {@code finance.gateway.PaymentGatewayPort} 同范式：永不抛受检异常阻塞业务，开关关/无厂商均以
 * 状态码表达，由业务侧决定处置（本期一律非阻塞豁免）。
 */
public interface FaceVerificationPort {

    /** 入场人脸/年龄核验（占位实现不调外部）。 */
    FaceVerificationResult verifyEntry(FaceVerificationRequest request);
}
