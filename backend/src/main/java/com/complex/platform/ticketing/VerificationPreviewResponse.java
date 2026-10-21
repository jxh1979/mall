package com.complex.platform.ticketing;

// 文件版本：V0.01

public record VerificationPreviewResponse(
        boolean allowed,
        String reason,
        OfflineEntitlementResponse entitlement,
        // TK-06：一人多次票且人脸供应商可用时，capture=本次首次留脸（须客人同意并拍照）、match=按留存人脸比对（须拍照）；其余为空。
        String faceBinding
) {
}
