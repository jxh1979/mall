package com.complex.platform.terminal;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import java.sql.Timestamp;
import java.time.Instant;
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
 * 第十八批 AF 段第 198 条 198-R5（终端与设施）：契约删去 DeviceReturnRequest 的 operator_note、evidence_attachment_ids
 * （TerminalDeviceService.returnDevice／returnBoundDevice 不读）、安卓员工端「确认本班手机实际归还」（MainActivity.showWorkPhoneReturn）
 * 停发这两键后，按停发后的安卓形状——idempotency_key、device_code、has_pending_tasks=false、reason——走真实 HTTP、真实授权链归还在用的工作手机。
 * 请求头沿用 StaffRequestFixture 的后台形状（同一入口网页设备台账页也在调；设备签名不在本条范围）。入口未改，本类只锁现行为，不先红。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class WorkPhoneReturnShapeAfter198R5Tests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String digits = String.valueOf(System.nanoTime());

    // 设备接收人（本项目员工，持 terminal.device.bind）按安卓形状归还本项目员工在用的工作手机：200，设备待领用、归还说明落库，
    // 状态流水一条、持有员工记一次归还下班、本次领用以 shift_end 结束。
    @Test
    void returnWorkPhone_androidShapeWithoutOperatorNoteAndEvidence_returned() throws Exception {
        String project = "prj_b52ws_" + digits;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','归还形状综合体','active',now(),now())",
                project);
        StaffRequestFixture fixture = new StaffRequestFixture(jdbc);
        RequestPostProcessor receiver = fixture.staff("usr_b52ws_recv_" + digits, "staff", project, null, "terminal.device.bind");
        String holderUser = "usr_b52ws_holder_" + digits;
        fixture.staff(holderUser, "staff", project, null);
        String profile = "stf_b52ws_" + digits;
        jdbc.update("""
                INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, company_id,
                        registration_status, status, employment_status, created_at, updated_at)
                VALUES (?, ?, NULL, ?, '归还形状员工', ?, 'approved', 'active', 'employed', now(), now())
                """, profile, project, holderUser, project);
        String deviceRow = "td_b52ws_" + digits;
        String deviceId = "dev-b52ws-" + digits;
        String deviceCode = "DC-b52ws-" + digits;
        jdbc.update("""
                INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, staff_id, status,
                    bound_at, created_at, updated_at)
                VALUES (?, ?, ?, 'staff_work_phone', 'space', ?, 'bound', ?, now(), now())
                """, deviceRow, deviceId, deviceCode, profile, Timestamp.from(Instant.now().minusSeconds(7200)));
        String reason = "本班手机实物归还，收尾检查完成";

        mockMvc.perform(post("/api/v1/terminals/devices/return").with(receiver).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"work-phone-return-b52ws-" + digits + "\",\"device_code\":\"" + deviceCode
                                + "\",\"has_pending_tasks\":false,\"reason\":\"" + reason + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("returned"));

        assertThat(jdbc.queryForMap("SELECT status, return_reason FROM terminal_devices WHERE id = ?", deviceRow))
                .containsEntry("status", "returned")
                .containsEntry("return_reason", reason);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM terminal_device_status_events WHERE device_id = ?"
                + " AND event_type = 'terminal.device.returned' AND reason = ?", Integer.class, deviceRow, reason)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_records WHERE user_id = ? AND punch_type = 'check_out'"
                + " AND method = 'device_return'", Integer.class, holderUser)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT close_reason FROM staff_work_phone_contexts WHERE device_id = ? AND closed_at IS NOT NULL",
                String.class, deviceId)).isEqualTo("shift_end");
    }
}
