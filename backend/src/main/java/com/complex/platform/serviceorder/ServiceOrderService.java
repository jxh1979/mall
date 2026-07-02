package com.complex.platform.serviceorder;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * G批(2026-07-02 六口径拍板)服务点单:
 * ① 后台目录固定参考价;② 服务员代客下单(客人App自点接口预留);③ 下单即指定履约人;
 * ④ 直付口径(税务)——服务费客人现场扫履约人收款码直付,酒吧不经手不抽成,
 *   不进酒吧账单/平台净收入/薪资提成,参考价仅作台账;⑤ 履约前可取消,完成后不可退;
 * ⑥ 不进酒吧桌台账单,仅关联桌台记录来源。
 */
@Service
public class ServiceOrderService {

    private final ServiceOrderRepository repository;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;

    public ServiceOrderService(ServiceOrderRepository repository, AuditEventService auditEventService,
            AuthorizationService authorizationService) {
        this.repository = repository;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
    }

    public ServiceCatalogItemRecord upsertCatalogItem(com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest request) {
        authorizationService.requirePermission("service.catalog.manage");
        if (request == null || request.name() == null || request.name().isBlank()
                || request.priceCent() == null || request.priceCent() < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "name and non-negative price_cent required");
        }
        var actor = authorizationService.currentActor();
        String id = request.id() == null || request.id().isBlank() ? "svc_" + UUID.randomUUID() : request.id().trim();
        ServiceCatalogItemRecord saved = repository.upsertCatalogItem(new ServiceCatalogItemRecord(
                id, request.name().trim(), request.priceCent(), request.durationMinutes(),
                request.active() == null || request.active(), actor == null ? null : actor.id(), Instant.now()));
        auditEventService.recordSuccess("service.catalog.saved", "service.catalog.manage",
                "service_catalog_item", saved.id(), null, request.idempotencyKey(),
                Map.of("name", saved.name(), "price_cent", saved.priceCent()));
        return saved;
    }

    public PageResponse<ServiceCatalogItemRecord> listCatalog() {
        authorizationService.requirePermission("service.order.create");
        return page(repository.listCatalog(true));
    }

    public PageResponse<ServiceCatalogItemRecord> listCatalogAll() {
        authorizationService.requirePermission("service.catalog.manage");
        return page(repository.listCatalog(false));
    }

    /** 口径④:履约人录入自己的固定收款码(存二维码内容链接,App 端重绘 QR)。 */
    public void setMyPaymentCode(com.complex.platform.serviceorder.dto.StaffPaymentCodeRequest request) {
        authorizationService.requirePermission("service.order.perform");
        if (request == null || request.qrContent() == null || request.qrContent().isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "qr_content is required");
        }
        var actor = authorizationService.currentActor();
        repository.upsertPaymentCode(actor.id(), request.qrContent().trim(), Instant.now());
        auditEventService.recordSuccess("service.payment_code.saved", "service.order.perform",
                "staff_payment_code", actor.id(), null, request.idempotencyKey(), Map.of());
    }

    public Map<String, String> myPaymentCode() {
        authorizationService.requirePermission("service.order.perform");
        var actor = authorizationService.currentActor();
        return Map.of("qr_content", repository.findPaymentCode(actor.id()).orElse(""));
    }

    /** 口径②③:代客下单+指定履约人;价格与名称取目录快照;履约人须已录收款码(否则客人没法付)。 */
    public ServiceOrderRecord createOrder(com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest request) {
        authorizationService.requirePermission("service.order.create");
        if (request == null || request.catalogItemId() == null || request.catalogItemId().isBlank()
                || request.performerUserId() == null || request.performerUserId().isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "catalog_item_id and performer_user_id required");
        }
        ServiceCatalogItemRecord item = repository.findCatalogItem(request.catalogItemId().trim())
                .filter(ServiceCatalogItemRecord::active)
                .orElseThrow(() -> ApiException.notFound("SERVICE_ITEM_NOT_FOUND", "service item not found or inactive"));
        if (repository.findPaymentCode(request.performerUserId().trim()).isEmpty()) {
            throw ApiException.badRequest("PERFORMER_NO_PAYMENT_CODE",
                    "performer has no payment code yet (direct-pay requires it)");
        }
        var actor = authorizationService.currentActor();
        Instant now = Instant.now();
        ServiceOrderRecord saved = repository.saveOrder(new ServiceOrderRecord(
                "svo_" + UUID.randomUUID(), item.id(), item.name(), item.priceCent(),
                trimToNull(request.barId()), trimToNull(request.tableId()), request.performerUserId().trim(),
                actor == null ? null : actor.id(), trimToNull(request.customerNote()),
                "pending_accept", null, now, now, null));
        auditEventService.recordSuccess("service.order.created", "service.order.create",
                "service_order", saved.id(), null, request.idempotencyKey(),
                Map.of("item", saved.itemName(), "performer", saved.performerUserId(),
                        "price_cent_reference_direct_pay", saved.priceCent()));
        return saved;
    }

    public PageResponse<ServiceOrderRecord> listAssignedToMe() {
        authorizationService.requirePermission("service.order.perform");
        var actor = authorizationService.currentActor();
        return page(repository.listOrdersForPerformer(actor.id()));
    }

    public ServiceOrderRecord accept(String id, com.complex.platform.serviceorder.dto.ServiceOrderActionRequest request) {
        return transition(id, request, "pending_accept", "accepted", "service.order.accepted", null);
    }

    /** 完成履约(口径④:完成即结束——直付已在现场发生,系统不经手资金)。 */
    public ServiceOrderRecord complete(String id, com.complex.platform.serviceorder.dto.ServiceOrderActionRequest request) {
        return transition(id, request, "accepted", "completed", "service.order.completed", Instant.now());
    }

    private ServiceOrderRecord transition(String id, com.complex.platform.serviceorder.dto.ServiceOrderActionRequest request,
            String fromStatus, String toStatus, String event, Instant completedAt) {
        authorizationService.requirePermission("service.order.perform");
        ServiceOrderRecord current = repository.findOrder(id)
                .orElseThrow(() -> ApiException.notFound("SERVICE_ORDER_NOT_FOUND", "service order not found"));
        var actor = authorizationService.currentActor();
        if (actor == null || !actor.id().equals(current.performerUserId())) {
            throw ApiException.forbidden("NOT_PERFORMER", "only assigned performer can act on this order");
        }
        if (!current.status().equals(fromStatus)) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "order status is " + current.status() + ", expecting " + fromStatus);
        }
        Instant now = Instant.now();
        ServiceOrderRecord saved = repository.saveOrder(new ServiceOrderRecord(
                current.id(), current.catalogItemId(), current.itemName(), current.priceCent(),
                current.barId(), current.tableId(), current.performerUserId(), current.orderedBy(),
                current.customerNote(), toStatus, current.cancelReason(), current.createdAt(), now,
                completedAt == null ? current.completedAt() : completedAt));
        auditEventService.recordSuccess(event, "service.order.perform", "service_order", saved.id(), null,
                request == null ? null : request.idempotencyKey(), Map.of("status", toStatus));
        return saved;
    }

    /** 口径⑤:履约前可取消(下单者或目录管理岗),留原因;完成后不可退。 */
    public ServiceOrderRecord cancel(String id, com.complex.platform.serviceorder.dto.ServiceOrderCancelRequest request) {
        authorizationService.requirePermission("service.order.create");
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "cancel reason is required");
        }
        ServiceOrderRecord current = repository.findOrder(id)
                .orElseThrow(() -> ApiException.notFound("SERVICE_ORDER_NOT_FOUND", "service order not found"));
        if (current.status().equals("completed")) {
            throw ApiException.badRequest("ALREADY_COMPLETED",
                    "completed service cannot be cancelled (direct pay already happened; disputes handled offline)");
        }
        if (current.status().equals("cancelled")) {
            return current;
        }
        Instant now = Instant.now();
        ServiceOrderRecord saved = repository.saveOrder(new ServiceOrderRecord(
                current.id(), current.catalogItemId(), current.itemName(), current.priceCent(),
                current.barId(), current.tableId(), current.performerUserId(), current.orderedBy(),
                current.customerNote(), "cancelled", request.reason().trim(), current.createdAt(), now,
                current.completedAt()));
        auditEventService.recordSuccess("service.order.cancelled", "service.order.create",
                "service_order", saved.id(), null, request.idempotencyKey(),
                Map.of("reason", request.reason().trim()));
        return saved;
    }

    /** 口径④:向客人出示履约人收款码(下单者/履约者可取)。 */
    public Map<String, String> orderPaymentCode(String id) {
        authorizationService.requirePermission("service.order.create");
        ServiceOrderRecord order = repository.findOrder(id)
                .orElseThrow(() -> ApiException.notFound("SERVICE_ORDER_NOT_FOUND", "service order not found"));
        String qr = repository.findPaymentCode(order.performerUserId())
                .orElseThrow(() -> ApiException.notFound("PERFORMER_NO_PAYMENT_CODE", "performer has no payment code"));
        return Map.of("performer_user_id", order.performerUserId(), "qr_content", qr,
                "price_cent", String.valueOf(order.priceCent()));
    }

    public PageResponse<ServiceOrderRecord> listOrders(java.time.LocalDate date, String status) {
        authorizationService.requirePermission("service.catalog.manage");
        return page(repository.listOrders(date, status));
    }

    /** 口径④:履约人当月完成次数+参考收入(直付台账,非营收、非薪资提成)。 */
    public Map<String, Object> myStats(String month) {
        authorizationService.requirePermission("service.order.perform");
        if (month == null || !month.matches("\\d{4}-\\d{2}")) {
            throw ApiException.badRequest("VALIDATION_FAILED", "month(YYYY-MM) required");
        }
        var actor = authorizationService.currentActor();
        long[] stats = repository.performerMonthlyStats(actor.id(), month);
        return Map.of("month", month, "completed_count", stats[0], "reference_income_cent", stats[1]);
    }

    private <T> PageResponse<T> page(List<T> items) {
        return new PageResponse<>(items, 1, 500, items.size());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
