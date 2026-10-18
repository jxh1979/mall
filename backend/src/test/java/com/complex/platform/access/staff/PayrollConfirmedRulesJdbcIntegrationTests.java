package com.complex.platform.access.staff;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.auth.AuthService;
import com.complex.platform.common.api.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
    "spring.datasource.username=complex",
    "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"})
@Transactional
class PayrollConfirmedRulesJdbcIntegrationTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired JdbcStaffRepository repository;
    @Autowired StaffService staff;
    @Autowired AccessRepository access;

    @BeforeEach void before() {
        StaffTestActors.grant(jdbc, access, "usr_admin", "project_main");
        var actor = new AuthService.UserRecord("usr_admin", "seed-admin", "hash", "platform_admin",
            "seed-org", null, "active", Instant.parse("2026-06-14T00:00:00Z"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            actor, "token", List.of(new SimpleGrantedAuthority("ROLE_P0_SMOKE"))));
        StaffTestActors.alignRowToSession(jdbc);
    }
    @AfterEach void after() { SecurityContextHolder.clearContext(); }
    static String key() { return UUID.randomUUID().toString().replace("-", ""); }
    String employee() {
        String user = "u_" + key();
        jdbc.update("INSERT INTO staff_profiles(id,project_id,user_id,name,company_id,registration_status,status,created_at,updated_at) VALUES(?,'project_main',?,'工资回归','project_main','approved','active',now(),now())",
            "p_" + key(), user);
        return user;
    }

    @Test void oldRepositoryCannotCloseWithoutApprovedCalculationFacts() {
        String user = employee();
        repository.upsertBaseSalary(user,500000,"usr_admin",Instant.now());
        assertThatThrownBy(()->repository.closePayrollMonth("2098-07","usr_admin",Instant.now(),"project_main"))
            .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("PAYROLL_CONFIRMED_FACTS_REQUIRED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_payroll_statements WHERE user_id=?",Integer.class,user)).isZero();
    }

    @Test void humanResourcesCannotMarkPaidWithoutFinanceSuccess() {
        String user = employee();
        String statement = "pay_"+key();
        jdbc.update("INSERT INTO staff_payroll_statements(id,user_id,month,base_cent,total_cent,status,project_id,payer_company_id) VALUES(?,?,'2098-08',500000,500000,'closed','project_main','project_main')",
            statement,user);
        assertThatThrownBy(() -> staff.markPayrollPaid(new com.complex.platform.access.staff.dto.StaffPayrollPayRequest(
            key(),"2098-08",user,"bank_transfer","人工输入不是银行成功")))
            .isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.code()).isEqualTo("PAYROLL_FINANCE_RESULT_REQUIRED"));
        assertThat(jdbc.queryForObject("SELECT status FROM staff_payroll_statements WHERE id=?",String.class,statement)).isEqualTo("closed");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_payroll_payouts WHERE statement_id=?",Integer.class,statement)).isZero();
    }
}
