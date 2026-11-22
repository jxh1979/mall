package com.complex.platform.ticketing;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiRequests;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.ticketing.TicketConcessionApplicationService.TicketConcessionApplicationView;
import com.complex.platform.ticketing.TicketConcessionApplicationService.TicketOption;
import com.complex.platform.ticketing.dto.MerchantConcessionApplicationRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 第十八批 BV 段第 264 条（第十八批裁决 78）：商户专区让利申请（提交并在线确认、查看本商户的），以及票务侧按票种列出各商户的申请。 */
@RestController
@RequestMapping("/api/v1")
public class TicketConcessionApplicationController {

    private final TicketConcessionApplicationService service;

    public TicketConcessionApplicationController(TicketConcessionApplicationService service) {
        this.service = service;
    }

    @GetMapping("/merchant-portal/concession-ticket-types")
    public ApiResponse<List<TicketOption>> listMerchantConcessionTicketTypes() {
        return ApiResponse.ok(service.ticketOptions());
    }

    @GetMapping("/merchant-portal/concession-applications")
    public ApiResponse<List<TicketConcessionApplicationView>> listMerchantConcessionApplications() {
        return ApiResponse.ok(service.listMine());
    }

    @PostMapping("/merchant-portal/concession-applications")
    public ApiResponse<TicketConcessionApplicationView> submitMerchantConcessionApplication(
            @RequestBody MerchantConcessionApplicationRequest request) {
        ApiRequests.requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.submit(request));
    }

    @GetMapping("/ticketing/ticket-types/{id}/concession-applications")
    public ApiResponse<List<TicketConcessionApplicationView>> listTicketTypeConcessionApplications(@PathVariable("id") String id) {
        return ApiResponse.ok(service.listForTicketType(id));
    }
}
