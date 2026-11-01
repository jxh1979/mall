package com.complex.platform.bar;

// 文件版本：V0.02

import static com.complex.platform.support.CustomerProjectSwitchRequests.keyed;
import static com.complex.platform.support.CustomerProjectSwitchRequests.rejected;
import static com.complex.platform.support.CustomerTicketPaymentFixture.read;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.finance.gateway.PaymentGatewayRouter;
import com.complex.platform.support.CustomerProjectSwitchRequests;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * PGM 第 736 行「可以按全局、综合体、商户、业务系统、模块、版本/设备、灰度开关」、第 744 行「前端隐藏只是辅助，后端接口必须独立校验」：
 * 有两个综合体时关掉甲的客人端，甲的酒吧对象在服务端按酒吧（订台、台位所属酒吧商户）的综合体判——不带 X-Project-Id 或把头写成乙都
 * 403 TERMINAL_DISABLED，写操作在幂等之前判（开着时成功过的同一请求编号，关掉后重放也拒）；乙照常。
 * 涉及：订卡邀请函接受、查看、预览、移出、本人动态入场码，定金付款发起与确认，扫码点单菜单、下单、付款确认与本桌账单（匿名）。
 * 真库、真接口，客人请求按客人端真实形状；app、miniapp 翻成 enforce（与生产一致）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "access.device_credential.enforcement.app=enforce",
        "access.device_credential.enforcement.miniapp=enforce",
        "bar.scan_order.enabled=true"
})
@AutoConfigureMockMvc
@Transactional
class BarCustomerProjectSwitchJdbcIntegrationTests {

    private static final String CONFIRM = "/api/v1/customer/bar-invitations/confirm";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @Autowired private BarRepository bars;
    @Autowired private BarInvitationTokenSigner signer;
    @MockitoBean private PaymentGatewayRouter gateway;

    private CustomerTicketPaymentFixture fa;
    private CustomerTicketPaymentFixture fb;
    private CustomerProjectSwitchRequests req;
    /** 甲＝被关的综合体，乙＝另一个综合体：各一家酒吧（订卡邀请函、待付定金的订台、开着台的散台与一款酒）。 */
    private Bar a;
    private Bar b;

    @BeforeEach
    void seed() {
        fa = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        fb = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
        fb.stubGateway(gateway); // 渠道替身对两个综合体的付款单都生效（markPaid 一律经 fb）
        req = new CustomerProjectSwitchRequests(mockMvc, jdbc, json);
        a = new Bar(fa);
        b = new Bar(fb);
    }

    @Test
    void invitationsFollowBarProject() throws Exception {
        Map<String, Object> accept = a.accept();
        String member = read(req.post(CONFIRM, a.friend, null, accept).andExpect(status().isOk()), "$.data.id");
        Map<String, Object> remove = keyed("b16-inv-rm");
        req.post(a.removePath(member), a.f.token, null, remove).andExpect(status().isOk());
        req.post(CONFIRM, a.friend, null, a.accept()).andExpect(status().isOk());
        req.close(fa.projectId);

        req.get("/api/v1/customer/bar-invitations/" + b.reservation, b.f.token, null).andExpect(status().isOk());
        b.preview(null).andExpect(status().isOk());
        String bMember = read(req.post(CONFIRM, b.friend, null, b.accept()).andExpect(status().isOk()), "$.data.id");
        req.get(b.entryCodePath(), b.friend, null).andExpect(status().isOk());
        req.post(b.removePath(bMember), b.f.token, null, keyed("b16-inv-rm")).andExpect(status().isOk());

        rejected(fb.projectId, h -> req.post(CONFIRM, a.friend, h, accept));
        rejected(fb.projectId, h -> req.post(a.removePath(member), a.f.token, h, remove));
        rejected(fb.projectId, h -> req.get("/api/v1/customer/bar-invitations/" + a.reservation, a.f.token, h));
        rejected(fb.projectId, a::preview);
        rejected(fb.projectId, h -> req.get(a.entryCodePath(), a.friend, h));
    }

    @Test
    void depositPaymentsFollowBarProject() throws Exception {
        Map<String, Object> start = keyed("b16-dep", "channel", "wechat");
        String intent = read(req.post(a.depositPath(), a.f.token, null, start).andExpect(status().isOk()), "$.data.payment_intent_id");
        fb.markPaid(intent);
        Map<String, Object> confirm = keyed("b16-dep-confirm");
        req.post(a.depositPath() + "/" + intent + "/confirm", a.f.token, null, confirm).andExpect(status().isOk());
        req.close(fa.projectId);

        String bIntent = read(req.post(b.depositPath(), b.f.token, null, keyed("b16-dep", "channel", "wechat"))
                .andExpect(status().isOk()), "$.data.payment_intent_id");
        fb.markPaid(bIntent);
        req.post(b.depositPath() + "/" + bIntent + "/confirm", b.f.token, null, keyed("b16-dep-confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deposit_status").value("paid"));

        rejected(fb.projectId, h -> req.post(a.depositPath(), a.f.token, h, start));
        rejected(fb.projectId, h -> req.post(a.depositPath() + "/" + intent + "/confirm", a.f.token, h, confirm));
    }

    /** 扫码点单非会员可不登录（ENUM 第 129 行），终端类型自报；综合体按台位所属酒吧在服务端取。 */
    @Test
    void scanOrdersFollowTableProject() throws Exception {
        Map<String, Object> order = a.scanOrder();
        ResultActions created = req.post(a.tablePath() + "/scan-orders", null, null, order).andExpect(status().isOk());
        String scanOrderId = read(created, "$.data.scan_order_id");
        fb.markPaid(read(created, "$.data.payment_intent_id"));
        Map<String, Object> confirm = keyed("b16-scan-confirm");
        req.post(a.tablePath() + "/scan-orders/" + scanOrderId + "/confirm", null, null, confirm).andExpect(status().isOk());
        req.close(fa.projectId);

        req.get(b.tablePath() + "/menu", null, null).andExpect(status().isOk());
        ResultActions bCreated = req.post(b.tablePath() + "/scan-orders", null, null, b.scanOrder()).andExpect(status().isOk());
        fb.markPaid(read(bCreated, "$.data.payment_intent_id"));
        req.post(b.tablePath() + "/scan-orders/" + read(bCreated, "$.data.scan_order_id") + "/confirm", null, null,
                keyed("b16-scan-confirm")).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("paid"));
        req.get(b.tablePath() + "/current-bill", null, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1));

        rejected(fb.projectId, h -> req.get(a.tablePath() + "/menu", null, h));
        // 业务层把台位编号去首尾空格再查：前面加空格也绕不过
        rejected(fb.projectId, h -> req.get("/api/v1/customer/bar-tables/ " + a.looseTable + "/menu", null, h));
        rejected(fb.projectId, h -> req.get(a.tablePath() + "/current-bill", null, h));
        rejected(fb.projectId, h -> req.post(a.tablePath() + "/scan-orders", null, h, order));
        rejected(fb.projectId, h -> req.post(a.tablePath() + "/scan-orders/" + scanOrderId + "/confirm", null, h, confirm));
    }

    /** 一个综合体里的一家酒吧：开卡人＝该综合体夹具的客人，另一位朋友接受邀请。 */
    private final class Bar {
        final CustomerTicketPaymentFixture f;
        final String barId;
        final String inviteTable;
        final String reservation;
        final String depositReservation;
        final String looseTable;
        final String product;
        final String friend;
        final String invitationToken;
        final String signature;

        Bar(CustomerTicketPaymentFixture f) {
            this.f = f;
            String s = f.suffix;
            String company = "ent_b16bar_" + s;
            barId = "bar_b16_" + s;
            jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at)"
                    + " VALUES(?,'经营主体','management_company','active',now(),now())", company);
            jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, f.projectId);
            jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,"
                    + "project_id,status,created_at,updated_at) VALUES(?,'综合体开关测试酒吧','bar',?,?,?,?,'active',now(),now())",
                    barId, company, company, company, f.projectId);
            jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at)"
                    + " VALUES(?,?,?,'散台区','unit','active',now(),now())", "space_b16_" + s, f.projectId, barId);
            Instant now = Instant.now();
            inviteTable = table("card", "card_seat", "active");
            String depositTable = table("dep", "card_seat", "active");
            looseTable = table("loose", "loose_table", "opened");
            bars.assignTableSpace(looseTable, "space_b16_" + s);
            bars.saveTableSession(new BarTableSessionRecord("session_b16_" + s, barId, barId, LocalDate.now(), looseTable, null, null,
                    2, 0L, 0, null, null, "walk_in", "open", now, null, null, 1L, now, now));
            product = "product_b16_" + s;
            bars.saveProduct(new BarProductRecord(product, barId, barId, "精酿啤酒", "beer", 3800L, "CNY", null, null, false, "active",
                    now, now, null, null, null, false, null, null, null));
            // 已付定金、员工已签发邀请函的订台（第十三批裁决第 4 条「酒吧员工确认后签发」）：卡座可进 3 人。
            reservation = "reservation_b16_" + s;
            invitationToken = "invtok_b16_" + s;
            signature = signer.sign(signer.canonical(reservation, barId, inviteTable, 3, invitationToken));
            Timestamp at = Timestamp.from(now);
            jdbc.update("INSERT INTO bar_reservations (id, bar_id, table_id, customer_id, reserved_at, guest_count, allowed_pass_free_guest_count,"
                    + " deposit_amount_cent, deposit_status, order_source, status, created_terminal, created_at, updated_at, latest_arrival_at,"
                    + " hold_status, invitation_token, invitation_signature, invitation_capacity, invitation_status, invitation_issued_at)"
                    + " VALUES (?, ?, ?, ?, ?, 3, 3, 20000, 'paid', 'miniapp', 'reserved', 'miniapp', ?, ?, ?, 'held', ?, ?, 3, 'active', ?)",
                    reservation, barId, inviteTable, f.customerId, Timestamp.from(now.plusSeconds(3600)), at, at,
                    Timestamp.from(now.plusSeconds(7200)), invitationToken, signature, at);
            // 待付定金的自助订台（定金付款成功才占台）。
            depositReservation = "reservation_b16_dep_" + s;
            customers.saveCustomerBarReservation(new BarReservationRecord(depositReservation, barId, null, depositTable, f.customerId,
                    now.plusSeconds(7200), 2, 2, 20_000L, "pending", "miniapp", null, "reserved", "miniapp", null, List.of(), now, now, null));
            friend = CustomerProjectSwitchRequests.customer(customers, "friend");
        }

        private String table(String tag, String type, String tableStatus) {
            String id = "table_b16_" + tag + "_" + f.suffix;
            Instant now = Instant.now();
            bars.saveTable(new BarTableRecord(id, barId, barId, "T-" + tag + "-" + f.suffix, tag, type, 4, 3, 0L, "CNY",
                    tableStatus, now, now));
            return id;
        }

        Map<String, Object> accept() {
            return keyed("b16-inv", "invitation_token", invitationToken, "invitation_signature", signature,
                    "table_id", inviteTable, "capacity", 3);
        }

        ResultActions preview(String projectHeader) throws Exception {
            return req.get("/api/v1/customer/bar-invitations/preview", friend, projectHeader, "invitation_token", invitationToken,
                    "invitation_signature", signature, "table_id", inviteTable, "capacity", "3");
        }

        String removePath(String memberId) {
            return "/api/v1/customer/bar-invitations/" + reservation + "/members/" + memberId + "/remove";
        }

        String entryCodePath() {
            return "/api/v1/customer/bar-invitations/" + reservation + "/entry-code";
        }

        String depositPath() {
            return "/api/v1/customer/bar-reservations/" + depositReservation + "/deposit-payments";
        }

        String tablePath() {
            return "/api/v1/customer/bar-tables/" + looseTable;
        }

        Map<String, Object> scanOrder() {
            return keyed("b16-scan", "items", List.of(Map.of("product_id", product, "quantity", 1)));
        }
    }
}
