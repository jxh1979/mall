package com.complex.platform.floorplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
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
                "polygon requires at least 3 points",
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
