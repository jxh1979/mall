package com.complex.platform.ticketing;

// 文件版本：V0.01

/**
 * 第十八批 BP 段第 354 条 354-R2、第 300 条 300-R1（ENUM:32「酒吧通票由娱乐平台公司负责；纯游乐票和纯游乐通票由游乐公司负责；演出票由传媒公司负责」；
 * PGM:419「单一经营域的定价、售票、内部结算和分配规则由对应域主管公司负责」；BSM:1673「域内规则由对应域主管公司负责」）：票务域只问「这家商户归哪家域主管公司、
 * 这个人属于哪家公司、公司叫什么」，取法与内部结算价审批同一处（结算配置中心承接，同 {@link SingleBarServiceFeeConfigPort} 范式）。
 */
public interface TicketDomainCompanyPort {

    /**
     * 商户业态对应的域主管公司：餐饮与零售取该项目城市项目公司，「其他」取商户档案主管公司，游乐、演出、酒吧取收入归属。
     *
     * @throws com.complex.platform.common.api.ApiException 定不出时 409（CITY_PROJECT_COMPANY_MISSING 或 REVENUE_STREAM_ATTRIBUTION_MISSING）
     */
    String domainCompanyOfMerchant(String merchantId);

    /** 人员所属公司（在职已审员工档案公司，否则任职机构，否则账号机构）；定不出或已停用为空。 */
    String companyOfUser(String userId);

    /** 公司主体名称；查不到时返回编号本身。 */
    String companyName(String companyId);
}
