package com.complex.platform.access.staff;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.TemporaryGrant;
import com.complex.platform.access.staff.dto.StaffEmploymentStatusRequest;
import com.complex.platform.governance.review.SupervisorReviewApprovalRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第 96 条 96-R2（ENUM:241「设备状态」「待领用、在用、待归还、维修中、挂失、报废、归档」；ENUM:240「停岗、离职生效后的撤权、未结事项交接及
 * 设备与工资后续办理，按《平台治理地图》“员工和手机”中的停岗、离职规则执行。」；PGM:581「……设备交接、工资核算及依法结清分别继续办理，
 * 不因交接未完保留业务操作权限。」；第十八批裁决 23「停岗离职转待归还」）：真实库、真实接口与真实授权链回归，请求体按后台前端的蛇形键。
 * 员工停岗或离职生效时，名下还在用的工作手机、扫码设备自动转待归还并留痕；按点位与责任人管的其他现场设备不转；待归还的设备不能领用；
 * 设备管理员收回实物后在后台办归还回待领用，不给该员工记下班；收不回的在后台挂失。
 * 修复前员工生效后设备仍在用（第一处 pending_return 的断言即红）。
 *
 * <p>真实 PostgreSQL，@Transactional 回滚。复核链照 HrApprovalChainJdbcIntegrationTests：员工 E → S1 → S2 → S3 → S4，HR 持 staff.hr.review，
 * S1 发起、S2、S3、HR 依次通过；生效时点定在一小时后，到点前设备不动，改到点后由员工状态生效作业落地。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=${COMPLEX_DATABASE_USERNAME:complex}",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class DevicePendingReturnJdbcIntegrationTests {

    private static final String DEVICES = "/api/v1/terminals/devices";

    @Autowired private MockMvc mockMvc;
    @Autowired private StaffService service;
    @Autowired private SupervisorReviewService reviews;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AccessRepository access;
    @Autowired private StaffEmploymentStatusJob employmentJob;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String employee = "usr_d96_e_" + suffix;
    private final String initiator = "usr_d96_i_" + suffix;
    private final String hr = "usr_d96_hr_" + suffix;
    private final List<String> superiors = List.of("usr_d96_s1_" + suffix, "usr_d96_s2_" + suffix, "usr_d96_s3_" + suffix,
            "usr_d96_s4_" + suffix);
    // 设备管理员：项目员工（不带商户归属），持领用归还与设备治理权限
    private final String manager = "usr_d96_m_" + suffix;
    private final String profileId = "stf_d96_" + suffix;
    private final String otherProfile = "stf_d96_o_" + suffix;
    private final String phone = "td_d96_ph_" + suffix;
    private final String scanner = "td_d96_sc_" + suffix;
    private final String otherDevice = "td_d96_os_" + suffix;
    private final String context = "phone_d96_" + suffix;

    @BeforeEach
    void setUp() {
        for (String user : List.of(employee, initiator, hr)) {
            StaffTestActors.grant(jdbc, access, user, "project_main");
        }
        for (String user : superiors) {
            StaffTestActors.grant(jdbc, access, user, "project_main");
        }
        String below = employee;
        for (String superior : superiors) {
            jdbc.update("INSERT INTO org_hierarchy (user_id, superior_id, line_code, updated_by) VALUES (?, ?, 'main', ?)",
                    below, superior, initiator);
            below = superior;
        }
        jdbc.update("INSERT INTO approval_lines(line_code,line_name,min_start_rank,status,hr_final_approver_id) VALUES (?,?,1,'active',?)",
                "dept_" + suffix, "设备待归还测试部门", hr);
        jdbc.update("UPDATE org_hierarchy SET line_code=? WHERE user_id=?", "dept_" + suffix, employee);
        jdbc.update("""
                INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, company_id,
                        registration_status, status, employment_status, created_at, updated_at)
                VALUES (?, 'project_main', NULL, ?, '离职员工', 'project_main', 'approved', 'active', 'employed', now(), now())
                """, profileId, employee);
        jdbc.update("""
                INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, company_id,
                        registration_status, status, employment_status, created_at, updated_at)
                VALUES (?, 'project_main', NULL, ?, '接手员工', 'project_main', 'approved', 'active', 'employed', now(), now())
                """, otherProfile, "usr_" + otherProfile);
        Instant now = Instant.now();
        access.createUser(new AccessUser(manager, manager, "staff", "project_main", null, "active", now));
        access.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), manager,
                List.of("terminal.device.bind", "terminal.device.lifecycle"), now.minusSeconds(3600), now.plusSeconds(86_400),
                false, false, "active", "第 96 条设备待归还回归", "seed-admin", now, null, null));
        // 员工名下在用的工作手机（带一条未结束的领用上下文）、扫码设备，以及以该员工为责任人的其他现场设备
        Timestamp boundAt = Timestamp.from(now.minusSeconds(7200));
        device(phone, "PH", "staff_work_phone", boundAt);
        device(scanner, "SC", "scanner", boundAt);
        device(otherDevice, "OS", "other_site_device", boundAt);
        jdbc.update("""
                INSERT INTO staff_work_phone_contexts(id, device_id, binding_at, staff_profile_id, user_id, created_at)
                VALUES (?, ?, ?, ?, ?, now())
                """, context, "dev-d96-PH-" + suffix, boundAt, profileId, employee);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // 离职生效：工作手机、扫码设备转待归还（各一行状态流水与成功留痕），其他现场设备仍在用；待归还的手机不能领给别人（409）；
    // 设备管理员收回手机办归还（不带收尾检查确认）→ 待领用，不给离职员工记下班、领用上下文以 employment_ended 结束；扫码设备收不回挂失。
    @Test
    void departedEmployeesHeldDevicesTurnPendingReturnAndAreTakenBackWithoutCheckout() throws Exception {
        applyEmploymentStatus("departed", "离职生效");
        assertThat(deviceStatus(phone)).isEqualTo("pending_return");
        assertThat(deviceStatus(scanner)).isEqualTo("pending_return");
        assertThat(deviceStatus(otherDevice)).isEqualTo("bound");
        for (String id : List.of(phone, scanner)) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM terminal_device_status_events WHERE device_id = ? "
                    + "AND event_type = 'terminal.device.pending_return' AND status = 'pending_return'", Integer.class, id))
                    .isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'terminal.device.pending_return' "
                    + "AND event_result = 'success' AND business_id = ?", Integer.class, id)).isEqualTo(1);
        }

        send(DEVICES + "/bind", """
                {"idempotency_key": "%s", "device_id": "%s", "terminal_type": "staff_work_phone", "binding_scope": "space",
                 "staff_id": "%s"}
                """.formatted(key(), "dev-d96-PH-" + suffix, otherProfile))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("INVALID_DEVICE_STATUS"));

        send(DEVICES + "/return", """
                {"idempotency_key": "%s", "device_code": "%s", "reason": "离职交回"}
                """.formatted(key(), "PH-d96-" + suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("returned"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_attendance_records WHERE user_id = ? AND punch_type = 'check_out'",
                Integer.class, employee)).isZero();
        assertThat(jdbc.queryForObject("SELECT close_reason FROM staff_work_phone_contexts WHERE id = ? AND closed_at IS NOT NULL",
                String.class, context)).isEqualTo("employment_ended");

        send(DEVICES + "/" + scanner + "/lost", """
                {"idempotency_key": "%s", "reason": "离职未交回"}
                """.formatted(key()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("lost"));
    }

    // 停岗生效同样转待归还。
    @Test
    void suspendedEmployeesHeldDevicesTurnPendingReturn() throws Exception {
        applyEmploymentStatus("suspended", "停岗调查");
        assertThat(deviceStatus(phone)).isEqualTo("pending_return");
        assertThat(deviceStatus(scanner)).isEqualTo("pending_return");
        assertThat(deviceStatus(otherDevice)).isEqualTo("bound");
    }

    // S1 发起、S2、S3、HR 通过；生效时点在一小时后 → 设备不动；改到点后跑员工状态生效作业。
    private void applyEmploymentStatus(String target, String reason) {
        actAs(superiors.get(0));
        var review = service.setEmploymentStatus(profileId,
                new StaffEmploymentStatusRequest(key(), target, reason, Instant.now().plusSeconds(3600)));
        for (String approver : List.of(superiors.get(1), superiors.get(2), hr)) {
            actAs(approver);
            reviews.approve(review.reviewId(), new SupervisorReviewApprovalRequest(key(), "复核", List.of(), null, "approved", "已核"));
        }
        assertThat(deviceStatus(phone)).as("未到生效时点设备不动").isEqualTo("bound");
        jdbc.update("UPDATE staff_employment_status_revisions SET effective_at=now()-interval '1 second' WHERE review_id=?",
                review.reviewId());
        actAs(hr);
        employmentJob.runOnce(Instant.now());
        assertThat(jdbc.queryForObject("SELECT employment_status FROM staff_profiles WHERE id = ?", String.class, profileId))
                .isEqualTo(target);
    }

    private void device(String id, String code, String type, Timestamp boundAt) {
        jdbc.update("""
                INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, staff_id, status,
                    bound_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'space', ?, 'bound', ?, now(), now())
                """, id, "dev-d96-" + code + "-" + suffix, code + "-d96-" + suffix, type, profileId, boundAt);
    }

    private String deviceStatus(String id) {
        return jdbc.queryForObject("SELECT status FROM terminal_devices WHERE id = ?", String.class, id);
    }

    private void actAs(String userId) {
        StaffTestActors.actor(userId, "staff", "project_main", null);
        StaffTestActors.alignRowToSession(jdbc);
    }

    // 后台 Web 终端 + 生产过滤器放入的 AccessActor 主体（设备管理员）。
    private ResultActions send(String url, String body) throws Exception {
        return mockMvc.perform(post(url).with(request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(
                    new AccessActor(manager, manager, "staff", "project_main", null, "active"), null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        }).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String key() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
