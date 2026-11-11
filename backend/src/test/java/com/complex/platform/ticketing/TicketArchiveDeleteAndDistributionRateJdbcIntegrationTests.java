package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AE 段第 201 条 201-R2（控制器入口补进 OpenAPI，契约与代码一致）：按契约声明的请求体（蛇形键）与状态码，
 * 走真实入口与真实授权链锁住现行为——入口已存在，本类只锁现行为，不涉及先红后绿。
 * ① 票务分销商户费率：per_guest_cent 与 fixed_amount_cent 二选一，两个都填或都不填 400 VALIDATION_FAILED，只填一项 200。
 * ② 票种存档、申请删除、审批删除（BSM:569「存档（先下线）」；BSM:576「存档及删除都需要填写理由」「申请人不得审批自己的申请」）：
 * 存档缺理由 400，带理由 200；申请删除后申请人本人审批 403 TICKET_TYPE_SELF_APPROVE_FORBIDDEN，票种仍在、仍待审批。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class TicketArchiveDeleteAndDistributionRateJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "tadr_prj_" + s;
    private StaffRequestFixture staffs;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','票务契约回归综合体','active',now(),now())", project);
        staffs = new StaffRequestFixture(jdbc);
    }

    @Test
    void distributionMerchantRateTakesExactlyOneOfPerGuestOrFixedAmount() throws Exception {
        RequestPostProcessor pricing = staffs.staff("usr_tadr_price_" + s, "staff", project, null, "ticket.price_variant.manage");
        String merchant = "tadr_mch_" + s;

        rate(pricing, Map.of("idempotency_key", "tadr-dmr-both-" + s, "project_id", project, "merchant_id", merchant,
                "per_guest_cent", 500, "fixed_amount_cent", 20000))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        rate(pricing, Map.of("idempotency_key", "tadr-dmr-none-" + s, "project_id", project, "merchant_id", merchant))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_distribution_merchant_rates WHERE merchant_id = ?",
                Integer.class, merchant)).isZero();

        rate(pricing, Map.of("idempotency_key", "tadr-dmr-guest-" + s, "project_id", project, "merchant_id", merchant,
                "per_guest_cent", 500))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rule_type").value("per_guest"))
                .andExpect(jsonPath("$.data.per_guest_cent").value(500))
                .andExpect(jsonPath("$.data.status").value("active"));
        assertThat(jdbc.queryForObject("SELECT rule_type FROM ticket_distribution_merchant_rates WHERE merchant_id = ?",
                String.class, merchant)).isEqualTo("per_guest");
    }

    @Test
    void ticketTypeArchiveNeedsReasonAndDeleteRequesterCannotApproveOwnRequest() throws Exception {
        RequestPostProcessor manager = staffs.staff("usr_tadr_ticket_" + s, "staff", project, null,
                "ticket.product.manage", "ticket.type.approve");
        String ticketType = "tt_tadr_" + s;
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, status, created_at, updated_at)"
                + " VALUES (?, '契约回归票种', 'fixed', 'active', now(), now())", ticketType);
        String base = "/api/v1/ticketing/ticket-types/" + ticketType;

        call(manager, base + "/archive", Map.of("idempotency_key", "tadr-arch-noreason-" + s))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        call(manager, base + "/archive", Map.of("idempotency_key", "tadr-arch-" + s, "reason", "配错下线"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.archived_at").isNotEmpty());
        call(manager, base + "/delete-request", Map.of("idempotency_key", "tadr-delreq-" + s, "reason", "废弃票种清理"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.delete_status").value("pending"));
        call(manager, base + "/delete-approve", Map.of("idempotency_key", "tadr-delapp-" + s, "decision", "approved"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TICKET_TYPE_SELF_APPROVE_FORBIDDEN"));

        assertThat(jdbc.queryForObject("SELECT delete_status FROM ticket_types WHERE id = ?", String.class, ticketType))
                .isEqualTo("pending");
    }

    private ResultActions rate(RequestPostProcessor actor, Map<String, ?> body) throws Exception {
        return call(actor, "/api/v1/ticketing/distribution-merchant-rates", body);
    }

    private ResultActions call(RequestPostProcessor actor, String url, Map<String, ?> body) throws Exception {
        return mockMvc.perform(post(url).with(actor).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }
}
