package com.complex.platform.amusement;

/**
 * 待建档（无在役设备档案）的游乐项目清单项。
 *
 * <p>T3.3 存量零档建档：列出尚无任何 active 设备档案的项目，供运营在开启强制建档总开关
 * （{@code amusement.device-registry.enforce.enabled}，T3.2）前照单补齐设备档案。
 * 这些项目一旦开关开启即被拒核销（{@code AMUSEMENT_DEVICE_REGISTRY_MISSING}）。</p>
 *
 * <p>ponytail：实验环境可一刀切，故不做每项目 device_registry_required 列 + 宽限期状态机，
 * 「总开关 + 本待办清单」即过渡机制（镜像 {@link AmusementProjectSpaceBacklogRecord} 缺项 backlog 范式）。</p>
 */
public record AmusementProjectDeviceRegistryBacklogRecord(
        String id,
        String projectName,
        String openStatus,
        String merchantId
) {
}
