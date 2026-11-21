package com.complex.platform.merchantservice;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BY 段第 383 条 383-R1（BSM:569「进去可以看到全部被存档的对象」）：商户服务工单列表加 page、size 查询参数，
 * 含已存档列表能翻到第 100 条以后的已存档工单；不带参数时照旧第 1 页 100 条，size 超过 100 收口为 100。
 * 以新商户的员工账号请求（走 findWorkOrdersByScope），再以挂新项目、不挂商户的平台员工请求（18BY 收尾复核意见 1：经 staffReach 走
 * findWorkOrdersByMerchantProjects，新项目下只此一个商户），列表都只含本商户的单，不受共享测试库里别的工单影响。夹具随测试事务回滚（101 张新单若提交留库，
 * 会把 MerchantServiceJdbcIntegrationTests 定日建的单挤出全库最近 100 条）；角色名中文、编号后缀纯数字。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class MerchantWorkOrderListPagingJdbcIntegrationTests {

    private static final String LIST = "/api/v1/merchant-service/work-orders?include_archived=true";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = Long.toString(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    private final String project = "r383pg_prj_" + s;
    private final String merchant = "r383pg_mch_" + s;
    private final String earliest = "r383pg_wo_101_" + s;
    private RequestPostProcessor viewer;
    private RequestPostProcessor platformViewer;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','工单分页回归综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at)"
                + " VALUES (?, '工单分页回归商户', 'retail', ?, 'active', now(), now())", merchant, project);
        List<Object[]> rows = new ArrayList<>();
        for (int n = 1; n <= 101; n++) {
            rows.add(new Object[] {"r383pg_wo_" + n + "_" + s, merchant, "usr_r383pg_rep_" + s, n});
        }
        jdbc.batchUpdate("INSERT INTO merchant_service_work_orders (id, merchant_id, work_order_type, priority, status, title, description,"
                + " reported_by_user_id, reported_channel, closed_at, created_at, updated_at)"
                + " VALUES (?, ?, 'repair', 'medium', 'closed', '门锁报修', '门锁松动', ?, 'web', now(),"
                + " now() - make_interval(mins => ?), now())", rows);
        jdbc.update("UPDATE merchant_service_work_orders SET archived_at = now(), archived_by = ?, archive_reason = '已结案'"
                + " WHERE id = ?", "usr_r383pg_rep_" + s, earliest);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        viewer = staffs.staff("usr_r383pg_view_" + s, "staff", project, merchant, "merchant.service_work_order.view");
        platformViewer = staffs.staff("usr_r383pg_pview_" + s, "staff", project, null, "merchant.service_work_order.view");
    }

    @Test
    void includeArchivedListPagesPastFirstHundred() throws Exception {
        assertPagesPastFirstHundred(viewer);
    }

    @Test
    void platformStaffIncludeArchivedListPagesPastFirstHundred() throws Exception {
        assertPagesPastFirstHundred(platformViewer);
    }

    private void assertPagesPastFirstHundred(RequestPostProcessor who) throws Exception {
        String first = body(who, LIST);
        assertThat(JsonPath.<List<String>>read(first, "$.data.items[*].id")).hasSize(100).doesNotContain(earliest);
        assertThat(JsonPath.<Integer>read(first, "$.data.page")).isEqualTo(1);
        assertThat(JsonPath.<Integer>read(first, "$.data.page_size")).isEqualTo(100);

        String second = body(who, LIST + "&page=2&size=100");
        assertThat(JsonPath.<List<String>>read(second, "$.data.items[*].id")).containsExactly(earliest);
        assertThat(JsonPath.<List<String>>read(second, "$.data.items[*].archived_at")).allSatisfy(at -> assertThat(at).isNotNull());
        assertThat(JsonPath.<Integer>read(second, "$.data.page")).isEqualTo(2);

        String capped = body(who, LIST + "&size=500");
        assertThat(JsonPath.<List<String>>read(capped, "$.data.items[*].id")).hasSize(100);
        assertThat(JsonPath.<Integer>read(capped, "$.data.page_size")).isEqualTo(100);
    }

    private String body(RequestPostProcessor who, String url) throws Exception {
        return mockMvc.perform(get(url).with(who)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
