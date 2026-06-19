package com.complex.platform.inventory;

import java.util.List;

/**
 * R5 T17 交接全链视图：表头 + 五段步骤 + 异常清单。作为状态机各写操作的返回体。
 */
public record TraceHandoverChainView(
        TraceHandoverRecord handover,
        List<TraceHandoverStepRecord> steps,
        List<TraceHandoverExceptionRecord> exceptions
) {
}
