package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.LocalZone;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 第十八批 F 段第 14 条 14-R6 服务责任区间补录真库、真接口回归（请求体按前端蛇形键）。依据：BSM 第 1642 行「记录缺失时允许人工补录，但必须写原因和责任人……
 * 涉及金额、销售归属、提成或责任认定的补录，须由被影响方之外的主管复核。补录记录与原始记录并列展示，标明“人工补录”」；第 1613 行「卡座或桌台由谁主责服务……
 * 是否中途换人」；第 1077 行「小费按各人实际负责该台的时长占合计实际负责时长的比例分配，以真实交接记录为依据」。开台未登记服务员、之后才交接的，
 * 开台到首次交接是空档，卡座小费月结待结；补录复核通过后补上这段区间，小费按实际负责时长分配。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarServiceBackfillJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BarPayrollAllocationService allocations;
    @Autowired private PlatformTransactionManager transactions;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "prj_bsv_" + suffix;
    private final String merchant = "mch_bsv_" + suffix;
    private final String company = "ent_bsv_" + suffix;
    private final AtomicInteger seq = new AtomicInteger();
    private RequestPostProcessor clerk;
    private RequestPostProcessor mid;
    private RequestPostProcessor waiter;
    private RequestPostProcessor viewer;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                project, "服务补录项目 " + suffix);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'经营主体','management_company','active',now(),now())",
                company);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,"
                + "created_at,updated_at) VALUES(?,'服务补录酒吧','bar',?,?,?,?,'active',now(),now())", merchant, company, company, company, project);
        String staffRole = role("st", "'bar.table_session.transfer','bar.payroll.view','bar.payroll.manage','bar.order.create',"
                + "'evidence.attachment.create','governance.supervisor_review.view','governance.supervisor_review.approve'");
        clerk = account("clerk", staffRole);
        mid = account("mid", staffRole);
        waiter = account("waiter", staffRole);
        viewer = account("viewer", role("vw", "'bar.table_session.view'"));
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", user("clerk"), user("mid"));
        jdbc.update("INSERT INTO staff_profiles(id,project_id,merchant_id,user_id,name,company_id,registration_status,status,employment_status,"
                + "created_at,updated_at) VALUES(?,?,?,?,'在岗服务员',?,'approved','active','employed',now(),now())",
                "pro_bsv_" + suffix, project, merchant, user("waiter"), company);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 卡座开台未登记服务员，开台 30 分钟后交接给服务员（真实入口）：补录区间超出开台或开始不早于结束 400；与未结束的交接区间重叠 409；
     * 补开台到交接这段 → 待复核；被补为责任人的服务员本人不得复核；发起人上级复核通过后多一条标「人工补录」的区间（登记人为提交人），
     * 原交接区间不变，两段并列返回；补录记录可查，没有工资查看权限的账号看不到。
     */
    @Test
    void gapBeforeFirstHandoverIsBackfilledBesideTheOriginalInterval() throws Exception {
        Instant opened = Instant.now().minus(Duration.ofHours(1)).truncatedTo(ChronoUnit.SECONDS);
        Instant handover = opened.plus(Duration.ofMinutes(30));
        String s = session(opened, null, null, LocalDate.now(LocalZone.ID));
        mockMvc.perform(post("/api/v1/bar/table-sessions/" + s + "/service-handover").with(clerk).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"ho-" + suffix + "\",\"next_user_id\":\"" + user("waiter")
                                + "\",\"session_version\":1,\"reason\":\"开台未登记服务员，由当班服务员接手\"}"))
                .andExpect(status().isOk());
        jdbc.update("UPDATE bar_service_responsibilities SET started_at=? WHERE session_id=?", Timestamp.from(handover), s);
        String photo = upload("wechat.png");

        submit(s, "early", opened.minus(Duration.ofMinutes(1)), handover, photo)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("SERVICE_INTERVAL_INVALID"));
        submit(s, "reverse", handover, opened, photo)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("SERVICE_INTERVAL_INVALID"));
        submit(s, "overlap", opened, handover.plus(Duration.ofMinutes(10)), photo)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("SERVICE_INTERVAL_OVERLAP"));
        String body = submit(s, "ok", opened, handover, photo).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.target_type").value("bar_session_service"))
                .andExpect(jsonPath("$.data.proposed.user_id").value(user("waiter")))
                .andExpect(jsonPath("$.data.original_snapshot.length()").value(1))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String review = JsonPath.read(body, "$.data.review_id");
        String backfill = JsonPath.read(body, "$.data.id");
        assertThat(jdbc.queryForObject("SELECT action_type FROM supervisor_review_decisions WHERE review_id=?", String.class, review))
                .isEqualTo("bar.service.backfill");

        approve(waiter, review).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
        approve(mid, review).andExpect(status().isOk());

        List<Map<String, Object>> rows = jdbc.queryForList("SELECT user_id,started_at,ended_at,source_id,recorded_by,reason "
                + "FROM bar_service_responsibilities WHERE session_id=? ORDER BY started_at", s);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsEntry("user_id", user("waiter")).containsEntry("source_id", "manual_backfill:" + backfill)
                .containsEntry("recorded_by", user("clerk"));
        assertThat(((Timestamp) rows.get(0).get("started_at")).toInstant()).isEqualTo(opened);
        assertThat(((Timestamp) rows.get(0).get("ended_at")).toInstant()).isEqualTo(handover);
        assertThat((String) rows.get(0).get("reason")).contains("人工补录");
        assertThat(((Timestamp) rows.get(1).get("started_at")).toInstant()).isEqualTo(handover);
        assertThat(rows.get(1)).containsEntry("ended_at", null).containsEntry("reason", "开台未登记服务员，由当班服务员接手");

        mockMvc.perform(get("/api/v1/bar/table-sessions/" + s + "/service-responsibilities").with(clerk))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].reason").value(containsString("人工补录")));
        mockMvc.perform(get("/api/v1/evidence/manual-backfills").param("target_type", "bar_session_service").param("target_id", s).with(clerk))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("approved"))
                .andExpect(jsonPath("$.data[0].original_snapshot.length()").value(1));
        mockMvc.perform(get("/api/v1/evidence/manual-backfills").param("target_type", "bar_session_service").param("target_id", s).with(viewer))
                .andExpect(status().isForbidden());
    }

    /** 已关台卡座：开台到首次交接的空档使小费月结待结（「空档」）；补录复核通过后月结完成，服务员得全部小费。 */
    @Test
    void approvedBackfillClosesTheGapSoTheCardSeatTipIsAllocated() throws Exception {
        YearMonth month = YearMonth.now(LocalZone.ID).minusMonths(2);
        LocalDate day = month.atDay(15);
        Instant opened = day.atTime(20, 0).atZone(LocalZone.ID).toInstant();
        Instant handover = opened.plus(Duration.ofMinutes(30));
        Instant closed = opened.plus(Duration.ofHours(6));
        // 本项目补录时限配置为 90 天（业务发生在两个月前）
        jdbc.update("INSERT INTO manual_backfill_policies(project_id,window_days,window_min_days,window_max_days,recording_lead_minutes,updated_by) "
                + "VALUES(?,90,1,120,30,'fixture')", project);
        String s = session(opened, closed, user("waiter"), day);
        jdbc.update("INSERT INTO bar_service_responsibilities(id,session_id,table_id,project_id,merchant_id,user_id,payer_company_id,started_at,ended_at,"
                + "source_id,recorded_by,reason) SELECT ?,id,table_id,?,merchant_id,?,?,?,?,?,?,'真实交接' FROM bar_table_sessions WHERE id=?",
                "bsr_bsv_" + suffix, project, user("waiter"), company, Timestamp.from(handover), Timestamp.from(closed), "src_bsv_" + suffix,
                user("clerk"), s);
        tipBill(s, day, closed);
        actAs("clerk");
        assertThat(allocations.overview(project, merchant, month.toString()))
                .anyMatch(run -> run.unresolvedSources().stream().anyMatch(reason -> reason.contains("空档")));

        String review = reviewOf(submit(s, "pay", opened, handover, upload("wechat.png")));
        approve(mid, review).andExpect(status().isOk());

        actAs("clerk");
        assertThat(allocations.close(new BarPayrollRequests.CloseMonth("close-" + suffix, project, merchant, month.toString())))
                .singleElement().satisfies(run -> {
                    assertThat(run.status()).isEqualTo("confirmed");
                    assertThat(run.payableTipCent()).isEqualTo(20000L);
                });
        assertThat(allocations.confirmedAllocations(user("waiter"), month.toString()))
                .singleElement().extracting(BarPayrollAllocation::amountCent).isEqualTo(20000L);
    }

    /** 补录时限按开台起算，出厂 3 天：开台 4 天前的空档 409 不得补录。 */
    @Test
    void backfillAfterTheWindowIsRefused() throws Exception {
        Instant opened = Instant.now().minus(Duration.ofDays(4)).truncatedTo(ChronoUnit.SECONDS);
        String s = session(opened, null, null, LocalDate.now(LocalZone.ID));
        submit(s, "late", opened, opened.plus(Duration.ofHours(1)), upload("wechat.png"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("MANUAL_BACKFILL_WINDOW_EXPIRED"));
    }

    /** 批准时重核：同一空档两笔重叠的补录，先批的生效，后批的 409、整笔批准不生效。 */
    @Test
    void approvalIsRefusedWhenTheGapWasFilledMeanwhile() throws Exception {
        Instant opened = Instant.now().minus(Duration.ofHours(2)).truncatedTo(ChronoUnit.SECONDS);
        String s = session(opened, null, null, LocalDate.now(LocalZone.ID));
        String photo = upload("wechat.png");
        String first = reviewOf(submit(s, "c1", opened, opened.plus(Duration.ofMinutes(30)), photo));
        String second = reviewOf(submit(s, "c2", opened.plus(Duration.ofMinutes(10)), opened.plus(Duration.ofMinutes(40)), photo));
        approve(mid, first).andExpect(status().isOk());
        approveInSavepoint(second).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("MANUAL_BACKFILL_TARGET_CHANGED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_service_responsibilities WHERE session_id=?", Integer.class, s)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM manual_backfills WHERE review_id=?", String.class, second)).isEqualTo("pending");
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    private ResultActions submit(String session, String key, Instant start, Instant end, String file) throws Exception {
        String body = "{\"idempotency_key\":\"bsv-" + key + "-" + suffix + "\",\"user_id\":\"" + user("waiter") + "\""
                + ",\"started_at\":\"" + start + "\",\"ended_at\":\"" + end + "\""
                + ",\"reason\":\"开台时未登记服务员，实际由该服务员接待\",\"responsible_staff_id\":\"" + user("clerk") + "\""
                + ",\"evidence_attachment_ids\":[\"" + file + "\"]}";
        return mockMvc.perform(post("/api/v1/bar/table-sessions/" + session + "/service-backfill").with(clerk)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String reviewOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("pending")).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8), "$.data.review_id");
    }

    private String upload(String fileName) throws Exception {
        String body = mockMvc.perform(multipart("/api/v1/evidence/attachments/upload")
                        .file(new MockMultipartFile("file", fileName, "application/octet-stream", new byte[]{1, 2, 3}))
                        .param("business_type", "manual_backfill").with(clerk))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.data.id");
    }

    private ResultActions approve(RequestPostProcessor who, String review) throws Exception {
        return mockMvc.perform(post("/api/v1/platform/supervisor-review/" + review + "/approve").with(who)
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"approved\",\"comment\":\"已核对监控与点单记录\","
                        + "\"idempotency_key\":\"ap_" + UUID.randomUUID() + "\"}"));
    }

    /** 批准放在保存点里：批准抛错标为只回滚时回到保存点，等同生产中整笔批准回滚。 */
    private ResultActions approveInSavepoint(String review) {
        TransactionTemplate nested = new TransactionTemplate(transactions);
        nested.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        ResultActions[] result = new ResultActions[1];
        try {
            nested.executeWithoutResult(tx -> {
                try {
                    result[0] = approve(mid, review);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (UnexpectedRollbackException rolledBack) {
            // 预期：批准失败把事务标为只回滚，保存点已回滚
        }
        return result[0];
    }

    /** 每个会话一张卡座（铺位挂本项目本商户）；closedAt 非空即已关台。 */
    private String session(Instant openedAt, Instant closedAt, String serviceStaff, LocalDate businessDate) {
        String n = suffix + "_" + seq.incrementAndGet();
        String table = "tbl_bsv_" + n;
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at) "
                + "VALUES(?,?,?,'卡座区','unit','active',now(),now())", "spc_bsv_" + n, project, merchant);
        jdbc.update("INSERT INTO bar_tables(id,bar_id,merchant_id,table_code,table_name,table_type,capacity,allowed_pass_free_guest_count,"
                + "min_consumption_cent,currency,status,created_at,updated_at,space_id) VALUES(?,?,?,?,'卡座','card_seat',4,0,0,'CNY','active',now(),now(),?)",
                table, merchant, merchant, "K" + n, "spc_bsv_" + n);
        String id = "ses_bsv_" + n;
        jdbc.update("INSERT INTO bar_table_sessions(id,bar_id,merchant_id,business_date,table_id,service_staff_id,status,opened_at,closed_at,"
                + "created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)", id, merchant, merchant, businessDate, table, serviceStaff,
                closedAt == null ? "open" : "closed", Timestamp.from(openedAt), closedAt == null ? null : Timestamp.from(closedAt),
                Timestamp.from(openedAt), Timestamp.from(openedAt));
        return id;
    }

    /** 已真实收款的账单：商品 1000 元、小费 200 元，场次来源、小费收费原件与收款关联齐全（同 BarPayrollAllocationJdbcIntegrationTests 夹具）。 */
    private void tipBill(String session, LocalDate day, Instant paidAt) {
        String n = suffix + "_" + seq.incrementAndGet();
        String table = jdbc.queryForObject("SELECT table_id FROM bar_table_sessions WHERE id=?", String.class, session);
        String order = "ord_bsv_" + n;
        String bill = "bil_bsv_" + n;
        jdbc.update("INSERT INTO bar_orders(id,bar_id,merchant_id,table_id,order_source,status,total_amount_cent,currency,created_at,updated_at) "
                + "VALUES(?,?,?,?,'staff','created',100000,'CNY',?,?)", order, merchant, merchant, table, Timestamp.from(paidAt), Timestamp.from(paidAt));
        jdbc.update("INSERT INTO bar_bills(id,bill_no,order_id,bar_id,merchant_id,table_id,subtotal_amount_cent,discount_amount_cent,deposit_deduct_amount_cent,"
                + "payable_amount_cent,paid_amount_cent,currency,payment_status,settlement_status,tip_amount_cent,created_at,updated_at) "
                + "VALUES(?,?,?,?,?,?,100000,0,0,100000,120000,'CNY','paid','settled',20000,?,?)", bill, "B" + n, order, merchant, merchant, table,
                Timestamp.from(paidAt), Timestamp.from(paidAt));
        jdbc.update("INSERT INTO bar_bill_settlements(id,bill_id,settle_channel,settlement_amount_cent,merchant_id,settled_at) VALUES(?,?,'aggregate_pay',120000,?,?)",
                "pay_bsv_" + n, bill, merchant, Timestamp.from(paidAt));
        jdbc.update("INSERT INTO bar_order_service_sources(order_id,session_id,table_id,project_id,merchant_id,business_date) VALUES(?,?,?,?,?,?)",
                order, session, table, project, merchant, day);
        jdbc.update("INSERT INTO bar_payroll_bill_components(id,source_bill_id,session_id,source_table_id,project_id,merchant_id,business_date,component_type,"
                + "amount_cent,original_base_cent,source_event_id,source_snapshot,collection_bill_id) VALUES(?,?,?,?,?,?,?,'tip',20000,20000,?,'[]'::jsonb,?)",
                "cmp_bsv_" + n, bill, session, table, project, merchant, day, "evt_bsv_" + n, bill);
        jdbc.update("INSERT INTO bar_payroll_collection_links(source_bill_id,collection_bill_id) VALUES(?,?)", bill, bill);
    }

    private String role(String tag, String permissions) {
        String role = "role_bsv_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'服务补录回归','project','active',now())", role, role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN("
                + permissions + ")", role);
        return role;
    }

    private RequestPostProcessor account(String tag, String role) {
        String userId = user(tag);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",
                userId, userId, project, merchant);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId, project);
        AccessActor actor = new AccessActor(userId, userId, "staff", project, merchant, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private void actAs(String tag) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(user(tag), user(tag), "staff", project, merchant, "active"), "test", List.of()));
    }

    private String user(String tag) {
        return "usr_bsv_" + tag + "_" + suffix;
    }
}
