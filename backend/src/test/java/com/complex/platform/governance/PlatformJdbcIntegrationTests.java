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
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
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
                "governance.space.manage",
                "governance.supervisor_review.view",
                "governance.supervisor_review.manage",
                "governance.supervisor_review.approve"
        );
    }

    @Test
    void listMerchantsPagedReturnsRealTotalAndReachesNextPage() {
        // AUD-005 真分页: SQL COUNT + LIMIT/OFFSET(含 include_archived + delete_status 谓词), 第二页可达首页之后的商户。
        Instant t = Instant.now();
        repository.saveBusinessEntity(new BusinessEntityRecord(
                "be_pg", "PG Entity", "operation", null, null, null, null, "active", t, t));
        repository.saveMerchant(new MerchantRecord("mch_pg_1", "PG A", "other", "be_pg", "be_pg", "be_pg",
                "C1", "18800000001", "Owner", "1F-A", new java.math.BigDecimal("10.00"), new java.math.BigDecimal("9.00"),
                null, null, new java.math.BigDecimal("1.00"), "fixed_rent_plus_commission", "draft", t, t));
        repository.saveMerchant(new MerchantRecord("mch_pg_2", "PG B", "other", "be_pg", "be_pg", "be_pg",
                "C2", "18800000002", "Owner", "1F-B", new java.math.BigDecimal("10.00"), new java.math.BigDecimal("9.00"),
                null, null, new java.math.BigDecimal("1.00"), "fixed_rent_plus_commission", "draft",
                t.plusSeconds(1), t.plusSeconds(1)));
        var page1 = repository.listMerchantsPaged(true, null, 1, 1);
        var page2 = repository.listMerchantsPaged(true, null, 2, 1);
        assertThat(page1.total()).isGreaterThanOrEqualTo(2L);
        assertThat(page1.items()).hasSize(1);
        assertThat(page2.items()).hasSize(1);
        assertThat(page2.items()).extracting(MerchantRecord::id)
                .doesNotContainAnyElementsOf(page1.items().stream().map(MerchantRecord::id).toList());
        // V348 按业态查询走 SQL 谓词：两个 seed 商户业态都是「其他」→ 按「酒吧」筛应为空、按「其他」筛应命中。
        assertThat(repository.listMerchantsPaged(true, "bar", 1, 500).items())
                .extracting(MerchantRecord::id).doesNotContain("mch_pg_1", "mch_pg_2");
        assertThat(repository.listMerchantsPaged(true, "other", 1, 500).items())
                .extracting(MerchantRecord::id).contains("mch_pg_1", "mch_pg_2");
    }

    @Test
    void createAndListBusinessEntityPersistsFormalFields() {
        Instant createdAt = Instant.parse("2026-06-14T01:00:00Z");
        BusinessEntityRecord saved = repository.saveBusinessEntity(new BusinessEntityRecord(
                "be_jdbc_formal",
                "  P0 Platform Entity  ",
                "operation",
                null,
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
                "Owner Zhang",
                "1F-A",
                new BigDecimal("128.50"),
                new BigDecimal("120.00"),
                "Jack Contact",
                "18800000013",
                new BigDecimal("12.30"),
                "fixed_rent_plus_commission",
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
                "Owner Li",
                "1F-B",
                new BigDecimal("188.00"),
                new BigDecimal("170.00"),
                "Jill Contact",
                "18800000014",
                new BigDecimal("15.80"),
                "revenue_share",
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
                        MerchantRecord::ownerPerson,
                        MerchantRecord::mallBlockCode,
                        MerchantRecord::areaSquareMeter,
                        MerchantRecord::leasedAreaSquareMeter,
                        MerchantRecord::secondaryContactName,
                        MerchantRecord::secondaryContactPhone,
                        MerchantRecord::rentPerSquareMeter,
                        MerchantRecord::cooperationForm,
                        MerchantRecord::status
                )
                .contains(tuple(
                        "mch_jdbc_formal",
                        "P0 Merchant Updated",
                        "retail",
                        "be_jdbc_formal",
                        "18800000012",
                        "Owner Li",
                        "1F-B",
                        new BigDecimal("188.00"),
                        new BigDecimal("170.00"),
                        "Jill Contact",
                        "18800000014",
                        new BigDecimal("15.80"),
                        "revenue_share",
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
        assertThat(currentFlywayVersion()).isEqualTo(com.complex.platform.support.MigrationVersions.latest());
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

    // ── P0-A / ★GAP-07 / SEC-04（V339）：多主体联系电话 PII 落库加密 ──────────────────
    @Test
    void merchantContactPhonesArePersistedAsCiphertextAndReadBackAsPlaintext() {
        Instant t = Instant.now();
        repository.saveBusinessEntity(new BusinessEntityRecord(
                "be_pii", "PII Entity", "operation", null, null, null, null, "active", t, t));
        repository.saveMerchant(new MerchantRecord("mch_pii_1", "PII 商户", "other", "be_pii", "be_pii", "be_pii",
                "第一联系人", "18800000021", "负责人", "1F-P", new BigDecimal("10.00"), new BigDecimal("9.00"),
                "第二联系人", "18800000022", new BigDecimal("1.00"), "fixed_rent_plus_commission", "draft", t, t));

        Map<String, Object> raw = jdbcTemplate.queryForMap(
                "SELECT contact_phone, secondary_contact_phone FROM merchants WHERE id = 'mch_pii_1'");
        // 库里是 AES-GCM 信封密文，不是明文
        assertThat((String) raw.get("contact_phone")).startsWith("enc:v1:").doesNotContain("18800000021");
        assertThat((String) raw.get("secondary_contact_phone")).startsWith("enc:v1:").doesNotContain("18800000022");

        // 读回业务层仍是明文
        MerchantRecord loaded = repository.findMerchantById("mch_pii_1").orElseThrow();
        assertThat(loaded.contactPhone()).isEqualTo("18800000021");
        assertThat(loaded.secondaryContactPhone()).isEqualTo("18800000022");
    }

    @Test
    void merchantPhoneReSaveDoesNotDoubleEncrypt() {
        // 防二次加密：把读回来的（明文）记录原样再存一次，落库仍是可解密的单层密文。
        Instant t = Instant.now();
        repository.saveBusinessEntity(new BusinessEntityRecord(
                "be_pii2", "PII Entity 2", "operation", null, null, null, null, "active", t, t));
        repository.saveMerchant(new MerchantRecord("mch_pii_2", "PII 商户2", "other", "be_pii2", "be_pii2", "be_pii2",
                "联系人", "18800000023", "负责人", "1F-Q", new BigDecimal("10.00"), new BigDecimal("9.00"),
                null, null, new BigDecimal("1.00"), "fixed_rent_plus_commission", "draft", t, t));
        repository.saveMerchant(repository.findMerchantById("mch_pii_2").orElseThrow());

        assertThat(repository.findMerchantById("mch_pii_2").orElseThrow().contactPhone()).isEqualTo("18800000023");
    }

    @Test
    void plaintextPhoneWriteIsRejectedByDatabaseConstraint() {
        // DB 级边界（V249 思路）：任何绕开仓库的手写 SQL / 批量导入写明文，数据库直接拒。
        Instant t = Instant.now();
        repository.saveBusinessEntity(new BusinessEntityRecord(
                "be_pii3", "PII Entity 3", "operation", null, null, null, null, "active", t, t));
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO merchants (id, merchant_name, merchant_type, contact_phone, status, created_at, updated_at) "
                        + "VALUES ('mch_pii_plain', 'X', 'other', '18800000024', 'draft', now(), now())"))
                .hasMessageContaining("chk_merchants_contact_phone_pii");
    }

    @Test
    void v339SeedsContactPhonePlaintextViewPermission() {
        assertThat(permissionCodes()).contains("pii.contact_phone.view");
        assertThat(platformAdminPermissionCodes()).contains("pii.contact_phone.view");
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
