package com.complex.platform.evidence;

// 文件版本：V0.01

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 第十八批 AN 段第 227 条 227-R2（PGM:287「……字段名、状态、枚举取值……必须全部为中文……不得以“这是标识”为由保留英文。」）：
 * 操作留痕标签字典，类路径 audit/audit-labels.json，四节（business_types 业务类型、actor_types 操作人类型、event_names 事件名、
 * extra_permissions 权限表外的权限状态与伪码），每节「码→中文名」，启动时读一次。新增留痕业务类型须同步该字典（门禁 verify:audit-labels）。
 */
@Component
public class AuditLabels {

    private static final String PATH = "audit/audit-labels.json";

    private final Map<String, Map<String, String>> sections;

    public AuditLabels() {
        try (InputStream in = new ClassPathResource(PATH).getInputStream()) {
            sections = new ObjectMapper().readValue(in, new TypeReference<Map<String, Map<String, String>>>() { });
        } catch (IOException exception) {
            throw new IllegalStateException("audit labels unavailable: " + PATH, exception);
        }
    }

    /** 某一节按码排序的「码、中文名」列表。 */
    public List<AuditLabel> list(String section) {
        return sections.getOrDefault(section, Map.of()).entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new AuditLabel(entry.getKey(), entry.getValue()))
                .toList();
    }

    public record AuditLabel(String code, String name) {
    }
}
