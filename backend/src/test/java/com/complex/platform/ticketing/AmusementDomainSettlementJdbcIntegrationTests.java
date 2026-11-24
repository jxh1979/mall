package com.complex.platform.ticketing;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.amusement.AmusementBusinessDayService;
import com.complex.platform.amusement.AmusementRepository;
import com.complex.platform.amusement.AmusementSessionLifecycleService;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.complex.platform.settlement.AmusementDomainSettlement;
import com.complex.platform.settlement.RevenueStreamAttributionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 第十八批 BR 段第 357 条 357-R2（BSM:753「游乐公司再按已批准的内部结算价或域内规则，与实际提供服务的游乐商户结算。」；BSM:1689「通票权益核销后才按所属域的
 * 已生效规则与实际提供方结算；未核销不产生提供方结算。」；BSM:1671「核销或结算时把当时价格固化到流水，历史不得手改。」；ENUM:54「任何商户在没有获批内部结算价
 * 之前，不得为其开通需要该价格结算的权益或券。」）：游乐入场核销逐笔固化域内结算行，撤销记冲减行。
 *
 * <p>夹具照 {@link AmusementTicketJdbcFixture}（真实项目、已批准营业日、开放场次与父票）；游乐商户经营该项目、项目引用该商户的目录条目由夹具直写。入场与撤销
 * 走员工端真实请求形状（蛇形键、真实授权链）。核销被拒要在业务事务回滚后另起事务落拒绝记录，本类不开测试事务、逐项清理（商户与目录条目留库，中文名）。
 * 修复前没有结算行：各用例第一条行数断言即红。</p>
 */
@SpringBootTest(properties={
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
    "spring.datasource.username=complex",
    "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"})
@AutoConfigureMockMvc
class AmusementDomainSettlementJdbcIntegrationTests {
    private static final ObjectMapper JSON=new ObjectMapper();
    @Autowired AmusementRepository repository;
    @Autowired AmusementSessionLifecycleService lifecycle;
    @Autowired AmusementBusinessDayService days;
    @Autowired SupervisorReviewService reviews;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mockMvc;
    @Autowired AmusementDomainSettlement settlement;
    @Autowired BusinessDayResolver resolver;
    @Autowired RevenueStreamAttributionRepository attribution;
    private final String s=String.valueOf(System.nanoTime());
    private final String merchant="ads_m_"+s;
    private final String code="sku_ads_"+s;
    private AmusementTicketJdbcFixture.Data f;
    private AccessActor actor;
    private final List<AmusementTicketJdbcFixture.Data> seeded=new ArrayList<>();

    @AfterEach void cleanup(){
        try {
            if(f!=null)jdbc.update("DELETE FROM amusement_domain_settlement_lines WHERE amusement_project_id=?",f.project());
            for(var ticket:seeded)AmusementTicketJdbcFixture.cleanupTicket(jdbc,ticket);
            if(f!=null)AmusementTicketJdbcFixture.cleanupProject(jdbc,f);
        } finally {SecurityContextHolder.clearContext();}
    }

    /** ①②⑥：入场固化当时生效价 3000（带价格版本与入场原件营业日）；改价 3500 后再入场记 3500、前一行不变；撤销第一次入场多一行负 3000。 */
    @Test void admissionLocksTheCurrentInternalPriceAndReversalAddsANegativeLine() throws Exception {
        String v1=seed(true);
        String first=admit(f,"first");
        assertThat(lines(f.project())).containsExactly("settled/3000/"+merchant+"/"+payer()+"/"+v1);
        assertThat(jdbc.queryForObject("SELECT business_date=(SELECT av.business_date FROM ticket_verifications tv JOIN amusement_verifications av"
                + " ON av.id=tv.amusement_verification_id WHERE tv.id=?) FROM amusement_domain_settlement_lines WHERE verification_id=?",
                Boolean.class,first,first)).isTrue();

        jdbc.update("UPDATE internal_price_catalog SET status='inactive' WHERE item_code=? AND is_draft=FALSE AND status='active'",code);
        String v2=catalogRow(code+"_v2",3500);
        var next=AmusementTicketJdbcFixture.anotherTicket(jdbc,f,3,"per_use");seeded.add(next);
        String second=admit(next,"second");
        assertThat(jdbc.queryForObject("SELECT amount_cent||'/'||price_catalog_entry_id FROM amusement_domain_settlement_lines WHERE verification_id=?",
                String.class,second)).isEqualTo("3500/"+v2);
        assertThat(jdbc.queryForObject("SELECT amount_cent||'/'||price_catalog_entry_id FROM amusement_domain_settlement_lines WHERE verification_id=?",
                String.class,first)).isEqualTo("3000/"+v1);

        // 上月入场、本月撤销：reversed 行营业日取撤销当时的营业日（357-R2），不照抄原行；357-R3 追收与 274-R1 撤销当期冲减都只看它。
        jdbc.update("UPDATE amusement_domain_settlement_lines SET business_date=business_date-40 WHERE verification_id=? AND kind='settled'",first);
        LocalDate before=resolver.businessDate(f.complex(),null,Instant.now());
        as(actor,"/api/v1/ticketing/verifications/"+first+"/reverse",Map.of("idempotency_key","ads_rev_"+s,"reason","复核误刷入场"))
                .andExpect(status().isOk());
        LocalDate after=resolver.businessDate(f.complex(),null,Instant.now());
        assertThat(jdbc.queryForList("SELECT kind||'/'||amount_cent||'/'||provider_merchant_id||'/'||price_catalog_entry_id"
                + " FROM amusement_domain_settlement_lines WHERE verification_id=? ORDER BY kind DESC",String.class,first))
                .containsExactly("settled/3000/"+merchant+"/"+v1,"reversed/-3000/"+merchant+"/"+v1);
        assertThat(jdbc.queryForObject("SELECT business_date FROM amusement_domain_settlement_lines WHERE verification_id=? AND kind='reversed'",
                java.sql.Date.class,first).toLocalDate()).isIn(before,after);
        assertThat(jdbc.queryForObject("SELECT date_trunc('month',r.business_date)>date_trunc('month',s.business_date) FROM amusement_domain_settlement_lines r"
                + " JOIN amusement_domain_settlement_lines s ON s.verification_id=r.verification_id AND s.kind='settled'"
                + " WHERE r.verification_id=? AND r.kind='reversed'",Boolean.class,first)).isTrue();
    }

    /** ③：游乐商户的项目未引用目录条目，入场照常成功、不记结算行。 */
    @Test void unreferencedProjectAdmitsWithoutSettlementLine() throws Exception {
        seed(false);
        admit(f,"plain");
        assertThat(lines(f.project())).isEmpty();
        assertThat(used(f)).isEqualTo(1);
    }

    /** ④：被引用条目已存档、没有生效价，入场 400 ENTITLEMENT_PRICE_UNRESOLVED，权益次数不扣、没有成功的入场与核销记录。 */
    @Test void archivedReferencedPriceRejectsAdmissionWithoutConsuming() throws Exception {
        seed(true);
        jdbc.update("UPDATE internal_price_catalog SET status='inactive' WHERE item_code=?",code);
        http(f,"unresolved").andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("ENTITLEMENT_PRICE_UNRESOLVED"));
        assertThat(used(f)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_verifications WHERE entitlement_id=? AND status='success'",Integer.class,
                f.entitlement())).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM amusement_verifications WHERE entitlement_id=? AND verification_result='success'",
                Integer.class,f.entitlement())).isZero();
        assertThat(lines(f.project())).isEmpty();
    }

    /** ⑤：补偿补发权益的入场（事件带补偿记录号）已由 FS-012 另结，不记域内结算行；同一事件不带补偿记录号即记。 */
    @Test void compensationAdmissionIsLeftToCompensationFulfillment() throws Exception {
        seed(true);
        String verification=admit(f,"comp");
        jdbc.update("DELETE FROM amusement_domain_settlement_lines WHERE verification_id=?",verification);
        settlement.onSettled(event(verification,"acomp_"+s));
        assertThat(lines(f.project())).isEmpty();
        settlement.onSettled(event(verification,null));
        assertThat(lines(f.project())).hasSize(1);
    }

    /**
     * 第十八批 CC 段 400-R1（第十八批裁决 83「同一设施当天只按内部结算价跟提供设施的游乐商户结算一次」）：按营业日计次的游乐权益同一营业日两次进同一设施，
     * 两次都核销成功，只有首入记一行域内结算（settled/3000），当天再入（day_replay）不记。回归守护，修复前后都绿。
     */
    @Test void businessDayReentryToTheSameRideSettlesOnlyOnce() throws Exception {
        seed(true);
        var next=AmusementTicketJdbcFixture.anotherTicket(jdbc,f,2,"unlimited_in_day");seeded.add(next);
        String first=admit(next,"day_first");
        String again=admit(next,"day_again");
        assertThat(jdbc.queryForList("SELECT kind||'/'||amount_cent FROM amusement_domain_settlement_lines WHERE verification_id=?",String.class,first))
                .containsExactly("settled/3000");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM amusement_domain_settlement_lines WHERE verification_id=?",Integer.class,again)).isZero();
        assertThat(lines(f.project())).hasSize(1);
        assertThat(used(next)).isEqualTo(1);
    }

    // ---------- 夹具 ----------

    /** 种项目并交给游乐商户经营；referenced 时项目引用该商户 3000 分的已生效目录条目。返回生效目录行 id。 */
    private String seed(boolean referenced){
        f=AmusementTicketJdbcFixture.seed(jdbc,repository,lifecycle,days,reviews,3,"per_use",10);seeded.add(f);
        actor=(AccessActor)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at)"
                + " VALUES (?, ?, 'amusement', ?, 'active', now(), now())",merchant,"游乐域内结算测试商户"+s,f.complex());
        String v1=catalogRow(code+"_v1",3000);
        jdbc.update("UPDATE amusement_projects SET merchant_id=?, catalog_item_code=? WHERE id=?",merchant,referenced?code:null,f.project());
        return v1;
    }

    private String catalogRow(String id,long priceCent){
        jdbc.update("INSERT INTO internal_price_catalog (id, item_code, merchant_id, category, item_name, price_cent, status, is_draft, updated_by,"
                + " created_at, updated_at) VALUES (?, ?, ?, 'service', '游乐域内结算测试条目', ?, 'active', FALSE, 'seed_357', now(), now())",
                id,code,merchant,priceCent);
        return id;
    }

    private String payer(){return attribution.resolveManagementEntity(f.complex(),"amusement_ticket");}

    private List<String> lines(String project){
        return jdbc.queryForList("SELECT kind||'/'||amount_cent||'/'||provider_merchant_id||'/'||payer_entity_id||'/'||price_catalog_entry_id"
                + " FROM amusement_domain_settlement_lines WHERE amusement_project_id=? ORDER BY created_at",String.class,project);
    }

    private int used(AmusementTicketJdbcFixture.Data ticket){
        return jdbc.queryForObject("SELECT used_times FROM issued_entitlements WHERE id=?",Integer.class,ticket.entitlement());
    }

    private SettlementSettledEvent event(String verification,String compensationItemId){
        return new SettlementSettledEvent(verification,Instant.now(),f.entitlement(),f.order(),f.type(),"amusement_pass",null,3,1,1,0L,
                Instant.now().plusSeconds(86400),null,null,null,f.complex(),f.parent(),compensationItemId);
    }

    /** 员工端门票核销屏实发形状入场，返回票务核销记录号。 */
    private String admit(AmusementTicketJdbcFixture.Data ticket,String request) throws Exception {
        String body=http(ticket,request).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JSON.readTree(body).path("data").path("id").asText();
    }

    private ResultActions http(AmusementTicketJdbcFixture.Data ticket,String request) throws Exception {
        return as(actor,"/api/v1/ticketing/verifications",Map.of("idempotency_key","ads_"+request+"_"+ticket.code(),"credential_type","qr",
                "credential_payload",ticket.code(),"device_id",f.device(),"point_id",f.point(),
                "business_context",Map.of("scenario","ticket_checkin","staff_id","gate")));
    }

    private ResultActions as(AccessActor who,String path,Map<String,?> body) throws Exception {
        return mockMvc.perform(post(path).with(r->{
                    r.addHeader("X-Terminal-Type","web");
                    return authentication(new UsernamePasswordAuthenticationToken(who,null,
                            List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(r);})
                .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)));
    }
}
