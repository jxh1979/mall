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
 * 服务点单(G批六口径拍板;H批 2026-07-02 最终拍板将④修订为公司统一账户代收):
 * ① 后台目录固定价;② 服务员代客下单(客人App自点接口预留);③ 下单即指定履约人;
 * ④ 公司代收(推翻直付)——客人扫公司统一收款码付款,全额进公司账户,服务员现场确认收款;
 *   完成履约时分账:管理费归公司,履约人应得写提成台账,月结进薪资并按劳务报酬税表代扣个税;
 * ⑤ 履约前可取消(已收款后取消记退款备注,公司线下退),完成后不可退;
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

    /** openapi-p0 契约:失败路径落 x-audit-event-failure 事件后再抛(审计闭合,2026-07-03 P1)。 */
    private <T> T withFailureAudit(String failureEvent, String permissionCode, String businessType,
            String businessId, String idempotencyKey, java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(failureEvent, permissionCode, businessType, businessId,
                    null, idempotencyKey,
                    exception instanceof ApiException apiException ? apiException.code()
                            : exception.getClass().getSimpleName());
            throw exception;
        }
    }

    public ServiceCatalogItemRecord upsertCatalogItem(com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest request) {
        return withFailureAudit("service.catalog.save_failed", "service.catalog.manage",
                "service_catalog_item", request == null ? null : request.id(),
                request == null ? null : request.idempotencyKey(),
                () -> doUpsertCatalogItem(request));
    }

    private ServiceCatalogItemRecord doUpsertCatalogItem(com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest request) {
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
        return page(repository.listCatalog(true, false));
    }

    public PageResponse<ServiceCatalogItemRecord> listCatalogAll() {
        authorizationService.requirePermission("service.catalog.manage");
        return page(repository.listCatalog(false, true));
    }

    public ServiceCatalogItemRecord archiveCatalogItem(String id, boolean archive, String reason) {
        return withFailureAudit("service.catalog.archive_failed", "service.catalog.manage", "service_catalog_item", id, null, () -> {
            authorizationService.requirePermission("service.catalog.manage");
            repository.findCatalogItem(id).orElseThrow(() -> ApiException.notFound("NOT_FOUND", "catalog item not found"));
            var actor = authorizationService.currentActor();
            String r = null;
            if (archive) {
                if (reason == null || reason.isBlank()) throw ApiException.badRequest("VALIDATION_FAILED", "\u5b58\u6863\u7406\u7531\u4e0d\u80fd\u4e3a\u7a7a");
                r = reason.trim();
            }
            repository.archiveCatalogItem(id, archive, r, actor == null ? null : actor.id());
            auditEventService.recordSuccess("service.catalog." + (archive ? "archived" : "unarchived"), "service.catalog.manage",
                    "service_catalog_item", id, null, null, Map.of());
            return repository.findCatalogItem(id).orElseThrow();
        });
    }

    public ServiceCatalogItemRecord requestDeleteCatalogItem(String id, String reason) {
        return withFailureAudit("service.catalog.delete_request_failed", "service.catalog.manage", "service_catalog_item", id, null, () -> {
            authorizationService.requirePermission("service.catalog.manage");
            var item = repository.findCatalogItem(id).orElseThrow(() -> ApiException.notFound("NOT_FOUND", "catalog item not found"));
            if (item.archivedAt() == null) throw ApiException.badRequest("VALIDATION_FAILED", "\u8bf7\u5148\u5b58\u6863\u518d\u7533\u8bf7\u5220\u9664");
            if (reason == null || reason.isBlank()) throw ApiException.badRequest("VALIDATION_FAILED", "\u5220\u9664\u7406\u7531\u4e0d\u80fd\u4e3a\u7a7a");
            var actor = authorizationService.currentActor();
            repository.requestDeleteCatalogItem(id, reason.trim(), actor == null ? null : actor.id());
            auditEventService.recordSuccess("service.catalog.delete_requested", "service.catalog.manage", "service_catalog_item", id, null, null, Map.of());
            return repository.findCatalogItem(id).orElseThrow();
        });
    }

    public ServiceCatalogItemRecord approveDeleteCatalogItem(String id, String decision) {
        return withFailureAudit("service.catalog.delete_approve_failed", "service.catalog.approve", "service_catalog_item", id, null, () -> {
            authorizationService.requirePermission("service.catalog.approve");
            var item = repository.findCatalogItem(id).orElseThrow(() -> ApiException.notFound("NOT_FOUND", "catalog item not found"));
            if (!"pending".equals(item.deleteStatus())) throw ApiException.badRequest("VALIDATION_FAILED", "\u8be5\u9879\u65e0\u5f85\u5ba1\u6279\u5220\u9664");
            var actor = authorizationService.currentActor();
            if (actor != null && actor.id() != null && actor.id().equals(item.deleteRequestedBy()))
                throw ApiException.forbidden("SELF_APPROVAL_FORBIDDEN", "\u4e0d\u80fd\u5ba1\u6279\u81ea\u5df1\u53d1\u8d77\u7684\u5220\u9664\u7533\u8bf7");
            boolean approved = "approved".equals(decision);
            repository.approveDeleteCatalogItem(id, approved);
            auditEventService.recordSuccess("service.catalog.delete_" + (approved ? "approved" : "rejected"), "service.catalog.approve", "service_catalog_item", id, null, null, Map.of());
            return item;
        });
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
        return withFailureAudit("service.order.create_failed", "service.order.create",
                "service_order", null, request == null ? null : request.idempotencyKey(), () -> {
            authorizationService.requirePermission("service.order.create");
            return idempotencyService.execute("createServiceOrder",
                    request == null ? null : request.idempotencyKey(), request,
                    () -> doCreateOrder(request), ServiceOrderRecord.class);
        });
    }

    private ServiceOrderRecord doCreateOrder(com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest request) {
        if (request == null || request.catalogItemId() == null || request.catalogItemId().isBlank()
                || request.performerUserId() == null || request.performerUserId().isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "catalog_item_id and performer_user_id required");
        }
        // 清单 P1:项目员工只可为本综合体履约人下单(空 scope=平台放行)。守卫先于目录查找。
        requirePerformerInScope(request.performerUserId().trim());
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
                "created", null, null, null, null, null, 0, 0, now, now, null,
                item.mgmtFeeMode(), item.mgmtFeeValue()));
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
        return withFailureAudit("service.order.accept_failed", "service.order.perform",
                "service_order", id, request == null ? null : request.idempotencyKey(), () -> {
            authorizationService.requirePermission("service.order.perform");
            return idempotencyService.execute("acceptServiceOrder:" + id,
                    request == null ? null : request.idempotencyKey(), request,
                    () -> doAccept(id, request), ServiceOrderRecord.class);
        });
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
        return withFailureAudit("service.order.pay_confirm_failed", "service.order.create",
                "service_order", id, request == null ? null : request.idempotencyKey(), () -> {
            authorizationService.requirePermission("service.order.create");
            return idempotencyService.execute("markServiceOrderPaid:" + id,
                    request == null ? null : request.idempotencyKey(), request,
                    () -> doMarkPaid(id, request), ServiceOrderRecord.class);
        });
    }

    private ServiceOrderRecord doMarkPaid(String id, com.complex.platform.serviceorder.dto.ServiceOrderMarkPaidRequest request) {
        ServiceOrderRecord current = require(id);
        requirePerformerInScope(current.performerUserId());
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
                current.createdAt(), now, current.completedAt(),
                current.snapMgmtFeeMode(), current.snapMgmtFeeValue()));
        // P1 收款对账:落一条公司应收/已收流水到复用的 ar_ap_items(带订单号+金额+时间;确认人=paid_by)。
        // ponytail: 价为 0 的单不生成应收行(ar_ap_items.amount_cent>0 CHECK);正价单幂等一单一条。
        if (saved.priceCent() > 0) {
            repository.insertReceivableForOrder(saved.id(), saved.priceCent(), now);
        }
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
        return withFailureAudit("service.order.complete_failed", "service.order.create",
                "service_order", id, request == null ? null : request.idempotencyKey(), () -> {
            authorizationService.requirePermission("service.order.create");
            return idempotencyService.execute("completeServiceOrder:" + id,
                    request == null ? null : request.idempotencyKey(), request,
                    () -> doComplete(id, request), ServiceOrderRecord.class);
        });
    }

    private ServiceOrderRecord doComplete(String id, com.complex.platform.serviceorder.dto.ServiceOrderActionRequest request) {
        ServiceOrderRecord current = require(id);
        requirePerformerInScope(current.performerUserId());
        // P2-#4:仅 paid 可完成(移除 accepted 绕过路径;paid 蕴含 paid_at!=null,无收款不得完成)。
        if (!current.status().equals("paid")) {
            throw ApiException.badRequest("NOT_PAID_YET",
                    "order must be paid (company account) before completion; status=" + current.status());
        }
        // P2-#3:分账费率取下单快照(非完成时活目录);存量无快照的历史单回退活目录。
        String feeMode = current.snapMgmtFeeMode();
        Long feeValue = current.snapMgmtFeeValue();
        if (feeMode == null || feeValue == null) {
            var live = repository.findCatalogItem(current.catalogItemId());
            feeMode = live.map(ServiceCatalogItemRecord::mgmtFeeMode).orElse("none");
            feeValue = live.map(ServiceCatalogItemRecord::mgmtFeeValue).orElse(0L);
        }
        final long feeValueFinal = feeValue;
        long mgmtFee = switch (feeMode) {
            case "percent" -> current.priceCent() * feeValueFinal / 10000;
            case "fixed" -> Math.min(feeValueFinal, current.priceCent());
            default -> 0L;
        };
        long dancerShare = current.priceCent() - mgmtFee;
        Instant now = Instant.now();
        // P1 并发守卫:paid→completed 原子转移作为唯一进入凭据。受影响行数!=1 → 已被其它并发/重放拿走 → 拒。
        // ponytail: 用 DB 行级 CAS 取代应用层 read-then-write,彻底消除多 key 并发多完成/多分账。
        if (repository.markCompletedIfPaid(current.id(), mgmtFee, dancerShare, now, now) != 1) {
            throw ApiException.badRequest("ALREADY_COMPLETED",
                    "service order already completed or no longer payable; status changed concurrently");
        }
        // 赢得 CAS 后再入提成(部分唯一索引 + ON CONFLICT DO NOTHING,双保险幂等);再落审计。
        String month = java.time.YearMonth.from(java.time.LocalDate.ofInstant(now, java.time.ZoneOffset.UTC)).toString();
        staffRepository.insertCommission("cml_" + UUID.randomUUID(), current.performerUserId(), month,
                "service_order", current.id(), dancerShare, current.itemName(), now);
        ServiceOrderRecord saved = new ServiceOrderRecord(
                current.id(), current.catalogItemId(), current.itemName(), current.priceCent(),
                current.barId(), current.tableId(), current.performerUserId(), current.orderedBy(),
                current.customerNote(), "completed", current.cancelReason(),
                current.paidAt(), current.paidBy(), current.payMethod(), current.refundNote(),
                mgmtFee, dancerShare, current.createdAt(), now, now,
                current.snapMgmtFeeMode(), current.snapMgmtFeeValue());
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

    /** 清单 P1(2026-07-03 审计):按项目 scope 守卫——项目员工只可操作本综合体履约人的服务单;
     * 平台管理员(空 scope)放行。scope 由 token 派生(currentProjectScopeQuietly),不取请求体。 */
    private void requirePerformerInScope(String performerUserId) {
        authorizationService.currentProjectScopeQuietly().ifPresent(scope -> {
            String performerProject = staffRepository.findUserProjectId(performerUserId).orElse(null);
            if (!scope.equals(performerProject)) {
                throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN",
                        "service order performer is out of the caller's project scope");
            }
        });
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
                completedAt == null ? current.completedAt() : completedAt,
                current.snapMgmtFeeMode(), current.snapMgmtFeeValue()));
        auditEventService.recordSuccess(event, "service.order.perform", "service_order", saved.id(), null,
                idempotencyKey, Map.of("status", toStatus));
        return saved;
    }

    /** 口径⑤:履约前可取消(下单者或目录管理岗),留原因;完成后不可退。 */
    public ServiceOrderRecord cancel(String id, com.complex.platform.serviceorder.dto.ServiceOrderCancelRequest request) {
        return withFailureAudit("service.order.cancel_failed", "service.order.create",
                "service_order", id, request == null ? null : request.idempotencyKey(),
                () -> doCancel(id, request));
    }

    private ServiceOrderRecord doCancel(String id, com.complex.platform.serviceorder.dto.ServiceOrderCancelRequest request) {
        authorizationService.requirePermission("service.order.create");
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "cancel reason is required");
        }
        ServiceOrderRecord current = repository.findOrder(id)
                .orElseThrow(() -> ApiException.notFound("SERVICE_ORDER_NOT_FOUND", "service order not found"));
        requirePerformerInScope(current.performerUserId());
        if (current.status().equals("completed")) {
            throw ApiException.badRequest("ALREADY_COMPLETED",
                    "服务单已完成履约并完成分账,不能取消;如需退款走财务退款流程");
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
                current.completedAt(),
                current.snapMgmtFeeMode(), current.snapMgmtFeeValue()));
        auditEventService.recordSuccess("service.order.cancelled", "service.order.create",
                "service_order", saved.id(), null, request.idempotencyKey(),
                Map.of("reason", request.reason().trim()));
        return saved;
    }

    /** H批:向客人出示**公司统一收款码**(代收;个人码流程已废弃)。 */
    public Map<String, String> orderPaymentCode(String id) {
        authorizationService.requirePermission("service.order.create");
        ServiceOrderRecord order = require(id);
        requirePerformerInScope(order.performerUserId());
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
        withFailureAudit("service.pay_config.save_failed", "service.catalog.manage",
                "service_pay_config", "default", request == null ? null : request.idempotencyKey(), () -> {
            authorizationService.requirePermission("service.catalog.manage");
            if (request == null || request.qrContent() == null || request.qrContent().isBlank()) {
                throw ApiException.badRequest("VALIDATION_FAILED", "qr_content is required");
            }
            var actor = authorizationService.currentActor();
            repository.upsertPayConfig(request.qrContent().trim(), actor == null ? null : actor.id(), Instant.now());
            auditEventService.recordSuccess("service.pay_config.saved", "service.catalog.manage",
                    "service_pay_config", "default", null, request.idempotencyKey(), Map.of());
            return null;
        });
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
