package com.complex.platform.archive;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
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
 * 第十八批 BY 段第 387 条 387-R1（第十八批裁决 81「每个分类都显示存档理由」；BSM:569「后台另设「已存档内容」页，进去可以看到全部被存档的对象」；
 * BSM:577「存档及删除都需要填写理由」）：各类含已存档列表带出存档理由——带理由存档后，含已存档列表里该行 archive_reason 等于所填；
 * 取消存档后再读为空。真实入口、真实授权链、蛇形键；夹具随测试事务回滚；角色名中文、编号后缀纯数字。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class ArchiveReasonListedJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = Long.toString(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    private final String project = "r387_prj_" + s;
    private final String merchant = "r387_mch_" + s;
    private StaffRequestFixture staffs;
    private RequestPostProcessor admin;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','存档理由回归综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at)"
                + " VALUES (?, '存档理由回归酒吧', 'bar', ?, 'active', now(), now())", merchant, project);
        staffs = new StaffRequestFixture(jdbc);
        admin = staffs.staff("usr_r387_adm_" + s, "platform_admin", project, null,
                "ticket.product.view", "ticket.product.manage", "facility.equipment.manage", "staff.profile.view",
                "staff.profile.submit", "bar.product.view", "bar.product.manage");
    }

    @Test
    void ticketTypeListCarriesArchiveReason() throws Exception {
        String id = "tt_r387_" + s;
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, status, created_at, updated_at)"
                + " VALUES (?, '存档理由回归票种', 'fixed', 'active', now(), now())", id);
        String base = "/api/v1/ticketing/ticket-types/" + id;
        send(admin, base + "/archive", "{\"idempotency_key\":\"r387-tt-a-" + s + "\",\"reason\":\"季节票下线\"}");
        assertThat(reason(admin, "/api/v1/ticketing/ticket-types?includeArchived=true", id)).isEqualTo("季节票下线");
        send(admin, base + "/unarchive", "{\"idempotency_key\":\"r387-tt-u-" + s + "\"}");
        assertThat(reason(admin, "/api/v1/ticketing/ticket-types?includeArchived=true", id)).isNull();
    }

    @Test
    void facilityEquipmentListCarriesArchiveReason() throws Exception {
        String id = "feq_r387_" + s;
        jdbc.update("INSERT INTO facility_equipment (id, name, equipment_type, status, project_id) VALUES (?, '3号冷却塔', 'hvac', 'normal', ?)",
                id, project);
        String base = "/api/v1/facility/equipment/" + id;
        send(admin, base + "/archive", "{\"reason\":\"设备迁出\"}");
        assertThat(reason(admin, "/api/v1/facility/equipment?include_archived=true", id)).isEqualTo("设备迁出");
        send(admin, base + "/unarchive", "{}");
        assertThat(reason(admin, "/api/v1/facility/equipment?include_archived=true", id)).isNull();
    }

    @Test
    void merchantWorkOrderListCarriesArchiveReason() throws Exception {
        String id = "r387_wo_" + s;
        jdbc.update("INSERT INTO merchant_service_work_orders (id, merchant_id, work_order_type, priority, status, title, description,"
                + " reported_by_user_id, reported_channel, closed_at, created_at, updated_at)"
                + " VALUES (?, ?, 'repair', 'medium', 'closed', '门锁报修', '门锁松动', ?, 'web', now(), now(), now())",
                id, merchant, "usr_r387_rep_" + s);
        RequestPostProcessor merchantStaff = staffs.staff("usr_r387_mst_" + s, "staff", project, merchant,
                "merchant.service_work_order.view", "merchant.service_work_order.close");
        String base = "/api/v1/merchant-service/work-orders/" + id;
        send(merchantStaff, base + "/archive", "{\"reason\":\"已结案\"}");
        assertThat(reason(merchantStaff, "/api/v1/merchant-service/work-orders?include_archived=true", id)).isEqualTo("已结案");
        send(merchantStaff, base + "/unarchive", "{}");
        assertThat(reason(merchantStaff, "/api/v1/merchant-service/work-orders?include_archived=true", id)).isNull();
    }

    @Test
    void staffProfileListCarriesArchiveReason() throws Exception {
        String id = "stf_r387_" + s;
        jdbc.update("INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, company_id, registration_status, status, created_at, updated_at)"
                + " VALUES (?, ?, NULL, NULL, '存档理由回归员工', ?, 'approved', 'active', now(), now())", id, project, "r387_co_" + s);
        String base = "/api/v1/staff/profiles/" + id;
        send(admin, base + "/archive", "{\"reason\":\"已离职\"}");
        assertThat(reason(admin, "/api/v1/staff/profiles?include_archived=true", id)).isEqualTo("已离职");
        send(admin, base + "/unarchive", "{}");
        assertThat(reason(admin, "/api/v1/staff/profiles?include_archived=true", id)).isNull();
    }

    @Test
    void barProductListCarriesArchiveReason() throws Exception {
        String id = "bp_r387_" + s;
        jdbc.update("INSERT INTO bar_products (id, bar_id, merchant_id, product_name, product_type, price_cent, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, '精酿啤酒', 'beer', 500, 'active', now(), now())", id, merchant, merchant);
        String base = "/api/v1/bar/catalog/products/" + id;
        send(admin, base + "/archive", "{\"reason\":\"季节性下架\"}");
        assertThat(reason(admin, "/api/v1/bar/products?includeArchived=true", id)).isEqualTo("季节性下架");
        send(admin, base + "/unarchive", "{}");
        assertThat(reason(admin, "/api/v1/bar/products?includeArchived=true", id)).isNull();
    }

    /**
     * 18BY 收尾复核意见 2：不持 bar.internal_settlement_price.view 的酒吧账号读套餐列表走 BarComboRecord.withoutInternalPrice，
     * 存档时间、存档理由照样带出（已存档内容页「酒吧套餐」按 archived_at 判已存档）。
     */
    @Test
    void barComboListWithoutInternalPriceViewCarriesArchiveInfo() throws Exception {
        String id = "bc_r387_" + s;
        jdbc.update("INSERT INTO bar_combos (id, bar_id, merchant_id, combo_name, price_cent, internal_settlement_price_cent, status,"
                + " created_at, updated_at) VALUES (?, ?, ?, '双人畅饮套餐', 9900, 6000, 'active', now(), now())", id, merchant, merchant);
        RequestPostProcessor barManager = staffs.staff("usr_r387_bar_" + s, "staff", project, merchant, "bar.combo.view", "bar.combo.manage");
        send(barManager, "/api/v1/bar/catalog/combos/" + id + "/archive", "{\"reason\":\"套餐停售\"}");
        String body = mockMvc.perform(get("/api/v1/bar/catalog/combos?includeArchived=true").with(barManager)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String row = "$.data[?(@.id == '" + id + "')]";
        assertThat(JsonPath.<List<Object>>read(body, row)).hasSize(1);
        assertThat(JsonPath.<List<Object>>read(body, row + ".internal_settlement_price_cent")).allSatisfy(price -> assertThat(price).isNull());
        assertThat(JsonPath.<List<Object>>read(body, row + ".archived_at")).singleElement().isNotNull();
        assertThat(JsonPath.<List<Object>>read(body, row + ".archive_reason")).containsExactly("套餐停售");
    }

    private void send(RequestPostProcessor who, String url, String body) throws Exception {
        mockMvc.perform(post(url).with(who).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
    }

    /** 含已存档列表里该行的 archive_reason（键缺省按空）。 */
    private String reason(RequestPostProcessor who, String url, String id) throws Exception {
        String body = mockMvc.perform(get(url).with(who)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Object> rows = JsonPath.read(body, "$.data.items[?(@.id == '" + id + "')]");
        assertThat(rows).as("列表里应有 %s", id).hasSize(1);
        List<String> reasons = JsonPath.read(body, "$.data.items[?(@.id == '" + id + "')].archive_reason");
        return reasons.isEmpty() ? null : reasons.get(0);
    }
}
