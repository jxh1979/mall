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

    @Test
    void fromCodeRoundTrips() {
        assertThat(ApprovalLevel.fromCode("two_levels_up")).isEqualTo(ApprovalLevel.TWO_LEVELS_UP);
        assertThat(ApprovalLevel.fromCode("top_authority").code()).isEqualTo("top_authority");
    }
}
