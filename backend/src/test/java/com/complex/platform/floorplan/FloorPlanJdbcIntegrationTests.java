package com.complex.platform.floorplan;
// 文件版本：V0.04

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class FloorPlanJdbcIntegrationTests {

    @Autowired
    private JdbcFloorPlanRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void migrationCreatesFloorPlanTablesAndSeedsPermissions() {
        assertThat(regclass("mall_floor_plans")).isEqualTo("mall_floor_plans");
        assertThat(regclass("floor_plan_shapes")).isEqualTo("floor_plan_shapes");
        assertThat(regclass("floor_plan_import_tasks")).isEqualTo("floor_plan_import_tasks");
        assertThat(regclass("floor_plan_import_candidates")).isEqualTo("floor_plan_import_candidates");
        assertThat(regclass("floor_plan_import_errors")).isEqualTo("floor_plan_import_errors");
        assertThat(currentFlywayVersion()).isEqualTo(com.complex.platform.support.MigrationVersions.latest());

        assertThat(permissionCodes()).contains(
                "floor_plan.view",
                "floor_plan.manage",
                "floor_plan.import.view",
                "floor_plan.import.manage"
        );
        assertThat(platformAdminPermissionCodes()).contains(
                "floor_plan.view",
                "floor_plan.manage",
                "floor_plan.import.view",
                "floor_plan.import.manage"
        );
    }

    /** 第十八批 AG 段 259-R7（OpenAPI 与代码一致：FloorPlanCreateRequest 的 project_id 进 required）：新建平面图不带 project_id → 400「项目未填写」；带齐 → 200。 */
    @Test
    void createFloorPlanRequiresProjectId() throws Exception {
        String n = String.valueOf(System.nanoTime());
        String project = "prj_259_fp_" + n;
        jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '平面图必填回归', 'active', now(), now())", project);
        RequestPostProcessor admin = new StaffRequestFixture(jdbcTemplate).staff("usr_259_fp_" + n, "platform_admin", null, null,
                "floor_plan.manage");
        String body = """
                {"idempotency_key":"fp-259-%s-%s",%s"floor_no":"B1","plan_name":"负一层平面图","image_url":"https://example.invalid/b1.png",
                 "image_width":1200,"image_height":800,"version_no":"v1","status":"active"}""";

        mockMvc.perform(post("/api/v1/floor-plans").with(admin).contentType(MediaType.APPLICATION_JSON).content(body.formatted("none", n, "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("项目未填写"));
        mockMvc.perform(post("/api/v1/floor-plans").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted("ok", n, "\"project_id\":\"" + project + "\",")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.project_id").value(project));
    }

    @Test
    void saveAndListFloorPlansAndShapesThroughPostgres() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveFloorPlan(new FloorPlanRecord(
                "fp_jdbc",
                "prj_jdbc",
                "A",
                "1F",
                "1F plan",
                "https://assets.example.com/floor-1.png",
                1600,
                1200,
                "v1",
                "active",
                now,
                now,
                null
        ));
        repository.saveShape(new FloorPlanShapeRecord(
                "fps_jdbc",
                "fp_jdbc",
                "shop",
                "space",
                "sp_jdbc",
                "Shop 101",
                List.of(
                        new FloorPlanPoint(0.1, 0.1),
                        new FloorPlanPoint(0.4, 0.1),
                        new FloorPlanPoint(0.4, 0.3),
                        new FloorPlanPoint(0.1, 0.3)
                ),
                10,
                "active",
                now,
                now
        ));

        assertThat(repository.findFloorPlanById("fp_jdbc"))
                .get()
                .extracting(FloorPlanRecord::floorNo, FloorPlanRecord::imageWidth, FloorPlanRecord::imageHeight)
                .containsExactly("1F", 1600, 1200);
        assertThat(repository.findAllFloorPlans())
                .extracting(FloorPlanRecord::id, FloorPlanRecord::planName, FloorPlanRecord::status)
                .contains(tuple("fp_jdbc", "1F plan", "active"));
        assertThat(repository.findShapesByFloorPlanId("fp_jdbc"))
                .extracting(FloorPlanShapeRecord::id, FloorPlanShapeRecord::shapeType, FloorPlanShapeRecord::refType)
                .contains(tuple("fps_jdbc", "shop", "space"));
        assertThat(repository.findShapesByFloorPlanId("fp_jdbc").getFirst().polygonPoints())
                .containsExactly(
                        new FloorPlanPoint(0.1, 0.1),
                        new FloorPlanPoint(0.4, 0.1),
                        new FloorPlanPoint(0.4, 0.3),
                        new FloorPlanPoint(0.1, 0.3)
                );
    }

    @Test
    void saveAndListImportTasksCandidatesAndErrorsThroughPostgres() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveFloorPlan(new FloorPlanRecord(
                "fp_import_jdbc",
                "prj_jdbc",
                "A",
                "1F",
                "1F plan",
                "https://assets.example.com/floor-1.png",
                1600,
                1200,
                "v1",
                "active",
                now,
                now,
                null
        ));
        repository.saveImportTask(new FloorPlanImportTaskRecord(
                "fpi_jdbc",
                "fp_import_jdbc",
                "svg",
                "1F.svg",
                "hash",
                "<svg />",
                "parsed",
                1,
                1,
                "import svg",
                now,
                now
        ));
        repository.saveImportCandidate(new FloorPlanImportCandidateRecord(
                "fpic_jdbc",
                "fpi_jdbc",
                1,
                "shop",
                "space",
                "sp_jdbc",
                "Shop 101",
                List.of(
                        new FloorPlanPoint(0.1, 0.1),
                        new FloorPlanPoint(0.4, 0.1),
                        new FloorPlanPoint(0.4, 0.3),
                        new FloorPlanPoint(0.1, 0.3)
                ),
                10,
                "pending",
                now,
                now
        ));
        repository.saveImportError(new FloorPlanImportErrorRecord(
                "fpie_jdbc",
                "fpi_jdbc",
                1,
                "points",
                "POINTS_TOO_FEW",
                "多边形至少需要 3 个顶点",
                now
        ));

        assertThat(repository.findImportTasksByFloorPlanId("fp_import_jdbc"))
                .extracting(FloorPlanImportTaskRecord::id, FloorPlanImportTaskRecord::status)
                .contains(tuple("fpi_jdbc", "parsed"));
        assertThat(repository.findImportCandidatesByTaskId("fpi_jdbc"))
                .extracting(FloorPlanImportCandidateRecord::id, FloorPlanImportCandidateRecord::label)
                .contains(tuple("fpic_jdbc", "Shop 101"));
        assertThat(repository.findImportErrorsByTaskId("fpi_jdbc"))
                .extracting(FloorPlanImportErrorRecord::errorCode)
                .containsExactly("POINTS_TOO_FEW");
    }

    // M016-002：库层 shape_type 闭集须含治理地图「摊位」「公共区」（V423 前 CHECK 六值 → 此例红）
    @Test
    void shapeTypeBoothAndPublicAreaAreAcceptedByDatabase() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveFloorPlan(floorPlan("fp_shape_types", "prj_jdbc", now));
        repository.saveShape(shape("fps_booth", "fp_shape_types", "booth", now));
        repository.saveShape(shape("fps_public_area", "fp_shape_types", "public_area", now));

        assertThat(repository.findShapesByFloorPlanId("fp_shape_types"))
                .extracting(FloorPlanShapeRecord::shapeType)
                .containsExactlyInAnyOrder("booth", "public_area");
    }

    // M016-001：mall_floor_plans.project_id 列级不得为空（V423 前可空 → 此例红）
    @Test
    void floorPlanWithoutProjectIsRejectedByDatabase() {
        assertThatThrownBy(() -> repository.saveFloorPlan(
                floorPlan("fp_no_project", null, Instant.parse("2026-06-14T00:00:00Z"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // M017-007：projectExists 走真实 projects 表（内存仓夹具掩盖不了 SQL）
    @Test
    void projectExistsQueriesRealProjectsTable() {
        assertThat(repository.projectExists("prj_jdbc_missing")).isFalse();
        jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                + "VALUES ('prj_jdbc_real', 'mall', '测试综合体', 'active', now(), now())");

        assertThat(repository.projectExists("prj_jdbc_real")).isTrue();
    }

    // M017-005：导入候选表的 shape_type 八值 CHECK（V423）——booth/public_area 可落库、非闭集值被拒（M016-004 观察）
    @Test
    void importCandidateShapeTypeFollowsDatabaseCheck() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveFloorPlan(floorPlan("fp_cand_types", "prj_jdbc", now));
        repository.saveImportTask(new FloorPlanImportTaskRecord("fpi_cand_types", "fp_cand_types", "svg", "1F.svg", "hash",
                "<svg />", "parsed", 2, 0, null, now, now));
        repository.saveImportCandidate(candidate("fpic_cand_booth", "fpi_cand_types", 1, "booth", now));
        repository.saveImportCandidate(candidate("fpic_cand_public", "fpi_cand_types", 2, "public_area", now));

        assertThat(repository.findImportCandidatesByTaskId("fpi_cand_types"))
                .extracting(FloorPlanImportCandidateRecord::shapeType)
                .containsExactlyInAnyOrder("booth", "public_area");
        assertThatThrownBy(() -> repository.saveImportCandidate(candidate("fpic_cand_corridor", "fpi_cand_types", 3, "corridor", now)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static FloorPlanImportCandidateRecord candidate(String id, String taskId, int no, String shapeType, Instant now) {
        return new FloorPlanImportCandidateRecord(id, taskId, no, shapeType, "space", null, shapeType + " candidate",
                List.of(new FloorPlanPoint(0.1, 0.1), new FloorPlanPoint(0.4, 0.1), new FloorPlanPoint(0.4, 0.3)),
                0, "pending", now, now);
    }

    private static FloorPlanRecord floorPlan(String id, String projectId, Instant now) {
        return new FloorPlanRecord(id, projectId, "A", "1F", "1F plan", "https://assets.example.com/floor-1.png",
                1600, 1200, "v1", "active", now, now, null);
    }

    private static FloorPlanShapeRecord shape(String id, String floorPlanId, String shapeType, Instant now) {
        return new FloorPlanShapeRecord(id, floorPlanId, shapeType, "space", null, shapeType + " shape",
                List.of(new FloorPlanPoint(0.1, 0.1), new FloorPlanPoint(0.4, 0.1), new FloorPlanPoint(0.4, 0.3)),
                0, "active", now, now);
    }

    private String regclass(String tableName) {
        return jdbcTemplate.queryForObject("SELECT to_regclass('public." + tableName + "')::text", String.class);
    }

    private String currentFlywayVersion() {
        return jdbcTemplate.queryForObject("""
                SELECT version
                  FROM flyway_schema_history
                 WHERE success = true
                 ORDER BY installed_rank DESC
                 LIMIT 1
                """, String.class);
    }

    private List<String> permissionCodes() {
        return jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM permissions
                 ORDER BY permission_code
                """, String.class);
    }

    private List<String> platformAdminPermissionCodes() {
        return jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM role_permissions
                 WHERE role_id = 'role_platform_admin'
                 ORDER BY permission_code
                """, String.class);
    }
}
