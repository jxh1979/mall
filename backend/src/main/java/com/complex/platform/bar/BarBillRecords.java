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
                updatedAt,
                // V091：金额变更不改反结/小费元数据，原样携带。
                bill.reopened(),
                bill.reopenedByUserId(),
                bill.reopenedAt(),
                bill.reopenReason(),
                bill.reopenCount(),
                bill.tipAmountCent(),
                // V231：账单首次转 paid/partial_paid（结账成功）时记支付时刻=本次 updatedAt；已有 paid_at 保持不覆盖。
                //   反结 billReopened 走 26 参兼容构造器（paid_at=null）会清空，重结账再记，符合语义。
                bill.paidAt() != null ? bill.paidAt()
                        : (("paid".equals(paymentStatus) || "partial_paid".equals(paymentStatus)) ? updatedAt : null)
        );
    }

    /** V091 反结：settled→reopened，重置可调整状态（unpaid/unsettled），保留累计反结次数 +1，留痕。 */
    static BarBillRecord billReopened(
            BarBillRecord bill,
            String reopenedByUserId,
            String reopenReason,
            Instant reopenedAt
    ) {
        return new BarBillRecord(
                bill.id(),
                bill.billNo(),
                bill.orderId(),
                bill.barId(),
                bill.merchantId(),
                bill.tableId(),
                bill.reservationId(),
                bill.subtotalAmountCent(),
                bill.discountAmountCent(),
                bill.depositDeductAmountCent(),
                bill.payableAmountCent(),
                0L,
                bill.currency(),
                "unpaid",
                "unsettled",
                bill.salesStaffId(),
                bill.salesRoleType(),
                bill.createdAt(),
                reopenedAt,
                true,
                reopenedByUserId,
                reopenedAt,
                reopenReason,
                bill.reopenCount() + 1,
                bill.tipAmountCent()
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
        // 单号唯一性:前截断会把账单号后的区分符(:split:/:merge:/幂等键)截掉,导致同一账单多次操作
        // 生成同一单号→撞主键。改为 >40 用 SHA-256 定长32哈希(保唯一+定长≤64),短的保持可读。
        if (resolved.length() > 40) {
            try {
                return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                        .digest(resolved.getBytes(java.nio.charset.StandardCharsets.UTF_8))).substring(0, 32);
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new IllegalStateException("billSlug hashing unavailable", e);
            }
        }
        return resolved;
    }
}
