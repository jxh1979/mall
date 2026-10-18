package com.complex.platform.customer;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.EvidenceAttachmentRecord;
import com.complex.platform.evidence.EvidenceEventRepository;
import com.complex.platform.evidence.EvidenceAttachmentValidator;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketRewardConfigService {
    public static final String VIEW = "ticket.reward.config.view";
    public static final String MANAGE = "ticket.reward.config.manage";
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final AuthorizationService auth;
    private final SupervisorReviewService reviews;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;
    private final EvidenceEventRepository evidence;
    private final EvidenceAttachmentValidator attachments;

    public TicketRewardConfigService(JdbcTemplate jdbc, ObjectMapper json, AuthorizationService auth,
            SupervisorReviewService reviews, IdempotencyService idempotency, AuditEventService audit,
            EvidenceEventRepository evidence, EvidenceAttachmentValidator attachments) {
        this.jdbc=jdbc; this.json=json; this.auth=auth; this.reviews=reviews;
        this.idempotency=idempotency; this.audit=audit; this.evidence=evidence; this.attachments=attachments;
    }

    public List<TicketRewardConfig> list(String projectId) {
        scope(projectId, VIEW);
        String merchant=auth.currentActor().merchantId();
        return jdbc.query("SELECT c.* FROM ticket_reward_config_versions c LEFT JOIN ticket_types t ON t.id=c.ticket_type_id WHERE c.project_id IS NOT DISTINCT FROM ? AND (?::text IS NULL OR t.bar_id=?) ORDER BY c.created_at DESC,c.id", this::map, projectId,merchant,merchant);
    }

    @Transactional
    public TicketRewardConfig submit(TicketRewardConfig.Request r) {
        if(r==null || r.values()==null || r.effectiveAt()==null || !r.effectiveAt().isAfter(Instant.now()))
            throw invalid("须填写规则内容及未来生效时间");
        scope(r.projectId(),MANAGE);
        String kind=requireText(r.kind(),"kind");
        if(!Set.of("points","growth","national","sales_contract").contains(kind)) throw invalid("不支持的奖励配置类型");
        validate(r);
        requireApproverScope(r);
        return idempotency.execute("ticketRewardConfig",requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
            String key=kind+":"+Objects.toString(r.projectId(),"")+":"+Objects.toString(r.ticketTypeId(),"");
            jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))", rs -> {}, key);
            int seq=jdbc.queryForObject("SELECT count(*)+1 FROM ticket_reward_config_versions WHERE kind=? AND project_id IS NOT DISTINCT FROM ? AND ticket_type_id IS NOT DISTINCT FROM ?",Integer.class,kind,r.projectId(),r.ticketTypeId());
            String id="trc_"+UUID.randomUUID().toString().replace("-","");
            Instant now=Instant.now();
            jdbc.update("INSERT INTO ticket_reward_config_versions(id,kind,ticket_type_id,project_id,version,payload,effective_at,status,created_by,created_at) VALUES(?,?,?,?,?,?::jsonb,?,'pending_review',?,?)",
                    id,kind,r.ticketTypeId(),r.projectId(),"V"+(seq/100)+"."+String.format("%02d",seq%100),encode(r.values()),Timestamp.from(r.effectiveAt()),auth.currentActor().id(),Timestamp.from(now));
            String action="ticket.reward."+switch(kind){case "national" -> "national";case "sales_contract" -> "sales_contract";default -> "rule";};
            SupervisorReviewDecisionRecord review=reviews.evaluate(new SupervisorReviewEvaluateRequest(
                    r.idempotencyKey()+":review",requireText(r.reason(),"reason"),r.values().attachmentIds(),action,
                    "ticket_reward_config",id,0L,r.reason(),r.finalApproverId(),r.parallelLines()));
            if(!"pending_review".equals(review.reviewStatus())) throw ApiException.conflict("TICKET_REWARD_APPROVAL_REQUIRED","规则须进入审批，不得直接生效");
            jdbc.update("UPDATE ticket_reward_config_versions SET review_id=? WHERE id=?",review.reviewId(),id);
            audit.recordSuccess("ticket.reward.config.submitted",MANAGE,"ticket_reward_config",id,null,r.idempotencyKey(),Map.of("kind",kind,"review_id",review.reviewId()));
            return find(id);
        },TicketRewardConfig.class);
    }

    @EventListener
    @Transactional
    public void decided(SupervisorReviewDecisionRecord decision) {
        if(!"ticket_reward_config".equals(decision.objectType()) || !decision.actionType().startsWith("ticket.reward.")) return;
        if(!Set.of("approved","rejected").contains(decision.reviewStatus())) return;
        List<TicketRewardConfig> found=jdbc.query("SELECT * FROM ticket_reward_config_versions WHERE id=? FOR UPDATE",this::map,decision.objectId());
        if(found.isEmpty()) return;
        TicketRewardConfig row=found.getFirst();
        if(!"pending_review".equals(row.status()) || !Objects.equals(row.reviewId(),decision.reviewId())) return;
        jdbc.update("UPDATE ticket_reward_config_versions SET status=?,approved_at=? WHERE id=?",decision.reviewStatus(),Timestamp.from(Instant.now()),row.id());
    }

    @Transactional
    public TicketRewardConfig publish(String id, TicketRewardConfig.PublishRequest r) {
        TicketRewardConfig original=find(id); scope(original.projectId(),MANAGE);
        if(!"national".equals(original.kind()))requireEvidenceScope(original.projectId()+":"+original.ticketTypeId(),false);
        if(r==null) throw invalid("请填写发布说明");
        requireText(r.reason(),"reason");
        return idempotency.execute("publishTicketRewardConfig:"+id,requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
            TicketRewardConfig row=jdbc.query("SELECT * FROM ticket_reward_config_versions WHERE id=? FOR UPDATE",this::map,id).getFirst();
            Instant now=Instant.now();
            if(!"approved".equals(row.status()) || row.publishedAt()!=null || !now.isBefore(row.effectiveAt()))
                throw ApiException.conflict("TICKET_REWARD_NOT_PUBLISHABLE","仅审批通过且尚未到生效时间的版本可以公告");
            jdbc.update("UPDATE ticket_reward_config_versions SET published_at=? WHERE id=?",Timestamp.from(now),id);
            audit.recordSuccess("ticket.reward.config.published",MANAGE,"ticket_reward_config",id,null,r.idempotencyKey(),Map.of("reason",r.reason(),"effective_at",row.effectiveAt().toString()));
            return find(id);
        },TicketRewardConfig.class);
    }

    TicketRewardConfig effective(String kind,String ticketType,String project,Instant at) {
        List<TicketRewardConfig> rows=jdbc.query("SELECT * FROM ticket_reward_config_versions WHERE kind=? AND ticket_type_id IS NOT DISTINCT FROM ? AND project_id IS NOT DISTINCT FROM ? AND status='approved' AND published_at<effective_at AND effective_at<=? ORDER BY effective_at DESC LIMIT 1",this::map,kind,ticketType,project,Timestamp.from(at));
        if(rows.isEmpty()) throw ApiException.conflict("TICKET_REWARD_CONFIG_REQUIRED","缺少已审批、公告且生效的门票奖励或销售合同配置");
        return rows.getFirst();
    }

    public List<TicketRewardConfig> publishedRules(String ticketType,String project) {
        return jdbc.query("SELECT * FROM ticket_reward_config_versions WHERE kind IN ('points','growth') AND (?::text IS NULL OR ticket_type_id=?) AND (?::text IS NULL OR project_id=?) AND status='approved' AND published_at IS NOT NULL ORDER BY effective_at DESC,kind",this::map,ticketType,ticketType,project,project);
    }

    public List<TicketRewardController.PublishedRule> announcements(String ticketType,String project) {
        return jdbc.query("SELECT c.*,t.ticket_name,p.name AS project_name FROM ticket_reward_config_versions c JOIN ticket_types t ON t.id=c.ticket_type_id JOIN projects p ON p.id=c.project_id WHERE c.kind IN ('points','growth') AND (?::text IS NULL OR c.ticket_type_id=?) AND (?::text IS NULL OR c.project_id=?) AND c.status='approved' AND c.published_at IS NOT NULL ORDER BY c.effective_at DESC,c.kind",(rs,n)->{
            TicketRewardConfig c=map(rs,n);
            return new TicketRewardController.PublishedRule(c.id(),c.ticketTypeId(),rs.getString("ticket_name"),c.projectId(),rs.getString("project_name"),c.kind(),c.version(),c.values().fixedValue(),c.effectiveAt(),c.publishedAt());
        },ticketType,ticketType,project,project);
    }

    private void validate(TicketRewardConfig.Request r) {
        var v=r.values();
        if("national".equals(r.kind())) {
            if(r.projectId()!=null || r.ticketTypeId()!=null) throw invalid("全国参数不可限定项目或票种");
            range(v.waitingHours()==null?24L:v.waitingHours().longValue(),v.minimumWaitingHours()==null?null:v.minimumWaitingHours().longValue(),v.maximumWaitingHours()==null?null:v.maximumWaitingHours().longValue());
            if(v.waitingHours()==null) throw invalid("等待时长须明确填写，初始建议24小时");
            if(v.issuancePriceCent()==null || v.minimumPriceCent()==null || v.maximumPriceCent()==null || v.minimumPriceCent().signum()<=0 || v.issuancePriceCent().compareTo(v.minimumPriceCent())<0 || v.issuancePriceCent().compareTo(v.maximumPriceCent())>0 || v.issuancePriceCent().scale()>6)
                throw invalid("积分发行单价及正数上下限须完整填写，精度最多六位小数分");
            entity(v.receiverEntityId());
            // 全国审批显式指定积分结算公司；不得由当前登录组织或商户推断收款主体。
            return;
        }
        requireText(r.ticketTypeId(),"ticket_type_id"); requireText(r.projectId(),"project_id");
        List<String> bars=jdbc.query("SELECT bar_id FROM ticket_types WHERE id=?",(rs,n)->rs.getString(1),r.ticketTypeId());
        if(bars.isEmpty()) throw invalid("票种不存在");
        requireEvidenceScope(r.projectId()+":"+r.ticketTypeId(),false);
        if(!"sales_contract".equals(r.kind())) {range(v.fixedValue(),v.minimumValue(),v.maximumValue());return;}
        requireText(v.contractNumber(),"contract_number"); entity(v.sellerEntityId());
        if(!Objects.equals(bars.getFirst(),v.sellerMerchantId())) throw invalid("单店酒吧合同销售方必须是该票种酒吧；公司票不可填履约商户代替销售方");
        if(v.sellerMerchantId()!=null) {
            Integer count=jdbc.queryForObject("SELECT count(*) FROM merchants m WHERE m.id=? AND COALESCE((SELECT b.contract_entity_id FROM business_entity_bindings b WHERE b.merchant_id=m.id AND b.status='active' AND b.effective_from<=CURRENT_DATE AND (b.effective_to IS NULL OR b.effective_to>=CURRENT_DATE) ORDER BY b.created_at DESC LIMIT 1),m.contract_entity_id)=? AND m.status='active'",Integer.class,v.sellerMerchantId(),v.sellerEntityId());
            if(count==null || count!=1) throw invalid("销售法律主体与酒吧已生效合同主体不一致");
        } else {
            if(!domainCompanies(r.ticketTypeId(),r.projectId()).contains(v.sellerEntityId())) throw invalid("公司票销售方须为本票权益对应的有效域主管公司，并已关联该综合体");
        }
        if(v.attachmentIds()==null || v.attachmentIds().isEmpty()) throw invalid("须上传对应销售合同凭证");
        for(String attachmentId:v.attachmentIds()) {
            EvidenceAttachmentRecord a=evidence.findAttachmentById(attachmentId).orElseThrow(()->invalid("销售合同凭证不存在"));
            if(!"ticket_sales_contract".equals(a.businessType()) || !(r.projectId()+":"+r.ticketTypeId()).equals(a.businessId())) throw invalid("合同凭证须绑定本综合体及票种销售合同");
            attachments.requireUploaded(a);
            if(auth.currentActor().merchantId()!=null && !auth.currentActor().merchantId().equals(a.merchantId())) throw ApiException.forbidden("FORBIDDEN","无权使用其他商户合同附件");
        }
    }

    /** 附件上传与读取共用业务权限，平台工作人员无需伪造商户身份。 */
    public void requireEvidenceScope(String businessId,boolean reading) {
        String[] parts=requireText(businessId,"business_id").split(":",-1);
        if(parts.length!=2 || parts[0].isBlank() || parts[1].isBlank()) throw invalid("销售合同凭证须明确综合体与票种");
        scope(parts[0],reading?VIEW:MANAGE);
        if(jdbc.queryForObject("SELECT count(*) FROM projects WHERE id=? AND status='active'",Integer.class,parts[0])!=1) throw invalid("综合体不存在或未生效");
        List<String> bars=jdbc.query("SELECT bar_id FROM ticket_types WHERE id=?",(rs,n)->rs.getString(1),parts[1]);
        if(bars.isEmpty())throw invalid("票种不存在");
        String merchant=bars.getFirst();
        if(auth.currentActor().merchantId()!=null && !auth.currentActor().merchantId().equals(merchant)) throw ApiException.forbidden("FORBIDDEN","无权访问其他商户或公司票的销售配置");
        if(merchant!=null && jdbc.queryForObject("SELECT count(*) FROM merchants m WHERE m.id=? AND m.status='active' AND (m.project_id=? OR EXISTS(SELECT 1 FROM merchant_projects mp WHERE mp.merchant_id=m.id AND mp.project_id=?))",Integer.class,merchant,parts[0],parts[0])!=1)throw invalid("票种酒吧未关联该综合体");
        if(merchant==null)domainCompanies(parts[1],parts[0]);
    }

    private List<String> domainCompanies(String ticketType,String project) {
        List<String> owners=jdbc.queryForList("""
            SELECT DISTINCT COALESCE(b.operation_entity_id,m.business_entity_id)
            FROM ticket_type_entitlements t JOIN entitlement_templates e ON e.id=t.entitlement_template_id
            JOIN ticket_types tt ON tt.id=t.ticket_type_id
            LEFT JOIN projects venue ON venue.id=e.venue_id
            LEFT JOIN merchants m ON m.id=COALESCE(tt.bar_id,e.merchant_id,venue.merchant_id)
            LEFT JOIN LATERAL (SELECT operation_entity_id FROM business_entity_bindings
              WHERE merchant_id=m.id AND status='active' AND effective_from<=CURRENT_DATE
                AND (effective_to IS NULL OR effective_to>=CURRENT_DATE) ORDER BY created_at DESC LIMIT 1) b ON TRUE
            WHERE t.ticket_type_id=?
            """,String.class,ticketType);
        if(owners.isEmpty() || owners.contains(null))throw invalid("票种权益尚未明确对应经营商户及域主管公司，须先补齐真实归属");
        for(String owner:owners)if(jdbc.queryForObject("SELECT count(*) FROM business_entities e JOIN business_entity_projects p ON p.business_entity_id=e.id WHERE e.id=? AND p.project_id=? AND e.status='active' AND e.entity_role IN ('management_company','project_company')",Integer.class,owner,project)!=1)throw invalid("票种域主管公司未有效关联该综合体");
        return owners;
    }

    private void requireApproverScope(TicketRewardConfig.Request r) {
        String approver=requireText(r.finalApproverId(),"final_approver_id");
        // user_position_assignments.user_id 为主键且无 status/effective 列；任命时间不得在未来，已停岗档案不可回退到用户组织。
        List<String> companies=jdbc.queryForList("SELECT COALESCE((SELECT company_id FROM staff_profiles WHERE user_id=u.id AND status='active' AND registration_status='approved' ORDER BY updated_at DESC LIMIT 1),(SELECT organization_id FROM user_position_assignments WHERE user_id=u.id AND assigned_at<=now()),u.organization_id) FROM access_users u WHERE u.id=? AND u.status='active' AND NOT EXISTS(SELECT 1 FROM staff_profiles s WHERE s.user_id=u.id AND (s.status<>'active' OR s.registration_status<>'approved'))",String.class,approver);
        if(companies.size()!=1 || companies.getFirst()==null)throw invalid("最终审批人尚无有效的所属公司");
        String company=companies.getFirst();
        List<String> domains="national".equals(r.kind())?List.of():domainCompanies(r.ticketTypeId(),r.projectId());
        boolean platform="national".equals(r.kind()) || domains.size()>1;
        if(platform) {
            if(jdbc.queryForObject("SELECT count(*) FROM business_entities WHERE id=? AND entity_role='platform' AND status='active'",Integer.class,company)!=1)throw invalid("全国或跨域奖励须由顶层平台主管审批");
        } else if(!domains.contains(company))throw invalid("最终审批人须属于本票种对应的域主管公司");
    }
    private void scope(String project,String permission) {
        auth.requirePermission(permission);
        if(project==null) {if(!auth.allowsPlatformScope(auth.currentActor(),permission)) throw ApiException.forbidden("FORBIDDEN","全国积分配置须有全国范围授权");}
        else if(!auth.projectReach(auth.currentActor(),permission).covers(project)) throw ApiException.forbidden("FORBIDDEN","无权管理该项目奖励配置");
    }
    private void entity(String id) {
        if(jdbc.queryForObject("SELECT count(*) FROM business_entities WHERE id=? AND status='active'",Integer.class,requireText(id,"entity_id"))!=1) throw invalid("须选择有效法律主体");
    }
    private void range(Long value,Long min,Long max) {
        if(value==null || min==null || max==null || min<0 || value<min || value>max) throw invalid("数值及上下限必须完整且合法");
    }
    TicketRewardConfig find(String id) {
        return jdbc.query("SELECT * FROM ticket_reward_config_versions WHERE id=?",this::map,id).stream().findFirst().orElseThrow(()->ApiException.notFound("TICKET_REWARD_CONFIG_NOT_FOUND","奖励配置不存在"));
    }
    TicketRewardConfig map(ResultSet rs,int row) throws SQLException {
        try {
            return new TicketRewardConfig(rs.getString("id"),rs.getString("kind"),rs.getString("ticket_type_id"),rs.getString("project_id"),rs.getString("version"),json.readValue(rs.getString("payload"),TicketRewardConfig.Values.class),rs.getTimestamp("effective_at").toInstant(),instant(rs,"published_at"),rs.getString("status"),rs.getString("review_id"),rs.getString("created_by"),rs.getTimestamp("created_at").toInstant(),instant(rs,"approved_at"));
        } catch(java.io.IOException e) {throw new IllegalStateException("奖励配置内容无法读取",e);}
    }
    private String encode(Object v) {try{return json.writeValueAsString(v);}catch(java.io.IOException e){throw new IllegalStateException(e);}}
    static Instant instant(ResultSet rs,String field)throws SQLException {Timestamp t=rs.getTimestamp(field);return t==null?null:t.toInstant();}
    private static ApiException invalid(String message){return ApiException.badRequest("VALIDATION_FAILED",message);}
}
