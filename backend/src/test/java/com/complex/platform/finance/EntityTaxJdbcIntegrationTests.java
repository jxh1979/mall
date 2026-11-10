package com.complex.platform.finance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AC 段第 210 条 210-R2（第十八批裁决 42：财务按公司、税种、所属期手工登记「实际申报金额」，经财务、税务、法务审核并由顶层平台公司老板
 * 最终确认后生效）真库回归。依据 BSM:1760「平台只接收专业财税软件回传的数据，或保存经授权人员手工填写并通过审批的数据」「所有税费规则和税费调整均须
 * 经过财务审核、税务审核、法务审核，再由顶层平台公司的老板最终确认后生效，提交人不得审批自己提交的调整」。
 *
 * <p>夹具随测试事务回滚：持 finance.entity_tax.manage 的平台财务按前端蛇形键走真实登记入口；财务、税务、法务三条线的起点即最终审批人，各用自己的账号、
 * 最高确认用最高权限会话，都经真实审批接口逐级批。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class EntityTaxJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = String.valueOf(System.nanoTime());
    private final String project = "etd_prj_" + s;
    private final String entity = "etd_be_" + s;
    private final String financeApprover = "usr_etd_fr_" + s;
    private final String taxApprover = "usr_etd_tr_" + s;
    private final String legalApprover = "usr_etd_lr_" + s;
    private RequestPostProcessor finance;
    private RequestPostProcessor financeReviewer;
    private RequestPostProcessor taxReviewer;
    private RequestPostProcessor legalReviewer;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','税费登记综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at)"
                + " VALUES (?, ?, 'management_company', 'active', now(), now())", entity, "税费登记酒吧公司" + s);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        // 提交人也持审批权，才能证明「提交人不得审批自己提交的」由复核链拦住而不是缺权限。
        finance = staffs.staff("usr_etd_fin_" + s, "staff", project, null,
                "finance.entity_tax.manage", "finance.entity_tax.view", "governance.supervisor_review.approve");
        financeReviewer = staffs.staff(financeApprover, "staff", project, null, "governance.supervisor_review.approve");
        taxReviewer = staffs.staff(taxApprover, "staff", project, null, "governance.supervisor_review.approve");
        legalReviewer = staffs.staff(legalApprover, "staff", project, null, "governance.supervisor_review.approve");
        for (String[] line : List.of(new String[]{"finance_review", financeApprover}, new String[]{"tax_review", taxApprover},
                new String[]{"legal_review", legalApprover})) {
            jdbc.update("INSERT INTO approval_lines(line_code,line_name,min_start_rank,entry_user_id,status) VALUES(?,?,1,?,'active')"
                    + " ON CONFLICT(line_code) DO UPDATE SET entry_user_id=EXCLUDED.entry_user_id,status='active'", line[0], line[0], line[1]);
        }
    }

    /** 修复前没有登记入口：第一次登记 404，状态断言红。 */
    @Test
    void declarationTakesEffectOnlyAfterFinanceTaxLegalAndTopConfirmation() throws Exception {
        String body = declare("etd-1-" + s, "vat", "2026-01", "2026-03", 1234567)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"))
                .andExpect(jsonPath("$.data.entity_name").value("税费登记酒吧公司" + s))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.data.id");
        String review = JsonPath.read(body, "$.data.review_id");

        approve(finance, review, "approved").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
        approve(master(), review, "approved").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DEPARTMENT_REVIEWS_PENDING"));
        for (RequestPostProcessor reviewer : List.of(financeReviewer, taxReviewer, legalReviewer)) {
            approve(reviewer, review, "approved").andExpect(status().isOk());
        }
        assertThat(statusOf(id)).isEqualTo("pending_review");
        approve(master(), review, "approved").andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/finance/entity-taxes").param("business_entity_id", entity).param("month", "2026-02").with(finance))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(id))
                .andExpect(jsonPath("$.data.items[0].tax_type").value("vat"))
                .andExpect(jsonPath("$.data.items[0].period_start_month").value("2026-01"))
                .andExpect(jsonPath("$.data.items[0].period_end_month").value("2026-03"))
                .andExpect(jsonPath("$.data.items[0].declared_amount_cent").value(1234567))
                .andExpect(jsonPath("$.data.items[0].status").value("approved"))
                .andExpect(jsonPath("$.data.items[0].approved_at").isNotEmpty());

        declare("etd-2-" + s, "vat", "2026-01", "2026-03", 1).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ENTITY_TAX_DECLARATION_DUPLICATE"));
    }

    @Test
    void rejectedDeclarationCanBeRegisteredAgain() throws Exception {
        String body = declare("etd-r1-" + s, "corporate_income", "2026-03", "2026-03", 50000)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.data.id");
        approve(taxReviewer, JsonPath.read(body, "$.data.review_id"), "rejected").andExpect(status().isOk());
        assertThat(statusOf(id)).isEqualTo("rejected");

        declare("etd-r2-" + s, "corporate_income", "2026-03", "2026-03", 60000)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"))
                .andExpect(jsonPath("$.data.declared_amount_cent").value(60000));
    }

    @Test
    void missingDepartmentRoutingIsRejectedWithoutWritingTheDeclaration() throws Exception {
        jdbc.update("UPDATE approval_lines SET entry_user_id = NULL WHERE line_code = 'tax_review'");
        declare("etd-m-" + s, "surcharge", "2026-04", "2026-04", 300)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TAX_DEPARTMENT_ROUTING_REQUIRED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM entity_tax_declarations WHERE business_entity_id = ?", Integer.class, entity)).isZero();
    }

    // ---------- helpers ----------

    private ResultActions declare(String key, String taxType, String start, String end, long cent) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/entity-taxes").with(finance).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + key + "\",\"business_entity_id\":\"" + entity + "\",\"tax_type\":\"" + taxType
                        + "\",\"period_start_month\":\"" + start + "\",\"period_end_month\":\"" + end + "\",\"declared_amount_cent\":" + cent
                        + ",\"reason\":\"按税务申报表登记\",\"evidence_attachment_ids\":[],\"parallel_lines\":["
                        + "{\"line_code\":\"finance_review\",\"final_approver_id\":\"" + financeApprover + "\"},"
                        + "{\"line_code\":\"tax_review\",\"final_approver_id\":\"" + taxApprover + "\"},"
                        + "{\"line_code\":\"legal_review\",\"final_approver_id\":\"" + legalApprover + "\"}]}"));
    }

    private ResultActions approve(RequestPostProcessor who, String review, String decision) throws Exception {
        return mockMvc.perform(post("/api/v1/platform/supervisor-review/" + review + "/approve").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"" + decision + "\",\"comment\":\"已核对申报表\",\"idempotency_key\":\"etd-ap-" + System.nanoTime() + "\"}"));
    }

    private String statusOf(String id) {
        return jdbc.queryForObject("SELECT status FROM entity_tax_declarations WHERE id = ?", String.class, id);
    }

    private RequestPostProcessor master() {
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE,
                "platform-company", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
