package com.complex.platform.voucher;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.voucher.dto.CustomerVoucherTransferRequest;
import com.complex.platform.voucher.dto.VoucherRedeemRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 操作员裁决第十三批第 9 条「和门票一致不冻结（推荐）：发起转赠后原持有人仍可自用或改转他人，先成功的动作为准，其余自动作废」的并发守卫：
 * 发起转赠、确认领取、按动态码核销、按券编号核销四处都先锁同一张券行再读（{@link VoucherRepository#lockInstance}）。
 * 本类【非事务】：各动作是真实服务、各自独立事务提交；先到者在事务里停住，必须在库里观测到后到者真的在锁上等待才放行。
 * 门票侧同一裁决见 TicketTransferConcurrencyJdbcIntegrationTests。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class VoucherTransferConcurrencyJdbcIntegrationTests {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private CustomerRepository customers;
    @Autowired private VoucherTransferService transfers;
    @Autowired private VoucherService vouchers;
    @Autowired private PlatformTransactionManager txManager;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    private final String holder = "cus_vtc_h_" + suffix;
    private final String receiver = "cus_vtc_r_" + suffix;
    private final String voucher = "vi_vtc_" + suffix;
    private final String staffId = "usr_vtc_" + suffix;
    private AccessActor cashier;
    private ExecutorService pool;

    @BeforeEach
    void seed() {
        Instant now = Instant.now();
        saveCustomer(holder, "138", now);
        saveCustomer(receiver, "139", now);
        String definition = "vdef_vtc_" + suffix;
        String merchant = "merchant_vtc_" + suffix;
        jdbc.update("INSERT INTO voucher_definitions (id, name, voucher_kind, redeem_method, settlement_mode, merchant_id, face_value_cent,"
                + " valid_days, status, transferable) VALUES (?, '并发转赠测试券', 'merchant_discount', 'in_store_staff', 'none', ?, 2000, 30,"
                + " 'active', TRUE)", definition, merchant);
        jdbc.update("INSERT INTO voucher_instances (id, definition_id, customer_id, code, source_type, source_ref, status, issued_at,"
                + " expires_at) VALUES (?, ?, ?, ?, 'marketing_grant', 'vgrant_vtc', 'issued', now(), now() + interval '30 days')",
                voucher, definition, holder, "VC" + voucher);
        // 券所属商户的核销店员：真库账号＋角色＋权限码（StaffRequestFixture 同款），核销走真实权限与商户范围校验。
        String role = "role_" + staffId;
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at)"
                + " VALUES (?, ?, '券转赠并发回归', 'project', 'active', now())", role, role);
        jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, 'customer.voucher.redeem')", role);
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'staff', NULL, ?, 'active', now())", staffId, staffId, merchant);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", staffId, role);
        cashier = new AccessActor(staffId, staffId, "staff", null, merchant, "active");
        pool = Executors.newFixedThreadPool(3);
    }

    @AfterEach
    void cleanup() throws InterruptedException {
        pool.shutdownNow();
        pool.awaitTermination(20, TimeUnit.SECONDS);
        jdbc.update("DELETE FROM voucher_transfers WHERE voucher_instance_id = ?", voucher);
        jdbc.update("DELETE FROM voucher_redeem_codes WHERE voucher_instance_id = ?", voucher);
        jdbc.update("DELETE FROM user_roles WHERE user_id = ?", staffId);
        jdbc.update("DELETE FROM role_permissions WHERE role_id = ?", "role_" + staffId);
        jdbc.update("DELETE FROM roles WHERE id = ?", "role_" + staffId);
        jdbc.update("DELETE FROM access_users WHERE id = ?", staffId);
    }

    /** ①（发起处的锁）领取已改归属、未提交时原持有人改转：等领取提交后按锁后的持有人重判，改转失效，不留下可被第三人领走的在途转赠。 */
    @Test
    void retransferWaitingBehindClaimRechecksHolderAfterLock() throws Exception {
        VoucherTransferResponse first = linkTransfer();
        CountDownLatch release = new CountDownLatch(1);
        Future<String> claim = holdClaim(first, release);
        Future<String> retransfer = run(null, this::linkTransfer);
        awaitLockWaiters(1);
        release.countDown();
        assertThat(claim.get(20, TimeUnit.SECONDS)).isEqualTo("ok");
        assertThat(retransfer.get(20, TimeUnit.SECONDS)).isEqualTo("VOUCHER_TRANSFER_NOT_HOLDER");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM voucher_transfers WHERE voucher_instance_id = ? AND status = 'pending'",
                Integer.class, voucher)).isZero();
    }

    /** ②（按动态码核销处的锁）领取已改归属、未提交时店员扫原持有人的码：等领取提交后码已不属于持有人，核销失效，券归受赠人仍可用。 */
    @Test
    void redeemByCodeWaitingBehindClaimSeesNewHolder() throws Exception {
        VoucherTransferResponse first = linkTransfer();
        String code = vouchers.issueCustomerRedeemCode(voucher, holder).code();
        CountDownLatch release = new CountDownLatch(1);
        Future<String> claim = holdClaim(first, release);
        Future<String> redeem = run(cashier, () -> vouchers.redeem(redeemRequest(code, null)));
        awaitLockWaiters(1);
        release.countDown();
        assertThat(claim.get(20, TimeUnit.SECONDS)).isEqualTo("ok");
        assertThat(redeem.get(20, TimeUnit.SECONDS)).isEqualTo("VOUCHER_REDEEM_CODE_EXPIRED");
        assertThat(jdbc.queryForMap("SELECT status, customer_id FROM voucher_instances WHERE id = ?", voucher))
                .containsEntry("status", "issued").containsEntry("customer_id", receiver);
    }

    /**
     * ③（领取处的锁）领取与核销同时到：领取先锁券行再动转赠行，与核销、改转同一加锁顺序。转赠行被别的事务短暂占住、领取排在前头时，
     * 核销进来也只能排在领取后面——只有一方成功、另一方得到业务提示，不出现两边互等被数据库判死锁。
     */
    @Test
    void claimAndRedeemTogetherExactlyOneWinsWithoutDeadlock() throws Exception {
        VoucherTransferResponse first = linkTransfer();
        String code = vouchers.issueCustomerRedeemCode(voucher, holder).code();
        CountDownLatch rowHeld = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<String> holdRow = run(null, () -> new TransactionTemplate(txManager).execute(status -> {
            jdbc.queryForList("SELECT id FROM voucher_transfers WHERE id = ? FOR UPDATE", String.class, first.id());
            rowHeld.countDown();
            await(release);
            return null;
        }));
        assertThat(rowHeld.await(20, TimeUnit.SECONDS)).isTrue();
        Future<String> claim = run(null, () -> transfers.claim(receiver, null, first.id(), first.claimCode()));
        awaitLockWaiters(1);
        Future<String> redeem = run(cashier, () -> vouchers.redeem(redeemRequest(code, null)));
        awaitLockWaiters(2);
        release.countDown();
        assertThat(holdRow.get(20, TimeUnit.SECONDS)).isEqualTo("ok");
        assertThat(claim.get(20, TimeUnit.SECONDS)).isEqualTo("ok");
        assertThat(redeem.get(20, TimeUnit.SECONDS)).isEqualTo("VOUCHER_REDEEM_CODE_EXPIRED");
        assertThat(jdbc.queryForMap("SELECT status, customer_id FROM voucher_instances WHERE id = ?", voucher))
                .containsEntry("status", "issued").containsEntry("customer_id", receiver);
    }

    /** ④（按券编号核销处的锁）领取已改归属、未提交时店员按券编号核销：等领取提交后再读，核销记到受赠人名下（先成功的领取为准）。 */
    @Test
    void redeemByInstanceIdWaitingBehindClaimRecordsNewHolder() throws Exception {
        VoucherTransferResponse first = linkTransfer();
        CountDownLatch release = new CountDownLatch(1);
        Future<String> claim = holdClaim(first, release);
        Future<String> redeem = run(cashier, () -> vouchers.redeem(redeemRequest(null, voucher)));
        awaitLockWaiters(1);
        release.countDown();
        assertThat(claim.get(20, TimeUnit.SECONDS)).isEqualTo("ok");
        assertThat(redeem.get(20, TimeUnit.SECONDS)).isEqualTo("ok");
        assertThat(jdbc.queryForObject("SELECT customer_id FROM voucher_redemption_records WHERE voucher_instance_id = ?",
                String.class, voucher)).isEqualTo(receiver);
    }

    // ---------- helpers ----------

    private VoucherTransferResponse linkTransfer() {
        return transfers.initiate(holder, null,
                new CustomerVoucherTransferRequest("vtc-" + UUID.randomUUID(), voucher, "link", null));
    }

    private VoucherRedeemRequest redeemRequest(String code, String instanceId) {
        return new VoucherRedeemRequest("vtc-redeem-" + UUID.randomUUID(), code, instanceId, null);
    }

    /** 受赠人领取完成（券已改归属、码已撤）但事务停住不提交，直到放行。 */
    private Future<String> holdClaim(VoucherTransferResponse transfer, CountDownLatch release) throws InterruptedException {
        CountDownLatch claimed = new CountDownLatch(1);
        Future<String> claim = run(null, () -> new TransactionTemplate(txManager).execute(status -> {
            transfers.claim(receiver, null, transfer.id(), transfer.claimCode());
            claimed.countDown();
            await(release);
            return null;
        }));
        assertThat(claimed.await(20, TimeUnit.SECONDS)).as("领取已在事务内完成").isTrue();
        return claim;
    }

    /** 结果：成功＝ok；业务拒绝＝错误码；其余（如数据库判死锁）＝异常类名。 */
    private Future<String> run(AccessActor actor, Supplier<?> action) {
        return pool.submit(() -> {
            if (actor != null) {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
            }
            try {
                action.get();
                return "ok";
            } catch (ApiException e) {
                return e.code();
            } catch (RuntimeException e) {
                return e.getClass().getSimpleName();
            } finally {
                SecurityContextHolder.clearContext();
            }
        });
    }

    /** 必须在库里实际观测到 n 个事务在行锁上等待，才算后到者已排上队。 */
    private void awaitLockWaiters(int n) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < deadline) {
            Integer waiting = jdbc.queryForObject("SELECT count(*) FROM pg_stat_activity WHERE datname = current_database()"
                    + " AND wait_event_type = 'Lock'", Integer.class);
            if (waiting != null && waiting >= n) {
                return;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
        }
        throw new AssertionError("未观测到 " + n + " 个事务在锁上等待");
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(20, TimeUnit.SECONDS)) {
                throw new AssertionError("未获放行");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private void saveCustomer(String id, String prefix, Instant now) {
        String phone = prefix + String.format("%08d", Math.floorMod((id).hashCode(), 100_000_000));
        customers.saveCustomer(new CustomerRecord(id, phone, prefix + "****" + phone.substring(7), "active", null, now, now, now, null));
    }
}
