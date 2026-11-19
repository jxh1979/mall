package com.complex.platform.ticketing;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.ticketing.dto.PartnerVoucherConfigRequest;
import com.complex.platform.ticketing.dto.PartnerVoucherRedemptionImportRequest;
import java.time.Instant;
import java.util.Map;
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
    private final AuthorizationService authorization;
    private final AuditEventService audit;

    @Autowired
    public PartnerVoucherService(TicketingRepository repository, @Lazy FinanceService settlementPort,
            AuthorizationService authorization, AuditEventService audit) {
        this.repository = repository;
        this.settlementPort = settlementPort;
        this.authorization = authorization;
        this.audit = audit;
    }

    /** 配置某票务权益模板为第三方券(通票包含该权益即附赠此券)。 */
    public PartnerVoucherConfigRecord createConfig(PartnerVoucherConfigRequest request) {
        PartnerVoucherConfigRequest actual = requireRequest(request);
        String templateId = requireText(actual.entitlementTemplateId(), "entitlement_template_id");
        EntitlementTemplateRecord template = repository.findEntitlementTemplateById(templateId)
                .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_TEMPLATE_NOT_FOUND", "权益模板不存在或已被删除"));
        requireTemplateProject(template.merchantId());
        String mode = requireEnum(actual.settlementMode(), MODES, "settlement_mode");
        String kind = requireEnum(actual.partnerKind(), KINDS, "partner_kind");
        String partnerId = requireText(actual.partnerId(), "partner_id");
        Long price = actual.internalPriceCent();
        if ("internal_purchased".equals(mode) && (price == null || price <= 0)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "内部采购权益必须填写大于 0 的内部结算价");
        }
        PartnerVoucherConfigRecord saved = repository.savePartnerVoucherConfig(
                new PartnerVoucherConfigRecord(templateId, mode, kind, partnerId, price, Instant.now()));
        // 第十八批 BB 段控制席并入②（PGM:966）：配置人与时间由留痕按会话自动记。
        audit.recordSuccess("ticket.partner_voucher_config.saved", "ticket.entitlement.manage", "partner_voucher_config", templateId,
                null, null, Map.of());
        return saved;
    }

    /**
     * 第十八批 BB 段控制席并入②（PGM:659「所有业务数据默认按项目隔离」；PGM:744「前端隐藏只是辅助，后端接口必须独立校验。」）：
     * 配置挂在权益模板上，模板本身不记项目，按模板提供方商户（BSM:1754「每项权益都必须绑定实际提供方」）所在的项目（主项目与经营项目）判：
     * 操作人按 ticket.entitlement.manage 能触及其中之一才放行；平台管理员恒可；模板没有提供方的只有平台管理员能配。
     */
    private void requireTemplateProject(String merchantId) {
        AuthorizationService.ProjectReach reach = authorization.projectReach(authorization.currentActor(), "ticket.entitlement.manage");
        if (!reach.platformAdmin() && repository.listMerchantProjectIds(merchantId).stream().noneMatch(reach::covers)) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "该权益模板的提供方不在本人可管理的项目范围内");
        }
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
        // 第十八批 BB 段 280-R1（PGM:528「④看不到别的商户和别的项目的数据」；PGM:659「数据的项目与商户隔离不提供关闭开关」）：
        // 券的项目取发券订单的项目（同 TicketVerificationService.requestProjectId），闸在按外部单号去重之前；
        // 取不到项目的普通账号按不可触及（同 TicketRefundService 原单无项目），平台管理员恒可。
        String project = issued.orderId() == null ? null
                : repository.findTicketOrderById(issued.orderId()).map(TicketOrderRecord::projectId).orElse(null);
        if (!authorization.canReachProject(authorization.currentActor(), project, "verification.execute")) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "该券码属于其他项目，不能导入其核销回流");
        }
        PartnerVoucherConfigRecord config = repository.findPartnerVoucherConfig(issued.entitlementTemplateId())
                .orElseThrow(() -> ApiException.badRequest("NOT_PARTNER_VOUCHER", "该权益不是合作方券"));
        if (externalRef != null) {
            Optional<PartnerVoucherRedemptionRecord> existing =
                    repository.findPartnerVoucherRedemptionByExternalRef(config.partnerId(), externalRef);
            if (existing.isPresent()) {
                return imported(existing.get(), externalRef, true);
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
        return imported(repository.savePartnerVoucherRedemption(new PartnerVoucherRedemptionRecord(
                redemptionId, issued.id(), code, config.partnerKind(), config.partnerId(), config.settlementMode(),
                redeemedAt, externalRef, settlementAmount, arApItemId, source == null ? "reconciliation_file" : source,
                Instant.now())), externalRef, false);
    }

    /**
     * 第十八批 BB 段 279-R1（BSM:1606「只要一个动作有过程，就要尽量在业务发生时自动留下记录。」；BSM:1618「第三方回流失败、重复……」）：
     * 新存与按同一外部单号命中原记录都记一条成功留痕（duplicate 区分），导入人与时间由留痕按会话自动记；幂等键栏记外部核销单号。
     */
    private PartnerVoucherRedemptionRecord imported(PartnerVoucherRedemptionRecord record, String externalRef, boolean duplicate) {
        audit.recordSuccess("ticket.partner_voucher_redemption.imported", "verification.execute", "partner_voucher_redemption",
                record.id(), null, externalRef, Map.of("duplicate", duplicate));
        return record;
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
