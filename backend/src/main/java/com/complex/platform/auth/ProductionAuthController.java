package com.complex.platform.auth;

import com.complex.platform.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("prod")
@RequestMapping("/api/v1/auth")
public class ProductionAuthController {

    private final ProductionAuthService authService;

    public ProductionAuthController(ProductionAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthService.LoginResponse> login(
            @RequestBody @jakarta.validation.Valid AuthService.LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        AuthService.LoginResponse result = authService.login(request);
        // 后台 Web：令牌种入 HttpOnly Cookie（JS 读不到）+ 下发 CSRF 令牌。设备端仍读响应体令牌，行为不变。
        AuthCookies.writeSession(httpRequest, httpResponse,
                result.accessToken(), result.refreshToken(), AuthCookies.newCsrfToken());
        return ApiResponse.ok(result);
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthService.LoginResponse> refresh(
            @RequestBody(required = false) AuthService.RefreshTokenRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        boolean cookieSession = AuthCookies.readAccessToken(httpRequest) != null
                || AuthCookies.readRefreshToken(httpRequest) != null;
        String refreshToken = request != null && request.refreshToken() != null && !request.refreshToken().isBlank()
                ? request.refreshToken()
                : AuthCookies.readRefreshToken(httpRequest);
        String deviceId = request == null ? null : request.deviceId();
        String accessToken = bearerToken(authorizationHeader);
        if (accessToken == null) {
            accessToken = AuthCookies.readAccessToken(httpRequest);
        }
        AuthService.LoginResponse result = authService.refresh(
                new AuthService.RefreshTokenRequest(refreshToken, deviceId), accessToken);
        if (cookieSession) {
            AuthCookies.writeSession(httpRequest, httpResponse, result.accessToken(), result.refreshToken(), null);
        }
        return ApiResponse.ok(result);
    }

    @PostMapping("/logout")
    public ApiResponse<AuthService.LogoutResponse> logout(
            @RequestBody @jakarta.validation.Valid AuthService.LogoutRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        AuthCookies.clearSession(httpRequest, httpResponse);
        String accessToken = bearerToken(authorizationHeader);
        if (accessToken == null) {
            accessToken = AuthCookies.readAccessToken(httpRequest);
        }
        return ApiResponse.ok(authService.logout(request, accessToken));
    }

    private String bearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring("Bearer ".length()).trim();
    }
}
