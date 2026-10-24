package com.complex.platform.customer;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.bar.BarCustomerInvitationService;
import com.complex.platform.bar.BarReservationInvitationMemberRecord;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.customer.dto.CustomerBarMenuItemResponse;
import com.complex.platform.customer.dto.CustomerBarScanOrderRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ROOT-03 客人端订台与扫码点单接口：我的订台、订卡邀请函预览/移出/本人动态入场码（clients-R6），固定桌台二维码扫码点单（clients-R7）。
 * 邀请函接口只许客人本人对象；扫码点单匿名可用（ENUM 第 129 行「非会员可以浏览、点单和付款」），带客人会话时记会员本人。
 */
@RestController
@RequestMapping("/api/v1/customer")
public class CustomerBarController {

    private final CustomerService customers;
    private final CustomerBarScanOrderService scanOrders;

    public CustomerBarController(CustomerService customers, CustomerBarScanOrderService scanOrders) {
        this.customers = customers;
        this.scanOrders = scanOrders;
    }

    @GetMapping("/bar-reservations")
    public ApiResponse<PageResponse<BarCustomerInvitationService.ReservationView>> listMyBarReservations(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(customers.listMyBarReservations(requireToken(customerToken, authorization)));
    }

    @GetMapping("/bar-invitations/preview")
    public ApiResponse<BarCustomerInvitationService.InvitationPreview> previewBarInvitation(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam("invitation_token") String invitationToken,
            @RequestParam("invitation_signature") String invitationSignature,
            @RequestParam("table_id") String tableId,
            @RequestParam("capacity") Integer capacity) {
        return ApiResponse.ok(customers.previewBarInvitation(requireToken(customerToken, authorization),
                invitationToken, invitationSignature, tableId, capacity));
    }

    @PostMapping("/bar-invitations/{reservation_id}/members/{member_id}/remove")
    public ApiResponse<BarReservationInvitationMemberRecord> removeBarInvitationMember(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable("reservation_id") String reservationId,
            @PathVariable("member_id") String memberId,
            @RequestBody(required = false) Map<String, Object> request) {
        return ApiResponse.ok(customers.removeBarInvitationMember(requireToken(customerToken, authorization),
                requireKey(idempotencyKey, request), reservationId, memberId));
    }

    @GetMapping("/bar-invitations/{reservation_id}/entry-code")
    public ApiResponse<BarCustomerInvitationService.EntryCode> getBarInvitationEntryCode(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable("reservation_id") String reservationId) {
        return ApiResponse.ok(customers.getBarInvitationEntryCode(requireToken(customerToken, authorization), reservationId));
    }

    @GetMapping("/bar-tables/{table_id}/menu")
    public ApiResponse<PageResponse<CustomerBarMenuItemResponse>> barScanMenu(@PathVariable("table_id") String tableId) {
        return ApiResponse.ok(scanOrders.menu(tableId));
    }

    @PostMapping("/bar-tables/{table_id}/scan-orders")
    public ApiResponse<Map<String, Object>> createBarScanOrder(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable("table_id") String tableId,
            @RequestBody CustomerBarScanOrderRequest request) {
        String key = trimToNull(idempotencyKey) != null ? idempotencyKey.trim() : request == null ? null : request.idempotencyKey();
        com.complex.platform.common.api.ApiRequests.requireIdempotencyKey(key);
        return ApiResponse.ok(scanOrders.create(resolveToken(customerToken, authorization), tableId, key, request));
    }

    @PostMapping("/bar-tables/{table_id}/scan-orders/{scan_order_id}/confirm")
    public ApiResponse<Map<String, Object>> confirmBarScanOrder(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable("table_id") String tableId,
            @PathVariable("scan_order_id") String scanOrderId,
            @RequestBody(required = false) Map<String, Object> request) {
        return ApiResponse.ok(scanOrders.confirm(tableId, scanOrderId, requireKey(idempotencyKey, request)));
    }

    @GetMapping("/bar-tables/{table_id}/current-bill")
    public ApiResponse<CustomerBarScanOrderService.CurrentBill> barTableCurrentBill(@PathVariable("table_id") String tableId) {
        return ApiResponse.ok(scanOrders.currentBill(tableId));
    }

    private static String requireKey(String header, Map<String, Object> body) {
        String key = trimToNull(header);
        if (key == null && body != null && body.get("idempotency_key") != null) {
            key = trimToNull(String.valueOf(body.get("idempotency_key")));
        }
        com.complex.platform.common.api.ApiRequests.requireIdempotencyKey(key);
        return key;
    }

    private static String requireToken(String customerToken, String authorization) {
        String resolved = resolveToken(customerToken, authorization);
        if (resolved == null) {
            throw ApiException.unauthorized("CUSTOMER_UNAUTHENTICATED", "缺少客人登录凭证，请重新登录");
        }
        return resolved;
    }

    private static String resolveToken(String customerToken, String authorization) {
        String header = trimToNull(customerToken);
        if (header != null) {
            return header;
        }
        String bearer = trimToNull(authorization);
        return bearer != null && bearer.startsWith("Bearer ") ? trimToNull(bearer.substring("Bearer ".length())) : null;
    }
}
