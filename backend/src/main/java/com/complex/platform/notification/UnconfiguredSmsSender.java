package com.complex.platform.notification;

// 文件版本：V0.01

import org.springframework.stereotype.Component;

/**
 * 未接入真实短信供应商时的短信发送器（BSM 短信平台行「不自研短信通道」；ENUM「不伪造对账结果」）：
 * 每次发送都如实返回 failed / SMS_PROVIDER_NOT_CONFIGURED，通知日志记失败，不出现「已发送」。
 * 接入后由真实适配器实现 {@link SmsSender} 替换本类，不得返回 sent。
 */
@Component
public class UnconfiguredSmsSender implements SmsSender {

    static final String REASON = "SMS_PROVIDER_NOT_CONFIGURED";

    @Override
    public SmsSendResult send(SmsMessage message) {
        return new SmsSendResult("failed", null, REASON);
    }
}
