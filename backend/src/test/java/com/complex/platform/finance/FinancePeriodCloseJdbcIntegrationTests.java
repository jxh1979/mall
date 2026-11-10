package com.complex.platform.finance;

// 文件版本：V0.01

import static com.complex.platform.support.DailyCloseFixture.closeWithMarks;
import static com.complex.platform.support.DailyCloseFixture.markRangeFinal;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
 * 第十八批 Z 段第 188 条 188-R1 封账真库回归（真实入口、真实授权链、蛇形键）。依据：BSM:1803「封账由财务部门执行，条件是账目已算清楚、经确认无误、四流一致。」；
 * PGM:664「跨主体与平台层面的财务工作——三方对账、分账复核、封账……由财务中心负责。」；ENUM:190「非最终快照的数据一律不得进入……对账关账」；
 * 第十八批裁决 41「财务中心按综合体＋月份在平台办理；只放行该月每天五类数据都已最终、日结都已锁定的月份；四流一致由财务核对后勾选确认留痕；
 * 封账不能撤销，不另加拦截」。
 *
 * <p>夹具随测试事务回滚（建档时间回填到 2026-01-01，会进检查单位集合）：综合体 P 与其下酒吧 A，月份 2026-02。P 的 project 日结、A 的 bar 日结
 * 按营业日直插并标五类（{@code DailyCloseFixture.markRangeFinal}），确认与锁定走真实日结接口。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class FinancePeriodCloseJdbcIntegrationTests {

    private static final LocalDate FROM = LocalDate.of(2026, 2, 1);
    private static final LocalDate TO = LocalDate.of(2026, 2, 28);
    private static final LocalDate D15 = LocalDate.of(2026, 2, 15);

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "prj_fpc_" + s;
    private final String projectName = "封账综合体" + s;
    private final String otherProject = "prj_fpc_q_" + s;
    private final String bar = "mer_fpc_a_" + s;
    private final String barName = "封账酒吧" + s;
    private final String financeUser = "usr_fpc_fin_" + s;
    private RequestPostProcessor finance;
    private StaffRequestFixture staffs;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', ?, 'active', TIMESTAMPTZ '2026-01-01 00:00:00+08', now())", project, projectName);
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', ?, 'active', TIMESTAMPTZ '2026-01-01 00:00:00+08', now())", otherProject, "封账别的综合体" + s);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at)"
                + " VALUES (?, ?, 'bar', ?, 'active', TIMESTAMPTZ '2026-01-01 00:00:00+08', now())", bar, barName, project);
        staffs = new StaffRequestFixture(jdbc);
        finance = staffs.staff(financeUser, "staff", project, null,
                "finance.period_close.execute", "finance.daily_close.lock", "finance.locked_backfill.request");
    }

    /** ①②③④⑤⑦⑧：缺一类 → 未最终；有一天未锁 → 未锁定；未勾四流一致 → 400；勾选 → 封账；再封 → 已封账；列表可查；封账后锁定后补录照常可申请。 */
    @Test
    void closesMonthOnlyWhenEveryDayFinalAndLockedAndFourFlowsConfirmed() throws Exception {
        markRangeFinal(jdbc, "project", project, FROM, TO);
        markRangeFinal(jdbc, "bar", bar, FROM, TO.minusDays(1));
        closeWithMarks(jdbc, "bar", bar, TO, List.of("orders_verifications", "refunds_reversals", "production_handover",
                "anomaly_resolutions"));
        String item = "fpc_item_" + s;
        jdbc.update("""
                INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,paid_amount_cent,
                    unpaid_amount_cent,currency,due_date,accounting_period,status,created_at,updated_at,merchant_id,project_id)
                VALUES(?,?,'receivable',?,'manual',?,100,0,100,'CNY',DATE '2026-02-10','2026-02','pending_collection',now(),now(),?,?)
                """, item, "FPC-" + item, bar, item, bar, project);

        close(finance, "c1", true).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PERIOD_CLOSE_DATA_NOT_FINAL"))
                .andExpect(jsonPath("$.message").value("以下营业日数据尚未标记为最终，不能封账：" + TO + " " + barName + "：当日考勤记录"));

        closeWithMarks(jdbc, "bar", bar, TO, List.of("attendance"));
        for (LocalDate day = FROM; !day.isAfter(TO); day = day.plusDays(1)) {
            lock("bar", bar, day);
            if (!day.equals(D15)) {
                lock("project", project, day);
            }
        }
        close(finance, "c2", true).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PERIOD_CLOSE_DAY_NOT_LOCKED"))
                .andExpect(jsonPath("$.message").value("以下营业日的日结尚未锁定，不能封账：" + D15 + " " + projectName));

        lock("project", project, D15);
        close(finance, "c3", false).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        close(finance, "c4", true).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.project_id").value(project))
                .andExpect(jsonPath("$.data.period_month").value("2026-02"))
                .andExpect(jsonPath("$.data.closed_by").value(financeUser))
                .andExpect(jsonPath("$.data.closed_at").isNotEmpty());
        close(finance, "c5", true).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PERIOD_ALREADY_CLOSED"));

        mockMvc.perform(get("/api/v1/finance/period-closes").param("project_id", project).with(finance))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].period_month").value("2026-02"))
                .andExpect(jsonPath("$.data.items[0].closed_by").value(financeUser));

        // ⑧ 封账不另加拦截：该月锁定台账的锁定后补录照常可申请。
        mockMvc.perform(post("/api/v1/finance/ar-ap-adjustments").with(finance).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "fpc-adj-" + s, "ar_ap_item_id", item,
                                "adjustment_amount_cent", 10, "reason", "封账后补录"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approval_status").value("pending"));
    }

    /** ⑥ 无权限 403；别的项目的财务 403 CROSS_PROJECT_FORBIDDEN；商户账号 403 CROSS_MERCHANT_FORBIDDEN。 */
    @Test
    void rejectsWithoutPermissionOtherProjectAndMerchantAccount() throws Exception {
        close(staffs.staff("usr_fpc_none_" + s, "staff", project, null, "finance.daily_close.lock"), "n1", true)
                .andExpect(status().isForbidden());
        close(staffs.staff("usr_fpc_q_" + s, "staff", otherProject, null, "finance.period_close.execute"), "q1", true)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        close(staffs.staff("usr_fpc_m_" + s, "staff", project, bar, "finance.period_close.execute"), "m1", true)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
    }

    private ResultActions close(RequestPostProcessor who, String key, boolean fourFlows) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "fpc-" + key + "-" + s);
        body.put("project_id", project);
        body.put("period_month", "2026-02");
        body.put("four_flows_confirmed", fourFlows);
        return mockMvc.perform(post("/api/v1/finance/period-closes").with(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    /** 已直插并标齐五类的日结经真实接口确认、锁定。 */
    private void lock(String scopeType, String scopeId, LocalDate day) throws Exception {
        String id = closeWithMarks(jdbc, scopeType, scopeId, day, List.of());
        for (String action : List.of("confirm", "lock")) {
            mockMvc.perform(post("/api/v1/finance/daily-closes/" + id + "/" + action).with(finance)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("idempotency_key", "fpc-" + action + "-" + id))))
                    .andExpect(status().isOk());
        }
    }
}
