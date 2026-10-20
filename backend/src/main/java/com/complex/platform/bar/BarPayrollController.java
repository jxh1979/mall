package com.complex.platform.bar;
// 文件版本：V0.02
import static com.complex.platform.bar.BarPayrollRequests.*;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bar")
public class BarPayrollController {
    private final BarPayrollRuleService rules;
    private final BarPayrollAllocationService allocations;
    private final BarServiceResponsibilityService responsibilities;
    private final BarPayrollRefundService refunds;
    private final AuditEventService audit;
    public BarPayrollController(BarPayrollRuleService rules,BarPayrollAllocationService allocations,BarServiceResponsibilityService responsibilities,BarPayrollRefundService refunds,AuditEventService audit) {
        this.rules=rules;this.allocations=allocations;this.responsibilities=responsibilities;this.refunds=refunds;this.audit=audit;
    }
    @GetMapping("/bills/{id}/refund-context")
    public ApiResponse<BarPayrollRefundService.RefundContext> refundContext(@PathVariable("id")String id) {
        return call("bar.refund_context.read","bar.bill.refund","bar_bill",id,null,true,()->refunds.context(id));
    }
    @GetMapping("/payroll-rules")
    public ApiResponse<List<Rule>> rules(@RequestParam("project_id")String project,@RequestParam(value="merchant_id",required=false)String merchant) {
        return call("bar.payroll_rule.read","bar.payroll.view","bar_payroll_rule",null,null,true,()->rules.list(project,merchant));
    }
    @PostMapping("/payroll-rules")
    public ApiResponse<Rule> draft(@RequestBody DraftRule r) {
        return call("bar.payroll_rule.draft","bar.payroll.manage","bar_payroll_rule",null,r==null?null:r.idempotencyKey(),false,()->rules.draft(r));
    }
    @PostMapping("/payroll-rules/{id}/confirm")
    public ApiResponse<Rule> confirm(@PathVariable("id")String id,@RequestBody ConfirmRule r) {
        return call("bar.payroll_rule.confirm","bar.payroll.manage","bar_payroll_rule",id,r==null?null:r.idempotencyKey(),false,()->rules.confirm(id,r));
    }
    @PostMapping("/payroll-rules/{id}/submit")
    public ApiResponse<Rule> submit(@PathVariable("id")String id,@RequestBody SubmitRule r) {
        return call("bar.payroll_rule.submit","bar.payroll.manage","bar_payroll_rule",id,r==null?null:r.idempotencyKey(),false,()->rules.submit(id,r));
    }
    @GetMapping("/payroll-runs")
    public ApiResponse<List<MonthRun>> runs(@RequestParam("project_id")String project,@RequestParam("merchant_id")String merchant,@RequestParam("month")String month) {
        return call("bar.payroll.read","bar.payroll.view","bar_payroll_run",null,null,true,()->allocations.overview(project,merchant,month));
    }
    @PostMapping("/payroll-runs/close")
    public ApiResponse<List<MonthRun>> close(@RequestBody CloseMonth r) {
        return call("bar.payroll.month_processed","bar.payroll.manage","bar_payroll_run",null,r==null?null:r.idempotencyKey(),false,()->allocations.close(r));
    }
    @GetMapping("/table-sessions/{id}/service-responsibilities")
    public ApiResponse<List<Responsibility>> history(@PathVariable("id")String id) {
        return call("bar.service.read","bar.payroll.view","bar_table_session",id,null,true,()->responsibilities.history(id));
    }
    @PostMapping("/table-sessions/{id}/service-handover")
    public ApiResponse<List<Responsibility>> handover(@PathVariable("id")String id,@RequestBody Handover r) {
        return call("bar.service.handover","bar.table_session.transfer","bar_table_session",id,r==null?null:r.idempotencyKey(),false,()->responsibilities.handover(id,r));
    }
    @PostMapping("/table-sessions/{id}/service-close")
    public ApiResponse<List<Responsibility>> serviceClose(@PathVariable("id")String id,@RequestBody CloseSession r) {
        return call("bar.service.closed","bar.table.open_close","bar_table_session",id,r==null?null:r.idempotencyKey(),false,()->responsibilities.close(id,r));
    }
    private <T> ApiResponse<T> call(String event,String permission,String type,String id,String key,boolean read,Supplier<T> work) {
        try {
            T value=work.get();
            if(read)audit.recordSuccess(event,permission,type,id,null,key,Map.of());
            return ApiResponse.ok(value);
        } catch(RuntimeException e) {
            audit.recordFailure(event+"_failed",permission,type,id,null,key,e instanceof ApiException a?a.code():e.getClass().getSimpleName());
            throw e;
        }
    }
}
