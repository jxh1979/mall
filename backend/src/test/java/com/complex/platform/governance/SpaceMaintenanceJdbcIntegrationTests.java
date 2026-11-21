package com.complex.platform.governance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BL 段第 388 条 388-R1（PGM:469「维护楼层、区域、点位、商铺、卡座、吧台位、游乐入口、库房和设备点位。」；
 * PGM:470「记录空间面积、用途、状态、负责人和所属商户。」）：新建空间记用途与负责人；修改空间（含改所属商户、状态改为不可用即停用），
 * 不改所属项目与平面图点位；入口口径同新建——平台范围＋governance.space.manage 可触及空间所属项目。真实 HTTP、蛇形键、真实授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SpaceMaintenanceJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = String.valueOf(System.nanoTime());
    private final String projectP = "prj_b84sp_p_" + s;
    private final String projectQ = "prj_b84sp_q_" + s;
    private final String barA = "mch_b84sp_a_" + s;
    private final String barB = "mch_b84sp_b_" + s;
    private final String archivedMerchant = "mch_b84sp_c_" + s;
    private final String typeId = "sptype_b84sp_" + s;
    private final String typeCode = "b84sp_" + s;
    private final String archivedTypeCode = "b84sp_old_" + s;
    private int seq;
    private RequestPostProcessor admin;
    private RequestPostProcessor projectStaff;
    private RequestPostProcessor merchantStaff;
    private RequestPostProcessor noCode;

    @BeforeEach
    void seed() {
        for (String project : List.of(projectP, projectQ)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex',?,'active',now(),now())",
                    project, "空间维护综合体" + project);
        }
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at) VALUES(?,'甲酒吧','bar',?,'active',now(),now())",
                barA, projectP);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at) VALUES(?,'乙酒吧','bar',?,'active',now(),now())",
                barB, projectP);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at,archived_at) "
                + "VALUES(?,'丙酒吧','bar',?,'active',now(),now(),now())", archivedMerchant, projectP);
        jdbc.update("INSERT INTO space_type_dictionary(id,type_code,type_name,sort_order,status,created_at,updated_at) VALUES(?,?,?,100,'active',now(),now())",
                typeId, typeCode, "回归卡座区" + s);
        jdbc.update("INSERT INTO space_type_dictionary(id,type_code,type_name,sort_order,status,created_at,updated_at) VALUES(?,?,?,101,'archived',now(),now())",
                typeId + "_old", archivedTypeCode, "回归旧种类" + s);
        StaffRequestFixture accounts = new StaffRequestFixture(jdbc);
        admin = accounts.staff("usr_b84sp_admin_" + s, "platform_admin", null, null, "governance.space.manage", "governance.space.view");
        projectStaff = accounts.staff("usr_b84sp_p_" + s, "staff", projectP, null, "governance.space.manage");
        merchantStaff = accounts.staff("usr_b84sp_m_" + s, "staff", projectP, barA, "governance.space.manage");
        noCode = accounts.staff("usr_b84sp_n_" + s, "platform_admin", null, null, "governance.space.view");
    }

    // ①新建空间带用途、负责人、所属商户：200，响应与库里的用途、负责人等于所填。修复前响应没有 purpose（断言失败）。
    @Test
    void createSpaceRecordsPurposeAndOwner() throws Exception {
        String body = "{\"idempotency_key\":\"b84sp-c-" + s + "\",\"project_id\":\"" + projectP + "\",\"space_name\":\"卡座区一\","
                + "\"space_type\":\"" + typeCode + "\",\"merchant_id\":\"" + barA + "\",\"purpose\":\"酒吧卡座\",\"owner_user_id\":\"usr_b84sp_owner\"}";
        String id = read(send(admin, post("/api/v1/platform/spaces"), body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.purpose").value("酒吧卡座"))
                .andExpect(jsonPath("$.data.owner_user_id").value("usr_b84sp_owner"))
                .andExpect(jsonPath("$.data.merchant_id").value(barA)), "$.data.id");
        Map<String, Object> row = jdbc.queryForMap("SELECT purpose, owner_user_id FROM spaces WHERE id = ?", id);
        assertThat(row.get("purpose")).isEqualTo("酒吧卡座");
        assertThat(row.get("owner_user_id")).isEqualTo("usr_b84sp_owner");
    }

    // ②修改名称、所属商户、状态（停用）、面积、用途、负责人：200；列表里该行为新值，所属项目与平面图点位不变。修复前没有修改入口（状态断言失败）。
    @Test
    void updateSpaceChangesFieldsButKeepsProjectAndPolygon() throws Exception {
        String id = space(projectP, barA, typeCode);
        String body = "{\"idempotency_key\":\"b84sp-u-" + s + "\",\"space_name\":\"卡座区改名\",\"space_type\":\"" + typeCode + "\","
                + "\"merchant_id\":\"" + barB + "\",\"status\":\"unavailable\",\"area_square_meter\":12.5,\"floor_no\":\"2F\","
                + "\"purpose\":\"改作包厢\",\"owner_user_id\":\"usr_b84sp_owner2\"}";
        send(admin, put("/api/v1/platform/spaces/" + id), body).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));

        String list = mockMvc.perform(get("/api/v1/platform/spaces").param("size", "500").with(admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(field(list, id, "space_name")).isEqualTo("卡座区改名");
        assertThat(field(list, id, "merchant_id")).isEqualTo(barB);
        assertThat(field(list, id, "status")).isEqualTo("unavailable");
        assertThat(field(list, id, "floor_no")).isEqualTo("2F");
        assertThat(field(list, id, "purpose")).isEqualTo("改作包厢");
        assertThat(field(list, id, "owner_user_id")).isEqualTo("usr_b84sp_owner2");
        assertThat(field(list, id, "project_id")).isEqualTo(projectP);
        assertThat(jdbc.queryForObject("SELECT area_square_meter FROM spaces WHERE id = ?", java.math.BigDecimal.class, id))
                .isEqualByComparingTo("12.5");
        assertThat(jdbc.queryForObject("SELECT polygon_points::text FROM spaces WHERE id = ?", String.class, id)).contains("\"x\": 1");
    }

    // ③只到项目 P 的平台侧账号改项目 Q 的空间：403 PROJECT_SCOPE_MISMATCH、行不变、落失败留痕；挂商户账号 403 PLATFORM_SCOPE_REQUIRED；
    // 不持码 403；空间不存在 404 SPACE_NOT_FOUND。修复前没有修改入口（状态断言失败）。
    @Test
    void updateSpaceRejectsOutOfScopeCallersAndMissingSpace() throws Exception {
        String id = space(projectQ, null, typeCode);
        String key = "b84sp-scope-" + s;
        send(projectStaff, put("/api/v1/platform/spaces/" + id), update(key, "越权改名", typeCode))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PROJECT_SCOPE_MISMATCH"));
        assertThat(jdbc.queryForObject("SELECT space_name FROM spaces WHERE id = ?", String.class, id)).isEqualTo("回归空间");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'space.update_failed' AND idempotency_key = ?",
                Integer.class, key)).isEqualTo(1);

        send(merchantStaff, put("/api/v1/platform/spaces/" + id), update("b84sp-m-" + s, "越权改名", typeCode))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PLATFORM_SCOPE_REQUIRED"));
        send(noCode, put("/api/v1/platform/spaces/" + id), update("b84sp-n-" + s, "越权改名", typeCode))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT space_name FROM spaces WHERE id = ?", String.class, id)).isEqualTo("回归空间");

        send(admin, put("/api/v1/platform/spaces/sp_b84sp_none_" + s), update("b84sp-404-" + s, "不存在", typeCode))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("SPACE_NOT_FOUND"));
    }

    // ④改成已存档商户：400 MERCHANT_ARCHIVED；该空间的类型存档后只改状态（类型照原值）：200；改成已存档的类型：400 VALIDATION_FAILED；
    // 空间所属商户存档后只改状态、merchant_id 照原值带回：200（BSM:569 存档「不再被新业务选用」只管新选，沿用原值不核）。
    @Test
    void updateSpaceChecksOnlyChangedMerchantAndType() throws Exception {
        String id = space(projectP, barA, typeCode);
        String kept = space(projectP, barA, typeCode);
        send(admin, put("/api/v1/platform/spaces/" + id), "{\"idempotency_key\":\"b84sp-arch-m-" + s + "\",\"space_name\":\"回归空间\","
                + "\"space_type\":\"" + typeCode + "\",\"merchant_id\":\"" + archivedMerchant + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("MERCHANT_ARCHIVED"));

        send(admin, post("/api/v1/platform/space-types/" + typeId + "/archive"), "{\"reason\":\"不再新建这一种\"}").andExpect(status().isOk());
        send(admin, put("/api/v1/platform/spaces/" + id), "{\"idempotency_key\":\"b84sp-keep-t-" + s + "\",\"space_name\":\"回归空间\","
                + "\"space_type\":\"" + typeCode + "\",\"merchant_id\":\"" + barA + "\",\"status\":\"unavailable\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("unavailable"));

        send(admin, put("/api/v1/platform/spaces/" + id), update("b84sp-arch-t-" + s, "回归空间", archivedTypeCode))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT space_type FROM spaces WHERE id = ?", String.class, id)).isEqualTo(typeCode);

        jdbc.update("UPDATE merchants SET archived_at = now() WHERE id = ?", barA);
        send(admin, put("/api/v1/platform/spaces/" + kept), "{\"idempotency_key\":\"b84sp-keep-m-" + s + "\",\"space_name\":\"回归空间\","
                + "\"space_type\":\"" + typeCode + "\",\"merchant_id\":\"" + barA + "\",\"status\":\"unavailable\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("unavailable"))
                .andExpect(jsonPath("$.data.merchant_id").value(barA));
        assertThat(jdbc.queryForObject("SELECT merchant_id FROM spaces WHERE id = ?", String.class, kept)).isEqualTo(barA);
    }

    // ⑤同一请求编号重发：返回同一结果并提示已完成，修改成功留痕只一条。
    @Test
    void updateSpaceReplayReturnsFirstResultOnce() throws Exception {
        String id = space(projectP, barA, typeCode);
        String key = "b84sp-replay-" + s;
        send(admin, put("/api/v1/platform/spaces/" + id), update(key, "重发改名", typeCode))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.space_name").value("重发改名"));
        send(admin, put("/api/v1/platform/spaces/" + id), update(key, "重发改名", typeCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").value(true))
                .andExpect(jsonPath("$.data.space_name").value("重发改名"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'space.updated' AND idempotency_key = ?",
                Integer.class, key)).isEqualTo(1);
    }

    /** 平台管理员经新建空间接口建一行（带平面图点位），返回编号。 */
    private String space(String project, String merchant, String type) throws Exception {
        String body = "{\"idempotency_key\":\"b84sp-seed-" + (++seq) + "-" + s + "\",\"project_id\":\"" + project + "\",\"space_name\":\"回归空间\","
                + "\"space_type\":\"" + type + "\"" + (merchant == null ? "" : ",\"merchant_id\":\"" + merchant + "\"")
                + ",\"polygon_points\":[{\"x\":1,\"y\":2}]}";
        return read(send(admin, post("/api/v1/platform/spaces"), body).andExpect(status().isOk()), "$.data.id");
    }

    private String update(String key, String name, String type) {
        return "{\"idempotency_key\":\"" + key + "\",\"space_name\":\"" + name + "\",\"space_type\":\"" + type + "\"}";
    }

    private ResultActions send(RequestPostProcessor who, MockHttpServletRequestBuilder request, String body) throws Exception {
        return mockMvc.perform(request.with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String read(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), path);
    }

    private static Object field(String json, String id, String key) {
        List<Object> values = JsonPath.read(json, "$.data.items[?(@.id == '" + id + "')]." + key);
        assertThat(values).hasSize(1);
        return values.get(0);
    }
}
