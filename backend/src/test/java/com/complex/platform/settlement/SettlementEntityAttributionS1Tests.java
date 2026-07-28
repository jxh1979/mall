package com.complex.platform.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * S1「主体与归属层」验收(V354 / V355)。
 *
 * <p>业务上这一刀只回答一个问题:<b>每一类收入到底归谁</b>。它不改任何一分钱的算法——
 * 归属只是结算单上的一个「收款方」标注。所以本类最核心的一条断言是
 * <b>K:归属配没配,结算单每一列金额逐分不变</b>;财务对账时看到的数字不会因为本刀而动。
 *
 * <ol>
 *   <li><b>K 恒等式</b>:同一项目同一月,先在「归属未配置」下结一次,再配上归属重结一次 ——
 *       结算单除 {@code attributed_entity_id} 外<b>每一列完全相同</b>,明细金额逐条相同;</li>
 *   <li><b>阻断开关</b>:默认关(现状)——归属缺失返空、只告警不阻断;开关打开才 fail-loud 拒绝结算;</li>
 *   <li><b>归各商户自己</b>({@code per_merchant}):合法且不等于「未配置」,开关打开也不阻断;</li>
 *   <li><b>九类收入流</b>:新增的门票综合服务费 / 通票赠送权益预估成本 / 向商户供货的货款可落库;</li>
 *   <li><b>六层主体骨架</b>:稳定 id + 父链正确,工商信息一律留空(不得由代码编造);</li>
 *   <li><b>B-7 账本能落主体</b>:{@code settlement_transactions} 可落 {@code beneficiary_type='entity'} 行,
 *       缺主体 id 被 DB 拒;冲正读投影原样带回主体 id(否则冲正行会撞 CHECK 落不下)。</li>
 * </ol>
 *
 * <p>非 @Transactional(settleMonth 内部真提交);@AfterEach 按项目清理;每例独立 projectId。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SettlementEntityAttributionS1Tests {

    private static final YearMonth MONTH = YearMonth.of(2015, 3); // 远离今日的确定月
    private static final String ENT_PLATFORM = "be_ent_platform"; // V354 seed:娱乐平台公司

    @Autowired private TicketDistributionSettlementService settlementService;
    @Autowired private RevenueStreamAttributionRepository attributionRepository;
    @Autowired private JdbcSettlementTransactionRepository transactionRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private NamedParameterJdbcTemplate namedJdbcTemplate;

    private final List<String> projectIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (String pid : projectIds) {
            // V344 起 settlement_transactions 是 append-only 硬强制表(触发器拒 UPDATE/DELETE),清场须走测试专用旁路。
            com.complex.platform.support.AppendOnlyTestBypass.update(
                    jdbcTemplate, "DELETE FROM settlement_transactions WHERE project_id = ?", pid);
            jdbcTemplate.update(
                    "DELETE FROM settlement_items WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)",
                    pid);
            jdbcTemplate.update("DELETE FROM settlement_bills WHERE project_id = ?", pid);
            jdbcTemplate.update("DELETE FROM ticket_distribution_rank_tiers WHERE project_id = ?", pid);
            jdbcTemplate.update("DELETE FROM platform_retention_rules WHERE project_id = ?", pid);
            jdbcTemplate.update("DELETE FROM revenue_stream_attribution WHERE project_id = ?", pid);
        }
        projectIds.clear();
    }

    // ---------------------------------------------------------------- ① K 恒等式

    /**
     * K:归属配置为空 vs 配上归属 —— 结算单<b>除收款方标注外每一列逐分不变</b>,明细逐条不变。
     * 这是本刀「不动一分钱」的正面证明,也是 S2/S6 敢往下改的地基。
     */
    @Test
    void billAmountsAreIdenticalWhetherAttributionIsConfiguredOrNot() {
        String pid = newProject();
        setRetentionBps(pid, 2000);
        setRankTier(pid, 1, 9, 3333);
        seedVerified(pid, "bar_k1", 3, 2, 40001L);
        seedVerified(pid, "bar_k2", 2, 1, 30007L);
        seedVerified(pid, "bar_k3", 1, 1, 29991L);

        // (1) 归属未配置 —— 现状口径
        assertThat(attributionRepository.resolveManagementEntity(pid, "all_pass_ticket")).isNull();
        settlementService.settleMonth(pid, MONTH);
        Map<String, Object> before = bill(pid);
        Map<String, Long> itemsBefore = itemAmounts(pid);
        assertThat(before.get("attributed_entity_id")).isNull();
        assertThat(itemsBefore).isNotEmpty();
        assertThat(itemsBefore.values().stream().mapToLong(Long::longValue).sum()).isPositive();

        // (2) 配上归属(通票收入 → 娱乐平台公司)后重结同一月
        attributionRepository.upsert(new RevenueStreamAttributionRecord(
                rsaId("a"), pid, "all_pass_ticket", ENT_PLATFORM, "active", "tester", Instant.now()));
        assertThat(attributionRepository.resolveManagementEntity(pid, "all_pass_ticket")).isEqualTo(ENT_PLATFORM);
        settlementService.settleMonth(pid, MONTH);
        Map<String, Object> after = bill(pid);
        Map<String, Long> itemsAfter = itemAmounts(pid);

        // 收款方标注变了(这正是本刀要做的),别的一律没变。
        assertThat(after.get("attributed_entity_id")).isEqualTo(ENT_PLATFORM);
        Map<String, Object> b = new HashMap<>(before);
        Map<String, Object> a = new HashMap<>(after);
        for (String volatileCol : List.of("attributed_entity_id", "updated_at")) {
            b.remove(volatileCol);
            a.remove(volatileCol);
        }
        assertThat(a)
                .as("配归属只改收款方标注,结算单其余每一列(含每一项金额)必须逐分不变")
                .isEqualTo(b);
        assertThat(itemsAfter)
                .as("各商户分账明细必须逐条逐分不变")
                .isEqualTo(itemsBefore);
    }

    // ---------------------------------------------------------------- ② 阻断开关

    @Test
    void missingAttributionReturnsNullAndDoesNotBlockWhenSwitchIsOff() {
        String pid = newProject();
        // 默认开关关(application.yml 不配该键)→ 现状行为:返空、只告警、不阻断。
        assertThat(attributionRepository.resolveAttribution(pid, "pass_gift_cost")).isNull();
        assertThat(attributionRepository.resolveManagementEntity(pid, "ticket_service_fee")).isNull();
        // 归属为空也能正常结算(引擎不因此崩)
        seedVerified(pid, "bar_off", 1, 1, 1000L);
        assertThat(settlementService.settleMonth(pid, MONTH)).isNotBlank();
    }

    @Test
    void missingAttributionIsRejectedWhenSwitchIsOn() {
        String pid = newProject();
        RevenueStreamAttributionRepository blocking =
                new RevenueStreamAttributionRepository(namedJdbcTemplate, true);

        assertThatThrownBy(() -> blocking.resolveAttribution(pid, "ticket_service_fee"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("收入归属未配置")
                .hasMessageContaining("ticket_service_fee");

        // 配上之后同一个开关打开的解析器就不再阻断
        attributionRepository.upsert(new RevenueStreamAttributionRecord(
                rsaId("b"), pid, "ticket_service_fee", ENT_PLATFORM, "active", "tester", Instant.now()));
        assertThat(blocking.resolveManagementEntity(pid, "ticket_service_fee")).isEqualTo(ENT_PLATFORM);
    }

    // ---------------------------------------------------------------- ③ 归各商户自己

    @Test
    void perMerchantAttributionIsConfiguredNotMissing() {
        String pid = newProject();
        // 订台低消 / 二次消费 / 额外酒水按业务口径归各酒吧商户自己,没有单一收款主体。
        attributionRepository.upsert(new RevenueStreamAttributionRecord(
                rsaId("c"), pid, "table_dining", null, "active", "tester", Instant.now(), "per_merchant"));

        var hit = attributionRepository.resolveAttribution(pid, "table_dining");
        assertThat(hit).isNotNull();
        assertThat(hit.beneficiaryKind()).isEqualTo("per_merchant");
        assertThat(hit.managementEntityId()).isNull();
        // 主体 id 为空但「已配置」——开关打开也不阻断(否则会把合法口径当成配置缺口拦下)。
        RevenueStreamAttributionRepository blocking =
                new RevenueStreamAttributionRepository(namedJdbcTemplate, true);
        assertThat(blocking.resolveManagementEntity(pid, "table_dining")).isNull();

        // 归各商户自己却又指定主体 = 自相矛盾,写入即拒。
        assertThatThrownBy(() -> attributionRepository.upsert(new RevenueStreamAttributionRecord(
                rsaId("d"), pid, "bar_service_fee", ENT_PLATFORM, "active", "tester", Instant.now(),
                "per_merchant")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("不得再指定单一主体");
        // 归主体却不给主体,同样拒。
        assertThatThrownBy(() -> attributionRepository.upsert(new RevenueStreamAttributionRecord(
                rsaId("e"), pid, "bar_service_fee", null, "active", "tester", Instant.now(), "entity")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("必须指定主体");
    }

    // ---------------------------------------------------------------- ④ 九类收入流

    @Test
    void threeNewRevenueStreamsAreAccepted() {
        String pid = newProject();
        assertThat(RevenueStreamAttributionRepository.REVENUE_STREAMS).hasSize(9);
        // 门票综合服务费 / 通票赠送权益预估成本 → 娱乐平台公司;向商户供货的货款 → 中央餐饮公司。
        for (Map.Entry<String, String> e : Map.of(
                "ticket_service_fee", ENT_PLATFORM,
                "pass_gift_cost", ENT_PLATFORM,
                "supply_goods", "be_central_catering").entrySet()) {
            attributionRepository.upsert(new RevenueStreamAttributionRecord(
                    rsaId("f" + e.getKey().charAt(0)), pid, e.getKey(), e.getValue(),
                    "active", "tester", Instant.now()));
            assertThat(attributionRepository.resolveManagementEntity(pid, e.getKey())).isEqualTo(e.getValue());
        }
        assertThatThrownBy(() -> attributionRepository.upsert(new RevenueStreamAttributionRecord(
                rsaId("g"), pid, "not_a_stream", ENT_PLATFORM, "active", "tester", Instant.now())))
                .isInstanceOf(ApiException.class);
    }

    // ---------------------------------------------------------------- ⑤ 六层主体骨架

    @Test
    void sixEntitySkeletonIsSeededWithParentChainAndNoFabricatedRegistryData() {
        Map<String, String> expectedRole = Map.of(
                "be_platform", "platform",
                "be_proj_changsha", "project_company",
                "be_ent_platform", "management_company",
                "be_central_catering", "management_company",
                "be_amusement", "management_company",
                "be_media", "management_company");
        Map<String, String> expectedParent = Map.of(
                "be_proj_changsha", "be_platform",
                "be_ent_platform", "be_proj_changsha",
                "be_central_catering", "be_proj_changsha",
                "be_amusement", "be_proj_changsha",
                "be_media", "be_proj_changsha");

        for (Map.Entry<String, String> e : expectedRole.entrySet()) {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT * FROM business_entities WHERE id = ?", e.getKey());
            assertThat(row.get("entity_role")).as("%s 的主体角色", e.getKey()).isEqualTo(e.getValue());
            assertThat(row.get("status")).isEqualTo("active");
            assertThat(row.get("parent_entity_id")).isEqualTo(expectedParent.get(e.getKey()));
            // 工商信息不得由代码编造,一律留空由运营在后台补。
            assertThat(row.get("unified_social_credit_code")).as("%s 不得预置统一社会信用代码", e.getKey()).isNull();
            assertThat(row.get("legal_representative")).isNull();
            assertThat(row.get("bank_account_ref")).isNull();
            // 软删留痕列已就位(V354)
            assertThat(row).containsKey("archive_reason");
        }
        // 资产公司不单设主体:场地租金直接归 L2 长沙项目公司(governance「资产业态不单设主体」)。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM business_entities WHERE entity_name = '资产公司'", Integer.class)).isZero();
        // entity_role 白名单已生效
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at)
                VALUES ('be_s1_bad', '角色打错的主体', 'managment_company', 'active', now(), now())
                """))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------------------------------------------------------------- ⑥ B-7 账本能落主体

    @Test
    void ledgerAcceptsEntityBeneficiaryAndRejectsMissingEntityId() {
        String pid = newProject();
        // 归主体的一行(未核销权益款归娱乐平台公司即走这条路)
        insertLedger(pid, "st_s1_ent", "entity", null, ENT_PLATFORM);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT beneficiary_entity_id FROM settlement_transactions WHERE id = 'st_s1_ent'", String.class))
                .isEqualTo(ENT_PLATFORM);

        // beneficiary_type='entity' 却不给主体 id → DB CHECK 直接拒(不许留白)
        assertThatThrownBy(() -> insertLedger(pid, "st_s1_ent_bad", "entity", null, null))
                .isInstanceOf(DataIntegrityViolationException.class);

        // 老口径两类照旧可落(零行为变更)
        insertLedger(pid, "st_s1_merchant", "merchant", "bar_b7", null);
        insertLedger(pid, "st_s1_project", "project", null, null);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM settlement_transactions WHERE project_id = ?", Integer.class, pid))
                .isEqualTo(3);
    }

    @Test
    void reversalProjectionCarriesBeneficiaryEntitySoReversalRowCanLand() {
        String pid = newProject();
        String verificationId = "vf_s1_" + System.nanoTime();
        insertLedgerWithVerification(pid, "st_s1_rev", "entity", ENT_PLATFORM, verificationId);

        var fact = transactionRepository.findVerificationSettledByVerificationId(verificationId);
        assertThat(fact).isNotNull();
        assertThat(fact.beneficiaryType()).isEqualTo("entity");
        assertThat(fact.beneficiaryEntityId())
                .as("冲正读投影必须带回主体 id,否则冲正行会撞 chk_st_beneficiary 落不下、正向永不冲销")
                .isEqualTo(ENT_PLATFORM);
    }

    // ---------------------------------------------------------------- helpers

    /** revenue_stream_attribution.id 是 VARCHAR(48),不能把整个项目 id 拼进去。 */
    private String rsaId(Object tag) {
        return "rsa_s1_" + tag + "_" + (System.nanoTime() % 1_000_000_000L);
    }

    private String newProject() {
        String pid = "proj_s1_" + System.nanoTime();
        projectIds.add(pid);
        return pid;
    }

    private void seedVerified(String projectId, String merchantId, int verifiedCount, int numPeople,
                              long verifiedCent) {
        Instant createdAt = MONTH.atDay(15).atTime(12, 0).toInstant(ZoneOffset.UTC);
        java.sql.Timestamp ts = java.sql.Timestamp.from(createdAt);
        String id = "st_s1v_" + System.nanoTime();
        jdbcTemplate.update("""
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    entitlement_type, verified_count, total_times, num_people, allocated_revenue_cent,
                    verified_amount_cent, unverified_amount_cent, beneficiary_type, merchant_id, project_id,
                    verification_id, settlement_status, entitlement_valid_to, created_at, updated_at)
                VALUES (?, ?, 'verification_settled', ?, 'entry', ?, ?, ?, ?, ?, 0, 'merchant', ?, ?, ?,
                    'draft', ?, ?, ?)
                """,
                id, "TN-" + id, "ent_" + id, verifiedCount,
                Math.max(verifiedCount, 1), numPeople, verifiedCent, verifiedCent,
                merchantId, projectId, "vf_" + id, ts, ts, ts);
    }

    private void insertLedger(String projectId, String id, String beneficiaryType, String merchantId,
                              String entityId) {
        insertLedgerRow(projectId, id, beneficiaryType, merchantId, entityId, null);
    }

    private void insertLedgerWithVerification(String projectId, String id, String beneficiaryType,
                                              String entityId, String verificationId) {
        insertLedgerRow(projectId, id, beneficiaryType, null, entityId, verificationId);
    }

    private void insertLedgerRow(String projectId, String id, String beneficiaryType, String merchantId,
                                 String entityId, String verificationId) {
        Instant at = MONTH.atDay(10).atTime(12, 0).toInstant(ZoneOffset.UTC);
        java.sql.Timestamp ts = java.sql.Timestamp.from(at);
        jdbcTemplate.update("""
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    entitlement_type, verified_count, total_times, num_people, allocated_revenue_cent,
                    verified_amount_cent, unverified_amount_cent, beneficiary_type, merchant_id,
                    beneficiary_entity_id, project_id, verification_id, settlement_status,
                    entitlement_valid_to, business_at, created_at, updated_at)
                VALUES (?, ?, 'verification_settled', ?, 'entry', 1, 1, 1, 100, 100, 0, ?, ?, ?, ?, ?,
                    'draft', ?, ?, ?, ?)
                """,
                id, verificationId == null ? "TN-" + id : "ST-VS-" + verificationId, "ent_" + id, beneficiaryType, merchantId, entityId, projectId,
                verificationId == null ? "vf_" + id : verificationId, ts, ts, ts, ts);
    }

    private void setRetentionBps(String projectId, int bps) {
        jdbcTemplate.update("""
                INSERT INTO platform_retention_rules (id, project_id, retention_bps, status, is_draft)
                VALUES (?, ?, ?, 'active', FALSE)
                """, "prr_s1_" + projectId, projectId, bps);
    }

    private void setRankTier(String projectId, int rankFrom, int rankTo, int shareBps) {
        jdbcTemplate.update("""
                INSERT INTO ticket_distribution_rank_tiers (id, project_id, rank_from, rank_to, share_bps,
                    status, is_draft)
                VALUES (?, ?, ?, ?, ?, 'active', FALSE)
                """, "trt_s1_" + projectId + "_" + rankFrom, projectId, rankFrom, rankTo, shareBps);
    }

    private Map<String, Object> bill(String projectId) {
        return jdbcTemplate.queryForMap("SELECT * FROM settlement_bills WHERE project_id = ?", projectId);
    }

    private Map<String, Long> itemAmounts(String projectId) {
        Map<String, Long> out = new HashMap<>();
        jdbcTemplate.query("""
                SELECT si.merchant_id, si.distribution_method, si.calculated_amount_cent
                  FROM settlement_items si JOIN settlement_bills sb ON sb.id = si.bill_id
                 WHERE sb.project_id = ?
                """, rs -> {
            out.put(rs.getString("merchant_id") + "|" + rs.getString("distribution_method"),
                    rs.getLong("calculated_amount_cent"));
        }, projectId);
        return out;
    }
}
