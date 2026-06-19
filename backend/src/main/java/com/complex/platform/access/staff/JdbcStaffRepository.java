package com.complex.platform.access.staff;
import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcStaffRepository implements StaffRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcStaffRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ---------------------------------------------------------------- staff_profiles

    @Override
    public StaffProfileRecord saveProfile(StaffProfileRecord profile) {
        jdbcTemplate.update("""
                INSERT INTO staff_profiles (
                    id, project_id, merchant_id, user_id, name, phone_hash, id_doc_hash, staff_level_id,
                    registration_status, approved_by, approved_at, reject_reason, status, created_at, updated_at
                )
                VALUES (
                    :id, :project_id, :merchant_id, :user_id, :name, :phone_hash, :id_doc_hash, :staff_level_id,
                    :registration_status, :approved_by, :approved_at, :reject_reason, :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    project_id = EXCLUDED.project_id,
                    merchant_id = EXCLUDED.merchant_id,
                    user_id = EXCLUDED.user_id,
                    name = EXCLUDED.name,
                    phone_hash = EXCLUDED.phone_hash,
                    id_doc_hash = EXCLUDED.id_doc_hash,
                    staff_level_id = EXCLUDED.staff_level_id,
                    registration_status = EXCLUDED.registration_status,
                    approved_by = EXCLUDED.approved_by,
                    approved_at = EXCLUDED.approved_at,
                    reject_reason = EXCLUDED.reject_reason,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", profile.id())
                .addValue("project_id", profile.projectId())
                .addValue("merchant_id", profile.merchantId())
                .addValue("user_id", profile.userId())
                .addValue("name", profile.name())
                .addValue("phone_hash", profile.phoneHash())
                .addValue("id_doc_hash", profile.idDocHash())
                .addValue("staff_level_id", profile.staffLevelId())
                .addValue("registration_status", profile.registrationStatus())
                .addValue("approved_by", profile.approvedBy())
                .addValue("approved_at", timestampWithTimeZone(profile.approvedAt()))
                .addValue("reject_reason", profile.rejectReason())
                .addValue("status", profile.status())
                .addValue("created_at", timestampWithTimeZone(profile.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(profile.updatedAt())));
        return profile;
    }

    @Override
    public Optional<StaffProfileRecord> findProfileById(String id) {
        List<StaffProfileRecord> result = jdbcTemplate.query("""
                SELECT id, project_id, merchant_id, user_id, name, phone_hash, id_doc_hash, staff_level_id,
                       registration_status, approved_by, approved_at, reject_reason, status, created_at, updated_at
                  FROM staff_profiles
                 WHERE id = :id
                """, Map.of("id", id), this::mapProfile);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public List<StaffProfileRecord> listProfiles(String merchantScope) {
        if (merchantScope == null) {
            return jdbcTemplate.query("""
                    SELECT id, project_id, merchant_id, user_id, name, phone_hash, id_doc_hash, staff_level_id,
                           registration_status, approved_by, approved_at, reject_reason, status, created_at, updated_at
                      FROM staff_profiles
                     ORDER BY created_at DESC, id
                    """, this::mapProfile);
        }
        return jdbcTemplate.query("""
                SELECT id, project_id, merchant_id, user_id, name, phone_hash, id_doc_hash, staff_level_id,
                       registration_status, approved_by, approved_at, reject_reason, status, created_at, updated_at
                  FROM staff_profiles
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantScope), this::mapProfile);
    }

    // ---------------------------------------------------------- staff_device_credentials

    @Override
    public StaffDeviceCredentialRecord saveCredential(StaffDeviceCredentialRecord credential) {
        jdbcTemplate.update("""
                INSERT INTO staff_device_credentials (
                    id, staff_profile_id, device_public_key, device_fingerprint_hash, bind_status,
                    supersedes_credential_id, integrity_status, revoked_reason, bound_at, revoked_at,
                    created_at, updated_at
                )
                VALUES (
                    :id, :staff_profile_id, :device_public_key, :device_fingerprint_hash, :bind_status,
                    :supersedes_credential_id, :integrity_status, :revoked_reason, :bound_at, :revoked_at,
                    :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    device_public_key = EXCLUDED.device_public_key,
                    device_fingerprint_hash = EXCLUDED.device_fingerprint_hash,
                    bind_status = EXCLUDED.bind_status,
                    supersedes_credential_id = EXCLUDED.supersedes_credential_id,
                    integrity_status = EXCLUDED.integrity_status,
                    revoked_reason = EXCLUDED.revoked_reason,
                    bound_at = EXCLUDED.bound_at,
                    revoked_at = EXCLUDED.revoked_at,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", credential.id())
                .addValue("staff_profile_id", credential.staffProfileId())
                .addValue("device_public_key", credential.devicePublicKey())
                .addValue("device_fingerprint_hash", credential.deviceFingerprintHash())
                .addValue("bind_status", credential.bindStatus())
                .addValue("supersedes_credential_id", credential.supersedesCredentialId())
                .addValue("integrity_status", credential.integrityStatus())
                .addValue("revoked_reason", credential.revokedReason())
                .addValue("bound_at", timestampWithTimeZone(credential.boundAt()))
                .addValue("revoked_at", timestampWithTimeZone(credential.revokedAt()))
                .addValue("created_at", timestampWithTimeZone(credential.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(credential.updatedAt())));
        return credential;
    }

    @Override
    public Optional<StaffDeviceCredentialRecord> findCredentialById(String id) {
        List<StaffDeviceCredentialRecord> result = jdbcTemplate.query(
                credentialSelect() + " WHERE id = :id",
                Map.of("id", id), this::mapCredential);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public Optional<StaffDeviceCredentialRecord> findActiveCredential(String staffProfileId) {
        List<StaffDeviceCredentialRecord> result = jdbcTemplate.query(
                credentialSelect() + " WHERE staff_profile_id = :staff_profile_id AND bind_status = 'active'",
                Map.of("staff_profile_id", staffProfileId), this::mapCredential);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public List<StaffDeviceCredentialRecord> listCredentials(String staffProfileId) {
        return jdbcTemplate.query(
                credentialSelect() + " WHERE staff_profile_id = :staff_profile_id ORDER BY created_at DESC, id",
                Map.of("staff_profile_id", staffProfileId), this::mapCredential);
    }

    private String credentialSelect() {
        return """
                SELECT id, staff_profile_id, device_public_key, device_fingerprint_hash, bind_status,
                       supersedes_credential_id, integrity_status, revoked_reason, bound_at, revoked_at,
                       created_at, updated_at
                  FROM staff_device_credentials
                """;
    }

    // ---------------------------------------------------------------- employee_notices

    @Override
    public EmployeeNoticeRecord saveNotice(EmployeeNoticeRecord notice) {
        jdbcTemplate.update("""
                INSERT INTO employee_notices (
                    id, staff_profile_id, notice_type, title, body, consent_required, consent_status,
                    consented_at, created_by, created_at, updated_at
                )
                VALUES (
                    :id, :staff_profile_id, :notice_type, :title, :body, :consent_required, :consent_status,
                    :consented_at, :created_by, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    notice_type = EXCLUDED.notice_type,
                    title = EXCLUDED.title,
                    body = EXCLUDED.body,
                    consent_required = EXCLUDED.consent_required,
                    consent_status = EXCLUDED.consent_status,
                    consented_at = EXCLUDED.consented_at,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", notice.id())
                .addValue("staff_profile_id", notice.staffProfileId())
                .addValue("notice_type", notice.noticeType())
                .addValue("title", notice.title())
                .addValue("body", notice.body())
                .addValue("consent_required", notice.consentRequired())
                .addValue("consent_status", notice.consentStatus())
                .addValue("consented_at", timestampWithTimeZone(notice.consentedAt()))
                .addValue("created_by", notice.createdBy())
                .addValue("created_at", timestampWithTimeZone(notice.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(notice.updatedAt())));
        return notice;
    }

    @Override
    public Optional<EmployeeNoticeRecord> findNoticeById(String id) {
        List<EmployeeNoticeRecord> result = jdbcTemplate.query("""
                SELECT id, staff_profile_id, notice_type, title, body, consent_required, consent_status,
                       consented_at, created_by, created_at, updated_at
                  FROM employee_notices
                 WHERE id = :id
                """, Map.of("id", id), this::mapNotice);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public List<EmployeeNoticeRecord> listNotices(String merchantScope) {
        if (merchantScope == null) {
            return jdbcTemplate.query("""
                    SELECT id, staff_profile_id, notice_type, title, body, consent_required, consent_status,
                           consented_at, created_by, created_at, updated_at
                      FROM employee_notices
                     ORDER BY created_at DESC, id
                    """, this::mapNotice);
        }
        // 隔离：仅返回所属员工 merchant_id=该值的通知（跨商户不可见）。
        return jdbcTemplate.query("""
                SELECT n.id, n.staff_profile_id, n.notice_type, n.title, n.body, n.consent_required,
                       n.consent_status, n.consented_at, n.created_by, n.created_at, n.updated_at
                  FROM employee_notices n
                  JOIN staff_profiles p ON p.id = n.staff_profile_id
                 WHERE p.merchant_id = :merchant_id
                 ORDER BY n.created_at DESC, n.id
                """, Map.of("merchant_id", merchantScope), this::mapNotice);
    }

    // ---------------------------------------------------------------- row mappers

    private StaffProfileRecord mapProfile(ResultSet rs, int rowNumber) throws SQLException {
        return new StaffProfileRecord(
                rs.getString("id"),
                rs.getString("project_id"),
                rs.getString("merchant_id"),
                rs.getString("user_id"),
                rs.getString("name"),
                rs.getString("phone_hash"),
                rs.getString("id_doc_hash"),
                rs.getString("staff_level_id"),
                rs.getString("registration_status"),
                rs.getString("approved_by"),
                nullableInstant(rs, "approved_at"),
                rs.getString("reject_reason"),
                rs.getString("status"),
                nullableInstant(rs, "created_at"),
                nullableInstant(rs, "updated_at")
        );
    }

    private StaffDeviceCredentialRecord mapCredential(ResultSet rs, int rowNumber) throws SQLException {
        return new StaffDeviceCredentialRecord(
                rs.getString("id"),
                rs.getString("staff_profile_id"),
                rs.getString("device_public_key"),
                rs.getString("device_fingerprint_hash"),
                rs.getString("bind_status"),
                rs.getString("supersedes_credential_id"),
                rs.getString("integrity_status"),
                rs.getString("revoked_reason"),
                nullableInstant(rs, "bound_at"),
                nullableInstant(rs, "revoked_at"),
                nullableInstant(rs, "created_at"),
                nullableInstant(rs, "updated_at")
        );
    }

    private EmployeeNoticeRecord mapNotice(ResultSet rs, int rowNumber) throws SQLException {
        return new EmployeeNoticeRecord(
                rs.getString("id"),
                rs.getString("staff_profile_id"),
                rs.getString("notice_type"),
                rs.getString("title"),
                rs.getString("body"),
                rs.getBoolean("consent_required"),
                rs.getString("consent_status"),
                nullableInstant(rs, "consented_at"),
                rs.getString("created_by"),
                nullableInstant(rs, "created_at"),
                nullableInstant(rs, "updated_at")
        );
    }
}
