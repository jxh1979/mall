package com.complex.platform.governance;

import java.util.List;
import java.util.Optional;

public interface PlatformRepository {

    BusinessEntityRecord saveBusinessEntity(BusinessEntityRecord businessEntity);

    List<BusinessEntityRecord> listBusinessEntities();

    MerchantRecord saveMerchant(MerchantRecord merchant);

    List<MerchantRecord> listMerchants();

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
}
