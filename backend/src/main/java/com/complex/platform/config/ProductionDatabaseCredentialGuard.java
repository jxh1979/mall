package com.complex.platform.config;

import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 生产数据库凭证守卫（3.4，fail-closed）：prod profile 启动时强制
 * ①运行时 app 角色与 owner/Flyway 角色分离（最小权限，app 被 REVOKE 掉 append-only 表写权）；
 * ②不得回退 dev 默认凭证（complex / complex_dev_pwd / 空）。任一违反即抛错、阻止启动。
 * 复用 {@link com.complex.platform.common.crypto.SigningSecretGuard} 的 prod fail-closed 思路。
 * 仅对确以共享 dev 库角色跑 prod-profile 的集成测试，可用
 * {@code complex.database.credential-separation.enforced=false} 显式豁免（默认强制）。
 */
@Component
@Profile("prod")
public class ProductionDatabaseCredentialGuard {

    private static final String DEV_DEFAULT_USER = "complex";
    private static final String DEV_DEFAULT_PASSWORD = "complex_dev_pwd";

    public ProductionDatabaseCredentialGuard(Environment environment) {
        boolean enforced = Boolean.parseBoolean(
                environment.getProperty("complex.database.credential-separation.enforced", "true"));
        if (!enforced) {
            return;
        }
        String appUser = environment.getProperty("spring.datasource.username", "");
        String appPassword = environment.getProperty("spring.datasource.password", "");
        String ownerUser = environment.getProperty("spring.flyway.user", "");
        String ownerPassword = environment.getProperty("spring.flyway.password", "");

        if (isDevDefault(appUser, appPassword) || isDevDefault(ownerUser, ownerPassword)) {
            throw new IllegalStateException(
                    "production database must not fall back to dev default credentials; "
                            + "set COMPLEX_DATABASE_APP_USERNAME/PASSWORD and COMPLEX_DATABASE_OWNER_USERNAME/PASSWORD");
        }
        if (appUser.equals(ownerUser) || appPassword.equals(ownerPassword)) {
            throw new IllegalStateException(
                    "production runtime app credentials must differ from owner/Flyway credentials (least privilege): "
                            + "app role is REVOKE-restricted on append-only tables, owner runs DDL/migrations");
        }
    }

    private boolean isDevDefault(String user, String password) {
        return user == null || user.isBlank() || DEV_DEFAULT_USER.equals(user)
                || password == null || password.isBlank() || DEV_DEFAULT_PASSWORD.equals(password);
    }
}
