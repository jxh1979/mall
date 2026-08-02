package com.complex.platform.merchantbilling;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcMerchantBillingRepository implements MerchantBillingRepository {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final String MERCHANT_BILL_SELECT = """
            SELECT id, merchant_id, ar_ap_item_id, bill_no, bill_period_start, bill_period_end,
                   due_date, total_amount_cent, currency, status, aging_days, source_type,
                   source_id, created_at, updated_at
              FROM merchant_bills
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcMerchantBillingRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public List<MerchantBillRecord> findAllBills() {
        return queryBillsWithLines(MERCHANT_BILL_SELECT + """
                 ORDER BY created_at DESC, id
                 LIMIT 200
                """, new MapSqlParameterSource());
    }

    @Override
    public List<MerchantBillRecord> findBillsByMerchantId(String merchantId) {
        return queryBillsWithLines(MERCHANT_BILL_SELECT + """
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                 LIMIT 200
                """, new MapSqlParameterSource("merchant_id", merchantId));
    }

    @Override
    public Optional<MerchantBillRecord> findBillById(String id) {
        return jdbcTemplate.query(MERCHANT_BILL_SELECT + """
                 WHERE id = :id
                """, Map.of("id", id), this::mapBill).stream().findFirst();
    }

    @Override
    public MerchantBillRecord saveBill(MerchantBillRecord record) {
        jdbcTemplate.update("""
                INSERT INTO merchant_bills (
                    id, merchant_id, ar_ap_item_id, bill_no, bill_period_start, bill_period_end,
                    due_date, total_amount_cent, currency, status, aging_days, source_type,
                    source_id, created_at, updated_at
                )
                VALUES (
                    :id, :merchant_id, :ar_ap_item_id, :bill_no, :bill_period_start, :bill_period_end,
                    :due_date, :total_amount_cent, :currency, :status, :aging_days, :source_type,
                    :source_id, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    merchant_id = EXCLUDED.merchant_id,
                    ar_ap_item_id = EXCLUDED.ar_ap_item_id,
                    bill_no = EXCLUDED.bill_no,
                    bill_period_start = EXCLUDED.bill_period_start,
                    bill_period_end = EXCLUDED.bill_period_end,
                    due_date = EXCLUDED.due_date,
                    total_amount_cent = EXCLUDED.total_amount_cent,
                    currency = EXCLUDED.currency,
                    status = EXCLUDED.status,
                    aging_days = EXCLUDED.aging_days,
                    source_type = EXCLUDED.source_type,
                    source_id = EXCLUDED.source_id,
                    updated_at = EXCLUDED.updated_at
                """, billParameters(record));
        if (record.lines() != null) {
            for (MerchantBillLineRecord line : record.lines()) {
                saveLine(line);
            }
        }
        return record;
    }

    @Override
    public MerchantBillDisputeRecord saveDispute(MerchantBillDisputeRecord record) {
        jdbcTemplate.update("""
                INSERT INTO merchant_bill_disputes (
                    id, bill_id, merchant_id, reason_code, description, status,
                    evidence_attachment_ids, operator_user_id, created_at, updated_at
                )
                VALUES (
                    :id, :bill_id, :merchant_id, :reason_code, :description, :status,
                    CAST(:evidence_attachment_ids AS jsonb), :operator_user_id, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    reason_code = EXCLUDED.reason_code,
                    description = EXCLUDED.description,
                    status = EXCLUDED.status,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    operator_user_id = EXCLUDED.operator_user_id,
                    updated_at = EXCLUDED.updated_at
                """, disputeParameters(record));
        return record;
    }

    @Override
    public List<MerchantBillDisputeRecord> findDisputesByBillId(String billId) {
        return jdbcTemplate.query("""
                SELECT id, bill_id, merchant_id, reason_code, description, status,
                       evidence_attachment_ids, operator_user_id, created_at, updated_at
                  FROM merchant_bill_disputes
                 WHERE bill_id = :bill_id
                 ORDER BY created_at DESC, id
                """, Map.of("bill_id", billId), this::mapDispute);
    }

    @Override
    public MerchantBillDunningRecord saveDunningRecord(MerchantBillDunningRecord record) {
        jdbcTemplate.update("""
                INSERT INTO merchant_bill_dunning_records (
                    id, bill_id, merchant_id, dunning_channel, message_template,
                    status, sent_at, operator_user_id, created_at
                )
                VALUES (
                    :id, :bill_id, :merchant_id, :dunning_channel, :message_template,
                    :status, :sent_at, :operator_user_id, :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    dunning_channel = EXCLUDED.dunning_channel,
                    message_template = EXCLUDED.message_template,
                    status = EXCLUDED.status,
                    sent_at = EXCLUDED.sent_at,
                    operator_user_id = EXCLUDED.operator_user_id
                """, dunningParameters(record));
        return record;
    }

    @Override
    public List<MerchantBillDunningRecord> findDunningRecordsByBillId(String billId) {
        return jdbcTemplate.query("""
                SELECT id, bill_id, merchant_id, dunning_channel, message_template,
                       status, sent_at, operator_user_id, created_at
                  FROM merchant_bill_dunning_records
                 WHERE bill_id = :bill_id
                 ORDER BY created_at DESC, id
                """, Map.of("bill_id", billId), this::mapDunning);
    }

    private MerchantBillLineRecord saveLine(MerchantBillLineRecord line) {
        jdbcTemplate.update("""
                INSERT INTO merchant_bill_lines (
                    id, bill_id, fee_type, description, amount_cent, currency, created_at
                )
                VALUES (
                    :id, :bill_id, :fee_type, :description, :amount_cent, :currency, :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    fee_type = EXCLUDED.fee_type,
                    description = EXCLUDED.description,
                    amount_cent = EXCLUDED.amount_cent,
                    currency = EXCLUDED.currency
                """, lineParameters(line));
        return line;
    }

    private MerchantBillRecord mapBill(ResultSet resultSet, int rowNumber) throws SQLException {
        String billId = resultSet.getString("id");
        return new MerchantBillRecord(
                billId,
                resultSet.getString("merchant_id"),
                resultSet.getString("ar_ap_item_id"),
                resultSet.getString("bill_no"),
                resultSet.getDate("bill_period_start").toLocalDate(),
                resultSet.getDate("bill_period_end").toLocalDate(),
                resultSet.getDate("due_date").toLocalDate(),
                resultSet.getLong("total_amount_cent"),
                resultSet.getString("currency"),
                resultSet.getString("status"),
                resultSet.getInt("aging_days"),
                resultSet.getString("source_type"),
                resultSet.getString("source_id"),
                findLinesByBillId(billId),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant()
        );
    }

    private List<MerchantBillLineRecord> findLinesByBillId(String billId) {
        return jdbcTemplate.query("""
                SELECT id, bill_id, fee_type, description, amount_cent, currency, created_at
                  FROM merchant_bill_lines
                 WHERE bill_id = :bill_id
                 ORDER BY CASE fee_type
                              WHEN 'rent' THEN 1
                              WHEN 'property_fee' THEN 2
                              WHEN 'energy_fee' THEN 3
                              WHEN 'cam' THEN 4
                              WHEN 'deposit' THEN 5
                              ELSE 6
                          END,
                          created_at ASC,
                          id
                """, Map.of("bill_id", billId), this::mapLine);
    }

    private MerchantBillLineRecord mapLine(ResultSet resultSet, int rowNumber) throws SQLException {
        return new MerchantBillLineRecord(
                resultSet.getString("id"),
                resultSet.getString("bill_id"),
                resultSet.getString("fee_type"),
                resultSet.getString("description"),
                resultSet.getLong("amount_cent"),
                resultSet.getString("currency"),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }

    private MerchantBillDisputeRecord mapDispute(ResultSet resultSet, int rowNumber) throws SQLException {
        return new MerchantBillDisputeRecord(
                resultSet.getString("id"),
                resultSet.getString("bill_id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("reason_code"),
                resultSet.getString("description"),
                resultSet.getString("status"),
                readStringList(resultSet.getString("evidence_attachment_ids")),
                resultSet.getString("operator_user_id"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant()
        );
    }

    private MerchantBillDunningRecord mapDunning(ResultSet resultSet, int rowNumber) throws SQLException {
        return new MerchantBillDunningRecord(
                resultSet.getString("id"),
                resultSet.getString("bill_id"),
                resultSet.getString("merchant_id"),
                resultSet.getString("dunning_channel"),
                resultSet.getString("message_template"),
                resultSet.getString("status"),
                resultSet.getTimestamp("sent_at").toInstant(),
                resultSet.getString("operator_user_id"),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }

    private MapSqlParameterSource billParameters(MerchantBillRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("merchant_id", record.merchantId())
                .addValue("ar_ap_item_id", record.arApItemId())
                .addValue("bill_no", record.billNo())
                .addValue("bill_period_start", record.billPeriodStart())
                .addValue("bill_period_end", record.billPeriodEnd())
                .addValue("due_date", record.dueDate())
                .addValue("total_amount_cent", record.totalAmountCent())
                .addValue("currency", record.currency())
                .addValue("status", record.status())
                .addValue("aging_days", record.agingDays())
                .addValue("source_type", record.sourceType())
                .addValue("source_id", record.sourceId())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt()));
    }

    private MapSqlParameterSource lineParameters(MerchantBillLineRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("bill_id", record.billId())
                .addValue("fee_type", record.feeType())
                .addValue("description", record.description())
                .addValue("amount_cent", record.amountCent())
                .addValue("currency", record.currency())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()));
    }

    private MapSqlParameterSource disputeParameters(MerchantBillDisputeRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("bill_id", record.billId())
                .addValue("merchant_id", record.merchantId())
                .addValue("reason_code", record.reasonCode())
                .addValue("description", record.description())
                .addValue("status", record.status())
                .addValue("evidence_attachment_ids", writeJson(record.evidenceAttachmentIds()))
                .addValue("operator_user_id", record.operatorUserId())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt()));
    }

    private MapSqlParameterSource dunningParameters(MerchantBillDunningRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("bill_id", record.billId())
                .addValue("merchant_id", record.merchantId())
                .addValue("dunning_channel", record.dunningChannel())
                .addValue("message_template", record.messageTemplate())
                .addValue("status", record.status())
                .addValue("sent_at", timestampWithTimeZone(record.sentAt()))
                .addValue("operator_user_id", record.operatorUserId())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()));
    }


    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("json field is not serializable", exception);
        }
    }

    private List<String> readStringList(String value) {
        try {
            return value == null ? List.of() : objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("json field is not readable", exception);
        }
    }

    private List<MerchantBillRecord> queryBillsWithLines(String sql, MapSqlParameterSource params) {
        List<MerchantBillRecord> headers = jdbcTemplate.query(sql, params, this::mapBillHeader);
        if (headers.isEmpty()) {
            return headers;
        }
        List<String> billIds = headers.stream().map(MerchantBillRecord::id).toList();
        Map<String, List<MerchantBillLineRecord>> linesMap = findLinesByBillIds(billIds);
        return headers.stream()
                .map(bill -> bill.withLines(linesMap.getOrDefault(bill.id(), List.of())))
                .toList();
    }

    private MerchantBillRecord mapBillHeader(ResultSet rs, int rowNum) throws SQLException {
        return new MerchantBillRecord(
                rs.getString("id"),
                rs.getString("merchant_id"),
                rs.getString("ar_ap_item_id"),
                rs.getString("bill_no"),
                rs.getDate("bill_period_start").toLocalDate(),
                rs.getDate("bill_period_end").toLocalDate(),
                rs.getDate("due_date").toLocalDate(),
                rs.getLong("total_amount_cent"),
                rs.getString("currency"),
                rs.getString("status"),
                rs.getInt("aging_days"),
                rs.getString("source_type"),
                rs.getString("source_id"),
                List.of(),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private Map<String, List<MerchantBillLineRecord>> findLinesByBillIds(List<String> billIds) {
        List<MerchantBillLineRecord> allLines = jdbcTemplate.query("""
                SELECT id, bill_id, fee_type, description, amount_cent, currency, created_at
                  FROM merchant_bill_lines
                 WHERE bill_id IN (:bill_ids)
                 ORDER BY bill_id,
                          CASE fee_type
                              WHEN 'rent' THEN 1
                              WHEN 'property_fee' THEN 2
                              WHEN 'energy_fee' THEN 3
                              WHEN 'cam' THEN 4
                              WHEN 'deposit' THEN 5
                              ELSE 6
                          END,
                          created_at ASC, id
                """,
                new MapSqlParameterSource("bill_ids", billIds),
                this::mapLine);
        Map<String, List<MerchantBillLineRecord>> linesByBillId = new LinkedHashMap<>();
        for (MerchantBillLineRecord line : allLines) {
            linesByBillId.computeIfAbsent(line.billId(), k -> new ArrayList<>()).add(line);
        }
        return linesByBillId;
    }
}
