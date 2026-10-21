package com.complex.platform.customer;

// 文件版本：V0.02

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PointsRecoveryWaiverService {
    public static final String VIEW = "finance.points_recovery.view";
    public static final String WAIVE = "finance.points_recovery.waive";
    public record Line(String rewardId,String projectId,String merchantId,long originalPoints,
            long remainingPoints,String status,Instant createdAt,Instant updatedAt) {}
    public record Waiver(String id,String customerId,String configVersionId,long threshold,long waivedPoints,
            String actorId,String reason,List<Line> lines,Instant createdAt) {}
    public record Detail(String customerId,String configVersionId,long threshold,long pendingPoints,
            List<Line> lines,List<Waiver> waivers) {}
    public record Request(String idempotencyKey,String configVersionId,long expectedPendingPoints,String reason) {}
    private final JdbcTemplate jdbc;
    private final CustomerRepository customers;
    private final TicketRewardConfigService configs;
    private final AuthorizationService auth;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;
    private final ObjectMapper json;
    public PointsRecoveryWaiverService(JdbcTemplate jdbc,CustomerRepository customers,TicketRewardConfigService configs,
            AuthorizationService auth,IdempotencyService idempotency,AuditEventService audit,ObjectMapper json) {
        this.jdbc=jdbc;this.customers=customers;this.configs=configs;this.auth=auth;
        this.idempotency=idempotency;this.audit=audit;this.json=json;
    }

    public Detail get(String customerId) {
        auth.requirePermission(VIEW);
        List<Line> lines=lines(customerId,false);
        requireScope(lines,VIEW);
        TicketRewardConfig config=configs.effective("recovery_waiver",null,null,Instant.now());
        return new Detail(customerId,config.id(),config.values().fixedValue(),pending(lines),lines,
                jdbc.query("SELECT * FROM customer_points_recovery_waivers WHERE customer_id=? ORDER BY created_at DESC,id", (rs,n)->{
                    try {
                        List<Line> originalLines=json.readValue(rs.getString("lines"),new TypeReference<List<Line>>(){});
                        requireScope(originalLines,VIEW);
                        return new Waiver(rs.getString("id"),rs.getString("customer_id"),rs.getString("config_version_id"),
                            rs.getLong("threshold"),rs.getLong("waived_points"),rs.getString("actor_id"),rs.getString("reason"),
                            originalLines,rs.getTimestamp("created_at").toInstant());}
                    catch(java.io.IOException e){throw new IllegalStateException("积分豁免原件无法读取",e);}
                },customerId));
    }

    @Transactional
    @com.complex.platform.gate.GuardedAction(capability=WAIVE)
    public Waiver waive(String customerId,Request request) {
        auth.requirePermissions(VIEW,WAIVE);
        if(request==null || request.idempotencyKey()==null || request.idempotencyKey().isBlank()
                || request.configVersionId()==null || request.reason()==null || request.reason().isBlank()
                || request.expectedPendingPoints()<=0) throw ApiException.badRequest("VALIDATION_FAILED","须填写本次确认的全部待追回积分、配置版本及豁免原因");
        List<Line> visible=lines(customerId,false);
        requireScope(visible,VIEW);
        requireScope(visible,WAIVE);
        return idempotency.execute("pointsRecoveryWaiver:"+customerId,request.idempotencyKey(),request,()->{
            customers.findPointsByCustomerIdForUpdate(customerId)
                    .orElseThrow(()->ApiException.notFound("POINTS_ACCOUNT_NOT_FOUND","会员积分账户不存在"));
            List<Line> lines=lines(customerId,true);
            requireScope(lines,VIEW);
            requireScope(lines,WAIVE);
            var member=customers.findCustomerById(customerId).orElseThrow();
            if(!"active".equals(member.status())) throw ApiException.conflict("CUSTOMER_NOT_ACTIVE","会员已停用或注销，须按对应的积分处理流程办理");
            Instant now=Instant.now();
            TicketRewardConfig config=configs.effective("recovery_waiver",null,null,now);
            long total=pending(lines),threshold=config.values().fixedValue();
            if(!config.id().equals(request.configVersionId()) || total!=request.expectedPendingPoints())
                throw ApiException.conflict("POINTS_RECOVERY_CHANGED","待追回总额或豁免配置已变化，请重新查看并确认");
            if(total<=0 || total>threshold)
                throw ApiException.conflict("POINTS_RECOVERY_ABOVE_WAIVER_LIMIT","须按会员全部待追回总额判断，不得分笔豁免或超过已批准的豁免线");
            List<Line> pending=lines.stream().filter(line->"pending".equals(line.status())).toList();
            Waiver result=new Waiver("prw_"+UUID.randomUUID().toString().replace("-",""),customerId,config.id(),
                    threshold,total,auth.currentActor().id(),request.reason().trim(),pending,now);
            jdbc.update("INSERT INTO customer_points_recovery_waivers(id,customer_id,config_version_id,threshold,waived_points,actor_id,reason,lines,created_at) VALUES(?,?,?,?,?,?,?,?::jsonb,?)",
                    result.id(),customerId,config.id(),threshold,total,result.actorId(),result.reason(),encode(pending),Timestamp.from(now));
            jdbc.update("UPDATE customer_points_recoveries SET remaining_points=0,status='waived',updated_at=? WHERE customer_id=? AND status='pending'",Timestamp.from(now),customerId);
            audit.recordSuccess("points.recovery.waived",WAIVE,"points_recovery_waiver",result.id(),null,
                    request.idempotencyKey(),Map.of("customer_id",customerId,"waived_points",total,"config_version_id",config.id()));
            return result;
        },Waiver.class);
    }

    private List<Line> lines(String customerId,boolean lock) {
        return jdbc.query("""
                SELECT r.*,COALESCE(r.reward_id,r.merchant_reward_id) AS line_id,COALESCE(w.project_id,m.project_id) AS line_project_id,
                       COALESCE(s.seller_merchant_id,m.merchant_id) AS line_merchant_id FROM customer_points_recoveries r
                LEFT JOIN ticket_use_rewards w ON w.id=r.reward_id
                LEFT JOIN ticket_order_sales_contracts s ON s.order_id=w.order_id
                LEFT JOIN merchant_consumption_rewards m ON m.id=r.merchant_reward_id
                WHERE r.customer_id=? ORDER BY r.created_at,line_id
                """+(lock?" FOR UPDATE OF r":""),(rs,n)->new Line(rs.getString("line_id"),rs.getString("line_project_id"),
                    rs.getString("line_merchant_id"),rs.getLong("original_points"),rs.getLong("remaining_points"),
                    rs.getString("status"),rs.getTimestamp("created_at").toInstant(),rs.getTimestamp("updated_at").toInstant()),customerId);
    }
    private void requireScope(List<Line> lines,String permission) {
        if(lines.isEmpty()) throw ApiException.notFound("POINTS_RECOVERY_NOT_FOUND","该会员没有待追回或已处理的积分记录");
        var reach=auth.projectReach(auth.currentActor(),permission);
        String merchant=auth.currentActor().merchantId();
        if(lines.stream().anyMatch(line->!reach.covers(line.projectId()) || (merchant!=null&&!merchant.equals(line.merchantId()))))
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN","无权处理该会员涉及的全部积分归属，不能只挑可见部分豁免");
    }
    private long pending(List<Line> lines) {
        long total=0;
        for(Line line:lines)if("pending".equals(line.status()))total=Math.addExact(total,line.remainingPoints());
        return total;
    }
    private String encode(Object value){try{return json.writeValueAsString(value);}catch(java.io.IOException e){throw new IllegalStateException(e);}}
}
