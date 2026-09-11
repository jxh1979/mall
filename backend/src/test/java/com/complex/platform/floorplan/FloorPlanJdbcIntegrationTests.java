package com.complex.platform.floorplan;
// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class FloorPlanJdbcIntegrationTests {

    @Autowired
    private JdbcFloorPlanRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
