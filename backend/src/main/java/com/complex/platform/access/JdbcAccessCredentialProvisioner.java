package com.complex.platform.access;

import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import java.security.SecureRandom;
import java.time.Instant;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("prod")
class JdbcAccessCredentialProvisioner implements AccessCredentialProvisioner {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    JdbcAccessCredentialProvisioner(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = new BCryptPasswordEncoder(10, new SecureRandom());
    }

    @Override
    @Transactional
    public void provisionCredential(String userId, String password, String status, Instant now) {
        if (isBlank(userId) || isBlank(password)) {
            return;
        }
        String credentialStatus = "disabled".equals(status) ? "disabled" : "active";
        jdbcTemplate.update("""
                INSERT INTO auth_credentials (
                    user_id, password_hash, password_algo, failed_attempt_count, locked_until,
                    password_changed_at, status, created_at, updated_at
                )
                VALUES (
                    :user_id, :password_hash, 'bcrypt', 0, NULL, :now, :status, :now, :now
                )
                ON CONFLICT (user_id) DO UPDATE SET
                    password_hash = EXCLUDED.password_hash,
                    password_algo = 'bcrypt',
                    failed_attempt_count = 0,
                    locked_until = NULL,
                    password_changed_at = EXCLUDED.password_changed_at,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("user_id", userId)
                .addValue("password_hash", passwordEncoder.encode(password))
                .addValue("status", credentialStatus)
                .addValue("now", timestampWithTimeZone(now)));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
