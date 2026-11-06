package com.complex.platform.finance;

// 文件版本：V0.01

import static com.complex.platform.support.DailyCloseFixture.closeWithMarks;
import static com.complex.platform.support.DailyCloseFixture.markRangeFinal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
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
 * 第十八批 V 段第 159 条 159-R1 最终快照判定助手真库回归（{@link FinanceService#nonFinalDays}、{@link FinanceService#nonFinalDaysAllUnits}、
 * {@link FinanceService#describeNonFinalDays}）。依据：ENUM:190「非最终快照的数据一律不得进入结算、分红、正式提成计算和对账关账」「「最终快照」指某一营业日的
 * 某一类数据在该营业日结束后不再变动的状态。」；第十八批裁决 35「酒吧逐店逐日查；游乐等按综合体逐日查」；18R 143-R3 复核修复补记「检查单位只取未存档的酒吧……
 * 酒吧与综合体都跳过建档之前的营业日」。
 *
 * <p>夹具随测试事务回滚：综合体 P 建档 2026-06-01；P 下酒吧 X 建档 2026-07-10T02:30Z（北京 10:30，7-09 营业日在 7-10 10:00 结束，不查）、
 * 餐饮商户 C、已存档酒吧 Y。7-10 X 的 bar、merchant 两张日结各标一部分（并集五类齐）；7-11 X 的 bar 日结经真实生成、标记接口（蛇形键）由持
 * finance.daily_close.lock、负责范围覆盖 P 的平台侧财务 BP 生成并标除退款与冲正外四类；7-12 X 没有日结。P 的 project 日结 7-09、7-10、7-12 五类齐，
 * 7-11 缺订单与核销。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class FinanceFinalSnapshotJdbcIntegrationTests {

    private static final List<String> TWO = List.of("orders_verifications", "refunds_reversals");
    private static final LocalDate D9 = LocalDate.of(2026, 7, 9);
    private static final LocalDate D10 = LocalDate.of(2026, 7, 10);
    private static final LocalDate D11 = LocalDate.of(2026, 7, 11);
    private static final LocalDate D12 = LocalDate.of(2026, 7, 12);

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mockMvc;
    @Autowired private FinanceService finance;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "prj_ffs_" + s;
    private final String projectName = "快照综合体" + s;
    private final String barX = "mer_ffs_x_" + s;
    private final String barName = "快照酒吧" + s;
    private final String shopC = "mer_ffs_c_" + s;
    private final String barY = "mer_ffs_y_" + s;

    // 修复前没有助手（桩返回空）：第一条断言即红。
    @Test
    void listsNonFinalBusinessDaysPerBarOrComplexSkippingDaysBeforeOnboarding() throws Exception {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', ?, 'active', TIMESTAMPTZ '2026-06-01 00:00:00+08', now())", project, projectName);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at)"
                + " VALUES (?, ?, 'bar', ?, 'active', TIMESTAMPTZ '2026-07-10 02:30:00+00', now())", barX, barName, project);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at)"
                + " VALUES (?, ?, 'catering', ?, 'active', TIMESTAMPTZ '2026-06-01 00:00:00+08', now())", shopC, "快照餐饮" + s, project);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, archived_at, created_at, updated_at)"
                + " VALUES (?, ?, 'bar', ?, 'active', now(), TIMESTAMPTZ '2026-06-01 00:00:00+08', now())", barY, "快照存档酒吧" + s, project);

        String close10 = closeWithMarks(jdbc, "bar", barX, D10, List.of("orders_verifications", "production_handover", "attendance"));
        closeWithMarks(jdbc, "merchant", barX, D10, List.of("refunds_reversals", "anomaly_resolutions"));
        assertThat(closeWithMarks(jdbc, "bar", barX, D10, List.of())).isEqualTo(close10);

        RequestPostProcessor financeBp = new StaffRequestFixture(jdbc).staff("usr_ffs_bp_" + s, "staff", project, null,
                "finance.daily_close.lock");
        String close11 = JsonPath.read(mockMvc.perform(post("/api/v1/finance/daily-closes").with(financeBp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"ffs-gen-" + s + "\",\"business_date\":\"" + D11
                                + "\",\"scope_type\":\"bar\",\"scope_id\":\"" + barX + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
        for (String category : List.of("orders_verifications", "production_handover", "attendance", "anomaly_resolutions")) {
            finalMark(financeBp, close11, category, "ffs-" + category + "-" + s);
        }

        markRangeFinal(jdbc, "project", project, D9, D10);
        closeWithMarks(jdbc, "project", project, D11, List.of("refunds_reversals", "production_handover", "attendance",
                "anomaly_resolutions"));
        markRangeFinal(jdbc, "project", project, D12, D12);

        String x11 = D11 + " " + barName + "：当日退款与冲正记录";
        String x12 = D12 + " " + barName + "：当日订单与核销记录、当日退款与冲正记录";
        String p11 = D11 + " " + projectName + "：当日订单与核销记录";
        assertThat(finance.nonFinalDays(barX, null, D9, D12, TWO)).containsExactly(x11, x12);
        assertThat(finance.nonFinalDays(barX, null, D9, D12, List.of("orders_verifications")))
                .containsExactly(D12 + " " + barName + "：当日订单与核销记录");
        assertThat(finance.nonFinalDays(shopC, null, D9, D12, TWO)).containsExactly(p11);
        assertThat(finance.nonFinalDays(null, project, D9, D12, TWO)).containsExactly(p11);
        assertThat(finance.nonFinalDays(barY, null, D9, D12, TWO)).isEmpty();
        assertThat(finance.nonFinalDays("mer_ffs_none_" + s, null, D9, D12, TWO)).isEmpty();
        assertThat(finance.nonFinalDaysAllUnits(project, D10, D12, FinanceService.FINAL_MARK_CATEGORIES.keySet())).containsExactly(
                x11,
                D12 + " " + barName + "：当日订单与核销记录、当日退款与冲正记录、当日出品与物品交接记录、当日考勤记录、当日异常处置记录",
                p11);

        List<String> twelve = IntStream.rangeClosed(1, 12).mapToObj(i -> "第" + i + "行").toList();
        assertThat(FinanceService.describeNonFinalDays(twelve))
                .isEqualTo(String.join("；", twelve.subList(0, 10)) + "等，共 12 条");

        finalMark(financeBp, close11, "refunds_reversals", "ffs-refunds-" + s);
        assertThat(finance.nonFinalDays(barX, null, D9, D12, TWO)).containsExactly(x12);
    }

    /** 143-R2 五类最终标记的真实请求形状（蛇形键）。 */
    private void finalMark(RequestPostProcessor who, String dailyCloseId, String category, String key) throws Exception {
        mockMvc.perform(post("/api/v1/finance/daily-closes/" + dailyCloseId + "/final-marks").with(who)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"" + key + "\",\"category\":\"" + category + "\"}"))
                .andExpect(status().isOk());
    }
}
