package com.complex.platform.gate;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 从当前 HTTP 请求头解析终端类型与 scope 上下文（R5 T2）。
 * 无请求上下文时回退到全局默认（terminal_type 视为通配，仅 global 维度）。
 */
final class GateRequestContext {

    private GateRequestContext() {
    }

    static String terminalType() {
        String header = header("X-Terminal-Type");
        return header == null || header.isBlank() ? "*" : header.trim();
    }

    /**
     * 返回 scope 优先级有序映射（最具体在前）：
     * device &gt; version &gt; module &gt; business_system &gt; merchant &gt; project &gt; global。
     * global 维度始终以固定 scope_id 'global' 兜底。
     */
    static Map<String, String> orderedScopes() {
        Map<String, String> scopes = new LinkedHashMap<>();
        scopes.put("device", header("X-Device-Id"));
        scopes.put("version", header("X-Client-Version"));
        scopes.put("module", header("X-Module-Code"));
        scopes.put("business_system", header("X-Business-System"));
        scopes.put("merchant", header("X-Merchant-Id"));
        scopes.put("project", header("X-Project-Id"));
        scopes.put("global", "global");
        return scopes;
    }

    private static String header(String name) {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        return request.getHeader(name);
    }

    private static HttpServletRequest currentRequest() {
        try {
            var attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes servletAttributes) {
                return servletAttributes.getRequest();
            }
        } catch (RuntimeException ignored) {
            // best-effort: no request context (e.g. async/test) → null
        }
        return null;
    }
}
