package com.complex.platform.contract;

/**
 * CG-09 铺位租赁状态推导共用 SQL 片段（PGM「合同运营细则」：「铺位租赁状态随意向与合同自动联动并在平面图着色（洽谈中、已租、即将到期等），
 * 退租结算完成即转空置，不可租人工标注」；BSM「铺位看板」/ ENUM「铺位租赁状态」：「空置、洽谈中、已租、即将到期、已退租、不可租」）。
 * 铺位看板（{@code JdbcContractRepository.listSpaceRentalRaw}）与经营看板出租率/空置明细
 * （{@code JdbcAnalyticsRepository.occupancyAreas} / {@code listVacantSpaces}）共用同一片段，保证同一口径（BSM「出租」）。
 *
 * <p>推导顺序：不可租（人工 {@code spaces.status=unavailable}）&gt; 已退租 &gt; 已租/即将到期 &gt; 洽谈中 &gt; 空置。
 * 以该铺「最近一份已进入租期的合同」为准（{@code contract_spaces} 绑定、排除起草/待确认/待生效及生效日在 as_of 之后的合同，
 * 按 effective_date、created_at 取最新一份）：
 * <ul>
 *   <li>已退租：该合同存在未结清的退租结算单（{@code contract_settlements.status ∉ settled/voided}，含 move_out / expiry /
 *       early_termination 三种触发，不论合同是否仍 effective），或该合同已不在生效租期内（expired / terminated / 过期未扫）而没有已结清的结算单；</li>
 *   <li>已租：该合同 effective 且 as_of 未过 expiry_date 且无未结清结算单（即将到期由 service 按着色分类再判）；</li>
 *   <li>洽谈中：铺位有招商意向；</li>
 *   <li>空置：结算单已结清（settled）或从未绑约；只有已签约未生效合同的铺位不算已退租。</li>
 * </ul>
 */
public final class SpaceRentalDerivationSql {

    private SpaceRentalDerivationSql() {
    }

    /**
     * LATERAL 子查询体：铺位别名 s 的最近一份已进入租期的合同及其退租结算单状态；参数 :as_of。
     * 用法：{@code LEFT JOIN LATERAL (<本片段>) l ON true}（不用 CTE：租户隔离扫描把 CTE 名当作未登记的表）。
     */
    public static final String LATEST_CONTRACT_LATERAL = """
            SELECT c.id AS contract_id, c.lifecycle_status, c.effective_date, c.expiry_date,
                   EXISTS (SELECT 1 FROM contract_settlements st
                            WHERE st.contract_id = c.id AND st.status NOT IN ('settled', 'voided')) AS settlement_open,
                   EXISTS (SELECT 1 FROM contract_settlements st
                            WHERE st.contract_id = c.id AND st.status = 'settled') AS settlement_settled
              FROM contract_spaces cs JOIN contracts c ON c.id = cs.contract_id
             WHERE cs.space_id = s.id
               AND c.lifecycle_status NOT IN ('drafting', 'pending_confirmation', 'pending_effective')
               AND NOT (c.lifecycle_status = 'effective' AND c.effective_date > :as_of)
             ORDER BY c.effective_date DESC, c.created_at DESC, c.id DESC
             LIMIT 1""";

    /** 已租（含即将到期）：别名 l = 上述 LATERAL 行；参数 :as_of。无合同行时为 NULL（按 false 用）。 */
    public static final String IS_LET = """
            (l.lifecycle_status = 'effective' AND l.expiry_date >= :as_of AND NOT l.settlement_open)""";

    /** 已退租：别名 l；参数 :as_of。无合同行时为 false。 */
    public static final String IS_MOVED_OUT = """
            (l.contract_id IS NOT NULL AND (l.settlement_open
                OR (NOT l.settlement_settled AND NOT (l.lifecycle_status = 'effective' AND l.expiry_date >= :as_of))))""";
}
