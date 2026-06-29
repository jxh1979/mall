package com.complex.platform.facility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.facility.dto.SiteEntryReportAcceptanceRequest;
import com.complex.platform.facility.dto.SiteEntryReportRequest;
import com.complex.platform.facility.dto.SiteEntryReportReviewRequest;
import com.complex.platform.facility.dto.SiteEntryReportSubmitRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SiteEntryReportServiceTests {

    private InMemorySiteEntryReportRepository reports;

    @BeforeEach
    void setUp() {
        reports = new InMemorySiteEntryReportRepository();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void outsourcedListAndDetailAreLimitedToOwnAssigneeAndProject() {
        SiteEntryReportService scoped = outsourcedService("usr_vendor_a", "proj_1");
        reports.insert(raw("ser_own", "submitted", "usr_vendor_a", "proj_1"));
        reports.insert(raw("ser_other", "submitted", "usr_vendor_b", "proj_1"));
        reports.insert(raw("ser_cross_project", "submitted", "usr_vendor_a", "proj_2"));

        PageResponse<SiteEntryReportRecord> page = scoped.list();

        assertThat(page.items()).extracting(SiteEntryReportRecord::id).containsExactly("ser_own");
        assertThat(scoped.findById("ser_own").id()).isEqualTo("ser_own");
        assertThatThrownBy(() -> scoped.findById("ser_other"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FORBIDDEN"));
        assertThatThrownBy(() -> scoped.findById("ser_cross_project"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FORBIDDEN"));
    }

    @Test
    void outsourcedCannotOperateOthersOrPropertyReviewAcceptance() {
        SiteEntryReportService scoped = outsourcedService("usr_vendor_a", "proj_1");
        reports.insert(raw("ser_other", "approved", "usr_vendor_b", "proj_1"));
        reports.insert(raw("ser_own_submitted", "submitted", "usr_vendor_a", "proj_1"));
        reports.insert(raw("ser_own_progress", "in_progress", "usr_vendor_a", "proj_1"));

        assertThatThrownBy(() -> scoped.submit("ser_other", submit("k_submit_other")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FORBIDDEN"));
        assertThatThrownBy(() -> scoped.review("ser_own_submitted", review("approve", "k_review")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("PLATFORM_SCOPE_REQUIRED"));
        assertThatThrownBy(() -> scoped.accept("ser_own_progress", accept("pass", "k_accept")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("PLATFORM_SCOPE_REQUIRED"));
    }

    @Test
    void createApproveSubmitAcceptHappyPath() {
        SiteEntryReportService scoped = outsourcedService("usr_vendor_a", "proj_1");
        SiteEntryReportRecord created = scoped.create(createReq("proj_1", "k_create"));

        SiteEntryReportService property = propertyService("usr_ops", "proj_1");
        SiteEntryReportRecord approved = property.review(created.id(), review("approve", "k_review"));
        authenticateOutsourced("usr_vendor_a", "proj_1");
        SiteEntryReportRecord inProgress = scoped.submit(approved.id(), submit("k_submit"));
        property = propertyService("usr_ops", "proj_1");
        SiteEntryReportRecord accepted = property.accept(inProgress.id(), accept("pass", "k_accept"));

        assertThat(created.status()).isEqualTo("submitted");
        assertThat(created.vendorUserId()).isEqualTo("usr_vendor_a");
        assertThat(created.createdBy()).isEqualTo("usr_vendor_a");
        assertThat(approved.status()).isEqualTo("approved");
        assertThat(inProgress.status()).isEqualTo("in_progress");
        assertThat(accepted.status()).isEqualTo("accepted");
    }

    @Test
    void propertyReadsOnlyOwnProjectAndMerchantScopeIsRejected() {
        SiteEntryReportService property = propertyService("usr_ops", "proj_1");
        reports.insert(raw("ser_project", "submitted", "usr_vendor_a", "proj_1"));
        reports.insert(raw("ser_other_project", "submitted", "usr_vendor_b", "proj_2"));

        PageResponse<SiteEntryReportRecord> page = property.list();

        assertThat(page.items()).extracting(SiteEntryReportRecord::id).containsExactly("ser_project");
        assertThat(property.findById("ser_project").id()).isEqualTo("ser_project");
        assertThatThrownBy(() -> property.findById("ser_other_project"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FORBIDDEN"));
        assertThatThrownBy(() -> property.review("ser_other_project", review("approve", "k_review_cross_project")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FORBIDDEN"));

        SiteEntryReportService merchant = merchantService("usr_merchant", "proj_1", "mch_1");
        assertThatThrownBy(merchant::list)
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("PLATFORM_SCOPE_REQUIRED"));
    }

    @Test
    void rejectsIllegalStateTransitions() {
        SiteEntryReportService scoped = outsourcedService("usr_vendor_a", "proj_1");
        reports.insert(raw("ser_submitted", "submitted", "usr_vendor_a", "proj_1"));
        reports.insert(raw("ser_approved", "approved", "usr_vendor_a", "proj_1"));

        assertThatThrownBy(() -> scoped.submit("ser_submitted", submit("k_submit_bad")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("INVALID_STATE"));
        SiteEntryReportService property = propertyService("usr_ops", "proj_1");
        assertThatThrownBy(() -> property.review("ser_approved", review("approve", "k_review_bad")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("INVALID_STATE"));
        assertThatThrownBy(() -> property.accept("ser_approved", accept("pass", "k_accept_bad")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("INVALID_STATE"));
    }

    private SiteEntryReportRequest createReq(String projectId, String key) {
        return new SiteEntryReportRequest(projectId, "sp_1", "店铺装修围挡施工",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-05"), 6,
                "张三", "13800000000", key);
    }

    private SiteEntryReportSubmitRequest submit(String key) {
        return new SiteEntryReportSubmitRequest("进场施工", key);
    }

    private SiteEntryReportReviewRequest review(String decision, String key) {
        return new SiteEntryReportReviewRequest(decision, "ok", key);
    }

    private SiteEntryReportAcceptanceRequest accept(String result, String key) {
        return new SiteEntryReportAcceptanceRequest(result, "ok", key);
    }

    private SiteEntryReportRecord raw(String id, String status, String assigneeId, String projectId) {
        Instant now = Instant.now();
        return new SiteEntryReportRecord(id, projectId, "sp_1", "work", LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-07-05"), 3, "contact", "13800000000", assigneeId, status,
                null, null, assigneeId, now, now, null);
    }

    private SiteEntryReportService outsourcedService(String userId, String projectId) {
        authenticateOutsourced(userId, projectId);
        return new SiteEntryReportService(reports, mock(AuditEventService.class), new AuthorizationService(null));
    }

    private SiteEntryReportService propertyService(String userId, String projectId) {
        authenticate(new AccessActor(userId, userId, "staff", projectId, null, "active"));
        return new SiteEntryReportService(reports, mock(AuditEventService.class), new AuthorizationService(null));
    }

    private SiteEntryReportService merchantService(String userId, String projectId, String merchantId) {
        authenticate(new AccessActor(userId, userId, "merchant", projectId, merchantId, "active"));
        return new SiteEntryReportService(reports, mock(AuditEventService.class), new AuthorizationService(null));
    }

    private void authenticateOutsourced(String userId, String projectId) {
        authenticate(new AccessActor(userId, userId, "outsourced_vendor", projectId, null, "active"));
    }

    private void authenticate(AccessActor actor) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }

    static class InMemorySiteEntryReportRepository implements SiteEntryReportRepository {
        final Map<String, SiteEntryReportRecord> store = new LinkedHashMap<>();

        @Override
        public void insert(SiteEntryReportRecord record) {
            store.put(record.id(), record);
        }

        @Override
        public void update(SiteEntryReportRecord record) {
            store.put(record.id(), record);
        }

        @Override
        public Optional<SiteEntryReportRecord> findById(String id) {
            return Optional.ofNullable(store.get(id))
                    .filter(record -> record.deletedAt() == null);
        }

        @Override
        public List<SiteEntryReportRecord> list() {
            return store.values().stream()
                    .filter(record -> record.deletedAt() == null)
                    .sorted(Comparator.comparing(SiteEntryReportRecord::createdAt).reversed())
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }
    }
}
