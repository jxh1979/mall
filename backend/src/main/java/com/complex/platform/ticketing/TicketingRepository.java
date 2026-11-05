package com.complex.platform.ticketing;

// 文件版本：V0.10

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TicketingRepository {
    TicketTypeRecord saveTicketType(TicketTypeRecord ticketType);

    /** 票种商户和明确游乐点位对应的合法综合体；多个候选不得由操作人项目覆盖。 */
    default List<String> listTicketTypeProjectIds(String ticketTypeId) { return List.of(); }

    /** 仅用于原订单已记录商户的归属核实，不按当前票种配置反推历史。 */
    default List<String> listMerchantProjectIds(String merchantId) { return List.of(); }

    List<TicketTypeRecord> listTicketTypes(boolean includeArchived);

    default List<TicketTypeRecord> listTicketTypes() {
        return listTicketTypes(false);
    }

    void deleteTicketType(String id);

    long countTicketTypeReferences(String id);

    Optional<TicketTypeRecord> findTicketTypeById(String id);

    /**
     * #172(2026-07-19 拍板)：全场统一动态码刷新间隔（秒）。凡出示验证类二维码发凭证时读它决定刷新窗口，
     * 不再按票种各配。default 返回 15（兼容既有测试假实现；生产由 Jdbc 覆盖读 ticket_qr_refresh_config）。
     */
    /** clients-R40：作废某人份的在途转赠（原持有人改转他人），返回作废条数。 */
    default int voidPendingTransfersForSlot(String parentTicketId, int slotIndex, String reason, java.time.Instant now) {
        return 0;
    }

    /** clients-R40：父票首场次（未开启返回空）。 */
    default java.util.Optional<FirstSessionWindow.Session> findFirstSession(String parentTicketId) {
        return java.util.Optional.empty();
    }

    default int findQrRefreshIntervalSeconds() {
        return 15;
    }

    /** #172：后台改全场统一刷新间隔（秒）。default 空实现（测试假实现无持久层）；Jdbc 覆盖 upsert 单行 'default'。 */
    default void upsertQrRefreshIntervalSeconds(int refreshIntervalSeconds, String updatedBy, java.time.Instant now) {
    }

    /**
     * P0 修复（V133）：原子扣减票种库存——sold_quantity += quantity，仅当不限量（stock_quantity 为 null）
     * 或扣减后不超上限时成功。返回 true=扣减成功；false=库存不足（超卖被拒）。
     * default 返回 true（不限量）兼容既有测试假实现。
     */
    default boolean atomicDeductTicketStock(String ticketTypeId, int quantity) {
        return true;
    }

    /**
     * P0 修复（V133）：释放票种库存（退款/取消时回补）——sold_quantity = GREATEST(0, sold_quantity - quantity)。
     * default 空实现兼容既有测试假实现。
     */
    default void releaseTicketStock(String ticketTypeId, int quantity) {
    }

    EntitlementTemplateRecord saveEntitlementTemplate(EntitlementTemplateRecord template);

    List<EntitlementTemplateRecord> listEntitlementTemplates(boolean includeArchived);

    /** 物理删除权益模板(审批通过后调用)。 */
    void deleteEntitlementTemplate(String id);

    /** 该模板被票种绑定/已发放权益引用的活跃条数(>0 则禁止硬删)。 */
    long countTemplateReferences(String id);

    Optional<EntitlementTemplateRecord> findEntitlementTemplateById(String id);

    TicketTypeEntitlementRecord saveTicketTypeEntitlement(TicketTypeEntitlementRecord entitlement);

    List<TicketTypeEntitlementRecord> listTicketTypeEntitlements(String ticketTypeId);

    IssuedEntitlementRecord saveIssuedEntitlement(IssuedEntitlementRecord entitlement);

    Optional<IssuedEntitlementRecord> findIssuedEntitlementByCode(String entitlementCode);

    // AUD-066 父票实例：default 兼容既有假实现（不落父票，无父票的核销守卫自然跳过）；Jdbc+InMemory 双写覆盖。
    default TicketInstanceRecord saveTicketInstance(TicketInstanceRecord instance) {
        return instance;
    }

    default Optional<TicketInstanceRecord> findTicketInstanceById(String id) {
        return Optional.empty();
    }

    default List<TicketInstanceRecord> listTicketInstances() {
        return java.util.List.of();
    }

    default List<TicketInstanceRecord> listTicketInstancesByOrderId(String orderId) {
        return java.util.List.of();
    }

    /** AUD-066 + AUD 复核 P2-6：原子抢占父票首场次键（写一次）。命中 0 行=已被别的场次抢占→调用方回滚消费。
     *  default 视为放行（返回 1）：无库支撑的测试替身不做场次约束。 */
    default int markTicketInstanceFirstUse(String id, String sessionKey, java.time.Instant now) {
        return 1;
    }

    /** 方案2 申请即冻结：父票 active→refund_pending（CAS，返回命中行数）。default 返回 0（无父票=不放行整票退）。 */
    default int markTicketInstanceRefundPending(String id, java.time.Instant now) {
        return 0;
    }

    /** 方案2 生效：父票 refund_pending→refunded（CAS）。default 返回 0。 */
    default int markTicketInstanceRefunded(String id, java.time.Instant now) {
        return 0;
    }

    /** 方案2 驳回：父票 refund_pending→active 解冻（CAS）。default 返回 0。 */
    default int restoreTicketInstanceActive(String id, java.time.Instant now) {
        return 0;
    }

    /** 方案2 复核驳回：退款单 requested→rejected（CAS，返回命中行数）。default 返回 0。 */
    default int markTicketRefundRejected(String refundId, String rejectedBy, java.time.Instant rejectedAt) {
        return 0;
    }

    /** TK-05（BSM:806「记录退款原因、责任主体」）：主体主数据中在用（active）的法律主体。default false（无库支撑=不放行）。 */
    default boolean isActiveBusinessEntity(String id) {
        return false;
    }

    /**
     * S5：按 bar_products.id 取提供方商品投影（内部结算价 + 提供方归属 + 扣库前置）。
     * bar_item_ref 语义已定死为 bar_products.id，本方法是票务侧唯一的商品解析入口
     * （取价走 {@link InternalSettlementPriceResolver}，建模板校验走 TicketCatalogService）。
     * 无此商品 → 空 → 调用方 fail-closed。default 返回空兼容既有测试假实现。
     */
    default Optional<BarProductRefRecord> findBarProductRef(String barProductId) {
        return Optional.empty();
    }

    /**
     * 批10/E(V379)：内部结算 SKU 目录生效价(is_draft=FALSE + status=active)。
     * 解析器两支自此统一从目录取价：草案不生效、下线归档无生效行 → 空 → 调用方 fail-closed。
     * default 返回空：内存测试替身若要跑内部价解析，须自行覆写。
     */
    default Optional<CatalogPriceRef> findActiveCatalogPrice(String itemCode) {
        return Optional.empty();
    }

    /** 目录生效价投影：绑定商户(引用归属校验用) + 价(分,0 合法=商户让利型) + 目录行 id(FS-012:转正即新行,行 id＝价格版本)。 */
    record CatalogPriceRef(String merchantId, long priceCent, String id) {
    }

    /**
     * 批10/E：目录条目(生效或草案)的绑定商户——建模板设引用时校验「条目商家 = 模板提供方」用。
     * 草案态条目也可被引用(转正前解析 fail-closed)，故本查询不限定 is_draft。空 = 无此条目。
     */
    default Optional<String> findCatalogItemMerchant(String itemCode) {
        return Optional.empty();
    }

    /**
     * S5 K16 跨提供方核销守卫：按核销设备定位「本次核销发生在哪家商户」（terminal_devices.merchant_id，V007）。
     * 设备未绑商户 → 空 → 调用方 fail-closed（说不清这杯酒在哪家核销的，就不能核销）。
     * default 返回空：内存测试替身若要跑提供方兑付类权益的核销，须自行覆写并绑好设备归属。
     */
    default Optional<String> findTerminalDeviceMerchantId(String deviceId) {
        return Optional.empty();
    }

    /**
     * S5：该票种绑定的权益 + 各自提供方商户（含商户业态）。K17 发布校验与票面按吧分组投影共用一份数据。
     * default 返回空列表兼容既有测试假实现。
     */
    default List<TicketTypeProviderEntitlementRow> listTicketTypeProviderEntitlements(String ticketTypeId) {
        return List.of();
    }

    /**
     * 结算重构批3-2(PRD §6.2)：按 venue 营业时间表惰性解析当前场次。
     * 当前营业 → 取/开当日 open 场次并返回 session_id；非营业时段 → 空(调用方报 VENUE_NOT_IN_SESSION)。
     * default 返回空兼容既有测试假实现（无 per_session 权益的测试不受影响）。
     */
    default Optional<String> resolveOpenSessionForVenue(String venueId, java.time.Instant now) {
        return Optional.empty();
    }

    /**
     * 结算重构批3-2：记录该权益在该场次的首次入场(唯一键 entitlement_id+session_id)。
     * 返回 true=本次真插入(本场次首入,应消耗一次)；false=本场次已入过(放行不消耗)。default 恒 true 兼容假实现。
     */
    default boolean recordEntitlementEntry(String entitlementId, String sessionId, java.time.Instant now) {
        return true;
    }

    /** 结算重构批3-2：该权益在该场次是否已有入场记录。default false。 */
    default boolean hasEntitlementEntryInSession(String entitlementId, String sessionId) {
        return false;
    }

    /** 结算重构批3-2：该权益是否曾在别的场次入过场(session_id != 当前) → 过场作废判定。default false。 */
    default boolean hasEntitlementEntryInOtherSession(String entitlementId, String sessionId) {
        return false;
    }

    /**
     * 结算重构批3-3(PRD §6.3)：游乐「当天无限」——记录该权益在该自然日的首次激活
     * (唯一键 entitlement_id+usage_date)。返回 true=本次真插入(当天首刷,调用方应消耗一次)；
     * false=当天已激活/并发抢输(放行不消耗)。default 恒 true 兼容假实现。
     */
    default boolean recordEntitlementDayActivation(String entitlementId, java.time.LocalDate usageDate, java.time.Instant now) {
        return true;
    }

    /** 结算重构批3-3：该权益在该自然日是否已有激活记录(当天已激活→放行不消耗)。default false。 */
    default boolean hasEntitlementDayActivation(String entitlementId, java.time.LocalDate usageDate) {
        return false;
    }

    IssuedEntitlementRecord updateIssuedEntitlement(IssuedEntitlementRecord entitlement);

    VerificationRecord saveVerification(VerificationRecord verification);

    /**
     * 健壮性修复：核销写 ticket_verifications 前校验 device_id 是否已注册（terminal_devices.id）。
     * FK fk_ticket_verifications_device (device_id -> terminal_devices.id, NOT VALID) 对新插入强制；
     * 未注册设备会在 INSERT 时违反 FK 使事务中止、进而连累失败留痕，掩盖原始业务错。
     * 默认放行（=true，供内存测试替身不改）；JdbcTicketingRepository 用真实 EXISTS 查询覆写。
     */
    default boolean terminalDeviceExists(String deviceId) {
        return true;
    }

    /** 批3:第三方券配置 upsert。default no-op 兼容测试双子。 */
    default PartnerVoucherConfigRecord savePartnerVoucherConfig(PartnerVoucherConfigRecord config) {
        return config;
    }

    default Optional<PartnerVoucherConfigRecord> findPartnerVoucherConfig(String entitlementTemplateId) {
        return Optional.empty();
    }

    default PartnerVoucherRedemptionRecord savePartnerVoucherRedemption(PartnerVoucherRedemptionRecord redemption) {
        return redemption;
    }

    default Optional<PartnerVoucherRedemptionRecord> findPartnerVoucherRedemptionByExternalRef(String partnerId, String externalRef) {
        return Optional.empty();
    }

    /**
     * 第十八批 Q 段 148-R1（ENUM:55）：父票 parentTicketId 第 slotIndex 号人份在这家酒吧（权益 bar_id，为空取 merchant_id）的入场类权益
     * （entry / admission）自 since 起有核销成功记录。父票为空返回 false。
     */
    boolean hasVerifiedBarEntrySince(String parentTicketId, int slotIndex, String providerMerchantId, java.time.Instant since);

    List<VerificationRecord> listVerifications();

    Optional<VerificationRecord> findVerificationById(String id);

    // F-TXN-04 并发双花修复：把"原 success 核销 → reversed"做成状态 CAS。仅当 status='success' 且 reversed_at IS NULL
    // 才翻转并记 reversed_by/reversed_at，返回受影响行数。并发两路冲正只有一路得 1（CAS 胜出方），另一路得 0 →
    // 调用方据此判定"已被并发冲正"拒绝，杜绝同一核销被重复冲正、used_times 被多次回滚（多次票尤甚）。
    int markVerificationReversed(String id, String reversedBy, java.time.Instant reversedAt);

    /** TK-14：该原核销是否有待处置的异常复核。default 无库替身视为没有；Jdbc 覆写。 */
    default boolean openVerificationAnomalyExists(String verificationId) {
        return false;
    }

    /** TK-14：异常复核处置 CAS（仅待处置的 anomaly 记录得 1）。default 返回 0（无库替身不支持处置）；Jdbc 覆写。 */
    default int markVerificationAnomalyResolved(String anomalyId, String resolution, String resolvedBy,
                                                java.time.Instant resolvedAt, String reason) {
        return 0;
    }

    /** F-TXN-04 退款审批状态 CAS：仅当退款单仍为 requested 才翻转 approved，返回受影响行数（并发只一路得 1，防双拨付）。 */
    int markTicketRefundApproved(String refundId, String approvedBy, java.time.Instant approvedAt);

    /** AUD-018：退款兑付完成 CAS(status='approved'→'success' + succeeded_at)，仅当仍 approved 生效。返回受影响行数。 */
    int markTicketRefundSucceeded(String refundId, java.time.Instant succeededAt);

    /** 退款单台账（后台“待确认拨付”看板的数据源）：按创建时间倒序。 */
    List<TicketRefundRecord> listTicketRefunds();

    TicketPriceRuleRecord savePriceRule(TicketPriceRuleRecord rule);

    /** upsert 分销商户费率。default 原样返回兼容测试双子。 */
    default TicketDistributionMerchantRateRecord saveDistributionMerchantRate(TicketDistributionMerchantRateRecord rate) {
        return rate;
    }

    List<TicketPriceRuleRecord> listPriceRules(boolean includeArchived);

    default List<TicketPriceRuleRecord> listPriceRules() {
        return listPriceRules(false);
    }

    void deletePriceRule(String id);

    long countPriceRuleReferences(String id);

    Optional<TicketPriceRuleRecord> findPriceRuleById(String id);

    TicketDynamicPricingRuleRecord saveDynamicPricingRule(TicketDynamicPricingRuleRecord rule);

    List<TicketDynamicPricingRuleRecord> listDynamicPricingRules();

    Optional<TicketDynamicPricingRuleRecord> findDynamicPricingRuleById(String id);

    TicketOrderRecord saveTicketOrder(TicketOrderRecord order);

    Optional<TicketOrderRecord> findTicketOrderById(String id);

    default Optional<TicketOrderRecord> findTicketOrderByIdForUpdate(String id) {
        return findTicketOrderById(id);
    }

    TicketOrderRecord updateTicketOrder(TicketOrderRecord order);

    List<TicketOrderRecord> listTicketOrders();

    List<TicketOrderRecord> listTicketOrdersByCustomerId(String customerId);

    // 行级隔离（V135）：按下单受理商户过滤订单列表（商户 scope 仅见自身，平台 scope 走 listTicketOrders 看全部）。
    List<TicketOrderRecord> listTicketOrdersByMerchant(String merchantId);

    // 第十八批 N 段第 120 条 120-R2：按购票手机号查订单（一体机订单查询）；merchantScope 非空时同 listTicketOrdersByMerchant 口径。
    default List<TicketOrderRecord> listTicketOrdersByBuyerPhone(String phone, String merchantScope) {
        return (merchantScope == null ? listTicketOrders() : listTicketOrdersByMerchant(merchantScope)).stream()
                .filter(order -> phone.equals(order.buyerPhone()))
                .toList();
    }

    TicketPaymentIntentRecord saveTicketPaymentIntent(TicketPaymentIntentRecord intent);

    // 支付回写闭环（2a-2）：读取票务支付意图（确认支付成功时按 id 校验归属/金额并回写订单 paid）。
    Optional<TicketPaymentIntentRecord> findTicketPaymentIntentById(String id);

    // TK-20-R3：该票务订单已成功的支付意图（最新一条；储值支付时 id = 储值消费流水 id、channel = stored_value）。default 供内存双子兼容。
    default Optional<TicketPaymentIntentRecord> findSucceededTicketPaymentIntentByOrder(String orderId) {
        return Optional.empty();
    }

    // ROOT-03-R20 组合支付：该票务订单的全部付款意向，按付款先后（updated_at、id 升序）——算已付与待付、超时退回、倒序退款都用它。
    // default 供内存双子兼容。
    default List<TicketPaymentIntentRecord> listTicketPaymentIntentsByOrder(String orderId) {
        return findSucceededTicketPaymentIntentByOrder(orderId).map(List::of).orElse(List.of());
    }

    // ROOT-03-R20：本订单除指定退款单外已批准、已成功的退款合计（按收款相反顺序分配退款笔时，已退走的从后往前扣）。
    default long sumApprovedRefundAmountByOrderExcluding(String orderId, String refundId) {
        return 0L;
    }

    // ROOT-03-R27：客人订单命中的全部资格价（[{rule_id, variant_type, unit_price_cent, rule_updated_at}]，规则版本以规则更新时刻快照）。
    default void saveTicketOrderPriceHits(String orderId, List<Map<String, Object>> priceHits) {
    }

    // ROOT-03-R20（BSM 第 1749 行）：批准时分到在线笔、待财务原路打款的各笔。内存桩不落。
    default void saveTicketRefundPayoutItems(String refundId, List<TicketRefundRecord.PayoutItem> items) {
    }

    default List<Map<String, Object>> findTicketOrderPriceHits(String orderId) {
        return List.of();
    }

    // 退款冻结（2a-3）：退款审批通过 → 把 active 权益置 refunded（不可再核销）。返回受影响行数（幂等：已 refunded 返回 0）。
    int freezeEntitlementForRefund(String id, java.time.Instant updatedAt);

    // 库存超时释放（2a-5）：原子取消未支付订单（仅 created+unpaid）。返回 1 时调用方才回补库存（防并发重复释放）。
    int atomicCancelUnpaidOrder(String orderId, java.time.Instant now);

    // 库存超时释放（2a-5）：列出超时未支付订单（created+unpaid 且 created_at < cutoff），供定时任务回补库存。
    List<TicketOrderRecord> listExpiredUnpaidOrders(java.time.Instant cutoff);

    /**
     * F-TXN-03 门票生命周期：列出已过期（valid_to &lt; now）、仍 active、且全未核销（used_times=0）的权益——
     * 到期自动退款 / 一张用后其余作废的候选集。default 空兼容既有测试双子。
     */
    default List<IssuedEntitlementRecord> listExpiredUnusedActiveEntitlements(java.time.Instant now) {
        return List.of();
    }

    /**
     * F-TXN-03 一张用后其余作废：把到期未用权益置 forfeited（仅 active 且 used_times=0 才置，幂等）。返回受影响行数。
     * default 返回 0 兼容既有测试双子。
     */
    default int forfeitEntitlement(String id, java.time.Instant now) {
        return 0;
    }

    /**
     * ROOT-03-R21 注销／拉黑：把本人持有的未核销权益置 forfeited（含已开始使用的多次票剩余次数，不退款；仅 active 且未用完才置，幂等）。返回受影响行数。
     * default 返回 0 兼容既有测试双子。
     */
    default int forfeitHeldEntitlement(String id, java.time.Instant now) {
        return 0;
    }

    /**
     * AUD-076 撤场提前到期：把归属该酒吧（bar_id）的在效权益 valid_to 提前到截止营业时刻（只提前不延长），
     * 由 processExpiredPaidEntitlements 走方案2到期规则处置。返回受影响行数。default 返回 0 兼容既有测试双子。
     */
    default int expireBarEntitlements(String barId, java.time.Instant validTo, java.time.Instant now) {
        return 0;
    }

    List<IssuedEntitlementRecord> listIssuedEntitlements();

    List<IssuedEntitlementRecord> listIssuedEntitlementsByOrderId(String orderId);

    Optional<IssuedEntitlementRecord> findIssuedEntitlementById(String id);

    TicketCredentialRecord saveTicketCredential(TicketCredentialRecord credential);

    Optional<TicketCredentialRecord> findTicketCredentialByToken(String credentialToken);

    TicketRefundRecord saveTicketRefund(TicketRefundRecord refund);

    Optional<TicketRefundRecord> findTicketRefundById(String id);

    TicketRefundLineRecord saveTicketRefundLine(TicketRefundLineRecord line);

    List<TicketRefundLineRecord> listTicketRefundLines(String refundId);

    long sumPendingRefundAmountByOrderId(String orderId);

    Optional<IssuedEntitlementRecord> atomicUseEntitlement(String id, java.time.Instant updatedAt);

    Optional<IssuedEntitlementRecord> atomicReverseEntitlement(String id, java.time.Instant updatedAt);

    boolean tryLockOrderForIssuance(String orderId);

    String findLatestFlywayVersion();

    /** V271 存档理由定向写入(reason 非空=存档,null=取消存档清空)。 */
    void setEntitlementTemplateArchiveReason(String id, String reason);

    /** V271 存档理由定向写入(reason 非空=存档,null=取消存档清空)。 */
    void setTicketTypeArchiveReason(String id, String reason);

    /** V271 存档理由定向写入(reason 非空=存档,null=取消存档清空)。 */
    void setPriceRuleArchiveReason(String id, String reason);

    // AUD-077 门票转赠 / slot 持有人 (default 空兼容测试双子; JdbcTicketingRepository 实现真 SQL)
    default TicketSlotHolderRecord upsertSlotHolder(String parentTicketId, int slotIndex, String holderCustomerId, java.time.Instant now) {
        return new TicketSlotHolderRecord(parentTicketId, slotIndex, holderCustomerId, now, now);
    }
    default Optional<TicketSlotHolderRecord> findSlotHolder(String parentTicketId, int slotIndex) { return Optional.empty(); }

    /** 复核修正(AUD-012 残留竞态):取码链共享锁读持有人——与 claim 对同行的 UPDATE 互斥,
     * 封死「claim 撤销扫不到未提交新凭证」的毫秒级双花窗口。内存实现回退普通读(单线程测试无竞态)。 */
    default Optional<TicketSlotHolderRecord> findSlotHolderForShare(String parentTicketId, int slotIndex) {
        return findSlotHolder(parentTicketId, slotIndex);
    }
    default List<TicketSlotHolderRecord> listSlotHoldersByHolder(String holderCustomerId) { return List.of(); }
    default int updateTicketInstanceHolder(String id, String holderCustomerId, java.time.Instant now) { return 0; }
    default List<IssuedEntitlementRecord> listIssuedEntitlementsByParentSlot(String parentTicketId, int slotIndex) { return List.of(); }
    /** AUD-018 发起转赠锁定读:锁住 slot 权益行与核销互斥。default 退化普通读(测试双子);Jdbc 覆写 FOR UPDATE。 */
    default List<IssuedEntitlementRecord> listIssuedEntitlementsByParentSlotForUpdate(String parentTicketId, int slotIndex) {
        return listIssuedEntitlementsByParentSlot(parentTicketId, slotIndex);
    }
    /** AUD-012 领取转赠即撤销该 slot 现存 active 动态凭证(active→revoked,返回行数)。default 0(无凭证存储的测试双子)。 */
    default int revokeActiveCredentialsForSlot(String parentTicketId, int slotIndex, java.time.Instant now) { return 0; }
    /** AUD-012 凭证消费 CAS(仅 active→consumed,返回命中行数)。default 1=视为消费成功(无并发撤销语义的测试双子)。 */
    default int markTicketCredentialConsumed(String credentialId, java.time.Instant now) { return 1; }
    default TicketTransferRecord saveTicketTransfer(TicketTransferRecord record) { return record; }
    default Optional<TicketTransferRecord> findTicketTransferById(String id) { return Optional.empty(); }
    default Optional<TicketTransferRecord> findTicketTransferByClaimCode(String claimCode) { return Optional.empty(); }
    default int claimTicketTransfer(String id, String receiverCustomerId, java.time.Instant now) { return 0; }
    default int revokeTicketTransfer(String id, String initiatorCustomerId, java.time.Instant now) { return 0; }
    default int voidPendingTransfersForParent(String parentTicketId, String reason, java.time.Instant now) { return 0; }
    default boolean existsPendingTransferForSlot(String parentTicketId, int slotIndex) { return false; }
    default boolean existsPendingTransferForParent(String parentTicketId) { return false; }
    default List<TicketTransferRecord> listPendingTransfersByReceiverPhoneHash(String receiverPhoneHash) { return List.of(); }
    default List<TicketTransferRecord> listTicketTransfersByInitiator(String initiatorCustomerId) { return List.of(); }
    default List<TicketTransferRecord> listTicketTransfers() { return List.of(); }

    // ── ROOT-03-R29 渠道票与一体机票按已验证手机号归集、我的票使用记录、渠道接入状态；R21 注销删购票手机号 ──
    /** 该手机号已注册、未注销未关闭的会员编号（按 customers.phone_hash 盲索引）。default 无会员（测试双子）。 */
    default Optional<String> findMemberIdByPhone(String phone) { return Optional.empty(); }
    /**
     * 注册时把尚未归属会员、按该手机号购买的 salesChannels 渠道订单归到本人：订单 customer_id、父票购票人与持有人、各人员权益组持有人。
     * 返回归集的订单数。default 0（测试双子）。
     */
    default int linkUnclaimedOrdersToMember(String phone, String customerId, java.util.Set<String> salesChannels,
            java.time.Instant now) { return 0; }
    /** 一张票（权益）的使用记录：原核销记录（成功或已被冲正），新到旧。default 空（测试双子）。 */
    default List<CustomerTicketUsageResponse> listTicketUsages(String entitlementId) { return List.of(); }
    /** 第三方渠道售票能力开关（V071 integration.ticket_channel.sync 全局行）是否开启。default 关。 */
    default boolean ticketChannelSyncEnabled() { return false; }
    /** R21 注销：清空本人各订单的购票手机号与其盲索引，返回行数。default 0（测试双子）。 */
    default int clearBuyerPhonesOfCustomer(String customerId) { return 0; }
    /** R21 注销：本人当前持有的各人员权益组（含未核销）的权益。default 空（测试双子）。 */
    default List<IssuedEntitlementRecord> listHeldEntitlements(String customerId) {
        return listSlotHoldersByHolder(customerId).stream()
                .flatMap(holder -> listIssuedEntitlementsByParentSlot(holder.parentTicketId(), holder.slotIndex()).stream())
                .toList();
    }
}
