package com.complex.platform.gate;

import java.util.Optional;

/**
 * 准入闸触发判定的只读事实数据源（R5 T3 待确认 #4：trigger_mode 接真实数据）。
 *
 * <p>仅做只读查询，复用既有表（contracts / compliance_expiry_items；over_contract_scope /
 * over_space 第二阶段补 contracts.allowed_business_scope + contract_spaces，见 V080）。
 * 实现以 fail-open（查询异常 → 返回 false=未命中，宁放行不误伤）为口径，与 AdmissionGuard
 * 整体风格一致；具体放行/阻断口径见各方法说明与 {@link AdmissionGuard}。</p>
 */
public interface AdmissionFactRepository {

    /**
     * 给定商户是否“无生效合同”（合同过期或不在生效状态）。
     *
     * <p>判定：该商户存在至少一份 lifecycle_status='approved' 且 effective_date &lt;= today &lt;= expiry_date
     * 的合同 → 视为有生效合同（返回 false=未过期）；否则视为过期/无生效合同（返回 true=命中 expired）。</p>
     *
     * @param merchantId 商户 id；为空返回 false（无商户上下文，不误伤）
     * @return true=过期/无生效合同（命中）；false=有生效合同或数据不可判定（放行）
     */
    boolean isContractExpiredForMerchant(String merchantId);

    /**
     * 给定商户的资质是否异常（合规到期项已过期或非生效）。
     *
     * <p>判定：compliance_expiry_items 中 subject_type IN ('merchant','supplier')、该商户、status='active'
     * 的资质项里，存在 valid_to &lt; today 的过期项 → 资质异常（命中）。</p>
     *
     * @param merchantId 商户 id；为空返回 false（无商户上下文，不误伤）
     * @return true=存在过期资质（命中）；false=资质正常或数据不可判定（放行）
     */
    boolean isQualificationAbnormalForMerchant(String merchantId);

    /**
     * 给定商户的生效合同经营范围是否“不含”指定业务域（over_contract_scope 命中判定）。
     *
     * <p>判定（数据源 contracts，V010 + V080 allowed_business_scope）：</p>
     * <ul>
     *   <li>该商户存在至少一份 lifecycle_status='approved' 且日期覆盖今天的生效合同，其
     *       allowed_business_scope 含 businessDomain → 放行（返回 false=未命中）。</li>
     *   <li>该商户存在至少一份生效合同其 allowed_business_scope 为空数组 [] → 视为“未配置=不限”→
     *       放行（返回 false），保守不误伤。</li>
     *   <li>该商户有生效合同但无任一含 businessDomain 也无空范围 → 经营范围越界 → 命中（返回 true）。</li>
     *   <li>该商户无生效合同 → 命中（返回 true），与 expired 语义一致。</li>
     * </ul>
     *
     * @param merchantId     商户 id；为空返回 false（无商户上下文，不误伤）
     * @param businessDomain 业务域（actionType 第一段，如 bar/amusement/ticketing/finance）；为空返回 false
     * @return true=经营范围越界（命中）；false=范围内/未配置不限/数据不可判定（放行）
     */
    boolean isOverContractScopeForMerchant(String merchantId, String businessDomain);

    /**
     * 给定空间是否“不在”该商户生效合同的租赁空间内（over_space 命中判定）。
     *
     * <p>判定（数据源 contracts + contract_spaces，V080）：该商户存在至少一份生效合同
     * （approved + 日期覆盖今天）经 contract_spaces 关联到 spaceId → 在租赁范围内（返回 false=放行）；
     * 否则越界（返回 true=命中）。</p>
     *
     * @param merchantId 商户 id；为空返回 false（无商户上下文，不误伤）
     * @param spaceId    已解析出的空间 id；为空返回 false（无法判定，放行）
     * @return true=空间不在生效合同租赁内（命中）；false=在租赁内/数据不可判定（放行）
     */
    boolean isOverSpaceForMerchant(String merchantId, String spaceId);

    /**
     * 按 objectType/objectId 解析对象所属空间 id（over_space 用）。
     *
     * <p>已支持解析的受控对象类型（V084 扩大覆盖面）：</p>
     * <ul>
     *   <li>{@code merchant_service_work_order} → merchant_service_work_orders.space_id（V022 直连）。</li>
     *   <li>{@code bar_bill}（objectId=bill_id）→ bar_bills.table_id → bar_tables.space_id（V084 多级）。</li>
     *   <li>{@code inspection} → amusement_safety_inspections.project_id → amusement_projects.space_id（V084）。</li>
     *   <li>{@code amusement_maintenance_record} → amusement_maintenance_record.project_id → amusement_projects.space_id（V084）。</li>
     *   <li>{@code amusement_incident} → amusement_incidents.project_id → amusement_projects.space_id（V084）。</li>
     *   <li>{@code amusement_group_booking} → amusement_group_bookings.project_id → amusement_projects.space_id（V084）。</li>
     * </ul>
     * <p>仍未支持（无可用 对象→空间 映射或无 objectId）→ 返回 empty（无法解析 → over_space 放行）：
     * {@code amusement_manual_override}（写接口首参非 String，objectId=null）、ticketing（verification /
     * ticket_refund / ticket_dynamic_pricing_rule）、finance（ar_ap_item / finance_refund / ...）、
     * inventory（saved_bottle / inventory_stock_adjustment）等无空间归属或非空间受控的对象。</p>
     *
     * @param objectType 受控对象类型（GuardedAction.objectType）
     * @param objectId   受控对象 id
     * @return 解析出的 space id；无法解析/数据不可得 → empty（放行）
     */
    Optional<String> resolveSpaceId(String objectType, String objectId);
}
