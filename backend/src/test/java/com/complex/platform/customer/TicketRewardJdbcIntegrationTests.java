package com.complex.platform.customer;

// 文件版本：V0.05

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.complex.platform.ticketing.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties={"spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test","spring.datasource.username=complex","spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"})
@Transactional
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class TicketRewardJdbcIntegrationTests {
    @Autowired com.complex.platform.settlement.SettlementInvoiceService invoices;
    @Autowired org.springframework.test.web.servlet.MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired TicketRewardService rewards;
    @Autowired TicketRewardConfigService configs;
    @Autowired PointsRecoveryService recoveries;
    @Autowired PointsRecoveryWaiverService waivers;
    @Autowired CustomerRepository customers;
    @Autowired TicketingRepository tickets;
    @Autowired ObjectMapper json;
    @Autowired TicketPointsBillingService billing;
    @Autowired com.complex.platform.governance.review.SupervisorReviewService reviewService;
    @Autowired com.complex.platform.finance.FinanceService finance;
    @Autowired com.complex.platform.common.crypto.PiiCipher piiCipher;
    @Autowired CustomerService customerService;
    @Autowired LoyaltyService loyalty;
    @org.junit.jupiter.api.AfterEach void clearActor(){org.springframework.security.core.context.SecurityContextHolder.clearContext();}
    private final Instant at=Instant.parse("2026-01-15T08:00:00Z");

    @Test void nationalConfigRequiresRealSeparateApprovalAndPublicAnnouncementBeforeEffect() throws Exception {
        Fixture f=seed();String requester=actor(f.entity(),List.of("ticket.reward.config.manage","ticket.reward.config.view"));
        String approver=actor(f.entity(),List.of("ticket.reward.config.manage","ticket.reward.config.view","ticket.reward.national.approve","governance.supervisor_review.approve"));
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)",requester,approver);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id,assigned_by,assigned_at) VALUES(?,'lvl_m1_lower',?,'fixture-hr-approved',now())",approver,f.entity());
        authenticate(requester,f.entity());
        Instant future=Instant.now().plusSeconds(7200).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        TicketRewardConfig pending=configs.submit(new TicketRewardConfig.Request("national-"+UUID.randomUUID(),"national",null,null,values(null,f.entity(),new BigDecimal("0.01")),future,"公告新发行价",approver,List.of()));
        assertThat(pending.status()).isEqualTo("pending_review");
        assertThatThrownBy(()->configs.publish(pending.id(),new TicketRewardConfig.PublishRequest("premature-"+UUID.randomUUID(),"尚未审批"))).hasMessageContaining("仅审批通过");
        authenticate(approver,f.entity());
        var approved=reviewService.approve(pending.reviewId(),new com.complex.platform.governance.review.SupervisorReviewApprovalRequest("approve-"+UUID.randomUUID(),"审核",List.of(),null,"approved","通过"));
        assertThat(approved.reviewStatus()).isEqualTo("approved");
        assertThat(configs.find(pending.id()).publishedAt()).isNull();
        TicketRewardConfig published=configs.publish(pending.id(),new TicketRewardConfig.PublishRequest("publish-"+UUID.randomUUID(),"提前公告"));
        assertThat(published.publishedAt()).isBefore(published.effectiveAt());
        assertThat(configs.effective("national",null,null,future).id()).isEqualTo(pending.id());
    }

    @Test void recoveryWaiverThresholdUsesApprovedNationalVersion() throws Exception {
        Fixture f=seed();
        String requester=actor(f.entity(),List.of("ticket.reward.config.manage","ticket.reward.config.view"));
        String approver=actor(f.entity(),List.of("ticket.reward.config.manage","ticket.reward.config.view","ticket.reward.national.approve","governance.supervisor_review.approve"));
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)",requester,approver);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id,assigned_by,assigned_at) VALUES(?,'lvl_m1_lower',?,'fixture-hr-approved',now())",approver,f.entity());
        authenticate(requester,f.entity());
        Instant future=Instant.now().plusSeconds(7200).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var limits=new TicketRewardConfig.Values(100L,0L,200L,null,null,null,null,null,null,null,null,null,null,List.of());
        TicketRewardConfig pending=configs.submit(new TicketRewardConfig.Request("waiver-config-"+UUID.randomUUID(),"recovery_waiver",null,null,limits,future,"全国待追回积分小额豁免线",approver,List.of()));
        assertThat(pending.status()).isEqualTo("pending_review");
        assertThatThrownBy(()->configs.effective("recovery_waiver",null,null,future)).hasMessageContaining("缺少已审批");
        authenticate(approver,f.entity());
        assertThat(approve(pending.reviewId()).reviewStatus()).isEqualTo("approved");
        configs.publish(pending.id(),new TicketRewardConfig.PublishRequest("waiver-publish-"+UUID.randomUUID(),"公布已批准的豁免线"));
        assertThat(configs.effective("recovery_waiver",null,null,future).values().fixedValue()).isEqualTo(100L);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(longs={99,100})
    void financeWaiverAtOrBelowLimitKeepsOriginalFactsAndBalances(long amount) throws Exception {
        Fixture f=seed();String config=waiverConfig();seedRecovery(f,1,amount);
        authenticate(actor(f.entity(),List.of(PointsRecoveryWaiverService.VIEW,PointsRecoveryWaiverService.WAIVE)),f.entity());
        long fees=jdbc.queryForObject("SELECT sum(amount_cent) FROM ticket_points_issuance_fees WHERE reward_id IN (SELECT id FROM ticket_use_rewards WHERE customer_id=?)",Long.class,f.member());
        var request=new PointsRecoveryWaiverService.Request("waive-"+UUID.randomUUID(),config,amount,"财务核实小额豁免");
        var result=waivers.waive(f.member(),request);
        assertThat(result.waivedPoints()).isEqualTo(amount);
        assertThat(waivers.waive(f.member(),request).id()).isEqualTo(result.id());
        assertThat(recoveries.outstanding(f.member())).isZero();
        assertThat(customers.findPointsByCustomerId(f.member()).orElseThrow().balance()).isZero();
        var detail=waivers.get(f.member());
        assertThat(detail.lines()).allSatisfy(line->{assertThat(line.originalPoints()).isEqualTo(amount);assertThat(line.status()).isEqualTo("waived");});
        assertThat(detail.waivers()).hasSize(1);
        assertThat(detail.waivers().getFirst().lines().getFirst().remainingPoints()).isEqualTo(amount);
        assertThat(jdbc.queryForObject("SELECT sum(amount_cent) FROM ticket_points_issuance_fees WHERE reward_id IN (SELECT id FROM ticket_use_rewards WHERE customer_id=?)",Long.class,f.member())).isEqualTo(fees);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"100,1","60,60"})
    void waiverCannotSplitTotalAboveLimit(long first,long second) throws Exception {
        Fixture f=seed();String config=waiverConfig();
        Use firstUse=use(f,1,1,"ticket",f.member(),at.plusSeconds(1));
        rewards.used(new TicketRewardFacts.Used(firstUse.verification(),firstUse.credential()));
        rewards.release(only(f).id(),at.plusSeconds(86401));
        addConfig("points",f.type(),f.project(),values(second,null,null),at.plusSeconds(2),"V0.02");
        Use secondUse=use(f,1,2,"ticket",f.member(),at.plusSeconds(3));
        rewards.used(new TicketRewardFacts.Used(secondUse.verification(),secondUse.credential()));
        rewards.release(only(f).id(),at.plusSeconds(86403));
        jdbc.update("UPDATE customer_points_accounts SET balance=? WHERE customer_id=?",100-first,f.member());
        reverseUse(firstUse);reverseUse(secondUse);
        authenticate(actor(f.entity(),List.of(PointsRecoveryWaiverService.VIEW,PointsRecoveryWaiverService.WAIVE)),f.entity());
        var request=new PointsRecoveryWaiverService.Request("too-large-"+UUID.randomUUID(),config,first+second,"不得拆分");
        assertThatThrownBy(()->waivers.waive(f.member(),request)).hasMessageContaining("不得分笔");
        assertThat(recoveries.outstanding(f.member())).isEqualTo(first+second);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customer_points_recovery_waivers WHERE customer_id=?",Integer.class,f.member())).isZero();
    }

    @Test void waiverRequiresExplicitFinancePermissionAndFreshConfirmedAmount() throws Exception {
        Fixture f=seed();String config=waiverConfig();seedRecovery(f,1,40);
        authenticate(actor(f.entity(),List.of(PointsRecoveryWaiverService.VIEW)),f.entity());
        assertThatThrownBy(()->waivers.waive(f.member(),new PointsRecoveryWaiverService.Request("no-grant-"+UUID.randomUUID(),config,40,"未获独立授权")))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class);
        authenticate(actor(f.entity(),List.of(PointsRecoveryWaiverService.VIEW,PointsRecoveryWaiverService.WAIVE)),f.entity());
        loyalty.earnPoints(f.member(),10,"earn","recover-before-waive-"+UUID.randomUUID(),"真实新增积分先抵扣",1);
        assertThat(recoveries.outstanding(f.member())).isEqualTo(30);
        assertThatThrownBy(()->waivers.waive(f.member(),new PointsRecoveryWaiverService.Request("stale-"+UUID.randomUUID(),config,40,"旧页面总额")))
                .hasMessageContaining("总额或豁免配置已变化");
        assertThat(waivers.waive(f.member(),new PointsRecoveryWaiverService.Request("fresh-"+UUID.randomUUID(),config,30,"确认新总额")).waivedPoints()).isEqualTo(30);
    }

    @Test void waiverCannotTreatCrossProjectRecoveryAsOnlyVisibleDebt() throws Exception {
        Fixture f=seed();waiverConfig();seedRecovery(f,1,40);
        String user=actor(f.project(),List.of(PointsRecoveryWaiverService.VIEW,PointsRecoveryWaiverService.WAIVE));
        jdbc.update("UPDATE access_users SET account_type='employee',organization_id=? WHERE id=?","another-project",user);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new com.complex.platform.access.AccessActor(user,user,"employee","another-project",null,"active"),"token",List.of()));
        assertThatThrownBy(()->waivers.get(f.member())).hasMessageContaining("全部积分归属");
    }

    @Test void successfulWaiverReplayStillRequiresCurrentProjectScope() throws Exception {
        Fixture f=seed();String config=waiverConfig();seedRecovery(f,1,40);
        String user=actor(f.entity(),List.of(PointsRecoveryWaiverService.VIEW,PointsRecoveryWaiverService.WAIVE));
        authenticate(user,f.entity());
        var request=new PointsRecoveryWaiverService.Request("replay-scope-"+UUID.randomUUID(),config,40,"财务核实");
        waivers.waive(f.member(),request);
        jdbc.update("UPDATE access_users SET account_type='employee',organization_id=? WHERE id=?","another-project",user);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new com.complex.platform.access.AccessActor(user,user,"employee","another-project",null,"active"),"token",List.of()));
        assertThatThrownBy(()->waivers.waive(f.member(),request)).hasMessageContaining("全部积分归属");
    }

    private String waiverConfig() throws Exception {
        return addConfig("recovery_waiver",null,null,new TicketRewardConfig.Values(100L,0L,200L,null,null,null,null,null,null,null,null,null,null,List.of()),at.minusSeconds(120),"V0.01");
    }
    private void seedRecovery(Fixture f,int index,long amount) {
        Use u=use(f,1,index,"ticket",f.member(),at.plusSeconds(index));
        rewards.used(new TicketRewardFacts.Used(u.verification(),u.credential()));
        rewards.release(only(f).id(),at.plusSeconds(86400+index));
        jdbc.update("UPDATE customer_points_accounts SET balance=? WHERE customer_id=?",100-amount,f.member());
        reverseUse(u);
    }

    private String actor(String company,List<String> permissions){String user="u_"+UUID.randomUUID().toString().replace("-","");String role="r_"+user;
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,status,created_at) VALUES(?,?,'platform_admin',?,'active',now())",user,user,company);
        jdbc.update("INSERT INTO roles(id,role_name,created_at) VALUES(?,?,now())",role,role);
        for(String permission:permissions)jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,?)",role,permission);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)",user,role);return user;
    }
    private void authenticate(String user,String company){org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(new com.complex.platform.access.AccessActor(user,user,"platform_admin",company,null,"active"),"token",List.of()));}

    @Test void approvedExtensionProtectsAddedCalendarIntervalAndAllowsSameFactsReview() throws Exception {
        Fixture f=seed();Use use=use(f,1,1,"ticket",f.member());rewards.used(new TicketRewardFacts.Used(use.verification(),use.credential()));
        rewards.release(only(f).id(),at.plusSeconds(86400));seedBilling(f);billing.closeReadyMonths(Instant.parse("2026-02-01T08:00:00Z"));
        String bill=jdbc.queryForObject("SELECT id FROM ticket_points_monthly_bills WHERE project_id=?",String.class,f.project());billing.finalizeDue(bill,Instant.parse("2026-02-13T08:00:00Z"));
        String requester=actor(f.entity(),List.of("ticket.points.billing.manage","ticket.points.billing.view"));
        String approver=actor(f.entity(),List.of("ticket.points.billing.manage","ticket.points.billing.view","ticket.reward.national.approve","governance.supervisor_review.approve"));
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)",requester,approver);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id,assigned_by,assigned_at) VALUES(?,'lvl_m1_lower',?,'fixture-hr-approved',now())",approver,f.entity());
        authenticate(requester,f.entity());
        var outage=billing.submitOutage(new TicketPointsBilling.OutageRequest("outage-"+UUID.randomUUID(),f.project(),Instant.parse("2026-02-01T16:00:00Z"),Instant.parse("2026-02-06T16:00:00Z"),"核实五个完整自然日不可用",approver));
        authenticate(approver,f.entity());approve(outage.reviewId());
        assertThat(jdbc.queryForObject("SELECT due_date FROM ar_ap_items WHERE source_id=?",java.time.LocalDate.class,bill)).isEqualTo(java.time.LocalDate.parse("2026-02-25"));
        authenticate(requester,f.entity());
        var same=billing.submitCalendar(new TicketPointsBilling.CalendarRequest("same-calendar-"+UUID.randomUUID(),List.of(new TicketPointsBilling.Day(java.time.LocalDate.parse("2026-02-18"),true,true)),"不更改既有日期事实",approver));
        authenticate(approver,f.entity());assertThat(approve(same.reviewId()).reviewStatus()).isEqualTo("approved");
        authenticate(requester,f.entity());
        var changed=billing.submitCalendar(new TicketPointsBilling.CalendarRequest("changed-calendar-"+UUID.randomUUID(),List.of(new TicketPointsBilling.Day(java.time.LocalDate.parse("2026-02-18"),false,false)),"尝试改写延期所用日期",approver));
        authenticate(approver,f.entity());assertThatThrownBy(()->approve(changed.reviewId())).hasMessageContaining("不可回改");
        assertThat(jdbc.queryForObject("SELECT due_date FROM ticket_points_monthly_bills WHERE id=?",java.time.LocalDate.class,bill)).isEqualTo(java.time.LocalDate.parse("2026-02-16"));
        assertThat(jdbc.queryForObject("SELECT working_day FROM ticket_points_calendar_days WHERE calendar_date='2026-02-18'",Boolean.class)).isTrue();
    }

    private com.complex.platform.governance.review.SupervisorReviewDecisionRecord approve(String review){return reviewService.approve(review,new com.complex.platform.governance.review.SupervisorReviewApprovalRequest("approve-"+UUID.randomUUID(),"真实审批",List.of(),null,"approved","核实通过"));}
    private String customerToken(String member){String token="token_"+UUID.randomUUID();customers.saveSession(new CustomerSessionRecord("session_"+UUID.randomUUID(),member,token,"web",Instant.now(),Instant.now().plusSeconds(3600),"active"));return token;}
    private Use confirmedSpentUse(Fixture f) {Use use=use(f,1,1,"ticket",f.member());rewards.used(new TicketRewardFacts.Used(use.verification(),use.credential()));rewards.release(only(f).id(),at.plusSeconds(86400));jdbc.update("UPDATE customer_points_accounts SET balance=60 WHERE customer_id=?",f.member());return use;}
    private void reverseUse(Use use){jdbc.update("UPDATE ticket_verifications SET status='reversed' WHERE id=?",use.verification());rewards.reversed(new TicketRewardFacts.Reversed(use.verification()));}

    @Test void deregistrationThenVerificationCorrectionDoesNotCreateNewRecovery() throws Exception {
        Fixture f=seed();Use use=confirmedSpentUse(f);customerService.deregisterAccount(customerToken(f.member()));reverseUse(use);
        assertThat(recoveries.outstanding(f.member())).isZero();
        // ROOT-03-R21（BSM「积分、券一律清零」）：注销已把剩余 60 分清零，奖励冲正的 100 分全无余额可扣，登记即作废、不追偿。
        assertThat(jdbc.queryForObject("SELECT original_points FROM customer_points_recoveries WHERE reward_id=?",Long.class,only(f).id())).isEqualTo(100);
        assertThat(jdbc.queryForObject("SELECT status FROM customer_points_recoveries WHERE reward_id=?",String.class,only(f).id())).isEqualTo("cancelled");
        assertThat(jdbc.queryForObject("SELECT remaining_points FROM customer_points_recoveries WHERE reward_id=?",Long.class,only(f).id())).isZero();
    }

    @Test void verificationCorrectionThenDeregistrationCancelsExistingRecovery() throws Exception {
        Fixture f=seed();Use use=confirmedSpentUse(f);reverseUse(use);assertThat(recoveries.outstanding(f.member())).isEqualTo(40);
        customerService.deregisterAccount(customerToken(f.member()));
        assertThat(recoveries.outstanding(f.member())).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM customer_points_recoveries WHERE reward_id=?",String.class,only(f).id())).isEqualTo("cancelled");
    }

    @Test void recoveryOffsetLeavesOnlyNetDirectAwardToExpire() throws Exception {
        Fixture f=seed();reverseUse(confirmedSpentUse(f));assertThat(recoveries.outstanding(f.member())).isEqualTo(40);
        loyalty.earnPoints(f.member(),100,"earn","expiring-"+UUID.randomUUID(),"本批实际到账60",1);
        assertThat(customers.findPointsByCustomerId(f.member()).orElseThrow().balance()).isEqualTo(60);
        assertThat(customers.findPointsByCustomerId(f.member()).orElseThrow().keepThresholdProgress()).isEqualTo(100);
        loyalty.earnPoints(f.member(),100,"earn","newer-"+UUID.randomUUID(),"后续仍有效100",30);
        assertThat(loyalty.expirePointsForCustomer(f.member(),Instant.now().plusSeconds(2*86400))).isEqualTo(60);
        assertThat(customers.findPointsByCustomerId(f.member()).orElseThrow().balance()).isEqualTo(100);
    }

    @Test void recoveryOffsetLeavesOnlyNetConfirmedBatchAndDoesNotReduceGrowth() throws Exception {
        Fixture f=seed();reverseUse(confirmedSpentUse(f));String source="pending_"+UUID.randomUUID();
        loyalty.earnPointsPending(f.member(),100,"service_order",source,"分两次履约",1);
        loyalty.confirmPoints("service_order",source,1,2);
        assertThat(customers.findPointsByCustomerId(f.member()).orElseThrow().balance()).isEqualTo(10);
        assertThat(customers.findPointsByCustomerId(f.member()).orElseThrow().keepThresholdProgress()).isEqualTo(50);
        loyalty.confirmPoints("service_order",source,2,2);
        assertThat(jdbc.queryForObject("SELECT remaining_points FROM customer_points_ledger WHERE id=?",Long.class,"ledger_earn_"+source)).isEqualTo(60);
        assertThat(customers.findPointsByCustomerId(f.member()).orElseThrow().keepThresholdProgress()).isEqualTo(100);
        loyalty.earnPoints(f.member(),100,"earn","newer-"+UUID.randomUUID(),"后续仍有效100",30);
        assertThat(loyalty.expirePointsForCustomer(f.member(),Instant.now().plusSeconds(2*86400))).isEqualTo(60);
        assertThat(customers.findPointsByCustomerId(f.member()).orElseThrow().balance()).isEqualTo(100);
    }

    @Test void rejectedInitialMonthCanBeCorrectedAndResubmittedWithoutDeletingHistory() throws Exception {
        Fixture f=seed();String requester=actor(f.entity(),List.of("ticket.points.billing.manage"));
        String approver=actor(f.entity(),List.of("ticket.reward.national.approve","governance.supervisor_review.approve"));
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)",requester,approver);
        authenticate(requester,f.entity());
        java.time.LocalDate month=java.time.LocalDate.now(com.complex.platform.common.LocalZone.ID).withDayOfMonth(1);
        String rejected="rejected_"+UUID.randomUUID();
        jdbc.update("INSERT INTO ticket_points_billing_rules(id,version,effective_month,statement_day,objection_working_days,minimum_days,maximum_days,pay_days,status,created_by,created_at) VALUES(?,'V0.01',?,1,10,1,30,'{5,15,25}','rejected','reviewed-requester',now())",rejected,month);
        String calendar="calendar_"+UUID.randomUUID();
        jdbc.update("INSERT INTO ticket_points_calendar_cases(id,days,source_note,status,created_by,created_at) VALUES(?,'[]','本测试明确给定每日事实','approved','calendar-reviewer',now())",calendar);
        for(var day=month.plusMonths(1);day.isBefore(month.plusMonths(3));day=day.plusDays(1))jdbc.update("INSERT INTO ticket_points_calendar_days(calendar_date,working_day,bank_working_day,case_id) VALUES(?,true,true,?) ON CONFLICT (calendar_date) DO NOTHING",day,calendar);
        var pending=billing.submitRule(new TicketPointsBilling.RuleRequest("resubmit-"+UUID.randomUUID(),month,1,10,1,30,"修正驳回原因后重新审批",approver));
        assertThat(pending.status()).isEqualTo("pending_review");
        assertThat(pending.id()).isNotEqualTo(rejected);
        assertThat(jdbc.queryForObject("SELECT status FROM ticket_points_billing_rules WHERE id=?",String.class,rejected)).isEqualTo("rejected");
    }

    @Test void lateApprovedWholeDayOutageExtendsOnlyUnpaidBalanceAndKeepsOriginalBillAndPayment() throws Exception {
        Fixture f=seed();Use use=use(f,1,1,"ticket",f.member());rewards.used(new TicketRewardFacts.Used(use.verification(),use.credential()));
        rewards.release(only(f).id(),at.plusSeconds(86400));seedBilling(f);billing.closeReadyMonths(Instant.parse("2026-02-01T08:00:00Z"));
        String bill=jdbc.queryForObject("SELECT id FROM ticket_points_monthly_bills WHERE project_id=?",String.class,f.project());
        billing.finalizeDue(bill,Instant.parse("2026-02-13T08:00:00Z"));String arap="arap_"+bill;
        authenticate(actor(f.entity(),List.of("finance.payment.register","ticket.points.billing.view")),f.entity());
        var paid=finance.registerArApPayment(arap,new com.complex.platform.finance.dto.ArApPaymentRequest("paid-"+UUID.randomUUID(),"已收到40分",List.of(),40L,"bank_transfer",Instant.parse("2026-02-16T08:00:00Z"),"真实已核验回款"));
        String outage="late_"+UUID.randomUUID();
        jdbc.update("INSERT INTO ticket_points_portal_outages(id,project_id,unavailable_from,unavailable_to,reason,status,created_by,created_at,review_id) VALUES(?,?,?,?,'后来核实的整日不可用','pending_review','reviewer',now(),?)",outage,f.project(),Timestamp.from(Instant.parse("2026-02-01T16:00:00Z")),Timestamp.from(Instant.parse("2026-02-02T16:00:00Z")),outage);
        billing.decided(new com.complex.platform.governance.review.SupervisorReviewDecisionRecord(outage,"policy","ticket.reward.national","ticket_points_portal_outage",outage,false,"approved",1,1,"approve","审核",List.of(),Instant.now(),"reviewer",null,null));
        assertThat(jdbc.queryForObject("SELECT due_date FROM ticket_points_monthly_bills WHERE id=?",java.time.LocalDate.class,bill)).isEqualTo(java.time.LocalDate.parse("2026-02-16"));
        assertThat(jdbc.queryForObject("SELECT due_date FROM ar_ap_items WHERE id=?",java.time.LocalDate.class,arap)).isEqualTo(java.time.LocalDate.parse("2026-02-25"));
        assertThat(jdbc.queryForObject("SELECT unpaid_amount_cent FROM ticket_points_bill_schedule_adjustments WHERE bill_id=?",Long.class,bill)).isEqualTo(60);
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM ar_ap_payments WHERE id=?",Long.class,paid.payment().id())).isEqualTo(40);
        assertThatThrownBy(()->finance.registerArApPayment(arap,new com.complex.platform.finance.dto.ArApPaymentRequest("early-"+UUID.randomUUID(),"不能按原到期日补登记",List.of(),60L,"bank_transfer",Instant.parse("2026-02-16T10:00:00Z"),"old-due"))).hasMessageContaining("可付日");
    }

    @Test void publishedBillNoticeIsIdempotentAndFailedDeliveryDoesNotChangeObjectionClock() throws Exception {
        Fixture f=seed();seedBilling(f);String merchant="m_"+UUID.randomUUID(),bill="notice_"+UUID.randomUUID();
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,contact_phone,status,created_at,updated_at) VALUES(?,'月结通知商户','bar',?,'active',now(),now())",merchant,piiCipher.encrypt("13800138000"));
        String rule=jdbc.queryForObject("SELECT id FROM ticket_points_billing_rules WHERE effective_month='2026-01-01'",String.class);
        jdbc.update("INSERT INTO ticket_points_monthly_bills(id,billing_month,billing_rule_id,seller_entity_id,seller_merchant_id,receiver_entity_id,project_id,amount_cent,created_at,visible_at,objection_deadline,due_date) VALUES(?,'2026-01-01',?,?,?,?,?,100,now(),'2026-02-01T08:00:00Z','2026-02-13','2026-02-16')",bill,rule,f.entity(),merchant,f.entity(),f.project());
        billing.notifyPublishedBills();billing.notifyPublishedBills();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification_send_log WHERE request_id=?",Integer.class,"ticket-points-bill:"+bill)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT params->>'objection_deadline' FROM notification_send_log WHERE request_id=?",String.class,"ticket-points-bill:"+bill)).isEqualTo("2026-02-13");
        assertThat(jdbc.queryForObject("SELECT status FROM notification_send_log WHERE request_id=?",String.class,"ticket-points-bill:"+bill)).isNotEqualTo("sent");
        assertThat(jdbc.queryForObject("SELECT objection_deadline FROM ticket_points_monthly_bills WHERE id=?",java.time.LocalDate.class,bill)).isEqualTo(java.time.LocalDate.parse("2026-02-13"));
    }

    @Test void issuanceFeeWaitsForMonthlyStatementAndRealObjectionPeriodBeforeAccounting() throws Exception {
        Fixture f=seed(); Use use=use(f,1,1,"ticket",f.member()); rewards.used(new TicketRewardFacts.Used(use.verification(),use.credential()));
        assertThat(rewards.release(only(f).id(),at.plusSeconds(86400))).isTrue();
        seedBilling(f);
        assertThat(billing.closeReadyMonths(Instant.parse("2026-01-31T08:00:00Z"))).isZero();
        assertThat(billing.closeReadyMonths(Instant.parse("2026-02-01T08:00:00Z"))).isEqualTo(1);
        String bill=jdbc.queryForObject("SELECT id FROM ticket_points_monthly_bills WHERE project_id=?",String.class,f.project());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE source_id=?",Integer.class,bill)).isZero();
        assertThat(billing.finalizeDue(bill,Instant.parse("2026-02-12T08:00:00Z"))).isFalse();
        assertThat(billing.finalizeDue(bill,Instant.parse("2026-02-13T08:00:00Z"))).isTrue();
        assertThat(jdbc.queryForObject("SELECT due_date FROM ar_ap_items WHERE source_id=?",java.time.LocalDate.class,bill)).isEqualTo(java.time.LocalDate.parse("2026-02-16"));
        assertThat(jdbc.queryForObject("SELECT project_id FROM ar_ap_items WHERE source_id=?",String.class,bill)).isEqualTo(f.project());
        assertThat(jdbc.queryForObject("SELECT counterparty_id FROM ar_ap_items WHERE source_id=?",String.class,bill)).isEqualTo(f.entity());
        assertThat(billing.finalizeDue(bill,Instant.parse("2026-02-16T08:00:00Z"))).isFalse();
        assertThat(billing.closeReadyMonths(Instant.parse("2026-02-16T08:00:00Z"))).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE source_id=?",Integer.class,bill)).isEqualTo(1);
    }

    @Test void wholeNaturalDayOutageIsCombinedButPartialDayDoesNotExtendDeadline() throws Exception {
        Fixture f=seed();seedBilling(f);
        outage(f,"2026-02-01T16:00:00Z","2026-02-02T04:00:00Z");
        outage(f,"2026-02-02T04:00:00Z","2026-02-02T16:00:00Z");
        outage(f,"2026-02-03T04:00:00Z","2026-02-03T05:00:00Z");
        assertThat(billing.deadline(java.time.LocalDate.parse("2026-02-01"),10,f.project())).isEqualTo(java.time.LocalDate.parse("2026-02-16"));
        assertThat(billing.deadline(java.time.LocalDate.parse("2026-02-01"),10,"other-project")).isEqualTo(java.time.LocalDate.parse("2026-02-13"));
        assertThat(billing.due(java.time.LocalDate.parse("2026-02-16"),List.of(5,15,25))).isEqualTo(java.time.LocalDate.parse("2026-02-25"));
    }

    @Test void missingBankDayCannotBeGuessedAsMondayToFriday() throws Exception {
        Fixture f=seed();seedBilling(f);
        jdbc.update("DELETE FROM ticket_points_calendar_days WHERE calendar_date='2026-02-16'");
        assertThatThrownBy(()->billing.due(java.time.LocalDate.parse("2026-02-13"),List.of(5,15,25))).hasMessageContaining("缺少已审批");
    }

    @Test void publishedCalendarAndDueDateCannotBeRewrittenByLaterCalendarApproval() throws Exception {
        Fixture f=seed();Use use=use(f,1,1,"ticket",f.member());rewards.used(new TicketRewardFacts.Used(use.verification(),use.credential()));
        rewards.release(only(f).id(),at.plusSeconds(86400));seedBilling(f);billing.closeReadyMonths(Instant.parse("2026-02-01T08:00:00Z"));
        String bill=jdbc.queryForObject("SELECT id FROM ticket_points_monthly_bills WHERE project_id=?",String.class,f.project());
        assertThat(jdbc.queryForObject("SELECT objection_deadline FROM ticket_points_monthly_bills WHERE id=?",java.time.LocalDate.class,bill)).isEqualTo(java.time.LocalDate.parse("2026-02-13"));
        String key="calendar_"+UUID.randomUUID();
        jdbc.update("INSERT INTO ticket_points_calendar_cases(id,days,source_note,status,created_by,created_at,review_id) VALUES(?,?::jsonb,'拟更改已使用日历','pending_review','reviewer',now(),?)",key,json.writeValueAsString(List.of(new TicketPointsBilling.Day(java.time.LocalDate.parse("2026-02-02"),false,false))),key);
        assertThatThrownBy(()->billing.decided(new com.complex.platform.governance.review.SupervisorReviewDecisionRecord(key,"policy","ticket.reward.national","ticket_points_calendar",key,false,"approved",1,1,"approve","审核",List.of(),Instant.now(),"reviewer",null,null))).hasMessageContaining("不可回改");
        assertThat(jdbc.queryForObject("SELECT working_day FROM ticket_points_calendar_days WHERE calendar_date='2026-02-02'",Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject("SELECT due_date FROM ticket_points_monthly_bills WHERE id=?",java.time.LocalDate.class,bill)).isEqualTo(java.time.LocalDate.parse("2026-02-16"));
    }

    @Test void confirmationAfterMonthCloseStaysPendingUntilActualNewMonthAndDoesNotAppendOldBill() throws Exception {
        Fixture f=seed();Use first=use(f,1,1,"ticket",f.member());rewards.used(new TicketRewardFacts.Used(first.verification(),first.credential()));
        rewards.release(only(f).id(),at.plusSeconds(86400));seedBilling(f);billing.closeReadyMonths(Instant.parse("2026-02-01T08:00:00Z"));
        Use later=use(f,1,2,"ticket",f.member(),at.plusSeconds(1));rewards.used(new TicketRewardFacts.Used(later.verification(),later.credential()));
        String reward=only(f).id();
        assertThat(rewards.release(reward,at.plusSeconds(86401))).isFalse();
        assertThat(rewards.release(reward,Instant.parse("2026-02-01T08:00:00Z"))).isTrue();
        assertThat(jdbc.queryForObject("SELECT sum(amount_cent) FROM ticket_points_monthly_bills WHERE project_id=?",Long.class,f.project())).isEqualTo(100);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_points_monthly_bill_items i JOIN ticket_points_monthly_bills b ON b.id=i.bill_id WHERE b.project_id=?",Integer.class,f.project())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT confirmed_at FROM ticket_points_issuance_fees WHERE reward_id=?",Timestamp.class,reward).toInstant()).isEqualTo(Instant.parse("2026-02-01T08:00:00Z"));
    }

    // ─── FS-022-R1：积分发行费月账单接入统一开票清单 ───────────────────────────
    // ENUM L84「商户应收的兑换补偿与平台应收的积分发行费各自全额挂账、各自开票……不得轧差」；BSM L1750「开票金额≤已收−已退−已开……
    // 本系统不自建开票引擎，只做状态对接与留痕」；BSM L1803「平台内部各主体之间的往来需要开具发票」；CG 第 64 项人工登记真实开票结果。

    @Test void pointsBillInvoiceLineAppearsOnlyAfterCollectionForCollectedAmount() throws Exception {
        Fixture f=seed();seedBilling(f);String receiver=receiverEntity();
        String bill=receivablePointsBill(f,receiver,100L);
        assertThat(invoices.generateForPointsBill(bill,"system")).as("未收款不开票").isZero();
        assertThat(pointsLines(f,bill)).isEmpty();
        pay(f,"arap_"+bill,60L);
        assertThat(invoices.pointsBillsToInvoice()).contains(bill);
        assertThat(invoices.generateForPointsBill(bill,"system")).isEqualTo(1);
        var lines=pointsLines(f,bill);
        assertThat(lines).hasSize(1);
        var line=lines.getFirst();
        assertThat(line.amountCent()).as("按已收额开").isEqualTo(60L);
        assertThat(line.status()).isEqualTo("pending");
        assertThat(line.incomeType()).isEqualTo("points_issuance");
        assertThat(line.invoiceItemName()).isEqualTo("积分发行费");
        assertThat(line.subjectEntityId()).as("开票主体＝收款方").isEqualTo(receiver);
        assertThat(line.buyerEntityId()).as("购方＝合同销售方").isEqualTo(f.entity());
        assertThat(line.buyerName()).isEqualTo("奖励测试销售公司");
        assertThat(line.billId()).isNull();
    }

    @Test void pointsBillInvoiceIsIdempotentAndSecondCollectionAddsOnlyRemainder() throws Exception {
        Fixture f=seed();seedBilling(f);String receiver=receiverEntity();
        String bill=receivablePointsBill(f,receiver,100L);
        pay(f,"arap_"+bill,60L);
        assertThat(invoices.generateForPointsBill(bill,"system")).isEqualTo(1);
        assertThat(invoices.generateForPointsBill(bill,"system")).as("重跑不增行").isZero();
        assertThat(invoices.pointsBillsToInvoice()).doesNotContain(bill);
        pay(f,"arap_"+bill,40L);
        assertThat(invoices.generateForPointsBill(bill,"system")).isEqualTo(1);
        assertThat(pointsLines(f,bill)).extracting(l->l.amountCent()).containsExactlyInAnyOrder(60L,40L);
    }

    @Test void pointsInvoiceListAndIssuedRegistrationUseFrontendRequestShape() throws Exception {
        Fixture f=seed();seedBilling(f);String receiver=receiverEntity();
        String bill=receivablePointsBill(f,receiver,100L);
        pay(f,"arap_"+bill,100L);
        invoices.generateForPointsBill(bill,"system");
        String user=actor(f.entity(),List.of("settlement.invoice.view","settlement.invoice.manage"));
        var who=org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new com.complex.platform.access.AccessActor(user,user,"platform_admin",f.entity(),null,"active"),null,
                        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_P0_STAFF"))));
        String body=mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/settlement/invoice-items")
                        .param("projectId",f.project()).param("periodStart","2026-01-01").header("X-Terminal-Type","web").with(who))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
        var items=json.readTree(body).path("data").path("items");
        assertThat(items).as("积分发行费行进入开票清单").hasSize(1);
        var node=items.get(0);
        assertThat(node.path("points_bill_id").asText()).isEqualTo(bill);
        assertThat(node.path("buyer_name").asText()).isEqualTo("奖励测试销售公司");
        assertThat(node.path("income_type").asText()).isEqualTo("points_issuance");
        String item=node.path("id").asText();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/settlement/invoice-items/"+item+"/status")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"issued\",\"invoice_no\":\"FP-POINTS-001\",\"remark\":\"第三方平台已开具\"}")
                        .header("X-Terminal-Type","web").with(who))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        assertThat(jdbc.queryForObject("SELECT status||'|'||invoice_no FROM settlement_invoice_items WHERE id=?",String.class,item)).isEqualTo("issued|FP-POINTS-001");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='settlement.invoice.status_changed' AND business_id=?",Integer.class,item)).isEqualTo(1);
    }

    private String receiverEntity(){String id="be_center_"+UUID.randomUUID().toString().replace("-","");
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'全国积分结算中心','platform','active',now(),now())",id);return id;}
    /** 收款方≠合同销售方的已公布积分月账单，经真实 finalizeDue 形成正式应收（arap_+账单号）。 */
    private String receivablePointsBill(Fixture f,String receiver,long amount){
        String rule=jdbc.queryForObject("SELECT id FROM ticket_points_billing_rules WHERE effective_month='2026-01-01'",String.class),bill="tpmb_inv_"+UUID.randomUUID().toString().replace("-","");
        jdbc.update("INSERT INTO ticket_points_monthly_bills(id,billing_month,billing_rule_id,seller_entity_id,receiver_entity_id,project_id,amount_cent,created_at,visible_at) VALUES(?,'2026-01-01',?,?,?,?,?,now(),'2026-02-01T08:00:00Z')",bill,rule,f.entity(),receiver,f.project(),amount);
        assertThat(billing.finalizeDue(bill,Instant.parse("2026-02-13T08:00:00Z"))).isTrue();
        return bill;
    }
    private void pay(Fixture f,String arap,long amount){
        authenticate(actor(f.entity(),List.of("finance.payment.register","ticket.points.billing.view")),f.entity());
        finance.registerArApPayment(arap,new com.complex.platform.finance.dto.ArApPaymentRequest("paid-"+UUID.randomUUID(),"已收到积分发行费",List.of(),amount,"bank_transfer",Instant.parse("2026-02-16T08:00:00Z"),"真实已核验回款"));
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
    private List<com.complex.platform.settlement.JdbcSettlementInvoiceRepository.SettlementInvoiceItemView> pointsLines(Fixture f,String bill){
        return invoices.list(f.project(),java.time.LocalDate.parse("2026-01-01"),null,null,1,50).items().stream().filter(i->bill.equals(i.pointsBillId())).toList();
    }

    private void seedBilling(Fixture f) {
        actor(f.entity(),List.of("ticket.points.billing.view"));
        String key=UUID.randomUUID().toString().replace("-","");
        jdbc.update("INSERT INTO ticket_points_billing_rules(id,version,effective_month,statement_day,objection_working_days,minimum_days,maximum_days,pay_days,status,created_by,created_at,approved_at) VALUES(?,'V0.01','2026-01-01',1,10,1,30,'{5,15,25}','approved','real-config-review',now(),now())","billrule_"+key);
        jdbc.update("INSERT INTO ticket_points_calendar_cases(id,days,source_note,status,created_by,created_at,approved_at) VALUES(?,'[]','本用例明确给定的工作日日历','approved','calendar-review',now(),now())",key);
        for(java.time.LocalDate d=java.time.LocalDate.parse("2026-02-01");!d.isAfter(java.time.LocalDate.parse("2026-03-31"));d=d.plusDays(1)){
            boolean working=d.getDayOfWeek().getValue()<=5;
            jdbc.update("INSERT INTO ticket_points_calendar_days(calendar_date,working_day,bank_working_day,case_id) VALUES(?,?,?,?)",d,working,working,key);
        }
    }
    private void outage(Fixture f,String start,String end){jdbc.update("INSERT INTO ticket_points_portal_outages(id,project_id,unavailable_from,unavailable_to,reason,status,created_by,created_at,approved_at) VALUES(?,?,?,?,'已核实故障','approved','outage-review',now(),now())","out_"+UUID.randomUUID(),f.project(),Timestamp.from(Instant.parse(start)),Timestamp.from(Instant.parse(end)));}

    @Test void actualHolderGetsFixedPointsAndIndependentGrowthOnlyAfterUse() throws Exception {
        Fixture f=seed();
        assertThat(rewards.list(f.member())).isEmpty();
        Use u=use(f,1,1,"ticket",f.member());
        rewards.used(new TicketRewardFacts.Used(u.verification(),u.credential()));
        var r=only(f);
        assertThat(r.points()).isEqualTo(100);
        assertThat(r.growth()).isEqualTo(7);
        assertThat(r.status()).isEqualTo("pending");
        assertThat(customers.findPointsByCustomerId(f.member()).orElseThrow().balance()).isZero();
        assertThat(customers.findPointsByCustomerId(f.member()).orElseThrow().keepThresholdProgress()).isEqualTo(7);
        assertThat(rewards.list(f.buyer())).isEmpty();
    }
    @Test void giftDrinkAndRepeatedEntryDoNotIssueAnotherReward() throws Exception {
        Fixture f=seed();Use a=use(f,1,1,"ticket",f.member());
        rewards.used(new TicketRewardFacts.Used(a.verification(),a.credential()));
        rewards.used(new TicketRewardFacts.Used(a.verification(),a.credential()));
        Use b=use(f,1,1,"admission",f.member());rewards.used(new TicketRewardFacts.Used(b.verification(),b.credential()));
        Use gift=use(f,1,1,"gift_drink",f.member());rewards.used(new TicketRewardFacts.Used(gift.verification(),gift.credential()));
        assertThat(rewards.list(f.member())).hasSize(1);
        assertThat(customers.findPointsByCustomerId(f.member()).orElseThrow().keepThresholdProgress()).isEqualTo(7);
    }
    @Test void secondPersonAndLaterUseLockSeparatePublishedRule() throws Exception {
        Fixture f=seed();Use first=use(f,1,1,"ticket",f.member());rewards.used(new TicketRewardFacts.Used(first.verification(),first.credential()));
        Use second=use(f,2,1,"ticket",f.buyer());rewards.used(new TicketRewardFacts.Used(second.verification(),second.credential()));
        addConfig("points",f.type(),f.project(),values(120L,null,null),at.plusSeconds(30),"V0.02");
        Use later=use(f,1,2,"ticket",f.member(),at.plusSeconds(60));rewards.used(new TicketRewardFacts.Used(later.verification(),later.credential()));
        assertThat(rewards.list(f.member())).extracting(TicketUseRewardRecord::points).containsExactly(120L,100L);
        assertThat(rewards.list(f.buyer())).extracting(TicketUseRewardRecord::points).containsExactly(100L);
    }
    @Test void waitBoundaryAndPriceAtReleaseAreIndependentLockedVersions() throws Exception {
        Fixture f=seed();Use u=use(f,1,1,"ticket",f.member());rewards.used(new TicketRewardFacts.Used(u.verification(),u.credential()));
        var r=only(f);
        addConfig("national",null,null,values(null,f.entity(),new BigDecimal("2")),at.plusSeconds(3600),"V9.99");
        assertThat(rewards.release(r.id(),at.plusSeconds(86399))).isFalse();
        assertThat(rewards.release(r.id(),at.plusSeconds(86400))).isTrue();
        assertThat(rewards.release(r.id(),at.plusSeconds(86400))).isFalse();
        assertThat(customers.findPointsByCustomerId(f.member()).orElseThrow().balance()).isEqualTo(100);
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM ticket_points_issuance_fees WHERE reward_id=?",Long.class,r.id())).isEqualTo(200);
        assertThat(only(f).waitingHours()).isEqualTo(24);
        assertThat(jdbc.queryForObject("SELECT seller_entity_id FROM ticket_points_issuance_fees WHERE reward_id=?",String.class,r.id())).isEqualTo(f.entity());
    }
    @Test void pendingAndApprovedRefundSuspendButRejectionKeepsOriginalDeadline() throws Exception {
        Fixture f=seed();Use u=use(f,1,1,"ticket",f.member());rewards.used(new TicketRewardFacts.Used(u.verification(),u.credential()));
        String refund=refund(f,u,"requested");var r=only(f);
        assertThat(rewards.release(r.id(),at.plusSeconds(86400))).isFalse();
        jdbc.update("UPDATE ticket_refunds SET status='approved' WHERE id=?",refund);
        assertThat(rewards.release(r.id(),at.plusSeconds(86400))).isFalse();
        jdbc.update("UPDATE ticket_refunds SET status='rejected' WHERE id=?",refund);
        assertThat(rewards.release(r.id(),at.plusSeconds(86400))).isTrue();
    }
    @Test void successfulRefundCancelsOnlyItsOriginalTicketAndNeverIssuesFee() throws Exception {
        Fixture f=seed();Use u=use(f,1,1,"ticket",f.member());rewards.used(new TicketRewardFacts.Used(u.verification(),u.credential()));
        String refund=refund(f,u,"success");
        rewards.refunded(new TicketRewardFacts.Refunded(refund));rewards.refunded(new TicketRewardFacts.Refunded(refund));
        assertThat(only(f).status()).isEqualTo("cancelled");
        assertThat(customers.findPointsByCustomerId(f.member()).orElseThrow().balance()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_points_issuance_fees WHERE reward_id=?",Integer.class,only(f).id())).isZero();
    }
    /** TK-05-R2（2026-10-21 操作员裁决补充第 5 条「多人通票安全例外只退出事一人份，同票其他人照常使用」）：退款成功只冲回所退人份的奖励。 */
    @Test void refundOfOneShareCancelsOnlyThatSharesReward() throws Exception {
        Fixture f=seed();Use mine=use(f,1,1,"ticket",f.member());Use other=use(f,2,1,"ticket",f.buyer());
        rewards.used(new TicketRewardFacts.Used(mine.verification(),mine.credential()));rewards.used(new TicketRewardFacts.Used(other.verification(),other.credential()));
        rewards.refunded(new TicketRewardFacts.Refunded(refund(f,mine,"success")));
        assertThat(rewards.list(f.member()).getFirst().status()).isEqualTo("cancelled");
        assertThat(rewards.list(f.buyer()).getFirst().status()).isEqualTo("pending");
    }
    /** BSM:822「上述处理仅作用于退款申请对应票或人份、次份的尚待确认积分」：他人份到期释放不被所退人份的退款卡住或冲回。 */
    @Test void refundOfOneShareDoesNotBlockOtherSharesRelease() throws Exception {
        Fixture f=seed();Use mine=use(f,1,1,"ticket",f.member());Use other=use(f,2,1,"ticket",f.buyer());
        rewards.used(new TicketRewardFacts.Used(mine.verification(),mine.credential()));rewards.used(new TicketRewardFacts.Used(other.verification(),other.credential()));
        rewards.refunded(new TicketRewardFacts.Refunded(refund(f,mine,"success")));
        assertThat(rewards.release(rewards.list(f.buyer()).getFirst().id(),at.plusSeconds(86400))).isTrue();
        assertThat(rewards.list(f.buyer()).getFirst().status()).isEqualTo("confirmed");
    }
    @Test void specialRefundKeepsUnrecoveredBalanceAndFuturePointsRepayIt() throws Exception {
        Fixture f=seed();Use u=use(f,1,1,"ticket",f.member());rewards.used(new TicketRewardFacts.Used(u.verification(),u.credential()));
        assertThat(rewards.release(only(f).id(),at.plusSeconds(86400))).isTrue();
        jdbc.update("UPDATE customer_points_accounts SET balance=60 WHERE customer_id=?",f.member());
        rewards.refunded(new TicketRewardFacts.Refunded(refund(f,u,"success")));
        assertThat(recoveries.outstanding(f.member())).isEqualTo(40);
        assertThatThrownBy(()->recoveries.requireClear(f.member())).hasMessageContaining("待追回");
        assertThat(customers.findPointsByCustomerId(f.member()).orElseThrow().balance()).isZero();
        assertThat(recoveries.consume(f.member(),30,0,"future-1",at.plusSeconds(90000))).isEqualTo(30);
        assertThat(recoveries.outstanding(f.member())).isEqualTo(10);
        assertThat(recoveries.consume(f.member(),20,0,"future-2",at.plusSeconds(90001))).isEqualTo(10);
        recoveries.requireClear(f.member());
    }
    @Test void reversingOneOfTwoAdmissionsPreservesRewardUntilLastAdmissionReversed() throws Exception {
        Fixture f=seed();Use a=use(f,1,1,"ticket",f.member());Use b=use(f,1,1,"admission",f.member());
        rewards.used(new TicketRewardFacts.Used(a.verification(),a.credential()));rewards.used(new TicketRewardFacts.Used(b.verification(),b.credential()));
        jdbc.update("UPDATE ticket_verifications SET status='reversed' WHERE id=?",a.verification());rewards.reversed(new TicketRewardFacts.Reversed(a.verification()));
        assertThat(only(f).status()).isEqualTo("pending");
        jdbc.update("UPDATE ticket_verifications SET status='reversed' WHERE id=?",b.verification());rewards.reversed(new TicketRewardFacts.Reversed(b.verification()));
        assertThat(only(f).status()).isEqualTo("cancelled");
        Use again=use(f,1,1,"ticket",f.member());rewards.used(new TicketRewardFacts.Used(again.verification(),again.credential()));
        assertThat(rewards.list(f.member())).hasSize(2);
    }
    @Test void buyerWithoutHolderCredentialDoesNotReceiveAnotherPersonsReward() throws Exception {
        Fixture f=seed();Use u=use(f,1,1,"ticket",f.member());
        jdbc.update("UPDATE ticket_slot_holders SET holder_customer_id=? WHERE parent_ticket_id=? AND slot_index=1",f.buyer(),f.parent());
        assertThatThrownBy(()->rewards.used(new TicketRewardFacts.Used(u.verification(),u.credential()))).hasMessageContaining("持有人");
        assertThat(rewards.list(f.member())).isEmpty();
    }
    @Test void unpublishedOrMissingRuleDoesNotGuessPointsOrContract() throws Exception {
        Fixture f=seed();
        assertThatThrownBy(()->configs.effective("points",f.type(),"wrong-project",at)).hasMessageContaining("缺少");
        assertThatThrownBy(()->jdbc.update("UPDATE ticket_reward_config_versions SET payload='{}' WHERE kind='points' AND ticket_type_id=?",f.type())).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    private TicketUseRewardRecord only(Fixture f){return rewards.list(f.member()).getFirst();}
    private Fixture seed() throws Exception {
        String key=UUID.randomUUID().toString().replace("-","");String type="tt_"+key,project="p_"+key,entity="be_"+key;
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'奖励测试销售公司','platform','active',now(),now())",entity);
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','奖励测试综合体','active',now(),now())",project);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)",entity,project);
        jdbc.update("INSERT INTO ticket_types(id,ticket_name,base_calendar_price_mode,status,created_at,updated_at) VALUES(?,'奖励测试票','fixed','active',now(),now())",type);
        String buyer=customer("138"),member=customer("139"),order="to_"+key,parent="ti_"+key;
        addConfig("points",type,project,values(100L,null,null),at.minusSeconds(120),"V0.01");
        addConfig("growth",type,project,values(7L,null,null),at.minusSeconds(120),"V0.01");
        // 每例选择尚未占用的全局生效时点，仍使用实际规则查找，不假造消费累计率。
        addConfig("national",null,null,values(null,entity,BigDecimal.ONE),at.minusSeconds(120),"V"+Math.abs(key.hashCode()%100000)+".01");
        String contract=addConfig("sales_contract",type,project,new TicketRewardConfig.Values(null,null,null,null,null,null,null,null,null,null,entity,null,"合同测试-"+key,List.of()),at.minusSeconds(120),"V0.01");
        jdbc.update("INSERT INTO ticket_orders(id,order_no,ticket_type_id,quantity,sales_channel,amount_cent,pay_status,issue_status,status,created_at,updated_at,project_id,customer_id) VALUES(?,?,?,1,'web',10000,'paid','issued','created',?,?,?,?)",order,order,type,Timestamp.from(at.minusSeconds(60)),Timestamp.from(at),project,buyer);
        rewards.orderCreated(new TicketRewardFacts.OrderCreated(order));
        tickets.saveTicketInstance(new TicketInstanceRecord(parent,parent,order,type,1,2,3,buyer,buyer,"active",null,null,at,at));
        tickets.upsertSlotHolder(parent,1,member,at);tickets.upsertSlotHolder(parent,2,buyer,at);
        return new Fixture(type,project,entity,buyer,member,order,parent);
    }
    private TicketRewardConfig.Values values(Long fixed,String receiver,BigDecimal price){return new TicketRewardConfig.Values(fixed,fixed==null?null:0L,fixed==null?null:100000L,price==null?null:24,price==null?null:1,price==null?null:720,price,price==null?null:new BigDecimal("0.001"),price==null?null:new BigDecimal("100"),receiver,null,null,null,List.of());}
    private String addConfig(String kind,String type,String project,TicketRewardConfig.Values values,Instant effective,String version)throws Exception {
        String id="trc_"+UUID.randomUUID().toString().replace("-","");
        jdbc.update("INSERT INTO ticket_reward_config_versions(id,kind,ticket_type_id,project_id,version,payload,effective_at,published_at,status,created_by,created_at,approved_at) VALUES(?,?,?,?,?,?::jsonb,?,?,'approved','test-reviewer',?,?)",id,kind,type,project,version,json.writeValueAsString(values),Timestamp.from(effective),Timestamp.from(effective.minusSeconds(60)),Timestamp.from(effective.minusSeconds(120)),Timestamp.from(effective.minusSeconds(60)));
        return id;
    }
    private String customer(String prefix){String key=UUID.randomUUID().toString().replace("-","");String id="c_"+key;String phone=prefix+String.format("%08d",Math.abs(key.hashCode()%100000000));customers.saveCustomer(new CustomerRecord(id,phone,"***","active",null,at,at,at));return id;}
    private Use use(Fixture f,int slot,int index,String kind,String member){return use(f,slot,index,kind,member,at);}
    private Use use(Fixture f,int slot,int index,String kind,String member,Instant time){
        String key=UUID.randomUUID().toString().replace("-","");String ent="e_"+key,template="et_"+key,device="dev_"+key,verification="v_"+key,credential="cred_"+key;
        jdbc.update("INSERT INTO entitlement_templates(id,name,entitlement_type,default_total_times,default_value_cent,status,created_at,updated_at) VALUES(?,'奖励核销',?,3,100,'active',now(),now())",template,kind);
        tickets.saveIssuedEntitlement(new IssuedEntitlementRecord(ent,ent,f.type(),f.order(),template,kind,null,null,null,3,index,100L,"active",at.minusSeconds(60),at.plusSeconds(360000),at,at,1,null,null,null,null,null,f.parent(),slot,null,null));
        jdbc.update("INSERT INTO terminal_devices(id,device_id,device_code,terminal_type,binding_scope,status,created_at,updated_at) VALUES(?,?,?,'kiosk','venue','bound',now(),now())",device,device,device);
        tickets.saveTicketCredential(new TicketCredentialRecord(credential,ent,f.order(),credential,"n_"+key,"h_"+key,"dynamic_qr",time,time.plusSeconds(60),"consumed",1,time,time,60,time,null,null));
        rewards.presented(new TicketRewardFacts.CredentialPresented(credential,member));
        tickets.saveVerification(new VerificationRecord(verification,verification,ent,ent,"dynamic_qr",credential,device,"gate","success",null,"android_terminal_online",null,time,"n_"+key,null,null,true,true,1,null,null,null,100L));
        return new Use(ent,verification,credential);
    }
    private String refund(Fixture f,Use u,String status){String id="refund_"+UUID.randomUUID().toString().replace("-","");jdbc.update("INSERT INTO ticket_refunds(id,target_id,refund_amount_cent,reason,refund_channel,status,requested_by,created_at,updated_at) VALUES(?,?,10000,'批准的退款','original',?,'test',now(),now())",id,f.order(),status);jdbc.update("INSERT INTO ticket_refund_lines(id,refund_id,order_id,entitlement_id,entitlement_type,line_type,refund_amount_cent,remaining_times_before,used_times_before,status,created_at,updated_at) VALUES(?,?,?,?,'ticket','entitlement',10000,0,1,'approved',now(),now())","line_"+id,id,f.order(),u.entitlement());return id;}
    private record Fixture(String type,String project,String entity,String buyer,String member,String order,String parent){}
    private record Use(String entitlement,String verification,String credential){}
}
