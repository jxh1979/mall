package com.complex.platform.customer;

// 文件版本：V0.07
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import static com.complex.platform.common.Strings.trimToNull;

@RestController
@RequestMapping("/api/v1/customer")
public class CustomerController {

    private final CustomerService service;
    // ROOT-03-R3 条款正文；WebMvcTest 切片无此 bean 时为 null（只影响 /terms）。
    private final CustomerTerms terms;
    // 生产环境强制会话 cookie Secure：即便 request.isSecure() 因反代配置误判为 http，
    // prod 也钉死 secure=true（dev/local 保持依赖 isSecure，便于本地 http 调试）。
    private final boolean forceSecureCookie;

    public CustomerController(CustomerService service, org.springframework.core.env.Environment environment,
            org.springframework.beans.factory.ObjectProvider<CustomerTerms> terms) {
        this.service = service;
        this.terms = terms.getIfAvailable();
        this.forceSecureCookie = environment.acceptsProfiles(org.springframework.core.env.Profiles.of("prod"));
    }

    @PostMapping("/auth/phone-login")
    public ApiResponse<CustomerLoginResponse> phoneLogin(
            @RequestBody @jakarta.validation.Valid CustomerPhoneLoginRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest,
            jakarta.servlet.http.HttpServletResponse httpResponse) {
        // ROOT-03-R2：登录端取设备凭证闸派生的本请求终端（客人只认 app、miniapp，由服务层判定）。
        CustomerLoginResponse login = service.phoneLogin(request, com.complex.platform.gate.TerminalIdentityHolder.trustedTerminalType(null));
        // 3.3 免登录：token 同时种 httpOnly+SameSite=Strict cookie（web 端刷新免登录，脚本不可读防 XSS 窃取）。
        // body 仍返回 token 供头 token 调用方（小程序/App/Android）使用，两轨并行。
        long maxAge = java.time.Duration.between(java.time.Instant.now(), login.expiresAt()).getSeconds();
        httpResponse.addHeader("Set-Cookie",
                sessionCookie(login.customerToken(), Math.max(0, maxAge), forceSecureCookie || httpRequest.isSecure()));
        return ApiResponse.ok(login);
    }

    /**
     * ROOT-03-R3 会员注册：公开端点（尚无会话）；条件全部满足即建有效会员并返回会话凭证，任一条件不满足整笔不落库。
     * 验证码与登录同一场景（customer.login），先用 POST /auth/sms-codes 取码。
     */
    @PostMapping("/auth/register")
    public ApiResponse<CustomerLoginResponse> register(
            @RequestBody @jakarta.validation.Valid com.complex.platform.customer.dto.CustomerRegisterRequest request) {
        return ApiResponse.ok(service.register(request, com.complex.platform.gate.TerminalIdentityHolder.trustedTerminalType(null)));
    }

    /** ROOT-03-R3 当前会员条款与隐私条款（匿名可读）：注册前展示，注册时回传两份版本号。 */
    @GetMapping("/terms")
    public ApiResponse<CustomerTerms.Current> terms() {
        return ApiResponse.ok(terms.current());
    }

    /** ROOT-05-R3 登录取码：公开端点，请求体只有 phone；短信服务未配置时 409，不假成功。 */
    @PostMapping("/auth/sms-codes")
    public ApiResponse<java.util.Map<String, Object>> requestLoginSmsCode(@RequestBody java.util.Map<String, String> request) {
        return ApiResponse.ok(service.requestLoginCode(request.get("phone")));
    }

    /** 3.3 免登录：登出——撤销服务端会话（AUD-030）并清除会话 cookie；端点幂等（无 token 也返回成功）。 */
    @PostMapping("/auth/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            jakarta.servlet.http.HttpServletRequest httpRequest,
            jakarta.servlet.http.HttpServletResponse httpResponse) {
        // 头 token（App/小程序）或 cookie（web，经 CustomerSessionCookieFilter 桥接为 X-Customer-Token）都撤销服务端会话，
        // 令泄漏/复制的 token 在登出后立即失效，而非仅靠客户端丢弃本地凭证。
        service.logout(resolveCustomerToken(customerToken, authorization));
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

    /**
     * 客人自助注销本人账号（V194）：凭本人 token；注销后动作/登录被拒，个人信息删除（ROOT-03-R21）。
     * TK-21-R3：请求体 {waive_balance}——true 放弃余额径行注销；false/缺省 需退本金的生成待退事项。
     * ROOT-03-R21：{abandon_saved_bottles}——名下有存酒须为 true（确认放弃，转为所在酒吧自有库存资产），否则 409 CUSTOMER_SAVED_BOTTLES_HELD。
     */
    @PostMapping("/deregister")
    public ApiResponse<CustomerRecord> deregister(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody(required = false) com.complex.platform.customer.dto.CustomerDeregisterRequest request) {
        return ApiResponse.ok(service.deregisterAccount(requireCustomerToken(customerToken, authorization),
                request != null && request.waive(), request != null && request.abandonBottles()));
    }

    /** TK-21-R3 / ROOT-03-R21（BSM 第 863 行「发起注销时系统须提醒其名下尚有存酒与储值卡余额」）：注销预览——各卡本金/赠送金与名下存酒。 */
    @GetMapping("/deregister/preview")
    public ApiResponse<com.complex.platform.customer.dto.CustomerDeregisterPreviewResponse> deregisterPreview(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(service.deregisterPreview(requireCustomerToken(customerToken, authorization)));
    }

    @GetMapping("/ticket-products")
    public ApiResponse<PageResponse<CustomerTicketProductResponse>> listTicketProducts(
            @RequestParam(name = "bar_id", required = false) String barId,
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        // AUD-076 专属入口：酒吧二维码/分享链接带 bar_id 锁定该酒吧单店票；不传=公共入口全量。
        // ROOT-03-R27：带客人会话时另给「我的价格」；匿名照旧。
        return ApiResponse.ok(service.listTicketProducts(barId, resolveCustomerToken(customerToken, authorization)));
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
    public ApiResponse<PageResponse<com.complex.platform.customer.dto.CustomerTicketOrderResponse>> listOrders(
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

    /** FS-016（BSM L867）：我的存酒——本人名下全部存酒、到期日，跨店汇总（每条带所属酒吧）；只下发客人可见字段。 */
    @GetMapping("/my-saved-bottles")
    public ApiResponse<PageResponse<com.complex.platform.customer.dto.CustomerSavedBottleResponse>> listMySavedBottles(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.ok(service.listMySavedBottles(requireCustomerToken(customerToken, authorization)));
    }

    /** FS-016（BSM L1073/L585）：官方动态取酒码，按当前持有人签发，窗口=全场统一刷新间隔。 */
    @GetMapping("/saved-bottles/{saved_bottle_id}/pickup-code")
    public ApiResponse<com.complex.platform.inventory.SavedBottlePickupCodeResponse> getSavedBottlePickupCode(
            @PathVariable("saved_bottle_id") String savedBottleId,
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.ok(service.getSavedBottlePickupCode(requireCustomerToken(customerToken, authorization), savedBottleId));
    }

    /** FS-025（ENUM L120）：客人本人转存发码（scene=renew，发到本人手机）。 */
    @PostMapping("/saved-bottles/{saved_bottle_id}/renewal-code")
    public ApiResponse<com.complex.platform.inventory.SavedBottleVerificationCodeReceipt> requestMySavedBottleRenewalCode(
            @PathVariable("saved_bottle_id") String savedBottleId,
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.ok(service.requestMySavedBottleRenewalCode(requireCustomerToken(customerToken, authorization), savedBottleId));
    }

    /** FS-025（ENUM L120「转存须客人本人短信验证码确认（与转让同款）」）：客人本人转存，验证成功即从当前到期日顺延。 */
    @PostMapping("/saved-bottles/{saved_bottle_id}/renew")
    public ApiResponse<com.complex.platform.inventory.SavedBottleRenewalRecord> renewMySavedBottle(
            @PathVariable("saved_bottle_id") String savedBottleId,
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody @jakarta.validation.Valid com.complex.platform.customer.dto.CustomerSavedBottleRenewRequest request
    ) {
        String key = firstNonBlank(idempotencyKey, request == null ? null : request.idempotencyKey());
        requireIdempotencyKey(key);
        return ApiResponse.ok(service.renewMySavedBottle(
                requireCustomerToken(customerToken, authorization),
                savedBottleId,
                new com.complex.platform.customer.dto.CustomerSavedBottleRenewRequest(key, request == null ? null : request.smsCode())));
    }

    @GetMapping("/my-tickets")
    public ApiResponse<PageResponse<com.complex.platform.ticketing.CustomerTicketResponse>> listMyTickets(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.ok(service.listMyTickets(requireCustomerToken(customerToken, authorization)));
    }

    @PostMapping("/ticket-transfers")
    public ApiResponse<com.complex.platform.ticketing.TicketTransferResponse> initiateTicketTransfer(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody @jakarta.validation.Valid com.complex.platform.customer.dto.CustomerTicketTransferRequest request
    ) {
        return ApiResponse.ok(service.initiateTicketTransfer(requireCustomerToken(customerToken, authorization), request));
    }

    @PostMapping("/ticket-transfers/{transfer_id}/claim")
    public ApiResponse<com.complex.platform.ticketing.TicketTransferResponse> claimTicketTransfer(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable("transfer_id") String transferId,
            @RequestBody(required = false) @jakarta.validation.Valid com.complex.platform.customer.dto.CustomerTicketTransferClaimRequest request
    ) {
        return ApiResponse.ok(service.claimTicketTransfer(
                requireCustomerToken(customerToken, authorization), transferId, request));
    }

    @PostMapping("/ticket-transfers/{transfer_id}/revoke")
    public ApiResponse<com.complex.platform.ticketing.TicketTransferResponse> revokeTicketTransfer(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable("transfer_id") String transferId
    ) {
        return ApiResponse.ok(service.revokeTicketTransfer(
                requireCustomerToken(customerToken, authorization), transferId));
    }

    @GetMapping("/ticket-transfers/preview")
    public ApiResponse<com.complex.platform.ticketing.TicketTransferPreviewResponse> previewTicketTransfer(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @org.springframework.web.bind.annotation.RequestParam("claim_code") String claimCode
    ) {
        return ApiResponse.ok(service.previewTicketTransfer(
                requireCustomerToken(customerToken, authorization), claimCode));
    }

    @GetMapping("/ticket-transfers/incoming")
    public ApiResponse<PageResponse<com.complex.platform.ticketing.TicketTransferResponse>> listIncomingTicketTransfers(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.ok(service.listIncomingTicketTransfers(requireCustomerToken(customerToken, authorization)));
    }

    @GetMapping("/ticket-transfers/outgoing")
    public ApiResponse<PageResponse<com.complex.platform.ticketing.TicketTransferResponse>> listOutgoingTicketTransfers(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.ok(service.listOutgoingTicketTransfers(requireCustomerToken(customerToken, authorization)));
    }

    private String requireCustomerToken(String customerToken, String authorization) {
        String resolved = resolveCustomerToken(customerToken, authorization);
        if (resolved == null) {
            throw ApiException.unauthorized("CUSTOMER_UNAUTHENTICATED", "缺少客人登录凭证，请重新登录");
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
}
