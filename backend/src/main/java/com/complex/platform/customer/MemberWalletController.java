package com.complex.platform.customer;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.customer.dto.StoredValueConsumeRequest;
import com.complex.platform.customer.dto.StoredValueSettleRequest;
import com.complex.platform.customer.dto.StoredValueTopupRequest;
import com.complex.platform.finance.PaymentIntentRecord;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员钱包（客人端 customer-token）：储值充值/入账/消费/查询。
 * 充值绑 finance 支付意图：先返回支付意图，支付成功后再调入账接口；保证不凭空加余额。
 */
@RestController
@RequestMapping("/api/v1/customer")
public class MemberWalletController {

    private final CustomerService customerService;
    private final StoredValueService storedValueService;
    private final LoyaltyService loyaltyService;
    private final CustomerRepository repository;

    public MemberWalletController(CustomerService customerService, StoredValueService storedValueService,
                                  LoyaltyService loyaltyService,
                                  CustomerRepository repository) {
        this.customerService = customerService;
        this.storedValueService = storedValueService;
        this.loyaltyService = loyaltyService;
        this.repository = repository;
    }

    // ---- 储值 ----

    @PostMapping("/stored-value/topup")
    public ApiResponse<PaymentIntentRecord> topup(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody StoredValueTopupRequest request
    ) {
        String customerId = customerService.resolveCustomerId(token(customerToken, authorization));
        return ApiResponse.ok(storedValueService.topup(customerId, request.projectId(), request.merchantId(),
                request.amountCent() == null ? 0L : request.amountCent(), request.channel(), request.idempotencyKey()));
    }

    @PostMapping("/stored-value/settle")
    public ApiResponse<CustomerStoredValueTransactionRecord> settle(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody StoredValueSettleRequest request
    ) {
        String customerId = customerService.resolveCustomerId(token(customerToken, authorization));
        return ApiResponse.ok(storedValueService.settleTopup(customerId, request.projectId(), request.merchantId(), request.paymentIntentId()));
    }

    @PostMapping("/stored-value/consume")
    public ApiResponse<CustomerStoredValueTransactionRecord> consume(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody StoredValueConsumeRequest request
    ) {
        String customerId = customerService.resolveCustomerId(token(customerToken, authorization));
        return ApiResponse.ok(storedValueService.consume(customerId, request.projectId(), request.merchantId(),
                request.amountCent() == null ? 0L : request.amountCent(), request.reason(), request.idempotencyKey()));
    }

    @GetMapping("/stored-value/account")
    public ApiResponse<CustomerStoredValueAccountRecord> getAccount(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(name = "project_id", required = false) String projectId,
            @RequestParam(name = "merchant_id", required = false) String merchantId
    ) {
        // B1d⑤：merchant_id 非空=查商户级卡；为空=查综合体通用卡。
        String customerId = customerService.resolveCustomerId(token(customerToken, authorization));
        return ApiResponse.ok(repository.findStoredValueAccount(customerId, projectId, merchantId).orElse(null));
    }

    @GetMapping("/stored-value/accounts")
    public ApiResponse<PageResponse<CustomerStoredValueAccountRecord>> listAccounts(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        // AUD-061 客人端「我的全部卡」：通用卡 + 各商户卡分别余额，供钱包分卡展示。
        String customerId = customerService.resolveCustomerId(token(customerToken, authorization));
        List<CustomerStoredValueAccountRecord> cards = repository.listStoredValueAccountsByCustomer(customerId);
        return ApiResponse.ok(new PageResponse<>(cards, 1, cards.size(), cards.size()));
    }

    // 积分转赠（会员间转移）已下线：HTML 明确「积分不支持会员间转赠」，端点/表/记录类已随 V172 清理移除。

    private String token(String customerToken, String authorization) {
        if (customerToken != null && !customerToken.isBlank()) {
            return customerToken.trim();
        }
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring("Bearer ".length()).trim();
        }
        return customerToken;
    }
}
