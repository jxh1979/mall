package com.complex.platform.bar;

// 文件版本：V0.05

import static com.complex.platform.support.DailyCloseFixture.closeWithMarks;
import static com.complex.platform.support.DailyCloseFixture.markRangeFinal;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.complex.platform.access.*;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BarPayrollAllocationJdbcIntegrationTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired AccessRepository access;
    @Autowired BarPayrollAllocationService allocations;
    @Autowired BarPayrollRuleService rules;
    @Autowired BarPayrollRefundService refunds;
    @Autowired BarRevenueSourceService revenue;
    @Autowired BarRepository bars;
    @Autowired BarPayrollSourceService sources;
    @Autowired BarServiceResponsibilityService responsibilities;
    @Autowired BarTableSessionService sessions;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired MockMvc mvc;
    String project,merchant,company,table,session,bill,first,second,actor;
    final Instant start=Instant.parse("2026-09-01T12:00:00Z");
    final Instant end=start.plusSeconds(8*3600);
    static String id(){return UUID.randomUUID().toString().replace("-","");}
    @BeforeEach void fixtures() {
        String suffix=id();
        project="bpp_"+suffix;merchant="bpm_"+suffix;company="bpe_"+suffix;
        table="bpt_"+suffix;session="bps_"+suffix;bill="bpb_"+suffix;first="bpu1_"+suffix;second="bpu2_"+suffix;actor="bpa_"+suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','工资综合体','active',now(),now())",project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'经营主体','management_company','active',now(),now())",company);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)",company,project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,created_at,updated_at) VALUES(?,'测试酒吧','bar',?,?,?,?,'active',now(),now())",merchant,company,company,company,project);
        String space="space_"+suffix;
        jdbc.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at) VALUES(?,?,?,'测试卡座区域','unit','active',now(),now())",space,project,merchant);
        jdbc.update("INSERT INTO bar_tables(id,bar_id,merchant_id,table_code,table_name,table_type,capacity,allowed_pass_free_guest_count,min_consumption_cent,currency,status,created_at,updated_at,space_id) VALUES(?,? ,?,'T','测试卡座','card_seat',4,0,0,'CNY','active',now(),now(),?)",table,merchant,merchant,space);
        for(String user:List.of(actor,first,second))access.createUser(new AccessUser(user,user,"staff",project,merchant,"active",start));
        String role="bpr_"+suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,'薪资测试单独授权','测试专用','platform','active',now())",role);
        for(String permission:List.of("bar.payroll.view","bar.payroll.manage"))jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,?)",role,permission);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'bar.order.create')",role);
        for(String user:List.of(actor,first,second)) {
            access.assignUserRole(user,role);
            jdbc.update("INSERT INTO staff_profiles(id,project_id,merchant_id,user_id,name,company_id,registration_status,status,employment_status,created_at,updated_at) VALUES(?,?,?,?,'真实在岗测试员工',?,'approved','active','employed',now(),now())",
                "profile_"+user,project,merchant,user,company);
        }
        as(actor,merchant);
        jdbc.update("INSERT INTO bar_table_sessions(id,bar_id,merchant_id,business_date,table_id,service_staff_id,status,opened_at,closed_at,created_at,updated_at) VALUES(?,?,?,'2026-09-01',?,?,'closed',?,?,?,?)",
            session,merchant,merchant,table,second,Timestamp.from(start),Timestamp.from(end),Timestamp.from(start),Timestamp.from(end));
        String order="bpo_"+suffix;
        jdbc.update("INSERT INTO bar_orders(id,bar_id,merchant_id,table_id,order_source,status,total_amount_cent,currency,created_at,updated_at) VALUES(?,?,?,?,'staff','created',100000,'CNY',?,?)",
            order,merchant,merchant,table,Timestamp.from(start),Timestamp.from(end));
        jdbc.update("INSERT INTO bar_bills(id,bill_no,order_id,bar_id,merchant_id,table_id,subtotal_amount_cent,discount_amount_cent,deposit_deduct_amount_cent,payable_amount_cent,paid_amount_cent,currency,payment_status,settlement_status,tip_amount_cent,created_at,updated_at) VALUES(?,?,?,?,?,?,100000,0,0,100000,120000,'CNY','paid','settled',20000,?,?)",
            bill,"B"+suffix,order,merchant,merchant,table,Timestamp.from(start),Timestamp.from(end));
        jdbc.update("INSERT INTO bar_bill_settlements(id,bill_id,settle_channel,settlement_amount_cent,merchant_id,settled_at) VALUES(?,?,'aggregate_pay',120000,?,?)","pay_"+suffix,bill,merchant,Timestamp.from(end));
        jdbc.update("INSERT INTO bar_order_service_sources(order_id,session_id,table_id,project_id,merchant_id,business_date) VALUES(?,?,?,?,?,'2026-09-01')",order,session,table,project,merchant);
        jdbc.update("INSERT INTO bar_payroll_bill_components(id,source_bill_id,session_id,source_table_id,project_id,merchant_id,business_date,component_type,amount_cent,original_base_cent,source_event_id,source_snapshot,collection_bill_id) VALUES(?,?,?,?,?,?,'2026-09-01','tip',20000,20000,?,'[]'::jsonb,?)",
            "c_"+suffix,bill,session,table,project,merchant,"event_"+suffix,bill);
        jdbc.update("INSERT INTO bar_payroll_collection_links(source_bill_id,collection_bill_id) VALUES(?,?)",bill,bill);
        duty(first,start,start.plusSeconds(2*3600));duty(second,start.plusSeconds(2*3600),end);
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    void as(String user,String tenant) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new AccessActor(user,user,"staff",project,tenant,"active"),"test",List.of()));
    }
    void duty(String user,Instant from,Instant to) {
        jdbc.update("INSERT INTO bar_service_responsibilities(id,session_id,table_id,project_id,merchant_id,user_id,payer_company_id,started_at,ended_at,source_id,recorded_by,reason) VALUES(?,?,?,?,?,?,?,?,?,?,?,'真实交接')",
            id(),session,table,project,merchant,user,company,Timestamp.from(from),Timestamp.from(to),id(),actor);
    }
    List<BarPayrollRequests.MonthRun> close(){return allocations.close(new BarPayrollRequests.CloseMonth(id(),project,merchant,"2026-09"));}
    @Test void realCollectionAndActualResponsibilitiesGenerateImmutableFiftyAndOneHundredFifty() {
        assertThat(close()).singleElement().satisfies(run->{
            assertThat(run.status()).isEqualTo("confirmed");
            assertThat(run.collectedTipCent()).isEqualTo(20000);assertThat(run.payableTipCent()).isEqualTo(20000);
        });
        assertThat(allocations.confirmedAllocations(first,"2026-09")).singleElement().satisfies(a->{
            assertThat(a.amountCent()).isEqualTo(5000);assertThat(a.payerCompanyId()).isEqualTo(company);assertThat(a.sourceBillIds()).containsExactly(bill);
        });
        assertThat(allocations.confirmedAllocations(second,"2026-09")).singleElement().extracting(BarPayrollAllocation::amountCent).isEqualTo(15000L);
        close();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_payroll_runs WHERE merchant_id=?",Integer.class,merchant)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_payroll_allocations WHERE merchant_id=?",Integer.class,merchant)).isEqualTo(2);
    }
    @Test void paymentFlagWithoutRealCollectionDoesNotCreatePayroll() {
        jdbc.update("UPDATE bar_bill_settlements SET reversed=true WHERE bill_id=?",bill);
        assertThat(close()).singleElement().satisfies(run->{
            assertThat(run.status()).isEqualTo("pending");assertThat(run.unresolvedSources()).anyMatch(s->s.contains("真实收款"));
        });
        assertThat(allocations.confirmedAllocations(first,"2026-09")).isEmpty();
    }
    @Test void incompleteResponsibilityHistoryIsUnresolvedRatherThanZeroCompleted() {
        jdbc.update("DELETE FROM bar_service_responsibilities WHERE session_id=? AND user_id=?",session,first);
        assertThat(close()).singleElement().satisfies(run->{assertThat(run.status()).isEqualTo("pending");assertThat(run.unresolvedSources()).anyMatch(s->s.contains("空档"));});
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_payroll_runs WHERE merchant_id=?",Integer.class,merchant)).isZero();
    }
    @Test void merchantCannotReadAnotherMerchantEvenWithPlatformScopedRole() {
        assertThatThrownBy(()->allocations.overview(project,"other_merchant","2026-09"))
            .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("BAR_PAYROLL_SCOPE_MISMATCH"));
    }
    @Test void accountWithoutExplicitSensitivePermissionCannotReadAllocations() {
        jdbc.update("DELETE FROM user_roles WHERE user_id=?",first);
        as(first,merchant);
        assertThatThrownBy(()->allocations.overview(project,merchant,"2026-09")).isInstanceOf(ApiException.class);
    }

    @Test void multiChannelPaymentOnlyBecomesRevenueWhenTheWholeBillIsCompleted() {
        jdbc.update("UPDATE bar_bill_settlements SET settlement_amount_cent=60000 WHERE bill_id=?",bill);
        jdbc.update("UPDATE bar_bills SET paid_amount_cent=60000,settlement_status='unsettled',payment_status='unpaid' WHERE id=?",bill);
        assertThat(revenue.completedCollections(merchant,start,end.plusSeconds(10))).singleElement().satisfies(r->{
            assertThat(r.status()).isEqualTo("payment_pending");assertThat(r.goodsRevenueCent()).isNull();
        });
        assertThat(close()).singleElement().extracting(BarPayrollRequests.MonthRun::status).isEqualTo("pending");
        jdbc.update("INSERT INTO bar_bill_settlements(id,bill_id,settle_channel,settlement_amount_cent,merchant_id,settled_at) VALUES(?,?,'pos',60000,?,?)",id(),bill,merchant,Timestamp.from(end.plusSeconds(1)));
        jdbc.update("UPDATE bar_bills SET paid_amount_cent=120000,settlement_status='settled',payment_status='paid' WHERE id=?",bill);
        assertThat(revenue.completedCollections(merchant,start,end.plusSeconds(10))).singleElement().satisfies(r->{
            assertThat(r.status()).isEqualTo("confirmed");assertThat(r.goodsRevenueCent()).isEqualTo(100000);
            assertThat(r.tipCent()).isEqualTo(20000);assertThat(r.completedAt()).isEqualTo(end.plusSeconds(1));
        });
        assertThat(close()).singleElement().extracting(BarPayrollRequests.MonthRun::payableTipCent).isEqualTo(20000L);
    }
    @Test void refundRequiresRealSourcesExactTotalsAndCumulativeAvailableComponents() {
        var b=bars.findBillById(bill).orElseThrow();
        assertThatThrownBy(()->refunds.validate(b,List.of(new BarBillAdjustmentRequest.RefundLine(bill,"goods",100)),200))
            .isInstanceOf(ApiException.class).hasMessageContaining("合计");
        assertThatThrownBy(()->refunds.validate(b,List.of(new BarBillAdjustmentRequest.RefundLine("foreign","tip",100)),100))
            .isInstanceOf(ApiException.class).hasMessageContaining("本收款账单");
        assertThatThrownBy(()->refunds.validate(b,List.of(new BarBillAdjustmentRequest.RefundLine(bill,"tip",20001)),20001))
            .isInstanceOf(ApiException.class).hasMessageContaining("累计超过");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_payroll_refund_lines WHERE source_bill_id=?",Integer.class,bill)).isZero();
    }
    @Test void realRefundPaymentRecalculatesOnlyApprovedComponentsWithImmutableDeltas() {
        close();
        String adjustment=id(),ap=id();
        jdbc.update("INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,unpaid_amount_cent,due_date,status,created_at,updated_at) VALUES(?,?,'payable',?,'refund',?,5000,5000,'2026-09-02','open',now(),now())",
            ap,ap,merchant,adjustment);
        var row=bars.saveBillAdjustment(new BarBillAdjustmentRecord(adjustment,bill,"refund",5000,100000,97000,"真实商品30元小费20元",actor,true,"applied",id(),List.of(),ap,"{}",end,end));
        var lines=refunds.validate(bars.findBillById(bill).orElseThrow(),List.of(
            new BarBillAdjustmentRequest.RefundLine(bill,"goods",3000),
            new BarBillAdjustmentRequest.RefundLine(bill,"tip",2000)),5000);
        refunds.record(row,lines);
        jdbc.update("UPDATE bar_bills SET payable_amount_cent=97000,tip_amount_cent=18000 WHERE id=?",bill);
        assertThat(close()).singleElement().satisfies(r->{assertThat(r.status()).isEqualTo("pending");assertThat(r.unresolvedSources()).anyMatch(s->s.contains("实际全部付成"));});
        assertThat(revenue.completedGoodsRefunds(merchant,start,end.plusSeconds(10))).isEmpty();
        jdbc.update("INSERT INTO ar_ap_payments(id,ar_ap_item_id,amount_cent,payment_method,payment_time,created_at) VALUES(?,?,5000,'bank_transfer',?,now())",id(),ap,Timestamp.from(end.plusSeconds(2)));
        assertThat(close()).singleElement().satisfies(r->{
            assertThat(r.payableTipCent()).isEqualTo(18000);assertThat(r.allocations()).extracting(BarPayrollAllocation::amountCent).containsExactlyInAnyOrder(-500L,-1500L);
        });
        assertThat(allocations.confirmedAllocations(first,"2026-09").stream().mapToLong(BarPayrollAllocation::amountCent).sum()).isEqualTo(4500);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_payroll_runs WHERE merchant_id=?",Integer.class,merchant)).isEqualTo(2);
        assertThat(revenue.completedGoodsRefunds(merchant,start,end.plusSeconds(10))).singleElement().satisfies(r->{
            assertThat(r.amountCent()).isEqualTo(3000);assertThat(r.completedAt()).isEqualTo(end.plusSeconds(2));
        });
    }

    /** 把 first 挂到另一人员所属主体（BSM L1143「由该酒吧按月拨付给人员所属主体」），second 仍与酒吧结算主体同主体。 */
    String crossEntityPayerForFirst() {
        String payerB="bpe2_"+id();
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'人员所属主体','management_company','active',now(),now())",payerB);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)",payerB,project);
        jdbc.update("UPDATE staff_profiles SET company_id=? WHERE user_id=?",payerB,first);
        jdbc.update("UPDATE bar_service_responsibilities SET payer_company_id=? WHERE session_id=? AND user_id=?",payerB,session,first);
        return payerB;
    }
    List<Map<String,Object>> transferItems() {
        return jdbc.queryForList("SELECT * FROM ar_ap_items WHERE source_type='bar_payroll_transfer' AND merchant_id=? ORDER BY created_at,id",merchant);
    }
    static long number(Map<String,Object> row,String key){return ((Number)row.get(key)).longValue();}
    @Test void crossEntityAllocationCreatesOnePayablePerAllocationAndSameEntityCreatesNoDebt() {
        String payerB=crossEntityPayerForFirst();
        close();
        String allocation=allocations.confirmedAllocations(first,"2026-09").getFirst().id();
        var items=transferItems();
        assertThat(items).singleElement().satisfies(i->{
            assertThat(i.get("direction")).isEqualTo("payable");assertThat(i.get("counterparty_id")).isEqualTo(payerB);
            assertThat(number(i,"amount_cent")).isEqualTo(5000);assertThat(number(i,"unpaid_amount_cent")).isEqualTo(5000);
            assertThat(i.get("project_id")).isEqualTo(project);assertThat(i.get("accounting_period")).isEqualTo("2026-09");
            assertThat(((java.sql.Date)i.get("due_date")).toLocalDate()).isEqualTo(LocalDate.of(2026,10,5));
            assertThat(i.get("status")).isEqualTo("pending_payment");assertThat(i.get("source_id")).isEqualTo(allocation);
        });
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM ar_ap_item_sources WHERE ar_ap_item_id=?",Long.class,items.getFirst().get("id"))).isEqualTo(5000);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE source_type='bar_payroll_transfer' AND counterparty_id=?",Integer.class,company)).isZero();
        close();
        assertThat(transferItems()).hasSize(1);
    }
    @Test void crossEntityRefundDeltaCreatesSeparateReceivableWithoutNetting() {
        String payerB=crossEntityPayerForFirst();
        close();
        String adjustment=id(),ap=id();
        jdbc.update("INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,unpaid_amount_cent,due_date,status,created_at,updated_at) VALUES(?,?,'payable',?,'refund',?,5000,5000,'2026-09-02','open',now(),now())",
            ap,ap,merchant,adjustment);
        var row=bars.saveBillAdjustment(new BarBillAdjustmentRecord(adjustment,bill,"refund",5000,100000,97000,"真实商品30元小费20元",actor,true,"applied",id(),List.of(),ap,"{}",end,end));
        refunds.record(row,refunds.validate(bars.findBillById(bill).orElseThrow(),List.of(
            new BarBillAdjustmentRequest.RefundLine(bill,"goods",3000),
            new BarBillAdjustmentRequest.RefundLine(bill,"tip",2000)),5000));
        jdbc.update("UPDATE bar_bills SET payable_amount_cent=97000,tip_amount_cent=18000 WHERE id=?",bill);
        jdbc.update("INSERT INTO ar_ap_payments(id,ar_ap_item_id,amount_cent,payment_method,payment_time,created_at) VALUES(?,?,5000,'bank_transfer',?,now())",id(),ap,Timestamp.from(end.plusSeconds(2)));
        close();
        String refundAllocation=allocations.confirmedAllocations(first,"2026-09").stream().filter(a->a.amountCent()==-500).map(BarPayrollAllocation::id).findFirst().orElseThrow();
        var items=transferItems();
        assertThat(items).filteredOn(i->"receivable".equals(i.get("direction"))).singleElement().satisfies(i->{
            assertThat(i.get("counterparty_id")).isEqualTo(payerB);assertThat(number(i,"amount_cent")).isEqualTo(500);
            assertThat(i.get("status")).isEqualTo("pending_collection");assertThat(i.get("source_id")).isEqualTo(refundAllocation);
        });
        assertThat(items).filteredOn(i->"payable".equals(i.get("direction"))).singleElement().satisfies(i->{
            assertThat(number(i,"amount_cent")).isEqualTo(5000);assertThat(number(i,"unpaid_amount_cent")).isEqualTo(5000);assertThat(i.get("status")).isEqualTo("pending_payment");
        });
        assertThat(items).hasSize(2);
    }

    /**
     * 第十七批（操作员裁决补充第 1 条「全部自动冲回（推荐）：作废时系统自动把已收款项登记为原路待退（走现有退款链）……」）：收款账单经主管
     * 「作废并冲正」后，本单小费、服务费分项按全额退款处理（等同退款链对分项的冲减），不再以「原收款账单尚未完成真实收款」挂待结；作废前已办过的
     * 分项退款不重复冲减；月结出负额分配冲回已确认分配，跨主体的负额单列应收、不轧差（BSM L1143）。请求体按前端蛇形键。
     */
    @Test void voidedCollectionBillReversesServiceIncomeAllocationsAsFullRefund() throws Exception {
        String payerB=crossEntityPayerForFirst();
        close();
        String adjustment=id(),ap=id();
        jdbc.update("INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,unpaid_amount_cent,due_date,status,created_at,updated_at) VALUES(?,?,'payable',?,'refund',?,5000,5000,'2026-09-02','open',now(),now())",
            ap,ap,merchant,adjustment);
        var row=bars.saveBillAdjustment(new BarBillAdjustmentRecord(adjustment,bill,"refund",5000,100000,97000,"真实商品30元小费20元",actor,true,"applied",id(),List.of(),ap,"{}",end,end));
        refunds.record(row,refunds.validate(bars.findBillById(bill).orElseThrow(),List.of(
            new BarBillAdjustmentRequest.RefundLine(bill,"goods",3000),
            new BarBillAdjustmentRequest.RefundLine(bill,"tip",2000)),5000));
        jdbc.update("UPDATE bar_bills SET payable_amount_cent=97000,tip_amount_cent=18000 WHERE id=?",bill);
        jdbc.update("INSERT INTO ar_ap_payments(id,ar_ap_item_id,amount_cent,payment_method,payment_time,created_at) VALUES(?,?,5000,'bank_transfer',?,now())",id(),ap,Timestamp.from(end.plusSeconds(2)));
        close();

        String role=jdbc.queryForObject("SELECT role_id FROM user_roles WHERE user_id=? LIMIT 1",String.class,actor);
        for(String permission:List.of("bar.bill.adjust","bar.bill.anomaly.resolve"))
            jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,?)",role,permission);
        mvc.perform(post("/api/v1/bar/bills/"+bill+"/anomaly").with(web()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"reason\":\"客人称整单不是本桌消费\",\"idempotency_key\":\"ra_"+id()+"\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/bar/bills/"+bill+"/anomaly/resolve").with(web()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"resolution\":\"voided_reversed\",\"reason\":\"核实整单记错，作废并冲正\",\"idempotency_key\":\"rv_"+id()+"\"}"))
            .andExpect(status().isOk());
        as(actor,merchant);

        assertThat(close()).singleElement().satisfies(r->{
            assertThat(r.status()).isEqualTo("confirmed");assertThat(r.payableTipCent()).isZero();
            assertThat(r.allocations()).extracting(BarPayrollAllocation::amountCent).containsExactlyInAnyOrder(-4500L,-13500L);
        });
        for(String user:List.of(first,second))
            assertThat(allocations.confirmedAllocations(user,"2026-09").stream().mapToLong(BarPayrollAllocation::amountCent).sum()).isZero();
        assertThat(allocations.unresolvedAllocationSources(first,"2026-09")).isEmpty();
        assertThat(transferItems()).filteredOn(i->"receivable".equals(i.get("direction")))
            .allSatisfy(i->assertThat(i.get("counterparty_id")).isEqualTo(payerB))
            .extracting(i->number(i,"amount_cent")).containsExactlyInAnyOrder(500L,4500L);
    }
    /** 本用例经办人（同前端后台登录，终端 web）。 */
    RequestPostProcessor web() {
        AccessActor a=new AccessActor(actor,actor,"staff",project,merchant,"active");
        return request->{request.addHeader("X-Terminal-Type","web");return authentication(new UsernamePasswordAuthenticationToken(a,"test",List.of())).postProcessRequest(request);};
    }

    @Test void splitAndMergeCollectionsDeductOriginalTipOnlyOnce() {
        String split=id();
        jdbc.update("""
            INSERT INTO bar_bills(id,bill_no,order_id,bar_id,merchant_id,table_id,subtotal_amount_cent,discount_amount_cent,deposit_deduct_amount_cent,
                payable_amount_cent,paid_amount_cent,currency,payment_status,settlement_status,tip_amount_cent,created_at,updated_at)
            SELECT ?,?,order_id,bar_id,merchant_id,table_id,40000,0,0,40000,40000,'CNY','paid','settled',0,created_at,updated_at
            FROM bar_bills WHERE id=?
            """,split,split,bill);
        jdbc.update("UPDATE bar_bills SET subtotal_amount_cent=60000,payable_amount_cent=60000,paid_amount_cent=80000 WHERE id=?",bill);
        jdbc.update("UPDATE bar_bill_settlements SET settlement_amount_cent=80000 WHERE bill_id=?",bill);
        jdbc.update("INSERT INTO bar_bill_settlements(id,bill_id,settle_channel,settlement_amount_cent,merchant_id,settled_at) VALUES(?,?,'pos',40000,?,?)",id(),split,merchant,Timestamp.from(end));
        sources.recordSplit(bill,split);
        var two=revenue.completedCollections(merchant,start,end.plusSeconds(1));
        assertThat(two).hasSize(2);assertThat(two.stream().mapToLong(r->r.goodsRevenueCent()).sum()).isEqualTo(100000);
        assertThat(two.stream().mapToLong(BarRevenueSourceService.CollectionRevenue::tipCent).sum()).isEqualTo(20000);
        assertThatThrownBy(()->refunds.validate(bars.findBillById(split).orElseThrow(),
            List.of(new BarBillAdjustmentRequest.RefundLine(bill,"tip",1)),1)).isInstanceOf(ApiException.class);
        sources.recordMerge(bill,split);
        jdbc.update("UPDATE bar_bills SET subtotal_amount_cent=100000,payable_amount_cent=100000,paid_amount_cent=120000 WHERE id=?",bill);
        jdbc.update("UPDATE bar_bill_settlements SET settlement_amount_cent=120000 WHERE bill_id=?",bill);
        jdbc.update("UPDATE bar_bill_settlements SET reversed=true WHERE bill_id=?",split);
        jdbc.update("UPDATE bar_bills SET paid_amount_cent=0,payable_amount_cent=0,settlement_status='merged',payment_status='merged' WHERE id=?",split);
        assertThat(revenue.completedCollections(merchant,start,end.plusSeconds(1))).singleElement().satisfies(r->{
            assertThat(r.goodsRevenueCent()).isEqualTo(100000);assertThat(r.tipCent()).isEqualTo(20000);
        });
    }
    @Test void cashCanCoverGoodsInMixedPaymentButNeverCompanyCollectedTip() {
        var b=bars.findBillById(bill).orElseThrow();
        assertThatCode(()->sources.validateCashCollection(b,100000)).doesNotThrowAnyException();
        assertThatThrownBy(()->sources.validateCashCollection(b,120000)).isInstanceOfSatisfying(ApiException.class,
            e->assertThat(e.code()).isEqualTo("BAR_CASH_TIP_DIRECT_TO_STAFF"));
    }

    @Test void mergedOriginalGuestFollowsActualTargetTableResponsibilityThroughHandoverAndTransfer() {
        String targetTable=id(),nextTable=id(),primary=id();
        for(String t:List.of(targetTable,nextTable))jdbc.update("""
            INSERT INTO bar_tables(id,bar_id,merchant_id,table_code,table_name,table_type,capacity,allowed_pass_free_guest_count,min_consumption_cent,currency,status,created_at,updated_at,space_id)
            SELECT ?,bar_id,merchant_id,?,'目标卡座','card_seat',4,0,0,'CNY','occupied',now(),now(),space_id FROM bar_tables WHERE id=?
            """,t,t,table);
        jdbc.update("DELETE FROM bar_service_responsibilities WHERE session_id=?",session);
        jdbc.update("UPDATE bar_table_sessions SET status='merged',closed_at=?,service_staff_id=?,source_session_id=? WHERE id=?",Timestamp.from(start.plusSeconds(2*3600)),first,primary,session);
        jdbc.update("INSERT INTO bar_table_sessions(id,bar_id,merchant_id,business_date,table_id,service_staff_id,status,opened_at,created_at,updated_at) VALUES(?,?,?,'2026-09-01',?,?,'open',?,?,?)",
            primary,merchant,merchant,targetTable,second,Timestamp.from(start),Timestamp.from(start),Timestamp.from(start));
        // 原台前两小时由甲承担；并入后跟随主台乙，原场次编号不变。
        var old=bars.findTableSessionById(session).orElseThrow();var main=bars.findTableSessionById(primary).orElseThrow();
        responsibilities.recordOpened(old);responsibilities.recordOpened(main);
        responsibilities.recordMerge(main,old,start.plusSeconds(2*3600),"fixture-merge");
        responsibilities.recordStaffHandover(primary,second,actor,start.plusSeconds(4*3600),"fixture-handover","实际接班");
        var before=bars.findTableSessionById(primary).orElseThrow();
        jdbc.update("UPDATE bar_table_sessions SET table_id=?,service_staff_id=? WHERE id=?",nextTable,first,primary);
        responsibilities.recordTransfer(before,bars.findTableSessionById(primary).orElseThrow(),start.plusSeconds(6*3600),"fixture-transfer");
        jdbc.update("UPDATE bar_service_responsibilities SET ended_at=? WHERE session_id IN (?,?) AND ended_at IS NULL",Timestamp.from(end),session,primary);
        jdbc.update("UPDATE bar_table_sessions SET closed_at=? WHERE id IN (?,?)",Timestamp.from(end),session,primary);
        var chain=responsibilities.responsibilityRows(session);
        assertThat(chain).extracting(BarPayrollRequests.Responsibility::tableId).containsExactly(table,targetTable,targetTable,nextTable);
        assertThat(chain).extracting(BarPayrollRequests.Responsibility::userId).containsExactly(first,second,actor,first);
        assertThat(close()).singleElement().extracting(BarPayrollRequests.MonthRun::status).isEqualTo("confirmed");
        assertThat(allocations.confirmedAllocations(first,"2026-09")).singleElement().extracting(BarPayrollAllocation::amountCent).isEqualTo(10000L);
        assertThat(allocations.confirmedAllocations(second,"2026-09")).singleElement().extracting(BarPayrollAllocation::amountCent).isEqualTo(5000L);
        assertThat(allocations.confirmedAllocations(actor,"2026-09")).singleElement().extracting(BarPayrollAllocation::amountCent).isEqualTo(5000L);
    }

    String shift(String user,String point,Instant from,Instant to) {
        String value=id();
        jdbc.update("INSERT INTO staff_shift_schedules(id,user_id,work_date,start_at,end_at,point_space_ids,created_by,project_id) VALUES(?,?,'2026-09-01',?,?,jsonb_build_array(?::text),?,?)",
            value,user,Timestamp.from(from),Timestamp.from(to),point,actor,project);
        return value;
    }
    String point(){return jdbc.queryForObject("SELECT space_id FROM bar_tables WHERE id=?",String.class,table);}
    void cover(String original,String replacement,Instant from,Instant to) {
        jdbc.update("INSERT INTO staff_shift_covers(id,shift_id,original_user_id,cover_user_id,arranged_by,reason,start_at,end_at,handover_note,cover_shift_id,source_updated_at) VALUES(?,?,?,?,?,'真实顶班',?,?,'实际交接',?,now())",
            id(),original,first,second,actor,Timestamp.from(from),Timestamp.from(to),replacement);
    }
    @Test void regionalTipUsesPublishedScheduleTableAndOriginalPayerSnapshots() {
        jdbc.update("UPDATE bar_tables SET table_type='bar_counter' WHERE id=?",table);
        shift(first,point(),start,start.plusSeconds(2*3600));
        shift(second,point(),start.plusSeconds(2*3600),end);
        jdbc.update("DELETE FROM bar_payroll_bill_components WHERE source_bill_id=?",bill);
        sources.recordComponent(bars.findBillById(bill).orElseThrow(),"tip",20000,id());
        assertThat(close()).singleElement().extracting(BarPayrollRequests.MonthRun::status).isEqualTo("confirmed");
        assertThat(allocations.confirmedAllocations(first,"2026-09")).singleElement().extracting(BarPayrollAllocation::amountCent).isEqualTo(10000L);
        assertThat(allocations.confirmedAllocations(second,"2026-09")).singleElement().extracting(BarPayrollAllocation::amountCent).isEqualTo(10000L);
    }
    @Test void approvedServiceFeeUsesActualCoverFractionsAndWithheldRevenue() throws Exception {
        approvedServiceFeeSource();
        assertThat(revenue.withheldServiceFees(merchant,LocalDate.parse("2026-09-01"),LocalDate.parse("2026-10-01")))
            .singleElement().satisfies(r->{assertThat(r.status()).isEqualTo("pending");assertThat(r.amountCent()).isNull();});
        assertThat(close()).singleElement().satisfies(r->{
            assertThat(r.status()).isEqualTo("confirmed");assertThat(r.withheldServiceFeeCent()).isEqualTo(2000);assertThat(r.payableServiceFeeCent()).isEqualTo(8000);
        });
        assertThat(allocations.confirmedAllocations(first,"2026-09")).singleElement().extracting(BarPayrollAllocation::amountCent).isEqualTo(2000L);
        assertThat(allocations.confirmedAllocations(second,"2026-09")).singleElement().extracting(BarPayrollAllocation::amountCent).isEqualTo(6000L);
        assertThat(revenue.withheldServiceFees(merchant,LocalDate.parse("2026-09-01"),LocalDate.parse("2026-10-01")))
            .singleElement().satisfies(r->{assertThat(r.status()).isEqualTo("confirmed");assertThat(r.amountCent()).isEqualTo(2000);});
    }
    /** ROOT-04-R4：跨期月结确认的服务费扣留按原营业日计酒吧收入，月结时登记该原营业日待看板重算（实施台账 ROOT-04「跨期月结、退费后的原经营日」）。 */
    @Test void monthCloseMarksOriginalServiceFeeBusinessDates() throws Exception {
        approvedServiceFeeSource();
        jdbc.update("DELETE FROM analytics_revenue_refresh_dates WHERE business_date='2026-09-01'");
        assertThat(close()).singleElement().extracting(BarPayrollRequests.MonthRun::status).isEqualTo("confirmed");
        assertThat(jdbc.queryForList("SELECT business_date FROM analytics_revenue_refresh_dates",LocalDate.class)).contains(LocalDate.parse("2026-09-01"));
    }
    void approvedServiceFeeSource() throws Exception {
        String original=shift(first,point(),start,end);
        String replacement=shift(second,point(),start.plusSeconds(2*3600),end);
        cover(original,replacement,start.plusSeconds(2*3600),end);
        var charge=new BarPayrollRequests.RuleValues(1000,0,2000,null,null,null,List.of());
        var allocation=new BarPayrollRequests.RuleValues(null,null,null,2000,0,5000,List.of(
            new BarPayrollRequests.Participant(first,1,"actual_shift_fraction",company),
            new BarPayrollRequests.Participant(second,1,"actual_shift_fraction",company)));
        for(String kind:List.of("charge","allocation"))jdbc.update("""
            INSERT INTO bar_payroll_rules(id,rule_kind,project_id,merchant_id,effective_from,version,rule_json,reason,status,platform_company_id,created_by)
            VALUES(?,?,?,?,'2026-09-01','V0.01',?::jsonb,'双方已确认并批准的分配原件','approved',?,?)
            """,id(),kind,project,"charge".equals(kind)?null:merchant,json.writeValueAsString("charge".equals(kind)?charge:allocation),company,actor);
        jdbc.update("DELETE FROM bar_payroll_bill_components WHERE source_bill_id=?",bill);
        jdbc.update("UPDATE bar_bills SET payable_amount_cent=110000,tip_amount_cent=0,paid_amount_cent=110000 WHERE id=?",bill);
        jdbc.update("UPDATE bar_bill_settlements SET settlement_amount_cent=110000 WHERE bill_id=?",bill);
        sources.recordComponent(bars.findBillById(bill).orElseThrow(),"service_fee",10000,id());
    }
    @Test void transferRequiresPublishedTargetDutyAndRecognizesRealCoverInsteadOfOriginalWorker() {
        String original=shift(first,point(),start,end);
        String replacement=shift(second,point(),start.plusSeconds(2*3600),end);
        cover(original,replacement,start.plusSeconds(2*3600),end);
        assertThatCode(()->responsibilities.requireTransferStaff(table,first,start.plusSeconds(3600))).doesNotThrowAnyException();
        assertThatCode(()->responsibilities.requireTransferStaff(table,second,start.plusSeconds(3*3600))).doesNotThrowAnyException();
        assertThatThrownBy(()->responsibilities.requireTransferStaff(table,first,start.plusSeconds(3*3600)))
            .isInstanceOf(ApiException.class).hasMessageContaining("真实顶班");
        // 只有待审修订、没有发布班次，不能成为目标点位当班人。
        jdbc.update("INSERT INTO staff_shift_revisions(id,shift_id,review_id,after_value,initiated_by) VALUES(?,?,?,jsonb_build_object('user_id',?::text),?)",id(),id(),id(),actor,actor);
        assertThatThrownBy(()->responsibilities.requireTransferStaff(table,actor,start.plusSeconds(3600)))
            .isInstanceOf(ApiException.class);
        assertThatThrownBy(()->responsibilities.requireTransferStaff(table,second,end)).isInstanceOf(ApiException.class);
    }
    @Test void transferRequestMissingTargetWorkerCannotMutateSessionOrTables() {
        String target=id();
        jdbc.update("""
            INSERT INTO bar_tables(id,bar_id,merchant_id,table_code,table_name,table_type,capacity,allowed_pass_free_guest_count,min_consumption_cent,currency,status,created_at,updated_at,space_id)
            SELECT ?,bar_id,merchant_id,?,'目标卡座','card_seat',4,0,0,'CNY','active',now(),now(),space_id FROM bar_tables WHERE id=?
            """,target,target,table);
        jdbc.update("UPDATE bar_table_sessions SET status='open',closed_at=NULL WHERE id=?",session);
        var before=bars.findTableSessionById(session).orElseThrow();
        assertThatThrownBy(()->sessions.transferTableSession(session,new BarTableTransferRequest(id(),"真实转台",List.of(),target,"真实转台",actor,before.sessionVersion())))
            .isInstanceOf(ApiException.class);
        assertThat(bars.findTableSessionById(session).orElseThrow()).isEqualTo(before);
        assertThat(bars.findTableById(target).orElseThrow().status()).isEqualTo("active");
        Instant now=Instant.now();shift(actor,point(),now.minusSeconds(3600),now.plusSeconds(3600));
        sessions.transferTableSession(session,new BarTableTransferRequest(id(),"真实转台",List.of(),target,"真实转台",actor,before.sessionVersion(),actor));
        var after=bars.findTableSessionById(session).orElseThrow();
        assertThat(after.tableId()).isEqualTo(target);assertThat(after.serviceStaffId()).isEqualTo(actor);
        assertThat(responsibilities.responsibilityRows(session).getLast().userId()).isEqualTo(actor);
    }

    /**
     * 第十八批 V 段第 159 条 159-R9：服务费与小费月结定格闸。依据：ENUM:190「非最终快照的数据一律不得进入结算、分红、正式提成计算和对账关账」；
     * BSM:1076「归属按服务费收取所属账期认定，以该账期月结定格时确定的应发数与扣留数为准」；第十八批裁决 35「酒吧逐店逐日查」。
     * 酒吧改为 2026-01-01 建档（随测试事务回滚）；9-01 账单所属营业日由财务 BP 经真实接口生成日结、标除「当日订单与核销记录」以外四类，
     * 另一张 9-02 账单当天五类标齐。月结按前端蛇形键走真实接口与授权链：9-01 账单待处理、不写原件，9-02 账单当场定格；
     * 补标后换新幂等键再办月结，9-01 账单按不加闸时的 50/150 定格。修复前不判最终，9-01 账单第一次就定格，第一条断言即红。
     */
    @Test void serviceIncomeCloseWaitsPerBillUntilOrdersOfItsBusinessDayAreFinal() throws Exception {
        jdbc.update("UPDATE merchants SET created_at=TIMESTAMPTZ '2026-01-01 00:00:00+08' WHERE id=?",merchant);
        var fixture=new StaffRequestFixture(jdbc);
        var payroll=fixture.staff("bpfin_pay_"+id(),"staff",project,merchant,"bar.payroll.manage");
        var financeBp=fixture.staff("bpfin_bp_"+id(),"staff",project,null,"finance.daily_close.lock","finance.daily_close.manage");
        String dailyClose=JsonPath.read(body(mvc.perform(post("/api/v1/finance/daily-closes").with(financeBp).contentType(MediaType.APPLICATION_JSON)
            .content("{\"idempotency_key\":\"bf-gen-"+id()+"\",\"business_date\":\"2026-09-01\",\"scope_type\":\"bar\",\"scope_id\":\""+merchant+"\"}"))
            .andExpect(status().isOk())),"$.data.id");
        for(String category:List.of("refunds_reversals","production_handover","attendance","anomaly_resolutions"))finalMark(financeBp,dailyClose,category);
        String next=billOn(LocalDate.parse("2026-09-02"));
        markRangeFinal(jdbc,"bar",merchant,LocalDate.parse("2026-09-02"),LocalDate.parse("2026-09-02"));

        List<Map<String,Object>> runs=closeVia(payroll);
        assertThat(runs).filteredOn(r->"pending".equals(r.get("status"))).singleElement()
            .satisfies(r->assertThat((List<?>)r.get("unresolved_sources")).singleElement().asString()
                .startsWith(bill+":").contains("尚未标记为最终").contains("2026-09-01 测试酒吧：当日订单与核销记录"));
        assertThat(runs).filteredOn(r->"confirmed".equals(r.get("status"))).hasSize(1);
        assertThat(runCount(bill)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_payroll_allocations WHERE source_bill_ids @> ?::jsonb",Integer.class,"[\""+bill+"\"]")).isZero();
        assertThat(runCount(next)).isEqualTo(1);

        finalMark(financeBp,dailyClose,"orders_verifications");
        assertThat(closeVia(payroll)).hasSize(2).extracting(r->r.get("status")).containsOnly("confirmed");
        assertThat(runCount(bill)).isEqualTo(1);
        assertThat(allocations.confirmedAllocations(first,"2026-09")).filteredOn(a->a.sourceBillIds().contains(bill)).singleElement()
            .extracting(BarPayrollAllocation::amountCent).isEqualTo(5000L);
        assertThat(allocations.confirmedAllocations(second,"2026-09")).filteredOn(a->a.sourceBillIds().contains(bill)).singleElement()
            .extracting(BarPayrollAllocation::amountCent).isEqualTo(15000L);
    }
    /** 159-R9：决策台账 §25 Q32「不要求无关五类数据全最终」——9-01 只缺「当日考勤记录」时照常定格（修复前后都绿，防闸收得过宽）。 */
    @Test void serviceIncomeCloseDoesNotWaitForUnrelatedAttendance() throws Exception {
        jdbc.update("UPDATE merchants SET created_at=TIMESTAMPTZ '2026-01-01 00:00:00+08' WHERE id=?",merchant);
        closeWithMarks(jdbc,"bar",merchant,LocalDate.parse("2026-09-01"),
            List.of("orders_verifications","refunds_reversals","production_handover","anomaly_resolutions"));
        var payroll=new StaffRequestFixture(jdbc).staff("bpfin_pay_"+id(),"staff",project,merchant,"bar.payroll.manage");
        assertThat(closeVia(payroll)).singleElement().satisfies(r->assertThat(r.get("status")).isEqualTo("confirmed"));
        assertThat(runCount(bill)).isEqualTo(1);
    }
    List<Map<String,Object>> closeVia(RequestPostProcessor caller) throws Exception {
        return JsonPath.read(body(mvc.perform(post("/api/v1/bar/payroll-runs/close").with(caller).contentType(MediaType.APPLICATION_JSON)
            .content("{\"idempotency_key\":\"bf-close-"+id()+"\",\"project_id\":\""+project+"\",\"merchant_id\":\""+merchant+"\",\"month\":\"2026-09\"}"))
            .andExpect(status().isOk())),"$.data");
    }
    void finalMark(RequestPostProcessor caller,String dailyClose,String category) throws Exception {
        mvc.perform(post("/api/v1/finance/daily-closes/"+dailyClose+"/final-marks").with(caller).contentType(MediaType.APPLICATION_JSON)
            .content("{\"idempotency_key\":\"bf-"+category+"-"+id()+"\",\"category\":\""+category+"\"}")).andExpect(status().isOk());
    }
    static String body(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }
    int runCount(String sourceBill) {
        return jdbc.queryForObject("SELECT count(*) FROM bar_payroll_runs WHERE source_snapshot->>'source_bill_id'=?",Integer.class,sourceBill);
    }
    /** 159-R9：同酒吧同卡座另一场次（营业日 day）一张已真实收款的账单，小费 20000 分，全程由 first 负责交接。 */
    String billOn(LocalDate day) {
        String s=id(),sess="bps2_"+s,order="bpo2_"+s,b="bpb2_"+s;
        Instant from=day.atTime(12,0).toInstant(ZoneOffset.UTC),to=from.plusSeconds(8*3600);
        jdbc.update("INSERT INTO bar_table_sessions(id,bar_id,merchant_id,business_date,table_id,service_staff_id,status,opened_at,closed_at,created_at,updated_at) VALUES(?,?,?,?,?,?,'closed',?,?,?,?)",
            sess,merchant,merchant,day,table,first,Timestamp.from(from),Timestamp.from(to),Timestamp.from(from),Timestamp.from(to));
        jdbc.update("INSERT INTO bar_orders(id,bar_id,merchant_id,table_id,order_source,status,total_amount_cent,currency,created_at,updated_at) VALUES(?,?,?,?,'staff','created',100000,'CNY',?,?)",
            order,merchant,merchant,table,Timestamp.from(from),Timestamp.from(to));
        jdbc.update("INSERT INTO bar_bills(id,bill_no,order_id,bar_id,merchant_id,table_id,subtotal_amount_cent,discount_amount_cent,deposit_deduct_amount_cent,payable_amount_cent,paid_amount_cent,currency,payment_status,settlement_status,tip_amount_cent,created_at,updated_at) VALUES(?,?,?,?,?,?,100000,0,0,100000,120000,'CNY','paid','settled',20000,?,?)",
            b,"B"+s,order,merchant,merchant,table,Timestamp.from(from),Timestamp.from(to));
        jdbc.update("INSERT INTO bar_bill_settlements(id,bill_id,settle_channel,settlement_amount_cent,merchant_id,settled_at) VALUES(?,?,'aggregate_pay',120000,?,?)","pay_"+s,b,merchant,Timestamp.from(to));
        jdbc.update("INSERT INTO bar_payroll_bill_components(id,source_bill_id,session_id,source_table_id,project_id,merchant_id,business_date,component_type,amount_cent,original_base_cent,source_event_id,source_snapshot,collection_bill_id) VALUES(?,?,?,?,?,?,?,'tip',20000,20000,?,'[]'::jsonb,?)",
            "c_"+s,b,sess,table,project,merchant,day,"event_"+s,b);
        jdbc.update("INSERT INTO bar_payroll_collection_links(source_bill_id,collection_bill_id) VALUES(?,?)",b,b);
        jdbc.update("INSERT INTO bar_service_responsibilities(id,session_id,table_id,project_id,merchant_id,user_id,payer_company_id,started_at,ended_at,source_id,recorded_by,reason) VALUES(?,?,?,?,?,?,?,?,?,?,?,'真实交接')",
            id(),sess,table,project,merchant,first,company,Timestamp.from(from),Timestamp.from(to),id(),actor);
        return b;
    }

    @Test void forgedApprovalEventCannotActivateRule() {
        String rule="r_"+id(),review="rv_"+id();
        jdbc.update("INSERT INTO bar_payroll_rules(id,rule_kind,project_id,merchant_id,effective_from,version,rule_json,reason,status,platform_company_id,review_id,created_by) VALUES(?,'allocation',?,?,'2026-09-01','V0.01','{}'::jsonb,'真实审批前保持待审','pending_review',?,?,?)",
            rule,project,merchant,company,review,actor);
        rules.decided(new SupervisorReviewDecisionRecord(review,"fixture","bar.payroll_rule.adjust","bar_payroll_rule",rule,false,"approved",1,1,"approve","伪造事件",List.of(),Instant.now(),actor,null,null));
        assertThat(jdbc.queryForObject("SELECT status FROM bar_payroll_rules WHERE id=?",String.class,rule)).isEqualTo("pending_review");
    }
}
