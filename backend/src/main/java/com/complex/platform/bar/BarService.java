package com.complex.platform.bar;

// 文件版本：V0.06

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.PageResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.requireText;

@Service
public class BarService {

    private final BarRepository repository;
    private final BarOperationExecutor operations;
    private final BarBillingService billingService;
    private final BarShiftService shiftService;
    private final BarTableSessionService tableSessionService;
    private final BarOrderService orderService;
    private final BarEntryService entryService;
    private final AuthorizationService authorizationService;

    public BarService(
            BarRepository repository,
            BarOperationExecutor operations,
            BarBillingService billingService,
            BarShiftService shiftService,
            BarTableSessionService tableSessionService,
            BarOrderService orderService,
            BarEntryService entryService,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.operations = operations;
        this.billingService = billingService;
        this.shiftService = shiftService;
        this.tableSessionService = tableSessionService;
        this.orderService = orderService;
        this.entryService = entryService;
        this.authorizationService = authorizationService;
    }


    public PageResponse<BarOrderView> listOrders() {
        return operations.readPage(
                BarReadOperation.LIST_ORDERS,
                () -> {
                    List<BarOrderRecord> orders = repository.listOrders(authorizationService.currentMerchantScope());
                    if (orders.isEmpty()) {
                        return List.of();
                    }
                    // 一次批量取回这些订单的账单并按订单分组为摘要（最新在前），避免 N+1；供前端账单列/结算/提成定位账单。
                    List<BarBillRecord> bills = repository
                            .listBillsByOrderIds(orders.stream().map(BarOrderRecord::id).toList());
                    // G/V349：再一次批量取回这些账单的提成台账，把【真实落库费率/提成额】随摘要带出——
                    // 后台提成页/订单页此前把 15%/10% 写死在页面文案里，配置中心改成 20% 后页面仍显示 15%，
                    // 对账时页面与台账对不上。改为页面只显示台账里真有的那个数（没计提就显示未计提）。
                    Map<String, BarCommissionCalculationRecord> commissionByBill = repository
                            .listCommissionsByBillIds(bills.stream().map(BarBillRecord::id).toList())
                            .stream()
                            .collect(Collectors.toMap(BarCommissionCalculationRecord::billId, c -> c, (first, dup) -> first));
                    Map<String, List<BarBillSummary>> billsByOrder = bills.stream()
                            .collect(Collectors.groupingBy(
                                    BarBillRecord::orderId,
                                    Collectors.mapping(
                                            bill -> {
                                                BarCommissionCalculationRecord commission = commissionByBill.get(bill.id());
                                                return new BarBillSummary(
                                                        bill.id(), bill.billNo(), bill.paymentStatus(),
                                                        bill.salesRoleType(),
                                                        commission == null ? null : commission.commissionRateBp(),
                                                        commission == null ? null : commission.commissionAmountCent());
                                            },
                                            Collectors.toList())));
                    return orders.stream()
                            .map(order -> BarOrderView.of(order, billsByOrder.getOrDefault(order.id(), List.of()),
                                    repository.listOrderItems(order.id())))
                            .toList();
                }
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.order.create")
    @Transactional
    public BarOrderRecord createOrder(BarOrderRequest request) {
        return operations.executeWrite(
                "createBarOrder",
                request == null ? null : request.idempotencyKey(),
                request,
                BarOrderRecord.class,
                BarWriteOperation.CREATE_ORDER,
                () -> orderService.createOrder(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.order.create")
    @Transactional
    public BarOrderRecord placeScanOrder(String sessionId, BarOrderRequest request) {
        String id = operations.requireText(sessionId, "session_id");
        return operations.executeWrite(
                "placeBarScanOrder",
                request == null ? null : request.idempotencyKey(),
                request,
                BarOrderRecord.class,
                BarWriteOperation.SCAN_ORDER,
                () -> orderService.placeScanOrder(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.order.create")
    @Transactional
    public BarBillRecord createOrderBill(String orderId, BarOrderBillCreateRequest request) {
        String id = operations.requireText(orderId, "order_id");
        return operations.executeWrite(
                "createBarOrderBill:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarBillRecord.class,
                BarWriteOperation.CREATE_ORDER_BILL,
                () -> billingService.createBill(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.order.create")
    @Transactional
    public BarOrderRecord deliverOrder(String orderId, BarOrderDeliveryRequest request) {
        String id = operations.requireText(orderId, "order_id");
        return operations.executeWrite(
                "deliverBarOrder:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarOrderRecord.class,
                BarWriteOperation.DELIVER_ORDER,
                () -> orderService.deliverOrder(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.order.create")
    @Transactional
    public BarOrderItemRecord addOrderItem(String orderId, BarOrderRequest request) {
        String id = operations.requireText(orderId, "order_id");
        return operations.executeWrite(
                "addBarOrderItem:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarOrderItemRecord.class,
                BarWriteOperation.ADD_ORDER_ITEM,
                () -> orderService.addOrderItem(id, request)
        );
    }

    /**
     * V345（BAR-VOID-A1）退酒审批队列：按状态列出退菜退酒申请；orderId 非空则只看该订单。
     * 商户 scope 由仓储层下沉为 SQL 谓词（跨商户不可见）。
     */
    public PageResponse<BarOrderItemVoidRecord> listOrderItemVoids(String orderId, String status) {
        String normalizedStatus = status == null || status.isBlank() ? "pending" : status.trim();
        String normalizedOrderId = orderId == null || orderId.isBlank() ? null : orderId.trim();
        return operations.readPage(
                BarReadOperation.LIST_ORDER_ITEM_VOIDS,
                () -> normalizedOrderId == null
                        ? repository.listOrderItemVoidsByStatus(
                                authorizationService.currentMerchantScope(), normalizedStatus)
                        : repository.listOrderItemVoids(normalizedOrderId).stream()
                                .filter(v -> normalizedStatus.equals(v.status()))
                                .filter(v -> authorizationService.currentMerchantScope()
                                        .map(scope -> scope.equals(v.merchantId())).orElse(true))
                                .toList()
        );
    }

    public PageResponse<BarProductionTicketRecord> listProductionTickets() {
        return operations.readPage(
                BarReadOperation.LIST_PRODUCTION_TICKETS,
                () -> repository.listProductionTickets(authorizationService.currentMerchantScope())
        );
    }

    public PageResponse<BarProductionTicketEventRecord> listProductionTicketEvents(String ticketId) {
        String id = operations.requireText(ticketId, "ticket_id");
        return operations.readPage(
                BarReadOperation.LIST_PRODUCTION_TICKET_EVENTS,
                () -> repository.listProductionTicketEvents(id)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.production_ticket.manage")
    @Transactional
    public BarProductionTicketRecord transitionProductionTicket(String ticketId, BarProductionTicketTransitionRequest request) {
        String id = operations.requireText(ticketId, "ticket_id");
        return operations.executeWrite(
                "transitionBarProductionTicket:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarProductionTicketRecord.class,
                BarWriteOperation.TRANSITION_PRODUCTION_TICKET,
                () -> orderService.transitionProductionTicket(id, request)
        );
    }

    public PageResponse<BarProductRecord> listProducts(boolean includeArchived) {
        return operations.readPage(
                BarReadOperation.LIST_PRODUCTS,
                // V262：列表默认排除已存档商品；includeArchived=true 才带出（内存过滤，商品目录量级小）。
                () -> repository.listProducts(authorizationService.currentMerchantScope()).stream()
                        .filter(p -> includeArchived || p.archivedAt() == null)
                        .toList()
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.order.create")
    @Transactional
    public BarProductRecord createProduct(BarProductRequest request) {
        return operations.executeWrite(
                "createBarProduct",
                request == null ? null : request.idempotencyKey(),
                request,
                BarProductRecord.class,
                BarWriteOperation.CREATE_PRODUCT,
                () -> orderService.createProduct(request)
        );
    }

    public PageResponse<BarReservationRecord> listReservations() {
        return listReservations(false);
    }

    /** V264：includeArchived=false（默认）排除已存档预订；true 供后台管理存档件。 */
    public PageResponse<BarReservationRecord> listReservations(boolean includeArchived) {
        return operations.readPage(
                BarReadOperation.LIST_RESERVATIONS,
                () -> repository.listReservations(authorizationService.currentMerchantScope(), includeArchived)
        );
    }

    // ================= V264 卡座预订：存档与审批后删除 =================

    /** 当前操作者 id（存档/删除留痕 + 自审校验）；无鉴权上下文（测试/内部）回退 null。 */
    private String reservationActorId() {
        try {
            return authorizationService.currentActor().id();
        } catch (RuntimeException unauthenticated) {
            return null;
        }
    }

    private BarReservationRecord requireReservationInScope(String id) {
        BarReservationRecord reservation = repository.findReservationById(operations.requireText(id, "reservation_id"))
                .orElseThrow(() -> com.complex.platform.common.api.ApiException.notFound(
                        "BAR_RESERVATION_NOT_FOUND", "订台记录不存在或已被删除"));
        authorizationService.currentMerchantScope().ifPresent(scope -> {
            if (!scope.equals(reservation.merchantId())) {
                throw com.complex.platform.common.api.ApiException.forbidden(
                        "BAR_MERCHANT_SCOPE", "该订台记录属于其他商户");
            }
        });
        return reservation;
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.reservation.manage")
    @Transactional
    public BarReservationRecord archiveReservation(String reservationId, boolean archive, String reason) {
        BarReservationRecord reservation = requireReservationInScope(reservationId);
        String r = archive ? requireText(reason, "reason") : null;
        java.time.Instant now = java.time.Instant.now();
        BarReservationRecord updated = archive
                ? reservation.withArchive(now, reservationActorId(), now)
                : reservation.withArchive(null, null, now).withDelete("none", null, null, null, now);
        BarReservationRecord saved = repository.saveReservation(updated);
        repository.setReservationArchiveReason(reservationId, r);
        return saved;
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.reservation.manage")
    @Transactional
    public BarReservationRecord requestDeleteReservation(String reservationId, BarCatalogDeleteRequest request) {
        return operations.auditedWrite(BarWriteOperation.REQUEST_DELETE_BAR_RESERVATION,
                () -> doRequestDeleteReservation(reservationId, request));
    }

    private BarReservationRecord doRequestDeleteReservation(String reservationId, BarCatalogDeleteRequest request) {
        BarReservationRecord reservation = requireReservationInScope(reservationId);
        if (reservation.archivedAt() == null) {
            throw com.complex.platform.common.api.ApiException.badRequest(
                    "BAR_RESERVATION_NOT_ARCHIVED", "订台记录须先存档才能申请删除");
        }
        if ("pending".equals(reservation.deleteStatus())) {
            throw com.complex.platform.common.api.ApiException.conflict(
                    "BAR_RESERVATION_DELETE_ALREADY_PENDING", "已有待审批的删除申请");
        }
        java.time.Instant now = java.time.Instant.now();
        String reason = requireText(request == null ? null : request.reason(), "reason");
        return repository.saveReservation(reservation.withDelete("pending", reservationActorId(), now,
                reason, now));
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.reservation.approve")
    @Transactional
    public BarCatalogDeleteDecision approveDeleteReservation(String reservationId, BarCatalogDeleteApprovalRequest request) {
        return operations.auditedWrite(BarWriteOperation.APPROVE_DELETE_BAR_RESERVATION,
                () -> doApproveDeleteReservation(reservationId, request));
    }

    private BarCatalogDeleteDecision doApproveDeleteReservation(String reservationId, BarCatalogDeleteApprovalRequest request) {
        BarReservationRecord reservation = requireReservationInScope(reservationId);
        if (!"pending".equals(reservation.deleteStatus())) {
            throw com.complex.platform.common.api.ApiException.badRequest(
                    "BAR_RESERVATION_DELETE_NOT_PENDING", "没有待审批的删除申请");
        }
        String decision = request == null || request.decision() == null
                ? null : request.decision().trim().toLowerCase(java.util.Locale.ROOT);
        if (!"approved".equals(decision) && !"rejected".equals(decision)) {
            throw com.complex.platform.common.api.ApiException.badRequest(
                    "VALIDATION_FAILED", "审批结论必须为通过（approved）或驳回（rejected）");
        }
        String actor = reservationActorId();
        if (actor != null && actor.equals(reservation.deleteRequestedBy())) {
            throw com.complex.platform.common.api.ApiException.forbidden(
                    "BAR_RESERVATION_SELF_APPROVE_FORBIDDEN", "删除申请的发起人不能审批自己的申请");
        }
        if ("rejected".equals(decision)) {
            repository.saveReservation(reservation.withDelete("none", null, null, null, java.time.Instant.now()));
            return new BarCatalogDeleteDecision(reservationId, "rejected", false);
        }
        long refs = repository.countReservationReferences(reservationId);
        if (refs > 0) {
            throw com.complex.platform.common.api.ApiException.conflict(
                    "BAR_RESERVATION_IN_USE", "该订台记录仍被 " + refs + " 条订单/账单/调整/邀请成员引用");
        }
        repository.deleteReservation(reservationId);
        return new BarCatalogDeleteDecision(reservationId, "approved", true);
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.reservation.create")
    @Transactional
    public BarReservationRecord createReservation(BarReservationRequest request) {
        return operations.executeWrite(
                "createBarReservation",
                request == null ? null : request.idempotencyKey(),
                request,
                BarReservationRecord.class,
                BarWriteOperation.CREATE_RESERVATION,
                () -> entryService.createReservation(request, null, "staff", "web")
        );
    }

    /** 第十八批 C 段第 15 条（BSM 第 1468 行）：代订权限停用中的经手员工（按商户范围）。 */
    public PageResponse<BarStaffBookingSuspensionRecord> listStaffBookingSuspensions() {
        return operations.readPage(
                BarReadOperation.LIST_STAFF_BOOKING_SUSPENSIONS,
                () -> repository.listStaffBookingSuspensions(authorizationService.currentMerchantScope())
        );
    }

    /** 第十八批 C 段第 15 条（BSM 第 1468 行「……自动停用其代订权限，由店长复核后恢复」）：店长复核恢复，只限本商户。 */
    @com.complex.platform.gate.GuardedAction(capability = "bar.reservation.manage")
    @Transactional
    public BarStaffBookingSuspensionRecord restoreStaffBookingSuspension(String suspensionId,
            BarStaffBookingSuspensionRecord.RestoreRequest request) {
        String id = operations.requireText(suspensionId, "suspension_id");
        return operations.executeWrite(
                "restoreBarStaffBooking:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarStaffBookingSuspensionRecord.class,
                BarWriteOperation.RESTORE_STAFF_BOOKING,
                () -> {
                    BarStaffBookingSuspensionRecord suspension = repository.findStaffBookingSuspension(id)
                            .orElseThrow(() -> com.complex.platform.common.api.ApiException.notFound(
                                    "BAR_STAFF_BOOKING_SUSPENSION_NOT_FOUND", "代订权限停用记录不存在"));
                    authorizationService.currentMerchantScope().ifPresent(scope -> {
                        if (!scope.equals(suspension.merchantId())) {
                            throw com.complex.platform.common.api.ApiException.forbidden(
                                    "CROSS_MERCHANT_FORBIDDEN", "该停用记录属于其他商户");
                        }
                    });
                    if (!"suspended".equals(suspension.status())) {
                        throw com.complex.platform.common.api.ApiException.conflict(
                                "BAR_STAFF_BOOKING_NOT_SUSPENDED", "该员工的代订权限不在停用中");
                    }
                    return repository.saveStaffBookingSuspension(
                            suspension.restored(authorizationService.currentActor().id(), java.time.Instant.now()));
                }
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.reservation.manage")
    @Transactional
    public BarReservationRecord payReservationDeposit(String reservationId, BarReservationDepositActionRequest request) {
        String id = operations.requireText(reservationId, "reservation_id");
        return operations.executeWrite(
                "payBarReservationDeposit:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarReservationRecord.class,
                BarWriteOperation.PAY_RESERVATION_DEPOSIT,
                () -> entryService.payDeposit(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.reservation.manage")
    @Transactional
    public BarReservationRecord checkInReservationDeposit(String reservationId, BarReservationDepositActionRequest request) {
        String id = operations.requireText(reservationId, "reservation_id");
        return operations.executeWrite(
                "checkInBarReservationDeposit:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarReservationRecord.class,
                BarWriteOperation.CHECK_IN_RESERVATION_DEPOSIT,
                () -> entryService.checkInDeposit(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.reservation.manage")
    @Transactional
    public BarReservationRecord cancelReservation(String reservationId, BarReservationCancelRequest request) {
        String id = operations.requireText(reservationId, "reservation_id");
        return operations.executeWrite(
                "cancelBarReservation:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarReservationRecord.class,
                BarWriteOperation.CANCEL_RESERVATION,
                () -> entryService.cancelReservation(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.deposit_rule.manage")
    @Transactional
    public BarDepositRuleRecord createDepositRule(BarDepositRuleRequest request) {
        return operations.executeWrite(
                "createBarDepositRule",
                request == null ? null : request.idempotencyKey(),
                request,
                BarDepositRuleRecord.class,
                BarWriteOperation.CREATE_DEPOSIT_RULE,
                () -> entryService.createDepositRule(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.table.guest_checkin")
    @Transactional
    public BarReservationEntryAllowanceVerificationRecord verifyReservationEntryAllowance(String reservationId, VerificationRequest request) {
        String id = operations.requireText(reservationId, "reservation_id");
        return operations.executeWrite(
                "verifyBarReservationEntryAllowance:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarReservationEntryAllowanceVerificationRecord.class,
                BarWriteOperation.VERIFY_RESERVATION_ENTRY_ALLOWANCE,
                () -> entryService.verifyGuestEntryAllowance(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.table.guest_checkin")
    @Transactional
    public BarTableGuestCheckinRecord checkInReservationGuest(String reservationId, VerificationRequest request) {
        String id = operations.requireText(reservationId, "reservation_id");
        return operations.executeWrite(
                "checkInBarReservationGuest:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarTableGuestCheckinRecord.class,
                BarWriteOperation.CHECK_IN_RESERVATION_GUEST,
                () -> entryService.checkInGuest(id, request)
        );
    }

    // ── V155 订卡邀请函 ───────────────────────────────────────────────────────
    @com.complex.platform.gate.GuardedAction(capability = "bar.reservation.invitation.manage")
    @Transactional
    public BarReservationInvitationRecord issueReservationInvitation(String reservationId, BarInvitationActionRequest request) {
        String id = operations.requireText(reservationId, "reservation_id");
        return operations.executeWrite(
                "issueBarReservationInvitation:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarReservationInvitationRecord.class,
                BarWriteOperation.ISSUE_INVITATION,
                () -> entryService.issueInvitation(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.reservation.invitation.manage")
    @Transactional
    public BarReservationInvitationRecord revokeReservationInvitation(String reservationId, BarInvitationActionRequest request) {
        String id = operations.requireText(reservationId, "reservation_id");
        return operations.executeWrite(
                "revokeBarReservationInvitation:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarReservationInvitationRecord.class,
                BarWriteOperation.REVOKE_INVITATION,
                () -> entryService.revokeInvitation(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.table.guest_checkin")
    @Transactional
    public BarReservationInvitationMemberRecord verifyReservationInvitationEntry(String reservationId, VerificationRequest request) {
        String id = operations.requireText(reservationId, "reservation_id");
        return operations.executeWrite(
                "verifyBarReservationInvitationEntry:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarReservationInvitationMemberRecord.class,
                BarWriteOperation.ENTER_INVITATION,
                () -> entryService.verifyInvitationEntry(id, request)
        );
    }

    /** 读：邀请函视图（含成员明细）；权限由控制器 requirePermission(bar.reservation.view) 把关。 */
    public BarReservationInvitationRecord getReservationInvitation(String reservationId) {
        return entryService.getInvitation(operations.requireText(reservationId, "reservation_id"));
    }

    /** BYOD 一期(2026-07-02):员工自有手机帮客人订位——空位视图=桌位+活跃预订(近4小时起,未取消)占用标记。 */
    public PageResponse<BarTableAvailabilityRecord> listTableAvailability() {
        return operations.readPage(
                BarReadOperation.LIST_TABLE_AVAILABILITY,
                () -> {
                    java.util.Set<String> reserved = repository.listActiveReservedTableIds();
                    return repository.listTables(authorizationService.currentMerchantScope()).stream()
                            .map(table -> new BarTableAvailabilityRecord(table.id(), table.barId(), table.tableCode(),
                                    table.tableName(), table.tableType(), table.capacity(), table.minConsumptionCent(),
                                    table.status(), reserved.contains(table.id())))
                            .toList();
                }
        );
    }

    public PageResponse<BarTableRecord> listTables() {
        return operations.readPage(
                BarReadOperation.LIST_TABLES,
                () -> repository.listTables(authorizationService.currentMerchantScope())
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.table.manage")
    @Transactional
    public BarTableRecord createTable(BarTableRequest request) {
        return operations.executeWrite(
                "createBarTable",
                request == null ? null : request.idempotencyKey(),
                request,
                BarTableRecord.class,
                BarWriteOperation.CREATE_TABLE,
                () -> tableSessionService.createTable(request)
        );
    }

    public PageResponse<BarTableSpaceBacklogRecord> listTablesWithoutSpace() {
        return operations.readPage(
                BarReadOperation.LIST_TABLES_WITHOUT_SPACE,
                tableSessionService::listTablesWithoutSpace
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.table.manage")
    @Transactional
    public BarTableSpaceAssignmentRecord assignTableSpace(String tableId, BarTableSpaceAssignmentRequest request) {
        String id = operations.requireText(tableId, "table_id");
        return operations.executeWrite(
                "assignBarTableSpace:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarTableSpaceAssignmentRecord.class,
                BarWriteOperation.ASSIGN_TABLE_SPACE,
                () -> tableSessionService.assignTableSpace(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.table.open_close")
    @Transactional
    public BarTableRecord openTable(String tableId, OpenBarTableRequest request) {
        String id = operations.requireText(tableId, "table_id");
        return operations.executeWrite(
                "openBarTable:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarTableRecord.class,
                BarWriteOperation.OPEN_TABLE,
                () -> tableSessionService.openTable(id, request)
        );
    }

    /** 第十批 TK-27-R2：清台登记恢复空闲（BSM L1058），与开台同权限码 bar.table.open_close。 */
    @com.complex.platform.gate.GuardedAction(capability = "bar.table.open_close")
    @Transactional
    public BarTableRecord clearTable(String tableId, BarTableClearRequest request) {
        String id = operations.requireText(tableId, "table_id");
        return operations.executeWrite(
                "clearBarTable:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarTableRecord.class,
                BarWriteOperation.CLEAR_TABLE,
                () -> tableSessionService.clearTable(id, request)
        );
    }

    public PageResponse<BarTableSessionRecord> listTableSessions() {
        return operations.readPage(
                BarReadOperation.LIST_TABLE_SESSIONS,
                () -> repository.listTableSessions(authorizationService.currentMerchantScope())
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.table_session.create")
    @Transactional
    public BarTableSessionRecord createTableSession(BarTableSessionRequest request) {
        return operations.executeWrite(
                "createBarTableSession",
                request == null ? null : request.idempotencyKey(),
                request,
                BarTableSessionRecord.class,
                BarWriteOperation.CREATE_TABLE_SESSION,
                () -> tableSessionService.createTableSession(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.table_session.transfer")
    @Transactional
    public BarTableSessionAdjustmentRecord transferTableSession(String sessionId, BarTableTransferRequest request) {
        String id = operations.requireText(sessionId, "session_id");
        return operations.executeWrite(
                "transferBarTableSession:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarTableSessionAdjustmentRecord.class,
                BarWriteOperation.TRANSFER_TABLE_SESSION,
                () -> tableSessionService.transferTableSession(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.table_session.merge")
    @Transactional
    public BarTableSessionAdjustmentRecord mergeTableSessions(String sessionId, BarTableMergeRequest request) {
        String id = operations.requireText(sessionId, "session_id");
        return operations.executeWrite(
                "mergeBarTableSession:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarTableSessionAdjustmentRecord.class,
                BarWriteOperation.MERGE_TABLE_SESSIONS,
                () -> tableSessionService.mergeTableSessions(id, request)
        );
    }

    /** 第十批 TK-27-R3：拆台（BSM L1079「由授权岗位操作」），整笔事务一次性成功或整体失败（DEC-R8-95）。 */
    @com.complex.platform.gate.GuardedAction(capability = "bar.table_session.split")
    @Transactional
    public BarTableSessionAdjustmentRecord splitTableSessions(String sessionId, BarTableSplitRequest request) {
        String id = operations.requireText(sessionId, "session_id");
        return operations.executeWrite(
                "splitBarTableSession:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarTableSessionAdjustmentRecord.class,
                BarWriteOperation.SPLIT_TABLE_SESSIONS,
                () -> tableSessionService.splitTableSessions(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.table_session.renew")
    @Transactional
    public BarTableSessionAdjustmentRecord renewTableSession(String sessionId, BarTableRenewRequest request) {
        String id = operations.requireText(sessionId, "session_id");
        return operations.executeWrite(
                "renewBarTableSession:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarTableSessionAdjustmentRecord.class,
                BarWriteOperation.RENEW_TABLE_SESSION,
                () -> tableSessionService.renewTableSession(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.payment_intent.create")
    @Transactional
    public PaymentIntentRecord createBillPayIntent(String billId, PaymentIntentRequest request) {
        String id = operations.requireText(billId, "bill_id");
        return operations.executeWrite(
                "createBarBillPayIntent:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                PaymentIntentRecord.class,
                BarWriteOperation.CREATE_BILL_PAY_INTENT,
                () -> billingService.createBillPayIntent(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.payment.register")
    public void requestStoredValueCode(String billId, String phone) {
        billingService.requestStoredValueCode(operations.requireText(billId, "bill_id"), phone);
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.payment.register")
    @Transactional
    public BarBillRecord settleBill(String billId, BarBillSettleRequest request) {
        String id = operations.requireText(billId, "bill_id");
        return operations.executeWrite(
                "settleBarBill:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarBillRecord.class,
                BarWriteOperation.SETTLE_BILL,
                () -> billingService.settleBill(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.bill.adjust", admissionAction = "bar.bill_waiver", objectType = "bar_bill")
    @Transactional
    public BarBillAdjustmentRecord applyBillAdjustment(String billId, BarBillAdjustmentRequest request) {
        String id = operations.requireText(billId, "bill_id");
        return operations.executeWrite(
                "adjustBarBill:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarBillAdjustmentRecord.class,
                BarWriteOperation.APPLY_BILL_ADJUSTMENT,
                () -> billingService.applyBillAdjustment(id, request)
        );
    }

    public PageResponse<BarBillOperationRecord> listBillOperations(String billId) {
        String id = operations.requireText(billId, "bill_id");
        return operations.readPage(
                BarReadOperation.LIST_BILL_OPERATIONS,
                () -> repository.listBillOperations(id)
        );
    }

    /** 第十一批 TK-04-R5：账单端查看本单商户消费奖励（积分、成长值结果与状态）。 */
    public PageResponse<com.complex.platform.customer.MerchantConsumptionRewardService.RewardView> listBillRewards(String billId) {
        String id = operations.requireText(billId, "bill_id");
        return operations.readPage(BarReadOperation.LIST_BILL_REWARDS, () -> billingService.listBillRewards(id));
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.bill.split_merge")
    @Transactional
    public BarBillOperationRecord splitBill(String billId, BarBillSplitRequest request) {
        String id = operations.requireText(billId, "bill_id");
        return operations.executeWrite(
                "splitBarBill:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarBillOperationRecord.class,
                BarWriteOperation.SPLIT_BILL,
                () -> billingService.splitBill(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.bill.split_merge")
    @Transactional
    public BarBillOperationRecord mergeBills(String billId, BarBillMergeRequest request) {
        String id = operations.requireText(billId, "bill_id");
        return operations.executeWrite(
                "mergeBarBill:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarBillOperationRecord.class,
                BarWriteOperation.MERGE_BILLS,
                () -> billingService.mergeBills(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.order_item.void", admissionAction = "bar.order_item_void", objectType = "bar_order_item")
    @Transactional
    public BarOrderItemVoidRecord voidOrderItem(String orderId, BarOrderItemVoidRequest request) {
        String id = operations.requireText(orderId, "order_id");
        return operations.executeWrite(
                "voidBarOrderItem:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarOrderItemVoidRecord.class,
                BarWriteOperation.VOID_ORDER_ITEM,
                () -> orderService.voidOrderItem(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(permission = "bar.order_item.void_decide")
    @Transactional
    public BarOrderItemVoidRecord decideOrderItemVoid(String voidId, BarOrderItemVoidDecisionRequest request) {
        String id = operations.requireText(voidId, "void_id");
        return operations.executeWrite(
                "decideBarOrderItemVoid:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarOrderItemVoidRecord.class,
                BarWriteOperation.DECIDE_ORDER_ITEM_VOID,
                () -> orderService.decideOrderItemVoid(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.bill.reopen", admissionAction = "bar.bill_reopen", objectType = "bar_bill")
    @Transactional
    public BarBillOperationRecord reopenBill(String billId, BarBillReopenRequest request) {
        String id = operations.requireText(billId, "bill_id");
        return operations.executeWrite(
                "reopenBarBill:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarBillOperationRecord.class,
                BarWriteOperation.REOPEN_BILL,
                () -> billingService.reopenBill(id, request)
        );
    }

    // 第十六批第 16 条（ENUM「异常状态的统一出口」）：字段注入免改构造点（单测未注入＝端点不可用）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    BarBillAnomalyService anomalyService;

    @com.complex.platform.gate.GuardedAction(capability = "bar.bill.adjust")
    @Transactional
    public BarBillRecord raiseBillAnomaly(String billId, BarBillAnomalyRequest request) {
        String id = operations.requireText(billId, "bill_id");
        return operations.executeWrite(
                "raiseBarBillAnomaly:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarBillRecord.class,
                BarWriteOperation.RAISE_BILL_ANOMALY,
                Map.of("reason", String.valueOf(request == null ? null : request.reason())),
                () -> anomalyService.raise(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.bill.anomaly.resolve")
    @Transactional
    public BarBillRecord resolveBillAnomaly(String billId, BarBillAnomalyResolveRequest request) {
        String id = operations.requireText(billId, "bill_id");
        return operations.executeWrite(
                "resolveBarBillAnomaly:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarBillRecord.class,
                BarWriteOperation.RESOLVE_BILL_ANOMALY,
                Map.of("resolution", String.valueOf(request == null ? null : request.resolution()),
                        "reason", String.valueOf(request == null ? null : request.reason())),
                () -> anomalyService.resolve(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.product.sold_out")
    @Transactional
    public BarProductRecord markProductSoldOut(String productId, BarProductSoldOutRequest request) {
        String id = operations.requireText(productId, "product_id");
        return operations.executeWrite(
                "markBarProductSoldOut:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarProductRecord.class,
                BarWriteOperation.MARK_PRODUCT_SOLD_OUT,
                () -> orderService.markSoldOut(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.bill.tip")
    @Transactional
    public BarBillRecord addBillTip(String billId, BarBillTipRequest request) {
        String id = operations.requireText(billId, "bill_id");
        return operations.executeWrite(
                "addBarBillTip:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarBillRecord.class,
                BarWriteOperation.ADD_BILL_TIP,
                () -> billingService.addTip(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.table_commission.calculate")
    @Transactional
    public BarCommissionCalculationRecord calculateCommission(String billId, CommissionCalculateRequest request) {
        String id = operations.requireText(billId, "bill_id");
        return operations.executeWrite(
                "calculateBarTableCommission:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarCommissionCalculationRecord.class,
                BarWriteOperation.CALCULATE_COMMISSION,
                () -> billingService.calculateCommission(id, request)
        );
    }

    // ── V095 交班 / 班结对账 ──────────────────────────────────────────────────
    public PageResponse<BarShiftRecord> listShifts() {
        return operations.readPage(
                BarReadOperation.LIST_SHIFTS,
                () -> repository.listShifts(authorizationService.currentMerchantScope())
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.shift.manage")
    @Transactional
    public BarShiftRecord openShift(BarShiftOpenRequest request) {
        return operations.executeWrite(
                "openBarShift",
                request == null ? null : request.idempotencyKey(),
                request,
                BarShiftRecord.class,
                BarWriteOperation.OPEN_SHIFT,
                () -> shiftService.openShift(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.shift.close", admissionAction = "bar.shift_close", objectType = "bar_shift")
    @Transactional
    public BarShiftRecord closeShift(String shiftId, BarShiftCloseRequest request) {
        String id = operations.requireText(shiftId, "shift_id");
        return operations.executeWrite(
                "closeBarShift:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                BarShiftRecord.class,
                BarWriteOperation.CLOSE_SHIFT,
                () -> shiftService.closeShift(id, request)
        );
    }
}
