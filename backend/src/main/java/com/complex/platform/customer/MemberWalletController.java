package com.complex.platform.customer;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.customer.dto.StoredValueConsumeRequest;
import com.complex.platform.customer.dto.StoredValueSettleRequest;
import com.complex.platform.customer.dto.StoredValueTopupRequest;
import com.complex.platform.finance.PaymentIntentRecord;
import com.complex.platform.voucher.VoucherOverviewRecord;
import com.complex.platform.voucher.VoucherService;
import com.complex.platform.voucher.VoucherTransferResponse;
import com.complex.platform.voucher.VoucherTransferService;
import com.complex.platform.voucher.dto.CustomerVoucherTransferClaimRequest;
import com.complex.platform.voucher.dto.CustomerVoucherTransferRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final VoucherService voucherService;
    private final VoucherTransferService voucherTransferService;

    public MemberWalletController(CustomerService customerService, StoredValueService storedValueService,
                                  LoyaltyService loyaltyService,
                                  CustomerRepository repository,
                                  VoucherService voucherService,
                                  VoucherTransferService voucherTransferService) {
        this.customerService = customerService;
        this.storedValueService = storedValueService;
        this.loyaltyService = loyaltyService;
        this.repository = repository;
        this.voucherService = voucherService;
        this.voucherTransferService = voucherTransferService;
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

    // 我的券包（客人端只读）：积分兑换券/营销赠券/通票第三方券聚合，按 token 强制本人 customer_id（不可查他人）。
    // 内部结算价客人天然不可见（VoucherOverviewRecord 不含成本字段）；核销在 POS/工作手机侧，本端不发起核销。
    @GetMapping("/vouchers")
    public ApiResponse<PageResponse<VoucherOverviewRecord>> listMyVouchers(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "50") int pageSize
    ) {
        String customerId = customerService.resolveCustomerId(token(customerToken, authorization));
        return ApiResponse.ok(voucherService.customerOverview(customerId, page, pageSize));
    }

    // 说明：积分本身仍不支持会员间转赠（HTML「积分不支持会员间转赠」，V172 下线）。以下为「积分兑换出的券实例」
    //   （统一台账，积分已消耗）的会员间转赠，2026-07-19 用户拍板新增，范式对齐门票转赠/存酒转让（出让人发起冻结
    //   → 接收方绑定 → 领取 CAS 改归属 / 撤回，双花窗口）。🔴 对客上线前须经用户确认并同步业务 HTML 增补。

    // 券「使用」：客人端出示的核销码即 /vouchers 聚合里的 code 字段（本人可用券），由店员/核销点扫码走既有
    //   staff 核销（POST /api/v1/vouchers/redeems，需 customer.voucher.redeem 权限）；客人端不独立发起核销，
    //   故「使用」无独立后端端点，仅前端出示 code。

    // 发起券转赠（channel=phone 需 receiverPhone；channel=link 服务端生成一次性领取码）。
    @PostMapping("/voucher-transfers")
    public ApiResponse<VoucherTransferResponse> initiateVoucherTransfer(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody CustomerVoucherTransferRequest request
    ) {
        CustomerRecord customer = customerService.requireCustomerRecord(token(customerToken, authorization));
        return ApiResponse.ok(voucherTransferService.initiate(customer.id(), customer.phone(), request));
    }

    // 确认领取（link 渠道带 claimCode；phone 渠道校验登录手机号=受赠手机号）。
    @PostMapping("/voucher-transfers/{transfer_id}/claim")
    public ApiResponse<VoucherTransferResponse> claimVoucherTransfer(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable("transfer_id") String transferId,
            @RequestBody(required = false) @Valid CustomerVoucherTransferClaimRequest request
    ) {
        CustomerRecord customer = customerService.requireCustomerRecord(token(customerToken, authorization));
        String claimCode = request == null ? null : request.claimCode();
        return ApiResponse.ok(voucherTransferService.claim(customer.id(), customer.phone(), transferId, claimCode));
    }

    // 出让人撤回。
    @PostMapping("/voucher-transfers/{transfer_id}/revoke")
    public ApiResponse<VoucherTransferResponse> revokeVoucherTransfer(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable("transfer_id") String transferId
    ) {
        CustomerRecord customer = customerService.requireCustomerRecord(token(customerToken, authorization));
        return ApiResponse.ok(voucherTransferService.revoke(customer.id(), transferId));
    }

    // 领取预览（凭领取码，不算领取）。
    @GetMapping("/voucher-transfers/preview")
    public ApiResponse<VoucherTransferResponse> previewVoucherTransfer(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam("claim_code") String claimCode
    ) {
        customerService.requireCustomerRecord(token(customerToken, authorization));
        return ApiResponse.ok(voucherTransferService.preview(claimCode));
    }

    // 我的待领取（手机号渠道）。
    @GetMapping("/voucher-transfers/incoming")
    public ApiResponse<List<VoucherTransferResponse>> listIncomingVoucherTransfers(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        CustomerRecord customer = customerService.requireCustomerRecord(token(customerToken, authorization));
        return ApiResponse.ok(voucherTransferService.listIncoming(customer.phone()));
    }

    // 我发起的转赠。
    @GetMapping("/voucher-transfers/outgoing")
    public ApiResponse<List<VoucherTransferResponse>> listOutgoingVoucherTransfers(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        CustomerRecord customer = customerService.requireCustomerRecord(token(customerToken, authorization));
        return ApiResponse.ok(voucherTransferService.listOutgoing(customer.id()));
    }

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
