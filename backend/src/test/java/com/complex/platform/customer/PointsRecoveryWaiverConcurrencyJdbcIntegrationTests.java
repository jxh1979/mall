package com.complex.platform.customer;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.EvidenceAttachmentValidator;
import com.complex.platform.evidence.EvidenceEventRepository;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.complex.platform.ticketing.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 两个真实事务竞争同一账户锁；种子提交后跨连接可见，不删除不可变奖励事实。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class PointsRecoveryWaiverConcurrencyJdbcIntegrationTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired CustomerRepository customers;
    @Autowired TicketingRepository tickets;
    @Autowired TicketRewardService rewards;
    @Autowired PointsRecoveryService recoveries;
    @Autowired LoyaltyService loyalty;
    @Autowired ObjectMapper json;
    @Autowired AuthorizationService auth;
    @Autowired IdempotencyService idempotency;
    @Autowired AuditEventService audit;
    @Autowired ApplicationContext context;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void earningFirstBlocksWaiverThenRejectsItsStaleConfirmedAmount() throws Exception {
        Seed seed = seed();
        var waivers = waiverServiceAt(seed.at());
        var results = inOrder(seed,
                () -> loyalty.earnPoints(seed.member(), 10, "earn", seed.earnSource(), "并发入账", 1),
                () -> waivers.waive(seed.member(), request(seed)));

        assertThat(results.getFirst()).isEqualTo(10L);
        assertThat(results.getLast()).isInstanceOfSatisfying(ApiException.class,
                error -> assertThat(error.code()).isEqualTo("POINTS_RECOVERY_CHANGED"));
        assertThat(recoveries.outstanding(seed.member())).isEqualTo(30);
        assertThat(customers.findPointsByCustomerId(seed.member()).orElseThrow().balance()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customer_points_recovery_waivers WHERE customer_id=?", Long.class, seed.member())).isZero();
        assertNewLedger(seed, -10, 0);
        assertOriginalFacts(seed);
    }

    @Test
    void waiverFirstBlocksEarningThenCreditsWithoutRecoveringWaivedPoints() throws Exception {
        Seed seed = seed();
        var waivers = waiverServiceAt(seed.at());
        var results = inOrder(seed,
                () -> waivers.waive(seed.member(), request(seed)),
                () -> loyalty.earnPoints(seed.member(), 10, "earn", seed.earnSource(), "豁免后入账", 1));

        assertThat(results.getFirst()).isInstanceOfSatisfying(PointsRecoveryWaiverService.Waiver.class,
                waiver -> assertThat(waiver.waivedPoints()).isEqualTo(40));
        assertThat(results.getLast()).isEqualTo(10L);
        assertThat(recoveries.outstanding(seed.member())).isZero();
        assertThat(customers.findPointsByCustomerId(seed.member()).orElseThrow().balance()).isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT status FROM customer_points_recoveries WHERE reward_id=?", String.class, seed.reward())).isEqualTo("waived");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customer_points_recovery_waivers WHERE customer_id=?", Long.class, seed.member())).isEqualTo(1);
        assertNewLedger(seed, 0, 10);
        assertOriginalFacts(seed);
    }

    @Test
    void readingOneProjectCannotReturnAnotherProjectsConcurrentWaiverOriginal() throws Exception {
        Seed first = seed(null, false);
        Seed second = seed(first, false);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("UPDATE customer_points_accounts SET balance=60 WHERE customer_id=?", first.member());
            customers.savePointsLedger(new CustomerPointsLedgerRecord("spent_scope_"+first.member(),first.member(),-80,60,"deduct","scope_spend_"+first.member(),"两票合计140分中已消费80分",null,null,false,Instant.now()));
            jdbc.update("UPDATE ticket_verifications SET status='reversed' WHERE id=?", first.verification());
            rewards.reversed(new TicketRewardFacts.Reversed(first.verification()));
        });
        assertThat(recoveries.outstanding(first.member())).isEqualTo(40);
        String reader="reader_"+UUID.randomUUID().toString().replace("-", ""), role="role_"+reader;
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,status,created_at) VALUES(?,?,'employee',?,'active',now())",reader,reader,first.project());
        jdbc.update("INSERT INTO roles(id,role_name,created_at) VALUES(?,?,now())",role,"积分并发测试角色"+new java.math.BigInteger(role.substring(role.lastIndexOf('_')+1),16));
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,?)",role,PointsRecoveryWaiverService.VIEW);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)",reader,role);
        var readActor=new AccessActor(reader,reader,"employee",first.project(),null,"active");
        var readFinishedFirstQuery=new CountDownLatch(1);
        var releaseRead=new CountDownLatch(1);
        var readerPid=new CompletableFuture<Integer>();
        var readingService=waiverServiceAt(second.at(),()->{
            readerPid.complete(jdbc.queryForObject("SELECT pg_backend_pid()",Integer.class));
            readFinishedFirstQuery.countDown();
            try {
                if(!releaseRead.await(20,TimeUnit.SECONDS))throw new AssertionError("查询未获放行");
            } catch(InterruptedException error) {Thread.currentThread().interrupt();throw new AssertionError(error);}
        });
        var pool=Executors.newSingleThreadExecutor();
        try {
            Future<Object> reading=pool.submit(()->transact(readActor,()->{
                assertThat(auth.projectReach(readActor,PointsRecoveryWaiverService.VIEW).covers(first.project())).isTrue();
                assertThat(auth.projectReach(readActor,PointsRecoveryWaiverService.VIEW).covers(second.project())).isFalse();
                return readingService.get(first.member());
            }));
            assertThat(readFinishedFirstQuery.await(15,TimeUnit.SECONDS)).isTrue();
            int readPid=readerPid.get(1,TimeUnit.SECONDS);
            Object written=transact(second,()->{
                assertThat(jdbc.queryForObject("SELECT pg_backend_pid()",Integer.class)).isNotEqualTo(readPid);
                jdbc.update("UPDATE ticket_verifications SET status='reversed' WHERE id=?",second.verification());
                rewards.reversed(new TicketRewardFacts.Reversed(second.verification()));
                assertThat(recoveries.outstanding(first.member())).isEqualTo(80);
                return waiverServiceAt(second.at()).waive(first.member(),new PointsRecoveryWaiverService.Request(
                        "scope_"+first.member(),second.waiverConfig(),80,"两项目全部待追回积分统一豁免"));
            });
            assertThat(written).isInstanceOfSatisfying(PointsRecoveryWaiverService.Waiver.class,waiver->{
                assertThat(waiver.waivedPoints()).isEqualTo(80);
                assertThat(waiver.lines()).extracting(PointsRecoveryWaiverService.Line::projectId)
                        .containsExactlyInAnyOrder(first.project(),second.project());
            });
            releaseRead.countDown();
            assertThat(reading.get(15,TimeUnit.SECONDS)).isInstanceOfSatisfying(ApiException.class,
                    error->assertThat(error.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
        } finally {
            releaseRead.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(20,TimeUnit.SECONDS)).as("真实读取事务已退出").isTrue();
        }
    }

    private List<Object> inOrder(Seed seed, Supplier<?> firstAction, Supplier<?> secondAction) throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        var firstReady = new CompletableFuture<Integer>();
        var secondReady = new CompletableFuture<Integer>();
        var releaseFirst = new CountDownLatch(1);
        Future<Object> first = null, second = null;
        try {
            first = pool.submit(() -> transact(seed, () -> {
                int pid = jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class);
                Object result = firstAction.get();
                firstReady.complete(pid);
                try {
                    if (!releaseFirst.await(20, TimeUnit.SECONDS)) throw new AssertionError("首事务未获放行");
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(error);
                }
                return result;
            }));
            int firstPid = firstReady.get(15, TimeUnit.SECONDS);
            second = pool.submit(() -> transact(seed, () -> {
                secondReady.complete(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
                return secondAction.get();
            }));
            int secondPid = secondReady.get(15, TimeUnit.SECONDS);
            assertThat(secondPid).isNotEqualTo(firstPid);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            boolean accountBlocked = false;
            while (System.nanoTime() < deadline && !second.isDone()) {
                accountBlocked = Boolean.TRUE.equals(jdbc.queryForObject("""
                        SELECT EXISTS(SELECT 1 FROM pg_stat_activity
                        WHERE pid=? AND ?=ANY(pg_blocking_pids(pid))
                        AND lower(query) LIKE '%customer_points_accounts%'
                        AND lower(query) LIKE '%for update%')
                        """, Boolean.class, secondPid, firstPid));
                if (accountBlocked) break;
                // 只减缓数据库观测频率；必须实际观测到指定 PID 的账户锁等待才能放行。
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
            }
            assertThat(accountBlocked).as("后事务真实阻塞于首事务持有的会员账户 FOR UPDATE 锁").isTrue();
            releaseFirst.countDown();
            return List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
        } finally {
            releaseFirst.countDown();
            pool.shutdown();
            if (!pool.awaitTermination(20, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).as("两个数据库事务均已退出").isTrue();
            }
        }
    }

    private Object transact(Seed seed, Supplier<?> action) {
        return transact(new AccessActor(seed.actor(),seed.actor(),"platform_admin",seed.entity(),null,"active"),action);
    }

    private Object transact(AccessActor actor, Supplier<?> action) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                actor, "token", List.of()));
        try {
            return new TransactionTemplate(transactionManager).execute(status -> {
                jdbc.execute("SET LOCAL lock_timeout='15s'");
                jdbc.execute("SET LOCAL statement_timeout='20s'");
                return action.get();
            });
        } catch (ApiException error) {
            return error;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private PointsRecoveryWaiverService.Request request(Seed seed) {
        return new PointsRecoveryWaiverService.Request("waive_" + seed.member(), seed.waiverConfig(), 40, "并发核实小额豁免");
    }

    private PointsRecoveryWaiverService waiverServiceAt(Instant at) {
        return waiverServiceAt(at,()->{});
    }

    private PointsRecoveryWaiverService waiverServiceAt(Instant at,Runnable beforeLookup) {
        // 只固定配置查询时刻；配置内容/筛选 SQL、权限、幂等、账户锁和追回业务均为真实实现。
        // 配置公告/审批另有测试；这里不把未来测试配置变成当前全局生效配置。
        var configs = new TicketRewardConfigService(jdbc, json, auth,
                context.getBean(SupervisorReviewService.class), idempotency, audit,
                context.getBean(EvidenceEventRepository.class), context.getBean(EvidenceAttachmentValidator.class)) {
            @Override TicketRewardConfig effective(String kind, String type, String project, Instant ignored) {
                beforeLookup.run();
                return super.effective(kind, type, project, at);
            }
        };
        // 并发工作线程显式开启真实事务，和生产 @Transactional 使用同一事务管理器。
        return new PointsRecoveryWaiverService(jdbc, customers, configs, auth, idempotency, audit, json);
    }

    private void assertNewLedger(Seed seed, long recoveryDelta, long balance) {
        assertThat(jdbc.queryForObject("SELECT sum(points_delta) FROM customer_points_ledger WHERE customer_id=? AND source_id=?", Long.class, seed.member(), seed.earnSource())).isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT COALESCE(sum(points_delta),0) FROM customer_points_ledger WHERE customer_id=? AND source_doc_type='ticket_points_recovery'", Long.class, seed.member())).isEqualTo(recoveryDelta);
        assertThat(jdbc.queryForObject("SELECT balance_after FROM customer_points_ledger WHERE customer_id=? AND source_id=?", Long.class, seed.member(), seed.earnSource())).isEqualTo(balance);
    }

    private void assertOriginalFacts(Seed seed) {
        assertThat(jdbc.queryForObject("SELECT original_points FROM customer_points_recoveries WHERE reward_id=?", Long.class, seed.reward())).isEqualTo(40);
        assertThat(jdbc.queryForObject("SELECT points FROM ticket_use_rewards WHERE id=?", Long.class, seed.reward())).isEqualTo(100);
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM ticket_points_issuance_fees WHERE reward_id=?", Long.class, seed.reward())).isEqualTo(100);
    }

    private Seed seed() {
        return seed(null,true);
    }

    private Seed seed(Seed existingMember,boolean reverseImmediately) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            String key = UUID.randomUUID().toString().replace("-", "");
            String type = "tt_" + key, project = "p_" + key, entity = "be_" + key;
            String member = existingMember==null?"c_"+key:existingMember.member(), order = "to_" + key, parent = "ti_" + key;
            // UUID 隔离永久事实；随机递增未来时间与版本隔离重复运行，当前生效配置不变。
            Instant at = jdbc.queryForObject("SELECT GREATEST(now()+interval '100 years',COALESCE(max(effective_at),now())) FROM ticket_reward_config_versions", Timestamp.class)
                    .toInstant().plusSeconds(86400 + Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), 86400));
            jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'并发测试销售公司','platform','active',now(),now())", entity);
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','并发测试综合体','active',now(),now())", project);
            jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", entity, project);
            jdbc.update("INSERT INTO ticket_types(id,ticket_name,base_calendar_price_mode,status,created_at,updated_at) VALUES(?,'并发测试票','fixed','active',now(),now())", type);
            if(existingMember==null)customers.saveCustomer(new CustomerRecord(member, "并发测试-" + key, "***", "active", null, at, at, at));
            addConfig("points", type, project, values(existingMember==null?100L:40L, null, null), at.minusSeconds(120));
            addConfig("growth", type, project, values(7L, null, null), at.minusSeconds(120));
            addConfig("national", null, null, values(null, entity, BigDecimal.ONE), at.minusSeconds(120));
            addConfig("sales_contract", type, project, new TicketRewardConfig.Values(null,null,null,null,null,null,null,null,null,null,entity,null,"合同-"+key,List.of()), at.minusSeconds(120));
            String waiverConfig = addConfig("recovery_waiver", null, null, new TicketRewardConfig.Values(100L,0L,200L,null,null,null,null,null,null,null,null,null,null,List.of()), at.minusSeconds(120));
            jdbc.update("INSERT INTO ticket_orders(id,order_no,ticket_type_id,quantity,sales_channel,amount_cent,pay_status,issue_status,status,created_at,updated_at,project_id,customer_id) VALUES(?,?,?,1,'web',10000,'paid','issued','created',?,?,?,?)", order,order,type,Timestamp.from(at.minusSeconds(60)),Timestamp.from(at),project,member);
            rewards.orderCreated(new TicketRewardFacts.OrderCreated(order));
            tickets.saveTicketInstance(new TicketInstanceRecord(parent,parent,order,type,1,1,1,member,member,"active",null,null,at,at));
            tickets.upsertSlotHolder(parent,1,member,at);
            String entitlement="e_"+key,template="et_"+key,device="dev_"+key,verification="v_"+key,credential="cred_"+key;
            jdbc.update("INSERT INTO entitlement_templates(id,name,entitlement_type,default_total_times,default_value_cent,status,created_at,updated_at) VALUES(?,'并发核销','ticket',1,100,'active',now(),now())",template);
            tickets.saveIssuedEntitlement(new IssuedEntitlementRecord(entitlement,entitlement,type,order,template,"ticket",null,null,null,1,1,100L,"active",at.minusSeconds(60),at.plusSeconds(360000),at,at,1,null,null,null,null,null,parent,1,null,null));
            jdbc.update("INSERT INTO terminal_devices(id,device_id,device_code,terminal_type,binding_scope,status,created_at,updated_at) VALUES(?,?,?,'kiosk','venue','bound',now(),now())",device,device,device);
            tickets.saveTicketCredential(new TicketCredentialRecord(credential,entitlement,order,credential,"n_"+key,"h_"+key,"dynamic_qr",at,at.plusSeconds(60),"consumed",1,at,at,60,at,null,null));
            rewards.presented(new TicketRewardFacts.CredentialPresented(credential,member));
            tickets.saveVerification(new VerificationRecord(verification,verification,entitlement,entitlement,"dynamic_qr",credential,device,"gate","success",null,"android_terminal_online",null,at,"n_"+key,null,null,true,true,1,null,null,null,100L));
            rewards.used(new TicketRewardFacts.Used(verification,credential));
            String reward = rewards.list(member).getFirst().id();
            assertThat(rewards.release(reward,at.plusSeconds(86400))).isTrue();
            if(reverseImmediately) {
                // 建立此前已使用 40 分的前置事实，随后由真实核销冲正产生 40 分待追回。
                jdbc.update("UPDATE customer_points_accounts SET balance=60 WHERE customer_id=?",member);
                customers.savePointsLedger(new CustomerPointsLedgerRecord("spent_"+key,member,-40,60,"deduct","prior_spend_"+key,"已发生消费夹具",null,null,false,at.plusSeconds(86401)));
                jdbc.update("UPDATE ticket_verifications SET status='reversed' WHERE id=?",verification);
                rewards.reversed(new TicketRewardFacts.Reversed(verification));
                assertThat(recoveries.outstanding(member)).isEqualTo(40);
            }
            String actor="u_"+key,role="r_"+key;
            jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,status,created_at) VALUES(?,?,'platform_admin',?,'active',now())",actor,actor,entity);
            jdbc.update("INSERT INTO roles(id,role_name,created_at) VALUES(?,?,now())",role,"积分并发测试角色"+new java.math.BigInteger(role.substring(role.lastIndexOf('_')+1),16));
            for(String permission:List.of(PointsRecoveryWaiverService.VIEW,PointsRecoveryWaiverService.WAIVE))jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,?)",role,permission);
            jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)",actor,role);
            return new Seed(member,entity,actor,reward,waiverConfig,"earn_"+key,at,project,verification);
        });
    }

    private TicketRewardConfig.Values values(Long fixed,String receiver,BigDecimal price) {
        return new TicketRewardConfig.Values(fixed,fixed==null?null:0L,fixed==null?null:100000L,price==null?null:24,price==null?null:1,price==null?null:720,price,price==null?null:new BigDecimal("0.001"),price==null?null:new BigDecimal("100"),receiver,null,null,null,List.of());
    }

    private String addConfig(String kind,String type,String project,TicketRewardConfig.Values values,Instant effective) {
        String key=UUID.randomUUID().toString().replace("-",""),id="trc_"+key;
        String version="V"+Math.floorMod(UUID.randomUUID().getMostSignificantBits(),1_000_000_000_000L)+".01";
        try {
            jdbc.update("INSERT INTO ticket_reward_config_versions(id,kind,ticket_type_id,project_id,version,payload,effective_at,published_at,status,created_by,created_at,approved_at) VALUES(?,?,?,?,?,?::jsonb,?,?,'approved','test-reviewer',?,?)",id,kind,type,project,version,json.writeValueAsString(values),Timestamp.from(effective),Timestamp.from(effective.minusSeconds(60)),Timestamp.from(effective.minusSeconds(120)),Timestamp.from(effective.minusSeconds(60)));
        } catch (com.fasterxml.jackson.core.JsonProcessingException error) {throw new IllegalStateException(error);}
        return id;
    }

    private record Seed(String member,String entity,String actor,String reward,String waiverConfig,String earnSource,Instant at,String project,String verification) {}
}
