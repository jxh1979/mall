package com.complex.platform.notification;

// 文件版本：V0.02

/**
 * HP-F01 外部语音呼叫服务端口（BSM L1417「外部语音服务按既有第三方接入规则对接，不自研语音通道」；PGM L965 第三方资料未定，
 * 先做自有系统和接口示例）。同 {@link SmsSender} 范式：真实供应商适配器接入前由 {@link UnconfiguredVoiceCallSender}
 * 如实返回呼叫失败（VOICE_PROVIDER_NOT_CONFIGURED）。
 * 拨通、响铃、接听都不等于明确接手。
 */
public interface VoiceCallSender {

    Result call(String requestId, String phone, String message);

    /** status：placed（已拨出）、answered（已接听）、failed（呼叫失败）。 */
    record Result(String status, String providerRef, String reason) {}
}
