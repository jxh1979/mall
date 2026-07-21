package com.complex.platform.gate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * AUD-010 端到端(真实 Spring 上下文 + 配置驱动分级强制)。证明:①拦截器/验签器已装配;②按终端类型的强制开关
 * 经 @Value 生效——staff_work_phone=enforce 时设备绑定型写请求缺凭证被拦截器 fail-closed 403;③非设备绑定型
 * (web)放行不受影响;④未在此设为 enforce 的 tablet 走默认 grace,缺凭证放行(证明分级强制到类型粒度)。
 *
 * <p>验签成功→派生可信类型注入 的正路由真实 EC 签名在 {@link DeviceCredentialVerifierTests} 全量覆盖(mock 仓储,
 * 免 FK 播种脆弱性);本类只验真实 bean 装配 + 配置驱动强制的端到端接线。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "access.device_credential.enforcement.staff_work_phone=enforce"
})
class DeviceCredentialEnforcementJdbcIntegrationTests {

    @Autowired
    private DeviceCredentialVerifier verifier;

    @Autowired
    private DeviceCredentialInterceptor interceptor;

    @Test
    void beansAreWired() {
        assertThat(verifier).isNotNull();
        assertThat(interceptor).isNotNull();
    }

    @Test
    void enforceRejectsDeviceBoundWriteWithoutCredential() {
        MockHttpServletRequest r = post("staff_work_phone");
        assertThatThrownBy(() -> verifier.verify(r))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("DEVICE_CREDENTIAL_INVALID"));
        // 拦截器把 403 传出(preHandle 抛,DispatcherServlet 内经 GlobalExceptionHandler 统一 JSON)。
        assertThatThrownBy(() -> interceptor.preHandle(post("staff_work_phone"), new MockHttpServletResponse(), new Object()))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void nonDeviceBoundWebPassesThrough() {
        TerminalIdentityHolder.VerifiedTerminal vt = verifier.verify(post("web"));
        assertThat(vt.terminalType()).isEqualTo("web");
        assertThat(vt.verified()).isFalse();
    }

    @Test
    void tabletFallsBackToGraceDefaultAndIsAllowed() {
        // tablet 未在属性中设 enforce → 走默认 grace,缺凭证的写请求放行(不瘫痪未登记设备)。
        TerminalIdentityHolder.VerifiedTerminal vt = verifier.verify(post("tablet"));
        assertThat(vt.terminalType()).isEqualTo("tablet");
        assertThat(vt.verified()).isFalse();
    }

    private static MockHttpServletRequest post(String terminalType) {
        MockHttpServletRequest r = new MockHttpServletRequest("POST", "/api/v1/services/orders/o1/mark-paid");
        r.setRequestURI("/api/v1/services/orders/o1/mark-paid");
        r.addHeader("X-Terminal-Type", terminalType);
        return r;
    }
}
