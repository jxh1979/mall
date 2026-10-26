package com.complex.platform.amusement;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.*;
import com.complex.platform.governance.review.SupervisorReviewApprovalRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties={"spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test","spring.datasource.username=complex","spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"})
@Transactional
class AmusementBusinessDayJdbcIntegrationTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired AmusementBusinessDayService service;
    @Autowired SupervisorReviewService reviews;
    @Autowired com.complex.platform.analytics.AmusementBusinessDayAnalytics analytics;
    @AfterEach void reset(){SecurityContextHolder.clearContext();ReflectionTestUtils.setField(service,"clock",Clock.systemUTC());}
    private final Instant at=Instant.parse("2098-10-18T02:00:00Z");
    private String complex(){return AmusementBusinessDayTestFixture.complex(jdbc);}
    private String ride(String complex){
        String id="abd_ride_"+UUID.randomUUID().toString().replace("-","");
        jdbc.update("INSERT INTO amusement_projects(id,project_name,capacity,open_status,eligibility_rule_json,created_at,updated_at) VALUES(?,'营业日项目',10,'open','{}'::jsonb,now(),now())",id);
        AmusementBusinessDayTestFixture.bind(jdbc,id,complex);return id;
    }
    @Test void complexesHaveIndependentZonesWhileRidesInSameComplexShareOneVersion() {
        String a=complex(),b=complex(),rideA=ride(a),rideA2=ride(a),rideB=ride(b);
        var av=AmusementBusinessDayTestFixture.approve(jdbc,service,reviews,a,"Asia/Shanghai",10,at.minusSeconds(86400*5));
        AmusementBusinessDayTestFixture.approve(jdbc,service,reviews,b,"America/Los_Angeles",4,at.minusSeconds(86400*5));
        var wa=service.requireWindow(rideA,at);
        var wb=service.requireWindow(rideB,at);
        assertThat(wa.businessDate()).isEqualTo(LocalDate.parse("2098-10-18"));
        assertThat(wb.businessDate()).isEqualTo(LocalDate.parse("2098-10-17"));
        assertThat(service.requireWindow(rideA2,at)).isEqualTo(wa);
        assertThat(wa.versionId()).isEqualTo(av.id());
        assertThat(service.requireWindow(rideA,at.minusSeconds(1)).businessDate()).isEqualTo(LocalDate.parse("2098-10-17"));
        assertThat(service.requireWindow(rideA,at.plusSeconds(1)).businessDate()).isEqualTo(wa.businessDate());
    }
    @Test void unconfiguredRideCannotBorrowGlobalCutoffOrAnotherComplexVersion() {
        String a=complex(),b=complex(),rideB=ride(b);
        AmusementBusinessDayTestFixture.approve(jdbc,service,reviews,a,"Asia/Shanghai",10,at.minusSeconds(86400));
        assertThatThrownBy(()->service.requireWindow(rideB,at)).hasMessageContaining("缺少已批准");
    }
    @Test void newApprovedVersionOnlyTakesEffectAtFutureBoundaryAndHistoryStaysOld() {
        String a=complex();
        var old=AmusementBusinessDayTestFixture.approve(jdbc,service,reviews,a,"Asia/Shanghai",10,at.minusSeconds(86400*5));
        var change=AmusementBusinessDayTestFixture.approve(jdbc,service,reviews,a,"Asia/Shanghai",12,at.plusSeconds(7200));
        assertThat(service.requireComplexWindow(a,at).versionId()).isEqualTo(old.id());
        assertThat(service.requireComplexWindow(a,at).endAt()).isEqualTo(change.effectiveAt());
        assertThat(service.requireComplexWindow(a,change.effectiveAt()).versionId()).isEqualTo(change.id());
        assertThat(service.requireComplexWindow(a,at.minusSeconds(86400)).versionId()).isEqualTo(old.id());
    }
    @Test void pendingVersionCannotBeUsedAndUpperApprovalStillRequiresMaster() {
        String a=complex();
        String requester=AmusementBusinessDayTestFixture.actor(jdbc,a,List.of(AmusementBusinessDayService.MANAGE,AmusementBusinessDayService.VIEW));
        String superior=AmusementBusinessDayTestFixture.actor(jdbc,a,List.of("governance.supervisor_review.approve","governance.supervisor_review.view"));
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)",requester,superior);
        AmusementBusinessDayTestFixture.authenticate(requester,a);
        var pending=service.submit(request(a,"Asia/Shanghai",10,0,23));
        assertThatThrownBy(()->service.requireComplexWindow(a,at)).hasMessageContaining("缺少已批准");
        AmusementBusinessDayTestFixture.authenticate(superior,a);
        reviews.approve(pending.reviewId(),approval("superior"));
        assertThat(service.find(pending.id()).status()).isEqualTo("pending_review");
        assertThatThrownBy(()->reviews.approve(pending.reviewId(),approval("not-master"))).hasMessageContaining("最高权限");
        AmusementBusinessDayTestFixture.master();reviews.approve(pending.reviewId(),approval("master"));
        assertThat(service.find(pending.id()).status()).isEqualTo("approved");
    }
    @Test void approvalAfterIntendedEffectDoesNotRetroactivelyActivateVersion() {
        String a=complex();
        String requester=AmusementBusinessDayTestFixture.actor(jdbc,a,List.of(AmusementBusinessDayService.MANAGE));
        AmusementBusinessDayTestFixture.authenticate(requester,a);
        ReflectionTestUtils.setField(service,"clock",Clock.fixed(at.minusSeconds(1),ZoneOffset.UTC));
        var row=service.submit(request(a,"Asia/Shanghai",10,0,23));
        ReflectionTestUtils.setField(service,"clock",Clock.fixed(at,ZoneOffset.UTC));
        AmusementBusinessDayTestFixture.master();reviews.approve(row.reviewId(),approval("late"));
        assertThat(service.find(row.id()).status()).isEqualTo("expired");
        assertThatThrownBy(()->service.requireComplexWindow(a,at)).hasMessageContaining("缺少已批准");
    }
    @Test void projectAuthorizationIsCheckedBeforeIdempotentReplayAndList() {
        String a=complex(),b=complex();
        String requester=AmusementBusinessDayTestFixture.actor(jdbc,a,List.of(AmusementBusinessDayService.MANAGE,AmusementBusinessDayService.VIEW));
        AmusementBusinessDayTestFixture.authenticate(requester,a);
        var request=request(a,"Asia/Shanghai",10,0,23);var pending=service.submit(request);
        assertThat(service.submit(request).id()).isEqualTo(pending.id());
        assertThatThrownBy(()->service.submit(request(b,"Asia/Shanghai",10,0,23))).hasMessageContaining("无权");
        assertThatThrownBy(()->service.list(b)).hasMessageContaining("无权");
        jdbc.update("DELETE FROM role_permissions WHERE role_id IN(SELECT role_id FROM user_roles WHERE user_id=?) AND permission_code=?",requester,AmusementBusinessDayService.MANAGE);
        assertThatThrownBy(()->service.submit(request)).isInstanceOf(com.complex.platform.common.api.ApiException.class);
        assertThat(service.list(null).projects()).extracting(AmusementBusinessDayService.ProjectOption::id).containsExactly(a);
    }
    @Test void invalidTimeZoneAndIncompleteBoundsDoNotCreateVersions() {
        String a=complex();AmusementBusinessDayTestFixture.authenticate(AmusementBusinessDayTestFixture.actor(jdbc,a,List.of(AmusementBusinessDayService.MANAGE)),a);
        assertThatThrownBy(()->service.submit(request(a,"Not/A_Zone",10,0,23))).hasMessageContaining("时区");
        assertThatThrownBy(()->service.submit(request(a,"Asia/Shanghai",10,11,23))).hasMessageContaining("上下限");
        assertThatThrownBy(()->service.submit(request(a,"Asia/Shanghai",10,null,23))).hasMessageContaining("上下限");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM amusement_business_day_configs WHERE project_id=?",Integer.class,a)).isZero();
    }
    @Test void missingOrConflictingComplexOwnershipIsNotGuessedFromActor() {
        String a=complex(),ride=ride(a),b=complex();
        jdbc.update("UPDATE amusement_projects SET space_id=NULL WHERE id=?",ride);
        assertThatThrownBy(()->service.complexId(ride)).hasMessageContaining("唯一综合体");
        String merchant="abd_m_"+UUID.randomUUID().toString().replace("-","");
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at) VALUES(?,'多综合体商户','amusement',?,'active',now(),now())",merchant,a);
        jdbc.update("INSERT INTO merchant_projects(merchant_id,project_id) VALUES(?,?)",merchant,b);
        jdbc.update("UPDATE amusement_projects SET merchant_id=? WHERE id=?",merchant,ride);
        assertThatThrownBy(()->service.complexId(ride)).hasMessageContaining("跨综合体");
        AmusementBusinessDayTestFixture.bind(jdbc,ride,b);
        assertThat(service.complexId(ride)).isEqualTo(b);
    }
    @Test void metricDatesUseEachComplexAndLockedFactsSurviveLaterSpaceChanges() {
        String a=complex(),b=complex(),ra=ride(a),rb=ride(b);
        AmusementBusinessDayTestFixture.approve(jdbc,service,reviews,a,"Asia/Shanghai",10,at.minusSeconds(86400*5));
        AmusementBusinessDayTestFixture.approve(jdbc,service,reviews,b,"America/Los_Angeles",4,at.minusSeconds(86400*5));
        verification(ra,at);verification(rb,at);
        var aRows=analytics.detail("traffic.amusement",LocalDate.parse("2098-10-18"),LocalDate.parse("2098-10-18"),at,false,20,0,List.of(a));
        var bRows=analytics.detail("traffic.amusement",LocalDate.parse("2098-10-17"),LocalDate.parse("2098-10-17"),at,false,20,0,List.of(b));
        assertThat(aRows.summaryValue()).isEqualByComparingTo("1");
        assertThat(bRows.summaryValue()).isEqualByComparingTo("1");
        assertThat(analytics.detail("traffic.amusement",LocalDate.parse("2098-10-18"),LocalDate.parse("2098-10-18"),at,false,20,0,List.of(b)).summaryValue()).isZero();
        jdbc.update("UPDATE spaces SET project_id=? WHERE id=(SELECT space_id FROM amusement_projects WHERE id=?)",b,ra);
        assertThat(analytics.detail("traffic.amusement",LocalDate.parse("2098-10-18"),LocalDate.parse("2098-10-18"),at,false,20,0,List.of(a)).summaryValue()).isEqualByComparingTo("1");
        assertThat(aRows.rows()).allSatisfy(row->assertThat(row.get("complexId")).isEqualTo(a));
    }
    @Test void capacityMetricCountsEachRideOnceAcrossMultipleSessions() {
        String a=complex(),ra=ride(a);
        AmusementBusinessDayTestFixture.approve(jdbc,service,reviews,a,"Asia/Shanghai",10,at.minusSeconds(86400*5));
        verification(ra,at);verification(ra,at.plusSeconds(1));
        var result=analytics.detail("amusement.session_capacity",null,null,at,true,20,0,List.of(a));
        assertThat(result.totalRows()).isEqualTo(1);
        assertThat(result.summaryValue()).isEqualByComparingTo("10");
        assertThat(analytics.detail("amusement.session_count",LocalDate.parse("2098-10-18"),LocalDate.parse("2098-10-18"),at,false,20,0,List.of(a)).totalRows()).isEqualTo(2);
    }
    @Test void datedMetricsRejectUnconfiguredHistoricalSourceRatherThanUsingGlobalWindow() {
        String a=complex(),ra=ride(a);
        String session="abd_session_"+UUID.randomUUID().toString().replace("-","");
        jdbc.update("INSERT INTO amusement_sessions(id,project_id,start_time,end_time,capacity,used_capacity,status,created_at,updated_at) VALUES(?,?,?, ?,0,0,'open',?,?)",session,ra,java.sql.Timestamp.from(at),java.sql.Timestamp.from(at.plusSeconds(3600)),java.sql.Timestamp.from(at),java.sql.Timestamp.from(at));
        assertThatThrownBy(()->analytics.detail("amusement.session_count",LocalDate.parse("2098-10-18"),LocalDate.parse("2098-10-18"),at,false,20,0,List.of(a))).hasMessageContaining("缺少真实综合体或对应历史");
        assertThat(analytics.detail("amusement.session_count",null,null,at,true,20,0,List.of(a)).totalRows()).isEqualTo(1);
    }

    @Test void sessionsCreatedAWeekEarlyStillUseTheirLockedOpeningBusinessDate() {
        String a=complex(),ra=ride(a);
        AmusementBusinessDayTestFixture.approve(jdbc,service,reviews,a,"Asia/Shanghai",10,at.minusSeconds(86400*10));
        verification(ra,at);
        verification(ra,at.plusSeconds(86400));
        jdbc.update("UPDATE amusement_sessions SET created_at=? WHERE project_id=?",java.sql.Timestamp.from(at.minusSeconds(86400*7)),ra);
        var date=LocalDate.parse("2098-10-18");
        var dated=analytics.detail("amusement.session_count",date,date,at,false,1,0,List.of(a));
        assertThat(dated.totalRows()).isEqualTo(1);
        assertThat(dated.summaryValue()).isEqualByComparingTo("1");
        assertThat(dated.rows()).hasSize(1);
        assertThat(dated.rows().getFirst().get("businessDate")).isEqualTo(date);
        var today=analytics.detail("amusement.session_count",null,null,at,false,1,0,List.of(a));
        assertThat(today.totalRows()).isEqualTo(1);
        assertThat(today.summaryValue()).isEqualByComparingTo("1");
        assertThat(today.rows()).isEqualTo(dated.rows());
        var nextPage=analytics.detail("amusement.session_count",date,date,at,false,1,1,List.of(a));
        assertThat(nextPage.rows()).isEmpty();
        assertThat(nextPage.totalRows()).isEqualTo(1);
        assertThat(nextPage.summaryValue()).isEqualByComparingTo("1");
    }

    private void verification(String ride,Instant time) {
        var window=service.requireWindow(ride,time);
        String id=UUID.randomUUID().toString().replace("-",""),session="abd_s_"+id;
        jdbc.update("INSERT INTO amusement_sessions(id,project_id,start_time,end_time,capacity,used_capacity,status,created_at,updated_at,complex_id,business_day_version_id,business_date,business_start_at,business_end_at) VALUES(?,?,?, ?,0,0,'open',?,?,?,?,?,?,?)",
                session,ride,java.sql.Timestamp.from(time),java.sql.Timestamp.from(time.plusSeconds(3600)),java.sql.Timestamp.from(time),java.sql.Timestamp.from(time),
                window.complexId(),window.versionId(),window.businessDate(),java.sql.Timestamp.from(window.startAt()),java.sql.Timestamp.from(window.endAt()));
        jdbc.update("INSERT INTO amusement_verifications(id,project_id,session_id,credential_payload,verification_result,created_at,complex_id,business_day_version_id,business_date,business_start_at,business_end_at) VALUES(?,?,?,'test','success',?,?,?,?,?,?)",
                "abd_v_"+id,ride,session,java.sql.Timestamp.from(time),window.complexId(),window.versionId(),window.businessDate(),java.sql.Timestamp.from(window.startAt()),java.sql.Timestamp.from(window.endAt()));
    }

    /** 第十八批第 1 条（ENUM:187「出厂默认起点为上午 10:00、闭店为次日 07:00，须同时配置上下限」）：综合体版本带闭店时点及上下限。 */
    @Test void complexVersionStoresCloseHourAndRequiresItsBounds() {
        String a=complex();AmusementBusinessDayTestFixture.authenticate(AmusementBusinessDayTestFixture.actor(jdbc,a,List.of(AmusementBusinessDayService.MANAGE)),a);
        var pending=service.submit(request(a,"Asia/Shanghai",10,8,12,7,5,8));
        assertThat(jdbc.queryForList("SELECT close_hour,min_close_hour,max_close_hour FROM amusement_business_day_configs WHERE id=?",pending.id()))
                .containsExactly(java.util.Map.of("close_hour",7,"min_close_hour",5,"max_close_hour",8));
        assertThat(pending.closeHour()).isEqualTo(7);
        assertThatThrownBy(()->service.submit(request(a,"Asia/Shanghai",10,8,12,null,5,8))).hasMessageContaining("闭店小时及上下限");
        assertThatThrownBy(()->service.submit(request(a,"Asia/Shanghai",10,8,12,9,5,8))).hasMessageContaining("闭店小时及上下限");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM amusement_business_day_configs WHERE project_id=?",Integer.class,a)).isEqualTo(1);
    }

    private AmusementBusinessDayService.Request request(String project,String zone,Integer hour,Integer min,Integer max) {
        return request(project,zone,hour,min,max,7,0,23);
    }
    private AmusementBusinessDayService.Request request(String project,String zone,Integer hour,Integer min,Integer max,
            Integer close,Integer minClose,Integer maxClose) {
        return new AmusementBusinessDayService.Request("abd-test-"+UUID.randomUUID(),project,zone,hour,min,max,close,minClose,maxClose,at,"测试营业计划",null,List.of());
    }
    private SupervisorReviewApprovalRequest approval(String key){return new SupervisorReviewApprovalRequest(key+UUID.randomUUID(),"复核",List.of(),null,"approved","同意");}
}
