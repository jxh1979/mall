package com.complex.platform.access.staff;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
 * 第十八批 BL 段第 390 条 390-R1（BSM:1474「主管排班：排到几点几分，可指定当天工作点位（比如某个服务区、某片保洁区域）」）：排班可选点位清单——
 * 最高权限与平台管理员看全部启用中的空间，挂商户的主管看本商户名下，其余看 staff.shift.manage 可触及项目下的；只列启用中的。
 * 空间由平台管理员经新建空间接口建。真实 HTTP、蛇形键、真实授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class StaffShiftPointOptionsJdbcIntegrationTests {

    private static final String URL = "/api/v1/staff/shift-point-options";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = String.valueOf(System.nanoTime());
    private final String projectP = "prj_b84pt_p_" + s;
    private final String projectQ = "prj_b84pt_q_" + s;
    private final String barA = "mch_b84pt_a_" + s;
    private final String barB = "mch_b84pt_b_" + s;
    private int seq;
    private RequestPostProcessor admin;

    @BeforeEach
    void seed() throws Exception {
        for (String project : List.of(projectP, projectQ)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex',?,'active',now(),now())",
                    project, "排班点位综合体" + project);
        }
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at) VALUES(?,'甲酒吧','bar',?,'active',now(),now())",
                barA, projectP);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at) VALUES(?,'乙酒吧','bar',?,'active',now(),now())",
                barB, projectP);
        admin = new StaffRequestFixture(jdbc).staff("usr_b84pt_admin_" + s, "platform_admin", null, null,
                "governance.space.manage", "staff.shift.manage");
        space(projectP, barA, "卡座-01" + s, "active");
        space(projectP, barA, "卡座-02" + s, "unavailable");
        space(projectP, barB, "卡座-09" + s, "active");
        space(projectP, null, "一楼保洁区" + s, "active");
        space(projectQ, null, "Q 区" + s, "active");
    }

    // ①挂酒吧甲的主管：只有甲名下启用的卡座-01。修复前接口不存在（状态断言失败）。
    @Test
    void merchantSupervisorSeesOnlyOwnMerchantActiveSpaces() throws Exception {
        RequestPostProcessor supervisor = new StaffRequestFixture(jdbc).staff("usr_b84pt_a_" + s, "staff", projectP, barA, "staff.shift.manage");
        assertThat(names(supervisor)).containsExactly("卡座-01" + s);
    }

    // ②只到项目 P 的平台侧主管：项目 P 下启用的三个（含无商户的公区），没有项目 Q 的与停用的。
    @Test
    void projectSupervisorSeesReachableProjectActiveSpaces() throws Exception {
        RequestPostProcessor supervisor = new StaffRequestFixture(jdbc).staff("usr_b84pt_p_" + s, "staff", projectP, null, "staff.shift.manage");
        assertThat(names(supervisor)).containsExactlyInAnyOrder("卡座-01" + s, "卡座-09" + s, "一楼保洁区" + s);
    }

    // ③平台管理员：四个启用的全有，停用的没有。
    @Test
    void platformAdminSeesAllActiveSpaces() throws Exception {
        List<String> names = names(admin);
        assertThat(names).contains("卡座-01" + s, "卡座-09" + s, "一楼保洁区" + s, "Q 区" + s);
        assertThat(names).doesNotContain("卡座-02" + s);
    }

    // ④不持 staff.shift.manage：403。
    @Test
    void callerWithoutShiftManageIsForbidden() throws Exception {
        RequestPostProcessor viewer = new StaffRequestFixture(jdbc).staff("usr_b84pt_v_" + s, "staff", projectP, null, "bar.order.create");
        mockMvc.perform(get(URL).with(viewer)).andExpect(status().isForbidden());
    }

    private void space(String project, String merchant, String name, String spaceStatus) throws Exception {
        String body = "{\"idempotency_key\":\"b84pt-sp-" + (++seq) + "-" + s + "\",\"project_id\":\"" + project + "\",\"space_name\":\"" + name
                + "\",\"space_type\":\"zone\",\"status\":\"" + spaceStatus + "\"" + (merchant == null ? "" : ",\"merchant_id\":\"" + merchant + "\"") + "}";
        mockMvc.perform(post("/api/v1/platform/spaces").with(admin).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    private List<String> names(RequestPostProcessor who) throws Exception {
        String content = mockMvc.perform(get(URL).with(who)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(content, "$.data.items[*].space_name");
    }
}
