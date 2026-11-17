package com.complex.platform.analytics;

// 文件版本：V0.01

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AP 段第 229 条 229-R10④（PGM:899「平台治理层要保证每个重要动作能追溯。……」）：后台人工新建风控预警不再带写死的规则编号
 * risk-rule-1 与对象编号 order-1——不带规则编号、对象编号照常建，两者都为空（按蛇形键走真实 HTTP、真实授权链）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class RiskAlertManualCreateJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String digits = String.valueOf(System.nanoTime());

    @Test
    void manualAlertWithoutRuleOrObjectIdIsCreatedWithBothEmpty() throws Exception {
        mockMvc.perform(post("/api/v1/analytics/risk-alerts")
                        .with(new StaffRequestFixture(jdbc).staff("usr_229_risk_" + digits, "platform_admin", null, null,
                                "analytics.risk_alert.manage"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key":"risk-alert-229-%s","alert_type":"staff_behavior","risk_level":"high",
                                 "object_type":"bar_order","summary":"员工连续给同桌打折"}
                                """.formatted(digits)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary").value("员工连续给同桌打折"))
                .andExpect(jsonPath("$.data.rule_id").doesNotExist())
                .andExpect(jsonPath("$.data.object_id").doesNotExist());
    }
}
