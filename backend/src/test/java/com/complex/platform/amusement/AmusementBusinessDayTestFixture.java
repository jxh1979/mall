package com.complex.platform.amusement;

// 文件版本：V0.02

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.governance.review.SupervisorReviewApprovalRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/** 仅测试夹具：固定配置时钟后实际发起、逐级及最高权限审批，不直接写 approved。 */
public final class AmusementBusinessDayTestFixture {
    private AmusementBusinessDayTestFixture() {}
    public static String complex(JdbcTemplate jdbc) {
        String id="abd_complex_"+key();
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','营业日测试综合体','active',now(),now())",id);
        return id;
    }
    public static void bind(JdbcTemplate jdbc,String amusement,String complex) {
        String space="abd_space_"+key();
        String merchant=jdbc.queryForObject("SELECT merchant_id FROM amusement_projects WHERE id=?",String.class,amusement);
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at) VALUES(?,?,?,'营业日测试空间','amusement','active',now(),now())",space,complex,merchant);
        if(merchant!=null) {
            String primary=jdbc.queryForObject("SELECT project_id FROM merchants WHERE id=?",String.class,merchant);
            if(primary==null)jdbc.update("UPDATE merchants SET project_id=? WHERE id=?",complex,merchant);
            else if(!primary.equals(complex))jdbc.update("INSERT INTO merchant_projects(merchant_id,project_id) VALUES(?,?) ON CONFLICT DO NOTHING",merchant,complex);
        }
        jdbc.update("UPDATE amusement_projects SET space_id=? WHERE id=?",space,amusement);
    }
    public static AmusementBusinessDayService.Config approve(JdbcTemplate jdbc,AmusementBusinessDayService service,
            SupervisorReviewService reviews,String complex,String zone,int hour,Instant effectiveAt) {
        var previous=SecurityContextHolder.getContext().getAuthentication();
        Object beforeClock=ReflectionTestUtils.getField(service,"clock");
        try {
            ReflectionTestUtils.setField(service,"clock",Clock.fixed(effectiveAt.minusSeconds(3600),ZoneOffset.UTC));
            String requester=actor(jdbc,complex,List.of(AmusementBusinessDayService.MANAGE,AmusementBusinessDayService.VIEW));
            String superior=actor(jdbc,complex,List.of("governance.supervisor_review.approve","governance.supervisor_review.view"));
            jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)",requester,superior);
            authenticate(requester,complex);
            var pending=service.submit(new AmusementBusinessDayService.Request("abd-fixture-"+key(),complex,zone,hour,0,23,7,0,23,effectiveAt,"测试时钟下的实际配置审批",null,List.of()));
            authenticate(superior,complex);
            reviews.approve(pending.reviewId(),new SupervisorReviewApprovalRequest("abd-superior-"+key(),"测试上级审批",List.of(),null,"approved","同意上提"));
            master();
            reviews.approve(pending.reviewId(),new SupervisorReviewApprovalRequest("abd-master-"+key(),"测试最高权限审批",List.of(),null,"approved","核实配置"));
            return service.find(pending.id());
        } finally {
            ReflectionTestUtils.setField(service,"clock",beforeClock);
            SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }
    public static String actor(JdbcTemplate jdbc,String project,List<String> permissions) {
        String id="abd_user_"+key(),role="abd_role_"+key();
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,status,created_at) VALUES(?,?,'staff',?,'active',now())",id,id,project);
        jdbc.update("INSERT INTO roles(id,role_name,created_at) VALUES(?,?,now())",role,"营业日审批测试角色-"+new java.math.BigInteger(key(),16));
        for(String permission:permissions)jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,?)",role,permission);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)",id,role);
        return id;
    }
    public static void authenticate(String user,String project) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new AccessActor(user,user,"staff",project,null,"active"),"fixture",List.of()));
    }
    public static void master() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new AccessActor(SessionIssuer.MASTER_USER_ID,SessionIssuer.MASTER_USERNAME,SessionIssuer.MASTER_ACCOUNT_TYPE,null,null,"active"),"fixture",List.of()));
    }
    private static String key(){return UUID.randomUUID().toString().replace("-","");}
}
