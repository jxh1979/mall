package com.complex.platform.analytics;

import java.util.regex.Pattern;

final class AiTextRedactor {

    private static final Pattern EMAIL = Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final Pattern ID_CARD = Pattern.compile("(?<![A-Za-z0-9])\\d{17}[\\dXx](?![A-Za-z0-9])");
    private static final Pattern CARD = Pattern.compile("(?<!\\d)\\d{13,19}(?!\\d)");

    private AiTextRedactor() {
    }

    static RedactedText redactAndTruncate(String value, int maxChars) {
        String original = value == null ? "" : value;
        String redacted = EMAIL.matcher(original).replaceAll("[EMAIL_REDACTED]");
        redacted = PHONE.matcher(redacted).replaceAll("[PHONE_REDACTED]");
        redacted = ID_CARD.matcher(redacted).replaceAll("[ID_REDACTED]");
        redacted = CARD.matcher(redacted).replaceAll("[CARD_REDACTED]");
        if (redacted.length() > maxChars) {
            redacted = redacted.substring(0, maxChars);
        }
        return new RedactedText(redacted, !redacted.equals(original));
    }

    record RedactedText(String value, boolean redactionApplied) {
    }
}
