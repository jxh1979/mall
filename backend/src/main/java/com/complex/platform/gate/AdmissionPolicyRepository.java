package com.complex.platform.gate;

import java.util.List;
import java.util.Optional;

public interface AdmissionPolicyRepository {

    /** 命中 (actionType, objectType) 的启用策略；无则空。objectType 为空串时不参与匹配。 */
    Optional<AdmissionPolicyRecord> findEnabledPolicy(String actionType, String objectType);

    /** 全部策略（供 listPolicies）。 */
    List<AdmissionPolicyRecord> listPolicies();
}
