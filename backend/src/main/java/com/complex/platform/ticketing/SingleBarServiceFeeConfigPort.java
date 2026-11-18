package com.complex.platform.ticketing;

// 文件版本：V0.01

import java.util.List;

/**
 * 第十八批 AW 段 294-R1（BSM:755「单一酒吧门票的综合服务费按物理项目分别维护，初始为空、不自动继承酒吧通票的默认比例，票种发布前必须先选定按比例还是
 * 按固定金额并填入数值，未选定或未填写的票种不得发布上架」）：票务域只问「这些项目里，哪些还没给这家酒吧配好已转正的综合服务费」，
 * 配置与复核转正由结算配置中心承接（同 {@link ComboPriceToleranceConfigPort} 范式）。
 */
public interface SingleBarServiceFeeConfigPort {

    /**
     * @return projectIds 里没有「本项目、本酒吧（票种为空或为本票种）、已转正」综合服务费行的项目名称；项目默认行、全平台默认行不算
     */
    List<String> unconfiguredProjects(String barId, String ticketTypeId, List<String> projectIds);
}
