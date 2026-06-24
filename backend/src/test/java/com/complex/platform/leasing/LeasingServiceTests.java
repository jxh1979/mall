package com.complex.platform.leasing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.ContractRecord;
import com.complex.platform.governance.PlatformService;
import com.complex.platform.leasing.dto.LeasingConvertRequest;
import com.complex.platform.leasing.dto.LeasingLeadRequest;
import com.complex.platform.leasing.dto.LeasingOfferRequest;
import com.complex.platform.leasing.dto.LeasingStatusRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LeasingServiceTests {

    private final InMemoryLeasingRepository repo = new InMemoryLeasingRepository();
    private final AuthorizationService authz = mock(AuthorizationService.class);
    private final PlatformService platform = mock(PlatformService.class);
    private LeasingService service;

    @BeforeEach
    void setUp() {
        service = new LeasingService(repo, mock(AuditEventService.class), authz, platform);
    }

    @Test
    void leadFunnelAndOfferLifecycle() {
        LeasingLeadRecord lead = service.createLead(new LeasingLeadRequest("张三", "bar", "sp_1", "walk_in", "k1"));
        assertThat(lead.status()).isEqualTo("new");
        service.updateLeadStatus(lead.id(), new LeasingStatusRequest("qualified", "k2"));
        assertThat(repo.findLeadById(lead.id()).orElseThrow().status()).isEqualTo("qualified");

        LeasingOfferRecord offer = service.createOffer(new LeasingOfferRequest(lead.id(), "sp_1", "mch_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of("baseRentCent", 1_000_000L),
                LocalDate.parse("2026-07-15"), "k3"));
        assertThat(offer.status()).isEqualTo("draft");
        service.updateOfferStatus(offer.id(), new LeasingStatusRequest("sent", "k4"));
        service.updateOfferStatus(offer.id(), new LeasingStatusRequest("accepted", "k5"));
        assertThat(repo.findOfferById(offer.id()).orElseThrow().status()).isEqualTo("accepted");

        // 非法状态拒。
        assertThatThrownBy(() -> service.updateOfferStatus(offer.id(), new LeasingStatusRequest("bogus", "k6")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void convertAcceptedOfferToContractAndIsGuarded() {
        when(platform.createContractDraft(any())).thenReturn(new ContractRecord(
                "ctr_new", "mch_1", "cs", "os", "ss", LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"),
                Map.of(), null, List.of(), "draft", Instant.now(), Instant.now()));
        LeasingLeadRecord lead = service.createLead(new LeasingLeadRequest("李四", "retail", null, null, "k1"));
        LeasingOfferRecord offer = service.createOffer(new LeasingOfferRequest(lead.id(), "sp_1", "mch_1",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), Map.of(), null, "k2"));

        // 未 accepted → 拒转。
        assertThatThrownBy(() -> service.convertOfferToContract(offer.id(), new LeasingConvertRequest("cs", "os", "ss", "k3")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("LEASING_OFFER_NOT_ACCEPTED"));

        service.updateOfferStatus(offer.id(), new LeasingStatusRequest("accepted", "k4"));
        ContractRecord contract = service.convertOfferToContract(offer.id(), new LeasingConvertRequest("cs", "os", "ss", "k5"));
        assertThat(contract.id()).isEqualTo("ctr_new");
        assertThat(repo.findOfferById(offer.id()).orElseThrow().status()).isEqualTo("converted");
        assertThat(repo.findLeadById(lead.id()).orElseThrow().status()).isEqualTo("converted");

        // 已转 → 拒重复建。
        assertThatThrownBy(() -> service.convertOfferToContract(offer.id(), new LeasingConvertRequest("cs", "os", "ss", "k6")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("LEASING_OFFER_ALREADY_CONVERTED"));
    }

    private static final class InMemoryLeasingRepository implements LeasingRepository {
        private final Map<String, LeasingLeadRecord> leads = new LinkedHashMap<>();
        private final Map<String, LeasingOfferRecord> offers = new LinkedHashMap<>();

        @Override
        public LeasingLeadRecord saveLead(LeasingLeadRecord lead) {
            leads.put(lead.id(), lead);
            return lead;
        }

        @Override
        public List<LeasingLeadRecord> listLeads() {
            return List.copyOf(leads.values());
        }

        @Override
        public Optional<LeasingLeadRecord> findLeadById(String id) {
            return Optional.ofNullable(leads.get(id));
        }

        @Override
        public LeasingOfferRecord saveOffer(LeasingOfferRecord offer) {
            offers.put(offer.id(), offer);
            return offer;
        }

        @Override
        public List<LeasingOfferRecord> listOffers() {
            return List.copyOf(offers.values());
        }

        @Override
        public Optional<LeasingOfferRecord> findOfferById(String id) {
            return Optional.ofNullable(offers.get(id));
        }
    }
}
