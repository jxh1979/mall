package com.complex.platform.access;

// 文件版本：V0.23
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.complex.platform.access.dto.*;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.support.InMemoryAuditEventRepository;
import com.complex.platform.support.InMemoryIdempotencyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class M006SecurityRegressionTests {
    private static final String LOW = "ops.production_readiness.view";
    private static final String HIGH = "finance.refund.create";
    private AccessRepository repository;
    private AccessCredentialProvisioner credentials;
    private AuthorizationService authorization;
    private AccessService service;
    private final Map<String, AccessUser> users = new LinkedHashMap<>();
    private final Map<String, RoleRecord> roles = new LinkedHashMap<>();
    private final Map<String, PermissionTemplate> templates = new LinkedHashMap<>();
    private final Map<String, List<PermissionTemplateItem>> items = new LinkedHashMap<>();
    private final List<AccessRepository.PermissionEffect> effects = new ArrayList<>();
    private final Set<String> consumed = new LinkedHashSet<>();
    private final Map<String, AccessGrantRequestRecord> requests = new LinkedHashMap<>();
    private final Map<String, String> fingerprints = new LinkedHashMap<>();
    private final Map<String, Set<String>> directRoles = new LinkedHashMap<>();
    private final Map<String, String> positionRoles = new LinkedHashMap<>();
    private final Map<String, PositionRoleTemplate> positionTemplates = new LinkedHashMap<>();

    @BeforeEach void setUp() {
        repository = mock(AccessRepository.class, invocation -> switch (invocation.getMethod().getName()) {
            case "lockGrantAuthorizationFacts" -> null;
            case "saveGrantFingerprint" -> { fingerprints.put(invocation.getArgument(0), invocation.getArgument(1)); yield null; }
            case "findGrantFingerprint" -> Optional.ofNullable(fingerprints.get(invocation.getArgument(0)));
            case "replacePositionGrants" -> { positionRoles.put(invocation.getArgument(0), invocation.getArgument(1)); yield null; }
            default -> CALLS_REAL_METHODS.answer(invocation);
        });
        when(repository.findGrantPolicy()).thenReturn(Optional.of("{\"windowDays\":{\"value\":30,\"min\":1,\"max\":2147483647},\"positionThreshold\":{\"value\":3,\"min\":1,\"max\":2147483647},\"userThreshold\":{\"value\":3,\"min\":1,\"max\":2147483647},\"riskReviews\":{\"low\":{\"approvalLevel\":\"one_level_up\",\"requiredLevel\":1},\"medium\":{\"approvalLevel\":\"one_level_up\",\"requiredLevel\":1},\"high\":{\"approvalLevel\":\"two_levels_up\",\"requiredLevel\":2},\"critical\":{\"approvalLevel\":\"top_authority\",\"requiredLevel\":3}}}"));
        credentials = mock(AccessCredentialProvisioner.class);
        when(repository.listPermissions()).thenReturn(List.of(
                new PermissionDefinition(LOW, "查看", "ops", "medium", false, "active"),
                new PermissionDefinition(HIGH, "退款", "finance", "high", true, "active")));
        when(repository.permissionExists(anyString())).thenAnswer(a ->
                repository.listPermissions().stream().anyMatch(p -> p.permissionCode().equals(a.getArgument(0))));
        when(repository.findPermissionEffects(anyString(), any())).thenAnswer(a -> {
            String id = a.getArgument(0);
            List<AccessRepository.PermissionEffect> result = new ArrayList<>(effects.stream()
                    .filter(e -> e.userId().equals(id) && !consumed.contains(e.sourceId())).toList());
            Set<String> assigned = new LinkedHashSet<>(directRoles.getOrDefault(id, Set.of()));
            if (positionRoles.containsKey(id)) assigned.add(positionRoles.get(id));
            for (String roleId : assigned) for (String code : roles.get(roleId).permissionCodes()) {
                result.add(new AccessRepository.PermissionEffect(id, code, AccessRepository.Effect.GRANT,
                        AccessRepository.Source.ROLE, roleId, null, null, false, false));
            }
            return result;
        });
        when(repository.markSingleUsePermissionConsumed(anyString(), anyString(), anyString(), any()))
                .thenAnswer(a -> consumed.add(a.getArgument(0)));
        when(repository.findUserById(anyString())).thenAnswer(a -> Optional.ofNullable(users.get(a.getArgument(0))));
        when(repository.listUsers()).thenAnswer(a -> new ArrayList<>(users.values()));
        when(repository.createUser(any())).thenAnswer(a -> {
            AccessUser u = a.getArgument(0); users.put(u.id(), u); return u;
        });
        when(repository.createRole(any())).thenAnswer(a -> {
            RoleRecord r = a.getArgument(0); roles.put(r.id(), r); return r;
        });
        when(repository.createTemporaryGrant(any())).thenAnswer(a -> a.getArgument(0));
        doAnswer(a -> {
            directRoles.computeIfAbsent(a.getArgument(0), key -> new LinkedHashSet<>()).add(a.getArgument(1));
            return new RoleAssignmentRecord(a.getArgument(0), a.getArgument(1));
        }).when(repository).assignUserRole(anyString(), anyString());
        when(repository.findRoleById(anyString())).thenAnswer(a -> Optional.ofNullable(roles.get(a.getArgument(0))));
        when(repository.findTemplateById(anyString())).thenAnswer(a -> Optional.ofNullable(templates.get(a.getArgument(0))));
        when(repository.findTemplateItems(anyString())).thenAnswer(a -> items.getOrDefault(a.getArgument(0), List.of()));
        when(repository.saveTemplate(any(), anyList())).thenAnswer(a -> {
            PermissionTemplate t = a.getArgument(0); templates.put(t.id(), t); items.put(t.id(), a.getArgument(1)); return t;
        });
        doAnswer(a -> { AccessGrantRequestRecord r = a.getArgument(0); requests.put(r.id(), r); return r; })
                .when(repository).saveGrantRequest(any());
        when(repository.findGrantRequestById(anyString())).thenAnswer(a -> Optional.ofNullable(requests.get(a.getArgument(0))));
        authorization = new AuthorizationService(repository);
        service = new AccessService(repository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(new InMemoryAuditEventRepository()), authorization, credentials, null, true, 3, 3);
        actor("admin", "platform_admin", "org", null);
    }

    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    // M006-001：同一管理员的执行、布尔查询和有效集都必须服从 DENY。
    @Test void administratorDenyWinsAndHighPermissionNeedsExplicitGrant() {
        effect("admin", LOW, AccessRepository.Effect.DENY, "override", false);
        assertDenied(() -> authorization.requirePermission(LOW));
        assertThat(authorization.currentActorHasPermission(LOW)).isFalse();
        assertDenied(() -> authorization.requirePermission(HIGH));
        assertThat(authorization.effectivePermissions().wildcard()).isFalse();
        effect("admin", HIGH, AccessRepository.Effect.GRANT, "approved-role", false);
        authorization.requirePermission(HIGH);
    }

    // M006-001：日常自动授予必须保留，高危定义不因默认管理员角色而获得。
    @Test void administratorKeepsDailyPermissionsWithoutAutomaticHighRole() {
        authorization.requirePermission(LOW);
        effect("admin", HIGH, AccessRepository.Effect.GRANT, "role_platform_admin", false);
        assertDenied(() -> authorization.requirePermission(HIGH));
    }

    // M006-I9-01（BSM-1.3.4「拥有系统里全部日常功能权限…不会出现"管理员反而没权限"的情况」；PGM-1.10.1 角色装配：平台管理员配置岗位权限模板与职位/层级映射）：
    // 治理本职码随默认管理员角色生效；资金等业务高危仍不因该角色获得；显式拒绝仍优先。
    @Test void administratorHoldsGovernanceWritePermissionsFromDefaultRoleButNotBusinessHighRisk() {
        String govern = "access.role.assign";
        var definitions = new ArrayList<>(repository.listPermissions());
        definitions.add(new PermissionDefinition(govern, "岗位装配", "access", "high", true, "active"));
        when(repository.listPermissions()).thenReturn(definitions);
        effect("admin", govern, AccessRepository.Effect.GRANT, "role_platform_admin", false);
        effect("admin", HIGH, AccessRepository.Effect.GRANT, "role_platform_admin", false);
        authorization.requirePermission(govern);
        assertDenied(() -> authorization.requirePermission(HIGH));
        assertThat(authorization.effectivePermissions().grants()).contains(govern).doesNotContain(HIGH);
        effect("admin", govern, AccessRepository.Effect.DENY, "override", false);
        assertDenied(() -> authorization.requirePermission(govern));
    }

    // M006-028：Spring 的未知 principal 不能凭空变成启用员工。
    @Test void untrustedPrincipalIsRejected() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("someone", "unused", List.of()));
        assertThatThrownBy(authorization::currentActor).isInstanceOf(ApiException.class);
    }

    // M006-003：缺组织并非全局授权。
    @Test void missingOrganizationCannotListAllUsersOrResolveGlobalProject() {
        actor("orphan", "staff", null, null);
        users.put("victim", user("victim", "other-org", null));
        assertThatThrownBy(service::listAccessUsers).isInstanceOf(ApiException.class);
        assertThatThrownBy(authorization::currentProjectScopeQuietly).isInstanceOf(ApiException.class);
    }

    // M006-002/012：同组织公司管理员可查；商户管理员仍不能跨商户。
    @Test void organizationReadWorksButCrossMerchantWriteDoesNot() {
        users.put("target", user("target", "org", "merchant-b"));
        actor("operator", "staff", "org", null);
        assertThat(authorization.effectivePermissionsForUser("target").userId()).isEqualTo("target");
        actor("operator", "merchant_admin", "org", "merchant-a");
        roles.put("daily", new RoleRecord("daily", "日常", null, "merchant", "active",
                Instant.now(), List.of(LOW), "org"));
        effect("operator", LOW, AccessRepository.Effect.GRANT, "own-role", false);
        assertThatThrownBy(() -> service.assignUserRole(new RoleAssignmentRequest("cross", null, "target", "daily")))
                .isInstanceOf(ApiException.class);
        verify(repository, never()).assignUserRole("target", "daily");
    }

    // M006-004：可读名称的字符归一化不能覆盖另一个账号。
    @Test void collidingUserSlugsRemainDifferentUsers() {
        AccessUser first = service.createAccessUser(account("first", "same-name", "active", "password"));
        AccessUser second = service.createAccessUser(account("second", "same.name", "active", "password"));
        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(users).hasSize(2);
    }

    // M006-005：已应用模板的内容编辑不能把不持有的高危权限发出去。
    @Test void templateEditCannotGrantAnUnownedHighPermission() {
        actor("manager", "staff", "org", null);
        effect("manager", LOW, AccessRepository.Effect.GRANT, "own", false);
        PermissionTemplate t = service.createPermissionTemplate(template("create", LOW));
        assertThatThrownBy(() -> service.updatePermissionTemplate(t.id(), template("edit", HIGH)))
                .isInstanceOf(ApiException.class);
        assertThat(items.get(t.id())).extracting(PermissionTemplateItem::permissionCode).containsExactly(LOW);
    }

    // M006-018：管理员改内容不转移模板所有权；不存在的模板不隐式创建。
    @Test void templateEditPreservesOwnerAndRejectsMissingTemplate() {
        actor("manager", "staff", "org", null);
        effect("manager", LOW, AccessRepository.Effect.GRANT, "own", false);
        PermissionTemplate t = service.createPermissionTemplate(template("create", LOW));
        actor("admin", "platform_admin", "org", null);
        assertThat(service.updatePermissionTemplate(t.id(), template("edit", LOW)).ownerScopeId()).isEqualTo("org");
        assertThatThrownBy(() -> service.updatePermissionTemplate("missing", template("missing", LOW)))
                .isInstanceOf(ApiException.class);
    }

    // M006-013：必须有目标、权限及有序时间窗。
    @Test void emptyPermissionsAndInvertedWindowAreRejectedBeforeGrantWrite() {
        users.put("target", user("target", "org", null));
        assertThatThrownBy(() -> service.createTemporaryGrant(new TemporaryGrantRequest(
                "empty", null, List.of(), "target", List.of(), null, Instant.now().plusSeconds(3600), false, "理由")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.createTemporaryGrant(new TemporaryGrantRequest(
                "reverse", null, List.of(), "target", List.of(LOW),
                Instant.now().plusSeconds(3600), Instant.now(), false, "理由")))
                .isInstanceOf(ApiException.class);
        verify(repository, never()).createTemporaryGrant(any());
    }

    // M006-024/029：验证必须早于任何用户/凭证写入。
    @Test void invalidStatusAndBlankPasswordHaveNoWrites() {
        assertThatThrownBy(() -> service.createAccessUser(account("bad-status", "bad-status", "typo", "password")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.createAccessUser(account("no-password", "no-password", "active", " ")))
                .isInstanceOf(ApiException.class);
        verify(repository, never()).createUser(any());
        verifyNoInteractions(credentials);
    }

    // M006-025：无消费用途的重复手机号不能继续被 SHA256 摘要存储和返回。
    @Test void manualAccountDoesNotPersistOrReturnPhoneDigest() {
        AccessUser created = service.createAccessUser(new AccessUserRequest("phone", null, List.of(),
                "vendor", "password", "13800000000", "outsourced_vendor", "org", null, "active"));
        assertThat(created.phoneHash()).isNull();
    }

    // M006-010：管理员身份本身不能形成最终审批级别。
    @Test void administratorWithoutApprovalRoleHasNoSupervisorLevel() {
        assertThat(authorization.currentSupervisorLevel()).isZero();
    }

    // M006-014：一个复核动作的两个权限先整体核验，只消耗同一来源一次。
    @Test void twoPermissionReviewConsumesOneSourceOnlyAfterBothChecksPass() {
        String first = "access.grant_request.review", second = "governance.supervisor_review.approve";
        when(repository.listPermissions()).thenReturn(List.of(
                new PermissionDefinition(first, "授权复核", "access", "high", true, "active"),
                new PermissionDefinition(second, "主管复核", "governance", "high", true, "active")));
        actor("reviewer", "staff", "org", null);
        effect("reviewer", first, AccessRepository.Effect.GRANT, "single-application", true);
        AccessService target = mock(AccessService.class);
        AccessController controller = new AccessController(target, authorization);
        AccessGrantReviewRequest request = new AccessGrantReviewRequest("review", null, List.of(), "approve", "同意");
        assertThatThrownBy(() -> controller.reviewGrant("pending", request)).isInstanceOf(ApiException.class);
        assertThat(consumed).isEmpty();
        effect("reviewer", second, AccessRepository.Effect.GRANT, "single-application", true);
        controller.reviewGrant("pending", request);
        verify(target).reviewGrant("pending", request);
        assertThat(consumed).containsExactly("single-application");
    }

    // M006-024/029：凭证适配器自身也拒绝伪成功，待激活账号不能取得可用密码。
    @Test void credentialAdapterDisablesInvitedAccountsAndRejectsBlankPassword() {
        var jdbc = mock(org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate.class);
        when(jdbc.update(anyString(), any(org.springframework.jdbc.core.namedparam.SqlParameterSource.class)))
                .thenReturn(1);
        var provisioner = new JdbcAccessCredentialProvisioner(jdbc);
        provisioner.provisionCredential("user", "password", "invited", Instant.now());
        var parameters = org.mockito.ArgumentCaptor.forClass(
                org.springframework.jdbc.core.namedparam.SqlParameterSource.class);
        verify(jdbc).update(anyString(), parameters.capture());
        assertThat(parameters.getValue().getValue("status")).isEqualTo("disabled");
        assertThatThrownBy(() -> provisioner.provisionCredential("user", " ", "active", Instant.now()))
                .isInstanceOf(ApiException.class);
        verifyNoMoreInteractions(jdbc);
    }

    // M006-029：开发适配器找不到账号，必须报告失败，不能静默略过。
    @Test void developmentCredentialAdapterRejectsMissingAccount() {
        var auth = mock(com.complex.platform.auth.AuthService.class);
        var provisioner = new DevAccessCredentialProvisioner(auth, repository);
        assertThatThrownBy(() -> provisioner.provisionCredential("missing", "password", "active", Instant.now()))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(auth);
    }

    // M006-006：申请时的角色含 A，待批期间变成 A+B，必须在审批写入前拒绝。
    @Test void changedRoleCannotBeAppliedUnderAnOldApproval() {
        var reviews = reviews();
        users.put("target", user("target", "org", null));
        roles.put("role", new RoleRecord("role", "角色", null, "project", "active", Instant.now(), List.of(HIGH), "org"));
        actor("requester", "staff", "org", null);
        AccessGrantRequestRecord pending = service.applyGrant(new AccessGrantApplyRequest("apply", null, List.of(),
                "user_role", "target", "role", null, null, null, null, null, List.of(), null, null, false, "申请"));
        roles.put("role", new RoleRecord("role", "角色", null, "global", "active", Instant.now(), List.of(HIGH, LOW), "org"));
        actor("reviewer", "staff", "org", null);
        effect("reviewer", HIGH, AccessRepository.Effect.GRANT, "approved", false);
        effect("reviewer", LOW, AccessRepository.Effect.GRANT, "approved", false);
        assertThatThrownBy(() -> service.reviewGrant(pending.id(),
                new AccessGrantReviewRequest("review", null, List.of(), "approve", "同意")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GRANT_SNAPSHOT_CHANGED"));
        verify(reviews, never()).approve(anyString(), any());
    }

    // M006-007：目标用户不能先把自己的授权审批落为通过。
    @Test void beneficiaryCannotApproveOwnGrant() {
        var reviews = reviews();
        users.put("beneficiary", user("beneficiary", "org", null));
        actor("requester", "staff", "org", null);
        AccessGrantRequestRecord pending = service.applyGrant(new AccessGrantApplyRequest("apply", null, List.of(),
                "temporary_permission", "beneficiary", null, null, null, null, null, null,
                List.of(HIGH), null, Instant.now().plusSeconds(3600), false, "申请"));
        actor("beneficiary", "staff", "org", null);
        effect("beneficiary", HIGH, AccessRepository.Effect.GRANT, "approved", false);
        assertThatThrownBy(() -> service.reviewGrant(pending.id(),
                new AccessGrantReviewRequest("review", null, List.of(), "approve", "同意")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("BENEFICIARY_SELF_REVIEW_FORBIDDEN"));
        verify(reviews, never()).approve(anyString(), any());
    }

    private com.complex.platform.governance.review.SupervisorReviewService reviews() {
        var reviews = mock(com.complex.platform.governance.review.SupervisorReviewService.class);
        var result = new com.complex.platform.governance.review.SupervisorReviewDecisionRecord(
                "review", "permission_grant_l2", "permission_grant_l2", "access_grant_request",
                "request", true, "approved", 2, 2, "approved", "理由", List.of(), Instant.now(), "requester",
                "two_levels_up", 6);
        when(reviews.blockForAdmission(anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(), any()))
                .thenReturn(result); // M007-A-02：授权申请改走带受益人的 7 参重载
        when(reviews.approve(anyString(), any())).thenReturn(result);
        service = new AccessService(repository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(new InMemoryAuditEventRepository()), authorization, credentials, reviews, true, 3, 3);
        return reviews;
    }

    // M006-009：换岗撤销派生角色；同角色若另有明确授权，则仍保留。
    @Test void positionChangeDropsDerivedRoleButKeepsIndependentAssignment() {
        String other = "ops.production_readiness.check";
        var definitions = new ArrayList<>(repository.listPermissions());
        definitions.add(new PermissionDefinition(other, "检查", "ops", "medium", false, "active"));
        when(repository.listPermissions()).thenReturn(definitions);
        when(repository.findStaffLevelById("level")).thenReturn(Optional.of(
                new StaffLevelRecord("level", "M1", "lower", 1, "职级", "active", Instant.now())));
        doAnswer(a -> a.getArgument(0)).when(repository).saveStaffLevelAssignment(any());
        doAnswer(a -> a.getArgument(0)).when(repository).saveUserPositionAssignment(any());
        doAnswer(a -> { PositionRoleTemplate mapping = a.getArgument(0); positionTemplates.put(mapping.positionCode(), mapping); return mapping; })
                .when(repository).savePositionRoleTemplate(any());
        when(repository.findPositionRoleTemplate(any(), anyString(), anyString()))
                .thenAnswer(a -> Optional.ofNullable(positionTemplates.get(a.getArgument(1))));
        when(repository.applyTemplate(anyString(), anyList(), any(), any(), anyBoolean(), anyString())).thenReturn(List.of());
        roles.put("old", new RoleRecord("old", "旧岗位", null, "project", "active", Instant.now(), List.of(LOW), "org"));
        roles.put("new", new RoleRecord("new", "新岗位", null, "project", "active", Instant.now(), List.of(other), "org"));
        actor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID, "platform_admin", "org", null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
                        com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", "org", null, "active"),
                "unused", List.of()));
        PermissionTemplate template = service.createPermissionTemplate(template("empty"));
        for (String code : List.of("old", "new")) service.createPositionRoleTemplate(
                new PositionRoleTemplateRequest(code, "理由", code, "level", code, template.id(), "active"));
        for (String id : List.of("derived", "independent")) {
            users.put(id, user(id, "org", null));
            if ("independent".equals(id)) repository.assignUserRole(id, "old");
            for (String position : List.of("old", "new")) service.assignUserPosition(
                    new UserPositionAssignmentRequest(id + position, "理由", id, position, "level", 0L, "CNY", null));
        }
        assertThat(repository.findPermissionEffects("derived", Instant.now()))
                .extracting(AccessRepository.PermissionEffect::permissionCode).containsExactly(other);
        assertThat(repository.findPermissionEffects("independent", Instant.now()))
                .extracting(AccessRepository.PermissionEffect::permissionCode).containsExactlyInAnyOrder(LOW, other);
    }


    // M006-005：不增加权限码也可能扩权，延长已授权模板和取消一次性均须重新授权。
    @Test void appliedTemplateCannotExtendTimeOrRemoveSingleUse() {
        Instant end = Instant.now().plusSeconds(3600);
        PermissionTemplate t = service.createPermissionTemplate(new PermissionTemplateRequest(
                "limited", "理由", List.of(), "模板", "base", null, List.of(LOW), "project", null, end, true));
        assertThatThrownBy(() -> service.updatePermissionTemplate(t.id(), new PermissionTemplateRequest(
                "extend", "理由", List.of(), "模板", "base", null, List.of(LOW), "project", null, end.plusSeconds(3600), true)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.updatePermissionTemplate(t.id(), new PermissionTemplateRequest(
                "repeat", "理由", List.of(), "模板", "base", null, List.of(LOW), "project", null, end, false)))
                .isInstanceOf(ApiException.class);
        assertThat(service.updatePermissionTemplate(t.id(), new PermissionTemplateRequest(
                "reduce", "理由", List.of(), "模板", "base", null, List.of(), "project", null, end, true))
                .permissionCodes()).isEmpty();
    }

    // M006-013：待审批申请也必须先校验时间窗，不能先生成复核单。
    @Test void pendingTemporaryGrantRejectsInvertedTimeWindow() {
        var reviews = reviews();
        users.put("target", user("target", "org", null));
        assertThatThrownBy(() -> service.applyGrant(new AccessGrantApplyRequest("inverted", null, List.of(),
                "temporary_permission", "target", null, null, null, null, null, null,
                List.of(HIGH), Instant.now().plusSeconds(7200), Instant.now().plusSeconds(3600), false, "申请")))
                .isInstanceOf(ApiException.class);
        verify(reviews, never()).blockForAdmission(anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(), any());
    }

    // M006-001/010：最高职级普通管理员不能代替 USB 最高权限；真实 USB 会话可审批。
    @Test void topAuthorityReviewRequiresMasterSessionRatherThanRankAlone() {
        var reviewRepository = mock(com.complex.platform.governance.review.SupervisorReviewRepository.class);
        var pending = new com.complex.platform.governance.review.SupervisorReviewDecisionRecord(
                "top", "permission_grant_l3", "permission_grant_l3", "access_grant_request", "request",
                true, "pending", 3, 0, null, "理由", List.of(), Instant.now(), "requester", "top_authority", 30);
        when(reviewRepository.findPendingSupervisorReviewById("top")).thenReturn(Optional.of(pending));
        // M087：最高权限事项的链末节点为主会话节点（可变持有，裁决后 listChainNodes 反映新状态）。
        List<com.complex.platform.governance.chain.ApprovalChainNode> topChain = new ArrayList<>(List.of(
                new com.complex.platform.governance.chain.ApprovalChainNode("n_top", "top", "main", 1, "master", null, "pending", null, null, null, null, 0)));
        when(reviewRepository.listChainNodes("top")).thenAnswer(a -> List.copyOf(topChain));
        when(reviewRepository.updateChainNode(any())).thenAnswer(a -> { topChain.set(0, a.getArgument(0)); return true; });
        when(reviewRepository.saveSupervisorReview(any())).thenAnswer(a -> a.getArgument(0));
        var reviews = new com.complex.platform.governance.review.SupervisorReviewService(authorization,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                reviewRepository, new AuditEventService(new InMemoryAuditEventRepository()));
        when(repository.findStaffLevelAssignment("admin")).thenReturn(Optional.of(new StaffLevelAssignment(
                "admin", "top-level", 0L, "CNY", null, "org", null, "admin", Instant.now())));
        when(repository.findStaffLevelById("top-level")).thenReturn(Optional.of(new StaffLevelRecord(
                "top-level", "M10", "upper", 30, "最高职级", "active", Instant.now())));
        effect("admin", "governance.supervisor_review.approve", AccessRepository.Effect.GRANT, "approval-role", false);
        var request = new com.complex.platform.governance.review.SupervisorReviewApprovalRequest(
                "top-admin", "同意", List.of(), null, "approved", "同意");
        assertThatThrownBy(() -> reviews.approve("top", request)).isInstanceOf(ApiException.class);
        verify(reviewRepository, never()).saveSupervisorReview(any());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
                        com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", null, null, "active"),
                "unused", List.of()));
        assertThat(reviews.approve("top", new com.complex.platform.governance.review.SupervisorReviewApprovalRequest(
                "top-master", "同意", List.of(), null, "approved", "同意")).decision()).isEqualTo("approved");
    }


    // M006-011：员工必须经登记审批路径，手工账号接口仅保留外部协作机构。
    @Test void manualEmployeeCreationCannotIssueCredentialsBeforeRegistrationApproval() {
        for (String type : List.of("staff", "sales", "supervisor", "merchant", "merchant_admin")) {
            assertThatThrownBy(() -> service.createAccessUser(new AccessUserRequest(type, "理由", List.of(),
                    type, "password", null, type, "org", "merchant", "active")))
                    .isInstanceOf(ApiException.class);
        }
        verify(repository, never()).createUser(any());
        verifyNoInteractions(credentials);
    }

    // M006-025：历史摘要也不进入账号列表的 JSON。
    @Test void accountJsonDoesNotExposeStoredPhoneDigest() throws Exception {
        var value = new AccessUser("id", "name", "staff", "org", null, "active", Instant.now(),
                "phone-digest", "manual_admin", null, List.of(), null);
        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(value);
        assertThat(json).doesNotContain("phoneHash", "phone_hash", "phone-digest");
    }

    // M006-029：凭证适配器不可用时必须在账号写入前失败。
    @Test void missingCredentialAdapterCannotReportSuccessfulCreation() {
        var withoutCredentials = new AccessService(repository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(new InMemoryAuditEventRepository()), authorization, null, null, true, 3, 3);
        assertThatThrownBy(() -> withoutCredentials.createAccessUser(account("missing-adapter", "vendor", "active", "password")))
                .isInstanceOf(ApiException.class);
        verify(repository, never()).createUser(any());
    }



    // M006-011/029：员工审批也必须成功创建凭证，不能退化成静默空操作。
    @Test void employeeApprovalWithoutCredentialAdapterWritesNoAccount() {
        var staffRepository = mock(com.complex.platform.access.staff.JdbcStaffRepository.class);
        when(staffRepository.saveProfile(any())).thenAnswer(a -> a.getArgument(0));
        when(staffRepository.saveNotice(any())).thenAnswer(a -> a.getArgument(0));
        @SuppressWarnings("unchecked") org.springframework.beans.factory.ObjectProvider<AccessCredentialProvisioner> provider =
                mock(org.springframework.beans.factory.ObjectProvider.class, invocation ->
                        invocation.getArguments().length == 1 && invocation.getArguments()[0] instanceof java.util.function.Supplier<?> supplier
                                ? supplier.get() : RETURNS_DEFAULTS.answer(invocation));
        var staff = new com.complex.platform.access.staff.StaffService(staffRepository, repository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(new InMemoryAuditEventRepository()), authorization, provider);
        var profile = staff.submitRegistration(new com.complex.platform.access.staff.dto.StaffRegistrationRequest(
                "no-credential-registration", null, "org", null, "员工", null, null, null));
        when(staffRepository.findProfileById(profile.id())).thenReturn(Optional.of(profile));
        assertThatThrownBy(() -> staff.reviewRegistration(profile.id(),
                new com.complex.platform.access.staff.dto.StaffRegistrationReviewRequest(
                        "no-credential-approval", null, "approve", null, "password"))).isInstanceOf(ApiException.class);
        verify(repository, never()).createUser(any());
        verify(staffRepository, times(1)).saveProfile(any());
    }

    // M006-011/002：登记不创建账号；商户不能自批，公司审批才开通并自动赋员工自助。
    @Test void registrationCreatesAccountOnlyAfterCompanyApproval() {
        var staffRepository = mock(com.complex.platform.access.staff.JdbcStaffRepository.class);
        Map<String, com.complex.platform.access.staff.StaffProfileRecord> profiles = new HashMap<>();
        when(staffRepository.saveProfile(any())).thenAnswer(a -> {
            com.complex.platform.access.staff.StaffProfileRecord p = a.getArgument(0);
            profiles.put(p.id(), p); return p;
        });
        when(staffRepository.findProfileById(anyString())).thenAnswer(a -> Optional.ofNullable(profiles.get(a.getArgument(0))));
        when(staffRepository.saveNotice(any())).thenAnswer(a -> a.getArgument(0));
        @SuppressWarnings("unchecked") org.springframework.beans.factory.ObjectProvider<AccessCredentialProvisioner> provider =
                mock(org.springframework.beans.factory.ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(credentials);
        var staff = new com.complex.platform.access.staff.StaffService(staffRepository, repository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(new InMemoryAuditEventRepository()), authorization, provider);
        var registration = staff.submitRegistration(new com.complex.platform.access.staff.dto.StaffRegistrationRequest(
                "registration", "后台辅助登记", "org", "merchant", "员工", null, null, null));
        assertThat(registration.registrationStatus()).isEqualTo("submitted");
        assertThat(registration.userId()).isNull();
        verify(repository, never()).createUser(any());
        verifyNoInteractions(credentials);
        actor("merchant-operator", "merchant_admin", "org", "merchant");
        assertThatThrownBy(() -> staff.reviewRegistration(registration.id(),
                new com.complex.platform.access.staff.dto.StaffRegistrationReviewRequest(
                        "merchant-review", null, "approve", null, "password"))).isInstanceOf(ApiException.class);
        actor("company-reviewer", "staff", "org", null);
        var approved = staff.reviewRegistration(registration.id(),
                new com.complex.platform.access.staff.dto.StaffRegistrationReviewRequest(
                        "company-review", null, "approve", null, "password"));
        assertThat(approved.registrationStatus()).isEqualTo("approved");
        assertThat(users.get(approved.userId()).organizationId()).isEqualTo("org");
        verify(repository).assignUserRole(approved.userId(), "role_staff_self");
        verify(credentials).provisionCredential(eq(approved.userId()), eq("password"), eq("active"), any());
    }


    // M006-016：只读查询参数不能改写异常判断阈值。
    @Test void summaryQueryCannotOverrideBusinessThresholds() {
        assertThatThrownBy(() -> service.grantSummary(null, Integer.MAX_VALUE, null))
                .isInstanceOf(ApiException.class);
    }

    // M006-030：全量参考字典的分页元数据必须与实际返回条数一致。
    @Test void referenceDictionaryDoesNotPretendItWasTruncatedAtFifty() {
        List<PermissionDefinition> definitions = java.util.stream.IntStream.range(0, 55)
                .mapToObj(i -> new PermissionDefinition("p" + i, "权限", "access", "low", false, "active")).toList();
        when(repository.listPermissions()).thenReturn(definitions);
        var result = service.listPermissions();
        assertThat(result.items()).hasSize(55);
        assertThat(result.pageSize()).isEqualTo(55);
    }

    // M006-030：授权申请默认读取配置窗口，不向仓储传无限时间范围。
    @Test void grantListHasADefaultTimeWindow() {
        service.listGrantRequests(null);
        var cutoff = org.mockito.ArgumentCaptor.forClass(Instant.class);
        verify(repository).listGrantRequests(cutoff.capture());
        assertThat(cutoff.getValue()).isNotNull();
    }


    // M006-026：旧 DELETE 入口不能再物理删除配置。
    @Test void directPositionTemplateDeleteCannotBypassArchiveAndApproval() {
        var mapping = new PositionRoleTemplate("mapping", "position", "level", "role", "template",
                null, "active", "creator", Instant.now(), Instant.now());
        when(repository.findPositionRoleTemplateById("mapping")).thenReturn(Optional.of(mapping));
        doNothing().when(repository).deletePositionRoleTemplate("mapping");
        assertThatThrownBy(() -> service.deletePositionRoleTemplate("mapping")).isInstanceOf(ApiException.class);
        verify(repository, never()).deletePositionRoleTemplate(anyString());
    }


    // M006-026：存档→申请→最高权限复核→软删除→恢复；占用和自审均被拒绝。
    @Test void positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval() {
        var mapping = new java.util.concurrent.atomic.AtomicReference<>(new PositionRoleTemplate(
                "lifecycle", "position", "level", "role", "template", null, "active", "creator", Instant.now(), Instant.now()));
        when(repository.findPositionRoleTemplateById("lifecycle")).thenAnswer(a -> Optional.of(mapping.get()));
        when(repository.listPositionRoleTemplates(null)).thenAnswer(a -> List.of(mapping.get()));
        doAnswer(a -> { mapping.set(a.getArgument(0)); return null; }).when(repository).savePositionTemplateLifecycle(any(), anyString());
        doReturn(1L).when(repository).countPositionTemplateUsage("lifecycle");
        var reviewRepository = mock(com.complex.platform.governance.review.SupervisorReviewRepository.class);
        Map<String, com.complex.platform.governance.review.SupervisorReviewDecisionRecord> decisions = new HashMap<>();
        when(reviewRepository.saveSupervisorReview(any())).thenAnswer(a -> {
            com.complex.platform.governance.review.SupervisorReviewDecisionRecord r = a.getArgument(0); decisions.put(r.reviewId(), r); return r;
        });
        when(reviewRepository.findPendingSupervisorReviewById(anyString())).thenAnswer(a ->
                Optional.ofNullable(decisions.get(a.getArgument(0))).filter(r -> "pending_review".equals(r.reviewStatus())));
        // M087：审批链节点存取（申请人无直接上级 → 最高权限事项只有主会话节点）。
        Map<String, List<com.complex.platform.governance.chain.ApprovalChainNode>> chains = new HashMap<>();
        doAnswer(a -> { chains.put(a.getArgument(0), new ArrayList<>(a.getArgument(1))); return null; })
                .when(reviewRepository).saveChainNodes(anyString(), any());
        when(reviewRepository.listChainNodes(anyString())).thenAnswer(a -> chains.getOrDefault(a.getArgument(0), List.of()));
        when(reviewRepository.updateChainNode(any())).thenAnswer(a -> {
            com.complex.platform.governance.chain.ApprovalChainNode n = a.getArgument(0);
            List<com.complex.platform.governance.chain.ApprovalChainNode> l = chains.get(n.reviewId());
            if (l == null) return false;
            for (int i = 0; i < l.size(); i++) if (l.get(i).nodeId().equals(n.nodeId())) l.set(i, n);
            return true;
        });
        var reviews = new com.complex.platform.governance.review.SupervisorReviewService(authorization,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                reviewRepository, new AuditEventService(new InMemoryAuditEventRepository()));
        service = new AccessService(repository, new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(new InMemoryAuditEventRepository()), authorization, credentials, reviews, true, 3, 3);
        service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("archive", "archive", "停止使用", List.of()));
        assertThat(mapping.get().archivedAt()).isNotNull();
        assertThat(service.listPositionRoleTemplates().items()).isEmpty();
        assertThatThrownBy(() -> service.changePositionTemplateLifecycle("lifecycle",
                new StatusChangeRequest("in-use", "request_delete", "删除申请", List.of()))).isInstanceOf(ApiException.class);
        doReturn(0L).when(repository).countPositionTemplateUsage("lifecycle");
        service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("request", "request_delete", "删除申请", List.of()));
        assertThatThrownBy(() -> service.changePositionTemplateLifecycle("lifecycle",
                new StatusChangeRequest("ordinary-admin", "approve_delete", "同意", List.of()))).isInstanceOf(ApiException.class);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
                        com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", null, null, "active"), "unused", List.of()));
        service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("approve", "approve_delete", "同意", List.of()));
        assertThat(mapping.get().deletedAt()).isNotNull();
        assertThat(service.listPositionRoleTemplates(true).items()).hasSize(1);
        service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("restore", "restore", "恢复留存配置", List.of()));
        assertThat(mapping.get().deletedAt()).isNull();
        assertThat(mapping.get().status()).isEqualTo("inactive");
        service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("self-archive", "archive", "停止使用", List.of()));
        int reviewCount = decisions.size();
        service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("self-request", "request_delete", "最高权限本人删除", List.of()));
        assertThat(mapping.get().deletedAt()).isNotNull();
        assertThat(decisions).hasSize(reviewCount);
    }

    private static AccessUserRequest account(String key, String name, String status, String password) {
        return new AccessUserRequest(key, null, List.of(), name, password, null,
                "outsourced_vendor", "org", null, status);
    }
    private static PermissionTemplateRequest template(String key, String... codes) {
        return new PermissionTemplateRequest(key, "理由", List.of(), "模板", "base", null,
                List.of(codes), "project", null, null, false);
    }
    private static AccessUser user(String id, String org, String merchant) {
        return new AccessUser(id, id, "staff", org, merchant, "active", Instant.now());
    }
    private static void actor(String id, String type, String org, String merchant) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(id, id, type, org, merchant, "active"), "unused", List.of()));
    }
    private void effect(String id, String code, AccessRepository.Effect effect, String source, boolean once) {
        effects.add(new AccessRepository.PermissionEffect(id, code, effect, AccessRepository.Source.ROLE,
                source, null, null, once, false));
    }
    private static void assertDenied(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
    }

    // M006-031：实际控制器须按组织和操作人区分首次请求。

    @Test void companyRequestsHaveIndependentResultsAndRetryOnlyOnce() {
        var idem = new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper());
        idem.setCallerScopeResolver(authorization);
        service = new AccessService(repository, idem, new AuditEventService(new InMemoryAuditEventRepository()),
                authorization, credentials, null, true, 3, 3);
        when(repository.listPermissions()).thenReturn(List.of(
                new PermissionDefinition(LOW, "查看", "ops", "medium", false, "active"),
                new PermissionDefinition("access.role.manage", "管理角色", "access", "medium", false, "active")));
        for(String id: List.of("manager-a", "manager-b")) {
            effect(id, LOW, AccessRepository.Effect.GRANT, id+"-read", false);
            effect(id, "access.role.manage", AccessRepository.Effect.GRANT, id+"-manage", false);
        }
        var controller = new AccessController(service, authorization);
        var request = new RoleRequest("company-first-role", "正常首次创建", List.of(), "巡检员", "只读", List.of(LOW), "project");
        actor("manager-a", "staff", "org-a", null);
        var first = controller.createRole(request).data();
        actor("manager-b", "staff", "org-b", null);
        var result = controller.createRole(request);
        assertThat(roles.values()).extracting(RoleRecord::organizationId).containsExactlyInAnyOrder("org-a", "org-b");
        assertThat(result.data().id()).isNotEqualTo(first.id());
        assertThat(controller.createRole(request).data().id()).isEqualTo(result.data().id());
        actor("manager-a", "staff", "org-a", null);
        assertThat(controller.createRole(request).data().id()).isEqualTo(first.id());
        assertThat(roles).hasSize(2);
    }

    // M006-032：实际错误响应须保持中文。

    @Test void invalidAccountTypeResponseIsChinese() throws Exception {
        when(repository.listPermissions()).thenReturn(List.of(
            new PermissionDefinition("access.user.manage", "管理账号", "access", "medium", false, "active")));
        var controller = new AccessController(service, authorization);
        ApiException failure = null;
        try {
            controller.createAccessUser(new AccessUserRequest("b5-invalid", "检查错误提示", List.of(),
                "example", "password", null, "platform_admin", "org", null, "active"));
        } catch(ApiException e) { failure = e; }
        assertThat(failure).isNotNull();
        assertThat(failure.code()).isEqualTo("INVALID_ACCOUNT_TYPE");
        var handler = new com.complex.platform.common.api.GlobalExceptionHandler();
        var method = handler.getClass().getDeclaredMethod("handleApiException", ApiException.class);
        method.setAccessible(true);
        var response = (org.springframework.http.ResponseEntity<?>) method.invoke(handler, failure);
        var body = (com.complex.platform.common.api.ApiResponse<?>) response.getBody();
        assertThat(body.message()).contains("账号类型").doesNotContainPattern("[A-Za-z]");
    }


    // M006-034：审批结论报错须为纯中文，不回显 approve/reject。

    // M006-R6-03（PGM-1.18 后台报错全中文）：控制器层临时授权缺失效时间、未登录两条残留英文文案改为中文。
    @Test void temporaryGrantMissingValidUntilAndUnauthenticatedMessagesAreChinese() {
        var controller = new AccessController(service, mock(AuthorizationService.class)); // 门禁交由 mock 放行，只看校验文案
        ApiException failure = null;
        try {
            controller.createTemporaryGrant(new TemporaryGrantRequest("b5-valid-until", "检查错误提示", List.of(), "u",
                List.of(LOW), java.time.Instant.parse("2026-06-14T00:00:00Z"), null, false, "r"));
        } catch(ApiException e) { failure = e; }
        assertThat(failure).isNotNull();
        assertThat(failure.code()).isEqualTo("VALIDATION_FAILED");
        assertThat(failure.getMessage()).contains("失效时间").doesNotContainPattern("[A-Za-z]");

        var saved = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        try {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
            ApiException unauthenticated = null;
            try { authorization.currentActor(); } catch(ApiException e) { unauthenticated = e; }
            assertThat(unauthenticated).isNotNull();
            assertThat(unauthenticated.code()).isEqualTo("UNAUTHENTICATED");
            assertThat(unauthenticated.getMessage()).contains("登录").doesNotContainPattern("[A-Za-z]");
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(saved);
        }
    }

    @Test void invalidDecisionMessageIsChinese() throws Exception {
        var method = AccessService.class.getDeclaredMethod("normalizeDecision", String.class);
        method.setAccessible(true);
        ApiException failure = null;
        try {
            method.invoke(service, "maybe");
        } catch(java.lang.reflect.InvocationTargetException e) { failure = (ApiException) e.getCause(); }
        assertThat(failure).isNotNull();
        assertThat(failure.code()).isEqualTo("VALIDATION_FAILED");
        assertThat(failure.getMessage()).contains("审批结论").doesNotContainPattern("[A-Za-z]");
    }


    // M006-035：平台管理员的有效权限计算不得按权限字典逐项重复查询（一次计算最多读 2 次字典）。

    @Test void platformAdminEffectivePermissionsDoNotQueryDictionaryPerPermission() {
        List<PermissionDefinition> dictionary = new ArrayList<>();
        for (int i = 0; i < 200; i++) dictionary.add(new PermissionDefinition("p" + i, "权限" + i, "access",
                i % 3 == 0 ? "high" : "low", i % 3 == 0, "active"));
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        when(repository.listPermissions()).thenAnswer(a -> { calls.incrementAndGet(); return dictionary; });
        long daily = dictionary.stream().filter(p -> !p.requiresApproval()).count();
        EffectivePermissions effective = authorization.effectivePermissions();
        assertThat(effective.grants()).hasSize((int) daily);
        assertThat(calls.get()).isLessThanOrEqualTo(2);
        calls.set(0);
        authorization.requirePermission("p1");
        assertThat(calls.get()).isLessThanOrEqualTo(2);
    }

    // M006-036：无组织归属的员工复制他组织模板必须得到可定位的拒绝，而不是空指针。

    @Test void orphanStaffCopyingOrganizationTemplateIsRejectedNotCrashed() {
        Instant now = Instant.now();
        templates.put("tpl-org", new PermissionTemplate("tpl-org", "tpl-org", "模板", null, "base", null,
                "organization", "org", "merge", null, null, false, "active", null, "seed", now, now, now, List.of(LOW)));
        actor("orphan", "staff", null, null);
        assertThatThrownBy(() -> service.copyPermissionTemplate("tpl-org", template("copy-orphan", LOW)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PROJECT_SCOPE_REQUIRED"));
    }


    // M006-G-02（PGM-1.10.1/1.10.2）：已定岗员工改职级，须按新职级的岗位映射重算派生权限，旧派生失效。

    @Test void staffLevelChangeRederivesPositionGrants() {
        String other = "ops.production_readiness.check";
        var definitions = new ArrayList<>(repository.listPermissions());
        definitions.add(new PermissionDefinition(other, "检查", "ops", "medium", false, "active"));
        when(repository.listPermissions()).thenReturn(definitions);
        for (String level : List.of("l1", "l2")) when(repository.findStaffLevelById(level)).thenReturn(Optional.of(
                new StaffLevelRecord(level, level.toUpperCase(Locale.ROOT), "lower", level.equals("l1") ? 1 : 4, "职级", "active", Instant.now())));
        doAnswer(a -> a.getArgument(0)).when(repository).saveStaffLevelAssignment(any());
        doAnswer(a -> a.getArgument(0)).when(repository).saveUserPositionAssignment(any());
        Map<String, PositionRoleTemplate> byLevel = new HashMap<>();
        doAnswer(a -> { PositionRoleTemplate mapping = a.getArgument(0); byLevel.put(mapping.staffLevelId(), mapping); return mapping; })
                .when(repository).savePositionRoleTemplate(any());
        when(repository.findPositionRoleTemplate(any(), anyString(), anyString()))
                .thenAnswer(a -> Optional.ofNullable(byLevel.get(a.getArgument(2))));
        when(repository.applyTemplate(anyString(), anyList(), any(), any(), anyBoolean(), anyString())).thenReturn(List.of());
        var positions = new HashMap<String, UserPositionAssignment>();
        doAnswer(a -> { UserPositionAssignment p = a.getArgument(0); positions.put(p.userId(), p); return p; })
                .when(repository).saveUserPositionAssignment(any());
        when(repository.findUserPositionAssignment(anyString())).thenAnswer(a -> Optional.ofNullable(positions.get(a.getArgument(0))));
        roles.put("junior", new RoleRecord("junior", "初级", null, "project", "active", Instant.now(), List.of(LOW), "org"));
        roles.put("senior", new RoleRecord("senior", "高级", null, "project", "active", Instant.now(), List.of(other), "org"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
                        com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", "org", null, "active"), "unused", List.of()));
        PermissionTemplate template = service.createPermissionTemplate(template("empty"));
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest("m-l1", "理由", "ops", "l1", "junior", template.id(), "active"));
        service.createPositionRoleTemplate(new PositionRoleTemplateRequest("m-l2", "理由", "ops", "l2", "senior", template.id(), "active"));
        users.put("worker", user("worker", "org", null));
        service.assignUserPosition(new UserPositionAssignmentRequest("pos", "理由", "worker", "ops", "l1", 0L, "CNY", null));
        assertThat(repository.findPermissionEffects("worker", Instant.now())).extracting(AccessRepository.PermissionEffect::permissionCode).containsExactly(LOW);
        service.assignStaffLevel(new StaffLevelAssignmentRequest("lvl", "晋升", "worker", "l2", 0L, "CNY", null));
        assertThat(repository.findPermissionEffects("worker", Instant.now())).extracting(AccessRepository.PermissionEffect::permissionCode).containsExactly(other);
        assertThat(positions.get("worker").staffLevelId()).isEqualTo("l2");
    }

}
