package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AT 段 298-R1（BSM:776「按日期平日票、周末票、节假日票（基础价，优先级最高）。」）：到访日落在经营参数页节假日日历内时，
 * 试算的基础价取节假日价；同一票种不在日历内的周六取周末价。真实 HTTP、真实授权链，节假日行与票种随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class TicketHolidayPriceWeb298R1Tests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = String.valueOf(System.nanoTime());

    @Test
    void simulate_visitDateInHolidayCalendar_takesHolidayPrice_plainSaturdayTakesWeekendPrice() throws Exception {
        String project = "prj_b66hol_" + s;
        String ticketType = "tt_b66hol_" + s;
        LocalDate start = LocalDate.parse("2099-10-01");
        LocalDate end = LocalDate.parse("2099-10-07");
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '节假日价综合体', 'active', now(), now())", project);
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, weekday_price_cent, weekend_price_cent,"
                + " holiday_price_cent, stock_required, status, validity_days, created_at, updated_at)"
                + " VALUES (?, '节假日价门票', 'weekday_weekend_holiday', 10000, 12000, 15000, FALSE, 'published', 1, now(), now())",
                ticketType);
        jdbc.update("INSERT INTO holiday_calendar (id, holiday_name, holiday_year, start_date, end_date, source)"
                + " VALUES (?, ?, 2099, ?, ?, 'manual')", "hol_b66_" + s, "国庆" + s, start, end);
        RequestPostProcessor staff = new StaffRequestFixture(jdbc).staff("usr_b66hol_" + s, "staff", project, null,
                "ticket.dynamic_pricing.simulate");
        LocalDate holidayWeekday = start;
        while (holidayWeekday.getDayOfWeek() == DayOfWeek.SATURDAY || holidayWeekday.getDayOfWeek() == DayOfWeek.SUNDAY) {
            holidayWeekday = holidayWeekday.plusDays(1);
        }
        LocalDate plainSaturday = end.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));

        simulate(staff, ticketType, holidayWeekday, "a")
                .andExpect(jsonPath("$.data.base_price_cent").value(15000));
        simulate(staff, ticketType, plainSaturday, "b")
                .andExpect(jsonPath("$.data.base_price_cent").value(12000));
    }

    private org.springframework.test.web.servlet.ResultActions simulate(RequestPostProcessor staff, String ticketType,
            LocalDate visitDate, String key) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "b66hol-sim-" + key + "-" + s);
        body.put("ticket_type_id", ticketType);
        body.put("visit_date", visitDate.toString());
        body.put("sales_channel", "web");
        return mockMvc.perform(post("/api/v1/ticketing/dynamic-pricing/simulations").with(staff)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk());
    }
}
