package com.complex.platform.auth;

import com.complex.platform.common.api.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * U 盘破窗登录入口（默认关；provision 公钥后启用）。挑战 → U 盘私钥签名 → 换取最高权限会话。
 * 公开路径（无需既有账号，这正是破窗的意义）；由 {@link MasterAuthorityService} 做一次性挑战 + Ed25519 验签 + 限速留痕。
 */
@RestController
@RequestMapping("/api/v1/auth/master")
public class MasterAuthController {

    private final MasterAuthorityService service;

    public MasterAuthController(MasterAuthorityService service) {
        this.service = service;
    }

    @PostMapping("/challenge")
    public ApiResponse<Map<String, String>> challenge() {
        return ApiResponse.ok(Map.of("challenge", service.issueChallenge()));
    }

    @PostMapping("/login")
    public ApiResponse<AuthService.LoginResponse> login(@RequestBody MasterLoginRequest request) {
        return ApiResponse.ok(service.login(
                request == null ? null : request.challenge(),
                request == null ? null : request.signature()));
    }

    public record MasterLoginRequest(String challenge, String signature) {
    }
}
