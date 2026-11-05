package com.complex.platform.finance;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.RoleRecord;
import com.complex.platform.common.LocalZone;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 R 段第 143 条 143-R3 日结五类数据逾期未标记自动发待办真库回归，直调 {@link FinanceFinalMarkOverdueService#scanFinalMarkOverdue}。
 * 依据：ENUM:190「最迟不晚于该营业日闭店时点所在自然日的次日 24:00」「某一类数据在上述时点仍未标记的，系统自动生成待办并通知该域主管公司的财务负责人，
 * 该营业日的日结不得确认锁定，直至标记完成」；第十八批裁决 35「酒吧逐店逐日查；游乐等按综合体逐日查；待办发同综合体有日结锁定权限的财务人员。」。
 *
 * <p>夹具随测试事务回滚：综合体 P（出厂营业日：起点 10:00、闭店次日 07:00）及其酒吧 X、综合体 Q；P 的平台侧财务 p 与 Q 的平台侧财务 q 持
 * finance.daily_close.lock，X 的商户账号 m 也持该码，P 的员工 n 不持该码，四人都有员工档案。共享库里别的酒吧与综合体也会被同一轮检查，
 * 断言只看本次夹具的单位与收件人。P、Q、X 建档于 30 天前；另有 P 里已存档（先下线）的酒吧 Y、今天才建档的酒吧 Z 与综合体 R
 * （R 的平台侧财务 r 持该码）：Y 不是在营酒吧（BSM:569「存档（先下线）」），Z、R 建档之前没有营业日，都不查、不发。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class FinanceFinalMarkOverdueJdbcIntegrationTests {

    private static final List<String> ALL = List.of("orders_verifications", "refunds_reversals", "production_handover",
            "attendance", "anomaly_resolutions");

    @Autowired private AccessRepository accessRepository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FinanceFinalMarkOverdueService overdueService;
    @Autowired private FinanceFinalMarkOverdueJob job;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String projectP = "prj_fmo_p_" + suffix;
    private final String projectQ = "prj_fmo_q_" + suffix;
    private final String barX = "mer_fmo_x_" + suffix;
    private final String barName = "逾期酒吧" + suffix;
    private final String projectName = "逾期综合体" + suffix;
    private final String barY = "mer_fmo_y_" + suffix;
    private final String archivedName = "存档酒吧" + suffix;
    private final String barZ = "mer_fmo_z_" + suffix;
    private final String newBarName = "新建酒吧" + suffix;
    private final String projectR = "prj_fmo_r_" + suffix;
    /** 第一轮的当前营业日 N；D＝N−3、D2＝N−2。 */
    private final LocalDate n = LocalDate.now(LocalZone.ID);
    private final LocalDate d = n.minusDays(3);
    private final LocalDate d2 = n.minusDays(2);

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now() - interval '30 days', now())",
                projectP, projectName);
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now() - interval '30 days', now())",
                projectQ, "范围外综合体" + suffix);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) "
                + "VALUES (?, ?, 'bar', ?, 'active', now() - interval '30 days', now())", barX, barName, projectP);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, archived_at, created_at, updated_at) "
                + "VALUES (?, ?, 'bar', ?, 'active', now(), now() - interval '30 days', now())", barY, archivedName, projectP);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) "
                + "VALUES (?, ?, 'bar', ?, 'active', now(), now())", barZ, newBarName, projectP);
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                projectR, "新建综合体" + suffix);
        String role = "role_fmo_" + suffix;
        accessRepository.createRole(new RoleRecord(role, "日结逾期待办回归" + suffix, null, "project", "active", Instant.now(),
                List.of("finance.daily_close.lock"), null, false, List.of()));
        staff("p", projectP, null, role);
        staff("q", projectQ, null, role);
        staff("m", projectP, barX, role);
        staff("n", projectP, null, null);
        staff("r", projectR, null, role);
        // X 在 D2 的酒吧日结五类已标齐；P 在 D 的综合体日结缺「当日考勤记录」；X 在 D、P 在 D2 都没生成日结。
        dailyClose("dc_fmo_x2_" + suffix, d2, "bar", barX, ALL);
        dailyClose("dc_fmo_p1_" + suffix, d, "project", projectP,
                List.of("orders_verifications", "refunds_reversals", "production_handover", "anomaly_resolutions"));
    }

    // 默认关闭的定时入口不发。第一轮（N 日 11:00，当前营业日 N）：D 的期限（闭店 N−2 日 07:00 所在自然日的次日 24:00＝N−1 日 24:00）已到，
    // D2 的期限（N 日 24:00）未到——X 在 D 没生成日结即列五类，P 在 D 的综合体日结只列缺的考勤；只发给持日结锁定权限、覆盖 P 的平台侧财务 p，
    // 商户账号、范围外与不持码的员工不收。p 把 X 的待办确认已读后第二、三轮（N+1 日 08:00，当前营业日仍为 N，D、D2 期限都已到）：
    // X 的 D 不重发且仍为已读，X 的 D2 五类已标齐不发，P 的 D2 没有日结即发。修复前作业不发（第一轮条数断言即红）。
    // 已存档的酒吧 Y、今天才建档的酒吧 Z 与综合体 R 三轮都不发。
    @Test
    void overdueUnmarkedCategoriesNotifyTheComplexFinanceStaffOncePerUnitAndBusinessDay() {
        job.scheduledScan();
        assertThat(notices("p")).isEmpty();

        overdueService.scanFinalMarkOverdue(at(n, 11));

        assertThat(notices("p", archivedName)).isEmpty();
        assertThat(notices("p", newBarName)).isEmpty();
        assertThat(notices("r")).isEmpty();
        assertThat(notices("p")).hasSize(2);
        assertThat(notices("p", barName)).singleElement().satisfies(row -> {
            assertThat(row.get("notice_type")).isEqualTo("system_notice");
            assertThat((String) row.get("title")).isEqualTo("五类数据逾期未标记：营业日 " + d + "、" + barName);
            assertThat((String) row.get("body")).contains("当日订单与核销记录", "当日退款与冲正记录", "当日出品与物品交接记录",
                    "当日考勤记录", "当日异常处置记录", "该营业日的日结不得确认锁定，直至标记完成");
        });
        assertThat(notices("p", projectName)).singleElement().satisfies(row -> {
            assertThat((String) row.get("title")).isEqualTo("五类数据逾期未标记：营业日 " + d + "、" + projectName);
            assertThat((String) row.get("body")).contains("当日考勤记录").doesNotContain("当日订单与核销记录");
        });
        for (String other : List.of("q", "m", "n")) {
            assertThat(notices(other, barName)).as(other).isEmpty();
            assertThat(notices(other, projectName)).as(other).isEmpty();
        }
        assertThat(jdbc.queryForMap("SELECT metadata->>'business_date' AS day, metadata->>'notified' AS notified FROM audit_events "
                + "WHERE event_name = 'finance.final_mark_overdue_notified' AND business_id = ?", barX))
                .containsEntry("day", d.toString()).containsEntry("notified", "1");

        jdbc.update("UPDATE employee_notices SET consent_status = 'acknowledged', consented_at = now() WHERE staff_profile_id = ? AND title LIKE ?",
                profile("p"), "%" + barName);
        overdueService.scanFinalMarkOverdue(at(n.plusDays(1), 8));
        overdueService.scanFinalMarkOverdue(at(n.plusDays(1), 8));

        assertThat(notices("p", barName)).singleElement().satisfies(row -> {
            assertThat((String) row.get("title")).contains("营业日 " + d);
            assertThat(row.get("consent_status")).isEqualTo("acknowledged");
        });
        assertThat(notices("p", projectName)).hasSize(2).last()
                .satisfies(row -> assertThat((String) row.get("title")).contains("营业日 " + d2));
        assertThat(notices("p", archivedName)).isEmpty();
        assertThat(notices("p", newBarName)).isEmpty();
        assertThat(notices("r")).isEmpty();
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    private static Instant at(LocalDate date, int hour) {
        return date.atTime(hour, 0).atZone(LocalZone.ID).toInstant();
    }

    private String profile(String tag) {
        return "stf_fmo_" + tag + "_" + suffix;
    }

    /** 在用账号（merchantId 非空即商户账号）＋已批准的员工档案；role 为空即不持日结锁定权限。 */
    private void staff(String tag, String projectId, String merchantId, String role) {
        String userId = "usr_fmo_" + tag + "_" + suffix;
        accessRepository.createUser(new AccessUser(userId, userId, "staff", projectId, merchantId, "active", Instant.now()));
        if (role != null) {
            accessRepository.assignUserRole(userId, role);
        }
        jdbc.update("""
                INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, registration_status, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'approved', 'active', now(), now())
                """, profile(tag), projectId, merchantId, userId, "财务 " + tag);
    }

    private void dailyClose(String id, LocalDate businessDate, String scopeType, String scopeId, List<String> marked) {
        jdbc.update("INSERT INTO finance_daily_closes (id, business_date, scope_type, scope_id, status, created_at) "
                + "VALUES (?, ?, ?, ?, 'pending_confirm', now())", id, businessDate, scopeType, scopeId);
        for (String category : marked) {
            jdbc.update("INSERT INTO finance_daily_close_final_marks (daily_close_id, category, marked_at) VALUES (?, ?, now())", id, category);
        }
    }

    private List<Map<String, Object>> notices(String tag) {
        return jdbc.queryForList("SELECT notice_type, title, body, consent_status FROM employee_notices WHERE staff_profile_id = ? "
                + "ORDER BY created_at, id", profile(tag));
    }

    private List<Map<String, Object>> notices(String tag, String unitName) {
        return jdbc.queryForList("SELECT notice_type, title, body, consent_status FROM employee_notices WHERE staff_profile_id = ? "
                + "AND title LIKE ? ORDER BY created_at, id", profile(tag), "%" + unitName);
    }
}
