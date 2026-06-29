package com.complex.platform.notification;

import org.springframework.stereotype.Component;

/**
 * Provider-neutral SMS scaffold. Until a real SMS vendor adapter is wired, this sender only records
 * deterministic placeholder outcomes for the notification pipeline and tests.
 */
@Component
public class ScaffoldSmsSender implements SmsSender {

    public enum Mode {
        SENT,
        FAILED,
        THROTTLED
    }

    private volatile Mode mode = Mode.SENT;

    public void setMode(Mode mode) {
        this.mode = mode == null ? Mode.SENT : mode;
    }

    @Override
    public SmsSendResult send(SmsMessage message) {
        return switch (mode) {
            case SENT -> new SmsSendResult("sent", "scaffold-sms-" + message.requestId(), null);
            case FAILED -> new SmsSendResult("failed", null, "scaffold SMS sender forced failure");
            case THROTTLED -> new SmsSendResult("throttled", null, "scaffold SMS sender forced throttling");
        };
    }
}
