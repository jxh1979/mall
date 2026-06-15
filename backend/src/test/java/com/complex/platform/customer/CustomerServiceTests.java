package com.complex.platform.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.bar.BarReservationRecord;
import com.complex.platform.bar.BarTableRecord;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.customer.dto.CustomerBarReservationRequest;
import com.complex.platform.customer.dto.CustomerConsentRequest;
import com.complex.platform.customer.dto.CustomerPhoneLoginRequest;
import com.complex.platform.customer.dto.CustomerPointsRedemptionRequest;
import com.complex.platform.customer.dto.CustomerTicketOrderRequest;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.inventory.InventoryService;
import com.complex.platform.inventory.StockAdjustmentRecord;
import com.complex.platform.inventory.dto.StockAdjustmentRequest;
import com.complex.platform.ticketing.TicketCredentialResponse;
import com.complex.platform.ticketing.TicketOrderRecord;
import com.complex.platform.ticketing.TicketTypeRecord;
import com.complex.platform.ticketing.TicketingService;
import com.complex.platform.ticketing.dto.TicketOrderRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CustomerServiceTests {

    private InMemoryCustomerRepository customerRepository;
    private InMemoryAuditEventRepository auditEventRepository;
    private TicketingService ticketingService;
    private InventoryService inventoryService;
    private CustomerService service;

    @BeforeEach
    void setUp() {
        customerRepository = new InMemoryCustomerRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        ticketingService = Mockito.mock(TicketingService.class);
        inventoryService = Mockito.mock(InventoryService.class);
        service = new CustomerService(
                customerRepository,
                ticketingService,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                inventoryService
        );
    }

    @Test
    void phoneLoginReturnsCustomerTokenAndMasksPhone() {
        CustomerLoginResponse response = service.phoneLogin(new CustomerPhoneLoginRequest(
                "13800138000",
                "000000",
                "miniapp"
        ));

        assertThat(response.customerToken()).startsWith("cust_");
        assertThat(response.maskedPhone()).isEqualTo("138****8000");
        assertThat(customerRepository.findSessionByToken(response.customerToken())).isPresent();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("customer.auth.login");
    }

    @Test
    void ticketProductsArePublicListable() {
        when(ticketingService.listTicketTypes()).thenReturn(new PageResponse<>(List.of(new TicketTypeRecord(
                "ticket_type_public",
                "Public Pass",
                "weekday_weekend_holiday",
                9900L,
                11900L,
                12900L,
                false,
                "published",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        )), 1, 50, 1));

        PageResponse<CustomerTicketProductResponse> response = service.listTicketProducts();

        assertThat(response.items())
                .extracting(CustomerTicketProductResponse::ticketTypeId, CustomerTicketProductResponse::currency)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("ticket_type_public", "CNY"));
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("customer.ticket_products_viewed");
    }

    @Test
    void customerTicketOrderUsesCustomerIdentityAndIdempotency() {
        CustomerLoginResponse login = service.phoneLogin(new CustomerPhoneLoginRequest("13800138000", "000000", "miniapp"));
        when(ticketingService.createTicketOrder(any(TicketOrderRequest.class))).thenReturn(new TicketOrderRecord(
                "ticket_order_customer",
                "TO20260614000001",
                login.customerId(),
                "ticket_type_1",
                null,
                2,
                LocalDate.parse("2026-06-20"),
                "miniapp",
                "13800138000",
                19800L,
                "CNY",
                "unpaid",
                "not_issued",
                "created",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));

        TicketOrderRecord response = service.createTicketOrder(
                login.customerToken(),
                "customer-order-once",
                new CustomerTicketOrderRequest(
                        "customer-order-once",
                        "ticket_type_1",
                        null,
                        2,
                        LocalDate.parse("2026-06-20"),
                        "13800138000",
                        "window"
                )
        );

        ArgumentCaptor<TicketOrderRequest> requestCaptor = ArgumentCaptor.forClass(TicketOrderRequest.class);
        verify(ticketingService).createTicketOrder(requestCaptor.capture());
        assertThat(requestCaptor.getValue().customerId()).isEqualTo(login.customerId());
        assertThat(requestCaptor.getValue().salesChannel()).isEqualTo("miniapp");
        assertThat(response.customerId()).isEqualTo(login.customerId());
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("customer.ticket_order.created");
    }

    @Test
    void customerOrdersAndQrAreScopedToCustomerToken() {
        CustomerLoginResponse login = service.phoneLogin(new CustomerPhoneLoginRequest("13800138000", "000000", "miniapp"));
        when(ticketingService.listTicketOrdersByCustomerId(login.customerId())).thenReturn(List.of(new TicketOrderRecord(
                "ticket_order_customer",
                "TO20260614000001",
                login.customerId(),
                "ticket_type_1",
                null,
                1,
                LocalDate.parse("2026-06-20"),
                "miniapp",
                "13800138000",
                9900L,
                "CNY",
                "paid",
                "issued",
                "created",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        )));
        when(ticketingService.getCustomerTicketCredential("ent_customer", login.customerId())).thenReturn(new TicketCredentialResponse(
                "credential_customer",
                "ent_customer",
                "ticket_order_customer",
                "cred_customer",
                "dynamic_qr",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:05:00Z"),
                1
        ));

        PageResponse<TicketOrderRecord> orders = service.listOrders(login.customerToken());
        TicketCredentialResponse qr = service.getTicketQr(login.customerToken(), "ent_customer");

        assertThat(orders.items()).extracting(TicketOrderRecord::customerId).containsExactly(login.customerId());
        assertThat(qr.credentialToken()).isEqualTo("cred_customer");
        verify(ticketingService).listTicketOrdersByCustomerId(login.customerId());
        verify(ticketingService).getCustomerTicketCredential("ent_customer", login.customerId());
    }

    @Test
    void consentAndPointsUseCustomerTokenBoundary() {
        CustomerLoginResponse login = service.phoneLogin(new CustomerPhoneLoginRequest("13800138000", "000000", "miniapp"));

        CustomerConsentRecord consent = service.recordConsent(login.customerToken(), "consent-once", new CustomerConsentRequest(
                "consent-once",
                "privacy",
                true,
                "v1",
                "miniapp"
        ));
        CustomerPointsResponse points = service.getPoints(login.customerToken());

        assertThat(consent.customerId()).isEqualTo(login.customerId());
        assertThat(consent.accepted()).isTrue();
        assertThat(points.balance()).isZero();
        assertThat(points.level()).isEqualTo("base");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("customer.consent.created", "customer.points_viewed");
    }

    @Test
    void barReservationOptionsAndCreateUseCustomerTokenWithoutStaffPermission() {
        CustomerLoginResponse login = service.phoneLogin(new CustomerPhoneLoginRequest("13800138000", "000000", "miniapp"));
        customerRepository.saveBarTable(new BarTableRecord(
                "table_customer",
                "bar_1",
                "T-CUST",
                "VIP Customer",
                "card_seat",
                6,
                2,
                50000L,
                "CNY",
                "active",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));

        PageResponse<CustomerBarReservationOptionResponse> options = service.listBarReservationOptions(login.customerToken());
        BarReservationRecord reservation = service.createBarReservation(
                login.customerToken(),
                "customer-bar-reservation",
                new CustomerBarReservationRequest(
                        null,
                        "bar_1",
                        "table_customer",
                        "card_seat",
                        Instant.parse("2026-06-20T21:00:00Z"),
                        2,
                        "13800138000"
                )
        );

        assertThat(options.items())
                .extracting(CustomerBarReservationOptionResponse::tableId, CustomerBarReservationOptionResponse::allowedPassFreeGuestCount)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("table_customer", 2));
        assertThat(reservation)
                .extracting(
                        BarReservationRecord::customerId,
                        BarReservationRecord::allowedPassFreeGuestCount,
                        BarReservationRecord::depositAmountCent,
                        BarReservationRecord::orderSource,
                        BarReservationRecord::status
                )
                .containsExactly(login.customerId(), 2, 0L, "miniapp", "reserved");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("customer.bar_reservation_options_viewed", "customer.bar_reservation.created");
    }

    @Test
    void pointsLedgerAndRedemptionItemsAreCustomerScoped() {
        CustomerLoginResponse login = service.phoneLogin(new CustomerPhoneLoginRequest("13800138000", "000000", "miniapp"));
        customerRepository.savePoints(new CustomerPointsAccountRecord(
                "points_" + login.customerId(),
                login.customerId(),
                500L,
                "silver",
                Map.of("last_earned", 200),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        customerRepository.savePointsLedger(new CustomerPointsLedgerRecord(
                "ledger_1",
                login.customerId(),
                200L,
                500L,
                "ticket_order",
                "order_1",
                "Ticket purchase reward",
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        customerRepository.savePointsLedger(new CustomerPointsLedgerRecord(
                "ledger_other",
                "customer_other",
                999L,
                999L,
                "manual",
                "manual_1",
                "Other customer",
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        customerRepository.saveRedemptionCatalogItem(new CustomerRedemptionCatalogItemRecord(
                "gift_active",
                "Plush Gift",
                "gift",
                120L,
                "item_gift",
                "gift_shelf",
                1,
                "active",
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        customerRepository.saveRedemptionCatalogItem(new CustomerRedemptionCatalogItemRecord(
                "gift_inactive",
                "Inactive Gift",
                "gift",
                100L,
                "item_inactive",
                "gift_shelf",
                1,
                "inactive",
                null,
                null,
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));

        PageResponse<CustomerPointsLedgerRecord> ledger = service.listPointsLedger(login.customerToken());
        PageResponse<CustomerRedemptionCatalogItemRecord> catalog = service.listRedemptionItems(login.customerToken());

        assertThat(ledger.items()).extracting(CustomerPointsLedgerRecord::id).containsExactly("ledger_1");
        assertThat(catalog.items()).extracting(CustomerRedemptionCatalogItemRecord::id).containsExactly("gift_active");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("customer.points_ledger_viewed", "customer.points_redemption_items_viewed");
    }

    @Test
    void redeemPointsDeductsBalanceCreatesLedgerAndInventoryAdjustment() {
        CustomerLoginResponse login = service.phoneLogin(new CustomerPhoneLoginRequest("13800138000", "000000", "miniapp"));
        customerRepository.savePoints(new CustomerPointsAccountRecord(
                "points_" + login.customerId(),
                login.customerId(),
                500L,
                "silver",
                Map.of(),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        customerRepository.saveRedemptionCatalogItem(new CustomerRedemptionCatalogItemRecord(
                "gift_active",
                "Plush Gift",
                "gift",
                120L,
                "item_gift",
                "gift_shelf",
                2,
                "active",
                null,
                null,
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        when(inventoryService.createStockAdjustment(any(StockAdjustmentRequest.class))).thenReturn(new StockAdjustmentRecord(
                "adjust_redeem",
                "item_gift",
                "gift_shelf",
                "loss",
                2,
                "points_redemption",
                List.of(),
                "completed",
                Instant.parse("2026-06-14T00:00:00Z")
        ));

        CustomerPointsRedemptionRecord redemption = service.redeemPoints(
                login.customerToken(),
                "redeem-once",
                new CustomerPointsRedemptionRequest(null, "gift_active", 1, "miniapp")
        );

        assertThat(redemption)
                .extracting(
                        CustomerPointsRedemptionRecord::customerId,
                        CustomerPointsRedemptionRecord::catalogItemId,
                        CustomerPointsRedemptionRecord::totalPoints,
                        CustomerPointsRedemptionRecord::status
                )
                .containsExactly(login.customerId(), "gift_active", 120L, "pending_pickup");
        assertThat(customerRepository.findPointsByCustomerId(login.customerId()).orElseThrow().balance()).isEqualTo(380L);
        assertThat(customerRepository.pointsLedger)
                .extracting(CustomerPointsLedgerRecord::pointsDelta, CustomerPointsLedgerRecord::balanceAfter, CustomerPointsLedgerRecord::sourceType)
                .contains(org.assertj.core.groups.Tuple.tuple(-120L, 380L, "points_redemption"));
        ArgumentCaptor<StockAdjustmentRequest> stockRequest = ArgumentCaptor.forClass(StockAdjustmentRequest.class);
        verify(inventoryService).createStockAdjustment(stockRequest.capture());
        assertThat(stockRequest.getValue().adjustmentType()).isEqualTo("loss");
        assertThat(stockRequest.getValue().reason()).contains("points_redemption");
        assertThat(stockRequest.getValue().items().getFirst())
                .containsEntry("item_id", "item_gift")
                .containsEntry("location_id", "gift_shelf")
                .containsEntry("quantity", 2);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("customer.points_redeemed");
    }

    @Test
    void redeemPointsRejectsInsufficientPointsWithoutInventoryCall() {
        CustomerLoginResponse login = service.phoneLogin(new CustomerPhoneLoginRequest("13800138000", "000000", "miniapp"));
        customerRepository.savePoints(new CustomerPointsAccountRecord(
                "points_" + login.customerId(),
                login.customerId(),
                50L,
                "base",
                Map.of(),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        customerRepository.saveRedemptionCatalogItem(new CustomerRedemptionCatalogItemRecord(
                "gift_active",
                "Plush Gift",
                "gift",
                120L,
                "item_gift",
                "gift_shelf",
                1,
                "active",
                null,
                null,
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));

        assertThatThrownBy(() -> service.redeemPoints(
                login.customerToken(),
                "redeem-insufficient",
                new CustomerPointsRedemptionRequest(null, "gift_active", 1, "miniapp")
        ))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("points balance is insufficient");

        assertThat(customerRepository.findPointsByCustomerId(login.customerId()).orElseThrow().balance()).isEqualTo(50L);
        verify(inventoryService, never()).createStockAdjustment(any(StockAdjustmentRequest.class));
    }

    @Test
    void redeemPointsKeepsBalanceWhenInventoryFails() {
        CustomerLoginResponse login = service.phoneLogin(new CustomerPhoneLoginRequest("13800138000", "000000", "miniapp"));
        customerRepository.savePoints(new CustomerPointsAccountRecord(
                "points_" + login.customerId(),
                login.customerId(),
                500L,
                "silver",
                Map.of(),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        customerRepository.saveRedemptionCatalogItem(new CustomerRedemptionCatalogItemRecord(
                "gift_active",
                "Plush Gift",
                "gift",
                120L,
                "item_gift",
                "gift_shelf",
                1,
                "active",
                null,
                null,
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        when(inventoryService.createStockAdjustment(any(StockAdjustmentRequest.class)))
                .thenThrow(ApiException.conflict("STOCK_INSUFFICIENT", "available stock is insufficient"));

        assertThatThrownBy(() -> service.redeemPoints(
                login.customerToken(),
                "redeem-stock-fails",
                new CustomerPointsRedemptionRequest(null, "gift_active", 1, "miniapp")
        ))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("available stock is insufficient");

        assertThat(customerRepository.findPointsByCustomerId(login.customerId()).orElseThrow().balance()).isEqualTo(500L);
        assertThat(customerRepository.redemptions).isEmpty();
        assertThat(customerRepository.pointsLedger).isEmpty();
    }

    private static final class InMemoryCustomerRepository implements CustomerRepository {
        private final Map<String, CustomerRecord> customers = new LinkedHashMap<>();
        private final Map<String, CustomerSessionRecord> sessions = new LinkedHashMap<>();
        private final Map<String, CustomerConsentRecord> consents = new LinkedHashMap<>();
        private final Map<String, CustomerPointsAccountRecord> points = new LinkedHashMap<>();
        private final List<CustomerPointsLedgerRecord> pointsLedger = new ArrayList<>();
        private final Map<String, CustomerRedemptionCatalogItemRecord> catalogItems = new LinkedHashMap<>();
        private final Map<String, CustomerPointsRedemptionRecord> redemptions = new LinkedHashMap<>();
        private final Map<String, BarTableRecord> barTables = new LinkedHashMap<>();
        private final Map<String, BarReservationRecord> barReservations = new LinkedHashMap<>();

        private void saveBarTable(BarTableRecord table) {
            barTables.put(table.id(), table);
        }

        @Override
        public Optional<CustomerRecord> findCustomerByPhone(String phone) {
            return customers.values().stream()
                    .filter(customer -> phone.equals(customer.phone()))
                    .findFirst();
        }

        @Override
        public Optional<CustomerRecord> findCustomerById(String customerId) {
            return Optional.ofNullable(customers.get(customerId));
        }

        @Override
        public CustomerRecord saveCustomer(CustomerRecord customer) {
            customers.put(customer.id(), customer);
            return customer;
        }

        @Override
        public CustomerSessionRecord saveSession(CustomerSessionRecord session) {
            sessions.put(session.customerToken(), session);
            return session;
        }

        @Override
        public Optional<CustomerSessionRecord> findSessionByToken(String token) {
            return Optional.ofNullable(sessions.get(token));
        }

        @Override
        public CustomerConsentRecord saveConsent(CustomerConsentRecord consent) {
            consents.put(consent.id(), consent);
            return consent;
        }

        @Override
        public Optional<CustomerPointsAccountRecord> findPointsByCustomerId(String customerId) {
            return Optional.ofNullable(points.get(customerId));
        }

        @Override
        public CustomerPointsAccountRecord savePoints(CustomerPointsAccountRecord account) {
            points.put(account.customerId(), account);
            return account;
        }

        @Override
        public CustomerPointsLedgerRecord savePointsLedger(CustomerPointsLedgerRecord ledger) {
            pointsLedger.add(ledger);
            return ledger;
        }

        @Override
        public List<CustomerPointsLedgerRecord> listPointsLedgerByCustomerId(String customerId) {
            return pointsLedger.stream()
                    .filter(ledger -> customerId.equals(ledger.customerId()))
                    .toList();
        }

        @Override
        public CustomerRedemptionCatalogItemRecord saveRedemptionCatalogItem(CustomerRedemptionCatalogItemRecord item) {
            catalogItems.put(item.id(), item);
            return item;
        }

        @Override
        public Optional<CustomerRedemptionCatalogItemRecord> findRedemptionCatalogItemById(String itemId) {
            return Optional.ofNullable(catalogItems.get(itemId));
        }

        @Override
        public List<CustomerRedemptionCatalogItemRecord> listActiveRedemptionCatalogItems(Instant now) {
            return catalogItems.values().stream()
                    .filter(item -> "active".equals(item.status()))
                    .filter(item -> item.availableFrom() == null || !item.availableFrom().isAfter(now))
                    .filter(item -> item.availableUntil() == null || item.availableUntil().isAfter(now))
                    .toList();
        }

        @Override
        public CustomerPointsRedemptionRecord savePointsRedemption(CustomerPointsRedemptionRecord redemption) {
            redemptions.put(redemption.id(), redemption);
            return redemption;
        }

        @Override
        public List<CustomerPointsRedemptionRecord> listPointsRedemptionsByCustomerId(String customerId) {
            return redemptions.values().stream()
                    .filter(redemption -> customerId.equals(redemption.customerId()))
                    .toList();
        }

        @Override
        public List<CustomerBarReservationOptionResponse> listBarReservationOptions() {
            return barTables.values().stream()
                    .filter(table -> "active".equals(table.status()))
                    .map(table -> new CustomerBarReservationOptionResponse(
                            table.barId(),
                            table.id(),
                            table.tableType(),
                            table.tableName(),
                            table.capacity(),
                            table.allowedPassFreeGuestCount(),
                            table.minConsumptionCent(),
                            table.currency(),
                            table.status()
                    ))
                    .toList();
        }

        @Override
        public Optional<BarTableRecord> findActiveBarTableForReservation(String barId, String tableId, String tableType) {
            return Optional.ofNullable(barTables.get(tableId))
                    .filter(table -> barId.equals(table.barId()))
                    .filter(table -> tableType.equals(table.tableType()))
                    .filter(table -> "active".equals(table.status()));
        }

        @Override
        public BarReservationRecord saveCustomerBarReservation(BarReservationRecord reservation) {
            barReservations.put(reservation.id(), reservation);
            return reservation;
        }
    }

    private static final class InMemoryIdempotencyRepository implements IdempotencyRepository {
        private final Map<String, IdempotencyRecord> records = new LinkedHashMap<>();

        @Override
        public Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey) {
            return Optional.ofNullable(records.get(operationId + ":" + idempotencyKey));
        }

        @Override
        public boolean tryReserve(IdempotencyRecord record) {
            String key = record.operationId() + ":" + record.idempotencyKey();
            if (records.containsKey(key)) {
                return false;
            }
            records.put(key, record);
            return true;
        }

        @Override
        public IdempotencyRecord complete(IdempotencyRecord record) {
            records.put(record.operationId() + ":" + record.idempotencyKey(), record);
            return record;
        }
    }

    private static final class InMemoryAuditEventRepository implements AuditEventRepository {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public AuditEvent save(AuditEvent event) {
            events.add(event);
            return event;
        }
    }
}
