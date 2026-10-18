package com.complex.platform.access.staff;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PayrollTaxCalculationTests {
    @Test void explicitlyApprovedLegacyFormulaPreservesTruncationWithoutAssumingTaxCategory() {
        var brackets=List.of(new PayrollTaxVersion.Bracket(1,0L,400000L,10000,80000L,2000,0L),
                new PayrollTaxVersion.Bracket(2,400001L,2500000L,8000,0L,2000,0L),
                new PayrollTaxVersion.Bracket(3,2500001L,6250000L,8000,0L,3000,200000L),
                new PayrollTaxVersion.Bracket(4,6250001L,null,8000,0L,4000,700000L));
        var version=version(PayrollTaxService.validateBrackets(brackets));
        assertThat(PayrollTaxService.calculate(version,3000000).taxCent()).isEqualTo(520000);
        assertThat(PayrollTaxService.calculate(version,400001).taxCent()).isEqualTo(64000);
        assertThat(PayrollTaxService.calculate(version,0).taxCent()).isZero();
    }
    @Test void largeIntermediateProductDoesNotOverflowBeforeDivision() {
        var row=new PayrollTaxVersion.Bracket(1,0L,null,10000,0L,10000,0L);
        assertThat(PayrollTaxService.calculate(version(List.of(row)),Long.MAX_VALUE).taxCent()).isEqualTo(Long.MAX_VALUE);
    }
    @Test void missingAndOverlappingRangesCannotFormCompleteVersion() {
        assertThatThrownBy(()->PayrollTaxService.validateBrackets(List.of(new PayrollTaxVersion.Bracket(1,1L,null,10000,0L,1000,0L)))).isInstanceOf(com.complex.platform.common.api.ApiException.class);
        assertThatThrownBy(()->PayrollTaxService.validateBrackets(List.of(new PayrollTaxVersion.Bracket(1,0L,100L,10000,0L,1000,0L),new PayrollTaxVersion.Bracket(2,100L,null,10000,0L,1000,0L)))).isInstanceOf(com.complex.platform.common.api.ApiException.class);
    }
    private PayrollTaxVersion version(List<PayrollTaxVersion.Bracket> rows){return new PayrollTaxVersion("test_version","professional_test","V0.01",LocalDate.of(2099,1,1),"测试适用对象","测试已确认旧公式，不作法律参数建议",PayrollTaxService.ROUNDING,rows,"test","approved","test_review",List.of(),"test",Instant.EPOCH,Instant.EPOCH);}
}
