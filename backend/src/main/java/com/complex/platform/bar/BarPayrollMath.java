package com.complex.platform.bar;
// 文件版本：V0.01
import com.complex.platform.common.money.MoneyRounding;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.Duration;
import java.util.*;
final class BarPayrollMath {
    record Interval(Instant from,Instant to) {
        Interval {if(from==null || to==null || !to.isAfter(from))throw BarPayrollSupport.invalid("责任区间起止无效");}
    }
    record Distribution(Map<String,Long> amounts,long residualCent) {}
    static Distribution distribute(long amount,Map<String,BigDecimal> weights) {
        if(amount<0 || weights.isEmpty() || weights.values().stream().anyMatch(v->v==null || v.signum()<=0))
            throw BarPayrollSupport.invalid("分配金额或计份依据无效");
        BigDecimal total=weights.values().stream().reduce(BigDecimal.ZERO,BigDecimal::add);
        Map<String,Long> result=new TreeMap<>();
        weights.forEach((user,weight)->result.put(user,BigDecimal.valueOf(amount).multiply(weight)
            .divide(total,0,RoundingMode.HALF_UP).longValueExact()));
        long sum=result.values().stream().reduce(0L,Math::addExact);
        return new Distribution(result,Math.subtractExact(amount,sum));
    }
    static long coveredMillis(List<Interval> input,Instant start,Instant end) {
        if(start==null || end==null || !end.isAfter(start))return 0;
        var spans=input.stream().filter(s->s.to().isAfter(start) && s.from().isBefore(end))
            .map(s->new Interval(s.from().isBefore(start)?start:s.from(),s.to().isAfter(end)?end:s.to()))
            .sorted(Comparator.comparing(Interval::from)).toList();
        Instant from=null,to=null;long result=0;
        for(var span:spans) {
            if(from==null){from=span.from();to=span.to();}
            else if(!span.from().isAfter(to)){if(span.to().isAfter(to))to=span.to();}
            else {result=Math.addExact(result,Duration.between(from,to).toMillis());from=span.from();to=span.to();}
        }
        return from==null?0:Math.addExact(result,Duration.between(from,to).toMillis());
    }
}
