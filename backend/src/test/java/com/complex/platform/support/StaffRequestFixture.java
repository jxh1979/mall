package com.complex.platform.support;

// 文件版本：V0.01

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import com.complex.platform.access.AccessActor;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * ROOT-03 B 段券与展示组：后台账号造数（真库建账号、角色与权限码），请求按后台网页真实形状（X-Terminal-Type: web）。
 * accountType＝staff 或 platform_admin；merchantId 为空＝不属任何商户。
 */
public final class StaffRequestFixture {

    private final JdbcTemplate jdbc;
    private final Set<String> created = new HashSet<>();

    public StaffRequestFixture(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public RequestPostProcessor staff(String userId, String accountType, String projectId, String merchantId,
            String... permissions) {
        if (created.add(userId)) {
            String role = "role_" + userId;
            jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at)"
                    + " VALUES (?, ?, 'B14 券与展示组回归', 'project', 'active', now())", role, role);
            for (String code : permissions) {
                jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, code);
            }
            jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                    + " VALUES (?, ?, ?, ?, ?, 'active', now())", userId, userId, accountType, projectId, merchantId);
            jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
        }
        AccessActor actor = new AccessActor(userId, userId, accountType, projectId, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
