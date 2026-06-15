package com.complex.platform.amusement;

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
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
@Transactional
class AmusementSafetyJdbcIntegrationTests {

    @Autowired
    private JdbcAmusementSafetyRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationCreatesAmusementSafetyTablesAndSeedsPermissions() {
        assertThat(regclass("amusement_safety_notices")).isEqualTo("amusement_safety_notices");
        assertThat(regclass("amusement_waiver_acknowledgements")).isEqualTo("amusement_waiver_acknowledgements");
        assertThat(regclass("amusement_incidents")).isEqualTo("amusement_incidents");
        assertThat(regclass("amusement_incident_actions")).isEqualTo("amusement_incident_actions");
        assertThat(currentFlywayVersion()).isEqualTo("039");

        assertThat(permissionCodes()).contains(
                "amusement.safety_notice.view",
                "amusement.safety_notice.manage",
                "amusement.waiver.acknowledge",
                "amusement.incident.view",
                "amusement.incident.create",
                "amusement.incident.process",
                "amusement.incident.close"
        );
        assertThat(platformAdminPermissionCodes()).contains(
                "amusement.safety_notice.manage",
                "amusement.waiver.acknowledge",
                "amusement.incident.create",
                "amusement.incident.process",
                "amusement.incident.close"
        );
    }

    @Test
    void saveAndListSafetyNoticeAcknowledgementIncidentAndActions() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveSafetyNotice(new AmusementSafetyNoticeRecord(
                "asn_jdbc",
                "prj_1",
                "amu_project_1",
                "safety_notice",
                "Rider safety",
                "v1",
                "Read before riding",
                "active",
                now,
                null,
                "staff_ops",
                List.of("att-notice"),
                now,
                now
        ));
        repository.saveWaiverAcknowledgement(new AmusementWaiverAcknowledgementRecord(
                "awa_jdbc",
                "asn_jdbc",
                "prj_1",
                "amu_project_1",
                "session_1",
                "customer_1",
                "Visitor Zhang",
                "Parent Zhang",
                "onsite_tablet",
                "v1",
                "sha256:notice-v1",
                now.plusSeconds(60),
                "staff_gate",
                List.of("att-signature"),
                now
        ));
        repository.saveIncident(new AmusementIncidentRecord(
                "ami_jdbc",
                "prj_1",
                "amu_project_1",
                "session_1",
                "device_1",
                "customer_1",
                "high",
                "created",
                "Guest slipped",
                "Guest slipped near entrance",
                "staff_reporter",
                "staff_app",
                now.plusSeconds(120),
                null,
                null,
                List.of("att-photo"),
                now,
                now
        ));
        repository.saveIncidentAction(new AmusementIncidentActionRecord(
                "amia_jdbc",
                "ami_jdbc",
                "created",
                null,
                "created",
                "staff_reporter",
                "created",
                List.of("att-photo"),
                now
        ));

        assertThat(repository.findAllSafetyNotices())
                .extracting(AmusementSafetyNoticeRecord::id, AmusementSafetyNoticeRecord::title)
                .contains(tuple("asn_jdbc", "Rider safety"));
        assertThat(repository.findSafetyNoticeById("asn_jdbc"))
                .get()
                .extracting(AmusementSafetyNoticeRecord::contentVersion, AmusementSafetyNoticeRecord::status)
                .containsExactly("v1", "active");
        assertThat(repository.findAllIncidents())
                .extracting(AmusementIncidentRecord::id, AmusementIncidentRecord::severity, AmusementIncidentRecord::status)
                .contains(tuple("ami_jdbc", "high", "created"));
        assertThat(repository.findIncidentActionsByIncidentId("ami_jdbc"))
                .extracting(AmusementIncidentActionRecord::id, AmusementIncidentActionRecord::actionType)
                .contains(tuple("amia_jdbc", "created"));
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
