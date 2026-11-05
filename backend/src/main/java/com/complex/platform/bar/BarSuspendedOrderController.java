package com.complex.platform.bar;

// 文件版本：V0.01

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.ConsumesSingleUseGrant;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.security.ExportFileService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 第十八批 T 段第 157 条 157-R7（BSM:1113「不设额度时，风险看板和审计导出必须持续展示挂起金额、账龄、责任人和未结清数量。」；
 * BSM:1840「看板按权限看，但把数据导出带走须单独授权并留痕」）：挂起订单风险看板与审计导出。
 */
@RestController
@RequestMapping("/api/v1/bar")
public class BarSuspendedOrderController implements ExportFileService.Source {

    private final BarSuspendedOrderBoard board;
    private final AuthorizationService authorizationService;

    public BarSuspendedOrderController(BarSuspendedOrderBoard board, AuthorizationService authorizationService) {
        this.board = board;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/suspended-orders")
    public ApiResponse<BarSuspendedOrderBoard.SuspensionBoard> listSuspendedOrders(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "bar_id", required = false) String barId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        authorizationService.requirePermission(BarSuspendedOrderBoard.VIEW);
        return ApiResponse.ok(board.board(status, barId, page, size));
    }

    /** 导出须单独授权：复用审计导出码（单次授权，导一次扣一次），只交服务端生成的带密码 PDF。 */
    @GetMapping("/suspended-orders/export")
    @ConsumesSingleUseGrant(BarSuspendedOrderBoard.EXPORT)
    public ResponseEntity<byte[]> exportSuspendedOrders(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "bar_id", required = false) String barId) {
        return board.export(status, barId).response();
    }

    @Override
    public String exportType() {
        return BarSuspendedOrderBoard.EXPORT_TYPE;
    }

    /** 第 23 条 23-R4 可编辑版重取：同查看看板（bar.order.view，范围按申请人当前账号），不再要导出权限。 */
    @Override
    public ExportFileService.Table viewTable(Map<String, Object> filters) {
        authorizationService.requirePermission(BarSuspendedOrderBoard.VIEW);
        return board.table(text(filters, "status"), text(filters, "bar_id"));
    }

    private static String text(Map<String, Object> filters, String key) {
        Object value = filters.get(key);
        return value == null ? null : value.toString();
    }
}
