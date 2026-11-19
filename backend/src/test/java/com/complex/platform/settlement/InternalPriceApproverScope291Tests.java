package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.settlement.SettlementConfigWriteRequests.InternalPriceItemRequest;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 第十八批 AW 段 291-R1（ENUM:54「实际提供商品或服务的主体提交，对应域主管公司审批，提交人与审批人分离：酒吧由娱乐平台公司审批，游乐由游乐公司审批，
 * 演出传媒由传媒公司审批，中央餐饮供应的酒水或小食由中央餐饮公司内部差人审批；餐饮与零售商户……统一由城市项目公司审批……业态为“其他”的商户，
 * 由其商户档案上指定的主管公司审批」；BSM:576「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批」）：内部结算价草案按后台真实请求转正，
 * 只有对应域主管公司的人能批。草案经写入服务直接落（不经提交闸），审批人所属公司取账号机构。不开测试事务，@AfterEach 按本例数据清理。
 *
 * <p>291-R2（ENUM:54「实际提供商品或服务的主体提交」；BSM:1034「酒吧商户只能提交价格资料」；PGM:660「查看、导出和修改分别授权」）：按后台真实请求
 * （蛇形键）提交，持 settlement.internal_price.submit 的商户账号只报本商户、经营主体人员只报本公司；最高权限本人提交即生效。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class InternalPriceApproverScope291Tests {

    private static final String ENT = "be_ent_platform";
    private static final String AMUSEMENT = "be_amusement";
    private static final String MEDIA = "be_media";
    private static final String CENTRAL = "be_central_catering";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private SettlementConfigWriteService writeService;

    private final long n = System.nanoTime();
    private final String project = "prj_c291_" + n;
    private final String city = "be_c291_city_" + n;
    private final List<String> merchants = new ArrayList<>();
    private final List<String> items = new ArrayList<>();
    private int seq;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', ?, 'active', now(), now())", project, "内部结算价审批综合体" + n);
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at)"
                + " VALUES (?, ?, 'project_company', 'active', now(), now())", city, "城市项目公司" + n);
        jdbc.update("INSERT INTO business_entity_projects (business_entity_id, project_id) VALUES (?, ?)", city, project);
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM voucher_definitions WHERE id = ?", "vd_c291_" + n);
        for (String item : items) {
            jdbc.update("DELETE FROM internal_price_catalog WHERE item_code = ?", item);
        }
        jdbc.update("DELETE FROM internal_price_catalog WHERE item_name LIKE ?", "提交条目%" + n);
        for (String m : merchants) {
            jdbc.update("DELETE FROM merchants WHERE id = ?", m);
        }
        jdbc.update("DELETE FROM business_entity_projects WHERE business_entity_id = ?", city);
        jdbc.update("DELETE FROM business_entities WHERE id = ?", city);
        jdbc.update("UPDATE projects SET status = 'suspended' WHERE id = ?", project);
    }

    @Test
    void barPriceIsApprovedByTheEntertainmentPlatformCompanyOnly() throws Exception {
        String draft = merchantDraft(merchant("bar", null));
        activate(draft, employee(AMUSEMENT)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("INTERNAL_PRICE_APPROVER_SCOPE"));
        activate(draft, employee(ENT)).andExpect(status().isOk());
    }

    @Test
    void cateringPriceIsApprovedByTheCityProjectCompany() throws Exception {
        String draft = merchantDraft(merchant("catering", null));
        activate(draft, employee(ENT)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("INTERNAL_PRICE_APPROVER_SCOPE"));
        activate(draft, employee(city)).andExpect(status().isOk());
    }

    @Test
    void mediaPriceIsApprovedByTheMediaCompany() throws Exception {
        String draft = merchantDraft(merchant("media", null));
        activate(draft, employee(ENT)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("INTERNAL_PRICE_APPROVER_SCOPE"));
        activate(draft, employee(MEDIA)).andExpect(status().isOk());
    }

    @Test
    void otherFormatPriceIsApprovedByTheSupervisingCompanyOnFile() throws Exception {
        String draft = merchantDraft(merchant("other", AMUSEMENT));
        activate(draft, employee(ENT)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("INTERNAL_PRICE_APPROVER_SCOPE"));
        activate(draft, employee(AMUSEMENT)).andExpect(status().isOk());
    }

    @Test
    void centralCateringSupplyIsApprovedInsideTheCompanyByAnotherPerson() throws Exception {
        String drafter = employee(CENTRAL);
        String draft = draft(new InternalPriceItemRequest("c291-" + n + "-" + (++seq), null, null, "dining", "中央餐饮小食" + n,
                800L, CENTRAL), drafter);
        activate(draft, drafter).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SELF_ACTIVATION_FORBIDDEN"));
        activate(draft, employee(city)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("INTERNAL_PRICE_APPROVER_SCOPE"));
        activate(draft, employee(CENTRAL)).andExpect(status().isOk());
    }

    @Test
    void masterAuthorityActivatesAnyDraft() throws Exception {
        String draft = merchantDraft(merchant("bar", null));
        mockMvc.perform(post("/api/v1/settlement/config/internal_price_item/" + draft + "/activate").with(master()))
                .andExpect(status().isOk());
    }

    @Test
    void merchantAccountSubmitsOnlyItsOwnPrice() throws Exception {
        String bar = merchant("bar", null);
        String neighbour = merchant("bar", null);
        String clerk = user(project, bar, InternalPriceScope.SUBMIT);
        submit(session(clerk), null, bar, null, 1500L).andExpect(status().isOk());
        submit(session(clerk), null, neighbour, null, 1500L).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("INTERNAL_PRICE_SUBMITTER_SCOPE"));
    }

    @Test
    void companyStaffSubmitsOnlyWhatTheCompanyProvides() throws Exception {
        String clerk = user(CENTRAL, null, InternalPriceScope.SUBMIT);
        submit(session(clerk), null, null, CENTRAL, 800L).andExpect(status().isOk());
        submit(session(clerk), null, null, AMUSEMENT, 800L).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("INTERNAL_PRICE_SUBMITTER_SCOPE"));
    }

    @Test
    void platformStaffWhoIsNotTheProviderCannotSubmitAMerchantPrice() throws Exception {
        String bar = merchant("bar", null);
        String platformStaff = user(ENT, null, "settlement.config.manage", InternalPriceScope.SUBMIT);
        submit(session(platformStaff), null, bar, null, 1500L).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("INTERNAL_PRICE_SUBMITTER_SCOPE"));
    }

    @Test
    void masterAuthoritySubmissionTakesEffectWithoutAnotherReviewer() throws Exception {
        String bar = merchant("bar", null);
        String body = submit(master(), null, bar, null, 2600L).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        String item = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).path("data").asText();
        assertThat(jdbc.queryForList("SELECT price_cent FROM internal_price_catalog WHERE item_code = ? AND is_draft = FALSE"
                + " AND status = 'active'", Long.class, item)).as("最高权限本人提交即生效").containsExactly(2600L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM internal_price_catalog WHERE item_code = ? AND is_draft = TRUE",
                Integer.class, item)).isZero();
    }

    @Test
    void masterAuthorityZeroPriceForAVoucherReferencedItemIsStillRejected() throws Exception {
        String bar = merchant("bar", null);
        String item = "sku_c291_vd_" + n;
        items.add(item);
        jdbc.update("INSERT INTO internal_price_catalog (id, item_code, merchant_id, category, item_name, price_cent, status, is_draft,"
                + " updated_by, created_at, updated_at) VALUES (?, ?, ?, 'dining', ?, 3000, 'active', FALSE, 'seed_c291', now(), now())",
                item, item, bar, "券引用条目" + n);
        jdbc.update("INSERT INTO voucher_definitions (id, name, voucher_kind, redeem_method, settlement_mode, merchant_id, inventory_item_id,"
                + " stock_location_id, stock_quantity, internal_price_cent, catalog_item_code, status, created_at, updated_at)"
                + " VALUES (?, '券引用', 'in_store_goods', 'in_store_staff', 'internal_price', ?, 'inv_c291', 'loc_c291', 1, 3000, ?,"
                + " 'active', now(), now())", "vd_c291_" + n, bar, item);
        submit(master(), item, bar, null, 0L).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("CATALOG_PRICE_ZERO_VOUCHER_REF"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM internal_price_catalog WHERE item_code = ? AND is_draft = TRUE",
                Integer.class, item)).as("提交即生效失败，不留草案").isZero();
    }

    // ─────────────────────────── 夹具 ───────────────────────────

    /** 后台内部结算价目录页提交（蛇形键）。 */
    private ResultActions submit(RequestPostProcessor who, String itemCode, String merchant, String entity, long price) throws Exception {
        int k = ++seq;
        return mockMvc.perform(post("/api/v1/settlement/config/internal-price-item").with(who)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"idempotency_key": "c291-sub-%s-%s", "item_code": %s, "merchant_id": %s, "provider_entity_id": %s,
                         "category": "dining", "item_name": "提交条目%s_%s", "price_cent": %d}
                        """.formatted(k, n, quoted(itemCode), quoted(merchant), quoted(entity), k, n, price)));
    }

    private static String quoted(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private String merchant(String format, String supervising) {
        String id = "m_c291_" + (++seq) + "_" + n;
        merchants.add(id);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, supervising_company_id,"
                + " supervising_company_reason, created_at, updated_at) VALUES (?, ?, ?, ?, 'active', ?, ?, now(), now())",
                id, "内部结算价商户" + seq + "_" + n, format, project, supervising, supervising == null ? null : "档案指定主管公司");
        return id;
    }

    private String merchantDraft(String merchant) {
        return draft(new InternalPriceItemRequest("c291-" + n + "-" + (++seq), null, merchant, "dining", "内部结算价条目" + seq + "_" + n,
                1200L), "drafter_c291_" + n);
    }

    private String draft(InternalPriceItemRequest request, String drafter) {
        String item = writeService.saveInternalPriceItem(request, drafter);
        items.add(item);
        return jdbc.queryForObject("SELECT id FROM internal_price_catalog WHERE item_code = ? AND is_draft = TRUE AND status = 'active'",
                String.class, item);
    }

    private ResultActions activate(String draft, String who) throws Exception {
        return mockMvc.perform(post("/api/v1/settlement/config/internal_price_item/" + draft + "/activate").with(session(who)));
    }

    /** 某公司持结算配置管理权限的员工（账号机构＝所属公司），返回账号 id。 */
    private String employee(String company) {
        return user(company, null, "settlement.config.manage");
    }

    private String user(String organization, String merchant, String... permissions) {
        String userId = "usr_c291_" + (++seq) + "_" + n;
        String roleId = "role_" + userId;
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'staff', ?, ?, 'active', now())", userId, userId, organization, merchant);
        jdbc.update("INSERT INTO roles (id, role_name, created_at) VALUES (?, ?, now())", roleId, "内部结算价岗位" + seq + n);
        for (String code : permissions) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", roleId, code);
        }
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        return userId;
    }

    private RequestPostProcessor session(String userId) {
        java.util.Map<String, Object> row = jdbc.queryForMap("SELECT organization_id, merchant_id FROM access_users WHERE id = ?", userId);
        return actor(new AccessActor(userId, userId, "staff", (String) row.get("organization_id"), (String) row.get("merchant_id"),
                "active"));
    }

    private RequestPostProcessor master() {
        return actor(new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE,
                "platform-company", null, "active"));
    }

    private static RequestPostProcessor actor(AccessActor actor) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
