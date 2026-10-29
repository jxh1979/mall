// 文件版本：V0.01
package com.complex.platform.security;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 第 23 条 23-R3：「我的导出」（登录即可，只列本人导出及其打开密码，PGM:687「导出人可以在专门的地方查到自己导出文件的密码」）；
 * 导出密码反查（security.watermark.trace，单独授予；密码放请求体不放 URL）；23-R4 可编辑版申请与发放（原导出人本人，最高权限审批，一次批准发放一次）。
 * 不加 @GuardedAction（18A 起缺配置行即关）。
 */
@RestController
@RequestMapping("/api/v1/security")
public class ExportFileController {

    public record LookupRequest(String password) {
    }

    private final ExportFileService exports;
    private final AuthorizationService authorization;
    private final EditableExportService editable;

    public ExportFileController(ExportFileService exports, AuthorizationService authorization, EditableExportService editable) {
        this.exports = exports;
        this.authorization = authorization;
        this.editable = editable;
    }

    @GetMapping("/my-exports")
    public ApiResponse<PageResponse<ExportFileService.MyExport>> myExports(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(exports.mine(page, size));
    }

    @PostMapping("/export-files/lookup")
    public ApiResponse<ExportFileService.PasswordTrace> lookup(@RequestBody LookupRequest request) {
        authorization.requirePermission(WatermarkMarkService.TRACE_PERMISSION);
        return ApiResponse.ok(exports.lookup(request == null ? null : request.password()));
    }

    /** 第 23 条 23-R4 申请可编辑版（请求体 purpose、recipient、idempotency_key）：进最高权限审批，返回复核单。 */
    @PostMapping("/export-files/{id}/editable-requests")
    public ApiResponse<SupervisorReviewDecisionRecord> requestEditable(@PathVariable("id") String id,
            @RequestBody(required = false) EditableExportService.EditableCopyRequest request) {
        return ApiResponse.ok(editable.request(id, request));
    }

    /** 第 23 条 23-R4 下载可编辑版：这次批准发放一次带密码与暗纹、可复制可编辑的 PDF，打开密码到「我的导出」查看。 */
    @GetMapping("/export-files/{id}/editable-copy")
    public ResponseEntity<byte[]> editableCopy(@PathVariable("id") String id,
            @RequestParam(name = "review_id", required = false) String reviewId) {
        return editable.issue(id, reviewId).response();
    }
}
