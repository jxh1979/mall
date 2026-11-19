package com.complex.platform.customer;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BC 段第 365 条 365-R1（BSM:833「账户可冻结/解冻。」「商户的员工只能管理本商户发行的储值卡。」；ENUM:86「综合体通用卡：……余额管理与退款均由
 * 长沙项目公司负责」）：储值账户冻结、解冻按退卡范围口径（{@link StoredValueRefundLookupService#requireRefundScope}），退卡查卡持退卡码或冻结码任一即可。
 * 真库、真实入口、真实授权链、蛇形键。修复前：不挂商户的项目侧账号与最高权限冻结通用卡 403 MERCHANT_SCOPE_REQUIRED；只持冻结码的账号查卡 403 PERMISSION_DENIED。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class StoredValueFreezeScopeJdbcIntegrationTests {

    private static final String FREEZE = "customer.stored_value.freeze";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CustomerRepository customerRepository;

    private final String s = Long.toString(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    private final String projectA = "r365prj_a_" + s;
    private final String projectB = "r365prj_b_" + s;
    private final String merchant1 = "r365mch_1_" + s;
    private final String merchant2 = "r365mch_2_" + s;
    private StaffRequestFixture staffs;
    private String customer;
    private String phone;
    private String generalA;
    private String generalB;
    private String card1;
    private String card2;

    @BeforeEach
    void seed() {
        for (String project : List.of(projectA, projectB)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex',?,'active',now(),now())",
                    project, "储值冻结回归综合体" + project);
        }
        for (String merchant : List.of(merchant1, merchant2)) {
            jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,?,'bar','active',?,now(),now())",
                    merchant, "储值冻结回归酒吧" + merchant, projectA);
        }
        customer = "r365cus_" + s;
        phone = "139" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        customerRepository.saveCustomer(new CustomerRecord(customer, phone, "139****" + phone.substring(7), "active", null, null,
                Instant.now(), Instant.now()));
        generalA = card(projectA, null);
        generalB = card(projectB, null);
        card1 = card(projectA, merchant1);
        card2 = card(projectA, merchant2);
        staffs = new StaffRequestFixture(jdbc);
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    /** ① 不挂商户的项目 A 账号冻结、解冻 A 的通用卡 200；② 冻结 B 的通用卡 403 CROSS_PROJECT_FORBIDDEN，卡仍有效。 */
    @Test
    void projectSideStaffFreezesOwnProjectsGeneralCardOnly() throws Exception {
        RequestPostProcessor staffA = staffs.staff("r365usr_pa_" + s, "staff", projectA, null, FREEZE);

        post(staffA, "freeze", projectA, null).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("frozen"));
        assertThat(cardStatus(generalA)).isEqualTo("frozen");
        post(staffA, "unfreeze", projectA, null).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("active"));
        assertThat(cardStatus(generalA)).isEqualTo("active");

        post(staffA, "freeze", projectB, null).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(cardStatus(generalB)).isEqualTo("active");
    }

    /** ③ 商户 1 账号冻结本商户卡 200，冻结商户 2 的卡 403 CROSS_MERCHANT_FORBIDDEN；④ 最高权限冻结 B 的通用卡 200。 */
    @Test
    void merchantStaffFreezesOwnCardsAndMasterFreezesAnyCard() throws Exception {
        post(master(), "freeze", projectB, null).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("frozen"));
        assertThat(cardStatus(generalB)).isEqualTo("frozen");

        RequestPostProcessor staff1 = staffs.staff("r365usr_m1_" + s, "staff", projectA, merchant1, FREEZE);
        post(staff1, "freeze", projectA, merchant1).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("frozen"));
        post(staff1, "freeze", projectA, merchant2).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        assertThat(cardStatus(card2)).isEqualTo("active");
    }

    /** ⑤ 只持冻结码的项目 A 账号查卡 200、列出本项目通用卡；两个码都不持的 403 PERMISSION_DENIED。 */
    @Test
    void freezeHolderCanLookUpCards() throws Exception {
        RequestPostProcessor freezer = staffs.staff("r365usr_fz_" + s, "staff", projectA, null, FREEZE);
        List<String> ids = JsonPath.read(lookup(freezer).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data[*].account_id");
        assertThat(ids).contains(generalA).doesNotContain(generalB);

        RequestPostProcessor neither = staffs.staff("r365usr_no_" + s, "staff", projectA, null, "customer.account.manage");
        lookup(neither).andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
    }

    // ---------- 夹具与请求 ----------

    private String card(String projectId, String merchantId) {
        String id = "svacc_" + UUID.randomUUID().toString().replace("-", "");
        customerRepository.saveStoredValueAccount(new CustomerStoredValueAccountRecord(id, customer, projectId, merchantId, 5000L, "active",
                Instant.now(), Instant.now(), merchantId == null ? null : "merchant_prepaid", 5000L, 0L));
        return id;
    }

    private ResultActions post(RequestPostProcessor who, String action, String projectId, String merchantId) throws Exception {
        String body = "{\"project_id\":\"" + projectId + "\"" + (merchantId == null ? "" : ",\"merchant_id\":\"" + merchantId + "\"")
                + ",\"reason\":\"客人报失\"}";
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/loyalty/stored-value/" + customer + "/" + action).with(who)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions lookup(RequestPostProcessor who) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/loyalty/stored-value/refund-cards/lookup").with(who)
                .contentType(MediaType.APPLICATION_JSON).content("{\"phone\":\"" + phone + "\"}"));
    }

    private String cardStatus(String accountId) {
        return jdbc.queryForObject("SELECT status FROM customer_stored_value_accounts WHERE id = ?", String.class, accountId);
    }

    /** 最高权限本人（保留超管账号，组织 platform-company、无商户），经后台 Web 终端。 */
    private static RequestPostProcessor master() {
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
