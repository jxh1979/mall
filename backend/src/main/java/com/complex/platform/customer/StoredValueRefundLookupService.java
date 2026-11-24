package com.complex.platform.customer;

// 文件版本：V0.04

import static com.complex.platform.common.Strings.requireText;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.finance.FinanceRefundRecord;
import com.complex.platform.finance.FinanceService;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * 第十八批 AZ 段第 278 条 278-R1（ENUM:86「综合体通用卡：……余额管理与退款均由长沙项目公司负责」；BSM:833「商户的员工只能管理本商户发行的储值卡」；
 * PGM:528「④看不到别的商户和别的项目的数据」）：储值退卡的查卡与退款范围。
 */
@Service
public class StoredValueRefundLookupService {

    private static final String PERMISSION = "customer.stored_value.refund";
    private static final String FREEZE_PERMISSION = "customer.stored_value.freeze";

    /** 退卡查卡的一张卡：页面据卡类型、本金、赠送金、发卡商户退款规则与账户关闭登记的待退事项办理退卡。 */
    public record StoredValueRefundCard(String accountId, String customerId, String maskedPhone, String projectId,
            String merchantId, long principalCent, long bonusCent, String status, boolean refundAllowed, int refundRatioBps,
            Long pendingRefundCent) {
    }

    private final CustomerRepository repository;
    private final AuthorizationService authorizationService;
    private final FinanceService financeService;

    public StoredValueRefundLookupService(CustomerRepository repository, AuthorizationService authorizationService,
            FinanceService financeService) {
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.financeService = financeService;
    }

    /**
     * 退款范围（查卡、线上退卡、柜台退卡首次提交与同编号重提、冻结、解冻共用）。auth 为 null 不强制。
     * 第十八批 CL 段 421-R1（第十八批裁决 79，BSM:833「商户级卡的发行、充值、余额与退款完全由发卡商户自行办理，不经平台；平台各岗位（含最高权限）
     * 一律看不到商户级卡的卡号、持卡人、余额与充值、退款流水」「商户的员工只能管理本商户发行的储值卡。」）：先比商户——当前账号的商户
     * （{@link AuthorizationService#currentMerchantIdQuietly}，平台管理员、最高权限与不挂商户的项目侧账号为空）须与卡的发卡商户相同（通用卡为空），
     * 不同即 403 CROSS_MERCHANT_FORBIDDEN：商户账号不碰通用卡与别家卡，不挂商户的账号不碰任何商户级卡。商户级卡不属于平台的数据与业务，最高权限不涉及。
     * 再比项目：最高权限（BSM:576、第十八批裁决 71）、卡不带项目、平台管理员不限；其余按该入口权限码 permission 可触及的项目判。
     * 第十八批 BD 段第 376 条 376-R1（PGM:659「被分配了可跨项目角色的员工（含经岗位得到），在所分配的权限内可以查看、处理该角色可查看项目的数据。」
     * 「逐个勾选的只能查看被选中的项目」「商户账号持有这类角色也不能跨项目」；ENUM:86「余额管理与退款均由长沙项目公司负责」）：项目维按
     * {@link AuthorizationService#projectReach}（本人项目与可跨项目角色勾选的项目）判，不再只比本人组织；商户账号 projectReach 只给本人项目，行为不变。
     */
    static void requireRefundScope(AuthorizationService auth, CustomerStoredValueAccountRecord card, String permission) {
        if (auth == null) {
            return;
        }
        if (!Objects.equals(auth.currentMerchantIdQuietly().orElse(null), card.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "该储值卡不在当前商户权限范围内");
        }
        if (auth.isMasterAuthority(auth.currentActor()) || card.projectId() == null || auth.currentProjectScopeQuietly().isEmpty()) {
            return; // 最高权限、不带项目的卡、平台管理员不限项目
        }
        if (!auth.projectReach(auth.currentActor(), permission).covers(card.projectId())) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "该储值卡不在当前项目权限范围内");
        }
    }

    /**
     * 按手机号查客人名下可办理退卡的卡（已销户的不列；范围外的不列）。查询不扣单次授权，由控制器标注。
     * 第十八批 BC 段 365-R1（BSM:833「账户可冻结/解冻。」）：冻结、解冻入口在储值退卡页，持退卡码或冻结码任一即可查卡，两码都不持 403。
     */
    public List<StoredValueRefundCard> lookup(String phone) {
        // 376-R1：查卡范围按所持码之一（优先退卡码）可触及的项目判
        String permission = authorizationService.currentActorHasPermission(PERMISSION) ? PERMISSION : FREEZE_PERMISSION;
        authorizationService.requirePermission(permission);
        CustomerRecord customer = repository.findCustomerByPhone(requireText(phone, "phone")).orElse(null);
        if (customer == null) {
            return List.of();
        }
        return repository.listStoredValueAccountsByCustomer(customer.id()).stream()
                .filter(card -> !"closed".equals(card.status()) && inScope(card, permission))
                .map(card -> view(customer, card))
                .toList();
    }

    private boolean inScope(CustomerStoredValueAccountRecord card, String permission) {
        try {
            requireRefundScope(authorizationService, card, permission);
            return true;
        } catch (ApiException outOfScope) {
            return false;
        }
    }

    private StoredValueRefundCard view(CustomerRecord customer, CustomerStoredValueAccountRecord card) {
        if (card.merchantId() == null) {
            return new StoredValueRefundCard(card.id(), customer.id(), customer.maskedPhone(), card.projectId(), null,
                    card.principalCent(), card.bonusCent(), card.status(), true, 10000, null);
        }
        StoredValueMerchantRefundRuleRecord rule = repository.findStoredValueMerchantRefundRule(card.merchantId())
                .orElse(StoredValueMerchantRefundRuleRecord.defaultAllowed(card.merchantId()));
        Long pending = financeService.findStoredValuePayoutRefund(StoredValueService.ACCOUNT_CLOSE_KEY_PREFIX + card.id())
                .filter(item -> "requested".equals(item.status()) || "processing".equals(item.status()))
                .map(FinanceRefundRecord::refundAmountCent)
                .orElse(null);
        return new StoredValueRefundCard(card.id(), customer.id(), customer.maskedPhone(), card.projectId(), card.merchantId(),
                card.principalCent(), card.bonusCent(), card.status(), rule.refundAllowed(), rule.refundRatioBps(), pending);
    }
}
