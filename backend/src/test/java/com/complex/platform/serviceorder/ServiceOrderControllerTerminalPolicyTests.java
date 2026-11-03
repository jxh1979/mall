package com.complex.platform.serviceorder;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 2026-07-03 用户裁决:mark-paid/complete 现场资金动作禁止私人手机,仅公司设备。 */
class ServiceOrderControllerTerminalPolicyTests {

    private static final Set<String> ALLOWED = Set.of("staff_work_phone", "web");

    @Test
    void privatePhoneIsRejected() {
        assertThatThrownBy(() -> ServiceOrderController.requireCompanyTerminal("staff_private_phone", ALLOWED))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.code()).isEqualTo("TERMINAL_NOT_ALLOWED"));
    }

    @Test
    void companyDevicesPass() {
        assertThatCode(() -> ServiceOrderController.requireCompanyTerminal("staff_work_phone", ALLOWED))
                .doesNotThrowAnyException();
        assertThatCode(() -> ServiceOrderController.requireCompanyTerminal("web", ALLOWED))
                .doesNotThrowAnyException();
    }

    @Test
    void absentOrBlankHeaderIsRejected() {
        // 清单 P1(2026-07-03 审计):现场资金动作 fail-closed——缺省/空白终端头不再放行
        // (正规客户端 web/staff_work_phone 均发标识,只挡无标识绕过)。
        assertThatThrownBy(() -> ServiceOrderController.requireCompanyTerminal(null, ALLOWED))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.code()).isEqualTo("TERMINAL_NOT_ALLOWED"));
        assertThatThrownBy(() -> ServiceOrderController.requireCompanyTerminal("  ", ALLOWED))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.code()).isEqualTo("TERMINAL_NOT_ALLOWED"));
    }

    @Test
    void createAndPaymentCodeRejectPrivatePhone() {
        assertThatThrownBy(() -> ServiceOrderController.requireCompanyTerminal(
                "staff_private_phone", ServiceOrderController.CREATE_AND_CODE_TERMINALS))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.code()).isEqualTo("TERMINAL_NOT_ALLOWED"));
    }

    /** 第十八批 O 段第 130 条（BSM:1132「……一律在公司工作手机上办理……」；BSM:1901 平板「只做酒吧点酒下单」）：平板不办服务点单的现场动作。 */
    @Test
    void tabletIsRejectedForOnSiteServiceOrderActions() {
        for (Set<String> allowed : java.util.List.of(ServiceOrderController.MARK_PAID_TERMINALS,
                ServiceOrderController.CREATE_AND_CODE_TERMINALS)) {
            assertThatThrownBy(() -> ServiceOrderController.requireCompanyTerminal("tablet", allowed))
                    .isInstanceOfSatisfying(ApiException.class,
                            e -> org.assertj.core.api.Assertions.assertThat(e.code()).isEqualTo("TERMINAL_NOT_ALLOWED"));
        }
    }

    @Test
    void createAndPaymentCodeAllowWorkPhone() {
        assertThatCode(() -> ServiceOrderController.requireCompanyTerminal(
                "staff_work_phone", ServiceOrderController.CREATE_AND_CODE_TERMINALS))
                .doesNotThrowAnyException();
    }
}
