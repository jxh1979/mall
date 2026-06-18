package com.complex.platform.customer;

import com.complex.platform.bar.BarReservationRecord;
import com.complex.platform.bar.BarTableRecord;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.customer.dto.CustomerBarReservationRequest;
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

    public CustomerService(
            CustomerRepository repository,
            CustomerOtpVerifier otpVerifier,
            TicketingService ticketingService,
            AuditedOperationExecutor operations,
            AuditEventService auditEventService,
            InventoryService inventoryService
    ) {
        this.repository = repository;
        this.otpVerifier = otpVerifier;
        this.ticketingService = ticketingService;
        this.operations = operations;
        this.auditEventService = auditEventService;
        this.inventoryService = inventoryService;
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
                    .map(existing -> repository.saveCustomer(new CustomerRecord(
                            existing.id(),
                            existing.phone(),
                            existing.maskedPhone(),
                            "active",
                            now,
                            existing.createdAt(),
                            now
                    )))
                    .orElseGet(() -> repository.saveCustomer(new CustomerRecord(
                            "customer_" + slug(phone),
                            phone,
                            maskPhone(phone),
                            "active",
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
                    .orElseGet(() -> {
                        Instant now = Instant.now();
                        return repository.savePoints(new CustomerPointsAccountRecord(
                                "points_" + customer.id(),
                                customer.id(),
                                0L,
                                "base",
                                Map.of(),
                                now,
                                now
                        ));
                    });
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

    public PageResponse<CustomerRedemptionCatalogItemRecord> listRedemptionItems(String customerToken) {
        requireCustomer(customerToken);
        return readPage(
                "customer.points_redemption_items_viewed",
                "customer.points_redemption_items_view_failed",
                "not_applicable",
                "customer_points_redemption_item",
                () -> repository.listActiveRedemptionCatalogItems(Instant.now())
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
                    long balanceAfter = account.balance() - totalPoints;
                    repository.savePoints(new CustomerPointsAccountRecord(
                            account.id(),
                            account.customerId(),
                            balanceAfter,
                            account.level(),
                            Map.of(
                                    "last_redemption_id", redemptionId,
                                    "last_redeemed_points", totalPoints
                            ),
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
                .orElseGet(() -> {
                    Instant createdAt = Instant.now();
                    return repository.savePoints(new CustomerPointsAccountRecord(
                            "points_" + customerId,
                            customerId,
                            0L,
                            "base",
                            Map.of(),
                            createdAt,
                            createdAt
                    ));
                });
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
                    return repository.saveCustomerBarReservation(new BarReservationRecord(
                            "reservation_" + slug(key),
                            barId,
                            null,
                            table.id(),
                            customer.id(),
                            actual.reservationTime() == null ? now : actual.reservationTime(),
                            guestCount,
                            table.allowedPassFreeGuestCount(),
                            0L,
                            "not_required",
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
        return repository.findCustomerById(session.customerId())
                .orElseThrow(() -> ApiException.unauthorized("CUSTOMER_UNAUTHENTICATED", "customer is unavailable"));
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
