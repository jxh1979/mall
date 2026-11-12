package com.complex.platform.settlement;

// 文件版本：V0.07

import com.complex.platform.common.money.MoneyRounding;
import com.complex.platform.finance.FinanceService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 结算重构批2-3：月度通票分账<b>计算引擎</b>（§5.0 全流程）。
 *
 * <p>按业态拆份额 → 扣渠道费 → 扣门票综合服务费(池链支) → 可分配池各酒吧分配 → 校验 → 写结算单(confirmed)/明细(draft)。
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
 * <p>月窗 = [月首营业日起点, 月末营业日结束) 按 settlement_transactions.business_at(核销业务时刻)；营业日按综合体（平台单取唯一启用综合体，
 * 没有或不止一个时出厂），见 {@link #monthWindow}（第十八批 X 段 189-R1）。
 *
 * <p><b>金额精度与尾差（R-7 / D15.1，2026-07-22 用户拍板）</b>：一切按比例拆分一律
 * {@link MoneyRounding} <b>四舍五入到分（HALF_UP）</b>，不再用 {@code a*bps/10000} 整除截断
 * （旧写法系统性少给收费方且与「四舍五入」字面口径不符）。各家分别舍入后与「应分总额」的差额=<b>尾差</b>，
 * <b>单列</b>落 {@code settlement_bills.rounding_residual_cent}（V343）留痕，并由该笔业务
 * <b>实际承担结算责任的主体</b>承担（主体快照 {@code rounding_residual_entity_id}，V383）——
 * 不得默认转给城市项目公司/顶层平台，绝不悄悄并进某一家、也不静默沉淀。守恒恒等式（运行期自检 + 集成测试断言）：
 * <ul>
 *   <li>{@code 通票收入 = Σ各业态销售渠道费 + Σ各业态通路费 + G + Σ各业态综合服务费 + Σ各业态净份额}（拆份额链无泄漏,{@link #verifyFormatSplit}）</li>
 *   <li>{@code 每份池 + 第一段超池额 = Σ第一段 + Σ第二段（实分与未核销减除）+ 未分配 + 暂挂 + 尾差}（逐份，{@link #settleUnit}）</li>
 * </ul>
 * HALF_UP 对 0 对称（{@code f(-x) = -f(x)}），故跨月红冲取原行之反仍逐分对消，守恒不被舍入破坏。
 *
 * <p><b>第十八批第 2 条：逐份分账（ENUM L52「档位为动态百分比，不是相对权重、不做归一化：一张票的第二段余额，由该票覆盖的酒吧中
 * 参加第二段的那几家，按其在第二段参与者内部的排名分配……按本张票的第二段参与家数查「参与家数档位表」」）</b>：
 * 结算单元＝父票 × 人份 × 第几次入场（BSM L778/L780/L800 每人每次一份完整票权益，按一张票计），单元池＝售票时固化的池按份均分；
 * <b>第一段</b>：售票时固化的第一段名单里的覆盖酒吧按每份固定金额 / 固定单价取，未核销的同额一次减除；
 * <b>第二段</b>：其余覆盖酒吧（参与家数 n，与是否核销无关）按售票所用方案的 n 家行、当期入场人数在 n 家内部排名（并列均分）取比例，
 * 已核销的拿、未核销的减除（单列「通票未核销减除额」，酒吧域归娱乐平台公司）；n=0 余额单列未分配；缺 n 家行暂挂待补配。
 * 边界：第一段合计超过单元池 → 不削第一段、第二段为 0，超出额记 pool_overflow_cent，过账时由娱乐平台公司承担；
 * 第 75 条（ENUM L52「超池的判定与审批一律在事前完成，结算环节不再设“超池待审批”」）：结算单一律 confirmed，不设结算期审批。
 * 覆盖酒吧的通票出票即固化池快照（{@link PassPoolSnapshotService}）；单一酒吧门票（订单挂酒吧）不建快照，其核销按专属直付结付订单酒吧
 * （ENUM L32，见 {@link SettlementSettledConsumer}）；不含酒吧入场的票不建快照，其酒吧业态净额按 ENUM L52 末句并入未分配池余额；
 * 研发阶段无存量（UD-07），不留无快照兼容分支。
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
    // FS-021-R2（操作员裁决 2026-10-19 第 4 条「单独列追收、按原月重算」）：原月已结付后才撤销的核销另列追收明细——
    //   单店（专属直付）按该次核销原结付金额，分池按原月剔除该次核销后重算各家应得的差额逐家追回；与当月应付分别收付。
    static final String METHOD_EXCLUSIVE_RECOVERY = "exclusive_recovery";
    static final String METHOD_POOLED_RECOVERY = "pooled_recovery";
    // 第十八批第 2 条（ENUM L52「补配生效后，因缺该行而暂挂的票按该行续算结算」）：续算明细另列，原月结算单不回改。
    static final String METHOD_POOLED_RESUME = "pooled_resume";

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
     * 「其他」没有单一主体，钱归提供这份权益的商户自己（per_merchant）；餐饮、零售归城市项目公司（第十八批 X 段 176-R2，见 {@link #CITY_COMPANY_FORMATS}）。
     *
     * <p>V384 补传媒一行：业务 HTML §「收入归属」明写「演出票 → 传媒公司」，
     * 缺这一行时演出份额会被当成 per_merchant 归到商户自己，传媒公司在结算单上完全看不见自己的钱。
     */
    private static final java.util.Map<String, String> ENTITY_BENEFICIARY_FORMATS = java.util.Map.of(
            FORMAT_BAR, REVENUE_STREAM_ALL_PASS_TICKET,
            "amusement", "amusement_ticket",
            FORMAT_MEDIA, "performance_ticket");
    /** 第十八批 X 段 176-R2（ENUM:49「餐饮域与零售域 → 城市项目公司」；DEC-R3-2026-08-04-11）：餐饮、零售份额归该项目城市项目公司。 */
    static final Set<String> CITY_COMPANY_FORMATS = Set.of("catering", "retail");

    private final JdbcSettlementBillRepository repository;
    private final RevenueStreamAttributionRepository attributionRepository;
    private final JdbcPassPoolRepository passPools;
    private final com.complex.platform.common.BusinessDayResolver businessDays;
    private final FinanceService financeService;
    private final JdbcSettlementTransactionRepository transactions;

    public TicketDistributionSettlementService(JdbcSettlementBillRepository repository,
                                               RevenueStreamAttributionRepository attributionRepository,
                                               JdbcPassPoolRepository passPools,
                                               com.complex.platform.common.BusinessDayResolver businessDays,
                                               FinanceService financeService,
                                               JdbcSettlementTransactionRepository transactions) {
        this.repository = repository;
        this.attributionRepository = attributionRepository;
        this.passPools = passPools;
        this.businessDays = businessDays;
        this.financeService = financeService;
        this.transactions = transactions;
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
        // 第十八批 V 段第 159 条 159-R2（ENUM:190「非最终快照的数据一律不得进入结算、分红、正式提成计算和对账关账」；BSM:1689「通票权益核销后
        //   才按所属域的已生效规则与实际提供方结算」）：分账只取核销与核销冲正事实、售票固化的池快照与入场使用、按核销挂与冲正的通票赠饮应付，
        //   对应「当日订单与核销记录」「当日退款与冲正记录」两类。单位同逾期待办（第十八批裁决 35「酒吧逐店逐日查；游乐等按综合体逐日查」）：
        //   projectId 为空＝全部在营未存档酒吧与启用综合体，非空＝该综合体及其下酒吧；建档前的营业日不查。有未最终的营业日就整月不结、一行不写。
        List<String> notFinal = financeService.nonFinalDaysAllUnits(projectId, periodStart, periodEnd,
                List.of("orders_verifications", "refunds_reversals"));
        if (!notFinal.isEmpty()) {
            throw com.complex.platform.common.api.ApiException.conflict("SETTLEMENT_DATA_NOT_FINAL",
                    "以下营业日数据尚未标记为最终，不能结算通票分账：" + FinanceService.describeNonFinalDays(notFinal));
        }
        Instant[] window = monthWindow(projectId, month);
        MonthCalc calc = compute(projectId, month, null);
        Recoveries recoveries = recoveries(projectId, window[0], window[1]);

        repository.upsertBill(calc.bill());

        // 幂等重跑:先清旧明细再写(明细集合随当月核销变化,覆盖式最简)
        String actualBillId = repository.findBillId(projectId, periodStart, periodEnd);
        repository.deleteItemsByBill(actualBillId);
        // S11④ 各业态结算出口的留痕:每个业态一行「毛额 / 折扣补回 / 渠道费 / 净份额 / 归谁」。
        //   本批只保证份额算得出、归属定得住、非酒吧业态不进酒吧池;各业态自己的完整结算单另批。
        repository.deleteFormatSharesByBill(actualBillId);
        // 第十八批 X 段 176-R2（ENUM:32「其余各域份额自收款之日起即为应付给对应域主管公司的款项」；ENUM:49「餐饮域与零售域 → 城市项目公司」）：
        //   餐饮、零售份额行归该项目城市项目公司（同 189-R1 口径：平台单取唯一启用综合体）；恰好一家才取，否则归属留空、不改金额。
        // ponytail: 多综合体平台单的餐饮、零售份额归属主体留空，上限同 189-R1；要逐综合体归属时把份额行按项目拆。
        String cityProject = dayProject(projectId);
        List<String> cityCompanies = cityProject == null ? List.of() : transactions.findProjectCompanies(cityProject);
        // 第十八批 AI 段 214-R1（决策台账 FS-021「单独列追收、按原月重算」）：追收按业态写进本单份额行；本单无该业态行的补零份额行（同 ③b 先例）。
        java.util.LinkedHashSet<String> formats = new java.util.LinkedHashSet<>(calc.formatShares().keySet());
        formats.addAll(recoveries.formatDiffs().keySet());
        for (String format : formats) {
            FormatShare share = calc.formatShares().getOrDefault(format, new FormatShare(format, 0L, 0L));
            long[] recovery = recoveries.formatDiffs().getOrDefault(format, new long[2]);
            boolean cityCompanyFormat = CITY_COMPANY_FORMATS.contains(share.format());
            String kind = ENTITY_BENEFICIARY_FORMATS.containsKey(share.format()) || cityCompanyFormat
                    ? BENEFICIARY_ENTITY : BENEFICIARY_PER_MERCHANT;
            String entityId = cityCompanyFormat
                    ? (cityCompanies.size() == 1 ? cityCompanies.getFirst() : null)
                    : BENEFICIARY_ENTITY.equals(kind)
                    ? attributionRepository.resolveManagementEntity(
                            projectId, ENTITY_BENEFICIARY_FORMATS.get(share.format()))
                    : null;
            repository.upsertFormatShare(new SettlementBillFormatShareRecord(
                    actualBillId, share.format(), share.grossCent(), share.platformBorneCent(),
                    share.channelFeeCent(), share.routeFeeCent(), share.compServiceFeeCent(), share.giftCostCent(),
                    share.giftPaidCent(), share.netCent(), kind, entityId, share.internalSettledCent(),
                    share.interEntityNetCent(), recovery[0], recovery[1], share.unverifiedNetCent()));
        }
        // S7 差额台账落痕(append-only):首结追加陈述行;幂等重算若结论有变,先冲正取反旧净额、再追加新陈述,
        //   历史结论永在;结论没变不追加(重跑零膨胀)。收款主体快照与 G 同源(pass_gift_cost 归属流)。
        writeGiftVarianceLedger(actualBillId, projectId, periodStart, periodEnd,
                calc.giftVariance(), calc.bill().giftBeneficiaryEntityId());
        // 第十八批第 2 条：逐份分账结果（每份每家覆盖酒吧一行，减除、暂挂与续算留痕）随单幂等覆盖。
        passPools.replaceUnitResults(actualBillId, calc.unitResults());
        List<ItemCalc> billItems = new ArrayList<>(calc.items());
        billItems.addAll(calc.resumeItems());
        for (ItemCalc item : billItems) {
            // 专属直付 / 第一段 / 续算明细 id 加 method 后缀，与该家第二段池分账明细 id 区分（否则 PK/唯一键相撞被覆盖丢账）。
            String itemId = "si_" + actualBillId + "_" + item.merchantId() + switch (item.method()) {
                case METHOD_EXCLUSIVE_FULL -> "_excl";
                case METHOD_BY_FIXED_AMOUNT -> "_f";
                case METHOD_BY_VENUE_PER_GUEST -> "_g";
                case METHOD_POOLED_RESUME -> "_rs";
                default -> "";
            };
            repository.upsertItem(new SettlementItemRecord(
                    itemId,
                    actualBillId, item.merchantId(),
                    item.verifiedCount(), item.guestCount(), item.rankNo(),
                    item.method(), item.amount(), item.amount(), ITEM_STATUS_DRAFT,
                    item.singleBarRouteFeeCent(), item.singleBarChannelFeeCent()));
        }
        // FS-021-R2：追收明细（每家每支一条，按原月拆分回指原月结算单），不计入当月分账合计与越池判定。
        for (RecoveryCalc recovery : recoveries.items()) {
            String itemId = "si_rcv_" + java.util.UUID.nameUUIDFromBytes((actualBillId + "|" + recovery.merchantId() + "|"
                    + recovery.method()).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString().replace("-", "");
            repository.upsertItem(new SettlementItemRecord(itemId, actualBillId, recovery.merchantId(), 0, 0, null,
                    recovery.method(), recovery.amount(), recovery.amount(), ITEM_STATUS_DRAFT, 0L, 0L));
            recovery.byOriginalBill().forEach((originalBill, amount) ->
                    repository.insertItemRecovery(itemId, originalBill, amount));
        }
        return actualBillId;
    }

    /**
     * FS-021-R2：本窗内被撤销核销所属的原月（已结付），以原月口径两次重算——只含早于本窗的撤销 vs 再含本窗的撤销——之差，
     * 逐家形成追收：单店（专属直付）即该次核销原结付金额，分池即原月剔除该次核销后各家应得的变化。原月账单与已付款不回改。
     * 第十八批 AI 段 214-R1（BSM:804「再冲回域间份额」）：同两次重算另比游乐、传媒、餐饮、零售业态行的域间份额与按内部结算价应直付额，按业态记追收。
     */
    private Recoveries recoveries(String projectId, Instant windowStart, Instant windowEnd) {
        java.util.LinkedHashMap<String, RecoveryCalc> out = new java.util.LinkedHashMap<>();
        // 业态 → {域间份额之差, 应直付额之差}
        java.util.Map<String, long[]> formatDiffs = new java.util.LinkedHashMap<>();
        // 189-R1：原月＝原核销业务时刻所属营业日的月（与月窗同一套营业日）。
        java.util.TreeSet<YearMonth> originals = new java.util.TreeSet<>();
        for (Instant at : repository.findRecoveredOriginalTimes(projectId, windowStart, windowEnd)) {
            originals.add(YearMonth.from(businessDays.businessDate(dayProject(projectId), null, at)));
        }
        for (YearMonth original : originals) {
            String originalBillId = repository.findBillId(projectId, original.atDay(1), original.atEndOfMonth());
            MonthCalc beforeCalc = compute(projectId, original, windowStart);
            MonthCalc afterCalc = compute(projectId, original, windowEnd);
            java.util.LinkedHashSet<String> formats = new java.util.LinkedHashSet<>(beforeCalc.formatShares().keySet());
            formats.addAll(afterCalc.formatShares().keySet());
            for (String format : formats) {
                if (!interEntityFormat(format)) {
                    continue;
                }
                FormatShare b = beforeCalc.formatShares().getOrDefault(format, new FormatShare(format, 0L, 0L));
                FormatShare a = afterCalc.formatShares().getOrDefault(format, new FormatShare(format, 0L, 0L));
                long[] d = formatDiffs.computeIfAbsent(format, k -> new long[2]);
                d[0] += a.interEntityNetCent() - b.interEntityNetCent();
                d[1] += a.internalSettledCent() - b.internalSettledCent();
            }
            java.util.Map<String, Long> before = recoveryKeys(beforeCalc.items());
            java.util.Map<String, Long> after = recoveryKeys(afterCalc.items());
            java.util.LinkedHashSet<String> keys = new java.util.LinkedHashSet<>(before.keySet());
            keys.addAll(after.keySet());
            for (String key : keys) {
                long diff = after.getOrDefault(key, 0L) - before.getOrDefault(key, 0L);
                if (diff == 0L) {
                    continue;
                }
                String[] parts = key.split("\\|", 2);
                RecoveryCalc cur = out.computeIfAbsent(key,
                        k -> new RecoveryCalc(parts[0], parts[1], 0L, new java.util.LinkedHashMap<>()));
                cur.byOriginalBill().merge(originalBillId, diff, Long::sum);
                out.put(key, new RecoveryCalc(cur.merchantId(), cur.method(), cur.amount() + diff, cur.byOriginalBill()));
            }
        }
        formatDiffs.values().removeIf(d -> d[0] == 0L && d[1] == 0L);
        return new Recoveries(new ArrayList<>(out.values()), formatDiffs);
    }

    /** 份额归娱乐平台公司之外的域主管公司的业态：游乐、传媒（ENUM:32）与餐饮、零售（ENUM:49「餐饮域与零售域 → 城市项目公司」）。 */
    private static boolean interEntityFormat(String format) {
        return CITY_COMPANY_FORMATS.contains(format) || ENTITY_BENEFICIARY_FORMATS.containsKey(format) && !FORMAT_BAR.equals(format);
    }

    /**
     * 第十八批 X 段 189-R1（BSM:1682「全系统的钱账统一按「营业日」归集」「起止时点由后台配置（安全阈值，按综合体分别配置」）：
     * 月窗＝[月首营业日起点, 月末营业日结束)，按综合体营业日、不传酒吧（与逐份分组 {@link #settleUnits} 同口径）。
     * 第十八批 AI 段 237-R2：结算单审阅明细（{@link SettlementQueryService#billReview}）按同一月窗读，故包内可见。
     */
    Instant[] monthWindow(String projectId, YearMonth month) {
        // ponytail: 启用综合体不止一个时平台单取出厂 10:00 窗；起点晚于 10:00 的综合体，偏差落在其非营业时段；起点早于 10:00 的综合体，月首 [起点, 10:00) 的营业时段数据进上月单，且不在上月单最终快照闸检查范围内；要逐综合体切月时把平台单按项目拆窗。
        String p = dayProject(projectId);
        return new Instant[] {businessDays.start(p, null, month.atDay(1)), businessDays.end(p, null, month.atEndOfMonth())};
    }

    /** 月窗所按的综合体：项目单即该项目；平台单（null）取唯一启用综合体（看板单综合体口径先例），没有或不止一个时 null（出厂）。 */
    private String dayProject(String projectId) {
        return projectId != null ? projectId : businessDays.soleProject();
    }

    private static java.util.Map<String, Long> recoveryKeys(List<ItemCalc> items) {
        java.util.Map<String, Long> out = new java.util.LinkedHashMap<>();
        for (ItemCalc item : items) {
            String method = METHOD_EXCLUSIVE_FULL.equals(item.method()) ? METHOD_EXCLUSIVE_RECOVERY : METHOD_POOLED_RECOVERY;
            out.merge(item.merchantId() + "|" + method, item.amount(), Long::sum);
        }
        return out;
    }

    /**
     * 按月窗算出结算单头、业态份额、赠付差额与分账明细（只读，不落库）。recoveryCutoff 为空＝当月结算口径；
     * 非空＝FS-021-R2 原月重算口径（另并入原核销在本月、冲正早于 cutoff 的撤销）。
     */
    private MonthCalc compute(String projectId, YearMonth month, Instant recoveryCutoff) {
        LocalDate periodStart = month.atDay(1);
        LocalDate periodEnd = month.atEndOfMonth();
        Instant[] window = monthWindow(projectId, month);
        Instant windowStart = window[0];
        Instant windowEnd = window[1];

        // D6 套票折扣归属(§7.4)：项目级配置（默认 platform_absorb）。折扣额已在核销时逐笔落 discount_amount_cent（append-only 账本），
        //   本引擎按 discount_bearer 应用——项目承担部分让商户拿满额、并记入 bill.combo_discount_cent。无折扣(折扣额=0)时全程 +0=零行为变更。
        var discountCfg = repository.findComboDiscountConfig(projectId);

        // ① 通票收入(已核销)按【业态】拆开(S11①,§二十.1)。一张联合通票的钱不是一整笔都归酒吧——
        //    游乐那份归游乐公司、餐饮那份归餐厅商户、零售那份归零售商户,只有酒吧那份才继续往下走进池。
        //    D6/§20.4：项目承担的折扣补回口径对【每一个业态】一视同仁(旧实现只补酒吧一路,同一张票各家口径不一致)。
        //    份额比例本身在【出票时】就按各权益票面原价占比拆好了(TicketOrderService.allocateRevenue),
        //    这里只是把权益级的份额按提供方业态归堆,不重新发明分摊算法。
        List<JdbcSettlementBillRepository.FormatRevenue> formatRows =
                repository.aggregateFormatRevenue(projectId, windowStart, windowEnd, recoveryCutoff);
        java.util.LinkedHashMap<String, FormatShare> formatShares = new java.util.LinkedHashMap<>();
        long ticketRevenue = 0L;
        long comboDiscountCent = 0L; // 本月项目公司实际承担的折扣入账(各业态 + 后续专属直付累加)。
        long pooledServiceFee = 0L;  // S4 门票综合服务费·池链支(F):Σ各业态当月逐笔计提,归娱乐平台公司。
        long pooledGiftCost = 0L;    // S6 赠送权益预估成本(G):Σ各业态当月逐笔计提,归娱乐平台公司。
        // 第十八批第 3 条（BSM L749「通路费……所有支付都会产生、官方渠道同样产生……在拆分给各域之前从整单扣除……由各域按分配份额自然共担」）：
        //   付款时按支付通路固化、每笔核销按核销额占本单实付比例分摊落 settlement_transactions.route_fee_cent，这里按业态线性求和。
        long pooledRouteFee = 0L;
        for (var fr : formatRows) {
            long borne = platformBorneDiscount(discountCfg, fr.discountCent());
            formatShares.put(fr.businessFormat(),
                    new FormatShare(fr.businessFormat(), fr.revenueCent() + borne, borne, 0L, fr.routeFeeCent(),
                            fr.compServiceFeeCent(), fr.giftCostCent()));
            ticketRevenue += fr.revenueCent() + borne;
            comboDiscountCent += borne;
            pooledServiceFee += fr.compServiceFeeCent();
            pooledGiftCost += fr.giftCostCent();
            pooledRouteFee += fr.routeFeeCent();
        }
        // K20 不重不漏:业态汇总必须恰好覆盖同一批进池核销(分组条件写错/漏一个业态会在这里响亮停,
        //   而不是悄悄少算一笔)。收入/综合服务费/赠送权益预估成本三个合计都核——漏一个业态,娱乐平台公司就少收一笔。
        var pooledTotals = repository.sumPooledTotals(projectId, windowStart, windowEnd, recoveryCutoff);
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
        if (pooledRouteFee != pooledTotals.routeFeeCent()) {
            throw com.complex.platform.common.api.ApiException.conflict("SETTLEMENT_CONSERVATION_VIOLATED",
                    "业态通路费汇总与进池分摊总额不符:Σ各业态通路费 " + pooledRouteFee
                            + " ≠ 当月进池分摊合计 " + pooledTotals.routeFeeCent());
        }

        // 第十八批 X 段 176-R2（BSM:749「跨域组合通票扣完上述通路费和适用的销售渠道费后，再按各业态价格占比分到各域」「由各域按分配份额自然共担」；
        //   ENUM:52「游乐、餐饮、零售份额各自直接归属」）：有价专属直付行按售票固化的域份额并入业态拆分（毛额＝Σ域份额、通路费＝Σ按域份额分摊），
        //   随后与各业态一起按毛额共担销售渠道费；按内部结算价应直付额另记 internal_settled（ENUM:53 差额＝净份额−它），直付明细（⑦b）不变。
        //   K20 互核只核进池核销，放在其后。
        for (var ds : repository.aggregateExclusiveDomainShares(projectId, windowStart, windowEnd, recoveryCutoff)) {
            FormatShare cur = formatShares.get(ds.businessFormat());
            formatShares.put(ds.businessFormat(),
                    (cur == null ? new FormatShare(ds.businessFormat(), 0L, 0L) : cur).plusExclusive(ds));
            ticketRevenue += ds.domainShareCent();
            pooledRouteFee += ds.routeFeeCent();
        }

        // ⑥ 当期排名口径：本月各酒吧入场类核销人数（ENUM L52「按各酒吧当期核销入场人数由多到少排定」；赠酒等其他权益的核销不计名次）。
        List<JdbcSettlementBillRepository.MerchantMonthlyStat> stats =
                repository.aggregateMerchantStats(projectId, windowStart, windowEnd, recoveryCutoff);
        // ⑥b 第十八批第 2 条：有池快照的父票逐份结算（其酒吧核销已在上面各聚合里剔除，金额与扣项改取售票时固化的快照）。
        UnitBatch units = settleUnits(projectId, periodStart, windowStart, windowEnd, recoveryCutoff, stats);

        // ①b S7 赠付差额结算(ENUM L52「该笔预扣归娱乐平台公司，预扣数与日后实付数的差额也留在娱乐平台公司」):
        //    同一事务、同一月窗,按「项目 × 规则(pass_gift_cost_configs)」结出 差额 V = 预扣 G − 实付 A。
        //    G = 本月进池计提净额(按计提行快照的规则 id 分组) + 本月有池快照各份按快照规则 id 均分的 G;
        //    A = ar_ap_items(pass_drink_settlement) 对冲净额(月窗/项目/票种锚点取该核销的结算事实行,票种再走与计提同一个
        //    findPassGiftCostConfig 归到规则)＋零元专属直付(平台出资的餐饮、零售赠送)按内部结算价直付的核销与冲正额(216-R1)。
        //    处理规则只剩 platform_absorb:差额留娱乐平台公司仅留痕(ENUM L52;18B 复核后裁决第 11 条)。
        GiftVarianceCalc giftVariance = settleGiftVariance(projectId, windowStart, windowEnd, recoveryCutoff,
                pooledGiftCost + units.giftCostCent(), units.giftCostByConfig());

        // ② 赠酒成本不在此扣(D5,2026-07-21 用户拍板删):通票免费酒水由 bar 域 BarGiftConsumer 每次核销按【内部结算价】
        //    给出品酒吧挂应付(pass_drink_settlement,幂等键 verificationId,可冲正)=唯一补偿口径;原「每杯 3 元池扣」
        //    是财务测算假设(PRD §3.7 假设不进系统)且与该内部结算价应付重复=对同一杯赠酒双重计,已删,不再从池扣减。

        // ③ 渠道抽成【整单前移】(S11②,§二十.1「先扣第三方渠道抽成,再按业态占比拆份额」)。
        //    渠道拿的是客人实付的那一整笔的抽成,不是只从酒吧那份里扣——旧实现把它记在池链上,
        //    等于让酒吧一家替全场的渠道费买单。改为按整单算出来,再按各业态份额占比分摊下去。
        //    费率按【订单付款时固化的销售渠道费配置】(ticket_order_channel_fees,付款时按订单项目×销售渠道解析 channel_fee_configs
        //    销售渠道费层;BSM L749「每笔分摊须记录所用合同版本」),不按结算当月生效的配置——同一张票各部分只用售出时那一版。
        //    第十八批第 3 条(BSM L749「官方渠道售出的票不扣销售渠道费,但仍扣通路费」):官方渠道不固化、一律不扣;
        //    通路费另由每笔核销分摊(见上方 pooledRouteFee),不在这里算。
        //    V381 两模式二选一(HTML「比例/金额后台可配」):比例=费率 × 该渠道当期实付额(HALF_UP);
        //    固定金额=每张票固定金额 × 该渠道当期签名票数(票数按首笔核销锚点计,一张票多次核销只收一道,
        //    冲正月票数为负 → 费为负=向渠道追回,与比例模式负收入同一带符号口径;整数乘法无舍入)。
        //    第十八批第 2 条:有池快照的票,酒吧那部分已按酒吧份额在售票时固化进快照(随各份并入酒吧业态行):比例模式的基数不含这些
        //    酒吧核销额;固定金额模式锚点票再扣回快照里已扣的那部分,整票仍只扣一次。
        long channelFee = 0L;
        for (var cr : repository.aggregateChannelRevenue(projectId, windowStart, windowEnd, recoveryCutoff)) {
            var feeCfg = cr.fee();
            // R-7:比例模式四舍五入到分(旧整除截断少扣渠道费)。
            channelFee += feeCfg.fixedAmountCent() != null
                    ? feeCfg.fixedAmountCent() * cr.ticketCount() - cr.pooledAtSaleFeeCent()
                    : MoneyRounding.bpsOf(cr.revenueCent(), feeCfg.feeBps());
        }
        // 渠道费按各业态毛额占比摊下去;分摊尾差【不再并进酒吧支】,而是单列到同一尾差口径
        //   (治理地图「不得悄悄并进任一参与方」—— 酒吧也是参与方,不能替全场认这几分)。
        long channelFeeResidual = allocateChannelFee(formatShares, channelFee, ticketRevenue);

        // 第十八批 AI 段 237-R1（ENUM:32「票款由娱乐平台公司统一收款」「收款主体不等于收入归属主体」「其余各域份额自收款之日起即为应付给对应域主管公司的
        //   款项」「纯游乐票和纯游乐通票由游乐公司负责」；BSM:1720「与娱乐平台公司无关」；BSM:1144「由传媒公司自行定价、售票、收款」）：域间份额＝净份额中
        //   娱乐平台公司代收、应归归属主体的部分——游乐、传媒行扣除归属主体自收的纯游乐票、纯游乐通票与传媒自办演出票那部分净额，餐饮零售行即净份额，
        //   酒吧（娱乐平台公司自身收入）与「其他」（归提供商户自己）为 0。
        // ponytail: 自收部分的销售渠道费按毛额占比从该业态行摊回，与 allocateChannelFee 按业态毛额分摊同一近似。
        Map<String, JdbcSettlementBillRepository.FormatRevenue> ownCollected = new HashMap<>();
        for (var oc : repository.aggregateOwnCollectedFormatRevenue(projectId, windowStart, windowEnd, recoveryCutoff)) {
            ownCollected.put(oc.businessFormat(), oc);
        }
        for (var e : formatShares.entrySet()) {
            FormatShare s = e.getValue();
            var oc = ownCollected.get(s.format());
            long ownGross = oc == null ? 0L : oc.revenueCent() + platformBorneDiscount(discountCfg, oc.discountCent());
            long ownNet = oc == null ? 0L : ownGross - oc.routeFeeCent()
                    - (s.grossCent() == 0L ? 0L : MoneyRounding.prorate(s.channelFeeCent(), ownGross, s.grossCent()));
            e.setValue(s.withInterEntity(interEntityFormat(s.format()) ? s.netCent() - ownNet : 0L));
        }
        // 第十八批 AI 段 214-R2（ENUM:49「其一，到期未核销票款」；第十八批裁决 51「从未核销、到期的部分在到期所在月另挂一笔」）：本月到期未核销、归非酒吧域
        //   归属主体的票款另列在该业态行（不进净份额与拆份额互核）；当月无该业态行的补零份额行。原月重算口径不并。
        if (recoveryCutoff == null) {
            for (var u : repository.aggregateUnverifiedNetByFormat(projectId, windowStart, windowEnd).entrySet()) {
                FormatShare cur = formatShares.get(u.getKey());
                formatShares.put(u.getKey(), (cur == null ? new FormatShare(u.getKey(), 0L, 0L) : cur).withUnverified(u.getValue()));
            }
        }

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

        // ④⑤ 可分配池(唯一的池)= 【酒吧业态】净份额 = 酒吧毛额 − 销售渠道费 − 通路费 − G − 综合服务费(池链支)。
        //    第十八批第 2 条(ENUM L52「池金额在售票时随订单固化」):有池快照的各份并入酒吧业态行——毛额/折扣补回/销售渠道费/通路费/G/F
        //    各取售票时固化值按份均分,其净份额即各份池之和;覆盖酒吧的通票出票即固化池快照,单一酒吧门票按专属直付(⑦b),研发阶段无存量(UD-07),不留无快照兼容分支。
        //    不含酒吧入场的票(如跨域组合票只送酒吧赠饮)不建池快照,其非专属酒吧业态核销净额并入可分配池、整笔单列未分配池余额
        //    (ENUM L52「该票从无第二段参与者时(……或该票未覆盖任何符合条件的第二段酒吧),余额单列为未分配池余额并归娱乐平台公司」)。
        //    游乐/餐饮/零售的净份额各走自己的出口,不进这个池。
        //    带符号(R-3):冲正月酒吧净份额可为负 → 各家分账为负 = 向酒吧追回,不钳零不丢账。
        FormatShare barShare = formatShares.get(FORMAT_BAR);
        long noSnapshotBarNet = barShare == null ? 0L : barShare.netCent();
        if (units.unitCount() > 0) {
            formatShares.put(FORMAT_BAR, (barShare == null ? new FormatShare(FORMAT_BAR, 0L, 0L) : barShare).plus(units));
            ticketRevenue += units.grossCent();
            comboDiscountCent += units.borneCent();
            channelFee += units.salesChannelFeeCent();
            pooledRouteFee += units.routeFeeCent();
            pooledServiceFee += units.serviceFeeCent();
            pooledGiftCost += units.giftCostCent();
        }
        long availablePool = units.poolCent() + noSnapshotBarNet;

        // ⑦ 分配方式快照:语义收窄(V374)后只剩 by_rank_tier＝第二段按参与家数档位表;旧值 fail-loud(与配置页口径对不上)。
        String method = repository.findDistributionMethod(projectId);
        if (!METHOD_BY_RANK_TIER.equals(method)) {
            throw com.complex.platform.common.api.ApiException.conflict("SETTLEMENT_CONFIG_INVALID",
                    "分配方式「" + method + "」已由分账方案的第一段名单(固定金额/固定单价)取代;"
                            + "第二段仅支持按参与家数档位表(by_rank_tier),请在结算配置中心改配分配方式");
        }
        List<ItemCalc> items = new ArrayList<>(units.items());

        // ⑦b 批3-4(§6.4)：专属某商户「不分摊」→ 每个专属商户一条直付明细（method=exclusive_full，
        //     不进可分配池、不参与他家分池排名/客数聚合）。
        // 综合服务费·专属支:专属商户直付前先抽综合服务费,商户拿【净额=收入-服务费】
        //   (记入 bill.comp_service_fee_exclusive_cent)。与池链支是同一道费的两支,都归娱乐平台公司。
        //   S2(V365):费率规则的范围是【项目 × 商户 × 票种】,且【每笔核销时】按票种解析并落库(P6 起本引擎只做
        //   线性求和,不再月末重算),故此处按 entitlement_type 分组只是聚合口径遗留、与费率命中无关
        //   (下面 merchantAgg 又按商户合并回去)。
        List<JdbcSettlementBillRepository.ExclusiveMerchantRevenue> exclusives =
                repository.aggregateExclusiveRevenue(projectId, windowStart, windowEnd, recoveryCutoff);
        // V336 综合服务费月度封顶(用户 2026-07-21 拍板 A「按费率规则各自封」):费率规则可跨票种(商户级规则
        //   ticket_type_id=NULL)或跨商户(全局默认 merchant_id=NULL),故封顶【不能】逐 (商户×票种) 聚合行各自压——
        //   否则同商户多票种落同一条规则时会被压 N 次=N×封顶,违背「商户级规则=该商户当月封顶」。改为按【商户 × 解析到的规则 id】
        //   把封顶前原始费聚合后再压:商户级规则=该商户当月封顶、票种级规则=该票种当月封顶、全局规则=各商户各自当月封顶(不跨商户合并)。
        //   两趟:①逐聚合行累计 商户毛额 + (商户,规则)原始费/cap ②各组封顶 → 商户净额一条 exclusive_full 明细。
        java.util.LinkedHashMap<String, long[]> merchantAgg = new java.util.LinkedHashMap<>();
        // P6:merchantAgg[mid] = {grossSum, verifiedCount, guestCount, compServiceFeeSum, singleBarRouteFee, singleBarChannelFee}
        //   compServiceFeeSum = 每笔核销/冲正落库的【实收】综合服务费之和(核销 fee>=0·增量封顶后·冲正 fee<=0·取原行之反)。
        for (var ex : exclusives) {
            // D6：项目按归属承担的折扣加回该商户毛额，使其拿满额(platform_absorb 全额/split_by_ratio 平台份额/merchant_absorb=0)。
            //   综合服务费在【核销时】已在此同口径毛额上算并落库(见 ComprehensiveServiceFee + SettlementSettledConsumer);
            //   此处只用毛额算净额，不再月末重算 fee。折扣=0 时 gross=revenue → 与 D6 前逐分一致(零行为变更)。
            long platformBorne = platformBorneDiscount(discountCfg, ex.discountCent());
            long gross = ex.revenueCent() + platformBorne;
            comboDiscountCent += platformBorne; // 项目承担的专属折扣入账
            long[] mg = merchantAgg.computeIfAbsent(ex.merchantId(), k -> new long[6]);
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
        // 第十八批 AI 段 212-R1（ENUM:51「所有支付都会产生通路费、官方渠道同样产生……不由该笔业务的收款主体单独承担」「任何票种经第三方渠道售出时，
        //   销售渠道费一律先从整单扣除且只扣一次」；BSM:749「官方渠道售出的票不扣销售渠道费，但仍扣通路费」）：单一酒吧门票行已按核销额分摊的通路费、
        //   按订单付款时固化配置计的销售渠道费（比例＝费率×核销额，固定额＝每张×锚点票数），从整单先扣、定出该酒吧直付额并逐商户留痕；
        //   不另挂对酒吧的应收，也不并入单头渠道费、通路费与业态份额（verifyFormatSplit 只核进池行）。
        for (var sb : repository.aggregateSingleBarFees(projectId, windowStart, windowEnd, recoveryCutoff)) {
            long[] mg = merchantAgg.computeIfAbsent(sb.merchantId(), k -> new long[6]);
            mg[4] += sb.routeFeeCent();
            mg[5] += sb.fee().fixedAmountCent() != null
                    ? sb.fee().fixedAmountCent() * sb.ticketCount()
                    : MoneyRounding.bpsOf(sb.revenueCent(), sb.fee().feeBps());
        }
        long exclusiveServiceFee = 0L;
        // 商户净额 = 毛额合计 − 该商户当月【实收】综合服务费合计 − 单一酒吧门票通路费与销售渠道费(212-R1);
        //   各商户一条 exclusive_full 直付明细(同商户多票种合并)。
        for (var e : merchantAgg.entrySet()) {
            long[] mg = e.getValue();
            long merchantFee = mg[3];
            exclusiveServiceFee += merchantFee;
            long net = mg[0] - merchantFee - mg[4] - mg[5];
            items.add(new ItemCalc(e.getKey(), (int) mg[1], (int) mg[2], null, net, METHOD_EXCLUSIVE_FULL, mg[4], mg[5]));
        }

        // distributed = 池分账 + 专属直付（两口径并列写入同一结算单明细；续算明细另列不计入）。
        long distributed = items.stream().mapToLong(ItemCalc::amount).sum();

        // 逐份守恒（份池 + 超池 = 实分 + 未核销减除 + 未分配 + 暂挂 + 尾差）已在 settleUnit 逐份自检；这里核拆份额链。
        verifyFormatSplit(formatShares.values(), ticketRevenue, channelFee, channelFeeResidual,
                pooledRouteFee, pooledServiceFee, pooledGiftCost);
        // ⑧ 越池:某份第一段合计超过该份池 → 超出额单列 pool_overflow_cent,过账时由娱乐平台公司承担(platform_incentive_subsidy)。
        //    第 75 条(ENUM L52「超池的判定与审批一律在事前完成，结算环节不再设“超池待审批”」):结算单一律 confirmed。
        String status = STATUS_CONFIRMED;

        // 六业态收入归属:通票收入归属的 L3 管理公司(娱乐平台公司),按 (项目, all_pass_ticket) 可配置解析;
        //   项目特例优先→全局默认→未配置返 null(仅记归属,不改任何金额)。
        String attributedEntityId =
                attributionRepository.resolveManagementEntity(projectId, REVENUE_STREAM_ALL_PASS_TICKET);
        // S8/K18 ③④:第二段无参与者时的池余额——显式归娱乐平台公司(= all_pass_ticket 流的归属主体,
        //   V375 种全局默认 be_ent_platform)+ 告警留痕。不是静默沉淀:金额与归属主体单列落 bill。
        //   18B 复核后裁决第 11 条:此栏只放无第二段参与者的余额(ENUM L52),赠付差额留娱乐平台公司、不进此栏;
        //   不含酒吧入场、未建池快照的票同属「该票未覆盖任何符合条件的第二段酒吧」,其酒吧业态净额一并单列于此。
        long poolUndistributed = units.undistributedCent() + noSnapshotBarNet;
        String poolUndistributedEntityId = poolUndistributed == 0L ? null : attributedEntityId;
        if (poolUndistributed != 0L && recoveryCutoff == null) {
            log.warn("结算告警:项目 {} 月 {} 无第二段参与者的余额 {} 分未分配,显式归娱乐平台公司(主体 {})留痕待查",
                    projectId == null ? "平台默认" : projectId, month, poolUndistributed,
                    poolUndistributedEntityId == null ? "未配置(归属留白,金额已单列)" : poolUndistributedEntityId);
        }
        // ENUM L52「按上述减除的未核销份额，按票种经营域归属（见本表「域归属对照」条），酒吧域即归娱乐平台公司；该笔在该张票的分账结果中
        //   单独立项留痕，不并入未分配池余额、也不并入金额尾差」:酒吧域主体与 all_pass_ticket 流同源解析,三栏各自落列。
        String unverifiedDeductionEntityId = units.deductionCent() == 0L ? null : attributedEntityId;
        // S4 综合服务费收款主体快照:两支都归娱乐平台公司(V370 种子;项目级可覆盖),月结时定格进单据——
        //   财务对账时「这笔费归了谁」直接看单据,不用追当时配置长什么样。
        String feeBeneficiaryEntityId =
                attributionRepository.resolveManagementEntity(projectId, REVENUE_STREAM_TICKET_SERVICE_FEE);
        // V383 尾差责任主体(治理地图「全局原则 · 金额一律精确到分」2026-08-01 翻转后的现行口径):
        //   池分账舍入尾差与渠道费分摊尾差同为「按比例拆分四舍五入」成因 → 合成一笔单列,
        //   挂到【实际承担结算责任的主体】—— 通票分账这笔业务就是 all_pass_ticket 流的归属主体
        //   (娱乐平台公司,后台可配、项目级可覆盖),与 attributedEntityId 同源解析不另写一套。
        //   尾差为 0 → 主体置空(没有尾差就没有责任人),与 pool_undistributed_entity_id 同范式。
        long totalRoundingResidual = units.residualCent() + channelFeeResidual;
        String roundingResidualEntityId = totalRoundingResidual == 0L ? null : attributedEntityId;
        // S6 G 收款主体快照:归娱乐平台公司(V372 种子;项目级可覆盖),月结时定格进单据。
        String giftBeneficiaryEntityId =
                attributionRepository.resolveManagementEntity(projectId, REVENUE_STREAM_PASS_GIFT_COST);

        SettlementBillRecord bill = new SettlementBillRecord(
                idFor(projectId, month), projectId, periodStart, periodEnd,
                ticketRevenue, channelFee, pooledRouteFee, availablePool, distributed,
                pooledServiceFee, exclusiveServiceFee, feeBeneficiaryEntityId,
                pooledGiftCost, giftBeneficiaryEntityId,
                method, status, attributedEntityId, comboDiscountCent,
                totalRoundingResidual, roundingResidualEntityId,
                poolUndistributed, poolUndistributedEntityId,
                units.deductionCent(), unverifiedDeductionEntityId, units.suspendedCent(), units.overflowCent());
        return new MonthCalc(bill, formatShares, giftVariance, items, units.results(), units.resumeItems());
    }

    /**
     * 第十八批第 2 条：本窗内有池快照父票的逐份结算（ENUM L52；BSM L752 同义）。
     *
     * <p>份＝（父票, 人份, 第 k 次入场）。该人份入场类核销（与到期作业「入场次数」同口径，扣除早于 cutoff 的冲正）按所属营业日
     * （ENUM L187，按售票项目的营业日）分组，第 k 个有入场的营业日即第 k 份，时刻取该营业日内最早一次核销，落在本窗内的份才在本窗结算；
     * 本份已核销酒吧＝该营业日内有进池入场核销的覆盖酒吧（ENUM L48 已检票、权益未在该场次内核销的，进入分账减除）。
     * 从未入场的人份与未开启的入场次数不进本引擎，由到期作业按到期未核销处理（ENUM L49 其一）。毛额与各扣项按份数最大余数均分
     * （与到期作业按次数均分同口径），份池＝份毛额−各份扣项。recoveryCutoff 为空＝当月结算（并续算更早月份暂挂、此前补配已生效的份）；
     * 非空＝FS-021-R2 原月重算，不续算。
     */
    private UnitBatch settleUnits(String projectId, LocalDate periodStart, Instant windowStart, Instant windowEnd,
                                  Instant recoveryCutoff, List<JdbcSettlementBillRepository.MerchantMonthlyStat> stats) {
        Map<String, Integer> people = new HashMap<>();
        Map<String, JdbcSettlementBillRepository.MerchantMonthlyStat> statByMerchant = new HashMap<>();
        Map<String, Integer> monthRank = new HashMap<>();
        for (int i = 0; i < stats.size(); i++) {
            var s = stats.get(i);
            people.put(s.merchantId(), s.guestCount());
            statByMerchant.put(s.merchantId(), s);
            monthRank.put(s.merchantId(), i > 0 && stats.get(i - 1).guestCount() == s.guestCount()
                    ? monthRank.get(stats.get(i - 1).merchantId()) : i + 1);
        }
        Instant now = Instant.now();
        // 补配行须在本窗结束前、且已到生效时刻才能用（ENUM L52「通告期届满方可生效」）。
        Instant resolvableAt = windowEnd.isAfter(now) ? now : windowEnd;
        UnitAccumulator acc = new UnitAccumulator();
        // 父票 → 人份 → 该人份的入场类核销
        Map<String, Map<Integer, List<JdbcPassPoolRepository.EntryUse>>> byTicket = new LinkedHashMap<>();
        for (var u : passPools.findEntryUses(projectId, windowStart, windowEnd, recoveryCutoff == null ? windowEnd : recoveryCutoff)) {
            byTicket.computeIfAbsent(u.ticketId(), k -> new TreeMap<>())
                    .computeIfAbsent(u.slotIndex(), k -> new ArrayList<>()).add(u);
        }
        Map<String, JdbcPassPoolRepository.Snapshot> snapshots = passPools.findSnapshots(byTicket.keySet());
        Map<String, Map<Integer, int[]>> tierCache = new HashMap<>();
        for (var ticket : byTicket.entrySet()) {
            JdbcPassPoolRepository.Snapshot s = snapshots.get(ticket.getKey());
            int units = s.slots() * s.visits();
            Set<String> firstSegmentBars = new HashSet<>();
            s.firstSegment().forEach(f -> firstSegmentBars.add(f.merchantId()));
            List<String> secondSegment = s.coveredBarIds().stream().filter(b -> !firstSegmentBars.contains(b)).toList();
            for (var slot : ticket.getValue().entrySet()) {
                // 第 k 份＝该人份第 k 个有入场的营业日（营业日内各家入场同属一份）。
                TreeMap<LocalDate, List<JdbcPassPoolRepository.EntryUse>> days = new TreeMap<>();
                for (var u : slot.getValue()) {
                    days.computeIfAbsent(businessDays.businessDate(s.projectId(), null, u.at()), d -> new ArrayList<>()).add(u);
                }
                int k = 0;
                for (List<JdbcPassPoolRepository.EntryUse> day : days.values()) {
                    if (++k > s.visits()) {
                        break;
                    }
                    Instant at = day.stream().map(JdbcPassPoolRepository.EntryUse::at).min(Instant::compareTo).orElseThrow();
                    Set<String> verified = new HashSet<>();
                    day.stream().filter(JdbcPassPoolRepository.EntryUse::poolBarEntry).forEach(u -> verified.add(u.merchantId()));
                    if (at.isBefore(windowStart) || !at.isBefore(windowEnd)) {
                        continue;
                    }
                    int index = unitIndex(units, s.visits(), slot.getKey(), k);
                    long gross = share(s.barGrossCent(), units, index);
                    long route = share(s.routeFeeCent(), units, index);
                    long sales = share(s.salesChannelFeeCent(), units, index);
                    long gift = share(s.giftCostCent(), units, index);
                    long service = share(s.serviceFeeCent(), units, index);
                    long pool = gross - route - sales - gift - service;
                    acc.unitCount++;
                    acc.grossCent += gross;
                    acc.borneCent += share(s.platformBorneCent(), units, index);
                    acc.routeFeeCent += route;
                    acc.salesChannelFeeCent += sales;
                    acc.giftCostCent += gift;
                    acc.serviceFeeCent += service;
                    acc.poolCent += pool;
                    if (s.giftCostConfigId() != null && gift != 0L) {
                        acc.giftCostByConfig.merge(s.giftCostConfigId(), gift, Long::sum);
                    }
                    // 售票所用方案缺本票第二段参与家数的行：先找此后已生效的补配行，没有就暂挂。
                    String version = s.schemeVersionId();
                    int n = secondSegment.size();
                    int[] row = n == 0 ? null : tierCache.computeIfAbsent(version, passPools::findTiers).get(n);
                    if (n > 0 && row == null) {
                        String resolving = passPools.findResolvingVersion(s.projectId(), n, s.schemeEffectiveAt(), resolvableAt);
                        if (resolving != null) {
                            version = resolving;
                            row = tierCache.computeIfAbsent(resolving, passPools::findTiers).get(n);
                        }
                    }
                    List<SegmentBar> participants = secondSegment.stream()
                            .map(b -> new SegmentBar(b, people.getOrDefault(b, 0), verified.contains(b))).toList();
                    settleUnit(acc, ticket.getKey(), slot.getKey(), k, pool, s.firstSegment(), participants, verified, row, version);
                }
            }
        }
        if (recoveryCutoff == null) {
            resumeSuspended(acc, projectId, periodStart, resolvableAt, tierCache);
        }
        List<ItemCalc> items = new ArrayList<>();
        acc.paid.forEach((key, amount) -> {
            String[] parts = key.split("\\|", 2);
            var stat = statByMerchant.get(parts[0]);
            items.add(new ItemCalc(parts[0], stat == null ? 0 : stat.verifiedCount(), stat == null ? 0 : stat.guestCount(),
                    METHOD_BY_RANK_TIER.equals(parts[1]) ? monthRank.get(parts[0]) : null, amount, parts[1]));
        });
        List<ItemCalc> resumeItems = new ArrayList<>();
        acc.resumed.forEach((merchant, amount) -> {
            var stat = statByMerchant.get(merchant);
            resumeItems.add(new ItemCalc(merchant, stat == null ? 0 : stat.verifiedCount(), stat == null ? 0 : stat.guestCount(),
                    null, amount, METHOD_POOLED_RESUME));
        });
        return new UnitBatch(acc.unitCount, acc.grossCent, acc.borneCent, acc.routeFeeCent, acc.salesChannelFeeCent,
                acc.serviceFeeCent, acc.giftCostCent, acc.giftCostByConfig, acc.poolCent, items, acc.deductionCent,
                acc.undistributedCent, acc.suspendedCent, acc.residualCent, acc.overflowCent, acc.results, resumeItems);
    }

    /**
     * 一份的两段分配（ENUM L52「可分配池只在实际发生核销的酒吧之间分配」）：
     * 第一段——已核销者按每份固定金额 / 固定单价（×1 人）取，未核销者同额一次减除，不再按第二段档位另行减除；
     * 第一段合计超过本份池时不削第一段、第二段为 0，超出额记超池；
     * 第二段——余额 P2 按 n 家行在 n 家内部排名取比例（并列均分，比例不取整），已核销拿、未核销减除；n=0 余额单列未分配；缺 n 家行暂挂。
     * 逐份守恒：Σ第一段 + Σ第二段 + 未分配 + 暂挂 + 尾差 ≡ 本份池 + 超池，否则拒绝出账。
     */
    private void settleUnit(UnitAccumulator acc, String ticketId, int slot, int visit, long pool,
                            List<JdbcPassPoolRepository.FirstSegment> firstSegment, List<SegmentBar> second,
                            Set<String> verified, int[] row, String version) {
        long q = 0L;
        for (var f : firstSegment) {
            q = Math.addExact(q, f.amountCent());
            boolean ok = verified.contains(f.merchantId());
            if (ok) {
                acc.paid.merge(f.merchantId() + "|" + (JdbcSettlementBillRepository.RULE_PER_GUEST.equals(f.ruleType())
                        ? METHOD_BY_VENUE_PER_GUEST : METHOD_BY_FIXED_AMOUNT), f.amountCent(), Long::sum);
            } else {
                acc.deductionCent += f.amountCent();
            }
            acc.results.add(new JdbcPassPoolRepository.UnitResult(ticketId, slot, visit, f.merchantId(), 1, null, null, null,
                    null, f.amountCent(), ok, "settled", version));
        }
        long p2 = firstSegment.isEmpty() ? pool : Math.max(0L, pool - q);
        long overflow = firstSegment.isEmpty() ? 0L : Math.max(0L, q - pool);
        long assigned = 0L;
        long undistributed = 0L;
        long suspended = 0L;
        if (second.isEmpty()) {
            undistributed = p2;
        } else if (row == null) {
            suspended = p2;
            for (SegmentBar p : second) {
                acc.results.add(new JdbcPassPoolRepository.UnitResult(ticketId, slot, visit, p.merchantId(), 2, p.people(),
                        null, null, p2, 0L, p.verified(), "suspended", version));
            }
            log.warn("结算待办:父票 {} 第 {} 人份第 {} 次入场的第二段参与 {} 家,方案 {} 未配该家数档位行,第二段 {} 分暂挂待补配",
                    ticketId, slot, visit, second.size(), version, p2);
        } else {
            for (Share sh : secondSegment(p2, second, row)) {
                if (sh.participant().verified()) {
                    acc.paid.merge(sh.participant().merchantId() + "|" + METHOD_BY_RANK_TIER, sh.amountCent(), Long::sum);
                } else {
                    acc.deductionCent += sh.amountCent();
                }
                assigned += sh.amountCent();
                acc.results.add(new JdbcPassPoolRepository.UnitResult(ticketId, slot, visit, sh.participant().merchantId(), 2,
                        sh.participant().people(), sh.rankNo(), sh.pct(), p2, sh.amountCent(), sh.participant().verified(),
                        "settled", version));
            }
        }
        long residual = second.isEmpty() || row == null ? 0L : p2 - assigned;
        acc.overflowCent += overflow;
        acc.undistributedCent += undistributed;
        acc.suspendedCent += suspended;
        acc.residualCent += residual;
        if (q + assigned + undistributed + suspended + residual != pool + overflow || Math.abs(residual) > Math.max(1, second.size())) {
            throw com.complex.platform.common.api.ApiException.conflict("SETTLEMENT_CONSERVATION_VIOLATED",
                    "结算资金守恒校验失败(逐份):父票 " + ticketId + " 第 " + slot + " 人份第 " + visit + " 次入场,第一段 " + q
                            + " + 第二段 " + assigned + " + 未分配 " + undistributed + " + 暂挂 " + suspended + " + 尾差 " + residual
                            + " ≠ 本份池 " + pool + " + 超池 " + overflow);
        }
    }

    /**
     * 第二段：参与者按当期入场人数由多到少在本票 n 家内部编名次；人数相同为并列——共同占用连续名次，各取这几名比例之和的算术平均
     * （适用比例本身不取整，金额按 HALF_UP 到分）；所取行合计必须 10000，否则配置坏了、拒绝出账。
     */
    private static List<Share> secondSegment(long p2, List<SegmentBar> participants, int[] row) {
        if (java.util.Arrays.stream(row).asLongStream().sum() != 10000L) {
            throw com.complex.platform.common.api.ApiException.conflict("SETTLEMENT_CONFIG_INVALID",
                    "第二段参与 " + row.length + " 家的档位行合计不等于 100%,不能用于分账");
        }
        List<SegmentBar> sorted = new ArrayList<>(participants);
        sorted.sort(java.util.Comparator.comparingInt(SegmentBar::people).reversed().thenComparing(SegmentBar::merchantId));
        List<Share> out = new ArrayList<>();
        int pos = 0;
        while (pos < sorted.size()) {
            int end = pos;
            while (end + 1 < sorted.size() && sorted.get(end + 1).people() == sorted.get(pos).people()) {
                end++;
            }
            int tied = end - pos + 1;
            long sumBps = 0L;
            for (int r = pos; r <= end; r++) {
                sumBps += row[r];
            }
            BigDecimal pct = BigDecimal.valueOf(sumBps).divide(BigDecimal.valueOf(tied * 100L), 8, java.math.RoundingMode.HALF_UP);
            long amount = MoneyRounding.prorate(p2, sumBps, tied * 10000L);
            for (int r = pos; r <= end; r++) {
                out.add(new Share(sorted.get(r), pos + 1, pct, amount));
            }
            pos = end + 1;
        }
        return out;
    }

    /**
     * 续算（ENUM L52「补配生效后，因缺该行而暂挂的票按该行续算结算——此为『只对通告生效后新售出的票适用』的唯一例外」）：
     * 更早月份暂挂、补配行此前已生效的份，按补配行与原月入场人数、核销状态重算；已核销的另列续算明细（原月结算单不回改），
     * 未核销的计入本单减除额。
     */
    private void resumeSuspended(UnitAccumulator acc, String projectId, LocalDate periodStart, Instant resolvableAt,
                                 Map<String, Map<Integer, int[]>> tierCache) {
        Map<String, List<JdbcPassPoolRepository.SuspendedRow>> byUnit = new LinkedHashMap<>();
        for (var r : passPools.findSuspendedRows(projectId, periodStart)) {
            byUnit.computeIfAbsent(r.ticketId() + "|" + r.slotIndex() + "|" + r.visitNo(), k -> new ArrayList<>()).add(r);
        }
        for (List<JdbcPassPoolRepository.SuspendedRow> rows : byUnit.values()) {
            var first = rows.getFirst();
            String version = passPools.findResolvingVersion(first.projectId(), rows.size(), first.schemeEffectiveAt(), resolvableAt);
            if (version == null) {
                continue;
            }
            int[] row = tierCache.computeIfAbsent(version, passPools::findTiers).get(rows.size());
            long p2 = first.segmentPoolCent();
            long assigned = 0L;
            for (Share sh : secondSegment(p2, rows.stream()
                    .map(r -> new SegmentBar(r.merchantId(), r.entryPeople(), r.verified())).toList(), row)) {
                if (sh.participant().verified()) {
                    acc.resumed.merge(sh.participant().merchantId(), sh.amountCent(), Long::sum);
                } else {
                    acc.deductionCent += sh.amountCent();
                }
                assigned += sh.amountCent();
                acc.results.add(new JdbcPassPoolRepository.UnitResult(first.ticketId(), first.slotIndex(), first.visitNo(),
                        sh.participant().merchantId(), 2, sh.participant().people(), sh.rankNo(), sh.pct(), p2, sh.amountCent(),
                        sh.participant().verified(), "resumed", version));
            }
            acc.residualCent += p2 - assigned;
        }
    }

    /** 最大余数均分：份 index（0 起）分得 total/units，余数给前几份（到期作业按未开启的份取同一份额）。 */
    static long share(long total, int units, int index) {
        return Math.floorDiv(total, units) + (index < Math.floorMod(total, units) ? 1L : 0L);
    }

    /** 第 slot 人份第 visit 次入场那一份的序号（0 起；人份在前、次数在后）。 */
    static int unitIndex(int units, int visits, int slot, int visit) {
        return Math.min(units - 1, (slot - 1) * visits + (visit - 1));
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
     * K20 业态份额拆分守恒（fail-loud）：{@code Σ各业态净份额 + Σ各业态销售渠道费 + Σ各业态通路费 + 赠送权益预估成本
     * + 综合服务费(池链支) ≡ 通票收入毛额}，且 {@code Σ各业态销售渠道费 + 分摊尾差 ≡ 整单销售渠道费}、
     * {@code Σ各业态通路费 ≡ 当月进池分摊合计}、{@code Σ各业态综合服务费 ≡ 池链支合计}、{@code Σ各业态 G ≡ G 合计}。
     * 分摊写错会让某个业态凭空多拿或少拿——这种错在月末对账时极难查（数字都「差不多对」），所以宁可当场拒绝出账。
     */
    private void verifyFormatSplit(java.util.Collection<FormatShare> shares, long ticketRevenue,
                                   long channelFee, long channelFeeResidual, long pooledRouteFee,
                                   long pooledServiceFee, long pooledGiftCost) {
        long netTotal = 0L;
        long feeTotal = 0L;
        long routeFeeTotal = 0L;
        long svcFeeTotal = 0L;
        long giftCostTotal = 0L;
        for (FormatShare s : shares) {
            netTotal += s.netCent();
            feeTotal += s.channelFeeCent();
            routeFeeTotal += s.routeFeeCent();
            svcFeeTotal += s.compServiceFeeCent();
            giftCostTotal += s.giftCostCent();
        }
        if (feeTotal + channelFeeResidual != channelFee) {
            throw com.complex.platform.common.api.ApiException.conflict("SETTLEMENT_CONSERVATION_VIOLATED",
                    "业态份额守恒校验失败(渠道费分摊):Σ各业态渠道费 " + feeTotal
                            + " + 分摊尾差 " + channelFeeResidual + " ≠ 整单渠道费 " + channelFee);
        }
        if (routeFeeTotal != pooledRouteFee) {
            throw com.complex.platform.common.api.ApiException.conflict("SETTLEMENT_CONSERVATION_VIOLATED",
                    "业态份额守恒校验失败(通路费):Σ各业态通路费 " + routeFeeTotal + " ≠ 当月进池分摊合计 " + pooledRouteFee);
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
        if (netTotal + feeTotal + routeFeeTotal + pooledGiftCost + pooledServiceFee != ticketRevenue) {
            throw com.complex.platform.common.api.ApiException.conflict("SETTLEMENT_CONSERVATION_VIOLATED",
                    "业态份额守恒校验失败(拆份额):Σ各业态净份额 " + netTotal
                            + " + Σ各业态销售渠道费 " + feeTotal
                            + " + Σ各业态通路费 " + routeFeeTotal
                            + " + 赠送权益预估成本 " + pooledGiftCost
                            + " + 综合服务费 " + pooledServiceFee + " ≠ 通票收入 " + ticketRevenue);
        }
    }

    private String idFor(String projectId, YearMonth month) {
        return "sb_" + (projectId == null ? "default" : projectId) + "_" + month;
    }

    /**
     * S7 赠付差额结算(HTML:962):按「项目 × 规则」结出 当期差额 V = 预扣 G − 实付 A。
     *
     * <p><b>G 按规则</b>:计提行落库时快照的 gift_cost_config_id 分组(冲正行沿用原行规则 id,净额天然正确);
     * <b>A 按规则</b>:兑付行按其核销的票种走与计提<b>同一个</b> {@code findPassGiftCostConfig} 归到规则——
     * 预扣关闭(整额 0,无计提行)时兑付照样归到该规则,差额为负 = 平台没预扣却真金白银付了出去,财务必须看见。
     *
     * <p>处理规则只剩 platform_absorb(ENUM L52「预扣数与日后实付数的差额也留在娱乐平台公司」;18B 复核后裁决第 11 条下线
     * 结转下期与回补当期可分配池):差额只留痕,不动池。G/A 全为 0 的规则不出行(留痕表零膨胀;K9 逐分不变基线 = 不落任何行)。
     *
     * <p>K12 接线守卫(fail-loud):Σ各规则 G 必须 ≡ 进池 G 合计——G 计提行永远带规则 id(S6 落库同写),
     * 若有 G 落库却没带规则 id,差额表会悄悄少一块,宁可当场拒绝出账。
     */
    private GiftVarianceCalc settleGiftVariance(String projectId,
                                                Instant windowStart, Instant windowEnd, Instant recoveryCutoff,
                                                long pooledGiftCost, Map<String, Long> unitGiftCostByConfig) {
        List<JdbcSettlementBillRepository.ConfigGiftCost> gRows =
                repository.aggregateGiftCostByConfig(projectId, windowStart, windowEnd, recoveryCutoff);
        java.util.LinkedHashMap<String, Long> gByConfig = new java.util.LinkedHashMap<>();
        long gSum = 0L;
        for (var g : gRows) {
            gByConfig.merge(g.configId(), g.giftCostCent(), Long::sum);
            gSum += g.giftCostCent();
        }
        // 第十八批第 2 条：有池快照的份按快照所记规则 id 计入预扣 G（售票时已固化，核销不再逐笔计提）。
        for (var g : unitGiftCostByConfig.entrySet()) {
            gByConfig.merge(g.getKey(), g.getValue(), Long::sum);
            gSum += g.getValue();
        }
        if (gSum != pooledGiftCost) {
            throw com.complex.platform.common.api.ApiException.conflict("SETTLEMENT_CONSERVATION_VIOLATED",
                    "赠付差额守恒校验失败(K12 接线):Σ各规则预扣 G " + gSum + " ≠ 进池计提合计 " + pooledGiftCost
                            + "(存在未带规则 id 的 G 计提行)");
        }
        java.util.LinkedHashMap<String, Long> aByConfig = new java.util.LinkedHashMap<>();
        java.util.LinkedHashMap<String, Long> paidByFormat = new java.util.LinkedHashMap<>();
        for (var p : repository.aggregateGiftPaid(projectId, windowStart, windowEnd, recoveryCutoff)) {
            paidByFormat.merge(p.businessFormat(), p.paidCent(), Long::sum);
            aByConfig.merge(repository.findPassGiftCostConfig(projectId, p.ticketTypeId()).id(),
                    p.paidCent(), Long::sum);
        }
        java.util.LinkedHashSet<String> configIds = new java.util.LinkedHashSet<>(gByConfig.keySet());
        configIds.addAll(aByConfig.keySet());
        List<VarianceTarget> targets = new ArrayList<>();
        for (String configId : configIds) {
            long accrued = gByConfig.getOrDefault(configId, 0L);
            long paid = aByConfig.getOrDefault(configId, 0L);
            if (accrued == 0L && paid == 0L) {
                continue; // 本期本规则无任何量,不出行。
            }
            targets.add(new VarianceTarget(configId, repository.findVarianceHandling(configId), accrued, paid));
        }
        return new GiftVarianceCalc(targets, paidByFormat);
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
                        && net.actualPaidCent() == target.actualPaidCent());
            if (unchanged) {
                continue;
            }
            if (net != null && !net.isZero()) {
                // 冲正行:现净额全额取反(处理规则按该规则现行快照取,与被冲行常同;规则中途改配亦如实留痕)。
                repository.insertVarianceRow(new JdbcSettlementBillRepository.VarianceLedgerRow(
                        varianceRowId(seq++), billId, projectId, periodStart, periodEnd, configId,
                        repository.findVarianceHandling(configId),
                        -net.accruedCostCent(), -net.actualPaidCent(), -net.varianceCent(),
                        beneficiaryEntityId, net.latestStatementId()));
            }
            if (target != null) {
                repository.insertVarianceRow(new JdbcSettlementBillRepository.VarianceLedgerRow(
                        varianceRowId(seq++), billId, projectId, periodStart, periodEnd, configId,
                        target.varianceHandling(),
                        target.accruedCostCent(), target.actualPaidCent(),
                        target.accruedCostCent() - target.actualPaidCent(),
                        beneficiaryEntityId, null));
            }
        }
    }

    /** S7 台账行 id:零填充纳秒前缀保证字典序≈时间序(冲正行可按 MAX(id) 找到最新陈述行)。 */
    private String varianceRowId(int seq) {
        return "pgv_" + String.format("%016x", System.nanoTime()) + "_" + seq;
    }

    /** S7 差额结算中间结果:各规则差额行 + 兑付实付按业态归堆(落业态份额行)。 */
    private record GiftVarianceCalc(List<VarianceTarget> targets, java.util.Map<String, Long> paidByFormat) {
    }

    /** S7 某规则本期结论:预扣 G / 实付 A(差额留娱乐平台公司,仅留痕)。 */
    private record VarianceTarget(String configId, String varianceHandling, long accruedCostCent, long actualPaidCent) {
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
     * S4/S6:净份额 = 毛额 − 摊到的销售渠道费 − 本业态核销分摊的通路费(第十八批第 3 条) − 本业态计提的赠送权益预估成本(G) − 本业态计提的门票综合服务费
     * (池链支;G 与 F 都归娱乐平台公司,瀑布顺序照 HTML:739 先 G 后 F——减法交换律下金额同,列示按此序)。
     * S7 giftPaidCent:本业态当月兑付实付(A,按提供方业态分记,HTML:962)——纯留痕<b>不进净份额</b>
     * (兑付由娱乐平台公司承担,不动各业态分账)。
     * 第十八批 X 段 176-R2 internalSettledCent:本业态有价专属直付按内部结算价应直付额(ENUM:53)——纯留痕不进净份额。
     * 第十八批 AI 段 237-R1 interEntityNetCent:域间份额(净份额中娱乐平台公司代收、应归归属主体的部分);
     * 214-R2 unverifiedNetCent:本月到期未核销归本业态归属主体的票款——两者都不进净份额。
     */
    private record FormatShare(String format, long grossCent, long platformBorneCent, long channelFeeCent,
                               long routeFeeCent, long compServiceFeeCent, long giftCostCent, long giftPaidCent,
                               long internalSettledCent, long interEntityNetCent, long unverifiedNetCent) {
        FormatShare(String format, long grossCent, long platformBorneCent) {
            this(format, grossCent, platformBorneCent, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
        }

        FormatShare(String format, long grossCent, long platformBorneCent, long channelFeeCent, long routeFeeCent,
                    long compServiceFeeCent, long giftCostCent) {
            this(format, grossCent, platformBorneCent, channelFeeCent, routeFeeCent, compServiceFeeCent, giftCostCent,
                    0L, 0L, 0L, 0L);
        }

        FormatShare withChannelFee(long fee) {
            return new FormatShare(format, grossCent, platformBorneCent, fee, routeFeeCent, compServiceFeeCent,
                    giftCostCent, giftPaidCent, internalSettledCent, interEntityNetCent, unverifiedNetCent);
        }

        FormatShare withGiftPaid(long paid) {
            return new FormatShare(format, grossCent, platformBorneCent, channelFeeCent, routeFeeCent,
                    compServiceFeeCent, giftCostCent, paid, internalSettledCent, interEntityNetCent, unverifiedNetCent);
        }

        FormatShare withInterEntity(long interEntity) {
            return new FormatShare(format, grossCent, platformBorneCent, channelFeeCent, routeFeeCent,
                    compServiceFeeCent, giftCostCent, giftPaidCent, internalSettledCent, interEntity, unverifiedNetCent);
        }

        FormatShare withUnverified(long unverified) {
            return new FormatShare(format, grossCent, platformBorneCent, channelFeeCent, routeFeeCent,
                    compServiceFeeCent, giftCostCent, giftPaidCent, internalSettledCent, interEntityNetCent, unverified);
        }

        /** 176-R2：并入本业态有价专属直付的域份额、分摊的通路费与按内部结算价应直付额。 */
        FormatShare plusExclusive(JdbcSettlementBillRepository.ExclusiveDomainShare d) {
            return new FormatShare(format, grossCent + d.domainShareCent(), platformBorneCent, channelFeeCent,
                    routeFeeCent + d.routeFeeCent(), compServiceFeeCent, giftCostCent, giftPaidCent,
                    internalSettledCent + d.internalSettledCent(), interEntityNetCent, unverifiedNetCent);
        }

        long netCent() {
            return grossCent - channelFeeCent - routeFeeCent - giftCostCent - compServiceFeeCent;
        }

        /** 第十八批第 2 条：并入本月有池快照各份的酒吧份额（毛额与各扣项均取售票时固化值按份均分）。 */
        FormatShare plus(UnitBatch u) {
            return new FormatShare(format, grossCent + u.grossCent(), platformBorneCent + u.borneCent(),
                    channelFeeCent + u.salesChannelFeeCent(), routeFeeCent + u.routeFeeCent(),
                    compServiceFeeCent + u.serviceFeeCent(), giftCostCent + u.giftCostCent(), giftPaidCent,
                    internalSettledCent, interEntityNetCent, unverifiedNetCent);
        }
    }

    /** 按月窗算出的结算单头 + 业态份额 + 赠付差额 + 分账明细 + 逐份结果 + 续算明细（落库前）。 */
    private record MonthCalc(SettlementBillRecord bill, java.util.LinkedHashMap<String, FormatShare> formatShares,
                             GiftVarianceCalc giftVariance, List<ItemCalc> items,
                             List<JdbcPassPoolRepository.UnitResult> unitResults, List<ItemCalc> resumeItems) {
    }

    /** FS-021-R2 追收明细与第十八批 AI 段 214-R1 业态追收（业态 → {域间份额之差, 应直付额之差}）。 */
    private record Recoveries(List<RecoveryCalc> items, java.util.Map<String, long[]> formatDiffs) {
    }

    /** FS-021-R2 追收明细：某家某支的追收合计（负＝向该家追回）及按原月结算单拆分。 */
    private record RecoveryCalc(String merchantId, String method, long amount, java.util.Map<String, Long> byOriginalBill) {
    }

    /**
     * 分账中间结果（method：第一段固定金额/固定单价、第二段 by_rank_tier、续算 pooled_resume、专属直付 exclusive_full）。
     * singleBarRouteFeeCent／singleBarChannelFeeCent：专属直付明细中已扣的单一酒吧门票通路费、销售渠道费（212-R1），其余分配方式为 0。
     */
    private record ItemCalc(String merchantId, int verifiedCount, int guestCount, Integer rankNo, long amount,
                            String method, long singleBarRouteFeeCent, long singleBarChannelFeeCent) {
        ItemCalc(String merchantId, int verifiedCount, int guestCount, Integer rankNo, long amount, String method) {
            this(merchantId, verifiedCount, guestCount, rankNo, amount, method, 0L, 0L);
        }
    }

    /** 第十八批第 2 条：第二段一家参与者（当期入场人数决定名次，是否在本份核销决定拿或减除）。 */
    private record SegmentBar(String merchantId, int people, boolean verified) {
    }

    /** 第二段一家的名次（并列取占用的首个名次）、适用比例（百分数，不取整）与金额（HALF_UP 到分）。 */
    private record Share(SegmentBar participant, int rankNo, BigDecimal pct, long amountCent) {
    }

    /** 本月有池快照各份的汇总（业态份额并入值、池、各栏合计、明细与逐份结果）。 */
    private record UnitBatch(int unitCount, long grossCent, long borneCent, long routeFeeCent, long salesChannelFeeCent,
                             long serviceFeeCent, long giftCostCent, Map<String, Long> giftCostByConfig,
                             long poolCent, List<ItemCalc> items, long deductionCent, long undistributedCent,
                             long suspendedCent, long residualCent, long overflowCent,
                             List<JdbcPassPoolRepository.UnitResult> results, List<ItemCalc> resumeItems) {
    }

    /** 逐份累加器。 */
    private static final class UnitAccumulator {
        int unitCount;
        long grossCent;
        long borneCent;
        long routeFeeCent;
        long salesChannelFeeCent;
        long serviceFeeCent;
        long giftCostCent;
        long poolCent;
        long deductionCent;
        long undistributedCent;
        long suspendedCent;
        long residualCent;
        long overflowCent;
        final Map<String, Long> giftCostByConfig = new LinkedHashMap<>();
        // 「商户|方式」→ 实分合计（第一段两种方式 + 第二段 by_rank_tier）。
        final Map<String, Long> paid = new LinkedHashMap<>();
        final Map<String, Long> resumed = new LinkedHashMap<>();
        final List<JdbcPassPoolRepository.UnitResult> results = new ArrayList<>();
    }
}
