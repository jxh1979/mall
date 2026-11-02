// 文件版本：V0.02
package com.complex.platform.gate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.contract.ContractRecord;
import com.complex.platform.contract.ContractRepository;
import com.complex.platform.contract.ContractSettlementRecord;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第 100 条（PGM:744「前端隐藏只是辅助，后端接口必须独立校验」；PGM:737「关闭后菜单不可见或不可点，接口也要拒绝业务动作」；
 * PGM:719「开关关闭时，端或模块就不能用；开关打开后，也还要继续看员工权限……」）。
 * 100-R1：Web 前端与商户网页门户的模块由服务端按接口定，不带或改报 X-Module-Code 都绕不过模块开关；别的模块关闭、共用接口照常可用。
 * 100-R2：无商户归属的员工账号改报他端一律按 Web 前端判总开关。
 * 第十八批 M 段第 109 条：挂了商户的外包账号照前端现状自报商户网页门户，也按 Web 前端判总开关与外包工作台模块。
 * 走真实生产登录链与授权链（prod 档网页登录 {login_name, password} → 浏览器 Cookie 会话＋X-XSRF-TOKEN → 设备凭证拦截器 → 终端总开关 → 控制器权限）。
 */
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "complex.database.credential-separation.enforced=false",
        "refund.tiered_authorization.enabled=true",
        "complex.auth.production.seed-admin.username=p0_server_module_admin",
        "complex.auth.production.seed-admin.password=p0_server_module_password",
        "ticket.credential.signing.secret=prod-test-ticket-signing-secret-32-bytes-minimum",
        "bar.invitation.signing.secret=prod-test-bar-invitation-signing-secret-32-bytes-minimum",
        "compliance.wecom.webhook.signing.secret=prod-test-wecom-webhook-signing-secret-32-bytes-minimum",
        "keyvault.keys.pii=prod-test-pii-keyvault-secret-32-bytes-minimum",
        "keyvault.keys.pii-index=prod-test-pii-index-blind-secret-32-bytes-minimum"
})
@Transactional
class ServerModuleSwitchJdbcIntegrationTests {

    private static final String VOID_REASONS = "/api/v1/bar/void-reason-codes";

    @DynamicPropertySource
    static void masterPublicKey(DynamicPropertyRegistry registry) throws Exception {
        String key = Base64.getEncoder().encodeToString(KeyPairGenerator.getInstance("Ed25519").generateKeyPair().getPublic().getEncoded());
        registry.add("complex.auth.master.public-key", () -> key);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ContractRepository contracts;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String password = "b32-server-module-" + suffix;
    private final String staff = "usr_b32sm_" + suffix;
    private StaffRequestFixture accounts;

    @BeforeEach
    void seed() {
        accounts = new StaffRequestFixture(jdbc);
    }

    /** 100-R1 (a)：酒吧模块关闭——酒吧接口不带模块头、改报别的模块都 403 并按 bar 留痕；写请求同样被拒、不落数据，加 X-Forwarded-Prefix 头也一样。 */
    @Test
    void barModuleOffRejectsBarApisWhateverModuleHeaderSays() throws Exception {
        account(staff, "staff", null, "bar.order_item.void", "bar.order_item.void_decide");
        Cookie[] session = webSession(staff);
        close("tcs_web_mod_bar");
        int before = moduleRejections("bar");

        mockMvc.perform(web(get(VOID_REASONS), session, "web"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
        assertThat(moduleRejections("bar")).as("留痕 module_code=bar、scope_type=module").isEqualTo(before + 1);
        mockMvc.perform(web(get(VOID_REASONS), session, "web").header("X-Module-Code", "overview"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));

        int codes = voidReasonCodes();
        mockMvc.perform(saveVoidReason(session, "web"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
        mockMvc.perform(saveVoidReason(session, "web").header("X-Forwarded-Prefix", "/x"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
        assertThat(voidReasonCodes()).isEqualTo(codes);
    }

    /** 100-R1 (b)：别的模块（库存）关闭不影响酒吧接口。 */
    @Test
    void otherModuleOffDoesNotAffectBarApis() throws Exception {
        account(staff, "staff", null, "bar.order_item.void", "bar.order_item.void_decide");
        Cookie[] session = webSession(staff);
        close("tcs_web_mod_inventory");

        mockMvc.perform(web(get(VOID_REASONS), session, "web")).andExpect(status().isOk());
    }

    /** 100-R1 (c)：库存模块关闭——酒吧页也用的库存余额照常可用；库房列表（只有库存页用）403 并按 inventory 留痕。
     *  平台账号（库存余额要商户范围或平台账号，无商户的普通员工本就 403 MERCHANT_SCOPE_REQUIRED）。 */
    @Test
    void inventoryModuleOffKeepsSharedStockBalancesButRejectsWarehouses() throws Exception {
        account(staff, "platform_admin", null, "inventory.stock.view", "inventory.warehouse.manage");
        Cookie[] session = webSession(staff);
        close("tcs_web_mod_inventory");
        int before = moduleRejections("inventory");

        mockMvc.perform(web(get("/api/v1/inventory/stock-balances"), session, "web")).andExpect(status().isOk());
        mockMvc.perform(web(get("/api/v1/inventory/warehouses"), session, "web"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
        assertThat(moduleRejections("inventory")).isEqualTo(before + 1);
    }

    /** 100-R1 (d)：商户专区模块关闭——商户账号不带模块头、改报别的模块都 403；外壳的未读账单提醒同样不给；异议被拒、不落异议，加 X-Forwarded-Prefix 头也一样。 */
    @Test
    void merchantZoneModuleOffRejectsMerchantPortalApisWithoutModuleHeader() throws Exception {
        String merchant = "mch_b32sm_" + suffix;
        String shop = "usr_b32ms_" + suffix;
        String settlement = "stl_b32sm_" + suffix;
        account(shop, "staff", merchant, "merchant.portal.settlement.view", "merchant.portal.settlement.dispute");
        Instant now = Instant.now();
        contracts.saveContract(contract("ctr_b32sm_" + suffix, merchant, now));
        contracts.saveSettlement(settlement(settlement, "ctr_b32sm_" + suffix, now));
        Cookie[] session = webSession(shop);
        close("tcs_mp_mod_merchant");

        mockMvc.perform(web(get("/api/v1/merchant-portal/settlements"), session, "merchant_portal"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
        mockMvc.perform(web(get("/api/v1/merchant-portal/settlements"), session, "merchant_portal").header("X-Module-Code", "overview"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
        mockMvc.perform(web(get("/api/v1/merchant-portal/unread-bills"), session, "merchant_portal"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));

        int disputed = disputes();
        mockMvc.perform(web(post("/api/v1/merchant-portal/settlements/" + settlement + "/dispute"), session, "merchant_portal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("idempotency_key", "b32sm-dispute-" + suffix, "reason", "租金与合同约定不符"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
        mockMvc.perform(web(post("/api/v1/merchant-portal/settlements/" + settlement + "/dispute"), session, "merchant_portal")
                        .header("X-Forwarded-Prefix", "/x")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("idempotency_key", "b32sm-dispute-" + suffix, "reason", "租金与合同约定不符"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
        assertThat(disputes()).isEqualTo(disputed);
    }

    /** 100-R2 (e)：Web 前端总开关关闭——无商户归属员工改报其他五端都 403 并按 web 留痕；改报经销商门户的写请求被拒、不落数据。 */
    @Test
    void nonMerchantStaffSelfReportCannotBypassWebSwitch() throws Exception {
        account(staff, "staff", null, "bar.order_item.void", "bar.order_item.void_decide");
        Cookie[] session = webSession(staff);
        close("tcs_term_web");
        int before = webRejections();

        for (String claimed : List.of("merchant_portal", "merchant_app", "dealer_portal", "mobile_admin", "app")) {
            mockMvc.perform(web(get(VOID_REASONS), session, claimed))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
        }
        assertThat(webRejections()).isEqualTo(before + 5);

        int codes = voidReasonCodes();
        mockMvc.perform(saveVoidReason(session, "dealer_portal"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
        assertThat(voidReasonCodes()).isEqualTo(codes);
    }

    /** 100-R2 (f)：Web 前端总开关开着时，同样的改报照常可用。 */
    @Test
    void nonMerchantStaffSelfReportWorksWhenWebSwitchOn() throws Exception {
        account(staff, "staff", null, "bar.order_item.void", "bar.order_item.void_decide");
        Cookie[] session = webSession(staff);

        for (String claimed : List.of("merchant_portal", "merchant_app", "dealer_portal", "mobile_admin", "app")) {
            mockMvc.perform(web(get(VOID_REASONS), session, claimed)).andExpect(status().isOk());
        }
    }

    private static final String SUBORDINATES = "/api/v1/partner/subordinates";

    /** 第十八批 M 段第 109 条 (g)（PGM:745「……Web 前端（业务人员，客人无网页端）……商户网页门户……」；PGM:719「开关关闭时，端或模块就不能用」）：
     *  挂了商户的外包账号，Web 外包工作台模块关闭——前端照现状自报商户网页门户，下属列表与开通下属都 403 并按 web、vendor 留痕，不落账号；
     *  effective 返回外包工作台的关闭行（前端据此隐藏菜单组）。 */
    @Test
    void merchantBoundVendorIsGatedByWebVendorModule() throws Exception {
        Cookie[] session = webSession(merchantBoundVendor());
        close("tcs_web_mod_vendor");
        int before = vendorModuleRejections();

        mockMvc.perform(web(get(SUBORDINATES), session, "merchant_portal"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
        assertThat(vendorModuleRejections()).as("留痕 module_code=vendor、terminal_type=web").isEqualTo(before + 1);

        String username = "b33vnd_sub_" + suffix;
        mockMvc.perform(web(post(SUBORDINATES), session, "merchant_portal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("idempotency_key", "b33vnd-sub-" + suffix, "username", username))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM access_users WHERE username = ?", Integer.class, username)).isZero();

        mockMvc.perform(web(get("/api/v1/terminals/capability-switches/effective"), session, "merchant_portal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == 'tcs_web_mod_vendor')]").exists());
    }

    /** 第 109 条 (h)：外包工作台模块开着、商户网页门户总开关关闭——同一账号照常可用；再关 Web 前端总开关即 403 并按 web 留痕。 */
    @Test
    void merchantBoundVendorFollowsWebSwitchNotMerchantPortal() throws Exception {
        Cookie[] session = webSession(merchantBoundVendor());
        close("tcs_term_merchant_portal");

        mockMvc.perform(web(get(SUBORDINATES), session, "merchant_portal")).andExpect(status().isOk());

        close("tcs_term_web");
        int before = webRejections();
        mockMvc.perform(web(get(SUBORDINATES), session, "merchant_portal"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("TERMINAL_DISABLED"));
        assertThat(webRejections()).isEqualTo(before + 1);
    }

    /** 外部协作机构账号（后台「员工账号」页可选所属商户），绑到一个启用的外部机构上。 */
    private String merchantBoundVendor() {
        String vendor = "usr_b33vnd_" + suffix;
        String org = "epo_b33vnd_" + suffix;
        account(vendor, "outsourced_vendor", "mch_b33vnd_" + suffix, "partner.subordinate.view", "partner.subordinate.manage");
        jdbc.update("""
                INSERT INTO external_partner_orgs (id, project_id, org_name, org_category, max_subordinates, status, created_at, updated_at)
                VALUES (?, 'proj_1', '第109条外包机构', 'facility_maintenance', 5, 'active', now(), now())
                """, org);
        jdbc.update("UPDATE access_users SET partner_org_id = ? WHERE id = ?", org, vendor);
        return vendor;
    }

    private int vendorModuleRejections() {
        return jdbc.queryForObject("""
                SELECT count(*) FROM audit_events WHERE event_name = 'terminal.access_rejected'
                   AND metadata->>'module_code' = 'vendor' AND metadata->>'terminal_type' = 'web'
                """, Integer.class);
    }

    private void close(String switchId) {
        assertThat(jdbc.update("UPDATE terminal_capability_switches SET enabled = FALSE WHERE id = ?", switchId)).isEqualTo(1);
    }

    /** 后台网页登录（AdminApp 登录体只有 login_name、password），取浏览器会话 Cookie 与 CSRF Cookie。 */
    private Cookie[] webSession(String loginName) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Terminal-Type", "web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("login_name", loginName, "password", password))))
                .andExpect(status().isOk()).andReturn();
        return new Cookie[] {login.getResponse().getCookie("admin_access_token"), login.getResponse().getCookie("XSRF-TOKEN")};
    }

    private MockHttpServletRequestBuilder web(MockHttpServletRequestBuilder request, Cookie[] session, String claimed) {
        return request.cookie(session).header("X-XSRF-TOKEN", session[1].getValue()).header("X-Terminal-Type", claimed);
    }

    private MockHttpServletRequestBuilder saveVoidReason(Cookie[] session, String claimed) throws Exception {
        return web(post(VOID_REASONS), session, claimed)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("idempotency_key", "b32sm-void-" + suffix, "code", "b32sm_" + suffix,
                        "label_zh", "测试原因", "sort_order", 99)));
    }

    private void account(String userId, String accountType, String merchantId, String... permissions) {
        accounts.staff(userId, accountType, null, merchantId, permissions);
        jdbc.update("""
                INSERT INTO auth_credentials (user_id, password_hash, password_algo, failed_attempt_count, locked_until,
                                              password_changed_at, status, created_at, updated_at)
                VALUES (?, ?, 'bcrypt', 0, NULL, now(), 'active', now(), now())
                """, userId, new BCryptPasswordEncoder(10).encode(password));
    }

    private static ContractRecord contract(String id, String merchantId, Instant at) {
        return new ContractRecord(id, merchantId, "subj_1", null, "subj_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of(),
                "fixed_ratio", "by_contract", 0L, Map.of(), List.of(), null, null, List.of(),
                "effective", "proj_1", null, null, null, at, at);
    }

    private static ContractSettlementRecord settlement(String id, String contractId, Instant at) {
        return new ContractSettlementRecord(id, contractId, "move_out", "pending_settlement", List.of(),
                null, null, null, "proj_1", at, at);
    }

    private int moduleRejections(String module) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM audit_events WHERE event_name = 'terminal.access_rejected'
                   AND metadata->>'module_code' = ? AND metadata->>'scope_type' = 'module'
                """, Integer.class, module);
    }

    private int webRejections() {
        return jdbc.queryForObject("""
                SELECT count(*) FROM audit_events WHERE event_name = 'terminal.access_rejected'
                   AND metadata->>'terminal_type' = 'web' AND metadata->>'capability' = 'terminal.enabled'
                """, Integer.class);
    }

    private int voidReasonCodes() {
        return jdbc.queryForObject("SELECT count(*) FROM bar_void_reason_codes", Integer.class);
    }

    private int disputes() {
        return jdbc.queryForObject(
                "SELECT count(*) FROM audit_events WHERE event_name = 'merchant_portal.settlement.disputed'", Integer.class);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
