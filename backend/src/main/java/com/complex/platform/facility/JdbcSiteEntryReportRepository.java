package com.complex.platform.facility;

import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcSiteEntryReportRepository implements SiteEntryReportRepository {

    private static final TypeReference<List<String>> LIST_STRING_TYPE = new TypeReference<>() {
    };
    private static final String COLS = """
            id, project_id, space_id, work_scope, planned_start_date, planned_end_date, crew_count, contact_name,
            contact_phone, vendor_user_id, status, review_comment, acceptance_comment, created_by, created_at,
            updated_at, completion_attachment_ids::text AS completion_attachment_ids, completion_note,
            completion_uploaded_at, deleted_at
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcSiteEntryReportRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public void insert(SiteEntryReportRecord r) {
        jdbcTemplate.update("""
                INSERT INTO site_entry_reports (id, project_id, space_id, work_scope, planned_start_date, planned_end_date,
                    crew_count, contact_name, contact_phone, vendor_user_id, status, review_comment, acceptance_comment,
                    created_by, created_at, updated_at, completion_attachment_ids, completion_note,
                    completion_uploaded_at, deleted_at)
                VALUES (:id, :project_id, :space_id, :work_scope, :planned_start_date, :planned_end_date,
                    :crew_count, :contact_name, :contact_phone, :vendor_user_id, :status, :review_comment,
                    :acceptance_comment, :created_by, :created_at, :updated_at,
                    CAST(:completion_attachment_ids AS jsonb), :completion_note, :completion_uploaded_at, :deleted_at)
                """, params(r));
    }

    @Override
    public void update(SiteEntryReportRecord r) {
        jdbcTemplate.update("""
                UPDATE site_entry_reports SET project_id=:project_id, space_id=:space_id, work_scope=:work_scope,
                    planned_start_date=:planned_start_date, planned_end_date=:planned_end_date, crew_count=:crew_count,
                    contact_name=:contact_name, contact_phone=:contact_phone, vendor_user_id=:vendor_user_id,
                    status=:status, review_comment=:review_comment, acceptance_comment=:acceptance_comment,
                    completion_attachment_ids=CAST(:completion_attachment_ids AS jsonb),
                    completion_note=:completion_note, completion_uploaded_at=:completion_uploaded_at,
                    updated_at=:updated_at, deleted_at=:deleted_at
                 WHERE id=:id
                """, params(r));
    }

    @Override
    public Optional<SiteEntryReportRecord> findById(String id) {
        return jdbcTemplate.query("SELECT " + COLS + """
                 FROM site_entry_reports
                 WHERE id = :id AND deleted_at IS NULL
                """, new MapSqlParameterSource("id", id), this::map).stream().findFirst();
    }

    @Override
    public List<SiteEntryReportRecord> list() {
        return jdbcTemplate.query("SELECT " + COLS + """
                 FROM site_entry_reports
                 WHERE deleted_at IS NULL
                 ORDER BY created_at DESC, id
                """, this::map);
    }

    @Override
    public List<SiteEntryReportRecord> listByProject(String projectId) {
        return jdbcTemplate.query("SELECT " + COLS + """
                 FROM site_entry_reports
                 WHERE project_id = :project_id AND deleted_at IS NULL
                 ORDER BY created_at DESC, id
                """, new MapSqlParameterSource("project_id", projectId), this::map);
    }

    @Override
    public List<SiteEntryReportRecord> listAssignedTo(String assigneeId, String projectId) {
        return jdbcTemplate.query("SELECT " + COLS + """
                 FROM site_entry_reports
                 WHERE vendor_user_id = :vendor_user_id AND project_id = :project_id AND deleted_at IS NULL
                 ORDER BY created_at DESC, id
                """, new MapSqlParameterSource("vendor_user_id", assigneeId).addValue("project_id", projectId),
                this::map);
    }

    private MapSqlParameterSource params(SiteEntryReportRecord r) {
        return new MapSqlParameterSource()
                .addValue("id", r.id()).addValue("project_id", r.projectId()).addValue("space_id", r.spaceId())
                .addValue("work_scope", r.workScope()).addValue("planned_start_date", r.plannedStartDate())
                .addValue("planned_end_date", r.plannedEndDate()).addValue("crew_count", r.crewCount())
                .addValue("contact_name", r.contactName()).addValue("contact_phone", r.contactPhone())
                .addValue("vendor_user_id", r.vendorUserId()).addValue("status", r.status())
                .addValue("review_comment", r.reviewComment())
                .addValue("acceptance_comment", r.acceptanceComment()).addValue("created_by", r.createdBy())
                .addValue("created_at", timestampWithTimeZone(r.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(r.updatedAt()))
                .addValue("completion_attachment_ids", writeJson(r.completionAttachmentIds()))
                .addValue("completion_note", r.completionNote())
                .addValue("completion_uploaded_at", timestampWithTimeZone(r.completionUploadedAt()))
                .addValue("deleted_at", timestampWithTimeZone(r.deletedAt()));
    }

    private SiteEntryReportRecord map(ResultSet rs, int n) throws SQLException {
        return new SiteEntryReportRecord(
                rs.getString("id"), rs.getString("project_id"), rs.getString("space_id"),
                rs.getString("work_scope"), localDate(rs, "planned_start_date"), localDate(rs, "planned_end_date"),
                rs.getInt("crew_count"), rs.getString("contact_name"), rs.getString("contact_phone"),
                rs.getString("vendor_user_id"), rs.getString("status"), rs.getString("review_comment"),
                rs.getString("acceptance_comment"), rs.getString("created_by"), nullableInstant(rs, "created_at"),
                nullableInstant(rs, "updated_at"), readListString(rs.getString("completion_attachment_ids")),
                rs.getString("completion_note"), nullableInstant(rs, "completion_uploaded_at"),
                nullableInstant(rs, "deleted_at"));
    }

    private LocalDate localDate(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDate.class);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize json", exception);
        }
    }

    private List<String> readListString(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, LIST_STRING_TYPE);
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }
}
