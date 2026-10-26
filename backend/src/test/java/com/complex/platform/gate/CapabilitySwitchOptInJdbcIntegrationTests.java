package com.complex.platform.gate;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.notification.NotificationSwitchResolver;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.ticketing.TicketingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;

/**
 * PGM 第 742 行「任何能力在后台没有显式开关配置行时一律默认关闭（opt-in），要启用必须有人显式打开；开关缺行不得视为开启」：
 * 默认值反转后，已发布在用的能力必须都有显式开启行（V554），否则现有业务被缺行拒绝；缺行即拒绝按客人端真实请求形状验证。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "access.device_credential.enforcement.app=enforce",
        "access.device_credential.enforcement.miniapp=enforce"
})
@AutoConfigureMockMvc
@Transactional
class CapabilitySwitchOptInJdbcIntegrationTests {

    @Autowired private ApplicationContext context;
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @Autowired private NotificationSwitchResolver smsSwitches;
    @Autowired private TicketingService ticketing;
    @Autowired private TerminalSwitchGuard guard;

    /** 全部 @GuardedAction 能力码都有跨端（'*'）全局显式行；缺的逐个列出。 */
    @Test
    void everyGuardedCapabilityHasExplicitGlobalRow() {
        Set<String> capabilities = new TreeSet<>();
        for (String name : context.getBeanDefinitionNames()) {
            Class<?> type = context.getType(name);
            if (type == null) {
                continue;
            }
            for (Method method : ClassUtils.getUserClass(type).getMethods()) {
                GuardedAction guarded = method.getAnnotation(GuardedAction.class);
                if (guarded != null && !guarded.capability().isBlank()) {
                    capabilities.add(guarded.capability());
                }
            }
        }
        Set<String> missing = new TreeSet<>(capabilities);
        missing.removeAll(jdbc.queryForList("SELECT capability_code FROM terminal_capability_switches"
                + " WHERE terminal_type = '*' AND scope_type = 'global' AND scope_id = 'global'", String.class));

        assertThat(capabilities).hasSizeGreaterThan(100);
        assertThat(missing).as("缺显式开关行的在用能力").isEmpty();
    }

    /** 客人端购票真实请求形状（蛇形键、X-Terminal-Type、X-Customer-Token）：有显式行照常下单；删掉该能力的全局行即 403，留痕 not_configured。 */
    @Test
    void customerTicketOrderRejectedOnceCapabilityRowIsMissing() throws Exception {
        CustomerTicketPaymentFixture fixture = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        fixture.createOrder(1, Map.of()).andExpect(status().isOk());

        Instant start = Instant.now();
        jdbc.update("DELETE FROM terminal_capability_switches WHERE terminal_type = '*' AND capability_code = 'ticket.order.create'"
                + " AND scope_type = 'global' AND scope_id = 'global'");
        fixture.createOrder(1, Map.of())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
        assertThat(jdbc.queryForList("SELECT metadata->>'reason' FROM audit_events WHERE event_name = 'terminal.access_rejected'"
                + " AND business_id = 'ticket.order.create' AND occurred_at >= ?", String.class, Timestamp.from(start)))
                .contains("not_configured");
    }

    /** 没有终端上下文的后台调用（线上支付结果定时补结确认停车缴费）按跨端 '*' 判所属综合体的总开关：有显式开启行，照常执行。 */
    @Test
    void headlessMasterCheckAtProjectPasses() {
        assertThatCode(() -> guard.checkTerminalEnabledAtProject("project_optin_headless")).doesNotThrowAnyException();
    }

    /** 在用但原先无行的短信域与事件已补显式开启行：总开关打开后门票转赠短信放行。 */
    @Test
    void seededSmsSwitchesAllowTicketTransferOnceMasterOn() {
        jdbc.update("UPDATE terminal_capability_switches SET enabled = TRUE WHERE capability_code = 'sms_notification'");

        assertThat(smsSwitches.resolve("ticketing", "ticket_transfer_created").allowed()).isTrue();
    }

    /** 渠道售票状态：没有 integration.ticket_channel.sync 全局行即三渠道都关。 */
    @Test
    void ticketChannelsDisabledWithoutSwitchRow() {
        jdbc.update("DELETE FROM terminal_capability_switches WHERE capability_code = 'integration.ticket_channel.sync'");

        assertThat(ticketing.listTicketChannels()).extracting(TicketingService.TicketChannelStatus::enabled)
                .containsExactly(false, false, false);
    }
}
