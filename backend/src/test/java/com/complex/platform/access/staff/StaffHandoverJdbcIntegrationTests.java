package com.complex.platform.access.staff;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.common.api.ApiException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties={"spring.datasource.url=${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=${COMPLEX_DATABASE_USERNAME:complex}","spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"})
@Transactional
class StaffHandoverJdbcIntegrationTests {
    @Autowired StaffHandoverService service;
    @Autowired StaffHandoverRepository repository;
    @Autowired AccessRepository access;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.complex.platform.serviceorder.ServiceOrderService services;
    private String suffix,oldUser,target,manager,profile,merchant,table,bar,stop;

    @BeforeEach void setup(){
        suffix=UUID.randomUUID().toString().substring(0,8);
        oldUser="old_"+suffix;target="new_"+suffix;manager="mgr_"+suffix;profile="profile_"+suffix;
        merchant="mer_"+suffix;table="table_"+suffix;bar=merchant;stop="stop_"+suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES('project_main','complex','交接测试项目','active',now(),now()) ON CONFLICT(id) DO NOTHING");
        for(String id:List.of(oldUser,target,manager)) StaffTestActors.grant(jdbc,access,id,"project_main");
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT 'role_m010_fixture',permission_code FROM permissions WHERE permission_code IN ('facility.work_order.manage','facility.inspection.manage','merchant.service_work_order.assign','merchant.service_work_order.process','merchant.service_work_order.close','service.order.create','service.order.perform','leasing.lead.manage','integration.reconciliation.resolve','bar.table_session.transfer','bar.order.create','risk_alert.transfer','risk_alert.resolve') ON CONFLICT DO NOTHING");
        profile(oldUser,profile,"departed");profile(target,"target_"+suffix,"employed");
        review(stop,"approved",manager);
        jdbc.update("INSERT INTO staff_employment_status_revisions(review_id,profile_id,user_id,before_status,after_status,reason,initiated_by,effective_at,applied_at) VALUES(?,?,?,'employed','departed','已批准生效',?,now()-interval '1 hour',now())",stop,profile,oldUser,manager);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,'交接门店','bar','active','project_main',now(),now())",merchant);
        // 台位须挂真实铺位（spaces.project_id）与商户：酒吧服务责任登记按 BarPayrollSupport.tableScope 取范围。
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at) VALUES(?,'project_main',?,'交接卡座区域','unit','active',now(),now())","space_"+suffix,merchant);
        jdbc.update("INSERT INTO bar_tables(id,bar_id,merchant_id,table_code,table_name,table_type,status,created_at,updated_at,space_id) VALUES(?,?,?,'T1','交接桌台','booth','available',now(),now(),?)",table,bar,merchant,"space_"+suffix);
        act(manager);
    }
    @AfterEach void cleanup(){SecurityContextHolder.clearContext();}

    @Test void nineRealResponsibilityTypesTransferWithoutChangingTheirBusinessStates(){
        seedAll();
        var before=service.overview(profile);
        assertThat(before.businessAccessStopped()).isTrue();
        assertThat(before.pending()).hasSize(9);
        for(var pending:before.pending()) {
            var result=service.assign(profile,request(pending.businessType(),pending.businessId(),target));
            assertThat(result.previousUserId()).isEqualTo(oldUser);
            assertThat(result.targetUserId()).isEqualTo(target);
            var source=repository.pending(target).stream().filter(s->s.id().equals(pending.businessId())).findFirst().orElseThrow();
            assertThat(StaffHandoverRepository.text(source.data(),"status")).isEqualTo(pending.status());
        }
        assertThat(service.overview(profile).pending()).isEmpty();
        assertThat(service.overview(profile).history()).hasSize(9);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='staff.business_handover.viewed' AND business_id=?",Integer.class,profile)).isGreaterThan(0);
        assertThat(jdbc.queryForObject("SELECT sales_staff_id FROM bar_table_sessions WHERE id=?",String.class,"session_"+suffix)).isEqualTo(oldUser);
        assertThat(jdbc.queryForObject("SELECT ordered_by FROM service_orders WHERE id=?",String.class,"svc_"+suffix)).isEqualTo(oldUser);
        assertThat(jdbc.queryForObject("SELECT approver_id FROM supervisor_review_nodes WHERE node_id=?",String.class,"node_"+suffix)).isEqualTo(oldUser);
        assertThat(jdbc.queryForObject("SELECT responsibility_started_at = (SELECT opened_at FROM bar_table_sessions WHERE id=?) FROM staff_business_handovers WHERE business_id=?",Boolean.class,"session_"+suffix,"session_"+suffix)).isTrue();
    }

    @Test void serviceOrderCanReallyCompleteAfterHandoverAndOnlyNewPerformerReceivesThisUnfinishedWorkCommission(){
        seedService("svc_"+suffix,"paid");
        service.assign(profile,request("service_order","svc_"+suffix,target));
        act(target);
        var completed=services.complete("svc_"+suffix,new com.complex.platform.serviceorder.dto.ServiceOrderActionRequest(key()));
        assertThat(completed.status()).isEqualTo("completed");
        assertThat(completed.performerUserId()).isEqualTo(target);
        assertThat(jdbc.queryForObject("SELECT user_id FROM staff_commission_ledger WHERE source_id=? AND source_type='service_order'",String.class,"svc_"+suffix)).isEqualTo(target);
        assertThat(jdbc.queryForObject("SELECT ordered_by FROM service_orders WHERE id=?",String.class,"svc_"+suffix)).isEqualTo(oldUser);
    }

    @Test void duplicateKeyReplaysAndDifferentKeyCannotTransferAlreadyTakenWork(){
        seedRepair("repair_"+suffix,"assigned","project_main");
        var request=request("facility_work_order","repair_"+suffix,target);
        var first=service.assign(profile,request);
        assertThat(service.assign(profile,request)).isEqualTo(first);
        assertThatThrownBy(()->service.assign(profile,request("facility_work_order","repair_"+suffix,target)))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("HANDOVER_SOURCE_CHANGED"));
    }

    @Test void completedRecordsAreNeverTransferred(){
        seedRepair("repair_"+suffix,"closed","project_main");seedService("svc_"+suffix,"completed");
        assertThat(service.overview(profile).pending()).isEmpty();
        assertThatThrownBy(()->service.assign(profile,request("service_order","svc_"+suffix,target))).isInstanceOf(ApiException.class);
        assertThat(jdbc.queryForObject("SELECT performer_user_id FROM service_orders WHERE id=?",String.class,"svc_"+suffix)).isEqualTo(oldUser);
    }

    @Test void unavailableRecipientNeverKeepsTheOldEmployeeAuthorized(){
        seedRepair("repair_"+suffix,"processing","project_main");
        jdbc.update("UPDATE staff_profiles SET employment_status='suspended' WHERE user_id=?",target);
        assertThatThrownBy(()->service.assign(profile,request("facility_work_order","repair_"+suffix,target)))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("HANDOVER_TARGET_NOT_ACTIVE"));
        assertThat(access.isEmploymentBusinessBlocked(oldUser,java.time.Instant.now())).isTrue();
        assertThat(jdbc.queryForObject("SELECT assignee_id FROM facility_work_orders WHERE id=?",String.class,"repair_"+suffix)).isEqualTo(oldUser);
    }

    @Test void crossProjectAndMissingExecutionPermissionAreRejected(){
        seedRepair("repair_"+suffix,"processing","project_main");
        jdbc.update("UPDATE access_users SET organization_id='outside_project' WHERE id=?",target);
        assertThatThrownBy(()->service.assign(profile,request("facility_work_order","repair_"+suffix,target)))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("HANDOVER_TARGET_SCOPE_INVALID"));
        jdbc.update("UPDATE access_users SET organization_id='project_main' WHERE id=?",target);
        access.saveUserPermissionOverrides(target,List.of("facility.work_order.manage"),AccessRepository.Effect.DENY,"不可执行",manager);
        assertThatThrownBy(()->service.assign(profile,request("facility_work_order","repair_"+suffix,target)))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("HANDOVER_TARGET_PERMISSION_MISSING"));
    }

    @Test void sourceWithUnknownProjectIsVisibleAsHeldAndCannotBeAssigned(){
        seedRepair("repair_"+suffix,"processing",null);
        assertThat(service.overview(profile).pending()).singleElement().satisfies(p->{assertThat(p.assignable()).isFalse();assertThat(p.projectId()).isNull();});
        assertThatThrownBy(()->service.assign(profile,request("facility_work_order","repair_"+suffix,target)))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("HANDOVER_SCOPE_UNRESOLVED"));
    }

    @Test void laterPendingApprovalNodeCanTransferButBeneficiaryCannotReceiveIt(){
        review("review_"+suffix,"pending_review",manager);
        jdbc.update("UPDATE supervisor_review_decisions SET beneficiary_id=? WHERE review_id=?",target,"review_"+suffix);
        node("first_"+suffix,"review_"+suffix,1,"user",manager,null);
        node("later_"+suffix,"review_"+suffix,2,"user",oldUser,null);
        assertThat(service.overview(profile).pending()).extracting(StaffHandoverService.Pending::businessId).contains("later_"+suffix);
        assertThatThrownBy(()->service.assign(profile,request("approval_node","later_"+suffix,target)))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("SUPERVISOR_FORWARD_TARGET_FORBIDDEN"));
    }

    @Test void managerCannotReplaceMasterAuthorityForwardedNode(){
        review("review_"+suffix,"pending_review",manager);
        node("master_"+suffix,"review_"+suffix,1,"master",null,oldUser);
        assertThatThrownBy(()->service.assign(profile,request("approval_node","master_"+suffix,target)))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("MASTER_AUTHORITY_REQUIRED"));
    }

    @Test void crossMerchantRecipientCannotTakeMerchantWork(){
        seedService("svc_"+suffix,"paid");
        jdbc.update("UPDATE access_users SET merchant_id='other_merchant' WHERE id=?",target);
        assertThatThrownBy(()->service.assign(profile,request("service_order","svc_"+suffix,target)))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("HANDOVER_TARGET_SCOPE_INVALID"));
    }

    @Test void actualPlatformDifferenceKeepsNullProjectAndDoesNotInventOne(){
        seedAll();
        jdbc.update("UPDATE integration_reconciliation_differences SET registered_by_user_id=NULL WHERE id=?","diff_"+suffix);
        jdbc.update("UPDATE access_users SET account_type='platform_admin' WHERE id IN (?,?)",manager,target);
        StaffTestActors.actor(manager,"platform_admin","project_main",null);
        var transfer=service.assign(profile,request("integration_difference","diff_"+suffix,target));
        assertThat(transfer.projectId()).isNull();
        assertThat(jdbc.queryForObject("SELECT project_id FROM staff_business_handovers WHERE id=?",String.class,transfer.id())).isNull();
    }

    @Test void thirdPartyDifferenceWithoutSourceCannotBecomePlatformScope(){
        seedAll();
        jdbc.update("UPDATE integration_reconciliation_differences SET registered_by_user_id=NULL,source_domain='thirdparty' WHERE id=?","diff_"+suffix);
        jdbc.update("UPDATE access_users SET account_type='platform_admin' WHERE id IN (?,?)",manager,target);
        StaffTestActors.actor(manager,"platform_admin","project_main",null);
        assertThat(service.overview(profile).pending()).filteredOn(p->p.businessType().equals("integration_difference"))
                .singleElement().satisfies(p->{assertThat(p.assignable()).isFalse();assertThat(p.projectId()).isNull();});
        assertThatThrownBy(()->service.assign(profile,request("integration_difference","diff_"+suffix,target)))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("HANDOVER_SCOPE_UNRESOLVED"));
    }

    private void seedAll(){
        review("review_"+suffix,"pending_review",manager);node("node_"+suffix,"review_"+suffix,2,"user",oldUser,null);
        seedRepair("repair_"+suffix,"processing","project_main");
        jdbc.update("INSERT INTO facility_inspection_tasks(id,inspection_type,status,title,assignee_id,project_id) VALUES(?,'facility','in_progress','巡检',?,'project_main')","inspect_"+suffix,oldUser);
        jdbc.update("INSERT INTO merchant_service_work_orders(id,merchant_id,work_order_type,priority,status,title,description,reported_by_user_id,reported_channel,owner_user_id,created_at,updated_at) VALUES(?,?,'repair','medium','processing','报修','报修',?,'web',?,now(),now())","merchant_"+suffix,merchant,manager,oldUser);
        seedService("svc_"+suffix,"paid");
        jdbc.update("INSERT INTO leasing_leads(id,contact_name,source,owner_user_id,status,project_id) VALUES(?,'线索','online_inquiry',?,'following_up','project_main')","lead_"+suffix,oldUser);
        jdbc.update("INSERT INTO integration_reconciliation_batches(id,provider_code,business_type,period_start,period_end,status,created_at,updated_at) VALUES(?,'fixture_provider','retail',current_date,current_date,'open',now(),now())","batch_"+suffix);
        jdbc.update("INSERT INTO integration_reconciliation_differences(id,batch_id,provider_code,business_type,external_business_id,difference_type,status,assigned_to_user_id,registered_by_user_id,created_at,updated_at) VALUES(?,?,'fixture_provider','retail',?,'amount_mismatch','open',?,?,now(),now())","diff_"+suffix,"batch_"+suffix,"external_"+suffix,oldUser,manager);
        jdbc.update("INSERT INTO bar_table_sessions(id,bar_id,merchant_id,business_date,table_id,sales_staff_id,service_staff_id,status,opened_at,created_at,updated_at) VALUES(?,?,?,current_date,?,?,?,'open',now()-interval '2 hours',now(),now())","session_"+suffix,bar,merchant,table,oldUser,oldUser);
        jdbc.update("INSERT INTO risk_alerts(alert_id,source_domain,alert_type,risk_level,status,summary,first_reported_by,first_handler_id,project_id,created_at,updated_at) VALUES(?,'platform','fixture','normal','handling','待处理',?,?,'project_main',now(),now())","alert_"+suffix,manager,oldUser);
    }
    private void seedRepair(String id,String status,String project){jdbc.update("INSERT INTO facility_work_orders(id,code,work_order_type,status,title,assignee_id,project_id) VALUES(?,?,'repair',?,'交接维修',?,?)",id,id,status,oldUser,project);}
    private void seedService(String id,String status){jdbc.update("INSERT INTO service_orders(id,catalog_item_id,item_name,price_cent,bar_id,table_id,performer_user_id,ordered_by,status,paid_at,project_id) VALUES(?,'fixture_catalog','未结服务',1000,?,?,?,?,?,now(),'project_main')",id,bar,table,oldUser,oldUser,status);}
    private void profile(String user,String id,String status){jdbc.update("INSERT INTO staff_profiles(id,user_id,name,project_id,company_id,registration_status,status,employment_status,created_at,updated_at) VALUES(?,?,'交接员工','project_main','project_main','approved','active',?,now(),now())",id,user,status);}
    private void review(String id,String status,String initiator){jdbc.update("INSERT INTO supervisor_review_decisions(review_id,policy_code,action_type,object_type,object_id,blocked,review_status,required_level,evaluated_at,initiated_by) VALUES(?,'fixture','fixture','fixture',?,false,?,1,now(),?)",id,id,status,initiator);}
    private void node(String id,String review,int seq,String kind,String approver,String forwarded){jdbc.update("INSERT INTO supervisor_review_nodes(node_id,review_id,seq,approver_kind,approver_id,forwarded_to) VALUES(?,?,?,?,?,?)",id,review,seq,kind,approver,forwarded);}
    private StaffHandoverService.Request request(String type,String id,String recipient){return new StaffHandoverService.Request(key(),type,id,recipient,"停离职业务承接");}
    private void act(String user){StaffTestActors.actor(user,"staff","project_main",null);}
    private static String key(){return UUID.randomUUID().toString();}
}
