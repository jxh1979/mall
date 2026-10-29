// 文件版本：V0.01
package com.complex.platform.security;

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.Strings;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 第 23 条 23-R2：页面打开发暗纹码并留痕（登录即可，后台、商户专区与员工手机端共用）；截屏或拍屏照片反查（security.watermark.trace）。
 * 两个端点都不加 @GuardedAction（18A 起缺配置行即关，留痕不能被开关关掉）；page-views 另在终端总开关豁免前缀里（DEC-R3B-2026-08-04-14）。
 */
@RestController
@RequestMapping("/api/v1/security")
public class WatermarkController {

    public record PageViewRequest(String pageId, Boolean sensitive, List<WatermarkMarkService.ObjectRef> objectRefs,
                                  String idempotencyKey) {
    }

    private final WatermarkMarkService marks;
    private final IdempotencyService idempotency;
    private final AuthorizationService authorization;

    public WatermarkController(WatermarkMarkService marks, IdempotencyService idempotency, AuthorizationService authorization) {
        this.marks = marks;
        this.idempotency = idempotency;
        this.authorization = authorization;
    }

    @PostMapping("/page-views")
    public ApiResponse<WatermarkMarkService.Mark> pageView(@RequestBody PageViewRequest request) {
        requireIdempotencyKey(request.idempotencyKey());
        String pageId = Strings.requireText(request.pageId(), "page_id");
        if (pageId.length() > 64) {
            throw ApiException.badRequest("VALIDATION_FAILED", "page_id is invalid");
        }
        List<WatermarkMarkService.ObjectRef> refs = request.objectRefs() == null ? List.of() : request.objectRefs();
        for (WatermarkMarkService.ObjectRef ref : refs) {
            Strings.requireText(ref.objectType(), "object_type");
            Strings.requireText(ref.objectId(), "object_id");
        }
        return ApiResponse.ok(idempotency.execute("securityPageView", request.idempotencyKey(), request,
                () -> marks.issue("page", pageId, null, Boolean.TRUE.equals(request.sensitive()), refs),
                WatermarkMarkService.Mark.class));
    }

    @PostMapping(value = "/watermarks/trace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<WatermarkMarkService.Trace> trace(@RequestPart("file") MultipartFile file) {
        authorization.requirePermission(WatermarkMarkService.TRACE_PERMISSION);
        BufferedImage image;
        try {
            image = ImageIO.read(file.getInputStream());
        } catch (IOException unreadable) {
            image = null;
        }
        if (image == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请上传 PNG 或 JPEG 图片");
        }
        return ApiResponse.ok(marks.trace(image));
    }
}
