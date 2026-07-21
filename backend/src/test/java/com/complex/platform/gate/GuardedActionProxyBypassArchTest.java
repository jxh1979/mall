package com.complex.platform.gate;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * AUD-020 架构护栏：任何声明了 {@link GuardedAction} 方法的类都不得在主源码里被 {@code new} 出来。
 *
 * <p>根因：{@link ActionGateAspect} 只能拦截 Spring 代理调用的 {@code @GuardedAction} 方法。业务服务
 * 若手工 {@code new} 一个带能力闸的协作者（如历史上的 {@code InventoryHighValueReviewService}），该对象
 * 非代理，切面永不命中，终端能力开关形同虚设。本测试扫描主源码，若发现 {@code new X(} 且 X 声明了
 * {@code @GuardedAction} 方法即失败——强制改为注入 Spring 管理的 bean，或把守卫上移到被代理的对外方法。
 *
 * <p>纯文本扫描、零依赖、非 DB。误报边界：仅按「行 trim 后以 @ 开头且含 GuardedAction」识别真注解
 * （排除注释 {@code //}、Javadoc {@code *}、import 行），按文件名推断类简名。
 */
class GuardedActionProxyBypassArchTest {

    @Test
    void noBusinessServiceHandInstantiatesAGuardedActionClass() {
        Path mainRoot = firstExisting("src/main/java", "backend/src/main/java");
        assertThat(mainRoot)
                .as("could not locate src/main/java from working dir " + Path.of("").toAbsolutePath())
                .isNotNull();

        List<Path> javaFiles = listJavaFiles(mainRoot);

        // 1) collect simple names of classes declaring a real @GuardedAction method annotation.
        Set<String> guardedClasses = new LinkedHashSet<>();
        for (Path file : javaFiles) {
            if (declaresGuardedAction(read(file))) {
                guardedClasses.add(simpleClassName(file));
            }
        }
        assertThat(guardedClasses)
                .as("expected at least one @GuardedAction-declaring class; scan/path likely broken")
                .isNotEmpty();

        // 2) fail if any main source constructs one of them with `new`.
        List<String> violations = new ArrayList<>();
        for (Path file : javaFiles) {
            String content = read(file);
            for (String guarded : guardedClasses) {
                Pattern p = Pattern.compile("\\bnew\\s+(?:[A-Za-z0-9_]+\\.)*" + Pattern.quote(guarded) + "\\s*\\(");
                if (p.matcher(content).find()) {
                    violations.add(simpleClassName(file) + " hand-instantiates guarded class " + guarded);
                }
            }
        }
        assertThat(violations)
                .as("guarded (@GuardedAction) classes must be Spring-injected, never `new`ed "
                        + "(non-proxy => ActionGateAspect bypassed). Inject the bean or move the guard "
                        + "onto a proxied wrapper.")
                .isEmpty();
    }

    private static Path firstExisting(String... candidates) {
        for (String c : candidates) {
            Path p = Path.of(c);
            if (Files.isDirectory(p)) {
                return p;
            }
        }
        return null;
    }

    private static List<Path> listJavaFiles(Path root) {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path p) {
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean declaresGuardedAction(String content) {
        for (String line : content.split("\n")) {
            String t = line.strip();
            if (t.startsWith("@") && t.contains("GuardedAction")) {
                return true;
            }
        }
        return false;
    }

    private static String simpleClassName(Path file) {
        String name = file.getFileName().toString();
        return name.endsWith(".java") ? name.substring(0, name.length() - ".java".length()) : name;
    }
}
