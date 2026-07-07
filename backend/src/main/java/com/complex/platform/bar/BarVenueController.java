package com.complex.platform.bar;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 酒吧门店主数据接口（V263）：门店 CRUD + 存档/删除审批。
 * 权限：查看 bar.venue.view（medium）；写 bar.venue.manage（high）；删除审批 bar.venue.approve（critical，职责分离）。
 */
@RestController
@RequestMapping("/api/v1/bar/venues")
public class BarVenueController {

    private final BarVenueService service;
    private final AuthorizationService authorizationService;

    public BarVenueController(BarVenueService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ApiResponse<List<BarVenueRecord>> list(
            @RequestParam(value = "includeArchived", required = false, defaultValue = "false") boolean includeArchived) {
        authorizationService.requirePermission("bar.venue.view");
        return ApiResponse.ok(service.list(includeArchived));
    }

    @PostMapping
    public ApiResponse<BarVenueRecord> create(@RequestBody(required = false) BarVenueRequest request) {
        authorizationService.requirePermission("bar.venue.manage");
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<BarVenueRecord> update(@PathVariable("id") String id,
            @RequestBody(required = false) BarVenueRequest request) {
        authorizationService.requirePermission("bar.venue.manage");
        return ApiResponse.ok(service.update(id, request));
    }

    @PostMapping("/{id}/archive")
    public ApiResponse<BarVenueRecord> archive(@PathVariable("id") String id) {
        authorizationService.requirePermission("bar.venue.manage");
        return ApiResponse.ok(service.archive(id, true));
    }

    @PostMapping("/{id}/unarchive")
    public ApiResponse<BarVenueRecord> unarchive(@PathVariable("id") String id) {
        authorizationService.requirePermission("bar.venue.manage");
        return ApiResponse.ok(service.archive(id, false));
    }

    @PostMapping("/{id}/delete-request")
    public ApiResponse<BarVenueRecord> requestDelete(@PathVariable("id") String id,
            @RequestBody(required = false) BarCatalogDeleteRequest request) {
        authorizationService.requirePermission("bar.venue.manage");
        return ApiResponse.ok(service.requestDelete(id, request));
    }

    @PostMapping("/{id}/delete-approve")
    public ApiResponse<BarCatalogDeleteDecision> approveDelete(@PathVariable("id") String id,
            @RequestBody(required = false) BarCatalogDeleteApprovalRequest request) {
        authorizationService.requirePermission("bar.venue.approve");
        return ApiResponse.ok(service.approveDelete(id, request));
    }
}
