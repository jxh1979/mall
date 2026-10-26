package com.complex.platform.analytics;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.support.TestSecurityActors;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * ROOT-04-R2 采购到货（BSM「经营看板」章「主要指标」：「供应链：采购到货、库存余额、吧台消耗、缺货预警、盘点差异、存酒余额。」；
 * BSM 采购条「逐次记录采购、实到、合格接收和拒收数量，只有合格部分进入可用库存」；BSM「卡片明细对账」；
 * 实施台账 latestConfirmedDecisions.M01「采购到货按商品及单位分别显示」）：
 * 按商品及单位分列合格接收、实到、拒收与到货次数，不同单位不相加；按实际到货时刻落库存营业日；明细到每一次到货；项目级只见本项目。
 *
 * <p>到货行按 V485 purchase_receipts 的形状直接写入（一次实际到货一行，实到＝合格＋拒收；正式写点 PurchaseWorkflowService.receive
 * 写出的行另见 PurchaseInboundPayableJdbcIntegrationTests 的采购到货用例）。真库、随测试事务回滚；日期取很靠后的营业日。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class PurchaseReceivedMetricJdbcIntegrationTests {

    private static final LocalDate D = LocalDate.of(2097, 3, 10);

    @Autowired private AnalyticsService service;
    @Autowired private BusinessDayResolver businessDayResolver;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String projectA = "prj_rcv_a_" + suffix;
    private final String projectB = "prj_rcv_b_" + suffix;
    private final String supplier = "sup_rcv_" + suffix;
    private final String wine = "item_rcv_wine_" + suffix;
    private final String device = "item_rcv_dev_" + suffix;
    private final String user = "usr_rcv_" + suffix;

    @BeforeEach
    void setUp() {
        TestSecurityActors.asPlatformAdmin();
        for (String p : List.of(projectA, projectB)) {
            jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())", p, p);
            jdbc.update("INSERT INTO warehouses (id, project_id, warehouse_type, name, status) VALUES (?, ?, 'main', '主仓', 'active')", "wh_" + p, p);
        }
        jdbc.update("INSERT INTO inventory_suppliers (id, supplier_name, status, created_at, updated_at) VALUES (?, '到货供应商', 'active', now(), now())", supplier);
        jdbc.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, created_at, updated_at) VALUES (?, '酒', 'beverage', '瓶', 'active', now(), now())", wine);
        jdbc.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, created_at, updated_at) VALUES (?, '设备', 'equipment', '台', 'active', now(), now())", device);
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, status, created_at) VALUES (?, ?, 'staff', ?, 'active', now())", user, user, projectA);
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    @Test
    void receivedShowsPerItemAndUnitWithoutCrossUnitSum() {
        Instant inDay = businessDayResolver.start(null, null, D).plusSeconds(3600);
        receipt("a_wine", projectA, wine, 12, 10, 2, inDay);
        receipt("a_dev", projectA, device, 5, 5, 0, inDay.plusSeconds(60));
        receipt("b_wine", projectB, wine, 3, 3, 0, inDay.plusSeconds(120));
        receipt("prev_wine", projectA, wine, 1, 1, 0, businessDayResolver.start(null, null, D).minusSeconds(60)); // 前一营业日

        AnalyticsRecords.MetricValue received = service.overview(Optional.empty(), D).metrics().get("supply.received");
        assertThat(received.dimension()).isNotNull();
        // 不出标量：卡片值为空，分组里只有各商品各自单位的数量，没有任何跨单位合计（不会出现 酒+设备 的 18）。
        assertThat(received.value()).isNull();
        assertThat(received.dimension()).containsOnlyKeys("groups");
        Map<String, Map<String, Object>> platform = byItem(groups(received.dimension().get("groups")));
        assertThat(platform).containsOnlyKeys("酒", "设备");
        assertThat(platform.get("酒")).containsEntry("unit", "瓶").containsEntry("acceptedQuantity", 13L)
                .containsEntry("arrivedQuantity", 15L).containsEntry("rejectedQuantity", 2L).containsEntry("receiptCount", 2L);
        assertThat(platform.get("设备")).containsEntry("unit", "台").containsEntry("acceptedQuantity", 5L)
                .containsEntry("arrivedQuantity", 5L).containsEntry("rejectedQuantity", 0L).containsEntry("receiptCount", 1L);

        // 明细到每一次到货；项目级只见本项目；分组与明细同谓词，合计不出标量。
        AnalyticsRecords.MetricDetailResponse detail = service.metricDetail("supply.received", null, D, D, 1, 20, Set.of(projectA));
        assertThat(detail.totalRows()).isEqualTo(2);
        assertThat(detail.rows()).extracting(row -> row.get("projectId")).containsOnly(projectA);
        assertThat(detail.summaryValue()).isNull();
        assertThat(detail.groupColumns()).extracting(MetricRegistry.MetricColumn::key)
                .containsExactly("itemName", "unit", "acceptedQuantity", "arrivedQuantity", "rejectedQuantity", "receiptCount");
        Map<String, Map<String, Object>> scoped = byItem(detail.groups());
        assertThat(scoped.get("酒")).containsEntry("acceptedQuantity", 10L).containsEntry("rejectedQuantity", 2L);
        assertThat(scoped.get("设备")).containsEntry("acceptedQuantity", 5L);
    }

    private void receipt(String tag, String project, String item, int arrived, int accepted, int rejected, Instant receivedAt) {
        String purchase = "pr_rcv_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO inventory_purchase_requests (id, request_no, supplier_id, item_id, quantity, status, project_id, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, 'arrived', ?, now(), now())", purchase, purchase, supplier, item, arrived, project);
        jdbc.update("""
                INSERT INTO purchase_receipts (id, purchase_id, project_id, supplier_id, item_id, warehouse_id, location_id,
                        arrived_quantity, accepted_quantity, rejected_quantity, unit_cost_cent, received_at, received_by, evidence_attachment_ids)
                VALUES (?, ?, ?, ?, ?, ?, 'loc_main', ?, ?, ?, 100, ?, ?, '[]'::jsonb)
                """, "prec_" + tag + "_" + suffix, purchase, project, supplier, item, "wh_" + project,
                arrived, accepted, rejected, Timestamp.from(receivedAt), user);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> groups(Object value) {
        return (List<Map<String, Object>>) value;
    }

    private static Map<String, Map<String, Object>> byItem(List<Map<String, Object>> groups) {
        return groups.stream().collect(Collectors.toMap(g -> (String) g.get("itemName"), g -> g));
    }
}
