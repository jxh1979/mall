package com.complex.platform.leasing;

import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

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

    @Override
    public LeasingLeadRecord saveLead(LeasingLeadRecord lead) {
        jdbcTemplate.update("""
                INSERT INTO leasing_leads (id, contact_name, intended_business_type, intended_space_id, source,
                        status, project_id, created_at, updated_at)
                VALUES (:id, :contact_name, :intended_business_type, :intended_space_id, :source,
                        :status, :project_id, :created_at, :updated_at)
                ON CONFLICT (id) DO UPDATE SET
                    contact_name = EXCLUDED.contact_name, intended_business_type = EXCLUDED.intended_business_type,
                    intended_space_id = EXCLUDED.intended_space_id, source = EXCLUDED.source,
                    status = EXCLUDED.status, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", lead.id())
                .addValue("contact_name", lead.contactName())
                .addValue("intended_business_type", lead.intendedBusinessType())
                .addValue("intended_space_id", lead.intendedSpaceId())
                .addValue("source", lead.source())
                .addValue("status", lead.status())
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
        return jdbcTemplate.query("SELECT * FROM leasing_leads WHERE id = :id",
                Map.of("id", id), this::mapLead).stream().findFirst();
    }

    @Override
    public LeasingOfferRecord saveOffer(LeasingOfferRecord offer) {
        jdbcTemplate.update("""
                INSERT INTO leasing_offers (id, lead_id, space_id, merchant_id, effective_date, expiry_date,
                        rent_terms, valid_until, status, converted_contract_id, project_id, created_at, updated_at)
                VALUES (:id, :lead_id, :space_id, :merchant_id, :effective_date, :expiry_date,
                        CAST(:rent_terms AS jsonb), :valid_until, :status, :converted_contract_id, :project_id,
                        :created_at, :updated_at)
                ON CONFLICT (id) DO UPDATE SET
                    space_id = EXCLUDED.space_id, merchant_id = EXCLUDED.merchant_id,
                    effective_date = EXCLUDED.effective_date, expiry_date = EXCLUDED.expiry_date,
                    rent_terms = EXCLUDED.rent_terms, valid_until = EXCLUDED.valid_until,
                    status = EXCLUDED.status, converted_contract_id = EXCLUDED.converted_contract_id,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", offer.id())
                .addValue("lead_id", offer.leadId())
                .addValue("space_id", offer.spaceId())
                .addValue("merchant_id", offer.merchantId())
                .addValue("effective_date", offer.effectiveDate())
                .addValue("expiry_date", offer.expiryDate())
                .addValue("rent_terms", writeJson(offer.rentTerms() == null ? Map.of() : offer.rentTerms()))
                .addValue("valid_until", offer.validUntil())
                .addValue("status", offer.status())
                .addValue("converted_contract_id", offer.convertedContractId())
                .addValue("project_id", offer.projectId())
                .addValue("created_at", timestampWithTimeZone(offer.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(offer.updatedAt())));
        return offer;
    }

    @Override
    public List<LeasingOfferRecord> listOffers() {
        return jdbcTemplate.query("""
                SELECT id, lead_id, space_id, merchant_id, effective_date, expiry_date, rent_terms::text AS rent_terms,
                       valid_until, status, converted_contract_id, project_id, created_at, updated_at
                  FROM leasing_offers ORDER BY created_at DESC, id LIMIT 500
                """, this::mapOffer);
    }

    @Override
    public Optional<LeasingOfferRecord> findOfferById(String id) {
        return jdbcTemplate.query("""
                SELECT id, lead_id, space_id, merchant_id, effective_date, expiry_date, rent_terms::text AS rent_terms,
                       valid_until, status, converted_contract_id, project_id, created_at, updated_at
                  FROM leasing_offers WHERE id = :id
                """, Map.of("id", id), this::mapOffer).stream().findFirst();
    }

    private LeasingLeadRecord mapLead(ResultSet rs, int n) throws SQLException {
        return new LeasingLeadRecord(
                rs.getString("id"), rs.getString("contact_name"), rs.getString("intended_business_type"),
                rs.getString("intended_space_id"), rs.getString("source"), rs.getString("status"),
                rs.getString("project_id"), nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"));
    }

    private LeasingOfferRecord mapOffer(ResultSet rs, int n) throws SQLException {
        return new LeasingOfferRecord(
                rs.getString("id"), rs.getString("lead_id"), rs.getString("space_id"), rs.getString("merchant_id"),
                rs.getObject("effective_date", LocalDate.class), rs.getObject("expiry_date", LocalDate.class),
                readMap(rs.getString("rent_terms")), rs.getObject("valid_until", LocalDate.class),
                rs.getString("status"), rs.getString("converted_contract_id"), rs.getString("project_id"),
                nullableInstant(rs, "created_at"), nullableInstant(rs, "updated_at"));
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
