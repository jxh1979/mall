package com.complex.platform.governance;

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
     * V260：列出商户，includeArchived=false 时排除已存档 + 已审批删除（默认列表）。
     * 默认返回全量（内存 fake 用）；JdbcPlatformRepository 用 SQL 谓词过滤。
     */
    default List<MerchantRecord> listMerchants(boolean includeArchived) {
        if (includeArchived) return listMerchants();
        return listMerchants().stream()
                .filter(m -> m.archivedAt() == null && !"approved".equals(m.deleteStatus()))
                .toList();
    }

    /** V260：物理删除商户（仅经删除审批通过后调用）。默认不支持，Jdbc/内存 fake 覆写。 */
    default void deleteMerchant(String merchantId) {
        throw new UnsupportedOperationException("deleteMerchant not supported by this repository");
    }

    /**
     * V260：统计商户被【核心运营表】以 merchant_id 引用的活跃条数，>0 则拒绝硬删（防连锁孤儿）。
     * 覆盖表见 JdbcPlatformRepository 实现。默认 0（内存 fake 无引用表）。
     */
    default long countMerchantReferences(String merchantId) {
        return 0L;
    }

    /**
     * 按经营主体 scope 列出商户（merchants.business_entity_id = token scope）。
     * 商户面数据：经营主体登录只能看到归属本主体的商户。默认在内存过滤，JdbcPlatformRepository 用 SQL 谓词下推。
     * 隔离闸口（AC-ISO-1）：提供 business_entity_id 行级 scoped 读通道。
     */
    default List<MerchantRecord> listMerchantsByBusinessEntity(String businessEntityId) {
        if (businessEntityId == null) return listMerchants();
        return listMerchants().stream()
                .filter(m -> businessEntityId.equals(m.businessEntityId()))
                .toList();
    }

    ProjectRecord saveProject(ProjectRecord project);

    List<ProjectRecord> listProjects();

    SpaceRecord saveSpace(SpaceRecord space);

    List<SpaceRecord> listSpaces();

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



}
