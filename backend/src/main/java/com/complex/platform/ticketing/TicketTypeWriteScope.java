package com.complex.platform.ticketing;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 第十八批 BP 段第 354 条：票种、派生价、动态价、权益模板与第三方券配置各写入口的商户与经营域范围。
 *
 * <p>354-R1（ENUM:32「单一酒吧门票由对应酒吧商户定价、销售并确认已核销部分的收入」；PGM:429「自行定价、销售并确认单一酒吧门票已核销部分的收入」；
 * BSM:707「④看不到别的商户和别的项目的数据」）：有商户号的（商户账号、派驻期内人员）只能办理本店的单一酒吧门票与本店提供的权益模板；没有商户号的人员不能办理
 * 挂了酒吧的票种。平台管理员（BSM:588「拥有系统里全部日常功能权限」）与最高权限本人（BSM:576）不受限。范围闸放在控制器入口、幂等之前。
 *
 * <p>354-R2（ENUM:32「酒吧通票由娱乐平台公司负责；纯游乐票和纯游乐通票由游乐公司负责；演出票由传媒公司负责」；PGM:419「单一经营域的定价、售票、内部结算和
 * 分配规则由对应域主管公司负责」）：没有商户号的人员只能办理本公司主管经营域的票——票属于哪个域按现表有价权益（各业态价格大于 0）的提供方定（BSM:749「零元免费
 * 权益不参与域间分配」，同跨域判定口径）；跨域组合通票与还没有有价权益的票不设域闸；绑权益只对已上架的票设域闸（组合配置逐条写入）。
 *
 * <p>300-R1（第十八批裁决 74）：域内票的票种规则由对应域主管公司的人审批，审批人资格同此处取法。
 */
@Component
public class TicketTypeWriteScope {

    static final String SCOPE_FORBIDDEN = "TICKET_TYPE_SCOPE_FORBIDDEN";

    private final AuthorizationService auth;
    private final TicketingRepository repository;
    private final TicketDomainCompanyPort port;

    public TicketTypeWriteScope(AuthorizationService auth, TicketingRepository repository, TicketDomainCompanyPort port) {
        this.auth = auth;
        this.repository = repository;
        this.port = port;
    }

    /** 建票：有商户号的只能建本店的单一酒吧门票；没有商户号的不能建挂酒吧的票。 */
    public void requireCreate(String barId) {
        if (unrestricted()) {
            return;
        }
        String bar = trimToNull(barId);
        Optional<String> own = auth.currentMerchantIdQuietly();
        if (own.isPresent() ? !own.get().equals(bar) : bar != null) {
            throw own.isPresent() ? merchantOnly() : barOnly();
        }
    }

    /** 改已有票种（改价、发布、撤回、存档、售卖设置、最低赠酒杯数、申请可退、派生价与动态价）：商户闸＋域闸。票种不存在不拦，交原逻辑报 404。 */
    public void requireWrite(String ticketTypeId) {
        check(ticketTypeId, false);
    }

    /** 绑权益：商户闸＋已上架票的域闸（未上架票的组成不按域限制）。 */
    public void requireBind(String ticketTypeId) {
        check(ticketTypeId, true);
    }

    /** 派生价存档、取消存档、申请删除：按所属票种。 */
    public void requirePriceRule(String priceRuleId) {
        repository.findPriceRuleById(priceRuleId).ifPresent(rule -> requireWrite(rule.ticketTypeId()));
    }

    /** 动态价发布：按所属票种。 */
    public void requireDynamicRule(String dynamicRuleId) {
        repository.findDynamicPricingRuleById(dynamicRuleId).ifPresent(rule -> requireWrite(rule.ticketTypeId()));
    }

    /** 建权益模板：有商户号的只能建本店提供的。 */
    public void requireTemplateMerchant(String merchantId) {
        if (unrestricted()) {
            return;
        }
        Optional<String> own = auth.currentMerchantIdQuietly();
        if (own.isPresent() && !own.get().equals(trimToNull(merchantId))) {
            throw ApiException.forbidden(SCOPE_FORBIDDEN, "商户账号只能办理本店提供的权益模板");
        }
    }

    /** 权益模板存档、取消存档、申请删除：按模板提供方。模板不存在不拦。 */
    public void requireTemplate(String templateId) {
        repository.findEntitlementTemplateById(templateId).ifPresent(template -> requireTemplateMerchant(template.merchantId()));
    }

    /** 全场统一设置（二维码刷新间隔，BSM:741「二维码按后台统一设置的刷新间隔自动刷新」）：有商户号的不能改。 */
    public void requireNotMerchant() {
        if (!unrestricted() && auth.currentMerchantIdQuietly().isPresent()) {
            throw ApiException.forbidden(SCOPE_FORBIDDEN, "商户账号不能修改全场统一设置");
        }
    }

    /**
     * 300-R1：域内票的最终审批人须是这张票对应域主管公司的人（BSM:1673「域内规则由对应域主管公司负责」）。
     *
     * @param merchants 有价权益的提供方商户（没有有价权益时取全部权益的提供方）
     */
    public void requireDomainApprover(String approverId, Collection<String> merchants) {
        Set<String> owners = companies(merchants);
        if (owners.size() != 1) {
            throw ApiException.conflict("TICKET_TYPE_DOMAIN_COMPANY_UNRESOLVED", "定不出这张票由哪家域主管公司审批");
        }
        String company = owners.iterator().next();
        if (!company.equals(port.companyOfUser(approverId))) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "这张票由「" + port.companyName(company) + "」负责，最终审批人须是该公司的人员");
        }
    }

    private void check(String ticketTypeId, boolean binding) {
        String id = trimToNull(ticketTypeId);
        if (id == null || unrestricted()) {
            return;
        }
        TicketTypeRecord type = repository.findTicketTypeById(id).orElse(null);
        if (type == null) {
            return;
        }
        String bar = trimToNull(type.barId());
        Optional<String> own = auth.currentMerchantIdQuietly();
        if (own.isPresent()) {
            if (!own.get().equals(bar)) {
                throw merchantOnly();
            }
            return;
        }
        if (bar != null) {
            throw barOnly();
        }
        if (binding && !"published".equals(type.status())) {
            return;
        }
        Set<String> owners = companies(paidProviders(repository.listTicketTypeEntitlements(id)));
        if (owners.size() != 1) {
            return; // 没有有价权益，或跨域组合通票
        }
        String company = owners.iterator().next();
        if (!company.equals(port.companyOfUser(auth.currentActor().id()))) {
            throw ApiException.forbidden(SCOPE_FORBIDDEN, "这张票由「" + port.companyName(company) + "」负责，只有该公司人员可以办理");
        }
    }

    /** 有价权益（各业态价格大于 0）的提供方商户。 */
    private List<String> paidProviders(List<TicketTypeEntitlementRecord> bindings) {
        List<String> templates = bindings.stream()
                .filter(binding -> binding.componentPriceCent() != null && binding.componentPriceCent() > 0L)
                .map(TicketTypeEntitlementRecord::entitlementTemplateId).distinct().toList();
        Map<String, TicketingRepository.TemplateProvider> providers = repository.findTemplateProviders(templates);
        return templates.stream().map(providers::get).filter(provider -> provider != null && provider.providerMerchantId() != null)
                .map(TicketingRepository.TemplateProvider::providerMerchantId).distinct().toList();
    }

    /** 商户清单对应的域主管公司（去重）；定不出的商户由端口抛 409。 */
    private Set<String> companies(Collection<String> merchants) {
        return merchants.stream().filter(merchant -> merchant != null).map(port::domainCompanyOfMerchant)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean unrestricted() {
        AccessActor actor = auth.currentActor();
        return auth.isPlatformAdmin(actor) || auth.isMasterAuthority(actor);
    }

    private static ApiException merchantOnly() {
        return ApiException.forbidden(SCOPE_FORBIDDEN, "商户账号只能办理本店的单一酒吧门票");
    }

    private static ApiException barOnly() {
        return ApiException.forbidden(SCOPE_FORBIDDEN, "单一酒吧门票由对应酒吧商户定价和办理");
    }
}
