package com.complex.platform.inventory;

import java.util.List;

/**
 * R5 T17 交接全链视图：表头 + 五段步骤 + 异常清单。作为状态机各写操作的返回体。
 * 第十八批 F 段第 14 条 14-R2：另带该交接单全部人工补录单，与原扫码五段并列（BSM:1201「补录记录与原始扫码记录并列展示、互不覆盖」）。
 */
public record TraceHandoverChainView(
        TraceHandoverRecord handover,
        List<TraceHandoverStepRecord> steps,
        List<TraceHandoverExceptionRecord> exceptions,
        List<com.complex.platform.evidence.ManualBackfillService.Backfill> backfills
) {
}
