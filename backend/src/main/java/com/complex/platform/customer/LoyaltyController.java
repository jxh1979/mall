package com.complex.platform.customer;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.customer.dto.LoyaltyFreezeRequest;
import com.complex.platform.customer.dto.StoredValueFreezeRequest;
import com.complex.platform.customer.dto.StoredValueMerchantCardConfigRequest;
import com.complex.platform.customer.dto.StoredValueMerchantRefundRuleRequest;
import com.complex.platform.customer.dto.StoredValueRefundRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员忠诚度后台管理（staff-token + 权限闸）：等级配置查询、积分账户冻结/解冻、手动等级评估；
 * 第2部分（V090）：储值账户冻结/解冻/退卡。
 */
@RestController
@RequestMapping("/api/v1/loyalty")
public class LoyaltyController {

    private final LoyaltyService service;
    private final StoredValueService storedValueService;
    private final CustomerRepository repository;
    private final CustomerService customerService;

    public LoyaltyController(LoyaltyService service,
                             StoredValueService storedValueService, CustomerRepository repository,
                             CustomerService customerService) {
        this.service = service;
        this.storedValueService = storedValueService;
        this.repository = repository;
        this.customerService = customerService;
    }

    /** 积分虚拟券核销领取(店员端):库存足则扣库存领实物,缺货则退回积分。 */
    @PostMapping("/points-redemptions/{redemption_id}/fulfill")
    public ApiResponse<CustomerPointsRedemptionRecord> fulfillRedemption(
            @PathVariable("redemption_id") String redemptionId
    ) {
        return ApiResponse.ok(customerService.fulfillPointsRedemption(redemptionId));
    }

    @GetMapping("/member-levels")
    public ApiResponse<PageResponse<CustomerMemberLevelRecord>> listMemberLevels(
            @RequestParam(name = "project_id", required = false) String projectId
    ) {
        List<CustomerMemberLevelRecord> levels = repository.listMemberLevels(projectId);
        return ApiResponse.ok(new PageResponse<>(levels, 1, levels.size(), levels.size()));
    }

    @PostMapping("/points-accounts/{customer_id}/freeze")
    public ApiResponse<CustomerPointsAccountRecord> freeze(
            @PathVariable("customer_id") String customerId,
            @RequestBody(required = false) @jakarta.validation.Valid LoyaltyFreezeRequest request
    ) {
        return ApiResponse.ok(service.freeze(customerId, request == null ? null : request.reason()));
    }

    @PostMapping("/points-accounts/{customer_id}/unfreeze")
    public ApiResponse<CustomerPointsAccountRecord> unfreeze(
            @PathVariable("customer_id") String customerId,
            @RequestBody(required = false) @jakarta.validation.Valid LoyaltyFreezeRequest request
    ) {
        return ApiResponse.ok(service.unfreeze(customerId, request == null ? null : request.reason()));
    }

    @PostMapping("/points-accounts/{customer_id}/evaluate-level")
    public ApiResponse<CustomerPointsAccountRecord> evaluateLevel(
            @PathVariable("customer_id") String customerId
    ) {
        return ApiResponse.ok(service.evaluateLevel(customerId, Instant.now()));
    }

    // ---- 第2部分（V090）：储值账户冻结/解冻/退卡 ----

    @PostMapping("/stored-value/{customer_id}/freeze")
    public ApiResponse<CustomerStoredValueAccountRecord> freezeStoredValue(
            @PathVariable("customer_id") String customerId,
            @RequestBody(required = false) @jakarta.validation.Valid StoredValueFreezeRequest request
    ) {
        return ApiResponse.ok(storedValueService.freeze(customerId,
                request == null ? null : request.projectId(),
                request == null ? null : request.merchantId(),
                request == null ? null : request.reason()));
    }

    @PostMapping("/stored-value/{customer_id}/unfreeze")
    public ApiResponse<CustomerStoredValueAccountRecord> unfreezeStoredValue(
            @PathVariable("customer_id") String customerId,
            @RequestBody(required = false) @jakarta.validation.Valid StoredValueFreezeRequest request
    ) {
        return ApiResponse.ok(storedValueService.unfreeze(customerId,
                request == null ? null : request.projectId(),
                request == null ? null : request.merchantId(),
                request == null ? null : request.reason()));
    }

    @PostMapping("/stored-value/{customer_id}/refund")
    public ApiResponse<CustomerStoredValueTransactionRecord> refundStoredValue(
            @PathVariable("customer_id") String customerId,
            @RequestBody @jakarta.validation.Valid StoredValueRefundRequest request
    ) {
        return ApiResponse.ok(storedValueService.refund(customerId, request.projectId(), request.merchantId(),
                request.amountCent() == null ? 0L : request.amountCent(), request.reason(),
                Boolean.TRUE.equals(request.closeAccount()), request.idempotencyKey()));
    }

    // ---- B1d⑤ phase3：商户级卡退款规则配置（发卡商户自设可否退/比例）----

    @PostMapping("/stored-value/merchant-refund-rules")
    public ApiResponse<StoredValueMerchantRefundRuleRecord> configureMerchantRefundRule(
            @RequestBody @jakarta.validation.Valid StoredValueMerchantRefundRuleRequest request
    ) {
        return ApiResponse.ok(storedValueService.configureMerchantRefundRule(
                request.merchantId(),
                request.refundAllowed() == null || request.refundAllowed(),
                request.refundRatioBps() == null ? 10000 : request.refundRatioBps()));
    }

    @GetMapping("/stored-value/merchant-refund-rules/{merchant_id}")
    public ApiResponse<StoredValueMerchantRefundRuleRecord> getMerchantRefundRule(
            @PathVariable("merchant_id") String merchantId
    ) {
        return ApiResponse.ok(storedValueService.getMerchantRefundRule(merchantId));
    }

    // ---- AUD-061：商户卡平台配置（仅平台可改）：发卡资格 + 资金模式（预收/代收）----

    @PostMapping("/stored-value/merchant-card-config")
    public ApiResponse<StoredValueMerchantRefundRuleRecord> configureMerchantCard(
            @RequestBody @jakarta.validation.Valid StoredValueMerchantCardConfigRequest request
    ) {
        return ApiResponse.ok(storedValueService.configureMerchantCard(
                request.merchantId(),
                Boolean.TRUE.equals(request.cardIssuanceEnabled()),
                request.fundOwnership()));
    }

    // 积分转赠风控配置已随「积分转赠下线」移除（HTML：积分不支持会员间转赠）。
}
