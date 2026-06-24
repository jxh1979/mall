package com.complex.platform.finance;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcRefundAuthorizationRepository implements RefundAuthorizationRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcRefundAuthorizationRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<RefundAuthorityLimitRecord> findActiveLimit(String subjectType, String subjectId, String refundDomain) {
        return jdbcTemplate.query("""
                SELECT id, subject_type, subject_id, refund_domain, limit_amount_cent,
                       project_id, status, created_at, updated_at
                  FROM refund_authority_limits
                 WHERE subject_type = :st AND subject_id = :sid AND refund_domain = :dom AND status = 'active'
                """, new MapSqlParameterSource()
                .addValue("st", subjectType).addValue("sid", subjectId).addValue("dom", refundDomain),
                this::mapLimit).stream().findFirst();
    }

    @Override
    public boolean isAutoRuleEnabled(String refundDomain, String ruleType) {
        return jdbcTemplate.query("""
                SELECT enabled FROM refund_auto_rules
                 WHERE refund_domain = :dom AND rule_type = :rt AND status = 'active'
                """, new MapSqlParameterSource().addValue("dom", refundDomain).addValue("rt", ruleType),
                (rs, n) -> rs.getBoolean("enabled")).stream().findFirst().orElse(false);
    }

    @Override
    public RefundAuthorityLimitRecord saveLimit(RefundAuthorityLimitRecord limit) {
        jdbcTemplate.update("""
                INSERT INTO refund_authority_limits (
                    id, subject_type, subject_id, refund_domain, limit_amount_cent, project_id, status, created_at, updated_at
                )
                VALUES (:id, :st, :sid, :dom, :lim, :pid, :status, now(), now())
                ON CONFLICT (subject_type, subject_id, refund_domain) DO UPDATE SET
                    limit_amount_cent = EXCLUDED.limit_amount_cent,
                    project_id = EXCLUDED.project_id,
                    status = EXCLUDED.status,
                    updated_at = now()
                """, new MapSqlParameterSource()
                .addValue("id", limit.id()).addValue("st", limit.subjectType()).addValue("sid", limit.subjectId())
                .addValue("dom", limit.refundDomain()).addValue("lim", limit.limitAmountCent())
                .addValue("pid", limit.projectId()).addValue("status", limit.status()));
        return findActiveLimit(limit.subjectType(), limit.subjectId(), limit.refundDomain()).orElse(limit);
    }

    @Override
    public List<RefundAuthorityLimitRecord> listLimits() {
        return jdbcTemplate.query("""
                SELECT id, subject_type, subject_id, refund_domain, limit_amount_cent,
                       project_id, status, created_at, updated_at
                  FROM refund_authority_limits
                 ORDER BY subject_type, subject_id, refund_domain
                """, this::mapLimit);
    }

    @Override
    public RefundAutoRuleRecord saveRule(RefundAutoRuleRecord rule) {
        jdbcTemplate.update("""
                INSERT INTO refund_auto_rules (
                    id, refund_domain, rule_type, enabled, project_id, status, created_at, updated_at
                )
                VALUES (:id, :dom, :rt, :enabled, :pid, :status, now(), now())
                ON CONFLICT (refund_domain, rule_type) DO UPDATE SET
                    enabled = EXCLUDED.enabled,
                    project_id = EXCLUDED.project_id,
                    status = EXCLUDED.status,
                    updated_at = now()
                """, new MapSqlParameterSource()
                .addValue("id", rule.id()).addValue("dom", rule.refundDomain()).addValue("rt", rule.ruleType())
                .addValue("enabled", rule.enabled()).addValue("pid", rule.projectId()).addValue("status", rule.status()));
        return listRules().stream()
                .filter(r -> r.refundDomain().equals(rule.refundDomain()) && r.ruleType().equals(rule.ruleType()))
                .findFirst().orElse(rule);
    }

    @Override
    public List<RefundAutoRuleRecord> listRules() {
        return jdbcTemplate.query("""
                SELECT id, refund_domain, rule_type, enabled, project_id, status, created_at, updated_at
                  FROM refund_auto_rules
                 ORDER BY refund_domain, rule_type
                """, this::mapRule);
    }

    private RefundAuthorityLimitRecord mapLimit(ResultSet rs, int rowNum) throws SQLException {
        return new RefundAuthorityLimitRecord(
                rs.getString("id"), rs.getString("subject_type"), rs.getString("subject_id"),
                rs.getString("refund_domain"), rs.getLong("limit_amount_cent"), rs.getString("project_id"),
                rs.getString("status"), rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private RefundAutoRuleRecord mapRule(ResultSet rs, int rowNum) throws SQLException {
        return new RefundAutoRuleRecord(
                rs.getString("id"), rs.getString("refund_domain"), rs.getString("rule_type"),
                rs.getBoolean("enabled"), rs.getString("project_id"), rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }
}
