package com.complex.platform.settlement;

// 文件版本：V0.02

import com.complex.platform.ticketing.SettlementSettledEvent;
import java.time.YearMonth;
import java.time.ZoneOffset;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.stereotype.Component;

/**
 * 结算状态机重构(AUD-012/017)：核销结算消费者——每次核销写一条<b>不可变增量</b> verification_settled 事实。
 *
 * <p>监听 ticketing 域 {@link SettlementSettledEvent}，向 settlement_transactions append 一行，
 * key=transaction_no {@code ST-VS-<verificationId>}（幂等：同一核销重放 ON CONFLICT DO NOTHING，永不 UPDATE）。
 * account 按 {@code business_at}=核销业务时刻聚合增量，跨月核销各归其月，不再用累计值覆盖首插行（AUD-012 根因）。
 *
 * <p><b>AFTER_COMMIT + REQUIRES_NEW</b>：本消费不属核销主链——核销主事务提交后才触发，且开新事务写入，
 * 故本消费失败不回滚核销（append-only 账本容忍最终一致；缺失由重建任务按核销权威事实补建，AUD-017）。
 *
 * <p><b>金额口径</b>（整数分，本行只记本次增量，unverified 恒 0；未核销到期归项目是独立事实）：
 * verified = f(usedTimes) − f(usedTimes−1)（{@link SettlementAmounts}，逐笔 telescope 回累计，无漂移）。
 * allocatedRevenueCent==null 且非专属定价（direct 出票无分摊快照）→ 跳过不写（与 analytics 一致）。
 */
@Component
public class SettlementSettledConsumer {

    private static final String TRANSACTION_TYPE_VERIFICATION_SETTLED = "verification_settled";
    private static final String BENEFICIARY_MERCHANT = "merchant";
    private static final String STATUS_DRAFT = "draft";

    private final JdbcSettlementTransactionRepository repository;
    // P6:算每笔实收综合服务费需读费率/折扣配置 + 增量封顶月累计(项目 scope 取事件带下来的真值,与 settleMonth 同源)。
    private final JdbcSettlementBillRepository billRepository;
    // S6:按件计 G 基数走 ticketing 的统一解析器(注入同一 bean=预扣与兑付同源同价)。
    //   为什么注入而不是事件携带:①settlement→ticketing 依赖方向本就存在(事件类就在 ticketing 包);
    //   ②两模式选择/首笔判定/增量封顶全在 settlement 侧,ticketing 无从判断要不要算——事件携带
    //   会让每笔核销都白算一遍按件和(多数场景配的是按票计或非首笔,算了也不用);
    //   ③解析器只此一处取价,配置改价后未计提的票用新价,与兑付侧天然一致。
    private final com.complex.platform.ticketing.InternalSettlementPriceResolver priceResolver;
    // FS-013：单一酒吧门票售出时已逐票锁定综合服务费、按计费月另挂应收，核销时不再另抽。
    private final SingleBarServiceFeeService singleBarServiceFees;

    public SettlementSettledConsumer(JdbcSettlementTransactionRepository repository,
                                     JdbcSettlementBillRepository billRepository,
                                     com.complex.platform.ticketing.InternalSettlementPriceResolver priceResolver,
                                     SingleBarServiceFeeService singleBarServiceFees) {
        this.repository = repository;
        this.billRepository = billRepository;
        this.priceResolver = priceResolver;
        this.singleBarServiceFees = singleBarServiceFees;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSettlementSettled(SettlementSettledEvent event) {
        if (event.compensationItemId() != null) {
            // FS-012（BSM L800「补偿履约与原履约分别记录，原核销和原结算不回改」；CG 第 39 条「防止重复记原票分账」）：
            // 补偿新增履约由承担主体按履约时锁定价与提供方另行结算（补偿履约记录 + 补偿应付），不进原票分账事实。
            return;
        }
        int total = event.totalTimes();
        int used = event.usedTimes();
        Long allocated = event.allocatedRevenueCent();
        boolean exclusiveByPrice = SettlementAmounts.exclusiveByPrice(
                event.exclusiveMerchantId(), event.settlementPriceCent());

        if (!exclusiveByPrice && allocated == null) {
            // direct 出票无分摊快照且非专属定价：保守跳过不写（与 analytics 一致，避免凭空造 net）。
            return;
        }
        // 本次核销的不可变增量已核销金额（第 used 次核销的增量 = f(used)−f(used−1)，恒 >=0）。
        long verified = SettlementAmounts.incrementalVerified(
                allocated, total, used, exclusiveByPrice, event.settlementPriceCent());
        // D6 套票折扣增量：直接把折扣总额(满额−净额,恒>=0)按【与净额同一非专属增量斜坡】逐笔分配，
        //   而非「满额斜坡−净额斜坡再 clamp」——后者两条独立取整斜坡非单调时负增量被 clamp 成 0，会向 Σ 多计舍入分。
        //   本写法每笔增量恒>=0(满足 V331 核销行>=0 CHECK)且 Σ 恰=满额−净额(无泄漏)。
        //   fullPriceCent 为空 → 折扣 0（零行为变更）；exclusiveByPrice(专属按内部价直付) → 折扣恒 0（无套票折扣概念，
        //   不能走 incrementalVerified 的专属分支[会返回 price×1],故显式置 0）。绝不因缺数据 skip 核销。
        long discountIncrement = 0L;
        if (!exclusiveByPrice && event.fullPriceCent() != null) {
            long discountTotal = Math.max(0L, event.fullPriceCent() - allocated);
            discountIncrement = SettlementAmounts.incrementalVerified(discountTotal, total, used, false, null);
        }

        // 【并发闸】2026-08-03:S3 综合服务费与 S6 赠送权益预估成本两处都是「读该票已收合计 → 算 → 写」,
        //   锁前同一张票的两笔核销并发提交会各自读到「还没收过」→ 一张票收两道。
        //   两处锚点同为父票 → 只取一把父票锁、摆在两处判定之前共用(也避免两把锁交叉取);
        //   事务级锁随本消费的 REQUIRES_NEW 事务提交自动释放。
        if (event.parentTicketId() != null) {
            billRepository.lockAccrualKey("st_ticket:" + event.parentTicketId());
        }

        // 综合服务费实收(每笔·增量封顶)。毛额=净收入+项目承担折扣(与 settleMonth 同口径),
        //   固定额/比例算本笔费,再按「商户×规则」当月增量封顶(月内 Σfee≤cap),落库作不可变实收事实供冲正精确取反。
        //
        // 【现行口径】2026-08-03 按业务 HTML 逐字矫回(business-enum-quick-reference §「门票综合服务费」
        //   + business-system-map 同名条):「门票综合服务费只适用于酒吧相关票务,归娱乐平台公司。
        //   单一酒吧门票…并可设置月度封顶;酒吧通票和跨域组合通票中的酒吧部分也可按生效规则收取,但不得封顶。
        //   纯游乐票、纯游乐通票和演出票不向娱乐平台公司支付该费用。」
        //   ①【经营域闸】只对【酒吧业态】的核销计提。判据源 = 提供方商户 merchants.merchant_type,
        //      与 aggregateFormatRevenue 拆业态份额【同一列同一兜底】—— 两处不同源会出现「计提时算酒吧、
        //      月结时归游乐」,K20 业态守恒当场炸。游乐/餐饮/零售/传媒(演出)一分不抽。
        //      ⚠️ 本条推翻 S2(2026-07-28)旧注释里的「所有门票项目都要收一道」—— 那句在三份 HTML 里从来没有,
        //      现行原文明写「只适用于酒吧相关票务」。
        //   ②【维度】费率按【票种】解析(event.ticketTypeId()),不再按权益类型。业务人员跟商户谈的是票种费率,
        //      一张通票里酒水/游乐两类权益按两套费率各抽一道,对账时根本对不上「这张票收了多少」。
        //   ③【项目】projectId 传事件带下来的真值(核销人 token 派生),不再硬编码 null —— 原先项目级费率特例
        //      永远解析不到,后台配了也不生效。同一个值同时用于解析费率与写 project_id 列,两处不再各写各的。
        //   ④【封顶只对单一酒吧门票】HTML 原文:单一酒吧门票「可设置月度封顶」,酒吧通票/跨域组合通票的
        //      酒吧部分「不得封顶」。代码判据 = exclusive_merchant_id 非空 ⇔ 该权益专属某一家酒吧直付
        //      = 单一酒吧门票;为空 = 进池的通票酒吧份额 → 不走封顶分支。
        //   ⚠️ 本刀【不动引擎数值公式】:非专属那部分 fee 只落 settlement_transactions 作事实,
        //      settleMonth 仍只从专属聚合读 fee(F 进主链是 S4)。费率种子仍为 0 → 每笔费恒 0 → 分账逐分不变。
        //
        // S3(2026-07-28)「一张票只收一道」：口径出自 system-map §「门票综合服务费」——该费与娱乐平台公司留存
        //   是同一项、分账瀑布中只扣一次。计提时点＝该票【首笔核销】时（用户已拍板）。
        //   ①【锚点】父票 parent_ticket_id 随事件带下来；为空 = 不合规发放数据（V285 起每次出票都建父票实例，
        //      新发放恒有）→ 一律不计提，绝不退化成「按核销笔数各收一道」——那正是本刀要修的错。
        //   ②【只对固定额设闸】固定额是「一张票 X 元」，按笔计提会让一张五次票收五道（对账时一张票冒出 5 条同名费，
        //      业务解释不了）。比例模式不设闸：按核销增量逐笔抽，Σ 恒 = 费率 × 该票总收入，本就是同一道费的线性拆分，
        //      设闸反而会把后面几次核销的那部分收入白白漏掉 → 故比例模式逐分不变。
        //   ③【复位】冲正行取原行之反，该票净额抵回 0 → 锚点自动复位，下一次核销重新收一道（票还在、费该收）。
        long compServiceFeeCent = 0L;
        String compServiceFeeConfigId = null;
        // FS-013(BSM L755「单店门票综合服务费按前述计费月汇总后的应收总额另行收取」;裁决 2026-10-19 第 3 条「售出月」):
        //   售出时已锁定的单一酒吧门票不在核销行上再抽,票款按毛额结付、服务费单独挂应收(不轧差)。
        if (verified > 0L && event.parentTicketId() != null
                && !singleBarServiceFees.accruedAtSale(event.parentTicketId())
                && TicketDistributionSettlementService.FORMAT_BAR.equals(
                        billRepository.merchantBusinessFormat(event.merchantId()))) {
            var svcFee = billRepository.findComprehensiveServiceFee(
                    event.projectId(), event.exclusiveMerchantId(), event.ticketTypeId());
            long platformBorne = ComprehensiveServiceFee.platformBorneCent(
                    billRepository.findComboDiscountConfig(event.projectId()), discountIncrement);
            long perTicket = ComprehensiveServiceFee.perTicketCent(svcFee, verified + platformBorne, 1);
            if (perTicket > 0L && svcFee.fixedAmountCent() != null
                    && billRepository.sumTicketServiceFeeCent(event.parentTicketId()) != 0L) {
                perTicket = 0L; // 这张票已经收过这一道固定额了
            }
            if (perTicket > 0L) {
                if (svcFee.monthlyCapAmountCent() != null && event.exclusiveMerchantId() != null) {
                    // 增量封顶:查月内同「商户×规则」已落 fee 累计(含冲正负→已释放额度),本笔 fee=min(perTicket, 剩余额度)。
                    //   只有专属(单一酒吧门票)走到这里,各商户各自一桶逐分不变
                    //   (用户 2026-07-21 拍板 A「按费率规则各自封、全局规则各商户各自封顶」不变);
                    //   进池的通票酒吧份额按 HTML「不得封顶」已在上面的条件里排除。
                    // 封顶桶是【跨票】的(同商户×规则月内累计),父票锁管不住 → 再取一把按规则 id 的同款锁
                    //   (否则两笔并发核销都读到同一个 mtd,合计超出封顶)。不配封顶不付锁的代价。
                    billRepository.lockAccrualKey("st_fee_cap:" + svcFee.id());
                    YearMonth m = YearMonth.from(event.verifiedAt().atZone(ZoneOffset.UTC));
                    long mtd = billRepository.sumMonthToDateServiceFee(
                            event.exclusiveMerchantId(), svcFee.id(),
                            m.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC),
                            m.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC));
                    perTicket = Math.min(perTicket, Math.max(0L, svcFee.monthlyCapAmountCent() - mtd));
                }
                compServiceFeeCent = perTicket;
                compServiceFeeConfigId = svcFee.id();
            }
        }

        // S6 赠送权益预估成本(G,归娱乐平台公司)。业务口径出自 system-map §「通票收入分账全流程」(:739):
        //   「再扣掉赠送权益预估成本…预扣金额按票种配置——按件计＝票面各权益数量 × 内部结算价之和,
        //    按票计＝该票种赠送部分的售价整额」。
        //   ①【时点】该票【首笔进池核销】一次性计提(用户已拍板「首笔核销」;限进池行是因为 G 是池链扣项:
        //     专属直付不进池、其净额上不扣 G,若把 G 落在专属行上,月结的进池聚合根本看不见这笔钱。
        //     正常流首笔核销=入场(进池),此限制不改变计提时点)。锚点=parent_ticket_id,为空=不合规发放数据
        //     → 不计提(与 S3 综合服务费同一态度,绝不退化成按核销笔数各收一道)。
        //   ②【两模式】按票种配置(pass_gift_cost_configs,项目×票种):by_ticket_amount 直取配置整额;
        //     by_item 走解析器按票面算(商户让利型/有票面价/非提供方兑付类不进和;任一进和权益缺内部价
        //     → fail-closed 抛错,本笔结算事实缺失由重建任务补价后补建,绝不按 0 或票面价凑数)。
        //   ③【一张票一道】该票 G 净额非零 → 本笔置 0;冲正取原行之反把净额抵回 0 → 锚点复位可再计提。
        //   ④【增量封顶】按「项目×规则」月内累计,perTicket=min(perTicket, max(0, cap−mtd)),
        //     绝不月末 clamp(会破坏 f(−x)=−f(x))。种子 by_ticket_amount+0 → G 恒 0 → 上线逐分不变。
        long giftCostCent = 0L;
        String giftCostConfigId = null;
        if (event.parentTicketId() != null && event.exclusiveMerchantId() == null) {
            var giftCfg = billRepository.findPassGiftCostConfig(event.projectId(), event.ticketTypeId());
            long perTicket = "by_item".equals(giftCfg.predeductMode())
                    ? priceResolver.byItemGiftCostCent(event.ticketTypeId())
                    : giftCfg.ticketGiftAmountCent();
            if (perTicket > 0L && billRepository.sumTicketGiftCostCent(event.parentTicketId()) != 0L) {
                perTicket = 0L; // 这张票已经计提过 G 了
            }
            if (perTicket > 0L && giftCfg.monthlyCapAmountCent() != null) {
                // 同 S3 封顶:【项目×规则】月累计桶跨票,再取一把按规则 id 的同款锁(取锁顺序在费率规则之后)。
                billRepository.lockAccrualKey("st_gift_cap:" + giftCfg.id());
                YearMonth m = YearMonth.from(event.verifiedAt().atZone(ZoneOffset.UTC));
                long mtd = billRepository.sumMonthToDateGiftCost(
                        event.projectId(), giftCfg.id(),
                        m.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC),
                        m.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC));
                perTicket = Math.min(perTicket, Math.max(0L, giftCfg.monthlyCapAmountCent() - mtd));
            }
            if (perTicket > 0L) {
                giftCostCent = perTicket;
                giftCostConfigId = giftCfg.id();
            }
        }

        repository.insertImmutableFact(new SettlementTransactionRecord(
                "st_vs_" + event.verificationId(),
                "ST-VS-" + event.verificationId(),
                TRANSACTION_TYPE_VERIFICATION_SETTLED,
                event.entitlementId(),
                event.orderId(),
                event.ticketTypeId(),
                event.verificationId(), // AUD-012：绑本次核销记录（不可变增量行的溯源+幂等锚点）
                event.entitlementType(),
                1,                       // verified_count：本行=本次核销 1 次（累计由多行相加）
                total,
                event.numPeople(),
                verified,                // allocated_revenue_cent：增量行等于本次已核销额（无未核销余量）
                verified,
                0L,                      // unverified_amount_cent：增量行恒 0（未核销到期归项目是独立事实）
                BENEFICIARY_MERCHANT,
                event.merchantId(),
                event.projectId(), // S2：项目 scope 传真值（与上面解析费率用的是同一个值；平台级操作者=null=平台默认口径）
                STATUS_DRAFT,
                event.validTo(),
                event.exclusiveMerchantId(),
                event.verifiedAt(),      // business_at：核销业务时刻（account 月窗聚合基准）
                discountIncrement,       // D6 折扣额增量（>=0；下传账本供结算按归属应用）
                compServiceFeeCent,      // P6 本笔实收综合服务费（>=0；增量封顶后）
                compServiceFeeConfigId,  // P6 解析到的费率规则 id（供增量封顶月累计 + 冲正沿用）
                null,                    // V355 beneficiary_entity_id：核销归商户，无主体受益方
                event.parentTicketId(),  // S3 父票锚点：「一张票只收一道综合服务费/一道 G」的判定键
                giftCostCent,            // S6 本笔计提的赠送权益预估成本 G（>=0；首笔进池核销一次性,封顶后）
                giftCostConfigId         // S6 解析到的 pass_gift_cost_configs 规则 id（供封顶月累计 + 冲正沿用）
        ));
    }
}
