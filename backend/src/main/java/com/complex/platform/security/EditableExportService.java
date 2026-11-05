// 文件版本：V0.02
package com.complex.platform.security;

import static com.complex.platform.common.Strings.requireText;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.FieldLabels;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewRepository;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第 23 条 23-R4 可编辑版。PGM:687「确需可编辑版本用于二次加工的，须另行申请、说明用途与接收方，经最高权限审批后发放，可编辑版同样带密码与暗纹
 * 并逐次留痕」。申请记录就是主管复核单（动作 security.export.editable_copy，对象 export_file＝原导出编号，最高权限审批，不另建申请表）；一次批准
 * 发放一次（{@link SupervisorReviewRepository#markSupervisorReviewReleased}，M007-A-01「一次批准只放行一次」），再要须另行申请。发放时按原导出的
 * 筛选条件、以申请人当前的查看范围重取数据（审批即授权，不再要导出权限），出一份新的带密码、带暗纹的 PDF（只放开复制与修改），新的导出记录指向
 * 原导出与复核单。不加 @GuardedAction（18A 起缺配置行即关）。
 */
@Service
public class EditableExportService {

    public static final String ACTION = "security.export.editable_copy";
    public static final String OBJECT_TYPE = "export_file";
    /** 导出内容（export_files.export_type 闭集）的中文名，同「我的导出」页面。 */
    private static final Map<String, String> TYPE_LABELS = Map.of(
            "analytics_monthly_report", "月度经营对比",
            "finance_ar_ap_ledger", "应收应付台账",
            "audit_operation_events", "操作留痕",
            "settlement_invoice_items", "开票清单",
            // 第十八批 T 段第 157 条 157-R7：挂起订单风险看板导出。
            "bar_suspended_orders", "挂起订单风险看板");

    /** 申请请求体：用途、接收方、幂等键。 */
    public record EditableCopyRequest(String purpose, String recipient, String idempotencyKey) {
    }

    private final ExportFileService exports;
    private final SupervisorReviewService reviews;
    private final SupervisorReviewRepository reviewRepository;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;
    private final AuthorizationService authorization;
    private final Map<String, ExportFileService.Source> sources;

    public EditableExportService(ExportFileService exports, SupervisorReviewService reviews, SupervisorReviewRepository reviewRepository,
                                 IdempotencyService idempotency, AuditEventService audit, AuthorizationService authorization,
                                 List<ExportFileService.Source> sources) {
        this.exports = exports;
        this.reviews = reviews;
        this.reviewRepository = reviewRepository;
        this.idempotency = idempotency;
        this.audit = audit;
        this.authorization = authorization;
        this.sources = sources.stream().collect(Collectors.toMap(ExportFileService.Source::exportType, Function.identity()));
    }

    /**
     * 申请可编辑版：先核原导出是本人的（否则 403 EXPORT_NOT_OWNED）、且不是可编辑版（409 EXPORT_ALREADY_EDITABLE），此闸在幂等之前；用途、接收方
     * 必填（400）。照证照宽限期写法进最高权限审批，复核单的说明写用途、接收方、导出内容与筛选（审批页原样显示）；审计 security.export.editable_requested。
     */
    @Transactional
    public SupervisorReviewDecisionRecord request(String exportFileId, EditableCopyRequest request) {
        ExportFileService.Owned source = exports.owned(exportFileId);
        if (source.editable()) {
            throw ApiException.conflict("EXPORT_ALREADY_EDITABLE", "这份已是可编辑版，不能再申请");
        }
        String purpose = requireText(request == null ? null : request.purpose(), "purpose");
        String recipient = requireText(request.recipient(), "recipient");
        String key = requireText(request.idempotencyKey(), "idempotency_key");
        return idempotency.execute("exportEditableRequest:" + source.id(), key, request, () -> {
            String reason = "用途：" + purpose + "\n接收方：" + recipient + "\n导出类型：" + TYPE_LABELS.getOrDefault(source.exportType(),
                    source.exportType()) + "\n筛选：" + filterText(source.filters());
            SupervisorReviewDecisionRecord decision = reviews.evaluate(new SupervisorReviewEvaluateRequest(key + ":review", reason, List.of(),
                    ACTION, OBJECT_TYPE, source.id(), 0L, reason, null, List.of()));
            if (!"pending_review".equals(decision.reviewStatus()) || !"top_authority".equals(decision.approvalLevel())) {
                throw ApiException.conflict("EDITABLE_EXPORT_APPROVAL_REQUIRED", "可编辑版必须进入最高权限审批");
            }
            audit.recordSuccess("security.export.editable_requested", ACTION, OBJECT_TYPE, source.id(), null, key,
                    Map.of("review_id", decision.reviewId(), "purpose", purpose, "recipient", recipient));
            return decision;
        }, SupervisorReviewDecisionRecord.class);
    }

    /**
     * 发放可编辑版：复核单须是本人为这份导出申请、已经最高权限批准的（否则 409 EDITABLE_EXPORT_NOT_APPROVED）；先把这次批准标为已发放，
     * 已发放过 409 EDITABLE_EXPORT_ALREADY_ISSUED（再要须另行申请）。按原导出筛选、以当前查看范围重取后出新文件；审计 security.export.editable_downloaded。
     */
    @Transactional
    public ExportFileService.ExportFile issue(String exportFileId, String reviewId) {
        ExportFileService.Owned source = exports.owned(exportFileId);
        String me = authorization.currentActor().id();
        SupervisorReviewDecisionRecord review = reviewRepository.findSupervisorReviewById(requireText(reviewId, "review_id"))
                .filter(found -> ACTION.equals(found.actionType()) && OBJECT_TYPE.equals(found.objectType())
                        && source.id().equals(found.objectId()) && me.equals(found.initiatedBy()) && "approved".equals(found.reviewStatus()))
                .orElseThrow(() -> ApiException.conflict("EDITABLE_EXPORT_NOT_APPROVED", "这份导出的可编辑版申请还没有经最高权限批准"));
        if (!reviewRepository.markSupervisorReviewReleased(review.reviewId(), me, Instant.now())) {
            throw ApiException.conflict("EDITABLE_EXPORT_ALREADY_ISSUED", "这次批准的可编辑版已发放过，再要须另行申请");
        }
        ExportFileService.Table table = sources.get(source.exportType()).viewTable(source.filters());
        ExportFileService.ExportFile file = exports.create(source.exportType(), table.title(), table.filters(), table.columns(), table.rows(),
                true, source.id(), review.reviewId());
        audit.recordSuccess("security.export.editable_downloaded", ACTION, OBJECT_TYPE, file.id(), null, null,
                Map.of("source_export_id", source.id(), "review_id", review.reviewId(), "row_count", table.rows().size()));
        return file;
    }

    /** 筛选条件与取数范围的中文说法（同「我的导出」页面）：键按字段中文名，空值为「不限」。 */
    private static String filterText(Map<String, Object> filters) {
        return filters.entrySet().stream().map(entry -> {
            String name = FieldLabels.label(entry.getKey());
            Object value = entry.getValue();
            String shown = value == null || "".equals(value) ? "不限"
                    : value instanceof Collection<?> list ? list.stream().map(String::valueOf).collect(Collectors.joining("、"))
                    : String.valueOf(value);
            return (name == null ? entry.getKey() : name) + "：" + shown;
        }).collect(Collectors.joining("；"));
    }
}
