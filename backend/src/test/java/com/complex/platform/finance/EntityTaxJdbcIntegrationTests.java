package com.complex.platform.finance;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    // 233-R1、233-R3 弹窗的三条线最终审批人（蛇形键）。
    private final String lines = "\"parallel_lines\":[{\"line_code\":\"finance_review\",\"final_approver_id\":\"" + financeApprover + "\"},"
            + "{\"line_code\":\"tax_review\",\"final_approver_id\":\"" + taxApprover + "\"},"
            + "{\"line_code\":\"legal_review\",\"final_approver_id\":\"" + legalApprover + "\"}]";

    private int seq;

    @BeforeEach
    void setUp() {
        // 233-R5：登记增值税、文化事业建设费、附加税前该月月度分账单须已确认（否则 409 TICKET_SETTLEMENT_PENDING），给既有用例的所属月补上（单独项目）。
        for (int m = 1; m <= 4; m++) {
            LocalDate first = LocalDate.of(2026, m, 1);
            jdbc.update("INSERT INTO settlement_bills (id, project_id, period_start, period_end, status) VALUES (?, ?, ?, ?, 'confirmed')",
                    "sb_etd_fx" + m + "_" + s, "etd_prjfx_" + s, first, first.withDayOfMonth(first.lengthOfMonth()));
        }
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

    /**
     * 第十八批 AR 段第 233 条 233-R1（BSM:1760「税费金额分三个阶段分别保存、互不覆盖：系统计算金额、实际申报金额、实际缴纳金额」「缴税差异等于实际缴纳金额
     * 减实际申报金额」「实缴与申报不一致的部分先按未缴、预缴、多缴、抵扣或待退分别处理，不得直接混入税费成本」）：已生效增值税申报 10000 元，财务按前端
     * 蛇形键登记实际缴纳 9000 元、未缴 1000 元；待审时再登记 409；经三线与最高确认逐级批后列表给出实际缴纳金额、缴税差异与分类；补缴到 10000 元再批准，
     * 列表取最新一条，旧登记留作历史。修复前没有登记入口：404，状态断言红。
     */
    @Test
    void paymentTakesEffectAfterFullApprovalAndListShowsLatestPaidAmountWithDifferenceCategories() throws Exception {
        String id = approvedDeclaration("etd-p-" + s, "vat", "2026-01", "2026-01", 1_000_000);
        String body = pay(id, "etd-p1-" + s, 900_000, "[{\"category\":\"unpaid\",\"amount_cent\":100000}]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"))
                .andExpect(jsonPath("$.data.payment_difference_cent").value(-100_000))
                .andReturn().getResponse().getContentAsString();
        pay(id, "etd-p2-" + s, 1_000_000, "[]").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ENTITY_TAX_PAYMENT_PENDING"));
        listed().andExpect(jsonPath("$.data.items[0].payment_pending").value(true))
                .andExpect(jsonPath("$.data.items[0].paid_amount_cent").doesNotExist());
        approveAll(JsonPath.read(body, "$.data.review_id"));

        listed().andExpect(jsonPath("$.data.items[0].declared_amount_cent").value(1_000_000))
                .andExpect(jsonPath("$.data.items[0].paid_amount_cent").value(900_000))
                .andExpect(jsonPath("$.data.items[0].payment_difference_cent").value(-100_000))
                .andExpect(jsonPath("$.data.items[0].payment_breakdown[0].category").value("unpaid"))
                .andExpect(jsonPath("$.data.items[0].payment_breakdown[0].amount_cent").value(100_000))
                .andExpect(jsonPath("$.data.items[0].payment_pending").value(false));

        approveAll(JsonPath.read(pay(id, "etd-p3-" + s, 1_000_000, "[]").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.data.review_id"));
        listed().andExpect(jsonPath("$.data.items[0].paid_amount_cent").value(1_000_000))
                .andExpect(jsonPath("$.data.items[0].payment_difference_cent").value(0))
                .andExpect(jsonPath("$.data.items[0].payment_breakdown.length()").value(0));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM entity_tax_payments WHERE declaration_id = ? AND status = 'approved'",
                Integer.class, id)).isEqualTo(2);
    }

    /** 233-R1、233-R3 入口校验：分类合计不等于差额绝对值、差额为 0 却带分类、调整金额为 0 一律 400；对待审批申报登记实缴或差异调整单 409。修复前 404：断言红。 */
    @Test
    void paymentAndAdjustmentRequireApprovedDeclarationAndConsistentAmounts() throws Exception {
        String id = approvedDeclaration("etd-v-" + s, "vat", "2026-02", "2026-02", 500_000);
        pay(id, "etd-v1-" + s, 400_000, "[{\"category\":\"unpaid\",\"amount_cent\":50000}]").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        pay(id, "etd-v2-" + s, 500_000, "[{\"category\":\"prepaid\",\"amount_cent\":1}]").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        adjust(id, "etd-v3-" + s, 0).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        String pending = JsonPath.read(declare("etd-v4-" + s, "surcharge", "2026-02", "2026-02", 9_000)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
        pay(pending, "etd-v5-" + s, 9_000, "[]").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ENTITY_TAX_DECLARATION_NOT_APPROVED"));
        adjust(pending, "etd-v6-" + s, 100).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ENTITY_TAX_DECLARATION_NOT_APPROVED"));
        assertThat(jdbc.queryForObject("SELECT (SELECT count(*) FROM entity_tax_payments WHERE business_entity_id = ?)"
                + " + (SELECT count(*) FROM entity_tax_adjustments WHERE business_entity_id = ? AND kind = 'period_difference')", Integer.class,
                entity, entity)).isZero();
    }

    /**
     * 233-R3（BSM:1760、ENUM:201「原期间已经关账、开票或申报的，不得修改历史数据，只能在当前开放期间生成关联原期间的差异调整单」；BSM:1760「所有税费规则和
     * 税费调整均须经过财务审核、税务审核、法务审核，再由顶层平台公司的老板最终确认后生效」）：1 月增值税申报 5000 元已生效，财务按前端蛇形键生成 +300 元
     * 差异调整单；待审时计入月份为空，逐级批准后原申报不变、调整单计入批准当月（北京时间）并关联原所属期；同期再登记申报仍 409；按更正后申报 5300 元
     * 登记实缴时缴税差异为 0、不要求分类（233-R1）。申报批准时 233-R5 另按申报差异出补提单（本用例系统计算为 0），差异调整单按种类取。
     * 修复前没有入口：404，状态断言红。
     */
    @Test
    void periodDifferenceAdjustmentKeepsOriginalDeclarationAndCountsInApprovalMonth() throws Exception {
        String id = approvedDeclaration("etd-a-" + s, "vat", "2026-01", "2026-01", 500_000);
        String body = adjust(id, "etd-a1-" + s, 30_000)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kind").value("period_difference"))
                .andExpect(jsonPath("$.data.status").value("pending_review"))
                .andReturn().getResponse().getContentAsString();
        listed().andExpect(jsonPath("$.data.items[0].adjustments[?(@.kind == 'period_difference')].status").value("pending_review"))
                .andExpect(jsonPath("$.data.items[0].adjustments[?(@.kind == 'period_difference')].accounting_month").value(contains(nullValue())));
        approveAll(JsonPath.read(body, "$.data.review_id"));

        String approvalMonth = jdbc.queryForObject("SELECT to_char(approved_at AT TIME ZONE 'Asia/Shanghai', 'YYYY-MM') FROM entity_tax_adjustments"
                + " WHERE id = ?", String.class, (String) JsonPath.read(body, "$.data.id"));
        listed().andExpect(jsonPath("$.data.items[0].declared_amount_cent").value(500_000))
                .andExpect(jsonPath("$.data.items[0].adjustments[?(@.kind == 'period_difference')].kind").value("period_difference"))
                .andExpect(jsonPath("$.data.items[0].adjustments[?(@.kind == 'period_difference')].amount_cent").value(30_000))
                .andExpect(jsonPath("$.data.items[0].adjustments[?(@.kind == 'period_difference')].status").value("approved"))
                .andExpect(jsonPath("$.data.items[0].adjustments[?(@.kind == 'period_difference')].accounting_month").value(approvalMonth))
                .andExpect(jsonPath("$.data.items[0].adjustments[?(@.kind == 'period_difference')].origin_period_start_month").value("2026-01"))
                .andExpect(jsonPath("$.data.items[0].adjustments[?(@.kind == 'period_difference')].origin_period_end_month").value("2026-01"));
        declare("etd-a2-" + s, "vat", "2026-01", "2026-01", 530_000).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ENTITY_TAX_DECLARATION_DUPLICATE"));
        pay(id, "etd-a3-" + s, 530_000, "[]").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payment_difference_cent").value(0));
    }

    /**
     * 第十八批 AR 段第 233 条 233-R2（ENUM:201「每个主体 × 每类收入 × 每种税（增值税/文化事业建设费/附加税）后台各自配“有没有这笔费用 + 收多少”」；BSM:521
     * 「所有涉及钱的配置一律版本化：每次改动生成新版本，必须填写生效起始日期……经高敏权限“转正”审批通过后按指定日期生效」「新版本只对生效日之后发生的业务适用，
     * 已发生的业务一律按当时生效的版本计算」；BSM:1760「所有税费规则和税费调整均须经过财务审核、税务审核、法务审核，再由顶层平台公司的老板最终确认后生效，提交人
     * 不得审批自己提交的调整」）：财务按前端蛇形键给本主体提交「通票可分配池分账 增值税 9% 自 2026-03-01」，返回待审；批准前该主体 3 月开票条目仍按平台默认 6%，
     * 提交人自审被拒；经三线与最高确认后该主体 3 月按 9%、2 月仍按 6%，别的主体 3 月仍按 6%；税率列表带主体名与已生效状态。开票清单只出门票综合服务费、
     * 可分配池分账与积分发行费三类条目，故用可分配池分账（方案写餐饮，餐饮没有开票条目可验）。修复前没有入口：404，状态断言红。
     */
    @Test
    void entityRateVersionAppliesFromEffectiveDateOnlyAfterFullApproval() throws Exception {
        String other = "etd_be2_" + s;
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at)"
                + " VALUES (?, ?, 'management_company', 'active', now(), now())", other, "税率别家酒吧公司" + s);
        String own = merchant("etd_m1_" + s, entity);
        String others = merchant("etd_m2_" + s, other);
        String feb = bill("02", 0, own);
        String mar = bill("03", 0, own, others);

        String body = rate("etd-rt1-" + s, entity, "spv_service", "vat", true, 900, "2026-03-01")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"))
                .andExpect(jsonPath("$.data.business_entity_id").value(entity))
                .andExpect(jsonPath("$.data.rate_bps").value(900))
                .andExpect(jsonPath("$.data.effective_from").value("2026-03-01"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.data.id");
        String review = JsonPath.read(body, "$.data.review_id");
        assertThat(vatRates(mar)).containsEntry(own, 600);
        jdbc.update("DELETE FROM settlement_invoice_items WHERE bill_id = ?", mar);

        approve(finance, review, "approved").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
        approveAll(review);

        assertThat(vatRates(feb)).containsEntry(own, 600);
        assertThat(vatRates(mar)).containsEntry(own, 900).containsEntry(others, 600);
        mockMvc.perform(get("/api/v1/finance/entity-taxes/rates").with(finance))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + id + "')].entity_name").value("税费登记酒吧公司" + s))
                .andExpect(jsonPath("$.data[?(@.id == '" + id + "')].income_type").value("spv_service"))
                .andExpect(jsonPath("$.data[?(@.id == '" + id + "')].applicable").value(true))
                .andExpect(jsonPath("$.data[?(@.id == '" + id + "')].status").value("approved"));
    }

    /**
     * 233-R2 入口校验：其他税费不配比例（第十八批裁决 59「其他税费和企业所得税一样，按季与财税软件核对、以财税软件为准拉平」，没有系统计算基数）、企业所得税只按
     * 全部收入配、增值税等不收「全部收入」一律 400；同主体同收入类型同税种同生效日已有待审版本 409 TAX_RATE_VERSION_CONFLICT；没配部门起点 409
     * TAX_DEPARTMENT_ROUTING_REQUIRED 且不落行。修复前 404：断言红。
     */
    @Test
    void rateVersionRejectsOtherTaxMismatchedIncomeTypeDuplicateDateAndMissingRouting() throws Exception {
        rate("etd-rv1-" + s, entity, "catering", "other", true, 100, "2026-03-01").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        rate("etd-rv2-" + s, entity, "catering", "corporate_income", true, 2500, "2026-03-01").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        rate("etd-rv3-" + s, entity, "all", "vat", true, 600, "2026-03-01").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        rate("etd-rv4-" + s, entity, "all", "corporate_income", true, 2500, "2026-04-01").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"))
                .andExpect(jsonPath("$.data.income_type").value("all"));
        rate("etd-rv5-" + s, entity, "all", "corporate_income", true, 2000, "2026-04-01").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TAX_RATE_VERSION_CONFLICT"));
        jdbc.update("UPDATE approval_lines SET entry_user_id = NULL WHERE line_code = 'legal_review'");
        rate("etd-rv6-" + s, entity, "goods", "surcharge", true, 700, "2026-04-01").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("TAX_DEPARTMENT_ROUTING_REQUIRED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM settlement_tax_configs WHERE business_entity_id = ?", Integer.class, entity)).isEqualTo(1);
    }

    /**
     * 第十八批 BU 段 374-R4（ENUM:201「每个主体 × 每类收入 × 每种税（增值税/文化事业建设费/附加税）后台各自配」；BSM:1760「税率、税目、发票对象和红冲流程由系统
     * 按有效规则配置」）：派驻提成与酒吧报酬拨付进开票清单后，人员报酬拨付是可配税率的收入类型。修复前 400 VALIDATION_FAILED：断言红。
     */
    @Test
    void staffRemunerationIsARateIncomeType() throws Exception {
        rate("etd-sr-" + s, entity, "staff_remuneration", "vat", true, 600, "2026-03-01").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.income_type").value("staff_remuneration"))
                .andExpect(jsonPath("$.data.status").value("pending_review"));
    }

    /**
     * 233-R2 读取口径（BSM:521「新版本只对生效日之后发生的业务适用」；ENUM:201「有没有这笔费用」）：本主体门票综合服务费的文化事业建设费新版本「没有这笔费用」
     * 自 2026-03-01 生效后，3 月开票按 0 出清单，不退回平台默认的旧版本 3%；2 月仍按 3%。修复前 404：断言红。
     */
    @Test
    void inapplicableNewVersionStopsCulturalFeeWithoutFallingBackToOlderVersion() throws Exception {
        jdbc.update("INSERT INTO revenue_stream_attribution (id, project_id, revenue_stream, management_entity_id, beneficiary_kind, status, updated_by,"
                + " updated_at) VALUES (?, ?, 'ticket_service_fee', ?, 'entity', 'active', 'tester', now())", "rsa_etd_" + s, project, entity);
        String feb = bill("02", 10_000);
        String mar = bill("03", 10_000);
        approveAll(JsonPath.read(rate("etd-rc-" + s, entity, "ticket", "cultural_fee", false, 0, "2026-03-01").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.data.review_id"));

        assertThat(vatRates(feb)).containsEntry("ticket", 600);
        assertThat(culturalFeeCent(feb)).isEqualTo(300L);
        assertThat(vatRates(mar)).containsEntry("ticket", 600);
        assertThat(culturalFeeCent(mar)).isZero();
    }

    /**
     * 233-R2 读取口径（BSM:521）：平台默认（不填主体）可分配池分账增值税新版本 7% 自 2026-03-01 生效后，没有主体特例的主体 3 月开票按 7%、2 月仍按出厂种子 6%
     * （出厂种子生效日为空，排在有生效日的新版本之后）。修复前 404：断言红。
     */
    @Test
    void platformDefaultVersionOverridesSeedFromEffectiveDate() throws Exception {
        String own = merchant("etd_m1_" + s, entity);
        String feb = bill("02", 0, own);
        String mar = bill("03", 0, own);
        approveAll(JsonPath.read(rate("etd-rd-" + s, null, "spv_service", "vat", true, 700, "2026-03-01").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.business_entity_id").doesNotExist())
                .andReturn().getResponse().getContentAsString(), "$.data.review_id"));

        assertThat(vatRates(feb)).containsEntry(own, 600);
        assertThat(vatRates(mar)).containsEntry(own, 700);
    }

    /**
     * 第十八批 AR 段第 233 条 233-R5（BSM:1760「税费金额分三个阶段分别保存、互不覆盖：系统计算金额、实际申报金额、实际缴纳金额；申报差异等于实际申报金额减
     * 系统计算金额……申报金额高于系统计算金额的生成补提调整单、低于的生成冲回调整单」「增值税、文化事业建设费等流转税按申报差异生成补提／冲回调整单，差异保留、
     * 不做拉平」；BSM:521「每一笔单据在生成时就把所用配置版本固化在单上」；第十八批裁决 59「附加税跟流转税走补提或冲回调整单；其他税费和企业所得税一样，按季与
     * 财税软件核对、以财税软件为准拉平」）：三家主体 2031-05 各有已确认月度分账单上归属本主体的餐饮份额 106000 元、餐饮增值税 6%（主体特例版本）。按前端
     * 蛇形键登记增值税 6100 元：系统计算 6000 元、申报差异 +100 元、差异处理为补提／冲回调整单，计算依据带所用税率版本 id，复核摘要写明系统计算金额；逐级批准后
     * 出一张已生效补提调整单 100 元、计入批准当月。另一家申报 5900 元出冲回 100 元，申报 6000 元不出单。附加税比例 12% 时系统计算 720 元（以系统计算增值税
     * 6000 元为基数）；其他税费没有系统计算金额、差异处理为按季核对拉平。修复前没有这些字段：断言红。
     */
    @Test
    void transferTaxSystemAmountDrivesSupplementOrReversalAdjustmentOnApproval() throws Exception {
        String a = cateringEntity("a", 10_600_000);
        String b = cateringEntity("b", 10_600_000);
        String c = cateringEntity("c", 10_600_000);
        String vatA = taxRate(a, "catering", "vat", 600);
        taxRate(b, "catering", "vat", 600);
        taxRate(c, "catering", "vat", 600);
        taxRate(a, "catering", "surcharge", 1200);

        String body = declareFor(a, "etd-s1-" + s, "vat", "2031-05", "2031-05", 610_000)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.system_calculated_amount_cent").value(600_000))
                .andExpect(jsonPath("$.data.declaration_difference_cent").value(10_000))
                .andExpect(jsonPath("$.data.difference_mechanism").value("adjustment"))
                .andExpect(jsonPath("$.data.calculation_basis.length()").value(1))
                .andExpect(jsonPath("$.data.calculation_basis[0].month").value("2031-05"))
                .andExpect(jsonPath("$.data.calculation_basis[0].income_type").value("catering"))
                .andExpect(jsonPath("$.data.calculation_basis[0].income_cent").value(10_600_000))
                .andExpect(jsonPath("$.data.calculation_basis[0].rate_bps").value(600))
                .andExpect(jsonPath("$.data.calculation_basis[0].rate_version_id").value(vatA))
                .andExpect(jsonPath("$.data.calculation_basis[0].tax_amount_cent").value(600_000))
                .andReturn().getResponse().getContentAsString();
        String review = JsonPath.read(body, "$.data.review_id");
        assertThat(jdbc.queryForObject("SELECT reason FROM supervisor_review_decisions WHERE review_id = ?", String.class, review))
                .contains("系统计算金额 6000.00 元", "申报差异 100.00 元", "生效后生成补提调整单 100.00 元");
        approveAll(review);
        String id = JsonPath.read(body, "$.data.id");
        String approvalMonth = jdbc.queryForObject("SELECT to_char(approved_at AT TIME ZONE 'Asia/Shanghai', 'YYYY-MM') FROM entity_tax_declarations"
                + " WHERE id = ?", String.class, id);
        listedFor(a).andExpect(jsonPath("$.data.items[0].id").value(id))
                .andExpect(jsonPath("$.data.items[0].declared_amount_cent").value(610_000))
                .andExpect(jsonPath("$.data.items[0].system_calculated_amount_cent").value(600_000))
                .andExpect(jsonPath("$.data.items[0].adjustments.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].adjustments[0].kind").value("supplement"))
                .andExpect(jsonPath("$.data.items[0].adjustments[0].amount_cent").value(10_000))
                .andExpect(jsonPath("$.data.items[0].adjustments[0].status").value("approved"))
                .andExpect(jsonPath("$.data.items[0].adjustments[0].accounting_month").value(approvalMonth))
                .andExpect(jsonPath("$.data.items[0].adjustments[0].origin_period_start_month").value("2031-05"));

        approveAll(JsonPath.read(declareFor(b, "etd-s2-" + s, "vat", "2031-05", "2031-05", 590_000).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.declaration_difference_cent").value(-10_000))
                .andReturn().getResponse().getContentAsString(), "$.data.review_id"));
        listedFor(b).andExpect(jsonPath("$.data.items[0].adjustments.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].adjustments[0].kind").value("reversal"))
                .andExpect(jsonPath("$.data.items[0].adjustments[0].amount_cent").value(10_000));
        approveAll(JsonPath.read(declareFor(c, "etd-s3-" + s, "vat", "2031-05", "2031-05", 600_000).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.declaration_difference_cent").value(0))
                .andReturn().getResponse().getContentAsString(), "$.data.review_id"));
        listedFor(c).andExpect(jsonPath("$.data.items[0].status").value("approved"))
                .andExpect(jsonPath("$.data.items[0].adjustments.length()").value(0));

        declareFor(a, "etd-s4-" + s, "surcharge", "2031-05", "2031-05", 72_000).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.system_calculated_amount_cent").value(72_000))
                .andExpect(jsonPath("$.data.declaration_difference_cent").value(0))
                .andExpect(jsonPath("$.data.calculation_basis[0].rate_bps").value(1200))
                .andExpect(jsonPath("$.data.calculation_basis[0].vat_amount_cent").value(600_000))
                .andExpect(jsonPath("$.data.calculation_basis[0].vat_rate_version_id").value(vatA));
        declareFor(c, "etd-s5-" + s, "other", "2031-05", "2031-05", 1_000).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.difference_mechanism").value("alignment"))
                .andExpect(jsonPath("$.data.system_calculated_amount_cent").doesNotExist());
    }

    /**
     * 第十八批 AR 段第 233 条 233-R4（ENUM:201「每个主体 × 每类收入」；BSM:1760「五家主体各自的收入、成本与利润，由平台汇总计算……平台据此与自身的内部结算
     * 数据合并计算」；第十八批裁决 60「收入＝按收入归属配置归到该主体的各类收入（含税实收）……酒吧经营主体沿用持股分红的税前口径」；BSM:1713「到期未核销部分
     * 按票种所属经营域归娱乐平台公司」、1716「相应门票综合服务费归娱乐平台公司」、1721「演出票｜传媒公司」、1722「场地租金｜对应城市项目公司」、1723「向商户
     * 供货的货款｜中央餐饮公司｜停车费｜城市项目公司」）：一个综合体 2031-05 造齐各来源，经 233-R5 登记增值税后的计算依据核各主体各收入类型——酒吧公司（名下
     * 一家酒吧）结账商品 1000 元退 100 元得餐饮 900 元；娱乐平台公司门票 2350 元（酒吧份额 2000、综合服务费 50、到期未核销 300）；游乐公司游乐 750 元（份额
     * 800 扣出综合服务费 50）；传媒公司演出 1000 元；城市项目公司租金 3000 元、停车 20 元；中央餐饮公司供货货款 500 元（借调买断 200 元不计）。2031-07 分账单
     * 未确认时娱乐平台公司与酒吧公司登记都 409 TICKET_SETTLEMENT_PENDING 且不落行。修复前没有计算依据：断言红。
     */
    @Test
    void entityIncomeCollectsEachSourceIntoItsEntityAndWaitsForConfirmedMonthlySettlement() throws Exception {
        String platformCo = newEntity("pf", "management_company", "娱乐平台公司");
        String amusementCo = newEntity("am", "management_company", "游乐公司");
        String mediaCo = newEntity("md", "management_company", "传媒公司");
        String cityCo = newEntity("ct", "project_company", "城市项目公司");
        String cateringCo = newEntity("cc", "management_company", "中央餐饮公司");
        String barCo = newEntity("br", "management_company", "酒吧公司");
        for (String[] a : List.of(new String[]{"venue_rent", cityCo}, new String[]{"supply_goods", cateringCo})) {
            jdbc.update("INSERT INTO revenue_stream_attribution (id, project_id, revenue_stream, management_entity_id, beneficiary_kind, status, updated_by,"
                    + " updated_at) VALUES (?, ?, ?, ?, 'entity', 'active', 'tester', now())", "rsa_etd_" + a[0] + "_" + s, project, a[0], a[1]);
        }
        String bar = "etd_mbar_" + s;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, business_entity_id, project_id, status, created_at, updated_at)"
                + " VALUES (?, ?, 'bar', ?, ?, 'active', now(), now())", bar, "税费收入酒吧" + s, barCo, project);
        barSale(bar, 100_000, 10_000);
        String bill = "sb_etd_inc_" + s;
        jdbc.update("INSERT INTO settlement_bills (id, project_id, period_start, period_end, status, fee_beneficiary_entity_id)"
                + " VALUES (?, ?, DATE '2031-05-01', DATE '2031-05-31', 'confirmed', ?)", bill, project, platformCo);
        share(bill, "bar", 200_000, 0, platformCo);
        share(bill, "amusement", 80_000, 5_000, amusementCo);
        jdbc.update("INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id, entitlement_type, verified_count,"
                + " total_times, num_people, allocated_revenue_cent, verified_amount_cent, unverified_amount_cent, beneficiary_type, beneficiary_entity_id,"
                + " project_id, settlement_status, entitlement_valid_to, business_at, discount_amount_cent, created_at, updated_at)"
                + " VALUES (?, ?, 'unverified_expired', ?, 'pass', 0, 1, 1, 30000, 0, 30000, 'entity', ?, ?, 'confirmed',"
                + " TIMESTAMPTZ '2031-05-16 00:00:00+08', TIMESTAMPTZ '2031-05-16 14:00:00+08', 0, now(), now())",
                "st_etd_" + s, "ST-ETD-" + s, "ent_etd_" + s, platformCo, project);
        String media = "etd_mmd_" + s;
        String show = "mps_etd_" + s;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, business_entity_id, project_id, status, created_at, updated_at)"
                + " VALUES (?, ?, 'media', ?, ?, 'active', now(), now())", media, "税费收入传媒" + s, mediaCo, project);
        jdbc.update("INSERT INTO media_performance_shows (id, show_group_id, project_id, bar_merchant_id, media_merchant_id, show_kind, show_date, created_by)"
                + " VALUES (?, ?, ?, ?, ?, 'bar_show', DATE '2031-05-18', 'usr_etd')", show, show, project, bar, media);
        jdbc.update("INSERT INTO media_performance_settlements (id, show_id, fee_kind, fixed_amount_cent, amount_cent, payer_merchant_id, payee_entity_id,"
                + " status, ar_ap_item_id, created_by) VALUES (?, ?, 'fixed_fee', 100000, 100000, ?, ?, 'posted', ?, 'usr_etd')", "mpss_etd_" + s, show,
                bar, mediaCo, arAp("payable", mediaCo, "media_performance_settlement", 100_000, "2031-05-18"));
        paid(arAp("receivable", "etd_mrent_" + s, "contract_rent", 300_000, "2031-05-05"), 300_000, "2031-05-10 14:00:00+08");
        parking(cityCo, 2_000);
        transfer("supply", "supply_entity", cateringCo, bar, 50_000);
        transfer("lending", "merchant", "etd_mlend_" + s, bar, 20_000);

        assertThat(incomes(barCo)).isEqualTo(Map.of("catering", 90_000L));
        assertThat(incomes(platformCo)).isEqualTo(Map.of("ticket", 235_000L));
        assertThat(incomes(amusementCo)).isEqualTo(Map.of("game", 75_000L));
        assertThat(incomes(mediaCo)).isEqualTo(Map.of("show", 100_000L));
        assertThat(incomes(cityCo)).isEqualTo(Map.of("rent", 300_000L, "parking", 2_000L));
        assertThat(incomes(cateringCo)).isEqualTo(Map.of("goods", 50_000L));

        for (String e : List.of(platformCo, barCo)) {
            declareFor(e, "etd-ip-" + e, "vat", "2031-07", "2031-07", 100).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail_code").value("TICKET_SETTLEMENT_PENDING"));
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM entity_tax_declarations WHERE business_entity_id IN (?, ?) AND period_start_month = '2031-07'",
                Integer.class, platformCo, barCo)).isZero();
    }

    /**
     * 第十八批 AR 段第 233 条 233-R6（BSM:1760「娱乐平台公司、游乐公司、传媒公司、中央餐饮公司、城市项目公司五家主体各自的收入、成本与利润，由平台汇总计算；
     * 其中涉税的正式核算数据由专业财税软件回传关键信息，平台据此与自身的内部结算数据合并计算」；第十八批裁决 60「成本＝该主体在平台内作为付款方的内部结算应付
     * （提成拨付、渠道费、赠送权益兑付等）＋该主体作为发放主体的工资单税前应发；利润＝收入−成本……酒吧经营主体沿用持股分红的税前口径（收入−销货成本−费用）」）：
     * 娱乐平台公司 2031-05 已确认月度分账单酒吧份额毛额 50000 元（渠道费 2000 元、通路费 500 元）、同单池内分配 30000 元、单一酒吧门票直付 5000 元（不计），
     * 发放的工资单税前应发 10000 元，域间份额应付 4000 元与下月过账的票务分账应付都不计：收入 50000 元、成本 42500 元、利润 7500 元。酒吧公司（名下一家酒吧）
     * 商品 1000 元退 100 元、销货成本 300 元、能耗费 200 元：按持股分红税前口径利润 400 元。三个参数缺一 400。修复前没有该接口：404，状态断言红。
     */
    @Test
    void entityProfitSumsPlatformInternalSettlementCostsAndBarEntityFollowsDividendPretaxBasis() throws Exception {
        String platformCo = newEntity("pp", "management_company", "娱乐平台公司");
        String bill = "sb_etd_pp_" + s;
        jdbc.update("INSERT INTO settlement_bills (id, project_id, period_start, period_end, status) VALUES (?, ?, DATE '2031-05-01', DATE '2031-05-31',"
                + " 'confirmed')", bill, project);
        jdbc.update("INSERT INTO settlement_bill_format_shares (bill_id, business_format, gross_revenue_cent, channel_fee_cent, route_fee_cent,"
                + " beneficiary_kind, beneficiary_entity_id) VALUES (?, 'bar', 5000000, 200000, 50000, 'entity', ?)", bill, platformCo);
        String poolBar = merchant("etd_mpool_" + s, null);
        jdbc.update("INSERT INTO settlement_items (id, bill_id, merchant_id, distribution_method, calculated_amount_cent, final_amount_cent, status)"
                + " VALUES (?, ?, ?, 'by_fixed_amount', 3000000, 3000000, 'draft'), (?, ?, ?, 'exclusive_full', 500000, 500000, 'draft')",
                "si_etd_pool_" + s, bill, poolBar, "si_etd_excl_" + s, bill, poolBar);
        payroll(platformCo, "2031-05", 1_000_000);
        jdbc.update("UPDATE ar_ap_items SET accounting_period = '2031-05' WHERE id = ?",
                arAp("payable", platformCo, "pass_domain_share", 400_000, "2031-05-20"));
        jdbc.update("UPDATE ar_ap_items SET accounting_period = '2031-06' WHERE id = ?",
                arAp("payable", poolBar, "ticket_distribution", 3_000_000, "2031-06-05"));

        String body = profitOf(platformCo, "2031-05", "2031-05").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.basis").value("platform"))
                .andExpect(jsonPath("$.data.revenue_cent").value(5_000_000))
                .andExpect(jsonPath("$.data.revenue_items.length()").value(1))
                .andExpect(jsonPath("$.data.revenue_items[0].income_type").value("ticket"))
                .andExpect(jsonPath("$.data.cost_cent").value(4_250_000))
                .andExpect(jsonPath("$.data.profit_cent").value(750_000))
                .andReturn().getResponse().getContentAsString();
        assertThat(costs(body)).isEqualTo(Map.of("pool_distribution", 3_000_000L, "channel_fee", 250_000L, "payroll", 1_000_000L));

        String barCo = newEntity("pb", "management_company", "酒吧公司");
        String bar = "etd_mpb_" + s;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, business_entity_id, project_id, status, created_at, updated_at)"
                + " VALUES (?, ?, 'bar', ?, ?, 'active', now(), now())", bar, "税费利润酒吧" + s, barCo, project);
        barSale(bar, 100_000, 10_000);
        jdbc.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, created_at, updated_at)"
                + " VALUES (?, '税费利润酒水', 'drink', '瓶', 'active', now(), now())", "item_etd_" + s);
        jdbc.update("INSERT INTO inventory_stock_movements (id, movement_type, item_id, source_location_id, target_location_id, quantity, merchant_id,"
                + " unit_cost_cent, total_cost_cent, status, created_at) VALUES (?, 'outbound', ?, ?, NULL, 1, ?, 30000, 30000, 'completed',"
                + " TIMESTAMPTZ '2031-05-15 15:00:00+08')", "mv_etd_" + s, "item_etd_" + s, "loc_etd_" + s, bar);
        jdbc.update("INSERT INTO merchant_expense_entries (id, merchant_id, period, expense_type, amount_cent, source, note, operator_user_id, created_at)"
                + " VALUES (?, ?, '2031-05', 'energy', 20000, 'manual', '税费利润能耗费', 'usr_etd', now())", "mex_etd_" + s, bar);
        body = profitOf(barCo, "2031-05", "2031-05").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.basis").value("dividend"))
                .andExpect(jsonPath("$.data.revenue_cent").value(90_000))
                .andExpect(jsonPath("$.data.cost_cent").value(50_000))
                .andExpect(jsonPath("$.data.profit_cent").value(40_000))
                .andReturn().getResponse().getContentAsString();
        assertThat(costs(body)).isEqualTo(Map.of("cost_inventory", 30_000L, "expense_energy", 20_000L));

        mockMvc.perform(get("/api/v1/finance/entity-taxes/profits").param("business_entity_id", barCo).param("start_month", "2031-05").with(finance))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    /**
     * 第十八批 AR 段第 233 条 233-R7（BSM:1760「企业所得税等税种：系统内配置税率比例并据以生成一版计算结果……两套数据须核对，以财税软件的结果为准……税务按季度
     * 处理」「同一税种只能走其中一套」；第十八批裁决 59「其他税费和企业所得税一样，按季与财税软件核对、以财税软件为准拉平」；裁决 60「企业所得税系统计算＝利润×
     * 配置比例」）：主体一季度利润 400000 元、企业所得税比例 25% 已生效，按前端蛇形键登记 2026-01～2026-03 企业所得税 95000 元：系统计算 100000 元、申报差异
     * −5000 元、差异处理为按季核对拉平，计算依据带收入、利润与所用税率版本，复核摘要写明以财税软件结果为准；逐级批准后拉平后金额 95000 元，不出补提或冲回调整单。
     * 其他税费没有系统计算金额，批准后同样不出单。利润为负时系统计算为 0。修复前企业所得税没有系统计算金额：断言红。
     */
    @Test
    void corporateIncomeAlignsToTaxSoftwareResultWithoutSupplementOrReversal() throws Exception {
        String co = newEntity("ci", "management_company", "游乐公司");
        share("sb_etd_fx1_" + s, "amusement", 40_000_000, 0, co);
        String rateId = taxRate(co, "all", "corporate_income", 2500);
        String body = declareFor(co, "etd-ci1-" + s, "corporate_income", "2026-01", "2026-03", 9_500_000).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.system_calculated_amount_cent").value(10_000_000))
                .andExpect(jsonPath("$.data.declaration_difference_cent").value(-500_000))
                .andExpect(jsonPath("$.data.difference_mechanism").value("alignment"))
                .andExpect(jsonPath("$.data.calculation_basis.length()").value(1))
                .andExpect(jsonPath("$.data.calculation_basis[0].income_type").value("all"))
                .andExpect(jsonPath("$.data.calculation_basis[0].rate_bps").value(2500))
                .andExpect(jsonPath("$.data.calculation_basis[0].rate_version_id").value(rateId))
                .andExpect(jsonPath("$.data.calculation_basis[0].tax_amount_cent").value(10_000_000))
                .andExpect(jsonPath("$.data.calculation_basis[0].profit.revenue_cent").value(40_000_000))
                .andExpect(jsonPath("$.data.calculation_basis[0].profit.profit_cent").value(40_000_000))
                .andExpect(jsonPath("$.data.aligned_amount_cent").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String review = JsonPath.read(body, "$.data.review_id");
        assertThat(jdbc.queryForObject("SELECT reason FROM supervisor_review_decisions WHERE review_id = ?", String.class, review))
                .contains("按季核对：系统计算 100000.00 元，财税软件结果（本次申报）95000.00 元，差异 -5000.00 元，以财税软件结果为准拉平");
        approveAll(review);
        approveAll(JsonPath.read(declareFor(co, "etd-ci2-" + s, "other", "2026-01", "2026-03", 100_000).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.system_calculated_amount_cent").doesNotExist())
                .andReturn().getResponse().getContentAsString(), "$.data.review_id"));
        for (String[] tax : List.of(new String[]{"corporate_income", "9500000"}, new String[]{"other", "100000"})) {
            mockMvc.perform(get("/api/v1/finance/entity-taxes").param("business_entity_id", co).param("tax_type", tax[0]).with(finance))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items[0].status").value("approved"))
                    .andExpect(jsonPath("$.data.items[0].aligned_amount_cent").value(Long.parseLong(tax[1])))
                    .andExpect(jsonPath("$.data.items[0].adjustments.length()").value(0));
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM entity_tax_adjustments WHERE business_entity_id = ?", Integer.class, co)).isZero();

        String loss = newEntity("cl", "management_company", "传媒公司");
        payroll(loss, "2026-02", 1_000_000);
        taxRate(loss, "all", "corporate_income", 2500);
        declareFor(loss, "etd-ci3-" + s, "corporate_income", "2026-01", "2026-03", 0).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.system_calculated_amount_cent").value(0))
                .andExpect(jsonPath("$.data.calculation_basis[0].profit.profit_cent").value(-1_000_000));
    }

    /**
     * 第十八批 AR 段第 233 条收尾复核（BSM:1760「已封账历史账单不回改，跨月退款和调整在当期生成冲正」；BSM:1145「已形成结算需要调整时，关联原记录并按既有
     * 历史调整规则办理，不覆盖原结果」）：2031-05-18 演出固定演出费 1000 元、门票分成 300 元出账，登记传媒公司 2031-05 增值税后，2031-06-20 两项调整为 800 元、
     * 200 元（原行改已调整，原应付追加冲正行，新应付另挂，冲正与新应付都落调整当日）：传媒公司 5 月演出收入仍 1300 元、6 月为新额−原额 −300 元；付门票分成的
     * 娱乐平台公司 5 月成本 300 元、6 月 −100 元。修复前只取已出账结算行：5 月取不到原行、6 月只取新行，断言红。
     */
    @Test
    void mediaSettlementAdjustedNextMonthKeepsOriginalMonthAndCountsDifferenceInAdjustmentMonth() throws Exception {
        String mediaCo = newEntity("ma", "management_company", "传媒公司");
        String platformCo = newEntity("mp", "management_company", "娱乐平台公司");
        for (String month : List.of("05", "06")) {
            LocalDate first = LocalDate.parse("2031-" + month + "-01");
            jdbc.update("INSERT INTO settlement_bills (id, project_id, period_start, period_end, status) VALUES (?, ?, ?, ?, 'confirmed')",
                    "sb_etd_ma" + month + "_" + s, "etd_prjma_" + s, first, first.withDayOfMonth(first.lengthOfMonth()));
        }
        String bar = merchant("etd_mabar_" + s, null);
        String media = "etd_mamd_" + s;
        String show = "mps_etd_ma_" + s;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, business_entity_id, project_id, status, created_at, updated_at)"
                + " VALUES (?, ?, 'media', ?, ?, 'active', now(), now())", media, "税费调整传媒" + s, mediaCo, project);
        jdbc.update("INSERT INTO media_performance_shows (id, show_group_id, project_id, bar_merchant_id, media_merchant_id, show_kind, show_date, created_by)"
                + " VALUES (?, ?, ?, ?, ?, 'bar_show', DATE '2031-05-18', 'usr_etd')", show, show, project, bar, media);
        String fixed = mediaPosted(show, "fixed_fee", 100_000, bar, null, mediaCo, "2031-05-18", null);
        String ticketShare = mediaPosted(show, "ticket_share", 30_000, null, platformCo, mediaCo, "2031-05-18", null);
        assertThat(incomes(mediaCo)).as("原月登记申报").isEqualTo(Map.of("show", 130_000L));

        mediaAdjusted(fixed, 80_000, "2031-06-20");
        mediaAdjusted(ticketShare, 20_000, "2031-06-20");

        for (String[] m : List.of(new String[]{"2031-05", "130000"}, new String[]{"2031-06", "-30000"})) {
            profitOf(mediaCo, m[0], m[0]).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.revenue_items.length()").value(1))
                    .andExpect(jsonPath("$.data.revenue_items[0].income_type").value("show"))
                    .andExpect(jsonPath("$.data.revenue_cent").value(Long.parseLong(m[1])));
        }
        assertThat(costs(profitOf(platformCo, "2031-05", "2031-05").andExpect(status().isOk()).andReturn().getResponse().getContentAsString()))
                .isEqualTo(Map.of("media_ticket_share", 30_000L));
        assertThat(costs(profitOf(platformCo, "2031-06", "2031-06").andExpect(status().isOk()).andReturn().getResponse().getContentAsString()))
                .isEqualTo(Map.of("media_ticket_share", -10_000L));
    }

    /**
     * 第十八批 AR 段第 233 条收尾复核（BSM:1723「向商户供货的货款｜中央餐饮公司」；BSM:1212「财务复核金额后办理补发、退回、货款调整或责任损失，不重复收款」；
     * 第十八批裁决 60「收入＝按收入归属配置归到该主体的各类收入（含税实收）」）：门店要货货款 500 元签收后两次「货款调整」（调减 100 元、再调减 50 元），每次
     * 追加冲正行与重述行、不写来源记录；门店 5 月 12 日在第一条重述行付 100 元、5 月 20 日在第二条重述行付 250 元：中央餐饮公司 5 月供货货款收入 350 元。
     * 修复前只认签收时那笔带来源记录的应付：断言红。
     */
    @Test
    void supplyPaymentsOnRestatedPayablesAfterPaymentAdjustmentsCountAsGoodsIncome() throws Exception {
        String cateringCo = newEntity("sg", "management_company", "中央餐饮公司");
        jdbc.update("INSERT INTO revenue_stream_attribution (id, project_id, revenue_stream, management_entity_id, beneficiary_kind, status, updated_by,"
                + " updated_at) VALUES (?, ?, 'supply_goods', ?, 'entity', 'active', 'tester', now())", "rsa_etd_sg_" + s, project, cateringCo);
        jdbc.update("INSERT INTO settlement_bills (id, project_id, period_start, period_end, status) VALUES (?, ?, DATE '2031-05-01', DATE '2031-05-31',"
                + " 'confirmed')", "sb_etd_sg_" + s, "etd_prjsg_" + s);
        String store = merchant("etd_msg_" + s, null);
        String first = restate(transferPayable("supply", "supply_entity", cateringCo, store, 50_000), 40_000);
        paid(first, 10_000, "2031-05-12 14:00:00+08");
        String second = restate(first, 35_000);
        paid(second, 25_000, "2031-05-20 14:00:00+08");

        assertThat(incomes(cateringCo)).isEqualTo(Map.of("goods", 35_000L));
    }

    /**
     * 第十八批 AR 段第 233 条收尾复核（决策台账 FS-021「单独列追收、按原月重算」；BSM:1713「娱乐平台公司不取得已核销门票的收入」；第十八批裁决 60「成本＝该主体
     * 在平台内作为付款方的内部结算应付……利润＝收入−成本」）：2031-06 已确认月度分账单上，原月已过账后撤销的有价餐饮权益在餐饮份额行列追收（域间份额 −120 元、
     * 按内部结算价应直付额 −100 元），另有分池追收明细 −50 元与单一酒吧门票直付的追收明细 −200 元：城市项目公司 6 月收入 −120 元与成本 −100 元同冲；
     * 娱乐平台公司池内分配只计分池追收 −50 元，单店直付追收不计。修复前收入不计追收、池内分配计入单店直付追收：断言红。
     */
    @Test
    void crossMonthRecoveriesCutShareIncomeWithCostAndKeepSingleBarRecoveryOutOfPoolCost() throws Exception {
        String platformCo = newEntity("rp", "management_company", "娱乐平台公司");
        String cityCo = newEntity("rc", "project_company", "城市项目公司");
        String bill = "sb_etd_rcv_" + s;
        jdbc.update("INSERT INTO settlement_bills (id, project_id, period_start, period_end, status) VALUES (?, ?, DATE '2031-06-01', DATE '2031-06-30',"
                + " 'confirmed')", bill, project);
        share(bill, "bar", 100_000, 0, platformCo);
        jdbc.update("INSERT INTO settlement_bill_format_shares (bill_id, business_format, recovery_net_cent, recovery_internal_settled_cent, beneficiary_kind,"
                + " beneficiary_entity_id) VALUES (?, 'catering', -12000, -10000, 'entity', ?)", bill, cityCo);
        String poolBar = merchant("etd_mrcv_" + s, null);
        jdbc.update("INSERT INTO settlement_items (id, bill_id, merchant_id, distribution_method, calculated_amount_cent, final_amount_cent, status)"
                + " VALUES (?, ?, ?, 'pooled_recovery', -5000, -5000, 'draft'), (?, ?, ?, 'exclusive_recovery', -20000, -20000, 'draft')",
                "si_etd_prcv_" + s, bill, poolBar, "si_etd_ercv_" + s, bill, poolBar);

        String city = profitOf(cityCo, "2031-06", "2031-06").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revenue_cent").value(-12_000))
                .andExpect(jsonPath("$.data.revenue_items[0].income_type").value("catering"))
                .andExpect(jsonPath("$.data.cost_cent").value(-10_000))
                .andExpect(jsonPath("$.data.profit_cent").value(-2_000))
                .andReturn().getResponse().getContentAsString();
        assertThat(costs(city)).isEqualTo(Map.of("exclusive_direct_settlement", -10_000L));
        String platform = profitOf(platformCo, "2031-06", "2031-06").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revenue_cent").value(100_000))
                .andReturn().getResponse().getContentAsString();
        assertThat(costs(platform)).isEqualTo(Map.of("pool_distribution", -5_000L));
    }

    /**
     * 第十八批 AR 段第 233 条收尾复核同根（同意见 5 的明细分类口径；FS-021-R2 单店直付追收即单一酒吧门票直付额的追收）：酒吧经营主体口径下，2031-05 已确认
     * 月度分账单本店单一酒吧门票直付 100 元、其追收明细 exclusive_recovery −20 元、池分账 30 元：门票收入（ticket）80 元，可分配池分账（spv_service）30 元。
     * 修复前追收明细按非直付归进池分账：断言红。
     */
    @Test
    void barEntitySingleBarTicketRecoveryStaysInTicketIncome() throws Exception {
        String barCo = newEntity("bt", "management_company", "酒吧公司");
        String bar = "etd_mbt_" + s;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, business_entity_id, project_id, status, created_at, updated_at)"
                + " VALUES (?, ?, 'bar', ?, ?, 'active', now(), now())", bar, "税费追收酒吧" + s, barCo, project);
        String bill = "sb_etd_bt_" + s;
        jdbc.update("INSERT INTO settlement_bills (id, project_id, period_start, period_end, status) VALUES (?, ?, DATE '2031-05-01', DATE '2031-05-31',"
                + " 'confirmed')", bill, project);
        jdbc.update("INSERT INTO settlement_items (id, bill_id, merchant_id, distribution_method, calculated_amount_cent, final_amount_cent, status)"
                + " VALUES (?, ?, ?, 'exclusive_full', 10000, 10000, 'draft'), (?, ?, ?, 'exclusive_recovery', -2000, -2000, 'draft'),"
                + " (?, ?, ?, 'by_fixed_amount', 3000, 3000, 'draft')",
                "si_etd_btx_" + s, bill, bar, "si_etd_btr_" + s, bill, bar, "si_etd_btf_" + s, bill, bar);

        assertThat(incomes(barCo)).isEqualTo(Map.of("ticket", 8_000L, "spv_service", 3_000L));
    }

    // ---------- helpers ----------

    private String newEntity(String tag, String role, String name) {
        String id = "etd_be_" + tag + "_" + s;
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at) VALUES (?, ?, ?, 'active', now(), now())",
                id, name + s, role);
        return id;
    }

    /** 233-R5：一家主体及其 2031-05 已确认月度分账单（单独项目）上归属本主体的餐饮份额（组合通票业态份额按收入归属定主体）。 */
    private String cateringEntity(String tag, long cent) {
        String id = newEntity(tag, "project_company", "税费餐饮份额主体" + tag);
        String bill = "sb_etd_c" + tag + "_" + s;
        jdbc.update("INSERT INTO settlement_bills (id, project_id, period_start, period_end, status) VALUES (?, ?, DATE '2031-05-01', DATE '2031-05-31',"
                + " 'confirmed')", bill, "etd_prjc" + tag + "_" + s);
        share(bill, "catering", cent, 0, id);
        return id;
    }

    private void share(String bill, String format, long gross, long compFee, String beneficiary) {
        jdbc.update("INSERT INTO settlement_bill_format_shares (bill_id, business_format, gross_revenue_cent, comp_service_fee_cent, beneficiary_kind,"
                + " beneficiary_entity_id) VALUES (?, ?, ?, ?, 'entity', ?)", bill, format, gross, compFee, beneficiary);
    }

    /** 主体特例税率版本（已生效、自始有效），返回版本 id。 */
    private String taxRate(String entityId, String incomeType, String taxType, int bps) {
        String id = "stc_etd" + (++seq) + "_" + s;
        jdbc.update("INSERT INTO settlement_tax_configs (id, business_entity_id, income_type, tax_type, applicable, rate_bps, status, is_draft)"
                + " VALUES (?, ?, ?, ?, TRUE, ?, 'active', FALSE)", id, entityId, incomeType, taxType, bps);
        return id;
    }

    /** 酒吧 bar 2031-05-15（北京 14:00）结清一张账单、商品实收 paid 分，05-20 退回储值卡退成商品 refund 分（写法同 BarPlatformTurnoverRentJdbcTests）。 */
    private void barSale(String bar, long paid, long refund) {
        String space = "etd_sp_" + s;
        String table = "etd_tb_" + s;
        String order = "etd_od_" + s;
        String billId = "etd_bb_" + s;
        String adjustment = "etd_rf_" + s;
        String at = "TIMESTAMPTZ '2031-05-15 14:00:00+08'";
        String refundAt = "TIMESTAMPTZ '2031-05-20 14:00:00+08'";
        jdbc.update("INSERT INTO spaces (id, project_id, space_name, space_type, area_square_meter, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, 'shop', 30, 'active', now(), now())", space, project, "税费酒吧卡座区" + s);
        jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, capacity, allowed_pass_free_guest_count,"
                + " min_consumption_cent, currency, status, created_at, updated_at, space_id)"
                + " VALUES (?, ?, ?, ?, '税费卡座', 'card_seat', 4, 0, 0, 'CNY', 'active', now(), now(), ?)", table, bar, bar, "ETD-" + s, space);
        jdbc.update("INSERT INTO bar_orders(id,bar_id,merchant_id,table_id,order_source,status,total_amount_cent,currency,created_at,updated_at)"
                + " VALUES(?,?,?,?,'staff','created',?,'CNY'," + at + "," + at + ")", order, bar, bar, table, paid);
        jdbc.update("INSERT INTO bar_bills(id,bill_no,order_id,bar_id,merchant_id,table_id,subtotal_amount_cent,discount_amount_cent,"
                + "deposit_deduct_amount_cent,payable_amount_cent,paid_amount_cent,currency,payment_status,settlement_status,tip_amount_cent,created_at,updated_at)"
                + " VALUES(?,?,?,?,?,?,?,0,0,?,?,'CNY','paid','settled',0," + at + "," + at + ")",
                billId, "ETD" + s, order, bar, bar, table, paid, paid - refund, paid);
        jdbc.update("INSERT INTO bar_bill_settlements(id,bill_id,settle_channel,settlement_amount_cent,merchant_id,settled_at)"
                + " VALUES(?,?,'aggregate_pay',?,?," + at + ")", "etd_bs_" + s, billId, paid, bar);
        jdbc.update("INSERT INTO bar_bill_adjustments(id,bill_id,adjustment_type,amount_cent,before_payable_amount_cent,after_payable_amount_cent,reason,"
                + "requested_by_user_id,approval_required,status,idempotency_key,created_at,refunded_at)"
                + " VALUES(?,?,'refund',?,?,?,'客人退部分商品',?,TRUE,'applied',?," + refundAt + "," + refundAt + ")",
                adjustment, billId, refund, paid, paid - refund, "usr_etd_fin_" + s, "etd-refund-" + s);
        jdbc.update("INSERT INTO bar_payroll_refund_lines(id,adjustment_id,collection_bill_id,source_bill_id,component_type,amount_cent)"
                + " VALUES(?,?,?,?,'goods',?)", "etd_prl_" + s, adjustment, billId, billId, refund);
    }

    private String arAp(String direction, String counterparty, String sourceType, long cent, String dueDate) {
        String id = "arap_etd" + (++seq) + "_" + s;
        jdbc.update("INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id, amount_cent, paid_amount_cent,"
                + " unpaid_amount_cent, due_date, status, created_at, updated_at, project_id) VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?::date, ?, now(), now(), ?)",
                id, id, direction, counterparty, sourceType, id, cent, cent, dueDate,
                "receivable".equals(direction) ? "pending_collection" : "pending_payment", project);
        return id;
    }

    private void paid(String item, long cent, String at) {
        jdbc.update("INSERT INTO ar_ap_payments (id, ar_ap_item_id, amount_cent, payment_method, payment_time, created_at)"
                + " VALUES (?, ?, ?, 'bank_transfer', ?::timestamptz, now())", "pay_" + item, item, cent, at);
    }

    /** 一笔已缴停车费（缴费时定格收款主体，同 ParkingDashboardJdbcIntegrationTests 写法）。 */
    private void parking(String receiver, long cent) {
        String session = "ps_etd_" + s;
        String customer = "cus_etd_" + s;
        String payment = "pp_etd_" + s;
        jdbc.update("INSERT INTO parking_sessions(id,project_id,vendor_session_ref,plate_no,vehicle_kind,entered_at,exited_at,status)"
                + " VALUES(?,?,?,'粤PETD1','temporary',TIMESTAMPTZ '2031-05-12 10:00:00+08',TIMESTAMPTZ '2031-05-12 13:00:00+08','exited')",
                session, project, "ref_etd_" + s);
        jdbc.update("INSERT INTO customers(id,phone,masked_phone,status,created_at,updated_at) VALUES(?,?,'***','active',now(),now())",
                customer, "enc:v1:" + customer);
        jdbc.update("INSERT INTO parking_payments(id,project_id,session_id,customer_id,amount_cent,payment_intent_id,status,paid_at,grace_until,"
                + "receiver_entity_id,idempotency_key) VALUES(?,?,?,?,?,?,'paid',TIMESTAMPTZ '2031-05-12 12:50:00+08',TIMESTAMPTZ '2031-05-12 13:10:00+08',?,?)",
                payment, project, session, customer, cent, "pi_" + payment, receiver, "idem_" + payment);
    }

    /** 门店应付的一笔调拨或借调货款（应付行带项目、来源回指流转单），2031-05-11 付讫。 */
    private void transfer(String type, String fromType, String fromId, String toMerchant, long cent) {
        paid(transferPayable(type, fromType, fromId, toMerchant, cent), cent, "2031-05-11 14:00:00+08");
    }

    /** 门店应付的一笔调拨或借调货款（应付行带项目、来源回指流转单），返回应付行。 */
    private String transferPayable(String type, String fromType, String fromId, String toMerchant, long cent) {
        String order = "itf_etd_" + type + "_" + s;
        jdbc.update("INSERT INTO inventory_transfer_orders (id, transfer_type, lending_mode, from_owner_type, from_owner_id, to_owner_type, to_owner_id, status)"
                + " VALUES (?, ?, ?, ?, ?, 'merchant', ?, 'completed')", order, type, "supply".equals(type) ? null : "purchase", fromType, fromId, toMerchant);
        String item = arAp("payable", fromId, "inventory_transfer", cent, "2031-05-11");
        jdbc.update("INSERT INTO ar_ap_item_sources (id, ar_ap_item_id, source_type, source_id, amount_cent, created_at)"
                + " VALUES (?, ?, 'inventory_transfer', ?, ?, now())", "src_" + item, item, order, cent);
        return item;
    }

    /** 差异处理「货款调整」批准（同 InventoryTransferFlowService 写法）：对当前有效货款追加冲正行与重述行，都回指它、沿用其来源、不写来源记录；返回重述行。 */
    private String restate(String old, long restatedCent) {
        String reversal = "arap_etd" + (++seq) + "_" + s;
        String restated = "arap_etd" + (++seq) + "_" + s;
        String copy = "INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id, amount_cent, paid_amount_cent, unpaid_amount_cent,"
                + " due_date, status, created_at, updated_at, merchant_id, project_id, origin_item_id) SELECT ?, ?, direction, counterparty_id, source_type, source_id,"
                + " %s, due_date, ?, now(), now(), merchant_id, project_id, id FROM ar_ap_items WHERE id = ?";
        jdbc.update(copy.formatted("-amount_cent, -paid_amount_cent, -unpaid_amount_cent"), reversal, reversal, "reversed", old);
        jdbc.update(copy.formatted(restatedCent + ", 0, " + restatedCent), restated, restated, "pending_payment", old);
        return restated;
    }

    /** 一条已出账的演出结算行及其应付（同 FinanceService.postMediaPerformancePayable：应付收方主体、来源回指结算行、到期日为出账营业日），返回结算行 id。 */
    private String mediaPosted(String show, String feeKind, long cent, String payerMerchant, String payerEntity, String payee, String day, String adjusts) {
        String id = "mpss_etd" + (++seq) + "_" + s;
        String item = arAp("payable", payee, "media_performance_settlement", cent, day);
        jdbc.update("UPDATE ar_ap_items SET source_id = ?, merchant_id = ? WHERE id = ?", id, payerMerchant, item);
        jdbc.update("INSERT INTO media_performance_settlements (id, show_id, fee_kind, amount_cent, payer_merchant_id, payer_entity_id, payee_entity_id, status,"
                + " ar_ap_item_id, adjusts_settlement_id, adjust_reason, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, 'posted', ?, ?, ?, 'usr_etd')",
                id, show, feeKind, cent, payerMerchant, payerEntity, payee, item, adjusts, adjusts == null ? null : "履约争议改判");
        return id;
    }

    /** 调整一条已出账结算行（同 MediaPerformanceSettlementService.adjustInternal）：原行改已调整，原应付追加冲正行，新金额另挂；冲正与新应付都落调整当日。 */
    private String mediaAdjusted(String original, long cent, String day) {
        Map<String, Object> o = jdbc.queryForMap("SELECT * FROM media_performance_settlements WHERE id = ?", original);
        jdbc.update("UPDATE media_performance_settlements SET status = 'adjusted' WHERE id = ?", original);
        String reversal = "arap_etd" + (++seq) + "_" + s;
        jdbc.update("INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id, amount_cent, paid_amount_cent, unpaid_amount_cent,"
                + " due_date, status, created_at, updated_at, merchant_id, project_id, origin_item_id) SELECT ?, ?, direction, counterparty_id, source_type, source_id,"
                + " -amount_cent, 0, -amount_cent, ?::date, 'reversed', now(), now(), merchant_id, project_id, id FROM ar_ap_items WHERE id = ?",
                reversal, reversal, day, o.get("ar_ap_item_id"));
        return mediaPosted((String) o.get("show_id"), (String) o.get("fee_kind"), cent, (String) o.get("payer_merchant_id"),
                (String) o.get("payer_entity_id"), (String) o.get("payee_entity_id"), day, original);
    }

    /** 233-R4 经 233-R5 的计算依据读某主体 2031-05 各收入类型的含税收入：按前端蛇形键登记一笔 0 元增值税申报（待审批）。 */
    private Map<String, Long> incomes(String entityId) throws Exception {
        List<Map<String, Object>> basis = JsonPath.read(declareFor(entityId, "etd-in-" + entityId, "vat", "2031-05", "2031-05", 0)
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.calculation_basis").isArray())
                .andReturn().getResponse().getContentAsString(), "$.data.calculation_basis");
        Map<String, Long> out = new HashMap<>();
        basis.forEach(row -> out.merge((String) row.get("income_type"), ((Number) row.get("income_cent")).longValue(), Long::sum));
        return out;
    }

    private String approvedDeclaration(String key, String taxType, String start, String end, long cent) throws Exception {
        String body = declare(key, taxType, start, end, cent).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        approveAll(JsonPath.read(body, "$.data.review_id"));
        return JsonPath.read(body, "$.data.id");
    }

    /** 财务、税务、法务三条线的审批人与最高确认各用自己的会话，经真实审批接口逐级批准。 */
    private void approveAll(String review) throws Exception {
        for (RequestPostProcessor reviewer : List.of(financeReviewer, taxReviewer, legalReviewer, master())) {
            approve(reviewer, review, "approved").andExpect(status().isOk());
        }
    }

    /** 233-R1 已生效行「登记实际缴纳」弹窗的真实请求形状（蛇形键）。 */
    private ResultActions pay(String id, String key, long paidCent, String breakdown) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/entity-taxes/" + id + "/payments").with(finance).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + key + "\",\"paid_amount_cent\":" + paidCent + ",\"difference_breakdown\":" + breakdown
                        + ",\"reason\":\"按完税凭证登记\",\"evidence_attachment_ids\":[]," + lines + "}"));
    }

    /** 233-R3 已生效行「生成差异调整单」弹窗的真实请求形状（蛇形键）。 */
    private ResultActions adjust(String id, String key, long amountCent) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/entity-taxes/" + id + "/adjustments").with(finance).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + key + "\",\"amount_cent\":" + amountCent
                        + ",\"reason\":\"更正申报表\",\"evidence_attachment_ids\":[]," + lines + "}"));
    }

    /** 233-R2「税率配置」页签「提交税率版本」弹窗的真实请求形状（蛇形键）；主体为空表示平台默认（不带 business_entity_id）。 */
    private ResultActions rate(String key, String entityId, String incomeType, String taxType, boolean applicable, int bps, String from)
            throws Exception {
        return mockMvc.perform(post("/api/v1/finance/entity-taxes/rates").with(finance).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + key + "\"," + (entityId == null ? "" : "\"business_entity_id\":\"" + entityId + "\",")
                        + "\"income_type\":\"" + incomeType + "\",\"tax_type\":\"" + taxType + "\",\"applicable\":" + applicable + ",\"rate_bps\":" + bps
                        + ",\"effective_from\":\"" + from + "\",\"reason\":\"按税务口径调整税率\",\"evidence_attachment_ids\":[]," + lines + "}"));
    }

    /** 酒吧商户，结算主体为 entityId；返回商户号。 */
    private String merchant(String id, String entityId) {
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, settlement_entity_id, status, created_at, updated_at)"
                + " VALUES (?, ?, 'bar', ?, 'active', now(), now())", id, "税率酒吧" + id, entityId);
        return id;
    }

    /** 2026 年该月已确认月度结算单：门票综合服务费 compCent，每个商户一条可分配池分账 530.00 元。 */
    private String bill(String month, long compCent, String... merchants) {
        String id = "sb_etd_" + month + "_" + s;
        LocalDate start = LocalDate.parse("2026-" + month + "-01");
        jdbc.update("INSERT INTO settlement_bills (id, project_id, period_start, period_end, comp_service_fee_exclusive_cent, distribution_method, status)"
                + " VALUES (?, ?, ?, ?, ?, 'by_rank_tier', 'confirmed')", id, project, start, start.withDayOfMonth(start.lengthOfMonth()), compCent);
        for (String m : merchants) {
            jdbc.update("INSERT INTO settlement_items (id, bill_id, merchant_id, distribution_method, calculated_amount_cent, final_amount_cent, status)"
                    + " VALUES (?, ?, ?, 'by_rank_tier', 53000, 53000, 'draft')", "si_" + m + "_" + month, id, m);
        }
        return id;
    }

    /** 经真实开票清单生成接口出条目，返回各条目的增值税率（分账行键为商户号，单头级键为收入类型）。 */
    private Map<String, Integer> vatRates(String billId) throws Exception {
        mockMvc.perform(post("/api/v1/settlement/invoice-items/generate").param("billId", billId).with(master())).andExpect(status().isOk());
        Map<String, Integer> rates = new HashMap<>();
        jdbc.query("SELECT COALESCE(i.merchant_id, v.income_type), v.vat_rate_bps FROM settlement_invoice_items v"
                + " LEFT JOIN settlement_items i ON i.id = v.settlement_item_id WHERE v.bill_id = ?", rs -> {
                    rates.put(rs.getString(1), rs.getInt(2));
                }, billId);
        return rates;
    }

    private long culturalFeeCent(String billId) {
        return jdbc.queryForObject("SELECT cultural_fee_cent FROM settlement_invoice_items WHERE bill_id = ? AND income_type = 'ticket'"
                + " AND status = 'pending'", Long.class, billId);
    }

    /** 233-R6：发放主体为 entityId 的一张已月结工资单，税前应发 cent（总额，税、小费、服务费为 0）。 */
    private void payroll(String entityId, String month, long cent) {
        jdbc.update("INSERT INTO staff_payroll_statements (id, user_id, month, total_cent, payer_company_id) VALUES (?, ?, ?, ?, ?)",
                "sps_etd_" + entityId, "usr_etd_pay_" + entityId, month, cent, entityId);
    }

    /** 233-R6「主体收入成本利润」页签的真实请求形状（蛇形键）。 */
    private ResultActions profitOf(String entityId, String start, String end) throws Exception {
        return mockMvc.perform(get("/api/v1/finance/entity-taxes/profits").param("business_entity_id", entityId).param("start_month", start)
                .param("end_month", end).with(finance));
    }

    /** 233-R6 成本分项按类型取金额。 */
    private static Map<String, Long> costs(String body) {
        List<Map<String, Object>> items = JsonPath.read(body, "$.data.cost_items");
        Map<String, Long> out = new HashMap<>();
        items.forEach(row -> out.merge((String) row.get("cost_type"), ((Number) row.get("amount_cent")).longValue(), Long::sum));
        return out;
    }

    private ResultActions listed() throws Exception {
        return listedFor(entity);
    }

    private ResultActions listedFor(String entityId) throws Exception {
        return mockMvc.perform(get("/api/v1/finance/entity-taxes").param("business_entity_id", entityId).with(finance)).andExpect(status().isOk());
    }

    private ResultActions declare(String key, String taxType, String start, String end, long cent) throws Exception {
        return declareFor(entity, key, taxType, start, end, cent);
    }

    private ResultActions declareFor(String entityId, String key, String taxType, String start, String end, long cent) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/entity-taxes").with(finance).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + key + "\",\"business_entity_id\":\"" + entityId + "\",\"tax_type\":\"" + taxType
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
