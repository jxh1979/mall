package com.complex.platform.gate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

// ponytail: 仓储直用实现类,无接口(one impl)。
@Repository
class JdbcAdmissionPolicyRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcAdmissionPolicyRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<AdmissionPolicyRecord> findEnabledPolicy(String actionType, String objectType) {
        return jdbcTemplate.query("""
                SELECT policy_code, action_type, object_type, trigger_mode, outcome,
                       approval_level, required_level, enabled
                  FROM supervisor_review_policies
                 WHERE enabled = true
                   AND action_type = :action_type
                   AND (:object_type = '' OR object_type = :object_type)
                 ORDER BY policy_code
                 LIMIT 1
                """, Map.of(
                "action_type", actionType,
                "object_type", objectType == null ? "" : objectType
        ), this::map).stream().findFirst();
    }

    public List<AdmissionPolicyRecord> listPolicies() {
        return jdbcTemplate.query("""
                SELECT policy_code, action_type, object_type, trigger_mode, outcome,
                       approval_level, required_level, enabled
                  FROM supervisor_review_policies
                 ORDER BY policy_code
                 LIMIT 1000
                """, this::map);
    }

    private AdmissionPolicyRecord map(ResultSet rs, int rowNum) throws SQLException {
        return new AdmissionPolicyRecord(
                rs.getString("policy_code"),
                rs.getString("action_type"),
                rs.getString("object_type"),
                rs.getString("trigger_mode"),
                rs.getString("outcome"),
                rs.getString("approval_level"),
                rs.getInt("required_level"),
                rs.getBoolean("enabled")
        );
    }
}
