package com.complex.platform.amusement;

// 文件版本：V0.01

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 第十八批 BR 段第 357 条 357-R1：游乐公司为游乐项目引用所属商户的内部结算价目录条目（规则在 {@link AmusementProjectPriceRefService}）。 */
@RestController
public class AmusementProjectPriceRefController {
    private final AmusementProjectPriceRefService service;
    private final AuthorizationService authorizationService;

    public AmusementProjectPriceRefController(AmusementProjectPriceRefService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @PutMapping("/api/v1/amusement/projects/{id}/internal-price-item")
    public ApiResponse<AmusementProjectRecord> setInternalPriceItem(@PathVariable String id,
            @RequestBody(required = false) AmusementProjectPriceRefService.Request request) {
        authorizationService.requirePermission(AmusementProjectPriceRefService.PERMISSION);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.setInternalPriceItem(id, request));
    }
}
