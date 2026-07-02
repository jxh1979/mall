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
                new com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest(key(), null, "点舞", 30000L, 15, true));
        assertThat(dance.priceCent()).isEqualTo(30000L);

        // 履约人未录收款码 → 下单被拒(直付前置)。
        assertThatThrownBy(() -> service.createOrder(new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(
                key(), dance.id(), "usr_admin", "bar_1", "tbl_1", null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("PERFORMER_NO_PAYMENT_CODE"));

        service.setMyPaymentCode(new com.complex.platform.serviceorder.dto.StaffPaymentCodeRequest(key(), "https://qr.alipay.com/test_fixed_code"));

        ServiceOrderRecord order = service.createOrder(new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(
                key(), dance.id(), "usr_admin", "bar_1", "tbl_1", "客人指定"));
        assertThat(order.status()).isEqualTo("pending_accept");
        assertThat(order.itemName()).isEqualTo("点舞");
        assertThat(order.priceCent()).isEqualTo(30000L);

        // 收款码出示 = 履约人录入的原文(客人扫码直付)。
        assertThat(service.orderPaymentCode(order.id()).get("qr_content"))
                .isEqualTo("https://qr.alipay.com/test_fixed_code");

        // 状态机:接单→完成(仅履约人)。
        ServiceOrderRecord accepted = service.accept(order.id(), new com.complex.platform.serviceorder.dto.ServiceOrderActionRequest(key()));
        assertThat(accepted.status()).isEqualTo("accepted");
        ServiceOrderRecord completed = service.complete(order.id(), new com.complex.platform.serviceorder.dto.ServiceOrderActionRequest(key()));
        assertThat(completed.status()).isEqualTo("completed");
        assertThat(completed.completedAt()).isNotNull();

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

        // 月度统计:完成 1 单,参考收入=300 元(直付台账,非营收)。
        String month = java.time.YearMonth.now(java.time.ZoneOffset.UTC).toString();
        var stats = service.myStats(month);
        assertThat((Long) stats.get("completed_count")).isEqualTo(1L);
        assertThat((Long) stats.get("reference_income_cent")).isEqualTo(30000L);

        // 直付口径:酒吧账单/平台快照无此金额(表隔离天然保证——service_orders 不写 ar/bar 账)。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM service_orders WHERE id = ?", Integer.class, order.id())).isEqualTo(1);
    }

    @Test
    void onlyAssignedPerformerCanActOnOrder() {
        service.setMyPaymentCode(new com.complex.platform.serviceorder.dto.StaffPaymentCodeRequest(key(), "https://qr.alipay.com/x"));
        ServiceCatalogItemRecord item = service.upsertCatalogItem(
                new com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest(key(), null, "专属服务", 10000L, 10, true));
        ServiceOrderRecord order = service.createOrder(new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(
                key(), item.id(), "usr_admin", null, null, null));

        authenticateAs("usr_other");
        assertThatThrownBy(() -> service.accept(order.id(), new com.complex.platform.serviceorder.dto.ServiceOrderActionRequest(key())))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("NOT_PERFORMER"));
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
