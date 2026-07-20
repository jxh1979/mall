package com.complex.platform.voucher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.crypto.BlindIndex;
import com.complex.platform.common.crypto.PiiCipher;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.voucher.dto.CustomerVoucherTransferRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * V313 券转赠服务层编排纯单测（Mockito，不触库）：发起持有人/自赠拦截、领取渠道授权（领取码匹配 / 手机号盲索引）
 * + 券仍 issued 复验 + 归属改写守卫（updateInstanceCustomer 守卫 0 行则回滚）、撤回发起人授权。
 */
class VoucherTransferServiceTests {

    private static final Instant T0 = Instant.parse("2026-07-19T00:00:00Z");

    private VoucherTransferRepository transfers;
    private VoucherRepository vouchers;
    private BlindIndex blindIndex;
    private PiiCipher piiCipher;
    private VoucherTransferService service;

    @BeforeEach
    void setUp() {
        transfers = mock(VoucherTransferRepository.class);
        vouchers = mock(VoucherRepository.class);
        blindIndex = mock(BlindIndex.class);
        piiCipher = mock(PiiCipher.class);
        // 确定性盲索引/加密（便于自赠比对与受赠授权）。
        when(blindIndex.hash(anyString())).thenAnswer(inv -> "h:" + ((String) inv.getArgument(0)).trim());
        when(piiCipher.encrypt(anyString())).thenAnswer(inv -> "e:" + inv.getArgument(0));
        // 默认券种可转让（V313 开关默认 true）；toggle 用例改写为不可转让。
        when(vouchers.findDefinitionById(anyString())).thenReturn(Optional.of(def(true)));
        service = new VoucherTransferService(transfers, vouchers, blindIndex, piiCipher,
                mock(AuditEventService.class), null, Clock.fixed(T0, ZoneOffset.UTC));
    }

    private VoucherInstanceRecord issued(String id, String owner) {
        return new VoucherInstanceRecord(id, "vdef_1", owner, "VC" + id, "marketing_grant", "g",
                "issued", T0, T0.plusSeconds(86400), null, null, null, false, T0, T0);
    }

    private VoucherDefinitionRecord def(boolean transferable) {
        return new VoucherDefinitionRecord("vdef_1", "券", "merchant_discount", "in_store_staff", "none",
                "merchant-1", 5000L, null, null, null, null, null, null, "active", transferable, T0, T0);
    }

    private VoucherTransferRecord pendingPhone(String id, String instanceId, String initiator, String recvHash) {
        return new VoucherTransferRecord(id, instanceId, initiator, "phone",
                "e:138", recvHash, "138****8888", null, null, "pending", null, T0, null, null, null, T0);
    }

    private VoucherTransferRecord pendingLink(String id, String instanceId, String initiator, String code) {
        return new VoucherTransferRecord(id, instanceId, initiator, "link",
                null, null, null, code, null, "pending", null, T0, null, null, null, T0);
    }

    // ── 发起 ──────────────────────────────────────────────────────────────
    @Test
    void initiateRejectsNonHolder() {
        when(vouchers.findInstanceById("vi1")).thenReturn(Optional.of(issued("vi1", "someone_else")));
        assertThatThrownBy(() -> service.initiate("me", "13800000001",
                new CustomerVoucherTransferRequest("k", "vi1", "link", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VOUCHER_TRANSFER_NOT_HOLDER"));
        verify(transfers, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void initiateRejectsSelfTransferByPhoneHash() {
        when(vouchers.findInstanceById("vi1")).thenReturn(Optional.of(issued("vi1", "me")));
        // 受赠手机号盲索引 == 发起人手机号盲索引 → 不能转给自己。
        assertThatThrownBy(() -> service.initiate("me", "13800000001",
                new CustomerVoucherTransferRequest("k", "vi1", "phone", "13800000001")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VOUCHER_TRANSFER_TO_SELF"));
        verify(transfers, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void initiateRejectsNonIssuedVoucher() {
        VoucherInstanceRecord redeemed = new VoucherInstanceRecord("vi1", "vdef_1", "me", "VCvi1",
                "marketing_grant", "g", "redeemed", T0, null, T0, null, null, false, T0, T0);
        when(vouchers.findInstanceById("vi1")).thenReturn(Optional.of(redeemed));
        assertThatThrownBy(() -> service.initiate("me", "13800000001",
                new CustomerVoucherTransferRequest("k", "vi1", "link", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VOUCHER_NOT_TRANSFERABLE"));
    }

    @Test
    void initiatePhoneChannelSucceedsAndEncryptsReceiver() {
        // 券种默认可转让（setUp 桩）→ 照常发起成功。
        when(vouchers.findInstanceById("vi1")).thenReturn(Optional.of(issued("vi1", "me")));
        VoucherTransferResponse res = service.initiate("me", "13800000001",
                new CustomerVoucherTransferRequest("k", "vi1", "phone", "13900000002"));
        assertThat(res.status()).isEqualTo("pending");
        assertThat(res.channel()).isEqualTo("phone");
        verify(piiCipher).encrypt("13900000002");
        verify(transfers).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void initiateRejectedWhenVoucherKindNotTransferable() {
        // V313 券种级开关：该券种设为不可转让 → 拒绝转赠（持有人/issued 校验已过）。
        when(vouchers.findInstanceById("vi1")).thenReturn(Optional.of(issued("vi1", "me")));
        when(vouchers.findDefinitionById("vdef_1")).thenReturn(Optional.of(def(false)));
        assertThatThrownBy(() -> service.initiate("me", "13800000001",
                new CustomerVoucherTransferRequest("k", "vi1", "link", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VOUCHER_KIND_NOT_TRANSFERABLE"));
        verify(transfers, never()).save(org.mockito.ArgumentMatchers.any());
    }

    // ── 领取 ──────────────────────────────────────────────────────────────
    @Test
    void claimLinkRejectsWrongClaimCode() {
        when(transfers.findById("t1")).thenReturn(Optional.of(pendingLink("t1", "vi1", "from", "vclm_right")));
        assertThatThrownBy(() -> service.claim("to", null, "t1", "vclm_wrong"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VOUCHER_TRANSFER_CLAIM_CODE_MISMATCH"));
        verify(transfers, never()).claim(anyString(), anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void claimPhoneRejectsReceiverMismatch() {
        when(transfers.findById("t1")).thenReturn(Optional.of(pendingPhone("t1", "vi1", "from", "h:13900000002")));
        // 领取人手机号盲索引 != 受赠手机号盲索引 → 拒。
        assertThatThrownBy(() -> service.claim("to", "13800000009", "t1", null))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VOUCHER_TRANSFER_RECEIVER_MISMATCH"));
        verify(transfers, never()).claim(anyString(), anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void claimPhoneSuccessReassignsVoucherToClaimer() {
        when(transfers.findById("t1")).thenReturn(Optional.of(pendingPhone("t1", "vi1", "from", "h:13900000002")));
        when(vouchers.findInstanceById("vi1")).thenReturn(Optional.of(issued("vi1", "from")));
        when(transfers.claim(eq("t1"), eq("to"), org.mockito.ArgumentMatchers.any())).thenReturn(1);
        when(vouchers.updateInstanceCustomer(eq("vi1"), eq("to"), org.mockito.ArgumentMatchers.any())).thenReturn(1);
        service.claim("to", "13900000002", "t1", null);
        // 领取赢家：CAS 领取 + 券归属原子改写给受赠人。
        verify(transfers).claim(eq("t1"), eq("to"), org.mockito.ArgumentMatchers.any());
        verify(vouchers).updateInstanceCustomer(eq("vi1"), eq("to"), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void claimRolledBackWhenInstanceReassignGuardRejects() {
        // 领取 CAS 赢了，但券在两次校验间被核销 → updateInstanceCustomer 守卫返回 0 → 抛错回滚整个领取。
        when(transfers.findById("t1")).thenReturn(Optional.of(pendingLink("t1", "vi1", "from", "vclm_ok")));
        when(vouchers.findInstanceById("vi1")).thenReturn(Optional.of(issued("vi1", "from")));
        when(transfers.claim(eq("t1"), eq("to"), org.mockito.ArgumentMatchers.any())).thenReturn(1);
        when(vouchers.updateInstanceCustomer(eq("vi1"), eq("to"), org.mockito.ArgumentMatchers.any())).thenReturn(0);
        assertThatThrownBy(() -> service.claim("to", "13900000002", "t1", "vclm_ok"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VOUCHER_TRANSFER_VOUCHER_NOT_CLAIMABLE"));
    }

    // ── 撤回 ──────────────────────────────────────────────────────────────
    @Test
    void revokeRejectsNonInitiator() {
        when(transfers.findById("t1")).thenReturn(Optional.of(pendingLink("t1", "vi1", "from", "vclm")));
        assertThatThrownBy(() -> service.revoke("not_from", "t1"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VOUCHER_TRANSFER_NOT_INITIATOR"));
        verify(transfers, never()).revoke(anyString(), anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void revokeSucceedsForInitiator() {
        when(transfers.findById("t1")).thenReturn(Optional.of(pendingLink("t1", "vi1", "from", "vclm")));
        when(transfers.revoke(eq("t1"), eq("from"), org.mockito.ArgumentMatchers.any())).thenReturn(1);
        service.revoke("from", "t1");
        verify(transfers).revoke(eq("t1"), eq("from"), org.mockito.ArgumentMatchers.any());
    }
}
