package com.complex.platform.bar;

// 文件版本：V0.02

import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.finance.PaymentIntentRecord;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class BarOperationExecutor {

    private final AuditedOperationExecutor operations;

    public BarOperationExecutor(AuditedOperationExecutor operations) {
        this.operations = operations;
    }

    <T> PageResponse<T> readPage(BarReadOperation operation, Supplier<List<T>> action) {
        return operations.readPage(operation, action);
    }

    <T> T executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            BarWriteOperation operation,
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
                Map.of("operation_id", operationId)
        );
    }

    /** 同上，成功留痕另带业务明细（如第十六批第 16 条账单异常的登记／处置原因）。 */
    <T> T executeWrite(String operationId, String idempotencyKey, Object request, Class<T> responseType,
            BarWriteOperation operation, Map<String, Object> detail, Supplier<T> action) {
        Map<String, Object> metadata = new java.util.LinkedHashMap<>(detail);
        metadata.put("operation_id", operationId);
        return operations.executeWrite(operationId, idempotencyKey, request, responseType, operation, action,
                this::businessId, metadata);
    }

    /** 只审计留痕、不套幂等键的写。用于目录删除生命周期(request/approve):
     *  物理删除不可逆须可追溯(谁申请/为何/谁批准),但删除审批无客户端幂等键;
     *  状态机天然防重放(删后重放 404、驳回后重放 400),故豁免幂等。 */
    <T> T auditedWrite(BarWriteOperation operation, java.util.function.Supplier<T> action) {
        return operations.readSingle(operation, action, this::businessId);
    }

    String requireText(String value, String fieldName) {
        return operations.requireText(value, fieldName);
    }

    private String businessId(Object value) {
        if (value instanceof BarTableRecord record) return record.id();
        if (value instanceof BarTableSpaceAssignmentRecord record) return record.tableId();
        if (value instanceof BarTableSessionRecord record) return record.id();
        if (value instanceof BarTableSessionAdjustmentRecord record) return record.id();
        if (value instanceof BarReservationRecord record) return record.id();
        if (value instanceof BarDepositRuleRecord record) return record.id();
        if (value instanceof BarReservationEntryAllowanceVerificationRecord record) return record.reservationId();
        if (value instanceof BarTableGuestCheckinRecord record) return record.id();
        if (value instanceof BarProductRecord record) return record.id();
        if (value instanceof BarOrderRecord record) return record.id();
        if (value instanceof BarOrderItemRecord record) return record.id();
        if (value instanceof BarOrderItemVoidRecord record) return record.id();
        if (value instanceof BarProductionTicketRecord record) return record.id();
        if (value instanceof BarProductionTicketEventRecord record) return record.id();
        if (value instanceof BarBillRecord record) return record.id();
        if (value instanceof BarBillAdjustmentRecord record) return record.id();
        if (value instanceof BarBillOperationRecord record) return record.id();
        if (value instanceof BarCommissionCalculationRecord record) return record.id();
        if (value instanceof BarGiftRedemptionRecord record) return record.id();
        if (value instanceof PaymentIntentRecord record) return record.id();
        if (value instanceof BarProductCategoryRecord record) return record.id();
        if (value instanceof BarComboRecord record) return record.id();
        if (value instanceof BarCatalogDeleteDecision record) return record.id();
        if (value instanceof BarStaffBookingSuspensionRecord record) return record.id();
        return null;
    }
}
