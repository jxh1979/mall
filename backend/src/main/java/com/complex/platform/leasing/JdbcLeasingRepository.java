package com.complex.platform.leasing;

import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

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

/** 招商仓储 JDBC 实现（V161 重建）。 */
@Repository
class JdbcLeasingRepository implements LeasingRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcLeasingRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    // ---------- leads ----------
    @Override
    public LeasingLeadRecord saveLead(LeasingLeadRecord lead) {
        jdbcTemplate.update("""
                INSERT INTO leasing_leads (id, contact_name, contact_phone, source, intended_business_type,
                        intended_space_id, intended_area_sqm, brand_id, owner_user_id, status, lost_or_hold_reason,
                        last_follow_up_at, project_id, created_at, updated_at)
                VALUES (:id, :contact_name, :contact_phone, :source, :intended_business_type,
                        :intended_space_id, :intended_area_sqm, :brand_id, :owner_user_id, :status, :lost_or_hold_reason,
                        :last_follow_up_at, :project_id, :created_at, :updated_at)
                ON CONFLICT (id) DO UPDATE SET
                    contact_name = EXCLUDED.contact_name, contact_phone = EXCLUDED.contact_phone,
                    source = EXCLUDED.source, intended_business_type = EXCLUDED.intended_business_type,
                    intended_space_id = EXCLUDED.intended_space_id, intended_area_sqm = EXCLUDED.intended_area_sqm,
                    brand_id = EXCLUDED.brand_id, owner_user_id = EXCLUDED.owner_user_id, status = EXCLUDED.status,
                    lost_or_hold_reason = EXCLUDED.lost_or_hold_reason, last_follow_up_at = EXCLUDED.last_follow_up_at,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", lead.id())
                .addValue("contact_name", lead.contactName())
                .addValue("contact_phone", lead.contactPhone())
                .addValue("source", lead.source())
                .addValue("intended_business_type", lead.intendedBusinessType())
                .addValue("intended_space_id", lead.intendedSpaceId())
                .addValue("intended_area_sqm", lead.intendedAreaSqm())
                .addValue("brand_id", lead.brandId())
                .addValue("owner_user_id", lead.ownerUserId())
                .addValue("status", lead.status())
                .addValue("lost_or_hold_reason", lead.lostOrHoldReason())
                .addValue("last_follow_up_at", timestampWithTimeZone(lead.lastFollowUpAt()))
                .addValue("project_id", lead.projectId())
                .addValue("created_at", timestampWithTimeZone(lead.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(lead.updatedAt())));
        return lead;
    }

    @Override
    public List<LeasingLeadRecord> listLeads() {
        return jdbcTemplate.query("SELECT * FROM leasing_leads ORDER BY created_at DESC, id LIMIT 500", this::mapLead);
    }

    @Override
    public Optional<LeasingLeadRecord> findLeadById(String id) {
        return jdbcTemplate.query("SELECT * FROM leasing_leads WHERE id = :id", Map.of("id", id), this::mapLead)
                .stream().findFirst();
    }

    // ---------- intents ----------
    @Override
    public LeasingIntentRecord saveIntent(LeasingIntentRecord intent) {
        jdbcTemplate.update("""
                INSERT INTO leasing_intents (id, lead_id, stage, intended_space_id, intended_area_sqm, rent_terms,
                        last_stage_change_at, last_stage_change_by, project_id, created_at, updated_at)
                VALUES (:id, :lead_id, :stage, :intended_space_id, :intended_area_sqm, CAST(:rent_terms AS jsonb),
                        :last_stage_change_at, :last_stage_change_by, :project_id, :created_at, :updated_at)
                ON CONFLICT (id) DO UPDATE SET
                    stage = EXCLUDED.stage, intended_space_id = EXCLUDED.intended_space_id,
                    intended_area_sqm = EXCLUDED.intended_area_sqm, rent_terms = EXCLUDED.rent_terms,
                    last_stage_change_at = EXCLUDED.last_stage_change_at,
                    last_stage_change_by = EXCLUDED.last_stage_change_by, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", intent.id())
                .addValue("lead_id", intent.leadId())
                .addValue("stage", intent.stage())
                .addValue("intended_space_id", intent.intendedSpaceId())
                .addValue("intended_area_sqm", intent.intendedAreaSqm())
                .addValue("rent_terms", writeJson(intent.rentTerms() == null ? Map.of() : intent.rentTerms()))
                .addValue("last_stage_change_at", timestampWithTimeZone(intent.lastStageChangeAt()))
                .addValue("last_stage_change_by", intent.lastStageChangeBy())
                .addValue("project_id", intent.projectId())
                .addValue("created_at", timestampWithTimeZone(intent.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(intent.updatedAt())));
        return intent;
    }

    @Override
    public List<LeasingIntentRecord> listIntents() {
        return jdbcTemplate.query("""
                SELECT id, lead_id, stage, intended_space_id, intended_area_sqm, rent_terms::text AS rent_terms,
                       last_stage_change_at, last_stage_change_by, project_id, created_at, updated_at
                  FROM leasing_intents ORDER BY created_at DESC, id LIMIT 500
                """, this::mapIntent);
    }

    @Override
    public Optional<LeasingIntentRecord> findIntentById(String id) {
        return jdbcTemplate.query("""
                SELECT id, lead_id, stage, intended_space_id, intended_area_sqm, rent_terms::text AS rent_terms,
                       last_stage_change_at, last_stage_change_by, project_id, created_at, updated_at
                  FROM leasing_intents WHERE id = :id
                """, Map.of("id", id), this::mapIntent).stream().findFirst();
    }

    // ---------- brands ----------
    @Override
    public LeasingBrandRecord saveBrand(LeasingBrandRecord brand) {
        jdbcTemplate.update("""
                INSERT INTO leasing_brands (id, brand_name, brand_type, business_type, blacklist_reason,
                        blacklisted_by, blacklisted_at, project_id, created_at, updated_at)
                VALUES (:id, :brand_name, :brand_type, :business_type, :blacklist_reason,
                        :blacklisted_by, :blacklisted_at, :project_id, :created_at, :updated_at)
                ON CONFLICT (id) DO UPDATE SET
                    brand_name = EXCLUDED.brand_name, brand_type = EXCLUDED.brand_type,
                    business_type = EXCLUDED.business_type, blacklist_reason = EXCLUDED.blacklist_reason,
                    blacklisted_by = EXCLUDED.blacklisted_by, blacklisted_at = EXCLUDED.blacklisted_at,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", brand.id())
                .addValue("brand_name", brand.brandName())
                .addValue("brand_type", brand.brandType())
                .addValue("business_type", brand.businessType())
                .addValue("blacklist_reason", brand.blacklistReason())
                .addValue("blacklisted_by", brand.blacklistedBy())
                .addValue("blacklisted_at", timestampWithTimeZone(brand.blacklistedAt()))
                .addValue("project_id", brand.projectId())
                .addValue("created_at", timestampWithTimeZone(brand.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(brand.updatedAt())));
        return brand;
    }

    @Override
    public List<LeasingBrandRecord> listBrands() {
        return jdbcTemplate.query("SELECT * FROM leasing_brands ORDER BY created_at DESC, id LIMIT 500", this::mapBrand);
    }

    @Override
    public Optional<LeasingBrandRecord> findBrandById(String id) {
        return jdbcTemplate.query("SELECT * FROM leasing_brands WHERE id = :id", Map.of("id", id), this::mapBrand)
                .stream().findFirst();
    }

    // ---------- follow-ups ----------
    @Override
    public LeasingFollowUpRecord saveFollowUp(LeasingFollowUpRecord followUp) {
        jdbcTemplate.update("""
                INSERT INTO leasing_follow_ups (id, ref_type, ref_id, method, content, next_plan_at,
                        operator_user_id, project_id, created_at)
                VALUES (:id, :ref_type, :ref_id, :method, :content, :next_plan_at,
                        :operator_user_id, :project_id, :created_at)
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", followUp.id())
                .addValue("ref_type", followUp.refType())
                .addValue("ref_id", followUp.refId())
                .addValue("method", followUp.method())
                .addValue("content", followUp.content())
                .addValue("next_plan_at", timestampWithTimeZone(followUp.nextPlanAt()))
                .addValue("operator_user_id", followUp.operatorUserId())
                .addValue("project_id", followUp.projectId())
                .addValue("created_at", timestampWithTimeZone(followUp.createdAt())));
        return followUp;
    }

    @Override
    public List<LeasingFollowUpRecord> listFollowUpsByRef(String refType, String refId) {
        return jdbcTemplate.query(
                "SELECT * FROM leasing_follow_ups WHERE ref_type = :ref_type AND ref_id = :ref_id "
                        + "ORDER BY created_at DESC, id LIMIT 500",
                new MapSqlParameterSource().addValue("ref_type", refType).addValue("ref_id", refId), this::mapFollowUp);
    }

    // ---------- row mappers ----------
    private LeasingLeadRecord mapLead(ResultSet rs, int n) throws SQLException {
        return new LeasingLeadRecord(
                rs.getString("id"), rs.getString("contact_name"), rs.getString("contact_phone"), rs.getString("source"),
                rs.getString("intended_business_type"), rs.getString("intended_space_id"),
                rs.getBigDecimal("intended_area_sqm"), rs.getString("brand_id"), rs.getString("owner_user_id"),
                rs.getString("status"), rs.getString("lost_or_hold_reason"), nullableInstant(rs, "last_follow_up_at"),
                rs.getString("project_id"), nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"));
    }

    private LeasingIntentRecord mapIntent(ResultSet rs, int n) throws SQLException {
        return new LeasingIntentRecord(
                rs.getString("id"), rs.getString("lead_id"), rs.getString("stage"), rs.getString("intended_space_id"),
                rs.getBigDecimal("intended_area_sqm"), readMap(rs.getString("rent_terms")),
                nullableInstant(rs, "last_stage_change_at"), rs.getString("last_stage_change_by"),
                rs.getString("project_id"), nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"));
    }

    private LeasingBrandRecord mapBrand(ResultSet rs, int n) throws SQLException {
        return new LeasingBrandRecord(
                rs.getString("id"), rs.getString("brand_name"), rs.getString("brand_type"),
                rs.getString("business_type"), rs.getString("blacklist_reason"), rs.getString("blacklisted_by"),
                nullableInstant(rs, "blacklisted_at"), rs.getString("project_id"),
                nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"));
    }

    private LeasingFollowUpRecord mapFollowUp(ResultSet rs, int n) throws SQLException {
        return new LeasingFollowUpRecord(
                rs.getString("id"), rs.getString("ref_type"), rs.getString("ref_id"), rs.getString("method"),
                rs.getString("content"), nullableInstant(rs, "next_plan_at"), rs.getString("operator_user_id"),
                rs.getString("project_id"), nullableInstant(rs, "created_at"));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize json", e);
        }
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return Map.of();
        }
    }
}
