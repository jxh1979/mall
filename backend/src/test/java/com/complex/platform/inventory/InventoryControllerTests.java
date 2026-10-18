// 文件版本：V0.02
package com.complex.platform.inventory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import com.complex.platform.inventory.dto.CreateStocktakeRequest;
import com.complex.platform.inventory.dto.InventoryItemRequest;
import com.complex.platform.inventory.dto.InventorySupplierRequest;
import com.complex.platform.inventory.dto.RecordVarianceRequest;
import com.complex.platform.inventory.dto.ReviewVarianceRequest;
import com.complex.platform.inventory.dto.InventoryRiskScanRequest;
import com.complex.platform.inventory.dto.InventoryStockBatchRequest;
import com.complex.platform.inventory.dto.PrintTraceCodeRequest;
import com.complex.platform.inventory.dto.PurchaseRequestCreate;
import com.complex.platform.inventory.dto.RegisterTraceCodeRequest;
import com.complex.platform.inventory.dto.SavedBottleClaimRequest;
import com.complex.platform.inventory.dto.SavedBottleRequest;
import com.complex.platform.inventory.dto.StockAdjustmentRequest;
import com.complex.platform.inventory.dto.StockInRequest;
import com.complex.platform.inventory.dto.StockMovementRequest;
import com.complex.platform.inventory.dto.SupplierQualificationRequest;
import com.complex.platform.inventory.dto.SupplierReconciliationRequest;
import com.complex.platform.inventory.dto.SupplierSettlementMethodRequest;
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
@Import({com.complex.platform.support.AuthAccessTestConfiguration.class, GlobalExceptionHandler.class, SecurityConfig.class, InventoryControllerTests.TestBeans.class})
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
    private SupplierQualificationService supplierQualificationService;

    @Autowired
    private AuthorizationService authorizationService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service, supplierQualificationService, authorizationService);
        when(supplierQualificationService.listQualifications(any()))
                .thenReturn(new PageResponse<>(List.of(), 1, 50, 0));
        when(supplierQualificationService.createQualification(any(SupplierQualificationRequest.class)))
                .thenReturn(qualification());
        when(supplierQualificationService.listSettlementMethods(any()))
                .thenReturn(new PageResponse<>(List.of(), 1, 50, 0));
        when(supplierQualificationService.createSettlementMethod(any(SupplierSettlementMethodRequest.class)))
                .thenReturn(settlementMethod());
        when(service.listInventoryItems()).thenReturn(new PageResponse<>(List.of(), 1, 50, 0));
        when(service.listPurchaseRequests()).thenReturn(new PageResponse<>(List.of(), 1, 50, 0));
        when(service.listSavedBottles()).thenReturn(new PageResponse<>(List.of(), 1, 50, 0));
        when(service.listStockBalances()).thenReturn(new PageResponse<>(List.of(), 1, 50, 0));
        when(service.listStockBatches()).thenReturn(new PageResponse<>(List.of(stockBatch()), 1, 50, 1));
        when(service.listRiskAlerts()).thenReturn(new PageResponse<>(List.of(riskAlert()), 1, 50, 1));
        when(service.listSupplierReconciliations()).thenReturn(new PageResponse<>(List.of(supplierReconciliation()), 1, 50, 1));
        when(service.listStockMovements(anyInt(), anyInt())).thenReturn(new PageResponse<>(List.of(), 1, 50, 0));
        // V296 成本台账两端点（要求 inventory.cost.view）。
        when(service.listCostLedger(any(), anyInt(), anyInt())).thenReturn(new PageResponse<>(List.of(), 1, 50, 0));
        when(service.getCostSummary(any(), any(), any())).thenReturn(new InventoryCostSummaryRecord(
                null, "2026-07-01", "2026-07-18", 0L, 0L, 0L));
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
        when(service.listStocktakes()).thenReturn(new PageResponse<>(List.of(stocktake()), 1, 50, 1));
        when(service.getStocktake(eq("stocktake_1")))
                .thenReturn(new StocktakeDetailRecord(stocktake(), List.of(variance())));
        when(service.createStocktake(any(CreateStocktakeRequest.class))).thenReturn(stocktake());
        when(service.recordVariance(eq("stocktake_1"), any(RecordVarianceRequest.class))).thenReturn(variance());
        when(service.reviewVariance(eq("stocktake_1"), eq("variance_1"), any(ReviewVarianceRequest.class)))
                .thenReturn(variance());
        when(service.listTraceCodes()).thenReturn(new PageResponse<>(List.of(traceCode()), 1, 50, 1));
        when(service.getTraceCode(eq("trace_code_1"))).thenReturn(traceCode());
        when(service.registerTraceCode(any(RegisterTraceCodeRequest.class))).thenReturn(traceCode());
        when(service.printTraceCode(eq("trace_code_1"), any(PrintTraceCodeRequest.class)))
                .thenReturn(traceCode());
    }

    @ParameterizedTest
    @CsvSource({
            "GET,/api/v1/inventory/items,inventory.stock.view",
            "POST,/api/v1/inventory/items,inventory.stock.move",
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
            "GET,/api/v1/inventory/cost-ledger,inventory.cost.view",
            "GET,/api/v1/inventory/cost-summary?period_start=2026-07-01&period_end=2026-07-18,inventory.cost.view",
            "POST,/api/v1/inventory/stock-movements,inventory.stock.move",
            "POST,/api/v1/inventory/stock-movements/transfer,inventory.stock.move",
            "POST,/api/v1/inventory/stock-movements/damage,inventory.stock.move",
            "GET,/api/v1/inventory/suppliers,inventory.supplier.view",
            "POST,/api/v1/inventory/suppliers,inventory.supplier.manage",
            "GET,/api/v1/inventory/supplier-qualifications,inventory.supplier.qualification.manage",
            "POST,/api/v1/inventory/supplier-qualifications,inventory.supplier.qualification.manage",
            "GET,/api/v1/inventory/supplier-settlement-methods,inventory.supplier.settlement.manage",
            "POST,/api/v1/inventory/supplier-settlement-methods,inventory.supplier.settlement.manage",
            "POST,/api/v1/inventory/trace-codes/scan-handover,inventory.handover.scan",
            "GET,/api/v1/inventory/trace-codes,inventory.trace_code.view",
            "GET,/api/v1/inventory/trace-codes/trace_code_1,inventory.trace_code.view",
            "POST,/api/v1/inventory/trace-codes,inventory.trace_code.register",
            "POST,/api/v1/inventory/trace-codes/trace_code_1/print,inventory.trace_code.print",
            "GET,/api/v1/inventory/stocktakes,inventory.stocktake.view",
            "GET,/api/v1/inventory/stocktakes/stocktake_1,inventory.stocktake.view",
            "POST,/api/v1/inventory/stocktakes,inventory.stocktake.create",
            "POST,/api/v1/inventory/stocktakes/stocktake_1/variances,inventory.stocktake.variance_record",
            "POST,/api/v1/inventory/stocktakes/stocktake_1/variances/variance_1/review,inventory.stocktake.variance_review"
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

    @org.junit.jupiter.api.Test
    void purchaseListPreservesAuditedServicePermissionFailure() throws Exception {
        when(service.listPurchaseRequests()).thenThrow(com.complex.platform.common.api.ApiException.forbidden(
                "PERMISSION_DENIED", "无权查看采购申请"));
        mockMvc.perform(get("/api/v1/inventory/purchase-requests").with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isForbidden());
        verify(service).listPurchaseRequests();
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
            "/api/v1/inventory/supplier-qualifications,'{\"supplier_id\":\"supplier_1\",\"qualification_type\":\"business_license\"}'",
            "/api/v1/inventory/supplier-settlement-methods,'{\"supplier_id\":\"supplier_1\",\"settlement_method\":\"monthly\"}'",
            "/api/v1/inventory/trace-codes/scan-handover,'{\"credential_type\":\"manual\",\"credential_payload\":\"TRACE-1\",\"device_id\":\"dev_1\",\"point_id\":\"bar_1\"}'",
            "/api/v1/inventory/trace-codes,'{\"item_id\":\"item_1\",\"trace_code\":\"TRC-1\"}'",
            "/api/v1/inventory/trace-codes/trace_code_1/print,'{}'",
            "/api/v1/inventory/stocktakes,'{\"warehouse_id\":\"wh_1\",\"business_date\":\"2026-06-19\"}'",
            "/api/v1/inventory/stocktakes/stocktake_1/variances,'{\"item_id\":\"item_1\",\"book_qty\":10,\"counted_qty\":7,\"reason\":\"shortage\"}'",
            "/api/v1/inventory/stocktakes/stocktake_1/variances/variance_1/review,'{\"decision\":\"approved\"}'"
    })
    void writeRoutesRejectMissingIdempotencyKey(String path, String body) throws Exception {
        mockMvc.perform(post(path)
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("请勿重复提交，请刷新后再试"));
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
        if (path.endsWith("/trace-codes/trace_code_1/print")) {
            return """
                    {"idempotency_key":"trace-code-print"}
                    """;
        }
        if (path.endsWith("/trace-codes")) {
            return """
                    {"idempotency_key":"trace-code-register","item_id":"item_1","batch_id":"BATCH-1","trace_code":"TRC-1"}
                    """;
        }
        if (path.contains("/variances/") && path.endsWith("/review")) {
            return """
                    {"idempotency_key":"variance-review","decision":"approved","reason":"confirmed"}
                    """;
        }
        if (path.endsWith("/variances")) {
            return """
                    {"idempotency_key":"variance-record","item_id":"item_1","book_qty":10,"counted_qty":7,"reason":"shortage","unit_cost_cents":500}
                    """;
        }
        if (path.endsWith("/stocktakes")) {
            return """
                    {"idempotency_key":"stocktake-create","warehouse_id":"wh_1","business_date":"2026-06-19","remark":"month-end"}
                    """;
        }
        if (path.contains("supplier-qualifications")) {
            return """
                    {"idempotency_key":"qual-create","supplier_id":"supplier_1","qualification_type":"business_license","qualification_no":"BL-1","valid_to":"2030-01-01"}
                    """;
        }
        if (path.contains("supplier-settlement-methods")) {
            return """
                    {"idempotency_key":"settle-create","supplier_id":"supplier_1","settlement_method":"monthly","payment_term_days":30,"applicable_category":"spirit","is_default":true}
                    """;
        }
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
        return new InventoryItemRecord("item_1", null, "Vodka", "spirit", "bottle", null, false, 2, "active", now, now, null, false);
    }

    private StocktakeRecord stocktake() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        return new StocktakeRecord("stocktake_1", "ST1", "wh_1", null,
                LocalDate.parse("2026-06-19"), "counting", null, now, now);
    }

    private InventoryVarianceRecord variance() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        return new InventoryVarianceRecord("variance_1", "stocktake_1", "item_1", null,
                new java.math.BigDecimal("10"), new java.math.BigDecimal("7"), new java.math.BigDecimal("-3"),
                -1500L, "shortage", null, "pending", null, null, null, null, now, now);
    }

    private InventorySupplierRecord supplier() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new InventorySupplierRecord("supplier_1", "Supplier", "general", null, null, "active", now, now, null);
    }

    private SupplierQualificationRecord qualification() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new SupplierQualificationRecord("supplier_qual_1", "supplier_1", "business_license", "BL-1", "机构",
                LocalDate.parse("2026-01-01"), LocalDate.parse("2030-01-01"), List.of("att_1"), "active", now, now, null);
    }

    private SupplierSettlementMethodRecord settlementMethod() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new SupplierSettlementMethodRecord("supplier_settle_1", "supplier_1", "monthly", 30, "spirit", true,
                "active", now, now, null);
    }

    private PurchaseRequestRecord purchaseRequest() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new PurchaseRequestRecord("purchase_1", "PR202606140001", "supplier_1", "item_1", 2, LocalDate.parse("2026-06-20"), "created", List.of(), now, now, null, null, null, null, null, null, null);
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
        return new StockMovementRecord("movement_1", movementType, "item_1", "wh_1", "bar_1", 1, null, null, "manual", null, null, null, List.of(), "completed", now, null);
    }

    /** I-2(V296)：成本=内部口径——无 inventory.cost.view 权限时流水响应成本列脱敏置 null。 */
    @Test
    void stockMovementCostColumnsMaskedWithoutCostViewPermission() throws Exception {
        when(service.listStockMovements(anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(costedMovement()), 1, 50, 1));
        when(authorizationService.currentActorHasPermission("inventory.cost.view")).thenReturn(false);

        mockMvc.perform(get("/api/v1/inventory/stock-movements").with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value("movement_cost_1"))
                .andExpect(jsonPath("$.data.items[0].unit_cost_cent").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.items[0].total_cost_cent").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void stockMovementCostColumnsVisibleWithCostViewPermission() throws Exception {
        when(service.listStockMovements(anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(costedMovement()), 1, 50, 1));
        when(authorizationService.currentActorHasPermission("inventory.cost.view")).thenReturn(true);

        mockMvc.perform(get("/api/v1/inventory/stock-movements").with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].unit_cost_cent").value(500))
                .andExpect(jsonPath("$.data.items[0].total_cost_cent").value(1500));
    }

    private StockMovementRecord costedMovement() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new StockMovementRecord("movement_cost_1", "outbound", "item_1", "wh_1", null, 3,
                500L, 1500L, "manual", null, null, null, List.of(), "completed", now, null);
    }

    private StockAdjustmentRecord stockAdjustment() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new StockAdjustmentRecord("adjust_1", "item_1", "wh_1", "loss", 1, "broken", List.of(), "completed", now, null, null);
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

    private InventoryTraceCodeRecord traceCode() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        return new InventoryTraceCodeRecord("trace_code_1", "TRC-1", "item_1", "BATCH-1", null,
                "registered", null, null, "usr_admin", now, now);
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        PurchaseWorkflowService purchaseWorkflowService() {
            return Mockito.mock(PurchaseWorkflowService.class);
        }

        @Bean
        InventoryService inventoryService() {
            return Mockito.mock(InventoryService.class);
        }

        @Bean
        SupplyContractService supplyContractService() {
            return Mockito.mock(SupplyContractService.class);
        }

        @Bean
        PurchaseRequestService purchaseRequestService() {
            return Mockito.mock(PurchaseRequestService.class);
        }

        @Bean
        WarehouseService warehouseService() {
            return Mockito.mock(WarehouseService.class);
        }

        @Bean
        SupplierQualificationService supplierQualificationService() {
            return Mockito.mock(SupplierQualificationService.class);
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
