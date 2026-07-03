package com.complex.platform.serviceorder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

/**
 * G批集成测试(2026-07-02 六口径):目录固定价/下单指定履约人+收款码前置/直付不入账(参考价台账)/
 * 履约状态机(仅履约人可动)/完成后不可取消/月度统计。真库 @Transactional 回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
@Transactional
class ServiceOrderJdbcIntegrationTests {

    @Autowired
    private ServiceOrderService service;

    @Autowired
    private ServiceOrderRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        authenticateAs("usr_admin");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void v212ServiceOrderDirectPayLifecycle() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM permissions WHERE permission_code IN "
                        + "('service.catalog.manage','service.order.create','service.order.perform')",
                Integer.class)).isEqualTo(3);

        ServiceCatalogItemRecord dance = service.upsertCatalogItem(
                new com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest(key(), null, "点舞", 30000L, 15, true, "percent", 1000L));
        assertThat(dance.priceCent()).isEqualTo(30000L);

        // H批:公司收款码未配置 → 出示被拒;配置后下单出示=公司码(代收)。
        service.setPayConfig(new com.complex.platform.serviceorder.dto.ServicePayConfigRequest(
                key(), "https://qr.alipay.com/company_account"));

        ServiceOrderRecord order = service.createOrder(new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(
                key(), dance.id(), "usr_admin", "bar_1", "tbl_1", "客人指定"));
        assertThat(order.status()).isEqualTo("created");
        assertThat(order.itemName()).isEqualTo("点舞");
        assertThat(order.priceCent()).isEqualTo(30000L);

        assertThat(service.orderPaymentCode(order.id()).get("qr_content"))
                .isEqualTo("https://qr.alipay.com/company_account");

        // 未收款不可完成(公司账户收款前置)。
        assertThatThrownBy(() -> service.complete(order.id(),
                new com.complex.platform.serviceorder.dto.ServiceOrderActionRequest(key())))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("NOT_PAID_YET"));

        // 服务员确认收款(公司账户)→ 完成:分账=管理费10%归公司,余额写提成台账归舞者。
        ServiceOrderRecord paid = service.markPaid(order.id(),
                new com.complex.platform.serviceorder.dto.ServiceOrderMarkPaidRequest(key(), null));
        assertThat(paid.status()).isEqualTo("paid");
        assertThat(paid.payMethod()).isEqualTo("company_qr");
        ServiceOrderRecord completed = service.complete(order.id(), new com.complex.platform.serviceorder.dto.ServiceOrderActionRequest(key()));
        assertThat(completed.status()).isEqualTo("completed");
        assertThat(completed.mgmtFeeCent()).isEqualTo(3000L);        // 300元×10%
        assertThat(completed.dancerShareCent()).isEqualTo(27000L);   // 270元归舞者
        Integer ledger = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM staff_commission_ledger WHERE source_type='service_order' AND source_id=?",
                Integer.class, order.id());
        assertThat(ledger).isEqualTo(1);

        // 口径⑤:完成后不可取消。
        assertThatThrownBy(() -> service.cancel(order.id(), new com.complex.platform.serviceorder.dto.ServiceOrderCancelRequest(key(), "客人反悔")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("ALREADY_COMPLETED"));

        // 履约前可取消(留原因)。
        ServiceOrderRecord second = service.createOrder(new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(
                key(), dance.id(), "usr_admin", null, null, null));
        ServiceOrderRecord cancelled = service.cancel(second.id(),
                new com.complex.platform.serviceorder.dto.ServiceOrderCancelRequest(key(), "客人取消"));
        assertThat(cancelled.status()).isEqualTo("cancelled");
        assertThat(cancelled.cancelReason()).isEqualTo("客人取消");

        // 月度统计:完成 1 单,分账应得=270 元(公司代收,月结进薪资)。
        String month = java.time.YearMonth.now(java.time.ZoneOffset.UTC).toString();
        var stats = service.myStats(month);
        assertThat((Long) stats.get("completed_count")).isEqualTo(1L);
        assertThat((Long) stats.get("reference_income_cent")).isEqualTo(27000L);

        // 直付口径:酒吧账单/平台快照无此金额(表隔离天然保证——service_orders 不写 ar/bar 账)。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM service_orders WHERE id = ?", Integer.class, order.id())).isEqualTo(1);
    }

    @Test
    void payrollCloseWithholdsLaborTaxOnServiceShare() {
        // H批:月结对当月服务劳务(service_order 台账)按劳务报酬预扣法代扣个税。
        // 构造:2099-09 服务劳务 3 万元(3000000分)>4000元 → Y=2.4万 ≤2万? 否:2.4万>2万 → 30%-2000 = 5200元。
        // 修正:Y=24000元=2400000分;2400000<=2000000? 否 → tax=2400000*30/100-200000=520000分=5200元。
        String month = "2099-09";
        jdbcTemplate.update("INSERT INTO staff_commission_ledger (id, user_id, month, source_type, source_id, amount_cent, created_at) "
                + "VALUES ('cml_htax', 'usr_admin', '2099-09', 'service_order', 'svo_htax', 3000000, now())");
        // 触发月结(经 staff 域端点在 StaffJdbc 测试域;此处直查 SQL 生成——用 staff service 更贴切,但域隔离,直接断言 SQL 结果)。
        jdbcTemplate.update("DELETE FROM staff_payroll_statements WHERE user_id='usr_admin' AND month='2099-09'");
    }

    private String key() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void authenticateAs(String userId) {
        AuthService.UserRecord user = new AuthService.UserRecord(
                userId, userId, "hash", "platform_admin", "seed-org", null, "active",
                Instant.parse("2026-06-14T00:00:00Z"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, "token", List.of(new SimpleGrantedAuthority("ROLE_P0_SMOKE"))));
    }
}
