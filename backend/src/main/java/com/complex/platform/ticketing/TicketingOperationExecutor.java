package com.complex.platform.ticketing;

// 文件版本：V0.10

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
        return executeWrite(operationId, idempotencyKey, request, responseType, operation, action, Map.of());
    }

    /** 第十八批 N 段第 120 条 120-R2：成功留痕带业务元数据（一体机售票的售票原因、终端、设备与点位）。 */
    <T> T executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            TicketingWriteOperation operation,
            Supplier<T> action,
            Map<String, Object> successMetadata
    ) {
        return operations.executeWrite(
                operationId,
                idempotencyKey,
                request,
                responseType,
                operation,
                action,
                this::businessId,
                successMetadata
        );
    }

    /** 第十八批 AO 段第 249 条 249-R4：同一请求里另记一条成功事件（最高权限本人发起即删时补记 delete_approved）。 */
    void recordSuccess(TicketingWriteOperation operation, String businessId) {
        operations.recordDetail(operation.successEvent(), operation.permissionCode(), operation.businessType(), businessId, Map.of());
    }

    String requireText(String value, String fieldName) {
        return operations.requireText(value, fieldName);
    }

    private String businessId(Object value) {
        if (value instanceof TicketTypeRecord record) return record.id();
        if (value instanceof TicketTypeGiftFloor floor) return floor.ticketTypeId();
        if (value instanceof TicketRefundableEnableService.TicketRefundableEnableResult result) return result.ticketTypeId();
        if (value instanceof TicketTypeRuleVersionView version) return version.id();
        if (value instanceof EntitlementTemplateRecord record) return record.id();
        if (value instanceof TicketTypeEntitlementRecord record) return record.id();
        if (value instanceof VerificationResponse response) return response.id();
        if (value instanceof VerificationRecord record) return record.id();
        if (value instanceof TicketTransferResponse response) return response.id();
        if (value instanceof IssuedTicketResponse response && !response.issuedEntitlements().isEmpty()) {
            return response.issuedEntitlements().getFirst().entitlementCode();
        }
        if (value instanceof TicketPriceRuleRecord record) return record.id();
        if (value instanceof TicketOrderRecord record) return record.id();
        if (value instanceof TicketPaymentIntentRecord record) return record.id();
        if (value instanceof TicketRefundRecord record) return record.id();
        if (value instanceof TicketSafetyEventRecord record) return record.id();
        if (value instanceof com.complex.platform.finance.PaymentConfirmationService.Confirmation confirmation) return confirmation.confirmationId();
        return null;
    }
}
