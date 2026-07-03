package com.complex.platform.serviceorder;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 2026-07-03 用户裁决:mark-paid/complete 现场资金动作禁止私人手机,仅公司设备。 */
class ServiceOrderControllerTerminalPolicyTests {

    private static final Set<String> ALLOWED = Set.of("staff_work_phone", "web", "tablet");

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
        // (正规客户端 web/staff_work_phone/tablet 均发标识,只挡无标识绕过)。
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

    @Test
    void createAndPaymentCodeAllowWorkPhone() {
        assertThatCode(() -> ServiceOrderController.requireCompanyTerminal(
                "staff_work_phone", ServiceOrderController.CREATE_AND_CODE_TERMINALS))
                .doesNotThrowAnyException();
    }
}
