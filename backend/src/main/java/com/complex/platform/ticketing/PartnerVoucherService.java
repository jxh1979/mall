package com.complex.platform.ticketing;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.ticketing.dto.PartnerVoucherConfigRequest;
import com.complex.platform.ticketing.dto.PartnerVoucherRedemptionImportRequest;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

/**
 * 第三方券(批3):通票包含的餐厅/第三方券。配置结算口径 + 接收餐厅自己系统核销后的对账回流并结算。
 * 免费营销券核销不结算;内部价采购券核销按内部结算价给第三方(商户/外部伙伴)挂应付。
 */
@Service
public class PartnerVoucherService {

    private static final Set<String> MODES = Set.of("free_marketing", "internal_purchased");
    private static final Set<String> KINDS = Set.of("merchant", "external_partner");

    private final TicketingRepository repository;
    // finance 结算端口(构造注入·@Lazy 防环;为 null=兼容测试不结算)。
    private final FinanceService settlementPort;

    @Autowired
    public PartnerVoucherService(TicketingRepository repository, @Lazy FinanceService settlementPort) {
        this.repository = repository;
        this.settlementPort = settlementPort;
    }

    /** 配置某票务权益模板为第三方券(通票包含该权益即附赠此券)。 */
    public PartnerVoucherConfigRecord createConfig(PartnerVoucherConfigRequest request) {
        PartnerVoucherConfigRequest actual = requireRequest(request);
        String templateId = requireText(actual.entitlementTemplateId(), "entitlement_template_id");
        repository.findEntitlementTemplateById(templateId)
                .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_TEMPLATE_NOT_FOUND", "权益模板不存在或已被删除"));
        String mode = requireEnum(actual.settlementMode(), MODES, "settlement_mode");
        String kind = requireEnum(actual.partnerKind(), KINDS, "partner_kind");
        String partnerId = requireText(actual.partnerId(), "partner_id");
        Long price = actual.internalPriceCent();
        if ("internal_purchased".equals(mode) && (price == null || price <= 0)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "内部采购权益必须填写大于 0 的内部结算价");
        }
        return repository.savePartnerVoucherConfig(
                new PartnerVoucherConfigRecord(templateId, mode, kind, partnerId, price, Instant.now()));
    }

    /**
     * 接收餐厅自己系统核销后的对账回流:匹配券 → 消费权益一次 → 内部价采购券挂应付。
     * 幂等键=(第三方, 外部核销单号);重复回流同一单号直接返回既有记录。
     */
    @Transactional
    public PartnerVoucherRedemptionRecord importRedemption(PartnerVoucherRedemptionImportRequest request) {
        PartnerVoucherRedemptionImportRequest actual = requireRequest(request);
        String code = requireText(actual.entitlementCode(), "entitlement_code");
        String externalRef = trimToNull(actual.externalRedemptionRef());
        IssuedEntitlementRecord issued = repository.findIssuedEntitlementByCode(code)
                .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_NOT_FOUND", "该券码没有对应的权益"));
        PartnerVoucherConfigRecord config = repository.findPartnerVoucherConfig(issued.entitlementTemplateId())
                .orElseThrow(() -> ApiException.badRequest("NOT_PARTNER_VOUCHER", "该权益不是合作方券"));
        if (externalRef != null) {
            Optional<PartnerVoucherRedemptionRecord> existing =
                    repository.findPartnerVoucherRedemptionByExternalRef(config.partnerId(), externalRef);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        Instant redeemedAt = actual.redeemedAt() == null ? Instant.now() : actual.redeemedAt();
        repository.atomicUseEntitlement(issued.id(), Instant.now())
                .orElseThrow(() -> ApiException.conflict("ENTITLEMENT_USED_UP", "该券剩余次数已用完"));
        String redemptionId = "pvr_" + UUID.randomUUID().toString().replace("-", "");
        Long settlementAmount = null;
        String arApItemId = null;
        // 内部价采购券:核销即按内部结算价给第三方挂应付。免费营销券不结算。
        if ("internal_purchased".equals(config.settlementMode()) && settlementPort != null && config.internalPriceCent() != null) {
            settlementAmount = config.internalPriceCent();
            arApItemId = settlementPort.createPartnerVoucherPayable(config.partnerId(), settlementAmount, redemptionId);
        }
        String source = trimToNull(actual.source());
        return repository.savePartnerVoucherRedemption(new PartnerVoucherRedemptionRecord(
                redemptionId, issued.id(), code, config.partnerKind(), config.partnerId(), config.settlementMode(),
                redeemedAt, externalRef, settlementAmount, arApItemId, source == null ? "reconciliation_file" : source,
                Instant.now()));
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private String requireEnum(String value, Set<String> allowed, String field) {
        String trimmed = requireText(value, field);
        if (!allowed.contains(trimmed)) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " 必须为以下之一：" + allowed);
        }
        return trimmed;
    }
}
