package com.complex.platform.settlement;

// 文件版本：V0.01

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.settlement.JdbcSettlementConfigWriteRepository.CatalogEntryRow;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 第十八批 BA 段第 355 条 355-R1（BSM:521「所有涉及钱的配置一律版本化：每次改动生成新版本，必须填写生效起始日期，并先存为草案、经高敏权限“转正”审批通过后
 * 按指定日期生效，不即改即生效。新版本只对生效日之后发生的业务适用」；BSM:1671「价格变更形成新版本、指定生效时间并只影响后续业务」；ENUM:187「全系统的钱账
 * 统一按「营业日」归集。」）：已批准待生效（scheduled）的内部结算价，生效起始日期不晚于提供方当前营业日的，按生效起始日期、批准先后逐条转正——旧生效行软替换、
 * 本行生效、同一事务同步四处业务快照（同一生效日后批准的覆盖先批准的）。某条失败落失败留痕后继续下一条，下一轮再试。
 * 由 {@link InternalPriceScheduledActivationJob} 定时调用。
 */
@Service
public class InternalPriceScheduledActivationService {

    private static final String PERM = "settlement.config.manage";

    private final JdbcSettlementConfigWriteRepository repository;
    private final BusinessDayResolver businessDays;
    private final AuditEventService auditEventService;

    public InternalPriceScheduledActivationService(JdbcSettlementConfigWriteRepository repository, BusinessDayResolver businessDays,
                                                   AuditEventService auditEventService) {
        this.repository = repository;
        this.businessDays = businessDays;
        this.auditEventService = auditEventService;
    }

    /** 提供方当前营业日：商户按该商户（酒吧按该酒吧，否则按所属综合体），经营主体按平台默认营业日。 */
    static LocalDate providerBusinessDate(BusinessDayResolver businessDays, String merchantId, Instant at) {
        return merchantId != null ? businessDays.merchantBusinessDate(null, merchantId, at) : businessDays.businessDate(null, null, at);
    }

    /** 转正到日的待生效版本，返回本轮转正条数。 */
    public int activateDue(Instant now) {
        int promoted = 0;
        for (CatalogEntryRow row : repository.listScheduledInternalPrices()) {
            if (row.effectiveFrom() != null && row.effectiveFrom().isAfter(providerBusinessDate(businessDays, row.merchantId(), now))) {
                continue;
            }
            try {
                if (repository.promoteScheduledInternalPrice(row.id(), row.itemCode(), row.priceCent(), now) > 0) {
                    auditEventService.recordSuccess("settlement.config.internal_price_catalog.effective", PERM, "internal_price_catalog",
                            row.itemCode(), null, null, Map.of("item_code", row.itemCode(), "effective_from", String.valueOf(row.effectiveFrom()),
                                    "price_cent", row.priceCent()));
                    promoted++;
                }
            } catch (RuntimeException error) {
                auditEventService.recordFailure("settlement.config.internal_price_catalog.effective_failed", PERM, "internal_price_catalog",
                        row.itemCode(), null, null, error instanceof ApiException api ? api.code() : error.getClass().getSimpleName());
            }
        }
        return promoted;
    }
}
