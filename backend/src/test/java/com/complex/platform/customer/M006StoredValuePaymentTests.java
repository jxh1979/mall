package com.complex.platform.customer;

// 文件版本：V0.12
// M006-027：现场储值扣款必须先核验报出的手机号和短信验证码。
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.bar.*;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.FinanceService;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

class M006StoredValuePaymentTests {
    final Instant now = Instant.parse("2026-09-05T00:00:00Z");
    final CustomerRepository customers = mock(CustomerRepository.class);
    final BarRepository bars = mock(BarRepository.class);
    final AuthorizationService auth = mock(AuthorizationService.class);
    final AtomicReference<CustomerStoredValueAccountRecord> balance = new AtomicReference<>();
    final AtomicReference<BarBillRecord> bill = new AtomicReference<>();
    CustomerService customerService;
    StoredValueService storedValue;
    BarBillingService billing;

    @BeforeEach void setup() {
        var customer = new CustomerRecord("customer_1", "13800138000", "138****8000",
                "active", null, now, now, now);
        when(customers.findCustomerById("customer_1")).thenReturn(Optional.of(customer));
        when(customers.findCustomerByPhone("13800138000")).thenReturn(Optional.of(customer));
        balance.set(new CustomerStoredValueAccountRecord("card_1", "customer_1", null, "merchant_1",
                10000L, "active", now, now, "merchant_prepaid"));
        when(customers.findStoredValueAccountForUpdate("customer_1", null, "merchant_1"))
                .thenAnswer(i -> Optional.of(balance.get()));
        when(customers.saveStoredValueAccount(any())).thenAnswer(i -> {balance.set(i.getArgument(0));return balance.get();});
        when(customers.saveStoredValueTransaction(any())).thenAnswer(i -> i.getArgument(0));
        when(auth.currentMerchantScope()).thenReturn(Optional.of("merchant_1"));
        bill.set(new BarBillRecord("bill_1", "B1", null, "bar_1", "merchant_1", null, null,
                3000L, 0, 0, 3000L, 0, "CNY", "unpaid", "unsettled", null, null, now, now,
                false, null, null, null, 0, 0));
        when(bars.findBillByIdForUpdate("bill_1")).thenAnswer(i -> Optional.of(bill.get()));
        when(bars.saveBill(any())).thenAnswer(i -> {bill.set(i.getArgument(0));return bill.get();});
        var audit = mock(AuditEventService.class);
        storedValue = new StoredValueService(customers, mock(FinanceService.class), audit, null, auth, null);
        customerService = new CustomerService(customers, new FixedCustomerOtpVerifier("123456"),
                null, null, audit, null, null, bars, null, null,
                Clock.fixed(now, ZoneOffset.UTC), null, null);
        billing = new BarBillingService(bars, null, auth, audit);
        ReflectionTestUtils.setField(billing, "storedValueService", storedValue);
        ReflectionTestUtils.setField(billing, "customerService", customerService);
    }

    BarBillRecord settle(String phone, String code) throws Exception {
        var mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        String json = mapper.writeValueAsString(Map.of("idempotencyKey", "sms_bill_1",
                "paymentId", "payment_1", "paymentChannel", "stored_value",
                "phone", phone, "smsCode", code));
        BarBillSettleRequest request = mapper.readValue(json, BarBillSettleRequest.class);
        return ReflectionTestUtils.invokeMethod(billing, "settleBill", "bill_1", request);
    }

    void assertUnchanged() {
        assertThat(balance.get().balanceCent()).isEqualTo(10000L);
        assertThat(bill.get().paidAmountCent()).isZero();
        verify(bars, never()).saveBillSettlement(any());
    }

    @Test void appOrderPaysItsAmountOnceFromTheSelectedOwnCard() {
        var tickets = mock(com.complex.platform.ticketing.TicketingRepository.class);
        var order = new AtomicReference<>(new com.complex.platform.ticketing.TicketOrderRecord(
                "ticket_order_1","T1","customer_1","type_1",null,1,null,"app","13800138000",
                2500L,null,2500L,0L,"CNY","unpaid","unissued","created",now,now,null,null,"merchant_1"));
        // JDBC 查单必须取得行锁，避免重复扣款与并发取消。
        when(tickets.findTicketOrderByIdForUpdate("ticket_order_1")).thenAnswer(i -> Optional.of(order.get()));
        when(tickets.updateTicketOrder(any())).thenAnswer(i -> {order.set(i.getArgument(0));return order.get();});
        when(customers.listStoredValueAccountsByCustomer("customer_1")).thenAnswer(i -> List.of(balance.get()));
        when(auth.currentActor()).thenReturn(new com.complex.platform.access.AccessActor(
                "customer_1","customer_1","customer",null,null,"active"));
        ReflectionTestUtils.setField(storedValue, "ticketingRepository", tickets);
        ReflectionTestUtils.setField(storedValue, "customerService", customerService);
        for (int n=0;n<2;n++) {
            ReflectionTestUtils.invokeMethod(storedValue, "payTicketOrder", "customer_1",
                    "ticket_order_1", "card_1", "13800138000", "123456");
        }
        assertThat(balance.get().balanceCent()).isEqualTo(7500L);
        assertThat(order.get().payStatus()).isEqualTo("paid");
        assertThat(order.get().paidAt()).isNotNull();
    }


    @Test void appOrderRejectsForeignOwnershipWrongSmsFrozenBalanceAndWrongCardScope() {
        var tickets = mock(com.complex.platform.ticketing.TicketingRepository.class);
        var currentOrder = new AtomicReference<com.complex.platform.ticketing.TicketOrderRecord>();
        when(tickets.findTicketOrderByIdForUpdate("ticket_order_1")).thenAnswer(i -> Optional.of(currentOrder.get()));
        when(customers.listStoredValueAccountsByCustomer("customer_1")).thenAnswer(i -> List.of(balance.get()));
        when(auth.currentActor()).thenReturn(new com.complex.platform.access.AccessActor(
                "customer_1","customer_1","customer",null,null,"active"));
        ReflectionTestUtils.setField(storedValue, "ticketingRepository", tickets);
        ReflectionTestUtils.setField(storedValue, "customerService", customerService);
        for (String invalid : List.of("foreign_order", "foreign_card", "wrong_phone", "wrong_sms", "frozen", "insufficient", "wrong_merchant", "cancelled")) {
            currentOrder.set(new com.complex.platform.ticketing.TicketOrderRecord(
                    "ticket_order_1","T1","foreign_order".equals(invalid) ? "other_customer" : "customer_1",
                    "type_1",null,1,null,"app","13800138000",2500L,null,2500L,0L,
                    "CNY","unpaid","unissued","cancelled".equals(invalid) ? "cancelled" : "created",now,now,null,null,"merchant_1"));
            balance.set(new CustomerStoredValueAccountRecord("card_1","customer_1",null,
                    "wrong_merchant".equals(invalid) ? "merchant_2" : "merchant_1",
                    "insufficient".equals(invalid) ? 1L : 10000L, "frozen".equals(invalid) ? "frozen" : "active",
                    now,now,"merchant_prepaid"));
            long before = balance.get().balanceCent();
            assertThatThrownBy(() -> storedValue.payTicketOrder("customer_1", "ticket_order_1",
                    "foreign_card".equals(invalid) ? "other_card" : "card_1",
                    "wrong_phone".equals(invalid) ? "13900139000" : "13800138000",
                    "wrong_sms".equals(invalid) ? "999999" : "123456"))
                    .as(invalid).isInstanceOf(ApiException.class);
            assertThat(balance.get().balanceCent()).as(invalid).isEqualTo(before);
        }
        verify(tickets, never()).saveTicketPaymentIntent(any());
        verify(tickets, never()).updateTicketOrder(any());
        verify(customers, never()).saveStoredValueTransaction(any());
    }

    @Test void onlyMatchingGatewayReceiptCreditsTheRechargeAndRepeatDoesNotCreditAgain() {
        var financeRepo = mock(com.complex.platform.finance.FinanceRepository.class);
        var gateway = mock(com.complex.platform.finance.gateway.PaymentGatewayRouter.class);
        when(gateway.isGatewayEnabled()).thenReturn(true);
        var intent = new AtomicReference<>(new com.complex.platform.finance.PaymentIntentRecord(
                "pay_1","stored_value_topup_merchant","customer_1",5000L,"CNY","wechat","created",
                "configured_gateway","ref_1","PENDING",now,now));
        when(financeRepo.findPaymentIntentByIdForUpdate("pay_1")).thenAnswer(i -> Optional.of(intent.get()));
        when(financeRepo.savePaymentIntent(any())).thenAnswer(i -> { intent.set(i.getArgument(0));return intent.get(); });
        var finance = new FinanceService(financeRepo,null,mock(AuditEventService.class),auth,null,gateway,null);
        var service = new StoredValueService(customers,finance,mock(AuditEventService.class),gateway,auth,null);
        when(customers.findStoredValueAccount("customer_1",null,"merchant_1")).thenAnswer(i -> Optional.of(balance.get()));
        when(customers.findStoredValueTopupIntent("pay_1")).thenReturn(Optional.of(
                new StoredValueTopupIntentScope("pay_1","customer_1",null,"merchant_1")));
        var txn = new AtomicReference<CustomerStoredValueTransactionRecord>();
        when(customers.saveStoredValueTransaction(any())).thenAnswer(i -> { txn.set(i.getArgument(0));return txn.get(); });
        when(customers.storedValueTransactionExistsForPaymentIntent("pay_1")).thenAnswer(i -> txn.get()!=null);
        when(customers.findStoredValueTransactionByIdempotencyKey("sv_settle_pay_1")).thenAnswer(i -> Optional.ofNullable(txn.get()));
        for (long amount : new long[] {1L,5000L,5000L}) {
            var receipt = new com.complex.platform.finance.gateway.PaymentGatewayRequest(
                    "stored_value_topup_merchant","customer_1",amount,"CNY","wechat","ref_1","merchant_1","merchant_prepaid");
            when(gateway.queryStatus("ref_1")).thenReturn(new com.complex.platform.finance.gateway.PaymentGatewayResult(
                    com.complex.platform.finance.gateway.PaymentGatewayResult.Status.SUCCEEDED,
                    "configured_gateway","ref_1","",null,receipt));
            if (amount==1L) {
                assertThatThrownBy(() -> service.settleTopup("customer_1",null,"merchant_1","pay_1"))
                        .isInstanceOf(ApiException.class).hasMessageContaining("确认与本次充值单不一致");
                assertThat(balance.get().balanceCent()).isEqualTo(10000L);
            } else {
                service.settleTopup("customer_1",null,"merchant_1","pay_1");
                assertThat(balance.get().balanceCent()).isEqualTo(15000L);
            }
        }
        assertThat(intent.get().gatewayRef()).isEqualTo("ref_1");
        assertThat(txn.get().paymentIntentId()).isEqualTo("pay_1");
    }

    @Test void onlineTopupCannotCreditAPlaceholderSuccessFlag() {
        var finance = mock(FinanceService.class);
        var gateway = com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled();
        var service = new StoredValueService(customers, finance, mock(AuditEventService.class), gateway, auth, null);
        when(finance.findPaymentIntent("pay_1")).thenReturn(Optional.of(new com.complex.platform.finance.PaymentIntentRecord(
                "pay_1","stored_value_topup_merchant","customer_1",5000L,"CNY","wechat","succeeded",now,now)));
        when(customers.findStoredValueTopupIntent("pay_1")).thenReturn(Optional.of(
                new StoredValueTopupIntentScope("pay_1","customer_1",null,"merchant_1")));
        assertThatThrownBy(() -> service.settleTopup("customer_1",null,"merchant_1","pay_1"))
                .isInstanceOf(ApiException.class).hasMessageContaining("支付渠道");
        assertUnchanged();
    }

    @Test void walletHasNoArbitraryAmountConsumeEndpoint() {
        boolean exposed = Arrays.stream(MemberWalletController.class.getDeclaredMethods())
                .map(m -> m.getAnnotation(org.springframework.web.bind.annotation.PostMapping.class))
                .filter(Objects::nonNull).flatMap(a -> Arrays.stream(a.value()))
                .anyMatch("/stored-value/consume"::equals);
        assertThat(exposed).isFalse();
    }

    @Test void requestingCodeWithUnconfiguredProviderFailsInsteadOfReportingSent() {
        ReflectionTestUtils.setField(customerService, "otpVerifier", new FailClosedCustomerOtpVerifier());
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(customerService,
                "requestStoredValueCode", (String) null, "13800138000"))
                .isInstanceOf(ApiException.class).hasMessageContaining("短信服务未配置");
        assertUnchanged();
    }

    @Test void validPhoneAndSmsPayTheActualUnboundBill() throws Exception {
        BarBillRecord result = settle("13800138000", "123456");
        assertThat(balance.get().balanceCent()).isEqualTo(7000L);
        assertThat(result.paidAmountCent()).isEqualTo(3000L);
        assertThat(result.settlementStatus()).isEqualTo("settled");
    }

    @Test void missingSmsCannotDebitAnAlreadyBoundCustomer() {
        when(bars.findOrderById("order_1")).thenReturn(Optional.of(new BarOrderRecord(
                "order_1", "bar_1", "merchant_1", null, null, "customer_1", "app", "created",
                3000L, "CNY", "miniapp", null, null, now, now)));
        BarBillRecord b=bill.get();
        bill.set(new BarBillRecord(b.id(),b.billNo(),"order_1",b.barId(),b.merchantId(),null,null,
                3000L,0,0,3000L,0,"CNY","unpaid","unsettled",null,null,now,now,false,null,null,null,0,0));
        assertThatThrownBy(() -> settle("13800138000", "")).isInstanceOf(ApiException.class);
        assertUnchanged();
    }

    @Test void incorrectSmsDoesNotDebit() {
        assertThatThrownBy(() -> settle("13800138000", "999999"))
                .isInstanceOf(ApiException.class).hasMessageContaining("验证码");
        assertUnchanged();
    }

    @Test void unavailableProductionSmsDoesNotDebit() {
        ReflectionTestUtils.setField(customerService, "otpVerifier", new FailClosedCustomerOtpVerifier());
        assertThatThrownBy(() -> settle("13800138000", "123456"))
                .isInstanceOf(ApiException.class).hasMessageContaining("短信服务未配置");
        assertUnchanged();
    }

    // M006-027：修改商户发卡配置后，已开卡续充仍使用卡本身定格的资金模式。
    @Test void rechargeAfterMerchantConfigChangeUsesTheCardsFrozenFundMode() {
        var finance=mock(FinanceService.class);
        var gateway=mock(com.complex.platform.finance.gateway.PaymentGatewayRouter.class);
        when(gateway.isGatewayEnabled()).thenReturn(true);
        var service=new StoredValueService(customers,finance,mock(AuditEventService.class),gateway,auth,null);
        when(customers.findStoredValueMerchantRefundRule("merchant_1")).thenReturn(Optional.of(
                new StoredValueMerchantRefundRuleRecord("merchant_1",true,10000,true,"platform_collect",now,now)));
        when(customers.findStoredValueAccount("customer_1",null,"merchant_1")).thenAnswer(i->Optional.of(balance.get()));
        var intent=new com.complex.platform.finance.PaymentIntentRecord("pay","stored_value_topup_merchant","customer_1",5000L,"CNY","wechat","created",now,now);
        when(finance.createPaymentIntent(any())).thenReturn(intent);
        when(customers.bindStoredValueTopupIntent(eq("pay"),eq("customer_1"),isNull(),eq("merchant_1"),any())).thenReturn(1);
        service.topup("customer_1",null,"merchant_1",5000L,"wechat","regular-recharge");
        var request=org.mockito.ArgumentCaptor.forClass(com.complex.platform.finance.dto.PaymentIntentRequest.class);
        verify(finance).createPaymentIntent(request.capture());
        assertThat(request.getValue().fundMode()).isEqualTo(balance.get().fundOwnership());
    }
}
