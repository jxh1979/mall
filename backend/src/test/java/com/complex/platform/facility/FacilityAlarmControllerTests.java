package com.complex.platform.facility;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.facility.dto.FacilityAlarmDispositionRequest;
import com.complex.platform.facility.dto.FacilityAlarmDutyRequest;
import com.complex.platform.facility.dto.FacilityAlarmPolicyRequest;
import com.complex.platform.facility.dto.FacilityAlarmTakeOverRequest;
import com.complex.platform.facility.dto.FacilityFaultRuleActionRequest;
import com.complex.platform.facility.dto.FacilityFaultRuleRequest;
import org.junit.jupiter.api.Test;

/**
 * HP-F01：接警接口逐权限码校验——明确接手与处置要求记录凭 facility.alarm.take_over，后台安排、矩阵与规则建档/停用凭
 * facility.alarm.manage，故障处置规则确认凭 facility.fault_rule.confirm（维保单位看规则也凭它）。
 */
class FacilityAlarmControllerTests {

    private final FacilityAlarmService service = mock(FacilityAlarmService.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final FacilityAlarmController controller = new FacilityAlarmController(service, authorization);

    @Test
    void takeOverAndMineRequireTakeOverPermission() {
        FacilityAlarmTakeOverRequest request = new FacilityAlarmTakeOverRequest("已到场", "k_take");
        controller.takeOver("falm_1", request);
        controller.mine();

        verify(authorization, org.mockito.Mockito.times(2)).requirePermission("facility.alarm.take_over");
        verify(service).takeOver("falm_1", request);
    }

    @Test
    void dutyPolicyAndListRequireManagePermissionAndDenyWithout() {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied: facility.alarm.manage"))
                .when(authorization).requirePermission("facility.alarm.manage");

        assertThatThrownBy(() -> controller.assignDuty(new FacilityAlarmDutyRequest("shift_1", "primary", "13800000000", "k_d")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> controller.savePolicy(new FacilityAlarmPolicyRequest("elevator", "urgent", 5, true, "k_p")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(controller::list).isInstanceOf(ApiException.class);
        verify(service, never()).assignDuty(org.mockito.ArgumentMatchers.any());
        verify(service, never()).savePolicy(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void takeOverRequiresIdempotencyKey() {
        assertThatThrownBy(() -> controller.takeOver("falm_1", new FacilityAlarmTakeOverRequest("note", null)))
                .isInstanceOf(ApiException.class);
        verify(service, never()).takeOver(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void dispositionRequiresTakeOverPermissionAndIdempotencyKey() {
        FacilityAlarmDispositionRequest request = new FacilityAlarmDispositionRequest(null, "停用", "k_dsp");
        controller.recordDisposition("falm_1", request);

        verify(authorization).requirePermission("facility.alarm.take_over");
        verify(service).recordDisposition("falm_1", request);
        assertThatThrownBy(() -> controller.recordDisposition("falm_1", new FacilityAlarmDispositionRequest(null, "停用", null)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void faultRuleConfirmNeedsConfirmPermissionWhileCreateAndRetireNeedManage() {
        FacilityFaultRuleActionRequest action = new FacilityFaultRuleActionRequest(null, "k_frc");
        controller.confirmFaultRule("ffr_1", action);
        verify(authorization).requirePermission("facility.fault_rule.confirm");

        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied: facility.alarm.manage"))
                .when(authorization).requirePermission("facility.alarm.manage");
        assertThatThrownBy(() -> controller.createFaultRule(new FacilityFaultRuleRequest("prj_1", null, "elevator", "困人", "停用", "k_fr")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> controller.retireFaultRule("ffr_1", action)).isInstanceOf(ApiException.class);
        verify(service, never()).createFaultRule(org.mockito.ArgumentMatchers.any());
        verify(service, never()).retireFaultRule(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void vendorListsFaultRulesWithConfirmPermission() {
        AccessActor vendor = new AccessActor("usr_v", "usr_v", "outsourced_vendor", "prj_1", null, "active");
        when(authorization.currentActor()).thenReturn(vendor);
        when(authorization.isOutsourcedVendor(vendor)).thenReturn(true);

        controller.listFaultRules(null);

        verify(authorization).requirePermission("facility.fault_rule.confirm");
        verify(authorization, never()).requirePermission("facility.alarm.manage");
    }
}
