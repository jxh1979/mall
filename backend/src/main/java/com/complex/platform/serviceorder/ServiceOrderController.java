package com.complex.platform.serviceorder;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/services")
public class ServiceOrderController {

    private final ServiceOrderService service;

    public ServiceOrderController(ServiceOrderService service) {
        this.service = service;
    }

    private void requireIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "idempotency_key is required");
        }
    }

    /** 2026-07-03 用户裁决 + 审计 P1 收紧:私人手机不允许带入工作现场——确认收款(mark-paid)/完成履约(complete)/
     * 下单/出示公司收款码为必须现场完成的资金动作,仅允许公司设备(工作手机 staff_work_phone / 后台 web / 平板)。
     * fail-closed:缺省/空白 X-Terminal-Type 一律拒绝(正规客户端 web/staff_work_phone/tablet 均发标识,
     * 只挡无标识绕过);允许清单与 openapi-p0 两 operation 的 x-terminal-allowed 一致。 */
    static void requireCompanyTerminal(String terminalType, java.util.Set<String> allowed) {
        if (terminalType == null || terminalType.isBlank()) {
            throw ApiException.forbidden("TERMINAL_NOT_ALLOWED",
                    "现场收银操作必须使用公司终端（缺少 X-Terminal-Type 标识）");
        }
        if (!allowed.contains(terminalType)) {
            throw ApiException.forbidden("TERMINAL_NOT_ALLOWED",
                    "终端「" + terminalType + "」不允许进行现场收银操作");
        }
    }

    private static final java.util.Set<String> MARK_PAID_TERMINALS =
            java.util.Set.of("staff_work_phone", "web", "tablet");
    private static final java.util.Set<String> COMPLETE_TERMINALS =
            java.util.Set.of("staff_work_phone", "web");
    /** 2026-07-03 补齐:代客下单(create)/出示公司收款码(payment-code)同为卡座现场动作,禁私人手机;
     * app=客人App自点预留,保留。与 openapi-p0 两 operation 的 x-terminal-allowed 一致。 */
    static final java.util.Set<String> CREATE_AND_CODE_TERMINALS =
            java.util.Set.of("staff_work_phone", "web", "tablet", "app");

    @GetMapping("/catalog")
    public ApiResponse<PageResponse<ServiceCatalogItemRecord>> listCatalog() {
        return ApiResponse.ok(service.listCatalog());
    }

    @GetMapping("/catalog/all")
    public ApiResponse<PageResponse<ServiceCatalogItemRecord>> listCatalogAll() {
        return ApiResponse.ok(service.listCatalogAll());
    }

    @PostMapping("/catalog/{id}/archive")
    public ApiResponse<ServiceCatalogItemRecord> archiveCatalog(@org.springframework.web.bind.annotation.PathVariable("id") String id,
            @RequestBody(required = false) com.complex.platform.serviceorder.dto.ServiceCatalogMutationRequest request) {
        return ApiResponse.ok(service.archiveCatalogItem(id, true, request == null ? null : request.reason()));
    }

    @PostMapping("/catalog/{id}/unarchive")
    public ApiResponse<ServiceCatalogItemRecord> unarchiveCatalog(@org.springframework.web.bind.annotation.PathVariable("id") String id) {
        return ApiResponse.ok(service.archiveCatalogItem(id, false, null));
    }

    @PostMapping("/catalog/{id}/delete-request")
    public ApiResponse<ServiceCatalogItemRecord> deleteRequestCatalog(@org.springframework.web.bind.annotation.PathVariable("id") String id,
            @RequestBody(required = false) com.complex.platform.serviceorder.dto.ServiceCatalogMutationRequest request) {
        return ApiResponse.ok(service.requestDeleteCatalogItem(id, request == null ? null : request.reason()));
    }

    @PostMapping("/catalog/{id}/delete-approve")
    public ApiResponse<ServiceCatalogItemRecord> deleteApproveCatalog(@org.springframework.web.bind.annotation.PathVariable("id") String id,
            @RequestBody(required = false) com.complex.platform.serviceorder.dto.ServiceCatalogMutationRequest request) {
        return ApiResponse.ok(service.approveDeleteCatalogItem(id, request == null ? null : request.decision()));
    }

    @PostMapping("/catalog")
    public ApiResponse<ServiceCatalogItemRecord> upsertCatalogItem(
            @RequestBody @jakarta.validation.Valid com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.upsertCatalogItem(request));
    }

    @PostMapping("/payment-code")
    public ApiResponse<Map<String, String>> setMyPaymentCode(
            @RequestBody @jakarta.validation.Valid com.complex.platform.serviceorder.dto.StaffPaymentCodeRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        service.setMyPaymentCode(request);
        return ApiResponse.ok(Map.of("status", "saved"));
    }

    @GetMapping("/payment-code/mine")
    public ApiResponse<Map<String, String>> myPaymentCode() {
        return ApiResponse.ok(service.myPaymentCode());
    }

    @PostMapping("/orders")
    public ApiResponse<ServiceOrderRecord> createOrder(
            @org.springframework.web.bind.annotation.RequestHeader(name = "X-Terminal-Type", required = false) String terminalType,
            @RequestBody @jakarta.validation.Valid com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest request) {
        // AUD-010:用设备凭证验签派生的服务端可信终端类型(拦截器已强制),再过公司设备白名单。
        requireCompanyTerminal(com.complex.platform.gate.TerminalIdentityHolder.trustedTerminalType(terminalType), CREATE_AND_CODE_TERMINALS);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createOrder(request));
    }

    @GetMapping("/orders/assigned-to-me")
    public ApiResponse<PageResponse<ServiceOrderRecord>> listAssignedToMe() {
        return ApiResponse.ok(service.listAssignedToMe());
    }

    @PostMapping("/orders/{id}/mark-paid")
    public ApiResponse<ServiceOrderRecord> markPaid(@PathVariable String id,
            @org.springframework.web.bind.annotation.RequestHeader(name = "X-Terminal-Type", required = false) String terminalType,
            @RequestBody @jakarta.validation.Valid com.complex.platform.serviceorder.dto.ServiceOrderMarkPaidRequest request) {
        requireCompanyTerminal(com.complex.platform.gate.TerminalIdentityHolder.trustedTerminalType(terminalType), MARK_PAID_TERMINALS);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.markPaid(id, request));
    }

    @PostMapping("/pay-config")
    public ApiResponse<Map<String, String>> setPayConfig(
            @RequestBody @jakarta.validation.Valid com.complex.platform.serviceorder.dto.ServicePayConfigRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        service.setPayConfig(request);
        return ApiResponse.ok(Map.of("status", "saved"));
    }

    @GetMapping("/pay-config")
    public ApiResponse<Map<String, String>> payConfig() {
        return ApiResponse.ok(service.payConfig());
    }

    @PostMapping("/orders/{id}/accept")
    public ApiResponse<ServiceOrderRecord> accept(@PathVariable String id,
            @RequestBody @jakarta.validation.Valid com.complex.platform.serviceorder.dto.ServiceOrderActionRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.accept(id, request));
    }

    @PostMapping("/orders/{id}/complete")
    public ApiResponse<ServiceOrderRecord> complete(@PathVariable String id,
            @org.springframework.web.bind.annotation.RequestHeader(name = "X-Terminal-Type", required = false) String terminalType,
            @RequestBody @jakarta.validation.Valid com.complex.platform.serviceorder.dto.ServiceOrderActionRequest request) {
        requireCompanyTerminal(com.complex.platform.gate.TerminalIdentityHolder.trustedTerminalType(terminalType), COMPLETE_TERMINALS);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.complete(id, request));
    }

    @PostMapping("/orders/{id}/cancel")
    public ApiResponse<ServiceOrderRecord> cancel(@PathVariable String id,
            @RequestBody @jakarta.validation.Valid com.complex.platform.serviceorder.dto.ServiceOrderCancelRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.cancel(id, request));
    }

    @GetMapping("/orders/{id}/payment-code")
    public ApiResponse<Map<String, String>> orderPaymentCode(@PathVariable String id,
            @org.springframework.web.bind.annotation.RequestHeader(name = "X-Terminal-Type", required = false) String terminalType) {
        requireCompanyTerminal(com.complex.platform.gate.TerminalIdentityHolder.trustedTerminalType(terminalType), CREATE_AND_CODE_TERMINALS);
        return ApiResponse.ok(service.orderPaymentCode(id));
    }

    @GetMapping("/orders")
    public ApiResponse<PageResponse<ServiceOrderRecord>> listOrders(
            @RequestParam(value = "date", required = false) String date,
            @RequestParam(value = "status", required = false) String status) {
        return ApiResponse.ok(service.listOrders(
                date == null || date.isBlank() ? null : java.time.LocalDate.parse(date), status));
    }

    @GetMapping("/orders/my-stats")
    public ApiResponse<Map<String, Object>> myStats(@RequestParam("month") String month) {
        return ApiResponse.ok(service.myStats(month));
    }
}
