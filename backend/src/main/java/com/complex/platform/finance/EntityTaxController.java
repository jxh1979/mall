package com.complex.platform.finance;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 税费申报登记端点（第十八批 AC 段第 210 条 210-R2）。view＝列表；manage＝登记并提交财税法审核与最高确认。 */
@RestController
@RequestMapping("/api/v1/finance/entity-taxes")
public class EntityTaxController {

    private final EntityTaxService service;

    public EntityTaxController(EntityTaxService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<EntityTaxDeclaration>> list(
            @RequestParam(value = "business_entity_id", required = false) String businessEntityId,
            @RequestParam(value = "month", required = false) String month,
            @RequestParam(value = "tax_type", required = false) String taxType,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "50") int pageSize) {
        return ApiResponse.ok(service.list(businessEntityId, month, taxType, status, page, pageSize));
    }

    @PostMapping
    public ApiResponse<EntityTaxDeclaration> declare(@RequestBody EntityTaxDeclaration.Request request) {
        return ApiResponse.ok(service.declare(request));
    }
}
