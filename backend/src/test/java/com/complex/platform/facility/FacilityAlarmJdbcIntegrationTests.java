package com.complex.platform.facility;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.facility.dto.FacilityAlarmDispositionRequest;
import com.complex.platform.facility.dto.FacilityAlarmDutyRequest;
import com.complex.platform.facility.dto.FacilityAlarmPolicyRequest;
import com.complex.platform.facility.dto.FacilityAlarmTakeOverRequest;
import com.complex.platform.facility.dto.FacilityEquipmentStatusRequest;
import com.complex.platform.facility.dto.FacilityFaultRuleActionRequest;
import com.complex.platform.facility.dto.FacilityFaultRuleRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderAssignRequest;
import com.complex.platform.notification.ScaffoldVoiceCallSender;
import com.complex.platform.support.TestSecurityActors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * HP-F01 R1～R4（BSM「楼宇运营」告警条 L1417；2026-10-21 操作员裁决补充第 1 条：告警类型＝设备四组台账、优先级紧急/高/普通
 * 由上报人或物联网当场选、后台 4×3 响应时限）：告警落接警单并按类型×优先级计时、值守安排挂已发布班次按顶班解析当班人、
 * 首次派发（系统消息＋短信＋语音，失败立即转当班备用，逐次留痕）、明确接手是唯一改变接警状态的动作；
 * R5～R7：超时只在当班范围升级、无人接手保留事实继续升级，换班未结告警须下一班明确接手，
 * 故障处置规则须专业负责人与维保单位两方确认、未覆盖的转当班专业处置人员判定且处置要求不改设备状态。
 * 请求按前端 adminClient.mapKeys 后的蛇形键反序列化。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class FacilityAlarmJdbcIntegrationTests {

    @Autowired private FacilityAlarmService alarms;
    @Autowired private FacilityAlarmEscalationJob job;
    @Autowired private FacilityEquipmentService equipment;
    @Autowired private FacilityWorkOrderService workOrders;
    @Autowired private ScaffoldVoiceCallSender voice;
    @Autowired private JdbcTemplate jdbc;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    private final Instant now = Instant.now();
    private String key;
    private String project;
    private String equipmentId;
    private String primary;
    private String backup;
    private String resting;
    private String owner;
    private String coverer;
    private String outsider;
    private String supervisor;
    private String lead;
    private String specialist;
    private String vendor;

    @BeforeEach
    void seed() throws Exception {
        key = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        project = "prj_hpf01_" + key;
        equipmentId = "feq_hpf01_" + key;
        primary = "usr_hpf01_p_" + key;
        backup = "usr_hpf01_b_" + key;
        resting = "usr_hpf01_r_" + key;
        owner = "usr_hpf01_o_" + key;
        coverer = "usr_hpf01_c_" + key;
        outsider = "usr_hpf01_x_" + key;
        supervisor = "usr_hpf01_s_" + key;
        lead = "usr_hpf01_l_" + key;
        specialist = "usr_hpf01_e_" + key;
        vendor = "usr_hpf01_v_" + key;
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', '接警综合体', 'active', now(), now())", project);
        for (String user : List.of(primary, backup, resting, owner, coverer, outsider, supervisor, lead, specialist)) {
            jdbc.update("INSERT INTO staff_profiles (id, project_id, name, user_id, phone, status, registration_status) VALUES (?, ?, ?, ?, ?, 'active', 'approved')",
                    "sp_" + user, project, "员工" + user, user, "139" + Math.abs(user.hashCode() % 100000000));
        }
        jdbc.update("""
                INSERT INTO facility_equipment (id, name, equipment_type, space_id, responsible_user_id, status, project_id)
                VALUES (?, '1号客梯', 'elevator', 'sp_lobby', ?, 'normal', ?)""", equipmentId, owner, project);
        TestSecurityActors.asPlatformAdmin();
        alarms.savePolicy(json.readValue("""
                {"alarm_type":"elevator","priority":"urgent","response_minutes":5,"idempotency_key":"pol_%s"}
                """.formatted(key), FacilityAlarmPolicyRequest.class));
    }

    @AfterEach
    void tearDown() {
        voice.setMode(ScaffoldVoiceCallSender.Mode.PLACED);
        TestSecurityActors.clear();
    }

    /** R1：设备置告警（优先级当场选）→ 工单带设备项目与优先级；接警单待接手，响应时限＝矩阵(电梯,紧急)。 */
    @Test
    void alarmStatusChangeOpensAlarmAwaitingTakeoverAndWorkOrderCarriesEquipmentProject() throws Exception {
        raiseAlarm("urgent");

        Map<String, Object> order = jdbc.queryForMap(
                "SELECT project_id, space_id, priority, status FROM facility_work_orders WHERE equipment_id = ?", equipmentId);
        assertThat(order.get("project_id")).isEqualTo(project);
        assertThat(order.get("space_id")).isEqualTo("sp_lobby");
        assertThat(order.get("priority")).isEqualTo("urgent");
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT status, alarm_type, priority, project_id, raised_at, response_due_at FROM facility_alarms WHERE equipment_id = ?",
                equipmentId);
        assertThat(rows).hasSize(1);
        Map<String, Object> alarm = rows.get(0);
        assertThat(alarm.get("status")).isEqualTo("awaiting_takeover");
        assertThat(alarm.get("alarm_type")).isEqualTo("elevator");
        assertThat(alarm.get("project_id")).isEqualTo(project);
        assertThat(Duration.between(((Timestamp) alarm.get("raised_at")).toInstant(),
                ((Timestamp) alarm.get("response_due_at")).toInstant())).isEqualTo(Duration.ofMinutes(5));

        // 该格（电梯×普通）未配置响应时限 → 拒绝上报，不静默套用别的时限。
        jdbc.update("UPDATE facility_equipment SET status = 'normal' WHERE id = ?", equipmentId);
        assertThatThrownBy(() -> raiseAlarm("normal"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("POLICY_NOT_FOUND"));
    }

    /** R2：值守安排挂已发布班次；主接班被顶班 → 解析出顶班人；班次未开始的人（休息）不返回；同班同角色二次安排 409。 */
    @Test
    void resolveOnDutyFollowsCoverAndSkipsRestingStaff() throws Exception {
        String shiftP = shift(primary, now.minusSeconds(3600), now.plusSeconds(3600));
        String shiftR = shift(resting, now.plusSeconds(3600), now.plusSeconds(7200));
        duty(shiftP, "primary");
        duty(shiftR, "backup");
        String coverShift = shift(coverer, now.minusSeconds(600), now.plusSeconds(1800));
        jdbc.update("""
                INSERT INTO staff_shift_covers (id, shift_id, original_user_id, cover_user_id, arranged_by, reason, start_at, end_at,
                    handover_note, cover_shift_id, source_updated_at)
                VALUES (?, ?, ?, ?, 'usr_lead', '临时顶班', ?, ?, '交接接警', ?, now())""",
                "cov_" + key, shiftP, primary, coverer, Timestamp.from(now.minusSeconds(600)),
                Timestamp.from(now.plusSeconds(1800)), coverShift);

        List<FacilityAlarmService.OnDuty> onDuty = alarms.resolveOnDuty(project, List.of("primary", "backup"), now);

        assertThat(onDuty).extracting(FacilityAlarmService.OnDuty::userId).containsExactly(coverer);
        assertThat(onDuty.get(0).shiftId()).isEqualTo(coverShift);
        assertThat(onDuty.get(0).role()).isEqualTo("primary");
        assertThatThrownBy(() -> duty(shiftP, "primary"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("ALARM_DUTY_DUPLICATE"));
    }

    /** R3：当班主接收系统消息＋短信＋语音各一次留痕；设备负责人只收系统消息；休息中的备用无任何联系。 */
    @Test
    void dispatchCallsOnDutyPrimaryAndOwnerGetsSystemMessageOnly() throws Exception {
        duty(shift(primary, now.minusSeconds(3600), now.plusSeconds(3600)), "primary");
        duty(shift(resting, now.plusSeconds(3600), now.plusSeconds(7200)), "backup");

        String alarmId = raiseAlarm("urgent");

        assertThat(channels(alarmId, primary)).containsExactlyInAnyOrder("system_message", "sms", "voice");
        assertThat(jdbc.queryForObject("SELECT outcome FROM facility_alarm_contact_attempts WHERE alarm_id = ? AND user_id = ? AND channel = 'voice'",
                String.class, alarmId, primary)).isEqualTo("placed");
        assertThat(channels(alarmId, owner)).containsExactly("system_message");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM employee_notices WHERE staff_profile_id = ?", Integer.class,
                "sp_" + owner)).isEqualTo(1);
        assertThat(channels(alarmId, resting)).isEmpty();
        assertThat(alarm(alarmId).get("fallback_to_backup_at")).isNull();
    }

    /** R3：主接短信未送出且语音失败 → 同一次上报内立即转当班备用，未到响应时限。 */
    @Test
    void primarySendFailureFallsBackToOnDutyBackupImmediately() throws Exception {
        duty(shift(primary, now.minusSeconds(3600), now.plusSeconds(3600)), "primary");
        duty(shift(backup, now.minusSeconds(3600), now.plusSeconds(3600)), "backup");
        voice.setMode(ScaffoldVoiceCallSender.Mode.FAILED);

        String alarmId = raiseAlarm("urgent");

        assertThat(channels(alarmId, backup)).containsExactlyInAnyOrder("system_message", "sms", "voice");
        Map<String, Object> alarm = alarm(alarmId);
        assertThat(alarm.get("fallback_to_backup_at")).isNotNull();
        assertThat(alarm.get("status")).isEqualTo("awaiting_takeover");
        assertThat(((Timestamp) alarm.get("response_due_at")).toInstant()).isAfter(Instant.now());
    }

    /** R3：此刻没有当班主接 → 直接呼叫当班备用。 */
    @Test
    void noOnDutyPrimaryGoesStraightToBackup() throws Exception {
        duty(shift(backup, now.minusSeconds(3600), now.plusSeconds(3600)), "backup");

        String alarmId = raiseAlarm("urgent");

        assertThat(channels(alarmId, backup)).containsExactlyInAnyOrder("system_message", "sms", "voice");
        assertThat(alarm(alarmId).get("fallback_to_backup_at")).isNotNull();
    }

    /** R4：只有此刻当班的值守人能明确接手；记录确认人和时间；工单不动；同键重放同结果、换键 409。 */
    @Test
    void takeOverRecordsConfirmerAndTimeOnlyByOnDutyDutyHolder() throws Exception {
        String shiftP = shift(primary, now.minusSeconds(3600), now.plusSeconds(3600));
        duty(shiftP, "primary");
        String alarmId = raiseAlarm("urgent");

        as(outsider);
        assertThatThrownBy(() -> takeOver(alarmId, "k_x_" + key))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("ALARM_DUTY_REQUIRED"));
        assertThat(alarm(alarmId).get("status")).isEqualTo("awaiting_takeover");

        as(primary);
        FacilityAlarmRecord taken = takeOver(alarmId, "k_p_" + key);
        assertThat(taken.status()).isEqualTo("taken_over");
        Map<String, Object> row = alarm(alarmId);
        assertThat(row.get("status")).isEqualTo("taken_over");
        assertThat(row.get("holder_user_id")).isEqualTo(primary);
        assertThat(row.get("holder_shift_id")).isEqualTo(shiftP);
        assertThat(row.get("taken_over_at")).isNotNull();
        assertThat(jdbc.queryForList("SELECT kind FROM facility_alarm_takeovers WHERE alarm_id = ? AND user_id = ?",
                String.class, alarmId, primary)).containsExactly("first");
        assertThat(jdbc.queryForObject("SELECT w.status FROM facility_work_orders w JOIN facility_alarms a ON a.work_order_id = w.id WHERE a.id = ?",
                String.class, alarmId)).isEqualTo("pending_assign");

        assertThat(takeOver(alarmId, "k_p_" + key).holderUserId()).isEqualTo(primary);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM facility_alarm_takeovers WHERE alarm_id = ?", Integer.class, alarmId))
                .isEqualTo(1);
        assertThatThrownBy(() -> takeOver(alarmId, "k_p2_" + key))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("ALARM_NOT_AWAITING_TAKEOVER"));
        assertThat(alarms.listMine().items()).extracting(FacilityAlarmRecord::id).containsExactly(alarmId);
    }

    /**
     * 复核有依据直接修（BSM L1417「接警不等于维修完成，接手后派相应维修人员并跟踪处理和验收」）：设备告警自动生成的工单，
     * 接警单被当班人员明确接手前拒绝派单（409 ALARM_TAKEOVER_REQUIRED）；明确接手后照常派单。派单请求按前端蛇形键。
     */
    @Test
    void alarmWorkOrderCannotBeAssignedBeforeExplicitTakeover() throws Exception {
        duty(shift(primary, now.minusSeconds(3600), now.plusSeconds(3600)), "primary");
        String alarmId = raiseAlarm("urgent");
        String workOrderId = jdbc.queryForObject("SELECT work_order_id FROM facility_alarms WHERE id = ?", String.class, alarmId);

        TestSecurityActors.asPlatformAdmin();
        assertThatThrownBy(() -> assign(workOrderId, "k_as0_" + key))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("ALARM_TAKEOVER_REQUIRED"));
        assertThat(workOrderStatus(alarmId)).isEqualTo("pending_assign");
        // OpenAPI 声明的失败审计 facility.work_order_assign_failed：派单闸拒绝留痕。
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='facility.work_order_assign_failed' AND idempotency_key=?",
                Integer.class, "k_as0_" + key)).isEqualTo(1);

        as(primary);
        takeOver(alarmId, "k_p_" + key);
        TestSecurityActors.asPlatformAdmin();
        assertThat(assign(workOrderId, "k_as1_" + key).status()).isEqualTo("assigned");
        assertThat(workOrderStatus(alarmId)).isEqualTo("assigned");
    }

    /**
     * 同一依据（接手后才派维修人员）：告警工单待派单时被挂起再恢复，没有处理人的回「待派单」而不是「处理中」——派单闸只剩派单一个入口，
     * 接警单仍待明确接手时照样 409 ALARM_TAKEOVER_REQUIRED。挂起、恢复请求按前端蛇形键。
     */
    @Test
    void suspendResumeOfUnassignedAlarmOrderStaysBehindDispatchGate() throws Exception {
        duty(shift(primary, now.minusSeconds(3600), now.plusSeconds(3600)), "primary");
        String alarmId = raiseAlarm("urgent");
        String workOrderId = jdbc.queryForObject("SELECT work_order_id FROM facility_alarms WHERE id = ?", String.class, alarmId);

        TestSecurityActors.asPlatformAdmin();
        workOrders.suspend(workOrderId, workOrderAction("等配件", "k_sus_" + key));
        assertThat(workOrders.resume(workOrderId, workOrderAction("配件到货", "k_res_" + key)).status()).isEqualTo("pending_assign");
        assertThat(workOrderStatus(alarmId)).isEqualTo("pending_assign");
        assertThatThrownBy(() -> assign(workOrderId, "k_as2_" + key))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("ALARM_TAKEOVER_REQUIRED"));
    }

    private com.complex.platform.facility.dto.FacilityWorkOrderActionRequest workOrderAction(String note, String idempotencyKey)
            throws Exception {
        return json.readValue("{\"note\":\"%s\",\"idempotency_key\":\"%s\"}".formatted(note, idempotencyKey),
                com.complex.platform.facility.dto.FacilityWorkOrderActionRequest.class);
    }

    /** R4：语音接听、消息送达都不改接警状态；后台接警记录带逐次联系明细。 */
    @Test
    void readOrAnsweredIsNotTakeover() throws Exception {
        duty(shift(primary, now.minusSeconds(3600), now.plusSeconds(3600)), "primary");
        voice.setMode(ScaffoldVoiceCallSender.Mode.ANSWERED);

        String alarmId = raiseAlarm("urgent");

        assertThat(jdbc.queryForObject("SELECT outcome FROM facility_alarm_contact_attempts WHERE alarm_id = ? AND user_id = ? AND channel = 'voice'",
                String.class, alarmId, primary)).isEqualTo("answered");
        assertThat(alarm(alarmId).get("status")).isEqualTo("awaiting_takeover");
        assertThat(alarm(alarmId).get("holder_user_id")).isNull();
        FacilityAlarmRecord.Detail detail = alarms.listAlarms().items().stream()
                .filter(d -> d.alarm().id().equals(alarmId)).findFirst().orElseThrow();
        assertThat(detail.attempts()).extracting(FacilityAlarmRecord.Attempt::channel).contains("voice", "sms", "system_message");
        assertThat(detail.takeovers()).isEmpty();
        as(primary);
        assertThat(alarms.listMine().items()).extracting(FacilityAlarmRecord::id).containsExactly(alarmId);
    }

    /** R5：超过响应时限仍无人接手 → 只向此刻当班的备用、物业主管升级，休息中的项目负责人不呼叫；下一时限新上班的项目负责人被纳入。 */
    @Test
    void escalatesAfterResponseDueToOnDutyBackupSupervisorProjectLeadOnly() throws Exception {
        duty(shift(backup, now.minusSeconds(3600), now.plusSeconds(7200)), "backup");
        duty(shift(supervisor, now.minusSeconds(3600), now.plusSeconds(7200)), "supervisor");
        duty(shift(lead, now.plusSeconds(420), now.plusSeconds(10800)), "project_lead");
        String alarmId = raiseAlarm("urgent");
        Instant due = dueAt(alarmId);
        int before = attemptCount(alarmId);

        job.escalate(due.minusSeconds(60));
        assertThat(attemptCount(alarmId)).isEqualTo(before);

        job.escalate(due.plusSeconds(1));
        assertThat(channels(alarmId, backup, 1)).containsExactlyInAnyOrder("system_message", "sms", "voice");
        assertThat(channels(alarmId, supervisor, 1)).containsExactlyInAnyOrder("system_message", "sms", "voice");
        assertThat(channels(alarmId, lead)).isEmpty();
        assertThat(channels(alarmId, owner, 1)).isEmpty();
        Instant nextDue = dueAt(alarmId);
        assertThat(nextDue).isEqualTo(due.plusSeconds(1).plus(Duration.ofMinutes(5)));

        job.escalate(nextDue.plusSeconds(1));
        assertThat(channels(alarmId, lead, 2)).containsExactlyInAnyOrder("system_message", "sms", "voice");
        Map<String, Object> row = alarm(alarmId);
        assertThat(row.get("escalation_round")).isEqualTo(2);
        assertThat(row.get("status")).isEqualTo("awaiting_takeover");
        assertThat(workOrderStatus(alarmId)).isEqualTo("pending_assign");
    }

    /** R5：值守安排内此刻无人当班 → 不联系安排外的人，只留「无人接手」事实，接警单仍待接手。 */
    @Test
    void nobodyOnDutyOnlyAuditsUnclaimed() throws Exception {
        duty(shift(backup, now.plusSeconds(3600), now.plusSeconds(7200)), "backup");
        String alarmId = raiseAlarm("urgent");

        job.escalate(dueAt(alarmId).plusSeconds(1));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM facility_alarm_contact_attempts WHERE alarm_id = ? AND round = 1",
                Integer.class, alarmId)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'facility.alarm_unclaimed' AND business_id = ?",
                Integer.class, alarmId)).isEqualTo(1);
        assertThat(alarm(alarmId).get("status")).isEqualTo("awaiting_takeover");
        assertThat(alarm(alarmId).get("escalation_round")).isEqualTo(1);
    }

    /** R6：接手人班次结束、工单未关 → 回到待接手并呼叫下一班主接；下一班只查看不改状态，明确接手记为交接接手。 */
    @Test
    void handoverAtShiftEndRequiresNextShiftExplicitTakeover() throws Exception {
        String alarmId = takenOverThenShiftEnded();
        duty(shift(coverer, now.minusSeconds(60), now.plusSeconds(10800)), "primary");

        job.escalate(Instant.now());

        Map<String, Object> row = alarm(alarmId);
        assertThat(row.get("status")).isEqualTo("awaiting_takeover");
        assertThat(row.get("holder_user_id")).isNull();
        assertThat(channels(alarmId, coverer, 1)).containsExactlyInAnyOrder("system_message", "sms", "voice");
        assertThat(channels(alarmId, primary, 1)).isEmpty();
        as(coverer);
        assertThat(alarms.listMine().items()).extracting(FacilityAlarmRecord::id).contains(alarmId);
        assertThat(alarm(alarmId).get("status")).isEqualTo("awaiting_takeover");
        takeOver(alarmId, "k_c_" + key);
        assertThat(jdbc.queryForList("SELECT kind FROM facility_alarm_takeovers WHERE alarm_id = ? ORDER BY taken_over_at, kind",
                String.class, alarmId)).containsExactly("first", "handover");
    }

    /**
     * R6（BSM L1417「换班时未结告警须由下一班明确确认接手」；V422「临时顶班由领班直接安排并在系统记录」）：接手后领班给接手人那一段班次
     * 追加从此刻起的顶班 → 接手人已不当班，告警回到待接手、向顶班人派发，不等原班次结束。
     */
    @Test
    void midShiftCoverOfHolderRequiresHandoverToCoverer() throws Exception {
        String shiftP = shift(primary, now.minusSeconds(7200), now.plusSeconds(7200));
        duty(shiftP, "primary");
        String alarmId = raiseAlarm("urgent");
        as(primary);
        takeOver(alarmId, "k_p_" + key);
        Instant coverStart = Instant.now();
        String coverShift = shift(coverer, coverStart, now.plusSeconds(7200));
        jdbc.update("""
                INSERT INTO staff_shift_covers (id, shift_id, original_user_id, cover_user_id, arranged_by, reason, start_at, end_at,
                    handover_note, cover_shift_id, source_updated_at)
                VALUES (?, ?, ?, ?, 'usr_lead', '班中临时顶班', ?, ?, '交接接警', ?, now())""",
                "cov_mid_" + key, shiftP, primary, coverer, Timestamp.from(coverStart), Timestamp.from(now.plusSeconds(7200)),
                coverShift);

        job.escalate(coverStart.plusSeconds(1));

        Map<String, Object> row = alarm(alarmId);
        assertThat(row.get("status")).isEqualTo("awaiting_takeover");
        assertThat(row.get("holder_user_id")).isNull();
        assertThat(channels(alarmId, coverer, 1)).containsExactlyInAnyOrder("system_message", "sms", "voice");
        assertThat(channels(alarmId, primary, 1)).isEmpty();
    }

    /** OpenAPI saveFacilityAlarmResponsePolicy：同键重放返回首次结果、只记一次成功审计；校验失败记 facility.alarm_policy_save_failed。 */
    @Test
    void savePolicyIdempotentAndAuditsFailure() throws Exception {
        String body = """
                {"alarm_type":"hvac","priority":"high","response_minutes":15,"idempotency_key":"polk_%s"}
                """.formatted(key);
        alarms.savePolicy(json.readValue(body, FacilityAlarmPolicyRequest.class));
        assertThat(alarms.savePolicy(json.readValue(body, FacilityAlarmPolicyRequest.class)).responseMinutes()).isEqualTo(15);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'facility.alarm_policy_saved' AND idempotency_key = ?",
                Integer.class, "polk_" + key)).isEqualTo(1);

        assertThatThrownBy(() -> alarms.savePolicy(json.readValue("""
                {"alarm_type":"hvac","priority":"high","response_minutes":0,"idempotency_key":"polf_%s"}
                """.formatted(key), FacilityAlarmPolicyRequest.class)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'facility.alarm_policy_save_failed' AND idempotency_key = ?",
                Integer.class, "polf_" + key)).isEqualTo(1);
    }

    /** 接警系统消息：优先级用中文（裁决补充第 1 条「紧急/高/普通」），响应时限按本地时间显示。 */
    @Test
    void takeoverNoticeShowsChinesePriorityAndLocalDueTime() throws Exception {
        duty(shift(primary, now.minusSeconds(3600), now.plusSeconds(3600)), "primary");

        String alarmId = raiseAlarm("urgent");

        String due = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(com.complex.platform.common.LocalZone.ID).format(dueAt(alarmId));
        assertThat(jdbc.queryForList("SELECT body FROM employee_notices WHERE staff_profile_id = ?", String.class, "sp_" + primary))
                .anyMatch(b -> b.contains("（优先级 紧急），请在 " + due + " 前"));
    }

    /** R6：工单已关闭的告警不进交接。 */
    @Test
    void closedWorkOrderAlarmNotHandedOver() throws Exception {
        String alarmId = takenOverThenShiftEnded();
        jdbc.update("UPDATE facility_work_orders SET status = 'closed' WHERE id = (SELECT work_order_id FROM facility_alarms WHERE id = ?)",
                alarmId);

        job.escalate(Instant.now());

        assertThat(alarm(alarmId).get("status")).isEqualTo("taken_over");
        assertThat(alarm(alarmId).get("holder_user_id")).isEqualTo(primary);
    }

    /** R7：故障处置规则只一方确认不生效、告警判未覆盖；专业负责人与维保单位都确认后生效，告警按规则覆盖并随接警记录返回。 */
    @Test
    void faultRuleActiveOnlyAfterSpecialistAndVendorConfirm() throws Exception {
        duty(shift(primary, now.minusSeconds(3600), now.plusSeconds(3600)), "primary");
        TestSecurityActors.asPlatformAdmin();
        FacilityAlarmRecord.FaultRule rule = alarms.createFaultRule(json.readValue("""
                {"project_id":"%s","equipment_type":"elevator","fault_scenario":"轿厢困人","handling_requirement":"停用该梯，安抚乘客并通知维保到场","idempotency_key":"fr_%s"}
                """.formatted(project, key), FacilityFaultRuleRequest.class));
        assertThat(rule.status()).isEqualTo("pending_confirmation");

        as(specialist);
        assertThat(alarms.confirmFaultRule(rule.id(), action("frc_s_" + key)).status()).isEqualTo("pending_confirmation");
        assertThatThrownBy(() -> alarms.confirmFaultRule(rule.id(), action("frc_s2_" + key)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FAULT_RULE_ALREADY_CONFIRMED"));
        String first = raiseAlarm("urgent");
        assertThat(alarm(first).get("uncovered")).isEqualTo(true);

        asVendor(vendor, "prj_other_" + key);
        assertThatThrownBy(() -> alarms.confirmFaultRule(rule.id(), action("frc_vx_" + key)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FORBIDDEN"));
        asVendor(vendor, project);
        assertThat(alarms.confirmFaultRule(rule.id(), action("frc_v_" + key)).status()).isEqualTo("active");
        assertThat(alarms.listFaultRules(null).items()).extracting(FacilityAlarmRecord.FaultRule::id).contains(rule.id());

        jdbc.update("UPDATE facility_equipment SET status = 'normal' WHERE id = ?", equipmentId);
        String second = raiseAlarm("urgent");
        assertThat(alarm(second).get("uncovered")).isEqualTo(false);
        FacilityAlarmRecord.Detail detail = alarms.listAlarms().items().stream()
                .filter(d -> d.alarm().id().equals(second)).findFirst().orElseThrow();
        assertThat(detail.rules()).extracting(FacilityAlarmRecord.FaultRule::id).containsExactly(rule.id());
        assertThat(jdbc.queryForList("SELECT body FROM employee_notices WHERE staff_profile_id = ?", String.class, "sp_" + primary))
                .anyMatch(body -> body.contains("已确认的故障处置规则：轿厢困人：停用该梯，安抚乘客并通知维保到场"));

        as(primary);
        disposition(second, "{\"applied_rule_id\":\"%s\"}".formatted(rule.id()));
        assertThat(alarm(second).get("applied_rule_id")).isEqualTo(rule.id());
        assertThat(alarm(second).get("disposition_by")).isEqualTo(primary);
    }

    /** R7：未覆盖的告警通知当班专业处置人员（系统消息＋短信）；只有当班专业处置人员能记录判定，记录不改设备状态（不远程停机）。 */
    @Test
    void uncoveredAlarmGoesToOnDutySpecialistAndDispositionKeepsEquipmentStatus() throws Exception {
        duty(shift(primary, now.minusSeconds(3600), now.plusSeconds(3600)), "primary");
        duty(shift(specialist, now.minusSeconds(3600), now.plusSeconds(3600)), "specialist");
        duty(shift(resting, now.plusSeconds(3600), now.plusSeconds(7200)), "specialist");

        String alarmId = raiseAlarm("urgent");

        assertThat(alarm(alarmId).get("uncovered")).isEqualTo(true);
        assertThat(channels(alarmId, specialist)).containsExactlyInAnyOrder("system_message", "sms");
        assertThat(channels(alarmId, resting)).isEmpty();
        as(primary);
        assertThatThrownBy(() -> disposition(alarmId, "{\"judgement\":\"停用\"}"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("ALARM_SPECIALIST_REQUIRED"));
        as(resting);
        assertThatThrownBy(() -> disposition(alarmId, "{\"judgement\":\"停用\"}"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("ALARM_SPECIALIST_REQUIRED"));

        as(specialist);
        assertThat(alarms.listMine().items()).extracting(FacilityAlarmRecord::id).contains(alarmId);
        disposition(alarmId, "{\"judgement\":\"停用该梯，等维保到场检查\"}");

        Map<String, Object> row = alarm(alarmId);
        assertThat(row.get("disposition_note")).isEqualTo("停用该梯，等维保到场检查");
        assertThat(row.get("disposition_by")).isEqualTo(specialist);
        assertThat(row.get("disposition_at")).isNotNull();
        assertThat(jdbc.queryForObject("SELECT status FROM facility_equipment WHERE id = ?", String.class, equipmentId))
                .isEqualTo("alarm");
        assertThatThrownBy(() -> disposition(alarmId, "{\"judgement\":\"继续使用\"}"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("ALARM_DISPOSITION_RECORDED"));
    }

    // ---------------------------------------------------------------- helpers

    /** 当班主接明确接手后，把其班次与接警单记录的班末改到一分钟前，模拟接手人下班。 */
    private String takenOverThenShiftEnded() throws Exception {
        String shiftP = shift(primary, now.minusSeconds(7200), now.plusSeconds(3600));
        duty(shiftP, "primary");
        String alarmId = raiseAlarm("urgent");
        as(primary);
        takeOver(alarmId, "k_p_" + key);
        Timestamp ended = Timestamp.from(now.minusSeconds(60));
        jdbc.update("UPDATE staff_shift_schedules SET end_at = ? WHERE id = ?", ended, shiftP);
        jdbc.update("UPDATE facility_alarms SET holder_shift_end_at = ? WHERE id = ?", ended, alarmId);
        return alarmId;
    }

    private FacilityAlarmRecord disposition(String alarmId, String body) throws Exception {
        String withKey = body.substring(0, body.length() - 1) + ",\"idempotency_key\":\"dsp_" + UUID.randomUUID() + "\"}";
        return alarms.recordDisposition(alarmId, json.readValue(withKey, FacilityAlarmDispositionRequest.class));
    }

    private FacilityFaultRuleActionRequest action(String idempotencyKey) throws Exception {
        return json.readValue("{\"idempotency_key\":\"%s\"}".formatted(idempotencyKey), FacilityFaultRuleActionRequest.class);
    }

    private void asVendor(String userId, String projectId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(userId, userId, "outsourced_vendor", projectId, null, "active"), "token", List.of()));
    }

    private Instant dueAt(String alarmId) {
        return ((Timestamp) alarm(alarmId).get("response_due_at")).toInstant();
    }

    private int attemptCount(String alarmId) {
        return jdbc.queryForObject("SELECT count(*) FROM facility_alarm_contact_attempts WHERE alarm_id = ?", Integer.class, alarmId);
    }

    /** OBS-13-06（PGM L823「告警来源支持人工上报与远程/物联自动采集两种」）：员工人工上报入口不能自行声明物联来源，不落接警单。 */
    @Test
    void selfDeclaredIotAlarmRejectedAndNoAlarmOpened() throws Exception {
        TestSecurityActors.asPlatformAdmin();
        FacilityEquipmentStatusRequest request = json.readValue("""
                {"status":"alarm","alarm_source":"iot","alarm_note":"轿厢困人","priority":"urgent","idempotency_key":"st_%s"}
                """.formatted(UUID.randomUUID()), FacilityEquipmentStatusRequest.class);
        assertThatThrownBy(() -> equipment.changeStatus(equipmentId, request))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM facility_alarms WHERE equipment_id = ?", Integer.class, equipmentId)).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM facility_equipment WHERE id = ?", String.class, equipmentId)).isEqualTo("normal");
    }

    private List<String> channels(String alarmId, String userId, int round) {
        return jdbc.queryForList("SELECT channel FROM facility_alarm_contact_attempts WHERE alarm_id = ? AND user_id = ? AND round = ?",
                String.class, alarmId, userId, round);
    }

    private String workOrderStatus(String alarmId) {
        return jdbc.queryForObject("SELECT w.status FROM facility_work_orders w JOIN facility_alarms a ON a.work_order_id = w.id WHERE a.id = ?",
                String.class, alarmId);
    }

    private String raiseAlarm(String priority) throws Exception {
        TestSecurityActors.asPlatformAdmin();
        equipment.changeStatus(equipmentId, json.readValue("""
                {"status":"alarm","alarm_source":"manual","alarm_note":"轿厢困人","priority":"%s","idempotency_key":"st_%s"}
                """.formatted(priority, UUID.randomUUID()), FacilityEquipmentStatusRequest.class));
        List<String> ids = jdbc.queryForList("SELECT id FROM facility_alarms WHERE equipment_id = ? ORDER BY created_at DESC",
                String.class, equipmentId);
        assertThat(ids).as("设备告警应同事务落接警单").isNotEmpty();
        return ids.get(0);
    }

    private String shift(String user, Instant start, Instant end) {
        String id = "shift_" + user + "_" + start.getEpochSecond();
        jdbc.update("""
                INSERT INTO staff_shift_schedules (id, user_id, work_date, start_at, end_at, project_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, now(), now())""", id, user, java.sql.Date.valueOf(LocalDate.now()),
                Timestamp.from(start), Timestamp.from(end), project);
        return id;
    }

    private void duty(String shiftId, String role) throws Exception {
        TestSecurityActors.asPlatformAdmin();
        alarms.assignDuty(json.readValue("""
                {"shift_id":"%s","duty_role":"%s","contact_phone":"13800000000","idempotency_key":"duty_%s"}
                """.formatted(shiftId, role, UUID.randomUUID()), FacilityAlarmDutyRequest.class));
    }

    private FacilityWorkOrderRecord assign(String workOrderId, String idempotencyKey) throws Exception {
        return workOrders.assign(workOrderId, json.readValue("""
                {"assignee_id":"%s","idempotency_key":"%s"}
                """.formatted(coverer, idempotencyKey), FacilityWorkOrderAssignRequest.class));
    }

    private FacilityAlarmRecord takeOver(String alarmId, String idempotencyKey) throws Exception {
        return alarms.takeOver(alarmId, json.readValue("""
                {"idempotency_key":"%s","note":"已到场处置"}
                """.formatted(idempotencyKey), FacilityAlarmTakeOverRequest.class));
    }

    private void as(String userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(userId, userId, "staff", project, null, "active"), "token", List.of()));
    }

    private List<String> channels(String alarmId, String userId) {
        return jdbc.queryForList("SELECT channel FROM facility_alarm_contact_attempts WHERE alarm_id = ? AND user_id = ?",
                String.class, alarmId, userId);
    }

    private Map<String, Object> alarm(String alarmId) {
        return jdbc.queryForMap("SELECT * FROM facility_alarms WHERE id = ?", alarmId);
    }
}
