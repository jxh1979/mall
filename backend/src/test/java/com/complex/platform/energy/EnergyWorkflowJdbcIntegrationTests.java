package com.complex.platform.energy;

// 文件版本：V0.02
import static org.assertj.core.api.Assertions.*;
import com.complex.platform.access.*;
import com.complex.platform.analytics.AnalyticsRepository;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.energy.dto.*;
import com.complex.platform.evidence.*;
import com.complex.platform.governance.review.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties={
    "spring.datasource.url=${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
    "spring.datasource.username=${COMPLEX_DATABASE_USERNAME:complex}",
    "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class EnergyWorkflowJdbcIntegrationTests {
    @Autowired EnergyService service;
    @Autowired EnergyRepository repository;
    @Autowired EnergyWorkflowRepository workflow;
    @Autowired EvidenceEventService evidence;
    @Autowired AccessRepository access;
    @Autowired SupervisorReviewService reviews;
    @Autowired SupervisorReviewRepository reviewRepository;
    @Autowired AnalyticsRepository analytics;
    @Autowired com.complex.platform.finance.FinanceService finance;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @Autowired EvidenceEventRepository evidenceRepository;
    @Autowired org.springframework.web.context.WebApplicationContext webContext;
    String project, merchant, contract, reader, verifier, supervisor, suffix;
    static final String PERIOD="2026-06";
    static final Instant OPEN=Instant.parse("2026-06-01T00:00:00Z"), MID=Instant.parse("2026-06-15T00:00:00Z"), END=Instant.parse("2026-06-30T00:00:00Z");
    static final byte[] PNG=Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jZKsAAAAASUVORK5CYII=");

    @BeforeEach void seed(){
        suffix=UUID.randomUUID().toString().substring(0,8);project="energy_p_"+suffix;merchant="energy_m_"+suffix;contract="energy_c_"+suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex',?,'active',now(),now())",project,"能源测试项目");
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,created_at,updated_at) VALUES(?,?,'bar','active',now(),now())",merchant,"能源测试商户");
        jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,lifecycle_status,project_id,energy_fee_basis,rent_terms,created_at,updated_at) VALUES(?,?,'subj_1','subj_1','2026-01-01','2026-12-31','effective',?,'by_contract','{}',now(),now())",contract,merchant,project);
        reader=user("抄表员");verifier=user("复抄员");supervisor=user("能耗主管");
        for(String user:List.of(reader,verifier,supervisor)){
            String role="role_energy_"+UUID.randomUUID();
            access.createRole(new RoleRecord(role,"能源岗位"+user,null,"project","active",Instant.now(),List.of("energy.meter.manage","energy.reading.manage","energy.reading.approve","evidence.attachment.create","evidence.timeline.view","governance.supervisor_review.manage","governance.supervisor_review.approve"),null,false,List.of()));
            access.assignUserRole(user,role);
            access.saveStaffLevelAssignment(new StaffLevelAssignment(user,"lvl_m3_lower",null,"CNY",null,project,null,reader,Instant.now()));
        }
        reviewRepository.saveDirectSuperior(reader,supervisor,"main",reader,Instant.now());
        reviewRepository.saveDirectSuperior(verifier,supervisor,"main",reader,Instant.now());
        act(reader);
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();}

    @Test void replacementUsesActualOldTerminalAndNewOpeningThenCollectsOnlyDocumentedFees(){
        var old=meter("旧商户电表","merchant",null);
        initialize(old,"4900");String oldPhoto=photo(old);
        var replacement=service.replace(old,new EnergyWorkflowRequests.Replacement(key(),"新商户电表",PERIOD,n("5000"),n("100"),MID,"现场换表交接",List.of(oldPhoto)));
        var newer=replacement.newMeterId();
        act(verifier);service.reviewReading(replacement.oldReadingId(),new EnergyReadingReviewRequest(key(),null));
        act(reader);var finalRead=read(newer,"150",END);
        act(verifier);service.reviewReading(finalRead.id(),new EnergyReadingReviewRequest(key(),null));
        assertThat(finalRead.prevReadingValue()).isEqualByComparingTo("100");
        assertThat(finalRead.consumption()).isEqualByComparingTo("50");
        assertThat(repository.findReadingById(replacement.oldReadingId()).orElseThrow().consumption()).isEqualByComparingTo("100");
        service.recordCharge(charge(old,700,oldPhoto));service.recordCharge(charge(newer,350,photo(newer)));
        var bills=service.settleEnergyFee(PERIOD,key());assertThat(bills).hasSize(1);
        assertThat(bills.getFirst().amountCent()).isEqualTo(1050);
        assertThat(jdbc.queryForObject("SELECT due_date FROM ar_ap_items WHERE id=?",LocalDate.class,bills.getFirst().arApItemId())).isEqualTo(LocalDate.parse("2026-07-08"));
        assertThat(service.settleEnergyFee(PERIOD,key())).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ar_ap_items WHERE source_type='energy_fee' AND merchant_id=?",Integer.class,merchant)).isEqualTo(1);
        assertThatThrownBy(()->service.recordReading(new EnergyReadingRecordRequest(key(),old,PERIOD,n("5100"),null,"manual",null,END,List.of(oldPhoto),"旧表不得继续计量"))).isInstanceOf(ApiException.class);
        service.requireMeterAttachment(newer,oldPhoto);
    }

    @Test void unknownOpeningRequiresRealSecondPersonAndScopedSupervisorWithoutRewritingOriginal(){
        var meter=meter("缺初值电表","merchant",null);var raw=read(meter,"150",MID);String proof=photo(meter);
        assertThat(raw.status()).isEqualTo("abnormal");assertThat(raw.prevReadingValue()).isNull();assertThat(raw.consumption()).isNull();
        assertThatThrownBy(()->service.reviewReading(raw.id(),new EnergyReadingReviewRequest(key(),supervisor))).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.verify(raw.id(),verification("150","100",proof))).isInstanceOf(ApiException.class);
        act(verifier);
        assertThatThrownBy(()->service.verify(raw.id(),verification("150","100","missing_attachment"))).isInstanceOf(ApiException.class);
        var checked=service.verify(raw.id(),verification("150","100",proof));
        assertThat(repository.findReadingById(raw.id()).orElseThrow().status()).isEqualTo("abnormal");
        act(supervisor);approve(checked.reviewId());
        var unchanged=repository.findReadingById(raw.id()).orElseThrow();
        assertThat(unchanged.status()).isEqualTo("reviewed");assertThat(unchanged.prevReadingValue()).isNull();assertThat(unchanged.consumption()).isNull();assertThat(unchanged.readingValue()).isEqualByComparingTo("150");
        assertThat(workflow.effectiveConsumption(unchanged)).isEqualByComparingTo("50");
        act(reader);var next=read(meter,"200",END);
        assertThat(next.prevReadingValue()).isEqualByComparingTo("150");assertThat(next.consumption()).isEqualByComparingTo("50");
        var totals=analytics.electricityReadingDetail(OPEN,END.plusSeconds(1),200,0);
        assertThat(totals.rows()).filteredOn(r->raw.id().equals(r.get("id"))).singleElement().satisfies(r->assertThat((BigDecimal)r.get("consumption")).isEqualByComparingTo("50"));
        assertThat(totals.rows()).noneMatch(r->next.id().equals(r.get("id")));
    }

    @Test void anyUnresolvedMeterBlocksWholeMerchantPeriodAndPublicAreaCannotCreateCharges(){
        String clean=meter("正常电表","merchant",null), abnormal=meter("异常电表","merchant",null), common=meter("公共电表","public_area",null);
        initialize(clean,"100");var good=read(clean,"150",END);var bad=read(abnormal,"200",END);String proof=photo(abnormal);
        act(verifier);service.reviewReading(good.id(),new EnergyReadingReviewRequest(key(),null));service.recordCharge(charge(clean,500,photo(clean)));
        assertThat(service.settleEnergyFee(PERIOD,key())).isEmpty();
        assertThatThrownBy(()->service.recordCharge(charge(common,1000,photo(common)))).isInstanceOf(ApiException.class);
        var checked=service.verify(bad.id(),verification("200","100",proof));act(supervisor);approve(checked.reviewId());
        assertThat(service.settleEnergyFee(PERIOD,key())).isEmpty();
        service.recordCharge(charge(abnormal,600,proof));
        assertThat(service.settleEnergyFee(PERIOD,key())).singleElement().satisfies(x->assertThat(x.amountCent()).isEqualTo(1100));
    }

    @Test void missingActiveMeterReadingAlsoBlocksWholePeriod(){
        String ready=meter("齐单表","merchant",null), missing=meter("未抄表","merchant",null);
        initialize(ready,"10");initialize(missing,"20");var raw=read(ready,"30",END);
        act(verifier);service.reviewReading(raw.id(),new EnergyReadingReviewRequest(key(),null));service.recordCharge(charge(ready,200,photo(ready)));
        assertThat(service.settleEnergyFee(PERIOD,key())).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ar_ap_items WHERE source_type='energy_fee' AND merchant_id=?",Integer.class,merchant)).isZero();
    }


    @Test void ordinaryDeactivationCannotDiscardAnOpenedMetersUnmeasuredPeriod(){
        String meter=meter("已启用待抄表","merchant",null);initialize(meter,"100");
        assertThatThrownBy(()->service.updateMeter(meter,new EnergyMeterRequest(null,null,null,null,null,null,null,null,null,false,key())))
            .isInstanceOf(ApiException.class);
        assertThat(repository.findMeterById(meter).orElseThrow().active()).isTrue();
        assertThat(workflow.hasMissingReadings(merchant,project,PERIOD)).isTrue();
    }

    @Test void inactiveFlagAloneCannotEraseHistoricalMissingReading(){
        String meter=meter("计量义务保留表","merchant",null);initialize(meter,"100");
        jdbc.update("UPDATE energy_meters SET active=false WHERE id=?",meter);
        assertThat(workflow.hasMissingReadings(merchant,project,PERIOD)).isTrue();
    }

    @Test void staffReadingCannotClaimRemoteToSkipPhotographicEvidence(){
        String meter=meter("远程配置表的人工补录","merchant",null);initialize(meter,"100");
        assertThatThrownBy(()->service.recordReading(new EnergyReadingRecordRequest(key(),meter,PERIOD,n("150"),null,"remote",null,END,List.of(),null)))
            .isInstanceOf(ApiException.class);
        assertThat(repository.listReadings(PERIOD)).noneMatch(r->meter.equals(r.meterId()));
        assertThatThrownBy(()->service.recordReading(new EnergyReadingRecordRequest(key(),meter,PERIOD,n("150"),null,null,null,END,List.of(),null)))
            .isInstanceOf(ApiException.class);
    }

    @Test void generatedEnergyReceivableUsesItsActualProjectForVisibilityAndCollection(){
        String meter=meter("项目应收表","merchant",null);initialize(meter,"100");var raw=read(meter,"150",END);
        act(verifier);service.reviewReading(raw.id(),new EnergyReadingReviewRequest(key(),null));
        service.recordCharge(charge(meter,200,photo(meter)));String ar=service.settleEnergyFee(PERIOD,key()).getFirst().arApItemId();
        assertThat(jdbc.queryForObject("SELECT project_id FROM ar_ap_items WHERE id=?",String.class,ar)).isEqualTo(project);
        String role="energy_finance_"+suffix;
        access.createRole(new RoleRecord(role,"项目能源财务",null,"project","active",Instant.now(),List.of("finance.ar_ap.view","finance.payment.register"),null,false,List.of()));
        access.assignUserRole(supervisor,role);act(supervisor);
        assertThat(finance.listArApItems(null,1,100).items()).anyMatch(item->ar.equals(item.id()));
        String otherProject="energy_fin_other_"+suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','其他项目','active',now(),now())",otherProject);
        String outsider=user("其他项目财务");
        jdbc.update("UPDATE access_users SET organization_id=? WHERE id=?",otherProject,outsider);
        access.assignUserRole(outsider,role);act(outsider,otherProject);
        assertThat(finance.listArApItems(null,1,100).items()).noneMatch(item->ar.equals(item.id()));
        assertThatThrownBy(()->finance.registerArApPayment(ar,new com.complex.platform.finance.dto.ArApPaymentRequest(key(),"其他项目不准收款",List.of(),200L,"bank_transfer",END,"跨项目流水")))
            .isInstanceOf(ApiException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ar_ap_payments WHERE ar_ap_item_id=?",Integer.class,ar)).isZero();
        act(supervisor);
        var payment=new com.complex.platform.finance.dto.ArApPaymentRequest(key(),"本项目核实到账",List.of(),200L,"bank_transfer",END,"本项目流水");
        var paid=finance.registerArApPayment(ar,payment);
        assertThat(paid.item().projectId()).isEqualTo(project);
        assertThat(paid.item().originItemId()).isEqualTo(jdbc.queryForObject("SELECT origin_item_id FROM ar_ap_items WHERE id=?",String.class,ar));
        assertThat(paid.item().adjustmentId()).isEqualTo(jdbc.queryForObject("SELECT adjustment_id FROM ar_ap_items WHERE id=?",String.class,ar));
        assertThat(jdbc.queryForObject("SELECT project_id FROM ar_ap_items WHERE id=?",String.class,ar)).isEqualTo(project);
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM ar_ap_items WHERE id=?",Long.class,ar)).isEqualTo(200L);
        assertThat(jdbc.queryForObject("SELECT paid_amount_cent FROM ar_ap_items WHERE id=?",Long.class,ar)).isEqualTo(200L);
        assertThat(finance.listArApPayments(ar).items()).hasSize(1);
        act(outsider,otherProject);
        assertThatThrownBy(()->finance.listArApPayments(ar)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->finance.registerArApPayment(ar,payment)).isInstanceOf(ApiException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ar_ap_payments WHERE ar_ap_item_id=?",Integer.class,ar)).isEqualTo(1);
    }

    @Test void financeReadGrantExpandsOnlyApprovedProjectsWithSqlPaginationAndMerchantIntersection(){
        String other="energy_read_other_"+suffix,excluded="energy_read_excluded_"+suffix;
        for(String p:List.of(other,excluded))jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','范围项目','active',now(),now())",p);
        String ownItem="energy_scope_own_"+suffix,otherItem="energy_scope_other_"+suffix,foreignItem="energy_scope_hidden_"+suffix,nullItem="energy_scope_null_"+suffix;
        ledger(ownItem,project,merchant);ledger(otherItem,other,merchant);ledger(foreignItem,excluded,merchant);ledger(nullItem,null,merchant);
        assertThatThrownBy(()->finance.listArApItems(null,1,1)).isInstanceOf(ApiException.class);
        String role="energy_read_scope_"+suffix;
        access.createRole(new RoleRecord(role,"跨项目财务查看",null,"project","active",Instant.now(),List.of("finance.ar_ap.view"),null,false,List.of(project,other)));
        access.assignUserRole(reader,role);
        var first=finance.listArApItems("ENERGY-SCOPE-"+suffix,1,1);
        assertThat(first.total()).isEqualTo(2);assertThat(first.items()).hasSize(1);
        var second=finance.listArApItems("ENERGY-SCOPE-"+suffix,2,1);
        assertThat(second.total()).isEqualTo(2);
        assertThat(List.of(first.items().getFirst().id(),second.items().getFirst().id())).containsExactlyInAnyOrder(ownItem,otherItem);
        assertThatThrownBy(finance::exportArApItems).isInstanceOf(ApiException.class);
        String exportRole="energy_export_scope_"+suffix;
        access.createRole(new RoleRecord(exportRole,"本项目财务导出",null,"project","active",Instant.now(),List.of("finance.ar_ap.export"),null,false,List.of()));
        access.assignUserRole(reader,exportRole);
        assertThat(finance.exportArApItems().rows()).extracting(com.complex.platform.finance.ArApItemRecord::id)
            .contains(ownItem).doesNotContain(otherItem,foreignItem,nullItem);
        var request=new com.complex.platform.finance.dto.ArApPaymentRequest(key(),"只有查看权",List.of(),100L,"bank_transfer",END,"flow");
        assertThatThrownBy(()->finance.registerArApPayment(ownItem,request)).isInstanceOf(ApiException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ar_ap_payments WHERE ar_ap_item_id=?",Integer.class,ownItem)).isZero();
        String paymentRole="energy_pay_scope_"+suffix;
        access.createRole(new RoleRecord(paymentRole,"本项目财务收款",null,"project","active",Instant.now(),List.of("finance.payment.register"),null,false,List.of()));
        access.assignUserRole(reader,paymentRole);
        assertThatThrownBy(()->finance.registerArApPayment(otherItem,request)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->finance.registerArApPayment(nullItem,request)).isInstanceOf(ApiException.class);
        jdbc.update("UPDATE access_users SET merchant_id=? WHERE id=?",merchant,reader);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new AccessActor(reader,reader,"staff",project,merchant,"active"),"test",List.of()));
        // 18E（20-R3）：商户账号只按商户过滤（XM-M001-D 答复 3 第 10 条），不再与可触及项目取交集
        var merchantPage=finance.listArApItems("ENERGY-SCOPE-"+suffix,1,10);
        assertThat(merchantPage.total()).isEqualTo(4);
        assertThat(merchantPage.items()).extracting(com.complex.platform.finance.ArApItemRecord::id).containsExactlyInAnyOrder(ownItem,otherItem,foreignItem,nullItem);
    }

    @Test void exportUsesItsOwnGrantAndIncludesRowsBeyondTheFirstPage(){
        String exportRole="energy_full_export_"+suffix;
        access.createRole(new RoleRecord(exportRole,"本项目完整导出",null,"project","active",Instant.now(),List.of("finance.ar_ap.export"),null,false,List.of()));
        access.assignUserRole(reader,exportRole);
        for(int i=0;i<201;i++)ledger("energy_export_"+suffix+"_"+i,project,merchant);
        assertThatThrownBy(()->finance.listArApItems(null,1,10)).isInstanceOf(ApiException.class);
        var exported=finance.exportArApItems();
        assertThat(exported.rowCount()).isEqualTo(201);
        assertThat(exported.rows()).hasSize(201).allSatisfy(row->assertThat(row.projectId()).isEqualTo(project));
    }

    private void ledger(String id,String p,String merchantId){
        jdbc.update("INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,paid_amount_cent,unpaid_amount_cent,currency,due_date,accounting_period,status,created_at,updated_at,merchant_id,project_id) VALUES(?,?,'receivable',?,'manual',?,100,0,100,'CNY','2026-07-08','2026-06','pending_collection',now(),now(),?,?)",id,"ENERGY-SCOPE-"+suffix+"-"+id,merchantId,id,merchantId,p);
    }

    @Test void permanentRetirementPreservesRealTerminalWithoutRequiringAReplacementMeter(){
        String meter=meter("永久停用表","merchant",null);initialize(meter,"100");String proof=photo(meter);
        assertThatThrownBy(()->service.retire(meter,new EnergyWorkflowRequests.Retirement(key(),PERIOD,n("150"),END,"实际停止使用",List.of())))
            .isInstanceOf(ApiException.class);
        var terminal=read(meter,"150",END);
        var stopped=service.retire(meter,new EnergyWorkflowRequests.Retirement(key(),PERIOD,n("150"),END,"实际停止使用",List.of(proof)));
        assertThat(stopped.terminalReadingId()).isEqualTo(terminal.id());
        assertThat(repository.listReadings(PERIOD)).filteredOn(r->meter.equals(r.meterId())).hasSize(1);
        assertThat(repository.findMeterById(meter).orElseThrow().active()).isFalse();
        assertThat(workflow.hasMissingReadings(merchant,project,"2026-07")).isFalse();
        assertThat(workflow.hasUnreviewed(merchant,project,PERIOD)).isTrue();
        assertThatThrownBy(()->service.recordCharge(charge(meter,200,proof))).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.updateMeter(meter,new EnergyMeterRequest(null,null,null,null,null,null,null,null,null,true,key()))).isInstanceOf(ApiException.class);
        service.reviewReading(terminal.id(),new EnergyReadingReviewRequest(key(),null));
        service.recordCharge(charge(meter,200,proof));
        assertThat(service.settleEnergyFee(PERIOD,key())).singleElement().satisfies(b->assertThat(b.amountCent()).isEqualTo(200));
        assertThat(service.history(meter).get("retirement")).isEqualTo(stopped);
    }

    @Test void retirementWithUnknownOpeningStillNeedsSecondPersonAndSupervisorBeforeBilling(){
        String meter=meter("停用时待核初值","merchant",null);String proof=photo(meter);
        var stopped=service.retire(meter,new EnergyWorkflowRequests.Retirement(key(),PERIOD,n("150"),END,"现场永久停用",List.of(proof)));
        assertThat(repository.findReadingById(stopped.terminalReadingId()).orElseThrow().status()).isEqualTo("abnormal");
        assertThatThrownBy(()->service.recordCharge(charge(meter,200,proof))).isInstanceOf(ApiException.class);
        act(verifier);var verified=service.verify(stopped.terminalReadingId(),verification("150","100",proof));
        act(supervisor);approve(verified.reviewId());
        service.recordCharge(charge(meter,200,proof));
        assertThat(service.settleEnergyFee(PERIOD,key())).hasSize(1);
    }

    @Test void correctedTerminalBecomesNextIntervalStartWhileOriginalWrongReadingSurvives(){
        String meter=meter("倒走复核表","merchant",null);initialize(meter,"100");var original=read(meter,"50",MID);String image=photo(meter);
        assertThat(original.abnormalReason()).isEqualTo("reverse");
        act(verifier);var verification=service.verify(original.id(),verification("120","100",image));
        act(supervisor);approve(verification.reviewId());
        assertThat(repository.findReadingById(original.id()).orElseThrow().readingValue()).isEqualByComparingTo("50");
        act(reader);var next=read(meter,"140",END);
        assertThat(next.prevReadingValue()).isEqualByComparingTo("120");assertThat(next.consumption()).isEqualByComparingTo("20");
        assertThat(jdbc.queryForObject("SELECT effective_consumption FROM energy_effective_meter_readings WHERE id=?",BigDecimal.class,original.id())).isEqualByComparingTo("20");
    }

    @Test void replacementPreservesParentHierarchyAtActualReadingTime(){
        String total=meter("总表","public_area",null), child=meter("分表","merchant",total);
        initialize(total,"1000");initialize(child,"100");var childBefore=read(child,"150",MID.minusSeconds(1));
        act(verifier);service.reviewReading(childBefore.id(),new EnergyReadingReviewRequest(key(),null));
        act(reader);var replacement=service.replace(total,new EnergyWorkflowRequests.Replacement(key(),"新总表",PERIOD,n("1100"),n("10"),MID,"总表交接",List.of(photo(total))));
        var childAfter=read(child,"200",END);
        assertThat(repository.listChildReadings(total,PERIOD)).extracting(EnergyMeterReadingRecord::id).containsExactly(childBefore.id());
        assertThat(repository.listChildReadings(replacement.newMeterId(),PERIOD)).extracting(EnergyMeterReadingRecord::id).containsExactly(childAfter.id());
        assertThat(repository.findMeterById(child).orElseThrow().parentMeterId()).isEqualTo(replacement.newMeterId());
    }

    @Test void actualRoleAndProjectAreRequiredForEvidenceAndAnomalyDecision(){
        String meter=meter("本项目表","merchant",null);String image=photo(meter);var raw=read(meter,"100",END);
        String otherProject="energy_other_"+suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','另项目','active',now(),now())",otherProject);
        jdbc.update("UPDATE access_users SET organization_id=? WHERE id=?",otherProject,supervisor);
        act(verifier);assertThatThrownBy(()->service.verify(raw.id(),verification("100","0",image))).isInstanceOf(ApiException.class);
        act(supervisor,otherProject);
        assertThatThrownBy(()->service.requireEvidenceScope(meter,"evidence.attachment.create")).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.history(meter)).isInstanceOf(ApiException.class);
    }

    @Test void overdueAbnormalityIsDirectedToConfiguredSupervisorAndRemainsUnbillable() throws Exception {
        var config=service.configure(project,new EnergyWorkflowRequests.ReviewSettings(key(),7,1,30,supervisor,"项目核实期限",null));
        act(supervisor);approve(config.reviewId());
        act(reader);String meter=meter("逾期异常表","merchant",null);
        Instant created=Instant.now().minus(Duration.ofDays(8));
        var raw=repository.saveReading(new EnergyMeterReadingRecord("overdue_"+suffix,meter,PERIOD,n("80"),null,null,"abnormal",null,"remote",reader,MID,null,null,created,created));
        assertThat(workflow.escalateOverdue()).isGreaterThanOrEqualTo(1);
        act(supervisor);assertThat(service.escalations()).anyMatch(e->raw.id().equals(e.get("reading_id")));
        org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(webContext).build()
            .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/energy/escalations"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data[0].reading_id").value(raw.id()))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data[0].meter_id").value(meter))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data[0].readingId").doesNotExist())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data[0].deadline_at").exists());
        service.assign(raw.id(),new EnergyWorkflowRequests.Assignment(key(),verifier,"限期现场复抄"));
        assertThat(workflow.assignments(raw.id())).singleElement().satisfies(a->assertThat(a.verifierId()).isEqualTo(verifier));
        assertThat(repository.findReadingById(raw.id()).orElseThrow().status()).isEqualTo("abnormal");
        assertThat(service.settleEnergyFee(PERIOD,key())).isEmpty();
    }

    @Test void historyHttpUsesActualMapKeysAndProductionRecordSerialization() throws Exception {
        String meter=meter("原件HTTP表","merchant",null);initialize(meter,"100");var raw=read(meter,"150",MID);
        act(verifier);service.reviewReading(raw.id(),new EnergyReadingReviewRequest(key(),null));
        org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(webContext).build()
            .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/energy/meters/{meter_id}/history",meter))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.opening.initial_value").value(100))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.readings[0].confirmed_value").value(150))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.readings[0].confirmed_consumption").value(50))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.readings[0].confirmedConsumption").doesNotExist())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.readings[0].original.reading_value").value(150))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.readings[0].original.readingValue").doesNotExist());
    }

    @Test void storedAttachmentMetadataCannotReplaceAnActuallyUploadedFile(){
        String meter=meter("凭据校验表","merchant",null);String missing="energy_missing_"+suffix;
        evidenceRepository.saveAttachment(new EvidenceAttachmentRecord(missing,"表底.png","image/png",68L,"energy_meter",meter,"photo","energy/missing-"+suffix+".png","/api/v1/evidence/attachments/"+missing+"/content","uploaded","sha256:missing",Instant.now(),Instant.now(),null));
        assertThatThrownBy(()->service.initialize(meter,new EnergyWorkflowRequests.Opening(key(),n("100"),OPEN,"附件元数据并非实物凭据",List.of(missing)))).isInstanceOf(ApiException.class);
        assertThat(workflow.opening(meter)).isEmpty();
    }

    @Test void periodLockSerializesDifferentMetersOfOneMerchantBeforeBilling() throws Exception {
        var entered=new java.util.concurrent.CountDownLatch(1);
        var release=new java.util.concurrent.CountDownLatch(1);
        var secondStarted=new java.util.concurrent.CountDownLatch(1);
        var secondEntered=new java.util.concurrent.CountDownLatch(1);
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try{
            var first=pool.submit(()->new org.springframework.transaction.support.TransactionTemplate(transactions).execute(status->{
                workflow.lockPeriod(merchant,project,PERIOD);entered.countDown();
                try{if(!release.await(5,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("首事务未释放");}catch(InterruptedException e){throw new IllegalStateException(e);}return true;
            }));
            assertThat(entered.await(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            var second=pool.submit(()->new org.springframework.transaction.support.TransactionTemplate(transactions).execute(status->{
                secondStarted.countDown();workflow.lockPeriod(merchant,project,PERIOD);secondEntered.countDown();return true;
            }));
            assertThat(secondStarted.await(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(secondEntered.await(150,java.util.concurrent.TimeUnit.MILLISECONDS)).isFalse();
            release.countDown();assertThat(first.get(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(second.get(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(secondEntered.getCount()).isZero();
        }finally{release.countDown();pool.shutdownNow();}
    }

    private String user(String name){String id="energy_u_"+UUID.randomUUID();access.createUser(new AccessUser(id,name,"staff",project,null,"active",Instant.now()));return id;}
    private void act(String id){act(id,project);}
    private void act(String id,String p){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new AccessActor(id,id,"staff",p,null,"active"),"test",List.of()));}
    private String meter(String name,String gather,String parent){return service.createMeter(new EnergyMeterRequest(null,name,"electricity",parent,gather,null,"merchant".equals(gather)?merchant:null,project,"remote",true,key())).id();}
    private void initialize(String id,String start){service.initialize(id,new EnergyWorkflowRequests.Opening(key(),n(start),OPEN,"现场启用表底",List.of(photo(id))));}
    private EnergyMeterReadingRecord read(String meter,String value,Instant at){return service.recordReading(new EnergyReadingRecordRequest(key(),meter,PERIOD,n(value),null,"manual",null,at,List.of(photo(meter)),"现场人工补录"));}
    private EnergyWorkflowRequests.Verification verification(String value,String opening,String image){return new EnergyWorkflowRequests.Verification(key(),n(value),n(opening),"onsite_check","第二人现场核表，补齐真实启用凭据",List.of(image),supervisor);}
    private EnergyWorkflowRequests.ActualCharge charge(String meter,long amount,String proof){return new EnergyWorkflowRequests.ActualCharge(key(),contract,meter,PERIOD,amount,LocalDate.parse("2026-07-08"),"供能单位单据-"+meter,List.of(proof));}
    private String photo(String meter){return evidence.uploadAttachment(new MockMultipartFile("file","表底照片.png","image/png",PNG),"energy_meter",meter,"photo").id();}
    private void approve(String id){assertThat(reviews.approve(id,new SupervisorReviewApprovalRequest(key(),"现场资料与核表确认",List.of(),null,"approved","确认")).reviewStatus()).isEqualTo("approved");}
    private static BigDecimal n(String n){return new BigDecimal(n);}
    private static String key(){return UUID.randomUUID().toString();}
}
