package com.complex.platform.ops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 集中数据清理工具（Tier-D）服务集成测试。以 bar_products 为清理目标验证四条安全闸：
 *  ① dry-run 只统计不删；② 只删【已存档超 N 天】的行、活数据/近期存档一律不删；
 *  ③ approved 才批量隐藏(软删)并记隐藏条数、rejected 不动；④ 禁自审（发起人≠审批人）。
 * 用 complex_test 库 + @Transactional 自动回滚，不污染共享库。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class DataCleanupServiceTests {

    private static final String SCOPE = "archived_bar_products";

    @Autowired
    private DataCleanupService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    /** 插一条 bar_product 并置 archived_at（archivedDaysAgo>=0 表示已存档；<0 表示不存档=活数据）。 */
    private void seedProduct(String id, int archivedDaysAgo) {
        Instant now = Instant.now();
        jdbcTemplate.update("""
                INSERT INTO bar_products (id, bar_id, merchant_id, product_name, product_type, price_cent,
                    status, created_at, updated_at, archived_at, delete_status)
                VALUES (?, ?, ?, ?, ?, ?, 'active', ?, ?, ?, 'none')
                """,
                id, "bar_cleanup_test", "merchant_cleanup_test", "清理测试-" + id, "spirit", 5000L,
                java.sql.Timestamp.from(now), java.sql.Timestamp.from(now),
                archivedDaysAgo < 0 ? null
                        : java.sql.Timestamp.from(now.minus(archivedDaysAgo, ChronoUnit.DAYS)));
    }

    private boolean exists(String id) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM bar_products WHERE id = ?", Integer.class, id);
        return c != null && c > 0;
    }

    /** 软删后行仍在库但 delete_status='approved'（被前台读取过滤隐藏）。 */
    private boolean hidden(String id) {
        String status = jdbcTemplate.queryForObject(
                "SELECT delete_status FROM bar_products WHERE id = ?", String.class, id);
        return "approved".equals(status);
    }

    @Test
    void previewCountsOnlyArchivedOlderThanAndDeletesNothing() {
        seedProduct("prod_old", 100);    // 已存档 100 天前 → 命中
        seedProduct("prod_recent", 5);   // 已存档 5 天前 → 不命中（未超 30 天）
        seedProduct("prod_active", -1);  // 活数据 → 永不命中

        var job = service.preview(new DataCleanupPreviewRequest(SCOPE, 30));

        assertThat(job.status()).isEqualTo("dry_run");
        assertThat(job.previewCount()).isEqualTo(1);
        assertThat(job.previewSample()).extracting(m -> m.get("id")).containsExactly("prod_old");
        assertThat(job.deletedCount()).isNull();
        // dry-run 绝不删：三条都还在。
        assertThat(exists("prod_old")).isTrue();
        assertThat(exists("prod_recent")).isTrue();
        assertThat(exists("prod_active")).isTrue();
    }

    @Test
    void approveHidesOnlyArchivedOldRowsButKeepsThemInDb() {
        seedProduct("prod_del", 100);
        seedProduct("prod_keep_recent", 5);
        seedProduct("prod_keep_active", -1);

        TestSecurityActors.asPlatformAdmin("usr_requester");
        var dryRun = service.preview(new DataCleanupPreviewRequest(SCOPE, 30));
        var pending = service.submitForApproval(dryRun.id(), new DataCleanupSubmitRequest("清理测试期废弃商品"));
        assertThat(pending.status()).isEqualTo("pending_approval");

        // 换一个审批人执行。
        TestSecurityActors.asPlatformAdmin("usr_approver");
        var executed = service.approveAndExecute(pending.id(), new DataCleanupApprovalRequest("approved"));

        assertThat(executed.status()).isEqualTo("executed");
        assertThat(executed.deletedCount()).isEqualTo(1); // deleted_count 语义已改为隐藏条数
        assertThat(executed.approvedBy()).isEqualTo("usr_approver");
        // 软删：只隐藏了已存档超期的那条，行仍在库（可恢复），仅被前台读取过滤隐藏。
        assertThat(exists("prod_del")).isTrue();   // 行仍在数据库里（不物理删）
        assertThat(hidden("prod_del")).isTrue();   // 已置 delete_status='approved' → 前台隐藏
        // 近期存档与活数据既未删也未隐藏。
        assertThat(exists("prod_keep_recent")).isTrue();
        assertThat(hidden("prod_keep_recent")).isFalse();
        assertThat(exists("prod_keep_active")).isTrue();
        assertThat(hidden("prod_keep_active")).isFalse();
    }

    @Test
    void rejectDoesNotDelete() {
        seedProduct("prod_reject", 100);
        TestSecurityActors.asPlatformAdmin("usr_requester");
        var dryRun = service.preview(new DataCleanupPreviewRequest(SCOPE, 30));
        var pending = service.submitForApproval(dryRun.id(), new DataCleanupSubmitRequest("试探"));

        TestSecurityActors.asPlatformAdmin("usr_approver");
        var rejected = service.approveAndExecute(pending.id(), new DataCleanupApprovalRequest("rejected"));

        assertThat(rejected.status()).isEqualTo("rejected");
        assertThat(rejected.deletedCount()).isNull();
        assertThat(exists("prod_reject")).isTrue();
    }

    @Test
    void initiatorCannotApproveOwnRequest() {
        seedProduct("prod_self", 100);
        TestSecurityActors.asPlatformAdmin("usr_same");
        var dryRun = service.preview(new DataCleanupPreviewRequest(SCOPE, 30));
        var pending = service.submitForApproval(dryRun.id(), new DataCleanupSubmitRequest("自审试探"));

        // 同一账号审批自己的申请 → 禁自审。
        assertThatThrownBy(() -> service.approveAndExecute(pending.id(), new DataCleanupApprovalRequest("approved")))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "DATA_CLEANUP_SELF_APPROVE_FORBIDDEN");
        assertThat(exists("prod_self")).isTrue();
    }

    @Test
    void deleteSkipsArchivedRowsThatStillHaveActiveReferences() {
        // 已存档超期，但仍被套餐明细(bar_combo_items)引用 → 删除时跳过（诚实：不强删被引用行）。
        seedProduct("prod_referenced", 100);
        jdbcTemplate.update("""
                INSERT INTO bar_combos (id, bar_id, merchant_id, combo_name, price_cent, status, created_at, updated_at)
                VALUES ('combo_ref', 'bar_cleanup_test', 'merchant_cleanup_test', '引用套餐', 9900, 'active', now(), now())
                """);
        jdbcTemplate.update("""
                INSERT INTO bar_combo_items (id, combo_id, product_id, quantity, created_at)
                VALUES ('comboitem_ref', 'combo_ref', 'prod_referenced', 1, now())
                """);

        TestSecurityActors.asPlatformAdmin("usr_requester");
        var dryRun = service.preview(new DataCleanupPreviewRequest(SCOPE, 30));
        var pending = service.submitForApproval(dryRun.id(), new DataCleanupSubmitRequest("引用测试"));
        TestSecurityActors.asPlatformAdmin("usr_approver");
        var executed = service.approveAndExecute(pending.id(), new DataCleanupApprovalRequest("approved"));

        assertThat(executed.deletedCount()).isZero();
        assertThat(exists("prod_referenced")).isTrue();
        assertThat(hidden("prod_referenced")).isFalse(); // 有活引用被跳过，未隐藏
    }
}
