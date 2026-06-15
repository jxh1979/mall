package com.complex.platform.governance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
class PlatformJdbcIntegrationTests {

    @Autowired
    private JdbcPlatformRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationCreatesPlatformMasterDataTablesAndSeedsPermissions() {
        assertThat(regclass("business_entities")).isEqualTo("business_entities");
        assertThat(regclass("merchants")).isEqualTo("merchants");
        assertThat(regclass("projects")).isEqualTo("projects");
        assertThat(regclass("spaces")).isEqualTo("spaces");

        assertThat(permissionCodes()).contains(
                "entity.binding.view",
                "entity.binding.manage",
                "merchant.profile.view",
                "merchant.profile.manage",
                "governance.project.view",
                "governance.project.manage",
                "governance.space.view",
                "governance.space.manage"
        );
        assertThat(platformAdminPermissionCodes()).contains(
                "entity.binding.manage",
                "merchant.profile.manage",
                "governance.project.manage",
                "governance.space.manage"
        );
    }

    @Test
    void createAndListBusinessEntityPersistsFormalFields() {
        Instant createdAt = Instant.parse("2026-06-14T01:00:00Z");
        BusinessEntityRecord saved = repository.saveBusinessEntity(new BusinessEntityRecord(
                "be_jdbc_formal",
                "  P0 Platform Entity  ",
                "operation",
                "91310000MA1P0",
                "Jane Ops",
                "vault://bank/entity",
                "active",
                createdAt,
                createdAt
        ));

        assertThat(saved.id()).isEqualTo("be_jdbc_formal");
        assertThat(repository.listBusinessEntities())
                .extracting(
                        BusinessEntityRecord::id,
                        BusinessEntityRecord::entityName,
                        BusinessEntityRecord::entityRole,
                        BusinessEntityRecord::unifiedSocialCreditCode,
                        BusinessEntityRecord::bankAccountRef,
                        BusinessEntityRecord::status
                )
                .contains(tuple(
                        "be_jdbc_formal",
                        "  P0 Platform Entity  ",
                        "operation",
                        "91310000MA1P0",
                        "vault://bank/entity",
                        "active"
                ));
    }

    @Test
    void createUpdateAndListMerchantPersistsFormalFields() {
        Instant createdAt = Instant.parse("2026-06-14T02:00:00Z");
        repository.saveMerchant(new MerchantRecord(
                "mch_jdbc_formal",
                "P0 Merchant",
                "other",
                "be_jdbc_formal",
                "be_contract",
                "be_settlement",
                "Jane Contact",
                "18800000011",
                "draft",
                createdAt,
                createdAt
        ));

        MerchantRecord updated = repository.saveMerchant(new MerchantRecord(
                "mch_jdbc_formal",
                "P0 Merchant Updated",
                "retail",
                "be_jdbc_formal",
                "be_contract",
                "be_settlement",
                "Jane Contact",
                "18800000012",
                "active",
                createdAt,
                Instant.parse("2026-06-14T03:00:00Z")
        ));

        assertThat(updated.id()).isEqualTo("mch_jdbc_formal");
        assertThat(repository.listMerchants())
                .extracting(
                        MerchantRecord::id,
                        MerchantRecord::merchantName,
                        MerchantRecord::merchantType,
                        MerchantRecord::businessEntityId,
                        MerchantRecord::contactPhone,
                        MerchantRecord::status
                )
                .contains(tuple(
                        "mch_jdbc_formal",
                        "P0 Merchant Updated",
                        "retail",
                        "be_jdbc_formal",
                        "18800000012",
                        "active"
                ));
    }

    @Test
    void createProjectAndSpacePersistsHierarchyFields() {
        Instant createdAt = Instant.parse("2026-06-14T04:00:00Z");
        repository.saveProject(new ProjectRecord(
                "prj_jdbc_formal",
                "mch_jdbc_formal",
                "complex",
                "P0 Complex",
                "P0-CX",
                "1F",
                120,
                "active",
                createdAt,
                createdAt
        ));

        SpaceRecord parent = repository.saveSpace(new SpaceRecord(
                "sp_jdbc_parent",
                "prj_jdbc_formal",
                "mch_jdbc_formal",
                null,
                "P0 Floor",
                "floor",
                "1F",
                BigDecimal.valueOf(1000),
                List.of(Map.of("x", 0, "y", 0)),
                "active",
                createdAt,
                createdAt
        ));
        repository.saveSpace(new SpaceRecord(
                "sp_jdbc_child",
                "prj_jdbc_formal",
                "mch_jdbc_formal",
                parent.id(),
                "P0 Shop",
                "shop",
                "1F-01",
                BigDecimal.valueOf(80.50),
                List.of(Map.of("x", 1, "y", 1)),
                "active",
                createdAt,
                createdAt
        ));

        assertThat(repository.listProjects())
                .extracting(ProjectRecord::id, ProjectRecord::merchantId, ProjectRecord::venueType, ProjectRecord::name)
                .contains(tuple("prj_jdbc_formal", "mch_jdbc_formal", "complex", "P0 Complex"));
        assertThat(repository.listSpaces())
                .extracting(SpaceRecord::id, SpaceRecord::projectId, SpaceRecord::merchantId, SpaceRecord::parentSpaceId, SpaceRecord::spaceName, SpaceRecord::spaceType)
                .contains(tuple("sp_jdbc_child", "prj_jdbc_formal", "mch_jdbc_formal", "sp_jdbc_parent", "P0 Shop", "shop"));
    }

    @Test
    void residualMigrationCreatesBindingContractTablesAndSeedsPermissions() {
        assertThat(regclass("business_entity_bindings")).isEqualTo("business_entity_bindings");
        assertThat(regclass("contracts")).isEqualTo("contracts");
        assertThat(currentFlywayVersion()).isEqualTo("039");
        assertThat(permissionCodes()).contains(
                "contract.view",
                "contract.draft.manage",
                "contract.lifecycle.approve"
        );
        assertThat(platformAdminPermissionCodes()).contains(
                "contract.view",
                "contract.draft.manage",
                "contract.lifecycle.approve"
        );
    }

    @Test
    void bindingRepositoryPreventsSecondActiveBindingForSameMerchantAndKeepsHistory() {
        Instant createdAt = Instant.parse("2026-06-14T05:00:00Z");
        repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(
                "00000000-0000-0000-0000-000000000101",
                "mch_binding_jdbc",
                "be_operation_1",
                "be_contract_1",
                "be_settlement_1",
                "active",
                LocalDate.parse("2026-06-14"),
                null,
                "initial",
                createdAt,
                createdAt
        ));

        BusinessEntityBindingRecord inactive = repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(
                "00000000-0000-0000-0000-000000000101",
                "mch_binding_jdbc",
                "be_operation_1",
                "be_contract_1",
                "be_settlement_1",
                "inactive",
                LocalDate.parse("2026-06-14"),
                null,
                "contract changed",
                createdAt,
                createdAt.plusSeconds(60)
        ));

        assertThat(inactive.status()).isEqualTo("inactive");
        assertThat(repository.listBusinessEntityBindings())
                .extracting(BusinessEntityBindingRecord::id, BusinessEntityBindingRecord::status)
                .contains(tuple("00000000-0000-0000-0000-000000000101", "inactive"));

        repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(
                "00000000-0000-0000-0000-000000000102",
                "mch_binding_jdbc",
                "be_operation_2",
                "be_contract_2",
                "be_settlement_2",
                "active",
                LocalDate.parse("2026-06-15"),
                null,
                "replacement active",
                createdAt,
                createdAt
        ));

        assertThatThrownBy(() -> repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(
                "00000000-0000-0000-0000-000000000103",
                "mch_binding_jdbc",
                "be_operation_3",
                "be_contract_3",
                "be_settlement_3",
                "active",
                LocalDate.parse("2026-06-16"),
                null,
                "second active",
                createdAt,
                createdAt
        ))).hasMessageContaining("uq_business_entity_bindings_active_merchant");
    }

    @Test
    void contractRepositoryPersistsDraftAttachmentsAndApprovalStatus() {
        Instant createdAt = Instant.parse("2026-06-14T06:00:00Z");
        repository.saveContract(new ContractRecord(
                "ctr_jdbc",
                "mch_contract_jdbc",
                "be_contract",
                "be_operation",
                "be_settlement",
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2027-06-30"),
                Map.of("monthly_rent_cent", 100000),
                BigDecimal.valueOf(12.50),
                List.of("att_contract_pdf", "att_appendix"),
                "draft",
                createdAt,
                createdAt
        ));
        repository.saveContract(new ContractRecord(
                "ctr_jdbc",
                "mch_contract_jdbc",
                "be_contract",
                "be_operation",
                "be_settlement",
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2027-06-30"),
                Map.of("monthly_rent_cent", 100000),
                BigDecimal.valueOf(12.50),
                List.of("att_contract_pdf", "att_appendix"),
                "approved",
                createdAt,
                createdAt.plusSeconds(60)
        ));

        assertThat(repository.findContractById("ctr_jdbc"))
                .get()
                .satisfies(contract -> {
                    assertThat(contract.lifecycleStatus()).isEqualTo("approved");
                    assertThat(contract.attachmentIds()).containsExactly("att_contract_pdf", "att_appendix");
                    assertThat(contract.effectiveDate()).isEqualTo(LocalDate.parse("2026-07-01"));
                });
    }

    private String regclass(String tableName) {
        return jdbcTemplate.queryForObject("SELECT to_regclass('public." + tableName + "')::text", String.class);
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

    private String currentFlywayVersion() {
        return jdbcTemplate.queryForObject("""
                SELECT version
                  FROM flyway_schema_history
                 WHERE success = TRUE
                 ORDER BY installed_rank DESC
                 LIMIT 1
                """, String.class);
    }
}
