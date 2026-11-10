package com.complex.platform.bar;

// 文件版本：V0.07

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
    private final BarDepositSettlementService deposits;
    public BarServiceResponsibilityService(BarPayrollSupport p,BarRepository bars,IdempotencyService idem,AuditEventService audit,
            BarDepositSettlementService deposits) {
        this.p=p;this.bars=bars;this.idem=idem;this.audit=audit;this.deposits=deposits;
    }
    // 第十八批 T 段第 157 条 157-R3：坏账放行（字段注入免改构造点；单测未注入即不放行）。
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    BarOrderSuspensionService suspensions;
    // 157-R5：包厢关台未付清待办（字段注入免改构造点；单测未注入即只拦不发）。
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    BarUnpaidCloseTodoService unpaidCloseTodos;
    void recordOpened(BarTableSessionRecord session) {
        if(session.serviceStaffId()==null)return;
        open(session,session.serviceStaffId(),session.openedAt(),"open:"+session.id(),"开台确认责任人");
    }
    /** 订单在创建事务锁定当时场次；历史订单不反查当前开台补造历史。
     *  复核 money B3：并台期间被并台位的会话为 merged（当前有效并台链内），订单归属该台位的并台会话——随台组计进度、关台检查其账单，
     *  拆台时按实际订单归属回到原台（BSM L1079）。
     *  第十八批 I 段 86-R1：下单前先取当时场次（订单销售取其销售归属），保存订单后再记来源。 */
    Optional<BarTableSessionRecord> currentSession(String tableId,String barId) {
        var sessions=p.jdbc.queryForList("SELECT id FROM bar_table_sessions WHERE table_id=? AND bar_id=? AND status IN ('open','merged') ORDER BY opened_at,id FOR UPDATE",
            String.class,tableId,barId);
        if(sessions.size()>1)throw conflict("BAR_SESSION_AMBIGUOUS","该台位存在多个开台场次，须先核实再点单");
        return sessions.isEmpty()?Optional.empty():Optional.of(session(sessions.getFirst(),false));
    }
    void recordOrder(BarOrderRecord order,BarTableSessionRecord session) {
        var scope=p.tableScope(order.tableId());
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
    /** 正式排班表仅保存已发布班次；待审修订不在本表，真实顶班同样发布为班次。
     *  第十八批 I 段 86-R3：该台位点位此刻当班（已发布排班且未被顶班覆盖、或顶班班次本身）的人员，开台指派与转台接手共用。 */
    List<String> scheduledServiceStaff(String table,Instant at) {
        return p.jdbc.queryForList("""
            SELECT DISTINCT s.user_id FROM staff_shift_schedules s JOIN bar_tables t ON t.id=?
                WHERE s.start_at<=? AND s.end_at>?
                  AND s.point_space_ids @> jsonb_build_array(t.space_id)
                  AND NOT EXISTS(SELECT 1 FROM staff_shift_covers c WHERE c.shift_id=s.id AND c.start_at<=? AND c.end_at>?)
                ORDER BY s.user_id
            """,String.class,table,ts(at),ts(at),ts(at),ts(at));
    }
    void requireTransferStaff(String table,String user,Instant at) {
        var scope=p.tableScope(table);
        p.requireWorker(requireText(user,"target_service_staff_id"),scope.project(),scope.merchant());
        if(!scheduledServiceStaff(table,at).contains(user))throw conflict("BAR_TARGET_SERVICE_STAFF_NOT_ON_DUTY","目标接手人须有该点位当前已发布排班或真实顶班记录，不能继承原台人员");
    }
    void recordTransfer(BarTableSessionRecord before,BarTableSessionRecord after,Instant at,String sourceId) {
        reassign(after,requireText(after.serviceStaffId(),"target_service_staff_id"),at,sourceId,"转台后由目标台位当班人员接手；原台区间保留");
    }
    /** 第十八批 AB 段第 190 条 190-R3（BSM:1079 撤销转台按转台前快照恢复）：台组现区间结束；转台前服务人非空的在原台接手，为空的（开台时可留空）服务人置空。 */
    void recordTransferUndo(BarTableSessionRecord restored,Instant at,String sourceId) {
        reassign(restored,restored.serviceStaffId(),at,sourceId,"撤销转台回到原台，由转台前服务人继续负责；转台期间区间保留");
    }
    private void reassign(BarTableSessionRecord after,String staff,Instant at,String sourceId,String reason) {
        for(String id:family(after.id())) {
            session(id,true);
            end(id,at);
            if(staff!=null)openFor(id,after.tableId(),staff,at,eventKey(sourceId+":"+id),reason);
            if(!id.equals(after.id()))p.jdbc.update("UPDATE bar_table_sessions SET service_staff_id=?,session_version=session_version+1,updated_at=? WHERE id=?",staff,ts(at),id);
        }
    }
    void recordMerge(BarTableSessionRecord primary,BarTableSessionRecord merged,Instant at,String sourceId) {
        for(String id:family(merged.id())) {
            session(id,true);end(id,at);
            if(primary.serviceStaffId()!=null)openFor(id,primary.tableId(),primary.serviceStaffId(),at,eventKey(sourceId+":"+id),"并台后由目标台位实际责任人接手");
            p.jdbc.update("UPDATE bar_table_sessions SET service_staff_id=?,updated_at=? WHERE id=?",primary.serviceStaffId(),ts(at),id);
        }
    }
    /** 第十批 TK-27-R3：拆出会话结束并台期间区间、由其现责任人继续负责（提成/小费归属不变）。 */
    void recordSplit(List<BarTableSessionRecord> restored,Instant at,String sourceId) {
        for(var s:restored) {
            session(s.id(),true);end(s.id(),at);
            if(s.serviceStaffId()!=null)openFor(s.id(),s.tableId(),s.serviceStaffId(),at,eventKey(sourceId+":"+s.id()),"拆台后拆出台位由其现责任人继续负责；并台期间区间保留");
        }
    }
    private List<String> family(String sessionId) {
        return p.jdbc.queryForList("""
            WITH RECURSIVE family AS (SELECT id FROM bar_table_sessions WHERE id=?
            UNION ALL SELECT s.id FROM bar_table_sessions s JOIN family f ON s.source_session_id=f.id WHERE s.status='merged')
            SELECT id FROM family ORDER BY id
            """,String.class,sessionId);
    }
    /**
     * 关台（第十批 TK-27-R1，BSM L1058「关台指账单全部结清……结束本次开台，由当班服务员或收银执行；除上述情形外，账单未结清
     * （含挂起未结清）不得关台」；DEC-R8-97-R1-03）：台组（含整体转台前旧链）全部账单须 settled 或 merged，挂账 receivable 亦拒；
     * 通过后未达标定金没收（BSM L1062）。坏账例外（第十八批 T 段第 157 条 157-R3，BSM:1109「经理确认最终无法收款后转为坏账损耗处理中并解除该订单
     * 对交班、日结的阻断」）：所属订单挂起已确认坏账的账单不算未结清。
     * 执行权限按「开台/关台」码 bar.table.open_close（V013）。
     */
    @Transactional
    @com.complex.platform.gate.GuardedAction(capability="bar.table.open_close")
    public List<Responsibility> close(String sessionId,CloseSession r) {
        var s=session(sessionId,false);var scope=p.tableScope(s.tableId());p.requireScope(scope.project(),scope.merchant(),"bar.table.open_close");
        if(r==null || r.sessionVersion()==null)throw invalid("须填写版本及实际服务结束原因");
        requireText(r.reason(),"reason");
        return Arrays.asList(idem.execute("barServiceClose:"+sessionId,requireText(r.idempotencyKey(),"idempotency_key"),r,()->{
            var current=session(sessionId,true);
            if(!"open".equals(current.status()) || current.sessionVersion()!=r.sessionVersion())throw conflict("BAR_TABLE_SESSION_VERSION_CONFLICT","场次已变化，请刷新");
            // 复核 money B3：台组全部台位自开台以来没有会话归属的订单，其账单同样纳入门槛。
            var familySessions=bars.familySessionIds(sessionId,true);
            var orderIds=new ArrayList<>(bars.listOrderIdsBySessionIds(familySessions));
            var groupTables=familySessions.stream().map(bars::findTableSessionById).flatMap(Optional::stream).toList();
            groupTables.stream().map(BarTableSessionRecord::openedAt).min(Comparator.naturalOrder()).ifPresent(since->
                bars.listOrdersWithoutSession(groupTables.stream().map(BarTableSessionRecord::tableId).distinct().toList(),since)
                    .forEach(o->orderIds.add(o.id())));
            // 第十六批第 16 条：主管「作废并冲正」后的账单（void）不算未结清。
            var unsettledBills=bars.listBillsByOrderIds(orderIds).stream()
                .filter(b->!List.of("settled","merged","void").contains(b.settlementStatus()))
                .filter(b->suspensions==null || !suspensions.badDebtConfirmed(b)).toList();
            var unsettled=unsettledBills.stream().map(BarBillRecord::billNo).toList();
            if(!unsettled.isEmpty()) {
                // 157-R5（第十八批裁决 37「包厢关台时没付清就发经理待办」）：关台照拦（BSM:1058）；包厢先在独立事务登记待办、通知当班经理，再抛原 409。
                var vip=bars.findTableById(current.tableId()).filter(t->"vip".equals(t.tableType())).orElse(null);
                if(vip!=null && unpaidCloseTodos!=null) {
                    unpaidCloseTodos.notifyAtClose(sessionId,vip,unsettledBills);
                    throw conflict("BAR_TABLE_SESSION_BILL_UNSETTLED","账单未结清，不得关台："+String.join("、",unsettled)+"；已通知当班经理确认能否收回");
                }
                throw conflict("BAR_TABLE_SESSION_BILL_UNSETTLED","账单未结清，不得关台："+String.join("、",unsettled));
            }
            var ids=family(sessionId);
            Instant now=Instant.now();
            for(String id:ids) {
                var child=session(id,true);end(id,now);
                p.jdbc.update("UPDATE bar_table_sessions SET status=CASE WHEN id=? THEN 'closed' ELSE status END,closed_at=?,updated_at=?,session_version=session_version+1 WHERE id=?",
                    sessionId,ts(now),ts(now),id);
                // 复核 money B1（BSM L1058「关台成功后台位自动进入待清台」）：台组每张原台都进入待清台；pending_clear_since 记进入时刻（money m4）。
                p.jdbc.update("UPDATE bar_tables SET status='pending_clear',pending_clear_since=?,updated_at=? WHERE id=?",ts(now),ts(now),child.tableId());
            }
            deposits.forfeitAtClose(sessionId);
            audit.recordSuccess("bar.service.closed","bar.table.open_close","bar_table_session",sessionId,null,r.idempotencyKey(),Map.of("reason",r.reason()));
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
    /** 第十八批 F 段第 14 条 14-R6：服务责任补录复核通过后追加一条已结束的区间（只补空档，已有区间与会话当前责任人不改）；登记人为补录提交人。 */
    void recordBackfilledInterval(BarTableSessionRecord session,String user,Instant start,Instant end,String backfillId,String submittedBy,String reason) {
        var scope=p.tableScope(session.tableId());
        String payer=p.requireWorker(user,scope.project(),scope.merchant());
        p.jdbc.update("INSERT INTO bar_service_responsibilities(id,session_id,table_id,project_id,merchant_id,user_id,payer_company_id,started_at,ended_at,source_id,recorded_by,reason) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
            id("bsr"),session.id(),session.tableId(),scope.project(),scope.merchant(),user,payer,ts(start),ts(end),"manual_backfill:"+backfillId,submittedBy,
            "人工补录（补录单 "+backfillId+"）："+reason);
    }
}
