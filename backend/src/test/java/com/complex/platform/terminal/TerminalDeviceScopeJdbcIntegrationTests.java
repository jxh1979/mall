package com.complex.platform.terminal;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.TemporaryGrant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * M723-F21（治理地图第 653 行「所有业务数据默认按项目隔离……数据的项目与商户隔离不提供关闭开关」；操作员 2026-10-06 裁决，
 * 见 decisions/20260924T114205Z-M723-terminal-device-management-scope.md）：真实库、真实接口回归。终端设备管理的入口按设备归属
 * 范围放行——平台管理员全部设备都能管；带商户归属的员工只能管本商户的设备；不带商户归属的项目员工能管本项目内的全部设备
 * （含本项目内各商户的设备和不挂商户的设备）。设备属于哪个项目：先看所属商户的项目，没有商户的看绑定空间的项目，再没有的看
 * 持有员工的项目；认不出项目的设备只有平台管理员能管。设备列表只显示本人能管的设备；登记新设备、改设备归属时目标归属也要在
 * 本人能管的范围内。
 * 本类覆盖九个入口：绑定、设备列表、改归属、归还、解绑、送修、挂失、找回、报废；第十个入口（登记签名公钥）的回归在
 * SingleUseGrantRequestTransactionTests（连同单次授权不算用掉一起验）。
 * 修复前服务层不核设备归属：各「越范围被拒」用例第一处 403 的断言即红（接口返回 200 并已写入）；标「守护」的用例修复前后都通过。
 *
 * <p>不开测试事务（走真实接口、真实提交）。夹具按随机后缀隔离、结束时删除；设备状态流水与审计事件只追加不删（V140 触发器），
 * 按后缀隔离留在库里。权限给长期来源（非单次的临时授权），不涉及单次消耗。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class TerminalDeviceScopeJdbcIntegrationTests {

    private static final String DEVICES = "/api/v1/terminals/devices";
    // 十个入口用到的四个权限码：绑定／归还／登记公钥、解绑、生命周期与改归属、设备列表
    private static final List<String> CODES = List.of(
            "terminal.device.bind", "terminal.device.unbind", "terminal.device.lifecycle", "terminal.switch.view");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AccessRepository accessRepository;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    // 两个项目：三名操作人都属于项目甲
    private final String projectA = "prj_m723s_a_" + suffix;
    private final String projectB = "prj_m723s_b_" + suffix;
    // 商户甲、商户乙在项目甲，商户丙在项目乙
    private final String merchantA = "mch_m723s_a_" + suffix;
    private final String merchantB = "mch_m723s_b_" + suffix;
    private final String merchantC = "mch_m723s_c_" + suffix;
    private final String spaceA = "spc_m723s_a_" + suffix;
    private final String spaceB = "spc_m723s_b_" + suffix;
    // 持有员工（员工档案）：一名在项目甲、一名在项目乙
    private final String holderA = "stf_m723s_a_" + suffix;
    private final String holderB = "stf_m723s_b_" + suffix;
    // 商户甲的员工、项目甲的项目员工（不带商户归属）、平台管理员
    private final String merchantStaff = "usr_m723s_m_" + suffix;
    private final String projectStaff = "usr_m723s_p_" + suffix;
    private final String admin = "usr_m723s_x_" + suffix;

    @BeforeEach
    void seed() {
        for (String project : List.of(projectA, projectB)) {
            jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                    + "VALUES (?, 'complex', ?, 'active', now(), now())", project, "M723-F21 " + project);
        }
        merchant(merchantA, projectA);
        merchant(merchantB, projectA);
        merchant(merchantC, projectB);
        space(spaceA, projectA);
        space(spaceB, projectB);
        holder(holderA, projectA);
        holder(holderB, projectB);
        user(merchantStaff, "staff", merchantA);
        user(projectStaff, "staff", null);
        user(admin, "platform_admin", null);
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("DELETE FROM terminal_devices WHERE device_id LIKE ?", "dev-m723s-%" + suffix);
        jdbcTemplate.update("DELETE FROM staff_profiles WHERE id IN (?, ?)", holderA, holderB);
        jdbcTemplate.update("DELETE FROM spaces WHERE id IN (?, ?)", spaceA, spaceB);
        jdbcTemplate.update("DELETE FROM merchants WHERE id IN (?, ?, ?)", merchantA, merchantB, merchantC);
        jdbcTemplate.update("DELETE FROM projects WHERE id IN (?, ?)", projectA, projectB);
        jdbcTemplate.update("DELETE FROM temporary_permission_grants WHERE subject_id IN (?, ?, ?)",
                merchantStaff, projectStaff, admin);
        jdbcTemplate.update("DELETE FROM access_users WHERE id IN (?, ?, ?)", merchantStaff, projectStaff, admin);
        jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key LIKE ?", "m723s-%" + suffix);
    }

    // ── 带商户归属的员工对别的商户的设备（同一项目的商户乙）：六个按设备定位的写入口 ──────────────

    // 归还（POST /devices/return，按设备码定位）。修复前：200，设备变为 returned（403 的断言即红）。
    @Test
    void aMerchantEmployeeCannotReturnAnotherMerchantsDevice() throws Exception {
        String id = device("ret", "bound", merchantB, null, null);
        expectRejectedWithoutWrite(merchantStaff, "POST", DEVICES + "/return", key("ret"),
                returnJson(key("ret"), "ret"), "CROSS_MERCHANT_FORBIDDEN", id);
    }

    // 解绑（POST /devices/{id}/unbind）。修复前：200，设备变为 unbound（MDM 开关开着时是 403 DEVICE_NOT_MANAGED，拒绝码的断言同样红）。
    @Test
    void aMerchantEmployeeCannotUnbindAnotherMerchantsDevice() throws Exception {
        String id = device("unb", "bound", merchantB, null, null);
        expectRejectedWithoutWrite(merchantStaff, "POST", DEVICES + "/" + id + "/unbind", key("unb"),
                reasonJson(key("unb")), "CROSS_MERCHANT_FORBIDDEN", id);
    }

    // 送修（POST /devices/{id}/repair）。修复前：200，设备变为 repair。
    @Test
    void aMerchantEmployeeCannotSendAnotherMerchantsDeviceForRepair() throws Exception {
        String id = device("rep", "bound", merchantB, null, null);
        expectRejectedWithoutWrite(merchantStaff, "POST", DEVICES + "/" + id + "/repair", key("rep"),
                reasonJson(key("rep")), "CROSS_MERCHANT_FORBIDDEN", id);
    }

    // 挂失（POST /devices/{id}/lost）。修复前：200，设备变为 lost。
    @Test
    void aMerchantEmployeeCannotReportAnotherMerchantsDeviceLost() throws Exception {
        String id = device("lost", "bound", merchantB, null, null);
        expectRejectedWithoutWrite(merchantStaff, "POST", DEVICES + "/" + id + "/lost", key("lost"),
                reasonJson(key("lost")), "CROSS_MERCHANT_FORBIDDEN", id);
    }

    // 找回（POST /devices/{id}/found；设备先处于挂失）。修复前：200，设备变为 found。
    @Test
    void aMerchantEmployeeCannotMarkAnotherMerchantsLostDeviceFound() throws Exception {
        String id = device("found", "lost", merchantB, null, null);
        expectRejectedWithoutWrite(merchantStaff, "POST", DEVICES + "/" + id + "/found", key("found"),
                reasonJson(key("found")), "CROSS_MERCHANT_FORBIDDEN", id);
    }

    // 报废（POST /devices/{id}/scrap）。修复前：200，设备变为 scrapped（终态，不可恢复）。
    @Test
    void aMerchantEmployeeCannotScrapAnotherMerchantsDevice() throws Exception {
        String id = device("scrap", "bound", merchantB, null, null);
        expectRejectedWithoutWrite(merchantStaff, "POST", DEVICES + "/" + id + "/scrap", key("scrap"),
                reasonJson(key("scrap")), "CROSS_MERCHANT_FORBIDDEN", id);
    }

    // ── 绑定（登记设备）：同号旧设备与绑定后的归属都要在范围内 ─────────────────────────────

    // 商户甲员工登记一台新设备、所属商户填商户乙 → 403，库里没有这台设备；拿商户乙名下已归还的设备号重新登记、所属商户改填
    // 本商户 → 403，设备行原样（不能借登记把别家的设备划到自己名下）。修复前：两处都 200（第一处 403 的断言即红）。
    @Test
    void aMerchantEmployeeCannotRegisterADeviceForAnotherMerchantOrTakeOverTheirs() throws Exception {
        expectBindRejected(merchantStaff, key("bind-new"), bindJson(key("bind-new"), "bnew", merchantB, null),
                "CROSS_MERCHANT_FORBIDDEN", "bnew");

        String theirs = device("btake", "returned", merchantB, null, null);
        expectRejectedWithoutWrite(merchantStaff, "POST", DEVICES + "/bind", key("bind-take"),
                bindJson(key("bind-take"), "btake", merchantA, null), "CROSS_MERCHANT_FORBIDDEN", theirs);
    }

    // 项目甲的项目员工登记新设备：绑到项目乙的空间、或所属商户填项目乙的商户丙 → 403 CROSS_PROJECT_FORBIDDEN，库里没有这台设备；
    // 绑到本项目的空间 → 200。修复前：三处都 200（第一处 403 的断言即红）。
    @Test
    void aProjectEmployeeCanOnlyRegisterDevicesIntoTheirOwnProject() throws Exception {
        expectBindRejected(projectStaff, key("bind-space"), bindJson(key("bind-space"), "pspace", null, spaceB),
                "CROSS_PROJECT_FORBIDDEN", "pspace");
        expectBindRejected(projectStaff, key("bind-merchant"), bindJson(key("bind-merchant"), "pmerch", merchantC, null),
                "CROSS_PROJECT_FORBIDDEN", "pmerch");

        send(projectStaff, "POST", DEVICES + "/bind", bindJson(key("bind-own"), "pown", null, spaceA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("bound"))
                .andExpect(jsonPath("$.data.bound_space_id").value(spaceA));
        assertThat(devicesWithDeviceId("pown")).isEqualTo(1);
    }

    // ── 改归属（PUT /devices/{id}/merchant） ─────────────────────────────────────────

    // 带商户归属的员工不能把设备改到别的商户：商户甲员工把本商户的设备改到商户乙 → 403，设备仍属商户甲；把商户乙的设备改到
    // 本商户 → 403，设备仍属商户乙。修复前：两处都 200（第一处 403 的断言即红，设备已改到商户乙）。
    @Test
    void aMerchantEmployeeCannotMoveADeviceToOrFromAnotherMerchant() throws Exception {
        String own = device("mown", "bound", merchantA, null, null);
        expectRejectedWithoutWrite(merchantStaff, "PUT", DEVICES + "/" + own + "/merchant", key("move-out"),
                merchantJson(key("move-out"), merchantB), "CROSS_MERCHANT_FORBIDDEN", own);

        String theirs = device("mtheirs", "bound", merchantB, null, null);
        expectRejectedWithoutWrite(merchantStaff, "PUT", DEVICES + "/" + theirs + "/merchant", key("move-in"),
                merchantJson(key("move-in"), merchantA), "CROSS_MERCHANT_FORBIDDEN", theirs);
    }

    // 项目员工改完后仍须认定为本项目：项目甲的项目员工把本项目公区设备（不挂商户、绑在本项目空间）改到项目乙的商户丙 → 403
    // CROSS_PROJECT_FORBIDDEN，设备行原样；改到本项目的商户乙 → 200。修复前：第一处 403 即红（200，设备已改到另一项目的商户）。
    @Test
    void aProjectEmployeeCanOnlyMoveADeviceToAMerchantOfTheirOwnProject() throws Exception {
        String id = device("pmove", "bound", null, spaceA, null);
        expectRejectedWithoutWrite(projectStaff, "PUT", DEVICES + "/" + id + "/merchant", key("pmove-out"),
                merchantJson(key("pmove-out"), merchantC), "CROSS_PROJECT_FORBIDDEN", id);

        send(projectStaff, "PUT", DEVICES + "/" + id + "/merchant", merchantJson(key("pmove-in"), merchantB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merchant_id").value(merchantB));
    }

    // ── 设备列表（GET /devices）：只显示本人能管的设备 ──────────────────────────────────

    // 八台设备：商户甲的、商户乙的、本项目公区的（绑本项目空间）、本项目员工持有的；另一项目空间上的、另一项目商户丙的、
    // 另一项目员工持有的、三样都没填的。商户甲员工只看到商户甲的那台；项目甲的项目员工看到前四台；平台管理员八台都看到。
    // 修复前：三种人都看到全部（商户甲员工「不含」的断言即红）。
    @Test
    void theDeviceListShowsOnlyTheDevicesTheViewerCanManage() throws Exception {
        String ownMerchant = device("l1", "bound", merchantA, null, null);
        String otherMerchant = device("l2", "bound", merchantB, null, null);
        String publicArea = device("l3", "bound", null, spaceA, null);
        String heldInProject = device("l4", "bound", null, null, holderA);
        String foreignSpace = device("l5", "bound", null, spaceB, null);
        String foreignMerchant = device("l6", "bound", merchantC, null, null);
        String foreignHolder = device("l7", "bound", null, null, holderB);
        String unknown = device("l8", "bound", null, null, null);

        assertThat(listedDeviceIds(merchantStaff)).contains(ownMerchant)
                .doesNotContain(otherMerchant, publicArea, heldInProject, foreignSpace, foreignMerchant, foreignHolder, unknown);
        assertThat(listedDeviceIds(projectStaff)).contains(ownMerchant, otherMerchant, publicArea, heldInProject)
                .doesNotContain(foreignSpace, foreignMerchant, foreignHolder, unknown);
        assertThat(listedDeviceIds(admin)).contains(ownMerchant, otherMerchant, publicArea, heldInProject,
                foreignSpace, foreignMerchant, foreignHolder, unknown);
    }

    // ── 不带商户归属的项目员工 ───────────────────────────────────────────────────

    // 别的项目的设备一律不能碰：绑在项目乙空间上的、属于项目乙商户丙的、由项目乙员工持有的——送修、归还、报废各 403
    // CROSS_PROJECT_FORBIDDEN，设备行原样。修复前：三处都 200（第一处 403 的断言即红）。
    @Test
    void aProjectEmployeeCannotManageAnotherProjectsDevices() throws Exception {
        String bySpace = device("fs", "bound", null, spaceB, null);
        expectRejectedWithoutWrite(projectStaff, "POST", DEVICES + "/" + bySpace + "/repair", key("fs"),
                reasonJson(key("fs")), "CROSS_PROJECT_FORBIDDEN", bySpace);

        String byMerchant = device("fm", "bound", merchantC, null, null);
        expectRejectedWithoutWrite(projectStaff, "POST", DEVICES + "/return", key("fm"),
                returnJson(key("fm"), "fm"), "CROSS_PROJECT_FORBIDDEN", byMerchant);

        String byHolder = device("fh", "bound", null, null, holderB);
        expectRejectedWithoutWrite(projectStaff, "POST", DEVICES + "/" + byHolder + "/scrap", key("fh"),
                reasonJson(key("fh")), "CROSS_PROJECT_FORBIDDEN", byHolder);
    }

    // 守护（修复前后都通过）：项目员工能管本项目内的全部设备——本项目商户乙的设备送修 200、本项目公区设备归还 200、
    // 本项目员工持有的设备挂失 200（三条认定路径：商户的项目、绑定空间的项目、持有员工的项目）。
    // 归还、送修、挂失、找回、报废可以顺带改写持有员工。项目员工把本项目员工持有的设备（不挂商户、不绑空间，按持有员工认定项目）
    // 送修时改成另一项目的员工持有 → 403 CROSS_PROJECT_FORBIDDEN、没有写入；不改持有员工照常 200。
    // 修复前第一处 200（设备的持有员工被改成另一项目的人，按认定规则设备就到了另一项目）。
    @Test
    void aProjectEmployeeCannotHandADeviceToAnotherProjectsEmployee() throws Exception {
        String byHolder = device("hh", "bound", null, null, holderA);
        String handOver = """
                {"idempotency_key": "%s", "reason": "M723-F21 回归", "staff_id": "%s"}
                """.formatted(key("hh"), holderB);
        expectRejectedWithoutWrite(projectStaff, "POST", DEVICES + "/" + byHolder + "/repair", key("hh"),
                handOver, "CROSS_PROJECT_FORBIDDEN", byHolder);
        send(projectStaff, "POST", DEVICES + "/" + byHolder + "/repair", reasonJson(key("hh2")))
                .andExpect(status().isOk());
    }

    @Test
    void aProjectEmployeeManagesEveryDeviceOfTheirOwnProject() throws Exception {
        String ofMerchant = device("om", "bound", merchantB, null, null);
        send(projectStaff, "POST", DEVICES + "/" + ofMerchant + "/repair", reasonJson(key("om")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("repair"));

        device("os", "bound", null, spaceA, null);
        send(projectStaff, "POST", DEVICES + "/return", returnJson(key("os"), "os"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("returned"));

        String ofHolder = device("oh", "bound", null, null, holderA);
        send(projectStaff, "POST", DEVICES + "/" + ofHolder + "/lost", reasonJson(key("oh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("lost"));
    }

    // ── 带商户归属的员工：范围内成功的代表用例 ─────────────────────────────────────────

    // 守护（修复前后都通过）：商户甲员工归还本商户的设备 200；登记一台新设备、所属商户填本商户 200。
    @Test
    void aMerchantEmployeeManagesTheirOwnMerchantsDevices() throws Exception {
        device("mine", "bound", merchantA, null, null);
        send(merchantStaff, "POST", DEVICES + "/return", returnJson(key("mine"), "mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("returned"));

        send(merchantStaff, "POST", DEVICES + "/bind", bindJson(key("mine-new"), "minenew", merchantA, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("bound"))
                .andExpect(jsonPath("$.data.merchant_id").value(merchantA));
    }

    // ── 认不出项目的设备、平台管理员 ───────────────────────────────────────────────

    // 三样都没填（没有所属商户、绑定空间、持有员工）的设备只有平台管理员能管：项目员工送修 → 403 CROSS_PROJECT_FORBIDDEN，
    // 商户员工送修 → 403 CROSS_MERCHANT_FORBIDDEN，设备行原样；平台管理员送修 → 200。
    // 修复前：项目员工那一处即红（200，设备已变为 repair）。
    @Test
    void aDeviceWhoseProjectCannotBeToldIsManagedOnlyByAPlatformAdmin() throws Exception {
        String id = device("unk", "bound", null, null, null);
        expectRejectedWithoutWrite(projectStaff, "POST", DEVICES + "/" + id + "/repair", key("unk-p"),
                reasonJson(key("unk-p")), "CROSS_PROJECT_FORBIDDEN", id);
        expectRejectedWithoutWrite(merchantStaff, "POST", DEVICES + "/" + id + "/repair", key("unk-m"),
                reasonJson(key("unk-m")), "CROSS_MERCHANT_FORBIDDEN", id);

        send(admin, "POST", DEVICES + "/" + id + "/repair", reasonJson(key("unk-x")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("repair"));
    }

    // 守护（修复前后都通过）：平台管理员不受范围限制——另一项目空间上的设备报废 200；把项目甲商户乙的设备改到项目乙的商户丙 200。
    @Test
    void aPlatformAdminManagesDevicesOfAnyProjectAndMerchant() throws Exception {
        String foreign = device("xa", "bound", null, spaceB, null);
        send(admin, "POST", DEVICES + "/" + foreign + "/scrap", reasonJson(key("xa")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("scrapped"));

        String ofMerchant = device("xb", "bound", merchantB, null, null);
        send(admin, "PUT", DEVICES + "/" + ofMerchant + "/merchant", merchantJson(key("xb"), merchantC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merchant_id").value(merchantC));
    }

    // ── 夹具与断言 ────────────────────────────────────────────────────────────

    // 必填列照 GiftRedemptionEndToEndTests 的商户夹具，另填所属项目（V432，外键指向 projects）。
    private void merchant(String id, String project) {
        jdbcTemplate.update("""
                INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)
                VALUES (?, ?, 'bar', 'active', now(), now(), ?)
                """, id, "M723-F21 " + id, project);
    }

    // 必填列照 VenueBusinessHoursServiceTests 的空间夹具。
    private void space(String id, String project) {
        jdbcTemplate.update("INSERT INTO spaces (id, project_id, space_name, space_type, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, 'shop', 'active', now(), now())", id, project, "M723-F21 " + id);
    }

    // 员工档案（设备的 staff_id 存员工档案 id）：只用到所属项目。必填列照 StaffJdbcIntegrationTests.seedProfile。
    private void holder(String id, String project) {
        jdbcTemplate.update("""
                INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, company_id,
                        registration_status, status, created_at, updated_at)
                VALUES (?, ?, NULL, ?, 'scoped', ?, 'approved', 'active', now(), now())
                """, id, project, "usr_" + id, project);
    }

    // 操作人账号（都属于项目甲）＋长期来源：一条非单次的临时授权，带十个入口用到的四个权限码。
    private void user(String id, String accountType, String merchant) {
        Instant now = Instant.now();
        accessRepository.createUser(new AccessUser(id, id, accountType, projectA, merchant, "active", now));
        accessRepository.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), id, CODES,
                now.minusSeconds(3600), now.plusSeconds(86_400), false, false, "active", "M723-F21 设备归属范围回归",
                "seed-admin", now, null, null));
    }

    // 一台终端设备（平板，不涉及一体机令牌）；所属商户、绑定空间、持有员工都可为空。返回记录 id（接口路径里的 id）。
    private String device(String tag, String status, String merchant, String space, String holder) {
        String id = "td_m723s_" + tag + "_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope,
                    bound_space_id, merchant_id, staff_id, status, created_at, updated_at)
                VALUES (?, ?, ?, 'tablet', 'space', ?, ?, ?, ?, now(), now())
                """, id, deviceId(tag), deviceCode(tag), space, merchant, holder, status);
        return id;
    }

    private String deviceId(String tag) {
        return "dev-m723s-" + tag + "-" + suffix;
    }

    private String deviceCode(String tag) {
        return "DC-m723s-" + tag + "-" + suffix;
    }

    private String key(String name) {
        return "m723s-" + name + "-" + suffix;
    }

    private String returnJson(String key, String tag) {
        return """
                {"idempotency_key": "%s", "device_code": "%s", "has_pending_tasks": false, "reason": "M723-F21 回归"}
                """.formatted(key, deviceCode(tag));
    }

    // 解绑与四个生命周期入口的请求体相同：提交编号＋原因。
    private String reasonJson(String key) {
        return """
                {"idempotency_key": "%s", "reason": "M723-F21 回归"}
                """.formatted(key);
    }

    private String merchantJson(String key, String merchant) {
        return """
                {"idempotency_key": "%s", "merchant_id": "%s", "reason": "M723-F21 回归"}
                """.formatted(key, merchant);
    }

    // 登记请求：设备号由标记拼出；所属商户、绑定空间可为空。
    private String bindJson(String key, String tag, String merchant, String space) {
        return """
                {"idempotency_key": "%s", "device_id": "%s", "terminal_type": "tablet", "binding_scope": "space",
                 "bound_space_id": %s, "merchant_id": %s}
                """.formatted(key, deviceId(tag), quoted(space), quoted(merchant));
    }

    private String quoted(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    // 后台 Web 终端 + 生产过滤器放入的 AccessActor 主体（与员工表当前行一致）。
    private RequestPostProcessor session(String user) {
        String accountType = admin.equals(user) ? "platform_admin" : "staff";
        String merchant = merchantStaff.equals(user) ? merchantA : null;
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(
                    new AccessActor(user, user, accountType, projectA, merchant, "active"), null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private ResultActions send(String user, String method, String url, String body) throws Exception {
        return mockMvc.perform(("PUT".equals(method) ? put(url) : post(url)).with(session(user))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    // 越范围被拒且没有写入：403＋拒绝码；设备行原样、没有设备状态流水、该提交编号没有幂等记录、没有成功留痕。
    private void expectRejectedWithoutWrite(String user, String method, String url, String key, String body,
            String detailCode, String rowId) throws Exception {
        String before = deviceRow(rowId);
        send(user, method, url, body)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value(detailCode));
        assertThat(deviceRow(rowId)).isEqualTo(before);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM terminal_device_status_events WHERE device_id = ?", Integer.class, rowId)).isZero();
        assertNothingRecorded(key);
    }

    // 登记新设备被拒：403＋拒绝码；库里没有这台设备、该提交编号没有幂等记录、没有成功留痕。
    private void expectBindRejected(String user, String key, String body, String detailCode, String tag) throws Exception {
        send(user, "POST", DEVICES + "/bind", body)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value(detailCode));
        assertThat(devicesWithDeviceId(tag)).isZero();
        assertNothingRecorded(key);
    }

    private void assertNothingRecorded(String key) {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM idempotency_records WHERE idempotency_key = ?", Integer.class, key)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events WHERE event_result = 'success' AND idempotency_key = ?",
                Integer.class, key)).isZero();
    }

    private Integer devicesWithDeviceId(String tag) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM terminal_devices WHERE device_id = ?", Integer.class, deviceId(tag));
    }

    // 设备行里会被十个入口改到的列，拼成一串比较前后是否原样。
    private String deviceRow(String rowId) {
        return jdbcTemplate.queryForObject("""
                SELECT concat_ws('|', status, coalesce(merchant_id, ''), coalesce(bound_space_id, ''), coalesce(staff_id, ''),
                                 coalesce(returned_at::text, ''), coalesce(unbound_at::text, ''), updated_at::text)
                  FROM terminal_devices
                 WHERE id = ?
                """, String.class, rowId);
    }

    // 设备列表里各行的记录 id。
    private List<String> listedDeviceIds(String user) throws Exception {
        String body = mockMvc.perform(get(DEVICES).with(session(user)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<String> ids = new ArrayList<>();
        for (JsonNode item : new ObjectMapper().readTree(body).get("data").get("items")) {
            ids.add(item.path("id").asText());
        }
        return ids;
    }
}
