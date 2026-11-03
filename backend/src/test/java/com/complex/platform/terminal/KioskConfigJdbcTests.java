package com.complex.platform.terminal;

// 文件版本：V0.01

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.StaffRequestFixture;
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
 * 第十八批 N 段第 120 条 120-R1（BSM:980「……其中只有启用售票能力的一体机，才允许授权服务人员在到店服务场景下辅助客人购票。」；
 * PGM:742「开关默认取值：任何能力在后台没有显式开关配置行时一律默认关闭（opt-in）……」）：一体机配置与心跳按设备台账读。
 * 管理员按后台真实请求把设备绑定为一体机、设所属商户、为本机开售票开关；一体机凭绑定时签发的设备令牌取配置、上报心跳。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class KioskConfigJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private String s;
    private String bar;
    private String point;
    private RequestPostProcessor admin;

    @BeforeEach
    void seed() {
        s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        bar = "bar_kcfg_" + s;
        point = "point_kcfg_" + s;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at)"
                + " VALUES (?, '一体机酒吧', 'bar', 'active', now(), now())", bar);
        admin = new StaffRequestFixture(jdbc).staff("usr_kcfg_admin_" + s, "platform_admin", null, null,
                "terminal.device.bind", "terminal.device.lifecycle", "terminal.switch.manage");
    }

    @Test
    void kioskConfigAndHeartbeatFollowTheDeviceLedgerAndTicketingSwitch() throws Exception {
        String ticketing = "kcfg-ticketing-" + s;
        String token = bind(ticketing, "ticketing");
        String row = jdbc.queryForObject("SELECT id FROM terminal_devices WHERE device_id = ?", String.class, ticketing);
        mockMvc.perform(put("/api/v1/terminals/devices/" + row + "/merchant").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key":"kcfg-merchant-%s","merchant_id":"%s","reason":"一体机所属酒吧"}
                                """.formatted(s, bar)))
                .andExpect(status().isOk());

        // 售票型、还没有本机的售票开关行：不开放售票（缺行即关）。
        config(ticketing, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.device_id").value(ticketing))
                .andExpect(jsonPath("$.data.kiosk_mode").value("ticketing"))
                .andExpect(jsonPath("$.data.ticketing_enabled").value(false))
                .andExpect(jsonPath("$.data.bound_scope").value(point))
                .andExpect(jsonPath("$.data.merchant_id").value(bar));

        enableTicketing(ticketing);
        config(ticketing, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticketing_enabled").value(true));

        // 服务型一体机开了售票开关也不开放售票。
        String service = "kcfg-service-" + s;
        String serviceToken = bind(service, "service");
        enableTicketing(service);
        config(service, serviceToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kiosk_mode").value("service"))
                .andExpect(jsonPath("$.data.ticketing_enabled").value(false));

        config(ticketing, "wrong-" + token)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("DEVICE_CREDENTIAL_INVALID"));

        // 心跳不带模式时取设备台账上的一体机模式。
        mockMvc.perform(post("/api/v1/terminals/kiosks/" + ticketing + "/heartbeat")
                        .header("X-Terminal-Type", "kiosk").header("X-Device-Token", token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"device_id":"%s","terminal_type":"kiosk","status":"online"}
                                """.formatted(ticketing)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("online"))
                .andExpect(jsonPath("$.data.kiosk_mode").value("ticketing"));
    }

    /** 管理员按后台真实请求把设备绑定为一体机（按空间绑点位），取响应里签发的设备令牌。 */
    private String bind(String deviceId, String kioskMode) throws Exception {
        return CustomerTicketPaymentFixture.read(mockMvc.perform(post("/api/v1/terminals/devices/bind").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key":"kcfg-bind-%s","device_id":"%s","terminal_type":"kiosk",
                                 "binding_scope":"space","bound_space_id":"%s","kiosk_mode":"%s"}
                                """.formatted(deviceId, deviceId, point, kioskMode)))
                .andExpect(status().isOk()), "$.data.device_token");
    }

    private void enableTicketing(String deviceId) throws Exception {
        mockMvc.perform(put("/api/v1/terminals/capability-switches/tcs_kiosk_ticketing_" + deviceId).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key":"kcfg-switch-%s","terminal_type":"kiosk","capability_code":"ticketing",
                                 "scope_id":"%s","enabled":true,"operator_note":"开启本机售票"}
                                """.formatted(deviceId, deviceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    private ResultActions config(String deviceId, String token) throws Exception {
        return mockMvc.perform(get("/api/v1/terminals/kiosks/" + deviceId + "/config")
                .header("X-Terminal-Type", "kiosk").header("X-Device-Token", token));
    }
}
