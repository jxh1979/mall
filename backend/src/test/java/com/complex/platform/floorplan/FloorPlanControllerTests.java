package com.complex.platform.floorplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.auth.AuthService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.GlobalExceptionHandler;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.config.SecurityConfig;
import com.complex.platform.floorplan.dto.FloorPlanImportCreateRequest;
import com.complex.platform.floorplan.dto.FloorPlanImportPublishRequest;
import com.complex.platform.floorplan.dto.FloorPlanCreateRequest;
import com.complex.platform.floorplan.dto.FloorPlanShapeSaveRequest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = FloorPlanController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, FloorPlanControllerTests.TestBeans.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class FloorPlanControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FloorPlanService service;

    @Autowired
    private AuthorizationService authorizationService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service, authorizationService);
    }

    @Test
    void unauthenticatedListReturnsUnauthorizedOrForbidden() throws Exception {
        int status = mockMvc.perform(get("/api/v1/floor-plans"))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status).isIn(401, 403);
    }

    @Test
    void createWithoutIdempotencyKeyReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/floor-plans")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "floor_no": "1F",
                                  "plan_name": "1F plan",
                                  "image_url": "https://assets.example.com/floor-1.png",
                                  "image_width": 1600,
                                  "image_height": 1200,
                                  "version_no": "v1",
                                  "status": "active"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createUsesManagePermissionAndReturnsFloorPlan() throws Exception {
        when(service.createFloorPlan(any(FloorPlanCreateRequest.class))).thenReturn(floorPlan());

        mockMvc.perform(post("/api/v1/floor-plans")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "floor-plan-create",
                                  "operator_note": "create floor plan",
                                  "project_id": "prj_1",
                                  "building_code": "A",
                                  "floor_no": "1F",
                                  "plan_name": "1F plan",
                                  "image_url": "https://assets.example.com/floor-1.png",
                                  "image_width": 1600,
                                  "image_height": 1200,
                                  "version_no": "v1",
                                  "status": "active"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("fp_1"))
                .andExpect(jsonPath("$.data.floor_no").value("1F"));

        verify(authorizationService).requirePermission("floor_plan.manage");
    }

    @Test
    void createPermissionDeniedReturnsForbidden() throws Exception {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("floor_plan.manage");

        mockMvc.perform(post("/api/v1/floor-plans")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "floor-plan-denied",
                                  "floor_no": "1F",
                                  "plan_name": "1F plan",
                                  "image_url": "https://assets.example.com/floor-1.png",
                                  "image_width": 1600,
                                  "image_height": 1200,
                                  "version_no": "v1",
                                  "status": "active"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void listUsesViewPermissionAndPageShape() throws Exception {
        when(service.listFloorPlans()).thenReturn(new PageResponse<>(List.of(floorPlan()), 1, 50, 1));

        mockMvc.perform(get("/api/v1/floor-plans")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value("fp_1"))
                .andExpect(jsonPath("$.data.total").value(1));

        verify(authorizationService).requirePermission("floor_plan.view");
    }

    @Test
    void listShapesUsesViewPermission() throws Exception {
        when(service.listShapes("fp_1")).thenReturn(new PageResponse<>(List.of(shape()), 1, 50, 1));

        mockMvc.perform(get("/api/v1/floor-plans/fp_1/shapes")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value("fps_1"));

        verify(authorizationService).requirePermission("floor_plan.view");
    }

    @Test
    void saveShapeUsesManagePermissionAndReturnsShape() throws Exception {
        when(service.saveShape(eq("fp_1"), any(FloorPlanShapeSaveRequest.class))).thenReturn(shape());

        mockMvc.perform(post("/api/v1/floor-plans/fp_1/shapes")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "shape-save",
                                  "operator_note": "save polygon",
                                  "shape_type": "shop",
                                  "ref_type": "space",
                                  "ref_id": "sp_1",
                                  "label": "Shop 101",
                                  "polygon_points": [
                                    {"x": 0.1, "y": 0.1},
                                    {"x": 0.4, "y": 0.1},
                                    {"x": 0.4, "y": 0.3}
                                  ],
                                  "z_index": 10,
                                  "status": "active"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("fps_1"))
                .andExpect(jsonPath("$.data.polygon_points[0].x").value(0.1));

        verify(authorizationService).requirePermission("floor_plan.manage");
    }

    @Test
    void createImportUsesImportManagePermissionAndReturnsTask() throws Exception {
        when(service.createImportTask(eq("fp_1"), any(FloorPlanImportCreateRequest.class))).thenReturn(importTask());

        mockMvc.perform(post("/api/v1/floor-plans/fp_1/imports")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "floor-import-create",
                                  "operator_note": "import svg",
                                  "source_format": "svg",
                                  "source_name": "1F.svg",
                                  "source_payload": "<svg viewBox='0 0 1000 800'><polygon id='A101' points='100,80 300,80 300,240 100,240' /></svg>"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("fpi_1"))
                .andExpect(jsonPath("$.data.status").value("parsed"))
                .andExpect(jsonPath("$.data.candidate_count").value(1));

        verify(authorizationService).requirePermission("floor_plan.import.manage");
    }

    @Test
    void listImportsUsesImportViewPermission() throws Exception {
        when(service.listImportTasks("fp_1")).thenReturn(new PageResponse<>(List.of(importTask()), 1, 50, 1));

        mockMvc.perform(get("/api/v1/floor-plans/fp_1/imports")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value("fpi_1"))
                .andExpect(jsonPath("$.data.total").value(1));

        verify(authorizationService).requirePermission("floor_plan.import.view");
    }

    @Test
    void listImportCandidatesUsesImportViewPermission() throws Exception {
        when(service.getImportDetails("fp_1", "fpi_1")).thenReturn(importDetails());

        mockMvc.perform(get("/api/v1/floor-plans/fp_1/imports/fpi_1/candidates")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.task.id").value("fpi_1"))
                .andExpect(jsonPath("$.data.candidates[0].id").value("fpic_1"));

        verify(authorizationService).requirePermission("floor_plan.import.view");
    }

    @Test
    void publishImportUsesImportManagePermissionAndRequiresIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/v1/floor-plans/fp_1/imports/fpi_1/publish")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operator_note": "publish import",
                                  "candidate_ids": ["fpic_1"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verify(authorizationService).requirePermission("floor_plan.import.manage");
    }

    @Test
    void publishImportUsesImportManagePermissionAndReturnsTask() throws Exception {
        when(service.publishImportTask(eq("fp_1"), eq("fpi_1"), any(FloorPlanImportPublishRequest.class))).thenReturn(publishedImportTask());

        mockMvc.perform(post("/api/v1/floor-plans/fp_1/imports/fpi_1/publish")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "floor-import-publish",
                                  "operator_note": "publish import",
                                  "candidate_ids": ["fpic_1"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("fpi_1"))
                .andExpect(jsonPath("$.data.status").value("published"));

        verify(authorizationService).requirePermission("floor_plan.import.manage");
    }

    private FloorPlanRecord floorPlan() {
        return new FloorPlanRecord(
                "fp_1",
                "prj_1",
                "A",
                "1F",
                "1F plan",
                "https://assets.example.com/floor-1.png",
                1600,
                1200,
                "v1",
                "active",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z"),
                null
        );
    }

    private FloorPlanShapeRecord shape() {
        return new FloorPlanShapeRecord(
                "fps_1",
                "fp_1",
                "shop",
                "space",
                "sp_1",
                "Shop 101",
                List.of(
                        new FloorPlanPoint(0.1, 0.1),
                        new FloorPlanPoint(0.4, 0.1),
                        new FloorPlanPoint(0.4, 0.3)
                ),
                10,
                "active",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        );
    }

    private FloorPlanImportTaskRecord importTask() {
        return new FloorPlanImportTaskRecord(
                "fpi_1",
                "fp_1",
                "svg",
                "1F.svg",
                "hash",
                "<svg />",
                "parsed",
                1,
                0,
                "import svg",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        );
    }

    private FloorPlanImportTaskRecord publishedImportTask() {
        return new FloorPlanImportTaskRecord(
                "fpi_1",
                "fp_1",
                "svg",
                "1F.svg",
                "hash",
                "<svg />",
                "published",
                1,
                0,
                "import svg",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        );
    }

    private FloorPlanImportCandidateRecord importCandidate() {
        return new FloorPlanImportCandidateRecord(
                "fpic_1",
                "fpi_1",
                1,
                "shop",
                "space",
                "sp_1",
                "Shop 101",
                List.of(
                        new FloorPlanPoint(0.1, 0.1),
                        new FloorPlanPoint(0.4, 0.1),
                        new FloorPlanPoint(0.4, 0.3)
                ),
                10,
                "pending",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        );
    }

    private FloorPlanImportDetailsResponse importDetails() {
        return new FloorPlanImportDetailsResponse(importTask(), List.of(importCandidate()), List.of());
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        FloorPlanService floorPlanService() {
            return Mockito.mock(FloorPlanService.class);
        }

        @Bean
        AuthorizationService authorizationService() {
            return Mockito.mock(AuthorizationService.class);
        }

        @Bean
        AuthService authService() {
            return new AuthService("", "");
        }
    }
}
