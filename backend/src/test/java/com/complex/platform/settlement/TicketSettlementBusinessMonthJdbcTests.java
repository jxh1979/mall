package com.complex.platform.settlement;

// 文件版本：V0.01

import static com.complex.platform.support.DailyCloseFixture.markRangeFinal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.support.AppendOnlyTestBypass;
import com.complex.platform.ticketing.IssuedEntitlementRecord;
import com.complex.platform.ticketing.TicketOrderRecord;
import com.complex.platform.ticketing.TicketingRepository;
import com.complex.platform.ticketing.VerificationReversedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 第十八批 X 段 189-R1 通票月度分账按营业日切月真库回归。依据：BSM:1682「全系统的钱账统一按「营业日」归集：营业日从当日上午 10:00 起算，
 * 至次日上午 10:00 止」「起止时点由后台配置（安全阈值，按综合体分别配置」。月窗＝该综合体月首营业日起点至月末营业日结束（平台单取唯一启用综合体，
 * 没有或不止一个时出厂）；撤销追收的原月、冲正落月按同一套营业日。
 *
 * <p>不开测试事务（核销结算事实与冲正事实由消费者在提交后另开事务写入），@AfterEach 清理并把综合体置 suspended 移出判定集合。年份取 2002：
 * 远离他测留库的近年平台单与在营单位（同 159-R2 用 2001 的理由）；综合体建档回填到北京 2002-05-01，6、7 月五类标齐。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class TicketSettlementBusinessMonthJdbcTests {

    private static final long PRICE = 3000L;
    private static final YearMonth JUNE = YearMonth.of(2002, 6);
    private static final YearMonth JULY = YearMonth.of(2002, 7);
    /** 北京 7-1 09:30（出厂营业日属 6-30）。 */
    private static final Instant JUL1_0930 = Instant.parse("2002-07-01T01:30:00Z");
    /** 北京 7-1 10:30（出厂营业日属 7-1）。 */
    private static final Instant JUL1_1030 = Instant.parse("2002-07-01T02:30:00Z");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TicketingRepository ticketingRepository;
    @Autowired private TicketDistributionSettlementService service;
    @Autowired private JdbcSettlementBillRepository repository;
    @Autowired private SettlementPostingService postingService;
    @Autowired private SettlementReversedConsumer consumer;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final long n = Long.parseLong(s, 16);
    private final String project = "p189r1_" + s;
    private final String restaurant = "m189r1_" + s;
    private final String admin = "usr_189r1_admin_" + s;
    private final String staff = "usr_189r1_staff_" + s;
    private final String device = "dev_189r1_" + s;
    private final String typeId = "tt_189r1_" + s;
    private final String catalogCode = "sku_189r1_" + s;
    private final List<String> templateIds = new ArrayList<>();
    private final List<String> codes = new ArrayList<>();

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', ?, 'active', TIMESTAMPTZ '2002-05-01 00:00:00+08', now())", project, "营业月综合体" + n);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, project_id, created_at, updated_at)"
                + " VALUES (?, ?, 'catering', 'active', ?, now(), now())", restaurant, "营业月餐厅" + n, project);
        markRangeFinal(jdbc, "project", project, JUNE.atDay(1), JULY.atEndOfMonth());
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        String bills = "SELECT id FROM settlement_bills WHERE project_id = ?"
                + " OR (project_id IS NULL AND period_start IN (DATE '2002-06-01', DATE '2002-07-01'))";
        jdbc.update("DELETE FROM settlement_item_recoveries WHERE original_bill_id IN (" + bills + ")", project);
        jdbc.update("DELETE FROM settlement_items WHERE bill_id IN (" + bills + ")", project);
        jdbc.update("DELETE FROM settlement_bills WHERE id IN (" + bills + ")", project);
        AppendOnlyTestBypass.update(jdbc, "DELETE FROM settlement_transactions WHERE project_id = ?", project);
        for (String code : codes) {
            jdbc.update("DELETE FROM ticket_verifications WHERE entitlement_id = ?", "ent_" + code);
            jdbc.update("DELETE FROM issued_entitlements WHERE id = ?", "ent_" + code);
            jdbc.update("DELETE FROM ticket_instances WHERE order_id = ?", "order_" + code);
            jdbc.update("DELETE FROM ticket_orders WHERE id = ?", "order_" + code);
        }
        jdbc.update("DELETE FROM ticket_type_entitlements WHERE ticket_type_id = ?", typeId);
        for (String id : templateIds) {
            jdbc.update("DELETE FROM entitlement_templates WHERE id = ?", id);
        }
        jdbc.update("DELETE FROM internal_price_catalog WHERE item_code = ?", catalogCode);
        jdbc.update("DELETE FROM ticket_types WHERE id = ?", typeId);
        jdbc.update("DELETE FROM terminal_devices WHERE id = ?", device);
        jdbc.update("UPDATE projects SET status = 'suspended' WHERE id = ?", project);
    }

    // ① 修复前月窗是 UTC 自然月：两笔都进 7 月平台单、6 月为 0，第一条断言即红。
    @Test
    void monthlyJobCutsPlatformBillsAtBusinessDayStart() throws Exception {
        seedTicketing();
        String early = issue("e");
        String late = issue("l");
        verify(early);
        verify(late);
        AppendOnlyTestBypass.update(jdbc, "UPDATE settlement_transactions SET business_at = ? WHERE entitlement_id = ?",
                Timestamp.from(JUL1_0930), "ent_" + early);
        AppendOnlyTestBypass.update(jdbc, "UPDATE settlement_transactions SET business_at = ? WHERE entitlement_id = ?",
                Timestamp.from(JUL1_1030), "ent_" + late);

        // UTC 2002-08-02 19:00＝北京 8-03 03:00（作业时点）：上月取 2002-07，回看 6、7 两月；过账闸关。
        SettlementMonthlyJob job = new SettlementMonthlyJob(service, repository, postingService,
                Clock.fixed(Instant.parse("2002-08-02T19:00:00Z"), ZoneOffset.UTC), true, false);
        job.backfillMonths = 2;
        job.scheduledSettle();

        assertThat(exclusive(null, JUNE)).as("北京 7-1 09:30 属营业日 6-30").isEqualTo(PRICE);
        assertThat(exclusive(null, JULY)).as("北京 7-1 10:30 属营业日 7-1").isEqualTo(PRICE);
    }

    // ② 修复前月窗是 UTC 自然月：北京 7-1 11:00 进 7 月单、6 月为 0，第一条断言即红。
    @Test
    void approvedNoonBusinessDayPutsJulyFirstElevenIntoJune() {
        Instant effective = Instant.parse("2002-05-01T00:00:00Z");
        jdbc.update("""
                INSERT INTO amusement_business_day_configs(id,project_id,version,zone_id,cutoff_hour,min_cutoff_hour,max_cutoff_hour,
                  close_hour,min_close_hour,max_close_hour,effective_at,status,reason,created_by,created_at,decided_by,decided_at)
                VALUES(?,?,'V0.01','Asia/Shanghai',12,0,23,7,0,23,?,'approved','测试已批准版本','r189_requester',?,'usr_master',?)
                """, "abd_r189_" + s, project, Timestamp.from(effective), Timestamp.from(effective.minusSeconds(7200)),
                Timestamp.from(effective.minusSeconds(3600)));
        seedExclusiveFact("n", Instant.parse("2002-07-01T03:00:00Z"));

        service.settleMonth(project, JUNE);
        service.settleMonth(project, JULY);

        assertThat(exclusive(project, JUNE)).as("起点 12:00 时北京 7-1 11:00 属营业日 6-30").isEqualTo(PRICE);
        assertThat(exclusive(project, JULY)).isZero();
    }

    // ③ 修复前冲正按 UTC 月查 7 月单（无单＝未过账）落回原时刻、进已结付的 6 月被丢，第一条断言即红。
    @Test
    void reversalAfterBusinessMonthPostedLandsNowAndRecoversAgainstThatMonth() {
        String ver = seedExclusiveFact("r", JUL1_0930);
        String june = "sb_r189_" + s;
        jdbc.update("INSERT INTO settlement_bills (id, project_id, period_start, period_end, status, created_at, updated_at)"
                + " VALUES (?, ?, DATE '2002-06-01', DATE '2002-06-30', 'settled', now(), now())", june, project);

        consumer.onVerificationReversed(new VerificationReversedEvent(ver, "rv189_" + s, "ent_" + ver, null, restaurant, 1));

        assertThat(reversalAt("rv189_" + s)).as("6 月单已结付 → 冲正落当月").isAfter(Instant.parse("2002-08-01T02:00:00Z"));
        // 夹具：撤销发生在 7 月（落当月即 7 月）。
        AppendOnlyTestBypass.update(jdbc, "UPDATE settlement_transactions SET business_at = ? WHERE transaction_no = ?",
                Timestamp.from(Instant.parse("2002-07-15T04:00:00Z")), "ST-VR-rv189_" + s);
        String july = service.settleMonth(project, JULY);
        assertThat(jdbc.queryForObject("""
                SELECT COALESCE(SUM(r.amount_cent), 0) FROM settlement_item_recoveries r JOIN settlement_items i ON i.id = r.item_id
                 WHERE i.bill_id = ? AND r.original_bill_id = ?
                """, Long.class, july, june)).as("7 月单追收回指 6 月单").isEqualTo(-PRICE);
    }

    // ④ 修复前月窗是 UTC 自然月：6 月单不含北京 7-1 09:30 那笔，第一条断言即红。
    @Test
    void reversalBeforeBusinessMonthPostedLandsAtOriginalAndRecomputeNetsIt() {
        String ver = seedExclusiveFact("u", JUL1_0930);
        service.settleMonth(project, JUNE);
        assertThat(exclusive(project, JUNE)).isEqualTo(PRICE);

        consumer.onVerificationReversed(new VerificationReversedEvent(ver, "ru189_" + s, "ent_" + ver, null, restaurant, 1));

        assertThat(reversalAt("ru189_" + s)).as("6 月单未过账 → 冲正落原时刻").isEqualTo(JUL1_0930);
        service.settleMonth(project, JUNE);
        assertThat(exclusive(project, JUNE)).as("6 月重算净掉").isZero();
    }

    /** 本商户在该月单（billProject 为空＝平台单）上的专属直付合计，没有单或没有明细为 0。 */
    private long exclusive(String billProject, YearMonth month) {
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(i.final_amount_cent), 0) FROM settlement_items i JOIN settlement_bills b ON b.id = i.bill_id
                 WHERE COALESCE(b.project_id, '') = ? AND b.period_start = ? AND i.merchant_id = ?
                   AND i.distribution_method = 'exclusive_full'
                """, Long.class, billProject == null ? "" : billProject, java.sql.Date.valueOf(month.atDay(1)), restaurant);
    }

    private Instant reversalAt(String reversalId) {
        return jdbc.queryForObject("SELECT business_at FROM settlement_transactions WHERE transaction_no = ?", Instant.class,
                "ST-VR-" + reversalId);
    }

    /** 本餐厅一笔专属直付核销事实（写法同 TicketSettlementFinalSnapshotGateJdbcTests.seedExclusiveFact）。 */
    private String seedExclusiveFact(String tag, Instant businessAt) {
        String ver = "v189_" + tag + "_" + s;
        Timestamp ts = Timestamp.from(businessAt);
        jdbc.update("""
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    verification_id, entitlement_type, verified_count, total_times, num_people,
                    allocated_revenue_cent, verified_amount_cent, unverified_amount_cent, beneficiary_type,
                    merchant_id, project_id, exclusive_merchant_id, settlement_status, entitlement_valid_to,
                    created_at, updated_at, business_at)
                VALUES (?, ?, 'verification_settled', ?, ?, 'dining', 1, 1, 1, ?, ?, 0, 'merchant',
                    ?, ?, ?, 'draft', ?, ?, ?, ?)
                """, "st_vs_" + ver, "ST-VS-" + ver, "ent_" + ver, ver, PRICE, PRICE, restaurant, project, restaurant,
                ts, ts, ts, ts);
        return ver;
    }

    // ---------- ① 真实核销链（照 MerchantConcessionDiningSettlementJdbcIntegrationTests：平台出资餐饮行按目录价专属直付） ----------

    private String templateId;

    private void seedTicketing() throws Exception {
        jdbc.update("""
                INSERT INTO internal_price_catalog (id, item_code, merchant_id, category, item_name,
                    price_cent, status, is_draft, updated_by, created_at, updated_at)
                VALUES (?, ?, ?, 'dining', '套餐', ?, 'active', FALSE, 'seed_189r1', now(), now())
                """, catalogCode, catalogCode, restaurant, PRICE);
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at)"
                + " VALUES (?, ?, 'fixed', FALSE, 'draft', now(), now())", typeId, "营业月通票" + n);
        jdbc.update("INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, status, merchant_id, created_at, updated_at)"
                + " VALUES (?, ?, ?, 'kiosk', 'venue', 'bound', ?, now(), now())", device, device, device, restaurant);
        user(admin, "platform_admin", "org_1", null, "票务策略配置" + n, List.of("ticket.entitlement.manage", "ticket.product.manage"));
        user(staff, "staff", project, restaurant, "餐厅核销员" + n, List.of("verification.execute"));

        String body = mockMvc.perform(post("/api/v1/ticketing/entitlement-templates").with(session(admin, "platform_admin", "org_1", null))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "r189-tpl-%s", "name": "营业月餐饮%d", "entitlement_type": "dining",
                                 "default_total_times": 1, "default_value_cent": 0, "status": "active", "merchant_id": "%s",
                                 "catalog_item_code": "%s", "exclusive_merchant_id": "%s", "bar_item_ref": null}
                                """.formatted(UUID.randomUUID(), n, restaurant, catalogCode, restaurant)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        templateId = new ObjectMapper().readTree(body).path("data").path("id").asText();
        templateIds.add(templateId);
        mockMvc.perform(post("/api/v1/ticketing/ticket-types/" + typeId + "/entitlements").with(session(admin, "platform_admin", "org_1", null))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "r189-bind-%s", "entitlement_template_id": "%s", "quantity": 1,
                                 "total_times": 1, "num_people": 1, "component_price_cent": 0, "cost_bearer": "platform_funded"}
                                """.formatted(UUID.randomUUID(), templateId)))
                .andExpect(status().isOk());
    }

    /**
     * 售出该通票后发到客人手上的餐饮权益（专属该餐厅、平台出资）。197-R1：父票 1 号人份下同发一项入场（admission，归属本店以过餐厅员工商户范围，
     * 码＝餐饮码加「-in」），写法同 MerchantConcessionDiningSettlementJdbcIntegrationTests。
     */
    private String issue(String tag) {
        String code = "r189-" + tag + "-" + s;
        String entry = code + "-in";
        String parent = "ti_" + code;
        codes.add(entry);
        codes.add(code);
        Instant now = Instant.now();
        ticketingRepository.saveTicketOrder(new TicketOrderRecord(
                "order_" + code, "ON-" + code, "cust_" + code, typeId, null, 1,
                LocalDate.now(), "web", null, 0L, null, 0L, 0L, "CNY", "paid", "issued", "created", now, now, null, null, null));
        jdbc.update("INSERT INTO ticket_instances (id, ticket_no, order_id, ticket_type_id, seq_in_order, num_people, total_times,"
                + " status, created_at, updated_at) VALUES (?, ?, ?, ?, 1, 1, 1, 'active', now(), now())",
                parent, "T-" + code, "order_" + code, typeId);
        jdbc.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status,"
                + " created_at, updated_at) VALUES (?, ?, 'admission', 1, 0, 'active', now(), now())",
                "et_" + entry, "本场入场" + n);
        templateIds.add("et_" + entry);
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + entry, entry, typeId, "order_" + code, "et_" + entry, "admission",
                restaurant, null, "point_" + s, 1, 0, 0L, "active",
                now.minusSeconds(60), now.plusSeconds(3600), now, now, 1, null,
                null, null, null, null, parent, 1, null, null));
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + code, code, typeId, "order_" + code, templateId, "dining",
                restaurant, null, "point_" + s, 1, 0, 0L, "active",
                now.minusSeconds(60), now.plusSeconds(3600), now, now, 1, null,
                "virtual", null, null, restaurant, parent, 1, null, null));
        return code;
    }

    /** 餐厅员工在本店核销设备上核销（前端蛇形键）；197-R1 起先核同票同人份的入场（本场检票），再核餐饮。 */
    private void verify(String code) throws Exception {
        scan(code + "-in");
        scan(code);
    }

    private void scan(String code) throws Exception {
        mockMvc.perform(post("/api/v1/ticketing/verifications").with(session(staff, "staff", project, restaurant))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "r189-verify-%s", "credential_type": "qr", "credential_payload": "%s",
                                 "device_id": "%s", "point_id": "point_%s", "business_context": {}}
                                """.formatted(UUID.randomUUID(), code, device, s)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("success"));
    }

    private void user(String userId, String accountType, String organizationId, String merchantId, String roleName,
                      List<String> permissions) {
        String roleId = "role_" + userId;
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, ?, ?, ?, 'active', now())", userId, userId, accountType, organizationId, merchantId);
        jdbc.update("INSERT INTO roles (id, role_name, created_at) VALUES (?, ?, now())", roleId, roleName);
        for (String code : permissions) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", roleId, code);
        }
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
    }

    // 后台 Web 终端 + 生产过滤器放入的 AccessActor 主体（与账号表当前行一致）。
    private RequestPostProcessor session(String userId, String accountType, String organizationId, String merchantId) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(
                    new AccessActor(userId, userId, accountType, organizationId, merchantId, "active"), null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
