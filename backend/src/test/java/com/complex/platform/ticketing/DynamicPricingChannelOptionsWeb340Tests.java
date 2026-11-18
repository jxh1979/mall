package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AW 段第 340 条 340-R1（PGM:287「展示给用户的一切文字……必须全部为中文，不得出现任何英文」）：动态定价页渠道改为中文下拉，
 * 页面列的六个渠道代码（web、miniapp、app、kiosk、admin、channel，同 TicketPricingService.SALES_CHANNELS）走真实接口试算都得 200，
 * 页面不列的代码（wechat）得 400 VALIDATION_FAILED。服务端未改，本类只钉住页面选项与后端取值一致，不先红。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class DynamicPricingChannelOptionsWeb340Tests {

    private static final String SIMULATIONS = "/api/v1/ticketing/dynamic-pricing/simulations";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = String.valueOf(System.nanoTime());

    @Test
    void everyPageChannelOptionSimulatesAndAnUnlistedChannelIsRejected() throws Exception {
        String project = "prj_d340_" + s;
        String ticketType = "tt_d340_" + s;
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '动态定价渠道综合体', 'active', now(), now())", project);
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, weekday_price_cent, weekend_price_cent,"
                + " holiday_price_cent, stock_required, status, validity_days, created_at, updated_at)"
                + " VALUES (?, '动态定价渠道门票', 'flat', 10000, 10000, 10000, FALSE, 'published', 1, now(), now())", ticketType);
        RequestPostProcessor simulator = new StaffRequestFixture(jdbc).staff("usr_d340_" + s, "staff", project, null,
                "ticket.dynamic_pricing.simulate");

        for (String channel : List.of("web", "miniapp", "app", "kiosk", "admin", "channel")) {
            simulate(simulator, ticketType, channel)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.final_price_cent").value(10000));
        }
        simulate(simulator, ticketType, "wechat")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    private ResultActions simulate(RequestPostProcessor who, String ticketType, String channel) throws Exception {
        // 2026-12-15 为周二，取平日价 10000 分；没有动态价规则，仿真价即平日价。
        return mockMvc.perform(post(SIMULATIONS).with(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "d340-" + channel + "-" + s,
                        "ticket_type_id", ticketType, "visit_date", "2026-12-15", "sales_channel", channel))));
    }
}
