package com.complex.platform.facility;

// 文件版本：V0.03

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerService;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.evidence.EvidenceFileStorage;
import com.complex.platform.gate.TerminalSwitchGuard;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * ROOT-03 clients-R8 / backend-R26 客人投诉（BSM 第 869 行「投诉与反馈：客人可在官方 App、小程序提交投诉，支持文字留言、照片、视频与语音」；
 * 第 1379 行「客人（消费客人通过客人端投诉，客人即提单方；客户关怀部记录问题解决方案及客人反馈后可以关闭）」；第 1385 行「满意度在回访/验收时
 * 由提单方评分」）。客人只能看、评本人的投诉；附件为真实文件（EvidenceFileStorage），后台按工单查看权限受控回读。
 */
@Service
public class CustomerComplaintService {

    /** 单次投诉附件上限：照片、视频、语音合计 9 个。 */
    static final int MAX_FILES = 9;
    private static final String BUSINESS_TYPE = "customer_complaint";
    static final Map<String, String> STATUS_LABELS = Map.of(
            "pending_assign", "待派单", "assigned", "已派单", "processing", "处理中", "pending_review", "待回访",
            "pending_acceptance", "待验收", "accepted", "已验收", "closed", "已关闭", "suspended", "已挂起");

    /** 客人可见的本人投诉：编号、状态（中文）、提交时间、处理结论、满意度。 */
    public record ComplaintView(String workOrderId, String code, String title, String description, String status,
            String statusLabel, Instant submittedAt, String resolution, Integer satisfaction, int mediaCount) {
    }

    /** 后台受控查看的客人投诉附件（不含落盘路径）。 */
    public record MediaView(String id, String contentType, long fileSize, Instant createdAt) {
    }

    /** 幂等指纹：同一请求编号重放须同一内容（文件按名称与大小）。 */
    record SubmitFingerprint(String projectId, String title, String description, List<String> files) {
    }

    record RateRequest(String idempotencyKey, Integer score) {
    }

    private final FacilityWorkOrderService workOrders;
    private final CustomerService customers;
    private final EvidenceFileStorage storage;
    private final NamedParameterJdbcTemplate jdbc;
    private final AuditedOperationExecutor operations;
    private final TerminalSwitchGuard terminalSwitch;

    public CustomerComplaintService(FacilityWorkOrderService workOrders, CustomerService customers, EvidenceFileStorage storage,
            NamedParameterJdbcTemplate jdbc, AuditedOperationExecutor operations, TerminalSwitchGuard terminalSwitch) {
        this.workOrders = workOrders;
        this.customers = customers;
        this.storage = storage;
        this.jdbc = jdbc;
        this.operations = operations;
        this.terminalSwitch = terminalSwitch;
    }

    /** clients-R8 提交投诉：文字＋最多 9 个照片/视频/语音；工单类型投诉、来源 customer、提单方＝本人。幂等键必填。 */
    @Transactional
    public ComplaintView submit(String customerToken, String idempotencyKey, String projectId, String title, String description,
            List<MultipartFile> files) {
        CustomerRecord customer = customers.requireCustomerRecord(customerToken);
        // PGM 第 744 行「前端隐藏只是辅助，后端接口必须独立校验」：按综合体的客人端终端开关以客人所选综合体（即工单所属综合体）在服务端判
        // （不取 X-Project-Id），先于幂等。
        terminalSwitch.checkTerminalEnabledAtProject(trimToNull(projectId));
        List<MultipartFile> actualFiles = files == null ? List.of() : files.stream().filter(f -> f != null && !f.isEmpty()).toList();
        SubmitFingerprint fingerprint = new SubmitFingerprint(projectId, title, description,
                actualFiles.stream().map(f -> f.getOriginalFilename() + ":" + f.getSize()).toList());
        return operations.executeWrite("submitCustomerComplaint:" + customer.id(), idempotencyKey, fingerprint, ComplaintView.class,
                AuditedOperation.of("customer.complaint_submitted", "customer.complaint_submit_failed", "not_applicable", BUSINESS_TYPE),
                () -> {
                    String project = requireText(projectId, "project_id");
                    if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM projects WHERE id = :id)",
                            Map.of("id", project), Boolean.class))) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "project_id is invalid");
                    }
                    String actualTitle = requireText(title, "title");
                    if (actualFiles.size() > MAX_FILES) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "投诉附件最多 " + MAX_FILES + " 个");
                    }
                    // 先落盘全部附件（类型、大小不合格即拒，不建工单），再建单与附件行。
                    List<EvidenceFileStorage.StoredFile> storedFiles = actualFiles.stream().map(storage::storeComplaintMedia).toList();
                    FacilityWorkOrderRecord order = workOrders.createCustomerComplaint(customer.id(), project, actualTitle,
                            trimToNull(description), idempotencyKey);
                    Instant now = Instant.now();
                    for (EvidenceFileStorage.StoredFile stored : storedFiles) {
                        jdbc.update("""
                                INSERT INTO customer_complaint_media (id, work_order_id, customer_id, object_key, content_type, file_size, created_at)
                                VALUES (:id, :work_order_id, :customer_id, :object_key, :content_type, :file_size, :created_at)
                                """, new MapSqlParameterSource("id", "ccm_" + UUID.randomUUID().toString().replace("-", ""))
                                .addValue("work_order_id", order.id()).addValue("customer_id", customer.id())
                                .addValue("object_key", stored.objectKey()).addValue("content_type", stored.contentType())
                                .addValue("file_size", stored.fileSize()).addValue("created_at", Timestamp.from(now)));
                    }
                    return view(order, actualFiles.size());
                }, value -> value instanceof ComplaintView v ? v.workOrderId() : null, Map.of());
    }

    /** clients-R8 我的投诉：只返回本人提交的投诉，带处理结论与满意度。 */
    public PageResponse<ComplaintView> listMine(String customerToken) {
        return operations.readPage(AuditedOperation.of("customer.complaints_viewed", "customer.complaints_view_failed",
                "not_applicable", BUSINESS_TYPE), () -> {
                    CustomerRecord customer = customers.requireCustomerRecord(customerToken);
                    return workOrders.listCustomerComplaints(customer.id()).stream()
                            .map(order -> view(order, mediaCount(order.id()))).toList();
                });
    }

    /** backend-R26 客人对本人投诉评满意度（1～5，已写处理结论后才可评，同单只评一次）。幂等键必填。 */
    @Transactional
    public ComplaintView rate(String customerToken, String workOrderId, String idempotencyKey, Integer score) {
        CustomerRecord customer = customers.requireCustomerRecord(customerToken);
        // PGM 第 744 行「前端隐藏只是辅助，后端接口必须独立校验」：按综合体的客人端终端开关以本人这张投诉工单所属综合体在服务端判
        // （不取 X-Project-Id），先于幂等；不是本人的工单这里不判，由评分时按本人校验拒绝。
        terminalSwitch.checkTerminalEnabledAtProject(jdbc.query(
                "SELECT project_id FROM facility_work_orders WHERE id = :id AND source = 'customer' AND reporter_id = :customer_id",
                new MapSqlParameterSource("id", trimToNull(workOrderId)).addValue("customer_id", customer.id()),
                (rs, n) -> rs.getString(1)).stream().findFirst().orElse(null));
        return operations.executeWrite("rateCustomerComplaint:" + customer.id() + ":" + workOrderId, idempotencyKey,
                new RateRequest(idempotencyKey, score), ComplaintView.class,
                AuditedOperation.of("customer.complaint_rated", "customer.complaint_rate_failed", "not_applicable", BUSINESS_TYPE),
                () -> {
                    FacilityWorkOrderRecord rated = workOrders.rateByCustomer(customer.id(), requireText(workOrderId, "work_order_id"), score);
                    return view(rated, mediaCount(rated.id()));
                }, value -> value instanceof ComplaintView v ? v.workOrderId() : null, Map.of());
    }

    /** 后台：客人投诉附件清单（工单查看权限与平台范围，与工单详情同口径）。 */
    public List<MediaView> listMedia(String workOrderId) {
        workOrders.findById(workOrderId);
        return jdbc.query("""
                SELECT id, content_type, file_size, created_at FROM customer_complaint_media
                 WHERE work_order_id = :work_order_id ORDER BY created_at, id
                """, Map.of("work_order_id", workOrderId), (rs, n) -> new MediaView(rs.getString("id"),
                rs.getString("content_type"), rs.getLong("file_size"), rs.getTimestamp("created_at").toInstant()));
    }

    /** 后台：受控回读单个附件（附件须属该工单）。返回落盘路径与类型。 */
    public Map.Entry<Path, String> readMedia(String workOrderId, String mediaId) {
        workOrders.findById(workOrderId);
        Map<String, Object> row = jdbc.query("""
                SELECT object_key, content_type FROM customer_complaint_media WHERE id = :id AND work_order_id = :work_order_id
                """, Map.of("id", mediaId, "work_order_id", workOrderId), (rs, n) -> Map.<String, Object>of(
                "object_key", rs.getString("object_key"), "content_type", rs.getString("content_type")))
                .stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("COMPLAINT_MEDIA_NOT_FOUND", "投诉附件不存在"));
        return Map.entry(storage.resolveForRead((String) row.get("object_key")), (String) row.get("content_type"));
    }

    private int mediaCount(String workOrderId) {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM customer_complaint_media WHERE work_order_id = :id",
                Map.of("id", workOrderId), Integer.class);
        return count == null ? 0 : count;
    }

    private static ComplaintView view(FacilityWorkOrderRecord order, int mediaCount) {
        return new ComplaintView(order.id(), order.code(), order.title(), order.description(), order.status(),
                STATUS_LABELS.getOrDefault(order.status(), order.status()), order.createdAt(), order.resolution(),
                order.satisfaction(), mediaCount);
    }
}
