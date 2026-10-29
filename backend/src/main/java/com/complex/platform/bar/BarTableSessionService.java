package com.complex.platform.bar;

// 文件版本：V0.05

import static com.complex.platform.bar.BarValidation.*;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class BarTableSessionService {

    private static final String CURRENCY_CNY = "CNY";

    @org.springframework.beans.factory.annotation.Autowired
    BarServiceResponsibilityService serviceResponsibilities;
    // 第十批 TK-24-R3：并台/转台后按新台组进度判定金达标退回（字段注入免改构造点；单测手工置位）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    BarDepositSettlementService depositSettlements;
    // 第十批 TK-27-R3：拆台时人工订单归属留痕（字段注入同上；单测手工置位）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    AuditEventService auditEvents;
    // 第十八批第 1 条（ENUM:187「以开台或下单时点所属的营业日为准」）：开台营业日按该酒吧营业日由服务端确定。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.common.BusinessDayResolver businessDays = new com.complex.platform.common.BusinessDayResolver();

    private final BarRepository repository;
    private final AuthorizationService authorizationService;

    public BarTableSessionService(BarRepository repository, AuthorizationService authorizationService) {
        this.repository = repository;
        this.authorizationService = authorizationService;
    }

    BarTableRecord createTable(BarTableRequest request) {
        BarTableRequest actual = requireRequest(request);
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        Instant now = Instant.now();
        String slug = slug(actual.idempotencyKey());
        int allowedFreeGuests = nonNegative(actual.allowedFreeEntryCount(), "allowed_free_entry_count");
        // 可进人数（容量）独立于免票入场人数：未填时沿用免票人数口径（向后兼容），填了就按填的落库。
        int capacity = actual.capacity() == null
                ? allowedFreeGuests
                : nonNegative(actual.capacity(), "capacity");
        long minimumSpend = nonNegative(actual.minimumSpendCent(), "minimum_spend_cent");
        return repository.saveTable(new BarTableRecord(
                "table_" + slug,
                requireText(actual.barId(), "bar_id"),
                merchantId,
                "T-" + slug.toUpperCase(Locale.ROOT),
                requireText(actual.tableName(), "table_name"),
                trimToDefault(actual.tableType(), "card_seat"),
                capacity,
                allowedFreeGuests,
                minimumSpend,
                CURRENCY_CNY,
                trimToDefault(actual.status(), "active"),
                now,
                now
        ));
    }

    BarTableRecord openTable(String tableId, OpenBarTableRequest request) {
        OpenBarTableRequest actual = requireRequest(request);
        BarTableRecord table = repository.findTableById(tableId)
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "台位不存在或已被删除"));
        checkMerchantOwnership(table.merchantId());
        if (!"active".equals(table.status())) {
            throw ApiException.conflict("BAR_TABLE_NOT_OPENABLE", "该台位未启用");
        }
        String reservationId = requireText(actual.reservationId(), "reservation_id");
        BarReservationRecord reservation = repository.findReservationById(reservationId)
                .orElseThrow(() -> ApiException.notFound("BAR_RESERVATION_NOT_FOUND", "订台记录不存在或已被删除"));
        if (!table.id().equals(reservation.tableId())) {
            throw ApiException.conflict("BAR_TABLE_RESERVATION_MISMATCH", "该订台不是本台位的预订");
        }
        requireNotHeldByOther(table.id(), reservation.id());
        // ROOT-03-R19（ENUM 第 100 行「已预留只能经取消订台、超时释放、到店开台三条路径退出」）：到店开台即退出占台。
        repository.releaseHold(reservation.id());
        return repository.saveTable(tableWithStatus(table, "opened", Instant.now()));
    }

    /**
     * 第十批 TK-27-R2：清台登记恢复空闲（BSM L1058「关台成功后台位自动进入待清台；由负责该台位的服务员完成清理后在系统登记
     * 恢复空闲，散台与卡座同一动作」）。只接受待清台台位；关台已保证待清台台位没有开台中的会话。
     */
    BarTableRecord clearTable(String tableId, BarTableClearRequest request) {
        requireRequest(request);
        BarTableRecord table = repository.findTableById(tableId)
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "台位不存在或已被删除"));
        checkMerchantOwnership(table.merchantId());
        if (!"pending_clear".equals(table.status())) {
            throw ApiException.conflict("BAR_TABLE_NOT_PENDING_CLEAR", "台位不在待清台状态，不能登记恢复空闲");
        }
        return repository.saveTable(tableWithStatus(table, "active", Instant.now()));
    }

    // 平台直营会话的租户占位：bar_table_sessions.merchant_id 为 NOT NULL（V019），
    // 而平台管理员/平台直营桌台的 merchant_id 为空，用与 analytics 同口径的 __platform__ 占位，
    // 既满足非空约束又不落进任何商户的行级隔离查询（WHERE merchant_id = :scope 永不命中占位）。
    private static final String PLATFORM_MERCHANT_PLACEHOLDER = "__platform__";

    BarTableSessionRecord createTableSession(BarTableSessionRequest request) {
        BarTableSessionRequest actual = requireRequest(request);
        BarTableRecord table = repository.findTableById(requireText(actual.tableId(), "table_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "台位不存在或已被删除"));
        // 会话商户归属：优先调用方商户范围，其次桌台商户，平台直营（皆空）落 __platform__ 占位满足非空约束。
        String merchantId = authorizationService.currentMerchantScope()
                .or(() -> Optional.ofNullable(trimToNull(table.merchantId())))
                .orElse(PLATFORM_MERCHANT_PLACEHOLDER);
        String barId = requireText(actual.barId(), "bar_id");
        if (!barId.equals(table.barId())) {
            throw ApiException.conflict("BAR_SCOPE_MISMATCH", "开台必须在台位所在的酒吧进行");
        }
        if (!"active".equals(table.status()) && !"opened".equals(table.status())) {
            throw ApiException.conflict("BAR_TABLE_NOT_OPENABLE", "该台位当前不可开台");
        }
        Instant now = Instant.now();
        // 第十八批 I 段 86-R3（BSM 第 1077 行「主责服务提供人按当日该场次排班指派到该卡座，同一卡座在不同场次可以更换」；DEC-R4-2026-08-04-25）：
        // 卡座与 VIP 包坐的主责服务人须为该卡座点位此刻已发布排班或真实顶班人员；不填时当班恰好一人即指派该人，多人或无人留空
        // （由开台人在当班人员里指定，或之后经服务交接、服务责任补录补上）。散台、吧台位照旧取请求值。
        String serviceStaffId = trimToNull(actual.serviceStaffId());
        if (serviceResponsibilities != null && java.util.Set.of("card_seat", "vip").contains(table.tableType())) {
            List<String> scheduled = serviceResponsibilities.scheduledServiceStaff(table.id(), now);
            if (serviceStaffId == null) {
                serviceStaffId = scheduled.size() == 1 ? scheduled.getFirst() : null;
            } else if (!scheduled.contains(serviceStaffId)) {
                throw ApiException.conflict("BAR_SERVICE_STAFF_NOT_SCHEDULED", "主责服务人须为该卡座点位当日该场次已发布排班或真实顶班人员");
            }
        }
        BarReservationRecord reservation = null;
        String reservationId = trimToNull(actual.reservationId());
        requireNotHeldByOther(table.id(), reservationId);
        if (reservationId != null) {
            reservation = repository.findReservationById(reservationId)
                    .orElseThrow(() -> ApiException.notFound("BAR_RESERVATION_NOT_FOUND", "订台记录不存在或已被删除"));
            if (!barId.equals(reservation.barId()) || !table.id().equals(reservation.tableId())) {
                throw ApiException.conflict("BAR_SCOPE_MISMATCH", "该订台不属于此台位会话");
            }
            // ROOT-03-R19：到店开台即退出占台（ENUM 第 100 行三条退出路径之一）。
            repository.releaseHold(reservation.id());
        }
        repository.saveTable(tableWithStatus(table, "opened", now));
        BarTableSessionRecord created = repository.saveTableSession(new BarTableSessionRecord(
                "session_" + slug(actual.idempotencyKey()),
                barId,
                merchantId,
                businessDays.businessDate(null, barId, now),
                table.id(),
                reservationId,
                trimToDefault(actual.customerId(), reservation == null ? null : reservation.customerId()),
                nonNegative(actual.guestCount(), "guest_count"),
                table.minConsumptionCent(),
                table.allowedPassFreeGuestCount(),
                // 第十八批 I 段 86-R2（BSM 第 1021 行「由有销售归属的订台开台的，自动记营销带客并带出该销售人员；……直接到店开台默认记自来客——此时现场可
                // 人工补选营销带客并选定销售人员」）：带订台一律取该订台的销售（员工代订＝经手员工，客人自助订台为空），请求销售只在直接到店时作现场补选。
                reservation != null ? reservation.salesStaffId() : trimToNull(actual.salesStaffId()),
                serviceStaffId,
                "open",
                now,
                null,
                null,
                1L,
                now,
                now
        ));
        if (serviceResponsibilities != null) serviceResponsibilities.recordOpened(created);
        return created;
    }

    BarTableSpaceAssignmentRecord assignTableSpace(String tableId, BarTableSpaceAssignmentRequest request) {
        BarTableSpaceAssignmentRequest actual = requireRequest(request);
        String id = requireText(tableId, "table_id");
        String spaceId = requireText(actual.spaceId(), "space_id");
        BarTableRecord table = repository.findTableById(id)
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "台位不存在或已被删除"));
        checkMerchantOwnership(table.merchantId());
        // 空间必须存在；且若属于某商户，必须与桌台同商户（不可指派到他商户空间，currentMerchantScope 隔离）。
        String spaceMerchant = repository.findSpaceMerchant(spaceId)
                .orElseThrow(() -> ApiException.notFound("SPACE_NOT_FOUND", "铺位不存在或已被删除"));
        if (!spaceMerchant.isBlank() && table.merchantId() != null && !spaceMerchant.equals(table.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "该铺位属于其他商户");
        }
        // 再以调用方商户范围兜底校验（防止桌台 merchant_id 为空时被越权指派他商户空间）。
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !spaceMerchant.isBlank() && !scope.get().equals(spaceMerchant)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "该铺位属于其他商户");
        }
        if (repository.assignTableSpace(id, spaceId) == 0) {
            throw ApiException.notFound("BAR_TABLE_NOT_FOUND", "台位不存在或已被删除");
        }
        return new BarTableSpaceAssignmentRecord(id, spaceId, Instant.now());
    }

    java.util.List<BarTableSpaceBacklogRecord> listTablesWithoutSpace() {
        return repository.listTablesWithoutSpace(authorizationService.currentMerchantScope());
    }

    BarTableSessionAdjustmentRecord transferTableSession(String sessionId, BarTableTransferRequest request) {
        BarTableTransferRequest actual = requireRequest(request);
        BarTableSessionRecord session = requireOpenTableSession(sessionId);
        checkMerchantOwnership(session.merchantId());
        requireSessionVersion(session, actual.sessionVersion());
        String requestedBy = authorizationService.currentActor().id();
        BarTableRecord sourceTable = repository.findTableById(session.tableId())
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "转出台位不存在或已被删除"));
        BarTableRecord targetTable = repository.findTableById(requireText(actual.targetTableId(), "target_table_id"))
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "转入台位不存在或已被删除"));
        if (!session.barId().equals(targetTable.barId())) {
            throw ApiException.conflict("BAR_SCOPE_MISMATCH", "转台不能跨酒吧");
        }
        if (!"active".equals(targetTable.status())) {
            throw ApiException.conflict("BAR_TABLE_NOT_TRANSFERABLE", "转入台位未启用");
        }
        requireNotHeldByOther(targetTable.id(), null);
        // 第十批 TK-24-R2（BSM L1079「合并台组整体转台到目标台后……原台及原并台关系不再参加之后的……数值计算」
        // 「原台已经发生的消费金额随台结转」；DEC-R8-95）：转台前记下当前并台链与合计阈值；转台后旧链子会话改
        // merged_transferred（只保留消费与留款、不再参与阈值、并台与拆台），目标台成为新的独立起点。
        long thresholdBefore = BarMinSpend.thresholdCent(repository, session.id());
        List<String> oldChain = repository.familySessionIds(session.id(), false).stream()
                .filter(id -> !id.equals(session.id())).toList();
        Instant now = Instant.now();
        String targetStaff = requireText(actual.targetServiceStaffId(), "target_service_staff_id");
        if (serviceResponsibilities != null) serviceResponsibilities.requireTransferStaff(targetTable.id(), targetStaff, now);
        // 第十批 TK-27-R2（BSM L1079「原台随转台成功立即释放，可供其他客人预订或开台」）：源台直接回空闲，不经待清台。
        // 复核 money B1（BSM L1079「单台转台与合并台组转台同样适用」）：合并台组整体转台时组内被并原台一并释放。
        repository.saveTable(tableWithStatus(sourceTable, "active", now));
        for (String childId : oldChain) {
            repository.findTableSessionById(childId).flatMap(child -> repository.findTableById(child.tableId()))
                    .ifPresent(childTable -> repository.saveTable(tableWithStatus(childTable, "active", now)));
        }
        repository.saveTable(tableWithStatus(targetTable, "opened", now));
        // 2c-5：DB 级乐观锁 CAS（替代仅内存比对的 requireSessionVersion），防并发转台双重占用。
        BarTableSessionRecord updated = sessionMovedToTable(session, targetTable, targetStaff, now);
        if (!repository.updateTableSessionIfVersionMatches(updated, session.sessionVersion())) {
            throw ApiException.conflict("BAR_TABLE_SESSION_VERSION_CONFLICT", "台位会话已被并发变更，请刷新后重试");
        }
        if (serviceResponsibilities != null) serviceResponsibilities.recordTransfer(session, updated, now, "transfer:" + actual.idempotencyKey());
        for (String childId : oldChain) {
            BarTableSessionRecord child = repository.findTableSessionById(childId).orElseThrow();
            if (!repository.updateTableSessionIfVersionMatches(sessionWithStatus(child, "merged_transferred", now), child.sessionVersion())) {
                throw ApiException.conflict("BAR_TABLE_SESSION_VERSION_CONFLICT", "台位会话已被并发变更，请刷新后重试");
            }
        }
        // 第十批 TK-24-R3（BSM L1062「转台按目标台的最低消费进度判断」）：按目标台阈值重判定金。
        if (depositSettlements != null) depositSettlements.evaluate(session.id());
        return repository.saveTableSessionAdjustment(newAdjustment(
                "transfer",
                updated,
                null,
                null,
                sourceTable.id(),
                targetTable.id(),
                thresholdBefore,
                targetTable.minConsumptionCent(),
                null,
                0L,
                0L,
                0L,
                requireText(actual.reason(), "reason"),
                requestedBy,
                targetTable.minConsumptionCent() > session.minConsumptionSnapshotCent(),
                actual.idempotencyKey(),
                safeList(actual.evidenceAttachmentIds()),
                transferSnapshot(session, updated, oldChain)
        ));
    }

    BarTableSessionAdjustmentRecord mergeTableSessions(String primarySessionId, BarTableMergeRequest request) {
        BarTableMergeRequest actual = requireRequest(request);
        BarTableSessionRecord primary = requireOpenTableSession(primarySessionId);
        checkMerchantOwnership(primary.merchantId());
        BarTableSessionRecord merged = requireOpenTableSession(requireText(actual.mergedSessionId(), "merged_session_id"));
        checkMerchantOwnership(merged.merchantId());
        requireSessionVersion(primary, actual.primarySessionVersion());
        requireSessionVersion(merged, actual.mergedSessionVersion());
        if (!primary.barId().equals(merged.barId())
                || !Objects.equals(primary.merchantId(), merged.merchantId())
                || !primary.businessDate().equals(merged.businessDate())) {
            throw ApiException.conflict("BAR_SCOPE_MISMATCH", "并台仅限同酒吧、同商户、同营业日的台位会话");
        }
        String requestedBy = authorizationService.currentActor().id();
        BarTableRecord primaryTable = repository.findTableById(primary.tableId())
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "主台位不存在或已被删除"));
        BarTableRecord mergedTable = repository.findTableById(merged.tableId())
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_NOT_FOUND", "被并台位不存在或已被删除"));
        // 第十批 TK-24-R2（BSM L1079「合并后的……最低消费额，分别按两侧所包含全部原台的对应数值相加计算」；DEC-SEAT-MERGE-001）：
        // 合并前先各取两侧台组阈值（并台后 family 已含被并侧，不能再分开取）。
        long thresholdBefore = BarMinSpend.thresholdCent(repository, primary.id());
        long thresholdAfter = thresholdBefore + BarMinSpend.thresholdCent(repository, merged.id());
        Instant now = Instant.now();
        // 2c-5：两个 session 均走 DB 级乐观锁 CAS，防并发合台重复占用。
        BarTableSessionRecord updatedPrimary = sessionWithNextVersion(primary, now);
        if (!repository.updateTableSessionIfVersionMatches(updatedPrimary, primary.sessionVersion())) {
            throw ApiException.conflict("BAR_TABLE_SESSION_VERSION_CONFLICT", "台位会话已被并发变更，请刷新后重试");
        }
        if (!repository.updateTableSessionIfVersionMatches(sessionMergedInto(merged, primary.id(), now), merged.sessionVersion())) {
            throw ApiException.conflict("BAR_TABLE_SESSION_VERSION_CONFLICT", "台位会话已被并发变更，请刷新后重试");
        }
        if (serviceResponsibilities != null) serviceResponsibilities.recordMerge(updatedPrimary, merged, now, "merge:" + actual.idempotencyKey());
        // 第十批 TK-27-R2（BSM L1058「并台期间全部原台位保持已开台并归属该合并台组」）：被并台位不再置待清台，关台时一并处理。
        // 第十批 TK-24-R3（BSM L1062「并台按合并台的合计最低消费进度判断」）：按合计阈值重判链内各笔定金。
        if (depositSettlements != null) depositSettlements.evaluate(primary.id());
        return repository.saveTableSessionAdjustment(newAdjustment(
                "merge",
                updatedPrimary,
                merged.id(),
                primary.id(),
                mergedTable.id(),
                primaryTable.id(),
                thresholdBefore,
                thresholdAfter,
                null,
                0L,
                0L,
                0L,
                requireText(actual.reason(), "reason"),
                requestedBy,
                false,
                actual.idempotencyKey(),
                safeList(actual.evidenceAttachmentIds()),
                sessionSnapshot(primary, updatedPrimary)
        ));
    }

    /**
     * 第十批 TK-27-R3：拆台（BSM L1079 拆台整段；BSM L1062「拆台时，各笔尚未处置定金……再按该结果台位或台组的最低消费进度判断」；
     * DEC-R6-19、DEC-R8-95 一次性成功或整体失败）。
     *
     * <ul>
     *   <li>mode=last_merge：按最近一次并台记录拆回该次并台前的两侧台组（被并侧连同其自身子树整体拆出）；
     *       single：任选一张原台单独拆出，其直接子节点改指其父＝压缩只剩一侧的空层级；all：一次把全部原台同时拆开。</li>
     *   <li>只允许拆 status='merged' 的节点；整体转台前的旧链（merged_transferred）拒绝（「后续拆台只处理该次转台之后新形成的并台关系」）。</li>
     *   <li>并台期间在台组台位上产生、但没有会话归属的订单，须由 order_assignments 人工明确归属并留痕，否则不得拆台。</li>
     *   <li>拆出会话恢复 open、脱离来源树、台位保持已开台；阈值/进度由台组动态求和自然重算（拆出台恢复其并入前冻结的低消快照）；
     *       各结果台组各判一次定金；低消不足不阻断。</li>
     * </ul>
     */
    BarTableSessionAdjustmentRecord splitTableSessions(String primarySessionId, BarTableSplitRequest request) {
        BarTableSplitRequest actual = requireRequest(request);
        BarTableSessionRecord primary = requireOpenTableSession(primarySessionId);
        checkMerchantOwnership(primary.merchantId());
        requireSessionVersion(primary, actual.primarySessionVersion());
        String mode = trimToDefault(actual.mode(), "last_merge");
        if (!List.of("last_merge", "single", "all").contains(mode)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "拆台方式须为 last_merge / single / all");
        }
        String requestedBy = authorizationService.currentActor().id();
        List<String> family = repository.familySessionIds(primary.id(), false);
        List<String> children = family.stream().filter(id -> !id.equals(primary.id())).toList();
        List<String> transferred = repository.familySessionIds(primary.id(), true).stream()
                .filter(id -> !family.contains(id)).toList();
        String splitSessionId = trimToNull(actual.splitSessionId());
        if ("single".equals(mode)) {
            requireText(splitSessionId, "split_session_id");
            if (transferred.contains(splitSessionId)) {
                throw historicalChain();
            }
            if (!children.contains(splitSessionId)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "拆出的原台不在当前合并台组内");
            }
        }
        if (children.isEmpty()) {
            if (!transferred.isEmpty()) {
                throw historicalChain();
            }
            throw ApiException.conflict("BAR_SPLIT_NOTHING_TO_SPLIT", "当前会话不是合并台组，无可拆出的原台");
        }
        List<String> roots = switch (mode) {
            case "single" -> List.of(splitSessionId);
            case "all" -> children;
            default -> List.of(family.stream()
                    .flatMap(id -> repository.listTableSessionAdjustments(id).stream())
                    .filter(adj -> "merge".equals(adj.adjustmentType()) && children.contains(adj.sourceSessionId()))
                    .reduce((a, b) -> b.createdAt().isBefore(a.createdAt()) ? a : b)
                    .map(BarTableSessionAdjustmentRecord::sourceSessionId)
                    .orElseThrow(() -> ApiException.conflict("BAR_SPLIT_MERGE_RECORD_MISSING", "找不到最近一次并台记录，不能按并台记录拆回")));
        };

        // 订单归属：台组台位上开台期间产生、但没有会话归属的订单，须先人工明确归属并留痕。
        List<BarTableSessionRecord> familySessions = family.stream()
                .map(repository::findTableSessionById).flatMap(Optional::stream).toList();
        Instant since = familySessions.stream().map(BarTableSessionRecord::openedAt).min(Comparator.naturalOrder())
                .orElse(primary.openedAt());
        Map<String, String> assignments = new LinkedHashMap<>();
        for (Map<String, Object> assignment : safeAssignments(actual.orderAssignments())) {
            String sessionId = requireItemText(assignment, "session_id", "sessionId");
            if (!family.contains(sessionId)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "订单归属的会话不在当前合并台组内：" + sessionId);
            }
            assignments.put(requireItemText(assignment, "order_id", "orderId"), sessionId);
        }
        List<BarOrderRecord> orphans = repository.listOrdersWithoutSession(
                familySessions.stream().map(BarTableSessionRecord::tableId).toList(), since);
        List<String> missing = orphans.stream().map(BarOrderRecord::id).filter(id -> !assignments.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw ApiException.conflict("BAR_SPLIT_ORDER_ATTRIBUTION_REQUIRED",
                    "以下订单无法确认归属，须先人工明确归属并留痕后才能拆台：" + String.join("、", missing));
        }
        String key = requireText(actual.idempotencyKey(), "idempotency_key");
        for (BarOrderRecord orphan : orphans) {
            String sessionId = assignments.get(orphan.id());
            repository.saveOrderServiceSource(orphan.id(), sessionId);
            if (auditEvents != null) {
                auditEvents.recordSuccess("bar.table_session.order_attributed", "bar.table_session.split", "bar_order",
                        orphan.id(), null, key, Map.of("session_id", sessionId, "split_session_id", primary.id()));
            }
        }

        long thresholdBefore = BarMinSpend.thresholdCent(repository, primary.id());
        Instant now = Instant.now();
        BarTableSessionRecord updatedPrimary = sessionWithNextVersion(primary, now);
        if (!repository.updateTableSessionIfVersionMatches(updatedPrimary, primary.sessionVersion())) {
            throw versionConflict();
        }
        List<BarTableSessionRecord> restored = new ArrayList<>();
        for (String rootId : roots) {
            BarTableSessionRecord root = repository.findTableSessionById(rootId).orElseThrow();
            if ("single".equals(mode)) {
                // 压缩空层级：拆出节点的直接子节点改指其父，留在剩余合并台组内。
                for (BarTableSessionRecord child : familySessions) {
                    if (rootId.equals(child.sourceSessionId()) && "merged".equals(child.status())) {
                        BarTableSessionRecord current = repository.findTableSessionById(child.id()).orElseThrow();
                        if (!repository.updateTableSessionIfVersionMatches(
                                sessionRelinked(current, root.sourceSessionId(), now), current.sessionVersion())) {
                            throw versionConflict();
                        }
                    }
                }
            }
            BarTableSessionRecord reopened = sessionRelinked(root, null, now);
            if (!repository.updateTableSessionIfVersionMatches(reopened, root.sessionVersion())) {
                throw versionConflict();
            }
            restored.add(reopened);
        }
        if (serviceResponsibilities != null) serviceResponsibilities.recordSplit(restored, now, "split:" + key);
        // 定金随结果台组重判（BSM L1062）；低消不足不阻断。
        if (depositSettlements != null) {
            depositSettlements.evaluate(primary.id());
            restored.forEach(session -> depositSettlements.evaluate(session.id()));
        }
        return repository.saveTableSessionAdjustment(newAdjustment(
                "split",
                updatedPrimary,
                roots.size() == 1 ? roots.getFirst() : null,
                primary.id(),
                primary.tableId(),
                restored.getFirst().tableId(),
                thresholdBefore,
                BarMinSpend.thresholdCent(repository, primary.id()),
                null,
                0L,
                0L,
                0L,
                requireText(actual.reason(), "reason"),
                requestedBy,
                false,
                key,
                safeList(actual.evidenceAttachmentIds()),
                splitSnapshot(primary, updatedPrimary, mode, roots, familySessions, assignments)
        ));
    }

    private static ApiException historicalChain() {
        return ApiException.conflict("BAR_SPLIT_HISTORICAL_CHAIN", "整体转台前的旧并台组已退出当前有效并台链，不得再拆出其原台");
    }

    private static ApiException versionConflict() {
        return ApiException.conflict("BAR_TABLE_SESSION_VERSION_CONFLICT", "台位会话已被并发变更，请刷新后重试");
    }

    private static List<Map<String, Object>> safeAssignments(List<Map<String, Object>> assignments) {
        return assignments == null ? List.of() : assignments;
    }

    /** 拆出：status 回 open、脱离来源树（source=null）、closed_at 清空；压缩层级：只改 source 指向。 */
    private BarTableSessionRecord sessionRelinked(BarTableSessionRecord session, String sourceSessionId, Instant now) {
        boolean reopen = sourceSessionId == null;
        return new BarTableSessionRecord(
                session.id(), session.barId(), session.merchantId(), session.businessDate(), session.tableId(),
                session.reservationId(), session.customerId(), session.guestCount(), session.minConsumptionSnapshotCent(),
                session.allowedPassFreeGuestCountSnapshot(), session.salesStaffId(), session.serviceStaffId(),
                reopen ? "open" : session.status(), session.openedAt(), reopen ? null : session.closedAt(),
                sourceSessionId, session.sessionVersion() + 1, session.createdAt(), now);
    }

    /** 拆台快照：拆前并台树、拆台方式、拆出会话与人工订单归属（只读留痕）。 */
    private String splitSnapshot(BarTableSessionRecord before, BarTableSessionRecord after, String mode, List<String> roots,
            List<BarTableSessionRecord> familySessions, Map<String, String> assignments) {
        String base = sessionSnapshot(before, after);
        String tree = familySessions.stream()
                .map(s -> "{\"id\":\"" + jsonText(s.id()) + "\",\"source_session_id\":"
                        + (s.sourceSessionId() == null ? "null" : "\"" + jsonText(s.sourceSessionId()) + "\"") + "}")
                .reduce((a, b) -> a + "," + b).orElse("");
        String rootList = roots.stream().map(id -> "\"" + jsonText(id) + "\"").reduce((a, b) -> a + "," + b).orElse("");
        String orders = assignments.entrySet().stream()
                .map(e -> "{\"order_id\":\"" + jsonText(e.getKey()) + "\",\"session_id\":\"" + jsonText(e.getValue()) + "\"}")
                .reduce((a, b) -> a + "," + b).orElse("");
        return base.substring(0, base.length() - 1) + ",\"mode\":\"" + jsonText(mode) + "\",\"split_session_ids\":[" + rootList
                + "],\"tree_before\":[" + tree + "],\"order_assignments\":[" + orders + "]}";
    }

    BarTableSessionAdjustmentRecord renewTableSession(String sessionId, BarTableRenewRequest request) {
        BarTableRenewRequest actual = requireRequest(request);
        BarTableSessionRecord session = requireOpenTableSession(sessionId);
        checkMerchantOwnership(session.merchantId());
        requireSessionVersion(session, actual.sessionVersion());
        String requestedBy = authorizationService.currentActor().id();
        Instant renewedUntil = requireInstant(actual.renewedUntil(), "renewed_until");
        Instant now = Instant.now();
        if (!renewedUntil.isAfter(now)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "续台截止时间必须晚于当前时间");
        }
        long additionalMinConsumption = nonNegative(actual.additionalMinConsumptionCent(), "additional_min_consumption_cent");
        long additionalDeposit = nonNegative(actual.additionalDepositAmountCent(), "additional_deposit_amount_cent");
        long additionalServiceFee = nonNegative(actual.additionalServiceFeeCent(), "additional_service_fee_cent");
        // 2c-5：DB 级乐观锁 CAS，防并发续台重复追加低消/押金。
        BarTableSessionRecord updated = sessionWithNextVersion(session, now);
        if (!repository.updateTableSessionIfVersionMatches(updated, session.sessionVersion())) {
            throw ApiException.conflict("BAR_TABLE_SESSION_VERSION_CONFLICT", "台位会话已被并发变更，请刷新后重试");
        }
        return repository.saveTableSessionAdjustment(newAdjustment(
                "renew",
                updated,
                null,
                null,
                session.tableId(),
                session.tableId(),
                session.minConsumptionSnapshotCent(),
                session.minConsumptionSnapshotCent() + additionalMinConsumption,
                renewedUntil,
                additionalMinConsumption,
                additionalDeposit,
                additionalServiceFee,
                requireText(actual.reason(), "reason"),
                requestedBy,
                additionalMinConsumption > 0 || additionalDeposit > 0 || additionalServiceFee > 0,
                actual.idempotencyKey(),
                safeList(actual.evidenceAttachmentIds()),
                sessionSnapshot(session, updated)
        ));
    }

    private BarTableSessionRecord requireOpenTableSession(String sessionId) {
        BarTableSessionRecord session = repository.findTableSessionById(sessionId)
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_SESSION_NOT_FOUND", "台位就餐会话不存在或已结束"));
        if (!"open".equals(session.status())) {
            throw ApiException.conflict("BAR_TABLE_SESSION_NOT_ACTIVE", "台位会话不在开台状态");
        }
        return session;
    }

    private void requireSessionVersion(BarTableSessionRecord session, Long expectedVersion) {
        if (expectedVersion == null || expectedVersion != session.sessionVersion()) {
            throw ApiException.conflict("BAR_TABLE_SESSION_VERSION_CONFLICT", "台位会话版本已变化，请刷新后重试");
        }
    }

    private void checkMerchantOwnership(String resourceMerchantId) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(resourceMerchantId)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "无权操作其他商户的数据");
        }
    }

    /**
     * ROOT-03-R19（ENUM 第 100 行「一个台位同一时刻只能存在一笔有效预留（排他锁，不是展示标记）……已预留只能经取消订台、超时释放、到店开台三条路径退出」）：
     * 台位正被有效预留占着时，只有带上占台的那笔订台到店开台才放行；散客开台、带别的订台号或转台转入都拒绝。
     */
    private void requireNotHeldByOther(String tableId, String reservationId) {
        repository.findHeldReservationId(tableId).filter(held -> !held.equals(reservationId)).ifPresent(held -> {
            throw ApiException.conflict("BAR_TABLE_ALREADY_RESERVED", "该台位已被预订占用；客人到店开台请填写该台位的订台号");
        });
    }

    private BarTableRecord tableWithStatus(BarTableRecord table, String status, Instant updatedAt) {
        return new BarTableRecord(
                table.id(),
                table.barId(),
                table.merchantId(),
                table.tableCode(),
                table.tableName(),
                table.tableType(),
                table.capacity(),
                table.allowedPassFreeGuestCount(),
                table.minConsumptionCent(),
                table.currency(),
                status,
                table.createdAt(),
                updatedAt
        );
    }

    private BarTableSessionRecord sessionMovedToTable(BarTableSessionRecord session, BarTableRecord table, String targetStaff, Instant now) {
        return new BarTableSessionRecord(
                session.id(),
                session.barId(),
                session.merchantId(),
                session.businessDate(),
                table.id(),
                session.reservationId(),
                session.customerId(),
                session.guestCount(),
                table.minConsumptionCent(),
                table.allowedPassFreeGuestCount(),
                session.salesStaffId(),
                targetStaff,
                session.status(),
                session.openedAt(),
                session.closedAt(),
                session.sourceSessionId(),
                session.sessionVersion() + 1,
                session.createdAt(),
                now
        );
    }

    private BarTableSessionRecord sessionWithNextVersion(BarTableSessionRecord session, Instant now) {
        return new BarTableSessionRecord(
                session.id(),
                session.barId(),
                session.merchantId(),
                session.businessDate(),
                session.tableId(),
                session.reservationId(),
                session.customerId(),
                session.guestCount(),
                session.minConsumptionSnapshotCent(),
                session.allowedPassFreeGuestCountSnapshot(),
                session.salesStaffId(),
                session.serviceStaffId(),
                session.status(),
                session.openedAt(),
                session.closedAt(),
                session.sourceSessionId(),
                session.sessionVersion() + 1,
                session.createdAt(),
                now
        );
    }

    private BarTableSessionRecord sessionMergedInto(BarTableSessionRecord session, String primarySessionId, Instant now) {
        return new BarTableSessionRecord(
                session.id(),
                session.barId(),
                session.merchantId(),
                session.businessDate(),
                session.tableId(),
                session.reservationId(),
                session.customerId(),
                session.guestCount(),
                session.minConsumptionSnapshotCent(),
                session.allowedPassFreeGuestCountSnapshot(),
                session.salesStaffId(),
                session.serviceStaffId(),
                "merged",
                session.openedAt(),
                now,
                primarySessionId,
                session.sessionVersion() + 1,
                session.createdAt(),
                now
        );
    }

    /** 第十批 TK-24-R2：整体转台后旧链子会话改 merged_transferred（版本 +1，CAS 写入）。 */
    private BarTableSessionRecord sessionWithStatus(BarTableSessionRecord session, String status, Instant now) {
        return new BarTableSessionRecord(
                session.id(), session.barId(), session.merchantId(), session.businessDate(), session.tableId(),
                session.reservationId(), session.customerId(), session.guestCount(), session.minConsumptionSnapshotCent(),
                session.allowedPassFreeGuestCountSnapshot(), session.salesStaffId(), session.serviceStaffId(), status,
                session.openedAt(), session.closedAt(), session.sourceSessionId(), session.sessionVersion() + 1,
                session.createdAt(), now);
    }

    /** 转台快照：在会话快照上追加转台前旧并台链（只读留痕，供「撤销转台」整笔恢复）。 */
    private String transferSnapshot(BarTableSessionRecord before, BarTableSessionRecord after, List<String> oldChain) {
        String base = sessionSnapshot(before, after);
        String chain = oldChain.stream().map(id -> "\"" + jsonText(id) + "\"").reduce((a, b) -> a + "," + b).orElse("");
        return base.substring(0, base.length() - 1) + ",\"transferred_chain\":[" + chain + "]}";
    }

    private BarTableSessionAdjustmentRecord newAdjustment(
            String type,
            BarTableSessionRecord session,
            String sourceSessionId,
            String targetSessionId,
            String fromTableId,
            String toTableId,
            long minConsumptionBeforeCent,
            long minConsumptionAfterCent,
            Instant renewedUntil,
            long additionalMinConsumptionCent,
            long additionalDepositAmountCent,
            long additionalServiceFeeCent,
            String reason,
            String requestedByUserId,
            boolean approvalRequired,
            String idempotencyKey,
            List<String> evidenceAttachmentIds,
            String snapshotJson
    ) {
        String key = requireText(idempotencyKey, "idempotency_key");
        return new BarTableSessionAdjustmentRecord(
                "adjustment_" + slug(type + ":" + key),
                session.id(),
                type,
                sourceSessionId,
                targetSessionId,
                fromTableId,
                toTableId,
                session.barId(),
                session.merchantId(),
                session.businessDate(),
                minConsumptionBeforeCent,
                minConsumptionAfterCent,
                renewedUntil,
                additionalMinConsumptionCent,
                additionalDepositAmountCent,
                additionalServiceFeeCent,
                reason,
                requestedByUserId,
                approvalRequired,
                "applied",
                key,
                evidenceAttachmentIds,
                snapshotJson,
                Instant.now()
        );
    }

    private String sessionSnapshot(BarTableSessionRecord before, BarTableSessionRecord after) {
        return """
                {"session_id":"%s","from_table_id":"%s","to_table_id":"%s","version_before":%d,"version_after":%d}
                """.formatted(
                jsonText(before.id()),
                jsonText(before.tableId()),
                jsonText(after.tableId()),
                before.sessionVersion(),
                after.sessionVersion()
        ).trim();
    }

}
