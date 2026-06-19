package com.complex.platform.floorplan;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcFloorPlanRepository implements FloorPlanRepository {

    private static final TypeReference<List<FloorPlanPoint>> FLOOR_PLAN_POINTS_TYPE = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcFloorPlanRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public List<FloorPlanRecord> findAllFloorPlans() {
        return jdbcTemplate.query("""
                SELECT id, project_id, building_code, floor_no, plan_name, image_url, image_width,
                       image_height, version_no, status, created_at, updated_at, merchant_id
                  FROM mall_floor_plans
                 ORDER BY floor_no, version_no, id
                """, this::mapFloorPlan);
    }

    @Override
    public List<FloorPlanRecord> findAllFloorPlansByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, building_code, floor_no, plan_name, image_url, image_width,
                       image_height, version_no, status, created_at, updated_at, merchant_id
                  FROM mall_floor_plans
                 WHERE merchant_id = :merchant_id
                 ORDER BY floor_no, version_no, id
                """, Map.of("merchant_id", merchantId), this::mapFloorPlan);
    }

    @Override
    public List<FloorPlanRecord> findAllFloorPlansByProject(String projectId) {
        // 综合体面 scoped 读通道：按 mall_floor_plans.project_id = token scope 过滤（AC-ISO-1，§4）。
        return jdbcTemplate.query("""
                SELECT id, project_id, building_code, floor_no, plan_name, image_url, image_width,
                       image_height, version_no, status, created_at, updated_at, merchant_id
                  FROM mall_floor_plans
                 WHERE project_id = :project_id
                 ORDER BY floor_no, version_no, id
                """, Map.of("project_id", projectId), this::mapFloorPlan);
    }

    @Override
    public Optional<FloorPlanRecord> findFloorPlanById(String id) {
        return jdbcTemplate.query("""
                SELECT id, project_id, building_code, floor_no, plan_name, image_url, image_width,
                       image_height, version_no, status, created_at, updated_at, merchant_id
                  FROM mall_floor_plans
                 WHERE id = :id
                """, Map.of("id", id), this::mapFloorPlan).stream().findFirst();
    }

    @Override
    public FloorPlanRecord saveFloorPlan(FloorPlanRecord record) {
        jdbcTemplate.update("""
                INSERT INTO mall_floor_plans (
                    id, project_id, building_code, floor_no, plan_name, image_url, image_width,
                    image_height, version_no, status, merchant_id, created_at, updated_at
                )
                VALUES (
                    :id, :project_id, :building_code, :floor_no, :plan_name, :image_url, :image_width,
                    :image_height, :version_no, :status, :merchant_id, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    project_id = EXCLUDED.project_id,
                    building_code = EXCLUDED.building_code,
                    floor_no = EXCLUDED.floor_no,
                    plan_name = EXCLUDED.plan_name,
                    image_url = EXCLUDED.image_url,
                    image_width = EXCLUDED.image_width,
                    image_height = EXCLUDED.image_height,
                    version_no = EXCLUDED.version_no,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, floorPlanParameters(record));
        return record;
    }

    @Override
    public List<FloorPlanShapeRecord> findShapesByFloorPlanId(String floorPlanId) {
        return jdbcTemplate.query("""
                SELECT id, floor_plan_id, shape_type, ref_type, ref_id, label, polygon_points,
                       z_index, status, created_at, updated_at
                  FROM floor_plan_shapes
                 WHERE floor_plan_id = :floor_plan_id
                 ORDER BY z_index ASC, id
                """, Map.of("floor_plan_id", floorPlanId), this::mapShape);
    }

    @Override
    public Optional<FloorPlanShapeRecord> findShapeById(String id) {
        return jdbcTemplate.query("""
                SELECT id, floor_plan_id, shape_type, ref_type, ref_id, label, polygon_points,
                       z_index, status, created_at, updated_at
                  FROM floor_plan_shapes
                 WHERE id = :id
                """, Map.of("id", id), this::mapShape).stream().findFirst();
    }

    @Override
    public FloorPlanShapeRecord saveShape(FloorPlanShapeRecord record) {
        jdbcTemplate.update("""
                INSERT INTO floor_plan_shapes (
                    id, floor_plan_id, shape_type, ref_type, ref_id, label, polygon_points,
                    z_index, status, created_at, updated_at
                )
                VALUES (
                    :id, :floor_plan_id, :shape_type, :ref_type, :ref_id, :label, CAST(:polygon_points AS jsonb),
                    :z_index, :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    floor_plan_id = EXCLUDED.floor_plan_id,
                    shape_type = EXCLUDED.shape_type,
                    ref_type = EXCLUDED.ref_type,
                    ref_id = EXCLUDED.ref_id,
                    label = EXCLUDED.label,
                    polygon_points = EXCLUDED.polygon_points,
                    z_index = EXCLUDED.z_index,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, shapeParameters(record));
        return record;
    }

    @Override
    public FloorPlanImportTaskRecord saveImportTask(FloorPlanImportTaskRecord record) {
        jdbcTemplate.update("""
                INSERT INTO floor_plan_import_tasks (
                    id, floor_plan_id, source_format, source_name, source_hash, source_payload,
                    status, candidate_count, error_count, operator_note, created_at, updated_at
                )
                VALUES (
                    :id, :floor_plan_id, :source_format, :source_name, :source_hash, CAST(:source_payload AS jsonb),
                    :status, :candidate_count, :error_count, :operator_note, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    source_format = EXCLUDED.source_format,
                    source_name = EXCLUDED.source_name,
                    source_hash = EXCLUDED.source_hash,
                    source_payload = EXCLUDED.source_payload,
                    status = EXCLUDED.status,
                    candidate_count = EXCLUDED.candidate_count,
                    error_count = EXCLUDED.error_count,
                    operator_note = EXCLUDED.operator_note,
                    updated_at = EXCLUDED.updated_at
                """, importTaskParameters(record));
        return record;
    }

    @Override
    public Optional<FloorPlanImportTaskRecord> findImportTaskById(String id) {
        return jdbcTemplate.query("""
                SELECT id, floor_plan_id, source_format, source_name, source_hash, source_payload,
                       status, candidate_count, error_count, operator_note, created_at, updated_at
                  FROM floor_plan_import_tasks
                 WHERE id = :id
                """, Map.of("id", id), this::mapImportTask).stream().findFirst();
    }

    @Override
    public List<FloorPlanImportTaskRecord> findImportTasksByFloorPlanId(String floorPlanId) {
        return jdbcTemplate.query("""
                SELECT id, floor_plan_id, source_format, source_name, source_hash, source_payload,
                       status, candidate_count, error_count, operator_note, created_at, updated_at
                  FROM floor_plan_import_tasks
                 WHERE floor_plan_id = :floor_plan_id
                 ORDER BY created_at DESC, id
                """, Map.of("floor_plan_id", floorPlanId), this::mapImportTask);
    }

    @Override
    public FloorPlanImportCandidateRecord saveImportCandidate(FloorPlanImportCandidateRecord record) {
        jdbcTemplate.update("""
                INSERT INTO floor_plan_import_candidates (
                    id, import_task_id, candidate_no, shape_type, ref_type, ref_id, label,
                    polygon_points, z_index, status, created_at, updated_at
                )
                VALUES (
                    :id, :import_task_id, :candidate_no, :shape_type, :ref_type, :ref_id, :label,
                    CAST(:polygon_points AS jsonb), :z_index, :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    candidate_no = EXCLUDED.candidate_no,
                    shape_type = EXCLUDED.shape_type,
                    ref_type = EXCLUDED.ref_type,
                    ref_id = EXCLUDED.ref_id,
                    label = EXCLUDED.label,
                    polygon_points = EXCLUDED.polygon_points,
                    z_index = EXCLUDED.z_index,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, importCandidateParameters(record));
        return record;
    }

    @Override
    public Optional<FloorPlanImportCandidateRecord> findImportCandidateById(String id) {
        return jdbcTemplate.query("""
                SELECT id, import_task_id, candidate_no, shape_type, ref_type, ref_id, label,
                       polygon_points, z_index, status, created_at, updated_at
                  FROM floor_plan_import_candidates
                 WHERE id = :id
                """, Map.of("id", id), this::mapImportCandidate).stream().findFirst();
    }

    @Override
    public List<FloorPlanImportCandidateRecord> findImportCandidatesByTaskId(String importTaskId) {
        return jdbcTemplate.query("""
                SELECT id, import_task_id, candidate_no, shape_type, ref_type, ref_id, label,
                       polygon_points, z_index, status, created_at, updated_at
                  FROM floor_plan_import_candidates
                 WHERE import_task_id = :import_task_id
                 ORDER BY candidate_no, id
                """, Map.of("import_task_id", importTaskId), this::mapImportCandidate);
    }

    @Override
    public FloorPlanImportErrorRecord saveImportError(FloorPlanImportErrorRecord record) {
        jdbcTemplate.update("""
                INSERT INTO floor_plan_import_errors (
                    id, import_task_id, line_no, field_name, error_code, error_message, created_at
                )
                VALUES (
                    :id, :import_task_id, :line_no, :field_name, :error_code, :error_message, :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    line_no = EXCLUDED.line_no,
                    field_name = EXCLUDED.field_name,
                    error_code = EXCLUDED.error_code,
                    error_message = EXCLUDED.error_message,
                    created_at = EXCLUDED.created_at
                """, importErrorParameters(record));
        return record;
    }

    @Override
    public List<FloorPlanImportErrorRecord> findImportErrorsByTaskId(String importTaskId) {
        return jdbcTemplate.query("""
                SELECT id, import_task_id, line_no, field_name, error_code, error_message, created_at
                  FROM floor_plan_import_errors
                 WHERE import_task_id = :import_task_id
                 ORDER BY id
                """, Map.of("import_task_id", importTaskId), this::mapImportError);
    }

    private MapSqlParameterSource floorPlanParameters(FloorPlanRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("project_id", record.projectId())
                .addValue("building_code", record.buildingCode())
                .addValue("floor_no", record.floorNo())
                .addValue("plan_name", record.planName())
                .addValue("image_url", record.imageUrl())
                .addValue("image_width", record.imageWidth())
                .addValue("image_height", record.imageHeight())
                .addValue("version_no", record.versionNo())
                .addValue("status", record.status())
                .addValue("merchant_id", record.merchantId())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt()));
    }

    private MapSqlParameterSource shapeParameters(FloorPlanShapeRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("floor_plan_id", record.floorPlanId())
                .addValue("shape_type", record.shapeType())
                .addValue("ref_type", record.refType())
                .addValue("ref_id", record.refId())
                .addValue("label", record.label())
                .addValue("polygon_points", writeJson(record.polygonPoints()))
                .addValue("z_index", record.zIndex())
                .addValue("status", record.status())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt()));
    }

    private MapSqlParameterSource importTaskParameters(FloorPlanImportTaskRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("floor_plan_id", record.floorPlanId())
                .addValue("source_format", record.sourceFormat())
                .addValue("source_name", record.sourceName())
                .addValue("source_hash", record.sourceHash())
                .addValue("source_payload", writeJson(record.sourcePayload()))
                .addValue("status", record.status())
                .addValue("candidate_count", record.candidateCount())
                .addValue("error_count", record.errorCount())
                .addValue("operator_note", record.operatorNote())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt()));
    }

    private MapSqlParameterSource importCandidateParameters(FloorPlanImportCandidateRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("import_task_id", record.importTaskId())
                .addValue("candidate_no", record.candidateNo())
                .addValue("shape_type", record.shapeType())
                .addValue("ref_type", record.refType())
                .addValue("ref_id", record.refId())
                .addValue("label", record.label())
                .addValue("polygon_points", writeJson(record.polygonPoints()))
                .addValue("z_index", record.zIndex())
                .addValue("status", record.status())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt()));
    }

    private MapSqlParameterSource importErrorParameters(FloorPlanImportErrorRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("import_task_id", record.importTaskId())
                .addValue("line_no", record.lineNo())
                .addValue("field_name", record.fieldName())
                .addValue("error_code", record.errorCode())
                .addValue("error_message", record.errorMessage())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()));
    }

    private FloorPlanRecord mapFloorPlan(ResultSet resultSet, int rowNumber) throws SQLException {
        return new FloorPlanRecord(
                resultSet.getString("id"),
                resultSet.getString("project_id"),
                resultSet.getString("building_code"),
                resultSet.getString("floor_no"),
                resultSet.getString("plan_name"),
                resultSet.getString("image_url"),
                resultSet.getInt("image_width"),
                resultSet.getInt("image_height"),
                resultSet.getString("version_no"),
                resultSet.getString("status"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant(),
                resultSet.getString("merchant_id")
        );
    }

    private FloorPlanShapeRecord mapShape(ResultSet resultSet, int rowNumber) throws SQLException {
        return new FloorPlanShapeRecord(
                resultSet.getString("id"),
                resultSet.getString("floor_plan_id"),
                resultSet.getString("shape_type"),
                resultSet.getString("ref_type"),
                resultSet.getString("ref_id"),
                resultSet.getString("label"),
                readPoints(resultSet.getString("polygon_points")),
                resultSet.getInt("z_index"),
                resultSet.getString("status"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant()
        );
    }

    private FloorPlanImportTaskRecord mapImportTask(ResultSet resultSet, int rowNumber) throws SQLException {
        return new FloorPlanImportTaskRecord(
                resultSet.getString("id"),
                resultSet.getString("floor_plan_id"),
                resultSet.getString("source_format"),
                resultSet.getString("source_name"),
                resultSet.getString("source_hash"),
                readString(resultSet.getString("source_payload")),
                resultSet.getString("status"),
                resultSet.getInt("candidate_count"),
                resultSet.getInt("error_count"),
                resultSet.getString("operator_note"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant()
        );
    }

    private FloorPlanImportCandidateRecord mapImportCandidate(ResultSet resultSet, int rowNumber) throws SQLException {
        return new FloorPlanImportCandidateRecord(
                resultSet.getString("id"),
                resultSet.getString("import_task_id"),
                resultSet.getInt("candidate_no"),
                resultSet.getString("shape_type"),
                resultSet.getString("ref_type"),
                resultSet.getString("ref_id"),
                resultSet.getString("label"),
                readPoints(resultSet.getString("polygon_points")),
                resultSet.getInt("z_index"),
                resultSet.getString("status"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant()
        );
    }

    private FloorPlanImportErrorRecord mapImportError(ResultSet resultSet, int rowNumber) throws SQLException {
        return new FloorPlanImportErrorRecord(
                resultSet.getString("id"),
                resultSet.getString("import_task_id"),
                resultSet.getObject("line_no", Integer.class),
                resultSet.getString("field_name"),
                resultSet.getString("error_code"),
                resultSet.getString("error_message"),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }


    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("json field is not serializable", exception);
        }
    }

    private List<FloorPlanPoint> readPoints(String value) {
        try {
            return value == null ? List.of() : objectMapper.readValue(value, FLOOR_PLAN_POINTS_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("json field is not readable", exception);
        }
    }

    private String readString(String value) {
        try {
            return value == null ? null : objectMapper.readValue(value, String.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("json string field is not readable", exception);
        }
    }
}
