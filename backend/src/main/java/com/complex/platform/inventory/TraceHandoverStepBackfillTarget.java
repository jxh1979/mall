package com.complex.platform.inventory;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.ManualBackfillService;
import com.complex.platform.evidence.ManualBackfillTarget;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 第十八批 F 段第 14 条 14-R2：交接链缺段人工补录（BSM 第 1201 行「酒水与可追责物品的交接，每一步都必须扫码——库管出库、后厨接收、
 * 服务员取走、送达客人或卡座，缺任何一步即视为该环节工作未完成……扫码缺失的可事后人工补录，但一律须附证据（照片、录音、录像等）并经主管
 * 复核后方生效，补录记录与原始扫码记录并列展示、互不覆盖。」）。只补这四个人工扫码段（机器人配送段不在四步内，不靠补录绕过），且只补仍待扫码、
 * 没有已通过补录的段；业务发生＝建立交接任务的时刻；被影响方＝责任人（责任认定，本人不得复核）；项目取商户主项目（交接表无项目列），
 * 没有商户的平台直营交接按出厂参数。复核通过后原段行不改，按「扫码完成或补录通过」回算整链（{@link InventoryService#applyStepBackfill}）。
 */
@Component
class TraceHandoverStepBackfillTarget implements ManualBackfillTarget {

    private static final Map<String, String> SEGMENT_NAMES = Map.of(
            "out_verify", "库管出库", "kitchen_receive", "后厨接收", "staff_pickup", "服务员取走", "deliver_customer", "送达客人或卡座");

    private final InventoryService inventory;
    private final InventoryRepository repository;

    TraceHandoverStepBackfillTarget(InventoryService inventory, InventoryRepository repository) {
        this.inventory = inventory;
        this.repository = repository;
    }

    @Override
    public String type() {
        return InventoryService.BACKFILL_TYPE;
    }

    @Override
    public String submitPermission() {
        return "inventory.handover.scan";
    }

    @Override
    public String viewPermission() {
        return "inventory.handover.scan";
    }

    @Override
    public String policyActionType() {
        return "inventory.trace.scan_backfill";
    }

    @Override
    public void requireScope(String targetId) {
        inventory.requireOwnedHandover(targetId);
    }

    @Override
    public Resolved resolve(String targetId, String part, ManualBackfillService.Request request) {
        String segment = inventory.requireStaffSegment(part);
        TraceHandoverRecord header = inventory.requireOwnedHandover(targetId);
        TraceHandoverStepRecord step = inventory.requireStep(targetId, segment);
        if (!"pending".equals(step.stepStatus()) || inventory.approvedBackfillSegments(targetId).contains(segment)) {
            throw ApiException.conflict("HANDOVER_STEP_NOT_MISSING", "该段已扫码或已有通过的补录，不是缺失的扫码，不能补录");
        }
        Instant occurred = request.occurredAt();
        if (occurred == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "occurred_at is required");
        }
        if (occurred.isBefore(header.createdAt()) || occurred.isAfter(Instant.now())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "原时点须在建立交接任务之后、当前时间之前");
        }
        String project = header.merchantId() == null ? null : repository.findBarProjectId(header.merchantId()).orElse(null);
        return new Resolved(project, header.merchantId(), header.createdAt(), occurred, null, null, request.responsibleStaffId(),
                Map.of("segment", segment, "occurred_at", occurred.toString()), step,
                "交接单 " + targetId + "（溯源码 " + header.traceCode() + "）的「" + SEGMENT_NAMES.get(segment) + "」段缺扫码，补录扫码");
    }

    @Override
    public void requireView(String targetId) {
        inventory.requireOwnedHandover(targetId);
    }

    @Override
    public void apply(ManualBackfillService.Backfill backfill) {
        inventory.applyStepBackfill(backfill);
    }
}
