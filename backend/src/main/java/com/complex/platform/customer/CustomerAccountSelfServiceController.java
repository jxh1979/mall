package com.complex.platform.customer;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiRequests;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.facility.CustomerComplaintService;
import com.complex.platform.ticketing.CustomerTicketUsageResponse;
import com.complex.platform.ticketing.TicketingService;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * ROOT-03 客人端账户类接口（B-account）：投诉提交、本人投诉与满意度（clients-R8、backend-R26），学生认证申请（R28），
 * 我的票使用记录与第三方渠道接入状态（R29）。客人接口只许本人对象；写口幂等键必填。
 */
@RestController
@RequestMapping("/api/v1/customer")
public class CustomerAccountSelfServiceController {

    private final CustomerService customers;
    private final CustomerComplaintService complaints;
    private final StudentCertificationService studentCertifications;

    public CustomerAccountSelfServiceController(CustomerService customers, CustomerComplaintService complaints,
            StudentCertificationService studentCertifications) {
        this.customers = customers;
        this.complaints = complaints;
        this.studentCertifications = studentCertifications;
    }

    /** clients-R8 提交投诉（multipart：project_id、title、description、files 0～9 个照片/视频/语音、idempotency_key）。 */
    @PostMapping(value = "/complaints", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CustomerComplaintService.ComplaintView> submitComplaint(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestParam(name = "idempotency_key", required = false) String bodyKey,
            @RequestParam(name = "project_id", required = false) String projectId,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "description", required = false) String description,
            @RequestPart(name = "files", required = false) List<MultipartFile> files) {
        return ApiResponse.ok(complaints.submit(requireToken(customerToken, authorization), requireKey(idempotencyKey, bodyKey),
                projectId, title, description, files));
    }

    /** clients-R8 我的投诉：只返回本人提交的投诉（状态中文、提交时间、处理结论、满意度）。 */
    @GetMapping("/complaints")
    public ApiResponse<PageResponse<CustomerComplaintService.ComplaintView>> listMyComplaints(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(complaints.listMine(requireToken(customerToken, authorization)));
    }

    /** backend-R26 对本人投诉评满意度 {idempotency_key, score}。 */
    @PostMapping("/complaints/{work_order_id}/satisfaction")
    public ApiResponse<CustomerComplaintService.ComplaintView> rateComplaint(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable("work_order_id") String workOrderId,
            @RequestBody(required = false) Map<String, Object> request) {
        Object score = request == null ? null : request.get("score");
        Object bodyKey = request == null ? null : request.get("idempotency_key");
        return ApiResponse.ok(complaints.rate(requireToken(customerToken, authorization), workOrderId,
                requireKey(idempotencyKey, bodyKey == null ? null : String.valueOf(bodyKey)), toScore(score)));
    }

    /**
     * R28 学生认证申请（multipart：consent_id、chsi_verification_code、files 学生证件照片 1～9 张、idempotency_key）。
     * 先经 POST /api/v1/customer/consents 单独同意证件采集（consent_type=student_certificate），申请带该同意编号。
     */
    @PostMapping(value = "/student-certifications", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<StudentCertificationService.MyCertification> applyStudentCertification(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestParam(name = "idempotency_key", required = false) String bodyKey,
            @RequestParam(name = "consent_id", required = false) String consentId,
            @RequestParam(name = "chsi_verification_code", required = false) String chsiVerificationCode,
            @RequestPart(name = "files", required = false) List<MultipartFile> files) {
        return ApiResponse.ok(studentCertifications.apply(requireToken(customerToken, authorization),
                requireKey(idempotencyKey, bodyKey), consentId, chsiVerificationCode, files));
    }

    /** R28 本人最近一次学生认证申请（没有则 data 为空）。 */
    @GetMapping("/student-certifications/current")
    public ApiResponse<StudentCertificationService.MyCertification> currentStudentCertification(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(studentCertifications.current(requireToken(customerToken, authorization)));
    }

    /** R29 我的票使用记录：只许本人持有的票，他人 403。 */
    @GetMapping("/tickets/{ticket_id}/usages")
    public ApiResponse<PageResponse<CustomerTicketUsageResponse>> listTicketUsages(
            @RequestHeader(name = "X-Customer-Token", required = false) String customerToken,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable("ticket_id") String ticketId) {
        return ApiResponse.ok(customers.listTicketUsages(requireToken(customerToken, authorization), ticketId));
    }

    /** R29 第三方渠道（美团、大众点评、抖音）接入状态，匿名可读，供客户端未接入时置灰。 */
    @GetMapping("/ticket-channels")
    public ApiResponse<List<TicketingService.TicketChannelStatus>> listTicketChannels() {
        return ApiResponse.ok(customers.listTicketChannels());
    }

    private static Integer toScore(Object score) {
        if (score == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(score).trim());
        } catch (NumberFormatException exception) {
            throw ApiException.badRequest("VALIDATION_FAILED", "满意度评分必须在 1 到 5 之间");
        }
    }

    private static String requireKey(String header, String body) {
        String key = trimToNull(header) != null ? trimToNull(header) : trimToNull(body);
        ApiRequests.requireIdempotencyKey(key);
        return key;
    }

    private static String requireToken(String customerToken, String authorization) {
        String header = trimToNull(customerToken);
        String bearer = trimToNull(authorization);
        String resolved = header != null ? header
                : bearer != null && bearer.startsWith("Bearer ") ? trimToNull(bearer.substring("Bearer ".length())) : null;
        if (resolved == null) {
            throw ApiException.unauthorized("CUSTOMER_UNAUTHENTICATED", "缺少客人登录凭证，请重新登录");
        }
        return resolved;
    }
}
