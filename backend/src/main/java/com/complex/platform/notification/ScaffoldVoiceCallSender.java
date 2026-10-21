package com.complex.platform.notification;

// 文件版本：V0.01

import org.springframework.stereotype.Component;

/** 语音呼叫占位实现：真实语音供应商适配器接入前只返回确定的占位结果，供接警流程与测试使用。 */
@Component
public class ScaffoldVoiceCallSender implements VoiceCallSender {

    public enum Mode {
        PLACED,
        ANSWERED,
        FAILED
    }

    private volatile Mode mode = Mode.PLACED;

    public void setMode(Mode mode) {
        this.mode = mode == null ? Mode.PLACED : mode;
    }

    @Override
    public Result call(String requestId, String phone, String message) {
        return switch (mode) {
            case PLACED -> new Result("placed", "scaffold-voice-" + requestId, null);
            case ANSWERED -> new Result("answered", "scaffold-voice-" + requestId, null);
            case FAILED -> new Result("failed", null, "scaffold voice sender forced failure");
        };
    }
}
