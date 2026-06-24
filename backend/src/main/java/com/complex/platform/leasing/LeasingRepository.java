package com.complex.platform.leasing;

import java.util.List;
import java.util.Optional;

/** 招商/租赁仓储（leasing_leads / leasing_offers，V164）。 */
public interface LeasingRepository {

    LeasingLeadRecord saveLead(LeasingLeadRecord lead);

    List<LeasingLeadRecord> listLeads();

    Optional<LeasingLeadRecord> findLeadById(String id);

    LeasingOfferRecord saveOffer(LeasingOfferRecord offer);

    List<LeasingOfferRecord> listOffers();

    Optional<LeasingOfferRecord> findOfferById(String id);
}
