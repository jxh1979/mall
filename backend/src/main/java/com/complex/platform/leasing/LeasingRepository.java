package com.complex.platform.leasing;

import java.util.List;
import java.util.Optional;

/** 招商仓储（leasing_leads / leasing_intents / leasing_brands / leasing_follow_ups，V161 重建）。 */
public interface LeasingRepository {

    LeasingLeadRecord saveLead(LeasingLeadRecord lead);

    /**
     * 3.2 跨项目读隔离：projectScope 非空时仅返回该项目的行（project_id = scope），
     * null（平台管理员/全局平台员工）返回全量。以下各 list 同语义。
     */
    List<LeasingLeadRecord> listLeads(String projectScope);

    /**
     * 3.6 三天未跟进提醒下沉仓储：直接查 due 线索（status 非 signed/lost 且
     * COALESCE(last_follow_up_at, created_at) < cutoff），避免拉全表内存过滤。
     * 默认实现内存过滤（供 InMemory 测试双子），Jdbc 覆盖为 SQL WHERE。
     */
    default List<LeasingLeadRecord> listLeadsNeedingFollowUp(java.time.Instant cutoff, String projectScope) {
        return listLeads(projectScope).stream()
                .filter(l -> !"signed".equals(l.status()) && !"lost".equals(l.status()))
                .filter(l -> {
                    java.time.Instant ref = l.lastFollowUpAt() != null ? l.lastFollowUpAt() : l.createdAt();
                    return ref != null && ref.isBefore(cutoff);
                })
                .toList();
    }

    Optional<LeasingLeadRecord> findLeadById(String id);

    LeasingIntentRecord saveIntent(LeasingIntentRecord intent);

    List<LeasingIntentRecord> listIntents(String projectScope);

    Optional<LeasingIntentRecord> findIntentById(String id);

    LeasingBrandRecord saveBrand(LeasingBrandRecord brand);

    List<LeasingBrandRecord> listBrands(String projectScope);

    Optional<LeasingBrandRecord> findBrandById(String id);

    LeasingFollowUpRecord saveFollowUp(LeasingFollowUpRecord followUp);

    List<LeasingFollowUpRecord> listFollowUpsByRef(String refType, String refId, String projectScope);

    /** 招商工作台首屏全量跟进（无 ref 过滤，projectScope 语义同上）。 */
    List<LeasingFollowUpRecord> listAllFollowUps(String projectScope);
}
