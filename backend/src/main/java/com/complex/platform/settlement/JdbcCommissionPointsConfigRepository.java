package com.complex.platform.settlement;

import com.complex.platform.bar.BarCommissionRateConfigPort;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.ticketing.PointsAccrualRateConfigPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * V340 提成/积分费率配置读取面(P0-D/CFG-01):消灭 {@code BarBillingService} 的 15%/10% 与
 * {@code TicketOrderService} 的「每元 1 分」两处硬编码。
 *
 * <p>范式与 {@link JdbcSettlementBillRepository} 各 finder 逐条对齐:
 * ① 只读<b>正式生效</b>行({@code status='active' AND is_draft=FALSE}),草案永不被引擎读到(编辑期保值);
 * ② 作用域「越具体越优先」——先项目公司、再商户,用 {@code ORDER BY (col IS NOT NULL) DESC} 排序取第一行;
 * ③ 缺配置<b>fail-loud</b>({@link #requireConfigured}),不静默套硬编码默认——这正是本批要根除的东西。
 *
 * <p><b>租户隔离(AC-ISO-1,§4)</b>:两个 finder 的项目/商户维度均用
 * {@code (<scope_col> IS NULL OR <scope_col> = :<scope_col>)} 谓词下沉到 SQL ——
 * 「本作用域的特例行」或「全局默认行(列为 NULL)」二者之一,<b>永不可能读到别的项目/别的商户的特例行</b>。
 * (2026-07-28 修 verify:tenant-isolation F1:原写法为 {@code COALESCE(project_id,'__DEFAULT__') IN (...)}
 * 哨兵比对,语义等价但 ① 机审识别不出 scoped 谓词,② 依赖写面 rejectSentinel 保证「没有真项目 id 叫
 * __DEFAULT__」——一旦哨兵与真实 project_id 撞名,哨兵写法会让该行对<b>所有</b>调用方可见=跨租户读。
 * 现写法不依赖任何哨兵约定,且与本类 merchant_id 谓词同形。)
 *
 * <p>实现 bar / ticketing 两侧声明的端口(依赖倒置):两域不感知配置表,配置中心单点承接作用域与草案闸。
 */
// ponytail: 单读取面仓储,无额外抽象;两张表 payload 各异故各写一条显式 SQL(与 JdbcSettlementBillRepository 一致)。
@Repository
public class JdbcCommissionPointsConfigRepository
        implements BarCommissionRateConfigPort, PointsAccrualRateConfigPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcCommissionPointsConfigRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 卡座提成率(万分比)。种子 __DEFAULT__:sales=1500 / non_sales=1000(= 改造前写死值,零行为改变)。
     * roleType 由调用方给定枚举(sales/non_sales),非用户输入;非法值查不到行 → fail-loud。
     */
    @Override
    public int findCommissionRateBp(String projectId, String merchantId, String roleType) {
        Integer bp = jdbcTemplate.query("""
                SELECT rate_bp FROM bar_commission_rate_configs
                 WHERE status = 'active' AND is_draft = FALSE
                   AND role_type = :role_type
                   AND (project_id IS NULL OR project_id = :project_id)
                   AND (merchant_id IS NULL OR merchant_id = :merchant_id)
                 ORDER BY (project_id IS NOT NULL) DESC,
                          (merchant_id IS NOT NULL) DESC
                 LIMIT 1
                """, new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("merchant_id", merchantId)
                .addValue("role_type", roleType),
                rs -> rs.next() ? rs.getInt("rate_bp") : null);
        return requireConfigured(bp, "bar_commission_rate_configs.rate_bp / role_type=" + roleType, projectId);
    }

    /**
     * 积分累计率:每积 1 基础分所需消费金额(分)。种子 __DEFAULT__=100(= 改造前 POINTS_PER_YUAN_CENT,零行为改变)。
     */
    @Override
    public long findAmountCentPerPoint(String projectId, String merchantId) {
        Long unit = jdbcTemplate.query("""
                SELECT amount_cent_per_point FROM points_accrual_rate_configs
                 WHERE status = 'active' AND is_draft = FALSE
                   AND (project_id IS NULL OR project_id = :project_id)
                   AND (merchant_id IS NULL OR merchant_id = :merchant_id)
                 ORDER BY (project_id IS NOT NULL) DESC,
                          (merchant_id IS NOT NULL) DESC
                 LIMIT 1
                """, new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("merchant_id", merchantId),
                rs -> rs.next() ? rs.getLong("amount_cent_per_point") : null);
        return requireConfigured(unit, "points_accrual_rate_configs.amount_cent_per_point", projectId);
    }

    /**
     * fail-loud:配置缺失(连 V340 的 __DEFAULT__ 种子都没有 = 种子被删/迁移未跑到位)时响亮停,
     * 而非静默回落硬编码常量——回落常量正是本批(P0-D)要根除的东西。正常态两表恒有生效种子,本助手不触发。
     */
    private <T> T requireConfigured(T value, String key, String projectId) {
        if (value == null) {
            throw ApiException.badRequest("SETTLEMENT_CONFIG_MISSING",
                    "结算配置缺失且无默认种子:" + key + (projectId == null ? "" : " / project=" + projectId)
                            + ";请先在结算配置中心配置并转正");
        }
        return value;
    }
}
