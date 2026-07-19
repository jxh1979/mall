package com.complex.platform.merchantexpense;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.merchantexpense.dto.MerchantExpenseEntryRequest;
import com.complex.platform.merchantexpense.dto.MerchantExpenseReversalRequest;
import com.complex.platform.merchantexpense.dto.MerchantExpenseSyncRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 商户费用归集台账端点（分红净利润费用侧）。权限：view=只读，manage=录入/冲销/自动挂接。 */
@RestController
@RequestMapping("/api/v1/finance/merchant-expenses")
public class MerchantExpenseController {

    private final MerchantExpenseService service;

    public MerchantExpenseController(MerchantExpenseService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<MerchantExpenseEntryRecord>> listEntries(
            @RequestParam(value = "merchant_id", required = false) String merchantId,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "expense_type", required = false) String expenseType,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "50") int pageSize) {
        return ApiResponse.ok(service.listEntries(merchantId, period, expenseType, source, page, pageSize));
    }

    @GetMapping("/summary")
    public ApiResponse<List<MerchantExpenseSummaryRecord>> summarize(
            @RequestParam("period") String period,
            @RequestParam(value = "merchant_id", required = false) String merchantId) {
        return ApiResponse.ok(service.summarize(period, merchantId));
    }

    @GetMapping("/period-locks")
    public ApiResponse<List<String>> periodLocks(@RequestParam("period") String period) {
        return ApiResponse.ok(service.listLockedMerchants(period));
    }

    @PostMapping
    public ApiResponse<MerchantExpenseEntryRecord> createManual(
            @RequestBody MerchantExpenseEntryRequest request) {
        return ApiResponse.ok(service.createManual(request));
    }

    @PostMapping("/{id}/reverse")
    public ApiResponse<MerchantExpenseEntryRecord> reverse(
            @PathVariable("id") String entryId,
            @RequestBody MerchantExpenseReversalRequest request) {
        return ApiResponse.ok(service.reverse(entryId, request));
    }

    @PostMapping("/sync-auto")
    public ApiResponse<MerchantExpenseSyncResultRecord> syncAuto(
            @RequestBody MerchantExpenseSyncRequest request) {
        return ApiResponse.ok(service.syncAuto(request));
    }
}
