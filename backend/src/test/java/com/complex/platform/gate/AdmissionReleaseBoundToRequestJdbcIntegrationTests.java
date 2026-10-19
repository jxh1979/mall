package com.complex.platform.gate;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.TemporaryGrant;
import com.complex.platform.bar.BarOrderItemRecord;
import com.complex.platform.bar.BarOrderRecord;
import com.complex.platform.bar.BarProductRecord;
import com.complex.platform.bar.BarRepository;
import com.complex.platform.bar.BarTableRecord;
import com.complex.platform.finance.ArApItemRecord;
import com.complex.platform.finance.ArApPaymentRecord;
import com.complex.platform.finance.FinanceRepository;
import com.complex.platform.finance.RefundAuthorityLimitRecord;
import com.complex.platform.finance.RefundAuthorizationRepository;
import com.complex.platform.support.AppendOnlyTestBypass;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
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
import org.springframework.test.web.servlet.ResultActions;

/**
 * M724-F16（治理地图第 528 行「每次只能对当前这一笔放行、不得整类关闭校验」、第 303 行「每次只对当前这一笔生效并留痕」；
 * 操作员 2026-10-07 裁决「批准只能被『被拦下的那次提交的同样内容』用掉，内容不同的另一笔照常拦截送审。」）：
 * 准入闸逐笔放行按请求内容绑定的真实库、真实接口回归。
 *
 * <p>三类入口各一个：只有请求体的入口用财务退款（{@code POST /api/v1/finance/refunds}，对象编号落成动作类型）；
 * 父单下的明细用酒吧退菜（{@code POST /api/v1/bar/orders/{order_id}/void-item}，对象编号是订单号、明细号在请求体里）；
 * 带路径对象编号的入口用票务动态定价发布（{@code POST /api/v1/ticketing/dynamic-pricing-rules/{id}/publish}）。
 * V099 种子策略 {@code finance_refund_over_scope}、{@code bar_order_item_void_over_scope}、{@code ticketing_dyn_pricing_over_scope}
 * 对无生效合同的商户员工命中拦截；本类的商户按随机后缀新建、没有合同，恒命中。
 * 放行单用真实拦截生成的待审复核单，再把它置为已批准（同 {@code AdmissionReleaseOneShotJdbcIntegrationTests}）。
 *
 * <p>本类不开测试事务：拦截复核单、放行消费、幂等记录都要真实提交才看得见。夹具按随机后缀隔离、结束时删除
 * （收款、台账项、退菜记录三张禁删表用 {@link AppendOnlyTestBypass}）；复核单与审计事件只追加不删（V148 / V140），按后缀隔离留在库里。
 * 写请求带 {@code X-Terminal-Type: web}。提交编号一律「f16-＋短标记＋后缀」，互不相同（退款单号由提交编号前 40 位派生）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class AdmissionReleaseBoundToRequestJdbcIntegrationTests {

    private static final String REFUND_PERMISSION = "finance.refund.create";
    private static final String VOID_PERMISSION = "bar.order_item.void";
    private static final String PUBLISH_PERMISSION = "ticket.dynamic_pricing.publish";
    private static final String REFUND_ACTION = "finance.refund";
    private static final String VOID_ACTION = "bar.order_item_void";
    private static final String PUBLISH_ACTION = "ticketing.dynamic_pricing_publish";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AccessRepository accessRepository;

    @Autowired
    private FinanceRepository financeRepository;

    @Autowired
    private RefundAuthorizationRepository refundAuthorizationRepository;

    @Autowired
    private BarRepository barRepository;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String userId = "usr_f16_" + suffix;
    private final String merchantId = "mch_f16_" + suffix;
    // 财务退款：一条应收台账项、挂在它上面的一笔收款（退款对象）
    private final String arApItemId = "arap_f16_" + suffix;
    private final String paymentId = "pay_f16_" + suffix;
    // 酒吧退菜：一张桌台、一个商品、一张订单、两条未出品明细
    private final String barId = "bar_f16_" + suffix;
    private final String tableId = "tbl_f16_" + suffix;
    private final String productId = "prd_f16_" + suffix;
    private final String orderId = "ord_f16_" + suffix;
    private final String itemOneId = "oi1_f16_" + suffix;
    private final String itemTwoId = "oi2_f16_" + suffix;
    // 动态定价发布：票种与一条草稿规则
    private final String ticketTypeId = "tt_f16_" + suffix;
    private final String ruleId = "tdpr_f16_" + suffix;

    @BeforeEach
    void seed() {
        assertSeedBlockPolicy("finance_refund_over_scope", REFUND_ACTION);
        assertSeedBlockPolicy("bar_order_item_void_over_scope", VOID_ACTION);
        assertSeedBlockPolicy("ticketing_dyn_pricing_over_scope", PUBLISH_ACTION);
        Instant now = Instant.now();
        accessRepository.createUser(new AccessUser(userId, userId, "staff", "org", merchantId, "active", now));
        accessRepository.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), userId,
                List.of(REFUND_PERMISSION, VOID_PERMISSION, PUBLISH_PERMISSION),
                now.minusSeconds(3600), now.plusSeconds(86_400), false, false, "active", "M724-F16 放行按内容绑定回归",
                "seed-admin", now, null, null));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        // 财务：退款单 → 收款（禁改删）→ 台账项（禁删）→ 额度。收款对台账项是级联删除，先删台账项会被收款的触发器挡住
        jdbcTemplate.update("DELETE FROM finance_refund_attempts WHERE refund_id IN "
                + "(SELECT id FROM finance_refunds WHERE target_id = ?)", paymentId);
        jdbcTemplate.update("DELETE FROM finance_refunds WHERE target_id = ?", paymentId);
        AppendOnlyTestBypass.updateOnTable(jdbcTemplate, "ar_ap_payments", "trg_append_only_ud",
                "DELETE FROM ar_ap_payments WHERE id = ?", paymentId);
        AppendOnlyTestBypass.updateOnTable(jdbcTemplate, "ar_ap_items", "trg_append_only_d",
                "DELETE FROM ar_ap_items WHERE id = ?", arApItemId);
        jdbcTemplate.update("DELETE FROM refund_authority_limits WHERE subject_id = ?", userId);
        // 酒吧：退菜记录（禁删；对明细、订单都是级联删除，须先删）→ 明细 → 订单 → 桌台 → 商品
        AppendOnlyTestBypass.updateOnTable(jdbcTemplate, "bar_order_item_voids", "trg_append_only_d",
                "DELETE FROM bar_order_item_voids WHERE order_id = ?", orderId);
        jdbcTemplate.update("DELETE FROM bar_order_items WHERE order_id = ?", orderId);
        jdbcTemplate.update("DELETE FROM bar_orders WHERE id = ?", orderId);
        jdbcTemplate.update("DELETE FROM bar_tables WHERE id = ?", tableId);
        jdbcTemplate.update("DELETE FROM bar_products WHERE id = ?", productId);
        // 票务
        jdbcTemplate.update("DELETE FROM ticket_dynamic_pricing_rules WHERE id = ?", ruleId);
        jdbcTemplate.update("DELETE FROM ticket_types WHERE id = ?", ticketTypeId);
        // 授权、员工、本类提交编号的幂等记录
        jdbcTemplate.update("DELETE FROM temporary_permission_grants WHERE subject_id = ?", userId);
        jdbcTemplate.update("DELETE FROM access_users WHERE id = ?", userId);
        jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key LIKE ?", "f16-%" + suffix);
    }

    // 只有请求体的入口（对象编号落成动作类型）：退款甲的批准只能被退款甲用掉；内容不同的退款乙照常拦截、另建复核单、不执行。
    // 修复前同一发起人、同一动作的任何一笔都共用这个对象编号，乙直接用掉甲的批准执行（200）
    @Test
    void anApprovedRefundIsSpentOnlyByTheSameRefund() throws Exception {
        receivedPayment();
        // ① 退款甲被拦下，主管批准
        String reviewA = blocked(refund(refundJson("f16-ra1-" + suffix, 3000L, "放行绑定回归退款甲")), REFUND_ACTION, REFUND_ACTION);
        approve(reviewA);

        // ② 同一人提交内容不同的退款乙：409 ADMISSION_BLOCKED、另建一张复核单、乙没有执行、甲的批准仍未用掉
        String reviewB = blocked(refund(refundJson("f16-rb1-" + suffix, 5000L, "放行绑定回归退款乙")), REFUND_ACTION, REFUND_ACTION);
        assertThat(reviewB).isNotEqualTo(reviewA);
        assertThat(refunds()).isZero();
        assertReleaseUnused(reviewA);
        // 两张复核单各记着被拦下那次提交的请求内容摘要（SHA-256 十六进制 64 位），互不相同
        assertThat(blockedContentDigest(reviewA)).matches("[0-9a-f]{64}");
        assertThat(blockedContentDigest(reviewB)).matches("[0-9a-f]{64}").isNotEqualTo(blockedContentDigest(reviewA));

        // ③ 乙也获批准（两张已批准未用、内容不同，乙建单更晚）。甲原样重提、换新提交编号：执行成功，恰好用掉甲的批准一次，乙的仍未用
        approve(reviewB);
        String bodyA = refundJson("f16-ra2-" + suffix, 3000L, "放行绑定回归退款甲");
        String first = body(refund(bodyA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refund_amount_cent").value(3000)));
        assertThat(refunds()).isEqualTo(1);
        assertReleasedOnce(reviewA);
        assertReleaseUnused(reviewB);

        // ④ 甲同号同内容再提：返回第一次的结果并提示已完成，不再执行、不消费别的批准、不建复核单
        String replayed = body(refund(bodyA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("该操作已完成（重复提交，返回首次结果）"))
                .andExpect(jsonPath("$.replayed").value(true)));
        assertThat(data(replayed)).isEqualTo(data(first));
        assertThat(refunds()).isEqualTo(1);
        assertReleasedOnce(reviewA);
        assertReleaseUnused(reviewB);
        assertThat(pendingReviews(REFUND_ACTION, REFUND_ACTION)).isEmpty();

        // ⑤ 第二次还能过：乙原样重提、换新提交编号，恰好用掉乙的批准一次
        refund(refundJson("f16-rb2-" + suffix, 5000L, "放行绑定回归退款乙"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refund_amount_cent").value(5000));
        assertReleasedOnce(reviewB);
        assertThat(refunds()).isEqualTo(2);
    }

    // 父单下的明细（对象编号是订单号，明细号只在请求体里）：明细一的批准不能被同一订单下明细二的提交用掉。
    // 修复前按订单号匹配，明细二直接用掉明细一的批准执行（200）
    @Test
    void theApprovedVoidOfOneItemIsNotSpentByAnotherItemOfTheSameOrder() throws Exception {
        orderWithTwoPendingItems();
        String reason = "放行绑定回归退菜";
        // ① 退明细一被拦下，主管批准
        String reviewOne = blocked(voidItem(voidJson("f16-v1a-" + suffix, itemOneId, reason)), VOID_ACTION, orderId);
        approve(reviewOne);

        // ② 同一订单退明细二（原因相同）：409 ADMISSION_BLOCKED、另建一张复核单、明细二没有退、明细一的批准仍未用掉
        String reviewTwo = blocked(voidItem(voidJson("f16-v2a-" + suffix, itemTwoId, reason)), VOID_ACTION, orderId);
        assertThat(reviewTwo).isNotEqualTo(reviewOne);
        assertThat(itemVoided(itemTwoId)).isFalse();
        assertThat(voidRecords(itemTwoId)).isZero();
        assertReleaseUnused(reviewOne);

        // ③ 明细一原样重提、换新提交编号：执行成功，恰好用掉明细一的批准一次；明细二仍未退，它的复核单仍待审
        voidItem(voidJson("f16-v1b-" + suffix, itemOneId, reason)).andExpect(status().isOk());
        assertThat(itemVoided(itemOneId)).isTrue();
        assertThat(voidRecords(itemOneId)).isEqualTo(1);
        assertReleasedOnce(reviewOne);
        assertThat(itemVoided(itemTwoId)).isFalse();
        assertThat(voidRecords(itemTwoId)).isZero();
        assertThat(pendingReviews(VOID_ACTION, orderId)).containsExactly(reviewTwo);
    }

    // 带路径对象编号的入口：同一规则、内容不同的另一次发布不能用掉前一次内容的批准。修复前只按规则号匹配，另一次发布直接执行（200）
    @Test
    void anApprovedPublishIsNotSpentByAPublishWithDifferentContent() throws Exception {
        draftRule();
        // ① 以原因甲发布被拦下，主管批准
        String reviewOne = blocked(publish(publishJson("f16-p1a-" + suffix, "放行绑定回归原因甲")), PUBLISH_ACTION, ruleId);
        approve(reviewOne);

        // ② 同一规则以原因乙发布：409 ADMISSION_BLOCKED、另建一张复核单、没有发布、原因甲的批准仍未用掉
        String otherKey = "f16-p2a-" + suffix;
        String reviewTwo = blocked(publish(publishJson(otherKey, "放行绑定回归原因乙")), PUBLISH_ACTION, ruleId);
        assertThat(reviewTwo).isNotEqualTo(reviewOne);
        assertThat(published(otherKey)).isZero();
        assertThat(ruleStatus()).isEqualTo("draft");
        assertReleaseUnused(reviewOne);

        // ③ 原因甲原样重提、换新提交编号：发布成功，恰好用掉这张批准一次
        String sameKey = "f16-p1b-" + suffix;
        publish(publishJson(sameKey, "放行绑定回归原因甲"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("published"));
        assertReleasedOnce(reviewOne);
        assertThat(published(sameKey)).isEqualTo(1);
    }

    // 验收①末句「升级前建的复核单没有内容摘要，不能再被用于放行（须重新送审）」：把一张已批准的复核单的内容摘要抹掉
    // （造一张升级前那样的单），同内容重提照常拦截、另建复核单、不执行。修复前照样放行（200）
    @Test
    void anApprovedReviewWithoutAContentDigestCannotBeSpent() throws Exception {
        draftRule();
        String reason = "放行绑定回归无摘要";
        String reviewOne = blocked(publish(publishJson("f16-n1a-" + suffix, reason)), PUBLISH_ACTION, ruleId);
        approve(reviewOne);
        jdbcTemplate.update("UPDATE supervisor_review_decisions SET blocked_content_digest = NULL WHERE review_id = ?", reviewOne);

        String key = "f16-n1b-" + suffix;
        String reviewTwo = blocked(publish(publishJson(key, reason)), PUBLISH_ACTION, ruleId);
        assertThat(reviewTwo).isNotEqualTo(reviewOne);
        assertThat(published(key)).isZero();
        assertThat(ruleStatus()).isEqualTo("draft");
        assertReleaseUnused(reviewOne);
    }

    private void assertSeedBlockPolicy(String policyCode, String action) {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM supervisor_review_policies
                 WHERE policy_code = ? AND action_type = ?
                   AND enabled AND outcome = 'block' AND trigger_mode = 'over_contract_scope'
                """, Integer.class, policyCode, action)).as("V099 种子准入策略：" + policyCode).isEqualTo(1);
    }

    /** 本类商户的一条应收台账项（20000 分，已收 10000 分）、挂在它上面的一笔 10000 分收款，以及本人的财务退款单笔额度 10000 分。 */
    private void receivedPayment() {
        Instant now = Instant.now();
        financeRepository.saveArApItem(new ArApItemRecord(
                arApItemId,
                "ARAP-F16-" + suffix,
                "receivable",
                merchantId,
                "order",
                "order_f16_" + suffix,
                20000L,
                10000L,
                10000L,
                "CNY",
                LocalDate.now(ZoneOffset.UTC).plusDays(30),
                YearMonth.now(ZoneOffset.UTC).toString(),
                "partial_paid",
                now,
                now,
                merchantId,
                // FS-021-R6：共享财务退款按项目范围办理，台账须有项目归属（本人项目 org）。
                "org"
        ));
        financeRepository.saveArApPayment(new ArApPaymentRecord(
                paymentId,
                arApItemId,
                10000L,
                "bank_transfer",
                now,
                "bank_flow_f16_" + suffix,
                now
        ));
        // 没有额度时业务按超出自助授权限额拒绝（REFUND_REVIEW_REQUIRED）；额度是单笔口径
        refundAuthorizationRepository.saveLimit(new RefundAuthorityLimitRecord(
                "rl_f16_" + suffix, "staff", userId, "finance", 10000L, null, "active", null, null));
    }

    /** 本类商户的一张订单，挂两条未出品明细（未出品明细退菜即时生效，不走高价值先审）。 */
    private void orderWithTwoPendingItems() {
        Instant now = Instant.now();
        barRepository.saveTable(new BarTableRecord(
                tableId, barId, merchantId, "T-F16" + suffix, "放行绑定回归桌", "card_seat",
                4, 1, 0L, "CNY", "active", now, now));
        barRepository.saveProduct(new BarProductRecord(
                productId,
                barId,
                merchantId,
                "放行绑定回归酒",
                "spirit",
                8800L,
                "CNY",
                null,
                null,
                false,
                "active",
                now,
                now,
                30,
                "ml",
                10,
                false,
                null,
                null,
                null
        ));
        barRepository.saveOrder(new BarOrderRecord(
                orderId, barId, merchantId, tableId, null, null,
                "staff", "created", 17600L, "CNY", "web", null, null, now, now));
        for (String itemId : List.of(itemOneId, itemTwoId)) {
            barRepository.saveOrderItem(new BarOrderItemRecord(
                    itemId,
                    orderId,
                    productId,
                    "放行绑定回归酒",
                    1,
                    8800L,
                    8800L,
                    "pending",
                    now,
                    now,
                    null,
                    0L
            ));
        }
    }

    private void draftRule() {
        jdbcTemplate.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at)
                VALUES (?, ?, 'weekday_weekend_holiday', FALSE, 'published', now(), now())
                """, ticketTypeId, "放行绑定回归票种-" + suffix);
        jdbcTemplate.update("""
                INSERT INTO ticket_dynamic_pricing_rules (
                    id, ticket_type_id, rule_name, priority, effective_from, effective_to, adjustment_type, adjustment_rate,
                    status, created_at, updated_at
                )
                VALUES (?, ?, ?, 0, CURRENT_DATE, CURRENT_DATE + 30, 'percent_delta', 10, 'draft', now(), now())
                """, ruleId, ticketTypeId, "放行绑定回归规则-" + suffix);
    }

    /** 这次提交须被准入闸拦下（409 ADMISSION_BLOCKED）；返回提示里的复核单号（须是本人名下该动作、该对象的一张待审复核单）。 */
    private String blocked(ResultActions submitted, String action, String objectId) throws Exception {
        String response = body(submitted
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ADMISSION_BLOCKED")));
        List<String> pending = pendingReviews(action, objectId).stream().filter(response::contains).toList();
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

    private ResultActions refund(String body) throws Exception {
        return submit("/api/v1/finance/refunds", body);
    }

    private ResultActions voidItem(String body) throws Exception {
        return submit("/api/v1/bar/orders/" + orderId + "/void-item", body);
    }

    private ResultActions publish(String body) throws Exception {
        return submit("/api/v1/ticketing/dynamic-pricing-rules/" + ruleId + "/publish", body);
    }

    private ResultActions submit(String path, String body) throws Exception {
        return mockMvc.perform(post(path)
                .with(request -> {
                    request.addHeader("X-Terminal-Type", "web");
                    return authentication(new UsernamePasswordAuthenticationToken(
                            new AccessActor(userId, userId, "staff", "org", merchantId, "active"), null,
                            List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    // 不带 refund_channel、device_id：非现场渠道，不过设备门控
    private String refundJson(String key, long amountCent, String reason) {
        return """
                {"idempotency_key": "%s", "target_id": "%s", "refund_amount_cent": %d, "reason": "%s"}
                """.formatted(key, paymentId, amountCent, reason);
    }

    // 不带账单号、原因码、附件：未出品明细即时生效，不带账单号时不冲账单金额
    private static String voidJson(String key, String orderItemId, String reason) {
        return """
                {"idempotency_key": "%s", "order_item_id": "%s", "reason": "%s"}
                """.formatted(key, orderItemId, reason);
    }

    private static String publishJson(String key, String reason) {
        return """
                {"idempotency_key": "%s", "reason": "%s"}
                """.formatted(key, reason);
    }

    private static String body(ResultActions actions) throws Exception {
        return actions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    // 响应包络的 request_id / server_time 每次不同，比较第一次与重提的业务结果只看 data
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

    private String blockedContentDigest(String reviewId) {
        return jdbcTemplate.queryForObject(
                "SELECT blocked_content_digest FROM supervisor_review_decisions WHERE review_id = ?", String.class, reviewId);
    }

    /** 本人名下该动作、该对象的待审复核单号。 */
    private List<String> pendingReviews(String action, String objectId) {
        return jdbcTemplate.queryForList("""
                SELECT review_id FROM supervisor_review_decisions
                 WHERE action_type = ? AND object_id = ? AND initiated_by = ? AND review_status = 'pending_review'
                """, String.class, action, objectId, userId);
    }

    // 挂在本类收款上的退款单数
    private Integer refunds() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM finance_refunds WHERE target_id = ?", Integer.class, paymentId);
    }

    private Boolean itemVoided(String orderItemId) {
        return jdbcTemplate.queryForObject("SELECT voided FROM bar_order_items WHERE id = ?", Boolean.class, orderItemId);
    }

    private Integer voidRecords(String orderItemId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM bar_order_item_voids WHERE order_item_id = ?", Integer.class, orderItemId);
    }

    private String ruleStatus() {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM ticket_dynamic_pricing_rules WHERE id = ?", String.class, ruleId);
    }

    private Integer published(String idempotencyKey) {
        return jdbcTemplate.queryForObject("""
                SELECT count(*) FROM audit_events
                 WHERE event_name = 'ticket_dynamic_pricing.published' AND event_result = 'success' AND idempotency_key = ?
                """, Integer.class, idempotencyKey);
    }
}
