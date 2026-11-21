package com.complex.platform.ticketing;

// 文件版本：V0.03

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.hamcrest.Matchers;
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
 * 第十八批 AW 段 294-R1（BSM:755「单一酒吧门票的综合服务费按物理项目分别维护，初始为空、不自动继承酒吧通票的默认比例，票种发布前必须先选定按比例还是
 * 按固定金额并填入数值，未选定或未填写的票种不得发布上架」）：单店票发布闸查本项目本酒吧已转正的综合服务费；项目默认行、草案都不算；通票不受影响。
 * 后台票务策略页、结算配置页真实请求（蛇形键）与真实授权链，随测试事务回滚。
 * 第十八批 BQ 段第 371 条 371-R1（BSM:521「经高敏权限“转正”审批通过后按指定日期生效，不即改即生效」）：综合服务费草案带次日生效起始日期，
 * 批准后待生效仍不算已选定，到生效起始日期由系统转正后才能发布。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SingleBarServiceFeePublishGate294Tests {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private com.complex.platform.common.BusinessDayResolver businessDays;
    @Autowired private com.complex.platform.settlement.SettlementConfigScheduledActivationService activation;

    private final String s = String.valueOf(System.nanoTime());
    private final String project = "prj_c294_" + s;
    private final String bar = "bar_c294_" + s;
    private final String restaurant = "rest_c294_" + s;
    private RequestPostProcessor admin;
    private RequestPostProcessor drafter;
    private RequestPostProcessor reviewer;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', ?, 'active', now(), now())", project, "服务费发布闸综合体" + s);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, project_id, created_at, updated_at)"
                + " VALUES (?, ?, 'bar', 'active', ?, now(), now())", bar, "服务费发布闸酒吧" + s, project);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, project_id, created_at, updated_at)"
                + " VALUES (?, ?, 'catering', 'active', ?, now(), now())", restaurant, "服务费发布闸餐厅" + s, project);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        admin = staffs.staff("usr_c294_admin_" + s, "platform_admin", "org_1", null,
                "ticket.product.manage", "ticket.product.view", "ticket.entitlement.manage");
        drafter = staffs.staff("usr_c294_a_" + s, "staff", project, null, "settlement.config.manage", "settlement.config.view");
        reviewer = staffs.staff("usr_c294_b_" + s, "staff", project, null, "settlement.config.manage", "settlement.config.view");
    }

    @Test
    void singleBarTicketWithOnlyProjectDefaultOrDraftServiceFeeCannotPublish_untilBarRowActivated() throws Exception {
        activate(serviceFee("pd", null, 800, null), null);   // 项目默认行：不算本酒吧已选定
        activateDue();
        String ticket = singleBarTicket("a");
        publish(ticket).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TICKET_TYPE_SERVICE_FEE_REQUIRED"))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("服务费发布闸综合体" + s)));

        serviceFee("bar", bar, 500, null).andExpect(status().isOk());   // 本酒吧只落草案、未复核转正
        publish(ticket).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TICKET_TYPE_SERVICE_FEE_REQUIRED"));

        activate(null, bar);
        publish(ticket).andExpect(status().isConflict())   // 371-R1：本酒吧行已批准但待生效，仍不算已选定
                .andExpect(jsonPath("$.detail_code").value("TICKET_TYPE_SERVICE_FEE_REQUIRED"))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("复核转正并到生效起始日期后才能上架")));
        activateDue();
        publish(ticket).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("published"));
    }

    @Test
    void fixedAmountBarRowAlsoCountsAsChosenAndFilled() throws Exception {
        activate(serviceFee("fx", bar, 0, 1000L), bar);
        activateDue();
        publish(singleBarTicket("b")).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("published"));
    }

    /** 通票不套本闸：发布闸全过、走到票种规则审批（第十八批 BP 段 300-R1：域内票须指定对应域主管公司的最终审批人，不指定 400），不报综合服务费未选定。 */
    @Test
    void passWithoutBarIsNotAffected() throws Exception {
        String pass = create("c", null);
        bind(pass, template("通票入场", restaurant));
        publish(pass).andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("最终审批人")));
    }

    private String singleBarTicket(String tag) throws Exception {
        String ticket = create(tag, bar);
        bind(ticket, template("单店入场", bar));
        return ticket;
    }

    private String create(String tag, String barId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/ticketing/ticket-types").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c294-%s-%s", "ticket_name": "服务费发布闸票%s",
                                 "base_calendar_price_mode": "weekday_weekend_holiday", "weekday_price_cent": 9900,
                                 "weekend_price_cent": 9900, "holiday_price_cent": 9900, "stock_required": false, "bar_id": %s}
                                """.formatted(tag, s, s, barId == null ? "null" : "\"" + barId + "\"")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JSON.readTree(body).path("data").path("id").asText();
    }

    private String template(String name, String merchant) throws Exception {
        String body = mockMvc.perform(post("/api/v1/ticketing/entitlement-templates").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c294-tpl-%s-%s", "name": "%s%s", "entitlement_type": "entry",
                                 "default_total_times": 1, "default_value_cent": 0, "status": "active", "merchant_id": "%s"}
                                """.formatted(name.hashCode(), System.nanoTime(), name, s, merchant)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JSON.readTree(body).path("data").path("id").asText();
    }

    private void bind(String ticket, String template) throws Exception {
        mockMvc.perform(post("/api/v1/ticketing/ticket-types/" + ticket + "/entitlements").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "c294-bind-%s", "entitlement_template_id": "%s", "quantity": 1,
                                 "total_times": 1, "num_people": 1, "component_price_cent": 9900}
                                """.formatted(System.nanoTime(), template)))
                .andExpect(status().isOk());
    }

    private ResultActions publish(String ticket) throws Exception {
        return mockMvc.perform(post("/api/v1/ticketing/ticket-types/" + ticket + "/publish").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key": "c294-pub-%s", "action": "publish", "reason": "单店票上架"}
                        """.formatted(System.nanoTime())));
    }

    private ResultActions serviceFee(String tag, String merchantId, int feeBps, Long fixedAmountCent) throws Exception {
        return mockMvc.perform(post("/api/v1/settlement/config/comprehensive-service-fee").with(drafter)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key": "c294-fee-%s-%s", "project_id": "%s", "merchant_id": %s, "fee_bps": %d,
                         "fixed_amount_cent": %s, "effective_from": "%s"}
                        """.formatted(tag, s, project, merchantId == null ? "null" : "\"" + merchantId + "\"", feeBps,
                        fixedAmountCent == null ? "null" : fixedAmountCent, (merchantId == null
                                ? businessDays.businessDate(project, null, java.time.Instant.now())
                                : businessDays.merchantBusinessDate(project, merchantId, java.time.Instant.now())).plusDays(1))));
    }

    /** 371-R1：到次日由系统转正已批准待生效的配置。 */
    private void activateDue() {
        activation.activateDue(java.time.Instant.now().plus(java.time.Duration.ofDays(1)));
    }

    /** 他人复核批准本项目（merchantId 为空＝项目默认行）的综合服务费草案（批准后待生效）。 */
    private void activate(ResultActions saved, String merchantId) throws Exception {
        if (saved != null) {
            saved.andExpect(status().isOk());
        }
        String draft = merchantId == null
                ? jdbc.queryForObject("SELECT id FROM comprehensive_service_fee_configs WHERE project_id = ? AND merchant_id IS NULL"
                        + " AND is_draft = TRUE AND status = 'active'", String.class, project)
                : jdbc.queryForObject("SELECT id FROM comprehensive_service_fee_configs WHERE project_id = ? AND merchant_id = ?"
                        + " AND is_draft = TRUE AND status = 'active'", String.class, project, merchantId);
        mockMvc.perform(post("/api/v1/settlement/config/comprehensive_service_fee/" + draft + "/activate").with(reviewer))
                .andExpect(status().isOk());
    }
}
