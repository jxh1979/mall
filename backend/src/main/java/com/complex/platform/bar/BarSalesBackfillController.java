package com.complex.platform.bar;

// 文件版本：V0.01

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.evidence.ManualBackfillService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 第十八批 F 段第 14 条台位会话人工补录提交：14-R3 销售归属（见 {@link BarSessionSalesBackfillTarget}）、14-R6 服务责任区间
 * （见 {@link BarSessionServiceBackfillTarget}）；公共流程在 {@link ManualBackfillService}。
 */
@RestController
@RequestMapping("/api/v1/bar")
public class BarSalesBackfillController {

    private final ManualBackfillService backfills;

    public BarSalesBackfillController(ManualBackfillService backfills) {
        this.backfills = backfills;
    }

    @PostMapping("/table-sessions/{session_id}/sales-backfill")
    public ApiResponse<ManualBackfillService.Backfill> submit(@PathVariable("session_id") String sessionId,
            @RequestBody(required = false) ManualBackfillService.Request request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(backfills.submit(BarSessionSalesBackfillTarget.TYPE, sessionId, null, request));
    }

    @PostMapping("/table-sessions/{id}/service-backfill")
    public ApiResponse<ManualBackfillService.Backfill> submitService(@PathVariable("id") String sessionId,
            @RequestBody(required = false) ManualBackfillService.Request request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(backfills.submit(BarSessionServiceBackfillTarget.TYPE, sessionId, null, request));
    }
}
