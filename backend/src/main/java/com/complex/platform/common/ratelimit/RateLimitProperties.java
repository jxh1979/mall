package com.complex.platform.common.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 敏感端点限速阈值（可配，前缀 {@code complex.rate-limit}）。窗口用 Duration 字面量绑定（如 {@code 1m} / {@code PT1M}）。
 * 默认值面向繁忙场馆（多终端共享 NAT 出口）留足余量，仅拦异常高频；如需更严可下调。
 */
@Component
@ConfigurationProperties(prefix = "complex.rate-limit")
public class RateLimitProperties {

    /** 总开关：关闭后应用层限速全部放行（既有账号锁定等机制不受影响）。 */
    private boolean enabled = true;

    /** 登录 / 刷新 / 手机登录——按来源 IP。 */
    private Rule authIp = new Rule(30, Duration.ofMinutes(1));

    /** 支付类写端点——按来源 IP。 */
    private Rule paymentIp = new Rule(120, Duration.ofMinutes(1));

    /** 退款类写端点——按来源 IP。 */
    private Rule refundIp = new Rule(60, Duration.ofMinutes(1));

    /** 手机验证码登录——按手机号（OTP 无账号锁定，此维度防单号撞码）。 */
    private Rule otpPhone = new Rule(5, Duration.ofMinutes(1));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Rule getAuthIp() {
        return authIp;
    }

    public void setAuthIp(Rule authIp) {
        this.authIp = authIp;
    }

    public Rule getPaymentIp() {
        return paymentIp;
    }

    public void setPaymentIp(Rule paymentIp) {
        this.paymentIp = paymentIp;
    }

    public Rule getRefundIp() {
        return refundIp;
    }

    public void setRefundIp(Rule refundIp) {
        this.refundIp = refundIp;
    }

    public Rule getOtpPhone() {
        return otpPhone;
    }

    public void setOtpPhone(Rule otpPhone) {
        this.otpPhone = otpPhone;
    }

    /** 单条限速规则：窗口内最大放行次数 + 窗口时长。 */
    public static class Rule {
        private int limit;
        private Duration window;

        public Rule() {
        }

        public Rule(int limit, Duration window) {
            this.limit = limit;
            this.window = window;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }
    }
}
