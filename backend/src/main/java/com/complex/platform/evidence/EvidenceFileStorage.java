package com.complex.platform.evidence;

// 文件版本：V0.02

import com.complex.platform.common.api.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 证据附件本地存储（照片 + 视频）。
 *
 * <p><b>为什么现在才有</b>：{@code evidence_attachments} 建表以来只有「登记元数据」三个端点，
 * {@code upload_url} 是硬编码的 {@code https://object-storage.local/upload/{id}}，没有任何对象存储实现，
 * 后台也没有任何上传 UI —— 于是「高价值报损/盘点差异/存酒作废必须照片+视频双证据」这条业务规则
 * 在业务人员侧根本无法执行，附件 id 全是手打的假串。本类补上真正的字节通路。
 *
 * <p>范式照搬已跑通的 {@code BarPosImageStorage}（本地磁盘、DB 只存相对 key、受控 GET 回读，不上云）——
 * 仓里一行对象存储 SDK 都没有，为一处需求引入 MinIO/OSS 是负债。
 *
 * <p><b>类型是服务端定的</b>：由扩展名推 content_type，不信客户端自报的 MIME——
 * 否则「照片+视频双证据」可以拿同一个文件报两次类型糊弄过去。
 */
// ponytail: 本地磁盘 + 白名单扩展名 + 大小上限。EXIF 清洗/转码/CDN/防病毒留后续，需要时再说。
@Component
public class EvidenceFileStorage {

    /** 扩展名 → content_type。照片与视频分列，判定「是不是照片/是不是视频」全靠它。 */
    private static final Map<String, String> ALLOWED_TYPES = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("webp", "image/webp"),
            Map.entry("heic", "image/heic"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("webm", "video/webm"));

    /** ROOT-03 clients-R8（BSM 第 869 行「支持文字留言、照片、视频与语音」）：只供客人投诉入口——照片、视频之外加语音。 */
    private static final Map<String, String> COMPLAINT_TYPES = withAudio();

    /**
     * 18Y 复核修复（ENUM:202「渠道接入前的人工录入必须同时上传渠道提供的对账单原件作为凭证」，原件即渠道给的文件本身）：只供对账差异——照片、视频之外
     * 另收 pdf、xlsx、csv，这三种按扩展名与浏览器报的内容类型一并判（不收 xlsm 等带宏格式）。
     */
    private static final Map<String, String> STATEMENT_DOCUMENTS = Map.of("pdf", "application/pdf",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "csv", "text/csv");
    private static final Map<String, String> STATEMENT_TYPES = withStatementDocuments();

    /** ROOT-03-R28 学生认证证件：只收照片。 */
    private static final Map<String, String> PHOTO_TYPES = Map.of("jpg", "image/jpeg", "jpeg", "image/jpeg",
            "png", "image/png", "webp", "image/webp", "heic", "image/heic");

    private static Map<String, String> withAudio() {
        Map<String, String> types = new java.util.HashMap<>(ALLOWED_TYPES);
        types.putAll(Map.of("m4a", "audio/mp4", "aac", "audio/aac", "mp3", "audio/mpeg", "amr", "audio/amr",
                "wav", "audio/wav"));
        return Map.copyOf(types);
    }

    private static Map<String, String> withStatementDocuments() {
        Map<String, String> types = new java.util.HashMap<>(ALLOWED_TYPES);
        types.putAll(STATEMENT_DOCUMENTS);
        return Map.copyOf(types);
    }

    static final String KEY_PREFIX = "evidence-files/";

    private final Path baseDir;
    private final long maxBytes;

    public EvidenceFileStorage(
            @Value("${evidence.attachment.dir:./data/evidence-files}") String dir,
            // 数字一律可配（DEC-CFG-ALL-NUMERIC-001）：视频比图片大，默认 50MB。
            @Value("${evidence.attachment.max-bytes:52428800}") long maxBytes) {
        this.baseDir = Path.of(dir).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
    }

    /** 落盘结果：相对 object_key + 服务端判定的 content_type + 字节数。 */
    public record StoredFile(String objectKey, String contentType, long fileSize) {
    }

    public StoredFile store(MultipartFile file) {
        return store(file, ALLOWED_TYPES, "只支持照片 jpg/jpeg/png/webp/heic 与视频 mp4/mov/webm", null);
    }

    /** 客人投诉附件：照片、视频、语音（类型仍由扩展名服务端判定）。 */
    public StoredFile storeComplaintMedia(MultipartFile file) {
        return store(file, COMPLAINT_TYPES, "只支持照片 jpg/jpeg/png/webp/heic、视频 mp4/mov/webm 与语音 m4a/aac/mp3/amr/wav", null);
    }

    /** 18Y 复核修复：对账差异的渠道对账单原件——照片、视频与 pdf、xlsx、csv；后三种浏览器报的内容类型须与扩展名一致。 */
    public StoredFile storeReconciliationStatement(MultipartFile file) {
        String ext = extensionOf(file == null ? null : file.getOriginalFilename());
        String expected = STATEMENT_DOCUMENTS.get(ext);
        if (expected != null) {
            String declared = file.getContentType() == null ? "" : file.getContentType().split(";")[0].trim().toLowerCase(Locale.ROOT);
            // Windows 装了 Excel 时浏览器把 csv 报成 application/vnd.ms-excel。
            if (!declared.equals(expected) && !("csv".equals(ext) && "application/vnd.ms-excel".equals(declared))) {
                throw ApiException.badRequest("VALIDATION_FAILED", "对账单原件的扩展名与文件类型不符");
            }
        }
        return store(file, STATEMENT_TYPES, "只支持照片 jpg/jpeg/png/webp/heic、视频 mp4/mov/webm 与对账单 pdf/xlsx/csv", null);
    }

    /** 学生认证证件照片：seal 把原字节加密后才落盘（PGM 第 876 行身份信息一律加密存放），磁盘上不留明文。 */
    public StoredFile storePhoto(MultipartFile file, java.util.function.UnaryOperator<byte[]> seal) {
        return store(file, PHOTO_TYPES, "只支持照片 jpg/jpeg/png/webp/heic", seal);
    }

    /** ROOT-03-R21（BSM 第 863 行「证件等个人信息一律删除」）：删除本存储签发的文件；不存在即视为已删。 */
    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(resolveForRead(objectKey));
        } catch (ApiException missing) {
            // 文件已不在：删除目的已达成。
        } catch (IOException exception) {
            throw ApiException.badRequest("EVIDENCE_FILE_DELETE_FAILED", "证据文件删除失败");
        }
    }

    private StoredFile store(MultipartFile file, Map<String, String> allowedTypes, String allowedHint,
            java.util.function.UnaryOperator<byte[]> seal) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "证据文件不能为空");
        }
        if (file.getSize() > maxBytes) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "证据文件超过上限 " + (maxBytes / 1024 / 1024) + "MB");
        }
        String ext = extensionOf(file.getOriginalFilename());
        String contentType = allowedTypes.get(ext);
        if (contentType == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", allowedHint);
        }
        String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            Files.createDirectories(baseDir);
            Path target = baseDir.resolve(name).normalize();
            if (!target.startsWith(baseDir)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "非法文件名");
            }
            if (seal != null) {
                Files.write(target, seal.apply(file.getBytes()));
            } else {
                try (InputStream in = file.getInputStream()) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException exception) {
            throw ApiException.badRequest("EVIDENCE_FILE_STORE_FAILED", "证据文件保存失败");
        }
        return new StoredFile(KEY_PREFIX + name, contentType, file.getSize());
    }

    /** 回读（供受控 GET 端点流式返回）。只接受本存储签发的 key，防目录穿越。 */
    public Path resolveForRead(String objectKey) {
        if (objectKey == null || !objectKey.startsWith(KEY_PREFIX)) {
            throw ApiException.notFound("EVIDENCE_FILE_NOT_FOUND", "证据文件不存在");
        }
        String name = objectKey.substring(KEY_PREFIX.length());
        if (name.isBlank() || name.contains("/") || name.contains("\\") || name.contains("..")) {
            throw ApiException.badRequest("VALIDATION_FAILED", "非法文件名");
        }
        Path target = baseDir.resolve(name).normalize();
        if (!target.startsWith(baseDir) || !Files.isRegularFile(target)) {
            throw ApiException.notFound("EVIDENCE_FILE_NOT_FOUND", "证据文件不存在");
        }
        return target;
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf(".");
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
