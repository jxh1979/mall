package com.complex.platform.bar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.support.TestSecurityActors;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 酒吧门店主数据（V263）集成测试：回填(bar-main)、countReferences、存档→申请删除→审批删除生命周期、
 * 禁自审、有活引用拒删。@Transactional 回滚（每用例清理）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
@Transactional
class BarVenueJdbcIntegrationTests {

    @Autowired
    private JdbcBarVenueRepository repository;

    @Autowired
    private BarVenueService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    @Test
    void backfillSeedsBarMain() {
        // V263 回填：迁移已把 bar-main 落成一条 bars（平台视角可见）。
        assertThat(repository.find("bar-main")).isPresent()
                .get().extracting(BarVenueRecord::barName).isEqualTo("主酒吧");
    }

    @Test
    void createListArchiveUnarchive() {
        TestSecurityActors.asPlatformAdmin();
        BarVenueRecord created = service.create(new BarVenueRequest(
                null, "proj_venue_test", null, null, "测试酒吧", "BAR-T", 50000L, null));
        assertThat(created.id()).startsWith("bar_");
        assertThat(created.projectId()).isEqualTo("proj_venue_test");
        assertThat(created.minimumSpendCent()).isEqualTo(50000L);

        List<BarVenueRecord> active = service.list(false);
        assertThat(active).extracting(BarVenueRecord::id).contains(created.id());

        BarVenueRecord archived = service.archive(created.id(), true);
        assertThat(archived.archivedAt()).isNotNull();
        assertThat(service.list(false)).extracting(BarVenueRecord::id).doesNotContain(created.id());
        assertThat(service.list(true)).extracting(BarVenueRecord::id).contains(created.id());

        BarVenueRecord unarchived = service.archive(created.id(), false);
        assertThat(unarchived.archivedAt()).isNull();
    }

    @Test
    void deleteLifecycleSucceedsWhenNoReferences() {
        TestSecurityActors.asPlatformAdmin("usr_requester");
        BarVenueRecord bar = service.create(new BarVenueRequest(
                null, "proj_venue_del", null, null, "待删酒吧", null, null, null));
        service.archive(bar.id(), true);
        service.requestDelete(bar.id(), new BarCatalogDeleteRequest("门店关停"));
        assertThat(repository.find(bar.id())).get().extracting(BarVenueRecord::deleteStatus).isEqualTo("pending");

        // 禁自审：申请人不能审批自己的申请。
        TestSecurityActors.asPlatformAdmin("usr_requester");
        assertThatThrownBy(() -> service.approveDelete(bar.id(),
                new BarCatalogDeleteApprovalRequest("approved", null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot approve");

        // 换审批人批准 → 物理删除。
        TestSecurityActors.asPlatformAdmin("usr_approver");
        BarCatalogDeleteDecision decision = service.approveDelete(bar.id(),
                new BarCatalogDeleteApprovalRequest("approved", null));
        assertThat(decision.deleted()).isTrue();
        assertThat(repository.find(bar.id())).isEmpty();
    }

    @Test
    void approveDeleteRejectedWhenInUse() {
        TestSecurityActors.asPlatformAdmin("usr_requester");
        BarVenueRecord bar = service.create(new BarVenueRequest(
                null, "proj_venue_use", null, null, "在用酒吧", null, null, null));
        // 造一条以该 bar_id 引用的运营行（bar_tables），countReferences 应 >0。
        jdbcTemplate.update("""
                INSERT INTO bar_tables (
                    id, bar_id, table_code, table_name, table_type, capacity,
                    allowed_pass_free_guest_count, min_consumption_cent, currency, status,
                    created_at, updated_at)
                VALUES (?, ?, 'T-USE', '测试卡座', 'standard', 4, 0, 0, 'CNY', 'idle', now(), now())
                """, "bartbl_venue_use", bar.id());
        assertThat(repository.countReferences(bar.id())).isGreaterThan(0L);

        service.archive(bar.id(), true);
        service.requestDelete(bar.id(), new BarCatalogDeleteRequest("试删"));
        TestSecurityActors.asPlatformAdmin("usr_approver");
        assertThatThrownBy(() -> service.approveDelete(bar.id(),
                new BarCatalogDeleteApprovalRequest("approved", null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("still referenced");
        assertThat(repository.find(bar.id())).isPresent();
    }

    @Test
    void requestDeleteRequiresArchiveFirst() {
        TestSecurityActors.asPlatformAdmin();
        BarVenueRecord bar = service.create(new BarVenueRequest(
                null, "proj_venue_arc", null, null, "未存档酒吧", null, null, null));
        assertThatThrownBy(() -> service.requestDelete(bar.id(), new BarCatalogDeleteRequest("x")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("must be archived");
    }

    @Test
    void listFiltersByProjectScope() {
        // 平台视角建两项目酒吧；项目员工视角只见本项目。
        TestSecurityActors.asPlatformAdmin();
        BarVenueRecord a = service.create(new BarVenueRequest(null, "proj_scope_a", null, null, "A吧", null, null, null));
        BarVenueRecord b = service.create(new BarVenueRequest(null, "proj_scope_b", null, null, "B吧", null, null, null));
        Optional<String> scopeA = Optional.of("proj_scope_a");
        List<BarVenueRecord> onlyA = repository.list(scopeA, false);
        assertThat(onlyA).extracting(BarVenueRecord::id).contains(a.id()).doesNotContain(b.id());
    }
}
