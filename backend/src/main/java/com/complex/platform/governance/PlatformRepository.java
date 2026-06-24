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

    // ② 履约闭环 S3：合同到期/退租/续约 + 空间释放。

    /** 改合同生命周期；terminatedAt/terminationReason 仅退租时写（到期/其它传 null）。 */
    void updateContractLifecycle(String contractId, String status, Instant terminatedAt, String terminationReason);

    /** 释放（删除）该合同的全部空间绑定；返回释放数。退租/到期后 over_space 即不再认其租赁。 */
    int releaseContractSpaces(String contractId);

    /** 列出已过 expiry_date 仍为 effective 的合同 id（到期 Job 据此转 expired + 释放空间）。 */
    List<String> listEffectiveContractIdsExpiredBy(LocalDate asOf);

    /** 续约链：记新合同由哪份旧合同续来。 */
    void setContractRenewedFrom(String newContractId, String fromContractId);
}
