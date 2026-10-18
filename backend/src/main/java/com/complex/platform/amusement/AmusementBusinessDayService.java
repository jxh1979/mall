package com.complex.platform.amusement;

// 文件版本：V0.01

import com.complex.platform.access.AuthorizationService;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.requireText;

@Service
public class AmusementBusinessDayService {
    public static final String VIEW = "amusement.business_day.view";
    public static final String MANAGE = "amusement.business_day.manage";
    public static final String ACTION = "amusement.business_day.configure";
    public static final String OBJECT = "amusement_business_day_config";
    public static final String COMPLEXES_SQL = """
        SELECT a.id AS amusement_project_id,
          CASE WHEN a.space_id IS NOT NULL THEN
            CASE WHEN s.project_id IS NOT NULL AND EXISTS(SELECT 1 FROM projects p WHERE p.id=s.project_id)
              AND (s.merchant_id IS NULL OR s.merchant_id=a.merchant_id)
              AND (a.merchant_id IS NULL OR s.project_id IN
                (SELECT project_id FROM merchants WHERE id=a.merchant_id UNION SELECT project_id FROM merchant_projects WHERE merchant_id=a.merchant_id))
              THEN s.project_id END
            WHEN m.count=1 THEN m.project_id END AS complex_id
        FROM amusement_projects a LEFT JOIN spaces s ON s.id=a.space_id
        LEFT JOIN LATERAL (
          SELECT count(*) AS count,min(project_id) AS project_id FROM
            (SELECT project_id FROM merchants WHERE id=a.merchant_id AND project_id IS NOT NULL
             UNION SELECT project_id FROM merchant_projects WHERE merchant_id=a.merchant_id) ids
        ) m ON TRUE
        """;

    private final JdbcTemplate jdbc;
    private final AuthorizationService auth;
    private final SupervisorReviewService reviews;
    private final SupervisorReviewRepository reviewRepository;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;
    private final EvidenceEventRepository evidence;
    private final EvidenceAttachmentValidator attachments;
    private final ObjectMapper json;
    private Clock clock = Clock.systemUTC();

    public AmusementBusinessDayService(JdbcTemplate jdbc, AuthorizationService auth,
            SupervisorReviewService reviews, SupervisorReviewRepository reviewRepository,
            IdempotencyService idempotency, AuditEventService audit, EvidenceEventRepository evidence,
            EvidenceAttachmentValidator attachments, ObjectMapper json) {
        this.jdbc=jdbc; this.auth=auth; this.reviews=reviews; this.reviewRepository=reviewRepository;
        this.idempotency=idempotency; this.audit=audit; this.evidence=evidence; this.attachments=attachments; this.json=json;
    }

    public record Request(String idempotencyKey, String projectId, String zoneId, Integer cutoffHour,
            Integer minCutoffHour, Integer maxCutoffHour, Instant effectiveAt, String reason,
            String finalApproverId, List<String> attachmentIds) {}
    public record Config(String id, String projectId, String version, String zoneId, int cutoffHour,
            int minCutoffHour, int maxCutoffHour, Instant effectiveAt, String status, String reviewId,
            String reason, List<String> attachmentIds, String createdBy, Instant createdAt,
            String decidedBy, Instant decidedAt) {}
    public record ProjectOption(String id, String name) {}
    public record Catalog(List<ProjectOption> projects, List<Config> versions) {}
    public record Window(String complexId, String versionId, LocalDate businessDate, Instant startAt, Instant endAt) {}

    public Catalog list(String project) {
        auth.requirePermission(VIEW);
        requireCompanyContext();
        var reach=auth.projectReach(auth.currentActor(),VIEW);
        List<ProjectOption> options=jdbc.query("SELECT id,name FROM projects WHERE status='active' ORDER BY name,id",
                (rs,n)->new ProjectOption(rs.getString(1),rs.getString(2))).stream().filter(p->reach.covers(p.id())).toList();
        if(project==null || project.isBlank())return new Catalog(options,List.of());
        scope(project,VIEW);
        return new Catalog(options,jdbc.query("SELECT * FROM amusement_business_day_configs WHERE project_id=? ORDER BY created_at DESC,id",this::map,project));
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
        if(r==null)throw invalid("请填写游乐营业日配置");
        scope(r.projectId(),MANAGE);
        validate(r);
        requireAttachments(r.projectId(),r.attachmentIds());
        return idempotency.execute("amusementBusinessDayConfig",requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
            lockComplex(r.projectId());
            scope(r.projectId(),MANAGE);
            Instant now=Instant.now(clock);
            if(r.effectiveAt()==null || !r.effectiveAt().isAfter(now))throw invalid("生效时间必须在未来");
            requireAttachments(r.projectId(),r.attachmentIds());
            int seq=jdbc.queryForObject("SELECT count(*)+1 FROM amusement_business_day_configs WHERE project_id=?",Integer.class,r.projectId());
            String version="V"+seq/100+"."+String.format("%02d",seq%100);
            String id="abd_"+UUID.randomUUID().toString().replace("-","");
            jdbc.update("""
                INSERT INTO amusement_business_day_configs(id,project_id,version,zone_id,cutoff_hour,min_cutoff_hour,max_cutoff_hour,
                  effective_at,status,reason,attachment_ids,created_by,created_at)
                VALUES(?,?,?,?,?,?,?,?,'pending_review',?,?::jsonb,?,?)
                """,id,r.projectId(),version,r.zoneId(),r.cutoffHour(),r.minCutoffHour(),r.maxCutoffHour(),
                    Timestamp.from(r.effectiveAt()),r.reason(),encode(r.attachmentIds()==null?List.of():r.attachmentIds()),
                    auth.currentActor().id(),Timestamp.from(now));
            List<Config> previous=jdbc.query("SELECT * FROM amusement_business_day_configs WHERE project_id=? AND status='approved' AND effective_at<=? ORDER BY effective_at DESC LIMIT 1",this::map,r.projectId(),Timestamp.from(r.effectiveAt()));
            String snapshot=r.reason()+"\n"+encode(Map.of("projectId",r.projectId(),"previousVersions",previous,"proposedConfig",r));
            var decision=reviews.evaluate(new SupervisorReviewEvaluateRequest(r.idempotencyKey()+":review",snapshot,
                    r.attachmentIds()==null?List.of():r.attachmentIds(),ACTION,OBJECT,id,0L,snapshot,r.finalApproverId(),List.of()));
            if(!"pending_review".equals(decision.reviewStatus()) || !"top_authority".equals(decision.approvalLevel()))
                throw ApiException.conflict("AMUSEMENT_BUSINESS_DAY_APPROVAL_REQUIRED","营业日配置必须进入最高权限审批");
            jdbc.update("UPDATE amusement_business_day_configs SET review_id=? WHERE id=?",decision.reviewId(),id);
            audit.recordSuccess("amusement.business_day.submitted",MANAGE,OBJECT,id,null,r.idempotencyKey(),
                    Map.of("project_id",r.projectId(),"review_id",decision.reviewId(),"version",version));
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
        List<Config> found=jdbc.query("SELECT * FROM amusement_business_day_configs WHERE id=? FOR UPDATE",this::map,decision.objectId());
        if(found.isEmpty())return;
        Config row=found.getFirst();
        if(!"pending_review".equals(row.status()) || !Objects.equals(row.reviewId(),decision.reviewId()))return;
        lockComplex(row.projectId());
        Instant now=Instant.now(clock);
        String status="approved".equals(decision.reviewStatus()) && !now.isBefore(row.effectiveAt()) ? "expired" : decision.reviewStatus();
        if("approved".equals(status)) {
            requireAttachments(row.projectId(),row.attachmentIds());
            if(jdbc.queryForObject("SELECT count(*) FROM amusement_business_day_configs WHERE project_id=? AND status='approved' AND effective_at=?",Integer.class,row.projectId(),Timestamp.from(row.effectiveAt()))>0)
                throw ApiException.conflict("AMUSEMENT_BUSINESS_DAY_VERSION_CONFLICT","该综合体同一时点已有生效版本，请重新提交不同生效时间");
        }
        jdbc.update("UPDATE amusement_business_day_configs SET status=?,decided_by=?,decided_at=? WHERE id=?",
                status,auth.currentActor().id(),Timestamp.from(now),row.id());
        audit.recordSuccess("amusement.business_day.decided","governance.supervisor_review.approve",OBJECT,row.id(),null,null,
                Map.of("project_id",row.projectId(),"status",status,"review_id",decision.reviewId()));
    }

    /** 内部履约及作业使用真实对象归属，不能拿操作者当前项目替代。 */
    public Window requireWindow(String amusementProjectId, Instant at) {
        return requireComplexWindow(complexId(amusementProjectId),at);
    }

    public String complexId(String amusementProjectId) {
        List<String> found=jdbc.query("SELECT complex_id FROM ("+COMPLEXES_SQL+") mapping WHERE amusement_project_id=?",
                (rs,n)->rs.getString(1),requireText(amusementProjectId,"amusement_project_id"));
        if(found.isEmpty())throw ApiException.notFound("AMUSEMENT_PROJECT_NOT_FOUND","游乐项目不存在");
        if(found.getFirst()==null)throw missing("游乐项目须有唯一综合体归属；空间冲突或跨综合体时须补齐真实空间");
        return found.getFirst();
    }

    public Window requireComplexWindow(String complexId, Instant at) {
        Objects.requireNonNull(at,"occurred_at");
        List<Config> rows=jdbc.query("""
            SELECT * FROM amusement_business_day_configs WHERE project_id=? AND status='approved' AND effective_at<=?
              ORDER BY effective_at DESC LIMIT 1
            """,this::map,requireText(complexId,"complex_id"),Timestamp.from(at));
        if(rows.isEmpty())throw missing("综合体缺少已批准且生效的游乐营业日版本");
        Config config=rows.getFirst();
        Instant next=jdbc.query("SELECT min(effective_at) FROM amusement_business_day_configs WHERE project_id=? AND status='approved' AND effective_at>?",
                (rs,n)->instant(rs,1),complexId,Timestamp.from(config.effectiveAt())).getFirst();
        return window(config,at,next);
    }

    static Window window(Config config,Instant at,Instant next) {
        ZoneId zone=ZoneId.of(config.zoneId());
        LocalDate date=at.atZone(zone).toLocalDate();
        if(at.isBefore(date.atTime(config.cutoffHour(),0).atZone(zone).toInstant()))date=date.minusDays(1);
        Instant start=date.atTime(config.cutoffHour(),0).atZone(zone).toInstant();
        Instant end=date.plusDays(1).atTime(config.cutoffHour(),0).atZone(zone).toInstant();
        if(start.isBefore(config.effectiveAt()))start=config.effectiveAt();
        if(next!=null && next.isBefore(end))end=next;
        return new Window(config.projectId(),config.id(),date,start,end);
    }

    public void requireEvidenceScope(String project,String evidencePermission) {
        scope(project,"evidence.timeline.view".equals(evidencePermission)?VIEW:MANAGE);
        auth.requirePermission(evidencePermission);
        if(!auth.projectReach(auth.currentActor(),evidencePermission).covers(project))
            throw ApiException.forbidden("FORBIDDEN","附件权限未覆盖本综合体");
    }

    private void scope(String project,String permission) {
        auth.requirePermission(permission);
        requireCompanyContext();
        if(!auth.projectReach(auth.currentActor(),permission).covers(requireText(project,"project_id")))
            throw ApiException.forbidden("FORBIDDEN","无权访问该综合体游乐营业日配置");
        if(jdbc.queryForObject("SELECT count(*) FROM projects WHERE id=? AND status='active'",Integer.class,project)!=1)
            throw invalid("综合体不存在或未生效");
    }
    private void requireCompanyContext() {
        if(auth.currentActor().merchantId()!=null)throw ApiException.forbidden("FORBIDDEN","商户限定授权不能管理整个综合体的游乐营业日");
    }
    private void lockComplex(String project) {
        if(jdbc.queryForList("SELECT id FROM projects WHERE id=? FOR UPDATE",String.class,project).isEmpty())throw invalid("综合体不存在");
    }
    private void requireAttachments(String project,List<String> ids) {
        if(ids==null || ids.isEmpty())return;
        auth.requirePermission("evidence.timeline.view");
        if(!auth.projectReach(auth.currentActor(),"evidence.timeline.view").covers(project))
            throw ApiException.forbidden("FORBIDDEN","附件查看权限未覆盖本综合体");
        for(String id:ids) {
            var attachment=evidence.findAttachmentById(id).orElseThrow(()->invalid("配置依据附件不存在"));
            if(!OBJECT.equals(attachment.businessType()) || !project.equals(attachment.businessId()) || attachment.merchantId()!=null)
                throw invalid("配置依据必须绑定本综合体且不能引用商户私有附件");
            attachments.requireUploaded(attachment);
        }
    }
    private void validate(Request r) {
        requireText(r.projectId(),"project_id");requireText(r.reason(),"reason");
        try { if(!ZoneId.getAvailableZoneIds().contains(requireText(r.zoneId(),"zone_id")))throw new java.time.DateTimeException("unknown zone"); ZoneId.of(r.zoneId()); } catch(java.time.DateTimeException failure) {throw invalid("所在地时区无效，须填写有效时区名称");}
        if(r.cutoffHour()==null || r.minCutoffHour()==null || r.maxCutoffHour()==null
                || r.minCutoffHour()<0 || r.maxCutoffHour()>23 || r.cutoffHour()<r.minCutoffHour() || r.cutoffHour()>r.maxCutoffHour())
            throw invalid("日切小时及上下限须完整，满足0≤下限≤日切小时≤上限≤23");
    }
    Config find(String id) {
        return jdbc.query("SELECT * FROM amusement_business_day_configs WHERE id=?",this::map,id).stream().findFirst()
                .orElseThrow(()->ApiException.notFound("AMUSEMENT_BUSINESS_DAY_NOT_FOUND","营业日配置版本不存在"));
    }
    private Config map(ResultSet rs,int n)throws SQLException {
        try {return new Config(rs.getString("id"),rs.getString("project_id"),rs.getString("version"),rs.getString("zone_id"),rs.getInt("cutoff_hour"),
                rs.getInt("min_cutoff_hour"),rs.getInt("max_cutoff_hour"),rs.getTimestamp("effective_at").toInstant(),rs.getString("status"),
                rs.getString("review_id"),rs.getString("reason"),json.readValue(rs.getString("attachment_ids"),json.getTypeFactory().constructCollectionType(List.class,String.class)),
                rs.getString("created_by"),rs.getTimestamp("created_at").toInstant(),rs.getString("decided_by"),instant(rs,"decided_at"));}
        catch(java.io.IOException error){throw new IllegalStateException("营业日配置附件记录无法读取",error);}
    }
    private String encode(Object value){try{return json.writeValueAsString(value);}catch(java.io.IOException error){throw new IllegalStateException(error);}}
    private static Instant instant(ResultSet rs,String key)throws SQLException {Timestamp t=rs.getTimestamp(key);return t==null?null:t.toInstant();}
    private static Instant instant(ResultSet rs,int key)throws SQLException {Timestamp t=rs.getTimestamp(key);return t==null?null:t.toInstant();}
    private static ApiException invalid(String message){return ApiException.badRequest("VALIDATION_FAILED",message);}
    private static ApiException missing(String message){return ApiException.conflict("AMUSEMENT_BUSINESS_DAY_CONFIGURATION_REQUIRED",message);}
}
