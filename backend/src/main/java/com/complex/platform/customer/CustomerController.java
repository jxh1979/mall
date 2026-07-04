package com.complex.platform.customer;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.bar.BarOrderRecord;
import com.complex.platform.bar.BarReservationInvitationMemberRecord;
import com.complex.platform.bar.BarReservationInvitationRecord;
import com.complex.platform.bar.BarReservationRecord;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.customer.dto.CustomerBarInvitationConfirmRequest;
import com.complex.platform.customer.dto.CustomerBarMenuItemResponse;
import com.complex.platform.customer.dto.CustomerBarReservationRequest;
import com.complex.platform.customer.dto.CustomerBarScanOrderRequest;
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
    // 生产环境强制会话 cookie Secure：即便 request.isSecure() 因反代配置误判为 http，
    // prod 也钉死 secure=true（dev/local 保持依赖 isSecure，便于本地 http 调试）。
    private final boolean forceSecureCookie;

    public CustomerController(CustomerService service, org.springframework.core.env.Environment environment) {
        this.service = service;
        this.forceSecureCookie = environment.acceptsProfiles(org.springframework.core.env.Profiles.of("prod"));
    }

    @PostMapping("/auth/phone-login")
    public ApiResponse<CustomerLoginResponse> phoneLogin(
            @RequestBody @jakarta.validation.Valid CustomerPhoneLoginRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest,
            jakarta.servlet.http.HttpServletResponse httpResponse) {
        CustomerLoginResponse login = service.phoneLogin(request);
        // 3.3 免登录：token 同时种 httpOnly+SameSite=Strict cookie（web 端刷新免登录，脚本不可读防 XSS 窃取）。
        // body 仍返回 token 供头 token 调用方（小程序/App/Android）使用，两轨并行。
        long maxAge = java.time.Duration.between(java.time.Instant.now(), login.expiresAt()).getSeconds();
        httpResponse.addHeader("Set-Cookie",
                sessionCookie(login.customerToken(), Math.max(0, maxAge), forceSecureCookie || httpRequest.isSecure()));
        return ApiResponse.ok(login);
    }

    /** 3.3 免登录：登出——清除会话 cookie（头 token 调用方丢弃本地 token 即可，端点幂等）。 */
    @PostMapping("/auth/logout")
    public ApiResponse<Void> logout(jakarta.servlet.http.HttpServletRequest httpRequest,
            jakarta.servlet.http.HttpServletResponse httpResponse) {
        httpResponse.addHeader("Set-Cookie", sessionCookie("", 0, forceSecureCookie || httpRequest.isSecure()));
        return ApiResponse.ok(null);
    }

    /**
     * 3.3 免登录：会话恢复——凭 cookie/头 token 返回当前客人身份（不回传凭证本身）。
     * #7：这是「是否已登录」的只读探测。匿名（无 cookie 无 token）返回 204 空会话，
     * 而非 401，避免未登录刷新每次污染控制台。真正需鉴权的写/读接口仍走 requireCustomerToken 抛 401。
     */
    @GetMapping("/session")
    public org.springframework.http.ResponseEntity<ApiResponse<CustomerSessionInfoResponse>> session(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        String resolved = resolveCustomerToken(customerToken, authorization);
        if (resolved == null) {
            // 匿名探测：空会话正常态，返回 204 No Content（无 body）。
            return org.springframework.http.ResponseEntity.noContent().build();
        }
        // 有凭证但失效/过期时 sessionInfo 抛 401（真·失效信号，非每次刷新噪声）。
        return org.springframework.http.ResponseEntity.ok(ApiResponse.ok(service.sessionInfo(resolved)));
    }

    private String sessionCookie(String value, long maxAgeSeconds, boolean secure) {
        return org.springframework.http.ResponseCookie
                .from(com.complex.platform.auth.CustomerSessionCookieFilter.COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api/v1/customer")
                .maxAge(maxAgeSeconds)
                .build()
                .toString();
    }

    /** 客人自助注销本人账号（V194）：凭本人 token；注销后动作/登录被拒、历史数据物理保留但客人不可见。 */
    @PostMapping("/deregister")
    public ApiResponse<CustomerRecord> deregister(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(service.deregisterAccount(requireCustomerToken(customerToken, authorization)));
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
            @RequestBody @jakarta.validation.Valid CustomerTicketOrderRequest request
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

    /** 1.8 我的票：订单展开票实例，点票直接出码（无需手输票 id）。 */
    @GetMapping("/orders/{order_id}/tickets")
    public ApiResponse<PageResponse<com.complex.platform.ticketing.CustomerTicketResponse>> listOrderTickets(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable("order_id") String orderId
    ) {
        return ApiResponse.ok(service.listOrderTickets(requireCustomerToken(customerToken, authorization), orderId));
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
            @RequestBody @jakarta.validation.Valid CustomerConsentRequest request
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
            @RequestBody(required = false) @jakarta.validation.Valid CustomerCheckInRequest request
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
            @RequestBody @jakarta.validation.Valid CustomerPointsRedemptionRequest request
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
            @RequestBody @jakarta.validation.Valid CustomerBarReservationRequest request
    ) {
        String key = firstNonBlank(idempotencyKey, request == null ? null : request.idempotencyKey());
        requireIdempotencyKey(key);
        return ApiResponse.ok(service.createBarReservation(
                requireCustomerToken(customerToken, authorization),
                key,
                request
        ));
    }

    @GetMapping("/bar-sessions/{session_id}/menu")
    public ApiResponse<PageResponse<CustomerBarMenuItemResponse>> listBarScanMenu(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable("session_id") String sessionId
    ) {
        return ApiResponse.ok(service.listBarScanMenu(
                requireCustomerToken(customerToken, authorization), sessionId));
    }

    @PostMapping("/bar-sessions/{session_id}/scan-orders")
    public ApiResponse<BarOrderRecord> placeBarScanOrder(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable("session_id") String sessionId,
            @RequestBody @jakarta.validation.Valid CustomerBarScanOrderRequest request
    ) {
        String key = firstNonBlank(idempotencyKey, request == null ? null : request.idempotencyKey());
        requireIdempotencyKey(key);
        return ApiResponse.ok(service.placeBarScanOrder(
                requireCustomerToken(customerToken, authorization), key, sessionId, request));
    }

    @PostMapping("/bar-invitations/confirm")
    public ApiResponse<BarReservationInvitationMemberRecord> confirmBarInvitation(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody @jakarta.validation.Valid CustomerBarInvitationConfirmRequest request
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
