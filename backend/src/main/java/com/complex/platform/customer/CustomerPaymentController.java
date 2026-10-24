package com.complex.platform.customer;

// 文件版本：V0.01

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.bar.BarReservationRecord;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.customer.dto.CustomerPaymentRequest;
import com.complex.platform.customer.dto.CustomerTicketOrderResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** ROOT-03-R4 客人本人订单在线付款发起与回前台查单确认（官方 App/小程序；只许本人订单，金额由服务端定）。 */
@RestController
@RequestMapping("/api/v1/customer")
public class CustomerPaymentController {

    private final CustomerPaymentService service;

    public CustomerPaymentController(CustomerPaymentService service) {
        this.service = service;
    }

    @PostMapping("/orders/{order_id}/payments")
    public ApiResponse<Map<String, Object>> startTicketPayment(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable("order_id") String orderId,
            @RequestBody(required = false) CustomerPaymentRequest request) {
        return ApiResponse.ok(service.startTicketPayment(token(customerToken, authorization), orderId, keyed(idempotencyKey, request)));
    }

    @PostMapping("/orders/{order_id}/payments/{payment_intent_id}/confirm")
    public ApiResponse<CustomerTicketOrderResponse> confirmTicketPayment(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable("order_id") String orderId,
            @PathVariable("payment_intent_id") String paymentIntentId,
            @RequestBody(required = false) CustomerPaymentRequest request) {
        CustomerPaymentRequest keyed = keyed(idempotencyKey, request);
        return ApiResponse.ok(service.confirmTicketPayment(token(customerToken, authorization), orderId, paymentIntentId,
                keyed.idempotencyKey()));
    }

    @PostMapping("/bar-reservations/{reservation_id}/deposit-payments")
    public ApiResponse<Map<String, Object>> startDepositPayment(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable("reservation_id") String reservationId,
            @RequestBody(required = false) CustomerPaymentRequest request) {
        return ApiResponse.ok(service.startDepositPayment(token(customerToken, authorization), reservationId,
                keyed(idempotencyKey, request)));
    }

    @PostMapping("/bar-reservations/{reservation_id}/deposit-payments/{payment_intent_id}/confirm")
    public ApiResponse<BarReservationRecord> confirmDepositPayment(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable("reservation_id") String reservationId,
            @PathVariable("payment_intent_id") String paymentIntentId,
            @RequestBody(required = false) CustomerPaymentRequest request) {
        CustomerPaymentRequest keyed = keyed(idempotencyKey, request);
        return ApiResponse.ok(service.confirmDepositPayment(token(customerToken, authorization), reservationId, paymentIntentId,
                keyed.idempotencyKey()));
    }

    /** Idempotency-Key 头与请求体 idempotency_key 同值（客人端请求协议），二者都缺即 400。 */
    private static CustomerPaymentRequest keyed(String header, CustomerPaymentRequest request) {
        String key = header != null && !header.isBlank() ? header.trim() : request == null ? null : request.idempotencyKey();
        requireIdempotencyKey(key);
        return new CustomerPaymentRequest(key, request == null ? null : request.channel());
    }

    private static String token(String customerToken, String authorization) {
        if (customerToken != null && !customerToken.isBlank()) {
            return customerToken.trim();
        }
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring("Bearer ".length()).trim();
        }
        return customerToken;
    }
}
