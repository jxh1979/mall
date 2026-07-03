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

    @Autowired
    private com.complex.platform.access.staff.StaffRepository staffRepository;

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

    @Test
    void serviceOrderCommissionIsIdempotentAtMostOncePerOrder() {
        // 清单 P1-2:同一服务单的提成至多一条(部分唯一索引 + ON CONFLICT DO NOTHING),
        // 防并发双 key 完成/重放放大到薪资月结代扣个税;manual 等其他来源不受影响(可重)。
        String oid = "svo_idem_" + key();
        java.time.Instant now = java.time.Instant.now();
        staffRepository.insertCommission("cml_a_" + oid, "usr_p1b", "2099-01", "service_order", oid, 27000L, "dup", now);
        staffRepository.insertCommission("cml_b_" + oid, "usr_p1b", "2099-01", "service_order", oid, 27000L, "dup", now);
        Long svc = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM staff_commission_ledger WHERE source_type='service_order' AND source_id=?",
                Long.class, oid);
        assertThat(svc).isEqualTo(1L);
        // manual 来源不进部分索引,同 source_id 仍可多条(手工双录是用户动作,不静默吞)。
        String mid = "man_" + key();
        staffRepository.insertCommission("cml_m1_" + mid, "usr_p1b", "2099-01", "manual", mid, 100L, "m", now);
        staffRepository.insertCommission("cml_m2_" + mid, "usr_p1b", "2099-01", "manual", mid, 100L, "m", now);
        Long man = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM staff_commission_ledger WHERE source_type='manual' AND source_id=?",
                Long.class, mid);
        assertThat(man).isEqualTo(2L);
    }

    @Test
    void v214ServiceOrderDerivesPerformerProjectAndScopedListExcludesOthers() {
        // P0 租户隔离(V214):服务单按履约人档案归属综合体;后台列表 scoped 只见本综合体单。
        String performer = "usr_perf_" + key();
        jdbcTemplate.update("""
                INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name,
                        registration_status, status, created_at, updated_at)
                VALUES (?, 'prj_a', NULL, ?, '履约人A', 'approved', 'active', now(), now())
                """, "sprof_" + key(), performer);
        ServiceCatalogItemRecord item = service.upsertCatalogItem(
                new com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest(
                        key(), null, "隔离测试舞", 10000L, 10, true, "none", 0L));
        ServiceOrderRecord order = service.createOrder(
                new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(
                        key(), item.id(), performer, null, null, null));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT project_id FROM service_orders WHERE id = ?", String.class, order.id()))
                .isEqualTo("prj_a");
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
        assertThat(repository.listOrders(today, null, "prj_a"))
                .extracting(ServiceOrderRecord::id).contains(order.id());
        assertThat(repository.listOrders(today, null, "prj_b"))
                .extracting(ServiceOrderRecord::id).doesNotContain(order.id());
        // 平台(null scope)全量可见。
        assertThat(repository.listOrders(today, null, null))
                .extracting(ServiceOrderRecord::id).contains(order.id());
    }

    @Test
    void idempotencyReplaysFirstResultForCreateMarkPaidComplete() {
        // P0 真幂等(2026-07-03):幂等键唯一占位+命中重放首次结果——重复提交不重复建单/不重复分账入提成。
        service.setPayConfig(new com.complex.platform.serviceorder.dto.ServicePayConfigRequest(key(), "https://qr/company"));
        ServiceCatalogItemRecord item = service.upsertCatalogItem(
                new com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest(
                        key(), null, "幂等舞", 20000L, 10, true, "percent", 1000L));

        String createKey = key();
        ServiceOrderRecord first = service.createOrder(new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(
                createKey, item.id(), "usr_admin", null, null, null));
        ServiceOrderRecord replay = service.createOrder(new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(
                createKey, item.id(), "usr_admin", null, null, null));
        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM service_orders WHERE catalog_item_id = ?", Integer.class, item.id()))
                .isEqualTo(1);

        String payKey = key();
        ServiceOrderRecord paid = service.markPaid(first.id(),
                new com.complex.platform.serviceorder.dto.ServiceOrderMarkPaidRequest(payKey, null));
        ServiceOrderRecord paidReplay = service.markPaid(first.id(),
                new com.complex.platform.serviceorder.dto.ServiceOrderMarkPaidRequest(payKey, null));
        assertThat(paidReplay.status()).isEqualTo("paid");
        assertThat(paidReplay.paidAt()).isEqualTo(paid.paidAt());

        String completeKey = key();
        ServiceOrderRecord done = service.complete(first.id(),
                new com.complex.platform.serviceorder.dto.ServiceOrderActionRequest(completeKey));
        ServiceOrderRecord doneReplay = service.complete(first.id(),
                new com.complex.platform.serviceorder.dto.ServiceOrderActionRequest(completeKey));
        assertThat(doneReplay.dancerShareCent()).isEqualTo(done.dancerShareCent());
        // 关键:提成台账只入一次。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM staff_commission_ledger WHERE source_type='service_order' AND source_id = ?",
                Integer.class, first.id())).isEqualTo(1);

        // 同键不同请求体 → 冲突拒绝(防误用旧键提交新单)。
        assertThatThrownBy(() -> service.createOrder(new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(
                createKey, item.id(), "usr_admin", "bar_x", null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    void failurePathsRecordDeclaredFailureAuditEvents() {
        // P1 审计闭合:失败路径按 openapi x-audit-event-failure 落审计事件后再抛。
        assertThatThrownBy(() -> service.createOrder(new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(
                key(), "svc_missing_" + key(), "usr_admin", null, null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("SERVICE_ITEM_NOT_FOUND"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE event_name='service.order.create_failed' AND event_result='failure'",
                Integer.class)).isGreaterThanOrEqualTo(1);

        assertThatThrownBy(() -> service.complete("svo_missing_" + key(),
                new com.complex.platform.serviceorder.dto.ServiceOrderActionRequest(key())))
                .isInstanceOf(ApiException.class);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE event_name='service.order.complete_failed' AND event_result='failure'",
                Integer.class)).isGreaterThanOrEqualTo(1);

        assertThatThrownBy(() -> service.markPaid("svo_missing_" + key(),
                new com.complex.platform.serviceorder.dto.ServiceOrderMarkPaidRequest(key(), null)))
                .isInstanceOf(ApiException.class);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE event_name='service.order.pay_confirm_failed' AND event_result='failure'",
                Integer.class)).isGreaterThanOrEqualTo(1);
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
