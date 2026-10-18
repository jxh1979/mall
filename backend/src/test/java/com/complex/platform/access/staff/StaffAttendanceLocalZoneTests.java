package com.complex.platform.access.staff;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessCredentialProvisioner;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.staff.dto.StaffAttendancePunchRequest;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 考勤两件事（2026-08-02）：
 *
 * <p>① <b>打卡配哪个班按时间区间判定，不折日历日</b>。排班本来就存「几月几日几点 → 几月几日几点」，
 * 跨零点夜班（北京 8/2 22:00 → 8/3 06:00）的 {@code work_date} 记的是 8/2，旧代码按「打卡当天」查
 * 必然漏掉它，员工凌晨下班被记「无排班」。红轮把 {@code punch} 改回按 work_date 查即复现该 bug。
 *
 * <p>② <b>展示/统计用的日历日按所在地（北京）取</b>（2026-07-22 NEW-03 拍板）。考勤月视图的取数窗口
 * 与日行归属卡在北京 2026-08-03 02:00（= UTC 2026-08-02 18:00）——两种口径在此刻必然分叉，
 * 把 {@code LocalZone.ID} 改回 UTC 即红。
 */
class StaffAttendanceLocalZoneTests {

    /** 北京 8/2 22:00（夜班开始）。 */
    private static final Instant NIGHT_SHIFT_START = Instant.parse("2026-08-02T14:00:00Z");
    /** 北京 8/3 06:00（夜班结束）。 */
    private static final Instant NIGHT_SHIFT_END = Instant.parse("2026-08-02T22:00:00Z");
    /** 北京 8/3 02:00。 */
    private static final Instant BEIJING_0803_0200 = Instant.parse("2026-08-02T18:00:00Z");
    /** 北京 8/3 10:00。 */
    private static final Instant BEIJING_0803_1000 = Instant.parse("2026-08-03T02:00:00Z");

    private final JdbcStaffRepository repo = mock(JdbcStaffRepository.class);
    private final AuthorizationService authz = mock(AuthorizationService.class);

    private StaffService build(Instant fixedNow) {
        when(authz.currentActor()).thenReturn(
                new AccessActor("usr_1", "夜班员工", "staff", "org_1", null, "active"));
        IdempotencyService idem=mock(IdempotencyService.class);
        when(idem.execute(any(),any(),any(),any(),any())).thenAnswer(i -> ((java.util.function.Supplier<?>)i.getArgument(3)).get());
        StaffService service=new StaffService(repo, mock(AccessRepository.class), idem,
                mock(AuditEventService.class), authz, AccessCredentialProvisioner.noop(), Clock.fixed(fixedNow, ZoneOffset.UTC));
        when(repo.findProfileByUserId("usr_1")).thenReturn(java.util.Optional.of(new StaffProfileRecord("sp1","org_1",null,"usr_1","员工",null,null,null,"approved",null,null,null,"active",fixedNow,fixedNow)));
        var devices=mock(com.complex.platform.terminal.TerminalDeviceRepository.class);
        when(devices.findByDeviceId("phone")).thenReturn(java.util.Optional.of(new com.complex.platform.terminal.TerminalDeviceRecord("phone","phone","phone","staff_work_phone","project",null,null,null,null,null,null,null,"sp1","bound",fixedNow,null,null,null,null,fixedNow,fixedNow)));
        org.springframework.test.util.ReflectionTestUtils.setField(service,"terminalRepository",devices);
        var phoneAttendance=mock(StaffWorkPhoneAttendanceService.class);
        when(phoneAttendance.receivedLive(any(),any())).thenAnswer(call -> new StaffWorkPhoneAttendanceService.Context(
                "ctx","phone",fixedNow,"sp1","usr_1",call.getArgument(1),null,null,null,fixedNow));
        when(phoneAttendance.receiptJudgement(anyString(),any(StaffShiftRecord.class),anyString(),any(Instant.class)))
                .thenAnswer(call -> ((Instant)call.getArgument(3)).isAfter(((StaffShiftRecord)call.getArgument(1)).startAt()) ? "late" : "normal");
        org.springframework.test.util.ReflectionTestUtils.setField(service,"workPhoneAttendance",phoneAttendance);
        var request=new org.springframework.mock.web.MockHttpServletRequest();
        request.setAttribute("com.complex.platform.gate.verified_terminal",new com.complex.platform.gate.TerminalIdentityHolder.VerifiedTerminal("staff_work_phone","phone",true));
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(new org.springframework.web.context.request.ServletRequestAttributes(request));
        return service;
    }

    @org.junit.jupiter.api.AfterEach void clearRequest() { org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes(); }

    /** 跨零点夜班（work_date=8/2，北京 8/2 22:00 → 8/3 06:00）。 */
    private StaffShiftRecord nightShift() {
        return new StaffShiftRecord("shf_night", "usr_1", LocalDate.parse("2026-08-02"),
                NIGHT_SHIFT_START, NIGHT_SHIFT_END, List.of(), null, null, NIGHT_SHIFT_START, NIGHT_SHIFT_START);
    }

    /** 区间查返回夜班；work_date 查只有问「8/2」才给（红轮据此复现旧 bug：打卡当天是 8/3，查空 → no_shift）。 */
    private void stubNightShift() {
        when(repo.listShiftsForUserOverlapping(eq("usr_1"), any(), any())).thenReturn(List.of(nightShift()));
        when(repo.listShiftsForUserBetween(eq("usr_1"), any(), any())).thenAnswer(invocation ->
                LocalDate.parse("2026-08-02").equals(invocation.getArgument(1)) ? List.of(nightShift()) : List.of());
        when(repo.saveAttendance(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    /** 北京 8/3 05:00 下班打卡：命中跨零点的那个班，且未到 06:00 结束 → 早退。 */
    @Test
    void crossMidnightReceiptHitsOriginalShiftAndUsesActualStart() {
        stubNightShift();
        Instant at0500 = Instant.parse("2026-08-02T21:00:00Z");

        StaffAttendanceRecord saved = build(at0500).punch(new StaffAttendancePunchRequest(
                "k1", "check_in", "qr", "space:test", null, null));

        assertThat(saved.shiftId()).isEqualTo("shf_night");
        assertThat(saved.judgement()).isEqualTo("late");

        // 拉候选走的是时间窗，不是 work_date：窗口须真正罩住该班。
        ArgumentCaptor<Instant> from = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> to = ArgumentCaptor.forClass(Instant.class);
        verify(repo).listShiftsForUserOverlapping(eq("usr_1"), from.capture(), to.capture());
        assertThat(from.getValue()).isBefore(NIGHT_SHIFT_START);
        assertThat(to.getValue()).isAfter(NIGHT_SHIFT_END);
    }

    /** 北京 8/3 06:30 下班打卡：已过 06:00 结束 → 正常（仍命中该班，靠「距结束最近」兜底）。 */
    @Test
    void independentCheckoutCannotReplaceActualPhoneReturn() {
        stubNightShift();
        Instant at0630 = Instant.parse("2026-08-02T22:30:00Z");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> build(at0630).punch(new StaffAttendancePunchRequest(
                "k2", "check_out", "qr", "space:test", null, null)))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        error -> assertThat(error.code()).isEqualTo("DEVICE_RETURN_REQUIRED"));
        org.mockito.Mockito.verify(repo,org.mockito.Mockito.never()).saveAttendance(any());
    }

    /**
     * 区间优先：当天有 09:00–18:00 的班，员工北京 03:00 来打卡（远在区间外）。
     * 没有任何班覆盖该时刻 → 退回「距起止最近」兜底，仍认这个班，且早于开始 → 正常（不算迟到）。
     * 这是 ±12 小时候选窗的直接后果：离班 6 小时仍绑定该班，而不是记「无排班」。
     */
    @Test
    void punchFarOutsideAnyShiftFallsBackToNearestShift() {
        StaffShiftRecord dayShift = new StaffShiftRecord("shf_day", "usr_1", LocalDate.parse("2026-08-03"),
                Instant.parse("2026-08-03T01:00:00Z"), Instant.parse("2026-08-03T10:00:00Z"),
                List.of(), null, null, NIGHT_SHIFT_START, NIGHT_SHIFT_START);
        when(repo.listShiftsForUserOverlapping(eq("usr_1"), any(), any())).thenReturn(List.of(dayShift));
        when(repo.saveAttendance(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StaffAttendanceRecord saved = build(Instant.parse("2026-08-02T19:00:00Z"))
                .punch(new StaffAttendancePunchRequest("k3", "check_in", "nfc", "space:test", null, null));

        assertThat(saved.shiftId()).isEqualTo("shf_day");
        assertThat(saved.judgement()).isEqualTo("normal");
    }

    /** 考勤月视图：取数窗口按北京日历月切；按班次结束所在北京日历日归行。 */
    @Test
    void monthViewWindowAndDayAttributionFollowBeijingCalendarDay() {
        StaffShiftRecord shift = new StaffShiftRecord("shf_x", "usr_1", LocalDate.parse("2026-08-03"),
                BEIJING_0803_0200, BEIJING_0803_1000, List.of(), null, null, BEIJING_0803_0200, BEIJING_0803_0200);
        when(repo.listShiftsEndingBetween(eq("usr_1"), any(), any())).thenReturn(List.of(shift));
        // 打卡关联具体班次，月归属按班次结束时间。
        when(repo.listAttendanceForShifts(eq("usr_1"), any())).thenReturn(List.of(
                new StaffAttendanceRecord("att_in", "usr_1", "check_in", BEIJING_0803_0200, "nfc",
                        null, null, null, "shf_x", "normal", BEIJING_0803_0200),
                new StaffAttendanceRecord("att_out", "usr_1", "check_out", BEIJING_0803_1000, "qr",
                        null, null, null, "shf_x", "normal", BEIJING_0803_1000)));

        List<StaffAttendanceDayRecord> days = build(BEIJING_0803_0200).myAttendanceMonthView("2026-08");

        // 窗口:北京 8/1 00:00 起、北京 9/1 00:00 止(UTC 口径会是 8/1T00:00Z / 9/1T00:00Z,漏掉 8/1 凌晨那班)。
        ArgumentCaptor<Instant> from = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> to = ArgumentCaptor.forClass(Instant.class);
        verify(repo).listShiftsEndingBetween(eq("usr_1"), from.capture(), to.capture());
        assertThat(from.getValue()).isEqualTo(Instant.parse("2026-07-31T16:00:00Z"));
        assertThat(to.getValue()).isEqualTo(Instant.parse("2026-08-31T16:00:00Z"));

        // 8/3 行:凌晨上班 + 上午下班 = 出勤;UTC 口径会把凌晨那次算到 8/2 → 8/3 误判「缺勤」。
        StaffAttendanceDayRecord day = days.stream()
                .filter(d -> d.date().equals(LocalDate.parse("2026-08-03"))).findFirst().orElseThrow();
        assertThat(day.dayStatus()).containsExactly("present");
    }
}
