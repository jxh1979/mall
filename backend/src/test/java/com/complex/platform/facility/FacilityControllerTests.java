package com.complex.platform.facility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.facility.dto.FacilityEquipmentRequest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FacilityControllerTests {

    private FacilityEquipmentService service;
    private AuthorizationService authorizationService;
    private FacilityController controller;
    private AccessActor outsourced;

    @BeforeEach
    void setUp() {
        service = mock(FacilityEquipmentService.class);
        authorizationService = mock(AuthorizationService.class);
        controller = new FacilityController(service, authorizationService, new com.complex.platform.evidence.AuditedOperationExecutor(null, mock(com.complex.platform.evidence.AuditEventService.class)));
        outsourced = new AccessActor("usr_vendor_a", "vendor-a", "outsourced_vendor", "proj_1", null, "active");
        when(authorizationService.currentActor()).thenReturn(outsourced);
        when(authorizationService.isOutsourcedVendor(outsourced)).thenReturn(true);
    }

    @Test
    void outsourcedCanReadEquipmentWithViewPermissionButCannotWrite() {
        when(service.listEquipment(false)).thenReturn(new PageResponse<>(List.of(), 1, 500, 0));
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied: facility.equipment.manage"))
                .when(authorizationService).requirePermission("facility.equipment.manage");

        controller.list(false);

        verify(authorizationService).requirePermission("facility.equipment.view");
        controller.maintenanceDue();
        verify(authorizationService, org.mockito.Mockito.times(2)).requirePermission("facility.equipment.view");
        assertThatThrownBy(() -> controller.create(new FacilityEquipmentRequest(
                null, "设备", "elevator", "sp_1", null, null, null,
                "usr_vendor_a", 30, LocalDate.parse("2026-01-01"),
                null, "proj_1", null, "k_create")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
    }
}
