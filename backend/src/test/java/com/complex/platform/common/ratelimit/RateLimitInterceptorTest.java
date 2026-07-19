package com.complex.platform.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.complex.platform.common.api.ApiException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitInterceptorTest {

    private static final String LOGIN = "/api/v1/auth/login";

    /** 把单个实例包成 ObjectProvider（生产用 Spring 注入，测试直接构造）。 */
    private static <T> ObjectProvider<T> provider(T instance) {
        return new ObjectProvider<T>() {
            @Override
            public T getObject() {
                return instance;
            }

            @Override
            public T getObject(Object... args) {
                return instance;
            }

            @Override
            public T getIfAvailable() {
                return instance;
            }

            @Override
            public T getIfUnique() {
                return instance;
            }
        };
    }

    private RateLimitInterceptor interceptor(RateLimitProperties properties) {
        return new RateLimitInterceptor(provider(new RateLimiter()), provider(properties));
    }

    private RateLimitProperties propsWithAuthLimit(int limit) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setAuthIp(new RateLimitProperties.Rule(limit, Duration.ofMinutes(1)));
        return properties;
    }

    private MockHttpServletRequest post(String uri, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRequestURI(uri);
        request.addHeader("X-Real-IP", ip);
        return request;
    }

    @Test
    void allowsUnderLimitAndThrows429WhenExceeded() {
        RateLimitInterceptor interceptor = interceptor(propsWithAuthLimit(2));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(post(LOGIN, "1.1.1.1"), response, new Object())).isTrue();
        assertThat(interceptor.preHandle(post(LOGIN, "1.1.1.1"), response, new Object())).isTrue();

        ApiException tooMany = assertThrows(ApiException.class,
                () -> interceptor.preHandle(post(LOGIN, "1.1.1.1"), response, new Object()));
        assertThat(tooMany.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(tooMany.code()).isEqualTo("RATE_LIMITED");
        assertThat(tooMany.retryAfterSeconds()).isNotNull();
    }

    @Test
    void differentIpsAreIndependent() {
        RateLimitInterceptor interceptor = interceptor(propsWithAuthLimit(1));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(post(LOGIN, "1.1.1.1"), response, new Object())).isTrue();
        // 同 IP 第二次超限。
        assertThrows(ApiException.class,
                () -> interceptor.preHandle(post(LOGIN, "1.1.1.1"), response, new Object()));
        // 另一 IP 独立计数，仍放行。
        assertThat(interceptor.preHandle(post(LOGIN, "2.2.2.2"), response, new Object())).isTrue();
    }

    @Test
    void nonSensitivePathIsNeverLimited() {
        RateLimitInterceptor interceptor = interceptor(propsWithAuthLimit(1));
        MockHttpServletResponse response = new MockHttpServletResponse();
        for (int i = 0; i < 10; i++) {
            assertThat(interceptor.preHandle(post("/api/v1/customer/session", "1.1.1.1"), response, new Object()))
                    .isTrue();
        }
    }

    @Test
    void getRequestsAreNotLimited() {
        RateLimitInterceptor interceptor = interceptor(propsWithAuthLimit(1));
        MockHttpServletResponse response = new MockHttpServletResponse();
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest get = new MockHttpServletRequest("GET", LOGIN);
            get.setRequestURI(LOGIN);
            get.addHeader("X-Real-IP", "1.1.1.1");
            assertThat(interceptor.preHandle(get, response, new Object())).isTrue();
        }
    }

    @Test
    void disabledPropertiesBypassLimiting() {
        RateLimitProperties properties = propsWithAuthLimit(1);
        properties.setEnabled(false);
        RateLimitInterceptor interceptor = interceptor(properties);
        MockHttpServletResponse response = new MockHttpServletResponse();
        for (int i = 0; i < 10; i++) {
            assertThat(interceptor.preHandle(post(LOGIN, "1.1.1.1"), response, new Object())).isTrue();
        }
    }

    @Test
    void paymentAndRefundPathsUseTheirOwnCategories() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setPaymentIp(new RateLimitProperties.Rule(1, Duration.ofMinutes(1)));
        properties.setRefundIp(new RateLimitProperties.Rule(1, Duration.ofMinutes(1)));
        RateLimitInterceptor interceptor = interceptor(properties);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // 支付端点第 1 次放行、第 2 次超限。
        assertThat(interceptor.preHandle(post("/api/v1/finance/payment-intents", "9.9.9.9"), response, new Object()))
                .isTrue();
        assertThrows(ApiException.class, () ->
                interceptor.preHandle(post("/api/v1/finance/payment-intents", "9.9.9.9"), response, new Object()));

        // 退款端点独立分类，仍放行（不被支付计数影响）。
        assertThat(interceptor.preHandle(post("/api/v1/finance/refunds", "9.9.9.9"), response, new Object()))
                .isTrue();
    }
}
