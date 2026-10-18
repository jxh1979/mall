package com.complex.platform.access.staff;

// 文件版本：V0.01

import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.complex.platform.common.api.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** 只枚举真实未结责任，历史经办和收益主体不参与交接。 */
@Repository
public class StaffHandoverRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper json;

    public StaffHandoverRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    enum Kind {
        approval_node("审批节点", "supervisor_review_nodes", "node_id", "COALESCE(s.forwarded_to,s.approver_id)",
                "s.status='pending' AND (s.approver_kind='user' OR s.forwarded_to IS NOT NULL) AND EXISTS (SELECT 1 FROM supervisor_review_decisions d WHERE d.review_id=s.review_id AND d.review_status='pending_review')",
                "governance.supervisor_review.approve", "governance.supervisor_review.approve"),
        facility_work_order("维修工单", "facility_work_orders", "id", "s.assignee_id", "s.status<>'closed'",
                "facility.work_order.manage", "facility.work_order.manage"),
        facility_inspection("巡检任务", "facility_inspection_tasks", "id", "s.assignee_id", "s.status<>'closed'",
                "facility.inspection.manage", "facility.inspection.manage"),
        merchant_work_order("商户工单", "merchant_service_work_orders", "id", "s.owner_user_id", "s.status NOT IN ('closed','cancelled')",
                "merchant.service_work_order.assign", "merchant.service_work_order.process"),
        service_order("服务订单", "service_orders", "id", "s.performer_user_id", "s.status NOT IN ('completed','cancelled')",
                "service.order.create", "service.order.create"),
        leasing_lead("招商线索", "leasing_leads", "id", "s.owner_user_id", "s.status<>'signed'",
                "leasing.lead.manage", "leasing.lead.manage"),
        integration_difference("渠道对账差异", "integration_reconciliation_differences", "id", "s.assigned_to_user_id", "s.status IN ('open','difference_found','needs_review')",
                "integration.reconciliation.resolve", "integration.reconciliation.resolve"),
        bar_table_session("卡座服务", "bar_table_sessions", "id", "s.service_staff_id", "s.status='open'",
                "bar.table_session.transfer", "bar.order.create"),
        risk_alert("异常处理", "risk_alerts", "alert_id", "CASE WHEN s.status='escalated' THEN s.escalated_to ELSE s.first_handler_id END", "s.status IN ('open','handling','escalated')",
                "risk_alert.transfer", "risk_alert.resolve");

        final String label, table, idColumn, owner, unfinished, managePermission, workPermission;
        Kind(String label, String table, String idColumn, String owner, String unfinished, String managePermission, String workPermission) {
            this.label=label; this.table=table; this.idColumn=idColumn; this.owner=owner; this.unfinished=unfinished;
            this.managePermission=managePermission; this.workPermission=workPermission;
        }
        static Kind parse(String value) {
            try { return valueOf(value == null ? "" : value); }
            catch (IllegalArgumentException ex) { throw ApiException.badRequest("HANDOVER_TYPE_INVALID", "无法识别的业务交接类型"); }
        }
    }

    record Source(Kind kind, String id, String owner, String version, JsonNode data) {}

    List<Source> pending(String userId) {
        List<Source> rows = new ArrayList<>();
        for (Kind kind : Kind.values()) rows.addAll(find(kind, userId, null, false));
        return rows;
    }

    Optional<Source> lock(Kind kind, String userId, String id) {
        return find(kind, userId, id, true).stream().findFirst();
    }

    private List<Source> find(Kind kind, String userId, String id, boolean lock) {
        String sql = "SELECT s."+kind.idColumn+" AS id,"+kind.owner+" AS owner,s.xmin::text AS version,to_jsonb(s)::text AS data FROM "+kind.table
                +" s WHERE "+kind.owner+"=:user_id AND "+kind.unfinished+(id == null ? "" : " AND s."+kind.idColumn+"=:id")
                +" ORDER BY s."+kind.idColumn+(lock ? " FOR UPDATE OF s" : "");
        return jdbc.query(sql, new MapSqlParameterSource().addValue("user_id",userId).addValue("id",id),
                (rs,i) -> new Source(kind,rs.getString("id"),rs.getString("owner"),rs.getString("version"),decode(rs.getString("data"))));
    }

    JsonNode review(String reviewId) {
        return jdbc.query("SELECT to_jsonb(d)::text AS data FROM supervisor_review_decisions d WHERE review_id=:id",
                Map.of("id",reviewId), (rs,i) -> decode(rs.getString("data"))).stream().findFirst()
                .orElseThrow(() -> ApiException.conflict("HANDOVER_SOURCE_CHANGED", "原复核单已不存在"));
    }

    record Scope(String projectId, String merchantId, boolean global, boolean resolved) {}

    Scope scope(Source source) {
        JsonNode row=source.data();
        String project=text(row,"project_id"), merchant=text(row,"merchant_id");
        if (source.kind()==Kind.approval_node) {
            JsonNode review=review(text(row,"review_id"));
            String subject="hr_three_levels_up".equals(text(review,"approval_level")) ? text(review,"beneficiary_id") : text(review,"initiated_by");
            return userScope(subject);
        }
        if (source.kind()==Kind.integration_difference) {
            var merchants=jdbc.queryForList("""
                    SELECT DISTINCT merchant_id FROM integration_order_syncs
                    WHERE provider_code=:provider AND business_type=:type AND external_business_id=:external AND merchant_id IS NOT NULL
                    """,new MapSqlParameterSource().addValue("provider",text(row,"provider_code")).addValue("type",text(row,"business_type"))
                    .addValue("external",text(row,"external_business_id")),String.class);
            if (merchants.size()>1) return new Scope(null,null,false,false);
            if (merchants.size()==1) return merchantScope(merchants.get(0),null);
            String registered=text(row,"registered_by_user_id");
            if (registered!=null) return userScope(registered);
            // 只有来源明确的平台系统汇总可使用全平台范围，第三方来源缺失不能推定为全平台。
            boolean platformSystem="platform".equals(text(row,"source_domain")) && "system".equals(text(row,"amount_source"));
            return new Scope(null,null,platformSystem,platformSystem);
        }
        if (source.kind()==Kind.merchant_work_order) {
            List<String> candidates=jdbc.queryForList("""
                    SELECT project_id FROM spaces WHERE id=:space AND project_id IS NOT NULL
                    UNION SELECT project_id FROM contracts WHERE id=:contract AND project_id IS NOT NULL
                    """,new MapSqlParameterSource().addValue("space",text(row,"space_id")).addValue("contract",text(row,"contract_id")),String.class);
            if(candidates.size()>1) return new Scope(null,merchant,false,false);
            project=candidates.isEmpty()?null:candidates.get(0);
        }
        if (source.kind()==Kind.service_order) {
            var merchants=jdbc.queryForList("SELECT DISTINCT merchant_id FROM bar_tables WHERE id=:table AND merchant_id IS NOT NULL",
                    new MapSqlParameterSource().addValue("table",text(row,"table_id")),String.class);
            merchant=merchants.isEmpty()?null:merchants.get(0);
        }
        if (source.kind()==Kind.facility_inspection) {
            var merchants=jdbc.queryForList("SELECT merchant_id FROM spaces WHERE id=:space AND merchant_id IS NOT NULL",
                    new MapSqlParameterSource().addValue("space",text(row,"space_id")),String.class);
            merchant=merchants.isEmpty()?null:merchants.get(0);
        }
        if(source.kind()==Kind.service_order || source.kind()==Kind.bar_table_session) {
            // V267：酒吧即商户，bar_id 与 merchant_id 必须指向同一实体，不查询已删除的 bars 表。
            String barMerchant=text(row,"bar_id");
            if(merchant!=null && barMerchant!=null && !merchant.equals(barMerchant)) return new Scope(null,merchant,false,false);
            if(merchant==null) merchant=barMerchant;
        }
        if (merchant!=null && !"__platform__".equals(merchant)) return merchantScope(merchant,project);
        if (project!=null) return new Scope(project,null,false,true);
        if (source.kind()==Kind.risk_alert) {
            Scope reporter=userScope(text(row,"first_reported_by"));
            if(reporter.global()) return reporter;
        }
        return new Scope(null,merchant,false,false);
    }

    private Scope merchantScope(String merchant, String explicitProject) {
        var projects=jdbc.queryForList("""
                SELECT project_id FROM merchants WHERE id=:id AND project_id IS NOT NULL
                UNION SELECT project_id FROM merchant_projects WHERE merchant_id=:id
                """,Map.of("id",merchant),String.class);
        if (explicitProject!=null) return new Scope(explicitProject,merchant,false,projects.contains(explicitProject));
        return projects.size()==1 ? new Scope(projects.get(0),merchant,false,true) : new Scope(null,merchant,false,false);
    }

    private Scope userScope(String userId) {
        if(userId==null) return new Scope(null,null,false,false);
        return jdbc.query("SELECT organization_id,merchant_id,account_type FROM access_users WHERE id=:id",Map.of("id",userId),(rs,i)->{
            String project=rs.getString("organization_id"),merchant=rs.getString("merchant_id"),type=rs.getString("account_type");
            boolean global=merchant==null && ("platform_admin".equals(type)||"master_authority".equals(type));
            return new Scope(global?null:project,merchant,global,global||project!=null);
        }).stream().findFirst().orElse(new Scope(null,null,false,false));
    }

    void transfer(Source source, String target, String actor, Instant at) {
        Kind kind=source.kind();
        String ownerColumn=switch(kind){
            case approval_node -> "forwarded_to";
            case facility_work_order,facility_inspection -> "assignee_id";
            case merchant_work_order,leasing_lead -> "owner_user_id";
            case service_order -> "performer_user_id";
            case integration_difference -> "assigned_to_user_id";
            case bar_table_session -> "service_staff_id";
            case risk_alert -> "escalated".equals(text(source.data(),"status"))?"escalated_to":"first_handler_id";
        };
        String extra=kind==Kind.approval_node ? ",forwarded_by=:actor" : ",updated_at=:at";
        if(kind==Kind.bar_table_session) extra+=",session_version=session_version+1";
        var params=new MapSqlParameterSource().addValue("target",target).addValue("actor",actor).addValue("at",timestampWithTimeZone(at))
                .addValue("id",source.id()).addValue("user_id",source.owner()).addValue("version",source.version());
        int changed=jdbc.update("UPDATE "+kind.table+" s SET "+ownerColumn+"=:target"+extra+" WHERE s."+kind.idColumn
                +"=:id AND "+kind.owner+"=:user_id AND s.xmin::text=:version AND "+kind.unfinished,params);
        if(changed!=1) throw ApiException.conflict("HANDOVER_SOURCE_CHANGED", "原业务已结束或责任已变化，请刷新后重试");
        if(kind==Kind.approval_node && jdbc.update("UPDATE supervisor_review_decisions SET forwarded_to=:target WHERE review_id=:review AND review_status='pending_review'",
                Map.of("target",target,"review",text(source.data(),"review_id")))!=1)
            throw ApiException.conflict("HANDOVER_SOURCE_CHANGED", "原审批已结束，请刷新后重试");
    }

    public record Transfer(String id,String businessType,String businessId,String previousUserId,String targetUserId,
            String reason,String assignedBy,Instant assignedAt,Instant responsibilityStartedAt,String projectId,String merchantId) {}

    List<Transfer> history(String profileId) {
        return jdbc.query("SELECT * FROM staff_business_handovers WHERE profile_id=:id ORDER BY assigned_at DESC,id",Map.of("id",profileId),
                (rs,i)->new Transfer(rs.getString("id"),rs.getString("business_type"),rs.getString("business_id"),rs.getString("previous_user_id"),
                        rs.getString("target_user_id"),rs.getString("reason"),rs.getString("assigned_by"),rs.getTimestamp("assigned_at").toInstant(),
                        rs.getTimestamp("responsibility_started_at")==null?null:rs.getTimestamp("responsibility_started_at").toInstant(),rs.getString("project_id"),rs.getString("merchant_id")));
    }

    void save(String profileId,String reviewId,Scope scope,Source source,Transfer transfer) {
        var snapshot=json.createObjectNode().put("status",text(source.data(),"status")).put("row_version",source.version())
                .put("previous_user_id",source.owner());
        if(source.kind()==Kind.bar_table_session) snapshot.put("table_id",text(source.data(),"table_id"))
                .put("sales_staff_id",text(source.data(),"sales_staff_id"));
        jdbc.update("""
                INSERT INTO staff_business_handovers(id,profile_id,review_id,business_type,business_id,previous_user_id,target_user_id,
                    project_id,merchant_id,reason,assigned_by,assigned_at,source_state,responsibility_started_at)
                VALUES(:id,:profile,:review,:type,:business,:previous,:target,:project,:merchant,:reason,:actor,:at,CAST(:state AS jsonb),:start)
                """,new MapSqlParameterSource().addValue("id",transfer.id()).addValue("profile",profileId).addValue("review",reviewId)
                .addValue("type",source.kind().name()).addValue("business",source.id()).addValue("previous",source.owner()).addValue("target",transfer.targetUserId())
                .addValue("project",scope.projectId()).addValue("merchant",scope.merchantId()).addValue("reason",transfer.reason())
                .addValue("actor",transfer.assignedBy()).addValue("at",timestampWithTimeZone(transfer.assignedAt())).addValue("state",snapshot.toString())
                .addValue("start",timestampWithTimeZone(transfer.responsibilityStartedAt()),java.sql.Types.TIMESTAMP_WITH_TIMEZONE));
    }

    Instant responsibilityStart(Source source) {
        if(source.kind()!=Kind.bar_table_session) return null;
        var starts=jdbc.query("SELECT max(assigned_at) AS start FROM staff_business_handovers WHERE business_type='bar_table_session' AND business_id=:id",
                Map.of("id",source.id()),(rs,i)->rs.getTimestamp("start")==null?null:rs.getTimestamp("start").toInstant());
        return starts.get(0)==null ? Instant.parse(text(source.data(),"opened_at")) : starts.get(0);
    }

    String effectiveStopReview(String profileId,Instant at) {
        return jdbc.queryForList("""
                SELECT r.review_id FROM staff_employment_status_revisions r JOIN supervisor_review_decisions d ON d.review_id=r.review_id
                WHERE r.profile_id=:id AND r.after_status IN ('suspended','departed') AND d.review_status='approved' AND r.effective_at<=:at
                ORDER BY r.effective_at DESC,r.created_at DESC LIMIT 1
                """,new MapSqlParameterSource().addValue("id",profileId).addValue("at",timestampWithTimeZone(at)),String.class).stream().findFirst()
                .orElseThrow(()->ApiException.conflict("STAFF_STOP_NOT_EFFECTIVE","停岗或离职尚未批准生效，不能办理本项交接"));
    }

    public record Revision(String reviewId,String beforeStatus,String afterStatus,String reason,String reviewStatus,Instant effectiveAt,Instant appliedAt) {}
    List<Revision> revisions(String profileId) {
        return jdbc.query("""
                SELECT r.*,d.review_status FROM staff_employment_status_revisions r JOIN supervisor_review_decisions d ON d.review_id=r.review_id
                WHERE r.profile_id=:id ORDER BY r.created_at DESC,r.review_id
                """,Map.of("id",profileId),(rs,i)->new Revision(rs.getString("review_id"),rs.getString("before_status"),rs.getString("after_status"),
                rs.getString("reason"),rs.getString("review_status"),rs.getTimestamp("effective_at").toInstant(),
                rs.getTimestamp("applied_at")==null?null:rs.getTimestamp("applied_at").toInstant()));
    }

    private JsonNode decode(String value) {
        try{return json.readTree(value);}catch(java.io.IOException ex){throw new IllegalStateException("业务责任记录读取失败",ex);}
    }
    static String text(JsonNode row,String key){return row.hasNonNull(key)&&!row.get(key).asText().isBlank()?row.get(key).asText():null;}
}
