package com.complex.platform.compliance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * 复现 High bug：企微会话合规监察首屏 GET /api/v1/compliance/wecom/risk-hits 稳定 500。
 * 走完整 HTTP 栈（真 controller + 真授权双闸 + 真 service + 真 repository），空表场景，
 * 用与端点相同的 admin token。断言应为 200；当前应因 500 而红，并在栈中打印确切抛出行。
 */
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd",
        "complex.database.credential-separation.enforced=false",
        "complex.auth.production.seed-admin.username=p0_riskhit_admin",
        "complex.auth.production.seed-admin.password=p0_riskhit_password",
        "ticket.credential.signing.secret=prod-test-ticket-signing-secret-32-bytes-minimum",
        "bar.invitation.signing.secret=prod-test-bar-invitation-signing-secret-32-bytes-minimum",
        "keyvault.keys.pii=prod-test-pii-keyvault-secret-32-bytes-minimum",
        "keyvault.keys.pii-index=prod-test-pii-index-blind-secret-32-bytes-minimum"
})
@Transactional
class WecomRiskHitsEndpointReproTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void listRiskHitsFirstScreenReturns200OnEmptyTable() throws Exception {
        // 空表场景：清掉命中表，模拟首屏无数据。
        jdbcTemplate.update("DELETE FROM wecom_risk_hits");

        JsonNode login = login("p0_riskhit_admin", "p0_riskhit_password");
        String accessToken = login.at("/data/access_token").asText();

        mockMvc.perform(get("/api/v1/compliance/wecom/risk-hits")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));
    }

    private JsonNode login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login_name": "%s",
                                  "password": "%s",
                                  "device_id": "prod-web-1",
                                  "terminal_type": "web"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
