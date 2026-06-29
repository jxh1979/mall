package com.complex.platform.notification;

public record NotificationSwitchDecision(
        boolean allowed,
        String reason
) {
    public static NotificationSwitchDecision allow() {
        return new NotificationSwitchDecision(true, null);
    }

    public static NotificationSwitchDecision blocked(String reason) {
        return new NotificationSwitchDecision(false, reason);
    }
}
