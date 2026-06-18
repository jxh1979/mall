package com.complex.platform.merchantservice;

import java.util.List;
import java.util.Optional;

public interface MerchantServiceRepository {
    List<MerchantWorkOrderRecord> findWorkOrdersByScope(Optional<String> merchantScope, int limit);

    Optional<MerchantWorkOrderRecord> findWorkOrderById(String id);

    MerchantWorkOrderRecord saveWorkOrder(MerchantWorkOrderRecord record);

    MerchantWorkOrderActionRecord saveAction(MerchantWorkOrderActionRecord record);

    List<MerchantWorkOrderActionRecord> findActionsByWorkOrderId(String workOrderId);
}
