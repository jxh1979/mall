package com.complex.platform.facility;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.complex.platform.access.AccessActor;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.EvidenceEventRepository;
import com.complex.platform.evidence.EvidenceAttachmentRecord;
import com.complex.platform.facility.dto.SiteEntryCompletionUploadRequest;
import com.complex.platform.facility.dto.SiteEntryReportAcceptanceRequest;
import com.complex.platform.support.TestSecurityActors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "evidence.attachment.dir=/tmp/complex-site-entry-completion-test"
})
@Transactional
class SiteEntryCompletionJdbcIntegrationTests {
    @Autowired SiteEntryReportService service;
    @Autowired SiteEntryReportController controller;
    @Autowired SiteEntryReportRepository repository;
    @Autowired EvidenceEventRepository evidence;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    private String project;
    private String vendor;
    private String reviewer;
    private String report;
    private final List<Path> files = new ArrayList<>();

    @BeforeEach void seed() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        project = "site_project_" + suffix; vendor = "site_vendor_" + suffix;
        reviewer = "site_reviewer_" + suffix; report = "site_report_" + suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','施工测试','active',now(),now())", project);
        seedUser(vendor, "outsourced_vendor", List.of("facility.site_entry.upload_completion_assigned", "facility.site_entry.view_assigned"));
        seedUser(reviewer, "staff", List.of("facility.site_entry.review", "facility.site_entry.accept"));
        Instant now = Instant.now();
        repository.insert(new SiteEntryReportRecord(report, project, null, "施工整改", LocalDate.now(), LocalDate.now().plusDays(1),
                2, "现场联系人", "13800000000", vendor, "in_progress", null, null, vendor, now, now, List.of(), null, null, null));
        as(vendor, "outsourced_vendor");
    }

    @AfterEach void clear() throws Exception {
        for (Path path : files) Files.deleteIfExists(path);
        TestSecurityActors.clear();
    }

    @Test void uploadedFileRectificationHistoryAndAcceptanceRoundTrip() throws Exception {
        String before = upload("before");
        var first = new SiteEntryCompletionUploadRequest(List.of(before), "第一轮", "first-" + report);
        service.uploadCompletion(report, first);
        service.uploadCompletion(report, first);
        as(reviewer, "staff");
        var reject = new SiteEntryReportAcceptanceRequest(1, "rectify", "补充现场图", "reject-" + report);
        controller.accept(report, reject);
        controller.accept(report, reject);
        as(vendor, "outsourced_vendor");
        String after = upload("after");
        service.uploadCompletion(report, new SiteEntryCompletionUploadRequest(List.of(after), "已整改", "second-" + report));
        as(reviewer, "staff");
        assertThat(controller.attachmentFile(report, after).getBody().getInputStream().readAllBytes()).isEqualTo(new byte[] {1, 2, 3, 4});
        var pass = new SiteEntryReportAcceptanceRequest(3, "pass", "现场通过", "pass-" + report);
        controller.accept(report, pass);
        controller.accept(report, pass);
        assertThat(service.findById(report).status()).isEqualTo("accepted");
        var history = service.history(report);
        assertThat(history).hasSize(4);
        assertThat(history.get(0).attachmentIds()).containsExactly(before);
        assertThat(history.get(1).note()).isEqualTo("补充现场图");
        assertThat(history.get(3).completionSequence()).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM site_entry_report_history WHERE report_id=?", Integer.class, report)).isEqualTo(4);
    }

    @Test void anotherVendorCannotUploadOrReadOriginalReportsFile() {
        String id = upload("own");
        String other = vendor + "_other";
        seedUser(other, "outsourced_vendor", List.of("facility.site_entry.upload_completion_assigned", "facility.site_entry.view_assigned"));
        as(other, "outsourced_vendor");
        assertThatThrownBy(() -> controller.uploadAttachment(report, file("cross"))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> controller.attachmentFile(report, id)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> controller.history(report)).isInstanceOf(ApiException.class);
    }

    @Test void fabricatedUploadedMetadataCannotCountAsCompletion() {
        Instant now = Instant.now();
        String id = "ghost-" + report;
        evidence.saveAttachment(new EvidenceAttachmentRecord(id, "ghost.png", "image/png", 4L, "facility_site_entry_report",
                report, "completion", "evidence-files/does-not-exist-" + report + ".png", "/no-file", "uploaded", null, now, now, null));
        assertThatThrownBy(() -> service.uploadCompletion(report, new SiteEntryCompletionUploadRequest(List.of(id), "假文件", "ghost-" + report)))
                .isInstanceOf(ApiException.class);
        assertThat(repository.history(report)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='facility.site_entry.upload_completion_failed' AND business_id=?", Integer.class, report)).isEqualTo(1);
    }

    @Test void propertyPermissionMustReachThisReportAndReadCanUseItsReviewGrant() {
        String otherProject = project + "_other";
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','另一项目','active',now(),now())", otherProject);
        jdbc.update("INSERT INTO role_viewable_projects(role_id,viewable_id) VALUES(?,?)", "role_" + reviewer, otherProject);
        as(reviewer, "staff");
        assertThat(controller.list().data().items()).isEmpty();
        assertThatThrownBy(() -> controller.get(report)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> controller.history(report)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> controller.accept(report, new SiteEntryReportAcceptanceRequest(1, "pass", "错误项目权限", "scope-deny-" + report)))
                .isInstanceOf(ApiException.class);
        String reviewRole = "review_" + reviewer;
        jdbc.update("INSERT INTO roles(id,role_name,created_at) VALUES(?,?,now())", reviewRole, "施工复核测试角色" + new java.math.BigInteger(UUID.randomUUID().toString().replace("-", ""), 16));
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'facility.site_entry.review')", reviewRole);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", reviewer, reviewRole);
        assertThat(controller.get(report).data().id()).isEqualTo(report);
        assertThat(controller.history(report).data()).isEmpty();
        assertThatThrownBy(() -> controller.accept(report, new SiteEntryReportAcceptanceRequest(1, "pass", "读权限不等于验收权限", "scope-still-deny-" + report)))
                .isInstanceOf(ApiException.class);
        assertThat(repository.history(report)).isEmpty();
    }

    @Test void failedAcceptanceLeavesAuditButNoAcceptanceFact() {
        as(reviewer, "staff");
        assertThatThrownBy(() -> service.accept(report,
                new SiteEntryReportAcceptanceRequest(1, "pass", "无材料不能通过", "no-material-" + report)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("COMPLETION_REQUIRED"));
        assertThat(repository.history(report)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='facility.site_entry.accept_failed' AND business_id=?", Integer.class, report)).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void concurrentNewCompletionMakesWaitingOldAcceptanceStale() throws Exception {
        String before = upload("before-race");
        service.uploadCompletion(report, new SiteEntryCompletionUploadRequest(List.of(before), "第一轮", "race-first-" + report));
        String after = upload("after-race");
        var saved = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var acceptStarted = new java.util.concurrent.CountDownLatch(1);
        var acceptingPid = new java.util.concurrent.atomic.AtomicInteger();
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        var tx = new org.springframework.transaction.support.TransactionTemplate(transactions);
        try {
            var writer = pool.submit(() -> {
                as(vendor, "outsourced_vendor");
                try {
                    tx.executeWithoutResult(status -> {
                        service.uploadCompletion(report, new SiteEntryCompletionUploadRequest(List.of(after), "新材料", "race-next-" + report));
                        saved.countDown();
                        try {
                            if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("验收未进入等待");
                        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                    });
                } finally { TestSecurityActors.clear(); }
            });
            assertThat(saved.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            var reader = pool.submit(() -> {
                as(reviewer, "staff");
                try {
                    tx.executeWithoutResult(status -> {
                        acceptingPid.set(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
                        acceptStarted.countDown();
                        service.accept(report, new SiteEntryReportAcceptanceRequest(1, "pass", "旧材料页面", "race-accept-" + report));
                    });
                    return "unexpected-success";
                } catch (ApiException error) { return error.code(); }
                finally { TestSecurityActors.clear(); }
            });
            assertThat(acceptStarted.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
            boolean blocked = false;
            while (System.nanoTime() < deadline && !reader.isDone()) {
                blocked = Boolean.TRUE.equals(jdbc.queryForObject("SELECT cardinality(pg_blocking_pids(?)) > 0", Boolean.class, acceptingPid.get()));
                if (blocked) break;
                java.util.concurrent.locks.LockSupport.parkNanos(java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(20));
            }
            assertThat(blocked).as("验收事务须等待材料提交事务的报备行锁").isTrue();
            release.countDown();
            writer.get(5, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(reader.get(5, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo("COMPLETION_CHANGED");
            assertThat(repository.history(report)).extracting(SiteEntryHistoryRecord::action).containsExactly("completion", "completion");
            assertThat(repository.findById(report).orElseThrow().completionAttachmentIds()).containsExactly(after);
            as(reviewer, "staff");
            service.accept(report, new SiteEntryReportAcceptanceRequest(2, "pass", "确认最新材料", "race-current-" + report));
            assertThat(repository.history(report)).hasSize(3);
        } finally {
            release.countDown();
            pool.shutdownNow();
            pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        }
    }

    private String upload(String name) {
        EvidenceAttachmentRecord saved = controller.uploadAttachment(report, file(name)).data();
        files.add(Path.of("/tmp/complex-site-entry-completion-test", saved.objectKey().substring("evidence-files/".length())));
        return saved.id();
    }
    private MockMultipartFile file(String name) {
        return new MockMultipartFile("file", name + ".png", "image/png", new byte[] {1, 2, 3, 4});
    }
    private void seedUser(String id, String type, List<String> codes) {
        String role = "role_" + id;
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,status,created_at) VALUES(?,?,?,?,'active',now())", id, id, type, project);
        jdbc.update("INSERT INTO roles(id,role_name,created_at) VALUES(?,?,now())", role, "施工测试角色" + new java.math.BigInteger(UUID.randomUUID().toString().replace("-", ""), 16));
        for (String code : codes) jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,?)", role, code);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", id, role);
    }
    private void as(String id, String type) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(id, id, type, project, null, "active"), "token", List.of()));
    }
}
