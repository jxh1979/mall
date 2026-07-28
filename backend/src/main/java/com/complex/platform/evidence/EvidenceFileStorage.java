package com.complex.platform.evidence;

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
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "证据文件不能为空");
        }
        if (file.getSize() > maxBytes) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "证据文件超过上限 " + (maxBytes / 1024 / 1024) + "MB");
        }
        String ext = extensionOf(file.getOriginalFilename());
        String contentType = ALLOWED_TYPES.get(ext);
        if (contentType == null) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "只支持照片 jpg/jpeg/png/webp/heic 与视频 mp4/mov/webm");
        }
        String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            Files.createDirectories(baseDir);
            Path target = baseDir.resolve(name).normalize();
            if (!target.startsWith(baseDir)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "非法文件名");
            }
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
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
