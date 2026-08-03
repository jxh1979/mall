package com.complex.platform.gate;

import com.complex.platform.access.AccessActor;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        // AUD-010:优先用设备凭证验签派生的服务端可信终端类型(拦截器已存入 holder);无则回退自报头。
        Optional<TerminalIdentityHolder.VerifiedTerminal> verified = TerminalIdentityHolder.current();
        if (verified.isPresent() && verified.get().terminalType() != null
                && !verified.get().terminalType().isBlank()) {
            return verified.get().terminalType().trim();
        }
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
        // 2026-08-03:device scope 与 deviceId() 收敛到同一条读取路径(优先服务端派生身份),不再另读原始头。
        scopes.put("device", deviceId());
        scopes.put("version", header("X-Client-Version"));
        scopes.put("module", header("X-Module-Code"));
        scopes.put("business_system", header("X-Business-System"));
        // C1 修复（数据隔离规格§2：scope 注入只能来自安全 token，绝不取请求参数）：merchant 维度一律以登录令牌为准——
        // 令牌带 merchant（商户主体）→ 强制用令牌值、忽略可伪造的 X-Merchant-Id（防商户改/删头绕过本商户关停开关）；
        // 令牌无 merchant（平台/客人主体）→ 回退请求头（平台为可信运维上下文）。project 暂无令牌来源，维持请求头（小残留）。
        scopes.put("merchant", tokenMerchantOrHeader());
        scopes.put("project", header("X-Project-Id"));
        scopes.put("global", "global");
        return scopes;
    }

    /** merchant scope 解析：优先安全令牌的 merchant_id，令牌无则回退 X-Merchant-Id 头。 */
    private static String tokenMerchantOrHeader() {
        String tokenMerchant = tokenMerchantId();
        if (tokenMerchant != null && !tokenMerchant.isBlank()) {
            return tokenMerchant;
        }
        return header("X-Merchant-Id");
    }

    /** 当前登录主体（AccessActor）的 merchant_id；无认证/非员工主体返回 null。 */
    private static String tokenMerchantId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AccessActor actor) {
                return actor.merchantId();
            }
        } catch (RuntimeException ignored) {
            // best-effort：无安全上下文（如异步/测试）→ null，回退请求头。
        }
        return null;
    }

    /** 当前请求的 business_system（X-Business-System 头），无=null。 */
    static String businessSystem() {
        return blankToNull(header("X-Business-System"));
    }

    /** 当前请求的 module_code（X-Module-Code 头），无=null。 */
    static String moduleCode() {
        return blankToNull(header("X-Module-Code"));
    }

    /** 当前请求的 device id（X-Device-Id 头），无=null。 */
    static String deviceId() {
        Optional<TerminalIdentityHolder.VerifiedTerminal> verified = TerminalIdentityHolder.current();
        if (verified.isPresent() && verified.get().deviceId() != null
                && !verified.get().deviceId().isBlank()) {
            return verified.get().deviceId().trim();
        }
        return blankToNull(header("X-Device-Id"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
