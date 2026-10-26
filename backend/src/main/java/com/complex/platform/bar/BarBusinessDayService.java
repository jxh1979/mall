package com.complex.platform.bar;

// 文件版本：V0.01

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.EvidenceAttachmentValidator;
import com.complex.platform.evidence.EvidenceEventRepository;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewRepository;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.requireText;

/**
 * 每家酒吧的营业日版本（ENUM:187「按每个项目、每家酒吧可配，调整须最高权限审批并留痕……须同时配置上下限」；
 * BSM:1682「酒吧仍按每家酒吧配置」）。逐段照综合体那一套（AmusementBusinessDayService）：提交即进最高权限审批，
 * 只从未来的生效时点起用，批准时已过点记 expired；没有已批准版本的酒吧按出厂起点 10:00（BusinessDayResolver）。
 */
@Service
public class BarBusinessDayService {
    public static final String VIEW = "bar.business_day.view";
    public static final String MANAGE = "bar.business_day.manage";
    public static final String ACTION = "bar.business_day.configure";
    public static final String OBJECT = "bar_business_day_config";

    private final JdbcTemplate jdbc;
    private final AuthorizationService auth;
    private final SupervisorReviewService reviews;
    private final SupervisorReviewRepository reviewRepository;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;
    private final EvidenceEventRepository evidence;
    private final EvidenceAttachmentValidator attachments;
    private final ObjectMapper json;
    private final BusinessDayResolver businessDays;
    private Clock clock = Clock.systemUTC();

    public BarBusinessDayService(JdbcTemplate jdbc, AuthorizationService auth,
            SupervisorReviewService reviews, SupervisorReviewRepository reviewRepository,
            IdempotencyService idempotency, AuditEventService audit, EvidenceEventRepository evidence,
            EvidenceAttachmentValidator attachments, ObjectMapper json, BusinessDayResolver businessDays) {
        this.jdbc=jdbc; this.auth=auth; this.reviews=reviews; this.reviewRepository=reviewRepository;
        this.idempotency=idempotency; this.audit=audit; this.evidence=evidence; this.attachments=attachments; this.json=json;
        this.businessDays=businessDays;
    }

    public record Request(String idempotencyKey, String barId, Integer cutoffHour, Integer minCutoffHour, Integer maxCutoffHour,
            Integer closeHour, Integer minCloseHour, Integer maxCloseHour, Instant effectiveAt, String reason,
            String finalApproverId, List<String> attachmentIds) {}
    public record Config(String id, String barId, String projectId, String version, int cutoffHour, int minCutoffHour,
            int maxCutoffHour, int closeHour, int minCloseHour, int maxCloseHour, Instant effectiveAt, String status,
            String reviewId, String reason, List<String> attachmentIds, String createdBy, Instant createdAt,
            String decidedBy, Instant decidedAt) {}
    public record BarOption(String id, String name, String projectId) {}
    public record Catalog(List<BarOption> bars, List<Config> versions) {}

    public Catalog list(String bar) {
        auth.requirePermission(VIEW);
        requireCompanyContext();
        var reach=auth.projectReach(auth.currentActor(),VIEW);
        List<BarOption> options=jdbc.query("""
            SELECT id,merchant_name,project_id FROM merchants WHERE merchant_type='bar' AND archived_at IS NULL AND project_id IS NOT NULL
             ORDER BY merchant_name,id
            """,(rs,n)->new BarOption(rs.getString(1),rs.getString(2),rs.getString(3))).stream().filter(b->reach.covers(b.projectId())).toList();
        if(bar==null || bar.isBlank())return new Catalog(options,List.of());
        scope(bar,VIEW);
        return new Catalog(options,jdbc.query("SELECT * FROM bar_business_day_configs WHERE bar_id=? ORDER BY created_at DESC,id",this::map,bar));
    }

    /** 审批原件只借当前这张审批单的可见性，不扩大配置列表。 */
    public Config original(String id) {
        Config row=find(id);
        auth.requirePermission("governance.supervisor_review.view");
        if(row.reviewId()==null || reviews.findReview(row.reviewId()).isEmpty())throw ApiException.forbidden("FORBIDDEN","无权读取该配置审批原件");
        return row;
    }

    @Transactional
    public Config submit(Request r) {
        if(r==null)throw invalid("请填写酒吧营业日配置");
        String project=scope(r.barId(),MANAGE);
        validate(r);
        requireAttachments(r.barId(),project,r.attachmentIds());
        return idempotency.execute("barBusinessDayConfig",requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
            lockBar(r.barId());
            scope(r.barId(),MANAGE);
            Instant now=Instant.now(clock);
            if(r.effectiveAt()==null || !r.effectiveAt().isAfter(now))throw invalid("生效时间必须在未来");
            requireAttachments(r.barId(),project,r.attachmentIds());
            int seq=jdbc.queryForObject("SELECT count(*)+1 FROM bar_business_day_configs WHERE bar_id=?",Integer.class,r.barId());
            String version="V"+seq/100+"."+String.format("%02d",seq%100);
            String id="bbd_"+UUID.randomUUID().toString().replace("-","");
            jdbc.update("""
                INSERT INTO bar_business_day_configs(id,bar_id,project_id,version,cutoff_hour,min_cutoff_hour,max_cutoff_hour,
                  close_hour,min_close_hour,max_close_hour,effective_at,status,reason,attachment_ids,created_by,created_at)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,'pending_review',?,?::jsonb,?,?)
                """,id,r.barId(),project,version,r.cutoffHour(),r.minCutoffHour(),r.maxCutoffHour(),
                    r.closeHour(),r.minCloseHour(),r.maxCloseHour(),Timestamp.from(r.effectiveAt()),r.reason(),
                    encode(r.attachmentIds()==null?List.of():r.attachmentIds()),auth.currentActor().id(),Timestamp.from(now));
            List<Config> previous=jdbc.query("SELECT * FROM bar_business_day_configs WHERE bar_id=? AND status='approved' AND effective_at<=? ORDER BY effective_at DESC LIMIT 1",this::map,r.barId(),Timestamp.from(r.effectiveAt()));
            String snapshot=r.reason()+"\n"+encode(Map.of("barId",r.barId(),"projectId",project,"previousVersions",previous,"proposedConfig",r));
            var decision=reviews.evaluate(new SupervisorReviewEvaluateRequest(r.idempotencyKey()+":review",snapshot,
                    r.attachmentIds()==null?List.of():r.attachmentIds(),ACTION,OBJECT,id,0L,snapshot,r.finalApproverId(),List.of()));
            if(!"pending_review".equals(decision.reviewStatus()) || !"top_authority".equals(decision.approvalLevel()))
                throw ApiException.conflict("BAR_BUSINESS_DAY_APPROVAL_REQUIRED","酒吧营业日配置必须进入最高权限审批");
            jdbc.update("UPDATE bar_business_day_configs SET review_id=? WHERE id=?",decision.reviewId(),id);
            audit.recordSuccess("bar.business_day.submitted",MANAGE,OBJECT,id,null,r.idempotencyKey(),
                    Map.of("bar_id",r.barId(),"project_id",project,"review_id",decision.reviewId(),"version",version));
            return find(id);
        },Config.class);
    }

    @EventListener
    @Transactional
    public void decided(SupervisorReviewDecisionRecord decision) {
        if(!OBJECT.equals(decision.objectType()) || !ACTION.equals(decision.actionType())
                || !List.of("approved","rejected").contains(decision.reviewStatus()))return;
        var persisted=reviewRepository.findSupervisorReviewById(decision.reviewId()).orElse(null);
        if(persisted==null || !Objects.equals(persisted.objectId(),decision.objectId())
                || !Objects.equals(persisted.reviewStatus(),decision.reviewStatus()))return;
        List<Config> found=jdbc.query("SELECT * FROM bar_business_day_configs WHERE id=? FOR UPDATE",this::map,decision.objectId());
        if(found.isEmpty())return;
        Config row=found.getFirst();
        if(!"pending_review".equals(row.status()) || !Objects.equals(row.reviewId(),decision.reviewId()))return;
        lockBar(row.barId());
        Instant now=Instant.now(clock);
        String status="approved".equals(decision.reviewStatus()) && !now.isBefore(row.effectiveAt()) ? "expired" : decision.reviewStatus();
        if("approved".equals(status)) {
            requireAttachments(row.barId(),row.projectId(),row.attachmentIds());
            if(jdbc.queryForObject("SELECT count(*) FROM bar_business_day_configs WHERE bar_id=? AND status='approved' AND effective_at=?",Integer.class,row.barId(),Timestamp.from(row.effectiveAt()))>0)
                throw ApiException.conflict("BAR_BUSINESS_DAY_VERSION_CONFLICT","该酒吧同一时点已有生效版本，请重新提交不同生效时间");
        }
        jdbc.update("UPDATE bar_business_day_configs SET status=?,decided_by=?,decided_at=? WHERE id=?",
                status,auth.currentActor().id(),Timestamp.from(now),row.id());
        if("approved".equals(status))businessDays.invalidate();
        audit.recordSuccess("bar.business_day.decided","governance.supervisor_review.approve",OBJECT,row.id(),null,null,
                Map.of("bar_id",row.barId(),"status",status,"review_id",decision.reviewId()));
    }

    /** 附件业务对象＝酒吧 id。 */
    public void requireEvidenceScope(String bar,String evidencePermission) {
        String project=scope(bar,"evidence.timeline.view".equals(evidencePermission)?VIEW:MANAGE);
        auth.requirePermission(evidencePermission);
        if(!auth.projectReach(auth.currentActor(),evidencePermission).covers(project))
            throw ApiException.forbidden("FORBIDDEN","附件权限未覆盖本酒吧所在项目");
    }

    /** 酒吧（业态为酒吧的商户，V267）须存在、未存档且挂了项目；返回所在项目。 */
    private String scope(String bar,String permission) {
        auth.requirePermission(permission);
        requireCompanyContext();
        List<String> project=jdbc.queryForList("SELECT project_id FROM merchants WHERE id=? AND merchant_type='bar' AND archived_at IS NULL AND project_id IS NOT NULL",
                String.class,requireText(bar,"bar_id"));
        if(project.isEmpty())throw invalid("酒吧不存在、已存档或未挂项目");
        if(!auth.projectReach(auth.currentActor(),permission).covers(project.getFirst()))
            throw ApiException.forbidden("FORBIDDEN","无权访问该酒吧营业日配置");
        return project.getFirst();
    }
    private void requireCompanyContext() {
        if(auth.currentActor().merchantId()!=null)throw ApiException.forbidden("FORBIDDEN","商户限定授权不能管理酒吧营业日");
    }
    private void lockBar(String bar) {
        if(jdbc.queryForList("SELECT id FROM merchants WHERE id=? FOR UPDATE",String.class,bar).isEmpty())throw invalid("酒吧不存在");
    }
    private void requireAttachments(String bar,String project,List<String> ids) {
        if(ids==null || ids.isEmpty())return;
        auth.requirePermission("evidence.timeline.view");
        if(!auth.projectReach(auth.currentActor(),"evidence.timeline.view").covers(project))
            throw ApiException.forbidden("FORBIDDEN","附件查看权限未覆盖本酒吧所在项目");
        for(String id:ids) {
            var attachment=evidence.findAttachmentById(id).orElseThrow(()->invalid("配置依据附件不存在"));
            if(!OBJECT.equals(attachment.businessType()) || !bar.equals(attachment.businessId()) || attachment.merchantId()!=null)
                throw invalid("配置依据必须绑定本酒吧且不能引用商户私有附件");
            attachments.requireUploaded(attachment);
        }
    }
    private static void validate(Request r) {
        requireText(r.reason(),"reason");
        if(!bounded(r.cutoffHour(),r.minCutoffHour(),r.maxCutoffHour()))
            throw invalid("营业日起点小时及上下限须完整，满足0≤下限≤起点小时≤上限≤23");
        if(!bounded(r.closeHour(),r.minCloseHour(),r.maxCloseHour()))
            throw invalid("闭店小时及上下限须完整，满足0≤下限≤闭店小时≤上限≤23");
    }
    /** 与综合体那一套共用的上下限校验。 */
    public static boolean bounded(Integer hour,Integer min,Integer max) {
        return hour!=null && min!=null && max!=null && min>=0 && max<=23 && hour>=min && hour<=max;
    }
    Config find(String id) {
        return jdbc.query("SELECT * FROM bar_business_day_configs WHERE id=?",this::map,id).stream().findFirst()
                .orElseThrow(()->ApiException.notFound("BAR_BUSINESS_DAY_NOT_FOUND","酒吧营业日配置版本不存在"));
    }
    private Config map(ResultSet rs,int n)throws SQLException {
        try {return new Config(rs.getString("id"),rs.getString("bar_id"),rs.getString("project_id"),rs.getString("version"),
                rs.getInt("cutoff_hour"),rs.getInt("min_cutoff_hour"),rs.getInt("max_cutoff_hour"),
                rs.getInt("close_hour"),rs.getInt("min_close_hour"),rs.getInt("max_close_hour"),
                rs.getTimestamp("effective_at").toInstant(),rs.getString("status"),rs.getString("review_id"),rs.getString("reason"),
                json.readValue(rs.getString("attachment_ids"),json.getTypeFactory().constructCollectionType(List.class,String.class)),
                rs.getString("created_by"),rs.getTimestamp("created_at").toInstant(),rs.getString("decided_by"),instant(rs,"decided_at"));}
        catch(java.io.IOException error){throw new IllegalStateException("营业日配置附件记录无法读取",error);}
    }
    private String encode(Object value){try{return json.writeValueAsString(value);}catch(java.io.IOException error){throw new IllegalStateException(error);}}
    private static Instant instant(ResultSet rs,String key)throws SQLException {Timestamp t=rs.getTimestamp(key);return t==null?null:t.toInstant();}
    private static ApiException invalid(String message){return ApiException.badRequest("VALIDATION_FAILED",message);}
}
