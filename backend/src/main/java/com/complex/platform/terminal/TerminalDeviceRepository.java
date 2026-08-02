package com.complex.platform.terminal;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TerminalDeviceRepository {

    TerminalDeviceRecord save(TerminalDeviceRecord device);

    /**
     * P2 并发缺口①修复：设备状态机条件流转（乐观并发保护）。
     * 仅当当前行 {@code status = expectedFrom} 时，才把整行更新为 {@code updated}，
     * 实现为条件 UPDATE {@code WHERE id=:id AND status=:expectedFrom}。
     * 返回受影响行数：1=流转成功；0=前态不符（被并发改过/非法前态/行不存在）→调用方判冲突。
     * <p>与无条件 {@link #save}（device_id upsert，用于绑定/复用）互补：本方法用于"必须基于已读前态"
     * 的生命周期流转，按受影响行数杜绝并发丢更新/非法状态跃迁，并保持终态不可逆语义。
     */
    int compareAndSetStatus(String id, String expectedFrom, TerminalDeviceRecord updated);

    Optional<TerminalDeviceRecord> findByDeviceCode(String deviceCode);

    Optional<TerminalDeviceRecord> findById(String id);

    Optional<TerminalDeviceRecord> findByDeviceId(String deviceId);

    /**
     * 按场地 scope 列出终端设备（terminal_devices.venue_id = token scope）。
     * 商户面数据：商户只能看到其场地下绑定的终端设备。默认空，JdbcTerminalDeviceRepository 用 SQL 谓词下推。
     * 隔离闸口（AC-ISO-1）：提供 venue_id 行级 scoped 读通道。
     */
    default List<TerminalDeviceRecord> listDevicesByVenue(String venueId) {
        return List.of();
    }

    /**
     * 平台后台设备台账：全量列出终端设备（含商户归属列，K16 改绑工作台数据源）。
     * ponytail: LIMIT 200 平铺无分页，设备量到数百台再补分页。
     */
    default List<TerminalDeviceRecord> listDevices() {
        return List.of();
    }

    Optional<DeviceCredentialRecord> findDeviceCredentialByDeviceId(String deviceId);

    DeviceCredentialRecord saveDeviceCredential(DeviceCredentialRecord record);

    TerminalDeviceStatusEventRecord saveStatusEvent(TerminalDeviceStatusEventRecord event);

    List<TerminalCapabilitySwitchRecord> listCapabilitySwitches();

    Optional<TerminalCapabilitySwitchRecord> findCapabilitySwitchById(String id);

    Optional<TerminalCapabilitySwitchRecord> findCapabilitySwitch(String terminalType, String capabilityCode, String scopeId);

    /**
     * R5 T2 + R-10：按 scope 层级解析生效开关行，语义为「取最严」。
     * {@code orderedScopes} 为 (scope_type -&gt; scope_id) 的有序映射，最具体在前
     * （device &gt; version &gt; module &gt; business_system &gt; merchant &gt; project &gt; global）。
     *
     * <p>求值口径（2026-07-22 用户裁决，业务 HTML「开关优先级：全局 &gt; 综合体 &gt; 商户 &gt; 业务系统
     * &gt; 模块 &gt; 版本/设备 &gt; 灰度，高层级关闭压倒低层级打开」）：
     * <ul>
     *   <li><b>任一层显式 enabled=false → 最终关闭</b>；返回其中<b>最高层</b>的关闭行（审计口径：
     *       告诉运营是哪一层把它关了）。低层"打开"不再压过高层"关闭"。</li>
     *   <li>全部命中层均为 enabled=true → 返回<b>最具体</b>的那条开启行。</li>
     *   <li>无任何显式行 → 返回空（调用方按各自默认值处理；终端开关闸口按"默认开启"opt-out）。</li>
     * </ul>
     * terminal_type 同时匹配请求端与跨端通配 '*'；同一层内仍是「专端行优先于通配行」
     * （通配/专端是另一坐标轴，非层级，不纳入取最严）。
     *
     * <p>注意：本方法只覆盖 {@code terminal_capability_switches} 的七个层级。最细一档的
     * <b>灰度层</b>（白名单试点，V342）落在 {@link TerminalCanaryRepository}，由
     * {@code TerminalSwitchGuard} 在本方法之后串联求值，同样只能收窄不能放宽。
     */
    Optional<TerminalCapabilitySwitchRecord> resolveEffectiveSwitch(
            String terminalType, String capabilityCode, Map<String, String> orderedScopes);

    TerminalCapabilitySwitchRecord saveCapabilitySwitch(TerminalCapabilitySwitchRecord record);

    Optional<TerminalKioskConfigRecord> findKioskConfigByDeviceId(String deviceId);

    TerminalKioskConfigRecord saveKioskConfig(TerminalKioskConfigRecord record);

    KioskHeartbeatRecord saveKioskHeartbeat(KioskHeartbeatRecord record);
}
