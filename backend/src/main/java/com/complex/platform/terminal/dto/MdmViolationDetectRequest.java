package com.complex.platform.terminal.dto;

import java.util.List;

/**
 * P0#9 服务端违规检测请求：上报设备当前安装的 app 包名 / 访问的站点域名，
 * 由服务端比对白名单表判定违规（不再仅依赖外部已判定的 POST 上报）。
 */
public record MdmViolationDetectRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String deviceId,
        List<String> appPackages,
        List<String> siteDomains
) {
}
