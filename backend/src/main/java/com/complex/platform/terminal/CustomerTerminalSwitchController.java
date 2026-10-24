package com.complex.platform.terminal;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.gate.TerminalIdentityHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ROOT-03-R2 客人两端读取本端已关闭的开关行（PGM 第 737 行「关闭后菜单不可见或不可点，接口也要拒绝业务动作」；第 744 行「开关拉取失败时按
 * “兜底关闭”处理」）。匿名可读（客人未登录也要按开关隐藏入口），只认客人手机应用与客人小程序；返回本端及全端（*）的全部关闭行
 * （含模块、版本、项目等维度），接口侧仍由终端总开关独立拒绝。
 */
@RestController
@RequestMapping("/api/v1/customer/terminal-switches")
public class CustomerTerminalSwitchController {

    private final TerminalDeviceService service;

    public CustomerTerminalSwitchController(TerminalDeviceService service) {
        this.service = service;
    }

    @GetMapping("/effective")
    public ApiResponse<PageResponse<TerminalCapabilitySwitchRecord>> effective(
            @RequestHeader(name = "X-Terminal-Type", required = false) String claimed) {
        String terminal = TerminalIdentityHolder.trustedTerminalType(claimed);
        if (!"app".equals(terminal) && !"miniapp".equals(terminal)) {
            throw ApiException.forbidden("CUSTOMER_TERMINAL_NOT_ALLOWED", "客人请使用官方 App 或小程序");
        }
        return ApiResponse.ok(service.listDisabledSwitchesFor(terminal));
    }
}
