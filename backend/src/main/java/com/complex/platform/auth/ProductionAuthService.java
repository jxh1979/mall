package com.complex.platform.auth;
import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import jakarta.annotation.PostConstruct;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("prod")
public class ProductionAuthService implements SessionIssuer {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AuditEventService auditEventService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;
    private final String seedUsername;
    private final String seedPassword;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;
    private final int maxFailedAttempts;
    private final Duration lockDuration;

    public ProductionAuthService(
            NamedParameterJdbcTemplate jdbcTemplate,
            AuditEventService auditEventService,
            @Value("${complex.auth.production.seed-admin.username:}") String seedUsername,
            @Value("${complex.auth.production.seed-admin.password:}") String seedPassword,
            @Value("${complex.auth.access-token-ttl:PT30M}") Duration accessTokenTtl,
            @Value("${complex.auth.refresh-token-ttl:PT8H}") Duration refreshTokenTtl,
            @Value("${complex.auth.failed-login.max-attempts:5}") int maxFailedAttempts,
            @Value("${complex.auth.failed-login.lock-duration:PT15M}") Duration lockDuration
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditEventService = auditEventService;
        this.secureRandom = new SecureRandom();
        this.passwordEncoder = new BCryptPasswordEncoder(10, secureRandom);
        this.seedUsername = seedUsername;
        this.seedPassword = seedPassword;
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
        this.maxFailedAttempts = Math.max(1, maxFailedAttempts);
        this.lockDuration = lockDuration.isNegative() || lockDuration.isZero() ? Duration.ofMinutes(15) : lockDuration;
    }

    @PostConstruct
    @Transactional
    public void seedAdminWhenConfigured() {
        if (isBlank(seedUsername) || isBlank(seedPassword)) {
            return;
        }
        requireStrongSeedPassword(seedPassword);
        if (SessionIssuer.MASTER_USERNAME.equals(seedUsername.trim())) {
            // M008-G2-02：保留用户名只属于 U 盘破窗签发的保留超管，种子管理员不得占用。
            throw new IllegalStateException("生产种子管理员不得使用保留用户名 master-authority（该身份只能凭 U 盘破窗签发）");
        }

        Instant now = Instant.now();
        String userId = jdbcTemplate.queryForObject("""
                INSERT INTO access_users (
                    id, username, account_type, organization_id, merchant_id, status, created_at
                )
                VALUES (
                    :id, :username, 'platform_admin', 'platform-company', NULL, 'active', :now
                )
                ON CONFLICT (username) DO UPDATE SET
                    account_type = 'platform_admin',
                    organization_id = COALESCE(access_users.organization_id, 'platform-company'),
                    status = 'active'
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("id", deterministicUserId(seedUsername))
                .addValue("username", seedUsername)
                .addValue("now", timestampWithTimeZone(now)), String.class);

        jdbcTemplate.update("""
                INSERT INTO auth_credentials (
                    user_id, password_hash, password_algo, failed_attempt_count, locked_until,
                    password_changed_at, status, created_at, updated_at
                )
                VALUES (
                    :user_id, :password_hash, 'bcrypt', 0, NULL, :now, 'active', :now, :now
                )
                ON CONFLICT (user_id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("user_id", userId)
                .addValue("password_hash", passwordEncoder.encode(seedPassword))
                .addValue("now", timestampWithTimeZone(now)));

        jdbcTemplate.update("""
                INSERT INTO user_roles (user_id, role_id)
                SELECT :user_id, 'role_platform_admin'
                 WHERE EXISTS (SELECT 1 FROM roles WHERE id = 'role_platform_admin')
                ON CONFLICT (user_id, role_id) DO NOTHING
                """, Map.of("user_id", userId));
    }

    /**
     * M008-A1-03（BSM-1.26.1「上线生产必须换掉开发默认的弱口令」）：生产种子管理员口令拒绝开发默认（admin-pass / change-me /
     * admin / password）与短于 12 位的值，启动即 fail-closed，与 SigningSecretGuard / ProductionDatabaseCredentialGuard 同范式。
     */
    static void requireStrongSeedPassword(String password) {
        String value = password == null ? "" : password.trim();
        String lower = value.toLowerCase(java.util.Locale.ROOT);
        if (value.length() < 12 || lower.contains("change-me") || lower.equals("admin-pass")
                || lower.equals("admin") || lower.equals("password")) {
            throw new IllegalStateException(
                    "生产种子管理员口令过弱或为开发默认值：complex.auth.production.seed-admin.password 须为 12 位以上强口令");
        }
    }

    @Transactional
    public AuthService.LoginResponse login(AuthService.LoginRequest request) {
        try {
            requireText(request == null ? null : request.loginName(), "login_name");
            requireText(request.password(), "password");

            CredentialRecord credential = findCredentialByUsername(request.loginName())
                    .orElseThrow(() -> ApiException.unauthorized("INVALID_CREDENTIALS", "账号或密码不正确"));
            Instant now = Instant.now();
            if (!"active".equals(credential.userStatus())) {
                throw ApiException.unauthorized("USER_NOT_ACTIVE", "该账号已停用");
            }
            if (credential.lockedUntil() != null && now.isBefore(credential.lockedUntil())) {
                throw ApiException.unauthorized("ACCOUNT_LOCKED", "账号已被临时锁定，请稍后再试");
            }
            if (!"active".equals(credential.credentialStatus()) && !"locked".equals(credential.credentialStatus())) {
                throw ApiException.unauthorized("USER_NOT_ACTIVE", "该账号已停用");
            }
            if (!passwordEncoder.matches(request.password(), credential.passwordHash())) {
                registerFailedLogin(credential, now);
                throw ApiException.unauthorized("INVALID_CREDENTIALS", "账号或密码不正确");
            }

            resetFailedLogin(credential.userId(), now);
            SessionRecord session = createSession(credential.toActor(), request.deviceId(), request.terminalType(), now);
            auditEventService.recordSuccess(
                    "auth.login.succeeded",
                    "auth.login",
                    "auth-session",
                    session.id(),
                    null,
                    null,
                    Map.of(
                            "user_id", credential.userId(),
                            "session_id", session.id(),
                            "device_id", session.deviceId(),
                            "terminal_type", session.terminalType()
                    )
            );
            return responseFor(session, credential.toApiUser());
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

    @Transactional
    public AuthService.LoginResponse refresh(AuthService.RefreshTokenRequest request, String currentAccessToken) {
        try {
            requireText(request == null ? null : request.refreshToken(), "refresh_token");
            requireText(currentAccessToken, "access_token");

            Instant now = Instant.now();
            SessionWithUser session = findActiveSessionByRefreshToken(request.refreshToken())
                    .orElseThrow(() -> ApiException.unauthorized("REFRESH_TOKEN_INVALID", "登录状态已失效，请重新登录"));
            if (now.isAfter(session.refreshExpiresAt())) {
                expireSession(session.id(), now);
                throw ApiException.unauthorized("REFRESH_TOKEN_INVALID", "登录已过期，请重新登录");
            }
            if (!session.accessTokenHash().equals(sha256Hex(currentAccessToken))) {
                throw ApiException.unauthorized("SESSION_MISMATCH", "登录凭证与当前会话不匹配，请重新登录");
            }
            requireText(request.deviceId(), "device_id");
            if (!request.deviceId().equals(session.deviceId())) {
                throw ApiException.unauthorized("SESSION_MISMATCH", "设备与当前会话不匹配，请重新登录");
            }
            if (SessionIssuer.MASTER_USER_ID.equals(session.userId())) {
                throw ApiException.unauthorized("MASTER_SESSION_REFRESH_FORBIDDEN", "最高权限须重新插授权 U 盘破窗，不能续期");
            }
            if (!"active".equals(session.userStatus())) {
                throw ApiException.unauthorized("USER_NOT_ACTIVE", "该账号已停用");
            }
            // A locked/disabled credential must not be able to extend session life via refresh either.
            if (!credentialUsable(session, now)) {
                throw ApiException.unauthorized("ACCOUNT_LOCKED", "账号已被临时锁定，请稍后再试");
            }

            SessionRecord refreshed = rotateSessionTokens(session, now);
            auditEventService.recordSuccess(
                    "auth.token.refreshed",
                    "auth.refresh",
                    "auth-session",
                    refreshed.id(),
                    null,
                    null,
                    Map.of(
                            "user_id", session.userId(),
                            "session_id", refreshed.id(),
                            "device_id", refreshed.deviceId()
                    )
            );
            return responseFor(refreshed, session.toApiUser());
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

    @Transactional
    public AuthService.LogoutResponse logout(AuthService.LogoutRequest request, String currentAccessToken) {
        try {
            requireText(request == null ? null : request.idempotencyKey(), "idempotency_key");
            requireText(request.sessionId(), "session_id");
            requireText(currentAccessToken, "access_token");

            Instant now = Instant.now();
            SessionWithUser session = findActiveSessionByAccessToken(currentAccessToken)
                    .orElseThrow(() -> ApiException.unauthorized("ACCESS_TOKEN_INVALID", "登录凭证无效，请重新登录"));
            if (!session.id().equals(request.sessionId())) {
                throw ApiException.unauthorized("SESSION_MISMATCH", "会话标识与当前登录凭证不匹配");
            }
            if (!isBlank(request.deviceId()) && !request.deviceId().equals(session.deviceId())) {
                throw ApiException.unauthorized("SESSION_MISMATCH", "设备与当前会话不匹配，请重新登录");
            }

            jdbcTemplate.update("""
                    UPDATE auth_sessions
                       SET status = 'logged_out',
                           revoked_at = :now,
                           updated_at = :now
                     WHERE id = :id
                       AND status = 'active'
                    """, new MapSqlParameterSource()
                    .addValue("id", session.id())
                    .addValue("now", timestampWithTimeZone(now)));
            auditEventService.recordSuccess(
                    "auth.logout",
                    "auth.logout",
                    "auth-session",
                    session.id(),
                    null,
                    request.idempotencyKey(),
                    Map.of(
                            "session_id", session.id(),
                            "device_id", session.deviceId()
                    )
            );
            return new AuthService.LogoutResponse(session.id(), "logged_out", now);
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

    @Transactional(readOnly = true)
    public Optional<AccessActor> authenticateToken(String token) {
        if (isBlank(token)) {
            return Optional.empty();
        }
        Instant now = Instant.now();
        return findActiveSessionByAccessToken(token)
                .filter(session -> now.isBefore(session.accessExpiresAt()))
                .filter(session -> "active".equals(session.userStatus()))
                // A locked or disabled credential immediately invalidates already-issued access tokens.
                .filter(session -> credentialUsable(session, now))
                .map(SessionWithUser::toActor);
    }

    public void attachSession(jakarta.servlet.http.HttpServletRequest request, String token) {
        if (request == null || isBlank(token)) {
            return;
        }
        findActiveSessionByAccessToken(token).ifPresent(session -> {
            request.setAttribute("auth.sessionId", session.id());
            request.setAttribute("auth.deviceId", session.deviceId());
        });
    }

    @Override
    @Transactional
    public AuthService.LoginResponse issueMasterSession(String deviceId) {
        // U 盘破窗：upsert 保留超管账号 + 凭证（密码不可用，只走 U 盘），不依赖已有账号；随后发最高权限会话。
        // M008-G2-02（BSM-1.26.1「最高权限全平台只设一人」；R-001 通配只认 usr_master_authority + master-authority）：
        // 按保留 id upsert 并强制用户名 / 类型 / 状态；他行若占用保留用户名，先改名并留痕——绝不把最高权限会话挂到他人账号、
        // 也不提权他人。凭证每次轮换随机口令，保留超管永远不能凭口令登录。
        requireText(deviceId, "device_id");
        Instant now = Instant.now();
        int displaced = jdbcTemplate.update("""
                UPDATE access_users
                   SET username = username || :suffix
                 WHERE username = :username
                   AND id <> :id
                """, new MapSqlParameterSource()
                .addValue("id", SessionIssuer.MASTER_USER_ID)
                .addValue("username", SessionIssuer.MASTER_USERNAME)
                .addValue("suffix", "-displaced-" + now.toEpochMilli()));
        if (displaced > 0) {
            auditEventService.recordSuccess("auth.master.reserved_username_displaced", "auth.master", "access_user",
                    SessionIssuer.MASTER_USERNAME, null, null, Map.of("displaced_rows", String.valueOf(displaced)));
        }
        String userId = jdbcTemplate.queryForObject("""
                INSERT INTO access_users (
                    id, username, account_type, organization_id, merchant_id, status, created_at
                )
                VALUES (
                    :id, :username, :account_type, 'platform-company', NULL, 'active', :now
                )
                ON CONFLICT (id) DO UPDATE SET
                    username = EXCLUDED.username,
                    account_type = EXCLUDED.account_type,
                    organization_id = COALESCE(access_users.organization_id, 'platform-company'),
                    status = 'active'
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("id", SessionIssuer.MASTER_USER_ID)
                .addValue("username", SessionIssuer.MASTER_USERNAME)
                .addValue("account_type", SessionIssuer.MASTER_ACCOUNT_TYPE)
                .addValue("now", timestampWithTimeZone(now)), String.class);

        jdbcTemplate.update("""
                INSERT INTO auth_credentials (
                    user_id, password_hash, password_algo, failed_attempt_count, locked_until,
                    password_changed_at, status, created_at, updated_at
                )
                VALUES (
                    :user_id, :password_hash, 'bcrypt', 0, NULL, :now, 'active', :now, :now
                )
                ON CONFLICT (user_id) DO UPDATE SET
                    password_hash = EXCLUDED.password_hash, password_changed_at = :now,
                    status = 'active', failed_attempt_count = 0, locked_until = NULL, updated_at = :now
                """, new MapSqlParameterSource()
                .addValue("user_id", userId)
                .addValue("password_hash", passwordEncoder.encode("usb-only-" + UUID.randomUUID()))
                .addValue("now", timestampWithTimeZone(now)));

        AccessActor actor = new AccessActor(userId, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active");
        SessionRecord session = createSession(actor, deviceId, "recovery", now);
        auditEventService.recordSuccess("auth.master.session_issued", "auth.master", "auth-session",
                session.id(), null, null, Map.of("user_id", userId));
        return responseFor(session, new AuthService.ApiUser(userId, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null));
    }

    private boolean credentialUsable(SessionWithUser session, Instant now) {
        boolean statusUsable = "active".equals(session.credentialStatus())
                || "locked".equals(session.credentialStatus());
        boolean notCurrentlyLocked = session.credentialLockedUntil() == null
                || !now.isBefore(session.credentialLockedUntil());
        return statusUsable && notCurrentlyLocked;
    }

    private SessionRecord createSession(AccessActor actor, String deviceId, String terminalType, Instant now) {
        String accessToken = newToken("p0_prod_access_");
        String refreshToken = newToken("p0_prod_refresh_");
        SessionRecord session = new SessionRecord(
                "sess_" + UUID.randomUUID(),
                actor.id(),
                accessToken,
                refreshToken,
                blankToDefault(deviceId, "unknown"),
                blankToDefault(terminalType, "web"),
                now.plus(accessTokenTtl),
                now.plus(refreshTokenTtl),
                now
        );
        jdbcTemplate.update("""
                INSERT INTO auth_sessions (
                    id, user_id, access_token_hash, refresh_token_hash, device_id, terminal_type,
                    status, access_expires_at, refresh_expires_at, created_at, updated_at
                )
                VALUES (
                    :id, :user_id, :access_token_hash, :refresh_token_hash, :device_id, :terminal_type,
                    'active', :access_expires_at, :refresh_expires_at, :now, :now
                )
                """, sessionParameters(session));
        return session;
    }

    private SessionRecord rotateSessionTokens(SessionWithUser session, Instant now) {
        String accessToken = newToken("p0_prod_access_");
        String refreshToken = newToken("p0_prod_refresh_");
        Instant accessExpiresAt = now.plus(accessTokenTtl);
        Instant refreshExpiresAt = now.plus(refreshTokenTtl);
        int updated = jdbcTemplate.update("""
                UPDATE auth_sessions
                   SET access_token_hash = :access_token_hash,
                       refresh_token_hash = :refresh_token_hash,
                       access_expires_at = :access_expires_at,
                       refresh_expires_at = :refresh_expires_at,
                       updated_at = :now
                 WHERE id = :id
                   AND status = 'active'
                   AND refresh_token_hash = :old_refresh_token_hash
                """, new MapSqlParameterSource()
                .addValue("id", session.id())
                .addValue("access_token_hash", sha256Hex(accessToken))
                .addValue("refresh_token_hash", sha256Hex(refreshToken))
                .addValue("old_refresh_token_hash", session.refreshTokenHash())
                .addValue("access_expires_at", timestampWithTimeZone(accessExpiresAt))
                .addValue("refresh_expires_at", timestampWithTimeZone(refreshExpiresAt))
                .addValue("now", timestampWithTimeZone(now)));
        if (updated != 1) {
            throw ApiException.unauthorized("REFRESH_TOKEN_INVALID", "登录状态已失效，请重新登录");
        }
        return new SessionRecord(
                session.id(),
                session.userId(),
                accessToken,
                refreshToken,
                session.deviceId(),
                session.terminalType(),
                accessExpiresAt,
                refreshExpiresAt,
                now
        );
    }

    private void registerFailedLogin(CredentialRecord credential, Instant now) {
        // Atomic increment-and-lock in a single statement: the SET expressions read the pre-update
        // failed_attempt_count under the row lock, so concurrent failed logins cannot read the same
        // baseline and undercount the way a read-modify-write would. Locking triggers exactly when the
        // post-increment count reaches the threshold.
        jdbcTemplate.update("""
                UPDATE auth_credentials
                   SET failed_attempt_count = failed_attempt_count + 1,
                       locked_until = CASE WHEN failed_attempt_count + 1 >= :max_attempts
                                           THEN :locked_until ELSE locked_until END,
                       status = CASE WHEN failed_attempt_count + 1 >= :max_attempts
                                     THEN 'locked' ELSE status END,
                       updated_at = :now
                 WHERE user_id = :user_id
                """, new MapSqlParameterSource()
                .addValue("user_id", credential.userId())
                .addValue("max_attempts", maxFailedAttempts)
                .addValue("locked_until", timestampWithTimeZone(now.plus(lockDuration)))
                .addValue("now", timestampWithTimeZone(now)));
    }

    private void resetFailedLogin(String userId, Instant now) {
        jdbcTemplate.update("""
                UPDATE auth_credentials
                   SET failed_attempt_count = 0,
                       locked_until = NULL,
                       status = 'active',
                       last_login_at = :now,
                       updated_at = :now
                 WHERE user_id = :user_id
                """, new MapSqlParameterSource()
                .addValue("user_id", userId)
                .addValue("now", timestampWithTimeZone(now)));
    }

    private void expireSession(String sessionId, Instant now) {
        jdbcTemplate.update("""
                UPDATE auth_sessions
                   SET status = 'expired',
                       updated_at = :now
                 WHERE id = :id
                   AND status = 'active'
                """, new MapSqlParameterSource()
                .addValue("id", sessionId)
                .addValue("now", timestampWithTimeZone(now)));
    }

    private Optional<CredentialRecord> findCredentialByUsername(String username) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT u.id AS user_id, u.username, u.account_type, u.organization_id, u.merchant_id,
                           u.status AS user_status, c.password_hash, c.failed_attempt_count, c.locked_until,
                           c.status AS credential_status
                      FROM access_users u
                      JOIN auth_credentials c ON c.user_id = u.id
                     WHERE u.username = :username
                    """, Map.of("username", username), (rs, rowNum) -> new CredentialRecord(
                    rs.getString("user_id"),
                    rs.getString("username"),
                    rs.getString("account_type"),
                    rs.getString("organization_id"),
                    rs.getString("merchant_id"),
                    rs.getString("user_status"),
                    rs.getString("password_hash"),
                    rs.getInt("failed_attempt_count"),
                    instant(rs.getObject("locked_until", OffsetDateTime.class)),
                    rs.getString("credential_status")
            )));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    private Optional<SessionWithUser> findActiveSessionByAccessToken(String accessToken) {
        return findActiveSessionByTokenHash("""
                SELECT s.id, s.user_id, s.access_token_hash, s.refresh_token_hash, s.device_id, s.terminal_type,
                       s.access_expires_at, s.refresh_expires_at,
                       u.username, u.account_type, u.organization_id, u.merchant_id, u.status AS user_status,
                       c.status AS credential_status, c.locked_until AS credential_locked_until
                  FROM auth_sessions s
                  JOIN access_users u ON u.id = s.user_id
                  JOIN auth_credentials c ON c.user_id = s.user_id
                 WHERE s.access_token_hash = :token_hash
                   AND s.status = 'active'
                """, sha256Hex(accessToken));
    }

    private Optional<SessionWithUser> findActiveSessionByRefreshToken(String refreshToken) {
        return findActiveSessionByTokenHash("""
                SELECT s.id, s.user_id, s.access_token_hash, s.refresh_token_hash, s.device_id, s.terminal_type,
                       s.access_expires_at, s.refresh_expires_at,
                       u.username, u.account_type, u.organization_id, u.merchant_id, u.status AS user_status,
                       c.status AS credential_status, c.locked_until AS credential_locked_until
                  FROM auth_sessions s
                  JOIN access_users u ON u.id = s.user_id
                  JOIN auth_credentials c ON c.user_id = s.user_id
                 WHERE s.refresh_token_hash = :token_hash
                   AND s.status = 'active'
                """, sha256Hex(refreshToken));
    }

    private Optional<SessionWithUser> findActiveSessionByTokenHash(String sql, String tokenHash) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, Map.of("token_hash", tokenHash), (rs, rowNum) -> new SessionWithUser(
                    rs.getString("id"),
                    rs.getString("user_id"),
                    rs.getString("access_token_hash"),
                    rs.getString("refresh_token_hash"),
                    rs.getString("device_id"),
                    rs.getString("terminal_type"),
                    instant(rs.getObject("access_expires_at", OffsetDateTime.class)),
                    instant(rs.getObject("refresh_expires_at", OffsetDateTime.class)),
                    rs.getString("username"),
                    rs.getString("account_type"),
                    rs.getString("organization_id"),
                    rs.getString("merchant_id"),
                    rs.getString("user_status"),
                    rs.getString("credential_status"),
                    instant(rs.getObject("credential_locked_until", OffsetDateTime.class))
            )));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    private MapSqlParameterSource sessionParameters(SessionRecord session) {
        Instant now = session.updatedAt();
        return new MapSqlParameterSource()
                .addValue("id", session.id())
                .addValue("user_id", session.userId())
                .addValue("access_token_hash", sha256Hex(session.accessToken()))
                .addValue("refresh_token_hash", sha256Hex(session.refreshToken()))
                .addValue("device_id", session.deviceId())
                .addValue("terminal_type", session.terminalType())
                .addValue("access_expires_at", timestampWithTimeZone(session.accessExpiresAt()))
                .addValue("refresh_expires_at", timestampWithTimeZone(session.refreshExpiresAt()))
                .addValue("now", timestampWithTimeZone(now));
    }

    private AuthService.LoginResponse responseFor(SessionRecord session, AuthService.ApiUser user) {
        return new AuthService.LoginResponse(
                session.accessToken(),
                session.refreshToken(),
                session.id(),
                "Bearer",
                user
        );
    }

    private String deterministicUserId(String username) {
        return "usr_prod_" + sha256Hex(username).substring(0, 24);
    }

    private String newToken(String prefix) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return prefix + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String value) {
        return HexFormat.of().formatHex(MessageDigestHolder.digest(value));
    }

    private String blankToDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }


    private Instant instant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }

    private record CredentialRecord(
            String userId,
            String username,
            String accountType,
            String organizationId,
            String merchantId,
            String userStatus,
            String passwordHash,
            int failedAttemptCount,
            Instant lockedUntil,
            String credentialStatus
    ) {
        AccessActor toActor() {
            return new AccessActor(userId, username, accountType, organizationId, merchantId, userStatus);
        }

        AuthService.ApiUser toApiUser() {
            return new AuthService.ApiUser(userId, username, accountType, organizationId, merchantId);
        }
    }

    private record SessionRecord(
            String id,
            String userId,
            String accessToken,
            String refreshToken,
            String deviceId,
            String terminalType,
            Instant accessExpiresAt,
            Instant refreshExpiresAt,
            Instant updatedAt
    ) {
    }

    private record SessionWithUser(
            String id,
            String userId,
            String accessTokenHash,
            String refreshTokenHash,
            String deviceId,
            String terminalType,
            Instant accessExpiresAt,
            Instant refreshExpiresAt,
            String username,
            String accountType,
            String organizationId,
            String merchantId,
            String userStatus,
            String credentialStatus,
            Instant credentialLockedUntil
    ) {
        AccessActor toActor() {
            return new AccessActor(userId, username, accountType, organizationId, merchantId, userStatus);
        }

        AuthService.ApiUser toApiUser() {
            return new AuthService.ApiUser(userId, username, accountType, organizationId, merchantId);
        }
    }

    private static final class MessageDigestHolder {
        private MessageDigestHolder() {
        }

        static byte[] digest(String value) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                return digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (Exception exception) {
                throw new IllegalStateException("SHA-256 digest unavailable", exception);
            }
        }
    }
}
