package com.complex.platform.gate;

/**
 * 准入闸触发判定的只读事实数据源（R5 T3 待确认 #4：trigger_mode 接真实数据）。
 *
 * <p>仅做只读查询，复用既有表（contracts / compliance_expiry_items），不新增迁移。
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
}
