package com.complex.platform.customer;

import com.complex.platform.support.InMemoryAuditEventRepository;
import com.complex.platform.support.InMemoryIdempotencyRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.inventory.InventoryService;
import com.complex.platform.inventory.StockMovementRecord;
import com.complex.platform.inventory.dto.StockMovementRequest;
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
    private com.complex.platform.bar.BarRepository barRepository;
    private com.complex.platform.bar.BarOrderService barOrderService;
    private RecordingVoucherIssuePort voucherIssuePort;
    private CustomerService service;

    /** 统一台账发券端口桩（V301）：记录调用并返回派生实例——redeemPoints 只发券不动库存的断言点。 */
    private static final class RecordingVoucherIssuePort implements com.complex.platform.voucher.VoucherIssuePort {
        final List<com.complex.platform.voucher.VoucherInstanceRecord> issued = new ArrayList<>();

        @Override
        public List<com.complex.platform.voucher.VoucherInstanceRecord> issueForPointsRedemption(
                CustomerRedemptionCatalogItemRecord catalogItem, String customerId, String redemptionId,
                int quantity, Instant now) {
            List<com.complex.platform.voucher.VoucherInstanceRecord> batch = new ArrayList<>();
            for (int i = 0; i < quantity; i++) {
                batch.add(new com.complex.platform.voucher.VoucherInstanceRecord(
                        "vi_" + redemptionId + "_" + i, "vdef_cat_" + catalogItem.id(), customerId,
                        "VC" + redemptionId + i, "points_redemption", redemptionId, "issued", now,
                        null, null, null, null, false, now, now));
            }
            issued.addAll(batch);
            return batch;
        }
    }

    @BeforeEach
    void setUp() {
        customerRepository = new InMemoryCustomerRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        ticketingService = Mockito.mock(TicketingService.class);
        inventoryService = Mockito.mock(InventoryService.class);
        AuditEventService auditEventService = new AuditEventService(auditEventRepository);
        AuditedOperationExecutor operations = new AuditedOperationExecutor(
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                auditEventService
        );
        LoyaltyService loyaltyService = new LoyaltyService(customerRepository, auditEventService);
        barRepository = Mockito.mock(com.complex.platform.bar.BarRepository.class);
        barOrderService = Mockito.mock(com.complex.platform.bar.BarOrderService.class);
        voucherIssuePort = new RecordingVoucherIssuePort();
        service = new CustomerService(
                customerRepository,
                new FixedCustomerOtpVerifier("000000"),
                ticketingService,
                operations,
                auditEventService,
                inventoryService,
                loyaltyService,
                barRepository,
                barOrderService,
                voucherIssuePort,
                java.time.Clock.fixed(java.time.Instant.parse("2026-06-14T00:00:00Z"), java.time.ZoneOffset.UTC),
                new com.complex.platform.common.ratelimit.RateLimiter(),
                new com.complex.platform.common.ratelimit.RateLimitProperties()
        );
    }

    @Test
    void barScanMenuReturnsSafeProjectionWithoutInternalPrice() {
        // 客人扫码看菜单：安全投影只给对客售卖信息；DTO 类型本身不含 internal_settlement_price（编译期即隐去敏感成本）。
        CustomerLoginResponse login = service.phoneLogin(new CustomerPhoneLoginRequest("13800138000", "000000", "miniapp"));
        java.time.Instant now = java.time.Instant.now();
        Mockito.when(barRepository.findTableSessionById("session_s1")).thenReturn(Optional.of(
                new com.complex.platform.bar.BarTableSessionRecord("session_s1", "bar_1", "merchant_1",
                        java.time.LocalDate.parse("2026-06-24"), "table_1", null, null, 2, 0L, 0, null, null,
                        "open", now, null, null, 1L, now, now)));
        Mockito.when(barRepository.listProducts(Optional.of("merchant_1"))).thenReturn(List.of(
                new com.complex.platform.bar.BarProductRecord("product_1", "bar_1", "merchant_1", "精酿", "beer",
                        3800L, "CNY", null, null, false, "active", now, now, null, null, null, false, null, null, 1500L)));

        PageResponse<com.complex.platform.customer.dto.CustomerBarMenuItemResponse> menu =
                service.listBarScanMenu(login.customerToken(), "session_s1");

        assertThat(menu.items()).hasSize(1);
        com.complex.platform.customer.dto.CustomerBarMenuItemResponse item = menu.items().get(0);
        assertThat(item.productId()).isEqualTo("product_1");
        assertThat(item.priceCent()).isEqualTo(3800L);
        assertThat(item.productType()).isEqualTo("beer");
    }

    @Test
    void customerScanOrderDelegatesToBarOrderServiceWithCustomerId() {
        // 客人自助扫码下单：委托 BarOrderService.placeCustomerScanOrder，传本人 customerId + 会话 id。
        CustomerLoginResponse login = service.phoneLogin(new CustomerPhoneLoginRequest("13800138000", "000000", "miniapp"));
        String customerId = customerRepository.findSessionByToken(login.customerToken()).orElseThrow().customerId();
        java.time.Instant now = java.time.Instant.now();
        com.complex.platform.bar.BarOrderRecord created = new com.complex.platform.bar.BarOrderRecord(
                "order_1", "bar_1", "merchant_1", "table_1", null, customerId, "app", "created",
                7600L, "CNY", "miniapp", null, null, now, now);
        Mockito.when(barOrderService.placeCustomerScanOrder(Mockito.eq("session_s1"), Mockito.any(), Mockito.eq(customerId)))
                .thenReturn(created);

        com.complex.platform.bar.BarOrderRecord order = service.placeBarScanOrder(
                login.customerToken(), "scan-key-1", "session_s1",
                new com.complex.platform.customer.dto.CustomerBarScanOrderRequest(
                        "scan-key-1", List.of(Map.of("product_id", "product_1", "quantity", 2))));

        assertThat(order.orderSource()).isEqualTo("app");
        assertThat(order.customerId()).isEqualTo(customerId);
        Mockito.verify(barOrderService)
                .placeCustomerScanOrder(Mockito.eq("session_s1"), Mockito.any(), Mockito.eq(customerId));
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
    void phoneLoginCustomerIdDoesNotLeakPhone() {
        String phone = "13800138000";
        CustomerLoginResponse response = service.phoneLogin(new CustomerPhoneLoginRequest(phone, "000000", "miniapp"));

        // 主键内嵌明文手机号会架空 phone 列的 AES-GCM 加密; id 必须与手机号无关且不可反推.
        assertThat(response.customerId()).startsWith("customer_");
        assertThat(response.customerId()).doesNotContain(phone);
        assertThat(response.customerId()).isNotEqualTo("customer_" + phone);
    }

    @Test
    void accountLifecycleSuspendBlacklistDeregisterEnforcedAtLoginAndActions() {
        // V194：四态账号生命周期闸——封禁/黑名单/注销拦截登录与动作；数据保留。
        CustomerLoginResponse login = service.phoneLogin(new CustomerPhoneLoginRequest("13900001234", "000000", "miniapp"));
        String customerId = customerRepository.findSessionByToken(login.customerToken()).orElseThrow().customerId();

        // 封禁(未到期，fixed clock 2026-06-14 → until 2026-06-20) → 动作被拒、登录被拒。
        service.suspendCustomer(customerId, java.time.Instant.parse("2026-06-20T00:00:00Z"), "risk");
        assertThatThrownBy(() -> service.listOrders(login.customerToken()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CUSTOMER_SUSPENDED"));
        assertThatThrownBy(() -> service.phoneLogin(new CustomerPhoneLoginRequest("13900001234", "000000", "miniapp")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CUSTOMER_SUSPENDED"));

        // 恢复 → 可用。
        service.reinstateCustomer(customerId, "appeal ok");
        assertThat(service.phoneLogin(new CustomerPhoneLoginRequest("13900001234", "000000", "miniapp")).customerToken()).isNotBlank();

        // 拉黑(永久) → 登录被拒。
        service.blacklistCustomer(customerId, "fraud");
        assertThatThrownBy(() -> service.phoneLogin(new CustomerPhoneLoginRequest("13900001234", "000000", "miniapp")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CUSTOMER_BLACKLISTED"));
        service.reinstateCustomer(customerId, "unblock");

        // 客人自助注销 → 后续动作被拒；客人行仍在库(数据物理保留)。
        CustomerLoginResponse s2 = service.phoneLogin(new CustomerPhoneLoginRequest("13900001234", "000000", "miniapp"));
        service.deregisterAccount(s2.customerToken());
        assertThatThrownBy(() -> service.listOrders(s2.customerToken()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CUSTOMER_DEREGISTERED"));
        assertThat(customerRepository.findCustomerById(customerId)).isPresent();
    }

    @Test
    void logoutRevokesServerSideSessionAndIsIdempotent() {
        CustomerLoginResponse login = service.phoneLogin(new CustomerPhoneLoginRequest("13900001234", "000000", "miniapp"));
        assertThat(customerRepository.findSessionByToken(login.customerToken())).isPresent();

        service.logout(login.customerToken());

        // AUD-030：登出后服务端会话置 logged_out，凭该 token 的后续动作按失效拒绝（不再靠客户端丢弃本地凭证）。
        assertThatThrownBy(() -> service.listOrders(login.customerToken()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CUSTOMER_UNAUTHENTICATED"));
        // 幂等：重复登出与空 token 不报错。
        service.logout(login.customerToken());
        service.logout(null);
    }

    @Test
    void suspensionAutoRestoresWhenExpiredOnNextLogin() {
        // V194：封禁到期(until 早于 fixed clock 2026-06-14) → 下次登录 lazy 自动恢复 active。
        CustomerLoginResponse login = service.phoneLogin(new CustomerPhoneLoginRequest("13900005678", "000000", "miniapp"));
        String customerId = customerRepository.findSessionByToken(login.customerToken()).orElseThrow().customerId();
        service.suspendCustomer(customerId, java.time.Instant.parse("2026-06-10T00:00:00Z"), "temp");
        assertThat(service.phoneLogin(new CustomerPhoneLoginRequest("13900005678", "000000", "miniapp")).customerToken()).isNotBlank();
        assertThat(customerRepository.findCustomerById(customerId).orElseThrow().status()).isEqualTo("active");
    }

    @Test
    void listOrderTicketsDelegatesAndEnforcesAccountGate() {
        // 1.8：我的票走 requireCustomer 状态闸；正常账号取到票实例投影，封禁账号被拒。
        CustomerLoginResponse login = service.phoneLogin(new CustomerPhoneLoginRequest("13900007777", "000000", "miniapp"));
        String customerId = customerRepository.findSessionByToken(login.customerToken()).orElseThrow().customerId();
        com.complex.platform.ticketing.CustomerTicketResponse ticket =
                new com.complex.platform.ticketing.CustomerTicketResponse(
                        "ent_1", "全日通票", "ticket", "active", 2, 0, 2, 1, null, null);
        Mockito.when(ticketingService.listCustomerOrderTickets("order_1", customerId))
                .thenReturn(java.util.List.of(ticket));

        PageResponse<com.complex.platform.ticketing.CustomerTicketResponse> page =
                service.listOrderTickets(login.customerToken(), "order_1");
        assertThat(page.items()).singleElement().extracting(t -> t.id()).isEqualTo("ent_1");

        service.suspendCustomer(customerId, java.time.Instant.parse("2026-06-20T00:00:00Z"), "risk");
        assertThatThrownBy(() -> service.listOrderTickets(login.customerToken(), "order_1"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CUSTOMER_SUSPENDED"));
    }

    @Test
    void phoneLoginRejectsInvalidSmsCodeWithoutCreatingSession() {
        assertThatThrownBy(() -> service.phoneLogin(new CustomerPhoneLoginRequest(
                "13800138000",
                "111111",
                "miniapp"
        ))).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("CUSTOMER_OTP_INVALID"));

        assertThat(customerRepository.sessions).isEmpty();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("customer.auth.login_failed");
    }

    @Test
    void ticketProductsFilterByBarAndResolveName() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        TicketTypeRecord pass = new TicketTypeRecord(
                "tt_pass", "综合体通票", "weekday_weekend_holiday", 9900L, 9900L, 9900L,
                false, "published", null, null, now, now);
        TicketTypeRecord barA = new TicketTypeRecord(
                "tt_bar_a", "周末单人票", "weekday_weekend_holiday", 9900L, 9900L, 9900L,
                false, "published", null, null, now, now, null, 0, null, false, "mch_bar_a");
        when(ticketingService.listTicketTypes()).thenReturn(new PageResponse<>(List.of(pass, barA), 2, 50, 2));
        when(ticketingService.resolveBarName("mch_bar_a")).thenReturn("话梅酒馆");

        // 公共入口：全量（通票 + 单店票）。
        assertThat(service.listTicketProducts(null).items()).hasSize(2);
        // 专属入口：锁定 bar_a，只见该酒吧单店票，且带实时酒吧名。
        PageResponse<CustomerTicketProductResponse> onlyA = service.listTicketProducts("mch_bar_a");
        assertThat(onlyA.items()).singleElement().satisfies(pr -> {
            assertThat(pr.barId()).isEqualTo("mch_bar_a");
            assertThat(pr.barName()).isEqualTo("话梅酒馆");
        });
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
                null,
                null,
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        )), 1, 50, 1));

        PageResponse<CustomerTicketProductResponse> response = service.listTicketProducts(null);

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

    /**
     * V368 人脸照同意留痕（用户 2026-07-28 裁决「要，落到同意台账」）。
     *
     * <p>客人在拍照告知上勾了同意 → 同意台账（customer_consents）必须多出一条：谁、什么时候、
     * 同意了哪一版告知；并且这张照片挂着这条留痕的 id。将来被问「这位客人同意过吗、同意的是什么」，
     * 答案就在这一条上，不再是浏览器里一个转瞬即逝的勾。
     */
    @Test
    void facePhotoOrderRecordsConsentTraceableToCustomerAndNoticeVersion() {
        CustomerLoginResponse login = service.phoneLogin(new CustomerPhoneLoginRequest("13800138000", "000000", "miniapp"));
        when(ticketingService.facePhotoNoticeVersion()).thenReturn("face_photo_notice_v1@retention_30d");
        when(ticketingService.createTicketOrder(any(TicketOrderRequest.class))).thenReturn(faceOrderRecord(login));

        service.createTicketOrder(login.customerToken(), "face-order-1", new CustomerTicketOrderRequest(
                "face-order-1", "ticket_type_1", null, 1, LocalDate.parse("2026-06-20"),
                "13800138000", null, null, List.of("Zm9v"), true));

        // ① 同意台账里确实多了一条，且能定位到人、时间、告知版本。
        CustomerConsentRecord consent = customerRepository.consents.values().stream()
                .filter(c -> "ticket_face_photo".equals(c.consentType()))
                .findFirst().orElseThrow();
        assertThat(consent.customerId()).isEqualTo(login.customerId());
        assertThat(consent.accepted()).isTrue();
        assertThat(consent.version()).isEqualTo("face_photo_notice_v1@retention_30d");
        assertThat(consent.acceptedAt()).isNotNull();
        assertThat(consent.sourceTerminal()).isEqualTo("miniapp");

        // ② 照片带着这条留痕的 id 下去——照片与同意是同一次业务动作，查得到彼此。
        ArgumentCaptor<TicketOrderRequest> captor = ArgumentCaptor.forClass(TicketOrderRequest.class);
        verify(ticketingService).createTicketOrder(captor.capture());
        assertThat(captor.getValue().faceConsentId()).isEqualTo(consent.id());
        assertThat(captor.getValue().facePhotos()).containsExactly("Zm9v");
    }

    /** V368：没有同意就不该有照片——没勾同意直接拒，订单不建、同意台账不落、照片一个字节都进不去。 */
    @Test
    void facePhotoOrderWithoutConsentIsRejectedAndNothingIsPersisted() {
        CustomerLoginResponse login = service.phoneLogin(new CustomerPhoneLoginRequest("13800138000", "000000", "miniapp"));

        assertThatThrownBy(() -> service.createTicketOrder(login.customerToken(), "face-order-2",
                new CustomerTicketOrderRequest("face-order-2", "ticket_type_1", null, 1,
                        LocalDate.parse("2026-06-20"), "13800138000", null, null, List.of("Zm9v"), null)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).code()).isEqualTo("FACE_PHOTO_CONSENT_REQUIRED"));

        assertThat(customerRepository.consents.values()).noneMatch(c -> "ticket_face_photo".equals(c.consentType()));
        verify(ticketingService, never()).createTicketOrder(any(TicketOrderRequest.class));
    }

    /** 不需要脸照的票种一律不问同意（PIPL 最小必要）：没拍照就不该有同意留痕。 */
    @Test
    void orderWithoutFacePhotoRecordsNoFaceConsent() {
        CustomerLoginResponse login = service.phoneLogin(new CustomerPhoneLoginRequest("13800138000", "000000", "miniapp"));
        when(ticketingService.createTicketOrder(any(TicketOrderRequest.class))).thenReturn(faceOrderRecord(login));

        service.createTicketOrder(login.customerToken(), "plain-order-1", new CustomerTicketOrderRequest(
                "plain-order-1", "ticket_type_1", null, 1, LocalDate.parse("2026-06-20"),
                "13800138000", null));

        assertThat(customerRepository.consents.values()).noneMatch(c -> "ticket_face_photo".equals(c.consentType()));
        ArgumentCaptor<TicketOrderRequest> captor = ArgumentCaptor.forClass(TicketOrderRequest.class);
        verify(ticketingService).createTicketOrder(captor.capture());
        assertThat(captor.getValue().faceConsentId()).isNull();
    }

    private TicketOrderRecord faceOrderRecord(CustomerLoginResponse login) {
        return new TicketOrderRecord(
                "ticket_order_face", "TO20260614000009", login.customerId(), "ticket_type_1", null, 1,
                LocalDate.parse("2026-06-20"), "miniapp", "13800138000", 9900L, "CNY", "unpaid",
                "not_issued", "created", Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z"));
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
    void consentIdsDoNotCollideForLongKeysDifferingPastTruncationCutoff() {
        CustomerLoginResponse login = service.phoneLogin(new CustomerPhoneLoginRequest("13800138000", "000000", "miniapp"));
        // Two distinct idempotency keys that are identical for the first 45 chars and differ only afterwards.
        String prefix = "a".repeat(45);
        String key1 = prefix + "-tail-one";
        String key2 = prefix + "-tail-two";

        CustomerConsentRecord consent1 = service.recordConsent(login.customerToken(), key1,
                new CustomerConsentRequest(key1, "privacy", true, "v1", "miniapp"));
        CustomerConsentRecord consent2 = service.recordConsent(login.customerToken(), key2,
                new CustomerConsentRequest(key2, "marketing", true, "v1", "miniapp"));

        // Before the fix both slugged to the same 40-char id and the second overwrote the first.
        assertThat(consent2.id()).isNotEqualTo(consent1.id());
        assertThat(consent1.consentType()).isEqualTo("privacy");
        assertThat(consent2.consentType()).isEqualTo("marketing");
    }

    @Test
    void barReservationOptionsAndCreateUseCustomerTokenWithoutStaffPermission() {
        CustomerLoginResponse login = service.phoneLogin(new CustomerPhoneLoginRequest("13800138000", "000000", "miniapp"));
        customerRepository.saveBarTable(new BarTableRecord(
                "table_customer",
                "bar_1",
                null,
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

    // P0#3: 订台定金按 bar_deposit_rules 计算（固定额规则 → deposit pending + 金额按规则）。
    @Test
    void barReservationComputesDepositFromFixedRule() {
        CustomerLoginResponse login = service.phoneLogin(new CustomerPhoneLoginRequest("13800138000", "000000", "miniapp"));
        customerRepository.saveBarTable(new BarTableRecord(
                "table_deposit", "bar_dep", null, "T-DEP", "VIP", "card_seat",
                6, 2, 50000L, "CNY", "active",
                Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")));
        customerRepository.depositRules.put("rule_fixed", new com.complex.platform.bar.BarDepositRuleRecord(
                "rule_fixed", "bar_dep", null, "bar", "bar_dep", "fixed",
                20000L, null, 60, true, Instant.now(), Instant.now()));

        BarReservationRecord reservation = service.createBarReservation(login.customerToken(), "dep-fixed",
                new CustomerBarReservationRequest(null, "bar_dep", "table_deposit", "card_seat",
                        Instant.parse("2026-06-20T21:00:00Z"), 2, "13800138000"));

        assertThat(reservation.depositAmountCent()).isEqualTo(20000L);
        assertThat(reservation.depositStatus()).isEqualTo("pending");
    }

    // P0#3: 比例规则（min_consumption_ratio）→ 低消 × ratioBps/10000。
    @Test
    void barReservationComputesDepositFromRatioRule() {
        CustomerLoginResponse login = service.phoneLogin(new CustomerPhoneLoginRequest("13800138000", "000000", "miniapp"));
        customerRepository.saveBarTable(new BarTableRecord(
                "table_ratio", "bar_ratio", null, "T-R", "VIP", "card_seat",
                6, 2, 100000L, "CNY", "active",
                Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")));
        customerRepository.depositRules.put("rule_ratio", new com.complex.platform.bar.BarDepositRuleRecord(
                "rule_ratio", "bar_ratio", null, "table_type", "card_seat", "min_consumption_ratio",
                null, 3000 /* 30% */, 0, true, Instant.now(), Instant.now()));

        BarReservationRecord reservation = service.createBarReservation(login.customerToken(), "dep-ratio",
                new CustomerBarReservationRequest(null, "bar_ratio", "table_ratio", "card_seat",
                        Instant.parse("2026-06-20T21:00:00Z"), 2, "13800138000"));

        assertThat(reservation.depositAmountCent()).isEqualTo(30000L); // 100000 * 3000 / 10000
        assertThat(reservation.depositStatus()).isEqualTo("pending");
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
                0L,
                "active",
                Instant.parse("2026-06-14T00:00:00Z"),
                0L,
                null,
                null,
                0,
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
                null,
                null,
                false,
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
                null,
                null,
                false,
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        customerRepository.seedRedemptionCatalogItem(new CustomerRedemptionCatalogItemRecord(
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
        customerRepository.seedRedemptionCatalogItem(new CustomerRedemptionCatalogItemRecord(
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
    void redeemPointsDeductsBalanceIssuesUnifiedVoucherWithoutInventoryCall() {
        CustomerLoginResponse login = service.phoneLogin(new CustomerPhoneLoginRequest("13800138000", "000000", "miniapp"));
        customerRepository.savePoints(new CustomerPointsAccountRecord(
                "points_" + login.customerId(),
                login.customerId(),
                500L,
                "silver",
                Map.of(),
                0L,
                "active",
                Instant.parse("2026-06-14T00:00:00Z"),
                0L,
                null,
                null,
                0,
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        customerRepository.seedRedemptionCatalogItem(new CustomerRedemptionCatalogItemRecord(
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
                .containsExactly(login.customerId(), "gift_active", 120L, "voucher_issued");
        assertThat(customerRepository.findPointsByCustomerId(login.customerId()).orElseThrow().balance()).isEqualTo(380L);
        assertThat(customerRepository.pointsLedger)
                .extracting(CustomerPointsLedgerRecord::pointsDelta, CustomerPointsLedgerRecord::balanceAfter, CustomerPointsLedgerRecord::sourceType)
                .contains(org.assertj.core.groups.Tuple.tuple(-120L, 380L, "points_redemption"));
        // V301 统一台账：兑换只发券（每件一张、source_ref=兑换记录 id），绝不直接扣库存。
        assertThat(voucherIssuePort.issued).hasSize(1);
        assertThat(voucherIssuePort.issued.get(0).sourceRef()).isEqualTo(redemption.id());
        assertThat(voucherIssuePort.issued.get(0).customerId()).isEqualTo(login.customerId());
        verify(inventoryService, never()).createTypedStockMovement(any(), any(StockMovementRequest.class));
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
                0L,
                "active",
                Instant.parse("2026-06-14T00:00:00Z"),
                0L,
                null,
                null,
                0,
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        customerRepository.seedRedemptionCatalogItem(new CustomerRedemptionCatalogItemRecord(
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
                .hasMessageContaining("积分余额不足");

        assertThat(customerRepository.findPointsByCustomerId(login.customerId()).orElseThrow().balance()).isEqualTo(50L);
        verify(inventoryService, never()).createTypedStockMovement(any(), any(StockMovementRequest.class));
        // 余额不足被挡下时也不得发券。
        assertThat(voucherIssuePort.issued).isEmpty();
    }

    @Test
    void redeemPointsRejectsFrozenAccountWithoutTouchingInventoryOrPoints() {
        // R3 P0-7 顺序修回归：冻结账户校验必须在扣库存之前——余额足够但账户冻结时，绝不能先扣库存。
        // 此前冻结校验误排在 createTypedStockMovement 之后，仅靠同事务回滚兜底；本用例以 never() 锁死顺序。
        CustomerLoginResponse login = service.phoneLogin(new CustomerPhoneLoginRequest("13800138000", "000000", "miniapp"));
        customerRepository.savePoints(new CustomerPointsAccountRecord(
                "points_" + login.customerId(),
                login.customerId(),
                500L,
                "silver",
                Map.of(),
                0L,
                "frozen",
                Instant.parse("2026-06-14T00:00:00Z"),
                0L,
                null,
                null,
                0,
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        customerRepository.seedRedemptionCatalogItem(new CustomerRedemptionCatalogItemRecord(
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
                "redeem-frozen",
                new CustomerPointsRedemptionRequest(null, "gift_active", 1, "miniapp")
        ))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("积分账户已被冻结");

        // 库存零调用：冻结校验在扣库存之前把请求挡下。
        verify(inventoryService, never()).createTypedStockMovement(any(), any(StockMovementRequest.class));
        // 积分未扣、无兑换记录、无扣分流水。
        assertThat(customerRepository.findPointsByCustomerId(login.customerId()).orElseThrow().balance()).isEqualTo(500L);
        assertThat(customerRepository.listPointsRedemptionsByCustomerId(login.customerId())).isEmpty();
        assertThat(customerRepository.pointsLedger)
                .noneMatch(entry -> "points_redemption".equals(entry.sourceType()));
        // 冻结账户被挡下时也不得发券。
        assertThat(voucherIssuePort.issued).isEmpty();
    }


    @Test
    void redeemPointsSeesCurrentBalanceAfterConcurrentModification() {
        // RED: before fix, service uses findPointsByCustomerId (no lock), hook never fires,
        // balance reads as 120, passes check, returns success — test expects exception → FAIL.
        // GREEN: after fix, service uses findPointsByCustomerIdForUpdate, hook fires first,
        // sets balance to 0, service reads 0 < 120 and throws POINTS_INSUFFICIENT → PASS.
        CustomerLoginResponse login = service.phoneLogin(new CustomerPhoneLoginRequest("13800138099", "000000", "miniapp"));
        customerRepository.savePoints(new CustomerPointsAccountRecord(
                "points_" + login.customerId(),
                login.customerId(),
                120L,
                "base",
                Map.of(),
                0L,
                "active",
                Instant.parse("2026-06-14T00:00:00Z"),
                0L,
                null,
                null,
                0,
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        customerRepository.seedRedemptionCatalogItem(new CustomerRedemptionCatalogItemRecord(
                "gift_race_item",
                "Race Gift",
                "gift",
                120L,
                "item_race",
                "race_shelf",
                1,
                "active",
                null,
                null,
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));

        // Simulate a concurrent transaction draining balance to 0 before the lock is acquired
        customerRepository.beforeFindForUpdate = () -> {
            CustomerPointsAccountRecord current = customerRepository.findPointsByCustomerId(login.customerId()).orElseThrow();
            customerRepository.savePoints(new CustomerPointsAccountRecord(
                    current.id(), current.customerId(), 0L, current.level(),
                    Map.of(), current.frozenPoints(), current.accountStatus(), current.levelPeriodStart(),
                    current.keepThresholdProgress(), current.graceUntil(), current.lastCheckInDate(),
                    current.checkInStreak(), current.createdAt(), Instant.parse("2026-06-14T00:00:01Z")
            ));
        };

        assertThatThrownBy(() -> service.redeemPoints(
                login.customerToken(),
                "redeem-race-concurrent",
                new CustomerPointsRedemptionRequest(null, "gift_race_item", 1, "miniapp")
        ))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("积分余额不足");

        assertThat(customerRepository.findPointsByCustomerId(login.customerId()).orElseThrow().balance()).isEqualTo(0L);
    }

    private static class InMemoryCustomerRepository implements CustomerRepository {
        private final Map<String, CustomerRecord> customers = new LinkedHashMap<>();
        private final Map<String, CustomerSessionRecord> sessions = new LinkedHashMap<>();
        private final Map<String, CustomerConsentRecord> consents = new LinkedHashMap<>();
        private final Map<String, CustomerPointsAccountRecord> points = new LinkedHashMap<>();
        private final List<CustomerPointsLedgerRecord> pointsLedger = new ArrayList<>();
        private final Map<String, CustomerRedemptionCatalogItemRecord> catalogItems = new LinkedHashMap<>();
        private final Map<String, CustomerPointsRedemptionRecord> redemptions = new LinkedHashMap<>();
        private final Map<String, BarTableRecord> barTables = new LinkedHashMap<>();
        private final Map<String, BarReservationRecord> barReservations = new LinkedHashMap<>();
        Runnable beforeFindForUpdate = null;

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
        public int revokeSessionByToken(String token) {
            CustomerSessionRecord session = sessions.get(token);
            if (session == null || !"active".equals(session.status())) {
                return 0;
            }
            sessions.put(token, new CustomerSessionRecord(session.id(), session.customerId(), session.customerToken(),
                    session.scene(), session.issuedAt(), session.expiresAt(), "logged_out"));
            return 1;
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
        public Optional<CustomerPointsAccountRecord> findPointsByCustomerIdForUpdate(String customerId) {
            if (beforeFindForUpdate != null) {
                beforeFindForUpdate.run();
            }
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

        public CustomerRedemptionCatalogItemRecord seedRedemptionCatalogItem(CustomerRedemptionCatalogItemRecord item) {
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

        final Map<String, com.complex.platform.bar.BarDepositRuleRecord> depositRules = new LinkedHashMap<>();

        @Override
        public List<com.complex.platform.bar.BarDepositRuleRecord> listDepositRules(String barId) {
            return depositRules.values().stream()
                    .filter(r -> barId.equals(r.barId()) && r.enabled())
                    .sorted((a, b) -> b.scopeType().compareTo(a.scopeType()))
                    .toList();
        }

        @Override
        public BarReservationRecord saveCustomerBarReservation(BarReservationRecord reservation) {
            barReservations.put(reservation.id(), reservation);
            return reservation;
        }

        final Map<String, CustomerMemberLevelRecord> memberLevels = new LinkedHashMap<>();
        final Map<String, CustomerCheckInTierRecord> checkInTiers = new LinkedHashMap<>();
        final Map<String, CustomerBirthdayRuleRecord> birthdayRules = new LinkedHashMap<>();
        final Map<String, CustomerCheckInRecord> checkIns = new LinkedHashMap<>();

        @Override
        public List<CustomerPointsAccountRecord> listAllPointsAccounts() {
            return new ArrayList<>(points.values());
        }

        @Override
        public CustomerMemberLevelRecord saveMemberLevel(CustomerMemberLevelRecord level) {
            memberLevels.put(level.id(), level);
            return level;
        }

        @Override
        public List<CustomerMemberLevelRecord> listMemberLevels(String projectId) {
            return memberLevels.values().stream()
                    .filter(l -> "active".equals(l.status()))
                    .sorted((a, b) -> Integer.compare(a.levelRank(), b.levelRank()))
                    .toList();
        }

        @Override
        public CustomerCheckInTierRecord saveCheckInTier(CustomerCheckInTierRecord tier) {
            checkInTiers.put(tier.id(), tier);
            return tier;
        }

        @Override
        public List<CustomerCheckInTierRecord> listCheckInTiers(String projectId) {
            return checkInTiers.values().stream()
                    .filter(t -> "active".equals(t.status()))
                    .sorted((a, b) -> Integer.compare(a.streakDays(), b.streakDays()))
                    .toList();
        }

        @Override
        public CustomerBirthdayRuleRecord saveBirthdayRule(CustomerBirthdayRuleRecord rule) {
            birthdayRules.put(rule.id(), rule);
            return rule;
        }

        @Override
        public List<CustomerBirthdayRuleRecord> listActiveBirthdayRules(String projectId) {
            return birthdayRules.values().stream()
                    .filter(r -> "active".equals(r.status()))
                    .toList();
        }

        @Override
        public CustomerCheckInRecord saveCheckIn(CustomerCheckInRecord checkIn) {
            checkIns.put(checkIn.customerId() + ":" + checkIn.checkInDate(), checkIn);
            return checkIn;
        }

        @Override
        public Optional<CustomerCheckInRecord> findCheckIn(String customerId, java.time.LocalDate checkInDate) {
            return Optional.ofNullable(checkIns.get(customerId + ":" + checkInDate));
        }

        @Override
        public List<CustomerCheckInRecord> listCheckIns(String customerId) {
            return checkIns.values().stream()
                    .filter(c -> customerId.equals(c.customerId()))
                    .sorted((a, b) -> b.checkInDate().compareTo(a.checkInDate()))
                    .toList();
        }

        @Override
        public List<CustomerPointsLedgerRecord> listExpirableBatches(String customerId, Instant now) {
            return pointsLedger.stream()
                    .filter(l -> customerId.equals(l.customerId()))
                    .filter(l -> l.pointsDelta() > 0 && !l.expired() && l.expireAt() != null)
                    .filter(l -> !l.expireAt().isAfter(now))
                    .filter(l -> l.remainingPoints() != null && l.remainingPoints() > 0)
                    .sorted((a, b) -> a.expireAt().compareTo(b.expireAt()))
                    .toList();
        }

        @Override
        public List<CustomerPointsLedgerRecord> listOpenBatches(String customerId) {
            return pointsLedger.stream()
                    .filter(l -> customerId.equals(l.customerId()))
                    .filter(l -> l.pointsDelta() > 0 && !l.expired() && l.expireAt() != null)
                    .filter(l -> l.remainingPoints() != null && l.remainingPoints() > 0)
                    .sorted((a, b) -> a.expireAt().compareTo(b.expireAt()))
                    .toList();
        }

        @Override
        public void markBatchExpired(String ledgerId) {
            replaceLedger(ledgerId, l -> new CustomerPointsLedgerRecord(
                    l.id(), l.customerId(), l.pointsDelta(), l.balanceAfter(), l.sourceType(),
                    l.sourceId(), l.description(), l.expireAt(), 0L, true, l.createdAt()));
        }

        @Override
        public void consumeBatchRemaining(String ledgerId, long remainingPoints) {
            replaceLedger(ledgerId, l -> new CustomerPointsLedgerRecord(
                    l.id(), l.customerId(), l.pointsDelta(), l.balanceAfter(), l.sourceType(),
                    l.sourceId(), l.description(), l.expireAt(), remainingPoints, l.expired(), l.createdAt()));
        }

        private void replaceLedger(String ledgerId, java.util.function.UnaryOperator<CustomerPointsLedgerRecord> op) {
            for (int i = 0; i < pointsLedger.size(); i++) {
                if (pointsLedger.get(i).id().equals(ledgerId)) {
                    pointsLedger.set(i, op.apply(pointsLedger.get(i)));
                    return;
                }
            }
        }
    }
}
