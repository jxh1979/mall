package com.complex.platform.ticketing;

import com.complex.platform.common.api.ApiException;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * S5 内部结算价统一解析器：全平台**只此一处**解析「赠送权益的提供方内部结算价」。
 *
 * <p>业务上为什么必须只有一处：预扣（分账时按票面估的成本）与兑付（客人真核销后付给商户的钱）
 * 必须用同一个价，否则月底对账时「预扣了多少」和「实付了多少」根本不可比，差额是真差额还是口径差
 * 谁也说不清（业务 HTML §通票分账全流程「每个权益的成本按提供方的内部结算价计」、
 * §赠送权益结算「预扣与核销实付的差额留在娱乐平台公司」）。
 *
 * <p><b>解析顺序</b>：
 * <ol>
 *   <li>模板绑了提供方商品（{@code bar_item_ref}，语义定死 = {@code bar_products.id}）
 *       → 取该商品的内部结算价；且该商品必须属于本权益的提供方商户
 *       （{@code bar_products.bar_id} = 权益提供方），否则视为未解析；</li>
 *   <li>否则取模板上的签约内部结算价 {@code agreement_price_cent}（餐饮/零售/服务类提供方）；</li>
 *   <li>否则**未解析** → 调用方一律 fail-closed，<b>绝不回退票面价</b>。</li>
 * </ol>
 *
 * <p><b>为什么不许回退票面价</b>：业务 HTML §综合／联合通票收入分账原文——「每个权益同时带票面原价与
 * 内部结算价：前者用于定价与拆份额，后者用于与提供商户结算成本，<b>两者不可混用</b>」。回退面值会让
 * 「按件计」的预扣基数悄悄用成售价，账面对得上、钱却全错，且无声无息。
 *
 * <p><b>0 与 NULL 语义分开</b>：内部结算价 = 0 是合法的（商户让利型的免费权益，平台不出钱）；
 * NULL 才是「没配价」→ 拒绝。与 S11 对 {@code component_price_cent} 的 NULL/0 收紧同一套语义。
 *
 * <p>待 PRD §9.7「内部结算 SKU 目录」建成后，①②两支改读目录即可，所有调用方无感。
 */
@Component
public class InternalSettlementPriceResolver {

    /** 赠饮权益类型（与 {@link TicketVerificationService} 同口径）。 */
    static final String GIFT_DRINK_ENTITLEMENT_TYPE = "gift_drink";
    /** 实物权益结算类别（V225 回填口径）。 */
    static final String KIND_PHYSICAL = "physical";

    private final TicketingRepository repository;

    public InternalSettlementPriceResolver(TicketingRepository repository) {
        this.repository = repository;
    }

    /**
     * 该权益是否属于「须到提供方核销、成本按提供方内部结算价计」的逐项赠送权益。
     *
     * <p>三条判据取并集，覆盖业务 HTML 点名的赠酒 / 餐饮 / 零售三类：赠饮类型、实物类别、
     * 或配了专属商户（餐饮/零售权益按「不分摊直付提供方」建模，见 PRD §6.4）。
     * 用判据而不用写死的类型清单，是为了以后加小食/零售类型时不用再回来改这里。
     */
    public static boolean providerRedeemed(String entitlementType, String entitlementKind, String exclusiveMerchantId) {
        return GIFT_DRINK_ENTITLEMENT_TYPE.equals(entitlementType)
                || KIND_PHYSICAL.equals(entitlementKind)
                || exclusiveMerchantId != null;
    }

    /**
     * 解析内部结算价（分）。
     *
     * @param barItemRef          提供方商品 id（= {@code bar_products.id}）；空则走 agreement 分支
     * @param providerMerchantId  该权益的提供方商户；商品必须属于它，否则不解析
     * @param agreementPriceCent  模板签约内部结算价（餐饮/零售提供方）
     * @return 解析到的价（可为 0）；空 = 未配价，调用方 fail-closed
     */
    public Optional<Long> resolve(String barItemRef, String providerMerchantId, Long agreementPriceCent) {
        if (barItemRef != null) {
            // 提供方归属必须对得上：A 吧的商品不能给 B 吧的权益当成本口径，否则钱结给谁全凭巧合。
            // 提供方为空 = 该权益没绑提供方 → 无从校验 → 不解析（自营吧同样不豁免，见 S5）。
            return repository.findBarProductRef(barItemRef)
                    .filter(product -> providerMerchantId != null && providerMerchantId.equals(product.barId()))
                    .map(BarProductRefRecord::internalSettlementPriceCent);
        }
        return Optional.ofNullable(agreementPriceCent);
    }

    /** 解析不到即抛 —— fail-closed 统一出口，错误码/文案只此一份。 */
    public long require(String barItemRef, String providerMerchantId, Long agreementPriceCent) {
        return resolve(barItemRef, providerMerchantId, agreementPriceCent)
                .orElseThrow(() -> ApiException.badRequest("ENTITLEMENT_PRICE_UNRESOLVED",
                        "该赠送权益未配置提供方的内部结算价，无法核销结算"));
    }

    /**
     * S6「按件计」预扣基数：该票种<b>票面</b>全部平台出资型赠送权益的
     * {@code Σ(quantity × num_people × 内部结算价)}（HTML:739「按件计＝票面各权益数量 × 内部结算价之和」；
     * 发放时每个 binding 恰好展开成 quantity × num_people 张独立权益，故这就是「票面数量」的真值）。
     *
     * <p>哪些 binding 进和（三条都要满足，判据与 S5/S11 同源、不另造）：
     * <ol>
     *   <li><b>票面原价为 0</b>（HTML:737「两类都因价格为 0 而不参与份额拆分」——有票面价的权益参与拆份额，
     *       不是赠送；S11 起 NULL=0 读取，发布已拦未配价）；</li>
     *   <li><b>成本承担方 = 平台出资型</b>（商户让利型平台不承担成本 → 不进 G 预扣，HTML:737）；</li>
     *   <li><b>提供方兑付类</b>（{@link #providerRedeemed}——赠酒/实物/专属直付；入场等免费权益无内部成本概念）。</li>
     * </ol>
     *
     * <p><b>fail-closed</b>：任一进和权益取不到内部结算价 → {@link #require} 抛
     * {@code ENTITLEMENT_PRICE_UNRESOLVED}。调用方（settlement 消费者）失败即本笔结算事实缺失、
     * 由重建任务在补价后按核销权威事实补建（AUD-017 既有语义）——绝不静默按 0 或票面价计提。
     *
     * <p>放在解析器上而不是 settlement 域：预扣（本方法）与兑付（{@link #require} 随
     * {@code ticket.gift_verified} 事件下发）必须同一个 bean 同一套取价逻辑，月底「预扣了多少/实付了多少」才可比。
     */
    public long byItemGiftCostCent(String ticketTypeId) {
        long total = 0L;
        for (TicketTypeEntitlementRecord binding : repository.listTicketTypeEntitlements(ticketTypeId)) {
            if (binding.componentPriceCent() != null && binding.componentPriceCent() != 0L) {
                continue; // 有票面价 → 参与份额拆分的付费组成，不是赠送权益。
            }
            if (!TicketTypeEntitlementRecord.COST_BEARER_PLATFORM_FUNDED.equals(binding.costBearer())) {
                continue; // 商户让利型：平台不承担成本、不进 G。
            }
            EntitlementTemplateRecord template =
                    repository.findEntitlementTemplateById(binding.entitlementTemplateId()).orElse(null);
            if (template == null || !providerRedeemed(
                    template.entitlementType(), template.entitlementKind(), template.exclusiveMerchantId())) {
                continue; // 非提供方兑付类（如免费入场资格）：无内部成本概念。
            }
            long price = require(template.barItemRef(), template.merchantId(), template.agreementPriceCent());
            total += (long) binding.quantity() * Math.max(1, binding.numPeople()) * price;
        }
        return total;
    }
}
