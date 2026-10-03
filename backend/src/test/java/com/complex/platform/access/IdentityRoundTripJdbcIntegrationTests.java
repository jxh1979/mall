package com.complex.platform.access;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * M722-F05（治理地图第 653 行项目与商户隔离、第 658 行跨项目或换商户后旧范围派生权限立即失效；技术推导）真库＋真实接口回归：
 * 一次权限判定进行中，操作人的账号归属改走又改回——中间范围已撤销的岗位角色授予不得配回原范围身份。
 *
 * <p>生产装配原样起；只把鉴权层拿到的仓储包一层（原样转给真实 JDBC 仓储），用来在「读能力事实」那一刻插入另一事务的提交。
 * 类上不加事务：夹具与交错中的三段事务都真实提交，各用例结束后清理（编号带随机后缀）。
 * 200 用例经 InventoryService 另起事务写下的一条库存查看审计行不清理（与本包其它真实提交的回归同口径）。
 *
 * <p>本类的静态助手同时供 AccessJdbcIntegrationTests 的 M722-F05 真库用例使用。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class IdentityRoundTripJdbcIntegrationTests {

    private static final String STOCK_VIEW = "inventory.stock.view";
    private static final String STOCK_BALANCES = "/api/v1/inventory/stock-balances";
    static final String IDENTITY_CHANGED = "账号归属或状态已变更";

    @TestConfiguration
    static class HookedRepositoryConfig {

        @Bean
        @Primary
        AccessRepository hookedAccessRepository(JdbcAccessRepository repository) {
            return Mockito.mock(AccessRepository.class, AdditionalAnswers.delegatesTo(repository));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccessRepository hooked;

    @Autowired
    private JdbcAccessRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    // 验收③前半句「经库存查询接口，往返后的请求被拒、拿不到数据」：员工在商户 A（岗位角色不含库存查看）；判定读能力事实那一刻，
    // 另一事务依次提交：改挂商户 B、B 岗位角色（含库存查看）落地；读取之后再提交改回商户 A。修复前这次请求 200 并带回商户 A 的库存余额。
    @Test
    void stockBalancesAreRefusedWhenMerchantMovesAwayAndBackDuringTheJudgment() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String project = "org_f05_h_" + suffix;
        String merchantA = "mch_f05_ha_" + suffix;
        String merchantB = "mch_f05_hb_" + suffix;
        String operator = "usr_f05_h_" + suffix;
        String roleA = "role_f05_ha_" + suffix;
        String roleStock = "role_f05_hs_" + suffix;
        String balance = "bal_f05_h_" + suffix;
        try {
            seedRole(repository, roleA, List.of());
            seedRole(repository, roleStock, List.of(STOCK_VIEW));
            seedStaff(repository, operator, project, merchantA, roleA, suffix);
            seedStockBalance(balance, merchantA, suffix);
            RequestPostProcessor session = session(operator, project, merchantA);

            // 对照：没有在途变更时，商户 A 的装配本就看不了库存——拒绝来自无权，不是身份核对
            mockMvc.perform(get(STOCK_BALANCES).with(session))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"))
                    .andExpect(jsonPath("$.message").value(not(containsString(IDENTITY_CHANGED))));

            RoundTrip trip = armScopeRoundTrip(hooked, repository, jdbcTemplate, transactionManager,
                    operator, project, merchantA, project, merchantB, roleStock, suffix);

            String body = mockMvc.perform(get(STOCK_BALANCES).with(session))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"))
                    .andExpect(jsonPath("$.message").value(containsString(IDENTITY_CHANGED)))
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(body).doesNotContain(balance); // 拿不到商户 A 的数据
            assertRoundTripCommitted(repository, trip, operator, project, merchantA, roleStock, STOCK_VIEW);
        } finally {
            cleanup(jdbcTemplate, List.of(operator), List.of(roleA, roleStock), suffix);
        }
    }

    // 验收③后半句「身份未变的同一装配照常 200」：同一套装配（含包了一层的仓储）、同一个含库存查看的岗位角色，员工稳定在商户 A——
    // 照常 200 并拿到商户 A 的那条余额（也证明上一条用例若放行就真能拿到数据）。修复前后都绿。
    @Test
    void stockBalancesReturn200ForAnUnchangedIdentityWithTheSameAssembly() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String project = "org_f05_s_" + suffix;
        String merchantA = "mch_f05_sa_" + suffix;
        String staff = "usr_f05_s_" + suffix;
        String roleStock = "role_f05_ss_" + suffix;
        String balance = "bal_f05_s_" + suffix;
        try {
            seedRole(repository, roleStock, List.of(STOCK_VIEW));
            seedStaff(repository, staff, project, merchantA, roleStock, suffix);
            seedStockBalance(balance, merchantA, suffix);

            mockMvc.perform(get(STOCK_BALANCES).with(session(staff, project, merchantA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items[*].id").value(hasItem(balance)));
        } finally {
            cleanup(jdbcTemplate, List.of(staff), List.of(roleStock), suffix);
        }
    }

    // 不带终端头：读请求没有终端类型时终端总闸直接放行，结果不取决于库里的开关行。
    private RequestPostProcessor session(String userId, String project, String merchant) {
        return authentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(userId, userId, "staff", project, merchant, "active"), null,
                List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF"))));
    }

    private void seedStockBalance(String balanceId, String merchant, String suffix) {
        jdbcTemplate.update("""
                INSERT INTO inventory_items (id, item_name, item_type, unit, status, created_at, updated_at)
                VALUES (?, 'M722-F05 回归物料', 'goods', '件', 'active', now(), now())
                """, itemId(suffix));
        jdbcTemplate.update("""
                INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity,
                                                      updated_at, merchant_id)
                VALUES (?, ?, ?, 5, 0, 5, now(), ?)
                """, balanceId, itemId(suffix), "loc_f05_" + suffix, merchant);
    }

    private static String itemId(String suffix) {
        return "item_f05_" + suffix;
    }

    // ── 以下静态助手与 AccessJdbcIntegrationTests 的 M722-F05 用例共用 ──────────────────────────────────────────────

    /** 往返交错的观测：按操作人读能力事实的次数、在中间范围那一刻读到的能力事实。 */
    record RoundTrip(AtomicInteger capabilityReads, AtomicReference<List<AccessRepository.PermissionEffect>> readInMiddleScope) {
    }

    static void seedRole(JdbcAccessRepository repository, String roleId, List<String> permissionCodes) {
        repository.createRole(new RoleRecord(roleId, "F05 岗位角色 " + roleId, null, "project", "active", Instant.now(),
                permissionCodes, null));
    }

    /** 员工账号＋岗位行＋岗位派生角色（定岗落地所用的同一组仓储写入）。 */
    static void seedStaff(JdbcAccessRepository repository, String userId, String project, String merchant, String positionRole,
                          String suffix) {
        Instant now = Instant.now();
        repository.createUser(new AccessUser(userId, userId, "staff", project, merchant, "active", now));
        repository.saveUserPositionAssignment(new UserPositionAssignment(
                userId, positionCode(suffix), "lvl_m1_lower", project, merchant, "seed-admin", now));
        repository.replacePositionGrants(userId, positionRole, null, List.of(), "seed-admin", now);
    }

    /**
     * 在 hooked（原样转给真实仓储的包装）上挂交错：判定第一次按操作人读能力事实时，另一事务依次真实提交——
     * ① 改走（与 changeUserScope 同一组仓储写入：清旧岗位派生、改账号归属、建中间范围待审单并存指纹）；
     * ② 中间范围岗位角色落地（与审批落地同一组仓储写入：岗位行、岗位派生角色、待审单置 applied）；
     * ③ 读能力事实（判定拿到的就是这一份）；④ 改回（同 ①，建原范围待审单）。
     * 每段事务先取 changeUserScope 与审批入口所取的同一把授权事实锁（判定读方不被它挡住；万一互等，5 秒超时变成失败而不是挂死）。
     * 夹具里操作人没有直接角色、单独模板套用、临时授权、单独授予与挂起行，这几类撤销是空操作，从略；
     * 夹具不设岗位模板与职级行，审批落地里的模板套用与职级落地这两步未做，不影响本项验证面（被读到的是岗位角色授予）。
     */
    static RoundTrip armScopeRoundTrip(AccessRepository hooked, JdbcAccessRepository repository, JdbcTemplate jdbcTemplate,
                                       PlatformTransactionManager transactionManager, String operator,
                                       String projectA, String merchantA, String projectB, String merchantB,
                                       String middleRole, String suffix) {
        RoundTrip trip = new RoundTrip(new AtomicInteger(), new AtomicReference<>());
        TransactionTemplate otherTransaction = new TransactionTemplate(transactionManager);
        otherTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Mockito.doAnswer(invocation -> {
            Instant checkedAt = invocation.getArgument(1);
            if (trip.capabilityReads().getAndIncrement() != 0) {
                return repository.findPermissionEffects(operator, checkedAt);
            }
            String middleRequest = "agr_f05_b_" + suffix;
            otherTransaction.executeWithoutResult(status -> {
                lockAsWriters(repository, jdbcTemplate);
                changeScope(repository, operator, projectB, merchantB, middleRequest, middleRole, suffix);
            });
            otherTransaction.executeWithoutResult(status -> {
                lockAsWriters(repository, jdbcTemplate);
                Instant at = Instant.now();
                repository.saveUserPositionAssignment(new UserPositionAssignment(
                        operator, positionCode(suffix), "lvl_m1_lower", projectB, merchantB, "seed-admin", at));
                repository.replacePositionGrants(operator, middleRole, null, List.of(), "seed-admin", at);
                repository.saveGrantRequest(scopeChangeRequest(middleRequest, operator, middleRole, projectB, merchantB,
                        "applied", suffix, at));
            });
            List<AccessRepository.PermissionEffect> read = repository.findPermissionEffects(operator, checkedAt);
            trip.readInMiddleScope().set(read);
            otherTransaction.executeWithoutResult(status -> {
                lockAsWriters(repository, jdbcTemplate);
                changeScope(repository, operator, projectA, merchantA, "agr_f05_a_" + suffix, null, suffix);
            });
            return read;
        }).when(hooked).findPermissionEffects(ArgumentMatchers.eq(operator), ArgumentMatchers.any());
        return trip;
    }

    /** 验收②③的四件事：中间范围的岗位角色授予已被读到、改回已提交、中间范围派生已清、原范围的新授权申请仍待审且没有已生效的新授权。 */
    static void assertRoundTripCommitted(JdbcAccessRepository repository, RoundTrip trip, String operator,
                                         String projectA, String merchantA, String middleRole, String code) {
        assertThat(trip.capabilityReads().get()).isEqualTo(1); // 交错确在这次判定读能力事实时发生
        assertThat(trip.readInMiddleScope().get()).anySatisfy(effect -> {
            assertThat(effect.source()).isEqualTo(AccessRepository.Source.ROLE);
            assertThat(effect.sourceId()).isEqualTo(middleRole);
            assertThat(effect.permissionCode()).isEqualTo(code);
            assertThat(effect.effect()).isEqualTo(AccessRepository.Effect.GRANT);
        });
        AccessUser row = repository.findUserById(operator).orElseThrow();
        assertThat(row.organizationId()).isEqualTo(projectA);
        assertThat(row.merchantId()).isEqualTo(merchantA);
        assertThat(repository.findAssignedPositionRoleId(operator)).isEmpty();
        assertThat(repository.listPendingGrantRequests(operator, AccessService.REQUEST_USER_SCOPE_CHANGE))
                .singleElement().satisfies(pending -> {
                    assertThat(pending.projectId()).isEqualTo(projectA);
                    assertThat(pending.merchantId()).isEqualTo(merchantA);
                });
        // 没有已生效的新授权：权限效果只按本人行取，夹具里本人没有其它授予来源，改回后不应剩任何授予
        assertThat(repository.findPermissionEffects(operator, Instant.now()))
                .noneMatch(effect -> effect.effect() == AccessRepository.Effect.GRANT);
    }

    /** 授权申请对账号没有级联，先删；岗位行随账号级联删除，角色在账号之后删（岗位行引用角色）。 */
    static void cleanup(JdbcTemplate jdbcTemplate, List<String> userIds, List<String> roleIds, String suffix) {
        jdbcTemplate.update("DELETE FROM inventory_stock_balances WHERE item_id = ?", itemId(suffix));
        jdbcTemplate.update("DELETE FROM inventory_items WHERE id = ?", itemId(suffix));
        for (String userId : userIds) {
            jdbcTemplate.update("DELETE FROM access_grant_requests WHERE target_user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM access_users WHERE id = ?", userId);
        }
        for (String roleId : roleIds) {
            jdbcTemplate.update("DELETE FROM role_permissions WHERE role_id = ?", roleId);
            jdbcTemplate.update("DELETE FROM roles WHERE id = ?", roleId);
        }
    }

    private static void lockAsWriters(JdbcAccessRepository repository, JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("SET LOCAL lock_timeout = '5s'");
        repository.lockGrantAuthorizationFacts();
    }

    private static void changeScope(JdbcAccessRepository repository, String operator, String project, String merchant,
                                    String requestId, String targetRole, String suffix) {
        Instant at = Instant.now();
        repository.replacePositionGrants(operator, null, null, List.of(), "seed-admin", at); // 旧派生立即失效
        repository.updateUserScope(operator, project, merchant, "seed-admin", at);
        repository.saveGrantRequest(scopeChangeRequest(requestId, operator, targetRole, project, merchant, "pending", suffix, at));
        repository.saveGrantFingerprint(requestId, "f05-" + requestId);
    }

    private static AccessGrantRequestRecord scopeChangeRequest(String requestId, String operator, String targetRole,
                                                               String project, String merchant, String status,
                                                               String suffix, Instant at) {
        boolean decided = !"pending".equals(status);
        return new AccessGrantRequestRecord(requestId, AccessService.REQUEST_USER_SCOPE_CHANGE, operator, targetRole, null,
                positionCode(suffix), "lvl_m1_lower", null, "CNY", null, List.of(), null, null, false, status, null,
                "M722-F05 回归", "seed-admin", decided ? "seed-admin" : null, decided ? at : null, project, merchant, at, at);
    }

    private static String positionCode(String suffix) {
        return "pos_f05_" + suffix;
    }
}
