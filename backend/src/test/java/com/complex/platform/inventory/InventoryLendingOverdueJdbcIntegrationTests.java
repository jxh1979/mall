package com.complex.platform.inventory;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.evidence.EvidenceEventService;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批第 18 条 借用逾期未归还自动建工单真库回归：建单、交货、收货、归还都按前端真实请求形状（蛇形键）走真实入口与授权链。
 * BSM:1201「超过约定归还时间仍未归还的，系统自动生成一张工单并通知借出方主管，按与其他工单相同的超时升级路径继续催办，直到归还或双方另行处理为止」；
 * BSM:1372「商户报修、客人投诉、设备故障、巡检发现的问题都进统一工单」；BSM:1385「工单由客户关怀部负责处理」。
 * 「借出方主管」沿用库存域「门店主管」口径＝持 inventory.high_value.review 的借出方员工。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class InventoryLendingOverdueJdbcIntegrationTests {

    private static final String BASE = "/api/v1/inventory/transfer-orders";
    private static final Instant DUE = Instant.parse("2026-12-31T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private InventoryRepository repository;

    @Autowired
    private InventoryTransferFlowService flow;

    @Autowired
    private EvidenceEventService evidence;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "b24od_prj_" + suffix;
    private final String lender = "b24od_lender_" + suffix;
    private final String borrower = "b24od_borrower_" + suffix;
    private final String device = "b24od_device_" + suffix;
    private final String location = "b24od_loc_" + suffix;
    private final List<String> codes = List.of("B24ODA" + suffix, "B24ODB" + suffix, "B24ODC" + suffix, "B24ODD" + suffix);
    private RequestPostProcessor admin;
    private RequestPostProcessor lenderStaff;
    private RequestPostProcessor borrowerStaff;

    @BeforeEach
    void seed() {
        String entity = "b24od_ent_" + suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','借用逾期回归项目','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'项目主体','project_company','active',now(),now())", entity);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", entity, project);
        for (String m : List.of(lender, borrower)) {
            jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,project_id,status,created_at,updated_at) VALUES(?,?,'retail',?,?,'active',now(),now())", m, m, entity, project);
            jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,lifecycle_status,allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,'effective','[\"inventory\",\"finance\"]'::jsonb,now(),now())", "b24od_ct_" + m, m, entity, entity);
        }
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at,non_consumable) VALUES(?,'舞台音响','equipment','台',0,'active',?,now(),now(),TRUE)", device, lender);
        for (String code : codes) {
            jdbc.update("INSERT INTO inventory_trace_codes(id,trace_code,item_id,merchant_id,owner_type,owner_id) VALUES(?,?,?,?,'merchant',?)", "tc_" + code, code, device, lender, lender);
        }
        repository.atomicIncreaseStockBalance(device, location, 5, "b24od_bal_" + suffix, lender, null, project);
        String role = "b24od_role_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'借用逾期回归','project','active',now())", role, role);
        jdbc.update("""
                INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN(
                'inventory.transfer_order.manage','inventory.transfer_order.view','evidence.attachment.create','facility.work_order.manage')
                """, role);
        String supervisor = "b24od_sup_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'门店主管','project','active',now())", supervisor, supervisor);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code='inventory.high_value.review'", supervisor);
        admin = account("admin", "platform_admin", "org", null, role);
        lenderStaff = account("lender", "staff", project, lender, role);
        account("lender_boss", "staff", project, lender, role);
        borrowerStaff = account("borrower", "staff", project, borrower, role);
        account("borrower_boss", "staff", project, borrower, role);
        for (String tag : List.of("lender_boss", "borrower_boss")) {
            jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", user(tag), supervisor);
        }
        for (String tag : List.of("lender", "lender_boss", "borrower_boss")) {
            jdbc.update("INSERT INTO staff_profiles(id,project_id,merchant_id,user_id,name,phone,registration_status,status,created_at,updated_at) VALUES(?,?,?,?,?,?,'approved','active',now(),now())",
                    "b24od_sp_" + tag + "_" + suffix, project, tag.startsWith("lender") ? lender : borrower, user(tag), tag, "1390000" + Math.abs((tag + suffix).hashCode() % 10000));
        }
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // ①② 到期仍未归还：恰好一张物业工单（来源借用逾期、其他×普通、待派单、记借出方与其项目、时限按「其他×普通」策略），单上回写工单号；
    // 只通知借出方持「门店主管」码的员工一条系统通知，借出方普通员工与借入方主管都不通知；再跑一次不重复建单、不重复通知。
    @Test
    void overdueBorrowRaisesOneFacilityWorkOrderAndNotifiesOnlyTheLendersSupervisorOnce() throws Exception {
        String id = lentOut(codes.get(0), codes.get(1));
        flow.raiseLendingOverdue(DUE.plus(Duration.ofMinutes(1)));

        List<Map<String, Object>> orders = workOrders();
        assertThat(orders).hasSize(1);
        Map<String, Object> wo = orders.getFirst();
        assertThat(wo).containsEntry("source", "lending_overdue").containsEntry("work_order_type", "other").containsEntry("priority", "normal")
                .containsEntry("status", "pending_assign").containsEntry("merchant_id", lender).containsEntry("project_id", project);
        Integer handleMinutes = jdbc.queryForObject("SELECT handle_minutes FROM facility_work_order_policy WHERE work_order_type='other' AND priority='normal'", Integer.class);
        assertThat(Duration.between(((Timestamp) wo.get("created_at")).toInstant(), ((Timestamp) wo.get("handle_due_at")).toInstant()))
                .isEqualTo(Duration.ofMinutes(handleMinutes));
        assertThat(jdbc.queryForObject("SELECT overdue_work_order_id FROM inventory_transfer_orders WHERE id=?", String.class, id)).isEqualTo(wo.get("id"));
        assertThat(notices("lender_boss")).singleElement().satisfies(n -> assertThat(n)
                .containsEntry("notice_type", "system_notice").containsEntry("title", "借用逾期未归还：" + id));
        assertThat(notices("lender")).isEmpty();
        assertThat(notices("borrower_boss")).isEmpty();

        flow.raiseLendingOverdue(DUE.plus(Duration.ofMinutes(2)));
        assertThat(workOrders()).hasSize(1);
        assertThat(notices("lender_boss")).hasSize(1);
    }

    // ③ 未到约定归还时间、已全部归还、全部数量都已登记无法归还（双方另行处理）的单都不建工单。
    @Test
    void notYetDueReturnedOrFullyClaimedBorrowsRaiseNothing() throws Exception {
        String returned = lentOut(codes.get(0), codes.get(1));
        flow.raiseLendingOverdue(DUE.minus(Duration.ofMinutes(1)));
        assertThat(workOrders()).isEmpty();

        giveBack(returned, 2, codes.get(0), codes.get(1)).andExpect(jsonPath("$.data.status").value("returned"));
        String claimed = lentOut(codes.get(2), codes.get(3));
        call(borrowerStaff, claimed, "unreturnable", "{\"line_id\":\"" + claimed + "_l0\",\"quantity\":2,\"trace_codes\":[\"" + codes.get(2) + "\",\""
                + codes.get(3) + "\"],\"reason\":\"演出中摔坏\",\"disposal_note\":\"借出方收回残件报废\",\"evidence_attachment_ids\":[\"" + upload(claimed) + "\"]}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("pending_counterparty"));

        flow.raiseLendingOverdue(DUE.plus(Duration.ofMinutes(1)));
        assertThat(workOrders()).isEmpty();
    }

    // ④「直到归还或双方另行处理为止」：部分归还工单仍开着；全部归还后系统自动结束工单并写处理结论。
    @Test
    void workOrderClosesOnlyWhenEverythingIsBack() throws Exception {
        String id = lentOut(codes.get(0), codes.get(1));
        flow.raiseLendingOverdue(DUE.plus(Duration.ofMinutes(1)));
        String wo = (String) workOrders().getFirst().get("id");

        giveBack(id, 1, codes.get(0)).andExpect(jsonPath("$.data.status").value("received"));
        assertThat(jdbc.queryForObject("SELECT status FROM facility_work_orders WHERE id=?", String.class, wo)).isEqualTo("pending_assign");

        giveBack(id, 1, codes.get(1)).andExpect(jsonPath("$.data.status").value("returned"));
        Map<String, Object> closed = jdbc.queryForMap("SELECT status,resolution FROM facility_work_orders WHERE id=?", wo);
        assertThat(closed).containsEntry("status", "closed");
        assertThat(closed.get("resolution")).isNotNull();
    }

    // ⑤「按与其他工单相同的超时升级路径继续催办」：借用逾期工单与其他工单同表同策略，超过处理时限即进物业工单超时清单。
    @Test
    void overdueWorkOrderJoinsTheSameOvertimeListAsOtherWorkOrders() throws Exception {
        lentOut(codes.get(0), codes.get(1));
        flow.raiseLendingOverdue(DUE.plus(Duration.ofMinutes(1)));
        String wo = (String) workOrders().getFirst().get("id");
        jdbc.update("UPDATE facility_work_orders SET handle_due_at=now()-interval '1 minute' WHERE id=?", wo);

        mockMvc.perform(get("/api/v1/facility/work-orders/overdue").with(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].id").value(hasItem(wo)));
    }

    // ⑥ 借用逾期是系统专用来源：后台人工建单带 source=lending_overdue 400 VALIDATION_FAILED，不落工单。
    @Test
    void staffCannotCreateALendingOverdueWorkOrderByHand() throws Exception {
        mockMvc.perform(post("/api/v1/facility/work-orders").with(admin).contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key":"%s","work_order_type":"other","priority":"normal","title":"手工借用逾期%s","source":"lending_overdue","project_id":"%s","merchant_id":"%s"}
                        """.formatted(key(), suffix, project, lender)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM facility_work_orders WHERE title=?", Long.class, "手工借用逾期" + suffix)).isZero();
    }

    // ── 夹具 ──

    /** 两码借用单：按蛇形键建单 → 借出方交货扫码 → 借入方收货扫码。 */
    private String lentOut(String first, String second) throws Exception {
        String body = """
                {"idempotency_key":"%s","transfer_type":"lending","lending_mode":"borrow","from_owner_type":"merchant","from_owner_id":"%s",
                 "to_owner_type":"merchant","to_owner_id":"%s","from_location_id":"%s","return_due_at":"%s",
                 "lines":[{"item_id":"%s","quantity":2,"unit_price_cent":80000,"trace_codes":["%s","%s"]}]}
                """.formatted(key(), lender, borrower, location, DUE, device, first, second);
        String id = JsonPath.read(mockMvc.perform(post(BASE).with(admin).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("created"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        call(lenderStaff, id, "ship", "{\"scanned_trace_codes\":[\"" + first + "\",\"" + second + "\"]}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("shipped"));
        call(borrowerStaff, id, "receive", "{\"scanned_trace_codes\":[\"" + first + "\",\"" + second + "\"]}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("received"));
        return id;
    }

    /** 借出方收货扫码归还（蛇形键）。 */
    private ResultActions giveBack(String id, int quantity, String... traceCodes) throws Exception {
        return call(lenderStaff, id, "return", "{\"lines\":[{\"line_id\":\"" + id + "_l0\",\"returned_quantity\":" + quantity
                + ",\"trace_codes\":[\"" + String.join("\",\"", traceCodes) + "\"]}]}").andExpect(status().isOk());
    }

    private List<Map<String, Object>> workOrders() {
        return jdbc.queryForList("SELECT * FROM facility_work_orders WHERE merchant_id=? AND source='lending_overdue'", lender);
    }

    private List<Map<String, Object>> notices(String tag) {
        return jdbc.queryForList("""
                SELECT n.notice_type, n.title FROM employee_notices n JOIN staff_profiles p ON p.id = n.staff_profile_id WHERE p.user_id=?
                """, user(tag));
    }

    /** 补上幂等键后按蛇形键原样提交。 */
    private ResultActions call(RequestPostProcessor who, String id, String action, String body) throws Exception {
        String withKey = "{\"idempotency_key\":\"" + key() + "\"," + body.substring(1);
        return mockMvc.perform(post(BASE + "/" + id + "/" + action).with(who).contentType(MediaType.APPLICATION_JSON).content(withKey));
    }

    private String upload(String orderId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(user("borrower"), user("borrower"), "staff", project, borrower, "active"), "test", List.of()));
        try {
            return evidence.uploadAttachment(new MockMultipartFile("file", "damage.png", "image/png",
                    Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jCYQAAAAASUVORK5CYII=")),
                    "inventory_transfer_order", orderId, "other").id();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private RequestPostProcessor account(String tag, String accountType, String organizationId, String merchantId, String role) {
        String userId = user(tag);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,?,?,?,'active',now())",
                userId, userId, accountType, organizationId, merchantId);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId, organizationId);
        AccessActor actor = new AccessActor(userId, userId, accountType, organizationId, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private String user(String tag) {
        return "b24od_" + tag + "_" + suffix;
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }
}
