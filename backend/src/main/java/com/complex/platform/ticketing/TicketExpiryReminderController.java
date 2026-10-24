package com.complex.platform.ticketing;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiRequests;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.customer.CustomerService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** ROOT-03 clients-R10 未核销票到期提醒：后台按项目配提前小时数（值与上下限同配），客人端拉取本人即将到期的票（见 {@link TicketExpiryReminderService}）。 */
@RestController
public class TicketExpiryReminderController {

    private final TicketExpiryReminderService service;
    private final CustomerService customerService;

    public TicketExpiryReminderController(TicketExpiryReminderService service, CustomerService customerService) {
        this.service = service;
        this.customerService = customerService;
    }

    @GetMapping("/api/v1/ticketing/expiry-reminder-policies/{project_id}")
    public ApiResponse<TicketExpiryReminderService.Policy> getPolicy(@PathVariable("project_id") String projectId) {
        return ApiResponse.ok(service.getPolicy(projectId));
    }

    @PutMapping("/api/v1/ticketing/expiry-reminder-policies/{project_id}")
    public ApiResponse<TicketExpiryReminderService.Policy> savePolicy(@PathVariable("project_id") String projectId,
            @RequestBody TicketExpiryReminderService.PolicyRequest request) {
        ApiRequests.requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.savePolicy(projectId, request));
    }

    @GetMapping("/api/v1/customer/ticket-expiry-reminders")
    public ApiResponse<List<TicketExpiryReminderService.Reminder>> reminders(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        String token = customerToken != null && !customerToken.isBlank() ? customerToken.trim()
                : authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7).trim() : null;
        return ApiResponse.ok(service.remindersFor(customerService.resolveCustomerId(token)));
    }
}
