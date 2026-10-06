package com.complex.platform.access;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.facility.SiteEntryReportRecord;
import com.complex.platform.facility.SiteEntryReportRepository;
import com.complex.platform.governance.MerchantRecord;
import com.complex.platform.governance.PlatformRepository;
import com.complex.platform.inventory.InventoryRepository;
import com.complex.platform.inventory.InventorySupplierRecord;
import com.complex.platform.leasing.LeasingLeadRecord;
import com.complex.platform.leasing.LeasingRepository;
import com.complex.platform.merchantbilling.MerchantFeeNotificationRecipientRecord;
import com.complex.platform.merchantbilling.MerchantFeeNotificationRecipientRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * M723-F06（操作员 2026-10-03：敏感明文「算用掉，看一次扣一次……普通的商户列表（脱敏）仍然不扣。」）：真实库、真实接口回归。
 * 进场报备、招商线索、费用通知接收人、库存供应商四个读接口按 pii.contact_phone.view 返回明文联系电话时按次消耗：
 * 明文码只有单次来源、业务读权限有长期来源（角色）的员工，第一次读返回明文、来源已消耗、消耗日志一条，第二次读返回掩码；
 * 从未持有明文码的员工始终看掩码、没有消耗日志；长期持有明文码（角色）的员工照常看明文、没有消耗日志。
 * 同一单次来源的两个并发读，返回明文的至多一个。商户列表（M723 已有位点）同样断言第一次明文、第二次掩码。
 * 重复提交（M723-F04 与 F06 叠加）：商户写接口只是返回第一次的结果、本请求新用掉的单次明文来源被回滚时，响应按掩码返回——
 * 不扣就不给明文，否则一张单次明文授权可以靠重复提交旧请求反复看到明文。
 * 修复前（main c40b57dc）四个控制器只做 currentActorHasPermission 的布尔判定、从不消耗：各用例第一次读之后的
 * 「来源已消耗」断言即红，并发用例两个请求都返回明文、「至多一个」断言红。
 *
 * <p>不开测试事务（消耗要真实提交才看得见）。夹具行经各自仓储写入（仓储负责电话加密），按随机后缀隔离、结束时删除；
 * 消耗日志与审计事件（线索、供应商列表的查看留痕）只追加不删，按后缀隔离留在库里。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class SingleUsePlaintextPhoneJdbcIntegrationTests {

    private static final String PHONE_PLAINTEXT = "pii.contact_phone.view";
    private static final String PLAIN = "13912345678";
    private static final String MASKED = "139****5678";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AccessRepository accessRepository;

    @Autowired
    private SiteEntryReportRepository siteEntryReportRepository;

    @Autowired
    private LeasingRepository leasingRepository;

    @Autowired
    private MerchantFeeNotificationRecipientRepository recipientRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private PlatformRepository platformRepository;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    // 明文码只有单次来源
    private final String userId = "usr_m723ph_" + suffix;
    // 从未持有明文码
    private final String noPhoneUserId = "usr_m723ph_n_" + suffix;
    // 长期持有明文码
    private final String longTermUserId = "usr_m723ph_l_" + suffix;
    private final String merchantId = "mch_m723ph_" + suffix;
    private final String reportId = "ser_m723ph_" + suffix;
    private final String leadId = "lead_m723ph_" + suffix;
    private final String recipientId = "mfnr_m723ph_" + suffix;
    private final String supplierId = "supplier_m723ph_" + suffix;
    private final String merchantProjectId = "prj_m723ph_" + suffix;
    private final String archiveKey = "m723ph-archive-" + suffix;
    // 员工所属项目：商户用例改成专门建的项目（商户列表、存档按真实项目校验），其余用例沿用 org
    private String project = "org";

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("DELETE FROM site_entry_reports WHERE id = ?", reportId);
        jdbcTemplate.update("DELETE FROM leasing_leads WHERE id = ?", leadId);
        jdbcTemplate.update("DELETE FROM merchant_fee_notification_recipient WHERE id = ?", recipientId);
        jdbcTemplate.update("DELETE FROM inventory_suppliers WHERE id = ?", supplierId);
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id IN (?, ?, ?)", userId, noPhoneUserId, longTermUserId);
        jdbcTemplate.update("DELETE FROM role_permissions WHERE role_id LIKE ?", "role_m723ph_%" + suffix);
        jdbcTemplate.update("DELETE FROM roles WHERE id LIKE ?", "role_m723ph_%" + suffix);
        jdbcTemplate.update("DELETE FROM temporary_permission_grants WHERE subject_id = ?", userId);
        jdbcTemplate.update("DELETE FROM access_users WHERE id IN (?, ?, ?)", userId, noPhoneUserId, longTermUserId);
        jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key = ?", archiveKey);
        jdbcTemplate.update("DELETE FROM merchants WHERE id = ?", merchantId);
        jdbcTemplate.update("DELETE FROM projects WHERE id = ?", merchantProjectId);
    }

    // 进场报备详情（GET /facility/site-entry-reports/{id}）。修复前红在 assertPlaintextIsUsedUpByOneView 里第一处 assertConsumedOnce()。
    @Test
    void aSiteEntryReportShowsThePlaintextPhoneOncePerSingleUseGrant() throws Exception {
        seedReport();

        assertPlaintextIsUsedUpByOneView(null, "facility.site_entry.review",
                "/api/v1/facility/site-entry-reports/" + reportId, reportId, "contact_phone");
    }

    // 进场报备列表（GET /facility/site-entry-reports）。去掉 SiteEntryReportController.list 上的标记则第一处 assertConsumedOnce() 红。
    @Test
    void theSiteEntryReportListShowsThePlaintextPhoneOncePerSingleUseGrant() throws Exception {
        seedReport();

        assertPlaintextIsUsedUpByOneView(null, "facility.site_entry.review", "/api/v1/facility/site-entry-reports",
                reportId, "contact_phone");
    }

    // 招商线索列表（GET /leasing/leads，按整页判断有没有非空电话）。修复前红在第一处 assertConsumedOnce()。
    @Test
    void theLeasingLeadListShowsThePlaintextPhoneOncePerSingleUseGrant() throws Exception {
        seedLead(Instant.now());

        assertPlaintextIsUsedUpByOneView(null, "leasing.lead.manage", "/api/v1/leasing/leads", leadId, "contact_phone");
    }

    // 待跟进线索列表（GET /leasing/leads/follow-up-due）：夹具线索建于 2000 年、从未跟进，落在待跟进条件内且按时间升序排在页首。
    // 去掉 LeasingController.listLeadsNeedingFollowUp 上的标记则第一处 assertConsumedOnce() 红。
    @Test
    void theFollowUpDueLeadListShowsThePlaintextPhoneOncePerSingleUseGrant() throws Exception {
        seedLead(Instant.parse("2000-01-01T00:00:00Z"));

        assertPlaintextIsUsedUpByOneView(null, "leasing.lead.manage", "/api/v1/leasing/leads/follow-up-due", leadId,
                "contact_phone");
    }

    // 费用通知接收人详情（GET /merchant-billing/fee-notification-recipients/{id}，平台口径）。修复前红在第一处 assertConsumedOnce()。
    @Test
    void aFeeNotificationRecipientShowsThePlaintextPhoneOncePerSingleUseGrant() throws Exception {
        seedRecipient();

        assertPlaintextIsUsedUpByOneView(null, "merchant.billing.manage",
                "/api/v1/merchant-billing/fee-notification-recipients/" + recipientId, recipientId, "recipient_phone");
    }

    // 费用通知接收人列表（GET /merchant-billing/fee-notification-recipients?merchant_id=，平台口径）。
    // 去掉 MerchantFeeNotificationRecipientController.list 上的标记则第一处 assertConsumedOnce() 红。
    @Test
    void theFeeNotificationRecipientListShowsThePlaintextPhoneOncePerSingleUseGrant() throws Exception {
        seedRecipient();

        assertPlaintextIsUsedUpByOneView(null, "merchant.billing.manage",
                "/api/v1/merchant-billing/fee-notification-recipients?merchant_id=" + merchantId, recipientId, "recipient_phone");
    }

    // 商户列表（GET /platform/merchants，M723 已有位点）：第一次明文、来源已消耗，第二次掩码。守护用例：修复前同样绿；
    // 去掉 PlatformController.listMerchants 上的标记则第一处 assertConsumedOnce() 红。
    @Test
    void theMerchantListShowsThePlaintextPhoneOncePerSingleUseGrant() throws Exception {
        seedMerchant();

        assertPlaintextIsUsedUpByOneView(null, "merchant.profile.view", "/api/v1/platform/merchants", merchantId,
                "contact_phone");
    }

    // M723-F04 与 F06 叠加：员工长期持有「管理商户档案」、没有明文码时存档商户成功（响应是掩码，幂等记录里存的是未打码结果）；
    // 之后拿到一张单次明文授权，用同一请求编号、同一请求体重复提交 → 200、提示已完成、电话仍是掩码，单次来源未消耗、没有消耗日志；
    // 再提交一次同样。修复前（只有 F04 回滚分支时）：重复提交返回明文、来源随请求事务回滚仍未消耗，可无限重复——第一处「掩码」断言即红。
    @Test
    void resubmittingAMerchantWriteWithASingleUsePlaintextGrantShowsTheMaskAndDoesNotUseItUp() throws Exception {
        seedMerchant();
        staff(userId, null, "merchant.profile.manage");
        String body = "{\"idempotency_key\": \"" + archiveKey + "\", \"reason\": \"M723 重复提交回归\"}";

        String first = archive(body);
        assertThat(phone(first, merchantId, "contact_phone")).isEqualTo(MASKED);
        assertThat(message(first)).isEqualTo("success");

        singleUsePhoneGrant();
        for (int i = 0; i < 2; i++) {
            String replayed = archive(body);
            assertThat(message(replayed)).isEqualTo(ApiResponse.REPLAYED_MESSAGE);
            assertThat(phone(replayed, merchantId, "contact_phone")).isEqualTo(MASKED);
            assertUnconsumed();
        }
    }

    private String archive(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/platform/merchants/" + merchantId + "/archive").with(session(userId, null))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String message(String body) throws Exception {
        return new ObjectMapper().readTree(body).path("message").asText();
    }

    // 库存供应商列表（GET /inventory/suppliers，按商户范围取数，主体带商户归属）。修复前红在第一处 assertConsumedOnce()。
    @Test
    void theInventorySupplierListShowsThePlaintextPhoneOncePerSingleUseGrant() throws Exception {
        Instant now = Instant.now();
        inventoryRepository.saveInventorySupplier(new InventorySupplierRecord(supplierId, "M723 供应商 " + suffix, "general",
                "联系人", PLAIN, "active", now, now, merchantId));

        assertPlaintextIsUsedUpByOneView(merchantId, "inventory.supplier.view", "/api/v1/inventory/suppliers",
                supplierId, "contact_phone");
    }

    // 同一单次来源的两个并发读（费用通知接收人详情）：返回明文的至多一个，来源恰好消耗一次。没抢到的那个要么 403、要么（排在后面时）看掩码。
    // 修复前两个请求都返回明文，红在「至多一个」断言。
    @Test
    void twoConcurrentViewsOnTheSameSingleUseGrantShowThePlaintextPhoneAtMostOnce() throws Exception {
        seedRecipient();
        staff(userId, null, "merchant.billing.manage");
        singleUsePhoneGrant();
        String url = "/api/v1/merchant-billing/fee-notification-recipients/" + recipientId;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<String> phones = new ArrayList<>();
        try {
            List<Future<MockHttpServletResponse>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return mockMvc.perform(get(url).with(session(userId, null))).andReturn().getResponse();
                }));
            }
            start.countDown();
            for (Future<MockHttpServletResponse> result : results) {
                MockHttpServletResponse response = result.get(60, TimeUnit.SECONDS);
                assertThat(response.getStatus()).isIn(200, 403);
                if (response.getStatus() == 200) {
                    phones.add(phone(response.getContentAsString(StandardCharsets.UTF_8), recipientId, "recipient_phone"));
                }
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(phones).isSubsetOf(PLAIN, MASKED);
        assertThat(phones.stream().filter(PLAIN::equals).count()).isLessThanOrEqualTo(1L);
        assertConsumedOnce();
    }

    // 四个位点共用的场景：明文码只有单次来源的员工看一次明文即消耗、再看是掩码；从未持有的看掩码；长期持有的照常看明文；后两者都没有消耗日志。
    private void assertPlaintextIsUsedUpByOneView(String merchant, String businessCode, String url, String rowId,
                                                  String phoneField) throws Exception {
        staff(userId, merchant, businessCode);
        singleUsePhoneGrant();
        staff(noPhoneUserId, merchant, businessCode);
        staff(longTermUserId, merchant, businessCode, PHONE_PLAINTEXT);

        // 第一次读：明文，单次来源已消耗、消耗日志一条（修复前：明文照返回，但来源未消耗——下一行红）
        assertThat(phone(read(userId, merchant, url), rowId, phoneField)).isEqualTo(PLAIN);
        assertConsumedOnce();
        // 第二次读：掩码，不再有新的消耗（修复前仍是明文）
        assertThat(phone(read(userId, merchant, url), rowId, phoneField)).isEqualTo(MASKED);
        assertConsumedOnce();

        // 从未持有明文码：掩码，没有消耗日志
        assertThat(phone(read(noPhoneUserId, merchant, url), rowId, phoneField)).isEqualTo(MASKED);
        assertThat(consumptionLogs(noPhoneUserId)).isZero();

        // 长期持有明文码：每次都是明文，没有消耗日志
        for (int i = 0; i < 2; i++) {
            assertThat(phone(read(longTermUserId, merchant, url), rowId, phoneField)).isEqualTo(PLAIN);
        }
        assertThat(consumptionLogs(longTermUserId)).isZero();
    }

    private void seedReport() {
        Instant now = Instant.now();
        siteEntryReportRepository.insert(new SiteEntryReportRecord(reportId, "org", null, "M723 明文电话回归",
                LocalDate.now(), LocalDate.now(), 1, "联系人", PLAIN, "vendor_m723ph_" + suffix, "submitted", null, null,
                userId, now, now, List.of(), null, null, null));
    }

    private void seedLead(Instant createdAt) {
        leasingRepository.saveLead(new LeasingLeadRecord(leadId, "联系人", PLAIN, "outbound", null, null, null, null, null,
                "pending_follow_up", null, null, "org", createdAt, createdAt));
    }

    // 商户夹具：专门建一个项目，商户归到该项目下，本用例的员工也改归该项目（商户列表、存档按真实项目校验）。
    private void seedMerchant() {
        Instant now = Instant.now();
        jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                + "VALUES (?, 'complex', 'M723 明文电话回归', 'active', now(), now())", merchantProjectId);
        project = merchantProjectId;
        platformRepository.saveMerchant(new MerchantRecord(merchantId, "M723 商户 " + suffix, "other", null, null, null,
                "联系人", PLAIN, null, null, null, null, null, null, null, null, "active", now, now)
                .withProject(merchantProjectId));
    }

    private void seedRecipient() {
        Instant now = Instant.now();
        recipientRepository.upsertFeeNotificationRecipient(new MerchantFeeNotificationRecipientRecord(recipientId, merchantId,
                "rent", "接收人", PLAIN, "active", userId, now, now, null));
    }

    // 员工账号 + 长期来源：一个只属于该员工的角色，带所列权限码。
    private void staff(String user, String merchant, String... longTermCodes) {
        Instant now = Instant.now();
        accessRepository.createUser(new AccessUser(user, user, "staff", project, merchant, "active", now));
        String roleId = "role_m723ph_" + user.substring("usr_m723ph_".length());
        accessRepository.createRole(new RoleRecord(roleId, "M723 明文电话回归 " + roleId, "M723-F06 回归", "project", "active",
                now, List.of(longTermCodes), "org"));
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", user, roleId);
    }

    private void singleUsePhoneGrant() {
        Instant now = Instant.now();
        accessRepository.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), userId, List.of(PHONE_PLAINTEXT),
                now.minusSeconds(3600), now.plusSeconds(86_400), true, false, "active", "M723 明文手机号单次授权",
                "seed-admin", now, null, null));
        assertUnconsumed();
    }

    private String read(String user, String merchant, String url) throws Exception {
        return mockMvc.perform(get(url).with(session(user, merchant))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    // 取夹具行的电话字段：列表响应在 data.items 里按 id 找，详情响应就是 data。
    private String phone(String body, String rowId, String phoneField) throws Exception {
        JsonNode data = new ObjectMapper().readTree(body).get("data");
        if (!data.has("items")) {
            return data.path(phoneField).asText();
        }
        for (JsonNode item : data.get("items")) {
            if (rowId.equals(item.path("id").asText())) {
                return item.path(phoneField).asText();
            }
        }
        throw new AssertionError("响应里没有夹具行：" + rowId);
    }

    // 后台 Web 终端 + 生产过滤器放入的 AccessActor 主体（与员工表当前行一致）。
    private RequestPostProcessor session(String user, String merchant) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(
                    new AccessActor(user, user, "staff", project, merchant, "active"), null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private void assertUnconsumed() {
        assertThat(jdbcTemplate.queryForList("SELECT consumed FROM temporary_permission_grants WHERE subject_id = ?",
                Boolean.class, userId)).containsExactly(false);
        assertThat(consumptionLogs(userId)).isZero();
    }

    private void assertConsumedOnce() {
        assertThat(jdbcTemplate.queryForList("SELECT consumed FROM temporary_permission_grants WHERE subject_id = ?",
                Boolean.class, userId)).containsExactly(true);
        assertThat(consumptionLogs(userId)).isEqualTo(1);
    }

    private Integer consumptionLogs(String user) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM permission_template_usage_logs WHERE user_id = ? AND result = 'consumed'",
                Integer.class, user);
    }
}
