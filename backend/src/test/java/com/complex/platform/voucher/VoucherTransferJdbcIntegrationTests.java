package com.complex.platform.voucher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * V313 券转赠 JDBC 集成测试（按约定交付会话不运行，待 DB 窗口串行跑）：
 * 表/索引/GRANT 落地、pending 部分唯一索引物理保证双花窗口（同券最多一笔在途）、
 * 领取/撤回 CAS 单赢家、按券实例作废在途、受赠盲索引待领取列表。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class VoucherTransferJdbcIntegrationTests {

    private static final Instant T0 = Instant.parse("2026-07-19T00:00:00Z");

    @Autowired
    private JdbcVoucherTransferRepository transfers;

    @Autowired
    private JdbcVoucherRepository vouchers;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String seedIssuedInstance(String suffix) {
        String defId = "vdef_vt_" + suffix;
        vouchers.insertDefinition(new VoucherDefinitionRecord(
                defId, "转赠折扣券", "merchant_discount", "in_store_staff", "none", "merchant_vt",
                5000L, null, null, null, null, 30, null, "active", true, T0, T0));
        String instanceId = "vi_vt_" + suffix;
        vouchers.insertInstance(new VoucherInstanceRecord(
                instanceId, defId, "cust_from_" + suffix, "VCVT" + suffix, "marketing_grant", "vgrant_vt",
                "issued", T0, T0.plusSeconds(86400L * 30), null, null, null, false, T0, T0));
        return instanceId;
    }

    private VoucherTransferRecord pending(String id, String instanceId, String initiator, String claimCode) {
        return new VoucherTransferRecord(id, instanceId, initiator, "link",
                null, null, null, claimCode, null, "pending", null, T0, null, null, null, T0);
    }

    @Test
    void migrationCreatesTableAndGrantsForComplexApp() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT to_regclass('voucher_transfers')::text", String.class)).isEqualTo("voucher_transfers");
        Boolean roleExists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app')", Boolean.class);
        if (!Boolean.TRUE.equals(roleExists)) {
            return;
        }
        for (String privilege : List.of("SELECT", "INSERT", "UPDATE")) {
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT has_table_privilege('complex_app', 'voucher_transfers', '" + privilege + "')", Boolean.class))
                    .as("%s on voucher_transfers", privilege).isTrue();
        }
    }

    @Test
    void pendingPartialUniqueIndexBlocksSecondInFlightPerInstance() {
        String instanceId = seedIssuedInstance("dbl");
        transfers.save(pending("vt_dbl_1", instanceId, "cust_from_dbl", "vclm_a"));
        // 同券第二笔 pending（不同 id）撞 uq_voucher_transfers_pending → 冲突。
        assertThatThrownBy(() -> transfers.save(pending("vt_dbl_2", instanceId, "cust_from_dbl", "vclm_b")))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void claimCasIsSingleWinnerAndRevokeReleasesFreeze() {
        String instanceId = seedIssuedInstance("cas");
        transfers.save(pending("vt_cas_1", instanceId, "cust_from_cas", "vclm_c"));
        assertThat(transfers.existsPendingForInstance(instanceId)).isTrue();
        // 领取 CAS 单赢家：第一次 1 行，第二次 0 行。
        assertThat(transfers.claim("vt_cas_1", "cust_to_cas", T0.plusSeconds(60))).isEqualTo(1);
        assertThat(transfers.claim("vt_cas_1", "cust_to_cas", T0.plusSeconds(61))).isZero();
        assertThat(transfers.existsPendingForInstance(instanceId)).isFalse();
        // 领取后 freeze 释放：同券可再建一笔 pending（转给下一个人）。
        transfers.save(pending("vt_cas_2", instanceId, "cust_to_cas", "vclm_d"));
        // 撤回 CAS：pending->revoked，freeze 再释放。
        assertThat(transfers.revoke("vt_cas_2", "cust_to_cas", T0.plusSeconds(120))).isEqualTo(1);
        assertThat(transfers.existsPendingForInstance(instanceId)).isFalse();
    }

    @Test
    void voidPendingForInstanceAndIncomingByReceiverHash() {
        String instanceId = seedIssuedInstance("void");
        transfers.save(new VoucherTransferRecord("vt_void_1", instanceId, "cust_from_void", "phone",
                // V396：receiver_phone 列有 enc:v1: 信封 CHECK，夹具密文也须为信封形态。
                "enc:v1:phone-fixture", "hash_recv_void", "138****8888", null, null, "pending", null, T0, null, null, null, T0));
        assertThat(transfers.listPendingByReceiverPhoneHash("hash_recv_void"))
                .extracting(VoucherTransferRecord::id).containsExactly("vt_void_1");
        // 券核销/作废/过期时按券实例作废在途转赠。
        assertThat(transfers.voidPendingForInstance(instanceId, "voucher_revoked", T0.plusSeconds(200))).isEqualTo(1);
        assertThat(transfers.existsPendingForInstance(instanceId)).isFalse();
        assertThat(transfers.listPendingByReceiverPhoneHash("hash_recv_void")).isEmpty();
    }
}
