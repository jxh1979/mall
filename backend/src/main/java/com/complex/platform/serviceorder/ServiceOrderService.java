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
    private final com.complex.platform.access.staff.StaffRepository staffRepository;
    private final com.complex.platform.common.idempotency.IdempotencyService idempotencyService;

    public ServiceOrderService(ServiceOrderRepository repository, AuditEventService auditEventService,
            AuthorizationService authorizationService,
            com.complex.platform.access.staff.StaffRepository staffRepository,
            com.complex.platform.common.idempotency.IdempotencyService idempotencyService) {
        this.repository = repository;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.staffRepository = staffRepository;
        this.idempotencyService = idempotencyService;
    }

    public ServiceCatalogItemRecord upsertCatalogItem(com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest request) {
        authorizationService.requirePermission("service.catalog.manage");
        if (request == null || request.name() == null || request.name().isBlank()
                || request.priceCent() == null || request.priceCent() < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "name and non-negative price_cent required");
        }
        var actor = authorizationService.currentActor();
        String id = request.id() == null || request.id().isBlank() ? "svc_" + UUID.randomUUID() : request.id().trim();
        String feeMode = request.mgmtFeeMode() == null ? "none" : request.mgmtFeeMode();
        if (!java.util.Set.of("none", "percent", "fixed").contains(feeMode)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "mgmt_fee_mode must be none/percent/fixed");
        }
        long feeValue = request.mgmtFeeValue() == null ? 0 : request.mgmtFeeValue();
        if (feeValue < 0 || (feeMode.equals("percent") && feeValue > 10000)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "mgmt_fee_value invalid (percent uses bp <= 10000)");
        }
        ServiceCatalogItemRecord saved = repository.upsertCatalogItem(new ServiceCatalogItemRecord(
                id, request.name().trim(), request.priceCent(), request.durationMinutes(),
                request.active() == null || request.active(), feeMode, feeValue,
                actor == null ? null : actor.id(), Instant.now()));
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

    /** 口径②③:代客下单+指定履约人;价格与名称取目录快照;履约人须已录收款码(否则客人没法付)。
     * P0 真幂等(2026-07-03):幂等键唯一占位+命中重放首次结果,重复提交不再重复建单。 */
    public ServiceOrderRecord createOrder(com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest request) {
        authorizationService.requirePermission("service.order.create");
        return idempotencyService.execute("createServiceOrder",
                request == null ? null : request.idempotencyKey(), request,
                () -> doCreateOrder(request), ServiceOrderRecord.class);
    }

    private ServiceOrderRecord doCreateOrder(com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest request) {
        if (request == null || request.catalogItemId() == null || request.catalogItemId().isBlank()
                || request.performerUserId() == null || request.performerUserId().isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "catalog_item_id and performer_user_id required");
        }
        ServiceCatalogItemRecord item = repository.findCatalogItem(request.catalogItemId().trim())
                .filter(ServiceCatalogItemRecord::active)
                .orElseThrow(() -> ApiException.notFound("SERVICE_ITEM_NOT_FOUND", "service item not found or inactive"));
        var actor = authorizationService.currentActor();
        Instant now = Instant.now();
        // H批(2026-07-02 最终拍板):公司统一账户代收——下单即出示公司收款码,服务员现场确认收款。
        ServiceOrderRecord saved = repository.saveOrder(new ServiceOrderRecord(
                "svo_" + UUID.randomUUID(), item.id(), item.name(), item.priceCent(),
                trimToNull(request.barId()), trimToNull(request.tableId()), request.performerUserId().trim(),
                actor == null ? null : actor.id(), trimToNull(request.customerNote()),
                "created", null, null, null, null, null, 0, 0, now, now, null));
        auditEventService.recordSuccess("service.order.created", "service.order.create",
                "service_order", saved.id(), null, request.idempotencyKey(),
                Map.of("item", saved.itemName(), "performer", saved.performerUserId(),
                        "price_cent_company_collect", saved.priceCent()));
        return saved;
    }

    public PageResponse<ServiceOrderRecord> listAssignedToMe() {
        authorizationService.requirePermission("service.order.perform");
        var actor = authorizationService.currentActor();
        return page(repository.listOrdersForPerformer(actor.id()));
    }

    /** 兼容保留(舞者工作时无手机,新流程不经 accept——created→paid 后由服务员直接完成)。 */
    public ServiceOrderRecord accept(String id, com.complex.platform.serviceorder.dto.ServiceOrderActionRequest request) {
        authorizationService.requirePermission("service.order.perform");
        return idempotencyService.execute("acceptServiceOrder:" + id,
                request == null ? null : request.idempotencyKey(), request,
                () -> doAccept(id, request), ServiceOrderRecord.class);
    }

    private ServiceOrderRecord doAccept(String id, com.complex.platform.serviceorder.dto.ServiceOrderActionRequest request) {
        ServiceOrderRecord current = require(id);
        if (!current.status().equals("pending_accept")) {
            throw ApiException.badRequest("VALIDATION_FAILED", "accept only applies to legacy pending_accept orders");
        }
        return persistStatus(current, "accepted", null, "service.order.accepted",
                request == null ? null : request.idempotencyKey());
    }

    /** H批:服务员现场目视客人扫公司收款码支付成功 → 确认收款(钱进公司账户,财务可对账)。
     * P0 真幂等:资金确认动作,重复提交重放首次结果。 */
    public ServiceOrderRecord markPaid(String id, com.complex.platform.serviceorder.dto.ServiceOrderMarkPaidRequest request) {
        authorizationService.requirePermission("service.order.create");
        return idempotencyService.execute("markServiceOrderPaid:" + id,
                request == null ? null : request.idempotencyKey(), request,
                () -> doMarkPaid(id, request), ServiceOrderRecord.class);
    }

    private ServiceOrderRecord doMarkPaid(String id, com.complex.platform.serviceorder.dto.ServiceOrderMarkPaidRequest request) {
        ServiceOrderRecord current = require(id);
        if (!current.status().equals("created")) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "order status is " + current.status() + ", expecting created");
        }
        var actor = authorizationService.currentActor();
        Instant now = Instant.now();
        ServiceOrderRecord saved = repository.saveOrder(new ServiceOrderRecord(
                current.id(), current.catalogItemId(), current.itemName(), current.priceCent(),
                current.barId(), current.tableId(), current.performerUserId(), current.orderedBy(),
                current.customerNote(), "paid", current.cancelReason(),
                now, actor == null ? null : actor.id(),
                request == null || request.payMethod() == null ? "company_qr" : request.payMethod(),
                current.refundNote(), current.mgmtFeeCent(), current.dancerShareCent(),
                current.createdAt(), now, current.completedAt()));
        auditEventService.recordSuccess("service.order.paid", "service.order.create",
                "service_order", saved.id(), null,
                request == null ? null : request.idempotencyKey(),
                Map.of("pay_method", saved.payMethod(), "price_cent", saved.priceCent()));
        return saved;
    }

    /** H批:完成履约(服务员现场操作;舞者无手机)。前置=已收款到公司账户;
     * 完成时分账:管理费(目录比例/固定)归公司,余额写提成台账归舞者(月结进薪资并代扣个税)。
     * P0 真幂等:分账+提成入台账为资金动作,重复提交重放首次结果,提成不重复入账。 */
    public ServiceOrderRecord complete(String id, com.complex.platform.serviceorder.dto.ServiceOrderActionRequest request) {
        authorizationService.requirePermission("service.order.create");
        return idempotencyService.execute("completeServiceOrder:" + id,
                request == null ? null : request.idempotencyKey(), request,
                () -> doComplete(id, request), ServiceOrderRecord.class);
    }

    private ServiceOrderRecord doComplete(String id, com.complex.platform.serviceorder.dto.ServiceOrderActionRequest request) {
        ServiceOrderRecord current = require(id);
        if (!current.status().equals("paid") && !current.status().equals("accepted")) {
            throw ApiException.badRequest("NOT_PAID_YET",
                    "order must be paid (company account) before completion; status=" + current.status());
        }
        long mgmtFee = repository.findCatalogItem(current.catalogItemId())
                .map(item -> switch (item.mgmtFeeMode()) {
                    case "percent" -> current.priceCent() * item.mgmtFeeValue() / 10000;
                    case "fixed" -> Math.min(item.mgmtFeeValue(), current.priceCent());
                    default -> 0L;
                }).orElse(0L);
        long dancerShare = current.priceCent() - mgmtFee;
        Instant now = Instant.now();
        ServiceOrderRecord saved = repository.saveOrder(new ServiceOrderRecord(
                current.id(), current.catalogItemId(), current.itemName(), current.priceCent(),
                current.barId(), current.tableId(), current.performerUserId(), current.orderedBy(),
                current.customerNote(), "completed", current.cancelReason(),
                current.paidAt(), current.paidBy(), current.payMethod(), current.refundNote(),
                mgmtFee, dancerShare, current.createdAt(), now, now));
        String month = java.time.YearMonth.from(java.time.LocalDate.ofInstant(now, java.time.ZoneOffset.UTC)).toString();
        staffRepository.insertCommission("cml_" + UUID.randomUUID(), saved.performerUserId(), month,
                "service_order", saved.id(), dancerShare, saved.itemName(), now);
        auditEventService.recordSuccess("service.order.completed", "service.order.create",
                "service_order", saved.id(), null,
                request == null ? null : request.idempotencyKey(),
                Map.of("mgmt_fee_cent", mgmtFee, "dancer_share_cent", dancerShare));
        return saved;
    }

    private ServiceOrderRecord require(String id) {
        return repository.findOrder(id)
                .orElseThrow(() -> ApiException.notFound("SERVICE_ORDER_NOT_FOUND", "service order not found"));
    }

    private ServiceOrderRecord persistStatus(ServiceOrderRecord current, String toStatus, Instant completedAt,
            String event, String idempotencyKey) {
        Instant now = Instant.now();
        ServiceOrderRecord saved = repository.saveOrder(new ServiceOrderRecord(
                current.id(), current.catalogItemId(), current.itemName(), current.priceCent(),
                current.barId(), current.tableId(), current.performerUserId(), current.orderedBy(),
                current.customerNote(), toStatus, current.cancelReason(),
                current.paidAt(), current.paidBy(), current.payMethod(), current.refundNote(),
                current.mgmtFeeCent(), current.dancerShareCent(), current.createdAt(), now,
                completedAt == null ? current.completedAt() : completedAt));
        auditEventService.recordSuccess(event, "service.order.perform", "service_order", saved.id(), null,
                idempotencyKey, Map.of("status", toStatus));
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
        // H批:已收款(公司账户)后取消 → 记退款备注(公司线下退,财务对账留痕)。
        String refundNote = current.paidAt() == null ? current.refundNote()
                : "已收款待退:" + request.reason().trim();
        ServiceOrderRecord saved = repository.saveOrder(new ServiceOrderRecord(
                current.id(), current.catalogItemId(), current.itemName(), current.priceCent(),
                current.barId(), current.tableId(), current.performerUserId(), current.orderedBy(),
                current.customerNote(), "cancelled", request.reason().trim(),
                current.paidAt(), current.paidBy(), current.payMethod(), refundNote,
                current.mgmtFeeCent(), current.dancerShareCent(), current.createdAt(), now,
                current.completedAt()));
        auditEventService.recordSuccess("service.order.cancelled", "service.order.create",
                "service_order", saved.id(), null, request.idempotencyKey(),
                Map.of("reason", request.reason().trim()));
        return saved;
    }

    /** H批:向客人出示**公司统一收款码**(代收;个人码流程已废弃)。 */
    public Map<String, String> orderPaymentCode(String id) {
        authorizationService.requirePermission("service.order.create");
        ServiceOrderRecord order = require(id);
        String qr = repository.companyPayQr();
        if (qr.isBlank()) {
            throw ApiException.badRequest("COMPANY_PAY_QR_NOT_CONFIGURED",
                    "company payment QR not configured (services pay-config)");
        }
        return Map.of("performer_user_id", order.performerUserId(), "qr_content", qr,
                "price_cent", String.valueOf(order.priceCent()));
    }

    /** H批:公司统一收款码配置(经营配置;财务对账基准)。 */
    public void setPayConfig(com.complex.platform.serviceorder.dto.ServicePayConfigRequest request) {
        authorizationService.requirePermission("service.catalog.manage");
        if (request == null || request.qrContent() == null || request.qrContent().isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "qr_content is required");
        }
        var actor = authorizationService.currentActor();
        repository.upsertPayConfig(request.qrContent().trim(), actor == null ? null : actor.id(), Instant.now());
        auditEventService.recordSuccess("service.pay_config.saved", "service.catalog.manage",
                "service_pay_config", "default", null, request.idempotencyKey(), Map.of());
    }

    public Map<String, String> payConfig() {
        authorizationService.requirePermission("service.order.create");
        return Map.of("qr_content", repository.companyPayQr());
    }

    public PageResponse<ServiceOrderRecord> listOrders(java.time.LocalDate date, String status) {
        authorizationService.requirePermission("service.catalog.manage");
        // P0 租户隔离(V214):项目员工只见本综合体服务单;平台管理员全量。
        return page(repository.listOrders(date, status,
                authorizationService.currentProjectScopeQuietly().orElse(null)));
    }

    /** H批:履约人当月完成次数+分账应得(公司代收,月结进薪资提成并代扣个税)。 */
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
