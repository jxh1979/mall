package com.complex.platform.voucher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.PageResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * V301 统一券台账 JDBC 集成测试（本迁移交付时按约定未在交付会话运行，待 DB 窗口跑）：
 * 表/索引/权限种子/GRANT 落地、append-only 触发器拦截 UPDATE/DELETE、状态 CHECK、
 * 端到端（积分兑券形态的实例落台账→统一核销→结算挂钩字段落核销记录）、实例级唯一双花闸、
 * "全部券"聚合 UNION 可执行。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class VoucherJdbcIntegrationTests {

    private static final Instant T0 = Instant.parse("2026-07-18T00:00:00Z");

    @Autowired
    private JdbcVoucherRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static VoucherDefinitionRecord goodsDefinition(String id, String merchantId, Long internalPriceCent) {
        return new VoucherDefinitionRecord(
                id, "JDBC 实物券", "in_store_goods", "in_store_staff",
                internalPriceCent == null ? "none" : "internal_price", merchantId,
                null, "item_jdbc", "loc_jdbc", 1, internalPriceCent, 30, null, "active", T0, T0);
    }

    private static VoucherInstanceRecord issuedInstance(String id, String definitionId, String sourceType, String sourceRef) {
        return new VoucherInstanceRecord(
                id, definitionId, "customer_jdbc", "VC" + id, sourceType, sourceRef,
                "issued", T0, T0.plusSeconds(86400L * 30), null, null, null, false, T0, T0);
    }

    @Test
    void migrationCreatesTablesCatalogColumnAndSeedsPermissions() {
        for (String table : List.of("voucher_definitions", "voucher_instances", "voucher_redemption_records")) {
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT to_regclass('" + table + "')::text", String.class)).isEqualTo(table);
        }
        // 兑换目录挂券模板列已加。
        Integer catalogColumn = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                 WHERE table_name = 'customer_redemption_catalog_items' AND column_name = 'voucher_definition_id'
                """, Integer.class);
        assertThat(catalogColumn).isEqualTo(1);
        // 权限种子＋平台管理员直授。
        List<String> permissions = jdbcTemplate.queryForList(
                "SELECT permission_code FROM permissions WHERE permission_code LIKE 'customer.voucher.%'", String.class);
        assertThat(permissions).contains("customer.voucher.view", "customer.voucher.manage", "customer.voucher.redeem");
        List<String> adminGrants = jdbcTemplate.queryForList(
                "SELECT permission_code FROM role_permissions WHERE role_id = 'role_platform_admin' "
                        + "AND permission_code LIKE 'customer.voucher.%'", String.class);
        assertThat(adminGrants).contains("customer.voucher.view", "customer.voucher.manage", "customer.voucher.redeem");
    }

    @Test
    void runtimeGrantsCoverEveryNewTableForComplexApp() {
        // 上轮 V296/V297 漏 GRANT 直接生产 500 的硬性检查项：complex_app 对三张新表须有运行时权限；
        // 核销记录与 V140 immutable 口径一致——只授 SELECT/INSERT，UPDATE/DELETE 被 REVOKE。
        Boolean roleExists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app')", Boolean.class);
        if (!Boolean.TRUE.equals(roleExists)) {
            return; // 本地无受限 role 的库：GRANT DO 块按设计跳过。
        }
        for (String table : List.of("voucher_definitions", "voucher_instances")) {
            for (String privilege : List.of("SELECT", "INSERT", "UPDATE", "DELETE")) {
                assertThat(jdbcTemplate.queryForObject(
                        "SELECT has_table_privilege('complex_app', '" + table + "', '" + privilege + "')", Boolean.class))
                        .as("%s on %s", privilege, table)
                        .isTrue();
            }
        }
        for (String privilege : List.of("SELECT", "INSERT")) {
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT has_table_privilege('complex_app', 'voucher_redemption_records', '" + privilege + "')",
                    Boolean.class)).isTrue();
        }
        for (String privilege : List.of("UPDATE", "DELETE")) {
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT has_table_privilege('complex_app', 'voucher_redemption_records', '" + privilege + "')",
                    Boolean.class)).as("%s must be revoked", privilege).isFalse();
        }
    }

    // UPDATE 与 DELETE 拆两个用例：触发器 RAISE 后整事务 abort（V297 测试同款约束）。

    @Test
    void appendOnlyTriggerBlocksUpdateOnRedemptionRecords() {
        seedRedeemedVoucher("vi_jdbc_ao_u");
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE voucher_redemption_records SET settlement_amount_cent = 999 WHERE voucher_instance_id = 'vi_jdbc_ao_u'"))
                .hasMessageContaining("COMPLEX_APPEND_ONLY_VIOLATION");
    }

    @Test
    void appendOnlyTriggerBlocksDeleteOnRedemptionRecords() {
        seedRedeemedVoucher("vi_jdbc_ao_d");
        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM voucher_redemption_records WHERE voucher_instance_id = 'vi_jdbc_ao_d'"))
                .hasMessageContaining("COMPLEX_APPEND_ONLY_VIOLATION");
    }

    @Test
    void instanceStatusCheckRejectsUnknownStatus() {
        repository.insertDefinition(goodsDefinition("vdef_jdbc_chk", "merchant_jdbc", null));
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO voucher_instances (id, definition_id, customer_id, code, source_type, status, issued_at)
                VALUES ('vi_jdbc_bad', 'vdef_jdbc_chk', 'customer_jdbc', 'VCBAD', 'marketing_grant', 'bogus', now())
                """))
                .hasMessageContaining("chk_vi_status");
    }

    @Test
    void endToEndPointsVoucherRedeemAndSettlementHook() {
        // 端到端（仓储层）：积分兑券形态的实例落台账 → CAS 核销 → 结算挂钩字段落核销记录。
        repository.insertDefinition(goodsDefinition("vdef_jdbc_e2e", "merchant_jdbc", 1500L));
        assertThat(repository.insertInstance(
                issuedInstance("vi_jdbc_e2e", "vdef_jdbc_e2e", "points_redemption", "redemption_jdbc_1"))).isTrue();
        // 实例 id 幂等（redeemPoints 重放派生同 id 不重复发券）。
        assertThat(repository.insertInstance(
                issuedInstance("vi_jdbc_e2e", "vdef_jdbc_e2e", "points_redemption", "redemption_jdbc_1"))).isFalse();

        // CAS 占位：issued→redeemed 恰好一次。
        Instant redeemedAt = T0.plusSeconds(3600);
        assertThat(repository.updateInstanceStatus("vi_jdbc_e2e", "issued", "redeemed", redeemedAt, null)).isEqualTo(1);
        assertThat(repository.updateInstanceStatus("vi_jdbc_e2e", "issued", "redeemed", redeemedAt, null)).isZero();

        // 核销记录带结算挂钩（内部结算价＋应付 id＋出库引用）。
        assertThat(repository.insertRedemption(new VoucherRedemptionEntryRecord(
                "vrr_vi_jdbc_e2e", "vi_jdbc_e2e", "vdef_jdbc_e2e", "in_store_goods", "customer_jdbc",
                "merchant_jdbc", "usr_jdbc", "in_store_staff", "internal_price", 1500L,
                "arap_jdbc_1", "voucher-redeem-vi_jdbc_e2e", null, "JDBC 端到端", redeemedAt, redeemedAt))).isTrue();

        PageResponse<VoucherRedemptionEntryRecord> redemptions =
                repository.listRedemptions("merchant_jdbc", "vi_jdbc_e2e", 1, 50);
        assertThat(redemptions.items()).singleElement().satisfies(entry -> {
            assertThat(entry.settlementAmountCent()).isEqualTo(1500L);
            assertThat(entry.arApItemId()).isEqualTo("arap_jdbc_1");
            assertThat(entry.stockMovementRef()).isEqualTo("voucher-redeem-vi_jdbc_e2e");
        });
        assertThat(repository.findInstanceById("vi_jdbc_e2e").orElseThrow().status()).isEqualTo("redeemed");
    }

    @Test
    void redemptionUniqueIndexBlocksDoubleSpendPerInstance() {
        seedRedeemedVoucher("vi_jdbc_dbl");
        // 第二条核销记录（不同 id、同实例）：实例级唯一索引 ON CONFLICT DO NOTHING → false，不产生第二行。
        assertThat(repository.insertRedemption(new VoucherRedemptionEntryRecord(
                "vrr_vi_jdbc_dbl_2", "vi_jdbc_dbl", "vdef_jdbc_dbl", "in_store_goods", "customer_jdbc",
                "merchant_jdbc", "usr_jdbc_2", "in_store_staff", "none", null,
                null, null, null, "双花重放", T0.plusSeconds(7200), T0.plusSeconds(7200)))).isFalse();
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM voucher_redemption_records WHERE voucher_instance_id = 'vi_jdbc_dbl'",
                Integer.class);
        assertThat(rows).isEqualTo(1);
    }

    @Test
    void overviewUnionQueryExecutesAndCarriesUnifiedRows() {
        repository.insertDefinition(goodsDefinition("vdef_jdbc_ov", "merchant_jdbc", null));
        repository.insertInstance(issuedInstance("vi_jdbc_ov", "vdef_jdbc_ov", "marketing_grant", "vgrant_jdbc"));
        // UNION 两源均可执行（partner 源无 fixture 时为空即可），统一台账行在列。
        PageResponse<VoucherOverviewRecord> page = repository.overview(null, "customer_jdbc", 1, 50);
        assertThat(page.items()).anySatisfy(row -> {
            assertThat(row.id()).isEqualTo("vi_jdbc_ov");
            assertThat(row.source()).isEqualTo("unified");
            assertThat(row.remainingTimes()).isEqualTo(1);
        });
    }

    @Test
    void customerWalletReadReturnsOwnRowsWithFaceValueAndNeverInternalPrice() {
        // 面额券（merchant_discount 带面额）+ 另一客人的券；客人端读法＝merchantScope=null + 强制本人 customer_id。
        repository.insertDefinition(new VoucherDefinitionRecord(
                "vdef_jdbc_cw", "JDBC 折扣券", "merchant_discount", "in_store_staff", "none", "merchant_jdbc",
                5000L, null, null, null, null, 30, null, "active", T0, T0));
        repository.insertInstance(issuedInstance("vi_jdbc_cw", "vdef_jdbc_cw", "marketing_grant", "vgrant_cw"));
        repository.insertInstance(new VoucherInstanceRecord(
                "vi_jdbc_cw_other", "vdef_jdbc_cw", "customer_other", "VCother", "marketing_grant", "vgrant_other",
                "issued", T0, T0.plusSeconds(86400L), null, null, null, false, T0, T0));

        PageResponse<VoucherOverviewRecord> mine = repository.overview(null, "customer_jdbc", 1, 50);

        // 只见本人券，看不到 customer_other 的券（不能查他人）。
        assertThat(mine.items()).extracting(VoucherOverviewRecord::customerId).containsOnly("customer_jdbc");
        assertThat(mine.items()).extracting(VoucherOverviewRecord::id)
                .contains("vi_jdbc_cw").doesNotContain("vi_jdbc_cw_other");
        // 面额/核销方式随视图返回，供券卡展示。
        assertThat(mine.items()).filteredOn(r -> "vi_jdbc_cw".equals(r.id())).singleElement().satisfies(r -> {
            assertThat(r.faceValueCent()).isEqualTo(5000L);
            assertThat(r.redeemMethod()).isEqualTo("in_store_staff");
        });
        // 结构性铁证：聚合视图 record 无内部结算价/成本字段，客人天然不可见。
        assertThat(java.util.Arrays.stream(VoucherOverviewRecord.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName))
                .doesNotContain("internalPriceCent", "settlementAmountCent");
    }

    private void seedRedeemedVoucher(String instanceId) {
        String definitionId = "vdef_" + instanceId.replace("vi_", "");
        repository.insertDefinition(goodsDefinition(definitionId, "merchant_jdbc", null));
        repository.insertInstance(issuedInstance(instanceId, definitionId, "marketing_grant", "vgrant_seed"));
        repository.updateInstanceStatus(instanceId, "issued", "redeemed", T0.plusSeconds(60), null);
        repository.insertRedemption(new VoucherRedemptionEntryRecord(
                "vrr_" + instanceId, instanceId, definitionId, "in_store_goods", "customer_jdbc",
                "merchant_jdbc", "usr_jdbc", "in_store_staff", "none", null,
                null, null, null, null, T0.plusSeconds(60), T0.plusSeconds(60)));
    }
}
