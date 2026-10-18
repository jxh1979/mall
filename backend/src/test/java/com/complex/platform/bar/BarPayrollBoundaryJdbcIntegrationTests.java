package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class BarPayrollBoundaryJdbcIntegrationTests {
    @Autowired JdbcBarRepository repository;
    @Autowired BarBillingService billing;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach void actor() { TestSecurityActors.asPlatformAdmin(); }
    @AfterEach void clear() { TestSecurityActors.clear(); }

    @Test void counterNeverCollectsCardSeatServiceFee() {
        var bill = bill("bar_counter", 0L);
        assertThatThrownBy(() -> billing.applyBillAdjustment(bill.id(),
                new BarBillAdjustmentRequest("counter-fee", null, List.of(), "service_fee", 100L, "服务费", null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException)e).code()).isEqualTo("BAR_SERVICE_FEE_TABLE_NOT_ELIGIBLE");
        assertThat(repository.findBillById(bill.id()).orElseThrow().payableAmountCent()).isEqualTo(1000L);
        assertThat(repository.listBillAdjustments(bill.id())).isEmpty();
    }

    @Test void cashTipsAreNotRecordedAsCompanyCollectedPayrollIncome() {
        var bill = bill("card_seat", 200L);
        assertThatThrownBy(() -> billing.settleBill(bill.id(),
                new BarBillSettleRequest("cash-tip", null, List.of(), "cash-payment", 1200L, "cash")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException)e).code()).isEqualTo("BAR_CASH_TIP_DIRECT_TO_STAFF");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_bill_settlements WHERE bill_id=?", Integer.class, bill.id())).isZero();
        assertThat(repository.findBillById(bill.id()).orElseThrow().paidAmountCent()).isZero();
    }

    private BarBillRecord bill(String type, long tip) {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        Instant now = Instant.now();
        String table = "bpt_" + suffix, order = "bpo_" + suffix;
        repository.saveTable(new BarTableRecord(table, "bar_payroll_boundary", null, "T" + suffix,
                "薪资边界台位", type, 4, 1, 0, "CNY", "active", now, now));
        repository.saveOrder(new BarOrderRecord(order, "bar_payroll_boundary", null, table, null, null,
                "staff", "created", 1000, "CNY", "web", null, null, now, now));
        return repository.saveBill(new BarBillRecord("bpb_" + suffix, "B" + suffix, order,
                "bar_payroll_boundary", null, table, null, 1000, 0, 0, 1000, 0, "CNY", "unpaid", "unsettled",
                null, null, now, now, false, null, null, null, 0, tip));
    }
}
