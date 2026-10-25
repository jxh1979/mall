package com.complex.platform.customer;

// 文件版本：V0.03

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class TicketRewardController {
    private final TicketRewardConfigService configs;
    private final TicketRewardService rewards;
    private final CustomerService customers;
    private final PointsRecoveryService recoveries;
    private final MerchantConsumptionRewardService merchantRewards;
    private final com.complex.platform.evidence.AuditedOperationExecutor operations;
    private final CustomerProjectSwitch projectSwitch;
    public TicketRewardController(TicketRewardConfigService configs,TicketRewardService rewards,CustomerService customers,PointsRecoveryService recoveries,MerchantConsumptionRewardService merchantRewards,com.complex.platform.evidence.AuditedOperationExecutor operations,CustomerProjectSwitch projectSwitch){this.configs=configs;this.rewards=rewards;this.customers=customers;this.recoveries=recoveries;this.merchantRewards=merchantRewards;this.operations=operations;this.projectSwitch=projectSwitch;}
    @GetMapping("/loyalty/ticket-reward-configs")
    public ApiResponse<List<TicketRewardConfig>> list(@RequestParam(name="project_id",required=false)String project){return audited("ticket.reward.config.listed",TicketRewardConfigService.VIEW,project,()->configs.list(project));}
    @PostMapping("/loyalty/ticket-reward-configs")
    public ApiResponse<TicketRewardConfig> submit(@RequestBody TicketRewardConfig.Request request){return audited("ticket.reward.config.submit.completed",TicketRewardConfigService.MANAGE,null,()->configs.submit(request));}
    @PostMapping("/loyalty/ticket-reward-configs/{id}/publish")
    public ApiResponse<TicketRewardConfig> publish(@PathVariable String id,@RequestBody TicketRewardConfig.PublishRequest request){return audited("ticket.reward.config.publish.completed",TicketRewardConfigService.MANAGE,id,()->configs.publish(id,request));}
    public record PublishedRule(String id,String ticketTypeId,String ticketName,String projectId,String projectName,String kind,String version,long fixedValue,Instant effectiveAt,Instant publishedAt){}
    @GetMapping("/customer/ticket-reward-rules")
    public ApiResponse<List<PublishedRule>> rules(@RequestParam(name="ticket_type_id",required=false)String type,@RequestParam(name="project_id",required=false)String project){
        // PGM 第 744 行：给了票种取票种所属综合体、否则取 project_id 判按综合体的客人端开关（匿名，终端类型自报）；两者都不带＝跨综合体列表，不判。
        projectSwitch.ticketTypeOrProject(type,project);
        return audited("ticket.reward.rules.viewed","not_applicable",type,()->configs.announcements(type,project));}
    public record RewardSummary(List<TicketUseRewardRecord> rewards,long pendingRecovery){}
    @GetMapping("/customer/ticket-rewards")
    public ApiResponse<RewardSummary> ownRewards(@RequestHeader(name="X-Customer-Token",required=false)String token,@RequestHeader(name="Authorization",required=false)String authorization){
        return audited("ticket.reward.customer.viewed","not_applicable",null,()->{
            String customer=customer(token,authorization);
            return new RewardSummary(rewards.list(customer),recoveries.outstanding(customer));
        });
    }
    /** 第十一批 TK-04-R5（BSM L877 客人端只展示结果；ENUM L77 与消费单据双向可查）：本人商户消费奖励及来源单号。 */
    @GetMapping("/customer/merchant-rewards")
    public ApiResponse<List<MerchantConsumptionRewardService.RewardView>> ownMerchantRewards(@RequestHeader(name="X-Customer-Token",required=false)String token,@RequestHeader(name="Authorization",required=false)String authorization){
        return audited("merchant.reward.customer.viewed","not_applicable",null,()->merchantRewards.listForCustomer(customer(token,authorization)));
    }
    private String customer(String token,String authorization){
        String resolved=token==null||token.isBlank()?authorization!=null&&authorization.startsWith("Bearer ")?authorization.substring(7):null:token;
        if(resolved==null||resolved.isBlank())throw ApiException.unauthorized("CUSTOMER_UNAUTHENTICATED","缺少客人登录凭证，请重新登录");
        return customers.requireCustomerRecord(resolved).id();
    }
    private <T> ApiResponse<T> audited(String event,String permission,String id,java.util.function.Supplier<T> call){return ApiResponse.ok(operations.readSingle(com.complex.platform.evidence.AuditedOperation.of(event,event+".failed",permission,"ticket_reward"),call,result->result instanceof TicketRewardConfig c?c.id():id==null?"all":id));}
}
