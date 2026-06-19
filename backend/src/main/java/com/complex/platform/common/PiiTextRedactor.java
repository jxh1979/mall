package com.complex.platform.common;

import java.util.regex.Pattern;

/**
 * 跨域共享的 PII / 凭证脱敏器（手机号/身份证/银行卡/邮箱/IBAN/各类密钥 token 正则脱敏）。
 *
 * <p>原为 analytics 包内 {@code AiTextRedactor}（AI 质检输入脱敏）；R5 T8 企微会话合规链需要在落库时
 * 对展示文本做同样的脱敏，故提升为 common 公共能力，analytics 旧入口委派至此，避免规则双份漂移。
 */
public final class PiiTextRedactor {

    private static final Pattern EMAIL = Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
    // JSON Web Tokens and common credential/API-key shapes (GitHub, Slack, Stripe, AWS access keys, …).
    private static final Pattern JWT = Pattern.compile("\\beyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\b");
    private static final Pattern SECRET_PREFIXED = Pattern.compile(
            "(?i)\\b(?:sk|pk|rk|ak|ghp|gho|ghu|ghs|ghr|xox[baprs])[-_][A-Za-z0-9-_]{8,}\\b");
    private static final Pattern AWS_ACCESS_KEY = Pattern.compile("\\b(?:AKIA|ASIA)[0-9A-Z]{16}\\b");
    private static final Pattern IBAN = Pattern.compile("(?<![A-Za-z0-9])[A-Z]{2}\\d{2}[A-Z0-9]{11,30}(?![A-Za-z0-9])");
    // International (E.164) phone numbers, e.g. +14155552671 / +442071838750.
    private static final Pattern INTL_PHONE = Pattern.compile("(?<![\\w+])\\+\\d{7,15}(?!\\d)");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final Pattern ID_CARD = Pattern.compile("(?<![A-Za-z0-9])\\d{17}[\\dXx](?![A-Za-z0-9])");
    // US Social Security Number style identifiers.
    private static final Pattern SSN = Pattern.compile("(?<!\\d)\\d{3}-\\d{2}-\\d{4}(?!\\d)");
    private static final Pattern CARD = Pattern.compile("(?<!\\d)\\d{13,19}(?!\\d)");
    // Generic catch-all for long opaque tokens (hex/base64-style API keys, session secrets, …).
    private static final Pattern LONG_TOKEN = Pattern.compile("(?<![A-Za-z0-9])[A-Za-z0-9]{32,}(?![A-Za-z0-9])");

    private PiiTextRedactor() {
    }

    /** 脱敏并截断到 maxChars。返回脱敏文本与是否发生脱敏。 */
    public static Result redactAndTruncate(String value, int maxChars) {
        String original = value == null ? "" : value;
        // Order matters: specific high-value patterns first, then the broad long-token catch-all last.
        String redacted = EMAIL.matcher(original).replaceAll("[EMAIL_REDACTED]");
        redacted = JWT.matcher(redacted).replaceAll("[SECRET_REDACTED]");
        redacted = SECRET_PREFIXED.matcher(redacted).replaceAll("[SECRET_REDACTED]");
        redacted = AWS_ACCESS_KEY.matcher(redacted).replaceAll("[SECRET_REDACTED]");
        redacted = IBAN.matcher(redacted).replaceAll("[SECRET_REDACTED]");
        redacted = INTL_PHONE.matcher(redacted).replaceAll("[PHONE_REDACTED]");
        redacted = PHONE.matcher(redacted).replaceAll("[PHONE_REDACTED]");
        redacted = ID_CARD.matcher(redacted).replaceAll("[ID_REDACTED]");
        redacted = SSN.matcher(redacted).replaceAll("[ID_REDACTED]");
        redacted = CARD.matcher(redacted).replaceAll("[CARD_REDACTED]");
        redacted = LONG_TOKEN.matcher(redacted).replaceAll("[SECRET_REDACTED]");
        if (redacted.length() > maxChars) {
            redacted = redacted.substring(0, maxChars);
        }
        return new Result(redacted, !redacted.equals(original));
    }

    public record Result(String value, boolean redactionApplied) {
    }
}
