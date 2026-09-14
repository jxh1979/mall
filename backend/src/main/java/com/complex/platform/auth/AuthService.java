package com.complex.platform.auth;

import static com.complex.platform.common.Strings.requireText;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
@Profile({"local", "dev", "test"})
public class AuthService implements SessionIssuer {

    private final ConcurrentMap<String, UserRecord> usersByUsername = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UserRecord> usersById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SessionRecord> sessionsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> accessTokensToSessionId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> refreshTokensToSessionId = new ConcurrentHashMap<>();
    private final AuditEventService auditEventService;
    // Salted, work-factored hashing — never store unsalted digests, even for the dev provider.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
    private final Duration accessTokenTtl;

    @Autowired
    public AuthService(
            @Value("${complex.smoke.admin.username:${P0_ADMIN_USERNAME:}}") String seedUsername,
            @Value("${complex.smoke.admin.password:${P0_ADMIN_PASSWORD:}}") String seedPassword,
            AuditEventService auditEventService,
            @Value("${complex.auth.access-token-ttl:PT30M}") Duration accessTokenTtl
    ) {
        this.auditEventService = auditEventService;
        // null → default 30m; explicit (incl. short/negative) values are respected so expiry is testable.
        this.accessTokenTtl = accessTokenTtl == null ? Duration.ofMinutes(30) : accessTokenTtl;
        if (!isBlank(seedUsername) && !isBlank(seedPassword)) {
            createUser(new CreateUserCommand(
                    seedUsername,
                    seedPassword,
                    "platform_admin",
                    "seed-org",
                    "seed-merchant",
                    "active"
            ));
        }
    }

    public AuthService(String seedUsername, String seedPassword, AuditEventService auditEventService) {
        this(seedUsername, seedPassword, auditEventService, Duration.ofMinutes(30));
    }

    public AuthService(String seedUsername, String seedPassword) {
        this(seedUsername, seedPassword, new AuditEventService(event -> event));
    }

    public UserRecord createUser(CreateUserCommand command) {
        if (command.username() == null || command.username().isBlank()) {
            throw ApiException.badRequest("USERNAME_REQUIRED", "username is required");
        }
        if (command.password() == null || command.password().isBlank()) {
            throw ApiException.badRequest("PASSWORD_REQUIRED", "password is required");
        }

        return usersByUsername.compute(command.username(), (username, existing) -> {
            if (existing != null) {
                throw ApiException.conflict("USER_ALREADY_EXISTS", "该账号已存在");
            }
            UserRecord user = new UserRecord(
                    "usr_" + UUID.randomUUID(),
                    command.username(),
                    passwordEncoder.encode(command.password()),
                    blankToDefault(command.accountType(), "staff"),
                    command.organizationId(),
                    command.merchantId(),
                    blankToDefault(command.status(), "active"),
                    Instant.now()
            );
            usersById.put(user.id(), user);
            return user;
        });
    }

    /**
     * dev/local 测试基建(商户专区一期):为 access_users 建的账号在内存认证层发放可登录凭证。
     * id 必须与库一致——权限装配(user_roles)按库 id 查、merchant scope 按 actor.merchantId 判,
     * 两侧对齐后 dev 后台才能用真实账号验收商户专区。幂等:重复发放按新密码覆盖(改密语义)。
     */
    public void provisionUser(String id, String username, String password, String accountType,
            String organizationId, String merchantId, String status) {
        if (isBlank(id) || isBlank(username) || isBlank(password)) {
            return;
        }
        UserRecord user = new UserRecord(
                id,
                username,
                passwordEncoder.encode(password),
                blankToDefault(accountType, "staff"),
                organizationId,
                merchantId,
                blankToDefault(status, "active"),
                Instant.now()
        );
        usersById.put(id, user);
        usersByUsername.put(username, user);
    }

    public LoginResponse login(LoginRequest request) {
        try {
            requireText(request == null ? null : request.loginName(), "login_name");
            requireText(request.password(), "password");

            UserRecord user = Optional.ofNullable(usersByUsername.get(request.loginName()))
                    .orElseThrow(() -> ApiException.unauthorized("INVALID_CREDENTIALS", "账号或密码不正确"));

            if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
                throw ApiException.unauthorized("INVALID_CREDENTIALS", "账号或密码不正确");
            }
            if (!"active".equals(user.status())) {
                throw ApiException.unauthorized("USER_NOT_ACTIVE", "该账号已停用");
            }

            SessionRecord session = createSession(user, request.deviceId(), request.terminalType());
            auditEventService.recordSuccess(
                    "auth.login.succeeded",
                    "auth.login",
                    "auth-session",
                    session.id(),
                    null,
                    null,
                    Map.of(
                            "user_id", user.id(),
                            "session_id", session.id(),
                            "device_id", blankToDefault(request.deviceId(), "unknown"),
                            "terminal_type", blankToDefault(request.terminalType(), "web")
                    )
            );
            return responseFor(session, user);
        } catch (ApiException exception) {
            auditEventService.recordFailure(
                    "auth.login.failed",
                    "auth.login",
                    "auth-session",
                    null,
                    null,
                    null,
                    exception.code()
            );
            throw exception;
        }
    }

    public LoginResponse refresh(RefreshTokenRequest request, String currentAccessToken) {
        try {
            requireText(request == null ? null : request.refreshToken(), "refresh_token");
            String refreshSessionId = refreshTokensToSessionId.get(request.refreshToken());
            SessionRecord session = refreshSessionId == null ? null : sessionsById.get(refreshSessionId);
            if (session == null || !"active".equals(session.status())) {
                throw ApiException.unauthorized("REFRESH_TOKEN_INVALID", "登录状态已失效，请重新登录");
            }

            requireText(currentAccessToken, "access_token");
            String accessSessionId = accessTokensToSessionId.get(currentAccessToken);
            if (!session.id().equals(accessSessionId)) {
                throw ApiException.unauthorized("SESSION_MISMATCH", "登录凭证与当前会话不匹配，请重新登录");
            }
            requireText(request.deviceId(), "device_id");
            if (!request.deviceId().equals(session.deviceId())) {
                throw ApiException.unauthorized("SESSION_MISMATCH", "设备与当前会话不匹配，请重新登录");
            }
            if (SessionIssuer.MASTER_USER_ID.equals(session.userId())) {
                throw ApiException.unauthorized("MASTER_SESSION_REFRESH_FORBIDDEN", "最高权限须重新插授权 U 盘破窗，不能续期");
            }

            UserRecord user = usersById.get(session.userId());
            if (user == null || !"active".equals(user.status())) {
                throw ApiException.unauthorized("USER_NOT_ACTIVE", "该账号已停用");
            }

            SessionRecord refreshed = rotateSessionTokens(session);
            auditEventService.recordSuccess(
                    "auth.token.refreshed",
                    "auth.refresh",
                    "auth-session",
                    refreshed.id(),
                    null,
                    null,
                    Map.of(
                            "user_id", user.id(),
                            "session_id", refreshed.id(),
                            "device_id", blankToDefault(refreshed.deviceId(), "unknown")
                    )
            );
            return responseFor(refreshed, user);
        } catch (ApiException exception) {
            auditEventService.recordFailure(
                    "auth.token_refresh_failed",
                    "auth.refresh",
                    "auth-session",
                    null,
                    null,
                    null,
                    exception.code()
            );
            throw exception;
        }
    }

    public LogoutResponse logout(LogoutRequest request, String currentAccessToken) {
        try {
            requireText(request == null ? null : request.idempotencyKey(), "idempotency_key");
            requireText(request.sessionId(), "session_id");
            requireText(currentAccessToken, "access_token");

            String accessSessionId = accessTokensToSessionId.get(currentAccessToken);
            SessionRecord session = accessSessionId == null ? null : sessionsById.get(accessSessionId);
            if (session == null || !"active".equals(session.status())) {
                throw ApiException.unauthorized("ACCESS_TOKEN_INVALID", "登录凭证无效，请重新登录");
            }
            if (!session.id().equals(request.sessionId())) {
                throw ApiException.unauthorized("SESSION_MISMATCH", "会话标识与当前登录凭证不匹配");
            }
            if (!isBlank(request.deviceId()) && !request.deviceId().equals(session.deviceId())) {
                throw ApiException.unauthorized("SESSION_MISMATCH", "设备与当前会话不匹配，请重新登录");
            }

            accessTokensToSessionId.remove(session.accessToken());
            refreshTokensToSessionId.remove(session.refreshToken());
            SessionRecord loggedOut = new SessionRecord(
                    session.id(),
                    session.userId(),
                    session.accessToken(),
                    session.refreshToken(),
                    session.deviceId(),
                    session.terminalType(),
                    "logged_out",
                    session.createdAt(),
                    Instant.now(),
                    session.accessExpiresAt()
            );
            sessionsById.put(loggedOut.id(), loggedOut);
            auditEventService.recordSuccess(
                    "auth.logout",
                    "auth.logout",
                    "auth-session",
                    loggedOut.id(),
                    null,
                    request.idempotencyKey(),
                    Map.of(
                            "session_id", loggedOut.id(),
                            "device_id", blankToDefault(loggedOut.deviceId(), "unknown")
                    )
            );
            return new LogoutResponse(loggedOut.id(), loggedOut.status(), loggedOut.updatedAt());
        } catch (ApiException exception) {
            auditEventService.recordFailure(
                    "auth.logout_failed",
                    "auth.logout",
                    "auth-session",
                    request == null ? null : request.sessionId(),
                    null,
                    request == null ? null : request.idempotencyKey(),
                    exception.code()
            );
            throw exception;
        }
    }

    public Optional<UserRecord> authenticateToken(String token) {
        if (isBlank(token)) {
            return Optional.empty();
        }
        return Optional.ofNullable(accessTokensToSessionId.get(token))
                .map(sessionsById::get)
                .filter(session -> "active".equals(session.status()))
                .filter(session -> session.accessExpiresAt() == null || Instant.now().isBefore(session.accessExpiresAt()))
                .map(SessionRecord::userId)
                .map(usersById::get);
    }

    public void attachSession(HttpServletRequest request, String token) {
        if (request == null || isBlank(token)) {
            return;
        }
        SessionRecord session = Optional.ofNullable(accessTokensToSessionId.get(token)).map(sessionsById::get).orElse(null);
        if (session == null || !"active".equals(session.status())) {
            return;
        }
        request.setAttribute("auth.sessionId", session.id());
        request.setAttribute("auth.deviceId", session.deviceId());
    }

    @Override
    public LoginResponse issueMasterSession(String deviceId) {
        requireText(deviceId, "device_id");
        UserRecord user = usersById.computeIfAbsent(SessionIssuer.MASTER_USER_ID, id ->
                new UserRecord(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                        "!usb-only-no-password", MASTER_ACCOUNT_TYPE, "platform-company", null, "active", Instant.now()));
        if (!MASTER_ACCOUNT_TYPE.equals(user.accountType())) {
            user = new UserRecord(user.id(), user.username(), user.passwordHash(), MASTER_ACCOUNT_TYPE,
                    user.organizationId(), user.merchantId(), user.status(), user.createdAt());
            usersById.put(user.id(), user);
        }
        usersByUsername.putIfAbsent(SessionIssuer.MASTER_USERNAME, user);
        SessionRecord session = createSession(user, deviceId, "recovery");
        auditEventService.recordSuccess("auth.master.session_issued", "auth.master", "auth-session",
                session.id(), null, null, java.util.Map.of("user_id", user.id()));
        return responseFor(session, user);
    }

    private SessionRecord createSession(UserRecord user, String deviceId, String terminalType) {
        Instant now = Instant.now();
        SessionRecord session = new SessionRecord(
                "sess_" + UUID.randomUUID(),
                user.id(),
                newAccessToken(),
                newRefreshToken(),
                blankToDefault(deviceId, "unknown"),
                blankToDefault(terminalType, "web"),
                "active",
                now,
                now,
                now.plus(accessTokenTtl)
        );
        sessionsById.put(session.id(), session);
        accessTokensToSessionId.put(session.accessToken(), session.id());
        refreshTokensToSessionId.put(session.refreshToken(), session.id());
        return session;
    }

    private SessionRecord rotateSessionTokens(SessionRecord session) {
        accessTokensToSessionId.remove(session.accessToken());
        refreshTokensToSessionId.remove(session.refreshToken());
        Instant now = Instant.now();
        SessionRecord refreshed = new SessionRecord(
                session.id(),
                session.userId(),
                newAccessToken(),
                newRefreshToken(),
                session.deviceId(),
                session.terminalType(),
                "active",
                session.createdAt(),
                now,
                now.plus(accessTokenTtl)
        );
        sessionsById.put(refreshed.id(), refreshed);
        accessTokensToSessionId.put(refreshed.accessToken(), refreshed.id());
        refreshTokensToSessionId.put(refreshed.refreshToken(), refreshed.id());
        return refreshed;
    }

    private LoginResponse responseFor(SessionRecord session, UserRecord user) {
        return new LoginResponse(
                session.accessToken(),
                session.refreshToken(),
                session.id(),
                "Bearer",
                user.toApiUser()
        );
    }

    private String newAccessToken() {
        return "p0_access_" + UUID.randomUUID();
    }

    private String newRefreshToken() {
        return "p0_refresh_" + UUID.randomUUID();
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record LoginRequest(
            @JsonAlias("username")
            String loginName,
            String password,
            String otpCode,
            String deviceId,
            String terminalType
    ) {
    }

    public record LoginResponse(
            String accessToken,
            String refreshToken,
            String sessionId,
            String tokenType,
            ApiUser user
    ) {
    }

    public record RefreshTokenRequest(
            String refreshToken,
            String deviceId
    ) {
    }

    public record LogoutRequest(
            String idempotencyKey,
            String sessionId,
            String deviceId,
            String operatorNote
    ) {
    }

    public record LogoutResponse(
            String sessionId,
            String status,
            Instant loggedOutAt
    ) {
    }

    public record ApiUser(
            String id,
            String username,
            String accountType,
            String organizationId,
            String merchantId
    ) {
    }

    public record CreateUserCommand(
            String username,
            String password,
            String accountType,
            String organizationId,
            String merchantId,
            String status
    ) {
    }

    public record UserRecord(
            String id,
            String username,
            String passwordHash,
            String accountType,
            String organizationId,
            String merchantId,
            String status,
            Instant createdAt
    ) {
        ApiUser toApiUser() {
            return new ApiUser(id, username, accountType, organizationId, merchantId);
        }
    }

    private record SessionRecord(
            String id,
            String userId,
            String accessToken,
            String refreshToken,
            String deviceId,
            String terminalType,
            String status,
            Instant createdAt,
            Instant updatedAt,
            Instant accessExpiresAt
    ) {
    }
}
