package com.complex.platform.terminal;

import java.util.List;
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

    TerminalCapabilitySwitchRecord saveCapabilitySwitch(TerminalCapabilitySwitchRecord record);

    Optional<TerminalKioskConfigRecord> findKioskConfigByDeviceId(String deviceId);

    TerminalKioskConfigRecord saveKioskConfig(TerminalKioskConfigRecord record);

    KioskHeartbeatRecord saveKioskHeartbeat(KioskHeartbeatRecord record);
}
