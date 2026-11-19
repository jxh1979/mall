package com.complex.platform.customer;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.TemporaryGrant;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AZ 段第 278 条 278-R1（ENUM:86「综合体通用卡：……余额管理与退款均由长沙项目公司负责」；BSM:833「商户的员工只能管理本商户发行的储值卡」
 * 「单张储值卡退款时如申请同时销卡，本金未退清的须拒绝销卡并明确报错」；PGM:528「④看不到别的商户和别的项目的数据」）：
 * 退卡查卡按范围只列可办理的卡；不挂商户的项目侧账号可办本项目通用卡；柜台退款首次提交核项目；申请销卡而本金未退清报错；查卡不扣单次授权。
 *
 * <p>真实库、真实接口、蛇形键；本类开测试事务、结束回滚，写请求带 {@code X-Terminal-Type: web}。被拒的请求放在各用例最后
 * （失败的请求把测试事务标成只能回滚；生产每个请求各自一个事务，不受影响）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class StoredValueRefundLookupJdbcIntegrationTests {

    private static final String LOOKUP = "/api/v1/loyalty/stored-value/refund-cards/lookup";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AccessRepository accessRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StoredValueService storedValueService;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String projectA = "org_b72sva_" + suffix;
    private final String projectB = "org_b72svb_" + suffix;
    private final String merchantM = "mch_b72svm_" + suffix;
    private final String merchantN = "mch_b72svn_" + suffix;

    private record Guest(String id, String phone, String maskedPhone) {
    }

    @AfterEach
    void clearActor() {
        SecurityContextHolder.clearContext();
    }

    // ① 商户员工查卡只见本商户的卡：不含通用卡与别家商户卡；本金、赠送金与库内一致，退款比例取发卡商户规则；响应不带明文手机号
    @Test
    void aMerchantStaffLookupListsOnlyTheirOwnMerchantsCards() throws Exception {
        String staff = staff("m1", projectA, merchantM, false);
        Guest guest = guest("one");
        card(guest.id(), projectA, null, 5000L, 500L, null);
        String own = card(guest.id(), projectA, merchantM, 4000L, 300L, "merchant_prepaid");
        card(guest.id(), null, merchantN, 2000L, 0L, "merchant_prepaid");
        rule(merchantM, 8000);

        String response = body(lookup(staff, projectA, merchantM, guest.phone())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].account_id").value(own))
                .andExpect(jsonPath("$.data[0].customer_id").value(guest.id()))
                .andExpect(jsonPath("$.data[0].masked_phone").value(guest.maskedPhone()))
                .andExpect(jsonPath("$.data[0].project_id").value(projectA))
                .andExpect(jsonPath("$.data[0].merchant_id").value(merchantM))
                .andExpect(jsonPath("$.data[0].principal_cent").value(4000))
                .andExpect(jsonPath("$.data[0].bonus_cent").value(300))
                .andExpect(jsonPath("$.data[0].status").value("active"))
                .andExpect(jsonPath("$.data[0].refund_allowed").value(true))
                .andExpect(jsonPath("$.data[0].refund_ratio_bps").value(8000)));
        assertThat(response).doesNotContain(guest.phone());
    }

    // ② 不挂商户、所属项目甲的员工查到项目甲通用卡，一次退还全部本金 → 200，卡关闭，财务退款台账出现待退（修复前 403 MERCHANT_SCOPE_REQUIRED）
    @Test
    void aProjectSideStaffLooksUpAndRefundsTheGeneralCardOfTheirProject() throws Exception {
        String staff = staff("pa", projectA, null, false);
        Guest guest = guest("two");
        String general = card(guest.id(), projectA, null, 5000L, 500L, null);

        assertThat(accountIds(lookup(staff, projectA, null, guest.phone()).andExpect(status().isOk()))).contains(general);
        write(staff, projectA, null, refundPath(guest), refundJson("b72sv-two-" + suffix, projectA, null, 5000L, true, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.txn_type").value("refund"))
                .andExpect(jsonPath("$.data.amount_cent").value(-5000));
        assertThat(cardStatus(general)).isEqualTo("closed");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM finance_refunds
                 WHERE target_id = ? AND source_type = 'stored_value_account' AND status = 'requested'
                   AND refund_amount_cent = 5000 AND project_id = ?
                """, Integer.class, general, projectA)).isEqualTo(1);
    }

    // ③ 不挂商户、所属项目乙的员工查不到项目甲通用卡；以项目甲首次提交退款 → 403 CROSS_PROJECT_FORBIDDEN，余额不变（修复前 403 MERCHANT_SCOPE_REQUIRED）
    @Test
    void aProjectSideStaffOfAnotherProjectNeitherSeesNorRefundsTheGeneralCard() throws Exception {
        String staff = staff("pb", projectB, null, false);
        Guest guest = guest("three");
        String general = card(guest.id(), projectA, null, 5000L, 0L, null);

        assertThat(accountIds(lookup(staff, projectB, null, guest.phone()).andExpect(status().isOk()))).doesNotContain(general);
        write(staff, projectB, null, refundPath(guest), refundJson("b72sv-three-" + suffix, projectA, null, 5000L, true, null))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(principal(general)).isEqualTo(5000L);
    }

    // ④ 同商户、项目乙的商户员工换新编号对带项目甲的商户卡柜台退款 → 403 CROSS_PROJECT_FORBIDDEN，余额不变（修复前 200）
    @Test
    void aMerchantStaffOfAnotherProjectCannotCounterRefundAProjectBoundMerchantCard() throws Exception {
        String staff = staff("mb", projectB, merchantM, false);
        Guest guest = guest("four");
        String card = card(guest.id(), projectA, merchantM, 4000L, 0L, "merchant_prepaid");

        write(staff, projectB, merchantM, counterPath(guest),
                refundJson("b72sv-four-" + suffix, projectA, merchantM, 1000L, false, "VCH-B72-4-" + suffix))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(principal(card)).isEqualTo(4000L);
    }

    // ⑤ 商户卡柜台退款申请同时销卡、只退部分本金 → 409 STORED_VALUE_CLOSE_PRINCIPAL_REMAINING，余额与状态不变（修复前 200 且不销卡）
    @Test
    void closingACardWhilePrincipalRemainsIsRejected() throws Exception {
        String staff = staff("m5", projectA, merchantM, false);
        Guest guest = guest("five");
        String card = card(guest.id(), projectA, merchantM, 4000L, 200L, "merchant_prepaid");

        write(staff, projectA, merchantM, counterPath(guest),
                refundJson("b72sv-five-" + suffix, projectA, merchantM, 1000L, true, "VCH-B72-5-" + suffix))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("STORED_VALUE_CLOSE_PRINCIPAL_REMAINING"));
        assertThat(principal(card)).isEqualTo(4000L);
        assertThat(cardStatus(card)).isEqualTo("active");
    }

    // ⑥ 账户关闭登记了待退事项的商户卡：查卡返回冻结状态与事项金额（按发卡商户规则 80% 登记 3200 分）
    @Test
    void aMerchantCardWithAnAccountClosurePendingRefundShowsTheItemAmount() throws Exception {
        String staff = staff("m6", projectA, merchantM, false);
        Guest guest = guest("six");
        String card = card(guest.id(), null, merchantM, 4000L, 0L, "merchant_prepaid");
        rule(merchantM, 8000);
        SecurityContextHolder.getContext().setAuthentication(token(staff, projectA, merchantM));
        storedValueService.closeCardsOnAccountClosure(guest.id(), "账户关闭");
        SecurityContextHolder.clearContext();

        lookup(staff, projectA, merchantM, guest.phone())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].account_id").value(card))
                .andExpect(jsonPath("$.data[0].status").value("frozen"))
                .andExpect(jsonPath("$.data[0].principal_cent").value(4000))
                .andExpect(jsonPath("$.data[0].pending_refund_cent").value(3200));
    }

    // ⑦ 只持单次储值退款授权的项目侧员工：先查卡（不扣单次授权）再退卡 → 200，退卡才用掉授权（操作员 2026-10-03「预览、查询不扣一次性授权」）
    @Test
    void aLookupDoesNotConsumeASingleUseRefundGrant() throws Exception {
        String staff = staff("su", projectA, null, true);
        Guest guest = guest("seven");
        String general = card(guest.id(), projectA, null, 3000L, 0L, null);

        assertThat(accountIds(lookup(staff, projectA, null, guest.phone()).andExpect(status().isOk()))).contains(general);
        assertThat(grantConsumed(staff)).isFalse();
        write(staff, projectA, null, refundPath(guest), refundJson("b72sv-seven-" + suffix, projectA, null, 3000L, true, null))
                .andExpect(status().isOk());
        assertThat(grantConsumed(staff)).isTrue();
        assertThat(cardStatus(general)).isEqualTo("closed");
    }

    // ⑧ 商户员工对通用卡线上退款仍 403 CROSS_MERCHANT_FORBIDDEN（商户员工只管本商户卡、不碰通用卡），余额不变
    @Test
    void aMerchantStaffStillCannotRefundAGeneralCard() throws Exception {
        String staff = staff("m8", projectA, merchantM, false);
        Guest guest = guest("eight");
        String general = card(guest.id(), projectA, null, 3000L, 0L, null);

        write(staff, projectA, merchantM, refundPath(guest), refundJson("b72sv-eight-" + suffix, projectA, null, 3000L, true, null))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        assertThat(principal(general)).isEqualTo(3000L);
    }

    // ⑨ 18AZ 收尾复核（PGM:528「④看不到别的商户和别的项目的数据」；BSM:1673「所有数据默认按项目隔离」）：发卡商户只在项目甲经营、卡不带项目，
    // 项目乙不挂商户的员工查不到（修复前查得到）；商户另有已批准的经营项目乙后查得到（同 FinanceService.recordStoredValuePayoutRefund 的经营项目口径）
    @Test
    void aProjectSideStaffSeesAProjectlessMerchantCardOnlyWhereTheIssuingMerchantOperates() throws Exception {
        for (String project : List.of(projectA, projectB)) {
            jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                    project, "储值退卡综合体" + project);
        }
        jdbcTemplate.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) "
                + "VALUES (?, ?, 'bar', ?, 'active', now(), now())", merchantM, "储值退卡酒吧" + suffix, projectA);
        String staff = staff("p9", projectB, null, false);
        Guest guest = guest("nine");
        String card = card(guest.id(), null, merchantM, 4000L, 0L, "merchant_prepaid");

        assertThat(accountIds(lookup(staff, projectB, null, guest.phone()).andExpect(status().isOk()))).doesNotContain(card);
        jdbcTemplate.update("INSERT INTO merchant_projects (merchant_id, project_id) VALUES (?, ?)", merchantM, projectB);
        assertThat(accountIds(lookup(staff, projectB, null, guest.phone()).andExpect(status().isOk()))).contains(card);
    }

    private String staff(String tag, String project, String merchant, boolean singleUse) {
        String userId = "usr_b72sv_" + tag + "_" + suffix;
        Instant now = Instant.now();
        accessRepository.createUser(new AccessUser(userId, userId, "staff", project, merchant, "active", now));
        accessRepository.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), userId,
                List.of("customer.stored_value.refund"), now.minusSeconds(3600), now.plusSeconds(86_400), singleUse, false,
                "active", "储值退卡回归", "seed-admin", now, null, null));
        return userId;
    }

    private Guest guest(String tag) {
        String id = "cus_b72sv_" + tag + "_" + suffix;
        String phone = "139" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        String masked = "139****" + phone.substring(7);
        customerRepository.saveCustomer(new CustomerRecord(id, phone, masked, "active", null, null, Instant.now(), Instant.now()));
        return new Guest(id, phone, masked);
    }

    private String card(String customerId, String projectId, String merchantId, long principalCent, long bonusCent,
            String fundOwnership) {
        String id = "svacc_" + UUID.randomUUID().toString().replace("-", "");
        customerRepository.saveStoredValueAccount(new CustomerStoredValueAccountRecord(id, customerId, projectId, merchantId,
                principalCent + bonusCent, "active", Instant.now(), Instant.now(), fundOwnership, principalCent, bonusCent));
        return id;
    }

    private void rule(String merchantId, int refundRatioBps) {
        customerRepository.saveStoredValueMerchantRefundRule(new StoredValueMerchantRefundRuleRecord(
                merchantId, true, refundRatioBps, Instant.now(), Instant.now()));
    }

    private ResultActions lookup(String user, String project, String merchant, String phone) throws Exception {
        return write(user, project, merchant, LOOKUP, "{\"phone\": \"" + phone + "\"}");
    }

    private ResultActions write(String user, String project, String merchant, String path, String body) throws Exception {
        return mockMvc.perform(post(path)
                .with(request -> {
                    request.addHeader("X-Terminal-Type", "web");
                    return authentication(token(user, project, merchant)).postProcessRequest(request);
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static UsernamePasswordAuthenticationToken token(String user, String project, String merchant) {
        return new UsernamePasswordAuthenticationToken(new AccessActor(user, user, "staff", project, merchant, "active"), null,
                List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")));
    }

    private static String refundPath(Guest guest) {
        return "/api/v1/loyalty/stored-value/" + guest.id() + "/refund";
    }

    private static String counterPath(Guest guest) {
        return "/api/v1/loyalty/stored-value/" + guest.id() + "/counter-refund";
    }

    private static String refundJson(String key, String projectId, String merchantId, long amountCent, boolean closeAccount,
            String voucher) {
        return """
                {"idempotency_key": "%s", "project_id": "%s", "amount_cent": %d, "reason": "客人到店退卡", "close_account": %s%s%s}
                """.formatted(key, projectId, amountCent, closeAccount,
                merchantId == null ? "" : ", \"merchant_id\": \"" + merchantId + "\"",
                voucher == null ? "" : ", \"redemption_voucher_no\": \"" + voucher + "\"");
    }

    private static List<String> accountIds(ResultActions actions) throws Exception {
        List<String> ids = new ArrayList<>();
        new ObjectMapper().readTree(body(actions)).path("data").forEach(row -> ids.add(row.path("account_id").asText()));
        return ids;
    }

    private static String body(ResultActions actions) throws Exception {
        return actions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private long principal(String accountId) {
        return jdbcTemplate.queryForObject("SELECT principal_cent FROM customer_stored_value_accounts WHERE id = ?",
                Long.class, accountId);
    }

    private String cardStatus(String accountId) {
        return jdbcTemplate.queryForObject("SELECT status FROM customer_stored_value_accounts WHERE id = ?",
                String.class, accountId);
    }

    private boolean grantConsumed(String userId) {
        return jdbcTemplate.queryForObject("SELECT consumed FROM temporary_permission_grants WHERE subject_id = ?",
                Boolean.class, userId);
    }
}
