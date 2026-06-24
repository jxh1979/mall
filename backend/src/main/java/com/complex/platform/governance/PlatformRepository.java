package com.complex.platform.governance;

import java.time.Instant;
import java.time.LocalDate;
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

    BusinessEntityBindingRecord saveBusinessEntityBinding(BusinessEntityBindingRecord binding);

    List<BusinessEntityBindingRecord> listBusinessEntityBindings();

    Optional<BusinessEntityBindingRecord> findBusinessEntityBindingById(String id);

    Optional<BusinessEntityBindingRecord> findActiveBusinessEntityBindingByMerchantId(String merchantId);

    ContractRecord saveContract(ContractRecord contract);

    List<ContractRecord> listContracts();

    Optional<ContractRecord> findContractById(String id);

    // ② 履约闭环 S1：合同生效绑定空间（contract_spaces 首次写入路径）。
    Optional<SpaceRecord> findSpaceById(String id);

    List<ContractSpaceRecord> listContractSpaces(String contractId);

    void bindContractSpace(String contractId, String spaceId, String projectId, Instant createdAt);

    /** 该空间是否已被另一份「生效中且租期与 [effectiveDate, expiryDate] 重叠」的合同占用（防重复出租）。 */
    boolean hasOverlappingEffectiveContractForSpace(
            String spaceId, LocalDate effectiveDate, LocalDate expiryDate, String excludeContractId);
}
