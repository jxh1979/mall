// 文件版本：V0.09
package com.complex.platform.inventory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
        when(service.listSavedBottles(any(String.class))).thenReturn(new PageResponse<>(List.of(), 1, 50, 0));
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
        // FS-016：转让 / 发码 / 人工例外四步。
        when(service.transferSavedBottle(eq("saved_1"), any(com.complex.platform.inventory.dto.SavedBottleTransferRequest.class)))
                .thenReturn(savedBottleTransfer());
        when(service.requestSavedBottleVerificationCode(eq("saved_1"), any(com.complex.platform.inventory.dto.SavedBottleVerificationCodeRequest.class)))
                .thenReturn(new SavedBottleVerificationCodeReceipt("saved_1", "transfer", Instant.parse("2026-06-14T00:00:00Z")));
        when(service.renewSavedBottle(eq("saved_1"), any(com.complex.platform.inventory.dto.SavedBottleRenewalRequest.class)))
                .thenReturn(renewal("pending_review"));
        when(service.reviewSavedBottleRenewal(eq("rnw_1"), any(com.complex.platform.inventory.dto.SavedBottleRenewalRequest.class)))
                .thenReturn(renewal("effective"));
        when(service.listSavedBottleRenewals(eq("saved_1")))
                .thenReturn(new PageResponse<>(List.of(renewal("effective")), 1, 50, 1));
        when(service.initiateManualPickup(eq("saved_1"), any(com.complex.platform.inventory.dto.SavedBottleManualPickupRequest.class)))
                .thenReturn(manualPickup("initiated"));
        when(service.reviewManualPickup(eq("mpk_1"), any(com.complex.platform.inventory.dto.SavedBottleManualPickupRequest.class)))
                .thenReturn(manualPickup("reviewed"));
        when(service.confirmManualPickupMarketing(eq("mpk_1"), any(com.complex.platform.inventory.dto.SavedBottleManualPickupRequest.class)))
                .thenReturn(manualPickup("marketing_confirmed"));
        when(service.dispenseManualPickup(eq("mpk_1"), any(com.complex.platform.inventory.dto.SavedBottleManualPickupDispenseRequest.class)))
                .thenReturn(manualPickup("dispensed"));
        // FS-017：补存/赔付、退出清点通知、交接。
        when(service.registerSavedBottleRemedy(eq("saved_1"), any(com.complex.platform.inventory.dto.SavedBottleRemedyRequest.class)))
                .thenReturn(remedy("pending_approval"));
        when(service.listSavedBottleRemedies(eq("saved_1"))).thenReturn(new PageResponse<>(List.of(remedy("closed")), 1, 50, 1));
        when(service.approveSavedBottleRemedy(eq("sbr_1"), any(com.complex.platform.inventory.dto.SavedBottleRemedyActionRequest.class)))
                .thenReturn(remedy("approved"));
        when(service.settleSavedBottleRemedy(eq("sbr_1"), any(com.complex.platform.inventory.dto.SavedBottleRemedyActionRequest.class)))
                .thenReturn(remedy("closed"));
        when(service.advanceSavedBottleRemedyByPlatform(eq("sbr_1"), any(com.complex.platform.inventory.dto.SavedBottleRemedyActionRequest.class)))
                .thenReturn(remedy("pending_approval"));
        when(service.assignSavedBottleRemedyStaffLiability(eq("sbr_1"), any(com.complex.platform.inventory.dto.SavedBottleRemedyActionRequest.class)))
                .thenReturn(remedy("closed"));
        when(service.settleSavedBottleRemedyRecovery(eq("sbr_1"), any(com.complex.platform.inventory.dto.SavedBottleRemedyActionRequest.class)))
                .thenReturn(remedy("closed"));
        when(service.noticeSavedBottleExit(any(com.complex.platform.inventory.dto.SavedBottleExitNoticeRequest.class)))
                .thenReturn(new SavedBottleExitNoticeResult("m_1", 2, 2, Map.of("item_1", 3)));
        when(service.handoverSavedBottlesToPlatform(any(com.complex.platform.inventory.dto.SavedBottleHandoverRequest.class)))
                .thenReturn(handover());
        when(service.listSavedBottleHandovers()).thenReturn(new PageResponse<>(List.of(handover()), 1, 50, 1));
        when(service.createStockAdjustment(any(StockAdjustmentRequest.class))).thenReturn(stockAdjustment());
        when(service.createStockBatch(any(InventoryStockBatchRequest.class))).thenReturn(stockBatch());
        when(service.runInventoryRiskScan(any(InventoryRiskScanRequest.class))).thenReturn(riskScanResult());
        when(service.createSupplierReconciliation(any(SupplierReconciliationRequest.class))).thenReturn(supplierReconciliation());
        when(service.createStockIn(any(StockInRequest.class))).thenReturn(stockMovement("inbound"));
        when(service.createStockMovement(any(StockMovementRequest.class))).thenReturn(stockMovement("outbound"));
        when(service.transferStockMovement(any(StockMovementRequest.class))).thenReturn(stockMovement("transfer"));
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
            "POST,/api/v1/inventory/saved-bottles/lookup,bar.saved_bottle.manage",
            "POST,/api/v1/inventory/saved-bottles/saved_1/transfer,bar.saved_bottle.manage",
            "POST,/api/v1/inventory/saved-bottles/saved_1/verification-code,bar.saved_bottle.manage",
            "POST,/api/v1/inventory/saved-bottles/saved_1/manual-pickups,bar.saved_bottle.manual_pickup.initiate",
            "POST,/api/v1/inventory/saved-bottles/manual-pickups/mpk_1/review,bar.saved_bottle.manual_pickup.review",
            "POST,/api/v1/inventory/saved-bottles/manual-pickups/mpk_1/confirm-marketing,bar.saved_bottle.manual_pickup.confirm_marketing",
            "POST,/api/v1/inventory/saved-bottles/manual-pickups/mpk_1/dispense,bar.saved_bottle.manage",
            "POST,/api/v1/inventory/saved-bottles/saved_1/renewals,bar.saved_bottle.manage",
            "POST,/api/v1/inventory/saved-bottles/renewals/rnw_1/review,bar.saved_bottle.renewal.review",
            "GET,/api/v1/inventory/saved-bottles/saved_1/renewals,bar.saved_bottle.manage",
            "PUT,/api/v1/inventory/saved-bottles/manual-pickup-policy,bar.saved_bottle.manual_pickup_policy.manage",
            "PUT,/api/v1/inventory/saved-bottles/renewal-policy,bar.saved_bottle.renewal_policy.manage",
            "PUT,/api/v1/inventory/saved-bottles/reminder-policy,bar.saved_bottle.reminder_policy.manage",
            "GET,/api/v1/inventory/saved-bottles/reminder-policy,bar.saved_bottle.reminder_policy.manage",
            "POST,/api/v1/inventory/saved-bottles/saved_1/remedy,bar.saved_bottle.manage",
            "GET,/api/v1/inventory/saved-bottles/saved_1/remedies,bar.saved_bottle.manage",
            "POST,/api/v1/inventory/saved-bottles/remedies/sbr_1/approve,inventory.high_value.review",
            "POST,/api/v1/inventory/saved-bottles/remedies/sbr_1/settle,bar.saved_bottle.manage",
            "POST,/api/v1/inventory/saved-bottles/remedies/sbr_1/advance,bar.saved_bottle.manage",
            "POST,/api/v1/inventory/saved-bottles/remedies/sbr_1/staff-liability,bar.saved_bottle.manage",
            "POST,/api/v1/inventory/saved-bottles/remedies/sbr_1/recovery/settle,bar.saved_bottle.manage",
            "POST,/api/v1/inventory/saved-bottles/exit-notice,bar.saved_bottle.manage",
            "POST,/api/v1/inventory/saved-bottles/handover,bar.saved_bottle.manage",
            "GET,/api/v1/inventory/saved-bottles/handovers,bar.saved_bottle.manage",
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
        } else if ("PUT".equals(method)) {
            mockMvc.perform(put(path)
                            .with(user("ops").roles("P0_SMOKE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyFor(path)))
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
            "/api/v1/inventory/saved-bottles/saved_1/transfer,'{\"to_name_or_nickname\":\"乙\",\"to_phone\":\"13900139000\",\"verification_code\":\"000000\"}'",
            "/api/v1/inventory/saved-bottles/saved_1/renewals,'{\"verification_code\":\"000000\"}'",
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

    // FS-018 R1：POST /stock-movements/damage（前端真实蛇形请求体）转入 stock-adjustments 同一实现：
    // adjustment_type=damage、reason 取 operator_note、库位取 from_location_id、照片/视频证据透传，不再走直接扣减的 createTypedStockMovement。
    @Test
    void typedDamageRouteDelegatesToStockAdjustment() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/stock-movements/damage")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key":"typed-damage-1","operator_note":"碰落破损","from_location_id":"wh_1",
                                 "items":[{"item_id":"item_1","quantity":1}],
                                 "photo_attachment_ids":["att_p"],"video_attachment_ids":["att_v"]}
                                """))
                .andExpect(status().isOk());

        verify(service).createStockAdjustment(argThat(a -> "damage".equals(a.adjustmentType())
                && "碰落破损".equals(a.reason())
                && "wh_1".equals(a.items().get(0).get("location_id"))
                && List.of("att_p").equals(a.photoAttachmentIds())
                && List.of("att_v").equals(a.videoAttachmentIds())));
        verify(service, never()).createTypedStockMovement(eq("damage"), any());
    }

    // BSM L1072：存酒到期提醒节点配置按前端真实蛇形请求体落到服务层（scope_id / reminder_days）。
    @Test
    void configureReminderPolicyUsesSnakeCaseRequest() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/saved-bottles/reminder-policy")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key":"rmp-snake","scope_id":"project:project_1","reminder_days":[14,7,3,1]}
                                """))
                .andExpect(status().isOk());

        verify(service).configureReminderPolicy(argThat(r -> "rmp-snake".equals(r.idempotencyKey())
                && "project:project_1".equals(r.scopeId()) && List.of(14, 7, 3, 1).equals(r.reminderDays())));
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
        if (path.endsWith("/remedy")) {
            return """
                    {"idempotency_key":"remedy-register","remedy_type":"compensation","amount_cent":12000,"reason":"无法替代"}
                    """;
        }
        if (path.contains("/remedies/") && path.endsWith("/approve")) {
            return """
                    {"idempotency_key":"remedy-approve","role":"supervisor","reason":"同意"}
                    """;
        }
        if (path.contains("/remedies/") && path.endsWith("/staff-liability")) {
            return """
                    {"idempotency_key":"remedy-staff","staff_user_id":"usr_staff_x","reason":"复核认定"}
                    """;
        }
        if (path.contains("/remedies/")) {
            return """
                    {"idempotency_key":"remedy-action","evidence_ids":["att-1"]}
                    """;
        }
        if (path.endsWith("saved-bottles/exit-notice")) {
            return """
                    {"idempotency_key":"exit-notice","merchant_id":"m_1"}
                    """;
        }
        if (path.endsWith("saved-bottles/handover")) {
            return """
                    {"idempotency_key":"handover","merchant_id":"m_1","pickup_location":"平台仓 A 区","items":[{"saved_bottle_id":"saved_1","received_quantity":1,"condition":"intact"}]}
                    """;
        }
        if (path.endsWith("saved-bottles/lookup")) {
            return """
                    {"phone":"13800138000"}
                    """;
        }
        if (path.contains("saved-bottles/") && path.endsWith("/transfer")) {
            return """
                    {"idempotency_key":"transfer-1","to_name_or_nickname":"乙","to_phone":"13900139000","verification_code":"000000"}
                    """;
        }
        if (path.contains("saved-bottles/") && path.endsWith("/verification-code")) {
            return """
                    {"idempotency_key":"vc-1","scene":"transfer"}
                    """;
        }
        if (path.endsWith("manual-pickup-policy")) {
            return """
                    {"idempotency_key":"mpp-1","project_id":"project_1","orders_within1m":2,"orders_within3m":3,"orders_total":5,"min_orders":1,"max_orders":10,"reason":"首次配置"}
                    """;
        }
        if (path.endsWith("reminder-policy")) {
            return """
                    {"idempotency_key":"rmp-1","scope_id":"project:project_1","reminder_days":[14,7,3,1]}
                    """;
        }
        if (path.endsWith("renewal-policy")) {
            return """
                    {"idempotency_key":"rnp-1","policy_key":"max_renewals","scope_id":"global","value":2,"min_value":1,"max_value":5}
                    """;
        }
        if (path.contains("renewals") && path.endsWith("/review")) {
            return """
                    {"idempotency_key":"rn-rv-1","decision":"approve"}
                    """;
        }
        if (path.contains("renewals")) {
            return """
                    {"idempotency_key":"rn-1","verification_code":"000000"}
                    """;
        }
        if (path.contains("manual-pickups") && path.endsWith("/dispense")) {
            return """
                    {"idempotency_key":"mp-4","claim_items":[{"quantity":1}],"present_in_person":true}
                    """;
        }
        if (path.contains("manual-pickups")) {
            return """
                    {"idempotency_key":"mp-1","reason":"客人手机遗失"}
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

    private SavedBottleTransferRecord savedBottleTransfer() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new SavedBottleTransferRecord("transfer_1", "saved_1", "cust_1", "甲", "乙", "13900139000", "hash",
                2, "usr_admin", "transferred", List.of(), now, "web", "hash0", null);
    }

    private SavedBottleRenewalRecord renewal(String status) {
        Instant now = Instant.parse("2026-06-15T00:00:00Z");
        return new SavedBottleRenewalRecord("rnw_1", "saved_1", "m_1", "project_1", "cust_1", "hash", 30,
                now.plusSeconds(86400), now.plusSeconds(31 * 86400L), 1, 2, "global:v1", "global:v1", "usr_admin", "web",
                "sms", null, null, null, status, now, now);
    }

    // FS-025：员工代办转存按前端真实请求形状（adminClient.mapKeys 蛇形键）；响应不外泄手机哈希。
    @Test
    void renewSavedBottleUsesSnakeCaseRequestAndResponse() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/saved-bottles/saved_1/renewals")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "admin-saved-bottle-renew-saved_1",
                                  "operator_note": "客人到店代办转存",
                                  "verification_code": "000000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("rnw_1"))
                .andExpect(jsonPath("$.data.status").value("pending_review"))
                .andExpect(jsonPath("$.data.extension_days").value(30))
                .andExpect(jsonPath("$.data.renewal_count").value(1))
                .andExpect(jsonPath("$.data.new_expire_at").exists())
                .andExpect(jsonPath("$.data.phone_hash").doesNotExist());

        org.mockito.ArgumentCaptor<com.complex.platform.inventory.dto.SavedBottleRenewalRequest> captor =
                org.mockito.ArgumentCaptor.forClass(com.complex.platform.inventory.dto.SavedBottleRenewalRequest.class);
        verify(service).renewSavedBottle(eq("saved_1"), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().verificationCode()).isEqualTo("000000");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().operatorNote()).isEqualTo("客人到店代办转存");
        verify(authorizationService).requirePermission("bar.saved_bottle.manage");
    }

    private static SavedBottleRemedyRecord remedy(String status) {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new SavedBottleRemedyRecord("sbr_1", "saved_1", "m_1", "project_1", "compensation", null, 12000L, null, null,
                "original_merchant", "m_1", null, null, null, null, status, null, List.of(), "无法替代", "none", null, null, "none",
                null, null, null, List.of(), "usr_admin", now, now);
    }

    private static SavedBottleHandoverRecord handover() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new SavedBottleHandoverRecord("sbh_1", "m_1", "project_1", "ent_platform", "平台仓 A 区", 1, 1, 0, List.of(), List.of(),
                List.of(new SavedBottleHandoverRecord.SavedBottleHandoverItemRecord("saved_1", 1, "intact", 0)), "usr_admin", now);
    }

    // FS-017：补存/赔付登记与交接按前端真实请求形状（adminClient.mapKeys 蛇形键，含嵌套 items）；响应不外泄 merchant_id。
    @Test
    void savedBottleRemedyAndHandoverUseSnakeCaseRequestShape() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/saved-bottles/saved_1/remedy")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "admin-saved-bottle-remedy-saved_1",
                                  "operator_note": "无法同品替代",
                                  "remedy_type": "compensation",
                                  "amount_cent": 12000,
                                  "reason": "无法替代"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("sbr_1"))
                .andExpect(jsonPath("$.data.remedy_type").value("compensation"))
                .andExpect(jsonPath("$.data.customer_delivery_status").value("pending_approval"))
                .andExpect(jsonPath("$.data.recovery_status").value("none"))
                .andExpect(jsonPath("$.data.merchant_id").doesNotExist());
        org.mockito.ArgumentCaptor<com.complex.platform.inventory.dto.SavedBottleRemedyRequest> captor =
                org.mockito.ArgumentCaptor.forClass(com.complex.platform.inventory.dto.SavedBottleRemedyRequest.class);
        verify(service).registerSavedBottleRemedy(eq("saved_1"), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().remedyType()).isEqualTo("compensation");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().amountCent()).isEqualTo(12000L);

        mockMvc.perform(post("/api/v1/inventory/saved-bottles/handover")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "admin-saved-bottle-handover-m_1",
                                  "merchant_id": "m_1",
                                  "pickup_location": "平台仓 A 区",
                                  "photo_attachment_ids": ["att_photo"],
                                  "video_attachment_ids": ["att_video"],
                                  "items": [{"saved_bottle_id": "saved_1", "received_quantity": 1, "condition": "intact"}]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("sbh_1"))
                .andExpect(jsonPath("$.data.pickup_location").value("平台仓 A 区"))
                .andExpect(jsonPath("$.data.items[0].condition").value("intact"))
                .andExpect(jsonPath("$.data.merchant_id").doesNotExist());
        org.mockito.ArgumentCaptor<com.complex.platform.inventory.dto.SavedBottleHandoverRequest> handoverCaptor =
                org.mockito.ArgumentCaptor.forClass(com.complex.platform.inventory.dto.SavedBottleHandoverRequest.class);
        verify(service).handoverSavedBottlesToPlatform(handoverCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(handoverCaptor.getValue().merchantId()).isEqualTo("m_1");
        org.assertj.core.api.Assertions.assertThat(handoverCaptor.getValue().items().getFirst().get("saved_bottle_id")).isEqualTo("saved_1");
        org.assertj.core.api.Assertions.assertThat(handoverCaptor.getValue().items().getFirst().get("condition")).isEqualTo("intact");

        // 财务审批 role=finance 核既有财务码（supervisor 走参数化映射表）。
        mockMvc.perform(post("/api/v1/inventory/saved-bottles/remedies/sbr_1/approve")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key": "admin-remedy-approve-fin", "role": "finance", "reason": "同意赔付"}
                                """))
                .andExpect(status().isOk());
        verify(authorizationService).requirePermission("finance.ar_ap.manage");
    }

    private SavedBottleManualPickupRecord manualPickup(String status) {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new SavedBottleManualPickupRecord("mpk_1", "saved_1", null, "project_1", "cust_1", "hash", "手机遗失",
                "orders_total", "usr_manager", now, null, null, null, null, null, null, null, null, status, List.of(), now, now);
    }

    // FS-016：转让按前端真实请求形状（adminClient.mapKeys 蛇形键）。
    @Test
    void transferSavedBottleUsesSnakeCaseRequestAndResponse() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/saved-bottles/saved_1/transfer")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "admin-saved-bottle-transfer-saved_1",
                                  "operator_note": "后台转让",
                                  "evidence_attachment_ids": ["att-1"],
                                  "to_name_or_nickname": "乙",
                                  "to_phone": "13900139000",
                                  "verification_code": "000000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("transfer_1"))
                .andExpect(jsonPath("$.data.to_name_or_nickname").value("乙"))
                .andExpect(jsonPath("$.data.operation_source").value("web"))
                .andExpect(jsonPath("$.data.to_phone").doesNotExist());

        org.mockito.ArgumentCaptor<com.complex.platform.inventory.dto.SavedBottleTransferRequest> captor =
                org.mockito.ArgumentCaptor.forClass(com.complex.platform.inventory.dto.SavedBottleTransferRequest.class);
        verify(service).transferSavedBottle(eq("saved_1"), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().toNameOrNickname()).isEqualTo("乙");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().toPhone()).isEqualTo("13900139000");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().verificationCode()).isEqualTo("000000");
        verify(authorizationService).requirePermission("bar.saved_bottle.manage");
    }

    // FS-016：领取按前端真实请求形状（claim_items/claim_by_phone/verification_code/bottle_label_value/customer_name_or_nickname）。
    @Test
    void claimSavedBottleUsesSnakeCaseRequestShape() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/saved-bottles/saved_1/claim")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "admin-saved-bottle-claim-saved_1-1",
                                  "claim_items": [{"saved_bottle_id": "saved_1", "quantity": 1}],
                                  "claim_by_phone": "13800138000",
                                  "verification_code": "000000",
                                  "bottle_label_value": "LBL-1",
                                  "customer_name_or_nickname": "甲",
                                  "evidence_attachment_ids": ["att-1"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("claim_1"));
        org.mockito.ArgumentCaptor<SavedBottleClaimRequest> captor = org.mockito.ArgumentCaptor.forClass(SavedBottleClaimRequest.class);
        verify(service).claimSavedBottle(eq("saved_1"), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().verificationCode()).isEqualTo("000000");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().bottleLabelValue()).isEqualTo("LBL-1");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().customerNameOrNickname()).isEqualTo("甲");
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
        InventoryTransferFlowService inventoryTransferFlowService() {
            return Mockito.mock(InventoryTransferFlowService.class);
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
