package com.complex.platform.auth;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.security.BrowserDeviceRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"local", "dev", "test"})
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final BrowserDeviceRegistry browserDevices;

    public AuthController(AuthService authService, BrowserDeviceRegistry browserDevices) {
        this.authService = authService;
        this.browserDevices = browserDevices;
    }

    @PostMapping("/login")
    public ApiResponse<AuthService.LoginResponse> login(
            @RequestBody @jakarta.validation.Valid AuthService.LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        // 第 25 条 25-R2（第十八批裁决 19「第一次登录时系统发一个设备编号，存进 App 的系统安全存储并在后台登记」）：商户手机应用登录用
        // App 带来的编号（已登记沿用，没有或未登记发新号）作会话设备号，登录成功后登记；不发浏览器编号 Cookie。
        String appDeviceNo = AuthService.MERCHANT_APP.equals(request.terminalType()) ? browserDevices.appDevice(request.deviceId()) : null;
        AuthService.LoginResponse result = authService.login(appDeviceNo == null ? request
                : new AuthService.LoginRequest(request.loginName(), request.password(), request.otpCode(), appDeviceNo, request.terminalType()));
        // 后台 Web：令牌种入 HttpOnly Cookie（JS 读不到）+ 下发 CSRF 令牌。设备端仍读响应体令牌，行为不变。
        AuthCookies.writeSession(httpRequest, httpResponse,
                result.accessToken(), result.refreshToken(), AuthCookies.newCsrfToken());
        if (appDeviceNo != null) {
            browserDevices.register(appDeviceNo, result.user().id());
        } else if (BrowserDeviceRegistry.browserLogin(httpRequest, request.deviceId())) {
            // 第 23 条 23-R1（第十八批裁决 6）：网页登录发浏览器设备编号并登记；带 device_id 或报设备绑定型终端的设备端不发（见 browserLogin）。
            browserDevices.ensure(httpRequest, httpResponse, result.user().id());
        }
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
        // 刷新令牌：优先请求体（设备端），否则从 Cookie 读（后台 Web）。当前访问令牌同理。
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
        // 仅 Cookie 会话（Web）轮转后重写 Cookie；csrfToken 传 null 以保留登录下发的同一 XSRF-TOKEN。设备端不写 Cookie。
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
        // 先清 Cookie（无论 service 是否抛错，Web 会话都要下线）；设备端无 Cookie，清空无副作用。
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
