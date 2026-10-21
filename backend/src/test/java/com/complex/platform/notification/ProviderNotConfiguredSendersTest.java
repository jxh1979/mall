package com.complex.platform.notification;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * ROOT-05 R1/R2：未接入真实短信/语音供应商时，发送器不得报「已发送/已拨出」（BSM 短信平台行「不自研短信通道」、
 * BSM 设备告警条「外部语音服务按既有第三方接入规则对接，不自研语音通道」；ENUM「不伪造对账结果」）。纯单元，不起 Spring。
 */
class ProviderNotConfiguredSendersTest {

    @Test
    void unconfiguredSmsSenderReportsFailedNotSent() {
        SmsSendResult result = new UnconfiguredSmsSender().send(new SmsMessage(
                "r1", "stored_wine", "deposit", "13800000000", "stored_wine_deposit", Map.of()));

        assertThat(result.status()).isEqualTo("failed");
        assertThat(result.reason()).isEqualTo("SMS_PROVIDER_NOT_CONFIGURED");
        assertThat(result.providerRef()).isNull();
    }

    @Test
    void unconfiguredVoiceSenderReportsFailedNotPlaced() {
        VoiceCallSender.Result result = new UnconfiguredVoiceCallSender().call("r1", "13800000000", "m");

        assertThat(result.status()).isEqualTo("failed");
        assertThat(result.reason()).isEqualTo("VOICE_PROVIDER_NOT_CONFIGURED");
        assertThat(result.providerRef()).isNull();
    }
}
