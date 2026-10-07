package com.complex.platform.gate;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.TemporaryGrant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * XM-ONESHOT-RETURN B01（治理地图第 303 行「全局原则 · 同时操作以先成功为准」：同一个操作被重复提交，系统只认一次；
 * 操作员 2026-09-25 确认的追加句：只能用一次的动作只有这次操作成功完成、返回结果确认后才算用掉，没有返回确认的不扣减；
 * 同一次操作重复提交时，返回第一次的结果）：准入闸逐笔放行的真实库、真实接口回归。
 *
 * <p>接口用票务动态定价发布（{@code POST /api/v1/ticketing/dynamic-pricing-rules/{id}/publish}，
 * {@code @GuardedAction(admissionAction = "ticketing.dynamic_pricing_publish")}）。V099 种子策略
 * {@code ticketing_dyn_pricing_over_scope} 对无生效合同的商户员工命中拦截；本类的商户按随机后缀新建、没有合同，恒命中。
 * 放行单用真实拦截生成的待审复核单，再把它置为已批准。
 *
 * <p>M724-F16（操作员 2026-10-07 裁决「批准只能被『被拦下的那次提交的同样内容』用掉，内容不同的另一笔照常拦截送审」）：
 * 被拦下的那次提交与之后用掉这张批准的那次提交内容（这里是发布原因）须相同，{@code blocked}/{@code blockedThenApproved} 带上被拦那次的原因。
 *
 * <p>本类不开测试事务：放行消费、回滚、并发都要真实提交才看得见。夹具按随机后缀隔离、结束时删除；
 * 复核单与审计事件只追加不删（V148 / V140），按后缀隔离留在库里。写请求带 {@code X-Terminal-Type: web}。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class AdmissionReleaseOneShotJdbcIntegrationTests {

    private static final String PUBLISH = "ticket.dynamic_pricing.publish";
    private static final String ACTION = "ticketing.dynamic_pricing_publish";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AccessRepository accessRepository;

    @Autowired
    private DataSource dataSource;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String userId = "usr_b01_" + suffix;
    private final String otherUserId = "usr_b01o_" + suffix;
    private final String merchantId = "mch_b01_" + suffix;
    // M724-F03：另一个商户的员工（同项目、不同商户——另一个提交范围），用到的用例里才建
    private final String outsiderId = "usr_b01x_" + suffix;
    private final String outsiderMerchantId = "mch_b01x_" + suffix;
    // M724-F03 ⑥后半：同商户、另一个项目的员工（另一个提交范围），用到的用例里才建
    private final String strangerId = "usr_b01p_" + suffix;
    private final String strangerProjectId = "org_b01p_" + suffix;
    private final String ticketTypeId = "tt_b01_" + suffix;
    private final String ruleId = "tdpr_b01_" + suffix;
    private final String contractId = "ctr_b01_" + suffix;

    @BeforeEach
    void seed() {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM supervisor_review_policies
                 WHERE policy_code = 'ticketing_dyn_pricing_over_scope' AND action_type = ?
                   AND enabled AND outcome = 'block' AND trigger_mode = 'over_contract_scope'
                """, Integer.class, ACTION)).as("V099 种子准入策略：动态定价发布超经营范围拦截").isEqualTo(1);
        staff(userId);
        staff(otherUserId);
        jdbcTemplate.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at)
                VALUES (?, ?, 'weekday_weekend_holiday', FALSE, 'published', now(), now())
                """, ticketTypeId, "放行回归票种-" + suffix);
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("DELETE FROM ticket_dynamic_pricing_rules WHERE id = ?", ruleId);
        jdbcTemplate.update("DELETE FROM ticket_types WHERE id = ?", ticketTypeId);
        jdbcTemplate.update("DELETE FROM contracts WHERE id = ?", contractId);
        jdbcTemplate.update("DELETE FROM temporary_permission_grants WHERE subject_id IN (?, ?, ?, ?)",
                userId, otherUserId, outsiderId, strangerId);
        jdbcTemplate.update("DELETE FROM access_users WHERE id IN (?, ?, ?, ?)", userId, otherUserId, outsiderId, strangerId);
    }

    // 【返回前已判已用／先扣后败】放行后业务失败：放行单随业务回滚，仍未用掉；同一请求再发按首次处理，恰好用掉一次；用掉后换请求编号须重新审批
    @Test
    void aFailedPublishLeavesTheApprovedReleaseUnusedAndTheSameRequestRetriedUsesItExactlyOnce() throws Exception {
        String reviewId = blockedThenApproved("b01-block-" + suffix, "放行回归发布");
        String key = "b01-publish-" + suffix;
        String body = publishJson(key, "放行回归发布");

        // 规则尚不存在：本次请求先用放行单放行，再在业务里 404，整笔回滚——修复前放行单已被单独提交为已放行
        publish(userId, body)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("DYNAMIC_PRICING_RULE_NOT_FOUND"));
        assertReleaseUnused(reviewId);
        assertThat(published(key)).isZero();

        // 第二次还能过：规则就绪后同键同正文再发，按首次处理，恰好用掉这张放行单一次
        draftRule();
        publish(userId, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("published"));
        assertReleasedOnce(reviewId);
        assertThat(published(key)).isEqualTo(1);
        assertThat(pendingReviews(userId)).isZero();

        // 用掉之后换请求编号再做同样内容：须重新审批（新的待审复核单，409），不执行
        String againKey = "b01-publish-again-" + suffix;
        publish(userId, publishJson(againKey, "放行回归发布"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ADMISSION_BLOCKED"));
        assertThat(pendingReviews(userId)).isEqualTo(1);
        assertThat(published(againKey)).isZero();
        assertReleasedOnce(reviewId);
    }

    // M724-F02 末句「长期权限的人与只持单次授权的人各有用例」之①前半：只持单次授权的人——闸口并入请求事务（长期权限的人是闸口自己开
    // 事务），回滚路径不同。放行后业务失败：放行单与单次授权都随请求事务回滚，都不算用掉、没有放行留痕；同一请求再发按首次处理，
    // 两者各用掉恰好一次。
    @Test
    void aFailedPublishByAHolderOfOnlyASingleUseGrantLeavesTheReleaseAndTheGrantUnusedAndTheRetrySpendsEachOnce()
            throws Exception {
        regrant(true);
        String reason = "放行回归单次授权发布";
        // 拦截那次 409 整笔回滚，单次授权仍未用掉
        String reviewId = blockedThenApproved("b01-block-" + suffix, reason);
        assertSingleUseGrantConsumed(false, 0);
        String key = "b01-publish-" + suffix;
        String body = publishJson(key, reason);

        // 规则尚不存在：权限核验用掉单次授权、闸口用掉放行单，业务 404，请求事务整笔回滚
        publish(userId, body)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("DYNAMIC_PRICING_RULE_NOT_FOUND"));
        assertReleaseUnused(reviewId);
        assertSingleUseGrantConsumed(false, 0);
        assertThat(published(key)).isZero();

        // 第二次还能过：规则就绪后同键同正文再发，放行单与单次授权各用掉恰好一次
        draftRule();
        publish(userId, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("published"));
        assertReleasedOnce(reviewId);
        assertSingleUseGrantConsumed(true, 1);
        assertThat(published(key)).isEqualTo(1);
        assertThat(pendingReviews(userId)).isZero();
    }

    // 【重复提交不返回首次结果】放行单用掉后同一请求重放：返回第一次的结果，不建复核单、不再执行、不去用同对象的另一张放行单
    @Test
    void theSameRequestResubmittedAfterItsReleaseWasUsedReturnsTheFirstResultWithoutANewReview() throws Exception {
        draftRule();
        String reviewId = blockedThenApproved("b01-block-" + suffix, "放行回归发布");
        String key = "b01-publish-" + suffix;
        String body = publishJson(key, "放行回归发布");
        String first = body(publish(userId, body).andExpect(status().isOk()));
        assertReleasedOnce(reviewId);

        // 同一操作人、同键、同正文重放：修复前准入闸先于幂等层，另建一张待审复核单并 409；
        // 返回第一次的结果并提示已完成（XM-ONESHOT-RETURN B02）
        String replayed = body(publish(userId, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("该操作已完成（重复提交，返回首次结果）"))
                .andExpect(jsonPath("$.replayed").value(true)));
        assertThat(data(replayed)).isEqualTo(data(first));
        assertThat(pendingReviews(userId)).isZero();
        assertThat(published(key)).isEqualTo(1);
        assertReleasedOnce(reviewId);

        // 同键改正文：幂等层按正文不同拒绝，不建复核单
        publish(userId, publishJson(key, "放行回归改了原因"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("IDEMPOTENCY_CONFLICT"));
        assertThat(pendingReviews(userId)).isZero();

        // M724-F03（操作员裁决「同一项目、同一商户的员工用同一个提交编号，只算一次、返回第一次的结果」）：换操作人——同项目同商户的同事
        // 拿同键同正文：闸口认重放不限定操作人，返回第一次的结果并提示已完成，不建复核单、不再执行。
        // 修复前闸口只认同一操作人的重放：同事被拦下、另建一张待审复核单（409 ADMISSION_BLOCKED）
        String byColleague = body(publish(otherUserId, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("该操作已完成（重复提交，返回首次结果）"))
                .andExpect(jsonPath("$.replayed").value(true)));
        assertThat(data(byColleague)).isEqualTo(data(first));
        assertThat(pendingReviews(otherUserId)).isZero();
        assertThat(published(key)).isEqualTo(1);
        assertReleasedOnce(reviewId);

        // 另一个商户的员工（同样的常规授权）拿同键同正文：闸口同样不拦、不建复核单，幂等层按提交范围拒绝——409「该提交编号已被使用」，
        // 拿不到第一次的结果、不再执行
        staff(outsiderId, outsiderMerchantId);
        publish(outsiderId, outsiderMerchantId, body)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("IDEMPOTENCY_KEY_USED"));
        assertThat(pendingReviews(outsiderId)).isZero();
        assertThat(published(key)).isEqualTo(1);
        assertReleasedOnce(reviewId);

        // M724-F03 ⑥后半「其他项目或其他商户的人」的另一类：同商户、另一个项目的员工（同样的常规授权）拿同键同正文——同样 409
        // 「该提交编号已被使用」，其名下没有待审复核单、不再执行
        staff(strangerId, strangerProjectId, merchantId);
        publish(strangerId, strangerProjectId, merchantId, body)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("IDEMPOTENCY_KEY_USED"));
        assertThat(pendingReviews(strangerId)).isZero();
        assertThat(published(key)).isEqualTo(1);
        assertReleasedOnce(reviewId);

        // 同对象另有一张已批准未用、内容相同的放行单：重放仍返回第一次的结果，不去用这张新单
        String secondReviewId = blockedThenApproved("b01-block-second-" + suffix, "放行回归发布");
        assertThat(data(body(publish(userId, body).andExpect(status().isOk())))).isEqualTo(data(first));
        assertReleaseUnused(secondReviewId);
        // 第二次还能过：新请求编号、同样内容才用它，且只用一次
        String nextKey = "b01-publish-next-" + suffix;
        publish(userId, publishJson(nextKey, "放行回归发布")).andExpect(status().isOk());
        assertReleasedOnce(secondReviewId);
        assertThat(published(nextKey)).isEqualTo(1);

        // 第一次的幂等记录不在：没有可返回的结果，认不出重放，照常拦截进复核、不执行
        jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key = ?", key);
        publish(userId, body)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ADMISSION_BLOCKED"));
        assertThat(published(key)).isEqualTo(1);
    }

    // 同一请求两次并发提交：后一笔在放行单行锁上等前一笔提交，随后认出重放，两笔都返回第一次的结果，业务只执行一次、不建复核单
    @Test
    void twoConcurrentSubmissionsOfTheSameRequestExecuteOnceAndBothReturnTheFirstResult() throws Exception {
        draftRule();
        String reviewId = blockedThenApproved("b01-block-" + suffix, "放行回归并发");
        String key = "b01-race-" + suffix;
        String body = publishJson(key, "放行回归并发");

        List<MvcResult> results = raceOnTheRelease(body, body, () -> { }, "UPDATE supervisor_review_decisions");

        assertThat(results).extracting(result -> result.getResponse().getStatus()).containsExactly(200, 200);
        assertThat(data(results.get(1).getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(data(results.get(0).getResponse().getContentAsString(StandardCharsets.UTF_8)));
        assertThat(published(key)).isEqualTo(1);
        assertReleasedOnce(reviewId);
        assertThat(pendingReviews(userId)).isZero();
    }

    // 同一放行单被两个不同请求（内容相同、只差提交编号）同时使用：最多一笔成功，后一笔等前一笔提交后拿不到，照常拦截进复核
    @Test
    void twoConcurrentDifferentRequestsOnOneReleaseSucceedAtMostOnce() throws Exception {
        draftRule();
        String reviewId = blockedThenApproved("b01-block-" + suffix, "放行回归并发");
        String firstKey = "b01-race-a-" + suffix;
        String secondKey = "b01-race-b-" + suffix;

        List<MvcResult> results = raceOnTheRelease(publishJson(firstKey, "放行回归并发"), publishJson(secondKey, "放行回归并发"),
                () -> { }, "UPDATE supervisor_review_decisions");

        assertThat(results).extracting(result -> result.getResponse().getStatus()).containsExactly(200, 409);
        assertThat(results.get(1).getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("ADMISSION_BLOCKED");
        assertThat(published(firstKey)).isEqualTo(1);
        assertThat(published(secondKey)).isZero();
        assertReleasedOnce(reviewId);
        assertThat(pendingReviews(userId)).isEqualTo(1);
    }

    // 重放认出前控制器已新用掉一张单次授权（第一次凭常规授权）：返回第一次的结果并提示已完成，请求事务整笔回滚，
    // 这张单次授权不算用掉、业务不再执行（M723-F04 的口径）
    @Test
    void aReplayThatSpendsAFreshSingleUseGrantReturnsTheFirstResultAndTheGrantStaysUnused() throws Exception {
        draftRule();
        String reviewId = blockedThenApproved("b01-block-" + suffix, "放行回归发布");
        String key = "b01-publish-" + suffix;
        String body = publishJson(key, "放行回归发布");
        String firstResult = body(publish(userId, body).andExpect(status().isOk()));
        assertReleasedOnce(reviewId);

        // 收回常规授权、改授一张单次授权，再同键同正文重放：准入闸认出重放、权限核验用掉这张单次授权、幂等层返回第一次的结果，
        // 请求事务随即整笔回滚——单次授权没有用掉。修复前准入闸不认重放，这一步是 409 ADMISSION_BLOCKED
        regrant(true);
        assertThat(data(body(publish(userId, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("该操作已完成（重复提交，返回首次结果）"))
                .andExpect(jsonPath("$.replayed").value(true))))).isEqualTo(data(firstResult));
        assertSingleUseGrantConsumed(false, 0);
        assertThat(published(key)).isEqualTo(1);
        assertReleasedOnce(reviewId);
        assertThat(pendingReviews(userId)).isZero();

        // 第二次还能过：再批一张放行单（拦截那次 409 整笔回滚，单次授权仍未用掉），换新请求编号凭这张单次授权，恰好用掉一次
        String secondReviewId = blockedThenApproved("b01-block-second-" + suffix, "放行回归再发布");
        assertSingleUseGrantConsumed(false, 0);
        String nextKey = "b01-publish-next-" + suffix;
        String nextBody = publishJson(nextKey, "放行回归再发布");
        String first = body(publish(userId, nextBody).andExpect(status().isOk()));
        assertSingleUseGrantConsumed(true, 1);
        assertReleasedOnce(secondReviewId);
        assertThat(published(nextKey)).isEqualTo(1);

        // 这次请求（单次授权与放行单都用在它身上）同键同正文重放：返回第一次的结果，都不再用掉
        assertThat(data(body(publish(userId, nextBody).andExpect(status().isOk())))).isEqualTo(data(first));
        assertSingleUseGrantConsumed(true, 1);
        assertReleasedOnce(secondReviewId);
        assertThat(published(nextKey)).isEqualTo(1);
    }

    // 审查修正（C3 求值异常统一 fail-closed、建待审复核）：合同经营范围不是数组、求值 SQL 报错时仍返回带复核单号的 409；
    // 没有求值保存点时（旧分支上一轮，变异 b01-m7）外层事务已中止，随后查放行单报错 500、不建复核单；
    // M724 基线上闸口不在事务里，前半段照旧 409，红在末尾的重放（准入闸不认重放，409）
    @Test
    void anEvaluationSqlErrorStillBlocksToAPendingReviewAndTheApprovedReleaseLetsTheSameRequestThroughOnce() throws Exception {
        draftRule();
        effectiveContract("{}");
        String key = "b01-eval-" + suffix;
        String body = publishJson(key, "放行回归求值异常");

        String blocked = body(publish(userId, body)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ADMISSION_BLOCKED")));
        List<String> pending = jdbcTemplate.queryForList("""
                SELECT review_id FROM supervisor_review_decisions
                 WHERE action_type = ? AND object_id = ? AND initiated_by = ? AND review_status = 'pending_review'
                """, String.class, ACTION, ruleId, userId);
        assertThat(pending).hasSize(1);
        assertThat(blocked).contains(pending.get(0));
        assertThat(evalFailedClosed()).isEqualTo(1);
        assertThat(published(key)).isZero();

        // 第二次还能过：主管批准后同键同正文再发，求值照样报错、照样 fail-closed，用掉这张放行单恰好一次并执行
        approve(pending.get(0));
        String first = body(publish(userId, body).andExpect(status().isOk()));
        assertReleasedOnce(pending.get(0));
        assertThat(published(key)).isEqualTo(1);
        assertThat(evalFailedClosed()).isEqualTo(2);
        // 再重放：返回第一次的结果，不建复核单、不再执行
        assertThat(data(body(publish(userId, body).andExpect(status().isOk())))).isEqualTo(data(first));
        assertReleasedOnce(pending.get(0));
        assertThat(published(key)).isEqualTo(1);
        assertThat(pendingReviews(userId)).isZero();
    }

    // 同键并发时后一笔用掉了同对象另一张已批准的放行单，随后幂等层只返回第一次的结果——后一笔照样返回第一次的结果并提示已完成，
    // 闸口自己开的事务整笔回滚，这张放行单不算用掉；
    // 修复前后一笔 200 并把另一张放行单白白用掉（released_at 非空、admission.release_executed 多一条）
    @Test
    void aDuplicateSubmissionRacingTheFirstDoesNotSpendAnotherApprovedRelease() throws Exception {
        draftRule();
        // 两张待审单先后建好（新单的 evaluated_at 较晚），只批准旧单
        String oldReviewId = blocked("b01-block-" + suffix, "放行回归并发重复");
        String newReviewId = blocked("b01-block-second-" + suffix, "放行回归并发重复");
        assertThat(newReviewId).isNotEqualTo(oldReviewId);
        approve(oldReviewId);
        String key = "b01-race-dup-" + suffix;
        String body = publishJson(key, "放行回归并发重复");

        // 第一笔用掉旧单后停在写规则行；这时批准新单，第二笔同键同正文进来用掉新单，在幂等占位上等第一笔提交
        List<MvcResult> results = raceOnTheRelease(body, body, () -> approve(newReviewId), "INSERT INTO idempotency_records");

        assertThat(results).extracting(result -> result.getResponse().getStatus()).containsExactly(200, 200);
        String racedSecond = results.get(1).getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(racedSecond).contains("该操作已完成（重复提交，返回首次结果）");
        assertThat(data(racedSecond)).isEqualTo(data(results.get(0).getResponse().getContentAsString(StandardCharsets.UTF_8)));
        assertReleasedOnce(oldReviewId);
        assertReleaseUnused(newReviewId);
        assertThat(published(key)).isEqualTo(1);
        assertThat(pendingReviews(userId)).isZero();

        // 后一笔再重试：认出是用掉旧单的那次请求的重放，返回第一次的结果，新单仍未用掉
        String first = results.get(0).getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(data(body(publish(userId, body).andExpect(status().isOk())))).isEqualTo(data(first));
        assertReleaseUnused(newReviewId);
        // 第二次还能过：换新请求编号、同样内容才用新单，恰好用掉一次
        String nextKey = "b01-race-dup-next-" + suffix;
        publish(userId, publishJson(nextKey, "放行回归并发重复")).andExpect(status().isOk());
        assertReleasedOnce(newReviewId);
        assertThat(published(nextKey)).isEqualTo(1);
    }

    // 第一次没被准入闸拦截、凭常规授权成功，什么单次来源都没用掉；之后同键同正文重放时新用掉的单次授权或放行单
    // 都不算用掉（返回第一次的结果并提示已完成，事务整笔回滚）。修复前幂等层照样返回第一次的结果，但放行单被白白用掉
    @Test
    void aReplayOfARequestThatSpentNothingDoesNotSpendAFreshSingleUseGrantOrAnApprovedRelease() throws Exception {
        draftRule();
        effectiveContract("[\"ticketing\"]");
        String key = "b01-open-" + suffix;
        String body = publishJson(key, "放行回归未拦截");
        String firstResult = body(publish(userId, body).andExpect(status().isOk()));
        assertThat(published(key)).isEqualTo(1);

        // 单次授权：收回常规授权、改授单次授权，同键同正文重放（准入闸不触发）
        regrant(true);
        assertThat(data(body(publish(userId, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("该操作已完成（重复提交，返回首次结果）"))))).isEqualTo(data(firstResult));
        assertSingleUseGrantConsumed(false, 0);
        assertThat(published(key)).isEqualTo(1);
        // 第二次还能过：换新请求编号凭这张单次授权，恰好用掉一次；这次请求再重放返回第一次的结果、不再用掉
        String grantKey = "b01-open-grant-" + suffix;
        String grantBody = publishJson(grantKey, "放行回归单次授权发布");
        String grantFirst = body(publish(userId, grantBody).andExpect(status().isOk()));
        assertSingleUseGrantConsumed(true, 1);
        assertThat(data(body(publish(userId, grantBody).andExpect(status().isOk())))).isEqualTo(data(grantFirst));
        assertSingleUseGrantConsumed(true, 1);
        assertThat(published(grantKey)).isEqualTo(1);

        // 放行单：恢复常规授权、合同撤掉后准入闸命中，本人另有一张已批准未用的放行单，同键同正文重放第一次的请求
        regrant(false);
        jdbcTemplate.update("DELETE FROM contracts WHERE id = ?", contractId);
        String reviewId = blockedThenApproved("b01-block-" + suffix, "放行回归未拦截");
        assertThat(data(body(publish(userId, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("该操作已完成（重复提交，返回首次结果）"))))).isEqualTo(data(firstResult));
        assertReleaseUnused(reviewId);
        assertThat(published(key)).isEqualTo(1);
        assertThat(pendingReviews(userId)).isZero();
        // 第二次还能过：换新请求编号、同样内容凭这张放行单，恰好用掉一次
        String releaseKey = "b01-open-release-" + suffix;
        publish(userId, publishJson(releaseKey, "放行回归未拦截")).andExpect(status().isOk());
        assertReleasedOnce(reviewId);
        assertThat(published(releaseKey)).isEqualTo(1);
    }

    /**
     * 让第一笔用掉放行单后停在业务写规则行上（测试连接先锁住规则行），第二笔进来在放行单行锁上等待，再放开规则行。
     * 修复前放行单在业务事务外已单独提交，第二笔不等待、直接查不到可用放行单。等待探测以 30 秒为限，超时照常往下走。
     * {@code beforeSecond} 在第一笔停住后、第二笔进来前执行；{@code secondWaitsOn} 是第二笔等锁时所执行语句的片段。
     */
    private List<MvcResult> raceOnTheRelease(String firstBody, String secondBody, Runnable beforeSecond, String secondWaitsOn)
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Connection lock = dataSource.getConnection();
        try {
            lock.setAutoCommit(false);
            try (PreparedStatement statement = lock.prepareStatement(
                    "SELECT id FROM ticket_dynamic_pricing_rules WHERE id = ? FOR UPDATE")) {
                statement.setString(1, ruleId);
                statement.executeQuery().close();
            }
            Future<MvcResult> first = pool.submit(() -> publish(userId, firstBody).andReturn());
            awaitLockWait("ticket_dynamic_pricing_rules", first);
            beforeSecond.run();
            Future<MvcResult> second = pool.submit(() -> publish(userId, secondBody).andReturn());
            awaitLockWait(secondWaitsOn, second);
            lock.commit();
            return List.of(first.get(60, TimeUnit.SECONDS), second.get(60, TimeUnit.SECONDS));
        } finally {
            lock.close();
            pool.shutdownNow();
        }
    }

    private void awaitLockWait(String statementFragment, Future<?> request) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (System.nanoTime() < deadline && !request.isDone()) {
            Integer waiting = jdbcTemplate.queryForObject("""
                    SELECT count(*) FROM pg_stat_activity
                     WHERE datname = current_database() AND pid <> pg_backend_pid()
                       AND wait_event_type = 'Lock' AND query LIKE ?
                    """, Integer.class, "%" + statementFragment + "%");
            if (waiting != null && waiting > 0) {
                return;
            }
            Thread.sleep(50);
        }
    }

    /** 本人以给定原因发起一次被拦截的发布（409 生成待审复核单），再把这张复核单置为已批准，返回其单号。 */
    private String blockedThenApproved(String key, String reason) throws Exception {
        String reviewId = blocked(key, reason);
        approve(reviewId);
        return reviewId;
    }

    /** 本人以给定原因发起一次被拦截的发布（409 生成待审复核单），返回提示里的复核单号（须是本人名下的一张待审复核单）。 */
    private String blocked(String key, String reason) throws Exception {
        String response = body(publish(userId, publishJson(key, reason))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ADMISSION_BLOCKED")));
        List<String> pending = jdbcTemplate.queryForList("""
                SELECT review_id FROM supervisor_review_decisions
                 WHERE action_type = ? AND object_id = ? AND initiated_by = ? AND review_status = 'pending_review'
                """, String.class, ACTION, ruleId, userId).stream().filter(response::contains).toList();
        assertThat(pending).hasSize(1);
        return pending.get(0);
    }

    private void approve(String reviewId) {
        jdbcTemplate.update("""
                UPDATE supervisor_review_decisions
                   SET review_status = 'approved', decision = 'approved', reviewer_level = 3
                 WHERE review_id = ?
                """, reviewId);
    }

    private void draftRule() {
        jdbcTemplate.update("""
                INSERT INTO ticket_dynamic_pricing_rules (
                    id, ticket_type_id, rule_name, priority, effective_from, effective_to, adjustment_type, adjustment_rate,
                    status, created_at, updated_at
                )
                VALUES (?, ?, ?, 0, CURRENT_DATE, CURRENT_DATE + 30, 'percent_delta', 10, 'draft', now(), now())
                """, ruleId, ticketTypeId, "放行回归规则-" + suffix);
    }

    private void staff(String user) {
        staff(user, merchantId);
    }

    private void staff(String user, String merchant) {
        staff(user, "org", merchant);
    }

    private void staff(String user, String project, String merchant) {
        accessRepository.createUser(new AccessUser(user, user, "staff", project, merchant, "active", Instant.now()));
        grant(user, false);
    }

    /** 收回本人现有授权，改授常规授权或单次授权。 */
    private void regrant(boolean singleUse) {
        jdbcTemplate.update("DELETE FROM temporary_permission_grants WHERE subject_id = ?", userId);
        grant(userId, singleUse);
    }

    private void grant(String user, boolean singleUse) {
        Instant now = Instant.now();
        accessRepository.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), user, List.of(PUBLISH),
                now.minusSeconds(3600), now.plusSeconds(86_400), singleUse, false, "active",
                singleUse ? "XM-ONESHOT-RETURN B01 单次授权回归" : "XM-ONESHOT-RETURN B01 放行回归", "seed-admin", now, null, null));
    }

    /** 本类商户一份覆盖今天的生效合同，经营范围按给定 JSON。 */
    private void effectiveContract(String allowedBusinessScope) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        jdbcTemplate.update("""
                INSERT INTO contracts (
                    id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, expiry_date,
                    lifecycle_status, allowed_business_scope, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, 'effective', ?::jsonb, now(), now())
                """, contractId, merchantId, contractId + "_subject", contractId + "_settlement",
                today.minusDays(1), today.plusDays(1), allowedBusinessScope);
    }

    private void assertSingleUseGrantConsumed(boolean consumed, int consumptionLogs) {
        assertThat(jdbcTemplate.queryForList("SELECT consumed FROM temporary_permission_grants WHERE subject_id = ?",
                Boolean.class, userId)).containsExactly(consumed);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM permission_template_usage_logs WHERE user_id = ? AND result = 'consumed'",
                Integer.class, userId)).isEqualTo(consumptionLogs);
    }

    private Integer evalFailedClosed() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events WHERE event_name = 'admission.eval_failed_closed' AND business_id = ?",
                Integer.class, ruleId);
    }

    private ResultActions publish(String user, String body) throws Exception {
        return publish(user, merchantId, body);
    }

    private ResultActions publish(String user, String merchant, String body) throws Exception {
        return publish(user, "org", merchant, body);
    }

    private ResultActions publish(String user, String project, String merchant, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/ticketing/dynamic-pricing-rules/" + ruleId + "/publish")
                .with(request -> {
                    request.addHeader("X-Terminal-Type", "web");
                    return authentication(new UsernamePasswordAuthenticationToken(
                            new AccessActor(user, user, "staff", project, merchant, "active"), null,
                            List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String publishJson(String key, String reason) {
        return """
                {"idempotency_key": "%s", "reason": "%s"}
                """.formatted(key, reason);
    }

    private static String body(ResultActions actions) throws Exception {
        return actions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    // 响应包络的 request_id / server_time 每次不同，比较第一次与重放的业务结果只看 data
    private static JsonNode data(String body) throws Exception {
        return new ObjectMapper().readTree(body).get("data");
    }

    private void assertReleaseUnused(String reviewId) {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT released_at IS NULL FROM supervisor_review_decisions WHERE review_id = ?", Boolean.class, reviewId))
                .as("放行单仍未用掉").isTrue();
        assertThat(releaseTrails(reviewId)).isZero();
    }

    private void assertReleasedOnce(String reviewId) {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT released_by FROM supervisor_review_decisions WHERE review_id = ? AND released_at IS NOT NULL",
                String.class, reviewId)).isEqualTo(userId);
        assertThat(releaseTrails(reviewId)).isEqualTo(1);
    }

    private Integer releaseTrails(String reviewId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events WHERE event_name = 'admission.release_executed' AND business_id = ?",
                Integer.class, reviewId);
    }

    private Integer pendingReviews(String user) {
        return jdbcTemplate.queryForObject("""
                SELECT count(*) FROM supervisor_review_decisions
                 WHERE action_type = ? AND object_id = ? AND initiated_by = ? AND review_status = 'pending_review'
                """, Integer.class, ACTION, ruleId, user);
    }

    private Integer published(String idempotencyKey) {
        return jdbcTemplate.queryForObject("""
                SELECT count(*) FROM audit_events
                 WHERE event_name = 'ticket_dynamic_pricing.published' AND event_result = 'success' AND idempotency_key = ?
                """, Integer.class, idempotencyKey);
    }
}
