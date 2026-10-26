package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.complex.platform.common.LocalZone;
import com.complex.platform.support.TestSecurityActors;
import com.complex.platform.ticketing.dto.TicketOrderRequest;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批第 1 条（ENUM:187「票价的日期类型（平日/周末/节假日）……一律以开台或下单时点所属的营业日为准，不按自然日切分」）：
 * 下单未带到访日时，缺省取票种当前营业日——酒吧票按该酒吧已批准版本（起点晚于此刻 → 仍是前一营业日），不取服务器时区自然日。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class TicketDefaultVisitDateJdbcIntegrationTests {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired private TicketOrderService orderService;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void orderWithoutVisitDateUsesTheBarsCurrentBusinessDay() {
        ZonedDateTime local = Instant.now().atZone(LocalZone.ID);
        assumeTrue(local.getHour() < 23, "北京 23 点后没有更晚的整点起点可配");
        String projectId = "project_tdv_" + suffix;
        String bar = "bar_tdv_" + suffix;
        String ticketTypeId = "tt_tdv_" + suffix;
        String templateId = "et_tdv_" + suffix;
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '营业日票务综合体', 'active', now(), now())", projectId);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '营业日酒吧', 'bar', 'active', now(), now(), ?)", bar, projectId);
        Instant effective = Instant.now().minusSeconds(3600);
        jdbc.update("""
                INSERT INTO bar_business_day_configs(id,bar_id,project_id,version,cutoff_hour,min_cutoff_hour,max_cutoff_hour,
                  close_hour,min_close_hour,max_close_hour,effective_at,status,reason,created_by,created_at,decided_by,decided_at)
                VALUES(?,?,?,'V0.01',?,0,23,7,0,23,?,'approved','测试已批准版本','tdv_requester',?,'usr_master',?)
                """, "bbd_tdv_" + suffix, bar, projectId, local.getHour() + 1, Timestamp.from(effective),
                Timestamp.from(effective.minusSeconds(7200)), Timestamp.from(effective.minusSeconds(3600)));
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, weekday_price_cent, weekend_price_cent,"
                + " holiday_price_cent, stock_required, status, validity_days, created_at, updated_at, bar_id)"
                + " VALUES (?, '酒吧入场票', 'flat', 5000, 5000, 5000, FALSE, 'published', 1, now(), now(), ?)", ticketTypeId, bar);
        jdbc.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status,"
                + " merchant_id, entitlement_kind, consumption_window, created_at, updated_at)"
                + " VALUES (?, '酒吧入场', 'entry', 1, 5000, 'active', ?, 'virtual', 'per_use', now(), now())", templateId, bar);
        jdbc.update("INSERT INTO ticket_type_entitlements (id, ticket_type_id, entitlement_template_id, quantity, total_times,"
                + " num_people, value_amount_cent, component_price_cent, created_at, updated_at)"
                + " VALUES (?, ?, ?, 1, 1, 1, 5000, 5000, now(), now())", "bind_tdv_" + suffix, ticketTypeId, templateId);
        com.complex.platform.support.TicketSalesContractTestFixtures.approvedPublished(jdbc, json, ticketTypeId, projectId, bar);

        TestSecurityActors.asPlatformAdmin();
        TicketOrderRecord order = orderService.createOrder(new TicketOrderRequest(
                "tdv-order-" + suffix, null, List.of(), ticketTypeId, null, 1, null, "admin", null, null, null, bar));
        assertThat(order.visitDate()).isEqualTo(local.toLocalDate().minusDays(1));
    }
}
