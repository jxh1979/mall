package com.complex.platform.access;

// 文件版本：V0.13
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/** M006-014/017：只用 complex_test 中连接内临时表；不修改共享业务表。 */
class M006JdbcRegressionTests {
    private Connection connection;
    private JdbcTemplate jdbc;
    private AccessRepository repository;

    @BeforeEach void openTemporaryTables() throws Exception {
        String url = System.getenv("COMPLEX_DATABASE_URL");
        assumeTrue(url != null && url.matches("jdbc:postgresql:.*?/complex_test(?:\\?.*)?"));
        connection = DriverManager.getConnection(url, System.getenv("COMPLEX_DATABASE_USERNAME"),
                System.getenv("COMPLEX_DATABASE_PASSWORD"));
        var dataSource = new SingleConnectionDataSource(connection, true);
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TEMP TABLE permission_templates (id varchar PRIMARY KEY, single_use boolean NOT NULL)");
        jdbc.execute("""
                CREATE TEMP TABLE permission_template_applications (
                    id varchar PRIMARY KEY, template_id varchar, target_id varchar,
                    single_use boolean NOT NULL, consumed boolean NOT NULL DEFAULT false,
                    consumed_at timestamptz, status varchar DEFAULT 'active',
                    valid_from timestamptz, valid_until timestamptz)
                """);
        jdbc.execute("""
                CREATE TEMP TABLE temporary_permission_grants (
                    id uuid PRIMARY KEY, subject_id varchar, permission_code varchar,
                    single_use boolean NOT NULL, consumed boolean NOT NULL DEFAULT false,
                    consumed_at timestamptz, status varchar DEFAULT 'active',
                    valid_from timestamptz, expires_at timestamptz)
                """);
        jdbc.execute("""
                CREATE TEMP TABLE permission_template_usage_logs (
                    id uuid PRIMARY KEY, application_id varchar REFERENCES pg_temp.permission_template_applications(id),
                    user_id varchar, permission_code varchar, result varchar, reason varchar, occurred_at timestamptz)
                """);
        jdbc.execute("CREATE TEMP TABLE access_grant_requests (LIKE public.access_grant_requests INCLUDING DEFAULTS INCLUDING CONSTRAINTS)");
        jdbc.execute("ALTER TABLE pg_temp.access_grant_requests ADD COLUMN IF NOT EXISTS authorization_fingerprint varchar(64)");
        var target = new JdbcAccessRepository(new NamedParameterJdbcTemplate(dataSource));
        var factory = new ProxyFactory(target);
        factory.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(dataSource),
                new AnnotationTransactionAttributeSource()));
        repository = (AccessRepository) factory.getProxy();
    }

    @AfterEach void closeConnection() throws Exception {
        if (connection != null) connection.close();
    }

    @Test void temporaryGrantLogsItsActualSourceAndConsumesOnlyOnce() {
        UUID id = temporaryGrant();
        assertThat(repository.markSingleUsePermissionConsumed(id.toString(), "permission", "user", Instant.now())).isTrue();
        Map<String, Object> log = jdbc.queryForMap("SELECT application_id, reason FROM permission_template_usage_logs");
        assertThat(log.get("application_id")).isNull();
        assertThat(log.get("reason").toString()).contains(id.toString());
        assertThat(repository.markSingleUsePermissionConsumed(id.toString(), "permission", "user", Instant.now())).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM permission_template_usage_logs", Integer.class)).isEqualTo(1);
    }

    @Test void auditFailureRollsBackConsumptionThroughActualSpringTransaction() {
        UUID id = temporaryGrant();
        jdbc.execute("ALTER TABLE permission_template_usage_logs ADD CONSTRAINT reject_log CHECK (result <> 'consumed')");
        assertThatThrownBy(() -> repository.markSingleUsePermissionConsumed(
                id.toString(), "permission", "user", Instant.now())).isInstanceOf(RuntimeException.class);
        assertThat(jdbc.queryForObject("SELECT consumed FROM temporary_permission_grants WHERE id = ?",
                Boolean.class, id)).isFalse();
    }

    @Test void inheritedTemplateSingleUseCanBeConsumed() {
        jdbc.update("INSERT INTO permission_templates VALUES ('template', true)");
        jdbc.update("""
                INSERT INTO permission_template_applications (id, template_id, target_id, single_use)
                VALUES ('application', 'template', 'user', false)
                """);
        assertThat(repository.markSingleUsePermissionConsumed("application", "permission", "user", Instant.now())).isTrue();
        assertThat(jdbc.queryForObject("SELECT application_id FROM permission_template_usage_logs", String.class))
                .isEqualTo("application");
    }

    // M006-008：读取待审记录须在服务事务内取得 PostgreSQL 行锁。
    @Test void grantReviewReadLocksTheRequestRow() {
        jdbc.update("INSERT INTO access_grant_requests (id, request_type) VALUES ('request', 'temporary_permission')");
        new org.springframework.transaction.support.TransactionTemplate(
                new DataSourceTransactionManager(jdbc.getDataSource())).execute(status -> {
            assertThat(repository.findGrantRequestById("request")).isPresent();
            assertThat(jdbc.queryForObject("""
                    SELECT count(*) FROM pg_locks
                     WHERE pid = pg_backend_pid()
                       AND relation = 'pg_temp.access_grant_requests'::regclass
                       AND mode = 'RowShareLock' AND granted
                    """, Integer.class)).isGreaterThan(0);
            return null;
        });
    }


    // M006-006：定义和子项的写入锁保持到整个审批事务结束，防止比对后重新解析不同授权。
    @Test void grantFactsRemainStableUntilTheGrantTransactionCommits() throws Exception {
        var tables = java.util.List.of("access_users", "roles", "role_permissions", "permissions",
                "permission_template_items", "position_role_templates", "staff_levels");
        for (String table : tables) jdbc.execute("CREATE TEMP TABLE " + table + " (id varchar)");
        new org.springframework.transaction.support.TransactionTemplate(
                new DataSourceTransactionManager(jdbc.getDataSource())).execute(status -> {
            org.springframework.test.util.ReflectionTestUtils.invokeMethod(repository, "lockGrantAuthorizationFacts");
            for (String table : java.util.stream.Stream.concat(tables.stream(), java.util.stream.Stream.of("permission_templates")).toList()) {
                assertThat(jdbc.queryForObject("""
                        SELECT count(*) FROM pg_locks WHERE pid=pg_backend_pid()
                          AND relation=to_regclass(?) AND mode='ShareLock' AND granted
                        """, Integer.class, "pg_temp." + table)).isGreaterThan(0);
            }
            return null;
        });
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_locks WHERE pid=pg_backend_pid() AND mode='ShareLock'",
                Integer.class)).isZero();
    }


    // M006-006/009/016/026：执行完整迁移 SQL，仅作用于本连接临时表；验证政策可持久化修改。
    @Test void migrationAppliesAndGrantPolicyRoundTripsInPostgres() throws Exception {
        preparePositionMigrationTables();
        jdbc.execute(migrationSql());
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var initial = mapper.readValue(repository.findGrantPolicy().orElseThrow(), AccessGrantPolicy.class);
        assertThat(initial.windowDays().value()).isEqualTo(30);
        String changed = mapper.writeValueAsString(new AccessGrantPolicy(
                new AccessGrantPolicy.BoundedInt(7, 1, 30), new AccessGrantPolicy.BoundedInt(2, 1, 20),
                new AccessGrantPolicy.BoundedInt(4, 1, 20), initial.riskReviews()));
        repository.saveGrantPolicy(changed);
        var reloaded = mapper.readValue(repository.findGrantPolicy().orElseThrow(), AccessGrantPolicy.class);
        assertThat(reloaded.windowDays().value()).isEqualTo(7);
        assertThat(reloaded.positionThreshold().value()).isEqualTo(2);
        assertThat(reloaded.userThreshold().value()).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM supervisor_review_policies WHERE policy_code='position_role_template_delete' AND approval_level='top_authority'",
                Integer.class)).isEqualTo(1);
        assertThatThrownBy(() -> new AccessGrantPolicy.BoundedInt(31, 1, 30)).isInstanceOf(com.complex.platform.common.api.ApiException.class);
    }

    // M006-006：快照可读取、不可覆盖；未记录快照明确返回空。
    @Test void approvalFingerprintIsWriteOnceAndMissingIsExplicit() {
        jdbc.update("INSERT INTO access_grant_requests (id, request_type) VALUES ('request', 'temporary_permission')");
        assertThat(repository.findGrantFingerprint("request")).isEmpty();
        repository.saveGrantFingerprint("request", "a".repeat(64));
        assertThat(repository.findGrantFingerprint("request")).contains("a".repeat(64));
        assertThatThrownBy(() -> repository.saveGrantFingerprint("request", "b".repeat(64)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(repository.findGrantFingerprint("request")).contains("a".repeat(64));
    }


    // M006-009：换岗只撤销上一岗位派生应用，独立应用仍有效。
    @Test void positionReplacementRevokesOnlyItsOwnPreviousApplications() {
        jdbc.execute("ALTER TABLE permission_template_applications ADD COLUMN revoked_by varchar, ADD COLUMN revoked_at timestamptz");
        jdbc.execute("CREATE TEMP TABLE user_position_assignments (user_id varchar PRIMARY KEY, assigned_role_id varchar, assigned_position_template_id varchar, position_application_ids varchar[] NOT NULL DEFAULT '{}')");
        jdbc.update("INSERT INTO user_position_assignments VALUES ('user', 'old-role', 'old-mapping', ARRAY['old-position'])");
        jdbc.update("""
                INSERT INTO permission_template_applications (id, template_id, target_id, single_use)
                VALUES ('old-position', 'template', 'user', false),
                       ('independent', 'template', 'user', false),
                       ('new-position', 'template', 'user', false)
                """);
        repository.replacePositionGrants("user", "new-role", "new-mapping", java.util.List.of("new-position"), "reviewer", Instant.now());
        assertThat(jdbc.queryForObject("SELECT status FROM permission_template_applications WHERE id = 'old-position'", String.class)).isEqualTo("revoked");
        assertThat(jdbc.queryForObject("SELECT status FROM permission_template_applications WHERE id = 'independent'", String.class)).isEqualTo("active");
        assertThat(jdbc.queryForObject("SELECT status FROM permission_template_applications WHERE id = 'new-position'", String.class)).isEqualTo("active");
        assertThat(jdbc.queryForObject("SELECT assigned_role_id FROM user_position_assignments", String.class)).isEqualTo("new-role");
        assertThat(jdbc.queryForObject("SELECT position_application_ids[1] FROM user_position_assignments", String.class)).isEqualTo("new-position");
    }


    // M006-004：创建碰撞必须失败，不允许把现有对象移动到新组织。
    @Test void createCollisionCannotOverwriteExistingUserOrRole() {
        jdbc.execute("CREATE TEMP TABLE access_users (LIKE public.access_users INCLUDING ALL)");
        jdbc.execute("CREATE TEMP TABLE roles (LIKE public.roles INCLUDING ALL)");
        jdbc.execute("CREATE TEMP TABLE role_permissions (LIKE public.role_permissions INCLUDING ALL)");
        var now = Instant.now();
        var first = new AccessUser("existing", "first", "staff", "org-a", null, "active", now);
        repository.createUser(first);
        assertThatThrownBy(() -> repository.createUser(new AccessUser(
                "existing", "second", "staff", "org-b", null, "active", now)))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(repository.findUserById("existing").orElseThrow().organizationId()).isEqualTo("org-a");
        repository.createRole(new RoleRecord("role", "原角色", null, "project", "active", now, java.util.List.of(), "org-a"));
        assertThatThrownBy(() -> repository.createRole(new RoleRecord(
                "role", "新角色", null, "project", "active", now, java.util.List.of(), "org-b")))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(repository.findRoleById("role").orElseThrow().organizationId()).isEqualTo("org-a");
    }


    // M006-030/002：后续页可达；COUNT 与数据共用组织、商户和时间谓词。
    @Test void grantPaginationKeepsScopeAndCanReachTheSecondPage() {
        jdbc.execute("CREATE TEMP TABLE access_users (id varchar PRIMARY KEY, organization_id varchar)");
        jdbc.update("INSERT INTO access_users VALUES ('target', 'org'), ('other-target', 'other-org')");
        jdbc.update("""
                INSERT INTO access_grant_requests (id, request_type, target_user_id, merchant_id)
                SELECT 'request-' || i, 'temporary_permission', 'target', 'merchant' FROM generate_series(1, 55) i
                """);
        jdbc.update("INSERT INTO access_grant_requests (id, request_type, target_user_id, merchant_id) VALUES ('outside', 'temporary_permission', 'other-target', 'other-merchant')");
        var page = repository.pageGrantRequests(Instant.now().minusSeconds(3600), "org", "merchant", 2, 50);
        assertThat(page.total()).isEqualTo(55);
        assertThat(page.items()).hasSize(5).allMatch(item -> "target".equals(item.targetUserId()));
        assertThat(page.page()).isEqualTo(2);
        assertThat(repository.pageGrantRequests(Instant.now().minusSeconds(3600), null, null, 1, 50).total()).isEqualTo(56);
    }


    // M006-026：真实 SQL 只更新生命周期，恢复后原角色与模板仍可查询。
    @Test void positionTemplateSoftDeleteCanBeRestoredWithoutLosingItsDefinition() {
        jdbc.execute("CREATE TEMP TABLE position_role_templates (LIKE public.position_role_templates INCLUDING ALL)");
        jdbc.execute("""
                ALTER TABLE pg_temp.position_role_templates
                    ADD COLUMN IF NOT EXISTS archived_at timestamptz, ADD COLUMN IF NOT EXISTS archived_by varchar,
                    ADD COLUMN IF NOT EXISTS deleted_at timestamptz, ADD COLUMN IF NOT EXISTS deleted_by varchar,
                    ADD COLUMN IF NOT EXISTS delete_review_id varchar, ADD COLUMN IF NOT EXISTS delete_reason text
                """);
        var original = new PositionRoleTemplate("mapping", "position", "level", "role", "template",
                null, "active", "creator", Instant.now(), Instant.now());
        repository.savePositionRoleTemplate(original);
        var deleted = original.withLifecycle(Instant.now(), Instant.now(), "review", "批准删除", Instant.now());
        repository.savePositionTemplateLifecycle(deleted, "master");
        assertThat(repository.findPositionRoleTemplate(null, "position", "level")).isEmpty();
        assertThat(repository.findPositionRoleTemplateById("mapping").orElseThrow().deletedAt()).isNotNull();
        repository.savePositionTemplateLifecycle(deleted.withLifecycle(null, null, null, null, Instant.now()), "master");
        var restored = repository.findPositionRoleTemplateById("mapping").orElseThrow();
        assertThat(restored.deletedAt()).isNull();
        assertThat(restored.roleId()).isEqualTo("role");
        assertThat(restored.templateId()).isEqualTo("template");
        assertThat(restored.status()).isEqualTo("inactive");
        assertThatThrownBy(() -> repository.deletePositionRoleTemplate("mapping")).isInstanceOf(IllegalStateException.class);
    }

    private UUID temporaryGrant() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO temporary_permission_grants (id, subject_id, permission_code, single_use) VALUES (?, 'user', 'permission', true)", id);
        return id;
    }

    // M006-009/026：旧版本真实写入形态经迁移后仍区分岗位来源与独立来源。
    @Test void legacyPositionMigrationPreservesIndependentGrantsAndCountsOccupiedDefinitions() throws Exception {
        preparePositionMigrationTables();
        jdbc.update("INSERT INTO roles (id, role_name, data_scope, status, created_at) VALUES ('old-role','旧岗位','project','active',now()), ('prior-role','更早岗位','project','active',now()), ('new-role','新岗位','project','active',now())");
        jdbc.update("INSERT INTO access_users (id,username,account_type,status,organization_id,created_at) VALUES ('legacy','legacy','staff','active','org',now()), ('explicit','explicit','staff','active','org',now()), ('no-audit','no-audit','staff','active','org',now())");
        jdbc.update("INSERT INTO position_role_templates (id,position_code,staff_level_id,role_id,template_id,organization_id) VALUES ('old-mapping','old-position','level','old-role','old-template','org'), ('outside','old-position','level','old-role','old-template','other-org'), ('new-mapping','new-position','level','new-role','new-template','org')");
        jdbc.update("INSERT INTO user_position_assignments (user_id,position_code,staff_level_id,organization_id,assigned_by) VALUES ('legacy','old-position','level','org','operator'), ('explicit','old-position','level','org','operator'), ('no-audit','old-position','level','org','operator')");
        jdbc.update("INSERT INTO user_roles (user_id,role_id) VALUES ('legacy','old-role'), ('legacy','prior-role'), ('explicit','old-role')");
        jdbc.update("""
                INSERT INTO permission_template_applications (id,template_id,target_id,target_type,single_use,applied_by,applied_at)
                VALUES ('legacy-app','old-template','legacy','user',false,'operator',now()),
                       ('independent-app','old-template','legacy','user',false,'access-api',now()),
                       ('new-app','new-template','legacy','user',false,'operator',now())
                """);
        for (String user : java.util.List.of("legacy", "explicit")) {
            jdbc.update("""
                    INSERT INTO audit_events (id,occurred_at,event_name,event_result,business_type,business_id,metadata)
                    VALUES (?,now(),'access.user_position.assigned','success','user_position_assignment',?,
                            '{"position_code":"old-position","staff_level_id":"level","role_id":"old-role","template_id":"old-template"}')
                    """, UUID.randomUUID(), user);
        }
        jdbc.update("""
                INSERT INTO audit_events (id,occurred_at,event_name,event_result,business_type,business_id,metadata)
                VALUES (?,now() - interval '1 day','access.user_position.assigned','success','user_position_assignment','legacy',
                        '{"position_code":"prior-position","staff_level_id":"level","role_id":"prior-role","template_id":"prior-template"}'),
                       (?,now(),'access.role.assigned','success','user_role','explicit:old-role','{"role_id":"old-role"}')
                """, UUID.randomUUID(), UUID.randomUUID());
        jdbc.execute(migrationSql());
        assertThat(repository.countPositionTemplateUsage("old-mapping")).isEqualTo(3);
        assertThat(repository.countPositionTemplateUsage("outside")).isZero();
        assertThat(jdbc.queryForObject("SELECT assigned_role_id FROM user_position_assignments WHERE user_id='legacy'", String.class)).isEqualTo("old-role");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_roles WHERE user_id='legacy'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_roles WHERE user_id='explicit' AND role_id='old-role'", Integer.class)).isEqualTo(1);
        repository.replacePositionGrants("legacy", "new-role", "new-mapping", java.util.List.of("new-app"), "operator", Instant.now());
        assertThat(jdbc.queryForObject("SELECT status FROM permission_template_applications WHERE id='legacy-app'", String.class)).isEqualTo("revoked");
        assertThat(jdbc.queryForObject("SELECT status FROM permission_template_applications WHERE id='independent-app'", String.class)).isEqualTo("active");
        assertThat(jdbc.queryForObject("SELECT status FROM permission_template_applications WHERE id='new-app'", String.class)).isEqualTo("active");
    }

    private void preparePositionMigrationTables() {
        jdbc.execute("ALTER TABLE pg_temp.access_grant_requests DROP COLUMN authorization_fingerprint");
        for (String table : java.util.List.of("supervisor_review_policies", "position_role_templates",
                "user_position_assignments", "roles", "user_roles", "access_users", "audit_events")) {
            jdbc.execute("CREATE TEMP TABLE " + table + " (LIKE public." + table + " INCLUDING ALL)");
        }
        // M006-F-02：LIKE 复制的是 complex_test 当前结构（库已迁到 V386 时含这些列），下面回放的 V386 会再次 ADD COLUMN；
        // 先在临时表上去掉，使本测试与库的迁移水位无关。
        jdbc.execute("ALTER TABLE pg_temp.supervisor_review_policies DROP COLUMN IF EXISTS parameters");
        jdbc.execute("ALTER TABLE pg_temp.position_role_templates DROP COLUMN IF EXISTS archived_at, DROP COLUMN IF EXISTS archived_by, "
                + "DROP COLUMN IF EXISTS deleted_at, DROP COLUMN IF EXISTS deleted_by, DROP COLUMN IF EXISTS delete_review_id, DROP COLUMN IF EXISTS delete_reason");
        jdbc.execute("ALTER TABLE pg_temp.user_position_assignments DROP COLUMN IF EXISTS assigned_role_id, "
                + "DROP COLUMN IF EXISTS assigned_position_template_id, DROP COLUMN IF EXISTS position_application_ids");
        jdbc.execute("ALTER TABLE pg_temp.permission_template_applications ADD COLUMN target_type varchar, ADD COLUMN applied_by varchar, ADD COLUMN applied_at timestamptz, ADD COLUMN revoked_by varchar, ADD COLUMN revoked_at timestamptz");
    }

    // M006-F-02：surefire 的 cwd 是 backend/，相对仓库根的路径必错；迁移文件在测试类路径上。
    private static String migrationSql() throws java.io.IOException {
        try (var in = M006JdbcRegressionTests.class.getResourceAsStream("/db/migration/V386__access_grant_authorization_fingerprint.sql")) {
            if (in == null) throw new java.io.FileNotFoundException("V386 migration not on classpath");
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
