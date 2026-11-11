package com.complex.platform.governance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
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
 * 第十八批 AE 段第 198 条 198-R2 tests②：契约删去 SpaceTypeRequest.status（代码不读，198-R3⑧ 起网页也不发）后，平台管理员按空间种类页
 * （SpaceTypesPage）的真实请求形状——idempotency_key、type_code、type_name、sort_order——走真实 HTTP、真实授权链新建，种类一律为启用。
 * 入口未改，本类只锁现行为，不先红。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SpaceTypeCreateWebShapeJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String digits = String.valueOf(System.nanoTime());

    // 网页形状（不带 status）新建：200，返回与库内种类均为 active。
    @Test
    void createSpaceType_webShapeWithoutStatus_createdActive() throws Exception {
        RequestPostProcessor platformAdmin = new StaffRequestFixture(jdbc)
                .staff("usr_b51ws_admin_" + digits, "platform_admin", null, null, "governance.space.manage");
        String code = "b51ws_" + digits;
        mockMvc.perform(post("/api/v1/platform/space-types").with(platformAdmin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"space-type-b51ws-" + digits + "\",\"type_code\":\"" + code
                                + "\",\"type_name\":\"网页形状种类" + digits + "\",\"sort_order\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type_code").value(code))
                .andExpect(jsonPath("$.data.status").value("active"));
        assertThat(jdbc.queryForObject("SELECT status FROM space_type_dictionary WHERE type_code = ?", String.class, code))
                .isEqualTo("active");
    }
}
