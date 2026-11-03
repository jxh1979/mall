package com.complex.platform.governance;

// 文件版本：V0.01

import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** 第 123 条 123-R1：项目敏感操作授权范围（V591 project_sensitive_operation_ranges，每项目一行）。 */
@Repository
public class JdbcSensitiveOperationRangeRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcSensitiveOperationRangeRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 该项目的授权范围；未配置返回空。 */
    public Optional<SensitiveOperationRangeRecord> find(String projectId) {
        return jdbcTemplate.query("""
                SELECT project_id, center_latitude, center_longitude, radius_meters, min_radius_meters, max_radius_meters
                  FROM project_sensitive_operation_ranges
                 WHERE project_id = :project_id
                """, new MapSqlParameterSource("project_id", projectId), (rs, n) -> new SensitiveOperationRangeRecord(
                rs.getString("project_id"), rs.getBigDecimal("center_latitude"), rs.getBigDecimal("center_longitude"),
                rs.getInt("radius_meters"), rs.getInt("min_radius_meters"), rs.getInt("max_radius_meters"))).stream().findFirst();
    }

    void upsert(SensitiveOperationRangeRecord range, String actorId) {
        jdbcTemplate.update("""
                INSERT INTO project_sensitive_operation_ranges (project_id, center_latitude, center_longitude, radius_meters,
                    min_radius_meters, max_radius_meters, updated_by, updated_at)
                VALUES (:project_id, :lat, :lng, :radius, :min, :max, :actor, now())
                ON CONFLICT (project_id) DO UPDATE SET center_latitude = EXCLUDED.center_latitude,
                    center_longitude = EXCLUDED.center_longitude, radius_meters = EXCLUDED.radius_meters,
                    min_radius_meters = EXCLUDED.min_radius_meters, max_radius_meters = EXCLUDED.max_radius_meters,
                    updated_by = EXCLUDED.updated_by, updated_at = now()
                """, new MapSqlParameterSource()
                .addValue("project_id", range.projectId())
                .addValue("lat", range.centerLatitude())
                .addValue("lng", range.centerLongitude())
                .addValue("radius", range.radiusMeters())
                .addValue("min", range.minRadiusMeters())
                .addValue("max", range.maxRadiusMeters())
                .addValue("actor", actorId));
    }
}
