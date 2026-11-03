package com.complex.platform.amusement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 N 段第 120 条 120-R5（BSM:991「查看游乐设施状态：开放、排队、容量、暂停、关闭、故障、维护或安全提醒。」；BSM:1268「通过触摸屏一体机
 * 展示游乐设施开放、排队、容量、暂停、故障和维护状态。」；BSM:1002「……展示游乐设施开放状态、容量和异常，并形成状态检查……记录。」；BSM:1004「……一体机的
 * 下单、领取、状态检查和异常提示都要记录设备、点位和时间。」）：一体机游乐设施状态看板——每个可见项目一行：开放状态、容量、在玩人数、排队人数、维护设备数、
 * 未结故障数与生效安全提醒标题，不出客人编号与排队号；读取留痕记一体机设备号、终端与绑定点位。请求按一体机真实形状（一体机终端头、登记设备号）走真实入口与授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class AmusementStatusBoardJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String complex = "prj_asb_" + s;
    private final String merchant = "mch_asb_" + s;
    private final String otherMerchant = "mch_asb_o_" + s;
    private final String space = "space_asb_" + s;
    private final String kioskDevice = "asb-kiosk-" + s;
    private final String p = "amp_asb_p_" + s;
    private final String q = "amp_asb_q_" + s;
    private RequestPostProcessor admin;
    private RequestPostProcessor staff;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                complex, complex);
        for (String m : List.of(merchant, otherMerchant)) {
            jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                    + " VALUES (?, '游乐商户', 'amusement', 'active', now(), now(), ?)", m, complex);
        }
        jdbc.update("INSERT INTO spaces (id, project_id, space_name, space_type, status, created_at, updated_at)"
                + " VALUES (?, ?, '游乐服务点', 'shop', 'active', now(), now())", space, complex);
        jdbc.update("INSERT INTO amusement_projects (id, project_name, capacity, open_status, merchant_id, created_at, updated_at)"
                + " VALUES (?, '旋转木马', 30, 'open', ?, now(), now())", p, merchant);
        jdbc.update("INSERT INTO amusement_projects (id, project_name, capacity, open_status, merchant_id, created_at, updated_at)"
                + " VALUES (?, '他家过山车', 20, 'open', ?, now(), now())", q, otherMerchant);
        jdbc.update("INSERT INTO amusement_capacity_snapshot (id, project_id, dimension, capacity_limit, occupied_count, updated_at)"
                + " VALUES (?, ?, 'project', 30, 12, now())", "snap_asb_" + s, p);
        String session = "ses_asb_" + s;
        jdbc.update("INSERT INTO amusement_sessions (id, project_id, start_time, end_time, capacity, used_capacity, status, created_at, updated_at, complex_id)"
                + " VALUES (?, ?, now(), now() + interval '1 hour', 30, 0, 'open', now(), now(), ?)", session, p, complex);
        int n = 0;
        for (String st : List.of("waiting", "waiting", "waiting", "called")) {
            n++;
            jdbc.update("INSERT INTO amusement_queue_entries (id, project_id, session_id, customer_id, queue_no, status, created_at, updated_at)"
                    + " VALUES (?, ?, ?, ?, ?, ?, now(), now())", "aq_asb_" + n + "_" + s, p, session, "cust_asb_" + n + "_" + s, "Q" + n, st);
        }
        for (String st : List.of("maintenance", "inspection_failed", "operational")) {
            jdbc.update("INSERT INTO amusement_device_statuses (id, project_id, device_id, provider_code, status, last_seen_at, created_at, updated_at)"
                    + " VALUES (?, ?, ?, 'manual', ?, now(), now(), now())", "ads_asb_" + st + "_" + s, p, "dev_asb_" + st + "_" + s, st);
        }
        incident("fault", "processing");
        incident("fault", "closed");
        incident("accident", "created");
        jdbc.update("INSERT INTO amusement_safety_notices (id, project_id, amusement_project_id, notice_type, title, content_version, content_text,"
                + " status, effective_from, effective_to, created_by_user_id, created_at, updated_at, merchant_id)"
                + " VALUES (?, ?, ?, 'safety_notice', '身高 1.2 米以下须家长陪同', 'v1', '安全提醒', 'active', now() - interval '1 day',"
                + " now() + interval '1 day', 'usr_asb', now(), now(), ?)", "asn_live_" + s, complex, p, merchant);
        jdbc.update("INSERT INTO amusement_safety_notices (id, project_id, amusement_project_id, notice_type, title, content_version, content_text,"
                + " status, effective_from, effective_to, created_by_user_id, created_at, updated_at, merchant_id)"
                + " VALUES (?, ?, ?, 'safety_notice', '已过期的提醒', 'v1', '过期', 'active', now() - interval '3 day',"
                + " now() - interval '1 day', 'usr_asb', now(), now(), ?)", "asn_old_" + s, complex, p, merchant);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        admin = staffs.staff("usr_asb_admin_" + s, "platform_admin", complex, null, "terminal.device.bind", "terminal.device.lifecycle");
        staff = kiosk(staffs.staff("usr_asb_staff_" + s, "staff", complex, merchant, "amusement.session.view"));
    }

    @Test
    void kioskStatusBoardShowsOpenQueueCapacityMaintenanceFaultAndSafetyNoticeAndRecordsDeviceAndPoint() throws Exception {
        // 管理员按后台真实请求把设备绑定为一体机（按空间绑点位）。
        mockMvc.perform(post("/api/v1/terminals/devices/bind").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "idempotency_key", "asb-bind-" + s, "device_id", kioskDevice, "terminal_type", "kiosk",
                                "binding_scope", "space", "bound_space_id", space, "kiosk_mode", "service"))))
                .andExpect(status().isOk());

        String body = mockMvc.perform(get("/api/v1/amusement/projects/status-board").with(staff))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        List<JsonNode> rows = new ArrayList<>();
        json.readTree(body).path("data").path("items").forEach(rows::add);
        assertThat(rows).extracting(row -> row.path("project_id").asText()).contains(p).doesNotContain(q);
        JsonNode row = rows.stream().filter(r -> p.equals(r.path("project_id").asText())).findFirst().orElseThrow();
        assertThat(row.path("project_name").asText()).isEqualTo("旋转木马");
        assertThat(row.path("open_status").asText()).isEqualTo("open");
        assertThat(row.path("capacity").asInt()).isEqualTo(30);
        assertThat(row.path("occupied_count").asInt()).isEqualTo(12);
        assertThat(row.path("waiting_count").asInt()).isEqualTo(3);
        assertThat(row.path("maintenance_device_count").asInt()).isEqualTo(1);
        assertThat(row.path("fault_count").asInt()).isEqualTo(1);
        assertThat(row.path("safety_notices")).hasSize(1);
        assertThat(row.path("safety_notices").get(0).asText()).isEqualTo("身高 1.2 米以下须家长陪同");
        assertThat(body).doesNotContain("customer_id").doesNotContain("queue_no").doesNotContain("cust_asb_");

        // 状态检查留痕：设备、点位、时间（时间即留痕时间）。
        assertThat(jdbc.queryForMap("SELECT metadata->>'device_id' AS device_id, metadata->>'terminal_type' AS terminal_type,"
                        + " metadata->>'point_id' AS point_id FROM audit_events WHERE event_name = 'amusement.status_board.viewed'"
                        + " AND actor_id = ? ORDER BY occurred_at DESC LIMIT 1", "usr_asb_staff_" + s))
                .containsEntry("device_id", kioskDevice).containsEntry("terminal_type", "kiosk").containsEntry("point_id", space);
    }

    /** 一体机真实形状：一体机终端头＋登记设备号（不签名，默认 grace）。 */
    private RequestPostProcessor kiosk(RequestPostProcessor who) {
        return request -> {
            MockHttpServletRequest r = who.postProcessRequest(request);
            r.removeHeader("X-Terminal-Type");
            r.addHeader("X-Terminal-Type", "kiosk");
            r.addHeader("X-Device-Id", kioskDevice);
            return r;
        };
    }

    private void incident(String type, String incidentStatus) {
        jdbc.update("INSERT INTO amusement_incidents (id, project_id, amusement_project_id, severity, incident_type, status, title, description,"
                + " reported_by_user_id, reported_channel, occurred_at, created_at, updated_at, merchant_id)"
                + " VALUES (?, ?, ?, 'medium', ?, ?, '设备异常', '现场登记', 'usr_asb', 'web', now(), now(), now(), ?)",
                "ainc_asb_" + type + "_" + incidentStatus + "_" + s, complex, p, type, incidentStatus, merchant);
    }
}
