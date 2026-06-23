package com.complex.platform.customer;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.bar.BarReservationInvitationMemberRecord;
import com.complex.platform.bar.BarReservationInvitationRecord;
import com.complex.platform.bar.BarReservationRecord;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.customer.dto.CustomerBarInvitationConfirmRequest;
import com.complex.platform.customer.dto.CustomerBarReservationRequest;
import com.complex.platform.customer.dto.CustomerCheckInRequest;
import com.complex.platform.customer.dto.CustomerConsentRequest;
import com.complex.platform.customer.dto.CustomerPhoneLoginRequest;
import com.complex.platform.customer.dto.CustomerPointsRedemptionRequest;
import com.complex.platform.customer.dto.CustomerTicketOrderRequest;
import com.complex.platform.ticketing.TicketCredentialResponse;
import com.complex.platform.ticketing.TicketOrderRecord;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @PostMapping("/auth/phone-login")
    public ApiResponse<CustomerLoginResponse> phoneLogin(@RequestBody CustomerPhoneLoginRequest request) {
        return ApiResponse.ok(service.phoneLogin(request));
    }

    @GetMapping("/ticket-products")
    public ApiResponse<PageResponse<CustomerTicketProductResponse>> listTicketProducts() {
        return ApiResponse.ok(service.listTicketProducts());
    }

    @PostMapping("/ticket-orders")
    public ApiResponse<TicketOrderRecord> createTicketOrder(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CustomerTicketOrderRequest request
    ) {
        return ApiResponse.ok(service.createTicketOrder(
                requireCustomerToken(customerToken, authorization),
                firstNonBlank(idempotencyKey, request == null ? null : request.idempotencyKey()),
                request
        ));
    }

    @GetMapping("/orders")
    public ApiResponse<PageResponse<TicketOrderRecord>> listOrders(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.ok(service.listOrders(requireCustomerToken(customerToken, authorization)));
    }

    @GetMapping("/tickets/{ticket_id}/qr")
    public ApiResponse<TicketCredentialResponse> getTicketQr(
            @PathVariable("ticket_id") String ticketId,
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.ok(service.getTicketQr(requireCustomerToken(customerToken, authorization), ticketId));
    }

    @PostMapping("/consents")
    public ApiResponse<CustomerConsentRecord> recordConsent(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CustomerConsentRequest request
    ) {
        return ApiResponse.ok(service.recordConsent(
                requireCustomerToken(customerToken, authorization),
                firstNonBlank(idempotencyKey, request == null ? null : request.idempotencyKey()),
                request
        ));
    }

    @GetMapping("/points")
    public ApiResponse<CustomerPointsResponse> getPoints(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.ok(service.getPoints(requireCustomerToken(customerToken, authorization)));
    }

    @GetMapping("/points/ledger")
    public ApiResponse<PageResponse<CustomerPointsLedgerRecord>> listPointsLedger(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.ok(service.listPointsLedger(requireCustomerToken(customerToken, authorization)));
    }

    @PostMapping("/check-ins")
    public ApiResponse<CustomerCheckInRecord> checkIn(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody(required = false) CustomerCheckInRequest request
    ) {
        return ApiResponse.ok(service.checkIn(requireCustomerToken(customerToken, authorization), request));
    }

    @GetMapping("/check-ins")
    public ApiResponse<PageResponse<CustomerCheckInRecord>> listCheckIns(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.ok(service.listCheckIns(requireCustomerToken(customerToken, authorization)));
    }

    @GetMapping("/points/redemption-items")
    public ApiResponse<PageResponse<CustomerRedemptionCatalogItemRecord>> listRedemptionItems(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.ok(service.listRedemptionItems(requireCustomerToken(customerToken, authorization)));
    }

    @PostMapping("/points/redemptions")
    public ApiResponse<CustomerPointsRedemptionRecord> redeemPoints(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CustomerPointsRedemptionRequest request
    ) {
        String key = firstNonBlank(idempotencyKey, request == null ? null : request.idempotencyKey());
        requireIdempotencyKey(key);
        return ApiResponse.ok(service.redeemPoints(
                requireCustomerToken(customerToken, authorization),
                key,
                request
        ));
    }

    @GetMapping("/points/redemptions")
    public ApiResponse<PageResponse<CustomerPointsRedemptionRecord>> listRedemptions(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.ok(service.listRedemptions(requireCustomerToken(customerToken, authorization)));
    }

    @GetMapping("/bar-reservation-options")
    public ApiResponse<PageResponse<CustomerBarReservationOptionResponse>> listBarReservationOptions(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.ok(service.listBarReservationOptions(requireCustomerToken(customerToken, authorization)));
    }

    @PostMapping("/bar-reservations")
    public ApiResponse<BarReservationRecord> createBarReservation(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CustomerBarReservationRequest request
    ) {
        String key = firstNonBlank(idempotencyKey, request == null ? null : request.idempotencyKey());
        requireIdempotencyKey(key);
        return ApiResponse.ok(service.createBarReservation(
                requireCustomerToken(customerToken, authorization),
                key,
                request
        ));
    }

    @PostMapping("/bar-invitations/confirm")
    public ApiResponse<BarReservationInvitationMemberRecord> confirmBarInvitation(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CustomerBarInvitationConfirmRequest request
    ) {
        String key = firstNonBlank(idempotencyKey, request == null ? null : request.idempotencyKey());
        requireIdempotencyKey(key);
        return ApiResponse.ok(service.confirmBarInvitation(
                requireCustomerToken(customerToken, authorization),
                key,
                request
        ));
    }

    @GetMapping("/bar-invitations/{reservation_id}")
    public ApiResponse<BarReservationInvitationRecord> getMyBarInvitation(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable("reservation_id") String reservationId
    ) {
        return ApiResponse.ok(service.getMyBarInvitation(
                requireCustomerToken(customerToken, authorization),
                reservationId
        ));
    }

    private String requireCustomerToken(String customerToken, String authorization) {
        String resolved = resolveCustomerToken(customerToken, authorization);
        if (resolved == null) {
            throw ApiException.unauthorized("CUSTOMER_UNAUTHENTICATED", "customer token is required");
        }
        return resolved;
    }

    private String resolveCustomerToken(String customerToken, String authorization) {
        String headerToken = trimToNull(customerToken);
        if (headerToken != null) {
            return headerToken;
        }
        String bearer = trimToNull(authorization);
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return trimToNull(bearer.substring("Bearer ".length()));
        }
        return null;
    }

    private String firstNonBlank(String first, String second) {
        String firstTrimmed = trimToNull(first);
        return firstTrimmed == null ? second : firstTrimmed;
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
