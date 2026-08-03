package com.complex.platform.settlement;

import com.complex.platform.common.money.MoneyRounding;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 结算重构批2-3：月度通票分账<b>计算引擎</b>（§5.0 全流程）。
 *
 * <p>按业态拆份额 → 扣渠道费 → 扣门票综合服务费(池链支) → 可分配池各酒吧分配 → 校验 → 写 draft 结算单/明细。
 * <b>不过账 ar_ap_items</b>（批2-4）；只落 settlement_bills / settlement_items（status=draft）。
 *
 * <p><b>S4(合并留存,2026-07-28)</b>:原「池内 20% 项目留存」与门票综合服务费是<b>同一项、只扣一次</b>
 * (业务 HTML「即娱乐平台公司留存/项目管理费」),留存全链已删——池 = 酒吧净份额 − 综合服务费(池链支),
 * 池内不再二次扣任何留存;两支服务费(池链/专属)都归娱乐平台公司。费在<b>每笔核销时</b>计提落
 * settlement_transactions(P6 不可变事实,冲正取原行之反),月结只做线性求和,绝不月末重算。
 *
 * <p><b>带符号(R-3,2026-07-28 用户拍板,照 V335 先例)</b>:冲正月(红冲多于新核销)收入/池/分账/费
 * 各数一律带符号落库——负数=当月净追回,静默钳零会丢账;HALF_UP 对 0 对称保证冲正逐分对消。
 *
 * <p>月窗 = [月首 00:00 UTC, 次月首 00:00 UTC) 按 settlement_transactions.business_at(核销业务时刻)。
 *
 * <p><b>金额精度与尾差（R-7 / D15.1，2026-07-22 用户拍板）</b>：一切按比例拆分一律
 * {@link MoneyRounding} <b>四舍五入到分（HALF_UP）</b>，不再用 {@code a*bps/10000} 整除截断
 * （旧写法系统性少给收费方且与「四舍五入」字面口径不符）。各家分别舍入后与「应分总额」的差额=<b>尾差</b>，
 * <b>单列</b>落 {@code settlement_bills.rounding_residual_cent}（V343）留痕，并由该笔业务
 * <b>实际承担结算责任的主体</b>承担（主体快照 {@code rounding_residual_entity_id}，V383）——
 * 不得默认转给城市项目公司/顶层平台，绝不悄悄并进某一家、也不静默沉淀。守恒恒等式（{@link #verifyConservation} 运行期自检 + 集成测试断言）：
 * <ul>
 *   <li>{@code 通票收入 = 渠道费 + Σ各业态综合服务费 + Σ各业态净份额}（拆份额链无泄漏,{@link #verifyFormatSplit}）</li>
 *   <li>{@code Σ池分账明细 + 尾差 = 应分总额}（分账链尾差单列后严格相等）</li>
 * </ul>
 * HALF_UP 对 0 对称（{@code f(-x) = -f(x)}），故跨月红冲取原行之反仍逐分对消，守恒不被舍入破坏。
 *
 * <p><b>S8 两段分配(2026-07-28,业务 HTML「可分配池按两段分配」逐字)</b>:三种方式不再是项目级三选一——
 * <b>第一段</b>:配了商户级固定规则(rule_type=per_guest 每客单价 / fixed 固定额,V374)的酒吧按<b>绝对金额</b>先拿;
 * <b>第二段</b>:其余酒吧按排名档位对「池 − 第一段」的余额<b>归一化分尽</b>(档位只作相对权重,只舍入一次);
 * <b>拿了第一段的酒吧不再参与第二段</b>(K18 ⑤)。边界(§二十一,用户已拍板):
 * ① 第一段合计 Q &gt; 池 P → 不削任何金额、第二段为 0,落既有超池特批路径(draft 待平台负责人特批);
 * ② 第二段无参与者(排名档位酒吧为空/权重全 0)而余额非 0 → 余额<b>显式归娱乐平台公司</b> + 告警留痕
 *   (bill.pool_undistributed_cent 单列,不是静默沉淀);当期完全无酒吧参与(D=0)同此出口。
 * 当期无固定规则酒吧且档位权重和恰为 10000 → 与两段化前逐分不变(prorate(P,w,10000) ≡ bpsOf(P,w))。
 */
@Service
public class TicketDistributionSettlementService {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(TicketDistributionSettlementService.class);

    static final String METHOD_BY_RANK_TIER = "by_rank_tier";
    static final String METHOD_BY_VENUE_PER_GUEST = "by_venue_per_guest";
    static final String METHOD_BY_FIXED_AMOUNT = "by_fixed_amount";
    // 批3-4(§6.4)：专属某商户「不分摊」直付明细方式——净额(毛额−综合服务费)归该商户，不进可分配池、不参与排名。
    static final String METHOD_EXCLUSIVE_FULL = "exclusive_full";

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_CONFIRMED = "confirmed";
    private static final String ITEM_STATUS_DRAFT = "draft";

    // 六业态收入归属(terminology-canonical §三):通票收入流 → all_pass_ticket。
    static final String REVENUE_STREAM_ALL_PASS_TICKET = "all_pass_ticket";
    // S4:门票综合服务费收入流(覆盖所有门票,不止通票)——收款主体由归属配置解析(V370 种子=娱乐平台公司)。
    static final String REVENUE_STREAM_TICKET_SERVICE_FEE = "ticket_service_fee";
    // S6:赠送权益预估成本收入流——收款主体由归属配置解析(V372 种子=娱乐平台公司 be_ent_platform)。
    static final String REVENUE_STREAM_PASS_GIFT_COST = "pass_gift_cost";
    // S11 业态词表(V348 五值 + V384 加 media 共六值,merchants.merchant_type 封口)。酒吧业态即「娱乐平台公司」那一支。
    static final String FORMAT_BAR = "bar";
    /** V384 传媒业态:演出票/演出权益的提供方,收入按 HTML「演出票 → 传媒公司」归 performance_ticket 流。 */
    static final String FORMAT_MEDIA = "media";
    private static final String BENEFICIARY_ENTITY = "entity";
    private static final String BENEFICIARY_PER_MERCHANT = "per_merchant";
    /**
     * S11④ 业态 → 收款方。酒吧/游乐/传媒份额各归一个经营主体（娱乐平台公司 / 游乐公司 / 传媒公司），
     * 主体是谁由 {@code revenue_stream_attribution} 后台可配，这里只定「查哪条收入流」；
     * 餐饮/零售/其他没有单一主体，钱归提供这份权益的商户自己（per_merchant）。
     *
     * <p>V384 补传媒一行：业务 HTML §「收入归属」明写「演出票 → 传媒公司」，
     * 缺这一行时演出份额会被当成 per_merchant 归到商户自己，传媒公司在结算单上完全看不见自己的钱。
     */
    private static final java.util.Map<String, String> ENTITY_BENEFICIARY_FORMATS = java.util.Map.of(
            FORMAT_BAR, REVENUE_STREAM_ALL_PASS_TICKET,
            "amusement", "amusement_ticket",
            FORMAT_MEDIA, "performance_ticket");

    private final JdbcSettlementBillRepository repository;
    private final RevenueStreamAttributionRepository attributionRepository;

    public TicketDistributionSettlementService(JdbcSettlementBillRepository repository,
                                               RevenueStreamAttributionRepository attributionRepository) {
        this.repository = repository;
        this.attributionRepository = attributionRepository;
    }

    /**
     * 结算某项目某月的通票分账，写 draft 结算单+明细，幂等重跑覆盖。
     *
     * @param projectId 项目公司（综合体）；null=平台默认口径（COALESCE 到 __DEFAULT__）
     * @param month     结算月（如 2026-06）
     * @return 落地的结算单 id
     */
    @Transactional
    public String settleMonth(String projectId, YearMonth month) {
        LocalDate periodStart = month.atDay(1);
        LocalDate periodEnd = month.atEndOfMonth();

        // B-1 守卫(2026-07-06 第一性原理审计 + 独立复核):仅 draft/confirmed(未过账)可安全重算(幂等覆盖);
        //   settled/reversed 及将来 CHECK 新增的任何"已出账/终结"态一律 fail-closed 拒绝——重跑会 deleteItemsByBill
        //   删掉已挂应付(ar_ap_item_id 已置)的明细,产生孤儿 AR/AP。反向白名单跟随 schema,不必每加终态回来改;
        //   注:posted 是 settlement_items 的状态、bill 永不为之(V224 CHECK 只含 draft/confirmed/settled/reversed),故不单列。
        String existingStatus = repository.findBillStatusByPeriod(projectId, periodStart, periodEnd);
        if (existingStatus != null && !"draft".equals(existingStatus) && !"confirmed".equals(existingStatus)) {
            throw com.complex.platform.common.api.ApiException.conflict("SETTLEMENT_ALREADY_POSTED",
                    "该月份结算单已过账/结算/冲正(状态=" + existingStatus + "),不可重结;如需重算请先对已过账明细做冲正");
        }
        // D7(对抗复核 risk2):已超池特批的 confirmed 单视同冻结——special_approval_ref 是对「该次超池特定金额」的批准,
        //   重跑会改分配/超额额、使批准额与实际过账额脱钩且无二次审批门。故命中已特批单直接返回现有 id、不重算(保住批准快照);
        //   如需重算须先撤批(清 ref)。月度作业据此天然跳过(不抛异常、不 crash 批次),迟到核销并入须走人工撤批重算。
        // S9 到期回收:冻结只认【仍在有效期内】的特批;到期后本单回到普通单待遇——照常重算,仍超池即打回 draft。
        if ("confirmed".equals(existingStatus)
                && repository.findActiveSpecialApprovalRefByPeriod(projectId, periodStart, periodEnd) != null) {
            return repository.findBillId(projectId, periodStart, periodEnd);
        }
        Instant windowStart = periodStart.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant windowEnd = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        // D6 套票折扣归属(§7.4)：项目级配置（默认 platform_absorb）。折扣额已在核销时逐笔落 discount_amount_cent（append-only 账本），
        //   本引擎按 discount_bearer 应用——项目承担部分让商户拿满额、并记入 bill.combo_discount_cent。无折扣(折扣额=0)时全程 +0=零行为变更。
        var discountCfg = repository.findComboDiscountConfig(projectId);

        // ① 通票收入(已核销)按【业态】拆开(S11①,§二十.1)。一张联合通票的钱不是一整笔都归酒吧——
        //    游乐那份归游乐公司、餐饮那份归餐厅商户、零售那份归零售商户,只有酒吧那份才继续往下走进池。
        //    D6/§20.4：项目承担的折扣补回口径对【每一个业态】一视同仁(旧实现只补酒吧一路,同一张票各家口径不一致)。
        //    份额比例本身在【出票时】就按各权益票面原价占比拆好了(TicketOrderService.allocateRevenue),
        //    这里只是把权益级的份额按提供方业态归堆,不重新发明分摊算法。
        List<JdbcSettlementBillRepository.FormatRevenue> formatRows =
                repository.aggregateFormatRevenue(projectId, windowStart, windowEnd);
        java.util.LinkedHashMap<String, FormatShare> formatShares = new java.util.LinkedHashMap<>();
        long ticketRevenue = 0L;
        long comboDiscountCent = 0L; // 本月项目公司实际承担的折扣入账(各业态 + 后续专属直付累加)。
        long pooledServiceFee = 0L;  // S4 门票综合服务费·池链支(F):Σ各业态当月逐笔计提,归娱乐平台公司。
        long pooledGiftCost = 0L;    // S6 赠送权益预估成本(G):Σ各业态当月逐笔计提,归娱乐平台公司。
        for (var fr : formatRows) {
            long borne = platformBorneDiscount(discountCfg, fr.discountCent());
            formatShares.put(fr.businessFormat(),
                    new FormatShare(fr.businessFormat(), fr.revenueCent() + borne, borne, 0L,
                            fr.compServiceFeeCent(), fr.giftCostCent()));
            ticketRevenue += fr.revenueCent() + borne;
            comboDiscountCent += borne;
            pooledServiceFee += fr.compServiceFeeCent();
            pooledGiftCost += fr.giftCostCent();
        }
        // K20 不重不漏:业态汇总必须恰好覆盖同一批进池核销(分组条件写错/漏一个业态会在这里响亮停,
        //   而不是悄悄少算一笔)。收入/综合服务费/赠送权益预估成本三个合计都核——漏一个业态,娱乐平台公司就少收一笔。
        var pooledTotals = repository.sumPooledTotals(projectId, windowStart, windowEnd);
        if (ticketRevenue - comboDiscountCent != pooledTotals.revenueCent()) {
            throw com.complex.platform.common.api.ApiException.conflict("SETTLEMENT_CONSERVATION_VIOLATED",
                    "业态份额汇总与进池核销总额不符:Σ各业态净额 " + (ticketRevenue - comboDiscountCent)
                            + " ≠ 进池核销合计 " + pooledTotals.revenueCent());
        }
        if (pooledServiceFee != pooledTotals.compServiceFeeCent()) {
            throw com.complex.platform.common.api.ApiException.conflict("SETTLEMENT_CONSERVATION_VIOLATED",
                    "业态综合服务费汇总与进池计提总额不符:Σ各业态服务费 " + pooledServiceFee
                            + " ≠ 进池计提合计 " + pooledTotals.compServiceFeeCent());
        }
        if (pooledGiftCost != pooledTotals.giftCostCent()) {
            throw com.complex.platform.common.api.ApiException.conflict("SETTLEMENT_CONSERVATION_VIOLATED",
                    "业态赠送权益预估成本汇总与进池计提总额不符:Σ各业态 G " + pooledGiftCost
                            + " ≠ 进池计提合计 " + pooledTotals.giftCostCent());
        }

        // ①b S7 赠付差额结算(HTML:962「预扣与核销实付的差额留在娱乐平台公司,按可配规则调账留痕」):
        //    同一事务、同一月窗,按「项目 × 规则(pass_gift_cost_configs)」结出 差额 V = 预扣 G − 实付 A。
        //    G = 本月进池计提净额(按计提行快照的规则 id 分组);A = ar_ap_items(pass_drink_settlement) 对冲净额
        //    (月窗/项目/票种锚点取该核销的结算事实行,票种再走与计提同一个 findPassGiftCostConfig 归到规则)。
        //    三种规则(V373 快照定义):platform_absorb 仅留痕/carry_forward 结转下期/true_up_pool 回补当期池。
        GiftVarianceCalc giftVariance = settleGiftVariance(projectId, periodStart, windowStart, windowEnd,
                pooledGiftCost);

        // ② 赠酒成本不在此扣(D5,2026-07-21 用户拍板删):通票免费酒水由 bar 域 BarGiftConsumer 每次核销按【内部结算价】
        //    给出品酒吧挂应付(pass_drink_settlement,幂等键 verificationId,可冲正)=唯一补偿口径;原「每杯 3 元池扣」
        //    是财务测算假设(PRD §3.7 假设不进系统)且与该内部结算价应付重复=对同一杯赠酒双重计,已删,不再从池扣减。

        // ③ 渠道抽成【整单前移】(S11②,§二十.1「先扣第三方渠道抽成,再按业态占比拆份额」)。
        //    渠道拿的是客人实付的那一整笔的抽成,不是只从酒吧那份里扣——旧实现把它记在池链上,
        //    等于让酒吧一家替全场的渠道费买单。改为按整单算出来,再按各业态份额占比分摊下去。
        //    费率按【订单销售渠道】解析(channel_fee_configs 可按项目×渠道配),官方直销/未配 → 默认率 0 → 逐分不变。
        //    V381 两模式二选一(HTML「比例/金额后台可配」):比例=费率 × 该渠道当期实付额(HALF_UP);
        //    固定金额=每张票固定金额 × 该渠道当期签名票数(票数按首笔核销锚点计,一张票多次核销只收一道,
        //    冲正月票数为负 → 费为负=向渠道追回,与比例模式负收入同一带符号口径;整数乘法无舍入)。
        long channelFee = 0L;
        for (var cr : repository.aggregateChannelRevenue(projectId, windowStart, windowEnd)) {
            String channel = cr.salesChannel() == null || cr.salesChannel().isBlank() ? null : cr.salesChannel();
            var feeCfg = repository.findChannelFee(projectId, channel);
            // R-7:比例模式四舍五入到分(旧整除截断少扣渠道费)。
            channelFee += feeCfg.fixedAmountCent() != null
                    ? feeCfg.fixedAmountCent() * cr.ticketCount()
                    : MoneyRounding.bpsOf(cr.revenueCent(), feeCfg.feeBps());
        }
        // 渠道费按各业态毛额占比摊下去;分摊尾差【不再并进酒吧支】,而是单列到同一尾差口径
        //   (治理地图「不得悄悄并进任一参与方」—— 酒吧也是参与方,不能替全场认这几分)。
        long channelFeeResidual = allocateChannelFee(formatShares, channelFee, ticketRevenue);

        // ③b S7 兑付成本按【提供方业态】分记(HTML:962「成本按提供方的商户业态分开记账」):
        //    A 按业态归堆落业态份额行 gift_actual_paid_cent。纯留痕不进净份额(兑付由娱乐平台公司承担,
        //    不动各业态分账,K11 兑付与池无函数关系不变)。当月只有兑付没有核销份额的业态补零份额行,
        //    「给餐厅/零售兑付了多少」不因该业态本月没进池收入而丢行。
        for (var pf : giftVariance.paidByFormat().entrySet()) {
            FormatShare cur = formatShares.get(pf.getKey());
            formatShares.put(pf.getKey(), cur == null
                    ? new FormatShare(pf.getKey(), 0L, 0L).withGiftPaid(pf.getValue())
                    : cur.withGiftPaid(pf.getValue()));
        }

        // ④⑤ 可分配池(唯一的池)= 【酒吧业态】净份额 = 酒吧毛额 − 摊到的渠道费 − 酒吧侧 G − 酒吧侧综合服务费(池链支)。
        //    S6:P = R − C − G − F_pool(HTML:739/:742 瀑布顺序——先扣赠送权益预估成本,再扣门票综合服务费)。
        //    G 按业态跟核销走(照 S4 的 F 同一范式):酒吧份额扣酒吧的 G,别的业态不替它埋单;
        //    常态首笔核销=入场(酒吧业态)→G 全落酒吧行,与 HTML:742「酒吧份额再依次扣掉」一致。
        //    游乐/餐饮/零售的净份额各走自己的出口,不进这个池;各业态的综合服务费同样从各自净份额里扣。
        //    S4:池内【不再二次扣任何留存】(业务 HTML 明文)——原「池 × 20% 项目留存」与综合服务费是同一项,
        //    已由等价保值种子(V370)平移进综合服务费费率,金额逐分不变、名目与收款方按 HTML 正名。
        //    带符号(R-3):冲正月酒吧净份额可为负 → 池为负 → 各家分账为负 = 向酒吧追回,不钳零不丢账。
        FormatShare barShare = formatShares.get(FORMAT_BAR);
        // S7 true_up_pool 规则:差额回补当期可分配池(净份额公式不动,池在其后加回;platform_absorb/carry_forward 恒 +0)。
        long availablePool = (barShare == null ? 0L : barShare.netCent()) + giftVariance.poolTrueUpTotal();

        // ⑥ 各酒吧当月统计(核销数降序)
        List<JdbcSettlementBillRepository.MerchantMonthlyStat> stats =
                repository.aggregateMerchantStats(projectId, windowStart, windowEnd);

        // ⑦ S8 两段分配（仅非专属进池；池基/排名/客数聚合已在仓储层按业态+排除专属过滤,S11）:
        //    第一段=固定规则酒吧按绝对额先拿;第二段=其余酒吧按排名档位归一化分尽余额;
        //    method(语义收窄后=第二段方式)仅 by_rank_tier 合法。
        String method = repository.findDistributionMethod(projectId);
        PooledDistribution pooled = distribute(projectId, method, availablePool, stats);
        List<ItemCalc> items = new ArrayList<>(pooled.items());

        // ⑦b 批3-4(§6.4)：专属某商户「不分摊」→ 每个专属商户一条直付明细（method=exclusive_full，
        //     不进可分配池、不参与他家分池排名/客数聚合）。
        // 综合服务费·专属支:专属商户直付前先抽综合服务费,商户拿【净额=收入-服务费】
        //   (记入 bill.comp_service_fee_exclusive_cent)。与池链支是同一道费的两支,都归娱乐平台公司。
        //   S2(V365):费率规则的范围是【项目 × 商户 × 票种】,且【每笔核销时】按票种解析并落库(P6 起本引擎只做
        //   线性求和,不再月末重算),故此处按 entitlement_type 分组只是聚合口径遗留、与费率命中无关
        //   (下面 merchantAgg 又按商户合并回去)。
        List<JdbcSettlementBillRepository.ExclusiveMerchantRevenue> exclusives =
                repository.aggregateExclusiveRevenue(projectId, windowStart, windowEnd);
        // V336 综合服务费月度封顶(用户 2026-07-21 拍板 A「按费率规则各自封」):费率规则可跨票种(商户级规则
        //   ticket_type_id=NULL)或跨商户(全局默认 merchant_id=NULL),故封顶【不能】逐 (商户×票种) 聚合行各自压——
        //   否则同商户多票种落同一条规则时会被压 N 次=N×封顶,违背「商户级规则=该商户当月封顶」。改为按【商户 × 解析到的规则 id】
        //   把封顶前原始费聚合后再压:商户级规则=该商户当月封顶、票种级规则=该票种当月封顶、全局规则=各商户各自当月封顶(不跨商户合并)。
        //   两趟:①逐聚合行累计 商户毛额 + (商户,规则)原始费/cap ②各组封顶 → 商户净额一条 exclusive_full 明细。
        java.util.LinkedHashMap<String, long[]> merchantAgg = new java.util.LinkedHashMap<>();
        // P6:merchantAgg[mid] = {grossSum, verifiedCount, guestCount, compServiceFeeSum}
        //   compServiceFeeSum = 每笔核销/冲正落库的【实收】综合服务费之和(核销 fee>=0·增量封顶后·冲正 fee<=0·取原行之反)。
        for (var ex : exclusives) {
            // D6：项目按归属承担的折扣加回该商户毛额，使其拿满额(platform_absorb 全额/split_by_ratio 平台份额/merchant_absorb=0)。
            //   综合服务费在【核销时】已在此同口径毛额上算并落库(见 ComprehensiveServiceFee + SettlementSettledConsumer);
            //   此处只用毛额算净额，不再月末重算 fee。折扣=0 时 gross=revenue → 与 D6 前逐分一致(零行为变更)。
            long platformBorne = platformBorneDiscount(discountCfg, ex.discountCent());
            long gross = ex.revenueCent() + platformBorne;
            comboDiscountCent += platformBorne; // 项目承担的专属折扣入账
            long[] mg = merchantAgg.computeIfAbsent(ex.merchantId(), k -> new long[4]);
            mg[0] += gross;
            mg[1] += ex.verifiedCount();
            mg[2] += ex.guestCount();
            // ✅ P6 守恒修复(2026-07-21·过账激活前必修·对抗审计坐实):综合服务费【不再月末对聚合 gross 重算】——
            //   旧 min(fixed×count,gross)/min(raw,cap) 是月度非线性(f(-x)≠-f(x))，过账开启后原核销月已过账时其跨月红冲落
            //   当月、聚合行 gross 带负，按费率/毛额重算 fee≠原月实收之反 → 触顶/固定额场景项目倒亏商户多拿、net<0 撞
            //   chk_si_amount 崩。改为【每笔实收 fee 之和】(核销时按每笔+增量封顶落库、冲正严格取原行之反)→ 固定额/封顶/
            //   比例三模式跨月红冲全守恒。封顶已在核销时按「商户×规则」按月增量应用，此处无需再 clamp。
            mg[3] += ex.compServiceFeeCent();
        }
        long exclusiveServiceFee = 0L;
        // 商户净额 = 毛额合计 − 该商户当月【实收】综合服务费合计;各商户一条 exclusive_full 直付明细(同商户多票种合并)。
        for (var e : merchantAgg.entrySet()) {
            long[] mg = e.getValue();
            long merchantFee = mg[3];
            exclusiveServiceFee += merchantFee;
            long net = mg[0] - merchantFee;
            items.add(new ItemCalc(e.getKey(), (int) mg[1], (int) mg[2], null, net, METHOD_EXCLUSIVE_FULL));
        }

        // distributed = 池分账 + 专属直付（两口径并列写入同一结算单明细）。
        long distributed = items.stream().mapToLong(ItemCalc::amount).sum();

        // ⑧ 校验 Σ<b>池分账</b> ≤ 可分配池(§5.0 步骤⑧);专属直付不占池、单独净额，故不纳入越池判定。
        //    超出 → status 保持 draft 待人工特批(special_approval_ref=null 待填),不自动确认;
        //    未超 → status=confirmed(可确认;过账仍在批2-4,confirmed 只表引擎校验通过)。
        long pooledDistributed = items.stream()
                .filter(i -> !METHOD_EXCLUSIVE_FULL.equals(i.method()))
                .mapToLong(ItemCalc::amount).sum();
        // R-7 尾差:应分总额(对各家精确份额合计只舍入一次) − Σ各家已舍入分账额。归项目公司/平台兜底,
        //   单列落 bill.rounding_residual_cent 留痕(V343),不并进任何一家、也不静默沉淀在「总池 − Σ分账」里;
        //   责任主体见下方 roundingResidualEntityId(V383)。
        //   专属直付(exclusive_full)明细是「毛额 − 每笔实收服务费」的整数补数,不产生舍入 → 不进尾差口径。
        long roundingResidual = MoneyRounding.residual(pooled.exactTotal(), pooledDistributed);
        verifyConservation(pooled.exactTotal(), pooledDistributed, roundingResidual, pooled.items().size());
        verifyFormatSplit(formatShares.values(), ticketRevenue, channelFee, channelFeeResidual,
                pooledServiceFee, pooledGiftCost);
        // 越池判定按【应分总额】(= Σ已舍入 + 尾差)而非 Σ已舍入:特批闸管的是「业务上分超了池」,
        //   不该被 ±n/2 分的舍入尾差触发(尾差本就由平台兜底,不需要为几分钱把整月单打回待特批)。
        //   无舍入(按客数/固定额/比例整除得尽)时应分总额 ≡ Σ已舍入 → 与改造前逐分同判(零行为变更)。
        long designedPooledTotal = pooledDistributed + roundingResidual;
        boolean overPool = designedPooledTotal > availablePool;
        String status = overPool ? STATUS_DRAFT : STATUS_CONFIRMED;
        String specialApprovalRef = null; // 越池时留 null 待人工填;未越池时也 null(无需特批)

        // 六业态收入归属:通票收入归属的 L3 管理公司(娱乐平台公司),按 (项目, all_pass_ticket) 可配置解析;
        //   项目特例优先→全局默认→未配置返 null(仅记归属,不改任何金额)。
        String attributedEntityId =
                attributionRepository.resolveManagementEntity(projectId, REVENUE_STREAM_ALL_PASS_TICKET);
        // S8/K18 ③④:第二段无参与者时的池余额——显式归娱乐平台公司(= all_pass_ticket 流的归属主体,
        //   V375 种全局默认 be_ent_platform)+ 告警留痕。不是静默沉淀:金额与归属主体单列落 bill。
        long poolUndistributed = pooled.undistributedCent();
        String poolUndistributedEntityId = poolUndistributed == 0L ? null : attributedEntityId;
        if (poolUndistributed != 0L) {
            log.warn("结算告警:项目 {} 月 {} 可分配池余额 {} 分无第二段参与者(排名档位酒吧为空或权重全 0),"
                            + "显式归娱乐平台公司(主体 {})留痕待查",
                    projectId == null ? "平台默认" : projectId, month, poolUndistributed,
                    poolUndistributedEntityId == null ? "未配置(归属留白,金额已单列)" : poolUndistributedEntityId);
        }
        // S4 综合服务费收款主体快照:两支都归娱乐平台公司(V370 种子;项目级可覆盖),月结时定格进单据——
        //   财务对账时「这笔费归了谁」直接看单据,不用追当时配置长什么样。
        String feeBeneficiaryEntityId =
                attributionRepository.resolveManagementEntity(projectId, REVENUE_STREAM_TICKET_SERVICE_FEE);
        // V383 尾差责任主体(治理地图「全局原则 · 金额一律精确到分」2026-08-01 翻转后的现行口径):
        //   池分账舍入尾差与渠道费分摊尾差同为「按比例拆分四舍五入」成因 → 合成一笔单列,
        //   挂到【实际承担结算责任的主体】—— 通票分账这笔业务就是 all_pass_ticket 流的归属主体
        //   (娱乐平台公司,后台可配、项目级可覆盖),与 attributedEntityId 同源解析不另写一套。
        //   尾差为 0 → 主体置空(没有尾差就没有责任人),与 pool_undistributed_entity_id 同范式。
        long totalRoundingResidual = roundingResidual + channelFeeResidual;
        String roundingResidualEntityId = totalRoundingResidual == 0L ? null : attributedEntityId;
        // S6 G 收款主体快照:归娱乐平台公司(V372 种子;项目级可覆盖),月结时定格进单据。
        String giftBeneficiaryEntityId =
                attributionRepository.resolveManagementEntity(projectId, REVENUE_STREAM_PASS_GIFT_COST);

        String billId = idFor(projectId, month);
        repository.upsertBill(new SettlementBillRecord(
                billId, projectId, periodStart, periodEnd,
                ticketRevenue, channelFee, availablePool, distributed,
                pooledServiceFee, exclusiveServiceFee, feeBeneficiaryEntityId,
                pooledGiftCost, giftBeneficiaryEntityId,
                method, specialApprovalRef, status, attributedEntityId, comboDiscountCent,
                totalRoundingResidual, roundingResidualEntityId,
                poolUndistributed, poolUndistributedEntityId));

        // 幂等重跑:先清旧明细再写(明细集合随当月核销变化,覆盖式最简)
        String actualBillId = repository.findBillId(projectId, periodStart, periodEnd);
        repository.deleteItemsByBill(actualBillId);
        // S11④ 各业态结算出口的留痕:每个业态一行「毛额 / 折扣补回 / 渠道费 / 净份额 / 归谁」。
        //   本批只保证份额算得出、归属定得住、非酒吧业态不进酒吧池;各业态自己的完整结算单另批。
        repository.deleteFormatSharesByBill(actualBillId);
        for (FormatShare share : formatShares.values()) {
            String kind = ENTITY_BENEFICIARY_FORMATS.containsKey(share.format()) ? BENEFICIARY_ENTITY : BENEFICIARY_PER_MERCHANT;
            String entityId = BENEFICIARY_ENTITY.equals(kind)
                    ? attributionRepository.resolveManagementEntity(
                            projectId, ENTITY_BENEFICIARY_FORMATS.get(share.format()))
                    : null;
            repository.upsertFormatShare(new SettlementBillFormatShareRecord(
                    actualBillId, share.format(), share.grossCent(), share.platformBorneCent(),
                    share.channelFeeCent(), share.compServiceFeeCent(), share.giftCostCent(),
                    share.giftPaidCent(), share.netCent(), kind, entityId));
        }
        // S7 差额台账落痕(append-only):首结追加陈述行;幂等重算若结论有变,先冲正取反旧净额、再追加新陈述,
        //   历史结论永在;结论没变不追加(重跑零膨胀)。收款主体快照与 G 同源(pass_gift_cost 归属流)。
        writeGiftVarianceLedger(actualBillId, projectId, periodStart, periodEnd,
                giftVariance, giftBeneficiaryEntityId);
        for (ItemCalc item : items) {
            // 专属直付明细 id 加 method 后缀,与该家池分账明细 id 区分(否则 PK/唯一键相撞被覆盖丢账)。
            String itemId = METHOD_EXCLUSIVE_FULL.equals(item.method())
                    ? "si_" + actualBillId + "_" + item.merchantId() + "_excl"
                    : "si_" + actualBillId + "_" + item.merchantId();
            repository.upsertItem(new SettlementItemRecord(
                    itemId,
                    actualBillId, item.merchantId(),
                    item.verifiedCount(), item.guestCount(), item.rankNo(),
                    item.method(), item.amount(), item.amount(), ITEM_STATUS_DRAFT));
        }
        return actualBillId;
    }

    /**
     * S8 两段分配(业务 HTML「可分配池按两段分配」逐字落地;整数分,四舍五入到分)。
     *
     * <p><b>第一段(绝对额优先)</b>:有 active 商户级固定规则(V374 rule_type 二选一)的酒吧,按
     * 每客单价 × 当月客数 / 固定额拿<b>绝对金额</b>(整数乘法无舍入,不归一化、不削);
     * <b>第二段(归一化分尽)</b>:其余酒吧按排名档位权重对余额 P2 一次归一化分光
     * ({@code b_j = round_half_up(P2 × w_j / Σw)},只舍入一次),档位只是相对权重——
     * 权重和不足 10000 也分尽(和恰为 10000 时与两段化前 bpsOf 逐分一致)。
     * 排名在<b>第二段参与者内部</b>编(拿第一段的不占名次,K18 ⑤)。
     *
     * <p>边界:P2 = 第一段为空时直传池额(带符号,负池月第二段分负额=向酒吧追回,R-3 不变);
     * 第一段非空时 max(0, P − Q)(绝对承诺不削,Q &gt; P 即超池,由上层按应分总额判定落特批);
     * 第二段无参与者/权重全 0 → 余额进 {@link PooledDistribution#undistributedCent()},
     * 上层显式归娱乐平台公司 + 告警(K18 ③④,不静默沉淀)。
     *
     * <p>R-7:exactTotal = Q + 第二段基数(两者皆整数分,归一化保证 Σ精确份额 ≡ 基数),
     * 供上层算尾差 = round_half_up(Σ精确份额) − Σ已舍入份额;只有第二段可能产生尾差。
     */
    private PooledDistribution distribute(String projectId, String method, long pool,
                                          List<JdbcSettlementBillRepository.MerchantMonthlyStat> stats) {
        // 语义收窄(V374):method 仅指第二段方式,只有 by_rank_tier 有意义。旧三选一里的另两个值
        // 已由商户级固定规则取代——若配置仍是旧值,静默按档位算会与配置页显示的口径对不上,fail-loud。
        if (!METHOD_BY_RANK_TIER.equals(method)) {
            throw com.complex.platform.common.api.ApiException.conflict("SETTLEMENT_CONFIG_INVALID",
                    "分配方式「" + method + "」已由商户级固定规则(每客单价/固定额,第一段)取代;"
                            + "第二段仅支持排名档位(by_rank_tier),请在结算配置中心改配分配方式");
        }
        java.util.Map<String, JdbcSettlementBillRepository.MerchantRateRule> rules =
                repository.findActiveMerchantRateRules(projectId);
        List<ItemCalc> out = new ArrayList<>();

        // ---- 第一段:FixedSet 按绝对金额先拿(绝对承诺,不归一化、不削)----
        long q = 0L;
        List<JdbcSettlementBillRepository.MerchantMonthlyStat> tierSet = new ArrayList<>();
        for (var s : stats) {
            var rule = rules.get(s.merchantId());
            if (rule == null) {
                tierSet.add(s);
                continue;
            }
            boolean perGuest = JdbcSettlementBillRepository.RULE_PER_GUEST.equals(rule.ruleType());
            long amount = perGuest
                    ? (long) s.guestCount() * rule.perGuestCent()   // 每客单价 × 当月客数,整数乘法无舍入
                    : rule.fixedAmountCent();                        // 固定额
            q = Math.addExact(q, amount);
            out.add(new ItemCalc(s.merchantId(), s.verifiedCount(), s.guestCount(), null, amount,
                    perGuest ? METHOD_BY_VENUE_PER_GUEST : METHOD_BY_FIXED_AMOUNT));
        }
        boolean fixedSetEmpty = out.isEmpty();

        // ---- 第二段基数 P2 ----
        // FixedSet 空 → P2 = 池额带符号直传(负池月第二段分负额=向酒吧追回,与两段化前逐分一致);
        // FixedSet 非空 → P2 = max(0, P − Q):绝对承诺不削,Q > P 时第二段为 0,
        //   上层按应分总额(= Q)> 池判超池 → draft 待特批(§二十一.1,第二段永不加剧超池)。
        long p2 = fixedSetEmpty ? pool : Math.max(0L, pool - q);

        // ---- 第二段:TierSet 按排名档位权重归一化分尽 P2 ----
        List<JdbcSettlementBillRepository.RankTier> tiers = repository.findRankTiers(projectId);
        // tierSet 保持 stats 的 verified_count DESC 序 → 顺位即第二段内部排名(1-based)
        int[] weights = new int[tierSet.size()];
        long weightSum = 0L;
        for (int i = 0; i < tierSet.size(); i++) {
            weights[i] = shareBpsForRank(tiers, i + 1);
            weightSum += weights[i];
        }
        long undistributed = 0L;
        long tierBase = 0L;
        if (weightSum > 0L) {
            tierBase = p2;
            for (int i = 0; i < tierSet.size(); i++) {
                var s = tierSet.get(i);
                // 一次归一化算到位(不「先分一轮再分剩余」):只舍入一次,尾差走既有 rounding_residual 范式。
                long amount = MoneyRounding.prorate(p2, weights[i], weightSum);
                out.add(new ItemCalc(s.merchantId(), s.verifiedCount(), s.guestCount(), i + 1, amount,
                        METHOD_BY_RANK_TIER));
            }
        } else {
            // K18 ③④:第二段无参与者(TierSet 空)或权重全 0(档位没配/名次全脱档)→ 余额不静默沉淀,
            // 显式归娱乐平台公司(由上层解析主体 + 告警 + bill 单列留痕)。名次仍留痕(分 0)。
            for (int i = 0; i < tierSet.size(); i++) {
                var s = tierSet.get(i);
                out.add(new ItemCalc(s.merchantId(), s.verifiedCount(), s.guestCount(), i + 1, 0L,
                        METHOD_BY_RANK_TIER));
            }
            undistributed = p2;
        }
        // exactTotal:Q 与第二段基数皆整数分;归一化保证第二段 Σ精确份额 ≡ tierBase,不必逐家累精确值。
        return new PooledDistribution(out, BigDecimal.valueOf(Math.addExact(q, tierBase)), undistributed);
    }

    /**
     * R-7 资金守恒运行期自检（fail-loud，宁可拒绝出账也不出一张不守恒的结算单）。
     *
     * <p>S4 起池链只有一道数(可分配池 = 酒吧净份额,扣项都在业态行上逐列可见),原「池 = 留存 + 总池」
     * 恒等式随留存链删除;拆份额链的守恒由 {@link #verifyFormatSplit} 把关。本方法管分账链:
     * ① 尾差单列后严格相等：{@code Σ池分账 + 尾差 = 应分总额}。
     * ② 尾差量级合理：|尾差| ≤ 参与分账商户数（每家最多贡献 0.5 分的舍入差，n 家上界 n/2，取 n 更宽松）；
     *    越界说明分配算法本身出错（而非舍入），必须响亮失败而不是把差额记成「尾差」蒙混过关。
     */
    private void verifyConservation(BigDecimal exactPooledTotal, long pooledDistributed,
                                    long roundingResidual, int itemCount) {
        long designedTotal = MoneyRounding.toCent(exactPooledTotal);
        if (pooledDistributed + roundingResidual != designedTotal) {
            throw com.complex.platform.common.api.ApiException.conflict("SETTLEMENT_CONSERVATION_VIOLATED",
                    "结算资金守恒校验失败(分账链):Σ池分账 " + pooledDistributed + " + 尾差 " + roundingResidual
                            + " ≠ 应分总额 " + designedTotal);
        }
        if (Math.abs(roundingResidual) > Math.max(1L, itemCount)) {
            throw com.complex.platform.common.api.ApiException.conflict("SETTLEMENT_CONSERVATION_VIOLATED",
                    "结算尾差异常(疑分配算法错而非舍入):尾差 " + roundingResidual
                            + " 超出参与分账商户数 " + itemCount + " 的合理上界");
        }
    }

    /**
     * S11② 把<b>整单</b>算出来的渠道抽成按各业态毛额占比摊下去（就地替换 map 里的份额行）。
     *
     * <p>业务上：渠道抽的是客人实付那一整笔的成，谁分得多谁就多担一点，不该由酒吧一家全扛。
     * 只舍入一次：各家（<b>含酒吧</b>）按精确占比 HALF_UP。
     *
     * <p><b>分摊尾差不再并进酒吧支</b>（2026-08-03）：治理地图「全局原则 · 金额一律精确到分」
     * 明写尾差「不得悄悄并进任一参与方」—— 酒吧也是参与方，旧写法让它拿补数就是让它替全场认这几分，
     * 对账时连一行留痕都没有。改为<b>返回尾差</b>，由调用方合进单列的 {@code rounding_residual_cent}（V383），
     * 挂到实际结算责任主体头上。
     *
     * <p>渠道费 0（官方直销 / 未配费率）→ 各家 +0、尾差 0 → 与改造前逐分一致。
     *
     * @return 分摊尾差（整单渠道费 − Σ各业态已舍入分摊额），调用方必须单列留痕
     */
    private long allocateChannelFee(java.util.LinkedHashMap<String, FormatShare> shares,
                                    long channelFee, long totalGross) {
        if (channelFee == 0L) {
            return 0L; // 各业态渠道费恒 0（默认口径）→ 净份额 = 毛额。
        }
        if (shares.isEmpty() || totalGross == 0L) {
            // 无份额行 / 毛额合计为 0（当月全额红冲）却仍有渠道费 → 无占比可依：
            // 整笔进尾差、由责任主体承担（旧写法整笔压给酒吧），不静默丢、也不压给任一参与方。
            return channelFee;
        }
        long assigned = 0L;
        for (var e : shares.entrySet()) {
            long fee = MoneyRounding.prorate(channelFee, e.getValue().grossCent(), totalGross);
            e.setValue(e.getValue().withChannelFee(fee));
            assigned += fee;
        }
        return channelFee - assigned; // 分摊尾差：调用方单列留痕，不压给酒吧。
    }

    /**
     * K20 业态份额拆分守恒（fail-loud）：{@code Σ各业态净份额 + 渠道费 + 赠送权益预估成本 + 综合服务费(池链支) ≡ 通票收入毛额}，
     * 且 {@code Σ各业态渠道费 ≡ 整单渠道费}、{@code Σ各业态综合服务费 ≡ 池链支合计}、{@code Σ各业态 G ≡ G 合计}。
     * 分摊写错会让某个业态凭空多拿或少拿——这种错在月末对账时极难查（数字都「差不多对」），所以宁可当场拒绝出账。
     */
    private void verifyFormatSplit(java.util.Collection<FormatShare> shares, long ticketRevenue,
                                   long channelFee, long channelFeeResidual,
                                   long pooledServiceFee, long pooledGiftCost) {
        long netTotal = 0L;
        long feeTotal = 0L;
        long svcFeeTotal = 0L;
        long giftCostTotal = 0L;
        for (FormatShare s : shares) {
            netTotal += s.netCent();
            feeTotal += s.channelFeeCent();
            svcFeeTotal += s.compServiceFeeCent();
            giftCostTotal += s.giftCostCent();
        }
        if (feeTotal + channelFeeResidual != channelFee) {
            throw com.complex.platform.common.api.ApiException.conflict("SETTLEMENT_CONSERVATION_VIOLATED",
                    "业态份额守恒校验失败(渠道费分摊):Σ各业态渠道费 " + feeTotal
                            + " + 分摊尾差 " + channelFeeResidual + " ≠ 整单渠道费 " + channelFee);
        }
        if (svcFeeTotal != pooledServiceFee) {
            throw com.complex.platform.common.api.ApiException.conflict("SETTLEMENT_CONSERVATION_VIOLATED",
                    "业态份额守恒校验失败(综合服务费):Σ各业态综合服务费 " + svcFeeTotal
                            + " ≠ 池链支合计 " + pooledServiceFee);
        }
        if (giftCostTotal != pooledGiftCost) {
            throw com.complex.platform.common.api.ApiException.conflict("SETTLEMENT_CONSERVATION_VIOLATED",
                    "业态份额守恒校验失败(赠送权益预估成本):Σ各业态 G " + giftCostTotal
                            + " ≠ G 合计 " + pooledGiftCost);
        }
        // 拆份额链用【各业态已摊到的渠道费】而非整单渠道费:分摊尾差不减任何一家的净份额,
        //   它单列到 rounding_residual_cent 由责任主体承担(V383)。
        if (netTotal + feeTotal + pooledGiftCost + pooledServiceFee != ticketRevenue) {
            throw com.complex.platform.common.api.ApiException.conflict("SETTLEMENT_CONSERVATION_VIOLATED",
                    "业态份额守恒校验失败(拆份额):Σ各业态净份额 " + netTotal
                            + " + Σ各业态渠道费 " + feeTotal
                            + " + 赠送权益预估成本 " + pooledGiftCost
                            + " + 综合服务费 " + pooledServiceFee + " ≠ 通票收入 " + ticketRevenue);
        }
    }

    /** 命中排名档位区间 [rank_from, rank_to] → share_bps;无命中 → 0（该家不分）。 */
    private int shareBpsForRank(List<JdbcSettlementBillRepository.RankTier> tiers, int rank) {
        for (var t : tiers) {
            if (rank >= t.rankFrom() && rank <= t.rankTo()) {
                return t.shareBps();
            }
        }
        return 0;
    }

    private String idFor(String projectId, YearMonth month) {
        return "sb_" + (projectId == null ? "default" : projectId) + "_" + month;
    }

    /** S7 三种差额处理规则(V373 快照定义;chk_pgc_variance/chk_pgcv_handling 同一词表)。 */
    static final String VARIANCE_CARRY_FORWARD = "carry_forward";
    static final String VARIANCE_TRUE_UP_POOL = "true_up_pool";

    /**
     * S7 赠付差额结算(HTML:962):按「项目 × 规则」结出 当期差额 V = 预扣 G − 实付 A,并按规则算出处理量。
     *
     * <p><b>G 按规则</b>:计提行落库时快照的 gift_cost_config_id 分组(冲正行沿用原行规则 id,净额天然正确);
     * <b>A 按规则</b>:兑付行按其核销的票种走与计提<b>同一个</b> {@code findPassGiftCostConfig} 归到规则——
     * 预扣关闭(整额 0,无计提行)时兑付照样归到该规则,差额为负 = 平台没预扣却真金白银付了出去,财务必须看见。
     * <b>carried_in</b>:此前各期 carry_forward 结转、尚未处理的净额,本期一并按本期规则处理。
     *
     * <p>处理量 = V + carried_in:platform_absorb → 两处理列全 0(差额留娱乐平台公司,仅留痕);
     * carry_forward → 全额结转下期;true_up_pool → 全额回补当期池(池 = 酒吧净份额 + Σ回补)。
     * G/A/结转全为 0 的规则不出行(留痕表零膨胀;K9 逐分不变基线 = 不落任何行)。
     *
     * <p>K12 接线守卫(fail-loud):Σ各规则 G 必须 ≡ 进池 G 合计——G 计提行永远带规则 id(S6 落库同写),
     * 若有 G 落库却没带规则 id,差额表会悄悄少一块,宁可当场拒绝出账。
     */
    private GiftVarianceCalc settleGiftVariance(String projectId, LocalDate periodStart,
                                                Instant windowStart, Instant windowEnd, long pooledGiftCost) {
        List<JdbcSettlementBillRepository.ConfigGiftCost> gRows =
                repository.aggregateGiftCostByConfig(projectId, windowStart, windowEnd);
        java.util.LinkedHashMap<String, Long> gByConfig = new java.util.LinkedHashMap<>();
        long gSum = 0L;
        for (var g : gRows) {
            gByConfig.merge(g.configId(), g.giftCostCent(), Long::sum);
            gSum += g.giftCostCent();
        }
        if (gSum != pooledGiftCost) {
            throw com.complex.platform.common.api.ApiException.conflict("SETTLEMENT_CONSERVATION_VIOLATED",
                    "赠付差额守恒校验失败(K12 接线):Σ各规则预扣 G " + gSum + " ≠ 进池计提合计 " + pooledGiftCost
                            + "(存在未带规则 id 的 G 计提行)");
        }
        java.util.LinkedHashMap<String, Long> aByConfig = new java.util.LinkedHashMap<>();
        java.util.LinkedHashMap<String, Long> paidByFormat = new java.util.LinkedHashMap<>();
        for (var p : repository.aggregateGiftPaid(projectId, windowStart, windowEnd)) {
            paidByFormat.merge(p.businessFormat(), p.paidCent(), Long::sum);
            aByConfig.merge(repository.findPassGiftCostConfig(projectId, p.ticketTypeId()).id(),
                    p.paidCent(), Long::sum);
        }
        java.util.LinkedHashSet<String> configIds = new java.util.LinkedHashSet<>(gByConfig.keySet());
        configIds.addAll(aByConfig.keySet());
        List<VarianceTarget> targets = new ArrayList<>();
        long poolTrueUpTotal = 0L;
        for (String configId : configIds) {
            long accrued = gByConfig.getOrDefault(configId, 0L);
            long paid = aByConfig.getOrDefault(configId, 0L);
            long carriedIn = repository.sumPriorCarry(projectId, configId, periodStart);
            if (accrued == 0L && paid == 0L && carriedIn == 0L) {
                continue; // 本期本规则无任何量,不出行。
            }
            String handling = repository.findVarianceHandling(configId);
            long effective = (accrued - paid) + carriedIn; // 本期待处理量 = 当期差额 + 往期结转
            long carriedForward = VARIANCE_CARRY_FORWARD.equals(handling) ? effective : 0L;
            long poolTrueUp = VARIANCE_TRUE_UP_POOL.equals(handling) ? effective : 0L;
            poolTrueUpTotal = Math.addExact(poolTrueUpTotal, poolTrueUp);
            targets.add(new VarianceTarget(configId, handling, accrued, paid, carriedIn,
                    carriedForward, poolTrueUp));
        }
        return new GiftVarianceCalc(targets, poolTrueUpTotal, paidByFormat);
    }

    /**
     * S7 差额台账写入(append-only,冲正取反范式):
     * 首结 → 每个有量的规则追加一行陈述;幂等重算 → 逐规则与台账现净额比对,结论没变不追加(零膨胀),
     * 变了则先追加「现净额的全额取反」冲正行(reversal_of 指向被冲陈述行),再追加新陈述行——
     * 历史结论永在,任何一行都满足 chk_pgcv_identity(V = G − A,取反后仍成立)。
     */
    private void writeGiftVarianceLedger(String billId, String projectId, LocalDate periodStart,
                                         LocalDate periodEnd, GiftVarianceCalc calc, String beneficiaryEntityId) {
        java.util.Map<String, JdbcSettlementBillRepository.VarianceNet> existing =
                repository.findVarianceNetByBill(billId);
        java.util.LinkedHashMap<String, VarianceTarget> targetByConfig = new java.util.LinkedHashMap<>();
        for (VarianceTarget t : calc.targets()) {
            targetByConfig.put(t.configId(), t);
        }
        java.util.LinkedHashSet<String> configIds = new java.util.LinkedHashSet<>(targetByConfig.keySet());
        configIds.addAll(existing.keySet());
        int seq = 0;
        for (String configId : configIds) {
            VarianceTarget target = targetByConfig.get(configId);
            JdbcSettlementBillRepository.VarianceNet net = existing.get(configId);
            boolean unchanged = target == null
                    ? (net == null || net.isZero())
                    : (net != null
                        && net.accruedCostCent() == target.accruedCostCent()
                        && net.actualPaidCent() == target.actualPaidCent()
                        && net.carriedInCent() == target.carriedInCent()
                        && net.carriedForwardCent() == target.carriedForwardCent()
                        && net.poolTrueUpCent() == target.poolTrueUpCent());
            if (unchanged) {
                continue;
            }
            if (net != null && !net.isZero()) {
                // 冲正行:现净额全额取反(处理规则按该规则现行快照取,与被冲行常同;规则中途改配亦如实留痕)。
                repository.insertVarianceRow(new JdbcSettlementBillRepository.VarianceLedgerRow(
                        varianceRowId(seq++), billId, projectId, periodStart, periodEnd, configId,
                        repository.findVarianceHandling(configId),
                        -net.accruedCostCent(), -net.actualPaidCent(), -net.varianceCent(),
                        -net.carriedInCent(), -net.carriedForwardCent(), -net.poolTrueUpCent(),
                        beneficiaryEntityId, net.latestStatementId()));
            }
            if (target != null) {
                repository.insertVarianceRow(new JdbcSettlementBillRepository.VarianceLedgerRow(
                        varianceRowId(seq++), billId, projectId, periodStart, periodEnd, configId,
                        target.varianceHandling(),
                        target.accruedCostCent(), target.actualPaidCent(),
                        target.accruedCostCent() - target.actualPaidCent(),
                        target.carriedInCent(), target.carriedForwardCent(), target.poolTrueUpCent(),
                        beneficiaryEntityId, null));
            }
        }
    }

    /** S7 台账行 id:零填充纳秒前缀保证字典序≈时间序(冲正行可按 MAX(id) 找到最新陈述行)。 */
    private String varianceRowId(int seq) {
        return "pgv_" + String.format("%016x", System.nanoTime()) + "_" + seq;
    }

    /** S7 差额结算中间结果:各规则处理行 + 回补池合计(true_up_pool) + 兑付实付按业态归堆(落业态份额行)。 */
    private record GiftVarianceCalc(List<VarianceTarget> targets, long poolTrueUpTotal,
                                    java.util.Map<String, Long> paidByFormat) {
    }

    /** S7 某规则本期结论:G/A/往期结转 + 按规则算出的处理量(结转下期/回补当期池,platform_absorb 两者皆 0)。 */
    private record VarianceTarget(String configId, String varianceHandling, long accruedCostCent,
                                  long actualPaidCent, long carriedInCent, long carriedForwardCent,
                                  long poolTrueUpCent) {
    }

    /**
     * D6 套票折扣按归属拆出「项目公司承担」的部分(§7.4)：
     * platform_absorb → 项目全额承担(商户拿满额)；split_by_ratio → 项目按 split_platform_bps 承担；
     * merchant_absorb → 项目 0(商户自担，池/直付按净额=现状)。
     *
     * <p>🔴 <b>带符号(不钳0)</b>:核销为正(平台吸收折扣、商户按满额分),该核销在<b>后续已过账月</b>被冲正时为负
     * (平台<b>追回</b>先前吸收的折扣)——与收入侧 verified_amount_cent 带符号跨月净掉<b>一致</b>(V335 放开 combo_discount ≥0)。
     * 旧「discountCent&lt;=0 钳0」会丢弃跨月负向追回 → 收入 -R 追回了、折扣 -D 没追回,项目多付(套票真上线前必修)。
     * R-7 改 HALF_UP 后仍<b>对 0 对称</b>({@code f(-x)=-f(x)},HALF_UP 远离零进位),split_by_ratio 的正负份额
     * 依旧逐分精确对消;折扣=0 → 0(无折扣零行为变更)。
     */
    private long platformBorneDiscount(JdbcSettlementBillRepository.ComboDiscountConfig cfg, long discountCent) {
        return switch (cfg.discountBearer()) {
            case "platform_absorb" -> discountCent;
            case "split_by_ratio" -> MoneyRounding.bpsOf(discountCent, cfg.splitPlatformBps());
            default -> 0L; // merchant_absorb（及未知值防御）：项目不承担
        };
    }

    /**
     * S11 某业态当月的份额（计算中间结果）：毛额 = 已核销净额 + 项目承担的折扣补回；
     * S4/S6:净份额 = 毛额 − 摊到的渠道费 − 本业态计提的赠送权益预估成本(G) − 本业态计提的门票综合服务费
     * (池链支;G 与 F 都归娱乐平台公司,瀑布顺序照 HTML:739 先 G 后 F——减法交换律下金额同,列示按此序)。
     * S7 giftPaidCent:本业态当月兑付实付(A,按提供方业态分记,HTML:962)——纯留痕<b>不进净份额</b>
     * (兑付由娱乐平台公司承担,不动各业态分账)。
     */
    private record FormatShare(String format, long grossCent, long platformBorneCent, long channelFeeCent,
                               long compServiceFeeCent, long giftCostCent, long giftPaidCent) {
        FormatShare(String format, long grossCent, long platformBorneCent) {
            this(format, grossCent, platformBorneCent, 0L, 0L, 0L, 0L);
        }

        FormatShare(String format, long grossCent, long platformBorneCent, long channelFeeCent,
                    long compServiceFeeCent, long giftCostCent) {
            this(format, grossCent, platformBorneCent, channelFeeCent, compServiceFeeCent, giftCostCent, 0L);
        }

        FormatShare withChannelFee(long fee) {
            return new FormatShare(format, grossCent, platformBorneCent, fee, compServiceFeeCent, giftCostCent,
                    giftPaidCent);
        }

        FormatShare withGiftPaid(long paid) {
            return new FormatShare(format, grossCent, platformBorneCent, channelFeeCent, compServiceFeeCent,
                    giftCostCent, paid);
        }

        long netCent() {
            return grossCent - channelFeeCent - giftCostCent - compServiceFeeCent;
        }
    }

    /** 分账中间结果（method：池分账=当月分配方式，专属直付=exclusive_full）。 */
    private record ItemCalc(String merchantId, int verifiedCount, int guestCount, Integer rankNo, long amount,
                            String method) {
    }

    /**
     * 池分账结果 + 各家<b>精确（未舍入）份额之和</b>：R-7 尾差 = round_half_up(exactTotal) − Σ items.amount。
     * exactTotal 只用于算尾差，绝不直接落库。
     * S8 undistributedCent：第二段无参与者/权重全 0 时的池余额（K18 ③④），上层显式归娱乐平台公司留痕；
     * 正常两段分尽时恒 0。带符号（冲正月可为负=由平台承接追回）。
     */
    private record PooledDistribution(List<ItemCalc> items, BigDecimal exactTotal, long undistributedCent) {
    }
}
