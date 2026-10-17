package com.complex.platform.dealer;

// 文件版本：V0.01

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.dealer.dto.DealerPortalAccountRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CG-11d 内部后台：经销商门户账号开通与查看。 */
@RestController
@RequestMapping("/api/v1/dealers")
public class DealerPortalAccountController {

    private final DealerPortalAccountService service;

    public DealerPortalAccountController(DealerPortalAccountService service) {
        this.service = service;
    }

    @GetMapping("/{id}/portal-accounts")
    public ApiResponse<PageResponse<DealerPortalAccountRecord>> listPortalAccounts(@PathVariable String id) {
        return ApiResponse.ok(service.listPortalAccounts(id));
    }

    @PostMapping("/{id}/portal-accounts")
    public ApiResponse<DealerPortalAccountRecord> createPortalAccount(@PathVariable String id, @RequestBody DealerPortalAccountRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createPortalAccount(id, request));
    }
}
