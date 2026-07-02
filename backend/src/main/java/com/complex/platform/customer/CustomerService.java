package com.complex.platform.customer;

import com.complex.platform.bar.BarDepositRuleRecord;
import com.complex.platform.bar.BarOrderRecord;
import com.complex.platform.bar.BarOrderRequest;
import com.complex.platform.bar.BarOrderService;
import com.complex.platform.bar.BarProductRecord;
import com.complex.platform.bar.BarRepository;
import com.complex.platform.bar.BarReservationInvitationMemberRecord;
import com.complex.platform.bar.BarReservationInvitationRecord;
import com.complex.platform.bar.BarReservationRecord;
import com.complex.platform.bar.BarTableRecord;
import com.complex.platform.bar.BarTableSessionRecord;
import com.complex.platform.customer.dto.CustomerBarInvitationConfirmRequest;
import com.complex.platform.customer.dto.CustomerBarMenuItemResponse;
import com.complex.platform.customer.dto.CustomerBarScanOrderRequest;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.customer.dto.CustomerBarReservationRequest;
import com.complex.platform.customer.dto.CustomerCheckInRequest;
import com.complex.platform.customer.dto.CustomerConsentRequest;
import com.complex.platform.customer.dto.CustomerPhoneLoginRequest;
import com.complex.platform.customer.dto.CustomerPointsRedemptionRequest;
import com.complex.platform.customer.dto.CustomerTicketOrderRequest;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.inventory.InventoryService;
import com.complex.platform.inventory.dto.StockMovementRequest;
import com.complex.platform.ticketing.TicketCredentialResponse;
import com.complex.platform.ticketing.TicketOrderRecord;
import com.complex.platform.ticketing.TicketTypeRecord;
import com.complex.platform.ticketing.TicketingService;
import com.complex.platform.ticketing.dto.TicketOrderRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;

    private final CustomerRepository repository;
    private final CustomerOtpVerifier otpVerifier;
    private final TicketingService ticketingService;
    private final AuditedOperationExecutor operations;
    private final AuditEventService auditEventService;
    private final InventoryService inventoryService;
    private final LoyaltyService loyaltyService;
    private final BarRepository barRepository;
    // 扫码点单跨域调入：客人自助下单复用 bar 下单逻辑（merchant/customer 取自会话），见 placeBarScanOrder。
    private final BarOrderService barOrderService;

    private final java.time.Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public CustomerService(
            CustomerRepository repository,
            CustomerOtpVerifier otpVerifier,
            TicketingService ticketingService,
            AuditedOperationExecutor operations,
            AuditEventService auditEventService,
            InventoryService inventoryService,
            LoyaltyService loyaltyService,
            BarRepository barRepository,
            BarOrderService barOrderService
    ) {
        this(repository, otpVerifier, ticketingService, operations, auditEventService,
                inventoryService, loyaltyService, barRepository, barOrderService, java.time.Clock.systemUTC());
    }

    /** 4.3 clock 化：注入 Clock 使可用期等时间比较可测（供单测传 fixed clock）。 */
    CustomerService(
            CustomerRepository repository,
            CustomerOtpVerifier otpVerifier,
            TicketingService ticketingService,
            AuditedOperationExecutor operations,
            AuditEventService auditEventService,
            InventoryService inventoryService,
            LoyaltyService loyaltyService,
            BarRepository barRepository,
            BarOrderService barOrderService,
            java.time.Clock clock
    ) {
        this.clock = clock;
        this.repository = repository;
        this.otpVerifier = otpVerifier;
        this.ticketingService = ticketingService;
        this.operations = operations;
        this.auditEventService = auditEventService;
        this.inventoryService = inventoryService;
        this.loyaltyService = loyaltyService;
        this.barRepository = barRepository;
        this.barOrderService = barOrderService;
    }

    @Transactional
    public CustomerLoginResponse phoneLogin(CustomerPhoneLoginRequest request) {
        try {
            CustomerPhoneLoginRequest actual = requireRequest(request);
            String phone = requireText(actual.phone(), "phone");
            String smsCode = requireText(actual.smsCode(), "sms_code");
            otpVerifier.verify(phone, smsCode, actual.scene());
            Instant now = Instant.now();
            CustomerRecord customer = repository.findCustomerByPhone(phone)
                    .map(existing -> {
                        // V194 需求：登录须先过账号状态闸——黑名单/注销/未到期封禁拒登录；封禁到期则自动恢复。
                        CustomerRecord usable = enforceUsableAccount(existing, clock.instant());
                        return repository.saveCustomer(new CustomerRecord(
                                usable.id(),
                                usable.phone(),
                                usable.maskedPhone(),
                                "active",
                                usable.birthDate(),
                                now,
                                usable.createdAt(),
                                now
                        ));
                    })
                    .orElseGet(() -> repository.saveCustomer(new CustomerRecord(
                            // PII: 主键不得由手机号派生, 否则从 id 即可反推手机号, 架空 phone 列的 AES-GCM 加密. 用随机不可反推 id.
                            "customer_" + UUID.randomUUID().toString().replace("-", ""),
                            phone,
                            maskPhone(phone),
                            "active",
                            null,
                            now,
                            now,
                            now
                    )));
            CustomerSessionRecord session = repository.saveSession(new CustomerSessionRecord(
                    "session_" + UUID.randomUUID().toString().replace("-", ""),
                    customer.id(),
                    "cust_" + UUID.randomUUID().toString().replace("-", ""),
                    trimToDefault(actual.scene(), "miniapp"),
                    now,
                    now.plusSeconds(86_400),
                    "active"
            ));
            auditEventService.recordSuccess(
                    "customer.auth.login",
                    "public",
                    "customer",
                    customer.id(),
                    null,
                    null,
                    Map.of("scene", session.scene())
            );
            return new CustomerLoginResponse(customer.id(), customer.maskedPhone(), session.customerToken(), session.expiresAt());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "customer.auth.login_failed",
                    "public",
                    "customer",
                    null,
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    public PageResponse<CustomerTicketProductResponse> listTicketProducts() {
        return readPage(
                "customer.ticket_products_viewed",
                "customer.ticket_products_view_failed",
                "public",
                "ticket_product",
                () -> ticketingService.listTicketTypes().items().stream()
                        .filter(ticketType -> "published".equals(ticketType.status()) || "active".equals(ticketType.status()))
                        .map(this::toTicketProduct)
                        .toList()
        );
    }

    @Transactional
    public TicketOrderRecord createTicketOrder(
            String customerToken,
            String idempotencyKey,
            CustomerTicketOrderRequest request
    ) {
        CustomerRecord customer = requireCustomer(customerToken);
        CustomerTicketOrderRequest actual = requireRequest(request);
        String key = firstNonBlank(idempotencyKey, actual.idempotencyKey());
        return executeWrite(
                "createCustomerTicketOrder:" + customer.id(),
                key,
                actual,
                TicketOrderRecord.class,
                "customer.ticket_order.created",
                "customer.ticket_order.create_failed",
                "not_applicable",
                "ticket_order",
                () -> ticketingService.createTicketOrder(new TicketOrderRequest(
                        key,
                        actual.buyerNote(),
                        List.of(),
                        actual.ticketTypeId(),
                        actual.priceRuleId(),
                        actual.quantity(),
                        actual.visitDate(),
                        "miniapp",
                        actual.contactPhone(),
                        customer.id()
                ))
        );
    }

    public PageResponse<TicketOrderRecord> listOrders(String customerToken) {
        CustomerRecord customer = requireCustomer(customerToken);
        return readPage(
                "customer.orders_viewed",
                "customer.orders_view_failed",
                "not_applicable",
                "ticket_order",
                () -> ticketingService.listTicketOrdersByCustomerId(customer.id())
        );
    }

    /** 1.8 我的票：订单内票实例列表（先过账号状态闸与订单归属校验，无需手输票 id）。 */
    public PageResponse<com.complex.platform.ticketing.CustomerTicketResponse> listOrderTickets(String customerToken, String orderId) {
        CustomerRecord customer = requireCustomer(customerToken);
        return readPage(
                "customer.order_tickets_viewed",
                "customer.order_tickets_view_failed",
                "not_applicable",
                "issued_entitlement",
                () -> ticketingService.listCustomerOrderTickets(orderId, customer.id())
        );
    }

    public TicketCredentialResponse getTicketQr(String customerToken, String ticketId) {
        CustomerRecord customer = requireCustomer(customerToken);
        return ticketingService.getCustomerTicketCredential(ticketId, customer.id());
    }

    @Transactional
    public CustomerConsentRecord recordConsent(String customerToken, String idempotencyKey, CustomerConsentRequest request) {
        CustomerRecord customer = requireCustomer(customerToken);
        CustomerConsentRequest actual = requireRequest(request);
        String key = firstNonBlank(idempotencyKey, actual.idempotencyKey());
        return executeWrite(
                "recordCustomerConsent:" + customer.id(),
                key,
                actual,
                CustomerConsentRecord.class,
                "customer.consent.created",
                "customer.consent.create_failed",
                "not_applicable",
                "customer_consent",
                () -> {
                    Boolean accepted = actual.accepted();
                    if (accepted == null) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "accepted is required");
                    }
                    Instant now = Instant.now();
                    return repository.saveConsent(new CustomerConsentRecord(
                            "consent_" + slug(key),
                            customer.id(),
                            requireText(actual.consentType(), "consent_type"),
                            accepted,
                            trimToNull(actual.version()),
                            trimToNull(actual.sourceTerminal()),
                            accepted ? now : null,
                            accepted ? null : now,
                            now,
                            now
                    ));
                }
        );
    }

    public CustomerPointsResponse getPoints(String customerToken) {
        CustomerRecord customer = requireCustomer(customerToken);
        try {
            CustomerPointsAccountRecord account = repository.findPointsByCustomerId(customer.id())
                    .orElseGet(() -> repository.savePoints(newPointsAccount(customer.id(), Instant.now())));
            auditEventService.recordSuccess(
                    "customer.points_viewed",
                    "not_applicable",
                    "customer_points",
                    customer.id(),
                    null,
                    null,
                    Map.of()
            );
            return new CustomerPointsResponse(
                    account.customerId(),
                    account.balance(),
                    account.level(),
                    account.recentSummary()
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "customer.points_view_failed",
                    "not_applicable",
                    "customer_points",
                    customer.id(),
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    public PageResponse<CustomerPointsLedgerRecord> listPointsLedger(String customerToken) {
        CustomerRecord customer = requireCustomer(customerToken);
        return readPage(
                "customer.points_ledger_viewed",
                "customer.points_ledger_view_failed",
                "not_applicable",
                "customer_points_ledger",
                () -> repository.listPointsLedgerByCustomerId(customer.id())
        );
    }

    @Transactional
    public CustomerCheckInRecord checkIn(String customerToken, CustomerCheckInRequest request) {
        CustomerRecord customer = requireCustomer(customerToken);
        String terminal = request == null ? null : trimToNull(request.sourceTerminal());
        try {
            CustomerCheckInRecord checkIn = loyaltyService.checkIn(customer.id(), trimToDefault(terminal, "miniapp"));
            auditEventService.recordSuccess(
                    "customer.checked_in",
                    "not_applicable",
                    "customer_check_in",
                    customer.id(),
                    null,
                    null,
                    Map.of("streak", checkIn.streak(), "awarded_points", checkIn.awardedPoints())
            );
            return checkIn;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "customer.check_in_failed",
                    "not_applicable",
                    "customer_check_in",
                    customer.id(),
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    public PageResponse<CustomerCheckInRecord> listCheckIns(String customerToken) {
        CustomerRecord customer = requireCustomer(customerToken);
        return readPage(
                "customer.check_ins_viewed",
                "customer.check_ins_view_failed",
                "not_applicable",
                "customer_check_in",
                () -> repository.listCheckIns(customer.id())
        );
    }

    public PageResponse<CustomerRedemptionCatalogItemRecord> listRedemptionItems(String customerToken) {
        requireCustomer(customerToken);
        return readPage(
                "customer.points_redemption_items_viewed",
                "customer.points_redemption_items_view_failed",
                "not_applicable",
                "customer_points_redemption_item",
                () -> repository.listActiveRedemptionCatalogItems(clock.instant())
        );
    }

    public PageResponse<CustomerPointsRedemptionRecord> listRedemptions(String customerToken) {
        CustomerRecord customer = requireCustomer(customerToken);
        return readPage(
                "customer.points_redemptions_viewed",
                "customer.points_redemptions_view_failed",
                "not_applicable",
                "customer_points_redemption",
                () -> repository.listPointsRedemptionsByCustomerId(customer.id())
        );
    }

    @Transactional
    public CustomerPointsRedemptionRecord redeemPoints(
            String customerToken,
            String idempotencyKey,
            CustomerPointsRedemptionRequest request
    ) {
        CustomerRecord customer = requireCustomer(customerToken);
        CustomerPointsRedemptionRequest actual = requireRequest(request);
        String key = firstNonBlank(idempotencyKey, actual.idempotencyKey());
        return executeWrite(
                "redeemCustomerPoints:" + customer.id(),
                key,
                actual,
                CustomerPointsRedemptionRecord.class,
                "customer.points_redeemed",
                "customer.points_redeem_failed",
                "not_applicable",
                "customer_points_redemption",
                () -> {
                    Instant now = Instant.now();
                    CustomerRedemptionCatalogItemRecord item = resolveActiveRedemptionItem(
                            requireText(actual.catalogItemId(), "catalog_item_id"), now);
                    int quantity = actual.quantity() == null ? 1 : actual.quantity();
                    if (quantity <= 0) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "quantity must be positive");
                    }
                    long totalPoints = item.pointsCost() * quantity;
                    CustomerPointsAccountRecord account = loadOrCreatePointsAccountForUpdate(customer.id());
                    if (account.balance() < totalPoints) {
                        throw ApiException.conflict("POINTS_INSUFFICIENT", "points balance is insufficient");
                    }
                    String redemptionId = "redemption_" + slug(key);
                    // R3 P0-7：兑换扣库存使用 redeem_consume 类型移动（替代此前误用的 "loss" 调整），
                    // 从礼物绑定的库位扣减；语义正确、可与损耗/报废区分对账。
                    inventoryService.createTypedStockMovement("redeem_consume", new StockMovementRequest(
                            "points-redemption-" + key,
                            "customer points redemption " + redemptionId,
                            List.of(),
                            item.stockLocationId(),
                            null,
                            List.of(Map.of(
                                    "item_id", item.inventoryItemId(),
                                    "quantity", item.stockQuantity() * quantity
                            )),
                            "points_redemption:" + redemptionId
                    ));
                    if ("frozen".equals(account.accountStatus())) {
                        throw ApiException.conflict("POINTS_ACCOUNT_FROZEN", "points account is frozen");
                    }
                    long balanceAfter = account.balance() - totalPoints;
                    // FIFO 消耗获取批次剩余，使过期清零只清未用部分。
                    consumeFifoBatches(customer.id(), totalPoints);
                    repository.savePoints(new CustomerPointsAccountRecord(
                            account.id(),
                            account.customerId(),
                            balanceAfter,
                            account.level(),
                            Map.of(
                                    "last_redemption_id", redemptionId,
                                    "last_redeemed_points", totalPoints
                            ),
                            account.frozenPoints(),
                            account.accountStatus(),
                            account.levelPeriodStart(),
                            account.keepThresholdProgress(),
                            account.graceUntil(),
                            account.lastCheckInDate(),
                            account.checkInStreak(),
                            account.createdAt(),
                            now
                    ));
                    repository.savePointsLedger(new CustomerPointsLedgerRecord(
                            "ledger_" + slug(key),
                            customer.id(),
                            -totalPoints,
                            balanceAfter,
                            "points_redemption",
                            redemptionId,
                            "Redeemed " + item.itemName(),
                            null,
                            null,
                            false,
                            now
                    ));
                    return repository.savePointsRedemption(new CustomerPointsRedemptionRecord(
                            redemptionId,
                            customer.id(),
                            item.id(),
                            item.itemName(),
                            quantity,
                            item.pointsCost(),
                            totalPoints,
                            item.inventoryItemId(),
                            item.stockLocationId(),
                            item.stockQuantity() * quantity,
                            trimToDefault(actual.sourceTerminal(), "miniapp"),
                            "pending_pickup",
                            now,
                            now
                    ));
                }
        );
    }

    /**
     * 兑换/扣减时按 FIFO 消耗获取批次的剩余积分。仅用于使后续按 expire_at 的过期清零只清"未使用"部分；
     * 不再持有 expire_at 的旧余额不影响（remaining_points 为空的批次跳过）。
     */
    private void consumeFifoBatches(String customerId, long amount) {
        long remaining = amount;
        // 取该客所有"尚有 remaining_points 的获取批次"按时间升序（含未到期），FIFO 扣减。
        List<CustomerPointsLedgerRecord> batches = repository.listOpenBatches(customerId);
        for (CustomerPointsLedgerRecord batch : batches) {
            if (remaining <= 0) {
                break;
            }
            long batchRemaining = batch.remainingPoints() == null ? 0 : batch.remainingPoints();
            if (batchRemaining <= 0) {
                continue;
            }
            long consumed = Math.min(batchRemaining, remaining);
            repository.consumeBatchRemaining(batch.id(), batchRemaining - consumed);
            remaining -= consumed;
        }
    }

    private CustomerRedemptionCatalogItemRecord resolveActiveRedemptionItem(String catalogItemId, Instant now) {
        return repository.findRedemptionCatalogItemById(catalogItemId)
                .filter(candidate -> redemptionItemIsActive(candidate, now))
                .orElseThrow(() -> ApiException.notFound(
                        "REDEMPTION_ITEM_NOT_AVAILABLE",
                        "redemption item is not available"
                ));
    }

    private CustomerPointsAccountRecord loadOrCreatePointsAccountForUpdate(String customerId) {
        return repository.findPointsByCustomerIdForUpdate(customerId)
                .orElseGet(() -> repository.savePoints(newPointsAccount(customerId, Instant.now())));
    }

    static CustomerPointsAccountRecord newPointsAccount(String customerId, Instant now) {
        return new CustomerPointsAccountRecord(
                "points_" + customerId,
                customerId,
                0L,
                "base",
                Map.of(),
                0L,
                "active",
                now,
                0L,
                null,
                null,
                0,
                now,
                now
        );
    }

    public PageResponse<CustomerBarReservationOptionResponse> listBarReservationOptions(String customerToken) {
        requireCustomer(customerToken);
        return readPage(
                "customer.bar_reservation_options_viewed",
                "customer.bar_reservation_options_view_failed",
                "not_applicable",
                "bar_reservation_option",
                repository::listBarReservationOptions
        );
    }

    @Transactional
    public BarReservationRecord createBarReservation(
            String customerToken,
            String idempotencyKey,
            CustomerBarReservationRequest request
    ) {
        CustomerRecord customer = requireCustomer(customerToken);
        CustomerBarReservationRequest actual = requireRequest(request);
        String key = firstNonBlank(idempotencyKey, actual.idempotencyKey());
        return executeWrite(
                "createCustomerBarReservation:" + customer.id(),
                key,
                actual,
                BarReservationRecord.class,
                "customer.bar_reservation.created",
                "customer.bar_reservation.create_failed",
                "not_applicable",
                "bar_reservation",
                () -> {
                    String barId = requireText(actual.barId(), "bar_id");
                    String tableId = requireText(actual.tableId(), "table_id");
                    String tableType = requireText(actual.tableType(), "table_type");
                    BarTableRecord table = repository.findActiveBarTableForReservation(barId, tableId, tableType)
                            .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "bar table reservation option not found"));
                    int guestCount = actual.guestCount() == null ? 0 : actual.guestCount();
                    if (guestCount < 0) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "guest_count must be non-negative");
                    }
                    if (table.capacity() > 0 && guestCount > table.capacity()) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "guest_count exceeds table capacity");
                    }
                    Instant now = Instant.now();
                    // P0#3 订台定金（V073 bar_deposit_rules，对齐 BarEntryService）：按 bar 读启用规则，
                    // table_type 优先于 bar。无匹配规则或计算额 ≤0 → 0/not_required（向后兼容）；
                    // 否则 pending（待支付）。比例模式按卡座低消 minConsumptionCent × ratioBps/10000。
                    BarDepositRuleRecord rule = resolveDepositRule(barId, table);
                    long depositAmountCent = rule == null ? 0L : computeDepositAmount(rule, table);
                    String depositStatus;
                    if (rule == null || depositAmountCent <= 0L) {
                        depositAmountCent = 0L;
                        depositStatus = "not_required";
                    } else {
                        depositStatus = "pending";
                    }
                    return repository.saveCustomerBarReservation(new BarReservationRecord(
                            "reservation_" + slug(key),
                            barId,
                            null,
                            table.id(),
                            customer.id(),
                            actual.reservationTime() == null ? now : actual.reservationTime(),
                            guestCount,
                            table.allowedPassFreeGuestCount(),
                            depositAmountCent,
                            depositStatus,
                            "miniapp",
                            null,
                            "reserved",
                            "miniapp",
                            null,
                            List.of(),
                            now,
                            now,
                            null
                    ));
                }
        );
    }

    /**
     * V155 订卡邀请函——朋友确认（客人端两步之第一步，占一个名额）。
     * 鉴权=客人 token（requireCustomer，能力 not_applicable，不经 requirePermission，对齐 createBarReservation）。
     * 幂等：同一朋友重复确认返回原成员；确认上限 confirmed<capacity（卡座可进人数）。
     */
    @Transactional
    public BarReservationInvitationMemberRecord confirmBarInvitation(
            String customerToken,
            String idempotencyKey,
            CustomerBarInvitationConfirmRequest request
    ) {
        CustomerRecord customer = requireCustomer(customerToken);
        CustomerBarInvitationConfirmRequest actual = requireRequest(request);
        String key = firstNonBlank(idempotencyKey, actual.idempotencyKey());
        return executeWrite(
                "confirmCustomerBarInvitation:" + customer.id(),
                key,
                actual,
                BarReservationInvitationMemberRecord.class,
                "customer.bar_invitation.confirmed",
                "customer.bar_invitation.confirm_failed",
                "not_applicable",
                "bar_reservation",
                () -> {
                    String token = requireText(actual.invitationToken(), "invitation_token");
                    BarReservationRecord reservation = barRepository.findReservationByInvitationToken(token)
                            .orElseThrow(() -> ApiException.notFound("BAR_INVITATION_NOT_FOUND", "card-seat invitation not found"));
                    // T1.1 并发超员修复：按预订行 FOR UPDATE 加锁（本方法 @Transactional），序列化并发确认，
                    // 防"读已确认人数 → 判容量 → 插入"竞态击穿卡座可进人数上限。
                    reservation = barRepository.findReservationByIdForUpdate(reservation.id())
                            .orElseThrow(() -> ApiException.notFound("BAR_INVITATION_NOT_FOUND", "card-seat invitation not found"));
                    if (!"active".equals(reservation.invitationStatus())) {
                        throw ApiException.conflict("BAR_INVITATION_NOT_ACTIVE", "card-seat invitation is not active");
                    }
                    String memberIdentifier = customer.id();
                    List<BarReservationInvitationMemberRecord> members = barRepository.listInvitationMembers(reservation.id());
                    BarReservationInvitationMemberRecord existing = members.stream()
                            .filter(member -> memberIdentifier.equals(member.memberIdentifier()))
                            .findFirst().orElse(null);
                    if (existing != null) {
                        return existing; // 幂等：同一朋友重复确认返回原记录，不重复占名额
                    }
                    int capacity = reservation.invitationCapacity() == null ? 0 : reservation.invitationCapacity();
                    long confirmed = members.stream().filter(member -> !"revoked".equals(member.status())).count();
                    if (confirmed >= capacity) {
                        throw ApiException.conflict("BAR_INVITATION_CAPACITY_EXHAUSTED", "card-seat capacity is exhausted");
                    }
                    Instant now = Instant.now();
                    return barRepository.saveInvitationMember(new BarReservationInvitationMemberRecord(
                            "invm_" + slug(reservation.id() + ":" + memberIdentifier),
                            reservation.id(), reservation.barId(), memberIdentifier, customer.id(),
                            false, "confirmed", now, null, null, now, now));
                }
        );
    }

    /**
     * V155 订卡邀请函——持函人（客人端）查看自己预订的邀请函（展示二维码 token / 容量 / 已确认成员，供转发）。
     * 只读，鉴权=客人 token + 校验该预订归属本人（CROSS_CUSTOMER_FORBIDDEN）；不签发（签发为 staff 动作）。
     */
    public BarReservationInvitationRecord getMyBarInvitation(String customerToken, String reservationId) {
        CustomerRecord customer = requireCustomer(customerToken);
        String id = requireText(reservationId, "reservation_id");
        BarReservationRecord reservation = barRepository.findReservationById(id)
                .orElseThrow(() -> ApiException.notFound("BAR_RESERVATION_NOT_FOUND", "bar reservation not found"));
        if (!customer.id().equals(reservation.customerId())) {
            throw ApiException.forbidden("CROSS_CUSTOMER_FORBIDDEN", "access to another customer's reservation is not allowed");
        }
        List<BarReservationInvitationMemberRecord> members = barRepository.listInvitationMembers(id);
        int confirmed = (int) members.stream().filter(member -> !"revoked".equals(member.status())).count();
        int entered = (int) members.stream().filter(member -> "entered".equals(member.status())).count();
        int capacity = reservation.invitationCapacity() == null ? 0 : reservation.invitationCapacity();
        return new BarReservationInvitationRecord(
                reservation.id(), reservation.barId(), reservation.tableId(),
                reservation.invitationToken(), reservation.invitationSignature(), capacity,
                reservation.invitationStatus(), reservation.invitationIssuedAt(),
                confirmed, entered, Math.max(capacity - confirmed, 0), members);
    }

    /** P0#3：scope 优先级 table_type > bar；listDepositRules 已按 scope_type DESC 返回（对齐 BarEntryService）。 */
    private BarDepositRuleRecord resolveDepositRule(String barId, BarTableRecord table) {
        List<BarDepositRuleRecord> rules = repository.listDepositRules(barId);
        BarDepositRuleRecord barScoped = null;
        for (BarDepositRuleRecord rule : rules) {
            if ("table_type".equals(rule.scopeType()) && rule.scopeId() != null
                    && rule.scopeId().equals(table.tableType())) {
                return rule;
            }
            if ("bar".equals(rule.scopeType()) && barScoped == null) {
                barScoped = rule;
            }
        }
        return barScoped;
    }

    /** P0#3：fixed=固定额；min_consumption_ratio=卡座低消 × ratioBps/10000（对齐 BarEntryService）。 */
    private long computeDepositAmount(BarDepositRuleRecord rule, BarTableRecord table) {
        if ("fixed".equals(rule.mode())) {
            return rule.fixedAmountCent() == null ? 0L : Math.max(0L, rule.fixedAmountCent());
        }
        if ("min_consumption_ratio".equals(rule.mode())) {
            int ratioBps = rule.ratioBps() == null ? 0 : rule.ratioBps();
            return Math.max(0L, table.minConsumptionCent() * ratioBps / 10_000L);
        }
        return 0L;
    }

    /** 解析客人 token → customerId（鉴权失败抛 401）。供储值/付费会员/积分转赠等客人端动作复用。 */
    public String resolveCustomerId(String customerToken) {
        return requireCustomer(customerToken).id();
    }

    /**
     * #14 钱包双路径：解析客人 token → customerId，无效/过期/缺失返回 empty（不抛异常）。
     * 供 CustomerTokenAuthenticationFilter 在过滤链中静默判定客人身份用（无效 token 直接跳过，由后续鉴权处理）。
     */
    public java.util.Optional<String> resolveCustomerIdQuietly(String customerToken) {
        try {
            return java.util.Optional.of(requireCustomer(customerToken).id());
        } catch (RuntimeException exception) {
            return java.util.Optional.empty();
        }
    }

    /**
     * 客人扫码点单——查菜单（安全投影，刻意隐去内部结算价）：扫桌台二维码得 session_id → 列该开台会话所在 bar 的在售商品。
     */
    public PageResponse<CustomerBarMenuItemResponse> listBarScanMenu(String customerToken, String sessionId) {
        requireCustomer(customerToken);
        String id = requireText(sessionId, "session_id");
        BarTableSessionRecord session = barRepository.findTableSessionById(id)
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_SESSION_NOT_FOUND", "bar table session not found"));
        if (!"open".equals(session.status())) {
            throw ApiException.conflict("BAR_TABLE_SESSION_NOT_OPEN", "scan order requires an open table session");
        }
        List<CustomerBarMenuItemResponse> items = barRepository.listProducts(Optional.ofNullable(session.merchantId())).stream()
                .filter(p -> session.barId().equals(p.barId()) && "active".equals(p.status()))
                .map(p -> new CustomerBarMenuItemResponse(
                        p.id(), p.productName(), p.productType(), p.priceCent(), p.currency(), p.soldOut()))
                .toList();
        return new PageResponse<>(items, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, items.size());
    }

    /**
     * 客人自助扫码下单：在开台会话上下单（bar/桌台/预订/商户取自会话，customerId=本人，来源 app/miniapp），
     * 继承入场闸不重复核龄；幂等经 executeWrite。复用 BarOrderService.placeCustomerScanOrder
     *（功能标志 bar.scan_order.enabled 默认关→关时拒）。
     */
    @Transactional
    public BarOrderRecord placeBarScanOrder(
            String customerToken, String idempotencyKey, String sessionId, CustomerBarScanOrderRequest request) {
        CustomerRecord customer = requireCustomer(customerToken);
        CustomerBarScanOrderRequest actual = requireRequest(request);
        String id = requireText(sessionId, "session_id");
        String key = firstNonBlank(idempotencyKey, actual.idempotencyKey());
        return executeWrite(
                "customerBarScanOrder:" + customer.id(),
                key,
                actual,
                BarOrderRecord.class,
                "customer.bar_scan_order.created",
                "customer.bar_scan_order.create_failed",
                "not_applicable",
                "bar_order",
                () -> barOrderService.placeCustomerScanOrder(
                        id,
                        new BarOrderRequest(key, null, List.of(), null, null, null, actual.items(), null, List.of(), null),
                        customer.id()));
    }

    /** 3.3 免登录：会话恢复——凭有效 token（cookie 桥接或显式头）返回客人身份与到期时间，不回凭证。 */
    public CustomerSessionInfoResponse sessionInfo(String customerToken) {
        CustomerRecord customer = requireCustomer(customerToken);
        Instant expiresAt = repository.findSessionByToken(customerToken.trim())
                .map(CustomerSessionRecord::expiresAt)
                .orElse(null);
        return new CustomerSessionInfoResponse(customer.id(), customer.maskedPhone(), expiresAt);
    }

    private CustomerRecord requireCustomer(String customerToken) {
        String token = trimToNull(customerToken);
        if (token == null) {
            throw ApiException.unauthorized("CUSTOMER_UNAUTHENTICATED", "customer token is required");
        }
        CustomerSessionRecord session = repository.findSessionByToken(token)
                .orElseThrow(() -> ApiException.unauthorized("CUSTOMER_UNAUTHENTICATED", "customer token is invalid"));
        if (!"active".equals(session.status()) || session.expiresAt().isBefore(Instant.now())) {
            throw ApiException.unauthorized("CUSTOMER_UNAUTHENTICATED", "customer token is expired");
        }
        CustomerRecord customer = repository.findCustomerById(session.customerId())
                .orElseThrow(() -> ApiException.unauthorized("CUSTOMER_UNAUTHENTICATED", "customer is unavailable"));
        // V194 需求：所有客人动作前过账号状态闸（黑名单/注销/未到期封禁拒；封禁到期 lazy 恢复）。
        return enforceUsableAccount(customer, clock.instant());
    }

    /**
     * V194 需求 · 客人账号状态闸：
     * blacklisted 永久拒、deregistered 已注销拒、suspended 未到期拒；suspended 已过 suspended_until → lazy 恢复 active 并落库。
     */
    private CustomerRecord enforceUsableAccount(CustomerRecord customer, Instant now) {
        String status = customer.status();
        if ("blacklisted".equals(status)) {
            throw ApiException.forbidden("CUSTOMER_BLACKLISTED", "this account is permanently blocked");
        }
        if ("deregistered".equals(status)) {
            throw ApiException.forbidden("CUSTOMER_DEREGISTERED", "this account has been deregistered");
        }
        if ("suspended".equals(status)) {
            Instant until = customer.suspendedUntil();
            if (until != null && !now.isBefore(until)) {
                return repository.saveCustomer(new CustomerRecord(
                        customer.id(), customer.phone(), customer.maskedPhone(), "active",
                        customer.birthDate(), customer.lastLoginAt(), customer.createdAt(), now, null));
            }
            throw ApiException.forbidden("CUSTOMER_SUSPENDED",
                    until == null ? "this account is suspended" : "this account is suspended until " + until);
        }
        return customer;
    }

    /** 平台封禁客人账号（临时，suspendedUntil 到期自动恢复）。需 customer.account.manage。 */
    @Transactional
    public CustomerRecord suspendCustomer(String customerId, Instant suspendedUntil, String reason) {
        return changeAccountStatus(customerId, "suspended", suspendedUntil, reason, "customer.account.suspended", "customer.account.suspend_failed");
    }

    /** 平台拉黑客人账号（永久）。需 customer.account.manage。 */
    @Transactional
    public CustomerRecord blacklistCustomer(String customerId, String reason) {
        return changeAccountStatus(customerId, "blacklisted", null, reason, "customer.account.blacklisted", "customer.account.blacklist_failed");
    }

    /** 平台恢复客人账号为正常（解封/解黑/复活）。需 customer.account.manage。 */
    @Transactional
    public CustomerRecord reinstateCustomer(String customerId, String reason) {
        return changeAccountStatus(customerId, "active", null, reason, "customer.account.reinstated", "customer.account.reinstate_failed");
    }

    /** 客人自助注销本人账号（凭本人 token，无需管理权限）。注销后动作/登录被拒、数据物理保留。 */
    @Transactional
    public CustomerRecord deregisterAccount(String customerToken) {
        CustomerRecord customer = requireCustomer(customerToken);
        return changeAccountStatus(customer.id(), "deregistered", null, "self-service deregistration",
                "customer.account.deregistered", "customer.account.deregister_failed");
    }

    /** 平台按手机号查客人账号（盲索引精确匹配；按手机号查人属敏感操作，成功与否均留审计）。 */
    public java.util.Optional<CustomerRecord> findAccountByPhone(String phone) {
        try {
            java.util.Optional<CustomerRecord> found = repository.findCustomerByPhone(requireText(phone, "phone"));
            auditEventService.recordSuccess("customer.account.viewed", "customer.account.manage", "customer",
                    found.map(CustomerRecord::id).orElse("not_found"), null, null, Map.of("matched", found.isPresent()));
            return found;
        } catch (ApiException e) {
            auditEventService.recordFailure("customer.account.view_failed", "customer.account.manage", "customer",
                    "unknown", null, null, e.code());
            throw e;
        }
    }

    private CustomerRecord changeAccountStatus(String customerId, String status, Instant suspendedUntil,
            String reason, String event, String failureEvent) {
        try {
            CustomerRecord existing = repository.findCustomerById(requireText(customerId, "customer_id"))
                    .orElseThrow(() -> ApiException.notFound("CUSTOMER_NOT_FOUND", "customer not found"));
            Instant now = clock.instant();
            CustomerRecord saved = repository.saveCustomer(new CustomerRecord(
                    existing.id(), existing.phone(), existing.maskedPhone(), status, existing.birthDate(),
                    existing.lastLoginAt(), existing.createdAt(), now, suspendedUntil));
            auditEventService.recordSuccess(event, "customer.account.manage", "customer", customerId,
                    null, null, Map.of("status", status, "reason", reason == null ? "" : reason));
            return saved;
        } catch (ApiException e) {
            auditEventService.recordFailure(failureEvent, "customer.account.manage", "customer", customerId,
                    null, null, e.code());
            throw e;
        }
    }

    private CustomerTicketProductResponse toTicketProduct(TicketTypeRecord ticketType) {
        return new CustomerTicketProductResponse(
                ticketType.id(),
                ticketType.ticketName(),
                ticketType.weekdayPriceCent(),
                ticketType.weekendPriceCent(),
                ticketType.holidayPriceCent(),
                "CNY",
                ticketType.status()
        );
    }

    private <T> PageResponse<T> readPage(
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<List<T>> action
    ) {
        return operations.readPage(
                AuditedOperation.of(successEvent, failureEvent, permissionCode, businessType),
                action
        );
    }

    private <T> T executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<T> action
    ) {
        return operations.executeWrite(
                operationId,
                idempotencyKey,
                request,
                responseType,
                AuditedOperation.of(successEvent, failureEvent, permissionCode, businessType),
                action,
                this::businessId,
                Map.of()
        );
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return trimmed;
    }

    private String firstNonBlank(String first, String second) {
        String firstTrimmed = trimToNull(first);
        return firstTrimmed == null ? second : firstTrimmed;
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean redemptionItemIsActive(CustomerRedemptionCatalogItemRecord item, Instant now) {
        return "active".equals(item.status())
                && (item.availableFrom() == null || !item.availableFrom().isAfter(now))
                && (item.availableUntil() == null || item.availableUntil().isAfter(now));
    }

    private String maskPhone(String phone) {
        if (phone.length() <= 7) {
            return phone.charAt(0) + "****" + phone.charAt(phone.length() - 1);
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String slug(String value) {
        String slug = trimToDefault(value, UUID.randomUUID().toString())
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        String fallback = UUID.randomUUID().toString().replace("-", "");
        String resolved = slug.isEmpty() ? fallback : slug;
        if (resolved.length() <= 40) {
            return resolved;
        }
        // Keep a readable prefix but disambiguate by a hash of the full value so two distinct keys that
        // differ only past the 40-char cutoff cannot truncate to the same id and overwrite each other.
        return resolved.substring(0, 31) + "_" + shortHash(value);
    }

    private String shortHash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 8);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 hashing unavailable", exception);
        }
    }

    private String businessId(Object value) {
        if (value instanceof TicketOrderRecord record) {
            return record.id();
        }
        if (value instanceof CustomerConsentRecord record) {
            return record.id();
        }
        if (value instanceof BarReservationRecord record) {
            return record.id();
        }
        if (value instanceof CustomerPointsRedemptionRecord record) {
            return record.id();
        }
        return null;
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }
}
