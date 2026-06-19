package com.complex.platform.access;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 字段级动态投影（platform-governance §24.2）行为测试：同一原值按调用方有效权限分别投影为
 * 明文 / 脱敏 / 不可见，并按编辑权限标注可编辑，DENY 优先于 GRANT 与 wildcard。
 */
class FieldProjectionServiceTests {

    private final FieldProjectionService service = new FieldProjectionService(new AuthorizationService(null));

    private EffectivePermissions perms(List<String> grants, List<String> denies, boolean wildcard) {
        return new EffectivePermissions("usr_test", grants, denies, wildcard, Instant.now());
    }

    @Test
    void plaintextWhenCallerHoldsPlaintextPermission() {
        EffectivePermissions p = perms(List.of("wecom.view_raw", "wecom.view"), List.of(), false);
        FieldProjection projection = service.projectField(p, FieldProjectionService.CoreFields.RAW_TEXT, "私收款转我 13800001111");

        assertThat(projection.visibility()).isEqualTo(FieldVisibility.PLAINTEXT);
        assertThat(projection.value()).isEqualTo("私收款转我 13800001111");
        assertThat(projection.editable()).isFalse(); // RAW_TEXT 无编辑权限
    }

    @Test
    void redactedWhenCallerHoldsOnlyViewPermission() {
        EffectivePermissions p = perms(List.of("wecom.view"), List.of(), false);
        FieldProjection projection = service.projectField(p, FieldProjectionService.CoreFields.RAW_TEXT, "联系 13800001111");

        assertThat(projection.visibility()).isEqualTo(FieldVisibility.REDACTED);
        assertThat(projection.value()).doesNotContain("13800001111");
        assertThat(projection.value()).contains("[PHONE_REDACTED]");
    }

    @Test
    void hiddenWhenCallerHoldsNeitherViewNorPlaintext() {
        EffectivePermissions p = perms(List.of(), List.of(), false);
        FieldProjection projection = service.projectField(p, FieldProjectionService.CoreFields.RAW_TEXT, "联系 13800001111");

        assertThat(projection.visibility()).isEqualTo(FieldVisibility.HIDDEN);
        assertThat(projection.value()).isNull();
    }

    @Test
    void maskableFieldRedactsForAnyAuthorizedCallerWithoutPlaintextPermission() {
        // PHONE 为 maskable：viewPermission=null，接口已粗粒度鉴权，无明文权限时一律脱敏（不会 HIDDEN）。
        EffectivePermissions p = perms(List.of(), List.of(), false);
        FieldProjection projection = service.projectField(p, FieldProjectionService.CoreFields.PHONE, "13800001111");

        assertThat(projection.visibility()).isEqualTo(FieldVisibility.REDACTED);
        assertThat(projection.value()).isEqualTo("[PHONE_REDACTED]");
    }

    @Test
    void editableWhenCallerHoldsPlaintextAndEditPermission() {
        EffectivePermissions p = perms(List.of("finance.amount.view_plain", "finance.amount.edit"), List.of(), false);
        FieldProjection projection = service.projectField(p, FieldProjectionService.CoreFields.AMOUNT, "123456");

        assertThat(projection.visibility()).isEqualTo(FieldVisibility.PLAINTEXT);
        assertThat(projection.value()).isEqualTo("123456");
        assertThat(projection.editable()).isTrue();
    }

    @Test
    void denyOverridesGrantAndWildcardForPlaintext() {
        // wildcard=true 但明文权限被 DENY 覆盖 → 只能脱敏，且不可编辑。
        EffectivePermissions p = perms(List.of("finance.amount.edit"), List.of("finance.amount.view_plain"), true);
        FieldProjection projection = service.projectField(p, FieldProjectionService.CoreFields.AMOUNT, "123456");

        assertThat(projection.visibility()).isEqualTo(FieldVisibility.REDACTED);
        assertThat(projection.editable()).isFalse();
    }

    @Test
    void wildcardGrantsPlaintextWhenNotDenied() {
        EffectivePermissions p = perms(List.of(), List.of(), true);
        FieldProjection projection = service.projectField(p, FieldProjectionService.CoreFields.RAW_TEXT, "原文 13800001111");

        assertThat(projection.visibility()).isEqualTo(FieldVisibility.PLAINTEXT);
        assertThat(projection.value()).isEqualTo("原文 13800001111");
    }
}
