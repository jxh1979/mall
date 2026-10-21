package com.complex.platform.parking;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HP-P01 停车场后台：业务参数、在场与记录、消费抵免台账、月租车牌、员工车辆、人工放行。 */
@RestController
@RequestMapping("/api/v1/parking")
public class ParkingController {

    private final ParkingService parking;
    private final ParkingReleaseService releases;
    private final ParkingCreditService credits;

    public ParkingController(ParkingService parking, ParkingReleaseService releases, ParkingCreditService credits) {
        this.parking = parking;
        this.releases = releases;
        this.credits = credits;
    }

    @GetMapping("/policies/{project_id}")
    public ApiResponse<Map<String, Object>> getPolicy(@PathVariable("project_id") String projectId) {
        return ApiResponse.ok(parking.getPolicy(projectId));
    }

    @PutMapping("/policies/{project_id}")
    public ApiResponse<Map<String, Object>> savePolicy(@PathVariable("project_id") String projectId,
            @RequestBody(required = false) Map<String, Object> request) {
        return ApiResponse.ok(parking.savePolicy(projectId, request));
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview(@RequestParam(value = "project_id", required = false) String projectId,
            @RequestParam(value = "status", required = false) String status) {
        return ApiResponse.ok(parking.overview(projectId, status));
    }

    @GetMapping("/monthly-vehicles")
    public ApiResponse<PageResponse<ParkingService.Vehicle>> monthlyVehicles(
            @RequestParam(value = "project_id", required = false) String projectId) {
        return ApiResponse.ok(parking.listMonthlyVehicles(projectId));
    }

    @PostMapping("/contracts/{contract_id}/vehicles")
    public ApiResponse<ParkingService.Vehicle> registerMonthlyVehicle(@PathVariable("contract_id") String contractId,
            @RequestBody(required = false) ParkingRequests.MonthlyVehicle request) {
        return ApiResponse.ok(parking.registerMonthlyVehicle(contractId, request));
    }

    @PostMapping("/monthly-vehicles/{vehicle_id}/revoke")
    public ApiResponse<ParkingService.Vehicle> revokeMonthlyVehicle(@PathVariable("vehicle_id") String id,
            @RequestBody(required = false) ParkingRequests.Revoke request) {
        return ApiResponse.ok(parking.revokeMonthlyVehicle(id, request));
    }

    @GetMapping("/staff-vehicles")
    public ApiResponse<PageResponse<ParkingService.Vehicle>> staffVehicles(
            @RequestParam(value = "project_id", required = false) String projectId) {
        return ApiResponse.ok(parking.listStaffVehicles(projectId));
    }

    @PostMapping("/staff-vehicles")
    public ApiResponse<ParkingService.Vehicle> registerStaffVehicle(@RequestBody(required = false) ParkingRequests.StaffVehicle request) {
        return ApiResponse.ok(parking.registerStaffVehicle(request));
    }

    @PostMapping("/staff-vehicles/{vehicle_id}/revoke")
    public ApiResponse<ParkingService.Vehicle> revokeStaffVehicle(@PathVariable("vehicle_id") String id,
            @RequestBody(required = false) ParkingRequests.Revoke request) {
        return ApiResponse.ok(parking.revokeStaffVehicle(id, request));
    }

    /** 消费抵免台账：抵免、构成资格的消费与成本分担（parking.ops.view，项目范围）。 */
    @GetMapping("/credits")
    public ApiResponse<PageResponse<Map<String, Object>>> credits(@RequestParam(value = "project_id", required = false) String projectId,
            @RequestParam(value = "business_date", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate businessDate) {
        return ApiResponse.ok(credits.list(projectId, businessDate));
    }

    @GetMapping("/manual-releases")
    public ApiResponse<PageResponse<ParkingReleaseService.Release>> manualReleases(
            @RequestParam(value = "project_id", required = false) String projectId) {
        return ApiResponse.ok(releases.list(projectId));
    }

    @PostMapping("/manual-releases")
    public ApiResponse<ParkingReleaseService.Release> recordManualRelease(
            @RequestBody(required = false) ParkingRequests.ManualRelease request) {
        return ApiResponse.ok(releases.record(request));
    }
}
