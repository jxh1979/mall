package com.complex.platform.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"local", "dev", "test"})
public class AuthService {

    private final ConcurrentMap<String, UserRecord> usersByUsername = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UserRecord> usersById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SessionRecord> sessionsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> accessTokensToSessionId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> refreshTokensToSessionId = new ConcurrentHashMap<>();
    private final AuditEventService auditEventService;

    @Autowired
    public AuthService(
            @Value("${complex.smoke.admin.username:${P0_ADMIN_USERNAME:}}") String seedUsername,
            @Value("${complex.smoke.admin.password:${P0_ADMIN_PASSWORD:}}") String seedPassword,
            AuditEventService auditEventService
    ) {
        this.auditEventService = auditEventService;
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
                throw ApiException.conflict("USER_ALREADY_EXISTS", "user already exists");
            }
            UserRecord user = new UserRecord(
                    "usr_" + UUID.randomUUID(),
                    command.username(),
                    hash(command.password()),
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

    public LoginResponse login(LoginRequest request) {
        try {
            requireText(request == null ? null : request.loginName(), "login_name");
            requireText(request.password(), "password");

            UserRecord user = Optional.ofNullable(usersByUsername.get(request.loginName()))
                    .orElseThrow(() -> ApiException.unauthorized("INVALID_CREDENTIALS", "invalid username or password"));

            if (!constantTimeEquals(user.passwordHash(), hash(request.password()))) {
                throw ApiException.unauthorized("INVALID_CREDENTIALS", "invalid username or password");
            }
            if (!"active".equals(user.status())) {
                throw ApiException.unauthorized("USER_NOT_ACTIVE", "user is not active");
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
                throw ApiException.unauthorized("REFRESH_TOKEN_INVALID", "refresh token is invalid");
            }

            requireText(currentAccessToken, "access_token");
            String accessSessionId = accessTokensToSessionId.get(currentAccessToken);
            if (!session.id().equals(accessSessionId)) {
                throw ApiException.unauthorized("SESSION_MISMATCH", "refresh token does not match current session");
            }
            if (!isBlank(request.deviceId()) && !request.deviceId().equals(session.deviceId())) {
                throw ApiException.unauthorized("SESSION_MISMATCH", "device does not match current session");
            }

            UserRecord user = usersById.get(session.userId());
            if (user == null || !"active".equals(user.status())) {
                throw ApiException.unauthorized("USER_NOT_ACTIVE", "user is not active");
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
                throw ApiException.unauthorized("ACCESS_TOKEN_INVALID", "access token is invalid");
            }
            if (!session.id().equals(request.sessionId())) {
                throw ApiException.unauthorized("SESSION_MISMATCH", "session_id does not match current token");
            }
            if (!isBlank(request.deviceId()) && !request.deviceId().equals(session.deviceId())) {
                throw ApiException.unauthorized("SESSION_MISMATCH", "device does not match current session");
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
                    Instant.now()
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
                .map(SessionRecord::userId)
                .map(usersById::get);
    }

    private SessionRecord createSession(UserRecord user, String deviceId, String terminalType) {
        SessionRecord session = new SessionRecord(
                "sess_" + UUID.randomUUID(),
                user.id(),
                newAccessToken(),
                newRefreshToken(),
                blankToDefault(deviceId, "unknown"),
                blankToDefault(terminalType, "web"),
                "active",
                Instant.now(),
                Instant.now()
        );
        sessionsById.put(session.id(), session);
        accessTokensToSessionId.put(session.accessToken(), session.id());
        refreshTokensToSessionId.put(session.refreshToken(), session.id());
        return session;
    }

    private SessionRecord rotateSessionTokens(SessionRecord session) {
        accessTokensToSessionId.remove(session.accessToken());
        refreshTokensToSessionId.remove(session.refreshToken());
        SessionRecord refreshed = new SessionRecord(
                session.id(),
                session.userId(),
                newAccessToken(),
                newRefreshToken(),
                session.deviceId(),
                session.terminalType(),
                "active",
                session.createdAt(),
                Instant.now()
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

    private void requireText(String value, String fieldName) {
        if (isBlank(value)) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("password hashing unavailable", exception);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                actual.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
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
            Instant updatedAt
    ) {
    }
}
