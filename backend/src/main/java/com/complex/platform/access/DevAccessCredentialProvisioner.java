package com.complex.platform.access;

// 文件版本：V0.02

import com.complex.platform.auth.AuthService;
import com.complex.platform.common.api.ApiException;
import java.time.Instant;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 商户专区一期:dev/local 的凭证桥——access_users 建号后同步到内存 AuthService,
 * 让 dev 后台能用真实账号(含 merchant 归属账号)登录验收;prod 由 JdbcAccessCredentialProvisioner 落库。
 * 身份要素(accountType/organizationId/merchantId)从库回读,保证与权限/scope 判定同源。
 */
@Service
@Profile({"local", "dev"})
class DevAccessCredentialProvisioner implements AccessCredentialProvisioner {

    private final AuthService authService;
    private final AccessRepository accessRepository;

    DevAccessCredentialProvisioner(AuthService authService, AccessRepository accessRepository) {
        this.authService = authService;
        this.accessRepository = accessRepository;
    }

    @Override
    public void changeStatus(String userId, String status, Instant now) {
        authService.changeUserStatus(userId, status);
    }

    @Override
    public void provisionCredential(String userId, String password, String status, Instant now) {
        if (userId == null || userId.isBlank() || password == null || password.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "账号和初始密码不能为空");
        }
        if (!java.util.Set.of("active", "invited", "disabled").contains(status == null ? "" : status)) {
            throw ApiException.badRequest("INVALID_ACCOUNT_STATUS", "无法识别的账号状态");
        }
        AccessUser user = accessRepository.findUserById(userId)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "账号不存在"));
        authService.provisionUser(
                user.id(),
                user.username(),
                password,
                user.accountType(),
                user.organizationId(),
                user.merchantId(),
                status
        );
    }
}
