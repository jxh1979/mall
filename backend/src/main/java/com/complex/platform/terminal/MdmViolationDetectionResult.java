package com.complex.platform.terminal;

import java.util.List;

/**
 * P0#9 服务端违规检测结果：本次检测对 app/site 与白名单比对后命中的违规清单。
 * {@code violationCount}=0 表示全部命中白名单（合规）。作为幂等响应可安全重放。
 */
public record MdmViolationDetectionResult(
        String deviceId,
        int violationCount,
        List<MdmPolicyViolationRecord> violations
) {
}
