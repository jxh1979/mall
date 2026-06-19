package com.complex.platform.access;

/**
 * 单个敏感字段按调用方有效权限投影后的结果（platform-governance §24.2）。
 *
 * @param fieldKey    字段键（如 {@code phone} / {@code amount} / {@code raw_text}）
 * @param visibility  可见度档位（明文/脱敏/不可见）
 * @param value       投影后的展示值：明文=原值；脱敏=脱敏文本；不可见={@code null}
 * @param editable    调用方是否可编辑该字段（仅明文且持有编辑权限时为 {@code true}）
 */
public record FieldProjection(
        String fieldKey,
        FieldVisibility visibility,
        String value,
        boolean editable
) {

    /** 是否发生了脱敏（脱敏档位）。 */
    public boolean redacted() {
        return visibility == FieldVisibility.REDACTED;
    }

    /** 是否对调用方完全隐藏。 */
    public boolean hidden() {
        return visibility == FieldVisibility.HIDDEN;
    }
}
