package com.complex.platform.access;

import com.complex.platform.common.PiiTextRedactor;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

/**
 * 字段级权限动态投影（platform-governance §24.2）。把 {@link EffectivePermissions} 真正落到数据出口：
 * 同一敏感字段对不同调用方按其有效权限返回 明文 / 脱敏 / 不可见，并标注是否可编辑。
 *
 * <p>此前字段脱敏只有固定的 {@link PiiTextRedactor}（无论权限一律脱敏），{@code EffectivePermissions} 仅供前端
 * 展示菜单、不参与服务端数据投影。本服务把"是否明文/可编辑"的判定下沉到服务端，供所有敏感字段统一调用，
 * 避免每个接口各写一套分级逻辑（既有 {@code ComplianceController#viewWecomMessages} 即此类孤例）。
 *
 * <p>权限判定严格对齐 {@link AuthorizationService#requirePermission} / {@link EffectivePermissions} 语义：
 * DENY 优先级最高（即便 wildcard 也被 DENY 覆盖），其次 GRANT，其次平台管理员 wildcard。
 *
 * <p>核心敏感字段策略见 {@link CoreFields}（手机号/金额/原文）。调用方也可传入自定义
 * {@link FieldProjectionPolicy}，无需扩散分级判定。
 */
@Service
@ConditionalOnBean(AuthorizationService.class)
public class FieldProjectionService {

    /** 投影脱敏时的截断上限，与 WeCom 展示口径一致的安全上限。 */
    private static final int MAX_REDACTED_CHARS = 4096;

    private final AuthorizationService authorizationService;

    public FieldProjectionService(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    /** 平台预置的核心敏感字段投影策略（手机号/金额/原文）。可先覆盖核心字段，不假装全量。 */
    public static final class CoreFields {

        /** 手机号：明文需 {@code pii.phone.view_plain}，否则脱敏（接口已粗粒度鉴权）。 */
        public static final FieldProjectionPolicy PHONE =
                FieldProjectionPolicy.maskable("phone", "pii.phone.view_plain");

        /** 金额（分）：明文需 {@code finance.amount.view_plain}，否则脱敏；持 {@code finance.amount.edit} 可编辑。 */
        public static final FieldProjectionPolicy AMOUNT =
                new FieldProjectionPolicy("amount", "finance.amount.view_plain", null, "finance.amount.edit");

        /** 企微/会话原文：明文需 {@code wecom.view_raw}，仅 {@code wecom.view} 时脱敏，二者皆无则不可见。 */
        public static final FieldProjectionPolicy RAW_TEXT =
                FieldProjectionPolicy.readOnly("raw_text", "wecom.view_raw", "wecom.view");

        private CoreFields() {
        }
    }

    /**
     * 按当前调用方的有效权限投影一个字段（最常用入口，内部自取当前 actor 的 {@link EffectivePermissions}）。
     */
    public FieldProjection projectField(FieldProjectionPolicy policy, String rawValue) {
        return projectField(authorizationService.effectivePermissions(), policy, rawValue);
    }

    /**
     * 按显式给定的有效权限投影一个字段。供批量场景（一次取权限、循环投影多行/多字段）避免重复计算权限，
     * 也供平台管理侧用 {@link AuthorizationService#effectivePermissionsForUser} 模拟指定用户的可见性。
     */
    public FieldProjection projectField(EffectivePermissions permissions, FieldProjectionPolicy policy, String rawValue) {
        FieldVisibility visibility = resolveVisibility(permissions, policy);
        return switch (visibility) {
            case PLAINTEXT -> new FieldProjection(
                    policy.fieldKey(), FieldVisibility.PLAINTEXT, rawValue, canEdit(permissions, policy));
            case REDACTED -> new FieldProjection(
                    policy.fieldKey(), FieldVisibility.REDACTED,
                    rawValue == null ? null : PiiTextRedactor.redactAndTruncate(rawValue, MAX_REDACTED_CHARS).value(),
                    false);
            case HIDDEN -> new FieldProjection(policy.fieldKey(), FieldVisibility.HIDDEN, null, false);
        };
    }

    /** 批量投影一组字段（同一调用方、同一行的多个敏感字段），只取一次有效权限。 */
    public Map<String, FieldProjection> projectFields(Map<FieldProjectionPolicy, String> fields) {
        EffectivePermissions permissions = authorizationService.effectivePermissions();
        java.util.LinkedHashMap<String, FieldProjection> out = new java.util.LinkedHashMap<>();
        fields.forEach((policy, rawValue) ->
                out.put(policy.fieldKey(), projectField(permissions, policy, rawValue)));
        return out;
    }

    private FieldVisibility resolveVisibility(EffectivePermissions permissions, FieldProjectionPolicy policy) {
        if (holds(permissions, policy.plaintextPermission())) {
            return FieldVisibility.PLAINTEXT;
        }
        // viewPermission == null 表示接口已粗粒度鉴权、字段级最低保底为脱敏可见。
        if (policy.viewPermission() == null || holds(permissions, policy.viewPermission())) {
            return FieldVisibility.REDACTED;
        }
        return FieldVisibility.HIDDEN;
    }

    private boolean canEdit(EffectivePermissions permissions, FieldProjectionPolicy policy) {
        return policy.editPermission() != null && holds(permissions, policy.editPermission());
    }

    /**
     * 判断有效权限集是否授予某权限码。DENY 优先（即便 wildcard 也被 DENY 覆盖），其次 GRANT，其次 wildcard。
     * 与 {@link AuthorizationService#requirePermission} / {@link EffectivePermissions} 语义一致。
     */
    private boolean holds(EffectivePermissions permissions, String permissionCode) {
        if (permissions == null || permissionCode == null) {
            return false;
        }
        String code = permissionCode.trim();
        if (permissions.denies() != null && permissions.denies().contains(code)) {
            return false;
        }
        if (permissions.grants() != null && permissions.grants().contains(code)) {
            return true;
        }
        return permissions.wildcard();
    }
}
