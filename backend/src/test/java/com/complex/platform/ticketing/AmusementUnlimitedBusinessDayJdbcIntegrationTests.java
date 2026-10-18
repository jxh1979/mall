package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.*;
import com.complex.platform.amusement.*;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties={
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
    "spring.datasource.username=complex",
    "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"})
@Transactional
class AmusementUnlimitedBusinessDayJdbcIntegrationTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired AmusementService amusement;
    @Autowired AmusementRepository amusementRepository;
    @Autowired AmusementSessionLifecycleService lifecycle;
    @Autowired AmusementBusinessDayService days;
    @Autowired SupervisorReviewService reviews;
    @Autowired TicketingService tickets;
    @Autowired TicketVerificationService verifications;
    @Autowired TicketAmusementAdmissionService admissions;
    @Autowired TicketingRepository ticketRepository;
    @Autowired com.complex.platform.customer.CustomerRepository customers;
    @Autowired com.complex.platform.customer.TicketRewardService rewards;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired com.complex.platform.access.AuthorizationService authorization;

    private String key, ride, session, entitlement, code, device, complex;
    @AfterEach void clear(){SecurityContextHolder.clearContext();}

    @Test void directAdmissionCreatesExactlyOneCrossReferencedTicketFact() {
        seed(2);
        var entry=enter("first");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_verifications WHERE amusement_verification_id=?",Integer.class,entry.id()))
            .as("真实游乐直连成功必须生成同事务票务核销事实").isEqualTo(1);
    }

    @Test void exitThenReenterSameBusinessDayDoesNotConsumeAnotherUse() {
        seed(2);
        var first=enter("first");
        amusement.exitVerification(new AmusementExitRequest("exit_"+key,code,null,null,"gate",List.of()));
        var second=enter("second");
        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(jdbc.queryForObject("SELECT used_times FROM issued_entitlements WHERE id=?",Integer.class,entitlement))
            .as("同营业日真实出场后再入场只放行，不重复扣次").isEqualTo(1);
    }

    @Test void amusementEntitlementWithoutActualProjectCannotUseGenericFallback() {
        seed(2);
        assertThatThrownBy(()->tickets.createVerification(new VerificationRequest(
            "orphan_"+key,"entitlement_code",code,device,"orphan_"+key,Map.of())))
            .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                error->assertThat(error.code()).isEqualTo("AMUSEMENT_PROJECT_UNRESOLVED"));
        assertThat(jdbc.queryForObject("SELECT used_times FROM issued_entitlements WHERE id=?",Integer.class,entitlement)).isZero();
    }

    @Test void usedUpSingleUseCanReenterAndPreviewWithinItsActivatedBusinessDay() {
        seed(1);
        enter("first");
        var preview=tickets.previewVerification(new VerificationRequest("preview_"+key,"entitlement_code",
                code,device,"ud_gate_"+key,Map.of()));
        assertThat(preview.allowed()).isTrue();
        amusement.exitVerification(new AmusementExitRequest("exit_"+key,code,null,null,"gate",List.of()));
        assertThat(enter("again").failureReason()).isEqualTo("day_replay");
        assertThat(used()).isEqualTo(1);
    }

    @Test void businessDaySpansNaturalMidnightAndChargesOnlyAtTheActualBoundary() {
        seed(3);
        var day=days.requireWindow(ride,Instant.now());
        var beforeMidnight=day.startAt().plusSeconds(13*3600);
        var afterMidnight=day.startAt().plusSeconds(15*3600);
        assertThat(admissions.consume(entitlement,days.requireWindow(ride,beforeMidnight),beforeMidnight))
                .isEqualTo(AmusementEntitlementGateway.Consumption.CONSUMED);
        assertThat(admissions.consume(entitlement,days.requireWindow(ride,afterMidnight),afterMidnight))
                .isEqualTo(AmusementEntitlementGateway.Consumption.DAY_REPLAY);
        var next=day.endAt().plusSeconds(1);
        assertThat(admissions.consume(entitlement,days.requireWindow(ride,next),next))
                .isEqualTo(AmusementEntitlementGateway.Consumption.CONSUMED);
        assertThat(used()).isEqualTo(2);
    }

    @Test void approvedCutoverCannotConsumeAgainBeforeTheOriginalActivationEnds() {
        seed(3);
        var original=days.requireWindow(ride,Instant.now());
        Instant first=original.startAt().plusSeconds(3600);
        assertThat(admissions.consume(entitlement,days.requireWindow(ride,first),first))
                .isEqualTo(AmusementEntitlementGateway.Consumption.CONSUMED);
        AmusementBusinessDayTestFixture.approve(jdbc,days,reviews,complex,"Asia/Shanghai",11,first.plusSeconds(60));
        Instant later=first.plusSeconds(120);
        assertThat(days.requireWindow(ride,later).versionId()).isNotEqualTo(original.versionId());
        assertThat(admissions.consume(entitlement,days.requireWindow(ride,later),later))
                .isEqualTo(AmusementEntitlementGateway.Consumption.DAY_REPLAY);
        Instant boundary=original.endAt().plusSeconds(1);
        assertThat(admissions.consume(entitlement,days.requireWindow(ride,boundary),boundary))
                .isEqualTo(AmusementEntitlementGateway.Consumption.CONSUMED);
        assertThat(used()).isEqualTo(2);
    }

    @Test void replayReversalDoesNotRestoreAnUnconsumedUseAndFirstReversalKeepsOriginal() {
        seed(2);
        var first=enter("first");
        amusement.exitVerification(new AmusementExitRequest("exit_"+key,code,null,null,"gate",List.of()));
        var again=enter("again");
        String replayTicket=ticketFor(again.id()),firstTicket=ticketFor(first.id());
        verifications.reverseVerification(replayTicket,"复核本次入场记录","test");
        assertThat(used()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM entitlement_business_day_activations WHERE entitlement_id=?",
                String.class,entitlement)).isEqualTo("active");
        verifications.reverseVerification(firstTicket,"复核原首刷记录","test");
        assertThat(used()).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM entitlement_business_day_activations WHERE entitlement_id=?",
                String.class,entitlement)).isEqualTo("reversed");
        assertThat(jdbc.queryForObject("SELECT first_ticket_verification_id FROM entitlement_business_day_activations WHERE entitlement_id=?",
                String.class,entitlement)).isEqualTo(firstTicket);
    }

    @Test void pendingRefundStillBlocksAnAlreadyActivatedDay() {
        seed(2);
        var first=Instant.now();
        admissions.consume(entitlement,days.requireWindow(ride,first),first);
        jdbc.update("UPDATE ticket_instances SET status='refund_pending' WHERE id=?","ud_parent_"+key);
        assertThatThrownBy(()->admissions.consume(entitlement,days.requireWindow(ride,first),first))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                    error->assertThat(error.code()).isEqualTo("TICKET_INSTANCE_REFUND_IN_PROGRESS"));
    }

    @Test void failedNewConsumptionReversesOnlyItsOwnActivation() {
        seed(2);
        Instant now=Instant.now();
        var window=days.requireWindow(ride,now);
        admissions.consume(entitlement,window,now);
        admissions.reverseNewConsumption(entitlement,window,now);
        assertThat(used()).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM entitlement_business_day_activations WHERE entitlement_id=?",
                String.class,entitlement)).isEqualTo("reversed");
    }

    @Test void dynamicMemberAdmissionCreatesOnlyOneRewardWithTwentyFourHourWait() throws Exception {
        seed(2);
        String member=rewardMember();
        var firstCredential=verifications.refreshCredential(entitlement,member);
        var first=enterWithCredential("member_first",firstCredential.credentialToken());
        var pending=rewards.list(member).getFirst();
        assertThat(pending.status()).isEqualTo("pending");
        assertThat(pending.points()).isEqualTo(100);
        assertThat(pending.availableAt()).isEqualTo(pending.verifiedAt().plusSeconds(86400));
        assertThat(rewards.release(pending.id())).isFalse();
        assertThat(customers.findPointsByCustomerId(member).orElseThrow().balance()).isZero();
        amusement.exitVerification(new AmusementExitRequest("exit_"+key,code,null,null,"gate",List.of()));
        var secondCredential=verifications.refreshCredential(entitlement,member);
        var second=enterWithCredential("member_second",secondCredential.credentialToken());
        assertThat(second.failureReason()).isEqualTo("day_replay");
        assertThat(rewards.list(member)).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_reward_uses WHERE verification_id=?",
                Integer.class,ticketFor(second.id()))).isZero();
        assertThat(customers.findPointsByCustomerId(member).orElseThrow().keepThresholdProgress()).isEqualTo(7);
        assertThat((Boolean)org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                rewards,"release",pending.id(),pending.availableAt())).isTrue();
        assertThat(customers.findPointsByCustomerId(member).orElseThrow().balance()).isEqualTo(100);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_points_issuance_fees WHERE reward_id=?",
                Integer.class,pending.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM ticket_credentials WHERE id=?",
                String.class,secondCredential.id())).isEqualTo("consumed");
        assertThat(used()).isEqualTo(1);
    }

    @Test void newAdmissionCannotReuseAConsumedDynamicCredential() throws Exception {
        seed(2);
        String member=rewardMember();
        var credential=verifications.refreshCredential(entitlement,member);
        enterWithCredential("member_first",credential.credentialToken());
        amusement.exitVerification(new AmusementExitRequest("exit_"+key,code,null,null,"gate",List.of()));
        assertThatThrownBy(()->enterWithCredential("different_request",credential.credentialToken()))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                    error->assertThat(error.code()).isEqualTo("TICKET_NOT_FOUND"));
    }

    @Test void invalidSignatureBlocksDirectAdmissionAtTheSharedCallback() throws Exception {
        seed(2);
        String member=rewardMember();
        var credential=verifications.refreshCredential(entitlement,member);
        jdbc.update("UPDATE ticket_credentials SET signature_value='invalid-signature' WHERE id=?",credential.id());
        assertThatThrownBy(()->enterWithCredential("invalid_signature",credential.credentialToken()))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                    error->assertThat(error.code()).isEqualTo("CREDENTIAL_SIGNATURE_INVALID"));
    }

    @Test void previewAndReversalRejectAnOperatorFromAnotherComplex() {
        seed(2);
        var first=enter("first");
        String other=AmusementBusinessDayTestFixture.complex(jdbc);
        String actor=AmusementBusinessDayTestFixture.actor(jdbc,other,List.of("verification.execute","ticket.verify.reverse"));
        AmusementBusinessDayTestFixture.authenticate(actor,other);
        assertThatThrownBy(()->tickets.previewVerification(new VerificationRequest("wrong_scope_"+key,"entitlement_code",
                code,device,"ud_gate_"+key,Map.of())))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                    error->assertThat(error.code()).isEqualTo("FORBIDDEN"));
        assertThatThrownBy(()->verifications.reverseVerification(ticketFor(first.id()),"跨综合体","test"))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                    error->assertThat(error.code()).isEqualTo("FORBIDDEN"));
    }

    @Test void anAdmissionCallbackCannotFabricateARealEntry() {
        seed(2);
        Instant at=Instant.now();
        var fake=AmusementVerificationRecord.entry("no_entry_"+key,ride,session,code,null,"gate","success",null,
                List.of(),at,1,entitlement,null);
        assertThatThrownBy(()->admissions.recordAdmission(fake,days.requireWindow(ride,at),AmusementEntitlementGateway.Consumption.CONSUMED))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                    error->assertThat(error.code()).isEqualTo("AMUSEMENT_ADMISSION_FACT_REQUIRED"));
        assertThat(used()).isZero();
    }

    private AmusementVerificationRecord enterWithCredential(String request,String payload) {
        return amusement.createVerification(new VerificationRequest("ud_"+request+"_"+key,
                "dynamic_qr",payload,null,null,Map.of("project_id",ride,"session_id",session,"staff_id","gate")));
    }

    /** 奖励配置为本测试事务的已批准历史夹具，不作为运营初值。 */
    private String rewardMember() throws Exception {
        String member="ud_member_"+key,entity="ud_entity_"+key,type="ud_type_"+key;
        Instant at=Instant.now(),effective=at.minusSeconds(600);
        customers.saveCustomer(new com.complex.platform.customer.CustomerRecord(member,
                "139"+String.format("%08d",Math.abs(key.hashCode()%100000000)),"***","active",null,at,at,at));
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'营业日测试销售主体','platform','active',now(),now())",entity);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)",entity,complex);
        rewardConfig("points",type,complex,rewardValues(100L,null),effective);
        rewardConfig("growth",type,complex,rewardValues(7L,null),effective);
        rewardConfig("national",null,null,rewardValues(null,entity),effective);
        rewardConfig("sales_contract",type,complex,new com.complex.platform.customer.TicketRewardConfig.Values(
                null,null,null,null,null,null,null,null,null,null,entity,null,"UD-CONTRACT-"+key,List.of()),effective);
        rewards.orderCreated(new TicketRewardFacts.OrderCreated("ud_order_"+key));
        ticketRepository.upsertSlotHolder("ud_parent_"+key,1,member,at);
        return member;
    }

    private com.complex.platform.customer.TicketRewardConfig.Values rewardValues(Long fixed,String entity) {
        return new com.complex.platform.customer.TicketRewardConfig.Values(fixed,fixed==null?null:0L,
                fixed==null?null:100000L,entity==null?null:24,entity==null?null:1,entity==null?null:720,
                entity==null?null:java.math.BigDecimal.ONE,entity==null?null:new java.math.BigDecimal("0.001"),
                entity==null?null:new java.math.BigDecimal("100"),entity,null,null,null,List.of());
    }

    private void rewardConfig(String kind,String type,String project,
            com.complex.platform.customer.TicketRewardConfig.Values values,Instant effective) throws Exception {
        String version="V"+Math.abs(UUID.randomUUID().hashCode()%1000000)+".01";
        jdbc.update("""
            INSERT INTO ticket_reward_config_versions(id,kind,ticket_type_id,project_id,version,payload,
              effective_at,published_at,status,created_by,created_at,approved_at)
            VALUES(?,?,?,?,?,?::jsonb,?,?,'approved','test-reviewer',?,?)
            ""","ud_rc_"+UUID.randomUUID().toString().replace("-",""),kind,type,project,version,
                json.writeValueAsString(values),java.sql.Timestamp.from(effective),
                java.sql.Timestamp.from(effective.minusSeconds(60)),java.sql.Timestamp.from(effective.minusSeconds(120)),
                java.sql.Timestamp.from(effective.minusSeconds(60)));
    }

    @Test void failedConsumptionCannotReverseAnActivationFromAnotherOccurrence() {
        seed(2);
        var window=days.requireWindow(ride,Instant.now());
        Instant first=window.startAt().plusSeconds(60);
        admissions.consume(entitlement,window,first);
        assertThatThrownBy(()->admissions.reverseNewConsumption(entitlement,window,first.plusSeconds(1)))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                    error->assertThat(error.code()).isEqualTo("AMUSEMENT_ACTIVATION_REVERSE_MISMATCH"));
        assertThat(used()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM entitlement_business_day_activations WHERE entitlement_id=?",
                String.class,entitlement)).isEqualTo("active");
    }

    @Test void offlineAmusementAttemptCannotWriteARejectionIntoAnotherComplexTicket() {
        seed(2);
        String other=AmusementBusinessDayTestFixture.complex(jdbc);
        String actor=AmusementBusinessDayTestFixture.actor(jdbc,other,
                List.of("verification.execute","terminal.device.bind"));
        AmusementBusinessDayTestFixture.authenticate(actor,other);
        assertThatThrownBy(()->tickets.syncTerminalTasks(new com.complex.platform.ticketing.dto.TerminalSyncTasksRequest(
                "cross_offline_"+key,null,null,device,actor,
                List.of(new com.complex.platform.ticketing.dto.TerminalSyncTaskItemRequest(
                    "cross_task_"+key,"offline_verification",Instant.now().toString(),
                    Map.of("entitlementCode",code,"pointId","ud_gate_"+key))))))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                    error->assertThat(error.code()).isEqualTo("FORBIDDEN"));
        assertThat(used()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_verifications WHERE entitlement_id=?",Integer.class,entitlement)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM amusement_verifications WHERE entitlement_id=?",Integer.class,entitlement)).isZero();
        assertThat(jdbc.queryForObject("SELECT COALESCE(SUM(occupied_count),0) FROM amusement_capacity_snapshot WHERE project_id=?",Integer.class,ride)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_use_rewards WHERE order_id=?",Integer.class,"ud_order_"+key)).isZero();
    }

    @Test void missingOriginalTicketComplexCannotCreateAnOnlineOrOfflineResult() {
        seed(2);
        jdbc.update("UPDATE ticket_orders SET project_id=NULL WHERE id=?","ud_order_"+key);
        assertThatThrownBy(()->tickets.createVerification(new VerificationRequest("no_origin_"+key,"entitlement_code",
                code,device,"ud_gate_"+key,Map.of())))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                    error->assertThat(error.code()).isEqualTo("TICKET_PROJECT_REQUIRED"));
        assertThatThrownBy(()->tickets.syncTerminalTasks(new com.complex.platform.ticketing.dto.TerminalSyncTasksRequest(
                "no_origin_sync_"+key,null,null,device,"gate",
                List.of(new com.complex.platform.ticketing.dto.TerminalSyncTaskItemRequest(
                    "no_origin_task_"+key,"offline_verification",Instant.now().toString(),
                    Map.of("entitlementCode",code,"pointId","ud_gate_"+key))))))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                    error->assertThat(error.code()).isEqualTo("TICKET_PROJECT_REQUIRED"));
        assertThat(used()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_verifications WHERE entitlement_id=?",Integer.class,entitlement)).isZero();
    }

    @Test void genericOnlineStillVerifiesAValidCredentialStoredOnlyByHash() throws Exception {
        seed(2);
        jdbc.update("UPDATE issued_entitlements SET entitlement_type='entry',consumption_window='per_use' WHERE id=?",entitlement);
        String member=rewardMember();
        var credential=verifications.refreshCredential(entitlement,member);
        assertThat(jdbc.queryForObject("SELECT credential_token FROM ticket_credentials WHERE id=?",String.class,credential.id())).isNull();
        var result=tickets.createVerification(new VerificationRequest("generic_signed_"+key,"dynamic_qr",
                credential.credentialToken(),device,"ud_gate_"+key,Map.of()));
        assertThat(result.status()).isEqualTo("success");
        assertThat(jdbc.queryForObject("SELECT signature_valid FROM ticket_verifications WHERE id=?",Boolean.class,result.id())).isTrue();
        assertThat(used()).isEqualTo(1);
    }


    @Test void ticketFacadeReplayRechecksScopeAfterItsSuccessfulDynamicAdmission() throws Exception {
        seed(2);
        String member=rewardMember();
        var credential=verifications.refreshCredential(entitlement,member);
        String role=crossProjectOperator();
        var request=new VerificationRequest("facade_replay_"+key,"dynamic_qr",
                credential.credentialToken(),device,"ud_gate_"+key,Map.of());
        var first=tickets.createVerification(request);
        jdbc.update("UPDATE amusement_sessions SET status='closed' WHERE id=?",session);
        assertThat(tickets.createVerification(request).id())
                .as("权限有效时，已消费动态凭证的相同请求在闭场后仍返回原事实").isEqualTo(first.id());
        jdbc.update("DELETE FROM role_viewable_projects WHERE role_id=? AND viewable_id=?",role,complex);
        assertThat(authorization.projectReach(authorization.currentActor(),"verification.execute").covers(complex)).isFalse();
        assertThatThrownBy(()->tickets.createVerification(request))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                    error->assertThat(error.code()).isEqualTo("FORBIDDEN"));
        assertThat(used()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_verifications WHERE entitlement_id=?",Integer.class,entitlement)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM amusement_verifications WHERE entitlement_id=?",Integer.class,entitlement)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT occupied_count FROM amusement_capacity_snapshot WHERE project_id=?",Integer.class,ride)).isEqualTo(1);
        assertThat(rewards.list(member)).hasSize(1);
    }

    @Test void ticketFacadePreviewReplayRechecksCurrentProjectScope() {
        seed(2);
        String role=crossProjectOperator();
        var request=new VerificationRequest("preview_replay_"+key,"entitlement_code",code,device,"ud_gate_"+key,Map.of());
        tickets.previewVerification(request);
        jdbc.update("DELETE FROM role_viewable_projects WHERE role_id=? AND viewable_id=?",role,complex);
        assertThat(authorization.projectReach(authorization.currentActor(),"verification.execute").covers(complex)).isFalse();
        assertThatThrownBy(()->tickets.previewVerification(request))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                    error->assertThat(error.code()).isEqualTo("FORBIDDEN"));
        assertNoAdmissionFacts();
    }

    @Test void ticketFacadeReversalReplayRechecksCurrentProjectScope() {
        seed(2);
        String role=crossProjectOperator();
        var entry=enter("reverse_facade");
        String ticket=ticketFor(entry.id());
        var request=new com.complex.platform.ticketing.dto.VerificationReverseRequest("reverse_replay_"+key,"测试原件冲正");
        var reversed=tickets.reverseVerification(ticket,request,"gate");
        assertThat(tickets.reverseVerification(ticket,request,"gate").id()).isEqualTo(reversed.id());
        jdbc.update("DELETE FROM role_viewable_projects WHERE role_id=? AND viewable_id=?",role,complex);
        assertThat(authorization.projectReach(authorization.currentActor(),"ticket.verify.reverse").covers(complex)).isFalse();
        assertThatThrownBy(()->tickets.reverseVerification(ticket,request,"gate"))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                    error->assertThat(error.code()).isEqualTo("FORBIDDEN"));
        assertThat(used()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_verifications WHERE reverses_verification_id=?",Integer.class,ticket)).isEqualTo(1);
    }

    @Test void directAdmissionRequiresTheOriginalTicketComplex() {
        seed(2);
        jdbc.update("UPDATE ticket_orders SET project_id=NULL WHERE id=?","ud_order_"+key);
        assertThatThrownBy(()->enter("no_original_complex"))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                    error->assertThat(error.code()).isEqualTo("TICKET_PROJECT_REQUIRED"));
        assertNoAdmissionFacts();
    }

    @Test void ticketPreviewRequiresTheOriginalTicketComplex() {
        seed(2);
        jdbc.update("UPDATE ticket_orders SET project_id=NULL WHERE id=?","ud_order_"+key);
        assertThatThrownBy(()->tickets.previewVerification(new VerificationRequest("preview_no_original_"+key,
                "entitlement_code",code,device,"ud_gate_"+key,Map.of())))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                    error->assertThat(error.code()).isEqualTo("TICKET_PROJECT_REQUIRED"));
        assertNoAdmissionFacts();
    }

    private String crossProjectOperator() {
        String other=AmusementBusinessDayTestFixture.complex(jdbc);
        String actor=AmusementBusinessDayTestFixture.actor(jdbc,complex,
                List.of("amusement.verification.execute","verification.execute","ticket.verify.reverse","terminal.device.bind"));
        String role=jdbc.queryForObject("SELECT role_id FROM user_roles WHERE user_id=?",String.class,actor);
        jdbc.update("INSERT INTO role_viewable_projects(role_id,viewable_id) VALUES(?,?),(?,?)",role,complex,role,other);
        AmusementBusinessDayTestFixture.authenticate(actor,complex);
        assertThat(authorization.projectReach(authorization.currentActor(),"verification.execute").covers(complex)).isTrue();
        assertThat(authorization.projectReach(authorization.currentActor(),"verification.execute").covers(other)).isTrue();
        return role;
    }

    private void assertNoAdmissionFacts() {
        assertThat(used()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM entitlement_business_day_activations WHERE entitlement_id=?",Integer.class,entitlement)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_verifications WHERE entitlement_id=?",Integer.class,entitlement)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM amusement_verifications WHERE entitlement_id=?",Integer.class,entitlement)).isZero();
        assertThat(jdbc.queryForObject("SELECT COALESCE(SUM(occupied_count),0) FROM amusement_capacity_snapshot WHERE project_id=?",Integer.class,ride)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_use_rewards WHERE order_id=?",Integer.class,"ud_order_"+key)).isZero();
    }

    private int used(){return jdbc.queryForObject("SELECT used_times FROM issued_entitlements WHERE id=?",Integer.class,entitlement);}
    private String ticketFor(String admission){return jdbc.queryForObject("SELECT id FROM ticket_verifications WHERE amusement_verification_id=?",String.class,admission);}

    private AmusementVerificationRecord enter(String request) {
        return amusement.createVerification(new VerificationRequest("ud_"+request+"_"+key,
            "entitlement_code",code,null,null,Map.of("project_id",ride,"session_id",session,"staff_id","gate")));
    }

    private void seed(int uses) {
        key=UUID.randomUUID().toString().replace("-","").substring(0,12);
        ride="ud_ride_"+key;session="ud_session_"+key;entitlement="ud_ent_"+key;code="ud_code_"+key;device="ud_dev_"+key;
        String safety="ud_safety_"+key,type="ud_type_"+key,template="ud_template_"+key;
        AmusementBusinessDayTestFixture.master();
        jdbc.update("INSERT INTO amusement_safety_templates(id,template_name,items_json,status,created_at,updated_at) VALUES(?,'营业日重入巡检','[{\"name\":\"restraint\",\"required\":true}]'::jsonb,'active',now(),now())",safety);
        jdbc.update("INSERT INTO amusement_projects(id,project_name,capacity,open_status,safety_template_id,eligibility_rule_json,created_at,updated_at) VALUES(?,'营业日重入项目',10,'open',?,'{}'::jsonb,now(),now())",ride,safety);
        jdbc.update("INSERT INTO amusement_project_entitlements(id,project_id,entitlement_type,created_at) VALUES(?,?,'amusement_pass',now())","ud_bind_"+key,ride);
        jdbc.update("""
            INSERT INTO amusement_safety_inspections(id,project_id,template_id,items_json,overall_result,
                inspection_status,rectification_status,reopen_status,inspected_at,submitted_at,created_at,updated_at)
            VALUES(?,?,?,'[{"name":"restraint","result":"pass"}]'::jsonb,'pass','submitted','not_required','not_required',now(),now(),now(),now())
            ""","ud_inspect_"+key,ride,safety);
        jdbc.update("UPDATE amusement_projects SET location_point_id=? WHERE id=?", "ud_gate_"+key,ride);
        complex=AmusementBusinessDayTestFixture.complex(jdbc);
        AmusementBusinessDayTestFixture.bind(jdbc,ride,complex);
        AmusementBusinessDayTestFixture.approve(jdbc,days,reviews,complex,"Asia/Shanghai",10,Instant.now().minusSeconds(7*86400));
        jdbc.update("INSERT INTO amusement_sessions(id,project_id,start_time,end_time,capacity,used_capacity,status,created_at,updated_at) VALUES(?,?,now()-interval '1 hour',now()+interval '1 hour',10,0,'open',now(),now())",session,ride);
        lifecycle.lockSessionWindow(amusementRepository.findSessionById(session).orElseThrow());
        jdbc.update("INSERT INTO ticket_types(id,ticket_name,base_calendar_price_mode,status,created_at,updated_at) VALUES(?,'营业日重入票','fixed','active',now(),now())",type);
        jdbc.update("INSERT INTO entitlement_templates(id,name,entitlement_type,default_total_times,default_value_cent,status,created_at,updated_at) VALUES(?,'营业日重入权益','amusement_pass',?,5500,'active',now(),now())",template,uses);
        jdbc.update("""
            INSERT INTO ticket_orders(id,order_no,ticket_type_id,project_id,quantity,sales_channel,amount_cent,
                pay_status,issue_status,status,created_at,updated_at)
            VALUES(?,?,?,?,1,'web',0,'paid','issued','created',now(),now())
            ""","ud_order_"+key,"UD_ORDER_"+key,type,complex);
        jdbc.update("""
            INSERT INTO issued_entitlements(id,ticket_type_id,order_id,entitlement_template_id,entitlement_code,
                entitlement_type,total_times,used_times,value_amount_cent,allocated_revenue_cent,status,
                consumption_window,valid_from,valid_to,created_at,updated_at)
            VALUES(?,?,?,?,?,'amusement_pass',?,0,5500,0,'active','unlimited_in_day',
                now()-interval '1 day',now()+interval '3 days',now(),now())
            """,entitlement,type,"ud_order_"+key,template,code,uses);
        jdbc.update("""
            INSERT INTO ticket_instances(id,ticket_no,order_id,ticket_type_id,seq_in_order,num_people,total_times,
                status,created_at,updated_at) VALUES(?,?,?,?,1,1,?,'active',now(),now())
            ""","ud_parent_"+key,"UD_PARENT_"+key,"ud_order_"+key,type,uses);
        jdbc.update("UPDATE issued_entitlements SET parent_ticket_id=?,slot_index=1 WHERE id=?","ud_parent_"+key,entitlement);
        jdbc.update("INSERT INTO terminal_devices(id,device_id,device_code,terminal_type,binding_scope,status,created_at,updated_at) VALUES(?,?,?,'kiosk','venue','bound',now(),now())",device,device,device);
        String operator=AmusementBusinessDayTestFixture.actor(jdbc,complex,
            List.of("amusement.verification.execute","verification.execute","ticket.verify.reverse","terminal.device.bind"));
        jdbc.update("UPDATE access_users SET account_type='platform_admin' WHERE id=?",operator);
        SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                new com.complex.platform.access.AccessActor(operator,operator,"platform_admin",complex,null,"active"),
                "fixture",List.of()));
    }
}
