package com.complex.platform.gate;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 第十八批 M 段第 116 条（PGM:747「……员工自有手机只开放低风险功能（公告、我的工单、帮客人订位、查看考勤、请假、查自己薪资等）。」；
 * BSM:1894-1896「……必须在现场完成的操作（代客下单、出示公司收款码、完成履约、扫码核销）在公司工作手机上做」）：私人手机接口清单。
 */
class StaffAppFunctionsTests {

    @Test
    void privatePhoneAllowsLowRiskOffSiteCalls() {
        for (String[] call : new String[][] {
                {"GET", "/api/v1/staff/leaves/mine"},
                {"GET", "/api/v1/staff/payroll/mine"},
                {"GET", "/api/v1/staff/payroll/mine/statements/s1"},
                {"POST", "/api/v1/staff/shifts"},
                {"POST", "/api/v1/bar/reservations"},
                {"POST", "/api/v1/facility/work-orders/w1/review"},
                {"GET", "/api/v1/merchant-service/work-orders/assigned-to-me"},
                {"POST", "/api/v1/staff/lockers/scan-token"}}) {
            assertThat(StaffAppFunctions.privatePhoneAllows(call[0], call[1])).as(call[0] + " " + call[1]).isTrue();
        }
    }

    @Test
    void privatePhoneRejectsOnSiteAndManagementCalls() {
        for (String[] call : new String[][] {
                {"POST", "/api/v1/staff/leaves/x/review"},
                {"GET", "/api/v1/staff/shifts"},
                {"GET", "/api/v1/bar/reservations"},
                {"POST", "/api/v1/facility/work-orders/w1/assign"},
                {"POST", "/api/v1/staff/registrations"},
                {"GET", "/api/v1/services/payment-code/mine"},
                {"POST", "/api/v1/vouchers/redemptions/preview"},
                {"POST", "/api/v1/vouchers/redemptions"}}) {
            assertThat(StaffAppFunctions.privatePhoneAllows(call[0], call[1])).as(call[0] + " " + call[1]).isFalse();
        }
    }

    /**
     * 第十八批 M 段第 112 条：工作手机「礼物/券核销」（112-R1）与「一体机模式（权益领取）」（112-R2）是公司设备的入口；
     * 扫码核销属必须在公司工作手机上做的现场操作（BSM:931、BSM:1896），不给私人手机。
     */
    @Test
    void voucherRedeemAndKioskClaimAreCompanyDeviceFunctions() {
        assertThat(StaffAppFunctions.ALL).contains("voucher_redeem", "kiosk_claim");
        assertThat(StaffAppFunctions.functionsFor("staff_private_phone")).doesNotContain("voucher_redeem", "kiosk_claim");
    }

    @Test
    void otherTerminalsGetAllFunctions() {
        assertThat(StaffAppFunctions.functionsFor("staff_work_phone")).isEqualTo(StaffAppFunctions.ALL);
        assertThat(StaffAppFunctions.functionsFor(null)).isEqualTo(StaffAppFunctions.ALL);
    }
}
