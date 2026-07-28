package com.complex.platform.common;

/**
 * 手机号展示脱敏（前 3 后 4，中间固定四星）。
 *
 * <p>原为 {@code CustomerService} / {@code TicketTransferService} / {@code VoucherTransferService} 各自的私有
 * {@code maskPhone}，P0-A（多主体 PII 加密，V339）需要在商户/供应商/招商线索/费用通知/进场报备五处读端复用同一口径，
 * 故按 {@link PiiTextRedactor} 的先例提升为公共能力，避免规则多份漂移。
 *
 * <p>规则对齐业务 HTML「员工日常界面只显示脱敏号码，不见明文」：有明文查看权限的读端返回原值，
 * 无权限的读端一律走本类脱敏。
 */
public final class PhoneMask {

    /**
     * 明文手机号查看权限（V339 种子）。无此权限的读端一律看掩码。
     * 单点定义，避免五个 controller 各写一份魔法字符串导致漂移。
     */
    public static final String VIEW_PLAINTEXT_PERMISSION = "pii.contact_phone.view";

    private PhoneMask() {
    }

    /** 脱敏；null/空白原样返回（无值可脱敏）。 */
    public static String mask(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        String value = phone.trim();
        if (value.length() <= 7) {
            return value.charAt(0) + "****" + value.charAt(value.length() - 1);
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }
}
