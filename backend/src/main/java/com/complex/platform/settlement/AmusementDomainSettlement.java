package com.complex.platform.settlement;

// 文件版本：V0.01

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.ArApItemRecord;
import com.complex.platform.finance.ArApItemSourceRecord;
import com.complex.platform.finance.FinanceRepository;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.ticketing.InternalSettlementPriceResolver;
import com.complex.platform.ticketing.SettlementSettledEvent;
import com.complex.platform.ticketing.VerificationReversedEvent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BR 段第 357 条 357-R2、357-R3 游乐域内结算。依据：BSM:753「游乐公司再按已批准的内部结算价或域内规则，与实际提供服务的游乐商户结算。」；
 * BSM:1689「通票权益核销后才按所属域的已生效规则与实际提供方结算；未核销不产生提供方结算。」；BSM:1671「核销或结算时把当时价格固化到流水，历史不得手改。」；
 * BSM:1673「通票分账、单店门票结付、游乐域内结算、餐饮与零售的券结算，一律沿用与积分结算相同的账期：自然月计费、次月出账；付款落在三个付账日上。」；
 * BSM:755「收支两条线，不得轧差」；ENUM:190「非最终快照的数据一律不得进入结算」；第十八批裁决 35「游乐等按综合体逐日查」。
 * <ul>
 *   <li>357-R2：游乐入场核销（{@link SettlementSettledEvent}，与入场同一事务同步监听）对引用了内部结算价目录条目的项目，按项目所属商户取当时生效价并带价格版本
 *       固化一行 settled（营业日取该次入场原件锁定的营业日）；取不到价 400 ENTITLEMENT_PRICE_UNRESOLVED 整笔入场回滚。补偿补发权益另按 FS-012 结算，不记。
 *       核销撤销（{@link VerificationReversedEvent}）记一行金额取反的 reversed。</li>
 *   <li>357-R3：由 {@link AmusementDomainSettlementJob} 每日一轮，按（综合体、游乐商户、付款方、营业月、正常或追收）合并入账，写法照
 *       {@link GeneralCardConsumptionSettlement}：只入已结束且该综合体两类数据已标最终的营业月；正常组挂游乐公司对游乐商户的应付，撤销晚于原核销营业月的
 *       另成追收组挂应收；缺钱路付账日规则 409 PAY_DAY_NOT_CONFIGURED 整组回滚、下一轮重试。</li>
 * </ul>
 */
@Service
public class AmusementDomainSettlement {

    public static final String ROUTE = "amusement_domain_settlement";
    private static final List<String> FINAL_CATEGORIES = List.of("orders_verifications", "refunds_reversals");

    private final NamedParameterJdbcTemplate jdbc;
    private final FinanceRepository finance;
    private final FinanceService financeService;
    private final InternalPayDayService payDays;
    private final BusinessDayResolver businessDays;
    private final RevenueStreamAttributionRepository attribution;
    private final InternalSettlementPriceResolver prices;
    private final AuditEventService audit;

    public AmusementDomainSettlement(NamedParameterJdbcTemplate jdbc, FinanceRepository finance, FinanceService financeService,
            InternalPayDayService payDays, BusinessDayResolver businessDays, RevenueStreamAttributionRepository attribution,
            InternalSettlementPriceResolver prices, AuditEventService audit) {
        this.jdbc = jdbc;
        this.finance = finance;
        this.financeService = financeService;
        this.payDays = payDays;
        this.businessDays = businessDays;
        this.attribution = attribution;
        this.prices = prices;
        this.audit = audit;
    }

    // ─── 357-R2 逐笔固化 ───────────────────────────────────────────

    @EventListener
    public void onSettled(SettlementSettledEvent event) {
        if (event.entitlementType() == null || !event.entitlementType().startsWith("amusement") || event.compensationItemId() != null) {
            return;
        }
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT av.project_id, av.business_date, p.merchant_id, p.catalog_item_code
                  FROM ticket_verifications tv
                  JOIN amusement_verifications av ON av.id = tv.amusement_verification_id
                  JOIN amusement_projects p ON p.id = av.project_id
                 WHERE tv.id = :id AND p.catalog_item_code IS NOT NULL
                """, Map.of("id", event.verificationId()));
        if (rows.isEmpty()) {
            return; // 不是游乐入场原件，或项目未引用目录条目（不参与域内结算）
        }
        Map<String, Object> row = rows.getFirst();
        String merchant = (String) row.get("merchant_id");
        String payer = attribution.resolveManagementEntity(event.projectId(), "amusement_ticket");
        if (payer == null) {
            throw ApiException.conflict("REVENUE_STREAM_ATTRIBUTION_MISSING",
                    "定不出游乐域内结算的付款方：综合体「" + event.projectId() + "」游乐票收入归属未配置；请先补齐");
        }
        InternalSettlementPriceResolver.VersionedPrice price = prices.requireVersioned(null, merchant, (String) row.get("catalog_item_code"));
        jdbc.update("""
                INSERT INTO amusement_domain_settlement_lines (id, verification_id, kind, amusement_project_id, project_id, provider_merchant_id,
                    payer_entity_id, amount_cent, price_catalog_entry_id, business_at, business_date)
                VALUES (:id, :verification, 'settled', :amusement, :project, :merchant, :payer, :amount, :entry, :at, :date)
                ON CONFLICT (verification_id, kind) DO NOTHING
                """, new MapSqlParameterSource().addValue("id", newId()).addValue("verification", event.verificationId())
                .addValue("amusement", row.get("project_id")).addValue("project", event.projectId()).addValue("merchant", merchant)
                .addValue("payer", payer).addValue("amount", price.priceCent()).addValue("entry", price.catalogEntryId())
                .addValue("at", Timestamp.from(event.verifiedAt())).addValue("date", row.get("business_date")));
    }

    @EventListener
    public void onReversed(VerificationReversedEvent event) {
        List<String> projects = jdbc.queryForList("""
                SELECT project_id FROM amusement_domain_settlement_lines s
                 WHERE verification_id = :v AND kind = 'settled'
                   AND NOT EXISTS (SELECT 1 FROM amusement_domain_settlement_lines r WHERE r.verification_id = :v AND r.kind = 'reversed')
                """, Map.of("v", event.verificationId()), String.class);
        if (projects.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO amusement_domain_settlement_lines (id, verification_id, kind, amusement_project_id, project_id, provider_merchant_id,
                    payer_entity_id, amount_cent, price_catalog_entry_id, business_at, business_date)
                SELECT :id, verification_id, 'reversed', amusement_project_id, project_id, provider_merchant_id, payer_entity_id, -amount_cent,
                       price_catalog_entry_id, :at, :date
                  FROM amusement_domain_settlement_lines WHERE verification_id = :v AND kind = 'settled'
                ON CONFLICT (verification_id, kind) DO NOTHING
                """, new MapSqlParameterSource().addValue("id", newId()).addValue("v", event.verificationId())
                .addValue("at", Timestamp.from(now)).addValue("date", businessDays.businessDate(projects.getFirst(), null, now)));
    }

    // ─── 357-R3 按营业月入账 ───────────────────────────────────────

    /** 一条待入账的域内结算行：recovery＝撤销所在营业月晚于同一核销 settled 行的营业月（追收组）。 */
    record PendingLine(String id, String projectId, String merchantId, String payerEntityId, long amountCent, LocalDate businessDate,
            boolean recovery) {
    }

    /** 一组（综合体、游乐商户、付款方、营业月、正常或追收）的待入账行。 */
    public record Group(String projectId, String merchantId, String payerEntityId, YearMonth month, boolean recovery, List<PendingLine> lines) {
    }

    private record GroupKey(String projectId, String merchantId, String payerEntityId, YearMonth month, boolean recovery) {
    }

    /** 尚未入账、所属营业月已结束且该综合体该月两类数据已标最终的行，按（综合体、游乐商户、付款方、营业月、正常或追收）分组。 */
    public List<Group> pendingGroups(Instant now) {
        List<PendingLine> lines = jdbc.query("""
                SELECT l.id, l.project_id, l.provider_merchant_id, l.payer_entity_id, l.amount_cent, l.business_date,
                       date_trunc('month', s.business_date) < date_trunc('month', l.business_date) AS recovery
                  FROM amusement_domain_settlement_lines l
                  JOIN amusement_domain_settlement_lines s ON s.verification_id = l.verification_id AND s.kind = 'settled'
                 WHERE l.posted_at IS NULL
                 ORDER BY l.business_at, l.id
                """, Map.of(), (rs, i) -> new PendingLine(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getLong(5),
                rs.getObject(6, LocalDate.class), rs.getBoolean(7)));
        Map<GroupKey, List<PendingLine>> groups = new LinkedHashMap<>();
        for (PendingLine line : lines) {
            YearMonth month = YearMonth.from(line.businessDate());
            if (!month.isBefore(YearMonth.from(businessDays.businessDate(line.projectId(), null, now)))) {
                continue;
            }
            groups.computeIfAbsent(new GroupKey(line.projectId(), line.merchantId(), line.payerEntityId(), month, line.recovery()),
                    k -> new ArrayList<>()).add(line);
        }
        return groups.entrySet().stream()
                .filter(e -> financeService.nonFinalDays(e.getKey().merchantId(), e.getKey().projectId(), e.getKey().month().atDay(1),
                        e.getKey().month().atEndOfMonth(), FINAL_CATEGORIES).isEmpty())
                .map(e -> new Group(e.getKey().projectId(), e.getKey().merchantId(), e.getKey().payerEntityId(), e.getKey().month(),
                        e.getKey().recovery(), e.getValue()))
                .toList();
    }

    /**
     * 一组合并为一笔游乐公司与游乐商户之间的普通内部结算往来：正常组挂应付（净额为 0＝同月全额冲回，不挂），追收组挂应收；组内各行回写台账行与入账时刻。
     * 付款方是游乐公司不是该商户，台账行不填 merchant_id（同 AmusementCompensationFulfillmentAdapter 先例）。
     */
    @Transactional
    public void postGroup(Group g, Instant now) {
        long net = g.lines().stream().mapToLong(PendingLine::amountCent).sum();
        long amount = g.recovery() ? -net : net;
        String itemId = null;
        if (amount > 0) {
            String digest = digest(g.lines().stream().map(PendingLine::id).sorted().toList());
            itemId = (g.recovery() ? "arap_adr_" : "arap_ads_") + digest;
            InternalPayDayService.Schedule schedule = payDays.schedule(ROUTE, g.projectId(), g.month().atDay(1), now);
            ArApItemRecord item = new ArApItemRecord(itemId, (g.recovery() ? "ARAP-ADR-" : "ARAP-ADS-") + digest,
                    g.recovery() ? "receivable" : "payable", g.merchantId(), ROUTE, digest, amount, 0L, amount, "CNY",
                    schedule.dueDate(), g.month().toString(), g.recovery() ? "pending_collection" : "pending_payment", now, now,
                    null, g.projectId());
            finance.appendArApItem(item);
            finance.saveArApItemSource(new ArApItemSourceRecord("arap_src_" + itemId, itemId, ROUTE, digest, amount, now));
            payDays.record(item, schedule, g.projectId());
            audit.recordSuccess("settlement.amusement_domain.posted", "not_applicable", "ar_ap_item", itemId, null, itemId,
                    Map.of("provider_merchant_id", g.merchantId(), "payer_entity_id", g.payerEntityId(),
                            "accounting_period", g.month().toString(), "recovery", g.recovery(), "amount_cent", amount));
        }
        jdbc.update("UPDATE amusement_domain_settlement_lines SET ar_ap_item_id = :item, posted_at = :now WHERE id IN (:ids) AND posted_at IS NULL",
                new MapSqlParameterSource().addValue("item", itemId).addValue("now", Timestamp.from(now))
                        .addValue("ids", g.lines().stream().map(PendingLine::id).toList()));
    }

    private static String newId() {
        return "adsl_" + UUID.randomUUID().toString().replace("-", "");
    }

    /** 组内行号（已排序）拼接的 SHA-256 前 20 位：同一组行同一编号，晚到的行另成一笔。 */
    private static String digest(List<String> ids) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(String.join(",", ids).getBytes(StandardCharsets.UTF_8))).substring(0, 20);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
