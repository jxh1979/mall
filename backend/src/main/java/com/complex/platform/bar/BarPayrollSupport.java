package com.complex.platform.bar;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;
import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.staff.JdbcStaffRepository;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.governance.PlatformRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Bar 原件共用的范围和持久化校验；不对外提供任意员工收入读取。 */
@Component
class BarPayrollSupport {
    static final String VIEW="bar.payroll.view", MANAGE="bar.payroll.manage";
    final JdbcTemplate jdbc;
    final AuthorizationService auth;
    final JdbcStaffRepository staff;
    final AccessRepository access;
    final PlatformRepository platform;
    final ObjectMapper json;
    BarPayrollSupport(JdbcTemplate jdbc,AuthorizationService auth,JdbcStaffRepository staff,
            AccessRepository access,PlatformRepository platform,ObjectMapper json) {
        this.jdbc=jdbc;this.auth=auth;this.staff=staff;this.access=access;this.platform=platform;this.json=json;
    }
    record Scope(String project,String merchant) {}
    Scope tableScope(String tableId) {
        return jdbc.query("SELECT s.project_id,t.merchant_id FROM bar_tables t JOIN spaces s ON s.id=t.space_id WHERE t.id=?",
            (rs,n)->new Scope(rs.getString(1),rs.getString(2)),tableId).stream().findFirst()
            .filter(s->s.project()!=null && s.merchant()!=null)
            .orElseThrow(()->conflict("BAR_PAYROLL_SCOPE_REQUIRED","台位须关联真实综合体铺位及商户后才能登记服务收入"));
    }
    void requireScope(String project,String merchant,String permission) {
        auth.requirePermission(permission);
        var actor=auth.currentActor();
        if(!auth.canReachProject(actor,project,permission)
                || (actor.merchantId()!=null && !Objects.equals(actor.merchantId(),merchant)))
            throw ApiException.forbidden("BAR_PAYROLL_SCOPE_MISMATCH","无权处理该综合体和商户的服务收入");
    }
    boolean inScope(String project,String merchant,String permission) {
        var actor=auth.currentActor();
        return auth.canReachProject(actor,project,permission)
            && (actor.merchantId()==null || Objects.equals(actor.merchantId(),merchant));
    }
    String operationCompany(String project,String merchant) {
        var m=platform.findMerchantById(requireText(merchant,"merchant_id"))
            .orElseThrow(()->invalid("商户不存在"));
        if(!Objects.equals(project,m.projectId()))throw invalid("商户须先确认所属综合体");
        String company=platform.findActiveBusinessEntityBindingByMerchantId(merchant)
            .map(b->b.operationEntityId()).orElse(m.businessEntityId());
        requireCompanyProject(company,project);
        return company;
    }
    String settlementCompany(String merchant) {
        var m=platform.findMerchantById(merchant).orElseThrow(()->invalid("商户不存在"));
        return requireText(platform.findActiveBusinessEntityBindingByMerchantId(merchant)
            .map(b->b.settlementEntityId()).orElse(m.settlementEntityId()),"settlement_entity_id");
    }
    void requireCompanyProject(String company,String project) {
        if(company==null || jdbc.queryForObject("SELECT count(*) FROM business_entity_projects p JOIN business_entities e ON e.id=p.business_entity_id WHERE p.business_entity_id=? AND p.project_id=? AND e.status='active'",
                Integer.class,company,project)!=1)throw invalid("经营公司须具有该综合体的有效经营主体关联");
    }
    String requireWorker(String user,String project,String merchant) {
        var p=staff.findProfileByUserId(requireText(user,"user_id")).orElseThrow(()->invalid("服务人员须有员工档案"));
        var u=access.findUserById(user).orElseThrow(()->invalid("服务人员账号不存在"));
        var subject=new AccessActor(u.id(),u.username(),u.accountType(),u.organizationId(),u.merchantId(),u.status());
        if(!"active".equals(u.status()) || !"active".equals(p.status()) || !"approved".equals(p.registrationStatus())
                || p.archivedAt()!=null || !List.of("employed","trial").contains(p.employmentStatus())
                || access.isEmploymentBusinessBlocked(user,Instant.now())
                || !auth.canReachProject(subject,project,"bar.order.create")
                || (p.merchantId()!=null && !Objects.equals(p.merchantId(),merchant))
                || (u.merchantId()!=null && !Objects.equals(u.merchantId(),merchant)))
            throw conflict("BAR_SERVICE_WORKER_INVALID","服务人员须在岗且具有本综合体及商户的点单工作权限");
        return requireText(p.companyId(),"payer_company_id");
    }
    String encode(Object value) {try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException(e);}}
    <T>T decode(String text,Class<T> type) {try{return json.readValue(text,type);}catch(Exception e){throw new IllegalStateException(e);}}
    List<String> strings(String text) {try{return json.readValue(text,new TypeReference<List<String>>(){});}catch(Exception e){throw new IllegalStateException(e);}}
    void lock(String key) {jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",rs->{},"bar-payroll:"+key);}
    static String id(String prefix) {return prefix+"_"+UUID.randomUUID().toString().replace("-","");}
    static String eventKey(String value) {return "bar_event_"+UUID.nameUUIDFromBytes(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    static Timestamp ts(Instant value) {return value==null?null:Timestamp.from(value);}
    static YearMonth month(String value) {try{if(value==null || !value.matches("\\d{4}-\\d{2}"))throw new IllegalArgumentException();return YearMonth.parse(value);}catch(RuntimeException e){throw invalid("月份须为 YYYY-MM");}}
    static ApiException invalid(String message) {return ApiException.badRequest("BAR_PAYROLL_INVALID",message);}
    static ApiException conflict(String code,String message) {return ApiException.conflict(code,message);}
}
