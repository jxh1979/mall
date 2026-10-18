package com.complex.platform.bar;
// 文件版本：V0.01
import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BarPayrollMathTests {
    @Test void actualTwoAndSixHourResponsibilitiesProduceFiftyAndOneHundredFiftyYuan() {
        var result=BarPayrollMath.distribute(20000,Map.of("甲",BigDecimal.valueOf(2),"乙",BigDecimal.valueOf(6)));
        assertThat(result.amounts()).containsEntry("甲",5000L).containsEntry("乙",15000L);
        assertThat(result.residualCent()).isZero();
    }
    @Test void halfUpResidualBelongsToSettlementEntityRatherThanLastEmployee() {
        var result=BarPayrollMath.distribute(1,Map.of("甲",BigDecimal.ONE,"乙",BigDecimal.ONE));
        assertThat(result.amounts()).containsEntry("甲",1L).containsEntry("乙",1L);
        assertThat(result.residualCent()).isEqualTo(-1);
    }
    @Test void overlappingServiceTablesDoNotDoubleCountActualCoverage() {
        var start=Instant.parse("2026-09-01T12:00:00Z");
        var end=start.plusSeconds(8*3600);
        var covered=BarPayrollMath.coveredMillis(List.of(
            new BarPayrollMath.Interval(start,start.plusSeconds(4*3600)),
            new BarPayrollMath.Interval(start.plusSeconds(2*3600),end)),start,end);
        assertThat(covered).isEqualTo(8*3600*1000L);
    }
    @Test void aGapIsVisibleAndCannotBeFilledWithCurrentOwner() {
        var start=Instant.parse("2026-09-01T12:00:00Z");
        var covered=BarPayrollMath.coveredMillis(List.of(
            new BarPayrollMath.Interval(start,start.plusSeconds(3600)),
            new BarPayrollMath.Interval(start.plusSeconds(3*3600),start.plusSeconds(4*3600))),start,start.plusSeconds(4*3600));
        assertThat(covered).isEqualTo(2*3600*1000L);
    }
}
