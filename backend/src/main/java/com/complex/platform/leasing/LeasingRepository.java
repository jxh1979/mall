package com.complex.platform.leasing;
// 文件版本：V0.03

import com.complex.platform.common.api.PageResponse;
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

    Optional<LeasingLeadRecord> findLeadByIdForUpdate(String id);

    LeasingIntentRecord saveIntent(LeasingIntentRecord intent);

    List<LeasingIntentRecord> listIntents(String projectScope);

    Optional<LeasingIntentRecord> findIntentById(String id);

    /** 第十八批第 30 条：按待确认复核单锁定意向（关键阶段主管确认结论回写用）。 */
    Optional<LeasingIntentRecord> findIntentByPendingReviewId(String reviewId);

    // ── CG-01 招商线索「已签约」改到合同生效时（PGM「招商运营细则」）：default 以不破坏测试替身。 ──
    /** 签约起草合同时把合同 id 绑到来源意向（leasing_intents.contract_id，V442），供合同生效后反查线索。 */
    default void bindIntentContract(String intentId, String contractId) { }

    /** 合同生效时反查：按签约起草时绑定的合同 id 找来源意向；无 = 该合同不是招商签约起草的。 */
    default Optional<LeasingIntentRecord> findIntentByContractId(String contractId) {
        return Optional.empty();
    }

    /**
     * CAS 置已签约：仅当线索当前不是 signed 时改为 signed；返回本次是否真的写入
     * （已签约 / 重放返回 false，调用方据此不重复留痕）。
     */
    default boolean markLeadSignedIfNotSigned(String leadId, java.time.Instant now) {
        return false;
    }

    LeasingBrandRecord saveBrand(LeasingBrandRecord brand);

    List<LeasingBrandRecord> listBrands(String projectScope);

    Optional<LeasingBrandRecord> findBrandById(String id);

    LeasingFollowUpRecord saveFollowUp(LeasingFollowUpRecord followUp);

    List<LeasingFollowUpRecord> listFollowUpsByRef(String refType, String refId, String projectScope);

    /** 招商工作台首屏全量跟进（无 ref 过滤，projectScope 语义同上）。 */
    List<LeasingFollowUpRecord> listAllFollowUps(String projectScope);

    // AUD-005 真分页: 默认基于全量列表内存切片(供 InMemory 测试双子); Jdbc 覆写为 SQL COUNT + LIMIT/OFFSET。
    default PageResponse<LeasingLeadRecord> listLeadsPaged(String projectScope, int page, int size) {
        return PageResponse.of(listLeads(projectScope), page, size);
    }

    default PageResponse<LeasingLeadRecord> listLeadsNeedingFollowUpPaged(
            java.time.Instant cutoff, String projectScope, int page, int size) {
        return PageResponse.of(listLeadsNeedingFollowUp(cutoff, projectScope), page, size);
    }

    default PageResponse<LeasingIntentRecord> listIntentsPaged(String projectScope, int page, int size) {
        return PageResponse.of(listIntents(projectScope), page, size);
    }

    default PageResponse<LeasingBrandRecord> listBrandsPaged(String projectScope, int page, int size) {
        return PageResponse.of(listBrands(projectScope), page, size);
    }

    default PageResponse<LeasingFollowUpRecord> listFollowUpsByRefPaged(
            String refType, String refId, String projectScope, int page, int size) {
        return PageResponse.of(listFollowUpsByRef(refType, refId, projectScope), page, size);
    }

    default PageResponse<LeasingFollowUpRecord> listAllFollowUpsPaged(String projectScope, int page, int size) {
        return PageResponse.of(listAllFollowUps(projectScope), page, size);
    }

    // 18E（第 19 条 19-R2）：可跨项目角色持有人按可触及项目集合取（空集合＝一行不出）。默认内存过滤（测试双子）；Jdbc 覆写为 project_id IN。
    default PageResponse<LeasingLeadRecord> listLeadsPaged(java.util.Collection<String> projectIds, int page, int size) {
        return PageResponse.of(listLeads(null).stream().filter(l -> inProjects(l.projectId(), projectIds)).toList(), page, size);
    }

    default PageResponse<LeasingLeadRecord> listLeadsNeedingFollowUpPaged(
            java.time.Instant cutoff, java.util.Collection<String> projectIds, int page, int size) {
        return PageResponse.of(listLeadsNeedingFollowUp(cutoff, null).stream()
                .filter(l -> inProjects(l.projectId(), projectIds)).toList(), page, size);
    }

    default PageResponse<LeasingIntentRecord> listIntentsPaged(java.util.Collection<String> projectIds, int page, int size) {
        return PageResponse.of(listIntents(null).stream().filter(i -> inProjects(i.projectId(), projectIds)).toList(), page, size);
    }

    default PageResponse<LeasingBrandRecord> listBrandsPaged(java.util.Collection<String> projectIds, int page, int size) {
        return PageResponse.of(listBrands(null).stream().filter(b -> inProjects(b.projectId(), projectIds)).toList(), page, size);
    }

    default PageResponse<LeasingFollowUpRecord> listFollowUpsByRefPaged(
            String refType, String refId, java.util.Collection<String> projectIds, int page, int size) {
        return PageResponse.of(listFollowUpsByRef(refType, refId, null).stream()
                .filter(f -> inProjects(f.projectId(), projectIds)).toList(), page, size);
    }

    default PageResponse<LeasingFollowUpRecord> listAllFollowUpsPaged(java.util.Collection<String> projectIds, int page, int size) {
        return PageResponse.of(listAllFollowUps(null).stream().filter(f -> inProjects(f.projectId(), projectIds)).toList(), page, size);
    }

    private static boolean inProjects(String projectId, java.util.Collection<String> projectIds) {
        return projectId != null && projectIds.contains(projectId);
    }
}
