package com.complex.platform.governance;
// 文件版本：V0.10

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
import org.springframework.dao.DataIntegrityViolationException;
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
    @Autowired private PlatformService platformService;
    @Autowired private org.springframework.transaction.PlatformTransactionManager transactionManager;


    @Test
    void merchantContactNamesAreEncryptedAtRestAndRoundTrip() {
        String[][] names = {{"张".repeat(120), "李".repeat(120)}, {"王敏", "赵宁"}, {null, ""}};
        for (String[] pair : names) {
            repository.saveMerchant(new MerchantRecord(
                    "mch_m009_name_cipher", "姓名加密测试商户", "other", null, null, null,
                    pair[0], null, null, null, null, null, pair[1], null, null, null,
                    "active", Instant.EPOCH, Instant.EPOCH));
            Map<String, Object> stored = jdbcTemplate.queryForMap(
                    "SELECT contact_name, secondary_contact_name FROM merchants WHERE id = ?",
                    "mch_m009_name_cipher");
            for (int i = 0; i < pair.length; i++) {
                String ciphertext = (String) stored.get(i == 0 ? "contact_name" : "secondary_contact_name");
                if (pair[i] == null || pair[i].isEmpty()) {
                    assertThat(ciphertext).isEqualTo(pair[i]);
                } else {
                    assertThat(ciphertext).startsWith("enc:v1:").doesNotContain(pair[i]);
                }
            }
            MerchantRecord loaded = repository.findMerchantById("mch_m009_name_cipher").orElseThrow();
            assertThat(loaded.contactName()).isEqualTo(pair[0]);
            assertThat(loaded.secondaryContactName()).isEqualTo(pair[1]);
        }
    }

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
                "be_pg", "PG Entity", "management_company", null, null, null, "active", t, t)); // M009-012：主体记录不再带 parent 链；M010-003：operation 已出白名单
        repository.saveMerchant(new MerchantRecord("mch_pg_1", "PG A", "other", "be_pg", "be_pg", "be_pg",
                "C1", "18800000001", "Owner", "1F-A", new java.math.BigDecimal("10.00"), new java.math.BigDecimal("9.00"),
                null, null, new java.math.BigDecimal("1.00"), "lease", "active", t, t));
        repository.saveMerchant(new MerchantRecord("mch_pg_2", "PG B", "other", "be_pg", "be_pg", "be_pg",
                "C2", "18800000002", "Owner", "1F-B", new java.math.BigDecimal("10.00"), new java.math.BigDecimal("9.00"),
                null, null, new java.math.BigDecimal("1.00"), "lease", "active",
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
                "management_company",
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
                        "management_company",
                        "91310000MA1P0",
                        "vault://bank/entity",
                        "active"
                ));
    }

    // M011-002：夹具只用服务层白名单内的合作方式/商户状态（BEQR-148 六类、BEQR-155），更新路径 lease→joint_operation 证明列可改。
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
                "lease",
                "active",
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
                "joint_operation",
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
                        "joint_operation",
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

        // 按本夹具项目读取，避免全库前 500 条截断固定历史日期的记录。
        assertThat(repository.listProjectsPaged("prj_jdbc_formal", 1, 500).items())
                .extracting(ProjectRecord::id, ProjectRecord::merchantId, ProjectRecord::venueType, ProjectRecord::name)
                .containsExactly(tuple("prj_jdbc_formal", "mch_jdbc_formal", "complex", "P0 Complex"));
        assertThat(repository.listSpacesPaged("prj_jdbc_formal", 1, 500).items())
                .extracting(SpaceRecord::id, SpaceRecord::projectId, SpaceRecord::merchantId, SpaceRecord::parentSpaceId, SpaceRecord::spaceName, SpaceRecord::spaceType)
                .containsExactlyInAnyOrder(
                        tuple("sp_jdbc_parent", "prj_jdbc_formal", "mch_jdbc_formal", null, "P0 Floor", "floor"),
                        tuple("sp_jdbc_child", "prj_jdbc_formal", "mch_jdbc_formal", "sp_jdbc_parent", "P0 Shop", "shop"));
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


    // M009-010：软删除行保留可查（includeArchived=true 含已审批删除行），默认列表不显示（BSM-004）。
    @Test
    void approvedDeletedMerchantStaysQueryableWithIncludeArchived() {
        Instant t = Instant.now();
        repository.saveMerchant(new MerchantRecord("mch_soft_del", "软删商户", "other", null, null, null,
                null, null, null, null, null, null, null, null, null, null, "active", t, t));
        repository.deleteMerchant("mch_soft_del");

        assertThat(repository.findMerchantById("mch_soft_del")).isPresent()
                .get().extracting(MerchantRecord::deleteStatus).isEqualTo("approved");
        assertThat(repository.listMerchants(true)).extracting(MerchantRecord::id).contains("mch_soft_del");
        assertThat(repository.listMerchantsPaged(true, null, 1, 500).items()).extracting(MerchantRecord::id).contains("mch_soft_del");
        assertThat(repository.listMerchants(false)).extracting(MerchantRecord::id).doesNotContain("mch_soft_del");
        assertThat(repository.listMerchantsPaged(false, null, 1, 500).items()).extracting(MerchantRecord::id).doesNotContain("mch_soft_del");
        // M009-006：主体存在性查询按主键。
        assertThat(repository.businessEntityExists("be_definitely_missing")).isFalse();
    }

    // M009-027：空间存在性按主键查询，不受 listSpaces() LIMIT 500 截断（上级空间/招商意向空间校验）。
    @Test
    void findSpaceByIdIsNotTruncatedByListLimit() {
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        for (int i = 0; i < 501; i++) {
            Instant t = base.plusSeconds(i);
            repository.saveSpace(new SpaceRecord("sp_limit_" + i, null, null, null, "空间" + i, "shop", "1F",
                    null, List.of(), "active", t, t));
        }
        assertThat(repository.listSpaces()).hasSize(500);
        assertThat(repository.findSpaceById("sp_limit_0")).isPresent();
    }

    // M016-003：空间类型字典 status 只允许 active/disabled（V423 前无 CHECK → 此例红）
    @Test
    void spaceTypeDictionaryRejectsUnknownStatus() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        assertThatThrownBy(() -> repository.saveSpaceType(
                new SpaceTypeRecord("sptype_jdbc_bad", "jdbc_bad", "字典状态测试", 100, "Active", now, now)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }


    @Test
    void approvedProjectStorageExistsWithoutHistoricalBackfill() {
        assertThat(jdbcTemplate.queryForList("""
                SELECT column_name FROM information_schema.columns
                 WHERE table_schema = 'public' AND table_name = 'projects'
                """, String.class)).contains("owner_person", "address", "business_scope");
        assertThat(jdbcTemplate.queryForList("""
                SELECT column_name FROM information_schema.columns
                 WHERE table_schema = 'public' AND table_name = 'merchants'
                """, String.class)).contains("project_id");
        assertThat(regclass("business_entity_projects")).isEqualTo("business_entity_projects");
    }

    @Test
    void onlyEndedInactiveBindingsStopOccupyingMerchant() {
        String id = "00000000-0000-0000-0000-000000009045";
        String merchant = "m009_reference_period";
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);
        repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(id, merchant,
                "operation", "contract", "settlement", "inactive", today.minusDays(3), today.minusDays(1),
                "历史期间结束", now, now));
        assertThat(repository.countMerchantReferences(merchant)).isZero();
        for (String status : List.of("inactive", "suspended", "active")) {
            repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(id, merchant,
                    "operation", "contract", "settlement", status, today.plusDays(2), null,
                    "尚未结束", now, now));
            assertThat(repository.countMerchantReferences(merchant)).isEqualTo(1);
        }
        repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(id, merchant,
                "operation", "contract", "settlement", "inactive", today.plusDays(2), today.plusDays(3),
                "未来结束", now, now));
        assertThat(repository.countMerchantReferences(merchant)).isEqualTo(1);
        repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(id, merchant,
                "operation", "contract", "settlement", "inactive", today, today.minusDays(1),
                "倒置期间不算真正结束", now, now));
        assertThat(repository.countMerchantReferences(merchant)).isEqualTo(1);
    }


    private void seedProjectScopeRows() {
        Instant now = Instant.now();
        for (String key : List.of("a", "b")) {
            repository.saveProject(new ProjectRecord("m009_project_" + key, null, "complex", "项目" + key,
                    null, null, null, "active", now, now, "负责人" + key, "地址" + key, "范围" + key));
            repository.saveBusinessEntity(new BusinessEntityRecord("m009_company_" + key, "公司" + key,
                    "management_company", null, null, null, "active", now, now));
            repository.associateBusinessEntityProject("m009_company_" + key, "m009_project_" + key);
            repository.saveMerchant(new MerchantRecord("m009_merchant_" + key, "商户" + key, "other",
                    "m009_company_" + key, null, null, null, null, null, null, null, null, null, null, null,
                    null, "active", now, now).withProject("m009_project_" + key));
            repository.saveSpace(new SpaceRecord("m009_space_" + key, "m009_project_" + key, "m009_merchant_" + key,
                    null, "空间" + key, "shop", null, null, List.of(), "active", now, now));
            repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(
                    "00000000-0000-0000-0000-0000000090" + ("a".equals(key) ? "81" : "82"),
                    "m009_merchant_" + key, "m009_company_" + key, "m009_company_" + key, "m009_company_" + key,
                    "inactive", LocalDate.now().plusYears(1), null, "待生效", now, now));
        }
    }

    @Test
    void scopedSqlFiltersAllFiveTotalsAndSupportsCompanyManyToMany() {
        seedProjectScopeRows();
        String a = "m009_project_a";
        assertThat(repository.listProjectsPaged(a, 1, 1).total()).isEqualTo(1);
        assertThat(repository.listProjectsPaged(a, 1, 1).items()).singleElement().satisfies(project -> {
            assertThat(project.ownerPerson()).isEqualTo("负责人a");
            assertThat(project.address()).isEqualTo("地址a");
            assertThat(project.businessScope()).isEqualTo("范围a");
        });
        assertThat(repository.listMerchantsPaged(a, true, null, 1, 1).total()).isEqualTo(1);
        assertThat(repository.listMerchantsPaged(a, true, null, 1, 1).items()).extracting(MerchantRecord::id)
                .containsExactly("m009_merchant_a");
        assertThat(repository.listSpacesPaged(a, 1, 1).total()).isEqualTo(1);
        assertThat(repository.listSpacesPaged(a, 1, 1).items()).extracting(SpaceRecord::id).containsExactly("m009_space_a");
        assertThat(repository.listBusinessEntityBindingsPaged(a, 1, 1).total()).isEqualTo(1);
        assertThat(repository.listBusinessEntityBindingsPaged(a, 1, 1).items())
                .extracting(BusinessEntityBindingRecord::merchantId).containsExactly("m009_merchant_a");
        assertThat(repository.listBusinessEntitiesPaged(a, 1, 1).total()).isEqualTo(1);
        repository.associateBusinessEntityProject("m009_company_b", a);
        repository.associateBusinessEntityProject("m009_company_b", a);
        assertThat(repository.businessEntityInProject("m009_company_b", "m009_project_b")).isTrue();
        assertThat(repository.listBusinessEntitiesPaged(a, 1, 1).total()).isEqualTo(2);
        var companyPage1 = repository.listBusinessEntitiesPaged(a, 1, 1);
        var companyPage2 = repository.listBusinessEntitiesPaged(a, 2, 1);
        assertThat(companyPage2.items()).hasSize(1).doesNotContainAnyElementsOf(companyPage1.items());
        assertThat(repository.listMerchantsByBusinessEntity(a, "m009_company_b")).isEmpty();
        assertThat(repository.listMerchantsByBusinessEntity(a, "m009_company_a")).extracting(MerchantRecord::id)
                .containsExactly("m009_merchant_a");
        assertThat(repository.listMerchantsByBusinessEntity(null, "m009_company_a")).isEmpty();
        for (String scope : new String[] {null, "platform-company", "missing-project"}) {
            org.junit.jupiter.api.Assertions.assertAll(
                    () -> assertThat(repository.listMerchantsPaged(scope, true, null, 1, 1).total()).isZero(),
                    () -> assertThat(repository.listProjectsPaged(scope, 1, 1).total()).isZero(),
                    () -> assertThat(repository.listSpacesPaged(scope, 1, 1).total()).isZero(),
                    () -> assertThat(repository.listBusinessEntitiesPaged(scope, 1, 1).total()).isZero(),
                    () -> assertThat(repository.listBusinessEntityBindingsPaged(scope, 1, 1).total()).isZero());
        }
    }

    @Test
    void projectOnlyBackfillPreservesEveryOtherDatabaseColumnAndLegacyNulls() {
        seedProjectScopeRows();
        Instant now = Instant.now();
        MerchantRecord legacy = new MerchantRecord("m009_legacy", "旧资料", "other", "financial-original",
                "contract-original", "settlement-original", "联系人", "18800000001", "负责人", "A",
                BigDecimal.TEN, BigDecimal.ONE, "第二联系人", "18800000002", BigDecimal.ZERO, "lease",
                "suspended", now, now).withArchive(now, "original-actor", now)
                .withDelete("pending", "original-actor", now, "原理由", now);
        repository.saveMerchant(legacy);
        assertThat(repository.findMerchantById(legacy.id()).orElseThrow().projectId()).isNull();
        assertThat(repository.listMerchantsPaged("m009_project_a", true, null, 1, 500).items())
                .extracting(MerchantRecord::id).doesNotContain(legacy.id());
        String before = jdbcTemplate.queryForObject(
                "SELECT (to_jsonb(m) - 'project_id')::text FROM merchants m WHERE id = ?", String.class, legacy.id());
        assertThat(repository.assignMerchantProject(legacy.id(), "m009_project_a")).isTrue();
        assertThat(repository.assignMerchantProject(legacy.id(), "m009_project_a")).isTrue();
        assertThat(repository.assignMerchantProject(legacy.id(), "m009_project_b")).isFalse();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT (to_jsonb(m) - 'project_id')::text FROM merchants m WHERE id = ?", String.class, legacy.id())).isEqualTo(before);
        assertThat(repository.findMerchantById(legacy.id()).orElseThrow().projectId()).isEqualTo("m009_project_a");
        // Existing record saves cannot erase a separately assigned project.
        repository.saveMerchant(legacy);
        assertThat(repository.findMerchantById(legacy.id()).orElseThrow().projectId()).isEqualTo("m009_project_a");
        ProjectRecord oldRequest = new ProjectRecord("m009_legacy_project", null, "complex", "旧调用项目",
                null, null, null, "active", now, now);
        repository.saveProject(oldRequest);
        assertThat(repository.listProjectsPaged(oldRequest.id(), 1, 1).items()).singleElement().satisfies(project -> {
            assertThat(project.ownerPerson()).isNull();
            assertThat(project.address()).isNull();
            assertThat(project.businessScope()).isNull();
        });
    }

    @Test
    void completingSpaceTypeRequestKeepsPriorRequestHistory() {
        Instant now = Instant.now();
        repository.saveSpaceType(new SpaceTypeRecord("m009_type_request", "m009_type_request", "申请历史", 100, "archived", now, now));
        repository.saveSpaceTypeDeleteRequest("m009_type_request", "第一次申请", "first-actor");
        repository.completeSpaceTypeDeleteRequest("m009_type_request", "rejected");
        repository.saveSpaceTypeDeleteRequest("m009_type_request", "第二次申请", "second-actor");
        repository.completeSpaceTypeDeleteRequest("m009_type_request", "approved");
        assertThat(jdbcTemplate.queryForList(
                "SELECT requested_by || ':' || status FROM space_type_delete_requests WHERE space_type_id = ? ORDER BY requested_by",
                String.class, "m009_type_request")).containsExactly("first-actor:rejected", "second-actor:approved");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM space_type_delete_requests WHERE space_type_id = ? AND status = 'pending'",
                Integer.class, "m009_type_request")).isZero();
    }

    @Test
    void backToBackSpaceTypeDeleteRequestsNeverCollideOnId() {
        Instant now = Instant.now();
        repository.saveSpaceType(new SpaceTypeRecord("m036a_type_burst", "m036a_type_burst", "连续申请", 100, "archived", now, now));
        for (int i = 0; i < 50; i++) {
            repository.saveSpaceTypeDeleteRequest("m036a_type_burst", "第" + i + "次申请", "actor-" + i);
            repository.completeSpaceTypeDeleteRequest("m036a_type_burst", "rejected");
        }
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM space_type_delete_requests WHERE space_type_id = ?",
                Integer.class, "m036a_type_burst")).isEqualTo(50);
    }


    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void concurrentFutureBindingsWaitForMerchantLockAndOnlyOneReservesTheSlot() throws Exception {
        String key = "m009_concurrent_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        Instant now = Instant.now();
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        List<java.util.concurrent.Future<String>> futures = new java.util.ArrayList<>();
        try {
            repository.saveProject(new ProjectRecord(key, null, "complex", "并发绑定测试", null, null, null, "active", now, now));
            repository.saveBusinessEntity(new BusinessEntityRecord(key, "并发主体", "management_company", null, null, null, "active", now, now));
            repository.associateBusinessEntityProject(key, key);
            repository.saveMerchant(new MerchantRecord(key, "并发商户", "other", null, null, null,
                    null, null, null, null, null, null, null, null, null, null, "active", now, now).withProject(key));
            var transaction = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
            transaction.executeWithoutResult(status -> {
                repository.lockMerchant(key);
                for (int index = 0; index < 2; index++) {
                    String requestKey = key + "_" + index;
                    futures.add(pool.submit(() -> {
                        var actor = new com.complex.platform.access.AccessActor("usr_admin", "platform_admin_user",
                                "platform_admin", key, null, "active");
                        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(actor, "token", List.of()));
                        try {
                            platformService.createBusinessEntityBinding(
                                    new com.complex.platform.governance.dto.BusinessEntityBindingRequest(
                                            requestKey, null, List.of(), key, key, key, key,
                                            LocalDate.now(java.time.ZoneOffset.UTC).plusYears(1), "并发预约"));
                            return "CREATED";
                        } catch (com.complex.platform.common.api.ApiException exception) {
                            return exception.code();
                        } finally {
                            org.springframework.security.core.context.SecurityContextHolder.clearContext();
                        }
                    }));
                }
                int waiting = 0;
                long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
                while (waiting < 2 && System.nanoTime() < deadline) {
                    jdbcTemplate.execute("SELECT pg_stat_clear_snapshot()");
                    waiting = jdbcTemplate.queryForObject("""
                            SELECT count(*) FROM pg_stat_activity
                             WHERE datname = current_database() AND wait_event_type = 'Lock'
                               AND query LIKE 'SELECT id FROM merchants WHERE id = %FOR UPDATE%'
                            """, Integer.class);
                    if (waiting < 2) {
                        try { Thread.sleep(25); }
                        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException(interrupted); }
                    }
                }
                assertThat(waiting).as("两个请求均在商户行锁上等待，不能跳过锁直接检查").isEqualTo(2);
            });
            assertThat(List.of(futures.get(0).get(10, java.util.concurrent.TimeUnit.SECONDS),
                    futures.get(1).get(10, java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("CREATED", "ACTIVE_BINDING_EXISTS");
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM business_entity_bindings WHERE merchant_id = ? AND status = 'inactive' AND effective_to IS NULL",
                    Integer.class, key)).isEqualTo(1);
            MerchantRecord merchant = repository.findMerchantById(key).orElseThrow();
            assertThat(merchant.businessEntityId()).isNull();
            assertThat(merchant.contractEntityId()).isNull();
            assertThat(merchant.settlementEntityId()).isNull();
        } finally {
            pool.shutdownNow();
            assertThat(pool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            jdbcTemplate.update("DELETE FROM business_entity_bindings WHERE merchant_id = ?", key);
            jdbcTemplate.update("DELETE FROM merchants WHERE id = ?", key);
            jdbcTemplate.update("DELETE FROM business_entity_projects WHERE business_entity_id = ?", key);
            jdbcTemplate.update("DELETE FROM business_entities WHERE id = ?", key);
            jdbcTemplate.update("DELETE FROM projects WHERE id = ?", key);
            jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key IN (?, ?)", key + "_0", key + "_1");
        }
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
                "be_pii", "PII Entity", "management_company", null, null, null, "active", t, t));
        repository.saveMerchant(new MerchantRecord("mch_pii_1", "PII 商户", "other", "be_pii", "be_pii", "be_pii",
                "第一联系人", "18800000021", "负责人", "1F-P", new BigDecimal("10.00"), new BigDecimal("9.00"),
                "第二联系人", "18800000022", new BigDecimal("1.00"), "lease", "active", t, t));

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
                "be_pii2", "PII Entity 2", "management_company", null, null, null, "active", t, t));
        repository.saveMerchant(new MerchantRecord("mch_pii_2", "PII 商户2", "other", "be_pii2", "be_pii2", "be_pii2",
                "联系人", "18800000023", "负责人", "1F-Q", new BigDecimal("10.00"), new BigDecimal("9.00"),
                null, null, new BigDecimal("1.00"), "lease", "active", t, t));
        repository.saveMerchant(repository.findMerchantById("mch_pii_2").orElseThrow());

        assertThat(repository.findMerchantById("mch_pii_2").orElseThrow().contactPhone()).isEqualTo("18800000023");
    }

    @Test
    void plaintextPhoneWriteIsRejectedByDatabaseConstraint() {
        // DB 级边界（V249 思路）：任何绕开仓库的手写 SQL / 批量导入写明文，数据库直接拒。
        Instant t = Instant.now();
        repository.saveBusinessEntity(new BusinessEntityRecord(
                "be_pii3", "PII Entity 3", "management_company", null, null, null, "active", t, t));
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

    // ---- M010-001：V318 固定父链列已删除（BEQR-144/PGM-018 多对多协作，不是隶属链）。
    @Test
    void parentEntityColumnIsRemovedFromBusinessEntities() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.columns WHERE table_name = 'business_entities' AND column_name = 'parent_entity_id'",
                Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE tablename = 'business_entities' AND indexname = 'idx_business_entities_parent'",
                Integer.class)).isZero();
    }

    // ---- M010-002/003：entity_role 白名单只剩 HTML 三角色，spv/operation 被库层拒绝（操作员 2.2 答「删除」）。
    @Test
    void entityRoleCheckRejectsSpvAndOperation() {
        // 约束定义本身只剩 HTML 三角色（同一事务内只能放一条违例语句，PostgreSQL 违例后事务即中止）。
        String definition = jdbcTemplate.queryForObject(
                "SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid = 'business_entities'::regclass AND conname = 'chk_be_entity_role'",
                String.class);
        assertThat(definition).contains("'platform'", "'project_company'", "'management_company'")
                .doesNotContain("'spv'", "'operation'");
        assertThat(jdbcTemplate.update(
                "INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at) VALUES ('be_role_ok', '角色探针', 'management_company', 'active', now(), now())"))
                .isEqualTo(1);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at) VALUES ('be_role_spv', '角色探针', 'spv', 'active', now(), now())"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    // ---- M010-004：V048 组织隔离配套——不同组织可同名角色，同组织/全局同名仍唯一。
    @Test
    void organizationScopedRolesMayShareNameAcrossOrganizations() {
        String insert = "INSERT INTO roles (id, role_name, description, data_scope, status, created_at, organization_id) VALUES (?, ?, NULL, 'project', 'active', now(), ?)";
        assertThat(jdbcTemplate.update(insert, "role_m010_a", "店长（M010探针）", "org_m010_a")).isEqualTo(1);
        assertThat(jdbcTemplate.update(insert, "role_m010_b", "店长（M010探针）", "org_m010_b")).isEqualTo(1);
        assertThatThrownBy(() -> jdbcTemplate.update(insert, "role_m010_a2", "店长（M010探针）", "org_m010_a"))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
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
