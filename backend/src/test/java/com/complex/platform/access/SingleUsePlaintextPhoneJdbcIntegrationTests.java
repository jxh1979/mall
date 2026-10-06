package com.complex.platform.access;

// 文件版本：V0.02

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
import java.util.concurrent.Callable;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
 * M723-F11（第 656 行「同一次操作重复提交时，直接返回第一次的结果并提示已完成，不再重复执行、也不再扣减」）：本人持两份独立单次来源
 * （管理商户档案、明文手机号查看各一份）、商户存档入口一次核验两码——同编号同内容两笔并发，业务只执行一次、两份来源各消耗一次，落败的一笔
 * 返回第一次的结果并提示已完成；守护：同号不同内容仍 409（并在同号并发用例末尾）、不同号争用、部分消耗回滚（各一条用例）。
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

    @Autowired
    private PlatformTransactionManager transactionManager;

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

    // M723-F11（第 656 行「同一次操作重复提交时，直接返回第一次的结果并提示已完成，不再重复执行、也不再扣减」；M723-F04「并发同号请求业务
    // 只执行一次、来源只消耗一次」）：员工没有长期来源，持两份独立单次来源——「管理商户档案」一份、「明文手机号查看」一份；商户存档入口把两码
    // 一起核验。同编号同内容两笔并发，同步屏障用来源行的行锁：主线程先锁住两份来源行再放两笔进来，两笔都读到未消耗来源、都停在第一份来源的
    // 消耗上（等到两个会话排在本连接所持的行锁后面，等不到即失败）；放开后首笔把两份都消耗、存档、提交，另一笔在第一份上抢不到。
    // 结果：两笔都 200；恰有一笔真正执行（success、电话明文——它扣了明文来源），另一笔提示已完成、业务结果与首笔完全相同（第一次的结果原样返回）；
    // 存档成功留痕一条；两份来源各消耗一次（两条 consumed、没有 released）。之后同号同内容再提交（顺序重放）同样返回第一次的结果、不再消耗；
    // 同号改内容再提交 409，不再有新的消耗。
    // 修复前另一笔在消耗处直接 403（只在本次只消耗一份来源时才转重放）。
    @Test
    void twoConcurrentSubmissionsCheckingTwoIndependentSingleUseSourcesRunTheActionOnce() throws Exception {
        seedMerchant();
        staffWithTwoIndependentSingleUseSources();
        String body = "{\"idempotency_key\": \"" + archiveKey + "\", \"reason\": \"M723-F11 同号并发\"}";

        List<MockHttpServletResponse> responses = raceOnTheGrantRows(null, () -> postArchive(body), () -> postArchive(body));

        assertThat(responses.stream().map(MockHttpServletResponse::getStatus).toList()).containsExactly(200, 200);
        String first = null;
        String other = null;
        for (MockHttpServletResponse response : responses) {
            String content = response.getContentAsString(StandardCharsets.UTF_8);
            if ("success".equals(message(content))) {
                assertThat(first).as("恰有一笔真正执行").isNull();
                first = content;
            } else {
                other = content;
            }
        }
        assertThat(first).as("恰有一笔真正执行").isNotNull();
        assertThat(other).isNotNull();
        assertThat(message(other)).isEqualTo(ApiResponse.REPLAYED_MESSAGE);
        assertThat(phone(first, merchantId, "contact_phone")).isEqualTo(PLAIN);
        assertThat(data(other)).isEqualTo(data(first));
        assertThat(archivedEvents(archiveKey)).isEqualTo(1);
        assertBothSourcesConsumedOnce();

        // 同号同内容再提交（顺序重放）：同样返回第一次的结果并提示已完成，不再执行、不再消耗
        String again = archive(body);
        assertThat(message(again)).isEqualTo(ApiResponse.REPLAYED_MESSAGE);
        assertThat(data(again)).isEqualTo(data(first));
        assertThat(archivedEvents(archiveKey)).isEqualTo(1);
        assertBothSourcesConsumedOnce();

        // 守护：同编号不同内容仍 409，不再有新的消耗
        mockMvc.perform(post("/api/v1/platform/merchants/" + merchantId + "/archive").with(session(userId, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\": \"" + archiveKey + "\", \"reason\": \"M723-F11 改了内容\"}"))
                .andExpect(status().isConflict());
        assertThat(archivedEvents(archiveKey)).isEqualTo(1);
        assertBothSourcesConsumedOnce();
    }

    // M723-F11 守护（不同请求编号争用同两份来源）：两笔编号不同、都先读到未消耗来源、都停在第一份来源的消耗上；至多一笔成功、另一笔
    // 403 PERMISSION_DENIED（它抢不到的来源不是本请求编号用掉的，不转重放）；存档只做一次，两份来源各消耗一次、没有多出的消耗留痕。
    @Test
    void twoDifferentRequestsRacingOnTheSameTwoSingleUseSourcesSucceedAtMostOnce() throws Exception {
        seedMerchant();
        staffWithTwoIndependentSingleUseSources();
        String otherKey = "m723ph-archive-b-" + suffix;
        String bodyA = "{\"idempotency_key\": \"" + archiveKey + "\", \"reason\": \"M723-F11 不同号争用\"}";
        String bodyB = "{\"idempotency_key\": \"" + otherKey + "\", \"reason\": \"M723-F11 不同号争用\"}";
        try {
            List<MockHttpServletResponse> responses = raceOnTheGrantRows(null, () -> postArchive(bodyA), () -> postArchive(bodyB));

            assertThat(responses.stream().map(MockHttpServletResponse::getStatus).toList()).containsExactlyInAnyOrder(200, 403);
            for (MockHttpServletResponse response : responses) {
                String content = response.getContentAsString(StandardCharsets.UTF_8);
                if (response.getStatus() == 200) {
                    assertThat(message(content)).isEqualTo("success");
                } else {
                    assertThat(new ObjectMapper().readTree(content).path("detail_code").asText()).isEqualTo("PERMISSION_DENIED");
                }
            }
            assertThat(archivedEvents(archiveKey) + archivedEvents(otherKey)).isEqualTo(1);
            assertBothSourcesConsumedOnce();
        } finally {
            jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key = ?", otherKey);
        }
    }

    // M723-F11 守护（部分消耗回滚）：主线程只锁住「明文手机号查看」那份来源；请求先把「管理商户档案」那份消耗掉（在请求事务里），停在第二份
    // 的消耗上（等到一个会话排在本连接所持的行锁后面，等不到即失败）；这时主线程把第二份标成已消耗（相当于被别的请求抢走）再放开。请求在第二份上抢不到：
    // 本次调用已经新消耗过一份，不转重放——403 PERMISSION_DENIED，整笔回滚：第一份仍未消耗、没有消耗留痕、商户没有被存档。
    @Test
    void aRequestThatLosesItsSecondSingleUseSourceRollsBackTheFirstOneItHadConsumed() throws Exception {
        seedMerchant();
        staffWithTwoIndependentSingleUseSources();
        String body = "{\"idempotency_key\": \"" + archiveKey + "\", \"reason\": \"M723-F11 部分消耗回滚\"}";

        List<MockHttpServletResponse> responses = raceOnTheGrantRows(PHONE_PLAINTEXT, () -> postArchive(body));

        assertThat(responses.get(0).getStatus()).isEqualTo(403);
        assertThat(new ObjectMapper().readTree(responses.get(0).getContentAsString(StandardCharsets.UTF_8)).path("detail_code").asText())
                .isEqualTo("PERMISSION_DENIED");
        assertThat(consumedByCode()).containsExactlyInAnyOrder(MERCHANT_MANAGE + "=false", PHONE_PLAINTEXT + "=true");
        assertThat(consumptionLogs(userId)).isZero();
        assertThat(archivedEvents(archiveKey)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM merchants WHERE id = ? AND archived_at IS NOT NULL",
                Integer.class, merchantId)).isZero();
    }

    private static final String MERCHANT_MANAGE = "merchant.profile.manage";

    // 没有长期来源的员工（归到商户夹具的项目）＋两份独立的单次来源：管理商户档案一份、明文手机号查看一份
    private void staffWithTwoIndependentSingleUseSources() {
        Instant now = Instant.now();
        accessRepository.createUser(new AccessUser(userId, userId, "staff", project, null, "active", now));
        for (String code : List.of(MERCHANT_MANAGE, PHONE_PLAINTEXT)) {
            accessRepository.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), userId, List.of(code),
                    now.minusSeconds(3600), now.plusSeconds(86_400), true, false, "active", "M723-F11 独立单次来源 " + code,
                    "seed-admin", now, null, null));
        }
        assertThat(consumedByCode()).containsExactlyInAnyOrder(MERCHANT_MANAGE + "=false", PHONE_PLAINTEXT + "=false");
    }

    private MockHttpServletResponse postArchive(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/platform/merchants/" + merchantId + "/archive").with(session(userId, null))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse();
    }

    // 同步屏障：主线程在一个事务里锁住本人的来源行（lockedCode 为空锁全部，否则只锁该权限码那份），放请求进来，等到每个请求都排在本连接
    // 所持的行锁后面（沿 pg_blocking_pids 的等待关系从本连接往下数——同一行的第二个等待者排在第一个等待者后面；30 秒内等不到或等待被中断
    // 即失败——屏障没命中的并发用例不算数）。只锁一份时，放开之前
    // 把这份标成已消耗（相当于被别的请求抢走）。事务提交即放开，然后收齐各请求的响应（按提交顺序）。
    @SafeVarargs
    private List<MockHttpServletResponse> raceOnTheGrantRows(String lockedCode, Callable<MockHttpServletResponse>... requests)
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(requests.length);
        List<MockHttpServletResponse> responses = new ArrayList<>();
        try {
            List<Future<MockHttpServletResponse>> results = new ArrayList<>();
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                if (lockedCode == null) {
                    jdbcTemplate.queryForList("SELECT id FROM temporary_permission_grants WHERE subject_id = ? FOR UPDATE", userId);
                } else {
                    jdbcTemplate.queryForList(
                            "SELECT id FROM temporary_permission_grants WHERE subject_id = ? AND permission_code = ? FOR UPDATE",
                            userId, lockedCode);
                }
                for (Callable<MockHttpServletResponse> request : requests) {
                    results.add(pool.submit(request));
                }
                awaitBlockedByThisConnection(requests.length);
                if (lockedCode != null) {
                    jdbcTemplate.update("UPDATE temporary_permission_grants SET consumed = TRUE, consumed_at = now() "
                            + "WHERE subject_id = ? AND permission_code = ?", userId, lockedCode);
                }
            });
            for (Future<MockHttpServletResponse> result : results) {
                responses.add(result.get(60, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }
        return responses;
    }

    private void awaitBlockedByThisConnection(int expected) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (true) {
            // 本事务里第一次读会话视图后 PostgreSQL 会缓存会话列表；不清掉的话，之后连接池才新开的连接在本事务里一直看不见
            jdbcTemplate.execute("SELECT pg_stat_clear_snapshot()");
            Integer blocked = jdbcTemplate.queryForObject("""
                    WITH RECURSIVE queued(pid) AS (
                        SELECT a.pid FROM pg_stat_activity a WHERE pg_backend_pid() = ANY(pg_blocking_pids(a.pid))
                        UNION
                        SELECT a.pid FROM pg_stat_activity a JOIN queued q ON q.pid = ANY(pg_blocking_pids(a.pid))
                    )
                    SELECT count(*) FROM queued
                    """, Integer.class);
            if (blocked != null && blocked >= expected) {
                return;
            }
            if (System.nanoTime() > deadline) {
                List<String> sessions = jdbcTemplate.queryForList("""
                        SELECT pid || ' ' || coalesce(state, '-') || ' ' || coalesce(wait_event_type, '-') || '/' || coalesce(wait_event, '-')
                               || ' blockers=' || pg_blocking_pids(pid)::text || ' ' || left(regexp_replace(query, '[[:space:]]+', ' ', 'g'), 100)
                          FROM pg_stat_activity WHERE datname = current_database() AND pid <> pg_backend_pid()
                        """, String.class);
                StringBuilder stacks = new StringBuilder();
                Thread.getAllStackTraces().forEach((thread, frames) -> {
                    if (thread.getName().startsWith("pool-") && frames.length > 0) {
                        stacks.append(" || ").append(thread.getName()).append(' ').append(thread.getState());
                        java.util.Arrays.stream(frames).filter(frame -> frame.getClassName().startsWith("com.complex.")
                                        || frame.getClassName().startsWith("com.zaxxer.") || frame.getClassName().startsWith("org.postgresql.core.v3.QueryExecutorImpl"))
                                .limit(14).forEach(frame -> stacks.append(" < ").append(frame.getClassName().substring(frame.getClassName().lastIndexOf('.') + 1))
                                        .append('.').append(frame.getMethodName()).append(':').append(frame.getLineNumber()));
                    }
                });
                throw new AssertionError("同步屏障未命中：30 秒内排在本用例所持来源行锁后面的会话 " + blocked + " 个，需要 " + expected
                        + " 个；本库其它会话：" + sessions + "；在途请求线程：" + stacks);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError("同步屏障等待被中断", interrupted);
            }
        }
    }

    // 两份来源各消耗一次：来源行都已消耗，消耗留痕恰好两条（每份一条），没有撤回留痕
    private void assertBothSourcesConsumedOnce() {
        assertThat(consumedByCode()).containsExactlyInAnyOrder(MERCHANT_MANAGE + "=true", PHONE_PLAINTEXT + "=true");
        assertThat(jdbcTemplate.queryForList(
                "SELECT permission_code FROM permission_template_usage_logs WHERE user_id = ? AND result = 'consumed'",
                String.class, userId)).containsExactlyInAnyOrder(MERCHANT_MANAGE, PHONE_PLAINTEXT);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM permission_template_usage_logs WHERE user_id = ? AND result = 'released'",
                Integer.class, userId)).isZero();
    }

    private List<String> consumedByCode() {
        return jdbcTemplate.queryForList(
                "SELECT permission_code || '=' || consumed::text FROM temporary_permission_grants WHERE subject_id = ?", String.class, userId);
    }

    private Integer archivedEvents(String idempotencyKey) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events WHERE event_name = 'merchant.archived' AND event_result = 'success' AND idempotency_key = ?",
                Integer.class, idempotencyKey);
    }

    // 响应包络的 request_id / server_time 每次不同，比较第一次与重放的业务结果只看 data
    private JsonNode data(String body) throws Exception {
        return new ObjectMapper().readTree(body).get("data");
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
