package com.complex.platform.support;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 测试助手：从 classpath 上的 db/migration 目录动态解析最新 Flyway 迁移版本（零填充 3 位，如 "057"）。
 *
 * <p>替代各 JDBC 集成测试中硬钉的 {@code isEqualTo("054")}：硬钉每加一个迁移就要手改一堆测试，
 * 属 R1/R3 反复指出的"静态快照"反模式。动态读取后，断言语义升级为"DB 已迁到当前可用的最新迁移（head）"，
 * 既不脆弱、又是更强的不变式。事实源是迁移目录本身。
 */
public final class MigrationVersions {

    private static final Pattern VERSION = Pattern.compile("^V(\\d+)__");

    private MigrationVersions() {
    }

    /** 返回 db/migration 下最大迁移版本号，零填充为 3 位字符串。 */
    public static String latest() {
        try {
            var url = MigrationVersions.class.getResource("/db/migration");
            if (url == null) {
                throw new IllegalStateException("db/migration not found on test classpath");
            }
            Path dir = Paths.get(url.toURI());
            try (Stream<Path> files = Files.list(dir)) {
                int max = files
                        .map(path -> path.getFileName().toString())
                        .map(VERSION::matcher)
                        .filter(Matcher::find)
                        .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
                        .max()
                        .orElseThrow(() -> new IllegalStateException("no V<n>__ migrations found"));
                return String.format("%03d", max);
            }
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RuntimeException("cannot resolve latest migration version", exception);
        }
    }
}
