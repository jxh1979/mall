package com.complex.platform.notification;

// 文件版本：V0.02

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 测试替身（只在 test-classes，交付物不含）：组件扫描进测试上下文并以 @Primary 顶替生产的 {@link UnconfiguredVoiceCallSender}，
 * 按 {@link Mode} 返回确定结果，供接警流程用例覆盖已拨出、已接听、呼叫失败各分支。
 */
@Component
@Primary
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
