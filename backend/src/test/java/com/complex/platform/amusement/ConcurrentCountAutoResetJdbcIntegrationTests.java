package com.complex.platform.amusement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import com.complex.platform.support.TestSecurityActors;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** 真实审批配置、核销事实与快照联合验证；历史点位快照不再重复作为人数。 */
@SpringBootTest(properties={
    "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
    "spring.datasource.username=complex",
    "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class ConcurrentCountAutoResetJdbcIntegrationTests {
    @Autowired AmusementService service;
    @Autowired AmusementRepository repository;
    @Autowired AmusementBusinessDayService businessDays;
    @Autowired AmusementSessionLifecycleService lifecycle;
    @Autowired SupervisorReviewService reviews;
    @Autowired JdbcTemplate jdbc;
    String suffix,freeProject,endedProject,freeSession,endedSession,complex;
    Instant now,dayEnd;

    @BeforeEach void seed() {
        TestSecurityActors.clear();
        suffix=UUID.randomUUID().toString().replace("-","");
        now=Instant.now().truncatedTo(ChronoUnit.MILLIS);
        complex=AmusementBusinessDayTestFixture.complex(jdbc);
        AmusementBusinessDayTestFixture.approve(jdbc,businessDays,reviews,complex,"Asia/Shanghai",4,now.minusSeconds(10*86400));
        freeProject=project("free");endedProject=project("ended");
        dayEnd=businessDays.requireWindow(freeProject,now).endAt();
        freeSession=session(freeProject,"free",now.minusSeconds(3600),dayEnd.plusSeconds(2*86400));
        endedSession=session(endedProject,"ended",now.minusSeconds(3*3600),now.minusSeconds(3600));
        admission(freeProject,freeSession,"free",3,now);
        admission(endedProject,endedSession,"ended",4,now.minusSeconds(2*3600));
    }
    @AfterEach void clear(){TestSecurityActors.clear();}

    @Test void businessDayEndResetsFreeFlowCountsOnceWithAudit() {
        service.resetConcurrentCountsAtBusinessDayEnd(now);
        assertThat(count(freeProject)).isEqualTo(3);
        service.resetConcurrentCountsAtBusinessDayEnd(dayEnd.plusSeconds(1));
        assertThat(count(freeProject)).isZero();
        var logs=jdbc.queryForList("SELECT * FROM amusement_concurrent_reset_log WHERE project_id=? AND scope='business_day_end'",freeProject);
        assertThat(logs).hasSize(1);
        assertThat(logs.getFirst()).containsEntry("cleared_people",3).containsEntry("cleared_rows",1);
        assertThat(((Timestamp)logs.getFirst().get("boundary_at")).toInstant()).isEqualTo(dayEnd);
        assertThat(jdbc.queryForObject("SELECT release_source FROM amusement_verifications WHERE id=?",String.class,"entry_free_"+suffix)).isEqualTo("business_day_end");
        service.resetConcurrentCountsAtBusinessDayEnd(dayEnd.plusSeconds(60));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM amusement_concurrent_reset_log WHERE project_id=? AND scope='business_day_end'",Integer.class,freeProject)).isEqualTo(1);
    }

    @Test void sessionEndClosesSessionAndClearsCountsOnce() {
        service.closeEndedSessionsAndClearCounts(now);
        assertThat(count(endedProject)).isZero();
        assertThat(count(freeProject)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT status FROM amusement_sessions WHERE id=?",String.class,endedSession)).isEqualTo("closed");
        assertThat(jdbc.queryForObject("SELECT release_source FROM amusement_verifications WHERE id=?",String.class,"entry_ended_"+suffix)).isEqualTo("session_end");
        var ending=jdbc.queryForMap("SELECT * FROM amusement_session_endings WHERE session_id=?",endedSession);
        assertThat(ending).containsEntry("cleared_people",4).containsEntry("mode","natural");
        service.closeEndedSessionsAndClearCounts(now.plusSeconds(60));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM amusement_session_endings WHERE session_id=?",Integer.class,endedSession)).isEqualTo(1);
    }

    @Test void lateResetReleasesOldEntriesEvenAfterNewDaySnapshotWasUpdated() {
        admission(freeProject,freeSession,"new",2,dayEnd.plusSeconds(1));
        assertThat(count(freeProject)).isEqualTo(5);
        service.resetConcurrentCountsAtBusinessDayEnd(dayEnd.plusSeconds(2));
        assertThat(count(freeProject)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT released_at IS NULL FROM amusement_verifications WHERE id=?",Boolean.class,"entry_new_"+suffix)).isTrue();
        assertThat(jdbc.queryForObject("SELECT released_at IS NOT NULL FROM amusement_verifications WHERE id=?",Boolean.class,"entry_free_"+suffix)).isTrue();
    }

    @Test void missingHistoricalDayConfigurationDoesNotGuessBoundaryOrClearCounts() {
        String unknown="unknown_"+suffix;
        jdbc.update("INSERT INTO amusement_projects(id,project_name,capacity,open_status,created_at,updated_at) VALUES(?,'待补归属项目',10,'open',now(),now())",unknown);
        String session=sessionWithoutWindow(unknown,"unknown",now.minusSeconds(3600),now.plusSeconds(3600));
        repository.saveVerification(AmusementVerificationRecord.entry("entry_unknown_"+suffix,unknown,session,"fixture",null,"fixture","success",null,List.of(),now,1,null,null));
        jdbc.update("INSERT INTO amusement_capacity_snapshot(id,project_id,dimension,capacity_limit,occupied_count,updated_at) VALUES(?,?,'project',10,1,?)","snapshot_unknown_"+suffix,unknown,Timestamp.from(now));
        service.resetConcurrentCountsAtBusinessDayEnd(dayEnd.plusSeconds(1));
        assertThat(count(unknown)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT released_at IS NULL FROM amusement_verifications WHERE id=?",Boolean.class,"entry_unknown_"+suffix)).isTrue();
    }

    @Test void missingLockedEntryFactsStayPendingEvenWhenCurrentConfigurationExists() {
        jdbc.update("""
            UPDATE amusement_verifications SET complex_id=NULL,business_day_version_id=NULL,business_date=NULL,
                business_start_at=NULL,business_end_at=NULL WHERE id=?
            ""","entry_free_"+suffix);
        service.resetConcurrentCountsAtBusinessDayEnd(dayEnd.plusSeconds(1));
        assertThat(count(freeProject)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT business_day_version_id FROM amusement_verifications WHERE id=?",
            String.class,"entry_free_"+suffix)).isNull();
    }

    @Test void missingLockedSessionFactsStayPendingAtNaturalEnd() {
        jdbc.update("""
            UPDATE amusement_sessions SET complex_id=NULL,business_day_version_id=NULL,business_date=NULL,
                business_start_at=NULL,business_end_at=NULL WHERE id=?
            """,endedSession);
        service.closeEndedSessionsAndClearCounts(now);
        assertThat(count(endedProject)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT status FROM amusement_sessions WHERE id=?",String.class,endedSession))
            .isEqualTo("open");
    }

    @Test void contractHasNoManualConcurrentCountEndpoint() throws Exception {
        Path contract=Path.of("..","docs","openapi","openapi-p0.json");
        if(!Files.exists(contract))contract=Path.of("docs","openapi","openapi-p0.json");
        assertThat(Files.readString(contract)).doesNotContain("concurrent-count/clear").doesNotContain("concurrent-capacity/clear")
            .doesNotContain("clearProjectConcurrentCapacity").doesNotContain("resetConcurrentCount").doesNotContain("concurrent-reset");
    }

    private String project(String key) {
        String id="amp_"+key+"_"+suffix;
        jdbc.update("INSERT INTO amusement_projects(id,project_name,capacity,open_status,created_at,updated_at) VALUES(?,'系统释放测试项目',100,'open',now(),now())",id);
        AmusementBusinessDayTestFixture.bind(jdbc,id,complex);
        return id;
    }
    private String session(String project,String key,Instant start,Instant end) {
        String id=sessionWithoutWindow(project,key,start,end);
        lifecycle.lockSessionWindow(repository.findSessionById(id).orElseThrow());
        return id;
    }
    private String sessionWithoutWindow(String project,String key,Instant start,Instant end) {
        String id="session_"+key+"_"+suffix;
        jdbc.update("INSERT INTO amusement_sessions(id,project_id,start_time,end_time,capacity,used_capacity,status,created_at,updated_at) VALUES(?,?,?,?,0,0,'open',now(),now())",id,project,Timestamp.from(start),Timestamp.from(end));
        return id;
    }
    private void admission(String project,String session,String key,int people,Instant at) {
        assertThat(repository.claimCapacityAcrossDimensions(new CapacityClaimContext(project,session,null,null,100,people)).claimed()).isTrue();
        var entry=repository.saveVerification(AmusementVerificationRecord.entry("entry_"+key+"_"+suffix,project,session,"fixture_"+key,null,"fixture","success",null,List.of(),at,people,null,null));
        lifecycle.recordEntryWindow(entry.id(),businessDays.requireWindow(project,at));
    }
    private int count(String project) {
        return jdbc.queryForObject("SELECT occupied_count FROM amusement_capacity_snapshot WHERE project_id=? AND dimension='project'",Integer.class,project);
    }
}
