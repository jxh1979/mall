package com.complex.platform.governance;

import java.util.List;
import java.util.Optional;

public interface PlatformRepository {

    BusinessEntityRecord saveBusinessEntity(BusinessEntityRecord businessEntity);

    List<BusinessEntityRecord> listBusinessEntities();

    MerchantRecord saveMerchant(MerchantRecord merchant);

    List<MerchantRecord> listMerchants();

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
