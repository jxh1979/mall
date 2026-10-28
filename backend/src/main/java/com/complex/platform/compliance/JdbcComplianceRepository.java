package com.complex.platform.compliance;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcComplianceRepository implements ComplianceRepository {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Integer>> INTEGER_LIST_TYPE = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcComplianceRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public List<ContractKeyDateRecord> findAllContractKeyDates() {
        return jdbcTemplate.query("""
                SELECT id, contract_id, merchant_id, date_type, title, due_date, reminder_days,
                       status, owner_user_id, source_type, human_confirmed, evidence_attachment_ids,
                       created_at, updated_at
                  FROM contract_key_dates
                 ORDER BY due_date ASC, id
                """, this::mapContractKeyDate);
    }

    @Override
    public List<ContractKeyDateRecord> findContractKeyDatesByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, contract_id, merchant_id, date_type, title, due_date, reminder_days,
                       status, owner_user_id, source_type, human_confirmed, evidence_attachment_ids,
                       created_at, updated_at
                  FROM contract_key_dates
                 WHERE merchant_id = :merchant_id
                 ORDER BY due_date ASC, id
                """, Map.of("merchant_id", merchantId), this::mapContractKeyDate);
    }

    @Override
    public ContractKeyDateRecord saveContractKeyDate(ContractKeyDateRecord record) {
        jdbcTemplate.update("""
                INSERT INTO contract_key_dates (
                    id, contract_id, merchant_id, date_type, title, due_date, reminder_days,
                    status, owner_user_id, source_type, human_confirmed, evidence_attachment_ids,
                    created_at, updated_at
                )
                VALUES (
                    :id, :contract_id, :merchant_id, :date_type, :title, :due_date, CAST(:reminder_days AS jsonb),
                    :status, :owner_user_id, :source_type, :human_confirmed, CAST(:evidence_attachment_ids AS jsonb),
                    :created_at, :updated_at
                )
                ON CONFLICT (id) DO NOTHING
                """, contractKeyDateParameters(record));
        return record;
    }

    @Override
    public List<ComplianceExpiryItemRecord> findAllExpiryItems() {
        return jdbcTemplate.query("""
                SELECT id, subject_type, subject_id, merchant_id, item_type, item_name, document_no,
                       issuing_authority, valid_from, valid_to, reminder_days, expiry_action, status,
                       owner_user_id, source_type, human_confirmed, evidence_attachment_ids, created_at, updated_at,
                       grace_status, grace_until
                  FROM compliance_expiry_items
                 ORDER BY valid_to ASC, id
                """, this::mapExpiryItem);
    }

    @Override
    public List<ComplianceExpiryItemRecord> findExpiryItemsByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, subject_type, subject_id, merchant_id, item_type, item_name, document_no,
                       issuing_authority, valid_from, valid_to, reminder_days, expiry_action, status,
                       owner_user_id, source_type, human_confirmed, evidence_attachment_ids, created_at, updated_at,
                       grace_status, grace_until
                  FROM compliance_expiry_items
                 WHERE merchant_id = :merchant_id
                 ORDER BY valid_to ASC, id
                """, Map.of("merchant_id", merchantId), this::mapExpiryItem);
    }

    @Override
    public ComplianceExpiryItemRecord saveExpiryItem(ComplianceExpiryItemRecord record) {
        jdbcTemplate.update("""
                INSERT INTO compliance_expiry_items (
                    id, subject_type, subject_id, merchant_id, item_type, item_name, document_no,
                    issuing_authority, valid_from, valid_to, reminder_days, expiry_action, status,
                    owner_user_id, source_type, human_confirmed, evidence_attachment_ids, created_at, updated_at
                )
                VALUES (
                    :id, :subject_type, :subject_id, :merchant_id, :item_type, :item_name, :document_no,
                    :issuing_authority, :valid_from, :valid_to, CAST(:reminder_days AS jsonb), :expiry_action, :status,
                    :owner_user_id, :source_type, :human_confirmed, CAST(:evidence_attachment_ids AS jsonb),
                    :created_at, :updated_at
                )
                ON CONFLICT (id) DO NOTHING
                """, expiryItemParameters(record));
        return record;
    }

    @Override
    public List<ComplianceReminderRecord> findAllReminders() {
        return jdbcTemplate.query("""
                SELECT id, merchant_id, source_type, source_id, reminder_type, due_date, reminder_at, severity,
                       status, blocking_suggested, message, generated_at
                  FROM compliance_reminders
                 ORDER BY due_date ASC, generated_at DESC, id
                """, this::mapReminder);
    }

    @Override
    public List<ComplianceReminderRecord> findRemindersByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, merchant_id, source_type, source_id, reminder_type, due_date, reminder_at, severity,
                       status, blocking_suggested, message, generated_at
                  FROM compliance_reminders
                 WHERE merchant_id = :merchant_id
                 ORDER BY due_date ASC, generated_at DESC, id
                """, Map.of("merchant_id", merchantId), this::mapReminder);
    }

    @Override
    public ComplianceReminderRecord saveReminder(ComplianceReminderRecord record) {
        jdbcTemplate.update("""
                INSERT INTO compliance_reminders (
                    id, merchant_id, source_type, source_id, reminder_type, due_date, reminder_at, severity,
                    status, blocking_suggested, message, generated_at
                )
                VALUES (
                    :id, :merchant_id, :source_type, :source_id, :reminder_type, :due_date, :reminder_at, :severity,
                    :status, :blocking_suggested, :message, :generated_at
                )
                ON CONFLICT (source_type, source_id, reminder_type, due_date) DO UPDATE SET
                    id = EXCLUDED.id,
                    merchant_id = EXCLUDED.merchant_id,
                    severity = EXCLUDED.severity,
                    status = EXCLUDED.status,
                    blocking_suggested = EXCLUDED.blocking_suggested,
                    message = EXCLUDED.message,
                    generated_at = EXCLUDED.generated_at
                """, reminderParameters(record));
        return record;
    }

    @Override
    public List<ComplianceItemTypeConfigRecord> findItemTypeConfigs() {
        return jdbcTemplate.query("""
                SELECT item_type, grace_days, grace_min_days, grace_max_days, warning_months, warning_min_months, warning_max_months
                  FROM compliance_item_type_configs
                 ORDER BY item_type
                """, (rs, rowNumber) -> new ComplianceItemTypeConfigRecord(
                rs.getString("item_type"),
                rs.getObject("grace_days", Integer.class),
                rs.getObject("grace_min_days", Integer.class),
                rs.getObject("grace_max_days", Integer.class),
                rs.getObject("warning_months", Integer.class),
                rs.getObject("warning_min_months", Integer.class),
                rs.getObject("warning_max_months", Integer.class)));
    }

    @Override
    public void saveItemTypeConfig(ComplianceItemTypeConfigRecord record, String actorId) {
        jdbcTemplate.update("""
                INSERT INTO compliance_item_type_configs (item_type, grace_days, grace_min_days, grace_max_days,
                    warning_months, warning_min_months, warning_max_months, updated_by, updated_at)
                VALUES (:item_type, :grace_days, :grace_min_days, :grace_max_days,
                    :warning_months, :warning_min_months, :warning_max_months, :updated_by, now())
                ON CONFLICT (item_type) DO UPDATE SET
                    grace_days = EXCLUDED.grace_days,
                    grace_min_days = EXCLUDED.grace_min_days,
                    grace_max_days = EXCLUDED.grace_max_days,
                    warning_months = EXCLUDED.warning_months,
                    warning_min_months = EXCLUDED.warning_min_months,
                    warning_max_months = EXCLUDED.warning_max_months,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = now()
                """, new MapSqlParameterSource()
                .addValue("item_type", record.itemType())
                .addValue("grace_days", record.graceDays(), Types.INTEGER)
                .addValue("grace_min_days", record.graceMinDays(), Types.INTEGER)
                .addValue("grace_max_days", record.graceMaxDays(), Types.INTEGER)
                .addValue("warning_months", record.warningMonths(), Types.INTEGER)
                .addValue("warning_min_months", record.warningMinMonths(), Types.INTEGER)
                .addValue("warning_max_months", record.warningMaxMonths(), Types.INTEGER)
                .addValue("updated_by", actorId));
    }

    @Override
    public Optional<ComplianceExpiryItemRecord> findExpiryItemForUpdate(String id) {
        return jdbcTemplate.query("""
                SELECT id, subject_type, subject_id, merchant_id, item_type, item_name, document_no,
                       issuing_authority, valid_from, valid_to, reminder_days, expiry_action, status,
                       owner_user_id, source_type, human_confirmed, evidence_attachment_ids, created_at, updated_at,
                       grace_status, grace_until
                  FROM compliance_expiry_items
                 WHERE id = :id
                   FOR UPDATE
                """, Map.of("id", id), this::mapExpiryItem).stream().findFirst();
    }

    @Override
    public void markGraceRequested(String id, LocalDate graceUntil, String reviewId) {
        jdbcTemplate.update("""
                UPDATE compliance_expiry_items
                   SET grace_status = 'pending_review', grace_until = :grace_until, grace_review_id = :review_id, updated_at = now()
                 WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("grace_until", graceUntil)
                .addValue("review_id", reviewId));
    }

    @Override
    public int decideGrace(String id, String reviewId, String decision) {
        return jdbcTemplate.update("""
                UPDATE compliance_expiry_items
                   SET grace_status = :decision, updated_at = now()
                 WHERE id = :id AND grace_review_id = :review_id AND grace_status = 'pending_review'
                """, Map.of("id", id, "review_id", reviewId, "decision", decision));
    }

    @Override
    public void renewExpiryItem(String id, LocalDate validFrom, LocalDate validTo, String documentNo, List<String> evidenceAttachmentIds) {
        jdbcTemplate.update("""
                UPDATE compliance_expiry_items
                   SET valid_from = :valid_from, valid_to = :valid_to, document_no = :document_no,
                       evidence_attachment_ids = CAST(:evidence_attachment_ids AS jsonb),
                       grace_status = NULL, grace_until = NULL, grace_review_id = NULL, updated_at = now()
                 WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("valid_from", validFrom)
                .addValue("valid_to", validTo)
                .addValue("document_no", documentNo, Types.VARCHAR)
                .addValue("evidence_attachment_ids", writeJson(evidenceAttachmentIds)));
    }

    private MapSqlParameterSource contractKeyDateParameters(ContractKeyDateRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("contract_id", record.contractId())
                .addValue("merchant_id", record.merchantId())
                .addValue("date_type", record.dateType())
                .addValue("title", record.title())
                .addValue("due_date", record.dueDate())
                .addValue("reminder_days", writeJson(record.reminderDays()))
                .addValue("status", record.status())
                .addValue("owner_user_id", record.ownerUserId())
                .addValue("source_type", record.sourceType())
                .addValue("human_confirmed", record.humanConfirmed())
                .addValue("evidence_attachment_ids", writeJson(record.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt()));
    }

    private MapSqlParameterSource expiryItemParameters(ComplianceExpiryItemRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("subject_type", record.subjectType())
                .addValue("subject_id", record.subjectId())
                .addValue("merchant_id", record.merchantId())
                .addValue("item_type", record.itemType())
                .addValue("item_name", record.itemName())
                .addValue("document_no", record.documentNo())
                .addValue("issuing_authority", record.issuingAuthority())
                .addValue("valid_from", record.validFrom())
                .addValue("valid_to", record.validTo())
                .addValue("reminder_days", writeJson(record.reminderDays()))
                .addValue("expiry_action", record.expiryAction())
                .addValue("status", record.status())
                .addValue("owner_user_id", record.ownerUserId())
                .addValue("source_type", record.sourceType())
                .addValue("human_confirmed", record.humanConfirmed())
                .addValue("evidence_attachment_ids", writeJson(record.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt()));
    }

    private MapSqlParameterSource reminderParameters(ComplianceReminderRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("merchant_id", record.merchantId())
                .addValue("source_type", record.sourceType())
                .addValue("source_id", record.sourceId())
                .addValue("reminder_type", record.reminderType())
                .addValue("due_date", record.dueDate())
                .addValue("reminder_at", record.reminderAt())
                .addValue("severity", record.severity())
                .addValue("status", record.status())
                .addValue("blocking_suggested", record.blockingSuggested())
                .addValue("message", record.message())
                .addValue("generated_at", timestampWithTimeZone(record.generatedAt()));
    }

    private ContractKeyDateRecord mapContractKeyDate(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ContractKeyDateRecord(
                resultSet.getString("id"),
                resultSet.getString("contract_id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("date_type"),
                resultSet.getString("title"),
                resultSet.getObject("due_date", java.time.LocalDate.class),
                readIntegerList(resultSet.getString("reminder_days")),
                resultSet.getString("status"),
                resultSet.getString("owner_user_id"),
                resultSet.getString("source_type"),
                resultSet.getBoolean("human_confirmed"),
                readStringList(resultSet.getString("evidence_attachment_ids")),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant()
        );
    }

    private ComplianceExpiryItemRecord mapExpiryItem(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ComplianceExpiryItemRecord(
                resultSet.getString("id"),
                resultSet.getString("subject_type"),
                resultSet.getString("subject_id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("item_type"),
                resultSet.getString("item_name"),
                resultSet.getString("document_no"),
                resultSet.getString("issuing_authority"),
                resultSet.getObject("valid_from", java.time.LocalDate.class),
                resultSet.getObject("valid_to", java.time.LocalDate.class),
                readIntegerList(resultSet.getString("reminder_days")),
                resultSet.getString("expiry_action"),
                resultSet.getString("status"),
                resultSet.getString("owner_user_id"),
                resultSet.getString("source_type"),
                resultSet.getBoolean("human_confirmed"),
                readStringList(resultSet.getString("evidence_attachment_ids")),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant(),
                resultSet.getString("grace_status"),
                resultSet.getObject("grace_until", LocalDate.class)
        );
    }

    private ComplianceReminderRecord mapReminder(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ComplianceReminderRecord(
                resultSet.getString("id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("source_type"),
                resultSet.getString("source_id"),
                resultSet.getString("reminder_type"),
                resultSet.getObject("due_date", java.time.LocalDate.class),
                resultSet.getObject("reminder_at", java.time.LocalDate.class),
                resultSet.getString("severity"),
                resultSet.getString("status"),
                resultSet.getBoolean("blocking_suggested"),
                resultSet.getString("message"),
                resultSet.getTimestamp("generated_at").toInstant()
        );
    }


    // AUD-020：序列化失败/库内坏 JSON 是服务端数据问题，须走 500 并记堆栈（IllegalArgumentException 会被全局映射 400 掩盖）。
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("json field is not serializable", exception);
        }
    }

    private List<String> readStringList(String value) {
        try {
            return value == null ? List.of() : objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("json string list is not readable", exception);
        }
    }

    private List<Integer> readIntegerList(String value) {
        try {
            return value == null ? List.of() : objectMapper.readValue(value, INTEGER_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("json integer list is not readable", exception);
        }
    }
}
