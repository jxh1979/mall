package com.complex.platform.customer;

// 文件版本：V0.04
// M006-027：连接内临时表验证储值流水分页、支付行锁和真实事务回滚，不写共享业务表。
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import com.complex.platform.access.*;
import com.complex.platform.finance.*;
import com.complex.platform.ticketing.*;
import com.complex.platform.evidence.AuditEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.util.ReflectionTestUtils;

class M006StoredValueJdbcTests {
    Connection connection;
    SingleConnectionDataSource dataSource;
    JdbcTemplate jdbc;
    JdbcCustomerRepository customers;
    final Instant now = Instant.parse("2026-09-05T00:00:00Z");

    @BeforeEach void open() throws Exception {
        String url = System.getProperty("spring.datasource.hikari.jdbc-url", System.getenv("COMPLEX_DATABASE_URL"));
        Assumptions.assumeTrue(url != null, "需要显式的独立测试数据库");
        if (!url.matches("jdbc:postgresql://[^/]+/[A-Za-z0-9_]+_test(?:\\?.*)?")) {
            throw new IllegalStateException("仅允许在名称以 _test 结尾的独立测试库运行");
        }
        connection=DriverManager.getConnection(url,System.getenv("COMPLEX_DATABASE_USERNAME"),System.getenv("COMPLEX_DATABASE_PASSWORD"));
        dataSource=new SingleConnectionDataSource(connection,true);
        jdbc=new JdbcTemplate(dataSource);
        for(String table:List.of("customer_stored_value_transactions","customer_stored_value_accounts","finance_payment_intents")) {
            jdbc.execute("CREATE TEMP TABLE "+table+" (LIKE public."+table+" INCLUDING ALL)");
        }
        customers=new JdbcCustomerRepository(new NamedParameterJdbcTemplate(dataSource),new ObjectMapper(),null,null);
    }
    @AfterEach void close() throws Exception { if(connection!=null)connection.close(); }

    @Test void historyUsesBothOwnerAndCardAndReadsLaterPages() {
        for(int n=0;n<25;n++) customers.saveStoredValueTransaction(new CustomerStoredValueTransactionRecord(
                "txn_"+String.format("%02d",n),"card","customer","consume",-100L,10000L-n*100L,
                null,"bill_"+n,"key_"+n,now.plusSeconds(n)));
        customers.saveStoredValueTransaction(new CustomerStoredValueTransactionRecord(
                "foreign","card","another","consume",-999L,1L,null,"foreign","foreign",now.plusSeconds(100)));
        var first=customers.listStoredValueTransactions("customer","card",1,20);
        var second=customers.listStoredValueTransactions("customer","card",2,20);
        assertThat(first).hasSize(20);
        assertThat(second).hasSize(5);
        assertThat(first.getFirst().id()).isEqualTo("txn_24");
        assertThat(second.getFirst().id()).isEqualTo("txn_04");
        assertThat(customers.listStoredValueTransactions("customer","other_card",1,20)).isEmpty();
        assertThat(first).allMatch(txn->"customer".equals(txn.customerId()));
    }

    @Test void financePaymentReadTakesPostgresRowLock() throws Exception {
        var type=Class.forName("com.complex.platform.finance.JdbcFinanceRepository");
        var ctor=type.getDeclaredConstructor(NamedParameterJdbcTemplate.class,ObjectMapper.class);
        ctor.setAccessible(true);
        var finance=(FinanceRepository)ctor.newInstance(new NamedParameterJdbcTemplate(dataSource),new ObjectMapper());
        finance.savePaymentIntent(new PaymentIntentRecord("pay","stored_value_topup","customer",5000L,"CNY","wechat","created",now,now));
        new TransactionTemplate(new DataSourceTransactionManager(dataSource)).execute(status->{
            assertThat(finance.findPaymentIntentByIdForUpdate("pay")).isPresent();
            assertThat(jdbc.queryForObject("""
                    SELECT count(*) FROM pg_locks WHERE pid=pg_backend_pid()
                    AND relation='pg_temp.finance_payment_intents'::regclass AND mode='RowShareLock' AND granted
                    """,Integer.class)).isGreaterThan(0);
            return null;
        });
    }


    @Test void posRechargeUsesOfflineConfirmationAndCreditsTheBalanceOnlyOnce() throws Exception {
        for(String table : List.of("customers", "stored_value_topup_intents", "finance_offline_payment_confirmations")) {
            jdbc.execute("CREATE TEMP TABLE " + table + " (LIKE public." + table + " INCLUDING ALL)");
        }
        var cipher = new com.complex.platform.common.crypto.PiiCipher(purpose -> new javax.crypto.spec.SecretKeySpec(
                "0123456789abcdef0123456789abcdef".getBytes(java.nio.charset.StandardCharsets.UTF_8), "AES"));
        jdbc.update("INSERT INTO customers (id,phone,masked_phone,status,created_at,updated_at) VALUES ('customer',?,'138****8000','active',now(),now())",
                cipher.encrypt("13800138000"));
        var customerRepo = new JdbcCustomerRepository(new NamedParameterJdbcTemplate(dataSource),new ObjectMapper(),cipher,null);
        customerRepo.saveStoredValueAccount(new CustomerStoredValueAccountRecord(
                "card","customer",null,"merchant",0L,"active",now,now,"merchant_prepaid"));
        jdbc.update("INSERT INTO stored_value_topup_intents (payment_intent_id,customer_id,project_id,merchant_id,created_at) VALUES ('pos_pay','customer',null,'merchant',now())");
        var ctor=Class.forName("com.complex.platform.finance.JdbcFinanceRepository")
                .getDeclaredConstructor(NamedParameterJdbcTemplate.class,ObjectMapper.class);
        ctor.setAccessible(true);
        var financeRepo=(FinanceRepository)ctor.newInstance(new NamedParameterJdbcTemplate(dataSource),new ObjectMapper());
        financeRepo.savePaymentIntent(new PaymentIntentRecord("pos_pay","stored_value_topup_merchant","customer",5000L,"CNY","pos","created",now,now));
        var audit=mock(AuditEventService.class);
        var operations=new com.complex.platform.evidence.AuditedOperationExecutor(
                new com.complex.platform.common.idempotency.IdempotencyService(
                        new com.complex.platform.support.InMemoryIdempotencyRepository(),new ObjectMapper()),audit);
        var disabled=com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled();
        var finance=new FinanceService(financeRepo,operations,audit,mock(AuthorizationService.class),null,disabled,null);
        var target=new StoredValueService(customerRepo,finance,audit,disabled,mock(AuthorizationService.class),null);
        var factory=new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(dataSource),
                new AnnotationTransactionAttributeSource()));
        var service=(StoredValueService)factory.getProxy();
        for(String key : List.of("pos-first","pos-first","pos-retry")) {
            service.confirmOfflineTopupAndSettle("pos_pay",new com.complex.platform.finance.dto.OfflinePaymentConfirmRequest(
                    key,null,List.of(),"pos_pay",5000L,"pos",now));
        }
        assertThat(financeRepo.findPaymentIntentById("pos_pay").orElseThrow().status()).isEqualTo("succeeded");
        assertThat(customerRepo.findStoredValueAccount("customer",null,"merchant").orElseThrow().balanceCent()).isEqualTo(5000L);
        assertThat(customerRepo.listStoredValueTransactions("customer","card",1,20)).hasSize(1);
    }

    @Test void failedOrderUpdateRollsBackBothActualBalanceAndDebitLedger() {
        customers.saveStoredValueAccount(new CustomerStoredValueAccountRecord(
                "card","customer",null,"merchant",5000L,"active",now,now,"merchant_prepaid"));
        var auth=mock(AuthorizationService.class);
        when(auth.currentActor()).thenReturn(new AccessActor("customer","customer","customer",null,null,"active"));
        var tickets=mock(TicketingRepository.class);
        var order=new TicketOrderRecord("order","T1","customer","type",null,1,null,"app","13800138000",
                2500L,null,2500L,0,"CNY","unpaid","unissued","created",now,now,null,null,"merchant");
        when(tickets.findTicketOrderByIdForUpdate("order")).thenReturn(Optional.of(order));
        when(tickets.updateTicketOrder(any())).thenThrow(new IllegalStateException("订单写入失败"));
        var target=new StoredValueService(customers,mock(FinanceService.class),mock(AuditEventService.class),null,auth,null);
        ReflectionTestUtils.setField(target,"ticketingRepository",tickets);
        ReflectionTestUtils.setField(target,"customerService",mock(CustomerService.class));
        var factory=new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(dataSource),
                new AnnotationTransactionAttributeSource()));
        var service=(StoredValueService)factory.getProxy();
        assertThatThrownBy(()->service.payTicketOrder("customer","order","card","13800138000","123456"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("订单写入失败");
        assertThat(customers.findStoredValueAccount("customer",null,"merchant").orElseThrow().balanceCent()).isEqualTo(5000L);
        assertThat(customers.listStoredValueTransactions("customer","card",1,20)).isEmpty();
    }
}
