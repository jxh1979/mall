package com.complex.platform.partner;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
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
class PartnerJdbcIntegrationTests {

    @Autowired
    private JdbcPartnerRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void orgSubordinateAndExpiryFlow() {
        Instant now = Instant.parse("2026-06-30T00:00:00Z");
        repository.saveOrg(new ExternalPartnerOrgRecord(
                "epo-it", "prj1", "维保甲", "facility_maintenance", null, 2, null, "active", now, now));
        assertThat(repository.listOrgs(null)).extracting(ExternalPartnerOrgRecord::id).contains("epo-it");
        // 3.2 跨项目读隔离：scoped 查询只见本项目，他项目 scope 看不见。
        assertThat(repository.listOrgs("prj1")).extracting(ExternalPartnerOrgRecord::id).contains("epo-it");
        assertThat(repository.listOrgs("prj-other")).extracting(ExternalPartnerOrgRecord::id).doesNotContain("epo-it");

        repository.createSubordinate(new PartnerSubordinateRecord(
                "epu-it-1", "sub-it-1", "epo-it", "outsourced_vendor", "active", now), "role_outsourced_facility");
        assertThat(repository.countActiveSubordinates("epo-it")).isEqualTo(1);
        assertThat(repository.findPartnerOrgIdForUser("epu-it-1")).contains("epo-it");
        assertThat(repository.listSubordinates("epo-it")).extracting(PartnerSubordinateRecord::username).contains("sub-it-1");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM user_roles WHERE user_id = 'epu-it-1' AND role_id = 'role_outsourced_facility'",
                Integer.class)).isEqualTo(1);

        repository.disableSubordinate("epu-it-1");
        assertThat(repository.countActiveSubordinates("epo-it")).isEqualTo(0);

        // 时效到期：机构过期 → 机构 expired + 下属 disabled。
        repository.saveOrg(new ExternalPartnerOrgRecord(
                "epo-exp", null, "过期机构", "construction", null, 5,
                Instant.parse("2020-01-01T00:00:00Z"), "active", now, now));
        repository.createSubordinate(new PartnerSubordinateRecord(
                "epu-exp-1", "sub-exp-1", "epo-exp", "outsourced_vendor", "active", now), "role_outsourced_construction");
        int disabled = repository.disableExpiredOrgsAndSubordinates(now);
        assertThat(disabled).isGreaterThanOrEqualTo(1);
        assertThat(repository.findOrgById("epo-exp")).get()
                .extracting(ExternalPartnerOrgRecord::status).isEqualTo("expired");
    }

    @Test
    void v186SeedsPartnerOrgPermissions() {
        assertThat(jdbcTemplate.queryForList(
                "SELECT permission_code FROM permissions WHERE permission_code LIKE 'partner.org.%'", String.class))
                .contains("partner.org.view", "partner.org.manage");
    }
}
