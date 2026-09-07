package com.complex.platform.auth;

import com.complex.platform.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * U 盘破窗登录入口（默认关；provision 公钥后启用）。挑战 → U 盘私钥签名 → 换取最高权限会话。
 * 公开路径（无需既有账号，这正是破窗的意义）；由 {@link MasterAuthorityService} 做一次性挑战 + Ed25519 验签 + 按来源限速留痕。
 *
 * <p>M008-F-04（PGM-1.15.1 破窗「恢复进入系统」）：破窗成功后与日常登录同一会话通道——令牌种入 HttpOnly Cookie + 下发 CSRF 令牌，
 * 后台 Web 由 Cookie 会话进入、JS 读不到令牌；响应体仍回传令牌供非浏览器恢复工具（sign-challenge.sh + curl）使用。
 */
@RestController
@RequestMapping("/api/v1/auth/master")
public class MasterAuthController {

    private final MasterAuthorityService service;

    public MasterAuthController(MasterAuthorityService service) {
        this.service = service;
    }

    @PostMapping("/challenge")
    public ApiResponse<Map<String, String>> challenge(HttpServletRequest httpRequest) {
        return ApiResponse.ok(Map.of("challenge", service.issueChallenge(sourceKey(httpRequest))));
    }

    @PostMapping("/login")
    public ApiResponse<AuthService.LoginResponse> login(@RequestBody MasterLoginRequest request,
                                                        HttpServletRequest httpRequest,
                                                        HttpServletResponse httpResponse) {
        AuthService.LoginResponse result = service.login(
                request == null ? null : request.challenge(),
                request == null ? null : request.signature(),
                sourceKey(httpRequest));
        AuthCookies.writeSession(httpRequest, httpResponse,
                result.accessToken(), result.refreshToken(), AuthCookies.newCsrfToken());
        return ApiResponse.ok(result);
    }

    /** 限速分桶来源：请求方地址（TLS 终止反代由 server.forward-headers-strategy=framework 还原真实来源）。 */
    private static String sourceKey(HttpServletRequest request) {
        String address = request == null ? null : request.getRemoteAddr();
        return address == null || address.isBlank() ? "unknown" : address;
    }

    public record MasterLoginRequest(String challenge, String signature) {
    }
}
