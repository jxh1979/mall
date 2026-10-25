package com.complex.platform.support;

// 文件版本：V0.01

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerSessionRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * 第十三批 D 段共用：PGM 第 736 行「可以按全局、综合体、商户、业务系统、模块、版本/设备、灰度开关」、第 744 行「前端隐藏只是辅助，
 * 后端接口必须独立校验」——客人接口按业务对象在服务端所属的综合体判按综合体的客人端开关。后台关掉某综合体的小程序客人端；
 * 请求按客人端真实形状（蛇形键、X-Terminal-Type、X-Customer-Token，写请求 Idempotency-Key 头与请求体 idempotency_key 同值），
 * X-Project-Id 可带可不带。
 */
public final class CustomerProjectSwitchRequests {

    /** 一次请求；参数为本次带的 X-Project-Id（为空＝不带）。 */
    @FunctionalInterface
    public interface Call {
        ResultActions perform(String projectHeader) throws Exception;
    }

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public CustomerProjectSwitchRequests(MockMvc mockMvc, JdbcTemplate jdbc, ObjectMapper json) {
        this.mockMvc = mockMvc;
        this.jdbc = jdbc;
        this.json = json;
    }

    /** 后台按综合体关掉小程序客人端（terminal.enabled，scope=project）。 */
    public void close(String projectId) {
        close(projectId, "terminal.enabled");
    }

    /** 后台按综合体单独停小程序的某个模块（能力码，scope=project）。 */
    public void close(String projectId, String capability) {
        jdbc.update("""
                INSERT INTO terminal_capability_switches (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason,
                    created_at, updated_at)
                VALUES (?, 'miniapp', ?, 'project', ?, FALSE, '停用该综合体客人端', now(), now())""",
                "tcs_b16_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12), capability, projectId);
    }

    /** 该综合体的一家酒吧商户；返回商户编号。 */
    public String barMerchant(String projectId, String tag) {
        String id = "bar_b16_" + tag + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '综合体开关测试酒吧', 'bar', 'active', now(), now(), ?)", id, projectId);
        return id;
    }

    /** 另一位持小程序会话的客人；返回会话令牌。 */
    public static String customer(CustomerRepository customers, String tag) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String id = "cus_b16_" + tag + "_" + suffix;
        String phone = "136" + String.format("%08d", Math.floorMod(suffix.hashCode(), 100_000_000));
        Instant now = Instant.now();
        customers.saveCustomer(new CustomerRecord(id, phone, "136****" + phone.substring(7), "active", null, null, now, now));
        String token = "tok_b16_" + tag + "_" + suffix;
        customers.saveSession(new CustomerSessionRecord("sess_b16_" + tag + "_" + suffix, id, token, "miniapp", now,
                now.plusSeconds(3600), "active"));
        return token;
    }

    /** token 为空＝匿名；params 按「名, 值」成对给查询参数。 */
    public ResultActions get(String path, String token, String projectHeader, String... params) throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(path);
        for (int i = 0; i + 1 < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        return mockMvc.perform(headers(request, token, projectHeader));
    }

    public ResultActions post(String path, String token, String projectHeader, Map<String, ?> body) throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(path)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        Object key = body.get("idempotency_key");
        if (key != null) {
            request.header("Idempotency-Key", key.toString());
        }
        return mockMvc.perform(headers(request, token, projectHeader));
    }

    private static MockHttpServletRequestBuilder headers(MockHttpServletRequestBuilder request, String token, String projectHeader) {
        request.header("X-Terminal-Type", "miniapp");
        if (token != null) {
            request.header("X-Customer-Token", token);
        }
        if (projectHeader != null) {
            request.header("X-Project-Id", projectHeader);
        }
        return request;
    }

    /** 请求体：idempotency_key 加上成对的「键, 值」。 */
    public static Map<String, Object> keyed(String key, Object... pairs) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", key + "-" + UUID.randomUUID().toString().substring(0, 8));
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            body.put((String) pairs[i], pairs[i + 1]);
        }
        return body;
    }

    /**
     * 被关综合体的对象：不带 X-Project-Id、或把头写成开着的综合体，都 403 TERMINAL_DISABLED。写请求传开着时成功过的同一请求体
     * （同一幂等键）即「关掉后同键重放」。
     */
    public static void rejected(String openProjectId, Call call) throws Exception {
        for (String header : new String[] {null, openProjectId}) {
            call.perform(header)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
        }
    }
}
