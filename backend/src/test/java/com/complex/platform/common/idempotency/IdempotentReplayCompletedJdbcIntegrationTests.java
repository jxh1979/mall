package com.complex.platform.common.idempotency;

// 文件版本：V0.04

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.TemporaryGrant;
import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerStoredValueAccountRecord;
import com.complex.platform.voucher.VoucherDefinitionRecord;
import com.complex.platform.voucher.VoucherInstanceRecord;
import com.complex.platform.voucher.VoucherRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * XM-ONESHOT-RETURN B02（治理地图第 303 行「全局原则 · 同时操作以先成功为准」：同一个操作被重复提交，系统只认一次；
 * 操作员 2026-09-25 确认的追加句：同一次操作重复提交时，返回第一次的结果并提示已完成）：真实库、真实接口回归。
 *
 * <p>第一次执行：{@code message=success}、不带 {@code replayed}。同一请求编号重复提交同一请求：{@code data} 仍是第一次的结果，
 * {@code message} 提示已完成、带 {@code replayed=true}，不再执行。覆盖三条路径：经 {@code TicketingOperationExecutor} →
 * {@code AuditedOperationExecutor} → 幂等层的票务写（主#51、#8），经 {@code AuditedOperationExecutor} → 幂等层的券核销（主#16），
 * 储值退款自带的按键重放（线上退款与柜台退款）；同键改了请求内容（是否销户、柜台兑付凭证号）不是同一次操作，409、不带标记。
 *
 * <p>M724-F10（治理地图第 528 行红线④「看不到别的商户和别的项目的数据」；枚举速查第 86 行「商户员工只能管理本商户发行的储值卡」）：
 * 储值退款按键重放之前先核对当前操作人能不能管这张卡——别的商户持退款权限的员工拿原提交编号、原请求内容提交被拒，拿不到第一次的流水。
 *
 * <p>M724-F15（同一条红线④；治理地图第 653 行「数据的项目与商户隔离不提供关闭开关」）：商户之外还核项目——同商户另一个项目的员工
 * 拿原提交编号、原请求内容提交同样被拒，拿不到第一次的流水；同项目同商户的本人重提仍返回第一次的流水并提示已完成。
 *
 * <p>本类开测试事务、结束回滚；MockMvc 与测试同线程，请求并入测试事务。写请求带 {@code X-Terminal-Type: web}。
 * 例外：M724-F10、M724-F15 的单次授权用例不开测试事务（单次消耗随请求事务回滚要真实提交才看得见），夹具清理见
 * {@code outOfScopeStaffHoldingOnlyASingleUseRefundGrantIsRejectedAndKeepsTheGrant}。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class IdempotentReplayCompletedJdbcIntegrationTests {

    private static final String COMPLETED = "该操作已完成（重复提交，返回首次结果）";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AccessRepository accessRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String userId = "usr_b02_" + suffix;
    private final String merchantId = "mch_b02_" + suffix;
    // M724-F10：另一个商户的员工，用到的用例里才建
    private final String otherUserId = "usr_b02o_" + suffix;
    private final String otherMerchantId = "mch_b02o_" + suffix;
    // M724-F15：同商户、另一个项目的员工所在的项目（本类其余员工都在项目 org），用到的用例里才建
    private final String otherProjectId = "org_b02o_" + suffix;

    @BeforeEach
    void seed() {
        Instant now = Instant.now();
        accessRepository.createUser(new AccessUser(userId, userId, "staff", "org", merchantId, "active", now));
        accessRepository.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), userId,
                List.of("ticket.dynamic_pricing.publish", "customer.voucher.redeem", "customer.stored_value.refund"),
                now.minusSeconds(3600), now.plusSeconds(86_400), false, false, "active",
                "XM-ONESHOT-RETURN B02 已完成提示回归", "seed-admin", now, null, null));
    }

    @AfterEach
    void clearActor() {
        SecurityContextHolder.clearContext();
    }

    // 主#51、#8：票务写经幂等层重放——第一次照常，同键同正文重复提交返回第一次的结果并提示已完成
    @Test
    void aTicketingWriteResubmittedWithTheSameKeyReturnsTheFirstResultAndSaysItIsAlreadyCompleted() throws Exception {
        String ruleId = "tdpr_b02_" + suffix;
        String ticketTypeId = "tt_b02_" + suffix;
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        // 商户有覆盖今天、经营范围含票务的生效合同：准入闸不拦截
        jdbcTemplate.update("""
                INSERT INTO contracts (
                    id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, expiry_date,
                    lifecycle_status, allowed_business_scope, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, 'effective', '["ticketing"]'::jsonb, now(), now())
                """, "ctr_b02_" + suffix, merchantId, "ctr_b02_subject_" + suffix, "ctr_b02_settlement_" + suffix,
                today.minusDays(1), today.plusDays(1));
        jdbcTemplate.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at)
                VALUES (?, ?, 'weekday_weekend_holiday', FALSE, 'published', now(), now())
                """, ticketTypeId, "已完成提示回归票种-" + suffix);
        jdbcTemplate.update("""
                INSERT INTO ticket_dynamic_pricing_rules (
                    id, ticket_type_id, rule_name, priority, effective_from, effective_to, adjustment_type, adjustment_rate,
                    status, created_at, updated_at
                )
                VALUES (?, ?, ?, 0, CURRENT_DATE, CURRENT_DATE + 30, 'percent_delta', 10, 'draft', now(), now())
                """, ruleId, ticketTypeId, "已完成提示回归规则-" + suffix);
        String path = "/api/v1/ticketing/dynamic-pricing-rules/" + ruleId + "/publish";
        String key = "b02-publish-" + suffix;
        String body = """
                {"idempotency_key": "%s", "reason": "已完成提示回归发布"}
                """.formatted(key);

        String first = body(write(path, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.replayed").doesNotExist())
                .andExpect(jsonPath("$.data.status").value("published")));
        assertThat(published(key)).isEqualTo(1);

        // 同键同正文重复提交：返回第一次的结果，提示已完成（修复前与第一次的响应一模一样：message=success、没有标记）
        assertCompletedReplay(write(path, body), first);
        assertThat(published(key)).isEqualTo(1);
        // 第二次还能过：再重复提交一次，照样返回第一次的结果并提示已完成，仍不再执行
        assertCompletedReplay(write(path, body), first);
        assertThat(published(key)).isEqualTo(1);

        // 换请求编号是新的一次操作：照常执行，不带已完成标记（标记不串到别的请求）
        String nextKey = "b02-publish-next-" + suffix;
        write(path, """
                {"idempotency_key": "%s", "reason": "已完成提示回归再发布"}
                """.formatted(nextKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.replayed").doesNotExist());
        assertThat(published(nextKey)).isEqualTo(1);

        // 同键改正文：按请求编号冲突拒绝，不是「已完成」。放在最后：失败的请求把测试事务标成只能回滚，
        // 之后同一测试事务里准入闸的保存点求值会按异常 fail-closed 拦截（生产每个请求各自一个事务，不受影响）
        write(path, """
                {"idempotency_key": "%s", "reason": "已完成提示回归改了原因"}
                """.formatted(key))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("IDEMPOTENCY_CONFLICT"))
                .andExpect(jsonPath("$.replayed").doesNotExist());
    }

    // 主#16：券核销是只能用一次的动作——同键同正文重复提交返回第一次的核销结果并提示已完成；换请求编号再核销照常拒绝
    @Test
    void aVoucherRedemptionResubmittedWithTheSameKeyReturnsTheFirstRedemptionAndSaysItIsAlreadyCompleted() throws Exception {
        Instant now = Instant.now();
        String definitionId = "vdef_b02_" + suffix;
        String instanceId = "vi_b02_" + suffix;
        voucherRepository.insertDefinition(new VoucherDefinitionRecord(definitionId, "已完成提示回归折扣券", "merchant_discount",
                "in_store_staff", "none", merchantId, 5000L, null, null, null, null, 30, null, "active", true, now, now));
        assertThat(voucherRepository.insertInstance(new VoucherInstanceRecord(instanceId, definitionId, "cus_b02_" + suffix,
                "VC" + instanceId, "marketing_grant", "vgrant_b02_" + suffix, "issued", now, now.plusSeconds(86_400L * 30),
                null, null, null, false, now, now))).isTrue();
        String key = "b02-redeem-" + suffix;
        String body = redeemJson(key, instanceId);

        String first = body(write("/api/v1/vouchers/redemptions", body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.replayed").doesNotExist())
                .andExpect(jsonPath("$.data.voucher_instance_id").value(instanceId)));
        assertThat(redemptions(instanceId)).isEqualTo(1);

        // 同键同正文重复提交（例如第一次的响应丢了）：返回第一次的核销结果并提示已完成，不再核销
        assertCompletedReplay(write("/api/v1/vouchers/redemptions", body), first);
        // 第二次还能过：再重复提交一次，照样
        assertCompletedReplay(write("/api/v1/vouchers/redemptions", body), first);
        assertThat(redemptions(instanceId)).isEqualTo(1);

        // 换请求编号是另一次核销：券已用掉，照常拒绝，不冒充「已完成」
        write("/api/v1/vouchers/redemptions", redeemJson("b02-redeem-again-" + suffix, instanceId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("VOUCHER_ALREADY_REDEEMED"))
                .andExpect(jsonPath("$.replayed").doesNotExist());
        assertThat(redemptions(instanceId)).isEqualTo(1);
    }

    // 储值退款自带的按键重放（线上退款、柜台退款两条路径）：重复提交返回第一次的流水并提示已完成，余额只退一次
    @Test
    void aStoredValueRefundResubmittedWithTheSameKeyReturnsTheFirstTransactionAndSaysItIsAlreadyCompleted() throws Exception {
        String onlineCustomer = customer("on");
        card(onlineCustomer, 9000L, "platform_collect");
        String onlinePath = "/api/v1/loyalty/stored-value/" + onlineCustomer + "/refund";
        String onlineKey = "b02-refund-" + suffix;
        String onlineBody = refundJson(onlineKey, 3000L, null);

        String first = body(write(onlinePath, onlineBody)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.replayed").doesNotExist()));
        assertThat(balance(onlineCustomer)).isEqualTo(6000L);

        assertCompletedStoredValueReplay(write(onlinePath, onlineBody), first);
        // 第二次还能过：再重复提交一次，照样返回第一次的流水，余额仍只退一次
        assertCompletedStoredValueReplay(write(onlinePath, onlineBody), first);
        assertThat(balance(onlineCustomer)).isEqualTo(6000L);
        // 换请求编号是新的一次退款：照常执行，不带已完成标记
        write(onlinePath, refundJson("b02-refund-next-" + suffix, 1000L, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.replayed").doesNotExist());
        assertThat(balance(onlineCustomer)).isEqualTo(5000L);

        // 柜台退款的流水键带 svcr_ 命名空间前缀：按客人端原请求编号认出重复提交
        String counterCustomer = customer("ct");
        card(counterCustomer, 8000L, "merchant_prepaid");
        String counterPath = "/api/v1/loyalty/stored-value/" + counterCustomer + "/counter-refund";
        String counterBody = refundJson("b02-counter-" + suffix, 2000L, "VOUCHER-B02-" + suffix);
        String counterFirst = body(write(counterPath, counterBody)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.replayed").doesNotExist()));
        assertThat(balance(counterCustomer)).isEqualTo(6000L);
        assertCompletedStoredValueReplay(write(counterPath, counterBody), counterFirst);
        assertThat(balance(counterCustomer)).isEqualTo(6000L);
    }

    // 审查修正：储值退款的重放只核对客人、类型、金额、事由和卡时，同键改了是否销户或柜台兑付凭证号的请求也拿到「已完成」，
    // 实际上卡没销户、新凭证号也没入审计。改过的请求不是同一次操作：409 IDEMPOTENCY_KEY_CONFLICT，不带标记，余额与卡状态不变
    @Test
    void aStoredValueRefundResubmittedWithTheSameKeyButAChangedCloseAccountOrVoucherIsRejectedWithoutTheCompletedMark()
            throws Exception {
        // 线上退款：第一次退光余额、不销户
        String onlineCustomer = customer("onx");
        card(onlineCustomer, 5000L, "platform_collect");
        String onlinePath = "/api/v1/loyalty/stored-value/" + onlineCustomer + "/refund";
        String onlineKey = "b02-refund-close-" + suffix;
        String onlineFirst = body(write(onlinePath, refundJson(onlineKey, 5000L, null, false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.replayed").doesNotExist()));
        assertThat(balance(onlineCustomer)).isZero();
        assertThat(cardStatus(onlineCustomer)).isEqualTo("active");
        // 同键同正文照样按已完成返回：新增的核对不误伤真正的重复提交
        assertCompletedStoredValueReplay(write(onlinePath, refundJson(onlineKey, 5000L, null, false)), onlineFirst);

        // 柜台退款：第一次带兑付凭证号 A
        String counterCustomer = customer("ctx");
        card(counterCustomer, 8000L, "merchant_prepaid");
        String counterPath = "/api/v1/loyalty/stored-value/" + counterCustomer + "/counter-refund";
        String counterKey = "b02-counter-voucher-" + suffix;
        String counterFirst = body(write(counterPath, refundJson(counterKey, 2000L, "VOUCHER-B02-A-" + suffix, false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.replayed").doesNotExist()));
        assertThat(balance(counterCustomer)).isEqualTo(6000L);
        assertCompletedStoredValueReplay(
                write(counterPath, refundJson(counterKey, 2000L, "VOUCHER-B02-A-" + suffix, false)), counterFirst);

        // 改过的请求放在最后：被拒的请求把测试事务标成只能回滚（生产每个请求各自一个事务，不受影响）。
        // 第二次还能过：同一改过的请求再提交一次，仍然 409、不带标记
        for (int attempt = 0; attempt < 2; attempt++) {
            // 同键改成要求销户：第一次没销户，卡仍在用
            write(onlinePath, refundJson(onlineKey, 5000L, null, true))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail_code").value("IDEMPOTENCY_KEY_CONFLICT"))
                    .andExpect(jsonPath("$.replayed").doesNotExist());
            // 柜台同键改兑付凭证号：新凭证号没有办过兑付
            write(counterPath, refundJson(counterKey, 2000L, "VOUCHER-B02-B-" + suffix, false))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail_code").value("IDEMPOTENCY_KEY_CONFLICT"))
                    .andExpect(jsonPath("$.replayed").doesNotExist());
        }
        assertThat(balance(onlineCustomer)).isZero();
        assertThat(cardStatus(onlineCustomer)).isEqualTo("active");
        assertThat(balance(counterCustomer)).isEqualTo(6000L);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM audit_events
                 WHERE event_name = 'stored_value.counter_refund' AND idempotency_key = ?
                """, Integer.class, "svcr_" + counterKey)).isEqualTo(1);
    }

    // M724-F10（治理地图第 528 行红线④「看不到别的商户和别的项目的数据」；枚举速查第 86 行「商户员工只能管理本商户发行的储值卡」）：
    // 商户甲的员工退款成功之后，商户乙持退款权限的员工拿同一客户、同一张卡、原提交编号、原请求内容提交 → 403 CROSS_MERCHANT_FORBIDDEN
    // （与首次退款的范围校验同一错误码），响应里没有那笔退款的流水号、金额、余额，不提示已完成、不带 replayed；乙同号改请求内容、
    // 乙换新编号同样被拒，退款流水条数与余额不变；甲本人同号同内容重复提交仍返回第一次的流水并提示已完成。线上退款与柜台退款各走一遍。
    // 修复前乙的同号同内容提交 200：拿到甲那笔退款的流水、金额与余额，并提示已完成；乙同号改内容 409 IDEMPOTENCY_KEY_CONFLICT。
    @Test
    void anotherMerchantsStaffResubmittingAStoredValueRefundIsRejectedBeforeAnyFirstResultIsReturned() throws Exception {
        otherMerchantStaff(false);
        String onlineCustomer = customer("onf10");
        card(onlineCustomer, 9000L, "platform_collect");
        String onlinePath = "/api/v1/loyalty/stored-value/" + onlineCustomer + "/refund";
        String onlineKey = "b02-f10-refund-" + suffix;
        String onlineBody = refundJson(onlineKey, 3000L, null);
        String onlineFirst = body(write(onlinePath, onlineBody)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.replayed").doesNotExist()));
        String counterCustomer = customer("ctf10");
        card(counterCustomer, 8000L, "merchant_prepaid");
        String counterPath = "/api/v1/loyalty/stored-value/" + counterCustomer + "/counter-refund";
        String counterKey = "b02-f10-counter-" + suffix;
        String counterVoucher = "VOUCHER-F10-" + suffix;
        String counterBody = refundJson(counterKey, 2000L, counterVoucher);
        String counterFirst = body(write(counterPath, counterBody)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.replayed").doesNotExist()));

        // ③ 甲本人同号同内容重复提交：仍返回第一次的流水并提示已完成（新增的范围核对不误伤本人）
        assertCompletedStoredValueReplay(write(onlinePath, onlineBody), onlineFirst);
        assertCompletedStoredValueReplay(write(counterPath, counterBody), counterFirst);

        // 被拒的请求放在最后：失败的请求把测试事务标成只能回滚（生产每个请求各自一个事务，不受影响）
        // ① 乙同号同内容：按范围被拒，拿不到第一次的流水
        assertRejectedAsOutOfScope(write(otherUserId, otherMerchantId, onlinePath, onlineBody), onlineFirst);
        assertRejectedAsOutOfScope(write(otherUserId, otherMerchantId, counterPath, counterBody), counterFirst);
        // ② 乙同号改请求内容（金额）、乙换新编号：同样被拒
        assertRejectedAsOutOfScope(write(otherUserId, otherMerchantId, onlinePath, refundJson(onlineKey, 1000L, null)), onlineFirst);
        assertRejectedAsOutOfScope(write(otherUserId, otherMerchantId, onlinePath,
                refundJson("b02-f10-refund-new-" + suffix, 3000L, null)), onlineFirst);
        assertRejectedAsOutOfScope(write(otherUserId, otherMerchantId, counterPath,
                refundJson(counterKey, 1000L, counterVoucher)), counterFirst);
        assertRejectedAsOutOfScope(write(otherUserId, otherMerchantId, counterPath,
                refundJson("b02-f10-counter-new-" + suffix, 2000L, counterVoucher)), counterFirst);
        assertThat(refunds(onlineCustomer)).isEqualTo(1);
        assertThat(balance(onlineCustomer)).isEqualTo(6000L);
        assertThat(refunds(counterCustomer)).isEqualTo(1);
        assertThat(balance(counterCustomer)).isEqualTo(6000L);
    }

    // M724-F10（治理地图第 656 行「操作被拒绝或失败的不算用掉」）：乙持的是单次退款授权时，被拒的那一次不算用掉——来源仍未消耗、没有消耗日志。
    // 柜台退款入口。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void anotherMerchantsStaffHoldingASingleUseRefundGrantIsRejectedAndKeepsTheGrant() throws Exception {
        outOfScopeStaffHoldingOnlyASingleUseRefundGrantIsRejectedAndKeepsTheGrant(false, true);
    }

    // M724-F10 补覆盖（验收「线上退款与柜台退款两个入口各覆盖」）：同上，线上退款入口。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void anotherMerchantsStaffHoldingASingleUseRefundGrantIsRejectedOnTheOnlineRefundAndKeepsTheGrant() throws Exception {
        outOfScopeStaffHoldingOnlyASingleUseRefundGrantIsRejectedAndKeepsTheGrant(false, false);
    }

    // M724-F15（治理地图第 528 行红线④「看不到别的商户和别的项目的数据」；第 653 行「数据的项目与商户隔离不提供关闭开关」）：
    // 项目甲、商户 M 的员工对本项目本商户的储值卡退款成功之后，同商户 M、项目乙持退款权限的员工拿同一客户、原项目与商户参数、原提交编号、
    // 原请求内容提交 → 403 CROSS_PROJECT_FORBIDDEN，响应里没有那笔退款的流水号、金额、余额，不提示已完成、不带 replayed；退款流水条数与
    // 余额不变；甲本人同号同内容重复提交仍返回第一次的流水并提示已完成。线上退款与柜台退款各走一遍。
    // 修复前乙的同号同内容提交 200：商户相同、过了只比商户的范围核对，拿到甲那笔退款的流水、金额与余额，并提示已完成。
    @Test
    void staffOfAnotherProjectInTheSameMerchantResubmittingAStoredValueRefundIsRejectedBeforeAnyFirstResultIsReturned()
            throws Exception {
        otherProjectStaff(false);
        String onlineCustomer = customer("onf15");
        card(onlineCustomer, 9000L, "platform_collect", "org");
        String onlinePath = "/api/v1/loyalty/stored-value/" + onlineCustomer + "/refund";
        String onlineBody = refundJson("b02-f15-refund-" + suffix, 3000L, null, null, "org");
        String onlineFirst = body(write(onlinePath, onlineBody)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.replayed").doesNotExist()));
        String counterCustomer = customer("ctf15");
        card(counterCustomer, 8000L, "merchant_prepaid", "org");
        String counterPath = "/api/v1/loyalty/stored-value/" + counterCustomer + "/counter-refund";
        String counterBody = refundJson("b02-f15-counter-" + suffix, 2000L, "VOUCHER-F15-" + suffix, null, "org");
        String counterFirst = body(write(counterPath, counterBody)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.replayed").doesNotExist()));

        // ③ 项目甲商户 M 的本人同号同内容重复提交：仍返回第一次的流水并提示已完成（新增的项目核对不误伤本人）
        assertCompletedStoredValueReplay(write(onlinePath, onlineBody), onlineFirst);
        assertCompletedStoredValueReplay(write(counterPath, counterBody), counterFirst);

        // 被拒的请求放在最后：失败的请求把测试事务标成只能回滚（生产每个请求各自一个事务，不受影响）
        // ① 同商户、项目乙的员工同号同内容：按项目被拒，拿不到第一次的流水
        assertRejectedAsOutOfScope(write(otherUserId, otherProjectId, merchantId, onlinePath, onlineBody), onlineFirst,
                "CROSS_PROJECT_FORBIDDEN");
        assertRejectedAsOutOfScope(write(otherUserId, otherProjectId, merchantId, counterPath, counterBody), counterFirst,
                "CROSS_PROJECT_FORBIDDEN");
        assertThat(refunds(onlineCustomer)).isEqualTo(1);
        assertThat(balance(onlineCustomer)).isEqualTo(6000L);
        assertThat(refunds(counterCustomer)).isEqualTo(1);
        assertThat(balance(counterCustomer)).isEqualTo(6000L);
    }

    // M724-F15 ②：项目乙的员工只持单次退款授权时同样被拒，其单次来源未消耗、没有消耗日志。线上退款入口。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void staffOfAnotherProjectHoldingASingleUseRefundGrantIsRejectedOnTheOnlineRefundAndKeepsTheGrant() throws Exception {
        outOfScopeStaffHoldingOnlyASingleUseRefundGrantIsRejectedAndKeepsTheGrant(true, false);
    }

    // M724-F15 ②：同上，柜台退款入口。
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void staffOfAnotherProjectHoldingASingleUseRefundGrantIsRejectedOnTheCounterRefundAndKeepsTheGrant() throws Exception {
        outOfScopeStaffHoldingOnlyASingleUseRefundGrantIsRejectedAndKeepsTheGrant(true, true);
    }

    /**
     * 越范围的员工乙只持单次退款授权，拿甲那笔成功退款的原提交编号、原请求内容重提：被拒，乙的单次来源仍未消耗、没有消耗日志，
     * 退款流水条数与余额不变（治理地图第 656 行「操作被拒绝或失败的不算用掉」）。
     * {@code otherProject} 为假＝乙是同项目、另一个商户的人，卡不带项目（M724-F10，403 CROSS_MERCHANT_FORBIDDEN）；
     * 为真＝乙是同商户、另一个项目的人，卡带甲的项目（M724-F15，403 CROSS_PROJECT_FORBIDDEN）。{@code counter} 为真走柜台退款，否则走线上退款。
     *
     * <p>调用它的用例不开测试事务：单次消耗随请求事务回滚，要真实提交才看得见（开着测试事务时消耗并入测试事务、不随请求回滚）。
     * 储值流水只追加不删（V140），客人、卡与流水按后缀隔离留在库里；线上退款登记的财务退款待办、两名员工及其授权（含 seed 建的）结束时删除。
     */
    private void outOfScopeStaffHoldingOnlyASingleUseRefundGrantIsRejectedAndKeepsTheGrant(boolean otherProject, boolean counter)
            throws Exception {
        String tag = (otherProject ? "f15" : "f10") + (counter ? "ct" : "on");
        String customerId = null;
        try {
            if (otherProject) {
                otherProjectStaff(true);
            } else {
                otherMerchantStaff(true);
            }
            customerId = customer("su" + tag);
            // 线上退款只退平台代收的卡，商户预收的卡只能走柜台退款
            card(customerId, 8000L, counter ? "merchant_prepaid" : "platform_collect", otherProject ? "org" : null);
            String path = "/api/v1/loyalty/stored-value/" + customerId + (counter ? "/counter-refund" : "/refund");
            String body = refundJson("b02-" + tag + "-single-" + suffix, 2000L,
                    counter ? "VOUCHER-" + tag + "-" + suffix : null, null, otherProject ? "org" : null);
            String first = body(write(path, body)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("success")));

            assertRejectedAsOutOfScope(
                    otherProject ? write(otherUserId, otherProjectId, merchantId, path, body)
                            : write(otherUserId, otherMerchantId, path, body),
                    first, otherProject ? "CROSS_PROJECT_FORBIDDEN" : "CROSS_MERCHANT_FORBIDDEN");
            assertThat(jdbcTemplate.queryForList("SELECT consumed FROM temporary_permission_grants WHERE subject_id = ?",
                    Boolean.class, otherUserId)).containsExactly(false);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM permission_template_usage_logs WHERE user_id = ? AND result = 'consumed'",
                    Integer.class, otherUserId)).isZero();
            assertThat(refunds(customerId)).isEqualTo(1);
            assertThat(balance(customerId)).isEqualTo(6000L);
        } finally {
            jdbcTemplate.update("DELETE FROM finance_refunds WHERE target_id IN "
                    + "(SELECT id FROM customer_stored_value_accounts WHERE customer_id = ?)", customerId);
            jdbcTemplate.update("DELETE FROM temporary_permission_grants WHERE subject_id IN (?, ?)", userId, otherUserId);
            jdbcTemplate.update("DELETE FROM access_users WHERE id IN (?, ?)", userId, otherUserId);
        }
    }

    // M724-F10：按范围被拒——403 CROSS_MERCHANT_FORBIDDEN，响应里没有第一次那笔退款的流水号、金额、余额，不提示已完成、不带 replayed
    private void assertRejectedAsOutOfScope(ResultActions rejected, String first) throws Exception {
        assertRejectedAsOutOfScope(rejected, first, "CROSS_MERCHANT_FORBIDDEN");
    }

    // M724-F10、M724-F15：按范围被拒——403、给定的细码（越商户 CROSS_MERCHANT_FORBIDDEN、越项目 CROSS_PROJECT_FORBIDDEN），其余同上
    private void assertRejectedAsOutOfScope(ResultActions rejected, String first, String detailCode) throws Exception {
        String response = body(rejected
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value(detailCode))
                .andExpect(jsonPath("$.replayed").doesNotExist()));
        JsonNode envelope = new ObjectMapper().readTree(response);
        assertThat(envelope.get("message").asText()).isNotEqualTo(COMPLETED);
        assertThat(envelope.get("data") == null || envelope.get("data").isNull()).as("被拒的响应不带业务结果").isTrue();
        assertThat(response).doesNotContain(data(first).get("id").asText())
                .doesNotContain("amount_cent").doesNotContain("balance_after_cent");
    }

    private void assertCompletedReplay(ResultActions replay, String first) throws Exception {
        String replayed = body(replay
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value(COMPLETED))
                .andExpect(jsonPath("$.replayed").value(true)));
        assertThat(data(replayed)).isEqualTo(data(first));
    }

    // 储值流水的重放从库里读回：时间精度与第一次内存里的记录可能不同，只比流水号、类型、金额与余额
    private void assertCompletedStoredValueReplay(ResultActions replay, String first) throws Exception {
        JsonNode firstData = data(first);
        replay.andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(COMPLETED))
                .andExpect(jsonPath("$.replayed").value(true))
                .andExpect(jsonPath("$.data.id").value(firstData.get("id").asText()))
                .andExpect(jsonPath("$.data.txn_type").value("refund"))
                .andExpect(jsonPath("$.data.amount_cent").value(firstData.get("amount_cent").asLong()))
                .andExpect(jsonPath("$.data.balance_after_cent").value(firstData.get("balance_after_cent").asLong()));
    }

    private ResultActions write(String path, String body) throws Exception {
        return write(userId, merchantId, path, body);
    }

    private ResultActions write(String user, String merchant, String path, String body) throws Exception {
        return write(user, "org", merchant, path, body);
    }

    private ResultActions write(String user, String project, String merchant, String path, String body) throws Exception {
        return mockMvc.perform(post(path)
                .with(request -> {
                    request.addHeader("X-Terminal-Type", "web");
                    return authentication(new UsernamePasswordAuthenticationToken(
                            new AccessActor(user, user, "staff", project, merchant, "active"), null,
                            List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    // M724-F10：商户乙的员工（同项目、另一个商户），持储值退款权限——常规授权或单次授权
    private void otherMerchantStaff(boolean singleUse) {
        otherStaff("org", otherMerchantId, singleUse, "M724-F10 另一商户的退款权限");
    }

    // M724-F15：同商户、另一个项目的员工，持储值退款权限——常规授权或单次授权
    private void otherProjectStaff(boolean singleUse) {
        otherStaff(otherProjectId, merchantId, singleUse, "M724-F15 同商户另一项目的退款权限");
    }

    private void otherStaff(String project, String merchant, boolean singleUse, String grantReason) {
        Instant now = Instant.now();
        accessRepository.createUser(new AccessUser(otherUserId, otherUserId, "staff", project, merchant, "active", now));
        accessRepository.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), otherUserId,
                List.of("customer.stored_value.refund"), now.minusSeconds(3600), now.plusSeconds(86_400), singleUse, false,
                "active", grantReason, "seed-admin", now, null, null));
    }

    private Integer refunds(String customerId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM customer_stored_value_transactions WHERE customer_id = ? AND txn_type = 'refund'",
                Integer.class, customerId);
    }

    private String customer(String tag) {
        String id = "cus_b02_" + tag + "_" + suffix;
        String phone = "139" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        customerRepository.saveCustomer(new CustomerRecord(
                id, phone, "139****" + phone.substring(7), "active", null, null, Instant.now(), Instant.now()));
        return id;
    }

    private void card(String customerId, long balanceCent, String fundOwnership) {
        card(customerId, balanceCent, fundOwnership, null);
    }

    // 本类商户发的卡；projectId 为空＝不带项目的商户卡，非空＝该项目本商户的卡（M724-F15）
    private void card(String customerId, long balanceCent, String fundOwnership, String projectId) {
        customerRepository.saveStoredValueAccount(new CustomerStoredValueAccountRecord(
                "svacc_" + UUID.randomUUID().toString().replace("-", ""), customerId, projectId, merchantId, balanceCent,
                "active", Instant.now(), Instant.now(), fundOwnership));
    }

    private long balance(String customerId) {
        return jdbcTemplate.queryForObject(
                "SELECT balance_cent FROM customer_stored_value_accounts WHERE customer_id = ? AND merchant_id = ?",
                Long.class, customerId, merchantId);
    }

    private String cardStatus(String customerId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM customer_stored_value_accounts WHERE customer_id = ? AND merchant_id = ?",
                String.class, customerId, merchantId);
    }

    private Integer published(String idempotencyKey) {
        return jdbcTemplate.queryForObject("""
                SELECT count(*) FROM audit_events
                 WHERE event_name = 'ticket_dynamic_pricing.published.success' AND event_result = 'success' AND idempotency_key = ?
                """, Integer.class, idempotencyKey);
    }

    private Integer redemptions(String instanceId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM voucher_redemption_records WHERE voucher_instance_id = ?", Integer.class, instanceId);
    }

    private String refundJson(String key, long amountCent, String redemptionVoucherNo) {
        return refundJson(key, amountCent, redemptionVoucherNo, null);
    }

    private String refundJson(String key, long amountCent, String redemptionVoucherNo, Boolean closeAccount) {
        return refundJson(key, amountCent, redemptionVoucherNo, closeAccount, null);
    }

    private String refundJson(String key, long amountCent, String redemptionVoucherNo, Boolean closeAccount, String projectId) {
        return """
                {"idempotency_key": "%s", "merchant_id": "%s", "amount_cent": %d, "reason": "已完成提示回归退款"%s%s%s}
                """.formatted(key, merchantId, amountCent,
                redemptionVoucherNo == null ? "" : ", \"redemption_voucher_no\": \"" + redemptionVoucherNo + "\"",
                closeAccount == null ? "" : ", \"close_account\": " + closeAccount,
                projectId == null ? "" : ", \"project_id\": \"" + projectId + "\"");
    }

    private static String redeemJson(String key, String instanceId) {
        return """
                {"idempotency_key": "%s", "voucher_instance_id": "%s", "note": "已完成提示回归核销"}
                """.formatted(key, instanceId);
    }

    private static String body(ResultActions actions) throws Exception {
        return actions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    // 响应包络的 request_id / server_time 每次不同，比较第一次与重放的业务结果只看 data
    private static JsonNode data(String body) throws Exception {
        return new ObjectMapper().readTree(body).get("data");
    }
}
