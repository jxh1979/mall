package com.complex.platform.notification;

// 文件版本：V0.01

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 测试替身（只在 test-classes，交付物不含）：组件扫描进测试上下文并以 @Primary 顶替生产的 {@link UnconfiguredSmsSender}，
 * 默认返回 sent，供通知链路既有用例断言「已发送」分支；{@link #fail(String)} 可改为失败，{@link #reset()} 复位。
 */
@Component
@Primary
public class ScaffoldSmsSender implements SmsSender {

    private volatile String status = "sent";
    private volatile String reason;
    private volatile int sendCount;

    @Override
    public SmsSendResult send(SmsMessage message) {
        sendCount++;
        if ("sent".equals(status)) {
            return new SmsSendResult("sent", "scaffold-sms-" + message.requestId(), null);
        }
        return new SmsSendResult(status, null, reason);
    }

    public void fail(String reason) {
        this.status = "failed";
        this.reason = reason;
    }

    public void reset() {
        status = "sent";
        reason = null;
        sendCount = 0;
    }

    public int sendCount() {
        return sendCount;
    }
}
