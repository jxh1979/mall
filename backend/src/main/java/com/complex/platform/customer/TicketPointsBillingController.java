package com.complex.platform.customer;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/loyalty/ticket-points-billing")
public class TicketPointsBillingController {
    private final TicketPointsBillingService service;
    private final com.complex.platform.evidence.AuditedOperationExecutor operations;
    public TicketPointsBillingController(TicketPointsBillingService service,com.complex.platform.evidence.AuditedOperationExecutor operations){this.service=service;this.operations=operations;}
    @GetMapping("/rules") public ApiResponse<List<TicketPointsBilling.Rule>> rules(){return audited("rules.viewed",false,null,()->service.rules());}
    @PostMapping("/rules") public ApiResponse<TicketPointsBilling.Rule> rule(@RequestBody TicketPointsBilling.RuleRequest r){return audited("rule.submitted",true,null,()->service.submitRule(r));}
    @PostMapping("/calendars") public ApiResponse<TicketPointsBilling.CaseReceipt> calendar(@RequestBody TicketPointsBilling.CalendarRequest r){return audited("calendar.submitted",true,null,()->service.submitCalendar(r));}
    @GetMapping("/calendars") public ApiResponse<List<Map<String,Object>>> calendars(){return audited("calendars.viewed",false,null,()->service.calendarCases());}
    @GetMapping("/portal-outages") public ApiResponse<List<Map<String,Object>>> outages(){return audited("outages.viewed",false,null,()->service.outageCases());}
    @PostMapping("/portal-outages") public ApiResponse<TicketPointsBilling.CaseReceipt> outage(@RequestBody TicketPointsBilling.OutageRequest r){return audited("outage.submitted",true,null,()->service.submitOutage(r));}
    @GetMapping("/bills") public ApiResponse<List<TicketPointsBilling.Bill>> bills(@RequestParam(value="project_id",required=false) String project){return audited("bills.viewed",false,project,()->service.list(project));}
    @GetMapping("/bills/{id}/objections") public ApiResponse<List<Map<String,Object>>> objections(@PathVariable String id){return audited("objections.viewed",false,id,()->service.objections(id));}
    @GetMapping("/bills/{id}/items") public ApiResponse<List<TicketPointsBilling.FeeLine>> items(@PathVariable String id){return audited("items.viewed",false,id,()->service.items(id));}
    @GetMapping("/bills/{id}/schedule-adjustments") public ApiResponse<List<Map<String,Object>>> scheduleAdjustments(@PathVariable String id){return audited("schedule.adjustments.viewed",false,id,()->service.scheduleAdjustments(id));}
    @PostMapping("/bills/{id}/objections") public ApiResponse<TicketPointsBilling.CaseReceipt> object(@PathVariable String id,@RequestBody TicketPointsBilling.ObjectionRequest r){return audited("objection.submitted",false,id,()->service.object(id,r));}
    @PostMapping("/objections/{id}/confirm-unchanged") public ApiResponse<TicketPointsBilling.CaseReceipt> resolve(@PathVariable String id,@RequestBody TicketPointsBilling.ResolutionRequest r){return audited("objection.resolution.submitted",true,id,()->service.resolveUnchanged(id,r));}
    private <T> ApiResponse<T> audited(String name,boolean manage,String id,java.util.function.Supplier<T> call){String event="ticket.points.billing."+name;return ApiResponse.ok(operations.readSingle(com.complex.platform.evidence.AuditedOperation.of(event,event+".failed",manage?TicketPointsBillingService.MANAGE:TicketPointsBillingService.VIEW,"ticket_points_billing"),call,result->result instanceof TicketPointsBilling.Rule rule?rule.id():result instanceof TicketPointsBilling.CaseReceipt receipt?receipt.id():id==null?"all":id));}
}
