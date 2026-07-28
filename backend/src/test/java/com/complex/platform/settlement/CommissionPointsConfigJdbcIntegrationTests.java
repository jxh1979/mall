package com.complex.platform.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.settlement.SettlementConfigWriteRequests.ArchiveRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.CommissionRateRequest;
import com.complex.platform.settlement.SettlementConfigWriteRequests.PointsAccrualRateRequest;
import com.complex.platform.support.TestSecurityActors;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * V340(P0-D/CFG-01)提成/积分费率配置验收:出厂种子 = 改造前写死值(零行为改变的 DB 侧证据)、
 * 作用域越具体越优先、草案不被引擎读到(Path 2 保值)、缺配置 fail-loud、商户特例可软删回落默认、默认种子禁软删。
 *
 * <p>⚠️ 走真实 Flyway 迁移库;{@code @Transactional} = 每用例结束自动回滚(含删表行的 fail-loud 用例不污染库)。
 * <p>差人转正:起草用 {@code DRAFTER}、复核转正用 {@code APPROVER}(二者不同,否则 403)。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class CommissionPointsConfigJdbcIntegrationTests {

    private static final String DRAFTER = "drafter_cpc";
    private static final String APPROVER = "approver_cpc";
    private static final String PROJECT = "proj_cpc_it";
    private static final String MERCHANT = "merch_cpc_it";

    @Autowired
    private SettlementConfigWriteService writeService;

    @Autowired
    private JdbcCommissionPointsConfigRepository configRepository;

    @Autowired
    private JdbcSettlementConfigWriteRepository writeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        TestSecurityActors.asPlatformAdmin();
        cleanupProjectRows();
    }

    @AfterEach
    void tearDown() {
        cleanupProjectRows();
        TestSecurityActors.clear();
    }

    private void cleanupProjectRows() {
        for (String table : List.of("bar_commission_rate_configs", "points_accrual_rate_configs")) {
            jdbcTemplate.update("DELETE FROM " + table + " WHERE project_id = ?", PROJECT);
        }
    }

    private String draftRowId(String table) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM " + table + " WHERE project_id = ? AND is_draft = TRUE AND status = 'active' LIMIT 1",
                String.class, PROJECT);
    }

    /**
     * 🔴 零行为改变的核心证据:V340 出厂种子逐值等于改造前代码里写死的常量
     * (BarBillingService 的 sales=1500/non_sales=1000、TicketOrderService 的 POINTS_PER_YUAN_CENT=100)。
     * 引擎改读配置后,在未做任何后台配置的默认部署上算出的数与改造前逐笔相等。
     */
    @Test
    void factorySeedsEqualThePreviousHardcodedValues() {
        assertThat(configRepository.findCommissionRateBp(null, null, "sales")).isEqualTo(1500);
        assertThat(configRepository.findCommissionRateBp(null, null, "non_sales")).isEqualTo(1000);
        assertThat(configRepository.findAmountCentPerPoint(null, null)).isEqualTo(100L);
        // 任意项目/商户在无特例时同样回落到全局默认种子(= 现值),不因新增维度而改数。
        assertThat(configRepository.findCommissionRateBp(PROJECT, MERCHANT, "sales")).isEqualTo(1500);
        assertThat(configRepository.findAmountCentPerPoint(PROJECT, MERCHANT)).isEqualTo(100L);
    }

    /** Path 2 保值:草案期引擎仍读旧生效值;差人复核转正后才切换。提成/积分两类各证一遍。 */
    @Test
    void draftsDoNotAffectEngineUntilActivated() {
        writeService.saveCommissionRate(
                new CommissionRateRequest("cr1", PROJECT, MERCHANT, "sales", 2500), DRAFTER);
        assertThat(configRepository.findCommissionRateBp(PROJECT, MERCHANT, "sales")).isEqualTo(1500); // 草案不被读
        writeService.activateDraft("commission_rate", draftRowId("bar_commission_rate_configs"), APPROVER);
        assertThat(configRepository.findCommissionRateBp(PROJECT, MERCHANT, "sales")).isEqualTo(2500);
        // 同项目的 non_sales 未配特例 → 仍全局默认(维度互不串)。
        assertThat(configRepository.findCommissionRateBp(PROJECT, MERCHANT, "non_sales")).isEqualTo(1000);

        writeService.savePointsAccrualRate(
                new PointsAccrualRateRequest("pa1", PROJECT, MERCHANT, 50L), DRAFTER);
        assertThat(configRepository.findAmountCentPerPoint(PROJECT, MERCHANT)).isEqualTo(100L); // 草案不被读
        writeService.activateDraft("points_accrual_rate", draftRowId("points_accrual_rate_configs"), APPROVER);
        assertThat(configRepository.findAmountCentPerPoint(PROJECT, MERCHANT)).isEqualTo(50L);
    }

    /** 作用域越具体越优先:项目通用 < 项目×商户;未命中特例的商户回落项目通用。 */
    @Test
    void moreSpecificScopeWins() {
        writeService.saveCommissionRate(
                new CommissionRateRequest("cr-proj", PROJECT, null, "sales", 1800), DRAFTER);
        writeService.activateDraft("commission_rate", draftRowId("bar_commission_rate_configs"), APPROVER);
        assertThat(configRepository.findCommissionRateBp(PROJECT, MERCHANT, "sales")).isEqualTo(1800);
        assertThat(configRepository.findCommissionRateBp(PROJECT, null, "sales")).isEqualTo(1800);

        writeService.saveCommissionRate(
                new CommissionRateRequest("cr-merch", PROJECT, MERCHANT, "sales", 2200), DRAFTER);
        writeService.activateDraft("commission_rate", draftRowId("bar_commission_rate_configs"), APPROVER);
        assertThat(configRepository.findCommissionRateBp(PROJECT, MERCHANT, "sales")).isEqualTo(2200); // 商户特例赢
        assertThat(configRepository.findCommissionRateBp(PROJECT, "merch_other", "sales")).isEqualTo(1800); // 他商户回落项目通用
        assertThat(configRepository.findCommissionRateBp(null, MERCHANT, "sales")).isEqualTo(1500); // 他项目回落全局
    }

    /**
     * 🔴 跨租户读隔离(AC-ISO-1,§4):A 项目的特例费率<b>绝不能</b>被 B 项目/无 scope 的调用方读到。
     * 谓词 {@code (project_id IS NULL OR project_id = :project_id)} 保证只可能命中「本项目特例」或「全局默认」。
     */
    @Test
    void projectScopedRowIsInvisibleToOtherProjects() {
        writeService.saveCommissionRate(
                new CommissionRateRequest("iso1", PROJECT, null, "sales", 4000), DRAFTER);
        writeService.activateDraft("commission_rate", draftRowId("bar_commission_rate_configs"), APPROVER);
        writeService.savePointsAccrualRate(
                new PointsAccrualRateRequest("iso2", PROJECT, null, 25L), DRAFTER);
        writeService.activateDraft("points_accrual_rate", draftRowId("points_accrual_rate_configs"), APPROVER);

        // 本项目读得到自己的特例
        assertThat(configRepository.findCommissionRateBp(PROJECT, null, "sales")).isEqualTo(4000);
        assertThat(configRepository.findAmountCentPerPoint(PROJECT, null)).isEqualTo(25L);
        // 他项目 / 无 scope(平台默认口径)一律读不到,回落全局默认
        assertThat(configRepository.findCommissionRateBp("proj_other_tenant", null, "sales")).isEqualTo(1500);
        assertThat(configRepository.findCommissionRateBp(null, null, "sales")).isEqualTo(1500);
        assertThat(configRepository.findAmountCentPerPoint("proj_other_tenant", null)).isEqualTo(100L);
        assertThat(configRepository.findAmountCentPerPoint(null, null)).isEqualTo(100L);
    }

    /**
     * 🔴 跨商户读隔离:A 商户的特例费率不能被同项目下 B 商户读到(商户维同样是 scoped 谓词而非哨兵比对)。
     */
    @Test
    void merchantScopedRowIsInvisibleToOtherMerchants() {
        writeService.saveCommissionRate(
                new CommissionRateRequest("iso3", PROJECT, MERCHANT, "sales", 4500), DRAFTER);
        writeService.activateDraft("commission_rate", draftRowId("bar_commission_rate_configs"), APPROVER);

        assertThat(configRepository.findCommissionRateBp(PROJECT, MERCHANT, "sales")).isEqualTo(4500);
        assertThat(configRepository.findCommissionRateBp(PROJECT, "merch_other_tenant", "sales")).isEqualTo(1500);
        assertThat(configRepository.findCommissionRateBp(PROJECT, null, "sales")).isEqualTo(1500);
    }

    /**
     * 🔴 哨兵撞名不再致越权(本次 F1 修复的实质收益):
     * 旧写法 {@code COALESCE(project_id,'__DEFAULT__') IN (COALESCE(:project_id,'__DEFAULT__'),'__DEFAULT__')}
     * 一旦库里存在 project_id 字面量 = '__DEFAULT__' 的行,该行会对<b>任意</b>调用方命中(跨租户读);
     * 现写法按真实相等比对,只有同名 scope 的调用方才读得到。写面另有 rejectSentinel 前置拦截(纵深防御)。
     *
     * <p>探针挂在<b>商户维</b>而非项目默认槽:后者与全局默认种子在 uq_bcrc 索引里
     * (COALESCE(project_id,'__DEFAULT__') 同为 '__DEFAULT__') 会撞唯一键插不进去——即 DB 索引本身
     * 已挡住「顶层默认槽」的哨兵撞名;商户维则不撞(索引元组不同),旧写法在这条路径上确会漏读。
     */
    @Test
    void sentinelNamedProjectRowDoesNotLeakToEveryCaller() {
        // 绕过写面 rejectSentinel 直插一行「project_id 字面量 = 哨兵」的脏数据,模拟历史/外部写入
        jdbcTemplate.update("""
                INSERT INTO bar_commission_rate_configs
                    (id, project_id, merchant_id, role_type, rate_bp, status, is_draft)
                VALUES ('bcrc_sentinel_probe', '__DEFAULT__', 'merch_sentinel_probe', 'sales', 9999, 'active', FALSE)
                """);
        try {
            // 任意他项目 / 无 scope 调用方即便命中同一商户,也不得读到这条 9999(旧哨兵写法会读到)
            assertThat(configRepository.findCommissionRateBp(null, "merch_sentinel_probe", "sales")).isEqualTo(1500);
            assertThat(configRepository.findCommissionRateBp(PROJECT, "merch_sentinel_probe", "sales")).isEqualTo(1500);
            // 只有 scope 恰为该字面量的调用方才读得到(此时它就是个普通项目 id,无任何通配特权)
            assertThat(configRepository.findCommissionRateBp("__DEFAULT__", "merch_sentinel_probe", "sales"))
                    .isEqualTo(9999);
        } finally {
            jdbcTemplate.update("DELETE FROM bar_commission_rate_configs WHERE id = 'bcrc_sentinel_probe'");
        }
    }

    /** 缺配置 fail-loud:连全局默认种子都没有(种子被删/迁移未跑到位)时响亮停,不静默套回硬编码。 */
    @Test
    void findersFailLoudWhenSeedMissing() {
        jdbcTemplate.update("DELETE FROM bar_commission_rate_configs");
        assertThatThrownBy(() -> configRepository.findCommissionRateBp(null, null, "sales"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("SETTLEMENT_CONFIG_MISSING"));

        jdbcTemplate.update("DELETE FROM points_accrual_rate_configs");
        assertThatThrownBy(() -> configRepository.findAmountCentPerPoint(null, null))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("SETTLEMENT_CONFIG_MISSING"));
    }

    /** 未知角色档查不到行 → 同样 fail-loud(不返回 0 悄悄少发提成)。 */
    @Test
    void unknownRoleTypeFailsLoud() {
        assertThatThrownBy(() -> configRepository.findCommissionRateBp(null, null, "manager"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("SETTLEMENT_CONFIG_MISSING"));
    }

    /** 商户级特例可软删(留档 + 回落默认口径);全局默认种子禁软删(否则全平台无默认可用)。 */
    @Test
    void merchantOverrideCanBeArchivedButDefaultSeedCannot() {
        writeService.saveCommissionRate(
                new CommissionRateRequest("cr-arch", PROJECT, MERCHANT, "sales", 3000), DRAFTER);
        String draftId = draftRowId("bar_commission_rate_configs");
        writeService.activateDraft("commission_rate", draftId, APPROVER);
        assertThat(configRepository.findCommissionRateBp(PROJECT, MERCHANT, "sales")).isEqualTo(3000);

        writeService.archive("commission_rate", draftId, new ArchiveRequest("arc1", "商户合同改签，特例作废"), DRAFTER);
        // 软删 = 留库 status→inactive + 原因,不物理删;引擎回落全局默认。
        assertThat(configRepository.findCommissionRateBp(PROJECT, MERCHANT, "sales")).isEqualTo(1500);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT archive_reason FROM bar_commission_rate_configs WHERE id = ?", String.class, draftId))
                .isEqualTo("商户合同改签，特例作废");

        assertThatThrownBy(() -> writeService.archive("commission_rate", "bcrc_default_sales",
                new ArchiveRequest("arc2", "试图下线默认"), DRAFTER))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("DEFAULT_SEED_NOT_ARCHIVABLE"));
        assertThat(configRepository.findCommissionRateBp(null, null, "sales")).isEqualTo(1500);
    }

    /** 写面校验:角色档枚举、费率区间、积分单位须 &gt;0。 */
    @Test
    void writeSideValidatesEnumAndRanges() {
        assertThatThrownBy(() -> writeService.saveCommissionRate(
                new CommissionRateRequest("bad1", PROJECT, null, "manager", 1000), DRAFTER))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> writeService.saveCommissionRate(
                new CommissionRateRequest("bad2", PROJECT, null, "sales", 10001), DRAFTER))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> writeService.savePointsAccrualRate(
                new PointsAccrualRateRequest("bad3", PROJECT, null, 0L), DRAFTER))
                .isInstanceOf(ApiException.class);
    }

    /** 两类草案进「待复核草案队列」,供总控页复核转正。 */
    @Test
    void draftsAppearInPendingQueue() {
        writeService.saveCommissionRate(
                new CommissionRateRequest("q1", PROJECT, MERCHANT, "non_sales", 900), DRAFTER);
        writeService.savePointsAccrualRate(
                new PointsAccrualRateRequest("q2", PROJECT, MERCHANT, 200L), DRAFTER);
        // 覆盖 listPendingConfigDrafts 新增的两条 UNION 分支(总控页草案队列的数据源)。
        assertThat(writeRepository.listPendingConfigDrafts())
                .extracting(JdbcSettlementConfigWriteRepository.PendingConfigDraft::configGroup)
                .contains("commission_rate", "points_accrual_rate");
        assertThat(writeRepository.listPendingConfigDrafts())
                .filteredOn(d -> "commission_rate".equals(d.configGroup()) && d.summary().contains(PROJECT))
                .singleElement()
                .satisfies(d -> assertThat(d.summary()).contains(MERCHANT).contains("non_sales").contains("900bps"));
    }
}
