package com.complex.platform.finance;

// 文件版本：V0.01

import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.stereotype.Service;

/**
 * 第十八批 AZ 段 285-R1（BSM:1361「连接财务对账，租金、物业费、能源费、押金、违约金按合同口径形成应收」；BSM:1353「实际收款或退款成功后才更新已收押金余额」；
 * ENUM:256 账单类型「押金」）：合同生效即挂押金应收——来源 contract_deposit，往来方与归属商户均为该商户，来源单号与幂等键＝合同号，账期＝生效月，
 * 到期日＝生效日，挂合同所属项目；实收经登记收付款（FinanceService.settleArApItem 统一回写点）写押金台账 collect 行。
 * 来源单号不带「合同号|」前缀：押金应收不进退租欠费口径（JdbcContractRepository.sumUnpaidArForContract），也就不被押金自身抵扣。
 */
@Service
public class ContractDepositReceivableService {

    private final FinanceService finance;

    public ContractDepositReceivableService(FinanceService finance) {
        this.finance = finance;
    }

    /** 押金为空或不大于 0 不挂、返回 null；同一合同再登记按幂等键落在同一台账项。 */
    public ArApItemRecord register(String contractId, String merchantId, String projectId, Long depositCent, LocalDate effectiveDate) {
        if (depositCent == null || depositCent <= 0) {
            return null;
        }
        return finance.persistDerivedArApItem("receivable", merchantId, "contract_deposit", contractId, depositCent, contractId,
                merchantId, YearMonth.from(effectiveDate).toString(), effectiveDate, projectId);
    }
}
