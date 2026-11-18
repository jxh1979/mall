package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
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
 * 第十八批 AU 段第 265 条 265-R1（真库、真实入口、真实授权链）。不开测试事务：最高权限发起删除遇占用时要看到整笔回滚。
 * BSM:577「卡座预订这类业务记录，现在也支持存档、以及审批后删除……删除一律须由具备最高权限的人审批通过后执行，不设例外，申请人不得审批自己的申请；
 * 持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批。」；第十八批裁决 71（最高权限什么都能看、本人发起删除即时生效）。
 * ① 店长存档并申请删除本店订台；非最高权限的平台管理员审批 403 MASTER_APPROVAL_REQUIRED、仍待审批；最高权限审批即删除。
 * ② 最高权限本人对别家酒吧已存档的订台申请删除即生效，申请与批准各留一条成功事件。
 * ③ 订台仍被订单引用时最高权限发起删除 409 BAR_RESERVATION_IN_USE，整笔回滚，仍为未申请删除。
 * ④ 最高权限打开「卡座与订台」页读到的卡座、开台会话、订台、代订停用四个列表都不按商户范围；甲店店员照旧只看本店。
 * ⑤ 订台仍在预留中（员工代订已占台）时最高权限发起删除 409 BAR_RESERVATION_IN_USE，整笔回滚，仍占着台位。
 * ⑥ 客人已到店开台（status 仍为 reserved、hold_status=released）的订台按开台会话引用 409，不提示「先取消订台」。
 * 夹具不回滚：角色名用中文、编号后缀纯数字。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class BarReservationMasterDeleteJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = Long.toString(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    private final String project = "r265bar_prj_" + s;
    private final String barA = "r265bar_a_" + s;
    private final String barB = "r265bar_b_" + s;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','订台删除回归综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,?,'bar','active',?,now(),now())",
                barA, "订台删除回归甲店" + s, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,?,'bar','active',?,now(),now())",
                barB, "订台删除回归乙店" + s, project);
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    /** ① 店长申请删除本店订台；平台管理员（非最高权限）审批 403、仍待审批；最高权限审批即删除。 */
    @Test
    void onlyMasterAuthorityMayApproveReservationDelete() throws Exception {
        String id = reservation(barA, "approve", false);
        RequestPostProcessor manager = staff("mgr", barA, "订台存档回归店长角色", "bar.reservation.manage");

        send(manager, "/api/v1/bar/reservations/" + id + "/archive", "{\"reason\":\"客人改期，清理列表\"}")
                .andExpect(status().isOk());
        send(manager, "/api/v1/bar/reservations/" + id + "/delete-request", "{\"reason\":\"重复录入的订台\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.delete_status").value("pending"));

        send(platformAdmin("approver"), "/api/v1/bar/reservations/" + id + "/delete-approve", "{\"decision\":\"approved\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("MASTER_APPROVAL_REQUIRED"));
        assertThat(deleteStatus(id)).isEqualTo("pending");

        send(master(), "/api/v1/bar/reservations/" + id + "/delete-approve", "{\"decision\":\"approved\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(true));
        assertThat(deleteStatus(id)).isEqualTo("approved");
    }

    /** ② 最高权限本人对别家酒吧已存档的订台申请删除，当场删除，申请与批准各留一条成功事件。 */
    @Test
    void masterDeleteRequestOnAnotherBarsArchivedReservationTakesEffectAtOnce() throws Exception {
        String id = reservation(barB, "direct", true);

        send(master(), "/api/v1/bar/reservations/" + id + "/delete-request", "{\"reason\":\"重复录入的订台\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.delete_status").value("approved"));

        assertThat(deleteStatus(id)).isEqualTo("approved");
        assertThat(masterEvents(id)).containsExactlyInAnyOrder(
                "bar.reservation.delete_requested|success", "bar.reservation.delete_approved|success");
    }

    /** ③ 订台仍被订单引用，最高权限发起删除 409，整笔回滚——仍为已存档、未申请删除，成功事件一条也不留。 */
    @Test
    void masterDeleteRequestOnReservationStillOrderedIsRejectedAndRolledBack() throws Exception {
        String id = reservation(barA, "ordered", true);
        jdbc.update("INSERT INTO bar_orders (id, bar_id, merchant_id, table_id, reservation_id, order_source, status, total_amount_cent, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, ?, 'staff', 'submitted', 5000, now(), now())",
                "r265bo_ordered_" + s, barA, barA, "r265tbl_ordered_" + s, id);

        send(master(), "/api/v1/bar/reservations/" + id + "/delete-request", "{\"reason\":\"重复录入的订台\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_RESERVATION_IN_USE"));

        assertThat(deleteStatus(id)).isEqualTo("none");
        assertThat(jdbc.queryForObject("SELECT archived_at IS NOT NULL FROM bar_reservations WHERE id = ?", Boolean.class, id)).isTrue();
        assertThat(masterEvents(id)).isEmpty();
    }

    /**
     * ⑤ 第十八批 AU 段收尾复核（BSM:572「只要还有占用就拦住不让删」；BSM:577「同样带删前占用检查：还被业务在用的删不掉」；ENUM:100「一个台位同一时刻
     * 只能存在一笔有效预留……已预留只能经取消订台、超时释放、到店开台三条路径退出」）：员工代订仍在预留中（status=reserved、hold_status=held）且已存档的订台，
     * 最高权限申请删除 409 BAR_RESERVATION_IN_USE、提示先取消订台，整笔回滚——仍为未申请删除、仍占着台位。
     * 修复前红：占用检查只数订单、账单、开台会话、邀请成员，删除照样通过，台位从此卡在「已预留」。
     */
    @Test
    void masterDeleteRequestOnReservationStillHoldingTheTableIsRejected() throws Exception {
        String id = reservation(barA, "held", true);
        jdbc.update("UPDATE bar_reservations SET status = 'reserved', hold_status = 'held' WHERE id = ?", id);

        send(master(), "/api/v1/bar/reservations/" + id + "/delete-request", "{\"reason\":\"重复录入的订台\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_RESERVATION_IN_USE"))
                .andExpect(jsonPath("$.message").value("订台仍在预留中，请先取消订台"));

        assertThat(jdbc.queryForMap("SELECT delete_status, status, hold_status FROM bar_reservations WHERE id = ?", id))
                .containsEntry("delete_status", "none").containsEntry("status", "reserved").containsEntry("hold_status", "held");
        assertThat(masterEvents(id)).isEmpty();
    }

    /**
     * ⑥ 第十八批 AU 段收尾复核（契约 requestDeleteBarReservation 的 409 条件「订台仍在预留中（未经取消、超时释放或到店开台）」；ENUM:100「已预留只能经
     * 取消订台、超时释放、到店开台三条路径退出」）：客人已到店开台的订台（status 仍为 reserved、hold_status=released、挂着开台会话），最高权限申请删除照样
     * 409 BAR_RESERVATION_IN_USE，但按开台会话引用拦，不提示「订台仍在预留中，请先取消订台」（照提示去取消一笔已履约的订台会给已到店的客人发取消通知）。
     * 修复前红：只判 status=reserved，已到店开台的也报「订台仍在预留中」。
     */
    @Test
    void masterDeleteRequestOnArrivedReservationIsRejectedBySessionNotAsStillReserved() throws Exception {
        String id = reservation(barA, "arrived", true);
        jdbc.update("UPDATE bar_reservations SET status = 'reserved', hold_status = 'released' WHERE id = ?", id);
        jdbc.update("INSERT INTO bar_table_sessions (id, bar_id, merchant_id, business_date, table_id, reservation_id, status, opened_at, created_at, updated_at)"
                + " VALUES (?, ?, ?, CURRENT_DATE, ?, ?, 'open', now(), now(), now())",
                "r265ses_arrived_" + s, barA, barA, "r265tbl_arrived_" + s, id);

        String body = send(master(), "/api/v1/bar/reservations/" + id + "/delete-request", "{\"reason\":\"清理已完成的订台\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_RESERVATION_IN_USE"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(JsonPath.<String>read(body, "$.message")).doesNotContain("预留中").contains("引用");
        assertThat(deleteStatus(id)).isEqualTo("none");
        assertThat(masterEvents(id)).isEmpty();
    }

    /** ④ 最高权限读四个列表都不按商户范围、看得到两家酒吧；甲店店员只看本店。 */
    @Test
    void masterSeesEveryBarsTablesSessionsAndReservationsWhileBarStaffSeeOnlyTheirOwn() throws Exception {
        String reservationA = reservation(barA, "lista", false);
        String reservationB = reservation(barB, "listb", true);
        String tableA = "r265tbl_lista_" + s;
        String tableB = "r265tbl_listb_" + s;
        String sessionA = session(barA, "sesa");
        String sessionB = session(barB, "sesb");
        RequestPostProcessor staffA = staff("viewa", barA, "订台查看回归角色",
                "bar.table.view", "bar.table_session.view", "bar.reservation.view", "bar.staff_booking.restore");

        assertThat(ids(master(), "/api/v1/bar/tables")).contains(tableA, tableB);
        assertThat(ids(master(), "/api/v1/bar/table-sessions")).contains(sessionA, sessionB);
        assertThat(ids(master(), "/api/v1/bar/reservations?include_archived=true")).contains(reservationA, reservationB);
        mockMvc.perform(get("/api/v1/bar/staff-booking-suspensions").with(master())).andExpect(status().isOk());

        assertThat(ids(staffA, "/api/v1/bar/tables")).contains(tableA).doesNotContain(tableB);
        assertThat(ids(staffA, "/api/v1/bar/table-sessions")).contains(sessionA).doesNotContain(sessionB);
        assertThat(ids(staffA, "/api/v1/bar/reservations?include_archived=true")).contains(reservationA).doesNotContain(reservationB);
        mockMvc.perform(get("/api/v1/bar/staff-booking-suspensions").with(staffA)).andExpect(status().isOk());
    }

    // ---------- 夹具 ----------

    /** 一张卡座上的一笔已预订的订台；archived=true 时已由店长存档。 */
    private String reservation(String bar, String tag, boolean archived) {
        String table = table(bar, tag);
        String id = "r265res_" + tag + "_" + s;
        jdbc.update("INSERT INTO bar_reservations (id, bar_id, merchant_id, table_id, reserved_at, guest_count, deposit_status, order_source,"
                + " status, created_at, updated_at, archived_at, archived_by, delete_status)"
                + " VALUES (?, ?, ?, ?, now(), 2, 'not_required', 'staff', 'cancelled', now(), now(),"
                + " CASE WHEN ? THEN now() END, CASE WHEN ? THEN ? END, 'none')",
                id, bar, bar, table, archived, archived, "r265bar_mgr_" + s);
        return id;
    }

    private String table(String bar, String tag) {
        String id = "r265tbl_" + tag + "_" + s;
        jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, '回归卡座', 'card_seat', 'available', now(), now())", id, bar, bar, "R" + tag + s);
        return id;
    }

    private String session(String bar, String tag) {
        String id = "r265ses_" + tag + "_" + s;
        jdbc.update("INSERT INTO bar_table_sessions (id, bar_id, merchant_id, business_date, table_id, status, opened_at, created_at, updated_at)"
                + " VALUES (?, ?, ?, CURRENT_DATE, ?, 'open', now(), now(), now())", id, bar, bar, table(bar, tag));
        return id;
    }

    // ---------- 断言与请求 ----------

    private ResultActions send(RequestPostProcessor who, String url, String body) throws Exception {
        return mockMvc.perform(post(url).with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private List<String> ids(RequestPostProcessor who, String url) throws Exception {
        String body = mockMvc.perform(get(url).with(who)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.data.items[*].id");
    }

    private String deleteStatus(String id) {
        return jdbc.queryForObject("SELECT delete_status FROM bar_reservations WHERE id = ?", String.class, id);
    }

    /** 最高权限本人在这笔订台上留下的删除类成功／失败事件。 */
    private List<String> masterEvents(String businessId) {
        return jdbc.queryForList("SELECT event_name || '|' || event_result FROM audit_events"
                + " WHERE actor_id = ? AND business_id = ? AND event_name LIKE 'bar.reservation.delete_%'",
                String.class, SessionIssuer.MASTER_USER_ID, businessId);
    }

    /** 最高权限本人（保留超管账号，组织 platform-company、无商户），经后台 Web 终端。 */
    private RequestPostProcessor master() {
        return session(new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active"));
    }

    /** 平台管理员（不是最高权限），经一个中文名角色持有订台删除审批权限码。 */
    private RequestPostProcessor platformAdmin(String tag) {
        String userId = "r265bar_" + tag + "_" + s;
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', NULL, NULL, 'active', now())", userId, userId);
        grant(userId, "订台删除审批回归角色", "bar.reservation.approve");
        return session(new AccessActor(userId, userId, "platform_admin", null, null, "active"));
    }

    /** 某家酒吧的员工，经一个中文名角色持有给定权限码。 */
    private RequestPostProcessor staff(String tag, String bar, String roleName, String... codes) {
        String userId = "r265bar_" + tag + "_" + s;
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at) VALUES (?, ?, 'staff', ?, ?, 'active', now())",
                userId, userId, project, bar);
        grant(userId, roleName, codes);
        return session(new AccessActor(userId, userId, "staff", project, bar, "active"));
    }

    private void grant(String userId, String roleName, String... codes) {
        String role = "role_" + userId;
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '订台删除回归', 'project', 'active', now())",
                role, roleName + s);
        for (String code : codes) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, code);
        }
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
    }

    private RequestPostProcessor session(AccessActor actor) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
