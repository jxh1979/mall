package com.complex.platform.leasing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.ContractRecord;
import com.complex.platform.governance.PlatformService;
import com.complex.platform.leasing.dto.LeasingBrandBlacklistRequest;
import com.complex.platform.leasing.dto.LeasingBrandRequest;
import com.complex.platform.leasing.dto.LeasingFollowUpRequest;
import com.complex.platform.leasing.dto.LeasingIntentAdvanceRequest;
import com.complex.platform.leasing.dto.LeasingIntentRequest;
import com.complex.platform.leasing.dto.LeasingLeadAssignRequest;
import com.complex.platform.leasing.dto.LeasingLeadRequest;
import com.complex.platform.leasing.dto.LeasingLeadStatusRequest;
import com.complex.platform.leasing.dto.LeasingSignRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 招商获客 unit 测试（V161 重建）：验 7 态/来源/黑名单拦截/意向卡控/签约/跟进提醒 M1 逻辑。无 DB。 */
class LeasingServiceTests {

    private final InMemoryLeasingRepository repo = new InMemoryLeasingRepository();
    private final AuthorizationService authz = mock(AuthorizationService.class);
    private final PlatformService platform = mock(PlatformService.class);
    private LeasingService service;

    @BeforeEach
    void setUp() {
        when(authz.currentActor()).thenReturn(
                new AccessActor("usr_admin", "platform_admin_user", "platform_admin", "org_1", null, "active"));
        service = new LeasingService(repo, mock(AuditEventService.class), authz, platform);
    }

    @Test
    void leadFunnel_defaultStatusSourceEnumAssignAndReason() {
        LeasingLeadRecord lead = service.createLead(new LeasingLeadRequest(
                "张三", "13800000000", "referral", "bar", "sp_1", null, null, "k1"));
        assertThat(lead.status()).isEqualTo("pending_follow_up");

        // 来源枚举校验。
        assertThatThrownBy(() -> service.createLead(new LeasingLeadRequest(
                "李四", null, "bogus_source", null, null, null, null, "k2")))
                .isInstanceOf(ApiException.class);

        // 主管手动派单。
        LeasingLeadRecord assigned = service.assignLead(lead.id(), new LeasingLeadAssignRequest("usr_sales", "k3"));
        assertThat(assigned.ownerUserId()).isEqualTo("usr_sales");

        // lost 必填原因。
        assertThatThrownBy(() -> service.updateLeadStatus(lead.id(), new LeasingLeadStatusRequest("lost", null, "k4")))
                .isInstanceOf(ApiException.class);
        LeasingLeadRecord lost = service.updateLeadStatus(lead.id(), new LeasingLeadStatusRequest("lost", "预算不足", "k5"));
        assertThat(lost.status()).isEqualTo("lost");
        assertThat(lost.lostOrHoldReason()).isEqualTo("预算不足");
    }

    @Test
    void blacklistedBrandBlocksNewLead() {
        LeasingBrandRecord brand = service.createBrand(new LeasingBrandRequest("脏脏堡", "prospective", "餐饮", "b1"));
        service.blacklistBrand(brand.id(), new LeasingBrandBlacklistRequest(true, "多次违约", "b2"));
        assertThat(repo.findBrandById(brand.id()).orElseThrow().brandType()).isEqualTo("blacklist");

        assertThatThrownBy(() -> service.createLead(new LeasingLeadRequest(
                "王五", null, "outbound", null, null, null, brand.id(), "k1")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("LEASING_BRAND_BLACKLISTED"));
    }

    @Test
    void intentKeyStageRequiresSupervisor() {
        LeasingLeadRecord lead = service.createLead(new LeasingLeadRequest(
                "赵六", null, "exhibition", "retail", "sp_2", null, null, "k1"));
        LeasingIntentRecord intent = service.createIntent(new LeasingIntentRequest(lead.id(), "sp_2", null, Map.of(), "k2"));
        assertThat(intent.stage()).isEqualTo("initial_contact");
        assertThat(repo.findLeadById(lead.id()).orElseThrow().status()).isEqualTo("converted_to_intent");

        // 非关键阶段无需主管。
        service.advanceIntent(intent.id(), new LeasingIntentAdvanceRequest("requirement_confirmed", null, "k3"));
        // 关键阶段缺主管 → 阻断。
        assertThatThrownBy(() -> service.advanceIntent(intent.id(),
                new LeasingIntentAdvanceRequest("proposal_quoted", null, "k4")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("LEASING_INTENT_SUPERVISOR_REQUIRED"));
        // 关键阶段带主管 → 通过。
        LeasingIntentRecord advanced = service.advanceIntent(intent.id(),
                new LeasingIntentAdvanceRequest("proposal_quoted", "usr_supervisor", "k5"));
        assertThat(advanced.stage()).isEqualTo("proposal_quoted");
        assertThat(advanced.lastStageChangeBy()).isEqualTo("usr_supervisor");
    }

    @Test
    void signPendingSigningIntentCreatesContractAndMarksLeadSigned() {
        when(platform.createContractDraft(any())).thenReturn(new ContractRecord(
                "ctr_new", "mch_1", "cs", "cs", "cs", LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"),
                Map.of(), null, List.of(), "draft", Instant.now(), Instant.now()));
        LeasingLeadRecord lead = service.createLead(new LeasingLeadRequest(
                "钱七", null, "online_inquiry", null, null, null, null, "k1"));
        LeasingIntentRecord intent = service.createIntent(new LeasingIntentRequest(lead.id(), null, null, Map.of(), "k2"));

        // 非 pending_signing → 拒签。
        assertThatThrownBy(() -> service.signIntent(intent.id(), new LeasingSignRequest(
                "mch_1", "cs", null, null, LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), "k3")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("LEASING_INTENT_NOT_PENDING_SIGNING"));

        service.advanceIntent(intent.id(), new LeasingIntentAdvanceRequest("pending_signing", null, "k4"));
        ContractRecord contract = service.signIntent(intent.id(), new LeasingSignRequest(
                "mch_1", "cs", null, null, LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), "k5"));
        assertThat(contract.id()).isEqualTo("ctr_new");
        assertThat(repo.findLeadById(lead.id()).orElseThrow().status()).isEqualTo("signed");
    }

    @Test
    void followUpWritesBackLastFollowUpAndReminderFiltersDueLeads() {
        LeasingLeadRecord lead = service.createLead(new LeasingLeadRequest(
                "孙八", null, "outbound", null, null, null, null, "k1"));
        service.createFollowUp(new LeasingFollowUpRequest("lead", lead.id(), "phone", "已电话沟通", null, "k2"));
        LeasingLeadRecord after = repo.findLeadById(lead.id()).orElseThrow();
        assertThat(after.lastFollowUpAt()).isNotNull();
        assertThat(after.status()).isEqualTo("following_up");

        // 刚跟进的不进提醒；构造一个 4 天前未跟进的线索 → 进提醒；signed 的排除。
        assertThat(service.listLeadsNeedingFollowUp().items()).extracting(LeasingLeadRecord::id).doesNotContain(lead.id());
        Instant old = Instant.now().minus(4, ChronoUnit.DAYS);
        repo.saveLead(new LeasingLeadRecord("lead_old", "老线索", null, "outbound", null, null, null, null, "u",
                "following_up", null, old, null, old, old));
        repo.saveLead(new LeasingLeadRecord("lead_signed", "已签", null, "outbound", null, null, null, null, "u",
                "signed", null, old, null, old, old));
        List<String> due = service.listLeadsNeedingFollowUp().items().stream().map(LeasingLeadRecord::id).toList();
        assertThat(due).contains("lead_old").doesNotContain("lead_signed");
    }

    /** 内存仓储（实现 V161 重建后的 11 个方法）。 */
    private static final class InMemoryLeasingRepository implements LeasingRepository {
        private final Map<String, LeasingLeadRecord> leads = new LinkedHashMap<>();
        private final Map<String, LeasingIntentRecord> intents = new LinkedHashMap<>();
        private final Map<String, LeasingBrandRecord> brands = new LinkedHashMap<>();
        private final List<LeasingFollowUpRecord> followUps = new java.util.ArrayList<>();

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
        public LeasingIntentRecord saveIntent(LeasingIntentRecord intent) {
            intents.put(intent.id(), intent);
            return intent;
        }

        @Override
        public List<LeasingIntentRecord> listIntents() {
            return List.copyOf(intents.values());
        }

        @Override
        public Optional<LeasingIntentRecord> findIntentById(String id) {
            return Optional.ofNullable(intents.get(id));
        }

        @Override
        public LeasingBrandRecord saveBrand(LeasingBrandRecord brand) {
            brands.put(brand.id(), brand);
            return brand;
        }

        @Override
        public List<LeasingBrandRecord> listBrands() {
            return List.copyOf(brands.values());
        }

        @Override
        public Optional<LeasingBrandRecord> findBrandById(String id) {
            return Optional.ofNullable(brands.get(id));
        }

        @Override
        public LeasingFollowUpRecord saveFollowUp(LeasingFollowUpRecord followUp) {
            followUps.add(followUp);
            return followUp;
        }

        @Override
        public List<LeasingFollowUpRecord> listFollowUpsByRef(String refType, String refId) {
            return followUps.stream().filter(f -> f.refType().equals(refType) && f.refId().equals(refId)).toList();
        }
    }
}
