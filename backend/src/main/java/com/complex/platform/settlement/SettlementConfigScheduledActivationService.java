package com.complex.platform.settlement;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.settlement.JdbcSettlementConfigWriteRepository.DatedRow;
import com.complex.platform.settlement.JdbcSettlementConfigWriteRepository.SingleRowConfig;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 第十八批 BQ 段第 371 条 371-R1、371-R3（BSM:521「所有涉及钱的配置一律版本化：每次改动生成新版本，必须填写生效起始日期，并先存为草案、经高敏权限
 * “转正”审批通过后按指定日期生效，不即改即生效。新版本只对生效日之后发生的业务适用，已发生的业务一律按当时生效的版本计算」；BSM:521「无法判断是否涉及钱的，
 * 一律按涉及钱处理。」；ENUM:187「全系统的钱账统一按「营业日」归集。」；第十八批裁决 72）：结算配置中心单行涉钱配置（{@link #DATED}）落草案必填生效起始日期
 * （须晚于该配置范围当前营业日），复核批准（高价值判定阈值为最高权限批准）后待生效，到日由 {@link #activateDue} 逐条转正（同范围旧生效行软替换留库，
 * 同一生效日后批准的覆盖先批准的）。做法同 355-R1 内部结算价（{@link InternalPriceScheduledActivationService}），由
 * {@link InternalPriceScheduledActivationJob} 同一开关、同一周期调用；业务读侧不动（各取值处只认 status = active 且 is_draft = FALSE）。
 */
@Service
public class SettlementConfigScheduledActivationService {

    private static final String PERM = "settlement.config.manage";
    /** 371-R1 六类（分配方式、门票综合服务费、通票凑整参数、渠道费用、卡座提成费率、赠送权益预估成本）＋ 371-R3 高价值判定阈值。 */
    public static final Set<SingleRowConfig> DATED = EnumSet.of(SingleRowConfig.DISTRIBUTION_METHOD,
            SingleRowConfig.COMPREHENSIVE_SERVICE_FEE, SingleRowConfig.COMBO_DISCOUNT, SingleRowConfig.CHANNEL_FEE,
            SingleRowConfig.COMMISSION_RATE, SingleRowConfig.PASS_GIFT_COST, SingleRowConfig.HIGH_VALUE_THRESHOLD);

    private final JdbcSettlementConfigWriteRepository repository;
    private final BusinessDayResolver businessDays;
    private final AuditEventService auditEventService;

    public SettlementConfigScheduledActivationService(JdbcSettlementConfigWriteRepository repository, BusinessDayResolver businessDays,
                                                      AuditEventService auditEventService) {
        this.repository = repository;
        this.businessDays = businessDays;
        this.auditEventService = auditEventService;
    }

    /** 配置范围的当前营业日：有商户按该商户（酒吧按该酒吧，否则按所属综合体）；没有商户按项目（项目也为空即平台默认营业日）。 */
    LocalDate scopeBusinessDate(String projectId, String merchantId, Instant at) {
        return merchantId != null ? businessDays.merchantBusinessDate(projectId, merchantId, at) : businessDays.businessDate(projectId, null, at);
    }

    /** 落草案：生效起始日期必填、须为有效日期且晚于该配置范围当前营业日（两句报错与 355-R1 内部结算价同文）。 */
    public LocalDate requireFutureEffectiveDate(String effectiveFrom, String projectId, String merchantId) {
        LocalDate date;
        try {
            date = LocalDate.parse(requireText(effectiveFrom, "effective_from"));
        } catch (DateTimeParseException error) {
            throw ApiException.badRequest("VALIDATION_FAILED", "生效起始日期须为有效日期");
        }
        if (!date.isAfter(scopeBusinessDate(trimToNull(projectId), trimToNull(merchantId), Instant.now()))) {
            throw ApiException.badRequest("VALIDATION_FAILED", "生效起始日期须晚于今天（按营业日）");
        }
        return date;
    }

    /** 复核批准：生效起始日期已到 409（草案不动，起草人改日期重存即覆盖同一草案槽），否则置待生效并返回生效起始日期。 */
    public LocalDate schedule(SingleRowConfig cfg, String id, String actorId, Instant now) {
        LocalDate effectiveFrom = scheduleIfAhead(cfg, id, actorId, now);
        if (effectiveFrom == null) {
            throw ApiException.conflict("SETTLEMENT_CONFIG_EFFECTIVE_DATE_PASSED", "生效起始日期已到，请改期后重新提交草案");
        }
        return effectiveFrom;
    }

    /** 批准：生效起始日期仍晚于该范围当前营业日则置待生效并返回该日期；已到（或为空）返回 null、草案不动。 */
    public LocalDate scheduleIfAhead(SingleRowConfig cfg, String id, String actorId, Instant now) {
        DatedRow draft = repository.findDatedDraft(cfg, id);
        if (draft == null) {
            throw ApiException.badRequest("DRAFT_NOT_ACTIVATABLE", "草案不存在或非草案态,无法转正");
        }
        if (due(draft, now)) {
            return null;
        }
        if (repository.scheduleSingleRowDraft(cfg, id, actorId, now) == 0) {
            throw ApiException.badRequest("DRAFT_NOT_ACTIVATABLE", "草案不存在或非草案态,无法转正");
        }
        return draft.effectiveFrom();
    }

    /** 转正到日的待生效版本（按生效起始日期、批准先后），返回本轮转正条数。 */
    public int activateDue(Instant now) {
        int promoted = 0;
        for (SingleRowConfig cfg : DATED) {
            for (DatedRow row : repository.listScheduledSingleRows(cfg)) {
                if (due(row, now) && repository.promoteScheduledSingleRow(cfg, row.id(), now) > 0) {
                    auditEventService.recordSuccess("settlement.config.version.effective", PERM, cfg.table(), row.id(), null, null,
                            Map.of("config_group", cfg.name().toLowerCase(Locale.ROOT), "effective_from", String.valueOf(row.effectiveFrom())));
                    promoted++;
                }
            }
        }
        return promoted;
    }

    /** 生效起始日期为空（既有行，自始有效）或不晚于该范围当前营业日＝已到。 */
    private boolean due(DatedRow row, Instant now) {
        return row.effectiveFrom() == null || !row.effectiveFrom().isAfter(scopeBusinessDate(row.projectId(), row.merchantId(), now));
    }
}
