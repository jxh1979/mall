package com.complex.platform.access.staff;

// 文件版本：V0.03

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.gate.TerminalIdentityHolder;
import com.complex.platform.terminal.TerminalDeviceRecord;
import com.complex.platform.terminal.TerminalDeviceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.requireText;

/** 工作手机事件保留原始证据；可信单调时钟事件才形成考勤，接收时间不冒充实际领取时间。 */
@Service
public class StaffWorkPhoneAttendanceService {
    public JdbcStaffRepository.NightDuty requireCurrentNightDuty(String userId,String projectId,Instant at) {
        return staff.findCurrentNightDuty(userId,projectId,at)
                .orElseThrow(() -> ApiException.forbidden("NIGHT_DUTY_NOT_ON_SHIFT", "只有本项目当前已发布排班的夜班值班经理可办理故障换机"));
    }

    public record AnchorRequest(String idempotencyKey, String shiftId, String bootId, Long elapsedMillis) {}
    public record Context(String id, String deviceId, Instant bindingAt, String staffProfileId, String userId,
            String shiftId, String previousContextId, Instant closedAt, String closeReason, Instant createdAt) {}
    public record Anchor(String id, String contextId, String bootId, long elapsedMillis, Instant serverAt) {}
    public record AnchorResponse(Context context, Anchor anchor) {}
    public record ReceiptRequest(String eventId, String deviceId, String contextId, String anchorId, String bootId,
            Long elapsedMillis, Instant observedAt, String method, String tagRef, String spaceId) {}
    public record RecoveryRequest(ReceiptRequest original,String reason,String handoverEvidence,String proposedContextId) {
        public RecoveryRequest(ReceiptRequest original,String reason,String handoverEvidence) {
            this(original,reason,handoverEvidence,null);
        }
    }
    public record Event(String id, String submittedBy, String deviceId, String contextId, String anchorId,
            String userId, String shiftId, Instant observedAt, Instant actualAt, String method, String status,
            String reason, String attendanceId, String reviewId, Instant receivedAt) {}

    private final JdbcStaffRepository staff;
    private final TerminalDeviceRepository devices;
    private final NamedParameterJdbcTemplate jdbc;
    private final AuthorizationService auth;
    private final AccessRepository access;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;
    private final ObjectMapper json;

    public StaffWorkPhoneAttendanceService(JdbcStaffRepository staff, TerminalDeviceRepository devices,
            NamedParameterJdbcTemplate jdbc, AuthorizationService auth, AccessRepository access,
            IdempotencyService idempotency, AuditEventService audit, ObjectMapper json) {
        this.staff=staff; this.devices=devices; this.jdbc=jdbc; this.auth=auth; this.access=access;
        this.idempotency=idempotency; this.audit=audit; this.json=json;
    }

    @Transactional
    public AnchorResponse anchor(AnchorRequest request) {
        try {
            auth.requirePermission("staff.attendance.punch");
            TerminalDeviceRecord device = ownPhone();
            if (request == null || request.elapsedMillis() == null || request.elapsedMillis() < 0)
                throw ApiException.badRequest("VALIDATION_FAILED", "须提供本次启动的单调时钟值");
            requireText(request.bootId(), "boot_id");
            var profile = staff.findProfileById(device.staffId()).orElseThrow();
            staff.lockAttendanceUser(profile.userId());
            var shift = requireMatchingShift(profile.userId(), requireText(request.shiftId(), "shift_id"),Instant.now());
            return idempotency.execute("staffAttendanceAnchor", request.idempotencyKey(), request, () -> {
                var context = context(device, shift.id());
                if (context.closedAt() != null) throw ApiException.conflict("WORK_PHONE_SHIFT_CLOSED", "本次领用已结束");
                if (context.shiftId() != null && !context.shiftId().equals(shift.id()))
                    throw ApiException.conflict("WORK_PHONE_SHIFT_CHANGED", "本次领用已关联另一班次");
                if (context.shiftId() == null) {
                    jdbc.update("UPDATE staff_work_phone_contexts SET shift_id=:shift WHERE id=:id AND shift_id IS NULL",
                            Map.of("shift", shift.id(), "id", context.id()));
                    context = findContext(context.id()).orElseThrow();
                }
                var anchor = new Anchor(id("anchor_"), context.id(), request.bootId(), request.elapsedMillis(), Instant.now());
                jdbc.update("""
                        INSERT INTO staff_attendance_time_anchors(id,context_id,boot_id,elapsed_millis,server_at,issued_by)
                        VALUES(:id,:context,:boot,:elapsed,:at,:actor)
                        """, new MapSqlParameterSource().addValue("id",anchor.id()).addValue("context",context.id())
                        .addValue("boot",anchor.bootId()).addValue("elapsed",anchor.elapsedMillis())
                        .addValue("at",ts(anchor.serverAt())).addValue("actor",profile.userId()));
                audit.recordSuccess("staff.attendance.anchor_issued", "staff.attendance.punch", "staff_attendance_anchor",
                        anchor.id(), null, request.idempotencyKey(), Map.of("context_id",context.id(),"shift_id",shift.id()));
                return new AnchorResponse(context, anchor);
            }, AnchorResponse.class);
        } catch (RuntimeException error) { failure("anchor_failed",error); throw error; }
    }

    @Transactional
    public Event receipt(ReceiptRequest request) {
        try {
            auth.requirePermission("staff.attendance.punch");
            var current = ownPhone();
            if (request == null) throw ApiException.badRequest("VALIDATION_FAILED", "须提供领取事件");
            String eventId = requireText(request.eventId(), "event_id");
            if (eventId.length() > 64) throw ApiException.badRequest("VALIDATION_FAILED", "事件编号过长");
            String deviceId = requireText(request.deviceId(), "device_id");
            if (!Set.of("nfc","qr","charging_disconnect").contains(String.valueOf(request.method())))
                throw ApiException.badRequest("VALIDATION_FAILED", "领取来源须为NFC、扫码或离开充电设施");
            if (!"charging_disconnect".equals(request.method())) requireText(request.tagRef(), "tag_ref");
            String actor = auth.currentActor().id();
            staff.lockAttendanceUser(actor);
            String payload = encode(request);
            var existing = findEvent(eventId);
            if (existing.isPresent()) {
                if (!actor.equals(existing.get().submittedBy())) throw ApiException.forbidden("ATTENDANCE_EVENT_OWNER", "该事件不属于本人");
                Boolean same = jdbc.queryForObject("SELECT payload=CAST(:payload AS jsonb) FROM staff_work_phone_events WHERE id=:id",
                        Map.of("payload",payload,"id",eventId),Boolean.class);
                if (!Boolean.TRUE.equals(same)) throw ApiException.conflict("ATTENDANCE_EVENT_CHANGED", "同一事件编号不能改写内容");
                return existing.get();
            }
            Context context = request.contextId() == null ? null : findContext(request.contextId())
                    .orElseThrow(()->ApiException.notFound("WORK_PHONE_CONTEXT_REQUIRED","原领用记录不存在，不能以当前领用替代"));
            if (context != null && (!actor.equals(context.userId()) || !deviceId.equals(context.deviceId())))
                throw ApiException.forbidden("ATTENDANCE_CONTEXT_OWNER", "原领用人与设备不匹配，不能认领他人历史事件");
            if (context == null && !deviceId.equals(current.deviceId()))
                throw ApiException.forbidden("ATTENDANCE_DEVICE_MISMATCH", "缺少原领用记录，不能通过另一台手机补报");
            // 无客户端时钟锚仍绑定服务端确认的这次领用；仅保留证据，不能把接收时刻当领取时刻。
            if (context == null) context=context(current,null);
            Anchor anchor = request.anchorId() == null ? null : findAnchor(request.anchorId()).orElse(null);
            Instant received = Instant.now();
            Instant actual = null;
            String reason = "TIME_PROOF_REQUIRED";
            if (context != null && anchor != null && anchor.contextId().equals(context.id())
                    && anchor.bootId().equals(request.bootId()) && request.elapsedMillis() != null
                    && request.elapsedMillis() >= anchor.elapsedMillis()) {
                try {
                    Instant candidate = anchor.serverAt().plusMillis(Math.subtractExact(request.elapsedMillis(),anchor.elapsedMillis()));
                    if (!candidate.isAfter(received) && !candidate.isBefore(context.bindingAt())
                            && (context.closedAt() == null || !candidate.isAfter(context.closedAt())) && context.shiftId() != null) {
                        var matching=StaffAttendanceShiftMatcher.match(staff,context.userId(),"check_in",candidate);
                        if(matching!=null && matching.id().equals(context.shiftId())) { actual=candidate; reason=null; }
                        else reason="ATTENDANCE_SHIFT_MISMATCH";
                    } else reason="TIME_OUTSIDE_BINDING";
                } catch (ArithmeticException | java.time.DateTimeException error) { reason="TIME_PROOF_INVALID"; }
            }
            // 捕获时只有缓存锚、没有已确认领用编号：证明失败不能把原事件固化到当前持有人。
            // 客户端保留原编号与原证据，仍可走主管独立提出领用关联的既有核时流程。
            if (request.contextId() == null && request.anchorId() != null && actual == null)
                throw ApiException.conflict("ATTENDANCE_CONTEXT_CONFIRMATION_REQUIRED","原锚不能证明本次领用，请保留原事件并由主管核实关联及时间");
            var params = new MapSqlParameterSource().addValue("id",eventId).addValue("actor",actor).addValue("device",deviceId)
                    .addValue("context",context==null?null:context.id()).addValue("anchor",request.anchorId()).addValue("boot",request.bootId())
                    .addValue("elapsed",request.elapsedMillis()).addValue("observed",ts(request.observedAt())).addValue("method",request.method())
                    .addValue("tag",request.tagRef()).addValue("space",request.spaceId()).addValue("payload",payload)
                    .addValue("user",context==null?actor:context.userId()).addValue("shift",context==null?null:context.shiftId())
                    .addValue("reason",reason).addValue("received",ts(received));
            jdbc.update("""
                    INSERT INTO staff_work_phone_events(id,submitted_by,device_id,context_id,anchor_id,boot_id,elapsed_millis,
                        observed_at,method,tag_ref,space_id,payload,user_id,shift_id,status,reason,received_at)
                    VALUES(:id,:actor,:device,:context,:anchor,:boot,:elapsed,:observed,:method,:tag,:space,CAST(:payload AS jsonb),
                        :user,:shift,'pending_review',:reason,:received)
                    """,params);
            if (actual != null) projectReceipt(eventId, context.userId(), context.shiftId(),actual,request.method(),request.tagRef(),request.spaceId(),deviceId);
            var result = findEvent(eventId).orElseThrow();
            audit.recordSuccess("staff.attendance.receipt_recorded", "staff.attendance.punch", "staff_work_phone_event",eventId,null,eventId,
                    Map.of("status",result.status(),"device_id",deviceId));
            return result;
        } catch (RuntimeException error) { failure("receipt_failed",error); throw error; }
    }

    /** 查本人真实当前领用，不选班、不产生考勤；为离线事件保留可核对的原领用编号。 */
    @Transactional
    public Context currentContext() {
        try {
            auth.requirePermission("staff.attendance.punch");
            var device=ownPhone();
            var profile=staff.findProfileById(device.staffId()).orElseThrow();
            staff.lockAttendanceUser(profile.userId());
            var value=context(device,null);
            if(value.closedAt()!=null) throw ApiException.conflict("WORK_PHONE_SHIFT_CLOSED","本次领用已结束");
            audit.recordSuccess("staff.attendance.context_viewed","staff.attendance.punch","staff_work_phone_context",value.id(),
                    null,null,Map.of("device_id",value.deviceId(),"user_id",value.userId()));
            return value;
        }catch(RuntimeException error){failure("context_view_failed",error);throw error;}
    }

    /** 仅查本验签工作手机、指定授权员工的真实领用历史，不从手机观察时间猜持有人。 */
    public List<Context> historicalContexts(String userId) {
        try {
            auth.requirePermission("staff.shift.manage");
            var device=TerminalIdentityHolder.current().filter(t->t.verified()&&"staff_work_phone".equals(t.terminalType()))
                    .orElseThrow(()->ApiException.forbidden("WORK_PHONE_REQUIRED","须在保留原事件的已验证工作手机查询"));
            String user=requireText(userId,"user_id");
            requireScope(user,"staff.shift.manage");
            var result=jdbc.query("SELECT * FROM staff_work_phone_contexts WHERE device_id=:device AND user_id=:user ORDER BY binding_at DESC,id",
                    Map.of("device",device.deviceId(),"user",user),this::mapContext);
            audit.recordSuccess("staff.attendance.contexts_viewed","staff.shift.manage","staff_work_phone_context",null,null,null,
                    Map.of("device_id",device.deviceId(),"user_id",user,"count",result.size()));
            return result;
        }catch(RuntimeException error){
            audit.recordFailure("staff.attendance.contexts_view_failed","staff.shift.manage","staff_work_phone_context",null,null,null,
                    error instanceof ApiException api?api.code():"INTERNAL_ERROR");throw error;
        }
    }

    /** 获授权主管在原工作手机上接收原待传事件，只入待核，不代替原持有人或核时审批。 */
    @Transactional
    public Event recover(RecoveryRequest request) {
        try {
            auth.requirePermission("staff.shift.manage");
            if(request==null || request.original()==null) throw ApiException.badRequest("VALIDATION_FAILED","须选择本机保留的原事件");
            var original=request.original();
            String reason=requireText(request.reason(),"reason");
            String evidence=requireText(request.handoverEvidence(),"handover_evidence");
            String eventId=requireText(original.eventId(),"event_id");
            if(eventId.length()>64) throw ApiException.badRequest("VALIDATION_FAILED","事件编号过长");
            boolean missingContext=original.contextId()==null;
            if(!missingContext && request.proposedContextId()!=null)
                throw ApiException.badRequest("ATTENDANCE_CONTEXT_IMMUTABLE","原事件已有领用编号，不能另行指定");
            String contextId=requireText(missingContext?request.proposedContextId():original.contextId(),
                    missingContext?"proposed_context_id":"context_id");
            var device=TerminalIdentityHolder.current().filter(t->t.verified()&&"staff_work_phone".equals(t.terminalType()))
                    .orElseThrow(()->ApiException.forbidden("WORK_PHONE_REQUIRED","须在保留原事件的已验证工作手机办理"));
            var context=findContext(contextId).orElseThrow(()->ApiException.notFound("WORK_PHONE_CONTEXT_REQUIRED","原设备领用记录不存在"));
            if(!device.deviceId().equals(context.deviceId()) || !context.deviceId().equals(original.deviceId()))
                throw ApiException.forbidden("ATTENDANCE_CONTEXT_DEVICE","须由原事件所在工作手机提交原领用证据");
            if(!Set.of("nfc","qr","charging_disconnect").contains(String.valueOf(original.method())))
                throw ApiException.badRequest("VALIDATION_FAILED","领取来源无效");
            if(!"charging_disconnect".equals(original.method())) requireText(original.tagRef(),"tag_ref");
            requireScope(context.userId(),"staff.shift.manage");
            staff.lockAttendanceUser(context.userId());
            String payload=encode(original);
            var previous=findEvent(eventId);
            if(previous.isPresent()) {
                if(!context.userId().equals(previous.get().userId()) || !context.id().equals(previous.get().contextId()))
                    throw ApiException.forbidden("ATTENDANCE_CONTEXT_OWNER","原事件归属不一致");
                if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT payload=CAST(:payload AS jsonb) FROM staff_work_phone_events WHERE id=:id",
                        Map.of("payload",payload,"id",eventId),Boolean.class)))
                    throw ApiException.conflict("ATTENDANCE_EVENT_CHANGED","同一事件编号不能改写内容");
                return previous.get();
            }
            jdbc.update("""
                    INSERT INTO staff_work_phone_events(id,submitted_by,device_id,context_id,anchor_id,boot_id,elapsed_millis,
                      observed_at,method,tag_ref,space_id,payload,user_id,shift_id,status,reason,received_at)
                    VALUES(:id,:actor,:device,:context,:anchor,:boot,:elapsed,:observed,:method,:tag,:space,
                      CAST(:payload AS jsonb),:user,:shift,'pending_review',:pendingReason,:received)
                    """,new MapSqlParameterSource().addValue("id",eventId).addValue("actor",auth.currentActor().id())
                    .addValue("device",context.deviceId()).addValue("context",context.id()).addValue("anchor",original.anchorId())
                    .addValue("boot",original.bootId()).addValue("elapsed",original.elapsedMillis()).addValue("observed",ts(original.observedAt()))
                    .addValue("method",original.method()).addValue("tag",original.tagRef()).addValue("space",original.spaceId())
                    .addValue("payload",payload).addValue("user",context.userId()).addValue("shift",context.shiftId()).addValue("received",ts(Instant.now()))
                    .addValue("pendingReason",missingContext?"SUPERVISOR_CONTEXT_RECOVERY_REVIEW_REQUIRED":"SUPERVISOR_RECOVERY_REVIEW_REQUIRED"));
            audit.recordSuccess("staff.attendance.receipt_recovered","staff.shift.manage","staff_work_phone_event",eventId,null,eventId,
                    Map.of("context_id",context.id(),"user_id",context.userId(),"reason",reason,"handover_evidence",evidence,"original_context_missing",missingContext,"status","pending_review"));
            return findEvent(eventId).orElseThrow();
        }catch(RuntimeException error){
            audit.recordFailure("staff.attendance.receipt_recover_failed","staff.shift.manage","staff_work_phone_event",
                    request==null||request.original()==null?null:request.original().eventId(),null,null,
                    error instanceof ApiException api?api.code():"INTERNAL_ERROR");throw error;
        }
    }

    String recoverySummary(Event event) {
        if(!"SUPERVISOR_RECOVERY_REVIEW_REQUIRED".equals(event.reason())
                && !"SUPERVISOR_CONTEXT_RECOVERY_REVIEW_REQUIRED".equals(event.reason())) return "";
        var details=jdbc.query("""
                SELECT metadata->>'reason' AS reason,metadata->>'handover_evidence' AS evidence
                  FROM audit_events WHERE event_name='staff.attendance.receipt_recovered'
                   AND business_id=:id AND event_result='success' ORDER BY occurred_at,id LIMIT 1
                """,Map.of("id",event.id()),(rs,n)->"；代补报原因 "+rs.getString("reason")+"；原关联核实依据 "+rs.getString("evidence"));
        if(details.isEmpty()) throw ApiException.conflict("RECOVERY_EVIDENCE_REQUIRED","须保留主管代补报原始依据");
        return ("SUPERVISOR_CONTEXT_RECOVERY_REVIEW_REQUIRED".equals(event.reason())
                ?"；原事件缺少领用编号：以下员工及领用关联由主管提出，须本次审批核实，尚非已确认身份"
                :"；原事件由主管代补报")+"；代补报主管 "+event.submittedBy()+details.getFirst();
    }

    public List<Event> events(String userId) {
        try {
            String target = userId == null || userId.isBlank() ? auth.currentActor().id() : userId;
            if (target.equals(auth.currentActor().id())) auth.requirePermission("staff.shift.view");
            else { auth.requirePermission("staff.attendance.view"); requireScope(target,"staff.attendance.view"); }
            var result=jdbc.query("SELECT * FROM staff_work_phone_events WHERE user_id=:user ORDER BY received_at DESC,id",
                    Map.of("user",target),this::mapEvent);
            audit.recordSuccess("staff.attendance.receipts_viewed","staff.attendance.view","staff",target,null,null,Map.of("count",result.size()));
            return result;
        } catch (RuntimeException error) { failure("receipts_view_failed",error); throw error; }
    }

    /** 仅由已完成范围/权限检查的设备真实归还事务调用。故障换机不结束原班。 */
    public Context returned(TerminalDeviceRecord device, Instant at, boolean swap) {
        if (!"staff_work_phone".equals(device.terminalType())) return null;
        var profile=staff.findProfileById(device.staffId()).orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND","原持有员工不存在"));
        staff.lockAttendanceUser(profile.userId());
        Context context=context(device,null);
        if (context.closedAt()!=null) throw ApiException.conflict("WORK_PHONE_SHIFT_CLOSED","该次手机领用已结束");
        String checkoutId=null;
        if (!swap) {
            checkoutId=id("att_");
            var shift=context.shiftId()==null?null:ownedShift(context.userId(),context.shiftId());
            String judgement=shift==null?"no_shift":at.isBefore(shift.endAt())?"early_leave":"normal";
            staff.saveAttendance(new StaffAttendanceRecord(checkoutId,context.userId(),"check_out",at,"device_return",
                    null,device.boundSpaceId(),device.deviceId(),context.shiftId(),judgement,Instant.now()));
        }
        jdbc.update("UPDATE staff_work_phone_contexts SET closed_at=:at,close_reason=:reason,checkout_attendance_id=:checkout WHERE id=:id AND closed_at IS NULL",
                new MapSqlParameterSource().addValue("at",ts(at)).addValue("reason",swap?"fault_swap":"shift_end").addValue("id",context.id()).addValue("checkout",checkoutId));
        audit.recordSuccess(swap?"staff.attendance.phone_swap_returned":"staff.attendance.phone_returned","terminal.device.bind",
                "staff_work_phone_context",context.id(),null,null,Map.of("user_id",context.userId(),"device_id",device.deviceId()));
        return context;
    }

    /**
     * 第 96 条 96-R2（第十八批裁决 23「设备管理员收回实物后在后台办归还，回「待领用」，不给该员工记下班」）：待归还的工作手机后台收回时
     * 结束该次领用（employment_ended），不写下班考勤。仅由设备归还事务调用。
     */
    public void closeOnEmploymentEnd(TerminalDeviceRecord device, Instant at) {
        jdbc.update("UPDATE staff_work_phone_contexts SET closed_at=:at,close_reason='employment_ended' WHERE device_id=:device AND closed_at IS NULL",
                new MapSqlParameterSource().addValue("at",ts(at)).addValue("device",device.deviceId()));
    }

    public Context requireSwapContext(TerminalDeviceRecord device) {
        var profile=staff.findProfileById(device.staffId()).orElseThrow();
        staff.lockAttendanceUser(profile.userId());
        var value=context(device,null);
        if (value.shiftId()==null || value.closedAt()!=null)
            throw ApiException.conflict("WORK_PHONE_SHIFT_REQUIRED","故障换机须关联原未结束班次");
        return value;
    }

    public void swapped(Context before, TerminalDeviceRecord replacement) {
        if (before == null || before.shiftId()==null) throw ApiException.conflict("WORK_PHONE_SHIFT_REQUIRED","故障换机须先明确原班次");
        Context next=context(replacement,before.shiftId());
        if (!before.userId().equals(next.userId())) throw ApiException.conflict("WORK_PHONE_OWNER_CHANGED","换机不能改变当班员工");
        jdbc.update("UPDATE staff_work_phone_contexts SET previous_context_id=:previous WHERE id=:id",
                Map.of("previous",before.id(),"id",next.id()));
    }

    /** 在线领取同样固化本次设备绑定与班次，归还时不按最近某张未结束旧班猜测。 */
    public Context receivedLive(TerminalDeviceRecord device, String shiftId) {
        Context value=context(device,shiftId);
        if (value.closedAt()!=null) throw ApiException.conflict("WORK_PHONE_SHIFT_CLOSED","本次工作手机领用已结束");
        if (value.shiftId()==null && shiftId!=null) {
            jdbc.update("UPDATE staff_work_phone_contexts SET shift_id=:shift WHERE id=:id AND shift_id IS NULL",
                    Map.of("shift",shiftId,"id",value.id()));
            value=findContext(value.id()).orElseThrow();
        }
        return value;
    }

    /** 经夜班经理确认的同班故障换机延误不记迟到；已有上班记录不会被此规则重写。 */
    public String receiptJudgement(String userId, StaffShiftRecord shift, String deviceId, Instant actual) {
        Boolean replacement=jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM staff_work_phone_contexts
                    WHERE user_id=:user AND shift_id=:shift AND device_id=:device
                      AND previous_context_id IS NOT NULL AND binding_at<=:at
                      AND (closed_at IS NULL OR closed_at>=:at))
                """,new MapSqlParameterSource().addValue("user",userId).addValue("shift",shift.id()).addValue("device",deviceId)
                .addValue("at",ts(actual)),Boolean.class);
        return Boolean.TRUE.equals(replacement) || !actual.isAfter(shift.startAt()) ? "normal" : "late";
    }

    private Context context(TerminalDeviceRecord device, String shiftId) {
        var found=jdbc.query("SELECT * FROM staff_work_phone_contexts WHERE device_id=:device AND binding_at=:bound FOR UPDATE",
                new MapSqlParameterSource().addValue("device",device.deviceId()).addValue("bound",ts(device.boundAt())),this::mapContext);
        if (!found.isEmpty()) return found.getFirst();
        var profile=staff.findProfileById(device.staffId()).orElseThrow();
        var value=new Context(id("phone_"),device.deviceId(),device.boundAt(),profile.id(),profile.userId(),shiftId,null,null,null,Instant.now());
        jdbc.update("""
                INSERT INTO staff_work_phone_contexts(id,device_id,binding_at,staff_profile_id,user_id,shift_id,created_at)
                VALUES(:id,:device,:bound,:profile,:user,:shift,:created)
                """,new MapSqlParameterSource().addValue("id",value.id()).addValue("device",value.deviceId()).addValue("bound",ts(value.bindingAt()))
                .addValue("profile",value.staffProfileId()).addValue("user",value.userId()).addValue("shift",shiftId).addValue("created",ts(value.createdAt())));
        return value;
    }

    private void projectReceipt(String eventId,String userId,String shiftId,Instant actual,String method,String tag,String space,String device) {
        staff.lockAttendanceUser(userId);
        var shift=requireMatchingShift(userId,shiftId,actual);
        var first=staff.findFirstCheckIn(userId,shiftId,java.time.LocalDate.ofInstant(actual,LocalZone.ID));
        String judgement=receiptJudgement(userId,shift,device,actual);
        String attendanceId;
        if (first.isEmpty()) {
            attendanceId=id("att_");
            staff.saveAttendance(new StaffAttendanceRecord(attendanceId,userId,"check_in",actual,method,tag,space,device,shiftId,judgement,Instant.now()));
        } else {
            var previous=first.get(); attendanceId=previous.id();
            if (actual.isBefore(previous.punchedAt())) {
                jdbc.update("""
                        INSERT INTO staff_attendance_time_corrections(id,attendance_id,event_id,before_at,after_at,created_at)
                        VALUES(:id,:attendance,:event,:before,:after,:now)
                        """,new MapSqlParameterSource().addValue("id",id("corr_")).addValue("attendance",attendanceId).addValue("event",eventId)
                        .addValue("before",ts(previous.punchedAt())).addValue("after",ts(actual)).addValue("now",ts(Instant.now())));
                jdbc.update("""
                        UPDATE staff_attendance_records SET punched_at=:at,method=:method,tag_ref=:tag,space_id=:space,
                            device_fingerprint=:device,judgement=:judgement WHERE id=:id
                        """,new MapSqlParameterSource().addValue("at",ts(actual)).addValue("method",method).addValue("tag",tag).addValue("space",space)
                        .addValue("device",device).addValue("judgement",judgement).addValue("id",attendanceId));
            }
        }
        jdbc.update("""
                UPDATE staff_work_phone_events SET status='accepted',actual_at=:at,attendance_id=:attendance,user_id=:user,shift_id=:shift,reason=NULL WHERE id=:id
                """,new MapSqlParameterSource().addValue("at",ts(actual)).addValue("attendance",attendanceId).addValue("user",userId).addValue("shift",shiftId).addValue("id",eventId));
    }

    Context reviewContext(String id) {
        if(id==null) throw ApiException.conflict("WORK_PHONE_CONTEXT_REQUIRED","原领取事件缺少可核实的设备领用记录");
        return jdbc.query("SELECT * FROM staff_work_phone_contexts WHERE id=:id FOR UPDATE",Map.of("id",id),this::mapContext)
                .stream().findFirst().orElseThrow(()->ApiException.notFound("WORK_PHONE_CONTEXT_REQUIRED","原设备领用记录不存在"));
    }

    /** 只由已批准的核时申请在同事务调用，保留原事件与审批历史。 */
    void applyReviewedReceipt(Event event,Context context,StaffShiftRecord shift,Instant actual) {
        if(context.shiftId()==null) jdbc.update("UPDATE staff_work_phone_contexts SET shift_id=:shift WHERE id=:id AND shift_id IS NULL",
                Map.of("shift",shift.id(),"id",context.id()));
        projectReceipt(event.id(),event.userId(),shift.id(),actual,"manual",null,null,event.deviceId());
        // 实际归还发生在补证之前时，仅修正这次领用的确切下班记录，不另猜旧班或创建一次归还。
        jdbc.update("""
                UPDATE staff_attendance_records a SET shift_id=:shift,
                    judgement=CASE WHEN a.punched_at<:end THEN 'early_leave' ELSE 'normal' END
                 FROM staff_work_phone_contexts c
                 WHERE c.id=:context AND c.checkout_attendance_id=a.id AND a.user_id=:user
                   AND a.punch_type='check_out' AND a.method='device_return'
                   AND (a.shift_id IS NULL OR a.shift_id=:shift)
                """,new MapSqlParameterSource().addValue("shift",shift.id()).addValue("end",ts(shift.endAt()))
                .addValue("context",context.id()).addValue("user",event.userId()));
    }

    /** 待核或被驳回的时间证据不是缺勤结论；没有班次关联时按真实设备领用区间待核。 */
    boolean hasPendingTimeEvidence(String userId,StaffShiftRecord shift) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM staff_work_phone_events e
                  LEFT JOIN staff_work_phone_contexts c ON c.id=e.context_id
                  WHERE e.user_id=:user AND e.status='pending_review' AND (
                    e.shift_id=:shift OR c.shift_id=:shift OR EXISTS(
                      SELECT 1 FROM staff_attendance_receipt_reviews r WHERE r.event_id=e.id AND r.shift_id=:shift)
                    OR (e.shift_id IS NULL AND c.shift_id IS NULL AND c.binding_at<=:end
                        AND COALESCE(c.closed_at,e.received_at)>=:start)))
                """,new MapSqlParameterSource().addValue("user",userId).addValue("shift",shift.id())
                .addValue("start",ts(shift.startAt())).addValue("end",ts(shift.endAt())),Boolean.class));
    }

    private TerminalDeviceRecord ownPhone() {
        var terminal=TerminalIdentityHolder.current().filter(t->t.verified()&&"staff_work_phone".equals(t.terminalType()))
                .orElseThrow(()->ApiException.forbidden("WORK_PHONE_REQUIRED","须使用已验证工作手机"));
        devices.lockDevice(devices.findByDeviceId(terminal.deviceId()).orElseThrow().id());
        var device=devices.findByDeviceId(terminal.deviceId()).filter(d->"bound".equals(d.status())&&"staff_work_phone".equals(d.terminalType()))
                .orElseThrow(()->ApiException.forbidden("WORK_PHONE_REQUIRED","工作手机当前不可用"));
        var profile=staff.findProfileByUserId(auth.currentActor().id()).orElseThrow(()->ApiException.notFound("STAFF_NOT_FOUND","员工档案不存在"));
        if (!profile.id().equals(device.staffId())) throw ApiException.forbidden("WORK_PHONE_OWNER_REQUIRED","工作手机不属于本人");
        return device;
    }

    StaffShiftRecord requireMatchingShift(String user,String shiftId,Instant actual) {
        var shift=ownedShift(user,shiftId);
        var matching=StaffAttendanceShiftMatcher.match(staff,user,"check_in",actual);
        if(matching==null || !matching.id().equals(shift.id()))
            throw ApiException.conflict("ATTENDANCE_SHIFT_MISMATCH","所选班次与实际领取时刻不匹配，须按原班次与时间证据核对");
        return shift;
    }

    StaffShiftRecord ownedShift(String user,String shiftId) {
        return staff.findShiftById(shiftId).filter(s->user.equals(s.userId()))
                .orElseThrow(()->ApiException.forbidden("ATTENDANCE_SHIFT_OWNER","须选择该员工已生效的班次"));
    }

    void requireScope(String userId,String permission) {
        var actor=auth.currentActor();
        var reach=auth.projectReach(actor,permission);
        var target=access.findUserById(userId).orElseThrow(()->ApiException.notFound("STAFF_NOT_FOUND","员工不存在"));
        if (!auth.canReachUser(reach,target)) throw ApiException.forbidden("STAFF_SCOPE_MISMATCH","员工不在管理范围");
        var owner=staff.findProfileByUserId(actor.id()).orElse(null);
        var employee=staff.findProfileByUserId(userId).orElseThrow();
        // 第十八批 E 段 19-R1（M738 iter1 所选选项原文一「角色勾了哪个项目，就看、管登记在该项目或改挂到该项目的员工。」）：
        // 凭可跨项目角色触及的员工按档案登记项目判，不再比本人公司；其余照旧按本公司。
        if (employee.projectId()!=null && reach.projects().contains(employee.projectId())) return;
        if (owner!=null && owner.companyId()!=null && !Objects.equals(owner.companyId(),employee.companyId()))
            throw ApiException.forbidden("STAFF_COMPANY_SCOPE_MISMATCH","员工不在本公司范围");
    }

    private Optional<Context> findContext(String id) { return jdbc.query("SELECT * FROM staff_work_phone_contexts WHERE id=:id",Map.of("id",id),this::mapContext).stream().findFirst(); }
    Optional<Event> findEvent(String id) { return jdbc.query("SELECT * FROM staff_work_phone_events WHERE id=:id",Map.of("id",id),this::mapEvent).stream().findFirst(); }
    private Optional<Anchor> findAnchor(String id) { return jdbc.query("SELECT * FROM staff_attendance_time_anchors WHERE id=:id",Map.of("id",id),
            (rs,n)->new Anchor(rs.getString("id"),rs.getString("context_id"),rs.getString("boot_id"),rs.getLong("elapsed_millis"),instant(rs,"server_at"))).stream().findFirst(); }
    private Context mapContext(ResultSet rs,int n)throws SQLException { return new Context(rs.getString("id"),rs.getString("device_id"),instant(rs,"binding_at"),rs.getString("staff_profile_id"),
            rs.getString("user_id"),rs.getString("shift_id"),rs.getString("previous_context_id"),instant(rs,"closed_at"),rs.getString("close_reason"),instant(rs,"created_at")); }
    private Event mapEvent(ResultSet rs,int n)throws SQLException { return new Event(rs.getString("id"),rs.getString("submitted_by"),rs.getString("device_id"),rs.getString("context_id"),rs.getString("anchor_id"),
            rs.getString("user_id"),rs.getString("shift_id"),instant(rs,"observed_at"),instant(rs,"actual_at"),rs.getString("method"),rs.getString("status"),rs.getString("reason"),
            rs.getString("attendance_id"),rs.getString("review_id"),instant(rs,"received_at")); }
    private static Instant instant(ResultSet rs,String name)throws SQLException { var value=rs.getTimestamp(name);return value==null?null:value.toInstant(); }
    private static Timestamp ts(Instant value) { return value==null?null:Timestamp.from(value); }
    private static String id(String prefix) { return prefix+UUID.randomUUID().toString().replace("-",""); }
    private String encode(Object value) { try{return json.writeValueAsString(value);}catch(Exception error){throw new IllegalArgumentException("事件序列化失败",error);} }
    private void failure(String event,RuntimeException error) { audit.recordFailure("staff.attendance."+event,"staff.attendance.punch","staff_work_phone_event",null,null,null,
            error instanceof ApiException api?api.code():"INTERNAL_ERROR"); }
}

