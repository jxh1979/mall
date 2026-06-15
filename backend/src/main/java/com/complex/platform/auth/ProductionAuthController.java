package com.complex.platform.auth;

import com.complex.platform.common.api.ApiResponse;
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
    public ApiResponse<AuthService.LoginResponse> login(@RequestBody AuthService.LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthService.LoginResponse> refresh(
            @RequestBody AuthService.RefreshTokenRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        return ApiResponse.ok(authService.refresh(request, bearerToken(authorizationHeader)));
    }

    @PostMapping("/logout")
    public ApiResponse<AuthService.LogoutResponse> logout(
            @RequestBody AuthService.LogoutRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        return ApiResponse.ok(authService.logout(request, bearerToken(authorizationHeader)));
    }

    private String bearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring("Bearer ".length()).trim();
    }
}
