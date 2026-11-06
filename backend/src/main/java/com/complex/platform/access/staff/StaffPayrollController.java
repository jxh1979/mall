package com.complex.platform.access.staff;

// 文件版本：V0.02

import static com.complex.platform.access.staff.StaffPayrollModels.*;
import com.complex.platform.access.staff.dto.StaffAdjustmentAppealRequest;
import com.complex.platform.common.api.*;
import com.complex.platform.evidence.AuditEventService;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/staff/payroll")
public class StaffPayrollController {
    private final StaffPayrollService payroll;
    private final StaffPayrollConfigurationService config;
    private final AuditEventService audit;
    public StaffPayrollController(StaffPayrollService payroll,StaffPayrollConfigurationService config,AuditEventService audit){this.payroll=payroll;this.config=config;this.audit=audit;}
    @GetMapping("/schemes") public ApiResponse<List<Scheme>> schemes(@RequestParam("user_id") String user){return call("scheme_list","staff_payroll_scheme",user,()->config.schemes(user));}
    @PostMapping("/schemes") public ApiResponse<Scheme> scheme(@RequestBody SchemeDraftRequest r){return call("scheme_draft","staff_payroll_scheme",null,()->config.draftScheme(r));}
    @PostMapping("/schemes/{id}/submit") public ApiResponse<Scheme> submitScheme(@PathVariable String id,@RequestBody SubmitRequest r){return call("scheme_submit","staff_payroll_scheme",id,()->config.submitScheme(id,r));}
    @GetMapping("/calendars") public ApiResponse<List<CalendarVersion>> calendars(@RequestParam("project_id") String project){return call("calendar_list","staff_payroll_calendar",project,()->config.calendars(project));}
    @PostMapping("/calendars") public ApiResponse<CalendarVersion> calendar(@RequestBody CalendarDraftRequest r){return call("calendar_draft","staff_payroll_calendar",null,()->config.draftCalendar(r));}
    @PostMapping("/calendars/{id}/submit") public ApiResponse<CalendarVersion> submitCalendar(@PathVariable String id,@RequestBody SubmitRequest r){return call("calendar_submit","staff_payroll_calendar",id,()->config.submitCalendar(id,r));}
    @GetMapping("/period-facts") public ApiResponse<List<PeriodFacts>> periods(@RequestParam("user_id") String user,@RequestParam String month){return call("period_list","staff_payroll_period",user,()->config.periods(user,month));}
    @PostMapping("/period-facts") public ApiResponse<PeriodFacts> period(@RequestBody PeriodDraftRequest r){return call("period_draft","staff_payroll_period",null,()->config.draftPeriod(r));}
    @PostMapping("/period-facts/{id}/submit") public ApiResponse<PeriodFacts> submitPeriod(@PathVariable String id,@RequestBody SubmitRequest r){return call("period_submit","staff_payroll_period",id,()->config.submitPeriod(id,r));}
    @GetMapping("/performance-rules") public ApiResponse<List<ScoreRuleVersion>> rules(@RequestParam("project_id") String project,@RequestParam(value="merchant_id",required=false) String merchant,@RequestParam("owner_company_id") String owner){return call("rule_list","staff_performance_rule",project,()->config.scoreRules(project,merchant,owner));}
    @PostMapping("/performance-rules") public ApiResponse<ScoreRuleVersion> rule(@RequestBody ScoreRuleDraftRequest r){return call("rule_draft","staff_performance_rule",null,()->config.draftScoreRule(r));}
    @PostMapping("/performance-rules/{id}/submit") public ApiResponse<ScoreRuleVersion> submitRule(@PathVariable String id,@RequestBody SubmitRequest r){return call("rule_submit","staff_performance_rule",id,()->config.submitScoreRule(id,r));}
    @GetMapping("/performance-adjustments") public ApiResponse<List<ScoreAdjustment>> scores(@RequestParam("user_id") String user,@RequestParam String month){return call("score_list","staff_performance_score",user,()->config.scores(user,month));}
    @GetMapping("/mine/performance-adjustments") public ApiResponse<List<ScoreAdjustment>> mineScores(@RequestParam String month){return call("score_mine","staff_performance_score",null,()->config.myScores(month));}
    @PostMapping("/performance-adjustments") public ApiResponse<ScoreAdjustment> score(@RequestBody ScoreRequest r){return call("score_deduct","staff_performance_score",null,()->config.deductScore(r));}
    @PostMapping("/performance-adjustments/{id}/appeals") public ApiResponse<ScoreAdjustment> appeal(@PathVariable String id,@RequestBody StaffAdjustmentAppealRequest r){return call("score_appeal","staff_performance_score",id,()->config.appealScore(id,r));}
    @GetMapping("/calculations") public ApiResponse<List<CalculationDraft>> calculations(@RequestParam("user_id") String user,@RequestParam String month){return call("calculation_list","staff_payroll_calculation",user,()->payroll.calculations(user,month));}
    @PostMapping("/calculations") public ApiResponse<CalculationDraft> calculate(@RequestBody SettleRequest r){return call("calculate","staff_payroll_calculation",null,()->payroll.calculate(r));}
    @PostMapping("/settlements") public ApiResponse<StatementDetail> settle(@RequestBody SettleRequest r){return call("settle","staff_payroll",null,()->payroll.settle(r));}
    @GetMapping("/statements/{id}") public ApiResponse<StatementDetail> detail(@PathVariable String id){return call("detail","staff_payroll",id,()->payroll.detail(id,false));}
    @GetMapping("/mine/statements/{id}") public ApiResponse<StatementDetail> mine(@PathVariable String id){return call("detail_mine","staff_payroll",id,()->payroll.detail(id,true));}
    private <T> ApiResponse<T> call(String name,String type,String id,Supplier<T> action) {
        try {T result=action.get();audit.recordSuccess("staff.payroll."+name,null,type,id,null,null,Map.of());return ApiResponse.ok(result);}
        catch(RuntimeException e){audit.recordFailure("staff.payroll."+name+"_failed",null,type,id,null,null,e instanceof ApiException a?a.code():"INTERNAL_ERROR");throw e;}
    }
}
