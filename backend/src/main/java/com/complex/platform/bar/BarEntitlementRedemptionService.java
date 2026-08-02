package com.complex.platform.bar;

import static com.complex.platform.bar.BarValidation.requireRequest;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BarEntitlementRedemptionService {

    private final BarRepository repository;
    private final BarOperationExecutor operations;
    private final AuthorizationService authorizationService;

    public BarEntitlementRedemptionService(
            BarRepository repository,
            BarOperationExecutor operations,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.operations = operations;
        this.authorizationService = authorizationService;
    }

    /**
     * 酒水权益兑换承接入口。
     *
     * <p>权益消费权威仍在 ticketing/verifications：赠饮核销成功后会同步生成 bar_gift_redemptions。
     * 本方法只按 verification_id 承接既有赠饮记录，避免酒吧端绕过票务核销直接扣权益或扣库存。</p>
     */
    public PageResponse<BarGiftRedemptionRecord> listRedemptions() {
        return operations.readPage(
                BarReadOperation.LIST_GIFT_REDEMPTIONS,
                () -> repository.listGiftRedemptions(authorizationService.currentMerchantScope())
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.entitlement.redeem")
    @Transactional
    public BarGiftRedemptionRecord redeemDrink(BarEntitlementDrinkRedemptionRequest request) {
        BarEntitlementDrinkRedemptionRequest actual = requireRequest(request);
        String verificationId = operations.requireText(actual.verificationId(), "verification_id");
        return operations.executeWrite(
                "redeemBarDrinkEntitlement:" + verificationId,
                actual.idempotencyKey(),
                actual,
                BarGiftRedemptionRecord.class,
                BarWriteOperation.REDEEM_DRINK_ENTITLEMENT,
                () -> existingRedemption(verificationId)
        );
    }

    private BarGiftRedemptionRecord existingRedemption(String verificationId) {
        BarGiftRedemptionRecord redemption = repository.findGiftRedemptionByVerification(verificationId)
                .orElseThrow(() -> ApiException.notFound(
                        "BAR_GIFT_REDEMPTION_NOT_FOUND",
                        "赠酒兑付不可用，请先在票务侧核销赠酒权益"));
        checkMerchantOwnership(redemption.merchantId());
        if ("reversed".equals(redemption.status())) {
            throw ApiException.conflict("BAR_GIFT_REDEMPTION_REVERSED", "该赠酒兑付已被冲正");
        }
        return redemption;
    }

    private void checkMerchantOwnership(String resourceMerchantId) {
        authorizationService.currentMerchantScope().ifPresent(scope -> {
            if (!scope.equals(resourceMerchantId)) {
                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "该数据属于其他商户");
            }
        });
    }
}
