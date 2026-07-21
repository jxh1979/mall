package com.complex.platform.gate;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.terminal.TerminalDeviceRecord;
import com.complex.platform.terminal.TerminalDeviceRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * AUD-010 终端身份服务端派生：把客户端自报的 {@code X-Terminal-Type} 与已注册设备表（{@code terminal_devices}）
 * 交叉核验，让"公司设备"不再等同于一个可伪造的请求头字符串。
 *
 * <p>规则：当请求带 {@code X-Device-Id} 且能命中已注册设备时，设备表登记的 {@code terminalType} 才是权威
 * （服务端真相覆盖请求头）；设备当前非 {@code bound}（已退还/解绑/停用）或其登记类型与声明矛盾 → fail-closed 拒绝。
 * 当未带 device id 或设备未知时，返回声明的类型不变——<b>诚实残留</b>：在缺少「每请求设备凭证认证」的基建下，
 * 无 device id 的声明仍是自证的。见类尾「最小可行方案」注释。
 *
 * <p>现状边界：本仓已有设备注册表（terminal_devices，含 terminalType/staffId/status）、kiosk 设备令牌
 * （仅 kiosk 端点校验）与员工私人手机设备凭证（Keystore 公钥/指纹，绑定生命周期），但<b>没有</b>一个作用于
 * 全量员工/后台请求的设备凭证认证过滤器把请求绑定到某台设备并注入可信终端类型。故本类是「有 device id 时按注册
 * 设备派生」的最大安全增量，不弄挂未带 device id 的现有终端；补齐 device id 的强制校验属后续（见 PRD）。
 */
@Component
public class TerminalIdentityResolver {

    private final TerminalDeviceRepository deviceRepository;

    public TerminalIdentityResolver(TerminalDeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    /**
     * 返回经服务端交叉核验后的可信终端类型。
     *
     * @param claimedType 客户端自报的 X-Terminal-Type（可空）
     * @param deviceId    客户端自报的 X-Device-Id（可空）
     * @return device id 命中已注册设备时=设备登记类型（权威）；否则=claimedType（残留）
     * @throws ApiException 403 当 device id 命中的设备非 bound 或其登记类型与声明矛盾
     */
    public String resolveTrustedTerminalType(String claimedType, String deviceId) {
        String did = deviceId == null ? null : deviceId.trim();
        if (did == null || did.isEmpty()) {
            return claimedType;
        }
        Optional<TerminalDeviceRecord> found = deviceRepository.findByDeviceId(did);
        if (found.isEmpty()) {
            return claimedType; // 未知 device id → 残留：回退声明（不弄挂未登记设备）。
        }
        TerminalDeviceRecord device = found.get();
        if (!"bound".equals(device.status())) {
            throw ApiException.forbidden("TERMINAL_DEVICE_NOT_BOUND",
                    "presented device is not an active bound terminal");
        }
        String registered = device.terminalType() == null ? null : device.terminalType().trim();
        if (registered != null && !registered.isEmpty()
                && claimedType != null && !claimedType.isBlank()
                && !registered.equalsIgnoreCase(claimedType.trim())) {
            throw ApiException.forbidden("TERMINAL_DEVICE_MISMATCH",
                    "claimed terminal type does not match the registered device");
        }
        return registered != null && !registered.isEmpty() ? registered : claimedType;
    }
}
