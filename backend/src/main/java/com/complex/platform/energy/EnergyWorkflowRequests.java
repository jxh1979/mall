package com.complex.platform.energy;

// 文件版本：V0.01
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class EnergyWorkflowRequests {
    private EnergyWorkflowRequests() { }
    public record Opening(String idempotencyKey, BigDecimal initialValue, Instant initialAt,
            String note, List<String> attachmentIds) { }
    public record Replacement(String idempotencyKey, String newMeterName, String period,
            BigDecimal oldFinalValue, BigDecimal newInitialValue, Instant replacedAt,
            String note, List<String> attachmentIds) { }
    public record Retirement(String idempotencyKey, String period, BigDecimal finalValue, Instant retiredAt,
            String note, List<String> attachmentIds) { }
    public record Verification(String idempotencyKey, BigDecimal verifiedValue, BigDecimal verifiedOpeningValue,
            String method, String reason, List<String> attachmentIds, String finalApproverId) { }
    public record ActualCharge(String idempotencyKey, String contractId, String meterId, String period,
            long amountCent, LocalDate dueDate, String supplierBillReference, List<String> attachmentIds) { }
    public record ReviewSettings(String idempotencyKey, int days, int minDays, int maxDays,
            String supervisorId, String reason, String finalApproverId) { }
    public record Assignment(String idempotencyKey, String verifierId, String reason) { }
}
