package com.complex.platform.bar;

// 文件版本：V0.01

import java.util.List;

/**
 * 第十批 TK-27-R2：清台登记恢复空闲（BSM L1058「关台成功后台位自动进入待清台；由负责该台位的服务员完成清理后在系统登记
 * 恢复空闲，散台与卡座同一动作」）。
 */
public record BarTableClearRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds
) {
}
