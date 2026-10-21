package com.complex.platform.ticketing;

// 文件版本：V0.01

import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditedOperationExecutor;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class TicketingOperationExecutor {

    private final AuditedOperationExecutor operations;

    public TicketingOperationExecutor(AuditedOperationExecutor operations) {
        this.operations = operations;
    }

    <T> PageResponse<T> readPage(TicketingReadOperation operation, Supplier<List<T>> action) {
        return operations.readPage(operation, action);
    }

    <T> T executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            TicketingWriteOperation operation,
            Supplier<T> action
    ) {
        return operations.executeWrite(
                operationId,
                idempotencyKey,
                request,
                responseType,
                operation,
                action,
                this::businessId,
                Map.of()
        );
    }

    String requireText(String value, String fieldName) {
        return operations.requireText(value, fieldName);
    }

    private String businessId(Object value) {
        if (value instanceof TicketTypeRecord record) return record.id();
        if (value instanceof EntitlementTemplateRecord record) return record.id();
        if (value instanceof TicketTypeEntitlementRecord record) return record.id();
        if (value instanceof VerificationResponse response) return response.id();
        if (value instanceof VerificationRecord record) return record.id();
        if (value instanceof TicketTransferResponse response) return response.id();
        if (value instanceof IssuedTicketResponse response && !response.issuedEntitlements().isEmpty()) {
            return response.issuedEntitlements().getFirst().entitlementCode();
        }
        if (value instanceof SyncTasksResponse response && !response.results().isEmpty()) {
            return response.results().getFirst().clientTaskId();
        }
        if (value instanceof TicketPriceRuleRecord record) return record.id();
        if (value instanceof TicketOrderRecord record) return record.id();
        if (value instanceof TicketPaymentIntentRecord record) return record.id();
        if (value instanceof TicketRefundRecord record) return record.id();
        if (value instanceof TicketSafetyEventRecord record) return record.id();
        return null;
    }
}
