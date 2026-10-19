package com.complex.platform.finance;

// 文件版本：V0.01

import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 已审批逐日日历与商户专区整日不可用事实（V472 ticket_points_calendar_days / ticket_points_portal_outages），积分月结与
 * 普通内部结算（FS-021）共用。缺日历不猜周末或银行营业状态（BSM L1597「仅当商户专区在项目时区的一个完整自然日内全天不可用时，
 * 该自然日才不计入异议期；部分时段不可用不顺延」；BSM L1683「遇法定节假日或非银行工作日顺延至下一个银行工作日」）。
 * 项目时区：现行库内无项目时区字段，沿用所在地时区 {@link LocalZone#ID}。
 */
@Component
public class WorkingDayCalendar {

    public record Day(LocalDate date, boolean workingDay, boolean bankWorkingDay) {
    }

    private final JdbcTemplate jdbc;

    public WorkingDayCalendar(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Day day(LocalDate date) {
        return jdbc.query("SELECT working_day, bank_working_day FROM ticket_points_calendar_days WHERE calendar_date=?",
                        (rs, n) -> new Day(date, rs.getBoolean(1), rs.getBoolean(2)), date).stream().findFirst()
                .orElseThrow(() -> ApiException.badRequest("WORKING_CALENDAR_MISSING", "缺少已审批的工作日／银行工作日记录：" + date));
    }

    /** 异议截止日：可查看之日次日起逐日计工作日，项目整日不可用的自然日不计；届满当日即可付（DEC-R8-56）。 */
    public LocalDate objectionDeadline(LocalDate visible, int workingDays, String projectId) {
        LocalDate day = visible;
        int remaining = workingDays;
        while (remaining > 0) {
            day = day.plusDays(1);
            if (day(day).workingDay() && (projectId == null || !wholeDayOutage(projectId, day))) {
                remaining--;
            }
        }
        return day;
    }

    /** 当天或之后的第一个银行工作日。 */
    public LocalDate bankDayOnOrAfter(LocalDate date) {
        LocalDate d = date;
        while (!day(d).bankWorkingDay()) {
            d = d.plusDays(1);
        }
        return d;
    }

    /** 已审批的门户不可用区间并集是否覆盖该项目时区自然日全天。 */
    public boolean wholeDayOutage(String projectId, LocalDate day) {
        Instant start = day.atStartOfDay(LocalZone.ID).toInstant();
        Instant end = day.plusDays(1).atStartOfDay(LocalZone.ID).toInstant();
        List<Instant[]> spans = jdbc.query("""
                SELECT unavailable_from, unavailable_to FROM ticket_points_portal_outages
                 WHERE project_id=? AND status='approved' AND unavailable_from<? AND unavailable_to>?
                 ORDER BY unavailable_from, unavailable_to
                """, (rs, n) -> new Instant[]{rs.getTimestamp(1).toInstant(), rs.getTimestamp(2).toInstant()},
                projectId, Timestamp.from(end), Timestamp.from(start));
        Instant covered = start;
        for (Instant[] span : spans) {
            if (span[0].isAfter(covered)) {
                return false;
            }
            if (span[1].isAfter(covered)) {
                covered = span[1];
            }
            if (!covered.isBefore(end)) {
                return true;
            }
        }
        return false;
    }
}
