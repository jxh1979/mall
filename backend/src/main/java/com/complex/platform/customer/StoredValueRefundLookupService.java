package com.complex.platform.customer;

// 文件版本：V0.01

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
     * 卡带项目的只办本人项目的（平台管理员不限）。不挂商户的项目侧账号由此可办本项目通用卡。auth 为 null 不强制。
     * 18AZ 收尾复核（PGM:528「④看不到别的商户和别的项目的数据」；BSM:1673「所有数据默认按项目隔离」）：不挂商户的账号碰到不带项目的商户卡，按发卡商户的
     * 经营项目（主项目或已批准的经营项目，同 FinanceService.recordStoredValuePayoutRefund 的 listMerchantOperatingProjectIds）判本人项目可否触及；
     * merchantProjects 为 null（单测未注入仓储）不判。
     */
    static void requireRefundScope(AuthorizationService auth, CustomerStoredValueAccountRecord card,
            Function<String, List<String>> merchantProjects) {
        if (auth == null || auth.isMasterAuthority(auth.currentActor())) {
            return;
        }
        String merchant = auth.currentMerchantIdQuietly().orElse(null);
        if (merchant != null && !merchant.equals(card.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "该储值卡不在当前商户权限范围内");
        }
        if (card.projectId() != null && auth.currentProjectScopeQuietly().filter(scope -> !scope.equals(card.projectId())).isPresent()) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "该储值卡不在当前项目权限范围内");
        }
        if (merchant == null && card.projectId() == null && card.merchantId() != null && merchantProjects != null
                && auth.currentProjectScopeQuietly().filter(scope -> !merchantProjects.apply(card.merchantId()).contains(scope)).isPresent()) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "该储值卡不在当前项目权限范围内");
        }
    }

    /** 按手机号查客人名下可办理退卡的卡（已销户的不列；范围外的不列）。查询不扣单次授权，由控制器标注。 */
    public List<StoredValueRefundCard> lookup(String phone) {
        authorizationService.requirePermission(PERMISSION);
        CustomerRecord customer = repository.findCustomerByPhone(requireText(phone, "phone")).orElse(null);
        if (customer == null) {
            return List.of();
        }
        return repository.listStoredValueAccountsByCustomer(customer.id()).stream()
                .filter(card -> !"closed".equals(card.status()) && inScope(card))
                .map(card -> view(customer, card))
                .toList();
    }

    private boolean inScope(CustomerStoredValueAccountRecord card) {
        try {
            requireRefundScope(authorizationService, card, financeRepository::listMerchantOperatingProjectIds);
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
