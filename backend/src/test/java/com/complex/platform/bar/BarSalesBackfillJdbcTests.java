package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
 * 第十八批 F 段第 14 条 14-R3 销售归属补录真库、真接口回归（请求体按前端蛇形键）。依据：BSM 第 1642 行「以通话录音为凭证的，录音起始时间必须早于客人
 * 到店时间、且早于客人开卡时间达到后台配置的时长……不满足的不予采信……涉及金额、销售归属、提成或责任认定的补录，须由被影响方之外的主管复核」；
 * 第 1021 行「由有销售归属的订台开台的，自动记营销带客并带出该销售人员；以上都不命中的直接到店开台默认记自来客……来源与销售人员的事后改派须店长复核
 * 并留痕，只能在该账单结清前办理」；第 870 行「开卡人（订下这个卡座的人）」；第 1070 行「销售人员可跨酒吧承接订单」；第 1468 行「销售归属改派后，
 * 业绩随提成归属一起改」。到店＝开台时刻，开卡＝订台时刻（直接到店开台即开台时刻）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarSalesBackfillJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BarBillingService billing;
    @Autowired private PlatformTransactionManager transactions;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "prj_bsb_" + suffix;
    private final String merchant = "mch_bsb_" + suffix;
    private final String otherMerchant = "mch_bsb_o_" + suffix;
    private final AtomicInteger seq = new AtomicInteger();
    private RequestPostProcessor clerk;
    private RequestPostProcessor mid;
    private RequestPostProcessor manager;
    private RequestPostProcessor plain;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                project, "销售补录项目 " + suffix);
        for (String m : List.of(merchant, otherMerchant)) {
            jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id) "
                    + "VALUES (?, ?, 'bar', 'active', now(), now(), ?)", m, "酒吧 " + m, project);
        }
        String staffRole = role("st", "'bar.table_session.create','bar.table_session.view','evidence.attachment.create',"
                + "'governance.supervisor_review.view','governance.supervisor_review.approve'");
        String managerRole = role("mg", "'bar.table_session.create','bar.table_session.view','evidence.attachment.create',"
                + "'governance.supervisor_review.view','governance.supervisor_review.approve','bar.sales_backfill.review'");
        clerk = account("clerk", merchant, staffRole);
        mid = account("mid", merchant, staffRole);
        manager = account("mgr", merchant, managerRole);
        plain = account("plain", merchant, staffRole);
        account("sales", merchant, staffRole);
        account("osales", otherMerchant, staffRole);
        account("nopro", merchant, staffRole);
        account("gone", merchant, staffRole);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", user("clerk"), user("mid"));
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", user("mid"), user("mgr"));
        profile("sales", merchant, "employed", true);
        profile("osales", otherMerchant, "employed", true);
        profile("gone", merchant, "departed", true);
        profile("mid", merchant, "employed", false);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 通话录音：直接到店开台的，到店与开卡都按开台时刻——附 .m4a 不填起始 400、起始早于开台 20 分（出厂 30）400 不予采信、早 40 分进复核；
     * 带订台开台的，开卡按订台时刻——起始早于开台 40 分但晚于订台仍 400；只附截图不做录音校验。
     */
    @Test
    void recordingMustStartEarlyEnoughBeforeArrivalAndOpenCard() throws Exception {
        Instant opened = Instant.now().minus(Duration.ofHours(1)).truncatedTo(ChronoUnit.SECONDS);
        String walkIn = session(opened, null, null, null);
        String audio = upload("call.m4a");
        submit(walkIn, "r0", user("sales"), List.of(audio), null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("RECORDING_START_REQUIRED"));
        submit(walkIn, "r1", user("sales"), List.of(audio), opened.minus(Duration.ofMinutes(20)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("RECORDING_NOT_EARLY_ENOUGH"));
        submit(walkIn, "r2", user("sales"), List.of(audio), opened.minus(Duration.ofMinutes(40)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.target_type").value("bar_session_sales"))
                .andExpect(jsonPath("$.data.proposed.sales_staff_id").value(user("sales")))
                .andExpect(jsonPath("$.data.project_id").value(project))
                .andExpect(jsonPath("$.data.merchant_id").value(merchant));

        Instant opened2 = Instant.now().minus(Duration.ofMinutes(30)).truncatedTo(ChronoUnit.SECONDS);
        String booked = session(opened2, opened2.minus(Duration.ofHours(2)), null, null);
        submit(booked, "r3", user("sales"), List.of(audio), opened2.minus(Duration.ofMinutes(40)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("RECORDING_NOT_EARLY_ENOUGH"));
        String photo = upload("wechat.png");
        submit(booked, "r4", user("sales"), List.of(photo), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("pending"));
    }

    /** 不是缺失：会话或其订台已有销售 409；账单已全部结清或已关台（BSM 第 1021 行「只能在该账单结清前办理」）409。都不落补录单。 */
    @Test
    void presentAttributionOrSettledBillIsRefused() throws Exception {
        Instant opened = Instant.now().minus(Duration.ofHours(1));
        String photo = upload("wechat.png");
        submit(session(opened, null, null, user("sales")), "p1", user("osales"), List.of(photo), null)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("SALES_ATTRIBUTION_PRESENT"));
        submit(session(opened, opened.minus(Duration.ofHours(3)), user("sales"), null), "p2", user("osales"), List.of(photo), null)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("SALES_ATTRIBUTION_PRESENT"));
        String settled = session(opened, null, null, null);
        bill(order(settled, null), "settled");
        submit(settled, "p3", user("sales"), List.of(photo), null)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_BILL_ALREADY_SETTLED"));
        String closed = session(opened, null, null, null);
        jdbc.update("UPDATE bar_table_sessions SET status='closed', closed_at=now() WHERE id=?", closed);
        submit(closed, "p4", user("sales"), List.of(photo), null)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_BILL_ALREADY_SETTLED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM manual_backfills WHERE target_type='bar_session_sales' AND merchant_id=?",
                Integer.class, merchant)).isZero();
    }

    /** 拟归属人：别的酒吧的在职员工可以（BSM 第 1070 行「销售人员可跨酒吧承接订单」）；没有员工档案、已离职或不填 → 400。 */
    @Test
    void proposedSalesMustBeOnDutyStaffButMayWorkForAnotherBar() throws Exception {
        String s = session(Instant.now().minus(Duration.ofHours(1)), null, null, null);
        String photo = upload("wechat.png");
        submit(s, "s1", user("nopro"), List.of(photo), null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        submit(s, "s2", user("gone"), List.of(photo), null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        submit(s, "s3", null, List.of(photo), null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        submit(s, "s4", user("osales"), List.of(photo), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("pending"));
    }

    /**
     * 复核：先直接上级、后店长（经角色持有 bar.sales_backfill.review）；不持该权限的人裁店长节点 403。批准后会话、未结清账单与其订单、尚未开单的订单改归
     * 拟归属人（账单销售岗位档按其员工档案），已结清账单与其订单不动；之后开单、计提按新归属；补录记录与原快照并列可查。
     */
    @Test
    void storeManagerApprovalMovesAttributionOnUnsettledBillsOnly() throws Exception {
        String s = session(Instant.now().minus(Duration.ofHours(1)), null, null, null);
        String paidOrder = order(s, null);
        String paidBill = bill(paidOrder, "settled");
        String openOrder = order(s, null);
        String openBill = bill(openOrder, "unsettled");
        String laterOrder = order(s, null);
        String photo = upload("wechat.png");
        String body = submit(s, "ok", user("sales"), List.of(photo), null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending")).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String review = JsonPath.read(body, "$.data.review_id");
        assertThat(jdbc.queryForObject("SELECT action_type FROM supervisor_review_decisions WHERE review_id=?", String.class, review))
                .isEqualTo("bar.sales.backfill");

        approve(mid, review, "approved").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT sales_staff_id FROM bar_table_sessions WHERE id=?", String.class, s)).isNull();
        approve(plain, review, "approved").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SUPERVISOR_ROLE_NOT_AUTHORIZED"));
        approve(manager, review, "approved").andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT sales_staff_id FROM bar_table_sessions WHERE id=?", String.class, s)).isEqualTo(user("sales"));
        assertThat(jdbc.queryForObject("SELECT session_version FROM bar_table_sessions WHERE id=?", Long.class, s)).isEqualTo(2L);
        assertThat(jdbc.queryForObject("SELECT sales_staff_id FROM bar_orders WHERE id=?", String.class, paidOrder)).isNull();
        assertThat(jdbc.queryForMap("SELECT sales_staff_id, sales_role_type FROM bar_bills WHERE id=?", paidBill))
                .containsEntry("sales_staff_id", null).containsEntry("sales_role_type", "non_sales");
        assertThat(jdbc.queryForObject("SELECT sales_staff_id FROM bar_orders WHERE id=?", String.class, openOrder)).isEqualTo(user("sales"));
        assertThat(jdbc.queryForMap("SELECT sales_staff_id, sales_role_type FROM bar_bills WHERE id=?", openBill))
                .containsEntry("sales_staff_id", user("sales")).containsEntry("sales_role_type", "sales");
        assertThat(jdbc.queryForObject("SELECT sales_staff_id FROM bar_orders WHERE id=?", String.class, laterOrder)).isEqualTo(user("sales"));

        actAs("clerk");
        BarBillRecord later = billing.createBill(laterOrder, new BarOrderBillCreateRequest("bl-" + suffix, null, List.of(), null, null, null));
        assertThat(later.salesStaffId()).isEqualTo(user("sales"));
        assertThat(later.salesRoleType()).isEqualTo("sales");
        BarCommissionCalculationRecord commission = billing.calculateCommission(openBill,
                new CommissionCalculateRequest("cm-" + suffix, openBill, null, null));
        assertThat(commission.salesStaffId()).isEqualTo(user("sales"));
        assertThat(commission.salesRoleType()).isEqualTo("sales");

        mockMvc.perform(get("/api/v1/evidence/manual-backfills").param("target_type", "bar_session_sales").param("target_id", s).with(clerk))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("approved"))
                .andExpect(jsonPath("$.data[0].proposed.sales_staff_id").value(user("sales")))
                .andExpect(jsonPath("$.data[0].original_snapshot.orders.length()").value(3))
                .andExpect(jsonPath("$.data[0].original_snapshot.bills.length()").value(2));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='bar.sales_backfill_applied' AND business_id=?",
                Integer.class, s)).isEqualTo(1);
    }

    /** 拟归属销售本人不得复核（被影响方回避）。 */
    @Test
    void proposedSalesCannotReviewTheirOwnAttribution() throws Exception {
        String s = session(Instant.now().minus(Duration.ofHours(1)), null, null, null);
        String photo = upload("wechat.png");
        String body = submit(s, "bn", user("mid"), List.of(photo), null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending")).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        approve(mid, JsonPath.read(body, "$.data.review_id"), "approved").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
    }

    /** 批准时重核：同一会话两笔，先批的生效、后批的 409；批准前账单已全部结清的 409，整笔批准不生效。 */
    @Test
    void approvalIsRefusedWhenTheSessionChangedMeanwhile() throws Exception {
        String s = session(Instant.now().minus(Duration.ofHours(1)), null, null, null);
        bill(order(s, null), "unsettled");
        String photo = upload("wechat.png");
        String first = reviewOf(submit(s, "c1", user("sales"), List.of(photo), null));
        String second = reviewOf(submit(s, "c2", user("osales"), List.of(photo), null));
        approve(mid, first, "approved").andExpect(status().isOk());
        approve(manager, first, "approved").andExpect(status().isOk());
        approve(mid, second, "approved").andExpect(status().isOk());
        approveInSavepoint(second).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("MANUAL_BACKFILL_TARGET_CHANGED"));
        assertThat(jdbc.queryForObject("SELECT sales_staff_id FROM bar_table_sessions WHERE id=?", String.class, s)).isEqualTo(user("sales"));

        String t = session(Instant.now().minus(Duration.ofHours(1)), null, null, null);
        String tBill = bill(order(t, null), "unsettled");
        String third = reviewOf(submit(t, "c3", user("sales"), List.of(photo), null));
        approve(mid, third, "approved").andExpect(status().isOk());
        jdbc.update("UPDATE bar_bills SET settlement_status='settled', payment_status='paid' WHERE id=?", tBill);
        approveInSavepoint(third).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("MANUAL_BACKFILL_TARGET_CHANGED"));
        assertThat(jdbc.queryForObject("SELECT sales_staff_id FROM bar_table_sessions WHERE id=?", String.class, t)).isNull();
        assertThat(jdbc.queryForObject("SELECT status FROM manual_backfills WHERE review_id=?", String.class, third)).isEqualTo("pending");
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    private ResultActions submit(String session, String key, String sales, List<String> files, Instant recording) throws Exception {
        String ids = String.join(",", files.stream().map(f -> "\"" + f + "\"").toList());
        String body = "{\"idempotency_key\":\"bsb-" + key + "-" + suffix + "\""
                + (sales == null ? "" : ",\"sales_staff_id\":\"" + sales + "\"")
                + ",\"reason\":\"客人电话预约时由销售接待，开台漏记\",\"responsible_staff_id\":\"" + user("clerk") + "\""
                + ",\"evidence_attachment_ids\":[" + ids + "]"
                + (recording == null ? "" : ",\"recording_started_at\":\"" + recording + "\"") + "}";
        return mockMvc.perform(post("/api/v1/bar/table-sessions/" + session + "/sales-backfill").with(clerk)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String reviewOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("pending")).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
                "$.data.review_id");
    }

    private String upload(String fileName) throws Exception {
        String body = mockMvc.perform(multipart("/api/v1/evidence/attachments/upload")
                        .file(new MockMultipartFile("file", fileName, "application/octet-stream", new byte[]{1, 2, 3}))
                        .param("business_type", "manual_backfill").with(clerk))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.data.id");
    }

    private ResultActions approve(RequestPostProcessor who, String review, String decision) throws Exception {
        return mockMvc.perform(post("/api/v1/platform/supervisor-review/" + review + "/approve").with(who)
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"" + decision
                        + "\",\"comment\":\"已核对录音与预约记录\",\"idempotency_key\":\"ap_" + UUID.randomUUID() + "\"}"));
    }

    /** 店长批准放在保存点里：批准抛错标为只回滚时回到保存点，等同生产中整笔批准回滚。 */
    private ResultActions approveInSavepoint(String review) {
        TransactionTemplate nested = new TransactionTemplate(transactions);
        nested.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        ResultActions[] result = new ResultActions[1];
        try {
            nested.executeWithoutResult(tx -> {
                try {
                    result[0] = approve(manager, review, "approved");
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (UnexpectedRollbackException rolledBack) {
            // 预期：批准失败把事务标为只回滚，保存点已回滚
        }
        return result[0];
    }

    /** 每个会话一张桌台（铺位挂本项目本商户；散台不收服务费，开单无须收费规则）；bookedAt 非空即带订台开台（订台 created_at＝订下的时刻）。 */
    private String session(Instant openedAt, Instant bookedAt, String bookedSales, String sales) {
        String n = suffix + "_" + seq.incrementAndGet();
        String table = "tbl_bsb_" + n;
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at) "
                + "VALUES(?,?,?,'卡座区','unit','active',now(),now())", "spc_bsb_" + n, project, merchant);
        jdbc.update("INSERT INTO bar_tables(id,bar_id,merchant_id,table_code,table_name,table_type,capacity,allowed_pass_free_guest_count,"
                + "min_consumption_cent,currency,status,created_at,updated_at,space_id) VALUES(?,?,?,?,'桌台','table',4,0,0,'CNY','active',now(),now(),?)",
                table, merchant, merchant, "T" + n, "spc_bsb_" + n);
        String reservation = null;
        if (bookedAt != null) {
            reservation = "rsv_bsb_" + n;
            jdbc.update("INSERT INTO bar_reservations(id,bar_id,merchant_id,table_id,reserved_at,guest_count,deposit_status,order_source,sales_staff_id,"
                    + "status,created_at,updated_at) VALUES(?,?,?,?,?,2,'not_required','customer',?,'arrived',?,?)", reservation, merchant, merchant,
                    table, Timestamp.from(openedAt), bookedSales, Timestamp.from(bookedAt), Timestamp.from(bookedAt));
        }
        String id = "ses_bsb_" + n;
        jdbc.update("INSERT INTO bar_table_sessions(id,bar_id,merchant_id,business_date,table_id,reservation_id,sales_staff_id,status,opened_at,"
                + "created_at,updated_at) VALUES(?,?,?,CURRENT_DATE,?,?,?,'open',?,?,?)", id, merchant, merchant, table, reservation, sales,
                Timestamp.from(openedAt), Timestamp.from(openedAt), Timestamp.from(openedAt));
        return id;
    }

    private String order(String session, String sales) {
        String id = "ord_bsb_" + suffix + "_" + seq.incrementAndGet();
        String table = jdbc.queryForObject("SELECT table_id FROM bar_table_sessions WHERE id=?", String.class, session);
        jdbc.update("INSERT INTO bar_orders(id,bar_id,merchant_id,table_id,order_source,status,total_amount_cent,currency,sales_staff_id,created_at,updated_at) "
                + "VALUES(?,?,?,?,'staff','created',10000,'CNY',?,now(),now())", id, merchant, merchant, table, sales);
        jdbc.update("INSERT INTO bar_order_service_sources(order_id,session_id,table_id,project_id,merchant_id,business_date) VALUES(?,?,?,?,?,CURRENT_DATE)",
                id, session, table, project, merchant);
        return id;
    }

    private String bill(String order, String settlement) {
        String id = "bil_bsb_" + suffix + "_" + seq.incrementAndGet();
        String table = jdbc.queryForObject("SELECT table_id FROM bar_orders WHERE id=?", String.class, order);
        boolean paid = "settled".equals(settlement);
        jdbc.update("INSERT INTO bar_bills(id,bill_no,order_id,bar_id,merchant_id,table_id,subtotal_amount_cent,discount_amount_cent,deposit_deduct_amount_cent,"
                + "payable_amount_cent,paid_amount_cent,currency,payment_status,settlement_status,sales_role_type,created_at,updated_at) "
                + "VALUES(?,?,?,?,?,?,10000,0,0,10000,?,'CNY',?,?,'non_sales',now(),now())", id, "B" + id, order, merchant, merchant, table,
                paid ? 10000L : 0L, paid ? "paid" : "unpaid", settlement);
        return id;
    }

    private String role(String tag, String permissions) {
        String role = "role_bsb_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'销售补录回归','project','active',now())", role, role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN("
                + permissions + ")", role);
        return role;
    }

    private RequestPostProcessor account(String tag, String merchantId, String role) {
        String userId = user(tag);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",
                userId, userId, project, merchantId);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId, project);
        AccessActor actor = new AccessActor(userId, userId, "staff", project, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private void profile(String tag, String merchantId, String employment, boolean salesPosition) {
        jdbc.update("INSERT INTO staff_profiles(id,project_id,merchant_id,user_id,name,registration_status,status,employment_status,is_sales_position,"
                + "created_at,updated_at) VALUES(?,?,?,?,'在岗员工','approved','active',?,?,now(),now())", "pro_bsb_" + tag + "_" + suffix, project,
                merchantId, user(tag), employment, salesPosition);
    }

    private void actAs(String tag) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(user(tag), user(tag), "staff", project, merchant, "active"), "test", List.of()));
    }

    private String user(String tag) {
        return "usr_bsb_" + tag + "_" + suffix;
    }
}
