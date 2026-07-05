package com.complex.platform.amusement;

import java.util.List;

/**
 * 出场核销入参（阶段2，2026-07-05 用户拍板）：自由进出型（并发容量）客人/闸机出场扫码 → 并发容量 -N 释放。
 *
 * <p>据 credentialPayload 定位其【未释放的成功入场记录】（direction=entry、released_at IS NULL），
 * 按入场时的 point/device 维度键把 amusement_capacity_snapshot.occupied_count -N（N=入场放行人数），
 * 标记入场 released_at 并落一条 direction=exit 记录。幂等：同一入场重复出场只释放一次（唯一约束 + 条件 UPDATE 兜底）。
 *
 * <p>场次/批次型（仅场次维、无并发快照）出场核销无并发容量可释放 → 幂等跳过（不误减 amusement_sessions.used_capacity）。
 *
 * @param idempotencyKey 幂等键（出场核销记录 id 稳定生成）
 * @param credentialPayload 出场凭证 payload（同入场：动态二维码 token 或权益码）
 * @param pointId 出场核销点位（可空；仅用于辅助定位，实际释放按入场记录留痕的 point/device）
 * @param deviceId 出场核销设备（可空）
 * @param staffId 操作员（落 exit 记录 staff_id；可空回退 "staff"）
 * @param evidenceAttachmentIds 证据附件（可空）
 */
public record AmusementExitRequest(
        String idempotencyKey,
        String credentialPayload,
        String pointId,
        String deviceId,
        String staffId,
        List<String> evidenceAttachmentIds
) {
}
