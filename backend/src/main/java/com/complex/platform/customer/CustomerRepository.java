package com.complex.platform.customer;

// 文件版本：V0.05

import com.complex.platform.bar.BarDepositRuleRecord;
import com.complex.platform.bar.BarReservationRecord;
import com.complex.platform.bar.BarTableRecord;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository {
    Optional<CustomerRecord> findCustomerByPhone(String phone);

    Optional<CustomerRecord> findCustomerById(String customerId);

    CustomerRecord saveCustomer(CustomerRecord customer);

    CustomerSessionRecord saveSession(CustomerSessionRecord session);

    Optional<CustomerSessionRecord> findSessionByToken(String token);

    /**
     * ROOT-03-R27（ENUM 第 39 行「注销后重新注册的，重新注册时间与注销时间间隔不足一年不可再享新人资格」）：本账户手机号在本账户注册前一年内
     * 注销过（注销记录只留手机号盲索引与时点，见 V537）。default 供内存双子兼容。
     */
    default boolean reRegisteredWithinOneYear(String customerId) {
        return false;
    }

    /** AUD-030：按 token 撤销会话（活动会话置 logged_out），登出后 token 立即失效。返回受影响行数（幂等：0=无匹配活动会话）。 */
    int revokeSessionByToken(String token);

    /** ROOT-03-R21 注销：本人全部活动会话置 revoked。default 供内存双子兼容。 */
    default int revokeSessionsOfCustomer(String customerId) {
        return 0;
    }

    CustomerConsentRecord saveConsent(CustomerConsentRecord consent);

    Optional<CustomerPointsAccountRecord> findPointsByCustomerId(String customerId);

    Optional<CustomerPointsAccountRecord> findPointsByCustomerIdForUpdate(String customerId);

    CustomerPointsAccountRecord savePoints(CustomerPointsAccountRecord account);

    CustomerPointsLedgerRecord savePointsLedger(CustomerPointsLedgerRecord ledger);

    List<CustomerPointsLedgerRecord> listPointsLedgerByCustomerId(String customerId);

    Optional<CustomerRedemptionCatalogItemRecord> findRedemptionCatalogItemById(String itemId);

    List<CustomerRedemptionCatalogItemRecord> listActiveRedemptionCatalogItems(Instant now);

    CustomerPointsRedemptionRecord savePointsRedemption(CustomerPointsRedemptionRecord redemption);

    List<CustomerPointsRedemptionRecord> listPointsRedemptionsByCustomerId(String customerId);

    /** 券核销领取按id读兑换记录;默认空以不破坏测试替身。 */
    default java.util.Optional<CustomerPointsRedemptionRecord> findPointsRedemptionById(String id) {
        return java.util.Optional.empty();
    }

    // 积分规则引擎读写口（原 REQ-12/P0-6）已于 2026-07-28 随 customer_points_rules 停用一并删除（V352）：
    // 该表四个配置维度全被活表接管（points_accrual_rate_configs / customer_member_levels /
    // customer_check_in_tiers / customer_birthday_rules），本表零读取方。表按删除铁律留库不物删。

    List<CustomerBarReservationOptionResponse> listBarReservationOptions();

    Optional<BarTableRecord> findActiveBarTableForReservation(String barId, String tableId, String tableType);

    /** 商户（酒吧）所属综合体 merchants.project_id（PGM 第 736 行按综合体的终端开关）。 */
    default Optional<String> findMerchantProjectId(String merchantId) {
        return Optional.empty();
    }

    // P0#3 订台定金：读 bar_deposit_rules（V073）按 bar 解析启用规则（scope_type DESC 优先）。
    List<BarDepositRuleRecord> listDepositRules(String barId);

    BarReservationRecord saveCustomerBarReservation(BarReservationRecord reservation);

    // ---- 会员忠诚度 第1部分（V089）：等级配置 / 签到 / 生日规则 / 过期批次 ----

    /** 列出所有积分账户（等级评估/过期清零定时作业逐账户处理用）。 */
    default List<CustomerPointsAccountRecord> listAllPointsAccounts() {
        return List.of();
    }

    /** 3.14 keyset 分页：customer_id > after 的下一批账户 id（过期清零逐批小事务用）。默认走内存过滤。 */
    default List<String> listPointsAccountCustomerIdsAfter(String afterCustomerId, int limit) {
        return listAllPointsAccounts().stream()
                .map(CustomerPointsAccountRecord::customerId)
                .filter(id -> afterCustomerId == null || id.compareTo(afterCustomerId) > 0)
                .sorted()
                .limit(Math.max(1, limit))
                .toList();
    }

    default CustomerMemberLevelRecord saveMemberLevel(CustomerMemberLevelRecord level) {
        throw new UnsupportedOperationException("saveMemberLevel not supported by this repository");
    }

    /** 按 level_rank 升序列出生效等级配置（第十八批第 12 条：会员等级全平台一套）。 */
    default List<CustomerMemberLevelRecord> listMemberLevels() {
        return List.of();
    }

    default CustomerCheckInTierRecord saveCheckInTier(CustomerCheckInTierRecord tier) {
        throw new UnsupportedOperationException("saveCheckInTier not supported by this repository");
    }

    default List<CustomerCheckInTierRecord> listCheckInTiers(String projectId) {
        return List.of();
    }

    default CustomerBirthdayRuleRecord saveBirthdayRule(CustomerBirthdayRuleRecord rule) {
        throw new UnsupportedOperationException("saveBirthdayRule not supported by this repository");
    }

    /** 生效的生日加倍规则（全平台一套，BSM L831）。 */
    default List<CustomerBirthdayRuleRecord> listActiveBirthdayRules() {
        return List.of();
    }

    /** 状态无关列出全部等级配置（含 inactive/archived）；供写路径 resolveId 与唯一性预检用。 */
    default List<CustomerMemberLevelRecord> listAllMemberLevels() {
        return List.of();
    }

    /** 状态无关、按 project 严格过滤列出签到阶梯（含 inactive）；供写路径 resolveId 与唯一性预检用。 */
    default List<CustomerCheckInTierRecord> listCheckInTiersByProject(String projectId) {
        return List.of();
    }

    /** 状态无关列出全部生日规则（含 inactive）；供写路径 resolveId 与唯一性预检用。 */
    default List<CustomerBirthdayRuleRecord> listAllBirthdayRules() {
        return List.of();
    }

    default CustomerCheckInRecord saveCheckIn(CustomerCheckInRecord checkIn) {
        throw new UnsupportedOperationException("saveCheckIn not supported by this repository");
    }

    default Optional<CustomerCheckInRecord> findCheckIn(String customerId, java.time.LocalDate checkInDate) {
        return Optional.empty();
    }

    default List<CustomerCheckInRecord> listCheckIns(String customerId) {
        return List.of();
    }

    /** 列出该账户已到期、尚有剩余的获取批次（FIFO 过期清零用），按 expire_at 升序。 */
    default List<CustomerPointsLedgerRecord> listExpirableBatches(String customerId, Instant now) {
        return List.of();
    }

    /** 列出该账户尚有剩余、有过期时间的获取批次（FIFO 消耗用，使过期只清未用部分），按时间升序。 */
    default List<CustomerPointsLedgerRecord> listOpenBatches(String customerId) {
        return List.of();
    }

    /** 标记某获取批次为已过期并清零剩余。 */
    default void markBatchExpired(String ledgerId) {
        throw new UnsupportedOperationException("markBatchExpired not supported by this repository");
    }

    /** 扣减获取批次剩余积分（兑换/扣减时 FIFO 消耗，使过期只清未用部分）。 */
    default void consumeBatchRemaining(String ledgerId, long remainingPoints) {
        throw new UnsupportedOperationException("consumeBatchRemaining not supported by this repository");
    }

    // ---- P0 清单#2 积分入账引擎（V100）：按业务单据幂等 + 履约/退款查账 ----

    /** 按业务单据 + 变动类型查已存在的入账流水（幂等：命中即视为已入账，不重复写）。 */
    default Optional<CustomerPointsLedgerRecord> findPointsLedgerBySourceDoc(
            String sourceDocType, String sourceDocId, String changeType) {
        return Optional.empty();
    }

    /**
     * AUD-014：同 findPointsLedgerBySourceDoc，但对命中的流水行加 FOR UPDATE 行锁。
     * confirmPoints 在算 delta 前锁定 earn 行 → 串行化同一 earn 行的并发确认/冲正，
     * 杜绝两事务基于同一旧 confirmed_amount 各自给账户加钱（余额多于明细的账实分叉）。锁序固定 earn→account。
     * 默认委派非锁版（内存仓储无行锁语义，行为不变）；仅 Jdbc 真仓储覆写加 FOR UPDATE。
     */
    default Optional<CustomerPointsLedgerRecord> findPointsLedgerBySourceDocForUpdate(
            String sourceDocType, String sourceDocId, String changeType) {
        return findPointsLedgerBySourceDoc(sourceDocType, sourceDocId, changeType);
    }

    /** 列出某业务单据下指定变动类型的入账流水（履约转 confirmed / 退款按已 confirmed 比例 deduct 用）。 */
    default List<CustomerPointsLedgerRecord> listPointsLedgerBySourceDoc(
            String sourceDocType, String sourceDocId, String changeType) {
        return List.of();
    }

    /**
     * P0-B（V344）积分台账改追加：某业务单据「已确认份额」的权威口径 = 该单 confirm 分录的净额和
     * （Σ points_delta，正=确认、负=冲正）。取代改造前直接读 earn 行被就地改写的 confirmed_amount。
     * 存量单由 V344 逐单补建一条等额 confirm 分录，故派生值与改造前逐单严格相等（零行为改变）。
     */
    default long sumConfirmedPointsBySourceDoc(String sourceDocType, String sourceDocId) {
        return listPointsLedgerBySourceDoc(sourceDocType, sourceDocId, CustomerPointsLedgerRecord.CHANGE_CONFIRM)
                .stream().mapToLong(CustomerPointsLedgerRecord::pointsDelta).sum();
    }

    /**
     * F-TXN-03 按核销张数：确认份额变动后同步 earn 行的生命周期列——status 与可过期批次余额
     * remainingPoints（非空时令其 = 已确认份额，使过期口径与实际入余额一致；为空则不动该列）。
     * P0-B（V344）：已确认份额本身<b>不再</b>就地回写 earn 行（confirmed_amount 已被 DB 触发器冻结），
     * 改为追加 confirm 分录；本方法只动 FIFO 批次运行态，与 ar_ap_items.paid/unpaid 可变口径同源。
     */
    default void updatePointsLedgerLifecycle(String ledgerId, String status, Long remainingPoints) {
        throw new UnsupportedOperationException("updatePointsLedgerLifecycle not supported by this repository");
    }

    // ---- 会员忠诚度 第2部分（V090）：储值 / 付费会员 / 积分转赠 ----

    /** 储值账户（每客每项目唯一），带行锁用于充值/消费一致。 */
    default Optional<CustomerStoredValueAccountRecord> findStoredValueAccountForUpdate(String customerId, String projectId) {
        return Optional.empty();
    }

    default Optional<CustomerStoredValueAccountRecord> findStoredValueAccount(String customerId, String projectId) {
        return Optional.empty();
    }

    // B1d⑤ 卡范围查找：merchantId 非空=商户级卡，为空=综合体通用卡。默认实现：通用卡委派 2 参版（保留既有假实现行为），
    // 商户卡返回空（仅 Jdbc 真仓储承载）。
    default Optional<CustomerStoredValueAccountRecord> findStoredValueAccount(String customerId, String projectId, String merchantId) {
        return merchantId == null ? findStoredValueAccount(customerId, projectId) : Optional.empty();
    }

    default Optional<CustomerStoredValueAccountRecord> findStoredValueAccountForUpdate(String customerId, String projectId, String merchantId) {
        return merchantId == null ? findStoredValueAccountForUpdate(customerId, projectId) : Optional.empty();
    }

    /** 客人端「我的全部卡」：列该客人所有储值账户（通用卡 + 各商户卡），供分卡余额展示。 */
    default List<CustomerStoredValueAccountRecord> listStoredValueAccountsByCustomer(String customerId) {
        return List.of();
    }

    default CustomerStoredValueAccountRecord saveStoredValueAccount(CustomerStoredValueAccountRecord account) {
        throw new UnsupportedOperationException("saveStoredValueAccount not supported by this repository");
    }

    /** ROOT-03-R30 客人端「我的余额」逐笔（带支付通路、扣款商户、回冲原流水）。 */
    default List<CustomerStoredValueTransactionView> listStoredValueTransactionViews(String customerId, String accountId, int page, int size) {
        throw new UnsupportedOperationException("listStoredValueTransactionViews not supported");
    }

    default List<CustomerStoredValueTransactionRecord> listStoredValueTransactions(
            String customerId, String accountId, int page, int size) {
        throw new UnsupportedOperationException("listStoredValueTransactions not supported");
    }

    default CustomerStoredValueTransactionRecord saveStoredValueTransaction(CustomerStoredValueTransactionRecord txn) {
        throw new UnsupportedOperationException("saveStoredValueTransaction not supported by this repository");
    }

    // B1d⑤ phase3 商户级卡退款规则（发卡商户自设可否退/比例）。无配置=默认允许全额。
    default Optional<StoredValueMerchantRefundRuleRecord> findStoredValueMerchantRefundRule(String merchantId) {
        return Optional.empty();
    }

    default StoredValueMerchantRefundRuleRecord saveStoredValueMerchantRefundRule(StoredValueMerchantRefundRuleRecord rule) {
        throw new UnsupportedOperationException("saveStoredValueMerchantRefundRule not supported by this repository");
    }

    /** 第十八批第 11 条：某时点生效的储值充值赠送比例版本（id 与万分比）。 */
    record StoredValueBonusVersion(String id, int bps) {}

    /**
     * 第十八批第 11 条（ENUM L86；BSM L521）：储值充值赠送比例取某时点已审批、批准不晚于生效、已到生效时间的最新一版
     * （kind＝{@link TicketRewardConfigService#STORED_VALUE_BONUS} 按项目，{@link TicketRewardConfigService#MERCHANT_CARD_BONUS} 按发卡商户）；无＝不赠送。
     */
    default Optional<StoredValueBonusVersion> findEffectiveStoredValueBonus(String kind, String projectId, String merchantId, Instant at) {
        return Optional.empty();
    }

    /** 幂等校验：按 idempotency_key 查已落库的储值流水。 */
    default Optional<CustomerStoredValueTransactionRecord> findStoredValueTransactionByIdempotencyKey(String key) {
        return Optional.empty();
    }

    /** 储值流水是否已引用某支付意图（防同一充值支付意图重复入账）。 */
    default boolean storedValueTransactionExistsForPaymentIntent(String paymentIntentId) {
        return false;
    }

    // AUD-058 充值支付意图 ↔ 卡范围绑定：topup 落绑定行，settleTopup 只认绑定的 (customer, project, merchant) 入账。
    default int bindStoredValueTopupIntent(String paymentIntentId, String customerId, String projectId,
                                           String merchantId, Instant createdAt) {
        throw new UnsupportedOperationException("bindStoredValueTopupIntent not supported by this repository");
    }

    default Optional<StoredValueTopupIntentScope> findStoredValueTopupIntent(String paymentIntentId) {
        return Optional.empty();
    }

    // ---------- TK-19 充值批次 / 消费分摊 / 退款恢复；TK-20 消费退款回原卡（V517）。default 供内存测试双子零改动兼容：读返回空、写不落 ----------

    /** 通路费率：按 (项目, 支付通路) 取 channel_fee_configs 生效配置（项目特例优先），未配置 = 0。 */
    default int findStoredValueChannelFeeBps(String projectId, String channel) {
        return 0;
    }

    default void saveStoredValueTopupBatch(StoredValueTopupBatchRecord batch) {
    }

    /** 该卡剩余本金 > 0 的批次（按充值先后），行锁。 */
    default List<StoredValueTopupBatchRecord> listOpenStoredValueTopupBatchesForUpdate(String accountId) {
        return List.of();
    }

    default void addStoredValueTopupBatchRemaining(String batchId, long deltaCent) {
    }

    default void saveStoredValueConsumeAllocation(StoredValueConsumeAllocationRecord allocation) {
    }

    default List<StoredValueConsumeAllocationRecord> listStoredValueConsumeAllocationsForUpdate(String consumeTxnId) {
        return List.of();
    }

    default void addStoredValueAllocationRestored(String allocationId, long deltaCent) {
    }

    default void saveStoredValueConsumeRestoration(String id, String reversalTxnId, String allocationId, long principalCent,
                                                   int feeBps, long feeAdjustCent, Instant createdAt) {
    }

    default Optional<CustomerStoredValueTransactionRecord> findStoredValueTransactionByIdForUpdate(String id) {
        return Optional.empty();
    }

    /** consume_reversal 流水（流水表 append-only，original_txn_id 随 INSERT 写入）。 */
    default CustomerStoredValueTransactionRecord saveStoredValueReversalTransaction(CustomerStoredValueTransactionRecord txn,
                                                                                    String originalTxnId) {
        return saveStoredValueTransaction(txn);
    }

    /** consume_reversal 流水回指的原消费流水 id（original_txn_id）。 */
    default Optional<String> findStoredValueOriginalTxnId(String txnId) {
        return Optional.empty();
    }

    /** 复核 money I3：撤回某笔消费退款回冲对原充值批次的恢复（分摊已恢复额与批次剩余本金按该次恢复记录减回）。 */
    default void undoStoredValueRestorations(String reversalTxnId) {
    }

    /** 原消费已回冲合计 {本金, 赠送金}（正数）。 */
    default long[] sumStoredValueReversals(String originalTxnId) {
        return new long[] {0L, 0L};
    }

    /** 幂等键前缀匹配的流水金额合计（酒吧退品驳回扣回已回冲储值用）。 */
    default long sumStoredValueTransactionsByIdempotencyPrefix(String prefix) {
        return 0L;
    }

    // 积分会员间转赠已下线（HTML：积分不支持会员间转赠；2026-06-24 裁决）。相关 repo 方法/记录类/表已随 V172 清理移除。
}
