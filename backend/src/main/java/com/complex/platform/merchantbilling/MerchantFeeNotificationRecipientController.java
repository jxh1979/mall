package com.complex.platform.merchantbilling;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.PhoneMask;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.merchantbilling.dto.MerchantFeeNotificationRecipientRequest;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商户费用通知接收人(V182)。两条口径(商户专区一期切片5):
 * 平台口径 = merchant.billing.manage,按请求指定任意商户(既有行为不变);
 * 商户自助口径 = merchant.portal.fee_recipient.manage + merchant scope,
 * 强制只管本商户接收人(merchant_id/操作人一律以登录主体为准,请求值忽略),
 * 跨商户读写 CROSS_MERCHANT_FORBIDDEN。
 */
@RestController
@RequestMapping("/api/v1/merchant-billing/fee-notification-recipients")
public class MerchantFeeNotificationRecipientController {

    private static final String PLATFORM_PERM = "merchant.billing.manage";
    private static final String SELF_PERM = "merchant.portal.fee_recipient.manage";

    private final MerchantFeeNotificationRecipientService service;
    private final AuthorizationService authorizationService;

    public MerchantFeeNotificationRecipientController(
            MerchantFeeNotificationRecipientService service,
            AuthorizationService authorizationService
    ) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    // M723-F06：明文联系电话看一次扣一次（操作员 2026-10-03）；业务读权限照旧不扣
    @com.complex.platform.access.ConsumesSingleUseGrant(PhoneMask.VIEW_PLAINTEXT_PERMISSION)
    public ApiResponse<PageResponse<MerchantFeeNotificationRecipientRecord>> list(
            @RequestParam(value = "merchant_id", required = false) String merchantId
    ) {
        Optional<String> selfScope = selfServiceScopeOrPlatform();
        return ApiResponse.ok(maskPhonesForUnauthorized(service.list(selfScope.orElse(merchantId))));
    }

    @GetMapping("/{id}")
    // M723-F06：明文联系电话看一次扣一次（操作员 2026-10-03）；业务读权限照旧不扣
    @com.complex.platform.access.ConsumesSingleUseGrant(PhoneMask.VIEW_PLAINTEXT_PERMISSION)
    public ApiResponse<MerchantFeeNotificationRecipientRecord> get(@PathVariable String id) {
        Optional<String> selfScope = selfServiceScopeOrPlatform();
        MerchantFeeNotificationRecipientRecord record = service.get(id);
        requireOwnMerchant(selfScope, record);
        return ApiResponse.ok(maskPhoneForUnauthorized(record));
    }

    @PostMapping
    public ApiResponse<MerchantFeeNotificationRecipientRecord> upsert(
            @RequestBody @jakarta.validation.Valid MerchantFeeNotificationRecipientRequest request
    ) {
        Optional<String> selfScope = selfServiceScopeOrPlatform();
        // 第十八批 AG 段 255-R3④（PGM:899）：两条口径的操作人都取登录人，请求体 operator_user_id 不采信；自助口径另强制本商户。
        MerchantFeeNotificationRecipientRequest effective = new MerchantFeeNotificationRecipientRequest(
                selfScope.orElse(request == null ? null : request.merchantId()),
                request == null ? null : request.feeEvent(),
                request == null ? null : request.recipientName(),
                request == null ? null : request.recipientPhone(),
                authorizationService.currentActor().id());
        return ApiResponse.ok(maskPhoneForUnauthorized(service.upsert(effective)));
    }

    @PostMapping("/{id}/deactivate")
    public ApiResponse<MerchantFeeNotificationRecipientRecord> deactivate(@PathVariable String id) {
        Optional<String> selfScope = selfServiceScopeOrPlatform();
        requireOwnMerchant(selfScope, service.get(id));
        return ApiResponse.ok(maskPhoneForUnauthorized(service.deactivate(id)));
    }

    /** 平台口径返回 empty(任意商户);商户自助口径返回本商户 scope(员工无商户归属 fail-closed)。 */
    private Optional<String> selfServiceScopeOrPlatform() {
        try {
            authorizationService.requirePermission(PLATFORM_PERM);
            return Optional.empty();
        } catch (ApiException platformDenied) {
            authorizationService.requirePermission(SELF_PERM);
            return Optional.of(authorizationService.currentMerchantScope()
                    .orElseThrow(() -> ApiException.forbidden("MERCHANT_SCOPE_REQUIRED",
                            "商户自助管理通知接收人必须在商户身份下操作")));
        }
    }

    // ── P0-A/GAP-07 接收人手机号展示脱敏（V339）。无 pii.contact_phone.view 的读端看掩码；
    // 脱敏放在 controller（service 外），与 V279 内部结算价同款范式。短信触达链路走 service/resolver，仍拿明文。 ──
    private MerchantFeeNotificationRecipientRecord maskPhoneForUnauthorized(
            MerchantFeeNotificationRecipientRecord record) {
        if (record == null || viewPhonePlaintext(java.util.stream.Stream.of(record.recipientPhone()))) {
            return record;
        }
        return record.withMaskedPhone();
    }

    private PageResponse<MerchantFeeNotificationRecipientRecord> maskPhonesForUnauthorized(
            PageResponse<MerchantFeeNotificationRecipientRecord> page) {
        if (viewPhonePlaintext(page.items().stream().map(MerchantFeeNotificationRecipientRecord::recipientPhone))) {
            return page;
        }
        return new PageResponse<>(
                page.items().stream().map(MerchantFeeNotificationRecipientRecord::withMaskedPhone).toList(),
                page.page(), page.pageSize(), page.total());
    }

    // M723-F06（操作员 2026-10-03：敏感明文「算用掉，看一次扣一次」）：持有明文码、且这次响应里确有非空电话，才核验明文码并返回明文
    // ——单次来源在这里消耗，长期来源不产生消耗；否则返回掩码、不碰明文码。一次响应只核一次（列表按整页判断）。
    private boolean viewPhonePlaintext(java.util.stream.Stream<String> phones) {
        if (phones.noneMatch(phone -> phone != null && !phone.isBlank())
                || !authorizationService.currentActorHasPermission(PhoneMask.VIEW_PLAINTEXT_PERMISSION)) {
            return false;
        }
        authorizationService.requirePermission(PhoneMask.VIEW_PLAINTEXT_PERMISSION);
        // 本请求只是返回第一次的结果、新用掉的单次来源会被回滚（不算用掉）时不给明文
        return !com.complex.platform.common.idempotency.IdempotentReplayContext.replayDiscardsConsumption();
    }

    private static void requireOwnMerchant(Optional<String> selfScope, MerchantFeeNotificationRecipientRecord record) {
        if (selfScope.isPresent() && !selfScope.get().equals(record.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                    "无权管理其他商户的费用通知接收人");
        }
    }
}
