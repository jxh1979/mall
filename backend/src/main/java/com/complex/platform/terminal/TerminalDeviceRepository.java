package com.complex.platform.terminal;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TerminalDeviceRepository {

    TerminalDeviceRecord save(TerminalDeviceRecord device);

    Optional<TerminalDeviceRecord> findByDeviceCode(String deviceCode);

    Optional<TerminalDeviceRecord> findById(String id);

    Optional<TerminalDeviceRecord> findByDeviceId(String deviceId);

    Optional<DeviceCredentialRecord> findDeviceCredentialByDeviceId(String deviceId);

    DeviceCredentialRecord saveDeviceCredential(DeviceCredentialRecord record);

    TerminalDeviceStatusEventRecord saveStatusEvent(TerminalDeviceStatusEventRecord event);

    List<TerminalCapabilitySwitchRecord> listCapabilitySwitches();

    Optional<TerminalCapabilitySwitchRecord> findCapabilitySwitchById(String id);

    Optional<TerminalCapabilitySwitchRecord> findCapabilitySwitch(String terminalType, String capabilityCode, String scopeId);

    /**
     * R5 T2：按 scope 优先级解析最具体的显式开关行。
     * {@code orderedScopes} 为 (scope_type -&gt; scope_id) 的有序映射，最具体在前
     * （device &gt; version &gt; module &gt; business_system &gt; merchant &gt; project &gt; global）。
     * 命中第一条显式行即返回；无任何行返回空（调用方按"默认开启"处理）。
     * terminal_type 同时匹配请求端与跨端通配 '*'。
     */
    Optional<TerminalCapabilitySwitchRecord> resolveEffectiveSwitch(
            String terminalType, String capabilityCode, Map<String, String> orderedScopes);

    TerminalCapabilitySwitchRecord saveCapabilitySwitch(TerminalCapabilitySwitchRecord record);

    Optional<TerminalKioskConfigRecord> findKioskConfigByDeviceId(String deviceId);

    TerminalKioskConfigRecord saveKioskConfig(TerminalKioskConfigRecord record);

    KioskHeartbeatRecord saveKioskHeartbeat(KioskHeartbeatRecord record);
}
