package com.complex.platform.bar;

import static com.complex.platform.bar.BarBillRecords.*;
import static com.complex.platform.bar.BarValidation.*;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class BarBillingService {

    private static final String CURRENCY_CNY = "CNY";
    private static final Set<String> SETTLE_CHANNELS =
            Set.of("aggregate_pay", "cash", "pos", "bank_transfer", "other");

    private final BarRepository repository;
    private final BarReceivableLedgerService receivableLedgerService;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;

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
                requireText(actual.channel(), "channel"),
                "created",
                now,
                now
        ));
    }

    BarBillRecord settleBill(String billId, BarBillSettleRequest request) {
        BarBillSettleRequest actual = requireRequest(request);
        BarBillRecord bill = repository.findBillById(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "bar bill not found"));
        checkMerchantOwnership(bill.merchantId());
        if ("settled".equals(bill.settlementStatus())) {
            throw ApiException.conflict("BAR_BILL_ALREADY_SETTLED", "bar bill is already settled");
        }
        requireText(actual.paymentId(), "payment_id");
        long settlementAmount = actual.settlementAmountCent() == null
                ? bill.payableAmountCent()
                : nonNegative(actual.settlementAmountCent(), "settlement_amount_cent");
        if (settlementAmount > bill.payableAmountCent()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "settlement amount cannot exceed payable amount");
        }
        Instant now = Instant.now();
        // A2/A3：结算渠道（统一词表，与 finance 对账归并）+ 现金/POS 收款留痕 + D4 权威事件。
        String channel = trimToDefault(actual.paymentChannel(), "aggregate_pay");
        if (!SETTLE_CHANNELS.contains(channel)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "unsupported settle_channel: " + channel);
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
        return repository.saveBill(billWithAmounts(
                bill,
                bill.subtotalAmountCent(),
                bill.discountAmountCent(),
                bill.depositDeductAmountCent(),
                bill.payableAmountCent(),
                settlementAmount,
                settlementAmount >= bill.payableAmountCent() ? "paid" : "partial_paid",
                settlementAmount >= bill.payableAmountCent() ? "settled" : "partial_settled",
                now
        ));
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
            afterPayable = beforePayable - amount;
            refundedAt = Instant.now();
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
        BarBillRecord bill = repository.findBillById(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "bar bill not found"));
        checkMerchantOwnership(bill.merchantId());
        String roleType = trimToNull(bill.salesStaffId()) == null ? "non_sales" : "sales";
        int rateBp = "sales".equals(roleType) ? 1500 : 1000;
        long base = Math.max(0L, bill.subtotalAmountCent() - bill.discountAmountCent());
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

    private void checkMerchantOwnership(String resourceMerchantId) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(resourceMerchantId)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "access to another merchant's resource is not allowed");
        }
    }

}
