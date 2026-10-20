package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十批 TK-27（BSM L1058「待清台超过后台配置时限……未恢复空闲的，系统提醒当班主管」；操作员裁决 2026-10-20 第 6 条：
 * 发给本酒吧、此刻有排班、且持有 bar.table.open_close 的员工；每台位每次待清台只发一次）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class BarPendingClearAlertJobJdbcIntegrationTests {

    @Autowired private BarPendingClearAlertJob job;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String bar = "pcj_bar_" + suffix;
    private final String project = "pcj_prj_" + suffix;
    private final Instant now = Instant.now();

    @BeforeEach
    void seed() {
        String company = "pcj_be_" + suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','提醒综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'经营主体','management_company','active',now(),now())", company);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,created_at,updated_at) VALUES(?,'提醒酒吧','bar',?,?,?,?,'active',now(),now())",
                bar, company, company, company, project);
        table("old", now.minusSeconds(30 * 60));
        table("new", now.minusSeconds(60));
        staff("ok", true, true, "13900000001");
        staff("noshift", true, false, "13900000002");
        staff("noperm", false, true, "13900000003");
    }

    // 超时台位 → 只对「此刻有排班且持 bar.table.open_close」的员工发一条；无排班或无权限者不发；未超时台位不发。
    @Test
    void overdueTable_alertsOnDutyHoldersOnly() {
        job.alertOverdue(now);
        assertThat(recipients("old")).containsExactly("13900000001");
        assertThat(recipients("new")).as("未超时不发").isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='bar.table.pending_clear_alerted' AND business_id=?",
                Integer.class, tableId("old"))).as("留审计").isEqualTo(1);
    }

    // 重复轮询不重发（每台位每次待清台只发一次）。
    @Test
    void repeatedPolling_doesNotResend() {
        job.alertOverdue(now);
        job.alertOverdue(now.plusSeconds(60));
        assertThat(recipients("old")).containsExactly("13900000001");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='bar.table.pending_clear_alerted' AND business_id=?",
                Integer.class, tableId("old"))).isEqualTo(1);
    }

    private List<String> recipients(String tag) {
        return jdbc.queryForList("SELECT recipient FROM notification_send_log WHERE domain='bar' AND event_key='pending_clear_overdue' "
                + "AND params->>'table_id' = ? ORDER BY recipient", String.class, tableId(tag));
    }

    private String tableId(String tag) {
        return "pcj_tbl_" + tag + "_" + suffix;
    }

    private void table(String tag, Instant since) {
        jdbc.update("INSERT INTO bar_tables(id,bar_id,merchant_id,table_code,table_name,table_type,status,created_at,updated_at,pending_clear_since) VALUES(?,?,?,?,?,'booth','pending_clear',now(),?,?)",
                tableId(tag), bar, bar, "T-" + tag, "台" + tag, Timestamp.from(since), Timestamp.from(since));
    }

    private void staff(String tag, boolean withPermission, boolean onShift, String phone) {
        String user = "pcj_u_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",
                user, user, project, bar);
        jdbc.update("INSERT INTO staff_profiles(id,project_id,merchant_id,user_id,name,phone,registration_status,status,created_at,updated_at) VALUES(?,?,?,?,?,?,'approved','active',now(),now())",
                "pcj_sp_" + tag + "_" + suffix, project, bar, user, "员工" + tag, phone);
        if (withPermission) {
            String role = "role_" + user;
            jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'待清台提醒回归','project','active',now())", role, role);
            jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'bar.table.open_close')", role);
            jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", user, role);
        }
        if (onShift) {
            jdbc.update("INSERT INTO staff_shift_schedules(id,user_id,work_date,start_at,end_at,project_id) VALUES(?,?,?,?,?,?)",
                    "pcj_sh_" + tag + "_" + suffix, user, LocalDate.now(), Timestamp.from(now.minusSeconds(7200)),
                    Timestamp.from(now.plusSeconds(7200)), project);
        }
    }
}
