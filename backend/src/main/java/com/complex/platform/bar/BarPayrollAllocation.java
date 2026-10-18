package com.complex.platform.bar;
// 文件版本：V0.01
import java.util.List;
public record BarPayrollAllocation(String id,String userId,String month,String componentType,long amountCent,
        String merchantId,String projectId,String payerCompanyId,List<String> sourceBillIds,String ruleVersionId,String status) {}
