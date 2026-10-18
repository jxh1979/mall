package com.complex.platform.ticketing;

// 文件版本：V0.01

import com.complex.platform.amusement.*;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** 游乐票务 JDBC 测试的真实项目、已批准营业日、场次与父票夹具。 */
final class AmusementTicketJdbcFixture {
    private AmusementTicketJdbcFixture() {}
    record Data(String key,String complex,String project,String session,String safety,
                String entitlement,String code,String parent,String order,String type,String template,
                String device,String point) {}

    static Data seed(JdbcTemplate jdbc,AmusementRepository repository,
            AmusementSessionLifecycleService lifecycle,AmusementBusinessDayService days,
            SupervisorReviewService reviews,int uses,String window,int capacity) {
        String k=UUID.randomUUID().toString().replace("-","").substring(0,14);
        String project="ajf_project_"+k,session="ajf_session_"+k,safety="ajf_safety_"+k;
        String device="ajf_device_"+k,point="ajf_point_"+k;
        AmusementBusinessDayTestFixture.master();
        jdbc.update("""
            INSERT INTO amusement_safety_templates(id,template_name,items_json,status,created_at,updated_at)
            VALUES(?,'营业日票务测试巡检','[{"name":"restraint","required":true}]'::jsonb,'active',now(),now())
            """,safety);
        jdbc.update("""
            INSERT INTO amusement_projects(id,project_name,capacity,open_status,safety_template_id,
              eligibility_rule_json,location_point_id,created_at,updated_at)
            VALUES(?,'营业日票务测试项目',?,'open',?,'{}'::jsonb,?,now(),now())
            """,project,capacity,safety,point);
        jdbc.update("INSERT INTO amusement_project_entitlements(id,project_id,entitlement_type,created_at) VALUES(?,?,'amusement_pass',now())","ajf_binding_"+k,project);
        jdbc.update("""
            INSERT INTO amusement_safety_inspections(id,project_id,template_id,items_json,overall_result,
              inspection_status,rectification_status,reopen_status,inspected_at,submitted_at,created_at,updated_at)
            VALUES(?,?,?,'[{"name":"restraint","result":"pass"}]'::jsonb,'pass','submitted',
              'not_required','not_required',now(),now(),now(),now())
            ""","ajf_inspect_"+k,project,safety);
        String complex=AmusementBusinessDayTestFixture.complex(jdbc);
        AmusementBusinessDayTestFixture.bind(jdbc,project,complex);
        AmusementBusinessDayTestFixture.approve(jdbc,days,reviews,complex,"Asia/Shanghai",10,Instant.now().minusSeconds(14*86400));
        jdbc.update("""
            INSERT INTO amusement_sessions(id,project_id,start_time,end_time,capacity,used_capacity,status,created_at,updated_at)
            VALUES(?,?,now()-interval '1 hour',now()+interval '1 hour',?,0,'open',now(),now())
            """,session,project,capacity);
        lifecycle.lockSessionWindow(repository.findSessionById(session).orElseThrow());
        jdbc.update("""
            INSERT INTO terminal_devices(id,device_id,device_code,terminal_type,binding_scope,status,created_at,updated_at)
            VALUES(?,?,?,'kiosk','venue','bound',now(),now())
            """,device,"physical_"+k,"terminal_"+k);
        Data base=new Data(k,complex,project,session,safety,null,null,null,null,null,null,device,point);
        Data ticket=anotherTicket(jdbc,base,uses,window);
        String actor=AmusementBusinessDayTestFixture.actor(jdbc,complex,List.of("amusement.verification.execute","verification.execute","ticket.verify.reverse","terminal.device.bind"));
        jdbc.update("UPDATE access_users SET account_type='platform_admin' WHERE id=?",actor);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                new com.complex.platform.access.AccessActor(actor,actor,"platform_admin",complex,null,"active"),
                "fixture",List.of()));
        return ticket;
    }

    static Data anotherTicket(JdbcTemplate jdbc,Data source,int uses,String window) {
        String k=UUID.randomUUID().toString().replace("-","").substring(0,14);
        String entitlement="ajf_ent_"+k,code="ajf_code_"+k,parent="ajf_parent_"+k,
                order="ajf_order_"+k,type="ajf_type_"+k,template="ajf_template_"+k;
        jdbc.update("INSERT INTO ticket_types(id,ticket_name,base_calendar_price_mode,status,created_at,updated_at) VALUES(?,'营业日票务测试票','fixed','active',now(),now())",type);
        jdbc.update("""
            INSERT INTO entitlement_templates(id,name,entitlement_type,default_total_times,default_value_cent,status,created_at,updated_at)
            VALUES(?,'营业日票务测试权益','amusement_pass',?,5500,'active',now(),now())
            """,template,uses);
        jdbc.update("""
            INSERT INTO ticket_orders(id,order_no,ticket_type_id,project_id,quantity,sales_channel,amount_cent,
              pay_status,issue_status,status,created_at,updated_at)
            VALUES(?,?,?,?,1,'web',0,'paid','issued','created',now(),now())
            """,order,"AJF_ORDER_"+k,type,source.complex());
        jdbc.update("""
            INSERT INTO ticket_instances(id,ticket_no,order_id,ticket_type_id,seq_in_order,num_people,total_times,
              status,created_at,updated_at) VALUES(?,?,?,?,1,1,?,'active',now(),now())
            """,parent,"AJF_PARENT_"+k,order,type,uses);
        jdbc.update("""
            INSERT INTO issued_entitlements(id,ticket_type_id,order_id,entitlement_template_id,entitlement_code,
              entitlement_type,total_times,used_times,value_amount_cent,allocated_revenue_cent,status,
              consumption_window,valid_from,valid_to,parent_ticket_id,slot_index,created_at,updated_at)
            VALUES(?,?,?,?,?,'amusement_pass',?,0,5500,0,'active',?,now()-interval '7 days',
              now()+interval '7 days',?,1,now(),now())
            """,entitlement,type,order,template,code,uses,window,parent);
        return new Data(source.key(),source.complex(),source.project(),source.session(),source.safety(),
                entitlement,code,parent,order,type,template,source.device(),source.point());
    }

    static void cleanupTicket(JdbcTemplate jdbc,Data f) {
        jdbc.update("DELETE FROM entitlement_business_day_activations WHERE entitlement_id=?",f.entitlement());
        com.complex.platform.support.AppendOnlyTestBypass.update(jdbc,"DELETE FROM settlement_transactions WHERE entitlement_id=?",f.entitlement());
        jdbc.update("DELETE FROM ticket_verifications WHERE entitlement_id=?",f.entitlement());
        jdbc.update("DELETE FROM amusement_verifications WHERE entitlement_id=?",f.entitlement());
        jdbc.update("DELETE FROM issued_entitlements WHERE id=?",f.entitlement());
        jdbc.update("DELETE FROM ticket_instances WHERE id=?",f.parent());
        jdbc.update("DELETE FROM ticket_orders WHERE id=?",f.order());
        jdbc.update("DELETE FROM entitlement_templates WHERE id=?",f.template());
        jdbc.update("DELETE FROM ticket_types WHERE id=?",f.type());
    }

    static void cleanupProject(JdbcTemplate jdbc,Data f) {
        jdbc.update("DELETE FROM amusement_projects WHERE id=?",f.project());
        jdbc.update("DELETE FROM amusement_safety_templates WHERE id=?",f.safety());
        jdbc.update("DELETE FROM terminal_devices WHERE id=?",f.device());
        // 日切审批及其综合体、空间和审批身份是测试原件，保留；不删除或改写已批准版本。
    }
}
