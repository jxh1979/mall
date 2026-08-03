package com.complex.platform.access.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
 * 考勤按所在地(北京)日历日（2026-07-22 NEW-03 拍板「考勤改所在地时间」，2026-08-02 落码）。
 *
 * <p>全组取值卡在北京 2026-08-03 02:00（= UTC 2026-08-02 18:00）这一刻：北京日历日是 8/3，
 * UTC 日历日是 8/2，两种取法在此刻必然分叉——故断言可证伪（把 LocalZone.ID 改回 ZoneOffset.UTC 即红）。
 * 业务上这就是夜场员工的常态：凌晨两点上班打卡、上午下班，UTC 口径会把凌晨那次算进前一天。
 */
class StaffAttendanceLocalZoneTests {

    /** 北京 2026-08-03 02:00。 */
    private static final Instant BEIJING_0803_0200 = Instant.parse("2026-08-02T18:00:00Z");
    /** 北京 2026-08-03 10:00。 */
    private static final Instant BEIJING_0803_1000 = Instant.parse("2026-08-03T02:00:00Z");

    private final JdbcStaffRepository repo = mock(JdbcStaffRepository.class);
    private final AuthorizationService authz = mock(AuthorizationService.class);

    private StaffService build(Instant fixedNow) {
        when(authz.currentActor()).thenReturn(
                new AccessActor("usr_1", "夜班员工", "staff", "org_1", null, "active"));
        return new StaffService(repo, mock(AccessRepository.class), mock(IdempotencyService.class),
                mock(AuditEventService.class), authz, AccessCredentialProvisioner.noop(),
                Clock.fixed(fixedNow, ZoneOffset.UTC));
    }

    /** 凌晨打卡认的是「北京当天」的排班：8/3 有班就该判出班次，而不是按 UTC 查 8/2 查空后记「无排班」。 */
    @Test
    void punchAtBeijingEarlyMorningMatchesSameBeijingDayShift() {
        StaffShiftRecord shift = new StaffShiftRecord("shf_night", "usr_1", LocalDate.parse("2026-08-03"),
                BEIJING_0803_0200, BEIJING_0803_1000, List.of(), null, null, BEIJING_0803_0200, BEIJING_0803_0200);
        when(repo.listShiftsForUserBetween(eq("usr_1"), any(), any())).thenAnswer(invocation ->
                LocalDate.parse("2026-08-03").equals(invocation.getArgument(1)) ? List.of(shift) : List.of());
        when(repo.saveAttendance(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StaffAttendanceRecord saved = build(BEIJING_0803_0200).punch(new StaffAttendancePunchRequest(
                "k_punch", "check_in", "nfc", "tag_1", "spc_1", "fp_1"));

        assertThat(saved.shiftId()).isEqualTo("shf_night");
        assertThat(saved.judgement()).isEqualTo("normal");

        ArgumentCaptor<LocalDate> from = ArgumentCaptor.forClass(LocalDate.class);
        verify(repo).listShiftsForUserBetween(eq("usr_1"), from.capture(), any());
        assertThat(from.getValue()).isEqualTo(LocalDate.parse("2026-08-03"));
    }

    /** 考勤月视图：取数窗口按北京日历月切；无 shift_id 的打卡按北京日历日归日行。 */
    @Test
    void monthViewWindowAndDayAttributionFollowBeijingCalendarDay() {
        StaffShiftRecord shift = new StaffShiftRecord("shf_night", "usr_1", LocalDate.parse("2026-08-03"),
                BEIJING_0803_0200, BEIJING_0803_1000, List.of(), null, null, BEIJING_0803_0200, BEIJING_0803_0200);
        when(repo.listShiftsForUserBetween(eq("usr_1"), any(), any())).thenReturn(List.of(shift));
        // shift_id 留空:强制走「按打卡时刻折日历日」这条归属分支(本次改的就是它)。
        when(repo.listAttendanceForUserBetween(eq("usr_1"), any(), any())).thenReturn(List.of(
                new StaffAttendanceRecord("att_in", "usr_1", "check_in", BEIJING_0803_0200, "nfc",
                        null, null, null, null, "normal", BEIJING_0803_0200),
                new StaffAttendanceRecord("att_out", "usr_1", "check_out", BEIJING_0803_1000, "nfc",
                        null, null, null, null, "normal", BEIJING_0803_1000)));

        List<StaffAttendanceDayRecord> days = build(BEIJING_0803_0200).myAttendanceMonthView("2026-08");

        // 窗口:北京 8/1 00:00 起、北京 9/1 00:00 止(UTC 口径会是 8/1T00:00Z / 9/1T00:00Z,漏掉 8/1 凌晨那班)。
        ArgumentCaptor<Instant> from = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> to = ArgumentCaptor.forClass(Instant.class);
        verify(repo).listAttendanceForUserBetween(eq("usr_1"), from.capture(), to.capture());
        assertThat(from.getValue()).isEqualTo(Instant.parse("2026-07-31T16:00:00Z"));
        assertThat(to.getValue()).isEqualTo(Instant.parse("2026-08-31T16:00:00Z"));

        // 8/3 行:凌晨上班 + 上午下班 = 出勤;UTC 口径会把凌晨那次算到 8/2 → 8/3 误判「缺勤」。
        StaffAttendanceDayRecord day = days.stream()
                .filter(d -> d.date().equals(LocalDate.parse("2026-08-03"))).findFirst().orElseThrow();
        assertThat(day.dayStatus()).containsExactly("present");
    }
}
