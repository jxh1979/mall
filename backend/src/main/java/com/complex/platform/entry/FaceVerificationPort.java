package com.complex.platform.entry;

// 文件版本：V0.01

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

    /**
     * TK-06（BSM:780 / BSM:843「一人多次票在首次核销时留存人脸，作为后续各次核销核对同一使用人的凭证」；BSM:1559 不自研人脸算法，
     * 按后台开关接入第三方供应商）：当前是否接入了可用的人脸比对供应商。不可用（开关关 / 未接入 / 故障）时，
     * 按 2026-10-21 操作员裁决「设备不可用时不留不核」照常核销，不要求同意与拍照、不比对。
     */
    boolean holderMatchAvailable();

    /**
     * TK-06：一人多次票后续各次核销，把留存人脸与本次拍照交供应商比对是否同一人。PASSED=同一人；FAILED=不是同一人（拒）；
     * RESERVED / NOT_CONFIGURED=供应商当次不可用（照常核销，不比对）。
     */
    FaceVerificationResult matchHolder(String storedPhotoBase64, String capturedPhotoBase64);
}
