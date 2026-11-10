package com.complex.platform.bar;

// 文件版本：V0.11
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bar")
public class BarController {

    private final BarService service;
    private final BarEntitlementRedemptionService entitlementRedemptionService;
    private final AuthorizationService authorizationService;
    /** V351 退酒原因码字典（后台可增删改；下拉数据源）。单测未注入=null，端点报不可用。 */
    private final BarVoidReasonCodeDirectory voidReasonCodeDirectory;
    private final AuditedOperationExecutor operations;
    private final AuditEventService audit;

    public BarController(
            BarService service,
            BarEntitlementRedemptionService entitlementRedemptionService,
            AuthorizationService authorizationService
    ) {
        this(service, entitlementRedemptionService, authorizationService, null, null, null);
    }

    // 第十八批 N 段第 124 条 124-R5（PGM L966「已拍板：所有有过程的业务动作都必须留痕……」、L738「访问被拒绝也要留痕……」）：写口任一步失败
    // 经 AuditedOperationExecutor.recordingFailure 落 OpenAPI 声明的 x-audit-event-failure 再原样抛出；服务层原本不写成功事件的，
    // 服务返回后在这里落声明的 x-audit-event-success。

    @org.springframework.beans.factory.annotation.Autowired
    public BarController(
            BarService service,
            BarEntitlementRedemptionService entitlementRedemptionService,
            AuthorizationService authorizationService,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            BarVoidReasonCodeDirectory voidReasonCodeDirectory,
            AuditedOperationExecutor operations,
            AuditEventService audit
    ) {
        this.service = service;
        this.entitlementRedemptionService = entitlementRedemptionService;
        this.authorizationService = authorizationService;
        this.voidReasonCodeDirectory = voidReasonCodeDirectory;
        this.operations = operations;
        this.audit = audit;
    }

    // ==================== V351 退酒原因码字典（出厂八条，后台可增删改）====================

    /** 列生效原因码（店员退酒下拉的数据源；include_inactive=true 时含已下线，后台维护用）。 */
    @GetMapping("/void-reason-codes")
    public ApiResponse<java.util.List<BarVoidReasonCodeRecord>> listVoidReasonCodes(
            @RequestParam(value = "include_inactive", required = false) Boolean includeInactive
    ) {
        // 发起退酒的店员就要看这份下拉，故用申请权限而非审批权限。
        authorizationService.requirePermission("bar.order_item.void");
        return ApiResponse.ok(requireVoidReasonCodeDirectory()
                .listReasonCodes(!Boolean.TRUE.equals(includeInactive)));
    }

    /** 新增/修改一条原因码（中文名与排序）。维护权归审批人（主管），与谁能选无关。 */
    @PostMapping("/void-reason-codes")
    public ApiResponse<BarVoidReasonCodeRecord> saveVoidReasonCode(
            @RequestBody(required = false) BarVoidReasonCodeRequest request
    ) {
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("bar.void_reason_code.save_failed", "bar.order_item.void_decide",
                "bar_void_reason_code", request == null || request.code() == null ? "new" : request.code(), key, () -> {
                    authorizationService.requirePermission("bar.order_item.void_decide");
                    BarVoidReasonCodeRequest actual = requireVoidReasonCodeRequest(request);
                    requireIdempotencyKey(actual.idempotencyKey());
                    BarVoidReasonCodeRecord saved = requireVoidReasonCodeDirectory().saveReasonCode(
                            actual.code(), actual.labelZh(), actual.sortOrder(),
                            authorizationService.currentActor().id());
                    audit.recordSuccess("bar.void_reason_code.saved", "bar.order_item.void_decide", "bar_void_reason_code", saved.code(),
                            null, key, java.util.Map.of());
                    return saved;
                }));
    }

    /** 下线一条原因码（软删铁律：置 inactive + 必填理由，历史退酒单仍引用得到）。 */
    @PostMapping("/void-reason-codes/{code}/archive")
    public ApiResponse<BarVoidReasonCodeRecord> archiveVoidReasonCode(
            @PathVariable("code") String code,
            @RequestBody(required = false) BarVoidReasonCodeRequest request
    ) {
        String key = request == null ? null : request.idempotencyKey();
        return ApiResponse.ok(operations.recordingFailure("bar.void_reason_code.archive_failed", "bar.order_item.void_decide",
                "bar_void_reason_code", code, key, () -> {
                    authorizationService.requirePermission("bar.order_item.void_decide");
                    BarVoidReasonCodeRequest actual = requireVoidReasonCodeRequest(request);
                    requireIdempotencyKey(actual.idempotencyKey());
                    BarVoidReasonCodeRecord archived = requireVoidReasonCodeDirectory().archiveReasonCode(
                            code, actual.archiveReason(), authorizationService.currentActor().id());
                    audit.recordSuccess("bar.void_reason_code.archived", "bar.order_item.void_decide", "bar_void_reason_code", code,
                            null, key, java.util.Map.of("archive_reason", actual.archiveReason() == null ? "" : actual.archiveReason()));
                    return archived;
                }));
    }

    private BarVoidReasonCodeDirectory requireVoidReasonCodeDirectory() {
        if (voidReasonCodeDirectory == null) {
            throw ApiException.badRequest("BAR_VOID_REASON_CODE_DIRECTORY_UNAVAILABLE",
                    "退酒原因码字典不可用");
        }
        return voidReasonCodeDirectory;
    }

    private BarVoidReasonCodeRequest requireVoidReasonCodeRequest(BarVoidReasonCodeRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    @GetMapping("/entitlement-redemptions")
    public ApiResponse<PageResponse<BarGiftRedemptionRecord>> listEntitlementRedemptions() {
        authorizationService.requirePermission("bar.entitlement.read");
        return ApiResponse.ok(entitlementRedemptionService.listRedemptions());
    }

    @PostMapping("/entitlement-redemptions/drink")
    public ApiResponse<BarGiftRedemptionRecord> redeemDrinkEntitlement(
            @RequestBody(required = false) @jakarta.validation.Valid BarEntitlementDrinkRedemptionRequest request
    ) {
        authorizationService.requirePermission("bar.entitlement.redeem");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(entitlementRedemptionService.redeemDrink(request));
    }

    @PostMapping("/bills/{bill_id}/commissions/calculate")
    public ApiResponse<BarCommissionCalculationRecord> calculateCommission(
            @PathVariable("bill_id") String billId,
            @RequestBody(required = false) @jakarta.validation.Valid CommissionCalculateRequest request
    ) {
        authorizationService.requirePermission("bar.table_commission.calculate");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.calculateCommission(billId, request));
    }

    @PostMapping("/bills/{bill_id}/pay-intents")
    public ApiResponse<PaymentIntentRecord> createBillPayIntent(
            @PathVariable("bill_id") String billId,
            @RequestBody(required = false) @jakarta.validation.Valid PaymentIntentRequest request
    ) {
        authorizationService.requirePermission("finance.payment_intent.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createBillPayIntent(billId, request));
    }

    @PostMapping("/bills/{bill_id}/stored-value/sms")
    public ApiResponse<java.util.Map<String, String>> requestStoredValueCode(
            @PathVariable("bill_id") String billId,
            @RequestBody java.util.Map<String, String> request) {
        authorizationService.requirePermission("finance.payment.register");
        service.requestStoredValueCode(billId, request.get("phone"));
        return ApiResponse.ok(java.util.Map.of("message", "验证码已发送"));
    }

    @PostMapping("/bills/{bill_id}/settle")
    public ApiResponse<BarBillRecord> settleBill(
            @PathVariable("bill_id") String billId,
            @RequestBody(required = false) @jakarta.validation.Valid BarBillSettleRequest request
    ) {
        authorizationService.requirePermission("finance.payment.register");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.settleBill(billId, request));
    }

    @PostMapping("/bills/{bill_id}/adjustments")
    public ApiResponse<BarBillAdjustmentRecord> applyBillAdjustment(
            @PathVariable("bill_id") String billId,
            @RequestBody(required = false) @jakarta.validation.Valid BarBillAdjustmentRequest request
    ) {
        authorizationService.requirePermission("bar.bill.adjust");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.applyBillAdjustment(billId, request));
    }

    @GetMapping("/bills/{bill_id}/operations")
    public ApiResponse<PageResponse<BarBillOperationRecord>> listBillOperations(@PathVariable("bill_id") String billId) {
        authorizationService.requirePermission("bar.bill.split_merge");
        return ApiResponse.ok(service.listBillOperations(billId));
    }

    /** 第十一批 TK-04-R5（ENUM L77）：从账单反查本单产生的商户消费积分与成长值。 */
    @GetMapping("/bills/{bill_id}/rewards")
    public ApiResponse<PageResponse<com.complex.platform.customer.MerchantConsumptionRewardService.RewardView>> listBillRewards(
            @PathVariable("bill_id") String billId) {
        authorizationService.requirePermission("bar.order.view");
        return ApiResponse.ok(service.listBillRewards(billId));
    }

    @PostMapping("/bills/{bill_id}/split")
    public ApiResponse<BarBillOperationRecord> splitBill(
            @PathVariable("bill_id") String billId,
            @RequestBody(required = false) @jakarta.validation.Valid BarBillSplitRequest request
    ) {
        authorizationService.requirePermission("bar.bill.split_merge");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.splitBill(billId, request));
    }

    @PostMapping("/bills/{bill_id}/merge")
    public ApiResponse<BarBillOperationRecord> mergeBills(
            @PathVariable("bill_id") String billId,
            @RequestBody(required = false) @jakarta.validation.Valid BarBillMergeRequest request
    ) {
        authorizationService.requirePermission("bar.bill.split_merge");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.mergeBills(billId, request));
    }

    @GetMapping("/orders")
    public ApiResponse<PageResponse<BarOrderView>> listOrders() {
        authorizationService.requirePermission("bar.order.view");
        return ApiResponse.ok(service.listOrders());
    }

    @PostMapping("/orders")
    public ApiResponse<BarOrderRecord> createOrder(@RequestBody(required = false) @jakarta.validation.Valid BarOrderRequest request) {
        authorizationService.requirePermission("bar.order.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createOrder(request));
    }

    @PostMapping("/table-sessions/{session_id}/scan-orders")
    public ApiResponse<BarOrderRecord> placeScanOrder(
            @PathVariable("session_id") String sessionId,
            @RequestBody(required = false) @jakarta.validation.Valid BarOrderRequest request
    ) {
        authorizationService.requirePermission("bar.order.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.placeScanOrder(sessionId, request));
    }

    @PostMapping("/orders/{order_id}/bill")
    public ApiResponse<BarBillRecord> createOrderBill(
            @PathVariable("order_id") String orderId,
            @RequestBody(required = false) @jakarta.validation.Valid BarOrderBillCreateRequest request
    ) {
        authorizationService.requirePermission("bar.order.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createOrderBill(orderId, request));
    }

    @PostMapping("/orders/{order_id}/deliver")
    public ApiResponse<BarOrderRecord> deliverOrder(
            @PathVariable("order_id") String orderId,
            @RequestBody(required = false) @jakarta.validation.Valid BarOrderDeliveryRequest request
    ) {
        authorizationService.requirePermission("bar.order.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.deliverOrder(orderId, request));
    }

    @PostMapping("/orders/{order_id}/void-item")
    public ApiResponse<BarOrderItemVoidRecord> voidOrderItem(
            @PathVariable("order_id") String orderId,
            @RequestBody(required = false) @jakarta.validation.Valid BarOrderItemVoidRequest request
    ) {
        authorizationService.requirePermission("bar.order_item.void");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.voidOrderItem(orderId, request));
    }

    /** V345（BAR-VOID-A1）退酒审批待审队列：默认 status=pending；order_id 非空则只看该订单的申请。 */
    @GetMapping("/order-item-voids")
    public ApiResponse<PageResponse<BarOrderItemVoidRecord>> listOrderItemVoids(
            @RequestParam(value = "order_id", required = false) String orderId,
            @RequestParam(value = "status", required = false) String status
    ) {
        authorizationService.requirePermission("bar.order_item.void");
        return ApiResponse.ok(service.listOrderItemVoids(orderId, status));
    }

    @PostMapping("/order-item-voids/{void_id}/decision")
    public ApiResponse<BarOrderItemVoidRecord> decideOrderItemVoid(
            @PathVariable("void_id") String voidId,
            @RequestBody(required = false) @jakarta.validation.Valid BarOrderItemVoidDecisionRequest request
    ) {
        // V125 P1-2：审批分离——决策端点用独立审批权限点，与申请权限 bar.order_item.void 区分，杜绝自审批。
        authorizationService.requirePermission("bar.order_item.void_decide");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.decideOrderItemVoid(voidId, request));
    }

    @PostMapping("/bills/{bill_id}/reopen")
    public ApiResponse<BarBillOperationRecord> reopenBill(
            @PathVariable("bill_id") String billId,
            @RequestBody(required = false) @jakarta.validation.Valid BarBillReopenRequest request
    ) {
        authorizationService.requirePermission("bar.bill.reopen");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.reopenBill(billId, request));
    }

    /** 第十六批第 16 条：客人对账单提出争议时，有权员工登记争议原因，账单置异常（沿用账单调整权限）。 */
    @PostMapping("/bills/{bill_id}/anomaly")
    public ApiResponse<BarBillRecord> raiseBillAnomaly(
            @PathVariable("bill_id") String billId,
            @RequestBody(required = false) @jakarta.validation.Valid BarBillAnomalyRequest request
    ) {
        authorizationService.requirePermission("bar.bill.adjust");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.raiseBillAnomaly(billId, request));
    }

    /** 第十六批第 16 条（ENUM「异常状态的统一出口」）：主管选「更正后恢复」或「作废并冲正」处置异常账单，原因必填。 */
    @PostMapping("/bills/{bill_id}/anomaly/resolve")
    public ApiResponse<BarBillRecord> resolveBillAnomaly(
            @PathVariable("bill_id") String billId,
            @RequestBody(required = false) @jakarta.validation.Valid BarBillAnomalyResolveRequest request
    ) {
        authorizationService.requirePermission("bar.bill.anomaly.resolve");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.resolveBillAnomaly(billId, request));
    }

    /** 第十八批 T 段第 157 条 157-R2（BSM:1106「收银员可以直接挂起……必须填写原因」；第十八批裁决 37「只有包厢能挂起」）：挂起包厢未付款订单。 */
    @PostMapping("/orders/{order_id}/suspend")
    public ApiResponse<BarOrderSuspensionService.Suspension> suspendOrder(
            @PathVariable("order_id") String orderId,
            @RequestBody(required = false) @jakarta.validation.Valid BarOrderSuspensionService.SuspendRequest request
    ) {
        authorizationService.requirePermission("bar.bill.hang");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.suspendOrder(orderId, request));
    }

    /** 第十八批 T 段第 157 条 157-R3（BSM:1109「经理确认最终无法收款后转为坏账损耗处理中」）：经理确认挂起订单无法收款（转坏账）。 */
    @PostMapping("/orders/{order_id}/suspension/bad-debt")
    public ApiResponse<BarOrderSuspensionService.Suspension> confirmOrderSuspensionBadDebt(
            @PathVariable("order_id") String orderId,
            @RequestBody(required = false) @jakarta.validation.Valid BarOrderSuspensionService.BadDebtRequest request
    ) {
        // 157-R5：鉴权移到服务层——持 bar.bill.hang_cancel，或关台未付清待办升级后的店长（bar.bill.hang_escalation）与本项目夜班值班经理。
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.confirmSuspensionBadDebt(orderId, request));
    }

    /** 第十八批 T 段第 157 条 157-R3（BSM:1111「财务确认……均完成后，订单转为已核销结案」）：财务确认坏账订单核销结案。 */
    @PostMapping("/orders/{order_id}/suspension/write-off")
    public ApiResponse<BarOrderSuspensionService.Suspension> writeOffOrderSuspension(
            @PathVariable("order_id") String orderId,
            @RequestBody(required = false) @jakarta.validation.Valid BarOrderSuspensionService.WriteOffRequest request
    ) {
        authorizationService.requirePermission("finance.ar_ap.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.writeOffSuspension(orderId, request));
    }

    /** 第十八批 T 段第 157 条 157-R6（BSM:1107「已出品挂起订单取消须经理审批」）：收银员对挂起订单发起取消（勾选可回库的已出品明细）。 */
    @PostMapping("/orders/{order_id}/suspension/cancel")
    public ApiResponse<BarOrderSuspensionService.Suspension> requestOrderSuspensionCancel(
            @PathVariable("order_id") String orderId,
            @RequestBody(required = false) @jakarta.validation.Valid BarOrderSuspensionService.CancelRequest request
    ) {
        authorizationService.requirePermission("bar.bill.hang");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.requestSuspensionCancel(orderId, request));
    }

    /** 157-R6（PGM:699「经理审批取消」；BSM:1108「取消审批生效与付款成功只能有一项先完成，先完成者生效」）：经理批准或驳回挂起订单取消。 */
    @PostMapping("/orders/{order_id}/suspension/cancel-decision")
    public ApiResponse<BarOrderSuspensionService.Suspension> decideOrderSuspensionCancel(
            @PathVariable("order_id") String orderId,
            @RequestBody(required = false) @jakarta.validation.Valid BarOrderSuspensionService.CancelDecisionRequest request
    ) {
        authorizationService.requirePermission("bar.bill.hang_cancel");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.decideSuspensionCancel(orderId, request));
    }

    @PostMapping("/bills/{bill_id}/tip")
    public ApiResponse<BarBillRecord> addBillTip(
            @PathVariable("bill_id") String billId,
            @RequestBody(required = false) @jakarta.validation.Valid BarBillTipRequest request
    ) {
        authorizationService.requirePermission("bar.bill.tip");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.addBillTip(billId, request));
    }

    @GetMapping("/shifts")
    public ApiResponse<PageResponse<BarShiftRecord>> listShifts() {
        // 第十八批 R 段第 142 条 142-R3（BSM:1848「财务看退款、差异、日结和结算依据。」；PGM:664「差异认定……由该酒吧对应的财务 BP 负责」）：
        // 持交班管理码照旧；否则须持日结管理码（对账页收银交班复核区块，服务层按该码可触及的项目列）。
        if (!authorizationService.currentActorHasPermission("bar.shift.manage")) {
            authorizationService.requirePermission(BarShiftService.FINANCE_REVIEW);
        }
        return ApiResponse.ok(service.listShifts());
    }

    @PostMapping("/shifts/open")
    public ApiResponse<BarShiftRecord> openShift(@RequestBody(required = false) @jakarta.validation.Valid BarShiftOpenRequest request) {
        authorizationService.requirePermission("bar.shift.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.openShift(request));
    }

    @PostMapping("/shifts/{shift_id}/close")
    public ApiResponse<BarShiftRecord> closeShift(
            @PathVariable("shift_id") String shiftId,
            @RequestBody(required = false) @jakarta.validation.Valid BarShiftCloseRequest request
    ) {
        authorizationService.requirePermission("bar.shift.close");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.closeShift(shiftId, request));
    }

    // 第十八批 R 段第 142 条 142-R4：加入同一钱箱（多人共同使用同一钱箱当班，加入后其收款并入本班次）。
    @PostMapping("/shifts/{shift_id}/join")
    public ApiResponse<BarShiftRecord> joinShift(
            @PathVariable("shift_id") String shiftId,
            @RequestBody(required = false) @jakarta.validation.Valid BarShiftActionRequest request
    ) {
        authorizationService.requirePermission("bar.shift.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.joinShift(shiftId, request));
    }

    // 第十八批 R 段第 142 条 142-R1：财务复核平账（短款确认已当场补足、长款登记原因并计该商户营业外收入）。
    @PostMapping("/shifts/{shift_id}/resolve")
    public ApiResponse<BarShiftRecord> resolveShift(
            @PathVariable("shift_id") String shiftId,
            @RequestBody(required = false) @jakarta.validation.Valid BarShiftResolveRequest request
    ) {
        authorizationService.requirePermission("finance.daily_close.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.resolveShift(shiftId, request));
    }

    // 第十八批 R 段第 142 条 142-R2：交班单签字（当班收银员与接班人各签本人一栏）。
    @PostMapping("/shifts/{shift_id}/sign")
    public ApiResponse<BarShiftRecord> signShift(
            @PathVariable("shift_id") String shiftId,
            @RequestBody(required = false) @jakarta.validation.Valid BarShiftActionRequest request
    ) {
        authorizationService.requirePermission("bar.shift.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.signShift(shiftId, request));
    }

    // 第十八批 R 段第 142 条 142-R2：财务见证归档（最后一班由见证财务代接班人签字）。
    @PostMapping("/shifts/{shift_id}/witness")
    public ApiResponse<BarShiftRecord> witnessShift(
            @PathVariable("shift_id") String shiftId,
            @RequestBody(required = false) @jakarta.validation.Valid BarShiftActionRequest request
    ) {
        authorizationService.requirePermission("finance.daily_close.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.witnessShift(shiftId, request));
    }

    @PostMapping("/products/{product_id}/sold-out")
    public ApiResponse<BarProductRecord> markProductSoldOut(
            @PathVariable("product_id") String productId,
            @RequestBody(required = false) @jakarta.validation.Valid BarProductSoldOutRequest request
    ) {
        authorizationService.requirePermission("bar.product.sold_out");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.markProductSoldOut(productId, request));
    }

    @PostMapping("/orders/{order_id}/items")
    public ApiResponse<BarOrderItemRecord> addOrderItem(
            @PathVariable("order_id") String orderId,
            @RequestBody(required = false) @jakarta.validation.Valid BarOrderRequest request
    ) {
        authorizationService.requirePermission("bar.order.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.addOrderItem(orderId, request));
    }

    @GetMapping("/production-tickets")
    public ApiResponse<PageResponse<BarProductionTicketRecord>> listProductionTickets() {
        authorizationService.requirePermission("bar.production_ticket.view");
        return ApiResponse.ok(service.listProductionTickets());
    }

    @GetMapping("/production-tickets/{ticket_id}/events")
    public ApiResponse<PageResponse<BarProductionTicketEventRecord>> listProductionTicketEvents(
            @PathVariable("ticket_id") String ticketId
    ) {
        authorizationService.requirePermission("bar.production_ticket.view");
        return ApiResponse.ok(service.listProductionTicketEvents(ticketId));
    }

    @PostMapping("/production-tickets/{ticket_id}/transition")
    public ApiResponse<BarProductionTicketRecord> transitionProductionTicket(
            @PathVariable("ticket_id") String ticketId,
            @RequestBody(required = false) @jakarta.validation.Valid BarProductionTicketTransitionRequest request
    ) {
        authorizationService.requirePermission("bar.production_ticket.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.transitionProductionTicket(ticketId, request));
    }

    @GetMapping("/products")
    public ApiResponse<PageResponse<BarProductRecord>> listProducts(
            @RequestParam(value = "includeArchived", required = false, defaultValue = "false") boolean includeArchived) {
        // 商品目录含敏感内部结算价（V152），查看独立授权（不再搭便车 bar.order.create）。
        authorizationService.requirePermission("bar.product.view");
        PageResponse<BarProductRecord> page = service.listProducts(includeArchived);
        if (!authorizationService.currentActorHasPermission("bar.internal_settlement_price.view")) {
            page = new PageResponse<>(page.items().stream().map(BarProductRecord::withoutInternalPrice).toList(),
                    page.page(), page.pageSize(), page.total());
        }
        return ApiResponse.ok(page);
    }

    @PostMapping("/products")
    public ApiResponse<BarProductRecord> createProduct(@RequestBody(required = false) @jakarta.validation.Valid BarProductRequest request) {
        // 录入/修改商品（含内部结算价）为高危动作，需专用高危权限。
        authorizationService.requirePermission("bar.product.manage");
        // AUD-043 写侧门控（批10/E 收编后语义不变）：挂内部结算 SKU 目录引用 = 建立成本价基，仍需内部价
        // 专用权限，与读侧脱敏对称（gov:197 仅授权角色可查看和录入）。直填 internalSettlementPriceCent 由服务层一律 400。
        if (request != null && request.catalogItemCode() != null
                && !authorizationService.currentActorHasPermission("bar.internal_settlement_price.view")) {
            throw ApiException.forbidden("BAR_INTERNAL_PRICE_FORBIDDEN",
                    "绑定内部结算价目录项需要内部结算价查看权限");
        }
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createProduct(request));
    }

    @GetMapping("/reservations")
    public ApiResponse<PageResponse<BarReservationRecord>> listReservations(
            @org.springframework.web.bind.annotation.RequestParam(value = "include_archived", required = false, defaultValue = "false") boolean includeArchived) {
        authorizationService.requirePermission("bar.reservation.view");
        return ApiResponse.ok(service.listReservations(includeArchived));
    }

    @PostMapping("/reservations/{id}/archive")
    public ApiResponse<BarReservationRecord> archiveReservation(@PathVariable String id,
            @org.springframework.web.bind.annotation.RequestBody(required = false) BarCatalogArchiveRequest request) {
        authorizationService.requirePermission("bar.reservation.manage");
        return ApiResponse.ok(service.archiveReservation(id, true, request == null ? null : request.reason()));
    }

    @PostMapping("/reservations/{id}/unarchive")
    public ApiResponse<BarReservationRecord> unarchiveReservation(@PathVariable String id) {
        authorizationService.requirePermission("bar.reservation.manage");
        return ApiResponse.ok(service.archiveReservation(id, false, null));
    }

    @PostMapping("/reservations/{id}/delete-request")
    public ApiResponse<BarReservationRecord> requestDeleteReservation(
            @PathVariable String id,
            @RequestBody(required = false) BarCatalogDeleteRequest request) {
        authorizationService.requirePermission("bar.reservation.manage");
        return ApiResponse.ok(service.requestDeleteReservation(id, request));
    }

    @PostMapping("/reservations/{id}/delete-approve")
    public ApiResponse<BarCatalogDeleteDecision> approveDeleteReservation(
            @PathVariable String id,
            @RequestBody(required = false) BarCatalogDeleteApprovalRequest request) {
        authorizationService.requirePermission("bar.reservation.approve");
        return ApiResponse.ok(service.approveDeleteReservation(id, request));
    }

    @PostMapping("/reservations")
    public ApiResponse<BarReservationRecord> createReservation(@RequestBody(required = false) @jakarta.validation.Valid BarReservationRequest request) {
        authorizationService.requirePermission("bar.reservation.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createReservation(request));
    }

    /** 第十八批 C 段第 15 条（BSM 第 1468 行）：代订权限因爽约率超限停用中的经手员工（店长复核用）。 */
    @GetMapping("/staff-booking-suspensions")
    public ApiResponse<PageResponse<BarStaffBookingSuspensionRecord>> listStaffBookingSuspensions() {
        authorizationService.requirePermission("bar.staff_booking.restore");
        return ApiResponse.ok(service.listStaffBookingSuspensions());
    }

    /** 第十八批 C 段第 15 条：店长复核后恢复经手员工的代订权限。 */
    @PostMapping("/staff-booking-suspensions/{id}/restore")
    public ApiResponse<BarStaffBookingSuspensionRecord> restoreStaffBookingSuspension(
            @PathVariable String id,
            @RequestBody(required = false) BarStaffBookingSuspensionRecord.RestoreRequest request
    ) {
        authorizationService.requirePermission("bar.staff_booking.restore");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.restoreStaffBookingSuspension(id, request));
    }

    @PostMapping("/reservations/{id}/pay-deposit")
    public ApiResponse<BarReservationRecord> payReservationDeposit(
            @PathVariable String id,
            @RequestBody(required = false) @jakarta.validation.Valid BarReservationDepositActionRequest request
    ) {
        authorizationService.requirePermission("bar.reservation.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.payReservationDeposit(id, request));
    }

    @PostMapping("/reservations/{id}/checkin")
    public ApiResponse<BarReservationRecord> checkInReservationDeposit(
            @PathVariable String id,
            @RequestBody(required = false) @jakarta.validation.Valid BarReservationDepositActionRequest request
    ) {
        authorizationService.requirePermission("bar.reservation.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.checkInReservationDeposit(id, request));
    }

    @PostMapping("/reservations/{id}/cancel")
    public ApiResponse<BarReservationRecord> cancelReservation(
            @PathVariable String id,
            @RequestBody(required = false) @jakarta.validation.Valid BarReservationCancelRequest request
    ) {
        authorizationService.requirePermission("bar.reservation.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.cancelReservation(id, request));
    }

    @PostMapping("/deposit-rules")
    public ApiResponse<BarDepositRuleRecord> createDepositRule(
            @RequestBody(required = false) @jakarta.validation.Valid BarDepositRuleRequest request
    ) {
        authorizationService.requirePermission("bar.deposit_rule.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createDepositRule(request));
    }

    @PostMapping("/reservations/{id}/invitation")
    public ApiResponse<BarReservationInvitationRecord> issueReservationInvitation(
            @PathVariable String id,
            @RequestBody(required = false) @jakarta.validation.Valid BarInvitationActionRequest request
    ) {
        authorizationService.requirePermission("bar.reservation.invitation.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.issueReservationInvitation(id, request));
    }

    @GetMapping("/reservations/{id}/invitation")
    public ApiResponse<BarReservationInvitationRecord> getReservationInvitation(@PathVariable String id) {
        authorizationService.requirePermission("bar.reservation.view");
        return ApiResponse.ok(service.getReservationInvitation(id));
    }

    @PostMapping("/reservations/{id}/invitation/revoke")
    public ApiResponse<BarReservationInvitationRecord> revokeReservationInvitation(
            @PathVariable String id,
            @RequestBody(required = false) @jakarta.validation.Valid BarInvitationActionRequest request
    ) {
        authorizationService.requirePermission("bar.reservation.invitation.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.revokeReservationInvitation(id, request));
    }

    @PostMapping("/reservations/{id}/invitation/entry-verify")
    public ApiResponse<BarReservationInvitationMemberRecord> verifyReservationInvitationEntry(
            @PathVariable String id,
            @RequestBody(required = false) @jakarta.validation.Valid VerificationRequest request
    ) {
        authorizationService.requirePermission("bar.table.guest_checkin");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.verifyReservationInvitationEntry(id, request));
    }

    @GetMapping("/tables/availability")
    public ApiResponse<PageResponse<BarTableAvailabilityRecord>> listTableAvailability() {
        return ApiResponse.ok(service.listTableAvailability());
    }

    @GetMapping("/tables")
    public ApiResponse<PageResponse<BarTableRecord>> listTables() {
        authorizationService.requirePermission("bar.table.view");
        return ApiResponse.ok(service.listTables());
    }

    @PostMapping("/tables")
    public ApiResponse<BarTableRecord> createTable(@RequestBody(required = false) @jakarta.validation.Valid BarTableRequest request) {
        authorizationService.requirePermission("bar.table.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createTable(request));
    }

    @GetMapping("/tables/without-space")
    public ApiResponse<PageResponse<BarTableSpaceBacklogRecord>> listTablesWithoutSpace() {
        authorizationService.requirePermission("bar.table.manage");
        return ApiResponse.ok(service.listTablesWithoutSpace());
    }

    @PutMapping("/tables/{table_id}/space")
    public ApiResponse<BarTableSpaceAssignmentRecord> assignTableSpace(
            @PathVariable("table_id") String tableId,
            @RequestBody(required = false) @jakarta.validation.Valid BarTableSpaceAssignmentRequest request
    ) {
        authorizationService.requirePermission("bar.table.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.assignTableSpace(tableId, request));
    }

    @PostMapping("/tables/{table_id}/open")
    public ApiResponse<BarTableRecord> openTable(
            @PathVariable("table_id") String tableId,
            @RequestBody(required = false) @jakarta.validation.Valid OpenBarTableRequest request
    ) {
        authorizationService.requirePermission("bar.table.open_close");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.openTable(tableId, request));
    }

    /** 第十批 TK-27-R2：清台登记恢复空闲（BSM L1058）。 */
    @PostMapping("/tables/{table_id}/clear")
    public ApiResponse<BarTableRecord> clearTable(
            @PathVariable("table_id") String tableId,
            @RequestBody(required = false) @jakarta.validation.Valid BarTableClearRequest request
    ) {
        authorizationService.requirePermission("bar.table.open_close");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.clearTable(tableId, request));
    }

    /** 第十八批 AA 段第 206 条 206-R1：台位停用（ENUM:100、BSM:1058「台位停用与恢复由店长及以上操作，填原因留痕」）；只停空闲台位，原因必填。 */
    @PostMapping("/tables/{table_id}/disable")
    public ApiResponse<BarTableRecord> disableTable(
            @PathVariable("table_id") String tableId,
            @RequestBody(required = false) @jakarta.validation.Valid BarTableStatusChangeRequest request
    ) {
        authorizationService.requirePermission("bar.table.disable");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.disableTable(tableId, request));
    }

    /** 第十八批 AA 段第 206 条 206-R1：停用台位恢复空闲（同上），原因必填。 */
    @PostMapping("/tables/{table_id}/enable")
    public ApiResponse<BarTableRecord> enableTable(
            @PathVariable("table_id") String tableId,
            @RequestBody(required = false) @jakarta.validation.Valid BarTableStatusChangeRequest request
    ) {
        authorizationService.requirePermission("bar.table.disable");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.enableTable(tableId, request));
    }

    @GetMapping("/table-sessions")
    public ApiResponse<PageResponse<BarTableSessionRecord>> listTableSessions() {
        authorizationService.requirePermission("bar.table_session.view");
        return ApiResponse.ok(service.listTableSessions());
    }

    @PostMapping("/table-sessions")
    public ApiResponse<BarTableSessionRecord> createTableSession(@RequestBody(required = false) @jakarta.validation.Valid BarTableSessionRequest request) {
        authorizationService.requirePermission("bar.table_session.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createTableSession(request));
    }

    @PostMapping("/table-sessions/{session_id}/transfer")
    public ApiResponse<BarTableSessionAdjustmentRecord> transferTableSession(
            @PathVariable("session_id") String sessionId,
            @RequestBody(required = false) @jakarta.validation.Valid BarTableTransferRequest request
    ) {
        authorizationService.requirePermission("bar.table_session.transfer");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.transferTableSession(sessionId, request));
    }

    @PostMapping("/table-sessions/{session_id}/merge")
    public ApiResponse<BarTableSessionAdjustmentRecord> mergeTableSessions(
            @PathVariable("session_id") String sessionId,
            @RequestBody(required = false) @jakarta.validation.Valid BarTableMergeRequest request
    ) {
        authorizationService.requirePermission("bar.table_session.merge");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.mergeTableSessions(sessionId, request));
    }

    /** 第十批 TK-27-R3：拆台（BSM L1079）。 */
    @PostMapping("/table-sessions/{session_id}/split")
    public ApiResponse<BarTableSessionAdjustmentRecord> splitTableSessions(
            @PathVariable("session_id") String sessionId,
            @RequestBody(required = false) @jakarta.validation.Valid BarTableSplitRequest request
    ) {
        authorizationService.requirePermission("bar.table_session.split");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.splitTableSessions(sessionId, request));
    }

    /** 第十八批 K 段 95-R2（BSM 第 1021 行「来源与销售人员的事后改派须店长复核并留痕，只能在该账单结清前办理」）：店长持权限改派。 */
    @PostMapping("/table-sessions/{session_id}/attribution-reassign")
    public ApiResponse<BarTableSessionRecord> reassignTableSessionAttribution(
            @PathVariable("session_id") String sessionId,
            @RequestBody(required = false) BarTableSessionAttributionReassignRequest request
    ) {
        authorizationService.requirePermission("bar.table_session.attribution_reassign");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.reassignTableSessionAttribution(sessionId, request));
    }
}
