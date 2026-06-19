package com.complex.platform.governance.review;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApprovalLevelTests {

    @Test
    void oneLevelUpAddsOneMainLevelOfThreeSubRanks() {
        // M2 lower = rank 4 → one level up = M3 lower = rank 7
        assertThat(ApprovalLevel.ONE_LEVEL_UP.requiredApproverRank(4)).isEqualTo(7);
        assertThat(ApprovalLevel.ONE_LEVEL_UP.rankBased()).isTrue();
    }

    @Test
    void twoLevelsUpAddsTwoMainLevels() {
        // M7 lower = rank 19 → two levels up = min(19+6,30) = 25
        assertThat(ApprovalLevel.TWO_LEVELS_UP.requiredApproverRank(19)).isEqualTo(25);
    }

    @Test
    void escalationIsCappedAtTopRankThirty() {
        assertThat(ApprovalLevel.ONE_LEVEL_UP.requiredApproverRank(29)).isEqualTo(30);
        assertThat(ApprovalLevel.TWO_LEVELS_UP.requiredApproverRank(28)).isEqualTo(30);
    }

    @Test
    void topAuthorityAlwaysRequiresRankThirty() {
        assertThat(ApprovalLevel.TOP_AUTHORITY.requiredApproverRank(1)).isEqualTo(30);
        assertThat(ApprovalLevel.TOP_AUTHORITY.requiredApproverRank(25)).isEqualTo(30);
    }

    @Test
    void specificRoleIsNotRankBased() {
        assertThat(ApprovalLevel.SPECIFIC_ROLE.rankBased()).isFalse();
        assertThat(ApprovalLevel.SPECIFIC_ROLE.requiredApproverRank(10)).isEqualTo(0);
    }

    // 双轨收口（§24.3）：发起人无职级时的安全保守 rank 下限——绝不弱于既有闸，且随升级类型递增。
    @Test
    void conservativeRankFloorsByRequiredLevelWhenInitiatorRankUnknown() {
        // requiredLevel=3 → floor = max(3,1)*3 = 9
        assertThat(ApprovalLevel.ONE_LEVEL_UP.conservativeRequiredApproverRank(3)).isEqualTo(12);   // 9+3
        assertThat(ApprovalLevel.TWO_LEVELS_UP.conservativeRequiredApproverRank(3)).isEqualTo(15);  // 9+6
        assertThat(ApprovalLevel.TOP_AUTHORITY.conservativeRequiredApproverRank(0)).isEqualTo(30);
        assertThat(ApprovalLevel.SPECIFIC_ROLE.conservativeRequiredApproverRank(3)).isEqualTo(0);
        // 即便 requiredLevel=0 也有非零下限（floor 用 max(level,1)），不退化为旁路。
        assertThat(ApprovalLevel.TWO_LEVELS_UP.conservativeRequiredApproverRank(0)).isEqualTo(9);    // 3+6
    }

    @Test
    void fromCodeRoundTrips() {
        assertThat(ApprovalLevel.fromCode("two_levels_up")).isEqualTo(ApprovalLevel.TWO_LEVELS_UP);
        assertThat(ApprovalLevel.fromCode("top_authority").code()).isEqualTo("top_authority");
    }
}
