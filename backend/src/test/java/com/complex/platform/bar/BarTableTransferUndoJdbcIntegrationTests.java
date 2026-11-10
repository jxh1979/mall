package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerSessionRecord;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 第十八批 AB 段第 190 条 190-R3：撤销最近一次转台（真库、真接口、真授权链，请求体按前端蛇形键）。
 * 依据：BSM:1079「撤销转台：仅当转台后未新增邀请、未发生邀请接受、成员入场或移出，也未新增点单、付款、退款等账单或资金记录时，方可按转台前快照恢复
 * 原台位、邀请关系、成员归属、可邀请人数、最低消费额和消费进度，并完整留痕；符合条件的撤销不得以新建单据冒充回退」「撤销转台前，还必须确认转台前涉及的
 * 全部原台均未被其他会话重新预订或开台」「再次转台、并台、拆台或任何邀请、成员、订单、付款、退款变化，均属于后续变化。撤销只能恢复最近一次仍满足条件的
 * 完整快照」；ENUM:114 同句；BSM:1062「撤销转台不得覆盖已经发生的资金记录」；决策台账 DEC-R6-19「撤销转台＝按原路回退到转台前的台位与进度并留痕，
 * 不得以新建单据代替回退」。
 * 不开测试事务：各请求各自提交、数据库时刻按请求取，才判得出转台时刻之后的变化（数据带随机数字后缀，留在一次性测试库）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "spring.task.scheduling.enabled=false"
})
@AutoConfigureMockMvc
class BarTableTransferUndoJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;

    private final String suffix = String.valueOf(Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 10_000_000_000L));
    private final String project = "prj_tund_" + suffix;
    private final String merchant = "mch_tund_" + suffix;
    private final String company = "ent_tund_" + suffix;
    private final String product = "prd_tund_" + suffix;
    private final AtomicInteger seq = new AtomicInteger();
    private final Set<String> createdCustomers = new HashSet<>();
    private RequestPostProcessor clerk;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                project, "撤销转台项目" + suffix);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'经营主体','management_company','active',now(),now())",
                company);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,"
                + "created_at,updated_at) VALUES(?,'撤销转台酒吧','bar',?,?,?,?,'active',now(),now())", merchant, company, company, company, project);
        jdbc.update("INSERT INTO bar_products (id, bar_id, merchant_id, product_name, product_type, price_cent, currency,"
                + " stock_deduction_required, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, '精酿啤酒', 'beer', 3800, 'CNY', FALSE, 'active', now(), now())", product, merchant, merchant);
        String role = "role_tund_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'撤销转台回归','project','active',now())",
                role, "撤销转台现场岗" + suffix);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN("
                + "'bar.table_session.create','bar.table_session.view','bar.table_session.transfer','bar.table_session.merge',"
                + "'bar.order.create','bar.reservation.invitation.manage')", role);
        clerk = account("k", role);
        for (String tag : List.of("t", "s")) {
            account(tag, role);
            jdbc.update("INSERT INTO staff_profiles(id,project_id,merchant_id,user_id,name,company_id,registration_status,status,employment_status,"
                    + "is_sales_position,created_at,updated_at) VALUES(?,?,?,?,?,?,'approved','active','employed',false,now(),now())",
                    "pro_tund_" + tag + "_" + suffix, project, merchant, user(tag), ("t".equals(tag) ? "接手服务员" : "当班服务员") + suffix, company);
        }
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 单台 A（低消 3000、4 人）开台时当班服务员 s、转台前已点一单，转到 B（低消 8000、6 人，接手 t）后撤销：转台前的订单不挡撤销；会话回到 A、
     * 低消与可邀请人数快照回 A 的、服务人回 s（台组现区间结束，s 在 A 的新区间开始）；A 已开台、B 空闲；新增一条撤销转台留痕（从 B 回 A、带原因与
     * 操作人、记被撤销的转台留痕编号），原转台留痕不变，本会话转台留痕仍 1 条（撤销不减次数）。
     */
    @Test
    void undoSingleTransferRestoresOriginalTableSnapshotsAndServiceStaff() throws Exception {
        String a = table(4, 3000);
        String b = table(6, 8000);
        shift("s", spaceOf(a));
        String session = open(a, null);
        assertThat(session(session)).containsEntry("service_staff_id", user("s"));
        order(a).andExpect(status().isOk());
        transfer(session, b).andExpect(status().isOk());
        Map<String, Object> transferred = jdbc.queryForMap("SELECT id, snapshot_json::text AS snapshot FROM bar_table_session_adjustments"
                + " WHERE session_id = ? AND adjustment_type = 'transfer'", session);

        undo(session, "转错台，回原台")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adjustment_type").value("transfer_undo"))
                .andExpect(jsonPath("$.data.from_table_id").value(b))
                .andExpect(jsonPath("$.data.to_table_id").value(a));

        assertThat(session(session)).containsEntry("table_id", a).containsEntry("min_consumption_snapshot_cent", 3000L)
                .containsEntry("allowed_pass_free_guest_count_snapshot", 4).containsEntry("service_staff_id", user("s"))
                .containsEntry("status", "open");
        assertThat(tableStatus(a)).isEqualTo("opened");
        assertThat(tableStatus(b)).isEqualTo("active");
        Map<String, Object> undone = jdbc.queryForMap("SELECT from_table_id, to_table_id, reason, requested_by_user_id,"
                + " min_consumption_before_cent, min_consumption_after_cent, snapshot_json->>'undone_adjustment_id' AS undone_id"
                + " FROM bar_table_session_adjustments WHERE session_id = ? AND adjustment_type = 'transfer_undo'", session);
        assertThat(undone).containsEntry("from_table_id", b).containsEntry("to_table_id", a).containsEntry("reason", "转错台，回原台")
                .containsEntry("requested_by_user_id", user("k")).containsEntry("min_consumption_before_cent", 8000L)
                .containsEntry("min_consumption_after_cent", 3000L).containsEntry("undone_id", transferred.get("id"));
        assertThat(jdbc.queryForObject("SELECT snapshot_json::text FROM bar_table_session_adjustments WHERE id = ?", String.class,
                transferred.get("id"))).isEqualTo(transferred.get("snapshot"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_table_session_adjustments WHERE session_id = ? AND adjustment_type = 'transfer'",
                Integer.class, session)).isEqualTo(1);
        assertThat(jdbc.queryForMap("SELECT user_id, table_id FROM bar_service_responsibilities WHERE session_id = ? AND ended_at IS NULL", session))
                .containsEntry("user_id", user("s")).containsEntry("table_id", a);
    }

    /**
     * A（低消 5000、4 人）与 B（低消 3000、2 人）并台、开台时都没指定服务人，整体转到 T（低消 9000、8 人）后撤销：A、B 已开台、T 空闲；
     * B 的会话回到并台（挂在 A 下），台组低消 8000、可邀请人数 6；服务人恢复为空，台组没有未结束的服务区间。
     */
    @Test
    void undoWholeGroupTransferRestoresMergedChainAndBlankServiceStaff() throws Exception {
        String a = table(4, 5000);
        String b = table(2, 3000);
        String t = table(8, 9000);
        String sa = open(a, null);
        String sb = open(b, null);
        merge(sa, sb).andExpect(status().isOk());
        transfer(sa, t).andExpect(status().isOk());
        assertThat(session(sb)).containsEntry("status", "merged_transferred");

        undo(sa, "整体转错台").andExpect(status().isOk());

        assertThat(session(sa)).containsEntry("table_id", a).containsEntry("status", "open").containsEntry("service_staff_id", null);
        assertThat(session(sb)).containsEntry("table_id", b).containsEntry("status", "merged").containsEntry("source_session_id", sa)
                .containsEntry("service_staff_id", null);
        assertThat(List.of(tableStatus(a), tableStatus(b), tableStatus(t))).containsExactly("opened", "opened", "active");
        assertThat(jdbc.queryForMap("SELECT sum(min_consumption_snapshot_cent)::bigint AS threshold, sum(allowed_pass_free_guest_count_snapshot) AS invite"
                + " FROM bar_table_sessions WHERE id IN (?, ?)", sa, sb)).containsEntry("threshold", 8000L).containsEntry("invite", 6L);
        assertThat(jdbc.queryForMap("SELECT min_consumption_after_cent, snapshot_json->>'invite_capacity_after' AS invite"
                + " FROM bar_table_session_adjustments WHERE session_id = ? AND adjustment_type = 'transfer_undo'", sa))
                .containsEntry("min_consumption_after_cent", 8000L).containsEntry("invite", "6");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_service_responsibilities WHERE session_id IN (?, ?) AND ended_at IS NULL",
                Integer.class, sa, sb)).isZero();
    }

    /** 转台后在目标台下单、或给转台前的订单加菜：409 BAR_TRANSFER_UNDO_BLOCKED，会话仍在目标台、原台仍空闲。 */
    @Test
    void orderOrAddedItemAfterTransferBlocksUndo() throws Exception {
        String a = table(4, 0);
        String b = table(4, 0);
        String session = open(a, null);
        transfer(session, b).andExpect(status().isOk());
        order(b).andExpect(status().isOk());
        blocked(undo(session, "回原台"));
        assertThat(session(session)).containsEntry("table_id", b);
        assertThat(tableStatus(a)).isEqualTo("active");

        String c = table(4, 0);
        String d = table(4, 0);
        String other = open(c, null);
        String orderId = CustomerTicketPaymentFixture.read(order(c).andExpect(status().isOk()), "$.data.id");
        transfer(other, d).andExpect(status().isOk());
        addItem(orderId).andExpect(status().isOk());
        blocked(undo(other, "回原台"));
        assertThat(session(other)).containsEntry("table_id", d);
    }

    /** 带订台开台、已签发邀请函，转台后被邀请人用原二维码接受：409 BAR_TRANSFER_UNDO_BLOCKED。 */
    @Test
    void invitationAcceptedAfterTransferBlocksUndo() throws Exception {
        String a = table(4, 0);
        String reservation = reserve(a, 0);
        Map<String, Object> issued = issue(reservation);
        String session = open(a, reservation);
        transfer(session, table(4, 0)).andExpect(status().isOk());
        confirm(issued, a, "a").andExpect(status().isOk());
        blocked(undo(session, "回原台"));
    }

    /** 带已付定金订台在 A（低消 5000）开台未消费，转到低消 0 的台当场达标、挂出定金原路退款应付：撤销 409 BAR_TRANSFER_UNDO_BLOCKED。 */
    @Test
    void depositRefundTriggeredByTransferBlocksUndo() throws Exception {
        String a = table(4, 5000);
        String reservation = reserve(a, 2000);
        String session = open(a, reservation);
        transfer(session, table(4, 0)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE source_type = 'bar_deposit_refund' AND source_id = ?",
                Integer.class, reservation)).isEqualTo(1);
        blocked(undo(session, "回原台"));
    }

    /** 转台后原台被别的订台占住、被别的客人开台或已停用：409 BAR_TRANSFER_UNDO_TABLE_OCCUPIED，会话仍在目标台。 */
    @Test
    void originalTableTakenBlocksUndo() throws Exception {
        String a = table(4, 0);
        String b = table(4, 0);
        String session = open(a, null);
        transfer(session, b).andExpect(status().isOk());
        String held = "rsv_tund_" + suffix + "_" + seq.incrementAndGet();
        jdbc.update("INSERT INTO bar_reservations(id,bar_id,merchant_id,table_id,reserved_at,guest_count,deposit_amount_cent,deposit_status,"
                + "order_source,status,hold_status,created_at,updated_at) VALUES(?,?,?,?,now(),2,0,'not_required','miniapp','reserved','held',now(),now())",
                held, merchant, merchant, a);
        occupied(undo(session, "回原台"));
        assertThat(session(session)).containsEntry("table_id", b);

        String c = table(4, 0);
        String other = open(c, null);
        transfer(other, table(4, 0)).andExpect(status().isOk());
        open(c, null);
        occupied(undo(other, "回原台"));

        String e = table(4, 0);
        String third = open(e, null);
        transfer(third, table(4, 0)).andExpect(status().isOk());
        jdbc.update("UPDATE bar_tables SET status = 'disabled' WHERE id = ?", e);
        occupied(undo(third, "回原台"));
    }

    /** 连撤两次第二次、转台后又并台：409 BAR_TRANSFER_UNDO_NOT_LATEST（撤销只能恢复最近一次转台）。 */
    @Test
    void undoOnlyTheLatestTransfer() throws Exception {
        String a = table(4, 0);
        String session = open(a, null);
        transfer(session, table(4, 0)).andExpect(status().isOk());
        undo(session, "回原台").andExpect(status().isOk());
        notLatest(undo(session, "再撤一次"));
        assertThat(session(session)).containsEntry("table_id", a);

        String other = open(table(4, 0), null);
        transfer(other, table(4, 0)).andExpect(status().isOk());
        merge(other, open(table(4, 0), null)).andExpect(status().isOk());
        notLatest(undo(other, "回原台"));
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    private void blocked(ResultActions result) throws Exception {
        result.andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_TRANSFER_UNDO_BLOCKED"));
    }

    private void occupied(ResultActions result) throws Exception {
        result.andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_TRANSFER_UNDO_TABLE_OCCUPIED"));
    }

    private void notLatest(ResultActions result) throws Exception {
        result.andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_TRANSFER_UNDO_NOT_LATEST"));
    }

    private Map<String, Object> session(String session) {
        return jdbc.queryForMap("SELECT table_id, status, source_session_id, service_staff_id, min_consumption_snapshot_cent,"
                + " allowed_pass_free_guest_count_snapshot FROM bar_table_sessions WHERE id = ?", session);
    }

    private String tableStatus(String table) {
        return jdbc.queryForObject("SELECT status FROM bar_tables WHERE id = ?", String.class, table);
    }

    private String spaceOf(String table) {
        return jdbc.queryForObject("SELECT space_id FROM bar_tables WHERE id = ?", String.class, table);
    }

    private Long version(String session) {
        return jdbc.queryForObject("SELECT session_version FROM bar_table_sessions WHERE id = ?", Long.class, session);
    }

    /** 后台「卡座与订台」页「撤销最近一次转台」请求（原因取「转/合/拆原因」框）。 */
    private ResultActions undo(String session, String reason) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "tund-undo-" + seq.incrementAndGet() + "-" + suffix);
        body.put("reason", reason);
        body.put("session_version", version(session));
        return mockMvc.perform(post("/api/v1/bar/table-sessions/" + session + "/transfer-undo").with(clerk)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private String open(String table, String reservation) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "tund-open-" + seq.incrementAndGet() + "-" + suffix);
        body.put("bar_id", merchant);
        body.put("table_id", table);
        if (reservation != null) {
            body.put("reservation_id", reservation);
        }
        body.put("guest_count", 2);
        return CustomerTicketPaymentFixture.read(mockMvc.perform(post("/api/v1/bar/table-sessions").with(clerk)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk()), "$.data.id");
    }

    /** 后台「卡座与订台」页转台请求（目标台接手人 t 有目标台点位此刻的已发布班次）。 */
    private ResultActions transfer(String session, String targetTable) throws Exception {
        shift("t", spaceOf(targetTable));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "tund-transfer-" + seq.incrementAndGet() + "-" + suffix);
        body.put("target_table_id", targetTable);
        body.put("target_service_staff_id", user("t"));
        body.put("reason", "客人换台");
        body.put("session_version", version(session));
        return mockMvc.perform(post("/api/v1/bar/table-sessions/" + session + "/transfer").with(clerk)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private ResultActions merge(String primary, String merged) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "tund-merge-" + seq.incrementAndGet() + "-" + suffix);
        body.put("merged_session_id", merged);
        body.put("reason", "两桌朋友拼台");
        body.put("primary_session_version", version(primary));
        body.put("merged_session_version", version(merged));
        return mockMvc.perform(post("/api/v1/bar/table-sessions/" + primary + "/merge").with(clerk)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 后台点单：该台位一杯精酿啤酒。 */
    private ResultActions order(String table) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "tund-order-" + seq.incrementAndGet() + "-" + suffix);
        body.put("bar_id", merchant);
        body.put("table_id", table);
        body.put("items", List.of(Map.of("product_id", product, "quantity", 1)));
        return mockMvc.perform(post("/api/v1/bar/orders").with(clerk)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 给已有订单加一杯。 */
    private ResultActions addItem(String orderId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "tund-add-" + seq.incrementAndGet() + "-" + suffix);
        body.put("items", List.of(Map.of("product_id", product, "quantity", 1)));
        return mockMvc.perform(post("/api/v1/bar/orders/" + orderId + "/items").with(clerk)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 开卡人 h 在小程序自助订该卡座（定金 depositCent，大于 0 时为已付现金定金）。 */
    private String reserve(String table, long depositCent) {
        String reservation = "rsv_tund_" + suffix + "_" + seq.incrementAndGet();
        int capacity = jdbc.queryForObject("SELECT allowed_pass_free_guest_count FROM bar_tables WHERE id = ?", Integer.class, table);
        jdbc.update("INSERT INTO bar_reservations(id,bar_id,merchant_id,table_id,customer_id,reserved_at,guest_count,allowed_pass_free_guest_count,"
                + "deposit_amount_cent,deposit_status,deposit_channel,deposit_paid_at,order_source,status,created_terminal,created_at,updated_at,latest_arrival_at)"
                + " VALUES(?,?,?,?,?,?,2,?,?,?,?,?,'miniapp','reserved','miniapp',now(),now(),?)",
                reservation, merchant, merchant, table, customer("h"), Timestamp.from(Instant.now().plus(Duration.ofMinutes(30))), capacity,
                depositCent, depositCent > 0 ? "paid" : "not_required", depositCent > 0 ? "cash" : null,
                depositCent > 0 ? Timestamp.from(Instant.now()) : null, Timestamp.from(Instant.now().plus(Duration.ofHours(2))));
        return reservation;
    }

    /** 员工在后台「卡座与订台」页签发邀请函，带回二维码口令、防伪标记与签发时的可进人数。 */
    private Map<String, Object> issue(String reservation) throws Exception {
        ResultActions issued = mockMvc.perform(post("/api/v1/bar/reservations/" + reservation + "/invitation").with(clerk)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "tund-issue-" + reservation))))
                .andExpect(status().isOk());
        return Map.of("token", CustomerTicketPaymentFixture.read(issued, "$.data.invitation_token"),
                "signature", CustomerTicketPaymentFixture.read(issued, "$.data.invitation_signature"),
                "capacity", jdbc.queryForObject("SELECT allowed_pass_free_guest_count FROM bar_reservations WHERE id = ?", Integer.class, reservation));
    }

    /** 客人按原二维码载荷接受（小程序请求形状）。 */
    private ResultActions confirm(Map<String, Object> issued, String table, String tag) throws Exception {
        customer(tag);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "tund-confirm-" + UUID.randomUUID());
        body.put("invitation_token", issued.get("token"));
        body.put("invitation_signature", issued.get("signature"));
        body.put("table_id", table);
        body.put("capacity", issued.get("capacity"));
        return mockMvc.perform(post("/api/v1/customer/bar-invitations/confirm").header("X-Terminal-Type", "miniapp")
                .header("X-Customer-Token", token(tag)).header("Idempotency-Key", body.get("idempotency_key").toString())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 本项目本商户铺位（带面积）上的卡座：可进人数＝免通票名额＝capacity，低消 minCent。 */
    private String table(int capacity, long minCent) {
        String space = "spc_tund_" + suffix + "_" + seq.incrementAndGet();
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,area_square_meter,status,created_at,updated_at) "
                + "VALUES(?,?,?,'卡座区','unit',20,'active',now(),now())", space, project, merchant);
        String id = "tbl_tund_" + suffix + "_" + seq.incrementAndGet();
        jdbc.update("INSERT INTO bar_tables(id,bar_id,merchant_id,table_code,table_name,table_type,capacity,allowed_pass_free_guest_count,"
                + "min_consumption_cent,currency,status,created_at,updated_at,space_id) VALUES(?,?,?,?,?,'card_seat',?,?,?,'CNY','active',now(),now(),?)",
                id, merchant, merchant, "K" + id, capacity + "人卡座", capacity, capacity, minCent, space);
        return id;
    }

    /** 已发布班次：覆盖此刻，工作点位为该铺位。 */
    private void shift(String tag, String space) {
        Instant now = Instant.now();
        jdbc.update("INSERT INTO staff_shift_schedules(id,user_id,work_date,start_at,end_at,point_space_ids,created_by,project_id) "
                + "VALUES(?,?,?,?,?,jsonb_build_array(?::text),?,?)", "sft_tund_" + tag + "_" + suffix + "_" + seq.incrementAndGet(), user(tag),
                LocalDate.now(), Timestamp.from(now.minus(Duration.ofHours(2))), Timestamp.from(now.plus(Duration.ofHours(4))), space, user("k"), project);
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

    private String user(String tag) {
        return "usr_tund_" + tag + "_" + suffix;
    }

    /** 小程序客人（同一测试内按 tag 只建一次），返回客人号。 */
    private String customer(String tag) {
        String id = "cus_tund_" + tag + "_" + suffix;
        if (createdCustomers.add(tag)) {
            Instant now = Instant.now();
            String phone = "137" + String.format("%08d", Math.floorMod((suffix + tag).hashCode(), 100_000_000));
            customers.saveCustomer(new CustomerRecord(id, phone, "137****" + phone.substring(7), "active", null, null, now, now));
            customers.saveSession(new CustomerSessionRecord("sess_tund_" + tag + "_" + suffix, id, token(tag), "miniapp", now,
                    now.plusSeconds(3600), "active"));
        }
        return id;
    }

    private String token(String tag) {
        return "tok_tund_" + tag + "_" + suffix;
    }
}
