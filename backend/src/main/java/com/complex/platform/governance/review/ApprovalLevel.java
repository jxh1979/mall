package com.complex.platform.governance.review;

/**
 * 审批层级逐级上提类型（REQ-69）。基于发起人职级 level_rank（1..30，见 staff_levels）计算所需审批人 rank：
 * 上一级=主级+1（+3 个子级），上两级=主级+2（+6），最高权限=顶级 30，指定角色=不按 rank（由角色判断）。
 */
public enum ApprovalLevel {
    ONE_LEVEL_UP("one_level_up"),
    TWO_LEVELS_UP("two_levels_up"),
    TOP_AUTHORITY("top_authority"),
    SPECIFIC_ROLE("specific_role");

    /** 一个主职级 = 上/中/下 3 个子级，故"上一级"= level_rank + 3。 */
    static final int LEVEL_STEP = 3;
    static final int MAX_RANK = 30;

    private final String code;

    ApprovalLevel(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static ApprovalLevel fromCode(String code) {
        for (ApprovalLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        throw new IllegalArgumentException("unknown approval_level code: " + code);
    }

    /** 是否按 level_rank 计算审批人门槛。SPECIFIC_ROLE 不按 rank。 */
    public boolean rankBased() {
        return this != SPECIFIC_ROLE;
    }

    /**
     * 给定发起人 level_rank，返回所需审批人的最低 level_rank。
     * 不按 rank 的类型（SPECIFIC_ROLE）返回 0（不适用）。
     */
    public int requiredApproverRank(int initiatorRank) {
        return switch (this) {
            case ONE_LEVEL_UP -> Math.min(initiatorRank + LEVEL_STEP, MAX_RANK);
            case TWO_LEVELS_UP -> Math.min(initiatorRank + 2 * LEVEL_STEP, MAX_RANK);
            case TOP_AUTHORITY -> MAX_RANK;
            case SPECIFIC_ROLE -> 0;
        };
    }
}
