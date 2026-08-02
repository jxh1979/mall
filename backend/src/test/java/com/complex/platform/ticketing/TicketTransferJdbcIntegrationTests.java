package com.complex.platform.ticketing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.crypto.BlindIndex;
import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.dto.CustomerTicketTransferRequest;
import java.time.Instant;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * AUD-077 门票转赠状态机集成测试(真 JDBC/CAS/盲索引)。@Transactional 逐用例回滚隔离。
 * 并发竞态另见 TicketTransferConcurrencyJdbcIntegrationTests(非事务,双线程)。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class TicketTransferJdbcIntegrationTests {

    @Autowired private JdbcTicketingRepository repository;
    @Autowired private TicketTransferService transferService;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private BlindIndex blindIndex;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private TicketVerificationService verificationService;
    @Autowired private TicketingAmusementEntitlementGateway amusementGateway;
    // AUD-006 幂等经由 TicketingService 包装层(executeWrite)生效,直调 transferService 不带幂等。
    @Autowired private TicketingService ticketingService;

    private final Instant now = Instant.now();

    // ── 发起 ─────────────────────────────────────────────────────────────────
    @Test
    void initiateLinkSuccessFreezesSlot() {
        Fixture f = seedSinglePerson();
        TicketTransferResponse r = transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, null));
        assertThat(r.status()).isEqualTo("pending");
        assertThat(r.claimCode()).isNotBlank();
        assertThat(repository.existsPendingTransferForSlot(f.parent, 1)).isTrue();
        assertThat(repository.existsPendingTransferForParent(f.parent)).isTrue();
    }

    @Test
    void initiatePhoneSuccessStoresHashAndMask() {
        Fixture f = seedSinglePerson();
        TicketTransferResponse r = transferService.initiate(f.buyer, f.buyerPhone, phone(f.parent, null, f.receiverPhone));
        assertThat(r.channel()).isEqualTo("phone");
        assertThat(r.claimCode()).isNull();
        assertThat(r.receiverPhoneMasked()).contains("****");
        TicketTransferRecord row = repository.findTicketTransferById(r.id()).orElseThrow();
        assertThat(row.receiverPhoneHash()).isEqualTo(blindIndex.hash(f.receiverPhone));
        assertThat(row.receiverPhone()).isNotEqualTo(f.receiverPhone); // 密文,非明文
    }

    @Test
    void initiateRejectsNonHolder() {
        Fixture f = seedSinglePerson();
        assertApiCode(() -> transferService.initiate(f.receiver, f.receiverPhone, link(f.parent, null)),
                "TICKET_TRANSFER_NOT_HOLDER");
    }

    @Test
    void initiateRejectsUsedSlot() {
        Fixture f = seedSinglePerson();
        jdbcTemplate.update("UPDATE issued_entitlements SET used_times = 1 WHERE parent_ticket_id = ? AND slot_index = 1", f.parent);
        assertApiCode(() -> transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, null)),
                "TICKET_TRANSFER_SLOT_USED"); // 覆盖:核销过整票不可转 + 多次票首核后不可转
    }

    @Test
    void initiateRejectsToSelf() {
        Fixture f = seedSinglePerson();
        assertApiCode(() -> transferService.initiate(f.buyer, f.buyerPhone, phone(f.parent, null, f.buyerPhone)),
                "TICKET_TRANSFER_TO_SELF");
    }

    @Test
    void initiateRejectsDuplicatePending() {
        Fixture f = seedSinglePerson();
        transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, null));
        assertApiCode(() -> transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, null)),
                "TICKET_TRANSFER_ALREADY_PENDING");
    }

    @Test
    void initiateRejectsNonActiveParent() {
        Fixture f = seedSinglePerson();
        jdbcTemplate.update("UPDATE ticket_instances SET status = 'forfeited' WHERE id = ?", f.parent);
        assertApiCode(() -> transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, null)),
                "TICKET_NOT_TRANSFERABLE");
    }

    @Test
    void initiateMultiPersonRequiresSlot() {
        Fixture f = seedMultiPerson(2);
        assertApiCode(() -> transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, null)),
                "TICKET_TRANSFER_SLOT_REQUIRED");
    }

    // ── 领取 ─────────────────────────────────────────────────────────────────
    @Test
    void claimLinkWrongCodeRejectedRightCodeFlipsHolder() {
        Fixture f = seedSinglePerson();
        TicketTransferResponse r = transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, null));
        assertApiCode(() -> transferService.claim(f.receiver, f.receiverPhone, r.id(), "clm_wrong"),
                "TICKET_TRANSFER_CLAIM_CODE_MISMATCH");
        transferService.claim(f.receiver, f.receiverPhone, r.id(), r.claimCode());
        assertThat(holderOf(f.parent, 1)).isEqualTo(f.receiver);
        assertThat(repository.existsPendingTransferForSlot(f.parent, 1)).isFalse();
    }

    @Test
    void claimPhoneOnlyDesignatedReceiver() {
        Fixture f = seedSinglePerson();
        TicketTransferResponse r = transferService.initiate(f.buyer, f.buyerPhone, phone(f.parent, null, f.receiverPhone));
        // 非指定手机号(third)领取被拒
        assertApiCode(() -> transferService.claim(f.third, f.thirdPhone, r.id(), null),
                "TICKET_TRANSFER_RECEIVER_MISMATCH");
        // 指定手机号领取成功
        transferService.claim(f.receiver, f.receiverPhone, r.id(), null);
        assertThat(holderOf(f.parent, 1)).isEqualTo(f.receiver);
    }

    @Test
    void claimIsIdempotentForSameReceiver() {
        Fixture f = seedSinglePerson();
        TicketTransferResponse r = transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, null));
        transferService.claim(f.receiver, f.receiverPhone, r.id(), r.claimCode());
        TicketTransferResponse again = transferService.claim(f.receiver, f.receiverPhone, r.id(), r.claimCode());
        assertThat(again.status()).isEqualTo("claimed");
        assertThat(holderOf(f.parent, 1)).isEqualTo(f.receiver);
    }

    @Test
    void previewDoesNotGrant() {
        Fixture f = seedSinglePerson();
        TicketTransferResponse r = transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, null));
        TicketTransferPreviewResponse p = transferService.preview(r.claimCode());
        assertThat(p.status()).isEqualTo("pending");
        assertThat(holderOf(f.parent, 1)).isEqualTo(f.buyer); // 仅预览未领取,持有人不变
    }

    // ── 撤回 ─────────────────────────────────────────────────────────────────
    @Test
    void revokeBeforeClaimUnfreezesHolderUnchanged() {
        Fixture f = seedSinglePerson();
        TicketTransferResponse r = transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, null));
        transferService.revoke(f.buyer, r.id());
        assertThat(repository.existsPendingTransferForSlot(f.parent, 1)).isFalse();
        assertThat(holderOf(f.parent, 1)).isEqualTo(f.buyer);
        // 撤回后可重新发起
        assertThat(transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, null)).status()).isEqualTo("pending");
    }

    @Test
    void revokeAfterClaimedRejected() {
        Fixture f = seedSinglePerson();
        TicketTransferResponse r = transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, null));
        transferService.claim(f.receiver, f.receiverPhone, r.id(), r.claimCode());
        assertApiCode(() -> transferService.revoke(f.buyer, r.id()), "TICKET_TRANSFER_ALREADY_CLAIMED");
    }

    @Test
    void revokeRejectsNonInitiator() {
        Fixture f = seedSinglePerson();
        TicketTransferResponse r = transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, null));
        assertApiCode(() -> transferService.revoke(f.receiver, r.id()), "TICKET_TRANSFER_NOT_INITIATOR");
    }

    // ── 取码授权跟随持有人(P0 越权回归) ──────────────────────────────────────────
    @Test
    void qrCredentialAuthorizationFollowsHolder() {
        Fixture f = seedSinglePerson();
        String entId = firstEntitlementId(f.parent, 1);
        // 转赠前:购票人可取码
        assertThat(verificationService.refreshCredential(entId, f.buyer)).isNotNull();
        TicketTransferResponse r = transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, null));
        transferService.claim(f.receiver, f.receiverPhone, r.id(), r.claimCode());
        // 转出后:原持票人取码 403,受赠人可取
        assertApiCode(() -> verificationService.refreshCredential(entId, f.buyer), "CUSTOMER_ORDER_FORBIDDEN");
        assertThat(verificationService.refreshCredential(entId, f.receiver)).isNotNull();
    }

    // ── 冻结:等待期间不可核销 ────────────────────────────────────────────────
    @Test
    void pendingTransferBlocksConsumption() {
        Fixture f = seedSinglePerson();
        String entId = firstEntitlementId(f.parent, 1);
        TicketTransferResponse r = transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, null));
        assertThat(repository.atomicUseEntitlement(entId, now)).isEmpty(); // 冻结:所有消费路径 NOT EXISTS 拦截
        transferService.claim(f.receiver, f.receiverPhone, r.id(), r.claimCode());
        assertThat(repository.atomicUseEntitlement(entId, now)).isPresent(); // 领取后归受赠人可核销
    }

    // ── 多人票:只冻结在转 slot,其他 slot 不受影响 ──────────────────────────────
    @Test
    void multiPersonFreezesOnlyTransferredSlot() {
        Fixture f = seedMultiPerson(2);
        transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, 1));
        assertThat(repository.existsPendingTransferForSlot(f.parent, 1)).isTrue();
        assertThat(repository.existsPendingTransferForSlot(f.parent, 2)).isFalse();
        // slot2 权益仍可核销
        assertThat(repository.atomicUseEntitlement(firstEntitlementId(f.parent, 2), now)).isPresent();
        // slot1 权益被冻结
        assertThat(repository.atomicUseEntitlement(firstEntitlementId(f.parent, 1), now)).isEmpty();
    }

    // ── 领取后可再转(链式两跳) ──────────────────────────────────────────────
    @Test
    void claimedTicketCanBeReTransferred() {
        Fixture f = seedSinglePerson();
        TicketTransferResponse r1 = transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, null));
        transferService.claim(f.receiver, f.receiverPhone, r1.id(), r1.claimCode());
        // 受赠人再转给 third
        TicketTransferResponse r2 = transferService.initiate(f.receiver, f.receiverPhone, phone(f.parent, null, f.thirdPhone));
        transferService.claim(f.third, f.thirdPhone, r2.id(), null);
        assertThat(holderOf(f.parent, 1)).isEqualTo(f.third);
    }

    // ── 票到期终止在途转赠 ────────────────────────────────────────────────────
    @Test
    void expiryVoidsPendingTransfer() {
        Fixture f = seedSinglePerson();
        TicketTransferResponse r = transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, null));
        int voided = transferService.voidPendingTransfersForParent(f.parent, "ticket_expired", now);
        assertThat(voided).isEqualTo(1);
        TicketTransferRecord row = repository.findTicketTransferById(r.id()).orElseThrow();
        assertThat(row.status()).isEqualTo("voided");
        assertThat(row.voidReason()).isEqualTo("ticket_expired");
        assertThat(repository.existsPendingTransferForSlot(f.parent, 1)).isFalse();
        assertThat(holderOf(f.parent, 1)).isEqualTo(f.buyer); // 退回原持票人
    }

    // ── 转赠在途拒退款 ───────────────────────────────────────────────────────
    @Test
    void pendingTransferBlocksRefund() {
        Fixture f = seedSinglePerson();
        assertThat(repository.existsPendingTransferForParent(f.parent)).isFalse();
        transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, null));
        assertThat(repository.existsPendingTransferForParent(f.parent)).isTrue(); // requireInstanceRefundable/isFullyUnusedActive 据此拒
    }

    // ── 列表 ─────────────────────────────────────────────────────────────────
    @Test
    void incomingAndOutgoingLists() {
        Fixture f = seedSinglePerson();
        transferService.initiate(f.buyer, f.buyerPhone, phone(f.parent, null, f.receiverPhone));
        assertThat(transferService.listOutgoing(f.buyer)).hasSize(1);
        assertThat(transferService.listIncoming(f.receiverPhone)).hasSize(1);
        assertThat(transferService.listIncoming(f.thirdPhone)).isEmpty();
    }

    // ── AUD 复核 P1-3：退款申请即冻结父票后，游乐 gateway 直连消费也须被父票状态闸拦截 ──────────────
    @Test
    void refundPendingParentBlocksAmusementGatewayConsumption() {
        Fixture f = seedMultiPerson(2);
        String frozenSlotEntId = firstEntitlementId(f.parent, 1);
        // 基线：父票 active 时 slot2 可核销
        assertThat(repository.atomicUseEntitlement(firstEntitlementId(f.parent, 2), now)).isPresent();
        // 申请即冻结：父票 active→refund_pending（子权益仍 active，模拟申请~审批空窗）
        assertThat(repository.markTicketInstanceRefundPending(f.parent, now)).isEqualTo(1);
        // 游乐 gateway 直连 atomicUseEntitlement / consumeEntitlement 均须被父票状态闸拦截（0 行/false）
        assertThat(repository.atomicUseEntitlement(frozenSlotEntId, now)).isEmpty();
        assertThat(amusementGateway.consumeEntitlement(frozenSlotEntId)).isFalse();
    }

    // ── AUD 复核 P2-6：多人票各人份额首刷原子抢占场次，第二个跨场次被拒（CAS 命中 0 行）──────────────
    @Test
    void multiPersonSecondSessionClaimRejectedByCas() {
        Fixture f = seedMultiPerson(2);
        // slot1 首刷抢占 sessA（1 行）
        assertThat(repository.markTicketInstanceFirstUse(f.parent, "sessA", now)).isEqualTo(1);
        // slot2 尝试不同场次 sessB → CAS WHERE 不命中 → 0 行（跨场次冲突，调用方据此回滚本次消费）
        assertThat(repository.markTicketInstanceFirstUse(f.parent, "sessB", now)).isEqualTo(0);
        // 同场次 sessA 再抢占幂等成功（1 行）
        assertThat(repository.markTicketInstanceFirstUse(f.parent, "sessA", now)).isEqualTo(1);
    }

    // ── AUD 复核回归修：单人多次票（num_people=1）第二次跨场次核销不被 CAS 误伤（WHERE num_people<=1 恒真）──
    @Test
    void singlePersonMultiTimesSecondSessionNotRejectedByCas() {
        Fixture f = seedSinglePerson();
        // 第一次核销抢占 sessA（1 行，顺带写入 first_session_key，对单人票无害）
        assertThat(repository.markTicketInstanceFirstUse(f.parent, "sessA", now)).isEqualTo(1);
        // 第二次在不同场次/自然日 sessB 核销：单人票 WHERE num_people<=1 恒真 → 仍更新 1 行，不被拒（回归防线）
        assertThat(repository.markTicketInstanceFirstUse(f.parent, "sessB", now)).isEqualTo(1);
        // 再换一个场次 sessC 亦放行（单人多次票可多次核销于任意场次）
        assertThat(repository.markTicketInstanceFirstUse(f.parent, "sessC", now)).isEqualTo(1);
    }

    // ── AUD-012:领取转赠即撤销原持有人已取的动态凭证,旧码不可再被消费 ────────────────
    @Test
    void claimRevokesActiveCredentialsOfTransferredSlot() {
        Fixture f = seedSinglePerson();
        String entId = firstEntitlementId(f.parent, 1);
        // 原持有人转赠前取动态码(active)。
        TicketCredentialResponse cred = verificationService.refreshCredential(entId, f.buyer);
        TicketTransferResponse r = transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, null));
        transferService.claim(f.receiver, f.receiverPhone, r.id(), r.claimCode());
        // 旧凭证已撤销:状态 revoked、明文 token 清空;消费 CAS 0 行(核销主链据此 CREDENTIAL_EXPIRED 拒绝并回滚)。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM ticket_credentials WHERE id = ?", String.class, cred.id()))
                .isEqualTo("revoked");
        assertThat(repository.markTicketCredentialConsumed(cred.id(), now)).isZero();
        // 受赠人可取新码(新码 active 不受撤销影响)。
        assertThat(verificationService.refreshCredential(entId, f.receiver)).isNotNull();
    }

    // ── 复核裁定钉死:重放已消费动态码=按已消费拒(CREDENTIAL_EXPIRED),非 404——
    // 查码按 credential_hash(消费不清),明文 token 列清空只是隐私最小化,不影响定位。 ──
    @Test
    void consumedCredentialStillResolvesByHashSoReplayHitsExpiredNotNotFound() {
        Fixture f = seedSinglePerson();
        String entId = firstEntitlementId(f.parent, 1);
        TicketCredentialResponse cred = verificationService.refreshCredential(entId, f.buyer);
        assertThat(repository.markTicketCredentialConsumed(cred.id(), now)).isEqualTo(1);
        var replayed = repository.findTicketCredentialByToken(cred.credentialToken());
        assertThat(replayed).isPresent();
        assertThat(replayed.get().status()).isEqualTo("consumed");
        assertThat(repository.markTicketCredentialConsumed(cred.id(), now)).isZero();
    }

    // ── AUD-018 领取前复验:pending 在途却已被消费(竞态/历史残留) → 拒领取,转赠留在 pending ──
    @Test
    void claimRejectsWhenSlotUsedBehindPendingTransfer() {
        Fixture f = seedSinglePerson();
        TicketTransferResponse r = transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, null));
        // 直改库模拟绕过冻结的已消费状态(NOT EXISTS 竞态窗口/历史数据)。
        jdbcTemplate.update("UPDATE issued_entitlements SET used_times = 1 WHERE parent_ticket_id = ?", f.parent);
        assertApiCode(() -> transferService.claim(f.receiver, f.receiverPhone, r.id(), r.claimCode()),
                "TICKET_TRANSFER_SLOT_USED");
        assertThat(repository.findTicketTransferById(r.id()).orElseThrow().status()).isEqualTo("pending");
        assertThat(holderOf(f.parent, 1)).isEqualTo(f.buyer);
    }

    // ── AUD-006:发起幂等重放(丢响应重试拿回同一 claim_code,不撞唯一约束不重复建单) ──────
    @Test
    void initiateIdempotentReplayReturnsSameTransferAndClaimCode() {
        Fixture f = seedSinglePerson();
        CustomerTicketTransferRequest req = new CustomerTicketTransferRequest(
                UUID.randomUUID().toString(), f.parent, null, "link", null);
        TicketTransferResponse first = ticketingService.initiateTicketTransfer(f.buyer, f.buyerPhone, req);
        TicketTransferResponse replay = ticketingService.initiateTicketTransfer(f.buyer, f.buyerPhone, req);
        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(replay.claimCode()).isEqualTo(first.claimCode());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ticket_transfers WHERE parent_ticket_id = ?", Integer.class, f.parent))
                .isEqualTo(1);
    }

    // ── AUD-006:同键不同请求=指纹冲突 409(键被占用,不静默错放) ─────────────────────
    @Test
    void initiateSameKeyDifferentRequestConflicts() {
        Fixture f = seedSinglePerson();
        String key = UUID.randomUUID().toString();
        ticketingService.initiateTicketTransfer(f.buyer, f.buyerPhone,
                new CustomerTicketTransferRequest(key, f.parent, null, "link", null));
        assertApiCode(() -> ticketingService.initiateTicketTransfer(f.buyer, f.buyerPhone,
                new CustomerTicketTransferRequest(key, f.parent, null, "phone", f.receiverPhone)),
                "IDEMPOTENCY_CONFLICT");
    }

    // ── AUD-006:幂等键必填(统一键来源=请求体 idempotency_key,客户端恒自动补) ─────────
    @Test
    void initiateAndClaimRequireIdempotencyKey() {
        Fixture f = seedSinglePerson();
        assertApiCode(() -> ticketingService.initiateTicketTransfer(f.buyer, f.buyerPhone,
                new CustomerTicketTransferRequest(null, f.parent, null, "link", null)),
                "VALIDATION_FAILED");
        TicketTransferResponse r = transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, null));
        assertApiCode(() -> ticketingService.claimTicketTransfer(f.receiver, f.receiverPhone, r.id(),
                new com.complex.platform.customer.dto.CustomerTicketTransferClaimRequest(null, r.claimCode())),
                "VALIDATION_FAILED");
    }

    // ── AUD-006:领取幂等重放(同键同请求返回同一结果) ─────────────────────────────
    @Test
    void claimIdempotentReplayByKey() {
        Fixture f = seedSinglePerson();
        TicketTransferResponse r = transferService.initiate(f.buyer, f.buyerPhone, link(f.parent, null));
        var claimReq = new com.complex.platform.customer.dto.CustomerTicketTransferClaimRequest(
                UUID.randomUUID().toString(), r.claimCode());
        TicketTransferResponse first = ticketingService.claimTicketTransfer(f.receiver, f.receiverPhone, r.id(), claimReq);
        TicketTransferResponse replay = ticketingService.claimTicketTransfer(f.receiver, f.receiverPhone, r.id(), claimReq);
        assertThat(first.status()).isEqualTo("claimed");
        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(replay.status()).isEqualTo("claimed");
        assertThat(holderOf(f.parent, 1)).isEqualTo(f.receiver);
    }

    // ── helpers ─────────────────────────────────────────────────────────────
    private CustomerTicketTransferRequest link(String parent, Integer slot) {
        return new CustomerTicketTransferRequest(UUID.randomUUID().toString(), parent, slot, "link", null);
    }

    private CustomerTicketTransferRequest phone(String parent, Integer slot, String receiverPhone) {
        return new CustomerTicketTransferRequest(UUID.randomUUID().toString(), parent, slot, "phone", receiverPhone);
    }

    private String holderOf(String parent, int slot) {
        return repository.findSlotHolder(parent, slot).orElseThrow().holderCustomerId();
    }

    private String firstEntitlementId(String parent, int slot) {
        return repository.listIssuedEntitlementsByParentSlot(parent, slot).get(0).id();
    }

    private static void assertApiCode(ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo(code);
    }

    private Fixture seedSinglePerson() {
        return seed(1);
    }

    private Fixture seedMultiPerson(int people) {
        return seed(people);
    }

    private Fixture seed(int numPeople) {
        seedTypeAndTemplate();
        Fixture f = new Fixture();
        f.buyerPhone = "138" + rnd();
        f.receiverPhone = "139" + rnd();
        f.thirdPhone = "137" + rnd();
        f.buyer = saveCustomer(f.buyerPhone);
        f.receiver = saveCustomer(f.receiverPhone);
        f.third = saveCustomer(f.thirdPhone);
        f.parent = "tkt_" + UUID.randomUUID().toString().replace("-", "");
        String orderId = "ord_" + f.parent;
        jdbcTemplate.update("INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, "
                + "pay_status, issue_status, status, created_at, updated_at) "
                + "VALUES (?, ?, 'tt_tr', 1, 'web', 0, 'paid', 'issued', 'created', now(), now())", orderId, "ONO_" + f.parent);
        repository.saveTicketInstance(new TicketInstanceRecord(
                f.parent, "TNO_" + f.parent, orderId, "tt_tr", 1, numPeople, 1,
                f.buyer, f.buyer, "active", null, null, now, now));
        for (int slot = 1; slot <= numPeople; slot++) {
            repository.upsertSlotHolder(f.parent, slot, f.buyer, now);
            String eid = "ent_" + UUID.randomUUID().toString().replace("-", "");
            repository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                    eid, "code_" + eid, "tt_tr", "ord_" + f.parent, "et_tr", "ticket", "m1", null, null,
                    1, 0, 100L, "active", now.minusSeconds(60), now.plusSeconds(86_400), now, now,
                    1, null, null, null, null, null, f.parent, slot, null, null));
        }
        return f;
    }

    private String saveCustomer(String phone) {
        String id = "cust_" + UUID.randomUUID().toString().replace("-", "");
        customerRepository.saveCustomer(new CustomerRecord(id, phone, mask(phone), "active", null, now, now, now, null));
        return id;
    }

    private void seedTypeAndTemplate() {
        jdbcTemplate.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, status, created_at, updated_at) "
                + "VALUES ('tt_tr','转赠测试票','fixed','active', now(), now()) ON CONFLICT (id) DO NOTHING");
        jdbcTemplate.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status, created_at, updated_at) "
                + "VALUES ('et_tr','转赠测试权益','ticket',1,100,'active', now(), now()) ON CONFLICT (id) DO NOTHING");
    }

    private static String rnd() {
        return String.valueOf(100_000_00L + (long) (Math.random() * 89_999_99L));
    }

    private static String mask(String phone) {
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private static final class Fixture {
        String buyer, receiver, third, parent;
        String buyerPhone, receiverPhone, thirdPhone;
    }
}
