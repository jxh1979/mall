package com.complex.platform.access;

/**
 * 单个敏感字段的投影策略（platform-governance §24.2）：声明持有何种权限可看到明文 / 至少看到脱敏 / 可编辑。
 *
 * <p>语义（与 {@link EffectivePermissions} 配合，DENY 优先于 GRANT 与 wildcard）：
 * <ul>
 *   <li>持有 {@code plaintextPermission} → 明文（{@link FieldVisibility#PLAINTEXT}）；
 *   <li>否则持有 {@code viewPermission} → 脱敏（{@link FieldVisibility#REDACTED}）；
 *   <li>两者都没有 → 不可见（{@link FieldVisibility#HIDDEN}）；
 *   <li>额外持有 {@code editPermission} 且已是明文 → 可编辑。
 * </ul>
 *
 * <p>{@code viewPermission} 为 {@code null} 表示该字段对所有通过粗粒度接口鉴权的调用方都至少可脱敏查看
 * （例如接口已用 {@code wecom.view} 把过门，字段级仅再决定明文/脱敏）。{@code editPermission} 为
 * {@code null} 表示该字段为只读投影，永不可编辑。
 */
public record FieldProjectionPolicy(
        String fieldKey,
        String plaintextPermission,
        String viewPermission,
        String editPermission
) {

    /** 只读、按明文/脱敏分级的字段（无编辑能力）。 */
    public static FieldProjectionPolicy readOnly(String fieldKey, String plaintextPermission, String viewPermission) {
        return new FieldProjectionPolicy(fieldKey, plaintextPermission, viewPermission, null);
    }

    /** 接口已粗粒度鉴权、字段级只区分明文/脱敏的只读字段（脱敏对所有调用方可见）。 */
    public static FieldProjectionPolicy maskable(String fieldKey, String plaintextPermission) {
        return new FieldProjectionPolicy(fieldKey, plaintextPermission, null, null);
    }
}
