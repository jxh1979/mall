package com.complex.platform.facility;
// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.facility.dto.SiteEntryCompletionUploadRequest;
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
    private final Map<String, com.complex.platform.evidence.EvidenceAttachmentRecord> files = new LinkedHashMap<>();
    private com.complex.platform.evidence.EvidenceEventRepository evidence;
    private com.complex.platform.evidence.EvidenceAttachmentValidator attachments;
    private com.complex.platform.common.idempotency.IdempotencyService idempotency;

    @BeforeEach
    void setUp() {
        reports = new InMemorySiteEntryReportRepository();
        evidence = mock(com.complex.platform.evidence.EvidenceEventRepository.class);
        attachments = mock(com.complex.platform.evidence.EvidenceAttachmentValidator.class);
        org.mockito.Mockito.when(evidence.findAttachmentById(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(call -> Optional.ofNullable(files.get(call.getArgument(0))));
        idempotency = new com.complex.platform.common.idempotency.IdempotencyService(
                new com.complex.platform.support.InMemoryIdempotencyRepository(), new com.fasterxml.jackson.databind.ObjectMapper());
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
        registerFile("att_done", inProgress.id());
        scoped.uploadCompletion(inProgress.id(), new SiteEntryCompletionUploadRequest(List.of("att_done"), "完工", "k_material"));
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
    void outsourcedCanUploadCompletionForOwnInProgressReportOnly() {
        SiteEntryReportService scoped = outsourcedService("usr_vendor_a", "proj_1");
        reports.insert(raw("ser_own_progress", "in_progress", "usr_vendor_a", "proj_1"));
        reports.insert(raw("ser_other_progress", "in_progress", "usr_vendor_b", "proj_1"));

        registerFile("att_1", "ser_own_progress");
        registerFile("att_2", "ser_own_progress");
        SiteEntryReportRecord uploaded = scoped.uploadCompletion(
                "ser_own_progress", new SiteEntryCompletionUploadRequest(List.of("att_1", "att_2"), "完工照片", "k_upload"));

        assertThat(uploaded.completionAttachmentIds()).containsExactly("att_1", "att_2");
        assertThat(uploaded.completionNote()).isEqualTo("完工照片");
        assertThat(uploaded.completionUploadedAt()).isNotNull();
        assertThatThrownBy(() -> scoped.uploadCompletion(
                "ser_other_progress", new SiteEntryCompletionUploadRequest(List.of("att_3"), "cross", "k_upload_other")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FORBIDDEN"));
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

    @Test
    void rectifiedCompletionCanBeSubmittedAndAcceptedAgain() {
        reports.insert(raw("ser_rectifying", "rectifying", "usr_vendor_a", "proj_1"));
        registerFile("att_rectified", "ser_rectifying");
        SiteEntryReportService vendor = outsourcedService("usr_vendor_a", "proj_1");
        SiteEntryReportRecord submitted = vendor.uploadCompletion("ser_rectifying",
                new SiteEntryCompletionUploadRequest(List.of("att_rectified"), "整改后的完工材料", "rectified-upload"));
        assertThat(submitted.status()).isEqualTo("in_progress");
        SiteEntryReportService property = propertyService("usr_ops", "proj_1");
        assertThat(property.accept(submitted.id(), accept("pass", "rectified-accept")).status()).isEqualTo("accepted");
    }

    @Test
    void completionAndAcceptanceKeepEveryRoundAndReplayOnlyOnce() {
        reports.insert(raw("rounds", "in_progress", "usr_vendor_a", "proj_1"));
        registerFile("before", "rounds");
        registerFile("after", "rounds");
        var vendor = outsourcedService("usr_vendor_a", "proj_1");
        var first = new SiteEntryCompletionUploadRequest(List.of("before"), "原材料", "first");
        vendor.uploadCompletion("rounds", first);
        vendor.uploadCompletion("rounds", first);
        var property = propertyService("usr_ops", "proj_1");
        var rejected = new SiteEntryReportAcceptanceRequest(1, "rectify", "须补照片", "reject");
        property.accept("rounds", rejected);
        property.accept("rounds", rejected);
        vendor = outsourcedService("usr_vendor_a", "proj_1");
        var activeVendor = vendor;
        assertThatThrownBy(() -> activeVendor.uploadCompletion("rounds",
                new SiteEntryCompletionUploadRequest(List.of("before"), "仍用旧附件", "reuse")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("NEW_COMPLETION_REQUIRED"));
        vendor.uploadCompletion("rounds", new SiteEntryCompletionUploadRequest(List.of("after"), "已整改", "second"));
        property = propertyService("usr_ops", "proj_1");
        var activeProperty = property;
        assertThatThrownBy(() -> activeProperty.accept("rounds", new SiteEntryReportAcceptanceRequest(1, "pass", "旧页面", "stale")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("COMPLETION_CHANGED"));
        property.accept("rounds", new SiteEntryReportAcceptanceRequest(3, "pass", "通过", "accept-new"));
        var history = property.history("rounds");
        assertThat(history).extracting(SiteEntryHistoryRecord::action)
                .containsExactly("completion", "acceptance", "completion", "acceptance");
        assertThat(history.get(0).attachmentIds()).containsExactly("before");
        assertThat(history.get(1).note()).isEqualTo("须补照片");
        assertThat(history.get(3).completionSequence()).isEqualTo(3);
    }

    @Test
    void acceptanceRequiresCompletionAndEvidenceMustBelongToReport() {
        reports.insert(raw("empty", "in_progress", "usr_vendor_a", "proj_1"));
        var property = propertyService("usr_ops", "proj_1");
        assertThatThrownBy(() -> property.accept("empty", accept("pass", "without-material")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("COMPLETION_REQUIRED"));
        registerFile("foreign", "other-report");
        var vendor = outsourcedService("usr_vendor_a", "proj_1");
        assertThatThrownBy(() -> vendor.uploadCompletion("empty",
                new SiteEntryCompletionUploadRequest(List.of("made-up"), "伪造", "missing")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("EVIDENCE_ATTACHMENT_NOT_FOUND"));
        assertThatThrownBy(() -> vendor.uploadCompletion("empty",
                new SiteEntryCompletionUploadRequest(List.of("foreign"), "跨报备", "foreign")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SITE_ENTRY_EVIDENCE_MISMATCH"));
        assertThat(reports.history("empty")).isEmpty();
    }

    @Test
    void reusedSubmissionKeyWithDifferentMaterialsIsRejected() {
        reports.insert(raw("same-key", "in_progress", "usr_vendor_a", "proj_1"));
        registerFile("a", "same-key");
        registerFile("b", "same-key");
        var vendor = outsourcedService("usr_vendor_a", "proj_1");
        vendor.uploadCompletion("same-key", new SiteEntryCompletionUploadRequest(List.of("a"), "first", "one"));
        assertThatThrownBy(() -> vendor.uploadCompletion("same-key",
                new SiteEntryCompletionUploadRequest(List.of("b"), "changed", "one")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
        assertThat(reports.history("same-key")).hasSize(1);
    }

    private void registerFile(String id, String reportId) {
        files.put(id, new com.complex.platform.evidence.EvidenceAttachmentRecord(id, id + ".png", "image/png", 10L,
                "facility_site_entry_report", reportId, "completion", "file/" + id, "/file/" + id, "uploaded", null,
                Instant.now(), Instant.now(), null));
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
        return new SiteEntryReportAcceptanceRequest(1, result, "ok", key);
    }

    private SiteEntryReportRecord raw(String id, String status, String assigneeId, String projectId) {
        Instant now = Instant.now();
        return new SiteEntryReportRecord(id, projectId, "sp_1", "work", LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-07-05"), 3, "contact", "13800000000", assigneeId, status,
                null, null, assigneeId, now, now, List.of(), null, null, null);
    }

    private SiteEntryReportService outsourcedService(String userId, String projectId) {
        authenticateOutsourced(userId, projectId);
        return new SiteEntryReportService(reports, mock(AuditEventService.class), new AuthorizationService(null), evidence, attachments, idempotency);
    }

    private SiteEntryReportService propertyService(String userId, String projectId) {
        authenticate(new AccessActor(userId, userId, "staff", projectId, null, "active"));
        var auth = org.mockito.Mockito.spy(new AuthorizationService(null));
        org.mockito.Mockito.doAnswer(call -> projectId.equals(call.getArgument(1)))
                .when(auth).canReachProject(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(String[].class));
        // 18E（第 19 条 19-R3）：列表按可触及项目取数——只到本人项目（与现行等价）
        org.mockito.Mockito.doReturn(AuthorizationService.ProjectReach.ownOnly(projectId))
                .when(auth).projectReach(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(String[].class));
        return new SiteEntryReportService(reports, mock(AuditEventService.class), auth, evidence, attachments, idempotency);
    }

    private SiteEntryReportService merchantService(String userId, String projectId, String merchantId) {
        authenticate(new AccessActor(userId, userId, "merchant", projectId, merchantId, "active"));
        return new SiteEntryReportService(reports, mock(AuditEventService.class), new AuthorizationService(null), evidence, attachments, idempotency);
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
        final Map<String, List<SiteEntryHistoryRecord>> histories = new LinkedHashMap<>();

        @Override public Optional<SiteEntryReportRecord> findByIdForUpdate(String id) { return findById(id); }
        @Override public List<SiteEntryHistoryRecord> history(String id) { return List.copyOf(histories.getOrDefault(id, List.of())); }
        @Override public void appendHistory(SiteEntryHistoryRecord record) {
            histories.computeIfAbsent(record.reportId(), id -> new ArrayList<>()).add(record);
        }

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
