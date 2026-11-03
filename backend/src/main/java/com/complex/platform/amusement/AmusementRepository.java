package com.complex.platform.amusement;

// 文件版本：V0.02

import java.time.Instant;
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

    /**
     * 阶段1 分流：由核销点位/设备定位游乐项目。优先按设备（amusement_devices.device_code / location_point_id）
     * 命中其 project_id；否则按项目入口点位（amusement_projects.location_point_id）命中。均定位不到返回 empty
     * （调用方明确报错，不硬造关联）。
     */
    Optional<String> findProjectIdByPointOrDevice(String pointId, String deviceId);

    /**
     * 阶段1 分流：定位项目在 now 时刻正在进行中的 open 场次（start_time≤now≤end_time 且 status='open'）。
     * 多条命中取 start_time 最新一条；无则返回 empty（调用方报「当前无进行中场次」）。
     */
    Optional<AmusementSessionRecord> resolveOpenSessionForProject(String projectId, Instant now);

    /** 处理清单 #8：设置 amusement_projects.space_id（空间指派/回填），返回受影响行数。 */
    int assignProjectSpace(String projectId, String spaceId);

    /** 处理清单 #8：解析空间所属商户（spaces.merchant_id），用于跨商户校验；空间不存在返回 empty。 */
    Optional<String> findSpaceMerchant(String spaceId);

    /** 处理清单 #8：列出 space_id 为空、待运营回填的项目（按商户范围过滤）。 */
    List<AmusementProjectSpaceBacklogRecord> listProjectsWithoutSpace(Optional<String> merchantScope);

    /** T3.3：列出无任何在役（active）设备档案、待建档的项目（按商户范围过滤）。 */
    List<AmusementProjectDeviceRegistryBacklogRecord> listProjectsMissingDeviceRegistry(Optional<String> merchantScope);

    /**
     * 返回该项目【生效中】的可用权益类型绑定（amusement_project_entitlements）。
     * V341（R-12）软删：已存档 / 已审批删除（墓碑）的绑定一律不返回，核销绑定校验只认生效绑定。
     * 为空表示该项目无生效绑定配置——核销时回退按「entitlement_type 为游乐类」放行（V082 注释、向后兼容）。
     */
    List<String> listProjectEntitlementTypes(String projectId);

    /**
     * 写入/更新项目可用权益类型绑定（amusement_project_entitlements）。按 (project_id, entitlement_type) 唯一幂等。
     * V341：唯一键使已存档/墓碑行占位，故【复绑=恢复】——命中冲突时清空存档/删除标记，保留原 id/created_at 与审计链。
     */
    AmusementProjectEntitlementRecord saveProjectEntitlement(AmusementProjectEntitlementRecord entitlement);

    /**
     * 列出项目权益类型绑定明细（管理端读取，含 id/created_at 与存档/删除字段族）。
     * includeArchived=false（默认视图）只返回生效中绑定；true 时连同已存档与墓碑一并返回，供「查看已归档 / 恢复」用。
     */
    List<AmusementProjectEntitlementRecord> listProjectEntitlements(String projectId, boolean includeArchived);

    /** 按 (project_id, entitlement_type) 查单条绑定；含已存档/墓碑行（管理端存档/恢复/审批链需要读到）。 */
    Optional<AmusementProjectEntitlementRecord> findProjectEntitlement(String projectId, String entitlementType);

    /** V341 存档（软下线/停用）：archived_at/archived_by/archive_reason 落值。返回受影响行数。 */
    int archiveProjectEntitlement(String projectId, String entitlementType, String reason, String actorId);

    /** V341 恢复（取消存档）：清空存档标记；同时把删除轴回退 none（含墓碑恢复）。返回受影响行数。 */
    int restoreProjectEntitlement(String projectId, String entitlementType);

    /** V341 发起删除申请：delete_status→pending + 申请人/时间/理由。返回受影响行数。 */
    int requestDeleteProjectEntitlement(String projectId, String entitlementType, String reason, String actorId);

    /** V341 审批删除：approved→'approved'（墓碑，不物理删）；rejected→回 'none'。返回受影响行数。 */
    int decideDeleteProjectEntitlement(String projectId, String entitlementType, boolean approved);

    /** 写入/更新点位/设备并发容量配置（amusement_capacity_config）。按 id 幂等 upsert。 */
    AmusementCapacityConfigRecord saveCapacityConfig(AmusementCapacityConfigRecord config);

    /** 列出项目全部点位/设备并发容量配置明细（管理端读取）。 */
    List<AmusementCapacityConfigRecord> listCapacityConfigs(String projectId);

    /** 按 id 查询容量配置（用于归属隔离校验与停用）。 */
    Optional<AmusementCapacityConfigRecord> findCapacityConfigById(String id);

    /** 写入/更新游乐设备主数据（amusement_devices，V143）。按 id 幂等 upsert。 */
    AmusementDeviceRecord saveDevice(AmusementDeviceRecord device);

    /** 列出项目全部设备主数据明细（管理端读取）。 */
    List<AmusementDeviceRecord> listDevicesByProject(String projectId);

    /** 按 id 查询设备主数据（用于归属隔离校验）。 */
    Optional<AmusementDeviceRecord> findDeviceById(String id);

    /** 按 (project_id, device_code) 查询设备主数据（桥接旧 device_id 字符串，唯一）。 */
    Optional<AmusementDeviceRecord> findDeviceByCode(String projectId, String deviceCode);

    /** 回填设备最近一次 PASS 巡检时间（巡检设备级化链路冗余字段，加速核销校验与到期计算）。 */
    void updateDeviceLastPassInspection(String projectId, String deviceCode, java.time.Instant inspectedAt);

    /** 项目级缺省安全检查周期（amusement_projects.safety_check_interval_hours，缺省 24 小时）。 */
    int findProjectSafetyCheckIntervalHours(String projectId);

    /** 项目级巡检 PASS 生效时回填该项目全部设备的最近 PASS 巡检时间（项目级巡检覆盖全设备）。 */
    void updateProjectDevicesLastPass(String projectId, java.time.Instant inspectedAt);

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

    /**
     * REQ-53 权益会话（V129 entitlement_sessions）：写入新会话（发起 status=initiated）。按 id 幂等 upsert。
     * merchant_id 由 Service 强制注入（domain-ownership direct scope）。
     */
    EntitlementSessionRecord saveEntitlementSession(EntitlementSessionRecord session);

    /** REQ-53：按 id 查询权益会话（用于一体机展示/领取与归属隔离校验）。 */
    Optional<EntitlementSessionRecord> findEntitlementSessionById(String id);

    /** REQ-53：按 session_code 查询权益会话（一体机扫会话码展示）。 */
    Optional<EntitlementSessionRecord> findEntitlementSessionByCode(String sessionCode);

    /** REQ-53：列出本商户全部权益会话（merchant scope 强制隔离）。 */
    List<EntitlementSessionRecord> listEntitlementSessionsByMerchant(String merchantId);

    /** REQ-53：列出全部权益会话（无 merchant scope 的平台视角）。 */
    List<EntitlementSessionRecord> listEntitlementSessions();

    /**
     * REQ-53 防错领领取核销：条件 UPDATE，仅当 {@code status IN ('initiated','displayed') AND expires_at > now}
     * 时把会话置为 claimed 并回填 claimed_by/claimed_at，受影响行数 == 1 返回 true。
     * 过期 / 已领 / 重复领 / 已取消均受影响 0 行返回 false（防错领、防重复领）。
     */
    boolean claimEntitlementSession(String id, String claimedBy, Instant now);

    /**
     * REQ-53（可选）：一体机首次展示时把 initiated → displayed（条件 UPDATE，仅 status='initiated' 命中）。
     * 命中返回 true；非 initiated（已展示/已领/过期/取消）返回 false，保持幂等读语义。
     */
    boolean markEntitlementSessionDisplayed(String id, Instant now);

    AmusementProjectRuleRecord saveProjectRule(AmusementProjectRuleRecord rule);

    List<AmusementProjectRuleRecord> listActiveBlockingProjectRules(String projectId);

    AmusementSessionRecord saveSession(AmusementSessionRecord session);

    List<AmusementSessionRecord> listSessions();

    List<AmusementSessionRecord> listSessionsByMerchant(String merchantId);

    List<AmusementSessionRecord> listSessionsScoped(boolean unrestricted,
            java.util.Set<String> complexIds, String merchantId);

    Optional<AmusementSessionRecord> findSessionById(String id);

    /** Safety writes and automatic reopen serialize on the same project. */
    default void lockProjectForSafety(String projectId) {}

    default boolean isHardCapacityPaused(String projectId) { return false; }

    boolean incrementSessionUsedCapacityIfAvailable(String sessionId);

    /** 项目实时在玩人数唯一硬容量；场次、点位和团体预约不另设入场上限。 */
    CapacityClaimResult claimCapacityAcrossDimensions(CapacityClaimContext context);

    boolean lockSessionCapacityIfAvailable(String sessionId, int capacityToLock);

    void releaseSessionLockedCapacity(String sessionId, int capacityToRelease);

    AmusementGroupBookingRecord saveGroupBooking(AmusementGroupBookingRecord booking);

    List<AmusementGroupBookingRecord> listGroupBookings();

    List<AmusementGroupBookingRecord> listGroupBookingsByMerchant(String merchantId);

    com.complex.platform.common.api.PageResponse<AmusementGroupBookingRecord> listGroupBookingsPaged(
            String merchantId, int page, int size);

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

    /** 阶段2 出场核销：定位某权益实例【未释放的成功入场】记录（direction=entry、released_at IS NULL；含 project/point/device 留痕）。 */
    Optional<AmusementVerificationRecord> findActiveEntryByEntitlement(String entitlementId);

    /** 阶段2 出场核销幂等：按入场 id 定位已落的 exit 记录（并发/重放判定后回读）。 */
    Optional<AmusementVerificationRecord> findExitByEntry(String entryVerificationId);

    /** 阶段2 出场核销幂等：某权益最近一条 exit 记录（已出场后再次扫码 → 返回既有 exit，不重复减容量）。 */
    Optional<AmusementVerificationRecord> findLatestExitByEntitlement(String entitlementId);

    /**
     * 阶段2 出场核销原子释放：条件标记入场 released_at（并发/幂等赢家判定）→ 赢家把并发快照 -N（point_device+project 维）→
     * 写 exit 记录。返回 true=真实释放；false=入场已释放（幂等跳过，不重复减容量）。
     */
    boolean releaseEntryConcurrentCapacity(
            AmusementVerificationRecord entry, AmusementVerificationRecord exitRecord, java.time.Instant now);

    List<AmusementVerificationRecord> listVerifications();

    List<AmusementVerificationRecord> listVerificationsByMerchant(String merchantId);

    /** 阶段3 只读：按 项目/场次/方向/日期 筛选核销记录并反查关联通票号；merchantId=null 为平台视角不加租户谓词。 */
    List<AmusementVerificationListItem> listVerificationItems(AmusementVerificationQuery query, String merchantId);

    /** 阶段3 只读：并发在场容量快照；merchantId=null 为平台视角不加租户谓词。 */
    List<AmusementCapacitySnapshotRecord> listCapacitySnapshots(String merchantId);

    // ---- CG-03 在玩人数系统自动清零（BSM「出场自动扣减」「在玩人数不允许人工调整」）：以下写口只供系统调度调用，无人工入口。----

    /** 该项目在 before 之前最后变动、且仍有在玩人数的并发快照（项目维/点位设备维）：{行数, 人数合计}。default 0 兼容内存双子。 */
    default int[] countStaleConcurrentSnapshots(String projectId, Instant before) {
        return new int[] {0, 0};
    }

    /** 营业日结束自动清零：只清 before 之前最后变动的并发快照（项目维/点位设备维），新营业日已产生的计数不动。返回行数。 */
    default int zeroStaleConcurrentSnapshots(String projectId, Instant before, Instant now) {
        return 0;
    }

    /** 场次结束前该场次维与该项目并发维仍有在玩人数的快照：{行数, 人数合计}。 */
    default int[] countSessionConcurrentSnapshots(String projectId, String sessionId) {
        return new int[] {0, 0};
    }

    /** 场次结束自动清空：该场次维 + 该项目并发维（项目/点位设备）在玩人数清零。返回行数。 */
    default int zeroSessionConcurrentSnapshots(String projectId, String sessionId, Instant now) {
        return 0;
    }

    /** end_time 已过、仍为 scheduled/open 的场次（待系统按场次结束置 closed）。 */
    default List<AmusementSessionRecord> listEndedUnclosedSessions(Instant now) {
        return List.of();
    }

    /** 系统按场次结束置 closed（仅 scheduled/open 且 end_time ≤ now）。返回是否本次流转。 */
    default boolean closeSessionIfEnded(String sessionId, Instant now) {
        return false;
    }

    /** 清零留痕纯追加；同项目同营业日 / 同场次已记录则不重复（返回 false）。 */
    default boolean insertConcurrentResetLogIfAbsent(AmusementConcurrentResetLogRecord record) {
        return false;
    }

    AmusementDeviceStatusRecord saveDeviceStatus(AmusementDeviceStatusRecord status);

    List<AmusementDeviceStatusRecord> listDeviceStatuses();

    List<AmusementDeviceStatusRecord> listDeviceStatusesByMerchant(String merchantId);

    AmusementQueueEntryRecord saveQueueEntry(AmusementQueueEntryRecord entry);

    List<AmusementQueueEntryRecord> listQueueEntries();

    List<AmusementQueueEntryRecord> listQueueEntriesByMerchant(String merchantId);

    /** 第十八批 N 段第 120 条 120-R5：各游乐项目排队中（waiting）的人数（游乐设施状态看板）。default 空兼容内存双子。 */
    default java.util.Map<String, Integer> countWaitingQueueEntriesByProject() {
        return java.util.Map.of();
    }

    /** 第十八批 N 段第 120 条 120-R5：各游乐项目未结（已登记、处理中、待外部）的故障事件数（游乐设施状态看板）。default 空兼容内存双子。 */
    default java.util.Map<String, Integer> countOpenFaultIncidentsByProject() {
        return java.util.Map.of();
    }

    com.complex.platform.common.api.PageResponse<AmusementQueueEntryRecord> listQueueEntriesPaged(
            String merchantId, int page, int size);

    Optional<AmusementQueueEntryRecord> findQueueEntryById(String id);

    AmusementReservationRecord saveReservation(AmusementReservationRecord reservation);

    List<AmusementReservationRecord> listReservations();

    List<AmusementReservationRecord> listReservationsByMerchant(String merchantId);

    com.complex.platform.common.api.PageResponse<AmusementReservationRecord> listReservationsPaged(
            String merchantId, int page, int size);

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

    /**
     * CG-02（BSM「因达到硬安全容量而自动停运的项目，在玩人数回落到硬安全容量以下后由系统自动恢复开放」）：
     * 单条原子 UPDATE——仅当项目 paused 且 auto_paused_reason=hard_capacity 且项目维实时在玩人数
     * （amusement_capacity_snapshot dimension=project 的 occupied_count）< 项目硬安全容量（amusement_projects.capacity，>0）
     * 时 paused→open 并清空原因；返回本次是否真的翻转（并发多路出场只有一路为 true）。
     * 事故停运（safety_incident）、人工 maintenance/closed/paused 一律不满足条件、不动。default false 以不破坏测试替身。
     */
    default boolean reopenProjectIfHardCapacityRecovered(String projectId, Instant now) {
        return false;
    }

    /** 项目维实时在玩人数（amusement_capacity_snapshot dimension=project 的 occupied_count）；无快照行 = 空。 */
    default java.util.OptionalInt findProjectInPlayCount(String projectId) {
        return java.util.OptionalInt.empty();
    }

    /**
     * T3.1 巡检到期调度（amusement_inspection_tasks，V143）：按 (project_id, COALESCE(device_id,''), due_at)
     * 唯一索引幂等 upsert 一张 pending 待办（ON CONFLICT DO NOTHING——同一到期点只生成一次，重复调度不产生重复待办）。
     */
    void upsertInspectionTask(AmusementInspectionTaskRecord task);

    /**
     * T3.1：把已到期未完成的巡检待办（status='pending' AND due_at &lt; now）批量翻 overdue；返回受影响行数。
     */
    int markOverdueInspectionTasks(java.time.Instant now);

    /**
     * T3.4 巡检前端：列出巡检到期待办（amusement_inspection_tasks），按到期时间升序（最紧急在前）。
     * merchantScope 非空 → 仅本商户；空（平台管理员）→ 全部。
     */
    java.util.List<AmusementInspectionTaskRecord> listInspectionTasks(java.util.Optional<String> merchantScope);
}
