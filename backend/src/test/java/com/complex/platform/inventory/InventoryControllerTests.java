package com.complex.platform.inventory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.auth.AuthService;
import com.complex.platform.common.api.GlobalExceptionHandler;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.config.SecurityConfig;
import com.complex.platform.inventory.dto.InventoryItemRequest;
import com.complex.platform.inventory.dto.InventorySupplierRequest;
import com.complex.platform.inventory.dto.InventoryRiskScanRequest;
import com.complex.platform.inventory.dto.InventoryStockBatchRequest;
import com.complex.platform.inventory.dto.PurchaseRequestCreate;
import com.complex.platform.inventory.dto.SavedBottleClaimRequest;
import com.complex.platform.inventory.dto.SavedBottleRequest;
import com.complex.platform.inventory.dto.StockAdjustmentRequest;
import com.complex.platform.inventory.dto.StockInRequest;
import com.complex.platform.inventory.dto.StockMovementRequest;
import com.complex.platform.inventory.dto.SupplierReconciliationRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = InventoryController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, InventoryControllerTests.TestBeans.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class InventoryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryService service;

    @Autowired
    private AuthorizationService authorizationService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service, authorizationService);
        when(service.listInventoryItems()).thenReturn(new PageResponse<>(List.of(), 1, 50, 0));
        when(service.listPurchaseRequests()).thenReturn(new PageResponse<>(List.of(), 1, 50, 0));
        when(service.listSavedBottles()).thenReturn(new PageResponse<>(List.of(), 1, 50, 0));
        when(service.listStockBalances()).thenReturn(new PageResponse<>(List.of(), 1, 50, 0));
        when(service.listStockBatches()).thenReturn(new PageResponse<>(List.of(stockBatch()), 1, 50, 1));
        when(service.listRiskAlerts()).thenReturn(new PageResponse<>(List.of(riskAlert()), 1, 50, 1));
        when(service.listSupplierReconciliations()).thenReturn(new PageResponse<>(List.of(supplierReconciliation()), 1, 50, 1));
        when(service.listStockMovements()).thenReturn(new PageResponse<>(List.of(), 1, 50, 0));
        when(service.listInventorySuppliers()).thenReturn(new PageResponse<>(List.of(), 1, 50, 0));
        when(service.createInventoryItem(any(InventoryItemRequest.class))).thenReturn(item());
        when(service.createPurchaseRequest(any(PurchaseRequestCreate.class))).thenReturn(purchaseRequest());
        when(service.createSavedBottle(any(SavedBottleRequest.class))).thenReturn(savedBottle());
        when(service.claimSavedBottle(eq("saved_1"), any(SavedBottleClaimRequest.class))).thenReturn(savedBottleClaim());
        when(service.createStockAdjustment(any(StockAdjustmentRequest.class))).thenReturn(stockAdjustment());
        when(service.createStockBatch(any(InventoryStockBatchRequest.class))).thenReturn(stockBatch());
        when(service.runInventoryRiskScan(any(InventoryRiskScanRequest.class))).thenReturn(riskScanResult());
        when(service.createSupplierReconciliation(any(SupplierReconciliationRequest.class))).thenReturn(supplierReconciliation());
        when(service.createStockIn(any(StockInRequest.class))).thenReturn(stockMovement("inbound"));
        when(service.createStockMovement(any(StockMovementRequest.class))).thenReturn(stockMovement("outbound"));
        when(service.transferStockMovement(any(StockMovementRequest.class))).thenReturn(stockMovement("transfer"));
        when(service.createTypedStockMovement(eq("damage"), any(StockMovementRequest.class))).thenReturn(stockMovement("damage"));
        when(service.createInventorySupplier(any(InventorySupplierRequest.class))).thenReturn(supplier());
        when(service.scanTraceCodeHandover(any(VerificationRequest.class))).thenReturn(traceHandover());
    }

    @ParameterizedTest
    @CsvSource({
            "GET,/api/v1/inventory/items,inventory.stock.view",
            "POST,/api/v1/inventory/items,inventory.stock.move",
            "GET,/api/v1/inventory/purchase-requests,inventory.purchase_request.view",
            "POST,/api/v1/inventory/purchase-requests,inventory.purchase_request.manage",
            "GET,/api/v1/inventory/saved-bottles,bar.saved_bottle.manage",
            "POST,/api/v1/inventory/saved-bottles,bar.saved_bottle.manage",
            "POST,/api/v1/inventory/saved-bottles/saved_1/claim,bar.saved_bottle.manage",
            "POST,/api/v1/inventory/stock-adjustments,inventory.stock.adjust",
            "GET,/api/v1/inventory/stock-balances,inventory.stock.view",
            "GET,/api/v1/inventory/stock-batches,inventory.stock.view",
            "POST,/api/v1/inventory/stock-batches,inventory.stock.batch.manage",
            "GET,/api/v1/inventory/risk-alerts,inventory.risk_alert.view",
            "POST,/api/v1/inventory/risk-alerts/scan,inventory.risk.scan",
            "GET,/api/v1/inventory/supplier-reconciliations,inventory.supplier.reconcile",
            "POST,/api/v1/inventory/supplier-reconciliations,inventory.supplier.reconcile",
            "POST,/api/v1/inventory/stock-in,inventory.stock.move",
            "GET,/api/v1/inventory/stock-movements,inventory.stock.view",
            "POST,/api/v1/inventory/stock-movements,inventory.stock.move",
            "POST,/api/v1/inventory/stock-movements/transfer,inventory.stock.move",
            "POST,/api/v1/inventory/stock-movements/damage,inventory.stock.move",
            "GET,/api/v1/inventory/suppliers,inventory.supplier.view",
            "POST,/api/v1/inventory/suppliers,inventory.supplier.manage",
            "POST,/api/v1/inventory/trace-codes/scan-handover,inventory.handover.scan"
    })
    void inventoryRoutesUseExactOpenApiPermission(String method, String path, String permission) throws Exception {
        if ("GET".equals(method)) {
            mockMvc.perform(get(path).with(user("ops").roles("P0_SMOKE")))
                    .andExpect(status().isOk());
        } else {
            mockMvc.perform(post(path)
                            .with(user("ops").roles("P0_SMOKE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyFor(path)))
                    .andExpect(status().isOk());
        }

        verify(authorizationService).requirePermission(permission);
    }

    @ParameterizedTest
    @CsvSource({
            "/api/v1/inventory/items,'{\"item_name\":\"Vodka\",\"category\":\"spirit\",\"unit\":\"bottle\"}'",
            "/api/v1/inventory/purchase-requests,'{\"items\":[{\"item_id\":\"item_1\",\"quantity\":2}]}'",
            "/api/v1/inventory/saved-bottles,'{\"customer_id\":\"cust_1\",\"bar_id\":\"bar_1\",\"items\":[{\"item_id\":\"item_1\",\"quantity\":1,\"bottle_name\":\"Vodka\",\"unit\":\"bottle\"}]}'",
            "/api/v1/inventory/saved-bottles/saved_1/claim,'{\"claim_items\":[{\"quantity\":1}]}'",
            "/api/v1/inventory/stock-adjustments,'{\"adjustment_type\":\"loss\",\"items\":[{\"item_id\":\"item_1\",\"quantity\":1,\"location_id\":\"wh_1\"}],\"reason\":\"broken\"}'",
            "/api/v1/inventory/stock-batches,'{\"item_id\":\"item_1\",\"location_id\":\"wh_1\",\"batch_no\":\"BATCH-1\",\"quantity\":2}'",
            "/api/v1/inventory/risk-alerts/scan,'{\"expiry_warning_days\":7,\"operator_user_id\":\"staff_ops\"}'",
            "/api/v1/inventory/supplier-reconciliations,'{\"supplier_id\":\"supplier_1\",\"period_start\":\"2026-06-01\",\"period_end\":\"2026-06-30\",\"expected_amount_cent\":100000,\"actual_amount_cent\":88000,\"operator_user_id\":\"staff_finance\"}'",
            "/api/v1/inventory/stock-in,'{\"source_type\":\"purchase\",\"warehouse_id\":\"wh_1\",\"items\":[{\"item_id\":\"item_1\",\"quantity\":5}]}'",
            "/api/v1/inventory/stock-movements,'{\"from_location_id\":\"wh_1\",\"to_location_id\":\"bar_1\",\"items\":[{\"item_id\":\"item_1\",\"quantity\":1}]}'",
            "/api/v1/inventory/stock-movements/transfer,'{\"from_location_id\":\"wh_1\",\"to_location_id\":\"bar_1\",\"items\":[{\"item_id\":\"item_1\",\"quantity\":1}]}'",
            "/api/v1/inventory/stock-movements/outbound,'{\"from_location_id\":\"wh_1\",\"to_location_id\":\"bar_1\",\"items\":[{\"item_id\":\"item_1\",\"quantity\":1}]}'",
            "/api/v1/inventory/suppliers,'{\"supplier_name\":\"Supplier\"}'",
            "/api/v1/inventory/trace-codes/scan-handover,'{\"credential_type\":\"manual\",\"credential_payload\":\"TRACE-1\",\"device_id\":\"dev_1\",\"point_id\":\"bar_1\"}'"
    })
    void writeRoutesRejectMissingIdempotencyKey(String path, String body) throws Exception {
        mockMvc.perform(post(path)
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("idempotency_key is required"));
    }

    @Test
    void createInventoryItemUsesSnakeCaseRequestAndResponse() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/items")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "item-create",
                                  "item_name": "Vodka",
                                  "category": "spirit",
                                  "unit": "bottle",
                                  "min_quantity": 2,
                                  "status": "active"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("item_1"))
                .andExpect(jsonPath("$.data.item_name").value("Vodka"))
                .andExpect(jsonPath("$.data.min_quantity").value(2));

        verify(authorizationService).requirePermission("inventory.stock.move");
    }

    private String bodyFor(String path) {
        if (path.contains("purchase-requests")) {
            return """
                    {"idempotency_key":"purchase-create","items":[{"item_id":"item_1","quantity":2}]}
                    """;
        }
        if (path.contains("saved-bottles/") && path.endsWith("/claim")) {
            return """
                    {"idempotency_key":"claim-create","claim_items":[{"quantity":1}]}
                    """;
        }
        if (path.endsWith("saved-bottles")) {
            return """
                    {"idempotency_key":"saved-create","customer_id":"cust_1","bar_id":"bar_1","items":[{"item_id":"item_1","quantity":2,"bottle_name":"Vodka","unit":"bottle"}]}
                    """;
        }
        if (path.contains("stock-adjustments")) {
            return """
                    {"idempotency_key":"adjust-create","adjustment_type":"loss","items":[{"item_id":"item_1","quantity":1,"location_id":"wh_1"}],"reason":"broken"}
                    """;
        }
        if (path.contains("stock-batches")) {
            return """
                    {"idempotency_key":"batch-create","item_id":"item_1","location_id":"wh_1","batch_no":"BATCH-1","quantity":2,"expire_at":"2026-06-20T00:00:00Z","status":"active"}
                    """;
        }
        if (path.contains("risk-alerts")) {
            return """
                    {"idempotency_key":"risk-scan","expiry_warning_days":7,"operator_user_id":"staff_ops"}
                    """;
        }
        if (path.contains("supplier-reconciliations")) {
            return """
                    {"idempotency_key":"supplier-recon","supplier_id":"supplier_1","period_start":"2026-06-01","period_end":"2026-06-30","expected_amount_cent":100000,"actual_amount_cent":88000,"status":"difference_found","operator_user_id":"staff_finance"}
                    """;
        }
        if (path.contains("stock-in")) {
            return """
                    {"idempotency_key":"stock-in-create","source_type":"purchase","warehouse_id":"wh_1","items":[{"item_id":"item_1","quantity":5}]}
                    """;
        }
        if (path.contains("stock-movements")) {
            return """
                    {"idempotency_key":"move-create","from_location_id":"wh_1","to_location_id":"bar_1","items":[{"item_id":"item_1","quantity":1}]}
                    """;
        }
        if (path.contains("suppliers")) {
            return """
                    {"idempotency_key":"supplier-create","supplier_name":"Supplier","status":"active"}
                    """;
        }
        if (path.contains("trace-codes")) {
            return """
                    {"idempotency_key":"trace-create","credential_type":"manual","credential_payload":"TRACE-1","device_id":"dev_1","point_id":"bar_1","business_context":{"item_id":"item_1","from_staff_id":"staff_a","to_staff_id":"staff_b"}}
                    """;
        }
        return """
                {"idempotency_key":"item-create","item_name":"Vodka","category":"spirit","unit":"bottle","min_quantity":2,"status":"active"}
                """;
    }

    private InventoryItemRecord item() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new InventoryItemRecord("item_1", null, "Vodka", "spirit", "bottle", null, false, 2, "active", now, now, null);
    }

    private InventorySupplierRecord supplier() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new InventorySupplierRecord("supplier_1", "Supplier", "general", null, null, "active", now, now, null);
    }

    private PurchaseRequestRecord purchaseRequest() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new PurchaseRequestRecord("purchase_1", "PR202606140001", "supplier_1", "item_1", 2, LocalDate.parse("2026-06-20"), "created", List.of(), now, now, null);
    }

    private SavedBottleRecord savedBottle() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new SavedBottleRecord("saved_1", "cust_1", "bar_1", "item_1", "Vodka", 2, 2, "bottle", null, "stored", List.of(), now, now, null, false, null, null, null, null, null);
    }

    private SavedBottleClaimRecord savedBottleClaim() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new SavedBottleClaimRecord("claim_1", "saved_1", 1, null, "staff_work_phone", null, "picked_up", List.of(), now);
    }

    private StockMovementRecord stockMovement(String movementType) {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new StockMovementRecord("movement_1", movementType, "item_1", "wh_1", "bar_1", 1, "manual", null, null, null, List.of(), "completed", now, null);
    }

    private StockAdjustmentRecord stockAdjustment() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new StockAdjustmentRecord("adjust_1", "item_1", "wh_1", "loss", 1, "broken", List.of(), "completed", now, null);
    }

    private InventoryStockBatchRecord stockBatch() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new InventoryStockBatchRecord("batch_1", "item_1", "wh_1", "BATCH-1", "supplier_1", 2, Instant.parse("2026-06-20T00:00:00Z"), "active", List.of(), now, now, null);
    }

    private InventoryRiskAlertRecord riskAlert() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new InventoryRiskAlertRecord("risk_1", "safety_stock_low", "medium", "item_1", "wh_1", null, 2, 5, null, "open", "staff_ops", now, null);
    }

    private InventoryRiskScanResult riskScanResult() {
        return new InventoryRiskScanResult(1, List.of(riskAlert()));
    }

    private SupplierReconciliationRecord supplierReconciliation() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new SupplierReconciliationRecord("supplier_recon_1", "supplier_1", LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-30"), 100000L, 88000L, -12000L, "difference_found", List.of("att_invoice"), "staff_finance", now, now, null);
    }

    private TraceHandoverRecord traceHandover() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new TraceHandoverRecord("trace_1", "TRACE-1", "item_1", "staff_a", "staff_b", "dev_1",
                "chain_started", "in_progress", "out_verify", List.of(), now);
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        InventoryService inventoryService() {
            return Mockito.mock(InventoryService.class);
        }

        @Bean
        AuthorizationService authorizationService() {
            return Mockito.mock(AuthorizationService.class);
        }

        @Bean
        AuthService authService() {
            return Mockito.mock(AuthService.class);
        }
    }
}
