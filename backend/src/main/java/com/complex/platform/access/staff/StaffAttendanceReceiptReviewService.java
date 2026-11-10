package com.complex.platform.access.staff;

// 文件版本：V0.02

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.requireText;

/** 无可信时间的领取事件：经办主管补证，由其真实上一级审批后投影；原事件不改写。 */
@Service
public class StaffAttendanceReceiptReviewService {
    public static final String ACTION = "staff.attendance.receipt.review";
    public static final String OBJECT = "staff_attendance_receipt_review";
    private static final String PERMISSION = "staff.shift.manage";

    public record Request(String idempotencyKey,String shiftId,Instant actualAt,String reason,String handoverEvidence) {}
    public record Review(String id,String eventId,String contextId,String shiftId,Instant shiftUpdatedAt,
            Instant actualAt,String reason,String handoverEvidence,String originalEvent,String reviewId,
            String status,String initiatedBy,Instant createdAt,Instant decidedAt,Instant appliedAt) {}

    private final StaffWorkPhoneAttendanceService phone;
    private final JdbcStaffRepository staff;
    private final AuthorizationService auth;
    private final NamedParameterJdbcTemplate jdbc;
    private final SupervisorReviewService reviews;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;
    private final ObjectMapper json;

    public StaffAttendanceReceiptReviewService(StaffWorkPhoneAttendanceService phone,JdbcStaffRepository staff,
            AuthorizationService auth,NamedParameterJdbcTemplate jdbc,SupervisorReviewService reviews,
            IdempotencyService idempotency,AuditEventService audit,ObjectMapper json) {
        this.phone=phone;this.staff=staff;this.auth=auth;this.jdbc=jdbc;this.reviews=reviews;
        this.idempotency=idempotency;this.audit=audit;this.json=json;
    }

    @Transactional
    public Review submit(String eventId,Request request) {
        try {
            auth.requirePermission(PERMISSION);
            var event=event(eventId);
            // 第十八批 AB 段复核意见（第 191 条）：派驻放行只认本商户设备上的事件
            phone.requireEventScope(event,PERMISSION);
            if(request==null || request.actualAt()==null)
                throw ApiException.badRequest("VALIDATION_FAILED","须填写核实的实际领取时间");
            String reason=requireText(request.reason(),"reason");
            String evidence=requireText(request.handoverEvidence(),"handover_evidence");
            staff.lockAttendanceUser(event.userId());
            return idempotency.execute("staffReceiptReview:"+event.id(),request.idempotencyKey(),request,()->{
                var current=event(eventId);
                if(!"pending_review".equals(current.status()))
                    throw ApiException.conflict("RECEIPT_ALREADY_RESOLVED","领取事件已核实，不能重新提交");
                Boolean pending=jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM staff_attendance_receipt_reviews WHERE event_id=:event AND status='pending')",Map.of("event",eventId),Boolean.class);
                if(Boolean.TRUE.equals(pending)) throw ApiException.conflict("RECEIPT_REVIEW_PENDING","该事件已有待审申请");
                var context=phone.reviewContext(current.contextId());
                var shift=phone.ownedShift(current.userId(),requireText(request.shiftId(),"shift_id"));
                validate(current,context,shift,request.actualAt());
                String id="receipt_review_"+UUID.randomUUID().toString().replace("-","");
                String proposal="员工 "+current.userId()+"；原事件 "+current.id()+"；设备 "+current.deviceId()
                        +"；原领用 "+context.id()+"（"+context.bindingAt()+" 至 "+context.closedAt()+"）"
                        +"；班次 "+shift.id()+"（"+shift.startAt()+" 至 "+shift.endAt()+"）"
                        +"；手机观察时间（未核实） "+current.observedAt()+"；拟确认实际时间 "+request.actualAt()
                        +"；原因 "+reason+"；交接核实事实 "+evidence+phone.recoverySummary(current);
                var decision=reviews.evaluate(new SupervisorReviewEvaluateRequest(request.idempotencyKey()+":review",reason,
                        List.of(),ACTION,OBJECT,id,0L,proposal,null,List.of()));
                Instant now=Instant.now();
                jdbc.update("""
                        INSERT INTO staff_attendance_receipt_reviews(id,event_id,context_id,shift_id,shift_updated_at,
                          actual_at,reason,handover_evidence,original_event,review_id,status,initiated_by,created_at)
                        VALUES(:id,:event,:context,:shift,:version,:actual,:reason,:evidence,CAST(:original AS jsonb),
                          :review,'pending',:actor,:now)
                        """,new MapSqlParameterSource().addValue("id",id).addValue("event",eventId).addValue("context",context.id())
                        .addValue("shift",shift.id()).addValue("version",ts(shift.updatedAt())).addValue("actual",ts(request.actualAt()))
                        .addValue("reason",reason).addValue("evidence",evidence).addValue("original",encode(current))
                        .addValue("review",decision.reviewId()).addValue("actor",auth.currentActor().id()).addValue("now",ts(now)));
                jdbc.update("UPDATE staff_work_phone_events SET review_id=:review,review_payload=CAST(:payload AS jsonb) WHERE id=:event",
                        Map.of("review",decision.reviewId(),"payload",encode(request),"event",eventId));
                audit.recordSuccess("staff.attendance.receipt_review_submitted",PERMISSION,OBJECT,id,null,request.idempotencyKey(),
                        Map.of("event_id",eventId,"review_id",decision.reviewId(),"shift_id",shift.id()));
                return findByReview(decision.reviewId(),false);
            },Review.class);
        }catch(RuntimeException error){failure("receipt_review_submit_failed",eventId,error);throw error;}
    }

    public List<Review> history(String eventId) {
        try {
            var event=event(eventId);
            if(event.userId().equals(auth.currentActor().id())) auth.requirePermission("staff.shift.view");
            else {auth.requirePermission("staff.attendance.view");phone.requireEventScope(event,"staff.attendance.view");}
            var result=jdbc.query("SELECT * FROM staff_attendance_receipt_reviews WHERE event_id=:event ORDER BY created_at,id",Map.of("event",eventId),this::map);
            audit.recordSuccess("staff.attendance.receipt_review_viewed","staff.attendance.view","staff_work_phone_event",eventId,null,null,Map.of("count",result.size()));
            return result;
        }catch(RuntimeException error){failure("receipt_review_view_failed",eventId,error);throw error;}
    }

    @EventListener
    @Transactional
    public void afterDecision(SupervisorReviewDecisionRecord decision) {
        if(!ACTION.equals(decision.actionType()) || !OBJECT.equals(decision.objectType())
                || !List.of("approved","rejected").contains(decision.reviewStatus())) return;
        try {
            var row=findByReview(decision.reviewId(),true);
            if(row==null || !"pending".equals(row.status())) return;
            var event=event(row.eventId());staff.lockAttendanceUser(event.userId());
            Instant now=Instant.now();
            if("approved".equals(decision.reviewStatus())) {
                if(!"pending_review".equals(event.status()) || !row.reviewId().equals(event.reviewId()))
                    throw ApiException.conflict("RECEIPT_REVIEW_CHANGED","原领取事件已改变，请重新核对");
                var context=phone.reviewContext(row.contextId());var shift=phone.ownedShift(event.userId(),row.shiftId());
                if(!shift.updatedAt().equals(row.shiftUpdatedAt())) throw ApiException.conflict("SHIFT_CHANGED","原班次审批期间已改变，请重新核对");
                validate(event,context,shift,row.actualAt());
                phone.applyReviewedReceipt(event,context,shift,row.actualAt());
            }
            jdbc.update("UPDATE staff_attendance_receipt_reviews SET status=:status,decided_at=:now,applied_at=:applied WHERE id=:id AND status='pending'",
                    new MapSqlParameterSource().addValue("status",decision.reviewStatus()).addValue("now",ts(now))
                    .addValue("applied","approved".equals(decision.reviewStatus())?ts(now):null).addValue("id",row.id()));
            jdbc.update("UPDATE staff_work_phone_events SET reviewed_at=:now WHERE id=:event AND review_id=:review",
                    Map.of("now",ts(now),"event",row.eventId(),"review",row.reviewId()));
            audit.recordSuccess("staff.attendance.receipt_review_decided","governance.supervisor_review.approve",OBJECT,row.id(),null,null,
                    Map.of("event_id",row.eventId(),"review_id",row.reviewId(),"status",decision.reviewStatus()));
        }catch(RuntimeException error){failure("receipt_review_apply_failed",decision.objectId(),error);throw error;}
    }

    private void validate(StaffWorkPhoneAttendanceService.Event event,StaffWorkPhoneAttendanceService.Context context,
            StaffShiftRecord shift,Instant actual) {
        if(!Objects.equals(context.userId(),event.userId()) || !Objects.equals(context.deviceId(),event.deviceId())
                || !Objects.equals(context.id(),event.contextId()))
            throw ApiException.forbidden("ATTENDANCE_CONTEXT_OWNER","原领取事件与领用记录不一致");
        if(context.shiftId()!=null && !context.shiftId().equals(shift.id()))
            throw ApiException.conflict("WORK_PHONE_SHIFT_CHANGED","本次领用已关联另一班次");
        if(actual.isBefore(context.bindingAt()) || actual.isAfter(event.receivedAt())
                || (context.closedAt()!=null && actual.isAfter(context.closedAt())))
            throw ApiException.badRequest("ATTENDANCE_TIME_OUTSIDE_BINDING","实际领取时间须在本次真实领用和事件接收、归还之间");
        phone.requireMatchingShift(event.userId(),shift.id(),actual);
    }

    private StaffWorkPhoneAttendanceService.Event event(String id) {
        return phone.findEvent(requireText(id,"event_id")).orElseThrow(()->ApiException.notFound("RECEIPT_EVENT_NOT_FOUND","原领取事件不存在"));
    }
    private Review findByReview(String id,boolean lock) {
        return jdbc.query("SELECT * FROM staff_attendance_receipt_reviews WHERE review_id=:id"+(lock?" FOR UPDATE":""),Map.of("id",id),this::map).stream().findFirst().orElse(null);
    }
    private Review map(java.sql.ResultSet rs,int n)throws java.sql.SQLException {
        return new Review(rs.getString("id"),rs.getString("event_id"),rs.getString("context_id"),rs.getString("shift_id"),at(rs,"shift_updated_at"),
                at(rs,"actual_at"),rs.getString("reason"),rs.getString("handover_evidence"),rs.getString("original_event"),rs.getString("review_id"),
                rs.getString("status"),rs.getString("initiated_by"),at(rs,"created_at"),at(rs,"decided_at"),at(rs,"applied_at"));
    }
    private static Instant at(java.sql.ResultSet rs,String name)throws java.sql.SQLException {var t=rs.getTimestamp(name);return t==null?null:t.toInstant();}
    private static Timestamp ts(Instant at){return Timestamp.from(at);}
    private String encode(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalArgumentException("核时证据序列化失败",e);}}
    private void failure(String name,String id,RuntimeException error){audit.recordFailure("staff.attendance."+name,PERMISSION,OBJECT,id,null,null,error instanceof ApiException api?api.code():"INTERNAL_ERROR");}
}
