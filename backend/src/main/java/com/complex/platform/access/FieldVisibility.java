package com.complex.platform.access;

/**
 * 字段级动态投影的可见度档位（platform-governance §24.2 字段级权限动态投影）。
 *
 * <ul>
 *   <li>{@link #PLAINTEXT} 明文：调用方持有明文权限，原值原样返回；
 *   <li>{@link #REDACTED} 脱敏：调用方仅持有普通查看权限，按 {@link com.complex.platform.common.PiiTextRedactor} 脱敏；
 *   <li>{@link #HIDDEN} 不可见：调用方连普通查看权限都没有，字段置空（{@code null}）。
 * </ul>
 *
 * <p>编辑能力单独由 {@link FieldProjection#editable()} 表达（明文且持有编辑权限才可编辑）。
 */
public enum FieldVisibility {
    PLAINTEXT,
    REDACTED,
    HIDDEN
}
