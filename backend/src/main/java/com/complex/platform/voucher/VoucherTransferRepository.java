package com.complex.platform.voucher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** V313 券转赠仓储。CAS 全落 status='pending' 谓词，数据库行锁天然串行化，先提交者赢。 */
public interface VoucherTransferRepository {

    /** 插入 pending 转赠；撞 uq_voucher_transfers_pending / claim_code 唯一 → DuplicateKeyException（已在途）。 */
    VoucherTransferRecord save(VoucherTransferRecord record);

    Optional<VoucherTransferRecord> findById(String id);

    Optional<VoucherTransferRecord> findByClaimCode(String claimCode);

    /** CAS 领取：status pending→claimed 并回填受赠人，返回受影响行数（0＝竞态输/已终态）。 */
    int claim(String id, String receiverCustomerId, Instant now);

    /** CAS 撤回：status pending→revoked（仅发起人），返回受影响行数。 */
    int revoke(String id, String initiatorCustomerId, Instant now);

    /** CAS 系统作废：该券实例所有 pending→voided（券核销/作废/过期时解冻），返回作废条数。 */
    int voidPendingForInstance(String voucherInstanceId, String reason, Instant now);

    /** 该券实例是否有在途（pending）转赠——核销/冻结判定用。 */
    boolean existsPendingForInstance(String voucherInstanceId);

    /** 受赠人「待领取」（手机号盲索引匹配 pending）。 */
    List<VoucherTransferRecord> listPendingByReceiverPhoneHash(String receiverPhoneHash);

    /** 我发起的转赠（全状态，按时间倒序）。 */
    List<VoucherTransferRecord> listByInitiator(String initiatorCustomerId);
}
