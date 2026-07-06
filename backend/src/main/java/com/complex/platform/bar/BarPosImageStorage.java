package com.complex.platform.bar;

import com.complex.platform.common.api.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 商品/套餐图片本地存储（V256 图形化点单 POS 阶段1）。
 * 沿用 image_url 先例（floor_plan）：DB 只存相对路径字符串，二进制落在服务器本地目录（不上云）。
 * 存：POST /bar/catalog/images 收 MultipartFile → 落盘 {base}/{uuid}.{ext} → 返回相对路径 "bar-images/{uuid}.{ext}"。
 * 取：GET /bar/catalog/images/{name} 由控制器回读同目录文件（鉴权走既有 API 会话，不开公共静态目录）。
 * ponytail: 只支持常见图片类型 + 大小上限，够点单配图；EXIF 清洗/缩略图/CDN 留阶段2按需加。
 */
@Component
public class BarPosImageStorage {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5MB
    static final String URL_PREFIX = "bar-images/";

    private final Path baseDir;

    public BarPosImageStorage(@Value("${bar.pos.image.dir:./data/bar-images}") String dir) {
        this.baseDir = Path.of(dir).toAbsolutePath().normalize();
    }

    /** 存图，返回可写入 image_url 的相对路径。 */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "image file is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw ApiException.badRequest("VALIDATION_FAILED", "image exceeds 5MB limit");
        }
        String ext = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "only jpg/jpeg/png/webp images are allowed");
        }
        String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            Files.createDirectories(baseDir);
            Path target = baseDir.resolve(name).normalize();
            if (!target.startsWith(baseDir)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "invalid image name");
            }
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw ApiException.badRequest("IMAGE_STORE_FAILED", "failed to store image");
        }
        return URL_PREFIX + name;
    }

    /** 回读一张图（供受控 GET 端点流式返回）。name 只能是纯文件名，防目录穿越。 */
    public Path resolveForRead(String name) {
        if (name == null || name.isBlank() || name.contains("/") || name.contains("\\") || name.contains("..")) {
            throw ApiException.badRequest("VALIDATION_FAILED", "invalid image name");
        }
        Path target = baseDir.resolve(name).normalize();
        if (!target.startsWith(baseDir) || !Files.isRegularFile(target)) {
            throw ApiException.notFound("IMAGE_NOT_FOUND", "image not found");
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
