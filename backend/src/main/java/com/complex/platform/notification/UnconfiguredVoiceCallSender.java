package com.complex.platform.notification;

// 文件版本：V0.01

import org.springframework.stereotype.Component;

/**
 * 未接入外部语音供应商时的语音呼叫发送器（BSM 设备告警条「外部语音服务按既有第三方接入规则对接，不自研语音通道」）：
 * 每次呼叫都如实返回 failed / VOICE_PROVIDER_NOT_CONFIGURED，接警按发送失败立即转当班备用，不出现「已拨出」。
 * 接入后由真实适配器实现 {@link VoiceCallSender} 替换本类，不得返回 placed/answered。
 */
@Component
public class UnconfiguredVoiceCallSender implements VoiceCallSender {

    static final String REASON = "VOICE_PROVIDER_NOT_CONFIGURED";

    @Override
    public Result call(String requestId, String phone, String message) {
        return new Result("failed", null, REASON);
    }
}
