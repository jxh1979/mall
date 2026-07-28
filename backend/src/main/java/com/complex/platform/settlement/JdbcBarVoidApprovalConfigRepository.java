package com.complex.platform.settlement;

import com.complex.platform.bar.BarVoidApprovalConfigPort;
import com.complex.platform.common.api.ApiException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * V345 高价值退酒审批阈值读取面（P0-C/FLOW-07）：消灭 BAR-VOID-A1「超过 5000 元」这一口径的写死可能。
 *
 * <p>范式与 {@link JdbcCommissionPointsConfigRepository} 逐条对齐：
 * ① 只读<b>正式生效</b>行（{@code status='active' AND is_draft=FALSE}），草案永不被引擎读到（编辑期保值）；
 * ② 作用域「越具体越优先」——先项目公司、再商户，用 {@code ORDER BY (col IS NOT NULL) DESC} 排序取第一行；
 * ③ 缺配置 <b>fail-loud</b>，不静默套 5000 元硬编码——这正是本批要根除的东西。
 *
 * <p><b>租户隔离（AC-ISO-1）</b>：项目/商户维度均用 {@code (<scope_col> IS NULL OR <scope_col> = :<scope_col>)}
 * 谓词下沉到 SQL ——「本作用域的特例行」或「全局默认行（列为 NULL）」二者之一，
 * 永不可能读到别的项目/别的商户的特例行；不依赖任何 COALESCE 哨兵约定。
 */
// ponytail: 单读取面仓储，无额外抽象；与 JdbcCommissionPointsConfigRepository 分开是因为 payload/生命周期各自独立。
@Repository
public class JdbcBarVoidApprovalConfigRepository implements BarVoidApprovalConfigPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcBarVoidApprovalConfigRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 高价值退酒阈值（分）。种子 {@code bvac_default}=500000（= BAR-VOID-A1 默认 5000 元）。
     */
    @Override
    public long findVoidHighValueThresholdCent(String projectId, String merchantId) {
        Long threshold = jdbcTemplate.query("""
                SELECT high_value_threshold_cent FROM bar_void_approval_configs
                 WHERE status = 'active' AND is_draft = FALSE
                   AND (project_id IS NULL OR project_id = :project_id)
                   AND (merchant_id IS NULL OR merchant_id = :merchant_id)
                 ORDER BY (project_id IS NOT NULL) DESC,
                          (merchant_id IS NOT NULL) DESC
                 LIMIT 1
                """, new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("merchant_id", merchantId),
                rs -> rs.next() ? rs.getLong("high_value_threshold_cent") : null);
        if (threshold == null) {
            throw ApiException.badRequest("SETTLEMENT_CONFIG_MISSING",
                    "结算配置缺失且无默认种子：bar_void_approval_configs.high_value_threshold_cent"
                            + (projectId == null ? "" : " / project=" + projectId)
                            + "；请先在结算配置中心配置高价值退酒阈值并复核转正");
        }
        return threshold;
    }
}
