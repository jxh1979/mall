package com.complex.platform.parking;

// 文件版本：V0.01

/** HP-P01 停车场请求体（前端经 adminClient.mapKeys 蛇形化，后端 SNAKE_CASE 反序列化）。 */
public final class ParkingRequests {

    private ParkingRequests() {
    }

    /** 月租车牌登记（合同在路径上）。 */
    public record MonthlyVehicle(String idempotencyKey, String plateNo) {}

    /** 员工车辆登记：所属主体为本主体员工登记车牌。 */
    public record StaffVehicle(String idempotencyKey, String projectId, String userId, String plateNo) {}

    public record Revoke(String idempotencyKey) {}

    /**
     * 人工放行：release_reason 取 recognition_failed（识别失败）／gate_fault（道闸故障）／fee_dispute（缴费争议）；
     * waive_type 取 none／free（免费放行）／reduction（减免，带 reduction_cent），免费与减免须指定物业主管为最终审批人。
     */
    public record ManualRelease(String idempotencyKey, String projectId, String plateNo, String parkingSessionId,
            String releaseReason, String reason, String waiveType, Long reductionCent, String finalApproverId) {}

    /** 客人端缴费下单。 */
    public record CustomerPayment(String idempotencyKey, String parkingSessionId) {}
}
