package com.complex.platform.bar;

import static com.complex.platform.bar.BarValidation.jsonText;
import static com.complex.platform.bar.BarValidation.trimToDefault;

import com.complex.platform.common.api.ApiException;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

final class BarBillRecords {

    private BarBillRecords() {
    }

    static BarBillRecord billWithAmounts(
            BarBillRecord bill,
            long subtotalAmountCent,
            long discountAmountCent,
            long depositDeductAmountCent,
            long payableAmountCent,
            long paidAmountCent,
            String paymentStatus,
            String settlementStatus,
            Instant updatedAt
    ) {
        return new BarBillRecord(
                bill.id(),
                bill.billNo(),
                bill.orderId(),
                bill.barId(),
                bill.merchantId(),
                bill.tableId(),
                bill.reservationId(),
                subtotalAmountCent,
                discountAmountCent,
                depositDeductAmountCent,
                payableAmountCent,
                paidAmountCent,
                bill.currency(),
                paymentStatus,
                settlementStatus,
                bill.salesStaffId(),
                bill.salesRoleType(),
                bill.createdAt(),
                updatedAt
        );
    }

    static void requireBillMovable(BarBillRecord bill, String code) {
        if (bill.paidAmountCent() > 0
                || "settled".equals(bill.settlementStatus())
                || "merged".equals(bill.settlementStatus())
                || "merged".equals(bill.paymentStatus())) {
            throw ApiException.conflict(code, "bar bill has payment, settlement, or merge state");
        }
    }

    static String billAdjustmentSnapshot(
            BarBillRecord before,
            long afterPayableAmountCent,
            String afterPaymentStatus,
            String afterSettlementStatus,
            String arApItemId
    ) {
        return """
                {"bill_id":"%s","payable_before":%d,"payable_after":%d,"payment_status_before":"%s","payment_status_after":"%s","settlement_status_before":"%s","settlement_status_after":"%s","ar_ap_item_id":"%s"}
                """.formatted(
                jsonText(before.id()),
                before.payableAmountCent(),
                afterPayableAmountCent,
                jsonText(before.paymentStatus()),
                jsonText(afterPaymentStatus),
                jsonText(before.settlementStatus()),
                jsonText(afterSettlementStatus),
                jsonText(arApItemId)
        ).trim();
    }

    static String splitBillNo(String sourceBillNo, String idempotencyKey) {
        String suffix = "-S" + billSlug(idempotencyKey).substring(0, 6).toUpperCase(Locale.ROOT);
        String prefix = sourceBillNo == null ? "BILL" : sourceBillNo;
        int maxPrefixLength = Math.max(1, 64 - suffix.length());
        return prefix.substring(0, Math.min(prefix.length(), maxPrefixLength)) + suffix;
    }

    static String billOperationSnapshot(BarBillRecord before, BarBillRecord after, BarBillRecord related) {
        return """
                {"primary_bill_id":"%s","related_bill_id":"%s","primary_payable_before":%d,"primary_payable_after":%d,"related_payable":%d}
                """.formatted(
                jsonText(before.id()),
                jsonText(related.id()),
                before.payableAmountCent(),
                after.payableAmountCent(),
                related.payableAmountCent()
        ).trim();
    }

    static String billSlug(String value) {
        String slug = trimToDefault(value, UUID.randomUUID().toString())
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        String fallback = UUID.randomUUID().toString().replace("-", "");
        String resolved = slug.isEmpty() ? fallback : slug;
        return resolved.length() > 40 ? resolved.substring(0, 40) : resolved;
    }
}
