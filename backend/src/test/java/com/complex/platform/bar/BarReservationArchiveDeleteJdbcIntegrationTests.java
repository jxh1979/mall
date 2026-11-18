package com.complex.platform.bar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** V264 卡座预订存档与审批后删除集成测试。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class BarReservationArchiveDeleteJdbcIntegrationTests {

    private static final String MERCHANT = "merchant_resv_test";
    private static final String BAR = "bar_resv_test";
    private static final String TABLE = "tbl_resv_test";

    @Autowired
    private BarService service;
    @Autowired
    private BarRepository repository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    private void seedTable() {
        Instant now = Instant.now();
        jdbcTemplate.update("""
                INSERT INTO bar_tables (id, bar_id, table_code, table_name, table_type, capacity, status,
                    created_at, updated_at)
                VALUES (?, ?, 'C1', '卡座1', 'booth', 6, 'available', ?, ?)
                ON CONFLICT (id) DO NOTHING
                """, TABLE, BAR, java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));
    }

    /**
     * 已取消的订台（第十八批 AU 段收尾复核：仍在预留中的订台不能删，BSM:572「只要还有占用就拦住不让删」、ENUM:100「已预留只能经取消订台、超时释放、
     * 到店开台三条路径退出」，删除用例的订台须已退出预留）。
     */
    private BarReservationRecord seedReservation(String id) {
        seedTable();
        Instant now = Instant.now();
        return repository.saveReservation(new BarReservationRecord(
                id, BAR, MERCHANT, TABLE, null, now, 4, 0, 0L, "not_required", "staff", null,
                "cancelled", "web", null, List.of(), now, now, null));
    }

    @Test
    void archiveThenApproveDeleteSucceedsWhenNoReferences() {
        TestSecurityActors.asPlatformAdmin();
        seedReservation("barresv_del");
        service.archiveReservation("barresv_del", true, "测试存档理由");
        service.requestDeleteReservation("barresv_del", new BarCatalogDeleteRequest("取消"));
        // 第十八批 AU 段第 265 条 265-R1（BSM:577「删除一律须由具备最高权限的人审批通过后执行」）：审批人为最高权限本人。
        TestSecurityActors.asMasterAuthority();
        BarCatalogDeleteDecision decision = service.approveDeleteReservation(
                "barresv_del", new BarCatalogDeleteApprovalRequest("approved", null));
        assertThat(decision.deleted()).isTrue();
        assertThat(repository.findReservationById("barresv_del")).isEmpty();
    }

    @Test
    void requestDeleteRequiresArchiveFirst() {
        TestSecurityActors.asPlatformAdmin();
        seedReservation("barresv_noarch");
        assertThatThrownBy(() -> service.requestDeleteReservation("barresv_noarch", new BarCatalogDeleteRequest("x")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("存档");
    }

    @Test
    void approveDeleteRejectedWhenReferencedByOrder() {
        TestSecurityActors.asPlatformAdmin();
        seedReservation("barresv_use");
        service.archiveReservation("barresv_use", true, "测试存档理由");
        service.requestDeleteReservation("barresv_use", new BarCatalogDeleteRequest("试删"));
        // 挂一个订单形成活引用（bar_orders.reservation_id）。
        Instant now = Instant.now();
        jdbcTemplate.update("""
                INSERT INTO bar_orders (id, bar_id, table_id, reservation_id, order_source, status,
                    total_amount_cent, created_at, updated_at)
                VALUES ('barorder_use', ?, ?, 'barresv_use', 'staff', 'open', 0, ?, ?)
                """, BAR, TABLE, java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));
        TestSecurityActors.asMasterAuthority();
        assertThatThrownBy(() -> service.approveDeleteReservation("barresv_use",
                new BarCatalogDeleteApprovalRequest("approved", null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("引用");
    }

    /**
     * 第十八批 AU 段第 265 条 265-R1：平台管理员审批自己的申请，先被「只认最高权限」拦下（最高权限本人申请即当场删除，构造不出最高权限自审；
     * 禁止自审代码保留）。
     */
    @Test
    void selfApproveForbidden() {
        TestSecurityActors.asPlatformAdmin("usr_same");
        seedReservation("barresv_self");
        service.archiveReservation("barresv_self", true, "测试存档理由");
        service.requestDeleteReservation("barresv_self", new BarCatalogDeleteRequest("x"));
        assertThatThrownBy(() -> service.approveDeleteReservation("barresv_self",
                new BarCatalogDeleteApprovalRequest("approved", null)))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "MASTER_APPROVAL_REQUIRED");
    }

    /** 第十八批 AU 段第 265 条 265-R1（BSM:577「删除一律须由具备最高权限的人审批通过后执行，不设例外」）：平台管理员审批他人申请也 403，仍待审批。 */
    @Test
    void platformAdminCannotApproveOthersDeleteRequest() {
        TestSecurityActors.asPlatformAdmin("usr_initiator");
        seedReservation("barresv_admin");
        service.archiveReservation("barresv_admin", true, "测试存档理由");
        service.requestDeleteReservation("barresv_admin", new BarCatalogDeleteRequest("x"));
        TestSecurityActors.asPlatformAdmin("usr_approver");
        assertThatThrownBy(() -> service.approveDeleteReservation("barresv_admin",
                new BarCatalogDeleteApprovalRequest("approved", null)))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "MASTER_APPROVAL_REQUIRED");
        assertThat(repository.findReservationById("barresv_admin")).get()
                .extracting(BarReservationRecord::deleteStatus).isEqualTo("pending");
    }
}
