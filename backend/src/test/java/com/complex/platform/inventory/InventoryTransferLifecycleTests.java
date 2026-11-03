package com.complex.platform.inventory;

// 文件版本：V0.04

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.TraceabilityService;
import com.complex.platform.finance.FinanceRepository;
import com.complex.platform.gate.TerminalSwitchGuard;
import com.complex.platform.inventory.dto.ReceiveTransferRequest;
import com.complex.platform.inventory.dto.ReturnTransferRequest;
import com.complex.platform.inventory.dto.ReturnTransferRequest.ReturnTransferLineRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * AUD-002/003：借调归还生命周期（return/cancel）单测。
 * FS-019（BSM L1201「「借」只转移保管责任、不转移所有权」「不得伪造归还扫码或实际归还时间」）：借用型(lending+borrow)收货只改保管主体、
 * 所有权始终是借出方；归还须借出方按行扫码/数量登记，保管主体翻回借出方、全部数量归还才 returned；
 * 已发货的单不能直接取消（FS-011：须实际退回验收或经复核完成损失处理），只有新建的借调单可直接取消。
 *
 * <p>InventoryRepository 用 Mockito mock + 有状态 Answer(orders/lines/traceOwners/custodians 四张内存表),
 * 只桩流转/溯源相关方法,避免手写上百个接口方法。
 */
class InventoryTransferLifecycleTests {

    private final Map<String, InventoryTransferOrderRecord> orders = new LinkedHashMap<>();
    private final Map<String, List<InventoryTransferLineRecord>> lines = new LinkedHashMap<>();
    private final Map<String, String> traceOwners = new LinkedHashMap<>();
    private final Map<String, String> custodians = new LinkedHashMap<>();

    private InventoryRepository repository;
    private FinanceRepository financeRepository;
    private InventoryService service;

    @BeforeEach
    void setUp() {
        orders.clear();
        lines.clear();
        traceOwners.clear();
        custodians.clear();
        repository = mock(InventoryRepository.class);
        when(repository.findTransferOrderById(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(orders.get(inv.<String>getArgument(0))));
        when(repository.listTransferLinesByOrder(anyString()))
                .thenAnswer(inv -> lines.getOrDefault(inv.<String>getArgument(0), List.of()));
        when(repository.saveTransferOrder(any())).thenAnswer(inv -> {
            InventoryTransferOrderRecord o = inv.getArgument(0);
            orders.put(o.id(), o);
            return o;
        });
        when(repository.traceCodeExists(anyString()))
                .thenAnswer(inv -> traceOwners.containsKey(inv.<String>getArgument(0)));
        // FS-019：保管主体 CAS——所有权须为 ownerId，只改保管人，所有权不动。
        when(repository.updateTraceCodeCustodianIfOwnedBy(anyString(), anyString(), anyString(), anyString(), any(), any()))
                .thenAnswer(inv -> {
                    String code = inv.getArgument(0);
                    if (!traceOwners.containsKey(code) || !Objects.equals(traceOwners.get(code), inv.<String>getArgument(1))) {
                        return 0;
                    }
                    custodians.put(code, inv.getArgument(3));
                    return 1;
                });
        when(repository.lendingFacts(anyString())).thenReturn(InventoryLendingFacts.EMPTY);

        financeRepository = mock(FinanceRepository.class);
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        // 归还由借出方 m_a 的商户账号登记。
        when(authorizationService.currentMerchantIdQuietly()).thenReturn(Optional.of("m_a"));
        when(authorizationService.currentActor()).thenReturn(new com.complex.platform.access.AccessActor(
                "usr_lender", "usr_lender", "staff", "prj_a", "m_a", "active"));
        service = new InventoryService(
                repository,
                new IdempotencyService(new FakeIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(new FakeAuditEventRepository()),
                authorizationService,
                mock(TerminalSwitchGuard.class),
                mock(TraceabilityService.class),
                null,
                null,
                null,
                financeRepository, null, null, null);
        service.transferFlow = mock(InventoryTransferFlowService.class);
    }

    // FS-019 R2（BSM L1201「实际归还时间取借出方收货扫码的时刻」）：借出方扫齐两码归还 → 保管主体翻回 m_a、所有权始终 m_a、置 returned。
    @Test
    void returnBorrowedOrderFlipsCustodyBackToLenderAndMarksReturned() {
        seedOrder(order("to_borrow", "lending", "borrow", "m_a", "m_b", "received"));
        seedLine("to_borrow", List.of("TRACE-1", "TRACE-2"));
        traceOwners.put("TRACE-1", "m_a");
        traceOwners.put("TRACE-2", "m_a");
        custodians.put("TRACE-1", "m_b");
        custodians.put("TRACE-2", "m_b");

        InventoryTransferOrderRecord returned = service.returnTransferOrder("to_borrow", new ReturnTransferRequest("idem-return-1", null,
                List.of(new ReturnTransferLineRequest("to_borrow_l0", 2, List.of("TRACE-1", "TRACE-2")))));

        assertThat(returned.status()).isEqualTo("returned");
        assertThat(custodians.get("TRACE-1")).isEqualTo("m_a");
        assertThat(custodians.get("TRACE-2")).isEqualTo("m_a");
        assertThat(traceOwners.get("TRACE-1")).isEqualTo("m_a");
        assertThat(traceOwners.get("TRACE-2")).isEqualTo("m_a");
        verify(repository, never()).transferTraceCodeOwnership(anyString(), anyString(), anyString(), anyString(), any(), any());
        verify(financeRepository, never()).updateArApItemLedger(any());
    }

    // FS-019 R2（BSM L1201「不得伪造归还扫码或实际归还时间」）：不带扫码/数量的归还请求被拒，单据不动。
    @Test
    void returnWithoutScannedLinesIsRejected() {
        seedOrder(order("to_borrow_noscan", "lending", "borrow", "m_a", "m_b", "received"));
        seedLine("to_borrow_noscan", List.of("TRACE-9"));
        traceOwners.put("TRACE-9", "m_a");
        custodians.put("TRACE-9", "m_b");

        assertThatThrownBy(() -> service.returnTransferOrder("to_borrow_noscan", new ReturnTransferRequest("idem-return-noscan", null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("TRANSFER_RETURN_REQUIRES_SCAN"));
        assertThat(orders.get("to_borrow_noscan").status()).isEqualTo("received");
        assertThat(custodians.get("TRACE-9")).isEqualTo("m_b");
    }

    // FS-019 R1（BSM L1201「借用期间保管人变更为借入方并在溯源码上登记」「借出方始终是这批货的所有权人」）：借用收货只改保管主体。
    @Test
    void receiveBorrowKeepsOwnerAndSetsCustodian() {
        seedOrder(order("to_borrow_recv", "lending", "borrow", "m_a", "m_b", "shipped"));
        seedLine("to_borrow_recv", List.of("TRACE-5"));
        traceOwners.put("TRACE-5", "m_a");

        InventoryTransferOrderRecord received = service.receiveTransferOrder("to_borrow_recv",
                new ReceiveTransferRequest("idem-recv-borrow", List.of("TRACE-5"), null));

        assertThat(received.status()).isEqualTo("received");
        assertThat(traceOwners.get("TRACE-5")).isEqualTo("m_a");
        assertThat(custodians.get("TRACE-5")).isEqualTo("m_b");
        verify(repository, never()).transferTraceCodeOwnership(anyString(), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void returnRejectsNonBorrowTransferType() {
        seedOrder(order("to_supply", "supply", null, "supply_1", "m_b", "received"));

        assertThatThrownBy(() -> service.returnTransferOrder(
                "to_supply", new ReturnTransferRequest("idem-return-2", null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("TRANSFER_NOT_RETURNABLE"));
    }

    @Test
    void returnRejectsWhenNotReceived() {
        seedOrder(order("to_borrow2", "lending", "borrow", "m_a", "m_b", "shipped"));

        assertThatThrownBy(() -> service.returnTransferOrder(
                "to_borrow2", new ReturnTransferRequest("idem-return-3", null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("TRANSFER_ORDER_NOT_RECEIVED"));
    }

    @Test
    void cancelShippedOrderIsRejectedUntilReturnOrLoss() {
        // FS-011（BSM L1212「已出库数量须实际退回验收，或经复核完成损失处理，不能仅点取消就回库」）：
        // 已发货的借调单不能直接取消，也不动台账。
        seedOrder(order("to_lend", "lending", "purchase", "m_a", "m_b", "shipped"));

        assertThatThrownBy(() -> service.cancelTransferOrder("to_lend", new ReceiveTransferRequest("idem-cancel-to_lend", null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("TRANSFER_SHIPPED_REQUIRES_RETURN_OR_LOSS"));
        assertThat(orders.get("to_lend").status()).isEqualTo("shipped");
        verify(financeRepository, never()).updateArApItemLedger(any());
    }

    @Test
    void cancelCreatedOrderNeedsNoReversal() {
        seedOrder(order("to_created", "lending", "borrow", "m_a", "m_b", "created"));

        InventoryTransferOrderRecord cancelled = service.cancelTransferOrder(
                "to_created", new ReceiveTransferRequest("idem-cancel-3", null, null));

        assertThat(cancelled.status()).isEqualTo("cancelled");
        verify(financeRepository, never()).findArApItemById(any());
    }

    private void seedOrder(InventoryTransferOrderRecord order) {
        orders.put(order.id(), order);
    }

    private void seedLine(String orderId, List<String> traceCodes) {
        lines.put(orderId, List.of(new InventoryTransferLineRecord(
                orderId + "_l0", orderId, "item_x", traceCodes.size(), traceCodes, 0L, Instant.now())));
    }

    private static InventoryTransferOrderRecord order(String id, String transferType, String lendingMode,
            String fromOwnerId, String toOwnerId, String status) {
        Instant now = Instant.now();
        return new InventoryTransferOrderRecord(id, transferType, lendingMode,
                "supply".equals(transferType) ? "supply_entity" : "merchant", fromOwnerId,
                "merchant", toOwnerId, null, null, status, null, "usr_admin", now, now, List.of());
    }

    private static final class FakeIdempotencyRepository implements IdempotencyRepository {
        private final Map<String, IdempotencyRecord> records = new LinkedHashMap<>();

        @Override
        public Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey) {
            return Optional.ofNullable(records.get(operationId + ":" + idempotencyKey));
        }

        @Override
        public boolean tryReserve(IdempotencyRecord record) {
            String key = record.operationId() + ":" + record.idempotencyKey();
            if (records.containsKey(key)) {
                return false;
            }
            records.put(key, record);
            return true;
        }

        @Override
        public IdempotencyRecord complete(IdempotencyRecord record) {
            records.put(record.operationId() + ":" + record.idempotencyKey(), record);
            return record;
        }
    }

    private static final class FakeAuditEventRepository implements AuditEventRepository {
        @Override
        public AuditEvent save(AuditEvent event) {
            return event;
        }
    }
}
