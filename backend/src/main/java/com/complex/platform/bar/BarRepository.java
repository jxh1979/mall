package com.complex.platform.bar;

// 文件版本：V0.06

import com.complex.platform.finance.PaymentIntentRecord;
import java.util.List;
import java.util.Optional;

public interface BarRepository {
    BarTableRecord saveTable(BarTableRecord table);

    /** BYOD 一期空位视图：正被有效预留占着的台位 id 集（ROOT-03-R19 按 hold_status='held'）。 */
    java.util.Set<String> listActiveReservedTableIds();

    /**
     * ROOT-03-R19（ENUM 第 100 行）：在台位行锁下占台——台位非空闲（已开台、待清台、停用）或已被其它订台占着时返回 false 不占；
     * 否则把该订台置为占台中。内存桩默认放行。
     */
    default boolean holdTable(String reservationId, String tableId) {
        return true;
    }

    /** ROOT-03-R19：下单前预检（不加锁）——台位空闲且没有被占着才可订。内存桩默认可订。 */
    default boolean isTableReservable(String tableId) {
        return true;
    }

    /**
     * ROOT-03-R19：取消、超时、到店开台三条路径退出占台。尚未占台（定金未付）的订台同样记为已退出，之后不再被当成未到店。内存桩默认无操作。
     */
    default void releaseHold(String reservationId) {
    }

    /** ROOT-03-R19（ENUM 第 100 行「排他锁」）：正占着该台位的那笔订台（一个台位同一时刻至多一笔）。内存桩默认没有。 */
    default Optional<String> findHeldReservationId(String tableId) {
        return Optional.empty();
    }

    /** ROOT-03-R19（BSM 第 1062 行）：超过最晚到店时间仍未到店的有效订台 id。内存桩默认无。 */
    default List<String> listOverdueReservationIds(java.time.Instant now) {
        return List.of();
    }

    /** OBS-13-03-R1：员工代订录入的客人手机号加密留存（只供订台短信，不进订台记录、不回显）。内存桩默认不存。 */
    default void saveReservationCustomerPhone(String reservationId, String phone) {
    }

    /** OBS-13-03-R1：员工代订留存的客人手机号（解密后）。内存桩默认没有。 */
    default Optional<String> findReservationCustomerPhone(String reservationId) {
        return Optional.empty();
    }

    // ── 第十八批 C 段第 15 条（BSM 第 1468 行）员工代订爽约与代订权限停用；只有 JDBC 实现，内存桩默认如下。

    /** 超时释放的员工代订记一笔爽约到经手员工名下（只写爽约时刻，不进订台记录）。内存桩默认无操作。 */
    default void markReservationNoShow(String reservationId, java.time.Instant at) {
    }

    /** 该酒吧员工代订爽约率上限（%）；未配置为空＝不自动停用。内存桩默认未配置。 */
    default Integer staffBookingNoShowLimitPct(String barId) {
        return null;
    }

    /** 该酒吧该经手员工最晚到店时间在 [from, to) 且已到（≤ now）的员工代订：{笔数, 其中爽约笔数}。内存桩默认无。 */
    default int[] staffBookingNoShowStats(String barId, String staffId, java.time.Instant from, java.time.Instant to, java.time.Instant now) {
        return new int[] {0, 0};
    }

    /** 该经手员工在该酒吧的代订权限是否停用中。内存桩默认否。 */
    default boolean hasActiveStaffBookingSuspension(String barId, String staffId) {
        return false;
    }

    default BarStaffBookingSuspensionRecord saveStaffBookingSuspension(BarStaffBookingSuspensionRecord suspension) {
        return suspension;
    }

    default Optional<BarStaffBookingSuspensionRecord> findStaffBookingSuspension(String id) {
        return Optional.empty();
    }

    /** 停用中的记录（商户范围为空＝平台视角全部）。 */
    default List<BarStaffBookingSuspensionRecord> listStaffBookingSuspensions(Optional<String> merchantScope) {
        return List.of();
    }

    // ── 第十八批 C 段第 17 条（BSM 第 1065 行；第十八批裁决 4）按杯出酒：吧台有无采集设备与本酒吧损耗率阈值。

    /** 按杯商品扣库库存行（物品＋库位）所挂库房是否为配有出酒量采集设备的吧台；查不到即未配。内存桩默认未配。 */
    default boolean pourDeviceEquipped(String itemId, String locationId) {
        return false;
    }

    /** 本酒吧损耗率阈值（%），未配置取出厂 5（BSM 第 2015 行）。内存桩默认出厂值。 */
    default int pourLossRatePct(String barId) {
        return com.complex.platform.governance.MerchantPercentParamRecord.Param.POUR_LOSS_RATE.factoryDefault();
    }

    List<BarTableRecord> listTables(Optional<String> merchantScope);

    Optional<BarTableRecord> findTableById(String id);

    /** R5 T15：按吧台解析一张台位用于赠饮 0 元单落 FK（无可靠台位来源时的兜底；无台位则跳过落单）。 */
    Optional<BarTableRecord> findFirstTableByBar(String barId);

    /** 处理清单 #8：设置 bar_tables.space_id（空间指派/回填），返回受影响行数。 */
    int assignTableSpace(String tableId, String spaceId);

    /** 处理清单 #8：解析空间所属商户（spaces.merchant_id），用于跨商户校验；空间不存在返回 empty。 */
    Optional<String> findSpaceMerchant(String spaceId);

    /** 处理清单 #8：列出 space_id 为空、待运营回填的卡座/桌台（按商户范围过滤）。 */
    List<BarTableSpaceBacklogRecord> listTablesWithoutSpace(Optional<String> merchantScope);

    BarReservationRecord saveReservation(BarReservationRecord reservation);

    List<BarReservationRecord> listReservations(Optional<String> merchantScope);

    /** V264：includeArchived=false（默认）排除已存档预订。默认委托给旧签名（内存桩无存档概念，返回全量）。 */
    default List<BarReservationRecord> listReservations(Optional<String> merchantScope, boolean includeArchived) {
        return listReservations(merchantScope);
    }

    Optional<BarReservationRecord> findReservationById(String id);

    /** V264：预订活引用计数（订单/账单/桌位调整/邀请函成员）；&gt;0 拒删。内存桩默认 0。 */
    default long countReservationReferences(String id) {
        return 0L;
    }

    /** V264：物理删除预订（子表 guest_checkins 随 CASCADE 清除）。内存桩默认无操作。 */
    default void deleteReservation(String id) {
    }

    /**
     * 卡座免通票入场名额核销前锁定 reservation 行，保证并发工作手机不会同时读到相同剩余额度。
     * 内存实现可退化为普通查询；JDBC 实现必须 SELECT ... FOR UPDATE。
     */
    default Optional<BarReservationRecord> findReservationByIdForUpdate(String id) {
        return findReservationById(id);
    }

    /** R5 T16：保存订台定金规则。 */
    default BarDepositRuleRecord saveDepositRule(BarDepositRuleRecord rule) {
        return rule;
    }

    /** R5 T16：按吧台列启用中的定金规则（scope_type 倒序，table_type/time_slot 优先于 bar）。 */
    default List<BarDepositRuleRecord> listDepositRules(String barId) {
        return List.of();
    }

    int countGuestCheckins(String reservationId);

    BarTableGuestCheckinRecord saveGuestCheckin(BarTableGuestCheckinRecord checkin);

    // ── V155 订卡邀请函（默认空实现，JDBC + 邀请函测试桩覆盖；其它内存桩无需改动）─────────────
    /** 按邀请函 token 反查预订（confirm/entry 校验入口）；未签发/不存在返回空。 */
    default Optional<BarReservationRecord> findReservationByInvitationToken(String token) {
        return Optional.empty();
    }

    /** upsert 邀请函成员（confirm 新增 / entry 标 entered）；幂等键 (reservation_id, member_identifier)。 */
    default BarReservationInvitationMemberRecord saveInvitationMember(BarReservationInvitationMemberRecord member) {
        return member;
    }

    /** 列邀请函成员（含持函人），供容量计数与门口核验。 */
    default List<BarReservationInvitationMemberRecord> listInvitationMembers(String reservationId) {
        return List.of();
    }

    BarProductRecord saveProduct(BarProductRecord product);

    List<BarProductRecord> listProducts(Optional<String> merchantScope);

    Optional<BarProductRecord> findProductById(String id);

    BarOrderRecord saveOrder(BarOrderRecord order);

    List<BarOrderRecord> listOrders(Optional<String> merchantScope);

    /** 订单列表内联账单摘要用：一次批量取回给定订单们的全部账单（created_at 倒序，最新在前）。 */
    List<BarBillRecord> listBillsByOrderIds(List<String> orderIds);

    Optional<BarOrderRecord> findOrderById(String id);

    BarOrderItemRecord saveOrderItem(BarOrderItemRecord item);

    /** 第十八批 K 段 97-R3（V580）：套餐下单展开出的明细记所属套餐，吧台消耗统计据此分出「套餐」；内存替身不记。 */
    default void markOrderItemsCombo(List<String> orderItemIds, String comboId) {
    }

    List<BarOrderItemRecord> listOrderItems(String orderId);

    Optional<BarOrderItemRecord> findOrderItemById(String id);

    /** V091 退菜退酒流水（区分 cancel/return + 库存回补/资金扣减留痕）。 */
    default BarOrderItemVoidRecord saveOrderItemVoid(BarOrderItemVoidRecord voidRecord) {
        return voidRecord;
    }

    default List<BarOrderItemVoidRecord> listOrderItemVoids(String orderId) {
        return List.of();
    }

    /** V345 退酒审批待审队列：按状态列出申请（商户 scope 非空则只看本商户）。 */
    default List<BarOrderItemVoidRecord> listOrderItemVoidsByStatus(Optional<String> merchantScope, String status) {
        return List.of();
    }

    /** V115：按 id 取单条退菜退酒流水（审批决策回写时定位待审记录）。 */
    default Optional<BarOrderItemVoidRecord> findOrderItemVoidById(String id) {
        return Optional.empty();
    }

    /**
     * V125 P1-1：审批决策时对该 void 流水行加 FOR UPDATE 行锁后再读，使 status==pending 守卫与状态翻转
     * 置于同一锁内事务——不同 idempotencyKey 的并发第二次 reject 会阻塞到首事务提交后读到已非 pending 被拒，
     * 杜绝库存/waiver_reversal 双扣。默认委托无锁查询，向后兼容内存/轻量实现。
     */
    default Optional<BarOrderItemVoidRecord> findOrderItemVoidByIdForUpdate(String id) {
        return findOrderItemVoidById(id);
    }

    BarProductionTicketRecord saveProductionTicket(BarProductionTicketRecord ticket);

    List<BarProductionTicketRecord> listProductionTickets(Optional<String> merchantScope);

    Optional<BarProductionTicketRecord> findProductionTicketById(String id);

    Optional<BarProductionTicketRecord> findProductionTicketByOrderId(String orderId);

    BarProductionTicketEventRecord saveProductionTicketEvent(BarProductionTicketEventRecord event);

    List<BarProductionTicketEventRecord> listProductionTicketEvents(String ticketId);

    /** R5 T15：赠饮兑换桥 upsert（幂等键 verification_id；首次插入返回 true，已存在返回 false）。 */
    boolean insertGiftRedemptionIfAbsent(BarGiftRedemptionRecord redemption);

    BarGiftRedemptionRecord saveGiftRedemption(BarGiftRedemptionRecord redemption);

    Optional<BarGiftRedemptionRecord> findGiftRedemptionByVerification(String verificationId);

    List<BarGiftRedemptionRecord> listGiftRedemptions(Optional<String> merchantScope);

    BarBillRecord saveBill(BarBillRecord bill);

    Optional<BarBillRecord> findBillById(String id);

    // 2c-2：行锁读取账单（settle/commission 序列化防 TOCTOU）。
    Optional<BarBillRecord> findBillByIdForUpdate(String id);

    // 2c-2：按 bill_id 查已有提成计算（计提查重守卫）。
    Optional<BarCommissionCalculationRecord> findCommissionByBillId(String billId);

    // G/V349：订单列表批量带出已落库提成（费率/金额），供后台提成页与订单页显示【真实落库值】而非页面写死的百分比。
    List<BarCommissionCalculationRecord> listCommissionsByBillIds(List<String> billIds);

    // 2c-3 reopen 冲正：把账单现有结算流水标 reversed（聚合排除，防反结→重结双计）。返回受影响行数。
    int markBillSettlementsReversed(String billId);

    BarBillAdjustmentRecord saveBillAdjustment(BarBillAdjustmentRecord adjustment);

    List<BarBillAdjustmentRecord> listBillAdjustments(String billId);

    BarBillOperationRecord saveBillOperation(BarBillOperationRecord operation);

    List<BarBillOperationRecord> listBillOperations(String billId);

    BarCommissionCalculationRecord saveCommissionCalculation(BarCommissionCalculationRecord calculation);

    BarTableSessionRecord saveTableSession(BarTableSessionRecord session);

    // 2c-5 卡座并发：DB 级乐观锁 CAS（WHERE session_version = expected）。返回 false=版本被并发改动。
    boolean updateTableSessionIfVersionMatches(BarTableSessionRecord session, long expectedVersion);

    List<BarTableSessionRecord> listTableSessions(Optional<String> merchantScope);

    Optional<BarTableSessionRecord> findTableSessionById(String id);

    BarTableSessionAdjustmentRecord saveTableSessionAdjustment(BarTableSessionAdjustmentRecord adjustment);

    List<BarTableSessionAdjustmentRecord> listTableSessionAdjustments(String sessionId);

    Optional<StockBalanceSnapshot> findStockBalance(String itemId, String locationId);

    boolean atomicDecreaseStock(String itemId, String locationId, int quantity);

    /** V091 退酒回补库存（原子加回）。 */
    default boolean atomicIncreaseStock(String itemId, String locationId, int quantity) {
        return false;
    }

    StockBalanceSnapshot saveStockBalance(StockBalanceSnapshot balance);

    StockMovementSnapshot saveStockMovement(StockMovementSnapshot movement);

    // ── 第十八批 I 段第 76 条（BSM:1065「按杯按标准出酒量扣库存、超出记损耗」；ENUM:79 库存按商品规格逐项进行）：按杯按容量扣库。

    /**
     * 按容量扣库存行：先用已开瓶余量，不够再开新瓶（开瓶数见 {@link BarPourLoss#bottlesToOpen}）——瓶数与可用各减开瓶数，
     * 余量＝余量＋开瓶数×规格容量−需量。返回开的新瓶数；瓶数不够开、库存行不存在或物料没登记规格容量返回空，库存不动。
     */
    Optional<Integer> decreaseStockByVolume(String itemId, String locationId, int volumeMl);

    /** 按杯退品回库：只把毫升加回已开瓶余量，瓶数不变；库存行不存在返回 false。 */
    boolean increaseOpenedVolume(String itemId, String locationId, int volumeMl);

    /** 库存物料的规格容量（毫升），没登记为空。 */
    Optional<Integer> findInventoryItemSpecVolumeMl(String itemId);

    default PaymentIntentRecord savePaymentIntent(PaymentIntentRecord paymentIntent) {
        return paymentIntent;
    }

    /** A2/A3：账单结算流水（现金/POS 收款留痕 + channel）。默认 no-op/空，JdbcBarRepository 覆盖。 */
    default BarBillSettlementRecord saveBillSettlement(BarBillSettlementRecord settlement) {
        return settlement;
    }

    default List<BarBillSettlementRecord> listBillSettlements(String billId) {
        return List.of();
    }

    // ── V095 交班 / 班结对账 ──────────────────────────────────────────────────
    /** 保存班次（开班 / 班结）。默认 no-op，JdbcBarRepository 覆盖。 */
    default BarShiftRecord saveShift(BarShiftRecord shift) {
        return shift;
    }

    default Optional<BarShiftRecord> findShiftById(String id) {
        return Optional.empty();
    }

    /** 同一吧台同一收银员当前未关闭的班次（防重复开班）。 */
    default Optional<BarShiftRecord> findOpenShift(String barId, String cashierUserId) {
        return Optional.empty();
    }

    default List<BarShiftRecord> listShifts(Optional<String> merchantScope) {
        return List.of();
    }

    /**
     * 班结时按班次时间窗口 [from, to) 聚合本吧台（可选本收银员）的系统应收：
     * 各支付方式结算流水 / 退款（refunded_at 落窗口）/ 小费（结算账单 tip）。只读聚合，不改账单。
     */
    default BarShiftReconciliationSnapshot aggregateShiftReconciliation(
            String barId,
            String cashierUserId,
            java.time.Instant from,
            java.time.Instant to,
            Optional<String> merchantScope
    ) {
        return BarShiftReconciliationSnapshot.empty();
    }

    /** V271 存档理由定向写入(reason 非空=存档,null=取消存档清空)。 */
    void setReservationArchiveReason(String id, String reason);

    // ── 第十批 TK-24/TK-25：台组（并台链）口径原语；求和逻辑统一在 BarMinSpend ─────────────────
    /**
     * 当前有效并台链：本会话 + 递归 status='merged' 子会话；includeTransferred=true 时连同整体转台前的旧链
     * （status='merged_transferred'，只算消费与留款、不算低消阈值）。默认只含自身（轻量桩）。
     */
    default List<String> familySessionIds(String sessionId, boolean includeTransferred) {
        return List.of(sessionId);
    }

    /** 开台期间锁定归属到这些会话的订单（JDBC：bar_order_service_sources）。默认空。 */
    default List<String> listOrderIdsBySessionIds(List<String> sessionIds) {
        return List.of();
    }

    /** 复核 money I2：该订台是否已挂出定金原路退款应付（ar_ap_items source_type=bar_deposit_refund）。默认 false。 */
    default boolean depositRefundPayableExists(String reservationId) {
        return false;
    }

    /** 复核 money B2：一笔账单级退款调整中商品部分的金额（有薪资退款分项按 goods 分项合计，无分项＝全额）。默认＝全额。 */
    default long refundGoodsCent(String adjustmentId, long amountCent) {
        return amountCent;
    }

    /** 订单落单时锁定的开台会话（JDBC：bar_order_service_sources.order_id）。默认空=无会话归属。 */
    default Optional<String> findSessionIdByOrderId(String orderId) {
        return Optional.empty();
    }

    // ── 第十批 TK-27-R3 拆台：订单归属校验与人工归属留痕 ─────────────────────────────────────
    /** 这些台位上自 since 起产生、但没有会话归属（无 bar_order_service_sources）且未取消的订单。默认空。 */
    default List<BarOrderRecord> listOrdersWithoutSession(List<String> tableIds, java.time.Instant since) {
        return List.of();
    }

    /** 人工明确订单归属到某会话（JDBC：写 bar_order_service_sources）。默认无操作。 */
    default void saveOrderServiceSource(String orderId, String sessionId) {
    }
}
