package com.complex.platform.governance;
// 文件版本：V0.02

import java.util.List;
import java.util.Optional;

public interface PlatformRepository {

    BusinessEntityRecord saveBusinessEntity(BusinessEntityRecord businessEntity);

    List<BusinessEntityRecord> listBusinessEntities();

    MerchantRecord saveMerchant(MerchantRecord merchant);

    List<MerchantRecord> listMerchants();

    /** V260：按 id 查商户（存档/删除状态机读取用）。默认基于 listMerchants 过滤；Jdbc 覆写按主键。 */
    default Optional<MerchantRecord> findMerchantById(String merchantId) {
        return listMerchants().stream().filter(m -> m.id().equals(merchantId)).findFirst();
    }

    /**
     * V260：列出商户，includeArchived=false 时排除已存档 + 已审批删除（默认列表）；
     * M009-010：includeArchived=true 返回全量（含已审批删除行，供「已存档内容」查阅与恢复，BSM-004）。
     * 默认返回全量（内存 fake 用）；JdbcPlatformRepository 用 SQL 谓词过滤。
     */
    default List<MerchantRecord> listMerchants(boolean includeArchived) {
        if (includeArchived) return listMerchants();
        return listMerchants().stream()
                .filter(m -> m.archivedAt() == null && !"approved".equals(m.deleteStatus()))
                .toList();
    }

    /** V260/M009-010：软删除商户——标记 delete_status='approved'，行保留可查可恢复（BSM-004）；仅经删除审批通过或最高权限直接执行时调用。默认不支持，Jdbc/内存 fake 覆写。 */
    default void deleteMerchant(String merchantId) {
        throw new UnsupportedOperationException("deleteMerchant not supported by this repository");
    }

    /**
     * 按经营主体 scope 列出商户（merchants.business_entity_id = token scope）。
     * 商户面数据：经营主体登录只能看到归属本主体的商户。默认在内存过滤，JdbcPlatformRepository 用 SQL 谓词下推。
     * 隔离闸口（AC-ISO-1）：提供 business_entity_id 行级 scoped 读通道。
     * 勿删:本方法零调用是常态(多综合体隔离通道,建成待激活),但 verify:tenant-isolation 门禁
     * 以「该表存在 scoped 查询」为隔离能力判据,删除会导致门禁 fail-closed 阻断部署。
     */
    default List<MerchantRecord> listMerchantsByBusinessEntity(String businessEntityId) {
        if (businessEntityId == null) return listMerchants();
        return listMerchants().stream()
                .filter(m -> businessEntityId.equals(m.businessEntityId()))
                .toList();
    }

    /**
     * V260：统计商户被【核心运营表】以 merchant_id 引用的活跃条数，>0 则拒绝删除（含软删除标记，M009-026；防连锁孤儿）。
     * 覆盖表见 JdbcPlatformRepository 实现。默认 0（内存 fake 无引用表）。
     */
    default long countMerchantReferences(String merchantId) {
        return 0L;
    }

    ProjectRecord saveProject(ProjectRecord project);

    List<ProjectRecord> listProjects();

    SpaceRecord saveSpace(SpaceRecord space);

    List<SpaceRecord> listSpaces();

    /** M009-006：主体是否存在（绑定引用校验）。默认基于 listBusinessEntities；Jdbc 覆写按主键。 */
    default boolean businessEntityExists(String businessEntityId) {
        return listBusinessEntities().stream().anyMatch(entity -> entity.id().equals(businessEntityId));
    }

    /** M009-007：项目是否存在（空间归属校验）。默认基于 listProjects；Jdbc 覆写按主键。 */
    default boolean projectExists(String projectId) {
        return listProjects().stream().anyMatch(project -> project.id().equals(projectId));
    }

    /** 3.13：按 id 查空间（存在性校验用）。默认基于 listSpaces 过滤（空间量级小）；Jdbc 可按需覆写。 */
    default Optional<SpaceRecord> findSpaceById(String spaceId) {
        return listSpaces().stream().filter(space -> space.id().equals(spaceId)).findFirst();
    }

    /** 条目3：空间类型字典中处于 active 的 type_code 列表，供 createSpace 硬校验与前端下拉。 */
    List<String> listActiveSpaceTypeCodes();

    /** 条目3：空间类型字典全量（含停用），后台管理列表用，按 sort_order 排序。 */
    List<SpaceTypeRecord> listSpaceTypes();

    /** 条目3：录入/更新一条空间类型字典（按 type_code upsert）。 */
    SpaceTypeRecord saveSpaceType(SpaceTypeRecord spaceType);

    BusinessEntityBindingRecord saveBusinessEntityBinding(BusinessEntityBindingRecord binding);

    List<BusinessEntityBindingRecord> listBusinessEntityBindings();

    Optional<BusinessEntityBindingRecord> findBusinessEntityBindingById(String id);

    Optional<BusinessEntityBindingRecord> findActiveBusinessEntityBindingByMerchantId(String merchantId);




    /** V271 存档理由定向写入(reason 非空=存档,null=取消存档清空)。 */
    void setMerchantArchiveReason(String id, String reason);

    // AUD-005 真分页: 默认内存切片(测试双子); Jdbc 覆写 SQL COUNT + LIMIT/OFFSET。
    default com.complex.platform.common.api.PageResponse<BusinessEntityRecord> listBusinessEntitiesPaged(
            int page, int size) {
        return com.complex.platform.common.api.PageResponse.of(listBusinessEntities(), page, size);
    }

    /** V348 businessFormat 非空＝只列该业态的商户（分账按业态拆份额要用）。 */
    default com.complex.platform.common.api.PageResponse<MerchantRecord> listMerchantsPaged(
            boolean includeArchived, String businessFormat, int page, int size) {
        List<MerchantRecord> rows = listMerchants(includeArchived).stream()
                .filter(m -> businessFormat == null || businessFormat.equals(m.merchantType()))
                .toList();
        return com.complex.platform.common.api.PageResponse.of(rows, page, size);
    }

    default com.complex.platform.common.api.PageResponse<ProjectRecord> listProjectsPaged(int page, int size) {
        return com.complex.platform.common.api.PageResponse.of(listProjects(), page, size);
    }

    default com.complex.platform.common.api.PageResponse<SpaceRecord> listSpacesPaged(int page, int size) {
        return com.complex.platform.common.api.PageResponse.of(listSpaces(), page, size);
    }

    default com.complex.platform.common.api.PageResponse<BusinessEntityBindingRecord> listBusinessEntityBindingsPaged(
            int page, int size) {
        return com.complex.platform.common.api.PageResponse.of(listBusinessEntityBindings(), page, size);
    }
}
