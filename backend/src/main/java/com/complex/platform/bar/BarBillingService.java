package com.complex.platform.bar;

import static com.complex.platform.bar.BarBillRecords.*;
import static com.complex.platform.bar.BarValidation.*;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.PaymentChannelPolicy;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.finance.RefundAuthorizationService;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class BarBillingService {

    private static final String CURRENCY_CNY = "CNY";
    private final BarRepository repository;
    private final BarReceivableLedgerService receivableLedgerService;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;
    // Stage B（审计 A2，适用所有退款）：退款分级授权引擎 + 功能标志（默认关）。字段注入以免改构造点；
    // 可为 null（旧构造/单测未注入）；包私有便于单测置位。开标志后超额酒吧退款须复核（见 requireRefundTierAuthorized）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    RefundAuthorizationService refundAuthorizationService;
    // AUD-061 结账-储值支付：结账渠道选「储值支付」时扣发卡商户的储值卡余额（复用 StoredValueService.consumeInternal）。
    // 字段注入（required=false）以免改构造点/单测；bar 依赖 customer 无构造环（StoredValueService 不依赖 bar bean）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.customer.StoredValueService storedValueService;

    public BarBillingService(
            BarRepository repository,
            BarReceivableLedgerService receivableLedgerService,
            AuthorizationService authorizationService,
            AuditEventService auditEventService
    ) {
        this.repository = repository;
        this.receivableLedgerService = receivableLedgerService;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
    }

    /**
     * 分级授权闸（适用所有退款）：退款额按 {@link RefundAuthorizationService#classify} 判档——额度内/规则既定放行，
     * 超额（REVIEW_REQUIRED）因单步退款无独立复核状态机而直接拒（REFUND_REVIEW_REQUIRED），须由额度内的授权人办理。
     *
     * <p>AUD-007 fail-closed：不再受功能标志控制、不再对平台管理员隐式旁路——闸常在；引擎缺失
     * （仅旧兼容/单测构造，生产恒注入）时拒绝而非放行。如需给管理员更高额度，走同一 (subject, domain, project) 显式额度配置 + 审计。
     * <p>AUD-008：projectId 为权威综合体归属，由调用方从原账单/操作者 scope 推导传入，不信任请求体。
     */
    private void requireRefundTierAuthorized(String domain, long amountCent, String projectId) {
        if (refundAuthorizationService == null) {
            throw ApiException.forbidden("REFUND_AUTHORIZATION_UNAVAILABLE",
                    "refund tiered authorization engine is unavailable; over-limit refunds cannot be self-processed");
        }
        var actor = authorizationService.currentActor();
        if (refundAuthorizationService.classify(domain, amountCent, actor.id(), null, projectId)
                == RefundAuthorizationService.RefundTier.REVIEW_REQUIRED) {
            throw ApiException.forbidden("REFUND_REVIEW_REQUIRED",
                    "refund amount exceeds the self-service authorization limit; an authorized approver must process it");
        }
    }

    BarBillRecord createBill(String orderId, BarOrderBillCreateRequest request) {
        requireRequest(request);
        BarOrderRecord order = repository.findOrderById(orderId)
                .orElseThrow(() -> ApiException.notFound("BAR_ORDER_NOT_FOUND", "bar order not found"));
        checkMerchantOwnership(order.merchantId());
        long subtotal = repository.listOrderItems(order.id()).stream()
                .mapToLong(BarOrderItemRecord::amountCent)
                .sum();
        BarReservationRecord reservation = order.reservationId() == null
                ? null
                : repository.findReservationById(order.reservationId()).orElse(null);
        long depositDeduct = reservation == null ? 0L : Math.min(reservation.depositAmountCent(), subtotal);
        long payable = subtotal - depositDeduct;
        Instant now = Instant.now();
        String salesRoleType = trimToNull(order.salesStaffId()) == null ? "non_sales" : "sales";
        return repository.saveBill(new BarBillRecord(
                "bill_" + billSlug(order.id() + ":" + now.toEpochMilli()),
                "BILL" + now.toEpochMilli(),
                order.id(),
                order.barId(),
                order.merchantId(),
                order.tableId(),
                order.reservationId(),
                subtotal,
                0L,
                depositDeduct,
                payable,
                0L,
                CURRENCY_CNY,
                "unpaid",
                "unsettled",
                order.salesStaffId(),
                salesRoleType,
                now,
                now
        ));
    }

    PaymentIntentRecord createBillPayIntent(String billId, PaymentIntentRequest request) {
        PaymentIntentRequest actual = requireRequest(request);
        BarBillRecord bill = repository.findBillById(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "bar bill not found"));
        checkMerchantOwnership(bill.merchantId());
        String businessOrderType = requireText(actual.businessOrderType(), "business_order_type");
        String businessOrderId = requireText(actual.businessOrderId(), "business_order_id");
        if (!"bar_bill".equals(businessOrderType) || !bill.id().equals(businessOrderId)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "payment intent business order must match bar bill");
        }
        long amountCent = positive(actual.amountCent(), "amount_cent");
        if (amountCent != bill.payableAmountCent()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "payment intent amount must match bill payable amount");
        }
        Instant now = Instant.now();
        return repository.savePaymentIntent(new PaymentIntentRecord(
                "pay_intent_" + billSlug(actual.idempotencyKey()),
                businessOrderType,
                businessOrderId,
                amountCent,
                trimToDefault(actual.currency(), CURRENCY_CNY),
                PaymentChannelPolicy.requirePaymentIntentChannel(actual.channel()),
                "created",
                now,
                now
        ));
    }

    BarBillRecord settleBill(String billId, BarBillSettleRequest request) {
        BarBillSettleRequest actual = requireRequest(request);
        // 2c-2：行锁读取账单，序列化并发结算（配合 settled 守卫防 TOCTOU 重复结算）。
        BarBillRecord bill = repository.findBillByIdForUpdate(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "bar bill not found"));
        checkMerchantOwnership(bill.merchantId());
        if ("settled".equals(bill.settlementStatus())) {
            throw ApiException.conflict("BAR_BILL_ALREADY_SETTLED", "bar bill is already settled");
        }
        // 2c-1 低消硬阻断（用户裁定 + PRD §617/716）：未满足最低消费不可结账。
        // 低消基数 = 实际消费 subtotal + 已补差 min_spend_top_up（运营补足低消的手段），不含 tip/deposit/service_fee。
        if (bill.tableId() != null) {
            BarTableRecord table = repository.findTableById(bill.tableId()).orElse(null);
            if (table != null && table.minConsumptionCent() > 0) {
                long topUp = repository.listBillAdjustments(bill.id()).stream()
                        .filter(a -> "min_spend_top_up".equals(a.adjustmentType()))
                        .mapToLong(BarBillAdjustmentRecord::amountCent)
                        .sum();
                if (bill.subtotalAmountCent() + topUp < table.minConsumptionCent()) {
                    throw ApiException.conflict("BAR_MIN_CONSUMPTION_NOT_MET",
                            "table minimum consumption not met; add consumption or apply min_spend_top_up before settle");
                }
            }
        }
        requireText(actual.paymentId(), "payment_id");
        // V091：结算应收含小费（payable + tip）。小费为代收（归属服务员/交班结算），不计入低消/提成基数。
        long settleTotal = bill.payableAmountCent() + bill.tipAmountCent();
        // AUD-009 续付累计：本次金额按「剩余未付」校验并累计入 paidAmount，不再按原始总额校验/覆盖——
        // 否则部分付款后二次付全额会超收（流水累计>应收但账单标 settled），或续付覆盖掉首付金额致账实不符。
        long existingPaid = bill.paidAmountCent();
        long remaining = settleTotal - existingPaid;
        // 复核修正（P2）：全免单/押金全抵/减免后剩余应收≤0 的账单必须能关——此路径「结账=关单」，
        // 不产生收款流水（0 元无收款），否则免单账单永远停在未结。
        if (remaining <= 0) {
            if (actual.settlementAmountCent() != null && actual.settlementAmountCent() != 0) {
                throw ApiException.badRequest("VALIDATION_FAILED",
                        "settlement amount cannot exceed remaining unpaid amount");
            }
            Instant closeNow = Instant.now();
            BarBillRecord closedBill = repository.saveBill(billWithAmounts(
                    bill,
                    bill.subtotalAmountCent(),
                    bill.discountAmountCent(),
                    bill.depositDeductAmountCent(),
                    bill.payableAmountCent(),
                    existingPaid,
                    "paid",
                    "settled",
                    closeNow
            ));
            if (bill.orderId() != null) {
                repository.findOrderById(bill.orderId())
                        .filter(o -> o.paidAt() == null)
                        .ifPresent(o -> repository.saveOrder(new BarOrderRecord(
                                o.id(), o.barId(), o.merchantId(), o.tableId(), o.reservationId(), o.customerId(),
                                o.orderSource(), o.status(), o.totalAmountCent(), o.currency(), o.createdTerminal(),
                                o.deviceId(), o.salesStaffId(), o.createdAt(), o.updatedAt(), o.orderType(), closeNow)));
            }
            return closedBill;
        }
        long settlementAmount = actual.settlementAmountCent() == null
                ? remaining
                : nonNegative(actual.settlementAmountCent(), "settlement_amount_cent");
        if (settlementAmount <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "settlement amount must be positive");
        }
        if (settlementAmount > remaining) {
            throw ApiException.badRequest("VALIDATION_FAILED", "settlement amount cannot exceed remaining unpaid amount");
        }
        Instant now = Instant.now();
        // A2/A3：结算渠道（统一词表，与 finance 对账归并）+ 现金/POS 收款留痕 + D4 权威事件。
        String channel = PaymentChannelPolicy.requireSettlementChannel(actual.paymentChannel(), "aggregate_pay");
        // AUD-061 储值支付：扣该客人在本发卡商户的储值卡余额（复用 StoredValueService.consumeInternal；
        // platform_collect 卡消费即逐笔挂应付商户，merchant_prepaid 卡无跨主体分录）。余额不足则整单结账失败（同事务回滚）。
        if ("stored_value".equals(channel)) {
            if (storedValueService == null) {
                throw ApiException.conflict("STORED_VALUE_UNAVAILABLE", "stored-value payment is not available");
            }
            String svCustomerId = resolveBillCustomerId(bill);
            if (svCustomerId == null) {
                throw ApiException.badRequest("VALIDATION_FAILED",
                        "储值支付需先绑定客人（账单未关联客人，无法扣储值卡）");
            }
            storedValueService.consumeInternal(svCustomerId, null, bill.merchantId(), settlementAmount,
                    "酒吧结账-储值支付 账单" + bill.id(), "barsv_" + slug(actual.idempotencyKey()));
        }
        repository.saveBillSettlement(new BarBillSettlementRecord(
                "settle_" + slug(actual.idempotencyKey()),
                bill.id(),
                channel,
                trimToNull(actual.cashierStaffId()),
                trimToNull(actual.posTransactionNo()),
                settlementAmount,
                bill.merchantId(),
                now
        ));
        if ("cash".equals(channel)) {
            auditEventService.recordSuccess("payment.cash_received", "bar.bill.settle", "bar_bill",
                    bill.id(), null, actual.idempotencyKey(),
                    Map.of("amount_cent", settlementAmount, "cashier_staff_id",
                            String.valueOf(actual.cashierStaffId())));
        } else if ("pos".equals(channel)) {
            auditEventService.recordSuccess("payment.pos_received", "bar.bill.settle", "bar_bill",
                    bill.id(), null, actual.idempotencyKey(),
                    Map.of("amount_cent", settlementAmount, "pos_transaction_no",
                            String.valueOf(actual.posTransactionNo())));
        }
        // AUD-009：paidAmount=历史已付+本次；状态按累计后金额判定。
        long newPaid = existingPaid + settlementAmount;
        BarBillRecord settledBill = repository.saveBill(billWithAmounts(
                bill,
                bill.subtotalAmountCent(),
                bill.discountAmountCent(),
                bill.depositDeductAmountCent(),
                bill.payableAmountCent(),
                newPaid,
                newPaid >= settleTotal ? "paid" : "partial_paid",
                newPaid >= settleTotal ? "settled" : "partial_settled",
                now
        ));
        // V231：酒吧订单本身无支付生命周期，支付走账单结算；结账成功即回填链上订单 paid_at（首次为空才写，与账单同事务）。
        if (bill.orderId() != null) {
            repository.findOrderById(bill.orderId())
                    .filter(o -> o.paidAt() == null)
                    .ifPresent(o -> repository.saveOrder(new BarOrderRecord(
                            o.id(), o.barId(), o.merchantId(), o.tableId(), o.reservationId(), o.customerId(),
                            o.orderSource(), o.status(), o.totalAmountCent(), o.currency(), o.createdTerminal(),
                            o.deviceId(), o.salesStaffId(), o.createdAt(), o.updatedAt(), o.orderType(), now)));
        }
        return settledBill;
    }

    /** 结账-储值支付：账单本身不带 customerId，经链上订单解析下单客人。 */
    private String resolveBillCustomerId(BarBillRecord bill) {
        if (bill.orderId() == null) {
            return null;
        }
        return repository.findOrderById(bill.orderId()).map(BarOrderRecord::customerId).orElse(null);
    }

    BarBillAdjustmentRecord applyBillAdjustment(String billId, BarBillAdjustmentRequest request) {
        BarBillAdjustmentRequest actual = requireRequest(request);
        BarBillRecord bill = repository.findBillById(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "bar bill not found"));
        checkMerchantOwnership(bill.merchantId());
        String type = requireText(actual.adjustmentType(), "adjustment_type");
        boolean settled = "settled".equals(bill.settlementStatus());
        // refund（退款）是结账后操作，必须在已结算账单上执行；其余调整（加项/免单/挂账）只能在未结算前执行。
        if ("refund".equals(type)) {
            if (!settled) {
                throw ApiException.conflict("BAR_BILL_NOT_REFUNDABLE", "only a settled bar bill can be refunded");
            }
        } else if (settled) {
            throw ApiException.conflict("BAR_BILL_NOT_ADJUSTABLE", "settled bar bill cannot be adjusted");
        }
        String reason = requireText(actual.reason(), "reason");
        String requestedBy = authorizationService.currentActor().id();
        long beforePayable = bill.payableAmountCent();
        long amount = nonNegative(actual.amountCent(), "amount_cent");
        long afterPayable;
        String arApItemId = null;
        String paymentStatus = bill.paymentStatus();
        String settlementStatus = bill.settlementStatus();
        Instant refundedAt = null;
        if ("service_fee".equals(type) || "min_spend_top_up".equals(type)) {
            amount = positive(actual.amountCent(), "amount_cent");
            afterPayable = beforePayable + amount;
        } else if ("waiver".equals(type)) {
            // 免单/减免：结账前减应付。后续结算金额本就更低 → 不作为独立 net 冲减（已体现在更低实收）。
            amount = positive(actual.amountCent(), "amount_cent");
            if (amount > beforePayable) {
                throw ApiException.badRequest("VALIDATION_FAILED", "waiver amount cannot exceed payable amount");
            }
            afterPayable = beforePayable - amount;
        } else if ("refund".equals(type)) {
            // 退款：结账后退钱，需独立高危权限（区别于结账前 bar.bill.adjust）。
            authorizationService.requirePermission("bar.bill.refund");
            // 记退款时点，聚合按 refunded_at 落经营日窗口冲减当日 net。
            amount = positive(actual.amountCent(), "amount_cent");
            if (amount > beforePayable) {
                throw ApiException.badRequest("VALIDATION_FAILED", "refund amount cannot exceed payable amount");
            }
            // AUD-007 分级授权 fail-closed 常在：超额度退款须复核——单步退款无独立复核状态机，超额/无额度直接拒，由额度内授权人办理。
            // AUD-008 权威综合体：酒吧账单不自带 project，取操作者授权 scope（非请求体）；平台/无组织 → null=仅全局配置。
            requireRefundTierAuthorized("bar", amount,
                    authorizationService.currentProjectScopeQuietly().orElse(null));
            afterPayable = beforePayable - amount;
            refundedAt = Instant.now();
            // RC1①：退款过账到 finance ar_ap 台账（direction=payable, source_type=refund），表示"应退/已退"。
            // 同事务执行：过账失败抛出 → 回滚整个退款（账单减项 + 调整记录），保证财务台账强一致。
            BarOrderRecord refundOrder = repository.findOrderById(bill.orderId())
                    .orElseThrow(() -> ApiException.notFound("BAR_ORDER_NOT_FOUND", "bar order not found"));
            String refundAdjustmentId =
                    "bill_adjustment_" + billSlug(bill.id() + ":" + type + ":" + actual.idempotencyKey());
            arApItemId = receivableLedgerService.createRefundPayable(
                    bill,
                    refundOrder,
                    refundAdjustmentId,
                    amount,
                    requireText(actual.idempotencyKey(), "idempotency_key"),
                    reason,
                    safeList(actual.evidenceAttachmentIds())
            );
        } else if ("on_account".equals(type)) {
            if (amount != 0L) {
                throw ApiException.badRequest("VALIDATION_FAILED", "on_account amount_cent must be zero");
            }
            afterPayable = beforePayable;
            paymentStatus = "on_account";
            settlementStatus = "receivable";
            if ("on_account".equals(bill.paymentStatus()) || "receivable".equals(bill.settlementStatus())) {
                throw ApiException.conflict("BAR_BILL_ALREADY_ON_ACCOUNT", "bar bill is already on account");
            }
            BarOrderRecord order = repository.findOrderById(bill.orderId())
                    .orElseThrow(() -> ApiException.notFound("BAR_ORDER_NOT_FOUND", "bar order not found"));
            arApItemId = receivableLedgerService.createReceivableForOnAccountBill(
                    bill,
                    order,
                    requireText(actual.idempotencyKey(), "idempotency_key"),
                    reason,
                    safeList(actual.evidenceAttachmentIds())
            );
        } else {
            throw ApiException.badRequest("VALIDATION_FAILED", "unsupported bar bill adjustment type");
        }
        Instant now = Instant.now();
        repository.saveBill(billWithAmounts(
                bill,
                bill.subtotalAmountCent(),
                bill.discountAmountCent(),
                bill.depositDeductAmountCent(),
                afterPayable,
                bill.paidAmountCent(),
                paymentStatus,
                settlementStatus,
                now
        ));
        return repository.saveBillAdjustment(new BarBillAdjustmentRecord(
                "bill_adjustment_" + billSlug(bill.id() + ":" + type + ":" + actual.idempotencyKey()),
                bill.id(),
                type,
                amount,
                beforePayable,
                afterPayable,
                reason,
                requestedBy,
                true,
                "applied",
                requireText(actual.idempotencyKey(), "idempotency_key"),
                safeList(actual.evidenceAttachmentIds()),
                arApItemId,
                billAdjustmentSnapshot(bill, afterPayable, paymentStatus, settlementStatus, arApItemId),
                now,
                refundedAt
        ));
    }

    BarBillOperationRecord splitBill(String billId, BarBillSplitRequest request) {
        BarBillSplitRequest actual = requireRequest(request);
        BarBillRecord source = repository.findBillById(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "bar bill not found"));
        checkMerchantOwnership(source.merchantId());
        requireBillMovable(source, "BAR_BILL_NOT_SPLITTABLE");
        long amount = positive(actual.splitAmountCent(), "split_amount_cent");
        if (amount >= source.payableAmountCent()) {
            throw ApiException.conflict("BAR_BILL_SPLIT_AMOUNT_INVALID", "split amount must be less than payable amount");
        }
        String reason = requireText(actual.reason(), "reason");
        String requestedBy = authorizationService.currentActor().id();
        String key = requireText(actual.idempotencyKey(), "idempotency_key");
        Instant now = Instant.now();
        String generatedBillId = "bill_split_" + billSlug(source.id() + ":" + key);
        BarBillRecord updatedSource = billWithAmounts(
                source,
                source.subtotalAmountCent() - amount,
                source.discountAmountCent(),
                source.depositDeductAmountCent(),
                source.payableAmountCent() - amount,
                source.paidAmountCent(),
                source.paymentStatus(),
                source.settlementStatus(),
                now
        );
        BarBillRecord generated = new BarBillRecord(
                generatedBillId,
                splitBillNo(source.billNo(), key),
                source.orderId(),
                source.barId(),
                source.merchantId(),
                source.tableId(),
                source.reservationId(),
                amount,
                0L,
                0L,
                amount,
                0L,
                source.currency(),
                "unpaid",
                "unsettled",
                source.salesStaffId(),
                source.salesRoleType(),
                now,
                now
        );
        repository.saveBill(updatedSource);
        repository.saveBill(generated);
        return repository.saveBillOperation(new BarBillOperationRecord(
                "bill_operation_" + billSlug(source.id() + ":split:" + key),
                "split",
                source.id(),
                generated.id(),
                generated.id(),
                amount,
                reason,
                requestedBy,
                "applied",
                key,
                safeList(actual.evidenceAttachmentIds()),
                billOperationSnapshot(source, updatedSource, generated),
                now
        ));
    }

    BarBillOperationRecord mergeBills(String billId, BarBillMergeRequest request) {
        BarBillMergeRequest actual = requireRequest(request);
        String secondaryBillId = requireText(actual.mergedBillId(), "merged_bill_id");
        if (billId.equals(secondaryBillId)) {
            throw ApiException.conflict("BAR_BILL_MERGE_SAME_BILL", "cannot merge bill into itself");
        }
        BarBillRecord primary = repository.findBillById(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "primary bill not found"));
        checkMerchantOwnership(primary.merchantId());
        BarBillRecord secondary = repository.findBillById(secondaryBillId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "secondary bill not found"));
        checkMerchantOwnership(secondary.merchantId());
        requireBillMovable(primary, "BAR_BILL_NOT_MERGEABLE");
        requireBillMovable(secondary, "BAR_BILL_NOT_MERGEABLE");
        if (!primary.barId().equals(secondary.barId())) {
            throw ApiException.conflict("BAR_BILL_MERGE_BAR_MISMATCH", "bills must belong to the same bar");
        }
        String reason = requireText(actual.reason(), "reason");
        String requestedBy = authorizationService.currentActor().id();
        String key = requireText(actual.idempotencyKey(), "idempotency_key");
        long amount = secondary.payableAmountCent();
        Instant now = Instant.now();
        BarBillRecord updatedPrimary = billWithAmounts(
                primary,
                primary.subtotalAmountCent() + secondary.subtotalAmountCent(),
                primary.discountAmountCent() + secondary.discountAmountCent(),
                primary.depositDeductAmountCent() + secondary.depositDeductAmountCent(),
                primary.payableAmountCent() + secondary.payableAmountCent(),
                primary.paidAmountCent(),
                primary.paymentStatus(),
                primary.settlementStatus(),
                now
        );
        BarBillRecord mergedSecondary = billWithAmounts(secondary, 0L, 0L, 0L, 0L, 0L, "merged", "merged", now);
        repository.saveBill(updatedPrimary);
        repository.saveBill(mergedSecondary);
        return repository.saveBillOperation(new BarBillOperationRecord(
                "bill_operation_" + billSlug(primary.id() + ":merge:" + secondary.id() + ":" + key),
                "merge",
                primary.id(),
                secondary.id(),
                null,
                amount,
                reason,
                requestedBy,
                "applied",
                key,
                safeList(actual.evidenceAttachmentIds()),
                billOperationSnapshot(primary, updatedPrimary, secondary),
                now
        ));
    }

    BarCommissionCalculationRecord calculateCommission(String billId, CommissionCalculateRequest request) {
        CommissionCalculateRequest actual = requireRequest(request);
        String bodyBillId = trimToNull(actual.billId());
        if (bodyBillId != null && !billId.equals(bodyBillId)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "bill_id must match path");
        }
        // 2c-2：行锁读取账单，序列化并发计提。
        BarBillRecord bill = repository.findBillByIdForUpdate(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "bar bill not found"));
        checkMerchantOwnership(bill.merchantId());
        // 2c-2 提成防重：同一账单已计提则返回已有（幂等），不重复建提成台账与 finance 应付，杜绝多幂等键刷提成。
        java.util.Optional<BarCommissionCalculationRecord> existingCommission = repository.findCommissionByBillId(bill.id());
        if (existingCommission.isPresent()) {
            return existingCommission.get();
        }
        String roleType = trimToNull(bill.salesStaffId()) == null ? "non_sales" : "sales";
        int rateBp = "sales".equals(roleType) ? 1500 : 1000;
        // 裁决#4 / 开发文档 §6.9A.2：提成基数 = 可计提消费额 − 排除项合计。
        // 排除项中"免单(waiver)/退款(refund)"已落 bar_bill_adjustments（V079），按净额扣除：
        // waiver+refund 为减项，其 V124 反向冲销 waiver_reversal/refund_reversal（退菜退酒被拒时撤销原减免）回加，
        // 避免免单/退款被撤销后仍少发提成。仅统计 applied 态记录（applyBillAdjustment/void 流程均落 applied）。
        // 注意：抹零(round_off)经业主确认无此业务；通票免费酒水/票务权益赠饮在核销环节即为 0 元平台赠饮单、
        // 不进 subtotal，故无需在此扣除（见 §7.2 赠饮核销闭环）。赠菜成本(gift)见下方 giftDeduction（F-TXN-05）。
        long commissionExcluded = repository.listBillAdjustments(bill.id()).stream()
                .filter(a -> "applied".equals(a.status()))
                .mapToLong(a -> switch (a.adjustmentType()) {
                    case "waiver", "refund" -> a.amountCent();
                    case "waiver_reversal", "refund_reversal" -> -a.amountCent();
                    default -> 0L;
                })
                .sum();
        // F-TXN-05 赠菜扣提成：吧台随单赠送项(is_gift)对客 amount_cent=0 已不计入 subtotal，其成本再按
        // 商品内部结算价(V152 internal_settlement_price_cent)× 数量从提成基数扣除——即赠送项成本不计提成。
        // 商品未录内部价时该项不扣（orElse 0，向后兼容既有商品）。
        long giftDeduction = repository.listOrderItems(bill.orderId()).stream()
                .filter(BarOrderItemRecord::isGift)
                .mapToLong(it -> repository.findProductById(it.productId())
                        .map(BarProductRecord::internalSettlementPriceCent)
                        .map(price -> price * it.quantity())
                        .orElse(0L))
                .sum();
        long base = Math.max(0L, bill.subtotalAmountCent() - bill.discountAmountCent() - commissionExcluded - giftDeduction);
        long amount = base * rateBp / 10_000L;
        BarCommissionCalculationRecord saved = repository.saveCommissionCalculation(new BarCommissionCalculationRecord(
                "commission_" + billSlug(actual.idempotencyKey()),
                bill.id(),
                bill.salesStaffId(),
                roleType,
                base,
                rateBp,
                amount,
                "calculated",
                Instant.now()
        ));
        // A5(REQ-26b 增强)：有归属销售人员且金额>0 时贯通 finance 应付台账（非销售/0 元仅保留计算台账，满足 D13"或"）。
        if (saved.commissionAmountCent() > 0 && saved.salesStaffId() != null) {
            receivableLedgerService.createPayableForCommission(saved);
        }
        return saved;
    }

    // ========================================================================
    // V091 反结：settled→reopened→重结的状态机。需高危权限 + 原因 + 审批；与 T7 日结锁定协同。
    // ========================================================================
    BarBillOperationRecord reopenBill(String billId, BarBillReopenRequest request) {
        BarBillReopenRequest actual = requireRequest(request);
        BarBillRecord bill = repository.findBillById(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "bar bill not found"));
        checkMerchantOwnership(bill.merchantId());
        if (!"settled".equals(bill.settlementStatus())) {
            throw ApiException.conflict("BAR_BILL_NOT_REOPENABLE", "only a settled bar bill can be reopened");
        }
        // 与 T7 日结锁定协同：若账单结算经营日已纳入锁定日结，反结改影响金额须走补录审批 → 硬拦截 DAY_CLOSE_LOCKED。
        BarBillSettlementRecord latestSettlement = repository.listBillSettlements(bill.id()).stream()
                .reduce((first, second) -> second)
                .orElse(null);
        LocalDate settleDay = latestSettlement != null && latestSettlement.settledAt() != null
                ? latestSettlement.settledAt().atZone(ZoneOffset.UTC).toLocalDate()
                : LocalDate.now();
        if (receivableLedgerService.isBusinessDayLocked(settleDay)) {
            throw ApiException.conflict("DAY_CLOSE_LOCKED",
                    "settlement business day " + settleDay + " is locked; use the backfill adjustment workflow");
        }
        String reason = requireText(actual.reason(), "reason");
        String requestedBy = authorizationService.currentActor().id();
        String key = requireText(actual.idempotencyKey(), "idempotency_key");
        Instant now = Instant.now();
        BarBillRecord reopened = BarBillRecords.billReopened(bill, requestedBy, reason, now);
        repository.saveBill(reopened);
        // 2c-3 reopen 冲正：把原结算流水标 reversed，使 finance 对账聚合 + analytics 营收聚合排除，
        // 避免反结→重结后原结算与重结算同窗口双计入对账/net。
        repository.markBillSettlementsReversed(bill.id());
        auditEventService.recordSuccess("bar.bill_reopened", "bar.bill.reopen", "bar_bill",
                bill.id(), null, key,
                Map.of("reopen_count", reopened.reopenCount(), "payable_amount_cent", reopened.payableAmountCent()));
        return repository.saveBillOperation(new BarBillOperationRecord(
                "bill_operation_" + billSlug(bill.id() + ":reopen:" + key),
                "reopen",
                bill.id(),
                null,
                null,
                bill.payableAmountCent(),
                reason,
                requestedBy,
                "applied",
                key,
                safeList(actual.evidenceAttachmentIds()),
                billOperationSnapshot(bill, reopened, bill),
                now));
    }

    // ========================================================================
    // V091 小费：向账单追加小费（额/比例之一），计入 payable 与结算（代收，不计低消/提成基数）。
    // ========================================================================
    BarBillRecord addTip(String billId, BarBillTipRequest request) {
        BarBillTipRequest actual = requireRequest(request);
        BarBillRecord bill = repository.findBillById(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "bar bill not found"));
        checkMerchantOwnership(bill.merchantId());
        if ("settled".equals(bill.settlementStatus()) || "merged".equals(bill.settlementStatus())) {
            throw ApiException.conflict("BAR_BILL_NOT_TIPPABLE", "settled or merged bar bill cannot accept a tip");
        }
        long tip;
        if (actual.tipAmountCent() != null) {
            tip = positive(actual.tipAmountCent(), "tip_amount_cent");
        } else if (actual.tipRatioBps() != null) {
            int ratioBps = actual.tipRatioBps();
            if (ratioBps <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "tip_ratio_bps must be positive");
            }
            tip = bill.payableAmountCent() * ratioBps / 10_000L;
            if (tip <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "computed tip must be positive");
            }
        } else {
            throw ApiException.badRequest("VALIDATION_FAILED", "tip_amount_cent or tip_ratio_bps is required");
        }
        Instant now = Instant.now();
        long newTip = bill.tipAmountCent() + tip;
        BarBillRecord updated = new BarBillRecord(
                bill.id(), bill.billNo(), bill.orderId(), bill.barId(), bill.merchantId(),
                bill.tableId(), bill.reservationId(), bill.subtotalAmountCent(), bill.discountAmountCent(),
                bill.depositDeductAmountCent(), bill.payableAmountCent(), bill.paidAmountCent(),
                bill.currency(), bill.paymentStatus(), bill.settlementStatus(), bill.salesStaffId(),
                bill.salesRoleType(), bill.createdAt(), now,
                bill.reopened(), bill.reopenedByUserId(), bill.reopenedAt(), bill.reopenReason(),
                bill.reopenCount(), newTip);
        BarBillRecord saved = repository.saveBill(updated);
        auditEventService.recordSuccess("bar.bill_tip_added", "bar.bill.tip", "bar_bill",
                bill.id(), null, actual.idempotencyKey(),
                Map.of("tip_amount_cent", tip, "tip_total_cent", newTip));
        return saved;
    }

    private void checkMerchantOwnership(String resourceMerchantId) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(resourceMerchantId)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "access to another merchant's resource is not allowed");
        }
    }

}
