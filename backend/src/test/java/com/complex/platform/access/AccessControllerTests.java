// 文件版本：V0.01
package com.complex.platform.access;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.auth.AuthService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.GlobalExceptionHandler;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.config.SecurityConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AccessController.class)
@Import({com.complex.platform.support.AuthAccessTestConfiguration.class, GlobalExceptionHandler.class, SecurityConfig.class, AccessControllerTests.TestBeans.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class AccessControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccessService service;

    @Autowired
    private AuthorizationService authorizationService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service, authorizationService);
    }

    @Test
    void unauthenticatedAccessEndpointReturnsUnauthorizedOrForbidden() throws Exception {
        int status = mockMvc.perform(get("/api/v1/access/permissions"))
                .andReturn()
                .getResponse()
                .getStatus();

        org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
    }

    @Test
    void createTemplateWithoutPermissionReturnsPermissionDenied() throws Exception {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("access.permission_template.manage");

        mockMvc.perform(post("/api/v1/access/permission-templates")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "template-denied",
                                  "template_name": "Ops template",
                                  "permission_codes": ["ops.production_readiness.view"]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
    }

    @Test
    void listPermissionsReturnsApiResponsePageShape() throws Exception {
        when(service.listPermissions()).thenReturn(new PageResponse<>(
                List.of(new PermissionDefinition(
                        "ops.production_readiness.view",
                        "View production readiness",
                        "ops",
                        "medium",
                        false,
                        "active"
                )),
                1,
                50,
                1
        ));

        mockMvc.perform(get("/api/v1/access/permissions")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items[0].permission_code").value("ops.production_readiness.view"))
                .andExpect(jsonPath("$.data.total").value(1));
        Mockito.verify(authorizationService).requirePermission("access.permission.view");
    }

    @Test
    void createPermissionTemplateMissingIdempotencyKeyReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/access/permission-templates")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "template_name": "Ops template",
                                  "permission_codes": ["ops.production_readiness.view"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    @Test
    void assignUserRoleWithoutRoleAssignPermissionReturnsPermissionDenied() throws Exception {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("access.role.assign");

        mockMvc.perform(post("/api/v1/access/role-assignments")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "assign-denied",
                                  "user_id": "usr_1",
                                  "role_id": "role_ops"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
    }

    @Test
    void createTemporaryGrantMissingValidUntilReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/access/temporary-grants")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "temporary-missing-validity",
                                  "target_user_id": "usr_1",
                                  "permission_codes": ["ops.production_readiness.check"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    // M006-G-01：员工级覆盖写入口受 access.role.assign 门禁；缺幂等键拒绝；成功返回目标用户有效集。
    @Test
    void userPermissionOverrideEndpointGatesOnRoleAssignAndReturnsEffectivePermissions() throws Exception {
        when(service.setUserPermissionOverrides(any(), any())).thenReturn(new EffectivePermissions(
                "usr_target", List.of(), List.of("ops.production_readiness.view"), false,
                java.time.Instant.parse("2026-09-06T08:30:00Z")));

        mockMvc.perform(post("/api/v1/access/users/usr_target/permission-overrides")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "override-deny",
                                  "permission_codes": ["ops.production_readiness.view"],
                                  "effect": "deny",
                                  "reason": "停权"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.denies[0]").value("ops.production_readiness.view"));

        mockMvc.perform(post("/api/v1/access/users/usr_target/permission-overrides/remove")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"permission_codes": ["ops.production_readiness.view"], "effect": "deny"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));

        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("access.role.assign");
        mockMvc.perform(post("/api/v1/access/users/usr_target/permission-overrides")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key": "override-denied", "permission_codes": ["ops.production_readiness.view"], "effect": "deny"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
    }

    @Test
    void listStaffLevelsReturnsApiResponsePageShape() throws Exception {
        when(service.listStaffLevels()).thenReturn(new PageResponse<>(
                List.of(new StaffLevelRecord(
                        "lvl_m1_lower", "M1", "lower", 1, "M1 薪资大范围", "active",
                        java.time.Instant.parse("2026-06-14T00:00:00Z"))),
                1, 50, 1));

        mockMvc.perform(get("/api/v1/access/staff-levels")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items[0].id").value("lvl_m1_lower"))
                .andExpect(jsonPath("$.data.items[0].level_rank").value(1));
        Mockito.verify(authorizationService).requirePermission("access.staff_level.view");
    }

    @Test
    void assignStaffLevelReturnsOkWithAssignment() throws Exception {
        when(service.assignStaffLevel(any())).thenReturn(new StaffLevelAssignment(
                "usr_emp1", "lvl_m1_lower", 100000L, "CNY", "月薪", "org", "merchant",
                "access-api", java.time.Instant.parse("2026-06-14T00:00:00Z")));

        mockMvc.perform(put("/api/v1/access/users/usr_emp1/staff-level")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "assign-1",
                                  "staff_level_id": "lvl_m1_lower",
                                  "base_salary_amount_cent": 100000,
                                  "salary_currency": "CNY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.staff_level_id").value("lvl_m1_lower"))
                .andExpect(jsonPath("$.data.salary_currency").value("CNY"));
        Mockito.verify(authorizationService).requirePermission("access.staff_level.manage");
    }

    @Test
    void assignStaffLevelMissingIdempotencyKeyReturnsValidationFailed() throws Exception {
        mockMvc.perform(put("/api/v1/access/users/usr_emp1/staff-level")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "staff_level_id": "lvl_m1_lower"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    @Test
    void effectivePermissionsSelfReturnsGrantsDeniesAndWildcard() throws Exception {
        when(authorizationService.effectivePermissions()).thenReturn(new EffectivePermissions(
                "usr_self",
                List.of("ticket.order.view", "bar.table.open_close"),
                List.of("finance.refund.create"),
                false,
                java.time.Instant.parse("2026-06-18T08:30:00Z")
        ));

        mockMvc.perform(get("/api/v1/access/effective-permissions")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.user_id").value("usr_self"))
                .andExpect(jsonPath("$.data.grants[0]").value("ticket.order.view"))
                .andExpect(jsonPath("$.data.denies[0]").value("finance.refund.create"))
                .andExpect(jsonPath("$.data.wildcard").value(false))
                .andExpect(jsonPath("$.data.computed_at").value("2026-06-18T08:30:00Z"));
    }

    @Test
    void userEffectivePermissionsWithoutUserViewPermissionReturnsPermissionDenied() throws Exception {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("access.user.view");

        mockMvc.perform(get("/api/v1/access/users/usr_target/effective-permissions")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
    }

    @Test
    void userEffectivePermissionsReturnsTargetUserSetWithUserViewPermission() throws Exception {
        when(service.effectivePermissionsForUser("usr_target")).thenReturn(new EffectivePermissions(
                "usr_target",
                List.of("amusement.project.view"),
                List.of(),
                true,
                java.time.Instant.parse("2026-06-18T09:00:00Z")
        ));

        mockMvc.perform(get("/api/v1/access/users/usr_target/effective-permissions")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.user_id").value("usr_target"))
                .andExpect(jsonPath("$.data.grants[0]").value("amusement.project.view"))
                .andExpect(jsonPath("$.data.wildcard").value(true));
    }

    // M035-002：岗位模板生命周期端点按动作分两种门控——批准/驳回删除须同时持两个权限码，其余动作只须角色装配权限。
    @Test
    void positionTemplateLifecycleGatesDeleteDecisionsOnBothPermissions() throws Exception {
        mockMvc.perform(post("/api/v1/access/position-role-templates/prt_1/lifecycle")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key": "lifecycle-approve", "action": "approve_delete", "reason": "同意删除"}
                                """))
                .andExpect(status().isOk());
        Mockito.verify(authorizationService).requirePermissions("access.role.assign", "governance.supervisor_review.approve");
        Mockito.verify(authorizationService, Mockito.never()).requirePermission("access.role.assign");

        Mockito.reset(authorizationService);
        mockMvc.perform(post("/api/v1/access/position-role-templates/prt_1/lifecycle")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key": "lifecycle-archive", "action": "archive", "reason": "停止使用"}
                                """))
                .andExpect(status().isOk());
        Mockito.verify(authorizationService).requirePermission("access.role.assign");
        Mockito.verify(authorizationService, Mockito.never())
                .requirePermissions("access.role.assign", "governance.supervisor_review.approve");
    }

    // M035-002：授权申请审批须同时持审批权限与主管复核权限；缺任一即 403，且不进服务层。
    @Test
    void grantReviewRequiresBothReviewAndSupervisorApprovePermissions() throws Exception {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermissions("access.grant_request.review", "governance.supervisor_review.approve");

        mockMvc.perform(post("/api/v1/access/grant-requests/agr_1/review")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key": "review-denied", "decision": "approve"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        Mockito.verify(service, Mockito.never()).reviewGrant(any(), any());
    }

    // M002-F42（治理地图第 651、656 行；M002-F37）：三类申请（类型去首尾空白、不分大小写）持两码时，入口对申请权限与分配角色权限一次核验
    // （同一单次来源只消耗一次），不再单独核验申请权限，随后进服务层。
    @Test
    void roleAssignInitiatedGrantRequestsVerifyApplyAndRoleAssignInOneCheck() throws Exception {
        for (String type : List.of("user_permission_grant", "user_permission_deny_removal", "position_high_risk_release", "  User_Permission_GRANT ")) {
            Mockito.reset(service, authorizationService);
            when(authorizationService.currentActorHasPermission("access.grant_request.apply")).thenReturn(true);
            when(authorizationService.currentActorHasPermission("access.role.assign")).thenReturn(true);

            mockMvc.perform(post("/api/v1/access/grant-requests")
                            .with(user("ops").roles("P0_SMOKE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(m002F42ApplyJson("f42-both", type)))
                    .andExpect(status().isOk());
            Mockito.verify(authorizationService).requirePermissions("access.grant_request.apply", "access.role.assign");
            Mockito.verify(authorizationService, Mockito.never()).requirePermission(Mockito.anyString());
            Mockito.verify(service).applyGrant(any());
        }
    }

    // M002-F42：三类申请持申请权限、不持（含显式拒绝）分配角色权限——403 PERMISSION_DENIED，提示语为 F37 原文；
    // 不做会消耗来源的核验，不进服务层。
    @Test
    void roleAssignInitiatedGrantRequestWithoutRoleAssignIsRefusedBeforeAnySourceIsConsumed() throws Exception {
        for (String type : List.of("user_permission_grant", "user_permission_deny_removal", "position_high_risk_release", " POSITION_HIGH_RISK_RELEASE")) {
            Mockito.reset(service, authorizationService);
            when(authorizationService.currentActorHasPermission("access.grant_request.apply")).thenReturn(true);

            mockMvc.perform(post("/api/v1/access/grant-requests")
                            .with(user("ops").roles("P0_SMOKE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(m002F42ApplyJson("f42-no-assign", type)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(
                            "员工单独授予、解除员工显式拒绝、岗位模板高危权限释放这三类授权申请，只能由具备分配/取消分配角色权限的人发起")));
            Mockito.verify(authorizationService, Mockito.never()).requirePermissions("access.grant_request.apply", "access.role.assign");
            Mockito.verify(authorizationService, Mockito.never()).requirePermission(Mockito.anyString());
            Mockito.verify(service, Mockito.never()).applyGrant(any());
        }
    }

    // M002-F42：三类申请不持申请权限——照基线由 requirePermission(apply) 给出标准拒绝，不再判分配角色权限，不进服务层。
    @Test
    void roleAssignInitiatedGrantRequestWithoutApplyGetsTheStandardRefusal() throws Exception {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "没有操作权限：access.grant_request.apply"))
                .when(authorizationService)
                .requirePermission("access.grant_request.apply");

        mockMvc.perform(post("/api/v1/access/grant-requests")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(m002F42ApplyJson("f42-no-apply", "user_permission_grant")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("没有操作权限")));
        Mockito.verify(authorizationService, Mockito.never()).currentActorHasPermission("access.role.assign");
        Mockito.verify(authorizationService, Mockito.never()).requirePermissions("access.grant_request.apply", "access.role.assign");
        Mockito.verify(service, Mockito.never()).applyGrant(any());
    }

    // M002-F42：临时权限、角色、岗位申请不属三类，仍只核验申请权限，不判分配角色权限。
    @Test
    void otherGrantRequestTypesStillVerifyOnlyTheApplyPermission() throws Exception {
        for (String type : List.of("temporary_permission", "user_role", "user_position")) {
            Mockito.reset(service, authorizationService);

            mockMvc.perform(post("/api/v1/access/grant-requests")
                            .with(user("ops").roles("P0_SMOKE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(m002F42ApplyJson("f42-other", type)))
                    .andExpect(status().isOk());
            Mockito.verify(authorizationService).requirePermission("access.grant_request.apply");
            Mockito.verify(authorizationService, Mockito.never()).currentActorHasPermission(Mockito.anyString());
            Mockito.verify(authorizationService, Mockito.never()).requirePermissions("access.grant_request.apply", "access.role.assign");
            Mockito.verify(service).applyGrant(any());
        }
    }

    private String m002F42ApplyJson(String key, String type) {
        return """
                {"idempotency_key": "%s", "request_type": "%s", "target_user_id": "usr_target", "permission_codes": ["ops.production_readiness.check"]}
                """.formatted(key, type);
    }

    // M035-002：职级与定岗端点以路径里的账号为准，请求体里的 user_id 不生效。
    @Test
    void staffLevelAndPositionEndpointsUseThePathUserIdNotTheBodyUserId() throws Exception {
        mockMvc.perform(put("/api/v1/access/users/usr_path/staff-level")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key": "level-path", "user_id": "usr_body", "staff_level_id": "lvl_m1_lower"}
                                """))
                .andExpect(status().isOk());
        var level = org.mockito.ArgumentCaptor.forClass(com.complex.platform.access.dto.StaffLevelAssignmentRequest.class);
        Mockito.verify(service).assignStaffLevel(level.capture());
        org.assertj.core.api.Assertions.assertThat(level.getValue().userId()).isEqualTo("usr_path");

        mockMvc.perform(put("/api/v1/access/users/usr_path/position")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key": "position-path", "user_id": "usr_body", "position_code": "ops_clerk", "staff_level_id": "lvl_m1_lower"}
                                """))
                .andExpect(status().isOk());
        var position = org.mockito.ArgumentCaptor.forClass(com.complex.platform.access.dto.UserPositionAssignmentRequest.class);
        Mockito.verify(service).assignUserPosition(position.capture());
        org.assertj.core.api.Assertions.assertThat(position.getValue().userId()).isEqualTo("usr_path");
    }

    // M736-C1（操作员 2026-10-03 所选选项原文：「项目、城市项目公司、在用的域主管公司各列为独立条目，勾哪条只管哪条，互不连带。」）：
    // 建角色时逐个勾选项目的候选清单接口，与建角色同一门禁（access.role.manage）；三类条目各自成条；没有该权限 403、不进服务层。
    @Test
    void roleProjectOptionsGateOnRoleManageAndListTheThreeKindsAsSeparateEntries() throws Exception {
        when(service.listRoleProjectOptions()).thenReturn(new PageResponse<>(
                List.of(new RoleProjectOption("prj_a", "甲项目", "project"),
                        new RoleProjectOption("be_city", "城市项目公司", "project_company"),
                        new RoleProjectOption("be_domain", "域主管公司", "management_company")),
                1, 50, 3));

        mockMvc.perform(get("/api/v1/access/role-project-options")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.items[0].id").value("prj_a"))
                .andExpect(jsonPath("$.data.items[0].name").value("甲项目"))
                .andExpect(jsonPath("$.data.items[0].kind").value("project"))
                .andExpect(jsonPath("$.data.items[1].kind").value("project_company"))
                .andExpect(jsonPath("$.data.items[2].kind").value("management_company"));
        Mockito.verify(authorizationService).requirePermission("access.role.manage");

        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("access.role.manage");
        mockMvc.perform(get("/api/v1/access/role-project-options")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        Mockito.verify(service, Mockito.times(1)).listRoleProjectOptions();
    }

    // M736-C1（操作员 2026-10-01 说明「权限配置表格当中有一个可以查看全部项目选项……可以逐个独立的项目去选」）：建角色接口的两项勾选以
    // 蛇形字段进出——请求体的 view_all_projects、viewable_project_ids 原样到服务层，返回体带回同名字段；角色列表每行同样带两项。
    @Test
    void createRoleAndRoleListCarryTheViewableProjectTicksInSnakeCase() throws Exception {
        when(service.createRole(any())).thenReturn(new RoleRecord("role_ticked", "跨项目巡检", null, "project", "active",
                java.time.Instant.parse("2026-10-07T00:00:00Z"), List.of("ops.production_readiness.view"), "prj_a",
                false, List.of("prj_b", "be_city")));

        mockMvc.perform(post("/api/v1/access/roles")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "role-ticked",
                                  "role_name": "跨项目巡检",
                                  "permission_codes": ["ops.production_readiness.view"],
                                  "view_all_projects": false,
                                  "viewable_project_ids": ["prj_b", "be_city"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.view_all_projects").value(false))
                .andExpect(jsonPath("$.data.viewable_project_ids[0]").value("prj_b"))
                .andExpect(jsonPath("$.data.viewable_project_ids[1]").value("be_city"));
        Mockito.verify(authorizationService).requirePermission("access.role.manage");
        var request = org.mockito.ArgumentCaptor.forClass(com.complex.platform.access.dto.RoleRequest.class);
        Mockito.verify(service).createRole(request.capture());
        org.assertj.core.api.Assertions.assertThat(request.getValue().viewAllProjects()).isFalse();
        org.assertj.core.api.Assertions.assertThat(request.getValue().viewableProjectIds()).containsExactly("prj_b", "be_city");

        when(service.listRoles()).thenReturn(new PageResponse<>(
                List.of(new RoleRecord("role_all", "集团巡检", null, "project", "active",
                        java.time.Instant.parse("2026-10-07T00:00:00Z"), List.of(), null, true, List.of())),
                1, 50, 1));
        mockMvc.perform(get("/api/v1/access/roles")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].view_all_projects").value(true))
                .andExpect(jsonPath("$.data.items[0].viewable_project_ids").isEmpty());
    }

    // M736-C2（M736 第二步；操作员 2026-10-08 确认的治理地图第 703 行追加句：「可跨项目角色分配出去以后，可以直接在原角色上增加或减少
    // 可查看的项目」）：读现状、增减两个接口与建角色同一门禁（access.role.manage），增减须带提交编号；请求体的四个增减字段与最终审批人
    // 以蛇形字段原样到服务层，返回体的现状（两项勾选、减少在待审的条目、还没重新审批的持有人）以蛇形字段带回；缺提交编号 400、
    // 没有该权限 403，都不进服务层。
    @Test
    void viewableProjectsEndpointsRequireRoleManageAndIdempotencyKey() throws Exception {
        RoleViewableProjectsView view = new RoleViewableProjectsView("role_ticked", false, List.of("prj_b", "prj_c"), List.of("prj_b"),
                List.of(new RoleViewableProjectsView.PendingReapproval("usr_zhang", "张三", List.of("prj_c"), true)));
        when(service.getRoleViewableProjects("role_ticked")).thenReturn(view);
        when(service.updateRoleViewableProjects(Mockito.eq("role_ticked"), any())).thenReturn(view);

        mockMvc.perform(get("/api/v1/access/roles/role_ticked/viewable-projects")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.role_id").value("role_ticked"))
                .andExpect(jsonPath("$.data.view_all_projects").value(false))
                .andExpect(jsonPath("$.data.viewable_project_ids[0]").value("prj_b"))
                .andExpect(jsonPath("$.data.viewable_project_ids[1]").value("prj_c"))
                .andExpect(jsonPath("$.data.pending_removal_ids[0]").value("prj_b"))
                .andExpect(jsonPath("$.data.pending_reapprovals[0].user_id").value("usr_zhang"))
                .andExpect(jsonPath("$.data.pending_reapprovals[0].username").value("张三"))
                .andExpect(jsonPath("$.data.pending_reapprovals[0].viewable_ids[0]").value("prj_c"))
                .andExpect(jsonPath("$.data.pending_reapprovals[0].reapproval_pending").value(true));
        mockMvc.perform(put("/api/v1/access/roles/role_ticked/viewable-projects")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "ticks-change",
                                  "operator_note": "后台改可查看项目",
                                  "add_view_all_projects": false,
                                  "add_viewable_project_ids": ["prj_c"],
                                  "remove_view_all_projects": true,
                                  "remove_viewable_project_ids": ["prj_b"],
                                  "final_approver_id": "usr_boss"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role_id").value("role_ticked"))
                .andExpect(jsonPath("$.data.pending_removal_ids[0]").value("prj_b"));
        Mockito.verify(authorizationService, Mockito.times(2)).requirePermission("access.role.manage");
        var request = org.mockito.ArgumentCaptor.forClass(com.complex.platform.access.dto.RoleViewableProjectsRequest.class);
        Mockito.verify(service).updateRoleViewableProjects(Mockito.eq("role_ticked"), request.capture());
        org.assertj.core.api.Assertions.assertThat(request.getValue().idempotencyKey()).isEqualTo("ticks-change");
        org.assertj.core.api.Assertions.assertThat(request.getValue().addViewAllProjects()).isFalse();
        org.assertj.core.api.Assertions.assertThat(request.getValue().addViewableProjectIds()).containsExactly("prj_c");
        org.assertj.core.api.Assertions.assertThat(request.getValue().removeViewAllProjects()).isTrue();
        org.assertj.core.api.Assertions.assertThat(request.getValue().removeViewableProjectIds()).containsExactly("prj_b");
        org.assertj.core.api.Assertions.assertThat(request.getValue().finalApproverId()).isEqualTo("usr_boss");

        mockMvc.perform(put("/api/v1/access/roles/role_ticked/viewable-projects")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"add_viewable_project_ids": ["prj_c"]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));

        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("access.role.manage");
        mockMvc.perform(get("/api/v1/access/roles/role_ticked/viewable-projects")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        mockMvc.perform(put("/api/v1/access/roles/role_ticked/viewable-projects")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key": "ticks-denied", "add_viewable_project_ids": ["prj_c"]}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        Mockito.verify(service, Mockito.times(1)).getRoleViewableProjects(Mockito.anyString());
        Mockito.verify(service, Mockito.times(1)).updateRoleViewableProjects(Mockito.anyString(), any());
    }

    // M736-C3（M736 第二步；治理地图第 651 行收紧的是「员工单独授予、解除员工显式拒绝、岗位模板高危权限释放这三类授权申请」，不含本类）：
    // 重新审批可查看项目（role_project_reapproval）在入口只核申请权限——不判分配角色权限、不走三类申请的两码核验，随后进服务层，
    // 目标员工与角色编号原样到服务层。入口本批没有改（守护）：若把本类并进三类申请，红在 never 两条。
    @Test
    void grantApplyAcceptsTheReapprovalTypeWithApplyPermissionOnly() throws Exception {
        mockMvc.perform(post("/api/v1/access/grant-requests")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key": "reapproval-apply", "request_type": "role_project_reapproval", "target_user_id": "usr_zhang", "role_id": "role_ticked"}
                                """))
                .andExpect(status().isOk());
        Mockito.verify(authorizationService).requirePermission("access.grant_request.apply");
        Mockito.verify(authorizationService, Mockito.never()).currentActorHasPermission(Mockito.anyString());
        Mockito.verify(authorizationService, Mockito.never()).requirePermissions("access.grant_request.apply", "access.role.assign");
        var request = org.mockito.ArgumentCaptor.forClass(com.complex.platform.access.dto.AccessGrantApplyRequest.class);
        Mockito.verify(service).applyGrant(request.capture());
        org.assertj.core.api.Assertions.assertThat(request.getValue().requestType()).isEqualTo("role_project_reapproval");
        org.assertj.core.api.Assertions.assertThat(request.getValue().targetUserId()).isEqualTo("usr_zhang");
        org.assertj.core.api.Assertions.assertThat(request.getValue().roleId()).isEqualTo("role_ticked");
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        AccessService accessService() {
            return Mockito.mock(AccessService.class);
        }

        @Bean
        AuthorizationService authorizationService() {
            return Mockito.mock(AuthorizationService.class);
        }

        @Bean
        AuthService authService() {
            return new AuthService("", "");
        }
    }
}
