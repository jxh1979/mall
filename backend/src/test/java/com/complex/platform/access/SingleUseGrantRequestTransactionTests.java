package com.complex.platform.access;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.idempotency.IdempotentReplayContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
 * XM-M002-J（M002-F18 / CODEX-M002-6-02；治理地图「权限规则」「临时授权可设有效期或单次使用；……单次消耗……全部留痕」）：
 * 真实库、真实接口回归。单次临时授权只在请求成功时消耗一次：缺幂等键、业务校验失败都不留消耗与消耗日志，失败留痕照常；
 * 同一单次来源两笔并发最多一笔成功；幂等重放不再消耗。另以非 access 包的风险预警上报接口证明对全平台生效。
 * 闸口链「先写入、再拒绝」的产物（准入拦截复核单与 admission.blocked、terminal.access_rejected）走独立事务，
 * 请求事务回滚单次消耗时它们仍在（酒吧免单接口，V068 种子策略 bar_bill_waiver_block 恒拦截）。
 * V0.02（操作员 2026-09-25 答复：单次授权只在业务成功并返回结果时才算用掉，同一请求编号重放应返回第一次的结果）：
 * 同一操作人以同一幂等键重放成功的请求 → 返回第一次的结果、不再消耗、业务不再执行；换幂等键 403、改正文 409、
 * 换操作人：同项目同商户且自己持未用掉来源的返回第一次的结果（见 M723-F04），已无可用来源的 403；幂等记录不在时 403；
 * 第一次失败后同键再发按首次处理。
 * M723：查看（GET）与以提交方式实现的预览、查询不消耗单次授权，接口声明仍然扣的码（导出、明文手机号查看；操作员 2026-10-03）照旧消耗；
 * 同一请求里分层核验同一单次来源只算一次（账本 XM-M001-W、XM-M002-AU、M002 第 75 行）。
 * M723-F04（操作员 2026-09-25 确认句「同一次操作重复提交时，直接返回第一次的结果并提示已完成，不再重复执行、也不再扣减」；
 * 2026-10-03「同一项目、同一商户的员工用同一个提交编号，只算一次、返回第一次的结果」）：重放的响应消息提示已完成；
 * 同项目同商户的同事用自己的单次来源重复提交、本人持第二份单次来源重复提交，都拿到第一次的结果，新的来源不算用掉。
 * M723 第 4 轮（F07、F09、F10）：企微原文模式按规范化后的取值核权；写请求上下文里没有实际事务时命中单次来源不消耗、按不具备处理
 * （真实接口按「请求进入—判定期间授出—核权—拒绝」的顺序复现）；本人同编号并发提交落败的一笔转为返回第一次的结果——只对经幂等层、
 * 第一次已成功完成的请求，不经幂等层的入口（招商线索创建、月报导出）落败的一笔照旧 403、业务只执行一次。
 *
 * <p>本类不开测试事务：消耗、回滚、并发都要真实提交才看得见。夹具按随机后缀隔离、结束时删除；
 * 消耗日志、审计事件、风险预警及其流水只追加不删（V140 触发器），按后缀隔离留在库里。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class SingleUseGrantRequestTransactionTests {

    private static final String ROLE_MANAGE = "access.role.manage";
    private static final String GRANT_REQUEST_VIEW = "access.grant_request.view";
    private static final String RISK_REPORT = "risk_alert.report";
    private static final String BAR_BILL_ADJUST = "bar.bill.adjust";

    @Autowired
    private JdbcAccessRepository repository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String userId = "usr_xmj_" + suffix;
    private final String otherUserId = "usr_xmj2_" + suffix;
    private final String merchantId = "mch_xmj_" + suffix;

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id IN (?, ?)", userId, otherUserId);
        jdbcTemplate.update("DELETE FROM role_permissions WHERE role_id IN (SELECT id FROM roles WHERE role_name LIKE ?)", roleName("%"));
        jdbcTemplate.update("DELETE FROM roles WHERE role_name LIKE ?", roleName("%"));
        jdbcTemplate.update("DELETE FROM temporary_permission_grants WHERE subject_id IN (?, ?)", userId, otherUserId);
        jdbcTemplate.update("DELETE FROM terminal_capability_switches WHERE scope_type = 'merchant' AND scope_id = ?", merchantId);
        jdbcTemplate.update("DELETE FROM data_cleanup_jobs WHERE requested_by = ?", userId);
        jdbcTemplate.update("DELETE FROM wecom_conversations WHERE id = ?", "wconv_xmj_" + suffix);
        jdbcTemplate.update("DELETE FROM leasing_leads WHERE contact_name = ?", roleName("线索"));
        jdbcTemplate.update("DELETE FROM access_users WHERE id IN (?, ?)", userId, otherUserId);
        jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key LIKE ?", "xmj-%" + suffix);
    }

    @Test
    void singleUseRoleRequestsConsumeOnlyOnSuccessAndTheReplayReturnsTheFirstResult() throws Exception {
        staffWithSingleUseGrant(null, ROLE_MANAGE);

        // 缺幂等键：控制器在核权限之后才校验，修复前此处已把单次授权耗掉
        mockMvc.perform(post("/api/v1/access/roles").with(session(null))
                        .contentType(MediaType.APPLICATION_JSON).content(roleJson(null, roleName("甲"), "[]")))
                .andExpect(status().isBadRequest());
        assertUnconsumed();

        // 业务校验失败（未知权限码，服务层抛错）：消耗随请求回滚，失败留痕（独立事务）照常落库
        String failedKey = "xmj-role-unknown-" + suffix;
        mockMvc.perform(post("/api/v1/access/roles").with(session(null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roleJson(failedKey, roleName("乙"), "[\"xm_m002_j.unknown_" + suffix + "\"]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("UNKNOWN_PERMISSION"));
        assertUnconsumed();
        assertThat(failureEvents("role.change_failed", failedKey)).isEqualTo(1);

        // 同一请求第一次业务失败（同项目已有同名角色，唯一约束 409）：不扣减；清掉占名角色后同键、同正文再发，按首次处理
        String okKey = "xmj-role-ok-" + suffix;
        String body = roleJson(okKey, roleName("丙"), "[]");
        String blockingRoleId = "role_xmj_block_" + suffix;
        repository.createRole(new RoleRecord(blockingRoleId, roleName("丙"), "XM-M002-J 占名", "project", "active",
                Instant.now(), List.of(), "org"));
        mockMvc.perform(post("/api/v1/access/roles").with(session(null))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        assertUnconsumed();
        jdbcTemplate.update("DELETE FROM roles WHERE id = ?", blockingRoleId);

        // 恰好消耗一次、建出一个角色
        String first = mockMvc.perform(post("/api/v1/access/roles").with(session(null))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertConsumedOnce();
        assertThat(rolesCreated()).isEqualTo(1);
        String roleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE role_name = ?", String.class, roleName("丙"));
        assertThat(first).contains(roleId);

        // 同一操作人、同一幂等键、同一请求重放：返回第一次的结果（同一角色）并提示已完成（M723-F04），不再消耗、不再建角色
        String replayed = mockMvc.perform(post("/api/v1/access/roles").with(session(null))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(roleId))
                .andExpect(jsonPath("$.message").value(ApiResponse.REPLAYED_MESSAGE))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(data(replayed)).isEqualTo(data(first));
        assertConsumedOnce();
        assertThat(rolesCreated()).isEqualTo(1);

        // 同一幂等键改了正文：幂等层按正文不同拒绝（409），不执行
        mockMvc.perform(post("/api/v1/access/roles").with(session(null))
                        .contentType(MediaType.APPLICATION_JSON).content(roleJson(okKey, roleName("丁"), "[]")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("IDEMPOTENCY_CONFLICT"));
        // 换幂等键：不是用掉单次授权的那次请求，照旧 403
        mockMvc.perform(post("/api/v1/access/roles").with(session(null))
                        .contentType(MediaType.APPLICATION_JSON).content(roleJson("xmj-role-new-" + suffix, roleName("丙"), "[]")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        assertConsumedOnce();
        assertThat(rolesCreated()).isEqualTo(1);

        // 第一次的幂等记录不在（清理、丢失）：没有可返回的结果，不得放行
        jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key = ?", okKey);
        mockMvc.perform(post("/api/v1/access/roles").with(session(null))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        assertConsumedOnce();
        assertThat(rolesCreated()).isEqualTo(1);
    }

    // 换操作人：同商户两名员工，乙已用掉自己的单次授权（已无可用来源），拿甲的幂等键与正文重放 → 权限核验处 403，
    // 拿不到甲的结果、也不会再建；甲本人重放照常返回第一次的结果。乙自己还持有未用掉的单次来源时见下一条用例（M723-F04）
    @Test
    void anotherOperatorCannotReplayARequestThatConsumedSomeoneElsesSingleUseGrant() throws Exception {
        staffWithSingleUseGrant(merchantId, ROLE_MANAGE);
        singleUseGrant(otherUserId, merchantId, ROLE_MANAGE);
        String firstKey = "xmj-role-a-" + suffix;
        String firstBody = roleJson(firstKey, roleName("甲方"), "[]");
        String first = mockMvc.perform(post("/api/v1/access/roles").with(session(userId, merchantId))
                        .contentType(MediaType.APPLICATION_JSON).content(firstBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        mockMvc.perform(post("/api/v1/access/roles").with(session(otherUserId, merchantId))
                        .contentType(MediaType.APPLICATION_JSON).content(roleJson("xmj-role-b-" + suffix, roleName("乙方"), "[]")))
                .andExpect(status().isOk());
        assertThat(rolesCreated()).isEqualTo(2);

        mockMvc.perform(post("/api/v1/access/roles").with(session(otherUserId, merchantId))
                        .contentType(MediaType.APPLICATION_JSON).content(firstBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        assertThat(rolesCreated()).isEqualTo(2);
        assertThat(consumptionLogs(userId)).isEqualTo(1);
        assertThat(consumptionLogs(otherUserId)).isEqualTo(1);

        mockMvc.perform(post("/api/v1/access/roles").with(session(userId, merchantId))
                        .contentType(MediaType.APPLICATION_JSON).content(firstBody))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(data(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                        .isEqualTo(data(first)));
        assertThat(rolesCreated()).isEqualTo(2);
        assertThat(consumptionLogs(userId)).isEqualTo(1);
    }

    // M723-F04（操作员 2026-10-03「同一项目、同一商户的员工用同一个提交编号，只算一次、返回第一次的结果」；2026-09-25 确认句
    // 「同一次操作重复提交时，直接返回第一次的结果并提示已完成，不再重复执行、也不再扣减」）：同项目同商户的同事乙自己持一份未用掉的
    // 同码单次来源，用甲的请求编号与相同请求体重复提交 → 200、甲那次的结果、提示已完成；角色只建一个；乙的来源仍未消耗、没有乙的消耗日志
    // （乙的来源在权限核验处先被用掉，幂等层只返回第一次的结果，请求事务回滚）。修复前乙的来源在权限核验返回时即消耗并提交，消息固定为 success。
    @Test
    void aColleagueResubmittingWithTheirOwnSingleUseGrantGetsTheFirstResultWithoutUsingItUp() throws Exception {
        staffWithSingleUseGrant(merchantId, ROLE_MANAGE);
        singleUseGrant(otherUserId, merchantId, ROLE_MANAGE);
        String body = roleJson("xmj-role-shared-" + suffix, roleName("同事重复提交"), "[]");
        String first = mockMvc.perform(post("/api/v1/access/roles").with(session(userId, merchantId))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertConsumedOnce();
        assertThat(rolesCreated()).isEqualTo(1);

        String replayed = mockMvc.perform(post("/api/v1/access/roles").with(session(otherUserId, merchantId))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(ApiResponse.REPLAYED_MESSAGE))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(data(replayed)).isEqualTo(data(first));
        assertThat(rolesCreated()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForList("SELECT consumed FROM temporary_permission_grants WHERE subject_id = ?",
                Boolean.class, otherUserId)).containsExactly(false);
        assertThat(consumptionLogs(otherUserId)).isZero();
        assertConsumedOnce();
    }

    // M723-F04（同一确认句「不再重复执行、也不再扣减」）：本人对同一权限码持两份单次来源，首次成功消耗一份；同一请求编号、同一请求体
    // 重放 → 200、第一次的结果、提示已完成；第二份仍未消耗、消耗日志仍是一条、角色只建一个。修复前重放在权限核验处把第二份也扣掉。
    @Test
    void theOwnerResubmittingWhileHoldingASecondSingleUseGrantDoesNotUseUpTheSecondOne() throws Exception {
        staffWithSingleUseGrant(null, ROLE_MANAGE);
        Instant now = Instant.now();
        repository.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), userId, List.of(ROLE_MANAGE),
                now.minusSeconds(3600), now.plusSeconds(86_400), true, false, "active", "M723-F04 第二份单次来源",
                "seed-admin", now, null, null));
        String body = roleJson("xmj-role-twice-" + suffix, roleName("两份来源"), "[]");
        String first = mockMvc.perform(post("/api/v1/access/roles").with(session(null))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(consumedFlags()).containsExactlyInAnyOrder(true, false);
        assertThat(consumptionLogs()).isEqualTo(1);

        String replayed = mockMvc.perform(post("/api/v1/access/roles").with(session(null))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(ApiResponse.REPLAYED_MESSAGE))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(data(replayed)).isEqualTo(data(first));
        assertThat(consumedFlags()).containsExactlyInAnyOrder(true, false);
        assertThat(consumptionLogs()).isEqualTo(1);
        assertThat(rolesCreated()).isEqualTo(1);
    }

    // M723-F04「不同项目或不同商户不串用结果」：另一个项目的员工乙（自己持一份未用掉的同码单次来源）用甲的请求编号与相同请求体提交 →
    // 幂等范围按所属项目＋所属商户隔离，不认甲的结果，按首次执行：200、消息 success、建出第二个角色（角色名按项目唯一），乙的来源消耗一次。
    @Test
    void aSubmissionFromAnotherProjectWithTheSameRequestNumberIsExecutedOnItsOwn() throws Exception {
        staffWithSingleUseGrant(merchantId, ROLE_MANAGE);
        String otherProject = "org_xmj_" + suffix;
        Instant now = Instant.now();
        repository.createUser(new AccessUser(otherUserId, otherUserId, "staff", otherProject, merchantId, "active", now));
        repository.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), otherUserId, List.of(ROLE_MANAGE),
                now.minusSeconds(3600), now.plusSeconds(86_400), true, false, "active", "M723-F04 另一项目的单次来源",
                "seed-admin", now, null, null));
        RequestPostProcessor otherSession = request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(
                    new AccessActor(otherUserId, otherUserId, "staff", otherProject, merchantId, "active"), null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
        String body = roleJson("xmj-role-cross-" + suffix, roleName("跨项目同号"), "[]");
        String first = mockMvc.perform(post("/api/v1/access/roles").with(session(userId, merchantId))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertConsumedOnce();

        String second = mockMvc.perform(post("/api/v1/access/roles").with(otherSession)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(data(second).get("id").asText()).isNotEqualTo(data(first).get("id").asText());
        assertThat(rolesCreated()).isEqualTo(2);
        assertThat(jdbcTemplate.queryForList("SELECT consumed FROM temporary_permission_grants WHERE subject_id = ?",
                Boolean.class, otherUserId)).containsExactly(true);
        assertThat(consumptionLogs(otherUserId)).isEqualTo(1);
        assertConsumedOnce();
    }

    private List<Boolean> consumedFlags() {
        return jdbcTemplate.queryForList("SELECT consumed FROM temporary_permission_grants WHERE subject_id = ?",
                Boolean.class, userId);
    }

    @Test
    void twoConcurrentRequestsOnTheSameSingleUseGrantSucceedAtMostOnce() throws Exception {
        staffWithSingleUseGrant(null, ROLE_MANAGE);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (String name : List.of("丁", "戊")) {
                results.add(pool.submit(() -> {
                    start.await();
                    return mockMvc.perform(post("/api/v1/access/roles").with(session(null))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(roleJson("xmj-role-race-" + name + "-" + suffix, roleName(name), "[]")))
                            .andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) {
                statuses.add(result.get(60, TimeUnit.SECONDS));
            }
            assertThat(statuses).containsExactlyInAnyOrder(200, 403);
        } finally {
            pool.shutdownNow();
        }
        assertConsumedOnce();
        assertThat(rolesCreated()).isEqualTo(1);
    }

    // M723-F04「并发同号请求业务只执行一次、来源只消耗一次」、M723-F10（第 656 行「同一次操作重复提交时，直接返回第一次的结果并提示已完成，
    // 不再重复执行、也不再扣减」）：同一人、唯一单次来源、同一请求编号与请求体两笔并发建角色。同步屏障用来源行的行锁：主线程先锁住来源行
    // 再放两笔进来——两笔都读到未消耗来源，都停在消耗行的 UPDATE 上（等到 pg_stat_activity 里两条被阻塞）；放开后首笔消耗、执行、提交，
    // 另一笔的条件更新才返回 0 行。另一笔须 200、业务结果与首笔相同、提示已完成（若在首笔完成前拿到 409 IDEMPOTENCY_IN_PROGRESS，
    // 再同号重发一次须 200＋已完成）；角色只建一个、来源只消耗一次。修复前另一笔在消耗处直接 403。
    @Test
    void twoConcurrentSubmissionsOfTheSameRequestRunTheActionOnceAndConsumeOnce() throws Exception {
        staffWithSingleUseGrant(null, ROLE_MANAGE);
        String body = roleJson("xmj-role-same-" + suffix, roleName("并发同号"), "[]");
        List<MockHttpServletResponse> responses = raceTwoOnTheGrantRow(() -> mockMvc.perform(post("/api/v1/access/roles")
                        .with(session(null)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse());
        assertThat(responses.stream().map(MockHttpServletResponse::getStatus).toList()).contains(200).isSubsetOf(200, 409);
        String first = null;
        String other = null;
        for (MockHttpServletResponse response : responses) {
            String content = response.getContentAsString(StandardCharsets.UTF_8);
            if (response.getStatus() == 200 && "success".equals(message(content))) {
                first = content;
            } else {
                other = content;
            }
        }
        assertThat(first).as("恰有一笔真正执行").isNotNull();
        assertThat(other).isNotNull();
        if (!ApiResponse.REPLAYED_MESSAGE.equals(message(other))) {
            // 首笔尚未完成时另一笔在幂等占位上拿到 409：再同号重发一次
            assertThat(new ObjectMapper().readTree(other).get("detail_code").asText()).isEqualTo("IDEMPOTENCY_IN_PROGRESS");
            other = mockMvc.perform(post("/api/v1/access/roles").with(session(null))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        }
        assertThat(message(other)).isEqualTo(ApiResponse.REPLAYED_MESSAGE);
        assertThat(data(other)).isEqualTo(data(first));
        assertConsumedOnce();
        assertThat(rolesCreated()).isEqualTo(1);
    }

    // 同步屏障（M723-F09／F10 覆盖缺口，CODEX-M723-5-02）：在本用例持锁的事务里调用，等到 expected 个会话排在本连接所持的来源行锁后面——
    // 只认排在本用例这把锁后面的会话，不认库里别的阻塞。同一行的第二个等待者排在第一个等待者后面（pg_blocking_pids 只给出它前面那个会话），
    // 所以沿等待关系从本连接的后端进程号往下数。时限内等不到或等待被中断即判失败：屏障没命中的并发用例不算数，不能退化成先后执行还照样通过。
    private void awaitBlockedOnGrantRow(int expected) {
        awaitBlockedOnGrantRow(expected, 30_000);
    }

    private void awaitBlockedOnGrantRow(int expected, long timeoutMillis) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
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
                throw new AssertionError("同步屏障未命中：" + timeoutMillis + " 毫秒内排在本用例所持来源行锁后面的会话 " + blocked
                        + " 个，需要 " + expected + " 个；本库其它会话：" + sessions + "；在途请求线程：" + stacks);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError("同步屏障等待被中断", interrupted);
            }
        }
    }

    // 屏障辅助方法自测：本用例持锁的事务没有挡住任何会话时，短时限内必抛断言错误（用它的并发用例因此不会在屏障没命中时照样通过）。
    @Test
    void theSynchronisationBarrierFailsWhenNoRequestReachesTheLock() {
        staffWithSingleUseGrant(null, ROLE_MANAGE);
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbcTemplate.queryForList("SELECT id FROM temporary_permission_grants WHERE subject_id = ? FOR UPDATE", userId);
            awaitBlockedOnGrantRow(1, 500);
        })).isInstanceOf(AssertionError.class).hasMessageContaining("同步屏障未命中");
        assertUnconsumed();
    }

    private String message(String body) throws Exception {
        return new ObjectMapper().readTree(body).get("message").asText();
    }

    private String detailCode(String body) throws Exception {
        return new ObjectMapper().readTree(body).get("detail_code").asText();
    }

    // 同号并发的同步屏障：主线程先锁住本人的来源行再放两笔进来——两笔都读到未消耗来源，都停在消耗行的 UPDATE 上（等到两个会话排在本连接
    // 所持的行锁后面，等不到即失败）；放开后先拿到锁的一笔消耗、执行、提交，另一笔的条件更新才返回 0 行。
    private List<MockHttpServletResponse> raceTwoOnTheGrantRow(Callable<MockHttpServletResponse> request) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<MockHttpServletResponse> responses = new ArrayList<>();
        try {
            List<Future<MockHttpServletResponse>> results = new ArrayList<>();
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                jdbcTemplate.queryForList("SELECT id FROM temporary_permission_grants WHERE subject_id = ? FOR UPDATE", userId);
                for (int i = 0; i < 2; i++) {
                    results.add(pool.submit(request));
                }
                awaitBlockedOnGrantRow(2);
            });
            for (Future<MockHttpServletResponse> result : results) {
                responses.add(result.get(60, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }
        return responses;
    }

    // 恰一笔 200、另一笔 403 PERMISSION_DENIED
    private void assertOneSucceededAndTheOtherWasDenied(List<MockHttpServletResponse> responses) throws Exception {
        assertThat(responses.stream().map(MockHttpServletResponse::getStatus).toList()).containsExactlyInAnyOrder(200, 403);
        for (MockHttpServletResponse response : responses) {
            if (response.getStatus() == 403) {
                assertThat(detailCode(response.getContentAsString(StandardCharsets.UTF_8))).isEqualTo("PERMISSION_DENIED");
            }
        }
    }

    // M723-F10 的边界（第 656 行「不再重复执行、也不再扣减」；转重放与 SingleUseReplay 第③项同口径：只对经幂等层、第一次已成功完成的请求）：
    // 招商线索创建不经幂等层（LeasingController.createLead 直接调服务层，只能重放的标记无人尊重），本人唯一单次来源、同编号同体两笔并发——
    // 落败的一笔没有可返回的第一次结果，照旧 403 PERMISSION_DENIED；线索只建一条、来源只消耗一次。只按消耗日志转重放时落败笔
    // 标成只能重放后控制器照常执行：线索建两条（消耗一次、业务两次）。
    @Test
    void aConcurrentResubmissionOutsideTheIdempotencyLayerIsDeniedAndRunsTheActionOnce() throws Exception {
        staffWithSingleUseGrant(null, "leasing.lead.manage");
        String body = """
                {"idempotency_key": "xmj-lead-same-%s", "contact_name": "%s", "source": "referral"}
                """.formatted(suffix, roleName("线索"));
        List<MockHttpServletResponse> responses = raceTwoOnTheGrantRow(() -> mockMvc.perform(post("/api/v1/leasing/leads")
                        .with(session(null)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse());
        assertOneSucceededAndTheOtherWasDenied(responses);
        assertConsumedOnce();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM leasing_leads WHERE contact_name = ?", Integer.class,
                roleName("线索"))).isEqualTo(1);
    }

    // 同上，查看请求里接口声明仍然扣的月报导出（GET，幂等键取自 Idempotency-Key 头，不经幂等层；操作员 2026-10-03「导一次扣一次」）：
    // 同头同 URL 两笔并发——恰一笔 200、另一笔 403，来源只消耗一次。只按消耗日志转重放时落败笔也 200、导出两次。
    @Test
    void aConcurrentExportWithTheSameRequestNumberIsDeniedOnceAndUsesUpTheGrantOnce() throws Exception {
        staffWithSingleUseGrant(null, "analytics.report.export");
        List<MockHttpServletResponse> responses = raceTwoOnTheGrantRow(() -> mockMvc.perform(
                        get("/api/v1/analytics/reports/monthly-export").header("Idempotency-Key", "xmj-export-same-" + suffix)
                                .with(session(null)))
                .andReturn().getResponse());
        assertOneSucceededAndTheOtherWasDenied(responses);
        assertConsumedOnce();
    }

    // M723-F09（第 656 行「操作被拒绝或失败的不算用掉」）真实接口复现「请求进入—判定期间授出—核权—拒绝」的顺序：员工起初只有一条已到期、
    // 非单次的临时授权（切面的持有查询不算它，只用作屏障）。主线程锁住这一行后放请求进来：切面查不到未消耗单次来源、不开请求事务；
    // 控制器核权时读取权限前的到期清理 UPDATE 停在这一行上（这时主线程才授出单次来源，与放锁同一提交）。请求继续：读到刚授出的单次来源、
    // 当前没有实际事务 → 403 PERMISSION_DENIED，来源仍未消耗、无消耗日志、角色未建。同一主体再发一次（进入时已持有，切面开请求事务）
    // → 200、恰好消耗一次。修复前第一笔 200：消耗在授权服务自建事务里提交（「403」与「未消耗」断言红）。
    @Test
    void aSingleUseGrantIssuedAfterTheRequestEnteredIsNotConsumedWithoutARequestTransaction() throws Exception {
        Instant now = Instant.now();
        repository.createUser(new AccessUser(userId, userId, "staff", "org", null, "active", now));
        String lapsedGroup = UUID.randomUUID().toString();
        repository.createTemporaryGrant(new TemporaryGrant(lapsedGroup, userId, List.of(ROLE_MANAGE),
                now.minusSeconds(7200), now.minusSeconds(3600), false, false, "active", "M723-F09 已到期的屏障行",
                "seed-admin", now.minusSeconds(7200), null, null));
        String body = roleJson("xmj-role-f09-" + suffix, roleName("进入后才授出"), "[]");
        ExecutorService pool = Executors.newSingleThreadExecutor();
        MockHttpServletResponse first;
        try {
            List<Future<MockHttpServletResponse>> results = new ArrayList<>();
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                jdbcTemplate.queryForList("SELECT id FROM temporary_permission_grants WHERE subject_id = ? FOR UPDATE", userId);
                results.add(pool.submit(() -> mockMvc.perform(post("/api/v1/access/roles").with(session(null))
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                        .andReturn().getResponse()));
                awaitBlockedOnGrantRow(1);
                // 判定期间授出：请求已过切面的持有查询、停在到期清理上
                repository.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), userId, List.of(ROLE_MANAGE),
                        now.minusSeconds(3600), now.plusSeconds(86_400), true, false, "active", "M723-F09 判定期间授出",
                        "seed-admin", now, null, null));
            });
            first = results.get(0).get(60, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        assertThat(first.getStatus()).isEqualTo(403);
        assertThat(detailCode(first.getContentAsString(StandardCharsets.UTF_8))).isEqualTo("PERMISSION_DENIED");
        assertThat(rolesCreated()).isZero();
        // 屏障行已到期、与本项无关，去掉后按整组断言
        jdbcTemplate.update("DELETE FROM temporary_permission_grants WHERE grant_group_id = ?", lapsedGroup);
        assertUnconsumed();

        mockMvc.perform(post("/api/v1/access/roles").with(session(null))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"));
        assertConsumedOnce();
        assertThat(rolesCreated()).isEqualTo(1);
    }

    // 同上的服务层直调对照（补充）：手工放入非只读请求上下文（相当于切面没开事务的写请求）后调用 requirePermission → 403、未消耗；
    // 同一上下文里有实际事务时照常消耗一次。修复前第一处就在授权服务自己的事务里消耗并提交（「未消耗」断言红）。
    @Test
    void aSingleUseGrantFoundDuringAWriteRequestWithoutARequestTransactionIsNotConsumed() {
        staffWithSingleUseGrant(null, ROLE_MANAGE);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                actor(null), null, List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF"))));
        IdempotentReplayContext previous = IdempotentReplayContext.enter("xmj-f09-" + suffix, "digest-" + suffix);
        try {
            assertThatThrownBy(() -> authorizationService.requirePermission(ROLE_MANAGE))
                    .isInstanceOfSatisfying(ApiException.class,
                            exception -> assertThat(exception.code()).isEqualTo("PERMISSION_DENIED"));
            assertUnconsumed();
            // 同根：切面没有给本请求开请求事务（记了「无请求事务」）时，即使核验发生在业务层自己的事务里也不消耗
            IdempotentReplayContext outer = IdempotentReplayContext.enter("xmj-f09-svc-" + suffix, "digest-svc-" + suffix);
            try {
                IdempotentReplayContext.markWithoutRequestTransaction();
                assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                        authorizationService.requirePermission(ROLE_MANAGE)))
                        .isInstanceOfSatisfying(ApiException.class,
                                exception -> assertThat(exception.code()).isEqualTo("PERMISSION_DENIED"));
            } finally {
                IdempotentReplayContext.restore(outer);
            }
            assertUnconsumed();
            // 对照：同一请求有实际事务（请求事务，或进入前已有的外层事务）时照常消耗一次
            new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                    authorizationService.requirePermission(ROLE_MANAGE));
            assertConsumedOnce();
        } finally {
            IdempotentReplayContext.restore(previous);
        }
    }

    // M723-F09 同根（真实接口）：权限只在业务层 @Transactional 方法里核验的入口（商户费用冲正：控制器不核权，
    // MerchantExpenseService.reverse 事务内核 finance.merchant_expense.manage）。顺序同上一条真实接口用例：请求进入时不持有单次来源、
    // 切面不开请求事务并记下「无请求事务」；业务层事务里核权时读取权限前的到期清理停在屏障行上，这时才授出单次来源。请求继续：
    // 读到刚授出的来源、当前有业务层事务，但本请求没有请求事务 → 403 PERMISSION_DENIED，来源未消耗、无消耗日志。
    // 同一主体再发一次（进入时已持有，切面开请求事务）→ 核权通过、走到业务（费用记录不存在 404），消耗随请求事务回滚、仍未消耗。
    // 修复前第一笔不是 403：消耗并入业务层事务，请求走到业务（404）——那个事务若正常提交，消耗不受切面的重放回滚与失败撤回约束。
    @Test
    void aSingleUseGrantIssuedAfterTheRequestEnteredIsNotConsumedInsideAServiceTransactionEither() throws Exception {
        String manage = "finance.merchant_expense.manage";
        Instant now = Instant.now();
        repository.createUser(new AccessUser(userId, userId, "staff", "org", null, "active", now));
        String lapsedGroup = UUID.randomUUID().toString();
        repository.createTemporaryGrant(new TemporaryGrant(lapsedGroup, userId, List.of(manage),
                now.minusSeconds(7200), now.minusSeconds(3600), false, false, "active", "M723-F09 已到期的屏障行",
                "seed-admin", now.minusSeconds(7200), null, null));
        String url = "/api/v1/finance/merchant-expenses/mexp_absent_" + suffix + "/reverse";
        String body = """
                {"idempotency_key": "xmj-f09-svc-%s", "note": "M723-F09"}
                """.formatted(suffix);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        MockHttpServletResponse first;
        try {
            List<Future<MockHttpServletResponse>> results = new ArrayList<>();
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                jdbcTemplate.queryForList("SELECT id FROM temporary_permission_grants WHERE subject_id = ? FOR UPDATE", userId);
                results.add(pool.submit(() -> mockMvc.perform(post(url).with(session(null))
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                        .andReturn().getResponse()));
                awaitBlockedOnGrantRow(1);
                // 判定期间授出：请求已过切面的持有查询、在业务层事务里停在到期清理上
                repository.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), userId, List.of(manage),
                        now.minusSeconds(3600), now.plusSeconds(86_400), true, false, "active", "M723-F09 判定期间授出",
                        "seed-admin", now, null, null));
            });
            first = results.get(0).get(60, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        assertThat(first.getStatus()).isEqualTo(403);
        assertThat(detailCode(first.getContentAsString(StandardCharsets.UTF_8))).isEqualTo("PERMISSION_DENIED");
        // 屏障行已到期、与本项无关，去掉后按整组断言
        jdbcTemplate.update("DELETE FROM temporary_permission_grants WHERE grant_group_id = ?", lapsedGroup);
        assertUnconsumed();

        mockMvc.perform(post(url).with(session(null)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("EXPENSE_ENTRY_NOT_FOUND"));
        assertUnconsumed();
    }

    // M723-F03「一个请求核验多个权限码时失败不留部分消耗」（多来源）：员工持两份不同来源的单次授权——「管理商户档案」与「明文手机号查看」，
    // 商户存档接口入口把两码一起核验（两份来源都在请求事务里消耗）。缺请求编号 400、商户不存在（业务失败）404：两份都未消耗、没有消耗日志。
    // 修复前权限核验返回即把两份都消耗并提交。
    @Test
    void aFailedRequestThatCheckedTwoSingleUseSourcesLeavesBothUnconsumed() throws Exception {
        String manage = "merchant.profile.manage";
        String phone = "pii.contact_phone.view";
        staffWithSingleUseGrant(null, manage);
        Instant now = Instant.now();
        repository.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), userId, List.of(phone),
                now.minusSeconds(3600), now.plusSeconds(86_400), true, false, "active", "M723-F03 第二份来源",
                "seed-admin", now, null, null));
        String url = "/api/v1/platform/merchants/mch_xmj_missing_" + suffix + "/archive";

        mockMvc.perform(post(url).with(session(null))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\": \"M723 多来源回归\"}"))
                .andExpect(status().isBadRequest());
        assertThat(consumedByCode()).containsExactlyInAnyOrder(manage + "=false", phone + "=false");
        assertThat(consumptionLogs()).isZero();

        mockMvc.perform(post(url).with(session(null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\": \"xmj-archive-missing-" + suffix + "\", \"reason\": \"M723 多来源回归\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("MERCHANT_NOT_FOUND"));
        assertThat(consumedByCode()).containsExactlyInAnyOrder(manage + "=false", phone + "=false");
        assertThat(consumptionLogs()).isZero();
    }

    // 非 access 包的写接口（风险预警上报，evidence 包）：同样由请求事务切面兜住；成功后同键重放返回第一次的预警
    @Test
    void riskAlertReportsConsumeTheSingleUseGrantOnlyOnSuccessAndTheReplayReturnsTheFirstAlert() throws Exception {
        staffWithSingleUseGrant(merchantId, RISK_REPORT);

        mockMvc.perform(post("/api/v1/evidence/risk-alerts").with(session(merchantId))
                        .contentType(MediaType.APPLICATION_JSON).content(alertJson(null, "platform")))
                .andExpect(status().isBadRequest());
        assertUnconsumed();

        String failedKey = "xmj-alert-bad-" + suffix;
        mockMvc.perform(post("/api/v1/evidence/risk-alerts").with(session(merchantId))
                        .contentType(MediaType.APPLICATION_JSON).content(alertJson(failedKey, "xm_invalid")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertUnconsumed();
        assertThat(failureEvents("exception.report_failed", failedKey)).isEqualTo(1);

        String okKey = "xmj-alert-ok-" + suffix;
        String first = mockMvc.perform(post("/api/v1/evidence/risk-alerts").with(session(merchantId))
                        .contentType(MediaType.APPLICATION_JSON).content(alertJson(okKey, "platform")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertConsumedOnce();
        assertThat(successEvents("exception.reported", okKey)).isEqualTo(1);

        // 同键重放：返回第一次的预警并提示已完成（M723-F04），不再消耗、不再上报（成功事件仍只 1 条）
        mockMvc.perform(post("/api/v1/evidence/risk-alerts").with(session(merchantId))
                        .contentType(MediaType.APPLICATION_JSON).content(alertJson(okKey, "platform")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(ApiResponse.REPLAYED_MESSAGE))
                .andExpect(result -> assertThat(data(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                        .isEqualTo(data(first)));
        assertConsumedOnce();
        assertThat(successEvents("exception.reported", okKey)).isEqualTo(1);

        // 换幂等键：照旧 403
        mockMvc.perform(post("/api/v1/evidence/risk-alerts").with(session(merchantId))
                        .contentType(MediaType.APPLICATION_JSON).content(alertJson("xmj-alert-new-" + suffix, "platform")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        assertConsumedOnce();
    }

    // 准入闸命中 block：复核单与 admission.blocked 在独立事务里提交，409 回滚请求事务时单次授权未消耗、复核单仍在且待审
    @Test
    void anAdmissionBlockKeepsItsReviewTicketWhileTheSingleUseGrantStaysUnconsumed() throws Exception {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM supervisor_review_policies
                 WHERE policy_code = 'bar_bill_waiver_block' AND enabled AND outcome = 'block' AND trigger_mode = 'always'
                """, Integer.class)).as("V068 种子准入策略：酒吧免单恒拦截").isEqualTo(1);
        staffWithSingleUseGrant(null, BAR_BILL_ADJUST);
        String billId = "bill_xmj_" + suffix;

        String body = mockMvc.perform(post("/api/v1/bar/bills/" + billId + "/adjustments").with(session(null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\": \"xmj-waiver-" + suffix + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ADMISSION_BLOCKED"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        List<Map<String, Object>> reviews = jdbcTemplate.queryForList("""
                SELECT review_id, review_status FROM supervisor_review_decisions
                 WHERE action_type = 'bar.bill_waiver' AND object_id = ? AND initiated_by = ?
                """, billId, userId);
        assertThat(reviews).hasSize(1);
        String reviewId = (String) reviews.get(0).get("review_id");
        assertThat(reviews.get(0).get("review_status")).isEqualTo("pending_review");
        assertThat(body).contains(reviewId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events WHERE event_name = 'admission.blocked' AND business_id = ?",
                Integer.class, reviewId)).isEqualTo(1);
        assertUnconsumed();
    }

    // 终端能力停用：terminal.access_rejected 在独立事务里落库，403 回滚请求事务时单次授权未消耗、拒绝留痕仍在
    @Test
    void aTerminalSwitchRejectionKeepsItsTrailWhileTheSingleUseGrantStaysUnconsumed() throws Exception {
        staffWithSingleUseGrant(merchantId, BAR_BILL_ADJUST);
        // 只对本用例的商户关停该能力（merchant 层取自登录令牌），不影响别的用例
        jdbcTemplate.update("""
                INSERT INTO terminal_capability_switches (
                    id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at
                )
                VALUES (?, '*', ?, 'merchant', ?, FALSE, 'XM-M002-J 单次授权回归', now(), now())
                """, "tcs_xmj_" + suffix, BAR_BILL_ADJUST, merchantId);

        mockMvc.perform(post("/api/v1/bar/bills/bill_xmj_" + suffix + "/adjustments").with(session(merchantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\": \"xmj-switch-" + suffix + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));

        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM audit_events
                 WHERE event_name = 'terminal.access_rejected' AND actor_id = ? AND business_id = ?
                """, Integer.class, userId, BAR_BILL_ADJUST)).isEqualTo(1);
        assertUnconsumed();
    }

    // 核权限不过、被调用方捕获后改走别的权限（如商户费用通知接收人接口）时，外层事务不得被标成只能回滚
    @Test
    void aDeniedCheckCaughtByTheCallerLeavesTheSurroundingTransactionCommittable() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                actor(null), null, List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF"))));
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                assertThatThrownBy(() -> authorizationService.requirePermission(ROLE_MANAGE))
                        .isInstanceOfSatisfying(ApiException.class,
                                exception -> assertThat(exception.code()).isEqualTo("PERMISSION_DENIED")));
    }

    // ── M723：查看不算一次使用（AT）；同一请求分层核验同一单次来源只算一次（账本 XM-M001-W、XM-M002-AU、M002 第 75 行） ──

    // AT（操作员 2026-10-02「只有真正执行的写操作（分配、审批等）才扣单次授权，打开页面、看列表不扣」）：单次组 {查看授权申请, 管理角色}。
    // 看两次授权申请列表都 200、来源未消耗；建角色 200、整组消耗一次（每码一条消耗日志）；之后再看列表 403。
    // 修复前第一次 GET 就把整组扣掉：第二次 GET 与建角色都 403。
    @Test
    void readingDoesNotConsumeASingleUseGrant() throws Exception {
        staffWithSingleUseGrant(null, GRANT_REQUEST_VIEW, ROLE_MANAGE);
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get("/api/v1/access/grant-requests").with(session(null))).andExpect(status().isOk());
            assertUnconsumed();
        }
        mockMvc.perform(post("/api/v1/access/roles").with(session(null))
                        .contentType(MediaType.APPLICATION_JSON).content(roleJson("xmj-at-role-" + suffix, roleName("看后再建"), "[]")))
                .andExpect(status().isOk());
        assertConsumed(2);
        assertThat(rolesCreated()).isEqualTo(1);
        mockMvc.perform(get("/api/v1/access/grant-requests").with(session(null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
    }

    // AT（M723-F05 点名的查看请求）：单次组 {分配角色, 查看角色}。岗位模板列表、角色列表、GET /effective-permissions 各看两次都 200、
    // 来源未消耗；随后的真实执行——给另一名员工分配一个不含权限码的角色 200，整组恰好消耗一次。
    // 修复前第一次看岗位模板列表就把整组扣掉，随后的查看与分配都 403。
    @Test
    void viewingAccessListsKeepsTheSingleUseGroupForTheRoleAssignmentThatFollows() throws Exception {
        staffWithSingleUseGrant(null, "access.role.assign", "access.role.view");
        repository.createUser(new AccessUser(otherUserId, otherUserId, "staff", "org", null, "active", Instant.now()));
        String blankRole = "role_xmj_blank_" + suffix;
        repository.createRole(new RoleRecord(blankRole, roleName("空"), "M723", "project", "active", Instant.now(), List.of(), "org"));
        for (int i = 0; i < 2; i++) {
            for (String url : List.of("/api/v1/access/position-role-templates", "/api/v1/access/roles",
                    "/api/v1/access/effective-permissions")) {
                mockMvc.perform(get(url).with(session(null))).andExpect(status().isOk());
                assertUnconsumed();
            }
        }

        mockMvc.perform(post("/api/v1/access/role-assignments").with(session(null))
                        .contentType(MediaType.APPLICATION_JSON).content(assignJson("xmj-view-assign-" + suffix, blankRole)))
                .andExpect(status().isOk());
        assertConsumed(2);
        assertThat(jdbcTemplate.queryForList("SELECT role_id FROM user_roles WHERE user_id = ?", String.class, otherUserId))
                .containsExactly(blankRole);
    }

    // AT 的边界（M009-058「单次许可只供一次响应」；操作员 2026-10-03：看敏感明文「算用掉，看一次扣一次」，普通的商户列表（脱敏）仍然不扣）：员工持两条单次来源——
    // 「查看商户」与「明文手机号查看」。看一次商户列表 200：「查看商户」未消耗（看列表不扣），「明文手机号查看」已消耗（看一次扣一次）；
    // 再看一次仍 200（列表照常能看）。修复前第一次 GET 把两条都扣掉：「查看商户」未消耗的断言即红。
    // 去掉 PlatformController.listMerchants 上的 ConsumesSingleUseGrant 标记则「明文手机号查看已消耗」的断言红。
    @Test
    void viewingTheMerchantListKeepsTheViewGrantButStillUsesUpThePlaintextPhoneGrant() throws Exception {
        String view = "merchant.profile.view";
        String phone = "pii.contact_phone.view";
        staffWithSingleUseGrant(null, view);
        Instant now = Instant.now();
        repository.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), userId, List.of(phone),
                now.minusSeconds(3600), now.plusSeconds(86_400), true, false, "active", "M723 明文手机号单次授权",
                "seed-admin", now, null, null));

        mockMvc.perform(get("/api/v1/platform/merchants").with(session(null))).andExpect(status().isOk());
        assertThat(consumedByCode()).containsExactlyInAnyOrder(view + "=false", phone + "=true");
        assertThat(consumptionLogs()).isEqualTo(1);
        mockMvc.perform(get("/api/v1/platform/merchants").with(session(null))).andExpect(status().isOk());
        assertThat(consumedByCode()).containsExactlyInAnyOrder(view + "=false", phone + "=true");
        assertThat(consumptionLogs()).isEqualTo(1);
    }

    // AT 的边界（治理地图「导出须单独授权、逐次留痕」；操作员 2026-10-03：导出「算用掉，导一次扣一次」）：单次导出授权导出一次 200
    // 即消耗，再导出 403。守护用例：修复前同样绿；去掉对应控制器处理方法上的 ConsumesSingleUseGrant 标记则红。
    // 操作日志导出、应收应付导出按商户范围取数，主体带商户归属（不带商户的普通员工在 currentMerchantScope() 处 403）；
    // 月报导出是平台口径，主体不带商户。
    @Test
    void anExportStillUsesUpItsSingleUseGrant() throws Exception {
        assertExportUsesUpTheGrant(merchantId, "audit.operation_log.export", "/api/v1/audit/operation-events/export?limit=1");
    }

    @Test
    void aFinanceLedgerExportStillUsesUpItsSingleUseGrant() throws Exception {
        assertExportUsesUpTheGrant(merchantId, "finance.ar_ap.export", "/api/v1/finance/ar-ap-items/export");
    }

    @Test
    void aMonthlyReportExportStillUsesUpItsSingleUseGrant() throws Exception {
        assertExportUsesUpTheGrant(null, "analytics.report.export", "/api/v1/analytics/reports/monthly-export");
    }

    private void assertExportUsesUpTheGrant(String merchant, String permissionCode, String url) throws Exception {
        staffWithSingleUseGrant(merchant, permissionCode);
        mockMvc.perform(get(url).with(session(merchant))).andExpect(status().isOk());
        assertConsumedOnce();
        mockMvc.perform(get(url).with(session(merchant)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        assertConsumedOnce();
    }

    // AT 的边界（操作员 2026-10-03：看敏感明文「算用掉，看一次扣一次」）：企微会话原文调阅（mode=raw，专码 wecom.view_raw）。
    // 单次组 {企微会话查看, 企微原文调阅}：脱敏调阅 200、未消耗（查看不扣）；原文调阅 200、整组消耗一次；再调阅原文 403。
    // 修复前脱敏调阅就把整组扣掉（第一处「未消耗」即红）。去掉 ComplianceController.viewWecomMessages 上的 ConsumesSingleUseGrant 标记，
    // 则原文调阅后「已消耗」的断言红。
    @Test
    void viewingRawWecomMessagesUsesUpTheSingleUseGrantWhileTheRedactedViewDoesNot() throws Exception {
        String conversation = "wconv_xmj_" + suffix;
        jdbcTemplate.update("INSERT INTO wecom_conversations (id, external_conversation_id, project_id) VALUES (?, ?, 'org')",
                conversation, "ext-" + conversation);
        String url = "/api/v1/compliance/wecom/conversations/" + conversation + "/messages";
        staffWithSingleUseGrant(null, "wecom.view", "wecom.view_raw");

        mockMvc.perform(get(url).with(session(null))).andExpect(status().isOk());
        assertUnconsumed();
        mockMvc.perform(get(url).param("mode", "raw").param("approval_ref", "approval-m723").with(session(null)))
                .andExpect(status().isOk());
        assertConsumed(2);
        mockMvc.perform(get(url).param("mode", "raw").param("approval_ref", "approval-m723").with(session(null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        assertConsumed(2);
    }

    // M723-F07（治理地图第 660 行「查看企业微信会话完整原文……必须逐项单独授予并留痕」；操作员 2026-10-03「看一次扣一次」）：
    // mode 取 raw 的大小写、首尾空白变体都按原文核权，与服务层同一规范化。只持单次「企微会话查看」的乙："raw"、" raw "、"RAW" 都 403，
    // 响应不含原文引用；" redacted "、"REDACTED" 200、只核 wecom.view、不消耗；"xx" 400。只持单次「企微原文调阅」的甲：" raw " 首次
    // 200 含原文、恰好消耗一次、留原文查看留痕；"RAW" 再次 403、不再返回原文。修复前控制器只认精确 "raw"：" raw " 对乙核的是
    // wecom.view（200 且服务层返回原文——第一处 403 即红），对甲核 wecom.view 则 403。
    @Test
    void theRawWecomModeIsRecognisedAfterNormalisationForBothTheCheckAndTheResponse() throws Exception {
        String conversation = "wconv_xmj_" + suffix;
        String rawRef = "raw-ref-" + suffix;
        jdbcTemplate.update("INSERT INTO wecom_conversations (id, external_conversation_id, project_id) VALUES (?, ?, 'org')",
                conversation, "ext-" + conversation);
        jdbcTemplate.update("""
                INSERT INTO wecom_messages (id, conversation_id, sender_ref, content_raw_ref, content_redacted)
                VALUES (?, ?, 'cust', ?, '***')
                """, "wmsg_xmj_" + suffix, conversation, rawRef);
        String url = "/api/v1/compliance/wecom/conversations/" + conversation + "/messages";
        staffWithSingleUseGrant(null, "wecom.view_raw");
        singleUseGrant(otherUserId, null, "wecom.view");

        for (String mode : List.of("raw", " raw ", "RAW")) {
            String denied = mockMvc.perform(get(url).param("mode", mode).param("approval_ref", "approval-m723")
                            .with(session(otherUserId, null)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"))
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(denied).doesNotContain(rawRef).doesNotContain("raw_content");
        }
        for (String mode : List.of(" redacted ", "REDACTED")) {
            String redacted = mockMvc.perform(get(url).param("mode", mode).with(session(otherUserId, null)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items[0].content").value("***"))
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(redacted).doesNotContain(rawRef);
        }
        mockMvc.perform(get(url).param("mode", "xx").with(session(otherUserId, null)))
                .andExpect(status().isBadRequest());
        assertThat(jdbcTemplate.queryForList("SELECT consumed FROM temporary_permission_grants WHERE subject_id = ?",
                Boolean.class, otherUserId)).containsExactly(false);
        assertThat(consumptionLogs(otherUserId)).isZero();

        mockMvc.perform(get(url).param("mode", " raw ").param("approval_ref", "approval-m723").with(session(null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].raw_content").value(rawRef));
        assertConsumedOnce();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM wecom_access_audits
                 WHERE conversation_id = ? AND viewer_user_id = ? AND view_type = 'raw'
                """, Integer.class, conversation, userId)).isEqualTo(1);
        String deniedAgain = mockMvc.perform(get(url).param("mode", "RAW").param("approval_ref", "approval-m723").with(session(null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(deniedAgain).doesNotContain(rawRef);
        assertConsumedOnce();
    }

    // 操作员 2026-10-03「预览、查询不扣一次性授权，只有真正核销、真正执行的那一次才扣」：按手机号查客人账号是以提交方式做的查询
    // （POST /lookup，手机号放请求体）。只持单次「客户账户管理」授权的员工查两次都 200，来源未消耗、无消耗日志。
    // 修复前第一次查询就把授权扣掉。去掉 CustomerAccountAdminController.lookup 上的 DoesNotConsumeSingleUseGrant 标记则红。
    @Test
    void aQuerySubmittedByPostDoesNotConsumeASingleUseGrant() throws Exception {
        staffWithSingleUseGrant(null, "customer.account.manage");
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/customer-accounts/lookup").with(session(null))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"phone\": \"19900000000\"}"))
                    .andExpect(status().isOk());
            assertUnconsumed();
        }
    }

    // 同一条答复：数据清理「预览」只统计与取样（落一条试运行台账），与「提交清理申请」同码。只持单次「申请数据清理」授权的员工
    // 先预览 200、来源未消耗，再提交申请 200、恰好消耗一次；之后再预览 403。修复前预览就把授权扣掉，提交申请 403。
    // 去掉 DataCleanupController.preview 上的 DoesNotConsumeSingleUseGrant 标记则红。
    @Test
    void aCleanupPreviewKeepsTheSingleUseGrantForTheSubmissionThatFollows() throws Exception {
        staffWithSingleUseGrant(null, "data.cleanup.request");
        String previewed = mockMvc.perform(post("/api/v1/ops/data-cleanup/preview").with(session(null))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"scope_type\": \"archived_bar_combos\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("dry_run"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertUnconsumed();
        String jobId = data(previewed).get("id").asText();

        mockMvc.perform(post("/api/v1/ops/data-cleanup/jobs/" + jobId + "/submit").with(session(null))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\": \"M723 单次授权回归\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_approval"));
        assertConsumedOnce();
        mockMvc.perform(post("/api/v1/ops/data-cleanup/preview").with(session(null))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"scope_type\": \"archived_bar_combos\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        assertConsumedOnce();
    }

    private List<String> consumedByCode() {
        return jdbcTemplate.queryForList(
                "SELECT permission_code || '=' || consumed::text FROM temporary_permission_grants WHERE subject_id = ?",
                String.class, userId);
    }

    // XM-M001-W：一个写请求（请求上下文＋请求事务）里分层核验同一单次来源——同码两次、同组另一码、布尔判定——全部通过，
    // 来源只消耗一次（每码一条消耗日志）；事务回滚则未消耗。修复前第二次核验读到已消耗即 403。
    @Test
    void layeredChecksInOneRequestConsumeASingleUseSourceOnce() {
        staffWithSingleUseGrant(null, ROLE_MANAGE, GRANT_REQUEST_VIEW);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                actor(null), null, List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF"))));
        for (boolean commit : List.of(false, true)) {
            IdempotentReplayContext previous = IdempotentReplayContext.enter("xmj-layered-" + commit + "-" + suffix, "digest-" + suffix);
            try {
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    authorizationService.requirePermission(ROLE_MANAGE);
                    authorizationService.requirePermission(ROLE_MANAGE);
                    authorizationService.requirePermission(GRANT_REQUEST_VIEW);
                    assertThat(authorizationService.currentActorHasPermission(GRANT_REQUEST_VIEW)).isTrue();
                    assertThat(consumptionLogs()).isEqualTo(2);
                    if (!commit) {
                        status.setRollbackOnly();
                    }
                });
            } finally {
                IdempotentReplayContext.restore(previous);
            }
            if (commit) {
                assertConsumed(2);
            } else {
                assertUnconsumed();
            }
        }
        // 请求结束后（新的请求上下文）已消耗的来源不再可用
        IdempotentReplayContext previous = IdempotentReplayContext.enter("xmj-layered-next-" + suffix, "digest-" + suffix);
        try {
            assertThat(authorizationService.currentActorHasPermission(ROLE_MANAGE)).isFalse();
        } finally {
            IdempotentReplayContext.restore(previous);
        }
    }

    // XM-M002-AU：酒吧商品录入先核「管理商品」、再用布尔判定「内部结算价查看」，两码同在一个单次组。绑定目录项的录入不再被
    // 自己刚消耗的来源拦成 403 BAR_INTERNAL_PRICE_FORBIDDEN，而是进到服务层（这里用不存在的酒吧与目录项，由服务层拒绝）；
    // 动作失败，单次组未消耗、无消耗日志。修复前入口核验即把整组扣掉并提交，随后的布尔判定为否。
    @Test
    void barProductEntryPassesTheInternalPriceCheckWithOneSingleUseGroup() throws Exception {
        staffWithSingleUseGrant(merchantId, "bar.product.manage", "bar.internal_settlement_price.view");
        var response = mockMvc.perform(post("/api/v1/bar/products").with(session(merchantId))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "xmj-bar-product-%s", "bar_id": "bar_xmj_%s", "product_name": "单次授权回归",
                                 "category": "beer", "price_cent": 100, "catalog_item_code": "ipc_xmj_%s"}
                                """.formatted(suffix, suffix, suffix)))
                .andReturn().getResponse();
        assertThat(response.getStatus()).isBetween(400, 499);
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .doesNotContain("BAR_INTERNAL_PRICE_FORBIDDEN").doesNotContain("PERMISSION_DENIED");
        assertUnconsumed();
    }

    // M002 账本第 75 行：员工只经一个单次组持有 {分配角色, 管理角色, 查看角色}。把「查看角色」经员工单独授予、配角色、建角色授出，
    // 各 403 GRANT_EXCEEDS_OWN_PERMISSIONS（只单次持有的权限不能授出），单次组未消耗、无消耗日志；随后分配一个不含权限码的角色 200，
    // 整组恰好消耗一次。修复前返回码相同，但入口核验已把整组扣掉并提交：「未消耗」断言即红，随后的分配 403。
    @Test
    void grantingACodeHeldOnlyThroughTheSameSingleUseGroupIsRejectedWithoutConsumingIt() throws Exception {
        String held = "access.role.view";
        staffWithSingleUseGrant(null, "access.role.assign", ROLE_MANAGE, held);
        repository.createUser(new AccessUser(otherUserId, otherUserId, "staff", "org", null, "active", Instant.now()));
        String heldRole = "role_xmj_held_" + suffix;
        String blankRole = "role_xmj_blank_" + suffix;
        repository.createRole(new RoleRecord(heldRole, roleName("含码"), "M723", "project", "active", Instant.now(), List.of(held), "org"));
        repository.createRole(new RoleRecord(blankRole, roleName("空"), "M723", "project", "active", Instant.now(), List.of(), "org"));

        mockMvc.perform(post("/api/v1/access/users/" + otherUserId + "/permission-overrides").with(session(null))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "xmj-75-override-%s", "permission_codes": ["%s"], "effect": "grant", "reason": "M723"}
                                """.formatted(suffix, held)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("GRANT_EXCEEDS_OWN_PERMISSIONS"));
        assertUnconsumed();
        mockMvc.perform(post("/api/v1/access/role-assignments").with(session(null))
                        .contentType(MediaType.APPLICATION_JSON).content(assignJson("xmj-75-assign-" + suffix, heldRole)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("GRANT_EXCEEDS_OWN_PERMISSIONS"));
        assertUnconsumed();
        mockMvc.perform(post("/api/v1/access/roles").with(session(null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roleJson("xmj-75-role-" + suffix, roleName("越权"), "[\"" + held + "\"]")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("GRANT_EXCEEDS_OWN_PERMISSIONS"));
        assertUnconsumed();
        assertThat(rolesCreated()).isEqualTo(2);

        mockMvc.perform(post("/api/v1/access/role-assignments").with(session(null))
                        .contentType(MediaType.APPLICATION_JSON).content(assignJson("xmj-75-ok-" + suffix, blankRole)))
                .andExpect(status().isOk());
        assertConsumed(3);
        assertThat(jdbcTemplate.queryForList("SELECT role_id FROM user_roles WHERE user_id = ?", String.class, otherUserId))
                .containsExactly(blankRole);
    }

    private String assignJson(String key, String roleId) {
        return """
                {"idempotency_key": "%s", "user_id": "%s", "role_id": "%s"}
                """.formatted(key, otherUserId, roleId);
    }

    private void staffWithSingleUseGrant(String merchant, String... permissionCodes) {
        singleUseGrant(userId, merchant, permissionCodes);
        assertUnconsumed();
    }

    // 多个码时是同一个单次授权组（同一来源）：消耗任一码即整组消耗，每码一条消耗日志。
    private void singleUseGrant(String user, String merchant, String... permissionCodes) {
        Instant now = Instant.now();
        repository.createUser(new AccessUser(user, user, "staff", "org", merchant, "active", now));
        repository.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), user, List.of(permissionCodes),
                now.minusSeconds(3600), now.plusSeconds(86_400), true, false, "active", "XM-M002-J 单次授权回归",
                "seed-admin", now, null, null));
    }

    private AccessActor actor(String merchant) {
        return actor(userId, merchant);
    }

    private AccessActor actor(String user, String merchant) {
        return new AccessActor(user, user, "staff", "org", merchant, "active");
    }

    // 后台 Web 终端（grace 模式下自报 web 的写请求留痕放行）+ 生产过滤器放入的 AccessActor 主体。
    private RequestPostProcessor session(String merchant) {
        return session(userId, merchant);
    }

    private RequestPostProcessor session(String user, String merchant) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor(user, merchant), null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private void assertUnconsumed() {
        assertThat(jdbcTemplate.queryForList("SELECT consumed FROM temporary_permission_grants WHERE subject_id = ?",
                Boolean.class, userId)).containsOnly(false);
        assertThat(consumptionLogs()).isZero();
    }

    private void assertConsumedOnce() {
        assertConsumed(1);
    }

    // 整组消耗一次：组内每个码的行都已消耗，每码一条消耗日志。
    private void assertConsumed(int codes) {
        assertThat(jdbcTemplate.queryForList("SELECT consumed FROM temporary_permission_grants WHERE subject_id = ?",
                Boolean.class, userId)).hasSize(codes).containsOnly(true);
        assertThat(consumptionLogs()).isEqualTo(codes);
    }

    private Integer consumptionLogs() {
        return consumptionLogs(userId);
    }

    private Integer consumptionLogs(String user) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM permission_template_usage_logs WHERE user_id = ? AND result = 'consumed'",
                Integer.class, user);
    }

    // 响应包络的 request_id / server_time 每次不同，比较第一次与重放的业务结果只看 data
    private JsonNode data(String body) throws Exception {
        return new ObjectMapper().readTree(body).get("data");
    }

    private Integer successEvents(String eventName, String idempotencyKey) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events WHERE event_name = ? AND event_result = 'success' AND idempotency_key = ?",
                Integer.class, eventName, idempotencyKey);
    }

    private Integer failureEvents(String eventName, String idempotencyKey) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events WHERE event_name = ? AND event_result = 'failure' AND idempotency_key = ?",
                Integer.class, eventName, idempotencyKey);
    }

    private Integer rolesCreated() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM roles WHERE role_name LIKE ?", Integer.class, roleName("%"));
    }

    private String roleName(String tail) {
        return "单次授权回归-" + suffix + "-" + tail;
    }

    private String roleJson(String key, String name, String codes) {
        return """
                {%s"role_name": "%s", "permission_codes": %s}
                """.formatted(key == null ? "" : "\"idempotency_key\": \"" + key + "\", ", name, codes);
    }

    private String alertJson(String key, String sourceDomain) {
        return """
                {%s"source_domain": "%s", "alert_type": "xm_m002_j", "risk_level": "normal", "summary": "单次授权回归"}
                """.formatted(key == null ? "" : "\"idempotency_key\": \"" + key + "\", ", sourceDomain);
    }
}
