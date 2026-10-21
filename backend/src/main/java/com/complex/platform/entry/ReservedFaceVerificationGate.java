package com.complex.platform.entry;

// 文件版本：V0.01

/**
 * 闸接缝实现：能力开关 {@code face.verification.enabled} 开但当前无真实厂商接入时使用。
 *
 * <p>这是为真实人脸/实名/活体 SDK 预留的接缝。当前返回 {@code NOT_CONFIGURED}（不崩、不调外部），
 * 接入真实厂商后，在此实现内调用对应 SDK：按实名身份证出生日期判龄 + 活体核验，返回真实
 * {@code PASSED/FAILED} 及 provider/reason，并改为 fail-closed（无通过记录不得入场）。
 */
public final class ReservedFaceVerificationGate implements FaceVerificationPort {

    private static final String MSG = "face verification enabled but no real provider adapter is configured; "
            + "seam reserved, real face/identity SDK pending";

    @Override
    public FaceVerificationResult verifyEntry(FaceVerificationRequest request) {
        return FaceVerificationResult.notConfigured(MSG);
    }

    // TK-06：无真实供应商 = 不可用；接入真实人脸 SDK 后在此返回 true 并实现比对。
    @Override
    public boolean holderMatchAvailable() {
        return false;
    }

    @Override
    public FaceVerificationResult matchHolder(String storedPhotoBase64, String capturedPhotoBase64) {
        return FaceVerificationResult.notConfigured(MSG);
    }
}
