package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * ROOT-03-R28 学生认证真库、真接口（客人端 multipart 与蛇形键）。BSM 第 903 行「学生认证必须在本人注册会员后单独申请，认证通过只增加学生资格，
 * 不自动创建会员身份」；第 777 行「学生票要票务运营岗人工审核证件 + 学信网核验通过才可用」；第 890 行「采集证件……前先单独取得同意」；
 * 第十三批裁决第 1 条「有效到客人提供的学信网《学籍在线验证报告》上的预计毕业日期，审核人通过时按报告填写」。票价 5000 分，会员价 8 折、学生价 5 折。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "evidence.attachment.dir=/tmp/complex-student-cert-test"
})
@AutoConfigureMockMvc
@Transactional
class StudentCertificationJdbcIntegrationTests {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @Autowired private JdbcTicketingRepository repository;

    private CustomerTicketPaymentFixture f;
    private final Set<String> granted = new HashSet<>();

    @BeforeEach
    void seed() {
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 0L);
    }

    @Test
    void customerAppliesStaffApprovesStudentPriceThenApplies() throws Exception {
        rule("member", 80);
        String student = rule("student", 50);
        // 认证通过前：只有会员价。
        assertThat(order(f.createOrderId(1))).containsEntry("amount_cent", 4000L);
        String id = read(apply(consent("student_certificate", true)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.status_label").value("待审核")), "$.data.id");
        RequestPostProcessor reviewer = staff("usr_sc_rev_" + f.suffix, "ticket.student_cert.review");
        mockMvc.perform(get("/api/v1/student-certifications").param("status", "pending").with(reviewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == '" + id + "')].chsi_verification_code").value("CHSI1234567890AB"))
                .andExpect(jsonPath("$.data.items[?(@.id == '" + id + "')].attachment_count").value(1));
        mockMvc.perform(get("/api/v1/student-certifications/" + id + "/files/1").with(reviewer)).andExpect(status().isOk());
        String graduation = LocalDate.now().plusYears(2).toString();
        staffPost(reviewer, "/api/v1/student-certifications/" + id + "/approve", Map.of("valid_until", graduation))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("approved"))
                .andExpect(jsonPath("$.data.valid_until").value(graduation));
        // 只增加学生资格，不动会员身份。
        assertThat(jdbc.queryForObject("SELECT status FROM customers WHERE id = ?", String.class, f.customerId)).isEqualTo("active");
        assertThat(order(f.createOrderId(1))).containsEntry("amount_cent", 2500L).containsEntry("price_rule_id", student);
        mockMvc.perform(get("/api/v1/customer/student-certifications/current").header("X-Terminal-Type", "miniapp")
                        .header("X-Customer-Token", f.token))
                .andExpect(jsonPath("$.data.status_label").value("已通过"))
                .andExpect(jsonPath("$.data.valid_until").value(graduation));
    }

    @Test
    void certificatePhotoAndChsiCodeStoredEncryptedReviewerReadsOriginal() throws Exception {
        byte[] photo = "STUDENT-CARD-PHOTO-BYTES".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String id = read(apply(consent("student_certificate", true), photo).andExpect(status().isOk()), "$.data.id");
        // PGM 第 876 行「客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放」：库里与磁盘上都不留明文。
        Map<String, Object> row = jdbc.queryForMap("SELECT chsi_verification_code, attachment_keys ->> 0 AS object_key"
                + " FROM customer_student_certifications WHERE id = ?", id);
        assertThat((String) row.get("chsi_verification_code")).isNotEqualTo("CHSI1234567890AB").startsWith("enc:v1:");
        java.nio.file.Path stored = java.nio.file.Path.of("/tmp/complex-student-cert-test")
                .resolve(((String) row.get("object_key")).substring("evidence-files/".length()));
        String onDisk = java.nio.file.Files.readString(stored);
        assertThat(onDisk).startsWith("enc:v1:").doesNotContain("STUDENT-CARD-PHOTO-BYTES");
        // 审核人照常看到学信网验证码与证件照片原样。
        RequestPostProcessor reviewer = staff("usr_sc_enc_" + f.suffix, "ticket.student_cert.review");
        mockMvc.perform(get("/api/v1/student-certifications").param("status", "pending").with(reviewer))
                .andExpect(jsonPath("$.data.items[?(@.id == '" + id + "')].chsi_verification_code").value("CHSI1234567890AB"));
        byte[] served = mockMvc.perform(get("/api/v1/student-certifications/" + id + "/files/1").with(reviewer))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(served).isEqualTo(photo);
    }

    @Test
    void applyWithoutDocumentConsentRejected() throws Exception {
        apply(null).andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("CUSTOMER_CONSENT_REQUIRED"));
        apply(consent("marketing", true)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("CUSTOMER_CONSENT_REQUIRED"));
        apply(consent("student_certificate", false)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("CUSTOMER_CONSENT_REQUIRED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customer_student_certifications WHERE customer_id = ?", Integer.class,
                f.customerId)).isZero();
    }

    @Test
    void secondPendingRejected() throws Exception {
        String consentId = consent("student_certificate", true);
        apply(consentId).andExpect(status().isOk());
        apply(consentId).andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("STUDENT_CERT_PENDING"));
    }

    @Test
    void expiredCertificationNoLongerQualifies() throws Exception {
        rule("member", 80);
        rule("student", 50);
        jdbc.update("""
                INSERT INTO customer_student_certifications (id, customer_id, status, consent_id, valid_until, reviewed_by, reviewed_at,
                    created_at, updated_at)
                VALUES (?, ?, 'approved', 'consent_x', ?, 'usr_x', now(), now(), now())
                """, "scert_old_" + f.suffix, f.customerId, java.sql.Date.valueOf(LocalDate.now().minusDays(1)));
        assertThat(order(f.createOrderId(1))).containsEntry("amount_cent", 4000L);
    }

    @Test
    void reviewerWithoutPermission403() throws Exception {
        String id = read(apply(consent("student_certificate", true)).andExpect(status().isOk()), "$.data.id");
        staffPost(staff("usr_sc_nop_" + f.suffix, "ticket.order.create"), "/api/v1/student-certifications/" + id + "/approve",
                Map.of("valid_until", LocalDate.now().plusYears(1).toString()))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT status FROM customer_student_certifications WHERE id = ?", String.class, id))
                .isEqualTo("pending");
    }

    // ---------- helpers ----------

    private String consent(String type, boolean accepted) throws Exception {
        return read(f.customerPost("/api/v1/customer/consents", Map.of("idempotency_key", "sc_consent_" + UUID.randomUUID(),
                "consent_type", type, "accepted", accepted, "version", "student_cert_v1")).andExpect(status().isOk()), "$.data.id");
    }

    private ResultActions apply(String consentId) throws Exception {
        return apply(consentId, new byte[] {1, 2, 3, 4});
    }

    private ResultActions apply(String consentId, byte[] photo) throws Exception {
        var request = multipart("/api/v1/customer/student-certifications")
                .file(new MockMultipartFile("files", "student-card.jpg", "image/jpeg", photo))
                .param("idempotency_key", "sc_apply_" + UUID.randomUUID())
                .param("chsi_verification_code", "CHSI1234567890AB");
        if (consentId != null) {
            request.param("consent_id", consentId);
        }
        return mockMvc.perform(request.header("X-Terminal-Type", "miniapp").header("X-Customer-Token", f.token));
    }

    private ResultActions staffPost(RequestPostProcessor who, String path, Map<String, Object> body) throws Exception {
        Map<String, Object> actual = new java.util.LinkedHashMap<>(body);
        actual.putIfAbsent("idempotency_key", "sc_staff_" + UUID.randomUUID());
        return mockMvc.perform(post(path).with(who).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(actual)));
    }

    private String rule(String variant, int discount) {
        String id = "price_rule_sc_" + variant + "_" + f.suffix;
        Instant now = Instant.now();
        repository.savePriceRule(new TicketPriceRuleRecord(id, f.ticketTypeId, variant, "derive_from_calendar_price",
                null, null, null, BigDecimal.valueOf(discount), null, "CNY", "active", Map.of(), now, now));
        return id;
    }

    private Map<String, Object> order(String orderId) {
        return jdbc.queryForMap("SELECT amount_cent, price_rule_id FROM ticket_orders WHERE id = ?", orderId);
    }

    private RequestPostProcessor staff(String userId, String... permissions) {
        if (granted.add(userId)) {
            String role = "role_" + userId;
            jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '学生认证回归', 'project', 'active', now())",
                    role, role);
            for (String code : permissions) {
                jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, code);
            }
            jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at) VALUES (?, ?, 'staff', ?, NULL, 'active', now())",
                    userId, userId, f.projectId);
            jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
        }
        AccessActor actor = new AccessActor(userId, userId, "staff", f.projectId, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private static String read(ResultActions result, String path) throws Exception {
        return CustomerTicketPaymentFixture.read(result, path);
    }
}
