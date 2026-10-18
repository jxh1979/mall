package com.complex.platform.energy;

// 文件版本：V0.01
import com.complex.platform.common.api.ApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EnergyWorkflowRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    public EnergyWorkflowRepository(JdbcTemplate jdbc, ObjectMapper json) { this.jdbc=jdbc; this.json=json; }
    public record Opening(String meterId, BigDecimal initialValue, Instant initialAt, String recordedBy,
            String note, List<String> attachmentIds) { }
    public record Verification(String id, String readingId, String projectId, String merchantId,
            BigDecimal verifiedValue, BigDecimal verifiedOpeningValue, String method, String reason,
            List<String> attachmentIds, String verifiedBy, String reviewId, String status, Instant verifiedAt) { }
    public record Charge(String id, String contractId, String meterId, String merchantId, String projectId,
            String period, long amountCent, LocalDate dueDate, String supplierBillReference,
            List<String> attachmentIds, List<String> readingIds, BigDecimal consumption,
            String recordedBy, Instant createdAt, String arApItemId, Instant settledAt) { }
    public record Replacement(String id, String oldMeterId, String newMeterId, String period,
            BigDecimal oldFinalValue, BigDecimal newInitialValue, Instant replacedAt,
            String oldReadingId, String note, List<String> attachmentIds, String recordedBy) { }
    public record Retirement(String meterId, String terminalReadingId, Instant retiredAt) { }
    public void saveRetirement(Retirement r) { jdbc.update("INSERT INTO energy_meter_retirements(meter_id,terminal_reading_id,retired_at) VALUES(?,?,?)",r.meterId(),r.terminalReadingId(),java.sql.Timestamp.from(r.retiredAt())); }
    public Optional<Retirement> retirement(String meter) { return jdbc.query("SELECT meter_id,terminal_reading_id,retired_at FROM energy_meter_retirements WHERE meter_id=?",(r,n)->new Retirement(r.getString("meter_id"),r.getString("terminal_reading_id"),r.getTimestamp("retired_at").toInstant()),meter).stream().findFirst(); }
    public record Settings(String id, String projectId, int days, int minDays, int maxDays,
            String supervisorId, String reviewId, String status, Instant createdAt) { }
    public record Assignment(String readingId, String verifierId, String reason, String assignedBy, Instant assignedAt) { }

    public void lockMeter(String meterId) { jdbc.queryForList("SELECT id FROM energy_meters WHERE id=? FOR UPDATE",meterId); }
    public void lockPeriod(EnergyMeterRecord meter, String period) {
        jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",
                "energy:"+meter.projectId()+":"+(meter.merchantId()==null?meter.id():meter.merchantId())+":"+period);
    }
    public void lockPeriod(String merchantId,String projectId,String period) {
        jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,0))","energy:"+projectId+":"+merchantId+":"+period);
    }
    public boolean periodSettled(EnergyMeterRecord meter,String period) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM energy_actual_charges WHERE project_id=? AND merchant_id=? AND period=? AND settled_at IS NOT NULL)",Boolean.class,meter.projectId(),meter.merchantId(),period));
    }
    public boolean hasCharge(String meter,String period) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM energy_actual_charges WHERE meter_id=? AND period=?)",Boolean.class,meter,period)); }
    public boolean wasReplaced(String meter) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM energy_meter_replacements WHERE old_meter_id=?)",Boolean.class,meter)); }
    public Optional<Opening> opening(String meter) {
        return jdbc.query("SELECT * FROM energy_meter_openings WHERE meter_id=?",(r,n)->new Opening(r.getString("meter_id"),r.getBigDecimal("initial_value"),r.getTimestamp("initial_at").toInstant(),r.getString("recorded_by"),r.getString("note"),strings(r.getString("attachment_ids"))),meter).stream().findFirst();
    }
    public void saveOpening(Opening o) {
        jdbc.update("INSERT INTO energy_meter_openings(meter_id,initial_value,initial_at,recorded_by,note,attachment_ids) VALUES(?,?,?,?,?,?::jsonb)",o.meterId(),o.initialValue(),java.sql.Timestamp.from(o.initialAt()),o.recordedBy(),o.note(),encode(o.attachmentIds()));
    }
    public BigDecimal effectiveValue(EnergyMeterReadingRecord r) {
        return approvedVerification(r.id()).map(Verification::verifiedValue).orElse(r.readingValue());
    }
    public BigDecimal effectiveConsumption(EnergyMeterReadingRecord r) {
        return approvedVerification(r.id()).map(v->v.verifiedValue().subtract(v.verifiedOpeningValue())).orElse(r.consumption());
    }
    public Optional<Verification> approvedVerification(String readingId) {
        return verifications(readingId).stream().filter(v->"approved".equals(v.status())).findFirst();
    }
    public List<Verification> verifications(String readingId) {
        return jdbc.query("SELECT * FROM energy_reading_verifications WHERE reading_id=? ORDER BY verified_at,id",this::verification,readingId);
    }
    public void saveVerification(Verification v) {
        jdbc.update("INSERT INTO energy_reading_verifications(id,reading_id,project_id,merchant_id,verified_value,verified_opening_value,method,reason,attachment_ids,verified_by,review_id,status,verified_at) VALUES(?,?,?,?,?,?,?,?,?::jsonb,?,?,?,?)",
                v.id(),v.readingId(),v.projectId(),v.merchantId(),v.verifiedValue(),v.verifiedOpeningValue(),v.method(),v.reason(),encode(v.attachmentIds()),v.verifiedBy(),v.reviewId(),v.status(),java.sql.Timestamp.from(v.verifiedAt()));
    }
    public Optional<Verification> byReview(String reviewId) { return jdbc.query("SELECT * FROM energy_reading_verifications WHERE review_id=?",this::verification,reviewId).stream().findFirst(); }
    public void decideVerification(String id,String status) { jdbc.update("UPDATE energy_reading_verifications SET status=? WHERE id=? AND status='pending_review'",status,id); }
    public void saveReplacement(Replacement r) {
        jdbc.update("INSERT INTO energy_meter_replacements(id,old_meter_id,new_meter_id,period,old_final_value,new_initial_value,replaced_at,old_reading_id,note,attachment_ids,recorded_by) VALUES(?,?,?,?,?,?,?,?,?,?::jsonb,?)",r.id(),r.oldMeterId(),r.newMeterId(),r.period(),r.oldFinalValue(),r.newInitialValue(),java.sql.Timestamp.from(r.replacedAt()),r.oldReadingId(),r.note(),encode(r.attachmentIds()),r.recordedBy());
    }
    public List<Replacement> replacements(String meterId) {
        return jdbc.query("SELECT * FROM energy_meter_replacements WHERE old_meter_id=? OR new_meter_id=? ORDER BY replaced_at,id",(r,n)->new Replacement(r.getString("id"),r.getString("old_meter_id"),r.getString("new_meter_id"),r.getString("period"),r.getBigDecimal("old_final_value"),r.getBigDecimal("new_initial_value"),r.getTimestamp("replaced_at").toInstant(),r.getString("old_reading_id"),r.getString("note"),strings(r.getString("attachment_ids")),r.getString("recorded_by")),meterId,meterId);
    }
    public void saveCharge(Charge c) {
        jdbc.update("INSERT INTO energy_actual_charges(id,contract_id,meter_id,merchant_id,project_id,period,amount_cent,due_date,supplier_bill_reference,attachment_ids,reading_ids,consumption,recorded_by,created_at) VALUES(?,?,?,?,?,?,?,?,?,?::jsonb,?::jsonb,?,?,?)",c.id(),c.contractId(),c.meterId(),c.merchantId(),c.projectId(),c.period(),c.amountCent(),c.dueDate(),c.supplierBillReference(),encode(c.attachmentIds()),encode(c.readingIds()),c.consumption(),c.recordedBy(),java.sql.Timestamp.from(c.createdAt()));
    }
    public List<Charge> charges(String period) { return jdbc.query("SELECT * FROM energy_actual_charges WHERE period=? ORDER BY id",this::charge,period); }
    public void markBilled(List<Charge> charges,String arId) { for(Charge c:charges) jdbc.update("UPDATE energy_actual_charges SET ar_ap_item_id=?,settled_at=now() WHERE id=? AND settled_at IS NULL",arId,c.id()); }
    public boolean validContract(String contract,String merchant,String project,String period) {
        var month=java.time.YearMonth.parse(period);
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM contracts WHERE id=? AND merchant_id=? AND project_id=? AND lifecycle_status='effective' AND energy_fee_basis='by_contract' AND (effective_date IS NULL OR effective_date<=?) AND (expiry_date IS NULL OR expiry_date>=?))",Boolean.class,contract,merchant,project,month.atEndOfMonth(),month.atDay(1)));
    }
    public boolean hasUnreviewed(String merchant,String project,String period) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM energy_meter_readings r JOIN energy_meters m ON m.id=r.meter_id WHERE m.merchant_id=? AND m.project_id=? AND r.period=? AND r.status<>'reviewed')",Boolean.class,merchant,project,period));
    }
    public boolean hasUnchargedReadings(String merchant,String project,String period) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM energy_meter_readings r JOIN energy_meters m ON m.id=r.meter_id WHERE m.merchant_id=? AND m.project_id=? AND r.period=? AND NOT EXISTS(SELECT 1 FROM energy_actual_charges c WHERE c.meter_id=m.id AND c.period=r.period))",Boolean.class,merchant,project,period));
    }
    public Optional<Settings> settings(String projectId) { return jdbc.query("SELECT * FROM energy_review_settings WHERE project_id=? AND status='approved' ORDER BY created_at DESC,id DESC LIMIT 1",this::mapSettings,projectId).stream().findFirst(); }
    public List<Settings> settingsHistory(String projectId) { return jdbc.query("SELECT * FROM energy_review_settings WHERE project_id=? ORDER BY created_at DESC,id DESC",this::mapSettings,projectId); }
    public void saveSettings(Settings s) { jdbc.update("INSERT INTO energy_review_settings(id,project_id,days,min_days,max_days,supervisor_id,review_id,status,created_at) VALUES(?,?,?,?,?,?,?,?,?)",s.id(),s.projectId(),s.days(),s.minDays(),s.maxDays(),s.supervisorId(),s.reviewId(),s.status(),java.sql.Timestamp.from(s.createdAt())); }
    public Optional<Settings> settingsByReview(String id){return jdbc.query("SELECT * FROM energy_review_settings WHERE review_id=?",this::mapSettings,id).stream().findFirst();}
    public void decideSettings(String review,String status) { jdbc.update("UPDATE energy_review_settings SET status=? WHERE review_id=? AND status='pending_review'",status,review); }
    public void saveAssignment(Assignment a) { jdbc.update("INSERT INTO energy_verifier_assignments(reading_id,verifier_id,reason,assigned_by,assigned_at) VALUES(?,?,?,?,?)",a.readingId(),a.verifierId(),a.reason(),a.assignedBy(),java.sql.Timestamp.from(a.assignedAt())); }
    public List<Assignment> assignments(String reading) { return jdbc.query("SELECT * FROM energy_verifier_assignments WHERE reading_id=? ORDER BY assigned_at",(r,n)->new Assignment(r.getString("reading_id"),r.getString("verifier_id"),r.getString("reason"),r.getString("assigned_by"),r.getTimestamp("assigned_at").toInstant()),reading); }
    public boolean activeStaff(String user,String project) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM access_users WHERE id=? AND account_type='staff' AND organization_id=? AND status='active')",Boolean.class,user,project)); }
    public record ReadingEvidence(String readingId,List<String> attachmentIds,String note) { }
    public void saveReadingEvidence(String reading,List<String> ids,String note) { jdbc.update("INSERT INTO energy_reading_evidence(reading_id,attachment_ids,note) VALUES(?,?::jsonb,?)",reading,encode(ids),note); }
    public ReadingEvidence readingEvidence(String reading) { return jdbc.query("SELECT * FROM energy_reading_evidence WHERE reading_id=?",(r,n)->new ReadingEvidence(r.getString("reading_id"),strings(r.getString("attachment_ids")),r.getString("note")),reading).stream().findFirst().orElse(new ReadingEvidence(reading,List.of(),null)); }
    public Map<String,Object> escalation(String reading){return jdbc.queryForList("SELECT supervisor_id,deadline_at,escalated_at FROM energy_anomaly_escalations WHERE reading_id=?",reading).stream().findFirst().orElse(Map.of());}
    public int escalateOverdue(){return jdbc.update("""
        INSERT INTO energy_anomaly_escalations(reading_id,supervisor_id,deadline_at,escalated_at)
        SELECT r.id,c.supervisor_id,r.created_at+make_interval(days=>c.days),now()
        FROM energy_meter_readings r JOIN energy_meters m ON m.id=r.meter_id
        CROSS JOIN LATERAL (SELECT supervisor_id,days FROM energy_review_settings
            WHERE project_id=m.project_id AND status='approved' ORDER BY created_at DESC,id DESC LIMIT 1) c
        WHERE r.status='abnormal' AND now()>=r.created_at+make_interval(days=>c.days)
        ON CONFLICT (reading_id) DO NOTHING
        """);}
    public void lockMeterGraph(String project){jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,0))","energy-meter-graph:"+project);}
    public boolean validProject(String id){return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM projects WHERE id=?)",Boolean.class,id));}
    public boolean validMerchant(String merchant,String project){return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM merchants m WHERE m.id=? AND (EXISTS(SELECT 1 FROM contracts c WHERE c.merchant_id=m.id AND c.project_id=?) OR EXISTS(SELECT 1 FROM spaces s WHERE s.merchant_id=m.id AND s.project_id=?)))",Boolean.class,merchant,project,project));}
    public void transferChildren(String oldParent,String newParent,Instant at,String replacement){
        jdbc.update("INSERT INTO energy_meter_parent_transfers(meter_id,old_parent_id,new_parent_id,effective_at,replacement_id) SELECT id,parent_meter_id,?,?,? FROM energy_meters WHERE parent_meter_id=?",newParent,java.sql.Timestamp.from(at),replacement,oldParent);
        jdbc.update("UPDATE energy_meters SET parent_meter_id=?,updated_at=now() WHERE parent_meter_id=?",newParent,oldParent);
    }
    public boolean hasMissingReadings(String merchant,String project,String period){
        var ym=java.time.YearMonth.parse(period);
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM energy_meters m LEFT JOIN energy_meter_openings o ON o.meter_id=m.id
            WHERE m.gather_object='merchant' AND m.merchant_id=? AND m.project_id=?
              AND COALESCE(o.initial_at,m.created_at)<?
              AND (m.active OR o.meter_id IS NOT NULL OR EXISTS(SELECT 1 FROM energy_meter_readings old WHERE old.meter_id=m.id))
              AND NOT EXISTS(SELECT 1 FROM energy_meter_replacements x WHERE x.old_meter_id=m.id AND x.replaced_at<?)
              AND NOT EXISTS(SELECT 1 FROM energy_meter_retirements x WHERE x.meter_id=m.id AND x.retired_at<?)
              AND NOT EXISTS(SELECT 1 FROM energy_meter_readings r WHERE r.meter_id=m.id AND r.period=?))
            """,Boolean.class,merchant,project,java.sql.Timestamp.from(ym.plusMonths(1).atDay(1).atStartOfDay(com.complex.platform.common.LocalZone.ID).toInstant()),java.sql.Timestamp.from(ym.atDay(1).atStartOfDay(com.complex.platform.common.LocalZone.ID).toInstant()),java.sql.Timestamp.from(ym.atDay(1).atStartOfDay(com.complex.platform.common.LocalZone.ID).toInstant()),period));
    }
    public List<Map<String,Object>> pendingEscalations(String supervisor){return jdbc.queryForList("SELECT e.reading_id,e.supervisor_id,e.deadline_at,e.escalated_at,r.meter_id,r.period,m.project_id FROM energy_anomaly_escalations e JOIN energy_meter_readings r ON r.id=e.reading_id JOIN energy_meters m ON m.id=r.meter_id WHERE e.supervisor_id=? AND r.status='abnormal' ORDER BY e.deadline_at,e.reading_id",supervisor);}
    private Verification verification(ResultSet r,int n) throws SQLException { return new Verification(r.getString("id"),r.getString("reading_id"),r.getString("project_id"),r.getString("merchant_id"),r.getBigDecimal("verified_value"),r.getBigDecimal("verified_opening_value"),r.getString("method"),r.getString("reason"),strings(r.getString("attachment_ids")),r.getString("verified_by"),r.getString("review_id"),r.getString("status"),r.getTimestamp("verified_at").toInstant()); }
    private Charge charge(ResultSet r,int n) throws SQLException { return new Charge(r.getString("id"),r.getString("contract_id"),r.getString("meter_id"),r.getString("merchant_id"),r.getString("project_id"),r.getString("period"),r.getLong("amount_cent"),r.getDate("due_date").toLocalDate(),r.getString("supplier_bill_reference"),strings(r.getString("attachment_ids")),strings(r.getString("reading_ids")),r.getBigDecimal("consumption"),r.getString("recorded_by"),r.getTimestamp("created_at").toInstant(),r.getString("ar_ap_item_id"),r.getTimestamp("settled_at")==null?null:r.getTimestamp("settled_at").toInstant()); }
    private Settings mapSettings(ResultSet r,int n) throws SQLException { return new Settings(r.getString("id"),r.getString("project_id"),r.getInt("days"),r.getInt("min_days"),r.getInt("max_days"),r.getString("supervisor_id"),r.getString("review_id"),r.getString("status"),r.getTimestamp("created_at").toInstant()); }
    private String encode(Object o) { try { return json.writeValueAsString(o); } catch(Exception e) { throw new IllegalArgumentException(e); } }
    private List<String> strings(String s) { try { return s==null?List.of():json.readValue(s,new TypeReference<List<String>>(){}); } catch(Exception e) { throw ApiException.conflict("ENERGY_EVIDENCE_INVALID","能源证据记录无法读取"); } }
}
