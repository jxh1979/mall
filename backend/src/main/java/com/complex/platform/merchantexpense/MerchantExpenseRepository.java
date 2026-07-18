package com.complex.platform.merchantexpense;

import com.complex.platform.common.api.PageResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** 商户费用归集台账存取（append-only：只 INSERT，不 UPDATE/DELETE）。 */
public interface MerchantExpenseRepository {

    /** 分页查询（筛选参数均可空；null=不过滤）。 */
    PageResponse<MerchantExpenseEntryRecord> listEntries(
            String merchantId, String period, String expenseType, String source, int page, int pageSize);

    /** 期间汇总：period 必填，merchantId 可空（空=全部商户），按商户 + 类型分组合计。 */
    List<MerchantExpenseSummaryRecord> summarize(String period, String merchantId);

    Optional<MerchantExpenseEntryRecord> findById(String id);

    /** 插入一行（ON CONFLICT (id) DO NOTHING 幂等）；返回是否真的新插入。 */
    boolean insertEntry(MerchantExpenseEntryRecord entry);

    /** 能耗费自动挂接：ar_ap_items(source_type='energy_fee', 应收) 按期汇入；返回新计入行数。 */
    int attachEnergyFees(String period, String operatorUserId, Instant now);

    /** 物业费自动挂接（账单路径）：property_fee_bills 按期汇入；返回新计入行数。 */
    int attachPropertyFees(String period, String operatorUserId, Instant now);

    /** 物业费自动挂接（合同计费路径）：ar_ap_items(source_type='contract_property_fee', 应收) 按期汇入；返回新计入行数。 */
    int attachContractPropertyFees(String period, String operatorUserId, Instant now);

    /** 金额漂移报告：该期每个已挂接源的台账净额（原行+冲销行合计）对上游单据当前金额，不一致的清单。 */
    /** 租金自动挂接：ar_ap_items receivable 且 source_type ∈ (contract_rent, turnover_rent)，按 accounting_period 归期。 */
    int attachContractRent(String period, String operatorUserId, Instant now);

    List<MerchantExpenseDriftRecord> listAutoDrift(String period);
}
