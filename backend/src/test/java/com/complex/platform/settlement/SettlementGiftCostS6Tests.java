package com.complex.platform.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.ticketing.SettlementSettledEvent;
import com.complex.platform.ticketing.VerificationReversedEvent;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * S6「赠送权益预估成本(G 层)·预扣两模式」验收(V372/V373)。
 *
 * <p>业务口径(business-system-map §「通票收入分账全流程」:739 / §「综合／联合通票收入分账」:742 /
 * §「免费权益的成本承担」:737):通票的钱先扣渠道抽成,<b>再扣赠送权益预估成本(归娱乐平台公司)</b>,
 * 再扣门票综合服务费,剩余进可分配池——预扣金额按票种配置,<b>按件计</b>＝票面各权益数量 × 内部结算价之和,
 * <b>按票计</b>＝该票种赠送部分的售价整额;<b>商户让利型</b>权益平台不承担成本、不进预扣。本类七组断言:
 *
 * <ol>
 *   <li><b>K9 逐分不变(上线安全网)</b>:种子 by_ticket_amount + 整额 0 → G 恒 0 → 结算单整行
 *       {@code SELECT *} 全列比对与改造前(S4 K9 同一组手算数)逐分一致,新列仅 0/归属快照;</li>
 *   <li><b>按票计</b>:该票首笔进池核销一次性计提配置整额,后续核销不再计提;</li>
 *   <li><b>按件计</b>:Σ(quantity × num_people × 内部结算价),只数「票面原价 0 + 平台出资型 + 提供方兑付类」,
 *       商户让利型/有票面价的权益不进和;</li>
 *   <li><b>冲正取反 + 锚点复位</b>:冲正行 G 取原行之反、config 沿用,该票净额抵回 0 后可再计提;</li>
 *   <li><b>增量封顶</b>:按「项目 × 规则」月内累计,超出部分不计提;</li>
 *   <li><b>瀑布 P = R − C − G − F_pool</b>:月结时 G 从酒吧份额扣、进结算单与业态份额行,守恒成立;</li>
 *   <li><b>线性取反净零 / 专属不计提 / 无锚点拒绝计提</b>。</li>
 * </ol>
 *
 * <p>非 @Transactional(settleMonth/消费者内部真提交);@AfterEach 按项目/商户/票种清理。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SettlementGiftCostS6Tests {

    private static final YearMonth MONTH = YearMonth.of(2013, 5); // 远离今日的确定月
    private static final Instant AT = MONTH.atDay(12).atTime(21, 0).toInstant(ZoneOffset.UTC);

    @Autowired private SettlementSettledConsumer settledConsumer;
    @Autowired private SettlementReversedConsumer reversedConsumer;
    @Autowired private TicketDistributionSettlementService settlementService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final List<String> projectIds = new ArrayList<>();
    private final List<String> merchants = new ArrayList<>();
    private final List<String> giftConfigIds = new ArrayList<>();
    private final List<String> ticketTypeIds = new ArrayList<>();
    private final List<String> templateIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
    }

    @AfterEach
    void cleanup() {
        com.complex.platform.support.TestSecurityActors.clear();
        for (String m : merchants) {
            com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate,
                    "DELETE FROM settlement_transactions WHERE merchant_id = ?", m);
        }
        for (String pid : projectIds) {
            com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate,
                    "DELETE FROM settlement_transactions WHERE project_id = ?", pid);
            com.complex.platform.support.AppendOnlyTestBypass.updateOnTable(jdbcTemplate,
                    "pass_gift_cost_variance_ledger", "trg_append_only_ud",
                    "DELETE FROM pass_gift_cost_variance_ledger WHERE project_id = ?", pid);
            jdbcTemplate.update(
                    "DELETE FROM settlement_items WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)",
                    pid);
            jdbcTemplate.update(
                    "DELETE FROM settlement_bill_format_shares WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)",
                    pid);
            jdbcTemplate.update("DELETE FROM settlement_bills WHERE project_id = ?", pid);
            jdbcTemplate.update("DELETE FROM ticket_distribution_rank_tiers WHERE project_id = ?", pid);
        }
        for (String id : giftConfigIds) {
            jdbcTemplate.update("DELETE FROM pass_gift_cost_configs WHERE id = ?", id);
        }
        for (String id : ticketTypeIds) {
            jdbcTemplate.update("DELETE FROM ticket_type_entitlements WHERE ticket_type_id = ?", id);
            jdbcTemplate.update("DELETE FROM ticket_types WHERE id = ?", id);
        }
        for (String id : templateIds) {
            jdbcTemplate.update("DELETE FROM entitlement_templates WHERE id = ?", id);
            jdbcTemplate.update("DELETE FROM internal_price_catalog WHERE item_code = ?", "sku_et_" + id);
        }
        projectIds.clear();
        merchants.clear();
        giftConfigIds.clear();
        ticketTypeIds.clear();
        templateIds.clear();
    }

    // ───────────────── ① K9 逐分不变:种子 by_ticket_amount + 0 → G 恒 0,整行全列一致 ─────────────────

    /**
     * <b>K9</b>:不配任何项目级 G 规则(命中 V373 全局种子 pgc_default = by_ticket_amount + 整额 0)时,
     * 复算 S4 K9 同一组数(4 家酒吧、收入 150000、费 2000bps):结算单<b>每一列</b>与改造前手算值逐分一致,
     * S6 新列只多出 G=0 与归属快照;酒吧业态份额行 G=0、净份额不变。
     */
    @Test
    void k9_defaultSeedZero_everyBillColumnMatchesPreS6Figures() {
        String pid = newProject();
        seedVerified(pid, "bar1", 20, 8, 60000L, 12000L, 0L);
        seedVerified(pid, "bar2", 15, 6, 40000L, 8000L, 0L);
        seedVerified(pid, "bar3", 10, 4, 30000L, 6000L, 0L);
        seedVerified(pid, "bar4", 5, 2, 20000L, 4000L, 0L);

        String billId = settlementService.settleMonth(pid, MONTH);

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("id", billId);
        expected.put("project_id", pid);
        expected.put("period_start", java.sql.Date.valueOf(MONTH.atDay(1)));
        expected.put("period_end", java.sql.Date.valueOf(MONTH.atEndOfMonth()));
        expected.put("ticket_revenue_cent", 150000L);
        expected.put("channel_fee_cent", 0L);
        expected.put("comp_service_fee_pooled_cent", 30000L);
        expected.put("available_pool_cent", 120000L);
        // S8 第二段归一化分尽:档位 1600/1200×3(Σ=5200)对 120000 一次分光 → Σ=119999+尾差1。
        expected.put("distributed_cent", 119999L);
        expected.put("comp_service_fee_exclusive_cent", 0L);
        expected.put("fee_beneficiary_entity_id", "be_ent_platform");
        // S6 新列:G 恒 0(逐分不变的全部含义——老列一分不动,新列只记 0 与收款方快照)。
        expected.put("gift_cost_cent", 0L);
        expected.put("gift_beneficiary_entity_id", "be_ent_platform"); // V372 归属种子:归娱乐平台公司
        expected.put("distribution_method", "by_rank_tier");
        expected.put("special_approval_ref", null);
        expected.put("special_approval_valid_until", null);
        expected.put("status", "confirmed");
        expected.put("confirmed_at", null);
        expected.put("settled_at", null);
        expected.put("attributed_entity_id", "be_ent_platform"); // V375 all_pass_ticket 全局种子
        expected.put("combo_discount_cent", 0L);
        expected.put("rounding_residual_cent", 1L);
        expected.put("pool_undistributed_cent", 0L);        // S8:第二段有参与者 → 恒 0
        expected.put("pool_undistributed_entity_id", null);

        Map<String, Object> actual = new HashMap<>(bill(pid));
        actual.remove("created_at");
        actual.remove("updated_at");
        assertThat(actual)
                .as("K9:种子 by_ticket_amount+0 时结算单每一列与改造前(S4 K9)逐分一致,G 恒 0")
                .isEqualTo(expected);

        // 业态份额行:酒吧行 G=0,净份额 = 150000 − 30000 = 120000(与改造前一致)。
        Map<String, Object> barShare = formatShare(billId, "bar");
        assertThat(((Number) barShare.get("gift_cost_cent")).longValue()).isZero();
        assertThat(((Number) barShare.get("net_revenue_cent")).longValue()).isEqualTo(120000L);
    }

    // ───────────────── ② 按票计:首笔进池核销一次性计提整额 ─────────────────

    /** 按票计 5900:同一张票核销 3 次只有首笔计提 5900,其余 0;不同的票各计各的一道。 */
    @Test
    void byTicketAmount_firstPooledVerificationProvisionsWholeAmountOnce() {
        String pid = newProject();
        String merchant = merchant();
        seedGiftConfig(pid, null, "by_ticket_amount", 5900L, null);
        String t1 = "tkt_bta1_" + Long.toHexString(System.nanoTime());
        String t2 = "tkt_bta2_" + Long.toHexString(System.nanoTime());

        verifyPooled(pid, merchant, t1, "vbta1_" + t1, 1);
        verifyPooled(pid, merchant, t1, "vbta2_" + t1, 2);
        verifyPooled(pid, merchant, t1, "vbta3_" + t1, 3);
        verifyPooled(pid, merchant, t2, "vbta1_" + t2, 1);

        assertThat(giftOf("ST-VS-vbta1_" + t1)).as("首笔进池核销计提整额").isEqualTo(5900L);
        assertThat(giftOf("ST-VS-vbta2_" + t1)).as("同一张票第 2 笔不再计提").isZero();
        assertThat(giftOf("ST-VS-vbta3_" + t1)).isZero();
        assertThat(giftOf("ST-VS-vbta1_" + t2)).as("另一张票是另一道,照计").isEqualTo(5900L);
        assertThat(ticketGiftNet(t1)).as("一张票 G 净额恒 = 一道").isEqualTo(5900L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT gift_cost_config_id FROM settlement_transactions WHERE transaction_no = ?",
                String.class, "ST-VS-vbta1_" + t1))
                .as("计提行落解析到的规则 id(供封顶月累计 + 冲正沿用)").isNotNull();
    }

    // ───────────────── ③ 按件计:Σ(数量 × 人数 × 内部结算价),三判据过滤 ─────────────────

    /**
     * 票面三条 binding:①平台出资型赠饮(票面价 0,签约内部价 800,数量 2 × 每份 1 人)→ 进和 1600;
     * ②商户让利型赠品(内部价 500)→ 平台不承担成本,不进和;③付费入场(票面价 10000)→ 参与拆份额,不是赠送,不进和。
     * → G = 1600。
     */
    @Test
    void byItem_sumsOnlyPlatformFundedZeroPricedProviderRedeemedEntitlements() {
        String pid = newProject();
        String merchant = merchant();
        String ticketType = seedTicketType("byitem");
        seedGiftBinding(ticketType, "physical", 800L, 2, 1, "platform_funded", 0L);
        seedGiftBinding(ticketType, "physical", 500L, 1, 1, "merchant_concession", 0L);
        seedGiftBinding(ticketType, "virtual", null, 1, 1, "platform_funded", 10000L); // 付费组成
        seedGiftConfig(pid, ticketType, "by_item", null, null);
        String ticket = "tkt_bi_" + Long.toHexString(System.nanoTime());

        verifyPooledWithType(pid, merchant, ticketType, ticket, "vbi1_" + ticket, 1);

        assertThat(giftOf("ST-VS-vbi1_" + ticket))
                .as("按件计 = 2×1×800(平台出资型赠饮);让利型/付费组成不进和")
                .isEqualTo(1600L);
    }

    // ───────────────── ④ 冲正取反 + 锚点复位 ─────────────────

    /** 冲掉计提了 G 的首笔核销:冲正行 G 取反、config 沿用 → 净额抵回 0 → 下一笔重新计提;全生命周期净额恒一道。 */
    @Test
    void reversalNegatesGiftCost_anchorResets_nextVerificationProvisionsAgain() {
        String pid = newProject();
        String merchant = merchant();
        seedGiftConfig(pid, null, "by_ticket_amount", 5900L, null);
        String ticket = "tkt_rev_" + Long.toHexString(System.nanoTime());
        String v1 = "vgrv1_" + ticket;
        String v3 = "vgrv3_" + ticket;

        verifyPooled(pid, merchant, ticket, v1, 1);
        assertThat(giftOf("ST-VS-" + v1)).isEqualTo(5900L);

        reversedConsumer.onVerificationReversed(new VerificationReversedEvent(
                v1, "rev_" + v1, "ent_" + ticket, "ord_x", merchant, 1));

        assertThat(giftOf("ST-VR-rev_" + v1)).as("冲正行 G 取原行之反").isEqualTo(-5900L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT gift_cost_config_id FROM settlement_transactions WHERE transaction_no = ?",
                String.class, "ST-VR-rev_" + v1)).as("冲正行沿用原行规则 id").isNotNull();
        assertThat(ticketGiftNet(ticket)).as("冲正后该票 G 净额抵回 0(锚点复位)").isZero();

        verifyPooled(pid, merchant, ticket, v3, 2);
        assertThat(giftOf("ST-VS-" + v3)).as("锚点复位后重新计提一道").isEqualTo(5900L);
        assertThat(ticketGiftNet(ticket)).as("全生命周期净额恒 = 一道").isEqualTo(5900L);
    }

    // ───────────────── ⑤ 增量封顶:按「项目 × 规则」月内累计 ─────────────────

    /** 整额 5900、封顶 8000:第 1 张票 5900,第 2 张票只剩 2100,第 3 张票 0——增量封顶,不月末 clamp。 */
    @Test
    void monthlyCapAppliesIncrementallyAcrossTickets() {
        String pid = newProject();
        String merchant = merchant();
        seedGiftConfig(pid, null, "by_ticket_amount", 5900L, 8000L);
        String t1 = "tkt_cap1_" + Long.toHexString(System.nanoTime());
        String t2 = "tkt_cap2_" + Long.toHexString(System.nanoTime());
        String t3 = "tkt_cap3_" + Long.toHexString(System.nanoTime());

        verifyPooled(pid, merchant, t1, "vcap_" + t1, 1);
        verifyPooled(pid, merchant, t2, "vcap_" + t2, 1);
        verifyPooled(pid, merchant, t3, "vcap_" + t3, 1);

        assertThat(giftOf("ST-VS-vcap_" + t1)).isEqualTo(5900L);
        assertThat(giftOf("ST-VS-vcap_" + t2)).as("月内剩余额度 8000−5900").isEqualTo(2100L);
        assertThat(giftOf("ST-VS-vcap_" + t3)).as("额度用尽不再计提").isZero();
    }

    // ───────────────── ⑥ 瀑布:P = R − C − G − F_pool,G 落业态份额行 ─────────────────

    /**
     * 直接落账本形态(消费者产物):收入 100000、费 20000、G 5900 → 池 = 100000 − 0 − 5900 − 20000 = 74100;
     * 单档 100% 分尽;结算单 G 列 5900、归属快照娱乐平台公司;酒吧业态份额行 G=5900、净份额=74100。
     */
    @Test
    void waterfall_availablePoolEqualsRevenueMinusChannelMinusGiftMinusFee() {
        String pid = newProject();
        setRankTier(pid, 1, 9, 10000);
        seedVerified(pid, "bar_wf", 10, 4, 100000L, 20000L, 5900L);

        String billId = settlementService.settleMonth(pid, MONTH);

        Map<String, Object> b = bill(pid);
        assertThat(num(b, "ticket_revenue_cent")).isEqualTo(100000L);
        assertThat(num(b, "gift_cost_cent")).as("G 进结算单留痕").isEqualTo(5900L);
        assertThat(num(b, "comp_service_fee_pooled_cent")).isEqualTo(20000L);
        assertThat(num(b, "available_pool_cent")).as("P = R − C − G − F_pool").isEqualTo(74100L);
        assertThat(num(b, "distributed_cent")).isEqualTo(74100L);
        assertThat(b.get("gift_beneficiary_entity_id")).as("G 归娱乐平台公司(V372 种子)")
                .isEqualTo("be_ent_platform");

        Map<String, Object> barShare = formatShare(billId, "bar");
        assertThat(((Number) barShare.get("gift_cost_cent")).longValue())
                .as("G 按业态跟核销走:落在酒吧业态行").isEqualTo(5900L);
        assertThat(((Number) barShare.get("net_revenue_cent")).longValue()).isEqualTo(74100L);
    }

    // ───────────────── ⑦ 线性取反净零 / 专属不计提 / 无锚点拒绝 ─────────────────

    /** 同月「核销 + 对应冲正」→ 结算单各金额列(含 G)全部净零。 */
    @Test
    void verificationPlusReversalNetsEveryAmountColumnToZero() {
        String pid = newProject();
        setRankTier(pid, 1, 9, 10000);
        seedVerified(pid, "bar_net6", 5, 2, 50000L, 10000L, 5900L);
        seedReversal(pid, "bar_net6", 5, 2, 50000L, 10000L, 5900L);

        settlementService.settleMonth(pid, MONTH);

        Map<String, Object> b = bill(pid);
        for (String col : List.of("ticket_revenue_cent", "channel_fee_cent", "gift_cost_cent",
                "comp_service_fee_pooled_cent", "available_pool_cent", "distributed_cent",
                "comp_service_fee_exclusive_cent", "combo_discount_cent", "rounding_residual_cent")) {
            assertThat(num(b, col)).as("线性取反后 %s 必须净零", col).isZero();
        }
    }

    /** 专属直付核销不进池,其净额上不扣 G → 不计提(G 是池链扣项,必须落在进池行上月结才看得见)。 */
    @Test
    void exclusiveVerificationDoesNotProvisionGiftCost() {
        String pid = newProject();
        String merchant = merchant();
        seedGiftConfig(pid, null, "by_ticket_amount", 5900L, null);
        String ticket = "tkt_ex_" + Long.toHexString(System.nanoTime());
        String vid = "vex_" + ticket;

        settledConsumer.onSettlementSettled(new SettlementSettledEvent(
                vid, AT, "ent_" + ticket, "ord_x", "tt_x", "entry", merchant,
                5, 1, 1, 50000L, AT.plusSeconds(86400), merchant, null, null, pid, ticket));

        assertThat(giftOf("ST-VS-" + vid)).as("专属行不计提 G").isZero();
    }

    /** parent_ticket_id 为空 = 不合规发放数据 → 拒绝计提(与 S3 综合服务费同一态度);核销流水本身照落。 */
    @Test
    void missingParentTicket_refusesGiftAccrual() {
        String pid = newProject();
        String merchant = merchant();
        seedGiftConfig(pid, null, "by_ticket_amount", 5900L, null);
        String vid = "vnog_" + Long.toHexString(System.nanoTime());

        settledConsumer.onSettlementSettled(new SettlementSettledEvent(
                vid, AT, "ent_" + vid, "ord_x", "tt_x", "entry", merchant,
                1, 1, 1, 10000L, AT.plusSeconds(86400), null, null, null, pid, null));

        assertThat(giftOf("ST-VS-" + vid)).as("无锚点直接拒绝计提").isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT verified_amount_cent FROM settlement_transactions WHERE transaction_no = ?",
                Long.class, "ST-VS-" + vid)).isEqualTo(10000L);
    }

    // ─────────────────────────── helpers ───────────────────────────

    private String newProject() {
        String pid = "proj_s6_" + System.nanoTime();
        projectIds.add(pid);
        return pid;
    }

    private String merchant() {
        String m = "bars6_" + Long.toHexString(System.nanoTime());
        merchants.add(m);
        return m;
    }

    /** 项目级 G 预扣规则(ticketTypeId 可空=该项目所有票种通用),直接生效(is_draft=FALSE)。 */
    private void seedGiftConfig(String projectId, String ticketTypeId, String mode,
                                Long ticketGiftAmountCent, Long capCent) {
        String id = "pgc_s6_" + Long.toHexString(System.nanoTime());
        giftConfigIds.add(id);
        jdbcTemplate.update(
                "INSERT INTO pass_gift_cost_configs (id, project_id, ticket_type_id, prededuct_mode,"
                        + " ticket_gift_amount_cent, monthly_cap_amount_cent, status, is_draft, updated_by)"
                        + " VALUES (?, ?, ?, ?, ?, ?, 'active', FALSE, 'tester')",
                id, projectId, ticketTypeId, mode, ticketGiftAmountCent, capCent);
    }

    private String seedTicketType(String label) {
        String id = "tts6_" + label + "_" + Long.toHexString(System.nanoTime());
        ticketTypeIds.add(id);
        jdbcTemplate.update(
                "INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status,"
                        + " created_at, updated_at) VALUES (?, ?, 'fixed_price', FALSE, 'active', now(), now())",
                id, "S6测试票种-" + label);
        return id;
    }

    /**
     * 票面 binding + 模板:kind=physical 走签约内部价,提供方商户挂模板;
     * componentPriceCent=0 即免费/赠送权益,>0 即付费组成(参与拆份额,不进 G)。
     * 批10/E(V380)收编形态:有价模板 = 目录生效条目 + 模板挂引用 + agreement_price_cent 转快照;
     * 无价(agreementPriceCent=null)= 不挂引用(解析 fail-closed,与旧 NULL 签约价同语义)。
     */
    private void seedGiftBinding(String ticketTypeId, String kind, Long agreementPriceCent,
                                 int quantity, int numPeople, String costBearer, long componentPriceCent) {
        String tpl = "ets6_" + Long.toHexString(System.nanoTime());
        templateIds.add(tpl);
        String itemCode = agreementPriceCent == null ? null : "sku_et_" + tpl;
        if (itemCode != null) {
            jdbcTemplate.update(
                    "INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at)"
                            + " VALUES ('m_prov_s6', 'S6测试提供方', 'bar', 'active', now(), now())"
                            + " ON CONFLICT (id) DO NOTHING");
            jdbcTemplate.update(
                    "INSERT INTO internal_price_catalog (id, item_code, merchant_id, category, item_name,"
                            + " price_cent, status, is_draft, updated_by, created_at, updated_at)"
                            + " VALUES (?, ?, 'm_prov_s6', 'gift_item', ?, ?, 'active', FALSE, 'seed_s6', now(), now())",
                    itemCode, itemCode, "S6模板-" + tpl, agreementPriceCent);
        }
        jdbcTemplate.update(
                "INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times,"
                        + " default_value_cent, status, merchant_id, entitlement_kind, agreement_price_cent,"
                        + " catalog_item_code, created_at, updated_at)"
                        + " VALUES (?, ?, 'gift_item', 1, 0, 'active', 'm_prov_s6', ?, ?, ?, now(), now())",
                tpl, "S6模板-" + tpl, kind, agreementPriceCent, itemCode);
        jdbcTemplate.update(
                "INSERT INTO ticket_type_entitlements (id, ticket_type_id, entitlement_template_id, quantity,"
                        + " total_times, value_amount_cent, num_people, component_price_cent, cost_bearer,"
                        + " created_at, updated_at) VALUES (?, ?, ?, ?, 1, 0, ?, ?, ?, now(), now())",
                "ttes6_" + Long.toHexString(System.nanoTime()), ticketTypeId, tpl, quantity,
                numPeople, componentPriceCent, costBearer);
    }

    /** 进池(非专属)核销:同一张票(parent=ticket)的第 used 次核销(allocated 50000/5 次,增量各 10000)。 */
    private void verifyPooled(String projectId, String merchant, String ticket, String verificationId, int used) {
        verifyPooledWithType(projectId, merchant, "tt_x", ticket, verificationId, used);
    }

    private void verifyPooledWithType(String projectId, String merchant, String ticketTypeId,
                                      String ticket, String verificationId, int used) {
        settledConsumer.onSettlementSettled(new SettlementSettledEvent(
                verificationId, AT.plusSeconds(used), "ent_" + ticket, "ord_x", ticketTypeId, "entry", merchant,
                5, used, 1, 50000L, AT.plusSeconds(86400), null, null, null, projectId, ticket));
    }

    /** 直接落账本形态的进池核销行(消费者产物,带 G)。 */
    private void seedVerified(String projectId, String merchantId, int verifiedCount, int numPeople,
                              long verifiedCent, long compServiceFeeCent, long giftCostCent) {
        insert(projectId, merchantId, "verification_settled",
                verifiedCount, numPeople, verifiedCent, compServiceFeeCent, giftCostCent);
    }

    private void seedReversal(String projectId, String merchantId, int verifiedCount, int numPeople,
                              long verifiedCent, long compServiceFeeCent, long giftCostCent) {
        insert(projectId, merchantId, "verification_reversed",
                -verifiedCount, -numPeople, -verifiedCent, -compServiceFeeCent, -giftCostCent);
    }

    private void insert(String projectId, String merchantId, String type, int verifiedCount,
                        int numPeople, long verifiedCent, long compServiceFeeCent, long giftCostCent) {
        java.sql.Timestamp ts = java.sql.Timestamp.from(AT);
        String id = "st_s6_" + System.nanoTime();
        com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate, """
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    entitlement_type, verified_count, total_times, num_people, allocated_revenue_cent,
                    verified_amount_cent, unverified_amount_cent, beneficiary_type, merchant_id, project_id,
                    verification_id, settlement_status, entitlement_valid_to, business_at, created_at, updated_at,
                    comp_service_fee_cent, gift_cost_cent, gift_cost_config_id)
                VALUES (?, ?, ?, ?, 'entry', ?, ?, ?, ?, ?, 0, 'merchant', ?, ?, ?, 'draft', ?, ?, ?, ?, ?, ?, ?)
                """,
                id, "TN-" + id, type, "ent_" + id, verifiedCount,
                Math.max(Math.abs(verifiedCount), 1), numPeople, verifiedCent, verifiedCent,
                merchantId, projectId, "vf_" + id, ts, ts, ts, ts, compServiceFeeCent, giftCostCent,
                // S7 K12 接线守卫要求:G 非零的计提/冲正行必带规则 id(生产路径由消费者同写;夹具挂全局种子)。
                giftCostCent == 0L ? null : "pgc_default");
    }

    private void setRankTier(String projectId, int rankFrom, int rankTo, int shareBps) {
        jdbcTemplate.update(
                "INSERT INTO ticket_distribution_rank_tiers (id, project_id, rank_from, rank_to, share_bps,"
                        + " status, is_draft) VALUES (?, ?, ?, ?, ?, 'active', FALSE)",
                "trt_s6_" + Long.toHexString(System.nanoTime()), projectId, rankFrom, rankTo, shareBps);
    }

    private long giftOf(String transactionNo) {
        return jdbcTemplate.queryForObject(
                "SELECT gift_cost_cent FROM settlement_transactions WHERE transaction_no = ?",
                Long.class, transactionNo);
    }

    private long ticketGiftNet(String parentTicketId) {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(gift_cost_cent), 0) FROM settlement_transactions WHERE parent_ticket_id = ?",
                Long.class, parentTicketId);
    }

    private Map<String, Object> bill(String projectId) {
        return jdbcTemplate.queryForMap("SELECT * FROM settlement_bills WHERE project_id = ?", projectId);
    }

    private Map<String, Object> formatShare(String billId, String format) {
        return jdbcTemplate.queryForMap(
                "SELECT * FROM settlement_bill_format_shares WHERE bill_id = ? AND business_format = ?",
                billId, format);
    }

    private static long num(Map<String, Object> row, String col) {
        return ((Number) row.get(col)).longValue();
    }
}
