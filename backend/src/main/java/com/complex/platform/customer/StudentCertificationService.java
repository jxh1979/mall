package com.complex.platform.customer;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.crypto.PiiCipher;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.evidence.EvidenceFileStorage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * ROOT-03-R28 学生认证（BSM 第 903 行「学生认证必须在本人注册会员后单独申请，认证通过只增加学生资格，不自动创建会员身份」；第 777 行「学生票要
 * 票务运营岗人工审核证件 + 学信网核验通过才可用」；第 890 行「采集证件……前先单独取得同意，并留同意版本、时间和范围」）。
 * 操作员裁决（第十三批第 1 条）「到预计毕业日（推荐）：有效到客人提供的学信网《学籍在线验证报告》上的预计毕业日期，审核人通过时按报告填写」。
 * 学信网核验由票务运营岗凭客人提供的《学籍在线验证报告》在线验证码在学信网网页人工完成，系统只记审核人与时间，不写「学信网自动核验通过」。
 */
@Service
public class StudentCertificationService {

    static final String PERMISSION = "ticket.student_cert.review";
    /** 证件采集单独同意的类型（经既有 POST /api/v1/customer/consents 记录版本、时间与来源端）。 */
    static final String CONSENT_TYPE = "student_certificate";
    static final int MAX_FILES = 9;
    private static final String BUSINESS_TYPE = "customer_student_certification";
    private static final Map<String, String> STATUS_LABELS = Map.of("pending", "待审核", "approved", "已通过", "rejected", "已驳回");
    private static final ObjectMapper JSON = new ObjectMapper();

    /** 客人看本人申请：状态（中文）、资格有效期至、驳回原因。 */
    public record MyCertification(String id, String status, String statusLabel, LocalDate validUntil, String rejectReason,
            Instant submittedAt, Instant reviewedAt) {
    }

    /** 后台审核视图：证件照片按序号受控回读，学信网验证码供审核人在学信网网页核验。 */
    public record ReviewItem(String id, String customerId, String status, String statusLabel, String chsiVerificationCode,
            int attachmentCount, LocalDate validUntil, String rejectReason, String reviewedBy, Instant reviewedAt,
            Instant submittedAt) {
    }

    record ApplyFingerprint(String consentId, String chsiVerificationCode, List<String> files) {
    }

    public record ApproveRequest(String idempotencyKey, LocalDate validUntil) {
    }

    public record RejectRequest(String idempotencyKey, String reason) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final CustomerService customers;
    private final EvidenceFileStorage storage;
    private final AuditedOperationExecutor operations;
    private final AuthorizationService authorizationService;
    private final PiiCipher piiCipher;
    private final Clock clock = Clock.system(LocalZone.ID);

    public StudentCertificationService(NamedParameterJdbcTemplate jdbc, CustomerService customers, EvidenceFileStorage storage,
            AuditedOperationExecutor operations, AuthorizationService authorizationService, PiiCipher piiCipher) {
        this.jdbc = jdbc;
        this.customers = customers;
        this.storage = storage;
        this.operations = operations;
        this.authorizationService = authorizationService;
        this.piiCipher = piiCipher;
    }

    /** 客人申请：须本人有效会员、已单独同意证件采集（本人、已同意、类型 student_certificate），带学信网验证码与证件照片；同一客人只能有一笔待审。 */
    @Transactional
    public MyCertification apply(String customerToken, String idempotencyKey, String consentId, String chsiVerificationCode,
            List<MultipartFile> files) {
        CustomerRecord customer = customers.requireCustomerRecord(customerToken);
        List<MultipartFile> photos = files == null ? List.of() : files.stream().filter(f -> f != null && !f.isEmpty()).toList();
        ApplyFingerprint fingerprint = new ApplyFingerprint(consentId, chsiVerificationCode,
                photos.stream().map(f -> f.getOriginalFilename() + ":" + f.getSize()).toList());
        return operations.executeWrite("applyStudentCertification:" + customer.id(), idempotencyKey, fingerprint,
                MyCertification.class,
                AuditedOperation.of("customer.student_cert.applied", "customer.student_cert.apply_failed", "not_applicable", BUSINESS_TYPE),
                () -> {
                    requireConsent(customer.id(), trimToNull(consentId));
                    String code = requireText(chsiVerificationCode, "chsi_verification_code");
                    if (code.length() > 64) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "chsi_verification_code is invalid");
                    }
                    if (photos.isEmpty() || photos.size() > MAX_FILES) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "请上传 1～" + MAX_FILES + " 张学生证件照片");
                    }
                    if (Boolean.TRUE.equals(jdbc.queryForObject("""
                            SELECT EXISTS (SELECT 1 FROM customer_student_certifications WHERE customer_id = :id AND status = 'pending')
                            """, Map.of("id", customer.id()), Boolean.class))) {
                        throw ApiException.conflict("STUDENT_CERT_PENDING", "已有待审核的学生认证申请，请等待审核结果");
                    }
                    // PGM 第 876 行「客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放」：证件照片与人脸照片同一写法——base64 后经 PiiCipher
                    // 加密成信封串再落盘，学信网验证码同样存密文；审核回读时解密。
                    List<String> keys = photos.stream().map(f -> storage.storePhoto(f, this::seal))
                            .map(EvidenceFileStorage.StoredFile::objectKey).toList();
                    String id = "scert_" + UUID.randomUUID().toString().replace("-", "");
                    Instant now = clock.instant();
                    jdbc.update("""
                            INSERT INTO customer_student_certifications (id, customer_id, status, consent_id, attachment_keys,
                                chsi_verification_code, created_at, updated_at)
                            VALUES (:id, :customer_id, 'pending', :consent_id, CAST(:keys AS jsonb), :code, :now, :now)
                            """, new MapSqlParameterSource("id", id).addValue("customer_id", customer.id())
                            .addValue("consent_id", consentId.trim()).addValue("keys", toJson(keys)).addValue("code", piiCipher.encrypt(code))
                            .addValue("now", Timestamp.from(now)));
                    return mine(load(id));
                }, value -> value instanceof MyCertification c ? c.id() : null, Map.of());
    }

    /** 客人本人最近一次学生认证申请（没有则空）。 */
    public MyCertification current(String customerToken) {
        CustomerRecord customer = customers.requireCustomerRecord(customerToken);
        return jdbc.query("SELECT * FROM customer_student_certifications WHERE customer_id = :id ORDER BY created_at DESC, id LIMIT 1",
                Map.of("id", customer.id()), this::map).stream().findFirst().map(StudentCertificationService::mine).orElse(null);
    }

    /** 后台审核列表（票务运营岗，平台范围）。 */
    public PageResponse<ReviewItem> list(String status) {
        authorizationService.requirePermission(PERMISSION);
        authorizationService.requirePlatformScope();
        String filter = trimToNull(status);
        return operations.readPage(AuditedOperation.of("customer.student_certs_viewed", "customer.student_certs_view_failed",
                PERMISSION, BUSINESS_TYPE), () -> jdbc.query("""
                SELECT * FROM customer_student_certifications
                 WHERE (CAST(:status AS VARCHAR) IS NULL OR status = :status)
                 ORDER BY created_at, id LIMIT 500
                """, new MapSqlParameterSource("status", filter), this::map).stream().map(StudentCertificationService::review).toList());
    }

    /** 审核通过：资格有效期至＝审核人按客人《学籍在线验证报告》填写的预计毕业日期（不早于今天）。只增加学生资格，不动会员身份与等级。 */
    @Transactional
    public ReviewItem approve(String certificationId, ApproveRequest request) {
        authorizationService.requirePermission(PERMISSION);
        authorizationService.requirePlatformScope();
        return operations.executeWrite("approveStudentCertification:" + certificationId,
                request == null ? null : request.idempotencyKey(), request, ReviewItem.class,
                AuditedOperation.of("customer.student_cert.approved", "customer.student_cert.approve_failed", PERMISSION, BUSINESS_TYPE),
                () -> {
                    LocalDate validUntil = request.validUntil();
                    if (validUntil == null) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "valid_until is required");
                    }
                    if (validUntil.isBefore(LocalDate.now(clock))) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "预计毕业日期已过，不能通过学生认证");
                    }
                    return decide(certificationId, "approved", validUntil, null);
                }, value -> value instanceof ReviewItem r ? r.id() : null, Map.of());
    }

    /** 审核驳回：须填驳回原因。 */
    @Transactional
    public ReviewItem reject(String certificationId, RejectRequest request) {
        authorizationService.requirePermission(PERMISSION);
        authorizationService.requirePlatformScope();
        return operations.executeWrite("rejectStudentCertification:" + certificationId,
                request == null ? null : request.idempotencyKey(), request, ReviewItem.class,
                AuditedOperation.of("customer.student_cert.rejected", "customer.student_cert.reject_failed", PERMISSION, BUSINESS_TYPE),
                () -> decide(certificationId, "rejected", null, requireText(request.reason(), "reason")),
                value -> value instanceof ReviewItem r ? r.id() : null, Map.of());
    }

    /** 后台受控回读第 index 张证件照片（从 1 起），解密后返回原字节与类型。 */
    public Map.Entry<byte[], String> readFile(String certificationId, int index) {
        authorizationService.requirePermission(PERMISSION);
        authorizationService.requirePlatformScope();
        String key = index < 1 ? null : jdbc.query("""
                SELECT attachment_keys ->> (CAST(:index AS INTEGER) - 1) AS object_key FROM customer_student_certifications WHERE id = :id
                """, new MapSqlParameterSource("id", certificationId).addValue("index", index),
                (rs, n) -> rs.getString("object_key")).stream().findFirst().orElse(null);
        if (key == null) {
            throw ApiException.notFound("STUDENT_CERT_FILE_NOT_FOUND", "学生证件照片不存在或已删除");
        }
        Path path = storage.resolveForRead(key);
        String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        byte[] photo;
        try {
            photo = Base64.getDecoder().decode(piiCipher.decrypt(Files.readString(path, StandardCharsets.UTF_8)));
        } catch (java.io.IOException unreadable) {
            throw ApiException.notFound("STUDENT_CERT_FILE_NOT_FOUND", "学生证件照片不存在或已删除");
        }
        return Map.entry(photo, name.endsWith(".png") ? "image/png" : name.endsWith(".webp") ? "image/webp"
                : name.endsWith(".heic") ? "image/heic" : "image/jpeg");
    }

    private byte[] seal(byte[] photo) {
        return piiCipher.encrypt(Base64.getEncoder().encodeToString(photo)).getBytes(StandardCharsets.UTF_8);
    }

    /** ROOT-03-R27 资格判定：存在已通过且资格有效期至不早于 date 的认证。 */
    public boolean qualifies(String customerId, LocalDate date) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM customer_student_certifications
                                WHERE customer_id = :id AND status = 'approved' AND valid_until >= :date)
                """, new MapSqlParameterSource("id", customerId).addValue("date", Date.valueOf(date)), Boolean.class));
    }

    /**
     * ROOT-03-R21（BSM 第 863 行「注销后：客人的姓名、手机号、人脸、证件等个人信息一律删除」）：清空本人各认证的证件照片与学信网验证码，
     * 照片文件在注销事务提交后删除（事务回滚则不删）。返回清掉的照片张数。
     */
    public int purgeOnDeregistration(String customerId) {
        List<String> keys = jdbc.queryForList("""
                SELECT jsonb_array_elements_text(attachment_keys) FROM customer_student_certifications WHERE customer_id = :id
                """, Map.of("id", customerId), String.class);
        jdbc.update("""
                UPDATE customer_student_certifications SET attachment_keys = '[]'::jsonb, chsi_verification_code = NULL, updated_at = now()
                 WHERE customer_id = :id
                """, Map.of("id", customerId));
        Runnable delete = () -> keys.forEach(storage::delete);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    delete.run();
                }
            });
        } else {
            delete.run();
        }
        return keys.size();
    }

    private ReviewItem decide(String certificationId, String status, LocalDate validUntil, String reason) {
        int updated = jdbc.update("""
                UPDATE customer_student_certifications
                   SET status = :status, valid_until = :valid_until, reject_reason = :reason, reviewed_by = :by,
                       reviewed_at = now(), updated_at = now()
                 WHERE id = :id AND status = 'pending'
                """, new MapSqlParameterSource("id", requireText(certificationId, "certification_id")).addValue("status", status)
                .addValue("valid_until", validUntil == null ? null : Date.valueOf(validUntil))
                .addValue("reason", reason).addValue("by", authorizationService.currentActor().id()));
        if (updated == 0) {
            load(certificationId);
            throw ApiException.conflict("STUDENT_CERT_NOT_PENDING", "该学生认证申请已审核");
        }
        return review(load(certificationId));
    }

    private void requireConsent(String customerId, String consentId) {
        boolean ok = consentId != null && Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM customer_consents
                                WHERE id = :id AND customer_id = :customer_id AND consent_type = :type AND accepted = TRUE)
                """, new MapSqlParameterSource("id", consentId).addValue("customer_id", customerId).addValue("type", CONSENT_TYPE),
                Boolean.class));
        if (!ok) {
            throw ApiException.conflict("CUSTOMER_CONSENT_REQUIRED", "上传学生证件前须先单独同意证件信息采集");
        }
    }

    private Row load(String id) {
        return jdbc.query("SELECT * FROM customer_student_certifications WHERE id = :id", Map.of("id", id), this::map)
                .stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("STUDENT_CERT_NOT_FOUND", "学生认证申请不存在"));
    }

    private record Row(String id, String customerId, String status, String chsiVerificationCode, int attachmentCount,
            LocalDate validUntil, String rejectReason, String reviewedBy, Instant reviewedAt, Instant createdAt) {
    }

    private Row map(ResultSet rs, int n) throws SQLException {
        Date validUntil = rs.getDate("valid_until");
        Timestamp reviewedAt = rs.getTimestamp("reviewed_at");
        int count;
        try {
            count = JSON.readTree(rs.getString("attachment_keys")).size();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("attachment_keys is not json", exception);
        }
        return new Row(rs.getString("id"), rs.getString("customer_id"), rs.getString("status"),
                piiCipher.decrypt(rs.getString("chsi_verification_code")), count, validUntil == null ? null : validUntil.toLocalDate(),
                rs.getString("reject_reason"), rs.getString("reviewed_by"), reviewedAt == null ? null : reviewedAt.toInstant(),
                rs.getTimestamp("created_at").toInstant());
    }

    private static MyCertification mine(Row row) {
        return new MyCertification(row.id(), row.status(), STATUS_LABELS.get(row.status()), row.validUntil(), row.rejectReason(),
                row.createdAt(), row.reviewedAt());
    }

    private static ReviewItem review(Row row) {
        return new ReviewItem(row.id(), row.customerId(), row.status(), STATUS_LABELS.get(row.status()), row.chsiVerificationCode(),
                row.attachmentCount(), row.validUntil(), row.rejectReason(), row.reviewedBy(), row.reviewedAt(), row.createdAt());
    }

    private static String toJson(List<String> keys) {
        try {
            return JSON.writeValueAsString(keys);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
