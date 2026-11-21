package com.complex.platform.access;

// 文件版本：V0.01

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 第十八批 BX 段第 372 条 372-R3（第十八批裁决 77「各清单、明细、导出只看得到这段时间发生的记录」）：请求开始时给已划定时段的外部人员账号
 * 把外审数据时段放进请求属性（{@link AuditDataWindow#current()} 读它），一处取值、只取不拒。ObjectProvider：@WebMvcTest 等切片不装
 * {@link AuditDataWindowService} 时跳过（同 gate/GateWebMvcConfig）。
 */
@Configuration
public class AuditDataWindowWebMvcConfig implements WebMvcConfigurer, HandlerInterceptor {

    private final ObjectProvider<AuditDataWindowService> windows;

    public AuditDataWindowWebMvcConfig(ObjectProvider<AuditDataWindowService> windows) {
        this.windows = windows;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        windows.ifAvailable(service -> registry.addInterceptor(this).addPathPatterns("/api/**"));
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        windows.getObject().currentWindow().ifPresent(window -> request.setAttribute(AuditDataWindow.ATTRIBUTE, window));
        return true;
    }
}
