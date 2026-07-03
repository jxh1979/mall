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

    @GetMapping("/catalog")
    public ApiResponse<PageResponse<ServiceCatalogItemRecord>> listCatalog() {
        return ApiResponse.ok(service.listCatalog());
    }

    @GetMapping("/catalog/all")
    public ApiResponse<PageResponse<ServiceCatalogItemRecord>> listCatalogAll() {
        return ApiResponse.ok(service.listCatalogAll());
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
            @RequestBody @jakarta.validation.Valid com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createOrder(request));
    }

    @GetMapping("/orders/assigned-to-me")
    public ApiResponse<PageResponse<ServiceOrderRecord>> listAssignedToMe() {
        return ApiResponse.ok(service.listAssignedToMe());
    }

    @PostMapping("/orders/{id}/mark-paid")
    public ApiResponse<ServiceOrderRecord> markPaid(@PathVariable String id,
            @RequestBody @jakarta.validation.Valid com.complex.platform.serviceorder.dto.ServiceOrderMarkPaidRequest request) {
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
            @RequestBody @jakarta.validation.Valid com.complex.platform.serviceorder.dto.ServiceOrderActionRequest request) {
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
    public ApiResponse<Map<String, String>> orderPaymentCode(@PathVariable String id) {
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
