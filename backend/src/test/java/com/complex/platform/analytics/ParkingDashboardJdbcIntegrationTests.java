package com.complex.platform.analytics;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.common.BusinessDayResolver;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * HP-P01 R10 真库：BSM「停车场」章「连接经营看板，车位使用率、日周转次数、停车收入、抵免占比、平均停留时长与车位出租率并列为地产指标」；
 * BSM「出租」条「车位出租率单独列（月租和员工车位占用计入）」。第十二批操作员裁决补充第 5 条（月租按合同已租车位个数；员工按已登记且在职的员工车辆数，
 * 一辆算一个车位）、第 6 条（抵免占比＝当日用了消费抵免的临停离场车次 ÷ 当日临停离场车次）、第 7 条（停车收入＝临停停车费实收＋当期车位月租账单金额）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class ParkingDashboardJdbcIntegrationTests {

    @Autowired private JdbcAnalyticsRepository repository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private BusinessDayResolver businessDays;

    private final String s = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "pkd_prj_" + s;
    private LocalDate day;
    private Instant start;

    @BeforeEach
    void seed() {
        day = businessDays.businessDate(null, null, Instant.now()).minusDays(1);
        start = businessDays.start(null, null, day);
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','停车看板回归','active',now(),now())",
                project);
    }

    @Test
    void parkingDayFactsFromSessionsSnapshotsPaymentsAndMonthlyBills() {
        // 车位占用：两次回传 40/100、60/100 → 使用率 100/200；最近一次回传车位 100。
        snapshot(1, 100, 40);
        snapshot(2, 100, 60);
        // 当日入场 3 车次；当日离场 4 车次（临停 3、员工 1），其中临停 1 车次用了消费抵免；停留 60＋120＋30＋180 分钟。
        String credited = session("a", "temporary", start.plus(Duration.ofHours(1)), 60);
        String other = session("b", "temporary", start.plus(Duration.ofHours(2)), 120);
        String staff = session("c", "staff", start.plus(Duration.ofHours(3)), 30);
        session("d", "temporary", start.minus(Duration.ofHours(1)), 180);
        usedCredit(credited);
        // 临停停车费实收：平台收 500 分、停车系统自收 300 分（平台收款的回报不重复计）；
        // 员工车辆折扣缴费（平台收 200 分、停车系统自收 100 分）不是临停，不计入（第 7 条「临停停车费实收」）。
        platformPayment("ppay_pkd_" + s, credited, 500);
        platformPayment("ppay_pkd_staff_" + s, staff, 200);
        vendorPayment("v1", 300, other, null);
        vendorPayment("v2", 500, credited, "ppay_pkd_" + s);
        vendorPayment("v3", 100, staff, null);
        // 停车系统自收但回报没带停车记录号（session_ref 选填）的 70 分：实收停车费，按临停计入。
        vendorPayment("v4", 70, null, null);
        // 当期车位月租账单 200 元（当日到期）。
        monthlyBill(20_000);

        AnalyticsRecords.ParkingDayFacts facts = repository.parkingDayFacts(day).get(project);
        assertThat(facts).isNotNull();
        assertThat(facts.snapshotOccupied()).isEqualTo(100);
        assertThat(facts.snapshotSpaces()).isEqualTo(200);
        assertThat(facts.latestSpaces()).isEqualTo(100);
        assertThat(facts.entries()).isEqualTo(3);
        assertThat(facts.exits()).isEqualTo(4);
        assertThat(facts.temporaryExits()).isEqualTo(3);
        assertThat(facts.creditExits()).isEqualTo(1);
        assertThat(facts.stayMinutes()).isEqualTo(390);
        assertThat(facts.revenueCent()).isEqualTo(800 + 70 + 20_000);
    }

    /** 裁决补充第 7 条括注「车位月租照旧是合同账单，不改其收入归集」；ENUM L256 车位月租与固定租金、物业费、能源费并列：当日到期的车位月租应收计入合同租金挂账。 */
    @Test
    void parkingMonthlyBillCountsAsContractRent() {
        java.math.BigDecimal before = repository.sumContractRentReceivableByMerchant(day, "platform")
                .getOrDefault("mch_pkd", java.math.BigDecimal.ZERO);
        monthlyBill(20_000);
        java.math.BigDecimal after = repository.sumContractRentReceivableByMerchant(day, "platform")
                .getOrDefault("mch_pkd", java.math.BigDecimal.ZERO);
        assertThat(after.subtract(before)).isEqualByComparingTo("20000");
    }

    /** 车位出租率：分子＝合同已租车位个数＋已登记且在职的员工车辆数，分母＝车位个数（不可租除外）；按个数计、不按面积。 */
    @Test
    void parkingRentalCountsLetSpacesAndActiveStaffVehicles() {
        LocalDate asOf = LocalDate.now(com.complex.platform.common.LocalZone.ID);
        AnalyticsRecords.OccupancyAreas baseline = repository.occupancyAreas(asOf);
        space("pk_let", "35");
        space("pk_free", "12");
        jdbc.update("""
                INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, expiry_date, lifecycle_status, created_at, updated_at)
                VALUES (?, 'mch_pkd', 'subj_pkd', 'subj_pkd', ?, ?, 'effective', now(), now())""", "ct_pkd_" + s, asOf.minusDays(5), asOf.plusDays(30));
        jdbc.update("INSERT INTO contract_spaces (contract_id, space_id) VALUES (?, ?)", "ct_pkd_" + s, "sp_pkd_pk_let_" + s);
        staffVehicle("on", "active", "active");
        staffVehicle("left", "active", "inactive");
        staffVehicle("revoked", "revoked", "active");

        AnalyticsRecords.OccupancyAreas areas = repository.occupancyAreas(asOf);
        assertThat(areas.parkingSpaces().subtract(baseline.parkingSpaces())).isEqualByComparingTo("2");
        assertThat(areas.parkingRentedSpaces().subtract(baseline.parkingRentedSpaces())).isEqualByComparingTo("2");
    }

    private void snapshot(int hour, int total, int occupied) {
        jdbc.update("INSERT INTO parking_occupancy_snapshots(project_id,reported_at,total_spaces,occupied_total) VALUES(?,?,?,?)",
                project, ts(start.plus(Duration.ofHours(hour))), total, occupied);
    }

    /** 离场时点在当日营业窗口内。 */
    private String session(String tag, String kind, Instant entered, int stayMinutes) {
        String id = "ps_pkd_" + tag + "_" + s;
        Instant exited = entered.plus(Duration.ofMinutes(stayMinutes));
        jdbc.update("""
                INSERT INTO parking_sessions(id,project_id,vendor_session_ref,plate_no,vehicle_kind,entered_at,exited_at,status)
                VALUES(?,?,?,?,?,?,?,'exited')""", id, project, "ref_" + tag, "粤P" + tag.toUpperCase(), kind, ts(entered), ts(exited));
        return id;
    }

    private void usedCredit(String session) {
        String customer = "pkd_cus_" + s;
        jdbc.update("INSERT INTO customers(id,phone,masked_phone,status,created_at,updated_at) VALUES(?,?,'***','active',now(),now())",
                customer, "enc:v1:" + customer);
        jdbc.update("""
                INSERT INTO parking_credits(id,project_id,customer_id,business_date,threshold_cent,credit_minutes,eligible_amount_cent,status,
                    session_id,offset_hours,hour_price_cent,cost_cent,receiver_entity_id,used_at)
                VALUES(?,?,?,?,20000,120,30000,'used',?,1,1000,1000,'be_pkd',?)""",
                "pc_pkd_" + s, project, customer, day, session, ts(start.plus(Duration.ofHours(2))));
    }

    private void platformPayment(String id, String session, long amount) {
        jdbc.update("""
                INSERT INTO parking_payments(id,project_id,session_id,customer_id,amount_cent,payment_intent_id,status,paid_at,grace_until,
                    receiver_entity_id,idempotency_key) VALUES(?,?,?,?,?,?,'paid',?,?,'be_pkd',?)""",
                id, project, session, "pkd_cus_" + s, amount, "pi_" + id, ts(start.plus(Duration.ofHours(2))), ts(start.plus(Duration.ofHours(3))),
                "idem_" + id);
    }

    private void vendorPayment(String tag, long amount, String session, String platformPayment) {
        jdbc.update("""
                INSERT INTO parking_vendor_payments(id,project_id,vendor_payment_ref,session_id,amount_cent,paid_at,platform_payment_id)
                VALUES(?,?,?,?,?,?,?)""", "pvp_pkd_" + tag + "_" + s, project, "vp_" + tag, session, amount, ts(start.plus(Duration.ofHours(4))),
                platformPayment);
    }

    private void monthlyBill(long amount) {
        String contract = "ct_pkm_" + s;
        jdbc.update("""
                INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, expiry_date, lifecycle_status, created_at, updated_at)
                VALUES (?, 'mch_pkd', 'subj_pkd', 'subj_pkd', ?, ?, 'effective', now(), now())""", contract, day.minusDays(10), day.plusDays(300));
        String item = "arap_pkm_" + s;
        jdbc.update("""
                INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,paid_amount_cent,unpaid_amount_cent,
                    currency,due_date,accounting_period,status,created_at,updated_at,merchant_id)
                VALUES(?,?,'receivable','mch_pkd','contract_parking_monthly',?,?,0,?,'CNY',?,?,'pending_collection',now(),now(),'mch_pkd')""",
                item, "PARKING_MONTHLY-" + s, contract + "|" + day.withDayOfMonth(1), amount, amount, day, day.toString().substring(0, 7));
        jdbc.update("""
                INSERT INTO contract_rent_bills(id,contract_id,merchant_id,project_id,period,amount_cent,ar_ap_item_id,created_at,fee_type)
                VALUES(?,?,'mch_pkd',?,?,?,?,now(),'parking_monthly')""", "crb_pkm_" + s, contract, project, day.toString().substring(0, 7),
                amount, item);
    }

    private void space(String tag, String area) {
        jdbc.update("""
                INSERT INTO spaces (id, project_id, space_name, space_type, floor_no, area_square_meter, polygon_points, status, created_at, updated_at)
                VALUES (?, ?, ?, 'parking', 'B1', ?, '[]'::jsonb, 'active', now(), now())""",
                "sp_pkd_" + tag + "_" + s, project, tag, new java.math.BigDecimal(area));
    }

    private void staffVehicle(String tag, String status, String userStatus) {
        String user = "pkd_u_" + tag + "_" + s;
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,status,created_at) VALUES(?,?,'staff',?,?,now())",
                user, user, project, userStatus);
        jdbc.update("""
                INSERT INTO parking_staff_vehicles(id,project_id,user_id,plate_no,status,registered_by) VALUES(?,?,?,?,?,'pkd_admin')""",
                "psv_pkd_" + tag + "_" + s, project, user, "粤S" + tag.toUpperCase(), status);
    }

    private static Timestamp ts(Instant at) {
        return Timestamp.from(at);
    }
}
