package com.complex.platform.terminal;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.terminal.dto.CanaryRolloutRequest;
import com.complex.platform.terminal.dto.CanaryTargetRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 终端开关灰度层后台维护接口（V342，R-10）。权限复用终端开关的
 * {@code terminal.switch.view} / {@code terminal.switch.manage}，写口同样只允许平台级主体
 * （与 {@code TerminalDeviceController#updateCapabilitySwitch} 同一 F-CROSS-01 收口：
 * 灰度能改变谁能用哪个端/能力，属平台级治理，不得下放给商户主体）。
 */
@RestController
@RequestMapping("/api/v1/terminals")
public class TerminalCanaryController {

    private final TerminalCanaryService service;
    private final AuthorizationService authorizationService;

    public TerminalCanaryController(TerminalCanaryService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/canary-rollouts")
    public ApiResponse<PageResponse<TerminalCanaryService.TerminalCanaryRolloutView>> listRollouts() {
        authorizationService.requirePermission("terminal.switch.view");
        return ApiResponse.ok(service.listRollouts());
    }

    /** 开/关某 端×能力 的灰度试点（按 端×能力 唯一，重复提交即覆盖）。 */
    @PutMapping("/canary-rollouts")
    public ApiResponse<TerminalCanaryRolloutRecord> upsertRollout(
            @RequestBody CanaryRolloutRequest request) {
        authorizationService.requirePermission("terminal.switch.manage");
        authorizationService.requirePlatformScope();
        return ApiResponse.ok(service.upsertRollout(request));
    }

    /** 加一个试点对象进白名单。 */
    @PostMapping("/canary-rollouts/{id}/targets")
    public ApiResponse<TerminalCanaryTargetRecord> addTarget(
            @PathVariable String id,
            @RequestBody CanaryTargetRequest request) {
        authorizationService.requirePermission("terminal.switch.manage");
        authorizationService.requirePlatformScope();
        return ApiResponse.ok(service.addTarget(id, request));
    }

    /** 移出白名单（软删除，需填存档理由）。 */
    @PostMapping("/canary-targets/{id}/archive")
    public ApiResponse<TerminalCanaryTargetRecord> archiveTarget(
            @PathVariable String id,
            @RequestBody CanaryTargetRequest request) {
        authorizationService.requirePermission("terminal.switch.manage");
        authorizationService.requirePlatformScope();
        return ApiResponse.ok(service.archiveTarget(id, request));
    }
}
