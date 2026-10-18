package com.complex.platform.bar;

// 文件版本：V0.01

import static com.complex.platform.bar.BarPayrollSupport.*;
import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.bar.BarPayrollRequests.*;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BarServiceResponsibilityService {
    private final BarPayrollSupport p;
    private final BarRepository bars;
    private final IdempotencyService idem;
    private final AuditEventService audit;
    public BarServiceResponsibilityService(BarPayrollSupport p,BarRepository bars,IdempotencyService idem,AuditEventService audit) {
        this.p=p;this.bars=bars;this.idem=idem;this.audit=audit;
    }
    void recordOpened(BarTableSessionRecord session) {
        if(session.serviceStaffId()==null)return;
        open(session,session.serviceStaffId(),session.openedAt(),"open:"+session.id(),"开台确认责任人");
    }
    /** 订单在创建事务锁定当时场次；历史订单不反查当前开台补造历史。 */
    void recordOrder(BarOrderRecord order) {
        var sessions=p.jdbc.queryForList("SELECT id FROM bar_table_sessions WHERE table_id=? AND bar_id=? AND status='open' ORDER BY opened_at,id FOR UPDATE",
            String.class,order.tableId(),order.barId());
        if(sessions.size()>1)throw conflict("BAR_SESSION_AMBIGUOUS","该台位存在多个开台场次，须先核实再点单");
        if(sessions.isEmpty())return;
        var session=session(sessions.getFirst(),false);var scope=p.tableScope(order.tableId());
        if(!Objects.equals(scope.merchant(),session.merchantId()) || (order.merchantId()!=null && !order.merchantId().equals(scope.merchant())))
            throw conflict("BAR_PAYROLL_SOURCE_SCOPE","订单与场次的商户不一致");
        p.jdbc.update("INSERT INTO bar_order_service_sources(order_id,session_id,table_id,project_id,merchant_id,business_date) VALUES(?,?,?,?,?,?)",
            order.id(),session.id(),order.tableId(),scope.project(),scope.merchant(),session.businessDate());
    }
    public List<Responsibility> history(String sessionId) {
        var s=session(sessionId,false);var scope=p.tableScope(s.tableId());p.requireScope(scope.project(),scope.merchant(),VIEW);
        return responsibilityRows(sessionId);
    }
    @Transactional
    @com.complex.platform.gate.GuardedAction(capability="bar.table_session.transfer")
    public List<Responsibility> handover(String sessionId,Handover r) {
        var s=session(sessionId,false);var scope=p.tableScope(s.tableId());p.requireScope(scope.project(),scope.merchant(),"bar.table_session.transfer");
        if(r==null || r.sessionVersion()==null)throw invalid("须填写当前场次版本和接手人");
        requireText(r.reason(),"reason");
        return Arrays.asList(idem.execute("barServiceHandover:"+sessionId,requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
            var current=session(sessionId,true);
            if(!"open".equals(current.status()) || current.sessionVersion()!=r.sessionVersion())throw conflict("BAR_TABLE_SESSION_VERSION_CONFLICT","场次已变化，请刷新");
            if(Objects.equals(current.serviceStaffId(),r.nextUserId()))throw invalid("接手人不能仍为当前责任人");
            Instant now=Instant.now();
            recordStaffHandover(sessionId,current.serviceStaffId(),r.nextUserId(),now,"handover:"+r.idempotencyKey(),r.reason());
            p.jdbc.update("UPDATE bar_table_sessions SET service_staff_id=?,session_version=session_version+1,updated_at=? WHERE id=?",
                r.nextUserId(),ts(now),sessionId);
            audit.recordSuccess("bar.service.handover","bar.table_session.transfer","bar_table_session",sessionId,null,r.idempotencyKey(),Map.of("next_user_id",r.nextUserId()));
            return responsibilityRows(sessionId).toArray(Responsibility[]::new);
        },Responsibility[].class));
    }
    /** HR 原交接事务调用；不回填旧责任人的未知起点。 */
    public void recordStaffHandover(String sessionId,String previousUser,String nextUser,Instant at,String sourceId,String reason) {
        var current=session(sessionId,true);
        for(String id:family(sessionId)) {
            session(id,true);
            var open=responsibilityRows(id).stream().filter(x->x.endedAt()==null).toList();
            if(!open.isEmpty() && !Objects.equals(open.getFirst().userId(),previousUser))
                throw conflict("BAR_SERVICE_HANDOVER_CONFLICT","责任区间与待交接人不一致，须先核实");
            end(id,at);
            openFor(id,current.tableId(),requireText(nextUser,"next_user_id"),at,eventKey(sourceId+":"+id),requireText(reason,"reason"));
            if(!id.equals(sessionId))p.jdbc.update("UPDATE bar_table_sessions SET service_staff_id=?,session_version=session_version+1,updated_at=? WHERE id=?",nextUser,ts(at),id);
        }
    }
    /** 正式排班表仅保存已发布班次；待审修订不在本表，真实顶班同样发布为班次。 */
    void requireTransferStaff(String table,String user,Instant at) {
        var scope=p.tableScope(table);
        p.requireWorker(requireText(user,"target_service_staff_id"),scope.project(),scope.merchant());
        boolean scheduled=Boolean.TRUE.equals(p.jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM staff_shift_schedules s JOIN bar_tables t ON t.id=?
                WHERE s.user_id=? AND s.start_at<=? AND s.end_at>?
                  AND s.point_space_ids @> jsonb_build_array(t.space_id)
                  AND NOT EXISTS(SELECT 1 FROM staff_shift_covers c WHERE c.shift_id=s.id AND c.start_at<=? AND c.end_at>?))
            """,Boolean.class,table,user,ts(at),ts(at),ts(at),ts(at)));
        if(!scheduled)throw conflict("BAR_TARGET_SERVICE_STAFF_NOT_ON_DUTY","目标接手人须有该点位当前已发布排班或真实顶班记录，不能继承原台人员");
    }
    void recordTransfer(BarTableSessionRecord before,BarTableSessionRecord after,Instant at,String sourceId) {
        String target=requireText(after.serviceStaffId(),"target_service_staff_id");
        for(String id:family(before.id())) {
            session(id,true);
            end(id,at);
            openFor(id,after.tableId(),target,at,eventKey(sourceId+":"+id),"转台后由目标台位当班人员接手；原台区间保留");
            if(!id.equals(after.id()))p.jdbc.update("UPDATE bar_table_sessions SET service_staff_id=?,session_version=session_version+1,updated_at=? WHERE id=?",target,ts(at),id);
        }
    }
    void recordMerge(BarTableSessionRecord primary,BarTableSessionRecord merged,Instant at,String sourceId) {
        for(String id:family(merged.id())) {
            session(id,true);end(id,at);
            if(primary.serviceStaffId()!=null)openFor(id,primary.tableId(),primary.serviceStaffId(),at,eventKey(sourceId+":"+id),"并台后由目标台位实际责任人接手");
            p.jdbc.update("UPDATE bar_table_sessions SET service_staff_id=?,updated_at=? WHERE id=?",primary.serviceStaffId(),ts(at),id);
        }
    }
    private List<String> family(String sessionId) {
        return p.jdbc.queryForList("""
            WITH RECURSIVE family AS (SELECT id FROM bar_table_sessions WHERE id=?
            UNION ALL SELECT s.id FROM bar_table_sessions s JOIN family f ON s.source_session_id=f.id WHERE s.status='merged')
            SELECT id FROM family ORDER BY id
            """,String.class,sessionId);
    }
    @Transactional
    @com.complex.platform.gate.GuardedAction(capability="bar.table_session.transfer")
    public List<Responsibility> close(String sessionId,CloseSession r) {
        var s=session(sessionId,false);var scope=p.tableScope(s.tableId());p.requireScope(scope.project(),scope.merchant(),"bar.table_session.transfer");
        if(r==null || r.sessionVersion()==null)throw invalid("须填写版本及实际服务结束原因");
        requireText(r.reason(),"reason");
        return Arrays.asList(idem.execute("barServiceClose:"+sessionId,requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
            var current=session(sessionId,true);
            if(!"open".equals(current.status()) || current.sessionVersion()!=r.sessionVersion())throw conflict("BAR_TABLE_SESSION_VERSION_CONFLICT","场次已变化，请刷新");
            var ids=family(sessionId);
            Instant now=Instant.now();
            for(String id:ids) {
                var child=session(id,true);end(id,now);
                p.jdbc.update("UPDATE bar_table_sessions SET status=CASE WHEN id=? THEN 'closed' ELSE status END,closed_at=?,updated_at=?,session_version=session_version+1 WHERE id=?",
                    sessionId,ts(now),ts(now),id);
                if(id.equals(sessionId))p.jdbc.update("UPDATE bar_tables SET status='pending_clear',updated_at=? WHERE id=?",ts(now),child.tableId());
            }
            audit.recordSuccess("bar.service.closed","bar.table_session.transfer","bar_table_session",sessionId,null,r.idempotencyKey(),Map.of("reason",r.reason()));
            return responsibilityRows(sessionId).toArray(Responsibility[]::new);
        },Responsibility[].class));
    }
    BarTableSessionRecord session(String id,boolean lock) {
        if(lock)p.jdbc.queryForList("SELECT id FROM bar_table_sessions WHERE id=? FOR UPDATE",String.class,id);
        return bars.findTableSessionById(id).orElseThrow(()->invalid("场次不存在"));
    }
    List<Responsibility> responsibilityRows(String session) {
        return p.jdbc.query("SELECT * FROM bar_service_responsibilities WHERE session_id=? ORDER BY started_at,id",(rs,n)->new Responsibility(
            rs.getString("id"),rs.getString("session_id"),rs.getString("table_id"),rs.getString("user_id"),rs.getString("payer_company_id"),
            rs.getTimestamp("started_at").toInstant(),rs.getTimestamp("ended_at")==null?null:rs.getTimestamp("ended_at").toInstant(),
            rs.getString("source_id"),rs.getString("reason")),session);
    }
    private void end(String session,Instant at) {
        if(p.jdbc.queryForObject("SELECT count(*) FROM bar_service_responsibilities WHERE session_id=? AND ended_at IS NULL AND started_at>=?",Integer.class,session,ts(at))>0)
            throw conflict("BAR_SERVICE_TIME_INVALID","交接时间须晚于现责任区间开始时间");
        p.jdbc.update("UPDATE bar_service_responsibilities SET ended_at=? WHERE session_id=? AND ended_at IS NULL",ts(at),session);
    }
    private void open(BarTableSessionRecord session,String user,Instant at,String source,String reason) {
        openFor(session.id(),session.tableId(),user,at,source,reason);
    }
    private void openFor(String sessionId,String table,String user,Instant at,String source,String reason) {
        var scope=p.tableScope(table);
        String payer=p.requireWorker(user,scope.project(),scope.merchant());
        p.jdbc.update("INSERT INTO bar_service_responsibilities(id,session_id,table_id,project_id,merchant_id,user_id,payer_company_id,started_at,source_id,recorded_by,reason) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
            id("bsr"),sessionId,table,scope.project(),scope.merchant(),user,payer,ts(at),source,p.auth.currentActor().id(),reason);
    }
}
