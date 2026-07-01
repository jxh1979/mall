package com.complex.platform.leasing;

import java.util.List;
import java.util.Optional;

/** 招商仓储（leasing_leads / leasing_intents / leasing_brands / leasing_follow_ups，V161 重建）。 */
public interface LeasingRepository {

    LeasingLeadRecord saveLead(LeasingLeadRecord lead);

    List<LeasingLeadRecord> listLeads();

    /**
     * 3.6 三天未跟进提醒下沉仓储：直接查 due 线索（status 非 signed/lost 且
     * COALESCE(last_follow_up_at, created_at) < cutoff），避免拉全表内存过滤。
     * 默认实现内存过滤（供 InMemory 测试双子），Jdbc 覆盖为 SQL WHERE。
     */
    default List<LeasingLeadRecord> listLeadsNeedingFollowUp(java.time.Instant cutoff) {
        return listLeads().stream()
                .filter(l -> !"signed".equals(l.status()) && !"lost".equals(l.status()))
                .filter(l -> {
                    java.time.Instant ref = l.lastFollowUpAt() != null ? l.lastFollowUpAt() : l.createdAt();
                    return ref != null && ref.isBefore(cutoff);
                })
                .toList();
    }

    Optional<LeasingLeadRecord> findLeadById(String id);

    LeasingIntentRecord saveIntent(LeasingIntentRecord intent);

    List<LeasingIntentRecord> listIntents();

    Optional<LeasingIntentRecord> findIntentById(String id);

    LeasingBrandRecord saveBrand(LeasingBrandRecord brand);

    List<LeasingBrandRecord> listBrands();

    Optional<LeasingBrandRecord> findBrandById(String id);

    LeasingFollowUpRecord saveFollowUp(LeasingFollowUpRecord followUp);

    List<LeasingFollowUpRecord> listFollowUpsByRef(String refType, String refId);
}
