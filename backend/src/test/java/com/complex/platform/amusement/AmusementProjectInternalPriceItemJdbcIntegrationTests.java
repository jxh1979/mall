package com.complex.platform.amusement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
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
 * 第十八批 BR 段第 357 条 357-R1（BSM:753「游乐权益核销的收入按具体的游乐项目归属……游乐公司再按已批准的内部结算价或域内规则，与实际提供服务的游乐商户
 * 结算。」；ENUM:53「采用哪种规则由域主管公司决定，商户不能决定。」；BSM:1671「实际提供商品或服务的主体提交价格，对应域主管公司审批」「不得跨项目引用」；
 * ENUM:54「任何商户在没有获批内部结算价之前，不得为其开通需要该价格结算的权益或券。」）：游乐公司为游乐项目引用所属商户的内部结算价目录条目。
 *
 * <p>夹具随测试事务回滚：游乐商户 A 经营项目 P；A 有一条已生效条目与一条只有待审批草案的条目，商户 B 有一条已生效条目。游乐公司人员（无商户范围、持
 * amusement.session.manage）与 A 的账号按前端蛇形键走真实接口。修复前没有该接口：状态断言红。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class AmusementProjectInternalPriceItemJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String complex = "c357r1_" + s;
    private final String shopA = "ma357r1_" + s;
    private final String shopB = "mb357r1_" + s;
    private final String project = "ap357r1_" + s;
    private final String activeA = "sku_a_" + s;
    private final String draftA = "sku_ad_" + s;
    private final String activeB = "sku_b_" + s;
    private RequestPostProcessor company;
    private RequestPostProcessor merchantA;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                complex, "游乐结算价综合体" + s);
        for (String shop : new String[]{shopA, shopB}) {
            jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at)"
                    + " VALUES (?, ?, 'amusement', ?, 'active', now(), now())", shop, "游乐结算价商户" + shop, complex);
        }
        catalog(activeA, shopA, false);
        catalog(draftA, shopA, true);
        catalog(activeB, shopB, false);
        jdbc.update("INSERT INTO amusement_projects (id, project_name, capacity, open_status, eligibility_rule_json, created_at, updated_at, merchant_id)"
                + " VALUES (?, '商户经营的游乐项目', 20, 'open', '{}'::jsonb, now(), now(), ?)", project, shopA);
        // 项目列表按项目所在综合体判可见范围：放进综合体的游乐空间（夹具同营业日用例）。
        AmusementBusinessDayTestFixture.bind(jdbc, project, complex);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        company = staffs.staff("usr_357r1_co_" + s, "platform_admin", complex, null, "amusement.session.manage", "amusement.session.view");
        merchantA = staffs.staff("usr_357r1_ma_" + s, "staff", complex, shopA, "amusement.session.manage", "amusement.session.view");
    }

    /** ①④：游乐公司人员引用项目所属商户的已生效条目 200，列表里该项目带回编码；同一幂等键重放返回第一次的结果、只留痕一次。 */
    @Test
    void domainCompanyReferencesTheOwningMerchantsActiveItem() throws Exception {
        set(company, "k1", activeA).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(project))
                .andExpect(jsonPath("$.data.catalog_item_code").value(activeA));
        mockMvc.perform(get("/api/v1/amusement/projects").with(company)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == '" + project + "')].catalog_item_code", contains(activeA)));

        set(company, "k1", activeA).andExpect(status().isOk()).andExpect(jsonPath("$.data.catalog_item_code").value(activeA));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'amusement.project.internal_price_ref.updated'"
                + " AND business_id = ?", Integer.class, project)).isEqualTo(1);
    }

    /** ②：别的商户的已生效条目、本商户只有待审批草案的条目都不能引用（400），引用不变。 */
    @Test
    void otherMerchantsItemOrUnapprovedItemIsRejected() throws Exception {
        set(company, "k2", activeB).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("AMUSEMENT_PRICE_ITEM_UNUSABLE"));
        set(company, "k3", draftA).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("AMUSEMENT_PRICE_ITEM_UNUSABLE"));
        assertThat(reference()).isNull();
    }

    /** ③：项目所属游乐商户的账号不能设置（403），引用不变。 */
    @Test
    void owningMerchantCannotChooseTheSettlementRule() throws Exception {
        set(merchantA, "k4", activeA).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("AMUSEMENT_SETTLEMENT_RULE_DOMAIN_COMPANY_ONLY"));
        assertThat(reference()).isNull();
    }

    private ResultActions set(RequestPostProcessor who, String key, String code) throws Exception {
        return mockMvc.perform(put("/api/v1/amusement/projects/" + project + "/internal-price-item").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"357r1-" + key + "-" + s + "\",\"catalog_item_code\":\"" + code + "\"}"));
    }

    private String reference() {
        return jdbc.queryForObject("SELECT catalog_item_code FROM amusement_projects WHERE id = ?", String.class, project);
    }

    private void catalog(String code, String shop, boolean draft) {
        jdbc.update("INSERT INTO internal_price_catalog (id, item_code, merchant_id, category, item_name, price_cent, status, is_draft, updated_by,"
                + " created_at, updated_at) VALUES (?, ?, ?, 'service', '游乐项目结算价', 3000, 'active', ?, 'seed_357r1', now(), now())",
                code + "_v1", code, shop, draft);
    }
}
