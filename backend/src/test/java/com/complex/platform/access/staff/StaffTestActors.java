package com.complex.platform.access.staff;

// 文件版本：V0.06

import com.complex.platform.access.*;
import com.complex.platform.auth.SessionIssuer;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** 测试明确写入角色授权，不依赖管理员类型自动拥有敏感权限。事务结束自动回滚。 */
final class StaffTestActors {
    static void grant(JdbcTemplate jdbc, AccessRepository access, String id, String project) {
        if (access.findUserById(id).isEmpty()) access.createUser(new AccessUser(id,id,"staff",project,null,"active",Instant.now()));
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES('role_m010_fixture','M010 测试管理','M010 测试角色','platform','active',now()) ON CONFLICT(id) DO NOTHING");
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT 'role_m010_fixture',permission_code FROM permissions WHERE permission_code LIKE 'staff.%' OR permission_code='governance.supervisor_review.approve' ON CONFLICT DO NOTHING");
        access.assignUserRole(id,"role_m010_fixture");
    }
    static void actor(String id, String type, String project, String merchant) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(id,id,type,project,merchant,"active"),"test",List.of()));
    }
    /**
     * XM-M001-AF：判权限时会核对会话身份与账号行。写入会话后调用——把当前会话账号自己的行对齐到本次会话的账号类型、项目与商户
     * （库里没有该账号行则不动；不动其他账号的行）。
     */
    static void alignRowToSession(JdbcTemplate jdbc) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof com.complex.platform.auth.AuthService.UserRecord user) {
            principal = new AccessActor(user.id(), user.username(), user.accountType(), user.organizationId(), user.merchantId(), user.status());
        }
        if (principal instanceof AccessActor actor) {
            jdbc.update("UPDATE access_users SET account_type=?, organization_id=?, merchant_id=? WHERE id=?",
                    actor.accountType(), actor.organizationId(), actor.merchantId(), actor.id());
        }
    }
    static void master() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(SessionIssuer.MASTER_USER_ID,SessionIssuer.MASTER_USERNAME,com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE,null,null,"active"),"test",List.of()));
    }

    /** U05-003/U11-007：模拟私人手机安装时生成的 EC 密钥对 → [Base64(X.509 DER 公钥), SHA-256(DER) 十六进制指纹]。 */
    static String[] deviceProof() {
        try {
            var generator = java.security.KeyPairGenerator.getInstance("EC");
            generator.initialize(256);
            byte[] der = generator.generateKeyPair().getPublic().getEncoded();
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(der);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return new String[] { java.util.Base64.getEncoder().encodeToString(der), sb.toString() };
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
