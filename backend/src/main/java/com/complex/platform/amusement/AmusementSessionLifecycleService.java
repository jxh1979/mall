package com.complex.platform.amusement;

// 文件版本：V0.05

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.EvidenceAttachmentValidator;
import com.complex.platform.evidence.EvidenceEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.requireText;

@Service
public class AmusementSessionLifecycleService {
    public static final String END = "amusement.session.end";
    public static final String VIEW = "amusement.session.view";
    private final JdbcTemplate jdbc;
    private final AmusementRepository repository;
    private final AmusementBusinessDayService businessDays;
    private final AuthorizationService auth;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;
    private final EvidenceEventRepository evidence;
    private final EvidenceAttachmentValidator attachments;
    private final ObjectMapper json;
    private final AmusementFaultCompensationService compensation;

    public AmusementSessionLifecycleService(JdbcTemplate jdbc, AmusementRepository repository,
            AmusementBusinessDayService businessDays, AuthorizationService auth, IdempotencyService idempotency,
            AuditEventService audit, EvidenceEventRepository evidence, EvidenceAttachmentValidator attachments,
            ObjectMapper json, AmusementFaultCompensationService compensation) {
        this.jdbc=jdbc; this.repository=repository; this.businessDays=businessDays; this.auth=auth;
        this.idempotency=idempotency; this.audit=audit; this.evidence=evidence; this.attachments=attachments; this.json=json;
        this.compensation=compensation;
    }

    public record EndRequest(String idempotencyKey, Boolean clearanceConfirmed, Instant clearedAt,
            String reason, List<String> evidenceAttachmentIds) {}

    public record Ending(String id, String sessionId, String projectId, String complexId, String configVersionId,
            LocalDate businessDate, Instant businessStartAt, Instant businessEndAt, String mode, Instant plannedEndAt,
            Instant actualEndAt, String confirmedBy, Instant confirmedAt, String reason, List<String> evidenceAttachmentIds,
            int clearedPeople) {}

    public record Admission(Instant occurredAt, AmusementBusinessDayService.Window window) {}

    /** 核销事务持有项目锁后才确定成功入场时刻，场次与日切使用同一事实时间。 */
    public Admission prepareEntry(String projectId, String sessionId, String permission) {
        repository.lockProjectForSafety(projectId);
        requireScope(sessionId,permission);
        Instant now=Instant.now();
        var project=repository.findProjectById(projectId).orElseThrow(()->missing("游乐项目不存在"));
        if(!"open".equals(project.openStatus()))throw ApiException.conflict("PROJECT_NOT_OPEN","该游乐项目未开放");
        AmusementSessionRecord session=lockSession(sessionId);
        if(!projectId.equals(session.projectId()) || !"open".equals(session.status()))
            throw ApiException.conflict("SESSION_NOT_OPEN","场次不属于该项目或已经结束");
        if(now.isBefore(session.startTime()) || !now.isBefore(session.endTime()))
            throw ApiException.conflict("SESSION_NOT_OPEN","当前时间不在开放场次内");
        requireSessionWindow(session.id());
        var window=businessDays.requireWindow(projectId,now);
        releaseExpiredEntries(projectId,now);
        return new Admission(now,window);
    }

    /** 仅由新建场次事务调用；核销、散场及任务均不得推算补写已有记录。 */
    public AmusementBusinessDayService.Window lockSessionWindow(AmusementSessionRecord session) {
        var window=businessDays.requireWindow(session.projectId(),session.startTime());
        jdbc.update("""
            UPDATE amusement_sessions SET complex_id=?,business_day_version_id=?,business_date=?,
                business_start_at=?,business_end_at=? WHERE id=? AND business_day_version_id IS NULL
            """,window.complexId(),window.versionId(),java.sql.Date.valueOf(window.businessDate()),
                timestamp(window.startAt()),timestamp(window.endAt()),session.id());
        return requireSessionWindow(session.id());
    }

    private AmusementBusinessDayService.Window requireSessionWindow(String sessionId) {
        return jdbc.query("""
            SELECT complex_id,business_day_version_id,business_date,business_start_at,business_end_at
              FROM amusement_sessions WHERE id=? AND complex_id IS NOT NULL
               AND business_day_version_id IS NOT NULL AND business_date IS NOT NULL
               AND business_start_at IS NOT NULL AND business_end_at IS NOT NULL
            """,(rs,n)->new AmusementBusinessDayService.Window(rs.getString(1),rs.getString(2),
                rs.getDate(3).toLocalDate(),rs.getTimestamp(4).toInstant(),rs.getTimestamp(5).toInstant()),sessionId)
            .stream().findFirst().orElseThrow(()->ApiException.conflict(
                "AMUSEMENT_SESSION_DAY_PENDING","该场次缺少创建时锁定的营业日事实，待核实后处理"));
    }

    public void requireEntryFacts(String entryId) {
        if(jdbc.queryForObject("""
            SELECT count(*) FROM amusement_verifications WHERE id=? AND direction='entry'
              AND complex_id IS NOT NULL AND business_day_version_id IS NOT NULL AND business_date IS NOT NULL
              AND business_start_at IS NOT NULL AND business_end_at IS NOT NULL
            """,Integer.class,entryId)!=1)throw ApiException.conflict(
                "AMUSEMENT_ENTRY_DAY_PENDING","该入场缺少核销时锁定的营业日事实，不能推定释放人数");
    }

    public void recordEntryWindow(String verificationId, AmusementBusinessDayService.Window window) {
        jdbc.update("""
            UPDATE amusement_verifications SET complex_id=?,business_day_version_id=?,business_date=?,
                business_start_at=?,business_end_at=? WHERE id=? AND direction='entry'
            """,window.complexId(),window.versionId(),java.sql.Date.valueOf(window.businessDate()),
                timestamp(window.startAt()),timestamp(window.endAt()),verificationId);
    }

    @com.complex.platform.gate.GuardedAction(capability = END)
    @Transactional
    public Ending endEarly(String sessionId, EndRequest request) {
        String id=requireText(sessionId,"session_id");
        requireScope(id,END);
        if(request==null)throw invalid("请提交实际清场确认");
        if(!Boolean.TRUE.equals(request.clearanceConfirmed()))throw invalid("仍有客人在场或尚未确认清场，不能提前结束");
        requireText(request.reason(),"reason");
        requireEvidence(id,request.evidenceAttachmentIds(),false);
        return idempotency.execute("endAmusementSession",requireText(request.idempotencyKey(),"idempotency_key"),
            Map.of("sessionId",id,"request",request),()->{
                String projectId=requireScope(id,END);
                repository.lockProjectForSafety(projectId);
                AmusementSessionRecord session=lockSession(id);
                requireScope(id,END);
                Instant now=Instant.now(), at=request.clearedAt();
                if(at==null || at.isAfter(now) || at.isBefore(session.startTime()) || !at.isBefore(session.endTime()))
                    throw invalid("实际清场时刻须在场次开始后、计划结束前，且不能在未来");
                if(!"open".equals(session.status()))throw ApiException.conflict("SESSION_NOT_OPEN","只有开放中的场次可提前结束");
                if(jdbc.queryForObject("""
                    SELECT count(*) FROM amusement_verifications WHERE session_id=? AND direction='entry'
                        AND verification_result='success' AND created_at>?
                    """,Integer.class,id,timestamp(at))>0)throw ApiException.conflict(
                            "SESSION_CLEARANCE_CONFLICT","所填清场时刻后仍有成功入场记录，请核实实际清场事实");
                requireEvidence(id,request.evidenceAttachmentIds(),false);
                Ending ending=finish(session,"early",at,now,request.reason(),request.evidenceAttachmentIds(),
                        request.idempotencyKey(),auth.currentActor().id());
                audit.recordSuccess("amusement.session.end_completed",END,"amusement_session",id,
                        null,request.idempotencyKey(),Map.of("ending_id",ending.id(),"project_id",projectId));
                return ending;
            },Ending.class);
    }

    @Transactional
    public int closeEndedSessions(Instant now) {
        int changed=0;
        for(AmusementSessionRecord candidate:repository.listEndedUnclosedSessions(now)) {
            // Missing real ownership or an approved historical day is a backlog, never a guessed boundary.
            try { requireSessionWindow(candidate.id()); }
            catch(ApiException missingConfig) {
                audit.recordFailure("amusement.session.end_pending",null,"amusement_session",candidate.id(),
                        null,null,missingConfig.code()); continue;
            }
            repository.lockProjectForSafety(candidate.projectId());
            AmusementSessionRecord session=lockSession(candidate.id());
            if(!List.of("open","scheduled").contains(session.status()) || session.endTime().isAfter(now))continue;
            try {
                finish(session,"natural",session.endTime(),now,"计划场次结束",List.of(),"natural:"+session.id(),null);
                changed++;
            } catch(ApiException missingFacts) {
                if(!"AMUSEMENT_ENTRY_DAY_PENDING".equals(missingFacts.code()))throw missingFacts;
                audit.recordFailure("amusement.session.end_pending",null,"amusement_session",session.id(),
                    null,null,missingFacts.code());
            }
        }
        return changed;
    }

    @Transactional
    public int resetExpiredBusinessDays(Instant now) {
        int changed=0;
        for(AmusementProjectRecord project:repository.listProjects()) {
            repository.lockProjectForSafety(project.id());
            changed+=releaseExpiredEntries(project.id(),now);
        }
        return changed;
    }

    private int releaseExpiredEntries(String projectId, Instant now) {
        for(String entry:jdbc.queryForList("""
            SELECT id FROM amusement_verifications WHERE project_id=? AND direction='entry'
              AND verification_result='success' AND released_at IS NULL
              AND (complex_id IS NULL OR business_day_version_id IS NULL OR business_date IS NULL
                OR business_start_at IS NULL OR business_end_at IS NULL)
            """,String.class,projectId)) {
            audit.recordFailure("amusement.concurrent_count.day_pending",null,"amusement_verification",
                entry,null,null,"AMUSEMENT_ENTRY_DAY_PENDING");
        }
        List<Map<String,Object>> expired=jdbc.queryForList("""
            SELECT complex_id,business_day_version_id,business_date,business_start_at,business_end_at
              FROM amusement_verifications WHERE project_id=? AND direction='entry'
               AND verification_result='success' AND released_at IS NULL AND business_end_at<=?
               AND complex_id IS NOT NULL AND business_day_version_id IS NOT NULL
               AND business_date IS NOT NULL AND business_start_at IS NOT NULL
             GROUP BY complex_id,business_day_version_id,business_date,business_start_at,business_end_at
             ORDER BY business_end_at
            """,projectId,timestamp(now));
        int changed=0;
        for(var day:expired) {
            String logId="acrl_"+UUID.randomUUID().toString().replace("-","");
            List<Integer> people=jdbc.query("""
                UPDATE amusement_verifications SET released_at=?,release_source='business_day_end',release_source_id=?
                 WHERE project_id=? AND direction='entry' AND verification_result='success' AND released_at IS NULL
                   AND business_day_version_id=? AND business_end_at=?
                 RETURNING verified_people
                """,(rs,n)->rs.getInt(1),timestamp(now),logId,projectId,day.get("business_day_version_id"),day.get("business_end_at"));
            int cleared=people.stream().mapToInt(Integer::intValue).sum();
            if(cleared==0)continue;
            decrementProject(projectId,cleared,now);
            jdbc.update("""
                INSERT INTO amusement_concurrent_reset_log(id,project_id,scope,business_date,cleared_rows,
                    cleared_people,boundary_at,reset_at,source_id,complex_id,config_version_id)
                VALUES(?,?,'business_day_end',?,1,?,?,?,?,?,?)
                """,logId,projectId,day.get("business_date"),cleared,day.get("business_end_at"),
                    timestamp(now),logId,day.get("complex_id"),day.get("business_day_version_id"));
            audit.recordSuccess("amusement.concurrent_count.business_day_reset",null,"amusement_project",
                    projectId,null,null,Map.of("cleared_people",cleared,"source_id",logId));
            changed++;
        }
        return changed;
    }

    private Ending finish(AmusementSessionRecord session,String mode,Instant at,Instant now,String reason,
            List<String> evidenceIds,String key,String actor) {
        var window=requireSessionWindow(session.id());
        List<Map<String,Object>> entries=jdbc.queryForList("""
            SELECT id,entitlement_id,verified_people FROM amusement_verifications
             WHERE session_id=? AND direction='entry' AND verification_result='success' AND released_at IS NULL
             ORDER BY id FOR UPDATE
            """,session.id());
        for(var entry:entries)requireEntryFacts((String)entry.get("id"));
        int people=entries.stream().mapToInt(e->((Number)e.get("verified_people")).intValue()).sum();
        String id="ase_"+UUID.randomUUID().toString().replace("-","");
        jdbc.update("""
            INSERT INTO amusement_session_endings(id,session_id,project_id,complex_id,config_version_id,business_date,
                business_start_at,business_end_at,mode,planned_end_at,actual_end_at,confirmed_by,confirmed_at,reason,
                evidence_attachment_ids,idempotency_key,cleared_people)
            VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?::jsonb,?,?)
            """,id,session.id(),session.projectId(),window.complexId(),window.versionId(),
                java.sql.Date.valueOf(window.businessDate()),timestamp(window.startAt()),timestamp(window.endAt()),
                mode,timestamp(session.endTime()),timestamp(at),actor,timestamp(now),reason,encode(evidenceIds),key,people);
        if("early".equals(mode))createFollowups(id,session,now,at,reason,window.complexId());
        jdbc.update("""
            UPDATE amusement_verifications SET released_at=?,release_source='session_end',release_source_id=?
             WHERE session_id=? AND direction='entry' AND verification_result='success' AND released_at IS NULL
            """,timestamp(at),id,session.id());
        decrementProject(session.projectId(),people,now);
        jdbc.update("UPDATE amusement_sessions SET status='closed',updated_at=? WHERE id=?",timestamp(now),session.id());
        jdbc.update("""
            INSERT INTO amusement_concurrent_reset_log(id,project_id,scope,business_date,session_id,cleared_rows,
                cleared_people,boundary_at,reset_at,source_id,complex_id,config_version_id)
            VALUES(?,?,'session_end',?,?,1,?,?,?,?,?,?)
            ""","acrl_"+UUID.randomUUID().toString().replace("-",""),session.projectId(),
                java.sql.Date.valueOf(window.businessDate()),session.id(),people,timestamp(at),timestamp(now),id,
                window.complexId(),window.versionId());
        audit.recordSuccess("amusement.concurrent_count.session_end_cleared",END,"amusement_session",session.id(),
                null,key,Map.of("source_id",id,"mode",mode,"cleared_people",people));
        return findEnding(id);
    }

    private void createFollowups(String ending,AmusementSessionRecord session,Instant now,
            Instant at,String reason,String complexId) {
        // 18AH 复核修复：V619 起同一权益同场次可有多条未释放入场——入场待办按权益只建一条，取最新那条入场，与补偿取数
        // （JdbcAmusementFaultCompensationRepository.listSessionEndAffectedShares 的 DISTINCT ON (e.id) … ORDER BY av.created_at DESC）
        // 同一条，发放时 completeSessionEndFollowups 才能写回；无权益的入场照旧每条一条。
        for(var entry:jdbc.queryForList("""
            SELECT DISTINCT ON (COALESCE(entitlement_id,id)) id,entitlement_id FROM amusement_verifications
             WHERE session_id=? AND direction='entry' AND verification_result='success' AND released_at IS NULL
             ORDER BY COALESCE(entitlement_id,id),created_at DESC
            """,session.id())) {
            String entitlement=(String)entry.get("entitlement_id");
            List<String> orders=entitlement==null?List.of():jdbc.queryForList(
                    "SELECT order_id FROM issued_entitlements WHERE id=?",String.class,entitlement);
            followup(ending,"entry",(String)entry.get("id"),entitlement,orders.isEmpty()?null:orders.getFirst(),now);
        }
        // TK-18（2026-10-18 操作员裁决补充第 3 条）：已入场（已扣次）的客人比照 ENUM L148 全园故障补偿规则——复用 CG-08b 补偿记录生成
        // 「场次提前结束」补偿待发放批次（关联场次与结束原件，影响范围＝实际清场时刻至原计划结束，费用游乐公司承担），发放前须
        // 游乐公司负责人与工程或安全岗位共同确认；入场待办在发放时写回。未入场的票/预约不建补偿待办，预约与团体预约按既有取消规则处理。
        compensation.createSessionEndBatch(ending,session.id(),complexId,"场次提前结束："+reason,at,session.endTime(),now);
        for(String reservation:jdbc.queryForList(
                "SELECT id FROM amusement_reservations WHERE session_id=? AND reservation_status='reserved'",
                String.class,session.id()))followup(ending,"reservation",reservation,null,null,now);
        for(String booking:jdbc.queryForList(
                "SELECT id FROM amusement_group_bookings WHERE session_id=? AND booking_status IN ('pending_approval','approved')",
                String.class,session.id()))followup(ending,"group_booking",booking,null,null,now);
    }

    private void followup(String ending,String type,String source,String entitlement,String order,Instant now) {
        jdbc.update("""
            INSERT INTO amusement_session_followups(id,ending_id,source_type,source_id,entitlement_id,order_id,created_at)
            VALUES(?,?,?,?,?,?,?)
            ""","asf_"+UUID.randomUUID().toString().replace("-",""),ending,type,source,entitlement,order,timestamp(now));
    }

    /** 既有取消／释放规则的结果写回提前结束待办（BSM L1263「按既有取消、补偿规则处理并留痕」）；命中 0 行即该来源无待办，静默。 */
    void resolveFollowup(String sourceType,String sourceId,String resolutionType,String resolutionId) {
        jdbc.update("""
            UPDATE amusement_session_followups SET status='completed',resolution_type=?,resolution_id=?,resolved_by=?,resolved_at=?
             WHERE source_type=? AND source_id=? AND status='pending'
            """,resolutionType,resolutionId,auth.currentActor().id(),timestamp(Instant.now()),sourceType,sourceId);
    }

    public List<Ending> endings(String sessionId) {
        return auditedRead(VIEW,"amusement_session",sessionId,()->{
            requireScope(sessionId,VIEW,true);
            return jdbc.query("SELECT id FROM amusement_session_endings WHERE session_id=? ORDER BY confirmed_at",
                    (rs,n)->findEnding(rs.getString(1)),sessionId);
        });
    }

    public List<Map<String,Object>> followups(String sessionId) {
        return auditedRead(VIEW,"amusement_session",sessionId,()->{
            requireScope(sessionId,VIEW,true);
            return jdbc.queryForList("""
                SELECT f.* FROM amusement_session_followups f JOIN amusement_session_endings e ON e.id=f.ending_id
                 WHERE e.session_id=? ORDER BY f.created_at,f.id
                """,sessionId);
        });
    }

    public List<Map<String,Object>> releases(String projectId) {
        String permission="amusement.verification.view";
        return auditedRead(permission,"amusement_project",projectId==null?"all":projectId,()->{
            auth.requirePermission(permission);
            if(projectId!=null)requireReadScope(projectId,permission);
            // 第十八批 E 段 20-R4（治理地图第 659 行「一个商户在两个或更多项目中有经营的，看到的是每一个项目内他经营的商户」）：商户账号只按商户，其余按可触及综合体
            String merchant=auth.currentMerchantIdQuietly().orElse(null);
            var reach=merchant==null?auth.projectReach(auth.currentActor(),permission):null;
            return jdbc.queryForList("""
                SELECT l.*,p.merchant_id FROM amusement_concurrent_reset_log l
                  JOIN amusement_projects p ON p.id=l.project_id
                 WHERE l.complex_id IS NOT NULL AND (?::text IS NULL OR l.project_id=?)
                 ORDER BY l.reset_at DESC,l.id
                """,projectId,projectId).stream()
                    .filter(row->merchant==null?reach.covers((String)row.get("complex_id")):merchant.equals(row.get("merchant_id")))
                    .toList();
        });
    }

    private <T> List<T> auditedRead(String permission,String businessType,String businessId,
            java.util.function.Supplier<List<T>> action) {
        try {
            List<T> result=action.get();
            audit.recordSuccess("amusement.session.viewed",permission,businessType,businessId,
                    null,null,Map.of("count",result.size()));
            return result;
        } catch(RuntimeException failure) {
            audit.recordFailure("amusement.session.view_failed",permission,businessType,businessId,
                    null,null,failure instanceof ApiException api?api.code():"INTERNAL_ERROR");
            throw failure;
        }
    }

    public void requireEvidence(String sessionId,List<String> ids,boolean reading) {
        requireScope(sessionId,reading?VIEW:END,reading);
        for(String id:ids==null?List.<String>of():ids) {
            var attachment=evidence.findAttachmentById(id).orElseThrow(()->invalid("清场凭据不存在"));
            if(!"amusement_session".equals(attachment.businessType()) || !sessionId.equals(attachment.businessId()))
                throw invalid("清场凭据须绑定本场次");
            attachments.requireUploaded(attachment);
        }
    }

    public String requireEvidenceScope(String sessionId,String evidencePermission) {
        boolean reading="evidence.timeline.view".equals(evidencePermission);
        requireScope(sessionId,reading?VIEW:END,reading);
        String projectId=requireScope(sessionId,evidencePermission,reading);
        return repository.findProjectById(projectId).orElseThrow(()->missing("项目不存在")).merchantId();
    }

    public void requireVerificationScope(String sessionId,String permission) {
        requireScope(sessionId,permission);
    }

    private String requireScope(String sessionId,String permission) {
        return requireScope(sessionId,permission,false);
    }

    private String requireScope(String sessionId,String permission,boolean reading) {
        auth.requirePermission(permission);
        AmusementSessionRecord session=repository.findSessionById(requireText(sessionId,"session_id"))
                .orElseThrow(()->missing("场次不存在"));
        if(reading)requireReadScope(session.projectId(),permission);
        else requireProjectScope(session.projectId(),permission);
        return session.projectId();
    }

    /** 第十八批 E 段 20-R4（治理地图第 659 行「一个商户在两个或更多项目中有经营的，看到的是每一个项目内他经营的商户」）：只用于读——
     *  商户账号（非平台管理员）只核游乐项目属本商户，不再限本人项目；其余同 {@link #requireProjectScope}。写入口仍走 requireProjectScope。 */
    private void requireReadScope(String projectId,String permission) {
        String merchant=auth.currentMerchantIdQuietly().orElse(null);
        if(merchant==null){requireProjectScope(projectId,permission);return;}
        auth.requirePermission(permission);
        if(!merchant.equals(repository.findProjectById(projectId).map(AmusementProjectRecord::merchantId).orElse(null)))
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN","无权处理其他商户场次");
    }

    public void requireProjectScope(String projectId,String permission) {
        auth.requirePermission(permission);
        String complex=businessDays.complexId(projectId);
        if(!auth.projectReach(auth.currentActor(),permission).covers(complex))
            throw ApiException.forbidden("FORBIDDEN","无权处理该综合体场次");
        String merchant=auth.currentActor().merchantId();
        if(merchant!=null && !merchant.isBlank() && !Objects.equals(merchant,
                repository.findProjectById(projectId).map(AmusementProjectRecord::merchantId).orElse(null)))
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN","无权处理其他商户场次");
    }

    private AmusementSessionRecord lockSession(String id) {
        jdbc.queryForList("SELECT id FROM amusement_sessions WHERE id=? FOR NO KEY UPDATE",id);
        return repository.findSessionById(id).orElseThrow(()->missing("场次不存在"));
    }

    private void decrementProject(String project,int people,Instant now) {
        jdbc.update("""
            UPDATE amusement_capacity_snapshot SET occupied_count=GREATEST(occupied_count-?,0),updated_at=?
             WHERE project_id=? AND dimension='project' AND session_id IS NULL
               AND location_point_id IS NULL AND device_id IS NULL
            """,people,timestamp(now),project);
    }

    private Ending findEnding(String id) {
        return jdbc.query("SELECT * FROM amusement_session_endings WHERE id=?",(rs,n)->{
            List<String> files;
            try { files=json.readValue(rs.getString("evidence_attachment_ids"),new com.fasterxml.jackson.core.type.TypeReference<List<String>>(){}); }
            catch(Exception failure) { throw new IllegalStateException("清场凭据记录无法读取",failure); }
            return new Ending(rs.getString("id"),rs.getString("session_id"),rs.getString("project_id"),
                rs.getString("complex_id"),rs.getString("config_version_id"),rs.getDate("business_date").toLocalDate(),
                rs.getTimestamp("business_start_at").toInstant(),rs.getTimestamp("business_end_at").toInstant(),
                rs.getString("mode"),rs.getTimestamp("planned_end_at").toInstant(),rs.getTimestamp("actual_end_at").toInstant(),
                rs.getString("confirmed_by"),rs.getTimestamp("confirmed_at").toInstant(),rs.getString("reason"),files,
                rs.getInt("cleared_people"));
        },id).stream().findFirst().orElseThrow(()->missing("清场原件不存在"));
    }
    private String encode(List<String> ids) {
        try{return json.writeValueAsString(ids==null?List.of():ids);}
        catch(Exception failure){throw new IllegalStateException(failure);}
    }
    private static Timestamp timestamp(Instant at){return Timestamp.from(at);}
    private static ApiException invalid(String message){return ApiException.badRequest("VALIDATION_FAILED",message);}
    private static ApiException missing(String message){return ApiException.notFound("AMUSEMENT_SESSION_NOT_FOUND",message);}
}

