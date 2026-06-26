package com.complex.platform.access;

public record UserPositionAssignmentResult(
        UserPositionAssignment assignment,
        StaffLevelAssignment staffLevelAssignment,
        String roleId,
        String templateId,
        int templateApplicationCount
) {
}
