package com.complex.platform.settlement;

import com.complex.platform.inventory.HighValueThresholdPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 全平台高价值判定阈值读取面（V350 归一；前身 V345 的 {@code JdbcHighValueThresholdRepository}）。
 *
 * <p>第十八批第 24 条（ENUM:164「全平台统一可配」）：只读<b>全平台那一行正式生效</b>行
 * （{@code status='active' AND is_draft=FALSE}、项目与商户为空），草案永不被引擎读到（在审期间保值）。
 *
 * <p><b>与其它配置 finder 的唯一不同：缺配置返回 null 而不是抛错。</b>
 * 业务口径（业务HTML 2026-07-28）是「阈值未配置一律按高价值处理、拦下走审批，不得静默放行」——
 * 判定侧收到 null 就把这单当高价值拦下，主管仍可逐单放行，运营补配置后自动恢复分流；
 * 若在这里 fail-loud 抛错，退酒/报损/存酒作废入口会整个不可用，反而比「拦下」更糟。
 * 注意这**不是**静默回落硬编码——回落 5000 元才是要根除的东西，返回 null 走的是从严那一侧。
 */
// ponytail: 单读取面仓储，无额外抽象；与 JdbcCommissionPointsConfigRepository 分开是因为 payload/生命周期各自独立。
@Repository
public class JdbcHighValueThresholdRepository implements HighValueThresholdPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcHighValueThresholdRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 高价值阈值（分）。出厂种子 {@code hvt_default}=500000（5000 元）。未配置返回 null。 */
    @Override
    public Long findHighValueThresholdCent() {
        return jdbcTemplate.query("""
                SELECT high_value_threshold_cent FROM high_value_thresholds
                 WHERE status = 'active' AND is_draft = FALSE
                   AND project_id IS NULL AND merchant_id IS NULL
                 LIMIT 1
                """, new MapSqlParameterSource(),
                rs -> rs.next() ? rs.getLong("high_value_threshold_cent") : null);
    }
}
