// 文件版本：V0.03
package com.complex.platform.serviceorder;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.auth.AuthService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 服务订单域资金安全 TDD(2026-07-05 审计四项)。
 * 并发用真多线程复现;非 @Transactional(并发跨连接需真提交,测试自行清理构造数据)。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class ServiceOrderSafetyTests {

    @Autowired
    private ServiceOrderService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        authenticateAs("usr_admin");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** 1【P1】同一 paid 单并发多次 complete(不同幂等键)→ 只 1 次成功,审计只 1 条完成,提成只 1 条。 */
    @Test
    void concurrentCompleteOnlyOneWins() throws Exception {
        ServiceCatalogItemRecord item = service.upsertCatalogItem(
                new com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest(
                        key(), null, "并发舞", 30000L, 15, true, "percent", 1000L));
        String performer = "usr_perf_" + key();
        ServiceOrderRecord order = service.createOrder(
                new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(
                        key(), item.id(), performer, null, null, null));
        service.markPaid(order.id(),
                new com.complex.platform.serviceorder.dto.ServiceOrderMarkPaidRequest(key(), null));

        long auditBefore = completedAuditCount(order.id());
        int threads = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        try {
            List<Callable<Void>> tasks = new java.util.ArrayList<>();
            for (int i = 0; i < threads; i++) {
                tasks.add(() -> {
                    authenticateAs("usr_admin");
                    try {
                        service.complete(order.id(),
                                new com.complex.platform.serviceorder.dto.ServiceOrderActionRequest(key()));
                        ok.incrementAndGet();
                    } catch (RuntimeException e) {
                        rejected.incrementAndGet();
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                    return null;
                });
            }
            for (Future<Void> f : pool.invokeAll(tasks)) {
                f.get();
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(ok.get()).as("并发 complete 只允许 1 次成功").isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(threads - 1);
        assertThat(completedAuditCount(order.id()) - auditBefore)
                .as("完成审计只 1 条").isEqualTo(1);
        assertThat(commissionCount(order.id())).as("提成台账只 1 条").isEqualTo(1);
        cleanupOrder(order.id());
    }

    /** 2【P1】mark-paid 后产生一条公司应收/已收流水(带订单号+金额+确认人),供对账查询。 */
    @Test
    void markPaidCreatesReceivableLedgerRow() {
        ServiceCatalogItemRecord item = service.upsertCatalogItem(
                new com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest(
                        key(), null, "对账舞", 12345L, 10, true, "none", 0L));
        String performer = "usr_perf_" + key();
        ServiceOrderRecord order = service.createOrder(
                new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(
                        key(), item.id(), performer, null, null, null));
        service.markPaid(order.id(),
                new com.complex.platform.serviceorder.dto.ServiceOrderMarkPaidRequest(key(), null));

        // 应收流水:一条 receivable、金额=订单价、source 关联订单号、已收=全额、确认人=paid_by。
        var row = jdbcTemplate.queryForMap(
                "SELECT amount_cent, paid_amount_cent, direction, status FROM ar_ap_items "
                        + "WHERE source_type='service_order' AND source_id=?", order.id());
        assertThat(((Number) row.get("amount_cent")).longValue()).isEqualTo(12345L);
        assertThat(((Number) row.get("paid_amount_cent")).longValue()).isEqualTo(12345L);
        assertThat(row.get("direction")).isEqualTo("receivable");
        // 确认人可从订单 paid_by 对账。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT paid_by FROM service_orders WHERE id=?", String.class, order.id()))
                .isEqualTo("usr_admin");
        cleanupOrder(order.id());
    }

    /** 3【P2】下单后改目录费率,完成时应按下单快照费率分账(非活目录)。 */
    @Test
    void completeUsesOrderTimeFeeSnapshotNotLiveCatalog() {
        String itemId = "svc_snap_" + key();
        ServiceCatalogItemRecord item = service.upsertCatalogItem(
                new com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest(
                        key(), itemId, "快照舞", 30000L, 10, true, "percent", 1000L)); // 10%
        assertThat(item.id()).isEqualTo(itemId);
        String performer = "usr_perf_" + key();
        ServiceOrderRecord order = service.createOrder(
                new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(
                        key(), item.id(), performer, null, null, null));
        service.markPaid(order.id(),
                new com.complex.platform.serviceorder.dto.ServiceOrderMarkPaidRequest(key(), null));
        // 完成前把目录费率改高到 50%(想压舞者提成)。TK-23-R3 后 upsert 不再直改费率,直接改库模拟活目录已变。
        jdbcTemplate.update("UPDATE service_catalog_items SET mgmt_fee_value = 5000 WHERE id = ?", itemId);
        // 探针:确认活目录费率已被改到 50%(否则本用例不成立)。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT mgmt_fee_value FROM service_catalog_items WHERE id=?", Long.class, itemId))
                .as("活目录已改为 50%").isEqualTo(5000L);

        ServiceOrderRecord done = service.complete(order.id(),
                new com.complex.platform.serviceorder.dto.ServiceOrderActionRequest(key()));
        // 应按下单时 10% 快照:管理费 3000,舞者 27000(而非改后 50% → 15000/15000)。
        assertThat(done.mgmtFeeCent()).as("按下单快照 10% 分账").isEqualTo(3000L);
        assertThat(done.dancerShareCent()).isEqualTo(27000L);
        cleanupOrder(order.id());
        jdbcTemplate.update("DELETE FROM service_catalog_items WHERE id=?", itemId);
    }

    /** 4【P2】accepted 且 paid_at 为 null 的单 complete 应拒(无收款不得完成)。 */
    @Test
    void acceptedWithoutPaymentCannotComplete() {
        ServiceCatalogItemRecord item = service.upsertCatalogItem(
                new com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest(
                        key(), null, "潜伏舞", 20000L, 10, true, "none", 0L));
        String performer = "usr_perf_" + key();
        ServiceOrderRecord order = service.createOrder(
                new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(
                        key(), item.id(), performer, null, null, null));
        // 直接把状态改成 accepted 且 paid_at=null(模拟遗留 accepted 路径)。
        jdbcTemplate.update("UPDATE service_orders SET status='accepted', paid_at=NULL WHERE id=?", order.id());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.complete(order.id(),
                new com.complex.platform.serviceorder.dto.ServiceOrderActionRequest(key())))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("NOT_PAID_YET"));
        // 未产生提成、状态仍非 completed。
        assertThat(commissionCount(order.id())).isEqualTo(0);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM service_orders WHERE id=?", String.class, order.id()))
                .isNotEqualTo("completed");
        cleanupOrder(order.id());
    }

    // ───────────── TK-23-R1:固定金额管理费越界保存即拒绝,不静默封顶 ─────────────

    /** ENUM L291「固定金额必须在 0 至服务售价（含边界）之间;任一越界值在保存时直接拒绝」。 */
    @Test
    void fixedFeeAboveServicePriceIsRejectedOnSave() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.upsertCatalogItem(
                new com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest(
                        key(), null, "越界固定费", 10000L, 10, true, "fixed", 10001L)))
                .as("固定管理费 > 售价须在保存时拒绝")
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
    }

    /** 边界 0 与 =售价放行;=售价的单完成后管理费=售价、舞者 0。 */
    @Test
    void fixedFeeBoundariesZeroAndEqualToPriceAccepted() {
        ServiceCatalogItemRecord zero = service.upsertCatalogItem(
                new com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest(
                        key(), null, "零固定费", 10000L, 10, true, "fixed", 0L));
        assertThat(zero.mgmtFeeValue()).isEqualTo(0L);
        ServiceCatalogItemRecord full = service.upsertCatalogItem(
                new com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest(
                        key(), null, "满额固定费", 10000L, 10, true, "fixed", 10000L));
        ServiceOrderRecord order = service.createOrder(
                new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(
                        key(), full.id(), "usr_perf_" + key(), null, null, null));
        service.markPaid(order.id(), new com.complex.platform.serviceorder.dto.ServiceOrderMarkPaidRequest(key(), null));
        ServiceOrderRecord done = service.complete(order.id(),
                new com.complex.platform.serviceorder.dto.ServiceOrderActionRequest(key()));
        assertThat(done.mgmtFeeCent()).isEqualTo(10000L);
        assertThat(done.dancerShareCent()).isEqualTo(0L);
        cleanupOrder(order.id());
    }

    // ───────────── TK-23-R2:归属月按完成时点所在营业日 ─────────────

    @Autowired
    private com.complex.platform.common.BusinessDayResolver businessDayResolver;

    @Autowired
    private com.complex.platform.access.AccessRepository accessRepository;

    /** service.catalog.approve 为高危码,默认管理员角色不作逐项授权;按真实范式建用户(与 authenticateAs 同账号类型/组织)并单独授予
     * (user_permission_overrides)。起草人同时授 manage+approve,用于验证「有权复核但不得自审」。 */
    private String userGranted(String... permissionCodes) {
        String userId = "usr_svc_" + key();
        accessRepository.createUser(new com.complex.platform.access.AccessUser(
                userId, userId, "platform_admin", "seed-org", null, "active", Instant.now()));
        accessRepository.saveUserPermissionOverrides(userId, List.of(permissionCodes),
                com.complex.platform.access.AccessRepository.Effect.GRANT, "服务管理费草案测试授予", "usr_admin");
        return userId;
    }

    /** 提成台账 month 与完成时点的 bar 营业日年月一致(守一致性)。 */
    @Test
    void completionLedgerMonthMatchesBusinessDayOfCompletedAt() {
        ServiceOrderRecord order = paidOrder("归属月舞", 20000L);
        service.complete(order.id(), new com.complex.platform.serviceorder.dto.ServiceOrderActionRequest(key()));
        Instant completedAt = jdbcTemplate.queryForObject(
                "SELECT completed_at FROM service_orders WHERE id=?", java.sql.Timestamp.class, order.id()).toInstant();
        String month = jdbcTemplate.queryForObject(
                "SELECT month FROM staff_commission_ledger WHERE source_type='service_order' AND source_id=?",
                String.class, order.id());
        assertThat(month).isEqualTo(java.time.YearMonth.from(businessDayResolver.businessDate(null, null, completedAt)).toString());
        cleanupOrder(order.id());
    }

    /** 应收行 accounting_period 按收款时点的 bar 营业日年月(守护用例)。 */
    @Test
    void markPaidReceivablePeriodUsesBusinessDay() {
        ServiceOrderRecord order = paidOrder("营业日应收舞", 6000L);
        Instant paidAt = jdbcTemplate.queryForObject(
                "SELECT paid_at FROM service_orders WHERE id=?", java.sql.Timestamp.class, order.id()).toInstant();
        var row = jdbcTemplate.queryForMap(
                "SELECT accounting_period, due_date FROM ar_ap_items WHERE id=?", "arap_svo_" + order.id());
        java.time.LocalDate businessDate = businessDayResolver.businessDate(null, null, paidAt);
        assertThat(row.get("accounting_period")).isEqualTo(java.time.YearMonth.from(businessDate).toString());
        assertThat(((java.sql.Date) row.get("due_date")).toLocalDate()).isEqualTo(businessDate);
        cleanupOrder(order.id());
    }

    // ───────────── TK-23-R3:管理费变更比照销售提成——草案→差人转正→指定生效日版本 ─────────────

    /** 已有目录项带不同 mgmt_fee_* 直接 upsert → FEE_CHANGE_REQUIRES_DRAFT(不再直改生效)。 */
    @Test
    void upsertCannotSilentlyChangeFeeOfActiveItem() {
        String itemId = "svc_feechg_" + key();
        service.upsertCatalogItem(new com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest(
                key(), itemId, "费率直改舞", 30000L, 10, true, "percent", 1000L));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.upsertCatalogItem(
                new com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest(
                        key(), itemId, "费率直改舞", 30000L, 10, true, "percent", 2000L)))
                .as("已上架项改管理费须走草案")
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("FEE_CHANGE_REQUIRES_DRAFT"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT mgmt_fee_value FROM service_catalog_items WHERE id=?", Long.class, itemId)).isEqualTo(1000L);
        jdbcTemplate.update("DELETE FROM service_catalog_items WHERE id=?", itemId);
    }

    /** A(同时持有 manage+approve)提草案后 A 自己转正 → SELF_APPROVAL_FORBIDDEN;B 转正成功(只标记复核人,现行费率未变)。 */
    @Test
    void feeChangeOnActiveItemRequiresDraftAndDifferentApprover() {
        String itemId = "svc_feedraft_" + key();
        service.upsertCatalogItem(new com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest(
                key(), itemId, "草案舞", 30000L, 10, true, "percent", 1000L));
        java.time.LocalDate today = businessDayResolver.businessDate(null, null, Instant.now());
        String drafter = userGranted("service.catalog.manage", "service.catalog.approve");
        authenticateAs(drafter);
        ServiceCatalogItemRecord drafted = service.draftFeeChange(itemId,
                new com.complex.platform.serviceorder.dto.ServiceCatalogFeeDraftRequest(key(), "percent", 2000L, today));
        assertThat(drafted.nextRequestedBy()).isEqualTo(drafter);
        assertThat(drafted.nextMgmtFeeValue()).isEqualTo(2000L);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.activateFeeDraft(itemId, key()))
                .as("起草人不得自审转正")
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("SELF_APPROVAL_FORBIDDEN"));
        String reviewer = userGranted("service.catalog.approve");
        authenticateAs(reviewer);
        ServiceCatalogItemRecord activated = service.activateFeeDraft(itemId, key());
        assertThat(activated.nextApprovedBy()).isEqualTo(reviewer);
        assertThat(activated.mgmtFeeValue()).as("转正只标记复核,生效日起才翻版本").isEqualTo(1000L);
        assertThat(activated.feeVersion()).isEqualTo(1);
        jdbcTemplate.update("DELETE FROM service_catalog_items WHERE id=?", itemId);
    }

    /** 复核 authz M4(PGM L303 幂等):草案同一请求编号重放返回第一次结果,不得把已有复核人抹成 NULL。 */
    @Test
    void feeDraftReplayWithSameKeyDoesNotWipeApproval() {
        String itemId = "svc_feeidem_" + key();
        service.upsertCatalogItem(new com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest(
                key(), itemId, "幂等草案舞", 30000L, 10, true, "percent", 1000L));
        java.time.LocalDate today = businessDayResolver.businessDate(null, null, Instant.now());
        var draft = new com.complex.platform.serviceorder.dto.ServiceCatalogFeeDraftRequest(key(), "percent", 2000L, today.plusDays(1));
        service.draftFeeChange(itemId, draft);
        String reviewer = userGranted("service.catalog.approve");
        authenticateAs(reviewer);
        service.activateFeeDraft(itemId, key());
        authenticateAs("usr_admin");

        service.draftFeeChange(itemId, draft);

        assertThat(jdbcTemplate.queryForObject("SELECT next_approved_by FROM service_catalog_items WHERE id=?", String.class, itemId))
                .as("重放不重写草案").isEqualTo(reviewer);
        jdbcTemplate.update("DELETE FROM service_catalog_items WHERE id=?", itemId);
    }

    @Autowired
    private com.complex.platform.common.crypto.PiiCipher piiCipher;

    /**
     * 操作员裁决 2026-10-20 第三轮第 8 条 + 第十批 blocked_questions 裁决(B10-BRIEF 第 10 条):管理费转正通告收件人＝
     * ①本项目在职、持 service.order.perform 权限的员工;②该目录项服务单发生过的酒吧的主理人(当前有效分红方案 principal_user_id),
     * 均按员工档案手机号发;不再发商户档案联系电话、不再按全部历史履约人发。
     */
    @Test
    void feeActivationNotifiesPerformersWithPermissionAndBarPrincipal() {
        String tag = key().substring(0, 10);
        String itemId = "svc_feentf_" + tag;
        service.upsertCatalogItem(new com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest(
                key(), itemId, "通告舞", 30000L, 10, true, "percent", 1000L));
        String bar = "mch_feentf_" + tag;
        String barPhone = phone(tag, 1);
        jdbcTemplate.update("INSERT INTO merchants(id,merchant_name,merchant_type,contact_phone,status,created_at,updated_at) VALUES(?,'通告酒吧','bar',?,'active',now(),now())",
                bar, piiCipher.encrypt(barPhone));
        String performerPhone = phone(tag, 2);
        String noPermPhone = phone(tag, 3);
        String resignedPhone = phone(tag, 4);
        String principalPhone = phone(tag, 5);
        String historyPhone = phone(tag, 6);
        staffUser("svp_ok_" + tag, "active", "active", performerPhone, true);
        staffUser("svp_np_" + tag, "active", "active", noPermPhone, false);
        staffUser("svp_rs_" + tag, "active", "inactive", resignedPhone, true);
        staffUser("svp_pr_" + tag, "active", "active", principalPhone, false);
        staffUser("svp_hi_" + tag, "active", "active", historyPhone, false);
        jdbcTemplate.update("INSERT INTO dividend_plans(id,merchant_id,principal_user_id,principal_name,status) VALUES(?,?,?,'主理人','active')",
                "dp_feentf_" + tag, bar, "svp_pr_" + tag);
        // 历史履约人(无 service.order.perform 权限)在该酒吧做过该服务:旧口径会通告,新口径不通告。
        service.createOrder(new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(
                key(), itemId, "svp_hi_" + tag, bar, null, null));
        java.time.LocalDate today = businessDayResolver.businessDate(null, null, Instant.now());
        service.draftFeeChange(itemId, new com.complex.platform.serviceorder.dto.ServiceCatalogFeeDraftRequest(
                key(), "percent", 2000L, today.plusDays(1)));
        authenticateAs(userGranted("service.catalog.approve"));
        service.activateFeeDraft(itemId, key());
        authenticateAs("usr_admin");

        assertThat(feeNotified(performerPhone)).as("持权限在职员工收到").isEqualTo(1);
        assertThat(feeNotified(principalPhone)).as("酒吧主理人收到").isEqualTo(1);
        assertThat(feeNotified(noPermPhone)).as("无权限员工不收").isZero();
        assertThat(feeNotified(resignedPhone)).as("离职员工不收").isZero();
        assertThat(feeNotified(barPhone)).as("商户档案联系电话不再收").isZero();
        assertThat(feeNotified(historyPhone)).as("无权限的历史履约人不再收").isZero();
        jdbcTemplate.update("UPDATE dividend_plans SET status='ended' WHERE id=?", "dp_feentf_" + tag);
    }

    private void staffUser(String userId, String userStatus, String profileStatus, String phone, boolean withPerform) {
        jdbcTemplate.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff','seed-org',NULL,?,now())",
                userId, userId, userStatus);
        jdbcTemplate.update("INSERT INTO staff_profiles(id,project_id,merchant_id,user_id,name,phone,registration_status,status,created_at,updated_at) VALUES(?,'seed-org',NULL,?,?,?,'approved',?,now(),now())",
                "sp_" + userId, userId, "员工" + userId.substring(4, 6), phone, profileStatus);
        if (withPerform) {
            accessRepository.saveUserPermissionOverrides(userId, List.of("service.order.perform"),
                    com.complex.platform.access.AccessRepository.Effect.GRANT, "管理费通告测试授予", "usr_admin");
        }
    }

    private static String phone(String tag, int n) {
        String digits = tag.replaceAll("\\D", "") + "00000000";
        return "177" + digits.substring(0, 7) + n;
    }

    private int feeNotified(String phone) {
        // 收件人手机号落密文（V547），按明文筛行须解密后比。
        return com.complex.platform.support.NotificationSendLogRows.ids(jdbcTemplate, piiCipher, "service", "catalog_fee_changed", phone::equals).size();
    }

    /** 草案生效日=明日营业日:今日下单锁旧版本 V1;生效日到了(改为昨日)再下单 → 新版本 V2;先前订单完成仍按旧快照分账。 */
    @Test
    void orderBeforeEffectiveDateLocksOldVersionAfterUsesNew() {
        String itemId = "svc_feever_" + key();
        service.upsertCatalogItem(new com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest(
                key(), itemId, "版本舞", 30000L, 10, true, "percent", 1000L));
        java.time.LocalDate today = businessDayResolver.businessDate(null, null, Instant.now());
        service.draftFeeChange(itemId, new com.complex.platform.serviceorder.dto.ServiceCatalogFeeDraftRequest(
                key(), "percent", 2000L, today.plusDays(1)));
        authenticateAs(userGranted("service.catalog.approve"));
        service.activateFeeDraft(itemId, key());
        authenticateAs("usr_admin");
        String performer = "usr_perf_" + key();
        ServiceOrderRecord before = service.createOrder(
                new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(key(), itemId, performer, null, null, null));
        var snapBefore = jdbcTemplate.queryForMap(
                "SELECT snap_mgmt_fee_value, snap_fee_version FROM service_orders WHERE id=?", before.id());
        assertThat(((Number) snapBefore.get("snap_mgmt_fee_value")).longValue()).as("生效日前下单锁旧值").isEqualTo(1000L);
        assertThat(((Number) snapBefore.get("snap_fee_version")).intValue()).isEqualTo(1);
        // 把生效日拨到昨日,模拟日期已到。
        jdbcTemplate.update("UPDATE service_catalog_items SET next_effective_from = ? WHERE id = ?",
                java.sql.Date.valueOf(today.minusDays(1)), itemId);
        ServiceOrderRecord after = service.createOrder(
                new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(key(), itemId, performer, null, null, null));
        var snapAfter = jdbcTemplate.queryForMap(
                "SELECT snap_mgmt_fee_value, snap_fee_version FROM service_orders WHERE id=?", after.id());
        assertThat(((Number) snapAfter.get("snap_mgmt_fee_value")).longValue()).as("生效日起下单用新值").isEqualTo(2000L);
        assertThat(((Number) snapAfter.get("snap_fee_version")).intValue()).isEqualTo(2);
        var live = jdbcTemplate.queryForMap(
                "SELECT mgmt_fee_value, fee_version, next_mgmt_fee_value, next_approved_by FROM service_catalog_items WHERE id=?", itemId);
        assertThat(((Number) live.get("mgmt_fee_value")).longValue()).isEqualTo(2000L);
        assertThat(((Number) live.get("fee_version")).intValue()).isEqualTo(2);
        assertThat(live.get("next_mgmt_fee_value")).isNull();
        assertThat(live.get("next_approved_by")).isNull();
        // 先前订单完成仍按旧快照 10% 分账:3000/27000。
        service.markPaid(before.id(), new com.complex.platform.serviceorder.dto.ServiceOrderMarkPaidRequest(key(), null));
        ServiceOrderRecord done = service.complete(before.id(),
                new com.complex.platform.serviceorder.dto.ServiceOrderActionRequest(key()));
        assertThat(done.mgmtFeeCent()).as("在途单按下单时锁定的旧版本分账").isEqualTo(3000L);
        assertThat(done.dancerShareCent()).isEqualTo(27000L);
        cleanupOrder(before.id());
        cleanupOrder(after.id());
        jdbcTemplate.update("DELETE FROM service_catalog_items WHERE id=?", itemId);
    }

    private ServiceOrderRecord paidOrder(String itemName, long priceCent) {
        ServiceCatalogItemRecord item = service.upsertCatalogItem(
                new com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest(
                        key(), null, itemName, priceCent, 10, true, "none", 0L));
        ServiceOrderRecord order = service.createOrder(
                new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(
                        key(), item.id(), "usr_perf_" + key(), null, null, null));
        return service.markPaid(order.id(),
                new com.complex.platform.serviceorder.dto.ServiceOrderMarkPaidRequest(key(), null));
    }

    private long completedAuditCount(String orderId) {
        Long c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE event_name='service.order.completed' "
                        + "AND business_id=?", Long.class, orderId);
        return c == null ? 0 : c;
    }

    private long commissionCount(String orderId) {
        Long c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM staff_commission_ledger WHERE source_type='service_order' AND source_id=?",
                Long.class, orderId);
        return c == null ? 0 : c;
    }

    private void cleanupOrder(String orderId) {
        // audit_events/staff_commission_ledger/ar_ap_items 均 append-only(触发器禁 DELETE),不清理——
        // 每单 source_id 为唯一 UUID,残留行不影响按 source_id 隔离的断言。
        jdbcTemplate.update("DELETE FROM service_orders WHERE id=?", orderId);
    }

    private String key() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void authenticateAs(String userId) {
        AuthService.UserRecord user = new AuthService.UserRecord(
                userId, userId, "hash", "platform_admin", "seed-org", null, "active",
                Instant.parse("2026-06-14T00:00:00Z"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, "token", List.of(new SimpleGrantedAuthority("ROLE_P0_SMOKE"))));
    }
}
