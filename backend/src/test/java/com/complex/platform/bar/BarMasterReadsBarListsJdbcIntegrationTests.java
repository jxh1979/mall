package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AY 段第 344 条（真库、真实入口、真实授权链）：第十八批裁决 71「既然是最高权限，不应该什么都能看到吗？」；BSM:576「持授权 U 盘密钥的最高权限本人
 * 发起的操作，不需要经过任何审批。」。最高权限（保留超管账号，组织 platform-company、无商户）读后台酒吧各页与存酒页的列表不按商户、项目范围；
 * 甲店员工照旧只看本店，读乙店的仍 403。只放开读取。
 * ① 344-R1：订单、退酒待审、班次、挂起看板含甲乙两店；乙店账单奖励、退款原账单、服务责任区间、服务补录、服务收费规则、月结都 200。
 * ② 344-R2：存酒列表含甲乙两店；乙店存酒的续存记录、人工例外取酒、补偿方案都 200。
 * ③ 344-R3：乙店账单的证据附件回读与证据时间线 200。
 * 修复前：最高权限订单列表与挂起看板为空，其余 403（MERCHANT_SCOPE_REQUIRED、BAR_PAYROLL_SCOPE_MISMATCH）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarMasterReadsBarListsJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = Long.toString(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    private Bar a;
    private Bar b;
    private RequestPostProcessor staffA;
    private RequestPostProcessor staffB;

    /** 一家酒吧的夹具编号。 */
    private record Bar(String project, String id, String session, String order, String bill, String voidId, String shift, String rule,
            String bottle) {
    }

    @BeforeEach
    void seed() throws Exception {
        a = bar("a", "甲");
        b = bar("b", "乙");
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        staffA = staffs.staff("r344usr_a_" + s, "staff", a.project(), a.id(),
                "bar.order.view", "bar.saved_bottle.manage", "evidence.timeline.view");
        staffB = staffs.staff("r344usr_b_" + s, "staff", b.project(), b.id(), "evidence.attachment.create");
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    /** ① 344-R1：最高权限读酒吧各列表含甲乙两店，读乙店各明细 200；甲店员工只看本店，读乙店账单奖励 403。 */
    @Test
    void masterReadsEveryBarsListsAndDetails() throws Exception {
        assertThat(this.<String>read(master(), "/api/v1/bar/orders", "$.data.items[*].id")).contains(a.order(), b.order());
        assertThat(this.<String>read(master(), "/api/v1/bar/order-item-voids?status=pending", "$.data.items[*].id"))
                .contains(a.voidId(), b.voidId());
        assertThat(this.<String>read(master(), "/api/v1/bar/shifts", "$.data.items[*].id")).contains(a.shift(), b.shift());
        assertThat(this.<String>read(master(), "/api/v1/bar/suspended-orders?bar_id=" + a.id(), "$.data.orders[*].order_id"))
                .containsExactly(a.order());
        assertThat(this.<String>read(master(), "/api/v1/bar/suspended-orders?bar_id=" + b.id(), "$.data.orders[*].order_id"))
                .containsExactly(b.order());

        ok(master(), "/api/v1/bar/bills/" + b.bill() + "/rewards");
        ok(master(), "/api/v1/bar/bills/" + b.bill() + "/refund-context");
        ok(master(), "/api/v1/bar/table-sessions/" + b.session() + "/service-responsibilities");
        ok(master(), "/api/v1/evidence/manual-backfills?target_type=bar_session_service&target_id=" + b.session());
        assertThat(this.<String>read(master(), "/api/v1/bar/payroll-rules?project_id=" + b.project() + "&merchant_id=" + b.id(), "$.data[*].id"))
                .contains(b.rule());
        ok(master(), "/api/v1/bar/payroll-runs?project_id=" + b.project() + "&merchant_id=" + b.id() + "&month=" + YearMonth.now());

        assertThat(this.<String>read(staffA, "/api/v1/bar/orders", "$.data.items[*].id")).contains(a.order()).doesNotContain(b.order());
        mockMvc.perform(get("/api/v1/bar/bills/" + b.bill() + "/rewards").with(staffA))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
    }

    /** ② 344-R2：最高权限读存酒列表含甲乙两店，乙店存酒的续存记录、人工例外取酒、补偿方案 200；甲店员工只看本店，读乙店补偿方案 403。 */
    @Test
    void masterReadsEveryBarsSavedBottles() throws Exception {
        assertThat(this.<String>read(master(), "/api/v1/inventory/saved-bottles", "$.data.items[*].id")).contains(a.bottle(), b.bottle());
        ok(master(), "/api/v1/inventory/saved-bottles/" + b.bottle() + "/renewals");
        ok(master(), "/api/v1/inventory/saved-bottles/" + b.bottle() + "/manual-pickups");
        ok(master(), "/api/v1/inventory/saved-bottles/" + b.bottle() + "/remedies");

        assertThat(this.<String>read(staffA, "/api/v1/inventory/saved-bottles", "$.data.items[*].id"))
                .contains(a.bottle()).doesNotContain(b.bottle());
        mockMvc.perform(get("/api/v1/inventory/saved-bottles/" + b.bottle() + "/remedies").with(staffA))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
    }

    /** ③ 344-R3：乙店员工给本店账单上传的现场照片，最高权限回读与看证据时间线 200；甲店员工回读仍 403。 */
    @Test
    void masterReadsAnotherBarsEvidence() throws Exception {
        String attachment = JsonPath.read(mockMvc.perform(multipart("/api/v1/evidence/attachments/upload")
                        .file(new MockMultipartFile("file", "photo.jpg", "image/jpeg", "现场照片".getBytes(StandardCharsets.UTF_8)))
                        .param("business_type", "bar_bill").param("business_id", b.bill()).param("attachment_role", "photo")
                        .with(staffB)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");

        ok(master(), "/api/v1/evidence/attachments/" + attachment + "/file");
        ok(master(), "/api/v1/evidence/timelines/bar_bill/" + b.bill());

        mockMvc.perform(get("/api/v1/evidence/attachments/" + attachment + "/file").with(staffA))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
    }

    // ---------- 夹具 ----------

    /** 一个综合体里一家酒吧：卡座（挂铺位）、开台会话、一单（含账单、待审退品、包厢挂起）、开班班次、已批准服务收费规则、一瓶存酒。 */
    private Bar bar(String tag, String label) throws Exception {
        String n = tag + "_" + s;
        String project = "r344prj_" + n;
        String bar = "r344bar_" + n;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex',?,'active',now(),now())",
                project, "最高权限读取回归综合体" + label);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,?,'bar','active',?,now(),now())",
                bar, "最高权限读取回归酒吧" + label + s, project);
        String space = "r344spc_" + n;
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at)"
                + " VALUES(?,?,?,'卡座区','unit','active',now(),now())", space, project, bar);
        String table = "r344tbl_" + n;
        jdbc.update("INSERT INTO bar_tables(id,bar_id,merchant_id,table_code,table_name,table_type,capacity,allowed_pass_free_guest_count,"
                + "min_consumption_cent,currency,status,created_at,updated_at,space_id) VALUES(?,?,?,?,'卡座','card_seat',4,0,0,'CNY','active',now(),now(),?)",
                table, bar, bar, "K" + n, space);
        String session = "r344ses_" + n;
        jdbc.update("INSERT INTO bar_table_sessions(id,bar_id,merchant_id,business_date,table_id,status,opened_at,created_at,updated_at)"
                + " VALUES(?,?,?,CURRENT_DATE,?,'open',now(),now(),now())", session, bar, bar, table);
        String product = "r344prd_" + n;
        jdbc.update("INSERT INTO bar_products(id,bar_id,merchant_id,product_name,product_type,price_cent,status,created_at,updated_at)"
                + " VALUES(?,?,?,'回归啤酒','beer',5000,'active',now(),now())", product, bar, bar);
        String order = "r344ord_" + n;
        jdbc.update("INSERT INTO bar_orders(id,bar_id,merchant_id,table_id,order_source,status,total_amount_cent,currency,created_at,updated_at)"
                + " VALUES(?,?,?,?,'staff','submitted',5000,'CNY',now(),now())", order, bar, bar, table);
        String item = "r344itm_" + n;
        jdbc.update("INSERT INTO bar_order_items(id,order_id,product_id,product_name,quantity,unit_price_cent,amount_cent,delivery_status,created_at,updated_at)"
                + " VALUES(?,?,?,'回归啤酒',1,5000,5000,'delivered',now(),now())", item, order, product);
        String bill = "r344bil_" + n;
        jdbc.update("INSERT INTO bar_bills(id,bill_no,order_id,bar_id,merchant_id,table_id,subtotal_amount_cent,discount_amount_cent,deposit_deduct_amount_cent,"
                + "payable_amount_cent,paid_amount_cent,currency,payment_status,settlement_status,tip_amount_cent,created_at,updated_at)"
                + " VALUES(?,?,?,?,?,?,5000,0,0,5000,0,'CNY','unpaid','unsettled',0,now(),now())", bill, "B" + n, order, bar, bar, table);
        String voidId = "r344vd_" + n;
        jdbc.update("INSERT INTO bar_order_item_voids(id,order_item_id,order_id,bar_id,merchant_id,void_type,quantity,amount_cent,was_delivered,"
                + "bill_id,reason,requested_by_user_id,status,idempotency_key,created_at) VALUES(?,?,?,?,?,'return',1,5000,true,?,'客人退品',?,'pending',?,now())",
                voidId, item, order, bar, bar, bill, "r344req_" + n, "r344-void-" + n);
        String shift = "r344sft_" + n;
        jdbc.update("INSERT INTO bar_shifts(id,project_id,merchant_id,bar_id,cashier_user_id,status,opened_at,opened_by_user_id,created_at,updated_at)"
                + " VALUES(?,?,?,?,?,'open',now(),?,now(),now())", shift, project, bar, bar, "r344csh_" + n, "r344csh_" + n);
        jdbc.update("INSERT INTO bar_order_suspensions(id,order_id,bill_id,bar_id,merchant_id,project_id,table_id,status,amount_cent,reason,"
                + "suspended_by,suspended_at,updated_at) VALUES(?,?,?,?,?,?,?,'hanging_unpaid',5000,'包厢客人暂离',?,now(),now())",
                "r344sus_" + n, order, bill, bar, bar, project, table, "r344csh_" + n);
        String rule = "r344bpr_" + n;
        jdbc.update("INSERT INTO bar_payroll_rules(id,rule_kind,project_id,merchant_id,effective_from,version,rule_json,reason,status,platform_company_id,created_by)"
                + " VALUES(?,'charge',?,NULL,?,'V0.01',?::jsonb,'服务费 10%','approved',?,?)", rule, project,
                java.sql.Date.valueOf(LocalDate.now().minusDays(1)),
                json.writeValueAsString(new BarPayrollRequests.RuleValues(1000, 0, 2000, null, null, null, List.of())), "r344ent_" + n, "r344crt_" + n);
        String inventoryItem = "r344inv_" + n;
        jdbc.update("INSERT INTO inventory_items(id,sku_code,item_name,item_type,unit,trace_required,min_quantity,status,created_at,updated_at)"
                + " VALUES(?,?,'回归威士忌','wine','bottle',false,0,'active',now(),now())", inventoryItem, inventoryItem);
        String bottle = "r344sb_" + n;
        jdbc.update("INSERT INTO saved_bottles(id,bar_project_id,item_id,bottle_name,initial_quantity,remaining_quantity,unit,expire_at,status,"
                + "created_at,updated_at,customer_name_or_nickname,merchant_id) VALUES(?,?,?,'回归威士忌',1,1,'bottle',now() + interval '30 days','stored',"
                + "now(),now(),'王先生',?)", bottle, bar, inventoryItem, bar);
        return new Bar(project, bar, session, order, bill, voidId, shift, rule, bottle);
    }

    // ---------- 断言与请求 ----------

    private <T> List<T> read(RequestPostProcessor who, String url, String path) throws Exception {
        String body = ok(who, url).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, path);
    }

    private ResultActions ok(RequestPostProcessor who, String url) throws Exception {
        return mockMvc.perform(get(url).with(who)).andExpect(status().isOk());
    }

    /** 最高权限本人（保留超管账号，组织 platform-company、无商户），经后台 Web 终端。 */
    private RequestPostProcessor master() {
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
