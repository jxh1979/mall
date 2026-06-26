package com.complex.platform.leasing;

import java.util.List;
import java.util.Optional;

/** 招商仓储（leasing_leads / leasing_intents / leasing_brands / leasing_follow_ups，V161 重建）。 */
public interface LeasingRepository {

    LeasingLeadRecord saveLead(LeasingLeadRecord lead);

    List<LeasingLeadRecord> listLeads();

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
