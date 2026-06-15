package com.complex.platform.ops.productionreadiness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd",
        "complex.smoke.admin.username=p0_jdbc_test_admin",
        "complex.smoke.admin.password=p0_jdbc_test_password"
})
@Transactional
class ProductionReadinessJdbcIntegrationTests {

    @Autowired
    private ProductionReadinessService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void resetReadinessTables() {
        jdbcTemplate.execute("""
                TRUNCATE production_readiness_check_blockers,
                         production_readiness_checks,
                         idempotency_records,
                         audit_events,
                         production_readiness_items
                """);
    }

    @Test
    void listItemsSeedsDefaultsAndWritesAuditEventThroughPostgres() {
        var response = service.listItems();

        assertThat(response.items()).hasSize(12);
        assertThat(response.total()).isEqualTo(12);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM production_readiness_items",
                Long.class
        )).isEqualTo(12);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events WHERE event_name = 'production_readiness.viewed'",
                Long.class
        )).isEqualTo(1);
    }

    @Test
    void authenticatedListItemsApiReturnsOkAndDefaultTotalThroughPostgres() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login_name": "p0_jdbc_test_admin",
                                  "password": "p0_jdbc_test_password"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String accessToken = objectMapper.readTree(loginResponse)
                .path("data")
                .path("access_token")
                .asText();

        mockMvc.perform(get("/api/v1/ops/production-readiness/items")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.total").value(12));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM production_readiness_items",
                Long.class
        )).isEqualTo(12);
    }
}
