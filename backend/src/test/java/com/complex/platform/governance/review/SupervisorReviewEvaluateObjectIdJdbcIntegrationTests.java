package com.complex.platform.governance.review;

// 文件版本：V0.01

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AP 段第 229 条 229-R10⑤（PGM:899「平台治理层要保证每个重要动作能追溯。……」）：主管复核评估的对象编号不再预填 contract-1——
 * 不带对象编号 400（网页同口径拦空），按蛇形键走真实 HTTP、真实授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SupervisorReviewEvaluateObjectIdJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String digits = String.valueOf(System.nanoTime());

    @Test
    void evaluateWithoutObjectIdIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/platform/supervisor-review/evaluate")
                        .with(new StaffRequestFixture(jdbc).staff("usr_229_sr_" + digits, "staff", null, null,
                                "governance.supervisor_review.manage"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key":"sr-evaluate-229-%s","evidence_attachment_ids":[],"action_type":"contract_violation",
                                 "object_type":"合同","amount_cent":0,"violation_summary":"合同主体与营业执照不一致"}
                                """.formatted(digits)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }
}
