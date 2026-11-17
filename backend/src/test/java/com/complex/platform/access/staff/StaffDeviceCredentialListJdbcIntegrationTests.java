// 文件版本：V0.01
package com.complex.platform.access.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.amusement.AmusementBusinessDayTestFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
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
 * 第十八批 AN 段第 257 条（PGM:585「员工换手机必须先解绑旧手机，再绑定新手机。」；PGM:906「员工注册审批、设备绑定、换机解绑重绑。」；
 * BSM:938「私人手机 App 可收发通知、查看状态和审批结果……」；第十八批裁决 70 选「批准时先废旧再生效」）：
 * 257-R1 后台读员工私人手机凭证列表（持 staff.device.review 或 staff.device.revoke 任一，按所持权限可触及范围列生效中与待批准换机）；
 * 257-R2 批准换机后给员工发审批结果通知，解绑记原因。真库、真实授权链，请求按后台网页蛇形键。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class StaffDeviceCredentialListJdbcIntegrationTests {

    private static final String BASE = "/api/v1/staff/device-credentials";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcStaffRepository repository;

    private final ObjectMapper json = new ObjectMapper();
    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    private String project;
    private String otherProject;
    private String employee;
    private String profile;
    private StaffRequestFixture staffs;

    @BeforeEach
    void seed() {
        project = AmusementBusinessDayTestFixture.complex(jdbc);
        otherProject = AmusementBusinessDayTestFixture.complex(jdbc);
        staffs = new StaffRequestFixture(jdbc);
        employee = "sdc_emp_" + suffix;
        profile = "sdc_prof_" + suffix;
        staffs.staff(employee, "staff", project, null, "staff.device.bind");
        jdbc.update("""
                INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, company_id,
                        registration_status, status, created_at, updated_at)
                VALUES (?, ?, NULL, ?, '换机回归员工', ?, 'approved', 'active', now(), now())
                """, profile, project, employee, project);
    }

    /** 257-R1①～⑥ 同时持两码、只持其一都看得到本公司员工的生效中与待批准换机；别的组织看不到；两码都没有 403；已作废、已解绑的不列。 */
    @Test
    void listShowsActiveAndPendingWithinReachOfEitherPermission() throws Exception {
        Instant now = Instant.now();
        String active = credential("active", null, now);
        String pending = credential("pending_review", active, now);
        // ⑥ 已作废、已解绑的各落一条：下面每个账号拿到的都恰好是生效中与待批准换机两条（按 Map 整体相等判）
        credential("superseded", null, now);
        credential("revoked", null, now);
        Map<String, String> expected = Map.of(active, "active", pending, "pending_review");

        assertThat(ownCredentials(staffs.staff("sdc_both_" + suffix, "staff", project, null,
                "staff.device.review", "staff.device.revoke"))).isEqualTo(expected);
        assertThat(ownCredentials(staffs.staff("sdc_review_" + suffix, "staff", project, null,
                "staff.device.review"))).isEqualTo(expected);
        assertThat(ownCredentials(staffs.staff("sdc_revoke_" + suffix, "staff", project, null,
                "staff.device.revoke"))).isEqualTo(expected);
        assertThat(ownCredentials(staffs.staff("sdc_other_" + suffix, "staff", otherProject, null,
                "staff.device.review", "staff.device.revoke"))).isEmpty();
        mockMvc.perform(get(BASE).with(staffs.staff("sdc_none_" + suffix, "staff", project, null, "staff.profile.view")))
                .andExpect(status().isForbidden());
    }

    /** 257-R2 员工在新手机发起换机 → 后台批准：旧手机作废、新手机生效、员工收到审批结果通知并留痕；再解绑生效手机记原因，列表不再出现。 */
    @Test
    void approveRebindNotifiesEmployeeAndRevokeKeepsReason() throws Exception {
        RequestPostProcessor phone = staffs.staff(employee, "staff", project, null, "staff.device.bind");
        String first = bind(phone, "fp-first-" + suffix).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bind_status").value("active")).andReturn().getResponse().getContentAsString();
        String firstId = json.readTree(first).get("data").get("id").asText();
        String second = bind(phone, "fp-second-" + suffix).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bind_status").value("pending_review"))
                .andExpect(jsonPath("$.data.supersedes_credential_id").value(firstId)).andReturn().getResponse().getContentAsString();
        String secondId = json.readTree(second).get("data").get("id").asText();

        RequestPostProcessor admin = staffs.staff("sdc_admin_" + suffix, "staff", project, null,
                "staff.device.review", "staff.device.revoke");
        String approveKey = key();
        send(admin, BASE + "/" + secondId + "/approve-rebind", "{\"idempotency_key\":\"" + approveKey + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bind_status").value("active"));
        assertThat(bindStatus(firstId)).isEqualTo("superseded");
        assertThat(bindStatus(secondId)).isEqualTo("active");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM employee_notices WHERE staff_profile_id = ?"
                + " AND notice_type = 'approval_result' AND title = '换机已批准'", Integer.class, profile)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'staff_device.rebound'"
                + " AND idempotency_key = ?", Integer.class, approveKey)).isEqualTo(1);

        send(admin, BASE + "/" + secondId + "/revoke", "{\"idempotency_key\":\"" + key() + "\",\"reason\":\"员工手机丢失\"}")
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT bind_status || '|' || revoked_reason FROM staff_device_credentials WHERE id = ?",
                String.class, secondId)).isEqualTo("revoked|员工手机丢失");
        assertThat(ownCredentials(admin)).isEmpty();
    }

    /** GET 列表里属于本用例员工的凭证：编号 → bind_status。 */
    private Map<String, String> ownCredentials(RequestPostProcessor who) throws Exception {
        String body = mockMvc.perform(get(BASE).with(who)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<String, String> result = new LinkedHashMap<>();
        for (JsonNode item : json.readTree(body).get("data").get("items")) {
            if (profile.equals(item.get("staff_profile_id").asText())) {
                result.put(item.get("id").asText(), item.get("bind_status").asText());
            }
        }
        return result;
    }

    private String credential(String bindStatus, String supersedes, Instant now) {
        String id = "scred_sdc_" + UUID.randomUUID();
        repository.saveCredential(new StaffDeviceCredentialRecord(id, profile, "pk-" + id, "fp-" + id, bindStatus, supersedes,
                "trusted", "revoked".equals(bindStatus) ? "manual_revoke" : null, now,
                "revoked".equals(bindStatus) ? now : null, now, now));
        return id;
    }

    private ResultActions bind(RequestPostProcessor who, String fingerprint) throws Exception {
        return send(who, BASE, "{\"idempotency_key\":\"" + key() + "\",\"staff_profile_id\":\"" + profile
                + "\",\"device_public_key\":\"pk-" + fingerprint + "\",\"device_fingerprint_hash\":\"" + fingerprint + "\"}");
    }

    private ResultActions send(RequestPostProcessor who, String path, String body) throws Exception {
        return mockMvc.perform(post(path).with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String bindStatus(String id) {
        return jdbc.queryForObject("SELECT bind_status FROM staff_device_credentials WHERE id = ?", String.class, id);
    }

    private static String key() {
        return "sdc-" + UUID.randomUUID();
    }
}
