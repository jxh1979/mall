package com.complex.platform.ticketing;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.finance.PartnerVoucherSettlementPort;
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
    private final PartnerVoucherSettlementPort settlementPort;

    @Autowired
    public PartnerVoucherService(TicketingRepository repository, @Lazy PartnerVoucherSettlementPort settlementPort) {
        this.repository = repository;
        this.settlementPort = settlementPort;
    }

    public PartnerVoucherService(TicketingRepository repository) {
        this(repository, null);
    }

    /** 配置某票务权益模板为第三方券(通票包含该权益即附赠此券)。 */
    public PartnerVoucherConfigRecord createConfig(PartnerVoucherConfigRequest request) {
        PartnerVoucherConfigRequest actual = requireRequest(request);
        String templateId = requireText(actual.entitlementTemplateId(), "entitlement_template_id");
        repository.findEntitlementTemplateById(templateId)
                .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_TEMPLATE_NOT_FOUND", "entitlement template not found"));
        String mode = requireEnum(actual.settlementMode(), MODES, "settlement_mode");
        String kind = requireEnum(actual.partnerKind(), KINDS, "partner_kind");
        String partnerId = requireText(actual.partnerId(), "partner_id");
        Long price = actual.internalPriceCent();
        if ("internal_purchased".equals(mode) && (price == null || price <= 0)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "internal_purchased requires internal_price_cent > 0");
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
                .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_NOT_FOUND", "no entitlement for this voucher code"));
        PartnerVoucherConfigRecord config = repository.findPartnerVoucherConfig(issued.entitlementTemplateId())
                .orElseThrow(() -> ApiException.badRequest("NOT_PARTNER_VOUCHER", "this entitlement is not a partner voucher"));
        if (externalRef != null) {
            Optional<PartnerVoucherRedemptionRecord> existing =
                    repository.findPartnerVoucherRedemptionByExternalRef(config.partnerId(), externalRef);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        Instant redeemedAt = actual.redeemedAt() == null ? Instant.now() : actual.redeemedAt();
        repository.atomicUseEntitlement(issued.id(), Instant.now())
                .orElseThrow(() -> ApiException.conflict("ENTITLEMENT_USED_UP", "voucher has no remaining times"));
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
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private String requireText(String value, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        return trimmed;
    }

    private String requireEnum(String value, Set<String> allowed, String field) {
        String trimmed = requireText(value, field);
        if (!allowed.contains(trimmed)) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " must be one of " + allowed);
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
