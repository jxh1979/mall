package com.complex.platform.facility;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.staff.EmployeeNoticeRecord;
import com.complex.platform.access.staff.JdbcStaffRepository;
import com.complex.platform.access.staff.StaffProfileRecord;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.facility.dto.FacilityAlarmDispositionRequest;
import com.complex.platform.facility.dto.FacilityAlarmDutyCancelRequest;
import com.complex.platform.facility.dto.FacilityAlarmDutyRequest;
import com.complex.platform.facility.dto.FacilityAlarmPolicyRequest;
import com.complex.platform.facility.dto.FacilityAlarmTakeOverRequest;
import com.complex.platform.facility.dto.FacilityFaultRuleActionRequest;
import com.complex.platform.facility.dto.FacilityFaultRuleRequest;
import com.complex.platform.notification.NotificationSendLogRecord;
import com.complex.platform.notification.NotificationService;
import com.complex.platform.notification.VoiceCallSender;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * HP-F01 设备告警接警（BSM「楼宇运营」告警条 L1417；ENUM L265；PGM L820；2026-10-21 操作员裁决补充第 1 条）。
 * <ul>
 * <li>R1 告警自动建工单的同一事务内落接警单，响应时限按「告警类型（设备四组台账）×优先级（上报时当场选）」矩阵计；</li>
 * <li>R2 接警值守安排挂已发布班次，按顶班解析此刻实际当班人；不在任何进行中班次的人＝休息，不返回、不联系；</li>
 * <li>R3 首次派发：当班主接收系统消息＋短信＋语音，逐次留痕；设备负责人只收系统消息；发送失败立即转当班备用；</li>
 * <li>R4 明确接手是唯一改变接警状态的动作，记录确认人与时间；送达、已读、接听都不改接警状态，也不改工单状态。</li>
 * <li>R5/R6 超时只在当班范围升级、无人接手保留事实继续升级；换班时未结告警须下一班明确接手（{@link #escalate}）。</li>
 * <li>R7 故障处置规则两方确认才生效；告警无生效规则覆盖即转当班专业处置人员判定；处置要求只记录、不远程停机。</li>
 * </ul>
 */
@Service
public class FacilityAlarmService {

    static final Set<String> DUTY_ROLES = Set.of("primary", "backup", "supervisor", "project_lead", "specialist");
    /** 承担接警任务、可明确接手的角色（专业处置人员只做未覆盖故障判定，不在此列）。 */
    static final List<String> TAKEOVER_ROLES = List.of("primary", "backup", "supervisor", "project_lead");
    private static final String MANAGE = "facility.alarm.manage";
    private static final String TAKE_OVER = "facility.alarm.take_over";
    private static final String CONFIRM_RULE = "facility.fault_rule.confirm";
    private static final String RULE_BUSINESS_TYPE = "facility_fault_rule";
    private static final String BUSINESS_TYPE = "facility_alarm";
    private static final String DUTY_BUSINESS_TYPE = "facility_alarm_duty";
    private static final String AWAITING = "awaiting_takeover";
    /** 接警消息里的优先级叫法（裁决补充第 1 条「紧急/高/普通」）与本地时间的响应时限。 */
    private static final Map<String, String> PRIORITY_LABELS = Map.of("urgent", "紧急", "high", "高", "normal", "普通");
    private static final DateTimeFormatter DUE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(LocalZone.ID);

    /** 此刻的实际当班值守人：role 取值守安排角色，shiftId/endAt 为原班或顶班那一段，phone 为联系电话。 */
    record OnDuty(String dutyId, String role, String userId, String shiftId, Instant endAt, String phone) {}

    private final JdbcFacilityAlarmRepository repository;
    private final JdbcStaffRepository staffRepository;
    private final NotificationService notificationService;
    private final VoiceCallSender voiceCallSender;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;

    public FacilityAlarmService(JdbcFacilityAlarmRepository repository, JdbcStaffRepository staffRepository,
            NotificationService notificationService, VoiceCallSender voiceCallSender,
            IdempotencyService idempotencyService, AuditEventService auditEventService,
            AuthorizationService authorizationService) {
        this.repository = repository;
        this.staffRepository = staffRepository;
        this.notificationService = notificationService;
        this.voiceCallSender = voiceCallSender;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
    }

    // ================================================================ R1 接警单

    /**
     * 设备告警/故障自动建工单的同一事务内落接警单并首次派发。该格响应时限未配置即拒绝上报（同工单策略 POLICY_NOT_FOUND 范式）。
     * ponytail: 短信与语音外呼同步跑在告警事务内（占位通道无网络调用）；接真实供应商时改为提交后（AFTER_COMMIT）发送。
     */
    void open(FacilityEquipmentAlarmedEvent event, FacilityWorkOrderRecord workOrder) {
        FacilityAlarmPolicyRecord policy = repository.findActivePolicy(event.equipmentType(), event.priority())
                .orElseThrow(() -> ApiException.badRequest("POLICY_NOT_FOUND", "该告警类型与优先级的响应时限未配置"));
        Instant now = Instant.now();
        FacilityAlarmRecord alarm = new FacilityAlarmRecord("falm_" + UUID.randomUUID(), event.equipmentId(),
                workOrder.id(), event.projectId(), event.equipmentType(), event.priority(), event.alarmSource(),
                event.note(), AWAITING, now, now.plus(policy.responseMinutes(), ChronoUnit.MINUTES), 0, null, null, null,
                null, null, false, now, now, null, null, null, null);
        repository.insertAlarm(alarm);
        auditEventService.recordSuccess("facility.alarm_opened", MANAGE, BUSINESS_TYPE, alarm.id(), null, null,
                Map.of("work_order_id", workOrder.id(), "alarm_type", alarm.alarmType(), "priority", alarm.priority(),
                        "response_due_at", alarm.responseDueAt().toString()));
        String name = event.equipmentName();
        String ownerId = trimToNull(event.responsibleUserId());
        if (ownerId != null) {
            // 设备负责人只收系统消息（休息期间也只收这一条，下一工作时段确认），不发短信、不打语音、不参与响应超时。
            systemMessage(alarm, 0, ownerId, "equipment_owner", "设备告警：" + name,
                    "你负责的设备「" + name + "」发生告警。本消息只作告知；如在休息，下一工作时段确认。", now);
        }
        dispatchFirst(alarm, 0, name, now);
        if (repository.listActiveRulesFor(alarm.projectId(), alarm.equipmentId(), alarm.alarmType()).isEmpty()) {
            // R7（BSM L1417「未覆盖的转当班专业处置人员判定」）：无生效故障处置规则覆盖 → 标未覆盖并通知当班专业处置人员。
            repository.markUncovered(alarm.id());
            dispatch(alarm, 0, List.of("specialist"), name, now);
        }
    }

    /** 向当班主接派发；BSM L1417「发送失败立即转当班备用」：此刻无当班主接，或每个主接短信未送出且语音呼叫失败。 */
    private void dispatchFirst(FacilityAlarmRecord alarm, int round, String name, Instant now) {
        List<Contact> primaries = dispatch(alarm, round, List.of("primary"), name, now);
        if (primaries.stream().allMatch(Contact::failed)) {
            dispatch(alarm, round, List.of("backup"), name, now);
            repository.markFallbackToBackup(alarm.id(), now);
        }
    }

    public FacilityAlarmPolicyRecord savePolicy(FacilityAlarmPolicyRequest request) {
        try {
            authorizationService.requirePlatformScope();
            if (request == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
            }
            String type = allowed(request.alarmType(), "alarm_type", FacilityEquipmentService.TYPES);
            String priority = allowed(request.priority(), "priority", FacilityWorkOrderService.PRIORITIES);
            return idempotencyService.execute("facilityAlarmPolicySave:" + type + ":" + priority, request.idempotencyKey(),
                    request, () -> {
                        if (request.responseMinutes() == null || request.responseMinutes() <= 0) {
                            throw ApiException.badRequest("VALIDATION_FAILED", "response_minutes 必须大于 0");
                        }
                        FacilityAlarmPolicyRecord saved = repository.savePolicy(type, priority, request.responseMinutes(),
                                request.active() == null || request.active(), Instant.now());
                        auditEventService.recordSuccess("facility.alarm_policy_saved", MANAGE, "facility_alarm_policy",
                                type + ":" + priority, null, request.idempotencyKey(),
                                Map.of("response_minutes", saved.responseMinutes(), "active", saved.active()));
                        return saved;
                    }, FacilityAlarmPolicyRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("facility.alarm_policy_save_failed", MANAGE, "facility_alarm_policy", null, null,
                    request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    // ================================================================ R2 接警值守安排

    public FacilityAlarmDutyRecord assignDuty(FacilityAlarmDutyRequest request) {
        try {
            authorizationService.requirePlatformScope();
            return idempotencyService.execute("facilityAlarmDutyAssign", request == null ? null : request.idempotencyKey(),
                    request, () -> {
                        String shiftId = requireText(request.shiftId(), "shift_id");
                        String role = allowed(request.dutyRole(), "duty_role", DUTY_ROLES);
                        String phone = requireText(request.contactPhone(), "contact_phone");
                        if (phone.length() > 32) {
                            throw ApiException.badRequest("VALIDATION_FAILED", "联系电话不能超过 32 个字符");
                        }
                        JdbcFacilityAlarmRepository.ShiftRow shift = repository.findShift(shiftId)
                                .orElseThrow(() -> ApiException.notFound("SHIFT_NOT_FOUND", "班次不存在或未发布"));
                        Instant now = Instant.now();
                        if (!shift.endAt().isAfter(now)) {
                            throw ApiException.conflict("SHIFT_ENDED", "班次已结束，不能再安排接警值守");
                        }
                        if (shift.projectId() == null) {
                            throw ApiException.badRequest("SHIFT_PROJECT_REQUIRED", "班次须归属项目才能安排接警值守");
                        }
                        if (repository.activeDutyExists(shiftId, role)) {
                            throw ApiException.conflict("ALARM_DUTY_DUPLICATE", "该班次已安排了这一接警角色");
                        }
                        String id = "fadu_" + UUID.randomUUID();
                        repository.insertDuty(id, shift.projectId(), shiftId, role, phone, actorId(), now);
                        FacilityAlarmDutyRecord saved = repository.findDuty(id, false).orElseThrow();
                        auditEventService.recordSuccess("facility.alarm_duty_assigned", MANAGE, DUTY_BUSINESS_TYPE, id,
                                null, request.idempotencyKey(), Map.of("shift_id", shiftId, "duty_role", role));
                        return saved;
                    }, FacilityAlarmDutyRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("facility.alarm_duty_assign_failed", MANAGE, DUTY_BUSINESS_TYPE, null, null,
                    request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    public FacilityAlarmDutyRecord cancelDuty(String dutyId, FacilityAlarmDutyCancelRequest request) {
        try {
            authorizationService.requirePlatformScope();
            return idempotencyService.execute("facilityAlarmDutyCancel:" + dutyId,
                    request == null ? null : request.idempotencyKey(), request, () -> {
                        FacilityAlarmDutyRecord duty = repository.findDuty(dutyId, true)
                                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "接警值守安排不存在"));
                        if (duty.cancelledAt() != null) {
                            throw ApiException.conflict("ALARM_DUTY_CANCELLED", "该接警值守安排已撤销");
                        }
                        String reason = requireText(request.reason(), "reason");
                        repository.cancelDuty(dutyId, actorId(), reason, Instant.now());
                        auditEventService.recordSuccess("facility.alarm_duty_cancelled", MANAGE, DUTY_BUSINESS_TYPE,
                                dutyId, null, request.idempotencyKey(), Map.of("reason", reason));
                        return repository.findDuty(dutyId, false).orElseThrow();
                    }, FacilityAlarmDutyRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("facility.alarm_duty_cancel_failed", MANAGE, DUTY_BUSINESS_TYPE, dutyId, null,
                    request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    public PageResponse<FacilityAlarmDutyRecord> listDuties(String projectId) {
        authorizationService.requirePlatformScope();
        List<FacilityAlarmDutyRecord> items = repository.listDuties(trimToNull(projectId));
        return new PageResponse<>(items, 1, 500, items.size());
    }

    /**
     * 本项目此刻实际当班、且值守安排属指定角色的人。原班被顶班时取顶班人（夜班值班经理同一份顶班解析）；
     * 联系电话：原班本人取值守安排登记的电话，顶班人取其员工档案手机号。不做全天覆盖或缺岗补位（裁决 Q17C 未采用）。
     */
    List<OnDuty> resolveOnDuty(String projectId, Collection<String> roles, Instant at) {
        if (projectId == null) {
            return List.of();
        }
        List<OnDuty> out = new ArrayList<>();
        for (FacilityAlarmDutyRecord duty : repository.listActiveDutiesAt(projectId, roles, at)) {
            for (JdbcStaffRepository.ShiftHolder holder : staffRepository.findEffectiveShiftHolders(duty.shiftId(), at)) {
                String phone = holder.userId().equals(duty.shiftUserId()) ? duty.contactPhone()
                        : staffRepository.findProfileByUserId(holder.userId()).map(StaffProfileRecord::phone)
                                .map(p -> trimToNull(p)).orElse(null);
                out.add(new OnDuty(duty.id(), duty.dutyRole(), holder.userId(), holder.shiftId(), holder.endAt(), phone));
            }
        }
        return out;
    }

    // ================================================================ R3 派发

    /** 一次联系结果：短信未送出（非 sent）且语音呼叫失败＝发送失败。 */
    record Contact(OnDuty duty, String smsOutcome, String voiceOutcome) {
        boolean failed() {
            return !"sent".equals(smsOutcome) && "failed".equals(voiceOutcome);
        }
    }

    /**
     * 对此刻当班的指定角色逐人发系统消息、短信并语音呼叫，每次都写联系留痕；外呼失败只记结果，不回滚告警。
     * 专业处置人员只为未覆盖故障作判定，收系统消息＋短信，不在语音催接之列（BSM L1417 语音只呼叫主接、备用及升级人员）。
     */
    List<Contact> dispatch(FacilityAlarmRecord alarm, int round, List<String> roles, String equipmentName, Instant now) {
        List<Contact> contacts = new ArrayList<>();
        // 「发生告警时按已确认规则处理」：接警消息带上覆盖该设备的生效规则。
        String rules = repository.listActiveRulesFor(alarm.projectId(), alarm.equipmentId(), alarm.alarmType()).stream()
                .map(r -> r.faultScenario() + "：" + r.handlingRequirement()).collect(Collectors.joining("；"));
        for (OnDuty duty : resolveOnDuty(alarm.projectId(), roles, now)) {
            if ("specialist".equals(duty.role())) {
                systemMessage(alarm, round, duty.userId(), duty.role(), "设备告警待判定处置：" + equipmentName,
                        "设备「" + equipmentName + "」告警，没有已确认的故障处置规则覆盖，请判定停用或继续使用等处置要求并在系统中记录。", now);
                contacts.add(new Contact(duty, sms(alarm, round, duty, equipmentName, now), null));
                continue;
            }
            String title = "设备告警待接手：" + equipmentName;
            systemMessage(alarm, round, duty.userId(), duty.role(), title, "设备「" + equipmentName + "」告警（优先级 "
                    + PRIORITY_LABELS.getOrDefault(alarm.priority(), alarm.priority()) + "），请在 "
                    + DUE_FORMAT.format(alarm.responseDueAt()) + " 前在系统中确认「已收到并接手」。"
                    + (rules.isEmpty() ? "" : "已确认的故障处置规则：" + rules + "。"), now);
            String sms = sms(alarm, round, duty, equipmentName, now);
            String voice = voice(alarm, round, duty, title, now);
            contacts.add(new Contact(duty, sms, voice));
        }
        return contacts;
    }

    private void systemMessage(FacilityAlarmRecord alarm, int round, String userId, String role, String title,
            String body, Instant now) {
        StaffProfileRecord profile = staffRepository.findProfileByUserId(userId).orElse(null);
        if (profile == null) {
            attempt(alarm, round, userId, role, "system_message", "failed", null, "staff_profile_missing", now);
            return;
        }
        String noticeId = "notice_" + UUID.randomUUID().toString().replace("-", "");
        staffRepository.saveNotice(new EmployeeNoticeRecord(noticeId, profile.id(), "system_notice", title, body,
                false, "pending", null, "system", now, now));
        attempt(alarm, round, userId, role, "system_message", "sent", noticeId, null, now);
    }

    private String sms(FacilityAlarmRecord alarm, int round, OnDuty duty, String equipmentName, Instant now) {
        if (duty.phone() == null) {
            return attempt(alarm, round, duty.userId(), duty.role(), "sms", "failed", null, "no_contact_phone", now);
        }
        try {
            NotificationSendLogRecord log = notificationService.notify("facility", "alarm_takeover", duty.phone(),
                    "sms.facility.alarm_takeover", Map.of("alarm_id", alarm.id(), "equipment_name", equipmentName,
                            "priority", alarm.priority(), "response_due_at", alarm.responseDueAt().toString()),
                    requestId(alarm, round, duty, "sms"));
            return attempt(alarm, round, duty.userId(), duty.role(), "sms", log.status(), log.id(), log.reason(), now);
        } catch (RuntimeException exception) {
            return attempt(alarm, round, duty.userId(), duty.role(), "sms", "failed", null, codeOf(exception), now);
        }
    }

    private String voice(FacilityAlarmRecord alarm, int round, OnDuty duty, String message, Instant now) {
        if (duty.phone() == null) {
            return attempt(alarm, round, duty.userId(), duty.role(), "voice", "failed", null, "no_contact_phone", now);
        }
        try {
            VoiceCallSender.Result result = voiceCallSender.call(requestId(alarm, round, duty, "voice"), duty.phone(),
                    message + "，请在系统中确认已收到并接手");
            String status = result == null ? null : result.status();
            if (!Set.of("placed", "answered", "failed").contains(status)) {
                return attempt(alarm, round, duty.userId(), duty.role(), "voice", "failed", null,
                        "unsupported_voice_status:" + status, now);
            }
            return attempt(alarm, round, duty.userId(), duty.role(), "voice", status, result.providerRef(),
                    result.reason(), now);
        } catch (RuntimeException exception) {
            return attempt(alarm, round, duty.userId(), duty.role(), "voice", "failed", null, codeOf(exception), now);
        }
    }

    private String attempt(FacilityAlarmRecord alarm, int round, String userId, String role, String channel,
            String outcome, String providerRef, String reason, Instant now) {
        repository.insertAttempt(new FacilityAlarmRecord.Attempt("faat_" + UUID.randomUUID(), alarm.id(), round, userId,
                role, channel, outcome, providerRef, reason, now));
        return outcome;
    }

    private String requestId(FacilityAlarmRecord alarm, int round, OnDuty duty, String channel) {
        return "falm_" + UUID.nameUUIDFromBytes((alarm.id() + "|" + round + "|" + duty.userId() + "|" + duty.role()
                + "|" + channel).getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
    }

    // ================================================================ R4 明确接手

    /**
     * 「已收到并接手」：操作人此刻须是该告警项目当班且承担接警任务的值守人；写接手记录（确认人、班次、时间），
     * 接警单转 taken_over。只改接警状态，工单仍待派单，由接手人按原派单流程派维修。
     */
    public FacilityAlarmRecord takeOver(String alarmId, FacilityAlarmTakeOverRequest request) {
        try {
            authorizationService.requirePlatformScope();
            AccessActor actor = authorizationService.currentActor();
            return idempotencyService.execute("facilityAlarmTakeOver:" + alarmId,
                    request == null ? null : request.idempotencyKey(), request, () -> {
                        FacilityAlarmRecord alarm = repository.findAlarmForUpdate(alarmId)
                                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "接警单不存在"));
                        if (!AWAITING.equals(alarm.status())) {
                            throw ApiException.conflict("ALARM_NOT_AWAITING_TAKEOVER", "该告警已有人明确接手");
                        }
                        Instant now = Instant.now();
                        OnDuty me = resolveOnDuty(alarm.projectId(), TAKEOVER_ROLES, now).stream()
                                .filter(d -> d.userId().equals(actor.id())).findFirst()
                                .orElseThrow(() -> ApiException.forbidden("ALARM_DUTY_REQUIRED",
                                        "只有此刻当班且承担接警任务的人员才能明确接手"));
                        String kind = repository.hasTakeover(alarmId) ? "handover" : "first";
                        repository.insertTakeover(new FacilityAlarmRecord.Takeover("fato_" + UUID.randomUUID(), alarmId,
                                me.userId(), me.shiftId(), me.role(), kind, trimToNull(request.note()), now));
                        repository.markTakenOver(alarmId, me.userId(), me.shiftId(), me.endAt(), now);
                        auditEventService.recordSuccess("facility.alarm_taken_over", TAKE_OVER, BUSINESS_TYPE, alarmId,
                                null, request.idempotencyKey(), Map.of("kind", kind, "duty_role", me.role()));
                        return repository.findAlarm(alarmId).orElseThrow();
                    }, FacilityAlarmRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("facility.alarm_take_over_failed", TAKE_OVER, BUSINESS_TYPE, alarmId, null,
                    request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    /**
     * BSM L1417「接警不等于维修完成，接手后派相应维修人员并跟踪处理和验收」：设备告警自动生成的工单，接警单仍待明确接手
     * （含换班后回到待接手、须下一班明确接手）时不派单。
     */
    void requireTakenOverBeforeDispatch(String workOrderId) {
        if (repository.awaitingTakeoverForWorkOrder(workOrderId)) {
            throw ApiException.conflict("ALARM_TAKEOVER_REQUIRED", "该设备告警尚未有当班人员明确接手，请先确认「已收到并接手」再派单");
        }
    }

    /** 后台接警记录：接警单带逐次联系与接手明细。 */
    public PageResponse<FacilityAlarmRecord.Detail> listAlarms() {
        authorizationService.requirePlatformScope();
        List<FacilityAlarmRecord> alarms = repository.listAlarms();
        List<String> ids = alarms.stream().map(FacilityAlarmRecord::id).toList();
        Map<String, List<FacilityAlarmRecord.Attempt>> attempts = repository.listAttempts(ids).stream()
                .collect(Collectors.groupingBy(FacilityAlarmRecord.Attempt::alarmId));
        Map<String, List<FacilityAlarmRecord.Takeover>> takeovers = repository.listTakeovers(ids).stream()
                .collect(Collectors.groupingBy(FacilityAlarmRecord.Takeover::alarmId));
        List<FacilityAlarmRecord.Detail> items = alarms.stream().map(a -> new FacilityAlarmRecord.Detail(a,
                attempts.getOrDefault(a.id(), List.of()), takeovers.getOrDefault(a.id(), List.of()),
                repository.listActiveRulesFor(a.projectId(), a.equipmentId(), a.alarmType()))).toList();
        return new PageResponse<>(items, 1, 500, items.size());
    }

    /**
     * 我的接警：本人此刻当班可接的待接手告警，加本人已接手且工单未关闭的告警，加本人此刻当班专业处置待判定的未覆盖告警。
     * 只读，不改任何状态。
     */
    public PageResponse<FacilityAlarmRecord> listMine() {
        authorizationService.requirePlatformScope();
        String me = authorizationService.currentActor().id();
        Instant now = Instant.now();
        Map<String, Boolean> onDutyByProject = new LinkedHashMap<>();
        List<FacilityAlarmRecord> items = new ArrayList<>();
        // ponytail: 待接手单逐项目解析当班人，量级为同时未接手的告警数；告警量大时改为按本人值守安排先筛项目。
        for (FacilityAlarmRecord alarm : repository.listAwaitingTakeover()) {
            if (alarm.projectId() != null && onDutyByProject.computeIfAbsent(alarm.projectId(), p ->
                    resolveOnDuty(p, TAKEOVER_ROLES, now).stream().anyMatch(d -> d.userId().equals(me)))) {
                items.add(alarm);
            }
        }
        items.addAll(repository.listHeldOpenBy(me));
        Map<String, Boolean> specialistByProject = new LinkedHashMap<>();
        for (FacilityAlarmRecord alarm : repository.listUncoveredUndisposed()) {
            // R7：此刻当班的专业处置人员待判定的未覆盖告警。
            if (items.stream().noneMatch(i -> i.id().equals(alarm.id())) && alarm.projectId() != null
                    && specialistByProject.computeIfAbsent(alarm.projectId(), p -> resolveOnDuty(p, List.of("specialist"), now)
                            .stream().anyMatch(d -> d.userId().equals(me)))) {
                items.add(alarm);
            }
        }
        return new PageResponse<>(items, 1, 500, items.size());
    }

    // ================================================================ R5/R6 超时升级与换班交接

    /**
     * 定时作业逻辑（{@link FacilityAlarmEscalationJob}）：
     * <ul>
     * <li>R5 待接手且已到响应时限 → 轮次加一，向此刻当班的主接、备用、物业主管、项目负责人发系统消息＋短信＋语音，
     * 响应时限顺延一格（下一轮按届时的当班范围继续升级）；值守安排内此刻无人当班 → 不找安排外的人、不叫醒休息人员，
     * 只留「无人接手」事实（裁决 Q17A/B、Q17C）。任何一轮都不把告警或工单置为已响应/完成。</li>
     * <li>R6 已接手、接手人那一段班次已结束或被班中临时顶班顶走、且工单未关闭 → 回到待接手，向此刻当班主接（失败转备用）重新派发，
     * 须下一班经「已收到并接手」明确接手；查看清单不算接手。</li>
     * </ul>
     */
    int escalate(Instant now) {
        int handled = 0;
        for (FacilityAlarmRecord due : repository.listDueForEscalation(now)) {
            Instant nextDue = now.plus(repository.responseMinutes(due.alarmType(), due.priority()), ChronoUnit.MINUTES);
            if (!repository.advanceEscalation(due.id(), due.escalationRound(), nextDue, now)) {
                continue;
            }
            FacilityAlarmRecord alarm = repository.findAlarm(due.id()).orElseThrow();
            List<Contact> contacts = dispatch(alarm, alarm.escalationRound(), TAKEOVER_ROLES, equipmentName(alarm), now);
            auditEventService.recordSuccess(contacts.isEmpty() ? "facility.alarm_unclaimed" : "facility.alarm_escalated",
                    MANAGE, BUSINESS_TYPE, alarm.id(), null, null, Map.of("round", alarm.escalationRound(),
                            "project_id", Objects.toString(alarm.projectId(), ""), "contacted", contacts.size()));
            handled++;
        }
        for (FacilityAlarmRecord held : repository.listHandoverDue(now)) {
            Instant nextDue = now.plus(repository.responseMinutes(held.alarmType(), held.priority()), ChronoUnit.MINUTES);
            if (!repository.requireHandover(held.id(), held.escalationRound(), nextDue, now)) {
                continue;
            }
            FacilityAlarmRecord alarm = repository.findAlarm(held.id()).orElseThrow();
            dispatchFirst(alarm, alarm.escalationRound(), equipmentName(alarm), now);
            auditEventService.recordSuccess("facility.alarm_handover_required", MANAGE, BUSINESS_TYPE, alarm.id(), null,
                    null, Map.of("round", alarm.escalationRound(), "previous_holder", held.holderUserId()));
            handled++;
        }
        return handled;
    }

    private String equipmentName(FacilityAlarmRecord alarm) {
        return repository.findEquipment(alarm.equipmentId()).map(JdbcFacilityAlarmRepository.EquipmentRow::name)
                .orElse(alarm.equipmentId());
    }

    // ================================================================ R7 故障处置规则

    /** 建档：按设备登记时项目与分类取设备台账；建档即待两方确认，确认前不覆盖任何告警。 */
    public FacilityAlarmRecord.FaultRule createFaultRule(FacilityFaultRuleRequest request) {
        try {
            authorizationService.requirePlatformScope();
            return idempotencyService.execute("facilityFaultRuleCreate", request == null ? null : request.idempotencyKey(),
                    request, () -> {
                        String equipmentId = trimToNull(request.equipmentId());
                        String projectId;
                        String type;
                        if (equipmentId != null) {
                            JdbcFacilityAlarmRepository.EquipmentRow eq = repository.findEquipment(equipmentId)
                                    .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "设备不存在"));
                            projectId = requireText(eq.projectId(), "project_id");
                            type = eq.equipmentType();
                        } else {
                            projectId = requireText(request.projectId(), "project_id");
                            type = allowed(request.equipmentType(), "equipment_type", FacilityEquipmentService.TYPES);
                        }
                        Instant now = Instant.now();
                        FacilityAlarmRecord.FaultRule rule = new FacilityAlarmRecord.FaultRule("ffr_" + UUID.randomUUID(),
                                projectId, equipmentId, type, requireText(request.faultScenario(), "fault_scenario"),
                                requireText(request.handlingRequirement(), "handling_requirement"), "pending_confirmation",
                                null, null, null, null, actorId(), now, now);
                        repository.insertFaultRule(rule);
                        auditEventService.recordSuccess("facility.fault_rule_created", MANAGE, RULE_BUSINESS_TYPE, rule.id(),
                                null, request.idempotencyKey(), Map.of("project_id", projectId, "equipment_type", type));
                        return repository.findFaultRule(rule.id(), false).orElseThrow();
                    }, FacilityAlarmRecord.FaultRule.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("facility.fault_rule_create_failed", MANAGE, RULE_BUSINESS_TYPE, null, null,
                    request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    /**
     * 两方确认（BSM L1417「先由设备专业负责人和维保单位确认」）：外包维保账号确认的记维保单位一方（须本项目），
     * 平台员工确认的记设备专业负责人一方；两方都确认才生效。
     */
    public FacilityAlarmRecord.FaultRule confirmFaultRule(String ruleId, FacilityFaultRuleActionRequest request) {
        try {
            AccessActor actor = authorizationService.currentActor();
            boolean vendor = authorizationService.isOutsourcedVendor(actor);
            if (!vendor) {
                authorizationService.requirePlatformScope();
            }
            return idempotencyService.execute("facilityFaultRuleConfirm:" + ruleId,
                    request == null ? null : request.idempotencyKey(), request, () -> {
                        FacilityAlarmRecord.FaultRule rule = repository.findFaultRule(ruleId, true)
                                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "故障处置规则不存在"));
                        if (vendor && !authorizationService.requireProjectScope(actor).equals(rule.projectId())) {
                            throw ApiException.forbidden("FORBIDDEN", "该故障处置规则不属于本维保单位所在项目");
                        }
                        if (!"pending_confirmation".equals(rule.status())) {
                            throw ApiException.conflict("FAULT_RULE_NOT_PENDING", "该故障处置规则不在待确认状态");
                        }
                        if ((vendor ? rule.vendorConfirmedAt() : rule.specialistConfirmedAt()) != null) {
                            throw ApiException.conflict("FAULT_RULE_ALREADY_CONFIRMED",
                                    vendor ? "维保单位已确认过该规则" : "设备专业负责人已确认过该规则");
                        }
                        repository.confirmFaultRule(ruleId, vendor, actor.id(), Instant.now());
                        FacilityAlarmRecord.FaultRule saved = repository.findFaultRule(ruleId, false).orElseThrow();
                        auditEventService.recordSuccess("facility.fault_rule_confirmed", CONFIRM_RULE, RULE_BUSINESS_TYPE,
                                ruleId, null, request.idempotencyKey(),
                                Map.of("side", vendor ? "vendor" : "specialist", "status", saved.status()));
                        return saved;
                    }, FacilityAlarmRecord.FaultRule.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("facility.fault_rule_confirm_failed", CONFIRM_RULE, RULE_BUSINESS_TYPE, ruleId,
                    null, request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    public FacilityAlarmRecord.FaultRule retireFaultRule(String ruleId, FacilityFaultRuleActionRequest request) {
        try {
            authorizationService.requirePlatformScope();
            return idempotencyService.execute("facilityFaultRuleRetire:" + ruleId,
                    request == null ? null : request.idempotencyKey(), request, () -> {
                        FacilityAlarmRecord.FaultRule rule = repository.findFaultRule(ruleId, true)
                                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "故障处置规则不存在"));
                        if ("retired".equals(rule.status())) {
                            throw ApiException.conflict("FAULT_RULE_RETIRED", "该故障处置规则已停用");
                        }
                        repository.retireFaultRule(ruleId, Instant.now());
                        auditEventService.recordSuccess("facility.fault_rule_retired", MANAGE, RULE_BUSINESS_TYPE, ruleId,
                                null, request.idempotencyKey(), Map.of("note", Objects.toString(trimToNull(request.note()), "")));
                        return repository.findFaultRule(ruleId, false).orElseThrow();
                    }, FacilityAlarmRecord.FaultRule.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("facility.fault_rule_retire_failed", MANAGE, RULE_BUSINESS_TYPE, ruleId, null,
                    request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    /** 维保单位只看本项目规则；平台侧可按项目筛。 */
    public PageResponse<FacilityAlarmRecord.FaultRule> listFaultRules(String projectId) {
        AccessActor actor = authorizationService.currentActor();
        String project = trimToNull(projectId);
        if (authorizationService.isOutsourcedVendor(actor)) {
            project = authorizationService.requireProjectScope(actor);
        } else {
            authorizationService.requirePlatformScope();
        }
        List<FacilityAlarmRecord.FaultRule> items = repository.listFaultRules(project);
        return new PageResponse<>(items, 1, 500, items.size());
    }

    /**
     * 记录处置要求：按覆盖该设备的生效规则处理（applied_rule_id，此刻当班的值守人可记），或由此刻当班的专业处置人员判定
     * （judgement，未覆盖时只能这样记）。只落记录，不调用任何设备控制、不改设备状态（BSM L1417「不等于授权平台直接远程停机」）。
     */
    public FacilityAlarmRecord recordDisposition(String alarmId, FacilityAlarmDispositionRequest request) {
        try {
            authorizationService.requirePlatformScope();
            AccessActor actor = authorizationService.currentActor();
            return idempotencyService.execute("facilityAlarmDisposition:" + alarmId,
                    request == null ? null : request.idempotencyKey(), request, () -> {
                        FacilityAlarmRecord alarm = repository.findAlarmForUpdate(alarmId)
                                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "接警单不存在"));
                        if (alarm.dispositionAt() != null) {
                            throw ApiException.conflict("ALARM_DISPOSITION_RECORDED", "该告警已记录处置要求");
                        }
                        Instant now = Instant.now();
                        List<String> myRoles = resolveOnDuty(alarm.projectId(), DUTY_ROLES, now).stream()
                                .filter(d -> d.userId().equals(actor.id())).map(OnDuty::role).toList();
                        String ruleId = trimToNull(request.appliedRuleId());
                        String note = trimToNull(request.judgement());
                        if (ruleId != null) {
                            if (repository.listActiveRulesFor(alarm.projectId(), alarm.equipmentId(), alarm.alarmType())
                                    .stream().noneMatch(r -> r.id().equals(ruleId))) {
                                throw ApiException.badRequest("FAULT_RULE_NOT_APPLICABLE", "该规则未生效或不覆盖这台设备");
                            }
                            if (myRoles.isEmpty()) {
                                throw ApiException.forbidden("ALARM_DUTY_REQUIRED", "只有此刻当班的接警值守人员才能记录处置要求");
                            }
                        } else {
                            requireText(note, "judgement");
                            if (!myRoles.contains("specialist")) {
                                throw ApiException.forbidden("ALARM_SPECIALIST_REQUIRED",
                                        "未按已确认规则处理的，须由此刻当班的专业处置人员判定");
                            }
                        }
                        repository.recordDisposition(alarmId, ruleId, note, actor.id(), now);
                        auditEventService.recordSuccess("facility.alarm_disposition_recorded", TAKE_OVER, BUSINESS_TYPE,
                                alarmId, null, request.idempotencyKey(),
                                Map.of("basis", ruleId != null ? "rule" : "specialist_judgement"));
                        return repository.findAlarm(alarmId).orElseThrow();
                    }, FacilityAlarmRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("facility.alarm_disposition_failed", TAKE_OVER, BUSINESS_TYPE, alarmId, null,
                    request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    // ================================================================ helpers

    private String allowed(String value, String field, Set<String> allowed) {
        String normalized = requireText(value, field).toLowerCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is invalid");
        }
        return normalized;
    }

    private String actorId() {
        return authorizationService.currentActor().id();
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException && apiException.code() != null) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }
}
