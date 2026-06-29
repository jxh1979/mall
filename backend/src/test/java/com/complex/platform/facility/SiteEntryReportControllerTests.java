package com.complex.platform.facility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.facility.dto.SiteEntryReportAcceptanceRequest;
import com.complex.platform.facility.dto.SiteEntryReportRequest;
import com.complex.platform.facility.dto.SiteEntryReportReviewRequest;
import com.complex.platform.facility.dto.SiteEntryReportSubmitRequest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SiteEntryReportControllerTests {

    private SiteEntryReportService service;
    private AuthorizationService authorizationService;
    private SiteEntryReportController controller;
    private AccessActor outsourced;

    @BeforeEach
    void setUp() {
        service = mock(SiteEntryReportService.class);
        authorizationService = mock(AuthorizationService.class);
        controller = new SiteEntryReportController(service, authorizationService);
        outsourced = new AccessActor("usr_vendor_a", "vendor-a", "outsourced_vendor", "proj_1", null, "active");
        when(authorizationService.currentActor()).thenReturn(outsourced);
        when(authorizationService.isOutsourcedVendor(outsourced)).thenReturn(true);
    }

    @Test
    void outsourcedReadCreateAndSubmitUseAssignedFinePermissions() {
        when(service.list()).thenReturn(new PageResponse<>(List.of(), 1, 500, 0));

        controller.list();
        controller.get("ser_1");
        controller.create(createReq("k_create"));
        controller.submit("ser_1", submit("k_submit"));

        verify(authorizationService, times(2)).requirePermission("facility.site_entry.view_assigned");
        verify(authorizationService).requirePermission("facility.site_entry.create_assigned");
        verify(authorizationService).requirePermission("facility.site_entry.submit_assigned");
    }

    @Test
    void staffReadUsesReviewPermission() {
        AccessActor staff = new AccessActor("usr_ops", "ops", "staff", null, null, "active");
        when(authorizationService.currentActor()).thenReturn(staff);
        when(authorizationService.isOutsourcedVendor(staff)).thenReturn(false);
        when(service.list()).thenReturn(new PageResponse<>(List.of(), 1, 500, 0));

        controller.list();

        verify(authorizationService).requirePermission("facility.site_entry.review");
    }

    @Test
    void outsourcedReviewAndAcceptAreDeniedByPropertySidePermissions() {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied: facility.site_entry.review"))
                .when(authorizationService).requirePermission("facility.site_entry.review");
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied: facility.site_entry.accept"))
                .when(authorizationService).requirePermission("facility.site_entry.accept");

        assertThatThrownBy(() -> controller.review("ser_1", review("approve", "k_review")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
        assertThatThrownBy(() -> controller.accept("ser_1", accept("pass", "k_accept")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
    }

    private SiteEntryReportRequest createReq(String key) {
        return new SiteEntryReportRequest("proj_1", "sp_1", "装修施工",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-05"),
                5, "张三", "13800000000", key);
    }

    private SiteEntryReportSubmitRequest submit(String key) {
        return new SiteEntryReportSubmitRequest("进场", key);
    }

    private SiteEntryReportReviewRequest review(String decision, String key) {
        return new SiteEntryReportReviewRequest(decision, "ok", key);
    }

    private SiteEntryReportAcceptanceRequest accept(String result, String key) {
        return new SiteEntryReportAcceptanceRequest(result, "ok", key);
    }
}
