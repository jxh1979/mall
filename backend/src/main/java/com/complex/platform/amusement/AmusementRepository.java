package com.complex.platform.amusement;

import java.util.List;
import java.util.Optional;

public interface AmusementRepository {
    AmusementSafetyTemplateRecord saveSafetyTemplate(AmusementSafetyTemplateRecord template);

    List<AmusementSafetyTemplateRecord> listSafetyTemplates();

    List<AmusementSafetyTemplateRecord> listSafetyTemplatesByMerchant(String merchantId);

    Optional<AmusementSafetyTemplateRecord> findSafetyTemplateById(String id);

    AmusementProjectRecord saveProject(AmusementProjectRecord project);

    List<AmusementProjectRecord> listProjects();

    List<AmusementProjectRecord> listProjectsByMerchant(String merchantId);

    Optional<AmusementProjectRecord> findProjectById(String id);

    /** 处理清单 #8：设置 amusement_projects.space_id（空间指派/回填），返回受影响行数。 */
    int assignProjectSpace(String projectId, String spaceId);

    /** 处理清单 #8：解析空间所属商户（spaces.merchant_id），用于跨商户校验；空间不存在返回 empty。 */
    Optional<String> findSpaceMerchant(String spaceId);

    /** 处理清单 #8：列出 space_id 为空、待运营回填的项目（按商户范围过滤）。 */
    List<AmusementProjectSpaceBacklogRecord> listProjectsWithoutSpace(Optional<String> merchantScope);

    /**
     * 返回该项目已配置的可用权益类型绑定（amusement_project_entitlements）。
     * 为空表示该项目无绑定配置——核销时回退按「entitlement_type 为游乐类」放行（V082 注释、向后兼容）。
     */
    List<String> listProjectEntitlementTypes(String projectId);

    /** 写入/更新项目可用权益类型绑定（amusement_project_entitlements）。按 (project_id, entitlement_type) 唯一幂等。 */
    AmusementProjectEntitlementRecord saveProjectEntitlement(AmusementProjectEntitlementRecord entitlement);

    /** 列出项目全部权益类型绑定明细（管理端读取，含 id/created_at）。 */
    List<AmusementProjectEntitlementRecord> listProjectEntitlements(String projectId);

    /** 删除项目某权益类型绑定（停用）；返回是否删除成功。 */
    boolean deleteProjectEntitlement(String projectId, String entitlementType);

    /** 写入/更新点位/设备并发容量配置（amusement_capacity_config）。按 id 幂等 upsert。 */
    AmusementCapacityConfigRecord saveCapacityConfig(AmusementCapacityConfigRecord config);

    /** 列出项目全部点位/设备并发容量配置明细（管理端读取）。 */
    List<AmusementCapacityConfigRecord> listCapacityConfigs(String projectId);

    /** 按 id 查询容量配置（用于归属隔离校验与停用）。 */
    Optional<AmusementCapacityConfigRecord> findCapacityConfigById(String id);

    /**
     * P0 清单#6 查询项目「超容处置策略」（V104 amusement_over_capacity_policy）：
     * 运营容量（软上限）与处置模式（block_hard 硬阻断+软告警 / warn_only 仅告警留痕不阻断）。
     * 未配置则返回禁用占位（仅硬阻断、无软上限告警），退化为既有行为（向后兼容）。
     */
    AmusementOverCapacityPolicy findOverCapacityPolicy(String projectId);

    /**
     * P0 清单#6 写入/更新项目「超容处置策略」（V104 amusement_over_capacity_policy）。
     * 按 project_id 幂等 upsert（ON CONFLICT(project_id) DO UPDATE
     * set operating_capacity/disposal_mode/enabled/updated_at）；首配置补 created_at。
     * 复用既有 {@link #findOverCapacityPolicy(String)} 读路径供消费方（核销超容判定）。
     */
    AmusementOverCapacityPolicy saveOverCapacityPolicy(AmusementOverCapacityPolicy policy);

    AmusementProjectRuleRecord saveProjectRule(AmusementProjectRuleRecord rule);

    List<AmusementProjectRuleRecord> listActiveBlockingProjectRules(String projectId);

    AmusementSessionRecord saveSession(AmusementSessionRecord session);

    List<AmusementSessionRecord> listSessions();

    List<AmusementSessionRecord> listSessionsByMerchant(String merchantId);

    Optional<AmusementSessionRecord> findSessionById(String id);

    boolean incrementSessionUsedCapacityIfAvailable(String sessionId);

    /**
     * §6.4 三维度容量取最小可放行余量：对「项目安全上限 / 场次 / 点位·设备」三维度中
     * <b>已配置（受控）</b>的每个维度，在同一事务内原子 check+increment（used+1，仅当余量&gt;0）。
     *
     * <p>原子防超卖：逐维度执行 {@code UPDATE ... WHERE occupied < limit}（与 session 的
     * {@code used+locked < capacity} 同模式）；任一维度余量为 0（或并发耗尽）→ 立即回滚（递减）本次
     * 已成功 +1 的维度，整体判失败返回 {@code claimed=false}，调用方据此落 SESSION_CAPACITY_EXHAUSTED。
     * 全部受控维度均放行 → 各维度 used+1 已落库（场次维写 amusement_sessions.used_capacity，
     * 点位/设备维与项目安全上限维写 amusement_capacity_snapshot），返回 {@code claimed=true}。
     *
     * <p>向后兼容：未配置 capacity_config 的点位/设备维度跳过；project.capacity&lt;=0 时项目安全上限维跳过；
     * 三维均未配置时退化为仅场次维（既有单维行为不变）。
     */
    CapacityClaimResult claimCapacityAcrossDimensions(CapacityClaimContext context);

    boolean lockSessionCapacityIfAvailable(String sessionId, int capacityToLock);

    void releaseSessionLockedCapacity(String sessionId, int capacityToRelease);

    AmusementGroupBookingRecord saveGroupBooking(AmusementGroupBookingRecord booking);

    List<AmusementGroupBookingRecord> listGroupBookings();

    List<AmusementGroupBookingRecord> listGroupBookingsByMerchant(String merchantId);

    Optional<AmusementGroupBookingRecord> findGroupBookingById(String id);

    Optional<AmusementGroupBookingRecord> approveGroupBookingWithCapacityLock(
            String id,
            String approvalNote,
            List<String> evidenceAttachmentIds,
            java.time.Instant approvedAt
    );

    Optional<AmusementGroupBookingRecord> releaseGroupBookingAndCapacity(
            String id,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            java.time.Instant releasedAt
    );

    AmusementSafetyInspectionRecord saveSafetyInspection(AmusementSafetyInspectionRecord inspection);

    List<AmusementSafetyInspectionRecord> listSafetyInspections();

    List<AmusementSafetyInspectionRecord> listSafetyInspectionsByMerchant(String merchantId);

    Optional<AmusementSafetyInspectionRecord> findSafetyInspectionById(String id);

    Optional<AmusementSafetyInspectionRecord> latestSafetyInspectionForProject(String projectId);

    AmusementSafetyActionRecord saveSafetyAction(AmusementSafetyActionRecord action);

    AmusementVerificationRecord saveVerification(AmusementVerificationRecord verification);

    List<AmusementVerificationRecord> listVerifications();

    List<AmusementVerificationRecord> listVerificationsByMerchant(String merchantId);

    AmusementManualOverrideRecord saveManualOverride(AmusementManualOverrideRecord override);

    List<AmusementManualOverrideRecord> listManualOverrides();

    List<AmusementManualOverrideRecord> listManualOverridesByMerchant(String merchantId);

    AmusementDeviceStatusRecord saveDeviceStatus(AmusementDeviceStatusRecord status);

    List<AmusementDeviceStatusRecord> listDeviceStatuses();

    List<AmusementDeviceStatusRecord> listDeviceStatusesByMerchant(String merchantId);

    AmusementQueueEntryRecord saveQueueEntry(AmusementQueueEntryRecord entry);

    List<AmusementQueueEntryRecord> listQueueEntries();

    List<AmusementQueueEntryRecord> listQueueEntriesByMerchant(String merchantId);

    Optional<AmusementQueueEntryRecord> findQueueEntryById(String id);

    AmusementReservationRecord saveReservation(AmusementReservationRecord reservation);

    List<AmusementReservationRecord> listReservations();

    List<AmusementReservationRecord> listReservationsByMerchant(String merchantId);

    Optional<AmusementReservationRecord> findReservationById(String id);

    AmusementMaintenanceRecord saveMaintenanceRecord(AmusementMaintenanceRecord record);

    List<AmusementMaintenanceRecord> listMaintenanceRecords();

    List<AmusementMaintenanceRecord> listMaintenanceRecordsByMerchant(String merchantId);

    Optional<AmusementMaintenanceRecord> findMaintenanceRecordById(String id);

    /**
     * 检修单位存在性校验（amuse-3 / 设计 §6.6.1）：检修单位复用供应链供应商主数据
     * {@code inventory_suppliers}。返回该供应商主表记录的 {@code status}（active/paused/archived）；
     * 供应商不存在时返回 {@link Optional#empty()}，供 Service 层区分「不存在」与「已停用」。
     */
    Optional<String> findMaintenanceVendorStatus(String vendorId);

    /**
     * 联动项目 open_status（设计 §5.1.1：主管确认检修恢复后 maintenance → open）。
     * 仅当当前 open_status 等于 expectedCurrentStatus 时更新，避免并发误流转；返回是否更新成功。
     */
    boolean updateProjectOpenStatus(String projectId, String expectedCurrentStatus, String newStatus);
}
