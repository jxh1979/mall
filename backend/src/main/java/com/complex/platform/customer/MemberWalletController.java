package com.complex.platform.customer;

// 文件版本：V0.04

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.customer.dto.StoredValueSettleRequest;
import com.complex.platform.customer.dto.StoredValueTopupRequest;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
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

    private static final AuditedOperation INITIATE_VOUCHER_TRANSFER = AuditedOperation.of(
            "voucher_transfer.created", "voucher_transfer.create_failed", "not_applicable", "voucher_transfer");
    private static final AuditedOperation CLAIM_VOUCHER_TRANSFER = AuditedOperation.of(
            "voucher_transfer.claimed", "voucher_transfer.claim_failed", "not_applicable", "voucher_transfer");

    private final CustomerService customerService;
    private final StoredValueService storedValueService;
    private final LoyaltyService loyaltyService;
    private final CustomerRepository repository;
    private final VoucherService voucherService;
    private final VoucherTransferService voucherTransferService;
    private final AuditedOperationExecutor operations;
    // PGM 第 744 行「前端隐藏只是辅助，后端接口必须独立校验」：按卡、订单、券所属综合体在服务端判（不取 X-Project-Id），
    // 在本客人入口判、早于 @GuardedAction 切面与幂等；储值充值、入账、储值付票另按该动作的模块能力码（与 @GuardedAction 同码）
    // 在同一综合体判（PGM 第 736 行按模块开关）。StoredValueService、VoucherService 等共用方法不动。
    private final CustomerProjectSwitch projectSwitch;

    public MemberWalletController(CustomerService customerService, StoredValueService storedValueService,
                                  LoyaltyService loyaltyService,
                                  CustomerRepository repository,
                                  VoucherService voucherService,
                                  VoucherTransferService voucherTransferService,
                                  AuditedOperationExecutor operations,
                                  CustomerProjectSwitch projectSwitch) {
        this.customerService = customerService;
        this.storedValueService = storedValueService;
        this.loyaltyService = loyaltyService;
        this.repository = repository;
        this.voucherService = voucherService;
        this.voucherTransferService = voucherTransferService;
        this.operations = operations;
        this.projectSwitch = projectSwitch;
    }

    // ---- 储值 ----

    /** ROOT-03-R17/R20：可只付一部分（amount_cent），付足即同事务出票；响应为订单视图（已付、待付、已收各笔）。 */
    @PostMapping("/orders/{order_id}/stored-value/pay")
    public ApiResponse<com.complex.platform.customer.dto.CustomerTicketOrderResponse> payTicketOrder(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable("order_id") String orderId,
            @Valid @RequestBody com.complex.platform.customer.dto.StoredValueConsumeRequest request) {
        String customerId = customerService.resolveCustomerId(token(customerToken, authorization));
        projectSwitch.ticketOrder(orderId, customerId, "customer.stored_value.consume");
        return ApiResponse.ok(customerService.orderView(storedValueService.payTicketOrder(customerId, orderId,
                request.accountId(), request.phone(), request.smsCode(), request.amountCent(), request.idempotencyKey())));
    }

    @PostMapping("/stored-value/sms")
    public ApiResponse<java.util.Map<String, String>> requestStoredValueCode(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody java.util.Map<String, String> request) {
        String customerId = customerService.resolveCustomerId(token(customerToken, authorization));
        customerService.requestStoredValueCode(customerId, request.get("phone"));
        return ApiResponse.ok(java.util.Map.of("message", "验证码已发送"));
    }

    @PostMapping("/stored-value/topup")
    public ApiResponse<PaymentIntentRecord> topup(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody StoredValueTopupRequest request
    ) {
        String customerId = customerService.resolveCustomerId(token(customerToken, authorization));
        projectSwitch.card(request.projectId(), request.merchantId(), "customer.stored_value.topup");
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
        // 按充值时服务端绑定的卡范围判；settleTopupFromGateway 作业不经此入口。
        projectSwitch.topupIntent(request.paymentIntentId(), customerId, "customer.stored_value.topup");
        return ApiResponse.ok(storedValueService.settleTopup(customerId, request.projectId(), request.merchantId(), request.paymentIntentId()));
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
        projectSwitch.card(projectId, merchantId);
        return ApiResponse.ok(repository.findStoredValueAccount(customerId, projectId, merchantId).orElse(null));
    }

    @GetMapping("/stored-value/accounts/{account_id}/transactions")
    public ApiResponse<List<CustomerStoredValueTransactionView>> transactions(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable("account_id") String accountId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        String customerId = customerService.resolveCustomerId(token(customerToken, authorization));
        if (page < 1 || size < 1 || size > 100) {
            throw com.complex.platform.common.api.ApiException.badRequest("VALIDATION_FAILED", "分页参数无效");
        }
        projectSwitch.storedValueAccount(accountId, customerId);
        if (repository.listStoredValueAccountsByCustomer(customerId).stream()
                .noneMatch(account -> accountId.equals(account.id()))) {
            throw com.complex.platform.common.api.ApiException.notFound(
                    "STORED_VALUE_ACCOUNT_NOT_FOUND", "储值卡不存在");
        }
        return ApiResponse.ok(repository.listStoredValueTransactionViews(customerId, accountId, page, size));
    }

    /** ROOT-03-R30（ENUM 第 86 行）：充值前告知扣款顺序、赠送金性质与退卡口径；merchant_id 为空＝综合体通用卡。 */
    @GetMapping("/stored-value/topup-rules")
    public ApiResponse<CustomerStoredValueTopupRulesResponse> topupRules(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(name = "project_id", required = false) String projectId,
            @RequestParam(name = "merchant_id", required = false) String merchantId) {
        customerService.resolveCustomerId(token(customerToken, authorization));
        projectSwitch.card(projectId, merchantId);
        return ApiResponse.ok(storedValueService.topupRules(projectId, merchantId));
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

    // 券「使用」：ROOT-03-R32（BSM 第 585 行）客人出示按全场统一间隔刷新的动态券码（下方 redeem-code），店员/核销点扫码走既有
    //   staff 核销（POST /api/v1/vouchers/redemptions，需 customer.voucher.redeem 权限）；固定券码不再下发、也不再能核销。
    @GetMapping("/vouchers/{voucher_instance_id}/redeem-code")
    public ApiResponse<com.complex.platform.voucher.VoucherRedeemCodeResponse> getVoucherRedeemCode(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable("voucher_instance_id") String voucherInstanceId) {
        String customerId = customerService.resolveCustomerId(token(customerToken, authorization));
        // 商户券按发券商户的综合体判；平台通用券不属单一综合体，不判。
        projectSwitch.voucher(voucherInstanceId);
        return ApiResponse.ok(voucherService.issueCustomerRedeemCode(voucherInstanceId, customerId));
    }

    // 发起券转赠（channel=phone 需 receiverPhone；channel=link 服务端生成一次性领取码）。
    // 幂等照门票转赠（TicketingService#initiateTicketTransfer）：必填键＋请求指纹＋响应精确重放，operationId 绑定客人——网络中断后沿用原键重提
    // 只回放第一次的结果，不会按裁决第十三批第 9 条「再次转赠作废前一笔」把第一笔作废；成功/失败按 OpenAPI 记 voucher_transfer.created / create_failed。
    @PostMapping("/voucher-transfers")
    public ApiResponse<VoucherTransferResponse> initiateVoucherTransfer(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody CustomerVoucherTransferRequest request
    ) {
        CustomerRecord customer = customerService.requireCustomerRecord(token(customerToken, authorization));
        projectSwitch.voucher(request.voucherInstanceId());
        return ApiResponse.ok(operations.executeWrite("initiateVoucherTransfer:" + customer.id(), request.idempotencyKey(), request,
                VoucherTransferResponse.class, INITIATE_VOUCHER_TRANSFER,
                () -> voucherTransferService.initiate(customer.id(), customer.phone(), request),
                MemberWalletController::transferBusinessId, java.util.Map.of()));
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
        projectSwitch.voucherTransfer(transferId);
        // 幂等照门票转赠领取（operationId 绑定客人＋转赠单）；成功/失败记 voucher_transfer.claimed / claim_failed。
        return ApiResponse.ok(operations.executeWrite("claimVoucherTransfer:" + customer.id() + ":" + transferId,
                request == null ? null : request.idempotencyKey(), request, VoucherTransferResponse.class, CLAIM_VOUCHER_TRANSFER,
                () -> voucherTransferService.claim(customer.id(), customer.phone(), transferId, claimCode),
                MemberWalletController::transferBusinessId, java.util.Map.of()));
    }

    private static String transferBusinessId(Object saved) {
        return ((VoucherTransferResponse) saved).id();
    }

    // 出让人撤回。
    @PostMapping("/voucher-transfers/{transfer_id}/revoke")
    public ApiResponse<VoucherTransferResponse> revokeVoucherTransfer(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable("transfer_id") String transferId
    ) {
        CustomerRecord customer = customerService.requireCustomerRecord(token(customerToken, authorization));
        projectSwitch.voucherTransfer(transferId);
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
        projectSwitch.voucherTransferByClaimCode(claimCode);
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
