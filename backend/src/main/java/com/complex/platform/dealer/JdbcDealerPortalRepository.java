package com.complex.platform.dealer;

// 文件版本：V0.02

import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** CG-11d 经销商门户账号归属仓储（V454 dealer_portal_accounts）。 */
@Repository
public class JdbcDealerPortalRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcDealerPortalRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void saveAccount(DealerPortalAccountRecord account) {
        jdbc.update("""
                INSERT INTO dealer_portal_accounts (user_id, dealer_id, status, created_by, created_at, revoked_at)
                VALUES (:user, :dealer, :status, :by, :at, :revoked)
                ON CONFLICT (user_id) DO UPDATE SET status = EXCLUDED.status, revoked_at = EXCLUDED.revoked_at
                """, new MapSqlParameterSource().addValue("user", account.userId()).addValue("dealer", account.dealerId()).addValue("status", account.status())
                .addValue("by", account.createdBy()).addValue("at", Timestamp.from(account.createdAt()))
                .addValue("revoked", account.revokedAt() == null ? null : Timestamp.from(account.revokedAt())));
    }

    public Optional<DealerPortalAccountRecord> findByUser(String userId) {
        return jdbc.query(SELECT + " WHERE a.user_id = :u", Map.of("u", userId), (rs, i) -> map(rs)).stream().findFirst();
    }

    public List<DealerPortalAccountRecord> listByDealer(String dealerId) {
        return jdbc.query(SELECT + " WHERE a.dealer_id = :d ORDER BY a.created_at", Map.of("d", dealerId), (rs, i) -> map(rs));
    }

    public boolean usernameExists(String username) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM access_users WHERE username = :u", Map.of("u", username), Integer.class);
        return n != null && n > 0;
    }

    public void activateByDealer(String dealerId) {
        Map<String, Object> params = Map.of("d", dealerId);
        jdbc.update("UPDATE access_users SET status = 'active' WHERE id IN (SELECT user_id FROM dealer_portal_accounts WHERE dealer_id = :d AND status = 'disabled')", params);
        jdbc.update("UPDATE dealer_portal_accounts SET status = 'active' WHERE dealer_id = :d AND status = 'disabled'", params);
    }

    /** 停用撤权：门户账号置 revoked、access_users 置 disabled（会话由 SessionIssuer 作废）。 */
    public int revokeByDealer(String dealerId, Instant now) {
        jdbc.update("UPDATE access_users SET status = 'disabled' WHERE id IN (SELECT user_id FROM dealer_portal_accounts WHERE dealer_id = :d)", Map.of("d", dealerId));
        return jdbc.update("UPDATE dealer_portal_accounts SET status = 'revoked', revoked_at = :now WHERE dealer_id = :d AND status IN ('active', 'disabled')",
                new MapSqlParameterSource().addValue("d", dealerId).addValue("now", Timestamp.from(now)));
    }

    private static final String SELECT = "SELECT a.user_id, a.dealer_id, u.username, a.status, a.created_by, a.created_at, a.revoked_at FROM dealer_portal_accounts a JOIN access_users u ON u.id = a.user_id";

    private static DealerPortalAccountRecord map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new DealerPortalAccountRecord(rs.getString("user_id"), rs.getString("dealer_id"), rs.getString("username"), rs.getString("status"),
                rs.getString("created_by"), nullableInstant(rs, "created_at"), nullableInstant(rs, "revoked_at"));
    }
}
