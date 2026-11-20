package com.complex.platform.customer;

// 文件版本：V0.03

import static com.complex.platform.common.Strings.requireText;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.finance.FinanceRefundRecord;
import com.complex.platform.finance.FinanceRepository;
import com.complex.platform.finance.FinanceService;
import java.util.List;
import java.util.function.Function;
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
    private final FinanceRepository financeRepository;

    public StoredValueRefundLookupService(CustomerRepository repository, AuthorizationService authorizationService,
            FinanceService financeService, FinanceRepository financeRepository) {
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.financeService = financeService;
        this.financeRepository = financeRepository;
    }

    /**
     * 退款范围（口径同 TicketRefundService.enforceOrderScope）：最高权限放行（BSM:576、第十八批裁决 71）；商户账号只办本商户的卡、不碰通用卡；
     * 卡带项目的按该入口权限码 permission 可触及的项目判（平台管理员不限）。auth 为 null 不强制。
     * 18AZ 收尾复核（PGM:528「④看不到别的商户和别的项目的数据」；BSM:1673「所有数据默认按项目隔离」）：不挂商户的账号碰到不带项目的商户卡，按发卡商户的
     * 经营项目（主项目或已批准的经营项目，同 FinanceService.recordStoredValuePayoutRefund 的 listMerchantOperatingProjectIds）判可否触及；
     * merchantProjects 为 null（单测未注入仓储）不判。
     * 第十八批 BD 段第 376 条 376-R1（PGM:659「被分配了可跨项目角色的员工（含经岗位得到），在所分配的权限内可以查看、处理该角色可查看项目的数据。」
     * 「逐个勾选的只能查看被选中的项目」「商户账号持有这类角色也不能跨项目」；ENUM:86「余额管理与退款均由长沙项目公司负责」）：项目维按
     * {@link AuthorizationService#projectReach}（本人项目与可跨项目角色勾选的项目）判，不再只比本人组织；商户账号 projectReach 只给本人项目，行为不变。
     */
    static void requireRefundScope(AuthorizationService auth, CustomerStoredValueAccountRecord card,
            Function<String, List<String>> merchantProjects, String permission) {
        if (auth == null || auth.isMasterAuthority(auth.currentActor())) {
            return;
        }
        String merchant = auth.currentMerchantIdQuietly().orElse(null);
        if (merchant != null && !merchant.equals(card.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "该储值卡不在当前商户权限范围内");
        }
        boolean projectless = card.projectId() == null;
        if (projectless && (merchant != null || card.merchantId() == null || merchantProjects == null)) {
            return;
        }
        if (auth.currentProjectScopeQuietly().isEmpty()) {
            return; // 平台管理员不限
        }
        AuthorizationService.ProjectReach reach = auth.projectReach(auth.currentActor(), permission);
        boolean covered = projectless
                ? merchantProjects.apply(card.merchantId()).stream().anyMatch(reach::covers)
                : reach.covers(card.projectId());
        if (!covered) {
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
            requireRefundScope(authorizationService, card, financeRepository::listMerchantOperatingProjectIds, permission);
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
